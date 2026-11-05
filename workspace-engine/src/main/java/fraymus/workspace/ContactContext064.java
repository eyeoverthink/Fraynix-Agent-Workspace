package fraymus.workspace;
import org.json.*;
import java.util.*;

/** Preserve original records; compact repeated labels only in model planning. */
final class ContactContext064 {
    static JSONObject project(JSONObject original){var copy=WorldFrames.copy(original);var contact=copy.optJSONObject("contact064");var decision=copy.optJSONObject("decision065");if((contact==null||!contact.has("lastCompleted"))&&(decision==null||!decision.has("lastCompleted")))return copy;
        if(copy.has("recentWitnesses")){var rows=copy.getJSONArray("recentWitnesses");copy.remove("recentWitnesses");copy.put("contactWitnessHistory064",new JSONObject().put("count",rows.length()).put("sha256",WorldFrames.hash(new JSONObject().put("rows",rows))).put("meaning","Historical witness rows omitted from planning; complete original facts and native receipts remain in task context and checked report. Requirements and gates unchanged."));}
        if(decision!=null&&decision.has("lastCompleted"))compactDecision(decision.getJSONObject("lastCompleted"));
        return copy;
    }
    private static void compactDecision(JSONObject study){
        var evaluation=study.optJSONObject("evaluation");if(evaluation==null||!evaluation.has("methods"))return;
        var columns=new ArrayList<>(evaluation.getJSONObject("methods").getJSONObject("simple").keySet());Collections.sort(columns);
        table(evaluation,columns);
        var groups=evaluation.optJSONObject("subgroups");if(groups!=null)for(String group:groups.keySet())table(groups.getJSONObject(group),columns);
        if(study.has("shiftedEvaluation"))table(study.getJSONObject("shiftedEvaluation"),columns);
        study.put("methodColumns065",new JSONArray(columns)).put("methodRowsMeaning065","Each methodRows065 row is [method, then every metric in methodColumns065 order]. All subgroup, paired, null and zero values retained exactly. Full objects remain in original facts and verified report.");
    }
    private static void table(JSONObject evaluation,List<String> columns){
        var methods=evaluation.getJSONObject("methods");var rows=new JSONArray();
        for(String method:new TreeSet<>(methods.keySet())){var metrics=methods.getJSONObject(method);if(!metrics.keySet().equals(new HashSet<>(columns)))throw new IllegalStateException("Decision metric schema differs; cannot compact losslessly");var row=new JSONArray().put(method);for(String column:columns)row.put(metrics.get(column));rows.put(row);}
        evaluation.remove("methods");evaluation.put("methodRows065",rows);
    }
}
