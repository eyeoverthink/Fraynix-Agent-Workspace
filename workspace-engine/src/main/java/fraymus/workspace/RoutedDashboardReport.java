package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/** Exact decimal reference for the unit-separated browser dashboard. */
final class RoutedDashboardReport {
    private static final String TEMPLATE = FrayConversion.loadHtml("routing-dashboard.html");
    static final String TITLE = "Verified routing dashboard";
    private static final List<String> UNITS = List.of("°F", "mm");

    private record Row(String number, String classification, String lesson, String input, String inputUnit,
                       String value, String unit) {
        String text() {
            return number + " · " + classification + " · " + input + " " + inputUnit + " → " + value + " " + unit
                    + " · " + lesson;
        }
    }

    private record Group(String unit, int count, String min, String max, String mean) {
        String text() {
            return count + (count == 1 ? " row" : " rows") + " · Min " + min + " " + unit + " · Max " + max + " " + unit
                    + " · Mean " + mean + " " + unit;
        }

        JSONObject evidence() {
            return new JSONObject().put("unit", unit).put("count", count).put("min", min).put("max", max).put("mean", mean);
        }
    }

    private final String csv, html;
    private final List<Row> rows;
    private final List<Group> groups;

    RoutedDashboardReport(String csv) {
        this.csv = csv;
        var table = MeasurementStore.csv(csv, 1);
        var fields = RoutedCsvReport.FIELDS;
        int offset = table.getFirst().size() - fields.size();
        if (offset < 1 || table.size() > 25) throw new IllegalArgumentException("Use a verified routed CSV");
        String first = table.getFirst().get(offset);
        if (!first.endsWith("source_row")) throw new IllegalArgumentException("The routing CSV is missing its row columns");
        String prefix = first.substring(0, first.length() - "source_row".length());
        if (!prefix.matches("route_(?:[0-9]+_)?")
                || !table.getFirst().subList(offset, table.getFirst().size()).equals(fields.stream().map(field -> prefix + field).toList())
                || table.stream().anyMatch(row -> row.size() != offset + fields.size()))
            throw new IllegalArgumentException("The routing CSV schema changed");
        var readings = new ArrayList<Row>();
        for (var raw : table.subList(1, table.size())) {
            var row = raw.subList(offset, raw.size());
            if (!row.get(0).equals(Integer.toString(readings.size() + 2)) || !UNITS.contains(row.get(7)))
                throw new IllegalArgumentException("The routing CSV row order or output unit changed");
            readings.add(new Row(row.get(0), row.get(1), row.get(2), row.get(4), row.get(5), row.get(6), row.get(7)));
        }
        rows = List.copyOf(readings);
        groups = UNITS.stream().map(this::summarize).filter(group -> group.count() > 0).toList();
        html = TEMPLATE.replace("@CSV_BASE64@", Base64.getEncoder().encodeToString(csv.getBytes(StandardCharsets.UTF_8)));
    }

    String html() { return html; }
    String htmlSha256() { return MeasurementStore.hash(html); }

    private Group summarize(String unit) {
        var values = rows.stream().filter(row -> unit.equals(row.unit())).map(row -> new BigDecimal(row.value())).toList();
        if (values.isEmpty()) return new Group(unit, 0, "", "", "");
        var sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Group(unit, values.size(), plain(values.stream().min(BigDecimal::compareTo).orElseThrow()),
                plain(values.stream().max(BigDecimal::compareTo).orElseThrow()),
                plain(sum.divide(BigDecimal.valueOf(values.size()), 5, RoundingMode.HALF_UP)));
    }

    private static String plain(BigDecimal value) { return value.stripTrailingZeros().toPlainString(); }

    private String rowText(String unit) {
        return String.join("\n", rows.stream().filter(row -> unit == null || row.unit().equals(unit)).map(Row::text).toList());
    }

    static String unitId(String unit) { return "°F".equals(unit) ? "fahrenheit" : "millimetres"; }

    JSONArray steps() {
        var steps = new JSONArray().put(text("h1", TITLE))
                .put(text("#dashboard-count", rows.size() + (rows.size() == 1 ? " verified row" : " verified rows")
                        + " · " + groups.size() + (groups.size() == 1 ? " output unit" : " output units")))
                .put(text("#routed-rows", rowText(null)))
                .put(new JSONObject().put("selector", "#routed-rows li").put("expectCount", rows.size()));
        for (var group : groups) {
            String id = unitId(group.unit());
            steps.put(text("#summary-" + id, group.text()))
                    .put(new JSONObject().put("selector", "#filter-" + id).put("action", "click").put("expectCount", 1))
                    .put(text("#routed-rows", rowText(group.unit())))
                    .put(new JSONObject().put("selector", "#routed-rows li").put("expectCount", group.count()));
        }
        return steps.put(new JSONObject().put("selector", "#filter-all").put("action", "click").put("expectCount", 1))
                .put(text("#routed-rows", rowText(null)))
                .put(new JSONObject().put("selector", "a[href=\"routed-results.csv\"]").put("expectCount", 1));
    }

    private static JSONObject text(String selector, String content) {
        return new JSONObject().put("selector", selector).put("expectText", content);
    }

    JSONObject artifact(JSONObject file, JSONArray checks) {
        var steps = steps();
        if (checks.length() != steps.length()) throw new IllegalStateException("The routed dashboard browser checks are incomplete");
        for (int i = 0; i < steps.length(); i++) FrayConversion.verifyStep(steps.getJSONObject(i), checks.getJSONObject(i), i);
        var proof = new JSONObject().put("passed", true).put("exportId", file.getString("exportId"))
                .put("csvSha256", file.getString("sha256")).put("source", file.getJSONObject("source"))
                .put("groups", new JSONArray(groups.stream().map(Group::evidence).toList()))
                .put("rowOrder", new JSONArray(rows.stream().map(Row::number).toList()))
                .put("browserChecks", checks.length());
        return new JSONObject().put("title", TITLE)
                .put("sources", new JSONArray().put("verified-routing-csv:sha256:" + file.getString("sha256"))
                        .put("routing-decision:sha256:" + file.getJSONObject("source").getString("decisionSha256")))
                .put("files", new JSONObject().put("index.html", html).put("routed-results.csv", csv)
                        .put("verification.json", MapCheckpoint.canonical(proof) + "\n"));
    }
}
