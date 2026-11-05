package fraymus.workspace;
import org.json.*;
import java.util.*;
/** Lossless current outcome tables and explicit references for unrelated history. */
final class UsefulContext072 {
 static boolean project(JSONObject c){
  var facts=c.optJSONObject("agent042Facts");if(facts==null)return false;var state=facts.optJSONObject("useful072");if(state==null||!state.has("lastCompleted"))return false;
  var s=state.getJSONObject("lastCompleted");if(!s.has("clean"))return false;
  var other=new JSONObject();for(String key:List.of("clock","witness","limits","observations","appraisal","neural","fraynixGPT061","residual062","stopping063","contact064","decision065","recovery066","temporal067","calibrated068","later069","adaptive070","uncertain071","recentWitnesses"))if(facts.has(key))other.put(key,facts.remove(key));
  if(!other.isEmpty())facts.put("relatedEvidence072",new JSONObject().put("fields",new JSONArray(new TreeSet<>(other.keySet()))).put("sha256",WorldFrames.hash(other)).put("route","/api/agent042/state").put("scope","Planning reference only. Original task facts and report retain all source values."));
  var columns=new JSONObject();
  for(String phase:List.of("clean","disturbed","unsupported")){
   var e=s.getJSONObject(phase);var groups=e.getJSONObject("subgroups");e.remove("subgroups");
   e.put("subgroupSource072",new JSONObject().put("names",new JSONArray(new TreeSet<>(groups.keySet()))).put("sha256",WorldFrames.hash(groups)).put("route","/api/brain/read").put("id",s.getString("jobId")).put("field",phase+"Evaluation072.subgroups"));
   for(String name:List.of("methods","pairedUsefulMinus"))table(e,name,name,columns);
   table(e.getJSONObject("coverage"),"cells","coverageCells",columns);
   table(e.getJSONObject("diagnostics"),"goals","goalComparisons",columns);
  }
  s.put("tableColumns072",columns).put("tableMeaning072","Each row is [policy, values in column order]. All aggregate metrics, paired comparisons, gates, costs and diagnostics retained. Subgroups referenced in original checked report.");
  return true;
 }
 private static void table(JSONObject owner,String name,String ref,JSONObject columns){
  var data=owner.getJSONObject(name);var keys=new JSONArray(new TreeSet<>(data.getJSONObject(new TreeSet<>(data.keySet()).first()).keySet()));
  if(columns.has(ref)&&!columns.getJSONArray(ref).similar(keys))throw new IllegalStateException("Metric schema differs");columns.put(ref,keys);
  var rows=new JSONArray();for(String identity:new TreeSet<>(data.keySet())){var values=data.getJSONObject(identity);if(!values.keySet().equals(new TreeSet<>(keys.toList())))throw new IllegalStateException("Metric row differs");var row=new JSONArray().put(identity);for(Object key:keys)row.put(values.get((String)key));rows.put(row);}
  owner.put(name,new JSONObject().put("columnsRef",ref).put("rows",rows));
 }
}
