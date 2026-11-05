package fraymus.workspace;
import org.json.*;
import java.util.*;
final class LaterPolicy069 {
    static final List<String> METHODS=List.of("single","calibrated","later","laterCalibrated");
    static JSONObject initial(JSONObject task){return initial(task,null);}
    static JSONObject initial(JSONObject task,JSONObject timing){
        var original=CalibratedPolicy068.initial(task,FrozenCalibration069.model(),timing);var choices=new JSONObject();
        for(String method:METHODS){boolean screen=method.equals("calibrated")||method.equals("laterCalibrated");
            choices.put(method,WorldFrames.copy(original.getJSONObject("decisions").getJSONObject(screen?"calibrated":"temporal")).put("method",method).put("later",method.startsWith("later")));}
        return original.put("decisions",choices);
    }
    static JSONObject outcome(JSONObject task,JSONObject decision,JSONObject record){
        var old=WorldFrames.copy(decision).put("method",decision.has("calibration068")?"calibrated":"temporal");
        var r=record==null?null:WorldFrames.copy(record).put("timing068",record.getJSONObject("timing069"));
        var out=CalibratedPolicy068.outcome(task,old,r);boolean later=record!=null&&decision.getBoolean("later");
        if(later){var plan=record.getJSONObject("laterPlan");int action=plan.getBoolean("correct")?1:0;double fee=.10+.02*action;
            out.put("actions",out.getInt("actions")+action).put("observations",out.getInt("observations")+3).put("replans",out.getInt("replans")+1)
                .put("logicalCost",out.getDouble("logicalCost")+fee).put("utility",out.getDouble("utility")-fee).put("absoluteImpulseNs",out.getDouble("absoluteImpulseNs")+Math.abs(plan.getDouble("deltaVelocityMps")));}
        return out;
    }
    static JSONObject evaluate(JSONArray rows,boolean details){
        if(rows.isEmpty())throw new IllegalStateException("Empty evaluation");var methods=new JSONObject();int initialRefusals=0;for(Object raw:rows)if(((JSONObject)raw).getJSONObject("plan").getBoolean("initialPooledRefusal"))initialRefusals++;
        for(String method:METHODS){int success=0,miss=0,refuse=0,actions=0,observations=0,replans=0,screens=0,lookups=0,events=0,recovered=0,attempted=0;double utility=0,cost=0,error=0,impulse=0,millis=0;
            for(Object raw:rows){var r=(JSONObject)raw;var o=r.getJSONObject("results").getJSONObject(method).getJSONObject("outcome");if(o.getBoolean("executed")){if(o.getBoolean("success"))success++;else miss++;error+=o.getDouble("targetErrorM");if(r.getJSONObject("plan").getBoolean("initialPooledRefusal")){attempted++;if(o.getBoolean("success"))recovered++;}}else refuse++;
                utility+=o.getDouble("utility");cost+=o.getDouble("logicalCost");actions+=o.getInt("actions");observations+=o.getInt("observations");replans+=o.getInt("replans");screens+=o.getInt("screens");lookups+=o.getInt("lookups");events+=o.getInt("externalEvents");impulse+=o.getDouble("absoluteImpulseNs");millis+=o.getDouble("nativeWallMillis");}
            int n=rows.length(),executions=success+miss;var m=new JSONObject().put("tasks",n).put("successes",success).put("misses",miss).put("refusals",refuse).put("meanUtility",utility/n).put("meanLogicalCost",cost/n).put("allTaskSuccessRate",success/(double)n).put("allTaskMissRate",miss/(double)n).put("initiallyRefusedSuccesses",recovered).put("initiallyRefusedAttempts",attempted);
            if(details)m.put("executions",executions).put("executionRate",executions/(double)n).put("conditionalSuccessRate",executions==0?JSONObject.NULL:success/(double)executions).put("meanTargetErrorM",executions==0?JSONObject.NULL:error/executions).put("initialRefusals",initialRefusals).put("recoveryRate",initialRefusals==0?JSONObject.NULL:recovered/(double)initialRefusals).put("actions",actions).put("observations",observations).put("replans",replans).put("screens",screens).put("lookups",lookups).put("externalEvents",events).put("absoluteImpulseNs",impulse).put("nativeWallMillis",millis).put("nativeTicks",executions*180);
            methods.put(method,m);
        }
        var pairs=new JSONObject();for(String other:List.of("single","calibrated")){int wins=0,ties=0,losses=0;double sum=0;for(Object raw:rows){var r=((JSONObject)raw).getJSONObject("results");double delta=r.getJSONObject(other.equals("single")?"later":"laterCalibrated").getJSONObject("outcome").getDouble("utility")-r.getJSONObject(other).getJSONObject("outcome").getDouble("utility");sum+=delta;if(delta>1e-12)wins++;else if(delta< -1e-12)losses++;else ties++;}pairs.put(other,new JSONObject().put("meanDelta",sum/rows.length()).put("wins",wins).put("ties",ties).put("losses",losses));}
        return new JSONObject().put("methods",methods).put("pairedLaterMinus",pairs);
    }
}
