package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

public class WorldLedgerTest {
    static JSONObject suite() throws Exception {try(var in=WorldLedgerTest.class.getResourceAsStream("/world-sequences035.json")){return new JSONObject(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));}}
    static JSONObject source(JSONObject c){return SpatialScene.parse(c.getJSONObject("source").toString(),UUID.nameUUIDFromBytes(c.getString("id").getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString());}
    static JSONObject args(JSONObject observed,JSONObject step){var a=new JSONObject();for(String k:List.of("worldId","expectedRevision","expectedHash"))a.put(k,observed.get(k));for(String k:List.of("targets","forceLimitN","torqueLimitNm"))a.put(k,step.get(k));return a;}
    static JSONObject settled(WorldNativeStore nativeStore,String id) throws Exception {for(int i=0;i<24000;i++){if(!nativeStore.status(id).equals("running"))return nativeStore.run(id);Thread.sleep(5);}throw new AssertionError("Native persistent task timed out");}
    /** Software-only view fixture for ledger unit tests; real Chromium evidence is tested separately. */
    static JSONObject receipt(JSONObject run){var frames=new JSONArray();for(Object raw:WorldReport.charts(run)){var c=(JSONObject)raw;frames.put(new JSONObject().put("mode",c.getString("mode")).put("points",c.getJSONArray("points")));}return WorldReport.verifyView(run,new JSONObject().put("visible",true).put("rows",WorldReport.table(run)).put("data",WorldReport.data(run)).put("frames",frames));}
    static JSONObject meta(){return new JSONObject().put("id",UUID.randomUUID().toString()).put("version",1).put("sha256","a".repeat(64));}
    static JSONObject execute(WorldLedger ledger,WorldNativeStore store,JSONObject observed,JSONObject step,boolean realtime) throws Exception {var args=args(observed,step);var plan=ledger.preview(args);assertTrue(plan.toString(),plan.getBoolean("passed"));String goal=UUID.randomUUID().toString(),run=UUID.randomUUID().toString();ledger.reserve(observed,goal,run,plan);store.start(run,goal,ledger.source(observed),plan,realtime,true);return settled(store,run);}
    static JSONObject runCase(JSONObject c,Path root) throws Exception {
        var ledger=new WorldLedger(root);var w=ledger.create(source(c),c.getString("id"));String worldId=w.getString("id");var results=new JSONArray();int committed=0;
        try(var store=new WorldNativeStore(root)){
            for(Object raw:c.getJSONArray("steps")){
                var step=(JSONObject)raw;var before=ledger.observe(worldId);String expected=step.getString("expected");var args=args(before,step);
                if(expected.equals("reject-before-actuation")){var plan=ledger.preview(args);assertFalse(plan.toString(),plan.getBoolean("passed"));assertFalse(ledger.observe(worldId).has("pending"));results.put(new JSONObject().put("status","rejected").put("plan",plan));continue;}
                var run=execute(ledger,store,before,step,false);Files.writeString(root.resolve(run.getString("id")+".json"),run.toString());var metrics=WorldVerify.verify(run);
                if(expected.equals("native-failure-no-commit")){assertFalse(metrics.toString(),metrics.getBoolean("passed"));var unchanged=ledger.observe(worldId);assertEquals(before.getInt("expectedRevision"),unchanged.getInt("expectedRevision"));assertEquals(before.getString("expectedHash"),unchanged.getString("expectedHash"));assertThrows(Exception.class,()->ledger.preview(args));results.put(new JSONObject().put("status","native-failed").put("metrics",metrics).put("runId",run.getString("id")));continue;}
                assertEquals(run.optString("reason"),"completed",run.getString("status"));assertTrue(metrics.toString(),metrics.getBoolean("passed"));var view=receipt(run);var report=meta();var revision=ledger.commit(before,run.getString("goalId"),run,view,report);assertEquals(++committed,revision.getInt("revision"));assertTrue(revision.similar(ledger.commit(before,run.getString("goalId"),run,view,report)));assertThrows(Exception.class,()->ledger.preview(args));
                var after=ledger.observe(worldId);assertFalse(after.has("pending"));assertTrue(WorldFrames.after(before.getJSONObject("frame"),run).similar(after.getJSONObject("frame")));results.put(new JSONObject().put("status","committed").put("revision",revision.getInt("revision")).put("hash",revision.getString("hash")).put("metrics",metrics).put("runId",run.getString("id")));
            }
        }
        var result=new JSONObject().put("caseId",c.getString("id")).put("world",ledger.read(worldId)).put("results",results);Files.writeString(root.resolve("summary.json"),result.toString(2));return result;
    }
    @Test public void developmentSequencesPreserveNativeBodiesAndOnlyCommitVerifiedOutcomes() throws Exception {Path root=Files.createTempDirectory("world035-development-");var results=new JSONArray();for(Object raw:suite().getJSONArray("cases")){var c=(JSONObject)raw;if(!c.getString("split").equals("development"))continue;results.put(runCase(c,root.resolve(c.getString("id"))));}Files.writeString(root.resolve("summary.json"),results.toString(2));System.out.println("WORLD035_DEVELOPMENT "+root);}
    @Test public void heldNativeMotionSurvivesColdContinuationWithoutLosingWorldOwnership() throws Exception {
        Path root=Files.createTempDirectory("world035-resume-");var c=suite().getJSONArray("cases").getJSONObject(1);var ledger=new WorldLedger(root);var w=ledger.create(source(c),"Actual angular continuation");var before=ledger.observe(w.getString("id"));var step=c.getJSONArray("steps").getJSONObject(0);var plan=ledger.preview(args(before,step));String goal=UUID.randomUUID().toString(),id=UUID.randomUUID().toString();ledger.reserve(before,goal,id,plan);JSONObject prefix;
        try(var store=new WorldNativeStore(root)){store.start(id,goal,ledger.source(before),plan,true,true);boolean rotating=false;for(int i=0;i<3000;i++){var snap=store.snapshot();if(snap.has("sample")&&snap.getJSONObject("sample").getJSONObject("controller").getInt("stage")==1&&WorldVerify.norm(snap.getJSONObject("sample").getJSONArray("bodies").getJSONObject(0).getJSONArray("angularVelocity"))>.2){rotating=true;break;}Thread.sleep(5);}assertTrue(rotating);store.pause();prefix=settled(store,id);assertEquals("paused",prefix.getString("status"));}
        ledger=new WorldLedger(root);assertEquals(goal,ledger.observe(w.getString("id")).getJSONObject("pending").getString("goalId"));
        try(var store=new WorldNativeStore(root)){store.resume(id,ledger.source(before),false);var run=settled(store,id);assertEquals(run.optString("reason"),"completed",run.getString("status"));for(int i=0;i<prefix.getJSONArray("samples").length();i++)assertTrue(prefix.getJSONArray("samples").getJSONObject(i).similar(run.getJSONArray("samples").getJSONObject(i)));var metrics=WorldVerify.verify(run);assertEquals(0,metrics.getDouble("maxRestoreVelocityError"),2e-6);var bad=WorldFrames.copy(run);bad.getJSONObject("initialState").getJSONArray("bodies").getJSONObject(0).getJSONArray("velocity").put(0,1);assertThrows(IllegalStateException.class,()->WorldVerify.verify(bad));ledger.commit(before,goal,run,receipt(run),meta());assertEquals(1,ledger.observe(w.getString("id")).getInt("expectedRevision"));}
    }
    @Test public void processIndependentCasCorruptionAndExplicitRestoreAreReal() throws Exception {Path root=Files.createTempDirectory("world035-cas-");var c=suite().getJSONArray("cases").getJSONObject(0);var a=new WorldLedger(root);var b=new WorldLedger(root);var w=a.create(source(c),"CAS test");var observed=a.observe(w.getString("id"));var plan=a.preview(args(observed,c.getJSONArray("steps").getJSONObject(0)));String goal=UUID.randomUUID().toString(),run=UUID.randomUUID().toString();a.reserve(observed,goal,run,plan);assertThrows(Exception.class,()->b.preview(args(observed,c.getJSONArray("steps").getJSONObject(0))));assertThrows(Exception.class,()->b.restore(w.getString("id"),0,observed.getString("expectedHash"),0,""));var restored=b.restore(w.getString("id"),0,observed.getString("expectedHash"),0,goal);assertEquals(1,restored.getInt("revision"));assertEquals("restore",restored.getString("kind"));assertTrue(restored.getJSONObject("frame").similar(observed.getJSONObject("frame")));assertThrows(Exception.class,()->a.reserve(observed,goal,run,plan));Path file=root.resolve("persistent-worlds/state.json");var edited=new JSONObject(Files.readString(file));edited.getJSONObject("payload").getJSONArray("worlds").getJSONObject(0).getJSONArray("revisions").getJSONObject(0).getJSONObject("frame").getJSONArray("bodies").getJSONObject(0).getJSONArray("position").put(0,10);String text=edited.toString();Files.writeString(file,text);assertThrows(Exception.class,()->new WorldLedger(root));assertEquals(text,Files.readString(file));}

    @Test public void growingBankWorldCapacityIsBoundedAndAtomic() throws Exception {
        Path root=Files.createTempDirectory("world085-capacity-");var ledger=new WorldLedger(root);var src=source(suite().getJSONArray("cases").getJSONObject(0));
        var first=ledger.create(src,"First retained world");String id=first.getString("id");var before=ledger.observe(id);
        for(int i=1;i<WorldLedger.MAX_WORLDS;i++)ledger.create(src,"Retained world "+i);
        Path file=root.resolve("persistent-worlds/state.json");byte[] bytes=Files.readAllBytes(file);
        assertEquals(32,new WorldLedger(root).list().length());assertTrue(before.similar(new WorldLedger(root).observe(id)));
        assertThrows(IllegalStateException.class,()->ledger.create(src,"Over capacity"));assertArrayEquals(bytes,Files.readAllBytes(file));
    }
    static void markLegacy084(Path root)throws Exception {
        Path file=root.resolve("persistent-worlds/state.json");var e=new JSONObject(Files.readString(file));var payload=e.getJSONObject("payload");payload.getJSONArray("worlds").getJSONObject(0).put("codeSha256",WorldLedger.LEGACY_084);e.put("sha256",WorldFrames.hash(payload));Files.writeString(file,e.toString());
    }
    @Test public void legacy084RequiresExplicitFramePreservingUpgrade() throws Exception {
        Path root=Files.createTempDirectory("world085-upgrade-");var c=suite().getJSONArray("cases").getJSONObject(0);var old=new WorldLedger(root);String id=old.create(source(c),"Earlier closed world").getString("id");var before=old.observe(id);markLegacy084(root);
        var ledger=new WorldLedger(root);assertThrows(IllegalStateException.class,()->ledger.preview(args(before,c.getJSONArray("steps").getJSONObject(0))));
        var binding=ledger.upgrade051(id,0,before.getString("expectedHash"));assertEquals(1,binding.getInt("expectedRevision"));var after=new WorldLedger(root).observe(id);assertTrue(before.getJSONObject("frame").similar(after.getJSONObject("frame")));
        var rev=ledger.read(id).getJSONArray("revisions").getJSONObject(1);assertEquals("upgrade085",rev.getString("kind"));assertEquals(WorldLedger.LEGACY_084,rev.getString("previousCodeSha256"));assertTrue(ledger.preview(args(after,c.getJSONArray("steps").getJSONObject(0))).getBoolean("passed"));
    }
    @Test public void legacy084PendingOwnerCannotBeUpgraded() throws Exception {
        Path root=Files.createTempDirectory("world085-owned-");var c=suite().getJSONArray("cases").getJSONObject(0);var ledger=new WorldLedger(root);String id=ledger.create(source(c),"Owned earlier world").getString("id");var before=ledger.observe(id);var plan=ledger.preview(args(before,c.getJSONArray("steps").getJSONObject(0)));ledger.reserve(before,UUID.randomUUID().toString(),UUID.randomUUID().toString(),plan);markLegacy084(root);
        Path file=root.resolve("persistent-worlds/state.json");byte[] bytes=Files.readAllBytes(file);var reopened=new WorldLedger(root);assertThrows(IllegalStateException.class,()->reopened.upgrade051(id,0,before.getString("expectedHash")));assertArrayEquals(bytes,Files.readAllBytes(file));
    }
}
