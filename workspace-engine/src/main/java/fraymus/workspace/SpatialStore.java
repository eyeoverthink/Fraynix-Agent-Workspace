package fraymus.workspace;

import org.json.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Source-qualified imports and bounded native manipulation receipts, isolated from the ramp lab. */
public final class SpatialStore implements AutoCloseable {
    private final SpatialCarryStore carries;
    private final TileWorkshop library;
    private final SpatialPlacement placements;
    private final PlacementExchange exchange;
    final HeightStudyStore heightStudies;
    private final Path file,nativeDir;private final LinkedHashMap<String,JSONObject> scenes=new LinkedHashMap<>(),runs=new LinkedHashMap<>();
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"spatial-native-owner");t.setDaemon(true);return t;});
    private final AtomicBoolean cancelled=new AtomicBoolean();private String active="";private JSONObject current;private volatile boolean closed;
    public SpatialStore(Path project) throws Exception { this(project, new TileWorkshop(project)); }
    SpatialStore(Path project, TileWorkshop library) throws Exception {this.library=library;placements=new SpatialPlacement(project);exchange=new PlacementExchange(project);carries=new SpatialCarryStore(project);heightStudies=new HeightStudyStore(project);file=project.resolve("spatial/state.json");nativeDir=project.resolve("native");if(Files.exists(file)){var data=new JSONObject(Files.readString(file));for(Object raw:data.getJSONArray("scenes")){var s=(JSONObject)raw;validateDefinition(s);scenes.put(s.getString("id"),s);}for(Object raw:data.getJSONArray("runs")){var r=(JSONObject)raw;if(Set.of("executing","prepared").contains(r.getString("status")))r.put("status","interrupted").put("reason","Process ended; resume creates a separate native attempt from the immutable import");runs.put(r.getString("id"),r);}persist();}}
    private static JSONObject copy(JSONObject x){return new JSONObject(x.toString());}
    private Path glbFile(String id) {
        if (!UUID.fromString(id).toString().equals(id)) throw new IllegalArgumentException("Invalid spatial import identity.");
        return file.resolveSibling("glb-sources").resolve(id+".glb");
    }
    private static void validateDefinition(JSONObject scene) {
        var parsed=SpatialScene.parse(scene.getString("sourceText"),scene.getString("id"));
        for (String key:parsed.keySet()) if (!new JSONObject().put("v",parsed.get(key)).similar(new JSONObject().put("v",scene.get(key))))
            throw new IllegalStateException("Spatial source/definition integrity failed: "+key);
    }
    private static JSONObject findSourceBody(JSONObject scene, String sourceId) {
        for (Object raw : scene.getJSONArray("objects")) {
            var body = (JSONObject) raw;
            if (sourceId.equals(body.getString("sourceId"))) return body;
        }
        throw new IllegalStateException("The edited prop is missing.");
    }

    private Path reconstructionSource(String id) {
        if (!UUID.fromString(id).toString().equals(id)) throw new IllegalArgumentException("Invalid reconstruction identity.");
        return file.resolveSibling("reconstruction-sources").resolve(id+".json");
    }

    private void verifyReconstructionSource(String id,String expected) throws Exception {
        Path original=reconstructionSource(id);
        if (!Files.isRegularFile(original,LinkOption.NOFOLLOW_LINKS) || Files.size(original)>SpatialReconstruction.LIMIT
                || !MapDefinition.sha(Files.readAllBytes(original)).equals(expected))
            throw new IllegalStateException("The original silhouette package changed or is unavailable.");
    }

    private void validate(JSONObject scene) {
        validateDefinition(scene);
        if (scene.has("reconstruction")) {
            try {
                var proof=scene.getJSONObject("reconstruction");
                verifyReconstructionSource(scene.getString("id"),proof.getString("sourceSha256"));
                if (!WorldFrames.hash(proof).equals(scene.getString("reconstructionSha256"))
                        || !proof.getString("definitionSha256").equals(MeasurementStore.hash(scene.getString("sourceText"))))
                    throw new IllegalStateException("The reconstruction receipt or definition changed.");
                if ("sprite-library".equals(proof.optString("viewSource")))
                    library.verifyRegisteredViews(proof.getJSONObject("views"));
            } catch (Exception error) {
                throw new IllegalStateException("Cannot recheck the original visual reconstruction.",error);
            }
        }
        if (scene.has("meshEdit")) {
            var proof = scene.getJSONObject("meshEdit");
            var body = findSourceBody(scene, proof.getString("sourceId"));
            if (!proof.getString("definitionSha256").equals(MeasurementStore.hash(scene.getString("sourceText")))
                    || !body.getString("visualMesh").equals(proof.getString("meshSha256"))
                    || !body.getJSONArray("size").similar(proof.getJSONArray("colliderSizeM"))) {
                throw new IllegalStateException("The saved extrusion disagrees with its scene definition.");
            }
            var receipt = WorldFrames.copy(proof);
            receipt.remove("definitionSha256");
            String id = UUID.nameUUIDFromBytes(("mesh-extrusion:" + WorldFrames.hash(receipt))
                    .getBytes(StandardCharsets.UTF_8)).toString();
            if (!id.equals(scene.getString("id"))) throw new IllegalStateException("The saved extrusion receipt changed.");
        }
        if (scene.has("heightStudy")) {
            try {
                var original=heightStudies.boundDefinition(scene.getJSONObject("heightStudy"));
                if (!original.similar(new JSONObject(scene.getString("sourceText"))))
                    throw new IllegalStateException("The measured collider definition changed.");
            } catch (Exception error) {
                throw new IllegalStateException("Cannot recheck the original height study.",error);
            }
        }
        if (scene.has("propCopy")) {
            var receipt=scene.getJSONObject("propCopy");
            var review=receipt.getJSONObject("review");
            var parent=scenes.get(review.getJSONObject("source").getString("sceneId"));
            if (parent==null || !SpatialPropCopy.binding(parent).similar(review.getJSONObject("source")))
                throw new IllegalStateException("The original prop scene changed.");
            if (!WorldFrames.hash(receipt).equals(scene.getString("propCopySha256"))
                    || !WorldFrames.hash(review).equals(receipt.getString("approvalSha256"))
                    || !receipt.getJSONObject("verify").similar(receipt.getJSONObject("replay")))
                throw new IllegalStateException("The reviewed prop copy contact receipt changed.");
            String expected=WorldFrames.hash(SpatialPropCopy.definition(parent,review));
            if (!expected.equals(WorldFrames.hash(new JSONObject(scene.getString("sourceText")))))
                throw new IllegalStateException("The reviewed prop copy geometry changed.");
            // A copy can be used as a source for another copy. Each link must precede it.
            if (!Instant.parse(parent.getString("importedAt")).isBefore(Instant.parse(scene.getString("importedAt"))))
                throw new IllegalStateException("Invalid prop copy ancestry.");
            validate(parent);
        }
        if (scene.has("propPlacement")) {
            var receipt = scene.getJSONObject("propPlacement");
            var review = receipt.getJSONObject("review");
            String id = receipt.getString("reviewId");
            var parent = scenes.get(review.getJSONObject("source").getString("sceneId"));
            if (parent == null || !Instant.parse(parent.getString("importedAt")).isBefore(Instant.parse(scene.getString("importedAt"))))
                throw new IllegalStateException("The placement source scene is unavailable.");
            if (!WorldFrames.hash(receipt).equals(scene.getString("propPlacementSha256"))
                    || !scene.getString("id").equals(SpatialPlacement.sceneId(id))
                    || !receipt.getJSONObject("measured").optBoolean("passed")
                    || !receipt.getJSONObject("measured").similar(receipt.getJSONObject("repeat")))
                throw new IllegalStateException("The placement collision receipt changed.");
            try {
                if (!placements.approved(id) || !placements.readReview(id).similar(review)
                        || !SpatialPlacement.definition(parent,review).similar(new JSONObject(scene.getString("sourceText"))))
                    throw new IllegalStateException("The exact approved scene or review changed.");
            } catch (Exception error) {
                throw new IllegalStateException("Cannot recheck the approved placement.",error);
            }
            validate(parent);
        }
        if (scene.has("glbSha256")) {
            try {
                Path original=glbFile(scene.getString("id"));
                if (!Files.isRegularFile(original,LinkOption.NOFOLLOW_LINKS) || Files.size(original)>SpatialGlb.LIMIT)
                    throw new IllegalStateException("The original GLB source is unavailable.");
                byte[] bytes=Files.readAllBytes(original);
                if (!MapDefinition.sha(bytes).equals(scene.getString("glbSha256"))
                        || !SpatialGlb.importScene(bytes).similar(new JSONObject(scene.getString("sourceText"))))
                    throw new IllegalStateException("The original GLB source changed.");
            } catch (Exception problem) {
                throw new IllegalStateException("Cannot recheck the original GLB source.",problem);
            }
        }
    }
    private synchronized void persist() throws Exception {Files.createDirectories(file.getParent());var temp=file.resolveSibling("state.pending.json");Files.writeString(temp,new JSONObject().put("scenes",new JSONArray(scenes.values())).put("runs",new JSONArray(runs.values())).toString(),StandardCharsets.UTF_8);try{Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}}
    public synchronized JSONObject importScene(String text) throws Exception {
        available();
        if (scenes.size()>=32) throw new IllegalStateException("Spatial import history limit reached (32)");
        return retainScene(SpatialScene.parse(text,UUID.randomUUID().toString()).put("importedAt",Instant.now().toString()));
    }
    private record MeshChange(JSONObject definition, JSONObject evidence, String previewSha256, String encoded) {}

    synchronized JSONObject previewMesh(JSONObject input) {
        SpatialScene.keys(input, "id", "objectId", "triangle", "distanceM", "mode", "index", "edge", "deltaM", "refreshCollider");
        var edit = meshChange(input);
        return new JSONObject().put("previewSha256", edit.previewSha256)
                .put("evidence", edit.evidence).put("visualMesh", edit.encoded);
    }

    synchronized JSONObject saveMesh(JSONObject input) throws Exception {
        SpatialScene.keys(input, "id", "objectId", "triangle", "distanceM", "previewSha256", "mode", "index", "edge", "deltaM", "refreshCollider");
        available();
        var edit = meshChange(input);
        if (!edit.previewSha256.equals(input.getString("previewSha256"))) {
            throw new IllegalArgumentException("Preview the current face or component and its settings before saving.");
        }
        String definition = edit.definition.toString();
        String id = UUID.nameUUIDFromBytes(("mesh-extrusion:" + edit.previewSha256).getBytes(StandardCharsets.UTF_8)).toString();
        if (scenes.containsKey(id)) return read(id);
        if (scenes.size() >= 32) throw new IllegalStateException("Spatial import history limit reached (32)");
        var scene = SpatialScene.parse(definition, id)
                .put("importedAt", Instant.now().toString())
                .put("meshEdit", edit.evidence.put("definitionSha256", MeasurementStore.hash(definition)));
        return retainScene(scene);
    }

    synchronized JSONObject undoMesh(JSONObject input) {
        SpatialScene.keys(input, "id");
        available();
        var edited = read(input.getString("id"));
        var receipt = edited.optJSONObject("meshEdit");
        if (receipt == null) throw new IllegalArgumentException("This scene has no saved mesh edit to undo.");
        var parent = read(receipt.getString("sceneId"));
        var body = findSourceBody(parent, receipt.getString("sourceId"));
        if (!parent.getString("sourceSha256").equals(receipt.getString("sourceSha256"))
                || !body.getString("visualMesh").equals(receipt.getString("sourceMeshSha256"))) {
            throw new IllegalStateException("The previous mesh version changed.");
        }
        return parent;
    }

    private MeshChange meshChange(JSONObject input) {
        var parent = read(input.getString("id"));
        var body = SpatialScene.object(parent, input.getString("objectId"));
        if (!body.has("visualMesh")) throw new IllegalArgumentException("Select a triangle prop first.");
        var source = SpatialGlb.asset(parent, body);
        String mode = input.optString("mode", "extrude");
        boolean refresh = mode.equals("fit") || input.has("refreshCollider") && input.getBoolean("refreshCollider");
        var result = switch (mode) {
            case "extrude" -> SpatialFaceExtrusion.apply(source,
                    SpatialMeshComponents.integer(input, "triangle", source.indices().length / 3 - 1),
                    SpatialScene.number(input, "distanceM", .001, 1));
            case "fit" -> new SpatialFaceExtrusion.Result(source, new JSONObject().put("mode", "fit")
                    .put("sourceMeshSha256", source.key()).put("triangles", source.indices().length / 3));
            default -> SpatialMeshComponents.move(source, input);
        };
        var mesh = result.mesh();
        var proof = result.evidence().put("sceneId", parent.getString("id"))
                .put("sourceSha256", parent.getString("sourceSha256")).put("objectId", body.getString("id"))
                .put("sourceId", body.getString("sourceId")).put("colliderSizeM", body.getJSONArray("size"));
        var definition = new JSONObject(parent.getString("sourceText"));
        var assets = definition.getJSONObject("visualMeshes");
        String previous = body.getString("visualMesh");
        if (refresh) {
            var fit = SpatialMeshComponents.fit(mesh);
            var shift = ObserverGeometry.rotate(fit.center(), body.getJSONArray("rotation"), false);
            var target = findSourceBody(definition, body.getString("sourceId"));
            moveCentre(target, "position", shift);
            target.put("size", new JSONArray(fit.size()));
            var timeline = definition.optJSONObject("translationAnimation");
            var keys = timeline == null ? null : timeline.getJSONObject("tracks").optJSONArray(body.getString("sourceId"));
            if (keys != null) for (Object raw : keys) moveCentre((JSONObject) raw, "positionM", shift);
            proof.put("colliderSizeM", target.getJSONArray("size")).put("colliderCentreShiftM", new JSONArray(shift));
            mesh = fit.mesh();
        }
        String updated = mesh.key();
        proof.put("meshSha256", updated);
        String previewSha256 = WorldFrames.hash(proof);
        boolean stillUsed = false;
        for (Object raw : definition.getJSONArray("objects")) {
            var object = (JSONObject) raw;
            if (object.getString("sourceId").equals(body.getString("sourceId"))) object.put("visualMesh", updated);
            else if (previous.equals(object.optString("visualMesh"))) stillUsed = true;
        }
        if (!stillUsed) assets.remove(previous);
        String encoded = mesh.encoded();
        assets.put(updated, encoded);
        String title = definition.getString("title");
        String suffix = mode.equals("extrude") ? " · extruded" : mode.equals("fit") ? " · collider fit" : " · mesh edit";
        definition.put("title", title.substring(0, Math.min(120-suffix.length(), title.length())) + suffix);
        var provenance = definition.getJSONObject("source");
        String note = (mode.equals("extrude") ? "Planar extrusion" : "Mesh "+mode)+" from scene SHA-256 " + parent.getString("sourceSha256") + ". " + provenance.getString("note");
        provenance.put("note", note.substring(0, Math.min(1000, note.length())));
        if (refresh) SpatialScene.parse(definition.toString(), parent.getString("id"));
        return new MeshChange(definition, proof, previewSha256, result.mesh().encoded());
    }

    private static void moveCentre(JSONObject object, String key, double[] shift) {
        object.put(key, new JSONArray(ObserverGeometry.add(ObserverGeometry.v(object.getJSONArray(key)), shift)));
    }

    synchronized JSONObject previewAnimation(JSONObject input) {
        SpatialScene.keys(input,"id","sourceId","keys");
        var scene=read(input.getString("id"));
        var animation=input.has("keys") ? TranslationTimeline.edit(scene,input.getString("sourceId"),input.getJSONArray("keys"))
                : scene.getJSONObject("translationAnimation");
        return TranslationTimeline.sample(animation);
    }

    synchronized JSONObject saveAnimation(JSONObject input) throws Exception {
        SpatialScene.keys(input,"id","sourceId","keys");
        var parent=read(input.getString("id"));
        var animation=TranslationTimeline.edit(parent,input.getString("sourceId"),input.getJSONArray("keys"));
        var definition=new JSONObject(parent.getString("sourceText")).put("translationAnimation",animation);
        var source=definition.getJSONObject("source");
        String note="Translation timeline from scene SHA-256 "+parent.getString("sourceSha256")+". "+source.getString("note");
        source.put("note",note.substring(0,Math.min(1000,note.length())));
        String title=definition.getString("title");
        if (!parent.has("translationAnimation")) definition.put("title",title.substring(0,Math.min(105,title.length()))+" · animation");
        return importScene(definition.toString());
    }

    public synchronized JSONObject importGlb(byte[] bytes) throws Exception {
        available();
        if (scenes.size()>=32) throw new IllegalStateException("Spatial import history limit reached (32)");
        String sha=MapDefinition.sha(bytes);
        String id=UUID.randomUUID().toString();
        var scene=SpatialScene.parse(SpatialGlb.importScene(bytes).toString(),id)
                .put("importedAt",Instant.now().toString()).put("glbSha256",sha);
        Path source=glbFile(id);
        Files.createDirectories(source.getParent());
        Files.write(source,bytes,StandardOpenOption.CREATE_NEW);
        try { return retainScene(scene); }
        catch (Exception error) {
            try { Files.deleteIfExists(source); }
            catch (Exception cleanup) { error.addSuppressed(cleanup); }
            throw error;
        }
    }
    synchronized JSONObject reviewCarry(JSONObject input) throws Exception {
        available();
        return carries.review(read(input.getString("sceneId")),input);
    }

    synchronized JSONObject readCarry(JSONObject input) throws Exception {
        SpatialScene.keys(input,"sceneId","id");
        return carries.read(read(input.getString("sceneId")),input.getString("id"));
    }

    synchronized JSONObject latestCarry() throws Exception {
        var reference=carries.latestReference();
        return reference.isEmpty()?reference:readCarry(reference);
    }

    synchronized JSONObject approveCarry(JSONObject input) throws Exception {
        available();
        return carries.approve(read(input.getString("sceneId")),input);
    }

    synchronized JSONObject reviewProp(JSONObject args) {
        available();
        var review=SpatialPropCopy.review(read(args.getString("sceneId")),args);
        return new JSONObject().put("review",review).put("approvalSha256",WorldFrames.hash(review));
    }
    synchronized JSONObject copyProp(JSONObject input) throws Exception {
        SpatialScene.keys(input,"args","approvalSha256");
        var args=input.getJSONObject("args");
        var parent=read(args.getString("sceneId"));
        var review=SpatialPropCopy.review(parent,args);
        String digest=WorldFrames.hash(review);
        if (!digest.equals(input.getString("approvalSha256")))
            throw new IllegalStateException("Approve the exact selected prop, source and transform.");
        String id=UUID.nameUUIDFromBytes(("spatial-prop-copy-v1:"+digest).getBytes(StandardCharsets.UTF_8)).toString();
        if (scenes.containsKey(id)) return read(id);
        available();
        if (scenes.size()>=32) throw new IllegalStateException("Spatial import history limit reached (32)");
        var scene=SpatialScene.parse(SpatialPropCopy.definition(parent,review).toString(),id);
        var checked=SpatialPropCopy.verifyNative(scene,review,nativeDir);
        var replay=SpatialPropCopy.verifyNative(scene,review,nativeDir);
        if (!checked.similar(replay)) throw new IllegalStateException("The independent native collision replay differs.");
        var receipt=new JSONObject().put("review",review).put("approvalSha256",digest).put("verify",checked).put("replay",replay);
        return retainScene(scene.put("importedAt",Instant.now().toString()).put("propCopy",receipt)
                .put("propCopySha256",WorldFrames.hash(receipt)));
    }

    synchronized JSONObject openPlacementExchange(JSONObject input) throws Exception {
        return exchange.open(input);
    }

    synchronized JSONObject latestPlacementExchange(JSONObject input) throws Exception {
        return exchange.latest(input);
    }

    synchronized JSONObject replayPlacementExchange(JSONObject input) throws Exception {
        available();
        return exchange.replay(input);
    }

    synchronized JSONObject reviewPlacement(JSONObject input) throws Exception {
        available();
        var source = read(input.getString("sceneId"));
        var review = SpatialPlacement.plan(source,input);
        return placementStatus(placements.saveReview(review));
    }

    synchronized JSONObject latestPlacement(JSONObject input) throws Exception {
        SpatialScene.keys(input);
        String id = placements.latestId();
        return id.isEmpty() ? new JSONObject() : placementStatus(id);
    }

    synchronized JSONObject approvePlacement(JSONObject input) throws Exception {
        available();
        SpatialScene.keys(input, "reviewId", "approvalSha256");
        String id = input.getString("reviewId");
        if (!id.equals(input.getString("approvalSha256")))
            throw new IllegalArgumentException("Approve the exact saved placement review.");
        var review = placements.readReview(id);
        var source = read(review.getJSONObject("source").getString("sceneId"));
        SpatialPlacement.definition(source,review);
        placements.approve(id);
        return placementStatus(id);
    }

    synchronized JSONObject runPlacement(JSONObject input) throws Exception {
        available();
        SpatialScene.keys(input, "reviewId");
        String id = input.getString("reviewId");
        var review = placements.readReview(id);
        if (!placements.approved(id)) throw new IllegalStateException("Approve this placement before running contact trials.");
        String sceneId = SpatialPlacement.sceneId(id);
        if (scenes.containsKey(sceneId)) return placementStatus(id);
        if (scenes.size()>=32) throw new IllegalStateException("Spatial import history limit reached (32)");
        var parent = read(review.getJSONObject("source").getString("sceneId"));
        var scene = SpatialScene.parse(SpatialPlacement.definition(parent,review).toString(),sceneId);
        var measured = SpatialPlacement.verifyNative(scene,review,nativeDir);
        var repeat = SpatialPlacement.verifyNative(scene,review,nativeDir);
        if (!measured.similar(repeat)) throw new IllegalStateException("The independent native contact repeat differs.");
        var receipt = new JSONObject().put("reviewId",id).put("review",review).put("measured",measured).put("repeat",repeat);
        retainScene(scene.put("importedAt",Instant.now().toString()).put("propPlacement",receipt)
                .put("propPlacementSha256",WorldFrames.hash(receipt)));
        return placementStatus(id);
    }

    synchronized JSONObject replayPlacement(JSONObject input) throws Exception {
        available();
        SpatialScene.keys(input, "reviewId");
        String id = input.getString("reviewId");
        var scene = read(SpatialPlacement.sceneId(id));
        var original = scene.getJSONObject("propPlacement");
        var fresh = SpatialPlacement.verifyNative(scene,original.getJSONObject("review"),nativeDir);
        if (!fresh.similar(original.getJSONObject("measured"))) throw new IllegalStateException("Native placement replay differs.");
        return placementStatus(id).put("replayed",true);
    }

    synchronized JSONObject exportPlacement(JSONObject input) throws Exception {
        SpatialScene.keys(input, "reviewId");
        String id = input.getString("reviewId");
        var status = placementStatus(id);
        if (!"verified".equals(status.getString("status"))) throw new IllegalStateException("Verify the collision before exporting.");
        var scene = read(status.getString("sceneId"));
        var source = read(status.getJSONObject("review").getJSONObject("source").getString("sceneId"));
        return SpatialPlacement.bundle(scene, source);
    }

    private JSONObject placementStatus(String id) throws Exception {
        var review = placements.readReview(id);
        var result = new JSONObject().put("reviewId",id).put("review",review)
                .put("status",placements.approved(id) ? "approved" : "review");
        String key = SpatialPlacement.sceneId(id);
        if (scenes.containsKey(key)) {
            var scene = read(key);
            result.put("status","verified").put("sceneId",key)
                    .put("evidence",scene.getJSONObject("propPlacement"))
                    .put("evidenceSha256",scene.getString("propPlacementSha256"));
        }
        return result;
    }

    synchronized JSONObject reconstruct(byte[] source) throws Exception {
        return retainReconstruction(source,SpatialReconstruction.POLICY);
    }

    synchronized JSONObject traceBrowser(byte[] source) throws Exception {
        return retainReconstruction(source,BrowserShapeTrace.FORMAT);
    }

    synchronized JSONObject traceLibrary(JSONObject input) throws Exception {
        var original = library.registeredViews(input);
        return retainReconstruction(original.toString().getBytes(StandardCharsets.UTF_8), BrowserViewTrace.FORMAT_LIBRARY);
    }

    private JSONObject retainReconstruction(byte[] source, String format) throws Exception {
        if (source.length==0 || source.length>SpatialReconstruction.LIMIT)
            throw new IllegalArgumentException("Choose a silhouette set up to 1.2 MB.");
        String sha=MapDefinition.sha(source);
        String id=UUID.nameUUIDFromBytes((format+":"+sha).getBytes(StandardCharsets.UTF_8)).toString();
        if (scenes.containsKey(id)) return read(id);
        available();
        if (scenes.size()>=32) throw new IllegalStateException("Spatial import history limit reached (32)");
        var result = switch (format) {
            case BrowserViewTrace.FORMAT_LIBRARY -> BrowserViewTrace.build(source,
                    new JSONObject(new String(source, StandardCharsets.UTF_8)), true);
            case BrowserShapeTrace.FORMAT -> BrowserShapeTrace.build(source);
            default -> SpatialReconstruction.build(source);
        };
        String definition=result.definition().toString();
        var proof=result.evidence().put("definitionSha256",MeasurementStore.hash(definition));
        var scene=SpatialScene.parse(definition,id).put("importedAt",Instant.now().toString())
                .put("reconstruction",proof).put("reconstructionSha256",WorldFrames.hash(proof));
        Path original=reconstructionSource(id);
        Files.createDirectories(original.getParent());
        if (Files.exists(original)) verifyReconstructionSource(id,sha);
        else {
            Path pending=Files.createTempFile(original.getParent(),"reconstruction-",".pending");
            try {
                Files.write(pending,source);
                Files.move(pending,original,StandardCopyOption.ATOMIC_MOVE);
            } finally { Files.deleteIfExists(pending); }
        }
        return retainScene(scene);
    }

    synchronized JSONObject importHeightCollider(JSONObject approved) throws Exception {
        var definition=approved.getJSONObject("collider");
        String key=approved.getString("colliderApprovalSha256");
        String id=UUID.nameUUIDFromBytes(("height-collider:"+key).getBytes(StandardCharsets.UTF_8)).toString();
        if (scenes.containsKey(id)) {
            var existing=read(id);
            if (!new JSONObject(existing.getString("sourceText")).similar(definition))
                throw new IllegalStateException("Previously imported collider definition changed.");
            return existing;
        }
        available();
        if (scenes.size()>=32) throw new IllegalStateException("Spatial import history limit reached (32)");
        var source=new JSONObject().put("id",approved.getString("id")).put("colliderApprovalSha256",key)
                .put("evidenceSha256",approved.getString("evidenceSha256"));
        return retainScene(SpatialScene.parse(definition.toString(),id).put("importedAt",Instant.now().toString())
                .put("heightStudy",source));
    }

    private JSONObject retainScene(JSONObject scene) throws Exception {
        String id=scene.getString("id");
        scenes.put(id,scene);
        try { persist(); }
        catch (Exception error) { scenes.remove(id);throw error; }
        current=new JSONObject().put("sceneId",id).put("status","authored")
                .put("current",new JSONObject().put("tick",0).put("objects",scene.getJSONArray("objects")).put("gripping",false));
        return copy(scene);
    }
    public synchronized JSONArray list(){var out=new JSONArray();for(var s:scenes.values())out.put(new JSONObject().put("id",s.getString("id")).put("version",1).put("title",s.getString("title")).put("source",s.getJSONObject("source")).put("sourceSha256",s.getString("sourceSha256")));return out;}
    public synchronized JSONObject read(String id){UUID.fromString(id);var s=scenes.get(id);if(s==null)throw new IllegalArgumentException("Unknown spatial scene");validate(s);return copy(s);}
    public synchronized JSONObject snapshot(){return current==null?new JSONObject().put("status","idle"):copy(current);}
    public synchronized JSONObject stateForScene(String id){var s=read(id);if(current!=null&&current.optString("sceneId").equals(id))return copy(current);JSONObject last=null;for(var r:runs.values())if(r.getString("sceneId").equals(id)&&r.getString("status").equals("completed"))last=r;if(last!=null)return new JSONObject().put("id",last.getString("id")).put("sceneId",id).put("status","completed").put("current",last.getJSONObject("actual").getJSONObject("final")).put("verification",last.getJSONObject("verification"));return new JSONObject().put("sceneId",id).put("status","authored").put("current",new JSONObject().put("tick",0).put("objects",s.getJSONArray("objects")).put("gripping",false));}
    public synchronized void available(){if(closed||!active.isEmpty())throw new IllegalStateException("Wait for or cancel the owned spatial manipulation");}
    public synchronized JSONObject run(String id){UUID.fromString(id);var r=runs.get(id);if(r==null)throw new IllegalArgumentException("Unknown spatial run");return copy(r);}
    public synchronized boolean hasRun(String id){return runs.containsKey(id);}
    public synchronized JSONArray runs(){var out=new JSONArray();for(var r:runs.values())out.put(new JSONObject().put("id",r.getString("id")).put("sceneId",r.getString("sceneId")).put("status",r.getString("status")).put("createdAt",r.getString("createdAt")));return out;}
    public static JSONObject binding(JSONObject scene){return new JSONObject().put("sceneId",scene.getString("id")).put("version",1).put("sourceSha256",scene.getString("sourceSha256")).put("definitionSha256",MeasurementStore.hash(scene.toString()));}
    static JSONObject intent(JSONObject scene,String objectId,JSONArray displacement){var object=SpatialScene.object(scene,objectId);if(object.getDouble("massKg")<=0)throw new IllegalArgumentException("Select a dynamic object");var delta=SpatialScene.vector(displacement,6);if(ObserverGeometry.norm(delta)<.1||ObserverGeometry.norm(delta)>6||Math.abs(delta[1])>1e-8)throw new IllegalArgumentException("This procedure supports a 0.1..6 m horizontal displacement, ending at the source height");var target=ObserverGeometry.add(SpatialScene.vector(object.getJSONArray("position"),20),delta);for(double v:target)RampDefinition.range(v,-12,12,"target");return new JSONObject().put("binding",binding(scene)).put("objectId",objectId).put("sourceId",object.getString("sourceId")).put("displacementM",displacement).put("targetM",SpatialScene.array(target));}
    /** Four finite routes evaluated by native physics. No displayed world exists in these forks. */
    public JSONObject preview(JSONObject scene,String objectId,JSONArray displacement) throws Exception {
        var intent=intent(scene,objectId,displacement);var object=SpatialScene.object(scene,objectId);double[] start=SpatialScene.vector(object.getJSONArray("position"),20),target=SpatialScene.vector(intent.getJSONArray("targetM"),15),size=SpatialScene.vector(object.getJSONArray("size"),40);double radius=ObserverGeometry.norm(size)/2,top=Math.max(start[1],target[1]),left=Math.min(start[2],target[2]),right=Math.max(start[2],target[2]);
        for(Object raw:scene.getJSONArray("objects")){var o=(JSONObject)raw;if(o.getDouble("massKg")>0)continue;double[] p=SpatialScene.vector(o.getJSONArray("position"),20),s=SpatialScene.vector(o.getJSONArray("size"),40);double r=ObserverGeometry.norm(s)/2;double upper=p[1]+s[1]/2;if(upper<=Math.min(start[1],target[1])-.05)continue;top=Math.max(top,p[1]+r);left=Math.min(left,p[2]-r);right=Math.max(right,p[2]+r);}
        top+=radius+.25;left-=radius+.3;right+=radius+.3;
        var candidates=new LinkedHashMap<String,JSONArray>();candidates.put("direct",new JSONArray().put(SpatialScene.array(target)));
        if(top<10)candidates.put("over",new JSONArray().put(new JSONArray(new double[]{start[0],top,start[2]})).put(new JSONArray(new double[]{target[0],top,target[2]})).put(SpatialScene.array(target)));
        for(var side:List.of(Map.entry("left",left),Map.entry("right",right)))if(Math.abs(side.getValue())<10)candidates.put(side.getKey(),new JSONArray().put(new JSONArray(new double[]{start[0],start[1]+.15,side.getValue()})).put(new JSONArray(new double[]{target[0],target[1]+.15,side.getValue()})).put(SpatialScene.array(target)));
        var trials=new JSONArray();JSONObject best=null;double bestCost=Double.POSITIVE_INFINITY;
        for(var candidate:candidates.entrySet()){double cost=0;double[] prior=start;for(Object raw:candidate.getValue()){var p=SpatialScene.vector((JSONArray)raw,15);cost+=ObserverGeometry.norm(ObserverGeometry.sub(p,prior));prior=p;}if(cost>16){trials.put(new JSONObject().put("route",candidate.getKey()).put("passed",false).put("reason","Path exceeds 16 m budget"));continue;}
            try(var world=new SpatialPhysics(scene,nativeDir)){var result=world.execute(objectId,candidate.getValue(),()->closed||Thread.currentThread().isInterrupted(),s->{},false);trials.put(new JSONObject().put("route",candidate.getKey()).put("pathM",cost).put("passed",result.getJSONObject("metrics").getBoolean("passed")).put("metrics",result.getJSONObject("metrics")));if(result.getJSONObject("metrics").getBoolean("passed")&&cost<bestCost){bestCost=cost;best=new JSONObject().put("route",candidate.getKey()).put("waypoints",candidate.getValue()).put("forecast",result);}}
        }
        if(best==null)throw new IllegalStateException("No bounded native forecast reaches and releases at the target; no actual actuation occurred. Trials: "+trials);
        return best.put("intent",intent).put("trials",trials).put("modelKind","native rigid-body forward simulation; declared parameters; not a learned universal world model").put("previewOnly",true);
    }
    public synchronized JSONObject start(String runId,String goalId,int attempt,JSONObject scene,JSONObject plan) throws Exception {
        UUID.fromString(runId);var prior=runs.get(runId);if(prior!=null){if(!prior.optString("goalId").equals(goalId)||prior.optInt("attempt")!=attempt||!prior.getJSONObject("plan").similar(plan))throw new IllegalStateException("Foreign spatial effect identity");return copy(prior);}available();if(runs.size()>=32)throw new IllegalStateException("Spatial run history limit reached (32)");validate(scene);if(!binding(scene).similar(plan.getJSONObject("intent").getJSONObject("binding")))throw new IllegalStateException("Forecast source changed");
        var r=new JSONObject().put("id",runId).put("goalId",goalId).put("attempt",attempt).put("sceneId",scene.getString("id")).put("definition",scene).put("plan",plan).put("createdAt",Instant.now().toString()).put("status","prepared");runs.put(runId,r);persist();active=runId;cancelled.set(false);current=new JSONObject().put("id",runId).put("sceneId",r.getString("sceneId")).put("status","prepared").put("current",new JSONObject().put("tick",0).put("objects",scene.getJSONArray("objects")).put("gripping",false));
        worker.submit(()->execute(runId,scene,plan));return copy(r);
    }
    private void execute(String id,JSONObject scene,JSONObject plan){try{
        synchronized(this){runs.get(id).put("status","executing");persist();}
        JSONObject actual;try(var world=new SpatialPhysics(scene,nativeDir)){actual=world.execute(plan.getJSONObject("intent").getString("objectId"),plan.getJSONArray("waypoints"),cancelled::get,s->{synchronized(this){current=new JSONObject().put("id",id).put("sceneId",scene.getString("id")).put("status","executing").put("current",s);}},true);}
        synchronized(this){if(cancelled.get())throw new InterruptedException("Cancelled before native completion commit");var r=runs.get(id);r.put("actual",actual).put("status","completed");r.put("verification",verify(r));current.put("status","completed").put("current",actual.getJSONObject("final")).put("verification",r.getJSONObject("verification"));persist();}
    }catch(Exception e){synchronized(this){runs.get(id).put("status",cancelled.get()?"cancelled":"interrupted").put("reason",String.valueOf(e.getMessage()));if(current!=null)current.put("status",runs.get(id).getString("status"));try{persist();}catch(Exception failure){System.err.println(failure);}}}finally{synchronized(this){active="";}}}
    static JSONObject verify(JSONObject run){
        if(!run.getString("status").equals("completed"))throw new IllegalStateException("Spatial run did not complete");var plan=run.getJSONObject("plan");var actual=run.getJSONObject("actual");var expected=plan.getJSONObject("forecast");var intent=plan.getJSONObject("intent");if(!binding(run.getJSONObject("definition")).similar(intent.getJSONObject("binding")))throw new IllegalStateException("Spatial binding differs");
        var a=actual.getJSONArray("samples");var p=expected.getJSONArray("samples");if(a.length()!=p.length()||a.length()<10)throw new IllegalStateException("Forecast/sample alignment differs");double sum=0,max=0;for(int i=0;i<a.length();i++){var x=a.getJSONObject(i);var y=p.getJSONObject(i);if(x.getInt("tick")!=y.getInt("tick"))throw new IllegalStateException("Forecast tick differs");double e=ObserverGeometry.norm(ObserverGeometry.sub(SpatialScene.vector(x.getJSONArray("position"),30),SpatialScene.vector(y.getJSONArray("position"),30)));sum+=e*e;max=Math.max(max,e);}
        var last=a.getJSONObject(a.length()-1);var target=SpatialScene.vector(intent.getJSONArray("targetM"),15);var position=SpatialScene.vector(last.getJSONArray("position"),30);double horizontal=Math.hypot(position[0]-target[0],position[2]-target[2]),vertical=Math.abs(position[1]-target[1]),speed=ObserverGeometry.norm(SpatialScene.vector(last.getJSONArray("velocity"),100));var events=actual.getJSONArray("events");int grabs=0,releases=0,moves=0;double endpoint=0;
        for(Object raw:events){var e=(JSONObject)raw;switch(e.getString("kind")){case "grab"->grabs++;case "move"->{if(moves>=plan.getJSONArray("waypoints").length()||!e.getJSONArray("targetM").similar(plan.getJSONArray("waypoints").getJSONArray(moves)))throw new IllegalStateException("Move intent differs from forecast");moves++;double error=ObserverGeometry.norm(ObserverGeometry.sub(SpatialScene.vector(e.getJSONArray("position"),30),SpatialScene.vector(e.getJSONArray("targetM"),15)));if(Math.abs(error-e.getDouble("errorM"))>2e-6)throw new IllegalStateException("Move endpoint metric differs from native state");endpoint=Math.max(endpoint,error);}case "release"->{releases++;if(!e.getJSONArray("velocityBefore").similar(e.getJSONArray("velocityAfter")))throw new IllegalStateException("Release changed velocity");}default->throw new IllegalStateException("Unknown actual event");}}
        var finalObject=SpatialScene.object(new JSONObject().put("objects",actual.getJSONObject("final").getJSONArray("objects")),intent.getString("objectId"));if(!finalObject.getJSONArray("position").similar(last.getJSONArray("position")))throw new IllegalStateException("Final body differs from sample");
        boolean passed=horizontal<=.1&&vertical<=.15&&speed<=.25&&endpoint<=.08&&max<=.02&&grabs==1&&releases==1&&moves==plan.getJSONArray("waypoints").length()&&!actual.getJSONObject("final").getBoolean("gripping");
        return new JSONObject().put("passed",passed).put("horizontalErrorM",horizontal).put("verticalErrorM",vertical).put("terminalSpeedMps",speed).put("maxGripEndpointErrorM",endpoint).put("forecastRmseM",Math.sqrt(sum/a.length())).put("forecastMaxErrorM",max).put("samples",a.length()).put("grabs",grabs).put("releases",releases).put("route",plan.getString("route")).put("sourceKind",run.getJSONObject("definition").getJSONObject("source").getString("kind")).put("scaleIndependentlyCalibrated",false);
    }
    public synchronized JSONObject cancel(String id) throws Exception {var r=runs.get(id);if(r==null)return new JSONObject();if(id.equals(active)){cancelled.set(true);r.put("status","cancelled").put("reason","Explicitly cancelled; no successful procedure promoted");persist();}return copy(r);}
    @Override public void close(){synchronized(this){closed=true;cancelled.set(true);}worker.shutdownNow();try{worker.awaitTermination(3,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
}
