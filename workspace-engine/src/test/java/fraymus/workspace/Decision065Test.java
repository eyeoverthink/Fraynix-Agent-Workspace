package fraymus.workspace;
import org.junit.Test;
import static org.junit.Assert.*;
import org.json.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class Decision065Test {
    static JSONObject task(double center,double half){return new JSONObject().put("angleDeg",20).put("frictionRatio",1.05).put("centerM",center).put("halfWidthM",half);}
    @Test public void publishedCalibrationIsExactAndRecomputed()throws Exception{
        JSONObject original;try(var in=new GZIPInputStream(Files.newInputStream(Path.of("../docs/readmes/064/original/native064-final/original-experiment.json.gz")))){original=new JSONObject(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));}
        var bundle=Calibration065.bundle();for(String k:List.of("id","manifest064","calibration064","intervalModel064","intervalModelSha256064","evaluation064"))assertEquals(WorldFrames.hash(new JSONObject().put("value",original.get(k))),WorldFrames.hash(new JSONObject().put("value",bundle.get(k))));
        assertEquals(original.getJSONObject("intervalModel064").getDouble("radiusM"),Calibration065.radius(),0);assertEquals(Calibration065.info().getString("modelSha256"),original.getString("intervalModelSha256064"));
        bundle.getJSONObject("intervalModel064").put("radiusM",.5);assertThrows(IllegalStateException.class,()->Calibration065.verify(bundle));
        var info=Calibration065.info();info.put("radiusM",10);assertTrue(Calibration065.radius()<.1);
    }
    @Test public void policyFitsIntervalsAndSharesDomainFence(){
        var t=task(Stopping063.analytic(Stopping063.definition(20,1.05,.24),.75),.045);var plan=DecisionPolicy065.plan(t);
        for(String m:DecisionPolicy065.METHODS){var d=plan.getJSONObject("decisions").getJSONObject(m);assertTrue(d.getBoolean("execute"));assertEquals(.24,d.getDouble("speedMps"),0);}
        t.put("halfWidthM",.012);plan=DecisionPolicy065.plan(t);assertTrue(plan.getJSONObject("decisions").getJSONObject("simple").getBoolean("execute"));assertFalse(plan.getJSONObject("decisions").getJSONObject("fixed").getBoolean("execute"));assertFalse(plan.getJSONObject("decisions").getJSONObject("pooled").getBoolean("execute"));
        for(var outside:List.of(task(.1,.03).put("angleDeg",10),task(.1,.03).put("angleDeg",34),task(.1,.03).put("frictionRatio",1)))for(String m:DecisionPolicy065.METHODS)assertFalse(DecisionPolicy065.plan(outside).getJSONObject("decisions").getJSONObject(m).getBoolean("execute"));
        assertThrows(IllegalArgumentException.class,()->DecisionPolicy065.plan(task(.1,.03).put("override",true)));assertThrows(IllegalArgumentException.class,()->DecisionPolicy065.plan(task(.1,.0001)));
    }
    @Test public void utilityCountsMissesAndRefusalsWithoutInventingEndpoints(){
        var t=task(.1,.02);var go=new JSONObject().put("method","fixed").put("execute",true).put("lowerM",.08).put("upperM",.12);var stop=new JSONObject().put("execute",false);
        var record=new JSONObject().put("summary",new JSONObject().put("endpointM",.11));assertEquals(.98,DecisionPolicy065.outcome(t,go,record).getDouble("utility"),0);
        record.getJSONObject("summary").put("endpointM",.2);assertEquals(-2.02,DecisionPolicy065.outcome(t,go,record).getDouble("utility"),0);
        var refusal=DecisionPolicy065.outcome(t,stop,null);assertEquals(-.15,refusal.getDouble("utility"),0);assertFalse(refusal.has("endpointM"));assertThrows(IllegalStateException.class,()->DecisionPolicy065.outcome(t,stop,record));assertThrows(IllegalStateException.class,()->DecisionPolicy065.outcome(t,go,null));
        var results=new JSONObject().put("simple",new JSONObject().put("outcome",DecisionPolicy065.outcome(t,go,record))).put("fixed",new JSONObject().put("outcome",refusal)).put("pooled",new JSONObject().put("outcome",refusal));
        var e=DecisionPolicy065.evaluate(new JSONArray().put(new JSONObject().put("results",results)));assertEquals(1.87,e.getJSONObject("pairedPooledMinus").getJSONObject("simple").getDouble("meanDelta"),1e-12);assertEquals(0,e.getJSONObject("methods").getJSONObject("pooled").getInt("successes"));assertTrue(e.getJSONObject("methods").getJSONObject("pooled").isNull("conditionalSuccessRate"));
    }
    @Test public void gateRequiresBothComparisonsAndRealTaskSuccess(){
        var e=new JSONObject().put("pairedPooledMinus",new JSONObject().put("simple",new JSONObject().put("meanDelta",.1)).put("fixed",new JSONObject().put("meanDelta",.05))).put("methods",new JSONObject().put("pooled",new JSONObject().put("allTaskMissRate",.1).put("allTaskSuccessRate",.5)).put("simple",new JSONObject().put("allTaskMissRate",.1)));
        assertTrue(DecisionStudy065.qualifies(e));var p=e.getJSONObject("methods").getJSONObject("pooled");p.put("allTaskSuccessRate",0);assertFalse(DecisionStudy065.qualifies(e));p.put("allTaskSuccessRate",.5).put("allTaskMissRate",.2);assertFalse(DecisionStudy065.qualifies(e));p.put("allTaskMissRate",.1);e.getJSONObject("pairedPooledMinus").getJSONObject("fixed").put("meanDelta",0);assertFalse(DecisionStudy065.qualifies(e));
        assertEquals("agent",ChatPolicy044.route("Report the decision study and target window results",new JSONObject(),false));var original=new JSONObject().put("recentWitnesses",new JSONArray().put("retained")).put("decision065",new JSONObject().put("lastCompleted",new JSONObject().put("qualified",false)));assertFalse(ContactContext064.project(original).has("recentWitnesses"));assertTrue(original.has("recentWitnesses"));
    }
    @Test public void freshPartitionsHaveNoOldNativeDefinitions(){
        var old=new HashSet<String>();for(String phase:List.of("calibration","reserved","stress"))for(Object c:ContactStudy064.cases(phase))old.add(WorldFrames.hash((JSONObject)c));
        var seen=new HashSet<String>();for(Object raw:DecisionStudy065.cases(false)){var t=(JSONObject)raw;assertTrue(seen.add(WorldFrames.hash(t)));for(double v:DecisionPolicy065.SPEEDS){var c=DecisionPolicy065.definition(t,v);Stopping063.queryDomain(c);assertFalse(old.contains(WorldFrames.hash(c)));}}
        assertEquals(60,seen.size());assertEquals(12,DecisionStudy065.cases(true).length());for(Object raw:DecisionStudy065.cases(true))assertFalse(DecisionPolicy065.plan((JSONObject)raw).getBoolean("supported"));
    }
    @Test public void nativeDecisionsColdHistoryStopAndTampering()throws Exception{
        var root=Files.createTempDirectory("fraynix-decision065-");String id,hash;JSONObject job;
        try(var b=new BrainService(root)){
            id=b.start(new JSONObject().put("kind","decision-value")).getString("id");waitFor(b);job=b.read(id);assertEquals(job.optString("reason"),"completed",job.getString("status"));DecisionStudy065.audit(job);hash=WorldFrames.hash(job);Files.writeString(root.resolve("original-experiment.json"),job.toString());System.out.println("EDITION065_NATIVE_ORIGINALS="+root);System.out.println("DECISION065="+job.getJSONObject("evaluation065"));
            assertEquals(216,job.getInt("progress"));assertTrue(job.getInt("nativeExecutions")>=60);assertTrue(job.getInt("nativeExecutions")<=180);assertEquals(1,b.activePolicy().getInt("version"));
            for(Object raw:job.getJSONArray("shifted065"))for(String method:DecisionPolicy065.METHODS)assertFalse(((JSONObject)raw).getJSONObject("results").getJSONObject(method).has("record"));
            var query=new JSONObject().put("task",task(.1,.03));Files.writeString(root.resolve("plan.json"),b.decisionPlan065(query).toString());var trace=b.decisionTrace065(new JSONObject().put("jobId",id).put("index",0).put("shifted",false));assertEquals(46,trace.getJSONObject("results").getJSONObject("simple").getJSONArray("series").length());
            b.decisionState065().getJSONObject("lastCompleted").getJSONObject("evaluation").put("qualified","changed");assertEquals(job.getJSONObject("evaluation065").getBoolean("qualified"),b.decisionState065().getJSONObject("lastCompleted").getJSONObject("evaluation").getBoolean("qualified"));
            tamper(job,j->j.getJSONObject("evaluation065").put("qualified",!j.getJSONObject("evaluation065").getBoolean("qualified")));
            tamper(job,j->j.getJSONArray("tasks065").getJSONObject(0).getJSONObject("plan").getJSONObject("decisions").getJSONObject("simple").put("speedMps",.39));
            tamper(job,j->j.getJSONArray("tasks065").getJSONObject(0).put("plannedAt","2099-01-01T00:00:00Z"));
            tamper(job,j->j.getJSONArray("shifted065").getJSONObject(0).getJSONObject("results").getJSONObject("simple").put("record",JSONObject.NULL));
            tamper(job,j->j.put("nativeExecutions",999));tamper(job,j->j.getJSONArray("tasks065").put(1,j.getJSONArray("tasks065").getJSONObject(0)));
        }
        try(var b=new BrainService(root)){
            assertTrue(new JSONObject(Files.readString(root.resolve("plan.json"))).similar(b.decisionPlan065(new JSONObject().put("task",task(.1,.03)))));assertEquals(hash,WorldFrames.hash(b.read(id)));String stopped=b.start(new JSONObject().put("kind","decision-value")).getString("id");b.cancel(stopped);waitFor(b);assertEquals("cancelled",b.read(stopped).getString("status"));assertEquals(id,b.decisionState065().getJSONObject("lastCompleted").getString("jobId"));assertEquals(hash,WorldFrames.hash(b.read(id)));
        }
    }
    static void waitFor(BrainService b)throws Exception{long end=System.nanoTime()+600_000_000_000L;while(b.state().getBoolean("busy")&&System.nanoTime()<end)Thread.sleep(500);assertFalse(b.state().getBoolean("busy"));}
    static void tamper(JSONObject original,java.util.function.Consumer<JSONObject> edit){var copy=WorldFrames.copy(original);edit.accept(copy);assertThrows(IllegalStateException.class,()->DecisionStudy065.audit(copy));}
}
