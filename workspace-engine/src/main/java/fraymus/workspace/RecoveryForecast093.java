package fraymus.workspace;

import org.json.JSONObject;
import java.time.Instant;

/** Exact, bounded provenance for a new prediction from recovered measured state. */
final class RecoveryForecast093 {
    static final String FORMAT="recovered-stage-forecast-093-v1";

    static boolean matches(JSONObject forecast,JSONObject frame,JSONObject plan){
        return forecast.getJSONObject("definition").similar(WorldFrames.scene(frame))
            &&forecast.getJSONObject("plan").similar(plan);
    }
    static int reserve(int steps,int step){
        if(step<1||step>=steps||steps>3)throw new IllegalStateException("Replacement forecast requires a later bounded stage");
        return 2*(steps-step);
    }
    static void admit(int spent,int steps,int step,double elapsed){
        if(!Procedure078.admitted(spent,reserve(steps,step),elapsed))
            throw new IllegalStateException("Recovered forecast and remaining actual work exceed the original budget");
    }
    static JSONObject binding(int step,JSONObject prior,JSONObject previous,JSONObject anchor,JSONObject frame,JSONObject plan){
        if(step<1||anchor.optInt("resumes")<1||anchor.optInt("resumes")>2)
            throw new IllegalStateException("A verified resumed actual ancestor is required");
        return new JSONObject().put("format",FORMAT).put("step",step)
            .put("originalForecastId",prior.getString("id")).put("originalForecastSha256",WorldFrames.hash(prior))
            .put("previousActualId",previous.getString("id")).put("previousActualSha256",WorldFrames.hash(previous))
            .put("resumedAncestorId",anchor.getString("id")).put("resumedAncestorSha256",WorldFrames.hash(anchor))
            .put("frameSha256",WorldFrames.hash(frame)).put("planSha256",WorldFrames.hash(plan));
    }
    static void bindingMatches(JSONObject saved,JSONObject expected){
        var copy=WorldFrames.copy(saved);copy.remove("forecastId");
        if(!copy.similar(expected))throw new IllegalStateException("Recovered forecast provenance changed");
    }
    static void before(JSONObject completed,JSONObject next){
        if(!completed.getString("status").equals("completed")
            ||Instant.parse(completed.getString("settledAt")).isAfter(Instant.parse(next.getString("plannedAt"))))
            throw new IllegalStateException("Later stage was planned before its prerequisite completed");
    }
}
