package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

import static fraymus.workspace.RoutedComparisonReport.count;
import static fraymus.workspace.RoutedComparisonReport.text;

/** A reviewed selection of sections from one verified comparison. */
record ComparisonBriefing(String id, List<String> sections) {
    static final String TITLE = "Reviewed comparison briefing";
    private static final List<String> ORDER = List.of("totals", "chart", "rows");

    static List<String> sections(JSONArray selection) {
        var values = selection.toList();
        var ordered = ORDER.stream().filter(values::contains).toList();
        if (ordered.isEmpty() || ordered.size() != values.size())
            throw new IllegalArgumentException("Choose distinct sections from totals, chart and rows");
        return ordered;
    }

    boolean has(String section) { return sections.contains(section); }

    private String label() { return "Included sections · " + String.join(" · ", sections); }

    String html(String story) {
        return story.replace(RoutedComparisonStory.TITLE, TITLE)
                .replace("<h1>" + TITLE + "</h1>", "<h1>" + TITLE + "</h1>"
                        + "<p id=\"briefing-sections\">" + label() + "</p>")
                .replace("</style>", "[hidden]{display:none!important}\n</style>")
                .replace("<div id=\"visible-totals\">", "<div id=\"visible-totals\"" + hidden("totals") + ">")
                .replace("<section id=\"change-map\"", "<section id=\"change-map\"" + hidden("chart"))
                .replace("<div class=\"table-wrap\"", "<div class=\"table-wrap\"" + hidden("rows"));
    }

    private String hidden(String section) { return has(section) ? "" : " hidden"; }

    private List<String> visibilitySelectors() {
        return List.of(List.of("totals", "#visible-totals"), List.of("chart", "#change-map"), List.of("rows", ".table-wrap"))
                .stream().map(item -> item.get(1) + (has(item.getFirst()) ? ":not([hidden])" : "[hidden]")).toList();
    }

    void checkVisibility(JSONArray steps) {
        steps.put(text("#briefing-sections", label()));
        for (var selector : visibilitySelectors()) steps.put(count(selector, 1));
    }

    void checkSourceVisibility(JSONArray steps) {
        steps.put(text("#briefing-sections", label())).put(count(String.join(",", visibilitySelectors()), 3));
    }

    void checkSections(JSONArray steps) {
        checkVisibility(steps);
        steps.put(count("a[href=\"matched-comparison.csv\"]", 1))
                .put(count("a[href=\"import-receipt.json\"]", 1))
                .put(count("a[href=\"summary-proof.json\"]", 1));
    }

    JSONObject evidence() { return new JSONObject().put("id", id).put("sections", sections); }
}
