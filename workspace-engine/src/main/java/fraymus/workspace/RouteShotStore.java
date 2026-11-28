package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

/** Persists editorial shots and binds both video takes to one approved route and camera track. */
final class RouteShotStore {
    private static final int MAX_VIDEO = 16_000_000;
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
        SpatialScene.keys(input,"routeId","cameraKeys");
        String routeId = input.getString("routeId");
        JSONObject bound = approved(routeId,false);
        JSONObject review = bound.getJSONObject("review");
        JSONArray keys = input.has("cameraKeys")?input.getJSONArray("cameraKeys"):RouteShot.defaultKeys(review);
        var definition = definition(bound,keys);
        String id = WorldFrames.hash(definition);
        JSONObject record = Files.exists(file(id,"shot"))?open(id):create(id,definition,bound);
        AgentFiles042.saveChecked(root.resolve("latest.json"),new JSONObject().put("id",id));
        return record;
    }

    private JSONObject create(String id,JSONObject definition,JSONObject bound) throws Exception {
        approved(definition.getString("routeId"),true);
        ShotOutput output = render(renderer(definition,bound));
        byte[] clean = output.clean;
        byte[] annotated = output.annotated;
        if (clean.length>MAX_VIDEO || annotated.length>MAX_VIDEO)
            throw new IOException("Route-shot video exceeds its bounded output size.");
        var record = new JSONObject().put("id",id).put("definition",definition).put("audit",output.audit)
                .put("takes",new JSONObject().put("clean",receipt(clean)).put("annotated",receipt(annotated)));
        SpriteWorkshop.replace(file(id,"clean"),clean);
        SpriteWorkshop.replace(file(id,"annotated"),annotated);
        AgentFiles042.saveChecked(file(id,"shot"),record);
        return open(id);
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

    private JSONObject approved(String id,boolean replay) throws Exception {
        file(id,"shot");
        JSONObject record = replay?routes.replay(scenes,id):routes.read(scenes,id);
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
        record.remove("replayed");
        return record;
    }

    private static JSONObject definition(JSONObject bound,JSONArray keys) {
        RouteShot.readKeys(keys);
        var review = bound.getJSONObject("review");
        return new JSONObject().put("format",RouteShot.FORMAT).put("motion","planned-route")
                .put("routeId",bound.getString("id")).put("routeEvidenceSha256",WorldFrames.hash(bound))
                .put("sceneId",review.getString("parentId")).put("sceneSha256",review.getString("parentSha256"))
                .put("crate",review.getString("crate")).put("pathM",review.getJSONObject("route").getJSONArray("pointsM"))
                .put("cameraKeys",keys).put("fps",RouteShot.FPS).put("frameCount",RouteShot.FRAMES)
                .put("width",RouteShot.WIDTH).put("height",RouteShot.HEIGHT);
    }

    private RouteShot renderer(JSONObject definition,JSONObject bound) throws Exception {
        return new RouteShot(scenes.read(definition.getString("sceneId")),bound.getJSONObject("review"),
                definition.getJSONArray("cameraKeys"));
    }

    private JSONObject read(String id) throws Exception {
        JSONObject record = AgentFiles042.loadChecked(file(id,"shot"));
        JSONObject definition = record.getJSONObject("definition");
        if (!id.equals(record.getString("id")) || !id.equals(WorldFrames.hash(definition))
                || !definition.similar(definition(approved(definition.getString("routeId"),false),definition.getJSONArray("cameraKeys"))))
            throw new IllegalStateException("The source scene, route or shot definition changed.");
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
        var bound = approved(definition.getString("routeId"),true);
        var fresh = render(renderer(definition,bound));
        if (!fresh.audit.similar(retained.getJSONObject("audit")) ||
                !receipt(fresh.clean).similar(retained.getJSONObject("takes").getJSONObject("clean")) ||
                !receipt(fresh.annotated).similar(retained.getJSONObject("takes").getJSONObject("annotated")))
            throw new IllegalStateException("Reconstructed frames differ from the saved takes.");
        return retained.put("replayed",true);
    }

    synchronized JSONObject export(String id,String take) throws Exception {
        var record = open(id);
        byte[] output = take.equals("json")?(MapCheckpoint.canonical(record)+"\n").getBytes(StandardCharsets.UTF_8):bytes(record,take);
        return new JSONObject().put("base64",Base64.getEncoder().encodeToString(output)).put("sha256",sha(output))
                .put("source",new JSONObject().put("shotId",id).put("take",take)
                        .put("routeEvidenceSha256",record.getJSONObject("definition").getString("routeEvidenceSha256")));
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

    private static String sha(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
