package fraymus.workspace;
import org.json.*;
import java.util.*;
final class UncertainPolicy071 {
    static final List<String> METHODS=List.of("blind","fixed","adaptive","uncertain");
    static JSONObject initial(JSONObject task){
        var original=AdaptivePolicy070.initial(task);var source=original.getJSONObject("decisions").getJSONObject("fixed");var choices=new JSONObject();
        for(String method:METHODS)choices.put(method,WorldFrames.copy(source).put("method",method).put("adaptive",Set.of("adaptive","uncertain").contains(method)).put("later",!method.equals("blind")));
        return original.put("decisions",choices);
    }
    static JSONObject outcome(JSONObject task,JSONObject decision,JSONObject record){
        var copy=record==null?null:WorldFrames.copy(record).put("timing070",record.getJSONObject("timing071"));
        var out=AdaptivePolicy070.outcome(task,decision,copy);int calls=record!=null&&decision.getString("method").equals("uncertain")?1:0;
        return out.put("uncertaintyCalls",calls).put("logicalCost",out.getDouble("logicalCost")+.005*calls).put("utility",out.getDouble("utility")-.005*calls);
    }
    static JSONObject evaluate(JSONArray rows,boolean details){
        if(rows.isEmpty())throw new IllegalStateException("Empty evaluation");var methods=new JSONObject();int initialRefusals=0;for(Object raw:rows)if(((JSONObject)raw).getJSONObject("plan").getBoolean("initialPooledRefusal"))initialRefusals++;
        for(String method:METHODS){int success=0,miss=0,refuse=0,actions=0,observations=0,replans=0,screens=0,lookups=0,events=0,recovered=0,attempted=0;double utility=0,cost=0,error=0,impulse=0,millis=0;
            for(Object raw:rows){var r=(JSONObject)raw;var o=r.getJSONObject("results").getJSONObject(method).getJSONObject("outcome");if(o.getBoolean("executed")){if(o.getBoolean("success"))success++;else miss++;error+=o.getDouble("targetErrorM");if(r.getJSONObject("plan").getBoolean("initialPooledRefusal")){attempted++;if(o.getBoolean("success"))recovered++;}}else refuse++;
                utility+=o.getDouble("utility");cost+=o.getDouble("logicalCost");actions+=o.getInt("actions");observations+=o.getInt("observations");replans+=o.getInt("replans");screens+=o.getInt("screens");lookups+=o.getInt("lookups");events+=o.getInt("externalEvents");impulse+=o.getDouble("absoluteImpulseNs");millis+=o.getDouble("nativeWallMillis");}
            int n=rows.length(),executions=success+miss;var m=new JSONObject().put("tasks",n).put("successes",success).put("misses",miss).put("refusals",refuse).put("meanUtility",utility/n).put("meanLogicalCost",cost/n).put("allTaskSuccessRate",success/(double)n).put("allTaskMissRate",miss/(double)n).put("initiallyRefusedSuccesses",recovered).put("initiallyRefusedAttempts",attempted);
            if(details)m.put("executions",executions).put("executionRate",executions/(double)n).put("conditionalSuccessRate",executions==0?JSONObject.NULL:success/(double)executions).put("meanTargetErrorM",executions==0?JSONObject.NULL:error/executions).put("initialRefusals",initialRefusals).put("recoveryRate",initialRefusals==0?JSONObject.NULL:recovered/(double)initialRefusals).put("actions",actions).put("observations",observations).put("replans",replans).put("screens",screens).put("lookups",lookups).put("externalEvents",events).put("absoluteImpulseNs",impulse).put("nativeWallMillis",millis).put("nativeTicks",executions*180);
            int calls=0,extra=0,lateActions=0;double scheduleMillis=0;
            for(Object raw:rows){var o=((JSONObject)raw).getJSONObject("results").getJSONObject(method).getJSONObject("outcome");calls+=o.getInt("scheduleCalls");extra+=o.getInt("extraObservations");lateActions+=o.getInt("extraActions");scheduleMillis+=o.getDouble("schedulerWallMillis");}
            m.put("uncertaintyCalls",method.equals("uncertain")?executions:0).put("scheduleCalls",calls).put("extraObservations",extra).put("extraActions",lateActions).put("schedulerWallMillis",scheduleMillis);
            methods.put(method,m);
        }
        var pairs=new JSONObject();for(String other:List.of("blind","fixed","adaptive")){int wins=0,ties=0,losses=0;double sum=0;for(Object raw:rows){var r=((JSONObject)raw).getJSONObject("results");double delta=r.getJSONObject("uncertain").getJSONObject("outcome").getDouble("utility")-r.getJSONObject(other).getJSONObject("outcome").getDouble("utility");sum+=delta;if(delta>1e-12)wins++;else if(delta< -1e-12)losses++;else ties++;}pairs.put(other,new JSONObject().put("meanDelta",sum/rows.length()).put("wins",wins).put("ties",ties).put("losses",losses));}
        return new JSONObject().put("methods",methods).put("pairedUncertainMinus",pairs);
    }
}
