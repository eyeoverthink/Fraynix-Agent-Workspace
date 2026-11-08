package fraymus.workspace;

import org.json.*;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import static org.junit.Assert.*;

/** Test-only source chain. Does not authorize runtime proofs or change historical outcomes. */
final class WorldRegression096 {
    interface Reader { String read(String path) throws IOException; }
    static void verify(Path repo) throws IOException { verify(p -> Files.readString(repo.resolve(p))); }
    static String hash(String text) { return MeasurementStore.hash(text.replace("\r\n", "\n")); }
    static void verify(Reader read) throws IOException {
        var extension = new JSONObject(read.read("docs/WORLD_096_REGRESSION_LOCK.json"));
        assertEquals("historical-world-regression096-v1", extension.getString("version"));
        var inputs = new HashSet<String>();
        for (Object raw : extension.getJSONArray("lockedInputs")) {
            var input = (JSONObject) raw;
            String path = input.getString("path");
            assertTrue("Duplicate locked input: " + path, inputs.add(path));
            assertEquals(path, input.getString("sha256"), hash(read.read(path)));
        }
        assertEquals(Set.of("docs/WORLD_035_CANDIDATE_LOCK.json", "docs/WORLD_035_FROZEN_CONTRACT.json",
            "docs/WORLD_056_REGRESSION_LOCK.json", "workspace-engine/src/main/resources/world-sequences035.json"), inputs);
        var candidate = new JSONObject(read.read("docs/WORLD_035_CANDIDATE_LOCK.json"));
        var preserved = new JSONObject(read.read("docs/WORLD_035_FROZEN_CONTRACT.json"));
        assertEquals(preserved.getString("suiteUtf8LfSha256"), hash(read.read("workspace-engine/src/main/resources/world-sequences035.json")));
        var expected = new LinkedHashMap<String, String>();
        for (Object raw : candidate.getJSONArray("sources")) {
            var file = (JSONObject) raw;
            assertNull(expected.put(file.getString("path"), file.getString("sha256")));
        }
        apply(expected, new JSONObject(read.read("docs/WORLD_056_REGRESSION_LOCK.json")).getJSONArray("extensions"),
            "original035Sha256", "current056Sha256", null);
        apply(expected, extension.getJSONArray("extensions"), "previous056Sha256", "current096Sha256",
            Set.of("workspace-engine/src/main/java/fraymus/workspace/WorldLedger.java",
                "workspace-engine/src/main/java/fraymus/workspace/WorldNativeCheckpoint.java"));
        for (var file : expected.entrySet()) assertEquals(file.getKey(), file.getValue(), hash(read.read(file.getKey())));
        for (Object raw : preserved.getJSONArray("preservedSources")) {
            var file = (JSONObject) raw;
            assertEquals(file.getString("path"), file.getString("sha256"), hash(read.read(file.getString("path"))));
        }
    }
    private static void apply(Map<String, String> expected, JSONArray entries, String prior, String next, Set<String> allowed) {
        var seen = new HashSet<String>();
        for (Object raw : entries) {
            var entry = (JSONObject) raw;
            String path = entry.getString("path");
            assertTrue("Duplicate extension: " + path, seen.add(path));
            assertTrue("Unknown extension: " + path, expected.containsKey(path));
            assertEquals("Broken source chain: " + path, expected.get(path), entry.getString(prior));
            assertTrue(entry.getString(next).matches("[0-9a-f]{64}"));
            expected.put(path, entry.getString(next));
        }
        if (allowed != null) assertEquals(allowed, seen);
    }
}
