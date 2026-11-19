package fraymus.workspace;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class RoutedDashboardReportTest {
    private static final String HEADER = "id,route_2_source_row,route_2_classification,route_2_lesson,route_2_input_column,"
            + "route_2_input_value,route_2_input_unit,route_2_output_value,route_2_output_unit\n";

    private static String row(int n, String result, String unit) {
        return "id," + n + ",test,lesson,value,0,source," + result + "," + unit + "\n";
    }

    private static String summary(RoutedDashboardReport report, String selector) {
        for (var raw : report.steps()) {
            var step = (org.json.JSONObject)raw;
            if (selector.equals(step.getString("selector"))) return step.getString("expectText");
        }
        throw new AssertionError("Missing summary: " + selector);
    }

    @Test public void roundsNegativeMeanAtFivePlaces() {
        var report = new RoutedDashboardReport(HEADER + row(2, "-0.00001", "°F") + row(3, "0", "°F"));
        assertEquals("2 rows · Min -0.00001 °F · Max 0 °F · Mean -0.00001 °F", summary(report, "#summary-fahrenheit"));
        assertFalse(report.steps().toString().contains("#filter-millimetres"));
        assertEquals("2 verified rows · 1 output unit", summary(report, "#dashboard-count"));
    }

    @Test public void keepsUnitsSeparateAndIncludesDuplicateRows() {
        var report = new RoutedDashboardReport(HEADER + row(2, "99.05", "°F")
                + row(3, "946.15", "mm") + row(4, "99.05", "°F"));
        assertEquals("2 rows · Min 99.05 °F · Max 99.05 °F · Mean 99.05 °F", summary(report, "#summary-fahrenheit"));
        assertEquals("1 row · Min 946.15 mm · Max 946.15 mm · Mean 946.15 mm", summary(report, "#summary-millimetres"));
        assertTrue(summary(report, "#routed-rows").startsWith("2 ·"));
        assertTrue(summary(report, "#routed-rows").lines().toList().getLast().startsWith("4 ·"));
    }

    @Test public void refusesUnknownUnitsWrongSchemaAndMissingSourceRows() {
        assertThrows(IllegalArgumentException.class, () -> new RoutedDashboardReport(HEADER + row(2, "0", "cm")));
        assertThrows(IllegalArgumentException.class, () -> new RoutedDashboardReport(HEADER + row(3, "0", "mm")));
        assertThrows(IllegalArgumentException.class, () -> new RoutedDashboardReport(
                HEADER.replace("route_2_output_unit", "route_output_unit") + row(2, "0", "mm")));
    }
}
