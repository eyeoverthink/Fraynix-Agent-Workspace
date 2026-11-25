package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Reads the authored data in a scene graph or a generated runner without executing the runner. */
final class NexusSceneLevel {
    static final int MAX_SOURCE = 3_300_000;
    private static final int CELL = 24, COLUMNS = 30, ROWS = 13, PLAYER_HEIGHT = 32;
    private static final String SCENE_START = "\nconst SCENE = ";
    private static final String SCENE_END = ";\nconst WASM_B64 = ";
    private static final String PNG = "data:image/png;base64,";
    private static final List<String> COLORS = List.of("sky", "ground", "rock", "accent", "water");
    private static final Set<String> ANIMATION = Set.of("bob", "hover", "stride", "squash");
    private static final List<String> EFFECTS = List.of("", "ground", "brick", "hazard", "coin", "spring");

    private NexusSceneLevel() {}

    static JSONObject convert(JSONObject input) throws Exception {
        SpatialScene.keys(input, "base64");
        byte[] bytes = SpriteWorkshop.decode(input.getString("base64"), MAX_SOURCE);
        String content = new String(bytes, StandardCharsets.UTF_8).strip();
        if (content.startsWith("\ufeff")) content = content.substring(1).stripLeading();
        JSONObject document = read(content);
        var world = document.getJSONObject("world");
        var grid = document.optJSONArray("levelGrid2D");
        if (grid == null) grid = document.optJSONArray("grid");
        if (grid == null || grid.isEmpty())
            throw new IllegalArgumentException("Export the WebAssembly runner from Scene to Game after extracting a 2D grid.");
        if (grid.length() < 3 || grid.length() > ROWS)
            throw new IllegalArgumentException("Use a Nexus grid with 3 to 13 rows.");
        int columns = grid.getJSONArray(0).length();
        if (columns < 3 || columns > COLUMNS) throw new IllegalArgumentException("Use 3 to 30 grid columns.");
        int yOffset = ROWS - grid.length();
        int xOffset = (COLUMNS - columns) / 2;

        var stage = new JSONObject().put("sourceSha256", MapDefinition.sha(bytes))
                .put("palette", palette(world.getJSONObject("biomePalette")));
        if (world.has("styleId")) stage.put("style", SpatialScene.text(world, "styleId", 75));
        var assets = new JSONObject();
        var characters = document.getJSONArray("characters");
        if (characters.length() > 32) throw new IllegalArgumentException("This scene has too many cast entries.");
        for (Object value : characters) {
            if (value instanceof JSONObject hero && hero.optString("role").equals("playable")) {
                addHero(hero, stage, assets);
                break;
            }
        }
        String backdrop = world.optString("sourceImageUrl", "");
        if (backdrop.startsWith(PNG)) {
            byte[] png = SpriteWorkshop.decode(backdrop.substring(PNG.length()), 750_000);
            var image = BrowserShapeTrace.decode(png, 8);
            if (image.getWidth() > 1022 || image.getHeight() > 1022)
                throw new IllegalArgumentException("Resize the embedded backdrop to at most 1022 × 1022 pixels.");
            var bordered = new BufferedImage(image.getWidth() + 2, image.getHeight() + 2, BufferedImage.TYPE_INT_ARGB);
            var graphics = bordered.createGraphics();
            try {
                graphics.drawImage(image, 1, 1, null);
            } finally {
                graphics.dispose();
            }
            stage.put("backdropAsset", addAsset(assets, "Nexus backdrop.png", encode(bordered)))
                    .put("backdropOpacity", bounded(world, "opacity", 0, 1, 1))
                    .put("parallax", bounded(world, "parallaxSpeed", 0, 2, 0.3));
        }

        var marker = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
        var graphics = marker.createGraphics();
        try {
            graphics.setColor(java.awt.Color.decode(stage.getJSONObject("palette").getString("accent")));
            graphics.fillRect(1, 1, 22, 22);
        } finally {
            graphics.dispose();
        }
        String tile = null;
        var objects = new JSONArray();
        for (int row = 0; row < grid.length(); row++) {
            var cells = grid.getJSONArray(row);
            if (cells.length() != columns) throw new IllegalArgumentException("The Nexus grid must be rectangular.");
            for (int col = 0; col < columns; col++) {
                Object raw = cells.get(col);
                if (!(raw instanceof Number number) || number.doubleValue() != number.intValue()
                        || number.intValue() < 0 || number.intValue() > 7)
                    throw new IllegalArgumentException("Nexus grid cells must use integer roles 0 to 7.");
                int code = number.intValue();
                if (code == 0) continue;
                int x = (xOffset + col) * CELL, y = (yOffset + row) * CELL;
                if (code >= 6) {
                    String key = code == 7 ? "spawn" : "goal";
                    if (stage.has(key)) throw new IllegalArgumentException("Use exactly one Nexus spawn and one goal.");
                    stage.put(key, point(x, code == 7 ? y + CELL - PLAYER_HEIGHT : y));
                } else {
                    if (tile == null) tile = addAsset(assets, "Nexus tile roles.png", encode(marker));
                    var object = new JSONObject().put("id", "nexus-" + row + "-" + col).put("assetId", tile)
                            .put("kind", code <= 2 ? "tile" : "sprite")
                            .put("layer", code <= 2 ? "terrain" : code == 4 ? "pickup" : "foreground")
                            .put("x", x).put("y", y).put("size", CELL).put("effect", EFFECTS.get(code));
                    if (code <= 2) object.put("collider", point(0, 0).put("w", CELL).put("h", CELL));
                    objects.put(object);
                }
            }
        }
        if (!stage.has("spawn") || !stage.has("goal"))
            throw new IllegalArgumentException("Mark one spawn and one goal in the Nexus grid.");
        if (tile == null) throw new IllegalArgumentException("Add terrain or interactive tiles to the Nexus grid.");
        var layout = new JSONObject().put("title", SpatialScene.text(world, "name", 70))
                .put("objects", objects).put("stage", stage);
        return new JSONObject().put("format", "fraynix.tile-level-v1").put("layout", layout).put("assets", assets);
    }

    private static JSONObject read(String content) {
        if (content.startsWith("{")) {
            var parsed = new JSONObject(content);
            if (!parsed.optString("version").equals("2.0"))
                throw new IllegalArgumentException("Choose a Nexus scene graph version 2.0.");
            return parsed;
        }
        String html = content.replace("\r\n", "\n");
        int start = html.indexOf(SCENE_START);
        int end = start < 0 ? -1 : html.indexOf(SCENE_END, start + SCENE_START.length());
        if (!html.toLowerCase(Locale.ROOT).startsWith("<!doctype html>") || start < 0 || end < 0)
            throw new IllegalArgumentException("Choose the Nexus Scene to Game WebAssembly runner HTML or a scene graph JSON.");
        return new JSONObject(html.substring(start + SCENE_START.length(), end));
    }

    private static void addHero(JSONObject source, JSONObject stage, JSONObject assets) throws Exception {
        String sprite = source.optString("spriteDataUrl", "");
        if (!sprite.startsWith(PNG)) throw new IllegalArgumentException("The playable character needs an embedded PNG cutout.");
        var hero = new JSONObject().put("name", SpatialScene.text(source, "name", 70))
                .put("assetId", addAsset(assets, "Nexus playable character.png", sprite.substring(PNG.length())))
                .put("speed", bounded(source, "speed", 60, 360, 235))
                .put("jump", bounded(source, "jumpPower", 260, 740, 475))
                .put("scale", bounded(source, "scale", 0.65, 1.45, 1))
                .put("animation", source.optString("animMode", "stride"));
        stage.put("hero", hero);
    }

    static JSONObject validatedStage(JSONObject source) {
        SpatialScene.keys(source, "sourceSha256", "palette", "spawn", "goal", "hero", "style",
                "backdropAsset", "backdropOpacity", "parallax");
        String proof = source.getString("sourceSha256");
        if (!proof.matches("[a-f0-9]{64}")) throw new IllegalArgumentException("The Nexus source hash is invalid.");
        var stage = new JSONObject().put("sourceSha256", proof).put("palette", palette(source.getJSONObject("palette")))
                .put("spawn", location(source.getJSONObject("spawn"), ROWS * CELL - PLAYER_HEIGHT))
                .put("goal", location(source.getJSONObject("goal"), (ROWS - 1) * CELL));
        if (source.has("style")) stage.put("style", SpatialScene.text(source, "style", 75));
        if (source.has("hero")) {
            var hero = source.getJSONObject("hero");
            SpatialScene.keys(hero, "name", "assetId", "speed", "jump", "scale", "animation");
            String animation = hero.getString("animation");
            if (!ANIMATION.contains(animation)) throw new IllegalArgumentException("Choose a supported hero animation.");
            stage.put("hero", new JSONObject().put("name", SpatialScene.text(hero, "name", 70))
                    .put("assetId", assetId(hero, "assetId")).put("speed", SpatialScene.number(hero, "speed", 60, 360))
                    .put("jump", SpatialScene.number(hero, "jump", 260, 740))
                    .put("scale", SpatialScene.number(hero, "scale", 0.65, 1.45)).put("animation", animation));
        }
        if (source.has("backdropAsset")) {
            stage.put("backdropAsset", assetId(source, "backdropAsset"))
                    .put("backdropOpacity", SpatialScene.number(source, "backdropOpacity", 0, 1))
                    .put("parallax", SpatialScene.number(source, "parallax", 0, 2));
        }
        return stage;
    }

    static void addStageAssets(JSONObject stage, Set<String> ids) {
        if (stage.has("hero")) ids.add(stage.getJSONObject("hero").getString("assetId"));
        if (stage.has("backdropAsset")) ids.add(stage.getString("backdropAsset"));
    }

    static String effect(JSONObject row, String layer, boolean collider) {
        String effect = row.getString("effect");
        boolean valid = switch (effect) {
            case "ground", "brick" -> layer.equals("terrain") && collider;
            case "coin" -> layer.equals("pickup") && !collider;
            case "hazard", "spring" -> layer.equals("foreground") && !collider;
            default -> false;
        };
        if (!valid) throw new IllegalArgumentException("The Nexus tile role conflicts with its layer or collider.");
        return effect;
    }

    private static String assetId(JSONObject source, String key) {
        String id = source.getString(key);
        if (!id.matches("[a-f0-9]{64}")) throw new IllegalArgumentException("The Nexus image reference is invalid.");
        return id;
    }

    private static JSONObject palette(JSONObject source) {
        var palette = new JSONObject();
        for (String color : COLORS) {
            String hex = source.getString(color);
            if (!hex.matches("#[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Nexus palette colors must be six-digit hex.");
            palette.put(color, hex.toLowerCase(Locale.ROOT));
        }
        return palette;
    }

    private static double bounded(JSONObject source, String key, double min, double max, double fallback) {
        if (!source.has(key)) return fallback;
        return Math.clamp(SpatialScene.number(source, key, -20_000, 20_000), min, max);
    }

    private static JSONObject location(JSONObject source, int maxY) {
        SpatialScene.keys(source, "x", "y");
        return point(SpriteWorkshop.integer(source, "x", 0, (COLUMNS - 1) * CELL),
                SpriteWorkshop.integer(source, "y", 0, maxY));
    }

    private static JSONObject point(int x, int y) {
        return new JSONObject().put("x", x).put("y", y);
    }

    private static String addAsset(JSONObject assets, String name, String png) throws Exception {
        var source = SpriteWorkshop.source(new JSONObject().put("filename", name).put("png", png));
        var recipe = TileWorkshop.assetRecipe(source, SpriteWorkshop.sprite(source));
        String id = TileWorkshop.assetIdentity(recipe);
        assets.put(id, recipe);
        return id;
    }

    private static String encode(BufferedImage image) throws Exception {
        var bytes = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", bytes)) throw new IllegalStateException("The PNG encoder is unavailable.");
        return Base64.getEncoder().encodeToString(bytes.toByteArray());
    }
}
