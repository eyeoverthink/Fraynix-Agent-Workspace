package fraymus.workspace;

import fraymus.factory.*;
import org.json.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

/** Immutable measurement versions and comparisons; source labels are user supplied. */
public final class MeasurementStore {
    private final SchemaRegistry schemas = new SchemaRegistry();
    private final FactoryWorld world;
    private final WorldCheckpointStore checkpoint;
    public MeasurementStore(Path project) throws Exception {
        for (String type : List.of("workspace.measurement.dataset", "workspace.measurement.version", "workspace.measurement.comparison"))
            schemas.register(new ComponentSchema(type, 1, Map.of("payload", ComponentSchema.Kind.TEXT), Set.of("payload")));
        world = new FactoryWorld(schemas);
        checkpoint = new WorldCheckpointStore(project.resolve("measurements/data.fworld"));
        Files.createDirectories(checkpoint.path().getParent());
        if (Files.exists(checkpoint.path())) world.restore(checkpoint.load(schemas).snapshot());
    }
    private static String type(EntityRecord e) { return e.components().get(0).type(); }
    private static JSONObject payload(EntityRecord e) { return new JSONObject(e.components().get(0).values().get("payload")); }
    private EntityRecord find(String id) {
        UUID uuid = UUID.fromString(id);
        return world.snapshot().entities().stream().filter(e -> e.id().equals(uuid)).findFirst().orElse(null);
    }
    private JSONObject require(String id, String type) {
        var e = find(id);
        if (e == null || !type(e).equals(type)) throw new IllegalArgumentException("Unknown " + type);
        return payload(e);
    }
    private EntityRecord record(String type, JSONObject value) {
        var prior = find(value.getString("id"));
        return new EntityRecord(UUID.fromString(value.getString("id")), value.optString("title", type), List.of(
            new ComponentRecord(prior == null ? UUID.randomUUID() : prior.components().get(0).id(), type, 1, Map.of("payload", value.toString()))));
    }
    private void commit(EntityRecord... records) throws Exception {
        var before = world.snapshot(); var tx = new WorldTransaction(before);
        for (var e : records) if (find(e.id().toString()) == null) tx.add(new WorldTransaction.Create(e));
        else { tx.add(new WorldTransaction.Rename(e.id(), e.name())); tx.add(new WorldTransaction.Put(e.id(), e.components().get(0))); }
        try { tx.commit(world, schemas); checkpoint.save(world.snapshot(), schemas); }
        catch (Exception e) { world.restore(before); throw e; }
    }
    private static void fields(JSONObject input, String... allowed) {
        if (!Set.of(allowed).containsAll(input.keySet())) throw new IllegalArgumentException("Unsupported measurement field");
    }
    private static String text(JSONObject input, String key, int max) {
        String value = input.getString(key).strip();
        if (value.isEmpty() || value.length() > max || value.chars().anyMatch(c -> c < 32 && c != 10 && c != 13))
            throw new IllegalArgumentException(key + " requires 1.." + max + " text characters");
        return value;
    }
    private static double number(JSONObject o, String key, double fallback) {
        if (!o.has(key)) return fallback;
        if (!(o.get(key) instanceof Number n) || !Double.isFinite(n.doubleValue())) throw new IllegalArgumentException(key + " must be a finite number");
        return n.doubleValue();
    }
    private static int integer(JSONObject o, String key) {
        double value = number(o, key, -1);
        if (value < 1 || value > Integer.MAX_VALUE || value != Math.rint(value)) throw new IllegalArgumentException(key + " must be a positive integer");
        return (int)value;
    }
    static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    /** Bounded CSV, including quoted commas, newlines and escaped quotes; never executes cells. */
    static List<List<String>> csv(String source) {
        if (!StandardCharsets.UTF_8.newEncoder().canEncode(source) || source.getBytes(StandardCharsets.UTF_8).length > 262144 || source.indexOf('\0') >= 0) throw new IllegalArgumentException("CSV must be valid UTF-8, at most 256 KiB, without NUL");
        if (source.startsWith("\uFEFF")) source = source.substring(1);
        var rows = new ArrayList<List<String>>(); var row = new ArrayList<String>(); var cell = new StringBuilder();
        boolean quoted = false, closed = false;
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (quoted) {
                if (c == '"') { if (i + 1 < source.length() && source.charAt(i + 1) == '"') { cell.append('"'); i++; } else { quoted = false; closed = true; } }
                else cell.append(c);
            } else if (c == '"') {
                if (closed || cell.length() != 0) throw new IllegalArgumentException("Malformed CSV quote");
                quoted = true;
            } else if (c == ',' || c == '\n' || c == '\r') {
                row.add(cell.toString()); cell.setLength(0); closed = false;
                if (row.size() > 32) throw new IllegalArgumentException("CSV has more than 32 columns");
                if (c != ',') {
                    if (c == '\r' && i + 1 < source.length() && source.charAt(i + 1) == '\n') i++;
                    rows.add(List.copyOf(row)); row.clear();
                    if (rows.size() > 2049) throw new IllegalArgumentException("CSV has more than 2048 data rows");
                }
            } else {
                if (closed) throw new IllegalArgumentException("Unexpected text after closing CSV quote");
                cell.append(c);
            }
            if (cell.length() > 4096) throw new IllegalArgumentException("CSV cell exceeds 4096 characters");
        }
        if (quoted) throw new IllegalArgumentException("Unclosed CSV quote");
        if (cell.length() != 0 || !row.isEmpty() || closed) { row.add(cell.toString()); rows.add(List.copyOf(row)); }
        if (rows.size() < 3 || rows.size() > 2049 || rows.stream().anyMatch(r -> r.size() > 32)) throw new IllegalArgumentException("CSV needs a header and 2..2048 data rows, at most 32 columns");
        return rows;
    }
    private static double cellNumber(String raw, int row) {
        String s = raw.strip();
        if (!s.matches("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?")) throw new IllegalArgumentException("Non-numeric value at CSV row " + row);
        double value = Double.parseDouble(s);
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Non-finite value at CSV row " + row);
        return value;
    }
    public static JSONObject inspect(JSONObject input) {
        fields(input, "csv"); var rows = csv(input.getString("csv"));
        return new JSONObject().put("columns", rows.get(0).stream().map(String::strip).toList()).put("dataRows", rows.size() - 1);
    }
    public synchronized JSONObject importCsv(JSONObject input) throws Exception {
        fields(input, "id", "expectedVersion", "title", "filename", "csv", "sourceKind", "sourceNote", "mapping", "missingPolicy", "uncertaintyM");
        String title = text(input, "title", 120), filename = text(input, "filename", 160), source = input.getString("csv");
        if (filename.contains("/") || filename.contains("\\") || filename.contains("\n") || filename.contains("\r")) throw new IllegalArgumentException("Use a source filename, not a path");
        String kind = input.getString("sourceKind"), note = text(input, "sourceNote", 1000), missing = input.getString("missingPolicy");
        if (!Set.of("measured", "synthetic", "unverified").contains(kind)) throw new IllegalArgumentException("Declare measured, synthetic or unverified sourceKind");
        if (!Set.of("reject", "skip").contains(missing)) throw new IllegalArgumentException("Declare reject or skip missingPolicy");
        var mapping = input.getJSONObject("mapping");
        fields(mapping, "timeColumn", "distanceColumn", "timeUnit", "distanceUnit", "timeOffsetS", "distanceOrigin", "direction");
        String timeColumn = text(mapping, "timeColumn", 120), distanceColumn = text(mapping, "distanceColumn", 120);
        String timeUnit = mapping.getString("timeUnit"), distanceUnit = mapping.getString("distanceUnit");
        double timeScale = switch(timeUnit) { case "s" -> 1; case "ms" -> .001; default -> throw new IllegalArgumentException("Time unit must explicitly be s or ms"); };
        double distanceScale = switch(distanceUnit) { case "m" -> 1; case "cm" -> .01; case "mm" -> .001; default -> throw new IllegalArgumentException("Distance unit must explicitly be m, cm or mm"); };
        double offset = number(mapping, "timeOffsetS", 0), origin = number(mapping, "distanceOrigin", 0), direction = number(mapping, "direction", 1);
        if (Math.abs(offset) > 86400 || Math.abs(origin) > 1e9 || (direction != 1 && direction != -1)) throw new IllegalArgumentException("Invalid time offset, distance origin or direction (+1/-1)");
        mapping = new JSONObject(mapping.toString()).put("timeOffsetS", offset).put("distanceOrigin", origin).put("direction", direction);
        var raw = csv(source); var headers = raw.get(0).stream().map(String::strip).toList();
        if (headers.stream().anyMatch(String::isEmpty) || new HashSet<>(headers).size() != headers.size()) throw new IllegalArgumentException("CSV headers must be nonempty and unique");
        int ti = headers.indexOf(timeColumn), di = headers.indexOf(distanceColumn);
        if (ti < 0 || di < 0 || ti == di) throw new IllegalArgumentException("Choose distinct existing time and distance columns");
        var normalized = new JSONArray(); var skipped = new JSONArray(); double previous = -1;
        for (int i = 1; i < raw.size(); i++) {
            var row = raw.get(i); int sourceRow = i + 1;
            if (row.size() != headers.size()) throw new IllegalArgumentException("CSV row " + sourceRow + " has a different column count");
            if (row.get(ti).isBlank() || row.get(di).isBlank()) {
                if (missing.equals("reject")) throw new IllegalArgumentException("Missing measurement at CSV row " + sourceRow);
                skipped.put(sourceRow); continue;
            }
            double time = cellNumber(row.get(ti), sourceRow) * timeScale + offset;
            double distance = (cellNumber(row.get(di), sourceRow) - origin) * distanceScale * direction;
            if (!Double.isFinite(time) || !Double.isFinite(distance) || time < 0 || time > 86400 || time <= previous || Math.abs(distance) > 1e6)
                throw new IllegalArgumentException("Measurements need finite distance and strictly increasing nonnegative mapped time; CSV row " + sourceRow);
            normalized.put(new JSONObject().put("sourceRow", sourceRow).put("timeS", time).put("distanceM", distance)); previous = time;
        }
        if (normalized.length() < 2) throw new IllegalArgumentException("At least two usable measurements are required");
        double uncertainty = number(input, "uncertaintyM", -1);
        if (input.has("uncertaintyM") && (uncertainty < 0 || uncertainty > 1e6)) throw new IllegalArgumentException("uncertaintyM must be nonnegative");
        JSONObject prior = input.has("id") ? require(input.getString("id"), "workspace.measurement.dataset") : null;
        if (prior != null && integer(input, "expectedVersion") != prior.getInt("version")) throw new ConcurrentModificationException("Dataset changed; read the current version before importing");
        String id = prior == null ? UUID.randomUUID().toString() : prior.getString("id"); int version = prior == null ? 1 : prior.getInt("version") + 1;
        String sourceHash = hash(source), versionId = UUID.randomUUID().toString();
        var identity = new JSONArray(List.of(sourceHash, timeColumn, distanceColumn, timeUnit, distanceUnit, offset, origin, direction, missing));
        var value = new JSONObject().put("id", versionId).put("schemaVersion", 1).put("datasetId", id).put("version", version).put("title", title)
            .put("filename", filename).put("csv", source).put("sourceSha256", sourceHash).put("mappingSha256", hash(identity.toString())).put("sourceKind", kind)
            .put("sourceNote", note).put("sourceLabelUserSupplied", true).put("mapping", mapping).put("missingPolicy", missing).put("skippedRows", skipped)
            .put("uncertaintyM", uncertainty < 0 ? JSONObject.NULL : uncertainty).put("frame", "signed downhill displacement from release, metres; elapsed simulation seconds")
            .put("columns", headers).put("rows", normalized).put("createdAt", Instant.now());
        var head = new JSONObject().put("id", id).put("title", title).put("version", version).put("currentVersionId", versionId).put("sourceKind", kind).put("rowCount", normalized.length()).put("sourceSha256", sourceHash);
        commit(record("workspace.measurement.version", value), record("workspace.measurement.dataset", head));
        return value;
    }
    public synchronized JSONArray datasets() {
        var result = new JSONArray();
        for (var e : world.snapshot().entities()) if (type(e).equals("workspace.measurement.dataset")) result.put(payload(e));
        return result;
    }
    public synchronized JSONObject read(String id, int version) {
        var head = require(id, "workspace.measurement.dataset");
        if (version == 0) return require(head.getString("currentVersionId"), "workspace.measurement.version");
        for (var e : world.snapshot().entities()) if (type(e).equals("workspace.measurement.version")) {
            var value = payload(e);
            if (value.getString("datasetId").equals(id) && value.getInt("version") == version) return value;
        }
        throw new IllegalArgumentException("Unknown dataset version");
    }
    public synchronized JSONObject compare(JSONObject input, SceneStore scenes) throws Exception {
        return compare(input, scenes, null);
    }
    private JSONObject compare(JSONObject input, SceneStore scenes, String repeatedFrom) throws Exception {
        fields(input, "datasetId", "datasetVersion", "runId", "expectedSceneVersion", "toleranceM");
        var dataset = read(input.getString("datasetId"), integer(input, "datasetVersion"));
        var run = scenes.runRecord(input.getString("runId")); int expected = integer(input, "expectedSceneVersion");
        if (!run.getString("status").equals("completed")) throw new IllegalArgumentException("Select a completed native experiment");
        if (run.getInt("sceneVersion") != expected || scenes.read(run.getString("sceneId")).getInt("version") != expected)
            throw new ConcurrentModificationException("Run and current authored scene must match expectedSceneVersion; record the saved scene again");
        if (require(dataset.getString("datasetId"), "workspace.measurement.dataset").getInt("version") != dataset.getInt("version"))
            throw new ConcurrentModificationException("Dataset binding is stale; explicitly select its current version");
        double tolerance = number(input, "toleranceM", -1);
        if (tolerance < 0 || tolerance > 10) throw new IllegalArgumentException("Declare toleranceM from 0 to 10 metres");
        var samples = new JSONArray(run.getJSONArray("samples").toString()); var last = run.getJSONObject("final");
        if (samples.getJSONObject(samples.length() - 1).getDouble("timeS") < last.getDouble("timeS")) samples.put(last);
        var points = new JSONArray(); double squares = 0, total = 0, absolute = 0, maximum = 0; int within = 0, cursor = 0;
        double start = samples.getJSONObject(0).getDouble("timeS"), end = last.getDouble("timeS");
        for (Object item : dataset.getJSONArray("rows")) {
            var row = (JSONObject)item; double time = row.getDouble("timeS"), observed = row.getDouble("distanceM");
            if (time < start || time > end) throw new IllegalArgumentException("Measurement time " + time + " s is outside recorded run coverage; no extrapolation or silent trimming");
            while (cursor + 1 < samples.length() && samples.getJSONObject(cursor + 1).getDouble("timeS") < time) cursor++;
            var a = samples.getJSONObject(cursor); var b = samples.getJSONObject(Math.min(cursor + 1, samples.length() - 1));
            double ta = a.getDouble("timeS"), tb = b.getDouble("timeS");
            double value = a.getDouble("distanceM") + (tb == ta ? 0 : (time - ta) / (tb - ta) * (b.getDouble("distanceM") - a.getDouble("distanceM")));
            double residual = value - observed;
            if (!Double.isFinite(residual)) throw new IllegalArgumentException("Comparison produced non-finite values");
            squares += residual * residual; total += residual; absolute += Math.abs(residual); maximum = Math.max(maximum, Math.abs(residual)); if (Math.abs(residual) <= tolerance) within++;
            points.put(new JSONObject().put("sourceRow", row.getInt("sourceRow")).put("timeS", time).put("observedM", observed).put("simulatedM", value).put("residualM", residual));
        }
        int count = points.length();
        var result = new JSONObject().put("id", UUID.randomUUID().toString()).put("schemaVersion", 1).put("title", dataset.getString("title") + " comparison").put("createdAt", Instant.now())
            .put("datasetId", dataset.getString("datasetId")).put("datasetVersion", dataset.getInt("version")).put("datasetVersionId", dataset.getString("id"))
            .put("sourceSha256", dataset.getString("sourceSha256")).put("mappingSha256", dataset.getString("mappingSha256")).put("sourceKind", dataset.getString("sourceKind"))
            .put("runId", run.getString("id")).put("sceneId", run.getString("sceneId")).put("sceneVersion", run.getInt("sceneVersion"))
            .put("backend", run.getString("backend")).put("definition", run.getJSONObject("definition")).put("dt", run.getDouble("dt"))
            .put("method", "measurement-comparison-v1: linear interpolation of retained native distance samples; residual = simulated - observed")
            .put("criteria", "all absolute residuals <= declared toleranceM; agreement only, not causal validation")
            .put("count", count).put("rmseM", Math.sqrt(squares / count)).put("meanResidualM", total / count).put("maeM", absolute / count).put("maxAbsErrorM", maximum)
            .put("toleranceM", tolerance).put("withinTolerance", within).put("passed", within == count).put("uncertaintyM", dataset.get("uncertaintyM")).put("points", points);
        if (repeatedFrom != null) result.put("repeatedFrom", repeatedFrom);
        commit(record("workspace.measurement.comparison", result)); return result;
    }
    public synchronized JSONObject repeat(String id, SceneStore scenes) throws Exception {
        var prior = comparison(id);
        if (require(prior.getString("datasetId"), "workspace.measurement.dataset").getInt("version") != prior.getInt("datasetVersion") ||
            scenes.read(prior.getString("sceneId")).getInt("version") != prior.getInt("sceneVersion"))
            throw new ConcurrentModificationException("Saved comparison bindings changed; select the current scene and dataset explicitly");
        if (Set.of("running", "paused").contains(scenes.snapshot().getString("status")))
            throw new IllegalStateException("Reset the active experiment before repeating a saved comparison");
        scenes.control(new JSONObject().put("action", "reset"));
        var run = scenes.replay(prior.getString("runId"));
        return compare(new JSONObject().put("datasetId", prior.getString("datasetId")).put("datasetVersion", prior.getInt("datasetVersion"))
            .put("runId", run.getString("id")).put("expectedSceneVersion", prior.getInt("sceneVersion")).put("toleranceM", prior.getDouble("toleranceM")), scenes, id);
    }
    public synchronized JSONObject comparison(String id) { return require(id, "workspace.measurement.comparison"); }
    public synchronized JSONArray comparisons(String scene) {
        var result = new JSONArray();
        for (var e : world.snapshot().entities()) if (type(e).equals("workspace.measurement.comparison")) {
            var value = payload(e); if (!scene.isBlank() && !value.getString("sceneId").equals(scene)) continue;
            value.remove("points"); value.remove("definition"); result.put(value);
        }
        return result;
    }
    private static String escape(Object value) { return String.valueOf(value).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;"); }
    private static String metric(double value) { return String.format(Locale.ROOT, "%.6f", value); }
    public synchronized JSONObject report(String id) {
        var result = comparison(id); var data = read(result.getString("datasetId"), result.getInt("datasetVersion"));
        StringBuilder rows = new StringBuilder();
        double maxTime = .001, min = 0, max = .001;
        for (Object o : result.getJSONArray("points")) { var p = (JSONObject)o; maxTime = Math.max(maxTime, p.getDouble("timeS")); min = Math.min(min, Math.min(p.getDouble("observedM"), p.getDouble("simulatedM"))); max = Math.max(max, Math.max(p.getDouble("observedM"), p.getDouble("simulatedM"))); }
        StringBuilder observed = new StringBuilder(), simulated = new StringBuilder();
        for (Object o : result.getJSONArray("points")) {
            var p = (JSONObject)o; double x = 55 + 690 * p.getDouble("timeS") / maxTime;
            observed.append(String.format(Locale.ROOT, "%.3f,%.3f ", x, 260 - 215 * (p.getDouble("observedM") - min) / (max - min)));
            simulated.append(String.format(Locale.ROOT, "%.3f,%.3f ", x, 260 - 215 * (p.getDouble("simulatedM") - min) / (max - min)));
            rows.append("<tr><td>").append(p.getInt("sourceRow")).append("</td><td>").append(p.getDouble("timeS")).append("</td><td>").append(p.getDouble("observedM")).append("</td><td>").append(p.getDouble("simulatedM")).append("</td><td>").append(p.getDouble("residualM")).append("</td></tr>");
        }
        String html = "<!doctype html><html lang=\"en\"><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width\"><title>Measurement comparison</title><style>body{font:16px/1.6 system-ui;background:#101f26;color:#e1f5f0;max-width:1000px;margin:32px auto;padding:20px}h1{line-height:1.2}strong{color:#85ddc4}table{width:100%;border-collapse:collapse;font-size:13px}td,th{padding:8px;border-bottom:1px solid #365354;text-align:left}svg{width:100%;height:auto;background:#172d35}code{overflow-wrap:anywhere}section{overflow:auto}small{color:#adc9c4}</style>"
            + "<h1>" + escape(data.getString("title")) + "</h1><p>Source: <span id=\"source-kind\">" + escape(data.getString("sourceKind")) + "</span> · user-supplied classification · " + escape(data.getString("filename")) + "</p>"
            + "<p>" + escape(data.getString("sourceNote")) + "</p><strong id=\"comparison-result\">" + (result.getBoolean("passed") ? "Within declared tolerance" : "Outside declared tolerance") + "</strong>"
            + "<p>RMSE <span id=\"rmse\">" + metric(result.getDouble("rmseM")) + "</span> m · max absolute residual " + metric(result.getDouble("maxAbsErrorM")) + " m · tolerance " + metric(result.getDouble("toleranceM")) + " m</p>"
            + "<p>Residual = simulated − observed. All " + result.getInt("count") + " rows are compared by linear interpolation; no extrapolation. Acceptance requires every residual within tolerance. Measurement uncertainty (m): " + (data.isNull("uncertaintyM") ? "not supplied" : metric(data.getDouble("uncertaintyM"))) + ". A tolerance or numerical residual is not measurement uncertainty. Full precision is retained in comparison.json and the table.</p>"
            + "<svg viewBox=\"0 0 800 320\" role=\"img\" aria-label=\"Observed and simulated displacement versus time\"><path d=\"M55 30 V260 H750\" stroke=\"#adc9c4\" fill=\"none\"/><polyline points=\"" + observed + "\" fill=\"none\" stroke=\"#f3ba72\" stroke-width=\"3\"/><polyline points=\"" + simulated + "\" fill=\"none\" stroke=\"#85ddc4\" stroke-width=\"2\"/><g fill=\"#e1f5f0\" font-size=\"13\"><text x=\"55\" y=\"22\">Displacement m: " + metric(min) + " to " + metric(max) + "</text><text x=\"55\" y=\"292\">Elapsed time s: 0 to " + metric(maxTime) + " · orange observed · green simulated</text></g></svg>"
            + "<section><table><thead><tr><th>CSV row</th><th>Time s</th><th>Observed m</th><th>Simulated m</th><th>Residual m</th></tr></thead><tbody id=\"comparison-rows\">" + rows + "</tbody></table></section>"
            + "<h2>Retained evidence</h2><p>Dataset " + result.getString("datasetId") + " v" + result.getInt("datasetVersion") + " · scene v" + result.getInt("sceneVersion") + " · run " + result.getString("runId") + "</p><p>Source SHA-256 <code>" + result.getString("sourceSha256") + "</code></p><p>Mapping SHA-256 <code>" + result.getString("mappingSha256") + "</code></p><p>" + escape(result.getString("backend")) + " · dt " + result.getDouble("dt") + " s</p><small>Historical comparison bound to these exact versions. Later edits do not certify a new scene or dataset. Agreement under tested conditions is not proof of a general physical model. This standalone report has no scripts, remote assets or network dependencies.</small></html>";
        return new JSONObject().put("title", result.getString("title")).put("files", new JSONObject().put("index.html", html).put("comparison.json", result.toString(2)).put("measurements.csv", data.getString("csv")));
    }
}
