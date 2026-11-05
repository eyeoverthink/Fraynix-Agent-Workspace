package fraymus.workspace;

import org.json.*;
import java.util.*;

/** Frozen one-step replanning. Predictions and measured observations have distinct inputs. */
final class CalibratedPolicy068 {
    static final List<String> METHODS=List.of("simple","temporal","value","calibrated");

    static JSONObject initial(JSONObject task,JSONObject model){return initial(task,model,null);}
    static JSONObject initial(JSONObject task,JSONObject model,JSONObject timing){
        double[] measured={0};var original=TemporalPolicy067.initial(task,measured);var old=original.getJSONObject("decisions");
        var decisions=new JSONObject();for(String method:List.of("simple","temporal","value"))decisions.put(method,WorldFrames.copy(old.getJSONObject(method)));
        if(timing!=null)timing.put("valueMillis",measured[0]);
        var calibrated=WorldFrames.copy(old.getJSONObject("temporal")).put("method","calibrated");long at=System.nanoTime();
        if(calibrated.getBoolean("recover")){
            var screen=ValueScreen067.screen(task,calibrated.getDouble("speedMps"));double p=ValueCalibration068.predict(model,screen.getDouble("successFraction")),u=3*p-2-.147;
            var learned=new JSONObject().put("modelSha256",model.getString("sha256")).put("probability",p).put("estimatedUtility",u).put("declineUtility",-.157).put("requiredGain",.05).put("attempt",u>=-.157+.05);
            calibrated.put("screen",screen).put("calibration068",learned);
            if(!learned.getBoolean("attempt"))calibrated.put("execute",false).put("recover",false).put("reason","Calibrated value declined before the first push");
        }
        if(timing!=null)timing.put("calibratedMillis",calibrated.has("screen")?(System.nanoTime()-at)/1e6:0);
        decisions.put("calibrated",calibrated);return original.put("decisions",decisions).put("modelSha256",model.getString("sha256"));
    }
    static JSONObject outcome(JSONObject task,JSONObject decision,JSONObject record){
        var d=WorldFrames.copy(decision);if(d.getString("method").equals("calibrated"))d.put("method","value");
        var r=record==null?null:WorldFrames.copy(record).put("timing067",record.getJSONObject("timing068"));
        var out=TemporalPolicy067.outcome(task,d,r);int lookups=decision.has("calibration068")?1:0;double fee=.002*lookups;
        return out.put("lookups",lookups).put("logicalCost",out.getDouble("logicalCost")+fee).put("utility",out.getDouble("utility")-fee)
            .put("externalEvents",record==null?0:record.getJSONArray("externalReceipts").length());
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
        var pairs=new JSONObject();for(String other:List.of("simple","temporal","value")){int wins=0,ties=0,losses=0;double sum=0;for(Object raw:rows){var r=((JSONObject)raw).getJSONObject("results");double delta=r.getJSONObject("calibrated").getJSONObject("outcome").getDouble("utility")-r.getJSONObject(other).getJSONObject("outcome").getDouble("utility");sum+=delta;if(delta>1e-12)wins++;else if(delta< -1e-12)losses++;else ties++;}pairs.put(other,new JSONObject().put("meanDelta",sum/rows.length()).put("wins",wins).put("ties",ties).put("losses",losses));}
        return new JSONObject().put("methods",methods).put("pairedCalibratedMinus",pairs);
    }
}
