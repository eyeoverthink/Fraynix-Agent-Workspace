package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.nio.file.Files;

import static org.junit.Assert.*;

public class FrayChainTest {
    private static final String PROMPT = "Convert 12.3456 inches to millimetres, then millimetres to centimetres.";

    @Test public void refusesTwoDifferentWrongUnitHandoffsInChat() throws Exception {
        var project = Files.createTempDirectory("fray-chain-refusals-");
        try (var store = new WorkspaceStore(project)) {
            var workbench = new RequestWorkbench(project, store);
            for (String[] wrong : new String[][]{{"Celsius to Fahrenheit", "\u00b0C"}, {"inches to millimetres", "in"}}) {
                var reply = workbench.chat(new JSONObject().put("request",
                        "Convert 12 inches to millimetres, then " + wrong[0] + "."));
                assertTrue(reply.getBoolean("matched"));
                assertTrue(reply.getString("refusal").contains("mm to a step that expects " + wrong[1]));
                assertFalse(reply.has("task"));
            }
            assertFalse(Files.exists(project.resolve("request-workbench")));
        }
    }

    @Test public void verifiesTheFirstStageBeforeDispatchAndRetainsItAcrossRestart() throws Exception {
        var project = Files.createTempDirectory("fray-chain-recovery-");
        JSONObject binding;
        JSONObject handoff;
        JSONArray second;
        try (var store = new WorkspaceStore(project)) {
            var wb = new RequestWorkbench(project, store);
            var task = wb.chat(new JSONObject().put("request", PROMPT)).getJSONObject("task");
            binding = new JSONObject().put("id", task.getString("id")).put("reviewSha256", task.getString("reviewSha256"));
            var render = wb.prepareFray(binding);
            assertTrue(render.getBoolean("chain"));
            binding.put("htmlSha256", render.getString("htmlSha256"));
            var blank = copy(binding).put("handoff", new JSONObject()).put("checks", new JSONArray());
            assertThrows(IllegalStateException.class, () -> wb.finishChain(blank));
            var steps = render.getJSONArray("steps");
            var wrongUnit = observations(steps);
            wrongUnit.getJSONObject(3).getJSONObject("actual").put("text", "313.57824 cm");
            assertThrows(IllegalStateException.class, () -> wb.advanceChain(copy(binding).put("checks", wrongUnit)));
            var next = wb.advanceChain(copy(binding).put("checks", observations(steps)));
            handoff = next.getJSONObject("handoff");
            assertEquals("313.57824", handoff.getString("value"));
            assertEquals("mm", handoff.getString("unit"));
            second = next.getJSONArray("steps");
            assertEquals("31.357824 cm", second.getJSONObject(4).getString("expectText"));
            assertThrows(Exception.class, () -> wb.verify(new JSONObject().put("id",binding.getString("id"))));
        }
        try (var store = new WorkspaceStore(project)) {
            var wb = new RequestWorkbench(project, store);
            var ref = new JSONObject().put("id", binding.getString("id"));
            assertTrue(wb.read(ref).getBoolean("firstVerified"));
            assertEquals("review", wb.read(ref).getString("status"));
            for (JSONObject bad : new JSONObject[]{copy(handoff).put("unit","in"), copy(handoff).put("value","313.58")}) {
                var finish = copy(binding).put("handoff", bad).put("checks", observations(second));
                assertThrows(IllegalStateException.class, () -> wb.finishChain(finish));
            }
            var forged = observations(second);
            forged.getJSONObject(3).getJSONObject("actual").put("text", "313.57824 cm");
            assertThrows(IllegalStateException.class, () -> wb.finishChain(copy(binding).put("handoff", handoff).put("checks", forged)));
            var approved = copy(binding).put("handoff", handoff).put("checks", observations(second));
            var completed = wb.finishChain(approved);
            assertTrue(completed.getBoolean("passed"));
            var reopened = wb.verify(ref);
            assertTrue(completed.getJSONObject("artifact").similar(reopened.getJSONObject("artifact")));
            var artifact = reopened.getJSONObject("artifact").getString("id");
            assertEquals("31.357824 cm", new JSONObject(store.read(artifact).getJSONObject("contents")
                    .getString("verification.json")).getString("result"));
            wb.learn(ref);
            assertEquals("remembered", wb.chat(new JSONObject().put("request",PROMPT))
                    .getJSONObject("task").getJSONObject("choice").getString("by"));
            assertEquals("java", wb.chat(new JSONObject().put("request",PROMPT.replace("12.3456","-12.3456")))
                    .getJSONObject("task").getJSONObject("choice").getString("by"));
        }
    }

    private static JSONObject copy(JSONObject value) { return new JSONObject(value.toString()); }

    // Boundary fixture. The publication test drives the same ordered steps in Chromium.
    private static JSONArray observations(JSONArray steps) {
        var result = new JSONArray();
        for (int i = 0; i < steps.length(); i++) {
            var step = steps.getJSONObject(i);
            var actual = new JSONObject().put("count",step.optInt("expectCount",1))
                    .put("value",step.optString("expectValue")).put("text",step.optString("expectText"));
            result.put(new JSONObject().put("index",i).put("selector",step.getString("selector"))
                    .put("expected",step).put("actual",actual).put("passed",true));
        }
        return result;
    }
}
