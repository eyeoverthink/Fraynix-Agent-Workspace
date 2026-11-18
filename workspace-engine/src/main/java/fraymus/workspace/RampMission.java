package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Durable browser-to-physics experiment. Work is checkpointed after each independently checked stage. */
final class RampMission {
    static final String KIND = "local-ramp-experiment-v1";
    static final String FAMILY = "ramp-mission";
    static final String READ_TOOL = "localRamp.read";
    static final String CHECK_TOOL = "localRamp.check";
    static final String PORT = "/api/ramp-mission/";
    private static final Pattern REQUEST = Pattern.compile(
            "^run the local ramp experiment( and pause after reading)?[.!]?$", Pattern.CASE_INSENSITIVE);
    private static final int SAMPLE_TICKS = 12;
    private static final double REPLAY_TOLERANCE = .0001;
    private static final class Backend { static final String SHA = backendHash(); }
    private final Path root;
    private final Path nativeRoot;
    private final WorkspaceStore store;
    final RampInputPage inputs;

    RampMission(Path project, WorkspaceStore store) throws Exception {
        this.store = store;
        root = project.resolve("ramp-missions");
        nativeRoot = project.resolve("native");
        Files.createDirectories(root);
        inputs = new RampInputPage(store);
    }

    static boolean requested(String goal) { return REQUEST.matcher(goal.strip()).matches(); }

    private Path path(String id) { return root.resolve(RampInputPage.canonicalId(id) + ".json"); }

    private JSONObject load(String id) throws Exception {
        JSONObject task = AgentFiles042.loadChecked(path(id));
        require(id.equals(task.getString("id")) && KIND.equals(task.getString("procedure")), "Ramp mission identity changed");
        return task;
    }

    private JSONObject retain(JSONObject task) throws Exception {
        AgentFiles042.saveChecked(path(task.getString("id")), task);
        return view(task);
    }

    synchronized JSONObject start(JSONObject args) throws Exception {
        SpatialScene.keys(args, "id", "request", "source");
        String id = args.getString("id"), request = args.getString("request");
        JSONObject source = args.getJSONObject("source");
        Matcher match = REQUEST.matcher(request.strip());
        if (!match.matches()) throw new IllegalArgumentException("Choose the local ramp experiment");
        if (Files.exists(path(id))) {
            JSONObject prior = load(id);
            require(source.similar(prior.getJSONObject("source")) && request.equals(prior.getString("request")),
                    "Ramp mission binding changed");
            checkSource(prior, !prior.getString("status").equals("completed"));
            return view(prior);
        }
        try (var files = Files.list(root)) {
            require(files.filter(file -> file.getFileName().toString().endsWith(".json")).limit(64).count() < 64,
                    "Ramp experiment history is full");
        }
        var task = new JSONObject().put("id", id).put("procedure", KIND).put("source", source).put("request", request)
                .put("pause", match.group(1) != null).put("backendSha256", Backend.SHA).put("stages", new JSONArray());
        checkSource(task, true);
        return retain(stage(task, "waiting-host", "browser", "Reading the pinned local browser page"));
    }

    private JSONObject checkSource(JSONObject task, boolean current) throws Exception {
        JSONObject source = task.getJSONObject("source");
        JSONObject actual = inputs.bind(source.getString("path"), current);
        require(actual.similar(source), "The original ramp input bytes changed");
        return actual;
    }

    private static JSONObject stage(JSONObject task, String status, String stage, String reason) {
        return task.put("status", status).put("stage", stage).put("reason", reason);
    }

    synchronized JSONObject observe(String id, JSONObject observation) throws Exception {
        JSONObject task = load(id);
        JSONObject source = checkSource(task, true);
        checkObservation(source, observation);
        require(task.getString("stage").equals("browser"), "The browser receipt is already retained");
        task.put("observation", observation);
        note(task, "browser", observation);
        return retain(stage(task, "running", "experiment", "Local table checked against the pinned revision"));
    }

    private static JSONObject checkObservation(JSONObject source, JSONObject observation) {
        SpatialScene.keys(observation, "path", "rows", "visible");
        require(observation.getBoolean("visible") && source.getString("path").equals(observation.getString("path"))
                && RampInputPage.rows(source.getJSONObject("values")).similar(observation.getJSONArray("rows")),
                "The visible browser table differs from the pinned input revision");
        var read = new JSONObject();
        for (Object raw : observation.getJSONArray("rows")) {
            JSONObject row = (JSONObject) raw;
            read.put(row.getString("field"), Double.parseDouble(row.getString("value")));
        }
        return RampInputPage.values(read);
    }

    synchronized JSONObject resume(String id) throws Exception {
        JSONObject task = load(id);
        if (task.getString("status").equals("interrupted")) {
            checkSource(task, true);
            stage(task, "running", "experiment", "Continuing from the retained browser observation");
            return retain(task);
        }
        return view(task);
    }

    synchronized JSONObject cancel(String id) throws Exception {
        JSONObject task = load(id);
        if (task.getString("status").equals("completed")) return view(task);
        return retain(stage(task, "cancelled", task.getString("stage"), "Stopped by you"));
    }

    synchronized JSONObject advance(String id) throws Exception {
        JSONObject task = load(id);
        if (!task.getString("status").equals("running")) return view(task);
        JSONObject source = checkSource(task, true);
        JSONObject observed = checkObservation(source, task.getJSONObject("observation"));
        require(Backend.SHA.equals(task.getString("backendSha256")), "The installed physics backend changed");
        switch (task.getString("stage")) {
            case "experiment" -> {
                if (task.getBoolean("pause") && !task.optBoolean("reviewOffered")) {
                    task.put("reviewOffered", true);
                    return retain(stage(task, "interrupted", "experiment", "Input verified. Resume to start the world experiment."));
                }
                task.put("original", run(source, observed, id));
                note(task, "experiment", task.getJSONObject("original"));
                stage(task, "running", "replay", "Native experiment retained. Replaying in a fresh world.");
            }
            case "replay" -> {
                task.put("replay", run(source, observed, id));
                task.put("audit", audit(task));
                note(task, "replay", task.getJSONObject("replay"));
                stage(task, "running", "report", "Native replay matched. Building the report.");
            }
            case "report" -> {
                JSONObject report = store.writeOnce(KIND + ":" + id, RampMissionReport.build(task, audit(task)));
                var ref = new JSONObject().put("id", report.getString("id")).put("version", report.getInt("version"));
                task.put("report", ref);
                note(task, "artifact", ref);
                stage(task, "ready-report", "report", "Checking the actual report in the creation canvas");
            }
            default -> throw new IllegalStateException("Unknown ramp experiment stage");
        }
        return retain(task);
    }

    private JSONObject run(JSONObject source, JSONObject data, String id) throws Exception {
        var ids = new JSONObject();
        for (String name : List.of("ramp", "block", "ground")) {
            ids.put(name, UUID.nameUUIDFromBytes((id + ":" + name).getBytes(StandardCharsets.UTF_8)).toString());
        }
        JSONArray samples = new JSONArray();
        int last = ticks(data.getDouble("seconds"));
        try (var world = new BulletRamp(RampInputPage.definition(data), ids, nativeRoot)) {
            samples.put(world.sample());
            while (world.tick() < last) {
                world.step();
                if (world.tick() % SAMPLE_TICKS == 0 || world.tick() == last) samples.put(world.sample());
            }
        }
        // Make both independent runs use the same persisted numeric representation before comparison.
        return WorldFrames.copy(new JSONObject().put("sessionId", UUID.randomUUID().toString())
                .put("sourceSha256", WorldFrames.hash(source)).put("backend", BulletRamp.BACKEND)
                .put("backendSha256", Backend.SHA).put("samples", samples));
    }

    private static String backendHash() {
        try {
            var hash = MessageDigest.getInstance("SHA-256");
            Path minie = Path.of(com.jme3.bullet.PhysicsSpace.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            try (var in = new java.security.DigestInputStream(Files.newInputStream(minie), hash)) {
                in.transferTo(java.io.OutputStream.nullOutputStream());
            }
            for (Class<?> type : List.of(BulletRamp.class, RampDefinition.class)) {
                try (var in = type.getResourceAsStream(type.getSimpleName() + ".class")) {
                    if (in == null) throw new IllegalStateException("Missing physics implementation identity");
                    hash.update(in.readAllBytes());
                }
            }
            return HexFormat.of().formatHex(hash.digest());
        } catch (Exception e) {
            throw new IllegalStateException("Could not establish the actual physics backend identity", e);
        }
    }

    private static int ticks(double seconds) { return (int) Math.round(seconds / BulletRamp.DT); }

    private static void note(JSONObject task, String name, JSONObject record) {
        task.getJSONArray("stages").put(new JSONObject().put("name", name).put("status", "verified")
                .put("sha256", WorldFrames.hash(record)));
    }

    static JSONObject audit(JSONObject task) {
        JSONObject a = task.getJSONObject("original"), b = task.getJSONObject("replay"), source = task.getJSONObject("source");
        String sourceHash = WorldFrames.hash(source);
        require(!a.getString("sessionId").equals(b.getString("sessionId")), "Replay needs a fresh native world");
        int last = ticks(source.getJSONObject("values").getDouble("seconds"));
        for (JSONObject run : List.of(a, b)) {
            require(sourceHash.equals(run.getString("sourceSha256")) && BulletRamp.BACKEND.equals(run.getString("backend"))
                            && task.getString("backendSha256").equals(run.getString("backendSha256")),
                    "Native trial belongs to another source or engine");
            verifySamples(run.getJSONArray("samples"), last);
        }
        JSONArray before = a.getJSONArray("samples"), after = b.getJSONArray("samples");
        double maxPosition = 0, maxSpeed = 0;
        for (int i = 0; i < before.length(); i++) {
            JSONObject first = before.getJSONObject(i), replay = after.getJSONObject(i);
            require(first.getInt("contacts") == replay.getInt("contacts"), "Native replay contact differs");
            double spatial = 0, velocity = 0;
            for (int axis = 0; axis < 3; axis++) {
                double positionDelta = delta(first, replay, "position", axis);
                double velocityDelta = delta(first, replay, "velocity", axis);
                spatial += positionDelta * positionDelta;
                velocity += velocityDelta * velocityDelta;
            }
            double dot = 0;
            for (int axis = 0; axis < 4; axis++) dot += first.getJSONArray("rotation").getDouble(axis)
                    * replay.getJSONArray("rotation").getDouble(axis);
            require(Math.abs(1 - Math.abs(dot)) <= REPLAY_TOLERANCE
                    && Math.abs(first.getDouble("distanceM") - replay.getDouble("distanceM")) <= REPLAY_TOLERANCE,
                    "Native replay rotation or downhill travel differs");
            maxPosition = Math.max(maxPosition, Math.sqrt(spatial));
            maxSpeed = Math.max(maxSpeed, Math.sqrt(velocity));
        }
        require(maxPosition <= REPLAY_TOLERANCE && maxSpeed <= REPLAY_TOLERANCE, "Native replay differed beyond tolerance");
        JSONObject end = before.getJSONObject(before.length() - 1);
        return new JSONObject().put("kind", KIND).put("passed", true).put("sourceSha256", sourceHash)
                .put("observationSha256", WorldFrames.hash(task.getJSONObject("observation"))).put("ticks", last)
                .put("backendSha256", task.getString("backendSha256"))
                .put("sampleCount", before.length()).put("tolerance", REPLAY_TOLERANCE)
                .put("maxPositionErrorM", maxPosition).put("maxVelocityErrorMps", maxSpeed)
                .put("distanceM", end.getDouble("distanceM")).put("seconds", last * BulletRamp.DT)
                .put("originalSha256", WorldFrames.hash(a)).put("replaySha256", WorldFrames.hash(b));
    }

    private static double delta(JSONObject a, JSONObject b, String key, int axis) {
        return a.getJSONArray(key).getDouble(axis) - b.getJSONArray(key).getDouble(axis);
    }

    private static void verifySamples(JSONArray samples, int last) {
        int count = 1 + (last + SAMPLE_TICKS - 1) / SAMPLE_TICKS;
        require(samples.length() == count, "Native trial sample count changed");
        for (int i = 0; i < count; i++) {
            JSONObject sample = samples.getJSONObject(i);
            int tick = Math.min(i * SAMPLE_TICKS, last);
            require(sample.getInt("tick") == tick && sample.getDouble("timeS") == tick * BulletRamp.DT
                    && sample.getInt("contacts") >= 0, "Native sample cadence or contact count changed");
            for (String key : List.of("position", "velocity", "rotation")) {
                JSONArray vector = sample.getJSONArray(key);
                require(vector.length() == (key.equals("rotation") ? 4 : 3), "Native vector dimension changed");
                for (int k = 0; k < vector.length(); k++) require(Double.isFinite(vector.getDouble(k)), "Invalid native vector");
            }
            require(Double.isFinite(sample.getDouble("distanceM")) && Double.isFinite(sample.getDouble("speedMps")),
                    "Invalid native motion sample");
        }
    }

    private JSONObject artifact(JSONObject task, boolean currentSource) throws Exception {
        checkObservation(checkSource(task, currentSource), task.getJSONObject("observation"));
        JSONObject computed = audit(task);
        require(computed.similar(task.getJSONObject("audit")), "The original ramp experiment proof changed");
        JSONObject ref = task.getJSONObject("report"), artifact = store.read(ref.getString("id"));
        JSONObject regenerated = RampMissionReport.build(task, computed);
        require(artifact.getInt("version") == ref.getInt("version")
                && artifact.getJSONObject("contents").similar(regenerated.getJSONObject("files"))
                && artifact.getJSONArray("sources").similar(regenerated.getJSONArray("sources")), "Saved ramp report changed");
        return artifact;
    }

    synchronized JSONObject reportCheck(String id) throws Exception {
        JSONObject task = load(id);
        require(task.getString("status").equals("ready-report"), "The report is not ready");
        artifact(task, true);
        return new JSONObject().put("artifact", task.getJSONObject("report"))
                .put("steps", RampMissionReport.checks(task));
    }

    synchronized JSONObject acceptReport(String id, JSONObject receipt) throws Exception {
        JSONObject task = load(id);
        String status = task.getString("status");
        if (status.equals("completed")) return verify(id);
        require(status.equals("ready-report"), "The report is not ready");
        JSONObject stored = requireCanvas(task, true);
        require(stored.similar(receipt), "The returned canvas checks differ from the stored run");
        task.put("canvasReceipt", stored);
        note(task, "canvas", stored);
        return retain(stage(task, "completed", "verified", summary(task.getJSONObject("audit"))));
    }

    private JSONObject requireCanvas(JSONObject task, boolean currentSource) throws Exception {
        JSONObject report = artifact(task, currentSource);
        JSONObject test = report.optJSONObject("test");
        require(RampMissionReport.verifiedReceipt(task.getJSONObject("report"), test, RampMissionReport.checks(task)),
                "The saved report still needs its original canvas behaviour checks");
        return test;
    }

    synchronized JSONObject verify(String id) throws Exception {
        JSONObject task = load(id);
        require(task.getString("status").equals("completed"), "The ramp mission is incomplete");
        requireCanvas(task, false);
        require(RampMissionReport.verifiedReceipt(task.getJSONObject("report"), task.getJSONObject("canvasReceipt"),
                RampMissionReport.checks(task)), "The original tested report receipt changed");
        return view(task);
    }

    synchronized JSONObject read(String id) throws Exception { return view(load(id)); }

    private static JSONObject view(JSONObject task) {
        JSONObject brief = WorldFrames.copy(task);
        for (String key : List.of("original", "replay", "canvasReceipt")) {
            if (brief.optJSONObject(key) != null) brief.getJSONObject(key).remove("samples");
        }
        return brief;
    }

    static String summary(JSONObject audit) {
        return String.format(Locale.ROOT, "The block travelled %.3f m in %.2f s. Fresh native replay matched %d samples.",
                audit.getDouble("distanceM"), audit.getDouble("seconds"), audit.getInt("sampleCount"));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
