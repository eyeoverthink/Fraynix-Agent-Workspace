package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.nio.file.Files;

import static org.junit.Assert.*;

public class FrayConversionTest {
    @Test public void recognizesTheToolRatherThanAnIndividualConversion() {
        assertEquals(RequestLanguage.Intent.FRAY_INCH, RequestLanguage.direct(
                "Make me a little converter for inches to millimetres and give me a browser version."));
        assertEquals(RequestLanguage.Intent.FRAY_INCH,
                RequestLanguage.direct("Build an inches-to-mm browser tool."));
        assertNull(RequestLanguage.direct("How many millimetres is 1 inch?"));
        assertNull(RequestLanguage.direct("Build a millimetres to inches browser tool"));
        assertNull(RequestLanguage.direct("Do not build an inches-to-mm browser tool"));
    }

    @Test public void computesFrayResultsForDecimalsAndBounds() {
        assertEquals("304.8 mm", FrayConversion.expected("12"));
        assertEquals("3.175 mm", FrayConversion.expected("0.125"));
        assertEquals("-63.5 mm", FrayConversion.expected("-2.5"));
        assertEquals("25399999.99746 mm", FrayConversion.expected("999999.9999"));
        assertEquals("0 mm", FrayConversion.expected("-0.0000"));
        assertEquals("Enter a decimal within the stated range.", FrayConversion.expected("1000000.0001"));
        assertEquals("Enter a decimal within the stated range.", FrayConversion.expected("1e3"));
        assertEquals("Enter a measurement.", FrayConversion.expected(" "));
    }

    @Test public void requiresApprovalAndIndependentBrowserEvidenceAndRejectsChangedOutput() throws Exception {
        var project = Files.createTempDirectory("fray-conversion-");
        String artifactId;
        JSONObject ref;
        JSONObject original;
        try (var store = new WorkspaceStore(project)) {
            var workbench = new RequestWorkbench(project, store);
            var review = workbench.chat(new JSONObject().put("request", "Build an inches-to-mm browser tool"))
                    .getJSONObject("task");
            ref = new JSONObject().put("id", review.getString("id"));
            assertThrows(Exception.class, () -> workbench.verify(ref));
            var approved = new JSONObject(ref.toString()).put("reviewSha256", review.getString("reviewSha256"));
            assertThrows(IllegalStateException.class, () -> workbench.prepareFray(
                    new JSONObject(ref.toString()).put("reviewSha256", "different")));
            var prepared = workbench.prepareFray(approved);
            assertEquals(FrayConversion.previewPath(), prepared.getString("preview"));
            var steps = prepared.getJSONArray("steps");
            var complete = sampleBrowserContract(steps);
            var proof = new JSONObject(approved.toString()).put("htmlSha256", prepared.getString("htmlSha256"));
            assertThrows(IllegalStateException.class, () -> workbench.finishFray(
                    new JSONObject(proof.toString()).put("htmlSha256", "different").put("checks", complete)));
            assertThrows(IllegalStateException.class, () -> workbench.finishFray(
                    new JSONObject(proof.toString()).put("checks", new JSONArray())));
            var incorrect = new JSONArray(complete.toString());
            incorrect.getJSONObject(0).getJSONObject("actual").put("text", "25 mm");
            assertThrows(IllegalStateException.class, () -> workbench.finishFray(
                    new JSONObject(proof.toString()).put("checks", incorrect)));
            assertEquals("review", workbench.read(ref).getString("status"));
            original = workbench.finishFray(proof.put("checks", complete));
            artifactId = original.getJSONObject("artifact").getString("id");
            assertEquals(artifactId, workbench.finishFray(proof).getJSONObject("artifact").getString("id"));
            assertTrue(workbench.learn(ref).getBoolean("remembered"));
        }
        try (var store = new WorkspaceStore(project)) {
            var workbench = new RequestWorkbench(project, store);
            assertTrue(workbench.verify(ref).getJSONObject("artifact").similar(original.getJSONObject("artifact")));
            var recalled = workbench.chat(new JSONObject().put("request", "Build an inches-to-mm browser tool"))
                    .getJSONObject("task");
            assertEquals("remembered", recalled.getJSONObject("choice").getString("by"));
            assertEquals("review", recalled.getString("status"));
            Files.writeString(store.asset(artifactId, 1, "index.html"), "Changed original HTML");
            assertThrows(IllegalStateException.class, () -> workbench.verify(ref));
        }
    }

    // Boundary fixture. The publication test generates observations with the actual Chromium controls.
    private static JSONArray sampleBrowserContract(JSONArray steps) {
        var observations = new JSONArray();
        for (int i = 0; i < steps.length(); i++) {
            var step = steps.getJSONObject(i);
            var actual = new JSONObject().put("count", 1)
                    .put("text", step.optString("expectText")).put("value", step.optString("expectValue"));
            observations.put(new JSONObject().put("index", i).put("selector", step.getString("selector"))
                    .put("expected", step).put("actual", actual).put("passed", true));
        }
        return observations;
    }
}
