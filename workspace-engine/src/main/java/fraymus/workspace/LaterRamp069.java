package fraymus.workspace;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.*;
import org.json.*;
import java.nio.file.Path;
import java.util.concurrent.*;

/** Isolated instrumented experiment; the original runtime and its byte identity remain unchanged. */
final class LaterRamp069 implements AutoCloseable {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final RampDefinition config;
    private PhysicsSpace space;
    private PhysicsRigidBody block,ramp;
    private int tick;

    LaterRamp069(JSONObject definition,Path nativeDir)throws Exception {
        BulletRamp.load(nativeDir);config=RampDefinition.from(ResidualOutcome062.physical(definition));
        try {call(()->{
            space=new PhysicsSpace(PhysicsSpace.BroadphaseType.DBVT);
            space.setGravity(new Vector3f(0,(float)-config.gravityMps2(),0));space.setAccuracy((float)BulletRamp.DT);
            Quaternion rotation=new Quaternion().fromAngleAxis((float)-config.radians(),Vector3f.UNIT_Z);
            ramp=body(new Vector3f((float)config.lengthM()/2,.1f,(float)config.widthM()/2),0,new Vector3f(0,(float)config.centerY(),0),rotation,1);
            body(new Vector3f(20,.2f,12),0,new Vector3f(0,-.2f,0),new Quaternion(),1);
            double[] p=config.start();block=body(new Vector3f(.25f,.25f,.25f),1,new Vector3f((float)p[0],(float)p[1],(float)p[2]),rotation,(float)config.friction());
            block.setDamping(0,0);block.setSleepingThresholds(0,0);block.setCcdMotionThreshold(.1f);block.setCcdSweptSphereRadius(.2f);
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
    void correct(double delta){
        if(!Double.isFinite(delta))throw new IllegalArgumentException("Nonfinite correction");
        call(()->{
            double bound=tick==60?.16:tick==108?.12:0;
            if(bound==0||corrections.contains(tick)||Math.abs(delta)>bound+1e-12||spent+Math.abs(delta)>.24+1e-12)throw new IllegalStateException("Correction clock or budget changed");
            corrections.add(tick);spent+=Math.abs(delta);
            block.applyCentralImpulse(new Vector3f((float)(delta*Math.cos(config.radians())),(float)(-delta*Math.sin(config.radians())),0));return null;
        });
    }
    private final java.util.Set<Integer> corrections=new java.util.HashSet<>();
    private double spent;
    private boolean disturbed;
    static JSONObject event(String family,int amplitude){
        if(amplitude<0||amplitude>1)throw new IllegalArgumentException("Unknown amplitude");
        if(family.equals("none"))return new JSONObject().put("family","none").put("tick",-1).put("amplitude",0);
        int tick=family.equals("lift")?76:family.equals("after-second")?132:88;
        String axis=family.equals("lift")?"normal":family.equals("spin")?"world-z":"tangent";
        double impulse=switch(family){case "brake"->amplitude==0?-.08:-.14;case "lift"->amplitude==0?.24:.36;case "push","after-second"->amplitude==0?.08:.14;case "spin"->amplitude==0?.02:.04;default->throw new IllegalArgumentException("Unknown disturbance");};
        return new JSONObject().put("family",family).put("amplitude",amplitude).put("tick",tick).put("kind",family.equals("spin")?"torque":"central").put("axis",axis).put("impulse",impulse);
    }
    static double[] impulseVector(JSONObject definition,JSONObject event){
        double a=Math.toRadians(definition.getDouble("angleDeg")),m=event.getDouble("impulse");
        return switch(event.getString("axis")){
            case "tangent" -> new double[]{m*Math.cos(a),-m*Math.sin(a),0};
            case "normal" -> new double[]{m*Math.sin(a),m*Math.cos(a),0};
            case "world-z" -> new double[]{0,0,m};
            default -> throw new IllegalArgumentException("Unknown disturbance axis");
        };
    }
    void disturb(JSONObject definition,JSONObject event){
        if(!event(event.getString("family"),event.getInt("amplitude")).similar(event))throw new IllegalArgumentException("Changed disturbance");
        double[] v=impulseVector(definition,event);
        call(()->{if(disturbed||tick!=event.getInt("tick"))throw new IllegalStateException("One external event at its declared tick");disturbed=true;
            var impulse=new Vector3f((float)v[0],(float)v[1],(float)v[2]);
            if(event.getString("kind").equals("torque"))block.applyTorqueImpulse(impulse);else block.applyCentralImpulse(impulse);return null;});
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
