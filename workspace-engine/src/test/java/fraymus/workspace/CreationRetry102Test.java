package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Actual persisted worker/goal boundaries; the planner here is an explicit unit fixture. */
public class CreationRetry102Test {
 private record Paused(Path root,String goal,String job){}
 private Paused pauseAfterRejection() throws Exception {
  Path root=Files.createTempDirectory("retained-retry102-");
  var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var calls=new AtomicInteger();
  try(var store=new WorkspaceStore(root);var scenes=new SceneStore(root);var brain=new BrainService(root);
      var goals=new GoalRunner(root,scenes,brain,store);
      var worker=new CreationPlanner044(root,goals,request->{
       if(calls.getAndIncrement()==0)throw new IllegalArgumentException("The model returned no native tool call. Fixture refusal.");
       entered.countDown();assertTrue(release.await(5,TimeUnit.SECONDS));return CreationPlanner044Test.reply(CreationTest.MODEL);
      },id->release.countDown())){
   var goal=goals.startCreation(CreationPlanner044Test.input());
   var effect=CreationTest.claim(goals,"creation.generate");
   var job=worker.start(CreationPlanner044Test.authority(effect));
   assertTrue(entered.await(5,TimeUnit.SECONDS));
   worker.cancel(goal.getString("id"));goals.hostDetached042();
   for(int i=0;i<500&&!worker.read(job.getString("id")).getJSONArray("timings102").getJSONObject(1).has("endedAt");i++)Thread.sleep(10);
   assertTrue(worker.read(job.getString("id")).getJSONArray("timings102").getJSONObject(1).has("endedAt"));
   assertEquals("interrupted",goals.read(goal.getString("id")).getString("status"));
   assertEquals(0,store.artifacts().length());
   return new Paused(root,goal.getString("id"),job.getString("id"));
  }finally{release.countDown();}
 }
 @Test public void reopenedSameTaskRetainsRejectedNativeFormatWithoutChangingChecks() throws Exception {
  var paused=pauseAfterRejection();var calls=new AtomicInteger();
  try(var store=new WorkspaceStore(paused.root);var scenes=new SceneStore(paused.root);var brain=new BrainService(paused.root);
      var goals=new GoalRunner(paused.root,scenes,brain,store);
      var worker=new CreationPlanner044(paused.root,goals,request->{
       calls.incrementAndGet();assertTrue(request.getString("prompt").contains("Retained rejection from this same task"));
       assertTrue(request.getString("prompt").contains("Fixture refusal"));
       assertEquals("creation.submit",request.getJSONArray("allowedTools").getString(0));
       return CreationPlanner044Test.reply(CreationTest.MODEL);
      },id->{})){
   goals.resume(paused.goal);var effect=CreationTest.claim(goals,"creation.generate");
   var job=worker.start(CreationPlanner044Test.authority(effect));CreationTest.claim(goals,"creation.test");
   var current=worker.read(job.getString("id"));assertEquals(paused.job,current.getJSONObject("retryFrom102").getString("jobId"));
   assertEquals(1,calls.get());assertEquals(1,current.getInt("attempts"));
   assertTrue(goals.read(paused.goal).getJSONArray("testSteps").similar(CreationPlanner044Test.input().getJSONArray("checks")));
   assertEquals(1,store.artifacts().length());assertEquals(1,worker.read(paused.job).getJSONArray("rejections").length());
   goals.cancel(paused.goal);
  }
 }
 @Test public void anotherTaskCannotInheritTheRejectionHint() throws Exception {
  var paused=pauseAfterRejection();
  try(var store=new WorkspaceStore(paused.root);var scenes=new SceneStore(paused.root);var brain=new BrainService(paused.root);
      var goals=new GoalRunner(paused.root,scenes,brain,store);
      var worker=new CreationPlanner044(paused.root,goals,request->{
       assertFalse(request.getString("prompt").contains("Retained rejection from this same task"));
       return CreationPlanner044Test.reply(CreationTest.MODEL);
      },id->{})){
   goals.cancel(paused.goal);var next=goals.startCreation(CreationPlanner044Test.input());
   var job=worker.start(CreationPlanner044Test.authority(CreationTest.claim(goals,"creation.generate")));
   CreationTest.claim(goals,"creation.test");assertFalse(worker.read(job.getString("id")).has("retryFrom102"));
   goals.cancel(next.getString("id"));
  }
 }
}
