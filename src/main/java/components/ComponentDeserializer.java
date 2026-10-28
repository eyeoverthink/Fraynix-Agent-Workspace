package components;

import com.google.gson.*;
import components.Component;

import java.lang.reflect.Type;

public class ComponentDeserializer implements JsonSerializer<Component>,
        JsonDeserializer<Component> {

    // Stable external schemas will replace these compatibility payloads progressively.
    private static final java.util.Set<String> ALLOWED = java.util.Set.of(
        "jade.Transform", "components.SpriteRenderer", "components.NonPickable", "components.FontRenderer",
        "components.GridLines", "components.MouseControls", "components.EditorCamera", "components.Gizmo",
        "components.GizmoSystem", "components.ScaleGizmo", "components.TranslateGizmo",
        "physics2d.components.Rigidbody2D", "physics2d.components.Box2DCollider", "physics2d.components.CircleCollider", "physics2d.components.PillboxCollider",
        "components.Block", "components.BlockCoin", "components.BreakableBrick", "components.Coin", "components.Fireball", "components.Flagpole", "components.Flower", "components.GameCamera", "components.GoombaAI", "components.Ground", "components.KeyControls", "components.MushroomAI", "components.Pipe", "components.PlayerController", "components.QuestionBlock", "components.StateMachine", "components.TurtleAI"
    );

    public static java.util.Set<String> allowedTypes() { return ALLOWED; }

    @Override
    public Component deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String type = jsonObject.get("type").getAsString();
        if (!ALLOWED.contains(type)) throw new JsonParseException("Unregistered component type: " + type);
        JsonElement element = jsonObject.get("properties");

        try {
            return context.deserialize(element, Class.forName(type));
        } catch (ClassNotFoundException e) {
            throw new JsonParseException("Unknown element type: " + type, e);
        }
    }

    @Override
    public JsonElement serialize(Component src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject result = new JsonObject();
        result.add("type", new JsonPrimitive(src.getClass().getCanonicalName()));
        result.add("properties", context.serialize(src, src.getClass()));
        return result;
    }
}
