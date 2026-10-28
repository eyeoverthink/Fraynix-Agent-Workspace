package editor;

import com.google.gson.*;
import components.Component;
import components.ComponentDeserializer;
import factory.SceneProposal;
import jade.GameObject;
import jade.GameObjectDeserializer;
import jade.Transform;
import jade.Window;
import scenes.Scene;
import util.AssetPool;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deserializes GameObject / Transform JSON (via Gson GameObjectDeserializer & ComponentDeserializer)
 * or SceneProposal operations from Chat responses and applies them directly to the live Scene.
 */
public final class ChatSceneBridge {
    private static final Pattern FENCED_JSON = Pattern.compile("```(?:json)?\\s*([\\[{][\\s\\S]*?[\\]}])\\s*```", Pattern.CASE_INSENSITIVE);
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Component.class, new ComponentDeserializer())
            .registerTypeAdapter(GameObject.class, new GameObjectDeserializer())
            .create();

    private ChatSceneBridge() {}

    public static String applyIfPresent(Scene scene, String reply, String thinking) {
        if (scene == null || Window.isPlaying()) return "";
        List<String> candidates = extractJsonCandidates(reply);
        if (candidates.isEmpty() && thinking != null && !thinking.isBlank()) {
            candidates = extractJsonCandidates(thinking);
        }
        if (candidates.isEmpty()) return "";

        List<String> appliedSummaries = new ArrayList<>();
        for (String raw : candidates) {
            try {
                JsonElement parsed = JsonParser.parseString(raw);
                String result = applyElement(scene, parsed);
                if (!result.isBlank()) {
                    appliedSummaries.add(result);
                }
            } catch (Exception ignored) {
                // Candidate block was not a valid scene payload; try next block
            }
        }

        if (!appliedSummaries.isEmpty()) {
            try {
                Window.getFactorySession().save(scene);
                scene.save();
            } catch (Exception ignored) {}
            return "Scene updated: " + String.join("; ", appliedSummaries);
        }
        return "";
    }

    public static List<String> extractJsonCandidates(String text) {
        if (text == null || text.isBlank()) return List.of();
        List<String> results = new ArrayList<>();
        Matcher matcher = FENCED_JSON.matcher(text);
        while (matcher.find()) {
            results.add(matcher.group(1).trim());
        }
        if (!results.isEmpty()) return results;

        // Look for raw top-level JSON object or array
        int firstBrace = text.indexOf('{');
        int lastBrace = text.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            results.add(text.substring(firstBrace, lastBrace + 1).trim());
        }
        int firstBracket = text.indexOf('[');
        int lastBracket = text.lastIndexOf(']');
        if (firstBracket >= 0 && lastBracket > firstBracket) {
            results.add(text.substring(firstBracket, lastBracket + 1).trim());
        }
        return results;
    }

    private static String applyElement(Scene scene, JsonElement parsed) throws Exception {
        if (parsed.isJsonArray()) {
            return applyGameObjectArray(scene, parsed.getAsJsonArray());
        }
        if (!parsed.isJsonObject()) return "";
        JsonObject obj = parsed.getAsJsonObject();

        // 1. Check for SceneProposal operations format: {"kind":"proposal","operations":[...]} or {"operations":[...]}
        if (obj.has("operations") && obj.get("operations").isJsonArray()) {
            return applyOperations(scene, obj);
        }

        // 2. Check for wrapper object containing "entities" or "gameObjects" array
        if (obj.has("entities") && obj.get("entities").isJsonArray()) {
            return applyGameObjectArray(scene, obj.getAsJsonArray("entities"));
        }
        if (obj.has("gameObjects") && obj.get("gameObjects").isJsonArray()) {
            return applyGameObjectArray(scene, obj.getAsJsonArray("gameObjects"));
        }

        // 3. Single GameObject or entity patch object
        if (obj.has("components") || obj.has("entityId") || obj.has("persistentId") || obj.has("uid") || obj.has("position") || (obj.has("name") && (obj.has("x") || obj.has("y")))) {
            JsonArray single = new JsonArray();
            single.add(obj);
            return applyGameObjectArray(scene, single);
        }
        return "";
    }

    private static String applyOperations(Scene scene, JsonObject proposalObj) throws Exception {
        var session = Window.getFactorySession();
        var selected = Window.getImguiLayer().getPropertiesWindow().getActiveGameObject();
        var sheet = AssetPool.getSpritesheet("assets/images/spritesheets/decorationsAndBlocks.png");
        var observation = session.observe(scene, selected == null ? null : selected.getPersistentId(), Window.isPlaying(), sheet.size());

        JsonObject normalized = proposalObj.deepCopy();
        normalized.addProperty("kind", "proposal");
        normalized.addProperty("observationId", observation.fingerprint().substring(0, 16));
        if (!normalized.has("summary") || normalized.get("summary").isJsonNull()) {
            normalized.addProperty("summary", "Chat scene edit");
        }

        // Support shorthand "move" operation by converting to "set" on jade.Transform
        JsonArray ops = normalized.getAsJsonArray("operations");
        for (int i = 0; i < ops.size(); i++) {
            if (!ops.get(i).isJsonObject()) continue;
            JsonObject op = ops.get(i).getAsJsonObject();
            if (op.has("op") && "move".equalsIgnoreCase(op.get("op").getAsString())) {
                op.addProperty("op", "set");
                op.addProperty("component", "jade.Transform");
                JsonObject values = op.has("values") && op.get("values").isJsonObject() ? op.getAsJsonObject("values") : new JsonObject();
                if (op.has("x")) { values.addProperty("position.x", op.get("x").getAsFloat()); op.remove("x"); }
                if (op.has("y")) { values.addProperty("position.y", op.get("y").getAsFloat()); op.remove("y"); }
                op.add("values", values);
            }
        }

        try {
            var draft = SceneProposal.stage(normalized.toString(), observation, session.schemas(), sheet::getSprite);
            session.commitScene(scene, draft.candidate(), observation.fingerprint(), Window::installFactoryScene);
            return String.join(", ", draft.changes());
        } catch (Exception stagingFailure) {
            // Fallback: apply "set" operations directly to matching GameObjects in scene
            List<String> directChanges = new ArrayList<>();
            for (JsonElement el : ops) {
                if (!el.isJsonObject()) continue;
                JsonObject op = el.getAsJsonObject();
                String opName = op.has("op") ? op.get("op").getAsString() : "";
                if ("set".equalsIgnoreCase(opName) && op.has("entityId") && op.has("values") && op.get("values").isJsonObject()) {
                    GameObject target = findGameObject(scene, op.get("entityId").getAsString(), -1, null);
                    if (target != null && target.transform != null) {
                        JsonObject values = op.getAsJsonObject("values");
                        if (values.has("position.x")) target.transform.position.x = values.get("position.x").getAsFloat();
                        if (values.has("position.y")) target.transform.position.y = values.get("position.y").getAsFloat();
                        if (values.has("scale.x")) target.transform.scale.x = values.get("scale.x").getAsFloat();
                        if (values.has("scale.y")) target.transform.scale.y = values.get("scale.y").getAsFloat();
                        if (values.has("rotation")) target.transform.rotation = values.get("rotation").getAsFloat();
                        if (values.has("zIndex")) target.transform.zIndex = values.get("zIndex").getAsInt();
                        scene.physics().synchronize(target);
                        directChanges.add(target.getName() + " -> (" + target.transform.position.x + ", " + target.transform.position.y + ")");
                    }
                }
            }
            if (!directChanges.isEmpty()) return String.join(", ", directChanges);
            throw stagingFailure;
        }
    }

    private static String applyGameObjectArray(Scene scene, JsonArray array) {
        List<String> changes = new ArrayList<>();
        for (JsonElement elem : array) {
            if (!elem.isJsonObject()) continue;
            JsonObject obj = elem.getAsJsonObject();

            String idStr = obj.has("persistentId") ? obj.get("persistentId").getAsString()
                    : (obj.has("entityId") ? obj.get("entityId").getAsString() : null);
            int uid = obj.has("uid") && obj.get("uid").isJsonPrimitive() ? obj.get("uid").getAsInt() : -1;
            String name = obj.has("name") && obj.get("name").isJsonPrimitive() ? obj.get("name").getAsString() : null;

            GameObject existing = findGameObject(scene, idStr, uid, name);

            // Deserialize via Gson GameObjectDeserializer & ComponentDeserializer
            GameObject deserialized = null;
            if (obj.has("components") && obj.get("components").isJsonArray()) {
                try {
                    deserialized = GSON.fromJson(obj, GameObject.class);
                } catch (Exception ignored) {}
            }

            if (existing != null) {
                boolean modified = false;
                float beforeX = existing.transform.position.x;
                float beforeY = existing.transform.position.y;
                float beforeScaleX = existing.transform.scale.x;
                float beforeScaleY = existing.transform.scale.y;
                float beforeRot = existing.transform.rotation;

                if (deserialized != null && deserialized.transform != null) {
                    Transform dt = deserialized.transform;
                    if (Math.abs(dt.position.x - beforeX) > 1e-5f || Math.abs(dt.position.y - beforeY) > 1e-5f
                            || Math.abs(dt.scale.x - beforeScaleX) > 1e-5f || Math.abs(dt.scale.y - beforeScaleY) > 1e-5f
                            || Math.abs(dt.rotation - beforeRot) > 1e-5f || dt.zIndex != existing.transform.zIndex) {
                        existing.transform.position.set(dt.position.x, dt.position.y);
                        if (dt.scale.x > 0 && dt.scale.y > 0) {
                            existing.transform.scale.set(dt.scale.x, dt.scale.y);
                        }
                        existing.transform.rotation = dt.rotation;
                        existing.transform.zIndex = dt.zIndex;
                        modified = true;
                    }
                }

                // Also support direct top-level position / x / y fields if model omitted nested components wrapper
                if (obj.has("position") && obj.get("position").isJsonObject()) {
                    JsonObject pos = obj.getAsJsonObject("position");
                    if (pos.has("x")) { existing.transform.position.x = pos.get("x").getAsFloat(); modified = true; }
                    if (pos.has("y")) { existing.transform.position.y = pos.get("y").getAsFloat(); modified = true; }
                }
                if (obj.has("x") && obj.get("x").isJsonPrimitive()) {
                    existing.transform.position.x = obj.get("x").getAsFloat(); modified = true;
                }
                if (obj.has("y") && obj.get("y").isJsonPrimitive()) {
                    existing.transform.position.y = obj.get("y").getAsFloat(); modified = true;
                }

                if (modified) {
                    scene.physics().synchronize(existing);
                    changes.add(String.format(Locale.ROOT, "%s moved from (%.2f, %.2f) to (%.2f, %.2f)",
                            existing.getName(), beforeX, beforeY, existing.transform.position.x, existing.transform.position.y));
                }
            } else if (deserialized != null && obj.has("components")) {
                scene.addGameObjectToScene(deserialized);
                changes.add(String.format(Locale.ROOT, "Added %s at (%.2f, %.2f)",
                        deserialized.getName(), deserialized.transform.position.x, deserialized.transform.position.y));
            }
        }
        return String.join(", ", changes);
    }

    private static GameObject findGameObject(Scene scene, String idStr, int uid, String name) {
        if (idStr != null && !idStr.isBlank()) {
            try {
                UUID targetId = UUID.fromString(idStr.trim());
                for (GameObject go : scene.getGameObjects()) {
                    if (go.doSerialization() && !go.isDead() && go.getPersistentId().equals(targetId)) {
                        return go;
                    }
                }
            } catch (IllegalArgumentException ignored) {}
        }
        if (uid >= 0) {
            GameObject byUid = scene.getGameObject(uid);
            if (byUid != null && byUid.doSerialization() && !byUid.isDead()) return byUid;
        }
        if (name != null && !name.isBlank()) {
            for (GameObject go : scene.getGameObjects()) {
                if (go.doSerialization() && !go.isDead() && go.getName().equalsIgnoreCase(name.trim())) {
                    return go;
                }
            }
        }
        return null;
    }
}
