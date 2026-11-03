package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

/** Actual native trials; report pixels and local inference are verified in the separate desktop run. */
public class Design053Test {
    static JSONObject input(WorldLedger worlds,double force)throws Exception{
        var w=worlds.create(Construction051Test.scene(),"LABELLED design053 native acceptance");
        var observed=worlds.observe(w.getString("id"));var a=new JSONObject();
        for(String k:List.of("worldId","expectedRevision","expectedHash"))a.put(k,observed.get(k));
        return a.put("construction051",Construction051.options(new JSONObject())).put("forceLimitN",force).put("torqueLimitNm",4);
    }
    static JSONObject terminal(Counterfactual045 store,String id)throws Exception{
        long until=System.currentTimeMillis()+180000;
        while(System.currentTimeMillis()<until){var s=store.read(id);if(!Set.of("queued","running").contains(s.getString("status")))return s;Thread.sleep(10);}
        throw new AssertionError("Actual design trial timeout");
    }
    @Test public void actualTrialsPreserveSourceAndCheckBridgeBeforeSelectedExecution()throws Exception{
        Path root=Files.createTempDirectory("design053-native-");var worlds=new WorldLedger(root);var a=input(worlds,40);var binding=worlds.observe(a.getString("worldId"));byte[] before=Files.readAllBytes(root.resolve("persistent-worlds/state.json"));
        try(var store=new Counterfactual045(root,worlds)){
            var initial=store.prepare(binding,worlds.preview(a));assertTrue(initial.has("design053"));
            for(Object raw:initial.getJSONArray("candidates")){var c=(JSONObject)raw;assertTrue(c.has("hypothesis053"));assertFalse(c.has("metrics"));}
            store.begin(initial.getString("id"));var done=terminal(store,initial.getString("id"));assertEquals(done.toString(),"ready",done.getString("status"));
            assertArrayEquals(before,Files.readAllBytes(root.resolve("persistent-worlds/state.json")));
            var low=done.getJSONArray("candidates").getJSONObject(0);assertEquals("failed",low.getString("status"));assertFalse(low.getJSONObject("metrics").getBoolean("passed"));assertFalse(low.getJSONObject("structure053").getBoolean("passed"));
            for(Object raw:done.getJSONArray("candidates")){
                var c=(JSONObject)raw;var run=store.run(done.getString("id"),c.getString("id"));
                assertTrue(WorldVerify.verify(run).similar(c.getJSONObject("metrics")));assertTrue(Construction051.verifyStructure(run).similar(c.getJSONObject("structure053")));
                assertTrue(run.getJSONObject("definition").similar(worlds.source(binding)));assertTrue(run.getJSONObject("plan").getJSONObject("intent").getDouble("forceLimitN")<=40);
            }
            var chosen=Design053.choose(done.getJSONArray("candidates"));assertEquals(chosen.getString("id"),done.getJSONObject("decision").getString("selectedCandidate"));
            var plan=store.executePlan(done.getString("id"),done.getString("decisionSha256"));var actual=Counterfactual045Test.actual(worlds,root,binding,plan);assertTrue(Construction051.verifyStructure(actual).getBoolean("passed"));
            var data=CounterfactualReport045.data(store,done.getString("id"),actual);assertTrue(data.getJSONObject("comparison").getBoolean("passed"));assertTrue(data.getJSONObject("design053").similar(Design053.policy()));
            var report=CounterfactualReport045.build(actual,data,store.attempts(done.getString("id")));assertTrue(report.getJSONObject("files").getString("index.html").contains("design053-policy"));assertEquals(9,CounterfactualReport045.checks(data).length());
            assertThrows(IllegalStateException.class,()->store.executePlan(done.getString("id"),"changed"));
            worlds.commit(binding,actual.getString("goalId"),actual,WorldLedgerTest.receipt(actual),WorldLedgerTest.meta());assertThrows(IllegalStateException.class,()->store.executePlan(done.getString("id"),done.getString("decisionSha256")));
            Files.writeString(root.resolve("result.json"),new JSONObject().put("study",done).put("comparison",data).put("actual",WorldReport.packed(actual)).toString());
        }System.out.println("DESIGN053_NATIVE "+root);
    }
    @Test public void allFailedTrialsCannotEarnDecisionOrLiveOwnership()throws Exception{
        Path root=Files.createTempDirectory("design053-infeasible-");var worlds=new WorldLedger(root);var a=input(worlds,6);var before=worlds.observe(a.getString("worldId"));
        try(var store=new Counterfactual045(root,worlds)){
            var s=store.prepare(before,worlds.preview(a));store.begin(s.getString("id"));var done=terminal(store,s.getString("id"));
            assertEquals(done.toString(),"failed",done.getString("status"));assertFalse(done.has("decision"));assertNull(Design053.choose(done.getJSONArray("candidates")));assertTrue(before.similar(worlds.observe(a.getString("worldId"))));
            assertEquals(3,store.export(s.getString("id")).getJSONArray("originalAttempts").length());assertThrows(IllegalStateException.class,()->store.executePlan(s.getString("id"),""));
            Files.writeString(root.resolve("result.json"),done.toString());
        }System.out.println("DESIGN053_INFEASIBLE "+root);
    }
    @Test public void minimalCeilingDeduplicatesWithoutExceedingAuthority()throws Exception{
        Path root=Files.createTempDirectory("design053-duplicates-");var worlds=new WorldLedger(root);var a=input(worlds,2);
        try(var store=new Counterfactual045(root,worlds)){var s=store.prepare(worlds.observe(a.getString("worldId")),worlds.preview(a));var c=s.getJSONArray("candidates");assertEquals("queued",c.getJSONObject(0).getString("status"));for(int i=1;i<3;i++){assertEquals("duplicate",c.getJSONObject(i).getString("status"));assertEquals(2,c.getJSONObject(i).getJSONObject("hypothesis053").getDouble("forceLimitN"),0);}}
    }
    @Test public void aCompletedMotionWithoutSupportedBridgeIsIneligible(){
        var fake=new JSONObject().put("id","explicit verifier boundary fixture").put("ordinal",0).put("status","completed").put("metrics",new JSONObject().put("passed",true).put("effortNs",0).put("nativeTicks",1)).put("structure053",new JSONObject().put("passed",false));
        assertNull(Design053.choose(new JSONArray().put(fake)));
    }
    @Test public void chatComparisonRequiresConstructionAndPreservesReviewIntent(){
        assertEquals("construction",ChatPolicy044.route("Compare assembly designs and build the bridge",new JSONObject(),false));
        assertEquals("workbench",ChatPolicy044.route("Design a website comparing bridge construction",new JSONObject(),false));
        assertTrue(ChatPolicy044.designReview053("Compare assembly designs only"));assertTrue(ChatPolicy044.designReview053("Compare and build the bridge; do not move anything"));assertTrue(ChatPolicy044.designReview053("Reuse assembly skill; review before executing"));assertFalse(ChatPolicy044.designReview053("Compare assembly designs and build the bridge"));
    }
}
