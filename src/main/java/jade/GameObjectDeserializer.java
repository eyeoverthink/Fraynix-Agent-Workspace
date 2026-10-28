package jade;

import com.google.gson.*;
import components.Component;

import java.lang.reflect.Type;

public class GameObjectDeserializer implements JsonDeserializer<GameObject> {
    @Override
    public GameObject deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String name = jsonObject.has("name") ? jsonObject.get("name").getAsString() : "GameObject";
        JsonArray components = jsonObject.has("components") ? jsonObject.getAsJsonArray("components") : new JsonArray();

        GameObject go = new GameObject(name);
        int uid = jsonObject.has("uid") ? jsonObject.get("uid").getAsInt() : go.getUid();
        java.util.UUID persistentId = jsonObject.has("persistentId")
                ? java.util.UUID.fromString(jsonObject.get("persistentId").getAsString())
                : (jsonObject.has("entityId") ? java.util.UUID.fromString(jsonObject.get("entityId").getAsString()) : go.getPersistentId());
        go.restoreIdentity(uid, persistentId);
        for (JsonElement e : components) {
            Component c = context.deserialize(e, Component.class);
            go.addComponent(c);
        }
        go.transform = go.getComponent(Transform.class);
        if (go.transform == null) {
            go.addComponent(new Transform());
            go.transform = go.getComponent(Transform.class);
        }
        return go;
    }
}
