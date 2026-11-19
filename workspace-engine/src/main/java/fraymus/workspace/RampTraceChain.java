package fraymus.workspace;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Pattern;

/** The registered two-procedure chain and its deterministic simulation-to-analysis handoff. */
final class RampTraceChain {
    static final String FAMILY = "ramp-trace-chain";
    static final String KEY = "rampTraceChain";
    static final String APPROVED = "rampTraceChainApproved";
    static final String HANDOFF = "chainHandoff";
    static final String REQUEST = "Review a local chain: Compare both ramp setups, then analyze the farther setup's motion.";
    static final String REVIEW_REASON = "Review the two steps in Mission, then confirm to start the ramp comparison.";
    private static final Pattern REQUESTS = Pattern.compile(Pattern.quote(REQUEST) + "( Pause after comparison\\.)?",
            Pattern.CASE_INSENSITIVE);

    private RampTraceChain() {}

    static boolean proposed(String request) {
        return request.strip().regionMatches(true, 0, "Review a local chain:", 0, "Review a local chain:".length());
    }

    static JSONObject plan(String request) {
        var match = REQUESTS.matcher(request.strip());
        if (!match.matches()) throw new IllegalArgumentException("Choose the registered ramp-to-analysis chain");
        return new JSONObject().put("procedure", "verified-ramp-trace-analysis-v1")
                .put("pauseAfterComparison", match.group(1) != null);
    }

    private static String owned(String task, String purpose) {
        return UUID.nameUUIDFromBytes((FAMILY + ":" + purpose + ":" + task).getBytes(StandardCharsets.UTF_8)).toString();
    }

    static String workflowId(String task) { return owned(task, "analysis"); }

    static JSONObject derive(String task, JSONObject comparison, JSONObject artifact, JSONObject pinnedSource) throws Exception {
        var audit = comparison.getJSONObject("audit");
        var report = comparison.getJSONObject("report");
        if (!task.equals(comparison.getString("id")) || !RampProcedure.COMPARE.equals(comparison.getString("procedure"))
                || !comparison.getString("status").equals("completed") || !audit.getBoolean("passed")
                || !comparison.getJSONObject("source").similar(pinnedSource)
                || !report.getString("id").equals(artifact.getString("id"))
                || report.getInt("version") != artifact.getInt("version")
                || !report.getString("sha256").equals(Creation.digest(artifact.getJSONObject("contents")))) {
            throw new IllegalStateException("The verified comparison no longer matches its original source and report");
        }
        String side = audit.getString("farther");
        if (!side.equals("first") && !side.equals("second")) {
            throw new IllegalStateException("The verified comparison is a tie. Choose two setups with different travel for a farther-setup summary.");
        }
        var original = new JSONObject(artifact.getJSONObject("contents").getString("experiment.json")).getJSONObject("original");
        var samples = original.getJSONObject(side).getJSONArray("samples");
        if (samples.length() != audit.getInt("samplesPerSetup")) throw new IllegalStateException("The original trace row count changed");
        var csv = new StringBuilder("time_s,displacement_m\n");
        for (Object raw : samples) {
            var sample = (JSONObject) raw;
            csv.append(sample.getDouble("timeS")).append(',').append(sample.getDouble("distanceM")).append('\n');
        }
        return new JSONObject().put("datasetId", owned(task, "trace")).put("workflowId", workflowId(task))
                .put("winner", side).put("traceRows", samples.length()).put("traceCsv", csv.toString())
                .put("traceSha256", MeasurementStore.hash(csv.toString())).put("report", report)
                .put("comparisonSha256", WorldFrames.hash(comparison))
                .put("nativeTraceSha256", audit.getJSONObject("setups").getJSONObject(side).getString("originalSha256"));
    }

    static JSONObject dataset(JSONObject handoff) {
        return new JSONObject().put("title", "Verified simulated ramp trace · " + handoff.getString("winner") + " setup")
                .put("filename", "verified-ramp-trace.csv").put("csv", handoff.getString("traceCsv"))
                .put("sourceKind", "synthetic").put("missingPolicy", "reject")
                .put("sourceNote", "Downhill displacement from a replay-verified Bullet simulation, in elapsed seconds and metres. "
                        + "Original native trace SHA-256 " + handoff.getString("nativeTraceSha256") + ".")
                .put("mapping", new JSONObject().put("timeColumn", "time_s").put("distanceColumn", "displacement_m")
                        .put("timeUnit", "s").put("distanceUnit", "m"));
    }

    static void requireLinked(JSONObject handoff, JSONObject workflow) {
        var binding = workflow.getJSONObject("dataBinding");
        if (!DataSummary104.KIND.equals(workflow.getString("procedure"))
                || !handoff.getString("workflowId").equals(workflow.getString("id"))
                || !handoff.getString("datasetId").equals(binding.getString("datasetId"))
                || binding.getInt("datasetVersion") != 1 || !binding.getString("sourceKind").equals("synthetic")
                || !handoff.getString("traceSha256").equals(binding.getString("sourceSha256"))) {
            throw new IllegalStateException("The motion summary is not bound to the verified comparison trace");
        }
    }

    static void verifyPlan(JSONObject task) {
        var plan = task.getJSONObject(KEY);
        if (!FAMILY.equals(task.getString("family044")) || !plan.similar(plan(task.getString("goal"))))
            throw new IllegalStateException("The reviewed chain plan changed");
    }
}
