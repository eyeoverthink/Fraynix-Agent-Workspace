package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.regex.Pattern;

/** The registered contract shared by the direct command and Java's local-model adapter. */
final class RampProcedure {
    static final String ID = "local-ramp-experiment-v1";
    static final String COMPARE = "local-ramp-comparison-v1";
    static final String CLARIFY = "clarify";
    static final String SELECT_TOOL = "ramp.select";
    static final double MIN_SECONDS = .25;
    static final double MAX_SECONDS = 2;
    static final int SAMPLE_TICKS = 12;
    static final double REPLAY_TOLERANCE = .0001;

    private static final Pattern WORDING = Pattern.compile(
            "^(run|plan) the local ramp experiment( and pause after reading)?[.!]?$", Pattern.CASE_INSENSITIVE);

    private static final Pattern COMPARE_WORDING = Pattern.compile(
            "^compare the local ramp setups( and pause after reading)?[.!]?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern FLEXIBLE = Pattern.compile(
            "^plan a local ramp task( and pause after reading)?:[ \t]*(\\S.{0,1199})$", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMPARATIVE = Pattern.compile(
            "\\b(compare|both|which|each|difference|versus|farther|further|outperform|between)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern FIRST = Pattern.compile(
            "\\b(first|setup a|row one)\\b", Pattern.CASE_INSENSITIVE);

    enum Payload {
        SOURCE_REVISION, BROWSER_ROWS, NATIVE_TRACE, REPLAY_COMPARISON, REPORT_REVISION, CANVAS_RECEIPT
    }

    enum Format {
        FORM("form-v1"), CSV("csv-v1"), PAIRED_CSV("paired-csv-v1");
        final String id;
        Format(String id) { this.id = id; }
        static Format from(JSONObject source) {
            if (source.has("secondValues")) return PAIRED_CSV;
            return source.has("csvSha256") ? CSV : FORM;
        }
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

    record Intent(boolean planned, boolean pause, String explicit, String briefing) {
        boolean flexible() { return explicit.isEmpty(); }
        JSONObject directChoice() { return choice(explicit); }
        JSONObject choice(String procedure) {
            return new JSONObject().put("procedure", procedure).put("pauseAfterReading", pause);
        }
    }

    static Intent intent(String request) {
        String text = request.strip();
        var words = WORDING.matcher(text);
        if (words.matches()) return new Intent(words.group(1).equalsIgnoreCase("plan"), words.group(2) != null, ID, "");
        var comparison = COMPARE_WORDING.matcher(text);
        if (comparison.matches()) return new Intent(false, comparison.group(1) != null, COMPARE, "");
        var flexible = FLEXIBLE.matcher(text);
        if (flexible.matches()) return new Intent(true, flexible.group(1) != null, "", flexible.group(2));
        throw new IllegalArgumentException("Choose one registered local ramp task");
    }

    static boolean requested(String request) {
        String text = request.strip();
        return WORDING.matcher(text).matches() || COMPARE_WORDING.matcher(text).matches() || FLEXIBLE.matcher(text).matches();
    }

    static boolean registered(String procedure) { return ID.equals(procedure) || COMPARE.equals(procedure); }

    static JSONObject admit(String request, JSONObject proposal) {
        Intent intent = intent(request);
        SpatialScene.keys(proposal, "procedure", "pauseAfterReading");
        String procedure = proposal.getString("procedure");
        if (!registered(procedure) || !intent.flexible() && !intent.explicit().equals(procedure)) {
            throw new IllegalArgumentException("The procedure differs from the requested local ramp task");
        }
        if (!(proposal.opt("pauseAfterReading") instanceof Boolean pause) || pause != intent.pause()) {
            throw new IllegalArgumentException("The procedure must preserve your requested pause after reading");
        }
        return intent.choice(procedure);
    }

    static JSONObject select(String request, JSONObject proposal) {
        Intent intent = intent(request);
        ToolSchema043.validate(selectionTool(intent.flexible()).getJSONObject("function").getJSONObject("parameters"), proposal);
        String selected = proposal.getString("procedure");
        if (CLARIFY.equals(selected)) return intent.choice(selected);
        if (intent.flexible()) {
            String expected = signal(intent.briefing());
            if (!expected.isEmpty() && !expected.equals(selected)) {
                throw new IllegalArgumentException("The model chose a different outcome from the request. Say first setup or compare both setups.");
            }
        }
        return admit(request, intent.choice(selected));
    }

    static String question(Intent intent, JSONObject source) {
        if (source.has("secondValues")) return "Do you want the first setup's trajectory or a comparison of both setups?";
        if (COMPARE.equals(signal(intent.briefing()))) return "Add a second setup with the same duration in Ramp input, then start a new comparison.";
        return "Should I run the current setup, or will you add a second setup for comparison?";
    }

    private static String signal(String briefing) {
        if (COMPARATIVE.matcher(briefing).find()) return COMPARE;
        if (FIRST.matcher(briefing).find()) return ID;
        return "";
    }

    static JSONObject selectionTool() { return selectionTool(false); }
    static JSONObject selectionTool(boolean flexible) {
        List<String> options = flexible ? List.of(ID, COMPARE, CLARIFY) : List.of(ID);
        var properties = new JSONObject().put("procedure", new JSONObject().put("type", "string").put("enum", options));
        var schema = new JSONObject().put("type", "object").put("properties", properties)
                .put("additionalProperties", false).put("required", List.of("procedure"));
        var function = new JSONObject().put("name", SELECT_TOOL.replace('.', '_')).put("parameters", schema)
                .put("description", flexible ? "Select the requested local ramp procedure or ask for clarification." :
                        "Select the registered local ramp experiment. Java binds its existing input revision and verifies every stage.");
        return new JSONObject().put("type", "function").put("function", function);
    }

    static JSONObject catalog() {
        var stages = new JSONArray();
        for (Step step : Step.values()) stages.put(step.describe());
        return new JSONObject().put("id", ID).put("version", 1).put("stages", stages)
                .put("inputs", List.of(Format.FORM.id, Format.CSV.id))
                .put("modelSelection", selectionTool().getJSONObject("function"))
                .put("limits", new JSONObject().put("secondsMin", MIN_SECONDS).put("secondsMax", MAX_SECONDS)
                        .put("tickHz", (int) (1 / BulletRamp.DT)).put("sampleTicks", SAMPLE_TICKS)
                        .put("replayTolerance", REPLAY_TOLERANCE));
    }

    static JSONObject describe(String procedure) {
        if (ID.equals(procedure)) return catalog();
        if (!COMPARE.equals(procedure)) throw new IllegalArgumentException("Unknown local ramp procedure");
        return catalog().put("id", COMPARE).put("inputs", List.of(Format.PAIRED_CSV.id))
                .put("requiredSetups", 2).put("clock", "same duration")
                .put("result", "two independently replayed trajectories and signed distance difference")
                .put("modelSelection", selectionTool(true).getJSONObject("function"));
    }

    static JSONObject registry() {
        var procedures = new JSONArray();
        for (String id : List.of(ID, COMPARE)) procedures.put(describe(id).put("sha256", hash(id)));
        return catalog().put("sha256", hash()).put("procedures", procedures);
    }

    static String hash() { return hash(ID); }
    static String hash(String procedure) { return WorldFrames.hash(describe(procedure)); }

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
        if (task.has("contractSha256") && !hash(task.optString("procedure", ID)).equals(task.getString("contractSha256"))) {
            throw new IllegalStateException("The installed procedure contract changed");
        }
    }

    private static JSONObject receipt(Step step, JSONObject payload) {
        return new JSONObject().put("name", step.receipt).put("status", "verified")
                .put("sha256", WorldFrames.hash(payload));
    }
}
