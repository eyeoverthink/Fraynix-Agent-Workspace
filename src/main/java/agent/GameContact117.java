package agent;

import jade.GameObject;
import physics2d.components.Rigidbody2D;
import org.jbox2d.collision.WorldManifold;
import java.util.*;

/** Read-only native contacts. Their normals point away from the other body toward the player. */
public final class GameContact117 {
    public static Map<String,Object> observe(GameObject player) {
        var result=new LinkedHashMap<String,Object>();var rows=new ArrayList<Map<String,Object>>();
        var rb=player.getComponent(Rigidbody2D.class);var body=rb==null?null:rb.getRawBody();
        if(body!=null){
            result.put("bodyX",body.getPosition().x);result.put("bodyY",body.getPosition().y);
            int examined=0;
            for(var edge=body.getContactList();edge!=null&&examined++<32;edge=edge.next){
                var c=edge.contact;if(!c.isTouching()||!c.isEnabled())continue;
                boolean a=c.getFixtureA().getBody()==body;var other=a?c.getFixtureB():c.getFixtureA();
                if(other.isSensor())continue;var m=new WorldManifold();c.getWorldManifold(m);float sign=a?-1:1;
                var row=new LinkedHashMap<String,Object>();row.put("normalX",sign*m.normal.x);row.put("normalY",sign*m.normal.y);row.put("points",c.getManifold().pointCount);
                if(other.getUserData() instanceof GameObject object){row.put("id",object.getPersistentId().toString());row.put("ground",object.getComponent(components.Ground.class)!=null);}
                rows.add(row);
            }
            result.put("contactScanLimit",32);
        }
        result.put("contacts",rows);return result;
    }
}
