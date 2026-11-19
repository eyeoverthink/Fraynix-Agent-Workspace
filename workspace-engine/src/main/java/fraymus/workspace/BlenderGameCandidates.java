package fraymus.workspace;

import fraymus.neural.RelayCourse;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

/** Builds a new native level from checked original components and bounded Blender translations. */
final class BlenderGameCandidates {
    private static final String TRANSFORM = "jade.Transform";
    private static final String GROUND = "components.Ground";
    private final Path repo;
    private final Path root;
    private final GameContentPack content;

    BlenderGameCandidates(Path repo, Path project) throws Exception {
        this.repo = repo;
        root = project.resolve("blender-game-candidates");
        Files.createDirectories(root);
        content = new GameContentPack(repo);
    }

    record Candidate(Path directory, RelayCourse course, JSONObject pack, JSONObject info) {
        JSONObject binding() {
            return new JSONObject().put("id", info.getString("id")).put("levelSha256", info.getString("levelSha256"))
                    .put("snapshotSha256", info.getString("snapshotSha256"))
                    .put("blendSha256", info.getString("blendSha256")).put("manifestSha256", info.getString("manifestSha256"));
        }

        JSONObject open(EmbeddedGameClient game) throws Exception {
            return game.openBlenderCandidate(directory.resolve("level.json"), info.getString("id"),
                    info.getString("levelSha256"), course, pack).put("candidate", info);
        }
    }

    private record Build(RelayCourse course, JSONObject pack, JSONArray level, JSONObject info) {}
    private record Cell(int x, int y) {}

    synchronized JSONObject retain(JSONObject input) throws Exception {
        try (var folders = Files.list(root)) {
            if (folders.filter(Files::isDirectory).filter(p -> !p.getFileName().toString().startsWith("."))
                    .count() >= 32) throw new IllegalStateException("Blender candidate history is full (32)");
        }
        var build = compile(input);
        String id = UUID.randomUUID().toString();
        Path staging = Files.createTempDirectory(root, ".pending-");
        var info = build.info().put("id", id);
        Path level = staging.resolve("level.json");
        Files.writeString(level, build.level().toString(2), StandardCharsets.UTF_8);
        info.put("levelSha256", GameLevels114.hash(level));
        AgentFiles042.saveChecked(staging.resolve("source.json"), new JSONObject().put("input", input).put("info", info));
        Files.move(staging, directory(id), StandardCopyOption.ATOMIC_MOVE);
        return read(id).info();
    }

    synchronized JSONArray list() throws Exception {
        var list = new JSONArray();
        try (var files = Files.list(root)) {
            for (Path path : files.filter(Files::isDirectory).filter(p -> !p.getFileName().toString().startsWith("."))
                    .sorted().toList()) {
                String id = path.getFileName().toString();
                try {
                    list.put(read(id).info());
                } catch (Exception error) {
                    list.put(new JSONObject().put("id", id).put("error", String.valueOf(error.getMessage())));
                }
            }
        }
        return list;
    }

    Candidate read(String id) throws Exception {
        Path folder = directory(id);
        var input = AgentFiles042.loadChecked(folder.resolve("source.json"));
        var build = compile(input.getJSONObject("input"));
        Path level = folder.resolve("level.json");
        var info = build.info().put("id", id).put("levelSha256", GameLevels114.hash(level));
        if (!info.similar(input.getJSONObject("info")) ||
                !build.level().toString(2).equals(GameLevels114.read(level, 2_000_000)))
            throw new IllegalStateException("The saved Blender level differs from its original inputs");
        return new Candidate(folder, build.course(), build.pack(), info);
    }

    private Path directory(String id) {
        if (!UUID.fromString(id).toString().equals(id)) throw new IllegalArgumentException("Choose a saved Blender candidate");
        return root.resolve(id);
    }

    private Build compile(JSONObject input) throws Exception {
        SpatialScene.keys(input, "snapshot", "moves", "blendSha256");
        String snapshot = input.getString("snapshot");
        String blendHash = input.getString("blendSha256");
        if (snapshot.length() > 1_800_000 || !blendHash.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("Invalid Blender source receipt");
        var packageData = new JSONObject(snapshot);
        var verified = SpatialRuntime040.verifyExport(packageData);
        var frame = packageData.getJSONObject("frame");
        var source = frame.getJSONObject("source");
        RelayCourse course = RelayCourse.find(source.getString("levelId"));
        if (course == null || !"game".equals(source.getString("kind")) || source.getBoolean("continuous")
                || !"legacy-game-unit".equals(source.getString("units")))
            throw new IllegalArgumentException("Export a paused original Relay course before editing in Blender");

        var pack = content.verify(course);
        String sourceBytes = GameLevels114.read(repo.resolve(course.level()), 2_000_000);
        if (!pack.getJSONObject("files").getString(course.level()).equals(MeasurementStore.hash(sourceBytes)))
            throw new IllegalStateException("The original course changed during import");
        var level = new JSONArray(sourceBytes);
        var ground = new HashMap<String, JSONObject>();
        for (Object raw : level) {
            var object = (JSONObject)raw;
            if (GameLevels114.prop(object, GROUND) != null) {
                var rigid = GameLevels114.prop(object, "physics2d.components.Rigidbody2D");
                if (rigid == null || !"Static".equals(rigid.getString("bodyType")) || rigid.getBoolean("isSensor")
                        || GameLevels114.prop(object, "physics2d.components.Box2DCollider") == null)
                    throw new IllegalStateException("Original ground component contract changed");
                ground.put(object.getString("persistentId"), GameLevels114.prop(object, TRANSFORM).getJSONObject("position"));
            }
        }
        var snapshotGround = new HashMap<String, JSONArray>();
        for (Object raw : frame.getJSONArray("entities")) {
            var entity = (JSONObject)raw;
            String id = entity.getString("id");
            if (ground.containsKey(id) && "quad".equals(entity.optString("shape")))
                snapshotGround.put(id, entity.getJSONArray("position"));
        }
        if (!snapshotGround.keySet().equals(ground.keySet())) throw new IllegalArgumentException("The exported ground differs from the original Relay course");
        for (var entry : ground.entrySet()) {
            var pos = entry.getValue();
            var seen = snapshotGround.get(entry.getKey());
            if (seen.getDouble(0) != pos.getDouble("x") || seen.getDouble(1) != pos.getDouble("y")
                    || seen.getDouble(2) != 0) throw new IllegalArgumentException("The exported ground position differs from the original");
        }

        var moves = input.getJSONArray("moves");
        if (moves.length() < 1 || moves.length() > 16) throw new IllegalArgumentException("Move one to sixteen source ground tiles");
        var lineage = new JSONArray();
        var seen = new HashSet<String>();
        double finish = pack.getDouble("finishX");
        for (Object raw : moves) {
            var move = (JSONObject)raw;
            SpatialScene.keys(move, "sourceId", "to");
            String id = move.getString("sourceId");
            if (!seen.add(id) || !ground.containsKey(id)) throw new IllegalArgumentException("Each move needs a unique original ground ID");
            var pos = ground.get(id);
            var to = move.getJSONArray("to");
            if (to.length() != 2 || !(to.get(0) instanceof Number x) || !(to.get(1) instanceof Number y))
                throw new IllegalArgumentException("Ground translations need numeric XY coordinates");
            double nextX = x.doubleValue(), nextY = y.doubleValue();
            double fromX = pos.getDouble("x"), fromY = pos.getDouble("y");
            checkEditable(fromX, fromY, finish);
            checkEditable(nextX, nextY, finish);
            Cell from = cell(fromX, fromY), target = cell(nextX, nextY);
            if (from.equals(target)) throw new IllegalArgumentException("A moved tile must occupy a different grid cell");
            // Snap within the accepted tolerance before writing the original tile's transform.
            nextX = target.x() * .25 + .125;
            nextY = target.y() * .25 + .125;
            lineage.put(new JSONObject().put("sourceId", id).put("from", new JSONArray(new double[]{fromX, fromY}))
                    .put("to", new JSONArray(new double[]{nextX, nextY})));
            pos.put("x", nextX).put("y", nextY);
        }
        var occupied = new HashSet<Cell>();
        for (var pos : ground.values()) {
            if (!occupied.add(cell(pos.getDouble("x"), pos.getDouble("y"))))
                throw new IllegalArgumentException("Two ground tiles would occupy the same grid cell");
        }
        var info = new JSONObject().put("courseId", course.id()).put("title", course.title() + " · Blender candidate")
                .put("manifestSha256", pack.getString("manifestSha256")).put("finishX", finish)
                .put("snapshotSha256", verified.getString("snapshotHash")).put("blendSha256", blendHash).put("moves", lineage);
        return new Build(course, pack, level, info);
    }

    private static void checkEditable(double x, double y, double finish) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || x < 1.5 || x >= finish - .75 || y < .12499 || y > 2.12501)
            throw new IllegalArgumentException("Ground edits must stay inside the course, clear of the start and finish");
    }

    private static Cell cell(double x, double y) {
        int ix = (int)Math.round((x - .125) / .25), iy = (int)Math.round((y - .125) / .25);
        if (Math.abs(x - (ix * .25 + .125)) > 1e-5 || Math.abs(y - (iy * .25 + .125)) > 1e-5)
            throw new IllegalArgumentException("Align ground centres to the original quarter-unit grid");
        return new Cell(ix, iy);
    }
}
