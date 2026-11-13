package fraymus.workspace;

import org.json.*;

/** Copies only parsed JSON trees. Mutable or custom values must never alias a transaction. */
final class ParsedJson137 {
    static JSONObject copy(JSONObject source) {
        var result = new JSONObject();
        for (String key : source.keySet()) result.put(key, value(source.get(key)));
        return result;
    }
    private static Object value(Object source) {
        if (source instanceof JSONObject object) return copy(object);
        if (source instanceof JSONArray array) {
            var result = new JSONArray();
            for (Object item : array) result.put(value(item));
            return result;
        }
        if (source == JSONObject.NULL || source instanceof String || source instanceof Boolean
            || source instanceof Integer || source instanceof Long || source instanceof Double
            || source instanceof Float || source instanceof Short || source instanceof Byte
            || source instanceof java.math.BigDecimal || source instanceof java.math.BigInteger) return source;
        throw new IllegalArgumentException("Transaction copy requires parsed immutable JSON scalars");
    }
}
