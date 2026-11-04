package fraymus.workspace;

import org.json.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Versioned experiment series sharing the original native integration and verifier. */
final class PayloadSeries058 {
    private final JSONObject protocol;
    private final String phase;
    private final boolean asymmetric;
    PayloadSeries058(String phase){this(phase,false);}
    PayloadSeries058(String phase,boolean asymmetric) {
        this.asymmetric=asymmetric;
        if (!Set.of("design", "reserved").contains(phase)) throw new IllegalArgumentException("Unknown payload phase");
        this.phase=phase;
        protocol=Payload057.protocol();
        var cases=new JSONArray();
        if(asymmetric)cases=SupportDesign059.protocol().getJSONArray(phase.equals("design")?"designCases":"reservedCases");
        else if (phase.equals("design")) for (Object id:Design058.protocol().getJSONArray("trainingCaseIds")) cases.put(Payload057.specification(id.toString()));
        else cases=Design058.protocol().getJSONArray("reservedCases");
        protocol.put("cases",cases).put("version",(asymmetric?"payload-series-059-":"payload-series-058-")+phase);
    }
    JSONObject protocol(){return WorldFrames.copy(protocol);}
    private Path file(Path root,JSONObject run){UUID.fromString(run.getString("id"));return root.resolve(phase).resolve(run.getString("id")+".json");}
    private JSONObject load(Path root,JSONObject run)throws Exception {
        return validate(Payload057.load(file(root,run)),run);
    }
    private JSONObject validate(JSONObject d,JSONObject run)throws Exception {
        if(!d.getJSONObject("protocol").similar(protocol)||!d.getString("codeSha256").equals(Design058.code())||!d.getString("sourceRunSha256").equals(WorldFrames.hash(run)))throw new IllegalStateException("Payload series source, policy or implementation changed");
        if(d.getJSONArray("attempts").length()>protocol.getInt("maxAttempts"))throw new IllegalStateException("Payload attempt budget exceeded");
        var ids=new HashSet<String>();
        for(Object raw:d.getJSONArray("attempts")) {
            var a=(JSONObject)raw;UUID.fromString(a.getString("id"));
            if(!ids.add(a.getString("id"))||!Payload057.specification(protocol,a.getString("caseId")).similar(a.getJSONObject("spec"))||a.getInt("repeat")<0||a.getInt("repeat")>=2||!Set.of("running","interrupted","failed","completed").contains(a.getString("status"))||a.getJSONArray("rows").length()>protocol.getInt("ticks"))throw new IllegalStateException("Payload attempt identity or bounds changed");
        }
        return d;
    }
    private JSONObject completed(JSONObject d,JSONObject run,String id,int repeat) {
        JSONObject found=null;
        for(Object raw:d.getJSONArray("attempts")) {
            var a=(JSONObject)raw;
            if(a.getString("caseId").equals(id)&&a.getInt("repeat")==repeat&&a.getString("status").equals("completed")) {
                if(found!=null||!Payload057.verify(a,run,Payload057.specification(protocol,id),protocol).similar(a.getJSONObject("metrics")))throw new IllegalStateException("Completed payload evidence changed");
                found=a;
            }
        }
        return found;
    }
    JSONObject evaluate(Path root,JSONObject run,BooleanSupplier stopped)throws Exception {
        if(!WorldVerify.verify(run).getBoolean("passed")||!Construction051.verifyStructure(run).getBoolean("passed"))throw new IllegalStateException("Payload series requires verified released construction");
        Path file=file(root,run);
        var d=Files.exists(file)?load(root,run):new JSONObject().put("version","payload-series-058-v1").put("phase",phase).put("protocol",protocol()).put("codeSha256",Design058.code()).put("sourceRunSha256",WorldFrames.hash(run)).put("attempts",new JSONArray());
        for(Object raw:d.getJSONArray("attempts")){var a=(JSONObject)raw;if(a.getString("status").equals("running"))a.put("status","interrupted").put("reason","Previous owner ended; partial original retained");}
        for(Object raw:protocol.getJSONArray("cases"))for(int repeat=0;repeat<2;repeat++) {
            var spec=(JSONObject)raw;
            if(completed(d,run,spec.getString("id"),repeat)!=null)continue;
            if(stopped.getAsBoolean()||Thread.currentThread().isInterrupted())throw new InterruptedException("Payload series stopped between cases");
            if(d.getJSONArray("attempts").length()>=protocol.getInt("maxAttempts"))throw new IllegalStateException("Payload series attempt budget exhausted; originals retained");
            var a=new JSONObject().put("id",UUID.randomUUID().toString()).put("caseId",spec.getString("id")).put("spec",WorldFrames.copy(spec)).put("repeat",repeat).put("startedAt",Instant.now()).put("status","running").put("rows",new JSONArray());
            d.getJSONArray("attempts").put(a);Payload057.save(file,d);long began=System.nanoTime();
            try {
                Payload057.simulate(root.resolve("native"),run,spec,a,()->{if(stopped.getAsBoolean()||Thread.currentThread().isInterrupted())throw new InterruptedException("Payload series stopped; partial native rows retained");Payload057.save(file,d);return null;},protocol);
                a.put("metrics",Payload057.verify(new JSONObject(a.toString()),run,spec,protocol)).put("status","completed");
            }catch(InterruptedException e){Thread.interrupted();a.put("status","interrupted").put("reason",e.getMessage());throw e;}
            catch(Exception e){boolean stop=Thread.interrupted();a.put("status",stop?"interrupted":"failed").put("reason",e.toString());if(stop)throw new InterruptedException("Payload persistence interrupted");throw e;}
            finally{a.put("wallMillis",(System.nanoTime()-began)/1_000_000.0);Payload057.save(file,d);}
        }
        return read(root,run);
    }
    private String binding(JSONObject run,String operation){return operation+":"+WorldFrames.hash(run)+":"+Design058.code()+":"+WorldFrames.hash(protocol);}
    JSONObject read(Path root,JSONObject run)throws Exception {return Evidence059.derive(file(root,run),binding(run,"read"),d->readData(validate(d,run),run));}
    JSONObject readUncached(Path root,JSONObject run)throws Exception {return readData(load(root,run),run);}
    private JSONObject readData(JSONObject d,JSONObject run)throws Exception {var results=new JSONArray();long ticks=0;double wall=0;boolean repeatable=true,qualifies=true;
        String beamId=WorldPlan.sourceObject(run.getJSONObject("definition"),"beam").getString("id");
        for(Object raw:protocol.getJSONArray("cases")) {
            var spec=(JSONObject)raw;var first=completed(d,run,spec.getString("id"),0);var second=completed(d,run,spec.getString("id"),1);
            if(first==null||second==null)throw new IllegalStateException("Payload series unfinished");
            boolean agrees=first.getJSONObject("metrics").getBoolean("stable")==second.getJSONObject("metrics").getBoolean("stable");repeatable&=agrees;qualifies&=agrees&&first.getJSONObject("metrics").getBoolean("stable");
            var frames=new JSONArray();var chart=new JSONArray();var origin=ContactPhysics.dynamic(first.getJSONArray("nativeInitial"),beamId).getJSONArray("position");
            for(Object rr:first.getJSONArray("rows")){var row=(JSONArray)rr;if(row.getInt(0)%protocol.getInt("sampleEvery")!=0)continue;var bodies=Payload057.expand(row.getJSONArray(3));var point=new JSONArray().put(row.getInt(0)*BulletRamp.DT).put(Load055.distance(origin,ContactPhysics.dynamic(bodies,beamId).getJSONArray("position")));if(spec.getString("kind").equals("payload"))point.put(ContactPhysics.dynamic(bodies,Payload057.PAYLOAD).getJSONArray("position").getDouble(1));chart.put(point);frames.put(new JSONObject().put("tick",row.getInt(0)).put("bodies",bodies).put("contacts",Payload057.decodedContacts(row.getJSONArray(4),first.getJSONArray("objects"))));}
            results.put(new JSONObject().put("caseId",spec.getString("id")).put("spec",spec).put("firstAttemptId",first.getString("id")).put("repeatAttemptId",second.getString("id")).put("metrics",first.getJSONObject("metrics")).put("repeatMetrics",second.getJSONObject("metrics")).put("repeatAgrees",agrees).put("chart",chart).put("frames",frames).put("objects",first.getJSONArray("objects")));
        }
        for(Object raw:d.getJSONArray("attempts")){var a=(JSONObject)raw;ticks+=a.getJSONArray("rows").length();wall+=a.optDouble("wallMillis",0);}
        return new JSONObject().put("version","payload-series-058-v1").put("phase",phase).put("complete",true).put("qualifies",qualifies).put("repeatable",repeatable).put("scope",(asymmetric?SupportDesign059.protocol():Design058.protocol()).getString("scope")).put("sourceRunSha256",WorldFrames.hash(run)).put("codeSha256",Design058.code()).put("originalSha256",Payload057.evidenceHash(d)).put("attempts",d.getJSONArray("attempts").length()).put("totalTicks",ticks).put("observedWallMillis",wall).put("results",results);
    }
    JSONObject costs(Path root,JSONObject run)throws Exception {return Evidence059.derive(file(root,run),binding(run,"costs"),d->costData(validate(d,run)));}
    private JSONObject costData(JSONObject d){long ticks=0;double wall=0;for(Object raw:d.getJSONArray("attempts")){var a=(JSONObject)raw;ticks+=a.getJSONArray("rows").length();wall+=a.optDouble("wallMillis",0);}return new JSONObject().put("ticks",ticks).put("wallMillis",wall).put("attempts",d.getJSONArray("attempts").length());}
    boolean exists(Path root,JSONObject run){return Files.exists(file(root,run));}
    JSONObject originals(Path root,JSONObject run)throws Exception {return Evidence059.derive(file(root,run),binding(run,"originals"),d->originalData(validate(d,run),run));}
    private JSONObject originalData(JSONObject d,JSONObject run)throws Exception {var records=new JSONArray();var headers=new JSONObject();for(String key:d.keySet())if(!key.equals("attempts"))headers.put(key,d.get(key));
        for(Object raw:d.getJSONArray("attempts")){var a=(JSONObject)raw;if(a.getString("status").equals("completed")&&!Payload057.verify(a,run,a.getJSONObject("spec"),protocol).similar(a.getJSONObject("metrics")))throw new IllegalStateException("Original payload metrics changed");records.put(new JSONObject().put("attemptId",a.getString("id")).put("original",WorldReport.packed(a)));}
        return new JSONObject().put("header",headers).put("originalCanonicalSha256",Payload057.evidenceHash(d)).put("records",records);
    }
}
