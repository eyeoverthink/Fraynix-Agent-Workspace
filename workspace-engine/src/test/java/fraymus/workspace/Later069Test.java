package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

public class Later069Test {
 static JSONObject task(double center,double width){return new JSONObject().put("angleDeg",20).put("frictionRatio",1).put("centerM",center).put("halfWidthM",width);}
 static JSONObject state(double distance,double speed,boolean eligible){return new JSONObject().put("distanceM",distance).put("tangentMps",speed).put("contactWindow",new JSONObject().put("eligible",eligible));}
 @Test public void lateInverseHonorsBudgetGuardAndNoActionWhenAlreadyFits(){
  var t=task(.06,.007);var p=LaterContact069.inverse(t,state(0,.1,true),.16);
  assertTrue(p.getBoolean("correct"));assertEquals(.08,p.getDouble("deltaVelocityMps"),1e-12);assertEquals(.30,p.getDouble("remainingSeconds"),0);
  assertFalse(LaterContact069.inverse(task(.06,.001),state(0,.1,true),.16).getBoolean("correct"));
  assertFalse(LaterContact069.inverse(t,state(0,.1,false),0).getBoolean("correct"));
  assertFalse(LaterContact069.inverse(task(.03,.001),state(0,.1,true),0).getBoolean("correct"));
  assertFalse(LaterContact069.inverse(t,state(.1,.1,true),0).getBoolean("correct"));
  assertThrows(RuntimeException.class,()->LaterContact069.inverse(t,state(0,Double.NaN,true),0));
  assertThrows(IllegalArgumentException.class,()->LaterContact069.inverse(t,state(0,.1,true),.2));
 }
 @Test public void lateWindowChecksAllSamplesAndRejectsFuture(){
  var c=Stopping063.definition(20,1.05,.24);var original=Temporal067Test.prefix(c);var prefix=new JSONArray();
  for(int i=0;i<28;i++)prefix.put(WorldFrames.copy(original.getJSONObject(0)).put("tick",i*4).put("timeS",i/60.0));
  var t=Recovery066Test.task(.13,.045);var p=LaterContact069.plan(t,c,prefix,null);
  assertTrue(p.getJSONObject("state").getJSONObject("contactWindow").getBoolean("eligible"));
  prefix.getJSONObject(27).put("contactSeparationsM",new JSONArray());
  assertEquals(2,LaterContact069.plan(t,c,prefix,null).getJSONObject("state").getJSONObject("contactWindow").getInt("contactSamples"));
  prefix.getJSONObject(25).put("angularVelocity",new JSONArray(new double[]{0,0,3}));
  assertFalse(LaterContact069.plan(t,c,prefix,null).getJSONObject("state").getJSONObject("contactWindow").getBoolean("eligible"));
  assertThrows(IllegalArgumentException.class,()->LaterContact069.plan(t,c,new JSONArray(prefix.toString()).put(new JSONObject()),null));
 }
 @Test public void casesAreNewBalancedAndFrozenModelIsExact()throws Exception{
  var old=new HashSet<String>();for(String phase:List.of("calibration","clean","disturbed","unsupported"))for(Object x:CalibrationStudy068.cases(phase))old.add(WorldFrames.hash(((JSONObject)x).getJSONObject("task")));
  var current=new HashSet<String>();for(String phase:List.of("clean","disturbed","unsupported"))for(Object x:LaterStudy069.cases(phase)){
   var t=((JSONObject)x).getJSONObject("task");assertFalse(old.contains(WorldFrames.hash(t)));assertTrue(current.add(WorldFrames.hash(t)));
   var choices=LaterPolicy069.initial(t).getJSONObject("decisions");var prior=CalibratedPolicy068.initial(t,FrozenCalibration069.model()).getJSONObject("decisions");
   for(String m:LaterPolicy069.METHODS){var copy=WorldFrames.copy(choices.getJSONObject(m));copy.remove("later");copy.put("method",m.equals("single")||m.equals("later")?"temporal":"calibrated");assertTrue(copy.similar(prior.getJSONObject(copy.getString("method"))));}
  }
  assertEquals(120,current.size());for(String f:LaterStudy069.FAMILIES)for(int a=0;a<2;a++){int n=0;for(Object x:LaterStudy069.cases("disturbed")){var e=((JSONObject)x).getJSONObject("event");if(e.getString("family").equals(f)&&e.getInt("amplitude")==a)n++;}assertEquals(6,n);}
  var published=new JSONObject(Files.readString(Path.of("../docs/readmes/068/verification.json"))).getJSONObject("model");assertTrue(published.similar(FrozenCalibration069.model()));
 }
 @Test public void feesIncludeFailedAttemptAndEveryLaterObservation(){
  var decision=new JSONObject().put("method","later").put("execute",true).put("later",true).put("speedMps",.2);
  var r=new JSONObject().put("summary",new JSONObject().put("endpointM",.3)).put("timing069",new JSONObject().put("nativeWallMillis",1))
   .put("externalReceipts",new JSONArray()).put("laterPlan",new JSONObject().put("correct",false).put("deltaVelocityMps",0));
  var o=LaterPolicy069.outcome(task(.06,.01),decision,r);assertEquals(3,o.getInt("observations"));assertEquals(1,o.getInt("replans"));assertEquals(1,o.getInt("actions"));assertEquals(-2.12,o.getDouble("utility"),1e-12);
  r.getJSONObject("laterPlan").put("correct",true).put("deltaVelocityMps",.08);o=LaterPolicy069.outcome(task(.06,.01),decision,r);assertEquals(2,o.getInt("actions"));assertEquals(.28,o.getDouble("absoluteImpulseNs"),1e-12);
  var refused=new JSONObject().put("method","laterCalibrated").put("execute",false).put("later",true).put("screen",new JSONObject()).put("calibration068",new JSONObject());
  o=LaterPolicy069.outcome(task(.06,.01),refused,null);assertEquals(0,o.getInt("observations"));assertEquals(-.157,o.getDouble("utility"),1e-12);
 }
 @Test public void gateRejectsAddedCostsHarmsAndLateFailures(){
  var e=new JSONObject().put("methods",new JSONObject().put("single",new JSONObject().put("successes",30).put("allTaskSuccessRate",.5))
    .put("later",new JSONObject().put("successes",36).put("allTaskSuccessRate",.6).put("allTaskMissRate",.4)))
    .put("pairedLaterMinus",new JSONObject().put("single",new JSONObject().put("meanDelta",.1)));
  assertTrue(LaterStudy069.qualifies(e,"clean","single"));assertTrue(LaterStudy069.qualifies(e,"disturbed","single"));
  e.getJSONObject("methods").getJSONObject("later").put("allTaskMissRate",.46);assertFalse(LaterStudy069.qualifies(e,"disturbed","single"));
  e.getJSONObject("methods").getJSONObject("later").put("successes",29);assertFalse(LaterStudy069.qualifies(e,"clean","single"));
 }
 @Test public void nativeLaterStudyColdRestoreCostSourceChronologyAndStop()throws Exception{
  String resume=System.getenv("FRAYNIX069_NATIVE_RESUME");Path root=resume==null?Files.createTempDirectory("fraynix-later069-"):Path.of(resume);
  System.out.println("EDITION069_NATIVE_ROOT="+root);String id,hash;JSONObject job;
  try(var brain=new BrainService(root)){
   if(resume==null){id=brain.start(new JSONObject().put("kind","later-recovery").put("autoPromote",false)).getString("id");Recovery066Test.waitFor(brain);}else id=new JSONObject(Files.readString(root.resolve("original-experiment.json"))).getString("id");
   job=brain.read(id);if(!Files.exists(root.resolve("original-attempt.json")))Files.writeString(root.resolve("original-attempt.json"),job.toString());
   assertEquals(job.optString("reason"),"completed",job.getString("status"));LaterStudy069.audit(job);
   if(Files.exists(root.resolve("original-experiment.json")))assertTrue(new JSONObject(Files.readString(root.resolve("original-experiment.json"))).similar(job));else Files.writeString(root.resolve("original-experiment.json"),job.toString());
   hash=WorldFrames.hash(job);System.out.println("LATER069_CLEAN="+job.getJSONObject("cleanEvaluation069"));System.out.println("LATER069_DISTURBED="+job.getJSONObject("disturbedEvaluation069"));
   assertEquals(484,job.getInt("progress"));assertEquals(1,brain.activePolicy().getInt("version"));
   var record=job.getJSONArray("disturbed069").getJSONObject(0).getJSONObject("results").getJSONObject("later").getJSONObject("record");
   for(String kind:List.of("plan","event","time","source")){
    var bad=WorldFrames.copy(record);bad.remove("sha256");
    switch(kind){case "plan"->bad.getJSONObject("laterPlan").put("decisionTick",112);case "event"->bad.getJSONArray("externalReceipts").getJSONObject(0).getJSONObject("after").getJSONArray("velocity").put(0,999);case "time"->bad.put("laterPlannedAt","2099-01-01T00:00:00Z");case "source"->bad.put("source","forged");}
    bad.put("sha256",WorldFrames.hash(bad));assertThrows(IllegalStateException.class,()->LaterTrial069.verify(bad));
   }
   var altered=WorldFrames.copy(job);altered.getJSONObject("reusedModel069").getJSONArray("knots").getJSONObject(0).put("rate",.999);assertThrows(IllegalStateException.class,()->LaterStudy069.audit(altered));
   var trace=brain.laterTrace069(new JSONObject().put("jobId",id).put("index",0).put("phase","disturbed"));Files.writeString(root.resolve("trace.json"),trace.toString());assertEquals(46,trace.getJSONObject("results").getJSONObject("later").getJSONArray("series").length());
   var facts=new JSONObject().put("schema","persistent-java-agent-042-v1").put("later069",brain.laterState069()).put("calibrated068",new JSONObject().put("history","x".repeat(60000)));
   var task=Domain044Test.task("agent");task.getJSONObject("context044").put("agent042Facts",facts);var projection=ChatChain052.context(task);
   var packed=projection.getJSONObject("agent042Facts").getJSONObject("later069").getJSONObject("lastCompleted");var original=facts.getJSONObject("later069").getJSONObject("lastCompleted");
   for(String phase:List.of("clean","disturbed","unsupported"))for(String name:List.of("methods","pairedLaterMinus","diagnostics")){
    var table=packed.getJSONObject(phase).getJSONObject(name);var columns=packed.getJSONObject("tableColumns069").getJSONArray(table.getString("columnsRef"));var decoded=new JSONObject();
    for(Object x:table.getJSONArray("rows")){var row=(JSONArray)x;var values=new JSONObject();for(int k=0;k<columns.length();k++)values.put(columns.getString(k),row.get(k+1));decoded.put(row.getString(0),values);}assertTrue(original.getJSONObject(phase).getJSONObject(name).similar(decoded));
   }
   projection.put("realisticOverhead","x".repeat(2500));ChatChain052.exact(projection,12500);assertTrue(facts.has("calibrated068"));assertEquals(hash,WorldFrames.hash(brain.read(id)));
  }
  try(var brain=new BrainService(root)){
   assertEquals(hash,WorldFrames.hash(brain.read(id)));assertTrue(new JSONObject(Files.readString(root.resolve("trace.json"))).similar(brain.laterTrace069(new JSONObject().put("jobId",id).put("index",0).put("phase","disturbed"))));
   String stop=brain.start(new JSONObject().put("kind","later-recovery")).getString("id");brain.cancel(stop);Recovery066Test.waitFor(brain);assertEquals("cancelled",brain.read(stop).getString("status"));assertEquals(id,brain.laterState069().getJSONObject("lastCompleted").getString("jobId"));
  }
 }
}



