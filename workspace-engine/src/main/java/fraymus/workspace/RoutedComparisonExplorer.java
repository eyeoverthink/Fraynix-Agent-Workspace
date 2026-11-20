
package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.stream.Collectors;

import static fraymus.workspace.RoutedComparisonReport.count;
import static fraymus.workspace.RoutedComparisonReport.text;
import static fraymus.workspace.RoutedComparisonTotals.encode;

/** An interactive creation bound to a verified totals artifact and its original CSV export. */
final class RoutedComparisonExplorer implements RoutedReport {
    private static final String TITLE = "Verified comparison explorer";
    private static final String TEMPLATE = FrayConversion.loadHtml("routing-comparison-explorer.html");

    private record Filter(String unit, String status) {
        boolean includes(List<String> row) {
            return (unit == null || unit.equals(row.get(1))) && (status == null || status.equals(row.get(2)));
        }
    }

    private record View(Filter filter, List<List<String>> rows, List<RoutedComparisonTotals.Unit> totals) {
        String countText() { return rows.size() + (rows.size() == 1 ? " group" : " groups"); }

        String rowsText() { return rows.stream().map(RoutedComparisonExplorer::rowText).collect(Collectors.joining("\n")); }

        String totalsText() {
            if (totals.isEmpty()) return "No matching groups.";
            return totals.stream().map(unit -> unit.name() + " · " + unit.countsText()
                    + " · Net " + unit.deltaText(unit.net(), true) + " · Absolute " + unit.deltaText(unit.absolute(), false))
                    .collect(Collectors.joining("\n"));
        }

        JSONObject evidence() {
            return new JSONObject().put("unit", filter.unit() == null ? "all" : filter.unit())
                    .put("status", filter.status() == null ? "all" : filter.status())
                    .put("groups", rows.size()).put("totals", new JSONArray(totals.stream().map(RoutedComparisonTotals.Unit::evidence).toList()));
        }
    }

    private final String csv, receiptText, summaryText, bindingText, html;
    private final JSONObject receipt, summaryArtifact;
    private final List<View> views;

    RoutedComparisonExplorer(String csv, JSONObject receipt, JSONObject summaryArtifact, JSONObject summaryProof) {
        this.csv = csv;
        this.receipt = receipt;
        this.summaryArtifact = summaryArtifact;
        receiptText = MapCheckpoint.canonical(receipt) + "\n";
        summaryText = MapCheckpoint.canonical(summaryProof) + "\n";
        var binding = new JSONObject().put("artifact", summaryArtifact).put("proofSha256", MeasurementStore.hash(summaryText));
        bindingText = MapCheckpoint.canonical(binding);
        var table = MeasurementStore.csv(csv, 1, 32_000);
        List<List<String>> rows = table.subList(1, table.size());
        views = List.of(new Filter(null, null), new Filter("mm", null), new Filter("mm", "Duplicate"), new Filter("mm", "Missing"))
                .stream().map(filter -> view(filter, rows)).toList();
        html = TEMPLATE.replace("@EXPLORER_CSV@", encode(csv)).replace("@EXPLORER_RECEIPT@", encode(receiptText))
                .replace("@EXPLORER_SUMMARY@", encode(summaryText)).replace("@EXPLORER_BINDING@", encode(bindingText));
    }

    private static View view(Filter filter, List<List<String>> rows) {
        var selected = rows.stream().filter(filter::includes).toList();
        return new View(filter, selected, RoutedComparisonTotals.totalsFor(selected));
    }

    private static String rowText(List<String> row) {
        String key = new JSONArray("[" + row.get(0) + "]").getString(0);
        String delta = row.getLast();
        String shown = delta.isEmpty() ? "—" : delta.startsWith("-") || delta.equals("0") ? delta : "+" + delta;
        return String.join("\t", RoutedKeyComparisonReport.visibleKey(key), row.get(1), row.get(2),
                readingsText(row.get(3)), readingsText(row.get(4)), shown);
    }

    private static String readingsText(String json) {
        var readings = new JSONArray(json);
        if (readings.isEmpty()) return "—";
        var labels = new java.util.ArrayList<String>();
        for (Object value : readings) {
            var reading = (JSONObject)value;
            labels.add("#" + reading.getInt("row") + ": " + reading.getString("value"));
        }
        return String.join(" · ", labels);
    }

    @Override public String html() { return html; }

    @Override public JSONArray steps() {
        var all = views.get(0);
        var steps = new JSONArray().put(text("h1", TITLE))
                .put(text("#source-id", "Export " + receipt.getString("id")))
                .put(text("#source-hash", "CSV SHA-256 " + receipt.getString("sha256")))
                .put(text("#parent-proof", "Summary SHA-256 " + MeasurementStore.hash(summaryText)));
        addView(steps, all);
        for (String name : List.of("matched-comparison.csv", "import-receipt.json", "summary-proof.json"))
            steps.put(count("a[href=\"" + name + "\"]", 1));
        steps.put(click("#unit-mm"));
        addView(steps, views.get(1));
        steps.put(click("#status-duplicate"));
        addView(steps, views.get(2));
        steps.put(click("#status-missing")).put(text("#visible-count", views.get(3).countText()));
        steps.put(click("#reset-filters"));
        addView(steps, all);
        return steps;
    }

    private static JSONObject click(String selector) { return count(selector, 1).put("action", "click"); }

    private static void addView(JSONArray steps, View view) {
        steps.put(text("#visible-count", view.countText())).put(text("#visible-rows", view.rowsText()))
                .put(text("#visible-totals", view.totalsText()));
    }

    @Override public JSONObject artifact(JSONObject file, JSONArray checks) {
        verifyBrowser(checks);
        var binding = new JSONObject(bindingText);
        var proof = new JSONObject().put("passed", true).put("import", receipt).put("summary", binding)
                .put("views", new JSONArray(views.stream().map(View::evidence).toList())).put("browserChecks", checks.length());
        return new JSONObject().put("title", TITLE).put("sources", new JSONArray()
                        .put("comparison-summary:" + summaryArtifact.getString("id") + ":sha256:" + summaryArtifact.getString("sha256"))
                        .put("matched-comparison-csv:sha256:" + receipt.getString("sha256"))
                        .put("matched-comparison-receipt:sha256:" + MeasurementStore.hash(MapCheckpoint.canonical(receipt))))
                .put("files", new JSONObject().put("index.html", html).put("matched-comparison.csv", csv)
                        .put("import-receipt.json", receiptText).put("summary-proof.json", summaryText)
                        .put("verification.json", MapCheckpoint.canonical(proof) + "\n"));
    }
}
