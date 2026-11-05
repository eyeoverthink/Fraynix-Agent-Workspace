package fraymus.workspace;
import org.json.*;
import java.util.*;
/** Pure-Java prospective scheduling. No actual future event or trace is an input. */
final class ObservationSchedule070 {
    static final List<Integer> TICKS=List.of(92,116,140);
    static JSONArray scenarios(){
        var rows=new JSONArray().put(scenario("nominal",.50,0,-1,0))
            .put(scenario("offset-negative",.10,-.01,-1,0)).put(scenario("offset-positive",.10,.01,-1,0));
        for(int tick:new int[]{76,108,140})for(double impulse:new double[]{-.08,.08})
            rows.put(scenario("hypothetical-"+tick+"-"+impulse,.05,0,tick,impulse));
        return rows;
    }
    private static JSONObject scenario(String name,double weight,double offset,int tick,double delta){
        return new JSONObject().put("name",name).put("weight",weight).put("offsetM",offset).put("eventTick",tick).put("deltaMps",delta);
    }
    /** Integrate known incline with kinetic friction and a static rest threshold. */
    static double[] advance(JSONObject task,double distance,double speed,double seconds){
        if(!Double.isFinite(distance)||!Double.isFinite(speed)||!Double.isFinite(seconds)||seconds<0)throw new IllegalArgumentException("Invalid hypothetical state");
        double gravity=9.81*Math.sin(Math.toRadians(task.getDouble("angleDeg"))),ratio=task.getDouble("frictionRatio");
        double down=gravity*(1-ratio);
        if(speed==0)return down>0?new double[]{distance+.5*down*seconds*seconds,down*seconds}:new double[]{distance,0};
        double a=speed>0?down:gravity*(1+ratio);
        double zero=a*speed<0?-speed/a:Double.POSITIVE_INFINITY;
        if(zero<=seconds){
            distance+=speed*zero+.5*a*zero*zero;double rest=seconds-zero;
            return down>0?new double[]{distance+.5*down*rest*rest,down*rest}:new double[]{distance,0};
        }
        return new double[]{distance+speed*seconds+.5*a*seconds*seconds,speed+a*seconds};
    }
    static double[] propagate(JSONObject task,double[] start,JSONObject scenario,int from,int to){
        if(from>to)throw new IllegalArgumentException("Reverse hypothetical clock");
        int event=scenario.getInt("eventTick");
        if(event>from&&event<=to){
            var middle=advance(task,start[0],start[1],(event-from)/240.0);middle[1]+=scenario.getDouble("deltaMps");
            return advance(task,middle[0],middle[1],(to-event)/240.0);
        }
        return advance(task,start[0],start[1],(to-from)/240.0);
    }
    static JSONObject anchor(JSONObject task,JSONObject decision,JSONObject firstPlan){
        if(decision.getBoolean("recover")!=(firstPlan!=null))throw new IllegalArgumentException("Missing or unexpected first observation");
        if(firstPlan==null)return RecoveryPolicy066.predicted(task,decision.getDouble("speedMps")).put("tick",60).put("source","Initial action prediction; no measured tick60 observation").put("spentCorrectionMps",0);
        var state=firstPlan.getJSONObject("state");
        if(!state.getString("kind").equals("measured-window"))throw new IllegalArgumentException("First observation is not selected measured evidence");
        return new JSONObject().put("kind","measured-first-window-plus-command").put("tick",60).put("distanceM",state.getDouble("distanceM"))
            .put("tangentMps",state.getDouble("tangentMps")+firstPlan.getDouble("deltaVelocityMps")).put("spentCorrectionMps",Math.abs(firstPlan.getDouble("deltaVelocityMps")))
            .put("firstPlanSha256",WorldFrames.hash(firstPlan)).put("source","Selected tick60 state plus verified commanded delta; no new observation");
    }
    private static boolean success(JSONObject task,double x){return Math.abs(x-task.getDouble("centerM"))<=task.getDouble("halfWidthM");}
    static JSONObject plan(JSONObject task,JSONObject decision,JSONObject firstPlan){
        var anchor=anchor(task,decision,firstPlan);double spent=anchor.getDouble("spentCorrectionMps");var candidates=new JSONArray();
        int selected=-1;double best=.02-1e-12;
        for(int tick:TICKS){
            var rows=new JSONArray();double plainUtility=0,checkedUtility=0,actionWeight=0;
            for(Object raw:scenarios()){
                var scenario=(JSONObject)raw;double weight=scenario.getDouble("weight");double[] start={anchor.getDouble("distanceM")+scenario.getDouble("offsetM"),anchor.getDouble("tangentMps")};
                var plain=propagate(task,start,scenario,60,180);var observed=propagate(task,start,scenario,60,tick);
                var state=new JSONObject().put("kind","hypothetical-nominal-contact").put("distanceM",observed[0]).put("tangentMps",observed[1])
                    .put("contactWindow",new JSONObject().put("eligible",observed[1]>=0));
                var correction=AdaptiveContact070.inverse(task,state,spent,tick);
                var endpoint=propagate(task,new double[]{observed[0],observed[1]+correction.getDouble("deltaVelocityMps")},scenario,tick,180);
                boolean no=success(task,plain[0]),yes=success(task,endpoint[0]);
                plainUtility+=weight*(no?1:-2);checkedUtility+=weight*(yes?1:-2);
                if(correction.getBoolean("correct"))actionWeight+=weight;
                rows.put(new JSONObject().put("scenario",scenario).put("stateAtCheck",new JSONArray(observed)).put("noCheckEndpointM",plain[0])
                    .put("checkedEndpointM",endpoint[0]).put("noCheckSuccess",no).put("checkedSuccess",yes).put("correction",correction));
            }
            double no=plainUtility-.005,with=checkedUtility-.005-.10-.02*actionWeight,gain=with-no;
            candidates.put(new JSONObject().put("tick",tick).put("scenarios",rows).put("noCheckUtility",no).put("checkedUtility",with).put("gain",gain).put("weightedActions",actionWeight));
            if(gain>=.02-1e-12&&(selected<0||gain>best+1e-12)){selected=tick;best=gain;}
        }
        return new JSONObject().put("decisionTick",60).put("anchor",anchor).put("candidates",candidates).put("selectedTick",selected).put("minimumGain",.02)
            .put("selectedGain",selected<0?0:best).put("scheduleFee",.005).put("maximumExtraObservations",3)
            .put("reason",selected<0?"Skip: no candidate clears the observation-value threshold":"Observe at the earliest best-value time")
            .put("scope","Frozen hypothetical weights and nominal support assumptions; not calibrated probabilities or knowledge of actual future events");
    }
}
