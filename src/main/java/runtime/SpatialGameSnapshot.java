package runtime;

import jade.Window;
import components.SpriteRenderer;
import physics2d.components.Rigidbody2D;
import java.util.*;

/** Owner-thread, data-only adapter over the actual preserved game objects. */
final class SpatialGameSnapshot {
    static Map<String,Object> capture() {
        var scene=Window.getScene();var objects=scene.getGameObjects();
        if(objects.size()>4096)throw new IllegalStateException("Spatial game snapshot exceeds4096 entities");
        var rows=new ArrayList<Map<String,Object>>();
        for(var object:objects){
            var row=new LinkedHashMap<String,Object>();row.put("id",object.getPersistentId().toString());row.put("label",object.getName());row.put("dead",object.isDead());
            row.put("components",object.getAllComponents().stream().map(c->c.getClass().getName()).toList());
            var t=object.transform;
            if(t!=null){for(float f:new float[]{t.position.x,t.position.y,t.scale.x,t.scale.y,t.rotation})if(!Float.isFinite(f))throw new IllegalStateException("Nonfinite game transform");
                row.put("position",List.of(t.position.x,t.position.y));row.put("signedScale",List.of(t.scale.x,t.scale.y));row.put("rotationDeg",t.rotation);row.put("renderLayer",t.zIndex);
            }
            var body=object.getComponent(Rigidbody2D.class);if(body!=null)row.put("body",Map.of("type",body.getBodyType().name(),"velocity",List.of(body.getVelocity().x,body.getVelocity().y),"friction",body.getFriction(),"mass",body.getMass(),"units","legacy game parameters, not independently calibrated SI"));
            var sprite=object.getComponent(SpriteRenderer.class);if(sprite!=null){var uv=Arrays.stream(sprite.getTexCoords()).map(p->List.of(p.x,p.y)).toList();var c=sprite.getColor();row.put("sprite",Map.of("textureReference",sprite.getTexture()==null?"":sprite.getTexture().getFilepath(),"uv",uv,"color",List.of(c.x,c.y,c.z,c.w)));}
            rows.add(row);
        }
        var camera=scene.camera();return Map.of("entities",rows,"camera",Map.of("position",List.of(camera.position.x,camera.position.y),"span",List.of(camera.getProjectionSize().x*camera.getZoom(),camera.getProjectionSize().y*camera.getZoom())),"scope","Actual XY transforms/component names; sprite layer is draw order, not physical depth. No SI conversion.");
    }
}
