package fraymus.workspace;

import org.json.JSONObject;
import org.junit.*;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import com.sun.nio.file.ExtendedOpenOption;
import java.util.concurrent.*;
import static org.junit.Assert.*;

/** Real Windows reader locks on the native checkpoint writer; no simulated saves. */
public class NativeCheckpoint094Test {
 static Path file() throws Exception {
  Assume.assumeTrue(System.getProperty("os.name").startsWith("Windows"));
  Path p=Files.createTempDirectory("native-checkpoint094-").resolve("native.json");
  WorldNativeCheckpoint.write(p,new JSONObject().put("revision",1));return p;
 }
 static JSONObject payload(Path p) throws Exception {
  var e=new JSONObject(Files.readString(p));assertEquals(WorldNativeCheckpoint.VERSION,e.getString("version"));
  assertEquals(WorldFrames.hash(e.getJSONObject("payload")),e.getString("sha256"));return e.getJSONObject("payload");
 }
 @Test public void transientReaderRetainsAtomicNativeCheckpoint() throws Exception {
  Path p=file();var executor=Executors.newSingleThreadScheduledExecutor();
  try(var held=FileChannel.open(p,StandardOpenOption.READ,ExtendedOpenOption.NOSHARE_DELETE)){
   var release=executor.schedule(()->{try{held.close();}catch(Exception e){throw new RuntimeException(e);}},150,TimeUnit.MILLISECONDS);
   WorldNativeCheckpoint.write(p,new JSONObject().put("revision",2));release.get();assertEquals(2,payload(p).getInt("revision"));
   try(var files=Files.list(p.getParent())){assertEquals(1,files.count());}
  }finally{executor.shutdownNow();}
 }
 @Test public void exhaustedLockKeepsPreviousAndForcedPendingBytes() throws Exception {
  Path p=file();byte[] old=Files.readAllBytes(p);long at=System.nanoTime();
  try(var held=FileChannel.open(p,StandardOpenOption.READ,ExtendedOpenOption.NOSHARE_DELETE)){
   assertThrows(AccessDeniedException.class,()->WorldNativeCheckpoint.write(p,new JSONObject().put("revision",2)));
   assertTrue("The bounded retry must run",System.nanoTime()-at>=700_000_000L);assertArrayEquals(old,Files.readAllBytes(p));
   try(var files=Files.list(p.getParent())){var pending=files.filter(x->x.toString().endsWith(".pending")).findFirst().orElseThrow();assertEquals(2,payload(pending).getInt("revision"));}
  }
 }
 @Test public void interruptedRetryKeepsOldCheckpointAndInterrupt() throws Exception {
  Path p=file();byte[] old=Files.readAllBytes(p);var executor=Executors.newSingleThreadScheduledExecutor();Thread writer=Thread.currentThread();
  try(var held=FileChannel.open(p,StandardOpenOption.READ,ExtendedOpenOption.NOSHARE_DELETE)){
   executor.schedule(writer::interrupt,100,TimeUnit.MILLISECONDS);
   assertThrows(java.io.InterruptedIOException.class,()->WorldNativeCheckpoint.write(p,new JSONObject().put("revision",2)));
   assertTrue(Thread.currentThread().isInterrupted());assertArrayEquals(old,Files.readAllBytes(p));
  }finally{executor.shutdownNow();Thread.interrupted();}
 }
}
