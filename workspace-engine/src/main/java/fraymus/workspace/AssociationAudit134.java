package fraymus.workspace;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.json.*;

/** Source-only annotations are downstream audit evidence, never tracker input. */
final class AssociationAudit134 {
    static final String VERSION = "association-labels-134-v1";
    private static final Set<String> VISIBILITY = Set.of("visible", "partial", "occluded", "out-of-view", "uncertain");
    private final Path root;
    private final Path imageRoot;
    private final WorldObservationStore observations;

    AssociationAudit134(Path project, WorldObservationStore observations) {
        root = project.resolve("association-labels134");
        imageRoot = project.resolve("world-observations/datasets");
        this.observations = observations;
    }

    static String identity() {
        try (var in = AssociationAudit134.class.getResourceAsStream("AssociationAudit134.class")) {
            try (var extra = AppearanceAudit135.class.getResourceAsStream("AppearanceAudit135.class"); var anchored=AppearanceAudit136.class.getResourceAsStream("AppearanceAudit136.class")) {
                return MeasurementStore.hash(PerceptionStore.hash(in.readAllBytes()) + PerceptionStore.hash(extra.readAllBytes())+PerceptionStore.hash(anchored.readAllBytes()));
            }
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    JSONObject source(String datasetId, int ordinal) throws Exception {
        var d = observations.dataset(datasetId);
        byte[] pixels = observations.image(datasetId, ordinal);
        return new JSONObject().put("datasetId", datasetId).put("datasetSha256", WorldFrames.hash(d))
            .put("title", d.getString("title")).put("source", d.getString("source")).put("license", d.getString("license"))
            .put("width", d.getInt("width")).put("height", d.getInt("height"))
            .put("frameCount", d.getJSONArray("frames").length()).put("objects", d.getJSONArray("objects"))
            .put("ordinal", ordinal).put("frame", d.getJSONArray("frames").getJSONObject(ordinal))
            .put("image", Base64.getEncoder().encodeToString(pixels));
    }

    private JSONObject verifiedDataset(String id) throws Exception {
        var d = observations.dataset(id);
        String hash = WorldFrames.hash(new JSONObject().put("objects", d.getJSONArray("objects")).put("frames", d.getJSONArray("frames")));
        if (!hash.equals(d.getString("contentSha256"))) throw new IllegalStateException("Original dataset manifest changed");
        // dataset() has verified the complete retained observation store once. Dataset manifests
        // are immutable. Check EVERY original image against this snapshot without reopening and
        // hashing the growing world history once per frame. No cached checksum substitutes for bytes.
        UUID.fromString(id);
        var frames = d.getJSONArray("frames");
        for (int i = 0; i < frames.length(); i++) {
            Path file = imageRoot.resolve(id).resolve(i + ".image");
            long size = Files.size(file);
            if (size < 12 || size > 1_500_000) throw new IllegalStateException("Original source image size changed");
            if (!PerceptionStore.hash(Files.readAllBytes(file)).equals(frames.getJSONObject(i).getString("imageSha256"))) throw new IllegalStateException("Original observation image integrity changed");
        }
        return d;
    }

    private static void text(JSONObject input, String key, int bound) {
        String s = input.getString(key);
        if (s.isBlank() || s.length() > bound) throw new IllegalArgumentException("Declare " + key + " within " + bound + " characters");
    }

    static void validate(JSONObject input, JSONObject dataset) {
        SpatialScene.keys(input, "schema", "datasetId", "datasetSha256", "objectKey", "annotator", "method", "provenance", "exposure", "rows");
        if (!VERSION.equals(input.getString("schema")) || !dataset.getString("id").equals(input.getString("datasetId"))
            || !WorldFrames.hash(dataset).equals(input.getString("datasetSha256"))) throw new IllegalArgumentException("Labels must bind the exact original dataset");
        boolean found = false;
        for (Object raw : dataset.getJSONArray("objects")) if (((JSONObject)raw).getString("key").equals(input.getString("objectKey"))) found = true;
        if (!found) throw new IllegalArgumentException("Unknown source object");
        text(input, "annotator", 120); text(input, "provenance", 1500);
        if (!Set.of("human-source-only", "ai-source-only", "external-annotations", "synthetic-control").contains(input.getString("method"))) throw new IllegalArgumentException("Declare annotation method");
        if (!Set.of("before-tracking", "retrospective-source-only", "tracker-results-seen").contains(input.getString("exposure"))) throw new IllegalArgumentException("Declare prior exposure honestly");
        var rows = input.getJSONArray("rows");
        if (rows.isEmpty() || rows.length() > 95) throw new IllegalArgumentException("Provide 1..95 distinct later-frame labels; seed excluded");
        int previous = 0;
        for (Object raw : rows) {
            var r = (JSONObject)raw;
            SpatialScene.keys(r, "ordinal", "sourceFrame", "imageSha256", "visibility", "targetBox", "otherBoxes", "note");
            int ordinal=r.getInt("ordinal");
            if(ordinal!=r.getDouble("ordinal")||ordinal<=previous||ordinal>=dataset.getJSONArray("frames").length())throw new IllegalArgumentException("Ordered distinct later images required");
            previous=ordinal;var frame=dataset.getJSONArray("frames").getJSONObject(ordinal);
            if(r.getDouble("sourceFrame")!=frame.getInt("sourceFrame")||!r.getString("imageSha256").equals(frame.getString("imageSha256")))throw new IllegalArgumentException("Label source changed");
            String visibility=r.getString("visibility");if(!VISIBILITY.contains(visibility))throw new IllegalArgumentException("Unknown visibility");text(r,"note",500);
            if(Set.of("visible","partial").contains(visibility)){if(!PixelTracker.Box.from(r.getJSONArray("targetBox")).measurable())throw new IllegalArgumentException("Visible target requires measurable external box");}
            else if(r.has("targetBox"))throw new IllegalArgumentException("Do not invent visible boxes during loss or uncertainty");
            if(r.has("otherBoxes")){var others=r.getJSONArray("otherBoxes");if(others.length()>16)throw new IllegalArgumentException("Too many distractors");for(Object x:others)if(!PixelTracker.Box.from((JSONArray)x).measurable())throw new IllegalArgumentException("Invalid distractor");}

        }
    }

    synchronized JSONObject save(JSONObject input) throws Exception {
        var dataset = verifiedDataset(input.getString("datasetId"));
        validate(input, dataset);
        String id = WorldFrames.hash(input);
        write("labels", id, new JSONObject().put("id", id).put("input", WorldFrames.copy(input)));
        return read(id);
    }

    JSONObject importRecord(JSONObject record) throws Exception {
        SpatialScene.keys(record, "id", "input");
        if (!WorldFrames.hash(record.getJSONObject("input")).equals(record.getString("id"))) throw new IllegalArgumentException("Exported label identity differs; retain the original and explicitly author a correction");
        return save(record.getJSONObject("input"));
    }

    JSONObject read(String id) throws Exception {
        var record = readFile("labels", id);
        var input = record.getJSONObject("input");
        if (!WorldFrames.hash(input).equals(id)) throw new IllegalStateException("Frozen labels changed");
        validate(input, verifiedDataset(input.getString("datasetId")));
        return record;
    }

    synchronized JSONObject state() throws Exception {
        var labels = new JSONArray(); var audits = new JSONArray();
        for (String type : List.of("labels", "audits")) {
            Path directory = root.resolve(type);
            if (!Files.exists(directory)) continue;
            try (var files = Files.list(directory)) {
                var paths = files.filter(p -> p.toString().endsWith(".json")).sorted().toList();
                if (paths.size() > 64) throw new IllegalStateException("Retained evidence exceeds capacity");
                for (var path : paths) {
                    String id = path.getFileName().toString().replace(".json", "");
                    var record = readFile(type, id);
                    // Full source/outcome verification happens on opening, not silently at listing time.
                    if (type.equals("labels")) labels.put(new JSONObject().put("id", id).put("datasetId", record.getJSONObject("input").getString("datasetId")).put("objectKey", record.getJSONObject("input").getString("objectKey")).put("rows", record.getJSONObject("input").getJSONArray("rows").length()));
                    else audits.put(new JSONObject().put("id", id).put("labelId", record.getJSONObject("input").getString("labelId")).put("runId", record.getJSONObject("input").getString("runId")));
                }
            }
        }
        return new JSONObject().put("labels", labels).put("audits", audits);
    }

    private JSONObject readFile(String type, String id) throws Exception {
        if (!id.matches("[a-f0-9]{64}")) throw new IllegalArgumentException("Invalid evidence identity");
        Path file = root.resolve(type).resolve(id + ".json");
        if (Files.size(file) > 1_000_000) throw new IllegalStateException("Evidence size exceeds bound");
        var record = new JSONObject(Files.readString(file));
        if (type.equals("labels")) SpatialScene.keys(record, "id", "input");
        var payload = WorldFrames.copy(record); payload.remove("id");
        String actual = type.equals("labels") ? WorldFrames.hash(record.getJSONObject("input")) : WorldFrames.hash(payload);
        if (!id.equals(record.getString("id")) || !id.equals(actual)) throw new IllegalStateException("Retained evidence checksum changed");
        return record;
    }

    private void write(String type, String id, JSONObject value) throws Exception {
        Path directory = root.resolve(type); Files.createDirectories(directory);
        try (var channel = FileChannel.open(directory.resolve("write.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE); var lock = channel.lock()) {
            Path file = directory.resolve(id + ".json");
            if (Files.exists(file)) { if (!readFile(type, id).similar(value)) throw new IllegalStateException("Evidence identity conflict"); return; }
            try (var files = Files.list(directory)) { if (files.filter(p -> p.toString().endsWith(".json")).count() >= 64) throw new IllegalStateException("64 retained " + type + " reached; no pruning"); }
            byte[] bytes = value.toString(2).getBytes(StandardCharsets.UTF_8);
            if (bytes.length > 1_000_000) throw new IllegalArgumentException("Evidence size exceeds bound");
            Path pending = directory.resolve(id + ".pending");
            try (var out = FileChannel.open(pending, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                var buffer = ByteBuffer.wrap(bytes); while (buffer.hasRemaining()) out.write(buffer); out.force(true);
            }
            Files.move(pending, file, StandardCopyOption.ATOMIC_MOVE);
        }
    }

    synchronized JSONObject audit(JSONObject input) throws Exception {
        var result = evaluate(input); String id = WorldFrames.hash(result); result.put("id", id);
        write("audits", id, result); return readAudit(id);
    }

    JSONObject readAudit(String id) throws Exception {
        var result = readFile("audits", id); var expected = WorldFrames.copy(result); expected.remove("id");
        if (!evaluate(result.getJSONObject("input")).similar(expected)) throw new IllegalStateException("Original audit source, code or outcome changed");
        return result;
    }

    private JSONObject evaluate(JSONObject input)throws Exception{
        SpatialScene.keys(input,"labelId","runId");var record=read(input.getString("labelId"));var labels=record.getJSONObject("input");var run=observations.read(input.getString("runId"));
        if(!run.getString("status").equals("completed")||!run.getString("datasetId").equals(labels.getString("datasetId")))throw new IllegalArgumentException("Use the same completed labelled source");
        if(!run.getString("codeSha256").equals(WorldObservationStore.identity()))throw new IllegalStateException("Original tracker implementation differs; preserve its historical evidence");
        var dataset=verifiedDataset(run.getString("datasetId"));String body=null;
        for(Object raw:run.getJSONArray("bindings")){var b=(JSONObject)raw;if(b.getString("objectKey").equals(labels.getString("objectKey")))body=b.getString("bodyId");}
        if(body==null)throw new IllegalArgumentException("Missing source-object binding");
        return compute(labels,dataset,run,body).put("input",WorldFrames.copy(input)).put("codeSha256",identity()).put("trackerCodeSha256",run.getString("codeSha256")).put("runSha256",WorldFrames.hash(run)).put("labelSha256",WorldFrames.hash(record)).put("datasetSha256",WorldFrames.hash(dataset)).put("datasetId",dataset.getString("id")).put("trackerPolicy",run.optString("trackerPolicy","legacy-flow"));
    }
    private static JSONObject appearance(JSONObject labels,JSONObject dataset,JSONObject run,String body){
        var trace=run.getJSONArray("trace");if(!trace.isEmpty())for(Object raw:trace.getJSONObject(0).getJSONArray("objects")){
            var o=(JSONObject)raw;if(!o.getString("bodyId").equals(body))continue;var bank=o.getJSONObject("tracker").optJSONObject("appearance135");
            if(bank!=null&&bank.getString("schema").equals("seed-anchored-136-v1"))return AppearanceAudit136.compute(labels,dataset,run,body);
        }
        return AppearanceAudit135.compute(labels,dataset,run,body);
    }

    static JSONObject compute(JSONObject labels,JSONObject dataset,JSONObject run,String body){
        var trace=run.getJSONArray("trace");var frames=dataset.getJSONArray("frames");if(trace.length()!=frames.length()-1)throw new IllegalArgumentException("Complete source trace required");
        var objects=new HashMap<Integer,JSONObject>();
        for(int i=1;i<frames.length();i++){var f=frames.getJSONObject(i);var t=trace.getJSONObject(i-1);if(t.getInt("ordinal")!=i||t.getInt("sourceFrame")!=f.getInt("sourceFrame")||!t.getString("imageSha256").equals(f.getString("imageSha256")))throw new IllegalArgumentException("Trace source/order changed");
            JSONObject found=null;for(Object raw:t.getJSONArray("objects")){var o=(JSONObject)raw;if(o.getString("bodyId").equals(body)){if(found!=null)throw new IllegalArgumentException("Duplicate original binding");found=o;}}if(found==null)throw new IllegalArgumentException("Missing original binding");objects.put(i,found);}
        var scores=new JSONArray();var episodes=new JSONArray();JSONObject episode=null;boolean hiddenBefore=false;
        int visible=0,available=0,correct=0,hidden=0,unsupported=0,switches=0,reacquired=0,correctReacquired=0,uncertain=0;double overlap=0;
        for(Object raw:labels.getJSONArray("rows")){
            var l=(JSONObject)raw;int ordinal=l.getInt("ordinal");var original=frames.getJSONObject(ordinal);if(l.getInt("sourceFrame")!=original.getInt("sourceFrame")||!l.getString("imageSha256").equals(original.getString("imageSha256")))throw new IllegalArgumentException("Label image/order changed");var o=objects.get(ordinal);if(o==null)throw new IllegalArgumentException("Missing labelled frame");var t=o.getJSONObject("tracker");boolean accepted=o.getJSONObject("estimate").getBoolean("accepted")&&!t.isNull("box");
            String visibility=l.getString("visibility");boolean seen=Set.of("visible","partial").contains(visibility),lost=Set.of("occluded","out-of-view").contains(visibility);double iou=0,other=0;
            if(accepted){var box=PixelTracker.Box.from(t.getJSONArray("box"));if(seen)iou=PixelTracker.iou(box,PixelTracker.Box.from(l.getJSONArray("targetBox")));for(Object b:l.optJSONArray("otherBoxes")==null?new JSONArray():l.getJSONArray("otherBoxes"))other=Math.max(other,PixelTracker.iou(box,PixelTracker.Box.from((JSONArray)b)));}
            boolean correctHere=seen&&accepted&&iou>=.5,switched=accepted&&other>=.5&&(!seen||iou<.2);
            if(seen){visible++;if(accepted)available++;if(correctHere)correct++;overlap+=iou;}else if(lost){hidden++;if(accepted)unsupported++;}else uncertain++;
            if(switched)switches++;boolean recovery=t.getString("status").equals("reacquired")&&accepted;if(recovery){reacquired++;if(correctHere)correctReacquired++;}
            if(lost){if(!hiddenBefore){episode=new JSONObject().put("hiddenStart",ordinal).put("visibleReturn",JSONObject.NULL).put("verifiedRecovery",JSONObject.NULL);episodes.put(episode);}hiddenBefore=true;}
            else if(seen&&episode!=null){if(episode.isNull("visibleReturn"))episode.put("visibleReturn",ordinal);if(correctHere&&episode.isNull("verifiedRecovery"))episode.put("verifiedRecovery",ordinal).put("sampleDelay",ordinal-episode.getInt("visibleReturn"));hiddenBefore=false;}
            else if(!seen){hiddenBefore=false;episode=null;}
            scores.put(WorldFrames.copy(l).put("accepted",accepted).put("box",t.opt("box")).put("trackerStatus",t.getString("status")).put("targetOverlap",iou).put("otherOverlap",other).put("correct",correctHere).put("confirmedDistractorMatch",switched).put("unsupportedHiddenMeasurement",accepted&&lost).put("appearance135",t.optJSONObject("appearance135")==null?JSONObject.NULL:AppearanceAudit135.frameSummary(t.getJSONObject("appearance135"))).put("association",t.optJSONObject("association")==null?JSONObject.NULL:t.getJSONObject("association")));
        }
        return new JSONObject().put("appearance135",appearance(labels,dataset,run,body)).put("schema","association-audit-134-v1").put("scores",scores).put("episodes",episodes).put("scope","Independent target boxes and separately declared visibility score original runs. IoU failure is not automatically an identity switch. Distractor match requires overlap with an independently labelled other object. Missing and uncertain labels remain explicit; no physical or personal identity certification.")
          .put("summary",new JSONObject().put("labels",scores.length()).put("visible",visible).put("available",available).put("missing",visible-available).put("correct",correct).put("overlapFailures",available-correct).put("meanOverlapAllVisible",visible==0?JSONObject.NULL:overlap/visible).put("hidden",hidden).put("unsupported",unsupported).put("confirmedDistractorMatches",switches).put("reacquired",reacquired).put("correctReacquired",correctReacquired).put("uncertain",uncertain));
    }
}
