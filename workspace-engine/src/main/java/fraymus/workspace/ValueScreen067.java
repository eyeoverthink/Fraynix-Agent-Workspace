package fraymus.workspace;

import org.json.*;

/** Prospective sensitivity scenarios, not calibrated recovery probabilities. */
final class ValueScreen067 {
    static JSONObject screen(JSONObject task,double speed){
        double radius=Calibration065.radius();var nominal=RecoveryPolicy066.predicted(task,speed);
        var scenarios=new JSONArray();int successes=0;
        for(int offset=-1;offset<=1;offset++)for(int residual=-1;residual<=1;residual++){
            var state=WorldFrames.copy(nominal).put("distanceM",nominal.getDouble("distanceM")+offset*radius);
            var plan=RecoveryPolicy066.replan(task,state);
            double endpoint=plan.getDouble("predictedEndpointM")+residual*radius;
            boolean success=Math.abs(endpoint-task.getDouble("centerM"))<=task.getDouble("halfWidthM");
            if(success)successes++;
            scenarios.put(new JSONObject().put("decisionOffsetM",offset*radius).put("terminalResidualM",residual*radius)
                .put("state",state).put("plan",plan).put("endpointM",endpoint).put("success",success));
        }
        double fraction=successes/9.0,utility=3*fraction-2-.145;
        return new JSONObject().put("radiusM",radius).put("model",Calibration065.info()).put("scenarios",scenarios)
            .put("successFraction",fraction).put("estimatedUtility",utility).put("declineUtility",-.155)
            .put("requiredGain",.05).put("attempt",utility>=-.155+.05)
            .put("meaning","Nine equally weighted sensitivity scenarios with nominal contact; not calibrated recovery confidence");
    }
}
