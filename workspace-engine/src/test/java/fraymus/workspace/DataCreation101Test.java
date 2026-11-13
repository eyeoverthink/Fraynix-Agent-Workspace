package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

/** Software lifecycle controls. Host receipts are fixtures; real Chromium/model trials are separate. */
public class DataCreation101Test {
    @Test public void ownPausedReservationDoesNotHideOtherWorkAndChatStopCancelsIt()throws Exception {
        try(var f=new WorkflowTest.Fixture(Files.createTempDirectory("data101-owned-resume"))){var id=f.workflows.start(input(f,f.source("t,x\n0,0\n1,2\n")).put("reviewAfterData101",true)).getString("id");
            for(int i=0;i<300;i++){var e=f.goals.claim();if(e.has("id"))WorkflowTest.hostFixture(f,e);if(f.workflows.read(id).getString("status").equals("interrupted"))break;Thread.sleep(20);}
            assertEquals("interrupted",f.workflows.read(id).getString("status"));assertTrue(f.goals.busy());assertFalse(f.goals.busyExceptWorkflow101(id));assertTrue(f.goals.busyExceptWorkflow101(UUID.randomUUID().toString()));
            try(var chat=new Agency043Test.Fixture(request->{throw new AssertionError("No model needed for control fixture");})){
                var task=chat.start("Explicit paused ownership boundary fixture","workbench");chat.agency.install044(new Domain044((route,args)->{if(route.equals("/api/workflows/cancel"))return f.workflows.cancel(args.getString("id"));return new JSONObject();},chat.store));
                var method=Agency043.class.getDeclaredMethod("task",String.class);method.setAccessible(true);var raw=(JSONObject)method.invoke(chat.agency,task.getString("id"));raw.put("status","interrupted").put("family044","workflow").put("context044",new JSONObject().put("ownedWorkflowId",id));
                assertEquals(id,chat.agency.resumeWorkflow101(task.getString("id")));assertThrows(IllegalStateException.class,()->chat.agency.control(task.getString("id"),"resume",()->true));assertEquals("interrupted",chat.agency.read(task.getString("id")).getString("status"));
                chat.agency.control(task.getString("id"),"cancel");assertEquals("cancelled",f.workflows.read(id).getString("status"));assertFalse(f.goals.workflowReserved());assertEquals(0,f.goals.procedures().length());
            }
        }
    }
    @Test public void chatBootstrapBindsFreshInputCriteriaAndWholeSkill()throws Exception {
        try(var s=new WorkspaceStore(Files.createTempDirectory("data101-chat"))){
            var g=Domain044Test.task("workflow").put("goal","Analyze data and create an explorer with gap limit 0.02 seconds and speed limit 0.4 m/s; save workflow as whole-one; review after data")
                .put("bindings044",new JSONObject().put("dataset",new JSONObject().put("datasetId","fresh").put("version",3)).put("scene",new JSONObject().put("id","unrelated").put("version",1))).put("options044",new JSONObject());
            var domain=Domain044Test.domain(s);var next=domain.bootstrap(g);assertEquals("workflow.start",next.getString("tool"));var a=next.getJSONObject("args");assertEquals("fresh",a.getString("datasetId"));assertEquals(3,a.getInt("datasetVersion"));assertEquals(.02,a.getDouble("maxGapS"),0);assertEquals(.4,a.getDouble("speedLimitMps"),0);assertEquals("whole-one",a.getString("saveSkillName"));assertTrue(a.getBoolean("reviewAfterData101"));assertFalse(a.has("sceneId"));
            g.put("goal","Reuse whole-one version 2 on selected data with speed limit 0.3 m/s");g.getJSONObject("context044").put("procedureBinding",new JSONObject().put("name","whole-one").put("version",2).put("kind",DataCreation101.KIND));next=domain.bootstrap(g);assertEquals("workflow.reuse",next.getString("tool"));a=next.getJSONObject("args");assertEquals(2,a.getInt("version"));assertEquals(.3,a.getDouble("speedLimitMps"),0);assertFalse(a.has("saveSkillName"));assertFalse(a.has("kind"));assertFalse(a.has("profile101"));
        }
    }
    @Test public void actualPostCompletionLogFailureKeepsCompletedParentAndPublishedSkill()throws Exception {
        Path root=Files.createTempDirectory("data101-log-failure");String id;Path event=root.resolve("events.jsonl"),backup=root.resolve("events-before-fault.jsonl");
        try(var f=new WorkflowTest.Fixture(root)){var d=f.source("t,x\n0,0\n1,2\n");id=f.workflows.start(input(f,d)).getString("id");var e=auditEffect(f,id);
            synchronized(f.workflows){ack(f,e,fixtureAudit(f,e,d));Files.move(event,backup);Files.createDirectory(event);}
            try{var done=finish(f,id);assertEquals("completed",done.getString("status"));assertTrue(done.has("savedSkill"));Thread.sleep(200);assertEquals("completed",f.workflows.read(id).getString("status"));assertEquals(1,f.goals.procedures().length());assertFalse(f.goals.workflowReserved());}
            finally{Files.delete(event);Files.move(backup,event);}
        }
        try(var f=new WorkflowTest.Fixture(root)){assertEquals("completed",f.workflows.read(id).getString("status"));assertEquals(1,f.goals.procedures().length());}
    }
    @Test public void lostPointerAndRepairRestartKeepTheChildAndDraftBudget()throws Exception {
        Path root=Files.createTempDirectory("data101-pointer");String id,child;
        try(var f=new WorkflowTest.Fixture(root)){var d=f.source("t,x\n0,0\n1,2\n");id=f.workflows.start(input(f,d)).getString("id");var e=auditEffect(f,id);
            synchronized(f.workflows){var bad=fixtureAudit(f,e,d);bad.getJSONArray("frames").getJSONObject(1).put("visible",false);ack(f,e,bad);var w=raw(f.workflows,id);var stage=w.getJSONArray("stages").getJSONObject(1);child=stage.getString("goalId");stage.remove("goalId");persist(f.workflows,w);f.workflows.close();}
        }
        try(var f=new WorkflowTest.Fixture(root)){assertEquals(2,f.goals.list().length());f.workflows.resume(id);var e=auditEffect(f,id);assertEquals(child,f.workflows.read(id).getJSONArray("stages").getJSONObject(1).getString("goalId"));assertEquals(2,f.goals.list().length());assertEquals(2,f.goals.read(child).getInt("draftNumber"));f.workflows.cancel(id);assertEquals(0,f.goals.procedures().length());}
    }
    static JSONObject input(WorkflowTest.Fixture f,JSONObject d){return f.input(d).put("profile101",DataCreation101.KIND).put("request","Analyze selected data and create a verified explorer").put("speedLimitMps",.1);}
    static JSONObject fixtureAudit(WorkflowTest.Fixture f,JSONObject effect,JSONObject data) throws Exception {
        var w=f.workflows.read(effect.getString("workflowId"));var h=f.goals.creationSource100(new JSONObject().put("goalId",w.getJSONArray("stages").getJSONObject(0).getString("goalId")));
        var original=MotionData.originalRows(data);var frames=new JSONArray();String filter=String.valueOf(original.getJSONObject(0).getInt("sourceRow"));
        for(int n=0;n<4;n++){var rows=new JSONArray();int count=n==1?1:n==2?0:original.length();for(int i=0;i<count;i++){var row=original.getJSONObject(i);rows.put(new JSONArray(List.of(String.valueOf(row.getInt("sourceRow")),String.valueOf(row.getDouble("timeS")),String.valueOf(row.getDouble("distanceM")))));}
            frames.put(new JSONObject().put("rows",rows).put("headers",new JSONArray(List.of("source_row","time_s","displacement_m"))).put("filter",n==1?filter:n==2?"__no_match_101__":"").put("count",String.valueOf(count)).put("scope",DataCreation101.SCOPE).put("quality",DataCreation101.quality(h)).put("visible",true));}
        return new JSONObject().put("id",effect.getJSONObject("report").getString("id")).put("version",effect.getJSONObject("report").getInt("version")).put("frames",frames);
    }
    static JSONObject auditEffect(WorkflowTest.Fixture f,String id) throws Exception {
        for(int i=0;i<1000;i++){var w=f.workflows.read(id);assertNotEquals(w.toString(),"interrupted",w.getString("status"));var e=f.goals.claim();if(e.has("id"))WorkflowTest.hostFixture(f,e);var a=f.workflows.claim();if(a.has("id"))return a;Thread.sleep(20);}throw new AssertionError("No workflow audit");
    }
    static JSONObject raw(WorkflowRunner w,String id)throws Exception {var m=WorkflowRunner.class.getDeclaredMethod("raw",String.class);m.setAccessible(true);return(JSONObject)m.invoke(w,id);}
    static void persist(WorkflowRunner w,JSONObject value)throws Exception {var m=WorkflowRunner.class.getDeclaredMethod("save",JSONObject.class);m.setAccessible(true);m.invoke(w,value);}
    static void ack(WorkflowTest.Fixture f,JSONObject e,JSONObject actual)throws Exception {f.workflows.acknowledge(new JSONObject().put("workflowId",e.getString("workflowId")).put("id",e.getString("id")).put("nonce",e.getString("nonce")).put("result",actual));}
    static JSONObject finish(WorkflowTest.Fixture f,String id)throws Exception {for(int i=0;i<200;i++){var w=f.workflows.read(id);if(!w.getString("status").equals("running"))return w;Thread.sleep(20);}throw new AssertionError("Not settled");}
    @Test public void actualRowStatesHeadersVisibilityUnitsAndQualityAreRequired()throws Exception {
        try(var f=new WorkflowTest.Fixture(Files.createTempDirectory("data101-audit"))){var d=f.source("t,x\n0,0\n1,2\n2,3\n");var w=f.workflows.start(input(f,d));var e=auditEffect(f,w.getString("id"));var actual=fixtureAudit(f,e,d);var a=f.artifacts.read(e.getJSONObject("report").getString("id"));var h=f.goals.creationSource100(new JSONObject().put("goalId",f.workflows.read(w.getString("id")).getJSONArray("stages").getJSONObject(0).getString("goalId")));
            assertTrue(DataCreation101.audit(a,d,h,actual).getBoolean("passed"));
            for(String defect:List.of("counter-only","no-match","wrong-cell","wrong-unit","hidden","headers","quality","reset")){
                var bad=WorldFrames.copy(actual);var frames=bad.getJSONArray("frames");var first=frames.getJSONObject(0);
                switch(defect){case"counter-only"->frames.getJSONObject(1).put("rows",first.getJSONArray("rows"));case"no-match"->frames.getJSONObject(2).put("rows",first.getJSONArray("rows"));case"wrong-cell"->first.getJSONArray("rows").getJSONArray(0).put(2,"99.0");case"wrong-unit"->first.getJSONArray("rows").getJSONArray(0).put(2,"0.0 mm");case"hidden"->first.put("visible",false);case"headers"->first.getJSONArray("headers").put(2,"displacement_cm");case"quality"->first.put("quality","Within declared thresholds");case"reset"->frames.getJSONObject(3).put("filter","2");}
                assertFalse(defect,DataCreation101.audit(a,d,h,bad).getBoolean("passed"));
            }
            assertThrows(IllegalArgumentException.class,()->DataCreation101.audit(a,d,h,WorldFrames.copy(actual).put("passed",true)));
            assertThrows(IllegalStateException.class,()->DataCreation101.audit(a,d,h,WorldFrames.copy(actual).put("id",UUID.randomUUID().toString())));
            f.workflows.cancel(w.getString("id"));assertThrows(IllegalStateException.class,()->ack(f,e,actual));assertEquals(0,f.goals.procedures().length());
        }
    }
    @Test public void restartAfterDataKeepsChildIdentityAndChangedReportRefusesResume()throws Exception {
        Path root=Files.createTempDirectory("data101-boundary");String id,child;
        try(var f=new WorkflowTest.Fixture(root)){var d=f.source("t,x\n0,0\n1,2\n");var w=f.workflows.start(input(f,d).put("reviewAfterData101",true));id=w.getString("id");
            for(int i=0;i<300;i++){var e=f.goals.claim();if(e.has("id"))WorkflowTest.hostFixture(f,e);w=f.workflows.read(id);if(w.getString("status").equals("interrupted"))break;Thread.sleep(20);}assertEquals("interrupted",w.getString("status"));assertEquals("creation",w.getString("step"));child=w.getJSONArray("stages").getJSONObject(0).getString("goalId");assertEquals(1,f.goals.list().length());}
        try(var f=new WorkflowTest.Fixture(root)){assertEquals(child,f.workflows.read(id).getJSONArray("stages").getJSONObject(0).getString("goalId"));var g=f.goals.read(child);var a=f.artifacts.read(g.getJSONObject("report").getString("id"));f.artifacts.write(new JSONObject().put("id",a.getString("id")).put("expectedVersion",1).put("title","Retained user edit").put("files",new JSONObject().put("normalized.csv","source_row,time_s,displacement_m\n2,0,99\n")));assertThrows(IllegalStateException.class,()->f.workflows.resume(id));assertEquals(1,f.goals.list().length());f.workflows.cancel(id);assertEquals(0,f.goals.procedures().length());}
    }
    @Test public void pendingPublicationWindowsReconcileOnceAndCancelNeverPublishes()throws Exception {
        for(String window:List.of("before-parent","after-parent","after-publish")){
            Path root=Files.createTempDirectory("data101-"+window);String id;JSONObject ref;
            try(var f=new WorkflowTest.Fixture(root)){var d=f.source("t,x\n0,0\n1,2\n");id=f.workflows.start(input(f,d)).getString("id");var e=auditEffect(f,id);
                synchronized(f.workflows){ack(f,e,fixtureAudit(f,e,d));var w=raw(f.workflows,id);ref=f.goals.retainWorkflow(DataCreation101.procedure(w),w.getJSONObject("definition").getString("saveSkillName"));w.put("pendingSkill101",ref);assertEquals(0,f.goals.procedures().length());
                    if(!window.equals("before-parent"))w.put("status","completed");persist(f.workflows,w);
                    if(window.equals("after-publish"))f.goals.publishWorkflow101(DataCreation101.procedure(w),ref);
                    f.workflows.close();
                }
            }
            try(var f=new WorkflowTest.Fixture(root)){
                if(window.equals("before-parent")){assertEquals("interrupted",f.workflows.read(id).getString("status"));assertEquals(0,f.goals.procedures().length());assertTrue(assertThrows(IllegalStateException.class,()->f.workflows.reuse(new JSONObject().put("name",ref.getString("name")).put("version",1))).getMessage().contains("unpublished"));f.workflows.cancel(id);assertEquals(0,f.goals.procedures().length());}
                else{var w=f.workflows.read(id);assertEquals("completed",w.getString("status"));assertEquals(ref.getString("id"),w.getJSONObject("savedSkill").getString("id"));assertFalse(w.has("pendingSkill101"));assertEquals(1,f.goals.procedures().length());assertEquals(2,f.goals.list().length());}
            }
            try(var f=new WorkflowTest.Fixture(root)){assertEquals(window.equals("before-parent")?0:1,f.goals.procedures().length());assertEquals(2,f.goals.list().length());}
        }
    }
    @Test public void completedWholeProcedureRebindsFreshDataAndRefusesChangedContract()throws Exception {
        try(var f=new WorkflowTest.Fixture(Files.createTempDirectory("data101-reuse"))){var d=f.source("t,x\n0,0\n1,2\n");var id=f.workflows.start(input(f,d)).getString("id");var e=auditEffect(f,id);ack(f,e,fixtureAudit(f,e,d));var done=finish(f,id);assertEquals("completed",done.getString("status"));assertTrue(done.has("savedSkill"));var fresh=f.source("t,x\n0,0\n1,3\n2,7\n");var next=f.workflows.reuse(new JSONObject().put("name",done.getJSONObject("savedSkill").getString("name")).put("version",1).put("datasetId",fresh.getString("datasetId")).put("datasetVersion",1).put("speedLimitMps",.2));assertNotEquals(done.getJSONObject("dataBinding").getString("sourceSha256"),next.getJSONObject("dataBinding").getString("sourceSha256"));f.workflows.cancel(next.getString("id"));
            synchronized(f.workflows){var changed=raw(f.workflows,id);changed.put("code101","old-verifier");persist(f.workflows,changed);}
            assertThrows(IllegalStateException.class,()->f.workflows.reuse(new JSONObject().put("name",done.getJSONObject("savedSkill").getString("name")).put("version",1).put("datasetId",fresh.getString("datasetId")).put("datasetVersion",1)));
        }
    }
    @Test public void routingAndContractsPreserveOtherWorkflows()throws Exception {
        assertEquals("workflow",ChatPolicy044.route("Analyze the selected dataset, create a verified data explorer and save workflow as sample-one",new JSONObject(),false));
        assertFalse(DataCreation101.requested("Analyze data, run a separate physics simulation and create an explorer"));
        try(var f=new WorkflowTest.Fixture(Files.createTempDirectory("data101-fields"))){var d=f.source("t,x\n0,0\n1,1\n");assertThrows(IllegalArgumentException.class,()->f.workflows.start(input(f,d).put("checks",new JSONArray())));assertThrows(IllegalArgumentException.class,()->f.workflows.start(input(f,d).put("reviewAfterData101","true")));assertThrows(IllegalArgumentException.class,()->f.workflows.start(input(f,d).put("datasetVersion",1.5)));assertEquals(0,f.goals.list().length());}
    }
    @Test public void failedReceiptPersistenceCannotPublishAnInMemoryProof()throws Exception {
        Path root=Files.createTempDirectory("data101-receipt");String id;
        try(var store=new WorkspaceStore(root)){var a=store.write(new JSONObject().put("title","Receipt persistence control").put("files",new JSONObject().put("index.html","<p>Explicit unit fixture</p>")));id=a.getString("id");var args=new JSONObject().put("id",id).put("version",1).put("steps",new JSONArray().put(new JSONObject().put("selector","p").put("expectText","ok"))).put("checks",new JSONArray().put(new JSONObject().put("passed",false)));var before=store.recordTest(args);Path receipt=root.resolve("receipts").resolve(id+"-1.json"),backup=receipt.resolveSibling("prior-proof.json");Files.move(receipt,backup);Files.createDirectory(receipt);Files.writeString(receipt.resolve("blocked"),"Explicit failure fixture");
            try{args.getJSONArray("checks").getJSONObject(0).put("passed",true);assertThrows(java.io.IOException.class,()->store.recordTest(args));assertTrue(before.similar(store.read(id).getJSONObject("test")));}
            finally{Files.delete(receipt.resolve("blocked"));Files.delete(receipt);Files.move(backup,receipt);}
        }
        try(var restored=new WorkspaceStore(root)){assertFalse(restored.read(id).getJSONObject("test").getBoolean("passed"));}
    }
}
