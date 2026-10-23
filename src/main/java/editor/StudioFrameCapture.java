package editor;

import java.awt.image.BufferedImage;
import java.nio.file.*;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL11.*;

/** Export the editor's own rendered framebuffer for native smoke-test visual review. */
public final class StudioFrameCapture {
    private StudioFrameCapture() {}
    public static void save(Path path, int width, int height) throws java.io.IOException {
        var pixels = BufferUtils.createByteBuffer(width * height * 4);
        glReadBuffer(GL_BACK); glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            int i = (y * width + x) * 4;
            image.setRGB(x, height - 1 - y, 0xff000000 | (pixels.get(i) & 255) << 16 | (pixels.get(i + 1) & 255) << 8 | (pixels.get(i + 2) & 255));
        }
        Files.createDirectories(path.toAbsolutePath().getParent());
        javax.imageio.ImageIO.write(image, "png", path.toFile());
    }
}
