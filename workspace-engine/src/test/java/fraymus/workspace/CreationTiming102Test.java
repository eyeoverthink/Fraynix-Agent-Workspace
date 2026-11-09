package fraymus.workspace;
import org.junit.Test;
import static org.junit.Assert.*;
import org.json.*;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
public class CreationTiming102Test {
 @Test public void exceptionsRejectedOutputsAndAcceptedReplyAllRetainCosts()throws Exception{
  var root=Files.createTempDirectory("timing102-retry");var calls=new AtomicInteger();try(var store=new WorkspaceStore(root);var scenes=new SceneStore(root);var brain=new BrainService(root);var goals=new GoalRunner(root,scenes,brain,store);var worker=new CreationPlanner044(root,goals,request->{int n=calls.getAndIncrement();Thread.sleep(10);if(n==0)throw new IllegalArgumentException("Explicit fixture transient failure");var reply=CreationPlanner044Test.reply(CreationTest.MODEL);if(n==1)reply.put("text",new JSONObject().put("tool","creation.contract").put("args",new JSONObject()).toString());return reply;},id->{})){
   var goal=goals.startCreation(CreationPlanner044Test.input());var e=CreationTest.claim(goals,"creation.generate");var started=worker.start(CreationPlanner044Test.authority(e));CreationTest.claim(goals,"creation.test");var done=worker.read(started.getString("id"));var costs=done.getJSONArray("timings102");assertEquals(3,costs.length());assertEquals(2,done.getJSONArray("rejections").length());for(int i=0;i<3;i++){var c=costs.getJSONObject(i);assertEquals(i+1,c.getInt("attempt"));assertEquals(i==0?"failed":"returned",c.getString("status"));assertTrue(c.getLong("elapsedNanos")>=1_000_000);assertFalse(java.time.Instant.parse(c.getString("endedAt")).isBefore(java.time.Instant.parse(c.getString("startedAt"))));assertEquals(64,c.getString("requestSha256").length());}goals.cancel(goal.getString("id"));
  }
 }
 @Test public void interruptedPersistedCallRemainsUnknownAfterReopening()throws Exception{
  var root=Files.createTempDirectory("timing102-open");var entered=new CountDownLatch(1);var release=new CountDownLatch(1);byte[] checkpoint;String id;
  try(var store=new WorkspaceStore(root);var scenes=new SceneStore(root);var brain=new BrainService(root);var goals=new GoalRunner(root,scenes,brain,store);var worker=new CreationPlanner044(root,goals,request->{entered.countDown();release.await(5,TimeUnit.SECONDS);return CreationPlanner044Test.reply(CreationTest.MODEL);},x->{})){
   var goal=goals.startCreation(CreationPlanner044Test.input());var e=CreationTest.claim(goals,"creation.generate");id=worker.start(CreationPlanner044Test.authority(e)).getString("id");assertTrue(entered.await(5,TimeUnit.SECONDS));checkpoint=Files.readAllBytes(root.resolve("creation-044/requests.json"));release.countDown();CreationTest.claim(goals,"creation.test");goals.cancel(goal.getString("id"));
  }finally{release.countDown();}
  // Explicit unit recovery fixture: restore the genuine pre-return durable record.
  Files.write(root.resolve("creation-044/requests.json"),checkpoint);
  try(var store=new WorkspaceStore(root);var scenes=new SceneStore(root);var brain=new BrainService(root);var goals=new GoalRunner(root,scenes,brain,store);var worker=new CreationPlanner044(root,goals,request->{throw new AssertionError("No replay allowed");},x->{})){
   var job=worker.read(id);assertEquals("interrupted",job.getString("status"));var cost=job.getJSONArray("timings102").getJSONObject(0);assertEquals("running",cost.getString("status"));assertFalse(cost.has("endedAt"));assertFalse(cost.has("elapsedNanos"));
  }
 }
}
