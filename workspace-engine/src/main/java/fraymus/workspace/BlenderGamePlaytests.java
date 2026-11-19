package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** A checked Blender candidate runs twice in isolated Java game sessions. */
final class BlenderGamePlaytests implements AutoCloseable {
    private static final int CHUNK_TICKS = 24;
    private static final int MAX_CHUNKS = 72;
    private static final String POLICY = "contact122";
    private final BlenderGameCandidates candidates;
    private final EmbeddedGameClient game;
    private final Path jars;
    private final java.util.concurrent.ExecutorService worker = Executors.newSingleThreadExecutor(job -> {
        var thread = new Thread(job, "blender-game-playtest");
        thread.setDaemon(true);
        return thread;
    });
    private volatile String active;
    private volatile boolean closed;
    private volatile boolean stopped;

    BlenderGamePlaytests(Path repo, Path project, BlenderGameCandidates candidates) {
        this.candidates = candidates;
        this.game = new EmbeddedGameClient(repo, project.resolve("blender-playtest-runtime"));
        jars = repo.resolve("build/install/Fraynix-Studio/lib");
    }

    synchronized JSONObject start(String id) throws Exception {
        var candidate = candidates.read(id);
        var task = load(candidate);
        if (active != null) {
            if (active.equals(id)) return view(task);
            throw new IllegalStateException("Wait for the running Blender playtest");
        }
        if (closed) throw new IllegalStateException("Game playtests are closing");
        if ("passed".equals(task.getString("status")) || "failed".equals(task.getString("status"))) return view(task);
        var attempts = task.getJSONArray("attempts");
        if (attempts.length() >= 3) throw new IllegalStateException("Playtest recovery limit reached");
        var attempt = new JSONObject().put("id", UUID.randomUUID().toString()).put("trials", new JSONArray())
                .put("engine", engine()).put("status", "running");
        attempts.put(attempt);
        task.put("status", "running").put("message", "Preparing the first isolated AI playtest.");
        stopped = false;
        active = id;
        try {
            save(candidate, task);
        } catch (Exception error) {
            active = null;
            throw error;
        }
        var initial = view(task).put("started", true);
        worker.execute(() -> run(candidate, task, attempt));
        return initial;
    }

    synchronized void stop(String id) {
        if (id.equals(active)) stopped = true;
    }

    JSONObject failure(String id) throws Exception {
        var candidate = candidates.read(id);
        var task = load(candidate);
        require("failed".equals(task.getString("status")), "A completed failed playtest is required");
        var attempts = task.getJSONArray("attempts");
        var attempt = attempts.getJSONObject(attempts.length() - 1);
        var trials = attempt.getJSONArray("trials");
        require("failed".equals(attempt.getString("status")) && trials.length() >= 1 && trials.length() <= 2
                && engine().similar(attempt.getJSONObject("engine")), "The original failed run or engine changed");
        for (int index = 0; index < trials.length(); index++)
            checkTrial(candidate, attempt, index, index != trials.length() - 1);
        return new JSONObject().put("candidate", candidate.binding()).put("attempt", attempt);
    }

    JSONObject state(String id) throws Exception {
        var candidate = candidates.read(id);
        return view(load(candidate));
    }

    JSONObject verify(String id) throws Exception {
        var candidate = candidates.read(id);
        var task = load(candidate);
        if (!"passed".equals(task.getString("status"))) throw new IllegalStateException("Two matching playtests must pass first");
        var attempts = task.getJSONArray("attempts");
        var attempt = attempts.getJSONObject(attempts.length() - 1);
        var proof = audit(candidate, attempt);
        if (!proof.similar(task.getJSONObject("proof"))) throw new IllegalStateException("Saved playtest proof changed");
        return task.put("layout", candidate.info());
    }

    private JSONObject load(BlenderGameCandidates.Candidate candidate) throws Exception {
        Path file = candidate.directory().resolve("playtest.json");
        if (!Files.exists(file)) return new JSONObject().put("candidate", candidate.binding())
                .put("status", "ready").put("message", "Ready for AI playtest.").put("attempts", new JSONArray());
        var task = AgentFiles042.loadChecked(file);
        if (!candidate.binding().similar(task.getJSONObject("candidate")))
            throw new IllegalStateException("Playtest source binding changed");
        if ("running".equals(task.getString("status")) && !candidate.info().getString("id").equals(active)) {
            var attempts = task.getJSONArray("attempts");
            attempts.getJSONObject(attempts.length() - 1).put("status", "interrupted");
            task.put("status", "interrupted").put("message", "The game stopped. Start again to run two fresh trials.");
        }
        return task;
    }

    private static void save(BlenderGameCandidates.Candidate candidate, JSONObject task) throws Exception {
        AgentFiles042.saveChecked(candidate.directory().resolve("playtest.json"), task);
    }

    private static JSONObject view(JSONObject task) {
        var result = new JSONObject().put("status", task.getString("status")).put("message", task.getString("message"))
                .put("candidateId", task.getJSONObject("candidate").getString("id"));
        if (task.has("proof")) result.put("proof", task.getJSONObject("proof"));
        return result;
    }

    private JSONObject engine() throws Exception {
        var files = new JSONObject();
        try (var list = Files.list(jars)) {
            for (Path jar : list.filter(p -> p.getFileName().toString().endsWith(".jar")).sorted().toList())
                files.put(jar.getFileName().toString(), GameLevels114.hash(jar));
        }
        if (files.isEmpty()) throw new IllegalStateException("The installed Java game is missing");
        return files;
    }

    private void run(BlenderGameCandidates.Candidate candidate, JSONObject task, JSONObject attempt) {
        try {
            for (int trial = 0; trial < 2; trial++) {
                if (!drive(candidate, task, attempt, trial)) {
                    attempt.put("status", "failed");
                    task.put("status", "failed").put("message", "The controller did not reach the beacon. The failed trace is saved.");
                    save(candidate, task);
                    return;
                }
            }
            if (stopped || closed) throw new InterruptedException("Playtest interrupted");
            var checked = candidates.read(candidate.info().getString("id"));
            var proof = audit(checked, attempt);
            attempt.put("status", "passed");
            task.put("status", "passed").put("proof", proof)
                    .put("message", "Both AI playtests reached the beacon with matching traces.");
            save(checked, task);
        } catch (Exception error) {
            attempt.put("status", "interrupted");
            task.put("status", "interrupted").put("message", String.valueOf(error.getMessage()));
            try { save(candidate, task); } catch (Exception failure) { System.err.println(failure); }
        } finally {
            game.close();
            active = null;
        }
    }

    private boolean drive(BlenderGameCandidates.Candidate original, JSONObject task, JSONObject attempt, int index)
            throws Exception {
        var candidate = candidates.read(original.info().getString("id"));
        require(candidate.binding().similar(original.binding()), "The candidate changed before opening");
        var frame = candidate.open(game);
        var state = frame.getJSONObject("state");
        JSONObject initial = sample(state);
        require(initial.getInt("tick") == 0 && !initial.getJSONObject("player").getBoolean("won")
                && !initial.getJSONObject("player").getBoolean("dead"), "Fresh native starting state required");
        String session = state.getString("sessionId");
        var trace = new JSONArray().put(initial);
        var trial = new JSONObject().put("session", session).put("trace", trace).put("status", "running");
        attempt.getJSONArray("trials").put(trial);
        double target = candidate.info().getDouble("finishX") + 1;
        for (int chunk = 1; chunk <= MAX_CHUNKS; chunk++) {
            if (closed || stopped) throw new InterruptedException("Playtest interrupted");
            frame = game.command(action(target));
            state = frame.getJSONObject("state");
            require(session.equals(state.getString("sessionId")) && frame.getString("levelId").equals(candidate.info().getString("id"))
                    && state.getString("contentId").equals(candidate.course().id()) && state.getInt("tick") == chunk * CHUNK_TICKS
                    && POLICY.equals(state.getJSONObject("controller").getString("policy"))
                    && state.getJSONObject("player").getString("id").equals(initial.getJSONObject("player").getString("id")),
                    "Native game source, controller or steps changed");
            var step = sample(state);
            trace.put(step);
            var player = step.getJSONObject("player");
            boolean won = player.getBoolean("won") && !player.getBoolean("dead");
            if (won || player.getBoolean("dead") || chunk == MAX_CHUNKS) {
                String picture = attempt.getString("id") + "-trial-" + (index + 1) + ".png";
                Path file = candidate.directory().resolve(picture);
                byte[] bytes = Base64.getDecoder().decode(frame.getString("pngBase64"));
                Files.write(file, bytes, StandardOpenOption.CREATE_NEW);
                trial.put("imageSha256", GameLevels114.hash(file)).put("status", won ? "won" : "failed");
                save(candidate, task);
                return won;
            }
            task.put("message", "AI playtest " + (index + 1) + " of 2 at tick " + state.getInt("tick") + ".");
            save(candidate, task);
        }
        throw new IllegalStateException("The native playtest did not stop at its limit");
    }

    private static JSONObject action(double target) {
        return new JSONObject().put("action", "ai-step").put("ticks", CHUNK_TICKS).put("targetX", target).put("policy", POLICY);
    }

    private static JSONObject sample(JSONObject state) {
        var out = new JSONObject().put("tick", state.getInt("tick")).put("player", state.getJSONObject("player"));
        if (state.has("courseProgress")) out.put("progress", state.getJSONObject("courseProgress"));
        return out;
    }

    private JSONObject audit(BlenderGameCandidates.Candidate candidate, JSONObject attempt) throws Exception {
        require(engine().similar(attempt.getJSONObject("engine")), "The installed Java engine changed");
        var trials = attempt.getJSONArray("trials");
        require(trials.length() == 2, "Two fresh trials are required");
        var sessions = new HashSet<String>();
        for (int i = 0; i < trials.length(); i++) {
            var trial = trials.getJSONObject(i);
            require(sessions.add(trial.getString("session")), "Distinct game sessions required");
            checkTrial(candidate, attempt, i, true);
        }
        var first = trials.getJSONObject(0).getJSONArray("trace");
        require(first.similar(trials.getJSONObject(1).getJSONArray("trace")), "Fresh playtest motion differs");
        return new JSONObject().put("passed", true).put("attemptId", attempt.getString("id"))
                .put("candidate", candidate.binding()).put("controller", action(candidate.info().getDouble("finishX") + 1))
                .put("trials", trials.length()).put("ticks", first.getJSONObject(first.length() - 1).getInt("tick"))
                .put("traceSha256", WorldFrames.hash(new JSONObject().put("trace", first)));
    }

    private static void checkTrial(BlenderGameCandidates.Candidate candidate, JSONObject attempt, int i, boolean won)
            throws Exception {
        var trial = attempt.getJSONArray("trials").getJSONObject(i);
        UUID.fromString(trial.getString("session"));
        require(trial.getString("status").equals(won ? "won" : "failed"), "Unexpected native trial status");
        var trace = trial.getJSONArray("trace");
        require(trace.length() >= 2 && trace.length() <= MAX_CHUNKS + 1, "Invalid native trace length");
        String id = trace.getJSONObject(0).getJSONObject("player").getString("id");
        for (int j = 0; j < trace.length(); j++) {
            var sample = trace.getJSONObject(j);
            var player = sample.getJSONObject("player");
            boolean last = j == trace.length() - 1;
            require(sample.getInt("tick") == j * CHUNK_TICKS && id.equals(player.getString("id"))
                    && (last && !won || !player.getBoolean("dead")) && player.getBoolean("won") == (last && won),
                    "Native trace continuity or beacon contact changed");
            if (last && !won) require(player.getBoolean("dead") || j == MAX_CHUNKS,
                    "Failed trial lacks death or an exhausted input budget");
            SpatialScene.number(player, "x", -200, 200);
            SpatialScene.number(player, "y", -200, 200);
        }
        String job = UUID.fromString(attempt.getString("id")).toString();
        Path image = candidate.directory().resolve(job + "-trial-" + (i + 1) + ".png");
        require(GameLevels114.hash(image).equals(trial.getString("imageSha256")), "Original finish screenshot changed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    @Override public void close() {
        closed = true;
        worker.shutdownNow();
        try { worker.awaitTermination(6, TimeUnit.SECONDS); }
        catch (InterruptedException error) { Thread.currentThread().interrupt(); }
        game.close();
    }
}
