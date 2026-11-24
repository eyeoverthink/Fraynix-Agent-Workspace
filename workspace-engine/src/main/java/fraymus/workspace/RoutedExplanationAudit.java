package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static fraymus.workspace.RoutedComparisonReport.count;
import static fraymus.workspace.RoutedComparisonReport.text;
import static fraymus.workspace.RoutedDashboardReport.plain;
import static fraymus.workspace.SpatialReport.escape;

/** Builds an offline, inspectable report and checks its live browser representation against Java. */
final class RoutedExplanationAudit implements RoutedReport {
    private static final String TITLE = "Explanation audit";
    private static final String SCOPE = "Agreement is conditional on the stated assumptions, controls and error bounds. Trial dates and source references are supplied with the observations.";
    private static final String TEMPLATE = FrayConversion.loadHtml("routing-explanation-audit.html");
    private static final Pattern SLOT = Pattern.compile("@([A-Z_]+)@");
    private final JSONObject audit, claim, proposal;
    private final ExplanationEvidence.Result result;
    private final String originalEvidence, html, browserHash;
    private final StringBuilder fields = new StringBuilder();

    RoutedExplanationAudit(JSONObject audit, JSONObject briefing) throws Exception {
        this.audit = audit;
        proposal = audit.getJSONObject("proposal");
        int index = proposal.getInt("claimIndex");
        claim = WorldFrames.copy(ExplanationAuditBook.claim(audit, index));
        claim.remove("index");
        originalEvidence = briefing.getJSONObject("contents").getString("claim-evidence.json");
        if (!claim.similar(new JSONObject(originalEvidence).getJSONArray("rows").getJSONObject(index))) {
            throw new IllegalStateException("The selected claim no longer matches the original briefing.");
        }
        result = ExplanationEvidence.evaluate(audit.getJSONObject("attachment").getString("base64"), claim,
                proposal.getString("margin"), audit.getString("sourceSha256"));
        if (!result.sha256().equals(audit.getString("observationsSha256"))) {
            throw new IllegalStateException("The attached observations changed.");
        }

        var substitutions = Map.ofEntries(
                Map.entry("CLAIM", field(claim.getString("sentence"))),
                Map.entry("CITATION", field(citation())),
                Map.entry("HYPOTHESIS", field(proposal.getString("hypothesis"))),
                Map.entry("ASSUMPTIONS", field(proposal.getString("assumptions"))),
                Map.entry("PREDICTION", field(prediction())),
                Map.entry("OUTCOME", field(label(result.status()))),
                Map.entry("TALLY", field(tally())),
                Map.entry("PLOT", plot()),
                Map.entry("TRIALS", trials()),
                Map.entry("SCOPE", field(SCOPE)),
                Map.entry("BRIEFING", field(audit.getJSONObject("briefingArtifact").getString("id"))),
                Map.entry("ORIGINAL_SHA", field(audit.getString("sourceSha256"))),
                Map.entry("OBSERVATION_SHA", field(result.sha256())));
        browserHash = MeasurementStore.hash(fields.toString());
        html = SLOT.matcher(TEMPLATE).replaceAll(match -> Matcher.quoteReplacement(substitutions.get(match.group(1))));
    }

    private String citation() {
        return "Earlier row " + claim.getJSONObject("earlier").getInt("row")
                + " · Later row " + claim.getJSONObject("later").getInt("row");
    }

    private String prediction() {
        return result.prediction().label(result.unit()) + " (margin ±" + plain(result.margin()) + " " + result.unit() + ")";
    }

    private String tally() {
        return result.consistent() + " consistent · " + result.conflicting() + " conflicting · " + result.unresolved() + " unresolved";
    }

    private static String label(String value) {
        return switch (value) {
            case "supports-prediction" -> "Trials support the prediction";
            case "mixed" -> "Mixed trial evidence";
            case "conflicting" -> "Trials conflict with the prediction";
            case "inconclusive" -> "Trial evidence is inconclusive";
            case "consistent" -> "Within prediction";
            case "unresolved" -> "Overlaps prediction";
            default -> throw new IllegalArgumentException("Unknown explanation audit outcome");
        };
    }

    private void frame(String value) { fields.append(value.length()).append(':').append(value); }

    private String field(String value) {
        frame(value);
        return "<span data-audit-field>" + escape(value) + "</span>";
    }

    private record PlotScale(BigDecimal low, BigDecimal high) {
        int position(BigDecimal value) {
            return 12 + value.subtract(low).multiply(BigDecimal.valueOf(456))
                    .divide(high.subtract(low), 0, RoundingMode.HALF_UP).intValueExact();
        }
    }

    private String plot() {
        var low = result.prediction().low();
        var high = result.prediction().high();
        for (var trial : result.trials()) {
            low = low.min(trial.interval().low());
            high = high.max(trial.interval().high());
        }
        var scale = new PlotScale(low, high);
        int height = 20 + 24 * result.trials().size();
        var svg = new StringBuilder("<svg role=\"img\" aria-label=\"Trial intervals against the shaded prediction range\" viewBox=\"0 0 480 ")
                .append(height).append("\">");
        int start = scale.position(result.prediction().low());
        int width = scale.position(result.prediction().high()) - start;
        frame(start + "," + width + "," + height);
        svg.append("<rect class=\"prediction-band\" data-plot=\"x width height\" x=\"").append(start)
                .append("\" width=\"").append(width).append("\" height=\"").append(height).append("\"/>");
        for (int i = 0; i < result.trials().size(); i++) {
            var trial = result.trials().get(i);
            int from = scale.position(trial.interval().low());
            int to = scale.position(trial.interval().high());
            int center = scale.position(trial.effect());
            int y = 22 + i * 24;
            frame(from + "," + to + "," + y + "," + y);
            frame(center + "," + y);
            svg.append("<line class=\"trial-mark\" data-plot=\"x1 x2 y1 y2\" x1=\"").append(from).append("\" x2=\"")
                    .append(to).append("\" y1=\"").append(y).append("\" y2=\"").append(y)
                    .append("\"/><circle class=\"trial-point\" data-plot=\"cx cy\" r=\"3\" cx=\"").append(center)
                    .append("\" cy=\"").append(y).append("\"/>");
        }
        return svg.append("</svg><div class=\"axis\">").append(field(plain(low) + " " + result.unit()))
                .append(field(plain(high) + " " + result.unit())).append("</div>").toString();
    }

    private String trials() {
        var output = new StringBuilder();
        for (int i = 0; i < result.trials().size(); i++) {
            var trial = result.trials().get(i);
            output.append("<article class=\"trial\" id=\"trial-").append(i)
                    .append("\"><button type=\"button\" aria-expanded=\"false\" aria-controls=\"trial-readings-").append(i).append("\">")
                    .append(field(trial.id())).append(" · ").append(field(trial.interval().label(result.unit())))
                    .append(" · ").append(field(trial.status().equals("conflicting") ? "Outside prediction" : label(trial.status())))
                    .append("</button><dl id=\"trial-readings-").append(i).append("\" hidden>")
                    .append("<dt>Recorded at</dt><dd class=\"recorded\">").append(field(trial.when().toString()))
                    .append("</dd><dt>Source reference</dt><dd>").append(field(trial.source()))
                    .append("</dd><dt>Treatment</dt><dd class=\"treatment\">")
                    .append(field(readings(trial.treatmentBefore(), trial.treatmentAfter())))
                    .append("</dd><dt>Control</dt><dd>").append(field(readings(trial.controlBefore(), trial.controlAfter())))
                    .append("</dd><dt>Adjusted effect</dt><dd>")
                    .append(field(plain(trial.treatment()) + " − (" + plain(trial.control()) + ") = " + plain(trial.effect()) + " " + result.unit()))
                    .append("</dd><dt>Declared error bound</dt><dd>")
                    .append(field("±" + plain(trial.bound()) + " " + result.unit())).append("</dd></dl></article>");
        }
        return output.toString();
    }

    private String readings(BigDecimal first, BigDecimal last) {
        return plain(first) + " → " + plain(last) + " " + result.unit();
    }

    JSONObject summary() {
        return new JSONObject().put("status", result.status()).put("label", label(result.status()))
                .put("trials", result.trials().size()).put("consistent", result.consistent())
                .put("conflicting", result.conflicting()).put("unresolved", result.unresolved())
                .put("observationsSha256", result.sha256()).put("briefing", audit.getJSONObject("briefingArtifact"));
    }

    @Override public String html() { return html; }

    @Override public JSONArray steps() {
        var first = result.trials().getFirst();
        return new JSONArray().put(text("h1", TITLE)).put(text("#audit-claim", claim.getString("sentence")))
                .put(text("#audit-outcome", label(result.status()))).put(text("#audit-prediction", prediction()))
                .put(text("#audit-assumptions", proposal.getString("assumptions")))
                .put(text("#observations-sha", result.sha256())).put(text("#audit-browser-digest", browserHash))
                .put(count(".trial", result.trials().size())).put(count(".trial-mark", result.trials().size()))
                .put(count("a[download]", 3)).put(count("#trial-0 button", 1).put("action", "click"))
                .put(count("#trial-0 dl:not([hidden])", 1)).put(text("#trial-0 .recorded", first.when().toString()))
                .put(text("#trial-0 .treatment", readings(first.treatmentBefore(), first.treatmentAfter())));
    }

    @Override public JSONObject artifact(JSONObject file, JSONArray checks) {
        verifyBrowser(checks);
        var evidence = result.evidence().put("scope", SCOPE).put("claim", claim).put("proposal", proposal)
                .put("originalBriefing", audit.getJSONObject("briefingArtifact"))
                .put("originalComparisonCsvSha256", audit.getString("sourceSha256"))
                .put("observationFile", audit.getJSONObject("attachment").getString("name"))
                .put("browserAuditSha256", browserHash).put("browserChecks", checks.length());
        return new JSONObject().put("title", TITLE).put("files", new JSONObject().put("index.html", html)
                .put("audit-evidence.json", MapCheckpoint.canonical(evidence) + "\n").put("observations.csv", result.csv())
                .put("claim-evidence.json", originalEvidence))
                .put("sources", new JSONArray().put("chart-briefing:sha256:" + audit.getJSONObject("briefingArtifact").getString("sha256"))
                        .put("trial-observations:sha256:" + result.sha256()));
    }
}
