package agent;
import components.*;
import scenes.Scene;
import physics2d.components.Rigidbody2D;
import java.util.*;

/** Read-only original enemy bodies; no inferred enemy identity from artwork. */
public final class GameThreat118 {
    public static Map<String,Object> observe(Scene scene){
        var rows=new ArrayList<Map<String,Object>>();int total=0;
        for(var o:scene.getGameObjects())if(!o.isDead()&&(o.getComponent(GoombaAI.class)!=null||o.getComponent(TurtleAI.class)!=null)){
            var rb=o.getComponent(Rigidbody2D.class);if(rb==null)continue;total++;
            if(rows.size()<64)rows.add(Map.of("id",o.getPersistentId().toString(),"kind",o.getComponent(GoombaAI.class)!=null?"goomba":"turtle","x",o.transform.position.x,"y",o.transform.position.y,"vx",rb.getVelocity().x,"vy",rb.getVelocity().y,"sensor",rb.isSensor()));
        }
        return Map.of("enemies",rows,"total",total,"limit",64);
    }
}
