
package fraymus.workspace;

import fraymus.neural.RelayCourse;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/** A local mission owns an isolated game process and checks each completed run in a fresh one. */
final class RelayPlaytest implements AutoCloseable {
    private static final Pattern REQUEST = Pattern.compile(
            "^(?:playtest|verify) relay (ridge|switchyard) at (all three paces|(?:normal|steady|careful) pace)[.!]?$",
            Pattern.CASE_INSENSITIVE);
    private static final List<String> PACES = List.of("normal", "steady", "careful");
    private static final int STEP_TICKS = 24;
    private static final int MAX_TICKS = 864;
    private static final double MARGIN = .035;
    static final String KIND = "relay-playtest-v1";

    private final Path repo;
    private final Path root;
    private final WorkspaceStore store;
    private final GameContentPack content;
    private final EmbeddedGameClient game;

    RelayPlaytest(Path repo, Path project, WorkspaceStore store) throws Exception {
        this.repo = repo;
        this.root = project.resolve("relay-playtests");
        this.store = store;
        content = new GameContentPack(repo);
        game = new EmbeddedGameClient(repo, root.resolve("runtime"));
        Files.createDirectories(root);
        try (var files = Files.list(root)) {
            for (Path path : files.filter(p -> p.getFileName().toString().endsWith(".json")).toList()) {
                var task = AgentFiles042.loadChecked(path);
                if (!path.equals(path(task.getString("id")))) throw new IllegalStateException("Relay mission ID changed");
                if (task.getString("status").equals("running")) interrupt(task, "Game process stopped. Resume to repeat the unfinished trial.");
            }
        }
    }

    static boolean requested(String text) { return REQUEST.matcher(text.strip()).matches(); }

    private Path path(String id) {
        if (!UUID.fromString(id).toString().equals(id)) throw new IllegalArgumentException("Canonical mission ID required");
        return root.resolve(id + ".json");
    }

    synchronized boolean owns(String id) { return Files.isRegularFile(path(id)); }
    private JSONObject load(String id) throws Exception {
        JSONObject task = AgentFiles042.loadChecked(path(id));
        if (!id.equals(task.getString("id")) || !KIND.equals(task.getString("procedure"))) {
            throw new IllegalStateException("Relay mission identity changed");
        }
        return task;
    }
    private void save(JSONObject task) throws Exception { AgentFiles042.saveChecked(path(task.getString("id")), task); }

    synchronized JSONObject start(JSONObject input) throws Exception {
        SpatialScene.keys(input, "id", "request", "expectedSource");
        String id = input.getString("id");
        String text = input.getString("request");
        var match = REQUEST.matcher(text.strip());
        if (!match.matches()) throw new IllegalArgumentException("Choose a Relay course and pace");
        if (owns(id)) {
            JSONObject prior = load(id);
            require(text.equals(prior.getString("request")), "Mission ID has a different request");
            if (input.has("expectedSource")) {
                checkSource(prior);
                require(input.getJSONObject("expectedSource").similar(prior.getJSONObject("source")), "The reviewed Relay source changed");
            }
            return view(prior);
        }
        try (var files = Files.list(root)) {
            require(files.filter(p -> p.getFileName().toString().endsWith(".json")).count() < 24,
                    "Relay playtest history is full (24)");
        }
        RelayCourse course = courseFor(match.group(1));
        String pace = match.group(2).toLowerCase(Locale.ROOT);
        List<String> paces = pace.equals("all three paces") ? PACES : List.of(pace.split(" ")[0]);
        JSONObject source = binding(course);
        if (input.has("expectedSource"))
            require(input.getJSONObject("expectedSource").similar(source), "The reviewed Relay source changed");
        JSONObject task = new JSONObject().put("id", id).put("request", text).put("procedure", KIND)
                .put("course", course.id()).put("title", course.title()).put("paces", paces)
                .put("source", source).put("status", "running").put("stage", "prepare")
                .put("trials", new JSONArray()).put("reason", "Checking the original course and installed engine");
        save(task);
        return view(task);
    }

    synchronized JSONObject bind(String request) throws Exception {
        var match = REQUEST.matcher(request.strip());
        if (!match.matches()) throw new IllegalArgumentException("Choose a Relay course and pace");
        return binding(courseFor(match.group(1)));
    }

    private static RelayCourse courseFor(String name) {
        return name.equalsIgnoreCase("ridge") ? RelayCourse.RIDGE : RelayCourse.SWITCHYARD;
    }

    private JSONObject binding(RelayCourse course) throws Exception {
        JSONObject pack = content.verify(course);
        var rails = new JSONObject();
        for (Object item : new JSONArray(GameLevels114.read(repo.resolve(course.level()), 2_000_000))) {
            JSONObject entity = (JSONObject) item;
            for (Object value : entity.getJSONArray("components")) {
                JSONObject component = (JSONObject) value;
                if (component.getString("type").equals("components.CourseContact")
                        && component.getJSONObject("properties").getString("kind").equals("HAZARD")) {
                    rails.put(entity.getString("persistentId"), entity.getString("name"));
                }
            }
        }
        require(!rails.isEmpty(), "An original rail fixture is required");
        var engine = new JSONObject();
        try (var libs = Files.list(repo.resolve("build/install/Fraynix-Studio/lib"))) {
            for (Path jar : libs.filter(p -> p.getFileName().toString().endsWith(".jar")).sorted().toList()) {
                engine.put(jar.getFileName().toString(), GameLevels114.hash(jar));
            }
        }
        require(!engine.isEmpty(), "The installed native game engine is missing");
        return new JSONObject().put("manifestSha256", pack.getString("manifestSha256"))
                .put("finishX", pack.getDouble("finishX")).put("rails", rails).put("engine", engine);
    }

    private RelayCourse checkSource(JSONObject task) throws Exception {
        RelayCourse course = RelayCourse.find(task.getString("course"));
        require(course != null && course.hasPaceOptions(), "Unknown original playtest course");
        require(binding(course).similar(task.getJSONObject("source")), "Original course or native game engine changed");
        return course;
    }

    private static List<JSONObject> accepted(JSONObject task) {
        var rows = new ArrayList<JSONObject>();
        for (Object value : task.getJSONArray("trials")) {
            JSONObject trial = (JSONObject) value;
            if (trial.getString("status").equals("accepted")) rows.add(trial);
        }
        return rows;
    }

    private static JSONObject running(JSONObject task) {
        JSONArray rows = task.getJSONArray("trials");
        JSONObject last = rows.optJSONObject(rows.length() - 1);
        return last != null && last.getString("status").equals("running") ? last : null;
    }

    synchronized JSONObject advance(String id) throws Exception {
        JSONObject task = load(id);
        if (!task.getString("status").equals("running")) return view(task);
        try {
            JSONObject active = running(task);
            if (active != null) {
                step(task, active);
            } else {
                int slot = accepted(task).size();
                if (slot == task.getJSONArray("paces").length() * 2) finish(task);
                else open(task, slot);
            }
        } catch (Exception error) {
            interrupt(task, String.valueOf(error.getMessage()));
        }
        save(task);
        return view(task);
    }

    private void open(JSONObject task, int slot) throws Exception {
        RelayCourse course = checkSource(task);
        var frame = game.openCourse(course);
        var state = frame.getJSONObject("state");
        require(state.getLong("tick") == 0 && course.id().equals(state.getString("contentId")), "Fresh game session required");
        JSONObject trial = new JSONObject().put("status", "running").put("slot", slot)
                .put("pace", task.getJSONArray("paces").getString(slot / 2)).put("replay", slot % 2 == 1)
                .put("sessionId", state.getString("sessionId")).put("playerId", state.getJSONObject("player").getString("id"))
                .put("trace", new JSONArray());
        task.getJSONArray("trials").put(trial);
        stage(task, trial, 0);
    }

    private static void stage(JSONObject task, JSONObject trial, long tick) {
        boolean replay = trial.getBoolean("replay");
        task.put("stage", replay ? "replay" : "playtest")
                .put("reason", (replay ? "Replaying " : "Playtesting ") + trial.getString("pace")
                        + " pace · tick " + tick);
    }

    private void step(JSONObject task, JSONObject trial) throws Exception {
        JSONObject before = game.frame().getJSONObject("state");
        require(trial.getString("sessionId").equals(before.getString("sessionId")), "Owned game session changed");
        JSONArray trace = trial.getJSONArray("trace");
        long beforeTick = trace.isEmpty() ? 0 : trace.getJSONObject(trace.length() - 1).getLong("tick");
        require(before.getLong("tick") == beforeTick, "Game tick differs from the saved checkpoint");
        JSONObject state = game.command(new JSONObject().put("action", "ai-step").put("ticks", STEP_TICKS)
                .put("railPace", trial.getString("pace")).put("targetX", task.getJSONObject("source").getDouble("finishX") + 1))
                .getJSONObject("state");
        JSONObject player = state.getJSONObject("player"), rails = state.getJSONObject("railNavigation");
        long tick = state.getLong("tick");
        require(trial.getString("sessionId").equals(state.getString("sessionId"))
                && trial.getString("playerId").equals(player.getString("id"))
                && task.getString("course").equals(state.getString("contentId"))
                && rails.getString("pace").equals(trial.getString("pace"))
                && state.getJSONObject("controller").getString("policy").equals("contact122")
                && tick == beforeTick + STEP_TICKS, "Observed run, tick or pace changed");
        var frame = new JSONObject().put("tick", tick).put("player", player)
                .put("progress", state.getJSONObject("courseProgress")).put("navigation", rails);
        trace.put(frame);
        stage(task, trial, tick);
        require(!player.getBoolean("dead") && !frame.getJSONObject("progress").has("hazard"), "The robot hit a hazard or fell");
        if (player.getBoolean("won")) {
            JSONObject witness = witness(task, trial);
            checkSource(task);
            if (trial.getBoolean("replay")) {
                var original = accepted(task).get(trial.getInt("slot") - 1);
                require(!original.getString("sessionId").equals(trial.getString("sessionId"))
                        && original.getJSONObject("witness").similar(witness), "Fresh replay differs from the original trial");
            }
            trial.put("witness", witness).put("status", "accepted");
            game.close();
        } else {
            require(tick < MAX_TICKS, "The bounded playtest timed out before beacon contact");
        }
    }

    private static JSONObject witness(JSONObject task, JSONObject trial) {
        JSONArray trace = trial.getJSONArray("trace");
        require(!trace.isEmpty() && trace.length() <= MAX_TICKS / STEP_TICKS, "Missing or oversized trace");
        for (int i = 0; i < trace.length(); i++) {
            JSONObject row = trace.getJSONObject(i);
            require(row.getLong("tick") == (i + 1) * STEP_TICKS
                    && row.getJSONObject("player").getString("id").equals(trial.getString("playerId"))
                    && row.getJSONObject("navigation").getString("pace").equals(trial.getString("pace")),
                    "Trace identity, pace or time changed");
        }
        JSONObject end = trace.getJSONObject(trace.length() - 1);
        JSONObject nav = end.getJSONObject("navigation");
        require(end.getJSONObject("player").getBoolean("won") && !end.getJSONObject("player").getBoolean("dead")
                && !end.getJSONObject("progress").has("hazard") && nav.getInt("rejectedMeasurements") == 0,
                "Native beacon and hazard checks failed");
        JSONObject remaining = WorldFrames.copy(task.getJSONObject("source").getJSONObject("rails"));
        JSONArray crossings = nav.getJSONArray("crossings");
        for (Object value : crossings) {
            JSONObject row = (JSONObject) value;
            String id = row.getString("id");
            require(row.getString("name").equals(remaining.optString(id))
                    && row.getLong("launchTick") < row.getLong("entryTick")
                    && row.getLong("entryTick") < row.getLong("exitTick")
                    && row.getLong("exitTick") <= end.getLong("tick")
                    && row.getInt("overlapSamples") >= 5
                    && Double.isFinite(row.getDouble("measuredClearance")) && row.getDouble("measuredClearance") > MARGIN
                    && Double.isFinite(row.getDouble("entrySpeed")) && row.getDouble("entrySpeed") > 0,
                    "Rail crossing identity, timing or clearance is invalid");
            remaining.remove(id);
        }
        require(remaining.isEmpty(), "Every original rail needs a verified crossing");
        return new JSONObject().put("finishTick", end.getLong("tick")).put("traceSha256", WorldFrames.hash(new JSONObject().put("trace",trace)))
                .put("crossings", crossings).put("launches", nav.getInt("launches"))
                .put("pickups", end.getJSONObject("progress").getJSONArray("pickups"));
    }

    private static JSONObject audit(JSONObject task) {
        List<JSONObject> rows = accepted(task);
        JSONArray paces = task.getJSONArray("paces");
        require(rows.size() == paces.length() * 2, "Both trials are required for each pace");
        var runs = new JSONArray();
        double minimum = Double.POSITIVE_INFINITY;
        int crossings = 0;
        for (int i = 0; i < paces.length(); i++) {
            JSONObject original = rows.get(i * 2), replay = rows.get(i * 2 + 1);
            String pace = paces.getString(i);
            require(original.getInt("slot") == i * 2 && replay.getInt("slot") == i * 2 + 1
                    && pace.equals(original.getString("pace")) && pace.equals(replay.getString("pace"))
                    && !original.getString("sessionId").equals(replay.getString("sessionId")), "Trial order or identity changed");
            JSONObject first = witness(task, original), second = witness(task, replay);
            require(first.similar(original.getJSONObject("witness")) && second.similar(replay.getJSONObject("witness"))
                    && first.similar(second), "Fresh replay does not reproduce the measured evidence");
            runs.put(first.put("pace", pace));
            for (Object value : first.getJSONArray("crossings")) {
                minimum = Math.min(minimum, ((JSONObject) value).getDouble("measuredClearance"));
                crossings++;
            }
        }
        return new JSONObject().put("kind", KIND).put("passed", true).put("title", task.getString("title"))
                .put("paceCount", paces.length()).put("crossings", crossings).put("minimumClearance", minimum)
                .put("independentReplays", paces.length()).put("runs", runs)
                .put("manifestSha256", task.getJSONObject("source").getString("manifestSha256"));
    }

    private void finish(JSONObject task) throws Exception {
        checkSource(task);
        JSONObject audit = audit(task);
        JSONObject report = store.writeOnce("relay-playtest:" + task.getString("id"), presentation(audit));
        task.put("audit", audit).put("report", new JSONObject().put("id", report.getString("id"))
                .put("version", report.getInt("version")))
                .put("status", "completed").put("stage", "verified").put("reason", summary(audit));
    }

    static String summary(JSONObject proof) {
        return String.format(Locale.ROOT, "%s completed at %d pace(s). %d rail crossings reproduced in fresh local runs. Minimum clearance %.3f game units.",
                proof.getString("title"), proof.getInt("paceCount"), proof.getInt("crossings"), proof.getDouble("minimumClearance"));
    }

    private static JSONObject presentation(JSONObject audit) {
        var rows = new StringBuilder();
        for (Object item : audit.getJSONArray("runs")) {
            JSONObject run = (JSONObject) item;
            for (Object raw : run.getJSONArray("crossings")) {
                JSONObject rail = (JSONObject) raw;
                rows.append(String.format(Locale.ROOT, "<tr><td>%s</td><td>%s</td><td>%.3f</td><td>%.3f</td><td>%d</td></tr>",
                        SpatialReport.escape(run.getString("pace")), SpatialReport.escape(rail.getString("name")),
                        rail.getDouble("entrySpeed"), rail.getDouble("measuredClearance"), run.getLong("finishTick")));
            }
        }
        String html = """
                <!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
                <title>Verified Relay playtest</title><style>
                *{box-sizing:border-box}body{margin:0;background:#111e27;color:#e9f6f2;font:16px/1.55 system-ui}
                main{max-width:860px;margin:auto;padding:clamp(18px,4vw,48px)}h1{line-height:1.1;color:#95ead3}
                table{text-align:left;width:100%%;border-collapse:collapse}td,th{padding:9px;border-bottom:1px solid #45615f}
                .scroll{overflow:auto}a{color:#95ead3}
                </style><main><h1>%s</h1><p id="outcome">%s</p>
                <p>Entry speed in game units per second. Clearance in game units.</p>
                <div class="scroll"><table><thead><tr><th>Pace</th><th>Rail</th><th>Entry speed</th><th>Clearance</th><th>Beacon tick</th></tr></thead>
                <tbody>%s</tbody></table></div><p><a href="playtest.json">Read the verified evidence</a></p></main></html>
                """.formatted(SpatialReport.escape(audit.getString("title")), SpatialReport.escape(summary(audit)), rows);
        return new JSONObject().put("title", audit.getString("title") + " · verified playtest")
                .put("files", new JSONObject().put("index.html", html).put("playtest.json", audit.toString(2)))
                .put("sources", List.of("relay-manifest:" + audit.getString("manifestSha256")));
    }

    synchronized JSONObject verify(String id) throws Exception {
        JSONObject task = load(id);
        require(task.getString("status").equals("completed"), "The Relay playtest is incomplete");
        checkSource(task);
        JSONObject proof = audit(task);
        require(proof.similar(task.getJSONObject("audit")), "The retained Relay audit changed");
        JSONObject ref = task.getJSONObject("report"), artifact = store.read(ref.getString("id"));
        JSONObject expected = presentation(proof);
        require(ref.getInt("version") == artifact.getInt("version")
                && artifact.getJSONObject("contents").similar(expected.getJSONObject("files"))
                && artifact.getJSONArray("sources").similar(expected.getJSONArray("sources")), "The saved Relay report changed");
        return view(task);
    }

    synchronized JSONObject read(String id) throws Exception { return view(load(id)); }

    synchronized JSONObject control(String id, String action) throws Exception {
        JSONObject task = load(id);
        String status = task.getString("status");
        switch (action) {
            case "resume" -> {
                require(status.equals("interrupted"), "Only interrupted Relay missions can resume");
                checkSource(task);
                task.put("status", "running").put("stage", "prepare").put("reason", "Repeating the unfinished trial");
            }
            case "interrupt" -> { if (status.equals("running")) interrupt(task, "The mission was interrupted"); }
            case "cancel" -> {
                if (status.equals("running")) interrupt(task, "Stopped by you");
                if (status.equals("running") || status.equals("interrupted")) task.put("status", "cancelled");
            }
            default -> throw new IllegalArgumentException("Unknown Relay mission action");
        }
        save(task);
        return view(task);
    }

    private void interrupt(JSONObject task, String reason) throws Exception {
        JSONObject trial = running(task);
        if (trial != null) trial.put("status", "interrupted").put("reason", reason);
        game.close();
        task.put("status", "interrupted").put("reason", reason);
        save(task);
    }

    private static JSONObject view(JSONObject task) {
        var copy = WorldFrames.copy(task);
        for (Object value : copy.getJSONArray("trials")) {
            JSONObject trial = (JSONObject) value;
            JSONArray trace = (JSONArray) trial.remove("trace");
            trial.put("inputChunks", trace.length());
        }
        return copy;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    @Override public synchronized void close() { game.close(); }
}
