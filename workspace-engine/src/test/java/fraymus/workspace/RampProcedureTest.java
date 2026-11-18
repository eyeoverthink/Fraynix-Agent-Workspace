package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.*;

public class RampProcedureTest {
    private static final String CSV = "\uFEFFduration_s,friction,mass_kg,gravity_mps2,angle_deg\r\n"
            + "\"0.83\",0.025,1.45,10.9,35\r\n";

    @Test public void acceptsOneQuotedReorderedRowAndRetainsItsExactSourceThroughRestart() throws Exception {
        Path project = Files.createTempDirectory("ramp-csv");
        JSONObject input;
        try (var store = new WorkspaceStore(project)) {
            var pages = new RampInputPage(store);
            input = pages.save(new JSONObject().put("csv", CSV));
            assertEquals(.83, input.getJSONObject("values").getDouble("seconds"), 0);
            assertEquals(35, input.getJSONObject("values").getDouble("angleDeg"), 0);
            assertEquals(MeasurementStore.hash(CSV), input.getString("csvSha256"));
            assertThrows(IllegalArgumentException.class, () -> MeasurementStore.csv(CSV));
        }
        try (var store = new WorkspaceStore(project)) {
            var pages = new RampInputPage(store);
            assertTrue(input.similar(pages.bind(input.getString("path"), true)));
            assertEquals(CSV, pages.originalCsv(input));
            Path original = store.asset(input.getString("id"), input.getInt("version"), "ramp-source.csv");
            Files.writeString(original, CSV.replace("\r\n", "\n"));
            assertThrows(IllegalStateException.class, () -> pages.bind(input.getString("path"), true));
            assertThrows(IllegalStateException.class, () -> pages.originalCsv(input));
        }
    }

    @Test public void refusesExtraRowsDuplicateHeadersAndFormulaValues() {
        assertThrows(IllegalArgumentException.class, () -> RampInputPage.parseCsv(CSV + ".7,0,1,9.81,25\r\n"));
        assertThrows(IllegalArgumentException.class, () -> RampInputPage.parseCsv(CSV.replace("angle_deg", "mass_kg")));
        assertThrows(IllegalArgumentException.class, () -> RampInputPage.parseCsv(CSV.replace("\"0.83\"", "=1+2")));
        assertThrows(IllegalArgumentException.class, () -> RampInputPage.parseCsv(CSV.replace("35", "55")));
    }

    @Test public void directAndPlannerAdmissionShareTheSchemaAndPreserveTheRequestedPause() {
        var direct = RampProcedure.intent("Run the local ramp experiment and pause after reading.").directChoice();
        String request = "Plan the local ramp experiment and pause after reading.";
        assertTrue(direct.similar(RampProcedure.admit(request, direct)));
        assertThrows(IllegalArgumentException.class, () -> RampProcedure.admit(request,
                WorldFrames.copy(direct).put("pauseAfterReading", false)));
        assertThrows(IllegalArgumentException.class, () -> RampProcedure.admit(request,
                WorldFrames.copy(direct).put("procedure", "unregistered")));
        assertThrows(IllegalArgumentException.class, () -> RampProcedure.admit(request,
                WorldFrames.copy(direct).put("source", "foreign")));
    }

    @Test public void aReceiptCannotSkipAStageOrSubstituteItsPayload() {
        var stored = new JSONObject().put("stage", "browser").put("stages", new JSONArray())
                .put("contractSha256", RampProcedure.hash());
        var observation = new JSONObject().put("path", "bound");
        assertThrows(IllegalStateException.class, () -> RampProcedure.append(stored, RampProcedure.Step.RUN, observation));
        RampProcedure.append(stored, RampProcedure.Step.OBSERVE, observation);
        RampProcedure.verify(stored, List.of(observation));
        assertThrows(IllegalStateException.class, () -> RampProcedure.verify(stored, List.of(new JSONObject().put("path", "other"))));
    }
}
