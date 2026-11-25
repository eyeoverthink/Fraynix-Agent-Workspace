package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;

final class MotionMedia {
    private static final String FORMAT = "fraynix.motion-media-v1";
    private static final List<String> EXTENSIONS = List.of("png","wav","avi","json");
    private static final int MAX_ARTIFACT = 16_000_000;
    private final Path root;
    private final SceneStore scenes;

    MotionMedia(Path project,SceneStore scenes) {
        root=project.resolve("motion-media");
        this.scenes=scenes;
    }

    JSONObject sources(JSONObject input) {
        SpatialScene.keys(input);
        var result=new JSONArray();
        JSONArray runs=scenes.runs("");
        for (int i=runs.length()-1;i>=0 && result.length()<40;i--) {
            var row=runs.getJSONObject(i);
            if (row.optString("status").equals("completed") && MotionTimeline.eligible(scenes.runRecord(row.getString("id"))))
                result.put(row);
        }
        return new JSONObject().put("runs",result);
    }

    synchronized JSONObject build(JSONObject input) throws Exception {
        SpatialScene.keys(input,"runId");
        var run = scenes.runRecord(input.getString("runId"));
        var source = binding(run);
        String id = WorldFrames.hash(source);
        Path receipt = path(id,"receipt");
        if (Files.exists(receipt,LinkOption.NOFOLLOW_LINKS)) {
            var retained=check(id);
            retainLatest(id);
            return present(retained);
        }
        var timeline = new MotionTimeline(run);
        var sound = timeline.sound();
        var video = timeline.video(rememberedEncoder());
        byte[] poster = timeline.poster(), wav = MotionMediaContainers.wav(sound.pcm());
        byte[] avi = MotionMediaContainers.avi(video.frames(),sound.pcm());
        MotionMediaContainers.checkWav(wav,sound.pcm());
        MotionMediaContainers.checkAvi(avi,video.frames(),sound.pcm());
        var decoded = ImageIO.read(new ByteArrayInputStream(poster));
        if (decoded==null || decoded.getWidth()!=MotionMediaContainers.WIDTH || decoded.getHeight()!=MotionMediaContainers.HEIGHT)
            throw new IOException("The still image did not decode at the timeline dimensions.");
        var artifacts=new LinkedHashMap<String,byte[]>();
        artifacts.put("png",poster);
        artifacts.put("wav",wav);
        artifacts.put("avi",avi);
        artifacts.put("json",new JSONObject().put("format",FORMAT).put("source",source)
                .put("timeline",timeline.sourceTimeline()).put("encoder",video.evidence())
                .put("sound",sound.evidence()).toString(2).getBytes(StandardCharsets.UTF_8));
        var files=new JSONObject();
        for (var file:artifacts.entrySet()) {
            byte[] bytes=file.getValue();
            if (bytes.length==0 || bytes.length>MAX_ARTIFACT) throw new IOException("A media output exceeds its bounded size.");
            files.put(file.getKey(),new JSONObject().put("bytes",bytes.length).put("sha256",MapDefinition.sha(bytes)));
        }
        var result = new JSONObject().put("id",id).put("source",source).put("files",files)
                .put("frames",timeline.frameCount()).put("durationS",timeline.duration())
                .put("width",MotionMediaContainers.WIDTH).put("height",MotionMediaContainers.HEIGHT)
                .put("encoder",video.evidence()).put("sound",sound.evidence());
        Files.createDirectories(root);
        for (var file:artifacts.entrySet()) SpriteWorkshop.replace(path(id,file.getKey()),file.getValue());
        AgentFiles042.saveChecked(receipt,result);
        retainLatest(id);
        return present(check(id));
    }

    synchronized JSONObject latest(JSONObject input) throws Exception {
        SpatialScene.keys(input);
        String retained=latestId();
        return retained==null ? new JSONObject() : present(check(retained));
    }

    private String latestId() throws IOException {
        Path latest=root.resolve("latest");
        if (!Files.exists(latest,LinkOption.NOFOLLOW_LINKS)) return null;
        return new String(SpriteWorkshop.read(latest,64),StandardCharsets.US_ASCII);
    }

    private JSONObject rememberedEncoder() {
        try {
            String previous=latestId();
            if (previous==null) return null;
            var checked=check(previous);
            return new JSONObject().put("recipeId",previous).put("quality",checked.getJSONObject("encoder").getNumber("quality"));
        } catch (IOException | RuntimeException stale) {
            // A stale package cannot guide a new encode. New work can still qualify its own settings.
            return null;
        }
    }

    synchronized JSONObject open(JSONObject input) throws Exception {
        SpatialScene.keys(input,"id");
        return present(check(input.getString("id")));
    }

    synchronized JSONObject export(JSONObject input) throws Exception {
        SpatialScene.keys(input,"id","format");
        String id=input.getString("id"), extension=input.getString("format");
        if (!EXTENSIONS.contains(extension)) throw new IllegalArgumentException("Choose PNG, WAV, AVI, or timeline JSON.");
        var receipt=check(id);
        byte[] bytes=asset(receipt,extension);
        return new JSONObject().put("base64",Base64.getEncoder().encodeToString(bytes))
                .put("sha256",receipt.getJSONObject("files").getJSONObject(extension).getString("sha256"))
                .put("source",receipt.getJSONObject("source"));
    }

    private JSONObject check(String id) throws IOException {
        var saved = AgentFiles042.loadChecked(path(id,"receipt"));
        var source=saved.getJSONObject("source");
        if (!saved.getString("id").equals(id) || !WorldFrames.hash(source).equals(id) ||
                !source.similar(binding(scenes.runRecord(source.getString("runId")))))
            throw new IOException("The original native recording differs from the media recipe.");
        for (String format:EXTENSIONS) asset(saved,format);
        return saved;
    }

    private static JSONObject binding(JSONObject run) {
        if (!MotionTimeline.eligible(run)) throw new IllegalArgumentException("Select a completed native motion run.");
        return new JSONObject().put("recipe",FORMAT).put("runId",run.getString("id")).put("title",run.getString("title"))
                .put("sceneVersion",run.getInt("sceneVersion")).put("runSha256",WorldFrames.hash(run));
    }

    private JSONObject present(JSONObject saved) throws IOException {
        return saved.put("posterPng",Base64.getEncoder().encodeToString(asset(saved,"png")));
    }

    private void retainLatest(String id) throws IOException {
        SpriteWorkshop.replace(root.resolve("latest"),id.getBytes(StandardCharsets.US_ASCII));
    }

    private byte[] asset(JSONObject record,String extension) throws IOException {
        var file=record.getJSONObject("files").getJSONObject(extension);
        try {
            byte[] data=SpriteWorkshop.read(path(record.getString("id"),extension),MAX_ARTIFACT,file.getString("sha256"));
            if (file.getInt("bytes")!=data.length) throw new IOException("The recorded media length changed.");
            return data;
        } catch (IOException | IllegalArgumentException error) {
            throw new IOException("The saved "+extension.toUpperCase(java.util.Locale.ROOT)+" differs from its verified bytes.",error);
        }
    }

    private Path path(String id,String suffix) {
        if (!id.matches("[a-f0-9]{64}")) throw new IllegalArgumentException("Invalid motion media recipe ID.");
        return root.resolve(id+"."+suffix);
    }
}
