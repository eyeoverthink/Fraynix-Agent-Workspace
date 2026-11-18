package fraymus.workspace;

import java.nio.file.*;
import java.util.Base64;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class OriginalGameContentTest {
    @Test public void originalPackMustMatchBothSourceAndInstalledBytes() throws Exception {
        Path repo = Path.of("..").toAbsolutePath().normalize(), copy = Files.createTempDirectory("relay-verify");
        try (var paths = Files.list(repo.resolve(GameContentPack.ROOT))) {
            for (Path p : paths.filter(Files::isRegularFile).toList()) {
                for (Path base : new Path[]{copy, copy.resolve("build/install/Fraynix-Studio")}) {
                    Path dest = base.resolve(GameContentPack.ROOT).resolve(p.getFileName());
                    Files.createDirectories(dest.getParent()); Files.copy(p, dest);
                }
            }
        }
        var pack = new GameContentPack(copy); assertTrue(pack.verify().getBoolean("checked"));
        Path asset = copy.resolve("build/install/Fraynix-Studio/" + GameContentPack.ROOT + "atlas.png");
        byte[] original = Files.readAllBytes(asset), changed = original.clone(); changed[changed.length - 1] ^= 1;
        var time = Files.getLastModifiedTime(asset); Files.write(asset, changed); Files.setLastModifiedTime(asset, time);
        assertThrows(IllegalStateException.class, pack::verify); Files.write(asset, original);
        Path level = copy.resolve(GameContentPack.ROOT + "level.json"); Files.writeString(level, " ", StandardOpenOption.APPEND);
        assertThrows(IllegalStateException.class, pack::verify);
    }

    @Test public void actualPhysicsCompletesJumpsFailsWithoutThemAndRestarts() throws Exception {
        Path repo = Path.of("..").toAbsolutePath().normalize(), project = Files.createTempDirectory("relay-game");
        Path out = Path.of("build/original-content"); Files.createDirectories(out);
        JSONObject result = new JSONObject().put("passed", false).put("inputOnly", true);
        try (var game = new EmbeddedGameClient(repo, project)) {
            JSONObject frame = game.openRelay(), initial = frame.getJSONObject("state");
            result.put("start", initial); image(out, "start", frame);
            assertFalse(initial.getBoolean("visibleWindow")); assertFalse(initial.getBoolean("editorInitialized"));
            assertEquals(GameContentPack.ID, initial.getString("contentId"));
            assertEquals(17, initial.getJSONArray("contentPaths").length());
            for (Object path : initial.getJSONArray("contentPaths")) assertTrue(path.toString().startsWith(GameContentPack.ROOT));
            JSONArray trace = new JSONArray(), gaps = frame.getJSONObject("content").getJSONArray("gaps");
            for (int step = 0; step < 180; step++) {
                JSONObject player = frame.getJSONObject("state").getJSONObject("player");
                trace.put(frame.getJSONObject("state")); if (player.getBoolean("won") || player.getBoolean("dead")) break;
                double x = player.getDouble("x"); boolean jump = false;
                for (Object value : gaps) { var gap = (JSONObject) value; if (x > gap.getDouble("startX") - .7 && x < gap.getDouble("endX") + .1) jump = true; }
                frame = game.command(new JSONObject().put("action", "step").put("ticks", 12).put("horizontal", 1).put("jump", jump));
            }
            result.put("trace", trace).put("finish", frame.getJSONObject("state")); image(out, "finish", frame);
            assertTrue("Finish requires actual sensor contact", frame.getJSONObject("state").getJSONObject("player").getBoolean("won"));
            assertNotEquals(initial.getJSONObject("player").getDouble("x"), frame.getJSONObject("state").getJSONObject("player").getDouble("x"), .1);
            frame = game.command(new JSONObject().put("action", "reset"));
            assertEquals(initial.getJSONObject("player").getDouble("x"), frame.getJSONObject("state").getJSONObject("player").getDouble("x"), .0001);
            for (int n = 0; n < 100 && !frame.getJSONObject("state").getJSONObject("player").getBoolean("dead"); n++)
                frame = game.command(new JSONObject().put("action", "step").put("ticks", 12).put("horizontal", 1).put("jump", false));
            result.put("noJump", frame.getJSONObject("state"));
            assertTrue("Gap must be real", frame.getJSONObject("state").getJSONObject("player").getBoolean("dead"));
            game.close(); frame = game.openRelay(); result.put("reopened", frame.getJSONObject("state"));
            assertNotEquals(initial.getString("sessionId"), frame.getJSONObject("state").getString("sessionId"));
            assertEquals(initial.getJSONObject("player").getString("id"), frame.getJSONObject("state").getJSONObject("player").getString("id"));
            assertFalse(frame.getJSONObject("state").getJSONObject("player").getBoolean("won"));
            result.put("passed", true);
        } finally { Files.writeString(out.resolve("result.json"), result.toString(2)); }
    }

    private static void image(Path out, String name, JSONObject frame) throws Exception {
        Files.write(out.resolve(name + ".png"), Base64.getDecoder().decode(frame.getString("pngBase64")));
    }
}
