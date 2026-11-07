package fraymus.workspace;

import org.json.*;
import java.util.*;

/** Repeated observations estimate a future complete task, never motion authority. */
final class TaskCost084 {
    static final String VERSION="repeated-complete-task-cost-084-v1";
    static final int REPEATS=3, TASKS=2;

    static JSONObject estimate(JSONObject candidate,JSONObject spec,JSONObject workload,JSONObject model) {
        var out=new JSONObject().put("available",false).put("method",VERSION)
            .put("reason","No repeated complete-task calibration").put("neighbors",new JSONArray())
            .put("minimumRepeats",REPEATS).put("minimumDistinctTasks",TASKS).put("empiricalOnly",true);
        if(model.isEmpty())return out;
        out.put("modelId",model.getString("id")).put("modelSha256",WorldFrames.hash(model));
        if(!VERSION.equals(model.optString("version")))return out.put("reason","Calibration method differs; collect fresh repeated comparisons");
        if(!model.getJSONObject("profile").similar(TaskCost080.profile()))return out.put("reason","Runtime profile differs from calibration");
        if(candidate.getString("family").equals("direct"))return out.put("reason","Direct control needs no remembered ordering");
        var grouped=new LinkedHashMap<String,List<JSONObject>>();var seen=new HashSet<String>();
        for(Object raw:model.getJSONArray("pairs")) {
            var p=(JSONObject)raw;
            // Also defend this pure estimator when called outside the checked model reader.
            if(!seen.add(p.getString("simpleId"))||!seen.add(p.getString("memoryId")))return out.put("reason","Repeated goal provenance is not independent evidence");
            if(!p.getString("family").equals(candidate.getString("family"))||!p.getString("receiptId").equals(candidate.getString("id"))
                ||p.getBoolean("warm")!=candidate.getBoolean("warm")||!p.getJSONObject("workload").similar(workload))continue;
            double simple=p.getDouble("simpleMillis"),memory=p.getDouble("memoryMillis");
            if(!Double.isFinite(simple)||!Double.isFinite(memory)||simple<=0||memory<=0)return out.put("reason","Invalid complete-task timing");
            try {
                double distance=Procedure078.distance(p.getJSONObject("spec"),spec);
                if(distance<=.10)grouped.computeIfAbsent(p.getString("taskSha256"),k->new ArrayList<>())
                    .add(new JSONObject().put("pair",p).put("distance",distance));
            }catch(IllegalArgumentException unmatched){ /* Different object roles cannot predict this task. */ }
        }
        var groups=new ArrayList<>(grouped.values());
        groups.sort(Comparator.comparingDouble(g->g.getFirst().getDouble("distance")));
        if(groups.size()>4)groups=new ArrayList<>(groups.subList(0,4));
        var neighbors=new JSONArray();var counts=new JSONArray();
        for(var g:groups){for(var x:g)neighbors.put(x);counts.put(g.size());}
        out.put("neighbors",neighbors).put("repeatCounts",counts).put("distinctTasks",groups.size());
        if(groups.size()<TASKS||groups.stream().anyMatch(g->g.size()<REPEATS))
            return out.put("reason","Need two nearby distinct tasks with three independent comparisons each for this receipt, workload and proof state");
        double simple=0,memory=0,within=500,maxTime=0,distance=0;var savings=new ArrayList<Double>();
        for(var g:groups) {
            double s=0,m=0,smin=Double.POSITIVE_INFINITY,smax=0,mmin=Double.POSITIVE_INFINITY,mmax=0;
            for(var x:g) {
                var p=x.getJSONObject("pair");double a=p.getDouble("simpleMillis"),b=p.getDouble("memoryMillis");
                s+=a;m+=b;smin=Math.min(smin,a);smax=Math.max(smax,a);mmin=Math.min(mmin,b);mmax=Math.max(mmax,b);
                maxTime=Math.max(maxTime,Math.max(a,b));distance=Math.max(distance,x.getDouble("distance"));
            }
            s/=g.size();m/=g.size();simple+=s;memory+=m;savings.add(s-m);within=Math.max(within,smax-smin+mmax-mmin);
        }
        simple/=groups.size();memory/=groups.size();double saving=simple-memory,between=0;
        for(double s:savings)between=Math.max(between,Math.abs(s-saving));
        double extrapolation=2*distance*maxTime,margin=within+between+extrapolation;
        return out.put("available",true).put("reason","Repeated complete tasks; empirical next-task range, not a probability guarantee")
            .put("simpleMillis",simple).put("memoryMillis",memory).put("savingMillis",saving)
            .put("withinTaskMillis",within).put("betweenTaskMillis",between).put("distanceMillis",extrapolation)
            .put("marginMillis",margin).put("lowerSavingMillis",saving-margin).put("upperSavingMillis",saving+margin);
    }

    static JSONObject choose(JSONObject spec,JSONArray candidates,JSONObject workload,JSONObject model) {
        var decision=Procedure079.choose(spec,"memory",candidates);decision.put("mode","calibrated");
        var candidate=decision.optJSONObject("candidate");
        var estimate=candidate==null?new JSONObject().put("available",false).put("reason","No compatible verified memory"):estimate(candidate,spec,workload,model);
        boolean use=estimate.optBoolean("available")&&estimate.getDouble("lowerSavingMillis")>0;
        return decision.put("useMemory",use).put("preferred",use?candidate.getString("family"):"direct").put("cost080",estimate)
            .put("reason",use?"Complete-task savings exceed repeat variability and scene uncertainty":estimate.optBoolean("available")?"Repeated task benefit is too uncertain; simple control":estimate.getString("reason")+"; simple control");
    }
}
