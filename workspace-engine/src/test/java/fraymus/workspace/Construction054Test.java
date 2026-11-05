package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

/** Original native runs from frozen declared cases; no model/report pixels are simulated here. */
public class Construction054Test {
 static JSONObject input(WorldLedger worlds,String preset)throws Exception{
  var kit=preset.equals("legacy")?Construction051.kit():Construction054.kit(new JSONObject().put("preset",preset));
  var scene=SpatialScene.parse(kit.toString(),UUID.randomUUID().toString());var w=worlds.create(scene,"LABELLED054 "+preset);var o=worlds.observe(w.getString("id"));var in=new JSONObject();for(String k:List.of("worldId","expectedRevision","expectedHash"))in.put(k,o.get(k));
  return in.put("construction051",Construction051.options(new JSONObject())).put("forceLimitN",40).put("torqueLimitNm",4);
 }
 static JSONObject nativeRun(Path root,JSONObject scene,JSONObject plan)throws Exception{try(var store=new WorldNativeStore(root)){String id=UUID.randomUUID().toString();store.start(id,UUID.randomUUID().toString(),scene,plan,false,true);return WorldLedgerTest.settled(store,id);}}
 @Test public void frozenGeometryMatrixComparesFreshNativeAttemptsAndPreservesEveryOutcome()throws Exception{
  Path root=Files.createTempDirectory("construction054-matrix-");var results=new JSONArray();var errors=new ArrayList<String>();
  for(Object raw:Construction054.protocol().getJSONArray("cases")){var c=(JSONObject)raw;String preset=c.getString("preset");Path folder=root.resolve(c.getString("id"));var result=new JSONObject().put("case",c);results.put(result);
   try{var worlds=new WorldLedger(folder);var in=input(worlds,preset);var before=worlds.observe(in.getString("worldId"));byte[] bytes=Files.readAllBytes(folder.resolve("persistent-worlds/state.json"));var scene=worlds.source(before);var plan=worlds.preview(in);result.put("scene",scene).put("plan",plan);
    if(c.getString("expect").equals("admission-refused")){assertFalse(plan.getBoolean("passed"));assertThrows(IllegalStateException.class,()->WorldPlan.verify(scene,plan));assertTrue(before.similar(worlds.observe(in.getString("worldId"))));result.put("passed",true).put("outcome","admission-refused; zero native attempts");continue;}
    assertTrue(plan.getString("reason"),plan.getBoolean("passed"));long started=System.nanoTime();var simple=nativeRun(folder.resolve("independent-simple"),scene,plan);result.put("simpleWallMillis",(System.nanoTime()-started)/1_000_000.0).put("simple",WorldReport.packed(simple));var sm=WorldVerify.verify(simple);result.put("simpleMetrics",sm).put("simpleStructure",Construction051.verifyStructure(simple));
    try(var study=new Counterfactual045(folder,worlds)){var s=study.prepare(before,plan);study.begin(s.getString("id"));var ready=Design053Test.terminal(study,s.getString("id"));result.put("study",study.export(s.getString("id")));assertEquals(ready.toString(),"ready",ready.getString("status"));
     var max=study.run(s.getString("id"),"maximum-force");assertTrue("Independent maximum-force repetition differs from actual candidate",WorldVerify.verify(max).similar(sm));assertTrue(sm.toString(),sm.getBoolean("passed"));assertTrue(Construction051.verifyStructure(simple).getBoolean("passed"));
     var selected=study.executePlan(s.getString("id"),ready.getString("decisionSha256"));var actual=nativeRun(folder.resolve("selected-actual"),scene,selected);result.put("actual",WorldReport.packed(actual));var am=WorldVerify.verify(actual);assertTrue(am.toString(),am.getBoolean("passed"));assertTrue(Construction051.verifyStructure(actual).getBoolean("passed"));
     var data=CounterfactualReport045.data(study,s.getString("id"),actual);assertTrue(data.getJSONObject("comparison").getBoolean("passed"));result.put("comparison",data).put("study",study.export(s.getString("id")));
     assertEquals(preset.equals("legacy")?9:11,CounterfactualReport045.checks(data).length());
     if(!preset.equals("legacy")){var b=data.getJSONObject("baseline054");assertTrue(b.getBoolean("baselineFeasible"));assertEquals(3,b.getInt("trialAttempts"));assertEquals(b.getLong("allTrialTicks")+am.getInt("nativeTicks"),b.getLong("comparisonPlusExecutionTicks"));assertTrue(b.getLong("comparisonPlusExecutionTicks")>sm.getInt("nativeTicks"));assertTrue(b.getBoolean("wallTimeComplete"));assertEquals(0,Construction051.verifyStructure(actual).getInt("obstacleContacts"));}
     if(preset.equals("barrier")){var altered=WorldFrames.copy(actual);var sample=altered.getJSONArray("samples").getJSONObject(0);sample.getJSONArray("contacts").put(new JSONObject().put("a",WorldPlan.sourceObject(scene,"obstacle-barrier").getString("id")).put("b",WorldPlan.sourceObject(scene,"beam").getString("id")).put("distanceM",0).put("normalImpulseNs",1));assertFalse("Any obstacle contact must defeat the new structure gate",Construction051.verifyStructure(altered).getBoolean("passed"));}
    }
    assertArrayEquals(bytes,Files.readAllBytes(folder.resolve("persistent-worlds/state.json")));result.put("passed",true).put("outcome","original native motion, support, baseline and exact source checks passed");
   }catch(Throwable error){result.put("passed",false).put("error",error.toString());errors.add(c.getString("id")+": "+error);}
   finally{Files.writeString(root.resolve("results.json"),new JSONObject().put("protocol",Construction054.protocol()).put("results",results).toString());}
  }
  System.out.println("CONSTRUCTION054_MATRIX "+root);assertTrue(errors.toString(),errors.isEmpty());
 }
 @Test public void invalidStockAndOccupiedCorridorsRefuseBeforeNativeEffects()throws Exception{
  assertThrows(IllegalArgumentException.class,()->Construction054.configuration(new JSONObject().put("preset","wide").put("spanM",2).put("beamLengthM",1.4)));
  assertThrows(IllegalArgumentException.class,()->Construction054.configuration(new JSONObject().put("preset","wide").put("supportHeightM","0.75")));
  assertThrows(IllegalArgumentException.class,()->Construction054.configuration(new JSONObject().put("preset","wide").put("script","run")));
  Path root=Files.createTempDirectory("construction054-admission-");var w=new WorldLedger(root);var in=input(w,"barrier");var before=w.observe(in.getString("worldId"));in.getJSONObject("construction051").put("centerZ",-.5);assertFalse(w.preview(in).getBoolean("passed"));assertTrue(before.similar(w.observe(in.getString("worldId"))));
  var edited=w.source(before);WorldPlan.sourceObject(edited,"obstacle-barrier").getJSONArray("position").put(1,0);assertThrows(IllegalStateException.class,()->Construction054.stock(edited));
 }
 @Test public void declaredDimensionsAreDataAndBaselineRemainsVisibleInProjection(){
  var c=Construction054.configuration(new JSONObject().put("preset","compact").put("beamLengthM",1.8).put("spanM",1.2));var kit=Construction054.kit(c);assertEquals(1.8,kit.getJSONArray("objects").getJSONObject(3).getJSONArray("size").getDouble(0),0);
  var g=new JSONObject().put("counterfactual045",new JSONObject().put("data",new JSONObject().put("comparison",new JSONObject().put("passed",true)).put("baseline054",new JSONObject().put("verdict","Original native comparison"))));
  assertEquals("Original native comparison",GoalRunner.view(g).getJSONObject("counterfactual045").getJSONObject("baseline054").getString("verdict"));assertTrue(g.getJSONObject("counterfactual045").has("data"));
 }
}
