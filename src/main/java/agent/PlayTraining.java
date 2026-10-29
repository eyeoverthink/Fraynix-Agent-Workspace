package agent;

import jade.Window;
import observers.events.*;

/** Finite repeated practice; every restart restores the authoring level and releases old leases. */
public final class PlayTraining {
    private static int remaining,completed;
    private static float target;
    private static String status="Practice idle";
    private PlayTraining() {}
    public static boolean active(){return remaining>0;}
    public static int completed(){return completed;}
    public static String status(){return status;}
    public static void cancel(){remaining=0;status="Practice stopped; human control";}
    public static void start(int runs,float targetX) {
        if(runs<1 || runs>10 || !Float.isFinite(targetX))throw new IllegalArgumentException("Practice needs 1 to 10 runs and a finite target");
        cancel();
        if(Window.isPlaying())Window.get().onNotify(null,new Event(EventType.GameEngineStopPlay));
        target=targetX;completed=0;remaining=runs;begin();
    }
    private static void begin() {
        Window.get().onNotify(null,new Event(EventType.GameEngineStartPlay));
        GameInput.start(Window.getScene(),target);
        status="Practice run "+(completed+1)+" | "+remaining+" remaining; Escape / Take over cancels";
    }
    public static void tick() {
        if(!active() || GameInput.automatic())return;
        if(!Window.isPlaying() || GameInput.lastOutcome().equals("interrupted")) {cancel();return;}
        completed++;remaining--;
        Window.get().onNotify(null,new Event(EventType.GameEngineStopPlay));
        if(remaining>0)begin();
        else status="Practice complete: "+completed+" measured runs; experience saved";
    }
}
