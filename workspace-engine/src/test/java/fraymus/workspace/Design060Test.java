package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;

/** New compact outcomes are measurements, not assertions that a repair must succeed. */
public class Design060Test {
 @Test public void compactRepairMeasuresAllAdmittedBearingsAndRetainsFailures()throws Exception{experiment("compact",1);}
 @Test public void wideOriginalLayoutColdReviewAndPairedReservedEvaluation()throws Exception{experiment("wide",1.9);}
 private void experiment(String preset,double span)throws Exception{
  Path root=Files.createTempDirectory("design060-"+preset+"-");System.out.println("DESIGN060_NATIVE "+root);var worlds=new WorldLedger(root);var input=Load056Test.input(worlds,preset,span,0);var before=worlds.observe(input.getString("worldId"));String id,decision="";JSONObject plan=null;
  try(var store=new Counterfactual045(root,worlds)){
   id=store.prepare(before,worlds.preview(input),true,0,true,true,true,false,true).getString("id");store.begin(id);var study=Design056Test.terminal(store,id);Files.writeString(root.resolve("design-summary.json"),study.toString(2));
   assertTrue(study.toString(),java.util.Set.of("ready","failed").contains(study.getString("status")));assertTrue(study.has("repairEvidence060"));var original=study.getJSONArray("candidates").getJSONObject(0);assertTrue(original.has("payloadDesign058"));
   assertTrue(SupportRepair060.witness(store.payloadSeries058(id,"maximum-force"),store.run(id,"maximum-force")).similar(study.getJSONObject("repairEvidence060")));
   for(int i=1;i<3;i++){var c=study.getJSONArray("candidates").getJSONObject(i);if(original.getJSONObject("payloadDesign058").getBoolean("qualifies"))assertEquals("not-needed",c.getString("status"));else{assertEquals("completed",c.getString("status"));assertTrue(c.has("payloadDesign058"));}}
   for(Object raw:study.getJSONArray("candidates")){var c=(JSONObject)raw;if(c.has("payloadDesign058"))for(Object r:c.getJSONObject("payloadDesign058").getJSONArray("results"))assertFalse(((JSONObject)r).getString("caseId").startsWith("reserved"));}
   Files.writeString(root.resolve("forecast-originals.json"),store.payloadOriginals058(id).toString());
   if(study.getString("status").equals("ready")){decision=study.getString("decisionSha256");plan=store.executePlan(id,decision);assertEquals(Design058.choose(study).getString("id"),study.getJSONObject("decision").getString("selectedCandidate"));}
   else assertNull(Design058.choose(study));assertTrue(before.similar(worlds.observe(input.getString("worldId"))));
  }
  try(var store=new Counterfactual045(root,worlds)){
   if(plan!=null){assertTrue(plan.similar(store.executePlan(id,decision)));var actual=Construction054Test.nativeRun(root.resolve("fresh-assembly"),worlds.source(before),plan);Files.writeString(root.resolve("fresh-assembly.json"),WorldReport.packed(actual).toString());var result=Design058.evaluate(root.resolve("evaluation"),store,id,actual,()->false);Files.writeString(root.resolve("evaluation-summary.json"),Payload057.summary(result).toString(2));assertTrue(result.getBoolean("complete"));assertTrue(result.getBoolean("repeatable"));assertTrue(result.getBoolean("freshDesignPassed"));assertEquals("independent-support-060-v1",result.getString("version"));assertTrue(result.similar(Design058.read(root.resolve("evaluation"),store,id,actual)));Files.writeString(root.resolve("all-originals.json"),Design058.originals(root.resolve("evaluation"),store,id,actual).toString());}
   assertTrue(before.similar(worlds.observe(input.getString("worldId"))));
  }
  // Tamper with a derived diagnosis while keeping its container checksum valid.
  {Path file=root.resolve("counterfactual-045/studies.json");byte[] original=Files.readAllBytes(file);var d=AgentFiles042.loadChecked(file);d.getJSONArray("studies").getJSONObject(0).getJSONObject("repairEvidence060").put("baselineQualifies",!d.getJSONArray("studies").getJSONObject(0).getJSONObject("repairEvidence060").getBoolean("baselineQualifies"));AgentFiles042.saveChecked(file,d);try(var store=new Counterfactual045(root,worlds)){assertThrows(IllegalStateException.class,()->store.payloadOriginals058(id));}finally{Files.write(file,original);}}
 }
}
