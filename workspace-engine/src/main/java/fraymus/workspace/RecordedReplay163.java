package fraymus.workspace;

import org.json.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Only identical, freshly checked source bytes may reuse a completed reconstruction.
 * No native, current-world, measurement-gate or rendered-outcome check is memoized here. */
final class RecordedReplay163 {
    static final int CAPACITY=4, MAX_ENTRY=2_000_000;
    static final long MAX_BYTES=128_000_000;
    private final Path project;
    private final VerifiedComputationCache metadata = new VerifiedComputationCache();
    private final Alignment041Store alignment;
    private final CalibratedPlane159 planes;
    private final PlaneReplay160 replay;
    private final boolean enabled;
    private final LinkedHashMap<String,Entry> entries=new LinkedHashMap<>(8,.75f,true);
    private record Entry(String fingerprint,String result) {}
    private record Snapshot(String fingerprint,long bytes,int files) {}
    private long calls,hits,replays,failures,evictions,readBytes,readFiles,hashNs,replayNs,totalNs;
    private JSONObject last=new JSONObject();

    RecordedReplay163(Path project,Alignment041Store alignment,WorldLedger worlds,PlaneReplay160 replay){
        this(project,alignment,worlds,replay,!"full".equals(System.getenv("FRAYNIX_WORKSPACE_RECORDED_REPLAY")));
    }
    RecordedReplay163(Path project,Alignment041Store alignment,WorldLedger worlds,PlaneReplay160 replay,boolean enabled){
        this.project=project;this.alignment=alignment;this.planes=new CalibratedPlane159(project,alignment,worlds);this.replay=replay;this.enabled=enabled;
    }

    synchronized JSONObject read(JSONObject binding,boolean force,Runnable checkpoint)throws Exception {
        long begin=System.nanoTime();calls++;String id=binding.getString("trialId");boolean hit=false;long byteStart=readBytes,fileStart=readFiles;
        try {
            checkpoint.run();Snapshot before=snapshot(binding);checkpoint.run();
            Entry entry=entries.get(id);hit=enabled&&!force&&entry!=null&&entry.fingerprint.equals(before.fingerprint);
            if(!hit)entries.remove(id);
            JSONObject result;
            if(hit){result=new JSONObject(entry.result);}
            else{long start=System.nanoTime();replays++;try{result=replay.replay(binding,checkpoint);}finally{replayNs+=System.nanoTime()-start;}}
            checkpoint.run();Snapshot after=snapshot(binding);checkpoint.run();
            if(!before.fingerprint.equals(after.fingerprint))throw new IllegalStateException("Recorded source or implementation changed during verification");
            if(hit)hits++;
            String serialized=result.toString();
            if(enabled&&serialized.getBytes(StandardCharsets.UTF_8).length<=MAX_ENTRY){
                entries.put(id,new Entry(after.fingerprint,serialized));
                while(entries.size()>CAPACITY){entries.remove(entries.keySet().iterator().next());evictions++;}
            }
            last=new JSONObject().put("passed",true).put("trialId",id).put("reused",hit).put("forced",force).put("fingerprint",after.fingerprint);
            return result;
        }catch(Exception e){entries.clear();metadata.clear();failures++;last=new JSONObject().put("passed",false).put("trialId",id).put("reason",String.valueOf(e.getMessage()));throw e;}
        finally{long elapsed=System.nanoTime()-begin;totalNs+=elapsed;last.put("totalMs",elapsed/1e6).put("checkedFrameAndCodeBytes",readBytes-byteStart).put("checkedFrameAndCodeFiles",readFiles-fileStart);}
    }

    synchronized JSONObject status(){return new JSONObject().put("mode",enabled?"verified-memo":"full-replay").put("sourceReuse",metadata.status()).put("calls",calls).put("reused",hits).put("fullReplays",replays).put("failures",failures).put("evictions",evictions).put("checkedFrameAndCodeBytes",readBytes).put("checkedFrameAndCodeFiles",readFiles).put("sourceCheckMs",hashNs/1e6).put("reconstructionMs",replayNs/1e6).put("totalMs",totalNs/1e6).put("entries",entries.size()).put("capacity",CAPACITY).put("maxEntryBytes",MAX_ENTRY).put("last",WorldFrames.copy(last)).put("scope","Process-local recording reconstruction only. Every use rereads original image, metadata, labels, calibration, historical-world and code evidence before and after. Cold or changed evidence reconstructs. Native and outcome checks are never memoized here.");}

    private Snapshot snapshot(JSONObject frozen)throws Exception {
        long begin=System.nanoTime();
        try {
            // Older stores initialize missing state. Refuse before calling either accessor.
            for(String state:List.of("alignment-041/state.json","persistent-worlds/state.json"))regular(project.resolve(state));
            var checked=metadata(frozen);
            var binding=checked.getJSONObject("binding");
            var trial=checked.getJSONObject("trial");var source=checked.getJSONObject("source");
            var hash=MessageDigest.getInstance("SHA-256");
            hash.update(WorldFrames.hash(new JSONObject().put("binding",binding).put("trial",trial).put("source",source)).getBytes(StandardCharsets.UTF_8));
            hash.update((System.getProperty("java.runtime.version")+"\n"+System.getProperty("java.vm.name")).getBytes(StandardCharsets.UTF_8));
            var leaves=new TreeMap<String,Path>();
            addFrame(leaves,trial.getString("registrationId"),0,source.getJSONObject("registration").getJSONObject("frame"));
            addFrame(leaves,trial.getString("id"),0,trial.getJSONObject("seed"));
            var trace=trial.getJSONArray("trace");if(trace.length()>300)throw new IllegalStateException("Original frame limit exceeded");
            for(int i=0;i<trace.length();i++)addFrame(leaves,trial.getString("id"),i+1,trace.getJSONObject(i).getJSONObject("frame"));
            var origins=new LinkedHashSet<Path>();for(Class<?> type:List.of(RecordedReplay163.class,JSONObject.class))origins.add(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()));
            int n=0;for(Path origin:origins){String prefix="code"+(n++);ReplayEvidence157.unlinkedRealPath(origin);if(Files.isDirectory(origin)){try(var stream=Files.walk(origin)){for(Path p:stream.toList()){ReplayEvidence157.unlinkedRealPath(p);if(Files.isDirectory(p,LinkOption.NOFOLLOW_LINKS))continue;regular(p);if(!p.toString().endsWith(".class"))continue;leaves.put(prefix+"/"+origin.relativize(p).toString().replace('\\','/'),p);}}}else{regular(origin);leaves.put(prefix,origin);}}
            if(leaves.size()>8192)throw new IllegalStateException("Recorded implementation inventory limit exceeded");
            long bytes=0;byte[] buffer=new byte[65536];
            for(var leaf:leaves.entrySet()){
                Path p=leaf.getValue();regular(p);long size=Files.size(p);if(size>MAX_BYTES-bytes)throw new IllegalStateException("Recorded source fingerprint byte limit exceeded");
                hash.update((leaf.getKey()+"\0"+size+"\0").getBytes(StandardCharsets.UTF_8));long count=0;
                try(var in=Files.newInputStream(p)){int len;while((len=in.read(buffer))!=-1){count+=len;bytes+=len;readBytes+=len;if(bytes>MAX_BYTES)throw new IllegalStateException("Recorded source byte limit exceeded");hash.update(buffer,0,len);}}
                readFiles++;if(count!=size)throw new IllegalStateException("Recorded source size changed during verification");
            }
            if(!metadata(frozen).getJSONObject("binding").similar(frozen))throw new IllegalStateException("Recorded source changed during byte read");
            return new Snapshot(HexFormat.of().formatHex(hash.digest()),bytes,leaves.size());
        }finally{hashNs+=System.nanoTime()-begin;}
    }


    private JSONObject metadata(JSONObject frozen) throws Exception {
        String registration = frozen.getString("registrationId");
        UUID.fromString(registration);
        return metadata.read(WorldFrames.hash(frozen), () -> new EvidenceFingerprint()
                .file(project.resolve("alignment-041/state.json"), 32_000_000)
                .file(project.resolve("persistent-worlds/state.json"), 16_000_000)
                .file(project.resolve("alignment-041/frames").resolve(registration).resolve("0.image"), 1_000_000)
                .file(project.resolve("alignment-041/frames").resolve(registration).resolve("0.meta.json"), 20_000)
                .code(RecordedReplay163.class, JSONObject.class).finish(), () -> {
            var binding = replay.binding(frozen.getString("trialId"));
            if (!binding.similar(frozen)) {
                throw new IllegalStateException("Frozen trial or independent labels changed");
            }
            var trial = alignment.trial(frozen.getString("trialId"));
            var source = planes.source(trial.getString("registrationId"));
            return new JSONObject().put("binding", binding).put("trial", trial).put("source", source);
        });
    }

    private void addFrame(Map<String,Path> leaves,String id,int ordinal,JSONObject ref)throws Exception{
        UUID.fromString(id);Path image=project.resolve("alignment-041/frames").resolve(id).resolve(ordinal+".image"),meta=image.resolveSibling(ordinal+".meta.json");regular(image);regular(meta);
        if(Files.size(image)>1_000_000||Files.size(meta)>20_000)throw new IllegalStateException("Original frame budget exceeded");
        byte[] bytes=Files.readAllBytes(image),metadata=Files.readAllBytes(meta);readBytes+=bytes.length+metadata.length;readFiles+=2;
        if(bytes.length!=ref.getInt("bytes")||!PerceptionStore.hash(bytes).equals(ref.getString("imageSha256"))||!new JSONObject(new String(metadata,StandardCharsets.UTF_8)).similar(ref))throw new IllegalStateException("Original frame bytes or metadata changed");
        leaves.put("frames/"+id+"/"+ordinal+".image",image);leaves.put("frames/"+id+"/"+ordinal+".meta.json",meta);
    }
    private static void regular(Path p)throws Exception{ReplayEvidence157.unlinkedRealPath(p);if(!Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS))throw new IllegalStateException("Original regular evidence file missing");}
}
