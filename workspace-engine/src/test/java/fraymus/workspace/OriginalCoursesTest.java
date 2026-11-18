package fraymus.workspace;

import fraymus.neural.RelayCourse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class OriginalCoursesTest {
    private static final Path REPO = Path.of("..").toAbsolutePath().normalize();

    @Test public void bothCoursesCheckSharedAndOwnContentBeforeLaunch() throws Exception {
        Path copy = Files.createTempDirectory("relay-course-content");
        Path installed = copy.resolve("build/install/Fraynix-Studio");
        var paths = new HashSet<String>();
        for (RelayCourse course : List.of(RelayCourse.CIRCUIT, RelayCourse.TERRACES)) {
            paths.add(course.manifest());
            paths.addAll(course.files());
        }
        for (String name : paths) {
            for (Path base : List.of(copy, installed)) {
                Path target = base.resolve(name);
                Files.createDirectories(target.getParent());
                Files.copy(REPO.resolve(name), target);
            }
        }
        var pack = new GameContentPack(copy);
        for (RelayCourse course : List.of(RelayCourse.CIRCUIT, RelayCourse.TERRACES)) {
            assertTrue(pack.verify(course).getBoolean("checked"));
        }
        Path sprite = installed.resolve(RelayCourse.OBJECTS);
        byte[] saved = Files.readAllBytes(sprite), changed = saved.clone();
        changed[changed.length - 1] ^= 1;
        Files.write(sprite, changed);
        for (RelayCourse course : List.of(RelayCourse.CIRCUIT, RelayCourse.TERRACES)) {
            assertThrows(IllegalStateException.class, () -> pack.verify(course));
        }
        Files.write(sprite, saved);
        Files.delete(copy.resolve(RelayCourse.CIRCUIT.level()));
        assertThrows(IllegalStateException.class, () -> pack.verify(RelayCourse.CIRCUIT));
        assertTrue(pack.verify(RelayCourse.TERRACES).getBoolean("checked"));
    }

    @Test public void actualContactsJumpsResetsAndFinishOnBothOriginalCourses() throws Exception {
        Path project = Files.createTempDirectory("relay-courses-game");
        Path receipt = Path.of("build/original-courses/result.json");
        Files.createDirectories(receipt.getParent());
        JSONObject result = new JSONObject().put("passed", false);
        try (var game = new EmbeddedGameClient(REPO, project)) {
            var traces = new JSONArray();
            result.put("courses", traces);
            var playerIds = new HashSet<String>();
            for (RelayCourse course : List.of(RelayCourse.CIRCUIT, RelayCourse.TERRACES)) {
                JSONObject state = game.openCourse(course).getJSONObject("state");
                JSONObject record = new JSONObject().put("course", course.id());
                traces.put(record);
                assertEquals(course.id(), state.getString("contentId"));
                for (Object name : state.getJSONArray("contentPaths")) {
                    assertTrue(course.files().contains((String) name));
                }
                assertTrue(playerIds.add(player(state).getString("id")));
                record.put("initial", state);
                JSONArray trace = new JSONArray();
                record.put("trace", trace);
                boolean flat = course == RelayCourse.CIRCUIT;
                var jumps = flat ? List.of(3.50, 5.57) : List.of(2.95, 5.50, 8.20);
                int nextJump = 0;
                for (int n = 0; n < 130 && !terminal(state); n++) {
                    boolean jump = nextJump < jumps.size() && player(state).getDouble("x") >= jumps.get(nextJump);
                    if (jump) nextJump++;
                    state = step(game, jump ? 28 : 6, jump);
                    trace.put(compact(state, jump));
                }
                record.put("finish", state);
                assertTrue(course.title() + " must reach the actual finish sensor", player(state).getBoolean("won"));
                var seen = new HashSet<String>();
                for (Object pickup : progress(state).getJSONArray("pickups")) {
                    JSONObject event = (JSONObject) pickup;
                    assertTrue(event.getLong("tick") > 0 && event.getLong("tick") <= state.getLong("tick"));
                    assertTrue(seen.add(event.getString("name")));
                }
                assertEquals(flat ? Set.of("Copper cell", "Mint cell")
                        : Set.of("Lower cell", "Midway cell", "Return cell"), seen);
                assertFalse(progress(state).has("hazard"));
                state = reset(game);
                assertEquals(0, progress(state).getJSONArray("pickups").length());
                assertEquals(.875, player(state).getDouble("x"), .0001);
                if (!flat) {
                    for (int n = 0; n < 60 && player(state).getDouble("x") < 2.95; n++) state = step(game, 6, false);
                    state = step(game, 28, true);
                    for (int n = 0; n < 60 && player(state).getDouble("x") < 5.50; n++) state = step(game, 6, false);
                    state = step(game, 28, true);
                }
                for (int n = 0; n < 105 && !terminal(state); n++) state = step(game, 10, false);
                record.put("hazardAttempt", state);
                assertTrue(player(state).getBoolean("dead"));
                assertEquals(flat ? "Live rail" : "High rail", progress(state).getJSONObject("hazard").getString("name"));
                state = reset(game);
                record.put("reset", compact(state, false));
                assertFalse(progress(state).has("hazard"));
                assertFalse(terminal(state));
                assertEquals(0, progress(state).getJSONArray("pickups").length());
                JSONObject reopened = game.openCourse(course).getJSONObject("state");
                assertEquals(player(state).getString("id"), player(reopened).getString("id"));
                assertNotEquals(state.getString("sessionId"), reopened.getString("sessionId"));
                assertEquals(0, progress(reopened).getJSONArray("pickups").length());
            }
            result.put("passed", true);
        } finally {
            Files.writeString(receipt, result.toString(2));
        }
    }

    private static JSONObject player(JSONObject state) { return state.getJSONObject("player"); }
    private static JSONObject progress(JSONObject state) { return state.getJSONObject("courseProgress"); }

    private static boolean terminal(JSONObject state) {
        return player(state).getBoolean("won") || player(state).getBoolean("dead");
    }

    private static JSONObject step(EmbeddedGameClient game, int ticks, boolean jump) throws Exception {
        return game.command(new JSONObject().put("action", "step").put("ticks", ticks)
                .put("horizontal", 1).put("jump", jump)).getJSONObject("state");
    }

    private static JSONObject reset(EmbeddedGameClient game) throws Exception {
        return game.command(new JSONObject().put("action", "reset")).getJSONObject("state");
    }

    private static JSONObject compact(JSONObject state, boolean jump) {
        return new JSONObject().put("tick", state.getLong("tick")).put("player", player(state))
                .put("jump", jump).put("progress", progress(state));
    }
}
