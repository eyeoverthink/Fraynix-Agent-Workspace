package fraymus.workspace;

import org.json.*;
import java.util.*;

/** Forecast -> authority -> utility. Typed numerical stages, not autonomous LLM personas. */
final class ObservationValue072 {
    private static final double[] OFFSETS={-1,0,1},WEIGHTS={.25,.5,.25};
    private static boolean fits(JSONObject t,double x){return Math.abs(x-t.getDouble("centerM"))<=t.getDouble("halfWidthM")+1e-12;}
    private static JSONObject state(double x,double v){return new JSONObject().put("kind","hypothetical-state-grid").put("distanceM",x).put("tangentMps",v).put("contactWindow",new JSONObject().put("eligible",v>=0));}
    static JSONObject plan(JSONObject task,JSONObject decision,JSONObject first,JSONObject models,boolean simple){
        var forecast=StateUncertainty072.forecast(task,decision,first,models);var anchor=forecast.getJSONObject("anchor");double spent=anchor.getDouble("spentCorrectionMps"),bound=Math.max(0,Math.min(.12,.24-spent));var candidates=new JSONArray();int selected=-1;double best=.02-1e-12;
        if(simple){
            var b=forecast.getJSONArray("bands").getJSONObject(0);var end=forecast.getJSONArray("bands").getJSONObject(3).getJSONObject("distance");
            boolean risk=end.getDouble("lower")<task.getDouble("centerM")-task.getDouble("halfWidthM")-1e-12||end.getDouble("upper")>task.getDouble("centerM")+task.getDouble("halfWidthM")+1e-12;
            var inverse=AdaptiveContact070.inverse(task,state(b.getJSONObject("distance").getDouble("nominal"),b.getJSONObject("speed").getDouble("nominal")),spent,92);
            if(risk&&inverse.getBoolean("correct"))selected=92;
            candidates.put(new JSONObject().put("tick",92).put("atRisk",risk).put("nominalCorrection",inverse).put("availableCorrectionMps",bound).put("selected",selected==92));
            return new JSONObject().put("decisionTick",60).put("anchor",anchor).put("forecast",forecast).put("candidates",candidates).put("selectedTick",selected).put("scheduleFee",.01).put("maximumExtraObservations",3).put("role","simple nominal reachability").put("reason",selected<0?"Skip: nominal correction is unavailable or the terminal band fits":"Look early: uncertain terminal position and a nominal bounded correction exists").put("scope","One tick92 nominal-contact inverse; uncertainty from separate calibration. Actual measured contact guards still apply.");
        }
        for(int index=0;index<3;index++){
            int tick=ObservationSchedule070.TICKS.get(index);var band=forecast.getJSONArray("bands").getJSONObject(index);var dx=band.getJSONObject("distance");var dv=band.getJSONObject("speed");
            double no=0,yes=0,actions=0,saved=0,harmed=0,unreachable=0,safe=0,lower=Double.POSITIVE_INFINITY,upper=Double.NEGATIVE_INFINITY;var scenarios=new JSONArray();
            for(Object raw:ObservationSchedule070.scenarios()){
                var scenario=(JSONObject)raw;var predicted=ObservationSchedule070.propagate(task,new double[]{anchor.getDouble("distanceM")+scenario.getDouble("offsetM"),anchor.getDouble("tangentMps")},scenario,60,tick);
                double sn=0,sy=0,sa=0,ss=0,sh=0,su=0,si=0,min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;
                for(int i=0;i<3;i++)for(int j=0;j<3;j++){
                    double weight=WEIGHTS[i]*WEIGHTS[j];double x=predicted[0]+dx.getDouble("residualCenter")+OFFSETS[i]*dx.getDouble("radius"),v=predicted[1]+dv.getDouble("residualCenter")+OFFSETS[j]*dv.getDouble("radius");
                    var plain=ObservationSchedule070.propagate(task,new double[]{x,v},scenario,tick,180);var inverse=AdaptiveContact070.inverse(task,state(x,v),spent,tick);
                    var corrected=ObservationSchedule070.propagate(task,new double[]{x,v+inverse.getDouble("deltaVelocityMps")},scenario,tick,180);
                    boolean n=fits(task,plain[0]),c=fits(task,corrected[0]),a=inverse.getBoolean("correct");
                    sn+=weight*(n?1:-2);sy+=weight*(c?1:-2);if(a)sa+=weight;if(!n&&c)ss+=weight;if(n&&!c)sh+=weight;if(n)si+=weight;if(!n&&!a)su+=weight;
                    min=Math.min(min,plain[0]);max=Math.max(max,plain[0]);
                }
                double weight=scenario.getDouble("weight");no+=weight*sn;yes+=weight*sy;actions+=weight*sa;saved+=weight*ss;harmed+=weight*sh;unreachable+=weight*su;safe+=weight*si;lower=Math.min(lower,min);upper=Math.max(upper,max);
                scenarios.put(new JSONObject().put("scenario",scenario).put("noCheckUtility",sn).put("correctedUtility",sy).put("correctingFraction",sa).put("savedFraction",ss).put("harmedFraction",sh).put("unreachableFraction",su).put("alreadySafeFraction",si).put("endpointLowerM",min).put("endpointUpperM",max));
            }
            double gain=yes-no-.10-.02*actions;boolean eligible=saved>0&&actions>0&&gain>=.02-1e-12;
            var row=new JSONObject().put("tick",tick).put("scenarios",scenarios).put("noCheckUtility",no-.01).put("checkedUtility",yes-.01-.10-.02*actions).put("gain",gain).put("weightedActions",actions).put("savedFraction",saved).put("harmedFraction",harmed).put("unreachableFraction",unreachable).put("alreadySafeFraction",safe).put("availableCorrectionMps",bound).put("endpointLowerM",lower).put("endpointUpperM",upper).put("eligible",eligible).put("hypotheticalStates",81);
            candidates.put(row);if(eligible&&(selected<0||gain>best+1e-12)){selected=tick;best=gain;}
        }
        return new JSONObject().put("decisionTick",60).put("anchor",anchor).put("forecast",forecast).put("candidates",candidates).put("selectedTick",selected).put("selectedGain",selected<0?0:best).put("minimumGain",.02).put("scheduleFee",.01).put("maximumExtraObservations",3).put("role","forecast, bounded-control and value stages").put("reason",selected<0?"Skip: no uncertain state comparison offers useful correction above its sensing cost":"Observe at the earliest best-value window where bounded correction can save a goal").put("scope","243 hypothetical states, original scenario weights and independent .25/.50/.25 residual grid assumptions; not learned probabilities. Contact is assumed for planning and measured before actual correction.");
    }
}
