package fraymus.workspace;

import org.json.*;
import java.util.*;

/** Handwritten task decisions; all plans precede observations and can be recomputed. */
final class DecisionPolicy065 {
    static final List<String> METHODS=List.of("simple","fixed","pooled");
    static final double[] SPEEDS={.08,.16,.24,.32,.38};
    static JSONObject definition(JSONObject task,double speed){return Stopping063.definition(task.getDouble("angleDeg"),task.getDouble("frictionRatio"),speed);}
    static void validate(JSONObject t){
        SpatialScene.keys(t,"angleDeg","frictionRatio","centerM","halfWidthM");
        for(String k:List.of("angleDeg","frictionRatio","centerM","halfWidthM"))if(!Double.isFinite(t.getDouble(k)))throw new IllegalArgumentException("Non-finite task");
        if(t.getDouble("centerM")<.01||t.getDouble("centerM")>.6||t.getDouble("halfWidthM")<.005||t.getDouble("halfWidthM")>.1)throw new IllegalArgumentException("Target outside task limits");
        // Physical/calibrated bounds are checked below as a recorded refusal,
        // before any analytical prediction or native execution is allowed.
    }
    static JSONObject plan(JSONObject t){
        validate(t);boolean supported=true;try{Stopping063.queryDomain(definition(t,.08));}catch(IllegalArgumentException e){supported=false;}
        double radius=Calibration065.radius(),center=t.getDouble("centerM"),half=t.getDouble("halfWidthM");var choices=new JSONObject();var candidates=new JSONArray();
        if(supported)for(double speed:SPEEDS){double mean=Stopping063.analytic(definition(t,speed),.75);candidates.put(new JSONObject().put("speedMps",speed).put("predictionM",mean));}
        for(String method:METHODS){double margin=method.equals("simple")?0:method.equals("fixed")?.02:radius,best=Double.POSITIVE_INFINITY;JSONObject selected=null;
            for(Object raw:candidates){var c=(JSONObject)raw;double mean=c.getDouble("predictionM"),error=Math.abs(mean-center);boolean admit=method.equals("simple")||(mean-margin>=center-half&&mean+margin<=center+half);if(admit&&error<best){best=error;selected=c;}}
            var decision=new JSONObject().put("method",method).put("execute",selected!=null).put("radiusM",margin).put("reason",!supported?"Outside calibrated domain":selected==null?"No candidate interval fits the target window":"Nearest admitted analytical endpoint");
            if(selected!=null)decision.put("speedMps",selected.getDouble("speedMps")).put("predictionM",selected.getDouble("predictionM")).put("lowerM",selected.getDouble("predictionM")-margin).put("upperM",selected.getDouble("predictionM")+margin);
            choices.put(method,decision);
        }
        return new JSONObject().put("supported",supported).put("task",WorldFrames.copy(t)).put("candidates",candidates).put("decisions",choices).put("model",Calibration065.info()).put("advisory",true);
    }
    static JSONObject outcome(JSONObject task,JSONObject decision,JSONObject record){
        if(!decision.getBoolean("execute")){if(record!=null)throw new IllegalStateException("A refusal cannot contain an execution");return new JSONObject().put("executed",false).put("success",false).put("utility",-.15);}
        if(record==null)throw new IllegalStateException("Missing native execution");
        double endpoint=record.getJSONObject("summary").getDouble("endpointM"),error=Math.abs(endpoint-task.getDouble("centerM"));boolean success=endpoint>=task.getDouble("centerM")-task.getDouble("halfWidthM")&&endpoint<=task.getDouble("centerM")+task.getDouble("halfWidthM");
        var out=new JSONObject().put("executed",true).put("success",success).put("endpointM",endpoint).put("targetErrorM",error).put("utility",(success?1:-2)-.02);
        if(!decision.getString("method").equals("simple"))out.put("intervalCovered",endpoint>=decision.getDouble("lowerM")&&endpoint<=decision.getDouble("upperM"));return out;
    }
    static JSONObject evaluate(JSONArray tasks){
        if(tasks.isEmpty())throw new IllegalStateException("Empty decision evaluation");var methods=new JSONObject();
        for(String method:METHODS){int success=0,miss=0,refused=0,covered=0,opportunities=0;double utility=0,error=0;
            for(Object raw:tasks){var row=(JSONObject)raw;var o=row.getJSONObject("results").getJSONObject(method).getJSONObject("outcome");utility+=o.getDouble("utility");if(o.getBoolean("executed")){if(o.getBoolean("success"))success++;else miss++;error+=o.getDouble("targetErrorM");if(o.optBoolean("intervalCovered"))covered++;}else{refused++;if(row.getJSONObject("results").getJSONObject("simple").getJSONObject("outcome").getBoolean("success"))opportunities++;}}
            int executed=success+miss;methods.put(method,new JSONObject().put("tasks",tasks.length()).put("successes",success).put("misses",miss).put("refusals",refused).put("executions",executed).put("allTaskSuccessRate",success/(double)tasks.length()).put("allTaskMissRate",miss/(double)tasks.length()).put("executionRate",executed/(double)tasks.length()).put("conditionalSuccessRate",executed==0?JSONObject.NULL:success/(double)executed).put("meanTargetErrorM",executed==0?JSONObject.NULL:error/executed).put("selectedIntervalCoverage",executed==0||method.equals("simple")?JSONObject.NULL:covered/(double)executed).put("meanUtility",utility/tasks.length()).put("missedSimpleSuccesses",opportunities));
        }
        var pairs=new JSONObject();for(String other:List.of("simple","fixed")){int wins=0,ties=0,losses=0;double sum=0;for(Object raw:tasks){var r=((JSONObject)raw).getJSONObject("results");double delta=r.getJSONObject("pooled").getJSONObject("outcome").getDouble("utility")-r.getJSONObject(other).getJSONObject("outcome").getDouble("utility");sum+=delta;if(delta>1e-12)wins++;else if(delta< -1e-12)losses++;else ties++;}pairs.put(other,new JSONObject().put("meanDelta",sum/tasks.length()).put("wins",wins).put("ties",ties).put("losses",losses));}
        return new JSONObject().put("methods",methods).put("pairedPooledMinus",pairs);
    }
}
