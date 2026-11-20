package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

interface RoutedReport {
    String html();
    JSONArray steps();
    JSONObject artifact(JSONObject file, JSONArray checks);

    default String htmlSha256() { return MeasurementStore.hash(html()); }

    default void verifyBrowser(JSONArray evidence) {
        var plan = steps();
        if (evidence.length() != plan.length()) throw new IllegalStateException("The routed browser checks are incomplete");
        for (int i = 0; i < plan.length(); i++) FrayConversion.verifyStep(plan.getJSONObject(i), evidence.getJSONObject(i), i);
    }
}
