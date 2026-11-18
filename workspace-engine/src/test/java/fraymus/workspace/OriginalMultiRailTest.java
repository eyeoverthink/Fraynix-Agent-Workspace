
package fraymus.workspace;

import fraymus.neural.RelayCourse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class OriginalMultiRailTest {
    private static final List<String> PACES = List.of("normal", "steady", "careful");
    private static final Map<RelayCourse, List<String>> RAILS = Map.of(
            RelayCourse.SWITCHYARD, List.of("West rail", "East rail"),
            RelayCourse.RIDGE, List.of("First ridge rail", "Second ridge rail", "Lower rail"));

    @Test public void everyRailClearsAtThreeMeasuredApproachSpeeds() throws Exception {
        Path repo = Path.of("..").toAbsolutePath().normalize();
        Path project = Files.createTempDirectory("multi-rail-navigation");
        Path output = Path.of("build/multi-rail-navigation/result.json");
        Files.createDirectories(output.getParent());
        var result = new JSONObject().put("passed", false);
        var runs = new JSONArray();
        result.put("runs", runs);
        try (var game = new EmbeddedGameClient(repo, project)) {
            for (RelayCourse course : List.of(RelayCourse.SWITCHYARD, RelayCourse.RIDGE)) {
                Map<String, float[]> measured = new LinkedHashMap<>();
                Map<String, float[]> entries = new LinkedHashMap<>();
                for (String name : RAILS.get(course)) {
                    measured.put(name, new float[PACES.size()]);
                    entries.put(name, new float[PACES.size()]);
                }
                for (int i = 0; i < PACES.size(); i++) {
                    String pace = PACES.get(i);
                    var opening = game.openCourse(course);
                    JSONObject state = opening.getJSONObject("state");
                    assertTrue(state.getBoolean("railPaceAvailable"));
                    assertFalse(state.has("railNavigation"));
                    double target = opening.getJSONObject("content").getDouble("finishX") + 1;
                    JSONObject run = new JSONObject().put("course", course.id()).put("pace", pace);
                    runs.put(run);
                    var trace = new JSONArray();
                    run.put("trace", trace);
                    for (int chunk = 0; chunk < 75 && !terminal(state); chunk++) {
                        JSONObject args = command(pace, target);
                        state = game.command(args).getJSONObject("state");
                        trace.put(new JSONObject().put("tick", state.getLong("tick"))
                                .put("player", state.getJSONObject("player"))
                                .put("rail", state.getJSONObject("railNavigation"))
                                .put("input", state.getJSONObject("controller").getBoolean("jump")));
                        if (chunk == 0 && i == 0) {
                            assertThrows(IllegalStateException.class,
                                    () -> game.command(command("steady", target)));
                            assertThrows(IllegalStateException.class,
                                    () -> game.command(command("bogus", target)));
                            assertEquals(state.getLong("tick"), game.frame().getJSONObject("state").getLong("tick"));
                        }
                    }
                    run.put("final", state);
                    assertTrue(course.title() + " " + pace + " reaches beacon", state.getJSONObject("player").getBoolean("won"));
                    assertFalse(state.getJSONObject("courseProgress").has("hazard"));
                    assertEquals("contact122", state.getJSONObject("controller").getString("policy"));
                    JSONObject navigation = state.getJSONObject("railNavigation");
                    assertEquals(pace, navigation.getString("pace"));
                    assertEquals(0, navigation.getInt("rejectedMeasurements"));
                    JSONArray detected = navigation.getJSONArray("rails");
                    JSONArray crossed = navigation.getJSONArray("crossings");
                    assertEquals(RAILS.get(course).size(), detected.length());
                    assertEquals(detected.length(), crossed.length());
                    assertEquals(2, navigation.getInt("launches"));
                    for (int j = 0; j < crossed.length(); j++) {
                        JSONObject fixture = detected.getJSONObject(j), witness = crossed.getJSONObject(j);
                        String name = RAILS.get(course).get(j);
                        assertEquals(name, fixture.getString("name"));
                        assertEquals(name, witness.getString("name"));
                        assertEquals(fixture.getString("id"), witness.getString("id"));
                        assertTrue(witness.getLong("launchTick") < witness.getLong("entryTick"));
                        assertTrue(witness.getLong("entryTick") < witness.getLong("exitTick"));
                        assertTrue(witness.getInt("overlapSamples") >= 5);
                        assertTrue(witness.getDouble("measuredClearance") > .035);
                        assertTrue(witness.getDouble("predictedClearance") >= .035);
                        assertTrue(witness.getDouble("entrySpeed") > 0);
                        measured.get(name)[i] = witness.getNumber("launchSpeed").floatValue();
                        entries.get(name)[i] = witness.getNumber("entrySpeed").floatValue();
                    }
                    if (course == RelayCourse.RIDGE) {
                        assertEquals(crossed.getJSONObject(0).getLong("launchTick"), crossed.getJSONObject(1).getLong("launchTick"));
                        assertTrue(crossed.getJSONObject(0).getLong("exitTick") < crossed.getJSONObject(1).getLong("entryTick"));
                    }
                    JSONObject reset = game.command(new JSONObject().put("action", "reset")).getJSONObject("state");
                    assertFalse(reset.has("railNavigation"));
                    assertFalse(terminal(reset));
                    assertEquals(0, reset.getJSONObject("courseProgress").getJSONArray("pickups").length());
                }
                for (var entry : measured.entrySet()) {
                    float[] speed = entry.getValue();
                    assertTrue(entry.getKey() + " needs a measured steady reduction", speed[0] - speed[1] > .4f);
                    assertTrue(entry.getKey() + " needs a measured careful reduction", speed[1] - speed[2] > .2f);
                    assertTrue(entry.getKey() + " needs a moving careful approach", speed[2] > .6f);
                    float[] overlap = entries.get(entry.getKey());
                    assertTrue(entry.getKey() + " needs a distinct steady entry", overlap[0] - overlap[1] > .10f);
                    assertTrue(entry.getKey() + " needs a distinct careful entry", overlap[1] - overlap[2] > .07f);
                }
            }
            game.openCourse(RelayCourse.CIRCUIT);
            assertThrows(IllegalStateException.class, () -> game.command(command("steady", 13.625)));
            assertFalse(game.frame().getJSONObject("state").has("railNavigation"));
            result.put("passed", true);
        } finally {
            Files.writeString(output, result.toString(2));
        }
    }

    private static JSONObject command(String pace, double target) {
        return new JSONObject().put("action", "ai-step").put("ticks", 12)
                .put("targetX", target).put("railPace", pace);
    }

    private static boolean terminal(JSONObject state) {
        JSONObject player = state.getJSONObject("player");
        return player.getBoolean("dead") || player.getBoolean("won");
    }
}
