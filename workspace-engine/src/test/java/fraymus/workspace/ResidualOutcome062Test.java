package fraymus.workspace;
import org.junit.Test;
import static org.junit.Assert.*;
import org.json.*;
import java.nio.file.*;
import java.util.*;

public class ResidualOutcome062Test {
 @Test public void realNativeSelectionReservedAuditAndColdPrediction()throws Exception{
  Path p=Files.createTempDirectory("fraynix-residual062-");JSONObject j;String id;
  try(var b=new BrainService(p)){
   id=b.start(new JSONObject().put("kind","physics-residuals").put("autoPromote",true)).getString("id");waitFor(b);
   j=b.read(id);Files.writeString(p.resolve("original-experiment.json"),j.toString());System.out.println("EDITION062_NATIVE_ORIGINALS="+p);
   assertEquals(j.optString("reason"),"completed",j.getString("status"));assertEquals(60,j.getInt("progress"));assertEquals(1,b.activePolicy().getInt("version"));
   assertEquals(32,j.getJSONArray("train062").length());assertEquals(12,j.getJSONArray("select062").length());assertEquals(16,j.getJSONArray("reserved062").length());
   ResidualOutcome062.audit(j);var request=new JSONObject().put("jobId",id).put("definition",ResidualOutcome062.definitions("reserved").getJSONObject(0));
   Files.writeString(p.resolve("prediction.json"),b.residualPredict062(request).toString());
   assertTrue(b.outcomeState062().has("lastCompleted"));
   tamper(j,v->v.getJSONArray("train062").put(1,v.getJSONArray("train062").getJSONObject(0)));
   tamper(j,v->v.getJSONArray("reserved062").getJSONObject(0).getJSONObject("predictionBeforeObservation").put("linear",42));
   tamper(j,v->v.getJSONObject("evaluation062").put("qualified",!v.getJSONObject("evaluation062").getBoolean("qualified")));
   tamper(j,v->{v.getJSONObject("decision062").put("method",v.getJSONObject("decision062").getString("method").equals("cortex")?"linear":"cortex").put("selectionQualified",true);v.put("decisionSha256062",WorldFrames.hash(v.getJSONObject("decision062")));});
   tamper(j,v->{var model=v.getJSONObject("model062");model.put("meanResidualM",.012345);v.put("modelSha256062",WorldFrames.hash(model));});
   tamper(j,v->{var r=v.getJSONArray("train062").getJSONObject(0).getJSONObject("record");r.getJSONObject("physicalParameters").put("massKg",2);r.remove("sha256");r.put("sha256",WorldFrames.hash(r));});
   tamper(j,v->v.getJSONArray("reserved062").getJSONObject(0).put("predictedAt","2000-01-01T00:00:00Z"));
   System.out.println("SELECTION="+j.getJSONObject("decision062"));System.out.println("RESERVED="+j.getJSONObject("evaluation062"));
  }
  try(var b=new BrainService(p)){
   var request=new JSONObject().put("jobId",id).put("definition",ResidualOutcome062.definitions("reserved").getJSONObject(0));
   assertTrue(new JSONObject(Files.readString(p.resolve("prediction.json"))).similar(b.residualPredict062(request)));
   request.getJSONObject("definition").put("frictionRatio",1.4);try{b.residualPredict062(request);fail();}catch(IllegalArgumentException expected){}
   String stopped=b.start(new JSONObject().put("kind","physics-residuals")).getString("id");b.cancel(stopped);waitFor(b);assertEquals("cancelled",b.read(stopped).getString("status"));
   assertEquals(id,b.outcomeState062().getJSONObject("lastCompleted").getString("jobId"));assertEquals(1,b.activePolicy().getInt("version"));
   assertTrue(j.similar(b.read(id)));
  }
 }
 private interface Edit {void run(JSONObject j);}
 private static void tamper(JSONObject original,Edit e)throws Exception{var j=WorldFrames.copy(original);e.run(j);try{ResidualOutcome062.audit(j);fail("Altered evidence accepted");}catch(IllegalStateException expected){}}
 static void waitFor(BrainService b)throws Exception{long end=System.nanoTime()+180_000_000_000L;while(b.state().getBoolean("busy")&&System.nanoTime()<end)Thread.sleep(100);assertFalse(b.state().getBoolean("busy"));}
 @Test public void partitionsAreDistinctAndInsideContactDomain(){
  var ids=new HashSet<String>();for(String phase:List.of("train","select","reserved"))for(Object raw:ResidualOutcome062.definitions(phase)){
   var c=(JSONObject)raw;assertTrue(ids.add(WorldFrames.hash(c)));var p=ResidualOutcome062.physical(c);assertTrue(p.getDouble("friction")>=0&&p.getDouble("friction")<=1);assertEquals(8,ResidualOutcome062.features(c).length);
  }assertEquals(60,ids.size());
  var d=ResidualOutcome062.definitions("train").getJSONObject(0).put("angleDeg",7);try{ResidualOutcome062.features(d);fail();}catch(IllegalArgumentException expected){}
 }
 @Test public void tiesPreferSimplerMethodsAndEveryGateMatters(){
  var means=new JSONObject();var max=new JSONObject();var regimes=new JSONObject();for(String method:ResidualOutcome062.METHODS){means.put(method,.01);max.put(method,.015);regimes.put(method,new JSONObject().put("accelerating",.01).put("slowing",.01));}
  var metrics=new JSONObject().put("meanAbsoluteErrorM",means).put("maxErrorM",max).put("regimeMeanErrorM",regimes).put("count",16);
  assertEquals("analytical",ResidualOutcome062.select(metrics).getString("method"));
  means.put("linear",.008).put("cortex",.008);var decision=ResidualOutcome062.select(metrics);assertEquals("linear",decision.getString("method"));assertTrue(decision.getBoolean("selectionQualified"));assertTrue(ResidualOutcome062.evaluate(decision,metrics).getBoolean("qualified"));
  regimes.getJSONObject("linear").put("slowing",.012);assertFalse(ResidualOutcome062.evaluate(decision,metrics).getBoolean("qualified"));regimes.getJSONObject("linear").put("slowing",.01);
  max.put("linear",.021);assertFalse(ResidualOutcome062.evaluate(decision,metrics).getBoolean("qualified"));
 }
 @Test public void chatUsesExistingFactsAndReports(){assertEquals("agent",ChatPolicy044.route("Inspect physics residual learning results",new JSONObject(),false));}
 @Test public void published061OutcomeRemainsReadable()throws Exception{
  Path original=Path.of("../docs/readmes/061/original/final061/native-experiment.json.gz");
  try(var in=new java.util.zip.GZIPInputStream(Files.newInputStream(original))){
   var job=new JSONObject(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));FraynixOutcome061.audit(job);
   assertFalse(job.getJSONObject("outcomeEvaluation061").getBoolean("qualified"));
   var c=FraynixOutcome061.definitions(false).getJSONObject(0);assertTrue(FraynixOutcome061.predictions(job.getJSONObject("outcomeModel061"),c).similar(job.getJSONArray("reserved061").getJSONObject(0).getJSONObject("predictionBeforeObservation")));
  }
 }
}
