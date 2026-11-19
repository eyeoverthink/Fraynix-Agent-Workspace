package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class ChainRecommendationTest {
    private static JSONObject admitted(String text, JSONObject ramp, JSONObject relay) {
        var task = new JSONObject().put("goal", text).put("family044", ChainRecommendation.FAMILY)
                .put("model", "local-model").put("modelDigest", "test-model-digest")
                .put(ChainRecommendation.KEY, new JSONObject().put("course", ChainRecommendation.course(text)))
                .put("history", new JSONArray()).put("context044", new JSONObject());
        if (ramp != null) task.put("rampSource", ramp);
        if (relay != null) task.put("relaySource", relay);
        record(task, "queued", new JSONObject().put("objectiveSha256", ChatChain052.objectiveHash(task)));
        return task;
    }

    private static void record(JSONObject task, String kind, JSONObject detail) {
        task.getJSONArray("history").put(new JSONObject().put("kind", kind).put("detail", WorldFrames.copy(detail)));
    }

    @Test public void allowsOneTypedRecommendationAndRetainsItsOriginalCourse() {
        var task = admitted("Let the robot clear every rail on Relay Ridge.", null,
                new JSONObject().put("manifestSha256", "original"));
        assertEquals(List.of(ChainRecommendation.TOOL), ChatPolicy044.allowed(task));
        var schema = Tools044.schema(ChainRecommendation.TOOL, task);
        assertThrows(IllegalArgumentException.class, () -> ToolSchema043.validate(schema,
                new JSONObject().put("choice", "relay").put("approval", true)));
        assertThrows(IllegalArgumentException.class, () -> ToolSchema043.validate(schema, new JSONObject().put("choice", "web")));
        var decision = ChainRecommendation.retain(task, "relay");
        record(task, ChainRecommendation.EVENT, decision);
        ChatChain052.verifyObjective(task);
        assertEquals(ReviewedChain.RELAY, ReviewedChain.of(task));
        assertFalse(task.optBoolean(ReviewedChain.RELAY.approval));
        assertFalse(task.getJSONObject("context044").has("ownedGameTask115"));
        assertEquals("Relay Ridge", decision.getJSONObject("plan").getString("course"));
        assertThrows(IllegalStateException.class, () -> ChainRecommendation.retain(task, "ramp"));
        task.getJSONObject("relaySource").put("manifestSha256", "different");
        assertThrows(IllegalStateException.class, () -> ChatChain052.verifyObjective(task));
    }

    @Test public void ignoresFittedPlanWhenMissingSourceAndChecksReviewOrdering() {
        var missing = admitted("Compare the slopes and report on the winning one.", null, null);
        record(missing, ChainRecommendation.EVENT, ChainRecommendation.retain(missing, "ramp"));
        ChatChain052.verifyObjective(missing);
        assertNull(ReviewedChain.of(missing));
        assertTrue(missing.getString(ChainRecommendation.QUESTION).contains("two setups"));
        assertEquals("", ChainRecommendation.course("Relay Ridge or Relay Switchyard?"));

        var paired = admitted("Which incline takes the block farther?", new JSONObject().put("secondValues", new JSONObject()), null);
        var decision = ChainRecommendation.retain(paired, "ramp");
        record(paired, "chain-confirmed", decision.getJSONObject("plan"));
        record(paired, ChainRecommendation.EVENT, decision);
        assertThrows(IllegalStateException.class, () -> ChatChain052.verifyObjective(paired));
    }
}
