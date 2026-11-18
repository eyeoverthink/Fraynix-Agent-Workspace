package fraymus.workspace;

import fraymus.neural.RelayCourse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class OriginalRailsTest {
    @Test public void nativeAiForecastsClearsAndFinishesBothCourses() throws Exception {
        Path repo = Path.of("..").toAbsolutePath().normalize();
        Path project = Files.createTempDirectory("rail-navigation-test");
        Path output = Path.of("build/rail-navigation/result.json");
        Files.createDirectories(output.getParent());
        JSONArray trials = new JSONArray();
        JSONObject result = new JSONObject().put("passed", false).put("trials", trials);
        try (var game = new EmbeddedGameClient(repo, project)) {
            for (RelayCourse course : List.of(RelayCourse.CIRCUIT, RelayCourse.TERRACES)) {
                var opening = game.openCourse(course);
                JSONObject state = opening.getJSONObject("state");
                double destination = opening.getJSONObject("content").getDouble("finishX") + 1;
                assertFalse(state.has("railNavigation"));
                JSONArray trace = new JSONArray();
                JSONObject trial = new JSONObject().put("id", course.id()).put("trace", trace).put("initial", state);
                trials.put(trial);
                for (int chunk = 0; chunk < 100 && !terminal(state); chunk++) {
                    state = game.command(new JSONObject().put("action", "ai-step").put("ticks", 6)
                            .put("targetX", destination)).getJSONObject("state");
                    trace.put(new JSONObject().put("tick", state.getLong("tick"))
                            .put("player", state.getJSONObject("player"))
                            .put("rails", state.getJSONObject("railNavigation"))
                            .put("input", state.getJSONObject("controller").getBoolean("jump")));
                }
                trial.put("final", state);
                assertEquals("contact122", state.getJSONObject("controller").getString("policy"));
                assertTrue(course.title(), state.getJSONObject("player").getBoolean("won"));
                assertFalse(state.getJSONObject("player").getBoolean("dead"));
                assertFalse(state.getJSONObject("courseProgress").has("hazard"));
                JSONObject navigation = state.getJSONObject("railNavigation");
                assertEquals(1, navigation.getJSONArray("rails").length());
                assertEquals(1, navigation.getInt("launches"));
                assertEquals(0, navigation.getInt("rejectedMeasurements"));
                assertEquals(1, navigation.getJSONArray("crossings").length());
                JSONObject observed = navigation.getJSONArray("crossings").getJSONObject(0);
                JSONObject rail = navigation.getJSONArray("rails").getJSONObject(0);
                assertEquals(rail.getString("id"), observed.getString("id"));
                assertTrue(observed.getLong("launchTick") < observed.getLong("entryTick"));
                assertTrue(observed.getLong("entryTick") < observed.getLong("exitTick"));
                assertTrue(observed.getInt("overlapSamples") >= 5);
                assertTrue(observed.getDouble("measuredClearance") > .035);
                assertTrue(observed.getDouble("predictedClearance") > .035);
                state = game.command(new JSONObject().put("action", "reset")).getJSONObject("state");
                assertFalse(state.has("railNavigation"));
                assertFalse(terminal(state));
                assertEquals(0, state.getJSONObject("courseProgress").getJSONArray("pickups").length());
            }

            for (var opening : List.of(game.openRelay(), game.open())) {
                assertFalse(opening.getJSONObject("state").has("railNavigation"));
            }
            game.openRelay();
            JSONObject garden = game.command(new JSONObject().put("action", "ai-step")
                    .put("ticks", 6).put("targetX", 14.625)).getJSONObject("state");
            assertFalse(garden.has("railNavigation"));
            assertEquals("contact122", garden.getJSONObject("controller").getString("policy"));
            result.put("passed", true);
        } finally {
            Files.writeString(output, result.toString(2));
        }
    }

    private static boolean terminal(JSONObject state) {
        JSONObject player = state.getJSONObject("player");
        return player.getBoolean("dead") || player.getBoolean("won");
    }
}
