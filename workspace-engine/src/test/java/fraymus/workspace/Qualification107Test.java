package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;

public class Qualification107Test {
    static JSONObject start(WorkflowTest.Fixture f,boolean review)throws Exception{return f.workflows.start(Qualification105Test.input(f,f.source("t,x\n0,0\n0.125,-0.004\n"),"refresh107").put("refreshQualification107",true).put("reviewQualification105",review));}
    static JSONObject seed(WorkflowTest.Fixture f)throws Exception{return Qualification105Test.finish(f,Qualification105Test.start(f,"seed107").getString("id"));}
    static void alter(WorkflowTest.Fixture f,JSONObject ref)throws Exception{f.artifacts.write(new JSONObject().put("id",ref.getString("id")).put("expectedVersion",ref.getInt("version")).put("title","Deliberate changed original").put("files",new JSONObject().put("index.html","<html>Changed original proof</html>")));}
    @Test public void explicitRefreshCreatesNewProofsWhilePreservingOriginalsAcrossColdRestart()throws Exception{
        Path root=Files.createTempDirectory("qualification107-cold");JSONObject original;
        try(var f=new WorkflowTest.Fixture(root)){original=seed(f);assertEquals("completed",original.getString("status"));}
        try(var f=new WorkflowTest.Fixture(root)){
            var done=Qualification105Test.finish(f,start(f,false).getString("id"));assertEquals(done.toString(),"completed",done.getString("status"));var q=done.getJSONObject("qualification105");assertEquals("refresh",q.getJSONObject("maintenance107").getString("mode"));assertEquals(5,q.getInt("createdExamples"));assertEquals(0,q.getInt("reusedExamples"));assertEquals(5,Qualification107.verify(q).length());assertEquals(2,f.goals.procedures().length());assertEquals(24,f.goals.list().length());
            for(int i=0;i<5;i++){var old=original.getJSONArray("stages").getJSONObject(i);var fresh=done.getJSONArray("stages").getJSONObject(i);assertNotEquals(old.getString("workflowId"),fresh.getString("workflowId"));assertTrue(old.getJSONObject("proof").similar(Qualification107.verify(q).getJSONObject(i).getJSONObject("proof")));assertEquals("completed",f.workflows.read(old.getString("workflowId")).getString("status"));}
            var selected=f.workflows.read(done.getJSONArray("stages").getJSONObject(5).getString("workflowId"));assertEquals(10,selected.getJSONObject("qualification104").getJSONArray("proofs").length());
        }
    }
    @Test public void changedOriginalCannotBeHiddenByRefresh()throws Exception{
        try(var f=new WorkflowTest.Fixture(Files.createTempDirectory("qualification107-changed"))){var old=seed(f);alter(f,old.getJSONArray("stages").getJSONObject(0).getJSONObject("proof").getJSONObject("report"));int count=f.workflows.list().length();assertThrows(IllegalStateException.class,()->start(f,false));assertEquals(count,f.workflows.list().length());assertEquals(1,f.goals.procedures().length());}
    }
    @Test public void rawSourceRevisionCannotBeHiddenByRefresh()throws Exception{
        try(var f=new WorkflowTest.Fixture(Files.createTempDirectory("qualification107-source"))){seed(f);var c=Qualification105.cases().getFirst();var d=Qualification105.dataset(f.data,c);f.data.importCsv(WorldFrames.copy(c.getJSONObject("input")).put("id",d.getString("datasetId")).put("expectedVersion",1));int count=f.workflows.list().length();var e=assertThrows(IllegalStateException.class,()->start(f,false));assertTrue(e.getMessage().contains("revised"));assertEquals(count,f.workflows.list().length());assertEquals(1,f.goals.procedures().length());}
    }
    @Test public void originalBaselineIsRecheckedBeforeRecoveryEvenAfterFreshExamplesPass()throws Exception{
        Path root=Files.createTempDirectory("qualification107-baseline");String id;
        try(var f=new WorkflowTest.Fixture(root)){var old=seed(f);id=start(f,true).getString("id");var paused=Qualification105Test.finish(f,id);assertEquals("interrupted",paused.getString("status"));assertEquals(5,paused.getJSONObject("qualification105").getInt("checked"));alter(f,old.getJSONArray("stages").getJSONObject(0).getJSONObject("proof").getJSONObject("report"));}
        try(var f=new WorkflowTest.Fixture(root)){assertThrows(IllegalStateException.class,()->f.workflows.resume(id));assertEquals(1,f.goals.procedures().length());f.workflows.cancel(id);}
    }
    @Test public void refreshIntentIsStrictAndDependencyOrReferenceTamperingIsRefused()throws Exception{
        try(var f=new WorkflowTest.Fixture(Files.createTempDirectory("qualification107-intent"))){var a=Qualification105Test.input(f,f.source("t,x\n0,0\n1,1\n"),"strict");assertThrows(IllegalArgumentException.class,()->f.workflows.start(WorldFrames.copy(a).put("refreshQualification107","true")));var noSuite=WorldFrames.copy(a).put("refreshQualification107",true);noSuite.remove("qualify105");assertThrows(IllegalArgumentException.class,()->f.workflows.start(noSuite));assertEquals(0,f.workflows.list().length());}
        var m=Qualification107.manifest(true,new JSONArray());var q=new JSONObject().put("maintenance107",m).put("maintenanceSha256",WorldFrames.hash(m));Qualification107.verify(q);m.put("mode","recheck");assertThrows(IllegalStateException.class,()->Qualification107.verify(q));q.put("maintenanceSha256",WorldFrames.hash(m));m.getJSONObject("dependencies").put("GoalRunner","forged");q.put("maintenanceSha256",WorldFrames.hash(m));assertThrows(IllegalStateException.class,()->Qualification107.verify(q));
    }
    @Test public void chatRefreshIsExplicitAndDoesNotTurnOrdinaryRequestsIntoQualification()throws Exception{
        assertTrue(Qualification107.requested("Refresh qualification for the input-driven summary"));assertTrue(Qualification107.requested("Rerun summary qualification"));assertFalse(Qualification107.requested("Qualify the summary"));assertFalse(Qualification107.requested("Refresh my browser"));
        try(var f=new WorkflowTest.Fixture(Files.createTempDirectory("qualification107-chat"))){var g=Domain044Test.task("workflow").put("goal","Refresh qualification for the input-driven summary and save workflow as refreshed").put("bindings044",new JSONObject().put("dataset",new JSONObject().put("datasetId","selected").put("version",1))).put("options044",new JSONObject());assertTrue(Domain044Test.domain(f.artifacts).bootstrap(g).getJSONObject("args").getBoolean("refreshQualification107"));}
    }
}
