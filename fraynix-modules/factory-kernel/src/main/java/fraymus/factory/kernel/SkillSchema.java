package fraymus.factory.kernel;

import java.util.*;

/** Versioned flat wire contracts. Strings carry portable values; validation enforces their declared types. */
public record SkillSchema(Map<String, Field> fields) {
    public enum Kind { TEXT, INTEGER, BOOLEAN, SHA256 }
    public record Field(Kind kind, boolean required, int maxLength) {
        public Field { Objects.requireNonNull(kind); if (maxLength < 1) throw new IllegalArgumentException("Invalid field limit"); }
    }
    public SkillSchema { fields = Map.copyOf(fields); }
    public Map<String, String> validate(Map<String, String> values) {
        var copy = Map.copyOf(values);
        for (var field : fields.entrySet()) if (field.getValue().required() && !copy.containsKey(field.getKey()))
            throw new IllegalArgumentException("Missing field: " + field.getKey());
        for (var value : copy.entrySet()) {
            Field field = fields.get(value.getKey());
            if (field == null) throw new IllegalArgumentException("Unknown field: " + value.getKey());
            if (value.getValue().length() > field.maxLength()) throw new IllegalArgumentException("Field too long: " + value.getKey());
            switch (field.kind()) {
                case INTEGER -> { if (!value.getValue().matches("0|[1-9][0-9]*")) throw new IllegalArgumentException("Expected nonnegative integer: " + value.getKey()); Long.parseLong(value.getValue()); }
                case BOOLEAN -> { if (!Set.of("true", "false").contains(value.getValue())) throw new IllegalArgumentException("Expected boolean: " + value.getKey()); }
                case SHA256 -> { if (!value.getValue().matches("[a-f0-9]{64}")) throw new IllegalArgumentException("Expected lowercase SHA-256: " + value.getKey()); }
                case TEXT -> { }
            }
        }
        return copy;
    }
    public static Field text(int limit) { return new Field(Kind.TEXT, true, limit); }
    public static Field integer() { return new Field(Kind.INTEGER, true, 20); }
    public static Field bool() { return new Field(Kind.BOOLEAN, true, 5); }
    public static Field hash() { return new Field(Kind.SHA256, true, 64); }
}
