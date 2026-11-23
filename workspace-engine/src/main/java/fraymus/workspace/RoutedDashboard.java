package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

/** Binds a local CSV export to its current Java proof and its independent browser dashboard. */
final class RoutedDashboard {
    static final String PREVIEW = "/routed-dashboard-preview/";
    private final Path directory;
    private final ClassifiedProcedurePlan routing;
    private final WorkspaceStore store;
    private final ComparisonExplorerLessons lessons;

    private enum Presentation { TOTALS, EXPLORER, STORY }

    RoutedDashboard(Path project, ClassifiedProcedurePlan routing, WorkspaceStore store) {
        var workbench = project.resolve("request-workbench");
        directory = workbench.resolve("routed-dashboards");
        lessons = new ComparisonExplorerLessons(workbench);
        this.routing = routing;
        this.store = store;
    }

    synchronized JSONObject teachExplorer(JSONObject input) throws Exception {
        SpatialScene.keys(input, "id", "name");
        String name = ComparisonExplorerLessons.name(input.getString("name"));
        return lessons.save(teachingExample(load(input.getString("id")), name));
    }

    synchronized JSONObject qualifyExplorer(JSONObject input) throws Exception {
        SpatialScene.keys(input, "name", "receipt");
        var lesson = checkedLesson(ComparisonExplorerLessons.name(input.getString("name")));
        ComparisonExplorerLessons.requireFresh(lesson, input.getJSONObject("receipt"));
        return lesson;
    }

    private JSONObject checkedLesson(String name) throws Exception {
        var lesson = lessons.read(name);
        var original = teachingExample(load(lesson.getString("teacherTaskId")), name);
        if (!lesson.similar(original)) throw new IllegalStateException("The original explorer teaching evidence changed");
        return lesson;
    }

    private JSONObject teachingExample(JSONObject task, String name) throws Exception {
        if (presentation(task) != Presentation.EXPLORER || lessonName(task) != null || !task.has("artifact"))
            throw new IllegalStateException("Teach from a verified original comparison explorer");
        var imported = comparisonImport(task);
        var report = checkedReport(task.getJSONObject("file"), task.getJSONObject("baseline"), keyColumn(task),
                imported, Presentation.EXPLORER, null);
        return ComparisonExplorerLessons.example(name, proof(task, report), imported.getJSONObject("receipt"),
                MeasurementStore.hash(MapCheckpoint.canonical(task.getJSONArray("checks"))));
    }

    synchronized JSONObject reviewBriefing(JSONObject input) throws Exception {
        SpatialScene.keys(input, "file", "baseline", "keyColumn", "comparisonImport", "sections");
        var imported = input.getJSONObject("comparisonImport");
        var receipt = imported.getJSONObject("receipt");
        var sections = ComparisonBriefing.sections(input.getJSONArray("sections"));
        checkedReport(input.getJSONObject("file"), input.getJSONObject("baseline"), keyColumn(input),
                imported, Presentation.STORY, null);
        var summary = load(importedId(imported, Presentation.TOTALS, null)).getJSONObject("artifact");
        var id = UUID.randomUUID().toString();
        var review = new JSONObject().put("id", id).put("sections", sections)
                .put("exportId", receipt.getString("id")).put("sourceSha256", receipt.getString("sha256"))
                .put("summary", summary);
        review.put("reviewSha256", WorldFrames.hash(review)).put("approved", false);
        AgentFiles042.saveChecked(briefingPath(id), review);
        return review;
    }

    synchronized JSONObject readBriefing(JSONObject input, boolean approve) throws Exception {
        if (approve) SpatialScene.keys(input, "id", "reviewSha256");
        else SpatialScene.keys(input, "id");
        String id = input.getString("id");
        var review = loadBriefing(id);
        if (approve) {
            if (!review.getString("reviewSha256").equals(input.getString("reviewSha256")))
                throw new IllegalArgumentException("The briefing review changed");
            if (!review.getBoolean("approved")) {
                review.put("approved", true);
                AgentFiles042.saveChecked(briefingPath(id), review);
            }
        }
        return review;
    }

    private JSONObject loadBriefing(String id) throws Exception {
        var file = briefingPath(id);
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS))
            throw new IllegalArgumentException("Unknown comparison briefing");
        var review = AgentFiles042.loadChecked(file);
        var body = new JSONObject(review.toString());
        String hash = (String)body.remove("reviewSha256");
        body.remove("approved");
        if (!id.equals(review.getString("id")) || !WorldFrames.hash(body).equals(hash))
            throw new IllegalStateException("The briefing review changed");
        return review;
    }

    private Path briefingPath(String id) {
        if (!UUID.fromString(id).toString().equals(id)) throw new IllegalArgumentException("Expected a briefing review ID");
        return directory.resolveSibling("comparison-briefings").resolve(id + ".json");
    }

    private static String briefingId(JSONObject record) {
        return record.has("briefingId") ? record.getString("briefingId") : null;
    }

    private static void requireBriefing(JSONObject task, String briefing) {
        if (!Objects.equals(briefingId(task), briefing))
            throw new IllegalArgumentException("The selected briefing review changed");
    }

    synchronized JSONObject prepare(JSONObject input) throws Exception {
        SpatialScene.keys(input, "file", "baseline", "keyColumn", "comparisonImport", "explorer", "story", "explorerLesson", "briefingId", "verifyOnly");
        var file = input.getJSONObject("file");
        var baseline = input.optJSONObject("baseline");
        String key = keyColumn(input);
        var imported = comparisonImport(input);
        Presentation presentation = presentation(input);
        String lesson = lessonName(input);
        String briefing = briefingId(input);
        var report = checkedReport(file, baseline, key, imported, presentation, lesson, briefing);
        String id = briefing != null
                ? UUID.nameUUIDFromBytes(("comparison-briefing-v1:" + briefing).getBytes(StandardCharsets.UTF_8)).toString()
                : imported == null ? reportId(file, baseline, key) : importedId(imported, presentation, lesson);
        var path = path(id);
        var task = Files.exists(path, LinkOption.NOFOLLOW_LINKS) ? load(id) : null;
        if (task != null) {
            requireSources(task, file, baseline, key, imported, presentation, lesson);
            requireBriefing(task, briefing);
        }
        if (task != null && task.has("artifact")) {
            var saved = proof(task, report);
            return input.optBoolean("verifyOnly") ? saved.put("steps", report.steps()) : saved;
        }
        if (input.optBoolean("verifyOnly")) {
            if (presentation == Presentation.STORY) throw new IllegalStateException("Build this change story before rechecking it");
            if (presentation == Presentation.EXPLORER) throw new IllegalStateException("Build this comparison explorer before rechecking it");
            if (imported != null) throw new IllegalStateException("Summarize this comparison CSV before rechecking it");
            throw new IllegalStateException(baseline == null ? "Build this routed CSV dashboard before rechecking it"
                    : "Build this routed CSV comparison before rechecking it");
        }
        if (task == null) {
            var initial = new JSONObject().put("id", id).put("file", file);
            if (baseline != null) initial.put("baseline", baseline);
            if (key != null) initial.put("keyColumn", key);
            if (imported != null) initial.put("comparisonImport", imported);
            if (presentation == Presentation.EXPLORER) initial.put("explorer", true);
            if (presentation == Presentation.STORY) initial.put("story", true);
            if (lesson != null) initial.put("explorerLesson", lesson);
            if (briefing != null) initial.put("briefingId", briefing);
            save(initial);
        }
        var output = new JSONObject().put("id", id).put("htmlSha256", report.htmlSha256());
        if (task != null && task.has("checks")) return output.put("savePending", true);
        return output.put("preview", PREVIEW + id + "/" + report.htmlSha256() + "/index.html")
                .put("steps", report.steps());
    }

    synchronized JSONObject finish(JSONObject input) throws Exception {
        SpatialScene.keys(input, "id", "file", "baseline", "keyColumn", "comparisonImport", "explorer", "story", "explorerLesson", "briefingId", "htmlSha256", "checks", "exportCsv");
        var task = load(input.getString("id"));
        var file = input.getJSONObject("file");
        var baseline = input.optJSONObject("baseline");
        String key = keyColumn(input);
        var imported = comparisonImport(input);
        Presentation presentation = presentation(input);
        String lesson = lessonName(input);
        requireSources(task, file, baseline, key, imported, presentation, lesson);
        String briefing = briefingId(input);
        requireBriefing(task, briefing);
        var report = checkedReport(file, baseline, key, imported, presentation, lesson, briefing);
        if (!report.htmlSha256().equals(input.getString("htmlSha256")))
            throw new IllegalStateException("The reviewed dashboard changed");
        var supplied = input.optJSONArray("checks");
        boolean exportCsv = input.has("exportCsv") && input.getBoolean("exportCsv");
        if (exportCsv && (!(report instanceof RoutedKeyComparisonReport) || !task.has("artifact") || supplied == null))
            throw new IllegalStateException("Recheck a saved matched comparison before exporting");
        if (task.has("artifact")) {
            var saved = proof(task, report);
            if (supplied != null) {
                report.verifyBrowser(supplied);
                saved.put("rechecked", true);
            }
            if (exportCsv) saved.put("csvExport", ((RoutedKeyComparisonReport)report).exportCsv()
                    .put("browserEvidenceSha256", MeasurementStore.hash(MapCheckpoint.canonical(supplied))));
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
        var report = checkedReport(task.getJSONObject("file"), task.optJSONObject("baseline"),
                keyColumn(task), comparisonImport(task), presentation(task), lessonName(task), briefingId(task));
        return report.htmlSha256().equals(parts[1]) ? report.html().getBytes(StandardCharsets.UTF_8) : null;
    }

    private RoutedReport checkedReport(JSONObject file, JSONObject baseline, String key, JSONObject imported,
                                       Presentation presentation, String lessonName) throws Exception {
        return checkedReport(file, baseline, key, imported, presentation, lessonName, null);
    }

    private RoutedReport checkedReport(JSONObject file, JSONObject baseline, String key, JSONObject imported,
                                       Presentation presentation, String lessonName, String briefingId) throws Exception {
        if (briefingId != null && presentation != Presentation.STORY)
            throw new IllegalArgumentException("Use the reviewed briefing presentation");
        if (lessonName != null && presentation != Presentation.EXPLORER) throw new IllegalArgumentException("Use an explorer lesson with an explorer");
        if (presentation != Presentation.TOTALS && imported == null) throw new IllegalArgumentException(presentation == Presentation.STORY
                    ? "The change story requires a verified comparison summary"
                    : "The explorer requires a verified comparison summary");
        if (imported != null && (baseline == null || key == null))
            throw new IllegalArgumentException("A comparison CSV needs both original exports and the key");
        String current = checkedCsv(file);
        if (baseline == null) {
            if (key != null) throw new IllegalArgumentException("A key requires two verified routing exports");
            return new RoutedDashboardReport(current);
        }
        String earlier = checkedCsv(baseline);
        if (baseline.getJSONObject("source").getString("planId").equals(file.getJSONObject("source").getString("planId")))
            throw new IllegalArgumentException("Compare exports from two different verified routing plans");
        if (key == null) return new RoutedComparisonReport(baseline, earlier, file, current);
        var comparison = new RoutedKeyComparisonReport(baseline, earlier, file, current, key);
        if (imported == null) return comparison;
        var original = load(reportId(file, baseline, key));
        requireSources(original, file, baseline, key, null, Presentation.TOTALS, null);
        var totals = checkedImport(imported, original, comparison);
        if (presentation == Presentation.TOTALS) return totals;
        String id = importedId(imported, Presentation.TOTALS, null);
        if (!Files.exists(path(id), LinkOption.NOFOLLOW_LINKS))
            throw new IllegalStateException("Summarize the comparison CSV before building its explorer");
        var summary = load(id);
        requireSources(summary, file, baseline, key, imported, Presentation.TOTALS, null);
        if (!summary.has("artifact"))
            throw new IllegalStateException("Verify the comparison CSV summary before building its explorer");
        var verified = proof(summary, totals);
        var receipt = imported.getJSONObject("receipt");
        var lesson = lessonName == null ? null : checkedLesson(lessonName);
        if (lesson != null) ComparisonExplorerLessons.requireFresh(lesson, receipt);
        var summaryArtifact = verified.getJSONObject("artifact");
        ComparisonBriefing briefing = null;
        if (briefingId != null) {
            var selection = loadBriefing(briefingId);
            if (!selection.getBoolean("approved"))
                throw new IllegalStateException("Approve the comparison briefing before building it");
            if (!selection.getString("exportId").equals(receipt.getString("id"))
                    || !selection.getString("sourceSha256").equals(receipt.getString("sha256"))
                    || !selection.getJSONObject("summary").similar(summaryArtifact))
                throw new IllegalStateException("The original comparison briefing source changed");
            briefing = new ComparisonBriefing(briefingId, ComparisonBriefing.sections(selection.getJSONArray("sections")));
        }
        return new RoutedComparisonExplorer(totals.csv(), receipt, summaryArtifact,
                totals.totalsProof(verified.getInt("browserChecks")), lesson, presentation == Presentation.STORY, briefing);
    }

    private RoutedComparisonTotals checkedImport(JSONObject imported, JSONObject original,
                                                  RoutedKeyComparisonReport comparison) throws Exception {
        SpatialScene.keys(imported, "receipt", "base64");
        var receipt = imported.getJSONObject("receipt");
        SpatialScene.keys(receipt, "id", "kind", "bytes", "sha256", "createdAt", "source");
        requestId(receipt.getString("id"));
        Instant.parse(receipt.getString("createdAt"));
        if (!"verified-routed-comparison".equals(receipt.getString("kind")) || !original.has("artifact"))
            throw new IllegalArgumentException("Use an exported, verified matched comparison");
        var report = proof(original, comparison);
        var expected = comparison.exportCsv();
        String csv = expected.getString("csv");
        String encoded = imported.getString("base64");
        if (encoded.length() > 349_528) throw new IllegalArgumentException("The comparison CSV exceeds 256 KiB");
        byte[] bytes = Base64.getDecoder().decode(encoded);
        var source = new JSONObject().put("format", expected.getString("format")).put("groups", expected.getInt("groups"))
                .put("comparisonId", report.getString("id")).put("artifact", report.getJSONObject("artifact"))
                .put("keyColumn", report.getString("keyColumn"))
                .put("earlier", exportReference(report, "earlierExportId", "earlierCsvSha256"))
                .put("later", exportReference(report, "laterExportId", "csvSha256"))
                .put("browserChecks", report.getInt("browserChecks"))
                .put("browserEvidenceSha256", MeasurementStore.hash(MapCheckpoint.canonical(original.getJSONArray("checks"))));
        if (!source.similar(receipt.getJSONObject("source")) || bytes.length != receipt.getInt("bytes")
                || !expected.getString("sha256").equals(receipt.getString("sha256"))
                || !Arrays.equals(bytes, csv.getBytes(StandardCharsets.UTF_8)))
            throw new IllegalStateException("The comparison CSV or receipt disagrees with its verified source");
        return new RoutedComparisonTotals(csv, receipt);
    }

    private static JSONObject exportReference(JSONObject report, String id, String hash) {
        return new JSONObject().put("exportId", report.getString(id)).put("sha256", report.getString(hash));
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
        if (task.has("keyColumn")) result.put("keyColumn", task.getString("keyColumn"));
        var imported = comparisonImport(task);
        if (imported != null) result.put("comparisonExportId", imported.getJSONObject("receipt").getString("id"));
        if (lessonName(task) != null) result.put("explorerLesson", lessonName(task));
        if (briefingId(task) != null) result.put("briefingId", briefingId(task));
        else if (presentation(task) == Presentation.STORY) result.put("story", true);
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

    private static String keyColumn(JSONObject record) {
        if (!record.has("keyColumn")) return null;
        String key = record.getString("keyColumn");
        if (key.isBlank()) throw new IllegalArgumentException("Choose an original key column");
        return key;
    }

    private static JSONObject comparisonImport(JSONObject record) {
        return record.has("comparisonImport") ? record.getJSONObject("comparisonImport") : null;
    }

    private static Presentation presentation(JSONObject record) {
        boolean explorer = record.has("explorer") && record.getBoolean("explorer");
        boolean story = record.has("story") && record.getBoolean("story");
        if (story && explorer) throw new IllegalArgumentException("Choose one comparison presentation");
        if (story) return Presentation.STORY;
        return explorer ? Presentation.EXPLORER : Presentation.TOTALS;
    }

    private static String lessonName(JSONObject record) {
        return record.has("explorerLesson") ? ComparisonExplorerLessons.name(record.getString("explorerLesson")) : null;
    }

    private static void requireSources(JSONObject task, JSONObject file, JSONObject baseline, String key,
                                       JSONObject imported, Presentation presentation, String lesson) {
        if (!Objects.equals(lessonName(task), lesson)) throw new IllegalStateException("The selected explorer lesson changed");
        if (presentation(task) != presentation) throw new IllegalStateException("The selected comparison report changed");
        var earlier = task.optJSONObject("baseline");
        boolean sameBaseline = earlier == null ? baseline == null : earlier.similar(baseline);
        if (!task.getJSONObject("file").similar(file) || !sameBaseline)
            throw new IllegalStateException("The reviewed CSV export or receipt changed");
        if (!Objects.equals(keyColumn(task), key))
            throw new IllegalStateException("The reviewed comparison key changed");
        var priorImport = comparisonImport(task);
        if (priorImport == null ? imported != null : !priorImport.similar(imported))
            throw new IllegalStateException("The attached comparison CSV or receipt changed");
    }

    private static String importedId(JSONObject imported, Presentation presentation, String lesson) {
        String export = imported.getJSONObject("receipt").getString("id");
        String prefix = switch (presentation) {
            case STORY -> "routed-comparison-story:v1:";
            case EXPLORER -> "routed-comparison-explorer:";
            case TOTALS -> "routed-comparison-totals:";
        };
        String identity = lesson == null ? prefix + export : prefix + export + ":lesson:" + lesson;
        return UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static String reportId(JSONObject file, JSONObject baseline, String key) {
        if (baseline == null) return requestId(file.getString("exportId"));
        String pair = baseline.getString("exportId") + ":" + file.getString("exportId");
        String identity = key == null ? "routed-comparison:" + pair : "routed-key-comparison:" + pair + ":" + key;
        return UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8)).toString();
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
