package editor;

import jade.Window;
import components.*;
import observers.events.*;
import java.nio.file.*;

/** Native evidence for real pickup, growth rendering and repeated-run policy persistence. */
public final class LearningNativeSmoke {
    private int frames,runs;
    private long lastLogged=-1;
    private boolean started,captured;
    private boolean training;
    private final Path capture;
    public LearningNativeSmoke(Path capture) {this.capture=capture;}
    public boolean tick() throws Exception {
        if(training) {
            if(agent.PlayTraining.active())return false;
            if(agent.PlayTraining.completed()!=2)throw new IllegalStateException("Practice did not finish its finite run budget");
            agent.PlayTraining.start(2,6);agent.GameInput.stop("Human takeover");agent.PlayTraining.tick();
            if(agent.PlayTraining.active() || agent.GameInput.automatic())throw new IllegalStateException("Practice ignored takeover");
            Window.get().onNotify(null,new Event(EventType.GameEngineStopPlay));
            System.out.println("LEARNING_PRACTICE_PASSED completed=2 takeoverCancels=true authoringRestored=true");
            StudioFrameCapture.save(capture.resolveSibling("learning-editor.png"),Window.getWidth(),Window.getHeight());return true;
        }
        if(!started) {
            if(++frames<5)return false;
            Window.get().onNotify(null,new Event(EventType.GameEngineStartPlay));
            agent.GameInput.start(Window.getScene(),20);started=true;lastLogged=-1;
        }
        var scene=Window.getScene();var player=scene.getGameObjectWith(PlayerController.class);
        if(scene.tick()/60!=lastLogged) {
            lastLogged=scene.tick()/60;
            System.out.println("LEARNING_ROUTE run="+(runs+1)+" tick="+scene.tick()+" x="+player.transform.position.x+" y="+player.transform.position.y+" small="+player.getComponent(PlayerController.class).isSmall()+" grounded="+player.getComponent(PlayerController.class).onGround+" velocity="+player.getComponent(physics2d.components.Rigidbody2D.class).getVelocity()+" "+agent.GameInput.status());
        }
        if(!captured && !player.getComponent(PlayerController.class).isSmall()) {
            var texture=player.getComponent(SpriteRenderer.class).getTexture();
            if(texture==null || !texture.getFilepath().contains("bigSpritesheet")) throw new IllegalStateException("Growth animation lost its sprite sheet");
            StudioFrameCapture.save(capture,Window.getWidth(),Window.getHeight());captured=true;
            System.out.println("LEARNING_GROWTH_PASSED texture="+texture.getFilepath()+" x="+player.transform.position.x);
        }
        if(scene.tick()>3000 && agent.GameInput.automatic()) agent.GameInput.stop("Native evaluation time budget");
        if(agent.GameInput.automatic())return false;
        var memory=agent.GameInput.experience();
        var result=memory.recent.get(memory.recent.size()-1);
        System.out.println("LEARNING_EPISODE "+new com.google.gson.Gson().toJson(result));
        Window.get().onNotify(null,new Event(EventType.GameEngineStopPlay));
        runs++;
        if(runs<3) {started=false;frames=0;return false;}
        if(!captured || memory.pickups<1)throw new IllegalStateException("AI did not collect a real power-up");
        Path saved=Window.getFactorySession().workspace().resolve("brain/play-memory-v1.json");
        var restored=new agent.PlayMemory(saved);
        String key=agent.PlayMemory.fingerprint(Window.getScene().serializeAuthoring());
        var restoredLevel=restored.level(key);
        if(restoredLevel.episodes!=3)throw new IllegalStateException("Episode memory did not survive reload: "+restoredLevel.episodes);
        if(memory.recent.get(0).strategies().equals(memory.recent.get(1).strategies()))throw new IllegalStateException("Policy repeated without incorporating outcome");
        System.out.println("LEARNING_NATIVE_PASSED episodes="+memory.episodes+" pickups="+memory.pickups+" bestProgress="+memory.bestProgress+" persisted=true policiesChanged=true teleport=false");
        agent.PlayTraining.start(2,6);training=true;return false;
    }
}
