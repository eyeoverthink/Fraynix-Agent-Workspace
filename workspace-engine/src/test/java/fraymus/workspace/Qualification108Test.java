package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Actual archived107 source proofs;111 explicitly audits its pinned identity before fresh cases.
 * New host replies remain declared lifecycle fixtures. Original source/outcome and refusal assertions retained. */
public class Qualification108Test {
    static Path original()throws Exception{
        Path repo=Path.of("").toAbsolutePath();while(!Files.exists(repo.resolve("README-107.md")))repo=repo.getParent();
        Path root=Files.createTempDirectory("qualification108-original");String prefix="build/restart107/bank-1/";int count=0;
        var index=new JSONObject(Files.readString(repo.resolve("docs/readmes/107/original-evidence.json")));
        for(Object row:index.getJSONArray("files")){var f=(JSONObject)row;String logical=f.getString("logicalPath");if(!logical.startsWith(prefix))continue;
            byte[] raw;try(var in=new GZIPInputStream(Files.newInputStream(repo.resolve(f.getString("path"))))){raw=in.readAllBytes();}
            assertEquals(f.getString("rawSha256"),HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw)));Path out=root.resolve(logical.substring(prefix.length())).normalize();assertTrue(out.startsWith(root));Files.createDirectories(out.getParent());Files.write(out,raw);count++;
        }assertEquals(108,count);return root;
    }
    static JSONObject input(WorkflowTest.Fixture f)throws Exception{return Qualification105Test.input(f,f.source("t,x\n0,0\n0.125,-0.004\n"),"upgraded108").put("transitionQualification108",true);}
    static JSONObject parent(WorkflowTest.Fixture f){for(Object raw:f.workflows.list()){var w=(JSONObject)raw;if(w.has("qualification105"))return w;}throw new AssertionError("Original supervisor missing");}
    static JSONObject first(WorkflowTest.Fixture f)throws Exception{return f.workflows.read(parent(f).getJSONArray("stages").getJSONObject(0).getString("workflowId"));}
    @Test public void genuineEarlierEvidenceTransitionsWithFreshCasesAndColdPartialRecovery()throws Exception{
        Path root=original();String id,oldId;JSONObject originalParent;List<String> childIds=new ArrayList<>();
        try(var f=new WorkflowTest.Fixture(root)){originalParent=parent(f);oldId=originalParent.getString("id");var w=f.workflows.start(input(f));id=w.getString("id");w=Qualification105Test.drive(f,id,x->x.getJSONObject("qualification105").getInt("checked")==2,false);assertEquals("running",w.getString("status"));for(int i=0;i<2;i++)childIds.add(w.getJSONArray("stages").getJSONObject(i).getString("workflowId"));}
        try(var f=new WorkflowTest.Fixture(root)){f.workflows.resume(id);var w=Qualification105Test.finish(f,id);assertEquals(w.toString(),"completed",w.getString("status"));assertEquals(1,w.getInt("resumes"));assertEquals(5,w.getJSONObject("metrics").getInt("createdExamples"));assertEquals(0,w.getJSONObject("metrics").getInt("reusedExamples"));assertEquals(2,f.goals.procedures().length());assertEquals(24,f.goals.list().length());assertTrue(originalParent.similar(f.workflows.read(oldId)));var t=w.getJSONObject("qualification105").getJSONObject(Qualification111.KEY);assertEquals(oldId,t.getString("fromWorkflowId"));assertEquals(8,t.getJSONArray("changes").length());Qualification111.verify(w.getJSONObject("qualification105"));for(int i=0;i<2;i++)assertEquals(childIds.get(i),w.getJSONArray("stages").getJSONObject(i).getString("workflowId"));for(int i=0;i<5;i++)assertNotEquals(originalParent.getJSONArray("stages").getJSONObject(i).getString("workflowId"),w.getJSONArray("stages").getJSONObject(i).getString("workflowId"));reviseSource(f);var fresh=f.source("t,x\n0,0\n0.1,0.003\n");var reuse=new JSONObject().put("name","upgraded108").put("version",1).put("datasetId",fresh.getString("datasetId")).put("datasetVersion",1).put("reuseImplementation103",true);var error=assertThrows(IllegalStateException.class,()->f.workflows.reuse(reuse));assertTrue(error.getMessage().contains("revised"));assertEquals(2,f.goals.procedures().length());}
    }
    @Test public void defaultAndRefreshCannotSilentlyCrossRuntimeIdentity()throws Exception{
        try(var f=new WorkflowTest.Fixture(original())){var in=input(f);in.remove("transitionQualification108");int count=f.workflows.list().length();for(boolean refresh:List.of(false,true)){in.put("refreshQualification107",refresh);var e=assertThrows(IllegalStateException.class,()->f.workflows.start(in));assertTrue(e.getMessage().contains("explicitly request upgrade"));}var saved=f.goals.procedures().getJSONObject(0);var reuse=new JSONObject().put("name",saved.getString("name")).put("version",saved.getInt("version")).put("datasetId",in.getString("datasetId")).put("datasetVersion",1).put("reuseImplementation103",true);var refused=assertThrows(IllegalStateException.class,()->f.workflows.reuse(reuse));assertTrue(refused.getMessage().contains("earlier coordination code"));assertEquals(count,f.workflows.list().length());assertEquals(1,f.goals.procedures().length());}
    }
    @Test public void changedHistoricalArtifactRefusesWithoutNewWork()throws Exception{
        try(var f=new WorkflowTest.Fixture(original())){Qualification107Test.alter(f,first(f).getJSONObject("report"));int count=f.workflows.list().length();assertThrows(IllegalStateException.class,()->f.workflows.start(input(f)));assertEquals(count,f.workflows.list().length());assertEquals(1,f.goals.procedures().length());}
    }
    static void reviseSource(WorkflowTest.Fixture f)throws Exception{var w=first(f);var d=f.data.read(w.getJSONObject("dataBinding").getString("datasetId"),1);var args=new JSONObject().put("id",d.getString("datasetId")).put("expectedVersion",1);for(String k:List.of("title","filename","csv","sourceKind","sourceNote","missingPolicy","mapping"))args.put(k,d.get(k));f.data.importCsv(args);}
    @Test public void changedHistoricalRawRevisionRefusesEvenWhenValuesMatch()throws Exception{
        try(var f=new WorkflowTest.Fixture(original())){reviseSource(f);int count=f.workflows.list().length();var e=assertThrows(IllegalStateException.class,()->f.workflows.start(input(f)));assertTrue(e.getMessage().contains("revised"));assertEquals(count,f.workflows.list().length());}
    }
    @Test public void oldProofIsRecheckedBeforePublishingNewSelectedSource()throws Exception{
        try(var f=new WorkflowTest.Fixture(original())){String id=f.workflows.start(input(f)).getString("id");var w=Qualification105Test.drive(f,id,x->x.has("activeWorkflow105")&&x.getJSONObject("qualification105").getInt("checked")==5&&x.getJSONObject("activeWorkflow105").getString("step").equals("audit"),false);assertEquals("running",w.getString("status"));assertEquals(5,w.getJSONObject("qualification105").getInt("checked"));assertEquals("audit",w.getJSONObject("activeWorkflow105").getString("step"));reviseSource(f);w=Qualification105Test.finish(f,id);assertEquals("interrupted",w.getString("status"));assertEquals(1,f.goals.procedures().length());assertThrows(IllegalStateException.class,()->f.workflows.resume(id));f.workflows.cancel(id);}
    }
    @Test public void strictArgumentsAndChatRequireExplicitTransition()throws Exception{
        assertTrue(Qualification108.requested("Upgrade summary qualification and save workflow as upgraded"));assertFalse(Qualification108.requested("Refresh summary qualification"));assertFalse(Qualification108.requested("Upgrade browser"));
        try(var f=new WorkflowTest.Fixture(Files.createTempDirectory("qualification108-intent"))){var in=input(f);assertThrows(IllegalArgumentException.class,()->f.workflows.start(WorldFrames.copy(in).put("transitionQualification108","true")));assertThrows(IllegalArgumentException.class,()->f.workflows.start(WorldFrames.copy(in).put("refreshQualification107",true)));assertThrows(IllegalStateException.class,()->f.workflows.start(in));assertEquals(0,f.workflows.list().length());var g=Domain044Test.task("workflow").put("goal","Upgrade summary qualification and save workflow as upgraded").put("bindings044",new JSONObject().put("dataset",new JSONObject().put("datasetId","selected").put("version",1))).put("options044",new JSONObject());assertTrue(Domain044Test.domain(f.artifacts).bootstrap(g).getJSONObject("args").getBoolean("transitionQualification108"));}
    }
    @Test public void unsupportedVerifierChangesAndTamperedManifestsRefuse()throws Exception{
        var old=Qualification107.dependencies().put("WorkflowRunner","a".repeat(64)).put("suite105","b".repeat(64));assertEquals(2,Qualification108.differences(old).length());assertThrows(IllegalStateException.class,()->Qualification108.differences(Qualification107.dependencies()));for(String k:List.of("GoalRunner","MotionData","Creation","DataSummary102","summary104")){var bad=WorldFrames.copy(old).put(k,"c".repeat(64));assertThrows(IllegalStateException.class,()->Qualification108.differences(bad));}
        try(var f=new WorkflowTest.Fixture(original())){var w=f.workflows.start(input(f));var q=w.getJSONObject("qualification105");Qualification111.verify(q);var t=q.getJSONObject(Qualification111.KEY);t.put("fromRecordSha256","forged");assertThrows(IllegalStateException.class,()->Qualification111.verify(q));q.put("transitionSha256",WorldFrames.hash(t));t.getJSONObject("toDependencies").put("WorkflowRunner","forged");q.put("transitionSha256",WorldFrames.hash(t));assertThrows(IllegalStateException.class,()->Qualification111.verify(q));f.workflows.cancel(w.getString("id"));}
    }
    @Test public void originalCapacityLimitsApplyBeforeAnyNewCases()throws Exception{
        try(var f=new WorkflowTest.Fixture(original())){var source=f.source("t,x\n0,0\n1,1\n");synchronized(f.workflows){for(int i=0;i<9;i++){var w=f.workflows.start(DataSummary104Test.input(f,source));f.workflows.cancel(w.getString("id"));}}int count=f.workflows.list().length();var e=assertThrows(IllegalStateException.class,()->f.workflows.start(input(f)));assertTrue(e.getMessage().contains("capacity"));assertEquals(count,f.workflows.list().length());assertEquals(1,f.goals.procedures().length());}
    }
}
