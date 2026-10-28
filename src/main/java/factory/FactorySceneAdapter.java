package factory;

import com.google.gson.*;
import components.Component;
import components.ComponentDeserializer;
import fraymus.factory.*;
import jade.*;
import scenes.Scene;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Two-way compatibility adapter. Persistent entity/component UUIDs belong to the shared runtime. */
public final class FactorySceneAdapter {
    private static final String OBJECT = "editor.object";
    private final Gson gson = new GsonBuilder().enableComplexMapKeySerialization().registerTypeAdapter(Component.class, new ComponentDeserializer())
        .registerTypeAdapter(GameObject.class, new GameObjectDeserializer()).create();

    public static void registerSchemas(SchemaRegistry registry) {
        registry.register(new ComponentSchema(OBJECT, 1, Map.of("pickingId", ComponentSchema.Kind.NUMBER), Set.of("pickingId")));
        for (String type : ComponentDeserializer.allowedTypes()) {
            Map<String, ComponentSchema.Kind> fields = new LinkedHashMap<>();
            fields.put("json", ComponentSchema.Kind.TEXT);
            if (type.equals("jade.Transform")) {
                for (String key : List.of("x", "y", "scaleX", "scaleY", "rotation", "zIndex")) fields.put(key, ComponentSchema.Kind.NUMBER);
            }
            registry.register(new ComponentSchema("editor." + type, 1, fields, fields.keySet()));
        }
    }

    public void capture(Scene scene, FactoryWorld world) {
        List<EntityRecord> entities = new ArrayList<>();
        for (EntityRecord prior : world.snapshot().entities()) {
            if (prior.components().stream().noneMatch(c -> c.type().equals(OBJECT))) entities.add(prior);
        }
        for (GameObject object : scene.getGameObjects()) {
            if (!object.doSerialization() || object.isDead()) continue;
            physics2d.Physics2D.validate(object);
            List<ComponentRecord> components = new ArrayList<>();
            UUID metaId = UUID.nameUUIDFromBytes((object.getPersistentId() + ":editor-meta").getBytes(StandardCharsets.UTF_8));
            components.add(new ComponentRecord(metaId, OBJECT, 1, Map.of("pickingId", Integer.toString(object.getUid()))));
            for (Component component : object.getAllComponents()) {
                String type = component.getClass().getCanonicalName();
                if (!ComponentDeserializer.allowedTypes().contains(type)) throw new IllegalArgumentException("Unregistered editor component: " + type);
                Map<String, String> fields = new LinkedHashMap<>();
                fields.put("json", gson.toJson(component, component.getClass()));
                if (component instanceof Transform transform) {
                    fields.put("x", Float.toString(transform.position.x)); fields.put("y", Float.toString(transform.position.y));
                    fields.put("scaleX", Float.toString(transform.scale.x)); fields.put("scaleY", Float.toString(transform.scale.y));
                    fields.put("rotation", Float.toString(transform.rotation)); fields.put("zIndex", Integer.toString(transform.zIndex));
                }
                components.add(new ComponentRecord(component.getPersistentId(), "editor." + type, 1, fields));
            }
            entities.add(new EntityRecord(object.getPersistentId(), object.getName(), components));
        }
        if (!world.snapshot().entities().equals(entities)) world.replace(world.snapshot().revision(), entities);
    }

    public void restore(FactoryWorld world, Scene target) {
        if (!target.getGameObjects().isEmpty()) throw new IllegalArgumentException("Restore requires an empty scene");
        List<GameObject> decoded = new ArrayList<>();
        Set<Integer> usedHandles = new HashSet<>(), usedComponentHandles = new HashSet<>();
        for (EntityRecord entity : world.snapshot().entities()) {
            ComponentRecord meta = entity.components().stream().filter(c -> c.type().equals(OBJECT)).findFirst().orElse(null);
            if (meta == null) continue;
            JsonObject json = new JsonObject(); json.addProperty("name", entity.name());
            json.addProperty("uid", Integer.parseInt(meta.values().get("pickingId"))); json.addProperty("persistentId", entity.id().toString());
            JsonArray components = new JsonArray();
            for (ComponentRecord component : entity.components()) {
                if (component.type().equals(OBJECT)) continue;
                if (!component.type().startsWith("editor.")) throw new IllegalArgumentException("Invalid scene component");
                String type = component.type().substring("editor.".length());
                if (!ComponentDeserializer.allowedTypes().contains(type)) throw new IllegalArgumentException("Unregistered editor component");
                JsonObject properties = JsonParser.parseString(component.values().get("json")).getAsJsonObject();
                properties.addProperty("persistentId", component.id().toString());
                if (type.equals("jade.Transform")) {
                    properties.add("position", vector(component.values().get("x"), component.values().get("y")));
                    properties.add("scale", vector(component.values().get("scaleX"), component.values().get("scaleY")));
                    properties.addProperty("rotation", Float.parseFloat(component.values().get("rotation")));
                    properties.addProperty("zIndex", Integer.parseInt(component.values().get("zIndex")));
                }
                JsonObject wrapper = new JsonObject(); wrapper.addProperty("type", type); wrapper.add("properties", properties); components.add(wrapper);
            }
            json.add("components", components);
            GameObject object = gson.fromJson(json, GameObject.class);
            physics2d.Physics2D.validate(object);
            Set<Class<?>> componentTypes = new HashSet<>();
            for (Component component : object.getAllComponents()) {
                if (!componentTypes.add(component.getClass())) throw new IllegalArgumentException("Duplicate component type: " + component.getClass().getSimpleName());
            }
            while (!usedHandles.add(object.getUid())) object.generateUid();
            for (Component component : object.getAllComponents()) {
                while (!usedComponentHandles.add(component.getUid())) component.regenerateEditorId();
            }
            decoded.add(object);
        }
        for (GameObject object : decoded) target.addGameObjectToScene(object);
    }

    public String verifyRoundTrip(Scene source, SchemaRegistry schemas) {
        FactoryWorld before = new FactoryWorld(schemas); capture(source, before);
        Scene decoded = new Scene(null); restore(before, decoded);
        FactoryWorld after = new FactoryWorld(schemas); capture(decoded, after);
        if (!before.snapshot().entities().equals(after.snapshot().entities())) throw new IllegalStateException("Scene component values changed during deserialization");
        long objects = decoded.getGameObjects().size();
        long bodies = decoded.getGameObjects().stream().filter(o -> o.getComponent(physics2d.components.Rigidbody2D.class) != null).count();
        long boxes = decoded.getGameObjects().stream().filter(o -> o.getComponent(physics2d.components.Box2DCollider.class) != null).count();
        return "Round-trip verified: " + objects + " objects, " + bodies + " rigidbodies, " + boxes + " box colliders";
    }

    private static JsonObject vector(String x, String y) {
        JsonObject result = new JsonObject(); result.addProperty("x", Float.parseFloat(x)); result.addProperty("y", Float.parseFloat(y)); return result;
    }
}
