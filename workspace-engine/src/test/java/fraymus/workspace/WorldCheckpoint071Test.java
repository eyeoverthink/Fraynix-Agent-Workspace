package fraymus.workspace;

import fraymus.factory.*;
import org.junit.Test;
import org.junit.Assume;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import com.sun.nio.file.ExtendedOpenOption;
import java.util.List;
import java.util.concurrent.*;

/** Actual Windows deny-delete handles against the binary world checkpoint. */
public class WorldCheckpoint071Test {
 static final SchemaRegistry SCHEMAS=new SchemaRegistry();
 static WorldSnapshot snapshot(long revision){return new WorldSnapshot(revision,List.of());}
 static WorldCheckpointStore store()throws Exception{
  Assume.assumeTrue(System.getProperty("os.name").startsWith("Windows"));
  var store=new WorldCheckpointStore(Files.createTempDirectory("world-checkpoint071-").resolve("tasks.fworld"));
  store.save(snapshot(1),SCHEMAS);return store;
 }
 @Test public void transientReaderCommitsExactlyOnceAndKeepsPrevious()throws Exception{
  var store=store();var executor=Executors.newSingleThreadScheduledExecutor();
  try(var held=FileChannel.open(store.path(),StandardOpenOption.READ,ExtendedOpenOption.NOSHARE_DELETE)){
   var release=executor.schedule(()->{try{held.close();}catch(Exception e){throw new RuntimeException(e);}},150,TimeUnit.MILLISECONDS);
   store.save(snapshot(2),SCHEMAS);release.get();assertEquals(2,store.load(SCHEMAS).snapshot().revision());
   assertEquals(1,new WorldCheckpointStore(store.path().resolveSibling("tasks.fworld.previous")).load(SCHEMAS).snapshot().revision());
   try(var paths=Files.list(store.path().getParent())){assertEquals(0,paths.filter(p->p.toString().endsWith(".pending")).count());}
  }finally{executor.shutdownNow();}
 }
 @Test public void prolongedLockKeepsCurrentPreviousAndRecoverablePending()throws Exception{
  var store=store();byte[] before=Files.readAllBytes(store.path());long started=System.nanoTime();
  try(var held=FileChannel.open(store.path(),StandardOpenOption.READ,ExtendedOpenOption.NOSHARE_DELETE)){
   assertThrows(AccessDeniedException.class,()->store.save(snapshot(2),SCHEMAS));
   assertTrue(System.nanoTime()-started>=700_000_000L);assertArrayEquals(before,Files.readAllBytes(store.path()));
   assertEquals(1,store.load(SCHEMAS).snapshot().revision());
   try(var paths=Files.list(store.path().getParent())){var pending=paths.filter(p->p.toString().endsWith(".pending")).findFirst().orElseThrow();assertEquals(2,new WorldCheckpointStore(pending).load(SCHEMAS).snapshot().revision());}
  }
 }
 @Test public void interruptedRetryKeepsInterruptAndOldGeneration()throws Exception{
  var store=store();byte[] before=Files.readAllBytes(store.path());var failure=new java.util.concurrent.atomic.AtomicReference<Throwable>();var interrupted=new java.util.concurrent.atomic.AtomicBoolean();
  var writer=new Thread(()->{try{store.save(snapshot(2),SCHEMAS);}catch(Throwable e){failure.set(e);}finally{interrupted.set(Thread.currentThread().isInterrupted());}},"world-retry071");
  try(var held=FileChannel.open(store.path(),StandardOpenOption.READ,ExtendedOpenOption.NOSHARE_DELETE)){
   writer.start();long end=System.nanoTime()+5_000_000_000L;
   while(writer.isAlive()&&writer.getState()!=Thread.State.TIMED_WAITING&&System.nanoTime()<end)Thread.sleep(1);
   assertEquals(Thread.State.TIMED_WAITING,writer.getState());writer.interrupt();writer.join(3000);assertFalse(writer.isAlive());
   assertTrue(String.valueOf(failure.get()),failure.get() instanceof java.io.InterruptedIOException);assertTrue(interrupted.get());assertArrayEquals(before,Files.readAllBytes(store.path()));
  }finally{if(writer.isAlive()){writer.interrupt();writer.join(3000);}}
 }
}
