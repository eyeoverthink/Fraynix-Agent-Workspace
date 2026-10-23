package editor;

import factory.*;
import jade.*;
import java.nio.file.*;
import java.util.*;

/** Opt-in native proof: real model -> validated preview -> saved scene, then undo on the next process. */
public final class SceneAgentNativeSmoke {
    private int phase, frames;
    private String beforeHash;
    private SceneKnowledge before;
    public boolean tick() throws Exception {
        var session = Window.getFactorySession(); var panel = Window.getImguiLayer().getFraynixPanel(); var creator = panel.sceneAgent(); var agent = creator.agent();
        if (agent == null) throw new IllegalStateException("Scene agent was not initialized");
        if (!agent.error().isBlank()) throw new IllegalStateException(agent.error());
        Path marker = session.workspace().resolve("native-scene-agent-proof.json");
        boolean undo = System.getProperty("fraymus.sceneAgentSmoke").equals("undo");
        if (undo) {
            if (phase == 0) {
                beforeHash = SceneJson.parse(Files.readString(marker)).get("beforeHash").getAsString();
                var observed = creator.capture(Window.getScene());
                if (!agent.canUndo(observed.fingerprint())) throw new IllegalStateException("No persisted undo matched the restored scene");
                agent.undo(Window.getScene(), Window::installFactoryScene); phase = 1; return false;
            }
            if (++frames < 4) return false;
            if (!creator.capture(Window.getScene()).fingerprint().equals(beforeHash)) throw new IllegalStateException("Native undo changed the original scene values or identities");
            Window.installFactoryScene(session.restore());
            if (!creator.capture(Window.getScene()).fingerprint().equals(beforeHash)) throw new IllegalStateException("Undo checkpoint did not restore exactly");
            System.out.println("SCENE_AGENT_NATIVE_UNDO_PASSED persistedUndo=true originalSceneHash=" + beforeHash + " bodies=" + Window.getScene().physics().bodyCount());
            return true;
        }
        if (phase == 0) {
            var model = panel.workspace().snapshot().models().stream().filter(m -> m.available()
                && m.name().equals(System.getProperty("fraymus.smokeModel", "fraynix-3b:latest"))).findFirst();
            if (model.isEmpty() || panel.workspace().busy()) return false;
            before = creator.capture(Window.getScene()); beforeHash = before.fingerprint();
            double x = Window.getScene().camera().position.x + 2.125, y = Window.getScene().camera().position.y + 1.125;
            String goal = "Create exactly ONE sprite entity named AI Playground Tile, with spriteIndex 0, x=" + x + ", y=" + y
                + ", width=0.25, height=0.25, rotation=0. Add no physics components. Do not change, duplicate, rename or delete any existing entity. Return a proposal.";
            agent.request(goal, before, model.get(), 8192, 4, 1); phase = 1; return false;
        }
        if (phase == 1) {
            if (!agent.busy() && agent.prepared() == null) throw new IllegalStateException("Local model returned no applicable proposal: " + agent.answer());
            if (agent.prepared() == null) return false;
            var draft = agent.prepared().draft();
            if (draft.candidate().entities().size() != before.spatial().items().size() + 1) throw new IllegalStateException("Model did not create exactly one entity");
            for (var original : before.world().entities().stream().filter(SceneKnowledge::authored).toList()) {
                if (!draft.candidate().entities().contains(original)) throw new IllegalStateException("Model changed an existing entity");
            }
            if (!draft.candidate().entities().stream().anyMatch(e -> e.name().equals("AI Playground Tile"))) throw new IllegalStateException("Model did not follow the requested entity name");
            phase = 2; return false;
        }
        if (phase == 2) {
            if (++frames < 4) return false;
            String capture = System.getProperty("fraymus.studioCapture", "");
            if (!capture.isBlank()) StudioFrameCapture.save(Path.of(capture), Window.getWidth(), Window.getHeight());
            var draft = agent.prepared().draft(); agent.apply(Window.getScene(), Window::installFactoryScene);
            SceneEditHistory.atomic(marker, SceneJson.GSON.toJson(Map.of("beforeHash", beforeHash, "afterHash", SceneKnowledge.fingerprint(draft.candidate()), "proposalId", draft.id().toString())));
            phase = 3; frames = 0; return false;
        }
        if (++frames < 4) return false;
        var after = creator.capture(Window.getScene());
        if (after.spatial().items().size() != before.spatial().items().size()+1) throw new IllegalStateException("Native scene installation lost a created entity");
        Window.installFactoryScene(session.restore());
        if (!creator.capture(Window.getScene()).fingerprint().equals(after.fingerprint())) throw new IllegalStateException("Applied native scene did not deserialize exactly");
        System.out.println("SCENE_AGENT_NATIVE_APPLY_PASSED realModel=true completeComponents=true preview=true savedRestore=true entityDelta=1 bodies=" + Window.getScene().physics().bodyCount());
        return true;
    }
}
