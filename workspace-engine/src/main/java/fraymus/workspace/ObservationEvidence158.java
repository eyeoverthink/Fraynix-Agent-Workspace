package fraymus.workspace;

import org.json.*;
import java.nio.file.*;
import java.util.*;

/** New reconstruction lineage. Original whole-store fingerprints are never overwritten or relabelled. */
final class ObservationEvidence158 implements ObservationTask158.Evidence {
    private final Path project,root;
    private final ContinuityExperiment157 replay;
    private final ReplayEvidence157 snapshots;
    private record Entry(String fingerprint,String reconstructedId){}
    private final LinkedHashMap<String,Entry> memo=new LinkedHashMap<>(16,.75f,true);
    ObservationEvidence158(Path project,ContinuityExperiment157 replay,ReplayEvidence157 snapshots){this.project=project;this.root=project.resolve("observation-revalidations158");this.replay=replay;this.snapshots=snapshots;}
    synchronized public JSONObject read(String id)throws Exception{
        if(!id.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Invalid original evidence ID");
        Path file=project.resolve("continuity-experiments157").resolve(id+".json");ReplayEvidence157.unlinkedRealPath(file);
        if(Files.size(file)>8_000_000)throw new IllegalStateException("Original evidence exceeds bound");byte[] original=Files.readAllBytes(file);var saved=new JSONObject(new String(original,java.nio.charset.StandardCharsets.UTF_8));
        var content=WorldFrames.copy(saved);content.remove("id");if(!saved.getString("id").equals(id)||!WorldFrames.hash(content).equals(id)||!saved.getString("schema").equals("continuity-experiment-157-v1"))throw new IllegalStateException("Original evidence checksum or schema changed");
        if(!Files.isRegularFile(project.resolve("world-observations/state.json"),LinkOption.NOFOLLOW_LINKS))throw new IllegalStateException("Original observation history missing");
        var before=snapshots.snapshot();var cached=memo.get(id);JSONObject fresh;
        try{
            // A new full reconstruction is mandatory after restart or changed source/code bytes.
            fresh=cached!=null&&cached.fingerprint.equals(before.hash())?replay.read(cached.reconstructedId).getJSONObject("experiment"):replay.run(saved.getString("auditId")).getJSONObject("experiment");
            compare(saved,fresh);var after=snapshots.snapshot();ReplayEvidence157.unlinkedRealPath(file);
            if(!before.hash().equals(after.hash())||!Arrays.equals(original,Files.readAllBytes(file)))throw new IllegalStateException("Original evidence changed during revalidation");
            var link=new JSONObject().put("schema","observation-revalidation-158-v1").put("originalId",id).put("originalSha256",PerceptionStore.hash(original)).put("originalFingerprint",saved.getString("evidenceFingerprint")).put("reconstructedId",fresh.getString("id")).put("currentFingerprint",after.hash()).put("codeSha256",ObservationTask158.code()).put("exactSourceAndOutcomeEquality",true).put("scope","New complete reconstruction; every field except whole-store snapshot fingerprint and derived ID is compared. Original record remains unchanged. No historical fingerprint is claimed current.");
            String linkId=WorldFrames.hash(link);link.put("id",linkId);Files.createDirectories(root);ReplayEvidence157.unlinkedRealPath(root);Path dest=root.resolve(linkId+".json");
            if(Files.exists(dest)){ReplayEvidence157.unlinkedRealPath(dest);if(Files.size(dest)>20_000||!new JSONObject(Files.readString(dest)).similar(link))throw new IllegalStateException("Revalidation lineage changed");}
            else{try(var files=Files.list(root)){if(files.filter(p->p.toString().endsWith(".json")).count()>=128)throw new IllegalStateException("Revalidation history limit128; preserve originals");}var temporary=Files.createTempFile(root,"revalidation-",".pending");Files.writeString(temporary,link.toString());Files.move(temporary,dest,StandardCopyOption.ATOMIC_MOVE);}
            memo.put(id,new Entry(after.hash(),fresh.getString("id")));while(memo.size()>8)memo.remove(memo.keySet().iterator().next());return new JSONObject().put("experiment",saved).put("revalidation",link);
        }catch(Exception e){memo.remove(id);throw e;}
    }
    static void compare(JSONObject original,JSONObject fresh){var a=WorldFrames.copy(original);var b=WorldFrames.copy(fresh);for(String k:List.of("id","evidenceFingerprint")){a.remove(k);b.remove(k);}if(!a.similar(b))throw new IllegalStateException("Original source or reconstructed outcome differs; no task authority granted");}
}
