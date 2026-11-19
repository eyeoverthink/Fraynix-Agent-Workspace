package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/** A durable Game Lab procedure bound to a selected Blender candidate and actual failed motion. */
final class BlenderGapRepair {
    static final String KIND = "blender-gap-repair-v1";
    static final String TITLE = "Verified Blender gap repair";
    static final String CHECK = "blenderRepair.check";
    static final String DISPLAY = "blenderRepairDisplay";
    private static final double TILE = .25;
    private static final int MAX_EDITS = 8;
    private static final int STILL_SAMPLES = 8;
    private static final Pattern REQUEST = Pattern.compile(
            "^Diagnose and repair Blender candidate ([0-9a-f-]{36}), then playtest and save the evidence[.!]?(?: Pause after repair[.!]?)?$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern LESSON_REQUEST = Pattern.compile(
            "^Use Blender repair lesson (" + BlenderRepairLessons.NAME_PATTERN
                    + ") on candidate ([0-9a-f-]{36}), then playtest and save the evidence[.!]?$",
            Pattern.CASE_INSENSITIVE);

    private final Path root;
    private final BlenderGameCandidates candidates;
    private final BlenderGamePlaytests playtests;
    private final WorkspaceStore store;
    private final BlenderRepairLessons lessons;

    BlenderGapRepair(Path project, BlenderGameCandidates candidates, BlenderGamePlaytests playtests, WorkspaceStore store)
            throws Exception {
        root = project.resolve("blender-gap-repairs");
        this.candidates = candidates;
        this.playtests = playtests;
        this.store = store;
        lessons = new BlenderRepairLessons(project);
        Files.createDirectories(root);
        try (var files = Files.list(root)) {
            for (Path file : files.filter(p -> p.getFileName().toString().endsWith(".json")).toList()) {
                var task = AgentFiles042.loadChecked(file);
                if (!file.equals(path(task.getString("id")))) throw new IllegalStateException("Gap repair task ID changed");
                if ("running".equals(task.getString("status"))) {
                    task.put("status", "interrupted").put("reason", "Resume the retained Blender repair.");
                    task.remove("ownedTrial");
                    save(task);
                }
            }
        }
    }

    static boolean requested(String goal) {
        String text = goal.strip();
        return REQUEST.matcher(text).matches() || LESSON_REQUEST.matcher(text).matches();
    }

    synchronized JSONObject teach(String id, String name) throws Exception {
        return lessons.save(name, verify(id));
    }

    synchronized JSONArray lessons() throws Exception { return lessons.list(); }

    synchronized boolean owns(String id) {
        return Files.isRegularFile(path(id));
    }

    synchronized JSONObject start(JSONObject args) throws Exception {
        SpatialScene.keys(args, "id", "request");
        String id = args.getString("id"), request = args.getString("request").strip();
        var lessonMatch = LESSON_REQUEST.matcher(request);
        boolean reuse = lessonMatch.matches();
        var direct = REQUEST.matcher(request);
        if (!reuse && !direct.matches()) throw new IllegalArgumentException("Choose a Blender candidate to diagnose and repair");
        if (owns(id)) {
            var prior = load(id);
            require(prior.getString("request").equals(request), "The original Blender repair request changed");
            return view(prior);
        }
        var parent = candidates.read((reuse ? lessonMatch.group(2) : direct.group(1)).toLowerCase(Locale.ROOT));
        require(!parent.info().has("repair"), "Select an original Blender import for one bounded repair");
        var task = new JSONObject().put("id", id).put("procedure", KIND).put("request", request)
                .put("parent", parent.binding()).put("candidateId", derivedId(id))
                .put("pauseAfterRepair", request.toLowerCase(Locale.ROOT).contains("pause after repair"))
                .put("status", "running").put("stage", "baseline").put("reason", "Checking the original Blender candidate.");
        if (reuse) {
            var lesson = checkedLesson(lessonMatch.group(1).toLowerCase(Locale.ROOT));
            BlenderRepairLessons.checkTarget(lesson, parent.binding());
            task.put("lesson", lesson);
        }
        save(task);
        return view(task);
    }

    synchronized JSONObject advance(String id) throws Exception {
        var task = load(id);
        if (!"running".equals(task.getString("status"))) return view(task);
        var parent = parent(task);
        switch (task.getString("stage")) {
            case "baseline" -> {
                var state = poll(task, parent.info().getString("id"));
                if ("passed".equals(state)) throw new IllegalStateException("The original candidate already passed its playtests");
                if ("failed".equals(state)) {
                    task.remove("ownedTrial");
                    stage(task, "diagnose", "Diagnosing the saved failed motion.");
                }
            }
            case "diagnose" -> {
                var failure = playtests.failure(parent.info().getString("id"));
                var repair = diagnose(parent, failure);
                checkLessonPlan(task, repair);
                task.put("diagnosis", repair).put("failureSha256", WorldFrames.hash(failure));
                stage(task, "repair", "The player stalled before an unsupported span. Preparing one bounded repair.");
            }
            case "repair" -> {
                var plan = checkedPlan(task, parent);
                var repaired = candidates.retain(task.getString("candidateId"), repairedInput(parent, plan));
                task.put("repaired", repaired);
                stage(task, "playtest", "Repaired " + plan.getJSONArray("edits").length() + " tiles. Ready for two fresh playtests.");
                if (task.getBoolean("pauseAfterRepair")) {
                    task.put("status", "interrupted").put("reason", "Repair saved. Resume to start the two fresh playtests.");
                }
            }
            case "playtest" -> {
                String repairedId = task.getString("candidateId");
                if (!task.has("ownedTrial")) checkedCandidate(task, parent, checkedPlan(task, parent));
                var state = poll(task, repairedId);
                if ("failed".equals(state)) throw new IllegalStateException("The one repaired candidate failed. Its actual trace was retained.");
                if ("passed".equals(state)) {
                    task.remove("ownedTrial");
                    stage(task, "save", "Two fresh wins recorded. Rechecking the sources and saving the evidence.");
                }
            }
            case "save" -> {
                var evidence = assembled(task, parent);
                var artifact = store.writeOnce("blender-gap-repair:" + id, presentation(evidence));
                task.put("report", new JSONObject().put("id", artifact.getString("id")).put("version", artifact.getInt("version")))
                        .put("audit", evidence.getJSONObject("audit")).put("status", "completed");
                stage(task, "verified", summary(task.getJSONObject("audit")));
            }
            default -> throw new IllegalStateException("Unknown Blender repair stage");
        }
        save(task);
        return view(task);
    }

    synchronized JSONObject control(String id, String action) throws Exception {
        var task = load(id);
        switch (action) {
            case "resume" -> {
                require("interrupted".equals(task.getString("status")), "Only interrupted repairs can resume");
                parent(task);
                if (task.has("lesson")) verifyLessonBinding(task);
                task.put("status", "running").put("reason", "Continuing the retained Blender repair.");
            }
            case "interrupt", "cancel" -> {
                if (task.has("ownedTrial")) playtests.stop(task.getString("ownedTrial"));
                task.remove("ownedTrial");
                if ("running".equals(task.getString("status")) || "interrupted".equals(task.getString("status")))
                    task.put("status", "cancel".equals(action) ? "cancelled" : "interrupted");
            }
            default -> throw new IllegalArgumentException("Unknown Blender repair control");
        }
        save(task);
        return view(task);
    }

    synchronized JSONObject read(String id) throws Exception { return view(load(id)); }

    synchronized JSONObject verify(String id) throws Exception {
        var task = load(id);
        require("completed".equals(task.getString("status")), "Blender repair is incomplete");
        var evidence = assembled(task, parent(task));
        require(evidence.getJSONObject("audit").similar(task.getJSONObject("audit")), "The saved repair audit changed");
        var ref = task.getJSONObject("report");
        var artifact = store.read(ref.getString("id"));
        var expected = presentation(evidence);
        require(ref.getInt("version") == artifact.getInt("version")
                && artifact.getJSONObject("contents").similar(expected.getJSONObject("files"))
                && artifact.getJSONArray("sources").similar(expected.getJSONArray("sources")), "The saved repair report changed");
        if (task.has("display")) checkDisplay(task, artifact, task.getJSONObject("display"));
        return view(task);
    }

    synchronized JSONObject acceptDisplay(String id, JSONObject returned) throws Exception {
        var task = verify(id);
        var artifact = store.read(task.getJSONObject("report").getString("id"));
        var stored = artifact.optJSONObject("test");
        require(stored != null && stored.similar(returned), "The returned Blender report receipt differs");
        checkDisplay(task, artifact, stored);
        task.put("display", stored);
        save(task);
        return stored;
    }

    private void checkDisplay(JSONObject task, JSONObject artifact, JSONObject bound) throws Exception {
        var test = artifact.optJSONObject("test");
        var report = task.getJSONObject("report");
        var steps = displayChecks(task);
        require(RampMissionReport.verifiedReceipt(report, test, steps)
                && test.getJSONArray("checks").similar(bound.getJSONArray("checks")), "Original Blender report checks changed");
        store.verifyRecordedTest(new JSONObject().put("id", report.getString("id")).put("version", report.getInt("version"))
                .put("steps", steps).put("checks", test.getJSONArray("checks")));
    }

    static JSONArray displayChecks(JSONObject task) {
        var steps = new JSONArray().put(new JSONObject().put("selector", "h1").put("expectText", TITLE))
                .put(new JSONObject().put("selector", "#outcome").put("expectText", summary(task.getJSONObject("audit"))))
                .put(new JSONObject().put("selector", "a[href=\"repair-evidence.json\"]").put("expectCount", 1));
        if (task.has("lesson")) steps.put(new JSONObject().put("selector", "#lesson")
                .put("expectText", lessonDisplay(task.getJSONObject("lesson"))));
        return steps;
    }

    private JSONObject checkedLesson(String name) throws Exception {
        var lesson = lessons.read(name);
        String teacherId = lesson.getString("teacherTaskId");
        require(!load(teacherId).has("lesson"), "The lesson teacher must be an original repair");
        require(lesson.similar(BlenderRepairLessons.example(name, verify(teacherId))), "The original teaching evidence changed");
        return lesson;
    }

    private JSONObject verifyLessonBinding(JSONObject task) throws Exception {
        var original = task.getJSONObject("lesson");
        var current = checkedLesson(original.getString("name"));
        require(current.similar(original), "The lesson selected for this task changed");
        BlenderRepairLessons.checkTarget(current, task.getJSONObject("parent"));
        return current;
    }

    private void checkLessonPlan(JSONObject task, JSONObject plan) throws Exception {
        if (task.has("lesson")) BlenderRepairLessons.checkPlan(verifyLessonBinding(task), plan);
    }

    private String poll(JSONObject task, String id) throws Exception {
        var state = playtests.state(id);
        if ("ready".equals(state.getString("status")) || "interrupted".equals(state.getString("status"))) {
            if (id.equals(task.optString("ownedTrial"))) throw new IllegalStateException(state.getString("message"));
            state = playtests.start(id);
            if (state.optBoolean("started")) task.put("ownedTrial", id);
        }
        task.put("reason", ("baseline".equals(task.getString("stage")) ? "Original candidate. " : "Repaired candidate. ")
                + state.getString("message"));
        return state.getString("status");
    }

    private JSONObject checkedPlan(JSONObject task, BlenderGameCandidates.Candidate parent) throws Exception {
        var failure = playtests.failure(parent.info().getString("id"));
        require(WorldFrames.hash(failure).equals(task.getString("failureSha256")), "Original failed motion changed");
        var plan = diagnose(parent, failure);
        checkLessonPlan(task, plan);
        require(plan.similar(task.getJSONObject("diagnosis")), "Original diagnosis or bounded repair changed");
        return plan;
    }

    private JSONObject repairedInput(BlenderGameCandidates.Candidate parent, JSONObject plan) throws Exception {
        var input = candidates.input(parent);
        var targets = new HashMap<String, JSONArray>();
        for (Object item : plan.getJSONArray("edits")) {
            var edit = (JSONObject)item;
            targets.put(edit.getString("sourceId"), edit.getJSONArray("to"));
        }
        for (Object raw : input.getJSONArray("moves")) {
            var move = (JSONObject)raw;
            var target = targets.get(move.getString("sourceId"));
            if (target != null) move.put("to", target);
        }
        return input.put("repair", new JSONObject().put("parentId", parent.info().getString("id"))
                .put("planSha256", WorldFrames.hash(plan)));
    }

    private BlenderGameCandidates.Candidate checkedCandidate(JSONObject task, BlenderGameCandidates.Candidate parent,
                                                             JSONObject plan) throws Exception {
        var repaired = candidates.read(task.getString("candidateId"));
        require(repaired.info().similar(task.getJSONObject("repaired"))
                && candidates.input(repaired).similar(repairedInput(parent, plan)), "The derived level no longer matches the bounded repair");
        return repaired;
    }

    private JSONObject assembled(JSONObject task, BlenderGameCandidates.Candidate parent) throws Exception {
        var plan = checkedPlan(task, parent);
        var repaired = checkedCandidate(task, parent, plan);
        var checked = playtests.verify(repaired.info().getString("id"));
        var verdict = checked.getJSONObject("proof");
        var audit = new JSONObject().put("kind", KIND).put("passed", true).put("title", TITLE)
                .put("parent", parent.binding()).put("candidate", repaired.binding()).put("diagnosis", plan)
                .put("failureSha256", task.getString("failureSha256")).put("playtest", verdict);
        if (task.has("lesson")) audit.put("lesson", task.getJSONObject("lesson"));
        return new JSONObject().put("audit", audit).put("failed", playtests.failure(parent.info().getString("id")))
                .put("repaired", checked);
    }

    private static JSONObject diagnose(BlenderGameCandidates.Candidate parent, JSONObject failure) throws Exception {
        var trials = failure.getJSONObject("attempt").getJSONArray("trials");
        var trace = trials.getJSONObject(trials.length() - 1).getJSONArray("trace");
        var last = trace.getJSONObject(trace.length() - 1);
        var player = last.getJSONObject("player");
        double x = player.getDouble("x"), y = player.getDouble("y");
        require(!player.getBoolean("dead") && trace.length() >= STILL_SAMPLES, "Repair requires an observed grounded stall");
        for (int i = trace.length() - STILL_SAMPLES; i < trace.length(); i++) {
            var pose = trace.getJSONObject(i).getJSONObject("player");
            require(pose.getBoolean("onGround") && Math.abs(pose.getDouble("x") - x) <= .025
                    && Math.abs(pose.getDouble("vx")) <= .025 && Math.abs(pose.getDouble("y") - y) <= .025,
                    "The saved trace does not show a stable grounded stall");
        }
        int column = (int)Math.floor(x / TILE);
        double deck = -Double.MAX_VALUE;
        var ground = new JSONArray(GameLevels114.read(parent.directory().resolve("level.json"), 2_000_000));
        var tiles = new JSONArray();
        for (Object raw : ground) {
            var object = (JSONObject)raw;
            if (GameLevels114.prop(object, "components.Ground") == null) continue;
            var at = GameLevels114.prop(object, "jade.Transform").getJSONObject("position");
            tiles.put(at);
            if ((int)Math.floor(at.getDouble("x") / TILE) == column && at.getDouble("y") <= y - TILE/2)
                deck = Math.max(deck, at.getDouble("y"));
        }
        require(y - deck >= .18 && y - deck <= .35, "No source deck immediately below the stall");
        var occupied = new HashSet<Integer>();
        for (Object raw : tiles) {
            var at = (JSONObject)raw;
            if (at.getDouble("y") == deck) occupied.add((int)Math.floor(at.getDouble("x") / TILE));
        }
        int first = column + 1, end = first;
        while (end - first <= MAX_EDITS + 2 && !occupied.contains(end)) end++;
        require(end - first >= 3 && end - first <= MAX_EDITS + 2, "No bounded unsupported span next to the stall");

        var edits = new JSONArray();
        int remaining = 0, maxRemaining = 0;
        for (int c = first; c < end; c++) {
            double center = c * TILE + TILE/2;
            JSONObject lowest = null;
            for (Object item : parent.info().getJSONArray("moves")) {
                var move = (JSONObject)item;
                var from = move.getJSONArray("from");
                var to = move.getJSONArray("to");
                if (from.getDouble(0) == center && from.getDouble(1) <= deck && to.getDouble(0) == center
                        && to.getDouble(1) >= deck + .75 && (lowest == null
                        || to.getDouble(1) < lowest.getJSONArray("to").getDouble(1))) lowest = move;
            }
            if (lowest != null) {
                edits.put(new JSONObject().put("sourceId", lowest.getString("sourceId")).put("from", lowest.getJSONArray("to"))
                        .put("to", new JSONArray(new double[]{center, deck})));
                remaining = 0;
            } else {
                maxRemaining = Math.max(maxRemaining, ++remaining);
            }
        }
        require(edits.length() >= 1 && edits.length() <= MAX_EDITS && maxRemaining <= 2,
                "The observed gap needs more repair than this request allows");
        return new JSONObject().put("kind", "stationary-before-missing-support").put("x", x).put("y", y)
                .put("lastTick", last.getInt("tick")).put("stillSamples", STILL_SAMPLES)
                .put("supportY", deck).put("gapStartX", first * TILE).put("gapEndX", end * TILE)
                .put("originalSpan", (end - first) * TILE).put("remainingSpan", maxRemaining * TILE)
                .put("budget", MAX_EDITS).put("edits", edits);
    }

    static String summary(JSONObject audit) {
        var plan = audit.getJSONObject("diagnosis");
        return String.format(Locale.ROOT,
                "Diagnosed a stall at X %.3f, repaired %d ground tiles, and saved two matching beacon runs at tick %d.",
                plan.getDouble("x"), plan.getJSONArray("edits").length(), audit.getJSONObject("playtest").getInt("ticks"));
    }

    private static JSONObject presentation(JSONObject evidence) {
        var audit = evidence.getJSONObject("audit");
        var plan = audit.getJSONObject("diagnosis");
        String html = """
                <!doctype html><html lang="en"><meta charset="utf-8">
                <meta name="viewport" content="width=device-width,initial-scale=1">
                <title>%s</title><style>
                body{margin:0;background:#12212a;color:#edf7f4;font:16px/1.6 system-ui}
                main{max-width:760px;margin:auto;padding:clamp(18px,5vw,60px)}h1,a{color:#96e9d0}
                dt{font-weight:650}dd{margin:0 0 12px}
                </style><main><h1>%s</h1><p id="outcome">%s</p>
                <dl><dt>Original unsupported span</dt><dd id="before">%.2f game units</dd>
                <dt>Largest remaining gap in that span</dt><dd id="after">%.2f game units</dd>
                <dt>Original stopped at</dt><dd>Tick %d</dd></dl>
                <p><a href="repair-evidence.json">Download native evidence</a></p></main></html>
                """.formatted(TITLE, TITLE, summary(audit), plan.getDouble("originalSpan"), plan.getDouble("remainingSpan"),
                plan.getInt("lastTick"));
        var sources = new JSONArray().put("blender-level:" + audit.getJSONObject("parent").getString("levelSha256"))
                .put("failed-trace:" + audit.getString("failureSha256"))
                .put("repaired-level:" + audit.getJSONObject("candidate").getString("levelSha256"));
        if (audit.has("lesson")) {
            var lesson = audit.getJSONObject("lesson");
            html = html.replace("<dl>", "<p id=\"lesson\">" + lessonDisplay(lesson) + "</p><dl>");
            sources.put("verified-blender-lesson:" + WorldFrames.hash(lesson));
        }
        return new JSONObject().put("title", TITLE)
                .put("files", new JSONObject().put("index.html", html).put("repair-evidence.json", evidence.toString(2)))
                .put("sources", sources);
    }

    private static String lessonDisplay(JSONObject lesson) {
        return "Lesson: " + lesson.getString("name") + ". Checked against this level's own failed motion and fresh playtests.";
    }

    private BlenderGameCandidates.Candidate parent(JSONObject task) throws Exception {
        var binding = task.getJSONObject("parent");
        var candidate = candidates.read(binding.getString("id"));
        require(candidate.binding().similar(binding), "The original Blender candidate changed");
        return candidate;
    }

    private Path path(String id) {
        if (!UUID.fromString(id).toString().equals(id)) throw new IllegalArgumentException("Canonical repair task ID required");
        return root.resolve(id + ".json");
    }

    private JSONObject load(String id) throws Exception {
        var task = AgentFiles042.loadChecked(path(id));
        require(task.getString("id").equals(id) && KIND.equals(task.getString("procedure")), "Blender task identity changed");
        return task;
    }

    private void save(JSONObject task) throws Exception { AgentFiles042.saveChecked(path(task.getString("id")), task); }

    private static String derivedId(String id) {
        return UUID.nameUUIDFromBytes(("blender-gap-repair:" + id).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static void stage(JSONObject task, String name, String reason) {
        task.put("stage", name).put("reason", reason);
    }

    private static JSONObject view(JSONObject task) {
        var copy = WorldFrames.copy(task);
        copy.remove("ownedTrial");
        return copy;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
