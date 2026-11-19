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

    @Test public void temperatureAliasesResolveToTheSameTypedPlan() {
        var expected = FrayChain.fromRequest("Convert -17.7778 Celsius to Fahrenheit, then Fahrenheit to Rankine.");
        for (String alias : new String[]{
                "Convert -17.7778\u2103 to \u2109, then \u00b0F to degrees Rankine.",
                "Please convert -17.7778 centigrade to degrees Fahrenheit, then deg F to deg R."}) {
            var parsed = FrayChain.fromRequest(alias);
            assertTrue(expected.definition().similar(parsed.definition()));
            assertEquals(expected.htmlSha256, parsed.htmlSha256);
        }
        assertEquals("c-f-r-v1", expected.definition().getString("id"));
        assertEquals("-0.00004 \u00b0F", expected.firstSteps().getJSONObject(3).getString("expectText"));
        assertThrows(IllegalArgumentException.class, () -> FrayChain.fromRequest(
                "Convert -273.1501 C to F, then F to R."));
        for (String second : new String[]{"mm to cm", "kelvins to R", "C to F"}) {
            var error = assertThrows(IllegalArgumentException.class, () -> FrayChain.fromRequest(
                    "Convert 0 C to F, then " + second + "."));
            assertTrue(error.getMessage().contains("Cannot hand off"));
        }
    }

    @Test public void findsAStagedTemperatureHandoffAndKeepsItsOriginalReceipt() throws Exception {
        var project = Files.createTempDirectory("fray-temperature-recovery-");
        JSONObject binding;
        JSONArray original;
        try (var store = new WorkspaceStore(project)) {
            var wb = new RequestWorkbench(project, store);
            var task = wb.chat(new JSONObject().put("request",
                    "Convert -17.7778 C to F, then F to R.")).getJSONObject("task");
            assertFalse(wb.recoverChain(new JSONObject()).has("task"));
            binding = new JSONObject().put("id", task.getString("id")).put("reviewSha256", task.getString("reviewSha256"));
            var render = wb.prepareFray(binding);
            binding.put("htmlSha256", render.getString("htmlSha256"));
            original = observations(render.getJSONArray("steps"));
            wb.advanceChain(copy(binding).put("checks", original));
        }
        try (var store = new WorkspaceStore(project)) {
            var wb = new RequestWorkbench(project, store);
            var recovered = wb.recoverChain(new JSONObject()).getJSONObject("task");
            assertEquals(binding.getString("id"), recovered.getString("id"));
            assertTrue(recovered.getBoolean("firstVerified"));
            assertEquals("-0.00004", recovered.getJSONObject("intermediate").getString("value"));
            var changed = new JSONArray(original.toString());
            changed.getJSONObject(2).getJSONObject("actual").put("text", "Different button label");
            assertThrows(IllegalStateException.class, () -> wb.advanceChain(copy(binding).put("checks", changed)));
            var next = wb.advanceChain(copy(binding).put("checks", original));
            var steps = next.getJSONArray("steps");
            assertEquals("459.66996 \u00b0R", steps.getJSONObject(4).getString("expectText"));
            var result = wb.finishChain(copy(binding).put("handoff", next.getJSONObject("handoff"))
                    .put("checks", observations(steps)));
            assertTrue(result.getBoolean("passed"));
            assertFalse(wb.recoverChain(new JSONObject()).has("task"));
        }
    }

    @Test public void listsOnlyStagedChainsAndRecoversAnExplicitReview() throws Exception {
        var project = Files.createTempDirectory("fray-pending-chains-");
        String temperatureId;
        JSONObject lengthBinding;
        JSONObject lengthNext;
        try (var store = new WorkspaceStore(project)) {
            var wb = new RequestWorkbench(project, store);
            var temperature = staged(wb, "Convert 37.25 C to F, then F to R.");
            temperatureId = temperature.getString("id");
            lengthBinding = staged(wb, PROMPT);
            var render = wb.prepareFray(new JSONObject().put("id", lengthBinding.getString("id"))
                    .put("reviewSha256", lengthBinding.getString("reviewSha256")));
            assertTrue(render.getBoolean("resuming"));
            lengthNext = wb.advanceChain(copy(lengthBinding).put("checks", observations(render.getJSONArray("steps"))));
            wb.chat(new JSONObject().put("request", "Convert -40 C to F, then F to R."));
            assertFalse(wb.recoverChain(new JSONObject()).has("task"));
        }
        try (var store = new WorkspaceStore(project)) {
            var wb = new RequestWorkbench(project, store);
            var rows = wb.pendingChains(new JSONObject()).getJSONArray("chains");
            assertEquals(2, rows.length());
            var lengthId = lengthBinding.getString("id");
            assertEquals(java.util.Set.of(temperatureId, lengthId), java.util.Set.of(
                    rows.getJSONObject(0).getString("id"), rows.getJSONObject(1).getString("id")));
            var chosen = new JSONObject().put("id", lengthId);
            var selected = wb.recoverChain(chosen).getJSONObject("task");
            assertEquals(lengthId, selected.getString("id"));
            assertEquals("313.57824", selected.getJSONObject("intermediate").getString("value"));
            wb.finishChain(copy(lengthBinding).put("handoff", lengthNext.getJSONObject("handoff"))
                    .put("checks", observations(lengthNext.getJSONArray("steps"))));
            assertThrows(IllegalStateException.class, () -> wb.recoverChain(chosen));
            assertEquals(temperatureId, wb.pendingChains(new JSONObject()).getJSONArray("chains")
                    .getJSONObject(0).getString("id"));
            assertEquals(temperatureId, wb.recoverChain(new JSONObject()).getJSONObject("task").getString("id"));
        }
    }

    @Test public void resumesAfterArtifactAndReceiptWereWrittenWithoutDuplicatingEither() throws Exception {
        var project = Files.createTempDirectory("fray-interrupted-artifact-");
        JSONObject input;
        JSONObject receipt;
        String artifactId;
        try (var store = new WorkspaceStore(project)) {
            var wb = new RequestWorkbench(project, store);
            var binding = staged(wb, PROMPT);
            String reviewId = binding.getString("id");
            input = new JSONObject().put("id", reviewId).put("reviewSha256", binding.getString("reviewSha256"));
            assertThrows(IllegalStateException.class, () -> wb.resumeChainSave(input));
            var taskPath = project.resolve("request-workbench").resolve(reviewId + ".task.json");
            var task = AgentFiles042.loadChecked(taskPath);
            var chain = FrayChain.fromDefinition(task.getJSONObject("choice").getJSONObject("chain"));
            var first = task.getJSONArray("frayFirstChecks");
            var secondSteps = chain.secondSteps(chain.verifiedIntermediate(first));
            var second = observations(secondSteps);
            var expected = chain.artifact(first, second);
            // A persisted queue entry and the real artifact store reproduce the point before review finalization.
            AgentFiles042.saveChecked(taskPath, task.put("frayChecks", second).put("fraySaveSha256", "different"));
            assertThrows(IllegalStateException.class, () -> wb.resumeChainSave(input));
            assertEquals(0, store.artifacts().length());
            AgentFiles042.saveChecked(taskPath, task.put("fraySaveSha256", WorldFrames.hash(expected)));
            var artifact = store.writeOnce("request-workbench:" + reviewId, expected);
            artifactId = artifact.getString("id");
            receipt = store.recordTest(new JSONObject().put("id", artifactId).put("version", 1)
                    .put("steps", FrayChain.join(chain.firstSteps(), secondSteps, false))
                    .put("checks", FrayChain.join(first, second, true)));
        }
        try (var store = new WorkspaceStore(project)) {
            var wb = new RequestWorkbench(project, store);
            assertTrue(wb.prepareFray(input).getBoolean("commitReady"));
            var saved = wb.resumeChainSave(input);
            assertEquals(artifactId, saved.getJSONObject("artifact").getString("id"));
            assertEquals(1, store.artifacts().length());
            assertTrue(receipt.similar(store.read(artifactId).getJSONObject("test")));
            assertTrue(saved.getJSONObject("artifact").similar(wb.resumeChainSave(input).getJSONObject("artifact")));
        }
    }

    private static JSONObject staged(RequestWorkbench workbench, String request) throws Exception {
        var task = workbench.chat(new JSONObject().put("request", request)).getJSONObject("task");
        var binding = new JSONObject().put("id", task.getString("id")).put("reviewSha256", task.getString("reviewSha256"));
        var render = workbench.prepareFray(binding);
        binding.put("htmlSha256", render.getString("htmlSha256"));
        workbench.advanceChain(copy(binding).put("checks", observations(render.getJSONArray("steps"))));
        return binding;
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
