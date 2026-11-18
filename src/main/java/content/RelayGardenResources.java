package content;

import com.google.gson.*;
import components.SpriteRenderer;
import components.StateMachine;
import java.nio.file.*;
import scenes.Scene;
import util.AssetPool;

/** Explicit content profile; legacy scenes and resource paths remain the default. */
public final class RelayGardenResources {
    public static boolean enabled() { return "relay-garden-v1".equals(System.getProperty("fraynix.content")); }

    public static void verify() {
        try {
            Path manifest = Path.of(RelayGardenGenerator.ROOT + "manifest.json");
            if (!RelayGardenGenerator.hash(manifest).equals(System.getProperty("fraynix.content.sha256")))
                throw new IllegalStateException("Original content manifest changed");
            JsonObject files = JsonParser.parseString(Files.readString(manifest)).getAsJsonObject().getAsJsonObject("files");
            for (var entry : files.entrySet()) {
                if (!entry.getKey().startsWith(RelayGardenGenerator.ROOT) || entry.getKey().contains("..")
                        || !entry.getValue().getAsString().equals(RelayGardenGenerator.hash(Path.of(entry.getKey()))))
                    throw new IllegalStateException("Original content changed: " + entry.getKey());
            }
        } catch (Exception e) { throw new IllegalStateException("Relay Garden verification refused", e); }
    }

    public static void load(Scene scene) {
        verify();
        AssetPool.getShader("assets/shaders/default.glsl");
        for (String cue : RelayGardenGenerator.CUES)
            AssetPool.addSound("assets/sounds/" + cue + ".ogg", cue.equals("main-theme-overworld"));
        for (var object : scene.getGameObjects()) {
            var sprite = object.getComponent(SpriteRenderer.class);
            if (sprite != null && sprite.getTexture() != null) {
                String path = sprite.getTexture().getFilepath();
                if (!path.startsWith(RelayGardenGenerator.ROOT)) throw new IllegalStateException("Non-pack sprite refused");
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
        if (!java.util.Arrays.asList(RelayGardenGenerator.CUES).contains(name))
            throw new IllegalArgumentException("Unknown original sound cue");
        return RelayGardenGenerator.ROOT + name + ".wav";
    }
}
