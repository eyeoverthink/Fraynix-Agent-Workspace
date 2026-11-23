package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.regex.Pattern;

import static fraymus.workspace.LocalWorkflowSelection.Procedure;

/** Recognizes complete, single-outcome requests and leaves the original wording on the durable task. */
final class PlainWorkflowLanguage {
    private static final String POLITE = "(?:(?:hey(?: fraynix)?[,]? |please )?(?:can|could|would) you )?(?:please )?";
    private static final String ASK = "(?:tell me |show me |work out )?";
    private static final String RAMP_PAIR = "(?:these two|these|the two|both(?: saved)?|the two saved|the two prepared) (?:ramps|inclines|ramp setups)";
    private static final String RECORDING = "(?:the |my )?(?:selected|open|current) (?:motion (?:recording|dataset|csv)|recording)";
    private static final String END = "(?:,? please)?[.!?]?";

    private record Wording(Procedure procedure, Pattern pattern) {
        Wording(Procedure procedure, String text) {
            this(procedure, Pattern.compile(POLITE + text + END, Pattern.CASE_INSENSITIVE));
        }
    }

    private static final List<Wording> DIRECT = List.of(
            new Wording(Procedure.RAMP, "(?:compare|stack up) " + RAMP_PAIR + "(?: side by side| over the same time)?"),
            new Wording(Procedure.RAMP, ASK + "which (?:of " + RAMP_PAIR + "|ramp of the two) "
                    + "(?:goes (?:farther|further)|(?:will |would )?send(?:s)? (?:its|the) block (?:farther|further)|"
                    + "will (?:its|the) block go (?:farther|further)(?: down)?)"),
            new Wording(Procedure.ANALYSIS, "(?:summari[sz]e|analy[sz]e) " + RECORDING + "(?: for travel and speed)?"),
            new Wording(Procedure.ANALYSIS, ASK + "(?:how far (?:(?:it|the object|the block) (?:moved|travell?ed|went)|did (?:it|the object|the block) (?:move|travel|go))|"
                    + "how fast (?:(?:it|the object|the block) (?:moved|was moving|was going)|was (?:it|the object|the block) (?:moving|going))|"
                    + "how much ground (?:it|the object|the block) covered) (?:in|from) " + RECORDING),
            new Wording(Procedure.CREATION, "(?:make|build|create|whip up) (?:me )?an? (?:little |small |simple )?"
                    + "offline (?:browser|web) (?:app|tool) (?:that (?:counts clicks|adds one) and (?:can )?resets?|"
                    + "with (?:an? )?(?:add(?: one)?|count) and reset buttons?)")
    );

    private static final Wording REVIEW = new Wording(null, "(?:read|look at|take a look at) " + RECORDING
            + " and (?:give me|write|make) a (?:checkable|verified|short) report on (?:its )?(?:travel and speed|distance and speed)");

    private PlainWorkflowLanguage() {}

    static LocalWorkflowSelection.Intent intent(String text) {
        String request = text.strip();
        if (request.isEmpty() || request.length() > 1200) return null;
        String wording = request.replaceAll("[ \t]+", " ");
        for (var entry : DIRECT) {
            if (entry.pattern().matcher(wording).matches()) {
                return new LocalWorkflowSelection.Intent(entry.procedure(), request, false);
            }
        }
        return REVIEW.pattern().matcher(wording).matches()
                ? new LocalWorkflowSelection.Intent(null, request, true) : null;
    }

    static JSONArray counterChecks() {
        return new JSONArray()
                .put(counterValue(0)).put(counterClick("#add")).put(counterValue(1))
                .put(counterClick("#add")).put(counterValue(2))
                .put(counterClick("#reset")).put(counterValue(0))
                .put(counterClick("#add")).put(counterValue(1));
    }

    private static JSONObject counterValue(int value) {
        return new JSONObject().put("selector", "#count").put("expectText", Integer.toString(value));
    }

    private static JSONObject counterClick(String selector) {
        return new JSONObject().put("selector", selector).put("action", "click").put("expectCount", 1);
    }

    static JSONObject preview(JSONObject input) {
        SpatialScene.keys(input, "request");
        var intent = intent(input.getString("request"));
        return new JSONObject().put("matched", intent != null)
                .put("needsRampInput", intent != null && intent.direct() == Procedure.RAMP);
    }
}
