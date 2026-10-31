package fraymus.workspace;

import com.jme3.bullet.*;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.collision.shapes.*;
import com.jme3.math.Vector3f;
import org.json.*;
import java.nio.file.Path;

/** A real native avatar driven by bounded forces. Only spawn sets position. */
final class MapPhysics implements AutoCloseable {
    final PhysicsSpace space;final PhysicsRigidBody avatar,floor;final JSONObject map;int tick,contacts,wallContacts,pathIndex,positionAssignments;double effort;Vector3f force=new Vector3f();
    MapPhysics(JSONObject map,Path nativeDir) throws Exception {
        BulletRamp.load(nativeDir);this.map=map;space=new PhysicsSpace(PhysicsSpace.BroadphaseType.DBVT);space.setAccuracy((float)BulletRamp.DT);space.setGravity(new Vector3f(0,-9.81f,0));PhysicsRigidBody player=null,ground=null;
        for(Object raw:map.getJSONArray("objects")){var o=(JSONObject)raw;var size=SpatialScene.vector(o.getJSONArray("size"),40);var shape=o.getString("id").equals("avatar")?new SphereCollisionShape(.18f):new BoxCollisionShape((float)size[0]/2,(float)size[1]/2,(float)size[2]/2);var body=new PhysicsRigidBody(shape,(float)o.getDouble("massKg"));body.setPhysicsLocation(SpatialPhysics.vec(SpatialScene.vector(o.getJSONArray("position"),20)));body.setFriction((float)o.getDouble("friction"));body.setRestitution(0);space.addCollisionObject(body);if(o.getString("id").equals("avatar")){player=body;positionAssignments++;body.setDamping(.12f,.8f);body.setSleepingThresholds(0,0);body.setCcdMotionThreshold(.02f);body.setCcdSweptSphereRadius(.15f);}if(o.getString("id").equals("floor"))ground=body;
        }avatar=player;floor=ground;if(avatar==null||floor==null)throw new IllegalStateException("Compiled avatar/floor missing");
    }
    void step(boolean automatic,double inputX,double inputZ){
        var p=avatar.getPhysicsLocation();var v=avatar.getLinearVelocity();double desiredX=inputX*2.2,desiredZ=inputZ*2.2;
        if(automatic){var path=map.getJSONArray("path");double[] target=MapDefinition.position(map,path.getInt(pathIndex),map.getDouble("cellSizeM"));double dx=target[0]-p.x,dz=target[2]-p.z,d=Math.hypot(dx,dz);if(d<.15&&pathIndex+1<path.length()){pathIndex++;target=MapDefinition.position(map,path.getInt(pathIndex),map.getDouble("cellSizeM"));dx=target[0]-p.x;dz=target[2]-p.z;d=Math.hypot(dx,dz);}double speed=Math.min(2.2,d*4);desiredX=d<1e-8?0:dx/d*speed;desiredZ=d<1e-8?0:dz/d*speed;}
        double fx=(desiredX-v.x)*12,fz=(desiredZ-v.z)*12,length=Math.hypot(fx,fz),scale=length>24?24/length:1;force.set((float)(fx*scale),0,(float)(fz*scale));avatar.applyCentralForce(force);effort+=force.length()*BulletRamp.DT;PhysicsSpace.setLocalThreadPhysicsSpace(space);space.update((float)BulletRamp.DT,0);tick++;
    }
    void disturb(double impulse){avatar.applyCentralImpulse(new Vector3f((float)impulse,0,0));}
    JSONObject sample(){contacts=0;wallContacts=0;space.contactTest(avatar,event->{if(event.getDistance1()<=.015f){contacts++;if(event.getObjectA()!=floor&&event.getObjectB()!=floor)wallContacts++;}});return new JSONObject().put("tick",tick).put("timeS",tick*BulletRamp.DT).put("position",SpatialPhysics.json(avatar.getPhysicsLocation())).put("velocity",SpatialPhysics.json(avatar.getLinearVelocity())).put("force",SpatialPhysics.json(force)).put("contacts",contacts).put("wallContacts",wallContacts).put("pathIndex",pathIndex).put("effortNs",effort).put("positionAssignments",positionAssignments);}
    double error(){var path=map.getJSONArray("path");double[] target=MapDefinition.position(map,path.getInt(path.length()-1),map.getDouble("cellSizeM"));var p=avatar.getPhysicsLocation();return Math.hypot(p.x-target[0],p.z-target[2]);}
    @Override public void close(){space.destroy();}
}
