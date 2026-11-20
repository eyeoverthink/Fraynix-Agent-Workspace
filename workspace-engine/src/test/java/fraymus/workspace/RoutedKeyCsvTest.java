
package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class RoutedKeyCsvTest {
    private static JSONObject reading(int row, String value) {
        return new JSONObject().put("row", row).put("value", value);
    }

    @Test public void preservesExactKeysAndAmbiguousSourceRows() {
        String literal = "=001,\"α\"\nline";
        var earlier = new JSONArray().put(reading(3, "-40")).put(reading(7, "50"));
        var later = new JSONArray().put(reading(8, "32"));
        var groups = new JSONArray().put(new JSONObject().put("key", literal)
                .put("unit", "°F").put("status", "Duplicate").put("earlier", earlier).put("later", later)
                .put("delta", JSONObject.NULL));
        var result = RoutedKeyCsv.write(groups);
        var csv = result.getString("csv");
        var cells = MeasurementStore.csv(csv, 1).get(1);
        assertEquals(JSONObject.quote(literal), cells.get(0));
        assertEquals(earlier.toString(), new JSONArray(cells.get(3)).toString());
        assertEquals("", cells.getLast());
        assertEquals(MeasurementStore.hash(csv), result.getString("sha256"));
        assertThrows(IllegalStateException.class, () -> RoutedKeyCsv.verify(csv.replace("Duplicate", "Matched"), groups));
        assertThrows(IllegalStateException.class, () -> RoutedKeyCsv.verify(csv.replace("-40", "-41"), groups));
        assertThrows(IllegalStateException.class, () -> RoutedKeyCsv.verify(csv.replace(",°F,", ",mm,"), groups));
        assertThrows(IllegalStateException.class, () -> RoutedKeyCsv.verify(csv.stripTrailing() + "0\n", groups));
    }

    @Test public void acceptsAFullLengthKeyThatNeedsEscaping() {
        String literal = "\\".repeat(4096);
        var groups = new JSONArray().put(new JSONObject().put("key", literal)
                .put("unit", "mm").put("status", "Matched")
                .put("earlier", new JSONArray().put(reading(2, "2.5")))
                .put("later", new JSONArray().put(reading(2, "2.50001"))).put("delta", "0.00001"));
        var csv = RoutedKeyCsv.write(groups).getString("csv");
        var cells = MeasurementStore.csv(csv, 1, 32_000).get(1);
        assertEquals(literal, new JSONArray("[" + cells.getFirst() + "]").getString(0));
        assertEquals("0.00001", cells.getLast());
    }
}
