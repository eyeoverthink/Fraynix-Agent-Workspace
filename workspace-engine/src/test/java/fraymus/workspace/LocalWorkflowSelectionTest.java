package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

public class LocalWorkflowSelectionTest {
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
