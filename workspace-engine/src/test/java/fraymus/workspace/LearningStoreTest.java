package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

public class LearningStoreTest {
    private JSONObject begin(LearningStore store) throws Exception {return store.begin(new JSONObject().put("id",UUID.randomUUID().toString()).put("taskKind","browser-task").put("goal","private task text"));}
    private String failure(LearningStore store) throws Exception {
        var episode=begin(store);String id=episode.getString("id");store.step(new JSONObject().put("id",id).put("step",new JSONObject().put("tool","browser.click").put("ok",false).put("failure","stale-observation")));
        store.end(new JSONObject().put("id",id).put("status","failed"));return id;
    }
    private JSONObject candidate(LearningStore store,String id,boolean refresh) throws Exception {
        var state=store.state();return store.propose(new JSONObject().put("parentVersion",state.getJSONObject("activePolicy").getInt("version")).put("parentGeneration",state.getInt("generation"))
            .put("policy",new JSONObject().put("observationLimit",2).put("refreshStale",refresh)).put("episodes",new JSONArray().put(id)));
    }
    private JSONObject results(JSONObject candidate,boolean improvement) {
        var rows=new JSONArray();for(String id:LearningStore.CASES)rows.put(new JSONObject().put("id",id)
            .put("baseline",new JSONObject().put("passed",!improvement||!id.contains("stale")).put("failures",improvement&&id.contains("stale")?1:0))
            .put("candidate",new JSONObject().put("passed",true).put("failures",0)));
        return new JSONObject().put("candidateId",candidate.getString("id")).put("suite",LearningStore.SUITE).put("suiteDigest",LearningStore.digest(LearningStore.SUITE+":"+String.join(",",LearningStore.CASES))).put("results",rows);
    }
    @Test public void interruptedEpisodeRestoresWithoutRawTaskOrPageText() throws Exception {
        Path root=Files.createTempDirectory("learning-restart");var store=new LearningStore(root);String id=begin(store).getString("id");
        var restored=new LearningStore(root);assertEquals("interrupted",restored.read("episode",id).getString("status"));assertFalse(restored.state().toString().contains("private task text"));
        assertEquals(1,restored.state().getLong("episodeCount"));
    }
    @Test public void episodeCannotClaimVerificationWithoutAnActualReceipt() throws Exception {
        var store=new LearningStore(Files.createTempDirectory("learning-receipt"));String id=begin(store).getString("id");
        try{store.end(new JSONObject().put("id",id).put("status","verified"));fail("Claim accepted");}catch(IllegalStateException expected){}
        try{store.step(new JSONObject().put("id",id).put("step",new JSONObject().put("tool","canvas.write").put("ok",true).put("testPassed",true)));fail("Invented test accepted");}catch(IllegalStateException expected){}
        try{store.step(new JSONObject().put("id",id).put("step",new JSONObject().put("tool","browser.observe").put("ok",true).put("rawPage","secret")));fail("Raw content accepted");}catch(IllegalArgumentException expected){}
    }
    @Test public void usefulCandidatePromotesPersistsAndRollsBack() throws Exception {
        Path root=Files.createTempDirectory("learning-promotion");var store=new LearningStore(root);String id=failure(store);var candidate=candidate(store,id,true);
        assertTrue(store.evaluate(results(candidate,true)).getBoolean("accepted"));assertTrue(store.state().getJSONObject("activePolicy").getBoolean("refreshStale"));
        var restored=new LearningStore(root);assertEquals(2,restored.state().getJSONObject("activePolicy").getInt("version"));restored.rollback();
        assertEquals(1,new LearningStore(root).state().getJSONObject("activePolicy").getInt("version"));assertEquals(2,restored.state().getJSONArray("promotions").length());
    }
    @Test public void noGainAndRegressionCannotBecomeLearning() throws Exception {
        var store=new LearningStore(Files.createTempDirectory("learning-rejected"));String id=failure(store);var noGain=candidate(store,id,false);
        assertFalse(store.evaluate(results(noGain,false)).getBoolean("accepted"));assertEquals(1,store.state().getJSONObject("activePolicy").getInt("version"));
        var harmful=candidate(store,id,true);var input=results(harmful,true);input.getJSONArray("results").getJSONObject(3).getJSONObject("candidate").put("passed",false).put("failures",1);
        assertFalse(store.evaluate(input).getBoolean("accepted"));assertTrue(store.state().getJSONArray("evaluations").getJSONObject(0).getBoolean("regression"));
    }
    @Test public void policyPrivilegesMissingFixturesAndStaleParentsAreRejected() throws Exception {
        var store=new LearningStore(Files.createTempDirectory("learning-guards"));String id=failure(store);var c=candidate(store,id,true);var incomplete=results(c,true);incomplete.getJSONArray("results").remove(0);
        try{store.evaluate(incomplete);fail("Incomplete replay accepted");}catch(IllegalArgumentException expected){}
        try{LearningStore.policy(new JSONObject().put("observationLimit",1).put("refreshStale",true).put("shell",true));fail("Privilege accepted");}catch(IllegalArgumentException expected){}
        store.evaluate(results(c,true));var oldParent=candidate(store,id,false);store.rollback();
        try{store.evaluate(results(oldParent,false));fail("Stale parent accepted");}catch(ConcurrentModificationException expected){}
    }
    @Test public void failedCheckpointDoesNotMutateTheLiveLearningWorld() throws Exception {
        Path root=Files.createTempDirectory("learning-atomic");var store=new LearningStore(root);Path checkpoint=root.resolve("learning/state.fworld");Files.delete(checkpoint);Files.createDirectory(checkpoint);Files.writeString(checkpoint.resolve("owned-blocker"),"nonempty");
        try{begin(store);fail("Checkpoint should fail");}catch(java.io.IOException expected){}
        assertEquals(0,store.state().getLong("episodeCount"));
    }
}
