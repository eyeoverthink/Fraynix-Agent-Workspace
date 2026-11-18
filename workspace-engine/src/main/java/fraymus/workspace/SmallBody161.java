package fraymus.workspace;
import org.json.*;
import java.nio.file.*;
import java.util.*;

/** Explicit small-box experiment domain. Existing native/world identities are untouched. */
final class SmallBody161 {
 static final String SCOPE="One 5..15 cm upright box, 20..200 g, on a declared flat floor. Bounded virtual force/torque grip; native simulation, not a robot, measured object geometry or live camera authority.";
 static String identity(){var text=new StringBuilder("small-domain-161-v1|").append(MapCheckpoint.identity());for(Class<?> c:List.of(SmallBody161.class,SmallPhysics161.class,SmallVerify161.class,SmallStore161.class,SmallReport161.class))try(var in=c.getResourceAsStream(c.getSimpleName()+".class")){text.append(PerceptionStore.hash(in.readAllBytes()));}catch(Exception e){throw new IllegalStateException(e);}return MeasurementStore.hash(text.toString());}
 static double margin(JSONObject o){var s=o.getJSONArray("size");return Math.min(.002,Math.min(s.getDouble(0),Math.min(s.getDouble(1),s.getDouble(2)))*.02);}
 static JSONObject authored(JSONObject imported){var s=SpatialScene.parse(imported.getString("sourceText"),imported.getString("id"));for(Object raw:s.getJSONArray("objects")){var o=(JSONObject)raw;o.put("velocity",new JSONArray("[0,0,0]")).put("angularVelocity",new JSONArray("[0,0,0]"));}scene(s);return s;}
 static void scene(JSONObject scene){
  var original=SpatialScene.parse(scene.getString("sourceText"),scene.getString("id"));for(String k:original.keySet())if(!k.equals("objects")&&!new JSONObject().put("v",original.get(k)).similar(new JSONObject().put("v",scene.get(k))))throw new IllegalArgumentException("Authored small-body source changed");
  if(scene.getJSONArray("objects").length()!=original.getJSONArray("objects").length())throw new IllegalArgumentException("Missing small-domain object");int dynamic=0;var ids=new HashSet<String>();
  for(Object raw:scene.getJSONArray("objects")){var o=(JSONObject)raw;if(!ids.add(o.getString("id")))throw new IllegalArgumentException("Repeated object");var src=SpatialScene.object(original,o.getString("id"));for(String k:src.keySet())if(!Set.of("position","rotation").contains(k)&&!new JSONObject().put("v",src.get(k)).similar(new JSONObject().put("v",o.get(k))))throw new IllegalArgumentException("Small-body geometry/material changed");
   var p=SpatialScene.vector(o.getJSONArray("position"),2);var q=o.getJSONArray("rotation");WorldVerify.quaternion(q);if(WorldPlan.angle(q,new JSONArray("[0,0,0,1]"))>1)throw new IllegalArgumentException("Small domain requires upright axis-aligned boxes");
   for(String k:List.of("velocity","angularVelocity"))if(WorldVerify.norm(o.getJSONArray(k))>(k.equals("velocity")?.02:.05))throw new IllegalArgumentException("Start from a verified settled state");
   if(o.getDouble("massKg")>0){dynamic++;RampDefinition.range(o.getDouble("massKg"),.02,.2,"small-body mass");for(Object n:o.getJSONArray("size"))RampDefinition.range(((Number)n).doubleValue(),.05,.15,"small-body side");if(Math.abs(p[1]-o.getJSONArray("size").getDouble(1)/2)>.004)throw new IllegalArgumentException("Small box must start on the floor");}
   else if(!o.getJSONArray("position").similar(src.getJSONArray("position"))||!o.getJSONArray("rotation").similar(src.getJSONArray("rotation")))throw new IllegalArgumentException("Static geometry moved");
  }
  if(dynamic!=1)throw new IllegalArgumentException("Select exactly one small dynamic box");var floor=WorldPlan.sourceObject(scene,"floor");if(WorldPlan.angle(floor.getJSONArray("rotation"),new JSONArray("[0,0,0,1]"))>1e-6||floor.getDouble("massKg")!=0||Math.abs(floor.getJSONArray("position").getDouble(1)+floor.getJSONArray("size").getDouble(1)/2)>.00001)throw new IllegalArgumentException("Require a flat static floor at Y=0");
 }
 static JSONObject body(JSONObject scene){for(Object raw:scene.getJSONArray("objects"))if(((JSONObject)raw).getDouble("massKg")>0)return (JSONObject)raw;throw new IllegalArgumentException("No dynamic body");}
 static JSONObject plan(JSONObject scene,JSONObject input){
  scene(scene);SpatialScene.keys(input,"targetXZ","forceLimitN","torqueLimitNm","coordinateRadiusM");var b=body(scene);var xz=input.getJSONArray("targetXZ");if(xz.length()!=2)throw new IllegalArgumentException("Choose X/Z metres");var to=new double[]{xz.getDouble(0),b.getJSONArray("size").getDouble(1)/2,xz.getDouble(1)};for(double n:to)RampDefinition.range(n,-1,1,"target metres");
  var d=WorldFrames.copy(input);if(!d.has("forceLimitN"))d.put("forceLimitN",3);if(!d.has("torqueLimitNm"))d.put("torqueLimitNm",.02);if(!d.has("coordinateRadiusM"))d.put("coordinateRadiusM",.01);double force=SpatialScene.number(d,"forceLimitN",.05,5),torque=SpatialScene.number(d,"torqueLimitNm",.001,.05),uncertainty=SpatialScene.number(d,"coordinateRadiusM",0,.03);
  if(force<b.getDouble("massKg")*scene.getDouble("gravityMps2")*1.2)throw new IllegalArgumentException("Insufficient force reserve to lift this mass");
  double[] from=SpatialScene.vector(b.getJSONArray("position"),2);double move=Math.hypot(to[0]-from[0],to[2]-from[2]);if(move<.02||move>.4)throw new IllegalArgumentException("Single move must be 2..40 cm");
  var floor=WorldPlan.sourceObject(scene,"floor");double radius=ObserverGeometry.norm(SpatialScene.vector(b.getJSONArray("size"),1))/2+uncertainty+.005,top=b.getJSONArray("size").getDouble(1);for(Object raw:scene.getJSONArray("objects")){var o=(JSONObject)raw;if(!o.getString("sourceId").equals("floor"))top=Math.max(top,o.getJSONArray("position").getDouble(1)+o.getJSONArray("size").getDouble(1)/2);}
  double lift=top+radius+.04;if(lift>.6)throw new IllegalArgumentException("Small-domain lift exceeds 60 cm");
  for(double[] p:List.of(from,to))for(int axis:new int[]{0,2})if(Math.abs(p[axis]-floor.getJSONArray("position").getDouble(axis))+radius>floor.getJSONArray("size").getDouble(axis)/2)throw new IllegalArgumentException("Uncertainty-expanded route leaves the floor");
  var positions=new LinkedHashMap<String,double[]>();positions.put(b.getString("sourceId"),from);double[][] route={from,{from[0],lift,from[2]},{to[0],lift,to[2]},to};
  for(int i=0;i<3;i++)if(!WorldPlan.clear(scene,b.getString("sourceId"),positions,route[i],route[i+1],radius))throw new IllegalArgumentException("Uncertainty-expanded small-body corridor is blocked");
  var target=new JSONObject().put("objectId",b.getString("id")).put("sourceId",b.getString("sourceId")).put("positionM",new JSONArray(to)).put("rotation",new JSONArray("[0,0,0,1]"));
  return new JSONObject().put("input",d).put("sceneHash",WorldFrames.hash(scene)).put("intent",new JSONObject().put("forceLimitN",force).put("torqueLimitNm",torque)).put("orderedTargets",new JSONArray().put(target)).put("liftHeightM",lift).put("scope",SCOPE).put("collisionMarginM",margin(b)).put("coordinateRadiusM",uncertainty).put("passed",true);
 }
 static void verifyPlan(JSONObject scene,JSONObject p){if(!plan(scene,p.getJSONObject("input")).similar(p))throw new IllegalStateException("Frozen small-body plan changed");}
 static synchronized JSONObject execute(JSONObject scene,JSONObject p,Path nativeDir,Runnable checkpoint){
  try{var events=new JSONArray();var samples=new JSONArray();try(var physics=new SmallPhysics161(scene,p,nativeDir,true,null,events)){var initial=physics.sample();while(!physics.settled()){checkpoint.run();physics.step();if(physics.tick%12==0||physics.settled()){var sample=physics.sample().put("integration",physics.takeLedger());samples.put(sample);}}
   var run=new JSONObject().put("definition",scene).put("plan",p).put("controllerVersion",SmallPhysics161.VERSION).put("orientationControl",true).put("initialState",initial).put("events",events).put("samples",samples).put("final",samples.getJSONObject(samples.length()-1));run.put("status",physics.controller.getString("phase").equals("finished")?"completed":"failed").put("reason",physics.controller.optString("reason",""));run.put("verification",SmallVerify161.verify(run));return run;}
  }catch(Exception e){throw new IllegalStateException(e.getMessage(),e);}
 }
 static JSONObject after(JSONObject scene,JSONObject run){if(!run.getJSONObject("definition").similar(scene)||!SmallVerify161.verify(run).getBoolean("passed"))throw new IllegalStateException("Only verified actual motion advances the small world");var next=WorldFrames.copy(scene);var original=body(next);var actual=ContactPhysics.dynamic(run.getJSONObject("final").getJSONArray("bodies"),original.getString("id"));for(String k:List.of("position","rotation","velocity","angularVelocity"))original.put(k,actual.get(k));scene(next);return next;}
}
