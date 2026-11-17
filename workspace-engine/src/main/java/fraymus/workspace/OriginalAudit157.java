package fraymus.workspace;

import java.nio.file.*;
import org.json.*;

/** Reconstruct an existing149 audit with exact frame-local RGB access. No historical proof is relabelled. */
final class OriginalAudit157 {
    private final Path project;
    private final WorldObservationStore observations;
    private final AssociationAudit134 original;
    private final boolean precomputed;
    OriginalAudit157(Path project,WorldObservationStore observations,AssociationAudit134 original,boolean precomputed){
        this.project=project;this.observations=observations;this.original=original;this.precomputed=precomputed;
    }
    JSONObject readAudit(String id)throws Exception{
        if(!precomputed)return new OriginalAudit156(project,observations,original,true).readAudit(id);
        if(!id.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Invalid evidence identity");
        Path file=project.resolve("association-labels134/audits").resolve(id+".json");
        ReplayEvidence151.unlinkedRealPath(file);
        if(Files.size(file)>1_000_000)throw new IllegalStateException("Evidence size exceeds bound");
        var saved=new JSONObject(Files.readString(file));var expected=WorldFrames.copy(saved);expected.remove("id");
        if(!id.equals(saved.getString("id"))||!id.equals(WorldFrames.hash(expected)))throw new IllegalStateException("Retained evidence checksum changed");
        var input=saved.getJSONObject("input");SpatialScene.keys(input,"labelId","runId");
        var policyRun=observations.read(input.getString("runId"));
        if(!IdentityAudit149.uses(ObservationMethods129.policy(policyRun)))return original.readAudit(id);
        if(!evaluate(input).similar(expected))throw new IllegalStateException("Original audit source, code or outcome changed");
        return saved;
    }
    private JSONObject evaluate(JSONObject input)throws Exception{
        SpatialScene.keys(input,"labelId","runId");
        var record=original.read(input.getString("labelId"));var labels=record.getJSONObject("input");
        var run=observations.read(input.getString("runId"));
        if(!run.getString("status").equals("completed")||!run.getString("datasetId").equals(labels.getString("datasetId")))throw new IllegalArgumentException("Use the same completed labelled source");
        if(!run.getString("codeSha256").equals(WorldObservationStore.identity()))throw new IllegalStateException("Original tracker implementation differs; preserve its historical evidence");
        observations.verifyReference137(run);var dataset=original.verifiedDataset(run.getString("datasetId"));String body=null;
        for(Object raw:run.getJSONArray("bindings")){var b=(JSONObject)raw;if(b.getString("objectKey").equals(labels.getString("objectKey")))body=b.getString("bodyId");}
        if(body==null)throw new IllegalArgumentException("Missing source-object binding");
        var result=AssociationAudit134.compute(labels,dataset,run,body);
        result.put("identity149",IdentityAudit157.check(run,dataset,labels,body,ordinal->{
            if(Thread.currentThread().isInterrupted())throw new InterruptedException("Original replay interrupted; no completion");
            Path file=project.resolve("world-observations/datasets").resolve(dataset.getString("id")).resolve(ordinal+".image");
            ReplayEvidence151.unlinkedRealPath(file);
            byte[] pixels=Files.readAllBytes(file);
            if(!PerceptionStore.hash(pixels).equals(dataset.getJSONArray("frames").getJSONObject(ordinal).getString("imageSha256")))throw new IllegalStateException("Original replay source changed");
            var image=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(pixels));
            if(image==null)throw new IllegalStateException("Original replay image unavailable");return image;
        },true));
        return result.put("input",WorldFrames.copy(input)).put("codeSha256",AssociationAudit134.identity()).put("trackerCodeSha256",run.getString("codeSha256")).put("runSha256",WorldFrames.hash(run)).put("labelSha256",WorldFrames.hash(record)).put("datasetSha256",WorldFrames.hash(dataset)).put("datasetId",dataset.getString("id")).put("trackerPolicy",run.optString("trackerPolicy","legacy-flow"));
    }
}
