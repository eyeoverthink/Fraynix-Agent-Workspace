package fraymus.workspace;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/** Local review, exact source binding and verified export for the request workbench. */
final class RequestWorkbench {
    interface Planner { JSONObject plan(JSONObject input) throws Exception; }
    private static final int MAX_BYTES = 65536;
    private final Path directory;
    private final WorkspaceStore store;

    RequestWorkbench(Path project, WorkspaceStore store) {
        this.directory = project.resolve("request-workbench");
        this.store = store;
    }

    JSONObject suggest(JSONObject input, Planner planner) throws Exception {
        SpatialScene.keys(input, "request", "model", "useModel");
        String text = RequestLanguage.request(input);
        String key = RequestLanguage.key(text);
        String model = input.optString("model");
        var known = knownPhrase(key);
        var direct = RequestLanguage.direct(text);
        JSONObject choice;
        if (known != null) choice = RequestLanguage.view(RequestLanguage.Intent.read(known.getString("action")), "remembered");
        else if (direct != null) choice = RequestLanguage.view(direct, "java");
        else if (!model.isBlank() && input.optBoolean("useModel"))
            choice = RequestLanguage.suggested(planner.plan(RequestLanguage.modelRequest(text, model)));
        else choice = RequestLanguage.view(RequestLanguage.Intent.CLARIFY, "java");
        return retain(text, choice);
    }

    synchronized JSONObject chat(JSONObject input) throws Exception {
        SpatialScene.keys(input, "request");
        String text = RequestLanguage.request(input);
        if (RequestLanguage.direct(text) != RequestLanguage.Intent.FRAY_INCH)
            return new JSONObject().put("matched", false);
        var known = knownPhrase(RequestLanguage.key(text));
        var choice = RequestLanguage.view(RequestLanguage.Intent.FRAY_INCH,
                known != null && known.getString("action").equals(RequestLanguage.Intent.FRAY_INCH.id)
                        ? "remembered" : "java");
        return new JSONObject().put("matched", true).put("task", retain(text, choice));
    }

    synchronized JSONObject choose(JSONObject input) throws Exception {
        SpatialScene.keys(input, "id", "action");
        var task = load(input);
        if (!task.getString("status").equals("review")) throw new IllegalStateException("Start a new review to change a verified action");
        var intent = RequestLanguage.Intent.read(input.getString("action"));
        if (intent == RequestLanguage.Intent.CLARIFY) throw new IllegalArgumentException("Choose a supported action");
        if (!intent(task).equals(intent)) {
            task.remove("source");
            task.put("choice", RequestLanguage.view(intent, "user"));
            save(task);
        }
        return view(task);
    }

    private synchronized JSONObject retain(String text, JSONObject choice) throws Exception {
        ensureDirectory();
        try (var files = Files.list(directory)) {
            if (files.filter(path -> path.getFileName().toString().endsWith(".task.json")).count() >= 128)
                throw new IllegalStateException("Request workbench history has reached 128 saved reviews");
        }
        String id = UUID.randomUUID().toString();
        var task = new JSONObject().put("id", id).put("request", text).put("choice", choice).put("status", "review");
        save(task);
        return view(task);
    }

    synchronized JSONObject attach(JSONObject input) throws Exception {
        SpatialScene.keys(input, "id", "name", "base64");
        var task = load(input);
        if (!task.getString("status").equals("review") || !intent(task).needsFile())
            throw new IllegalStateException("Choose a file for an outstanding file review");
        String name = input.getString("name").strip();
        if (name.isEmpty() || name.length() > 180 || name.indexOf('/') >= 0 || name.indexOf('\\') >= 0
                || name.chars().anyMatch(Character::isISOControl)) throw new IllegalArgumentException("Expected a filename");
        String encoded = input.getString("base64");
        byte[] data = decode(encoded);
        var source = new JSONObject().put("name", name).put("base64", encoded)
                .put("size", data.length).put("sha256", PerceptionStore.hash(data));
        task.put("source", source);
        save(task);
        return view(task);
    }

    synchronized JSONObject read(JSONObject input) throws Exception {
        SpatialScene.keys(input, "id");
        var task = load(input);
        if (task.has("report")) proof(task);
        return view(task);
    }

    synchronized JSONObject verify(JSONObject input) throws Exception {
        SpatialScene.keys(input, "id");
        return proof(load(input));
    }

    synchronized JSONObject execute(JSONObject input) throws Exception {
        SpatialScene.keys(input, "id", "reviewSha256");
        var task = load(input);
        requireReview(input, task);
        if (!intent(task).needsFile() || !task.has("source")) throw new IllegalStateException("Select a source file");
        if (task.has("report")) return proof(task).put("task", view(task));

        var output = converted(task);
        var artifact = store.writeOnce("request-workbench:" + task.getString("id"), files(task, output));
        var ref = artifactRef(artifact, files(task, output));
        task.put("report", ref).put("status", "verified").put("summary", output.note());
        save(task);
        return proof(task).put("task", view(task));
    }

    synchronized JSONObject prepareFray(JSONObject input) throws Exception {
        SpatialScene.keys(input, "id", "reviewSha256");
        var task = load(input);
        requireFray(input, task);
        if (task.has("report")) return proof(task).put("task", view(task));
        return new JSONObject().put("preview", FrayConversion.previewPath())
                .put("htmlSha256", FrayConversion.HTML_SHA256).put("steps", FrayConversion.steps());
    }

    synchronized JSONObject finishFray(JSONObject input) throws Exception {
        SpatialScene.keys(input, "id", "reviewSha256", "htmlSha256", "checks");
        var task = load(input);
        requireFray(input, task);
        if (!FrayConversion.HTML_SHA256.equals(input.getString("htmlSha256")))
            throw new IllegalStateException("The reviewed browser program changed");
        if (task.has("report")) return proof(task).put("task", view(task));
        var checks = input.getJSONArray("checks");
        var expected = FrayConversion.artifact(checks);
        var artifact = store.writeOnce("request-workbench:" + task.getString("id"), expected);
        var ref = artifactRef(artifact, expected);
        store.recordTest(new JSONObject().put("id", ref.getString("id")).put("version", ref.getInt("version"))
                .put("steps", FrayConversion.steps()).put("checks", checks));
        task.put("report", ref).put("status", "verified").put("frayChecks", checks)
                .put("summary", "Browser results matched the Fray reference. Open or export the offline tool.");
        save(task);
        return proof(task).put("task", view(task));
    }

    private static void requireFray(JSONObject input, JSONObject task) {
        requireReview(input, task);
        if (intent(task) != RequestLanguage.Intent.FRAY_INCH)
            throw new IllegalStateException("Review the inches-to-millimetres tool first");
    }

    private static void requireReview(JSONObject input, JSONObject task) {
        if (!reviewSha(task).equals(input.getString("reviewSha256")))
            throw new IllegalStateException("The reviewed action or attached source changed. Review it again.");
    }

    synchronized JSONObject brief(JSONObject input) throws Exception {
        SpatialScene.keys(input, "id", "reviewSha256");
        var task = load(input);
        if (!reviewSha(task).equals(input.getString("reviewSha256"))
                || !List.of(RequestLanguage.Intent.APP, RequestLanguage.Intent.TOOL).contains(intent(task)))
            throw new IllegalStateException("Review a creation request before continuing to Creation");
        return new JSONObject().put("request", task.getString("request")).put("action", intent(task).id);
    }

    synchronized JSONObject learn(JSONObject input) throws Exception {
        SpatialScene.keys(input, "id");
        var task = load(input);
        proof(task);
        String key = RequestLanguage.key(task.getString("request"));
        if (key.isBlank()) throw new IllegalArgumentException("Use words for a remembered phrase");
        var bank = phrases();
        if (bank.length() >= 128 && !bank.has(key)) throw new IllegalStateException("Phrase memory has reached 128 entries");
        bank.put(key, new JSONObject().put("action", intent(task).id).put("verifiedReview", task.getString("id")));
        AgentFiles042.saveChecked(directory.resolve("phrases.json"), bank);
        return new JSONObject().put("remembered", true).put("request", task.getString("request"));
    }

    private synchronized JSONObject phrases() throws Exception {
        ensureDirectory();
        Path path = directory.resolve("phrases.json");
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return new JSONObject();
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw new IllegalStateException("Phrase history is unavailable");
        return AgentFiles042.loadChecked(path);
    }

    private synchronized JSONObject knownPhrase(String key) throws Exception {
        var known = phrases().optJSONObject(key);
        if (known == null) return null;
        var original = load(new JSONObject().put("id", known.getString("verifiedReview")));
        if (!intent(original).id.equals(known.getString("action"))
                || !RequestLanguage.key(original.getString("request")).equals(key))
            throw new IllegalStateException("Remembered wording differs from its verified review");
        proof(original);
        return known;
    }

    private JSONObject proof(JSONObject task) throws Exception {
        var ref = task.getJSONObject("report");
        JSONObject expected;
        String name;
        if (intent(task) == RequestLanguage.Intent.FRAY_INCH) {
            expected = FrayConversion.artifact(task.getJSONArray("frayChecks"));
            name = "index.html";
        } else {
            var output = converted(task);
            expected = files(task, output);
            name = output.name();
        }
        var actual = artifactRef(store.read(ref.getString("id")), expected);
        if (!actual.similar(ref)) throw new IllegalStateException("The checked output revision changed");
        return new JSONObject().put("passed", true).put("artifact", actual).put("output", name)
                .put("summary", task.getString("summary"));
    }

    private static JSONObject artifactRef(JSONObject artifact, JSONObject expected) throws Exception {
        var contents = artifact.getJSONObject("contents");
        if (artifact.getInt("version") != 1 || !contents.similar(expected.getJSONObject("files"))
                || !artifact.getJSONArray("sources").similar(expected.getJSONArray("sources")))
            throw new IllegalStateException("The checked file output changed");
        return new JSONObject().put("id", artifact.getString("id")).put("version", artifact.getInt("version"))
                .put("sha256", Creation.digest(contents));
    }

    private static FileRepresentation.Output converted(JSONObject task) throws Exception {
        var source = task.getJSONObject("source");
        byte[] bytes = decode(source.getString("base64"));
        if (bytes.length != source.getInt("size") || !PerceptionStore.hash(bytes).equals(source.getString("sha256")))
            throw new IllegalStateException("The attached source bytes changed");
        return FileRepresentation.convert(intent(task), bytes);
    }

    private static JSONObject files(JSONObject task, FileRepresentation.Output output) {
        var source = task.getJSONObject("source");
        String title = intent(task).label;
        String scope = intent(task) == RequestLanguage.Intent.INSPECT
                ? "A signature is a format hint. The report reads bytes and computes the checksum locally."
                : "The exported file preserves table cells and column order. Java checked the inverse conversion.";
        String html = """
                <!doctype html><html lang="en"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width,initial-scale=1"><title>%s</title>
                <style>body{background:#111c26;color:#e7f3f4;font:16px/1.5 system-ui;margin:auto;padding:clamp(18px,5vw,48px);max-width:62rem}
                h1{line-height:1.1;color:#b9f8ea}a{color:#8fe5dc}code,pre{overflow-wrap:anywhere}pre{background:#233341;
                border-radius:.6rem;padding:1rem;white-space:pre-wrap;max-height:22rem;overflow:auto}</style>
                </head><body><main><h1>%s</h1><p id="summary">%s</p><p>%s</p>
                <p>Source <strong id="source">%s</strong><br>SHA-256 <code id="sha256">%s</code></p>
                <p><a id="output" href="%s" download>Download %s</a></p>
                <pre aria-label="Output preview">%s</pre></main></body></html>
                """.formatted(title, title, output.note(), scope, SpatialReport.escape(source.getString("name")),
                source.getString("sha256"), output.name(), output.name(),
                SpatialReport.escape(preview(output)));
        return new JSONObject().put("title", title).put("files", new JSONObject().put("index.html", html)
                .put(output.name(), output.data())).put("sources", List.of("sha256:" + source.getString("sha256")));
    }

    private static String preview(FileRepresentation.Output output) {
        if (output.name().equals("inspection.json")) {
            var text = new StringBuilder("OFFSET     HEX  DEC  BITS\n");
            for (Object row : new JSONObject(output.data()).getJSONArray("preview")) {
                var byteValue = (JSONObject) row;
                text.append(String.format(java.util.Locale.ROOT, "%08x   %s  %3d  %s%n",
                        byteValue.getInt("offset"), byteValue.getString("hex"), byteValue.getInt("decimal"),
                        byteValue.getString("bits")));
            }
            return text.toString().replace("\r\n", "\n");
        }
        var data = output.data();
        return data.substring(0, data.offsetByCodePoints(0, Math.min(1200, data.codePointCount(0, data.length()))));
    }

    private static RequestLanguage.Intent intent(JSONObject task) {
        return RequestLanguage.Intent.read(task.getJSONObject("choice").getString("action"));
    }

    private static byte[] decode(String encoded) {
        if (encoded.length() > (MAX_BYTES + 2) / 3 * 4) throw new IllegalArgumentException("Choose a file up to 64 KiB");
        byte[] bytes = Base64.getDecoder().decode(encoded);
        if (bytes.length > MAX_BYTES) throw new IllegalArgumentException("Choose a file up to 64 KiB");
        return bytes;
    }

    private static String reviewSha(JSONObject task) {
        var binding = new JSONObject().put("id", task.getString("id")).put("request", task.getString("request"))
                .put("choice", task.getJSONObject("choice"));
        if (task.has("source")) binding.put("source", sourceView(task));
        return WorldFrames.hash(binding);
    }

    private static JSONObject sourceView(JSONObject task) {
        var source = WorldFrames.copy(task.getJSONObject("source"));
        source.remove("base64");
        return source;
    }

    private static JSONObject view(JSONObject task) {
        var visible = WorldFrames.copy(task);
        visible.remove("frayChecks");
        if (task.has("source")) visible.put("source", sourceView(task));
        return visible.put("reviewSha256", reviewSha(task));
    }

    private void ensureDirectory() throws Exception {
        Files.createDirectories(directory);
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS))
            throw new IllegalStateException("Request workbench storage must be a local directory");
    }

    private Path path(String id) { return directory.resolve(UUID.fromString(id).toString() + ".task.json"); }
    private JSONObject load(JSONObject input) throws Exception {
        ensureDirectory();
        String id = input.getString("id");
        Path file = path(id);
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) throw new IllegalArgumentException("Unknown request review");
        var task = AgentFiles042.loadChecked(file);
        if (!id.equals(task.getString("id"))) throw new IllegalStateException("Saved review identity changed");
        return task;
    }
    private void save(JSONObject task) throws Exception { AgentFiles042.saveChecked(path(task.getString("id")), task); }
}
