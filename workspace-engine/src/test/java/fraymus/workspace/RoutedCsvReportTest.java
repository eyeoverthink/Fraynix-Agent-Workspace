package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class RoutedCsvReportTest {
    private static final List<List<String>> SOURCE = List.of(
            List.of("probe", "inches", "route_source_row"),
            List.of("37.25", "37.25", "equal"),
            List.of("37.25", "37.25", "different unit"),
            List.of("37.25", "37.25", "equal"));

    private static JSONObject row(int number, boolean temperature) {
        return new JSONObject().put("row", number).put("label", temperature ? "temperature" : "length")
                .put("lesson", temperature ? "temperature-tool" : "length-tool")
                .put("inputColumn", temperature ? "probe" : "inches")
                .put("unit", temperature ? "celsius" : "inches").put("value", "37.25");
    }

    private static JSONArray decisions() { return new JSONArray().put(row(2, true)).put(row(3, false)).put(row(4, true)); }

    private static JSONArray stages() {
        return new JSONArray()
                .put(new JSONObject().put("name", "temperature-tool").put("csv", "probe\n37.25\n37.25")
                        .put("lesson", new JSONObject().put("column", "celsius").put("tableColumn", "probe")))
                .put(new JSONObject().put("name", "length-tool").put("csv", "inches\n37.25")
                        .put("lesson", new JSONObject().put("column", "inches")));
    }

    private static JSONArray outputs(String temperature, String length) {
        return new JSONArray().put(new JSONObject().put("result", temperature)).put(new JSONObject().put("result", length));
    }

    @Test public void restoresDuplicateRowsAndSeparatesUnits() {
        String csv = RoutedCsvReport.build(SOURCE, decisions(), stages(),
                outputs("probe,fahrenheit\n37.25,99.05\n37.25,99.05", "946.15 mm"));
        var rows = MeasurementStore.csv(csv, 1);
        assertEquals("route_2_source_row", rows.get(0).get(3));
        assertEquals(List.of("99.05", "°F"), rows.get(1).subList(9, 11));
        assertEquals(List.of("946.15", "mm"), rows.get(2).subList(9, 11));
        assertEquals("4", rows.get(3).get(3));
        assertEquals(rows.get(1).subList(4, 11), rows.get(3).subList(4, 11));
    }

    @Test public void refusesAnIncorrectConversionEvenWithTheRightInputAndUnit() {
        assertThrows(IllegalStateException.class, () -> RoutedCsvReport.build(SOURCE, decisions(), stages(),
                outputs("probe,fahrenheit\n37.25,99.05\n37.25,99.05", "946.16 mm")));
    }

    @Test public void refusesAChangedRowOrUnit() {
        assertThrows(IllegalStateException.class, () -> RoutedCsvReport.build(SOURCE, decisions(), stages(),
                outputs("probe,fahrenheit\n37.25,99.05\n99,99.05", "946.15 mm")));
        assertThrows(IllegalStateException.class, () -> RoutedCsvReport.build(SOURCE, decisions(), stages(),
                outputs("probe,fahrenheit\n37.25,99.05\n37.25,99.05", "946.15 °F")));
    }

    @Test public void refusesIncompleteBatchesAndDuplicateSourceOrdinals() {
        assertThrows(IllegalStateException.class, () -> RoutedCsvReport.build(SOURCE, decisions(), stages(),
                new JSONArray().put(new JSONObject().put("result", "probe,fahrenheit\n37.25,99.05"))));
        var wrong = decisions().put(2, row(2, true));
        assertThrows(IllegalStateException.class, () -> RoutedCsvReport.build(SOURCE, wrong, stages(),
                outputs("probe,fahrenheit\n37.25,99.05\n37.25,99.05", "946.15 mm")));
    }
}
