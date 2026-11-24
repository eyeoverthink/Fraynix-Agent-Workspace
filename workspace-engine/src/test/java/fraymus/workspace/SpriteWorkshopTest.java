
package fraymus.workspace;

import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.util.Base64;

import static org.junit.Assert.*;

public class SpriteWorkshopTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void preservesPartialAlphaAndSmallSpriteAndDoesNotReplaceCheckedLevelOnInvalidLayout() throws Exception {
        var work = new SpriteWorkshop(temp.getRoot().toPath());
        var image = new BufferedImage(16, 12, BufferedImage.TYPE_INT_ARGB);
        for (int y = 3; y < 7; y++) {
            for (int x = 5; x < 10; x++) image.setRGB(x, y, 0x800C7DC2);
        }
        var source = new JSONObject().put("filename", "sample.png").put("png", png(image));
        var level = work.save(new JSONObject().put("source", source).put("layout", layout(302)));
        var sprite = level.getJSONObject("sprite");
        assertEquals(5, sprite.getInt("width"));
        assertEquals(4, sprite.getInt("height"));
        byte[] spriteBytes = Base64.getDecoder().decode(sprite.getString("png"));
        assertEquals(0x800C7DC2, BrowserShapeTrace.decode(spriteBytes, 1).getRGB(0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> work.save(new JSONObject().put("source", source).put("layout", layout(500))));
        assertEquals(level.getString("id"), work.latest(new JSONObject()).getString("id"));

        var restored = work.save(new JSONObject().put("fromId", level.getString("id")).put("layout", layout(304)));
        assertNotEquals(level.getString("id"), restored.getString("id"));
        assertEquals(sprite.getString("sha256"), restored.getJSONObject("sprite").getString("sha256"));
        assertTrue(Files.exists(temp.getRoot().toPath().resolve("media/" + level.getString("id") + ".json")));
    }

    @Test
    public void refusesANonContrastingSourceWithoutWritingAnything() throws Exception {
        var work = new SpriteWorkshop(temp.getRoot().toPath());
        var image = new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB);
        var source = new JSONObject().put("filename", "flat.png").put("png", png(image));
        assertThrows(IllegalArgumentException.class,
                () -> work.save(new JSONObject().put("source", source).put("layout", layout(302))));
        assertTrue(work.latest(new JSONObject()).isEmpty());
        assertFalse(Files.exists(temp.getRoot().toPath().resolve("media")));
    }

    private static JSONObject layout(int pickupX) {
        return new JSONObject().put("title", "Beacon").put("backdropX", 400).put("platformX", 276)
                .put("platformY", 242).put("pickupX", pickupX).put("foregroundX", 555);
    }

    private static String png(BufferedImage image) throws Exception {
        var bytes = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image, "PNG", bytes));
        return Base64.getEncoder().encodeToString(bytes.toByteArray());
    }
}
