package fraymus.workspace;

import org.json.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;

/** Durable observed-state revisions attached to existing035 world/body identities.
 * Observation evidence never masquerades as a native verified physical frame. */
final class WorldObservationStore implements AutoCloseable {
    static final String VERSION="persistent-world-observations-036-v1";
    private final Path root,file,lock;
    private final WorldLedger worlds;
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"world-observations");t.setDaemon(true);return t;});
    private volatile boolean closed;
    private JSONObject staging;
    private final JSONArray staged=new JSONArray();
    private long stagedBytes;
    // Cache authority is exact current byte identity, never file size/time. Read callbacks return detached values.
    private String verifiedBytes136;
    private JSONObject verifiedState136;
    private long reads136,bytes136,reused136,parsed136,validated136,writes136,readNs136,parseNs136,validateNs136,writeNs136;
    synchronized JSONObject verificationCost136(){return new JSONObject().put("stateReads",reads136).put("stateBytesHashed",bytes136).put("exactByteReuses",reused136).put("parsedStates",parsed136).put("structuralValidations",validated136).put("durableWrites",writes136).put("readHashMs",readNs136/1e6).put("parseMs",parseNs136/1e6).put("validationMs",validateNs136/1e6).put("writeMs",writeNs136/1e6).put("scope","Current process only. Every state read hashes all bytes; only exact byte identity reuses validated structure. Source images and outcome audits retain their own checks.");}
    private void validate136(JSONObject data){long start=System.nanoTime();try{validate(data);}finally{validated136++;validateNs136+=System.nanoTime()-start;}}

    WorldObservationStore(Path project,WorldLedger worlds) throws Exception {
        this.worlds=worlds;root=project.resolve("world-observations");file=root.resolve("state.json");lock=root.resolve("state.lock");
        access(true,data->{for(Object raw:data.getJSONArray("runs")){var r=(JSONObject)raw;if(r.getString("status").equals("running")){r.put("status","interrupted").put("reason","Process ended; explicitly resume this same run and exact prefix");}}return null;});
    }
    private interface Tx<T>{T apply(JSONObject state)throws Exception;}
    private synchronized <T>T access(boolean write,Tx<T> tx)throws Exception{
        Files.createDirectories(root);
        try(var channel=FileChannel.open(lock,StandardOpenOption.CREATE,StandardOpenOption.WRITE);var held=channel.lock()){
            JSONObject data;String rawHash=null;
            if(Files.exists(file)){
                long start=System.nanoTime();if(Files.size(file)>24_000_000)throw new IllegalStateException("Observation history exceeds24MB; preserve and export it");
                byte[] raw=Files.readAllBytes(file);if(raw.length>24_000_000)throw new IllegalStateException("Observation history exceeds24MB; preserve and export it");
                rawHash=PerceptionStore.hash(raw);reads136++;bytes136+=raw.length;readNs136+=System.nanoTime()-start;
                if(verifiedState136!=null&&rawHash.equals(verifiedBytes136)){reused136++;data=write?WorldFrames.copy(verifiedState136):verifiedState136;}
                else{
                    start=System.nanoTime();var e=new JSONObject(new String(raw,StandardCharsets.UTF_8));data=e.getJSONObject("payload");parsed136++;parseNs136+=System.nanoTime()-start;
                    if(!e.getString("version").equals(VERSION)||!e.getString("sha256").equals(WorldFrames.hash(data)))throw new IllegalStateException("Observation store integrity changed; original bytes preserved");
                    validate136(data);
                    if(!write){verifiedState136=data;verifiedBytes136=rawHash;}
                }
            }else {verifiedState136=null;verifiedBytes136=null;data=new JSONObject().put("datasets",new JSONArray()).put("runs",new JSONArray()).put("worlds",new JSONObject());validate136(data);}
            T result=tx.apply(data);
            if(write){
                validate136(data);long start=System.nanoTime();byte[] bytes=new JSONObject().put("version",VERSION).put("payload",data).put("sha256",WorldFrames.hash(data)).toString().getBytes(StandardCharsets.UTF_8);
                if(bytes.length>24_000_000)throw new IllegalStateException("Observation history capacity reached; no pruning");
                long parseStart=System.nanoTime();var detached=new JSONObject(new String(bytes,StandardCharsets.UTF_8));parsed136++;parseNs136+=System.nanoTime()-parseStart;
                var stored=detached.getJSONObject("payload");if(!WorldFrames.hash(stored).equals(detached.getString("sha256")))throw new IllegalStateException("Serialized observation integrity changed");
                var pending=file.resolveSibling("state.pending.json");try(var out=FileChannel.open(pending,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE)){var b=ByteBuffer.wrap(bytes);while(b.hasRemaining())out.write(b);out.force(true);}
                Files.move(pending,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
                verifiedBytes136=PerceptionStore.hash(bytes);verifiedState136=stored;writes136++;writeNs136+=System.nanoTime()-start;
            }
            return result;
        }
    }
    static String identity(){return ObservationMethods129.identity(VERSION); }
    private static void validate(JSONObject data){
        if(data.getJSONArray("datasets").length()>16||data.getJSONArray("runs").length()>24)throw new IllegalStateException("Observation dataset/run capacity reached");
        for(String id:data.getJSONObject("worlds").keySet()){
            UUID.fromString(id);var w=data.getJSONObject("worlds").getJSONObject(id);String parent="";int ordinal=0;
            for(Object raw:w.getJSONArray("history")){var h=(JSONObject)raw;var unsigned=WorldFrames.copy(h);unsigned.remove("hash");if(h.getInt("revision")!=++ordinal||!h.getString("parentHash").equals(parent)||!h.getString("hash").equals(WorldFrames.hash(unsigned)))throw new IllegalStateException("Observation revision chain changed");parent=h.getString("hash");}
            if(ordinal!=w.getInt("revision")||ordinal>2048)throw new IllegalStateException("Observation history/revision bound changed");
        }
    }
    private static JSONObject find(JSONArray values,String id){UUID.fromString(id);for(Object raw:values){var v=(JSONObject)raw;if(v.getString("id").equals(id))return v;}throw new IllegalArgumentException("Unknown observation record");}
    private static JSONObject summary(JSONObject r){var o=new JSONObject();for(String key:r.keySet())if(!key.equals("trace"))o.put(key,r.get(key));return WorldFrames.copy(o);}
    synchronized boolean busy()throws Exception{return access(false,s->{for(Object raw:s.getJSONArray("runs"))if(((JSONObject)raw).getString("status").equals("running"))return true;return false;});}
    JSONObject state(String worldId)throws Exception{
        var nativeState=worldId.isEmpty()?null:worlds.observe(worldId);
        return access(false,s->{var datasets=new JSONArray();for(Object raw:s.getJSONArray("datasets")){var d=WorldFrames.copy((JSONObject)raw);d.put("frameCount",d.getJSONArray("frames").length());d.remove("frames");datasets.put(d);}var runs=new JSONArray();for(Object raw:s.getJSONArray("runs"))runs.put(summary((JSONObject)raw));var result=new JSONObject().put("datasets",datasets).put("runs",runs).put("codeSha256",identity()).put("scope","Observed image-plane state on the same saved world IDs; native verified simulation remains separate");if(nativeState!=null){var w=s.getJSONObject("worlds").optJSONObject(worldId);result.put("estimate",w==null?new JSONObject().put("worldId",worldId).put("revision",0).put("tracks",new JSONObject()).put("history",new JSONArray()):WorldFrames.copy(w));result.put("nativeBinding",new JSONObject().put("worldId",worldId).put("expectedRevision",nativeState.getInt("expectedRevision")).put("expectedHash",nativeState.getString("expectedHash")));boolean stale=w!=null&&!bindingMatches(w.getJSONObject("nativeBinding"),nativeState);if(w!=null)for(String body:w.getJSONObject("tracks").keySet()){var track=w.getJSONObject("tracks").getJSONObject(body);if(!bindingMatches(track.getJSONObject("nativeBinding"),nativeState))stale=true;}result.put("stale",stale);}return result;});
    }
    JSONObject read(String id)throws Exception{return access(false,s->WorldFrames.copy(find(s.getJSONArray("runs"),id)));}
    JSONObject dataset(String id)throws Exception{return access(false,s->WorldFrames.copy(find(s.getJSONArray("datasets"),id)));}
    static BufferedImage decode(byte[] bytes)throws Exception{
        if(bytes.length<12||bytes.length>1_500_000)throw new IllegalArgumentException("Choose a JPEG/PNG frame up to1.5MB");
        try(var stream=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))){var readers=ImageIO.getImageReaders(stream);if(!readers.hasNext())throw new IllegalArgumentException("Unsupported image frame");var reader=readers.next();try{reader.setInput(stream);String type=reader.getFormatName();if(!type.equalsIgnoreCase("JPEG")&&!type.equalsIgnoreCase("PNG"))throw new IllegalArgumentException("Only JPEG/PNG pixels accepted");int w=reader.getWidth(0),h=reader.getHeight(0);if(w<32||h<32||w>1920||h>1080)throw new IllegalArgumentException("Frames require32..1920 by32..1080pixels");return reader.read(0);}finally{reader.dispose();}}
    }
    synchronized JSONObject begin(JSONObject input)throws Exception{
        SpatialScene.keys(input,"schema","title","source","license","clock","objects","frameCount");
        if(!input.getString("schema").equals("fraynix-observation-frames-036-v1")||input.getString("title").isBlank()||input.getString("title").length()>120||input.getString("source").isBlank()||input.getString("source").length()>1500||input.getString("license").isBlank()||input.getString("license").length()>1000)throw new IllegalArgumentException("Declare schema, title, source and license");
        if(!Set.of("source-frame","sample-index").contains(input.optString("clock","source-frame")))throw new IllegalArgumentException("Declare source-frame or sample-index clock");
        int n=input.getInt("frameCount");if(n<5||n>96||input.getDouble("frameCount")!=n)throw new IllegalArgumentException("Choose5..96 complete frames");
        if(staging!=null)throw new IllegalStateException("Finish or cancel the pending import");
        var keys=new HashSet<String>();var objects=input.getJSONArray("objects");if(objects.isEmpty()||objects.length()>4)throw new IllegalArgumentException("Choose1..4 initial object rectangles");
        for(Object raw:objects){var o=(JSONObject)raw;SpatialScene.keys(o,"key","label","box");if(!o.getString("key").matches("[a-zA-Z0-9_-]{1,40}")||!keys.add(o.getString("key"))||o.getString("label").length()>80)throw new IllegalArgumentException("Unique short object keys required");PixelTracker.Box.from(o.getJSONArray("box"));}
        if(access(false,s->s.getJSONArray("datasets").length())>=16)throw new IllegalStateException("Observation dataset capacity16 reached");
        staging=WorldFrames.copy(input).put("clock",input.optString("clock","source-frame")).put("id",UUID.randomUUID().toString());while(!staged.isEmpty())staged.remove(0);stagedBytes=0;return new JSONObject().put("id",staging.getString("id")).put("next",0);
    }
    synchronized JSONObject upload(String id,int ordinal,JSONObject frame)throws Exception{
        if(staging==null||!staging.getString("id").equals(id)||ordinal!=staged.length()||ordinal>=staging.getInt("frameCount"))throw new IllegalStateException("Observation import owner/order changed");
        SpatialScene.keys(frame,"sourceFrame","image","imageSha256");int t=frame.getInt("sourceFrame");if(t<0||t>1_000_000||frame.getDouble("sourceFrame")!=t||ordinal>0&&t<=staged.getJSONObject(ordinal-1).getInt("sourceFrame"))throw new IllegalArgumentException("Strictly increasing source frame ordinals required");
        byte[] bytes=Base64.getDecoder().decode(frame.getString("image"));if(!PerceptionStore.hash(bytes).equals(frame.getString("imageSha256")))throw new IllegalStateException("Observation source checksum differs");var image=decode(bytes);if(ordinal>0&&(image.getWidth()!=staging.getInt("width")||image.getHeight()!=staging.getInt("height")))throw new IllegalArgumentException("Frame dimensions changed");if(stagedBytes+bytes.length>32_000_000)throw new IllegalArgumentException("Frame package exceeds32MB");
        if(ordinal==0)staging.put("width",image.getWidth()).put("height",image.getHeight());Path folder=root.resolve("datasets").resolve(id);Files.createDirectories(folder);Files.write(folder.resolve(ordinal+".image"),bytes,StandardOpenOption.CREATE_NEW);var ref=WorldFrames.copy(frame);ref.remove("image");staged.put(ref);stagedBytes+=bytes.length;return new JSONObject().put("next",staged.length());
    }
    synchronized JSONObject commitImport(String id)throws Exception{
        if(staging==null||!staging.getString("id").equals(id)||staged.length()!=staging.getInt("frameCount"))throw new IllegalStateException("Observation import is incomplete");var d=WorldFrames.copy(staging);d.remove("frameCount");d.put("frames",new JSONArray(staged.toString())).put("importedAt",Instant.now()).put("contentSha256",WorldFrames.hash(new JSONObject().put("objects",staging.getJSONArray("objects")).put("frames",staged)));access(true,s->{s.getJSONArray("datasets").put(d);return null;});staging=null;while(!staged.isEmpty())staged.remove(0);return WorldFrames.copy(d);
    }
    synchronized JSONObject cancelImport(String id){if(staging==null||!staging.getString("id").equals(id))throw new IllegalStateException("Unknown observation import");staging=null;while(!staged.isEmpty())staged.remove(0);return new JSONObject().put("cancelled",true);}
    byte[] image(String id,int ordinal)throws Exception{var d=dataset(id);if(ordinal<0||ordinal>=d.getJSONArray("frames").length())throw new IllegalArgumentException("Unknown observation frame");byte[] b=Files.readAllBytes(root.resolve("datasets").resolve(id).resolve(ordinal+".image"));if(!PerceptionStore.hash(b).equals(d.getJSONArray("frames").getJSONObject(ordinal).getString("imageSha256")))throw new IllegalStateException("Original observation image integrity changed");return b;}
    private static boolean bindingMatches(JSONObject binding,JSONObject observation){return binding.getInt("expectedRevision")==observation.getInt("expectedRevision")&&binding.getString("expectedHash").equals(observation.getString("expectedHash"));}
    private void current(JSONObject binding)throws Exception{var o=worlds.observe(binding.getString("worldId"));if(!bindingMatches(binding,o)||o.has("pending"))throw new IllegalStateException("Stale or owned native world; observe and explicitly bind a new run");}
    JSONObject start(JSONObject input)throws Exception{
        SpatialScene.keys(input,"worldId","expectedRevision","expectedHash","expectedObservationRevision","datasetId","bindings","trackerPolicy");String trackerPolicy=ObservationMethods129.policy(input);var b=new JSONObject();for(String k:List.of("worldId","expectedRevision","expectedHash"))b.put(k,input.get(k));current(b);var d=dataset(input.getString("datasetId"));var nativeState=worlds.observe(b.getString("worldId"));var ids=new HashSet<String>();var keys=new HashSet<String>();for(Object raw:input.getJSONArray("bindings")){var v=(JSONObject)raw;SpatialScene.keys(v,"objectKey","bodyId");if(!ids.add(v.getString("bodyId"))||!keys.add(v.getString("objectKey")))throw new IllegalArgumentException("One distinct world body per image object");boolean found=false;for(Object o:d.getJSONArray("objects"))if(((JSONObject)o).getString("key").equals(v.getString("objectKey")))found=true;if(!found)throw new IllegalArgumentException("Unknown image object key");ContactPhysics.dynamic(nativeState.getJSONObject("frame").getJSONArray("bodies"),v.getString("bodyId"));}
        if(keys.isEmpty()||keys.size()!=d.getJSONArray("objects").length())throw new IllegalArgumentException("Bind every initial image rectangle to a stable world body");
        var r=access(true,s->{for(Object raw:s.getJSONArray("runs")){var old=(JSONObject)raw;if(Set.of("running","interrupted").contains(old.getString("status")))throw new IllegalStateException("Resume or Stop the owned observation run first");}var w=s.getJSONObject("worlds").optJSONObject(b.getString("worldId"));int rev=w==null?0:w.getInt("revision");if(input.getInt("expectedObservationRevision")!=rev)throw new ConcurrentModificationException("Stale observation revision");if(s.getJSONArray("runs").length()>=24)throw new IllegalStateException("Observation run capacity24 reached");var run=new JSONObject().put("id",UUID.randomUUID().toString()).put("datasetId",d.getString("id")).put("nativeBinding",b).put("bindings",input.getJSONArray("bindings")).put("trackerPolicy",trackerPolicy).put("status","running").put("reason","Tracking original pixels and updating world estimates").put("codeSha256",identity()).put("createdAt",Instant.now()).put("trace",new JSONArray()).put("completedFrames",0).put("totalFrames",d.getJSONArray("frames").length()-1).put("resumes",0).put("baseObservationRevision",rev);s.getJSONArray("runs").put(run);return WorldFrames.copy(run);});worker.submit(()->execute(r.getString("id"),r.getInt("resumes")));return summary(r);
    }
    JSONObject stop(String id)throws Exception{return access(true,s->{var r=find(s.getJSONArray("runs"),id);if(!Set.of("running","interrupted").contains(r.getString("status")))throw new IllegalStateException("Observation run already settled");r.put("status","cancelled").put("reason","Stopped explicitly; prior estimates and evidence retained");return summary(r);});}
    JSONObject resume(String id)throws Exception{
        var old=read(id);current(old.getJSONObject("nativeBinding"));if(!old.getString("codeSha256").equals(identity()))throw new IllegalStateException("Observation implementation changed; preserve original run");
        var r=access(true,s->{var run=find(s.getJSONArray("runs"),id);if(!Set.of("interrupted","cancelled").contains(run.getString("status")))throw new IllegalStateException("Only an interrupted/stopped run can resume");for(Object raw:s.getJSONArray("runs"))if(((JSONObject)raw).getString("status").equals("running"))throw new IllegalStateException("Another observation run is active");run.put("status","running").put("reason","Replaying exact tracker prefix before continuing same run").put("resumes",run.getInt("resumes")+1);return summary(run);});worker.submit(()->execute(id,r.getInt("resumes")));return r;
    }
    private void execute(String id,int generation){try{
        var run=read(id);var d=dataset(run.getString("datasetId"));var frames=d.getJSONArray("frames");var first=decode(image(d.getString("id"),0));var trackers=new LinkedHashMap<String,ImageTracker>();var filters=new LinkedHashMap<String,ObservationFilter>();
        for(Object raw:run.getJSONArray("bindings")){var binding=(JSONObject)raw;JSONObject object=null;for(Object o:d.getJSONArray("objects"))if(((JSONObject)o).getString("key").equals(binding.getString("objectKey")))object=(JSONObject)o;var box=PixelTracker.Box.from(object.getJSONArray("box"));String body=binding.getString("bodyId");trackers.put(body,ObservationMethods129.create(ObservationMethods129.policy(run),body,object.getString("label"),first,box));filters.put(body,new ObservationFilter(frames.getJSONObject(0).getInt("sourceFrame"),box));}
        for(int i=1;i<frames.length();i++){
            var live=read(id);if(closed||!live.getString("status").equals("running")||live.getInt("resumes")!=generation)return;
            current(run.getJSONObject("nativeBinding"));var pixels=decode(image(d.getString("id"),i));var f=frames.getJSONObject(i);var rows=new JSONArray();
            for(String body:trackers.keySet()){var tracked=trackers.get(body).update(pixels);var estimate=filters.get(body).step(f.getInt("sourceFrame"),tracked).put("timeUnit",d.getString("clock"));rows.put(new JSONObject().put("bodyId",body).put("tracker",tracked).put("estimate",estimate));}
            var step=new JSONObject().put("ordinal",i).put("sourceFrame",f.getInt("sourceFrame")).put("imageSha256",f.getString("imageSha256")).put("objects",rows);
            if(i<=run.getJSONArray("trace").length()){if(!step.similar(run.getJSONArray("trace").getJSONObject(i-1)))throw new IllegalStateException("Reconstructed observation prefix differs; original bytes retained");continue;}
            int ordinal=i;
            access(true,s->{var r=find(s.getJSONArray("runs"),id);if(closed||!r.getString("status").equals("running")||r.getInt("resumes")!=generation)return null;if(r.getInt("completedFrames")!=ordinal-1)throw new IllegalStateException("Observation prefix owner changed");current(r.getJSONObject("nativeBinding"));var all=s.getJSONObject("worlds");String worldId=r.getJSONObject("nativeBinding").getString("worldId");var w=all.optJSONObject(worldId);if(w==null){w=new JSONObject().put("worldId",worldId).put("revision",0).put("tracks",new JSONObject()).put("history",new JSONArray()).put("nativeBinding",r.getJSONObject("nativeBinding"));all.put(worldId,w);}var updates=new JSONObject();for(Object raw:rows){var row=(JSONObject)raw;var estimate=row.getJSONObject("estimate");if(estimate.getBoolean("accepted"))updates.put(row.getString("bodyId"),WorldFrames.copy(estimate).put("datasetId",d.getString("id")).put("sourceFrame",f.getInt("sourceFrame")).put("imageSha256",f.getString("imageSha256")).put("runId",id).put("nativeBinding",r.getJSONObject("nativeBinding")));}
                if(!updates.isEmpty()){
                    if(w.getInt("revision")>=2048)throw new IllegalStateException("Observation revision capacity2048 reached; preserve/export history");
                    var history=w.getJSONArray("history");var h=new JSONObject().put("revision",w.getInt("revision")+1).put("parentHash",history.isEmpty()?"":history.getJSONObject(history.length()-1).getString("hash")).put("runId",id).put("ordinal",ordinal).put("sourceFrame",f.getInt("sourceFrame")).put("nativeBinding",r.getJSONObject("nativeBinding")).put("updates",updates);h.put("hash",WorldFrames.hash(h));history.put(h);w.put("revision",h.getInt("revision")).put("nativeBinding",r.getJSONObject("nativeBinding"));for(String body:updates.keySet())w.getJSONObject("tracks").put(body,updates.get(body));
                }
                r.getJSONArray("trace").put(step);r.put("completedFrames",ordinal).put("observationRevision",w.getInt("revision"));if(ordinal==frames.length()-1)r.put("status","completed").put("completedAt",Instant.now()).put("reason","All source frames processed; accepted estimates retained with full evidence");return null;});
        }
    }catch(Exception e){try{access(true,s->{var r=find(s.getJSONArray("runs"),id);if(r.getString("status").equals("running")&&r.getInt("resumes")==generation)r.put("status","interrupted").put("reason",String.valueOf(e.getMessage()));return null;});}catch(Exception ignored){System.err.println("Observation persistence failure: "+e.getMessage());}}}
    JSONObject frame(String id,int ordinal)throws Exception{var r=read(id);var d=dataset(r.getString("datasetId"));var out=new JSONObject().put("ordinal",ordinal).put("source",d.getJSONArray("frames").getJSONObject(ordinal)).put("width",d.getInt("width")).put("height",d.getInt("height")).put("image",Base64.getEncoder().encodeToString(image(d.getString("id"),ordinal)));if(ordinal>0&&ordinal<=r.getJSONArray("trace").length())out.put("objects",r.getJSONArray("trace").getJSONObject(ordinal-1).getJSONArray("objects"));else out.put("objects",new JSONArray());return out;}
    JSONObject export(String id)throws Exception{
        // One atomic observation snapshot, including failures/cancellations with no accepted update.
        var snapshot=access(false,s->WorldFrames.copy(s));var selected=find(snapshot.getJSONArray("runs"),id);
        String worldId=selected.getJSONObject("nativeBinding").getString("worldId");
        var nativeWorld=worlds.read(worldId);var nativeBinding=WorldLedger.binding(nativeWorld);
        var observed=snapshot.getJSONObject("worlds").optJSONObject(worldId);
        if(observed==null)throw new IllegalStateException("No retained observed world yet");
        var relatedRuns=new JSONArray();var datasetIds=new LinkedHashSet<String>();
        datasetIds.add(selected.getString("datasetId"));
        for(Object raw:snapshot.getJSONArray("runs")){var r=(JSONObject)raw;if(r.getJSONObject("nativeBinding").getString("worldId").equals(worldId)){datasetIds.add(r.getString("datasetId"));if(!r.getString("id").equals(id))relatedRuns.put(r);}}
        var datasetPackages=new JSONArray();long sourceBytes=0;
        for(String datasetId:datasetIds){var d=find(snapshot.getJSONArray("datasets"),datasetId);var images=new JSONArray();for(int i=0;i<d.getJSONArray("frames").length();i++){byte[] bytes=image(datasetId,i);sourceBytes+=bytes.length;if(sourceBytes>40_000_000)throw new IllegalStateException("Complete observation world exceeds40MB original pixels/64MB raw export; preserve originals and archive this project without pruning");images.put(Base64.getEncoder().encodeToString(bytes));}datasetPackages.put(new JSONObject().put("dataset",d).put("images",images));}
        var primary=datasetPackages.getJSONObject(0);datasetPackages.remove(0);
        var world=new JSONObject().put("estimate",observed).put("nativeBinding",nativeBinding).put("stale",!bindingMatches(observed.getJSONObject("nativeBinding"),nativeBinding));
        var value=new JSONObject().put("format","fraynix-observed-world-036-v1").put("run",selected).put("dataset",primary.getJSONObject("dataset")).put("images",primary.getJSONArray("images")).put("relatedRuns",relatedRuns).put("relatedDatasets",datasetPackages).put("world",world).put("nativeWorld",nativeWorld).put("scope","Complete world observation history, every related run including failure/cancellation and all original pixels. Native verified simulation and image-plane estimates are separate; no metric calibration implied");
        if(!WorldLedger.binding(worlds.read(worldId)).similar(nativeBinding))throw new ConcurrentModificationException("Native world changed during export; retry from the current head");
        byte[] raw=value.toString().getBytes(StandardCharsets.UTF_8);if(raw.length>64_000_000)throw new IllegalStateException("Complete observation export exceeds64MB raw; original evidence preserved");
        try(var bytes=new ByteArrayOutputStream()){try(var gzip=new java.util.zip.GZIPOutputStream(bytes)){gzip.write(raw);}return new JSONObject().put("format","fraynix-observed-world-gzip-036-v1").put("rawBytes",raw.length).put("rawSha256",PerceptionStore.hash(raw)).put("data",Base64.getEncoder().encodeToString(bytes.toByteArray()));}
    }
    @Override public synchronized void close(){closed=true;worker.shutdownNow();}
}
