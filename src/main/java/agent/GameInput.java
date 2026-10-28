package agent;

import components.*;
import jade.GameObject;
import jade.KeyListener;
import org.joml.Vector2f;
import scenes.Scene;
import java.util.UUID;
import static org.lwjgl.glfw.GLFW.*;

/** Shared input boundary for the original player AND pipe mechanics. No teleport operations. */
public final class GameInput {
    private static final ActorControls controls = new ActorControls();
    private static UUID episode, actor;
    private static boolean automatic;
    private static long sequence, tick, jumpUntil, lastJump=-100;
    private static float startX, maxX, goalX;
    private static String status="Human control";
    private static long stalledTicks;
    private static boolean neuralJump;
    private static float[] lastSensory=new float[0];
    private static ActorControls.Input current=ActorControls.Input.NONE, previous=ActorControls.Input.NONE;
    private GameInput() {}
    public static boolean automatic() { return automatic; }
    public static String status() { return status; }
    public static float progress() { return Math.max(0,Math.abs(goalX-startX)-Math.abs(goalX-maxX)); }
    public static boolean neuralJump() { return neuralJump; }
    public static void neuralJump(boolean value) { neuralJump=value; }
    public static float[] sensory() { return lastSensory.clone(); }
    public static void requestJump() { if (automatic) { jumpUntil=tick+27; lastJump=tick; } }
    public static void start(Scene scene, float targetX) {
        GameObject player=scene.getGameObjectWith(PlayerController.class);
        if (player==null || !Float.isFinite(targetX)) throw new IllegalArgumentException("A live player and finite target are required");
        episode=UUID.randomUUID(); actor=player.getPersistentId(); sequence=0;
        controls.reset(episode,actor); automatic=true; startX=maxX=player.transform.position.x; goalX=targetX;
        current=previous=ActorControls.Input.NONE; jumpUntil=0; lastJump=-100; stalledTicks=0;
        status="Playing toward x="+goalX+" using local geometry and normal controls";
        nervous.BrainRuntime.start();
    }
    public static void stop(String reason) {
        automatic=false; controls.release(); current=previous=ActorControls.Input.NONE; status=reason;
    }
    public static void tick(Scene scene) {
        tick=scene.tick(); previous=current;
        if (!automatic) { current=ActorControls.Input.NONE; return; }
        GameObject player=scene.getGameObjectWith(PlayerController.class);
        if (player==null || !player.getPersistentId().equals(actor)) { stop("Player changed; control released"); return; }
        PlayerController controller=player.getComponent(PlayerController.class);
        if (controller.isDead()) { stop("Player died; episode recorded by the game"); return; }
        if (controller.hasWon()) { stop("Flag reached; game reports a win"); return; }
        float x=player.transform.position.x, y=player.transform.position.y;
        if (Math.abs(x-goalX)<.15f) { stop("Target reached at x="+x); return; }
        if (Math.abs(goalX-x)<Math.abs(goalX-maxX)-.002f) { maxX=x; stalledTicks=0; } else stalledTicks++;
        if (stalledTicks>600) { stop("No forward progress for 10 simulated seconds; take over or choose another target"); return; }
        float direction=goalX>=x?1:-1;
        var floor=scene.physics().raycast(player,new Vector2f(x+direction*.32f,y+.04f),new Vector2f(x+direction*.32f,y-.5f));
        var wall=scene.physics().raycast(player,new Vector2f(x,y),new Vector2f(x+direction*.4f,y));
        boolean hazard=scene.getGameObjects().stream().anyMatch(o -> !o.isDead()
            &&(o.getComponent(GoombaAI.class)!=null || o.getComponent(TurtleAI.class)!=null)
            &&(o.transform.position.x-x)*direction>0 && (o.transform.position.x-x)*direction<.85f && Math.abs(o.transform.position.y-y)<.6f);
        boolean obstacle=wall.hit && wall.hitObject!=null && wall.hitObject.getComponent(Ground.class)!=null;
        var body=player.getComponent(physics2d.components.Rigidbody2D.class);
        lastSensory=new float[]{Math.max(-1,Math.min(1,(goalX-x)/5)),body.getVelocity().x/3,body.getVelocity().y/3,
            controller.onGround?1:-1,floor.hit?1:-1,obstacle?1:-1,hazard?1:-1};
        nervous.BrainRuntime.observe(lastSensory);
        if (controller.onGround && tick-lastJump>35 && (!floor.hit || obstacle || hazard)) { jumpUntil=tick+27; lastJump=tick; }
        if(neuralJump && controller.onGround && tick-lastJump>35 && nervous.BrainRuntime.core()!=null
            && nervous.BrainRuntime.core().output(0)>.1f) { jumpUntil=tick+12; lastJump=tick; }
        ActorControls.Input input=new ActorControls.Input(direction,0,tick<jumpUntil,tick%30==0);
        controls.accept(new ActorControls.Lease(episode,actor,sequence++,tick,tick+1,input),tick);
        current=controls.at(tick);
    }
    public static boolean isKeyPressed(int key) {
        if (!automatic) return KeyListener.isKeyPressed(key);
        return switch(key) {
            case GLFW_KEY_RIGHT,GLFW_KEY_D -> current.horizontal()>0;
            case GLFW_KEY_LEFT,GLFW_KEY_A -> current.horizontal()<0;
            case GLFW_KEY_UP,GLFW_KEY_W -> current.vertical()>0;
            case GLFW_KEY_DOWN,GLFW_KEY_S -> current.vertical()<0;
            case GLFW_KEY_SPACE -> current.jump();
            case GLFW_KEY_E -> current.fire();
            default -> false;
        };
    }
    public static boolean keyBeginPress(int key) {
        if (!automatic) return KeyListener.keyBeginPress(key);
        return key==GLFW_KEY_E && current.fire() && !previous.fire();
    }
}
