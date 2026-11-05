package fraymus.workspace;
import org.json.*;
import java.util.*;

/** Planner view only: original task facts and checked reports retain the full calibration evidence. */
final class CalibrationContext068 {
    static void project(JSONObject context){
        var facts=context.optJSONObject("agent042Facts");if(facts==null)return;var state=facts.optJSONObject("calibrated068");if(state==null||!state.has("lastCompleted"))return;
        var study=state.getJSONObject("lastCompleted");if(!study.has("clean"))return;
        if(facts.has("recentWitnesses")){var rows=facts.getJSONArray("recentWitnesses");facts.remove("recentWitnesses");facts.put("witnessHistory068",new JSONObject().put("count",rows.length()).put("sha256",WorldFrames.hash(new JSONObject().put("rows",rows))).put("scope","History referenced in planning only; original facts/report retain it"));}
        var model=study.getJSONObject("model");var labels=model.getJSONArray("labels");model.remove("labels");model.put("labelSource068",new JSONObject().put("count",labels.length()).put("sha256",WorldFrames.hash(new JSONObject().put("labels",labels))).put("route","/api/brain/read").put("id",study.getString("jobId")).put("field","calibrationModel068.labels"));
        var columns=new JSONObject();
        columns.put("reliability",new JSONArray(List.of("low","high","count","meanProbability","successRate")));
        for(String phase:List.of("clean","disturbed","unsupported")){
            var e=study.getJSONObject(phase);var groups=e.getJSONObject("subgroups");e.remove("subgroups");e.put("subgroupSource068",new JSONObject().put("names",new JSONArray(new TreeSet<>(groups.keySet()))).put("sha256",WorldFrames.hash(groups)).put("route","/api/brain/read").put("id",study.getString("jobId")).put("field",phase+"Evaluation068.subgroups"));
            var reliability=e.getJSONObject("calibration");for(String name:List.of("rawReliability","calibratedReliability")){var rows=new JSONArray();for(Object raw:reliability.getJSONArray(name)){var row=new JSONArray();for(Object field:columns.getJSONArray("reliability"))row.put(((JSONObject)raw).get((String)field));rows.put(row);}reliability.put(name,new JSONObject().put("columnsRef","reliability").put("rows",rows));}
            for(String name:List.of("methods","pairedCalibratedMinus")){
                var data=e.getJSONObject(name);var names=new ArrayList<>(data.getJSONObject(new TreeSet<>(data.keySet()).first()).keySet());Collections.sort(names);var cols=new JSONArray(names);String ref=null;
                for(String key:columns.keySet())if(columns.getJSONArray(key).similar(cols))ref=key;if(ref==null){ref="c"+columns.length();columns.put(ref,cols);}
                var rows=new JSONArray();for(String method:new TreeSet<>(data.keySet())){var metrics=data.getJSONObject(method);if(!metrics.keySet().equals(new HashSet<>(names)))throw new IllegalStateException("Calibration metric schema differs");var row=new JSONArray().put(method);for(String key:names)row.put(metrics.get(key));rows.put(row);}
                e.put(name,new JSONObject().put("columnsRef",ref).put("rows",rows));
            }
        }
        study.put("tableColumns068",columns).put("planningScope068","Tables retain all aggregate metrics, paired comparisons, probabilities, reliability bins, nulls, costs and gates. Historical labels and subgroup details are explicit source references. Original facts and checked report remain complete. Model sha256 identifies the full original model, not this projection.");
    }
}
