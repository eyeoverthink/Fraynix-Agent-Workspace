package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;

/** Unit host receipts are explicit fixtures; the separate desktop cohort uses real Chromium. */
public class DataHandoff100Test {
    static JSONObject data(MeasurementStore m) throws Exception {
        return m.importCsv(new JSONObject().put("title","Synthetic unit samples").put("filename","unit.csv")
            .put("csv","t,x\n0,0\n1,2\n2,3\n").put("sourceKind","synthetic")
            .put("sourceNote","Unit fixture, not a measurement").put("missingPolicy","reject")
            .put("mapping",new JSONObject().put("timeColumn","t").put("distanceColumn","x").put("timeUnit","s").put("distanceUnit","m")));
    }
    static JSONObject complete(GoalRunner goals,WorkspaceStore store,JSONObject d) throws Exception {
        var g=goals.startData(new JSONObject().put("datasetId",d.getString("datasetId")).put("datasetVersion",1).put("speedLimitMps",.5));
        assertThrows(IllegalStateException.class,()->goals.creationSource100(new JSONObject().put("goalId",g.getString("id"))));
        var effect=CreationTest.claim(goals,"canvas.verify-report");CreationTest.receipt(store,effect,true);
        CreationTest.ack(goals,effect,CreationTest.result(effect));
        var done=CreationTest.settled(goals,g.getString("id"));assertEquals("completed",done.getString("status"));return done;
    }
    @Test public void checkedLineageAndQualitySurviveRestartAndCreationUsesExactFile() throws Exception {
        var root=Files.createTempDirectory("handoff100-restart-");var m=new MeasurementStore(root);
        try(var store=new WorkspaceStore(root);var scenes=new SceneStore(root);var brain=new BrainService(root)){
            var d=data(m);var goals=new GoalRunner(root,scenes,brain,store,m);JSONObject receipt;
            try{
                var done=complete(goals,store,d);receipt=goals.creationSource100(new JSONObject().put("goalId",done.getString("id")));
                assertEquals("normalized.csv",receipt.getJSONObject("source").getString("sourceFile"));
                assertEquals(3,receipt.getJSONObject("preview").getInt("rowCount"));assertFalse(receipt.getJSONObject("quality").getBoolean("qualityPassed"));
                assertThrows(IllegalArgumentException.class,()->goals.creationSource100(new JSONObject().put("goalId",done.getString("id")).put("passed",true)));
            }finally{goals.close();}
            try(var reopened=new GoalRunner(root,scenes,brain,store,m)){
                var source=receipt.getJSONObject("source");assertTrue(receipt.similar(reopened.creationSource100(new JSONObject().put("goalId",source.getString("dataGoalId100")))));
                var args=CreationTest.input(null);for(String k:source.keySet())args.put(k,source.get(k));
                var wrong=WorldFrames.copy(args).put("sourceFile","original.csv");assertThrows(IllegalArgumentException.class,()->reopened.startCreation(wrong));
                var created=reopened.startCreation(args);var saved=reopened.read(created.getString("id"));assertTrue(saved.getJSONObject("dataHandoff100").similar(receipt));
                assertEquals("3",saved.getJSONArray("testSteps").getJSONObject(0).getString("expectText"));
                assertEquals(store.read(source.getString("sourceArtifactId")).getJSONObject("contents").getString("normalized.csv"),saved.getString("sourceCsv"));reopened.cancel(created.getString("id"));
            }
        }
    }
    @Test public void changedReportAndChangedReceiptAreRefused() throws Exception {
        var root=Files.createTempDirectory("handoff100-changes-");var m=new MeasurementStore(root);
        try(var store=new WorkspaceStore(root);var scenes=new SceneStore(root);var brain=new BrainService(root);var goals=new GoalRunner(root,scenes,brain,store,m)){
            var d=data(m);var done=complete(goals,store,d);var request=new JSONObject().put("goalId",done.getString("id"));
            var report=store.read(done.getJSONObject("report").getString("id"));var proof=report.getJSONObject("test");
            var checks=WorldFrames.copy(proof).getJSONArray("checks");checks.getJSONObject(0).put("passed",false);
            store.recordTest(new JSONObject().put("id",report.getString("id")).put("version",report.getInt("version")).put("steps",proof.getJSONArray("steps")).put("checks",checks));
            assertThrows(IllegalStateException.class,()->goals.creationSource100(request));
            store.write(new JSONObject().put("id",report.getString("id")).put("expectedVersion",1).put("title","Changed unit report").put("files",new JSONObject().put("normalized.csv","t,x\n0,999\n")));
            assertThrows(IllegalStateException.class,()->goals.creationSource100(request));
        }
    }
    @Test public void unrelatedAndCancelledGoalsCannotEarnHandoff() throws Exception {
        var root=Files.createTempDirectory("handoff100-cancel-");var m=new MeasurementStore(root);
        try(var store=new WorkspaceStore(root);var scenes=new SceneStore(root);var brain=new BrainService(root);var goals=new GoalRunner(root,scenes,brain,store,m)){
            var d=data(m);var g=goals.startData(new JSONObject().put("datasetId",d.getString("datasetId")).put("datasetVersion",1));goals.cancel(g.getString("id"));
            assertThrows(IllegalStateException.class,()->goals.creationSource100(new JSONObject().put("goalId",g.getString("id"))));
            assertThrows(IllegalArgumentException.class,()->goals.creationSource100(new JSONObject().put("goalId",java.util.UUID.randomUUID().toString())));
        }
    }
}
