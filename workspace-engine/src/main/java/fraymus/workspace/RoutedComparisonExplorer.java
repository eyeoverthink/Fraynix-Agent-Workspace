
package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Comparator;
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
            return formatTotals(totals);
        }

        JSONObject evidence() {
            return new JSONObject().put("unit", filter.unit() == null ? "all" : filter.unit())
                    .put("status", filter.status() == null ? "all" : filter.status())
                    .put("groups", rows.size()).put("totals", new JSONArray(totals.stream().map(RoutedComparisonTotals.Unit::evidence).toList()));
        }
    }

    private final String csv, receiptText, summaryText, bindingText, html;
    private final JSONObject receipt, summaryArtifact, lesson;
    private final List<View> views;
    private final RoutedComparisonStory story;
    private final ComparisonBriefing briefing;

    RoutedComparisonExplorer(String csv, JSONObject receipt, JSONObject summaryArtifact, JSONObject summaryProof,
                             JSONObject lesson) {
        this(csv, receipt, summaryArtifact, summaryProof, lesson, false);
    }

    RoutedComparisonExplorer(String csv, JSONObject receipt, JSONObject summaryArtifact, JSONObject summaryProof,
                             JSONObject lesson, boolean asStory) {
        this(csv, receipt, summaryArtifact, summaryProof, lesson, asStory, null);
    }

    RoutedComparisonExplorer(String csv, JSONObject receipt, JSONObject summaryArtifact, JSONObject summaryProof,
                             JSONObject lesson, boolean asStory, ComparisonBriefing briefing) {
        this.briefing = briefing;
        this.csv = csv;
        this.lesson = lesson;
        this.receipt = receipt;
        this.summaryArtifact = summaryArtifact;
        receiptText = MapCheckpoint.canonical(receipt) + "\n";
        summaryText = MapCheckpoint.canonical(summaryProof) + "\n";
        var binding = new JSONObject().put("artifact", summaryArtifact).put("proofSha256", MeasurementStore.hash(summaryText));
        if (lesson != null) binding.put("lesson", lesson);
        bindingText = MapCheckpoint.canonical(binding);
        var table = MeasurementStore.csv(csv, 1, 32_000);
        List<List<String>> rows = table.subList(1, table.size());
        story = asStory ? new RoutedComparisonStory(rows) : null;
        views = lesson == null && !asStory ? List.of(new Filter(null, null), new Filter("mm", null),
                new Filter("mm", "Duplicate"), new Filter("mm", "Missing"))
                .stream().map(filter -> view(filter, rows)).toList() : reusableViews(rows);
        String template = asStory ? story.html(TEMPLATE) : TEMPLATE;
        if (briefing != null) template = briefing.html(template);
        html = template.replace("@EXPLORER_CSV@", encode(csv)).replace("@EXPLORER_RECEIPT@", encode(receiptText))
                .replace("@EXPLORER_SUMMARY@", encode(summaryText)).replace("@EXPLORER_BINDING@", encode(bindingText));
    }

    private static List<View> reusableViews(List<List<String>> rows) {
        var all = view(new Filter(null, null), rows);
        // Exercise a unit with no comparable rows when one exists, otherwise the most populated one.
        String unit = all.totals().stream()
                .max(Comparator.comparing((RoutedComparisonTotals.Unit result) -> result.matched() == 0)
                        .thenComparingInt(RoutedComparisonTotals.Unit::size)).orElseThrow().name();
        var byUnit = view(new Filter(unit, null), rows);
        var statuses = List.of("Duplicate", "Added", "Missing", "Matched").stream()
                .map(status -> view(new Filter(unit, status), byUnit.rows())).toList();
        var populated = statuses.stream().filter(selection -> !selection.rows().isEmpty()).findFirst().orElseThrow();
        var edge = statuses.stream().filter(selection -> selection.rows().isEmpty()).findFirst()
                .orElseGet(() -> statuses.stream().filter(selection -> selection != populated).findFirst().orElseThrow());
        return List.of(all, byUnit, populated, edge);
    }

    private static View view(Filter filter, List<List<String>> rows) {
        var selected = rows.stream().filter(filter::includes).toList();
        return new View(filter, selected, RoutedComparisonTotals.totalsFor(selected));
    }

    static String formatTotals(List<RoutedComparisonTotals.Unit> totals) {
        if (totals.isEmpty()) return "No matching groups.";
        return totals.stream().map(unit -> unit.name() + " · " + unit.countsText()
                + " · Net " + unit.deltaText(unit.net(), true) + " · Absolute " + unit.deltaText(unit.absolute(), false))
                .collect(Collectors.joining("\n"));
    }

    static String rowText(List<String> row) {
        String key = new JSONArray("[" + row.get(0) + "]").getString(0);
        String delta = row.getLast();
        String shown = delta.isEmpty() ? "—" : delta.startsWith("-") || delta.equals("0") ? delta : "+" + delta;
        return String.join("\t", RoutedKeyComparisonReport.visibleKey(key), row.get(1), row.get(2),
                readingsText(row.get(3)), readingsText(row.get(4)), shown);
    }

    static String readingsText(String json) {
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

    private String title() {
        if (briefing != null) return ComparisonBriefing.TITLE;
        return story == null ? TITLE : RoutedComparisonStory.TITLE;
    }

    @Override public JSONArray steps() {
        var all = views.get(0);
        var steps = new JSONArray().put(text("h1", title()))
                .put(text("#source-id", "Export " + receipt.getString("id")))
                .put(text("#source-hash", "CSV SHA-256 " + receipt.getString("sha256")))
                .put(text("#parent-proof", "Summary SHA-256 " + MeasurementStore.hash(summaryText)));
        if (briefing != null) return briefingSteps(steps, all);
        addView(steps, all);
        if (story != null) return storySteps(steps, all);
        for (String name : List.of("matched-comparison.csv", "import-receipt.json", "summary-proof.json"))
            steps.put(count("a[href=\"" + name + "\"]", 1));
        steps.put(click("mm".equals(views.get(1).filter().unit()) ? "#unit-mm" : "#unit-f"));
        addView(steps, views.get(1));
        steps.put(click(lesson == null ? "#status-duplicate" : statusButton(views.get(2))));
        addView(steps, views.get(2));
        steps.put(click(lesson == null ? "#status-missing" : statusButton(views.get(3))))
                .put(text("#visible-count", views.get(3).countText()));
        steps.put(click("#reset-filters"));
        addView(steps, all);
        return steps;
    }

    private JSONArray briefingSteps(JSONArray steps, View all) {
        briefing.checkSections(steps);
        addBriefingView(steps, all);
        if (briefing.has("chart")) story.checkOpen(steps);
        var unit = views.get(1);
        steps.put(click("mm".equals(unit.filter().unit()) ? "#unit-mm" : "#unit-f"));
        addBriefingView(steps, unit);
        steps.put(click("#reset-filters")).put(text("#visible-count", all.countText()));
        return steps;
    }

    private void addBriefingView(JSONArray steps, View view) {
        steps.put(text("#visible-count", view.countText()));
        if (briefing.has("rows")) steps.put(text("#visible-rows", view.rowsText()));
        if (briefing.has("totals")) steps.put(text("#visible-totals", view.totalsText()));
    }

    private JSONArray storySteps(JSONArray steps, View all) {
        story.checkOpen(steps);
        var unit = views.get(1);
        steps.put(click("mm".equals(unit.filter().unit()) ? "#unit-mm" : "#unit-f"));
        addView(steps, unit);
        steps.put(click("#status-missing"));
        addView(steps, view(new Filter(unit.filter().unit(), "Missing"), all.rows()));
        story.checkUnmatched(steps);
        steps.put(click("#reset-filters")).put(text("#visible-count", all.countText()));
        story.checkReset(steps);
        return steps;
    }

    private static String statusButton(View view) {
        return "button[data-status=\"" + view.filter().status() + "\"]";
    }

    private static JSONObject click(String selector) { return count(selector, 1).put("action", "click"); }

    private static void addView(JSONArray steps, View view) {
        steps.put(text("#visible-count", view.countText())).put(text("#visible-rows", view.rowsText()))
                .put(text("#visible-totals", view.totalsText()));
    }

    private List<View> verifiedViews() {
        if (story == null) return views;
        if (briefing != null) return views.subList(0, 2);
        var all = views.getFirst();
        var unit = views.get(1);
        return List.of(all, unit, view(new Filter(unit.filter().unit(), "Missing"), all.rows()));
    }

    @Override public JSONObject artifact(JSONObject file, JSONArray checks) {
        verifyBrowser(checks);
        var binding = new JSONObject(bindingText);
        var proof = new JSONObject().put("passed", true).put("import", receipt).put("summary", binding)
                .put("views", new JSONArray(verifiedViews().stream().map(View::evidence).toList())).put("browserChecks", checks.length());
        var sources = new JSONArray()
                .put("comparison-summary:" + summaryArtifact.getString("id") + ":sha256:" + summaryArtifact.getString("sha256"))
                .put("matched-comparison-csv:sha256:" + receipt.getString("sha256"))
                .put("matched-comparison-receipt:sha256:" + MeasurementStore.hash(MapCheckpoint.canonical(receipt)));
        if (lesson != null) {
            proof.put("lesson", lesson);
            sources.put("comparison-explorer-lesson:sha256:" + WorldFrames.hash(lesson));
        }
        if (briefing != null) {
            proof.put("presentation", "comparison-briefing-v1").put("briefing", briefing.evidence());
            sources.put("comparison-briefing-review:" + briefing.id());
        } else if (story != null) {
            proof.put("presentation", "change-story-v1");
        }
        return new JSONObject().put("title", title()).put("sources", sources)
                .put("files", new JSONObject().put("index.html", html).put("matched-comparison.csv", csv)
                        .put("import-receipt.json", receiptText).put("summary-proof.json", summaryText)
                        .put("verification.json", MapCheckpoint.canonical(proof) + "\n"));
    }
}
