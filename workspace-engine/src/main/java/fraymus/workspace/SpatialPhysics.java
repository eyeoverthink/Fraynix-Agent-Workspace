package fraymus.workspace;

import com.jme3.bullet.*;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.joints.Point2PointJoint;
import com.jme3.math.*;
import org.json.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.*;

/** One thread owns each native world. Forecasts use separate worlds, never viewport motion. */
final class SpatialPhysics implements AutoCloseable {
    final PhysicsSpace space;final JSONObject scene;final Map<String,PhysicsRigidBody> bodies=new LinkedHashMap<>();int tick;Point2PointJoint grip;double[] anchor;
    SpatialPhysics(JSONObject scene,Path nativeDir) throws Exception {
        BulletRamp.load(nativeDir);this.scene=scene;space=new PhysicsSpace(PhysicsSpace.BroadphaseType.DBVT);space.setGravity(new Vector3f(0,(float)-scene.getDouble("gravityMps2"),0));space.setAccuracy((float)BulletRamp.DT);
        for(Object raw:scene.getJSONArray("objects")){var o=(JSONObject)raw;var s=SpatialScene.vector(o.getJSONArray("size"),40);var body=new PhysicsRigidBody(new BoxCollisionShape((float)s[0]/2,(float)s[1]/2,(float)s[2]/2),(float)o.getDouble("massKg"));body.setPhysicsLocation(vec(SpatialScene.vector(o.getJSONArray("position"),20)));var q=o.getJSONArray("rotation");body.setPhysicsRotation(new Quaternion((float)q.getDouble(0),(float)q.getDouble(1),(float)q.getDouble(2),(float)q.getDouble(3)));body.setFriction((float)o.getDouble("friction"));body.setRestitution((float)o.getDouble("restitution"));
            if(o.getDouble("massKg")>0){body.setDamping(.05f,.3f);body.setSleepingThresholds(0,0);body.setCcdMotionThreshold(.03f);body.setCcdSweptSphereRadius((float)Math.min(s[0],Math.min(s[1],s[2]))*.4f);}space.addCollisionObject(body);bodies.put(o.getString("id"),body);
        }
    }
    static Vector3f vec(double[] p){return new Vector3f((float)p[0],(float)p[1],(float)p[2]);}
    static JSONArray json(Vector3f p){if(!Float.isFinite(p.x)||!Float.isFinite(p.y)||!Float.isFinite(p.z))throw new IllegalStateException("Non-finite native state");return new JSONArray(new double[]{p.x,p.y,p.z});}
    JSONObject snapshot(){var out=new JSONArray();for(Object raw:scene.getJSONArray("objects")){var o=new JSONObject(raw.toString());var b=bodies.get(o.getString("id"));var q=b.getPhysicsRotation();o.put("position",json(b.getPhysicsLocation())).put("velocity",b.getMass()>0?json(b.getLinearVelocity()):new JSONArray(new int[]{0,0,0})).put("rotation",new JSONArray(new double[]{q.getX(),q.getY(),q.getZ(),q.getW()}));out.put(o);}return new JSONObject().put("tick",tick).put("timeS",tick*BulletRamp.DT).put("objects",out).put("gripping",grip!=null).put("anchor",anchor==null?JSONObject.NULL:SpatialScene.array(anchor));}
    void step(){PhysicsSpace.setLocalThreadPhysicsSpace(space);space.update((float)BulletRamp.DT,0);tick++;}
    void grab(String id){if(grip!=null)throw new IllegalStateException("Already holding an object");var b=bodies.get(id);if(b==null||b.getMass()<=0)throw new IllegalArgumentException("Grab a dynamic body");grip=new Point2PointJoint(b,Vector3f.ZERO,b.getPhysicsLocation());grip.setImpulseClamp((float)(300*BulletRamp.DT));grip.setTau(.3f);grip.setDamping(1);space.addJoint(grip);anchor=SpatialScene.vector(json(b.getPhysicsLocation()),20);}
    void release(){if(grip==null)throw new IllegalStateException("No held object");space.removeJoint(grip);grip.destroy();grip=null;anchor=null;}
    JSONObject execute(String objectId,JSONArray waypoints,BooleanSupplier cancel,Consumer<JSONObject> progress,boolean realtime) throws Exception {
        var samples=new JSONArray();var events=new JSONArray();long start=System.nanoTime();
        for(int i=0;i<120;i++)advance(cancel,progress,realtime,start,samples,objectId);grab(objectId);events.put(new JSONObject().put("kind","grab").put("tick",tick).put("objectId",objectId));boolean reached=true;double maxError=0;
        for(Object raw:waypoints){double[] target=SpatialScene.vector((JSONArray)raw,15),from=anchor.clone();int travel=(int)Math.ceil(ObserverGeometry.norm(ObserverGeometry.sub(target,from))/1.2/BulletRamp.DT);if(travel>3600)throw new IllegalArgumentException("Path leg exceeds 15 seconds");
            for(int i=1;i<=travel+96;i++){double f=Math.min(1,i/(double)Math.max(1,travel));for(int j=0;j<3;j++)anchor[j]=from[j]+(target[j]-from[j])*f;grip.setPivotInB(vec(anchor));advance(cancel,progress,realtime,start,samples,objectId);}
            double error=bodies.get(objectId).getPhysicsLocation().distance(vec(target));maxError=Math.max(maxError,error);reached&=error<=.08;events.put(new JSONObject().put("kind","move").put("tick",tick).put("targetM",raw).put("position",json(bodies.get(objectId).getPhysicsLocation())).put("errorM",error).put("reached",error<=.08));
        }
        var velocity=json(bodies.get(objectId).getLinearVelocity());release();events.put(new JSONObject().put("kind","release").put("tick",tick).put("velocityBefore",velocity).put("velocityAfter",json(bodies.get(objectId).getLinearVelocity())));
        for(int i=0;i<240;i++)advance(cancel,progress,realtime,start,samples,objectId);samples.put(sample(objectId));var finalState=snapshot();var target=SpatialScene.vector(waypoints.getJSONArray(waypoints.length()-1),15);var b=bodies.get(objectId);double[] p=SpatialScene.vector(json(b.getPhysicsLocation()),30);double horizontal=Math.hypot(p[0]-target[0],p[2]-target[2]),speed=b.getLinearVelocity().length();
        return new JSONObject().put("samples",samples).put("events",events).put("final",finalState).put("metrics",new JSONObject().put("passed",reached&&horizontal<=.1&&speed<=.25&&Math.abs(p[1]-target[1])<=.15).put("allWaypointsReached",reached).put("maxGripEndpointErrorM",maxError).put("horizontalErrorM",horizontal).put("verticalErrorM",Math.abs(p[1]-target[1])).put("terminalSpeedMps",speed).put("released",true).put("ticks",tick));
    }
    JSONObject sample(String id){var b=bodies.get(id);return new JSONObject().put("tick",tick).put("position",json(b.getPhysicsLocation())).put("velocity",json(b.getLinearVelocity()));}
    void advance(BooleanSupplier cancel,Consumer<JSONObject> progress,boolean realtime,long start,JSONArray samples,String id) throws Exception {
        if(cancel.getAsBoolean()||Thread.currentThread().isInterrupted())throw new InterruptedException("Spatial action cancelled");if(tick>=6000)throw new IllegalStateException("Native manipulation exceeded 25 second simulation budget");step();if(tick%8==0){samples.put(sample(id));progress.accept(snapshot());}if(realtime){long wait=(long)(tick*BulletRamp.DT*1e9)-(System.nanoTime()-start);if(wait>0)Thread.sleep(Math.min(40,wait/1_000_000));}
    }
    @Override public void close(){if(grip!=null){space.removeJoint(grip);grip.destroy();}space.destroy();}
}
