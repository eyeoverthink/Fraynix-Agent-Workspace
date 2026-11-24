package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static fraymus.workspace.RoutedComparisonReport.count;
import static fraymus.workspace.RoutedComparisonReport.text;
import static fraymus.workspace.SpatialReport.escape;

/** Renders and verifies a descriptive briefing from the exact rows of a checked chart. */
final class RoutedChartBriefing implements RoutedReport {
    private static final String TEMPLATE = FrayConversion.loadHtml("routing-chart-briefing.html");
    private static final String EMPTY = "This chart selection contains no matched rows.";
    private static final String SCOPE = "This briefing describes the selected comparison. Causes require separate evidence.";

    private record Reading(int row, String value) {
        static Reading of(JSONArray readings) {
            if (readings.length() != 1) throw new IllegalStateException("A briefing needs one reading on each side of a matched row.");
            var reading = readings.getJSONObject(0);
            int row = reading.getInt("row");
            if (row < 2) throw new IllegalStateException("A cited row must follow the original header.");
            String value = reading.getString("value");
            new BigDecimal(value);
            return new Reading(row, value);
        }

        String citation(String unit) { return "Row " + row + " · " + value + " " + unit; }
        JSONObject valueRecord() { return new JSONObject().put("row", row).put("value", value); }
    }

    private record Claim(String key, String unit, BigDecimal delta, Reading earlier, Reading later) {
        static Claim checked(JSONObject row, String unit) {
            var earlier = Reading.of(row.getJSONArray("earlier"));
            var later = Reading.of(row.getJSONArray("later"));
            var delta = new BigDecimal(later.value()).subtract(new BigDecimal(earlier.value()));
            if (!unit.equals(row.getString("unit")) || delta.compareTo(new BigDecimal(row.getString("delta"))) != 0) {
                throw new IllegalStateException("The chart change differs from the two cited readings.");
            }
            return new Claim(row.getString("key"), unit, delta, earlier, later);
        }

        String sentence() {
            String label = RoutedKeyComparisonReport.visibleKey(key);
            if (delta.signum() == 0) return label + " stayed at " + later.value() + " " + unit + ".";
            String verb = delta.signum() > 0 ? " increased" : " decreased";
            return label + verb + " from " + earlier.value() + " " + unit + " to " + later.value() + " " + unit
                    + " (" + signed(delta, unit) + ").";
        }

        String citation() { return "Earlier row " + earlier.row() + " · Later row " + later.row(); }

        JSONObject evidence() {
            return new JSONObject().put("key", key).put("unit", unit).put("sentence", sentence())
                    .put("delta", RoutedDashboardReport.plain(delta))
                    .put("earlier", earlier.valueRecord()).put("later", later.valueRecord());
        }
    }

    private final JSONObject binding, chartFiles;
    private final String title, html, browserHash;
    private final JSONArray units = new JSONArray();
    private final List<Claim> claims = new ArrayList<>();

    RoutedChartBriefing(JSONObject chartArtifact, JSONObject chartRef) {
        chartFiles = chartArtifact.getJSONObject("contents");
        var chart = new JSONObject(chartFiles.getString("chart-data.json"));
        var original = chart.getJSONObject("source");
        String sourceHash = original.getString("sourceSha256");
        String csv = chartFiles.getString("matched-comparison.csv");
        if (!sourceHash.equals(MeasurementStore.hash(csv))) throw new IllegalStateException("The chart's original comparison CSV changed.");
        title = chart.getString("name") + " · change briefing";
        binding = new JSONObject().put("name", chart.getString("name")).put("chart", chartRef)
                .put("source", original).put("selection", chart.getJSONObject("selection"))
                .put("chartDataSha256", MeasurementStore.hash(chartFiles.getString("chart-data.json")));

        var content = new StringBuilder();
        var digest = new StringBuilder();
        var seen = new HashSet<String>();
        for (Object entry : chart.getJSONArray("groups")) {
            var group = (JSONObject) entry;
            String unit = group.getString("unit");
            if ((!unit.equals("°F") && !unit.equals("mm")) || !seen.add(unit)) {
                throw new IllegalStateException("The briefing needs one section per original chart unit.");
            }
            var list = new ArrayList<Claim>();
            for (Object row : group.getJSONArray("rows")) list.add(Claim.checked((JSONObject) row, unit));
            var net = list.stream().map(Claim::delta).reduce(BigDecimal.ZERO, BigDecimal::add);
            long gains = list.stream().filter(claim -> claim.delta().signum() > 0).count();
            long losses = list.stream().filter(claim -> claim.delta().signum() < 0).count();
            long steady = list.size() - gains - losses;
            String tally = list.size() + (list.size() == 1 ? " matched row" : " matched rows");
            String netText = "Net " + signed(net, unit);
            String directions = gains + (gains == 1 ? " gain" : " gains") + " · "
                    + losses + (losses == 1 ? " loss" : " losses") + " · " + steady + " steady";
            units.put(new JSONObject().put("unit", unit).put("rows", list.size()).put("gains", gains)
                    .put("losses", losses).put("steady", steady).put("netDelta", RoutedDashboardReport.plain(net)));
            content.append("<section class=\"unit\"><div class=\"unit-heading\"><h2>").append(field(digest, unit))
                    .append("</h2><strong>").append(field(digest, netText)).append("</strong></div><p class=\"unit-counts\">")
                    .append(field(digest, tally + " · " + directions)).append("</p><ol class=\"claims\">");
            for (var claim : list) appendClaim(content, digest, claim);
            content.append("</ol></section>");
        }
        if (claims.size() != chart.getInt("rows")) throw new IllegalStateException("The briefing does not cover the full chart selection.");
        browserHash = MeasurementStore.hash(digest.toString());
        html = TEMPLATE.replace("@TITLE@", escape(title)).replace("@SCOPE@", escape(SCOPE))
                .replace("@COUNT@", countText()).replace("@EMPTY@", claims.isEmpty() ? "<p id=\"empty\">" + EMPTY + "</p>" : "")
                .replace("@EXPORT@", original.getString("exportId")).replace("@SOURCE_SHA@", sourceHash)
                .replace("@CHART_ID@", chartRef.getString("id")).replace("@SECTIONS@", content.toString());
    }

    private void appendClaim(StringBuilder content, StringBuilder digest, Claim claim) {
        int index = claims.size();
        claims.add(claim);
        content.append("<li><p class=\"claim\">").append(field(digest, claim.sentence()))
                .append("</p><div class=\"citation\" id=\"citation-").append(index).append("\">")
                .append("<button type=\"button\" class=\"citation-toggle\" aria-expanded=\"false\" aria-controls=\"readings-").append(index)
                .append("\">").append(field(digest, claim.citation())).append("</button>")
                .append("<dl id=\"readings-").append(index).append("\" hidden>")
                .append("<div><dt>Earlier reading</dt><dd class=\"earlier\">").append(field(digest, claim.earlier().citation(claim.unit())))
                .append("</dd></div><div><dt>Later reading</dt><dd class=\"later\">").append(field(digest, claim.later().citation(claim.unit())))
                .append("</dd></div></dl></div></li>");
    }

    private static String field(StringBuilder digest, String text) {
        digest.append(text.length()).append(':').append(text);
        return "<span data-briefing-field>" + escape(text) + "</span>";
    }

    private static String signed(BigDecimal value, String unit) {
        return (value.signum() > 0 ? "+" : "") + RoutedDashboardReport.plain(value) + " " + unit;
    }

    private String countText() { return claims.size() + (claims.size() == 1 ? " cited change" : " cited changes"); }

    JSONObject summary() {
        return new JSONObject().put("name", binding.getString("name")).put("claims", claims.size())
                .put("units", units).put("binding", binding).put("browserBriefingSha256", browserHash);
    }

    @Override public String html() { return html; }

    @Override public JSONArray steps() {
        var source = binding.getJSONObject("source");
        var steps = new JSONArray().put(text("h1", title)).put(text("#briefing-scope", SCOPE))
                .put(text("#briefing-count", countText())).put(text("#original-chart", binding.getJSONObject("chart").getString("id")))
                .put(text("#comparison-export", source.getString("exportId"))).put(text("#comparison-sha", source.getString("sourceSha256")))
                .put(count(".unit", units.length())).put(count(".claim", claims.size())).put(count(".claims .citation", claims.size()))
                .put(text("#briefing-audit", browserHash)).put(count("a[href='named-chart.html']", 1)).put(count("a[download]", 4));
        if (claims.isEmpty()) return steps.put(text("#empty", EMPTY));
        checkCitation(steps, 0);
        if (claims.size() > 1) checkCitation(steps, claims.size() - 1);
        return steps;
    }

    private void checkCitation(JSONArray steps, int index) {
        String selector = "#citation-" + index;
        var claim = claims.get(index);
        steps.put(count(selector + " button", 1).put("action", "click"))
                .put(count(selector + " dl:not([hidden])", 1))
                .put(text(selector + " .earlier", claim.earlier().citation(claim.unit())))
                .put(text(selector + " .later", claim.later().citation(claim.unit())));
    }

    @Override public JSONObject artifact(JSONObject file, JSONArray checks) {
        verifyBrowser(checks);
        var evidence = summary().put("rows", new JSONArray(claims.stream().map(Claim::evidence).toList()))
                .put("evidenceScope", "descriptive-comparison").put("browserChecks", checks.length());
        var contents = WorldFrames.copy(chartFiles).put("named-chart.html", chartFiles.getString("index.html"))
                .put("index.html", html).put("claim-evidence.json", MapCheckpoint.canonical(evidence) + "\n");
        return new JSONObject().put("title", title).put("files", contents)
                .put("sources", new JSONArray().put("named-chart:sha256:" + binding.getJSONObject("chart").getString("sha256"))
                        .put("matched-comparison-csv:sha256:" + binding.getJSONObject("source").getString("sourceSha256")));
    }
}
