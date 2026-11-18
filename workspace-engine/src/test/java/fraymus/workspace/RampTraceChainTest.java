package fraymus.workspace;

import org.json.JSONObject;
import org.junit.Test;

import java.nio.file.Files;
import java.util.UUID;

import static org.junit.Assert.*;

public class RampTraceChainTest {
    @Test
    public void onlyTheReviewedContractCanBeAdmitted() {
        assertEquals(RampTraceChain.FAMILY, ChatPolicy044.route(RampTraceChain.REQUEST, new JSONObject(), false));
        assertFalse(RampTraceChain.plan(RampTraceChain.REQUEST).getBoolean("pauseAfterComparison"));
        assertTrue(RampTraceChain.plan(RampTraceChain.REQUEST + " Pause after comparison.").getBoolean("pauseAfterComparison"));
        assertThrows(IllegalArgumentException.class, () -> RampTraceChain.plan(RampTraceChain.REQUEST + " Add another step."));
    }

    @Test
    public void anInterruptedImportAndChildStartReopenUnderTheSameIdentities() throws Exception {
        var root = Files.createTempDirectory("ramp-trace-chain");
        String parent = UUID.randomUUID().toString(), dataset = UUID.randomUUID().toString(), child = RampTraceChain.workflowId(parent);
        var handoff = new JSONObject().put("datasetId", dataset).put("workflowId", child)
                .put("winner", "second").put("nativeTraceSha256", "a".repeat(64))
                .put("traceCsv", "time_s,displacement_m\n0,0\n0.05,0.1\n0.10,0.3\n");
        handoff.put("traceSha256", MeasurementStore.hash(handoff.getString("traceCsv")));
        var source = RampTraceChain.dataset(handoff);
        var start = new JSONObject().put("title", "Owned summary").put("request", "Summarize the verified motion")
                .put("profile101", DataSummary104.KIND).put("modelName", "java-owned-summary")
                .put("datasetId", dataset).put("datasetVersion", 1);
        try (var f = new WorkflowTest.Fixture(root)) {
            var saved = f.data.importSimulationOnce(dataset, source);
            assertTrue(new JSONObject(saved.toString()).similar(f.data.importSimulationOnce(dataset, source)));
            RampTraceChain.requireLinked(handoff, f.workflows.startOwnedSummary(child, start));
        }
        try (var f = new WorkflowTest.Fixture(root)) {
            assertEquals(1, f.data.datasets().length());
            assertEquals(1, f.data.importSimulationOnce(dataset, source).getInt("version"));
            RampTraceChain.requireLinked(handoff, f.workflows.startOwnedSummary(child, start));
            assertEquals(1, f.workflows.list().length());
            assertThrows(IllegalStateException.class, () -> f.data.importSimulationOnce(dataset,
                    WorldFrames.copy(source).put("csv", "time_s,displacement_m\n0,0\n0.05,0.2\n")));
            assertThrows(IllegalStateException.class, () -> f.workflows.startOwnedSummary(child,
                    WorldFrames.copy(start).put("speedLimitMps", 4)));
        }
    }
}
