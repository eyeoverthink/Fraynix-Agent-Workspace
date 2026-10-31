package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

public class ProceduralTest {
    /** These typed host observations are unit fixtures. Real pages are exercised separately by Electron. */
    static final class Fixture implements AutoCloseable {
        final Path root;final WorkspaceStore store;final SceneStore scenes;final BrainService brain;final GoalRunner goals;
        Fixture(Path root) throws Exception {this.root=root;store=new WorkspaceStore(root);scenes=new SceneStore(root);brain=new BrainService(root);goals=new GoalRunner(root,scenes,brain,store);}
        JSONObject input(String type,int dimension,int seed){return new JSONObject().put("title","Procedural unit fixture").put("request","Generate and verify original coordinates").put("definition",new JSONObject().put("type",type).put("dimensions",dimension).put("seed",seed));}
        @Override public void close() throws Exception {goals.close();brain.close();scenes.close();store.close();}
    }
    static JSONObject effect(Fixture f) throws Exception {for(int i=0;i<4000;i++){var e=f.goals.claim();if(e.has("id"))return e;Thread.sleep(5);}throw new AssertionError("No procedural host effect");}
    static JSONObject observation(JSONObject g){var points=Procedural.projections(Procedural.data(g)).getJSONObject(0).getJSONArray("points");var actual=new JSONArray();for(int i=0;i<points.length();i++)actual.put(new JSONObject().put("index",i).put("x",points.getJSONArray(i).getDouble(0)).put("y",points.getJSONArray(i).getDouble(1)));return new JSONObject().put("points",actual).put("angle","0°").put("slice","All");}
    static JSONObject ack(Fixture f,JSONObject e,JSONObject view) throws Exception {
        var checks=new JSONArray();for(int i=0;i<e.getJSONArray("steps").length();i++){var s=e.getJSONArray("steps").getJSONObject(i);checks.put(new JSONObject().put("index",i).put("selector",s.getString("selector")).put("passed",true).put("actual",new JSONObject().put("text",s.optString("expectText")).put("value",s.optString("expectValue")).put("count",s.optInt("expectCount",1))));}
        var ref=e.getJSONObject("report");var receipt=f.store.recordTest(new JSONObject().put("id",ref.getString("id")).put("version",ref.getInt("version")).put("steps",e.getJSONArray("steps")).put("checks",checks));return f.goals.acknowledge(new JSONObject().put("goalId",e.getString("goalId")).put("id",e.getString("id")).put("nonce",e.getString("nonce")).put("result",new JSONObject(receipt.toString()).put("proceduralView",view)));
    }
    static JSONObject completed(Fixture f,String id) throws Exception {for(int i=0;i<1000;i++){var g=f.goals.read(id);if(!Set.of("running","waiting-host").contains(g.getString("status")))return g;Thread.sleep(5);}throw new AssertionError("Procedural goal did not settle");}
    static JSONObject run(Fixture f,JSONObject input) throws Exception {String id=f.goals.startProcedural(input).getString("id");var e=effect(f);ack(f,e,observation(f.goals.read(id)));return completed(f,id);}
    @Test public void invalidInputsDoNotInstallHandlersOrCaptureTheRunner() throws Exception {
        try(var f=new Fixture(Files.createTempDirectory("procedural-invalid"))){assertThrows(IllegalArgumentException.class,()->f.goals.startProcedural(f.input("dungeon",16,1)));assertThrows(IllegalArgumentException.class,()->f.goals.startProcedural(f.input("colonization",3,1).put("handler","shell")));assertThrows(IllegalArgumentException.class,()->f.goals.startProcedural(f.input("colonization",3,1).put("definition",new JSONObject().put("seed",.5))));assertEquals(0,f.goals.list().length());assertFalse(f.goals.busy());}
    }
    @Test public void realJavaResultAndTypedViewFixtureRetainThenRebindANewSeed() throws Exception {
        try(var f=new Fixture(Files.createTempDirectory("procedural-skill"))){var a=run(f,f.input("dungeon",4,271).put("saveSkillName","unit-map"));assertEquals("completed",a.getString("status"));assertTrue(a.getJSONObject("metrics").getBoolean("connected"));assertTrue(a.getJSONObject("proceduralView").getBoolean("passed"));var data=Procedural.data(a);assertEquals(4,data.getJSONObject("definition").getInt("dimensions"));assertEquals(1,f.goals.procedures().length());var b=f.goals.runProcedure(new JSONObject().put("name","unit-map").put("version",1).put("seed",272));var e=effect(f);ack(f,e,observation(f.goals.read(b.getString("id"))));b=completed(f,b.getString("id"));assertEquals("completed",b.getString("status"));assertEquals(272,b.getJSONObject("proceduralDefinition").getInt("seed"));assertNotEquals(a.getString("proceduralSha256"),b.getString("proceduralSha256"));assertEquals(a.getJSONObject("savedSkill").getString("id"),b.getJSONObject("skill").getString("id"));}
    }
    @Test public void forgedDisplayedCoordinatesCannotCompleteOrRetainASkill() throws Exception {
        try(var f=new Fixture(Files.createTempDirectory("procedural-pixels"))){String id=f.goals.startProcedural(f.input("colonization",3,270).put("saveSkillName","not-retained")).getString("id");var e=effect(f);var actual=observation(f.goals.read(id));actual.getJSONArray("points").getJSONObject(0).put("x",12345);assertThrows(IllegalStateException.class,()->ack(f,e,actual));assertEquals(0,f.goals.procedures().length());f.goals.cancel(id);}
    }
    @Test public void cancellationRejectsLateEffectsAndDoesNotCreateAProcedure() throws Exception {
        try(var f=new Fixture(Files.createTempDirectory("procedural-cancel"))){String id=f.goals.startProcedural(f.input("colonization",16,271).put("saveSkillName","cancelled-space")).getString("id");f.goals.cancel(id);Thread.sleep(100);assertEquals("cancelled",f.goals.read(id).getString("status"));assertEquals(0,f.goals.procedures().length());assertEquals(0,f.store.artifacts().length());var g=run(f,f.input("dungeon",2,282));assertEquals("completed",g.getString("status"));}
    }
    @Test public void restartReusesStableReportAndReconcilesPassingViewWithoutReplayingControls() throws Exception {
        Path root=Files.createTempDirectory("procedural-restart");String id,artifact;
        try(var f=new Fixture(root)){id=f.goals.startProcedural(f.input("colonization",4,273).put("saveSkillName","recovered-space")).getString("id");var e=effect(f);artifact=e.getJSONObject("report").getString("id");synchronized(f.goals){ack(f,e,observation(f.goals.read(id)));f.goals.close();}}
        try(var f=new Fixture(root)){assertEquals("interrupted",f.goals.read(id).getString("status"));f.goals.resume(id);var g=completed(f,id);assertEquals("completed",g.getString("status"));assertEquals(artifact,g.getJSONObject("report").getString("id"));assertEquals(1,f.store.read(artifact).getInt("version"));assertEquals(1,f.goals.procedures().length());}
    }
    @Test public void realExportImportsByReconstructionAndRejectsChangedPayload() throws Exception {
        try(var f=new Fixture(Files.createTempDirectory("procedural-package"))){var g=run(f,f.input("colonization",16,274));var exported=f.goals.exportProcedural(g.getString("id"));var file=Path.of(exported.getString("path"));assertTrue(file.startsWith(f.root));String text=Files.readString(file);assertEquals(MeasurementStore.hash(text),exported.getString("sha256"));var pkg=new JSONObject(text);var imported=f.goals.importProcedural(new JSONObject().put("package",pkg));var e=effect(f);ack(f,e,observation(f.goals.read(imported.getString("id"))));imported=completed(f,imported.getString("id"));assertEquals("completed",imported.getString("status"));assertEquals(g.getString("proceduralSha256"),imported.getString("proceduralSha256"));pkg.getJSONObject("data").getJSONArray("nodes").getJSONArray(0).put(0,999);assertThrows(IllegalArgumentException.class,()->f.goals.importProcedural(new JSONObject().put("package",pkg)));Files.writeString(file,"User edit retained");assertThrows(IllegalStateException.class,()->f.goals.exportProcedural(g.getString("id")));assertEquals("User edit retained",Files.readString(file));}
    }
    @Test public void editedReportsArePreservedAndCannotReuseOldReceipts() throws Exception {
        try(var f=new Fixture(Files.createTempDirectory("procedural-report-edit"))){var g=run(f,f.input("dungeon",3,275));var ref=g.getJSONObject("report");f.store.write(new JSONObject().put("id",ref.getString("id")).put("expectedVersion",1).put("title","User-edited report").put("files",new JSONObject().put("index.html","<html><body>User content</body></html>")));assertThrows(IllegalStateException.class,()->f.goals.saveProcedure(g.getString("id"),"edited-map"));assertEquals(2,f.store.read(ref.getString("id")).getInt("version"));assertEquals("User-edited report",f.store.read(ref.getString("id")).getString("title"));}
    }
}
