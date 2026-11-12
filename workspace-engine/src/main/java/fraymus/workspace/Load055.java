package fraymus.workspace;

import com.jme3.bullet.*;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.math.*;
import org.json.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** Independent native force experiments initialized from an actually released assembly. */
final class Load055 {
    static final String VERSION="independent-load-055-v1";
    static final String SCOPE="Six-second rigid-body force experiments in isolated copies of the released assembly. Downward 19.62 N is a declared 2 kg-equivalent force, not an added mass or material-strength certification. No live-world mutation.";
    private Load055() {}
    static JSONObject protocol(){return Construction055.protocol().getJSONObject("loads");}
    static String code(){try(var in=Load055.class.getResourceAsStream("Load055.class")){return WorldFrames.hash(new JSONObject().put("class",MapDefinition.sha(in.readAllBytes())).put("native",WorldNativeCheckpoint.identity()).put("protocol",protocol()));}catch(Exception e){throw new IllegalStateException(e);}}
    private static Path file(Path root,JSONObject run){UUID.fromString(run.getString("id"));return root.resolve(run.getString("id")+".json");}
    static JSONObject evaluate(Path root,JSONObject run)throws Exception {return evaluate(root,run,()->null);}
    /** Checkpoint observer runs after durable native evidence, useful for coordinated interruption. */
    static JSONObject evaluate(Path root,JSONObject run,java.util.concurrent.Callable<Void> afterCheckpoint)throws Exception {
        if(!WorldVerify.verify(run).getBoolean("passed")||!Construction051.verifyStructure(run).getBoolean("passed"))throw new IllegalStateException("Load evaluation requires verified released assembly");
        Path file=file(root,run);var data=Files.exists(file)?load(file):new JSONObject().put("version",VERSION).put("codeSha256",code()).put("sourceRunSha256",WorldFrames.hash(run)).put("attempts",new JSONArray());
        binding(data,run);
        for(Object item:data.getJSONArray("attempts")){var old=(JSONObject)item;if(old.getString("status").equals("running"))old.put("status","interrupted").put("reason","Prior owner ended; retained partial trace is not successful evidence");}
        for(Object raw:protocol().getJSONArray("cases")) {
            var c=(JSONObject)raw;boolean done=false;
            for(Object item:data.getJSONArray("attempts")){var a=(JSONObject)item;if(a.getString("caseId").equals(c.getString("id"))&&a.getString("status").equals("completed")){verifyAttempt(a,run,c);done=true;}}
            if(done)continue;if(Thread.interrupted())throw new InterruptedException("Load experiment stopped before next case");
            if(data.getJSONArray("attempts").length()>=12)throw new IllegalStateException("Load history reached its 12-attempt limit; originals retained");
            var a=new JSONObject().put("id",UUID.randomUUID().toString()).put("caseId",c.getString("id")).put("startedAt",Instant.now()).put("status","running").put("initial",WorldFrames.copy(run.getJSONObject("final"))).put("rows",new JSONArray()).put("samples",new JSONArray());
            data.getJSONArray("attempts").put(a);save(file,data);long start=System.nanoTime();
            // Verify the exact JSON numeric representation that will be retained and reloaded.
            try {simulate(root.resolve("native"),run,c,a,()->{save(file,data);afterCheckpoint.call();return null;});a.put("status","completed");a.put("metrics",verifyAttempt(new JSONObject(a.toString()),run,c));}
            catch(InterruptedException e){a.put("status","interrupted").put("reason",e.getMessage());a.put("wallMillis",(System.nanoTime()-start)/1_000_000.0);save(file,data);throw e;}
            catch(Exception e){boolean interrupted=Thread.interrupted();a.put("status",interrupted?"interrupted":"failed").put("reason",e.toString());save(file,data);if(interrupted)throw new InterruptedException("Load recording interrupted; original partial attempt retained");throw e;}
            a.put("wallMillis",(System.nanoTime()-start)/1_000_000.0);save(file,data);
        }
        return read(root,run);
    }
    private static JSONObject load(Path path)throws Exception{var envelope=AgentFiles042.read(path,32_000_000);var data=envelope.getJSONObject("data");if(!envelope.getString("format").equals("java-agent-042-v1")||!WorldFrames.hash(data).equals(envelope.getString("sha256")))throw new IllegalStateException("Original load checkpoint checksum changed");return data;}
    private static void save(Path path,JSONObject data)throws Exception{if(data.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>31_999_000)throw new IllegalStateException("Load trace exceeds its 32 MB budget; prior checkpoint retained");AgentFiles042.saveChecked(path,data);}
    private static void binding(JSONObject data,JSONObject run) {
        var ids=new HashSet<String>();if(data.getJSONArray("attempts").length()>12)throw new IllegalStateException("Load attempt budget changed");for(Object raw:data.getJSONArray("attempts")){var a=(JSONObject)raw;UUID.fromString(a.getString("id"));if(!ids.add(a.getString("id"))||!Set.of("unloaded","downward-2kg-equivalent","horizontal-overload").contains(a.getString("caseId"))||!Set.of("running","interrupted","failed","completed").contains(a.getString("status"))||a.getJSONArray("rows").length()>protocol().getInt("ticks"))throw new IllegalStateException("Load attempt identity or bounds changed");}
        if(!data.getString("version").equals(VERSION)||!data.getString("codeSha256").equals(code())||!data.getString("sourceRunSha256").equals(WorldFrames.hash(run)))throw new IllegalStateException("Original load source/code changed");
    }
    static JSONObject read(Path root,JSONObject run)throws Exception {
        var data=load(file(root,run));binding(data,run);var results=new JSONArray();int ticks=0;
        for(Object raw:protocol().getJSONArray("cases")) {
            var c=(JSONObject)raw;JSONObject completed=null;
            for(Object item:data.getJSONArray("attempts")){var a=(JSONObject)item;ticks+=a.getString("caseId").equals(c.getString("id"))?a.getJSONArray("rows").length():0;
                if(a.getString("caseId").equals(c.getString("id"))&&a.getString("status").equals("completed")){var m=verifyAttempt(a,run,c);if(!m.similar(a.getJSONObject("metrics")))throw new IllegalStateException("Original load metrics changed");completed=new JSONObject().put("caseId",c.getString("id")).put("attemptId",a.getString("id")).put("forceN",c.getJSONArray("forceN")).put("metrics",m).put("wallMillis",a.getDouble("wallMillis")).put("chart",chart(a,run));}}
            if(completed==null)throw new IllegalStateException("Load case is unfinished: "+c.getString("id"));results.put(completed);
        }
        return new JSONObject().put("version",VERSION).put("scope",SCOPE).put("complete",true).put("sourceRunSha256",data.getString("sourceRunSha256"))
            .put("codeSha256",code()).put("originalSha256",WorldFrames.hash(data)).put("attempts",data.getJSONArray("attempts").length()).put("totalTicks",ticks).put("results",results);
    }
    static JSONObject export(Path root,JSONObject run)throws Exception {read(root,run);return original(root,run);}
    static JSONObject original(Path root,JSONObject run)throws Exception{var data=load(file(root,run));binding(data,run);return WorldReport.packed(data).put("format","fraynix.load-experiments-gzip-v1");}
    private static JSONArray states(Map<String,PhysicsRigidBody> bodies,Set<String> dynamic) {
        var out=new JSONArray();for(String id:dynamic){var b=bodies.get(id);var q=b.getPhysicsRotation();out.put(new JSONObject().put("id",id).put("position",SpatialPhysics.json(b.getPhysicsLocation())).put("rotation",new JSONArray(new float[]{q.getX(),q.getY(),q.getZ(),q.getW()})).put("velocity",SpatialPhysics.json(b.getLinearVelocity())).put("angularVelocity",SpatialPhysics.json(b.getAngularVelocity())).put("initializationPoseWrites",1));}return out;
    }
    private static JSONArray contacts(PhysicsSpace space,Map<String,PhysicsRigidBody> bodies) {
        var ids=new HashMap<Long,String>();for(var e:bodies.entrySet())ids.put(e.getValue().nativeId(),e.getKey());var out=new JSONArray();
        for(long manifold:space.listManifoldIds())for(long point:com.jme3.bullet.collision.PersistentManifolds.listPointIds(manifold)){
            float impulse=com.jme3.bullet.collision.ManifoldPoints.getAppliedImpulse(point),distance=com.jme3.bullet.collision.ManifoldPoints.getDistance1(point);
            if(impulse>1e-6&&distance<=.015)out.put(new JSONObject().put("a",ids.get(com.jme3.bullet.collision.PersistentManifolds.getBodyAId(manifold))).put("b",ids.get(com.jme3.bullet.collision.PersistentManifolds.getBodyBId(manifold))).put("distanceM",distance).put("normalImpulseNs",impulse));
        }return out;
    }
    static void simulate(Path nativeDir,JSONObject run,JSONObject spec,JSONObject attempt,java.util.concurrent.Callable<Void> checkpoint)throws Exception {
        BulletRamp.load(nativeDir);var space=new PhysicsSpace(PhysicsSpace.BroadphaseType.DBVT);var bodies=new LinkedHashMap<String,PhysicsRigidBody>();var dynamic=new LinkedHashSet<String>();
        var scene=run.getJSONObject("definition");String beam=WorldPlan.sourceObject(scene,"beam").getString("id");var protocol=protocol();
        try {
            space.setGravity(new Vector3f(0,(float)-scene.getDouble("gravityMps2"),0));space.setAccuracy((float)BulletRamp.DT);
            for(Object raw:scene.getJSONArray("objects")) {
                var o=(JSONObject)raw;var size=o.getJSONArray("size");String id=o.getString("id");
                var b=new PhysicsRigidBody(new BoxCollisionShape(size.getFloat(0)/2,size.getFloat(1)/2,size.getFloat(2)/2),o.getFloat("massKg"));var pose=o;
                if(b.getMass()>0){dynamic.add(id);pose=ContactPhysics.dynamic(run.getJSONObject("final").getJSONArray("bodies"),id);b.setDamping(.04f,.2f);b.setSleepingThresholds(0,0);b.setCcdMotionThreshold(.03f);b.setCcdSweptSphereRadius((float)Math.min(size.getDouble(0),Math.min(size.getDouble(1),size.getDouble(2)))*.35f);}
                b.setPhysicsLocation(SpatialPhysics.vec(SpatialScene.vector(pose.getJSONArray("position"),30)));var q=pose.getJSONArray("rotation");b.setPhysicsRotation(new Quaternion(q.getFloat(0),q.getFloat(1),q.getFloat(2),q.getFloat(3)));b.setFriction(o.getFloat("friction"));b.setRestitution(o.getFloat("restitution"));
                if(b.getMass()>0){b.setLinearVelocity(SpatialPhysics.vec(SpatialScene.vector(pose.getJSONArray("velocity"),100)));b.setAngularVelocity(SpatialPhysics.vec(SpatialScene.vector(pose.getJSONArray("angularVelocity"),100)));}space.addCollisionObject(b);bodies.put(id,b);
            }
            var initial=states(bodies,dynamic);for(Object raw:initial){var b=(JSONObject)raw;var old=ContactPhysics.dynamic(run.getJSONObject("final").getJSONArray("bodies"),b.getString("id"));for(String k:List.of("position","velocity","angularVelocity"))WorldVerify.close(distance(b.getJSONArray(k),old.getJSONArray(k)),0,2e-6,"Load boundary changed");WorldVerify.close(WorldPlan.angle(b.getJSONArray("rotation"),old.getJSONArray("rotation")),0,.0001,"Load boundary orientation changed");}
            attempt.put("nativeInitial",initial);
            for(int tick=0;tick<protocol.getInt("ticks");tick++) {
                if(Thread.interrupted())throw new InterruptedException("Load attempt interrupted; original partial trace retained");
                var before=states(bodies,dynamic);var force=tick>=protocol.getInt("startTick")&&tick<protocol.getInt("endTickExclusive")?SpatialPhysics.vec(SpatialScene.vector(spec.getJSONArray("forceN"),100)):new Vector3f();
                var appliedOffset=new Vector3f();if(spec.has("localPointM")){appliedOffset=bodies.get(beam).getPhysicsRotation().mult(SpatialPhysics.vec(SpatialScene.vector(spec.getJSONArray("localPointM"),2)));bodies.get(beam).applyForce(force,appliedOffset);}else bodies.get(beam).applyCentralForce(force);PhysicsSpace.setLocalThreadPhysicsSpace(space);space.update((float)BulletRamp.DT,0);
                var after=states(bodies,dynamic);var deltas=new JSONArray();double work=0;
                for(Object raw:after){var b=(JSONObject)raw;String id=b.getString("id");var old=ContactPhysics.dynamic(before,id);double[] dp=ObserverGeometry.sub(SpatialScene.vector(b.getJSONArray("position"),10000),SpatialScene.vector(old.getJSONArray("position"),10000));deltas.put(new JSONArray().put(id).put(new JSONArray(dp)).put(b.getJSONArray("rotation")));if(id.equals(beam)){if(spec.has("localPointM"))dp=ObserverGeometry.sub(point(b,spec.getJSONArray("localPointM")),point(old,spec.getJSONArray("localPointM")));work=force.x*dp[0]+force.y*dp[1]+force.z*dp[2];}}
                var integration=new JSONArray().put(tick+1).put(SpatialPhysics.json(force)).put(deltas).put(work);if(spec.has("localPointM"))integration.put(SpatialPhysics.json(appliedOffset));attempt.getJSONArray("rows").put(integration);
                if((tick+1)%protocol.getInt("sampleEvery")==0)attempt.getJSONArray("samples").put(new JSONObject().put("tick",tick+1).put("bodies",after).put("contacts",contacts(space,bodies)));
                if((tick+1)%240==0)checkpoint.call();
            }
        } finally {space.destroy();}
    }
    static JSONObject verifyAttempt(JSONObject a,JSONObject run,JSONObject spec) {
        if(!a.getJSONObject("initial").similar(run.getJSONObject("final")))throw new IllegalStateException("Load source pose/velocity changed");
        var p=protocol();var scene=run.getJSONObject("definition");String beam=WorldPlan.sourceObject(scene,"beam").getString("id");var supports=Set.of(WorldPlan.sourceObject(scene,"support-a").getString("id"),WorldPlan.sourceObject(scene,"support-b").getString("id"));
        var positions=new LinkedHashMap<String,double[]>();var rotations=new HashMap<String,JSONArray>();
        for(Object raw:a.getJSONArray("nativeInitial")){var b=(JSONObject)raw;var original=ContactPhysics.dynamic(run.getJSONObject("final").getJSONArray("bodies"),b.getString("id"));for(String k:List.of("position","velocity","angularVelocity"))WorldVerify.close(distance(b.getJSONArray(k),original.getJSONArray(k)),0,2e-6,"Native load initialization changed");WorldVerify.close(WorldPlan.angle(b.getJSONArray("rotation"),original.getJSONArray("rotation")),0,.0001,"Native load orientation changed");positions.put(b.getString("id"),SpatialScene.vector(b.getJSONArray("position"),10000));rotations.put(b.getString("id"),b.getJSONArray("rotation"));}
        if(positions.size()!=run.getJSONObject("final").getJSONArray("bodies").length())throw new IllegalStateException("Load body missing");
        int tick=0,index=0,settled=0,supported=0;double effort=0,work=0,maxDisplacement=0,maxTilt=0,maxSpeed=0;var initialBeam=ContactPhysics.dynamic(a.getJSONArray("nativeInitial"),beam).getJSONArray("position");
        for(Object raw:a.getJSONArray("rows")) {
            var row=(JSONArray)raw;if(row.getInt(0)!=++tick)throw new IllegalStateException("Missing load integration tick");var force=row.getJSONArray(1);boolean active=tick-1>=p.getInt("startTick")&&tick-1<p.getInt("endTickExclusive");
            for(int k=0;k<3;k++)WorldVerify.close(force.getDouble(k),active?spec.getJSONArray("forceN").getDouble(k):0,1e-5,"Unauthorized load force");
            var seen=new HashSet<String>();double observedWork=0;double[] beforePoint=spec.has("localPointM")?point(new JSONObject().put("position",new JSONArray(positions.get(beam))).put("rotation",rotations.get(beam)),spec.getJSONArray("localPointM")):null;double[] beforePosition=positions.get(beam).clone();
            for(Object value:row.getJSONArray(2)){var d=(JSONArray)value;String id=d.getString(0);if(!positions.containsKey(id)||!seen.add(id))throw new IllegalStateException("Foreign load body");var dp=SpatialScene.vector(d.getJSONArray(1),1);for(int k=0;k<3;k++){positions.get(id)[k]+=dp[k];if(id.equals(beam))observedWork+=force.getDouble(k)*dp[k];}WorldVerify.quaternion(d.getJSONArray(2));rotations.put(id,d.getJSONArray(2));}
            if(seen.size()!=positions.size())throw new IllegalStateException("Load trace lost body");if(beforePoint!=null){var afterPoint=point(new JSONObject().put("position",new JSONArray(positions.get(beam))).put("rotation",rotations.get(beam)),spec.getJSONArray("localPointM"));observedWork=0;for(int k=0;k<3;k++){WorldVerify.close(row.getJSONArray(4).getDouble(k),beforePoint[k]-beforePosition[k],2e-6,"Load application point changed");observedWork+=force.getDouble(k)*(afterPoint[k]-beforePoint[k]);}}WorldVerify.close(observedWork,row.getDouble(3),beforePoint==null?1e-7:2e-5,"Load work differs");work+=observedWork;effort+=WorldVerify.norm(force)*BulletRamp.DT;
            maxDisplacement=Math.max(maxDisplacement,distance(new JSONArray(positions.get(beam)),initialBeam));var q=rotations.get(beam);maxTilt=Math.max(maxTilt,Math.toDegrees(Math.acos(Math.max(-1,Math.min(1,1-2*(q.getDouble(0)*q.getDouble(0)+q.getDouble(2)*q.getDouble(2)))))));
            if(tick%p.getInt("sampleEvery")==0) {
                if(index>=a.getJSONArray("samples").length())throw new IllegalStateException("Missing load sample");var sample=a.getJSONArray("samples").getJSONObject(index++);if(sample.getInt("tick")!=tick||sample.getJSONArray("bodies").length()!=positions.size())throw new IllegalStateException("Load sample clock/bodies changed");
                for(String id:positions.keySet()){var b=ContactPhysics.dynamic(sample.getJSONArray("bodies"),id);WorldVerify.close(distance(new JSONArray(positions.get(id)),b.getJSONArray("position")),0,2e-6,"Load sample displacement changed");WorldVerify.close(WorldPlan.angle(rotations.get(id),b.getJSONArray("rotation")),0,.0001,"Load sample rotation changed");if(b.getInt("initializationPoseWrites")!=1)throw new IllegalStateException("Load pose write after initialization");}
                if(tick>p.getInt("ticks")-p.getInt("finalWindowTicks")){settled++;var found=new HashSet<String>();for(Object cr:sample.getJSONArray("contacts")){var c=(JSONObject)cr;String one=c.getString("a"),two=c.getString("b");if(c.getDouble("normalImpulseNs")<=1e-6||c.getDouble("distanceM")>.015)throw new IllegalStateException("Invalid native load contact");if(one.equals(beam)&&supports.contains(two))found.add(two);if(two.equals(beam)&&supports.contains(one))found.add(one);}if(found.size()==2)supported++;for(Object br:sample.getJSONArray("bodies"))maxSpeed=Math.max(maxSpeed,ObserverGeometry.norm(SpatialScene.vector(((JSONObject)br).getJSONArray("velocity"),10000)));}
            }
        }
        if(tick!=p.getInt("ticks")||index!=a.getJSONArray("samples").length()||settled!=p.getInt("finalWindowTicks")/p.getInt("sampleEvery"))throw new IllegalStateException("Load experiment incomplete");
        boolean stable=supported==settled&&maxDisplacement<=p.getDouble("maxDisplacementM")&&maxTilt<=p.getDouble("maxTiltDeg")&&maxSpeed<=p.getDouble("maxSettledSpeedMps");
        return new JSONObject().put("stable",stable).put("nativeTicks",tick).put("forceEffortNs",effort).put("workJ",work).put("maxBeamDisplacementM",maxDisplacement).put("maxBeamTiltDeg",maxTilt).put("maxSettledSpeedMps",maxSpeed).put("supportedSamples",supported).put("settledSamples",settled);
    }
    static JSONArray chart(JSONObject attempt,JSONObject run) {
        String beam=WorldPlan.sourceObject(run.getJSONObject("definition"),"beam").getString("id");var initial=ContactPhysics.dynamic(attempt.getJSONArray("nativeInitial"),beam).getJSONArray("position");var out=new JSONArray();
        for(Object raw:attempt.getJSONArray("samples")){var s=(JSONObject)raw;out.put(new JSONArray().put(s.getInt("tick")*BulletRamp.DT).put(distance(initial,ContactPhysics.dynamic(s.getJSONArray("bodies"),beam).getJSONArray("position"))));}return out;
    }
    static double distance(JSONArray a,JSONArray b){return ObserverGeometry.norm(ObserverGeometry.sub(SpatialScene.vector(a,10000),SpatialScene.vector(b,10000)));}
    static double[] point(JSONObject body,JSONArray local){var q=body.getJSONArray("rotation");double x=q.getDouble(0),y=q.getDouble(1),z=q.getDouble(2),w=q.getDouble(3),a=local.getDouble(0),b=local.getDouble(1),c=local.getDouble(2);var p=body.getJSONArray("position");return new double[]{p.getDouble(0)+(1-2*y*y-2*z*z)*a+2*(x*y-z*w)*b+2*(x*z+y*w)*c,p.getDouble(1)+2*(x*y+z*w)*a+(1-2*x*x-2*z*z)*b+2*(y*z-x*w)*c,p.getDouble(2)+2*(x*z-y*w)*a+2*(y*z+x*w)*b+(1-2*x*x-2*y*y)*c};}
    static String label(JSONObject result) {var m=result.getJSONObject("metrics");return String.format(Locale.ROOT,"%s · %s · displacement %.4f m · tilt %.3f deg · supported %d/%d samples",result.getString("caseId"),m.getBoolean("stable")?"stable":"unstable",m.getDouble("maxBeamDisplacementM"),m.getDouble("maxBeamTiltDeg"),m.getInt("supportedSamples"),m.getInt("settledSamples"));}
}
