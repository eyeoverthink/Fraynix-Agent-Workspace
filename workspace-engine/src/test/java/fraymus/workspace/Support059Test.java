package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;

public class Support059Test {
 @Test public void sameStockSpanAndBeamPositionArePreservedWithEndMargins()throws Exception {
  var root=Files.createTempDirectory("support059-geometry-");var worlds=new WorldLedger(root);var input=Load056Test.input(worlds,"wide",.8,0);var before=worlds.observe(input.getString("worldId"));var source=worlds.source(before);var plan=worlds.preview(input);var proposals=SupportDesign059.proposals(source,plan);assertEquals(3,proposals.length());
  for(int i=0;i<3;i++){var p=proposals.getJSONObject(i).getJSONObject("plan");WorldPlan.verify(source,p);var targets=p.getJSONObject("intent").getJSONArray("targets");double a=targets.getJSONObject(0).getJSONArray("positionM").getDouble(0),b=targets.getJSONObject(1).getJSONArray("positionM").getDouble(0);assertEquals(.8,b-a,1e-10);assertEquals(i==0?0:i==1?-.3:.3,(a+b)/2,1e-10);assertEquals(0,targets.getJSONObject(2).getJSONArray("positionM").getDouble(0),0);}
  var invalid=new JSONObject().put("supportSpanM",1.99).put("supportOffsetM",.3);assertThrows(IllegalArgumentException.class,()->Construction051.intent(source,invalid,40,4));assertTrue(before.similar(worlds.observe(input.getString("worldId"))));
 }
 @Test public void nativeOptionsAreExplicitAndDisabledRedesignRefusesBothOffsets()throws Exception {
  var root=Files.createTempDirectory("support059-policy-");var worlds=new WorldLedger(root);var input=Load056Test.input(worlds,"wide",.8,0);
  try(var cf=new Counterfactual045(root,worlds)){var s=cf.prepare(worlds.observe(input.getString("worldId")),worlds.preview(input),true,0,true,true,false,true);assertEquals("asymmetric-payload-059-v1",s.getJSONObject("payloadDesign058").getString("version"));for(int i=1;i<3;i++)assertEquals("rejected",s.getJSONArray("candidates").getJSONObject(i).getString("status"));}
  try(var artifacts=new WorkspaceStore(root.resolve("chat"))){var task=Domain044Test.task("construction").put("goal","Compare asymmetric supports using new payloads");task.getJSONObject("context044").put("designRequested053",true);var args=Domain044Test.domain(artifacts).prepare(task,"construction.start",new JSONObject().put("asymmetricSupports059",false));assertTrue(args.getBoolean("asymmetricSupports059"));assertTrue(args.getBoolean("payloadDesign058"));ToolSchema043.validate(Tools044.schema("construction.start",task),args);}
 }
 @Test public void distinctNewReservedProtocolCannotBeMistakenForDesignOr058(){var train=new PayloadSeries058("design",true).protocol();var reserved=new PayloadSeries058("reserved",true).protocol();assertEquals(3,train.getJSONArray("cases").length());assertEquals(4,reserved.getJSONArray("cases").length());assertFalse(Design058.policy(true,true).similar(Design058.policy(true)));for(Object raw:reserved.getJSONArray("cases")){String id=((JSONObject)raw).getString("id");assertThrows(IllegalArgumentException.class,()->Payload057.specification(train,id));assertThrows(IllegalArgumentException.class,()->Payload057.specification(new PayloadSeries058("reserved").protocol(),id));}}
}
