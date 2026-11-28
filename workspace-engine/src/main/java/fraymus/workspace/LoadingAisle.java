package fraymus.workspace;

import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static fraymus.workspace.ObserverGeometry.*;
import static fraymus.workspace.SpatialScene.array;

/** The commissioned edit is a translation along the horizontal loading-door normal. */
final class LoadingAisle {
    private static final double MOVE = .40, WIDTH = 1, HEIGHT = 1.80, BASE_GAP = .02;
    private static final Pattern BEGIN = Pattern.compile(
            "(?i)^(?:please\\s+)?(?:move|shift)\\s+(?:(?:the|this)\\s+)?(?:selected\\s+)?crate\\s+(?:by\\s+)?" +
            "(?:40\\s*(?:cm|centimet(?:er|re)s?)|forty\\s+centimet(?:er|re)s?|0?\\.4(?:0)?\\s*(?:m|met(?:er|re)s?)|400\\s*mm)" +
            "\\s+away\\s+from\\s+(?:the\\s+)?loading\\s+door\\b");
    private static final Pattern GUARDS = Pattern.compile(
            "(?i)^(?:[\\s,.;!?]+(?:and\\s+)?(?:keep\\s+(?:the\\s+)?(?:1\\s*(?:m|met(?:er|re)s?)|one[- ]met(?:er|re))\\s+" +
            "aisle\\s+clear|leave\\s+(?:the\\s+)?(?:shelves|shelving)\\s+(?:alone|unchanged)|" +
            "protect\\s+(?:the\\s+)?(?:shelves|shelving)))*[\\s.!?]*$");
    private static final String REVISION_SUFFIX = " · Reviewed aisle move";

    private LoadingAisle() {}

    static void validateRequest(String request) {
        var prefix = BEGIN.matcher(request.strip());
        if (!prefix.find() || !GUARDS.matcher(request.strip().substring(prefix.end())).matches())
            throw new IllegalArgumentException("Ask to move the selected crate 40 cm away from the loading door. This review permits only that crate translation.");
    }

    static JSONObject plan(JSONObject parent, String selected, String request) {
        validateRequest(request);
        if (parent.has("translationAnimation") || parent.has("recipientBranch"))
            throw new IllegalArgumentException("Choose a static layout for the reviewed aisle edit.");
        var crate = SpatialScene.object(parent, selected);
        if (!isCrate(crate))
            throw new IllegalArgumentException("Select the intended dynamic crate in the Spatial view.");
        var door = landmark(parent, "loading door");
        var floor = landmark(parent, "loading bay floor");
        var marker = landmark(parent, "aisle end");
        for (var object : List.of(crate, door, floor, marker)) upright(object);
        var protectedRows = new JSONArray();
        for (Object raw : parent.getJSONArray("objects")) {
            var object = (JSONObject) raw;
            if (!object.getString("label").toLowerCase(Locale.ROOT).matches(".*\\b(shelf|shelves|shelving)\\b.*"))
                continue;
            if (object.getDouble("massKg") != 0)
                throw new IllegalArgumentException("Shelves must be fixed to protect them during this edit.");
            protectedRows.put(new JSONObject().put("sourceId", object.getString("sourceId"))
                    .put("label", object.getString("label")));
        }
        if (protectedRows.isEmpty()) throw new IllegalArgumentException("Mark the protected shelves in this layout.");

        var q = door.getJSONArray("rotation");
        double[] normal = rotate(new double[]{0,0,1}, q, false);
        normal[1] = 0;
        normal = mul(normal, 1 / norm(normal));
        double[] start = v(crate.getJSONArray("position"));
        var entry = v(door.getJSONArray("position"));
        var floorSize = v(floor.getJSONArray("size"));
        double level = v(floor.getJSONArray("position"))[1] + floorSize[1] / 2;
        entry[1] = level;
        var end = v(marker.getJSONArray("position"));
        end[1] = level;
        var doorToEnd = sub(end, entry);
        double length = dot(doorToEnd, normal);
        if (Math.abs(length) < 1 || Math.abs(length) > 8 ||
                norm(sub(doorToEnd, mul(normal, length))) > .015)
            throw new IllegalArgumentException("Place the aisle end 1–8 m along the loading-door normal.");
        if (length < 0) normal = mul(normal, -1);
        length = Math.abs(length);
        end = add(entry, mul(normal, length));
        if (dot(sub(start, entry), normal) < .10)
            throw new IllegalArgumentException("The crate must be on the aisle side of the loading door.");
        var target = add(start, mul(normal, MOVE));

        var widthAxis = new double[]{normal[2],0,-normal[0]};
        for (var center : List.of(entry, end))
            for (int side : new int[]{-1, 1}) requireFloorPoint(floor, add(center, mul(widthAxis, side * WIDTH / 2)));
        requireCrateFloor(crate, floor, level, start);
        requireCrateFloor(crate, floor, level, target);
        var aisle = new JSONObject().put("entryM", array(entry)).put("endM", array(end))
                .put("widthM", WIDTH).put("heightM", HEIGHT).put("lengthM", length)
                .put("floorY", level).put("floor", floor.getString("sourceId"));
        return new JSONObject().put("format", "fraynix.loading-aisle-edit-v1").put("request", request)
                .put("parentId", parent.getString("id"))
                .put("parentSha256", parent.getString("sourceSha256"))
                .put("objectId", selected).put("crate", crate.getString("sourceId")).put("crateLabel", crate.getString("label"))
                .put("door", door.getString("sourceId")).put("directionM", array(normal))
                .put("deltaM", array(mul(normal, MOVE))).put("fromM", array(start)).put("toM", array(target))
                .put("aisle", aisle).put("protected", protectedRows);
    }

    static boolean isCrate(JSONObject object) {
        return object.getDouble("massKg") > 0 &&
                object.getString("label").toLowerCase(Locale.ROOT).matches(".*\\bcrate\\b.*");
    }

    private static JSONObject landmark(JSONObject scene, String label) {
        JSONObject match = null;
        for (Object raw : scene.getJSONArray("objects")) {
            var object = (JSONObject) raw;
            if (!object.getString("label").strip().equalsIgnoreCase(label)) continue;
            if (match != null || object.getDouble("massKg") != 0)
                throw new IllegalArgumentException("Mark one fixed " + label + ".");
            match = object;
        }
        if (match == null) throw new IllegalArgumentException("The scene needs a marked " + label + ".");
        return match;
    }

    private static void upright(JSONObject object) {
        var up = rotate(new double[]{0,1,0}, object.getJSONArray("rotation"), false);
        if (norm(sub(up, new double[]{0,1,0})) > .00001)
            throw new IllegalArgumentException("The door, floor and crate must have upright colliders.");
    }

    private static void requireFloorPoint(JSONObject floor, double[] point) {
        var local = rotate(sub(point, v(floor.getJSONArray("position"))), floor.getJSONArray("rotation"), true);
        var size = v(floor.getJSONArray("size"));
        if (Math.abs(local[0]) > size[0] / 2 + .00001 || Math.abs(local[2]) > size[2] / 2 + .00001)
            throw new FloorBounds();
    }

    private static void requireCrateFloor(JSONObject crate, JSONObject floor, double level, double[] center) {
        var size = v(crate.getJSONArray("size"));
        if (Math.abs(center[1] - size[1] / 2 - level) > .025)
            throw new IllegalArgumentException("The crate must be supported on the marked bay floor.");
        for (int x : new int[]{-1, 1}) for (int z : new int[]{-1, 1}) {
            var corner = new double[]{x * size[0] / 2,0,z * size[2] / 2};
            requireFloorPoint(floor, add(center, rotate(corner, crate.getJSONArray("rotation"), false)));
        }
    }

    static void adjust(JSONObject parent, JSONObject review, double lateral) {
        var start = v(review.getJSONArray("fromM"));
        var forward = v(review.getJSONArray("directionM"));
        var delta = add(mul(forward, MOVE), mul(new double[]{forward[2],0,-forward[0]}, lateral));
        var target = add(start,delta);
        var aisle = review.getJSONObject("aisle");
        requireCrateFloor(find(parent,review.getString("crate")),find(parent,aisle.getString("floor")),
                aisle.getDouble("floorY"),target);
        review.put("deltaM",array(delta)).put("toM",array(target));
    }

    static final class FloorBounds extends IllegalArgumentException {
        FloorBounds() { super("The crate or aisle extends outside the marked bay floor."); }
    }

    static final class Obstruction extends IllegalStateException {
        final String gate;
        Obstruction(String gate, String message) {
            super(message);
            this.gate = gate;
        }
        JSONObject evidence() {
            return new JSONObject().put("gate",gate).put("reason",getMessage());
        }
    }

    static JSONObject revision(JSONObject parent, JSONObject review) {
        var source = new JSONObject(parent.getString("sourceText"));
        String title = source.getString("title");
        source.put("title", title.substring(0, Math.min(title.length(), 95)) + REVISION_SUFFIX);
        double scale = switch (source.getString("units")) { case "cm" -> .01; case "mm" -> .001; default -> 1; };
        find(source, review.getString("crate")).put("position", array(mul(v(review.getJSONArray("toM")), 1 / scale)));
        return source;
    }

    static void checkScope(JSONObject parent, JSONObject candidate, JSONObject review) {
        var source = new JSONObject(parent.getString("sourceText"));
        var actual = new JSONObject(candidate.getString("sourceText"));
        String key = review.getString("crate");
        if (!vEquals(v(find(candidate, key).getJSONArray("position")), v(review.getJSONArray("toM"))))
            throw new IllegalStateException("The crate did not move to the exact reviewed coordinates.");
        if (!actual.getString("title").equals(revision(parent, review).getString("title")))
            throw new IllegalStateException("The reviewed revision label changed.");
        actual.put("title", source.getString("title"));
        find(actual,key).put("position", find(source,key).getJSONArray("position"));
        if (!actual.similar(source)) throw new IllegalStateException("The edit would change a shelf or another unapproved field.");
    }

    static JSONObject find(JSONObject scene, String sourceId) {
        for (Object raw : scene.getJSONArray("objects")) {
            var object = (JSONObject) raw;
            if (sourceId.equals(object.getString("sourceId"))) return object;
        }
        throw new IllegalStateException("A reviewed scene object is missing: " + sourceId);
    }

    private static boolean vEquals(double[] actual, double[] expected) { return norm(sub(actual, expected)) <= .000001; }

    static JSONObject measure(JSONObject parent, JSONObject candidate, JSONObject review, Path nativeDir) throws Exception {
        checkScope(parent, candidate, review);
        var delta = sub(v(review.getJSONArray("toM")), v(review.getJSONArray("fromM")));
        var normal = v(review.getJSONArray("directionM"));
        double distance = norm(delta), forward = dot(delta,normal);
        double lateral = dot(delta,new double[]{normal[2],0,-normal[0]});
        boolean guided = review.has("intent");
        double expectedLateral = review.has("alternate") ? review.getJSONObject("alternate").getDouble("lateralM") : 0;
        if (Math.abs(forward-MOVE) > .000001 || Math.abs(lateral-expectedLateral) > .000001 ||
                Math.abs(delta[1]) > .000001 || Math.abs(expectedLateral) > .650001)
            throw new IllegalStateException("The move must match the reviewed 0.40 m forward and lateral offsets.");
        var aisle = review.getJSONObject("aisle");
        int bodiesChecked = 0;
        try (var world = new SpatialPhysics(parent, nativeDir)) {
            bodiesChecked += verifyAisle(world, review, "initial-aisle");
            verifyPath(world, parent, review);
        }
        try (var world = new SpatialPhysics(candidate, nativeDir)) {
            bodiesChecked += verifyAisle(world, review, "aisle");
            String crateId = find(candidate, review.getString("crate")).getString("id");
            String floorId = find(candidate, aisle.getString("floor")).getString("id");
            var crate = world.bodies.get(crateId);
            var floor = world.bodies.get(floorId);
            if (guided) verifyLandingClear(world,crate,floor);
            int settledTicks = 0;
            for (int tick = 1; tick <= 180; tick++) {
                world.step();
                if (tick < 121) continue;
                boolean[] supported = {false};
                world.space.pairTest(crate, floor, hit -> supported[0] |= hit.getDistance1() <= .003f);
                var p = crate.getPhysicsLocation();
                var expected = v(review.getJSONArray("toM"));
                if (!supported[0] || Math.hypot(p.x - expected[0], p.z - expected[2]) > .006 ||
                        Math.abs(p.y - expected[1]) > .025 || crate.getLinearVelocity().length() > .025)
                    throw new Obstruction("support","The translated crate did not settle on the loading bay floor.");
                settledTicks++;
            }
            if (guided) verifyLandingClear(world,crate,floor);
            bodiesChecked += verifyAisle(world, review, "settled-aisle");
            var result = new JSONObject().put("passed", true).put("translationM", Math.round(distance*1e6)/1e6)
                    .put("nativeAisleChecks", 3).put("obstaclePairsChecked", bodiesChecked)
                    .put("widthM",WIDTH).put("heightM",HEIGHT).put("lengthM",aisle.getDouble("lengthM"))
                    .put("crateSupportedTicks", settledTicks).put("sweptPathClear", true)
                    .put("changedObjects", new JSONArray().put(review.getString("crate")))
                    .put("shelvesPreserved", review.getJSONArray("protected").length())
                    .put("candidateSourceSha256", candidate.getString("sourceSha256"));
            if (guided) result.put("forwardM",MOVE).put("lateralM",expectedLateral)
                    .put("aisleSweepClear",true).put("destinationClear",true);
            return result;
        }
    }

    private static void verifyLandingClear(SpatialPhysics world, PhysicsRigidBody crate, PhysicsRigidBody floor) {
        for (Object raw : world.scene.getJSONArray("objects")) {
            var body = (JSONObject)raw;
            var other = world.bodies.get(body.getString("id"));
            if (other == crate || other == floor) continue;
            boolean[] overlap = {false};
            world.space.pairTest(crate,other,hit -> overlap[0] |= hit.getDistance1() < -.001f);
            if (overlap[0])
                throw new Obstruction("destination","The crate overlaps " + body.getString("label") + " at its destination.");
        }
    }

    private static PhysicsRigidBody aisleProbe(JSONObject review) {
        var aisle = review.getJSONObject("aisle");
        var middle = mul(add(v(aisle.getJSONArray("entryM")), v(aisle.getJSONArray("endM"))), .5);
        middle[1] += BASE_GAP + HEIGHT / 2;
        var direction = v(review.getJSONArray("directionM"));
        var probe = new PhysicsRigidBody(new BoxCollisionShape((float)(WIDTH/2), (float)(HEIGHT/2),
                (float)(aisle.getDouble("lengthM")/2)), 0);
        probe.setPhysicsLocation(SpatialPhysics.vec(middle));
        probe.setPhysicsRotation(new Quaternion().fromAngleAxis((float)Math.atan2(direction[0],direction[2]),
                com.jme3.math.Vector3f.UNIT_Y));
        return probe;
    }

    private static int verifyAisle(SpatialPhysics world, JSONObject review, String gate) {
        var probe = aisleProbe(review);
        world.space.addCollisionObject(probe);
        try {
            for (Object raw : world.scene.getJSONArray("objects")) {
                var body = (JSONObject)raw;
                boolean[] collision = {false};
                world.space.pairTest(probe, world.bodies.get(body.getString("id")),
                        hit -> collision[0] |= hit.getDistance1() < -.001f);
                if (collision[0]) throw new Obstruction(gate,"The 1 m aisle is blocked by " + body.getString("label") + ".");
            }
            return world.bodies.size();
        } finally {
            world.space.removeCollisionObject(probe);
        }
    }

    private static void verifyPath(SpatialPhysics world, JSONObject parent, JSONObject review) {
        var crate = find(parent, review.getString("crate"));
        var size = v(crate.getJSONArray("size"));
        var q = crate.getJSONArray("rotation");
        var r = new Quaternion(q.getFloat(0),q.getFloat(1),q.getFloat(2),q.getFloat(3));
        var floor = find(parent, review.getJSONObject("aisle").getString("floor"));
        var box = new BoxCollisionShape((float)(size[0]/2), (float)(size[1]/2), (float)(size[2]/2));
        var source = v(review.getJSONArray("fromM"));
        var target = v(review.getJSONArray("toM"));
        // Lift only the clearance sweep a few millimetres to keep intentional floor contact out of the query.
        source[1] += .005;
        target[1] += .005;
        var hits = world.space.sweepTest(box, new Transform(SpatialPhysics.vec(source),r),
                new Transform(SpatialPhysics.vec(target),r));
        var blocked = new ArrayList<String>();
        for (Object raw : parent.getJSONArray("objects")) {
            var other = (JSONObject) raw;
            String id = other.getString("id");
            if (id.equals(crate.getString("id")) || id.equals(floor.getString("id"))) continue;
            if (hits.stream().anyMatch(hit -> hit.getCollisionObject() == world.bodies.get(id)))
                blocked.add(other.getString("label"));
        }
        if (!blocked.isEmpty())
            throw new Obstruction("path","The crate path is blocked by " + String.join(", ", blocked) + ".");
        if (!review.has("intent")) return;
        var probe = aisleProbe(review);
        world.space.addCollisionObject(probe);
        try {
            if (world.space.sweepTest(box,new Transform(SpatialPhysics.vec(source),r),
                    new Transform(SpatialPhysics.vec(target),r)).stream()
                    .anyMatch(hit -> hit.getCollisionObject() == probe))
                throw new Obstruction("sweep-aisle","The crate would cross the 1 m aisle during the move.");
        } finally {
            world.space.removeCollisionObject(probe);
        }
    }

    static JSONObject fixture(int yaw, String obstruction) {
        if (yaw != 0 && yaw != 37) throw new IllegalArgumentException("Choose the square or 37° loading bay.");
        if (!List.of("none","aisle","path").contains(obstruction)) throw new IllegalArgumentException("Unknown fixture.");
        double angle = Math.toRadians(yaw);
        double[] q = {0,Math.sin(angle/2),0,Math.cos(angle/2)};
        var rotation = new JSONArray(q);
        var objects = new JSONArray()
                .put(box("floor","Loading bay floor",new double[]{0,-.10,1.60},new double[]{6,.20,5.5},0,rotation))
                .put(box("door","Loading door",new double[]{0,2.14,0},new double[]{1.5,.24,.16},0,rotation))
                .put(box("jamb-left","Left door jamb",new double[]{-.64,1,0},new double[]{.16,2,.16},0,rotation))
                .put(box("jamb-right","Right door jamb",new double[]{.64,1,0},new double[]{.16,2,.16},0,rotation))
                .put(box("aisle-end","Aisle end",new double[]{0,-.015,3.6},new double[]{1,.05,.10},0,rotation))
                .put(box("cargo","Selected crate",new double[]{.97,.30,1.08},new double[]{.60,.60,.60},14,rotation))
                .put(box("spare","Spare crate",new double[]{-1.03,.30,2.26},new double[]{.60,.60,.60},14,rotation))
                .put(box("shelf-east","East shelf",new double[]{1.98,.75,1.42},new double[]{.53,1.5,1.65},0,rotation))
                .put(box("shelf-west","West shelf",new double[]{-1.92,.75,2.46},new double[]{.53,1.5,1.6},0,rotation));
        if ("aisle".equals(obstruction))
            objects.put(box("trolley","Parked trolley",new double[]{.12,.32,2.08},new double[]{.52,.64,.42},0,rotation));
        if ("path".equals(obstruction))
            objects.put(box("trolley","Parked trolley",new double[]{.97,.32,1.91},new double[]{.52,.64,.42},0,rotation));
        return new JSONObject().put("format","fraynix.spatial-scene").put("version",1)
                .put("title", (yaw == 0 ? "Loading Bay" : "Loading Bay · 37°") + (obstruction.equals("none") ? "" : " · Obstructed " + obstruction))
                .put("units","m").put("frame","right-handed-y-up").put("gravityMps2",9.81)
                .put("source",new JSONObject().put("kind","synthetic").put("physicalProperties","declared")
                        .put("note","Authored loading bay for scoped translation and native aisle clearance."))
                .put("scaleReference",new JSONObject().put("a",array(new double[]{0,0,0}))
                        .put("b",array(new double[]{0,1,0})).put("knownLengthM",1).put("toleranceM",0).put("basis","declared-test"))
                .put("objects",objects);
    }

    static JSONObject box(String id, String label, double[] p, double[] size, double mass, JSONArray rotation) {
        return new JSONObject().put("sourceId",id).put("label",label).put("shape","box")
                .put("position",array(rotate(p,rotation,false))).put("rotation",rotation).put("size",array(size))
                .put("massKg",mass).put("friction",.6).put("restitution",0);
    }
}
