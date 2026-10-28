package editor;

import factory.*;
import fraymus.factory.models.FactoryModelService;
import fraymus.factory.SpatialIndex2D;
import imgui.ImGui;
import imgui.type.*;
import jade.Window;
import org.joml.*;
import renderer.DebugDraw;
import scenes.Scene;
import util.AssetPool;
import java.util.*;

/** Creator-facing controls for complete observations, spatial tools, proposals and validated edits. */
public final class SceneAgentPanel implements AutoCloseable {
    private final FactorySession session;
    private final FraynixWorkspace workspace;
    private SceneAgent agent;
    private SceneKnowledge observation;
    private Scene observedScene;
    private long observedAt;
    private final ImString goal = new ImString("Create a static platform and a dynamic tile\nabove it in the current view.", 16384);
    private final ImString query = new ImString("{\"op\":\"nearest\",\"x\":0,\"y\":0,\"radius\":10,\"limit\":8}", 4096);
    private final ImString proposalJson = new ImString(131072);
    private final ImInt turns = new ImInt(4), context = new ImInt(16384);
    private final float[] seconds = {2};
    private String error = "", queryResult = "", lastSource = "";
    private boolean overlay = true;
    private boolean observeEnabled;
    private boolean automaticApply=true;
    public SceneAgentPanel(FactorySession session, FraynixWorkspace workspace) {
        this.session = session; this.workspace = workspace;
        try { agent = new SceneAgent(session, index -> AssetPool.getSpritesheet("assets/images/spritesheets/decorationsAndBlocks.png").getSprite(index)); }
        catch (java.io.IOException failure) { error = "Scene edit history could not be opened: " + failure.getMessage(); }
    }
    public SceneAgent agent() { return agent; }
    public boolean busy() { return agent != null && agent.busy(); }
    public SceneKnowledge capture(Scene scene) {
        var selected = Window.getImguiLayer().getPropertiesWindow().getActiveGameObject();
        observation = session.observe(scene, selected == null ? null : selected.getPersistentId(), Window.isPlaying(),
            AssetPool.getSpritesheet("assets/images/spritesheets/decorationsAndBlocks.png").size()); observedScene = scene; observedAt = System.nanoTime();
        return observation;
    }
    public void tick(Scene scene) {
        if (agent != null) agent.tick();
        if (!Window.isPlaying() && automaticApply && agent!=null && !agent.busy() && agent.prepared()!=null) {
            try { agent.apply(Window.getScene(),Window::installFactoryScene); observedScene=null; }
            catch (java.io.IOException | RuntimeException failure) { error=failure.getMessage(); automaticApply=false; }
            return;
        }
        if (Window.isPlaying() || !observeEnabled) { observeEnabled=false; return; }
        observeEnabled=false;
        if (observation == null || observedScene != scene || System.nanoTime() - observedAt > 500_000_000L) {
            try { capture(scene); if (agent != null) agent.reconcile(observation.fingerprint()); }
            catch (RuntimeException failure) { observation = null; error = failure.getMessage(); }
        }
        if (agent != null && !agent.source().equals(lastSource)) { lastSource = agent.source(); proposalJson.set(lastSource); }
    }
    public void imgui(Scene scene) {
        observeEnabled=true;
        if (Window.isPlaying()) { ImGui.textWrapped("Stop Play to edit the authoring scene. Live AI controls are in the viewport."); return; }
        ImGui.textWrapped("Fraynix can inspect components, query space and propose scene changes. Preview runs real physics on a detached copy.");
        ImGui.beginDisabled(workspace.busy() || agent != null && agent.busy());
        ImGui.setNextItemWidth(-1);
        if (ImGui.beginCombo("##CreatorModel", workspace.selectedModel().isEmpty() ? "Select a local model" : workspace.selectedModel())) {
            for (var model : workspace.snapshot().models()) if (model.available()
                && ImGui.selectable(model.name(), workspace.selectedModel().equals(model.name()))) workspace.select(model.name());
            ImGui.endCombo();
        }
        ImGui.endDisabled();
        if (observation != null) {
            ImGui.text(observation.spatial().items().size() + " entities observed | " + observation.json().length() + " characters");
            ImGui.textDisabled("Snapshot " + observation.fingerprint().substring(0, 16));
        }
        if (agent == null) { ImGui.textWrapped(error); return; }
        if (ImGui.checkbox("Apply validated edits automatically",automaticApply)) automaticApply=!automaticApply;
        ImGui.inputTextMultiline("##SceneGoal", goal, -1, 85);
        ImGui.beginDisabled(agent.busy() || workspace.busy() || Window.isPlaying());
        if (ImGui.button("Ask Fraynix for a proposal")) run(() -> {
            var model = workspace.snapshot().models().stream().filter(m -> m.available() && m.name().equals(workspace.selectedModel())).findFirst()
                .orElseThrow(() -> new IllegalStateException("Select an online model in Chat or Models"));
            agent.request(goal.get(), capture(scene), model, context.get(), turns.get(), seconds[0]);
        });
        ImGui.endDisabled();
        if (agent.busy() && ImGui.button("Stop after current response")) agent.stop();
        if (ImGui.collapsingHeader("Request settings")) {
            ImGui.inputInt("Context tokens", context); ImGui.inputInt("Maximum model turns", turns);
            ImGui.dragFloat("Preview seconds", seconds, .25f, .25f, 60f);
            ImGui.textWrapped("Larger contexts use more local memory. More turns allow the model to query the scene before proposing.");
        }
        ImGui.textWrapped(agent.progress());
        if (!agent.error().isBlank()) ImGui.textWrapped(agent.error());
        if (!agent.answer().isBlank()) ImGui.textWrapped(agent.answer());
        if (!error.isBlank()) ImGui.textWrapped(error);
        var prepared = agent.prepared();
        if (prepared != null) {
            ImGui.separator(); ImGui.textWrapped(prepared.draft().summary());
            ImGui.beginChild("ProposedChanges", 0, 150, true);
            for (String change : prepared.draft().changes()) ImGui.textWrapped(change);
            ImGui.endChild();
            var simulation = prepared.simulation();
            ImGui.textWrapped(String.format(Locale.ROOT, "Validated %.2fs | %d steps | %d bodies | %d peak contact pairs", simulation.seconds(), simulation.steps(), simulation.bodies(), simulation.peakContacts()));
            boolean stale = observation == null || !prepared.draft().baseHash().equals(observation.fingerprint());
            if (stale) ImGui.textWrapped("The scene has changed. Request a new proposal before applying this one.");
            if (ImGui.checkbox("Show preview bounds and trajectories", overlay)) overlay = !overlay;
            ImGui.textDisabled("Green: proposed | blue: simulated | red: removed");
            ImGui.beginDisabled(stale || agent.busy() || session.isRunning() || Window.isPlaying());
            if (ImGui.button("Apply and save proposal")) run(() -> { agent.apply(Window.getScene(), Window::installFactoryScene); observation = null; });
            ImGui.endDisabled();
            ImGui.textWrapped("Receipt: " + prepared.directory());
        }
        ImGui.beginDisabled(observation == null || !agent.canUndo(observation.fingerprint()) || agent.busy() || session.isRunning() || Window.isPlaying());
        if (ImGui.button("Undo last AI edit")) run(() -> { agent.undo(Window.getScene(), Window::installFactoryScene); observation = null; });
        ImGui.endDisabled();
        if (ImGui.collapsingHeader("Spatial queries")) {
            ImGui.inputTextMultiline("##SpatialQuery", query, -1, 75);
            if (ImGui.button("Run read-only query")) run(() -> queryResult = capture(Window.getScene()).query(query.get()));
            if (!queryResult.isBlank()) ImGui.textWrapped(queryResult);
            ImGui.textWrapped("Queries: inspect, components, nearest, overlap and relative. Bounds use broad-phase AABBs.");
        }
        if (ImGui.collapsingHeader("Complete scene observation")) {
            if (ImGui.button("Refresh observation")) run(() -> capture(Window.getScene()));
            if (observation != null) {
                if (ImGui.button("Copy observation JSON")) ImGui.setClipboardText(observation.json());
                ImGui.beginChild("CompleteSceneData", 0, 260, true); ImGui.textWrapped(observation.json()); ImGui.endChild();
            }
        }
        if (ImGui.collapsingHeader("Proposal JSON")) {
            ImGui.inputTextMultiline("##ProposedSceneJson", proposalJson, -1, 180);
            ImGui.beginDisabled(agent.busy() || Window.isPlaying());
            if (ImGui.button("Validate JSON preview")) run(() -> agent.preview(proposalJson.get(), capture(Window.getScene()), seconds[0]));
            ImGui.endDisabled();
        }
    }
    public void drawPreview() {
        if (!overlay || agent == null || agent.prepared() == null) return;
        var prepared = agent.prepared(); var simulation = prepared.simulation();
        for (UUID id : prepared.draft().affected()) {
            if (simulation.initialBounds().containsKey(id)) rectangle(simulation.initialBounds().get(id), new Vector3f(.15f, .8f, .35f));
            if (simulation.finalBounds().containsKey(id)) rectangle(simulation.finalBounds().get(id), new Vector3f(.2f, .5f, 1));
            if (!simulation.initialBounds().containsKey(id)) prepared.draft().before().entities().stream().filter(e -> e.id().equals(id)).findFirst()
                .ifPresent(entity -> rectangle(SceneKnowledge.bounds(entity), new Vector3f(1, .2f, .2f)));
        }
        simulation.trajectories().values().forEach(path -> {
            for (int i = 1; i < path.size(); i++) DebugDraw.addLine2D(new Vector2f((float) path.get(i-1).x(), (float) path.get(i-1).y()),
                new Vector2f((float) path.get(i).x(), (float) path.get(i).y()), new Vector3f(.2f, .5f, 1));
        });
    }
    private static void rectangle(SpatialIndex2D.Bounds bounds, Vector3f color) {
        DebugDraw.addBox2D(new Vector2f((float) ((bounds.minX()+bounds.maxX())/2), (float) ((bounds.minY()+bounds.maxY())/2)),
            new Vector2f((float) (bounds.maxX()-bounds.minX()), (float) (bounds.maxY()-bounds.minY())), 0, color);
    }
    private void run(Action action) { try { action.run(); error = ""; } catch (Exception failure) { error = String.valueOf(failure.getMessage()); } }
    private interface Action { void run() throws Exception; }
    @Override public void close() { if (agent != null) agent.close(); }
}
