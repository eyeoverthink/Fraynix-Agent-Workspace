package fraymus.workspace;

import org.json.*;
import java.time.Instant;
import java.util.*;

/** A bounded empirical freshness guard. It grants no execution or proof authority. */
final class TaskCost090 {
    static final String VERSION="verified-cost-freshness-090-v1";
    static final int LIMIT=3;

    // Conservative locality, in normalized metres; this is coverage, not a motion forecast.
    static boolean nearby(JSONObject older,JSONObject current) {
        try {
            if(Procedure078.distance(older,current)>.10)return false;
            var a=Procedure078.scene(older);var b=Procedure078.scene(current);
            if(a.getJSONArray("objects").length()!=b.getJSONArray("objects").length()
                ||Math.abs(a.getDouble("gravityMps2")-b.getDouble("gravityMps2"))>.01)return false;
            for(Object raw:a.getJSONArray("objects")) {
                var x=(JSONObject)raw;var y=WorldPlan.sourceObject(b,x.getString("sourceId"));
                // SpatialScene already requires boxes; normalized bodies retain bodyType, not shape.
                if(!x.getString("bodyType").equals(y.getString("bodyType"))
                    ||WorldVerify.distance(x.getJSONArray("position"),y.getJSONArray("position"))>.25
                    ||WorldPlan.angle(x.getJSONArray("rotation"),y.getJSONArray("rotation"))>5
                    ||WorldVerify.distance(x.getJSONArray("size"),y.getJSONArray("size"))>.05
                    ||Math.abs(x.getDouble("friction")-y.getDouble("friction"))>.05
                    ||Math.abs(x.getDouble("restitution")-y.getDouble("restitution"))>.05
                    ||Math.abs(x.getDouble("massKg")-y.getDouble("massKg"))>Math.max(.01,.25*x.getDouble("massKg")))return false;
            }
            if(older.getJSONArray("targets").length()!=current.getJSONArray("targets").length())return false;
            for(Object raw:older.getJSONArray("targets")) {
                var x=(JSONObject)raw;var y=MemoryPolicy075.target(current.getJSONArray("targets"),x.getString("sourceId"));
                if(WorldVerify.distance(x.getJSONArray("positionM"),y.getJSONArray("positionM"))>.25
                    ||Math.abs(Procedure078.yawDelta(x.getDouble("yawDeg"),y.getDouble("yawDeg")))>5)return false;
            }
            for(String key:List.of("forceLimitN","torqueLimitNm"))
                if(Math.abs(older.getDouble(key)-current.getDouble(key))>.25*older.getDouble(key))return false;
            return true;
        }catch(IllegalArgumentException|JSONException unmatched){return false;}
    }

    static boolean eligible(JSONObject cost,JSONObject candidate,JSONObject spec,JSONObject workload,JSONObject model) {
        if(!cost.optBoolean("eligible")||!cost.optBoolean("completed")||!cost.optBoolean("usedMemory")
            ||!cost.optString("code").equals(model.getString("code"))
            ||!cost.getJSONObject("profile").similar(model.getJSONObject("profile"))
            ||!cost.getJSONObject("workload").similar(workload)
            ||!cost.getString("receiptId").equals(candidate.getString("id"))
            ||!cost.getString("preferred").equals(candidate.getString("family"))
            ||cost.getBoolean("warm")!=candidate.getBoolean("warm")
            ||!nearby(cost.getJSONObject("spec"),spec)
            ||!Instant.parse(cost.getString("recordedAt")).isAfter(Instant.parse(model.getString("createdAt"))))return false;
        for(Object raw:model.getJSONArray("pairs")) {
            var p=(JSONObject)raw;
            if(cost.getString("id").equals(p.getString("simpleId"))||cost.getString("id").equals(p.getString("memoryId")))return false;
        }
        return true;
    }

    static JSONObject observation(JSONObject cost,JSONObject job,JSONObject candidate,JSONObject model) {
        // The same owned monotonic arithmetic as calibration; self-overlap does not matter here.
        TaskCost089.pairing(cost,cost);
        if(!cost.getString("jobSha256").equals(WorldFrames.hash(job)))throw new IllegalStateException("Recent cost job changed");
        var e=TaskCost089.estimate(candidate,cost.getJSONObject("spec"),cost.getJSONObject("workload"),model,job.getJSONArray("bankSnapshot").length(),false);
        var out=new JSONObject().put("id",cost.getString("id")).put("available",e.optBoolean("available"))
            .put("recordedAt",cost.getString("recordedAt")).put("costSha256",WorldFrames.hash(cost));
        if(!e.optBoolean("available"))return out.put("reason",e.getString("reason"));
        double predicted=e.getDouble("memoryMillis"),actual=cost.getDouble("totalMillis");
        double tolerance=Math.max(1000,Math.max(.20*predicted,e.getDouble("marginMillis"))),residual=actual-predicted;
        return out.put("predictedMillis",predicted).put("actualMillis",actual).put("toleranceMillis",tolerance)
            .put("residualMillis",residual).put("direction",residual>tolerance?"slower":residual< -tolerance?"faster":"within");
    }

    static JSONObject guard(JSONObject spec,JSONObject decision,JSONArray observations) {
        var out=new JSONObject().put("version",VERSION).put("status","unavailable").put("blocked",true)
            .put("observations",observations).put("scope","Empirical memory-duration drift; no counterfactual saving or probability claim");
        var e=decision.optJSONObject("cost080",new JSONObject());
        if(!e.optBoolean("available"))return out.put("reason","No matching complete-task calibration");
        for(Object raw:e.getJSONArray("neighbors"))if(!nearby(((JSONObject)raw).getJSONObject("pair").getJSONObject("spec"),spec))
            return out.put("status","outside-coverage").put("reason","Scene geometry or conditions changed beyond measured calibration coverage");
        if(observations.length()>LIMIT)throw new IllegalArgumentException("Freshness evidence exceeds three outcomes");
        var seen=new HashSet<String>();int slower=0,faster=0;
        for(Object raw:observations) {
            var o=(JSONObject)raw;if(!seen.add(o.getString("id")))throw new IllegalArgumentException("Duplicate freshness outcome");
            if(!o.optBoolean("available"))continue;
            if(o.getString("direction").equals("slower"))slower++;
            if(o.getString("direction").equals("faster"))faster++;
        }
        boolean stale=slower>=2||faster>=2;
        return out.put("status",stale?"suspect":observations.isEmpty()?"unobserved":"consistent-so-far").put("blocked",stale)
            .put("slower",slower).put("faster",faster).put("reason",stale?"Two recent verified memory costs disagree in the same direction; use simple control until fresh calibration":"No repeated verified cost disagreement; this is not a freshness guarantee");
    }

    static JSONObject apply(JSONObject spec,JSONObject decision,JSONArray observations) {
        if(!decision.getString("mode").equals("calibrated"))return decision;
        var g=guard(spec,decision,observations);decision.put("freshness090",g);
        if(g.getBoolean("blocked"))decision.put("useMemory",false).put("preferred","direct").put("reason",g.getString("reason")+"; simple control");
        return decision;
    }
}
