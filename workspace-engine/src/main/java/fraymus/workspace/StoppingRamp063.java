package fraymus.workspace;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.*;
import org.json.*;
import java.nio.file.Path;
import java.util.concurrent.*;

/** Isolated instrumented experiment; the original runtime and its byte identity remain unchanged. */
final class StoppingRamp063 implements AutoCloseable {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final RampDefinition config;
    private PhysicsSpace space;
    private PhysicsRigidBody block,ramp;
    private int tick;

    StoppingRamp063(JSONObject definition,String variant,Path nativeDir)throws Exception {
        BulletRamp.load(nativeDir);config=RampDefinition.from(ResidualOutcome062.physical(definition));
        try {call(()->{
            space=new PhysicsSpace(PhysicsSpace.BroadphaseType.DBVT);
            space.setGravity(new Vector3f(0,(float)-config.gravityMps2(),0));space.setAccuracy((float)BulletRamp.DT);
            if(variant.equals("iterations80"))space.setSolverNumIterations(80);
            Quaternion rotation=new Quaternion().fromAngleAxis((float)-config.radians(),Vector3f.UNIT_Z);
            ramp=body(new Vector3f((float)config.lengthM()/2,.1f,(float)config.widthM()/2),0,new Vector3f(0,(float)config.centerY(),0),rotation,1);
            body(new Vector3f(20,.2f,12),0,new Vector3f(0,-.2f,0),new Quaternion(),1);
            double[] p=config.start();block=body(new Vector3f(.25f,.25f,.25f),1,new Vector3f((float)p[0],(float)p[1],(float)p[2]),rotation,(float)config.friction());
            block.setDamping(0,0);block.setSleepingThresholds(0,0);block.setCcdMotionThreshold(.1f);block.setCcdSweptSphereRadius(.2f);
            if(variant.equals("rotationLocked"))block.setAngularFactor(Vector3f.ZERO);
            double push=definition.getDouble("pushMps");block.applyCentralImpulse(new Vector3f((float)(push*Math.cos(config.radians())),(float)(-push*Math.sin(config.radians())),0));return null;
        });}catch(Exception e){close();throw e;}
    }
    private PhysicsRigidBody body(Vector3f half,float mass,Vector3f p,Quaternion q,float friction){
        var b=new PhysicsRigidBody(new BoxCollisionShape(half),mass);b.setPhysicsLocation(p);b.setPhysicsRotation(q);b.setFriction(friction);b.setRestitution(0);space.addCollisionObject(b);return b;
    }
    private <T>T call(Callable<T> action){
        try{return worker.submit(action).get();}
        catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}
        catch(ExecutionException e){throw new IllegalStateException(e.getCause());}
    }
    void step(){call(()->{PhysicsSpace.setLocalThreadPhysicsSpace(space);space.update((float)BulletRamp.DT,0);tick++;return null;});}
    JSONObject settings(){return call(()->new JSONObject().put("solver",space.getSolverType().toString()).put("iterations",space.getSolverNumIterations()).put("angularFactor",vector(block.getAngularFactor(null))).put("backend",BulletRamp.BACKEND));}
    static JSONArray vector(Vector3f p){return new JSONArray(new double[]{p.x,p.y,p.z});}
    JSONObject sample(){return call(()->{
        Vector3f p=block.getPhysicsLocation(),v=block.getLinearVelocity(),w=block.getAngularVelocity();Quaternion q=block.getPhysicsRotation();
        var separation=new JSONArray();space.contactTest(block,event->{if(event.getObjectA()==ramp||event.getObjectB()==ramp)separation.put((double)event.getDistance1());});
        return new JSONObject().put("tick",tick).put("timeS",tick*BulletRamp.DT).put("position",vector(p)).put("velocity",vector(v)).put("angularVelocity",vector(w))
            .put("rotation",new JSONArray(new double[]{q.getX(),q.getY(),q.getZ(),q.getW()})).put("contactSeparationsM",separation);
    });}
    @Override public void close(){if(!worker.isShutdown()){try{call(()->{if(space!=null)space.destroy();return null;});}finally{worker.shutdownNow();}}}
}
