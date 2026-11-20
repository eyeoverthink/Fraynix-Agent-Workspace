
package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class RoutedKeyComparisonReportTest {
    private static final List<String> SUFFIX = RoutedCsvReport.FIELDS.stream().map(field -> "route_" + field).toList();

    private static String csv(String header, String... readings) {
        StringBuilder result = new StringBuilder(header + "," + String.join(",", SUFFIX) + "\n");
        for (int i = 0; i < readings.length; i++) {
            var reading = readings[i].split("\\|", -1);
            result.append(FrayTableInput.csvLine(List.of(reading[0], Integer.toString(i + 2), "class", "lesson",
                    "input", "0", "in", reading[1], reading[2]))).append('\n');
        }
        return result.toString();
    }

    private static JSONObject export(String label, String csv) {
        return new JSONObject().put("exportId", label).put("sha256", MeasurementStore.hash(csv))
                .put("base64", Base64.getEncoder().encodeToString(csv.getBytes(StandardCharsets.UTF_8)))
                .put("source", new JSONObject());
    }

    private static JSONObject proof(String earlier, String later, String key) {
        var file = export("later", later);
        var report = new RoutedKeyComparisonReport(export("earlier", earlier), earlier, file, later, key);
        var checks = new JSONArray();
        for (Object value : report.steps()) {
            var step = (JSONObject)value;
            checks.put(new JSONObject().put("index", checks.length()).put("selector", step.getString("selector"))
                    .put("expected", step).put("passed", true).put("actual", new JSONObject()
                            .put("count", step.optInt("expectCount", 1)).put("text", step.optString("expectText"))));
        }
        return new JSONObject(report.artifact(file, checks).getJSONObject("files").getString("verification.json"));
    }

    @Test public void matchesExactKeysByUnitAndPreservesAmbiguousOccurrences() {
        String earlier = csv("id", "shared|1.00001|mm", "shared|32|°F", " x |5|mm", "gone|12|mm",
                "only-old-duplicate|6|mm", "only-old-duplicate|7|mm");
        String later = csv("id", "shared|1.00000|mm", "shared|33|°F", "x|9|mm",
                "only-new-duplicate|10|mm", "only-new-duplicate|11|mm");
        var proof = proof(earlier, later, "id");
        assertTrue(new JSONObject().put("Matched", 2).put("Added", 1).put("Missing", 2).put("Duplicate", 2)
                .similar(proof.getJSONObject("counts")));
        var outcomes = proof.getJSONArray("outcomes");
        assertEquals("-0.00001", outcomes.getJSONObject(0).getString("delta"));
        assertEquals("1", outcomes.getJSONObject(1).getString("delta"));
        assertEquals(" x ", outcomes.getJSONObject(2).getString("key"));
        assertEquals("Missing", outcomes.getJSONObject(2).getString("status"));
        assertEquals("Duplicate", outcomes.getJSONObject(4).getString("status"));
        assertTrue(outcomes.getJSONObject(4).isNull("delta"));
        assertEquals(2, outcomes.getJSONObject(4).getJSONArray("earlier").length());
        assertEquals("Added", outcomes.getJSONObject(5).getString("status"));
        assertEquals("Duplicate", outcomes.getJSONObject(6).getString("status"));
        assertEquals(6, outcomes.getJSONObject(6).getJSONArray("later").getJSONObject(1).getInt("row"));
    }

    @Test public void rejectsEmptyKeysAndHeadersOutsideTheOriginalTable() {
        String valid = csv("id", "one|0|mm");
        String other = csv("tag", "one|0|mm");
        String blank = csv("id", "  |0|mm");
        assertThrows(IllegalArgumentException.class, () -> proof(valid, other, "id"));
        assertThrows(IllegalArgumentException.class, () -> proof(valid, valid, "route_output_value"));
        assertThrows(IllegalArgumentException.class, () -> proof(valid, blank, "id"));
    }
}
