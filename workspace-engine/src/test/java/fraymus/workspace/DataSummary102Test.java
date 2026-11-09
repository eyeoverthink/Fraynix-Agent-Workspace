package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

public class DataSummary102Test {
    // Explicit unit host fixtures; actual browser and model runs are separate evidence.
    static JSONObject observed(WorkflowTest.Fixture f,JSONObject e,JSONObject d)throws Exception{
        var result=DataCreation101Test.fixtureAudit(f,e,d);var w=f.workflows.read(e.getString("workflowId"));var handoff=f.goals.creationSource100(new JSONObject().put("goalId",w.getJSONArray("stages").getJSONObject(0).getString("goalId")));
        var rows=MotionData.originalRows(d);double lo=0,hi=0;for(Object raw:rows){double x=((JSONObject)raw).getDouble("distanceM");lo=Math.min(lo,x);hi=Math.max(hi,x);}double start=rows.getJSONObject(0).getDouble("timeS"),duration=rows.getJSONObject(rows.length()-1).getDouble("timeS")-start;var points=new JSONArray();for(Object raw:rows){var p=(JSONObject)raw;points.put(new JSONArray(List.of(40+560*(p.getDouble("timeS")-start)/duration,200-160*(p.getDouble("distanceM")-lo)/Math.max(1e-12,hi-lo))));}
        var extra=new JSONObject().put("values",DataSummary102.summary(handoff)).put("points",points).put("viewBox",new JSONArray(List.of(0,0,640,240))).put("visible",true).put("timeAxis","Time (s)").put("distanceAxis","Displacement (m)");var all=new JSONArray();for(int i=0;i<4;i++)all.put(WorldFrames.copy(extra));return result.put("summaries102",all);
    }
    @Test public void routingAndLegacyIdentityStaySeparate(){assertTrue(DataSummary102.requested("Analyze the selected data and create a motion summary report"));assertFalse(DataSummary102.requested("Analyze physics and create a motion summary report"));assertEquals("workflow",ChatPolicy044.route("Analyze the selected data and create a motion summary report",new JSONObject(),false));assertEquals("workflow",Domain044.familyFor(DataSummary102.KIND));assertEquals(DataCreation101.code(),DataSummary102.code(DataCreation101.KIND));assertNotEquals(DataCreation101.code(),DataSummary102.code(DataSummary102.KIND));assertFalse(DataSummary102.supported("unknown"));}
    @Test public void summaryTraceAndAllOldSourceChecksAreRequired()throws Exception{
        for(String csv:List.of("t,x\n0,-2\n1,0\n2,-1\n","t,x\n0,0\n1,0\n2,0\n"))try(var f=new WorkflowTest.Fixture(Files.createTempDirectory("summary102"))){
            var d=f.source(csv);var w=f.workflows.start(DataCreation101Test.input(f,d).put("profile101",DataSummary102.KIND));var e=DataCreation101Test.auditEffect(f,w.getString("id"));assertEquals("workflow.data-summary-audit102",e.getString("kind"));w=f.workflows.read(w.getString("id"));var h=f.goals.creationSource100(new JSONObject().put("goalId",w.getJSONArray("stages").getJSONObject(0).getString("goalId")));var a=f.artifacts.read(e.getJSONObject("report").getString("id"));var actual=observed(f,e,d);assertTrue(DataSummary102.audit(w,a,d,h,actual).getBoolean("passed"));
            for(String defect:List.of("summary","point","omitted","units","hidden","viewBox","filtered-summary","old-table")){var bad=WorldFrames.copy(actual);var s=bad.getJSONArray("summaries102").getJSONObject(0);switch(defect){case"summary"->s.getJSONObject("values").put("travel","invented");case"point"->s.getJSONArray("points").getJSONArray(1).put(0,0);case"omitted"->s.getJSONArray("points").remove(0);case"units"->s.put("distanceAxis","Displacement (cm)");case"hidden"->s.put("visible",false);case"viewBox"->s.put("viewBox",new JSONArray(List.of(0,0,800,300)));case"filtered-summary"->bad.getJSONArray("summaries102").getJSONObject(1).getJSONObject("values").put("samples","1");case"old-table"->bad.getJSONArray("frames").getJSONObject(0).getJSONArray("rows").getJSONArray(0).put(1,"99");}assertFalse(defect,DataSummary102.audit(w,a,d,h,bad).getBoolean("passed"));}
            assertThrows(IllegalArgumentException.class,()->DataSummary102.audit(f.workflows.read(e.getString("workflowId")),a,d,h,WorldFrames.copy(actual).put("trusted",true)));
            DataCreation101Test.ack(f,e,actual);var done=DataCreation101Test.finish(f,w.getString("id"));assertEquals("completed",done.getString("status"));assertEquals(DataSummary102.KIND,done.getString("procedure"));assertTrue(done.has("savedSkill"));var fresh=f.source("t,x\n0,0\n1,3\n2,4\n");var reused=f.workflows.reuse(new JSONObject().put("name",done.getJSONObject("savedSkill").getString("name")).put("version",1).put("datasetId",fresh.getString("datasetId")).put("datasetVersion",1));assertEquals(DataSummary102.KIND,reused.getString("profile101"));assertNotEquals(done.getJSONObject("dataBinding").getString("sourceSha256"),reused.getJSONObject("dataBinding").getString("sourceSha256"));f.workflows.cancel(reused.getString("id"));
        }
    }
    @Test public void summaryProcedureSurvivesColdRestartWithSourceIdentity()throws Exception{
        Path root=Files.createTempDirectory("summary102-restart");String id;
        try(var f=new WorkflowTest.Fixture(root)){var d=f.source("t,x\n0,0\n1,2\n");id=f.workflows.start(DataCreation101Test.input(f,d).put("profile101",DataSummary102.KIND)).getString("id");var e=DataCreation101Test.auditEffect(f,id);DataCreation101Test.ack(f,e,observed(f,e,d));assertEquals("completed",DataCreation101Test.finish(f,id).getString("status"));}
        try(var f=new WorkflowTest.Fixture(root)){var done=f.workflows.read(id);assertEquals("completed",done.getString("status"));assertEquals(1,f.goals.procedures().length());var source=f.source("t,x\n0,0\n1,4\n");var reused=f.workflows.reuse(new JSONObject().put("name",done.getJSONObject("savedSkill").getString("name")).put("version",1).put("datasetId",source.getString("datasetId")).put("datasetVersion",1));f.workflows.cancel(reused.getString("id"));}
    }
}
