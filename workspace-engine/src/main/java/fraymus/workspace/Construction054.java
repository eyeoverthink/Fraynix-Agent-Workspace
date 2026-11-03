package fraymus.workspace;

import org.json.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

/** Declared variable box stock and fixed obstacles; native motion/contacts remain the evidence. */
final class Construction054 {
 static final String LEGACY_053="05eae61fc6fed8df7e7d1649d7c726773b822bfd20d698a4b5e8811d230d85cb";
 static final String PREFIX="Edition 054 declared box construction: ";
 static final String VERSION="geometry-construction-054-v1";
 static final String SCOPE="Declared rectangular bridge stock and fixed box obstacles; conservative overhead clearance, actual released contacts. No arbitrary path finding, material strength or physical calibration.";
 private Construction054(){}
 static JSONObject protocol(){try(var in=Construction054.class.getResourceAsStream("/construction054-protocol.json")){return new JSONObject(new String(in.readAllBytes(),StandardCharsets.UTF_8));}catch(Exception e){throw new IllegalStateException(e);}}
 static JSONObject configurations(){return new JSONObject().put("cases",protocol().getJSONArray("cases")).put("scope",SCOPE);}
 static JSONObject configuration(JSONObject input){
  SpatialScene.keys(input,"preset","beamLengthM","supportHeightM","spanM","obstacleHeightM");String name=input.optString("preset","wide");JSONObject source=null;
  for(Object raw:protocol().getJSONArray("cases")){var c=(JSONObject)raw;if(c.getString("preset").equals(name)&&!name.equals("legacy"))source=c;}
  if(source==null)throw new IllegalArgumentException("Choose compact, wide, barrier or blocked geometry");
  var out=new JSONObject().put("preset",name);
  for(String k:List.of("beamLengthM","supportHeightM","spanM","obstacleHeightM"))if(input.has(k)){SpatialScene.number(input,k,.1,3);out.put(k,input.get(k));}
  out.put("beamLengthM",out.optDouble("beamLengthM",source.getJSONArray("beamSize").getDouble(0))).put("supportHeightM",out.optDouble("supportHeightM",source.getJSONArray("supportSize").getDouble(1)))
   .put("spanM",out.optDouble("spanM",source.getDouble("spanM"))).put("obstacleHeightM",out.optDouble("obstacleHeightM",source.optDouble("obstacleHeightM",.8)));
  SpatialScene.number(out,"beamLengthM",1.4,2.6);SpatialScene.number(out,"supportHeightM",.35,.9);SpatialScene.number(out,"spanM",.8,2);SpatialScene.number(out,"obstacleHeightM",.2,1.2);
  double w=source.getJSONArray("supportSize").getDouble(0);
  if(out.getDouble("spanM")<w+.2||out.getDouble("spanM")+w>out.getDouble("beamLengthM")-.16)throw new IllegalArgumentException("Beam must cover both supports with at least0.08m end margin");
  return out;
 }
 static JSONObject spec(JSONObject config){for(Object raw:protocol().getJSONArray("cases")){var c=(JSONObject)raw;if(c.getString("preset").equals(config.getString("preset")))return WorldFrames.copy(c);}throw new IllegalArgumentException("Unknown geometry preset");}
 static boolean is(JSONObject scene){return scene.getJSONObject("source").getString("note").startsWith(PREFIX);}
 static JSONObject config(JSONObject scene){if(!is(scene))throw new IllegalArgumentException("Selected scene is not a declared054 kit");var raw=new JSONObject(scene.getJSONObject("source").getString("note").substring(PREFIX.length()));var c=configuration(raw);if(!c.similar(raw))throw new IllegalStateException("Declared geometry configuration changed");return c;}
 static JSONObject kit(JSONObject requested){
  var c=configuration(requested);var s=spec(c);var support=s.getJSONArray("supportSize");support.put(1,c.getDouble("supportHeightM"));var beam=s.getJSONArray("beamSize");beam.put(0,c.getDouble("beamLengthM"));var objects=new JSONArray();
  objects.put(part("floor",0,-.1,0,new JSONArray(new double[]{8,.2,8}),0));
  objects.put(part("support-a",-2.2,support.getDouble(1)/2,-2,support,s.getDouble("supportMassKg")));
  objects.put(part("support-b",-.4,support.getDouble(1)/2,-2,support,s.getDouble("supportMassKg")));
  objects.put(part("beam",2,beam.getDouble(1)/2,-2,beam,s.getDouble("beamMassKg")));
  double h=c.getDouble("obstacleHeightM");
  if(s.getString("obstacle").equals("barrier"))objects.put(part("obstacle-barrier",0,h/2,-.8,new JSONArray(new double[]{5,h,.25}),0));
  if(s.getString("obstacle").equals("blocked-target"))objects.put(part("obstacle-post",-c.getDouble("spanM")/2,h/2,0,new JSONArray(new double[]{.35,h,.35}),0));
  var out=Construction051.kit();out.put("title","Declared054 "+c.getString("preset")+" bridge and obstacles").put("objects",objects).getJSONObject("source").put("note",PREFIX+c.toString());return out;
 }
 private static JSONObject part(String id,double x,double y,double z,JSONArray size,double mass){return new JSONObject().put("sourceId",id).put("label",id).put("shape","box").put("position",new JSONArray(new double[]{x,y,z})).put("size",new JSONArray(size.toString())).put("rotation",WorldPlan.quat(0)).put("massKg",mass).put("friction",.8).put("restitution",0);}
 static void stock(JSONObject scene){
  WorldFrames.verifyScene(scene);var expected=kit(config(scene)).getJSONArray("objects");if(scene.getJSONArray("objects").length()!=expected.length())throw new IllegalArgumentException("Declared stock or obstacle count changed");
  for(Object raw:expected){var e=(JSONObject)raw;var o=WorldPlan.sourceObject(scene,e.getString("sourceId"));
   for(String key:List.of("size","massKg","friction","restitution"))if(!new JSONObject().put("v",e.get(key)).similar(new JSONObject().put("v",o.get(key))))throw new IllegalArgumentException("Declared geometry/material changed: "+e.getString("sourceId"));
   var q=o.getJSONArray("rotation");if(o.getDouble("massKg")==0){if(!o.getJSONArray("position").similar(e.getJSONArray("position"))||!q.similar(e.getJSONArray("rotation")))throw new IllegalArgumentException("Fixed floor/obstacle changed");}
   else if(Math.abs(q.getDouble(0))+Math.abs(q.getDouble(2))>.043||WorldVerify.norm(o.getJSONArray("velocity"))>.2||WorldVerify.norm(o.getJSONArray("angularVelocity"))>.2)throw new IllegalArgumentException("Inspect tilted or unsettled stock before assembly");
  }
 }
 static JSONObject intent(JSONObject scene,JSONObject requested,double force,double torque){
  stock(scene);var o=Construction051.options(requested);var c=config(scene);RampDefinition.range(force,2,60,"gripper force N");RampDefinition.range(torque,0,4,"gripper torque Nm");var targets=new JSONArray();var beam=WorldPlan.sourceObject(scene,"beam");double bh=beam.getJSONArray("size").getDouble(1)/2,sh=c.getDouble("supportHeightM"),span=c.getDouble("spanM");
  if(beam.getJSONArray("position").getDouble(1)>bh+.15)targets.put(target("beam",2,bh,-2));double x=o.getDouble("centerX"),z=o.getDouble("centerZ");
  targets.put(target("support-a",x-span/2,sh/2,z)).put(target("support-b",x+span/2,sh/2,z)).put(target("beam",x,sh+bh+.02,z));
  return new JSONObject().put("binding",SpatialStore.binding(scene)).put("targets",targets).put("forceLimitN",force).put("torqueLimitNm",torque).put("actuator","bounded virtual force/torque gripper").put("construction051",o).put("constructionCodeSha256",Construction051.identity()).put("geometry054",c);
 }
 private static JSONObject target(String id,double x,double y,double z){return new JSONObject().put("sourceId",id).put("positionM",new JSONArray(new double[]{x,y,z})).put("yawDeg",0);}
 static JSONObject plan(JSONObject scene,JSONObject request){
  var expected=intent(scene,request.getJSONObject("construction051"),request.getDouble("forceLimitN"),request.getDouble("torqueLimitNm"));if(!expected.similar(request))throw new IllegalStateException("Geometry construction source/intent changed");
  var bodies=new LinkedHashMap<String,JSONObject>();double top=0,radius=0;
  for(Object raw:scene.getJSONArray("objects")){var b=WorldFrames.copy((JSONObject)raw);if(b.getString("sourceId").equals("floor"))continue;bodies.put(b.getString("sourceId"),b);top=Math.max(top,b.getJSONArray("position").getDouble(1)+b.getJSONArray("size").getDouble(1)/2);if(b.getDouble("massKg")>0)radius=Math.max(radius,WorldPlan.radius(b));}
  for(Object raw:expected.getJSONArray("targets")){var t=(JSONObject)raw;top=Math.max(top,t.getJSONArray("positionM").getDouble(1)+bodies.get(t.getString("sourceId")).getJSONArray("size").getDouble(1)/2);}
  double lift=top+radius+.3;boolean clear=lift<=4;String reason=clear?"Declared geometry and obstacle corridors clear; native support/contact proof still required":"Required overhead clearance exceeds4m";var ordered=new JSONArray();
  for(Object raw:expected.getJSONArray("targets")){var t=(JSONObject)raw;String id=t.getString("sourceId");var from=bodies.get(id);var next=WorldFrames.copy(from).put("position",t.getJSONArray("positionM")).put("rotation",WorldPlan.quat(0));
   if(!columnClear(from,bodies,id)||!columnClear(next,bodies,id)){clear=false;reason="A static obstacle or retained body occupies a pickup/lowering corridor; no actuation admitted";break;}
   for(int k:new int[]{0,2})if(Math.abs(next.getJSONArray("position").getDouble(k))+next.getJSONArray("size").getDouble(k)/2>3.9){clear=false;reason="Declared construction exceeds the fixed floor";}
   ordered.put(WorldFrames.copy(t).put("objectId",from.getString("id")).put("rotation",WorldPlan.quat(0)));bodies.put(id,next);
  }
  return new JSONObject().put("version",VERSION).put("scope",SCOPE).put("intent",expected).put("liftHeightM",lift).put("orderedTargets",ordered).put("passed",clear).put("reason",reason).put("clearance054",new JSONObject().put("highestBodyOrTargetM",top).put("rotationRadiusM",radius).put("marginM",.3));
 }
 private static double[] half(JSONObject o){var s=o.getJSONArray("size");double a=Math.toRadians(WorldPlan.yaw(o.getJSONArray("rotation"))),c=Math.abs(Math.cos(a)),n=Math.abs(Math.sin(a));return new double[]{(c*s.getDouble(0)+n*s.getDouble(2))/2,s.getDouble(1)/2,(n*s.getDouble(0)+c*s.getDouble(2))/2};}
 private static boolean columnClear(JSONObject selected,Map<String,JSONObject> bodies,String id){double[] a=half(selected);var p=selected.getJSONArray("position");for(var e:bodies.entrySet())if(!e.getKey().equals(id)){var b=e.getValue();var q=b.getJSONArray("position");double[] h=half(b);if(Math.abs(p.getDouble(0)-q.getDouble(0))<a[0]+h[0]+.025&&Math.abs(p.getDouble(2)-q.getDouble(2))<a[2]+h[2]+.025&&p.getDouble(1)-a[1]<q.getDouble(1)+h[1]-.005)return false;}return true;}
 static JSONObject verifyStructure(JSONObject run){
  var scene=run.getJSONObject("definition");var p=run.getJSONObject("plan");if(!plan(scene,p.getJSONObject("intent")).similar(p))throw new IllegalStateException("Geometry plan changed");var last=run.getJSONObject("final");String beam=WorldPlan.sourceObject(scene,"beam").getString("id");var supports=new HashSet<String>();var obstacles=new HashSet<String>();
  for(String id:List.of("support-a","support-b"))supports.add(WorldPlan.sourceObject(scene,id).getString("id"));for(Object raw:scene.getJSONArray("objects")){var o=(JSONObject)raw;if(o.getString("sourceId").startsWith("obstacle-"))obstacles.add(o.getString("id"));}
  var contacted=new HashSet<String>();int samples=0,obstacleContacts=0,end=last.getInt("tick");double maxSpeed=0;
  for(Object raw:run.getJSONArray("samples")){var s=(JSONObject)raw;for(Object value:s.getJSONArray("contacts")){var c=(JSONObject)value;String a=c.getString("a"),b=c.getString("b");if(obstacles.contains(a)||obstacles.contains(b))obstacleContacts++;if(s.getInt("tick")>=end-84){if(a.equals(beam)&&supports.contains(b))contacted.add(b);if(b.equals(beam)&&supports.contains(a))contacted.add(a);}}
   if(s.getInt("tick")>=end-84){samples++;for(Object b:s.getJSONArray("bodies"))maxSpeed=Math.max(maxSpeed,WorldVerify.norm(((JSONObject)b).getJSONArray("velocity")));}}
  double height=ContactPhysics.dynamic(last.getJSONArray("bodies"),beam).getJSONArray("position").getDouble(1),expected=config(scene).getDouble("supportHeightM")+WorldPlan.sourceObject(scene,"beam").getJSONArray("size").getDouble(1)/2;
  boolean released=last.getJSONObject("controller").getString("grippingId").isEmpty();boolean passed=contacted.size()==2&&Math.abs(height-expected)<.05&&samples>=6&&maxSpeed<.08&&released&&obstacleContacts==0;
  return new JSONObject().put("passed",passed).put("supportContacts",contacted.size()).put("beamCenterHeightM",height).put("expectedBeamCenterHeightM",expected).put("settledSamples",samples).put("maxSettledSpeedMps",maxSpeed).put("released",released).put("obstacleContacts",obstacleContacts).put("geometry054",config(scene)).put("design",p.getJSONObject("intent").getJSONObject("construction051")).put("scope",SCOPE);
 }
}
