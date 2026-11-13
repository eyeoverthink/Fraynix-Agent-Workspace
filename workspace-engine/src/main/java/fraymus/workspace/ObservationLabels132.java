package fraymus.workspace;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.json.*;

/** Source-only annotations are downstream audit evidence, never tracker input. */
final class ObservationLabels132 {
    static final String VERSION = "observation-labels-132-v1";
    private static final Set<String> VISIBILITY = Set.of("visible", "partial", "occluded", "out-of-view", "uncertain");
    private static final Set<String> CONTACT = Set.of("observed", "occluded", "out-of-view", "not-grounded", "uncertain");
    private final Path root;
    private final WorldObservationStore observations;
    private final MetricObservationStore metrics;

    ObservationLabels132(Path project, WorldObservationStore observations, MetricObservationStore metrics) {
        root = project.resolve("observation-labels132");
        this.observations = observations;
        this.metrics = metrics;
    }

    static String identity() {
        try (var in = ObservationLabels132.class.getResourceAsStream("ObservationLabels132.class")) {
            return PerceptionStore.hash(in.readAllBytes());
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    JSONObject source(String datasetId, int ordinal) throws Exception {
        var d = observations.dataset(datasetId);
        byte[] pixels = observations.image(datasetId, ordinal);
        return new JSONObject().put("datasetId", datasetId).put("datasetSha256", WorldFrames.hash(d))
            .put("title", d.getString("title")).put("source", d.getString("source")).put("license", d.getString("license"))
            .put("width", d.getInt("width")).put("height", d.getInt("height"))
            .put("frameCount", d.getJSONArray("frames").length()).put("objects", d.getJSONArray("objects"))
            .put("ordinal", ordinal).put("frame", d.getJSONArray("frames").getJSONObject(ordinal))
            .put("image", Base64.getEncoder().encodeToString(pixels));
    }

    private JSONObject verifiedDataset(String id) throws Exception {
        var d = observations.dataset(id);
        String hash = WorldFrames.hash(new JSONObject().put("objects", d.getJSONArray("objects")).put("frames", d.getJSONArray("frames")));
        if (!hash.equals(d.getString("contentSha256"))) throw new IllegalStateException("Original dataset manifest changed");
        for (int i = 0; i < d.getJSONArray("frames").length(); i++) observations.image(id, i);
        return d;
    }

    private static void text(JSONObject input, String key, int bound) {
        String s = input.getString(key);
        if (s.isBlank() || s.length() > bound) throw new IllegalArgumentException("Declare " + key + " within " + bound + " characters");
    }

    static void validate(JSONObject input, JSONObject dataset) {
        SpatialScene.keys(input, "schema", "datasetId", "datasetSha256", "objectKey", "annotator", "method", "provenance", "exposure", "rows");
        if (!VERSION.equals(input.getString("schema")) || !dataset.getString("id").equals(input.getString("datasetId"))
            || !WorldFrames.hash(dataset).equals(input.getString("datasetSha256"))) throw new IllegalArgumentException("Labels must bind the exact original dataset");
        boolean found = false;
        for (Object raw : dataset.getJSONArray("objects")) if (((JSONObject)raw).getString("key").equals(input.getString("objectKey"))) found = true;
        if (!found) throw new IllegalArgumentException("Unknown source object");
        text(input, "annotator", 120); text(input, "provenance", 1500);
        if (!Set.of("human-source-only", "ai-source-only", "external-annotations", "synthetic-control").contains(input.getString("method"))) throw new IllegalArgumentException("Declare annotation method");
        if (!Set.of("before-tracking", "retrospective-source-only", "tracker-results-seen").contains(input.getString("exposure"))) throw new IllegalArgumentException("Declare prior exposure honestly");
        var rows = input.getJSONArray("rows");
        if (rows.isEmpty() || rows.length() > 95) throw new IllegalArgumentException("Provide 1..95 distinct later-frame labels; seed excluded");
        int previous = 0;
        for (Object raw : rows) {
            var r = (JSONObject)raw;
            SpatialScene.keys(r, "ordinal", "sourceFrame", "imageSha256", "visibility", "contact", "pointImage", "radiusPx", "note");
            int ordinal = r.getInt("ordinal");
            if (ordinal != r.getDouble("ordinal") || ordinal <= previous || ordinal >= dataset.getJSONArray("frames").length()) throw new IllegalArgumentException("Labels must be ordered, unique later source frames");
            previous = ordinal;
            var original = dataset.getJSONArray("frames").getJSONObject(ordinal);
            if (r.getDouble("sourceFrame") != original.getInt("sourceFrame") || !r.getString("imageSha256").equals(original.getString("imageSha256"))) throw new IllegalArgumentException("Label frame number or image checksum differs");
            String visibility = r.getString("visibility"), contact = r.getString("contact");
            if (!VISIBILITY.contains(visibility) || !CONTACT.contains(contact)) throw new IllegalArgumentException("Unknown visibility/contact label");
            text(r, "note", 500);
            if (contact.equals("observed")) {
                if (!Set.of("visible", "partial").contains(visibility)) throw new IllegalArgumentException("Hidden or uncertain targets cannot have an observed contact");
                var point = r.getJSONArray("pointImage");
                if (point.length() != 2) throw new IllegalArgumentException("Contact requires two image coordinates");
                for (Object value : point) PlaneCalibration.bounded(((Number)value).doubleValue(), 0, 1);
                PlaneCalibration.bounded(r.getDouble("radiusPx"), 1, 32);
            } else if (r.has("pointImage") || r.has("radiusPx")) throw new IllegalArgumentException("Do not invent coordinates for unobservable contacts");
            if (visibility.equals("out-of-view") && !contact.equals("out-of-view")) throw new IllegalArgumentException("Out-of-view target has no in-frame contact");
            if (visibility.equals("occluded") && !Set.of("occluded", "uncertain").contains(contact)) throw new IllegalArgumentException("Fully occluded contact is unobservable");
        }
    }

    synchronized JSONObject save(JSONObject input) throws Exception {
        var dataset = verifiedDataset(input.getString("datasetId"));
        validate(input, dataset);
        String id = WorldFrames.hash(input);
        write("labels", id, new JSONObject().put("id", id).put("input", WorldFrames.copy(input)));
        return read(id);
    }

    JSONObject importRecord(JSONObject record) throws Exception {
        SpatialScene.keys(record, "id", "input");
        if (!WorldFrames.hash(record.getJSONObject("input")).equals(record.getString("id"))) throw new IllegalArgumentException("Exported label identity differs; retain the original and explicitly author a correction");
        return save(record.getJSONObject("input"));
    }

    JSONObject read(String id) throws Exception {
        var record = readFile("labels", id);
        var input = record.getJSONObject("input");
        if (!WorldFrames.hash(input).equals(id)) throw new IllegalStateException("Frozen labels changed");
        validate(input, verifiedDataset(input.getString("datasetId")));
        return record;
    }

    synchronized JSONObject state() throws Exception {
        var labels = new JSONArray(); var audits = new JSONArray();
        for (String type : List.of("labels", "audits")) {
            Path directory = root.resolve(type);
            if (!Files.exists(directory)) continue;
            try (var files = Files.list(directory)) {
                var paths = files.filter(p -> p.toString().endsWith(".json")).sorted().toList();
                if (paths.size() > 64) throw new IllegalStateException("Retained evidence exceeds capacity");
                for (var path : paths) {
                    String id = path.getFileName().toString().replace(".json", "");
                    var record = readFile(type, id);
                    // Full source/outcome verification happens on opening, not silently at listing time.
                    if (type.equals("labels")) labels.put(new JSONObject().put("id", id).put("datasetId", record.getJSONObject("input").getString("datasetId")).put("objectKey", record.getJSONObject("input").getString("objectKey")).put("rows", record.getJSONObject("input").getJSONArray("rows").length()));
                    else audits.put(new JSONObject().put("id", id).put("labelId", record.getJSONObject("input").getString("labelId")).put("runId", record.getJSONObject("input").getString("runId")));
                }
            }
        }
        return new JSONObject().put("labels", labels).put("audits", audits);
    }

    private JSONObject readFile(String type, String id) throws Exception {
        if (!id.matches("[a-f0-9]{64}")) throw new IllegalArgumentException("Invalid evidence identity");
        Path file = root.resolve(type).resolve(id + ".json");
        if (Files.size(file) > 1_000_000) throw new IllegalStateException("Evidence size exceeds bound");
        var record = new JSONObject(Files.readString(file));
        if (type.equals("labels")) SpatialScene.keys(record, "id", "input");
        var payload = WorldFrames.copy(record); payload.remove("id");
        String actual = type.equals("labels") ? WorldFrames.hash(record.getJSONObject("input")) : WorldFrames.hash(payload);
        if (!id.equals(record.getString("id")) || !id.equals(actual)) throw new IllegalStateException("Retained evidence checksum changed");
        return record;
    }

    private void write(String type, String id, JSONObject value) throws Exception {
        Path directory = root.resolve(type); Files.createDirectories(directory);
        try (var channel = FileChannel.open(directory.resolve("write.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE); var lock = channel.lock()) {
            Path file = directory.resolve(id + ".json");
            if (Files.exists(file)) { if (!readFile(type, id).similar(value)) throw new IllegalStateException("Evidence identity conflict"); return; }
            try (var files = Files.list(directory)) { if (files.filter(p -> p.toString().endsWith(".json")).count() >= 64) throw new IllegalStateException("64 retained " + type + " reached; no pruning"); }
            byte[] bytes = value.toString(2).getBytes(StandardCharsets.UTF_8);
            if (bytes.length > 1_000_000) throw new IllegalArgumentException("Evidence size exceeds bound");
            Path pending = directory.resolve(id + ".pending");
            try (var out = FileChannel.open(pending, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                var buffer = ByteBuffer.wrap(bytes); while (buffer.hasRemaining()) out.write(buffer); out.force(true);
            }
            Files.move(pending, file, StandardCopyOption.ATOMIC_MOVE);
        }
    }

    synchronized JSONObject audit(JSONObject input) throws Exception {
        var result = evaluate(input); String id = WorldFrames.hash(result); result.put("id", id);
        write("audits", id, result); return readAudit(id);
    }

    JSONObject readAudit(String id) throws Exception {
        var result = readFile("audits", id); var expected = WorldFrames.copy(result); expected.remove("id");
        if (!evaluate(result.getJSONObject("input")).similar(expected)) throw new IllegalStateException("Original audit source, code or outcome changed");
        return result;
    }

    private JSONObject evaluate(JSONObject input) throws Exception {
        SpatialScene.keys(input, "labelId", "runId", "geometryId");
        var labels = read(input.getString("labelId")).getJSONObject("input");
        var run = observations.read(input.getString("runId"));
        if (!run.getString("status").equals("completed") || !run.getString("datasetId").equals(labels.getString("datasetId"))) throw new IllegalArgumentException("Choose a completed run from the labelled dataset");
        if (!run.getString("codeSha256").equals(WorldObservationStore.identity())) throw new IllegalStateException("Tracker code changed; preserve the original audit/run");
        var dataset = verifiedDataset(run.getString("datasetId"));
        String body = null;
        for (Object raw : run.getJSONArray("bindings")) { var b = (JSONObject)raw; if (b.getString("objectKey").equals(labels.getString("objectKey"))) body = b.getString("bodyId"); }
        if (body == null) throw new IllegalArgumentException("Labelled object has no original run binding");
        JSONObject metric = null;
        if (input.has("geometryId")) { metric = metrics.view(new JSONObject().put("runId", run.getString("id")).put("geometryId", input.getString("geometryId"))); metric.remove("nativeStale"); }
        return compute(labels, dataset, run, body, metric).put("input", WorldFrames.copy(input)).put("codeSha256", identity())
            .put("runSha256", WorldFrames.hash(run)).put("metricSha256", metric == null ? JSONObject.NULL : WorldFrames.hash(metric));
    }

    static JSONObject compute(JSONObject labels, JSONObject dataset, JSONObject run, String body, JSONObject metric) {
        var scores = new JSONArray(); var counts = new JSONObject();
        for (String v : new TreeSet<>(VISIBILITY)) counts.put(v, new JSONObject().put("labels", 0).put("accepted", 0).put("missing", 0));
        int contacts = 0, contactAvailable = 0, rawAvailable = 0, within = 0, unsupported = 0, reacquired = 0, unverifiableRecovery = 0;
        double rawSum = 0, filteredSum = 0, metricSum = 0; int metricAvailable = 0;
        PlaneCalibration plane = metric == null ? null : new PlaneCalibration(metric.getJSONObject("geometry").getJSONObject("definition"));
        for (Object raw : labels.getJSONArray("rows")) {
            var label = (JSONObject)raw; int ordinal = label.getInt("ordinal"); JSONObject object = null;
            for (Object f : run.getJSONArray("trace")) { var frame = (JSONObject)f; if (frame.getInt("ordinal") == ordinal) {
                if (frame.getInt("sourceFrame") != label.getInt("sourceFrame") || !frame.getString("imageSha256").equals(label.getString("imageSha256"))) throw new IllegalStateException("Trace frame differs from labelled pixels");
                for (Object o : frame.getJSONArray("objects")) if (((JSONObject)o).getString("bodyId").equals(body)) { if (object != null) throw new IllegalStateException("Duplicate trace object"); object = (JSONObject)o; }
            }}
            if (object == null) throw new IllegalArgumentException("Label has no corresponding trace row");
            var tracker = object.getJSONObject("tracker"); boolean accepted = object.getJSONObject("estimate").getBoolean("accepted");
            String visibility = label.getString("visibility"); var group = counts.getJSONObject(visibility);
            group.increment("labels").increment(accepted ? "accepted" : "missing");
            boolean hidden = Set.of("occluded", "out-of-view").contains(visibility);
            var score = WorldFrames.copy(label).put("accepted", accepted).put("unsupportedHiddenMeasurement", accepted && hidden).put("trackerStatus", tracker.getString("status"));
            if (accepted && hidden) unsupported++;
            boolean recovery = tracker.getString("status").equals("reacquired");
            if (recovery) { reacquired++; if (!label.getString("contact").equals("observed") || !accepted) unverifiableRecovery++; }
            if (label.getString("contact").equals("observed")) {
                contacts++;
                if (accepted && tracker.optJSONArray("box") != null) {
                    var box = PixelTracker.Box.from(tracker.getJSONArray("box"));
                    var point = label.getJSONArray("pointImage"); double x = point.getDouble(0), y = point.getDouble(1);
                    double rawError = pixelError(box.x()+box.w()/2, box.y()+box.h(), x, y, dataset);
                    score.put("rawAnchorErrorPx", rawError); rawSum += rawError; rawAvailable++;
                    // This is the exact production metric anchor: filtered centre plus accepted box height / 2.
                    var position = object.getJSONObject("estimate").getJSONArray("position");
                    double fx = position.getDouble(0), fy = position.getDouble(1)+box.h()/2;
                    double error = pixelError(fx, fy, x, y, dataset);
                    score.put("filteredAnchorErrorPx", error).put("withinLabelRadius", error <= label.getDouble("radiusPx"));
                    filteredSum += error; contactAvailable++; if (error <= label.getDouble("radiusPx")) within++;
                    if (plane != null) {
                        if (plane.inPlane(x,y) && plane.inPlane(fx,fy)) { double e = PlaneCalibration.distance(plane.project(x,y), plane.project(fx,fy)); score.put("projectedContactErrorM", e); metricSum += e; metricAvailable++; }
                        else score.put("projectionUnavailable", true);
                    }
                }
            }
            scores.put(score);
        }
        return new JSONObject().put("schema", VERSION+"-audit").put("visibility", counts).put("scores", scores)
            .put("summary", new JSONObject().put("labels", scores.length()).put("unsupportedHiddenMeasurements", unsupported)
                .put("observableContacts", contacts).put("contactAvailable", contactAvailable).put("contactMissing", contacts-contactAvailable)
                .put("withinLabelRadius", within).put("rawAnchorMeanErrorPx", rawAvailable == 0 ? JSONObject.NULL : rawSum/rawAvailable)
                .put("filteredAnchorMeanErrorPx", contactAvailable == 0 ? JSONObject.NULL : filteredSum/contactAvailable)
                .put("projectedContactAvailable", metricAvailable).put("projectedContactMeanErrorM", metricAvailable == 0 ? JSONObject.NULL : metricSum/metricAvailable)
                .put("labelledReacquisitions", reacquired).put("reacquisitionsWithoutContactCheck", unverifiableRecovery))
            .put("scope", "Declared independent source annotations, not automatically authenticated ground truth. Contact errors compare box/filtered anchors to visible labelled contact only. Projection shares the declared camera calibration and is not independent metrology. Occluded measurements are unsupported, not proof of identity switching. Unlabelled frames are unevaluated. No training, fitting, default change or native world action.");
    }

    private static double pixelError(double x, double y, double rx, double ry, JSONObject d) {
        return Math.hypot((x-rx)*d.getInt("width"), (y-ry)*d.getInt("height"));
    }
}
