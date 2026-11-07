package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class TaskCost084Test {
    static JSONObject model(int repeats) {
        var m=TaskCost080Test.model(3000,4000).put("version",TaskCost084.VERSION);var pairs=new JSONArray();
        for(int n=0;n<2;n++)for(int r=0;r<repeats;r++)pairs.put(TaskCost080.pair(
            TaskCost080Test.measured("s"+n+"-"+r,"simple",10000+n*2000+r*100,n),
            TaskCost080Test.measured("m"+n+"-"+r,"memory",7000+n*1000-r*100,n)));
        return m.put("pairs",pairs);
    }
    static JSONObject estimate(JSONObject m){return TaskCost084.estimate(TaskCost080Test.candidate(),TaskCost080Test.spec(0),TaskCost080Test.workload(),m);}
    @Test public void realRepeatsAndTwoTasksRequired(){assertFalse(estimate(model(1)).getBoolean("available"));assertFalse(estimate(model(2)).getBoolean("available"));var e=estimate(model(3));assertTrue(e.getBoolean("available"));assertEquals(6,e.getJSONArray("neighbors").length());assertEquals(2,e.getInt("distinctTasks"));assertEquals(3700,e.getDouble("savingMillis"),1e-9);assertEquals(500,e.getDouble("withinTaskMillis"),1e-9);assertEquals(500,e.getDouble("betweenTaskMillis"),1e-9);}
    @Test public void repeatedNearestTaskCannotCrowdOutDiversity(){var m=model(4);assertFalse(TaskCost080.estimate(TaskCost080Test.candidate(),TaskCost080Test.spec(0),TaskCost080Test.workload(),m).getBoolean("available"));assertTrue(estimate(m).getBoolean("available"));assertEquals(8,estimate(m).getJSONArray("neighbors").length());}
    @Test public void moreRepeatsDoNotShrinkNextTaskVariation(){var m=model(3),p=m.getJSONArray("pairs").getJSONObject(0);p.put("simpleMillis",15000).put("savingMillis",8000);var e=estimate(m);assertTrue(e.getDouble("withinTaskMillis")>=5100);assertTrue(e.getDouble("marginMillis")>=e.getDouble("withinTaskMillis")+e.getDouble("betweenTaskMillis"));assertFalse(TaskCost084.choose(TaskCost080Test.spec(0),new JSONArray().put(TaskCost080Test.candidate()),TaskCost080Test.workload(),m).getBoolean("useMemory"));}
    @Test public void unequalRepeatsWeightTasksEqually(){var m=model(3),p=new JSONObject(m.getJSONArray("pairs").getJSONObject(0).toString());p.put("simpleId","s-extra").put("memoryId","m-extra");m.getJSONArray("pairs").put(p);assertEquals(3675,estimate(m).getDouble("savingMillis"),1e-9);}
    @Test public void duplicateGoalCannotManufactureIndependentRepeats(){var m=model(3);m.getJSONArray("pairs").getJSONObject(4).put("simpleId","s0-0");assertFalse(estimate(m).getBoolean("available"));}
    @Test public void unavailableVersionsAndRuntimeDoNotGuess(){assertFalse(estimate(model(3).put("version",TaskCost080.VERSION)).getBoolean("available"));var m=model(3);m.getJSONObject("profile").put("cpus",999);assertFalse(estimate(m).getBoolean("available"));assertFalse(estimate(new JSONObject()).getBoolean("available"));}
    @Test public void receiptWarmAndSavingBoundariesStaySeparate(){var m=model(3);for(var c:new JSONObject[]{TaskCost080Test.candidate().put("id","different"),TaskCost080Test.candidate().put("warm",false),TaskCost080Test.candidate().put("family","direct")})assertFalse(TaskCost084.estimate(c,TaskCost080Test.spec(0),TaskCost080Test.workload(),m).getBoolean("available"));for(String k:new String[]{"retain","skill"})assertFalse(TaskCost084.estimate(TaskCost080Test.candidate(),TaskCost080Test.spec(0),TaskCost080Test.workload().put(k,true),m).getBoolean("available"));}
    @Test public void rejectsNonfiniteTiming(){var m=model(3);m.getJSONArray("pairs").getJSONObject(0).put("simpleMillis",-1);assertFalse(estimate(m).getBoolean("available"));}
    @Test public void coverageDescribesRepeatReadinessWithoutAuthority(){var c=CalibrationCoverage083.describe(model(3)).getJSONArray("groups").getJSONObject(0);assertEquals(2,c.getInt("repeatedTasks"));assertTrue(c.getBoolean("repeatCountMet"));assertFalse(c.has("useMemory"));assertFalse(CalibrationCoverage083.describe(model(1)).getJSONArray("groups").getJSONObject(0).getBoolean("repeatCountMet"));}
}
