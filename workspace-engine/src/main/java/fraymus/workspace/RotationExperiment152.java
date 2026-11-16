package fraymus.workspace;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

/** Explicit read-only research track, separately versioned so original proofs remain original. */
final class RotationExperiment152 {
    private final Path root;
    private final WorldObservationStore observations;
    private final AssociationAudit134 association;
    private final ReplayEvidence151 replay;
    RotationExperiment152(Path project,WorldObservationStore observations,AssociationAudit134 association,ReplayEvidence151 replay){root=project.resolve("rotation-experiments152");this.observations=observations;this.association=association;this.replay=replay;}
    synchronized JSONObject run(String auditId)throws Exception{
        long started=System.nanoTime();var content=evaluate(auditId);String id=WorldFrames.hash(content);content.put("id",id);
        Files.createDirectories(root);ReplayEvidence151.unlinkedRealPath(root);
        Path file=root.resolve(id+".json");if(Files.exists(file)){if(!new JSONObject(Files.readString(file)).similar(content))throw new IllegalStateException("Saved rotation evidence differs");}
        else{
            try(var files=Files.list(root)){if(files.filter(p->p.toString().endsWith(".json")).count()>=64)throw new IllegalStateException("Rotation evidence capacity reached; originals preserved");}
            var temporary=Files.createTempFile(root,"rotation-",".pending");Files.writeString(temporary,content.toString(),StandardCharsets.UTF_8);Files.move(temporary,file,StandardCopyOption.ATOMIC_MOVE);
        }
        return new JSONObject().put("experiment",content).put("totalMs",(System.nanoTime()-started)/1e6).put("replayed",true);
    }
    synchronized JSONObject read(String id)throws Exception{
        if(!id.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Invalid rotation evidence identity");
        Path file=root.resolve(id+".json");ReplayEvidence151.unlinkedRealPath(file);
        if(Files.size(file)>8_000_000)throw new IllegalStateException("Rotation evidence exceeds bound");
        byte[] original=Files.readAllBytes(file);var saved=new JSONObject(new String(original,StandardCharsets.UTF_8));
        String embedded=saved.getString("id");saved.remove("id");if(!id.equals(embedded)||!id.equals(WorldFrames.hash(saved)))throw new IllegalStateException("Rotation evidence checksum changed");
        long start=System.nanoTime();var actual=evaluate(saved.getString("auditId"));
        if(!actual.similar(saved)||!java.util.Arrays.equals(original,Files.readAllBytes(file)))throw new IllegalStateException("Rotation source, implementation or outcome changed");
        return new JSONObject().put("experiment",actual.put("id",id)).put("totalMs",(System.nanoTime()-start)/1e6).put("replayed",true);
    }
    private JSONObject evaluate(String auditId)throws Exception{
        var audit=replay.read(auditId,false).getJSONObject("audit");var before=replay.snapshot();
        var run=observations.read(audit.getJSONObject("input").getString("runId"));var dataset=association.verifiedDataset(run.getString("datasetId"));
        if(!WorldFrames.hash(run).equals(audit.getString("runSha256"))||!WorldFrames.hash(dataset).equals(audit.getString("datasetSha256")))throw new IllegalStateException("Original comparison changed");
        // The label record selects the source object only. Its returned boxes and visibility
        // cannot reach RotationReturn152: the complete trace is produced before scoring.
        String key=association.read(audit.getJSONObject("input").getString("labelId")).getJSONObject("input").getString("objectKey");
        JSONObject source=null;for(Object raw:dataset.getJSONArray("objects")){var o=(JSONObject)raw;if(o.getString("key").equals(key))source=o;}
        if(source==null)throw new IllegalStateException("Original source object missing");
        var memory=run.optJSONObject("referenceMemory137");if(memory==null)memory=new JSONObject().put("examples",new JSONArray());
        var first=WorldObservationStore.decode(observations.image(dataset.getString("id"),0));
        var tracker=new RotationReturn152(first,PixelTracker.Box.from(source.getJSONArray("box")),memory);
        var trace=new JSONArray();long evaluations=0;
        for(int i=1;i<dataset.getJSONArray("frames").length();i++){
            if(Thread.currentThread().isInterrupted())throw new InterruptedException("Rotation experiment interrupted; no completion saved");
            var row=tracker.update(WorldObservationStore.decode(observations.image(dataset.getString("id"),i)));
            row.put("imageSha256",dataset.getJSONArray("frames").getJSONObject(i).getString("imageSha256"));trace.put(row);evaluations+=row.getLong("evaluations");
        }
        var scores=new JSONArray();int visible=0,correct=0,missing=0,wrong=0,hidden=0,unsupported=0,others=0;
        for(Object raw:audit.getJSONArray("scores")){
            var label=(JSONObject)raw;var found=trace.getJSONObject(label.getInt("ordinal")-1);boolean accepted=!found.isNull("box");String visibility=label.getString("visibility");
            double overlap=0,other=0;if(accepted){var box=PixelTracker.Box.from(found.getJSONArray("box"));if(label.has("targetBox")&&!label.isNull("targetBox"))overlap=PixelTracker.iou(box,PixelTracker.Box.from(label.getJSONArray("targetBox")));for(Object o:label.optJSONArray("otherBoxes")==null?new JSONArray():label.getJSONArray("otherBoxes"))other=Math.max(other,PixelTracker.iou(box,PixelTracker.Box.from((JSONArray)o)));}
            boolean good=accepted&&overlap>=.5;if(visibility.equals("visible")||visibility.equals("partial")){visible++;if(!accepted)missing++;else if(good)correct++;else wrong++;}
            if(visibility.equals("occluded")||visibility.equals("out-of-view")){hidden++;if(accepted)unsupported++;}
            if(accepted&&other>=.5)others++;
            scores.put(new JSONObject().put("ordinal",label.getInt("ordinal")).put("sourceFrame",label.getInt("sourceFrame")).put("visibility",visibility).put("accepted",accepted).put("correct",good).put("targetOverlap",overlap).put("otherOverlap",other));
        }
        var after=replay.snapshot();if(!before.hash().equals(after.hash()))throw new IllegalStateException("Original evidence or code changed during search; no completion retained");
        return new JSONObject().put("schema","rotation-experiment-152-v1").put("auditId",auditId).put("auditSha256",WorldFrames.hash(audit)).put("evidenceFingerprint",after.hash())
            .put("datasetId",dataset.getString("id")).put("datasetSha256",WorldFrames.hash(dataset)).put("runSha256",WorldFrames.hash(run)).put("objectKey",key)
            .put("referenceCount",memory.getJSONArray("examples").length()).put("trace",trace).put("scores",scores).put("searchEvaluations",evaluations)
            .put("summary",new JSONObject().put("visible",visible).put("correct",correct).put("missing",missing).put("overlapFailures",wrong).put("hidden",hidden).put("unsupported",unsupported).put("confirmedDistractorMatches",others))
            .put("baselineSummary",new JSONObject(audit.getJSONObject("summary").toString())).put("automaticAdmissions",0).put("physicalActionAuthorized",false)
            .put("scope","Experimental rotation search, not the active tracker. Only original seed and already verified references guide search; independent labels score the completed trace. Four adjacent full matches plus optical flow are required. Detected competitors remain a veto. Identical unseen substitution, calibrated pose and general physical accuracy are unresolved. No defaults, world state or appearance memory changed. Verification recomputes all source and outcome checks.");
    }
}
