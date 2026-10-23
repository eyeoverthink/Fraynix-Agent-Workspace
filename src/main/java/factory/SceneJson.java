package factory;

import com.google.gson.*;
import com.google.gson.stream.*;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** Strict command JSON and canonical content hashes; no executable expressions. */
public final class SceneJson {
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private SceneJson() {}
    public static JsonObject parse(String text) {
        String source = text.trim();
        if (source.startsWith("```json\n") && source.endsWith("```")) source = source.substring(8, source.length() - 3).trim();
        try (var reader = new JsonReader(new StringReader(source))) {
            reader.setLenient(false); JsonElement element = read(reader, 0);
            if (!element.isJsonObject() || reader.peek() != JsonToken.END_DOCUMENT) throw new IllegalArgumentException("Expected one JSON object");
            return element.getAsJsonObject();
        } catch (IOException | NumberFormatException error) { throw new IllegalArgumentException("Invalid scene command JSON: " + error.getMessage(), error); }
    }
    private static JsonElement read(JsonReader reader, int depth) throws IOException {
        if (depth > 64) throw new IllegalArgumentException("JSON nesting exceeds the scene command format");
        return switch (reader.peek()) {
            case BEGIN_OBJECT -> {
                JsonObject object = new JsonObject(); reader.beginObject();
                while (reader.hasNext()) { String key = reader.nextName(); if (object.has(key)) throw new IllegalArgumentException("Duplicate JSON field: " + key); object.add(key, read(reader, depth + 1)); }
                reader.endObject(); yield object;
            }
            case BEGIN_ARRAY -> { JsonArray array = new JsonArray(); reader.beginArray(); while (reader.hasNext()) array.add(read(reader, depth + 1)); reader.endArray(); yield array; }
            case STRING -> new JsonPrimitive(reader.nextString());
            case NUMBER -> new JsonPrimitive(new BigDecimal(reader.nextString()));
            case BOOLEAN -> new JsonPrimitive(reader.nextBoolean());
            case NULL -> { reader.nextNull(); yield JsonNull.INSTANCE; }
            default -> throw new IllegalArgumentException("Invalid JSON value");
        };
    }
    public static JsonElement canonical(JsonElement element) {
        if (element.isJsonObject()) {
            var sorted = new JsonObject(); var object = element.getAsJsonObject();
            object.keySet().stream().sorted().forEach(key -> sorted.add(key, canonical(object.get(key)))); return sorted;
        }
        if (element.isJsonArray()) { var array = new JsonArray(); element.getAsJsonArray().forEach(item -> array.add(canonical(item))); return array; }
        return element;
    }
    public static String hash(Object object) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical(GSON.toJsonTree(object)).toString().getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    public static void keys(JsonObject object, String... allowed) {
        var names = Set.of(allowed);
        for (String key : object.keySet()) if (!names.contains(key)) throw new IllegalArgumentException("Unsupported field: " + key);
    }
    public static double number(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Expected number: " + key);
        double result = value.getAsDouble(); if (!Double.isFinite(result)) throw new IllegalArgumentException("Non-finite number: " + key); return result;
    }
    public static String text(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString() || value.getAsString().isBlank()) throw new IllegalArgumentException("Expected text: " + key);
        return value.getAsString();
    }
}
