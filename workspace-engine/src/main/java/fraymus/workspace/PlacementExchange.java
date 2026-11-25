
package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;

final class PlacementExchange {
    static final int BYTE_LIMIT = 1_200_000;
    private static final int TICKS = 180, INTERVAL = 12;
    private static final double POSITION_M = .010, VELOCITY_MPS = .050, SPEED_MPS = .015, SEPARATION_M = .003;
    private static final String FORMAT = "fraynix.verified-placement-export-v1";
    private final Path directory, nativeDir;

    record Bundle(String id, JSONObject scene, JSONObject evidence, String glbSha256,
                  JSONObject layout, boolean destinationVerified) {
        boolean authored() { return layout != null; }
    }

    PlacementExchange(Path project) {
        directory = project.resolve("spatial/placement-exchanges");
        nativeDir = project.resolve("native");
    }

    JSONObject open(JSONObject input) throws Exception {
        SpatialScene.keys(input, "base64");
        byte[] bytes = SpriteWorkshop.decode(input.getString("base64"), BYTE_LIMIT);
        Bundle checked = inspect(bytes);
        Files.createDirectories(directory);
        Path target = file(checked.id, ".json");
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) SpriteWorkshop.read(target,BYTE_LIMIT,checked.id);
        else SpriteWorkshop.replace(target,bytes);
        SpriteWorkshop.replace(directory.resolve("latest"),checked.id.getBytes(StandardCharsets.US_ASCII));
        return status(checked);
    }

    JSONObject latest(JSONObject input) throws Exception {
        SpatialScene.keys(input);
        Path latest = directory.resolve("latest");
        if (!Files.exists(latest,LinkOption.NOFOLLOW_LINKS)) return new JSONObject();
        String id = new String(SpriteWorkshop.read(latest,64), StandardCharsets.US_ASCII);
        return status(load(id));
    }

    JSONObject replay(JSONObject input) throws Exception {
        SpatialScene.keys(input, "id");
        Bundle original = load(input.getString("id"));
        var review = original.evidence.getJSONObject("review");
        var fresh = run(original,review);
        var repeat = run(original,review);
        if (!fresh.similar(repeat)) throw new IllegalStateException("The two local native runs did not agree.");
        var checked = compare(original.evidence.getJSONObject("measured"), fresh, original.authored());
        var saved = new JSONObject().put("bundleSha256",original.id).put("trials",fresh).put("comparison",checked);
        saved.put("sha256",WorldFrames.hash(saved));
        SpriteWorkshop.replace(file(original.id,".replay.json"),saved.toString().getBytes(StandardCharsets.UTF_8));
        return summarize(original).put("replay",saved);
    }

    private JSONObject run(Bundle bundle, JSONObject review) throws Exception {
        return bundle.authored() ? SpatialLevelPlacement.verifyNative(bundle.scene,review,nativeDir)
                : SpatialPlacement.verifyNative(bundle.scene,review,nativeDir);
    }

    record ForkSource(Bundle bundle, JSONObject replay) {}

    ForkSource forkSource(String archive, String replayHash) throws Exception {
        var bundle = load(archive);
        if (!bundle.authored() || !bundle.destinationVerified())
            throw new IllegalArgumentException("Create a local copy from an authored bundle with its checked original destination.");
        var checked = status(bundle).optJSONObject("replay");
        if (checked == null || !replayHash.equals(checked.getString("sha256")))
            throw new IllegalStateException("Replay this exact bundle locally before creating an editable copy.");
        return new ForkSource(bundle,checked);
    }

    Bundle checkForkSource(String archive, JSONObject replay) throws Exception {
        var bundle = load(archive);
        if (!bundle.destinationVerified() || !validReplay(bundle,replay))
            throw new IllegalStateException("The original bundle or attached local replay changed.");
        return bundle;
    }

    JSONObject attached(String archive, JSONObject replay) throws Exception {
        return summarize(checkForkSource(archive,replay)).put("replay",replay);
    }

    private static boolean validReplay(Bundle bundle, JSONObject receipt) {
        SpatialScene.keys(receipt,"bundleSha256","trials","comparison","sha256");
        var sealed = WorldFrames.copy(receipt);
        String hash = receipt.getString("sha256");
        sealed.remove("sha256");
        return bundle.id.equals(receipt.getString("bundleSha256")) && WorldFrames.hash(sealed).equals(hash)
                && compare(bundle.evidence.getJSONObject("measured"),receipt.getJSONObject("trials"),bundle.authored())
                   .similar(receipt.getJSONObject("comparison"));
    }

    private Bundle load(String id) throws Exception {
        return inspect(SpriteWorkshop.read(file(id,".json"),BYTE_LIMIT,id));
    }

    private Path file(String id, String extension) {
        if (!id.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Invalid placement bundle ID.");
        return directory.resolve(id + extension);
    }

    private JSONObject status(Bundle bundle) throws Exception {
        var summary = summarize(bundle);
        Path path = file(bundle.id,".replay.json");
        if (!Files.exists(path,LinkOption.NOFOLLOW_LINKS)) return summary;
        try {
            var receipt = new JSONObject(new String(SpriteWorkshop.read(path,BYTE_LIMIT),StandardCharsets.UTF_8));
            if (validReplay(bundle,receipt)) return summary.put("replay",receipt);
        } catch (java.io.IOException | RuntimeException changed) {
            // The original archive was already checked. The user can regenerate this local replay.
        }
        return summary.put("replayUnavailable",true);
    }

    private static JSONObject summarize(Bundle bundle) {
        var review = bundle.evidence.getJSONObject("review");
        var result = new JSONObject().put("id",bundle.id).put("title",bundle.scene.getString("title"))
                .put("review",review).put("archived",bundle.evidence.getJSONObject("measured"))
                .put("sourceOverlap",review.getDouble("sourceOverlap"))
                .put("glbSha256",bundle.glbSha256);
        if (bundle.authored()) result.put("layout",bundle.layout).put("destinationVerified",bundle.destinationVerified);
        return result;
    }

    static Bundle inspect(byte[] bytes) throws Exception {
        if (bytes.length == 0 || bytes.length > BYTE_LIMIT)
            throw new IllegalArgumentException("Choose a placement bundle up to 1.2 MB.");
        var input = new JSONObject(new String(bytes,StandardCharsets.UTF_8));
        String format = input.getString("format");
        boolean exactDestination = SpatialLevelPlacement.ARCHIVE.equals(format);
        boolean authored = exactDestination || SpatialLevelPlacement.LEGACY_ARCHIVE.equals(format);
        if (authored) {
            SpatialScene.keys(input,"format","sceneId","scene","destinationSourceText",
                    "evidence","evidenceSha256","glbSha256","glbBase64");
            if (input.has("destinationSourceText") != exactDestination)
                throw new IllegalArgumentException("The authored destination source does not match the archive format.");
        } else {
            SpatialScene.keys(input,"format","sceneId","scene","sourceReconstruction",
                    "evidence","evidenceSha256","glbSha256","glbBase64");
            if (!FORMAT.equals(format) || bytes.length > 1_000_000)
                throw new IllegalArgumentException("Choose a Fraynix scene and contact evidence bundle.");
        }
        var evidence = input.getJSONObject("evidence");
        SpatialScene.keys(evidence,"reviewId","review","measured","repeat");
        var review = evidence.getJSONObject("review");
        String id = evidence.getString("reviewId");
        String expectedScene = authored ? SpatialLevelPlacement.sceneId(id) : SpatialPlacement.sceneId(id);
        if (!WorldFrames.hash(evidence).equals(input.getString("evidenceSha256")) || !WorldFrames.hash(review).equals(id)
                || !expectedScene.equals(input.getString("sceneId")))
            throw new IllegalArgumentException("The bundled review or collision evidence checksum differs.");
        var measured = evidence.getJSONObject("measured");
        if (!measured.similar(evidence.getJSONObject("repeat")))
            throw new IllegalArgumentException("The archived collision repeats differ.");
        checkTrials(measured,authored);

        var raw = input.getJSONObject("scene");
        var scene = SpatialScene.parse(raw.toString(),expectedScene);
        JSONObject layout = null;
        if (authored) {
            layout = checkLevel(raw,scene,review);
            if (Math.abs(layout.getDouble("routeLengthM") - measured.getDouble("routeLengthM")) > 1e-9)
                throw new IllegalArgumentException("The archived route length differs from the reviewed aisle.");
            if (exactDestination) checkDestination(raw,review,input.getString("destinationSourceText"));
        } else {
            checkContract(raw,scene,review,input.getJSONObject("sourceReconstruction"));
        }
        byte[] glb = SpriteWorkshop.decode(input.getString("glbBase64"),SpatialGlb.LIMIT);
        String digest = MapDefinition.sha(glb);
        if (!digest.equals(input.getString("glbSha256"))) throw new IllegalArgumentException("The embedded GLB checksum differs.");
        SpatialGlb.verifyScene(scene,glb);
        return new Bundle(MapDefinition.sha(bytes),scene,evidence,digest,layout,exactDestination);
    }

    private static JSONObject checkLevel(JSONObject raw, JSONObject scene, JSONObject review) {
        SpatialScene.keys(review,"format","goal","source","sourceTitle","objectId","meshSha256","sourceOverlap",
                "colliderM","targetM","yawDeg","rotation","contact","destination","destinationTitle","floor","floorLabel",
                "aisle","browserReference","editReference");
        if (!SpatialLevelPlacement.FORMAT.equals(review.getString("format"))
                || !review.getJSONObject("contact").similar(SpatialPlacement.contactSpec())
                || !"m".equals(raw.getString("units")) || scene.has("translationAnimation"))
            throw new IllegalArgumentException("Unsupported authored-level contact protocol.");
        SpatialScene.text(review,"goal",300);
        SpatialScene.text(review,"sourceTitle",120);
        UUID.fromString(review.getString("objectId"));
        double overlap = SpatialScene.number(review,"sourceOverlap",.86,1);
        for (String role : Set.of("source","destination")) {
            var identity = review.getJSONObject(role);
            SpatialScene.keys(identity,"sceneId","sourceSha256","definitionSha256");
            UUID.fromString(identity.getString("sceneId"));
            for (String hash : Set.of("sourceSha256","definitionSha256"))
                if (!identity.getString(hash).matches("[0-9a-f]{64}"))
                    throw new IllegalArgumentException("Invalid "+role+" evidence identity.");
        }
        if (review.getJSONObject("source").getString("sceneId").equals(review.getJSONObject("destination").getString("sceneId"))
                || !scene.getString("title").equals(review.getString("destinationTitle")))
            throw new IllegalArgumentException("The authored scene differs from the reviewed destination.");

        double halfYaw = Math.toRadians(SpatialScene.number(review,"yawDeg",-180,180))/2;
        double[] yaw = {0,Math.sin(halfYaw),0,Math.cos(halfYaw)};
        var rotation = review.getJSONArray("rotation");
        if (rotation.length()!=yaw.length) throw new IllegalArgumentException("The reviewed rotation needs four values.");
        for (int i=0;i<yaw.length;i++) {
            if (!(rotation.get(i) instanceof Number value) || Math.abs(value.doubleValue()-yaw[i])>1e-12)
                throw new IllegalArgumentException("The authored rotation disagrees with the reviewed yaw.");
        }
        SpatialScene.vector(review.getJSONArray("colliderM"),2);
        SpatialScene.vector(review.getJSONArray("targetM"),20);
        String mesh = review.getString("meshSha256");
        var prop = SpatialLevelPlacement.prop(scene,review);
        if (!rotation.similar(prop.getJSONArray("rotation"))
                || !mesh.equals(prop.optString("visualMesh")) || prop.getDouble("massKg") <= 0
                || !prop.getJSONArray("position").similar(review.getJSONArray("targetM"))
                || !prop.getJSONArray("size").similar(review.getJSONArray("colliderM")))
            throw new IllegalArgumentException("The authored prop disagrees with the reviewed pose or collider.");

        var edit = review.optJSONObject("editReference");
        var browser = review.optJSONObject("browserReference");
        if (edit != null && (browser != null || !mesh.equals(edit.getString("meshSha256"))
                || SpatialScene.number(edit,"heldOutIou",.86,1) != overlap))
            throw new IllegalArgumentException("The edit witness and reviewed mesh disagree.");
        if (browser != null && SpatialScene.number(browser,"heldOutIou",.86,1) != overlap)
            throw new IllegalArgumentException("The archived view evidence and review disagree.");
        return SpatialLevelPlacement.playable(scene,review);
    }

    private static void checkDestination(JSONObject scene, JSONObject review, String destinationText) {
        var binding = review.getJSONObject("destination");
        if (!MeasurementStore.hash(destinationText).equals(binding.getString("sourceSha256")))
            throw new IllegalArgumentException("The original destination source checksum differs.");
        SpatialScene.parse(destinationText,binding.getString("sceneId"));
        var original = new JSONObject(destinationText);
        var restored = WorldFrames.copy(scene);
        var objects = restored.getJSONArray("objects");
        var added = objects.getJSONObject(objects.length()-1);
        if (!SpatialLevelPlacement.propKey(review).equals(added.getString("sourceId")))
            throw new IllegalArgumentException("The reviewed prop is not the only appended object.");
        objects.remove(objects.length()-1);
        var meshes = restored.getJSONObject("visualMeshes");
        var before = original.optJSONObject("visualMeshes",new JSONObject());
        if (!before.has(review.getString("meshSha256"))) meshes.remove(review.getString("meshSha256"));
        if (meshes.isEmpty() && !original.has("visualMeshes")) restored.remove("visualMeshes");
        if (!original.similar(restored))
            throw new IllegalArgumentException("Existing destination geometry or properties changed in the authored scene.");
    }

    private static void checkTrials(JSONObject measured, boolean authored) {
        if (authored) {
            SpatialScene.keys(measured,"passed","hit","miss","playerEnvelopeClear","routeLengthM");
            if (!measured.getBoolean("playerEnvelopeClear"))
                throw new IllegalArgumentException("The trial has no passing aisle clearance.");
            SpatialScene.number(measured,"routeLengthM",.5,40);
        } else SpatialScene.keys(measured,"passed","hit","miss");
        if (!measured.getBoolean("passed")) throw new IllegalArgumentException("The trial has no passing collision evidence.");
        trial(measured.getJSONObject("hit"),true,authored);
        trial(measured.getJSONObject("miss"),false,authored);
    }

    private static void checkContract(JSONObject raw, JSONObject scene, JSONObject review, JSONObject proof) {
        SpatialScene.keys(review,"format","goal","source","sourceTitle","objectId","meshSha256","sourceOverlap",
                "colliderM","targetM","yawDeg","rotation","contact");
        if (!"fraynix.spatial-placement-v1".equals(review.getString("format"))
                || !review.getJSONObject("contact").similar(SpatialPlacement.contactSpec()))
            throw new IllegalArgumentException("Unsupported contact trial protocol.");
        SpatialScene.text(review,"goal",300);
        SpatialScene.text(review,"sourceTitle",120);
        UUID.fromString(review.getString("objectId"));
        var source = review.getJSONObject("source");
        SpatialScene.keys(source,"sceneId","sourceSha256","definitionSha256");
        UUID.fromString(source.getString("sceneId"));
        for (String key : Set.of("sourceSha256","definitionSha256")) {
            if (!source.getString(key).matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Invalid source evidence identity.");
        }
        String mesh = review.getString("meshSha256");
        if (!BrowserViewTrace.POLICY.equals(proof.getString("policy"))
                || !mesh.equals(proof.getString("meshSha256"))
                || !source.getString("sourceSha256").equals(proof.getString("definitionSha256"))
                || SpatialScene.number(proof,"heldOutIou",.86,1) != SpatialScene.number(review,"sourceOverlap",.86,1))
            throw new IllegalArgumentException("The bundled source reconstruction and review disagree.");

        var boxes = scene.getJSONArray("objects");
        double yaw = SpatialScene.number(review,"yawDeg",-180,180);
        double angle = Math.toRadians(yaw)/2;
        var collider = SpatialScene.vector(review.getJSONArray("colliderM"),2);
        var target = SpatialScene.vector(review.getJSONArray("targetM"),2);
        var expectedRotation = new double[]{0,Math.sin(angle),0,Math.cos(angle)};
        JSONArray rotation = review.getJSONArray("rotation");
        var expectedDeck = SpatialReconstruction.box("contact-deck","Contact test deck",
                new double[]{0,-.10,0},new double[]{10,.20,10},0);
        if (boxes.length() != 2 || !raw.getJSONArray("objects").getJSONObject(0).similar(expectedDeck)
                || !"m".equals(raw.getString("units")) || scene.has("translationAnimation")
                || !scene.getJSONObject("visualMeshes").keySet().equals(Set.of(mesh))
                || !("Reviewed placement · " + review.getString("sourceTitle")).equals(scene.getString("title"))
                || rotation.length() != 4 || target[1] != collider[1]/2)
            throw new IllegalArgumentException("The exported scene disagrees with the reviewed contact deck.");
        for (int i=0;i<4;i++) if (Math.abs(rotation.getDouble(i)-expectedRotation[i]) > 1e-12)
            throw new IllegalArgumentException("The exported rotation disagrees with the reviewed yaw.");
        var prop = boxes.getJSONObject(1);
        if (!"placed-prop".equals(prop.getString("sourceId")) || prop.getDouble("massKg") <= 0
                || !mesh.equals(prop.getString("visualMesh")) || !prop.getJSONArray("size").similar(review.getJSONArray("colliderM"))
                || !prop.getJSONArray("position").similar(review.getJSONArray("targetM"))
                || !prop.getJSONArray("rotation").similar(rotation))
            throw new IllegalArgumentException("The exported prop disagrees with the reviewed pose or collider.");
    }

    private static void trial(JSONObject value, boolean hit, boolean authored) {
        SpatialScene.keys(value,"firstContactTick","contactTicks","firstContactSeparationM","probeRadiusM","probeStartM",
                "probeIncomingMps","maxPropPlanarSpeedMps","sampleIntervalTicks","samples");
        int first = SpriteWorkshop.integer(value,"firstContactTick",0,TICKS);
        int contacts = SpriteWorkshop.integer(value,"contactTicks",0,TICKS);
        double speed = SpatialScene.number(value,"maxPropPlanarSpeedMps",0,20);
        if (hit ? first == 0 || contacts == 0 || contacts > TICKS-first+1 || speed < .02
                : first != 0 || contacts != 0 || speed > .01 || !value.isNull("firstContactSeparationM"))
            throw new IllegalArgumentException("The archived hit or miss does not meet the contact protocol.");
        if (hit) SpatialScene.number(value,"firstContactSeparationM",-.25,0);
        SpatialScene.number(value,"probeRadiusM",.01,.036);
        SpatialScene.vector(value.getJSONArray("probeStartM"),authored ? 22 : 20);
        SpatialScene.number(value,"probeIncomingMps",1.79,1.81);
        SpriteWorkshop.integer(value,"sampleIntervalTicks",INTERVAL,INTERVAL);
        var ticks = new ArrayList<Integer>();
        for (int i=0;i<=TICKS;i+=INTERVAL) {
            if (first>i-INTERVAL && first<i && first>0) ticks.add(first);
            ticks.add(i);
        }
        var samples = value.getJSONArray("samples");
        if (samples.length()!=ticks.size()) throw new IllegalArgumentException("The archived trace is missing samples.");
        for (int i=0;i<ticks.size();i++) {
            var sample = samples.getJSONObject(i);
            SpatialScene.keys(sample,"tick","probeM","propM","probeVelocityMps","propVelocityMps");
            SpriteWorkshop.integer(sample,"tick",ticks.get(i),ticks.get(i));
            for (String key : Set.of("probeM","propM","probeVelocityMps","propVelocityMps"))
                SpatialScene.vector(sample.getJSONArray(key),50);
        }
    }

    private static JSONObject compare(JSONObject archived, JSONObject local, boolean authored) {
        checkTrials(local,authored);
        var hit = compareTrial(archived.getJSONObject("hit"),local.getJSONObject("hit"),true);
        var miss = compareTrial(archived.getJSONObject("miss"),local.getJSONObject("miss"),false);
        var result = new JSONObject().put("passed",true).put("hit",hit).put("miss",miss)
                .put("positionToleranceM",POSITION_M).put("velocityToleranceMps",VELOCITY_MPS)
                .put("contactTickTolerance",1).put("contactDurationToleranceTicks",3)
                .put("peakSpeedToleranceMps",SPEED_MPS).put("contactSeparationToleranceM",SEPARATION_M);
        if (authored) {
            double routeError = Math.abs(archived.getDouble("routeLengthM") - local.getDouble("routeLengthM"));
            if (routeError > 1e-9) throw new IllegalStateException("The local aisle clearance differs from the archive.");
            result.put("aisle",new JSONObject().put("passed",true).put("routeLengthErrorM",routeError));
        }
        return result;
    }

    private static JSONObject compareTrial(JSONObject archived, JSONObject local, boolean hit) {
        int first = Math.abs(archived.getInt("firstContactTick")-local.getInt("firstContactTick"));
        int duration = Math.abs(archived.getInt("contactTicks")-local.getInt("contactTicks"));
        double speed = Math.abs(archived.getDouble("maxPropPlanarSpeedMps")-local.getDouble("maxPropPlanarSpeedMps"));
        double separation = hit ? Math.abs(archived.getDouble("firstContactSeparationM")-local.getDouble("firstContactSeparationM")) : 0;
        double position = 0, velocity = 0;
        var a = archived.getJSONArray("samples");
        var b = local.getJSONArray("samples");
        var samples = new java.util.HashMap<Integer,JSONObject>();
        for (Object row : b) {
            var sample = (JSONObject)row;
            samples.put(sample.getInt("tick"),sample);
        }
        for (Object row : a) {
            var sample = (JSONObject)row;
            int tick = sample.getInt("tick");
            var other = tick % INTERVAL == 0 ? samples.get(tick) : samples.get(local.getInt("firstContactTick"));
            position = Math.max(position,Math.max(delta(sample,other,"probeM"),delta(sample,other,"propM")));
            velocity = Math.max(velocity,Math.max(delta(sample,other,"probeVelocityMps"),delta(sample,other,"propVelocityMps")));
        }
        if (first>1 || duration>3 || speed>SPEED_MPS || separation>SEPARATION_M || position>POSITION_M || velocity>VELOCITY_MPS
                || delta(archived,local,"probeStartM")>1e-5
                || Math.abs(archived.getDouble("probeIncomingMps")-local.getDouble("probeIncomingMps"))>1e-6
                || Math.abs(archived.getDouble("probeRadiusM")-local.getDouble("probeRadiusM"))>1e-6)
            throw new IllegalStateException("The local " + (hit ? "hit" : "miss") + " replay differs from the archived measurements.");
        return new JSONObject().put("firstContactTickError",first).put("contactDurationErrorTicks",duration)
                .put("peakSpeedErrorMps",speed).put("firstSeparationErrorM",separation)
                .put("maxPositionErrorM",position).put("maxVelocityErrorMps",velocity);
    }

    private static double delta(JSONObject a, JSONObject b, String key) {
        return ObserverGeometry.norm(ObserverGeometry.sub(ObserverGeometry.v(a.getJSONArray(key)),ObserverGeometry.v(b.getJSONArray(key))));
    }
}
