package agent;
import components.*;
import jade.GameObject;
import scenes.Scene;
import physics2d.components.Rigidbody2D;
import org.jbox2d.common.Vec2;
import java.util.*;

/** Reads static terrain rays and actual enemy bodies, never changes simulation state. */
public final class PickupObserver118 {
    public static PickupSafety118.Decision assess(Scene scene,GameObject player,PowerupPlanner.Goal goal){
        var body=player.getComponent(Rigidbody2D.class).getRawBody();var world=body.getWorld();float[] floor={Float.NaN};
        float top=goal.kind()==PowerupPlanner.Kind.BUMP_BLOCK?goal.y()-.16f:Math.max(player.transform.position.y+.2f,goal.y()-.2f);
        world.raycast((f,p,n,fraction)->{if(f.isSensor()||!(f.getUserData() instanceof GameObject o)||o.getComponent(Ground.class)==null||n.y<.5f)return -1;floor[0]=p.y;return fraction;},new Vec2(goal.x(),top),new Vec2(goal.x(),top-2));
        boolean[] obstructed={false};float x=player.transform.position.x,y=player.transform.position.y;
        if(Math.abs(goal.x()-x)>.25f)world.raycast((f,p,n,fraction)->{if(f.isSensor()||!(f.getUserData() instanceof GameObject o)||o.getComponent(Ground.class)==null||Math.abs(n.x)<.5f)return -1;obstructed[0]=true;return fraction;},new Vec2(x,y),new Vec2(goal.x(),y));
        var enemies=new ArrayList<PickupSafety118.Enemy>();
        for(var o:scene.getGameObjects())if(!o.isDead()&&(o.getComponent(GoombaAI.class)!=null||o.getComponent(TurtleAI.class)!=null)){
            var rb=o.getComponent(Rigidbody2D.class);if(rb!=null&&!rb.isSensor())enemies.add(new PickupSafety118.Enemy(o.transform.position.x,o.transform.position.y,rb.getVelocity().x));
        }
        return PickupSafety118.assess(goal.x(),goal.y(),floor[0],goal.kind()==PowerupPlanner.Kind.BUMP_BLOCK,obstructed[0],enemies);
    }
}
