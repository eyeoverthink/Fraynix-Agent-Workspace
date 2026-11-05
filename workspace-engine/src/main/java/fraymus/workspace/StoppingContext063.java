package fraymus.workspace;

import org.json.*;
import java.util.List;

/** Planning projection only. The original context, native receipts and report data remain intact. */
final class StoppingContext063 {
    static JSONObject project(JSONObject original){
        var facts=WorldFrames.copy(original);var stopping=facts.optJSONObject("stopping063");
        if(stopping==null||!stopping.has("lastCompleted"))return facts;
        facts.put("retainedSource063",new JSONObject().put("sha256",WorldFrames.hash(original)).put("scope","Planning projection of historical evidence only. Complete agent facts remain in this task's context044/nativeReplies044 and checked report. Brain study records remain readable by jobId; requirements and verification gates are unchanged."));
        if(facts.has("recentWitnesses")){
            var history=facts.getJSONArray("recentWitnesses");facts.remove("recentWitnesses");facts.put("witnessHistory063",new JSONObject().put("count",history.length()).put("sha256",WorldFrames.hash(new JSONObject().put("rows",history))).put("omittedFromPlanning",true));
        }
        compact(stopping);compact(stopping.getJSONObject("lastCompleted"));return facts;
    }
    private static void compact(JSONObject study){
        var traces=study.optJSONArray("traces");if(traces==null)return;var rows=new JSONArray();
        for(Object raw:traces){var t=(JSONObject)raw;var s=t.getJSONObject("summary");var c=t.getJSONObject("definition");var row=new JSONArray().put(c.getDouble("angleDeg")).put(c.getDouble("pushMps")).put(t.getString("variant"));
            for(String k:List.of("endpointErrorM","finalErrorM","maximumRotationDeg","firstTouchTick","firstQuietWindowStartTick","finalQuiet100ms"))row.put(s.get(k));rows.put(row);
        }
        study.remove("traces");study.put("traceComparison063",new JSONObject().put("count",traces.length()).put("originalSha256",WorldFrames.hash(new JSONObject().put("traces",traces)))
            .put("columns",new JSONArray(List.of("angleDeg","pushMps","variant","endpointErrorM","finalErrorM","maximumRotationDeg","firstTouchTick","firstQuietWindowStartTick","finalQuiet100ms"))).put("rows",rows)
            .put("detailSource",new JSONObject().put("route","/api/brain/read").put("id",study.getString("jobId"))));
    }
}
