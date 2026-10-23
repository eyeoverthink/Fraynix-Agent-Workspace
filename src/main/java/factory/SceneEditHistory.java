package factory;

import com.google.gson.*;
import fraymus.factory.*;
import scenes.Scene;
import java.io.IOException;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

/** Durable proposal receipts and guarded undo across native process restarts. */
public final class SceneEditHistory {
    public record Prepared(SceneProposal.Draft draft, SceneSimulation.Result simulation, Path directory) {}
    private record Entry(UUID id, String beforeHash, String afterHash) {}
    private final FactorySession session;
    private final Path root, stackFile;
    private final List<Entry> stack = new ArrayList<>();
    public SceneEditHistory(FactorySession session) throws IOException {
        this.session = session; root = session.workspace().resolve("scene-edits"); stackFile = root.resolve("undo-stack.json");
        if (Files.isRegularFile(stackFile)) {
            var data = SceneJson.parse(Files.readString(stackFile));
            for (var item : data.getAsJsonArray("entries")) {
                var entry = item.getAsJsonObject(); stack.add(new Entry(UUID.fromString(entry.get("id").getAsString()), entry.get("beforeHash").getAsString(), entry.get("afterHash").getAsString()));
            }
        }
    }
    public Prepared prepare(SceneProposal.Draft draft, SceneSimulation.Result simulation) throws IOException {
        Path directory = root.resolve(draft.id().toString()); Files.createDirectories(directory);
        new WorldCheckpointStore(directory.resolve("before.fworld")).save(draft.before(), session.schemas());
        new WorldCheckpointStore(directory.resolve("candidate.fworld")).save(draft.candidate(), session.schemas());
        atomic(directory.resolve("proposal.json"), draft.source());
        var receipt = new JsonObject(); receipt.addProperty("id", draft.id().toString()); receipt.addProperty("summary", draft.summary());
        receipt.addProperty("beforeHash", draft.baseHash()); receipt.addProperty("afterHash", SceneKnowledge.fingerprint(draft.candidate()));
        receipt.addProperty("validatedAt", Instant.now().toString()); receipt.add("changes", SceneJson.GSON.toJsonTree(draft.changes()));
        receipt.add("simulation", SceneJson.GSON.toJsonTree(simulation)); atomic(directory.resolve("validation.json"), receipt.toString());
        return new Prepared(draft, simulation, directory);
    }
    public void apply(Prepared prepared, Scene current, Consumer<Scene> installer) throws IOException {
        var draft = prepared.draft(); session.capture(current);
        if (!SceneKnowledge.fingerprint(session.world().snapshot()).equals(draft.baseHash())) throw new ConcurrentModificationException("The scene changed; regenerate or revalidate against a new observation");
        if (stack.stream().anyMatch(entry -> entry.id().equals(draft.id()))) throw new IllegalStateException("This proposal was already applied");
        // Persist the prepared undo identity before the scene checkpoint. If interrupted, the scene hash reconciles it on restart.
        var entry = new Entry(draft.id(), draft.baseHash(), SceneKnowledge.fingerprint(draft.candidate())); stack.add(entry);
        try { saveStack(); session.commitScene(current, draft.candidate(), draft.baseHash(), installer); }
        catch (IOException | RuntimeException | Error error) {
            stack.remove(stack.size() - 1); try { saveStack(); } catch (IOException rollback) { error.addSuppressed(rollback); } throw error;
        }
    }
    public boolean canUndo(String currentHash) { return !stack.isEmpty() && stack.get(stack.size() - 1).afterHash().equals(currentHash); }
    public int size() { return stack.size(); }
    public void reconcile(String currentHash) throws IOException {
        boolean changed = false;
        while (!stack.isEmpty() && stack.get(stack.size() - 1).beforeHash().equals(currentHash)) { stack.remove(stack.size() - 1); changed = true; }
        if (changed) saveStack();
    }
    public void undo(Scene current, Consumer<Scene> installer) throws IOException {
        session.capture(current); String hash = SceneKnowledge.fingerprint(session.world().snapshot());
        reconcile(hash);
        if (!canUndo(hash)) throw new ConcurrentModificationException("Undo would overwrite later scene edits; the current scene was kept");
        var entry = stack.get(stack.size() - 1);
        var before = new WorldCheckpointStore(root.resolve(entry.id().toString()).resolve("before.fworld")).load(session.schemas()).snapshot();
        if (!SceneKnowledge.fingerprint(before).equals(entry.beforeHash())) throw new IOException("Undo receipt does not match its saved world");
        session.commitScene(current, before, hash, installer); stack.remove(stack.size() - 1); saveStack();
    }
    private void saveStack() throws IOException {
        var data = new JsonObject(); data.add("entries", SceneJson.GSON.toJsonTree(stack)); atomic(stackFile, data.toString());
    }
    public static void atomic(Path path, String text) throws IOException {
        Files.createDirectories(path.toAbsolutePath().getParent()); Path temporary = Files.createTempFile(path.toAbsolutePath().getParent(), "scene-edit-", ".pending");
        try {
            try (var channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                ByteBuffer data = StandardCharsets.UTF_8.encode(text); while (data.hasRemaining()) channel.write(data); channel.force(true);
            }
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary); }
    }
}
