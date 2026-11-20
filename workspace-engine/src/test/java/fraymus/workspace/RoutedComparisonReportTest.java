package fraymus.workspace;

import org.json.JSONObject;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.Assert.assertEquals;

public class RoutedComparisonReportTest {
    private static final String HEADER = "id,route_source_row,route_classification,route_lesson,route_input_column,"
            + "route_input_value,route_input_unit,route_output_value,route_output_unit\n";

    private static String row(int number, String value, String unit) {
        return "id," + number + ",test,lesson,value,0,source," + value + "," + unit + "\n";
    }

    private static JSONObject file(String text) {
        return new JSONObject().put("sha256", MeasurementStore.hash(text))
                .put("base64", Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8)));
    }

    private static String expected(RoutedComparisonReport report, String selector) {
        for (var raw : report.steps()) {
            var step = (JSONObject)raw;
            if (selector.equals(step.getString("selector"))) return step.getString("expectText");
        }
        throw new AssertionError(selector);
    }

    private static RoutedComparisonReport compare(String a, String b) {
        return new RoutedComparisonReport(file(a), a, file(b), b);
    }

    @Test public void computesChangeBeforeRoundingEitherMean() {
        var a = HEADER + row(2, "0", "mm") + row(3, "0", "mm") + row(4, "0.00001", "mm");
        var b = HEADER + row(2, "0.00001", "mm") + row(3, "0.00001", "mm") + row(4, "0", "mm");
        var report = compare(a, b);
        assertEquals("3 rows · Min 0 mm · Max 0.00001 mm · Mean 0 mm", expected(report, "#earlier-millimetres"));
        assertEquals("3 rows · Min 0 mm · Max 0.00001 mm · Mean 0.00001 mm", expected(report, "#later-millimetres"));
        assertEquals("Mean change · 0 mm", expected(report, "#change-millimetres"));
    }

    @Test public void marksUnitsMissingFromEitherSourceWithoutAChange() {
        var report = compare(HEADER + row(2, "-40", "°F"), HEADER + row(2, "63.5", "mm"));
        assertEquals("No rows in °F", expected(report, "#later-fahrenheit"));
        assertEquals("No rows in mm", expected(report, "#earlier-millimetres"));
        assertEquals("Mean change unavailable · no matching unit on both sides", expected(report, "#change-fahrenheit"));
        assertEquals("Mean change unavailable · no matching unit on both sides", expected(report, "#change-millimetres"));
    }
}
