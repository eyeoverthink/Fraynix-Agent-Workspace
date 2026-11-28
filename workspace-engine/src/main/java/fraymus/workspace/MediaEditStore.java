package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import static fraymus.workspace.MotionMediaContainers.*;

final class MediaEditStore {
    private static final String FORMAT="fraynix.timeline-edit-v1";
    private static final List<String> TYPES=List.of("avi","wav","png");
    private static final List<String> OUTPUTS=List.of("avi","wav","png","json");
    private static final int MAX_SOURCE=6_000_000, MAX_OUTPUT=16_000_000;
    private final Path root, bank, edits;
    private final MotionMedia generated;
    private final RouteShotStore routeShots;

    MediaEditStore(Path project,MotionMedia generated,RouteShotStore routeShots) {
        root=project.resolve("timeline");
        bank=root.resolve("assets");
        edits=root.resolve("edits");
        this.generated=generated;
        this.routeShots=routeShots;
    }

    synchronized JSONObject importAsset(JSONObject input) throws Exception {
        SpatialScene.keys(input,"filename","base64");
        String name=SpatialScene.text(input,"filename",130);
        int dot=name.lastIndexOf('.');
        String type=dot<0?"":name.substring(dot+1).toLowerCase(Locale.ROOT);
        if (!TYPES.contains(type)) throw new IllegalArgumentException("Import PNG, 24 fps MJPEG AVI, or mono PCM WAV.");
        byte[] bytes=SpriteWorkshop.decode(input.getString("base64"),MAX_SOURCE);
        return retainAsset(name,type,bytes,new JSONObject().put("type","imported"));
    }

    synchronized JSONObject attach(JSONObject input) throws Exception {
        SpatialScene.keys(input,"id");
        String id=input.optString("id","");
        var recipe=id.isBlank()?generated.latest(new JSONObject()):generated.open(new JSONObject().put("id",id));
        if (!recipe.has("id")) throw new IllegalArgumentException("Build a motion media package first.");
        String pinned=recipe.getString("id");
        var items=new JSONArray();
        for (String type:TYPES) {
            var export=generated.export(new JSONObject().put("id",pinned).put("format",type));
            var origin=new JSONObject().put("type","generated").put("recipeId",pinned)
                    .put("nativeSource",export.getJSONObject("source"));
            byte[] bytes=SpriteWorkshop.decode(export.getString("base64"),MAX_OUTPUT);
            items.put(retainAsset(recipe.getJSONObject("source").getString("title")+" · "+type.toUpperCase(Locale.ROOT),
                    type,bytes,origin));
        }
        return new JSONObject().put("assets",items).put("recipeId",pinned);
    }

    synchronized JSONObject attachRouteShot(JSONObject input) throws Exception {
        SpatialScene.keys(input,"id");
        String id = input.getString("id");
        var items = new JSONArray();
        for (String take : List.of("clean","annotated")) {
            var export = routeShots.export(id,take);
            var origin = new JSONObject().put("type","route-shot").put("nativeSource",export.getJSONObject("source"));
            byte[] bytes = SpriteWorkshop.decode(export.getString("base64"),MAX_OUTPUT);
            items.put(retainAsset("Loading-bay shot · "+take,"avi",bytes,origin));
        }
        return new JSONObject().put("assets",items).put("shotId",id);
    }

    private JSONObject retainAsset(String name,String type,byte[] bytes,JSONObject origin) throws Exception {
        var source=new JSONObject().put("title",name).put("format",type).put("origin",origin).put("sha256",MapDefinition.sha(bytes));
        String id=WorldFrames.hash(source);
        var assets=decode(WorldFrames.copy(source).put("id",id),bytes);
        var record=new JSONObject().put("id",id).put("source",source)
                .put("pictureFrames",assets.frames(EditableMedia.Lane.PICTURE))
                .put("soundFrames",assets.frames(EditableMedia.Lane.AUDIO1));
        Files.createDirectories(bank);
        Path metadata=path(bank,id,"asset");
        if (Files.exists(metadata,LinkOption.NOFOLLOW_LINKS)) {
            var previous=readAsset(id);
            if (!previous.similar(record)) throw new IOException("The timeline asset binding changed.");
            verifyAssetBytes(previous);
            return previous;
        }
        if (origin.getString("type").equals("imported")) SpriteWorkshop.replace(path(bank,id,"source"),bytes);
        AgentFiles042.saveChecked(metadata,record);
        return record;
    }

    synchronized JSONObject list(JSONObject input) throws IOException {
        SpatialScene.keys(input);
        var assets=new JSONArray();
        if (Files.isDirectory(bank,LinkOption.NOFOLLOW_LINKS)) try (var files=Files.list(bank)) {
            var sorted=files.map(file->file.getFileName().toString()).filter(file->file.matches("[a-f0-9]{64}[.]asset")).sorted().toList();
            for (String file:sorted) assets.put(readAsset(file.substring(0,64)));
        }
        return new JSONObject().put("assets",assets);
    }

    synchronized JSONObject render(JSONObject input) throws Exception {
        return render(input, true);
    }

    synchronized JSONObject renderPreview(JSONObject input) throws Exception {
        return render(input, false);
    }

    synchronized JSONObject verifiedRevision(String id) throws Exception {
        return check(id);
    }

    synchronized JSONObject assetSummary(String id) throws IOException {
        return readAsset(id);
    }

    synchronized byte[] verifiedVideo(JSONObject record) throws IOException {
        return output(record, "avi");
    }

    synchronized JSONObject playback(String id) throws Exception {
        var verified = check(id);
        var clip = readClip(output(verified, "avi"), EditableMedia.MAX_FRAMES, false);
        var frames = new JSONArray();
        var encoder = Base64.getEncoder();
        for (byte[] frame : clip.frames()) frames.put(encoder.encodeToString(frame));
        byte[] sound = wav(clip.pcm());
        if (!Arrays.equals(sound, output(verified, "wav")) || clip.pcm().length != clip.frames().size() * PCM_PER_FRAME)
            throw new IOException("The rendered picture and sound no longer share the approved timeline.");
        return new JSONObject().put("frames", frames).put("fps", FPS).put("wav", encoder.encodeToString(sound));
    }

    private JSONObject render(JSONObject input, boolean publish) throws Exception {
        SpatialScene.keys(input,"title","strips","parent");
        String title=SpatialScene.text(input,"title",90);
        String parent=input.optString("parent","");
        // An edit explicitly names its source revision. Verify it before accepting the new edit.
        if (!parent.isEmpty()) check(parent);
        var plan=new JSONObject().put("format",FORMAT).put("title",title);
        if (!parent.isEmpty()) plan.put("parent",parent);
        JSONArray raw=input.getJSONArray("strips");
        if (raw.length()==0 || raw.length()>20) throw new IllegalArgumentException("Choose 1 to 20 timeline strips.");
        var strips=new ArrayList<EditableMedia.Strip>();
        var rows=new JSONArray();
        var assets=new JSONObject();
        var loaded=new HashMap<String,EditableMedia.Asset>();
        for (Object entry:raw) {
            var row=(JSONObject)entry;
            String id=row.getString("assetId");
            var lane=EditableMedia.Lane.valueOf(row.getString("lane"));
            if (lane==EditableMedia.Lane.OVERLAY) SpatialScene.keys(row,"lane","assetId","start","in","length","x","y","size","opacity");
            else if (lane.audio()) SpatialScene.keys(row,"lane","assetId","start","in","length","gain");
            else SpatialScene.keys(row,"lane","assetId","start","in","length");
            var asset=loaded.get(id);
            if (asset==null) {
                var bound=readAsset(id);
                asset=decode(WorldFrames.copy(bound.getJSONObject("source")).put("id",id),verifyAssetBytes(bound));
                loaded.put(id,asset);
                assets.put(id,bound);
            }
            int start=integer(row,"start",0,EditableMedia.MAX_FRAMES-1), in=integer(row,"in",0,EditableMedia.MAX_SOURCE_FRAMES-1);
            int length=integer(row,"length",1,EditableMedia.MAX_FRAMES);
            int x=lane==EditableMedia.Lane.OVERLAY?integer(row,"x",-WIDTH,WIDTH):0;
            int y=lane==EditableMedia.Lane.OVERLAY?integer(row,"y",-HEIGHT,HEIGHT):0;
            int size=lane==EditableMedia.Lane.OVERLAY?integer(row,"size",1,200):100;
            int opacity=lane==EditableMedia.Lane.OVERLAY?integer(row,"opacity",1,100):100;
            double gain=lane.audio()?SpatialScene.number(row,"gain",0,1):1;
            var clip=new EditableMedia.Strip(lane,asset,start,in,length,x,y,size,opacity,gain);
            strips.add(clip);
            rows.put(clip.json());
        }
        plan.put("strips",rows).put("assets",assets);
        String id=WorldFrames.hash(plan);
        Files.createDirectories(edits);
        if (Files.exists(path(edits,id,"receipt"),LinkOption.NOFOLLOW_LINKS)) {
            var previous=check(id);
            if (publish) retainLatest(id);
            return display(previous);
        }
        var film=new EditableMedia(strips).render();
        var files=new LinkedHashMap<>(film.files());
        files.put("json",plan.toString(2).getBytes(StandardCharsets.UTF_8));
        var manifests=new JSONObject();
        for (var entry:files.entrySet()) {
            byte[] value=entry.getValue();
            if (value.length==0 || value.length>MAX_OUTPUT) throw new IOException("A timeline export exceeded its size limit.");
            SpriteWorkshop.replace(path(edits,id,entry.getKey()),value);
            manifests.put(entry.getKey(),new JSONObject().put("sha256",MapDefinition.sha(value)).put("bytes",value.length));
        }
        var result=new JSONObject().put("id",id).put("definition",plan).put("metrics",film.metrics()).put("files",manifests);
        AgentFiles042.saveChecked(path(edits,id,"receipt"),result);
        if (publish) retainLatest(id);
        return display(check(id));
    }

    synchronized JSONObject latest(JSONObject input) throws Exception {
        SpatialScene.keys(input);
        Path pointer=root.resolve("latest");
        if (!Files.exists(pointer,LinkOption.NOFOLLOW_LINKS)) return new JSONObject();
        return display(check(new String(SpriteWorkshop.read(pointer,64),StandardCharsets.US_ASCII)));
    }

    synchronized JSONObject open(JSONObject input) throws Exception {
        SpatialScene.keys(input,"id");
        return display(check(input.getString("id")));
    }

    synchronized JSONObject frame(JSONObject input) throws Exception {
        SpatialScene.keys(input,"id","frame");
        var checked=check(input.getString("id"));
        int last=checked.getJSONObject("metrics").getInt("frames")-1;
        int frame=integer(input,"frame",0,last);
        var clip=readClip(output(checked,"avi"),EditableMedia.MAX_FRAMES,false);
        var image=decodeFrame(clip.frames().get(frame),clip.width(),clip.height());
        var imageBytes=new ByteArrayOutputStream();
        ImageIO.write(image,"png",imageBytes);
        return new JSONObject().put("frame",frame).put("png",Base64.getEncoder().encodeToString(imageBytes.toByteArray()));
    }

    synchronized JSONObject export(JSONObject input) throws Exception {
        SpatialScene.keys(input,"id","format");
        String format=input.getString("format");
        if (!OUTPUTS.contains(format)) throw new IllegalArgumentException("Export AVI, WAV, PNG or editable JSON.");
        var checked=check(input.getString("id"));
        return new JSONObject().put("base64",Base64.getEncoder().encodeToString(output(checked,format)))
                .put("id",checked.getString("id")).put("sha256",checked.getJSONObject("files").getJSONObject(format).getString("sha256"));
    }

    private JSONObject check(String id) throws Exception {
        var record=AgentFiles042.loadChecked(path(edits,id,"receipt"));
        var def=record.getJSONObject("definition");
        if (!id.equals(record.getString("id")) || !id.equals(WorldFrames.hash(def))
                || !def.getString("format").equals(FORMAT)) throw new IOException("The edit definition differs from its retained revision.");
        var assets=def.getJSONObject("assets");
        for (String asset:assets.keySet()) {
            var stored=readAsset(asset);
            if (!stored.similar(assets.getJSONObject(asset))) throw new IOException("The edit's asset binding changed.");
            verifyAssetBytes(stored);
        }
        for (String extension:OUTPUTS) output(record,extension);
        return record;
    }

    private JSONObject readAsset(String id) throws IOException {
        var stored=AgentFiles042.loadChecked(path(bank,id,"asset"));
        if (!id.equals(stored.getString("id")) || !id.equals(WorldFrames.hash(stored.getJSONObject("source"))))
            throw new IOException("The timeline asset binding changed.");
        return stored;
    }

    private byte[] verifyAssetBytes(JSONObject record) throws Exception {
        var source=record.getJSONObject("source");
        var origin=source.getJSONObject("origin");
        String hash=source.getString("sha256");
        try {
            if (origin.getString("type").equals("imported"))
                return SpriteWorkshop.read(path(bank,record.getString("id"),"source"),MAX_SOURCE,hash);
            JSONObject nativeSource = origin.getJSONObject("nativeSource");
            var export = origin.getString("type").equals("route-shot")
                    ? routeShots.export(nativeSource.getString("shotId"),nativeSource.getString("take"))
                    : generated.export(new JSONObject().put("id",origin.getString("recipeId")).put("format",source.getString("format")));
            var bytes=SpriteWorkshop.decode(export.getString("base64"),MAX_OUTPUT);
            if (!MapDefinition.sha(bytes).equals(hash) || !export.getJSONObject("source").similar(origin.getJSONObject("nativeSource")))
                throw new IOException("Generated media differs from its bound native evidence.");
            return bytes;
        } catch (IllegalArgumentException | IOException error) {
            throw new IOException("Timeline source "+source.getString("title")+" differs from its verified bytes.",error);
        }
    }

    private byte[] output(JSONObject record,String extension) throws IOException {
        var part=record.getJSONObject("files").getJSONObject(extension);
        try {
            var bytes=SpriteWorkshop.read(path(edits,record.getString("id"),extension),MAX_OUTPUT,part.getString("sha256"));
            if (bytes.length!=part.getInt("bytes")) throw new IOException("The output length changed.");
            return bytes;
        } catch (IllegalArgumentException | IOException error) {
            throw new IOException("The saved timeline "+extension.toUpperCase(Locale.ROOT)+" differs from its verified bytes.",error);
        }
    }

    private JSONObject display(JSONObject record) throws IOException {
        return record.put("posterPng",Base64.getEncoder().encodeToString(output(record,"png")));
    }

    private void retainLatest(String id) throws IOException {
        SpriteWorkshop.replace(root.resolve("latest"),id.getBytes(StandardCharsets.US_ASCII));
    }

    private static EditableMedia.Asset decode(JSONObject source,byte[] bytes) throws Exception {
        return switch (source.getString("format")) {
            case "png" -> new EditableMedia.Asset(source,BrowserShapeTrace.decode(bytes,4),null,new byte[0]);
            case "wav" -> new EditableMedia.Asset(source,null,null,readPcmWav(bytes,EditableMedia.MAX_SOURCE_FRAMES));
            case "avi" -> {
                ImportedClip clip=readClip(bytes,EditableMedia.MAX_SOURCE_FRAMES);
                yield new EditableMedia.Asset(source,null,clip,clip.pcm());
            }
            default -> throw new IllegalArgumentException("The timeline asset format is unsupported.");
        };
    }

    private static int integer(JSONObject input,String key,int lo,int hi) {
        double value=SpatialScene.number(input,key,lo,hi);
        if (value!=Math.rint(value)) throw new IllegalArgumentException(key+" must be a whole timeline frame or pixel.");
        return (int)value;
    }

    private static Path path(Path dir,String id,String suffix) {
        if (!id.matches("[a-f0-9]{64}")) throw new IllegalArgumentException("Invalid timeline ID.");
        return dir.resolve(id+"."+suffix);
    }
}
