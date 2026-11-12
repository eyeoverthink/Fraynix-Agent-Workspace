package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.concurrent.*;

public class Load055Test {
    @Test public void stopPreservesPartialEvidenceThenExplicitRestartReevaluatesAndRejectsTampering()throws Exception {
        var root=Files.createTempDirectory("load055-recovery-");var worlds=new WorldLedger(root);var input=Construction054Test.input(worlds,"wide");
        var source=worlds.source(worlds.observe(input.getString("worldId")));var run=Construction054Test.nativeRun(root.resolve("native-source"),source,worlds.preview(input));
        var folder=root.resolve("loads");var file=folder.resolve(run.getString("id")+".json");var worker=Executors.newSingleThreadExecutor();
        var checkpoint=new CountDownLatch(1);var release=new CountDownLatch(1);
        try {var future=worker.submit(()->Load055.evaluate(folder,run,()->{checkpoint.countDown();release.await();return null;}));
            assertTrue("Native partial checkpoint must be reached",checkpoint.await(60,TimeUnit.SECONDS));
            var partial=AgentFiles042.loadChecked(file).getJSONArray("attempts").getJSONObject(0);
            assertEquals("running",partial.getString("status"));assertTrue(partial.getJSONArray("rows").length()>0);
            assertTrue("Stop must reach the checkpointed live experiment",future.cancel(true));}
        finally {worker.shutdown();if(!worker.awaitTermination(20,TimeUnit.SECONDS)){worker.shutdownNow();fail("Cancelled native experiment did not terminate");}}
        var stopped=AgentFiles042.loadChecked(file);Files.writeString(root.resolve("original-stopped.json"),stopped.toString());assertTrue(stopped.getJSONArray("attempts").toList().stream().anyMatch(v->new JSONObject((java.util.Map<?,?>)v).getString("status").equals("interrupted")));
        var results=Load055.evaluate(folder,run);assertTrue(results.getBoolean("complete"));assertTrue(results.getInt("attempts")>3);var original=AgentFiles042.loadChecked(file);Files.writeString(root.resolve("original-resumed.json"),original.toString());
        var completed=original.getJSONArray("attempts").getJSONObject(original.getJSONArray("attempts").length()-1);var spec=Load055.protocol().getJSONArray("cases").getJSONObject(2);assertFalse(Load055.verifyAttempt(completed,run,spec).getBoolean("stable"));
        var forceChanged=WorldFrames.copy(completed);forceChanged.getJSONArray("rows").getJSONArray(0).getJSONArray(1).put(0,1);assertThrows(IllegalStateException.class,()->Load055.verifyAttempt(forceChanged,run,spec));
        var gap=WorldFrames.copy(completed);gap.getJSONArray("rows").remove(10);assertThrows(IllegalStateException.class,()->Load055.verifyAttempt(gap,run,spec));
        var forged=WorldFrames.copy(completed);forged.getJSONObject("initial").put("tick",1);assertThrows(IllegalStateException.class,()->Load055.verifyAttempt(forged,run,spec));
        Files.writeString(root.resolve("result.json"),results.toString());System.out.println("LOAD055_RECOVERY "+root);
    }
}
