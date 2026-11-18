package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.regex.Pattern;

/** The registered contract shared by the direct command and Java's local-model adapter. */
final class RampProcedure {
    static final String ID = "local-ramp-experiment-v1";
    static final String SELECT_TOOL = "localRamp.select";
    static final double MIN_SECONDS = .25;
    static final double MAX_SECONDS = 2;
    static final int SAMPLE_TICKS = 12;
    static final double REPLAY_TOLERANCE = .0001;

    private static final Pattern WORDING = Pattern.compile(
            "^(run|plan) the local ramp experiment( and pause after reading)?[.!]?$", Pattern.CASE_INSENSITIVE);

    enum Payload {
        SOURCE_REVISION, BROWSER_ROWS, NATIVE_TRACE, REPLAY_COMPARISON, REPORT_REVISION, CANVAS_RECEIPT
    }

    enum Format {
        FORM("form-v1"), CSV("csv-v1");
        final String id;
        Format(String id) { this.id = id; }
        static Format from(JSONObject source) { return source.has("csvSha256") ? CSV : FORM; }
    }

    enum Step {
        OBSERVE("browser", "browser", Payload.SOURCE_REVISION, Payload.BROWSER_ROWS, "localRamp.read"),
        RUN("experiment", "experiment", Payload.BROWSER_ROWS, Payload.NATIVE_TRACE, ""),
        REPLAY("replay", "replay", Payload.NATIVE_TRACE, Payload.REPLAY_COMPARISON, ""),
        REPORT("report", "artifact", Payload.REPLAY_COMPARISON, Payload.REPORT_REVISION, ""),
        CHECK("report", "canvas", Payload.REPORT_REVISION, Payload.CANVAS_RECEIPT, "localRamp.check");

        final String checkpoint;
        final String receipt;
        final Payload input;
        final Payload output;
        final String surfaceTool;

        Step(String checkpoint, String receipt, Payload input, Payload output, String surfaceTool) {
            this.checkpoint = checkpoint;
            this.receipt = receipt;
            this.input = input;
            this.output = output;
            this.surfaceTool = surfaceTool;
        }

        JSONObject describe() {
            return new JSONObject().put("id", receipt).put("input", input.name()).put("output", output.name())
                    .put("owner", surfaceTool.isEmpty() ? "java" : "chromium");
        }
    }

    private RampProcedure() {}

    record Intent(boolean planned, boolean pause) {
        JSONObject directChoice() {
            return new JSONObject().put("procedure", ID).put("pauseAfterReading", pause);
        }
    }

    static Intent intent(String request) {
        var words = WORDING.matcher(request.strip());
        if (!words.matches()) throw new IllegalArgumentException("Choose the registered local ramp experiment");
        return new Intent(words.group(1).equalsIgnoreCase("plan"), words.group(2) != null);
    }

    static boolean requested(String request) { return WORDING.matcher(request.strip()).matches(); }

    static JSONObject admit(String request, JSONObject proposal) {
        Intent intent = intent(request);
        ToolSchema043.validate(selectionTool().getJSONObject("function").getJSONObject("parameters"), proposal);
        if (proposal.getBoolean("pauseAfterReading") != intent.pause()) {
            throw new IllegalArgumentException("The procedure must preserve your requested pause after reading");
        }
        return intent.directChoice();
    }

    static JSONObject selectionTool() {
        var properties = new JSONObject()
                .put("procedure", new JSONObject().put("type", "string").put("enum", List.of(ID)))
                .put("pauseAfterReading", new JSONObject().put("type", "boolean"));
        var schema = new JSONObject().put("type", "object").put("properties", properties)
                .put("additionalProperties", false).put("required", List.of("procedure", "pauseAfterReading"));
        var function = new JSONObject().put("name", SELECT_TOOL.replace('.', '_')).put("parameters", schema)
                .put("description", "Select the registered local ramp experiment. Java binds its existing input revision and verifies every stage.");
        return new JSONObject().put("type", "function").put("function", function);
    }

    static JSONObject catalog() {
        var stages = new JSONArray();
        for (Step step : Step.values()) stages.put(step.describe());
        return new JSONObject().put("id", ID).put("version", 1).put("stages", stages)
                .put("inputs", List.of(Format.FORM.id, Format.CSV.id))
                .put("limits", new JSONObject().put("secondsMin", MIN_SECONDS).put("secondsMax", MAX_SECONDS)
                        .put("tickHz", (int) (1 / BulletRamp.DT)).put("sampleTicks", SAMPLE_TICKS)
                        .put("replayTolerance", REPLAY_TOLERANCE));
    }

    static String hash() { return WorldFrames.hash(catalog()); }

    static Step next(JSONObject task) {
        int i = task.getJSONArray("stages").length();
        if (i >= Step.values().length || !Step.values()[i].checkpoint.equals(task.getString("stage"))) {
            throw new IllegalStateException("The saved stage differs from the procedure contract");
        }
        return Step.values()[i];
    }

    static void append(JSONObject task, Step step, JSONObject payload) {
        if (next(task) != step) throw new IllegalStateException("Procedure step arrived out of order");
        task.getJSONArray("stages").put(receipt(step, payload));
    }

    static void verify(JSONObject task, List<JSONObject> payloads) {
        var original = task.getJSONArray("stages");
        if (original.length() != payloads.size()) throw new IllegalStateException("Procedure receipt count changed");
        for (int i = 0; i < payloads.size(); i++) {
            if (!receipt(Step.values()[i], payloads.get(i)).similar(original.getJSONObject(i))) {
                throw new IllegalStateException("Original " + Step.values()[i].receipt + " receipt differs");
            }
        }
        if (task.has("contractSha256") && !hash().equals(task.getString("contractSha256"))) {
            throw new IllegalStateException("The installed procedure contract changed");
        }
    }

    private static JSONObject receipt(Step step, JSONObject payload) {
        return new JSONObject().put("name", step.receipt).put("status", "verified")
                .put("sha256", WorldFrames.hash(payload));
    }
}
