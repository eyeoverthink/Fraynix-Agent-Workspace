package fraymus.workspace;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.json.*;

/** Curates reference-supported examples AFTER evaluation; never supplies evaluation tracker inputs. */
final class VerifiedAppearances135 {
    private final Path root;
    private final AssociationAudit134 association;
    VerifiedAppearances135(Path project,AssociationAudit134 association){root=project.resolve("verified-appearances135");this.association=association;}

    private static String identity()throws Exception{
        try(var in=VerifiedAppearances135.class.getResourceAsStream("VerifiedAppearances135.class")){return PerceptionStore.hash(in.readAllBytes());}
    }

    private JSONObject evaluate(String auditId)throws Exception{
        var audit=association.readAudit(auditId);var bank=audit.getJSONObject("appearance135");
        if(!bank.getBoolean("available"))throw new IllegalArgumentException("Choose an audited appearance-bank run");
        var examples=new JSONArray();var references=new HashMap<Integer,JSONObject>();
        for(Object raw:audit.getJSONArray("scores")){var row=(JSONObject)raw;references.put(row.getInt("ordinal"),row);}
        for(Object raw:bank.getJSONArray("admissions")){
            var admitted=(JSONObject)raw;int ordinal=admitted.getInt("ordinal");var reference=references.get(ordinal);
            if(!admitted.getString("verdict").equals("correct")||reference==null||!reference.getString("visibility").equals("visible"))continue;
            var source=association.source(audit.getString("datasetId"),ordinal);
            if(!source.getJSONObject("frame").getString("imageSha256").equals(reference.getString("imageSha256")))throw new IllegalStateException("Original appearance image changed");
            byte[] bytes=Base64.getDecoder().decode(source.getString("image"));
            if(!PerceptionStore.hash(bytes).equals(reference.getString("imageSha256")))throw new IllegalStateException("Original appearance bytes changed");
            var box=PixelTracker.Box.from(reference.getJSONArray("targetBox"));
            double[] descriptor=PixelTracker.descriptor(PixelTracker.working(WorldObservationStore.decode(bytes)),box);
            var values=new JSONArray(descriptor);
            examples.put(new JSONObject().put("ordinal",ordinal).put("sourceFrame",reference.getInt("sourceFrame"))
                .put("imageSha256",reference.getString("imageSha256")).put("referenceBox",box.json()).put("proposedBox",admitted.getJSONObject("example").getJSONArray("box"))
                .put("proposedOverlap",admitted.getDouble("targetOverlap")).put("proposedDescriptorSha256",admitted.getJSONObject("example").getString("descriptorSha256"))
                .put("descriptor",values).put("descriptorSha256",WorldFrames.hash(new JSONObject().put("values",values)))
                .put("verification","fully visible independent reference; proposal overlap >=0.5; descriptor rebuilt from the independent box"));
        }
        if(examples.isEmpty())throw new IllegalArgumentException("No independently supported fully visible examples in this audit");
        return new JSONObject().put("schema","verified-appearances-135-v1").put("input",new JSONObject().put("auditId",auditId))
            .put("codeSha256",identity()).put("trackerCodeSha256",audit.getString("trackerCodeSha256"))
            .put("auditSha256",WorldFrames.hash(audit)).put("datasetId",audit.getString("datasetId")).put("datasetSha256",audit.getString("datasetSha256"))
            .put("examples",examples).put("excludedAdmissions",bank.getJSONArray("admissions").length()-examples.length())
            .put("scope","Post-evaluation curation from independent target boxes and declared visibility. Partial, uncertain and incorrect proposals are excluded. Rebuilt numeric appearances are portable; original project/source/audit are required to reverify. No semantic identity certification, automatic model training or feedback into the measured tracking run.");
    }

    synchronized JSONObject retain(String auditId)throws Exception{
        var result=evaluate(auditId);String id=WorldFrames.hash(result);result.put("id",id);Files.createDirectories(root);
        try(var channel=FileChannel.open(root.resolve("write.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);var lock=channel.lock()){
            Path file=root.resolve(id+".json");
            if(Files.exists(file)){if(!read(id).similar(result))throw new IllegalStateException("Appearance evidence identity conflict");return result;}
            try(var files=Files.list(root)){if(files.filter(p->p.toString().endsWith(".json")).count()>=64)throw new IllegalStateException("64 retained appearance sets reached; no pruning");}
            byte[] bytes=result.toString(2).getBytes(StandardCharsets.UTF_8);if(bytes.length>1_000_000)throw new IllegalArgumentException("Appearance set exceeds bound");
            Path pending=root.resolve(id+".pending");try(var out=FileChannel.open(pending,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE)){
                var buffer=ByteBuffer.wrap(bytes);while(buffer.hasRemaining())out.write(buffer);out.force(true);
            }
            Files.move(pending,file,StandardCopyOption.ATOMIC_MOVE);
        }
        return read(id);
    }

    JSONObject read(String id)throws Exception{
        if(!id.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Invalid appearance evidence identity");
        var file=root.resolve(id+".json");if(Files.size(file)>1_000_000)throw new IllegalStateException("Appearance set exceeds bound");
        var result=new JSONObject(Files.readString(file));var payload=WorldFrames.copy(result);payload.remove("id");
        if(!id.equals(result.getString("id"))||!id.equals(WorldFrames.hash(payload)))throw new IllegalStateException("Retained appearance checksum changed");
        if(!evaluate(result.getJSONObject("input").getString("auditId")).similar(payload))throw new IllegalStateException("Appearance source, label, descriptor or code changed");
        return result;
    }
}
