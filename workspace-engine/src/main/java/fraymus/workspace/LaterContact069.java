package fraymus.workspace;
import org.json.*;
/** Tick108 prefix only; independent physical guard and explicit remaining action budget. */
final class LaterContact069 {
    static JSONObject plan(JSONObject task,JSONObject definition,JSONArray prefix,JSONObject firstReceipt){
        if(prefix.length()!=28)throw new IllegalArgumentException("Exactly tick108 prefix required");
        var evidence=new JSONArray();boolean motion=true,geometry=true;int contacts=0;
        for(int i=25;i<=27;i++){
            var s=prefix.getJSONObject(i);if(s.getInt("tick")!=i*4)throw new IllegalArgumentException("Later clock differs");
            var p=Stopping063.projected(definition,s);double gap=TemporalContact067.clearance(definition,s);
            boolean safe=p.getDouble("rotationDeg")<=10&&Math.abs(p.getDouble("normalMps"))<=.1&&p.getDouble("angularSpeedRadps")<=2&&p.getDouble("tangentMps")>=0;
            boolean close=gap>=-.03&&gap<=.03;motion&=safe;geometry&=close;if(p.getInt("nearPoints")>0)contacts++;
            evidence.put(p.put("geometricClearanceM",gap).put("motionEligible",safe).put("geometryEligible",close).put("sampleSha256",WorldFrames.hash(s)));
        }
        var state=RecoveryPolicy066.measured(definition,prefix.getJSONObject(27)).put("kind","measured-later-window")
            .put("contactWindow",new JSONObject().put("samples",evidence).put("contactSamples",contacts).put("motionEligible",motion).put("geometryEligible",geometry).put("eligible",contacts>0&&motion&&geometry));
        double spent=firstReceipt==null?0:Math.abs(firstReceipt.getDouble("deltaVelocityMps"));if(!Double.isFinite(spent)||spent>.16+1e-12)throw new IllegalArgumentException("First budget invalid");
        var anchor=firstReceipt==null?prefix.getJSONObject(0):firstReceipt.getJSONObject("after");
        var base=Stopping063.projected(definition,anchor);double elapsed=(108-anchor.getInt("tick"))/240.0,a=RecoveryPolicy066.acceleration(task),v=Math.max(0,base.getDouble("tangentMps"));
        double expectedDistance=base.getDouble("distanceM")+RecoveryPolicy066.travel(v,a,elapsed),expectedSpeed=Math.max(0,v+a*elapsed);
        var prediction=new JSONObject().put("anchorTick",anchor.getInt("tick")).put("anchorSha256",WorldFrames.hash(anchor)).put("expectedDistanceM",expectedDistance).put("expectedTangentMps",expectedSpeed)
            .put("distanceErrorM",state.getDouble("distanceM")-expectedDistance).put("speedErrorMps",state.getDouble("tangentMps")-expectedSpeed);
        prediction.put("unexpected",Math.abs(prediction.getDouble("distanceErrorM"))>.01||Math.abs(prediction.getDouble("speedErrorMps"))>.04).put("meaning","Diagnostic error against prior actual state; not an external-event label");
        return inverse(task,state,spent).put("predictionError",prediction);
    }
    static JSONObject inverse(JSONObject task,JSONObject state,double spent){
        double distance=state.getDouble("distanceM"),speed=state.getDouble("tangentMps"),a=RecoveryPolicy066.acceleration(task),remaining=task.getDouble("centerM")-distance,h=.30,bound=Math.max(0,Math.min(.12,.24-spent));
        if(!Double.isFinite(distance)||!Double.isFinite(speed)||!Double.isFinite(spent)||spent<0||spent>.16+1e-12)throw new IllegalArgumentException("Invalid late input");
        double endpoint=distance+RecoveryPolicy066.travel(Math.max(0,speed),a,h),wanted=speed,delta=0;String reason="Current prediction already fits";
        if(!fits(task,endpoint)){
            reason="Target behind current state";
            if(remaining>=0){
                wanted=a>=0?Math.max(0,(remaining-.5*a*h*h)/h):remaining<=.5*(-a)*h*h?Math.sqrt(2*(-a)*remaining):remaining/h+.5*(-a)*h;
                double proposed=Math.max(-bound,Math.min(bound,wanted-speed)),next=speed+proposed;
                if(!state.getJSONObject("contactWindow").getBoolean("eligible"))reason="Later contact or motion guard refused correction";
                else if(next<0||next>.8||!fits(task,distance+RecoveryPolicy066.travel(Math.max(0,next),a,h)))reason="Remaining correction budget cannot reach goal";
                else if(Math.abs(proposed)<=1e-6)reason="No additional impulse required";
                else {delta=proposed;reason="Second bounded same-world correction";}
            }
        }
        return new JSONObject().put("state",WorldFrames.copy(state)).put("decisionTick",108).put("remainingSeconds",h).put("spentCorrectionMps",spent).put("availableCorrectionMps",bound)
            .put("desiredTangentMps",wanted).put("deltaVelocityMps",delta).put("correct",delta!=0).put("predictedEndpointM",distance+RecoveryPolicy066.travel(Math.max(0,speed+delta),a,h)).put("reason",reason).put("calibratedInterval",false);
    }
    private static boolean fits(JSONObject t,double x){return Math.abs(x-t.getDouble("centerM"))<=t.getDouble("halfWidthM")+1e-12;}
}
