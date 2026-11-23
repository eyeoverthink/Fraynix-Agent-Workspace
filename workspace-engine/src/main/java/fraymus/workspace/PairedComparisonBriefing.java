package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.stream.Collectors;

import static fraymus.workspace.RoutedComparisonReport.count;
import static fraymus.workspace.RoutedComparisonReport.text;
import static fraymus.workspace.RoutedComparisonTotals.encode;

/** An approved order and section selection for two independently checked comparisons. */
final class PairedComparisonBriefing implements RoutedReport {
    private static final String TITLE = "Reviewed two-source briefing";
    private static final String SCRIPT = FrayConversion.loadHtml("routing-paired-briefing.js");
    private static final String TEMPLATE = FrayConversion.loadHtml("routing-comparison-explorer.html");

    private record Chapter(RoutedDashboard.VerifiedSummary summary, List<List<String>> rows, String receipt, String proof) {
        Chapter(RoutedDashboard.VerifiedSummary summary) {
            this(summary, dataRows(summary.totals().csv()), sealed(summary.receipt()), sealed(summary.proof()));
        }

        private static List<List<String>> dataRows(String csv) {
            var table = MeasurementStore.csv(csv, 1, 32_000);
            return table.subList(1, table.size());
        }

        String proofHash() { return MeasurementStore.hash(proof); }

        JSONObject packed() {
            return new JSONObject().put("binding", summary.binding()).put("csv", encode(summary.totals().csv()))
                    .put("receipt", encode(receipt)).put("proof", encode(proof)).put("proofSha256", proofHash());
        }
    }

    private final ComparisonBriefing sectionPlan;
    private final List<Chapter> chapters;
    private final String html;

    PairedComparisonBriefing(String id, List<String> sections, List<RoutedDashboard.VerifiedSummary> sources) {
        sectionPlan = new ComparisonBriefing(id, sections);
        chapters = sources.stream().map(Chapter::new).toList();
        String base = sectionPlan.html(new RoutedComparisonStory(chapters.getFirst().rows()).html(TEMPLATE))
                .replace(ComparisonBriefing.TITLE, TITLE)
                .replace(".filters {gap:", ".source-tabs {display:flex;gap:8px;flex-wrap:wrap;margin-top:12px}\n.filters {gap:")
                .replace("<div class=\"filters\">", """
                        <nav class="source-tabs" aria-label="Briefing source">
                          <button type="button" id="brief-source-1" aria-pressed="true">Source 1</button>
                          <button type="button" id="brief-source-2" aria-pressed="false">Source 2</button>
                        </nav>
                        <div class="filters">""");
        // Keep the shared filters and chart. Replace only the single-source load routine.
        int begin = base.indexOf("async function render() {");
        int end = base.indexOf("const changeMap = ", begin);
        if (begin < 0 || end <= begin) throw new IllegalStateException("The comparison browser template changed");
        var payload = new JSONArray(chapters.stream().map(Chapter::packed).toList());
        html = (base.substring(0, begin) + base.substring(end))
                .replace("render().catch(error=>{", SCRIPT.replace("/*@BRIEFING_SOURCES@*/ []", MapCheckpoint.canonical(payload).replace("<", "\\u003c"))
                        + "\nrender().catch(error=>{");
    }

    @Override public String html() { return html; }

    JSONArray sourceBindings() {
        return new JSONArray(chapters.stream().map(chapter -> chapter.summary().binding()).toList());
    }

    @Override public JSONArray steps() {
        var steps = new JSONArray().put(text("h1", TITLE));
        sectionPlan.checkVisibility(steps);
        addChapter(steps, chapters.getFirst(), 1);
        steps.put(count("#brief-source-2", 1).put("action", "click"));
        addChapter(steps, chapters.getLast(), 2);
        return steps;
    }

    private void addChapter(JSONArray steps, Chapter chapter, int index) {
        var binding = chapter.summary().binding();
        var rows = chapter.rows();
        steps.put(text("#source-id", "Source " + index + " · Export " + binding.getString("exportId")))
                .put(text("#source-hash", "CSV SHA-256 " + binding.getString("sourceSha256")
                        + " · Summary SHA-256 " + chapter.proofHash()))
                .put(text("#visible-count", rows.size() + (rows.size() == 1 ? " group" : " groups")));
        if (sectionPlan.has("totals")) steps.put(text("#visible-totals",
                RoutedComparisonExplorer.formatTotals(RoutedComparisonTotals.totalsFor(rows))));
        if (sectionPlan.has("rows")) steps.put(text("#visible-rows", rows.stream()
                .map(RoutedComparisonExplorer::rowText).collect(Collectors.joining("\n"))));
        if (sectionPlan.has("chart")) new RoutedComparisonStory(rows).checkOpen(steps);
    }

    @Override public JSONObject artifact(JSONObject file, JSONArray checks) {
        verifyBrowser(checks);
        var sources = new JSONArray().put("comparison-briefing-review:" + sectionPlan.id());
        var files = new JSONObject().put("index.html", html);
        var bindings = sourceBindings();
        for (int i = 0; i < chapters.size(); i++) {
            var chapter = chapters.get(i);
            var binding = bindings.getJSONObject(i);
            String prefix = "source-" + (i + 1) + "-";
            files.put(prefix + "comparison.csv", chapter.summary().totals().csv())
                    .put(prefix + "receipt.json", chapter.receipt()).put(prefix + "summary.json", chapter.proof());
            sources.put("matched-comparison-csv:sha256:" + binding.getString("sourceSha256"))
                    .put("matched-comparison-receipt:sha256:" + MeasurementStore.hash(chapter.receipt()))
                    .put("comparison-summary:" + binding.getJSONObject("summary").getString("id") + ":sha256:"
                            + binding.getJSONObject("summary").getString("sha256"));
        }
        var proof = new JSONObject().put("passed", true).put("presentation", "paired-comparison-briefing-v1")
                .put("briefing", sectionPlan.evidence()).put("sources", bindings).put("browserChecks", checks.length());
        return new JSONObject().put("title", TITLE).put("sources", sources)
                .put("files", files.put("verification.json", sealed(proof)));
    }

    private static String sealed(Object value) { return MapCheckpoint.canonical(value) + "\n"; }
}
