
package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

final class TileWorkshop {
    private static final String FORMAT = "fraynix.tile-level-v1";
    private static final int MAX_ASSET = 1_050_000;
    private static final int MAX_LAYOUT = 110_000;
    private static final int MAX_PACKAGE = 6_500_000;
    private static final int MAX_LIBRARY = 64;
    private static final int MAX_USED = 6;
    private static final int WIDTH = 720, FLOOR = 312, TILE = 24;
    private final Path root, assets, levels;

    TileWorkshop(Path project) {
        root = project.resolve("media/tiles");
        assets = root.resolve("sprites");
        levels = root.resolve("levels");
    }

    synchronized JSONObject add(JSONObject input) throws Exception {
        SpatialScene.keys(input, "source");
        var source = SpriteWorkshop.source(input.getJSONObject("source"));
        var sprite = SpriteWorkshop.sprite(source);
        var recipe = assetRecipe(source, sprite);
        byte[] bytes = encoded(recipe);
        String id = MapDefinition.sha(bytes);
        store(assets, id, bytes, MAX_ASSET);
        return sprite.put("id", id).put("label", label(source));
    }

    static JSONObject assetRecipe(JSONObject source, JSONObject sprite) {
        return new JSONObject().put("source", source).put("sourceSha256", sprite.getString("sourceSha256"))
                .put("spriteSha256", sprite.getString("sha256"));
    }

    static String assetIdentity(JSONObject recipe) {
        return MapDefinition.sha(encoded(recipe));
    }

    synchronized JSONObject library(JSONObject input) throws IOException {
        SpatialScene.keys(input);
        var catalog = new JSONArray();
        for (String id : ids(assets, MAX_LIBRARY)) {
            var row = new JSONObject().put("id", id);
            try {
                var recipe = read(assets, id, MAX_ASSET);
                row.put("label", label(recipe.getJSONObject("source")));
            } catch (IOException | IllegalArgumentException | org.json.JSONException broken) {
                row.put("label", "Unavailable sprite").put("unavailable", true);
            }
            catalog.put(row);
        }
        return new JSONObject().put("assets", catalog);
    }

    synchronized JSONObject sprite(JSONObject input) throws Exception {
        SpatialScene.keys(input, "id");
        String id = input.getString("id");
        return checkAsset(id, read(assets, id, MAX_ASSET));
    }

    synchronized JSONObject list(JSONObject input) throws IOException {
        SpatialScene.keys(input);
        var saved = new JSONArray();
        for (String id : ids(levels, 60)) {
            try {
                var layout = read(levels, id, MAX_LAYOUT);
                saved.put(new JSONObject().put("id", id).put("title", layout.getString("title")));
            } catch (IOException | IllegalArgumentException | org.json.JSONException broken) {
                saved.put(new JSONObject().put("id", id).put("title", "Unavailable level").put("unavailable", true));
            }
        }
        return new JSONObject().put("levels", saved);
    }

    synchronized JSONObject open(JSONObject input) throws Exception {
        SpatialScene.keys(input, "id");
        Path latest = root.resolve("latest");
        if (!input.has("id") && !Files.exists(latest, LinkOption.NOFOLLOW_LINKS)) return new JSONObject();
        String id = input.has("id") ? input.getString("id")
                : new String(SpriteWorkshop.read(latest, 64), StandardCharsets.UTF_8);
        var level = checkedLevel(id, layout(read(levels, id, MAX_LAYOUT)));
        if (input.has("id")) SpriteWorkshop.replace(latest, id.getBytes(StandardCharsets.US_ASCII));
        return level;
    }

    synchronized JSONObject save(JSONObject input) throws Exception {
        SpatialScene.keys(input, "layout");
        var layout = layout(input.getJSONObject("layout"));
        String id = MapDefinition.sha(encoded(layout));
        var checked = checkedLevel(id, layout);
        commitLevel(id, layout);
        return checked;
    }

    synchronized JSONObject importLevel(JSONObject input) throws Exception {
        SpatialScene.keys(input, "base64");
        var bundle = new JSONObject(new String(SpriteWorkshop.decode(input.getString("base64"), MAX_PACKAGE),
                StandardCharsets.UTF_8));
        return importBundle(bundle);
    }

    synchronized JSONObject importNexus(JSONObject input) throws Exception {
        return importBundle(NexusSceneLevel.convert(input));
    }

    private JSONObject importBundle(JSONObject bundle) throws Exception {
        SpatialScene.keys(bundle, "format", "layout", "assets");
        if (!FORMAT.equals(bundle.getString("format"))) throw new IllegalArgumentException("Choose a Tile editor export.");
        var layout = layout(bundle.getJSONObject("layout"));
        var files = bundle.getJSONObject("assets");
        var used = used(layout);
        if (!files.keySet().equals(used)) throw new IllegalArgumentException("Level and bundled sprite references differ.");
        var checked = new JSONObject();
        for (String id : used) {
            var asset = files.getJSONObject(id);
            if (encoded(asset).length > MAX_ASSET) throw new IllegalArgumentException("Bundled sprite exceeds the limit.");
            checked.put(id, checkAsset(id, asset));
        }
        int available = MAX_LIBRARY - ids(assets, MAX_LIBRARY).size();
        long fresh = used.stream().filter(id -> !Files.exists(file(assets, id), LinkOption.NOFOLLOW_LINKS)).count();
        if (fresh > available) throw new IllegalArgumentException("The sprite library has room for up to 64 sprites.");
        for (String id : used) store(assets, id, encoded(files.getJSONObject(id)), MAX_ASSET);
        String id = MapDefinition.sha(encoded(layout));
        commitLevel(id, layout);
        return level(id, layout, checked);
    }

    synchronized JSONObject export(JSONObject input) throws Exception {
        SpatialScene.keys(input, "id", "format");
        String id = input.getString("id");
        String format = input.getString("format");
        if ("png".equals(format)) {
            var sprite = checkAsset(id, read(assets, id, MAX_ASSET));
            byte[] bytes = java.util.Base64.getDecoder().decode(sprite.getString("png"));
            return receipt(bytes, new JSONObject().put(id, hashes(sprite)));
        }
        var layout = layout(read(levels, id, MAX_LAYOUT));
        var originals = new JSONObject();
        var sprites = new JSONObject();
        var proof = new JSONObject();
        for (String key : used(layout)) {
            var original = read(assets, key, MAX_ASSET);
            var sprite = checkAsset(key, original);
            originals.put(key, original);
            sprites.put(key, new JSONObject().put("png", sprite.getString("png")));
            proof.put(key, hashes(sprite));
        }
        byte[] bytes = switch (format) {
            case "json" -> encoded(new JSONObject().put("format", FORMAT).put("layout", layout).put("assets", originals));
            case "html" -> SpriteWorkshop.portableGame(new JSONObject().put("layout", layout).put("sprites", sprites),
                    "media-tile-play.js", "mountTileGame");
            default -> throw new IllegalArgumentException("Choose playable HTML or editable level.");
        };
        if ("json".equals(format) && bytes.length > MAX_PACKAGE)
            throw new IllegalArgumentException("The editable bundle exceeds the package limit.");
        return receipt(bytes, proof);
    }

    private JSONObject checkedLevel(String id, JSONObject layout) throws Exception {
        var sprites = new JSONObject();
        for (String key : used(layout)) sprites.put(key, checkAsset(key, read(assets, key, MAX_ASSET)));
        return level(id, layout, sprites);
    }

    private static JSONObject level(String id, JSONObject layout, JSONObject sprites) {
        return new JSONObject().put("id", id).put("layout", layout).put("sprites", sprites);
    }

    private void commitLevel(String id, JSONObject layout) throws IOException {
        store(levels, id, encoded(layout), MAX_LAYOUT);
        SpriteWorkshop.replace(root.resolve("latest"), id.getBytes(StandardCharsets.US_ASCII));
    }

    private void store(Path directory, String id, byte[] bytes, int max) throws IOException {
        if (bytes.length > max) throw new IllegalArgumentException("Saved media file exceeds the size limit.");
        Path target = file(directory, id);
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            SpriteWorkshop.read(target, max, id);
            return;
        }
        if (directory.equals(assets) && ids(assets, MAX_LIBRARY).size() >= MAX_LIBRARY)
            throw new IllegalArgumentException("The sprite library has room for up to 64 sprites.");
        Files.createDirectories(directory);
        SpriteWorkshop.replace(target, bytes);
    }

    private static java.util.List<String> ids(Path directory, int limit) throws IOException {
        if (!Files.exists(directory)) return java.util.List.of();
        try (var files = Files.list(directory)) {
            return files.filter(path -> path.getFileName().toString().matches("[0-9a-f]{64}\\.json"))
                    .sorted(Comparator.comparingLong(TileWorkshop::modified).reversed().thenComparing(Path::toString))
                    .limit(limit).map(path -> path.getFileName().toString().substring(0, 64)).toList();
        }
    }

    private static long modified(Path path) {
        try {
            return Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS).toMillis();
        } catch (IOException removed) {
            return 0;
        }
    }

    private static Path file(Path directory, String id) {
        if (!id.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Invalid media ID.");
        return directory.resolve(id + ".json");
    }

    private static JSONObject read(Path directory, String id, int max) throws IOException {
        return new JSONObject(new String(SpriteWorkshop.read(file(directory, id), max, id), StandardCharsets.UTF_8));
    }

    private static byte[] encoded(JSONObject value) {
        return value.toString().getBytes(StandardCharsets.UTF_8);
    }

    synchronized JSONObject registeredViews(JSONObject request) throws Exception {
        SpatialScene.keys(request, "name", "widthM", "heldOutElevationDeg", "assets");
        var assets = request.getJSONObject("assets");
        SpatialScene.keys(assets, "front", "side", "top", "heldOut");
        var checked = new JSONObject();
        var seen = new HashSet<String>();
        for (String role : java.util.List.of("front", "side", "top", "heldOut")) {
            if ("top".equals(role) && !assets.has(role)) continue;
            String id = assets.getString(role);
            if (!seen.add(id)) throw new IllegalArgumentException("Choose different sprite library entries for each view.");
            var recipe = read(this.assets, id, MAX_ASSET);
            var sprite = checkAsset(id, recipe);
            checked.put(role, hashes(sprite).put("assetId", id).put("label", sprite.getString("label"))
                    .put("pngSha256", sprite.getString("imageSha256"))
                    .put("png", recipe.getJSONObject("source").getString("png")));
        }
        return new JSONObject().put("format", BrowserViewTrace.FORMAT_LIBRARY)
                .put("name", SpatialScene.text(request, "name", 70).strip())
                .put("widthM", SpatialScene.number(request, "widthM", .1, 5))
                .put("heldOutElevationDeg", SpatialScene.number(request, "heldOutElevationDeg", 0, 60))
                .put("views", checked);
    }

    synchronized void verifyRegisteredViews(JSONObject views) throws Exception {
        for (String role : views.keySet()) {
            var receipt = views.getJSONObject(role);
            String id = receipt.getString("assetId");
            var record = read(assets, id, MAX_ASSET);
            String pngSha256 = MapDefinition.sha(SpriteWorkshop.decode(record.getJSONObject("source").getString("png"), 750_000));
            if (!pngSha256.equals(receipt.getString("pngSha256"))
                    || !record.getString("sourceSha256").equals(receipt.getString("sourceSha256"))
                    || !record.getString("spriteSha256").equals(receipt.getString("spriteSha256")))
                throw new IllegalStateException("A source sprite changed since reconstruction.");
        }
    }

    private static JSONObject checkAsset(String id, JSONObject recipe) throws Exception {
        SpatialScene.keys(recipe, "source", "sourceSha256", "spriteSha256");
        if (!MapDefinition.sha(encoded(recipe)).equals(id)) throw new IllegalArgumentException("The sprite record changed.");
        var source = recipe.getJSONObject("source");
        var sprite = SpriteWorkshop.sprite(source);
        if (!sprite.getString("sourceSha256").equals(recipe.getString("sourceSha256"))
                || !sprite.getString("sha256").equals(recipe.getString("spriteSha256")))
            throw new IllegalArgumentException("The original image or isolated pixels changed.");
        return sprite.put("id", id).put("label", label(source));
    }

    private static String label(JSONObject source) {
        if (source.has("filename")) return source.getString("filename");
        var page = java.net.URI.create(source.getString("url"));
        String path = page.getPath();
        String name = path.substring(path.lastIndexOf('/') + 1);
        return name.isBlank() ? page.getHost() : name;
    }

    private static JSONObject hashes(JSONObject sprite) {
        return new JSONObject().put("sourceSha256", sprite.getString("sourceSha256"))
                .put("spriteSha256", sprite.getString("sha256"));
    }

    private static JSONObject receipt(byte[] bytes, JSONObject assets) {
        return new JSONObject().put("base64", java.util.Base64.getEncoder().encodeToString(bytes))
                .put("sha256", MapDefinition.sha(bytes)).put("assets", assets);
    }

    private static Set<String> used(JSONObject layout) {
        var ids = new java.util.LinkedHashSet<String>();
        for (Object entry : layout.getJSONArray("objects")) ids.add(((JSONObject) entry).getString("assetId"));
        if (layout.has("stage")) NexusSceneLevel.addStageAssets(layout.getJSONObject("stage"), ids);
        if (ids.size() > MAX_USED) throw new IllegalArgumentException("Use up to six different sprites per level.");
        return ids;
    }

    private static JSONObject layout(JSONObject input) {
        SpatialScene.keys(input, "title", "objects", "stage");
        String title = SpatialScene.text(input, "title", 70).strip();
        var stage = input.has("stage") ? NexusSceneLevel.validatedStage(input.getJSONObject("stage")) : null;
        var objects = input.getJSONArray("objects");
        int capacity = stage == null ? 160 : 390;
        if (objects.length() > capacity) throw new IllegalArgumentException("This level holds up to " + capacity + " tiles and items.");
        var result = new JSONArray();
        var ids = new HashSet<String>();
        var cells = new HashSet<Integer>();
        int pickups = 0;
        for (int i = 0; i < objects.length(); i++) {
            var item = objects.getJSONObject(i);
            SpatialScene.keys(item, "id", "assetId", "kind", "layer", "x", "y", "size", "collider", "effect");
            String id = item.getString("id"), asset = item.getString("assetId");
            if (!id.matches("[a-zA-Z0-9-]{1,40}") || !ids.add(id) || !asset.matches("[0-9a-f]{64}"))
                throw new IllegalArgumentException("Use a unique item ID and a valid sprite reference.");
            String kind = item.getString("kind"), layer = item.getString("layer");
            boolean tile = "tile".equals(kind);
            if (tile ? !"terrain".equals(layer) : (!"sprite".equals(kind)
                    || !Set.of("backdrop", "terrain", "pickup", "foreground").contains(layer)))
                throw new IllegalArgumentException("Choose a supported item and layer.");
            int size = number(item, "size", tile ? TILE : 16, tile ? TILE : 192);
            int x = number(item, "x", 0, WIDTH-size), y = number(item, "y", 0, FLOOR-size);
            if (tile && (x % TILE != 0 || y % TILE != 0 || !cells.add(y * WIDTH + x)))
                throw new IllegalArgumentException("Paint one terrain tile per grid cell.");
            var output = new JSONObject().put("id", id).put("assetId", asset).put("kind", kind).put("layer", layer)
                    .put("x", x).put("y", y).put("size", size);
            if (item.has("collider")) {
                if (!"terrain".equals(layer)) throw new IllegalArgumentException("Only terrain can be solid.");
                output.put("collider", collider(item.getJSONObject("collider"), x, y, size, stage));
            }
            if (item.has("effect")) {
                if (stage == null) throw new IllegalArgumentException("Nexus roles require their imported stage.");
                output.put("effect", NexusSceneLevel.effect(item, layer, item.has("collider")));
                if (item.getString("effect").equals("hazard") &&
                        intersects(x, y, size, size, stage.getJSONObject("spawn"), 24, 32))
                    throw new IllegalArgumentException("A hazard overlaps the player start.");
            }
            if ("pickup".equals(layer)) pickups++;
            result.put(output);
        }
        if (stage == null && pickups == 0) throw new IllegalArgumentException("Place at least one pickup to unlock the exit.");
        var output = new JSONObject().put("title", title).put("objects", result);
        return stage == null ? output : output.put("stage", stage);
    }

    private static JSONObject collider(JSONObject input, int itemX, int itemY, int size, JSONObject stage) {
        SpatialScene.keys(input, "x", "y", "w", "h");
        int x = number(input, "x", 0, size - 1), y = number(input, "y", 0, size - 1);
        int w = number(input, "w", 1, size - x), h = number(input, "h", 1, size - y);
        int left = itemX + x, top = itemY + y;
        var spawn = stage == null ? new JSONObject().put("x", 48).put("y", FLOOR - 32) : stage.getJSONObject("spawn");
        if (intersects(left, top, w, h, spawn, 24, 32))
            throw new IllegalArgumentException("A collider overlaps the player start.");
        if (stage != null && intersects(left, top, w, h, stage.getJSONObject("goal"), TILE, TILE))
            throw new IllegalArgumentException("A collider overlaps the goal.");
        return new JSONObject().put("x", x).put("y", y).put("w", w).put("h", h);
    }

    private static boolean intersects(int x, int y, int w, int h, JSONObject point, int width, int height) {
        int left = point.getInt("x"), top = point.getInt("y");
        return x < left + width && x + w > left && y < top + height && y + h > top;
    }

    private static int number(JSONObject input, String key, int min, int max) {
        return SpriteWorkshop.integer(input, key, min, max);
    }
}
