package fraymus.workspace;

import org.junit.Test;
import org.json.*;
import java.nio.file.*;
import java.util.*;
import com.jme3.bullet.*;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.math.Vector3f;
import static org.junit.Assert.*;

public class SceneStoreTest {
    private static JSONObject reset(){return new JSONObject().put("action","reset");}
    private static JSONObject run(JSONObject scene,double seconds){return new JSONObject().put("id",scene.getString("id")).put("expectedVersion",scene.getInt("version")).put("seconds",seconds);}
    @Test public void authoredEntitiesPersistAndRejectStaleEdits() throws Exception {
        Path root=Files.createTempDirectory("scene-contract-");JSONObject scene;
        try(var store=new SceneStore(root)){scene=store.save(new JSONObject());assertEquals(3,scene.getJSONArray("objects").length());assertEquals(4,scene.getJSONArray("objects").getJSONObject(0).getJSONArray("rotation").length());
            var next=store.save(new JSONObject().put("id",scene.getString("id")).put("expectedVersion",1).put("parameters",new JSONObject().put("angleDeg",15)));assertEquals(scene.getJSONObject("entityIds").toString(),next.getJSONObject("entityIds").toString());assertEquals(2,next.getInt("version"));
            assertThrows(ConcurrentModificationException.class,()->store.save(new JSONObject().put("id",scene.getString("id")).put("expectedVersion",1)));}
        try(var store=new SceneStore(root)){assertEquals(2,store.read(scene.getString("id")).getInt("version"));assertEquals(3,store.read(scene.getString("id")).getJSONArray("objects").length());}
    }
    @Test public void finiteBoundsAndUnitsAreEnforced() throws Exception {
        assertThrows(IllegalArgumentException.class,()->new RampDefinition(20,10,2,Double.NaN,1,0,0));
        assertThrows(IllegalArgumentException.class,()->RampDefinition.from(new JSONObject().put("massKg","1")));
        assertThrows(IllegalArgumentException.class,()->RampDefinition.from(new JSONObject().put("code","execute")));
        assertThrows(IllegalArgumentException.class,()->RampDefinition.from(new JSONObject().put("offsetZ",100)));
        try(var store=new SceneStore(Files.createTempDirectory("scene-bounds-"))){var scene=store.save(new JSONObject());assertTrue(scene.getString("units").contains("metre"));assertThrows(IllegalArgumentException.class,()->store.run(run(scene,100)));assertEquals("idle",store.snapshot().getString("status"));}
    }
    @Test public void actualBulletInclinesMatchReferenceAcrossAnglesAndMasses() throws Exception {
        try(var store=new SceneStore(Files.createTempDirectory("scene-physics-"))){
            for(double angle:new double[]{12,25,40})for(double mass:new double[]{.2,8}){var scene=store.save(new JSONObject().put("parameters",new JSONObject().put("angleDeg",angle).put("massKg",mass).put("offsetZ",.6)));
                var result=store.run(run(scene,.75));var check=result.getJSONObject("evaluation");assertTrue(result.toString(),check.getBoolean("passed"));assertTrue(result.getJSONObject("final").getInt("contacts")>0);assertEquals(.6,result.getJSONObject("final").getJSONArray("position").getDouble(2),.01);store.control(reset());}
        }
    }
    @Test public void frictionReducesTravelAndLevelContactRests() throws Exception {
        try(var store=new SceneStore(Files.createTempDirectory("scene-friction-"))){double[] distances=new double[2];int i=0;for(double friction:new double[]{0,.6}){var scene=store.save(new JSONObject().put("parameters",new JSONObject().put("friction",friction)));var result=store.run(run(scene,.75));distances[i++]=result.getJSONObject("final").getDouble("distanceM");if(friction>0)assertFalse(result.getJSONObject("evaluation").getBoolean("applicable"));store.control(reset());}assertTrue(Arrays.toString(distances),distances[1]<distances[0]*.5);
            var scene=store.save(new JSONObject().put("parameters",new JSONObject().put("angleDeg",0)));var result=store.run(run(scene,1));assertTrue(result.getJSONObject("final").getDouble("speedMps")<.02);assertTrue(result.getJSONObject("final").getInt("contacts")>0);}
    }
    @Test public void savedDefinitionAndReplaySurviveAuthoredRevisionAndRestart() throws Exception {
        Path root=Files.createTempDirectory("scene-replay-");JSONObject first,scene;
        try(var store=new SceneStore(root)){scene=store.save(new JSONObject());first=store.run(run(scene,.75));store.control(reset());store.save(new JSONObject().put("id",scene.getString("id")).put("expectedVersion",1).put("parameters",new JSONObject().put("angleDeg",10)));}
        try(var store=new SceneStore(root)){var replay=store.replay(first.getString("id"));assertEquals(1,replay.getInt("sceneVersion"));assertEquals(first.getString("id"),store.runRecord(replay.getString("id")).getString("replayOf"));assertEquals(first.getJSONObject("final").getDouble("distanceM"),replay.getJSONObject("final").getDouble("distanceM"),.0001);assertEquals(2,store.read(scene.getString("id")).getInt("version"));}
    }
    @Test public void pauseStepResetPreserveAuthoredState() throws Exception {
        try(var store=new SceneStore(Files.createTempDirectory("scene-clock-"))){var scene=store.save(new JSONObject());store.control(run(scene,2).put("action","start"));store.control(new JSONObject().put("action","pause"));int tick=store.snapshot().getJSONObject("current").getInt("tick");Thread.sleep(40);assertEquals(tick,store.snapshot().getJSONObject("current").getInt("tick"));store.control(new JSONObject().put("action","step"));assertEquals(tick+1,store.snapshot().getJSONObject("current").getInt("tick"));assertThrows(IllegalStateException.class,()->store.save(new JSONObject()));store.control(reset());assertEquals("idle",store.snapshot().getString("status"));assertEquals(1,store.read(scene.getString("id")).getInt("version"));}
    }
    @Test public void interruptedRunIsRecoveredWithoutFalseCompletion() throws Exception {
        Path root=Files.createTempDirectory("scene-interrupt-");String id;
        try(var store=new SceneStore(root)){var scene=store.save(new JSONObject());id=store.control(run(scene,5).put("action","start")).getString("id");}
        try(var store=new SceneStore(root)){assertEquals("interrupted",store.runRecord(id).getString("status"));assertFalse(store.runRecord(id).has("evaluation"));assertEquals("idle",store.snapshot().getString("status"));}
    }
    @Test public void native3DFreeFallSeparatesDepthAndHitsGround() throws Exception {
        BulletRamp.load(Files.createTempDirectory("bullet-native-"));var space=new PhysicsSpace(PhysicsSpace.BroadphaseType.DBVT);try{
            space.setGravity(new Vector3f(0,-9.81f,0));var box=new BoxCollisionShape(new Vector3f(.25f,.25f,.25f));var a=new PhysicsRigidBody(box,1);a.setPhysicsLocation(new Vector3f(0,4,0));var b=new PhysicsRigidBody(box,1);b.setPhysicsLocation(new Vector3f(0,4,2));space.addCollisionObject(a);space.addCollisionObject(b);
            var ground=new PhysicsRigidBody(new BoxCollisionShape(new Vector3f(5,.2f,5)),0);ground.setPhysicsLocation(new Vector3f(0,-.2f,0));space.addCollisionObject(ground);
            for(int i=0;i<120;i++)space.update((float)BulletRamp.DT,0);assertEquals(4-.5*9.81*.25,a.getPhysicsLocation().y,.03);assertEquals(2,b.getPhysicsLocation().z-a.getPhysicsLocation().z,.001);
            for(int i=0;i<600;i++)space.update((float)BulletRamp.DT,0);assertEquals(.25,a.getPhysicsLocation().y,.03);assertEquals(.25,b.getPhysicsLocation().y,.03);
        }finally{space.destroy();}
    }
}
