
package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static fraymus.workspace.RoutedComparisonReport.count;
import static fraymus.workspace.RoutedComparisonReport.text;

/** Totals derived from the independently revalidated comparison CSV. */
final class RoutedComparisonTotals implements RoutedReport {
    private static final String TITLE = "Verified comparison CSV totals";
    private static final String TEMPLATE = FrayConversion.loadHtml("routing-comparison-totals.html");
    private static final List<String> STATUSES = List.of("Matched", "Added", "Missing", "Duplicate");

    private record Unit(String name, int matched, int added, int missing, int duplicate, BigDecimal net,
                        BigDecimal absolute) {
        static Unit from(String name, List<List<String>> rows) {
            int[] counts = new int[STATUSES.size()];
            var net = BigDecimal.ZERO;
            var absolute = BigDecimal.ZERO;
            for (var row : rows) {
                if (!name.equals(row.get(1))) continue;
                int status = STATUSES.indexOf(row.get(2));
                counts[status]++;
                if (status == 0) {
                    var delta = new BigDecimal(row.get(5));
                    net = net.add(delta);
                    absolute = absolute.add(delta.abs());
                }
            }
            return new Unit(name, counts[0], counts[1], counts[2], counts[3], net, absolute);
        }

        int size() { return matched + added + missing + duplicate; }

        String countsText() {
            return "Matched " + matched + " · Added " + added + " · Missing " + missing + " · Duplicate " + duplicate;
        }

        String deltaText(BigDecimal value, boolean signed) {
            if (matched == 0) return "—";
            return (signed && value.signum() > 0 ? "+" : "") + RoutedDashboardReport.plain(value) + " " + name;
        }

        Object evidenceDelta(BigDecimal value) {
            return matched == 0 ? JSONObject.NULL : RoutedDashboardReport.plain(value);
        }

        JSONObject evidence() {
            return new JSONObject().put("unit", name).put("matched", matched).put("added", added)
                    .put("missing", missing).put("duplicate", duplicate).put("netDelta", evidenceDelta(net))
                    .put("absoluteDelta", evidenceDelta(absolute));
        }
    }

    private final JSONObject receipt;
    private final String csv, html;
    private final List<Unit> units;

    RoutedComparisonTotals(String csv, JSONObject receipt) {
        this.csv = csv;
        this.receipt = receipt;
        var rows = MeasurementStore.csv(csv, 1, 32_000);
        units = List.of("°F", "mm").stream().map(unit -> Unit.from(unit, rows.subList(1, rows.size())))
                .filter(unit -> unit.size() > 0).toList();
        html = TEMPLATE.replace("@COMPARISON_CSV@", encode(csv))
                .replace("@IMPORT_RECEIPT@", encode(MapCheckpoint.canonical(receipt) + "\n"));
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    @Override public String html() { return html; }

    @Override public JSONArray steps() {
        var steps = new JSONArray().put(text("h1", TITLE))
                .put(text("#import-id", "Export " + receipt.getString("id")))
                .put(text("#import-sha", "SHA-256 " + receipt.getString("sha256")))
                .put(text("#group-count", units.stream().mapToInt(Unit::size).sum() + " verified groups"))
                .put(count("#unit-totals article", units.size()));
        for (var unit : units) {
            String id = RoutedDashboardReport.unitId(unit.name());
            steps.put(text("#counts-" + id, unit.countsText()))
                    .put(text("#net-" + id, unit.deltaText(unit.net(), true)))
                    .put(text("#absolute-" + id, unit.deltaText(unit.absolute(), false)));
        }
        return steps.put(count("a[href=\"matched-comparison.csv\"]", 1))
                .put(count("a[href=\"import-receipt.json\"]", 1));
    }

    @Override public JSONObject artifact(JSONObject file, JSONArray checks) {
        verifyBrowser(checks);
        var proof = new JSONObject().put("passed", true).put("import", receipt)
                .put("totals", new JSONArray(units.stream().map(Unit::evidence).toList()))
                .put("browserChecks", checks.length());
        return new JSONObject().put("title", TITLE)
                .put("sources", new JSONArray().put("matched-comparison-csv:sha256:" + receipt.getString("sha256"))
                        .put("matched-comparison-receipt:sha256:" + MeasurementStore.hash(MapCheckpoint.canonical(receipt))))
                .put("files", new JSONObject().put("index.html", html).put("matched-comparison.csv", csv)
                        .put("import-receipt.json", MapCheckpoint.canonical(receipt) + "\n")
                        .put("verification.json", MapCheckpoint.canonical(proof) + "\n"));
    }
}
