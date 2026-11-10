package fraymus.workspace;

import fraymus.factory.*;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

/** Lifecycle and corruption fixtures. Actual browser outcomes are covered by archive110-integration. */
public class Archive110Test {
    static JSONObject completed(WorkflowTest.Fixture f)throws Exception{
        var w=Qualification105Test.finish(f,Qualification105Test.start(f,"archive-source").getString("id"));
        assertEquals(w.toString(),"completed",w.getString("status"));return w;
    }
    static JSONObject request(WorkflowTest.Fixture f){var h=f.workflows.history110();var ids=new JSONArray();for(Object item:h.getJSONArray("records")){var row=(JSONObject)item;if(row.getBoolean("eligible"))ids.put(row.getString("id"));}return new JSONObject().put("planSha256",h.getString("planSha256")).put("ids",ids);}
    static Map<String,String> payloads(Path root)throws Exception{
        var schemas=new SchemaRegistry();schemas.register(new ComponentSchema("workspace.workflow",1,Map.of("payload",ComponentSchema.Kind.TEXT),Set.of("payload")));
        var out=new TreeMap<String,String>();for(var e:new WorldCheckpointStore(root.resolve("workflows/tasks.fworld")).load(schemas).snapshot().entities())out.put(e.id().toString(),e.components().getFirst().values().get("payload"));return out;
    }
    static Path archived(Path root,JSONObject view){return root.resolve("workflows/archive110/"+view.getJSONObject("archive110").getString("sha256")+".json");}
    @Test public void exactBytesAndStableIdsSurviveRestartAndFreshQualification()throws Exception{
        Path root=Files.createTempDirectory("archive110-exact-");String parent;Map<String,String> originals;
        try(var f=new WorkflowTest.Fixture(root)){
            var w=completed(f);parent=w.getString("id");originals=payloads(root);var bank=f.goals.procedures();int goals=f.goals.list().length();
            var receipt=f.workflows.archive110(request(f));assertEquals(7,receipt.getJSONArray("archivedNow").length());assertEquals(0,f.workflows.list().length());assertEquals(7,receipt.getInt("archived"));assertEquals(goals,f.goals.list().length());assertTrue(bank.similar(f.goals.procedures()));
            for(var entry:originals.entrySet()){var view=f.workflows.read(entry.getKey());assertEquals(entry.getValue(),Files.readString(archived(root,view)));assertTrue(f.workflows.verifyArchive110(entry.getKey()).getBoolean("passed"));assertThrows(IllegalStateException.class,()->f.workflows.resume(entry.getKey()));assertThrows(IllegalStateException.class,()->f.workflows.cancel(entry.getKey()));}
        }
        try(var f=new WorkflowTest.Fixture(root)){
            assertTrue(f.workflows.read(parent).has("archive110"));assertEquals(0,f.workflows.list().length());
            var next=Qualification105Test.finish(f,Qualification105Test.start(f,"archive-next").getString("id"));assertEquals(next.toString(),"completed",next.getString("status"));assertEquals(5,next.getJSONObject("metrics").getInt("reusedExamples"));assertEquals(0,next.getJSONObject("metrics").getInt("createdExamples"));assertEquals(2,f.workflows.list().length());assertEquals(2,f.goals.procedures().length());assertEquals(14,f.goals.list().length());
            for(var entry:originals.entrySet())assertEquals(entry.getValue(),Files.readString(archived(root,f.workflows.read(entry.getKey()))));
        }
    }
    @Test public void missingAndChangedArchiveRefuseWithoutFallbackOrNewTask()throws Exception{
        Path root=Files.createTempDirectory("archive110-corrupt-");String id;byte[] original;Path file;
        try(var f=new WorkflowTest.Fixture(root)){
            id=completed(f).getString("id");f.workflows.archive110(request(f));file=archived(root,f.workflows.read(id));original=Files.readAllBytes(file);byte[] checkpoint=Files.readAllBytes(root.resolve("workflows/tasks.fworld"));
            Files.writeString(file,"{}");assertThrows(IllegalStateException.class,()->f.workflows.read(id));assertThrows(IllegalStateException.class,()->Qualification105Test.start(f,"bad-archive"));assertArrayEquals(checkpoint,Files.readAllBytes(root.resolve("workflows/tasks.fworld")));
            Files.delete(file);assertThrows(IllegalStateException.class,()->f.workflows.read(id));Files.write(file,original);assertTrue(f.workflows.verifyArchive110(id).getBoolean("passed"));
        }
        Files.delete(file);assertThrows(IllegalStateException.class,()->{try(var f=new WorkflowTest.Fixture(root)){fail("Missing archive was ignored on restart");}});Files.write(file,original);
        try(var f=new WorkflowTest.Fixture(root)){assertTrue(f.workflows.verifyArchive110(id).getBoolean("passed"));}
    }
    @Test public void changedReportStillRefusesAfterArchive()throws Exception{
        try(var f=new WorkflowTest.Fixture(Files.createTempDirectory("archive110-report-"))){var w=completed(f);String id=w.getString("id"),child=w.getJSONArray("stages").getJSONObject(0).getString("workflowId");f.workflows.archive110(request(f));var ref=f.workflows.read(child).getJSONObject("report");f.artifacts.write(new JSONObject().put("id",ref.getString("id")).put("expectedVersion",1).put("title","Altered retained report").put("files",new JSONObject().put("index.html","<html><body>Changed original</body></html>")));assertThrows(IllegalStateException.class,()->f.workflows.verifyArchive110(id));assertThrows(IllegalStateException.class,()->Qualification105Test.start(f,"changed-report"));assertEquals(1,f.goals.procedures().length());assertEquals(0,f.workflows.list().length());}
    }
    @Test public void changedDatasetStillRefusesAfterArchive()throws Exception{
        try(var f=new WorkflowTest.Fixture(Files.createTempDirectory("archive110-data-"))){var w=completed(f);String id=w.getString("id");var caseRow=f.workflows.read(w.getJSONArray("stages").getJSONObject(0).getString("workflowId"));f.workflows.archive110(request(f));var d=f.data.read(caseRow.getJSONObject("dataBinding").getString("datasetId"),1);var update=new JSONObject();for(String key:List.of("title","filename","csv","sourceKind","sourceNote","missingPolicy","mapping"))update.put(key,d.get(key));update.put("id",d.getString("datasetId")).put("expectedVersion",1);f.data.importCsv(update);assertThrows(IllegalStateException.class,()->f.workflows.verifyArchive110(id));assertThrows(IllegalStateException.class,()->Qualification105Test.start(f,"changed-data"));assertEquals(1,f.goals.procedures().length());}
    }
    @Test public void stalePreviewAndInvalidIdsNeverChangeCheckpoint()throws Exception{
        try(var f=new WorkflowTest.Fixture(Files.createTempDirectory("archive110-preview-"))){completed(f);var in=request(f);byte[] before=Files.readAllBytes(f.root.resolve("workflows/tasks.fworld"));final var bad=WorldFrames.copy(in).put("planSha256","0".repeat(64));assertThrows(IllegalStateException.class,()->f.workflows.archive110(bad));final var foreign=WorldFrames.copy(in).put("ids",new JSONArray().put(UUID.randomUUID().toString()));assertThrows(IllegalArgumentException.class,()->f.workflows.archive110(foreign));final var duplicate=WorldFrames.copy(in).put("ids",new JSONArray().put(in.getJSONArray("ids").getString(0)).put(in.getJSONArray("ids").getString(0)));assertThrows(IllegalArgumentException.class,()->f.workflows.archive110(duplicate));final var extra=WorldFrames.copy(in).put("force",true);assertThrows(IllegalArgumentException.class,()->f.workflows.archive110(extra));assertArrayEquals(before,Files.readAllBytes(f.root.resolve("workflows/tasks.fworld")));f.workflows.archive110(in);assertThrows(IllegalStateException.class,()->f.workflows.archive110(in));}
    }
    @Test public void activeInterruptedAndCancelledWorkCannotBeArchived()throws Exception{
        try(var f=new WorkflowTest.Fixture(Files.createTempDirectory("archive110-active-"))){String id=Qualification105Test.start(f,"incomplete-task").getString("id");var h=f.workflows.history110();var in=new JSONObject().put("planSha256",h.getString("planSha256")).put("ids",new JSONArray().put(id));assertThrows(IllegalStateException.class,()->f.workflows.archive110(in));f.workflows.cancel(id);in.put("planSha256",f.workflows.history110().getString("planSha256"));assertThrows(IllegalStateException.class,()->f.workflows.archive110(in));assertFalse(f.workflows.read(id).has("archive110"));}
        Path root=Files.createTempDirectory("archive110-interrupted-");String id;try(var f=new WorkflowTest.Fixture(root)){id=Qualification105Test.start(f,"restart-pending").getString("id");}
        try(var f=new WorkflowTest.Fixture(root)){assertEquals("interrupted",f.workflows.read(id).getString("status"));final String key=id;assertThrows(IllegalStateException.class,()->f.workflows.archive110(new JSONObject().put("planSha256",f.workflows.history110().getString("planSha256")).put("ids",new JSONArray().put(key))));f.workflows.cancel(id);}
    }
    @Test public void preparedArchivesWithoutCheckpointCommitKeepAllActiveOriginals()throws Exception{
        Path root=Files.createTempDirectory("archive110-atomic-");Map<String,String> originals;
        try(var f=new WorkflowTest.Fixture(root)){
            completed(f);originals=payloads(root);Path previous=root.resolve("workflows/tasks.fworld.previous"),backup=root.resolve("workflows/pre-archive.backup");Files.move(previous,backup);Files.createDirectory(previous);Files.writeString(previous.resolve("prevent-replacement"),"Explicit filesystem-failure fixture");byte[] before=Files.readAllBytes(root.resolve("workflows/tasks.fworld"));
            assertThrows(Exception.class,()->f.workflows.archive110(request(f)));assertArrayEquals(before,Files.readAllBytes(root.resolve("workflows/tasks.fworld")));assertEquals(7,f.workflows.list().length());assertEquals(originals,payloads(root));
            Files.delete(previous.resolve("prevent-replacement"));Files.delete(previous);Files.move(backup,previous);
        }
        try(var f=new WorkflowTest.Fixture(root)){assertEquals(7,f.workflows.list().length());var receipt=f.workflows.archive110(request(f));assertEquals(7,receipt.getInt("archived"));for(var entry:originals.entrySet())assertEquals(entry.getValue(),Files.readString(archived(root,f.workflows.read(entry.getKey()))));}
    }
    @Test public void qualifiedImplementationReusesArchivedSourceWithFreshAudit()throws Exception{
        try(var f=new WorkflowTest.Fixture(Files.createTempDirectory("archive110-reuse-"))){var parent=completed(f);var selected=f.workflows.read(parent.getJSONArray("stages").getJSONObject(5).getString("workflowId"));f.workflows.archive110(request(f));var data=f.source("t,x\n0,0.3\n0.5,0.2\n1,0.6\n");var w=f.workflows.reuse(Implementation103Test.args(selected,data));assertTrue(w.has("implementation103"));var effect=DataSummary104Test.audit(f,w.getString("id"));DataCreation101Test.ack(f,effect,DataSummary102Test.observed(f,effect,data));assertEquals("completed",DataCreation101Test.finish(f,w.getString("id")).getString("status"));assertTrue(f.workflows.verifyArchive110(parent.getString("id")).getBoolean("passed"));}
    }
}
