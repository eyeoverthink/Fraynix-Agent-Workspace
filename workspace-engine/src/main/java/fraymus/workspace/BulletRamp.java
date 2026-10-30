package fraymus.workspace;

import com.jme3.bullet.*;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.*;
import com.jme3.system.NativeLibraryLoader;
import org.json.*;
import java.nio.file.*;

/** Real native Bullet 3D integration. The viewport never computes authoritative motion. */
final class BulletRamp implements AutoCloseable {
    static final double DT=1.0/240;
    static final String BACKEND="Minie 9.0.3 / Libbulletjme 22.0.3 / single precision";
    private static boolean loaded;
    private final PhysicsSpace space;
    private final PhysicsRigidBody block;
    private final RampDefinition config;
    private final JSONObject ids;
    private int tick;
    static synchronized void load(Path directory) throws Exception {
        if(!loaded){Files.createDirectories(directory);NativeLibraryLoader.setCustomExtractionFolder(directory.toString());NativeLibraryLoader.loadNativeLibrary("bulletjme",true);loaded=true;}
    }
    BulletRamp(RampDefinition config,JSONObject ids,Path nativeDirectory) throws Exception {
        load(nativeDirectory);this.config=config;this.ids=ids;
        space=new PhysicsSpace(PhysicsSpace.BroadphaseType.DBVT);space.setGravity(new Vector3f(0,(float)-config.gravityMps2(),0));space.setAccuracy((float)DT);
        Quaternion rotation=new Quaternion().fromAngleAxis((float)-config.radians(),Vector3f.UNIT_Z);
        body(new Vector3f((float)config.lengthM()/2,.1f,(float)config.widthM()/2),0,new Vector3f(0,(float)config.centerY(),0),rotation,1);
        body(new Vector3f(20,.2f,12),0,new Vector3f(0,-.2f,0),new Quaternion(),1);
        double[] p=config.start();block=body(new Vector3f(.25f,.25f,.25f),(float)config.massKg(),new Vector3f((float)p[0],(float)p[1],(float)p[2]),rotation,(float)config.friction());
        block.setDamping(0,0);block.setSleepingThresholds(0,0);
    }
    private PhysicsRigidBody body(Vector3f half,float mass,Vector3f position,Quaternion rotation,float friction){
        var body=new PhysicsRigidBody(new BoxCollisionShape(half),mass);body.setPhysicsLocation(position);body.setPhysicsRotation(rotation);body.setFriction(friction);body.setRestitution(0);space.addCollisionObject(body);return body;
    }
    void step(){PhysicsSpace.setLocalThreadPhysicsSpace(space);space.update((float)DT,0);tick++;}
    int tick(){return tick;}
    JSONObject configuration(){return new JSONObject().put("solver",space.getSolverType().toString()).put("iterations",space.getSolverNumIterations()).put("broadphase","DBVT").put("stepMode","external fixed dt; update(dt, 0)").put("precision","single").put("restitution",0).put("damping",0).put("friction","block coefficient multiplied by static-surface coefficient 1");}
    JSONObject sample(){
        Vector3f p=block.getPhysicsLocation(),v=block.getLinearVelocity();Quaternion q=block.getPhysicsRotation();double[] initial=config.start();
        double distance=(p.x-initial[0])*Math.cos(config.radians())-(p.y-initial[1])*Math.sin(config.radians());
        for(float f:new float[]{p.x,p.y,p.z,v.x,v.y,v.z,q.getX(),q.getY(),q.getZ(),q.getW()})if(!Float.isFinite(f))throw new IllegalStateException("Non-finite Bullet state");
        int[] contacts={0};space.contactTest(block,event->{if(event.getDistance1()<=.03f)contacts[0]++;});
        return new JSONObject().put("tick",tick).put("timeS",tick*DT).put("position",new JSONArray(new float[]{p.x,p.y,p.z})).put("rotation",new JSONArray(new float[]{q.getX(),q.getY(),q.getZ(),q.getW()}))
            .put("velocity",new JSONArray(new float[]{v.x,v.y,v.z})).put("distanceM",distance).put("speedMps",v.length()).put("contacts",contacts[0]);
    }
    JSONObject snapshot(){var sample=sample();var objects=config.objects(ids);objects.getJSONObject(2).put("position",sample.getJSONArray("position")).put("rotation",sample.getJSONArray("rotation"));return sample.put("objects",objects);}
    @Override public void close(){space.destroy();}
}
