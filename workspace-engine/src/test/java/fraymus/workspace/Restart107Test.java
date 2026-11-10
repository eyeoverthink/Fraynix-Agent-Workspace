package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

/** Native lifecycle fixtures; actual abrupt-process and browser checks are separate. */
public class Restart107Test {
    static JSONArray cases()throws Exception{try(var in=Restart107Test.class.getResourceAsStream("/restart107/cases.json")){return new JSONObject(new String(in.readAllBytes(),StandardCharsets.UTF_8)).getJSONArray("cases");}}
    static JSONObject input(JSONObject c){return Numerical106Test.input(c).put("missingPolicy",c.optString("missingPolicy","reject"));}
    @Test public void declaredNewMappedAndExponentCasesRetainEveryValueAndQualityFlag()throws Exception{
        var store=new MeasurementStore(Files.createTempDirectory("restart107-arithmetic"));assertEquals(8,cases().length());for(Object raw:cases()){var c=(JSONObject)raw;var d=store.importCsv(input(c));var a=MotionData.analyze(d,c.getDouble("maxGapS"),c.getDouble("speedLimitMps"));var actual=DataSummary102.summary(new JSONObject().put("quality",a.getJSONObject("summary")));assertTrue(c.getString("name")+": "+actual,c.getJSONObject("expected").similar(actual));assertTrue(MotionData.originalRows(d).similar(a.getJSONArray("rows")));}
    }
    @Test public void persistedReportInputsAndChildIdentitiesSurviveRestartBeforeIndependentAudit()throws Exception{
        for(Object raw:cases()){
            var c=(JSONObject)raw;Path root=Files.createTempDirectory("restart107-"+c.getString("name"));String id;JSONObject before,data;
            try(var f=new WorkflowTest.Fixture(root)){data=f.data.importCsv(input(c));id=f.workflows.start(DataSummary104Test.input(f,data).put("maxGapS",c.getDouble("maxGapS")).put("speedLimitMps",c.getDouble("speedLimitMps"))).getString("id");DataSummary104Test.audit(f,id);before=f.workflows.read(id);}
            try(var f=new WorkflowTest.Fixture(root)){
                var ref=before.getJSONObject("report");var original=f.artifacts.read(ref.getString("id"));String reserved=original.getJSONObject("contents").getString("inputs/source.json");assertEquals(reserved,WorldFrames.copy(new JSONObject(reserved)).toString());assertFalse(new JSONObject(reserved).has("summary"));f.workflows.resume(id);var effect=DataSummary104Test.audit(f,id);DataCreation101Test.ack(f,effect,DataSummary102Test.observed(f,effect,data));var done=DataCreation101Test.finish(f,id);assertEquals(done.toString(),"completed",done.getString("status"));assertTrue(ref.similar(done.getJSONObject("report")));assertTrue(original.getJSONObject("contents").similar(f.artifacts.read(ref.getString("id")).getJSONObject("contents")));assertTrue(before.getJSONObject("dataBinding").similar(done.getJSONObject("dataBinding")));for(int i=0;i<2;i++)assertEquals(before.getJSONArray("stages").getJSONObject(i).getString("goalId"),done.getJSONArray("stages").getJSONObject(i).getString("goalId"));
            }
        }
    }
}
