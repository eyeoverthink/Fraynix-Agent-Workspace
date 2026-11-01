package fraymus.workspace;

import com.jme3.bullet.*;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.*;
import org.json.*;
import java.nio.file.Path;
import java.util.*;

/** Forces act on the pusher only; crate motion comes from native contact. */
final class ContactPhysics implements AutoCloseable {
    static final String VERSION="solver-contact-controller-033-v1";
    private JSONArray contactPoints=new JSONArray();
    static final int MAX_TICKS=14400,MAX_RECOVERIES=2;
    private final PhysicsSpace space;final JSONObject scene,plan,intent,controller;
    private final LinkedHashMap<String,PhysicsRigidBody> bodies=new LinkedHashMap<>();
    private final PhysicsRigidBody pusher,crate;final String pusherId,crateId;
    final JSONArray events;
    private final Map<String,Integer> initializationWrites=new LinkedHashMap<>();
    private final double frictionFeedforward,approachFeedforward;
    int tick,pairContacts;double effort,work;Vector3f force=new Vector3f();
    private JSONArray integration=new JSONArray();

    ContactPhysics(JSONObject scene,JSONObject plan,Path nativeDir,JSONObject restored,JSONArray events) throws Exception {
        this.scene=scene;this.plan=plan;intent=plan.getJSONObject("intent");this.events=events;
        pusherId=intent.getString("pusherId");crateId=intent.getString("objectId");
        BulletRamp.load(nativeDir);space=new PhysicsSpace(PhysicsSpace.BroadphaseType.DBVT);space.setAccuracy((float)BulletRamp.DT);
        space.setGravity(new Vector3f(0,(float)-scene.getDouble("gravityMps2"),0));
        try{
            for(Object raw:scene.getJSONArray("objects")){
                var o=(JSONObject)raw;String id=o.getString("id");var s=PushPlan.size(o);var body=new PhysicsRigidBody(new BoxCollisionShape((float)s[0]/2,(float)s[1]/2,(float)s[2]/2),(float)o.getDouble("massKg"));
                var pose=o;if(restored!=null&&o.getDouble("massKg")>0)pose=dynamic(restored.getJSONArray("bodies"),id);
                body.setPhysicsLocation(SpatialPhysics.vec(SpatialScene.vector(pose.getJSONArray("position"),30)));initializationWrites.merge(id,1,Integer::sum);var q=pose.getJSONArray("rotation");body.setPhysicsRotation(new Quaternion(q.getFloat(0),q.getFloat(1),q.getFloat(2),q.getFloat(3)));
                body.setFriction((float)o.getDouble("friction"));body.setRestitution((float)o.getDouble("restitution"));
                if(o.getDouble("massKg")>0){body.setDamping(.05f,.3f);body.setSleepingThresholds(0,0);body.setCcdMotionThreshold(.03f);body.setCcdSweptSphereRadius((float)Math.min(s[0],Math.min(s[1],s[2]))*.4f);
                    if(id.equals(pusherId))body.setAngularFactor(Vector3f.ZERO); // Declared upright sliding actuator.
                    if(restored!=null){body.setLinearVelocity(SpatialPhysics.vec(SpatialScene.vector(pose.getJSONArray("velocity"),100)));body.setAngularVelocity(SpatialPhysics.vec(SpatialScene.vector(pose.getJSONArray("angularVelocity"),300)));}
                }
                space.addCollisionObject(body);bodies.put(id,body);
            }
            pusher=bodies.get(pusherId);crate=bodies.get(crateId);if(pusher==null||crate==null)throw new IllegalStateException("Native pushing bodies missing");
            var properties=new PushPlanProperties(scene,pusherId,crateId);frictionFeedforward=properties.frictionFeedforward;approachFeedforward=properties.approachFeedforward;
            if(restored==null){controller=new JSONObject().put("action",0).put("alignedAction",-1).put("phase","actions").put("enteredTick",0).put("hold",0).put("forceLimitN",intent.getDouble("initialForceN")).put("recoveries",0).put("recoveryMode","").put("stallTick",0).put("stallPosition",SpatialPhysics.json(crate.getPhysicsLocation())).put("settling",0);}
            else{controller=new JSONObject(restored.getJSONObject("controller").toString());tick=restored.getInt("tick");effort=restored.getDouble("effortNs");work=restored.getDouble("pusherWorkJ");force=SpatialPhysics.vec(SpatialScene.vector(restored.getJSONArray("force"),100));}
        }catch(Exception e){space.destroy();throw e;}
    }
    static JSONObject dynamic(JSONArray values,String id){for(Object raw:values)if(((JSONObject)raw).getString("id").equals(id))return (JSONObject)raw;throw new IllegalStateException("Missing checkpoint dynamic body "+id);}
    JSONObject body(String id){var b=bodies.get(id);var q=b.getPhysicsRotation();return new JSONObject().put("id",id).put("position",SpatialPhysics.json(b.getPhysicsLocation())).put("rotation",new JSONArray(new float[]{q.getX(),q.getY(),q.getZ(),q.getW()})).put("velocity",SpatialPhysics.json(b.getLinearVelocity())).put("angularVelocity",SpatialPhysics.json(b.getAngularVelocity())).put("initializationPoseWrites",initializationWrites.get(id));}
    JSONArray dynamicBodies(){return new JSONArray().put(body(pusherId)).put(body(crateId));}
    int legacyProbe(){final int[] count={0};space.contactTest(pusher,e->{if(e.getDistance1()<=.015f&&(e.getObjectA()==crate||e.getObjectB()==crate))count[0]++;});return count[0];}
    int contacts(){int count=0;var observed=new JSONArray();for(long manifold:space.listManifoldIds()){long a=com.jme3.bullet.collision.PersistentManifolds.getBodyAId(manifold),b=com.jme3.bullet.collision.PersistentManifolds.getBodyBId(manifold);if(!(a==pusher.nativeId()&&b==crate.nativeId()||a==crate.nativeId()&&b==pusher.nativeId()))continue;for(long point:com.jme3.bullet.collision.PersistentManifolds.listPointIds(manifold)){float distance=com.jme3.bullet.collision.ManifoldPoints.getDistance1(point),impulse=com.jme3.bullet.collision.ManifoldPoints.getAppliedImpulse(point);if(distance<=.015f&&impulse>1e-6f)count++;observed.put(new JSONObject().put("distanceM",distance).put("normalImpulseNs",impulse));}}contactPoints=observed;return count;}

    boolean settled(){return Set.of("finished","failed").contains(controller.getString("phase"));}
    void fail(String reason){controller.put("phase","failed").put("reason",reason);events.put(new JSONObject().put("kind","failed").put("tick",tick).put("reason",reason).put("bodies",dynamicBodies()));}
    private void resetWindow(){controller.put("hold",0).put("enteredTick",tick).put("stallTick",tick).put("stallPosition",SpatialPhysics.json(crate.getPhysicsLocation()));}
    private double[] target(JSONObject action){return SpatialScene.vector(action.getJSONArray("target"),20);}
    private double horizontal(Vector3f p,double[] target){return Math.hypot(p.x-target[0],p.z-target[2]);}
    private Vector3f approach(double[] target){var p=pusher.getPhysicsLocation();var v=pusher.getLinearVelocity();double dx=target[0]-p.x,dz=target[2]-p.z,d=Math.hypot(dx,dz),speed=Math.min(.75,3*d);double feed=d>.015?approachFeedforward:0;return new Vector3f((float)((d<1e-9?0:dx/d*speed-v.x)*30+(d<1e-9?0:dx/d*feed)),0,(float)((d<1e-9?0:dz/d*speed-v.z)*30+(d<1e-9?0:dz/d*feed)));}
    private void completeAction(){int index=controller.getInt("action");events.put(new JSONObject().put("kind","action-complete").put("action",index).put("tick",tick).put("bodies",dynamicBodies()));controller.put("action",index+1);resetWindow();}
    private Vector3f control(){
        var phase=controller.getString("phase");var v=pusher.getLinearVelocity();
        if(phase.equals("settle")){
            controller.put("settling",controller.getInt("settling")+1);
            if(controller.getInt("settling")>=120){controller.put("phase","finished");events.put(new JSONObject().put("kind","settled").put("tick",tick).put("bodies",dynamicBodies()));}
            return new Vector3f(-30*v.x,0,-30*v.z);
        }
        if(controller.getInt("action")>=plan.getJSONArray("actions").length()){controller.put("phase","settle");return new Vector3f(-30*v.x,0,-30*v.z);}
        var action=plan.getJSONArray("actions").getJSONObject(controller.getInt("action"));var target=target(action);
        if(!controller.getString("recoveryMode").isEmpty()){
            var point=SpatialScene.vector(controller.getJSONArray("recoveryTarget"),20);double d=horizontal(pusher.getPhysicsLocation(),point);
            if(d<.07&&pusher.getLinearVelocity().length()<.18){controller.put("hold",controller.getInt("hold")+1);}else controller.put("hold",0);
            if(controller.getInt("hold")>=12){
                if(controller.getString("recoveryMode").equals("retreat")){var dir=action.getJSONArray("direction");var cp=crate.getPhysicsLocation();controller.put("recoveryMode","reapproach").put("recoveryTarget",new JSONArray(new double[]{cp.x-dir.getInt(0)*.8,0,cp.z-dir.getInt(1)*.8})).put("hold",0);}
                else{controller.put("recoveryMode","").put("forceLimitN",intent.getDouble("maxForceN"));resetWindow();events.put(new JSONObject().put("kind","recovery-complete").put("tick",tick).put("action",controller.getInt("action")).put("forceLimitN",controller.getDouble("forceLimitN")).put("bodies",dynamicBodies()));}
            }
            if(tick-controller.getInt("recoveryTick")>1440){fail("Recovery approach exceeded six simulated seconds");return new Vector3f();}
            return approach(point);
        }
        if(action.getString("kind").equals("approach")){
            if(horizontal(pusher.getPhysicsLocation(),target)<.07&&v.length()<.18)controller.put("hold",controller.getInt("hold")+1);else controller.put("hold",0);
            if(controller.getInt("hold")>=12)completeAction();
            if(tick-controller.getInt("enteredTick")>1800)fail("Standing-pose approach could not complete within its native budget");
            return approach(target);
        }
        var cp=crate.getPhysicsLocation();var cv=crate.getLinearVelocity();var dir=action.getJSONArray("direction");double nx=dir.getInt(0),nz=dir.getInt(1),error=horizontal(cp,target);
        if(controller.optInt("alignedAction",-1)!=controller.getInt("action")){
            double[] standing={cp.x-nx*.9,0,cp.z-nz*.9};
            if(horizontal(pusher.getPhysicsLocation(),standing)<.035&&v.length()<.12)controller.put("hold",controller.getInt("hold")+1);else controller.put("hold",0);
            if(controller.getInt("hold")>=12){controller.put("alignedAction",controller.getInt("action"));resetWindow();events.put(new JSONObject().put("kind","aligned").put("tick",tick).put("action",controller.getInt("action")).put("bodies",dynamicBodies()));}
            return approach(standing);
        }
        if(error<.035&&cv.length()<.12)controller.put("hold",controller.getInt("hold")+1);else controller.put("hold",0);
        if(controller.getInt("hold")>=12){completeAction();return new Vector3f(-30*v.x,0,-30*v.z);}
        if(tick-controller.getInt("stallTick")>=240){
            var before=SpatialScene.vector(controller.getJSONArray("stallPosition"),30);double progress=Math.hypot(cp.x-before[0],cp.z-before[2]);
            if(progress<.02&&contacts()>0&&error>.035){
                if(controller.getInt("recoveries")>=MAX_RECOVERIES){fail("Physical pushing stalled after two bounded recovery attempts");return new Vector3f();}
                events.put(new JSONObject().put("kind","jam").put("tick",tick).put("windowTick",controller.getInt("stallTick")).put("windowPosition",new JSONArray(before)).put("cratePosition",SpatialPhysics.json(cp)).put("progressM",progress).put("contacts",contacts()).put("action",controller.getInt("action")).put("forceLimitN",controller.getDouble("forceLimitN")));
                controller.put("recoveries",controller.getInt("recoveries")+1).put("recoveryMode","retreat").put("recoveryTick",tick).put("recoveryTarget",new JSONArray(new double[]{cp.x-nx,0,cp.z-nz})).put("hold",0);
                events.put(new JSONObject().put("kind","recovery-start").put("tick",tick).put("action",controller.getInt("action")).put("bodies",dynamicBodies()));return new Vector3f();
            }
            controller.put("stallTick",tick).put("stallPosition",SpatialPhysics.json(cp));
        }
        if(tick-controller.getInt("enteredTick")>3600){fail("Push leg exceeded fifteen simulated seconds");return new Vector3f();}
        double along=(target[0]-cp.x)*nx+(target[2]-cp.z)*nz;double desired=Math.max(0,Math.min(.65,along*2));
        var po=pusher.getPhysicsLocation();double cross=(po.x-cp.x)*(-nz)+(po.z-cp.z)*nx,crossV=v.x*(-nz)+v.z*nx;
        double feed=along>.015?frictionFeedforward:0,axial=(desired-(v.x*nx+v.z*nz))*30+feed,lateral=-cross*12-crossV*30+Math.max(-.12,Math.min(.12,((target[0]-cp.x)*(-nz)+(target[2]-cp.z)*nx)*2))*30;
        return new Vector3f((float)(nx*axial-nz*lateral),0,(float)(nz*axial+nx*lateral));
    }
    private static final class PushPlanProperties {
        final double frictionFeedforward,approachFeedforward;
        PushPlanProperties(JSONObject scene,String pusher,String crate){var p=SpatialScene.object(scene,pusher);var c=SpatialScene.object(scene,crate);double floor=1;for(Object raw:scene.getJSONArray("objects")){var o=(JSONObject)raw;if(o.getString("sourceId").equals("floor"))floor=o.getDouble("friction");}approachFeedforward=p.getDouble("massKg")*p.getDouble("friction")*floor*scene.getDouble("gravityMps2")*1.05;frictionFeedforward=(p.getDouble("massKg")*p.getDouble("friction")+c.getDouble("massKg")*c.getDouble("friction"))*floor*scene.getDouble("gravityMps2")*1.05;}
    }
    void step(){
        if(settled())return;if(tick>=MAX_TICKS){fail("Native pushing exceeded sixty simulated seconds");return;}
        var beforeP=pusher.getPhysicsLocation();var beforeC=crate.getPhysicsLocation();var proposed=control();
        double cap=controller.getString("recoveryMode").isEmpty()&&controller.getString("phase").equals("actions")&&controller.getInt("action")<plan.getJSONArray("actions").length()&&plan.getJSONArray("actions").getJSONObject(controller.getInt("action")).getString("kind").equals("push")?controller.getDouble("forceLimitN"):intent.getDouble("maxForceN");
        float length=proposed.length();force=length>cap?proposed.mult((float)(cap/length)):proposed;
        // This is the single application-level actuator call. The ledger names its body.
        pusher.applyCentralForce(force);effort+=Math.hypot(force.x,force.z)*BulletRamp.DT;
        PhysicsSpace.setLocalThreadPhysicsSpace(space);space.update((float)BulletRamp.DT,0);tick++;pairContacts=contacts();
        var afterP=pusher.getPhysicsLocation();var afterC=crate.getPhysicsLocation();work+=force.x*((double)afterP.x-beforeP.x)+force.z*((double)afterP.z-beforeP.z);
        integration.put(new JSONArray().put(tick).put(pusherId).put(force.x).put(force.z).put((double)afterP.x-beforeP.x).put((double)afterP.z-beforeP.z).put((double)afterC.x-beforeC.x).put((double)afterC.z-beforeC.z).put(pairContacts).put(controller.getInt("action")));
    }
    JSONObject sample(){return new JSONObject().put("tick",tick).put("timeS",tick*BulletRamp.DT).put("bodies",dynamicBodies()).put("force",SpatialPhysics.json(force)).put("contacts",pairContacts).put("effortNs",effort).put("pusherWorkJ",work).put("controller",new JSONObject(controller.toString())).put("nativeContact",new JSONObject().put("method","Persistent native solver manifold, actual positive normal impulse").put("points",new JSONArray(contactPoints.toString())).put("legacyProbeContacts",legacyProbe()));}
    JSONArray takeIntegration(){var result=integration;integration=new JSONArray();return result;}
    JSONObject snapshot(){var objects=new JSONArray();for(Object raw:scene.getJSONArray("objects")){var o=new JSONObject(raw.toString());if(o.getDouble("massKg")>0){var dynamic=body(o.getString("id"));for(String key:List.of("position","rotation","velocity","angularVelocity"))o.put(key,dynamic.get(key));}objects.put(o);}return new JSONObject().put("tick",tick).put("objects",objects).put("gripping",false).put("controller",new JSONObject(controller.toString()));}
    JSONObject checkpoint(JSONObject sample){return new JSONObject(sample.toString());}
    @Override public void close(){space.destroy();}
}
