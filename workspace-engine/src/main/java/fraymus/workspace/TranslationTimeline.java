
package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

/** Frame-one rest pose and linear translation tracks in workspace metres. */
final class TranslationTimeline {
    static final int FPS = 24;
    static final int LAST_FRAME = 721;
    private static final int MAX_KEYS = 32;
    private static final double REST_TOLERANCE_M = 1e-6;

    private TranslationTimeline() {}

    static JSONObject checked(JSONObject raw, JSONArray objects) {
        SpatialScene.keys(raw, "fps", "tracks");
        SpatialScene.number(raw, "fps", FPS, FPS);
        var tracks = raw.getJSONObject("tracks");
        if (tracks.isEmpty() || tracks.length() > 8) {
            throw new IllegalArgumentException("Choose 1–8 animated dynamic props.");
        }
        for (String sourceId : tracks.keySet()) {
            checkedKeys(tracks.getJSONArray(sourceId), dynamic(objects, sourceId).getJSONArray("position"));
        }
        return raw;
    }

    static JSONObject edit(JSONObject scene, String sourceId, JSONArray keys) {
        checkedKeys(keys, dynamic(scene.getJSONArray("objects"), sourceId).getJSONArray("position"));
        var existing = scene.optJSONObject("translationAnimation");
        var trackSet = existing == null ? new JSONObject().put("fps", FPS).put("tracks", new JSONObject())
                : WorldFrames.copy(existing);
        trackSet.getJSONObject("tracks").put(sourceId, keys);
        return checked(trackSet, scene.getJSONArray("objects"));
    }

    static JSONObject sample(JSONObject animation) {
        var tracks = animation.getJSONObject("tracks");
        var samples = new JSONObject();
        int end = 1;
        for (String sourceId : tracks.keySet()) {
            var keys = tracks.getJSONArray(sourceId);
            int last = frame(keys.getJSONObject(keys.length() - 1));
            var positions = new JSONArray();
            for (int segment = 1; segment < keys.length(); segment++) {
                var left = keys.getJSONObject(segment - 1);
                var right = keys.getJSONObject(segment);
                int a = frame(left);
                int b = frame(right);
                var start = left.getJSONArray("positionM");
                var finish = right.getJSONArray("positionM");
                for (int t = a; t < b; t++) {
                    double ratio = (t - a) / (double) (b - a);
                    var point = new JSONArray();
                    for (int axis = 0; axis < 3; axis++) {
                        double m = start.getDouble(axis);
                        point.put(m + (finish.getDouble(axis) - m) * ratio);
                    }
                    positions.put(point);
                }
            }
            positions.put(keys.getJSONObject(keys.length() - 1).getJSONArray("positionM"));
            end = Math.max(end, last);
            samples.put(sourceId, positions);
        }
        return new JSONObject().put("fps", FPS).put("endFrame", end).put("positionsM", samples);
    }

    private static JSONObject dynamic(JSONArray objects, String sourceId) {
        for (Object raw : objects) {
            var body = (JSONObject) raw;
            if (body.getString("sourceId").equals(sourceId) && body.getDouble("massKg") > 0) return body;
        }
        throw new IllegalArgumentException("Choose a dynamic prop in this scene.");
    }

    private static void checkedKeys(JSONArray keys, JSONArray rest) {
        if (keys.length() < 2 || keys.length() > MAX_KEYS) {
            throw new IllegalArgumentException("Use 2–32 translation keys for a prop.");
        }
        int previous = 0;
        for (int i = 0; i < keys.length(); i++) {
            var key = keys.getJSONObject(i);
            SpatialScene.keys(key, "frame", "positionM");
            int frame = frame(key);
            if (frame <= previous || i == 0 && frame != 1) {
                throw new IllegalArgumentException("Use strictly increasing keyframes starting at frame 1.");
            }
            previous = frame;
            double[] point = SpatialScene.vector(key.getJSONArray("positionM"), 20);
            if (i == 0) for (int axis = 0; axis < 3; axis++) {
                if (Math.abs(point[axis] - rest.getDouble(axis)) > REST_TOLERANCE_M) {
                    throw new IllegalArgumentException("Frame 1 must match the authored position.");
                }
            }
        }
    }

    private static int frame(JSONObject key) {
        double value = SpatialScene.number(key, "frame", 1, LAST_FRAME);
        if (value != (int) value) throw new IllegalArgumentException("Use whole keyframe numbers.");
        return (int) value;
    }
}
