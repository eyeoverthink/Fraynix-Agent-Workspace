package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;

/** Lifecycle fixtures; the separate Electron cohort supplies actual displayed observations. */
public class Qualification105Test {
    static JSONObject input(WorkflowTest.Fixture f,JSONObject d,String name){return DataSummary104Test.input(f,d).put("qualify105",true).put("saveSkillName",name);}
    static JSONObject drive(WorkflowTest.Fixture f,String id,Predicate<JSONObject> stop,boolean corrupt)throws Exception{
        long deadline=System.currentTimeMillis()+150_000;boolean changed=false;
        while(System.currentTimeMillis()<deadline){
            var w=f.workflows.read(id);if(stop.test(w)||!w.getString("status").equals("running"))return w;
            var e=f.goals.claim();if(e.has("id")){assertFalse(e.getString("kind").startsWith("creation.generate"));WorkflowTest.hostFixture(f,e);}
            JSONObject audit;
            synchronized(f.workflows){
                // Stop and claim share the runner lock: never acknowledge the boundary being inspected.
                var current=f.workflows.read(id);
                if(stop.test(current)||!current.getString("status").equals("running"))return current;
                audit=f.workflows.claim();
            }
            if(audit.has("id")){
                assertEquals(id,f.workflows.authorityOwner105(audit.getString("workflowId")));
                var b=f.workflows.read(audit.getString("workflowId")).getJSONObject("dataBinding");var data=f.data.read(b.getString("datasetId"),b.getInt("datasetVersion"));
                var observed=DataSummary102Test.observed(f,audit,data);if(corrupt&&!changed){observed.getJSONArray("summaries102").getJSONObject(0).getJSONObject("values").put("travel","999.000000");changed=true;}
                DataCreation101Test.ack(f,audit,observed);
            }
            Thread.sleep(15);
        }throw new AssertionError("Qualification timeout: "+f.workflows.read(id));
    }
    static JSONObject finish(WorkflowTest.Fixture f,String id)throws Exception{return drive(f,id,w->false,false);}
    static JSONObject start(WorkflowTest.Fixture f,String name)throws Exception{return f.workflows.start(input(f,f.source("t,x\n0,0\n0.3,0.01\n0.8,-0.005\n"),name));}
    @Test public void qualifiesFiveExamplesThenColdRechecksBeforeSavingFreshSource()throws Exception{
        Path root=Files.createTempDirectory("qualification105-cold");List<String> proofs;
        try(var f=new WorkflowTest.Fixture(root)){
            var w=finish(f,start(f,"auto-summary").getString("id"));assertEquals(w.toString(),"completed",w.getString("status"));assertTrue(w.getJSONObject("audit").getBoolean("passed"));assertEquals(5,w.getJSONObject("metrics").getInt("createdExamples"));assertEquals(0,w.getJSONObject("metrics").getInt("reusedExamples"));assertEquals(1,f.goals.procedures().length());assertEquals(12,f.goals.list().length());
            proofs=new ArrayList<>();for(int i=0;i<5;i++){var s=w.getJSONArray("stages").getJSONObject(i);proofs.add(s.getString("workflowId"));var b=f.workflows.read(s.getString("workflowId")).getJSONObject("dataBinding");assertEquals("synthetic",b.getString("sourceKind"));assertTrue(s.getLong("elapsedMs")>=0);}assertFalse(f.goals.busy());
        }
        try(var f=new WorkflowTest.Fixture(root)){
            var d=f.source("t,x\n0,-0.01\n0.2,-0.01\n0.4,-0.01\n");var w=finish(f,f.workflows.start(input(f,d,"auto-next")).getString("id"));assertEquals(w.toString(),"completed",w.getString("status"));assertEquals(0,w.getJSONObject("metrics").getInt("createdExamples"));assertEquals(5,w.getJSONObject("metrics").getInt("reusedExamples"));assertEquals(1,w.getJSONObject("metrics").getInt("bankBefore"));assertEquals(2,w.getJSONObject("metrics").getInt("bankAfter"));assertEquals(14,f.goals.list().length());for(int i=0;i<5;i++)assertEquals(proofs.get(i),w.getJSONArray("stages").getJSONObject(i).getString("workflowId"));
        }
    }
    @Test public void recoversPartlyCompletedSuiteWithoutDuplicatingExamples()throws Exception{
        Path root=Files.createTempDirectory("qualification105-recovery");String id;List<String> retained=new ArrayList<>();
        try(var f=new WorkflowTest.Fixture(root)){id=start(f,"recover-summary").getString("id");var w=drive(f,id,v->v.getJSONObject("qualification105").getInt("checked")==2,false);assertEquals("running",w.getString("status"));for(int i=0;i<2;i++)retained.add(w.getJSONArray("stages").getJSONObject(i).getString("workflowId"));}
        try(var f=new WorkflowTest.Fixture(root)){assertEquals("interrupted",f.workflows.read(id).getString("status"));f.workflows.resume(id);var w=finish(f,id);assertEquals(w.toString(),"completed",w.getString("status"));assertEquals(1,w.getInt("resumes"));assertEquals(12,f.goals.list().length());assertEquals(6,f.data.datasets().length());for(int i=0;i<2;i++)assertEquals(retained.get(i),w.getJSONArray("stages").getJSONObject(i).getString("workflowId"));}
    }
    @Test public void refusesCorruptExampleAndCannotResumePastItsFailure()throws Exception{
        try(var f=new WorkflowTest.Fixture(Files.createTempDirectory("qualification105-fault"))){String id=start(f,"never-save").getString("id");var w=drive(f,id,v->false,true);assertEquals("interrupted",w.getString("status"));assertEquals(0,f.goals.procedures().length());assertEquals(0,w.getJSONObject("qualification105").getInt("checked"));assertThrows(IllegalStateException.class,()->f.workflows.resume(id));f.workflows.cancel(id);assertFalse(f.goals.busy());assertEquals(2,f.goals.list().length());}
    }
    @Test public void alteredRetainedProofRefusesAdmissionWithoutRefreshingIt()throws Exception{
        try(var f=new WorkflowTest.Fixture(Files.createTempDirectory("qualification105-old-proof"))){var w=finish(f,start(f,"original-summary").getString("id"));var proof=f.workflows.read(w.getJSONArray("stages").getJSONObject(0).getString("workflowId"));var ref=proof.getJSONObject("report");f.artifacts.write(new JSONObject().put("id",ref.getString("id")).put("expectedVersion",ref.getInt("version")).put("title","Changed qualification source").put("files",new JSONObject().put("index.html","<html><body>Altered original</body></html>")));int count=f.workflows.list().length();assertThrows(IllegalStateException.class,()->start(f,"must-refuse"));assertEquals(count,f.workflows.list().length());assertEquals(1,f.goals.procedures().length());}
    }
    @Test public void reviewKeepsOwnershipAndRejectsChangedProofBeforeSelectedData()throws Exception{
        try(var f=new WorkflowTest.Fixture(Files.createTempDirectory("qualification105-review"))){var d=f.source("t,x\n0,0\n0.1,0.003\n");String id=f.workflows.start(input(f,d,"review-summary").put("reviewQualification105",true)).getString("id");var w=finish(f,id);assertEquals("interrupted",w.getString("status"));assertEquals(5,w.getJSONObject("qualification105").getInt("checked"));assertEquals(0,f.goals.procedures().length());assertThrows(IllegalStateException.class,()->f.goals.startData(new JSONObject()));var ref=w.getJSONArray("stages").getJSONObject(0).getJSONObject("proof").getJSONObject("report");f.artifacts.write(new JSONObject().put("id",ref.getString("id")).put("expectedVersion",1).put("title","Changed").put("files",new JSONObject().put("index.html","<html>Changed</html>")));assertThrows(IllegalStateException.class,()->f.workflows.resume(id));f.workflows.cancel(id);assertFalse(f.goals.busy());}
    }
    @Test public void refusesInsufficientCapacityBeforeCreatingAnyExamples()throws Exception{
        try(var f=new WorkflowTest.Fixture(Files.createTempDirectory("qualification105-capacity"))){var d=f.source("t,x\n0,0\n0.1,0.003\n");synchronized(f.workflows){for(int i=0;i<20;i++){var w=f.workflows.start(DataSummary104Test.input(f,d));f.workflows.cancel(w.getString("id"));}}assertThrows(IllegalStateException.class,()->f.workflows.start(input(f,d,"no-room")));assertEquals(1,f.data.datasets().length());assertEquals(20,f.workflows.list().length());}
    }
    @Test public void reconcilesOrphanInputAndRejectsRevisionAndForeignChildControl()throws Exception{
        try(var f=new WorkflowTest.Fixture(Files.createTempDirectory("qualification105-reconcile"))){var c=Qualification105.cases().getFirst();var d=Qualification105.dataset(f.data,c);assertEquals(d.getString("id"),Qualification105.dataset(f.data,c).getString("id"));String id=start(f,"owned-summary").getString("id");var w=drive(f,id,v->v.has("activeWorkflow105"),false);String child=w.getJSONObject("activeWorkflow105").getString("id");assertThrows(IllegalStateException.class,()->f.workflows.cancel(child));assertThrows(IllegalStateException.class,()->f.workflows.resume(child));f.workflows.cancel(id);var in=WorldFrames.copy(c.getJSONObject("input")).put("id",d.getString("datasetId")).put("expectedVersion",1);f.data.importCsv(in);assertThrows(IllegalStateException.class,()->Qualification105.dataset(f.data,c));}
    }
    @Test public void chatSelectsExplicitQualificationWithoutChangingOrdinarySummaries()throws Exception{
        assertEquals("workflow",ChatPolicy044.route("Qualify the input-driven summary and save workflow as qualified-report",new JSONObject(),false));
        try(var f=new WorkflowTest.Fixture(Files.createTempDirectory("qualification105-chat"))){var g=Domain044Test.task("workflow").put("goal","Qualify the input-driven summary, analyze selected data and save workflow as qualified-report").put("bindings044",new JSONObject().put("dataset",new JSONObject().put("datasetId","selected").put("version",1))).put("options044",new JSONObject());var a=Domain044Test.domain(f.artifacts).bootstrap(g).getJSONObject("args");assertTrue(a.getBoolean("qualify105"));assertEquals("qualified-report",a.getString("saveSkillName"));assertEquals(DataSummary104.KIND,a.getString("profile101"));assertEquals("selected",a.getString("datasetId"));}
    }
    @Test public void hostResumeRecognizesOnlyTheInterruptedParentsActualChildReservation()throws Exception{
        Path root=Files.createTempDirectory("qualification105-host-resume");String id;
        try(var f=new WorkflowTest.Fixture(root)){id=start(f,"host-resume").getString("id");drive(f,id,w->w.has("activeWorkflow105"),false);}
        try(var f=new WorkflowTest.Fixture(root)){
            var w=f.workflows.read(id);assertEquals("interrupted",w.getString("status"));String child=w.getJSONObject("activeWorkflow105").getString("id");
            assertTrue(f.goals.busyExceptWorkflow101(id));assertEquals(child,WorkspaceServer.resumeOwner105(w));assertFalse(f.goals.busyExceptWorkflow101(WorkspaceServer.resumeOwner105(w)));
            var forged=WorldFrames.copy(w);forged.getJSONObject("activeWorkflow105").put("qualificationOwner105",UUID.randomUUID().toString());assertThrows(IllegalStateException.class,()->WorkspaceServer.resumeOwner105(forged));
            assertEquals("",WorkspaceServer.resumeOwner105(WorldFrames.copy(w).put("status","running")));
            assertEquals("",WorkspaceServer.resumeOwner105(WorldFrames.copy(w).put("status","completed")));
            assertEquals(id,WorkspaceServer.resumeOwner105(new JSONObject().put("id",id).put("status","interrupted")));
            f.workflows.cancel(id);assertFalse(f.goals.busy());
        }
    }
}
