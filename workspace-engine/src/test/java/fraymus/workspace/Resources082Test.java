package fraymus.workspace;
import org.junit.Test;
import static org.junit.Assert.*;
public class Resources082Test {
 @Test public void readsActualVmWithoutInventingLimits(){
  var a=Resources082.snapshot();assertEquals(ProcessHandle.current().pid(),a.getLong("pid"));
  assertEquals(Runtime.getRuntime().maxMemory(),a.getJSONObject("profile").getLong("maxHeap"));
  assertEquals(Runtime.getRuntime().availableProcessors(),a.getJSONObject("profile").getInt("cpus"));
  assertTrue(a.getLong("heapUsedBytes")>=0);assertTrue(a.getLong("heapCommittedBytes")>=a.getLong("heapUsedBytes"));
  assertTrue(a.getLong("processCpuNanos")>=-1);assertTrue(a.getInt("liveThreads")>0);
  assertTrue(Resources082.snapshot().getLong("uptimeMillis")>=a.getLong("uptimeMillis"));
 }
}
