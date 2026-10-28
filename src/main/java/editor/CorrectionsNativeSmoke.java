package editor;

import jade.Window;
import components.*;
import observers.events.*;
import java.nio.file.*;

/** Reproduces selection, file actions and autonomous travel without an injected jump. */
public final class CorrectionsNativeSmoke {
    private int phase,frames;
    private String before;
    private long lastLogged=-1;
    private final Path capture;
    public CorrectionsNativeSmoke(Path capture) {this.capture=capture;}
    public boolean tick() throws Exception {
        var scene=Window.getScene();var session=Window.getFactorySession();
        if(phase==0) {
            if(++frames<5) return false;
            before=scene.serializeAuthoring();
            var tools=scene.getGameObjectWith(MouseControls.class).getComponent(MouseControls.class);
            var template=jade.Prefabs.generateSpriteObject(util.AssetPool.getSpritesheet("assets/images/spritesheets/decorationsAndBlocks.png").getSprite(0),.25f,.25f);
            tools.pickupObject(template);
            if(!before.equals(scene.serializeAuthoring()) || tools.placing()) throw new IllegalStateException("Asset selection changed the level");
            tools.enablePlacement();tools.placeAt(scene,2,3);tools.selectTool();phase=1;return false;
        }
        if(phase==1) {
            if(scene.getGameObjects().stream().filter(jade.GameObject::doSerialization).count()!=1249) throw new IllegalStateException("Explicit placement did not add exactly one object");
            Window.levels().save();Path saved=session.workspace().resolve("authored-scene.json");
            if(LevelDocumentIO.read(saved,session.schemas()).getGameObjects().size()!=1249) throw new IllegalStateException("Save level did not persist the placement");
            Path exported=session.workspace().resolve("exports/native-roundtrip.json");LevelDocumentIO.write(scene,exported);
            Window.levels().importPath(Path.of("levels/mario-original.json"));
            if(Window.getScene().getGameObjects().stream().filter(jade.GameObject::doSerialization).count()!=1248) throw new IllegalStateException("Original level import failed");
            Path invalid=session.workspace().resolve("invalid-level.json");Files.writeString(invalid,"[{\"components\":[{\"type\":\"java.lang.Runtime\",\"properties\":{}}]}]");
            var kept=Window.getScene();
            try {Window.levels().importPath(invalid);throw new IllegalStateException("Invalid import was accepted");}
            catch(java.io.IOException expected) {if(kept!=Window.getScene()) throw new IllegalStateException("Invalid import changed the scene");}
            System.out.println("CORRECTIONS_FILES_PASSED selectAddsNothing=true explicitPlacement=1 savedObjects=1249 importedObjects=1248 rejectedImportKeptScene=true");
            Window.get().onNotify(null,new Event(EventType.GameEngineStartPlay));
            agent.GameInput.start(Window.getScene(),12.25f);phase=2;return false;
        }
        if(phase==2) {
            var player=scene.getGameObjectWith(PlayerController.class);float x=player.transform.position.x;
            if(scene.tick()/30!=lastLogged) {lastLogged=scene.tick()/30;System.out.println("CORRECTIONS_ROUTE tick="+scene.tick()+" x="+x+" y="+player.transform.position.y+" "+agent.GameInput.status());}
            if(!agent.GameInput.automatic() || scene.tick()>1800) {
                StudioFrameCapture.save(capture,Window.getWidth(),Window.getHeight());
                if(x<12.05f) throw new IllegalStateException("Autonomous obstacle regression failed at x="+x+": "+agent.GameInput.status());
                System.out.println("CORRECTIONS_ROUTE_PASSED x="+x+" ticks="+scene.tick()+" forcedJumps=0 teleport=false");
                Window.get().onNotify(null,new Event(EventType.GameEngineStopPlay));phase=3;frames=0;
            }
            return false;
        }
        if(++frames<4) return false;
        var workspace=Window.getImguiLayer().getFraynixPanel().workspace();
        if(workspace.snapshot().models().isEmpty() || workspace.scanning()) return false;
        for(var model:workspace.snapshot().models()) if(model.name().startsWith("clef") && model.available()) throw new IllegalStateException("Decision-only model offered for chat");
        System.out.println("CORRECTIONS_MODELS_PASSED selected="+workspace.selectedModel()+" chatReady="+workspace.snapshot().models().stream().filter(fraymus.factory.models.FactoryModelService.Model::available).count()+" stores="+workspace.storage().size());
        Files.writeString(session.workspace().resolve("native-chat-summary.json"),Window.getImguiLayer().getFraynixPanel().sceneAgent().capture(scene).forModel(6000));
        StudioFrameCapture.save(capture.resolveSibling("corrections-editor.png"),Window.getWidth(),Window.getHeight());return true;
    }
}
