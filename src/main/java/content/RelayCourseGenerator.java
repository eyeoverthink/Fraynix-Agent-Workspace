package content;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fraymus.neural.RelayCourse;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;

import static content.RelayGardenGenerator.*;

/** Builds original layouts from tile coordinates and draws their contact markers. */
public final class RelayCourseGenerator {
    private record Deck(int first, int end, int height) {}
    private record Signal(String name, double x, double floor, boolean hazard) {}
    private record Layout(List<Deck> decks, List<Signal> signals, double finishX, double finishY) {}

    private static Layout layout(RelayCourse course) {
        return switch (course) {
            case CIRCUIT -> new Layout(List.of(new Deck(0, 24, 2), new Deck(27, 55, 2)),
                    List.of(new Signal("Copper cell", 2, .5, false),
                            new Signal("Live rail", 4.125, .5, true),
                            new Signal("Mint cell", 8.625, .5, false)),
                    12.625, .625);
            case TERRACES -> new Layout(List.of(new Deck(0, 14, 2), new Deck(14, 24, 3),
                    new Deck(26, 40, 4), new Deck(40, 58, 2)),
                    List.of(new Signal("Lower cell", 1.875, .5, false),
                            new Signal("Midway cell", 4.875, .75, false),
                            new Signal("High rail", 8.875, 1, true),
                            new Signal("Return cell", 11.625, .5, false)),
                    13.625, .625);
            case SWITCHYARD -> new Layout(List.of(new Deck(0, 40, 2), new Deck(42, 59, 2)),
                    List.of(new Signal("Yard cell", 1.875, .5, false),
                            new Signal("West rail", 3.375, .5, true),
                            new Signal("Signal cell", 6.125, .5, false),
                            new Signal("East rail", 7.375, .5, true),
                            new Signal("Return cell", 12.125, .5, false)),
                    13.75, .625);
            case RIDGE -> new Layout(List.of(new Deck(0, 12, 2), new Deck(12, 36, 3),
                    new Deck(36, 60, 2)),
                    List.of(new Signal("Lower cell", 1.875, .5, false),
                            new Signal("Ridge cell", 4.375, .75, false),
                            new Signal("First ridge rail", 6.375, .75, true),
                            new Signal("Second ridge rail", 7, .75, true),
                            new Signal("Lower rail", 11.5, .5, true),
                            new Signal("Finish cell", 13, .5, false)),
                    14.125, .625);
            case GARDEN -> throw new IllegalArgumentException("Use the existing Garden generator");
        };
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Choose a new output directory");
        generate(Path.of(args[0]));
    }

    public static void generate(Path out) throws Exception {
        if (Files.exists(out)) throw new IllegalArgumentException("Generation requires a new directory");
        Files.createDirectories(out);
        drawSignals(out.resolve("signals.png"));
        for (RelayCourse course : RelayCourse.values()) {
            if (!course.hasSignals()) continue;
            Layout layout = layout(course);
            Path manifestFile = relative(out, course.manifest());
            Files.createDirectories(manifestFile.getParent());
            Files.writeString(relative(out, course.level()), pretty(level(course, layout)));
            JsonObject manifest = object("id", course.id(), "title", course.title(), "originalContent", true,
                    "level", course.level(), "startX", .875, "finishX", layout.finishX(),
                    "generator", "src/main/java/content/RelayCourseGenerator.java");
            JsonObject files = new JsonObject();
            for (String name : course.files()) {
                Path source = name.startsWith(RelayCourse.ASSETS + "courses/") ? relative(out, name) : Path.of(name);
                files.addProperty(name, hash(source));
            }
            manifest.add("files", files);
            Files.writeString(manifestFile, pretty(manifest));
        }
    }

    private static String pretty(JsonArray value) {
        return new GsonBuilder().setPrettyPrinting().create().toJson(value) + "\n";
    }

    private static String pretty(JsonObject value) {
        return new GsonBuilder().setPrettyPrinting().create().toJson(value) + "\n";
    }

    private static Path relative(Path out, String path) {
        return out.resolve(path.substring((RelayCourse.ASSETS + "courses/").length()));
    }

    private static JsonArray level(RelayCourse course, Layout setup) {
        var level = new JsonArray();
        backdrop(level);
        for (Deck deck : setup.decks()) {
            for (int x = deck.first(); x < deck.end(); x++) {
                for (int y = 0; y < deck.height(); y++) {
                    deck(level, "Deck " + x + "/" + y, x, y, y == deck.height() - 1 ? 0 : 1);
                }
            }
            planter(level, deck.first() + 1, .125 + deck.height() * .25);
        }
        JsonObject player = robot(level);
        long count = setup.signals().stream().filter(signal -> !signal.hazard()).count();
        component(player, "components.CourseProgress", object("totalPickups", count));
        for (Signal signal : setup.signals()) addSignal(level, signal);
        beacon(level, setup.finishX(), setup.finishY());
        for (var row : level) {
            JsonObject object = row.getAsJsonObject();
            identify(course, object, "object");
            for (var c : object.getAsJsonArray("components")) {
                identify(course, c.getAsJsonObject().getAsJsonObject("properties"), "component");
            }
        }
        return level;
    }

    private static void identify(RelayCourse course, JsonObject object, String type) {
        String key = course.id() + ":" + type + object.get("uid").getAsInt();
        object.addProperty("persistentId", UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString());
    }

    private static void addSignal(JsonArray level, Signal signal) {
        double width = signal.hazard() ? .35 : .24;
        double height = signal.hazard() ? .25 : .23;
        var object = entity(level, signal.name(), signal.x(), signal.floor() + .125, width, height, 1);
        double left = signal.hazard() ? .5 : 0;
        double right = left + .5;
        var sprite = object("width", 32, "height", 32, "texture",
                object("filepath", RelayCourse.OBJECTS, "width", 64, "height", 32),
                "texCoords", array(object("x", right, "y", 1), object("x", right, "y", 0),
                        object("x", left, "y", 0), object("x", left, "y", 1)));
        component(object, "components.SpriteRenderer", object("sprite", sprite));
        component(object, "components.CourseContact", object("kind", signal.hazard() ? "HAZARD" : "PICKUP"));
        box(object, width, height, true);
    }

    private static void drawSignals(Path destination) throws Exception {
        var image = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();
        g.setColor(new Color(15, 37, 55));
        g.fillPolygon(new int[]{16, 31, 16, 1}, new int[]{0, 15, 31, 15}, 4);
        g.setColor(new Color(108, 251, 224));
        g.fillPolygon(new int[]{16, 27, 16, 5}, new int[]{4, 15, 27, 15}, 4);
        g.setColor(new Color(244, 255, 227));
        g.fillRect(12, 10, 8, 10);
        g.setColor(new Color(255, 108, 94));
        g.fillRect(32, 22, 32, 10);
        for (int x = 32; x < 61; x += 10) {
            g.fillPolygon(new int[]{x, x + 5, x + 10}, new int[]{26, 1, 26}, 3);
        }
        g.setColor(new Color(255, 219, 137));
        g.fillRect(32, 26, 32, 3);
        g.dispose();
        ImageIO.write(image, "png", destination.toFile());
    }
}
