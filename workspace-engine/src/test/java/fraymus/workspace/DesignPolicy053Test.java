package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

/** Explicit request/admission boundary fixtures; no inference or native success is simulated. */
public class DesignPolicy053Test {
 @Test public void taskProjectionRetainsDesignPolicyAndOriginalComparisonWithoutFullTraces(){
  var comparison=new JSONObject().put("passed",true);
  var goal=new JSONObject().put("design053",Design053.policy()).put("counterfactual045",new JSONObject().put("studyId","study").put("data",new JSONObject().put("comparison",comparison).put("largeTrace","private original")));
  var view=GoalRunner.view(goal);
  assertTrue(view.getJSONObject("design053").similar(Design053.policy()));
  assertTrue(view.getJSONObject("counterfactual045").getJSONObject("comparison").similar(comparison));
  assertFalse(view.getJSONObject("counterfactual045").has("data"));
  assertTrue(goal.getJSONObject("counterfactual045").has("data"));
 }
 @Test public void javaPinsCompareReviewAndForceCeilingBeforeAnyAction()throws Exception{
  try(var store=new WorkspaceStore(Files.createTempDirectory("design053-policy-"))){
   var d=new Domain044((route,args)->route.equals("/api/state")?new JSONObject().put("scenes",new JSONArray()).put("datasets",new JSONArray()).put("procedures",new JSONArray()):new JSONObject(),store);
   var g=Domain044Test.task("workbench").put("goal","Compare assembly designs for the bridge at centerX 0.35, forceLimitN 40; do not move anything");d.initialize(g,new JSONObject(),new JSONObject());
   assertEquals("construction",g.getString("family044"));var a=d.prepare(g,"construction.start",new JSONObject());assertTrue(a.getBoolean("design053"));assertTrue(a.getBoolean("reviewCounterfactual045"));assertEquals(40,a.getDouble("forceLimitN"),0);assertEquals(.35,a.getDouble("centerX"),0);
   assertThrows(IllegalArgumentException.class,()->d.prepare(g,"construction.start",new JSONObject().put("forceLimitN",60)));
   assertFalse(Tools044.schema("construction.start",g).getJSONObject("properties").has("design053"));
  }
 }
 @Test public void retainedDesignRespectsExplicitReviewAndFreshSelectedWorld()throws Exception{
  var skill=new JSONObject().put("name","design-bridge").put("version",1).put("kind",Construction051.KIND).put("defaults",new JSONObject().put("design053",true).put("forceLimitN",40).put("torqueLimitNm",4));
  var p=new Procedure044((route,args)->route.equals("/api/state")?new JSONObject().put("procedures",new JSONArray().put(skill)):new JSONObject().put("received",args));
  var in=new JSONObject().put("args",new JSONObject().put("name","design-bridge").put("version",1)).put("bindings",new JSONObject().put("world",new JSONObject().put("worldId","fresh-selected").put("expectedRevision",7).put("expectedHash","current"))).put("request","Reuse procedure design-bridge v1 at centerX .35; review before executing");
  var a=p.run(in).getJSONObject("record").getJSONObject("received");assertTrue(a.getBoolean("reviewCounterfactual045"));assertEquals("fresh-selected",a.getString("worldId"));assertEquals(7,a.getInt("expectedRevision"));
  in.getJSONObject("args").put("worldId","foreign");assertThrows(IllegalArgumentException.class,()->p.run(in));
 }
}
