package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/** Typed boundary between the native Relay playtest and the offline clearance dashboard. */
final class RelayClearanceChain {
    static final String FAMILY = "relay-clearance-chain";
    static final String PLAN = "relayClearanceChain";
    static final String REPORT = "relayDashboard";
    static final String RECEIPT = "relayDashboardReceipt";
    static final String CHECK = "relayDashboard.check";
    private static final String PROCEDURE = "verified-relay-clearance-v1";
    private static final Pattern REQUEST = Pattern.compile(
            "^Review a local chain: Playtest Relay (Ridge|Switchyard), then build a clearance dashboard\\.( Pause after playtest\\.)?$",
            Pattern.CASE_INSENSITIVE);

    private RelayClearanceChain() {}

    static boolean requested(String request) { return REQUEST.matcher(request.strip()).matches(); }

    static JSONObject plan(String request) {
        var match = REQUEST.matcher(request.strip());
        if (!match.matches()) throw new IllegalArgumentException("Choose Ridge or Switchyard for the Relay dashboard");
        String course = match.group(1).equalsIgnoreCase("Ridge") ? "Ridge" : "Switchyard";
        return new JSONObject().put("procedure", PROCEDURE).put("course", "Relay " + course)
                .put("request", "Playtest Relay " + course + " at all three paces.")
                .put("pauseAfterPlaytest", match.group(2) != null);
    }

    static void verifyPlan(JSONObject task) {
        if (!FAMILY.equals(task.getString("family044")) || !plan(task.getString("goal")).similar(task.getJSONObject(PLAN))
                || !task.getJSONObject("relaySource").getString("manifestSha256").matches("[0-9a-f]{64}"))
            throw new IllegalStateException("The reviewed Relay source or plan changed");
    }

    static String reportKey(String taskId) { return PROCEDURE + ":" + taskId; }

    static String reportId(String taskId) {
        return UUID.nameUUIDFromBytes(reportKey(taskId).getBytes(StandardCharsets.UTF_8)).toString();
    }

    static JSONObject handoff(JSONObject task, JSONObject run, JSONObject report) {
        var plan = task.getJSONObject(PLAN);
        var audit = run.getJSONObject("audit");
        var ref = run.getJSONObject("report");
        String manifest = task.getJSONObject("relaySource").getString("manifestSha256");
        if (!task.getString("id").equals(run.getString("id"))
                || !plan.getString("request").equals(run.getString("request"))
                || !task.getJSONObject("relaySource").similar(run.getJSONObject("source"))
                || !RelayPlaytest.KIND.equals(run.getString("procedure")) || !"completed".equals(run.getString("status"))
                || !plan.getString("course").equals(audit.getString("title"))
                || !audit.getBoolean("passed") || audit.getInt("paceCount") != 3 || audit.getInt("independentReplays") != 3
                || !manifest.equals(audit.getString("manifestSha256"))
                || !ref.getString("id").equals(report.getString("id")) || ref.getInt("version") != report.getInt("version")
                || !audit.similar(new JSONObject(report.getJSONObject("contents").getString("playtest.json")))) {
            throw new IllegalStateException("The verified playtest does not match its reviewed source and report");
        }
        var source = WorldFrames.copy(ref).put("sha256", Creation.digest(report.getJSONObject("contents")));
        return new JSONObject().put("type", PROCEDURE).put("report", source).put("playtestSha256", WorldFrames.hash(run))
                .put("manifestSha256", manifest).put("dashboardId", reportId(task.getString("id")))
                .put("audit", audit);
    }

    private record Crossing(String pace, String rail, double speed, double clearance, long finishTick) {
        String label() {
            return String.format(Locale.ROOT, "%s · %.3f u/s · %.3f u · tick %d", rail, speed, clearance, finishTick);
        }
    }

    private static List<Crossing> crossings(JSONObject handoff) {
        var rows = new ArrayList<Crossing>();
        for (Object rawRun : handoff.getJSONObject("audit").getJSONArray("runs")) {
            var run = (JSONObject) rawRun;
            for (Object rawRail : run.getJSONArray("crossings")) {
                var rail = (JSONObject) rawRail;
                rows.add(new Crossing(run.getString("pace"), rail.getString("name"), rail.getDouble("entrySpeed"),
                        rail.getDouble("measuredClearance"), run.getLong("finishTick")));
            }
        }
        return rows;
    }

    static JSONObject dashboard(JSONObject handoff) {
        var audit = handoff.getJSONObject("audit");
        var rows = new StringBuilder();
        var csv = new StringBuilder("pace,rail,entry_game_units_per_second,clearance_game_units,finish_tick\n");
        int index = 0;
        for (var crossing : crossings(handoff)) {
            rows.append("<li id=\"crossing-").append(++index).append("\" data-pace=\"")
                    .append(SpatialReport.escape(crossing.pace())).append("\"><strong>")
                    .append(SpatialReport.escape(crossing.pace())).append("</strong><span>")
                    .append(SpatialReport.escape(crossing.label())).append("</span></li>\n");
            csv.append(crossing.pace()).append(",\"").append(crossing.rail().replace("\"", "\"\"")).append("\",")
                    .append(crossing.speed()).append(',').append(crossing.clearance()).append(',')
                    .append(crossing.finishTick()).append('\n');
        }
        String title = audit.getString("title") + " clearance dashboard";
        String html = """
                <!doctype html><html lang="en"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width,initial-scale=1"><title>%s</title>
                <style>
                *{box-sizing:border-box}[hidden]{display:none!important}
                body{margin:0;background:#0e1821;color:#e9f2f6;font:16px/1.45 system-ui}
                main{max-width:860px;margin:auto;padding:clamp(16px,5vw,52px)}
                h1{font-size:clamp(1.8rem,5vw,3rem);line-height:1.1;margin:0;color:#eafaff}
                p,li,nav{margin:.8rem 0}button,a{color:#87e8d1}button{background:#172c36;border:1px solid #46727b;
                padding:.6rem .8rem;border-radius:.35rem;font:inherit;cursor:pointer}
                button[aria-pressed="true"]{background:#87e8d1;color:#102429}
                :focus-visible{outline:2px solid #fff;outline-offset:3px}
                ul{padding:0;list-style:none}li{background:#172832;border-radius:.45rem;padding:.7rem;display:grid;
                grid-template-columns:5.5rem 1fr;gap:.4rem}strong{text-transform:capitalize;color:#87e8d1}
                .source{font-size:.88rem;overflow-wrap:anywhere}
                </style></head><body><main><h1>%s</h1>
                <p id="minimum">%s</p><p>Replayed crossings in game units and engine ticks. Speed is in game units per second.</p>
                <nav aria-label="Filter by pace"><button type="button" data-filter="all" aria-pressed="true">All paces</button>
                <button type="button" data-filter="normal" aria-pressed="false">Normal</button>
                <button type="button" data-filter="steady" aria-pressed="false">Steady</button>
                <button type="button" data-filter="careful" aria-pressed="false">Careful</button></nav>
                <p id="shown" aria-live="polite">%d crossings shown</p><ul id="crossings">%s</ul>
                <p><a href="crossings.csv" download>Export crossings CSV</a> · <a href="playtest-source.json">Open source receipt</a></p>
                <p class="source">Playtest report <span id="source">%s</span></p></main>
                <script>
                const rows=[...document.querySelectorAll('#crossings li')];
                const buttons=[...document.querySelectorAll('[data-filter]')];
                for(const button of buttons) button.addEventListener('click',()=>{
                  const pace=button.dataset.filter;
                  for(const row of rows) row.hidden=pace!=='all'&&row.dataset.pace!==pace;
                  for(const option of buttons) option.setAttribute('aria-pressed', String(option===button));
                  document.querySelector('#shown').textContent=rows.filter(row=>!row.hidden).length+' crossings shown';
                });
                </script></body></html>
                """.formatted(title, title, minimum(audit), audit.getInt("crossings"), rows, sourceLabel(handoff));
        return new JSONObject().put("title", title)
                .put("files", new JSONObject().put("index.html", html).put("crossings.csv", csv.toString())
                        .put("playtest-source.json", MapCheckpoint.canonical(handoff) + "\n"))
                .put("sources", List.of("relay-report:" + sourceLabel(handoff),
                        "sha256:" + handoff.getJSONObject("report").getString("sha256")));
    }

    private static String minimum(JSONObject audit) {
        return String.format(Locale.ROOT, "Minimum replayed clearance %.3f game units", audit.getDouble("minimumClearance"));
    }

    private static String sourceLabel(JSONObject handoff) {
        var report = handoff.getJSONObject("report");
        return report.getString("id") + " · version " + report.getInt("version");
    }

    static JSONObject ref(JSONObject handoff, JSONObject artifact) {
        var generated = dashboard(handoff);
        if (!handoff.getString("dashboardId").equals(artifact.getString("id")) || artifact.getInt("version") != 1
                || !generated.getJSONObject("files").similar(artifact.getJSONObject("contents"))
                || !generated.getJSONArray("sources").similar(artifact.getJSONArray("sources")))
            throw new IllegalStateException("The dashboard changed after the verified playtest handoff");
        return new JSONObject().put("id", artifact.getString("id")).put("version", artifact.getInt("version"))
                .put("sha256", Creation.digest(artifact.getJSONObject("contents")));
    }

    static JSONArray checks(JSONObject handoff) {
        var audit = handoff.getJSONObject("audit");
        int count = audit.getInt("crossings");
        var steps = new JSONArray().put(text("#minimum", minimum(audit))).put(text("#source", sourceLabel(handoff)))
                .put(count("#crossings li:not([hidden])", count))
                .put(count("a[href='crossings.csv'][download]", 1));
        var rows = crossings(handoff);
        for (int i = 0; i < rows.size(); i++) steps.put(text("#crossing-" + (i + 1) + " span", rows.get(i).label()));
        for (String pace : List.of("normal", "steady", "careful")) {
            int visible = (int) rows.stream().filter(row -> row.pace().equals(pace)).count();
            steps.put(count("button[data-filter='" + pace + "']", 1).put("action", "click"))
                    .put(count("#crossings li:not([hidden])[data-pace='" + pace + "']", visible))
                    .put(text("#shown", visible + " crossings shown"));
        }
        return steps.put(count("button[data-filter='all']", 1).put("action", "click"))
                .put(count("#crossings li:not([hidden])", count));
    }

    private static JSONObject count(String selector, int expected) {
        return new JSONObject().put("selector", selector).put("expectCount", expected);
    }

    private static JSONObject text(String selector, String expected) {
        return new JSONObject().put("selector", selector).put("expectText", expected);
    }

    static void checkReceipt(JSONObject handoff, JSONObject artifact, JSONObject receipt) {
        var ref = ref(handoff, artifact);
        if (!RampMissionReport.verifiedReceipt(ref, receipt, checks(handoff)))
            throw new IllegalStateException("The dashboard's original Chromium checks changed");
    }
}
