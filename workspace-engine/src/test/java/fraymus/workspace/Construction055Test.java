package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

public class Construction055Test {
    @Test public void frozenDirectFirstComparisonAndIndependentLoads()throws Exception {
        Path root=Files.createTempDirectory("construction055-matrix-");var results=new JSONArray();var errors=new ArrayList<String>();
        for(String preset:List.of("compact","wide","barrier")) {
            var record=new JSONObject().put("case",preset);results.put(record);Path folder=root.resolve(preset);
            try {
                var worlds=new WorldLedger(folder);var in=Construction054Test.input(worlds,preset);var before=worlds.observe(in.getString("worldId"));var scene=worlds.source(before);var plan=worlds.preview(in);byte[] original=Files.readAllBytes(folder.resolve("persistent-worlds/state.json"));
                try(var direct=new Counterfactual045(folder.resolve("direct"),worlds);var exhaustive=new Counterfactual045(folder.resolve("exhaustive"),worlds)) {
                    var d=direct.prepare(before,plan,true);direct.begin(d.getString("id"));var done=Design053Test.terminal(direct,d.getString("id"));record.put("direct",direct.export(d.getString("id")));assertEquals(done.toString(),"ready",done.getString("status"));assertEquals("maximum-force",done.getJSONObject("decision").getString("selectedCandidate"));
                    assertEquals(1,direct.attempts(d.getString("id")).length());for(int i=1;i<3;i++){var c=done.getJSONArray("candidates").getJSONObject(i);assertEquals("not-needed",c.getString("status"));assertEquals(0,c.getJSONArray("attempts").length());assertFalse(c.has("metrics"));}
                    var e=exhaustive.prepare(before,plan);exhaustive.begin(e.getString("id"));var all=Design053Test.terminal(exhaustive,e.getString("id"));record.put("exhaustive",exhaustive.export(e.getString("id")));assertEquals(all.toString(),"ready",all.getString("status"));assertEquals(3,exhaustive.attempts(e.getString("id")).length());
                    assertTrue(WorldVerify.verify(direct.run(d.getString("id"),"maximum-force")).similar(WorldVerify.verify(exhaustive.run(e.getString("id"),"maximum-force"))));
                    var actual=Construction054Test.nativeRun(folder.resolve("actual"),scene,direct.executePlan(d.getString("id"),done.getString("decisionSha256")));record.put("actual",WorldReport.packed(actual));assertTrue(WorldVerify.verify(actual).getBoolean("passed"));assertTrue(Construction051.verifyStructure(actual).getBoolean("passed"));
                    var comparison=CounterfactualReport045.data(direct,d.getString("id"),actual);record.put("comparison",comparison).put("exhaustiveCosts",exhaustive.costs054(e.getString("id")));assertTrue(comparison.getJSONObject("comparison").getBoolean("passed"));assertTrue(direct.costs054(d.getString("id")).getLong("totalTicks")<exhaustive.costs054(e.getString("id")).getLong("totalTicks"));
                    if(preset.equals("wide")) {
                        var loads=Load055.evaluate(folder.resolve("loads"),actual);record.put("loads",loads).put("originalLoads",Load055.export(folder.resolve("loads"),actual));assertTrue(loads.getBoolean("complete"));assertEquals(3,loads.getInt("attempts"));
                        for(Object value:loads.getJSONArray("results")){var result=(JSONObject)value;assertEquals(result.toString(),!result.getString("caseId").equals("horizontal-overload"),result.getJSONObject("metrics").getBoolean("stable"));}
                        byte[] loadBytes=Files.readAllBytes(folder.resolve("loads").resolve(actual.getString("id")+".json"));assertTrue(loads.similar(Load055.evaluate(folder.resolve("loads"),actual)));assertArrayEquals(loadBytes,Files.readAllBytes(folder.resolve("loads").resolve(actual.getString("id")+".json")));
                    }
                    assertArrayEquals(original,Files.readAllBytes(folder.resolve("persistent-worlds/state.json")));record.put("passed",true);
                }
            } catch(Throwable error){record.put("passed",false).put("error",error.toString());errors.add(preset+": "+error);}
            finally {Files.writeString(root.resolve("results.json"),new JSONObject().put("protocol",Construction055.protocol()).put("results",results).toString());}
        }
        System.out.println("CONSTRUCTION055_MATRIX "+root);assertTrue(errors.toString(),errors.isEmpty());
    }
    @Test public void lateralRoutesUseNativeWaypointsAndSealedWallRefuses()throws Exception {
        Path root=Files.createTempDirectory("construction055-routes-");var results=new JSONArray();var errors=new ArrayList<String>();
        for(String preset:List.of("detour","sealed")){var record=new JSONObject().put("preset",preset);results.put(record);
            try {var worlds=new WorldLedger(root.resolve(preset));var in=Construction054Test.input(worlds,preset);var scene=worlds.source(worlds.observe(in.getString("worldId")));var plan=worlds.preview(in);record.put("scene",scene).put("plan",plan);
                if(preset.equals("sealed")){assertFalse(plan.toString(),plan.getBoolean("passed"));record.put("passed",true);continue;}
                assertTrue(plan.toString(),plan.getBoolean("passed"));assertTrue(plan.getJSONObject("route055").getInt("detours")>=1);assertTrue(plan.getDouble("liftHeightM")<=2);
                var run=Construction054Test.nativeRun(root.resolve("actual"),scene,plan);record.put("run",WorldReport.packed(run));var metrics=WorldVerify.verify(run);var structure=Construction051.verifyStructure(run);record.put("metrics",metrics).put("structure",structure);
                assertTrue(metrics.toString(),metrics.getBoolean("passed"));assertTrue(structure.toString(),structure.getBoolean("passed"));assertEquals(0,structure.getInt("obstacleContacts"));assertTrue(run.getJSONArray("events").toList().stream().map(x->new JSONObject((Map<?,?>)x)).anyMatch(e->e.optInt("stage")>3));
                record.put("passed",true);
            }catch(Throwable error){record.put("passed",false).put("error",error.toString());errors.add(preset+": "+error);}finally{Files.writeString(root.resolve("results.json"),new JSONObject().put("results",results).toString());}
        }System.out.println("CONSTRUCTION055_ROUTES "+root);assertTrue(errors.toString(),errors.isEmpty());
    }
    @Test public void failedDirectTrialExpandsWithoutInventingSuccessfulFallback()throws Exception {
        var root=Files.createTempDirectory("construction055-lowforce-");var worlds=new WorldLedger(root);var in=Construction054Test.input(worlds,"wide").put("forceLimitN",6);var before=worlds.observe(in.getString("worldId"));
        try(var owner=new Counterfactual045(root,worlds)){var s=owner.prepare(before,worlds.preview(in),true);owner.begin(s.getString("id"));var done=Design053Test.terminal(owner,s.getString("id"));Files.writeString(root.resolve("result.json"),owner.export(s.getString("id")).toString());assertEquals("failed",done.getString("status"));assertEquals(3,owner.attempts(s.getString("id")).length());assertFalse(done.has("decision"));assertTrue(before.similar(worlds.observe(in.getString("worldId"))));}System.out.println("CONSTRUCTION055_FAILURE "+root);
    }
}
