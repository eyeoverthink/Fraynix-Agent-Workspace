package fraymus.workspace;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Recomputes the route, interruption and released outcomes from native sample rows. */
final class CourierRecoveryEvidence {
    private static final double POSITION_TOLERANCE = .035;
    private static final double REST_SPEED = .008;
    private static final int HOLD_TICKS = 60;
    private final Path root;

    CourierRecoveryEvidence(Path project) { root = project.resolve("courier-recovery"); }

    static JSONObject setup(JSONObject raw) {
        SpatialScene.keys(raw, "direction", "supportFriction", "obstacle", "disturbance");
        double direction = SpatialScene.number(raw, "direction", -1, 1);
        double friction = SpatialScene.number(raw, "supportFriction", .065, .32);
        if (Math.abs(direction) != 1 || (friction != .065 && friction != .32) ||
                !(raw.get("obstacle") instanceof Boolean) || !(raw.get("disturbance") instanceof Boolean))
            throw new IllegalArgumentException("Choose left or right, one support surface and two fixture settings.");
        return new JSONObject().put("direction", (int)direction).put("supportFriction", friction)
                .put("obstacle", raw.getBoolean("obstacle")).put("disturbance", raw.getBoolean("disturbance"));
    }

    synchronized JSONObject run(JSONObject input, EmbeddedGameClient game) throws Exception {
        var requested = setup(input);
        var proof = check(game.testCourierRecovery(requested));
        if (!requested.similar(proof.getJSONObject("native").getJSONObject("setup")))
            throw new IllegalStateException("The executed setup differs from the chosen fixture.");
        String id = WorldFrames.hash(proof);
        Files.createDirectories(root);
        Path path = file(id);
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) read(id);
        else SpriteWorkshop.replace(path, MapCheckpoint.canonical(proof).getBytes(StandardCharsets.UTF_8));
        SpriteWorkshop.replace(root.resolve("latest.txt"), id.getBytes(StandardCharsets.US_ASCII));
        return proof.put("id", id);
    }

    synchronized JSONObject latest() throws Exception {
        Path entry = root.resolve("latest.txt");
        if (!Files.exists(entry, LinkOption.NOFOLLOW_LINKS)) return new JSONObject();
        return read(new String(SpriteWorkshop.read(entry, 64), StandardCharsets.US_ASCII));
    }

    synchronized JSONObject read(String id) throws Exception {
        byte[] bytes = SpriteWorkshop.read(file(id), 500_000);
        if (!id.equals(MapDefinition.sha(bytes)))
            throw new IllegalStateException("The saved Courier recovery evidence changed.");
        var receipt = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
        if (!receipt.similar(check(receipt.getJSONObject("native"))))
            throw new IllegalStateException("The saved comparison differs from the native measurements.");
        return receipt.put("id", id);
    }

    synchronized JSONObject replay(String id, EmbeddedGameClient game) throws Exception {
        var original = read(id);
        game.openCourierCrate();
        var fresh = check(game.testCourierRecovery(original.getJSONObject("native").getJSONObject("setup")));
        if (!id.equals(WorldFrames.hash(fresh)))
            throw new IllegalStateException("The fresh run did not reproduce the retained comparison.");
        return original.put("replayed", true);
    }

    private Path file(String id) {
        if (!id.matches("[a-f0-9]{64}")) throw new IllegalArgumentException("Invalid Courier recovery ID.");
        return root.resolve(id + ".json");
    }

    static JSONObject check(JSONObject data) {
        var setup = setup(data.getJSONObject("setup"));
        if (!"Game Lab / JBox2D".equals(data.getString("engine")) || data.getInt("tickHz") != 60)
            throw new IllegalStateException("Native Game Lab traces are required.");
        for (String key : List.of("atlasSha256", "crateArtSha256", "targetArtSha256", "levelSha256", "manifestSha256"))
            if (!data.getString(key).matches("[a-f0-9]{64}")) throw new IllegalStateException("Missing source identity.");
        var base = data.getJSONObject("baseline");
        var adaptive = data.getJSONObject("adaptive");
        if (!base.getString("playerId").equals(adaptive.getString("playerId")) ||
                base.getDouble("playerMass") != adaptive.getDouble("playerMass") ||
                !base.getJSONArray("rows").getJSONObject(0).similar(adaptive.getJSONArray("rows").getJSONObject(0)))
            throw new IllegalStateException("The paired controllers require the same starting world.");
        double target = data.getDouble("targetX");
        if (Math.abs(target - (2.08 + setup.getInt("direction") * .77)) > .00001)
            throw new IllegalStateException("The authored delivery target changed.");
        var slow = checkRun(base, setup, target, false);
        var recovery = checkRun(adaptive, setup, target, true);
        return new JSONObject().put("format", "fraynix.courier-recovery-v1").put("passed", true)
                .put("measurements", new JSONObject().put("baseline", slow).put("adaptive", recovery))
                .put("native", data);
    }

    private static JSONObject checkRun(JSONObject run, JSONObject setup, double target, boolean adaptive) {
        int direction = setup.getInt("direction");
        boolean obstacle = setup.getBoolean("obstacle"), disturbance = setup.getBoolean("disturbance");
        if (!"DYNAMIC".equals(run.getString("playerType")) || !run.getBoolean("playerFixedRotation") ||
                !(run.getDouble("playerMass") > 0) || !"DYNAMIC".equals(run.getString("crateType")) ||
                Math.abs(run.getDouble("mass") - .78) > .00001 || Math.abs(run.getDouble("friction") - .28) > .00001 ||
                run.getBoolean("obstacle") != obstacle ||
                !(obstacle ? "STATIC" : "none").equals(run.getString("obstacleType")) ||
                Math.abs(run.getDouble("supportFriction") - setup.getDouble("supportFriction")) > .00001)
            throw new IllegalStateException("Observed support or dynamic bodies differ from the fixture.");
        java.util.UUID.fromString(run.getString("playerId"));
        var rows = run.getJSONArray("rows");
        int end = rows.length() - 1;
        if (end < 100 || end > 1050) throw new IllegalStateException("Invalid bounded delivery length.");
        var start = rows.getJSONObject(0);
        double origin = start.getDouble("crateX");
        if (Math.abs(origin - 2.08) > .00001 ||
                Math.abs(start.getDouble("playerX") - (origin - direction * (obstacle ? 1.27 : .73))) > .00001)
            throw new IllegalStateException("The measured start does not match the requested side.");

        var sequence = new ArrayList<String>(List.of("SETTLE"));
        if (obstacle) sequence.add("APPROACH");
        sequence.add("PUSH");
        if (disturbance) sequence.addAll(List.of("INTERRUPT", "OBSERVE"));
        if (disturbance && adaptive) sequence.add("RECOVER");
        boolean expectedHold = adaptive || !disturbance;
        if (expectedHold) sequence.addAll(List.of("RETREAT", "HOLD"));
        int stage = 0, contact = -1, impulse = -1, motion = -1, begins = 0, observe = -1;
        int quiet = 0, restore = -1, release = -1, recontact = -1, crossing = -1, jumps = 0;
        double maxError = 0, drift = 0, releaseX = 0;
        double barrierX = run.getDouble("obstacleX");
        if (obstacle && (Math.abs(barrierX - (origin - direction * .70)) > .00001 ||
                Math.abs(run.getDouble("obstacleHeight") - .13) > .00001))
            throw new IllegalStateException("The native obstacle geometry changed.");
        for (int tick = 0; tick <= end; tick++) {
            var row = rows.getJSONObject(tick);
            String phase = row.getString("phase");
            boolean changed = !sequence.get(stage).equals(phase);
            if (changed && (++stage == sequence.size() || !sequence.get(stage).equals(phase)))
                throw new IllegalStateException("The recorded delivery phase is out of order.");
            int input = switch (phase) {
                case "APPROACH", "PUSH", "RECOVER" -> direction;
                case "INTERRUPT", "RETREAT" -> -direction;
                default -> 0;
            };
            int hits = row.getInt("begins"), active = row.getInt("contacts");
            double force = row.getDouble("impulse"), x = row.getDouble("crateX");
            boolean jump = row.getBoolean("jump");
            if (row.getInt("tick") != tick || row.getInt("input") != input || hits < begins ||
                    active < 0 || active > hits || force < 0 || (tick <= 24) != phase.equals("SETTLE") ||
                    (jump && !phase.equals("APPROACH")))
                throw new IllegalStateException("The native input or contact record is invalid.");
            if (jump) jumps++;
            begins = hits;
            if (contact < 0 && hits > 0) contact = tick;
            if (impulse < 0 && force > 0) impulse = tick;
            if (motion < 0 && Math.abs(x - origin) > .00025) motion = tick;
            if (obstacle && crossing < 0 && direction * (row.getDouble("playerX") - barrierX) >= 0)
                crossing = tick;

            if (phase.equals("OBSERVE")) {
                if (changed) observe = tick;
                quiet++;
                if (active != 0 || force != 0 || hits != rows.getJSONObject(observe - 1).getInt("begins"))
                    throw new IllegalStateException("The observation window included a player contact.");
            }
            if (phase.equals("RECOVER")) {
                if (changed) restore = tick;
                if (recontact < 0 && force > 0 && hits > rows.getJSONObject(observe - 1).getInt("begins")) recontact = tick;
            }
            if (phase.equals("HOLD")) {
                if (changed) {
                    release = tick - 1;
                    releaseX = rows.getJSONObject(release).getDouble("crateX");
                    maxError = Math.abs(releaseX - target);
                    for (int j = tick - 8; j < tick; j++)
                        if (rows.getJSONObject(j).getInt("contacts") != 0)
                            throw new IllegalStateException("The player did not clear the crate before release.");
                }
                if (active != 0 || force != 0 || hits != rows.getJSONObject(release).getInt("begins"))
                    throw new IllegalStateException("The released crate was contacted.");
                maxError = Math.max(maxError, Math.abs(x - target));
                drift = Math.max(drift, Math.abs(x - releaseX));
            }
        }
        if (stage != sequence.size() - 1 || contact <= 24 || impulse < contact || motion < impulse ||
                Math.abs(run.getDouble("firstContactX") - origin) > .00025)
            throw new IllegalStateException("The native run did not complete its expected contact and release phases. Last phase "
                    + rows.getJSONObject(end).getString("phase") + " at tick " + end + ".");
        if (obstacle && (crossing <= 24 || crossing >= contact || crossing != run.getInt("obstacleCrossedTick") ||
                !rows.getJSONObject(crossing).getString("phase").equals("APPROACH") ||
                rows.getJSONObject(crossing).getDouble("playerY") < .759 || jumps < 1))
            throw new IllegalStateException("A measured airborne obstacle crossing must precede cargo contact.");
        var result = new JSONObject().put("ticks", end)
                .put("contactTick", contact).put("firstMotionTick", motion).put("obstacleCrossingTick", crossing);
        if (obstacle) result.put("crossingY", rows.getJSONObject(crossing).getDouble("playerY"));
        if (disturbance) {
            var kick = run.getJSONObject("disturbance");
            if (observe <= impulse || quiet != HOLD_TICKS || kick.getInt("tick") != observe ||
                    direction * kick.getDouble("impulse") >= 0 ||
                    Math.abs(direction * kick.getDouble("impulse") / run.getDouble("mass") + .26) > .00001 ||
                    Math.abs((kick.getDouble("speedAfter") - kick.getDouble("speedBefore")) -
                            kick.getDouble("impulse") / run.getDouble("mass")) > .00001 ||
                    Math.abs(kick.getDouble("x") - rows.getJSONObject(observe - 1).getDouble("crateX")) > .00001)
                throw new IllegalStateException("The recorded disturbance or unaided observation is invalid.");
            for (int j = observe - 16; j < observe; j++) {
                var clear = rows.getJSONObject(j);
                if (!"INTERRUPT".equals(clear.getString("phase")) || clear.getInt("contacts") != 0)
                    throw new IllegalStateException("The Courier must clear the crate before the external disturbance.");
            }
            var resting = rows.getJSONObject(observe + quiet - 1);
            double shortBy = direction * (target - resting.getDouble("crateX"));
            if (shortBy < .18 || Math.abs(resting.getDouble("crateVx")) > REST_SPEED ||
                    (adaptive && (restore != observe + quiet || recontact < restore)))
                throw new IllegalStateException("The trace lacks the stopped-short observation and physical recovery.");
            result.put("disturbanceTick", observe).put("stoppedShortBy", shortBy)
                    .put("recoveryContactTick", recontact);
        } else if (run.has("disturbance")) {
            throw new IllegalStateException("An unrequested disturbance was reported.");
        }
        var last = rows.getJSONObject(end);
        if (expectedHold && (release < 0 || end - release != HOLD_TICKS))
            throw new IllegalStateException("The final observation window is incomplete.");
        double finalSpeed = Math.abs(last.getDouble("crateVx"));
        boolean delivered = expectedHold && maxError <= POSITION_TOLERANCE && drift <= .018 && finalSpeed <= REST_SPEED;
        if (adaptive && !delivered)
            throw new IllegalStateException("The released crate did not stay on target. Error " + maxError + ", drift " + drift + ".");
        return result.put("delivered", delivered).put("outcome", delivered ? "delivered" : expectedHold ? "missed-hold" : "stopped-short")
                .put("releaseTick", release).put("holdTicks", expectedHold ? HOLD_TICKS : 0)
                .put("maxHeldError", maxError).put("drift", drift).put("finalSpeed", finalSpeed)
                .put("finalX", last.getDouble("crateX"));
    }
}
