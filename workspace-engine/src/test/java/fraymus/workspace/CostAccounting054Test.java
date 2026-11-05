package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;

/** Actual interrupted native attempts are charged; missing timing is explicitly unknown. */
public class CostAccounting054Test {
 @Test public void resumedStudyChargesAllOriginalAttemptsAndNeverInventsWallTime()throws Exception{
  var root=Files.createTempDirectory("cost-accounting054-");var worlds=new WorldLedger(root);var args=Counterfactual045Test.input(worlds,Counterfactual045Test.control());var before=worlds.observe(args.getString("worldId"));String id;
  try(var store=new Counterfactual045(root,worlds)){
   id=store.prepare(before,worlds.preview(args)).getString("id");store.begin(id);long until=System.nanoTime()+120_000_000_000L;JSONObject state;
   do{state=store.read(id);if(state.getJSONArray("candidates").getJSONObject(0).getString("status").equals("completed")&&state.getJSONArray("candidates").getJSONObject(1).getString("status").equals("running"))break;Thread.sleep(2);}while(System.nanoTime()<until);assertEquals("running",state.getJSONArray("candidates").getJSONObject(1).getString("status"));
  }
  JSONObject costs;
  try(var store=new Counterfactual045(root,worlds)){
   store.resume(id);store.begin(id);Counterfactual045Test.ready(store,id);costs=store.costs054(id);var exports=store.export(id);long ticks=0;double effort=0;assertTrue(exports.getJSONArray("originalAttempts").length()>=4);
   for(Object raw:exports.getJSONArray("originalAttempts")){var packed=((JSONObject)raw).getJSONObject("run");byte[] bytes;try(var in=new java.util.zip.GZIPInputStream(new java.io.ByteArrayInputStream(java.util.Base64.getDecoder().decode(packed.getString("gzipBase64"))))){bytes=in.readAllBytes();}var run=new JSONObject(new String(bytes,java.nio.charset.StandardCharsets.UTF_8));var m=WorldVerify.verify(run);ticks+=m.getInt("nativeTicks");for(Object sample:run.getJSONArray("samples"))for(Object step:((JSONObject)sample).getJSONArray("integration"))effort+=WorldVerify.norm(((JSONArray)step).getJSONArray(2))/240.0;}
   assertEquals(exports.getJSONArray("originalAttempts").length(),costs.getJSONArray("attempts").length());assertEquals(ticks,costs.getLong("totalTicks"));assertEquals(effort,costs.getDouble("totalEffortNs"),1e-7);assertTrue(before.similar(worlds.observe(args.getString("worldId"))));Files.writeString(root.resolve("original.json"),exports.toString());
  }
  // Explicit metadata-loss fixture, not a fabricated physical trace or successful run.
  var file=root.resolve("counterfactual-045/studies.json");var data=AgentFiles042.loadChecked(file);var first=data.getJSONArray("studies").getJSONObject(0).getJSONArray("candidates").getJSONObject(0);first.remove("attemptCosts054");AgentFiles042.saveChecked(file,data);
  try(var store=new Counterfactual045(root,worlds)){var missing=store.costs054(id);assertFalse(missing.getBoolean("wallTimeComplete"));assertTrue(missing.getJSONArray("attempts").getJSONObject(0).isNull("wallMillis"));assertEquals(costs.getLong("totalTicks"),missing.getLong("totalTicks"));assertEquals(costs.getDouble("totalEffortNs"),missing.getDouble("totalEffortNs"),0);Files.writeString(root.resolve("costs.json"),new JSONObject().put("complete",costs).put("metadataLossFixture",missing).toString());}
  System.out.println("COST054_ORIGINAL "+root);
 }
}
