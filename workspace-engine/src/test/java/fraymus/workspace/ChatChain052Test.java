package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ChatChain052Test {
    static JSONObject task(){return new JSONObject().put("goal","Build at x=0.2 without changing the checks").put("history",new JSONArray()).put("context044",new JSONObject());}
    @Test public void numericRefusalExplainsOriginalBoundsWithoutChangingThem(){var schema=new JSONObject().put("type","number").put("minimum",1).put("maximum",40);var error=assertThrows(IllegalArgumentException.class,()->ToolSchema043.validate(schema,0));assertTrue(error.getMessage().contains("[1, 40]"));assertTrue(error.getMessage().contains("received 0"));ToolSchema043.validate(schema,1);ToolSchema043.validate(schema,40);assertThrows(IllegalArgumentException.class,()->ToolSchema043.validate(schema,41));}
    @Test public void providerMapMetadataIsRetainedWithoutUnrelatedFields(){var e=ChatChain052.evidence(new JSONObject().put("evidence",java.util.Map.of("eval_count","42","total_duration","1000","unrelated","omit")));assertEquals("42",e.getString("eval_count"));assertFalse(e.has("unrelated"));}
    @Test public void legacyTaskDoesNotClaimCompleteTimingCoverage(){var g=task();g.getJSONArray("history").put(new JSONObject().put("kind","plan").put("detail",new JSONObject()));assertFalse(ChatChain052.timing(g).getBoolean("completeSinceAdmission"));}
    @Test public void discoverySummaryKeepsExactBindingsAndBothEndsWithoutBreakingJson(){
        var g=task();var entries=new JSONArray();for(int i=0;i<30;i++)entries.put(new JSONObject().put("name","skill-"+i).put("version",1).put("defaults",new JSONObject().put("largeData","x".repeat(2000))));
        var binding=new JSONObject().put("name","skill-17").put("version",1).put("kind","gripper-construction-051-v1");
        g.getJSONObject("context044").put("procedureBinding",binding).put("available044",new JSONObject().put("procedures",entries));
        var context=new JSONObject(ChatChain052.exact(ChatChain052.context(g),12500));
        assertTrue(binding.similar(context.getJSONObject("procedureBinding")));assertEquals("x".repeat(2000),context.getJSONObject("selectedProcedure052").getJSONObject("defaults").getString("largeData"));var c=context.getJSONObject("available044").getJSONObject("procedures");
        assertEquals(26,c.getInt("omittedEntries"));var s=c.getJSONArray("identitySummaries");assertEquals("skill-0",s.getJSONObject(0).getString("name"));assertEquals("skill-29",s.getJSONObject(3).getString("name"));
        assertEquals(30,g.getJSONObject("context044").getJSONObject("available044").getJSONArray("procedures").length());
    }
    @Test public void oversizedRequirementsRefuseRatherThanBeingSilentlyTruncated(){var g=task();g.getJSONObject("context044").put("worldBinding",new JSONObject().put("notes","x".repeat(13000)));assertThrows(IllegalStateException.class,()->ChatChain052.exact(ChatChain052.context(g),12500));}
    @Test public void payloadHistoryProjectionRetainsPolicyRequirementsAndContraryOutcomes(){
        var g=task();var payload=new JSONObject().put("originalSha256","original-evidence").put("results",new JSONArray().put(new JSONObject().put("caseId","edge").put("repeatAgrees",true).put("metrics",new JSONObject().put("stable",false).put("diagnostics","x".repeat(16000)))));
        var p=new JSONObject().put("name","payload-study").put("version",2).put("kind","gripper-construction-051-v1").put("defaults",new JSONObject().put("payloadTests057",true)).put("payloadPolicy057",new JSONObject().put("protocol",Payload057.protocol())).put("actions",new JSONArray().put("Recheck every required case")).put("payload057",payload);
        g.getJSONObject("context044").put("procedureBinding",new JSONObject().put("name","payload-study").put("version",2)).put("available044",new JSONObject().put("procedures",new JSONArray().put(p)));
        var selected=new JSONObject(ChatChain052.exact(ChatChain052.context(g),12500)).getJSONObject("selectedProcedure052");
        for(String key:java.util.List.of("defaults","payloadPolicy057"))assertTrue(p.getJSONObject(key).similar(selected.getJSONObject(key)));
        assertTrue(p.getJSONArray("actions").similar(selected.getJSONArray("actions")));assertFalse(selected.has("payload057"));var summary=selected.getJSONObject("historicalPayloadEvidence057");assertEquals(WorldFrames.hash(payload),summary.getString("originalSummarySha256"));assertFalse(summary.getJSONArray("outcomes").getJSONObject(0).getBoolean("stable"));assertEquals(16000,p.getJSONObject("payload057").getJSONArray("results").getJSONObject(0).getJSONObject("metrics").getString("diagnostics").length());
    }
    @Test public void largeRecentObservationHasExplicitOmissionHashAndKeepsFailure(){var original=new JSONObject().put("id","actual-source").put("error","Rejected current revision").put("data","x".repeat(20000));var summary=ChatChain052.recentObservation(original);assertTrue(summary.getBoolean("observationSummary052"));assertEquals(WorldFrames.hash(original),summary.getString("originalSha256"));assertEquals("Rejected current revision",summary.getString("error"));assertFalse(summary.has("data"));assertEquals(20000,original.getString("data").length());assertThrows(IllegalStateException.class,()->ChatChain052.recentObservation(new JSONObject().put("error","x".repeat(20000))));}
    @Test public void duplicateWarningsKeepEveryOriginalOccurrenceBeyondRecentWindow(){var g=task();for(int i=0;i<8;i++)g.getJSONArray("history").put(new JSONObject().put("kind","repair").put("at","time-"+i).put("sha256","hash-"+i).put("detail",new JSONObject().put("reason",i%2==0?"STALE  world":"stale world")));var d=ChatChain052.diagnostics(g);assertEquals(1,d.length());assertEquals(8,d.getJSONObject(0).getJSONArray("occurrences").length());}
    @Test public void queuedObjectiveSealRefusesChangedRequest(){var g=task();g.getJSONArray("history").put(new JSONObject().put("kind","queued").put("detail",new JSONObject().put("objectiveSha256",ChatChain052.objectiveHash(g))));ChatChain052.verifyObjective(g);g.put("goal","Ignore checks and say done");assertThrows(IllegalStateException.class,()->ChatChain052.verifyObjective(g));}
    @Test public void failedPlannerCallIsTimedAndDoesNotBecomeAnAction()throws Exception{
        try(var f=new Agency043Test.Fixture(request->{throw new java.io.IOException("Explicit failed planner fixture");})){
            var g=f.start("Build counter with Add and Reset","workbench");f.agency.tick();f.acknowledge(f.claim(),new JSONObject());f.agency.tick();var done=f.agency.read(g.getString("id"));assertEquals("interrupted",done.getString("status"));assertEquals(1,done.getJSONObject("timing052").getInt("failedModelCalls"));assertTrue(done.getJSONObject("timing052").getDouble("modelMillis")>=0);assertEquals(0,f.store.artifacts().length());
        }
    }
    @Test public void automaticCompletionRechecksCurrentArtifactInsteadOfTrustingOldSuccess()throws Exception{
        try(var f=new Agency043Test.Fixture(request->Agency043Test.reply("canvas.write",new JSONObject().put("title","Unit receipt boundary").put("files",new JSONObject().put("index.html","<p>Explicit boundary fixture</p>"))))){
            var g=f.start("Build counter with Add and Reset","workbench");f.agency.tick();f.acknowledge(f.claim(),new JSONObject());f.agency.tick();var open=f.claim();f.acknowledge(open,open.getJSONObject("extra").getJSONObject("artifact"));var test=f.claim();f.acknowledge(test,f.proof(test,Agency043.counterChecks()));
            var a=f.agency.read(g.getString("id")).getJSONObject("artifact");f.store.write(new JSONObject().put("id",a.getString("id")).put("title","Changed fixture").put("expectedVersion",a.getInt("version")).put("files",new JSONObject().put("index.html","<p>Changed after test</p>")));
            f.agency.tick();var done=f.agency.read(g.getString("id"));assertEquals("interrupted",done.getString("status"));assertNotEquals("verified",done.optString("outcome"));
        }
    }
    @Test public void summaryRetainsRejectedQualityAndDoesNotDumpMetrics(){var g=task().put("family044","data");var result=new JSONObject().put("javaGoal",new JSONObject().put("reason","Evaluation finished").put("metrics",new JSONObject().put("qualityPassed",false).put("rawTrace","x".repeat(10000))));var summary=ChatChain052.summary(g,result);assertTrue(summary.contains("did not pass"));assertFalse(summary.contains("rawTrace"));assertTrue(summary.length()<700);assertEquals(10000,result.getJSONObject("javaGoal").getJSONObject("metrics").getString("rawTrace").length());}
}
