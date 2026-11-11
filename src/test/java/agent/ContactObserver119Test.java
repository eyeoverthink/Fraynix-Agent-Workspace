package agent;
import components.Ground;
import jade.GameObject;
import scenes.Scene;
import physics2d.components.*;
import physics2d.enums.BodyType;
import org.joml.Vector2f;
import org.junit.Test;
import static org.junit.Assert.*;

public class ContactObserver119Test {
    private GameObject body(Scene scene,float x,float y,boolean ground,boolean sensor){
        var o=scene.createGameObject("fixture");o.transform.position.set(x,y);o.transform.scale.set(.2f,.2f);
        if(ground)o.addComponent(new Ground());var rb=new Rigidbody2D();rb.setBodyType(BodyType.Static);if(sensor)rb.setIsSensor();o.addComponent(rb);
        var box=new Box2DCollider();box.setHalfSize(new Vector2f(.2f,.2f));o.addComponent(box);scene.physics().add(o);return o;
    }
    @Test public void changedGoalCannotSubstituteOppositeFloorForTheCommittedRetreat(){
        var scene=new Scene(null);var actor=body(scene,0,1,false,false);var left=body(scene,-.3f,0,true,false);body(scene,.3f,0,true,false);
        var policy=new ContactRecovery119();for(int t=0;t<=24;t++)policy.step(t,0,1,1,true,true,false,ContactObserver119.retreatSupported(actor,1),false,false);
        assertEquals(ContactRecovery119.Phase.RELEASE,policy.phase());
        scene.physics().destroyGameObject(left); // A changed scene loses the original retreat support.
        assertTrue(ContactObserver119.retreatSupported(actor,-1)); // A new leftward goal sees different floor.
        float committed=policy.observationDirection(-1);assertFalse(ContactObserver119.retreatSupported(actor,committed));
        var result=policy.step(25,0,1,committed,true,true,false,ContactObserver119.retreatSupported(actor,committed),false,false);
        assertFalse(result.override());assertEquals(ContactRecovery119.Phase.REFUSED,policy.phase());
    }
    @Test public void sensorsAndNonTerrainCannotJustifyARecoveryLanding(){
        var scene=new Scene(null);var actor=body(scene,0,1,false,false);body(scene,-.3f,.2f,true,true);body(scene,-.3f,-.1f,false,false);
        assertFalse(ContactObserver119.retreatSupported(actor,1));body(scene,-.3f,0,true,false);assertTrue(ContactObserver119.retreatSupported(actor,1));
    }
}
