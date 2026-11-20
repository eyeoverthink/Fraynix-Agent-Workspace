
package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RoutedComparisonTotalsTest {
    @Test public void distinguishesZeroNetFromNoComparableRows() {
        String csv = """
                key_json,unit,status,earlier_readings,later_readings,delta
                """;
        csv += "\"\"\"a\"\"\",°F,Matched,[],[],0.00001\n"
                + "\"\"\"b\"\"\",°F,Matched,[],[],-0.00001\n"
                + "\"\"\"c\"\"\",°F,Duplicate,[],[],\n"
                + "\"\"\"d\"\"\",mm,Missing,[],[],\n";
        var receipt = new JSONObject().put("id", "sample").put("sha256", MeasurementStore.hash(csv));
        var report = new RoutedComparisonTotals(csv, receipt);
        var checks = new JSONArray();
        for (Object value : report.steps()) {
            var step = (JSONObject)value;
            checks.put(new JSONObject().put("index", checks.length()).put("selector", step.getString("selector"))
                    .put("expected", step).put("passed", true).put("actual", new JSONObject()
                            .put("count", step.optInt("expectCount", 1)).put("text", step.optString("expectText"))));
        }
        var proof = new JSONObject(report.artifact(new JSONObject(), checks).getJSONObject("files").getString("verification.json"));
        var totals = proof.getJSONArray("totals");
        var temperatures = totals.getJSONObject(0);
        assertEquals(2, temperatures.getInt("matched"));
        assertEquals(1, temperatures.getInt("duplicate"));
        assertEquals("0", temperatures.getString("netDelta"));
        assertEquals("0.00002", temperatures.getString("absoluteDelta"));
        var lengths = totals.getJSONObject(1);
        assertEquals(1, lengths.getInt("missing"));
        assertTrue(lengths.isNull("netDelta"));
        assertTrue(lengths.isNull("absoluteDelta"));
    }
}
