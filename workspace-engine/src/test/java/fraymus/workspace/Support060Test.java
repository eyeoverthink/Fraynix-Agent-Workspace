package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;

public class Support060Test {
 static JSONObject pose(double x,double z,double yaw){return new JSONObject().put("x",x).put("z",z).put("yawDeg",yaw);}
 @Test public void independentPositionsRotationsAndStockArePreserved()throws Exception{
  var root=Files.createTempDirectory("support060-geometry-");var worlds=new WorldLedger(root);var input=Load056Test.input(worlds,"compact",1,0);var before=worlds.observe(input.getString("worldId"));var source=worlds.source(before);String sha=WorldFrames.hash(source);var proposals=SupportRepair060.proposals(source,worlds.preview(input));
  assertEquals(3,proposals.length());double[][] xs={{-.5,.5},{-.5,.47},{-.47,.47}};
  for(int i=0;i<3;i++){var p=proposals.getJSONObject(i).getJSONObject("plan");WorldPlan.verify(source,p);assertTrue(p.getBoolean("passed"));var targets=p.getJSONArray("orderedTargets");for(int k=0;k<2;k++){assertEquals(xs[i][k],targets.getJSONObject(k).getJSONArray("positionM").getDouble(0),1e-10);assertEquals(i==2||i==1&&k==1?90:0,WorldPlan.yaw(targets.getJSONObject(k).getJSONArray("rotation")),1e-6);}assertEquals(0,targets.getJSONObject(2).getJSONArray("positionM").getDouble(0),0);assertEquals(40,p.getJSONObject("intent").getDouble("forceLimitN"),0);assertEquals(4,p.getJSONObject("intent").getDouble("torqueLimitNm"),0);}
  assertEquals(sha,WorldFrames.hash(source));assertTrue(before.similar(worlds.observe(input.getString("worldId"))));
  var independent=new JSONObject().put("supportA060",pose(-.5,.04,0)).put("supportB060",pose(.47,0,90));var intent=Construction051.intent(source,independent,40,4);assertEquals(.04,intent.getJSONArray("targets").getJSONObject(0).getJSONArray("positionM").getDouble(2),0);
 }
 @Test public void incompleteMixedNonfiniteAndUncoveredPlacementsAreRejected()throws Exception{
  var root=Files.createTempDirectory("support060-bounds-");var worlds=new WorldLedger(root);var input=Load056Test.input(worlds,"compact",1,0);var source=worlds.source(worlds.observe(input.getString("worldId")));
  var valid=new JSONObject().put("supportA060",pose(-.47,0,90)).put("supportB060",pose(.47,0,90));
  for(String key:new String[]{"supportSpanM","supportOffsetM"}){var mixed=WorldFrames.copy(valid).put(key,key.equals("supportSpanM")?1:0);assertThrows(IllegalArgumentException.class,()->Construction051.options(mixed));}
  var missing=WorldFrames.copy(valid);missing.remove("supportB060");assertThrows(IllegalArgumentException.class,()->Construction051.options(missing));
  for(JSONObject bad:new JSONObject[]{pose(.48,0,90),pose(.47,.01,90),pose(.1,0,90),pose(.47,0,45),new JSONObject().put("x","NaN").put("z",0).put("yawDeg",90)}){var v=WorldFrames.copy(valid).put("supportB060",bad);assertThrows(IllegalArgumentException.class,()->Construction051.intent(source,v,40,4));}
 }
 @Test public void exactIndependentPolicyAndChatCannotBecomeAsymmetric()throws Exception{
  var root=Files.createTempDirectory("support060-policy-");var worlds=new WorldLedger(root);var input=Load056Test.input(worlds,"compact",1,0);
  try(var cf=new Counterfactual045(root,worlds)){var b=worlds.observe(input.getString("worldId"));var p=worlds.preview(input);assertThrows(IllegalArgumentException.class,()->cf.prepare(b,p,true,0,true,true,true,true,true));assertThrows(IllegalArgumentException.class,()->cf.prepare(b,p,true,0,true,false,true,false,true));var s=cf.prepare(b,p,true,0,true,true,false,false,true);assertEquals("independent-support-060-v1",s.getJSONObject("payloadDesign058").getString("version"));for(int i=1;i<3;i++)assertEquals("rejected",s.getJSONArray("candidates").getJSONObject(i).getString("status"));}
  try(var artifacts=new WorkspaceStore(root.resolve("chat"))){var task=Domain044Test.task("construction").put("goal","Repair compact stock with independent supports and unfamiliar payloads");task.getJSONObject("context044").put("designRequested053",true);var args=Domain044Test.domain(artifacts).prepare(task,"construction.start",new JSONObject());assertTrue(args.getBoolean("independentSupports060"));assertFalse(args.optBoolean("asymmetricSupports059"));assertTrue(args.getBoolean("payloadDesign058"));ToolSchema043.validate(Tools044.schema("construction.start",task),args);}
  assertFalse(Design058.policy(true,false,true).similar(Design058.policy(true,true)));var train=new PayloadSeries058("design",false,true).protocol();var reserved=new PayloadSeries058("reserved",false,true).protocol();assertEquals(3,train.getJSONArray("cases").length());assertEquals(4,reserved.getJSONArray("cases").length());for(Object raw:reserved.getJSONArray("cases")){String id=((JSONObject)raw).getString("id");assertThrows(IllegalArgumentException.class,()->Payload057.specification(train,id));assertThrows(IllegalArgumentException.class,()->Payload057.specification(new PayloadSeries058("reserved",true).protocol(),id));}
 }
 @Test public void tooNarrowRotatedBearingIsRetainedAsRefusalNotAnExecutableRoute()throws Exception{
  var root=Files.createTempDirectory("support060-refusal-");var worlds=new WorldLedger(root);var input=Load056Test.input(worlds,"wide",1.9,0);var source=worlds.source(worlds.observe(input.getString("worldId")));var proposals=SupportRepair060.proposals(source,worlds.preview(input));assertTrue(proposals.getJSONObject(0).getJSONObject("plan").getBoolean("passed"));for(int i=1;i<3;i++){var p=proposals.getJSONObject(i).getJSONObject("plan");assertFalse(p.getBoolean("passed"));assertTrue(p.getBoolean("rejectedGeometry060"));assertTrue(p.getJSONArray("orderedTargets").isEmpty());assertTrue(p.getString("reason").contains("cover beam width"));}
 }

}
