package fraymus.workspace;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.json.*;

/** New pixel-derived support evidence on a retained run. Existing metric anchors never change. */
final class ContactAudit133 {
    private final Path root,images;
    private final WorldObservationStore observations;
    private final ObservationLabels132 labels;
    ContactAudit133(Path project,WorldObservationStore observations,ObservationLabels132 labels){root=project.resolve("contact-audits133");images=project.resolve("world-observations/datasets");this.observations=observations;this.labels=labels;}
    static String identity(){var s=new StringBuilder("visual-support-audit-133-v1");for(Class<?> c:List.of(ContactAudit133.class,VisualSupport133.class,ObservationLabels132.class,PixelTracker.class,PixelTracker.Box.class))try(var in=c.getResourceAsStream(c.getName().substring(c.getName().lastIndexOf('.')+1)+".class")){s.append(PerceptionStore.hash(in.readAllBytes()));}catch(Exception e){throw new IllegalStateException(e);}return MeasurementStore.hash(s.toString());}
    synchronized JSONObject state()throws Exception{var out=new JSONArray();if(Files.exists(root))try(var paths=Files.list(root)){var files=paths.filter(p->p.toString().endsWith(".json")).sorted().toList();if(files.size()>64)throw new IllegalStateException("Contact audit capacity exceeded");for(var p:files){var r=readFile(p.getFileName().toString().replace(".json",""));out.put(new JSONObject().put("id",r.getString("id")).put("input",r.getJSONObject("input")));}}return new JSONObject().put("audits",out);}
    private JSONObject readFile(String id)throws Exception{
        if(!id.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Invalid contact evidence identity");Path p=root.resolve(id+".json");if(Files.size(p)>2_000_000)throw new IllegalStateException("Contact audit exceeds bound");var r=new JSONObject(Files.readString(p));var copy=WorldFrames.copy(r);copy.remove("id");if(!id.equals(r.getString("id"))||!id.equals(WorldFrames.hash(copy)))throw new IllegalStateException("Contact audit checksum changed");return r;
    }
    synchronized JSONObject audit(JSONObject input)throws Exception{
        var result=evaluate(input);String id=WorldFrames.hash(result);result.put("id",id);Files.createDirectories(root);
        try(var channel=FileChannel.open(root.resolve("write.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);var lock=channel.lock()){
            if(Files.exists(root.resolve(id+".json"))){if(!readFile(id).similar(result))throw new IllegalStateException("Contact identity conflict");}
            else{try(var files=Files.list(root)){if(files.filter(p->p.toString().endsWith(".json")).count()>=64)throw new IllegalStateException("64 retained contact audits reached; no pruning");}
                byte[] bytes=result.toString(2).getBytes(StandardCharsets.UTF_8);if(bytes.length>2_000_000)throw new IllegalArgumentException("Contact audit exceeds bound");Path pending=root.resolve(id+".pending");try(var file=FileChannel.open(pending,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE)){var buffer=ByteBuffer.wrap(bytes);while(buffer.hasRemaining())file.write(buffer);file.force(true);}Files.move(pending,root.resolve(id+".json"),StandardCopyOption.ATOMIC_MOVE);
            }
        }
        return read(id);
    }
    JSONObject read(String id)throws Exception{var result=readFile(id);var expected=WorldFrames.copy(result);expected.remove("id");if(!evaluate(result.getJSONObject("input")).similar(expected))throw new IllegalStateException("Original contact source, implementation or outcome changed");return result;}
    private JSONObject evaluate(JSONObject input)throws Exception{
        SpatialScene.keys(input,"labelId","runId");var labelRecord=labels.read(input.getString("labelId"));var annotation=labelRecord.getJSONObject("input");var run=observations.read(input.getString("runId"));
        if(!run.getString("status").equals("completed")||!run.getString("datasetId").equals(annotation.getString("datasetId")))throw new IllegalArgumentException("Choose a completed run from the labelled source");
        var dataset=observations.dataset(run.getString("datasetId"));if(!WorldFrames.hash(dataset).equals(annotation.getString("datasetSha256")))throw new IllegalStateException("Label source changed");
        String body=null;for(Object raw:run.getJSONArray("bindings")){var b=(JSONObject)raw;if(b.getString("objectKey").equals(annotation.getString("objectKey")))body=b.getString("bodyId");}if(body==null)throw new IllegalArgumentException("No original source-object binding");
        PixelTracker.Box seed=null;for(Object raw:dataset.getJSONArray("objects")){var o=(JSONObject)raw;if(o.getString("key").equals(annotation.getString("objectKey")))seed=PixelTracker.Box.from(o.getJSONArray("box"));}if(seed==null)throw new IllegalArgumentException("No original seed");
        var frames=dataset.getJSONArray("frames");var trace=run.getJSONArray("trace");if(trace.length()!=frames.length()-1)throw new IllegalStateException("Incomplete original trace");UUID.fromString(dataset.getString("id"));
        var marked=new HashMap<Integer,JSONObject>();for(Object raw:annotation.getJSONArray("rows")){var a=(JSONObject)raw;marked.put(a.getInt("ordinal"),a);}
        VisualSupport133 support=null;var scores=new JSONArray();int observable=0,available=0,within=0,unobservable=0,unsupported=0,associated=0,boxAvailable=0;double total=0,boxTotal=0;
        for(int i=0;i<frames.length();i++){
            var source=frames.getJSONObject(i);Path file=images.resolve(dataset.getString("id")).resolve(i+".image");long size=Files.size(file);if(size<12||size>1_500_000)throw new IllegalStateException("Original image size changed");byte[] bytes=Files.readAllBytes(file);if(!PerceptionStore.hash(bytes).equals(source.getString("imageSha256")))throw new IllegalStateException("Original image checksum changed");
            if(i==0){support=new VisualSupport133(WorldObservationStore.decode(bytes),seed);continue;}
            var frame=trace.getJSONObject(i-1);if(frame.getInt("ordinal")!=i||frame.getInt("sourceFrame")!=source.getInt("sourceFrame")||!frame.getString("imageSha256").equals(source.getString("imageSha256")))throw new IllegalStateException("Trace source changed");
            JSONObject object=null;for(Object raw:frame.getJSONArray("objects")){var o=(JSONObject)raw;if(o.getString("bodyId").equals(body)){if(object!=null)throw new IllegalStateException("Duplicate object binding");object=o;}}if(object==null)throw new IllegalStateException("Missing original object");
            if(!marked.containsKey(i))continue;
            var annotationRow=marked.get(i);var tracker=object.getJSONObject("tracker");boolean accepted=object.getJSONObject("estimate").getBoolean("accepted")&&!tracker.isNull("box");
            var contact=accepted?support.support(WorldObservationStore.decode(bytes),PixelTracker.Box.from(tracker.getJSONArray("box"))):VisualSupport133.unavailable("original association/filter withheld measurement");
            if(accepted&&tracker.has("contact133")&&!contact.similar(tracker.getJSONObject("contact133")))throw new IllegalStateException("Retained contact differs from recomputed original pixels");
            var row=WorldFrames.copy(annotationRow).put("associationAvailable",accepted).put("box",tracker.opt("box")).put("contact",contact).put("referenceContact",annotationRow.getString("contact"));if(tracker.has("association"))row.put("association",tracker.getJSONObject("association"));
            boolean found=!contact.isNull("pointImage");if(accepted)associated++;
            if(annotationRow.getString("contact").equals("observed")){
                observable++;var point=annotationRow.getJSONArray("pointImage");
                if(accepted){var b=PixelTracker.Box.from(tracker.getJSONArray("box"));double e=error(new JSONArray(new double[]{b.x()+b.w()/2,b.y()+b.h()}),point,dataset);row.put("boxErrorPx",e);boxAvailable++;boxTotal+=e;}
                if(found){double e=error(contact.getJSONArray("pointImage"),point,dataset);row.put("contactErrorPx",e).put("withinLabelRadius",e<=annotationRow.getDouble("radiusPx"));total+=e;available++;if(e<=annotationRow.getDouble("radiusPx"))within++;}
            }else{unobservable++;if(found)unsupported++;}
            scores.put(row);
        }
        return new JSONObject().put("schema","visual-support-audit-133-v1").put("input",WorldFrames.copy(input)).put("datasetId",dataset.getString("id")).put("datasetSha256",WorldFrames.hash(dataset)).put("labelSha256",WorldFrames.hash(labelRecord)).put("runSha256",WorldFrames.hash(run))
            .put("trackerPolicy",run.optString("trackerPolicy","legacy-flow")).put("originalTrackerCodeSha256",run.getString("codeSha256")).put("codeSha256",identity()).put("trackerReplayed",false)
            .put("scope","Original stored boxes are rechecked against every image/frame binding; source pixels independently recompute visual support. No retracking, no original metric rewrite, no physical contact certification, training or native actuation.")
            .put("scores",scores).put("summary",new JSONObject().put("labels",scores.length()).put("associated",associated).put("observableContacts",observable).put("contactAvailable",available).put("contactMissing",observable-available).put("withinLabelRadius",within).put("unobservableContacts",unobservable).put("unsupportedContacts",unsupported).put("boxAvailable",boxAvailable).put("boxMeanPx",boxAvailable==0?JSONObject.NULL:boxTotal/boxAvailable).put("contactMeanPx",available==0?JSONObject.NULL:total/available));
    }
    private static double error(JSONArray a,JSONArray b,JSONObject dataset){return Math.hypot((a.getDouble(0)-b.getDouble(0))*dataset.getInt("width"),(a.getDouble(1)-b.getDouble(1))*dataset.getInt("height"));}
}
