package fraymus.workspace;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;

/** Keeps the goal decision separate from the player control and solver. */
final class CourierTargetEvidence {
    private static final int MAX_BYTES = 320_000;
    private static final int HOLD_TICKS = 60;
    private static final double TOLERANCE = .035;
    private static final double MAX_DRIFT = .018;
    private static final double REST_SPEED = .008;
    private static final double MOTION_FLOOR = .00025;
    private static final List<String> PHASES = List.of("SETTLE", "PUSH", "RETREAT", "HOLD");
    private final Path root;

    CourierTargetEvidence(Path project) { root = project.resolve("courier-target"); }

    static JSONObject request(JSONObject input) {
        SpatialScene.keys(input, "distance", "mass", "friction");
        double distance = SpatialScene.number(input, "distance", -1, 1);
        if (Math.abs(distance) < .30) throw new IllegalArgumentException("Choose a travel distance of 0.30–1.00.");
        double mass = SpatialScene.number(input, "mass", .35, 1.30);
        double friction = SpatialScene.number(input, "friction", .06, .75);
        // Retain the actual precision sent to the native world.
        return new JSONObject().put("distance", (float)distance).put("mass", (float)mass).put("friction", (float)friction);
    }

    synchronized JSONObject start(JSONObject input, EmbeddedGameClient game) throws Exception {
        var spec = request(input);
        var proof = check(game.testCourierTarget(spec));
        if (!proof.getJSONObject("native").getJSONObject("request").similar(spec))
            throw new IllegalStateException("Game Lab returned a different target setup.");
        String id = WorldFrames.hash(proof);
        Files.createDirectories(root);
        Path file = file(id);
        if (Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
            read(id);
        } else {
            SpriteWorkshop.replace(file, MapCheckpoint.canonical(proof).getBytes(StandardCharsets.UTF_8));
        }
        SpriteWorkshop.replace(root.resolve("latest.txt"), id.getBytes(StandardCharsets.US_ASCII));
        return proof.put("id", id);
    }

    synchronized JSONObject latest() throws Exception {
        var latest = root.resolve("latest.txt");
        if (!Files.exists(latest, LinkOption.NOFOLLOW_LINKS)) return new JSONObject();
        return read(new String(SpriteWorkshop.read(latest, 64), StandardCharsets.US_ASCII));
    }

    synchronized JSONObject read(String id) throws Exception {
        byte[] bytes = SpriteWorkshop.read(file(id), MAX_BYTES);
        if (!MapDefinition.sha(bytes).equals(id)) throw new IllegalStateException("The retained Courier target evidence changed.");
        var proof = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
        if (!proof.similar(check(proof.getJSONObject("native"))))
            throw new IllegalStateException("The saved target conclusions differ from the native trace.");
        return proof.put("id", id);
    }

    synchronized JSONObject replay(String id, EmbeddedGameClient game) throws Exception {
        var expected = read(id);
        game.openCourierCrate();
        var actual = check(game.testCourierTarget(expected.getJSONObject("native").getJSONObject("request")));
        if (!id.equals(WorldFrames.hash(actual)))
            throw new IllegalStateException("The new target run differs from the retained source or tick evidence.");
        return expected.put("replayed", true);
    }

    private Path file(String id) {
        if (!id.matches("[a-f0-9]{64}")) throw new IllegalArgumentException("Invalid Courier target evidence ID.");
        return root.resolve(id + ".json");
    }

    static JSONObject check(JSONObject data) {
        var spec = request(data.getJSONObject("request"));
        int direction = spec.getDouble("distance") > 0 ? 1 : -1;
        if (!"Game Lab / JBox2D".equals(data.getString("engine")) || data.getInt("tickHz") != 60
                || !"DYNAMIC".equals(data.getString("playerType")) || !data.getBoolean("playerFixedRotation")
                || !(data.getDouble("playerMass") > 0)
                || !"DYNAMIC".equals(data.getString("crateType")))
            throw new IllegalStateException("The goal requires the native dynamic Courier and crate.");
        for (String key : List.of("atlasSha256", "crateArtSha256", "targetArtSha256", "levelSha256", "manifestSha256"))
            if (!data.getString(key).matches("[a-f0-9]{64}")) throw new IllegalStateException("Missing target source identity.");
        java.util.UUID.fromString(data.getString("playerId"));
        if (Math.abs(data.getDouble("actualMass") - spec.getDouble("mass")) > .00001 ||
                Math.abs(data.getDouble("actualFriction") - spec.getDouble("friction")) > .00001)
            throw new IllegalStateException("The body does not match the requested mass and friction.");
        var rows = data.getJSONArray("rows");
        int count = rows.length();
        if (count < 100 || count > 721) throw new IllegalStateException("The measured target run has an invalid tick count.");

        double origin = rows.getJSONObject(0).getDouble("crateX");
        double target = data.getDouble("targetX");
        if (Math.abs(origin - 2.08) > .00001 ||
                Math.abs(target - origin - spec.getDouble("distance")) > .00001 ||
                direction * (origin - rows.getJSONObject(0).getDouble("playerX")) < .65)
            throw new IllegalStateException("The initial pose or target differs from the request.");
        int phase = 0, firstContact = -1, firstImpulse = -1, firstMotion = -1, begins = 0, holdStart = -1;
        double releaseX = 0, maxError = 0, maxDrift = 0, maxSpeed = 0;
        for (int n = 0; n < count; n++) {
            var row = rows.getJSONObject(n);
            int next = PHASES.indexOf(row.getString("phase"));
            int input = next == 1 ? direction : next == 2 ? -direction : 0;
            if (row.getInt("tick") != n || next < phase || next > phase + 1 ||
                    (next == 0) != (n <= 24) || row.getInt("input") != input)
                throw new IllegalStateException("The target trace has an invalid tick or player input.");
            int currentBegins = row.getInt("begins"), contacts = row.getInt("contacts");
            double impulse = row.getDouble("impulse"), x = row.getDouble("crateX");
            if (currentBegins < begins || contacts < 0 || contacts > currentBegins || impulse < 0)
                throw new IllegalStateException("The target trace has invalid pair contacts.");
            begins = currentBegins;
            if (begins > 0 && firstContact < 0) firstContact = n;
            if (impulse > 0 && firstImpulse < 0) firstImpulse = n;
            if (firstMotion < 0 && Math.abs(x - origin) > MOTION_FLOOR) firstMotion = n;
            if (next == 3) {
                if (phase == 2) {
                    holdStart = n;
                    releaseX = rows.getJSONObject(n - 1).getDouble("crateX");
                    maxError = Math.abs(releaseX - target);
                    for (int gap = n - 8; gap < n; gap++)
                        if (gap < 0 || rows.getJSONObject(gap).getInt("contacts") != 0)
                            throw new IllegalStateException("The player did not back away before release.");
                }
                if (contacts != 0 || impulse != 0 || currentBegins != rows.getJSONObject(holdStart - 1).getInt("begins"))
                    throw new IllegalStateException("The player contacted the crate after release.");
                maxError = Math.max(maxError, Math.abs(x - target));
                maxDrift = Math.max(maxDrift, Math.abs(x - releaseX));
                maxSpeed = Math.max(maxSpeed, Math.abs(row.getDouble("crateVx")));
            }
            phase = next;
        }
        var last = rows.getJSONObject(count - 1);
        if (firstContact <= 24 || firstImpulse < firstContact || firstMotion < firstImpulse ||
                !"PUSH".equals(rows.getJSONObject(firstContact).getString("phase")) ||
                Math.abs(data.getDouble("firstContactX") - origin) > MOTION_FLOOR)
            throw new IllegalStateException("No verified horizontal contact initiated the push.");
        if (holdStart < 0 || count - holdStart != HOLD_TICKS || maxError > TOLERANCE ||
                maxDrift > MAX_DRIFT || Math.abs(last.getDouble("crateVx")) > REST_SPEED)
            throw new IllegalStateException(String.format(java.util.Locale.ROOT,
                    "Target not verified at tick %d (%s). Crate X %.4f, target %.4f, hold error %.4f, drift %.4f.",
                    count - 1, last.getString("phase"), last.getDouble("crateX"), target, maxError, maxDrift));

        var metrics = new JSONObject().put("firstContactTick", firstContact).put("firstImpulseTick", firstImpulse)
                .put("firstMotionTick", firstMotion)
                .put("releaseTick", holdStart - 1).put("holdTicks", HOLD_TICKS).put("targetX", target)
                .put("releaseX", releaseX).put("finalX", last.getDouble("crateX"))
                .put("maxError", maxError).put("tolerance", TOLERANCE).put("maxDrift", maxDrift)
                .put("maxHoldSpeed", maxSpeed).put("finalSpeed", Math.abs(last.getDouble("crateVx")));
        return new JSONObject().put("format", "fraynix.courier-target-v1").put("passed", true)
                .put("measurements", metrics).put("native", data);
    }
}
