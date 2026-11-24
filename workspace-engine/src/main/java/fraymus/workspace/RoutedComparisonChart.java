package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static fraymus.workspace.RoutedComparisonReport.count;
import static fraymus.workspace.RoutedComparisonReport.text;
import static fraymus.workspace.RoutedComparisonTotals.encode;
import static fraymus.workspace.SpatialReport.escape;

/** A fixed selection of original matched rows with a separate scale for each unit. */
final class RoutedComparisonChart implements RoutedReport {
    private static final String TEMPLATE = FrayConversion.loadHtml("routing-named-chart.html");
    private static final List<String> UNITS = List.of("°F", "mm");
    private static final String EMPTY = "No matched rows satisfy this selection.";
    private static final String PLACEHOLDER = "Select a row to inspect the earlier and later readings.";

    private record Row(String key, String unit, BigDecimal delta, JSONArray earlier, JSONArray later) {
        String label() { return RoutedKeyComparisonReport.visibleKey(key); }
        String amount() { return signed(delta, unit); }
        String side() { return delta.signum() < 0 ? "loss" : delta.signum() > 0 ? "gain" : "steady"; }
        int width(BigDecimal scale) {
            return scale.signum() == 0 ? 0
                    : delta.abs().movePointRight(4).divide(scale, 0, RoundingMode.DOWN).intValueExact();
        }
        String previous() { return RoutedComparisonExplorer.readingsText(earlier.toString()); }
        String current() { return RoutedComparisonExplorer.readingsText(later.toString()); }
        JSONObject data(int width) {
            return new JSONObject().put("key", key).put("unit", unit).put("delta", RoutedDashboardReport.plain(delta))
                    .put("earlier", earlier).put("later", later).put("direction", side()).put("widthBasisPoints", width);
        }
    }

    private final RoutedDashboard.VerifiedSummary source;
    private final JSONObject proof;
    private final List<Row> ordered = new ArrayList<>();
    private final String html, title, selection, auditHash;
    private final int lanes;

    RoutedComparisonChart(RoutedDashboard.VerifiedSummary source, JSONObject request) {
        this.source = source;
        var named = request.getJSONObject("named");
        var criteria = request.getJSONObject("selection");
        String name = named.getString("name");
        title = name + " · comparison chart";
        String unit = criteria.getString("unit"), direction = criteria.getString("direction");
        var keys = criteria.getJSONArray("keys").toList();
        String mode = switch (direction) {
            case "gain" -> "Gains";
            case "loss" -> "Losses";
            case "difference" -> "Gains and losses";
            default -> "Matched changes";
        };
        selection = mode + " · " + (unit.equals("all") ? "Each unit on its own scale" : unit)
                + (keys.isEmpty() ? " · All keys" : " · " + keys.size() + (keys.size() == 1 ? " selected key" : " selected keys"));

        var selected = selectedRows(source, criteria);

        var groups = new JSONArray();
        var chart = new StringBuilder();
        var audit = new StringBuilder();
        var inspector = new JSONArray();
        var groupRows = new ArrayList<List<Row>>();
        for (String supported : UNITS) {
            var rows = selected.stream().filter(row -> row.unit().equals(supported)).toList();
            if (!rows.isEmpty()) groupRows.add(rows);
        }
        for (var rows : groupRows) {
            String label = rows.getFirst().unit();
            var maximum = rows.stream().map(row -> row.delta().abs()).max(BigDecimal::compareTo).orElseThrow();
            String low = signed(maximum.negate(), label), high = signed(maximum, label);
            frame(audit, List.of(label, low, high));
            chart.append("<section class=\"lane\" data-unit=\"").append(escape(label)).append("\"><h2>")
                    .append(label).append("</h2><div class=\"axis\"><span class=\"axis-low\">").append(low)
                    .append("</span><span>0</span><span class=\"axis-high\">").append(high).append("</span></div>");
            var data = new JSONArray();
            for (var row : rows) {
                int width = row.width(maximum);
                String bar = BigDecimal.valueOf(width).divide(BigDecimal.valueOf(200)).stripTrailingZeros().toPlainString() + "%";
                chart.append("<button type=\"button\" class=\"mark\" id=\"row-").append(ordered.size())
                        .append("\" data-direction=\"").append(row.side()).append("\" aria-controls=\"details\" aria-pressed=\"false\">")
                        .append("<span class=\"key\">").append(escape(row.label())).append("</span><strong class=\"delta\">")
                        .append(row.amount()).append("</strong><span class=\"track\" aria-hidden=\"true\"><span style=\"--bar:")
                        .append(bar).append("\"></span></span></button>");
                ordered.add(row);
                data.put(row.data(width));
                inspector.put(new JSONObject().put("title", row.label() + " · " + row.amount())
                        .put("earlier", row.previous()).put("later", row.current()));
                frame(audit, List.of(row.label(), row.amount(), row.side(), bar));
            }
            chart.append("</section>");
            groups.put(new JSONObject().put("unit", label).put("maximumAbsoluteDelta", RoutedDashboardReport.plain(maximum))
                    .put("rows", data));
        }
        lanes = groups.length();
        auditHash = MeasurementStore.hash(audit.toString());
        proof = new JSONObject().put("passed", true).put("name", name).put("source", source.binding())
                .put("comparisonReceipt", source.receipt()).put("selection", criteria).put("groups", groups)
                .put("rows", ordered.size()).put("browserChartSha256", auditHash);
        html = TEMPLATE.replace("@TITLE@", escape(title)).replace("@SELECTION@", escape(selection))
                .replace("@COUNT@", countText())
                .replace("@DESCRIPTION@", escape(ordered.isEmpty() ? EMPTY : PLACEHOLDER))
                .replace("@EXPORT@", source.receipt().getString("id")).replace("@CSV_SHA@", source.receipt().getString("sha256"))
                .replace("@DETAILS@", encode(inspector.toString())).replace("@CHART@", chart.toString());
    }

    static void validateSelection(RoutedDashboard.VerifiedSummary source, JSONObject criteria) {
        selectedRows(source, criteria);
    }

    private static List<Row> selectedRows(RoutedDashboard.VerifiedSummary source, JSONObject criteria) {
        String unit = criteria.getString("unit"), direction = criteria.getString("direction");
        var keys = criteria.getJSONArray("keys").toList();
        var csv = MeasurementStore.csv(source.totals().csv(), 1, 32_000);
        var available = csv.subList(1, csv.size()).stream()
                .filter(row -> unit.equals("all") || unit.equals(row.get(1))).toList();
        var known = available.stream().map(RoutedComparisonChart::key).collect(Collectors.toSet());
        for (Object key : keys) {
            if (!known.contains(key)) throw new IllegalArgumentException("No original row for “"
                    + RoutedKeyComparisonReport.visibleKey((String) key) + "” in the requested units.");
        }
        return available.stream().filter(row -> row.get(2).equals("Matched"))
                .filter(row -> keys.isEmpty() || keys.contains(key(row))).map(RoutedComparisonChart::row)
                .filter(row -> direction.equals("all") || direction.equals("difference") && row.delta().signum() != 0
                        || direction.equals(row.side())).toList();
    }

    private static Row row(List<String> row) {
        return new Row(key(row), row.get(1), new BigDecimal(row.get(5)), new JSONArray(row.get(3)), new JSONArray(row.get(4)));
    }

    private static String key(List<String> row) { return new JSONArray("[" + row.getFirst() + "]").getString(0); }

    private static String signed(BigDecimal value, String unit) {
        return (value.signum() > 0 ? "+" : "") + RoutedDashboardReport.plain(value) + " " + unit;
    }

    private static void frame(StringBuilder target, List<String> fields) {
        for (String value : fields) target.append(value.length()).append(':').append(value);
    }

    private String countText() { return ordered.size() + (ordered.size() == 1 ? " matched row" : " matched rows"); }
    @Override public String html() { return html; }

    @Override public JSONArray steps() {
        var steps = new JSONArray().put(text("h1", title)).put(text("#query", selection)).put(text("#chart-count", countText()))
                .put(text("#chart-export", source.receipt().getString("id"))).put(text("#chart-csv-sha", source.receipt().getString("sha256")))
                .put(count(".lane", lanes)).put(count(".mark", ordered.size())).put(text("#chart-audit", auditHash))
                .put(count("a[download]", 3)).put(text("#detail-title", ordered.isEmpty() ? EMPTY : PLACEHOLDER));
        if (!ordered.isEmpty()) {
            checkRow(steps, 0);
            if (ordered.size() > 1) checkRow(steps, ordered.size() - 1);
        }
        return steps;
    }

    private void checkRow(JSONArray steps, int index) {
        var row = ordered.get(index);
        steps.put(count("#row-" + index, 1).put("action", "click"))
                .put(text("#detail-title", row.label() + " · " + row.amount()))
                .put(text("#detail-earlier", row.previous()))
                .put(text("#detail-later", row.current()))
                .put(count(".mark[aria-pressed=\"true\"]#row-" + index, 1));
    }

    JSONObject summary() {
        return new JSONObject().put("name", proof.getString("name")).put("rows", ordered.size())
                .put("units", new JSONArray(ordered.stream().map(Row::unit).distinct().toList()))
                .put("selection", proof.getJSONObject("selection")).put("browserChartSha256", auditHash);
    }

    @Override public JSONObject artifact(JSONObject file, JSONArray checks) {
        verifyBrowser(checks);
        var record = WorldFrames.copy(proof).put("browserChecks", checks.length());
        return new JSONObject().put("title", title)
                .put("sources", new JSONArray().put("matched-comparison-csv:sha256:" + source.receipt().getString("sha256"))
                        .put("comparison-summary:sha256:" + source.artifact().getString("sha256")))
                .put("files", new JSONObject().put("index.html", html).put("matched-comparison.csv", source.totals().csv())
                        .put("chart-data.json", MapCheckpoint.canonical(record) + "\n")
                        .put("summary-proof.json", MapCheckpoint.canonical(source.proof()) + "\n"));
    }
}
