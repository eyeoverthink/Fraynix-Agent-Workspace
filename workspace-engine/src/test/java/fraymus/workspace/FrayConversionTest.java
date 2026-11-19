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
        assertEquals("304.8 mm", FrayConversion.INCH.expected("12"));
        assertEquals("3.175 mm", FrayConversion.INCH.expected("0.125"));
        assertEquals("-63.5 mm", FrayConversion.INCH.expected("-2.5"));
        assertEquals("25399999.99746 mm", FrayConversion.INCH.expected("999999.9999"));
        assertEquals("0 mm", FrayConversion.INCH.expected("-0.0000"));
        assertEquals("Enter a decimal within the stated range.", FrayConversion.INCH.expected("1000000.0001"));
        assertEquals("Enter a decimal within the stated range.", FrayConversion.INCH.expected("1e3"));
        assertEquals("Enter a measurement.", FrayConversion.INCH.expected(" "));
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
            assertEquals(FrayConversion.INCH.previewPath(), prepared.getString("preview"));
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

    @Test public void temperatureChatRejectsOtherDirectionsAndOneOffQuestions() {
        assertEquals(RequestLanguage.Intent.FRAY_CELSIUS,
                RequestLanguage.direct("Build a Celsius-to-Fahrenheit browser tool."));
        assertEquals(RequestLanguage.Intent.FRAY_CELSIUS,
                RequestLanguage.direct("Build me a browser tool to convert °C to °F."));
        assertNull(RequestLanguage.direct("Build a Fahrenheit to Celsius browser tool."));
        assertNull(RequestLanguage.direct("Convert 37 Celsius to Fahrenheit."));
        assertNull(RequestLanguage.direct("Do not build a Celsius to Fahrenheit browser tool."));
    }

    @Test public void evaluatesTemperatureWithOffsetAndFullInputValidation() {
        var converter = FrayConversion.CELSIUS;
        assertEquals("212 \u00b0F", converter.expected("100"));
        assertEquals("-40 \u00b0F", converter.expected("-40"));
        assertEquals("-459.67 \u00b0F", converter.expected("-273.15"));
        assertEquals("-0.00004 \u00b0F", converter.expected("-17.7778"));
        assertEquals("0.00014 \u00b0F", converter.expected("-17.7777"));
        assertEquals("32.09 \u00b0F", converter.expected(".05"));
        assertEquals("1800031.99982 \u00b0F", converter.expected("+999999.9999"));
        assertEquals("Enter a decimal within the stated range.", converter.expected("1000000.0001"));
        assertEquals("Enter a decimal within the stated range.", converter.expected("1,5"));
        assertEquals("Enter a measurement.", converter.expected(" "));
    }

    @Test public void cannotSaveOneConvertersEvidenceForAnother() throws Exception {
        var project = Files.createTempDirectory("fray-temperature-");
        try (var store = new WorkspaceStore(project)) {
            var wb = new RequestWorkbench(project, store);
            var review = wb.chat(new JSONObject().put("request", "Build a Celsius to Fahrenheit browser tool"))
                    .getJSONObject("task");
            var confirmation = new JSONObject().put("id", review.getString("id"))
                    .put("reviewSha256", review.getString("reviewSha256"));
            var prepared = wb.prepareFray(confirmation);
            assertEquals(FrayConversion.CELSIUS.previewPath(), prepared.getString("preview"));
            assertNotEquals(FrayConversion.INCH.htmlSha256, FrayConversion.CELSIUS.htmlSha256);
            assertNull(FrayConversion.previewAt("/request-preview/unknown/index.html"));
            assertEquals(FrayConversion.INCH, FrayConversion.previewAt(FrayConversion.INCH.previewPath()));
            var admission = new JSONObject(confirmation.toString()).put("htmlSha256", FrayConversion.INCH.htmlSha256)
                    .put("checks", sampleBrowserContract(FrayConversion.INCH.steps()));
            assertThrows(IllegalStateException.class, () -> wb.finishFray(admission));
            admission.put("htmlSha256", prepared.getString("htmlSha256"));
            assertThrows(IllegalStateException.class, () -> wb.finishFray(admission));
            assertEquals("review", wb.read(new JSONObject().put("id", review.getString("id"))).getString("status"));
            var verified = wb.finishFray(admission.put("checks", sampleBrowserContract(prepared.getJSONArray("steps"))));
            assertTrue(verified.getBoolean("passed"));
            var artifact = store.read(verified.getJSONObject("artifact").getString("id"));
            assertEquals("Celsius to Fahrenheit", artifact.getString("title"));
            assertEquals("celsius * 9 / 5 + 32\n", artifact.getJSONObject("contents").getString("fray-source.txt"));
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
