package fraymus.workspace;

import brain_architecture.kernel.MerkleAppendOnlyJournal;
import java.nio.file.*;
import java.util.*;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class WitnessRecovery126Test {
    private record Pending(Path root,Path segment,Path log,Path pending,byte[] before,byte[] after,JSONObject anchor){}
    private Pending staged()throws Exception{
        Path root=Files.createTempDirectory("witness126-");var witness=new Witness042(root);witness.append("observation",new JSONObject().put("value",1));
        var anchor=witness.summary();Path segment=root.resolve("segment-000000"),log=segment.resolve("kernel-journal.log");byte[] before=Files.readAllBytes(log);
        witness.append("observation",new JSONObject().put("value",2));byte[] after=Files.readAllBytes(log);Files.write(log,before);
        Path pending=segment.resolve("kernel-journal-test.pending");Files.write(pending,after);return new Pending(root,segment,log,pending,before,after,anchor);
    }
    @Test public void completePendingAppendRetainsBothOriginalsAndCheckpointAnchor()throws Exception{
        var p=staged();var recovered=new Witness042(p.root);assertEquals(3,recovered.verify().getInt("records"));assertTrue(recovered.hasAnchor(p.anchor));assertArrayEquals(p.after,Files.readAllBytes(p.log));assertFalse(Files.exists(p.pending));
        String name=recovered.summary().getJSONArray("recoveredAppends126").getJSONObject(0).getString("evidenceFile");assertArrayEquals(p.after,Files.readAllBytes(p.segment.resolve(name)));assertArrayEquals(p.before,Files.readAllBytes(p.segment.resolve(name.replace(".evidence",".before"))));
        assertEquals(3,new Witness042(p.root).verify().getInt("records"));
    }
    @Test public void partialAndAmbiguousPendingFilesAreNeverDiscarded()throws Exception{
        var partial=staged();Files.write(partial.pending,Arrays.copyOf(partial.after,partial.after.length-20));byte[] original=Files.readAllBytes(partial.pending);assertThrows(Exception.class,()->new Witness042(partial.root));assertArrayEquals(original,Files.readAllBytes(partial.pending));assertArrayEquals(partial.before,Files.readAllBytes(partial.log));
        var multiple=staged();Files.copy(multiple.pending,multiple.segment.resolve("kernel-journal-other.pending"));assertThrows(Exception.class,()->new Witness042(multiple.root));assertArrayEquals(multiple.before,Files.readAllBytes(multiple.log));
    }
    @Test public void validChainWithInvalidWitnessPayloadCannotBePromoted()throws Exception{
        var p=staged();Files.delete(p.pending);var journal=new MerkleAppendOnlyJournal(p.segment);journal.append("observation","wrong owner data",Map.of("evidence","{}","sha256","wrong"));byte[] invalid=Files.readAllBytes(p.log);Files.write(p.log,p.before);Files.write(p.pending,invalid);
        assertThrows(Exception.class,()->new Witness042(p.root));assertArrayEquals(p.before,Files.readAllBytes(p.log));assertArrayEquals(invalid,Files.readAllBytes(p.pending));
    }
    @Test public void changedPrefixAndNonAppendCandidatesRemainRefused()throws Exception{
        var p=staged();Files.write(p.pending,p.before);assertThrows(Exception.class,()->new Witness042(p.root));assertArrayEquals(p.before,Files.readAllBytes(p.log));
        var foreign=staged();Files.write(p.pending,foreign.after);assertThrows(Exception.class,()->new Witness042(p.root));assertArrayEquals(p.before,Files.readAllBytes(p.log));
    }
    @Test public void interruptedFirstSegmentHeaderCanRecoverOnlyItsProperParent()throws Exception{
        Path root=Files.createTempDirectory("witness126-first-"),segment=root.resolve("segment-000000");var journal=new MerkleAppendOnlyJournal(segment);journal.append("segment","Witness segment opened",Map.of("parentRoot","GENESIS","segment","0"));Path log=segment.resolve("kernel-journal.log");Files.move(log,segment.resolve("kernel-journal-first.pending"));assertEquals(1,new Witness042(root).verify().getInt("records"));
        Path wrongRoot=Files.createTempDirectory("witness126-parent-"),wrong=wrongRoot.resolve("segment-000000");var bad=new MerkleAppendOnlyJournal(wrong);bad.append("segment","Witness segment opened",Map.of("parentRoot","foreign","segment","0"));Files.move(wrong.resolve("kernel-journal.log"),wrong.resolve("kernel-journal-first.pending"));assertThrows(Exception.class,()->new Witness042(wrongRoot));assertFalse(Files.exists(wrong.resolve("kernel-journal.log")));
    }
}
