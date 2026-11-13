package fraymus.workspace;

import java.nio.file.*;
import java.util.*;
import org.json.*;

/** Explicit separated reference reuse. A target prediction never becomes a reference. */
final class ReferenceMemory137 {
    static final int CAPACITY=6;
    private static final ThreadLocal<Boolean> RESOLVING=ThreadLocal.withInitial(()->false);
    private final Path project;
    private final WorldObservationStore observations;
    ReferenceMemory137(Path project,WorldObservationStore observations){this.project=project;this.observations=observations;}
    static boolean uses(String policy){return Set.of("verified-memory-137","seed-only-137").contains(policy);}

    JSONObject resolve(String setId,String targetId,String declaration)throws Exception{
        if(declaration==null||declaration.isBlank()||declaration.length()>500)throw new IllegalArgumentException("Declare why the reference and target show the same object (1..500 characters)");
        if(RESOLVING.get())throw new IllegalStateException("Reference-derived or recursive source evidence is not supported");
        RESOLVING.set(true);
        try{
            var association=new AssociationAudit134(project,observations);
            var set=new VerifiedAppearances135(project,association).read(setId);
            var source=association.verifiedDataset(set.getString("datasetId"));
            var target=association.verifiedDataset(targetId);
            if(source.getJSONArray("objects").length()!=1||target.getJSONArray("objects").length()!=1)throw new IllegalArgumentException("Reference reuse requires one source object and one target object");
            if(source.getString("id").equals(targetId))throw new IllegalArgumentException("Use a separate evaluation dataset");
            var hashes=new HashSet<String>();for(Object raw:source.getJSONArray("frames"))hashes.add(((JSONObject)raw).getString("imageSha256"));
            for(Object raw:target.getJSONArray("frames"))if(hashes.contains(((JSONObject)raw).getString("imageSha256")))throw new IllegalArgumentException("Reference and evaluation share source image bytes");
            var available=set.getJSONArray("examples");var chosen=new JSONArray();int n=Math.min(CAPACITY,available.length());
            // Deterministic chronological coverage, fixed before any target prediction.
            for(int i=0;i<n;i++)chosen.put(ParsedJson137.copy(available.getJSONObject(n==1?0:i*(available.length()-1)/(n-1))));
            return new JSONObject().put("schema","verified-reference-memory-137-v1").put("setId",setId)
                .put("setSha256",WorldFrames.hash(set)).put("sourceDatasetId",source.getString("id"))
                .put("sourceDatasetSha256",WorldFrames.hash(source)).put("targetDatasetId",targetId)
                .put("targetDatasetSha256",WorldFrames.hash(target)).put("declaration",declaration)
                .put("examples",chosen).put("availableExamples",available.length()).put("capacity",CAPACITY)
                .put("scope","Independent reference boxes and fully visible source annotations; original evidence reverified. Same-object declaration is supplied by the operator, not a semantic identity proof. No target predictions admitted.");
        }finally{RESOLVING.remove();}
    }

    void verify(JSONObject run)throws Exception{
        String policy=ObservationMethods129.policy(run);
        if(!uses(policy)){if(run.has("referenceMemory137"))throw new IllegalStateException("Unexpected reference memory on this method");return;}
        var memory=run.getJSONObject("referenceMemory137");
        if(!resolve(memory.getString("setId"),run.getString("datasetId"),memory.getString("declaration")).similar(memory))throw new IllegalStateException("Reference provenance or selected examples changed");
    }

    JSONObject list()throws Exception{
        var sets=new JSONArray();Path folder=project.resolve("verified-appearances135");
        if(Files.exists(folder))try(var stream=Files.list(folder)){
            var files=stream.filter(p->p.toString().endsWith(".json")).sorted().toList();
            if(files.size()>64)throw new IllegalStateException("Retained reference capacity exceeded");
            for(Path path:files){
                if(Files.size(path)>1_000_000)throw new IllegalStateException("Reference set exceeds bound");
                var set=new JSONObject(Files.readString(path));var payload=ParsedJson137.copy(set);payload.remove("id");String id=set.getString("id");
                if(!path.getFileName().toString().equals(id+".json")||!id.equals(WorldFrames.hash(payload)))throw new IllegalStateException("Retained reference checksum changed");
                sets.put(new JSONObject().put("id",id).put("datasetId",set.getString("datasetId")).put("examples",set.getJSONArray("examples").length()));
            }
        }
        return new JSONObject().put("sets",sets).put("scope","Listing checks saved bytes only; original source, code and outcome checks run before use. Older incompatible evidence remains historical.");
    }

    JSONObject bundle(JSONObject run)throws Exception{
        verify(run);var association=new AssociationAudit134(project,observations);
        var set=new VerifiedAppearances135(project,association).read(run.getJSONObject("referenceMemory137").getString("setId"));
        var audit=association.readAudit(set.getJSONObject("input").getString("auditId"));
        return new JSONObject().put("set",set).put("audit",audit)
            .put("labels",association.read(audit.getJSONObject("input").getString("labelId")))
            .put("sourceRun",observations.read(audit.getJSONObject("input").getString("runId")));
    }
}
