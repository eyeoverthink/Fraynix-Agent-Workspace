
package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/** A lossless CSV projection of the checked key-and-unit groups. */
final class RoutedKeyCsv {
    static final String FORMAT = "fraynix.routed-key-comparison-csv-v1";
    private static final List<String> HEADER =
            List.of("key_json", "unit", "status", "earlier_readings", "later_readings", "delta");

    private RoutedKeyCsv() {}

    static JSONObject write(JSONArray outcomes) {
        var csv = new StringBuilder(FrayTableInput.csvLine(HEADER)).append('\n');
        for (Object raw : outcomes) {
            JSONObject group = (JSONObject) raw;
            // The cell contains a JSON string so exact keys such as 0007 and =x stay textual.
            csv.append(FrayTableInput.csvLine(List.of(JSONObject.quote(group.getString("key")),
                    group.getString("unit"), group.getString("status"),
                    MapCheckpoint.canonical(group.getJSONArray("earlier")),
                    MapCheckpoint.canonical(group.getJSONArray("later")), delta(group)))).append('\n');
        }
        String text = csv.toString();
        verify(text, outcomes);
        return new JSONObject().put("format", FORMAT).put("groups", outcomes.length())
                .put("csv", text).put("sha256", MeasurementStore.hash(text));
    }

    static void verify(String csv, JSONArray outcomes) {
        // A 4096-character source cell can expand when escaped as a JSON string.
        var rows = MeasurementStore.csv(csv, 1, 32_000);
        if (!rows.getFirst().equals(HEADER) || rows.size() != outcomes.length() + 1)
            throw new IllegalStateException("The comparison CSV header or group count changed");
        for (int i = 0; i < outcomes.length(); i++) {
            var row = rows.get(i + 1);
            var group = outcomes.getJSONObject(i);
            if (row.size() != HEADER.size()
                    || !new JSONArray("[" + row.get(0) + "]").similar(new JSONArray().put(group.getString("key")))
                    || !row.get(1).equals(group.getString("unit"))
                    || !row.get(2).equals(group.getString("status"))
                    || !new JSONArray(row.get(3)).similar(group.getJSONArray("earlier"))
                    || !new JSONArray(row.get(4)).similar(group.getJSONArray("later"))
                    || !row.get(5).equals(delta(group)))
                throw new IllegalStateException("The comparison CSV changed group " + (i + 1));
        }
    }

    private static String delta(JSONObject group) {
        return group.isNull("delta") ? "" : group.getString("delta");
    }
}
