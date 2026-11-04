package fraymus.workspace;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.math.*;
import org.json.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Independent, source-bound native payload experiments. No continuing-world writes. */
final class Payload057 {
    static final String VERSION="dynamic-payload-057-v1", PAYLOAD="payload-057";
    private Payload057() {}
    static JSONObject protocol() {
        try(var in=Payload057.class.getResourceAsStream("/construction057-protocol.json")) {
            return new JSONObject(new String(in.readAllBytes(),StandardCharsets.UTF_8).replace("\ufeff",""));
        } catch(Exception e) { throw new IllegalStateException(e); }
    }
    static String code() {
        var value=new JSONObject().put("protocol",protocol()).put("native",WorldNativeCheckpoint.identity()).put("baseline",Load055.code());
        for(Class<?> c:new Class<?>[]{Payload057.class,PayloadReport057.class})try(var in=c.getResourceAsStream(c.getSimpleName()+".class")) {
            value.put(c.getSimpleName(),MapDefinition.sha(in.readAllBytes()));
        }catch(Exception e){throw new IllegalStateException(e);}
        return WorldFrames.hash(value);
    }
    static JSONObject policy(){return new JSONObject().put("version",VERSION).put("codeSha256",code()).put("protocol",protocol());}
    private static Path file(Path root,JSONObject run){UUID.fromString(run.getString("id"));return root.resolve(run.getString("id")+".json");}
    private static JSONObject load(Path path)throws Exception {
        var e=AgentFiles042.read(path,protocol().getInt("maxBytes"));var d=e.getJSONObject("data");
        if(!e.getString("sha256").equals(WorldFrames.hash(d)))throw new IllegalStateException("Payload checkpoint checksum changed");
        return d;
    }
    private static void save(Path path,JSONObject d)throws Exception {
        if(d.toString().getBytes(StandardCharsets.UTF_8).length>protocol().getInt("maxBytes")-1000)throw new IllegalStateException("Payload history exceeds64MB; previous checkpoint retained");
        AgentFiles042.saveChecked(path,d);
    }
    static JSONObject specification(String id) {
        for(Object raw:protocol().getJSONArray("cases"))if(((JSONObject)raw).getString("id").equals(id))return WorldFrames.copy((JSONObject)raw);
        throw new IllegalArgumentException("Unknown frozen payload case");
    }
    private static void binding(JSONObject d,JSONObject run) {
        if(!d.getString("version").equals(VERSION)||!d.getString("codeSha256").equals(code())||!d.getString("sourceRunSha256").equals(WorldFrames.hash(run)))throw new IllegalStateException("Payload source/code changed");
        if(d.getJSONArray("attempts").length()>protocol().getInt("maxAttempts"))throw new IllegalStateException("Payload attempt budget changed");
        var ids=new HashSet<String>();
        for(Object raw:d.getJSONArray("attempts")) {
            var a=(JSONObject)raw;UUID.fromString(a.getString("id"));
            if(!ids.add(a.getString("id"))||!specification(a.getString("caseId")).similar(a.getJSONObject("spec"))||a.getInt("repeat")<0||a.getInt("repeat")>=2||!Set.of("running","interrupted","failed","completed").contains(a.getString("status"))||a.getJSONArray("rows").length()>protocol().getInt("ticks"))throw new IllegalStateException("Payload attempt identity, mass, position or bounds changed");
        }
    }
    static JSONObject evaluate(Path root,JSONObject run,BooleanSupplier stopped)throws Exception {
        if(!WorldVerify.verify(run).getBoolean("passed")||!Construction051.verifyStructure(run).getBoolean("passed"))throw new IllegalStateException("Payload tests require a verified released assembly");
        Path path=file(root,run);var d=Files.exists(path)?load(path):new JSONObject().put("version",VERSION).put("codeSha256",code()).put("sourceRunSha256",WorldFrames.hash(run)).put("attempts",new JSONArray());
        binding(d,run);
        for(Object raw:d.getJSONArray("attempts")){var a=(JSONObject)raw;if(a.getString("status").equals("running"))a.put("status","interrupted").put("reason","Prior owner ended; incomplete original retained");}
        for(Object raw:protocol().getJSONArray("cases"))for(int repeat=0;repeat<protocol().getInt("repeats");repeat++) {
            var spec=(JSONObject)raw;String caseId=spec.getString("id");
            if(completed(d,run,caseId,repeat)!=null)continue;
            if(stopped.getAsBoolean()||Thread.interrupted())throw new InterruptedException("Payload test stopped between cases");
            if(d.getJSONArray("attempts").length()>=protocol().getInt("maxAttempts"))throw new IllegalStateException("Payload attempt budget exhausted; originals retained");
            var a=new JSONObject().put("id",UUID.randomUUID().toString()).put("caseId",caseId).put("spec",WorldFrames.copy(spec)).put("repeat",repeat).put("startedAt",Instant.now()).put("status","running").put("rows",new JSONArray());
            d.getJSONArray("attempts").put(a);save(path,d);long began=System.nanoTime();
            try {
                simulate(root.resolve("native"),run,spec,a,()->{if(stopped.getAsBoolean()||Thread.interrupted())throw new InterruptedException("Payload test stopped; original rows retained");save(path,d);return null;});
                a.put("metrics",verify(new JSONObject(a.toString()),run,spec)).put("status","completed");
            } catch(InterruptedException e) {a.put("status","interrupted").put("reason",e.getMessage());throw e;}
            catch(Exception e) {boolean stop=Thread.interrupted();a.put("status",stop?"interrupted":"failed").put("reason",e.toString());if(stop)throw new InterruptedException("Payload checkpoint interrupted");throw e;}
            finally {a.put("wallMillis",(System.nanoTime()-began)/1_000_000.0);save(path,d);}
        }
        return read(root,run);
    }
    private static JSONObject completed(JSONObject d,JSONObject run,String id,int repeat) {
        JSONObject found=null;
        for(Object raw:d.getJSONArray("attempts")){var a=(JSONObject)raw;if(a.getString("caseId").equals(id)&&a.getInt("repeat")==repeat&&a.getString("status").equals("completed")){
            if(found!=null||!verify(a,run,specification(id)).similar(a.getJSONObject("metrics")))throw new IllegalStateException("Payload completed evidence changed");found=a;
        }}return found;
    }
    private static JSONObject beam(JSONObject run){return WorldPlan.sourceObject(run.getJSONObject("definition"),"beam");}
    static JSONArray localPoint(JSONObject run,JSONObject spec){return new JSONArray(new double[]{beam(run).getJSONArray("size").getDouble(0)*.5*spec.getDouble("halfLengthFraction"),0,0});}
    private static JSONObject payloadDefinition(JSONObject run,JSONObject spec) {
        var p=protocol();double size=p.getDouble("payloadSizeM");var original=ContactPhysics.dynamic(run.getJSONObject("final").getJSONArray("bodies"),beam(run).getString("id"));
        var local=localPoint(run,spec);local.put(1,beam(run).getJSONArray("size").getDouble(1)*.5+size*.5+spec.getDouble("dropHeightM"));
        return new JSONObject().put("id",PAYLOAD).put("size",new JSONArray(new double[]{size,size,size})).put("massKg",spec.getDouble("massKg")).put("friction",p.getDouble("payloadFriction")).put("restitution",p.getDouble("payloadRestitution")).put("position",new JSONArray(Load055.point(original,local))).put("rotation",WorldFrames.copy(original).getJSONArray("rotation")).put("velocity",new JSONArray(new int[]{0,0,0})).put("angularVelocity",new JSONArray(new int[]{0,0,0}));
    }
    private static JSONArray states(Map<String,PhysicsRigidBody> bodies,Set<String> dynamic) {
        var out=new JSONArray();for(String id:dynamic){var b=bodies.get(id);var q=b.getPhysicsRotation();out.put(new JSONObject().put("id",id).put("position",SpatialPhysics.json(b.getPhysicsLocation())).put("rotation",new JSONArray(new float[]{q.getX(),q.getY(),q.getZ(),q.getW()})).put("velocity",SpatialPhysics.json(b.getLinearVelocity())).put("angularVelocity",SpatialPhysics.json(b.getAngularVelocity())).put("massKg",b.getMass()).put("initializationPoseWrites",1));}return out;
    }
    private static JSONArray compact(JSONArray bodies) {
        var out=new JSONArray();for(Object raw:bodies){var b=(JSONObject)raw;var row=new JSONArray().put(b.getString("id"));for(String key:List.of("position","rotation","velocity","angularVelocity"))for(Object v:b.getJSONArray(key))row.put(v);out.put(row);}return out;
    }
    static JSONArray expand(JSONArray rows) {
        var out=new JSONArray();for(Object raw:rows){var row=(JSONArray)raw;if(row.length()!=14)throw new IllegalStateException("Payload body state width changed");int i=1;var b=new JSONObject().put("id",row.getString(0));for(String key:List.of("position","rotation","velocity","angularVelocity")){var a=new JSONArray();for(int k=0;k<(key.equals("rotation")?4:3);k++)a.put(row.getDouble(i++));b.put(key,a);}out.put(b);}return out;
    }
    private static JSONArray contacts(PhysicsSpace space,Map<String,PhysicsRigidBody> bodies) {
        var ids=new HashMap<Long,Integer>();int index=0;for(var e:bodies.entrySet())ids.put(e.getValue().nativeId(),index++);var out=new JSONArray();
        for(long manifold:space.listManifoldIds())for(long point:com.jme3.bullet.collision.PersistentManifolds.listPointIds(manifold)){
            double impulse=com.jme3.bullet.collision.ManifoldPoints.getAppliedImpulse(point),distance=com.jme3.bullet.collision.ManifoldPoints.getDistance1(point);
            if(impulse>1e-6&&distance<=.015)out.put(new JSONArray().put(ids.get(com.jme3.bullet.collision.PersistentManifolds.getBodyAId(manifold))).put(ids.get(com.jme3.bullet.collision.PersistentManifolds.getBodyBId(manifold))).put(distance).put(impulse));
        }return out;
    }
    static void simulate(Path nativeDir,JSONObject run,JSONObject spec,JSONObject attempt,java.util.concurrent.Callable<Void> checkpoint)throws Exception {
        BulletRamp.load(nativeDir);var space=new PhysicsSpace(PhysicsSpace.BroadphaseType.DBVT);var bodies=new LinkedHashMap<String,PhysicsRigidBody>();var dynamic=new LinkedHashSet<String>();
        var scene=run.getJSONObject("definition");var objects=new JSONArray(scene.getJSONArray("objects").toString());boolean payload=spec.getString("kind").equals("payload");if(payload)objects.put(payloadDefinition(run,spec));
        try {
            space.setGravity(new Vector3f(0,(float)-scene.getDouble("gravityMps2"),0));space.setAccuracy((float)BulletRamp.DT);
            for(Object raw:objects){var o=(JSONObject)raw;String id=o.getString("id");var size=o.getJSONArray("size");var b=new PhysicsRigidBody(new BoxCollisionShape(size.getFloat(0)/2,size.getFloat(1)/2,size.getFloat(2)/2),o.getFloat("massKg"));var pose=o;
                if(b.getMass()>0){dynamic.add(id);if(!id.equals(PAYLOAD))pose=ContactPhysics.dynamic(run.getJSONObject("final").getJSONArray("bodies"),id);b.setDamping(.04f,.2f);b.setSleepingThresholds(0,0);b.setCcdMotionThreshold(.03f);b.setCcdSweptSphereRadius((float)Math.min(size.getDouble(0),Math.min(size.getDouble(1),size.getDouble(2)))*.35f);}
                b.setPhysicsLocation(SpatialPhysics.vec(SpatialScene.vector(pose.getJSONArray("position"),30)));var q=pose.getJSONArray("rotation");b.setPhysicsRotation(new Quaternion(q.getFloat(0),q.getFloat(1),q.getFloat(2),q.getFloat(3)));b.setFriction(o.getFloat("friction"));b.setRestitution(o.getFloat("restitution"));
                if(b.getMass()>0){b.setLinearVelocity(SpatialPhysics.vec(SpatialScene.vector(pose.getJSONArray("velocity"),100)));b.setAngularVelocity(SpatialPhysics.vec(SpatialScene.vector(pose.getJSONArray("angularVelocity"),100)));}space.addCollisionObject(b);bodies.put(id,b);
            }
            attempt.put("contactEncoding","objects-index-v1").put("nativeInitial",states(bodies,dynamic)).put("objects",objects).put("gravityMps2",scene.getDouble("gravityMps2"));String beamId=beam(run).getString("id");var p=protocol();
            for(int tick=0;tick<p.getInt("ticks");tick++){
                if(Thread.interrupted())throw new InterruptedException("Payload integration interrupted");var target=bodies.get(beamId);var offset=target.getPhysicsRotation().mult(SpatialPhysics.vec(SpatialScene.vector(localPoint(run,spec),2)));var force=new Vector3f();
                if(!payload&&tick>=p.getInt("forceStartTick")&&tick<p.getInt("forceEndTickExclusive")){force.y=-spec.getFloat("forceN");target.applyForce(force,offset);}
                PhysicsSpace.setLocalThreadPhysicsSpace(space);space.update((float)BulletRamp.DT,0);
                attempt.getJSONArray("rows").put(new JSONArray().put(tick+1).put(SpatialPhysics.json(force)).put(SpatialPhysics.json(offset)).put(compact(states(bodies,dynamic))).put(contacts(space,bodies)));
                if((tick+1)%240==0)checkpoint.call();
            }
        }finally{space.destroy();}
    }
    static JSONArray decodedContacts(JSONArray rows,JSONArray objects){var out=new JSONArray();for(Object raw:rows){var r=(JSONArray)raw;if(r.length()!=4)throw new IllegalStateException("Contact width changed");var c=new JSONArray();for(int k=0;k<2;k++){Object v=r.get(k);if(!(v instanceof Number)||((Number)v).doubleValue()!=((Number)v).intValue())throw new IllegalStateException("Contact index changed");int i=((Number)v).intValue();if(i<0||i>=objects.length())throw new IllegalStateException("Unknown contact object index");c.put(objects.getJSONObject(i).getString("id"));}out.put(c.put(r.get(2)).put(r.get(3)));}return out;}
    private static boolean pair(JSONArray c,String a,String b){return c.getString(0).equals(a)&&c.getString(1).equals(b)||c.getString(1).equals(a)&&c.getString(0).equals(b);}
    static JSONObject verify(JSONObject a,JSONObject run,JSONObject spec) {
        if(!a.getString("contactEncoding").equals("objects-index-v1"))throw new IllegalStateException("Contact encoding changed");
        if(!a.getJSONObject("spec").similar(spec)||!specification(spec.getString("id")).similar(spec))throw new IllegalStateException("Payload specification changed");
        var objects=new JSONArray(run.getJSONObject("definition").getJSONArray("objects").toString());boolean payload=spec.getString("kind").equals("payload");if(payload)objects.put(payloadDefinition(run,spec));
        if(!objects.similar(a.getJSONArray("objects"))||a.getDouble("gravityMps2")!=run.getJSONObject("definition").getDouble("gravityMps2"))throw new IllegalStateException("Payload dimensions, mass, material or gravity changed");
        var ids=new HashSet<String>();var dynamic=new LinkedHashSet<String>();for(Object raw:objects){var o=(JSONObject)raw;ids.add(o.getString("id"));if(o.getDouble("massKg")>0)dynamic.add(o.getString("id"));}
        JSONArray initial=a.getJSONArray("nativeInitial");if(initial.length()!=dynamic.size())throw new IllegalStateException("Payload dynamic body missing");
        for(Object raw:objects){var o=(JSONObject)raw;if(o.getDouble("massKg")<=0)continue;String id=o.getString("id");var b=ContactPhysics.dynamic(initial,id);var expected=id.equals(PAYLOAD)?o:ContactPhysics.dynamic(run.getJSONObject("final").getJSONArray("bodies"),id);
            for(String k:List.of("position","velocity","angularVelocity"))WorldVerify.close(Load055.distance(b.getJSONArray(k),expected.getJSONArray(k)),0,2e-6,"Payload initial state changed");WorldVerify.close(WorldPlan.angle(b.getJSONArray("rotation"),expected.getJSONArray("rotation")),0,.0001,"Payload initial rotation changed");WorldVerify.close(b.getDouble("massKg"),o.getDouble("massKg"),1e-5,"Native payload mass changed");if(b.getInt("initializationPoseWrites")!=1)throw new IllegalStateException("Pose writes after initialization");
        }
        String beamId=beam(run).getString("id"),sa=WorldPlan.sourceObject(run.getJSONObject("definition"),"support-a").getString("id"),sb=WorldPlan.sourceObject(run.getJSONObject("definition"),"support-b").getString("id");var initialBeam=ContactPhysics.dynamic(initial,beamId);JSONArray previous=initial;var p=protocol();
        int tick=0,supported=0,settled=0,payloadSupported=0,firstImpact=0,freeFallTicks=0;double maxDisplacement=0,maxTilt=0,maxSpeed=0,maxPayloadSpeed=0,impulse=0,effort=0,work=0,gravityWork=0,freeFallSum=0,payloadMinimumY=Double.POSITIVE_INFINITY;
        for(Object raw:a.getJSONArray("rows")){
            var row=(JSONArray)raw;if(row.length()!=5||row.getInt(0)!=++tick)throw new IllegalStateException("Payload tick sequence changed");var now=expand(row.getJSONArray(3));if(now.length()!=dynamic.size())throw new IllegalStateException("Payload body count changed");var seen=new HashSet<String>();
            for(Object br:now){var b=(JSONObject)br;String id=b.getString("id");if(!dynamic.contains(id)||!seen.add(id))throw new IllegalStateException("Payload body identity changed");for(String k:List.of("position","velocity","angularVelocity"))SpatialScene.vector(b.getJSONArray(k),10000);var q=b.getJSONArray("rotation");double norm=0;for(Object v:q){double n=((Number)v).doubleValue();if(!Double.isFinite(n))throw new IllegalStateException("Nonfinite quaternion");norm+=n*n;}WorldVerify.close(norm,1,.0001,"Payload rotation changed");}
            var before=ContactPhysics.dynamic(previous,beamId);var b=ContactPhysics.dynamic(now,beamId);var expectedForce=new JSONArray(new double[]{0,!payload&&tick>p.getInt("forceStartTick")&&tick<=p.getInt("forceEndTickExclusive")?-spec.getDouble("forceN"):0,0});WorldVerify.close(Load055.distance(row.getJSONArray(1),expectedForce),0,1e-6,"Payload applied force changed");var expectedPoint=ObserverGeometry.sub(Load055.point(before,localPoint(run,spec)),SpatialScene.vector(before.getJSONArray("position"),10000));WorldVerify.close(Load055.distance(row.getJSONArray(2),new JSONArray(expectedPoint)),0,2e-6,"Applied load position changed");
            effort+=WorldVerify.norm(row.getJSONArray(1))*BulletRamp.DT;var dp=ObserverGeometry.sub(Load055.point(b,localPoint(run,spec)),Load055.point(before,localPoint(run,spec)));double[] f=SpatialScene.vector(row.getJSONArray(1),100);work+=f[0]*dp[0]+f[1]*dp[1]+f[2]*dp[2];
            boolean contactA=false,contactB=false,contactPayload=false,anyPayload=false;
            for(Object cr:decodedContacts(row.getJSONArray(4),objects)){var c=(JSONArray)cr;if(c.length()!=4||!ids.contains(c.getString(0))||!ids.contains(c.getString(1))||c.getString(0).equals(c.getString(1))||!Double.isFinite(c.getDouble(2))||!Double.isFinite(c.getDouble(3))||c.getDouble(2)>.015||c.getDouble(3)<=1e-6)throw new IllegalStateException("Invalid original payload contact");contactA|=pair(c,beamId,sa);contactB|=pair(c,beamId,sb);if(pair(c,beamId,PAYLOAD)){contactPayload=true;impulse+=c.getDouble(3);if(firstImpact==0)firstImpact=tick;}anyPayload|=c.getString(0).equals(PAYLOAD)||c.getString(1).equals(PAYLOAD);}
            maxDisplacement=Math.max(maxDisplacement,Load055.distance(b.getJSONArray("position"),initialBeam.getJSONArray("position")));var q=b.getJSONArray("rotation");maxTilt=Math.max(maxTilt,Math.toDegrees(Math.acos(Math.max(-1,Math.min(1,1-2*(q.getDouble(0)*q.getDouble(0)+q.getDouble(2)*q.getDouble(2)))))));
            if(payload){var body=ContactPhysics.dynamic(now,PAYLOAD);var old=ContactPhysics.dynamic(previous,PAYLOAD);double y=body.getJSONArray("position").getDouble(1);payloadMinimumY=Math.min(payloadMinimumY,y);gravityWork-=spec.getDouble("massKg")*a.getDouble("gravityMps2")*(y-old.getJSONArray("position").getDouble(1));if(firstImpact==0&&!anyPayload&&tick<=24){double acceleration=(body.getJSONArray("velocity").getDouble(1)-old.getJSONArray("velocity").getDouble(1))/BulletRamp.DT;WorldVerify.close(acceleration,-a.getDouble("gravityMps2"),.2,"Payload free fall does not match native gravity");freeFallSum+=acceleration;freeFallTicks++;}}
            if(tick>p.getInt("ticks")-p.getInt("finalWindowTicks")&&tick%p.getInt("sampleEvery")==0){settled++;if(contactA&&contactB)supported++;if(contactPayload)payloadSupported++;for(Object br:now){var body=(JSONObject)br;double speed=ObserverGeometry.norm(SpatialScene.vector(body.getJSONArray("velocity"),10000));if(body.getString("id").equals(PAYLOAD))maxPayloadSpeed=Math.max(maxPayloadSpeed,speed);else maxSpeed=Math.max(maxSpeed,speed);}}
            previous=now;
        }
        if(tick!=p.getInt("ticks")||settled!=20||payload&&(firstImpact==0||freeFallTicks<8))throw new IllegalStateException("Incomplete native payload/drop/contact evidence");
        var gates=Load055.protocol();boolean beamStable=supported==20&&maxDisplacement<=gates.getDouble("maxDisplacementM")&&maxTilt<=gates.getDouble("maxTiltDeg")&&maxSpeed<=gates.getDouble("maxSettledSpeedMps"),retained=!payload||payloadSupported==20&&maxPayloadSpeed<=gates.getDouble("maxSettledSpeedMps");
        return new JSONObject().put("stable",beamStable&&retained).put("beamStable",beamStable).put("payloadRetained",payload?retained:JSONObject.NULL).put("nativeTicks",tick).put("maxBeamDisplacementM",maxDisplacement).put("maxBeamTiltDeg",maxTilt).put("maxSettledSpeedMps",maxSpeed).put("supportedSamples",supported).put("settledSamples",settled).put("payloadSupportedSamples",payloadSupported).put("maxPayloadSettledSpeedMps",maxPayloadSpeed).put("firstPayloadBeamContactTick",firstImpact).put("payloadBeamImpulseNs",impulse).put("payloadMinimumY",payload?payloadMinimumY:JSONObject.NULL).put("freeFallTicks",freeFallTicks).put("meanFreeFallAccelerationMps2",freeFallTicks>0?freeFallSum/freeFallTicks:JSONObject.NULL).put("forceEffortNs",effort).put("appliedWorkJ",work).put("payloadGravityWorkJ",gravityWork);
    }
    static JSONObject read(Path root,JSONObject run)throws Exception {
        var d=load(file(root,run));binding(d,run);var results=new JSONArray();long ticks=0;double wall=0;boolean repeatable=true;
        for(Object raw:protocol().getJSONArray("cases")){var spec=(JSONObject)raw;var first=completed(d,run,spec.getString("id"),0);var second=completed(d,run,spec.getString("id"),1);if(first==null||second==null)throw new IllegalStateException("Payload case unfinished");boolean agrees=first.getJSONObject("metrics").getBoolean("stable")==second.getJSONObject("metrics").getBoolean("stable");repeatable&=agrees;var frames=new JSONArray();var chart=new JSONArray();String beamId=beam(run).getString("id");var origin=ContactPhysics.dynamic(first.getJSONArray("nativeInitial"),beamId).getJSONArray("position");for(Object rr:first.getJSONArray("rows")){var row=(JSONArray)rr;if(row.getInt(0)%protocol().getInt("sampleEvery")==0){var state=expand(row.getJSONArray(3));var b=ContactPhysics.dynamic(state,beamId);var cr=new JSONArray().put(row.getInt(0)*BulletRamp.DT).put(Load055.distance(origin,b.getJSONArray("position")));if(spec.getString("kind").equals("payload"))cr.put(ContactPhysics.dynamic(state,PAYLOAD).getJSONArray("position").getDouble(1));chart.put(cr);frames.put(new JSONObject().put("tick",row.getInt(0)).put("bodies",state).put("contacts",decodedContacts(row.getJSONArray(4),first.getJSONArray("objects"))));}}
            results.put(new JSONObject().put("caseId",spec.getString("id")).put("spec",spec).put("firstAttemptId",first.getString("id")).put("repeatAttemptId",second.getString("id")).put("metrics",first.getJSONObject("metrics")).put("repeatMetrics",second.getJSONObject("metrics")).put("repeatAgrees",agrees).put("chart",chart).put("frames",frames).put("objects",first.getJSONArray("objects")));
        }
        for(Object raw:d.getJSONArray("attempts")){var a=(JSONObject)raw;ticks+=a.getJSONArray("rows").length();wall+=a.optDouble("wallMillis",0);}
        return new JSONObject().put("version",VERSION).put("scope",protocol().getString("scope")).put("complete",true).put("repeatable",repeatable).put("sourceRunSha256",WorldFrames.hash(run)).put("codeSha256",code()).put("originalSha256",WorldFrames.hash(d)).put("attempts",d.getJSONArray("attempts").length()).put("totalTicks",ticks).put("observedWallMillis",wall).put("results",results);
    }
    static JSONObject original(Path root,JSONObject run)throws Exception {var d=load(file(root,run));binding(d,run);for(Object raw:d.getJSONArray("attempts")){var a=(JSONObject)raw;if(a.getString("status").equals("completed")&&!verify(a,run,specification(a.getString("caseId"))).similar(a.getJSONObject("metrics")))throw new IllegalStateException("Payload original metrics changed");}return WorldReport.packed(d).put("format","fraynix.payload-native-gzip-v1");}
}
