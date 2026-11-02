package fraymus.workspace;

import org.json.JSONObject;

/** Functional appraisal: measured conditions change background work, not evidential truth. */
final class AgentAppraisal042 {
    private double alertness,progress;
    private static double unit(double x){return Math.max(0,Math.min(1,x));}
    JSONObject update(JSONObject facts,long elapsedDeltaMs,boolean enabled){
        double uncertainty=facts.optBoolean("observationFresh",false)?0:1;
        double pressure=unit(facts.optDouble("heapFraction",0)),lag=unit(facts.optDouble("collectionMs",0)/500);
        double change=facts.optBoolean("sourceChanged",false)?1:0;
        double alpha=1-Math.exp(-Math.max(0,Math.min(60_000,elapsedDeltaMs))/5000.0);
        alertness=unit(alertness+(Math.max(uncertainty*.6,change)-alertness)*alpha);
        progress=unit(progress+(facts.optBoolean("newCompletedGoal",false)?1:0)-alpha*.1);
        String decision=!enabled?"paused":pressure>.85||lag>.9?"rest":change>0?"integrate-observation":facts.optBoolean("hasRetainedEvidence")?"consolidate":uncertainty>0?"await-observation":"consolidate";
        return new JSONObject().put("alertness",alertness).put("goalProgress",progress).put("uncertainty",uncertainty).put("resourcePressure",pressure).put("collectionLag",lag).put("decision",decision).put("allowNeuralUpdate",enabled&&change>0&&!decision.equals("rest")).put("reason",decision.equals("rest")?"Measured memory/collection pressure reduces background work":decision.equals("integrate-observation")?"A changed source can update the shared sensory state":decision.equals("await-observation")?"No fresh external observation; retain unknown instead of inventing a measurement":"Bounded maintenance of retained evidence").put("meaning","Computational appraisal changes work scheduling. It is not a claim of felt emotion, calibrated probability or improved task performance.");
    }
    JSONObject save(){return new JSONObject().put("alertness",alertness).put("goalProgress",progress);}
    void restore(JSONObject state){alertness=unit(state.optDouble("alertness",0));progress=unit(state.optDouble("goalProgress",0));}
}
