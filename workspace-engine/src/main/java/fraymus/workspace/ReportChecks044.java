package fraymus.workspace;
import org.json.*;
final class ReportChecks044 {
 static JSONObject text(String s,Object v){return new JSONObject().put("selector",s).put("expectText",String.valueOf(v));}
 static JSONObject count(String s,int v){return new JSONObject().put("selector",s).put("expectCount",v);}
 static JSONArray expected(String route,JSONObject payload,JSONObject g,Domain044.Native api)throws Exception{
  var r=payload.optJSONObject("receipt",new JSONObject());var c=g.getJSONObject("context044");var a=new JSONArray();
  switch(route){
   case"/api/agent042/report"->a.put(text("#agent042-hash",r.getString("snapshotSha256"))).put(text("#agent042-id",r.getString("agentId"))).put(text("#agent042-boot",r.getString("bootId")));
   case"/api/alignment041/report"->a.put(text("#alignment-hash",r.getString("snapshotHash"))).put(text("#alignment-id",r.getString("registrationId"))).put(text("#alignment-trial",r.getString("trialId"))).put(count("#alignment-landmarks tr",r.getInt("landmarks"))).put(count(".views svg",4));
   case"/api/runtime040/report"->a.put(text("#runtime-snapshot",r.getString("snapshotHash"))).put(text("#runtime-selected",r.getString("selectedId"))).put(count(".views svg",r.getInt("views"))).put(count("#runtime-entities tr",r.getInt("rows")));
   case"/api/knowledge/report"->a.put(text("#object-id",r.getString("objectId"))).put(text("#job-id",r.getString("jobId"))).put(count(".reference",r.getInt("sources")));
   case"/api/tabletop/report"->a.put(count("#rows tr",r.getInt("frames"))).put(count("#metric option",5)).put(text("#identity","Run "+r.getString("runId")+" · world "+r.getString("worldId")+" · "+r.getString("status")));
   case"/api/tracking/report"->{return payload.getJSONArray("checks");}
   case"/api/calibration/report"->{var j=api.call("/api/calibration/read",new JSONObject().put("id",c.getJSONObject("calibrationEvidence").getString("id")));a.put(text("#verdict",j.getJSONObject("evaluation").getBoolean("accepted")?"Accepted by declared gates":"Rejected by declared gates")).put(count("svg",2)).put(count("tbody tr",j.getJSONArray("trials").length()));}
   case"/api/measurement/report"->{var j=c.getJSONObject("comparisonEvidence");a.put(text("#comparison-result",j.getBoolean("passed")?"Within declared tolerance":"Outside declared tolerance")).put(count("#comparison-rows tr",j.getInt("count"))).put(text("#source-kind",j.getString("sourceKind")));}
   case"/api/observations/report"->{var j=api.call("/api/observations/read",new JSONObject().put("id",c.optString("observationRunId",c.getJSONObject("observationEvidence").getString("id"))));int rows=0;for(Object raw:j.getJSONArray("trace"))rows+=((JSONObject)raw).getJSONArray("objects").length();var b=j.getJSONObject("nativeBinding");a.put(count("#rows tr",rows)).put(text("#identity","World "+b.getString("worldId")+" · native revision "+b.getInt("expectedRevision")+" · observation run "+j.getString("id")+" · "+j.getString("status"))).put(count("#metric option",5));}
   case"/api/metrics/report"->{var j=api.call("/api/metrics/view",c.getJSONObject("metricSelection"));var b=j.getJSONObject("nativeBinding");a.put(count("#rows tr",j.getJSONArray("rows").length())).put(count("#metric option",4)).put(text("#identity","World "+b.getString("worldId")+" · native revision "+b.getInt("expectedRevision")+" · observation "+c.getJSONObject("metricSelection").getString("runId")+(j.getBoolean("nativeStale")?" · STALE native binding":"")));}
   default->throw new IllegalStateException("No Java report contract for "+route);
  }return a;
 }
}
