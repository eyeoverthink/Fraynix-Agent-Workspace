
package fraymus.workspace;

import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Locale;

/** Immutable procedure names tied to the original, browser-checked teaching examples. */
final class ComparisonExplorerLessons {
    private static final String FORMAT = "fraynix.comparison-explorer-lesson-v1";
    private final Path directory;

    ComparisonExplorerLessons(Path workbench) {
        directory = workbench.resolve("comparison-explorer-lessons");
    }

    static String name(String input) {
        String name = input.strip().toLowerCase(Locale.ROOT);
        if (!name.matches("[a-z][a-z0-9-]{2,55}"))
            throw new IllegalArgumentException("Use a lesson name with 3–56 letters, digits or hyphens starting with a letter");
        return name;
    }

    JSONObject save(JSONObject example) throws Exception {
        String name = example.getString("name");
        Path file = directory.resolve(name(name) + ".json");
        if (Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
            if (!read(name).similar(example))
                throw new IllegalStateException("That explorer lesson name is already used. Choose a new name.");
        } else {
            AgentFiles042.saveChecked(file, example);
        }
        return example;
    }

    JSONObject read(String name) throws Exception {
        Path file = directory.resolve(name(name) + ".json");
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS))
            throw new IllegalArgumentException("No comparison explorer lesson named " + name);
        var record = AgentFiles042.loadChecked(file);
        if (!name.equals(record.getString("name")) || !FORMAT.equals(record.getString("format")))
            throw new IllegalStateException("The comparison explorer lesson identity changed");
        return record;
    }

    static JSONObject example(String name, JSONObject verified, JSONObject receipt, String browserSha256) {
        return new JSONObject().put("format", FORMAT).put("name", name).put("version", 1)
                .put("teacherTaskId", verified.getString("id")).put("teacherArtifact", verified.getJSONObject("artifact"))
                .put("teacherExportId", receipt.getString("id")).put("teacherCsvSha256", receipt.getString("sha256"))
                .put("teacherBrowserEvidenceSha256", browserSha256);
    }

    static void requireFresh(JSONObject lesson, JSONObject receipt) {
        if (lesson.getString("teacherCsvSha256").equals(receipt.getString("sha256")))
            throw new IllegalArgumentException("Use a different comparison CSV from the teaching example");
    }
}
