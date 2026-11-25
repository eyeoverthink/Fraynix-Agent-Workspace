
package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class PlacementExchange {
    static final int BYTE_LIMIT = 1_500_000;
    static final int AUTHORED_BYTE_LIMIT = 1_200_000;
    private static final int TICKS = 180, INTERVAL = 12;
    private static final double POSITION_M = .010, VELOCITY_MPS = .050, SPEED_MPS = .015, SEPARATION_M = .003;
    private static final String FORMAT = "fraynix.verified-placement-export-v1";
    private final Path directory, nativeDir;

    record Stage(JSONObject scene, JSONObject evidence, String propKey, boolean authored) {}

    record Bundle(String id, Stage stage, String glbSha256, JSONObject layout, boolean destinationVerified,
                  List<Stage> previous, int handoffDepth) {
        JSONObject scene() { return stage.scene; }
        JSONObject evidence() { return stage.evidence; }
        boolean authored() { return stage.authored; }
        boolean recipient() { return !previous.isEmpty(); }
        String originalArchiveSha256() {
            if (!recipient()) return id;
            var branch = scene().getJSONObject("recipientBranch");
            return branch.optString("originalArchiveSha256", branch.getString("archiveSha256"));
        }
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
        var previous = new JSONArray();
        for (var stage : original.previous) previous.put(verify(stage));
        var result = verify(original.stage);
        var saved = new JSONObject().put("bundleSha256",original.id)
                .put("trials",result.getJSONObject("trials")).put("comparison",result.getJSONObject("comparison"));
        if (original.recipient()) saved.put("previous",previous);
        saved.put("sha256",WorldFrames.hash(saved));
        byte[] bytes = saved.toString().getBytes(StandardCharsets.UTF_8);
        if (bytes.length > BYTE_LIMIT) throw new IllegalStateException("The local receipt exceeds the exchange limit.");
        SpriteWorkshop.replace(file(original.id,".replay.json"),bytes);
        return status(original,saved);
    }

    JSONObject compareCollider(JSONObject input, boolean sweep) throws Exception {
        SpatialScene.keys(input,"id","replaySha256");
        var bundle = load(input.getString("id"));
        var replay = localReplay(bundle);
        if (replay == null || !input.getString("replaySha256").equals(replay.getString("sha256")))
            throw new IllegalStateException("Replay the exact bundle locally before comparing colliders.");
        var plan = ColliderApproachComparison.plan(bundle,replay);
        if (plan == null) throw new IllegalArgumentException("This archive has no reviewed collider edit to compare.");
        var receipt = sweep ? new ColliderApproachSweep(plan).run(nativeDir) : plan.run(nativeDir);
        SpriteWorkshop.replace(file(bundle.id,sweep ? ".sweep.json" : ".comparison.json"),
                receipt.toString().getBytes(StandardCharsets.UTF_8));
        return receipt;
    }

    private JSONObject verify(Stage stage) throws Exception {
        var review = stage.evidence.getJSONObject("review");
        var fresh = run(stage,review);
        var repeat = run(stage,review);
        if (!fresh.similar(repeat)) throw new IllegalStateException("The two local native runs did not agree.");
        return new JSONObject().put("sceneId",stage.scene.getString("id")).put("trials",fresh)
                .put("comparison",compare(stage.evidence.getJSONObject("measured"),fresh,stage.authored));
    }

    private JSONObject run(Stage stage, JSONObject review) throws Exception {
        if (stage.propKey != null) return SpatialLevelPlacement.verifyNative(stage.scene,review,nativeDir,stage.propKey);
        return stage.authored ? SpatialLevelPlacement.verifyNative(stage.scene,review,nativeDir)
                : SpatialPlacement.verifyNative(stage.scene,review,nativeDir);
    }

    record ForkSource(Bundle bundle, JSONObject replay) {}

    ForkSource forkSource(String archive, String replayHash) throws Exception {
        var bundle = load(archive);
        if (!bundle.destinationVerified())
            throw new IllegalArgumentException("Create a local copy from an authored bundle with its checked original destination.");
        var checked = localReplay(bundle);
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

    static boolean validReplay(Bundle bundle, JSONObject receipt) {
        SpatialScene.keys(receipt,"bundleSha256","trials","comparison","sha256","previous");
        var sealed = WorldFrames.copy(receipt);
        String hash = receipt.getString("sha256");
        sealed.remove("sha256");
        if (!bundle.id.equals(receipt.getString("bundleSha256")) || !WorldFrames.hash(sealed).equals(hash)
                || !matches(bundle.stage,receipt) || receipt.has("previous") != bundle.recipient())
            return false;
        if (!bundle.recipient()) return true;
        var previous = receipt.getJSONArray("previous");
        if (previous.length() != bundle.previous.size()) return false;
        for (int i=0;i<previous.length();i++) {
            var stage = bundle.previous.get(i);
            var archived = previous.getJSONObject(i);
            SpatialScene.keys(archived,"sceneId","trials","comparison");
            if (!stage.scene.getString("id").equals(archived.getString("sceneId")) || !matches(stage,archived)) return false;
        }
        return true;
    }

    private static boolean matches(Stage stage, JSONObject checked) {
        return compare(stage.evidence.getJSONObject("measured"),checked.getJSONObject("trials"),stage.authored)
                .similar(checked.getJSONObject("comparison"));
    }

    byte[] archiveBytes(String id) throws Exception {
        return SpriteWorkshop.read(file(id,".json"),BYTE_LIMIT,id);
    }

    private Bundle load(String id) throws Exception {
        return inspect(archiveBytes(id));
    }

    private Path file(String id, String extension) {
        if (!id.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Invalid placement bundle ID.");
        return directory.resolve(id + extension);
    }

    private JSONObject status(Bundle bundle) throws Exception {
        return status(bundle,localReplay(bundle));
    }

    private JSONObject status(Bundle bundle, JSONObject receipt) {
        var summary = summarize(bundle);
        if (receipt != null) {
            summary.put("replay",receipt);
            var plan = ColliderApproachComparison.plan(bundle,receipt);
            if (plan != null) {
                summary.put("colliderComparisonReady",true);
                Path comparison = file(bundle.id,".comparison.json");
                if (Files.exists(comparison,LinkOption.NOFOLLOW_LINKS)) {
                    var retained = plan.read(comparison);
                    if (retained == null) summary.put("colliderComparisonUnavailable",true);
                    else summary.put("colliderComparison",retained);
                }
                Path sweep = file(bundle.id,".sweep.json");
                if (Files.exists(sweep,LinkOption.NOFOLLOW_LINKS)) {
                    var retained = new ColliderApproachSweep(plan).read(sweep);
                    if (retained == null) summary.put("colliderSweepUnavailable",true);
                    else summary.put("colliderSweep",retained);
                }
            }
        } else if (Files.exists(file(bundle.id,".replay.json"),LinkOption.NOFOLLOW_LINKS)) {
            summary.put("replayUnavailable",true);
        }
        return summary;
    }

    private JSONObject localReplay(Bundle bundle) {
        Path path = file(bundle.id,".replay.json");
        if (!Files.exists(path,LinkOption.NOFOLLOW_LINKS)) return null;
        try {
            var receipt = new JSONObject(new String(SpriteWorkshop.read(path,BYTE_LIMIT),StandardCharsets.UTF_8));
            if (validReplay(bundle,receipt)) return receipt;
        } catch (java.io.IOException | RuntimeException changed) {
            // The archive is still available for a fresh replay.
        }
        return null;
    }

    private static JSONObject summarize(Bundle bundle) {
        var result = viewStage(bundle.stage,bundle.layout).put("id",bundle.id)
                .put("title",bundle.scene().getString("title")).put("glbSha256",bundle.glbSha256);
        if (bundle.authored()) result.put("destinationVerified",bundle.destinationVerified);
        if (bundle.recipient()) {
            var original = bundle.previous.getFirst().evidence.getJSONObject("review");
            var past = new JSONArray();
            for (var stage : bundle.previous) {
                var review = stage.evidence.getJSONObject("review");
                String key = stage.propKey == null ? SpatialLevelPlacement.propKey(review) : stage.propKey;
                past.put(viewStage(stage,SpatialLevelPlacement.playable(stage.scene,review,key)));
            }
            result.put("sourceOverlap",original.getDouble("sourceOverlap")).put("previousStages",past)
                    .put("recipientRevision",bundle.previous.size())
                    .put("originalArchiveSha256",bundle.originalArchiveSha256());
            if (bundle.handoffDepth > 0) result.put("handoffCount",bundle.handoffDepth);
        } else result.put("sourceOverlap",bundle.evidence().getJSONObject("review").getDouble("sourceOverlap"));
        return result;
    }

    private static JSONObject viewStage(Stage stage, JSONObject layout) {
        var review = stage.evidence.getJSONObject("review");
        if (stage.propKey != null) {
            var body = SpatialLevelPlacement.propByKey(stage.scene,stage.propKey);
            review = WorldFrames.copy(review).put("contact",SpatialPlacement.contactSpec())
                    .put("colliderM",body.getJSONArray("size"));
        }
        return new JSONObject().put("review",review).put("archived",stage.evidence.getJSONObject("measured"))
                .put("layout",layout);
    }

    static Bundle inspect(byte[] bytes) throws Exception {
        return inspect(bytes,RecipientRevisionExchange.MAX_HANDOFFS);
    }

    static Bundle inspect(byte[] bytes, int remainingHandoffs) throws Exception {
        if (bytes.length == 0 || bytes.length > BYTE_LIMIT)
            throw new IllegalArgumentException("Choose a placement bundle up to 1.5 MB.");
        var input = new JSONObject(new String(bytes,StandardCharsets.UTF_8));
        String format = input.getString("format");
        if (RecipientRevisionExchange.FORMAT.equals(format) || RecipientRevisionExchange.HANDOFF.equals(format)) {
            if (remainingHandoffs < 0)
                throw new IllegalArgumentException("Choose a direct authored archive for the original level.");
            return RecipientRevisionExchange.inspect(input,MapDefinition.sha(bytes),remainingHandoffs);
        }
        if (bytes.length > AUTHORED_BYTE_LIMIT)
            throw new IllegalArgumentException("Choose a placement bundle up to 1.2 MB.");
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
        return new Bundle(MapDefinition.sha(bytes),new Stage(scene,evidence,null,authored),digest,layout,exactDestination,List.of(),0);
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

    static void checkTrials(JSONObject measured, boolean authored) {
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
