package editor;

import factory.*;
import fraymus.factory.models.FactoryModelService;
import fraymus.model.ModelAdapter;
import com.google.gson.*;
import components.Sprite;
import scenes.Scene;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;

/** Observe/query/propose/validate loop. Applying a validated draft remains an explicit editor action. */
public final class SceneAgent implements AutoCloseable {
    private record Plan(String source, String answer, SceneEditHistory.Prepared prepared) {}
    private final FactorySession session;
    private final SceneEditHistory history;
    private final IntFunction<Sprite> sprites;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> { var thread = new Thread(task, "fraynix-scene-agent"); thread.setDaemon(true); return thread; });
    private CompletableFuture<Plan> planning;
    private CompletableFuture<SceneEditHistory.Prepared> validating;
    private SceneKnowledge requestedObservation;
    private SceneEditHistory.Prepared prepared;
    private final AtomicBoolean stop = new AtomicBoolean();
    private volatile String progress = "Ready to observe the scene and propose changes.";
    private String error = "", answer = "", source = "";
    private double seconds = 2;
    public SceneAgent(FactorySession session, IntFunction<Sprite> sprites) throws java.io.IOException {
        this.session = session; this.sprites = sprites; history = new SceneEditHistory(session);
    }
    public boolean busy() { return planning != null || validating != null; }
    public String progress() { return progress; }
    public String error() { return error; }
    public String answer() { return answer; }
    public String source() { return source; }
    public SceneEditHistory.Prepared prepared() { return prepared; }
    public boolean canUndo(String hash) { return history.canUndo(hash); }
    public void reconcile(String hash) { try { history.reconcile(hash); } catch (java.io.IOException failure) { error = failure.getMessage(); } }
    public void stop() { stop.set(true); progress = "Stopping after the current local response..."; }
    public void request(String goal, SceneKnowledge observation, FactoryModelService.Model model, int context, int turns, double simulationSeconds) {
        if (busy()) throw new IllegalStateException("Wait for the current scene request");
        if (goal.isBlank() || !model.available()) throw new IllegalArgumentException("Enter a goal and select an online local model");
        if (context < 2048 || turns < 1) throw new IllegalArgumentException("Choose a valid context window and model turn budget");
        requestedObservation = observation; seconds = simulationSeconds; prepared = null; error = ""; answer = ""; source = ""; stop.set(false);
        // Freeze sprite descriptions on the render thread. Worker-side proposal creation never accesses AssetPool or OpenGL.
        int count = SceneJson.parse(observation.json()).getAsJsonObject("assets").get("spriteCount").getAsInt();
        var spriteData = new ArrayList<String>(); for (int index = 0; index < count; index++) spriteData.add(SceneJson.GSON.toJson(sprites.apply(index)));
        var frozenSprites = List.copyOf(spriteData);
        UUID requestId = UUID.randomUUID(); Path log = session.workspace().resolve("scene-edits/requests").resolve(requestId.toString());
        planning = CompletableFuture.supplyAsync(() -> {
            try {
                Files.createDirectories(log); SceneEditHistory.atomic(log.resolve("observation.json"), observation.json());
                SceneEditHistory.atomic(log.resolve("request.json"), SceneJson.GSON.toJson(Map.of("goal", goal, "model", model.name(), "endpoint", model.endpoint(), "context", context, "turns", turns)));
                return plan(goal, observation, model, context, turns, log, index -> SceneJson.GSON.fromJson(frozenSprites.get(index), Sprite.class));
            } catch (java.io.IOException failure) { throw new CompletionException(failure); }
        }, worker);
    }
    private Plan plan(String goal, SceneKnowledge observation, FactoryModelService.Model model, int context, int turns, Path log, IntFunction<Sprite> frozenSprites) throws java.io.IOException {
        var messages = new ArrayList<ModelAdapter.Message>(); messages.add(ModelAdapter.Message.system(SceneProposal.instructions()));
        String input = "Goal: " + goal + "\nCurrent complete scene observation:\n" + observation.json();
        var service = new FactoryModelService();
        String lastValidation = "";
        for (int turn = 1; turn <= turns; turn++) {
            if (stop.get()) throw new CancellationException("Scene request stopped");
            progress = "Local model turn " + turn + " / " + turns + " using " + model.name();
            var reply = service.chat(model, input, messages, Map.of("response_format", "json", "num_ctx", context, "num_predict", 4096, "temperature", .1));
            var entry = Map.of("turn", turn, "at", Instant.now().toString(), "input", input, "model", reply.model(), "endpoint", reply.endpoint(),
                "reply", String.valueOf(reply.text()), "success", reply.success(), "millis", reply.millis());
            Files.writeString(log.resolve("turns.jsonl"), SceneJson.GSON.toJson(entry) + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            if (stop.get()) throw new CancellationException("Scene request stopped");
            if (!reply.success()) throw new IllegalStateException(reply.text());
            messages.add(ModelAdapter.Message.user(input)); messages.add(ModelAdapter.Message.assistant(reply.text()));
            try {
                var object = SceneJson.parse(reply.text()); String kind = SceneJson.text(object, "kind");
                if (kind.equals("proposal")) {
                    progress = "Validating model proposal in detached JBox2D...";
                    var draft = SceneProposal.stage(reply.text(), observation, session.schemas(), frozenSprites);
                    if (SceneKnowledge.fingerprint(draft.candidate()).equals(draft.baseHash())) throw new IllegalArgumentException("The proposal makes no authored changes");
                    var simulation = SceneSimulation.validate(draft, session.schemas(), seconds);
                    if (stop.get()) throw new CancellationException("Scene request stopped");
                    return new Plan(reply.text(), "", history.prepare(draft, simulation));
                }
                if (kind.equals("answer")) { SceneJson.keys(object, "kind", "summary"); return new Plan("", SceneJson.text(object, "summary"), null); }
                if (!kind.equals("query")) throw new IllegalArgumentException("kind must be proposal, query or answer");
                SceneJson.keys(object, "kind", "queries"); var results = new JsonArray();
                if (!object.has("queries") || !object.get("queries").isJsonArray() || object.getAsJsonArray("queries").size() == 0) throw new IllegalArgumentException("Query list is empty");
                for (var query : object.getAsJsonArray("queries")) {
                    try { results.add(JsonParser.parseString(observation.query(query.toString()))); }
                    catch (IllegalArgumentException failure) { var problem = new JsonObject(); problem.addProperty("error", failure.getMessage()); results.add(problem); }
                }
                input = "Engine query results for the same observation: " + results + ". Continue toward the goal. Return a proposal or grounded answer when ready.";
            } catch (IllegalArgumentException | ConcurrentModificationException failure) {
                lastValidation = failure.getMessage();
                input = "The engine rejected the response or preview: " + failure.getMessage() + ". No live changes were made. Correct the proposal using observationId "
                    + observation.fingerprint().substring(0, 16) + ". Return one valid JSON object using the specified protocol.";
            }
        }
        throw new IllegalStateException("Model turn budget reached. No live scene changes were made. Increase the budget or refine the goal. "
            + (lastValidation.isBlank() ? "" : "Last validation: " + lastValidation));
    }
    public void tick() {
        if (planning != null && planning.isDone()) {
            try {
                var result = planning.join(); planning = null;
                if (!result.answer().isBlank()) { answer = result.answer(); progress = "Grounded answer received; no edits requested."; }
                else { source = result.source(); prepared = result.prepared(); progress = "Preview validated. Review the changes and physics result before applying."; }
            } catch (RuntimeException failure) { planning = null; failed(failure); }
        }
        if (validating != null && validating.isDone()) {
            try { prepared = validating.join(); progress = "Preview validated. Review the changes and physics result before applying."; }
            catch (RuntimeException failure) { failed(failure); }
            validating = null;
        }
    }
    public void preview(String json, SceneKnowledge observation, double simulationSeconds) {
        if (busy()) throw new IllegalStateException("Wait for the current scene request");
        source = json; error = ""; answer = ""; prepared = null;
        var draft = SceneProposal.stage(json, observation, session.schemas(), sprites);
        if (SceneKnowledge.fingerprint(draft.candidate()).equals(draft.baseHash())) throw new IllegalArgumentException("The proposal makes no authored scene changes");
        progress = "Running detached JBox2D simulation and checkpoint validation...";
        validating = CompletableFuture.supplyAsync(() -> {
            var result = SceneSimulation.validate(draft, session.schemas(), simulationSeconds);
            try { return history.prepare(draft, result); } catch (java.io.IOException failure) { throw new CompletionException(failure); }
        }, worker);
    }
    public void apply(Scene current, Consumer<Scene> installer) throws java.io.IOException {
        if (busy() || prepared == null) throw new IllegalStateException("Validate a preview first");
        history.apply(prepared, current, installer); progress = "Applied and saved. The next observation will reflect the new scene."; prepared = null;
    }
    public void undo(Scene current, Consumer<Scene> installer) throws java.io.IOException {
        if (busy()) throw new IllegalStateException("Wait for the current scene request");
        history.undo(current, installer); prepared = null; progress = "Undid the last AI edit and saved the restored scene.";
    }
    private void failed(RuntimeException failure) {
        Throwable cause = failure instanceof CompletionException && failure.getCause() != null ? failure.getCause() : failure;
        error = String.valueOf(cause.getMessage()); progress = cause instanceof CancellationException ? "Stopped; the live scene was kept." : "Proposal was not applied.";
    }
    @Override public void close() { stop.set(true); worker.shutdownNow(); }
}
