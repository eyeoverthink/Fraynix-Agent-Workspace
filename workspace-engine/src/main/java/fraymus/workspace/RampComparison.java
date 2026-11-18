package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Two isolated setups on one clock. Each setup has its own fresh replay and source binding. */
final class RampComparison {
    private static final List<String> SETUPS = List.of("first", "second");

    @FunctionalInterface
    interface Run {
        JSONObject execute(JSONObject source, JSONObject values, String id) throws Exception;
    }

    private RampComparison() {}

    private static JSONObject boundCase(JSONObject source, String side) {
        return WorldFrames.copy(source).put("comparisonCase", side)
                .put("values", source.getJSONObject(side.equals("first") ? "values" : "secondValues"));
    }

    static JSONObject run(JSONObject source, JSONObject first, JSONObject second, String taskId, Run run) throws Exception {
        return new JSONObject().put("first", run.execute(boundCase(source, "first"), first, taskId + ":first"))
                .put("second", run.execute(boundCase(source, "second"), second, taskId + ":second"));
    }

    static JSONObject audit(JSONObject task) {
        JSONObject source = task.getJSONObject("source");
        JSONObject original = task.getJSONObject("original"), replay = task.getJSONObject("replay");
        if (!original.keySet().equals(Set.copyOf(SETUPS)) || !replay.keySet().equals(Set.copyOf(SETUPS))) {
            throw new IllegalStateException("Both comparison setups must be retained");
        }
        var sessions = new HashSet<String>();
        var setups = new JSONObject();
        for (String side : SETUPS) {
            JSONObject a = original.getJSONObject(side), b = replay.getJSONObject(side);
            if (!sessions.add(a.getString("sessionId")) || !sessions.add(b.getString("sessionId"))) {
                throw new IllegalStateException("Comparison requires four separate native sessions");
            }
            setups.put(side, RampMission.auditTrace(task, a, b, boundCase(source, side)));
        }
        JSONObject a = setups.getJSONObject("first"), b = setups.getJSONObject("second");
        int ticks = a.getInt("ticks"), samples = a.getInt("sampleCount");
        if (ticks != b.getInt("ticks") || samples != b.getInt("sampleCount")) {
            throw new IllegalStateException("Comparison must use the same simulation clock for both setups");
        }
        double difference = b.getDouble("distanceM") - a.getDouble("distanceM");
        String farther = Math.abs(difference) <= RampProcedure.REPLAY_TOLERANCE ? "tie" : difference > 0 ? "second" : "first";
        return new JSONObject().put("kind", RampProcedure.COMPARE).put("passed", true).put("setups", setups)
                .put("ticks", ticks).put("seconds", a.getDouble("seconds")).put("samplesPerSetup", samples)
                .put("distanceDifferenceM", difference).put("farther", farther)
                .put("observationSha256", WorldFrames.hash(task.getJSONObject("observation")))
                .put("sourceSha256", WorldFrames.hash(source))
                .put("originalSha256", WorldFrames.hash(original)).put("replaySha256", WorldFrames.hash(replay));
    }

    static String summary(JSONObject audit) {
        JSONObject readings = audit.getJSONObject("setups");
        double a = readings.getJSONObject("first").getDouble("distanceM");
        double b = readings.getJSONObject("second").getDouble("distanceM");
        String result = audit.getString("farther").equals("tie") ? "Travel was within the replay tolerance." :
                String.format(Locale.ROOT, "The %s setup travelled %.3f m farther.", audit.getString("farther"),
                        Math.abs(audit.getDouble("distanceDifferenceM")));
        return String.format(Locale.ROOT, "First setup %.3f m, second setup %.3f m over %.3f s. %s",
                a, b, audit.getDouble("seconds"), result);
    }

    static JSONObject report(JSONObject task, JSONObject audit, String inputCsv) {
        JSONObject source = task.getJSONObject("source");
        JSONObject original = task.getJSONObject("original"), replay = task.getJSONObject("replay");
        JSONArray a = original.getJSONObject("first").getJSONArray("samples");
        JSONArray b = original.getJSONObject("second").getJSONArray("samples");
        StringBuilder csv = new StringBuilder("tick,time_s,first_distance_m,second_distance_m,second_minus_first_m\n");
        StringBuilder rows = new StringBuilder();
        double low = 0, high = 0;
        for (int i = 0; i < a.length(); i++) {
            JSONObject first = a.getJSONObject(i), second = b.getJSONObject(i);
            int tick = first.getInt("tick");
            double time = first.getDouble("timeS"), da = first.getDouble("distanceM"), db = second.getDouble("distanceM");
            low = Math.min(low, Math.min(da, db));
            high = Math.max(high, Math.max(da, db));
            csv.append(String.format(Locale.ROOT, "%d,%.9f,%.9f,%.9f,%.9f\n", tick, time, da, db, db - da));
            rows.append(String.format(Locale.ROOT, "<tr><td>%.3f</td><td>%.5f</td><td>%.5f</td><td>%+.5f</td></tr>",
                    time, da, db, db - da));
        }
        double span = Math.max(.001, high - low);
        int ticks = audit.getInt("ticks");
        String html = """
                <!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
                <title>Verified ramp comparison</title><style>
                *{box-sizing:border-box}body{margin:0;background:#101d25;color:#ecf6f1;font:16px/1.45 system-ui}
                main{max-width:900px;margin:auto;padding:clamp(14px,4vw,34px)}h1{line-height:1.15;color:#96ead0;margin:.3rem 0}
                svg{display:block;width:100%%;max-height:240px;background:#182d37;margin-top:.7rem}
                button{background:#96ead0;color:#17332f;border:0;border-radius:5px;padding:.45rem .65rem;font:inherit;cursor:pointer}
                a{color:#96ead0}table{width:100%%;border-collapse:collapse;text-align:left}td,th{padding:.45rem;border-bottom:1px solid #435f64}
                details{margin-top:.8rem}.scroller{overflow:auto}.first{color:#75edbb}.second{color:#f9cc83}
                </style><main><small id="revision">Input revision %d</small><h1>Verified ramp comparison</h1>
                <p id="result">%s</p><p><span class="first">First setup</span> · <span class="second">Second setup</span> · %d independently checked samples each</p>
                <button id="second-toggle" type="button" aria-pressed="true">Hide second setup</button>
                <svg role="img" aria-label="Downhill distance on the shared simulation clock" viewBox="0 0 600 200">
                <path d="M25,12 V174 H572" stroke="#567883" fill="none"/>
                <polyline id="first-line" points="%s" fill="none" stroke="#75edbb" stroke-width="3"/>
                <polyline id="second-line" points="%s" visibility="visible" fill="none" stroke="#f9cc83" stroke-width="3"/>
                <text x="28" y="193" fill="#b5d3cc">0 s</text><text x="526" y="193" fill="#b5d3cc">%.3f s</text>
                </svg><details><summary>Read the samples</summary><div class="scroller"><table><thead>
                <tr><th>Time (s)</th><th>First (m)</th><th>Second (m)</th><th>Difference (m)</th></tr>
                </thead><tbody>%s</tbody></table></div></details>
                <p><a href="samples.csv" download>Download samples</a> · <a href="input.csv" download>Download original inputs</a>
                · <a href="experiment.json" download>Download evidence</a></p></main>
                <script>
                const button=document.querySelector('#second-toggle'),line=document.querySelector('#second-line');
                button.addEventListener('click',()=>{const show=button.getAttribute('aria-pressed')==='false';
                button.setAttribute('aria-pressed',String(show));button.textContent=show?'Hide second setup':'Show second setup';
                line.setAttribute('visibility',show?'visible':'hidden');});
                </script></html>
                """.formatted(source.getInt("version"), summary(audit), audit.getInt("samplesPerSetup"),
                RampMissionReport.points(a, low, span, ticks), RampMissionReport.points(b, low, span, ticks),
                audit.getDouble("seconds"), rows);
        var evidence = new JSONObject().put("source", source).put("observation", task.getJSONObject("observation"))
                .put("audit", audit).put("original", original).put("replay", replay);
        return new JSONObject().put("title", "Verified ramp comparison · v" + source.getInt("version"))
                .put("files", new JSONObject().put("index.html", html).put("input.csv", inputCsv)
                        .put("samples.csv", csv.toString()).put("experiment.json", evidence.toString(2)))
                .put("sources", List.of("artifact:" + source.getString("id") + ":v" + source.getInt("version"),
                        "ramp-page:" + source.getString("pageSha256"), "ramp-csv:" + source.getString("csvSha256"),
                        "ramp-replay:" + audit.getString("replaySha256")));
    }

    static JSONArray checks(JSONObject task) {
        JSONObject audit = task.getJSONObject("audit");
        return new JSONArray().put(RampMissionReport.text("#revision", "Input revision " + task.getJSONObject("source").getInt("version")))
                .put(RampMissionReport.text("#result", summary(audit)))
                .put(RampMissionReport.count("tbody tr", audit.getInt("samplesPerSetup")))
                .put(RampMissionReport.count("a[href='input.csv'],a[href='samples.csv'],a[href='experiment.json']", 3))
                .put(RampMissionReport.count("#first-line,#second-line[visibility='visible']", 2))
                .put(RampMissionReport.text("#second-toggle", "Show second setup").put("action", "click"))
                .put(RampMissionReport.count("#second-line[visibility='hidden']", 1))
                .put(RampMissionReport.text("#second-toggle", "Hide second setup").put("action", "click"))
                .put(RampMissionReport.count("#second-line[visibility='visible']", 1));
    }
}
