
package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.Assert.*;

public class TranslationTimelineTest {
    @Test public void metreKeysApplyToCentimetreSceneAndStaySeparateByObject() throws Exception {
        var original = fixture();
        var objects = original.getJSONArray("objects");
        objects.put(WorldFrames.copy(objects.getJSONObject(2)).put("sourceId", "second"));
        var scene = parse(original);
        var a = TranslationTimeline.edit(scene, "crate", keys(25, new double[]{-1, .75, -.2}));
        var b = TranslationTimeline.edit(scene.put("translationAnimation", a), "second",
                keys(13, new double[]{-1.8, .5, .2}));
        var samples = TranslationTimeline.sample(b);
        assertEquals(24, samples.getInt("fps"));
        assertEquals(25, samples.getInt("endFrame"));
        var positions = samples.getJSONObject("positionsM");
        var first = positions.getJSONArray("crate");
        var second = positions.getJSONArray("second");
        assertArrayEquals(new double[]{-1.5, .5, -.1}, SpatialScene.vector(first.getJSONArray(12), 20), 1e-12);
        assertArrayEquals(new double[]{-1.8, .5, .2}, SpatialScene.vector(second.getJSONArray(12), 20), 1e-12);
        assertEquals(13, second.length());
        assertEquals(1, a.getJSONObject("tracks").length());
        var reopened = parse(original.put("translationAnimation", b));
        assertTrue(b.similar(reopened.getJSONObject("translationAnimation")));
    }

    @Test public void refusesInvalidRestFrameAndStaticTarget() throws Exception {
        var scene = parse(fixture());
        var track = keys(25, new double[]{-1, .25, 0});
        assertThrows(IllegalArgumentException.class, () -> TranslationTimeline.edit(scene, "floor", track));
        track.getJSONObject(0).put("positionM", new JSONArray(new double[]{-2.1, .25, 0}));
        var wrong = assertThrows(IllegalArgumentException.class, () -> TranslationTimeline.edit(scene, "crate", track));
        assertTrue(wrong.getMessage().contains("authored position"));
        assertThrows(IllegalArgumentException.class,
                () -> TranslationTimeline.edit(scene, "crate", keys(722, new double[]{-1, .25, 0})));
    }

    private static JSONArray keys(int end, double[] target) {
        return new JSONArray().put(new JSONObject().put("frame", 1)
                .put("positionM", new JSONArray(new double[]{-2, .25, 0})))
                .put(new JSONObject().put("frame", end).put("positionM", new JSONArray(target)));
    }

    private static JSONObject fixture() throws Exception {
        try (var stream = TranslationTimelineTest.class.getResourceAsStream("/spatial/declared-test-scene-v1.json")) {
            assertNotNull(stream);
            return new JSONObject(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private static JSONObject parse(JSONObject definition) {
        return SpatialScene.parse(definition.toString(), UUID.randomUUID().toString());
    }
}
