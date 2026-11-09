package fraymus.workspace;

import org.json.*;
import java.util.*;

/** Complete-task admission for automatic memory use; never execution authority. */
final class TaskCost098 {
    static final String VERSION="current-task-benefit-098-v1";
    static boolean automatic(String mode) { return Set.of("adaptive","calibrated").contains(mode); }

    static JSONObject timing(String owner,long start,long end) {
        var t=new JSONObject().put("clock089",TaskCost089.clock(owner,start,end))
            .put("totalMillis",Math.subtractExact(end,start)/1e6);
        millis(t);
        return t;
    }

    static double millis(JSONObject timing) {
        // Reuse the canonical owner/integer/monotonic arithmetic checks, not wall clocks.
        timing.getJSONObject("clock089");
        TaskCost089.pairing(timing,timing);
        return timing.getDouble("totalMillis");
    }

    static boolean movementCovered(JSONObject measured,JSONObject current) {
        try {
            var a=Procedure078.scene(measured);var b=Procedure078.scene(current);
            if(measured.getJSONArray("targets").length()!=current.getJSONArray("targets").length())return false;
            for(Object raw:current.getJSONArray("targets")) {
                var t=(JSONObject)raw;String role=t.getString("sourceId");
                var old=MemoryPolicy075.target(measured.getJSONArray("targets"),role);
                var x=WorldPlan.sourceObject(a,role);var y=WorldPlan.sourceObject(b,role);
                double before=WorldVerify.distance(x.getJSONArray("position"),old.getJSONArray("positionM"));
                double now=WorldVerify.distance(y.getJSONArray("position"),t.getJSONArray("positionM"));
                double beforeAngle=Math.abs(Procedure078.yawDelta(WorldPlan.yaw(x.getJSONArray("rotation")),old.getDouble("yawDeg")));
                double nowAngle=Math.abs(Procedure078.yawDelta(WorldPlan.yaw(y.getJSONArray("rotation")),t.getDouble("yawDeg")));
                // Match work per target rather than diluting it among unrelated scene features.
                // Floors are existing final-task tolerances; these are coverage limits, not new success thresholds.
                if(!comparable(before,now,.1)||!comparable(beforeAngle,nowAngle,5))return false;
            }
            return true;
        }catch(IllegalArgumentException|JSONException unsupported){return false;}
    }

    private static boolean comparable(double a,double b,double tolerance) {
        return Double.isFinite(a)&&Double.isFinite(b)&&Math.max(a,b)<=2*Math.max(tolerance,Math.min(a,b));
    }

    static JSONObject choose(JSONObject spec,String mode,JSONArray candidates,JSONObject workload,
                             JSONObject model,String receipt,int bankEntries,String selector,
                             JSONObject timing,JSONArray observations) {
        TaskCost085.validateSelection(mode,receipt);
        if(!automatic(mode))return TaskCost089.choose(spec,mode,candidates,workload,model,receipt,bankEntries,selector);
        double spent=millis(timing),reserve=Math.max(500,spent*.25);
        var d=TaskCost089.choose(spec,"calibrated",candidates,workload,model,receipt,bankEntries,selector);
        d=TaskCost090.apply(spec,d,observations);
        var e=d.getJSONObject("cost080");boolean covered=false;
        if(e.optBoolean("available")) {
            covered=true;
            for(Object raw:e.getJSONArray("neighbors"))
                covered&=movementCovered(((JSONObject)raw).getJSONObject("pair").getJSONObject("spec"),spec);
        }
        var gate=new JSONObject().put("version",VERSION).put("decisionMillis",spent).put("reserveMillis",reserve)
            .put("movementCovered",covered).put("scope","Complete-task empirical lower saving less measured decision work and max(500ms,25%) reserve; not a probability guarantee");
        if(e.optBoolean("available"))gate.put("netLowerSavingMillis",e.getDouble("lowerSavingMillis")-spent-reserve);
        if(d.getBoolean("useMemory")) {
            if(!covered)d.put("useMemory",false).put("preferred","direct").put("reason","Remaining movement differs from measured tasks; simple control first");
            else if(gate.getDouble("netLowerSavingMillis")<=0)d.put("useMemory",false).put("preferred","direct").put("reason","Expected benefit does not cover decision work and uncertainty; simple control first");
            else d.put("reason","Verified complete-task benefit covers decision work and uncertainty; use remembered procedure");
        }
        return d.put("mode",mode).put("benefit098",gate);
    }
}
