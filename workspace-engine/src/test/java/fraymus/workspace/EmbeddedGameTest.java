package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import java.io.*;
public class EmbeddedGameTest {
    @Test public void originalGameRunsHiddenWithoutEditorAndAcceptsActualRuntimeControls() throws Exception {
        var project=Files.createTempDirectory("embedded-game-test-");try(var game=new EmbeddedGameClient(Path.of(".."),project)){
            var first=game.open();var state=first.getJSONObject("state");assertFalse(state.getBoolean("visibleWindow"));assertFalse(state.getBoolean("editorInitialized"));assertTrue(state.getInt("objects")>100);
            var png=Base64.getDecoder().decode(first.getString("pngBase64"));var image=ImageIO.read(new ByteArrayInputStream(png));assertEquals(960,image.getWidth());assertEquals(540,image.getHeight());
            double before=state.getJSONObject("player").getDouble("x");var stepped=game.command(new JSONObject().put("action","step").put("ticks",60).put("horizontal",1).put("jump",false));
            assertEquals(60,stepped.getJSONObject("state").getLong("tick")-state.getLong("tick"));assertTrue(stepped.getJSONObject("state").getJSONObject("player").getDouble("x")>before+.1);
            assertNotEquals(first.getString("pngSha256"),stepped.getString("pngSha256"));
            assertThrows(IllegalStateException.class,()->game.command(new JSONObject().put("action","step").put("ticks",121)));
            var evidence=new JSONObject().put("passed",true).put("before",state).put("after",stepped.getJSONObject("state")).put("captureMs",stepped.getDouble("captureMs")).put("bridgeMs",stepped.getDouble("bridgeMs")).put("scope","Original Java OpenGL renderer, hidden context, no editor; fixed-step manual runtime, not a CPU-only renderer");
            Files.writeString(Path.of("build/embedded-game-evidence.json"),evidence.toString(2));Files.write(Path.of("build/embedded-game.png"),Base64.getDecoder().decode(stepped.getString("pngBase64")));
        }
        assertTrue(Files.isDirectory(project.resolve("embedded-game")));
    }
}
