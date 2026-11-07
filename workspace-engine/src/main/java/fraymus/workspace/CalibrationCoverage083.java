package fraymus.workspace;

import java.util.*;
import org.json.*;

/** Describes an already verified calibration; never supplies movement authority. */
final class CalibrationCoverage083 {
    static JSONObject describe(JSONObject model) {
        var result=new JSONObject().put("groups",new JSONArray()).put("minimumDistinctTasks",2)
            .put("scope","Coverage counts only. Execution still requires nearby tasks, the same receipt, proof state, saving workload and runtime, and the original outcome checks.");
        if(model.isEmpty())return result.put("profileMatches",false).put("reason","No saved calibration");
        boolean matches=model.getJSONObject("profile").similar(TaskCost080.profile());
        result.put("modelId",model.getString("id")).put("profileMatches",matches);
        var repeats=new HashMap<String,Map<String,Integer>>();
        var groups=new TreeMap<String,JSONObject>();var tasks=new HashMap<String,Set<String>>();
        for(Object raw:model.getJSONArray("pairs")) {
            var p=(JSONObject)raw;var w=p.getJSONObject("workload");
            var identity=new JSONObject().put("receiptId",p.getString("receiptId")).put("family",p.getString("family"))
                .put("warm",p.getBoolean("warm")).put("retain",w.getBoolean("retain")).put("skill",w.getBoolean("skill"));
            String key=WorldFrames.hash(identity);
            var group=groups.computeIfAbsent(key,k->identity.put("pairCount",0));
            group.put("pairCount",group.getInt("pairCount")+1);
            tasks.computeIfAbsent(key,k->new HashSet<>()).add(p.getString("taskSha256"));
            repeats.computeIfAbsent(key,k->new TreeMap<>()).merge(p.getString("taskSha256"),1,Integer::sum);
        }
        for(var entry:groups.entrySet()) {
            int count=tasks.get(entry.getKey()).size();var group=entry.getValue();
            group.put("distinctTasks",count).put("minimumCountMet",count>=2)
                .put("state",group.getBoolean("warm")?"Already checked this session":"After restart")
                .put("work",group.getBoolean("skill")?(group.getBoolean("retain")?"Save experience and skill":"Save skill"):(group.getBoolean("retain")?"Save experience":"Run without saving"))
                .put("reason",!matches?"Runtime differs from calibration":count<2?"Needs two distinct matched tasks":"Minimum count met; task distance and current receipt still checked when running");
            if(TaskCost084.VERSION.equals(model.optString("version"))||TaskCost085.VERSION.equals(model.optString("version"))||TaskCost089.VERSION.equals(model.optString("version"))) {
                var counts=repeats.get(entry.getKey());int repeated=(int)counts.values().stream().filter(n->n>=TaskCost084.REPEATS).count();
                group.put("repeatCounts",new JSONArray(counts.values())).put("repeatedTasks",repeated).put("minimumRepeats",TaskCost084.REPEATS)
                    .put("repeatCountMet",repeated>=TaskCost084.TASKS).put("reason",!matches?"Runtime differs from calibration":repeated<TaskCost084.TASKS?"Needs two nearby tasks with three independent comparisons each":"Repeat counts met; current receipt, task distance and original evidence still checked");
            }
            result.getJSONArray("groups").put(group);
        }
        if(model.has("setup085")){TaskCost085.verifySetup(model);result.put("setup085",WorldFrames.copy(model.getJSONObject("setup085")));}
        return result;
    }
}
