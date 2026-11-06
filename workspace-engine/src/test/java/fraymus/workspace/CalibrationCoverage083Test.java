package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class CalibrationCoverage083Test {
    private JSONObject pair(String task,boolean warm,boolean retain,boolean skill,String receipt) {
        return new JSONObject().put("taskSha256",task).put("warm",warm).put("receiptId",receipt).put("family","two-stage")
            .put("workload",new JSONObject().put("retain",retain).put("skill",skill));
    }
    private JSONObject model(JSONArray pairs){return new JSONObject().put("id","test").put("profile",TaskCost080.profile()).put("pairs",pairs);}
    @Test public void restartAndSavingGroupsCannotBorrowEachOthersCoverage(){
        var pairs=new JSONArray().put(pair("a",false,false,false,"one")).put(pair("b",true,false,false,"one"))
            .put(pair("c",false,true,true,"one")).put(pair("d",false,false,false,"other"));
        var groups=CalibrationCoverage083.describe(model(pairs)).getJSONArray("groups");assertEquals(4,groups.length());
        for(Object raw:groups)assertFalse(((JSONObject)raw).getBoolean("minimumCountMet"));
    }
    @Test public void repeatedTaskDoesNotPretendToBeTwoDistinctExamples(){
        var g=CalibrationCoverage083.describe(model(new JSONArray().put(pair("a",false,true,true,"one")).put(pair("a",false,true,true,"one")))).getJSONArray("groups").getJSONObject(0);
        assertEquals(2,g.getInt("pairCount"));assertEquals(1,g.getInt("distinctTasks"));assertFalse(g.getBoolean("minimumCountMet"));
    }
    @Test public void matchedCountsAreDescriptiveAndDoNotAuthorizeActions(){
        var m=model(new JSONArray().put(pair("a",false,true,true,"one")).put(pair("b",false,true,true,"one")));String before=m.toString();
        var r=CalibrationCoverage083.describe(m);var g=r.getJSONArray("groups").getJSONObject(0);assertTrue(g.getBoolean("minimumCountMet"));assertEquals("After restart",g.getString("state"));assertEquals("Save experience and skill",g.getString("work"));assertFalse(r.has("useMemory"));assertFalse(r.has("passed"));assertEquals(before,m.toString());
    }
    @Test public void absentAndForeignRuntimeNeverReportRuntimeCompatibility(){
        assertFalse(CalibrationCoverage083.describe(new JSONObject()).getBoolean("profileMatches"));var m=model(new JSONArray().put(pair("a",true,false,false,"one")));m.getJSONObject("profile").put("cpus",999);var r=CalibrationCoverage083.describe(m);assertFalse(r.getBoolean("profileMatches"));assertEquals("Runtime differs from calibration",r.getJSONArray("groups").getJSONObject(0).getString("reason"));
    }
}
