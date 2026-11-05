package fraymus.workspace;
import org.json.*;
import java.util.*;
/** Lossless current outcome tables and explicit references for unrelated history. */
final class LaterContext069 {
 static boolean project(JSONObject c){
  var facts=c.optJSONObject("agent042Facts");if(facts==null)return false;var state=facts.optJSONObject("later069");if(state==null||!state.has("lastCompleted"))return false;
  var s=state.getJSONObject("lastCompleted");if(!s.has("clean"))return false;
  var other=new JSONObject();for(String key:List.of("clock","witness","limits","observations","appraisal","neural","fraynixGPT061","residual062","stopping063","contact064","decision065","recovery066","temporal067","calibrated068","recentWitnesses"))if(facts.has(key))other.put(key,facts.remove(key));
  if(!other.isEmpty())facts.put("relatedEvidence069",new JSONObject().put("fields",new JSONArray(new TreeSet<>(other.keySet()))).put("sha256",WorldFrames.hash(other)).put("route","/api/agent042/state").put("scope","Planning reference only. Original task facts and report retain all source values."));
  var columns=new JSONObject();
  for(String phase:List.of("clean","disturbed","unsupported")){
   var e=s.getJSONObject(phase);var groups=e.getJSONObject("subgroups");e.remove("subgroups");
   e.put("subgroupSource069",new JSONObject().put("names",new JSONArray(new TreeSet<>(groups.keySet()))).put("sha256",WorldFrames.hash(groups)).put("route","/api/brain/read").put("id",s.getString("jobId")).put("field",phase+"Evaluation069.subgroups"));
   for(String name:List.of("methods","pairedLaterMinus","diagnostics")){
    var data=e.getJSONObject(name);var keys=new JSONArray(new TreeSet<>(data.getJSONObject(new TreeSet<>(data.keySet()).first()).keySet()));String ref=name;
    if(columns.has(ref)&&!columns.getJSONArray(ref).similar(keys))throw new IllegalStateException("Metric schema differs");columns.put(ref,keys);
    var rows=new JSONArray();for(String method:new TreeSet<>(data.keySet())){var row=new JSONArray().put(method);for(Object key:keys)row.put(data.getJSONObject(method).get((String)key));rows.put(row);}
    e.put(name,new JSONObject().put("columnsRef",ref).put("rows",rows));
   }
  }
  s.put("tableColumns069",columns).put("tableMeaning069","Each row is [policy, values in column order]. All aggregate metrics, paired comparisons, gates, costs and diagnostics retained. Subgroups referenced in original checked report.");
  return true;
 }
}
