package fraymus.workspace;
import brain_architecture.kernel.MerkleAppendOnlyJournal;
import org.junit.Test;
import org.junit.Assume;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import com.sun.nio.file.ExtendedOpenOption;
import java.util.Map;
import java.util.concurrent.*;

/** Real Windows sharing denial, including failure and interruption preservation. */
public class JournalAtomic067Test {
    static Path root()throws Exception{
        Assume.assumeTrue(System.getProperty("os.name").startsWith("Windows"));
        var root=Files.createTempDirectory("journal-atomic067-");
        new MerkleAppendOnlyJournal(root).append("first","retained",Map.of());
        return root;
    }
    @Test public void transientReadHandleRetainsExactlyOneAppend()throws Exception{
        var root=root();var journal=new MerkleAppendOnlyJournal(root);var executor=Executors.newSingleThreadScheduledExecutor();
        try(var held=FileChannel.open(root.resolve("kernel-journal.log"),StandardOpenOption.READ,ExtendedOpenOption.NOSHARE_DELETE)){
            var release=executor.schedule(()->{try{held.close();}catch(Exception e){throw new RuntimeException(e);}},150,TimeUnit.MILLISECONDS);
            journal.append("second","committed",Map.of());release.get();assertEquals(2,journal.size());
            assertEquals(2,new MerkleAppendOnlyJournal(root).verify().entries());assertTrue(journal.verify().pendingFiles().isEmpty());
        }finally{executor.shutdownNow();}
    }
    @Test public void persistentLockPreservesOldAndUncommittedEvidence()throws Exception{
        var root=root();var file=root.resolve("kernel-journal.log");var before=Files.readAllBytes(file);var journal=new MerkleAppendOnlyJournal(root);
        try(var held=FileChannel.open(file,StandardOpenOption.READ,ExtendedOpenOption.NOSHARE_DELETE)){
            assertThrows(AccessDeniedException.class,()->journal.append("second","uncommitted",Map.of()));
            assertEquals(1,journal.size());assertArrayEquals(before,Files.readAllBytes(file));
        }
        var v=new MerkleAppendOnlyJournal(root).verify();assertEquals(1,v.entries());assertEquals(1,v.pendingFiles().size());
        assertEquals(2,Files.readAllLines(root.resolve(v.pendingFiles().get(0))).size());
    }
    @Test public void interruptedRetryRetainsInterruptAndOriginalJournal()throws Exception{
        var root=root();var file=root.resolve("kernel-journal.log");var before=Files.readAllBytes(file);var journal=new MerkleAppendOnlyJournal(root);
        var executor=Executors.newSingleThreadScheduledExecutor();Thread writer=Thread.currentThread();
        try(var held=FileChannel.open(file,StandardOpenOption.READ,ExtendedOpenOption.NOSHARE_DELETE)){
            executor.schedule(writer::interrupt,100,TimeUnit.MILLISECONDS);
            assertThrows(java.io.InterruptedIOException.class,()->journal.append("second","interrupted",Map.of()));
            assertTrue(Thread.currentThread().isInterrupted());assertEquals(1,journal.size());assertArrayEquals(before,Files.readAllBytes(file));
        }finally{executor.shutdownNow();Thread.interrupted();}
        assertEquals(1,new MerkleAppendOnlyJournal(root).verify().pendingFiles().size());
    }
}
