package content;

import com.google.gson.*;
import components.SpriteRenderer;
import components.StateMachine;
import fraymus.neural.RelayCourse;
import java.nio.file.*;
import scenes.Scene;
import util.AssetPool;

/** Explicit content profile; legacy scenes and resource paths remain the default. */
public final class RelayGardenResources {
    public static RelayCourse course() {
        String id = System.getProperty("fraynix.content");
        if (id == null) return null;
        RelayCourse course = RelayCourse.find(id);
        if (course == null) throw new IllegalStateException("Unknown original course: " + id);
        return course;
    }

    public static boolean enabled() { return course() != null; }

    public static void verify() {
        try {
            RelayCourse course = course();
            Path manifest = Path.of(course.manifest());
            if (!RelayGardenGenerator.hash(manifest).equals(System.getProperty("fraynix.content.sha256")))
                throw new IllegalStateException("Original content manifest changed");
            JsonObject files = JsonParser.parseString(Files.readString(manifest)).getAsJsonObject().getAsJsonObject("files");
            if (!files.keySet().equals(course.files())) throw new IllegalStateException("Incomplete original content");
            for (var entry : files.entrySet()) {
                if (!entry.getValue().getAsString().equals(RelayGardenGenerator.hash(Path.of(entry.getKey()))))
                    throw new IllegalStateException("Original content changed: " + entry.getKey());
            }
        } catch (Exception e) { throw new IllegalStateException("Original course verification refused", e); }
    }

    public static void load(Scene scene) {
        verify();
        AssetPool.getShader("assets/shaders/default.glsl");
        for (String cue : RelayCourse.CUES)
            AssetPool.addSound("assets/sounds/" + cue + ".ogg", cue.equals("main-theme-overworld"));
        var verifiedPaths = course().files();
        for (var object : scene.getGameObjects()) {
            var sprite = object.getComponent(SpriteRenderer.class);
            if (sprite != null && sprite.getTexture() != null) {
                String path = sprite.getTexture().getFilepath();
                if (!verifiedPaths.contains(path)) throw new IllegalStateException("Non-pack sprite refused");
                sprite.setTexture(AssetPool.getTexture(path));
            }
            var state = object.getComponent(StateMachine.class);
            if (state != null) state.refreshTextures();
        }
        scene.camera().clearColor.set(.04f, .07f, .13f, 1);
    }

    public static String soundPath(String requested) {
        if (!enabled()) return requested;
        String name = Path.of(requested).getFileName().toString().replace(".ogg", "");
        if (!RelayCourse.CUES.contains(name))
            throw new IllegalArgumentException("Unknown original sound cue");
        return RelayGardenGenerator.ROOT + name + ".wav";
    }
}
