package fraymus.workspace;

import com.jme3.bullet.*;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.collision.shapes.*;
import com.jme3.math.Vector3f;
import com.jme3.math.Quaternion;
import org.json.*;
import java.nio.file.Path;
import java.util.*;

/** One thread owns Bullet. Pose writes occur only during native-body initialization. */
final class MapPhysics implements AutoCloseable {
    final PhysicsSpace space;
    final PhysicsRigidBody avatar,floor;
    final JSONObject map;
    private final Set<PhysicsRigidBody> floors=new HashSet<>();
    int tick,contacts,wallContacts,pathIndex,positionAssignments;
    double effort,actuatorWork,positiveWork,brakingWork,dragWork,gravityWork;
    Vector3f force=new Vector3f();
    private JSONArray integration=new JSONArray();

    MapPhysics(JSONObject map,Path nativeDir) throws Exception {this(map,nativeDir,null);}
    MapPhysics(JSONObject map,Path nativeDir,JSONObject restored) throws Exception {
        BulletRamp.load(nativeDir);this.map=map;
        space=new PhysicsSpace(PhysicsSpace.BroadphaseType.DBVT);
        space.setAccuracy((float)BulletRamp.DT);space.setGravity(new Vector3f(0,-9.81f,0));
        PhysicsRigidBody player=null,ground=null;
        try {
            for(Object raw:map.getJSONArray("objects")) {
                var o=(JSONObject)raw;String id=o.getString("id");boolean isAvatar=id.equals("avatar");
                var size=SpatialScene.vector(o.getJSONArray("size"),40);
                var shape=isAvatar?new SphereCollisionShape(.18f):new BoxCollisionShape((float)size[0]/2,(float)size[1]/2,(float)size[2]/2);
                var body=new PhysicsRigidBody(shape,(float)o.getDouble("massKg"));
                var position=isAvatar&&restored!=null?restored.getJSONArray("position"):o.getJSONArray("position");
                body.setPhysicsLocation(SpatialPhysics.vec(SpatialScene.vector(position,30)));
                body.setFriction((float)o.getDouble("friction"));body.setRestitution((float)o.optDouble("restitution",0));
                if(isAvatar) {
                    player=body;positionAssignments++;
                    body.setDamping(.12f,.8f);body.setSleepingThresholds(0,0);
                    body.setCcdMotionThreshold(.02f);body.setCcdSweptSphereRadius(.15f);
                    if(restored!=null) {
                        var q=restored.getJSONArray("rotation");
                        body.setPhysicsRotation(new Quaternion(q.getFloat(0),q.getFloat(1),q.getFloat(2),q.getFloat(3)));
                        body.setLinearVelocity(SpatialPhysics.vec(SpatialScene.vector(restored.getJSONArray("velocity"),30)));
                        body.setAngularVelocity(SpatialPhysics.vec(SpatialScene.vector(restored.getJSONArray("angularVelocity"),300)));
                    }
                }
                space.addCollisionObject(body);
                if(id.equals("floor")||id.startsWith("floor-")){floors.add(body);if(ground==null)ground=body;}
            }
            avatar=player;floor=ground;
            if(avatar==null||floor==null)throw new IllegalStateException("Compiled avatar/floor missing");
            if(restored!=null) {
                tick=restored.getInt("tick");pathIndex=restored.getInt("pathIndex");
                var s=restored.getJSONObject("sample");effort=s.getDouble("effortNs");
                actuatorWork=s.getDouble("actuatorWorkJ");positiveWork=s.getDouble("positiveWorkJ");brakingWork=s.getDouble("brakingWorkJ");
                dragWork=s.getDouble("dragWorkJ");gravityWork=s.getDouble("gravityWorkJ");
                force=SpatialPhysics.vec(SpatialScene.vector(s.getJSONArray("force"),30));
            }
        } catch(Exception e){space.destroy();throw e;}
    }

    void step(boolean automatic,double inputX,double inputZ) {
        var p=avatar.getPhysicsLocation();var v=avatar.getLinearVelocity();
        double desiredX=inputX*2.2,desiredZ=inputZ*2.2;
        if(automatic) {
            var path=map.getJSONArray("path");double[] target=MapDefinition.position(map,path.getInt(pathIndex),map.getDouble("cellSizeM"));
            double dx=target[0]-p.x,dz=target[2]-p.z,d=Math.hypot(dx,dz);
            if(d<.15&&pathIndex+1<path.length()) {
                pathIndex++;target=MapDefinition.position(map,path.getInt(pathIndex),map.getDouble("cellSizeM"));
                dx=target[0]-p.x;dz=target[2]-p.z;d=Math.hypot(dx,dz);
            }
            // Direct force actuator; friction is not substituted for available braking force.
            double speed=Math.min(2.2,d*4);
            desiredX=d<1e-8?0:dx/d*speed;desiredZ=d<1e-8?0:dz/d*speed;
        }
        double fx=(desiredX-v.x)*12,fz=(desiredZ-v.z)*12,length=Math.hypot(fx,fz),scale=length>24?24/length:1;
        force.set((float)(fx*scale),0,(float)(fz*scale));avatar.applyCentralForce(force);
        double drag=MapMaterials.material(map,MapMaterials.tileAt(map,p.x,p.z)).getDouble("dragNsPerM");
        var dragForce=new Vector3f((float)(-drag*v.x),0,(float)(-drag*v.z));avatar.applyCentralForce(dragForce);
        effort+=Math.hypot(force.x,force.z)*BulletRamp.DT;
        PhysicsSpace.setLocalThreadPhysicsSpace(space);space.update((float)BulletRamp.DT,0);tick++;
        var after=avatar.getPhysicsLocation();double dx=(double)after.x-p.x,dy=(double)after.y-p.y,dz=(double)after.z-p.z;
        double work=force.x*dx+force.z*dz;actuatorWork+=work;positiveWork+=Math.max(0,work);brakingWork+=Math.max(0,-work);
        dragWork+=dragForce.x*dx+dragForce.z*dz;gravityWork+=-9.81*dy;
        integration.put(new JSONArray(new double[]{tick,force.x,force.z,dx,dz,dragForce.x,dragForce.z,dy}));
    }

    double disturb(double impulse) {
        double before=kineticEnergy();avatar.applyCentralImpulse(new Vector3f((float)impulse,0,0));return kineticEnergy()-before;
    }
    double kineticEnergy() {
        // The only dynamic body is the unit-mass sphere, I=2/5*m*r².
        return .5*avatar.getLinearVelocity().lengthSquared()+.5*.4*.18*.18*avatar.getAngularVelocity().lengthSquared();
    }
    JSONArray takeIntegration(){var rows=integration;integration=new JSONArray();return rows;}
    JSONObject sample() {
        contacts=0;wallContacts=0;
        space.contactTest(avatar,event->{if(event.getDistance1()<=.015f){contacts++;if(!floors.contains(event.getObjectA())&&!floors.contains(event.getObjectB()))wallContacts++;}});
        var p=avatar.getPhysicsLocation();var q=avatar.getPhysicsRotation();int tile=MapMaterials.tileAt(map,p.x,p.z);
        return new JSONObject().put("tick",tick).put("timeS",tick*BulletRamp.DT).put("position",SpatialPhysics.json(p))
            .put("velocity",SpatialPhysics.json(avatar.getLinearVelocity())).put("rotation",new JSONArray(new float[]{q.getX(),q.getY(),q.getZ(),q.getW()}))
            .put("angularVelocity",SpatialPhysics.json(avatar.getAngularVelocity())).put("force",SpatialPhysics.json(force))
            .put("contacts",contacts).put("wallContacts",wallContacts).put("pathIndex",pathIndex).put("effortNs",effort)
            .put("positionAssignments",positionAssignments).put("actuatorWorkJ",actuatorWork).put("positiveWorkJ",positiveWork)
            .put("brakingWorkJ",brakingWork).put("dragWorkJ",dragWork).put("gravityWorkJ",gravityWork)
            .put("kineticEnergyJ",kineticEnergy()).put("material",MapMaterials.material(map,tile));
    }
    JSONObject checkpoint(JSONObject committedSample) {
        return new JSONObject().put("tick",tick).put("pathIndex",pathIndex).put("position",SpatialPhysics.json(avatar.getPhysicsLocation()))
            .put("rotation",committedSample.getJSONArray("rotation")).put("velocity",SpatialPhysics.json(avatar.getLinearVelocity()))
            .put("angularVelocity",SpatialPhysics.json(avatar.getAngularVelocity())).put("sample",committedSample);
    }
    double error() {
        var path=map.getJSONArray("path");double[] target=MapDefinition.position(map,path.getInt(path.length()-1),map.getDouble("cellSizeM"));
        var p=avatar.getPhysicsLocation();return Math.hypot(p.x-target[0],p.z-target[2]);
    }
    @Override public void close(){space.destroy();}
}
