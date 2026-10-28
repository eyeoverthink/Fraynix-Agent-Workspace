package editor;

import agent.GameInput;
import components.PlayerController;
import jade.Window;
import observers.events.*;
import java.nio.file.*;

/** Native end-to-end diagnostic: original content, controls, physics, render, and stop isolation. */
public final class MarioNativeSmoke {
    private int frames, phase;
    private float initialX, initialY, highestY;
    private long startTick;
    private String authored;
    private java.util.Map<Integer,Float> enemyPositions;
    private boolean jumped;
    private final Path capture;
    public MarioNativeSmoke(Path capture) { this.capture=capture; }
    public boolean tick() throws Exception {
        var scene=Window.getScene();
        if (phase==0) {
            if (++frames<5) return false;
            if (scene.getGameObjects().stream().filter(jade.GameObject::doSerialization).count()!=1248) throw new IllegalStateException("Full level missing");
            authored=scene.serializeAuthoring();
            Window.get().onNotify(null,new Event(EventType.GameEngineStartPlay));
            var player=Window.getScene().getGameObjectWith(PlayerController.class);
            initialX=player.transform.position.x; initialY=highestY=player.transform.position.y;
            GameInput.start(Window.getScene(),initialX+2.5f);
            startTick=Window.getScene().tick(); phase=1; return false;
        }
        if (phase==1) {
            if (enemyPositions==null) {
                enemyPositions=new java.util.HashMap<>();
                for(var object:scene.getGameObjects()) if(object.getComponent(components.GoombaAI.class)!=null) enemyPositions.put(object.getUid(),object.transform.position.x);
            }
            var player=scene.getGameObjectWith(PlayerController.class);
            highestY=Math.max(highestY,player.transform.position.y);
            long elapsed=scene.tick()-startTick;
            if (!jumped && elapsed>30 && player.getComponent(PlayerController.class).onGround) { GameInput.requestJump(); jumped=true; }
            if (elapsed<90) return false;
            float moved=player.transform.position.x-initialX;
            if (moved<.5f) throw new IllegalStateException("Live player did not move: "+moved);
            if (!jumped || highestY<initialY+.2f) throw new IllegalStateException("Live player did not jump");
            boolean enemyMoved=scene.getGameObjects().stream().anyMatch(o->enemyPositions.containsKey(o.getUid()) && Math.abs(o.transform.position.x-enemyPositions.get(o.getUid()))>.01f);
            if (!enemyMoved) throw new IllegalStateException("Enemy behavior did not advance");
            var brain=nervous.BrainRuntime.core();
            if (brain==null || brain.ticks()==0) throw new IllegalStateException("Live sensory cortex did not advance");
            StudioFrameCapture.save(capture,Window.getWidth(),Window.getHeight());
            System.out.println("MARIO_NATIVE_PLAY_PASSED moved="+moved+" highestY="+highestY+" initialY="+initialY+" ticks="+elapsed+" bodies="+scene.physics().bodyCount());
            System.out.println("MARIO_NATIVE_BRAIN_PASSED neurons="+brain.neurons()+" synapses="+brain.synapses()+" ticks="+brain.ticks()+" enemyMoved="+enemyMoved+" cortexMs="+nervous.BrainRuntime.milliseconds());
            Window.get().onNotify(null,new Event(EventType.GameEngineStopPlay));
            if (!authored.equals(Window.getScene().serializeAuthoring())) throw new IllegalStateException("Play changed authoring state");
            phase=2; frames=0; return false;
        }
        if (++frames<3) return false;
        StudioFrameCapture.save(capture.resolveSibling("mario-edit-restored.png"),Window.getWidth(),Window.getHeight());
        System.out.println("MARIO_NATIVE_STOP_PASSED authoringUnchanged=true"); return true;
    }
}
