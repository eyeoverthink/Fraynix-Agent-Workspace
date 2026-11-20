package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

/** Binds a local CSV export to its current Java proof and its independent browser dashboard. */
final class RoutedDashboard {
    static final String PREVIEW = "/routed-dashboard-preview/";
    private final Path directory;
    private final ClassifiedProcedurePlan routing;
    private final WorkspaceStore store;

    RoutedDashboard(Path project, ClassifiedProcedurePlan routing, WorkspaceStore store) {
        directory = project.resolve("request-workbench").resolve("routed-dashboards");
        this.routing = routing;
        this.store = store;
    }

    synchronized JSONObject prepare(JSONObject input) throws Exception {
        SpatialScene.keys(input, "file", "baseline", "verifyOnly");
        var file = input.getJSONObject("file");
        var baseline = input.optJSONObject("baseline");
        var report = checkedReport(file, baseline);
        String id = baseline == null ? requestId(file.getString("exportId")) : UUID.nameUUIDFromBytes(
                ("routed-comparison:" + baseline.getString("exportId") + ":" + file.getString("exportId"))
                        .getBytes(StandardCharsets.UTF_8)).toString();
        var path = path(id);
        var task = Files.exists(path, LinkOption.NOFOLLOW_LINKS) ? load(id) : null;
        if (task != null) requireFiles(task, file, baseline);
        if (task != null && task.has("artifact")) {
            var saved = proof(task, report);
            return input.optBoolean("verifyOnly") ? saved.put("steps", report.steps()) : saved;
        }
        if (input.optBoolean("verifyOnly")) throw new IllegalStateException(baseline == null
                ? "Build this routed CSV dashboard before rechecking it" : "Build this routed CSV comparison before rechecking it");
        if (task == null) {
            var initial = new JSONObject().put("id", id).put("file", file);
            if (baseline != null) initial.put("baseline", baseline);
            save(initial);
        }
        var output = new JSONObject().put("id", id).put("htmlSha256", report.htmlSha256());
        if (task != null && task.has("checks")) return output.put("savePending", true);
        return output.put("preview", PREVIEW + id + "/" + report.htmlSha256() + "/index.html")
                .put("steps", report.steps());
    }

    synchronized JSONObject finish(JSONObject input) throws Exception {
        SpatialScene.keys(input, "id", "file", "baseline", "htmlSha256", "checks");
        var task = load(input.getString("id"));
        var file = input.getJSONObject("file");
        var baseline = input.optJSONObject("baseline");
        requireFiles(task, file, baseline);
        var report = checkedReport(file, baseline);
        if (!report.htmlSha256().equals(input.getString("htmlSha256")))
            throw new IllegalStateException("The reviewed dashboard changed");
        var supplied = input.optJSONArray("checks");
        if (task.has("artifact")) {
            var saved = proof(task, report);
            if (supplied != null) {
                report.verifyBrowser(supplied);
                saved.put("rechecked", true);
            }
            return saved;
        }
        var checks = task.optJSONArray("checks");
        if (checks == null) checks = supplied;
        if (checks == null || supplied != null && !checks.similar(supplied))
            throw new IllegalStateException("The original dashboard browser evidence is required");
        var expected = report.artifact(file, checks);
        if (!task.has("checks")) {
            task.put("checks", checks).put("saveSha256", WorldFrames.hash(expected));
            save(task);
        }
        if (!WorldFrames.hash(expected).equals(task.getString("saveSha256")))
            throw new IllegalStateException("The staged dashboard changed");
        var artifact = store.writeOnce("routed-dashboard:" + task.getString("id"), expected);
        var ref = reference(artifact, expected);
        store.recordTestOnce(test(ref, report, checks));
        task.put("artifact", ref);
        save(task);
        return proof(task, report);
    }

    synchronized byte[] preview(String path) throws Exception {
        var parts = path.substring(PREVIEW.length()).split("/", -1);
        if (parts.length != 3 || !parts[1].matches("[a-f0-9]{64}") || !parts[2].equals("index.html")) return null;
        var task = load(parts[0]);
        if (task.has("artifact") || task.has("checks")) return null;
        var report = checkedReport(task.getJSONObject("file"), task.optJSONObject("baseline"));
        return report.htmlSha256().equals(parts[1]) ? report.html().getBytes(StandardCharsets.UTF_8) : null;
    }

    private RoutedReport checkedReport(JSONObject file, JSONObject baseline) throws Exception {
        String current = checkedCsv(file);
        if (baseline == null) return new RoutedDashboardReport(current);
        String earlier = checkedCsv(baseline);
        if (baseline.getJSONObject("source").getString("planId").equals(file.getJSONObject("source").getString("planId")))
            throw new IllegalArgumentException("Compare exports from two different verified routing plans");
        return new RoutedComparisonReport(baseline, earlier, file, current);
    }

    private String checkedCsv(JSONObject file) throws Exception {
        SpatialScene.keys(file, "exportId", "sha256", "source", "base64");
        requestId(file.getString("exportId"));
        String encoded = file.getString("base64");
        if (encoded.length() > 16_000) throw new IllegalArgumentException("The routed CSV exceeds the dashboard size limit");
        var bytes = Base64.getDecoder().decode(encoded);
        if (!PerceptionStore.hash(bytes).equals(file.getString("sha256")))
            throw new IllegalStateException("The exported routing CSV bytes changed");
        var bound = file.getJSONObject("source");
        var actual = routing.export(new JSONObject().put("id", bound.getString("planId")));
        String csv = (String)actual.remove("csv");
        String hash = (String)actual.remove("sha256");
        if (!actual.put("reportSha256", hash).similar(bound) || !Arrays.equals(bytes, csv.getBytes(StandardCharsets.UTF_8)))
            throw new IllegalStateException("The exported CSV or receipt disagrees with the verified routing plan");
        return csv;
    }

    private JSONObject proof(JSONObject task, RoutedReport report) throws Exception {
        var checks = task.getJSONArray("checks");
        var expected = report.artifact(task.getJSONObject("file"), checks);
        var original = task.getJSONObject("artifact");
        var ref = reference(store.read(original.getString("id")), expected);
        if (!ref.similar(original) || !WorldFrames.hash(expected).equals(task.getString("saveSha256")))
            throw new IllegalStateException("The retained dashboard proof changed");
        store.verifyRecordedTest(test(ref, report, checks));
        var result = new JSONObject().put("id", task.getString("id")).put("artifact", ref).put("passed", true)
                .put("browserChecks", checks.length()).put("csvSha256", task.getJSONObject("file").getString("sha256"))
                .put("htmlSha256", report.htmlSha256());
        var baseline = task.optJSONObject("baseline");
        if (baseline != null) result.put("earlierCsvSha256", baseline.getString("sha256"))
                .put("earlierExportId", baseline.getString("exportId"))
                .put("laterExportId", task.getJSONObject("file").getString("exportId"));
        return result;
    }

    private static JSONObject reference(JSONObject artifact, JSONObject expected) throws Exception {
        var contents = artifact.getJSONObject("contents");
        if (artifact.getInt("version") != 1 || !contents.similar(expected.getJSONObject("files"))
                || !artifact.getJSONArray("sources").similar(expected.getJSONArray("sources")))
            throw new IllegalStateException("The checked dashboard files changed");
        return new JSONObject().put("id", artifact.getString("id")).put("version", artifact.getInt("version"))
                .put("sha256", Creation.digest(contents));
    }

    private static JSONObject test(JSONObject ref, RoutedReport report, JSONArray checks) {
        return new JSONObject().put("id", ref.getString("id")).put("version", ref.getInt("version"))
                .put("steps", report.steps()).put("checks", checks);
    }

    private static void requireFiles(JSONObject task, JSONObject file, JSONObject baseline) {
        var earlier = task.optJSONObject("baseline");
        boolean sameBaseline = earlier == null ? baseline == null : earlier.similar(baseline);
        if (!task.getJSONObject("file").similar(file) || !sameBaseline)
            throw new IllegalStateException("The reviewed CSV export or receipt changed");
    }

    private static String requestId(String exportId) {
        if (!UUID.fromString(exportId).toString().equals(exportId)) throw new IllegalArgumentException("Expected a local export ID");
        return UUID.nameUUIDFromBytes(("routed-dashboard:" + exportId).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private Path path(String id) {
        if (!UUID.fromString(id).toString().equals(id)) throw new IllegalArgumentException("Expected a dashboard ID");
        return directory.resolve(id + ".json");
    }

    private JSONObject load(String id) throws Exception {
        var path = path(id);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw new IllegalArgumentException("Unknown dashboard");
        var task = AgentFiles042.loadChecked(path);
        if (!id.equals(task.getString("id"))) throw new IllegalStateException("Dashboard identity changed");
        return task;
    }

    private void save(JSONObject task) throws Exception { AgentFiles042.saveChecked(path(task.getString("id")), task); }
}
