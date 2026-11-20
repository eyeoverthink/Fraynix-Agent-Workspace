package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Same-unit comparisons between two independently verified routing exports. */
final class RoutedComparisonReport implements RoutedReport {
    static final String TITLE = "Verified routing comparison";
    private static final String TEMPLATE = FrayConversion.loadHtml("routing-comparison.html");
    private static final String NO_MATCH = "Mean change unavailable · no matching unit on both sides";

    private record Comparison(String unit, RoutedDashboardReport.Group earlier, RoutedDashboardReport.Group later) {
        String change() {
            if (earlier == null || later == null) return null;
            var a = BigDecimal.valueOf(earlier.count());
            var b = BigDecimal.valueOf(later.count());
            var numerator = later.sum().multiply(a).subtract(earlier.sum().multiply(b));
            return RoutedDashboardReport.plain(numerator.divide(a.multiply(b), 5, RoundingMode.HALF_UP));
        }

        String changeText() {
            String value = change();
            return value == null ? NO_MATCH : "Mean change · " + (new BigDecimal(value).signum() > 0 ? "+" : "") + value + " " + unit;
        }

        JSONObject evidence() {
            return new JSONObject().put("unit", unit).put("earlier", earlier == null ? JSONObject.NULL : earlier.evidence())
                    .put("later", later == null ? JSONObject.NULL : later.evidence())
                    .put("meanChange", change() == null ? JSONObject.NULL : change());
        }
    }

    private final JSONObject baseline, newer;
    private final String earlierCsv, laterCsv, html;
    private final int earlierRows, laterRows;
    private final List<Comparison> comparisons;

    RoutedComparisonReport(JSONObject baseline, String earlierCsv, JSONObject newer, String laterCsv) {
        this.baseline = baseline;
        this.newer = newer;
        this.earlierCsv = earlierCsv;
        this.laterCsv = laterCsv;
        var earlier = new RoutedDashboardReport(earlierCsv);
        var later = new RoutedDashboardReport(laterCsv);
        earlierRows = earlier.rowCount();
        laterRows = later.rowCount();
        comparisons = List.of("°F", "mm").stream().map(unit -> new Comparison(unit, group(earlier, unit), group(later, unit)))
                .filter(comparison -> comparison.earlier() != null || comparison.later() != null).toList();
        html = TEMPLATE.replace("@EARLIER_CSV@", baseline.getString("base64"))
                .replace("@LATER_CSV@", newer.getString("base64"));
    }

    @Override public String html() { return html; }

    private static RoutedDashboardReport.Group group(RoutedDashboardReport report, String unit) {
        return report.groups().stream().filter(group -> unit.equals(group.unit())).findFirst().orElse(null);
    }

    private static String groupText(RoutedDashboardReport.Group group, String unit) {
        return group == null ? "No rows in " + unit : group.text();
    }

    static String sourceText(String label, int rows, JSONObject file) {
        return label + " · " + rows + (rows == 1 ? " row" : " rows") + " · " + file.getString("sha256").substring(0, 12);
    }

    @Override public JSONArray steps() {
        var plan = new JSONArray().put(text("h1", TITLE))
                .put(text("#source-earlier", sourceText("Earlier export", earlierRows, baseline)))
                .put(text("#source-later", sourceText("Later export", laterRows, newer)))
                .put(count("#comparisons article", comparisons.size()));
        for (var comparison : comparisons) {
            String id = RoutedDashboardReport.unitId(comparison.unit());
            plan.put(text("#earlier-" + id, groupText(comparison.earlier(), comparison.unit())))
                    .put(text("#later-" + id, groupText(comparison.later(), comparison.unit())))
                    .put(text("#change-" + id, comparison.changeText()));
        }
        return plan.put(count("a[href=\"earlier-routed-results.csv\"]", 1))
                .put(count("a[href=\"later-routed-results.csv\"]", 1));
    }

    static JSONObject count(String selector, int value) {
        return new JSONObject().put("selector", selector).put("expectCount", value);
    }

    static JSONObject text(String selector, String value) {
        return new JSONObject().put("selector", selector).put("expectText", value);
    }

    static JSONObject evidenceSource(JSONObject file, int rows) {
        return new JSONObject().put("exportId", file.getString("exportId")).put("csvSha256", file.getString("sha256"))
                .put("rows", rows).put("routing", file.getJSONObject("source"));
    }

    @Override public JSONObject artifact(JSONObject file, JSONArray checks) {
        verifyBrowser(checks);
        var evidence = new JSONObject().put("passed", true).put("earlier", evidenceSource(baseline, earlierRows))
                .put("later", evidenceSource(file, laterRows)).put("browserChecks", checks.length())
                .put("comparisons", new JSONArray(comparisons.stream().map(Comparison::evidence).toList()));
        return new JSONObject().put("title", TITLE).put("sources", new JSONArray()
                .put("earlier-routing-csv:sha256:" + baseline.getString("sha256"))
                .put("later-routing-csv:sha256:" + file.getString("sha256")))
                .put("files", new JSONObject().put("index.html", html).put("earlier-routed-results.csv", earlierCsv)
                        .put("later-routed-results.csv", laterCsv).put("verification.json", MapCheckpoint.canonical(evidence) + "\n"));
    }
}
