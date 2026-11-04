package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Design059Test {
 @Test public void actualAsymmetricComparisonNewPayloadsAndMeasuredEvidencePerformance()throws Exception {
  Path root=Files.createTempDirectory("design059-native-");System.out.println("DESIGN059_NATIVE "+root);var worlds=new WorldLedger(root);var input=Load056Test.input(worlds,"wide",.8,0);var before=worlds.observe(input.getString("worldId"));String id,decision;JSONObject plan;
  try(var store=new Counterfactual045(root,worlds)){
   id=store.prepare(before,worlds.preview(input),true,0,true,true,true,true).getString("id");store.begin(id);var study=Design056Test.terminal(store,id);Files.writeString(root.resolve("design-summary.json"),study.toString(2));
   assertEquals(study.optString("reason"),"ready",study.getString("status"));assertFalse(study.getJSONArray("candidates").getJSONObject(0).getJSONObject("payloadDesign058").getBoolean("qualifies"));for(Object raw:study.getJSONArray("candidates")){var c=(JSONObject)raw;assertEquals("completed",c.getString("status"));assertTrue(c.has("payloadDesign058"));for(Object r:c.getJSONObject("payloadDesign058").getJSONArray("results"))assertFalse(((JSONObject)r).getString("caseId").startsWith("reserved"));}
   decision=study.getString("decisionSha256");plan=store.executePlan(id,decision);assertNotEquals("maximum-force",study.getJSONObject("decision").getString("selectedCandidate"));
   var run=store.run(id,"maximum-force");Path seriesRoot=root.resolve("counterfactual-045/branches").resolve(id).resolve("payload-058");var series=new PayloadSeries058("design",true);var timings=new JSONArray();Evidence059.clear();
   var uncached=series.readUncached(seriesRoot,run);String expected=JsonStream058.sha256(uncached);assertEquals(expected,JsonStream058.sha256(series.read(seriesRoot,run)));uncached=null;
   for(int round=0;round<5;round++){double cold=0,warm=0;for(int i=0;i<2;i++){boolean cached=(round+i)%2==1;long began=System.nanoTime();var value=cached?series.read(seriesRoot,run):series.readUncached(seriesRoot,run);double ms=(System.nanoTime()-began)/1e6;assertEquals(expected,JsonStream058.sha256(value));if(cached)warm=ms;else cold=ms;}timings.put(new JSONObject().put("uncachedMillis",cold).put("cachedMillis",warm));}
   Files.writeString(root.resolve("performance.json"),new JSONObject().put("method","Five alternating pairs after warmup; same actual six-attempt native design record, exact serialized output SHA256").put("outputSha256",expected).put("timings",timings).put("stats",Evidence059.stats()).toString(2));
  }
  try(var store=new Counterfactual045(root,worlds)){
   Evidence059.clear();assertTrue(plan.similar(store.executePlan(id,decision)));assertTrue(before.similar(worlds.observe(input.getString("worldId"))));var actual=Construction054Test.nativeRun(root.resolve("fresh-assembly"),worlds.source(before),plan);Files.writeString(root.resolve("fresh-assembly.json"),WorldReport.packed(actual).toString());Path evaluation=root.resolve("evaluation");var calls=new AtomicInteger();assertThrows(InterruptedException.class,()->Design058.evaluate(evaluation,store,id,actual,()->calls.incrementAndGet()>=3));
   var result=Design058.evaluate(evaluation,store,id,actual,()->false);assertTrue(result.getBoolean("complete"));assertTrue(result.getBoolean("repeatable"));assertTrue(result.getBoolean("freshDesignPassed"));assertEquals("asymmetric-payload-059-v1",result.getString("version"));Files.writeString(root.resolve("evaluation-summary.json"),Payload057.summary(result).toString(2));assertTrue(result.similar(Design058.read(evaluation,store,id,actual)));result=null;System.gc();
   Files.writeString(root.resolve("all-originals.json"),Design058.originals(evaluation,store,id,actual).toString());assertTrue(before.similar(worlds.observe(input.getString("worldId"))));var pin=evaluation.resolve(actual.getString("id")).resolve("evaluation.json");var saved=AgentFiles042.loadChecked(pin);Files.writeString(root.resolve("original-pin.json"),saved.toString());saved.getJSONObject("binding").put("decisionSha256","0".repeat(64));AgentFiles042.saveChecked(pin,saved);assertThrows(IllegalStateException.class,()->Design058.read(evaluation,store,id,actual));
  }
 }
}
