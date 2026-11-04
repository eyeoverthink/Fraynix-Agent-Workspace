package fraymus.workspace;
import org.json.*;

/** Cost/feasibility comparison derived only from the study's original native evidence. */
final class Baseline054 {
 private Baseline054(){}
 static JSONObject compare(JSONObject study,JSONObject actual,JSONObject costs){
  JSONObject baseline=null,chosen=null;int attempts=0,ticks=0;double trialEffort=0,wall=0;
  for(Object raw:study.getJSONArray("candidates")){var c=(JSONObject)raw;if(c.getString("id").equals("maximum-force"))baseline=c;if(c.getString("id").equals(study.getJSONObject("decision").getString("selectedCandidate")))chosen=c;
   if(c.has("metrics")){attempts++;var m=c.getJSONObject("metrics");ticks+=m.getInt("nativeTicks");trialEffort+=m.optDouble("effortNs",0);wall+=c.getDouble("trialWallMillis054");}}
  if(baseline==null||chosen==null||!baseline.has("metrics"))throw new IllegalStateException("Original simpler-control trial is missing");
  var bm=baseline.getJSONObject("metrics");var am=WorldVerify.verify(actual);boolean feasible=baseline.getString("status").equals("completed")&&bm.getBoolean("passed")&&baseline.getJSONObject("structure053").getBoolean("passed");
  String verdict=!feasible?"Simpler control failed; the selected execution passed":Math.abs(am.getDouble("effortNs")-bm.getDouble("effortNs"))<1e-6&&am.getInt("nativeTicks")==bm.getInt("nativeTicks")?"Same verified motion cost; comparison required extra trials":am.getDouble("effortNs")<bm.getDouble("effortNs")?"Selected execution used less force effort; trial cost is additional":"Simpler control used no more force effort; comparison added trials";
  if(study.has("directFirst055")&&Simple055.firstPassed(study))verdict="First direct attempt verified; additional candidates were not run";
  if(study.has("loadDesign056")){feasible=feasible&&baseline.optJSONObject("load056",new JSONObject()).optBoolean("qualifies");verdict=feasible?"Original spacing met the requested load; no redesign was needed":"Original spacing failed its requested load; selected wider spacing earned fresh verification";}
  var rows=new JSONArray().put(row("One direct maximum-force attempt",baseline.getJSONObject("hypothesis053").getDouble("forceLimitN"),feasible,bm,baseline.getDouble("trialWallMillis054")))
   .put(row("Selected fresh execution",actual.getJSONObject("plan").getJSONObject("intent").getDouble("forceLimitN"),am.getBoolean("passed")&&Construction051.verifyStructure(actual).getBoolean("passed"),am,JSONObject.NULL));
  return new JSONObject().put("version","simpler-control-054-v1").put("scope",study.has("loadDesign056")?"Same stock, source and force ceiling; support spacing changes only after measured load failure. Assembly ticks/effort below exclude separately reported load sweeps.":"Same source, targets, geometry-aware route, solver and verification. Baseline is the actual maximum-force candidate representing one direct attempt. Trial wall time excludes model/report work; fresh execution wall time is not inferred from simulated ticks.")
   .put("loadCosts056",costs.optJSONObject("loadCosts056",new JSONObject())).put("verdict",verdict).put("baselineRunId",baseline.getString("runId")).put("baselineRunSha256",baseline.getString("runSha256")).put("actualRunId",actual.getString("id")).put("actualRunSha256",WorldFrames.hash(actual)).put("sourceFrameSha256",study.getString("sourceFrameSha256"))
   .put("rows",rows).put("trialAttempts",costs.getJSONArray("attempts").length()).put("allTrialTicks",costs.getLong("totalTicks")).put("allTrialEffortNs",costs.getDouble("totalEffortNs")).put("allTrialWallMillis",costs.getDouble("observedWallMillis")).put("wallTimeComplete",costs.getBoolean("wallTimeComplete")).put("originalAttemptCosts",costs.getJSONArray("attempts"))
   .put("comparisonPlusExecutionTicks",costs.getLong("totalTicks")+am.getInt("nativeTicks")).put("comparisonPlusExecutionEffortNs",costs.getDouble("totalEffortNs")+am.getDouble("effortNs")).put("baselineFeasible",feasible);
 }
 private static JSONObject row(String label,double force,boolean passed,JSONObject m,Object wall){return new JSONObject().put("label",label).put("forceLimitN",force).put("passed",passed).put("nativeTicks",m.getInt("nativeTicks")).put("forceEffortNs",m.optDouble("effortNs",0)).put("workJ",m.optDouble("workJ",0)).put("trialWallMillis",wall);}
}
