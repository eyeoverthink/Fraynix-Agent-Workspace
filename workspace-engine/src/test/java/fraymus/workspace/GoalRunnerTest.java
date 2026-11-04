package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

public class GoalRunnerTest {
 @Test public void importedTextCannotInstallHandlersOrEscapePhysicalDomain() throws Exception {
  var root=Files.createTempDirectory("goals-domain-");try(var artifacts=new WorkspaceStore(root);var scenes=new SceneStore(root);var brain=new BrainService(root);var goals=new GoalRunner(root,scenes,brain,artifacts)){
   assertThrows(IllegalArgumentException.class,()->goals.start(new JSONObject().put("handler","shell")));
   assertThrows(IllegalArgumentException.class,()->goals.start(new JSONObject().put("parameters",new JSONObject().put("friction",.8))));
   assertThrows(IllegalArgumentException.class,()->goals.start(new JSONObject().put("parameters",new JSONObject().put("lengthM",6))));
   assertThrows(IllegalArgumentException.class,()->goals.start(new JSONObject().put("targetM",4)));
   assertThrows(IllegalArgumentException.class,()->goals.start(new JSONObject().put("disturbanceMps",1)));
   assertThrows(IllegalArgumentException.class,()->goals.start(new JSONObject().put("saveSkillName","../../code")));
   assertEquals(0,goals.list().length());assertEquals(0,goals.procedures().length());
  }
 }
 @Test public void aForeignNativeRunIsPreservedAndSourceVersionsAreRequired() throws Exception {
  var root=Files.createTempDirectory("goals-foreign-");try(var artifacts=new WorkspaceStore(root);var scenes=new SceneStore(root);var brain=new BrainService(root);var goals=new GoalRunner(root,scenes,brain,artifacts)){
   var s=scenes.save(new JSONObject().put("title","User source"));
   assertThrows(ConcurrentModificationException.class,()->goals.start(new JSONObject().put("sceneId",s.getString("id")).put("expectedVersion",2)));
   scenes.control(new JSONObject().put("action","start").put("id",s.getString("id")).put("expectedVersion",1));scenes.control(new JSONObject().put("action","pause"));var before=scenes.snapshot();
   assertThrows(IllegalStateException.class,()->goals.start(new JSONObject()));assertEquals(before.getString("id"),scenes.snapshot().getString("id"));assertEquals(before.getJSONObject("current").getInt("tick"),scenes.snapshot().getJSONObject("current").getInt("tick"));assertEquals(1,scenes.read(s.getString("id")).getInt("version"));
  }
 }
 @Test public void intentSurvivesRestartRequiresExplicitResumeAndHasFiniteRecovery() throws Exception {
  var root=Files.createTempDirectory("goals-restart-");String id;
  try(var artifacts=new WorkspaceStore(root);var scenes=new SceneStore(root);var brain=new BrainService(root)){
   var goals=new GoalRunner(root,scenes,brain,artifacts);synchronized(goals){id=goals.start(new JSONObject().put("parameters",new JSONObject().put("friction",.17))).getString("id");goals.close();}
   for(int i=0;i<4;i++){
    goals=new GoalRunner(root,scenes,brain,artifacts);synchronized(goals){
     assertEquals("interrupted",goals.read(id).getString("status"));assertFalse(goals.busy());assertEquals("interrupted",goals.list().getJSONObject(0).getString("status"));assertEquals(.17,goals.read(id).getJSONObject("parameters").getDouble("friction"),0);
     if(i<3){goals.resume(id);assertEquals(i+1,goals.read(id).getInt("resumes"));final var owned=goals;assertThrows(IllegalStateException.class,()->owned.start(new JSONObject()));}
     else {final var owned=goals;assertThrows(IllegalStateException.class,()->owned.resume(id));}
     goals.close();
    }
   }
  }
 }
 @Test public void cancelledIntentAndInventedHostReceiptsCannotBecomeSkills() throws Exception {
  var root=Files.createTempDirectory("goals-receipts-");try(var artifacts=new WorkspaceStore(root);var scenes=new SceneStore(root);var brain=new BrainService(root);var goals=new GoalRunner(root,scenes,brain,artifacts)){
   synchronized(goals){var g=goals.start(new JSONObject());String id=g.getString("id");assertThrows(IllegalStateException.class,()->goals.start(new JSONObject()));
    assertThrows(IllegalStateException.class,()->goals.acknowledge(new JSONObject().put("goalId",id).put("id",UUID.randomUUID()).put("nonce",UUID.randomUUID()).put("result",new JSONObject().put("passed",true))));
    goals.cancel(id);assertFalse(goals.busy());assertEquals("cancelled",goals.list().getJSONObject(0).getString("status"));var detached=goals.list().getJSONObject(0);detached.put("status","running");detached.getJSONArray("history").clear();assertFalse(goals.busy());assertTrue(goals.list().getJSONObject(0).getJSONArray("history").length()>0);assertEquals("cancelled",goals.read(id).getString("status"));assertThrows(IllegalStateException.class,()->goals.resume(id));assertThrows(IllegalStateException.class,()->goals.saveProcedure(id,"failed-procedure"));assertEquals(0,goals.procedures().length());}
  }
 }
 @Test public void frozenSceneAndReportEffectsHaveStableIdentitiesAndRejectChangedContent() throws Exception {
  var root=Files.createTempDirectory("goals-effects-");try(var artifacts=new WorkspaceStore(root);var scenes=new SceneStore(root)){
   String id=UUID.randomUUID().toString();var p=RampDefinition.from(new JSONObject()).json();var a=scenes.goalScene(id,p,new JSONObject());assertEquals(a.getString("id"),scenes.goalScene(id,p,new JSONObject()).getString("id"));assertThrows(IllegalStateException.class,()->scenes.goalScene(id,new JSONObject(p.toString()).put("friction",.2),new JSONObject()));
   var args=new JSONObject().put("title","Report effect unit fixture").put("files",new JSONObject().put("index.html","<h1>fixture</h1>")).put("sources",new JSONArray(List.of("goal:"+id)));var report=artifacts.writeOnce(id,args);assertEquals(report.getString("id"),artifacts.writeOnce(id,args).getString("id"));assertEquals(1,artifacts.artifacts().length());assertFalse(artifacts.read(report.getString("id")).has("test"));args.getJSONObject("files").put("index.html","changed");assertThrows(IllegalStateException.class,()->artifacts.writeOnce(id,args));
  }
 }
}
