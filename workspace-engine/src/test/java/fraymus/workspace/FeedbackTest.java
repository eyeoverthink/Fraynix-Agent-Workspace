package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
public class FeedbackTest {
 @Test public void actualNativeFeedbackCandidateFacesPredeclaredStrongBaselines() throws Exception {
  var root=Files.createTempDirectory("feedback-proof-");JSONObject job;
  try(var brain=new BrainService(root)){
   job=BrainServiceTest.await(brain,brain.start(new JSONObject().put("kind","feedback-train")).getString("id"));
   Files.writeString(Path.of("build/feedback-evidence.json"),job.toString(2));assertEquals(job.optString("reason"),"completed",job.getString("status"));assertEquals(86,job.getInt("progress"));
   assertTrue(job.getJSONObject("feedbackEvaluation").toString(),job.getBoolean("accepted"));assertEquals(2,brain.activeFeedback().getInt("version"));
   var sums=new java.util.HashMap<String,Double>();double best=Double.POSITIVE_INFINITY;JSONObject winner=null;
   for(Object raw:job.getJSONArray("search")){var row=(JSONObject)raw;double mean=0;for(Object value:row.getJSONArray("trials")){var trial=(JSONObject)value;assertTrue(trial.getJSONObject("case").getString("id").startsWith("train-"));mean+=trial.getJSONObject("metrics").getDouble("score");}mean/=6;assertEquals(mean,row.getDouble("meanScore"),1e-12);if(mean<best){best=mean;winner=row.getJSONObject("gains");}}
   assertTrue(winner.similar(job.getJSONObject("selectedGains")));assertEquals(FeedbackExperiment.manifest().toString(),job.getJSONObject("feedbackProtocol").toString());
   for(Object raw:job.getJSONArray("feedbackTrials")){var trial=(JSONObject)raw;var c=trial.getJSONObject("case");var last=trial.getJSONObject("final");double endpoint=Math.abs(last.getDouble("distanceM")-c.getDouble("targetM")),speed=Math.abs(FeedbackExperiment.velocity(last,c.getDouble("angleDeg"))),settling=0;for(Object value:trial.getJSONArray("samples")){var sample=(JSONObject)value;if(sample.getInt("tick")>=300)settling=Math.max(settling,Math.abs(sample.getDouble("distanceM")-c.getDouble("targetM")));}double score=endpoint+.15*speed+.5*settling;assertEquals(score,trial.getJSONObject("metrics").getDouble("score"),1e-7);sums.merge(trial.getString("mode"),score,Double::sum);assertTrue(c.getString("id").startsWith("eval-"));}
   for(var entry:sums.entrySet())assertEquals(entry.getValue()/8,job.getJSONObject("feedbackEvaluation").getJSONObject("meanScores").getDouble(entry.getKey()),1e-7);
   for(Object raw:job.getJSONArray("feedbackTrials")){var trial=(JSONObject)raw;var c=trial.getJSONObject("case");var metrics=trial.getJSONObject("metrics");assertEquals(Math.abs(trial.getJSONObject("final").getDouble("distanceM")-c.getDouble("targetM")),metrics.getDouble("endpointErrorM"),1e-12);assertEquals(360,trial.getJSONObject("final").getInt("tick"));for(Object event:trial.getJSONArray("events")){var e=(JSONObject)event;if(e.getString("kind").equals("control"))assertTrue(Math.abs(e.getDouble("deltaVelocityMps"))<=.6);double dv=FeedbackExperiment.velocity(e.getJSONObject("after"),c.getDouble("angleDeg"))-FeedbackExperiment.velocity(e.getJSONObject("before"),c.getDouble("angleDeg"));assertEquals(e.getDouble("deltaVelocityMps"),dv,1e-5);}}
  }
  try(var reopened=new BrainService(root)){assertEquals(2,reopened.activeFeedback().getInt("version"));assertEquals(job.getJSONObject("selectedGains").getDouble("kp"),reopened.activeFeedback().getDouble("kp"),0);assertEquals(3,reopened.rollbackFeedback(2).getInt("version"));assertEquals(25,reopened.activeFeedback().getDouble("kp"),0);}
 }
 @Test public void liveNativeRunHandlesUnknownImpulsePersistsAndReplaysItsActualCommands() throws Exception {
  var root=Files.createTempDirectory("feedback-live-");String runId;
  try(var brain=new BrainService(root);var scenes=new SceneStore(root)){
   BrainServiceTest.await(brain,brain.start(new JSONObject().put("kind","feedback-train")).getString("id"));
   var scene=scenes.save(new JSONObject().put("title","Unknown live disturbance").put("parameters",new JSONObject().put("angleDeg",27).put("friction",.13)));
   var started=scenes.feedbackStart(new JSONObject().put("id",scene.getString("id")).put("expectedVersion",1).put("targetM",1.2),brain);runId=started.getString("id");
   scenes.control(new JSONObject().put("action","pause"));while(scenes.snapshot().getJSONObject("current").getInt("tick")<132)scenes.control(new JSONObject().put("action","step"));
   String op=java.util.UUID.randomUUID().toString();var kick=new JSONObject().put("runId",runId).put("operationId",op).put("deltaVelocityMps",.4);var receipt=scenes.feedbackKick(kick);assertTrue(receipt.getJSONObject("after").getInt("contacts")>0);int count=scenes.snapshot().getJSONObject("feedbackProgress").getInt("actions");scenes.feedbackKick(kick);assertEquals(count,scenes.snapshot().getJSONObject("feedbackProgress").getInt("actions"));
   assertThrows(IllegalArgumentException.class,()->scenes.feedbackKick(new JSONObject(kick.toString()).put("deltaVelocityMps",.5)));assertThrows(IllegalStateException.class,()->scenes.feedbackKick(new JSONObject(kick.toString()).put("runId",java.util.UUID.randomUUID().toString())));
   while(scenes.snapshot().getJSONObject("current").getInt("tick")<360)scenes.control(new JSONObject().put("action","step"));var run=scenes.runRecord(runId);assertTrue(run.getJSONObject("feedbackEvaluation").toString(),run.getJSONObject("feedbackEvaluation").getBoolean("passed"));assertFalse(run.getJSONObject("evaluation").getBoolean("applicable"));
   scenes.control(new JSONObject().put("action","reset"));var replay=scenes.replay(runId);assertTrue(replay.getJSONObject("replayEvaluation").getBoolean("passed"));assertEquals(run.getJSONObject("final").getDouble("distanceM"),replay.getJSONObject("final").getDouble("distanceM"),1e-6);
  }
  try(var scenes=new SceneStore(root)){assertEquals("completed",scenes.runRecord(runId).getString("status"));assertTrue(scenes.runRecord(runId).getJSONArray("feedbackEvents").length()>=31);}
 }
 @Test public void cancelledTrainingCannotPromoteAndCancelledRunStopsNativeTicks() throws Exception {
  var root=Files.createTempDirectory("feedback-cancel-");try(var brain=new BrainService(root);var scenes=new SceneStore(root)){
   var job=brain.start(new JSONObject().put("kind","feedback-train"));brain.cancel(job.getString("id"));assertEquals("cancelled",BrainServiceTest.await(brain,job.getString("id")).getString("status"));assertEquals(1,brain.activeFeedback().getInt("version"));
   var scene=scenes.save(new JSONObject().put("title","Cancel proof"));var input=new JSONObject().put("id",scene.getString("id")).put("expectedVersion",1).put("targetM",1.2);var run=scenes.feedbackStart(input,brain);var stopped=scenes.feedbackCancel(run.getString("id"));int tick=stopped.getJSONObject("current").getInt("tick");Thread.sleep(100);assertEquals(tick,scenes.snapshot().getJSONObject("current").getInt("tick"));assertFalse(stopped.getJSONObject("goalEvaluation").getBoolean("passed"));assertEquals("cancelled",scenes.runRecord(run.getString("id")).getString("status"));
   assertThrows(IllegalStateException.class,()->scenes.feedbackKick(new JSONObject().put("runId",run.getString("id")).put("operationId",java.util.UUID.randomUUID().toString()).put("deltaVelocityMps",.4)));
  }
 }
 @Test public void abruptRuntimeEndIsInterruptedAndUnsupportedGeometryRejectsBeforeStarting() throws Exception {
  var root=Files.createTempDirectory("feedback-interrupt-");String id;
  try(var brain=new BrainService(root);var scenes=new SceneStore(root)){
   var bad=scenes.save(new JSONObject().put("title","Different geometry").put("parameters",new JSONObject().put("lengthM",8)));assertThrows(IllegalArgumentException.class,()->scenes.feedbackStart(new JSONObject().put("id",bad.getString("id")).put("expectedVersion",1).put("targetM",1.2),brain));assertEquals("idle",scenes.snapshot().getString("status"));
   var s=scenes.save(new JSONObject().put("title","Interrupted runtime"));var run=scenes.feedbackStart(new JSONObject().put("id",s.getString("id")).put("expectedVersion",1).put("targetM",1.2),brain);id=run.getString("id");scenes.control(new JSONObject().put("action","pause"));
  }
  try(var reopened=new SceneStore(root)){assertEquals("interrupted",reopened.runRecord(id).getString("status"));assertEquals("idle",reopened.snapshot().getString("status"));assertThrows(IllegalStateException.class,()->reopened.replay(id));}
 }
}
