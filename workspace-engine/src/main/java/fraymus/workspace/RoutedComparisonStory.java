package fraymus.workspace;

import org.json.JSONArray;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

import static fraymus.workspace.RoutedComparisonReport.count;
import static fraymus.workspace.RoutedComparisonReport.text;

/** The chart presentation has a separate identity from previously saved comparison explorers. */
final class RoutedComparisonStory {
    static final String TITLE = "Verified change story";
    private static final String EMPTY = "No matched changes in this view.";
    private static final String SELECT = "Select a bar to inspect both source readings.";
    private static final String CSS = FrayConversion.loadHtml("routing-story.css");
    private static final String SCRIPT = FrayConversion.loadHtml("routing-story.js");
    private static final String CHART = """
            <section id="change-map" aria-labelledby="change-heading">
              <h2 id="change-heading">What changed</h2>
              <p>Losses extend left, gains extend right. Each unit keeps its own scale as you filter.</p>
              <p id="change-empty" aria-live="polite"></p>
              <div id="change-bars"></div>
              <div id="change-selection" class="change-selection" aria-live="polite" hidden>
                <h3 id="change-selection-title"></h3><p id="change-selection-readings"></p>
              </div>
            </section>
            """;

    private final List<List<String>> matched;

    RoutedComparisonStory(List<List<String>> rows) {
        matched = rows.stream().filter(row -> "Matched".equals(row.get(2))).toList();
    }

    String html(String explorer) {
        return explorer.replace("Verified comparison explorer", TITLE)
                .replace("</style>", CSS + "</style>")
                .replace("<div class=\"table-wrap\"", CHART + "<div class=\"table-wrap\"")
                .replace("<div class=\"filters\">", "<p id=\"story-error\" role=\"alert\" hidden></p><div class=\"filters\">")
                .replace("function installFilters(rows) {", "function installFilters(rows) {\n    const draw = changeChart(rows);")
                .replace("        showTotals(visible);", "        showTotals(visible);\n        draw(visible);")
                .replace("function downloadJson(name,data)", "function downloadJson(name,data,mime='application/json')")
                .replace("{type:'application/json'}", "{type:mime}")
                .replace("    installFilters(rows);", """
                            downloadJson('matched-comparison.csv',data,'text/csv');
                            installFilters(rows);""")
                .replace("render().catch(error=>{console.error(error.message);});", SCRIPT + """
                        render().catch(error=>{
                            const notice = document.getElementById('story-error');
                            notice.hidden = false;
                            notice.textContent = 'Unable to open this verified story: ' + error.message;
                            changeMap.hidden = true;
                            document.querySelector('.filters').hidden = true;
                            document.querySelectorAll('a[download]').forEach(link=>link.remove());
                            console.error(error.message);
                        });
                        """);
    }

    void checkOpen(JSONArray steps) {
        steps.put(count("#change-bars .change-bar", matched.size()));
        if (matched.isEmpty()) {
            steps.put(text("#change-empty", EMPTY));
            return;
        }
        var first = firstMatch("°F").orElseGet(() -> firstMatch("mm").orElseThrow());
        String unit = first.get(1);
        var delta = new BigDecimal(first.get(5));
        var scale = matched.stream().filter(row -> row.get(1).equals(unit))
                .map(row -> new BigDecimal(row.get(5)).abs()).max(BigDecimal::compareTo).orElseThrow();
        int width = scale.signum() == 0 ? 0 :
                delta.abs().scaleByPowerOfTen(4).divide(scale, 0, RoundingMode.DOWN).intValueExact();
        String direction = delta.signum() < 0 ? "loss" : delta.signum() > 0 ? "gain" : "steady";
        String selector = "#change-bars [data-first][data-direction=\"" + direction + "\"][data-width=\"" + width + "\"]";
        String key = new JSONArray("[" + first.get(0) + "]").getString(0);
        String amount = (delta.signum() > 0 ? "+" : "") + RoutedDashboardReport.plain(delta) + " " + unit;
        steps.put(count(selector, 1).put("action", "click"))
                .put(text("#change-selection-title", RoutedKeyComparisonReport.visibleKey(key) + " · " + amount))
                .put(text("#change-selection-readings", "Earlier " + RoutedComparisonExplorer.readingsText(first.get(3))
                        + " | Later " + RoutedComparisonExplorer.readingsText(first.get(4))));
    }

    private Optional<List<String>> firstMatch(String unit) {
        return matched.stream().filter(row -> unit.equals(row.get(1))).findFirst();
    }

    void checkReset(JSONArray steps) {
        steps.put(count("#change-bars .change-bar", matched.size()));
        if (!matched.isEmpty()) steps.put(text("#change-selection-title", SELECT));
    }

    void checkUnmatched(JSONArray steps) { steps.put(text("#change-empty", EMPTY)); }
}
