package factory;

import fraymus.factory.*;
import fraymus.factory.kernel.FactoryKernelService;
import fraymus.factory.kernel.LocalSkillDispatcher;
import imgui.ImGui;
import jade.Window;
import scenes.Scene;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** UI-thread-owned world; the worker returns immutable results for the UI thread to commit. */
public final class FactorySession implements AutoCloseable {
    private final SchemaRegistry schemas = new SchemaRegistry();
    private final FactoryWorld world = new FactoryWorld(schemas);
    private final FactorySceneAdapter sceneAdapter = new FactorySceneAdapter();
    private final Path workspace;
    private final WorldCheckpointStore checkpoints;
    private final ExecutorService worker = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1), task -> {
        Thread thread = new Thread(task, "fraynix-factory-inspection"); thread.setDaemon(true); return thread;
    }, new ThreadPoolExecutor.AbortPolicy());
    private CompletableFuture<FactoryKernelService.Receipt> active;
    private UUID activeEntity;
    private String status = "Ready. Save a scene checkpoint, then run a Fraynix inspection.";
    private String trace = "";
    private String roundTrip = "";
    private long lastCaptureNanos;
    private CompletableFuture<FactoryKernelService.Monitor> monitorFuture;
    private FactoryKernelService.Monitor monitor;
    private long lastMonitorNanos;
    private String memoryQuery = "", monitorError = "";
    private CompletableFuture<LocalSkillDispatcher.Result> skillFuture;
    private LocalSkillDispatcher.Result skillResult;
    private String skillError = "";

    public FactorySession(Path workspace) {
        this.workspace = workspace.toAbsolutePath().normalize();
        this.checkpoints = new WorldCheckpointStore(this.workspace.resolve("scene.fworld"));
        FactorySceneAdapter.registerSchemas(schemas);
        schemas.register(new ComponentSchema("factory.task", 1, Map.of("status", ComponentSchema.Kind.TEXT, "trace", ComponentSchema.Kind.TEXT), Set.of("status", "trace")));
    }

    public FactoryWorld world() { return world; }
    public SchemaRegistry schemas() { return schemas; }
    public SceneKnowledge observe(Scene scene, UUID selected, boolean playing) {
        capture(scene); return SceneKnowledge.capture(scene, world.snapshot(), selected, playing);
    }
    public SceneKnowledge observe(Scene scene, UUID selected, boolean playing, int spriteCount) {
        capture(scene); return SceneKnowledge.capture(scene, world.snapshot(), selected, playing, spriteCount);
    }
    /** Commit an authored scene while retaining current non-scene task/data entities. */
    public void commitScene(Scene current, WorldSnapshot authored, String expectedHash, java.util.function.Consumer<Scene> installer) throws IOException {
        if (active != null) throw new IllegalStateException("Wait for the scene inspection to finish");
        capture(current); WorldSnapshot before = world.snapshot();
        if (authored.entities().stream().anyMatch(entity -> !SceneKnowledge.authored(entity))) throw new IllegalArgumentException("Scene commits require authored entities");
        if (!SceneKnowledge.fingerprint(before).equals(expectedHash)) throw new ConcurrentModificationException("The scene changed after observation; request a fresh preview");
        var entities = new ArrayList<>(before.entities().stream().filter(e -> !SceneKnowledge.authored(e)).toList()); entities.addAll(authored.entities());
        var candidate = new WorldSnapshot(Math.addExact(before.revision(), 1), entities); FactoryWorld.validate(candidate, schemas);
        var staged = new FactoryWorld(schemas); staged.restore(candidate); var replacement = new Scene(new scenes.LevelEditorSceneInitializer()); sceneAdapter.restore(staged, replacement);
        checkpoints.save(candidate, schemas);
        try { installer.accept(replacement); }
        catch (RuntimeException | Error failure) {
            try { checkpoints.save(before, schemas); } catch (IOException rollback) { failure.addSuppressed(rollback); }
            throw failure;
        }
        world.replace(before.revision(), entities); lastCaptureNanos = 0;
        status = "Applied and saved scene revision " + world.snapshot().revision();
    }
    public Path workspace() { return workspace; }
    public FactoryKernelService.Monitor monitor() { return monitor; }
    public String monitorError() { return monitorError; }
    public boolean isMonitoring() { return monitorFuture != null; }
    public void requestMonitor(String query) {
        memoryQuery = query;
        if (monitorFuture != null || active != null || skillFuture != null) return;
        lastMonitorNanos = System.nanoTime();
        monitorFuture = CompletableFuture.supplyAsync(() -> {
            try (var service = new FactoryKernelService(workspace)) { return service.monitor(query); }
            catch (IOException error) { throw new CompletionException(error); }
        }, worker);
    }
    public boolean isRunning() { return active != null || skillFuture != null; }
    public LocalSkillDispatcher.Result skillResult() { return skillResult; }
    public String skillError() { return skillError; }
    public void requestSkill(Scene scene, String id, Map<String, String> input) {
        if (isRunning() || isMonitoring()) throw new IllegalStateException("Wait for the current kernel operation");
        capture(scene); WorldSnapshot snapshot = world.snapshot(); Map<String, String> fields = Map.copyOf(input);
        skillError = ""; skillResult = null;
        // This host policy authorizes these four local capabilities for explicit operator tool calls.
        var grants = EnumSet.allOf(LocalSkillDispatcher.Capability.class);
        skillFuture = CompletableFuture.supplyAsync(() -> {
            try (var service = new FactoryKernelService(workspace)) { return service.invokeSkill(id, fields, grants, snapshot); }
            catch (IOException error) { throw new CompletionException(error); }
        }, worker);
    }
    public String status() { return status; }
    public boolean hasCheckpoint() {
        return Files.isRegularFile(checkpoints.path()) || Files.isRegularFile(checkpoints.path().resolveSibling(checkpoints.path().getFileName() + ".previous"));
    }
    public void reportFailure(Exception error) { status = error.getClass().getSimpleName() + ": " + error.getMessage(); }
    public void capture(Scene scene) { sceneAdapter.capture(scene, world); }

    public void save(Scene scene) throws IOException {
        capture(scene); checkpoints.save(world.snapshot(), schemas);
        status = "Saved revision " + world.snapshot().revision() + " to " + checkpoints.path();
    }

    public Scene restore() throws IOException {
        if (active != null) throw new IllegalStateException("Wait for the running inspection before restoring");
        var loaded = checkpoints.load(schemas);
        FactoryWorld staged = new FactoryWorld(schemas); staged.restore(loaded.snapshot());
        var recoveredEntities = new ArrayList<EntityRecord>();
        boolean interrupted = false;
        for (EntityRecord entity : staged.snapshot().entities()) {
            var components = new ArrayList<ComponentRecord>();
            for (ComponentRecord component : entity.components()) {
                if (component.type().equals("factory.task") && component.values().get("status").equals("running")) {
                    var values = new HashMap<>(component.values()); values.put("status", "interrupted");
                    component = new ComponentRecord(component.id(), component.type(), component.version(), values); interrupted = true;
                }
                components.add(component);
            }
            recoveredEntities.add(new EntityRecord(entity.id(), entity.name(), components));
        }
        if (interrupted) staged.replace(staged.snapshot().revision(), recoveredEntities);
        Scene scene = new Scene(new scenes.LevelEditorSceneInitializer());
        try { sceneAdapter.restore(staged, scene); }
        catch (RuntimeException error) { throw new IOException("Scene checkpoint could not be deserialized; the current scene was kept", error); }
        world.restore(staged.snapshot());
        status = "Restored revision " + world.snapshot().revision() + (loaded.recoveredPrevious() ? " from previous valid generation" : "")
            + (interrupted ? " | interrupted inspection recorded; rerun it explicitly" : "");
        return scene;
    }

    public void inspect(Scene scene) throws IOException {
        if (isRunning()) throw new IllegalStateException("A kernel operation is already running");
        roundTrip = sceneAdapter.verifyRoundTrip(scene, schemas);
        save(scene);
        activeEntity = world.snapshot().entities().stream().filter(FactorySession::isInspectionStatus)
            .map(EntityRecord::id).findFirst().orElseGet(UUID::randomUUID);
        updateTask("running", "");
        checkpoints.save(world.snapshot(), schemas);
        Path artifact = workspace.resolve("scene-summary.txt");
        StringBuilder text = new StringBuilder(roundTrip + "\nFactory scene revision=" + world.snapshot().revision() + "\n");
        for (EntityRecord entity : world.snapshot().entities()) text.append(entity.id()).append(' ').append(entity.name()).append(' ').append(entity.components().stream().map(ComponentRecord::type).toList()).append('\n');
        Files.writeString(artifact, text);
        status = "Fraynix inspection running in background...";
        active = CompletableFuture.supplyAsync(() -> {
            try (FactoryKernelService service = new FactoryKernelService(workspace)) { return service.inspect(artifact); }
            catch (IOException error) { throw new CompletionException(error); }
        }, worker);
    }

    public void poll() {
        if (skillFuture != null && skillFuture.isDone()) {
            try { skillResult = skillFuture.join(); }
            catch (CompletionException error) { skillError = "Tool: " + error.getCause().getMessage(); }
            skillFuture = null; lastMonitorNanos = 0;
        }
        if (monitorFuture != null && monitorFuture.isDone()) {
            try { monitor = monitorFuture.join(); monitorError = ""; }
            catch (CompletionException error) { monitorError = "Kernel: " + error.getCause().getMessage(); }
            monitorFuture = null;
        }
        if (monitorFuture == null && active == null && skillFuture == null && System.nanoTime() - lastMonitorNanos > 10_000_000_000L) requestMonitor(memoryQuery);
        if (active == null || !active.isDone()) return;
        try {
            var receipt = active.join(); trace = receipt.tracePath();
            status = roundTrip + " | artifact " + receipt.outcome();
            updateTask(receipt.passed() ? "validated" : "failed", trace);
        } catch (CompletionException error) {
            status = "Inspection failed: " + error.getCause(); updateTask("failed", "");
        } finally {
            active = null;
            lastMonitorNanos = 0;
            try { checkpoints.save(world.snapshot(), schemas); }
            catch (IOException error) { status += " | checkpoint failed: " + error.getMessage(); }
        }
    }

    private void updateTask(String state, String tracePath) {
        var entities = new ArrayList<>(world.snapshot().entities());
        EntityRecord old = entities.stream().filter(e -> e.id().equals(activeEntity)).findFirst().orElse(null);
        entities.removeIf(e -> e.id().equals(activeEntity) || isInspectionStatus(e));
        UUID componentId = old == null ? UUID.randomUUID() : old.components().get(0).id();
        entities.add(new EntityRecord(activeEntity, "Fraynix scene inspection", List.of(new ComponentRecord(componentId, "factory.task", 1, Map.of("status", state, "trace", tracePath == null ? "" : tracePath)))));
        world.replace(world.snapshot().revision(), entities);
    }

    private static boolean isInspectionStatus(EntityRecord entity) {
        return entity.name().equals("Fraynix scene inspection") && entity.components().stream().anyMatch(c -> c.type().equals("factory.task"));
    }

    public void imgui(Scene scene) {
        tick(scene);
        ImGui.begin("Fraynix Factory");
        drawControls(scene);
        ImGui.end();
    }
    public void tick(Scene scene) {
        poll();
        if (System.nanoTime() - lastCaptureNanos > 250_000_000L) {
            execute(() -> capture(scene));
            lastCaptureNanos = System.nanoTime();
        }
    }
    public void drawControls(Scene scene) {
        long sceneObjects = scene.getGameObjects().stream().filter(o -> o.doSerialization() && !o.isDead()).count();
        ImGui.text("Scene objects " + sceneObjects + " | JBox2D bodies " + scene.physics().bodyCount());
        ImGui.textWrapped("Checks that objects and physics settings survive saving and restoring. Run history is kept in receipts.");
        if (ImGui.button("Save checkpoint")) execute(() -> save(scene));
        ImGui.sameLine();
        if (ImGui.button("Restore checkpoint")) execute(() -> Window.installFactoryScene(restore()));
        if (!isRunning() && ImGui.button("Verify save / restore")) execute(() -> inspect(scene));
        ImGui.textWrapped(status);
        if (!trace.isEmpty()) ImGui.textWrapped("Trace: " + trace);
    }
    private void execute(CheckedAction action) {
        try { action.run(); } catch (Exception error) { status = error.getClass().getSimpleName() + ": " + error.getMessage(); }
    }
    private interface CheckedAction { void run() throws Exception; }
    @Override public void close() { worker.shutdownNow(); }
}
