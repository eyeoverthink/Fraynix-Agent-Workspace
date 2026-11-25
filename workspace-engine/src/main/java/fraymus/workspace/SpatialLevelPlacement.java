package fraymus.workspace;

import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;

/** A source-bound prop addition to an authored floor, with an explicit walking route. */
final class SpatialLevelPlacement {
    static final String FORMAT = "fraynix.spatial-level-placement-v1";
    static final double PLAYER_RADIUS = .18;
    static final double PLAYER_HEIGHT = 1.7;

    private SpatialLevelPlacement() {}

    static JSONObject plan(JSONObject source, JSONObject destination, JSONObject input) {
        SpatialScene.keys(input, "sceneId", "objectId", "destinationId", "floorId", "targetM", "yawDeg", "aisle", "goal");
        if (source.getString("id").equals(destination.getString("id")))
            throw new IllegalArgumentException("Choose a destination level separate from the source prop.");
        if (!"m".equals(destination.getString("sourceUnits")))
            throw new IllegalArgumentException("Choose an authored level in metres for detailed prop placement.");
        var target = coordinates(input.getJSONArray("targetM"), 2);
        var aisle = coordinates(input.getJSONArray("aisle"), 4);
        double yaw = SpatialScene.number(input, "yawDeg", -180, 180);
        String goal = SpatialScene.text(input, "goal", 300);
        var review = SpatialPlacement.plan(source, new JSONObject().put("goal", "Place the verified prop")
                .put("sceneId", input.getString("sceneId")).put("objectId", input.getString("objectId")),
                source.optJSONObject("meshQualification"));
        var floor = SpatialScene.object(destination, input.getString("floorId"));
        var r = floor.getJSONArray("rotation");
        if (floor.getDouble("massKg") != 0 || Math.abs(r.getDouble(0)) > 1e-8
                || Math.abs(r.getDouble(1)) > 1e-8 || Math.abs(r.getDouble(2)) > 1e-8)
            throw new IllegalArgumentException("Select a flat, axis-aligned static floor.");
        var floorBounds = bounds(floor);
        double height = floorBounds[4];
        double angle = Math.toRadians(yaw) / 2;
        review.put("format", FORMAT).put("goal", goal)
                .put("destination", SpatialPropCopy.binding(destination)).put("destinationTitle", destination.getString("title"))
                .put("floor", floor.getString("sourceId")).put("floorLabel", floor.getString("label"))
                .put("aisle", new JSONArray(aisle)).put("yawDeg", yaw)
                .put("targetM", new JSONArray(new double[]{target[0], height + review.getJSONArray("colliderM").getDouble(1) / 2, target[1]}))
                .put("rotation", new JSONArray(new double[]{0, Math.sin(angle), 0, Math.cos(angle)}));
        var reference = browserReference(source);
        if (reference != null) review.put("browserReference", reference);
        if (source.has("meshQualification")) {
            review.put("editReference", WorldFrames.copy(source.getJSONObject("meshQualification"))
                    .put("receiptSha256", source.getString("meshQualificationSha256")));
        }
        var scene = SpatialScene.parse(definition(source, destination, review).toString(), sceneId(WorldFrames.hash(review)));
        playable(scene, review);
        return review;
    }

    static JSONObject browserReference(JSONObject source) {
        var proof = source.optJSONObject("reconstruction");
        if (proof == null || !BrowserViewTrace.POLICY.equals(proof.optString("policy")) || proof.has("viewSource"))
            return null;
        var crops = new JSONObject();
        var views = proof.getJSONObject("views");
        for (String role : views.keySet()) {
            // Share capture identities, without embedding the source page address or pixels.
            crops.put(role, new JSONObject(views.getJSONObject(role), "tag", "pixels", "pngSha256"));
        }
        return new JSONObject().put("sourceSha256", proof.getString("sourceSha256"))
                .put("reconstructionSha256", source.getString("reconstructionSha256"))
                .put("declaredWidthM", proof.getDouble("declaredWidthM"))
                .put("heldOutIou", proof.getDouble("heldOutIou"))
                .put("heldOutElevationDeg", proof.optInt("heldOutElevationDeg", 0))
                .put("crops", crops);
    }

    static String sceneId(String reviewId) {
        return UUID.nameUUIDFromBytes((FORMAT + ":" + reviewId).getBytes(StandardCharsets.UTF_8)).toString();
    }

    static String propKey(JSONObject review) {
        return "level-prop-" + WorldFrames.hash(review).substring(0, 20);
    }

    static JSONObject definition(JSONObject source, JSONObject destination, JSONObject review) {
        if (!SpatialPropCopy.binding(destination).similar(review.getJSONObject("destination")))
            throw new IllegalStateException("The approved destination revision changed.");
        if (review.has("browserReference")) {
            var checked = browserReference(source);
            if (checked == null || !checked.similar(review.getJSONObject("browserReference")))
                throw new IllegalStateException("The approved browser reference changed.");
        }
        if (review.has("editReference") && (!source.has("meshQualification")
                || !review.getJSONObject("editReference").similar(WorldFrames.copy(source.getJSONObject("meshQualification"))
                    .put("receiptSha256", source.getString("meshQualificationSha256")))))
            throw new IllegalStateException("The approved edit qualification changed.");
        var prop = SpatialPlacement.placedBody(source, review, propKey(review));
        var original = new JSONObject(destination.getString("sourceText"));
        original.getJSONArray("objects").put(prop);
        String mesh = review.getString("meshSha256");
        var meshes = original.optJSONObject("visualMeshes");
        if (meshes == null) meshes = new JSONObject();
        original.put("visualMeshes", meshes.put(mesh, source.getJSONObject("visualMeshes").getString(mesh)));
        return original;
    }

    /** A conservative swept rectangle for an axis-aligned aisle on the selected floor. */
    static JSONObject playable(JSONObject scene, JSONObject review) {
        String floorKey = review.getString("floor");
        String propKey = propKey(review);
        var objects = scene.getJSONArray("objects");
        JSONObject floor = null, prop = null;
        for (Object raw : objects) {
            var body = (JSONObject) raw;
            if (floorKey.equals(body.getString("sourceId"))) floor = body;
            if (propKey.equals(body.getString("sourceId"))) prop = body;
        }
        if (floor == null || prop == null) throw new IllegalStateException("The reviewed floor or prop is missing.");

        double[] base = bounds(floor), placed = bounds(prop);
        if (!inside(base, placed)) throw new IllegalArgumentException("The whole prop must rest on the reviewed floor.");
        double[] route = coordinates(review.getJSONArray("aisle"), 4);
        double distance = Math.hypot(route[2] - route[0], route[3] - route[1]);
        if ((route[0] != route[2] && route[1] != route[3]) || distance < .5)
            throw new IllegalArgumentException("Set a straight X or Z aisle at least 0.5 m long.");
        double[] sweep = {Math.min(route[0], route[2]) - PLAYER_RADIUS, base[4],
                Math.min(route[1], route[3]) - PLAYER_RADIUS, Math.max(route[0], route[2]) + PLAYER_RADIUS,
                base[4] + PLAYER_HEIGHT, Math.max(route[1], route[3]) + PLAYER_RADIUS};
        if (!inside(base, sweep)) throw new IllegalArgumentException("The player aisle must stay on the reviewed floor.");

        var blockers = new JSONArray();
        for (Object raw : objects) {
            var body = (JSONObject) raw;
            if (body == floor) continue;
            var box = bounds(body);
            if (body != prop && overlaps(placed, box))
                throw new IllegalArgumentException("Placement intersects existing collider “" + body.getString("label") + "”.");
            if (overlaps(sweep, box))
                throw new IllegalArgumentException("The player aisle is blocked by “" + body.getString("label") + "”.");
            if (box[4] > sweep[1] && box[1] < sweep[4])
                blockers.put(new JSONObject().put("id", body.getString("sourceId"))
                        .put("label", body.getString("label")).put("box", new JSONArray(box)));
        }
        return new JSONObject().put("title", scene.getString("title")).put("floor", new JSONArray(base))
                .put("radius", PLAYER_RADIUS).put("heightM", PLAYER_HEIGHT).put("route", new JSONArray(route))
                .put("routeLengthM", distance).put("blockers", blockers).put("prop", propKey);
    }

    static JSONObject prop(JSONObject scene, JSONObject review) {
        for (Object raw : scene.getJSONArray("objects")) {
            var body = (JSONObject) raw;
            if (propKey(review).equals(body.getString("sourceId"))) return body;
        }
        throw new IllegalStateException("The reviewed level prop is missing.");
    }

    static JSONObject verifyNative(JSONObject scene, JSONObject review, Path nativeDir) throws Exception {
        var layout = playable(scene, review);
        var route = layout.getJSONArray("route");
        var floor = layout.getJSONArray("floor");
        double startX = route.getDouble(0), startZ = route.getDouble(1);
        double endX = route.getDouble(2), endZ = route.getDouble(3);
        try (var world = new SpatialPhysics(scene, nativeDir)) {
            var sweep = new PhysicsRigidBody(new BoxCollisionShape(
                    (float)(Math.abs(endX-startX)/2 + PLAYER_RADIUS), (float)(PLAYER_HEIGHT/2),
                    (float)(Math.abs(endZ-startZ)/2 + PLAYER_RADIUS)), 0);
            sweep.setPhysicsLocation(SpatialPhysics.vec(new double[]{(startX+endX)/2,
                    floor.getDouble(4) + PLAYER_HEIGHT/2, (startZ+endZ)/2}));
            world.space.addCollisionObject(sweep);
            for (var body : world.bodies.values()) {
                boolean[] hit = {false};
                world.space.pairTest(sweep, body, event -> hit[0] |= event.getDistance1() < -.001f);
                if (hit[0]) throw new IllegalStateException("The native player envelope intersects a collider.");
            }
        }
        return SpatialPlacement.verifyNative(scene, review, nativeDir, prop(scene, review), true)
                .put("playerEnvelopeClear", true).put("routeLengthM", layout.getDouble("routeLengthM"));
    }

    static JSONObject export(JSONObject scene, String kind) throws Exception {
        var evidence = scene.getJSONObject("levelPlacement");
        var review = evidence.getJSONObject("review");
        String receipt = scene.getString("levelPlacementSha256");
        var layout = playable(scene, review);
        byte[] bytes;
        if ("play".equals(kind)) {
            bytes = SpriteWorkshop.portableGame(new JSONObject().put("layout", layout).put("evidenceSha256", receipt),
                    "spatial-level-play.js", "mountSpatialLevel");
        } else if ("glb".equals(kind)) {
            bytes = SpatialGlb.verifiedExport(scene);
        } else if ("evidence".equals(kind)) {
            byte[] glb = SpatialGlb.verifiedExport(scene);
            var document = new JSONObject().put("format", "fraynix.verified-spatial-level-v1")
                    .put("sceneId", scene.getString("id")).put("scene", new JSONObject(scene.getString("sourceText")))
                    .put("evidence", evidence).put("evidenceSha256", receipt)
                    .put("glbSha256", MapDefinition.sha(glb)).put("glbBase64", Base64.getEncoder().encodeToString(glb));
            bytes = document.toString().getBytes(StandardCharsets.UTF_8);
        } else {
            throw new IllegalArgumentException("Choose a playable level, GLB, or an evidence bundle.");
        }
        return new JSONObject().put("sceneId", scene.getString("id")).put("evidenceSha256", receipt)
                .put("base64", Base64.getEncoder().encodeToString(bytes)).put("sha256", MapDefinition.sha(bytes));
    }

    private static double[] coordinates(JSONArray array, int length) {
        if (array.length() != length) throw new IllegalArgumentException("Give each X and Z coordinate in metres.");
        double[] values = new double[length];
        for (int i = 0; i < length; i++) {
            if (!(array.get(i) instanceof Number value)) throw new IllegalArgumentException("Use numeric coordinates.");
            values[i] = value.doubleValue();
            RampDefinition.range(values[i], -20, 20, "coordinate in metres");
        }
        return values;
    }

    private static double[] bounds(JSONObject object) {
        var p = ObserverGeometry.v(object.getJSONArray("position"));
        var size = ObserverGeometry.v(object.getJSONArray("size"));
        var q = object.getJSONArray("rotation");
        double[] radius = new double[3];
        for (int axis = 0; axis < 3; axis++) {
            double[] halfAxis = new double[3];
            halfAxis[axis] = size[axis] / 2;
            double[] rotated = ObserverGeometry.rotate(halfAxis, q, false);
            for (int j = 0; j < 3; j++) radius[j] += Math.abs(rotated[j]);
        }
        return new double[]{p[0]-radius[0], p[1]-radius[1], p[2]-radius[2],
                p[0]+radius[0], p[1]+radius[1], p[2]+radius[2]};
    }

    private static boolean inside(double[] floor, double[] box) {
        return box[0] >= floor[0] && box[2] >= floor[2] && box[3] <= floor[3] && box[5] <= floor[5];
    }

    private static boolean overlaps(double[] a, double[] b) {
        for (int i = 0; i < 3; i++) {
            if (Math.min(a[i+3], b[i+3]) - Math.max(a[i], b[i]) <= 1e-9) return false;
        }
        return true;
    }
}
