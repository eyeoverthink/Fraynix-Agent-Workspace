package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** An immutable teaching example with limits derived from its checked outcome. */
final class BlenderRepairLessons {
    private static final String KIND = "blender-gap-repair-lesson-v1";
    private static final int LIMIT = 64;
    static final String NAME_PATTERN = "[a-z0-9][a-z0-9-]{0,39}";
    private final Path root;

    BlenderRepairLessons(Path project) throws Exception {
        root = project.resolve("blender-repair-lessons");
        Files.createDirectories(root);
    }

    JSONObject save(String name, JSONObject teacher) throws Exception {
        name = name.strip().toLowerCase(Locale.ROOT);
        var file = path(name);
        var record = example(name, teacher);
        if (Files.exists(file)) {
            var existing = read(name);
            require(existing.similar(record), "That lesson name is already used. Choose a new name.");
            return existing;
        }
        try (var files = Files.list(root)) {
            require(files.filter(BlenderRepairLessons::recordFile).count() < LIMIT, "This project's Blender lesson bank is full");
        }
        AgentFiles042.saveChecked(file, record);
        return record;
    }

    JSONArray list() throws Exception {
        var rows = new JSONArray();
        try (var files = Files.list(root)) {
            for (var file : files.filter(BlenderRepairLessons::recordFile).sorted().toList()) {
                String name = file.getFileName().toString().replaceFirst("\\.json$", "");
                try {
                    rows.put(read(name));
                } catch (Exception damaged) {
                    rows.put(new JSONObject().put("name", name).put("error", "Stored lesson could not be read."));
                }
            }
        }
        return rows;
    }

    JSONObject read(String name) throws Exception {
        var file = path(name);
        require(Files.isRegularFile(file), "No Blender repair lesson named " + name);
        var record = AgentFiles042.loadChecked(file);
        require(KIND.equals(record.getString("kind")) && name.equals(record.getString("name")), "Blender lesson identity changed");
        return record;
    }

    static JSONObject example(String name, JSONObject teacher) {
        require(!teacher.has("lesson"), "Teach from an independently verified original repair");
        var display = teacher.optJSONObject("display");
        require(display != null && display.optBoolean("passed"), "Teaching requires the original browser-checked repair report");
        var audit = teacher.getJSONObject("audit");
        var plan = audit.getJSONObject("diagnosis");
        var checked = audit.getJSONObject("parent");
        return new JSONObject().put("kind", KIND).put("name", name).put("teacherTaskId", teacher.getString("id"))
                .put("teacherReport", teacher.getJSONObject("report"))
                .put("teacherAuditSha256", WorldFrames.hash(audit)).put("teacherBrowserSha256", WorldFrames.hash(display))
                .put("levelSha256", checked.getString("levelSha256")).put("courseManifest", checked.getString("manifestSha256"))
                .put("diagnosis", plan.getString("kind")).put("maxEdits", plan.getJSONArray("edits").length())
                .put("maxOriginalSpan", plan.getDouble("originalSpan")).put("maxRemainingSpan", plan.getDouble("remainingSpan"));
    }

    static void checkTarget(JSONObject lesson, JSONObject target) {
        require(lesson.getString("courseManifest").equals(target.getString("manifestSha256")),
                "This lesson was verified for a different course layout");
        require(!lesson.getString("levelSha256").equals(target.getString("levelSha256")),
                "Choose a different level from the teaching example");
    }

    static void checkPlan(JSONObject lesson, JSONObject plan) {
        require(lesson.getString("diagnosis").equals(plan.getString("kind"))
                && plan.getJSONArray("edits").length() <= lesson.getInt("maxEdits")
                && plan.getDouble("originalSpan") <= lesson.getDouble("maxOriginalSpan")
                && plan.getDouble("remainingSpan") <= lesson.getDouble("maxRemainingSpan"),
                "The new failure exceeds the conditions verified by this lesson");
    }

    private Path path(String name) {
        if (!name.matches(NAME_PATTERN)) throw new IllegalArgumentException("Use a short lesson name with letters, digits or hyphens");
        return root.resolve(name + ".json");
    }

    private static boolean recordFile(Path path) {
        return path.getFileName().toString().endsWith(".json");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
