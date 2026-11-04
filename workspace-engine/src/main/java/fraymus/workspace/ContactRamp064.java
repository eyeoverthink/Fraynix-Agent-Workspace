package fraymus.workspace;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.*;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.*;
import org.json.*;
import java.nio.file.Path;
import java.util.concurrent.*;

/** Reads the solver's existing manifold cache on its owning thread, after each step. */
final class ContactRamp064 implements AutoCloseable {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final RampDefinition config;
    private final double dt,clearance;
    private PhysicsSpace space;
    private PhysicsRigidBody block,ramp;
    private int tick;
    ContactRamp064(JSONObject definition,String variant,Path nativeDir)throws Exception {
        if(!ContactStudy064.VARIANTS.contains(variant))throw new IllegalArgumentException("Unknown contact intervention");
        BulletRamp.load(nativeDir);config=RampDefinition.from(ResidualOutcome062.physical(definition));
        dt=variant.equals("halfStep")?1.0/480:BulletRamp.DT;clearance=variant.equals("zeroClearance")?0:.001;
        try {call(()->{
            space=new PhysicsSpace(PhysicsSpace.BroadphaseType.DBVT);
            space.setGravity(new Vector3f(0,(float)-config.gravityMps2(),0));space.setAccuracy((float)dt);
            Quaternion rotation=new Quaternion().fromAngleAxis((float)-config.radians(),Vector3f.UNIT_Z);
            ramp=body(new Vector3f((float)config.lengthM()/2,.1f,(float)config.widthM()/2),0,new Vector3f(0,(float)config.centerY(),0),rotation,1);
            body(new Vector3f(20,.2f,12),0,new Vector3f(0,-.2f,0),new Quaternion(),1);
            double[] p=config.start();double shift=clearance-.001;
            block=body(new Vector3f(.25f,.25f,.25f),1,new Vector3f((float)(p[0]+shift*Math.sin(config.radians())),(float)(p[1]+shift*Math.cos(config.radians())),(float)p[2]),rotation,(float)config.friction());
            block.setDamping(0,0);block.setSleepingThresholds(0,0);block.setCcdMotionThreshold(.1f);block.setCcdSweptSphereRadius(.2f);
            if(variant.equals("rotationLocked"))block.setAngularFactor(Vector3f.ZERO);
            double push=definition.getDouble("pushMps");block.applyCentralImpulse(new Vector3f((float)(push*Math.cos(config.radians())),(float)(-push*Math.sin(config.radians())),0));return null;
        });}catch(Exception e){close();throw e;}
    }
    private PhysicsRigidBody body(Vector3f half,float mass,Vector3f p,Quaternion q,float friction){
        var b=new PhysicsRigidBody(new BoxCollisionShape(half),mass);b.setPhysicsLocation(p);b.setPhysicsRotation(q);b.setFriction(friction);b.setRestitution(0);space.addCollisionObject(b);return b;
    }
    private <T>T call(Callable<T> action){try{return worker.submit(action).get();}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}catch(ExecutionException e){throw new IllegalStateException(e.getCause());}}
    void step(){call(()->{PhysicsSpace.setLocalThreadPhysicsSpace(space);space.update((float)dt,0);tick++;return null;});}
    JSONObject settings(){return call(()->new JSONObject().put("dt",dt).put("initialClearanceM",clearance).put("solver",space.getSolverType().toString()).put("iterations",space.getSolverNumIterations()).put("mode",space.getSolverInfo().mode()).put("splitImpulse",space.getSolverInfo().isSplitImpulseEnabled()).put("angularFactor",vector(block.getAngularFactor(null))).put("backend",BulletRamp.BACKEND));}
    static JSONArray vector(Vector3f v){return new JSONArray(new double[]{v.x,v.y,v.z});}
    JSONObject sample(){return call(()->{
        var points=new JSONArray();var separations=new JSONArray();
        for(long manifold:space.listManifoldIds()){
            long a=PersistentManifolds.getBodyAId(manifold),b=PersistentManifolds.getBodyBId(manifold);
            if(!((a==block.nativeId()&&b==ramp.nativeId())||(b==block.nativeId()&&a==ramp.nativeId())))continue;
            for(long point:PersistentManifolds.listPointIds(manifold)){
                var normal=new Vector3f();var d1=new Vector3f();var d2=new Vector3f();var pa=new Vector3f();var pb=new Vector3f();
                ManifoldPoints.getNormalWorldOnB(point,normal);ManifoldPoints.getLateralFrictionDir1(point,d1);ManifoldPoints.getLateralFrictionDir2(point,d2);ManifoldPoints.getPositionWorldOnA(point,pa);ManifoldPoints.getPositionWorldOnB(point,pb);
                double separation=ManifoldPoints.getDistance1(point);separations.put(separation);
                points.put(new JSONObject().put("blockIsA",a==block.nativeId()).put("separationM",separation).put("lifetime",ManifoldPoints.getLifeTime(point)).put("normal",vector(normal)).put("lateral1",vector(d1)).put("lateral2",vector(d2)).put("positionA",vector(pa)).put("positionB",vector(pb)).put("normalImpulseNs",(double)ManifoldPoints.getAppliedImpulse(point)).put("lateralImpulse1Ns",(double)ManifoldPoints.getAppliedImpulseLateral1(point)).put("lateralImpulse2Ns",(double)ManifoldPoints.getAppliedImpulseLateral2(point)).put("friction",(double)ManifoldPoints.getCombinedFriction(point)));
            }
        }
        var q=block.getPhysicsRotation();return new JSONObject().put("tick",tick).put("timeS",tick*dt).put("position",vector(block.getPhysicsLocation())).put("velocity",vector(block.getLinearVelocity())).put("angularVelocity",vector(block.getAngularVelocity())).put("rotation",new JSONArray(new double[]{q.getX(),q.getY(),q.getZ(),q.getW()})).put("contacts",points).put("contactSeparationsM",separations);
    });}
    @Override public void close(){if(!worker.isShutdown()){try{call(()->{if(space!=null)space.destroy();return null;});}finally{worker.shutdownNow();}}}
}
