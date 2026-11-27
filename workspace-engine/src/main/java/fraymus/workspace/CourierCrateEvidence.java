package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/** Independently checks native samples before retaining or replaying the paired crate trial. */
final class CourierCrateEvidence {
    private static final String FORMAT = "fraynix.courier-crate-v1";
    private static final int MAX = 160_000;
    private static final int TICKS = 120, SETTLE = 24, APPROACH = 24;
    private static final double MOTION_FLOOR = .00025;
    private final Path directory;

    CourierCrateEvidence(Path project) {
        directory = project.resolve("courier-crate");
    }

    synchronized JSONObject save(JSONObject nativeResult) throws Exception {
        JSONObject proof = check(nativeResult);
        String id = WorldFrames.hash(proof);
        Files.createDirectories(directory);
        var file = file(id);
        if (Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
            read(id);
        } else {
            SpriteWorkshop.replace(file, MapCheckpoint.canonical(proof).getBytes(StandardCharsets.UTF_8));
        }
        SpriteWorkshop.replace(directory.resolve("latest.txt"), id.getBytes(StandardCharsets.US_ASCII));
        return proof.put("id", id);
    }

    synchronized JSONObject latest() throws Exception {
        Path latest = directory.resolve("latest.txt");
        if (!Files.exists(latest, LinkOption.NOFOLLOW_LINKS)) return new JSONObject();
        return read(new String(SpriteWorkshop.read(latest, 64), StandardCharsets.US_ASCII));
    }

    synchronized JSONObject read(String id) throws Exception {
        var file = file(id);
        byte[] bytes = SpriteWorkshop.read(file, MAX);
        if (!MapDefinition.sha(bytes).equals(id)) throw new IllegalStateException("The retained Courier evidence changed.");
        var proof = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
        if (!proof.similar(check(proof.getJSONObject("native"))))
            throw new IllegalStateException("The saved Courier conclusions differ from the trace.");
        return proof.put("id", id);
    }

    synchronized JSONObject replay(String id, EmbeddedGameClient game) throws Exception {
        var expected = read(id);
        game.openCourierCrate();
        var actual = check(game.testCourierCrate());
        if (!id.equals(WorldFrames.hash(actual)))
            throw new IllegalStateException("The new Courier run differs from the retained source or tick evidence.");
        return expected.put("replayed", true);
    }

    private Path file(String id) {
        if (!id.matches("[a-f0-9]{64}")) throw new IllegalArgumentException("Invalid Courier evidence ID.");
        return directory.resolve(id + ".json");
    }

    static JSONObject check(JSONObject data) {
        if (!"Game Lab / JBox2D".equals(data.getString("engine")) || data.getInt("tickHz") != 60)
            throw new IllegalArgumentException("A 60 Hz Game Lab native trace is required.");
        for (String binding : new String[]{"atlasSha256", "crateArtSha256", "levelSha256", "manifestSha256"})
            if (!data.getString(binding).matches("[a-f0-9]{64}"))
                throw new IllegalArgumentException("Courier source identity is missing.");
        java.util.UUID.fromString(data.getString("playerId"));
        JSONObject control = data.getJSONObject("control");
        JSONObject push = data.getJSONObject("push");
        for (JSONObject run : new JSONObject[]{control, push}) {
            if (!"DYNAMIC".equals(run.getString("playerType")) || !run.getBoolean("playerFixedRotation")
                    || !"DYNAMIC".equals(run.getString("crateType"))
                    || !(run.getDouble("playerMass") > 0) || !(run.getDouble("crateMass") > 0))
                throw new IllegalStateException("The pilot and cargo must be real dynamic bodies.");
        }
        if (control.getDouble("playerMass") != push.getDouble("playerMass") ||
                control.getDouble("crateMass") != push.getDouble("crateMass"))
            throw new IllegalStateException("The paired trials changed an actual body mass.");

        JSONArray controlRows = control.getJSONArray("rows");
        JSONArray pushRows = push.getJSONArray("rows");
        var baseline = audit(controlRows, false);
        var intervention = audit(pushRows, true);
        for (int tick = 0; tick <= SETTLE + APPROACH; tick++) {
            if (!controlRows.getJSONObject(tick).similar(pushRows.getJSONObject(tick)))
                throw new IllegalStateException("The two runs must match through the shared approach.");
        }
        if (baseline.firstContact >= 0 || baseline.firstImpulse >= 0 || baseline.firstMotion >= 0 ||
                intervention.firstContact <= SETTLE + APPROACH || intervention.firstImpulse < intervention.firstContact ||
                intervention.firstMotion < intervention.firstImpulse || intervention.finalDx < .15)
            throw new IllegalStateException("The native trace did not isolate a contact-driven crate push.");
        double atContact = push.getDouble("crateXAtFirstContact");
        if (Math.abs(atContact - pushRows.getJSONObject(0).getDouble("crateX")) > MOTION_FLOOR ||
                control.has("crateXAtFirstContact"))
            throw new IllegalStateException("Crate motion was present before the native contact callback.");

        var findings = new JSONObject().put("sharedApproachThroughTick", SETTLE + APPROACH)
                .put("motionThreshold", MOTION_FLOOR)
                .put("firstContactTick", intervention.firstContact)
                .put("firstHorizontalImpulseTick", intervention.firstImpulse)
                .put("firstMotionTick", intervention.firstMotion)
                .put("firstHorizontalNormalImpulse", intervention.initialImpulse)
                .put("crateXAtFirstContact", atContact)
                .put("maxPrecontactAbsDx", intervention.beforeContact)
                .put("controlMaxAbsDx", baseline.maxDx)
                .put("finalPushDx", intervention.finalDx);
        return new JSONObject().put("format", FORMAT).put("passed", true).put("measurements", findings)
                .put("native", data);
    }

    private static Audit audit(JSONArray rows, boolean push) {
        if (rows.length() != TICKS + 1) throw new IllegalArgumentException("The Courier trial lost ticks.");
        double startX = rows.getJSONObject(0).getDouble("crateX");
        int firstContact = -1, firstImpulse = -1, firstMotion = -1, previousBegins = 0;
        double beforeContact = 0, maxDx = 0, finalDx = 0, initialImpulse = 0;
        for (int tick = 0; tick <= TICKS; tick++) {
            JSONObject row = rows.getJSONObject(tick);
            int intended = tick > SETTLE && (push || tick <= SETTLE + APPROACH) ? 1 : 0;
            if (row.getInt("tick") != tick || row.getInt("input") != intended)
                throw new IllegalArgumentException("The recorded Courier input schedule changed.");
            int begins = row.getInt("beginContacts");
            double impulse = row.getDouble("horizontalNormalImpulse");
            if (begins < previousBegins || impulse < 0 || (tick == 0 && (begins != 0 || impulse != 0)))
                throw new IllegalArgumentException("Invalid Courier contact counters.");
            previousBegins = begins;
            if (begins > 0 && firstContact < 0) firstContact = tick;
            if (impulse > 0 && firstImpulse < 0) {
                firstImpulse = tick;
                initialImpulse = impulse;
            }
            finalDx = row.getDouble("crateX") - startX;
            double abs = Math.abs(finalDx);
            if (abs > MOTION_FLOOR && firstMotion < 0) firstMotion = tick;
            if (firstContact < 0) beforeContact = Math.max(beforeContact, abs);
            maxDx = Math.max(maxDx, abs);
        }
        return new Audit(firstContact, firstImpulse, firstMotion, initialImpulse, beforeContact, maxDx, finalDx);
    }

    private record Audit(int firstContact, int firstImpulse, int firstMotion, double initialImpulse,
                         double beforeContact, double maxDx, double finalDx) {}
}
