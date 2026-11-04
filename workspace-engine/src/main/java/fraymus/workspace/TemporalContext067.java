package fraymus.workspace;
import org.json.*;
import java.util.*;

/** Lossless tables first; explicit source references for subgroup detail only when planning exceeds its budget. */
final class TemporalContext067 {
    static void fitPlanningBudget(JSONObject context,int budget){
        if(context.toString().length()<=budget)return;
        var facts=context.optJSONObject("agent042Facts");if(facts==null)return;
        var state=facts.optJSONObject("temporal067");if(state==null)return;
        var study=state.optJSONObject("lastCompleted");if(study==null)return;
        var evaluation=study.optJSONObject("evaluation");if(evaluation==null||!evaluation.has("subgroups"))return;
        var groups=evaluation.getJSONObject("subgroups");
        evaluation.put("subgroupDetail067",new JSONObject().put("names",new JSONArray(new TreeSet<>(groups.keySet())))
            .put("planningTablesSha256",WorldFrames.hash(groups)).put("route","/api/brain/read").put("id",study.getString("jobId"))
            .put("field","evaluation067.subgroups").put("scope","Subgroup detail referenced, not injected in this planning turn. All aggregate metrics, paired comparisons, gates, costs and diagnostics remain here. Original task facts, Java report and readable study retain every subgroup value. Requirements and verification are unchanged."));
        evaluation.remove("subgroups");
    }
    static JSONObject project(JSONObject original){var copy=WorldFrames.copy(original);var state=copy.optJSONObject("temporal067");if(state==null||!state.has("lastCompleted"))return copy;var study=state.getJSONObject("lastCompleted");if(!study.has("evaluation"))return copy;
        if(copy.has("recentWitnesses")){var rows=copy.getJSONArray("recentWitnesses");copy.remove("recentWitnesses");copy.put("witnessHistory067",new JSONObject().put("count",rows.length()).put("sha256",WorldFrames.hash(new JSONObject().put("rows",rows))).put("meaning","Historical witnesses omitted from planning only; original facts/report retain them"));}
        var columns=new JSONObject();compact(study.getJSONObject("evaluation"),columns);compact(study.getJSONObject("shiftedEvaluation"),columns);study.put("tableColumns067",columns).put("tableMeaning067","Each methods/pairedValueMinus table contains columnsRef and rows [method, values in referenced column order]. Every metric, subgroup, null, cost and comparison is retained. Original facts and checked report keep full objects.");return copy;
    }
    private static void compact(JSONObject evaluation,JSONObject columns){for(String name:List.of("methods","pairedValueMinus")){var data=evaluation.getJSONObject(name);if(data.has("columnsRef"))throw new IllegalStateException("Already compacted recovery table");var names=new ArrayList<>(data.getJSONObject(new TreeSet<>(data.keySet()).first()).keySet());Collections.sort(names);var cols=new JSONArray(names);String ref=null;for(String key:columns.keySet())if(columns.getJSONArray(key).similar(cols))ref=key;if(ref==null){ref="c"+columns.length();columns.put(ref,cols);}var rows=new JSONArray();for(String method:new TreeSet<>(data.keySet())){var metrics=data.getJSONObject(method);if(!metrics.keySet().equals(new HashSet<>(names)))throw new IllegalStateException("Recovery metric schema differs");var row=new JSONArray().put(method);for(String key:names)row.put(metrics.get(key));rows.put(row);}evaluation.put(name,new JSONObject().put("columnsRef",ref).put("rows",rows));}var groups=evaluation.optJSONObject("subgroups");if(groups!=null)for(String key:groups.keySet())compact(groups.getJSONObject(key),columns);}
}
