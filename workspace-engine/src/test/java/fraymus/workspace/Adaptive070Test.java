package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;
public class Adaptive070Test {
 static void await070(BrainService brain)throws Exception{long end=System.currentTimeMillis()+3600000;while(brain.state().getBoolean("busy")&&System.currentTimeMillis()<end)Thread.sleep(500);assertFalse("Adaptive validation exceeded one hour",brain.state().getBoolean("busy"));}
 static JSONObject task(double ratio,double center,double width){return new JSONObject().put("angleDeg",20).put("frictionRatio",ratio).put("centerM",center).put("halfWidthM",width);}
 @Test public void signedPhysicsStopsSticksAndRestartsDownhill(){
  var t=task(1.1,.1,.01);double g=9.81*Math.sin(Math.toRadians(20)),a=-.1*g,v=.1,z=-v/a;
  var s=ObservationSchedule070.advance(t,0,v,1);assertEquals(v*z+.5*a*z*z,s[0],1e-12);assertEquals(0,s[1],0);
  double negativeZero=.1/(2.1*g);s=ObservationSchedule070.advance(t,0,-.1,1);assertEquals(-.1*negativeZero+.5*2.1*g*negativeZero*negativeZero,s[0],1e-12);assertEquals(0,s[1],0);
  t=task(.9,.1,.01);double zero=.1/(1.9*g),rest=1-zero;s=ObservationSchedule070.advance(t,0,-.1,1);assertEquals(-.1*zero+.5*1.9*g*zero*zero+.5*.1*g*rest*rest,s[0],1e-12);assertEquals(.1*g*rest,s[1],1e-12);
  assertThrows(IllegalArgumentException.class,()->ObservationSchedule070.advance(task(1,.1,.01),0,Double.NaN,1));assertThrows(IllegalArgumentException.class,()->ObservationSchedule070.advance(task(1,.1,.01),0,0,-1));
 }
 @Test public void eventBoundaryIsAppliedExactlyOnce(){
  var event=new JSONObject().put("eventTick",108).put("deltaMps",.08);var t=task(1,.1,.01);
  var full=ObservationSchedule070.propagate(t,new double[]{0,.1},event,60,180);var split=ObservationSchedule070.propagate(t,new double[]{0,.1},event,60,108);split=ObservationSchedule070.propagate(t,split,event,108,180);
  assertArrayEquals(full,split,1e-12);assertEquals(.1*.5+.08*.3,full[0],1e-12);assertEquals(.18,full[1],1e-12);
 }
 @Test public void scheduleHasNoFutureInputAndUsesOnlyPaidFirstState(){
  var t=task(1,.15,.045);var d=new JSONObject().put("recover",false).put("speedMps",.2);var s=ObservationSchedule070.plan(t,d,null);
  assertEquals(-1,s.getInt("selectedTick"));assertEquals(3,s.getJSONArray("candidates").length());assertEquals(.005,s.getDouble("scheduleFee"),0);
  assertEquals("predicted",s.getJSONObject("anchor").getString("kind"));assertFalse(s.toString().contains("actualEvent"));
  double weight=0;for(Object raw:ObservationSchedule070.scenarios())weight+=((JSONObject)raw).getDouble("weight");assertEquals(1,weight,1e-12);
  var first=new JSONObject().put("state",new JSONObject().put("kind","measured-window").put("distanceM",.02).put("tangentMps",.1)).put("deltaVelocityMps",.08);
  d.put("recover",true);var anchor=ObservationSchedule070.anchor(t,d,first);assertEquals(.18,anchor.getDouble("tangentMps"),1e-12);assertEquals(.08,anchor.getDouble("spentCorrectionMps"),0);
  assertThrows(IllegalArgumentException.class,()->ObservationSchedule070.anchor(t,d,null));
  first.getJSONObject("state").put("kind","future");assertThrows(IllegalArgumentException.class,()->ObservationSchedule070.anchor(t,d,first));
 }
 @Test public void selectedWindowsRejectFutureAndUnsafeEvidence(){
  var c=Stopping063.definition(20,1.05,.24);var seed=Temporal067Test.prefix(c).getJSONObject(0);
  for(int tick:ObservationSchedule070.TICKS){var prefix=new JSONArray();for(int i=0;i<=tick/4;i++)prefix.put(WorldFrames.copy(seed).put("tick",i*4).put("timeS",i/60.0));
   var plan=AdaptiveContact070.plan(Recovery066Test.task(.13,.045),c,prefix,null,tick);assertTrue(plan.getJSONObject("state").getJSONObject("contactWindow").getBoolean("eligible"));assertEquals((180-tick)/240.0,plan.getDouble("remainingSeconds"),0);
   prefix.getJSONObject(tick/4-1).put("angularVelocity",new JSONArray(new double[]{0,0,3}));assertFalse(AdaptiveContact070.plan(Recovery066Test.task(.13,.045),c,prefix,null,tick).getJSONObject("state").getJSONObject("contactWindow").getBoolean("eligible"));
   prefix.put(new JSONObject());assertThrows(IllegalArgumentException.class,()->AdaptiveContact070.plan(Recovery066Test.task(.13,.045),c,prefix,null,tick));
  }
  assertThrows(IllegalArgumentException.class,()->AdaptiveContact070.inverse(task(1,.1,.01),Later069Test.state(0,.1,true),0,108));
 }
 @Test public void newCasesPreserveFirstChoicesAndBalanceTiming(){
  var old=new HashSet<String>();for(String phase:List.of("clean","disturbed","unsupported")){for(Object x:LaterStudy069.cases(phase))old.add(WorldFrames.hash(((JSONObject)x).getJSONObject("task")));for(Object x:CalibrationStudy068.cases(phase))old.add(WorldFrames.hash(((JSONObject)x).getJSONObject("task")));}
  var ids=new HashSet<String>();for(String phase:List.of("clean","disturbed","unsupported"))for(Object raw:AdaptiveStudy070.cases(phase)){var t=((JSONObject)raw).getJSONObject("task");assertFalse(old.contains(WorldFrames.hash(t)));assertTrue(ids.add(WorldFrames.hash(t)));var current=AdaptivePolicy070.initial(t).getJSONObject("decisions");var prior=LaterPolicy069.initial(t).getJSONObject("decisions");
   for(String m:AdaptivePolicy070.METHODS){var d=WorldFrames.copy(current.getJSONObject(m));d.remove("adaptive");d.put("method",m.endsWith("Calibrated")?"laterCalibrated":"later");assertTrue(d.similar(prior.getJSONObject(d.getString("method"))));}}
  assertEquals(120,ids.size());for(String family:AdaptiveStudy070.FAMILIES)for(int amplitude=0;amplitude<2;amplitude++)for(int timing=0;timing<2;timing++){int n=0;for(Object raw:AdaptiveStudy070.cases("disturbed")){var e=((JSONObject)raw).getJSONObject("event");if(e.getString("family").equals(family)&&e.getInt("amplitude")==amplitude&&e.getInt("timing")==timing)n++;}assertEquals(3,n);}
 }
 @Test public void skipPaysSchedulerButNoExtraSensingAndFailureStillCounts(){
  var d=new JSONObject().put("method","adaptive").put("adaptive",true).put("execute",true).put("later",true).put("speedMps",.2);
  var r=new JSONObject().put("summary",new JSONObject().put("endpointM",.3)).put("timing070",new JSONObject().put("nativeWallMillis",1).put("schedulerMillis",.25)).put("externalReceipts",new JSONArray());
  var o=AdaptivePolicy070.outcome(task(1,.06,.01),d,r);assertEquals(0,o.getInt("observations"));assertEquals(1,o.getInt("scheduleCalls"));assertEquals(-2.025,o.getDouble("utility"),1e-12);
  r.put("laterPlan",new JSONObject().put("correct",false).put("deltaVelocityMps",0));o=AdaptivePolicy070.outcome(task(1,.06,.01),d,r);assertEquals(3,o.getInt("extraObservations"));assertEquals(-2.125,o.getDouble("utility"),1e-12);
 }
 @Test public void qualificationRequiresSensingSavingsAndPreservesLateMisses(){
  var before=new JSONObject().put("successes",40).put("extraObservations",144);var after=new JSONObject().put("successes",39).put("extraObservations",108);
  var e=new JSONObject().put("methods",new JSONObject().put("fixed",before).put("adaptive",after)).put("pairedAdaptiveMinus",new JSONObject().put("fixed",new JSONObject().put("meanDelta",.01)));
  assertTrue(AdaptiveStudy070.qualifies(e,"clean","fixed"));after.put("extraObservations",111);assertFalse(AdaptiveStudy070.qualifies(e,"clean","fixed"));after.put("successes",37);assertFalse(AdaptiveStudy070.qualifies(e,"disturbed","fixed"));
 }
 @Test public void nativeAdaptiveStudyColdRestoreCostSourceChronologyAndStop()throws Exception{
  String resume=System.getenv("FRAYNIX070_NATIVE_RESUME");Path root=resume==null?Files.createTempDirectory("fraynix-adaptive070-"):Path.of(resume);
  System.out.println("EDITION070_NATIVE_ROOT="+root);String id,hash;JSONObject job;
  try(var brain=new BrainService(root)){
   if(resume==null){id=brain.start(new JSONObject().put("kind","adaptive-observation").put("autoPromote",false)).getString("id");await070(brain);}else id=new JSONObject(Files.readString(root.resolve("original-experiment.json"))).getString("id");
   job=brain.read(id);if(!Files.exists(root.resolve("original-attempt.json")))Files.writeString(root.resolve("original-attempt.json"),job.toString());
   assertEquals(job.optString("reason"),"completed",job.getString("status"));AdaptiveStudy070.audit(job);
   if(Files.exists(root.resolve("original-experiment.json")))assertTrue(new JSONObject(Files.readString(root.resolve("original-experiment.json"))).similar(job));else Files.writeString(root.resolve("original-experiment.json"),job.toString());
   hash=WorldFrames.hash(job);System.out.println("ADAPTIVE070_CLEAN="+job.getJSONObject("cleanEvaluation070"));System.out.println("ADAPTIVE070_DISTURBED="+job.getJSONObject("disturbedEvaluation070"));
   assertEquals(488,job.getInt("progress"));assertEquals(1,brain.activePolicy().getInt("version"));
   var record=job.getJSONArray("disturbed070").getJSONObject(0).getJSONObject("results").getJSONObject("adaptive").getJSONObject("record");
   for(String kind:List.of("plan","event","time","source","value")){
    var bad=WorldFrames.copy(record);bad.remove("sha256");
    switch(kind){case "plan"->bad.getJSONObject("schedule070").put("selectedTick",108);case "event"->bad.getJSONArray("externalReceipts").getJSONObject(0).getJSONObject("after").getJSONArray("velocity").put(0,999);case "time"->bad.put("schedulePlannedAt","2099-01-01T00:00:00Z");case "source"->bad.put("source","forged");case "value"->bad.getJSONObject("schedule070").getJSONArray("candidates").getJSONObject(0).put("gain",99);}
    bad.put("sha256",WorldFrames.hash(bad));assertThrows(IllegalStateException.class,()->AdaptiveTrial070.verify(bad));
   }
   var altered=WorldFrames.copy(job);altered.getJSONObject("reusedModel070").getJSONArray("knots").getJSONObject(0).put("rate",.999);assertThrows(IllegalStateException.class,()->AdaptiveStudy070.audit(altered));
   var trace=brain.adaptiveTrace070(new JSONObject().put("jobId",id).put("index",0).put("phase","disturbed"));Files.writeString(root.resolve("trace.json"),trace.toString());assertEquals(46,trace.getJSONObject("results").getJSONObject("adaptive").getJSONArray("series").length());
   var facts=new JSONObject().put("schema","persistent-java-agent-042-v1").put("adaptive070",brain.adaptiveState070()).put("calibrated068",new JSONObject().put("history","x".repeat(60000)));
   var task=Domain044Test.task("agent");task.getJSONObject("context044").put("agent042Facts",facts);var projection=ChatChain052.context(task);
   var packed=projection.getJSONObject("agent042Facts").getJSONObject("adaptive070").getJSONObject("lastCompleted");var original=facts.getJSONObject("adaptive070").getJSONObject("lastCompleted");
   for(String phase:List.of("clean","disturbed","unsupported"))for(String name:List.of("methods","pairedAdaptiveMinus","diagnostics")){
    var table=packed.getJSONObject(phase).getJSONObject(name);var columns=packed.getJSONObject("tableColumns070").getJSONArray(table.getString("columnsRef"));var decoded=new JSONObject();
    for(Object x:table.getJSONArray("rows")){var row=(JSONArray)x;var values=new JSONObject();for(int k=0;k<columns.length();k++)values.put(columns.getString(k),row.get(k+1));decoded.put(row.getString(0),values);}assertTrue(original.getJSONObject(phase).getJSONObject(name).similar(decoded));
   }
   projection.put("realisticOverhead","x".repeat(2500));ChatChain052.exact(projection,12500);assertTrue(facts.has("calibrated068"));assertEquals(hash,WorldFrames.hash(brain.read(id)));
  }
  try(var brain=new BrainService(root)){
   assertEquals(hash,WorldFrames.hash(brain.read(id)));assertTrue(new JSONObject(Files.readString(root.resolve("trace.json"))).similar(brain.adaptiveTrace070(new JSONObject().put("jobId",id).put("index",0).put("phase","disturbed"))));
   String stop=brain.start(new JSONObject().put("kind","adaptive-observation")).getString("id");brain.cancel(stop);Recovery066Test.waitFor(brain);assertEquals("cancelled",brain.read(stop).getString("status"));assertEquals(id,brain.adaptiveState070().getJSONObject("lastCompleted").getString("jobId"));
  }
 }
}
