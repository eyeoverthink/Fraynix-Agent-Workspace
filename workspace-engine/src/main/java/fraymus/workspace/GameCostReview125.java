package fraymus.workspace;

import org.json.*;
import java.util.*;

/** Repeated independent comparisons; no statistical confidence or policy promotion. */
final class GameCostReview125 {
    static JSONObject analyze(JSONObject decision,JSONObject taskCost,JSONArray samples) {
        var groups=new HashMap<String,List<JSONObject>>();var ids=new HashSet<String>();
        for(Object raw:samples) {
            var s=(JSONObject)raw;String strategy=s.getString("strategy");double seconds=s.getDouble("seconds");int pickups=s.getInt("pickups");
            if(!Set.of("direct","full").contains(strategy)||!ids.add(s.getString("id"))||!Double.isFinite(seconds)||seconds<=0||pickups<0||pickups!=s.getDouble("pickups"))throw new IllegalArgumentException("Invalid independent comparison sample");
            s.getBoolean("safe");groups.computeIfAbsent(strategy,k->new ArrayList<>()).add(s);
        }
        var direct=groups.getOrDefault("direct",List.of());var full=groups.getOrDefault("full",List.of());
        var result=new JSONObject().put("directSamples",direct.size()).put("fullSamples",full.size()).put("established",false).put("missedOpportunity","unestablished");
        if(!taskCost.optBoolean("outcomeSafe",true))return result.put("reason","Original budget task failed or was damaged; no successful optional-action claim");
        for(Object raw:decision.getJSONArray("alternatives")){var o=(JSONObject)raw;if(o.getString("strategy").equals("full")&&o.has("requiredSecondsPerPickup"))result.put("predictedSecondsPerPickup",o.getDouble("requiredSecondsPerPickup"));}
        if(direct.size()<3||full.size()<3)return result.put("reason","Need three later independent direct and full trials; no benefit inferred from missing comparisons");
        if(samples.toList().stream().anyMatch(s->!new JSONObject((Map<?,?>)s).getBoolean("safe")))return result.put("reason","Failed or damaged independent outcome retained; no useful optional-action claim");
        var d=stats(direct);var f=stats(full);result.put("direct",d).put("full",f);
        double benefit=f.getDouble("minPickups")-d.getDouble("maxPickups"),benefitUpper=f.getDouble("maxPickups")-d.getDouble("minPickups");
        double extraLow=Math.max(0,f.getDouble("minSeconds")-d.getDouble("maxSeconds")),extraHigh=Math.max(0,f.getDouble("maxSeconds")-d.getDouble("minSeconds"));
        result.put("benefitLower",benefit).put("benefitUpper",benefitUpper).put("pairedExtraSecondsLower",extraLow).put("pairedExtraSecondsUpper",extraHigh);
        if(benefit<=0)return result.put("established",true).put("missedOpportunity","no consistent extra benefit").put("reason","Direct already achieves at least the smallest full-strategy pickup count");
        var terminal=taskCost.optJSONObject("terminal");String selected=decision.getString("selected");
        if(terminal==null||!terminal.getBoolean("comparable")||!Set.of("direct","full").contains(selected))return result.put("reason","Independent benefit exists, but the original decision has no comparable complete clock");
        var chosen=selected.equals("direct")?d:f;
        double observedExcess=Math.max(0,terminal.getDouble("seconds")-chosen.getDouble("minSeconds"));
        double threshold=(extraHigh+observedExcess)/benefit;
        result.put("decisionAndRunExcessSeconds",observedExcess).put("observedSecondsPerPickupUpper",threshold).put("established",true);
        if(result.has("predictedSecondsPerPickup"))result.put("predictionMinusObservedUpperSeconds",result.getDouble("predictedSecondsPerPickup")-threshold);
        boolean fits=threshold<=decision.getDouble("allowance");
        return result.put("missedOpportunity",selected.equals("direct")?(fits?"supported by these later comparisons":"extra cost did not fit the allowance"):"optional strategy was selected").put("reason","Complete first-outcome comparisons plus original decision/run excess. The excess includes variation and scheduling, not an isolated causal estimate of planning overhead");
    }
    private static JSONObject stats(List<JSONObject> list) {
        return new JSONObject().put("minSeconds",list.stream().mapToDouble(s->s.getDouble("seconds")).min().orElseThrow()).put("maxSeconds",list.stream().mapToDouble(s->s.getDouble("seconds")).max().orElseThrow()).put("minPickups",list.stream().mapToInt(s->s.getInt("pickups")).min().orElseThrow()).put("maxPickups",list.stream().mapToInt(s->s.getInt("pickups")).max().orElseThrow());
    }
    private GameCostReview125(){}
}
