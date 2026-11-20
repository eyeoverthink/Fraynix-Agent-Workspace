
package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static fraymus.workspace.RoutedComparisonReport.count;
import static fraymus.workspace.RoutedComparisonReport.evidenceSource;
import static fraymus.workspace.RoutedComparisonReport.sourceText;
import static fraymus.workspace.RoutedComparisonReport.text;

/** Matches exact original keys within each verified output unit. */
final class RoutedKeyComparisonReport implements RoutedReport {
    private static final String TITLE = "Verified routed row comparison";
    private static final String TEMPLATE = FrayConversion.loadHtml("routing-key-comparison.html");

    private enum Status {
        MATCHED("Matched"), ADDED("Added"), MISSING("Missing"), DUPLICATE("Duplicate");
        final String label;
        Status(String label) { this.label = label; }
    }

    private record Key(String value, String unit) {}

    private record Reading(int row, String value) {
        String label() { return "#" + row + ": " + value; }
        JSONObject evidence() { return new JSONObject().put("row", row).put("value", value); }
    }

    private record Outcome(Key key, List<Reading> earlier, List<Reading> later, Status status, String delta) {
        Outcome(Key key, List<Reading> earlier, List<Reading> later) {
            this(key, earlier, later, status(earlier, later), difference(earlier, later));
        }

        private static Status status(List<Reading> earlier, List<Reading> later) {
            if (earlier.size() > 1 || later.size() > 1) return Status.DUPLICATE;
            if (earlier.isEmpty()) return Status.ADDED;
            if (later.isEmpty()) return Status.MISSING;
            return Status.MATCHED;
        }

        private static String difference(List<Reading> earlier, List<Reading> later) {
            if (earlier.size() != 1 || later.size() != 1) return null;
            return RoutedDashboardReport.plain(new BigDecimal(later.getFirst().value())
                    .subtract(new BigDecimal(earlier.getFirst().value())));
        }

        private static String labels(List<Reading> readings) {
            return readings.isEmpty() ? "—" : readings.stream().map(Reading::label).collect(Collectors.joining(" · "));
        }

        String display() {
            String change = delta == null ? "—" : new BigDecimal(delta).signum() > 0 ? "+" + delta : delta;
            return String.join("\t", visibleKey(key.value()), key.unit(), status.label, labels(earlier), labels(later), change);
        }

        JSONObject evidence() {
            return new JSONObject().put("key", key.value()).put("unit", key.unit()).put("status", status.label)
                    .put("earlier", new JSONArray(earlier.stream().map(Reading::evidence).toList()))
                    .put("later", new JSONArray(later.stream().map(Reading::evidence).toList()))
                    .put("delta", delta == null ? JSONObject.NULL : delta);
        }
    }

    private record Source(JSONObject file, String csv, int rows, Map<Key, List<Reading>> groups) {
        static Source read(JSONObject file, String csv, String keyColumn) {
            var table = RoutedDashboardReport.checkedTable(csv);
            var header = table.getFirst();
            int offset = header.size() - RoutedCsvReport.FIELDS.size();
            var originals = header.subList(0, offset);
            int index = originals.indexOf(keyColumn);
            if (index < 0 || index != originals.lastIndexOf(keyColumn))
                throw new IllegalArgumentException("Choose a unique original key column present in both exports");
            var groups = new LinkedHashMap<Key, List<Reading>>();
            for (int i = 1; i < table.size(); i++) {
                var row = table.get(i);
                String key = row.get(index);
                if (key.isBlank())
                    throw new IllegalArgumentException("The key column has an empty value at source row " + (i + 1));
                groups.computeIfAbsent(new Key(key, row.getLast()), ignored -> new ArrayList<>())
                        .add(new Reading(i + 1, row.get(offset + 6)));
            }
            return new Source(file, csv, table.size() - 1, groups);
        }

        List<Reading> readings(Key key) { return groups.getOrDefault(key, List.of()); }
        JSONObject evidence() { return evidenceSource(file, rows); }
    }

    private final String keyColumn, html;
    private final Source earlier, later;
    private final List<Outcome> outcomes;

    RoutedKeyComparisonReport(JSONObject baseline, String earlierCsv, JSONObject file, String laterCsv, String keyColumn) {
        this.keyColumn = keyColumn;
        earlier = Source.read(baseline, earlierCsv, keyColumn);
        later = Source.read(file, laterCsv, keyColumn);
        var keys = new LinkedHashSet<>(earlier.groups().keySet());
        keys.addAll(later.groups().keySet());
        outcomes = keys.stream().map(key -> new Outcome(key, earlier.readings(key), later.readings(key))).toList();
        html = TEMPLATE.replace("@KEY_COLUMN@", Base64.getEncoder().encodeToString(keyColumn.getBytes(StandardCharsets.UTF_8)))
                .replace("@EARLIER_CSV@", baseline.getString("base64"))
                .replace("@LATER_CSV@", file.getString("base64"));
    }

    static String visibleKey(String key) {
        return "\"" + key.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\t", "\\t").replace("\r", "\\r").replace("\n", "\\n") + "\"";
    }

    private long countOf(Status status) { return outcomes.stream().filter(row -> row.status() == status).count(); }

    private String summary() {
        return String.join(" · ", java.util.Arrays.stream(Status.values())
                .map(status -> status.label + " " + countOf(status)).toList());
    }

    @Override public String html() { return html; }

    @Override public JSONArray steps() {
        return new JSONArray().put(text("h1", TITLE)).put(text("#key-column", "Key column · " + visibleKey(keyColumn)))
                .put(text("#source-earlier", sourceText("Earlier export", earlier.rows(), earlier.file())))
                .put(text("#source-later", sourceText("Later export", later.rows(), later.file())))
                .put(text("#row-counts", summary()))
                .put(count("#matched-rows tr", outcomes.size()))
                .put(text("#matched-rows", outcomes.stream().map(Outcome::display).collect(Collectors.joining("\n"))))
                .put(count("a[href=\"earlier-routed-results.csv\"]", 1))
                .put(count("a[href=\"later-routed-results.csv\"]", 1));
    }

    private JSONArray outcomeEvidence() {
        return new JSONArray(outcomes.stream().map(Outcome::evidence).toList());
    }

    JSONObject exportCsv() { return RoutedKeyCsv.write(outcomeEvidence()); }

    @Override public JSONObject artifact(JSONObject file, JSONArray checks) {
        verifyBrowser(checks);
        var totals = new JSONObject();
        for (Status status : Status.values()) totals.put(status.label, countOf(status));
        var proof = new JSONObject().put("passed", true).put("keyColumn", keyColumn)
                .put("earlier", earlier.evidence()).put("later", later.evidence()).put("counts", totals)
                .put("outcomes", outcomeEvidence())
                .put("browserChecks", checks.length());
        return new JSONObject().put("title", TITLE).put("sources", new JSONArray()
                        .put("earlier-routing-csv:sha256:" + earlier.file().getString("sha256"))
                        .put("later-routing-csv:sha256:" + file.getString("sha256")))
                .put("files", new JSONObject().put("index.html", html).put("earlier-routed-results.csv", earlier.csv())
                        .put("later-routed-results.csv", later.csv()).put("verification.json", MapCheckpoint.canonical(proof) + "\n"));
    }
}
