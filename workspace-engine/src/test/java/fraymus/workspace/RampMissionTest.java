package fraymus.workspace;

import org.json.JSONObject;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.Assert.*;

public class RampMissionTest {
    private static JSONObject values() {
        return new JSONObject().put("angleDeg", 25).put("friction", .08)
                .put("gravityMps2", 9.81).put("massKg", 1).put("seconds", .77);
    }

    @Test public void pageBindingRejectsChangedBytesAndNonNumericInputs() throws Exception {
        try (var store = new WorkspaceStore(Files.createTempDirectory("ramp-source"))) {
            var pages = new RampInputPage(store);
            assertThrows(IllegalArgumentException.class, () -> pages.save(new JSONObject().put("values", values().put("angleDeg", "25"))));
            assertThrows(IllegalArgumentException.class, () -> pages.save(new JSONObject().put("values", values().put("seconds", 3))));
            assertThrows(IllegalArgumentException.class, () -> pages.bind("https://example.test/ramp", true));
            JSONObject v1 = pages.save(new JSONObject().put("values", values()));
            String id = v1.getString("id");
            JSONObject v2 = pages.save(new JSONObject().put("id", id).put("expectedVersion", 1).put("values", values().put("seconds", 1.2)));
            assertEquals(2, v2.getInt("version"));
            assertThrows(IllegalStateException.class, () -> pages.bind(v1.getString("path"), true));
            assertTrue(v1.similar(pages.bind(v1.getString("path"), false)));
            Path file = store.asset(id, 2, "index.html");
            Files.writeString(file, Files.readString(file).replace(">25<", ">7<"));
            assertThrows(IllegalStateException.class, () -> pages.bind(v2.getString("path"), true));
        }
    }

    @Test public void eachNativeStagePersistsAcrossReopenAndAChangedTraceFails() throws Exception {
        Path project = Files.createTempDirectory("local-ramp-mission");
        String id = UUID.randomUUID().toString();
        try (var store = new WorkspaceStore(project)) {
            var mission = new RampMission(project, store);
            JSONObject source = mission.inputs.save(new JSONObject().put("values", values()));
            JSONObject started = mission.start(new JSONObject().put("id", id).put("request", "Run the local ramp experiment.")
                    .put("source", source));
            assertEquals("waiting-host", started.getString("status"));
            JSONObject observation = new JSONObject().put("path", source.getString("path"))
                    .put("rows", RampInputPage.rows(source.getJSONObject("values"))).put("visible", false);
            assertThrows(IllegalStateException.class, () -> mission.observe(id, observation));
            mission.observe(id, observation.put("visible", true));
            JSONObject initial = mission.advance(id);
            assertEquals("replay", initial.getString("stage"));
            var reopened = new RampMission(project, store);
            assertTrue(reopened.read(id).similar(initial));
            JSONObject replayed = reopened.advance(id);
            assertEquals("report", replayed.getString("stage"));
            assertEquals(185, replayed.getJSONObject("audit").getInt("ticks"));
            assertEquals(17, replayed.getJSONObject("audit").getInt("sampleCount"));
            assertNotEquals(replayed.getJSONObject("original").getString("sessionId"), replayed.getJSONObject("replay").getString("sessionId"));
            var review = new RampMission(project, store);
            assertEquals("ready-report", review.advance(id).getString("status"));
            assertThrows(IllegalStateException.class, () -> review.verify(id));
            assertThrows(IllegalStateException.class, () -> review.acceptReport(id, new JSONObject()));
            Path checkpoint = project.resolve("ramp-missions").resolve(id + ".json");
            JSONObject data = AgentFiles042.loadChecked(checkpoint);
            data.getJSONObject("replay").getJSONArray("samples").getJSONObject(1)
                    .getJSONArray("position").put(0, 50.0);
            AgentFiles042.saveChecked(checkpoint, data);
            assertThrows(IllegalStateException.class, () -> review.reportCheck(id));
        }
    }
}
