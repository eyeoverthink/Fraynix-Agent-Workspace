package fraymus.workspace;

import java.nio.file.*;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReplayEvidence151Test {
    @Test public void missingStateIsRefusedWithoutCreatingAnEmptyHistory()throws Exception {
        try(var f=new StartingReference150Test.Fixture()) {
            var memo=new ReplayEvidence151(f.root,f.association);String id=f.audit.getString("id");memo.read(id,false);
            Path file=f.root.resolve("world-observations/state.json");byte[] original=Files.readAllBytes(file);Files.delete(file);
            assertThrows(IllegalStateException.class,()->memo.read(id,false));assertFalse(Files.exists(file));Files.write(file,original);
            assertFalse(memo.read(id,false).getJSONObject("cost151").getBoolean("reusedReplay"));
        }
    }
    @Test public void warmCopyForceAndColdRestartRetainExactOriginalOutcomes()throws Exception {
        try(var f=new StartingReference150Test.Fixture()) {
            var memo=new ReplayEvidence151(f.root,f.association);String id=f.audit.getString("id");
            var first=memo.read(id,false);assertFalse(first.getJSONObject("cost151").getBoolean("reusedReplay"));assertTrue(first.getJSONObject("audit").similar(f.audit));
            first.getJSONObject("audit").getJSONObject("summary").put("correct",12345);
            var warm=memo.read(id,false);assertTrue(warm.getJSONObject("cost151").getBoolean("reusedReplay"));assertTrue(warm.getJSONObject("audit").similar(f.audit));assertTrue(warm.getJSONObject("cost151").getLong("bytesHashed")>0);
            assertFalse(memo.read(id,true).getJSONObject("cost151").getBoolean("reusedReplay"));
            assertFalse(new ReplayEvidence151(f.root,f.association).read(id,false).getJSONObject("cost151").getBoolean("reusedReplay"));
        }
    }
    @Test public void sameSizeSameTimestampPixelsAndLabelsCannotHitMemo()throws Exception {
        try(var f=new StartingReference150Test.Fixture()) {
            var memo=new ReplayEvidence151(f.root,f.association);String id=f.audit.getString("id");memo.read(id,false);
            Path image=f.root.resolve("world-observations/datasets/"+f.data.getString("id")+"/1.image");byte[] original=Files.readAllBytes(image);var time=Files.getLastModifiedTime(image);var altered=original.clone();altered[20]^=1;Files.write(image,altered);Files.setLastModifiedTime(image,time);
            assertThrows(Exception.class,()->memo.read(id,false));Files.write(image,original);Files.setLastModifiedTime(image,time);assertFalse(memo.read(id,false).getJSONObject("cost151").getBoolean("reusedReplay"));
            Path label=f.root.resolve("association-labels134/labels/"+f.audit.getJSONObject("input").getString("labelId")+".json");original=Files.readAllBytes(label);Files.writeString(label,Files.readString(label).replace("stationary","STATionary"));
            assertThrows(Exception.class,()->memo.read(id,false));Files.write(label,original);assertTrue(memo.read(id,false).getJSONObject("audit").similar(f.audit));
        }
    }
    @Test public void forgedOutcomeIsRefusedEvenWithItsOwnValidChecksum()throws Exception {
        try(var f=new StartingReference150Test.Fixture()) {
            var memo=new ReplayEvidence151(f.root,f.association);memo.read(f.audit.getString("id"),false);
            var forged=WorldFrames.copy(f.audit);forged.remove("id");forged.getJSONObject("summary").put("correct",0);String id=WorldFrames.hash(forged);forged.put("id",id);
            Files.writeString(f.root.resolve("association-labels134/audits/"+id+".json"),forged.toString());
            assertThrows(IllegalStateException.class,()->memo.read(id,false));
            Files.delete(f.root.resolve("association-labels134/audits/"+id+".json"));assertTrue(memo.read(f.audit.getString("id"),false).getJSONObject("audit").similar(f.audit));
        }
    }
    @Test public void namespacesAndEvidenceDeletionInvalidateRatherThanTrustMetadata()throws Exception {
        try(var f=new StartingReference150Test.Fixture()) {
            var memo=new ReplayEvidence151(f.root,f.association);String id=f.audit.getString("id");memo.read(id,false);
            Files.createDirectories(f.root.resolve("verified-appearances135"));var extra=f.root.resolve("verified-appearances135/unrelated.pending");Files.writeString(extra,"changed");
            assertFalse(memo.read(id,false).getJSONObject("cost151").getBoolean("reusedReplay"));Files.delete(extra);assertFalse(memo.read(id,false).getJSONObject("cost151").getBoolean("reusedReplay"));
            Path image=f.root.resolve("world-observations/datasets/"+f.data.getString("id")+"/2.image");byte[] original=Files.readAllBytes(image);Files.delete(image);assertThrows(Exception.class,()->memo.read(id,false));Files.write(image,original);
            assertFalse(memo.read(id,false).getJSONObject("cost151").getBoolean("reusedReplay"));assertThrows(IllegalArgumentException.class,()->memo.read("../../outside",false));
        }
    }
    @Test public void capacityIsBoundedAndOldestEntryRequiresReplay()throws Exception {
        try(var f=new StartingReference150Test.Fixture()) {
            var ids=new java.util.ArrayList<String>();ids.add(f.audit.getString("id"));
            for(int i=0;i<ReplayEvidence151.CAPACITY;i++) {var labels=WorldFrames.copy(f.labels);labels.put("provenance",labels.getString("provenance")+" Variation "+i);ids.add(f.audit(f.association.save(labels)).getString("id"));}
            var memo=new ReplayEvidence151(f.root,f.association);for(String id:ids)assertTrue(memo.read(id,false).getJSONObject("cost151").getInt("retainedEntries")<=ReplayEvidence151.CAPACITY);
            assertTrue(memo.read(ids.getLast(),false).getJSONObject("cost151").getBoolean("reusedReplay"));assertFalse(memo.read(ids.getFirst(),false).getJSONObject("cost151").getBoolean("reusedReplay"));
        }
    }
}
