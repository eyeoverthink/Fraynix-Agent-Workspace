package content;

import com.google.gson.*;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioSystem;
import org.junit.Test;
import static org.junit.Assert.*;

public class RelayGardenGeneratorTest {
    @Test public void reproducibleOriginalPixelsLayoutAndAudio() throws Exception {
        Path generated = Files.createTempDirectory("relay-generator").resolve("pack");
        RelayGardenGenerator.generate(generated);
        Path saved = Path.of(RelayGardenGenerator.ROOT);
        try (var paths = Files.list(generated)) {
            for (Path p : paths.toList()) assertArrayEquals(p.getFileName().toString(),
                Files.readAllBytes(p), Files.readAllBytes(saved.resolve(p.getFileName())));
        }
        assertThrows(IllegalArgumentException.class, () -> RelayGardenGenerator.generate(generated));
        var atlas = ImageIO.read(generated.resolve("atlas.png").toFile());
        assertEquals(256, atlas.getWidth()); assertEquals(64, atlas.getHeight());
        assertNotEquals(atlas.getRGB(18, 9), atlas.getRGB(0, 0));
        for (String cue : RelayGardenGenerator.CUES) {
            try (var audio = AudioSystem.getAudioInputStream(generated.resolve(cue + ".wav").toFile())) {
                assertEquals(22050, audio.getFormat().getSampleRate(), 0);
                assertEquals(1, audio.getFormat().getChannels());
                assertTrue(audio.getFrameLength() > 4000);
                byte[] bytes = audio.readAllBytes(); int nonzero = 0;
                for (byte b : bytes) if (b != 0) nonzero++;
                assertTrue("Original cue is audible PCM, not silence", nonzero > bytes.length / 2);
            }
        }
    }

    @Test public void originalLayoutHasStableIdentitiesAndPlayableComponents() {
        JsonArray level = RelayGardenGenerator.level();
        var ids = new java.util.HashSet<String>(); int players = 0, goals = 0, ground = 0;
        for (var element : level) {
            var object = element.getAsJsonObject(); assertTrue(ids.add(object.get("persistentId").getAsString()));
            for (var value : object.getAsJsonArray("components")) {
                var component = value.getAsJsonObject();
                assertTrue(ids.add(component.getAsJsonObject("properties").get("persistentId").getAsString()));
                switch (component.get("type").getAsString()) {
                    case "components.PlayerController" -> players++;
                    case "components.Flagpole" -> goals++;
                    case "components.Ground" -> ground++;
                }
            }
        }
        assertEquals(1, players); assertEquals(6, goals); assertEquals(112, ground);
        assertFalse(level.toString().contains("assets/images/"));
        assertFalse(level.toString().contains("mario-original"));
    }
}
