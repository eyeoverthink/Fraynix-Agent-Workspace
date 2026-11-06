package fraymus.workspace;

import org.json.*;
import java.util.*;

/** Complete measured tasks inform cost only; they never authorize motion. */
final class TaskCost080 {
    static final String VERSION="complete-task-cost-080-v1";
    static JSONObject profile(){
        return new JSONObject().put("os",System.getProperty("os.name")).put("arch",System.getProperty("os.arch"))
            .put("java",System.getProperty("java.runtime.version")).put("cpus",Runtime.getRuntime().availableProcessors())
            .put("maxHeap",Runtime.getRuntime().maxMemory()).put("processor",System.getenv().getOrDefault("PROCESSOR_IDENTIFIER","unknown"));
    }
    static JSONObject workload(JSONObject goal){return new JSONObject().put("retain",goal.optBoolean("retainExperience079",true)).put("skill",!goal.optString("saveSkillName").isBlank());}
    static JSONObject task(JSONObject job){
        var spec=job.getJSONObject("spec");var source=WorldFrames.scene(job.getJSONObject("initialFrame"));var objects=new ArrayList<JSONObject>();
        for(Object raw:source.getJSONArray("objects")){var o=(JSONObject)raw;var value=new JSONObject();
            for(String k:List.of("sourceId","shape","position","rotation","size","massKg","friction","restitution","velocity","angularVelocity"))if(o.has(k))value.put(k,o.get(k));objects.add(value);}
        objects.sort(Comparator.comparing(o->o.getString("sourceId")));
        return new JSONObject().put("objects",new JSONArray(objects)).put("gravity",source.getDouble("gravityMps2"))
            .put("targets",spec.getJSONArray("targets")).put("force",spec.getDouble("forceLimitN")).put("torque",spec.getDouble("torqueLimitNm"));
    }
    static JSONObject pair(JSONObject simple,JSONObject memory){
        for(var x:List.of(simple,memory)){
            if(!x.getBoolean("eligible")||!x.getBoolean("completed")||!Double.isFinite(x.getDouble("totalMillis"))||x.getDouble("totalMillis")<=0)
                throw new IllegalArgumentException("Only uninterrupted, completed measured tasks can calibrate cost");
        }
        if(simple.getString("id").equals(memory.getString("id"))||!simple.getString("mode").equals("simple")||!memory.getString("mode").equals("memory")||!memory.getBoolean("usedMemory"))
            throw new IllegalArgumentException("Distinct simple and actual memory controls are required");
        for(String k:List.of("code","profile","task","workload"))if(!new JSONObject().put("v",simple.get(k)).similar(new JSONObject().put("v",memory.get(k))))
            throw new IllegalArgumentException("Matched task, code, workload and runtime profile required: "+k);
        return new JSONObject().put("simpleId",simple.getString("id")).put("memoryId",memory.getString("id"))
            .put("spec",simple.getJSONObject("spec")).put("taskSha256",WorldFrames.hash(simple.getJSONObject("task")))
            .put("family",memory.getString("preferred")).put("receiptId",memory.getString("receiptId")).put("warm",memory.getBoolean("warm"))
            .put("workload",simple.getJSONObject("workload")).put("simpleMillis",simple.getDouble("totalMillis"))
            .put("memoryMillis",memory.getDouble("totalMillis")).put("savingMillis",simple.getDouble("totalMillis")-memory.getDouble("totalMillis"));
    }
    static JSONObject estimate(JSONObject candidate,JSONObject spec,JSONObject workload,JSONObject model){
        var result=new JSONObject().put("available",false).put("reason","No matched complete-task calibration").put("neighbors",new JSONArray());
        if(model.isEmpty())return result;
        result.put("modelId",model.getString("id")).put("modelSha256",WorldFrames.hash(model));
        if(!model.getJSONObject("profile").similar(profile()))return result.put("reason","Runtime profile differs from calibration");
        if(candidate.getString("family").equals("direct"))return result.put("reason","Direct control needs no remembered ordering");
        var nearby=new ArrayList<JSONObject>();
        for(Object raw:model.getJSONArray("pairs")){
            var p=(JSONObject)raw;
            if(!p.getString("family").equals(candidate.getString("family"))||!p.getString("receiptId").equals(candidate.getString("id"))||p.getBoolean("warm")!=candidate.getBoolean("warm")||!p.getJSONObject("workload").similar(workload))continue;
            try{double d=Procedure078.distance(p.getJSONObject("spec"),spec);if(d<=.10)nearby.add(new JSONObject().put("pair",p).put("distance",d));}catch(IllegalArgumentException incompatible){/* Unmatched roles cannot estimate a task. */}
        }
        nearby.sort(Comparator.comparingDouble(x->x.getDouble("distance")));if(nearby.size()>4)nearby=new ArrayList<>(nearby.subList(0,4));
        var unique=new HashSet<String>();for(var x:nearby)unique.add(x.getJSONObject("pair").getString("taskSha256"));
        result.put("neighbors",new JSONArray(nearby));if(unique.size()<2)return result.put("reason","Need two distinct matched tasks with this recipe, workload and proof state");
        double simple=0,memory=0,maxTime=0,distance=0;
        for(var x:nearby){var p=x.getJSONObject("pair");simple+=p.getDouble("simpleMillis");memory+=p.getDouble("memoryMillis");maxTime=Math.max(maxTime,Math.max(p.getDouble("simpleMillis"),p.getDouble("memoryMillis")));distance=Math.max(distance,x.getDouble("distance"));}
        simple/=nearby.size();memory/=nearby.size();double saving=simple-memory,spread=500;
        for(var x:nearby)spread=Math.max(spread,Math.abs(x.getJSONObject("pair").getDouble("savingMillis")-saving));
        double margin=spread+2*distance*maxTime;
        return result.put("available",true).put("reason","Matched complete tasks, including verification and commit")
            .put("simpleMillis",simple).put("memoryMillis",memory).put("savingMillis",saving).put("marginMillis",margin)
            .put("lowerSavingMillis",saving-margin).put("upperSavingMillis",saving+margin).put("empiricalOnly",true);
    }
    static JSONObject choose(JSONObject spec,JSONArray candidates,JSONObject workload,JSONObject model){
        var decision=Procedure079.choose(spec,"memory",candidates);decision.put("mode","calibrated");
        var candidate=decision.optJSONObject("candidate");var estimate=candidate==null?new JSONObject().put("available",false).put("reason","No compatible verified memory"):estimate(candidate,spec,workload,model);
        boolean use=estimate.optBoolean("available")&&estimate.getDouble("lowerSavingMillis")>0;
        return decision.put("useMemory",use).put("preferred",use?candidate.getString("family"):"direct").put("cost080",estimate)
            .put("reason",use?"Complete-task savings exceed the measured uncertainty margin":estimate.optBoolean("available")?"Complete-task benefit is too uncertain; simple control":estimate.getString("reason")+"; simple control");
    }
}
