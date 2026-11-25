
package fraymus.workspace;

import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Pattern;

/** A reviewed authored placement and its two independent native contact trials. */
final class SpatialPlacement {
    private static final String FORMAT = "fraynix.spatial-placement-v1";
    private static final Pattern REQUEST = Pattern.compile(
            "(?i)^(?:please )?place (?:the )?(?:selected |verified )?(?:3d )?prop\\b(.*)$");
    private static final String METRES = "\\s*(-?\\d+(?:\\.\\d+)?)\\s*(?:m|metres?|meters?)";
    private static final Pattern RELATIVE = Pattern.compile(
            "(?i)^" + METRES + " to the (left|right|front|back)$");
    private static final Pattern COORDINATES = Pattern.compile(
            "(?i)^at x\\s*=?"+METRES+",?\\s+z\\s*=?"+METRES
                    +"(?:,?\\s+yaw\\s*=?\\s*(-?\\d+(?:\\.\\d+)?)\\s*(?:°|degrees?))?$");
    private static final Pattern SUFFIX = Pattern.compile(
            "(?i)\\s*(?:,\\s*|\\s+and\\s+)?(?:then\\s+)?(?:test (?:a |the )?collision"
                    +"(?:,?\\s+(?:(?:and|then)\\s+){1,2}export(?: the scene)?(?: with evidence)?)?"
                    +"|export(?: the scene)?(?: with evidence)?)\\s*$");
    private static final int TICKS = 180;
    private static final float SPEED = 1.8f, PROBE_MASS = .35f;
    private static final byte[] YES = "approved\n".getBytes(StandardCharsets.US_ASCII);
    private final Path directory;

    SpatialPlacement(Path project) {
        directory = project.resolve("spatial/placement-reviews");
    }

    static JSONObject plan(JSONObject source, JSONObject input) {
        SpatialScene.keys(input, "sceneId", "objectId", "goal");
        String goal = SpatialScene.text(input, "goal", 300).strip().replaceFirst("[.!?]+$", "");
        var match = REQUEST.matcher(goal);
        if (!match.matches()) throw new IllegalArgumentException("Start with “Place the verified prop”.");
        var body = SpatialScene.object(source, input.getString("objectId"));
        var proof = source.optJSONObject("reconstruction");
        if (proof == null || !BrowserViewTrace.POLICY.equals(proof.optString("policy"))
                || !(proof.optDouble("heldOutIou", 0) >= .86) || !body.optString("visualMesh").equals(proof.optString("meshSha256"))
                || body.optString("visualMesh").isEmpty() || body.getDouble("massKg") <= 0)
            throw new IllegalArgumentException("Select an original Spatial prop with a checked held-out view.");

        String where = SUFFIX.matcher(match.group(1)).replaceFirst("").strip();
        double x = .75, z = 0, yaw = 0;
        if (!where.isEmpty()) {
            var relative = RELATIVE.matcher(where);
            var coordinates = COORDINATES.matcher(where);
            if (relative.matches()) {
                double distance = Double.parseDouble(relative.group(1));
                RampDefinition.range(distance, .05, 2, "distance");
                switch (relative.group(2).toLowerCase(java.util.Locale.ROOT)) {
                    case "left" -> x = -distance;
                    case "right" -> x = distance;
                    case "front" -> { x = 0; z = distance; }
                    case "back" -> { x = 0; z = -distance; }
                }
            } else if (coordinates.matches()) {
                x = Double.parseDouble(coordinates.group(1));
                z = Double.parseDouble(coordinates.group(2));
                if (coordinates.group(3) != null) yaw = Double.parseDouble(coordinates.group(3));
            } else {
                throw new IllegalArgumentException("Give a distance and direction, or use “at X 0.75 m, Z 0 m, yaw 20 degrees”.");
            }
        }
        RampDefinition.range(x, -2, 2, "X");
        RampDefinition.range(z, -2, 2, "Z");
        RampDefinition.range(yaw, -180, 180, "yaw");
        double[] size = ObserverGeometry.v(body.getJSONArray("size"));
        if (size[1] < .05 || size[0] > 2 || size[2] > 2 || size[1] > 2)
            throw new IllegalArgumentException("This contact deck needs a prop at least 5 cm tall and no axis above 2 m.");
        double angle = Math.toRadians(yaw) / 2;
        return new JSONObject().put("format", FORMAT).put("goal", goal)
                .put("source", SpatialPropCopy.binding(source)).put("sourceTitle", source.getString("title"))
                .put("objectId", body.getString("id")).put("meshSha256", body.getString("visualMesh"))
                .put("sourceOverlap", proof.getDouble("heldOutIou")).put("colliderM", body.getJSONArray("size"))
                .put("targetM", new JSONArray(new double[]{x, size[1] / 2, z})).put("yawDeg", yaw)
                .put("rotation", new JSONArray(new double[]{0,Math.sin(angle),0,Math.cos(angle)}))
                .put("contact", contactSpec());
    }

    static JSONObject contactSpec() {
        return new JSONObject().put("probeSpeedMps", SPEED).put("probeMassKg", PROBE_MASS)
                .put("ticksPerTrial", TICKS).put("stepS", BulletRamp.DT)
                .put("trialCount",4).put("policy", "moving-sphere-with-lateral-miss");
    }

    static JSONObject placedBody(JSONObject source, JSONObject review, String key) {
        if (!SpatialPropCopy.binding(source).similar(review.getJSONObject("source")))
            throw new IllegalStateException("The approved model source changed.");
        var body = SpatialScene.object(source, review.getString("objectId"));
        for (Object entry : new JSONObject(source.getString("sourceText")).getJSONArray("objects")) {
            var object = (JSONObject) entry;
            if (body.getString("sourceId").equals(object.getString("sourceId"))) {
                return WorldFrames.copy(object).put("sourceId",key).put("label",body.getString("label"))
                        .put("position",review.getJSONArray("targetM")).put("rotation",review.getJSONArray("rotation"));
            }
        }
        throw new IllegalStateException("The checked model is missing.");
    }

    static JSONObject definition(JSONObject source, JSONObject review) {
        var prop = placedBody(source, review, "placed-prop");
        var raw = new JSONObject(source.getString("sourceText"));
        var deck = SpatialReconstruction.box("contact-deck", "Contact test deck",
                new double[]{0,-.10,0}, new double[]{10,.20,10}, 0);
        String mesh = review.getString("meshSha256");
        raw.put("title", "Reviewed placement · " + source.getString("title"));
        raw.put("source", new JSONObject().put("kind","unverified").put("physicalProperties","declared")
                .put("note","Approved placement of held-out-verified mesh " + mesh + " on an authored contact deck."));
        raw.put("objects",new JSONArray().put(deck).put(prop));
        raw.put("visualMeshes",new JSONObject().put(mesh,source.getJSONObject("visualMeshes").getString(mesh)));
        raw.remove("translationAnimation");
        return raw;
    }

    static String sceneId(String id) {
        return UUID.nameUUIDFromBytes((FORMAT + ":" + id).getBytes(StandardCharsets.UTF_8)).toString();
    }

    String saveReview(JSONObject review) throws IOException {
        Files.createDirectories(directory);
        String id = WorldFrames.hash(review);
        Path file = file(id, ".json");
        if (Files.exists(file, LinkOption.NOFOLLOW_LINKS)) readReview(id);
        else SpriteWorkshop.replace(file, review.toString().getBytes(StandardCharsets.UTF_8));
        SpriteWorkshop.replace(directory.resolve("latest"), id.getBytes(StandardCharsets.US_ASCII));
        return id;
    }

    String latestId() throws IOException {
        Path latest = directory.resolve("latest");
        if (!Files.exists(latest, LinkOption.NOFOLLOW_LINKS)) return "";
        return new String(SpriteWorkshop.read(latest, 64), StandardCharsets.US_ASCII);
    }

    JSONObject readReview(String id) throws IOException {
        var document = new JSONObject(new String(SpriteWorkshop.read(file(id,".json"), 16_000), StandardCharsets.UTF_8));
        if (!WorldFrames.hash(document).equals(id)) throw new IllegalStateException("The saved placement review changed.");
        return document;
    }

    boolean approved(String id) throws IOException {
        Path file = file(id, ".approved");
        if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) return false;
        if (!java.util.Arrays.equals(SpriteWorkshop.read(file, YES.length), YES))
            throw new IllegalStateException("The saved placement approval changed.");
        return true;
    }

    void approve(String id) throws IOException {
        if (!approved(id)) SpriteWorkshop.replace(file(id,".approved"), YES);
    }

    private Path file(String id, String extension) {
        if (!id.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Invalid placement review ID.");
        return directory.resolve(id + extension);
    }

    static JSONObject verifyNative(JSONObject scene, JSONObject review, Path nativeDir) throws Exception {
        return verifyNative(scene, review, nativeDir, scene.getJSONArray("objects").getJSONObject(1), false);
    }

    static JSONObject verifyNative(JSONObject scene, JSONObject review, Path nativeDir, JSONObject prop,
                                   boolean checkOtherBodies) throws Exception {
        JSONObject hit = trial(scene, review, nativeDir, prop, true, checkOtherBodies);
        JSONObject miss = trial(scene, review, nativeDir, prop, false, checkOtherBodies);
        if (hit.getInt("firstContactTick") < 1 || hit.getDouble("maxPropPlanarSpeedMps") < .02
                || miss.getInt("firstContactTick") != 0 || miss.getDouble("maxPropPlanarSpeedMps") > .01)
            throw new IllegalStateException("The measured hit and miss did not verify this placement.");
        return new JSONObject().put("passed", true).put("hit", hit).put("miss", miss);
    }

    private static JSONObject trial(JSONObject scene, JSONObject review, Path nativeDir, JSONObject object,
                                    boolean hit, boolean checkOtherBodies) throws Exception {
        var size = ObserverGeometry.v(object.getJSONArray("size"));
        var target = ObserverGeometry.v(review.getJSONArray("targetM"));
        double yaw = Math.toRadians(review.getDouble("yawDeg"));
        double dx = (Math.abs(Math.cos(yaw))*size[0] + Math.abs(Math.sin(yaw))*size[2]) / 2;
        double dz = (Math.abs(Math.sin(yaw))*size[0] + Math.abs(Math.cos(yaw))*size[2]) / 2;
        float radius = (float) Math.min(.035, size[1] / 3);
        var start = new Vector3f((float)(target[0]-dx-.32), (float)target[1],
                (float)(target[2]+(hit ? 0 : dz+.14)));
        try (var world = new SpatialPhysics(scene,nativeDir)) {
            var prop = world.bodies.get(object.getString("id"));
            var probe = new PhysicsRigidBody(new SphereCollisionShape(radius), PROBE_MASS);
            probe.setPhysicsLocation(start);
            probe.setFriction(0);
            probe.setSleepingThresholds(0,0);
            world.space.addCollisionObject(probe);
            probe.setGravity(Vector3f.ZERO);
            probe.setLinearVelocity(new Vector3f(SPEED,0,0));
            int first = 0, contactTicks = 0;
            double maxSpeed = 0, firstSeparation = 0;
            var samples = new JSONArray();
            if (checkOtherBodies) requireClearProbe(world, probe, prop);
            samples.put(sample(0,probe,prop));
            for (int i=1;i<=TICKS;i++) {
                world.step();
                if (checkOtherBodies && (first == 0 || !hit)) requireClearProbe(world, probe, prop);
                float[] separation = {Float.POSITIVE_INFINITY};
                world.space.pairTest(probe,prop,event ->
                        separation[0] = Math.min(separation[0],event.getDistance1()));
                if (separation[0]<=0) {
                    contactTicks++;
                    if (first == 0) {
                        first = i;
                        firstSeparation = separation[0];
                        if (i%12!=0) samples.put(sample(i,probe,prop));
                    }
                }
                var velocity = prop.getLinearVelocity();
                maxSpeed = Math.max(maxSpeed,Math.hypot(velocity.x,velocity.z));
                if (i%12==0) samples.put(sample(i,probe,prop));
            }
            return new JSONObject().put("firstContactTick",first).put("contactTicks",contactTicks)
                    .put("firstContactSeparationM",first == 0 ? JSONObject.NULL : firstSeparation)
                    .put("probeRadiusM",radius).put("probeStartM",SpatialPhysics.json(start))
                    .put("probeIncomingMps",SPEED).put("maxPropPlanarSpeedMps",maxSpeed)
                    .put("sampleIntervalTicks",12).put("samples",samples);
        }
    }

    private static void requireClearProbe(SpatialPhysics world, PhysicsRigidBody probe, PhysicsRigidBody prop) {
        for (var other : world.bodies.values()) {
            if (other == prop) continue;
            boolean[] hit = {false};
            world.space.pairTest(probe, other, event -> hit[0] |= event.getDistance1() <= 0);
            if (hit[0]) throw new IllegalStateException(
                    "Another collider intercepted a contact trial. Choose a clear test approach.");
        }
    }

    private static JSONObject sample(int tick, PhysicsRigidBody probe, PhysicsRigidBody prop) {
        return new JSONObject().put("tick",tick).put("probeM",SpatialPhysics.json(probe.getPhysicsLocation()))
                .put("probeVelocityMps",SpatialPhysics.json(probe.getLinearVelocity()))
                .put("propM",SpatialPhysics.json(prop.getPhysicsLocation()))
                .put("propVelocityMps",SpatialPhysics.json(prop.getLinearVelocity()));
    }

    static JSONObject bundle(JSONObject scene, JSONObject source) throws Exception {
        byte[] glb = SpatialGlb.verifiedExport(scene);
        var evidence = scene.getJSONObject("propPlacement");
        var document = new JSONObject().put("format","fraynix.verified-placement-export-v1")
                .put("sceneId",scene.getString("id")).put("scene",new JSONObject(scene.getString("sourceText")))
                .put("sourceReconstruction",source.getJSONObject("reconstruction"))
                .put("evidence",evidence).put("evidenceSha256",scene.getString("propPlacementSha256"))
                .put("glbSha256",MapDefinition.sha(glb)).put("glbBase64",Base64.getEncoder().encodeToString(glb));
        byte[] bytes = document.toString().getBytes(StandardCharsets.UTF_8);
        return new JSONObject().put("base64",Base64.getEncoder().encodeToString(bytes)).put("sha256",MapDefinition.sha(bytes))
                .put("sceneId",scene.getString("id")).put("evidenceSha256",scene.getString("propPlacementSha256"))
                .put("glbSha256",document.getString("glbSha256"));
    }
}
