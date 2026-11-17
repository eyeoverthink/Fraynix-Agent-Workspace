package fraymus.workspace;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

/** Explicit read-only research track, separately versioned so original proofs remain original. */
final class ContinuityExperiment157 implements AutoCloseable {
    private final Path root, project;
    private static final int MEMO_CAPACITY=8;
    private record Memo(String fingerprint,String serialized) {}
    private final java.util.LinkedHashMap<String,Memo> memo=new java.util.LinkedHashMap<>(16,.75f,true);
    private JSONObject cost=new JSONObject();
    private final WorldObservationStore observations;
    private final AssociationAudit134 association;
    private final ReplayEvidence157 replay;
    private final Object jobsLock=new Object();
    private final java.util.LinkedHashMap<String,JSONObject> jobs=new java.util.LinkedHashMap<>();
    private Thread worker;
    private String activeJob;
    ContinuityExperiment157(Path project,WorldObservationStore observations,AssociationAudit134 association,ReplayEvidence157 replay){this.project=project;root=project.resolve("continuity-experiments157");this.observations=observations;this.association=association;this.replay=replay;}
    private void remember(JSONObject value){String text=value.toString();if(text.getBytes(StandardCharsets.UTF_8).length<=4_000_000){memo.put(value.getString("id"),new Memo(value.getString("evidenceFingerprint"),text));while(memo.size()>MEMO_CAPACITY)memo.remove(memo.keySet().iterator().next());}}
    private void requireHistory(){if(!Files.isRegularFile(project.resolve("world-observations/state.json"),LinkOption.NOFOLLOW_LINKS)){memo.clear();throw new IllegalStateException("Original observation state is missing; no history recreated");}}
    private JSONObject result(JSONObject value,long started,boolean reused){return new JSONObject().put("experiment",value).put("totalMs",(System.nanoTime()-started)/1e6).put("replayed",!reused)
        .put("cost157",new JSONObject(cost.toString()).put("reusedReplay",reused).put("retainedEntries",memo.size()).put("capacity",MEMO_CAPACITY)
        .put("scope","Every request rechecks original evidence/code bytes. Only exact process-local completed results can be reused; cold start or explicit full replay reconstructs. No size/time cache or persistent trust."));}
    private java.awt.image.BufferedImage image(JSONObject dataset,int ordinal)throws Exception{

        java.util.UUID.fromString(dataset.getString("id"));
        Path file=project.resolve("world-observations/datasets").resolve(dataset.getString("id")).resolve(ordinal+".image");ReplayEvidence157.unlinkedRealPath(file);
        long size=Files.size(file);if(size<12||size>1_500_000)throw new IllegalStateException("Original source image size changed");
        byte[] bytes=Files.readAllBytes(file);if(!PerceptionStore.hash(bytes).equals(dataset.getJSONArray("frames").getJSONObject(ordinal).getString("imageSha256")))throw new IllegalStateException("Original image integrity changed");
        return WorldObservationStore.decode(bytes);
    }
    JSONObject start(String id,boolean verify){return start(id,verify,false);}
    JSONObject start(String id,boolean verify,boolean forceReplay){
        if(!id.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Invalid evidence identity");
        synchronized(jobsLock){
            if(worker!=null&&worker.isAlive())throw new IllegalStateException("A rotation calculation is already running");
            while(jobs.size()>=4)jobs.remove(jobs.keySet().iterator().next());
            String job=java.util.UUID.randomUUID().toString();activeJob=job;
            jobs.put(job,new JSONObject().put("jobId",job).put("status","running").put("phase","Checking original source and outcomes").put("completedFrames",0).put("totalFrames",0));
            worker=Thread.ofVirtual().name("continuity-experiment157").unstarted(()->{
                try{var result=verify?read(id,forceReplay):run(id);synchronized(jobsLock){jobs.get(job).put("status","completed").put("phase","All source and outcome checks completed").put("result",result);}}
                catch(Exception e){synchronized(jobsLock){jobs.get(job).put("status","failed").put("phase","No verified completion").put("error",e.getMessage()==null?e.getClass().getSimpleName():e.getMessage());}}
            });worker.start();return new JSONObject(jobs.get(job).toString());
        }
    }
    JSONObject status(String job){synchronized(jobsLock){var value=jobs.get(job);if(value==null)throw new IllegalArgumentException("Unknown or expired calculation; restart from original evidence");return new JSONObject(value.toString());}}
    private void progress(String phase,int completed,int total){synchronized(jobsLock){if(Thread.currentThread()==worker&&activeJob!=null)jobs.get(activeJob).put("phase",phase).put("completedFrames",completed).put("totalFrames",total);}}
    public void close(){synchronized(jobsLock){if(worker!=null)worker.interrupt();}}
    synchronized JSONObject run(String auditId)throws Exception{
        long started=System.nanoTime();var content=evaluate(auditId,false);if(Thread.currentThread().isInterrupted())throw new InterruptedException("Rotation calculation interrupted; no completion saved");String id=WorldFrames.hash(content);content.put("id",id);
        Files.createDirectories(root);ReplayEvidence157.unlinkedRealPath(root);
        Path file=root.resolve(id+".json");if(Files.exists(file)){if(!new JSONObject(Files.readString(file)).similar(content))throw new IllegalStateException("Saved rotation evidence differs");}
        else{
            try(var files=Files.list(root)){if(files.filter(p->p.toString().endsWith(".json")).count()>=64)throw new IllegalStateException("Rotation evidence capacity reached; originals preserved");}
            var temporary=Files.createTempFile(root,"rotation-",".pending");Files.writeString(temporary,content.toString(),StandardCharsets.UTF_8);Files.move(temporary,file,StandardCopyOption.ATOMIC_MOVE);
        }
        remember(content);return result(content,started,false);
    }
    synchronized JSONObject read(String id)throws Exception{return read(id,false);}
    synchronized JSONObject read(String id,boolean forceReplay)throws Exception{
        long start=System.nanoTime();requireHistory();
        if(!id.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Invalid continuity evidence identity");
        Path file=root.resolve(id+".json");ReplayEvidence157.unlinkedRealPath(file);
        if(Files.size(file)>8_000_000)throw new IllegalStateException("Continuity evidence exceeds bound");
        byte[] original=Files.readAllBytes(file);var saved=new JSONObject(new String(original,StandardCharsets.UTF_8));
        String embedded=saved.getString("id");saved.remove("id");if(!id.equals(embedded)||!id.equals(WorldFrames.hash(saved)))throw new IllegalStateException("Continuity evidence checksum changed");
        long hashStart=System.nanoTime();var before=replay.snapshot();long hashNs=System.nanoTime()-hashStart;
        var entry=memo.get(id);boolean reused=!forceReplay&&entry!=null&&entry.fingerprint.equals(before.hash());
        if(!reused)memo.remove(id);
        JSONObject actual;
        try{actual=reused?new JSONObject(entry.serialized):evaluate(saved.getString("auditId"),forceReplay);actual.remove("id");
            if(!actual.similar(saved))throw new IllegalStateException("Continuity source, implementation or outcome changed");
            hashStart=System.nanoTime();var after=replay.snapshot();hashNs+=System.nanoTime()-hashStart;
            ReplayEvidence157.unlinkedRealPath(file);requireHistory();
            if(!before.hash().equals(after.hash())||!java.util.Arrays.equals(original,Files.readAllBytes(file)))throw new IllegalStateException("Evidence changed during verification");
            if(reused)cost=new JSONObject().put("originalReplayMs",0).put("sourceSetupMs",0).put("searchMs",0).put("imageLoadMs",0).put("scoringMs",0);
            cost.put("openingHashMs",hashNs/1e6).put("openingBytesHashed",before.bytes()+after.bytes());
            actual.put("id",id);remember(actual);return result(actual,start,reused);
        }catch(Exception e){memo.remove(id);throw e;}
    }
    private JSONObject evaluate(String auditId,boolean forceReplay)throws Exception{
        requireHistory();cost=new JSONObject();long phase=System.nanoTime();var stateStart=observations.verificationCost136();
        var audit=replay.read(auditId,forceReplay).getJSONObject("audit");cost.put("originalReplayMs",(System.nanoTime()-phase)/1e6);phase=System.nanoTime();var before=replay.snapshot();
        var run=observations.read(audit.getJSONObject("input").getString("runId"));var dataset=association.verifiedDataset(run.getString("datasetId"));
        if(!WorldFrames.hash(run).equals(audit.getString("runSha256"))||!WorldFrames.hash(dataset).equals(audit.getString("datasetSha256")))throw new IllegalStateException("Original comparison changed");
        // The label record selects the source object only. Its returned boxes and visibility
        // cannot reach RotationReturn153: the complete trace is produced before scoring.
        String key=association.read(audit.getJSONObject("input").getString("labelId")).getJSONObject("input").getString("objectKey");
        JSONObject source=null;for(Object raw:dataset.getJSONArray("objects")){var o=(JSONObject)raw;if(o.getString("key").equals(key))source=o;}
        if(source==null)throw new IllegalStateException("Original source object missing");
        var memory=run.optJSONObject("referenceMemory137");if(memory==null)memory=new JSONObject().put("examples",new JSONArray());
        String body=null;for(Object raw:run.getJSONArray("bindings")){var binding=(JSONObject)raw;if(binding.getString("objectKey").equals(key))body=binding.getString("bodyId");}
        if(body==null)throw new IllegalStateException("Original binding missing");
        var first=image(dataset,0);var combine=new ContinuityReturn154(PixelTracker.Box.from(source.getJSONArray("box")));
        cost.put("sourceSetupMs",(System.nanoTime()-phase)/1e6);phase=System.nanoTime();
        progress("Checking the original reference and competing objects",0,dataset.getJSONArray("frames").length()-1);
        var tracker=new RotationReturn155(first,PixelTracker.Box.from(source.getJSONArray("box")),memory,RotationSearch155.Arithmetic.GRID);
        var trace=new JSONArray();long evaluations=0,imageNs=0;
        if(run.getJSONArray("trace").length()!=dataset.getJSONArray("frames").length()-1)throw new IllegalStateException("Complete original trace required");
        for(int i=1;i<dataset.getJSONArray("frames").length();i++){
            if(Thread.currentThread().isInterrupted())throw new InterruptedException("Rotation experiment interrupted; no completion saved");
            long load=System.nanoTime();var pixels=image(dataset,i);imageNs+=System.nanoTime()-load;
            var rotation=tracker.update(pixels);var originalFrame=run.getJSONArray("trace").getJSONObject(i-1);
            if(originalFrame.getInt("ordinal")!=i||!originalFrame.getString("imageSha256").equals(dataset.getJSONArray("frames").getJSONObject(i).getString("imageSha256")))throw new IllegalStateException("Original trace order changed");
            JSONObject originalObject=null;for(Object raw:originalFrame.getJSONArray("objects")){var object=(JSONObject)raw;if(object.getString("bodyId").equals(body)){if(originalObject!=null)throw new IllegalStateException("Duplicate original binding");originalObject=object;}}
            if(originalObject==null)throw new IllegalStateException("Original observation missing");
            var row=combine.update(originalObject,rotation);
            row.put("imageSha256",dataset.getJSONArray("frames").getJSONObject(i).getString("imageSha256"));trace.put(row);evaluations+=rotation.getLong("evaluations");
            progress("Searching original images; completion is not yet verified",i,dataset.getJSONArray("frames").length()-1);
        }
        cost.put("searchMs",(System.nanoTime()-phase-imageNs)/1e6).put("imageLoadMs",imageNs/1e6);phase=System.nanoTime();
        var scores=new JSONArray();int visible=0,correct=0,missing=0,wrong=0,hidden=0,unsupported=0,others=0;
        for(Object raw:audit.getJSONArray("scores")){
            var label=(JSONObject)raw;var found=trace.getJSONObject(label.getInt("ordinal")-1);boolean accepted=!found.isNull("box");String visibility=label.getString("visibility");
            double overlap=0,other=0;if(accepted){var box=PixelTracker.Box.from(found.getJSONArray("box"));if(label.has("targetBox")&&!label.isNull("targetBox"))overlap=PixelTracker.iou(box,PixelTracker.Box.from(label.getJSONArray("targetBox")));for(Object o:label.optJSONArray("otherBoxes")==null?new JSONArray():label.getJSONArray("otherBoxes"))other=Math.max(other,PixelTracker.iou(box,PixelTracker.Box.from((JSONArray)o)));}
            boolean good=accepted&&overlap>=.5;if(visibility.equals("visible")||visibility.equals("partial")){visible++;if(!accepted)missing++;else if(good)correct++;else wrong++;}
            if(visibility.equals("occluded")||visibility.equals("out-of-view")){hidden++;if(accepted)unsupported++;}
            if(accepted&&other>=.5)others++;
            scores.put(new JSONObject().put("ordinal",label.getInt("ordinal")).put("sourceFrame",label.getInt("sourceFrame")).put("visibility",visibility).put("accepted",accepted).put("correct",good).put("targetOverlap",overlap).put("otherOverlap",other));
        }
        cost.put("scoringMs",(System.nanoTime()-phase)/1e6);phase=System.nanoTime();
        progress("Scoring labels and rechecking all original evidence",trace.length(),trace.length());
        var after=replay.snapshot();if(!before.hash().equals(after.hash()))throw new IllegalStateException("Original evidence or code changed during search; no completion retained");
        cost.put("finalHashMs",(System.nanoTime()-phase)/1e6).put("precomputedOriginalReplay",replay.precomputed())
            .put("historyReads",observations.verificationCost136().getLong("stateReads")-stateStart.getLong("stateReads"))
            .put("historyBytesHashed",observations.verificationCost136().getLong("stateBytesHashed")-stateStart.getLong("stateBytesHashed"));
        return new JSONObject().put("schema","continuity-experiment-157-v1").put("auditId",auditId).put("auditSha256",WorldFrames.hash(audit)).put("evidenceFingerprint",after.hash())
            .put("datasetId",dataset.getString("id")).put("datasetSha256",WorldFrames.hash(dataset)).put("runSha256",WorldFrames.hash(run)).put("objectKey",key)
            .put("referenceCount",memory.getJSONArray("examples").length()).put("trace",trace).put("scores",scores).put("searchEvaluations",evaluations)
            .put("summary",new JSONObject().put("visible",visible).put("correct",correct).put("missing",missing).put("overlapFailures",wrong).put("hidden",hidden).put("unsupported",unsupported).put("confirmedDistractorMatches",others))
            .put("baselineSummary",new JSONObject(audit.getJSONObject("summary").toString())).put("automaticAdmissions",0).put("physicalActionAuthorized",false)
            .put("scope","Explicit exact original-audit reconstruction comparison. Original149 sampling coordinates and immutable reference terms are computed once with exact arithmetic; frame grayscale comes from fresh source pixels; every historical source, reference, code, candidate, score and outcome check remains. Other original methods use their historical verifier. All search candidates, score arithmetic and verification checks retained. Original accepted boxes are preserved only within an uninterrupted measured segment with fresh full-object and local-motion support. Loss closes that segment; only unchanged153 four-frame confirmation adds returns. Every image is hashed against a verified manifest and all source/code bytes are checked before and after. Completed process-local replay may be reused only under exact bytes; cold start and forced replay reconstruct. Labels score the completed trace. No defaults, world or appearance memory changes. Invisible identical substitution and general physical accuracy remain unresolved.");
    }
}
