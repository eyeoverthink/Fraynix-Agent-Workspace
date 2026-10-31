package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

public class CreationTest {
    static final String MODEL="local-test-model",DIGEST="a".repeat(64);
    static JSONObject source(WorkspaceStore store,String csv) throws Exception {return store.write(new JSONObject().put("title","Explicit test CSV").put("files",new JSONObject().put("index.html","<html><body>Test source only</body></html>").put("rows.csv",csv)));}
    static JSONObject input(JSONObject source){var in=new JSONObject().put("request","Create a working source explorer").put("title","Created explorer").put("modelName",MODEL).put("saveSkillName","source-explorer").put("checks",new JSONArray().put(new JSONObject().put("selector","#rows").put("sourceRows",true)));if(source!=null)in.put("sourceArtifactId",source.getString("id")).put("sourceVersion",source.getInt("version")).put("sourceFile","rows.csv");return in;}
    static JSONObject claim(GoalRunner goals,String kind) throws Exception {for(int i=0;i<500;i++){var e=goals.claim();if(e.has("id")){assertEquals(kind,e.getString("kind"));return e;}Thread.sleep(10);}throw new AssertionError("No actual "+kind+" effect");}
    static JSONObject draft(){return new JSONObject().put("files",new JSONObject().put("index.html","<html><body><output id='rows'>3</output></body></html>")).put("modelName",MODEL).put("modelDigest",DIGEST);}
    static JSONObject ack(GoalRunner goals,JSONObject e,JSONObject result) throws Exception {return goals.acknowledge(new JSONObject().put("goalId",e.getString("goalId")).put("id",e.getString("id")).put("nonce",e.getString("nonce")).put("result",result));}
    static void receipt(WorkspaceStore store,JSONObject e,boolean pass) throws Exception {
        var checks=new JSONArray();var steps=e.getJSONArray("steps");for(int i=0;i<steps.length();i++){var s=steps.getJSONObject(i);checks.put(new JSONObject().put("index",i).put("selector",s.getString("selector")).put("passed",pass).put("actual",new JSONObject().put("count",1).put("text",pass?s.optString("expectText"):"wrong").put("value",s.optString("expectValue"))));}
        store.recordTest(new JSONObject().put("id",e.getJSONObject("report").getString("id")).put("version",e.getJSONObject("report").getInt("version")).put("steps",steps).put("checks",checks));
    }
    static JSONObject result(JSONObject e){return new JSONObject().put("id",e.getJSONObject("report").getString("id")).put("version",e.getJSONObject("report").getInt("version"));}
    static JSONObject settled(GoalRunner goals,String id) throws Exception {for(int i=0;i<500;i++){var g=goals.read(id);if(!Set.of("running","waiting-host").contains(g.getString("status")))return g;Thread.sleep(10);}throw new AssertionError("Creation did not settle");}
    @Test public void behaviorMustBeObservableAndSourceCountsAreDerived() throws Exception {
        try(var store=new WorkspaceStore(Files.createTempDirectory("creation-contract"))){var a=source(store,"profile,value\na,1\nb,2\na,3\n");var in=input(a);var d=Creation.definition(store,in);assertEquals("3",d.getJSONArray("testSteps").getJSONObject(0).getString("expectText"));assertEquals(3,d.getJSONObject("sourceData").getJSONArray("rows").length());
            var templates=new JSONArray().put(new JSONObject().put("selector","#rows").put("sourceCount",new JSONObject().put("column","profile").put("equals","a")));assertEquals("2",Creation.resolveChecks(d,templates).getJSONObject(0).getString("expectText"));
            assertThrows(IllegalArgumentException.class,()->Creation.definition(store,new JSONObject(in.toString()).put("checks",new JSONArray())));assertThrows(IllegalArgumentException.class,()->Creation.resolveChecks(d,new JSONArray().put(new JSONObject().put("selector","body").put("action","shell"))));assertThrows(IllegalArgumentException.class,()->Creation.files(d,new JSONObject().put("index.html","<html><body>Working file</body></html>").put("inputs/source.json","{}")));
            assertEquals(a.getJSONObject("contents").getString("rows.csv"),Creation.files(d,draft().getJSONObject("files")).getString("inputs/source.csv"));
        }
    }
    @Test public void changedSourceIsPreservedAndRejected() throws Exception {
        try(var store=new WorkspaceStore(Files.createTempDirectory("creation-source"))){var a=source(store,"x,y\n1,2\n3,4\n");var d=Creation.definition(store,input(a));store.write(new JSONObject().put("id",a.getString("id")).put("expectedVersion",1).put("title","User edit").put("files",new JSONObject().put("rows.csv","x,y\n7,8\n9,10\n")));assertThrows(IllegalStateException.class,()->Creation.sourceIntact(store,d));assertEquals("User edit",store.read(a.getString("id")).getString("title"));}
    }
    @Test public void failedReceiptCausesOwnedRevisionThenVerifiedFreshSourceSkill() throws Exception {
        var root=Files.createTempDirectory("creation-repair");try(var store=new WorkspaceStore(root);var scenes=new SceneStore(root);var brain=new BrainService(root);var goals=new GoalRunner(root,scenes,brain,store)){
            var a=source(store,"profile,value\na,1\nb,2\na,3\n");String id=goals.startCreation(input(a)).getString("id");assertFalse(goals.ownsPhysics());assertTrue(goals.ownsCanvas());ack(goals,claim(goals,"creation.generate"),draft());var e=claim(goals,"creation.test");String artifact=e.getJSONObject("report").getString("id");assertThrows(IllegalStateException.class,()->goals.requireArtifactWrite(artifact));receipt(store,e,false);ack(goals,e,result(e));var next=claim(goals,"creation.generate");assertFalse(next.getJSONObject("generation").getJSONArray("failedChecks").isEmpty());ack(goals,next,draft());var verify=claim(goals,"creation.test");assertEquals(artifact,verify.getJSONObject("report").getString("id"));assertEquals(2,verify.getJSONObject("report").getInt("version"));receipt(store,verify,true);ack(goals,verify,result(verify));var completed=settled(goals,id);assertEquals("completed",completed.getString("status"));assertEquals(2,completed.getInt("draftNumber"));assertEquals(2,completed.getJSONArray("draftHistory").length());assertEquals(1,goals.procedures().length());
            var fresh=source(store,"profile,value\na,4\na,5\nb,6\nb,7\n");var reused=goals.runProcedure(new JSONObject().put("name","source-explorer").put("version",1).put("sourceArtifactId",fresh.getString("id")).put("sourceVersion",1).put("sourceFile","rows.csv"));assertNotEquals(id,reused.getString("id"));assertEquals("4",goals.read(reused.getString("id")).getJSONArray("testSteps").getJSONObject(0).getString("expectText"));goals.cancel(reused.getString("id"));assertEquals(0,brain.state().getJSONArray("jobs").length());
        }
    }
    @Test public void restartReconcilesPendingWriteAndReusesPassingReceipt() throws Exception {
        var root=Files.createTempDirectory("creation-restart");try(var store=new WorkspaceStore(root);var scenes=new SceneStore(root);var brain=new BrainService(root)){
            var source=source(store,"x,y\n1,2\n3,4\n");var goals=new GoalRunner(root,scenes,brain,store);String id=goals.startCreation(input(source)).getString("id");var e=claim(goals,"creation.generate");synchronized(goals){ack(goals,e,draft());goals.close();}
            goals=new GoalRunner(root,scenes,brain,store);assertEquals("interrupted",goals.read(id).getString("status"));goals.resume(id);var testing=claim(goals,"creation.test");String artifact=testing.getJSONObject("report").getString("id");receipt(store,testing,true);goals.close();
            goals=new GoalRunner(root,scenes,brain,store);goals.resume(id);assertEquals("completed",settled(goals,id).getString("status"));assertEquals(1,store.read(artifact).getInt("version"));assertEquals(1,goals.read(id).getInt("draftNumber"));assertEquals(1,goals.read(id).getJSONArray("draftHistory").length());goals.close();
        }
    }
    @Test public void foreignNonceCancellationAndInventedPassingValuesCannotComplete() throws Exception {
        var root=Files.createTempDirectory("creation-receipts");try(var store=new WorkspaceStore(root);var scenes=new SceneStore(root);var brain=new BrainService(root);var goals=new GoalRunner(root,scenes,brain,store)){
            String id=goals.startCreation(input(source(store,"x,y\n1,2\n3,4\n"))).getString("id");var e=claim(goals,"creation.generate");var forged=new JSONObject(e.toString()).put("nonce",UUID.randomUUID().toString());assertThrows(IllegalStateException.class,()->ack(goals,forged,draft()));ack(goals,e,draft());var testing=claim(goals,"creation.test");var ref=result(testing);ref.put("passed",true);var stopped=ack(goals,testing,ref);assertEquals("interrupted",stopped.getString("status"));assertEquals(0,goals.procedures().length());goals.cancel(id);assertEquals("cancelled",goals.read(id).getString("status"));assertThrows(IllegalStateException.class,()->ack(goals,testing,result(testing)));
        }
    }
    @Test public void threeDraftBudgetIsNotResetByExplicitResume() throws Exception {
        var root=Files.createTempDirectory("creation-budget");try(var store=new WorkspaceStore(root);var scenes=new SceneStore(root);var brain=new BrainService(root);var goals=new GoalRunner(root,scenes,brain,store)){
            String id=goals.startCreation(input(source(store,"x,y\n1,2\n3,4\n"))).getString("id");for(int i=0;i<3;i++){ack(goals,claim(goals,"creation.generate"),draft());var e=claim(goals,"creation.test");receipt(store,e,false);ack(goals,e,result(e));}assertEquals("interrupted",goals.read(id).getString("status"));assertThrows(IllegalStateException.class,()->goals.resume(id));assertEquals(3,goals.read(id).getInt("draftNumber"));assertEquals(0,goals.procedures().length());
        }
    }
    @Test public void modelContractIsFrozenBeforeCodeAndDigestCannotChange() throws Exception {
        var root=Files.createTempDirectory("creation-plan");try(var store=new WorkspaceStore(root);var scenes=new SceneStore(root);var brain=new BrainService(root);var goals=new GoalRunner(root,scenes,brain,store)){
            var in=input(source(store,"x,y\n1,2\n3,4\n"));var checks=(JSONArray)in.remove("checks");String id=goals.startCreation(in).getString("id");assertEquals(0,goals.read(id).getInt("draftNumber"));var e=claim(goals,"creation.plan");ack(goals,e,new JSONObject().put("checks",checks).put("modelName",MODEL).put("modelDigest",DIGEST));assertEquals("2",goals.read(id).getJSONArray("testSteps").getJSONObject(0).getString("expectText"));var stopped=ack(goals,claim(goals,"creation.generate"),draft().put("modelDigest","b".repeat(64)));assertEquals("interrupted",stopped.getString("status"));assertEquals(0,goals.procedures().length());
        }
    }
    @Test public void externalRevisionAndFalsePassedFlagCannotBecomeSuccess() throws Exception {
        var root=Files.createTempDirectory("creation-tamper");try(var store=new WorkspaceStore(root);var scenes=new SceneStore(root);var brain=new BrainService(root);var goals=new GoalRunner(root,scenes,brain,store)){
            var a=source(store,"x,y\n1,2\n3,4\n");String id=goals.startCreation(input(a)).getString("id");ack(goals,claim(goals,"creation.generate"),draft());var e=claim(goals,"creation.test");var checks=new JSONArray().put(new JSONObject().put("index",0).put("selector","#rows").put("passed",true).put("actual",new JSONObject().put("text","wrong").put("count",1)));
            store.recordTest(result(e).put("steps",e.getJSONArray("steps")).put("checks",checks));assertEquals("interrupted",ack(goals,e,result(e)).getString("status"));assertEquals(0,goals.procedures().length());goals.cancel(id);
            id=goals.startCreation(input(a)).getString("id");ack(goals,claim(goals,"creation.generate"),draft());e=claim(goals,"creation.test");String artifact=e.getJSONObject("report").getString("id");store.write(new JSONObject().put("id",artifact).put("expectedVersion",1).put("title","External change").put("files",new JSONObject().put("index.html","<html><body>User revision</body></html>")));assertEquals("interrupted",ack(goals,e,result(e)).getString("status"));assertEquals("External change",store.read(artifact).getString("title"));assertEquals(0,goals.procedures().length());
        }
    }
}
