package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

/** Native execution is real; typed UI observations here are unit fixtures, not browser evidence. */
public class Calibrated159Test {
 static JSONObject scene()throws Exception{
  var s=WorldFrames.copy(WorldLedgerTest.suite().getJSONArray("cases").getJSONObject(0).getJSONObject("source"));var objects=s.getJSONArray("objects");
  while(objects.length()>3)objects.remove(objects.length()-1);
  for(int i=1;i<3;i++)objects.getJSONObject(i).put("size",new JSONArray("[0.2,0.2,0.2]")).put("massKg",.2).put("position",new JSONArray(new double[]{i==1?-.4:.7,.1,.2})).put("rotation",new JSONArray("[0,0,0,1]"));
  return SpatialScene.parse(s.toString(),UUID.randomUUID().toString());
 }
 static JSONObject input(String reg,JSONObject world){return SkillGraph046Test.binding(world).put("profile159",CalibratedAction159.KIND).put("registrationId",reg).put("markers",new JSONArray().put(new JSONObject().put("sourceId","A").put("landmark","landmark4"))).put("returnMarker","B").put("request","Run a calibrated world action").put("title","Explicit159 test").put("modelName","Java unit fixture").put("saveSkillName","calibrated-native-test");}
 static JSONObject install(Alignment041Test.Fixture seed,SkillGraph046Test.F f)throws Exception{f.workflows.install159(new CalibratedPlane159(seed.root,seed.store,f.goals.worlds()));var w=f.goals.worlds().create(scene(),"159 two real simulation markers");return input(seed.reg.getString("id"),f.goals.worlds().observe(w.getString("id")));}
 @Test public void denseIntervalProjectionContainsIndependentPointsAndBudgetsGrow()throws Exception{
  try(var f=new Alignment041Test.Fixture()){var service=new CalibratedPlane159(f.root,f.store,f.ledger);var source=service.source(f.reg.getString("id"));var b=CalibratedPlane159.bounds(source,"landmark4",2,.003);assertEquals(.075,b.getJSONArray("centerM").getDouble(0),1e-12);assertEquals(-.075,b.getJSONArray("centerM").getDouble(2),1e-12);assertFalse(b.getBoolean("probabilityCalibrated"));
   for(int i=0;i<=40;i++)for(int k=0;k<=40;k++){double px=.3+(i/40.0*2-1)*2/400,py=.7+(k/40.0*2-1)*2/300;double x=(px-.1)*.375,z=-(.9-py)*.375;assertTrue(x>=b.getJSONArray("minimumM").getDouble(0)&&x<=b.getJSONArray("maximumM").getDouble(0));assertTrue(z>=b.getJSONArray("minimumM").getDouble(2)&&z<=b.getJSONArray("maximumM").getDouble(2));}
   assertTrue(CalibratedPlane159.bounds(source,"landmark4",4,.006).getDouble("radiusM")>b.getDouble("radiusM"));assertThrows(IllegalArgumentException.class,()->CalibratedPlane159.bounds(source,"landmark0",2,.003));assertThrows(IllegalArgumentException.class,()->CalibratedPlane159.bounds(source,"landmark4",2,.001));
  }
 }
 @Test public void perspectiveIntervalsAndRotatedBasisContainDenseIndependentEvaluation(){
  var g=Alignment041Test.geometry().put("imagePoints",new JSONArray("[[0.12,0.84],[0.87,0.9],[0.8,0.17],[0.2,0.12]]"));var plane=new PlaneCalibration(g);var marks=Alignment041Test.landmarks();for(int i=0;i<4;i++)marks.getJSONObject(i).put("image",g.getJSONArray("imagePoints").getJSONArray(i));
  for(Object raw:marks){var l=(JSONObject)raw;double[] xy=plane.project(l.getJSONArray("image").getDouble(0),l.getJSONArray("image").getDouble(1));l.put("native",new JSONArray(new double[]{-xy[1],0,-xy[0]}));}
  var basis=Alignment041Test.basis().put("u",new JSONArray("[0,0,-1]")).put("v",new JSONArray("[-1,0,0]"));
  var t=Alignment041Test.tape();double[] a=plane.project(.3,.5),b=plane.project(.6,.5);t.put("markB",Math.hypot(a[0]-b[0],a[1]-b[1]));var fit=MatRegistration041.fit(g,basis,marks,t);assertTrue(fit.toString(),fit.getBoolean("passed"));
  var source=new JSONObject().put("registration",new JSONObject().put("geometry",g)).put("fit",fit).put("imageWidth",800).put("imageHeight",600);var region=CalibratedPlane159.bounds(source,"landmark4",3,.003);
  for(int i=0;i<=30;i++)for(int k=0;k<=30;k++){double[] p=plane.project(.3+(i/30.0*2-1)*3/800,.7+(k/30.0*2-1)*3/600);double[] q={-p[1],0,-p[0]};for(int axis:new int[]{0,2})assertTrue(q[axis]>=region.getJSONArray("minimumM").getDouble(axis)&&q[axis]<=region.getJSONArray("maximumM").getDouble(axis));}
 }
 @Test public void sourceByteChangesAndMetadataCannotBecomeCalibration()throws Exception{
  try(var f=new Alignment041Test.Fixture()){var service=new CalibratedPlane159(f.root,f.store,f.ledger);var first=service.source(f.reg.getString("id"));Path p=f.root.resolve("alignment-041/frames/"+f.reg.getString("id")+"/0.image");byte[] original=Files.readAllBytes(p),bad=original.clone();bad[bad.length/2]^=1;Files.write(p,bad);assertThrows(IllegalStateException.class,()->service.source(f.reg.getString("id")));Files.write(p,original);assertTrue(first.similar(service.source(f.reg.getString("id"))));p=p.resolveSibling("0.meta.json");var meta=new JSONObject(Files.readString(p)).put("seconds",999);Files.writeString(p,meta.toString());assertThrows(IllegalStateException.class,()->service.source(f.reg.getString("id")));}
 }
 @Test public void budgetsAndDestinationRevisionsRefuseBeforeActuation()throws Exception{
  try(var seed=new Alignment041Test.Fixture();var f=new SkillGraph046Test.F(seed.root)){var in=install(seed,f);assertTrue(f.workflows.preview159(in).getJSONObject("decision159").getJSONObject("plan").getBoolean("passed"));
   for(var bad:List.of(WorldFrames.copy(in).put("expectedRevision",.5),WorldFrames.copy(in).put("maxRadiusM",.001),WorldFrames.copy(in).put("maxMoveM",.01),WorldFrames.copy(in).put("maxTotalErrorM",.01),WorldFrames.copy(in).put("hardware",true)))assertThrows(Exception.class,()->f.workflows.preview159(bad));
   var before=f.goals.worlds().observe(in.getString("worldId"));f.goals.worlds().restore(in.getString("worldId"),0,in.getString("expectedHash"),0,"");assertThrows(IllegalStateException.class,()->f.workflows.preview159(in));assertEquals(0,f.goals.list().length());assertFalse(before.has("pending"));
  }
 }
 @Test public void failedChecksNonPlanarBasisEdgesAndNonfiniteInputsCannotAcquireAuthority()throws Exception{
  try(var seed=new Alignment041Test.Fixture()){
   var service=new CalibratedPlane159(seed.root,seed.store,seed.ledger);var source=service.source(seed.reg.getString("id"));var edge=WorldFrames.copy(source);edge.getJSONObject("fit").getJSONArray("landmarks").getJSONObject(4).put("image",new JSONArray("[0.1001,0.5]"));assertThrows(IllegalArgumentException.class,()->CalibratedPlane159.bounds(edge,"landmark4",2,.003));
   assertThrows(IllegalArgumentException.class,()->CalibratedPlane159.bounds(source,"landmark4",Double.NaN,.003));var bad=WorldFrames.copy(seed.reg);bad.getJSONObject("fit").put("scale",1.01);assertThrows(IllegalStateException.class,()->CalibratedPlane159.reconstruct(bad));
   var d=input(seed.reg.getString("id"),seed.observed).put("returnMarker","A");assertThrows(IllegalArgumentException.class,()->CalibratedAction159.definition(d));assertEquals("workflow",ChatPolicy044.route("Run a calibrated world action",new JSONObject(),false));
  }
 }
 @Test(timeout=240000) public void stricterRequestedEndpointBudgetRefusesBeforeAcceptedWorldCommit()throws Exception{
  try(var seed=new Alignment041Test.Fixture();var f=new SkillGraph046Test.F(seed.root)){
   var in=install(seed,f).put("executionToleranceM",.00001);String id=f.workflows.start(in).getString("id");
   var stopped=SkillGraph046Test.waitFor(f,()->{var w=f.workflows.read(id);return w.getString("status").equals("running")?null:w;});
   assertEquals(stopped.toString(),"interrupted",stopped.getString("status"));assertTrue(stopped.getString("reason"),stopped.getString("reason").contains("frozen position/error budget"));
   assertEquals(0,f.goals.worlds().observe(in.getString("worldId")).getInt("expectedRevision"));assertEquals(0,f.goals.procedures().length());f.workflows.cancel(id);
  }
 }
 static JSONObject host(JSONObject w,JSONObject a)throws Exception{
  var d=CalibratedReport159.data(w);var cells=new JSONArray();for(Object r:d.getJSONArray("rows"))cells.put(CalibratedReport159.cells((JSONObject)r));var p=d.getJSONArray("plot");var states=new JSONArray();
  for(String mode:List.of("all","bounds","actual")){var rects=new JSONArray();var circles=new JSONArray();for(Object raw:d.getJSONArray("rows")){var row=(JSONObject)raw;var b=row.getJSONObject("bound");double[] lo=xy(b.getJSONArray("minimumM"),p),hi=xy(b.getJSONArray("maximumM"),p),v=xy(row.getJSONObject("outcome").getJSONArray("actualPositionM"),p);if(!mode.equals("actual"))rects.put(new JSONObject().put("x",(double)(float)lo[0]).put("y",(double)(float)lo[1]).put("width",(double)(float)(hi[0]-lo[0])).put("height",(double)(float)(hi[1]-lo[1])).put("visible",true));if(!mode.equals("bounds"))circles.put(new JSONObject().put("cx",(double)(float)v[0]).put("cy",(double)(float)v[1]).put("visible",true));}states.put(new JSONObject().put("mode",mode).put("visible",true).put("rectangles",rects).put("circles",circles));}
  var s=d.getJSONObject("source");var o=d.getJSONObject("outcome");var values=new ArrayList<String>();for(Object n:p)values.add(CalibratedReport159.number(((Number)n).doubleValue()));
  return new JSONObject().put("id",a.getString("id")).put("version",a.getInt("version")).put("rows",cells).put("limits",d.getString("limits")).put("states",states).put("source","Registration "+s.getJSONObject("registration").getString("id")+" / original frame "+s.getString("frameSha256")+". Recorded reference only.").put("world","Committed simulation goal "+o.getString("goalId")+" / revision hash "+o.getString("commitHash")+". No hardware action or live object pose.").put("checks","Independent grid residual: "+CalibratedReport159.number(s.getJSONObject("fit").getDouble("maxCheckErrorM"))+" m; tape discrepancy: "+CalibratedReport159.number(s.getJSONObject("fit").getJSONObject("tape").getDouble("absoluteErrorM"))+" m. Declared budgets are not measured coverage probabilities.").put("axes","Native X and Z in metres. Display limits: "+String.join(", ",values));
 }
 static double[] xy(JSONArray a,JSONArray p){return new double[]{40+(a.getDouble(0)-p.getDouble(0))*720/(p.getDouble(2)-p.getDouble(0)),40+(a.getDouble(2)-p.getDouble(1))*370/(p.getDouble(3)-p.getDouble(1))};}
 @Test(timeout=240000) public void actualNativeActionRestartWrongAuditAndProcedureKeepEveryOriginalCheck()throws Exception{
  try(var seed=new Alignment041Test.Fixture()){
   String id,child;JSONObject input;
   try(var f=new SkillGraph046Test.F(seed.root)){input=install(seed,f).put("reviewBeforeAction159",true);id=f.workflows.start(input).getString("id");var paused=SkillGraph046Test.waitFor(f,()->{var w=f.workflows.read(id);return w.getString("status").equals("running")?null:w;});assertEquals("calibrated-world159",paused.getString("step"));assertEquals(0,f.goals.list().length());}
   try(var f=new SkillGraph046Test.F(seed.root)){f.workflows.install159(new CalibratedPlane159(seed.root,seed.store,f.goals.worlds()));f.workflows.resume(id);var effect=SkillGraph046Test.waitFor(f,()->{var w=f.workflows.read(id);assertNotEquals(w.optString("reason"),"interrupted",w.getString("status"));var e=f.workflows.claim();return e.has("id")?e:null;});var w=f.workflows.read(id);child=w.getJSONArray("stages").getJSONObject(1).getString("goalId");var a=f.artifacts.read(w.getJSONObject("report").getString("id"));var wrong=host(w,a);wrong.getJSONArray("rows").getJSONArray(0).put(2,"999");var failed=f.workflows.acknowledge(new JSONObject().put("workflowId",id).put("id",effect.getString("id")).put("nonce",effect.getString("nonce")).put("result",wrong));assertEquals("interrupted",failed.getString("status"));assertEquals(0,f.goals.procedures().length());}
   try(var f=new SkillGraph046Test.F(seed.root)){f.workflows.install159(new CalibratedPlane159(seed.root,seed.store,f.goals.worlds()));f.workflows.resume(id);var effect=SkillGraph046Test.waitFor(f,()->{var e=f.workflows.claim();return e.has("id")?e:null;});var w=f.workflows.read(id);assertEquals(child,w.getJSONArray("stages").getJSONObject(1).getString("goalId"));var a=f.artifacts.read(w.getJSONObject("report").getString("id"));f.workflows.acknowledge(new JSONObject().put("workflowId",id).put("id",effect.getString("id")).put("nonce",effect.getString("nonce")).put("result",host(w,a)));var done=SkillGraph046Test.waitFor(f,()->{var x=f.workflows.read(id);return x.getString("status").equals("running")?null:x;});assertEquals(done.optString("reason"),"completed",done.getString("status"));assertEquals(1,f.goals.procedures().length());assertTrue(f.workflows.verifyArchive110(id).getBoolean("passed"));assertEquals(1,f.goals.worlds().observe(input.getString("worldId")).getInt("expectedRevision"));assertFalse(f.goals.workflowReserved());}
  }
 }
}
