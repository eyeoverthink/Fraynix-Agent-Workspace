package fraymus.workspace;
import org.json.*;
import java.util.*;

/** Planner view only: original task facts and checked reports retain the full calibration evidence. */
final class CalibrationContext068 {
    static void project(JSONObject context){
        var facts=context.optJSONObject("agent042Facts");if(facts==null)return;var state=facts.optJSONObject("calibrated068");if(state==null||!state.has("lastCompleted"))return;
        var study=state.getJSONObject("lastCompleted");if(!study.has("clean"))return;
        var related=new JSONObject();for(String key:List.of("clock","witness","limits","observations","appraisal","neural","fraynixGPT061","residual062","stopping063","contact064","decision065","recovery066","temporal067"))if(facts.has(key))related.put(key,facts.remove(key));
        if(!related.isEmpty())facts.put("relatedEvidence068",new JSONObject().put("fields",new JSONArray(new TreeSet<>(related.keySet()))).put("sha256",WorldFrames.hash(related)).put("route","/api/agent042/state").put("scope","Operational clocks, observations and other studies referenced for calibration planning; full original task facts/report retain them"));
        if(facts.has("recentWitnesses")){var rows=facts.getJSONArray("recentWitnesses");facts.remove("recentWitnesses");facts.put("witnessHistory068",new JSONObject().put("count",rows.length()).put("sha256",WorldFrames.hash(new JSONObject().put("rows",rows))).put("scope","History referenced in planning only; original facts/report retain it"));}
        var model=study.getJSONObject("model");var labels=model.getJSONArray("labels");model.remove("labels");model.put("labelSource068",new JSONObject().put("count",labels.length()).put("sha256",WorldFrames.hash(new JSONObject().put("labels",labels))).put("route","/api/brain/read").put("id",study.getString("jobId")).put("field","calibrationModel068.labels"));
        for(String name:List.of("knots","blocks"))model.put(name,plainTable(model.getJSONArray(name)));
        var columns=new JSONObject();
        columns.put("reliability",new JSONArray(List.of("low","high","count","meanProbability","successRate")));
        columns.put("diagnostics",new JSONArray(new TreeSet<>(study.getJSONObject("clean").getJSONObject("diagnostics").keySet())));
        for(String phase:List.of("clean","disturbed","unsupported")){
            var e=study.getJSONObject(phase);var groups=e.getJSONObject("subgroups");e.remove("subgroups");e.put("subgroupSource068",new JSONObject().put("names",new JSONArray(new TreeSet<>(groups.keySet()))).put("sha256",WorldFrames.hash(groups)).put("route","/api/brain/read").put("id",study.getString("jobId")).put("field",phase+"Evaluation068.subgroups"));
            var diagnostic=e.getJSONObject("diagnostics");var values=new JSONArray();for(Object key:columns.getJSONArray("diagnostics"))values.put(diagnostic.get((String)key));e.put("diagnostics",new JSONObject().put("columnsRef","diagnostics").put("values",values));
            var reliability=e.getJSONObject("calibration");if(!study.has("calibrationLabelMeaning068"))study.put("calibrationLabelMeaning068",reliability.getString("meaning"));reliability.remove("meaning");reliability.put("meaningRef","calibrationLabelMeaning068");
            if(!study.has("breakEvenScope068"))study.put("breakEvenScope068",e.getString("breakEvenScope"));e.remove("breakEvenScope");e.put("breakEvenScopeRef","breakEvenScope068");
            for(String name:List.of("rawReliability","calibratedReliability")){var rows=new JSONArray();for(Object raw:reliability.getJSONArray(name)){var row=new JSONArray();for(Object field:columns.getJSONArray("reliability"))row.put(((JSONObject)raw).get((String)field));rows.put(row);}reliability.put(name,new JSONObject().put("columnsRef","reliability").put("rows",rows));}
            for(String name:List.of("methods","pairedCalibratedMinus")){
                var data=e.getJSONObject(name);var names=new ArrayList<>(data.getJSONObject(new TreeSet<>(data.keySet()).first()).keySet());Collections.sort(names);var shared=new JSONObject();
                for(int i=0;i<names.size();i++){String key=names.get(i);Object first=data.getJSONObject(new TreeSet<>(data.keySet()).first()).get(key);boolean same=true;for(String method:data.keySet())if(!Procedure044.equal(first,data.getJSONObject(method).get(key)))same=false;if(same)shared.put(""+i,first);}
                var cols=new JSONArray(names);String ref=null;
                for(String key:columns.keySet())if(columns.getJSONArray(key).similar(cols))ref=key;if(ref==null){ref="c"+columns.length();columns.put(ref,cols);}
                var all=new HashSet<>(names);var rows=new JSONArray();for(String method:new TreeSet<>(data.keySet())){var metrics=data.getJSONObject(method);if(!metrics.keySet().equals(all))throw new IllegalStateException("Calibration metric schema differs");var row=new JSONArray().put(method);for(int i=0;i<names.size();i++)if(!shared.has(""+i))row.put(metrics.get(names.get(i)));rows.put(row);}
                e.put(name,new JSONObject().put("columnsRef",ref).put("sharedIndexes",shared).put("rows",rows));
            }
        }
        study.put("tableColumns068",columns).put("planningScope068","Tables use columnsRef; rows=[method,nonshared values in column order]. sharedIndexes maps zero-based column indexes to values common to every method. All calibration metrics, comparisons, reliability, nulls, costs and gates retained. Other facts/labels/subgroups referenced. Model hash identifies full original.");
    }
    private static JSONObject plainTable(JSONArray original){var keys=new JSONArray(new TreeSet<>(original.getJSONObject(0).keySet()));var rows=new JSONArray();for(Object raw:original){var row=new JSONArray();for(Object key:keys)row.put(((JSONObject)raw).get((String)key));rows.put(row);}return new JSONObject().put("columns",keys).put("rows",rows);}
}
