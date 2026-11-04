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
        var failure=new java.util.concurrent.atomic.AtomicReference<Throwable>();
        var interrupted=new java.util.concurrent.atomic.AtomicBoolean();
        var writer=new Thread(()->{try{journal.append("second","interrupted",Map.of());}catch(Throwable e){failure.set(e);}finally{interrupted.set(Thread.currentThread().isInterrupted());}},"journal-retry067");
        try(var held=FileChannel.open(file,StandardOpenOption.READ,ExtendedOpenOption.NOSHARE_DELETE)){
            writer.start();long end=System.nanoTime()+5_000_000_000L;
            while(writer.isAlive()&&writer.getState()!=Thread.State.TIMED_WAITING&&System.nanoTime()<end)Thread.sleep(1);
            assertEquals("Interrupt the replacement retry, not an earlier file write",Thread.State.TIMED_WAITING,writer.getState());
            writer.interrupt();writer.join(3000);assertFalse(writer.isAlive());
            assertTrue(String.valueOf(failure.get()),failure.get() instanceof java.io.InterruptedIOException);
            assertTrue(interrupted.get());assertEquals(1,journal.size());assertArrayEquals(before,Files.readAllBytes(file));
        }finally{if(writer.isAlive()){writer.interrupt();writer.join(3000);}}
        assertEquals(1,new MerkleAppendOnlyJournal(root).verify().pendingFiles().size());
    }
}
