package fraymus.workspace;

import org.json.*;
import java.util.*;

/** Frozen one-step replanning. Predictions and measured observations have distinct inputs. */
final class TemporalPolicy067 {
    static final List<String> METHODS=List.of("simple","fixed","pooled","blind","observed","temporal","value");
    static JSONObject initial(JSONObject task){return initial(task,null);}
    static JSONObject initial(JSONObject task,double[] screenMillis){
        var plan=RecoveryPolicy066.initial(task);var choices=plan.getJSONObject("decisions");
        for(String method:List.of("temporal","value")){
            var d=WorldFrames.copy(choices.getJSONObject("observed")).put("method",method);
            if(method.equals("value")&&d.getBoolean("recover")){
                long at=System.nanoTime();var screen=ValueScreen067.screen(task,d.getDouble("speedMps"));
                if(screenMillis!=null)screenMillis[0]=(System.nanoTime()-at)/1e6;
                d.put("screen",screen);
                if(!screen.getBoolean("attempt"))d.put("execute",false).put("recover",false).put("reason","Recovery value screen declined before the first push");
            }
            choices.put(method,d);
        }
        return plan;
    }
    static JSONObject outcome(JSONObject task,JSONObject decision,JSONObject record){
        var compatibility=WorldFrames.copy(decision);String method=decision.getString("method");
        boolean temporal=method.equals("temporal")||method.equals("value");
        if(temporal)compatibility.put("method","observed");
        // Old scoring is reused exactly, then explicit extra observation and screening fees are added.
        JSONObject compatibleRecord=record==null?null:WorldFrames.copy(record).put("timing066",record.getJSONObject("timing067"));
        var out=RecoveryPolicy066.outcome(task,compatibility,compatibleRecord);
        int extra=temporal&&out.getInt("replans")>0?2:0,screens=decision.has("screen")?1:0;
        double added=.03*extra+.005*screens;
        out.put("observations",out.getInt("observations")+extra).put("screens",screens)
            .put("logicalCost",out.getDouble("logicalCost")+added).put("utility",out.getDouble("utility")-added);
        return out;
    }
    static JSONObject evaluate(JSONArray rows,boolean details){
        if(rows.isEmpty())throw new IllegalStateException("Empty evaluation");var methods=new JSONObject();int initialRefusals=0;for(Object raw:rows)if(((JSONObject)raw).getJSONObject("plan").getBoolean("initialPooledRefusal"))initialRefusals++;
        for(String method:METHODS){int success=0,miss=0,refuse=0,actions=0,observations=0,replans=0,screens=0,recovered=0,attempted=0;double utility=0,cost=0,error=0,impulse=0,millis=0;
            for(Object raw:rows){var r=(JSONObject)raw;var o=r.getJSONObject("results").getJSONObject(method).getJSONObject("outcome");if(o.getBoolean("executed")){if(o.getBoolean("success"))success++;else miss++;error+=o.getDouble("targetErrorM");if(r.getJSONObject("plan").getBoolean("initialPooledRefusal")){attempted++;if(o.getBoolean("success"))recovered++;}}else refuse++;
                utility+=o.getDouble("utility");cost+=o.getDouble("logicalCost");actions+=o.getInt("actions");observations+=o.getInt("observations");replans+=o.getInt("replans");screens+=o.getInt("screens");impulse+=o.getDouble("absoluteImpulseNs");millis+=o.getDouble("nativeWallMillis");}
            int n=rows.length(),executions=success+miss;var m=new JSONObject().put("tasks",n).put("successes",success).put("misses",miss).put("refusals",refuse).put("meanUtility",utility/n).put("meanLogicalCost",cost/n).put("allTaskSuccessRate",success/(double)n).put("allTaskMissRate",miss/(double)n).put("initiallyRefusedSuccesses",recovered).put("initiallyRefusedAttempts",attempted);
            if(details)m.put("executions",executions).put("executionRate",executions/(double)n).put("conditionalSuccessRate",executions==0?JSONObject.NULL:success/(double)executions).put("meanTargetErrorM",executions==0?JSONObject.NULL:error/executions).put("initialRefusals",initialRefusals).put("recoveryRate",initialRefusals==0?JSONObject.NULL:recovered/(double)initialRefusals).put("actions",actions).put("observations",observations).put("replans",replans).put("screens",screens).put("absoluteImpulseNs",impulse).put("nativeWallMillis",millis).put("nativeTicks",executions*180);
            methods.put(method,m);
        }
        var pairs=new JSONObject();for(String other:List.of("simple","fixed","pooled","blind","observed","temporal")){int wins=0,ties=0,losses=0;double sum=0;for(Object raw:rows){var r=((JSONObject)raw).getJSONObject("results");double delta=r.getJSONObject("value").getJSONObject("outcome").getDouble("utility")-r.getJSONObject(other).getJSONObject("outcome").getDouble("utility");sum+=delta;if(delta>1e-12)wins++;else if(delta< -1e-12)losses++;else ties++;}pairs.put(other,new JSONObject().put("meanDelta",sum/rows.length()).put("wins",wins).put("ties",ties).put("losses",losses));}
        return new JSONObject().put("methods",methods).put("pairedValueMinus",pairs);
    }
}
