package fraymus.workspace;

import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;

final class CharacterFrames {
    private static final int WIDTH = 96;
    private static final int HEIGHT = 112;
    private static final int COUNT = 9;
    private static final long PNG = 0x89504e470d0a1a0aL;

    private CharacterFrames() {}

    static JSONObject validate(JSONObject input, TileWorkshop tiles) throws Exception {
        SpatialScene.keys(input, "frames", "player");
        var encoded = input.getJSONArray("frames");
        if (encoded.length() != COUNT) throw new IllegalArgumentException("The rig needs all nine poses.");
        var images = new BufferedImage[COUNT];
        var appearance = new HashSet<String>();
        for (int index = 0; index < COUNT; index++) {
            String frame = encoded.getString(index);
            if (frame.length() > 134_000) throw new IllegalArgumentException("The character frame is too large.");
            images[index] = decode(Base64.getDecoder().decode(frame));
            int[] pixels = images[index].getRGB(0, 0, WIDTH, HEIGHT, null, 0, WIDTH);
            var rgba = ByteBuffer.allocate(pixels.length * Integer.BYTES);
            for (int pixel : pixels) rgba.putInt(pixel >>> 24 == 0 ? 0 : pixel);
            if (!appearance.add(MapDefinition.sha(rgba.array())))
                throw new IllegalArgumentException("Different poses must have different rendered pixels.");
        }

        JSONObject player = input.optJSONObject("player");
        if (input.has("player") && player == null) throw new IllegalArgumentException("Invalid rig player.");
        if (player != null) {
            SpatialScene.keys(player, "id", "name");
            String id = player.getString("id");
            String name = player.getString("name");
            if (!id.matches("[0-9a-f]{64}") || name.isBlank() || name.length() > 64)
                throw new IllegalArgumentException("The character identity is invalid.");
            return tiles.addPlayer(id, name, sheet(images, List.of(0), 1), sheet(images, List.of(2, 3, 4, 5), 4));
        }

        byte[] atlas = sheet(images, List.of(0, 1, 2, 3, 4, 5, 6, 7, 8), 3);
        return new JSONObject().put("png", Base64.getEncoder().encodeToString(atlas))
                .put("sha256", MapDefinition.sha(atlas)).put("frames", COUNT);
    }

    private static BufferedImage decode(byte[] png) throws Exception {
        if (png.length < 33 || png.length > 100_000) throw new IllegalArgumentException("Invalid character PNG size.");
        var header = ByteBuffer.wrap(png);
        if (header.getLong() != PNG || header.getInt() != 13 || header.getInt() != 0x49484452
                || header.getInt() != WIDTH || header.getInt() != HEIGHT)
            throw new IllegalArgumentException("Character poses must be 96 × 112 PNGs.");
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        if (image == null || !image.getColorModel().hasAlpha())
            throw new IllegalArgumentException("Character poses need a transparent background.");

        int visible = 0;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int alpha = image.getRGB(x, y) >>> 24;
                if (alpha == 0) continue;
                if (x == 0 || x == WIDTH - 1 || y == 0 || y == HEIGHT - 1)
                    throw new IllegalArgumentException("The character pose touches the edge of its frame.");
                if (alpha >= 128) visible++;
            }
        }
        if (visible < 100 || visible > WIDTH * HEIGHT * 0.75)
            throw new IllegalArgumentException("No clear character silhouette inside the frame.");
        return image;
    }

    private static byte[] sheet(BufferedImage[] frames, List<Integer> order, int columns) throws Exception {
        int rows = (order.size() + columns - 1) / columns;
        var atlas = new BufferedImage(columns * WIDTH, rows * HEIGHT, BufferedImage.TYPE_INT_ARGB);
        var graphics = atlas.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Src);
            for (int index = 0; index < order.size(); index++)
                graphics.drawImage(frames[order.get(index)], index % columns * WIDTH, index / columns * HEIGHT, null);
        } finally {
            graphics.dispose();
        }
        var output = new ByteArrayOutputStream();
        ImageIO.write(atlas, "png", output);
        return output.toByteArray();
    }
}
