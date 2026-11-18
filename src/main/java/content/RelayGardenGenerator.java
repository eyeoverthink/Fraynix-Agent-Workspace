package content;

import com.google.gson.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;

/** Original authored geometry, pixels and tones. Never reads another level or image. */
public final class RelayGardenGenerator {
    public static final String ROOT = "assets/original/relay-garden/";
    public static final String[] CUES = {"main-theme-overworld", "flagpole", "break_block", "bump",
        "coin", "gameover", "jump-small", "mario_die", "pipe", "powerup", "powerup_appears",
        "stage_clear", "stomp", "kick", "invincible"};
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Choose a new output directory");
        generate(Path.of(args[0]));
    }

    public static void generate(Path out) throws Exception {
        if (Files.exists(out)) throw new IllegalArgumentException("Generation requires a new directory");
        Files.createDirectories(out);
        BufferedImage atlas = new BufferedImage(256, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = atlas.createGraphics();
        for (int i = 0; i < 16; i++) {
            Graphics2D cell = (Graphics2D) g.create((i % 8) * 32, (i / 8) * 32, 32, 32);
            drawCell(cell, i); cell.dispose();
        }
        g.dispose(); ImageIO.write(atlas, "png", out.resolve("atlas.png").toFile());
        BufferedImage sky = new BufferedImage(1024, 512, BufferedImage.TYPE_INT_RGB);
        g = sky.createGraphics();
        for (int y = 0; y < 512; y++) {
            g.setColor(new Color(12 + y / 45, 19 + y / 30, 39 + y / 24)); g.drawLine(0, y, 1023, y);
        }
        Random stars = new Random(167);
        for (int i = 0; i < 160; i++) {
            g.setColor(new Color(90 + stars.nextInt(90), 160, 170));
            g.fillRect(stars.nextInt(1024), stars.nextInt(350), 2, 2);
        }
        g.setColor(new Color(55, 75, 98)); g.fillOval(740, 50, 95, 95);
        g.setColor(new Color(33, 51, 71));
        for (int i = 0; i < 16; i++) {
            int x = i * 72; g.fillPolygon(new int[]{x, x + 35, x + 85},
                new int[]{512, 290 + (i * 37 % 130), 512}, 3);
        }
        g.dispose(); ImageIO.write(sky, "png", out.resolve("sky.png").toFile());
        for (int i = 0; i < CUES.length; i++) tone(out.resolve(CUES[i] + ".wav"), i);
        Files.writeString(out.resolve("level.json"), JSON.toJson(level()) + "\n", StandardCharsets.UTF_8);
        JsonObject manifest = object("id", "relay-garden-v1", "title", "Relay Garden",
            "generator", "src/main/java/content/RelayGardenGenerator.java", "originalContent", true,
            "scope", "Original pixels, tone synthesis and layout; existing engine components and physics",
            "level", ROOT + "level.json", "finishX", 13.625, "startX", .875,
            "gaps", array(object("startX", 4.5, "endX", 5.0), object("startX", 8.25, "endX", 8.75)));
        JsonObject files = new JsonObject();
        try (var paths = Files.list(out)) {
            for (Path p : paths.sorted().toList()) files.addProperty(ROOT + p.getFileName(), hash(p));
        }
        manifest.add("files", files);
        Files.writeString(out.resolve("manifest.json"), JSON.toJson(manifest) + "\n", StandardCharsets.UTF_8);
    }

    private static void drawCell(Graphics2D g, int i) {
        Color ink = new Color(20, 31, 49), mint = new Color(99, 227, 195), brass = new Color(246, 183, 80);
        if (i < 8) {
            int lift = i == 3 ? -2 : 0, stride = i == 1 ? 3 : i == 2 ? -3 : 0;
            g.setColor(ink); g.fillRoundRect(7, 3 + lift, 19, 16, 4, 4);
            g.setColor(mint); g.fillRoundRect(8, 4 + lift, 17, 12, 3, 3);
            g.setColor(ink); g.fillRect(11, 8 + lift, 13, 5);
            g.setColor(brass); g.fillRect(18, 9 + lift, 4, 2);
            g.setColor(new Color(65, 124, 154)); g.fillRect(10, 17 + lift, 13, 8);
            g.setColor(brass); g.fillRect(5, 18 + lift, 4, 7); g.fillRect(24, 18 + lift, 4, 7);
            g.setColor(mint); g.fillRect(10, 24 + lift, 4, 5 - Math.max(0, stride));
            g.fillRect(20, 24 + lift, 4, 5 + Math.min(0, stride));
            g.setColor(ink); g.fillRect(7 + stride, 29 + lift, 8, 2); g.fillRect(18 - stride, 29 + lift, 8, 2);
            if (i == 4) { g.setColor(brass); g.drawLine(9, 3, 27, 20); g.drawLine(27, 3, 9, 20); }
        } else if (i == 11) {
            g.setColor(ink); g.fillRect(8, 0, 17, 32);
            g.setColor(mint); g.fillRect(11, 0, 11, 32);
            g.setColor(brass); g.fillPolygon(new int[]{16, 24, 16, 8}, new int[]{4, 15, 26, 15}, 4);
            g.setColor(ink); g.fillRect(14, 11, 5, 8);
        } else if (i == 10) {
            g.setColor(new Color(77, 108, 132)); g.fillRect(4, 20, 24, 12);
            g.setColor(mint); g.fillRect(15, 4, 3, 17); g.drawLine(15, 14, 7, 8); g.drawLine(17, 10, 25, 3);
            g.fillOval(3, 4, 8, 5); g.fillOval(21, 0, 8, 5);
        } else {
            g.setColor(ink); g.fillRect(0, 0, 32, 32);
            g.setColor(new Color(50 + (i % 3) * 7, 74, 98)); g.fillRect(1, 3, 30, 28);
            g.setColor(mint); g.fillRect(0, 0, 32, 3);
            g.setColor(new Color(73, 104, 124)); g.drawLine(2, 5, 28, 29); g.drawLine(28, 5, 2, 29);
            g.setColor(brass); g.fillRect(3, 6, 3, 3); g.fillRect(26, 25, 3, 3);
        }
    }

    private static void tone(Path target, int cue) throws Exception {
        int rate = 22050, count = cue == 0 ? rate * 2 : rate / 5 + cue * 300;
        byte[] pcm = new byte[count * 2];
        for (int n = 0; n < count; n++) {
            double t = n / (double) rate, envelope = Math.min(1, n / 200.0) * Math.pow(1 - n / (double) count, 2);
            double frequency = 180 + cue * 29 + (cue % 2 == 0 ? 70 : -50) * t;
            short sample = (short)(7000 * envelope * (Math.sin(2 * Math.PI * frequency * t)
                + .25 * Math.sin(2 * Math.PI * frequency * 1.5 * t)));
            pcm[n * 2] = (byte) sample; pcm[n * 2 + 1] = (byte)(sample >> 8);
        }
        try (var stream = new AudioInputStream(new ByteArrayInputStream(pcm),
                new AudioFormat(rate, 16, 1, true, false), count)) {
            AudioSystem.write(stream, AudioFileFormat.Type.WAVE, target.toFile());
        }
    }

    public static JsonArray level() {
        JsonArray result = new JsonArray();
        JsonObject sky = entity(result, "Distant garden", 7.5, 3.0, 18, 9, -10);
        component(sky, "components.SpriteRenderer", object("sprite", object("texture", object(
            "filepath", ROOT + "sky.png", "width", 1024, "height", 512))));
        for (int x = 0; x < 60; x++) if (!(x >= 18 && x < 20) && !(x >= 33 && x < 35)) {
            for (int y = 0; y < 2; y++) {
                JsonObject tile = entity(result, "Garden deck " + x + "/" + y, x * .25 + .125, y * .25 + .125, .25, .25, 0);
                component(tile, "components.SpriteRenderer", object("sprite", sprite(8 + y)));
                component(tile, "components.Ground", object()); box(tile, .25, .25, false);
            }
        }
        for (int x : new int[]{2, 24, 44, 57}) {
            JsonObject plant = entity(result, "Planter " + x, x * .25 + .125, .625, .25, .25, -1);
            component(plant, "components.SpriteRenderer", object("sprite", sprite(10)));
        }
        JsonObject player = entity(result, "Relay robot", .875, .625, .25, .25, 2);
        component(player, "components.SpriteRenderer", object("sprite", sprite(0)));
        JsonArray states = new JsonArray(), transitions = new JsonArray();
        String[] titles = {"Idle", "Run", "Jump", "Switch Direction", "Die"};
        int[][] frames = {{0}, {1, 0, 2, 0}, {3}, {0}, {4}};
        for (int i = 0; i < titles.length; i++) {
            JsonArray animation = new JsonArray();
            for (int f : frames[i]) animation.add(object("sprite", sprite(f), "frameTime", .12));
            states.add(object("title", titles[i], "animationFrames", animation, "doesLoop", true));
            for (String[] change : new String[][]{{"startRunning", "Run"}, {"stopRunning", "Idle"},
                    {"jump", "Jump"}, {"stopJumping", "Idle"}, {"switchDirection", "Switch Direction"}, {"die", "Die"}})
                transitions.add(array(object("state", titles[i], "trigger", change[0]), new JsonPrimitive(change[1])));
        }
        component(player, "components.StateMachine", object("states", states, "stateTransfers", transitions, "defaultStateTitle", "Idle"));
        component(player, "physics2d.components.PillboxCollider", object("width", .21, "height", .25));
        component(player, "physics2d.components.Rigidbody2D", object("bodyType", "Dynamic", "mass", 25,
            "friction", .8, "fixedRotation", true, "continuousCollision", false));
        component(player, "components.PlayerController", object());
        component(player, "components.FallBoundary", object("minimumY", -.75));
        for (int i = 0; i < 6; i++) {
            JsonObject beacon = entity(result, "Finish beacon " + i, 13.625, .625 + i * .25, .25, .25, 1);
            component(beacon, "components.SpriteRenderer", object("sprite", sprite(11)));
            component(beacon, "components.Flagpole", object("isTop", i == 5)); box(beacon, .25, .25, true);
        }
        return result;
    }

    private static JsonObject entity(JsonArray all, String name, double x, double y, double w, double h, int z) {
        int id = all.size();
        JsonObject entity = object("uid", id, "persistentId", identity("object" + id), "name", name,
            "doSerialization", true, "isDead", false, "components", new JsonArray());
        component(entity, "jade.Transform", object("position", object("x", x, "y", y),
            "scale", object("x", w, "y", h), "rotation", 0, "zIndex", z));
        all.add(entity); return entity;
    }
    private static void box(JsonObject entity, double w, double h, boolean sensor) {
        component(entity, "physics2d.components.Rigidbody2D", object("bodyType", "Static", "gravityScale", 0, "isSensor", sensor));
        component(entity, "physics2d.components.Box2DCollider", object("halfSize", object("x", w, "y", h)));
    }
    private static void component(JsonObject entity, String type, JsonObject properties) {
        JsonArray components = entity.getAsJsonArray("components");
        int id = entity.get("uid").getAsInt() * 16 + components.size();
        properties.addProperty("uid", id); properties.addProperty("persistentId", identity("component" + id));
        components.add(object("type", type, "properties", properties));
    }
    private static JsonObject sprite(int i) {
        double l = (i % 8) / 8.0, r = l + .125, top = 1 - (i / 8) * .5, bottom = top - .5;
        return object("width", 32, "height", 32, "texture", object("filepath", ROOT + "atlas.png", "width", 256, "height", 64),
            "texCoords", array(object("x", r, "y", top), object("x", r, "y", bottom), object("x", l, "y", bottom), object("x", l, "y", top)));
    }
    private static JsonArray array(JsonElement... values) { JsonArray a = new JsonArray(); for (var v : values) a.add(v); return a; }
    private static JsonObject object(Object... fields) {
        JsonObject o = new JsonObject();
        for (int i = 0; i < fields.length; i += 2) o.add((String) fields[i], JSON.toJsonTree(fields[i + 1]));
        return o;
    }
    private static String identity(String value) { return UUID.nameUUIDFromBytes(("relay-garden-v1:" + value).getBytes(StandardCharsets.UTF_8)).toString(); }
    public static String hash(Path p) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(p))); }
}
