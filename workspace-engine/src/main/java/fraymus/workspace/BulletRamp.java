package fraymus.workspace;

import com.jme3.bullet.*;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.joints.Point2PointJoint;
import com.jme3.math.*;
import com.jme3.system.NativeLibraryLoader;
import org.json.*;
import java.nio.file.*;
import java.util.concurrent.*;

/** Real native Bullet 3D integration. The viewport never computes authoritative motion. */
final class BulletRamp implements AutoCloseable {
    static final double DT=1.0/240;
    static final String BACKEND="Minie 9.0.3 / Libbulletjme 22.0.3 / single precision";
    private static boolean loaded;
    private PhysicsSpace space;
    private volatile Thread nativeThread;
    private final ExecutorService nativeWorker=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"bullet-owned-world");t.setDaemon(true);nativeThread=t;return t;});
    private PhysicsRigidBody block;
    private final RampDefinition config;
    private final JSONObject ids;
    private volatile int tick;
    private Point2PointJoint grip;
    private String gripId="",ownerId="";
    private Vector3f gripTarget;
    static final double GRIP_FORCE_PER_AXIS_N=300;
    static synchronized void load(Path directory) throws Exception {
        if(!loaded){Files.createDirectories(directory);NativeLibraryLoader.setCustomExtractionFolder(directory.toString());NativeLibraryLoader.loadNativeLibrary("bulletjme",true);loaded=true;}
    }
    BulletRamp(RampDefinition config,JSONObject ids,Path nativeDirectory) throws Exception {
        load(nativeDirectory);this.config=config;this.ids=ids;
        nativeCall(()->{
        space=new PhysicsSpace(PhysicsSpace.BroadphaseType.DBVT);space.setGravity(new Vector3f(0,(float)-config.gravityMps2(),0));space.setAccuracy((float)DT);
        Quaternion rotation=new Quaternion().fromAngleAxis((float)-config.radians(),Vector3f.UNIT_Z);
        body(new Vector3f((float)config.lengthM()/2,.1f,(float)config.widthM()/2),0,new Vector3f(0,(float)config.centerY(),0),rotation,1);
        body(new Vector3f(20,.2f,12),0,new Vector3f(0,-.2f,0),new Quaternion(),1);
        double[] p=config.start();block=body(new Vector3f(.25f,.25f,.25f),(float)config.massKg(),new Vector3f((float)p[0],(float)p[1],(float)p[2]),rotation,(float)config.friction());
        block.setDamping(0,0);block.setSleepingThresholds(0,0);
        block.setCcdMotionThreshold(.1f);block.setCcdSweptSphereRadius(.2f);return null;});
    }
    private PhysicsRigidBody body(Vector3f half,float mass,Vector3f position,Quaternion rotation,float friction){
        var body=new PhysicsRigidBody(new BoxCollisionShape(half),mass);body.setPhysicsLocation(position);body.setPhysicsRotation(rotation);body.setFriction(friction);body.setRestitution(0);space.addCollisionObject(body);return body;
    }
    private void stepNative(){PhysicsSpace.setLocalThreadPhysicsSpace(space);space.update((float)DT,0);tick++;}
    int tick(){return tick;}
    private boolean heldNative(){return grip!=null;}
    private void grabNative(String entityId,String owner,String operation){
        if(grip!=null)throw new IllegalStateException("A body is already held");
        if(!ids.getString("block").equals(entityId))throw new IllegalArgumentException("Only the dynamic Block can be grabbed; static surfaces remain fixed");
        grip=new Point2PointJoint(block,Vector3f.ZERO,block.getPhysicsLocation());
        grip.setImpulseClamp((float)(GRIP_FORCE_PER_AXIS_N*DT));grip.setTau(.3f);grip.setDamping(1);
        space.addJoint(grip);ownerId=owner;gripId=operation;gripTarget=block.getPhysicsLocation();
    }
    private void targetNative(double[] position){if(grip==null)throw new IllegalStateException("No held body");var point=new Vector3f((float)position[0],(float)position[1],(float)position[2]);grip.setPivotInB(point);gripTarget=point;}
    private JSONObject gripNative(){
        if(grip==null)return new JSONObject().put("held",false);
        var p=gripTarget;
        return new JSONObject().put("held",true).put("id",gripId).put("ownerId",ownerId).put("entityId",ids.getString("block"))
            .put("target",new JSONArray(new float[]{p.x,p.y,p.z})).put("kind","native point-to-point constraint")
            .put("impulseClampPerAxisNs",GRIP_FORCE_PER_AXIS_N*DT).put("equivalentForcePerAxisN",GRIP_FORCE_PER_AXIS_N);
    }
    private void releaseNative(){if(grip!=null){space.removeJoint(grip);grip.destroy();grip=null;ownerId="";gripId="";}}
    private JSONObject configurationNative(){return new JSONObject().put("solver",space.getSolverType().toString()).put("iterations",space.getSolverNumIterations()).put("broadphase","DBVT").put("stepMode","external fixed dt; update(dt, 0)").put("precision","single").put("restitution",0).put("damping",0).put("friction","block coefficient multiplied by static-surface coefficient 1");}
    private JSONObject sampleNative(){
        Vector3f p=block.getPhysicsLocation(),v=block.getLinearVelocity();Quaternion q=block.getPhysicsRotation();double[] initial=config.start();
        double distance=(p.x-initial[0])*Math.cos(config.radians())-(p.y-initial[1])*Math.sin(config.radians());
        for(float f:new float[]{p.x,p.y,p.z,v.x,v.y,v.z,q.getX(),q.getY(),q.getZ(),q.getW()})if(!Float.isFinite(f))throw new IllegalStateException("Non-finite Bullet state");
        int[] contacts={0};space.contactTest(block,event->{if(event.getDistance1()<=.03f)contacts[0]++;});
        return new JSONObject().put("tick",tick).put("timeS",tick*DT).put("position",new JSONArray(new float[]{p.x,p.y,p.z})).put("rotation",new JSONArray(new float[]{q.getX(),q.getY(),q.getZ(),q.getW()}))
            .put("velocity",new JSONArray(new float[]{v.x,v.y,v.z})).put("distanceM",distance).put("speedMps",v.length()).put("contacts",contacts[0]);
    }
    private JSONObject snapshotNative(){var sample=sample();var objects=config.objects(ids);objects.getJSONObject(2).put("position",sample.getJSONArray("position")).put("rotation",sample.getJSONArray("rotation"));return sample.put("objects",objects).put("grip",grip());}
    private <T>T nativeCall(Callable<T> action){
        try{if(Thread.currentThread()==nativeThread)return action.call();return nativeWorker.submit(action).get();}
        catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("Native world call interrupted",e);}
        catch(ExecutionException e){if(e.getCause() instanceof Error error)throw error;if(e.getCause() instanceof RuntimeException error)throw error;throw new IllegalStateException(e.getCause());}
        catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}
    }
    void step(){nativeCall(()->{stepNative();return null;});}
    boolean held(){return nativeCall(this::heldNative);}
    void grab(String entityId,String owner,String operation){nativeCall(()->{grabNative(entityId,owner,operation);return null;});}
    void target(double[] position){nativeCall(()->{targetNative(position);return null;});}
    JSONObject grip(){return nativeCall(this::gripNative);}
    void release(){nativeCall(()->{releaseNative();return null;});}
    JSONObject configuration(){return nativeCall(this::configurationNative);}
    JSONObject sample(){return nativeCall(this::sampleNative);}
    JSONObject snapshot(){return nativeCall(this::snapshotNative);}
    @Override public void close(){if(!nativeWorker.isShutdown()){nativeCall(()->{releaseNative();space.destroy();return null;});nativeWorker.shutdownNow();}}
}
