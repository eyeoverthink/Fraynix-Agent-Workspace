package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

/** Declared numerical assertions. Actual renderer results are collected by the Electron cohort. */
public class Numerical106Test {
    static JSONArray cases()throws Exception{try(var in=Numerical106Test.class.getResourceAsStream("/numerical106/cases.json")){return new JSONObject(new String(in.readAllBytes(),StandardCharsets.UTF_8)).getJSONArray("cases");}}
    static JSONObject input(JSONObject c){return new JSONObject().put("title",c.getString("name")).put("filename","edge.csv").put("csv",c.getString("csv")).put("mapping",c.getJSONObject("mapping")).put("sourceKind","synthetic").put("sourceNote","Declared numerical software test, not measured footage").put("missingPolicy","reject");}
    @Test public void mappedSignedScaleRoundingAndThresholdCasesMatchDeclaredValues()throws Exception{
        var store=new MeasurementStore(Files.createTempDirectory("numerical106"));assertEquals(10,cases().length());
        for(Object raw:cases()){var c=(JSONObject)raw;var d=store.importCsv(input(c));var a=MotionData.analyze(d,c.getDouble("maxGapS"),c.getDouble("speedLimitMps"));var actual=DataSummary102.summary(new JSONObject().put("quality",a.getJSONObject("summary")));assertTrue(c.getString("name")+": "+actual,c.getJSONObject("expected").similar(actual));assertTrue(MotionData.originalRows(d).similar(a.getJSONArray("rows")));}
    }
    @Test public void invalidTimesNonfiniteCellsAndMappedBoundsRemainRefused()throws Exception{
        var store=new MeasurementStore(Files.createTempDirectory("numerical106-invalid"));var base=cases().getJSONObject(0);var c=new JSONObject(base.toString()).put("mapping",new JSONObject().put("timeColumn","t").put("distanceColumn","x").put("timeUnit","s").put("distanceUnit","m"));
        for(String csv:new String[]{"t,x\n0,0\n0,1\n","t,x\n0,0\n1,NaN\n","t,x\n0,0\n1,Infinity\n","t,x\n0,0\n86401,1\n","t,x\n0,0\n1,1000001\n"}){c.put("csv",csv);assertThrows(IllegalArgumentException.class,()->store.importCsv(input(c)));}
        assertEquals(0,store.datasets().length());
    }
    @Test public void finiteInputsWithOverflowingDerivedVelocityAreNotClamped()throws Exception{
        var store=new MeasurementStore(Files.createTempDirectory("numerical106-overflow"));var c=new JSONObject(cases().getJSONObject(0).toString()).put("csv","t,x\n0,0\n5e-324,1\n").put("mapping",new JSONObject().put("timeColumn","t").put("distanceColumn","x").put("timeUnit","s").put("distanceUnit","m"));var d=store.importCsv(input(c));
        var error=assertThrows(IllegalArgumentException.class,()->MotionData.analyze(d,.000001,1));assertTrue(error.getMessage().contains("finite numerical range"));assertEquals(1,store.datasets().length());assertEquals("t,x\n0,0\n5e-324,1\n",d.getString("csv"));
    }
    @Test public void exponentCriteriaKeepExactReservedInputsAcrossNativeCheckpoint()throws Exception{
        for(Object raw:cases()){
            var c=(JSONObject)raw;if(!java.util.Set.of("tiny-scale","large-valid-scale").contains(c.getString("name")))continue;
            try(var f=new WorkflowTest.Fixture(Files.createTempDirectory("numeric106-persist"))){
                var d=f.data.importCsv(input(c));var w=f.workflows.start(DataSummary104Test.input(f,d).put("maxGapS",c.getDouble("maxGapS")).put("speedLimitMps",c.getDouble("speedLimitMps")));
                var effect=DataSummary104Test.audit(f,w.getString("id"));DataCreation101Test.ack(f,effect,DataSummary102Test.observed(f,effect,d));var done=DataCreation101Test.finish(f,w.getString("id"));assertEquals("completed",done.getString("status"));
                var a=f.artifacts.read(done.getJSONObject("report").getString("id"));String original=a.getJSONObject("contents").getString("inputs/source.json");var parsed=new JSONObject(original);
                assertEquals(original,WorldFrames.copy(parsed).toString());assertEquals(c.getDouble("maxGapS"),parsed.getJSONObject("criteria104").getDouble("maxGapS"),0);assertEquals(c.getDouble("speedLimitMps"),parsed.getJSONObject("criteria104").getDouble("speedLimitMps"),0);assertFalse(parsed.has("summary"));
            }
        }
    }
}
