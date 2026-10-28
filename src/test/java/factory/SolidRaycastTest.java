package factory;

import jade.GameObject;
import org.joml.Vector2f;
import org.junit.Test;
import physics2d.components.*;
import physics2d.enums.BodyType;
import scenes.Scene;
import static org.junit.Assert.*;

public class SolidRaycastTest {
    private GameObject body(Scene scene,String name,float x,boolean sensor) {
        var object=scene.createGameObject(name);object.transform.position.set(x,0);object.transform.scale.set(.25f,.25f);
        var rb=new Rigidbody2D();rb.setBodyType(BodyType.Static);if(sensor) rb.setIsSensor();object.addComponent(rb);
        var box=new Box2DCollider();box.setHalfSize(new Vector2f(.25f,.25f));object.addComponent(box);scene.physics().add(object);return object;
    }
    @Test public void sensorsAndTheActorDoNotHideTheNearestSolidBlock() {
        Scene scene=new Scene(null);var actor=body(scene,"player",0,false);body(scene,"sensor",.5f,true);
        var near=body(scene,"wall",1,false);body(scene,"far wall",1.75f,false);
        var result=scene.physics().raycastSolid(actor,new Vector2f(-.5f,0),new Vector2f(2,0));
        assertTrue(result.hit);assertSame(near,result.hitObject);assertTrue(result.normal.x<0);
    }
}
