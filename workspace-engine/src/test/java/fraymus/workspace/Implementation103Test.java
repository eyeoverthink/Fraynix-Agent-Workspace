package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

/** Lifecycle/altered-record controls with explicitly synthetic host receipts. Real browser trials are separate. */
public class Implementation103Test {
    static JSONObject original(WorkflowTest.Fixture f)throws Exception {
        var d=f.source("t,x\n0,0\n0.1,0.001\n");var w=f.workflows.start(DataCreation101Test.input(f,d));
        var e=DataCreation101Test.auditEffect(f,w.getString("id"));DataCreation101Test.ack(f,e,DataCreation101Test.fixtureAudit(f,e,d));
        var done=DataCreation101Test.finish(f,w.getString("id"));assertEquals("completed",done.getString("status"));return done;
    }
    static JSONObject args(JSONObject w,JSONObject fresh){return new JSONObject().put("name",w.getJSONObject("savedSkill").getString("name")).put("version",w.getJSONObject("savedSkill").getInt("version")).put("datasetId",fresh.getString("datasetId")).put("datasetVersion",1).put("reuseImplementation103",true);}
    static JSONObject testEffect(WorkflowTest.Fixture f,String id)throws Exception {
        for(int i=0;i<500;i++){var e=f.goals.claim();if(e.has("id")){assertNotEquals("creation.generate",e.getString("kind"));if(e.getString("kind").equals("creation.test"))return e;WorkflowTest.hostFixture(f,e);}assertNotEquals(f.workflows.read(id).toString(),"interrupted",f.workflows.read(id).getString("status"));Thread.sleep(20);}throw new AssertionError("No fresh browser test");
    }
    static JSONObject complete(WorkflowTest.Fixture f,JSONObject w,JSONObject d)throws Exception {
        WorkflowTest.hostFixture(f,testEffect(f,w.getString("id")));var e=DataCreation101Test.auditEffect(f,w.getString("id"));DataCreation101Test.ack(f,e,DataCreation101Test.fixtureAudit(f,e,d));return DataCreation101Test.finish(f,w.getString("id"));
    }
    @Test public void freshInputsGetNewArtifactFreshChecksAndLinkedSavedSkill()throws Exception {
        try(var f=new WorkflowTest.Fixture(Files.createTempDirectory("impl103-fresh"))){
            var parent=original(f);var old=f.artifacts.read(parent.getJSONObject("report").getString("id"));var d=f.source("t,x\n0,-1\n0.1,2\n0.2,-3\n");
            var next=f.workflows.reuse(args(parent,d).put("saveSkillName","reused-one"));var e=testEffect(f,next.getString("id"));var a=f.artifacts.read(e.getJSONObject("report").getString("id"));
            assertNotEquals(old.getString("id"),a.getString("id"));assertFalse(a.has("test"));assertTrue(Implementation103.files(old).similar(Implementation103.files(a)));assertNotEquals(old.getJSONObject("contents").getString("inputs/source.csv"),a.getJSONObject("contents").getString("inputs/source.csv"));
            WorkflowTest.hostFixture(f,e);e=DataCreation101Test.auditEffect(f,next.getString("id"));DataCreation101Test.ack(f,e,DataCreation101Test.fixtureAudit(f,e,d));var done=DataCreation101Test.finish(f,next.getString("id"));assertEquals("completed",done.getString("status"));assertEquals(2,f.goals.procedures().length());
            var third=f.workflows.reuse(args(done,f.source("t,x\n0,3\n0.1,4\n")));assertEquals(done.getString("id"),third.getJSONObject("implementation103").getString("sourceWorkflowId"));f.workflows.cancel(third.getString("id"));assertTrue(old.similar(f.artifacts.read(old.getString("id"))));
        }
    }
    @Test public void coldRestartAfterDataRetainsImplementationAndChildIdentity()throws Exception {
        Path root=Files.createTempDirectory("impl103-restart");String id,child;JSONObject data;
        try(var f=new WorkflowTest.Fixture(root)){var parent=original(f);data=f.source("t,x\n0,2\n0.1,4\n");id=f.workflows.reuse(args(parent,data).put("reviewAfterData101",true)).getString("id");
            for(int i=0;i<400&&!f.workflows.read(id).getString("status").equals("interrupted");i++){var e=f.goals.claim();if(e.has("id"))WorkflowTest.hostFixture(f,e);Thread.sleep(20);}
            var w=f.workflows.read(id);assertEquals("interrupted",w.getString("status"));child=w.getJSONArray("stages").getJSONObject(0).getString("goalId");}
        try(var f=new WorkflowTest.Fixture(root)){f.workflows.resume(id);var done=complete(f,f.workflows.read(id),data);assertEquals("completed",done.getString("status"));assertEquals(child,done.getJSONArray("stages").getJSONObject(0).getString("goalId"));assertEquals(4,f.goals.list().length());}
    }
    @Test public void failedFreshBehaviorCannotRegenerateOrPublishOnResume()throws Exception {
        try(var f=new WorkflowTest.Fixture(Files.createTempDirectory("impl103-fail"))){var parent=original(f);var d=f.source("t,x\n0,0\n0.1,4\n");var w=f.workflows.reuse(args(parent,d).put("saveSkillName","must-not-publish"));var e=testEffect(f,w.getString("id"));
            var checks=new JSONArray();var steps=e.getJSONArray("steps");for(int i=0;i<steps.length();i++){var s=steps.getJSONObject(i);var actual=new JSONObject().put("text",i==0?"wrong":s.optString("expectText")).put("count",s.optInt("expectCount",1)).put("value",s.optString("expectValue"));checks.put(new JSONObject().put("index",i).put("selector",s.getString("selector")).put("passed",i!=0).put("actual",actual));}
            var ref=e.getJSONObject("report");f.artifacts.recordTest(new JSONObject().put("id",ref.getString("id")).put("version",ref.getInt("version")).put("steps",steps).put("checks",checks));WorkflowTest.ack(f.goals,e,new JSONObject().put("id",ref.getString("id")).put("version",ref.getInt("version")));
            var failed=DataCreation101Test.finish(f,w.getString("id"));assertEquals("interrupted",failed.getString("status"));assertEquals(1,f.goals.procedures().length());assertThrows(IllegalStateException.class,()->f.workflows.resume(w.getString("id")));assertFalse(f.goals.claim().has("id"));f.workflows.cancel(w.getString("id"));
        }
    }
    @Test public void independentAuditFailureCannotReusePassingBehaviorReceipt()throws Exception {
        try(var f=new WorkflowTest.Fixture(Files.createTempDirectory("impl103-audit"))){var parent=original(f);var d=f.source("t,x\n0,0\n0.1,4\n");var w=f.workflows.reuse(args(parent,d));WorkflowTest.hostFixture(f,testEffect(f,w.getString("id")));var e=DataCreation101Test.auditEffect(f,w.getString("id"));var bad=DataCreation101Test.fixtureAudit(f,e,d);bad.getJSONArray("frames").getJSONObject(0).put("visible",false);DataCreation101Test.ack(f,e,bad);
            assertEquals("interrupted",DataCreation101Test.finish(f,w.getString("id")).getString("status"));assertThrows(IllegalStateException.class,()->f.workflows.resume(w.getString("id")));assertEquals(1,f.goals.procedures().length());f.workflows.cancel(w.getString("id"));}
    }
    @Test public void changedOriginalArtifactAndForgedProvenanceRefuseBeforeDispatch()throws Exception {
        for(String fault:List.of("artifact","source","receipt","code","goal","skill","cycle"))try(var f=new WorkflowTest.Fixture(Files.createTempDirectory("impl103-"+fault))){var parent=original(f);var d=f.source("t,x\n0,1\n0.1,2\n");String id;
            synchronized(f.workflows){var w=f.workflows.reuse(args(parent,d));id=w.getString("id");var raw=DataCreation101Test.raw(f.workflows,id);var b=raw.getJSONObject("implementation103");
                switch(fault){case"artifact"->{var a=f.artifacts.read(parent.getJSONObject("report").getString("id"));f.artifacts.write(new JSONObject().put("id",a.getString("id")).put("expectedVersion",a.getInt("version")).put("title","Changed original").put("files",new JSONObject().put("index.html","<html><body>Changed original code</body></html>")));}case"source"->{var child=f.goals.read(parent.getJSONArray("stages").getJSONObject(0).getString("goalId"));var a=f.artifacts.read(child.getJSONObject("report").getString("id"));f.artifacts.write(new JSONObject().put("id",a.getString("id")).put("expectedVersion",a.getInt("version")).put("title","Changed original source").put("files",new JSONObject().put("normalized.csv","source_row,time_s,displacement_m\n2,0,999\n")));}case"receipt"->{var a=f.artifacts.read(parent.getJSONObject("report").getString("id"));var checks=WorldFrames.copy(a.getJSONObject("test")).getJSONArray("checks");checks.getJSONObject(0).put("passed",false);f.artifacts.recordTest(new JSONObject().put("id",a.getString("id")).put("version",a.getInt("version")).put("steps",a.getJSONObject("test").getJSONArray("steps")).put("checks",checks));}case"code"->b.put("codeSha256","0".repeat(64));case"goal"->b.put("sourceGoalId",UUID.randomUUID().toString());case"skill"->b.getJSONObject("skill").put("version",999);case"cycle"->b.put("sourceWorkflowId",id);}
                DataCreation101Test.persist(f.workflows,raw);
            }
            assertEquals(fault,"interrupted",DataCreation101Test.finish(f,id).getString("status"));assertEquals(fault,2,f.goals.list().length());f.workflows.cancel(id);
        }
    }
    @Test public void publicCreationCannotInjectReuseAndModeMustBeBoolean()throws Exception {
        try(var f=new WorkflowTest.Fixture(Files.createTempDirectory("impl103-boundary"))){var p=original(f);var d=f.source("t,x\n0,1\n0.1,2\n");assertThrows(IllegalArgumentException.class,()->f.workflows.reuse(args(p,d).put("reuseImplementation103","true")));assertThrows(IllegalArgumentException.class,()->f.goals.startCreation(new JSONObject().put("request","foo").put("title","foo").put("modelName","unit").put("implementation103",p)));
            var legacy=f.workflows.reuse(args(p,d).put("reuseImplementation103",false));assertFalse(legacy.has("implementation103"));f.workflows.cancel(legacy.getString("id"));}
    }
    @Test public void candidateCannotCarryOldInputsOrChangedCode()throws Exception {
        var g=new JSONObject().put("sourceCsv","a\nnew\n").put("sourceData",new JSONObject().put("rows",new JSONArray().put("new")));
        var code=new JSONObject().put("index.html","<!doctype html><body>Candidate input boundary fixture</body>");var files=Creation.files(g,code);var binding=new JSONObject().put("implementationSha256",Creation.digest(code));
        Implementation103.candidate(binding,files);Implementation103.inputs(g,files);
        for(String name:List.of("inputs/source.csv","inputs/source.json","inputs/foreign.json","index.html")){var bad=WorldFrames.copy(files);bad.put(name,"changed");if(name.equals("index.html"))assertThrows(RuntimeException.class,()->Implementation103.candidate(binding,bad));else assertThrows(IllegalStateException.class,()->Implementation103.inputs(g,bad));}
    }
    @Test public void chatDefaultsToReuseButExplicitRegenerationStaysAvailable()throws Exception {
        try(var store=new WorkspaceStore(Files.createTempDirectory("impl103-chat"))){var g=Domain044Test.task("workflow").put("goal","Reuse saved-one version 1 on selected data").put("bindings044",new JSONObject().put("dataset",new JSONObject().put("datasetId","fresh").put("version",1))).put("options044",new JSONObject());g.getJSONObject("context044").put("procedureBinding",new JSONObject().put("name","saved-one").put("version",1).put("kind",DataCreation101.KIND));var domain=Domain044Test.domain(store);assertTrue(domain.bootstrap(g).getJSONObject("args").getBoolean("reuseImplementation103"));g.put("goal","Regenerate saved-one version 1 on selected data");assertFalse(domain.bootstrap(g).getJSONObject("args").getBoolean("reuseImplementation103"));}
    }
}
