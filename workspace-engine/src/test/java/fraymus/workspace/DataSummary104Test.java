package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

/** Unit host receipts are fixtures. Actual calculations/DOM results are tested in Chromium separately. */
public class DataSummary104Test {
    static JSONObject input(WorkflowTest.Fixture f,JSONObject d){return DataCreation101Test.input(f,d).put("profile101",DataSummary104.KIND).put("saveSkillName","").put("maxGapS",.25).put("speedLimitMps",.5);}
    static JSONObject run(WorkflowTest.Fixture f,JSONObject d,String name)throws Exception{
        var w=f.workflows.start(input(f,d).put("saveSkillName",name));var e=audit(f,w.getString("id"));DataCreation101Test.ack(f,e,DataSummary102Test.observed(f,e,d));return DataCreation101Test.finish(f,w.getString("id"));
    }
    static JSONObject audit(WorkflowTest.Fixture f,String id)throws Exception{
        for(int i=0;i<600;i++){var w=f.workflows.read(id);assertNotEquals(w.toString(),"interrupted",w.getString("status"));var e=f.goals.claim();if(e.has("id")){assertNotEquals("creation.generate",e.getString("kind"));assertNotEquals("creation.plan",e.getString("kind"));WorkflowTest.hostFixture(f,e);}var a=f.workflows.claim();if(a.has("id"))return a;Thread.sleep(15);}throw new AssertionError("No independent summary audit");
    }
    static List<JSONObject> qualify(WorkflowTest.Fixture f)throws Exception{
        var out=new ArrayList<JSONObject>();for(String csv:List.of("t,x\n0,0\n0.1,0.01\n0.2,0.02\n","t,x\n0,0\n0.1,0.01\n0.2,-0.01\n","t,x\n0,-2\n0.1,-2\n0.2,-2\n"))out.add(run(f,f.source(csv),""));
        var d=f.data.importCsv(new JSONObject().put("title","Changed-unit qualification fixture").put("filename","unit.csv").put("csv","t,x\n0,0\n100,100\n600,200\n").put("sourceKind","synthetic").put("sourceNote","Explicit unit fixture").put("missingPolicy","reject").put("mapping",new JSONObject().put("timeColumn","t").put("distanceColumn","x").put("timeUnit","ms").put("distanceUnit","cm")));out.add(run(f,d,""));return out;
    }
    @Test public void authoredReportUsesFreshCriteriaAndCannotPublishBeforeQualification()throws Exception{
        try(var f=new WorkflowTest.Fixture(Files.createTempDirectory("summary104-gate"))){var d=f.source("t,x\n0,0\n1,2\n");var w=run(f,d,"must-wait");assertEquals("interrupted",w.getString("status"));assertTrue(w.getString("reason").contains("four distinct"));assertEquals(0,f.goals.procedures().length());var child=f.goals.read(w.getJSONArray("stages").getJSONObject(1).getString("goalId"));assertEquals("Java input-summary104",child.getString("modelName"));var a=f.artifacts.read(w.getJSONObject("report").getString("id"));var source=new JSONObject(a.getJSONObject("contents").getString("inputs/source.json"));assertEquals(.25,source.getJSONObject("criteria104").getDouble("maxGapS"),0);assertFalse(source.has("quality"));assertFalse(source.has("summary"));assertTrue(DataSummary104.files().similar(Implementation103.files(a)));f.workflows.cancel(w.getString("id"));}
    }
    @Test public void variedEvidenceRetainsThenColdReusesAndRejectsChangedQualification()throws Exception{
        Path root=Files.createTempDirectory("summary104-qualified");String parent,proof;JSONObject next;
        try(var f=new WorkflowTest.Fixture(root)){var cases=qualify(f);proof=cases.get(0).getString("id");var w=run(f,f.source("t,x\n0,0\n0.2,0.03\n0.4,-0.02\n"),"qualified-summary");assertEquals("completed",w.getString("status"));assertEquals(4,w.getJSONObject("qualification104").getJSONArray("proofs").length());parent=w.getString("id");assertEquals(1,f.goals.procedures().length());}
        try(var f=new WorkflowTest.Fixture(root)){var d=f.source("t,x\n0,1\n0.3,1\n0.6,1\n");next=f.workflows.reuse(Implementation103Test.args(f.workflows.read(parent),d));var e=audit(f,next.getString("id"));DataCreation101Test.ack(f,e,DataSummary102Test.observed(f,e,d));assertEquals("completed",DataCreation101Test.finish(f,next.getString("id")).getString("status"));var original=f.workflows.read(proof);var a=f.artifacts.read(original.getJSONObject("report").getString("id"));f.artifacts.write(new JSONObject().put("id",a.getString("id")).put("expectedVersion",1).put("title","Altered qualification").put("files",new JSONObject().put("index.html","<html><body>Changed original proof</body></html>")));assertThrows(IllegalStateException.class,()->f.workflows.reuse(Implementation103Test.args(f.workflows.read(parent),d)));}
    }
    @Test public void currentAuditRejectsChangedSummaryAndDoesNotRegenerate()throws Exception{
        try(var f=new WorkflowTest.Fixture(Files.createTempDirectory("summary104-audit"))){var d=f.source("t,x\n0,0\n0.1,2\n");var w=f.workflows.start(input(f,d));var e=audit(f,w.getString("id"));var actual=DataSummary102Test.observed(f,e,d);actual.getJSONArray("summaries102").getJSONObject(0).getJSONObject("values").put("travel","999.000000");DataCreation101Test.ack(f,e,actual);assertEquals("interrupted",DataCreation101Test.finish(f,w.getString("id")).getString("status"));assertThrows(IllegalStateException.class,()->f.workflows.resume(w.getString("id")));assertEquals(0,f.goals.procedures().length());f.workflows.cancel(w.getString("id"));}
    }
    @Test public void publicCreationCannotInstallAuthoredCodeAndChatKeepsOldGenerationExplicit()throws Exception{
        try(var f=new WorkflowTest.Fixture(Files.createTempDirectory("summary104-boundary"))){assertThrows(IllegalArgumentException.class,()->f.goals.startCreation(new JSONObject().put("summary104",new JSONObject())));var g=Domain044Test.task("workflow").put("goal","Analyze data and create an input-driven motion summary report").put("bindings044",new JSONObject().put("dataset",new JSONObject().put("datasetId","fresh").put("version",1))).put("options044",new JSONObject());assertEquals(DataSummary104.KIND,Domain044Test.domain(f.artifacts).bootstrap(g).getJSONObject("args").getString("profile101"));g.put("goal","Analyze data and create a motion summary report");assertEquals(DataSummary102.KIND,Domain044Test.domain(f.artifacts).bootstrap(g).getJSONObject("args").getString("profile101"));}
    }
    @Test public void protocolsAndOldVerifiersRemainDistinct(){assertEquals(DataSummary102.code(DataSummary102.KIND),DataSummary104.code(DataSummary102.KIND));assertNotEquals(DataSummary104.code(DataSummary104.KIND),DataSummary102.code(DataSummary102.KIND));assertTrue(DataSummary104.supported(DataSummary104.KIND));assertFalse(DataSummary102.supported(DataSummary104.KIND));}
}
