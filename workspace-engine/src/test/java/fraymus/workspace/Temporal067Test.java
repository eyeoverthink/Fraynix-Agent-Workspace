package fraymus.workspace;
import org.junit.Test;
import static org.junit.Assert.*;
import org.json.*;
import java.nio.file.*;
import java.util.*;

public class Temporal067Test {
    static JSONObject task(double center,double width){return Recovery066Test.task(center,width);}
    static JSONArray prefix(JSONObject c){
        var config=RampDefinition.from(ResidualOutcome062.physical(c));var a=new JSONArray();
        for(int i=0;i<16;i++)a.put(new JSONObject().put("tick",i*4).put("timeS",i/60.0)
            .put("position",new JSONArray(config.start())).put("velocity",new JSONArray(new double[]{.2*Math.cos(config.radians()),-.2*Math.sin(config.radians()),0}))
            .put("angularVelocity",new JSONArray(new int[]{0,0,0})).put("rotation",new JSONArray(new double[]{0,0,-Math.sin(config.radians()/2),Math.cos(config.radians()/2)}))
            .put("contactSeparationsM",new JSONArray().put(.001)));
        return a;
    }
    @Test public void temporalEvidenceHandlesDropoutButRefusesLostOrUnsafeSupport(){
        var c=Stopping063.definition(20,1.05,.24);var a=prefix(c);
        assertEquals(.001,TemporalContact067.clearance(c,a.getJSONObject(0)),1e-12);
        a.getJSONObject(15).put("contactSeparationsM",new JSONArray());
        var s=TemporalContact067.state(c,a);assertEquals(0,s.getInt("nearPoints"));assertTrue(s.getJSONObject("contactWindow").getBoolean("eligible"));
        assertEquals(2,s.getJSONObject("contactWindow").getInt("contactSamples"));assertEquals(3,s.getJSONObject("contactWindow").getJSONArray("samples").length());
        var p=TemporalContact067.plan(task(.13,.045),s);assertTrue(p.getBoolean("correct"));assertEquals(0,p.getJSONObject("state").getInt("nearPoints"));
        var lost=new JSONArray(a.toString());for(int i=13;i<16;i++)lost.getJSONObject(i).put("contactSeparationsM",new JSONArray());
        assertFalse(TemporalContact067.plan(task(.13,.045),TemporalContact067.state(c,lost)).getBoolean("correct"));
        var lifted=new JSONArray(a.toString());lifted.getJSONObject(14).getJSONArray("position").put(1,lifted.getJSONObject(14).getJSONArray("position").getDouble(1)+.1);
        assertFalse(TemporalContact067.state(c,lifted).getJSONObject("contactWindow").getBoolean("geometryEligible"));
        var bad=new JSONArray(a.toString());bad.getJSONObject(13).put("angularVelocity",new JSONArray(new int[]{0,0,3}));
        assertFalse(TemporalContact067.state(c,bad).getJSONObject("contactWindow").getBoolean("eligible"));
        assertThrows(IllegalArgumentException.class,()->TemporalContact067.state(c,new JSONArray(a.toString()).put(a.get(0))));
        var identity=WorldFrames.copy(a.getJSONObject(0)).put("rotation",new JSONArray(new int[]{0,0,0,1}));
        double expected=.351-.1-.25*(Math.sin(configAngle(c))+Math.cos(configAngle(c)));
        assertEquals(expected,TemporalContact067.clearance(c,identity),1e-12);
    }
    static double configAngle(JSONObject c){return Math.toRadians(c.getDouble("angleDeg"));}
    @Test public void contextIsLosslessAndFutureMotionCannotChangePlanning(){
        var rows=new JSONArray();
        for(Object raw:TemporalStudy067.cases(false)){
            var t=(JSONObject)raw;var results=new JSONObject();
            for(String m:TemporalPolicy067.METHODS)results.put(m,new JSONObject().put("outcome",TemporalPolicy067.outcome(t,new JSONObject().put("execute",false).put("method",m),null)));
            rows.put(new JSONObject().put("task",t).put("plan",TemporalPolicy067.initial(t)).put("screenWallMillis",0).put("results",results));
        }
        var original=new JSONObject().put("temporal067",new JSONObject().put("lastCompleted",new JSONObject().put("evaluation",TemporalStudy067.evaluation(rows)).put("shiftedEvaluation",TemporalPolicy067.evaluate(rows,true))));
        String hash=WorldFrames.hash(original);var compact=TemporalContext067.project(original);assertEquals(hash,WorldFrames.hash(original));assertTrue(compact.toString().length()<original.toString().length()*.65);
        var overview=new JSONObject().put("agent042Facts",WorldFrames.copy(compact));
        var overviewStudy=overview.getJSONObject("agent042Facts").getJSONObject("temporal067").getJSONObject("lastCompleted");overviewStudy.put("jobId","original-study");
        String metrics=overviewStudy.getJSONObject("evaluation").getJSONObject("methods").toString();
        String groupHash=WorldFrames.hash(overviewStudy.getJSONObject("evaluation").getJSONObject("subgroups"));
        TemporalContext067.fitPlanningBudget(overview,overview.toString().length());assertTrue(overviewStudy.getJSONObject("evaluation").has("subgroups"));
        TemporalContext067.fitPlanningBudget(overview,1);
        assertEquals(metrics,overviewStudy.getJSONObject("evaluation").getJSONObject("methods").toString());
        var ref=overviewStudy.getJSONObject("evaluation").getJSONObject("subgroupDetail067");assertEquals(groupHash,ref.getString("planningTablesSha256"));assertEquals("original-study",ref.getString("id"));assertEquals("evaluation067.subgroups",ref.getString("field"));
        overview.put("worldBinding",new JSONObject().put("requirements","x".repeat(13000)));TemporalContext067.fitPlanningBudget(overview,12500);assertThrows(IllegalStateException.class,()->ChatChain052.exact(overview,12500));
        assertEquals(hash,WorldFrames.hash(original));
        var study=compact.getJSONObject("temporal067").getJSONObject("lastCompleted");var columns=study.getJSONObject("tableColumns067");
        restore(study.getJSONObject("evaluation"),columns);restore(study.getJSONObject("shiftedEvaluation"),columns);study.remove("tableColumns067");study.remove("tableMeaning067");assertTrue(original.similar(compact));
        var c=Stopping063.definition(20,1.05,.24);var a=prefix(c);var d=new JSONObject().put("method","temporal").put("speedMps",.24);
        var before=TemporalContact067.plannerState(task(.13,.045),d,c,a);a.put(new JSONObject().put("futureEndpointM",999));
        assertTrue(before.similar(TemporalContact067.plannerState(task(.13,.045),d,c,a)));
    }
    static void restore(JSONObject e,JSONObject columns){
        for(String name:List.of("methods","pairedValueMinus")){var table=e.getJSONObject(name);var cols=columns.getJSONArray(table.getString("columnsRef"));var values=new JSONObject();for(Object raw:table.getJSONArray("rows")){var row=(JSONArray)raw;var metrics=new JSONObject();for(int i=0;i<cols.length();i++)metrics.put(cols.getString(i),row.get(i+1));values.put(row.getString(0),metrics);}e.put(name,values);}
        var groups=e.optJSONObject("subgroups");if(groups!=null)for(String key:groups.keySet())restore(groups.getJSONObject(key),columns);
    }
    @Test public void initialChoicesAreCompatibleAndScreenIsProspective(){
        int accepted=0,declined=0;var old=new HashSet<String>();for(Object x:RecoveryStudy066.cases(false))old.add(WorldFrames.hash((JSONObject)x));
        for(Object raw:TemporalStudy067.cases(false)){
            var t=(JSONObject)raw;assertFalse(old.contains(WorldFrames.hash(t)));var p=TemporalPolicy067.initial(t);var prev=RecoveryPolicy066.initial(t);
            for(String m:RecoveryPolicy066.METHODS)assertTrue(prev.getJSONObject("decisions").getJSONObject(m).similar(p.getJSONObject("decisions").getJSONObject(m)));
            var v=p.getJSONObject("decisions").getJSONObject("value");
            if(v.has("screen")){var s=v.getJSONObject("screen");assertEquals(9,s.getJSONArray("scenarios").length());assertEquals(3*s.getDouble("successFraction")-2-.145,s.getDouble("estimatedUtility"),1e-12);assertEquals(s.getBoolean("attempt"),v.getBoolean("execute"));if(v.getBoolean("execute"))accepted++;else declined++;}
        }
        assertTrue(accepted>0);assertTrue(declined>0);
        for(Object raw:TemporalStudy067.cases(true))for(String m:TemporalPolicy067.METHODS){var d=TemporalPolicy067.initial((JSONObject)raw).getJSONObject("decisions").getJSONObject(m);assertFalse(d.getBoolean("execute"));assertFalse(d.has("screen"));}
    }
    @Test public void costsIncludeThreeObservationsAndDeclinedScreens(){
        var t=task(.13,.012);var d=new JSONObject().put("execute",true).put("speedMps",.24).put("method","value").put("screen",new JSONObject());
        var r=new JSONObject().put("summary",new JSONObject().put("endpointM",.13)).put("timing067",new JSONObject().put("nativeWallMillis",12)).put("correctionPlan",new JSONObject().put("correct",true).put("deltaVelocityMps",.04));
        var o=TemporalPolicy067.outcome(t,d,r);assertEquals(.855,o.getDouble("utility"),1e-12);assertEquals(3,o.getInt("observations"));assertEquals(1,o.getInt("screens"));
        r.getJSONObject("summary").put("endpointM",0);r.getJSONObject("correctionPlan").put("correct",false).put("deltaVelocityMps",0);
        assertEquals(-2.125,TemporalPolicy067.outcome(t,d,r).getDouble("utility"),1e-12);
        d.put("execute",false);o=TemporalPolicy067.outcome(t,d,null);assertEquals(-.155,o.getDouble("utility"),1e-12);assertEquals(0,o.getInt("actions"));assertFalse(o.has("endpointM"));
    }
    @Test public void qualificationKeepsAllComparisonsAndFailureLimits(){
        var pairs=new JSONObject();for(String m:List.of("fixed","pooled","blind","temporal"))pairs.put(m,new JSONObject().put("meanDelta",.05));
        var e=new JSONObject().put("pairedValueMinus",pairs).put("methods",new JSONObject().put("value",new JSONObject().put("allTaskSuccessRate",.4).put("allTaskMissRate",.15).put("recoveryRate",.2)));
        assertTrue(TemporalStudy067.qualifies(e));for(String m:pairs.keySet()){var c=WorldFrames.copy(e);c.getJSONObject("pairedValueMinus").getJSONObject(m).put("meanDelta",0);assertFalse(TemporalStudy067.qualifies(c));}
        e.getJSONObject("methods").getJSONObject("value").put("allTaskMissRate",.16);assertFalse(TemporalStudy067.qualifies(e));
        assertEquals("agent",ChatPolicy044.route("Inspect temporal contact and pre-push recovery value",new JSONObject(),false));
    }
    @Test public void nativeStudyColdRestoreStopAndAlteredEvidenceRefusal()throws Exception{
        var root=Files.createTempDirectory("fraynix-temporal067-");System.out.println("EDITION067_NATIVE_ROOT="+root);String id,hash;JSONObject job;
        try(var b=new BrainService(root)){
            id=b.start(new JSONObject().put("kind","temporal-recovery").put("autoPromote",false)).getString("id");Recovery066Test.waitFor(b);job=b.read(id);
            Files.writeString(root.resolve("original-attempt.json"),job.toString());assertEquals(job.optString("reason"),"completed",job.getString("status"));TemporalStudy067.audit(job);
            Files.writeString(root.resolve("original-experiment.json"),job.toString());System.out.println("TEMPORAL067="+job.getJSONObject("evaluation067"));hash=WorldFrames.hash(job);
            assertEquals(508,job.getInt("progress"));assertEquals(1,b.activePolicy().getInt("version"));assertEquals(0,job.getJSONObject("evaluation067").getJSONObject("diagnostics").getInt("acceptedTemporalDisagreements"));
            JSONObject selected=null;for(Object raw:job.getJSONArray("tasks067")){var r=((JSONObject)raw).getJSONObject("results").getJSONObject("temporal").optJSONObject("record");if(r!=null&&r.has("correctionReceipt")){selected=r;break;}}assertNotNull("Real temporal correction required",selected);final JSONObject record=selected;
            tamperRecord(record,r->r.getJSONObject("correctionPlan").getJSONObject("state").getJSONObject("contactWindow").getJSONArray("samples").getJSONObject(0).put("geometricClearanceM",999));
            tamperRecord(record,r->r.getJSONObject("correctionReceipt").getJSONObject("after").getJSONArray("position").put(0,99));
            tamperRecord(record,r->r.getJSONObject("correctionReceipt").put("worldId",UUID.randomUUID().toString()));
            tamperRecord(record,r->r.put("correctionPlannedAt","2099-01-01T00:00:00Z"));
            tamper(job,j->j.getJSONArray("tasks067").getJSONObject(0).getJSONObject("results").getJSONObject("value").getJSONObject("outcome").put("utility",999));
            JSONObject screened=null;for(Object raw:job.getJSONArray("tasks067"))if(((JSONObject)raw).getJSONObject("plan").getJSONObject("decisions").getJSONObject("value").has("screen")){screened=(JSONObject)raw;break;}
            assertNotNull(screened);var corrupt=WorldFrames.copy(job);for(Object raw:corrupt.getJSONArray("tasks067")){var v=((JSONObject)raw).getJSONObject("plan").getJSONObject("decisions").getJSONObject("value");if(v.has("screen")){v.getJSONObject("screen").put("estimatedUtility",999);break;}}assertThrows(IllegalStateException.class,()->TemporalStudy067.audit(corrupt));
            var query=new JSONObject().put("jobId",id).put("index",0).put("shifted",false);var trace=b.temporalTrace067(query);Files.writeString(root.resolve("trace.json"),trace.toString());
            assertEquals(46,trace.getJSONObject("results").getJSONObject("temporal").getJSONArray("series").length());
            b.temporalState067().getJSONObject("lastCompleted").getJSONObject("evaluation").put("qualified","changed");
            assertEquals(job.getJSONObject("evaluation067").getBoolean("qualified"),b.temporalState067().getJSONObject("lastCompleted").getJSONObject("evaluation").getBoolean("qualified"));
        }
        try(var b=new BrainService(root)){
            assertEquals(hash,WorldFrames.hash(b.read(id)));assertTrue(new JSONObject(Files.readString(root.resolve("trace.json"))).similar(b.temporalTrace067(new JSONObject().put("jobId",id).put("index",0).put("shifted",false))));
            String stopped=b.start(new JSONObject().put("kind","temporal-recovery")).getString("id");b.cancel(stopped);Recovery066Test.waitFor(b);assertEquals("cancelled",b.read(stopped).getString("status"));assertEquals(id,b.temporalState067().getJSONObject("lastCompleted").getString("jobId"));
        }
    }
    static void tamper(JSONObject job,java.util.function.Consumer<JSONObject> edit){var c=WorldFrames.copy(job);edit.accept(c);assertThrows(IllegalStateException.class,()->TemporalStudy067.audit(c));}
    static void tamperRecord(JSONObject record,java.util.function.Consumer<JSONObject> edit){var c=WorldFrames.copy(record);c.remove("sha256");edit.accept(c);c.put("sha256",WorldFrames.hash(c));assertThrows(IllegalStateException.class,()->TemporalTrial067.verify(c));}
}
