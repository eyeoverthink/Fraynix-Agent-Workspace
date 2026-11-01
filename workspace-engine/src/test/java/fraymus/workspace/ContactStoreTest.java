package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

public class ContactStoreTest {
    static JSONObject waitFor(ContactStore store,String id,String status) throws Exception {for(int n=0;n<3000;n++){var r=store.run(id);if(r.getString("status").equals(status))return r;if(r.getString("status").equals("interrupted")&&!status.equals("interrupted"))fail(r.optString("reason"));Thread.sleep(5);}throw new AssertionError(store.run(id));}
    static JSONObject start(ContactStore store,String variant,double initial,double max,boolean realtime) throws Exception {var scene=PushNativeTest.scene(variant);return store.start(UUID.randomUUID().toString(),UUID.randomUUID().toString(),scene,PushNativeTest.plan(scene,initial,max),realtime);}
    @Test public void storedNativeOutcomeReconstructsControlledContactAndWork() throws Exception {
        try(var store=new ContactStore(Files.createTempDirectory("contact033-store-"))){var r=start(store,"corner",8,32,false);r=waitFor(store,r.getString("id"),"completed");assertTrue(r.getJSONObject("verification").toString(),r.getJSONObject("verification").getBoolean("passed"));assertEquals(r.getJSONObject("final").getInt("tick"),r.getJSONObject("verification").getInt("nativeTicks"));assertTrue(r.getJSONObject("verification").getInt("plannedPushes")>3);
            assertTrue(ContactVerify.check(r).getBoolean("passed"));assertTrue(ContactVerify.check(r).getInt("solverContactSamples")>0);
            var wrongSensor=new JSONObject(r.toString());wrongSensor.getJSONArray("samples").getJSONObject(0).getJSONObject("nativeContact").put("method","fabricated geometry");assertThrows(IllegalStateException.class,()->ContactVerify.check(wrongSensor));
            var wrongCount=new JSONObject(r.toString());wrongCount.getJSONArray("samples").getJSONObject(0).put("contacts",100);assertThrows(IllegalStateException.class,()->ContactVerify.check(wrongCount));
            var bad=new JSONObject(r.toString());bad.getJSONArray("samples").getJSONObject(0).getJSONArray("integration").getJSONArray(0).put(1,r.getJSONObject("plan").getJSONObject("intent").getString("objectId"));assertThrows(IllegalStateException.class,()->PushVerify.verify(bad));
            var counter=new JSONObject(r.toString());counter.getJSONArray("samples").getJSONObject(0).put("effortNs",500);assertThrows(IllegalStateException.class,()->PushVerify.verify(counter));
            var frames=new JSONArray();for(Object raw:PushReport.charts(r)){var c=(JSONObject)raw;frames.put(new JSONObject().put("mode",c.getString("mode")).put("points",c.getJSONArray("points")));}
            var actual=new JSONObject(frames.getJSONObject(0).toString()).put("frames",frames).put("pushVisible",true).put("pushData",PushReport.data(r));assertTrue(PushReport.verifyView(r,actual).getBoolean("passed"));
            final var original=r;var wrongPoint=new JSONObject(actual.toString());wrongPoint.getJSONArray("frames").getJSONObject(5).getJSONArray("points").getJSONArray(0).put(1,900);assertThrows(IllegalStateException.class,()->PushReport.verifyView(original,wrongPoint));
            var wrongIntent=new JSONObject(actual.toString());wrongIntent.getJSONObject("pushData").getJSONObject("intent").put("maxForceN",60);assertThrows(IllegalStateException.class,()->PushReport.verifyView(original,wrongIntent));
        }
    }
    @Test public void bothMovingBodiesAndControllerContinueTheSameCommittedRun() throws Exception {
        var root=Files.createTempDirectory("contact033-resume-");String id;JSONObject saved;
        try(var store=new ContactStore(root)){var r=start(store,"open",8,32,true);id=r.getString("id");for(int n=0;n<1500;n++){var s=store.snapshot().optJSONObject("sample");if(s!=null&&s.getInt("contacts")>0&&s.getInt("tick")>=600){var b=s.getJSONArray("bodies");if(ObserverGeometry.norm(SpatialScene.vector(b.getJSONObject(0).getJSONArray("velocity"),100))>.05&&ObserverGeometry.norm(SpatialScene.vector(b.getJSONObject(1).getJSONArray("velocity"),100))>.05)break;}Thread.sleep(5);}store.pause();saved=waitFor(store,id,"paused");assertTrue(saved.getJSONObject("checkpoint").getInt("tick")>=600);}
        try(var store=new ContactStore(root)){store.resume(id,saved.getJSONObject("definition"),false);var r=waitFor(store,id,"completed");assertTrue(r.getJSONObject("verification").toString(),r.getJSONObject("verification").getBoolean("passed"));assertEquals(1,r.getJSONObject("verification").getInt("resumes"));assertEquals(0,r.getJSONObject("verification").getDouble("maxRestorePositionErrorM"),0);assertEquals(0,r.getJSONObject("verification").getDouble("maxRestoreVelocityError"),0);for(int i=0;i<saved.getJSONArray("samples").length();i++)assertTrue(saved.getJSONArray("samples").getJSONObject(i).similar(r.getJSONArray("samples").getJSONObject(i)));}
    }
    @Test public void interruptionDuringNativeRecoveryPreservesItsPhaseAndOneJam() throws Exception {
        var root=Files.createTempDirectory("contact033-recovery-phase-");String id;JSONObject saved;
        try(var store=new ContactStore(root)){var r=start(store,"high-friction",8,32,true);id=r.getString("id");boolean found=false;for(int n=0;n<1800;n++){var s=store.snapshot().optJSONObject("sample");if(s!=null&&!s.getJSONObject("controller").getString("recoveryMode").isEmpty()){found=true;break;}Thread.sleep(5);}assertTrue("Actual recovery phase was observed",found);store.pause();saved=waitFor(store,id,"paused");assertEquals(1,saved.getJSONArray("samples").getJSONObject(saved.getJSONArray("samples").length()-1).getJSONObject("controller").getInt("recoveries"));}
        try(var store=new ContactStore(root)){store.resume(id,saved.getJSONObject("definition"),false);var r=waitFor(store,id,"completed");assertTrue(r.getJSONObject("verification").toString(),r.getJSONObject("verification").getBoolean("passed"));assertEquals(1,r.getJSONObject("verification").getInt("jams"));assertEquals(1,r.getJSONObject("verification").getInt("recoveries"));assertEquals(1,r.getJSONObject("verification").getInt("resumes"));}
    }
    @Test public void insufficientOriginalForceCapFailsWithoutInventedSuccessOrEscalation() throws Exception {
        try(var store=new ContactStore(Files.createTempDirectory("contact033-insufficient-"))){var r=start(store,"high-friction",8,8,false);r=waitFor(store,r.getString("id"),"failed");assertFalse(r.getJSONObject("verification").getBoolean("passed"));assertTrue(r.getJSONObject("verification").getDouble("maxForceN")<=8.0001);assertEquals(2,r.getJSONObject("verification").getInt("recoveries"));final String id=r.getString("id");final JSONObject scene=r.getJSONObject("definition");assertThrows(IllegalStateException.class,()->store.resume(id,scene,false));}
    }
    @Test public void corruptOrIncompatibleCheckpointIsRefusedAndPreserved() throws Exception {
        var root=Files.createTempDirectory("contact033-corrupt-");String id;JSONObject scene;
        try(var store=new ContactStore(root)){var r=start(store,"open",8,32,true);id=r.getString("id");scene=r.getJSONObject("definition");Thread.sleep(200);store.pause();waitFor(store,id,"paused");}
        var file=root.resolve("contact-pushing/checkpoints/"+id+".json");var edited=new JSONObject(Files.readString(file));edited.getJSONObject("payload").getJSONObject("physics").getJSONArray("bodies").getJSONObject(0).getJSONArray("velocity").put(0,7);String text=edited.toString();Files.writeString(file,text);
        try(var store=new ContactStore(root)){assertThrows(IllegalStateException.class,()->store.resume(id,scene,false));assertNotEquals("running",store.status(id));}assertEquals(text,Files.readString(file));
        var payload=edited.getJSONObject("payload").put("codeSha256","f".repeat(64));ContactCheckpoint.write(file,payload);assertThrows(IllegalStateException.class,()->ContactCheckpoint.read(file,id,scene));
    }
    @Test public void original030MapCheckpointImplementationSourcesRemainByteIdentical() throws Exception {
        Path repo=Path.of("").toAbsolutePath();while(repo!=null&&!Files.exists(repo.resolve("docs/readmes/030/file-inventory.json")))repo=repo.getParent();assertNotNull(repo);var inventory=new JSONArray(Files.readString(repo.resolve("docs/readmes/030/file-inventory.json")));
        for(String file:List.of("MapCheckpoint.java","MapPhysics.java","MapStore.java","MapDefinition.java","MapWork.java","MapRecovery.java","MapMaterials.java")){
            String name="workspace-engine/src/main/java/fraymus/workspace/"+file;JSONObject entry=null;for(Object raw:inventory)if(((JSONObject)raw).getString("path").equals(name))entry=(JSONObject)raw;assertNotNull(entry);assertEquals(file,entry.getString("sha256"),MapDefinition.sha(Files.readString(repo.resolve(name)).replace("\r\n","\n").getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        }
    }
}
