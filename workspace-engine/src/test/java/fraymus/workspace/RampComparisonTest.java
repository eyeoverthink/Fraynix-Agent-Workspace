package fraymus.workspace;

import org.json.JSONObject;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.Assert.*;

public class RampComparisonTest {
    private static final String HEADER = "angle_deg,friction,gravity_mps2,mass_kg,duration_s\n";
    private static final String A = "38,0.04,9.81,1.2,0.85\n";
    private static final String B = "18,0.31,9.81,1.2,0.85\n";

    @Test public void comparesNativeMeasurementsAndRejectsExchangedSetupTraces() throws Exception {
        Path dir = Files.createTempDirectory("ramp-comparison");
        String id = UUID.randomUUID().toString();
        try (var store = new WorkspaceStore(dir)) {
            var runner = new RampMission(dir, store);
            JSONObject source = runner.inputs.save(new JSONObject().put("csv", HEADER+B+A));
            runner.start(new JSONObject().put("id", id).put("request", "Compare the local ramp setups.").put("source", source));
            JSONObject observation = new JSONObject().put("path", source.getString("path"))
                    .put("visible", true).put("rows", RampInputPage.rows(source.getJSONObject("values")))
                    .put("secondVisible", true).put("secondRows", RampInputPage.rows(source.getJSONObject("secondValues")));
            assertThrows(IllegalStateException.class, () -> runner.observe(id, WorldFrames.copy(observation).put("secondVisible", false)));
            runner.observe(id, observation);
            runner.advance(id);
            var reopened = new RampMission(dir, store);
            JSONObject paired = reopened.advance(id).getJSONObject("audit");
            assertEquals(204, paired.getInt("ticks"));
            assertEquals(18, paired.getInt("samplesPerSetup"));
            assertEquals("second", paired.getString("farther"));
            assertTrue(paired.getDouble("distanceDifferenceM") > 1);
            reopened.advance(id);
            assertEquals(9, reopened.reportCheck(id).getJSONArray("steps").length());
            assertThrows(IllegalStateException.class, () -> reopened.verify(id));
            Path checkpoint = dir.resolve("ramp-missions").resolve(id+".json");
            JSONObject saved = AgentFiles042.loadChecked(checkpoint);
            var replay = saved.getJSONObject("replay");
            var first = replay.getJSONObject("first");
            replay.put("first", replay.getJSONObject("second")).put("second", first);
            AgentFiles042.saveChecked(checkpoint, saved);
            assertThrows(IllegalStateException.class, () -> reopened.reportCheck(id));
        }
    }

    @Test public void comparisonNeedsTwoSetupsOnOneClock() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> RampInputPage.parseCsv(HEADER+A+B.replace("0.85", "0.90")));
        assertThrows(IllegalArgumentException.class, () -> RampInputPage.parseCsv(HEADER+A+B+A));
        Path dir = Files.createTempDirectory("ramp-missing-second");
        try (var store = new WorkspaceStore(dir)) {
            var runner = new RampMission(dir, store);
            var source = runner.inputs.save(new JSONObject().put("csv", HEADER+A));
            String id = UUID.randomUUID().toString();
            var request = new JSONObject().put("id", id).put("request", "Compare the local ramp setups.").put("source", source);
            assertThrows(IllegalStateException.class, () -> runner.start(request));
            assertFalse(Files.exists(dir.resolve("ramp-missions").resolve(id+".json")));
        }
    }

    @Test public void typedChoiceDistinguishesComparisonFromFirstSetupAndClarification() {
        String pair = "Plan a local ramp task and pause after reading: Compare both inputs.";
        var second = new JSONObject().put("procedure", RampProcedure.COMPARE);
        var single = new JSONObject().put("procedure", RampProcedure.ID);
        assertThrows(IllegalArgumentException.class, () -> RampProcedure.select(pair, second, false));
        assertTrue(RampProcedure.missingSetup(RampProcedure.intent(pair), new JSONObject()).contains("second setup"));
        assertTrue(RampProcedure.select(pair, second, true).getBoolean("pauseAfterReading"));
        assertThrows(IllegalArgumentException.class, () -> RampProcedure.select(pair, single, true));
        String first = "Plan a local ramp task: Give me the first setup's trajectory.";
        assertEquals(RampProcedure.ID, RampProcedure.select(first, single, true).getString("procedure"));
        assertThrows(IllegalArgumentException.class, () -> RampProcedure.select(first, second, true));
        assertEquals(RampProcedure.CLARIFY, RampProcedure.select(pair, new JSONObject().put("procedure", RampProcedure.CLARIFY), false)
                .getString("procedure"));
        assertThrows(IllegalArgumentException.class, () -> RampProcedure.select("Plan the local ramp experiment.", second, true));
    }
}
