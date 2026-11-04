package fraymus.workspace;

import org.json.*;
import java.util.*;

/** Frozen one-step replanning. Predictions and measured observations have distinct inputs. */
final class RecoveryPolicy066 {
    static final List<String> METHODS=List.of("simple","fixed","pooled","blind","observed");
    static JSONObject initial(JSONObject task){
        var base=DecisionPolicy065.plan(task);var choices=WorldFrames.copy(base.getJSONObject("decisions"));
        boolean recover=base.getBoolean("supported")&&!choices.getJSONObject("pooled").getBoolean("execute");
        for(String method:List.of("blind","observed")){var d=WorldFrames.copy(choices.getJSONObject(recover?"simple":"pooled"));d.put("method",method).put("recover",recover);if(recover)d.put("reason","Initial Pooled refusal: one real first push and at most one correction");choices.put(method,d);}
        base.put("decisions",choices).put("initialPooledRefusal",recover);return base;
    }
    static double acceleration(JSONObject task){return 9.81*Math.sin(Math.toRadians(task.getDouble("angleDeg")))*(1-task.getDouble("frictionRatio"));}
    static double travel(double speed,double acceleration,double seconds){double t=acceleration<0?Math.min(seconds,speed/-acceleration):seconds;return speed*t+.5*acceleration*t*t;}
    static JSONObject predicted(JSONObject task,double speed){double a=acceleration(task);return new JSONObject().put("kind","predicted").put("distanceM",travel(speed,a,.25)).put("tangentMps",Math.max(0,speed+a*.25)).put("nominalContactAssumed",true);}
    static JSONObject measured(JSONObject definition,JSONObject sample){var p=Stopping063.projected(definition,sample);var out=new JSONObject().put("kind","measured").put("sampleSha256",WorldFrames.hash(sample));for(String k:List.of("distanceM","tangentMps","normalMps","rotationDeg","angularSpeedRadps","nearPoints"))out.put(k,p.get(k));return out;}
    static JSONObject replan(JSONObject task,JSONObject state){
        String kind=state.getString("kind");if(!Set.of("predicted","measured").contains(kind))throw new IllegalArgumentException("Unknown observation source");
        double distance=state.getDouble("distanceM"),speed=state.getDouble("tangentMps"),a=acceleration(task),remaining=task.getDouble("centerM")-distance;
        if(!Double.isFinite(distance)||!Double.isFinite(speed))throw new IllegalArgumentException("Non-finite planner state");
        boolean eligible=speed>=0;
        if(kind.equals("measured"))eligible&=state.getInt("nearPoints")>=1&&state.getDouble("rotationDeg")<=10&&Math.abs(state.getDouble("normalMps"))<=.10&&state.getDouble("angularSpeedRadps")<=2;
        double wanted=0,delta=0;String reason="Target behind current state";
        if(remaining>=0){wanted=a>=0?Math.max(0,(remaining-.5*a*.5*.5)/.5):remaining<=.5*(-a)*.5*.5?Math.sqrt(2*(-a)*remaining):remaining/.5+.5*(-a)*.5;
            delta=Math.max(-.16,Math.min(.16,wanted-speed));double next=speed+delta;double endpoint=distance+travel(Math.max(0,next),a,.5);
            boolean fits=endpoint>=task.getDouble("centerM")-task.getDouble("halfWidthM")-1e-12&&endpoint<=task.getDouble("centerM")+task.getDouble("halfWidthM")+1e-12;
            if(!eligible){delta=0;reason="Measured contact or motion guard refused correction";}else if(next<0||next>.8||!fits){delta=0;reason="Bounded correction cannot predict arrival inside goal";}else if(Math.abs(delta)<=1e-6){delta=0;reason="No additional impulse required";}else reason="One bounded corrective impulse";
        }
        double predicted=distance+travel(Math.max(0,speed+delta),a,.5);
        return new JSONObject().put("state",WorldFrames.copy(state)).put("decisionTick",60).put("remainingSeconds",.5).put("desiredTangentMps",wanted).put("deltaVelocityMps",delta).put("correct",delta!=0).put("predictedEndpointM",predicted).put("reason",reason).put("calibratedInterval",false);
    }
    static JSONObject outcome(JSONObject task,JSONObject decision,JSONObject record){
        if(!decision.getBoolean("execute")){if(record!=null)throw new IllegalStateException("Refusal has motion");return new JSONObject().put("executed",false).put("success",false).put("utility",-.15).put("logicalCost",.15).put("actions",0).put("observations",0).put("replans",0).put("absoluteImpulseNs",0).put("nativeWallMillis",0);}
        if(record==null)throw new IllegalStateException("Missing attempted motion");
        double endpoint=record.getJSONObject("summary").getDouble("endpointM"),error=Math.abs(endpoint-task.getDouble("centerM"));boolean success=error<=task.getDouble("halfWidthM");
        var plan=record.optJSONObject("correctionPlan");int replans=plan==null?0:1,observations=replans==1&&decision.getString("method").equals("observed")?1:0,actions=1+(plan!=null&&plan.getBoolean("correct")?1:0);double delta=plan==null?0:plan.getDouble("deltaVelocityMps"),cost=.02*actions+.01*replans+.03*observations;
        return new JSONObject().put("executed",true).put("success",success).put("endpointM",endpoint).put("targetErrorM",error).put("actions",actions).put("observations",observations).put("replans",replans).put("logicalCost",cost).put("utility",(success?1:-2)-cost).put("absoluteImpulseNs",decision.getDouble("speedMps")+Math.abs(delta)).put("nativeWallMillis",record.getJSONObject("timing066").getDouble("nativeWallMillis"));
    }
    static JSONObject evaluate(JSONArray rows,boolean details){
        if(rows.isEmpty())throw new IllegalStateException("Empty evaluation");var methods=new JSONObject();int initialRefusals=0;for(Object raw:rows)if(((JSONObject)raw).getJSONObject("plan").getBoolean("initialPooledRefusal"))initialRefusals++;
        for(String method:METHODS){int success=0,miss=0,refuse=0,actions=0,observations=0,replans=0,recovered=0,attempted=0;double utility=0,cost=0,error=0,impulse=0,millis=0;
            for(Object raw:rows){var r=(JSONObject)raw;var o=r.getJSONObject("results").getJSONObject(method).getJSONObject("outcome");if(o.getBoolean("executed")){if(o.getBoolean("success"))success++;else miss++;error+=o.getDouble("targetErrorM");if(r.getJSONObject("plan").getBoolean("initialPooledRefusal")){attempted++;if(o.getBoolean("success"))recovered++;}}else refuse++;
                utility+=o.getDouble("utility");cost+=o.getDouble("logicalCost");actions+=o.getInt("actions");observations+=o.getInt("observations");replans+=o.getInt("replans");impulse+=o.getDouble("absoluteImpulseNs");millis+=o.getDouble("nativeWallMillis");}
            int n=rows.length(),executions=success+miss;var m=new JSONObject().put("tasks",n).put("successes",success).put("misses",miss).put("refusals",refuse).put("meanUtility",utility/n).put("meanLogicalCost",cost/n).put("allTaskSuccessRate",success/(double)n).put("allTaskMissRate",miss/(double)n).put("initiallyRefusedSuccesses",recovered).put("initiallyRefusedAttempts",attempted);
            if(details)m.put("executions",executions).put("executionRate",executions/(double)n).put("conditionalSuccessRate",executions==0?JSONObject.NULL:success/(double)executions).put("meanTargetErrorM",executions==0?JSONObject.NULL:error/executions).put("initialRefusals",initialRefusals).put("recoveryRate",initialRefusals==0?JSONObject.NULL:recovered/(double)initialRefusals).put("actions",actions).put("observations",observations).put("replans",replans).put("absoluteImpulseNs",impulse).put("nativeWallMillis",millis).put("nativeTicks",executions*180);
            methods.put(method,m);
        }
        var pairs=new JSONObject();for(String other:List.of("simple","fixed","pooled","blind")){int wins=0,ties=0,losses=0;double sum=0;for(Object raw:rows){var r=((JSONObject)raw).getJSONObject("results");double delta=r.getJSONObject("observed").getJSONObject("outcome").getDouble("utility")-r.getJSONObject(other).getJSONObject("outcome").getDouble("utility");sum+=delta;if(delta>1e-12)wins++;else if(delta< -1e-12)losses++;else ties++;}pairs.put(other,new JSONObject().put("meanDelta",sum/rows.length()).put("wins",wins).put("ties",ties).put("losses",losses));}
        return new JSONObject().put("methods",methods).put("pairedObservedMinus",pairs);
    }
}
