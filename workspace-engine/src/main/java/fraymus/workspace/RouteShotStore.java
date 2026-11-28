package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static fraymus.workspace.MapDefinition.sha;

/** Persists editorial shots and binds both video takes to one approved route and camera track. */
final class RouteShotStore {
    private static final int MAX_VIDEO = 16_000_000;
    private static final int MAX_BUNDLE = 10_000_000;
    private static final String BUNDLE_FORMAT = "fraynix.route-shot-bundle-v1";
    private static final Set<String> TAKES = Set.of("clean","annotated");
    private final Path root;
    private final SpatialStore scenes;
    private final LoadingAisleMission routes;

    RouteShotStore(Path project, SpatialStore scenes, LoadingAisleMission routes) throws IOException {
        root = project.resolve("route-shots");
        Files.createDirectories(root);
        this.scenes = scenes;
        this.routes = routes;
    }

    private Path file(String id,String suffix) {
        if (!id.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Invalid route-shot identity.");
        return root.resolve(id+"."+suffix);
    }

    synchronized JSONObject build(JSONObject input) throws Exception {
        SpatialScene.keys(input,"routeId","cameraKeys","overlays");
        String routeId = input.getString("routeId");
        var bound = source(routeId,false);
        JSONObject review = bound.mission().getJSONObject("review");
        JSONArray keys = input.has("cameraKeys")?input.getJSONArray("cameraKeys"):RouteShot.defaultKeys(review);
        var definition = definition(bound.mission(),keys,readOverlays(input));
        String id = WorldFrames.hash(definition);
        JSONObject record = Files.exists(file(id,"shot"))?open(id):create(id,definition);
        remember(id);
        return record;
    }

    private JSONObject create(String id,JSONObject definition) throws Exception {
        var bound = source(definition.getString("routeId"),true);
        var output = render(renderer(definition,bound));
        retain(record(id,definition,output),output.clean,output.annotated);
        return open(id);
    }

    private void remember(String id) throws IOException {
        AgentFiles042.saveChecked(root.resolve("latest.json"),new JSONObject().put("id",id));
    }

    private static JSONObject record(String id,JSONObject definition,ShotOutput output) {
        return new JSONObject().put("id",id).put("definition",definition).put("audit",output.audit)
                .put("takes",new JSONObject().put("clean",receipt(output.clean)).put("annotated",receipt(output.annotated)));
    }

    private void retain(JSONObject record,byte[] clean,byte[] annotated) throws Exception {
        String id = record.getString("id");

        if (clean.length>MAX_VIDEO || annotated.length>MAX_VIDEO)
            throw new IOException("Route-shot video exceeds its bounded output size.");
        if (Files.exists(file(id,"shot"))) {
            if (!open(id).similar(record)) throw new IllegalStateException("The existing shot differs from this package.");
            return;
        }
        SpriteWorkshop.replace(file(id,"clean"),clean);
        SpriteWorkshop.replace(file(id,"annotated"),annotated);
        AgentFiles042.saveChecked(file(id,"shot"),record);
    }

    private record ShotOutput(byte[] clean,byte[] annotated,JSONObject audit) {}

    private static ShotOutput render(RouteShot shot) throws IOException {
        var clean = new ArrayList<byte[]>(RouteShot.FRAMES);
        var annotated = new ArrayList<byte[]>(RouteShot.FRAMES);
        var rows = new JSONArray();
        int first = -1;
        int covered = -1;
        int returned = -1;
        int hidden = 0;
        for (int frame=0;frame<RouteShot.FRAMES;frame++) {
            var take = shot.frame(frame);
            var row = take.proof();
            byte[] cleanFrame = MotionTimeline.jpeg(take.clean(),.83f);
            byte[] annotatedFrame = MotionTimeline.jpeg(take.annotated(),.83f);
            clean.add(cleanFrame);
            annotated.add(annotatedFrame);
            rows.put(row.put("cleanJpegSha256",sha(cleanFrame)).put("annotatedJpegSha256",sha(annotatedFrame)));
            if (row.getBoolean("centerVisible")) {
                if (first<0) first = frame;
                if (covered>=0 && returned<0) returned = frame;
            }
            if (row.getBoolean("fullyHiddenByShelf")) {
                hidden++;
                if (first>=0 && covered<0) covered = frame;
            }
        }
        byte[] pcm = new byte[RouteShot.FRAMES*MotionMediaContainers.PCM_PER_FRAME];
        var audit = new JSONObject().put("frameCount",RouteShot.FRAMES).put("decodedFramesPerTake",RouteShot.FRAMES)
                .put("shelfHiddenFrames",hidden).put("occlusionDemonstrated",returned>=0)
                .put("firstVisibleFrame",first).put("shelfHiddenFrame",covered).put("returnedFrame",returned)
                .put("frames",rows);
        return new ShotOutput(verifiedAvi(clean,pcm),verifiedAvi(annotated,pcm),audit);
    }

    private static byte[] verifiedAvi(List<byte[]> frames,byte[] pcm) throws IOException {
        byte[] movie = MotionMediaContainers.avi(frames,pcm);
        MotionMediaContainers.checkAvi(movie,frames,pcm);
        for (byte[] jpeg : frames) {
            var image = ImageIO.read(new ByteArrayInputStream(jpeg));
            if (image==null || image.getWidth()!=RouteShot.WIDTH || image.getHeight()!=RouteShot.HEIGHT)
                throw new IOException("An exported frame could not be independently decoded.");
            image.flush();
        }
        return movie;
    }

    private LoadingAisleMission.ShotSource source(String id,boolean replay) throws Exception {
        if (!routes.hasReview(id) && Files.exists(file(id,"portable"))) {
            var retained = routes.checkShotSource(AgentFiles042.loadChecked(file(id,"portable")),replay);
            checkRoute(retained.mission());
            return retained;
        }
        var record = replay?routes.replay(scenes,id):routes.read(scenes,id);
        record.remove("replayed");
        checkRoute(record);
        return new LoadingAisleMission.ShotSource(scenes.read(record.getJSONObject("review").getString("parentId")),record);
    }

    private static void checkRoute(JSONObject record) {
        if (!"verified".equals(record.optString("status")) || !record.getJSONObject("review").has("route"))
            throw new IllegalArgumentException("Approve and verify a multi-segment loading-bay route first.");
        var review = record.getJSONObject("review");
        var points = review.getJSONObject("route").getJSONArray("pointsM");
        var legs = record.getJSONObject("proof").getJSONObject("measurements").getJSONObject("route").getJSONArray("segments");
        if (points.length()!=legs.length()+1) throw new IllegalStateException("Route legs and measurements disagree.");
        for (int i=0;i<legs.length();i++) {
            var leg = legs.getJSONObject(i);
            if (!points.getJSONArray(i).similar(leg.getJSONArray("fromM"))
                    || !points.getJSONArray(i+1).similar(leg.getJSONArray("toM"))
                    || !leg.getBoolean("sweptPathClear") || !leg.getBoolean("endpointsClear") || !leg.getBoolean("aisleClear"))
                throw new IllegalStateException("An approved route leg lacks its verified clearance.");
        }
    }

    private static RouteShotOverlays readOverlays(JSONObject value) {
        return value.has("overlays")?RouteShotOverlays.parse(value.getJSONObject("overlays")):null;
    }

    private static JSONObject definition(JSONObject bound,JSONArray keys,RouteShotOverlays overlays) {
        RouteShot.readKeys(keys);
        var review = bound.getJSONObject("review");
        var definition = new JSONObject().put("format",overlays==null?RouteShot.FORMAT:RouteShot.EDITABLE_FORMAT).put("motion","planned-route")
                .put("routeId",bound.getString("id")).put("routeEvidenceSha256",WorldFrames.hash(bound))
                .put("sceneId",review.getString("parentId")).put("sceneSha256",review.getString("parentSha256"))
                .put("crate",review.getString("crate")).put("pathM",review.getJSONObject("route").getJSONArray("pointsM"))
                .put("cameraKeys",keys).put("fps",RouteShot.FPS).put("frameCount",RouteShot.FRAMES)
                .put("width",RouteShot.WIDTH).put("height",RouteShot.HEIGHT);
        if (overlays!=null) definition.put("overlays",overlays.json());
        return definition;
    }

    private static RouteShot renderer(JSONObject definition,LoadingAisleMission.ShotSource bound) {
        return new RouteShot(bound.parent(),bound.mission().getJSONObject("review"),
                definition.getJSONArray("cameraKeys"),readOverlays(definition));
    }

    synchronized JSONObject overlayDefaults(String id) throws Exception {
        var record = read(id);
        var bound = source(record.getJSONObject("definition").getString("routeId"),false);
        return RouteShotOverlays.defaults(bound.parent(),bound.mission().getJSONObject("review"));
    }

    private static void checkIdentity(String id,JSONObject record,LoadingAisleMission.ShotSource bound) {
        var definition = record.getJSONObject("definition");
        if (!id.equals(record.getString("id")) || !id.equals(WorldFrames.hash(definition)) ||
                !definition.similar(definition(bound.mission(),definition.getJSONArray("cameraKeys"),readOverlays(definition))))
            throw new IllegalStateException("The source scene, route or shot definition changed.");
    }

    private JSONObject read(String id) throws Exception {
        JSONObject record = AgentFiles042.loadChecked(file(id,"shot"));
        checkIdentity(id,record,source(record.getJSONObject("definition").getString("routeId"),false));
        return record;
    }

    synchronized JSONObject open(String id) throws Exception {
        JSONObject record = read(id);
        for (String take : TAKES) bytes(record,take);
        return record;
    }

    synchronized JSONObject latest() throws Exception {
        var file = root.resolve("latest.json");
        return Files.exists(file)?open(AgentFiles042.loadChecked(file).getString("id")):new JSONObject();
    }

    synchronized JSONObject frame(JSONObject input) throws Exception {
        SpatialScene.keys(input,"id","take","frame");
        JSONObject record = read(input.getString("id"));
        String take = input.getString("take");
        int frame = input.getInt("frame");
        if (!TAKES.contains(take) || frame<0 || frame>=RouteShot.FRAMES || frame!=input.getDouble("frame"))
            throw new IllegalArgumentException("Choose a take and frame 0–143.");
        var clip = MotionMediaContainers.readClip(bytes(record,take),RouteShot.FRAMES,false);
        if (clip.frames().size()!=RouteShot.FRAMES) throw new IllegalStateException("The stored video clock changed.");
        byte[] jpg = clip.frames().get(frame);
        var proof = record.getJSONObject("audit").getJSONArray("frames").getJSONObject(frame);
        if (!sha(jpg).equals(proof.getString(take+"JpegSha256")))
            throw new IllegalStateException("The preview frame differs from verified video.");
        return new JSONObject().put("base64",Base64.getEncoder().encodeToString(jpg)).put("proof",proof);
    }

    synchronized JSONObject playback(JSONObject input) throws Exception {
        SpatialScene.keys(input,"id","take");
        JSONObject record = read(input.getString("id"));
        String take = input.getString("take");
        var clip = MotionMediaContainers.readClip(bytes(record,take),RouteShot.FRAMES,false);
        if (clip.frames().size()!=RouteShot.FRAMES) throw new IllegalStateException("The stored video clock changed.");
        var proof = record.getJSONObject("audit").getJSONArray("frames");
        var frames = new JSONArray();
        for (int i=0;i<RouteShot.FRAMES;i++) {
            byte[] jpeg=clip.frames().get(i);
            if (!sha(jpeg).equals(proof.getJSONObject(i).getString(take+"JpegSha256")))
                throw new IllegalStateException("A playback frame differs from verified video.");
            frames.put(Base64.getEncoder().encodeToString(jpeg));
        }
        return new JSONObject().put("frames",frames);
    }

    synchronized JSONObject replay(String id) throws Exception {
        var retained = open(id);
        var definition = retained.getJSONObject("definition");
        var bound = source(definition.getString("routeId"),true);
        var fresh = render(renderer(definition,bound));
        if (!record(id,definition,fresh).similar(retained))
            throw new IllegalStateException("Reconstructed frames differ from the saved takes.");
        return retained.put("replayed",true);
    }

    synchronized JSONObject export(String id,String take) throws Exception {
        var record = open(id);
        byte[] output = switch (take) {
            case "json" -> jsonBytes(record);
            case "bundle" -> pack(record);
            default -> bytes(record,take);
        };
        return new JSONObject().put("base64",Base64.getEncoder().encodeToString(output)).put("sha256",sha(output))
                .put("source",new JSONObject().put("shotId",id).put("take",take)
                        .put("routeEvidenceSha256",record.getJSONObject("definition").getString("routeEvidenceSha256")));
    }

    private byte[] pack(JSONObject record) throws Exception {
        var definition = record.getJSONObject("definition");
        if (!RouteShot.EDITABLE_FORMAT.equals(definition.getString("format")))
            throw new IllegalArgumentException("Save an overlay revision before exporting a portable shot.");
        String routeId = definition.getString("routeId");
        var source = routes.hasReview(routeId)?routes.packShotSource(scenes,routeId):
                AgentFiles042.loadChecked(file(routeId,"portable"));
        var clips = new JSONObject();
        for (String take : TAKES) clips.put(take,Base64.getEncoder().encodeToString(bytes(record,take)));
        byte[] result = jsonBytes(new JSONObject().put("format",BUNDLE_FORMAT).put("shot",record).put("route",source).put("video",clips));
        if (result.length>MAX_BUNDLE) throw new IOException("The portable shot exceeds 10 MB.");
        return result;
    }

    synchronized JSONObject importBundle(JSONObject input) throws Exception {
        SpatialScene.keys(input,"bundle");
        var bundle = input.getJSONObject("bundle");
        SpatialScene.keys(bundle,"format","shot","route","video");
        if (!BUNDLE_FORMAT.equals(bundle.getString("format"))) throw new IllegalArgumentException("Unsupported shot package.");
        var record = bundle.getJSONObject("shot");
        String id = record.getString("id");
        file(id,"shot");
        var definition = record.getJSONObject("definition");
        if (!RouteShot.EDITABLE_FORMAT.equals(definition.getString("format")))
            throw new IllegalArgumentException("The package must carry editable overlays.");
        var route = bundle.getJSONObject("route");
        var bound = routes.checkShotSource(route,false);
        checkRoute(bound.mission());
        checkIdentity(id,record,bound);
        var videos = bundle.getJSONObject("video");
        SpatialScene.keys(videos,"clean","annotated");
        byte[] clean = packedClip(videos,record,"clean");
        byte[] annotated = packedClip(videos,record,"annotated");
        var verified = routes.checkShotSource(route,true);
        var fresh = render(renderer(definition,verified));
        if (!record(id,definition,fresh).similar(record) ||
                !Arrays.equals(clean,fresh.clean) || !Arrays.equals(annotated,fresh.annotated))
            throw new IllegalStateException("The portable shot disagrees with freshly rendered frames.");
        String routeId = definition.getString("routeId");
        if (routes.hasReview(routeId)) {
            if (!routes.packShotSource(scenes,routeId).similar(route))
                throw new IllegalStateException("The package conflicts with the existing local route.");
        } else {
            var path = file(routeId,"portable");
            if (Files.exists(path)) {
                if (!AgentFiles042.loadChecked(path).similar(route))
                    throw new IllegalStateException("The existing imported route has different evidence.");
            } else {
                AgentFiles042.saveChecked(path,route);
            }
        }
        retain(record,clean,annotated);
        var opened = open(id);
        remember(id);
        return opened;
    }

    private static byte[] packedClip(JSONObject videos,JSONObject record,String take) throws IOException {
        byte[] bytes = SpriteWorkshop.decode(videos.getString(take),MAX_BUNDLE);
        if (!receipt(bytes).similar(record.getJSONObject("takes").getJSONObject(take)))
            throw new IllegalStateException("The bundled "+take+" video differs from its receipt.");
        return bytes;
    }

    private static byte[] jsonBytes(JSONObject value) {
        return (MapCheckpoint.canonical(value)+"\n").getBytes(StandardCharsets.UTF_8);
    }

    private byte[] bytes(JSONObject record,String take) throws IOException {
        if (!TAKES.contains(take)) throw new IllegalArgumentException("Choose the clean or annotated take.");
        var path = file(record.getString("id"),take);
        if (Files.size(path)>MAX_VIDEO) throw new IOException("Shot media exceeds its verified size limit.");
        byte[] bytes = Files.readAllBytes(path);
        if (!receipt(bytes).similar(record.getJSONObject("takes").getJSONObject(take)))
            throw new IllegalStateException("Saved "+take+" take differs from its receipt.");
        return bytes;
    }

    private static JSONObject receipt(byte[] bytes) {
        return new JSONObject().put("sha256",sha(bytes)).put("bytes",bytes.length);
    }

}
