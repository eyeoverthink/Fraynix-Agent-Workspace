package fraymus.workspace;

import org.junit.Test;
import org.json.*;
import java.util.*;
import static org.junit.Assert.*;

/** Boundary fixtures are not a claim of real browser/model execution. See the separate actual app study. */
public class BrowserOutcome126Test {
    static JSONObject contract(){return new JSONObject().put("url","http://127.0.0.1/fixture").put("checks",new JSONArray().put(new JSONObject().put("selector","#result").put("text","Recorded alpha")));}
    static JSONObject receipt(JSONObject c,JSONObject task,JSONObject effect){return new JSONObject().put("schema","browser-outcome-126-v1").put("taskId",task.getString("id")).put("generation",task.getInt("generation")).put("effectId",effect.getString("id")).put("contractSha256",WorldFrames.hash(c)).put("url",c.getString("url")).put("observedAt",java.time.Instant.now().toString()).put("checks",new JSONArray().put(new JSONObject().put("selector","#result").put("count",1).put("text","Recorded alpha").put("visible",true)));}
    static JSONObject start(Agency043Test.Fixture f)throws Exception{return f.agency.start(new JSONObject().put("goal","Record alpha in the browser").put("model","unit-fixture").put(BrowserOutcome126.KEY,contract()));}
    @Test public void strictFrozenCriteriaRejectActionsCredentialsDuplicatesAndUnboundedInput(){
        assertThrows(IllegalArgumentException.class,()->BrowserOutcome126.contract(contract().put("url","file:///secret")));
        assertThrows(IllegalArgumentException.class,()->BrowserOutcome126.contract(contract().put("url","https://user:pass@example.test")));
        assertThrows(IllegalArgumentException.class,()->BrowserOutcome126.contract(contract().put("passed",true)));
        var action=contract();action.getJSONArray("checks").getJSONObject(0).put("action","click");assertThrows(IllegalArgumentException.class,()->BrowserOutcome126.contract(action));
        var duplicate=contract();duplicate.getJSONArray("checks").put(duplicate.getJSONArray("checks").getJSONObject(0));assertThrows(IllegalArgumentException.class,()->BrowserOutcome126.contract(duplicate));
    }
    @Test public void actualValuesAreComparedAndModelSuccessFlagsAreRefused(){
        var c=contract();var t=new JSONObject().put("id",UUID.randomUUID().toString()).put("generation",2);var e=new JSONObject().put("id",UUID.randomUUID().toString());var r=receipt(c,t,e);
        assertTrue(BrowserOutcome126.compare(c,r,t.getString("id"),2,e.getString("id")));
        for(String k:List.of("count","text","visible")){var altered=WorldFrames.copy(r);altered.getJSONArray("checks").getJSONObject(0).put(k,k.equals("count")?2:k.equals("text")?"wrong":false);assertFalse(BrowserOutcome126.compare(c,altered,t.getString("id"),2,e.getString("id")));}
        assertFalse(BrowserOutcome126.compare(c,WorldFrames.copy(r).put("url","https://different.test/"),t.getString("id"),2,e.getString("id")));
        assertThrows(IllegalArgumentException.class,()->BrowserOutcome126.compare(c,WorldFrames.copy(r).put("passed",true),t.getString("id"),2,e.getString("id")));
        assertThrows(IllegalStateException.class,()->BrowserOutcome126.compare(c,r,t.getString("id"),3,e.getString("id")));
        assertThrows(IllegalStateException.class,()->BrowserOutcome126.compare(c,WorldFrames.copy(r).put("checks",new JSONArray()),t.getString("id"),2,e.getString("id")));
    }
    @Test public void criteriaArePartOfOriginalObjectiveAndWrongToolCannotExecute()throws Exception{
        try(var f=new Agency043Test.Fixture(q->Agency043Test.reply("canvas.write",new JSONObject().put("title","wrong").put("files",new JSONObject().put("index.html","wrong"))))){
            var t=start(f);var changed=f.agency.read(t.getString("id"));changed.getJSONObject(BrowserOutcome126.KEY).put("url","https://changed.test/");assertThrows(IllegalStateException.class,()->ChatChain052.verifyObjective(changed));
            f.agency.tick();f.acknowledge(f.claim(),new JSONObject());f.agency.tick();assertEquals(0,f.store.artifacts().length());assertEquals(1,f.agency.read(t.getString("id")).getInt("failures"));
        }
    }
    @Test public void prematureFinishNeedsClaimedActualObservationAndRestartDropsOldAuthority()throws Exception{
        try(var f=new Agency043Test.Fixture(q->Agency043Test.reply("finish",new JSONObject().put("summary","success claimed by model")))){
            var t=start(f);f.agency.tick();f.acknowledge(f.claim(),new JSONObject());f.agency.tick();var claim=f.claim();assertEquals("browser-check126",claim.getString("kind"));assertEquals("waiting-host",f.agency.read(t.getString("id")).getString("status"));
            f.agency.detach(f.session);assertThrows(IllegalStateException.class,()->f.agency.requireExecution(t.getString("id"),claim.getString("nonce")));
            f.agency.control(t.getString("id"),"resume");f.agency.tick();var fresh=f.claim();assertEquals("browser.observe",fresh.getString("tool"));assertEquals("tool",fresh.getString("kind"));assertEquals(2,fresh.getInt("generation"));assertFalse(f.agency.read(t.getString("id")).has(BrowserOutcome126.PROOF));
        }
    }
    @Test public void successfulFrozenObservationIsRetainedVerifiedAndArchivable()throws Exception{
        try(var f=new Agency043Test.Fixture(q->Agency043Test.reply("finish",new JSONObject().put("summary","claimed")))){
            var c=contract().put("url","http://127.0.0.1/labelled-unit-control");
            var t=f.agency.start(new JSONObject().put("goal","Record alpha").put("model","unit-fixture").put(BrowserOutcome126.KEY,c));
            f.agency.tick();f.acknowledge(f.claim(),new JSONObject());f.agency.tick();var claim=f.claim();
            f.acknowledge(claim,receipt(c,t,claim));var done=f.agency.read(t.getString("id"));assertEquals("verified",done.getString("outcome"));assertTrue(f.agency.verify112(t.getString("id")).getBoolean("passed"));
            var altered=WorldFrames.copy(done);altered.getJSONObject(BrowserOutcome126.PROOF).getJSONObject("receipt").getJSONArray("checks").getJSONObject(0).put("text","wrong");assertThrows(IllegalStateException.class,()->BrowserOutcome126.evidence(altered));
            var state=f.agency.history112();f.agency.archive112(new JSONObject().put("planSha256",state.getString("planSha256")).put("ids",new JSONArray().put(t.getString("id"))));assertTrue(f.agency.verify112(t.getString("id")).getBoolean("passed"));
        }
    }
    @Test public void unsuccessfulActualCheckReturnsToPlanningWithoutCertifyingSuccess()throws Exception{
        try(var f=new Agency043Test.Fixture(q->Agency043Test.reply("finish",new JSONObject().put("summary","claimed")))){
            var c=contract().put("url","http://127.0.0.1/labelled-unit-control");var t=f.agency.start(new JSONObject().put("goal","Record alpha").put("model","unit-fixture").put(BrowserOutcome126.KEY,c));
            f.agency.tick();f.acknowledge(f.claim(),new JSONObject());f.agency.tick();var claim=f.claim();var bad=receipt(c,t,claim);bad.getJSONArray("checks").getJSONObject(0).put("text","not done");
            f.acknowledge(claim,bad);var g=f.agency.read(t.getString("id"));assertEquals("ready",g.getString("status"));assertEquals(1,g.getInt("failures"));assertFalse(g.has(BrowserOutcome126.PROOF));
        }
    }
}
