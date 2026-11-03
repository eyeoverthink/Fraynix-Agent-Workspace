package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import org.junit.Assume;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import com.sun.nio.file.ExtendedOpenOption;
import java.util.concurrent.*;

/** Actual Windows deny-delete file handles, not a simulated successful save. */
public class AtomicCheckpoint054Test {
 static Path file()throws Exception{Assume.assumeTrue(System.getProperty("os.name").startsWith("Windows"));var root=Files.createTempDirectory("atomic-checkpoint054-");var p=root.resolve("task.json");AgentFiles042.saveChecked(p,new JSONObject().put("revision",1));return p;}
 @Test public void temporaryWindowsReadHandleRetriesWithoutDroppingCheckpoint()throws Exception{
  var p=file();var executor=Executors.newSingleThreadScheduledExecutor();long started=System.nanoTime();
  try(var held=FileChannel.open(p,StandardOpenOption.READ,ExtendedOpenOption.NOSHARE_DELETE)){
   var release=executor.schedule(()->{try{held.close();}catch(Exception e){throw new RuntimeException(e);}},150,TimeUnit.MILLISECONDS);
   AgentFiles042.saveChecked(p,new JSONObject().put("revision",2));release.get();assertTrue(System.nanoTime()-started>=100_000_000L);assertEquals(2,AgentFiles042.loadChecked(p).getInt("revision"));try(var paths=Files.list(p.getParent())){assertEquals(1,paths.count());}
  }finally{executor.shutdownNow();}
 }
 @Test public void prolongedWindowsLockPreservesPreviousAndForcedPendingBytes()throws Exception{
  var p=file();byte[] old=Files.readAllBytes(p);
  try(var held=FileChannel.open(p,StandardOpenOption.READ,ExtendedOpenOption.NOSHARE_DELETE)){
   assertThrows(AccessDeniedException.class,()->AgentFiles042.saveChecked(p,new JSONObject().put("revision",2)));assertArrayEquals(old,Files.readAllBytes(p));try(var paths=Files.list(p.getParent())){var pending=paths.filter(x->x.toString().endsWith(".pending")).findFirst().orElseThrow();assertEquals(2,AgentFiles042.loadChecked(pending).getInt("revision"));}
  }
 }
 @Test public void interruptedRetryPreservesCheckpointAndInterruptFlag()throws Exception{
  var p=file();byte[] old=Files.readAllBytes(p);var executor=Executors.newSingleThreadScheduledExecutor();Thread writer=Thread.currentThread();
  try(var held=FileChannel.open(p,StandardOpenOption.READ,ExtendedOpenOption.NOSHARE_DELETE)){
   executor.schedule(writer::interrupt,100,TimeUnit.MILLISECONDS);assertThrows(java.io.InterruptedIOException.class,()->AgentFiles042.saveChecked(p,new JSONObject().put("revision",2)));assertTrue(Thread.currentThread().isInterrupted());assertArrayEquals(old,Files.readAllBytes(p));
  }finally{executor.shutdownNow();Thread.interrupted();}
 }
}
