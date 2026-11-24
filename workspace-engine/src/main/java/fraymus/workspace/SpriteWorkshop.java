
package fraymus.workspace;

import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;

final class SpriteWorkshop {
    private static final int MAX_PNG = 750_000;
    private static final int MAX_RECIPE = 1_050_000;
    private static final String FORMAT = "fraynix.sprite-level-v1";
    private final Path library;

    SpriteWorkshop(Path project) {
        library = project.resolve("media");
    }

    JSONObject isolate(JSONObject input) throws Exception {
        SpatialScene.keys(input, "source");
        return sprite(input.getJSONObject("source"));
    }

    synchronized JSONObject save(JSONObject input) throws Exception {
        SpatialScene.keys(input, "source", "fromId", "layout");
        if (input.has("source") == input.has("fromId"))
            throw new IllegalArgumentException("Choose one new image or one saved level.");
        var layout = input.getJSONObject("layout");
        if (input.has("source")) {
            var source = source(input.getJSONObject("source"));
            return persist(source, layout(layout), sprite(source));
        }
        var previous = load(input.getString("fromId"));
        var checked = check(previous);
        return persist(previous.getJSONObject("source"), layout(layout), checked.getJSONObject("sprite"));
    }

    synchronized JSONObject importLevel(JSONObject input) throws Exception {
        SpatialScene.keys(input, "base64");
        var recipe = parse(decode(input.getString("base64"), MAX_RECIPE));
        var checked = check(recipe);
        return persist(source(recipe.getJSONObject("source")), checked.getJSONObject("layout"),
                checked.getJSONObject("sprite"));
    }

    synchronized JSONObject latest(JSONObject input) throws Exception {
        SpatialScene.keys(input);
        Path pointer = library.resolve("latest");
        if (!Files.exists(pointer, LinkOption.NOFOLLOW_LINKS)) return new JSONObject();
        String id = new String(read(pointer, 64), StandardCharsets.UTF_8);
        return check(load(id)).put("id", id);
    }

    synchronized JSONObject export(JSONObject input) throws Exception {
        SpatialScene.keys(input, "id", "format");
        String id = input.getString("id");
        var recipe = load(id);
        var level = check(recipe);
        byte[] png = Base64.getDecoder().decode(level.getJSONObject("sprite").getString("png"));
        byte[] bytes = switch (input.getString("format")) {
            case "png" -> png;
            case "json" -> read(library.resolve(id + ".json"), MAX_RECIPE, id);
            case "html" -> portableGame(level);
            default -> throw new IllegalArgumentException("Choose PNG, editable level, or playable HTML.");
        };
        return new JSONObject().put("base64", Base64.getEncoder().encodeToString(bytes))
                .put("sha256", MapDefinition.sha(bytes)).put("spriteSha256", MapDefinition.sha(png))
                .put("sourceSha256", level.getJSONObject("sprite").getString("sourceSha256"));
    }

    private JSONObject persist(JSONObject source, JSONObject layout, JSONObject sprite) throws IOException {
        var recipe = new JSONObject().put("format", FORMAT).put("source", source).put("layout", layout)
                .put("sourceSha256", sprite.getString("sourceSha256")).put("spriteSha256", sprite.getString("sha256"));
        byte[] bytes = recipe.toString().getBytes(StandardCharsets.UTF_8);
        String id = MapDefinition.sha(bytes);
        Files.createDirectories(library);
        Path target = library.resolve(id + ".json");
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) read(target, MAX_RECIPE, id);
        else replace(target, bytes);
        replace(library.resolve("latest"), id.getBytes(StandardCharsets.UTF_8));
        return new JSONObject().put("id", id).put("layout", layout).put("sprite", sprite);
    }

    private JSONObject load(String id) throws Exception {
        if (!id.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Invalid saved level ID.");
        return parse(read(library.resolve(id + ".json"), MAX_RECIPE, id));
    }

    private static JSONObject parse(byte[] bytes) {
        return new JSONObject(new String(bytes, StandardCharsets.UTF_8));
    }

    private static JSONObject check(JSONObject recipe) throws Exception {
        SpatialScene.keys(recipe, "format", "source", "layout", "sourceSha256", "spriteSha256");
        if (!FORMAT.equals(recipe.getString("format"))) throw new IllegalArgumentException("Unsupported media level format.");
        var sprite = sprite(recipe.getJSONObject("source"));
        if (!sprite.getString("sourceSha256").equals(recipe.getString("sourceSha256"))
                || !sprite.getString("sha256").equals(recipe.getString("spriteSha256")))
            throw new IllegalArgumentException("The image or sprite pixels differ from the saved level.");
        return new JSONObject().put("layout", layout(recipe.getJSONObject("layout"))).put("sprite", sprite);
    }

    private static JSONObject layout(JSONObject input) {
        SpatialScene.keys(input, "title", "backdropX", "platformX", "platformY", "pickupX", "foregroundX");
        String title = SpatialScene.text(input, "title", 70).strip();
        int platformX = integer(input, "platformX", 150, 505);
        return new JSONObject().put("title", title)
                .put("backdropX", integer(input, "backdropX", 80, 625))
                .put("platformX", platformX).put("platformY", integer(input, "platformY", 186, 246))
                .put("pickupX", integer(input, "pickupX", platformX + 20, platformX + 110))
                .put("foregroundX", integer(input, "foregroundX", 80, 670));
    }

    static int integer(JSONObject input, String key, int min, int max) {
        double n = SpatialScene.number(input, key, min, max);
        if (Math.rint(n) != n) throw new IllegalArgumentException(key + " must be a whole pixel.");
        return (int) n;
    }

    static JSONObject source(JSONObject input) {
        SpatialScene.keys(input, "url", "tag", "filename", "png");
        var source = new JSONObject().put("png", input.getString("png"));
        if (input.has("url")) {
            if (input.has("filename")) throw new IllegalArgumentException("Choose one image source.");
            return source.put("url", BrowserShapeTrace.sourceUrl(input))
                    .put("tag", SpatialScene.text(input, "tag", 180));
        }
        if (input.has("tag")) throw new IllegalArgumentException("Choose one image source.");
        String name = SpatialScene.text(input, "filename", 150);
        if (name.contains("/") || name.contains("\\") || name.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Choose a PNG filename.");
        return source.put("filename", name);
    }

    static JSONObject sprite(JSONObject input) throws Exception {
        var source = source(input);
        byte[] png = decode(source.getString("png"), MAX_PNG);
        var image = BrowserShapeTrace.decode(png, 8);
        int w = image.getWidth(), h = image.getHeight();
        if (w < 8 || h < 8 || w > 1024 || h > 1024)
            throw new IllegalArgumentException("Choose a PNG between 8 and 1024 pixels per side.");
        int[] pixels = image.getRGB(0, 0, w, h, null, 0, w);
        boolean alpha = false;
        for (int pixel : pixels) if ((pixel >>> 24) < 255) { alpha = true; break; }
        boolean[] mask = alpha ? null : BrowserShapeTrace.isolate(image);
        int left = w, top = h, right = -1, bottom = -1, count = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                if (alpha ? (pixels[i] >>> 24) == 0 : !mask[i]) {
                    pixels[i] = 0;
                    continue;
                }
                if (!alpha) pixels[i] |= 0xff000000;
                left = Math.min(left, x);
                top = Math.min(top, y);
                right = Math.max(right, x);
                bottom = Math.max(bottom, y);
                count++;
            }
        }
        if (count < 16) throw new IllegalArgumentException("The image has too few visible sprite pixels.");
        int width = right - left + 1, height = bottom - top + 1;
        var output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        output.setRGB(0, 0, width, height, pixels, top * w + left, w);
        var buffer = new ByteArrayOutputStream();
        if (!ImageIO.write(output, "PNG", buffer)) throw new IOException("PNG encoder unavailable.");
        byte[] bytes = buffer.toByteArray();
        int[] roundTrip = BrowserShapeTrace.decode(bytes, 1).getRGB(0, 0, width, height, null, 0, width);
        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                if (roundTrip[row * width + column] != pixels[(top + row) * w + left + column])
                    throw new IOException("The encoded sprite differs from its isolated pixels.");
            }
        }
        return new JSONObject().put("png", Base64.getEncoder().encodeToString(bytes))
                .put("width", width).put("height", height).put("visible", count).put("isolation", alpha ? "alpha" : "contrast")
                .put("origin", source.optString("url", source.optString("filename")))
                .put("sourceSha256", MapDefinition.sha(source.toString().getBytes(StandardCharsets.UTF_8)))
                .put("imageSha256", MapDefinition.sha(png)).put("sha256", MapDefinition.sha(bytes));
    }

    static byte[] decode(String encoded, int max) {
        if (encoded.length() > ((max + 2) / 3) * 4) throw new IllegalArgumentException("Media file exceeds the size limit.");
        byte[] bytes = Base64.getDecoder().decode(encoded);
        if (bytes.length > max) throw new IllegalArgumentException("Media file exceeds the size limit.");
        return bytes;
    }

    static byte[] read(Path path, int max) throws IOException {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
            throw new IllegalArgumentException("The saved media file is unavailable.");
        try (var in = Files.newInputStream(path)) {
            byte[] bytes = in.readNBytes(max + 1);
            if (bytes.length > max) throw new IllegalArgumentException("Saved media file exceeds the size limit.");
            return bytes;
        }
    }

    static byte[] read(Path path, int max, String expected) throws IOException {
        byte[] bytes = read(path, max);
        if (!MapDefinition.sha(bytes).equals(expected))
            throw new IllegalArgumentException("The saved level bytes changed.");
        return bytes;
    }

    static void replace(Path target, byte[] bytes) throws IOException {
        Path pending = Files.createTempFile(target.getParent(), "media-", ".pending");
        try {
            Files.write(pending, bytes);
            try {
                Files.move(pending, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unavailable) {
                Files.move(pending, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(pending);
        }
    }

    private static String resource(String name) throws IOException {
        try (var stream = SpriteWorkshop.class.getResourceAsStream("/" + name)) {
            if (stream == null) throw new IOException("Missing bundled game file " + name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static byte[] portableGame(JSONObject level) throws Exception {
        var sprite = level.getJSONObject("sprite");
        var game = new JSONObject().put("layout", level.getJSONObject("layout"))
                .put("sprite", new JSONObject().put("png", sprite.getString("png")));
        return portableGame(game, "media-play.js", "mountGame");
    }

    static byte[] portableGame(JSONObject game, String runtime, String entryPoint) throws Exception {
        String data = Base64.getEncoder().encodeToString(game.toString().getBytes(StandardCharsets.UTF_8));
        String script = "\n" + resource(runtime).replace("\r\n", "\n") + """

                const bytes = Uint8Array.from(atob(document.querySelector('#level-data').content.textContent), c => c.charCodeAt(0));
                const level = JSON.parse(new TextDecoder().decode(bytes));
                document.title = level.layout.title;
                document.querySelector('h1').textContent = level.layout.title;
                const game = %s(document.querySelector('canvas'), document.querySelector('#progress'), level);
                document.querySelector('#restart').onclick = () => { game.restart(); document.querySelector('canvas').focus(); };
                """.formatted(entryPoint);
        String hash = Base64.getEncoder().encodeToString(java.security.MessageDigest.getInstance("SHA-256")
                .digest(script.getBytes(StandardCharsets.UTF_8)));
        return resource("media-play.html").replace("@@SCRIPT_HASH@@", hash).replace("@@LEVEL_DATA@@", data)
                .replace("@@SCRIPT@@", script).getBytes(StandardCharsets.UTF_8);
    }
}
