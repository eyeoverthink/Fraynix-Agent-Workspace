package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.UUID;

public class Route055RecoveryTest {
    @Test public void lateralWaypointRestoresAfterColdRestart() throws Exception {
        Path root=Files.createTempDirectory("route055-recovery-");
        var worlds=new WorldLedger(root);var input=Construction054Test.input(worlds,"detour");
        var scene=worlds.source(worlds.observe(input.getString("worldId")));var plan=worlds.preview(input);
        String id=UUID.randomUUID().toString();int pausedTick;
        try(var store=new WorldNativeStore(root)) {
            store.start(id,UUID.randomUUID().toString(),scene,plan,true,true);
            long deadline=System.currentTimeMillis()+30000;JSONObject controller;
            do {Thread.sleep(20);controller=store.snapshot().optJSONObject("sample",new JSONObject()).optJSONObject("controller",new JSONObject());}
            while((controller.optInt("stage")<3||controller.optJSONArray("stages",new JSONArray()).length()<=4)&&System.currentTimeMillis()<deadline);
            assertTrue("Reach an actual intermediate lateral route stage",controller.optInt("stage")>=3&&controller.optJSONArray("stages",new JSONArray()).length()>4);
            store.pause();while(store.status(id).equals("running")&&System.currentTimeMillis()<deadline)Thread.sleep(20);
            assertEquals("paused",store.status(id));pausedTick=store.run(id).getJSONObject("checkpoint").getInt("tick");
        }
        try(var store=new WorldNativeStore(root)) {
            store.resume(id,scene,false);long deadline=System.currentTimeMillis()+45000;
            while(store.status(id).equals("running")&&System.currentTimeMillis()<deadline)Thread.sleep(20);
            var run=store.run(id);Files.writeString(root.resolve("result.json"),WorldReport.packed(run).toString());
            assertEquals(run.optString("reason"),"completed",run.getString("status"));
            var metrics=WorldVerify.verify(run);assertTrue(metrics.toString(),metrics.getBoolean("passed"));assertEquals(1,metrics.getInt("resumes"));assertEquals(0,metrics.getDouble("maxRestoreVelocityError"),2e-6);
            var structure=Construction051.verifyStructure(run);assertTrue(structure.toString(),structure.getBoolean("passed"));assertEquals(0,structure.getInt("obstacleContacts"));assertTrue(run.getJSONObject("final").getInt("tick")>pausedTick);
        }
        System.out.println("ROUTE055_RECOVERY "+root);
    }
}
