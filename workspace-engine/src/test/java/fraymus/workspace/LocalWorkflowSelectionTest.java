package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

public class LocalWorkflowSelectionTest {
    @Test
    public void ordinaryRequestsAreBoundToOneProcedureAndTheOriginalWording() {
        String ramp = "Which of these two ramps will the block go farther down?";
        assertEquals(LocalWorkflowSelection.Procedure.RAMP, LocalWorkflowSelection.intent(ramp).direct());
        assertTrue(PlainWorkflowLanguage.preview(new JSONObject().put("request", ramp)).getBoolean("needsRampInput"));

        String recording = "Hey, can you tell me how far the object travelled in the selected motion recording?";
        var intent = LocalWorkflowSelection.intent(recording);
        assertEquals(LocalWorkflowSelection.Procedure.ANALYSIS, intent.direct());
        assertEquals(recording, intent.briefing());
        assertEquals(LocalWorkflowSelection.FAMILY, ChatPolicy044.route(recording, new JSONObject(), false));
        assertEquals(LocalWorkflowSelection.Procedure.ANALYSIS, LocalWorkflowSelection.intent(
                "How far did it travel in the selected motion recording?").direct());
        assertEquals(LocalWorkflowSelection.Procedure.CREATION, LocalWorkflowSelection.intent(
                "Could you whip up a little offline browser app that counts clicks and resets?").direct());

        var report = LocalWorkflowSelection.intent(
                "Read the selected motion recording and give me a checkable report on its travel and speed.");
        assertTrue(report.quick());
        assertNull(report.direct());
        assertNull(QuickWorkflowRouting.match(report.briefing()));
        for (String other : new String[]{
                "Do not compare these two ramps.",
                "Compare these two ramps, then convert the CSV to JSON.",
                "Compare these two ramps or summarize the selected motion recording.",
                "If we did this again, what would change?",
                "\"How far did it travel in the selected motion recording?\"",
                "How far did it travel in the selected motion recording? Email the answer."
        }) assertNull(other, LocalWorkflowSelection.intent(other));
    }

    @Test
    public void quickRoutingMatchesTheWholeBriefingAndPreservesSuppliedThresholds() {
        var ramp = LocalWorkflowSelection.intent("Route a local workflow: Compare the two prepared inclines on the same clock.");
        assertTrue(ramp.quick());
        assertEquals(LocalWorkflowSelection.Procedure.RAMP, QuickWorkflowRouting.match(ramp.briefing()).procedure());
        String briefing = "Analyse my selected motion recording for travel. Gap limit .12 seconds and speed limit 0.6 m/s.";
        assertEquals(LocalWorkflowSelection.Procedure.ANALYSIS, QuickWorkflowRouting.match(briefing).procedure());
        var fields = Requested044.fields(briefing, "data");
        assertEquals(0.12, fields.getDouble("maxGapS"), 0);
        assertEquals(0.6, fields.getDouble("speedLimitMps"), 0);
        assertEquals(LocalWorkflowSelection.Procedure.CREATION,
                QuickWorkflowRouting.match("Build a standalone offline web app for a tally that adds 2 and resets to 0 with controls #total, #add-two and #reset.").procedure());
        assertFalse(LocalWorkflowSelection.intent("Plan a local workflow: Summarize the selected motion CSV.").quick());
        for (String uncovered : new String[]{
                "\"Compare both saved ramp setups.\"",
                "Do not compare both saved ramp setups.",
                "Compare both saved ramp setups and summarize the selected motion dataset.",
                "If possible, compare both saved ramp setups.",
                "Summarize the selected motion CSV. Gap limit .12 seconds and speed limit 0.6 m/s. Send it by email.",
                "Build a standalone offline web app for a tally that adds 2 and resets to 0, then compare both ramps.",
                "I have selected ramp and motion inputs. What should I do?"
        }) assertNull(uncovered, QuickWorkflowRouting.match(uncovered));
    }

    @Test
    public void pickedReviewCannotBeSubstitutedOrRecordedTwice() {
        String id = DataSummary104.KIND;
        var task = new JSONObject().put("goal", "Route a local workflow: Find the distance in my recording.")
                .put("family044", LocalWorkflowSelection.FAMILY).put(QuickWorkflowRouting.REVIEW, id)
                .put(LocalWorkflowSelection.KEY, new JSONObject().put("routingPolicy", QuickWorkflowRouting.POLICY))
                .put("history", new JSONArray().put(new JSONObject().put("kind", "local-workflow-review")
                        .put("detail", new JSONObject().put("procedure", id))));
        LocalWorkflowSelection.verifySelection(task);
        assertThrows(IllegalStateException.class, () -> LocalWorkflowSelection.verifySelection(WorldFrames.copy(task)
                .put(QuickWorkflowRouting.REVIEW, RampProcedure.COMPARE)));
        task.put("selectedLocalWorkflow", id).put("family044", LocalWorkflowSelection.Procedure.ANALYSIS.family);
        task.getJSONArray("history").put(new JSONObject().put("kind", "local-workflow-selected")
                .put("detail", new JSONObject().put("procedure", id).put("by", "confirmed")));
        LocalWorkflowSelection.verifySelection(task);
        task.getJSONArray("history").getJSONObject(1).getJSONObject("detail").put("by", "model");
        assertThrows(IllegalStateException.class, () -> LocalWorkflowSelection.verifySelection(task));
    }

    @Test
    public void selectorHasNoAuthorityOverInputsOrOutcome() {
        var schema = Tools044.definition(LocalWorkflowSelection.TOOL).getJSONObject("function").getJSONObject("parameters");
        assertEquals(4, schema.getJSONObject("properties").getJSONObject("procedure").getJSONArray("enum").length());
        assertThrows(IllegalArgumentException.class, () -> ToolSchema043.validate(schema,
                new JSONObject().put("procedure", "creation-web-v1").put("passed", true)));
        assertThrows(IllegalArgumentException.class, () -> ToolSchema043.validate(schema,
                new JSONObject().put("procedure", "unknown")));
        ToolSchema043.validate(schema, new JSONObject().put("procedure", "clarify"));
    }

    @Test
    public void explicitRouteSurvivesTheExistingBroadRequestParsersAndRequiresSelectedInput() {
        String request = "Run local workflow analysis: Compare observed travel with past notes and check gaps.";
        var task = new JSONObject().put("goal", request).put("model", "")
                .put(LocalWorkflowSelection.KEY, new JSONObject());
        var intended = LocalWorkflowSelection.Procedure.ANALYSIS;
        assertEquals(intended, LocalWorkflowSelection.intent(request).direct());
        assertEquals(LocalWorkflowSelection.FAMILY, ChatPolicy044.route(request, new JSONObject(), false));
        assertTrue(LocalWorkflowSelection.missing(task, intended).contains("Select a motion dataset"));
        assertTrue(LocalWorkflowSelection.missing(task, LocalWorkflowSelection.Procedure.RAMP).contains("two setups"));
        assertTrue(LocalWorkflowSelection.missing(task, LocalWorkflowSelection.Procedure.CREATION).contains("installed local model"));
    }

    @Test
    public void durableChoiceAndFrozenSourcesCannotBeSilentlyRebound() {
        var task = new JSONObject().put("goal", "Plan a local workflow: Summarize the CSV.").put("model", "local")
                .put("family044", "workflow").put("selectedLocalWorkflow", DataSummary104.KIND)
                .put(LocalWorkflowSelection.KEY, new JSONObject().put("dataset", "pinned"));
        var queue = new JSONObject().put("kind", "queued")
                .put("detail", new JSONObject().put("objectiveSha256", ChatChain052.objectiveHash(task)));
        var selected = new JSONObject().put("kind", "local-workflow-selected")
                .put("detail", new JSONObject().put("procedure", DataSummary104.KIND).put("by", "model"));
        task.put("history", new JSONArray().put(queue).put(selected));
        ChatChain052.verifyObjective(task);
        assertThrows(IllegalStateException.class, () -> ChatChain052.verifyObjective(
                WorldFrames.copy(task).put("family044", "creation")));
        var altered = WorldFrames.copy(task);
        altered.getJSONObject(LocalWorkflowSelection.KEY).put("dataset", "different");
        assertThrows(IllegalStateException.class, () -> ChatChain052.verifyObjective(altered));
    }
}
