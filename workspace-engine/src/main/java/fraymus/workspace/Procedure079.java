package fraymus.workspace;
import org.json.*;
import java.util.*;
final class Procedure079 {
 static final String KIND="selective-world-procedure-079-v1";
 static JSONObject spec(JSONObject frame,JSONObject args){
  WorldFrames.verify(frame);var source=new JSONObject(frame.getJSONObject("originalScene").getString("sourceText"));var current=WorldFrames.scene(frame);
  for(Object x:source.getJSONArray("objects")){var o=(JSONObject)x;var b=WorldPlan.sourceObject(current,o.getString("sourceId"));double scale=switch(source.getString("units")){case "cm"->.01;case "mm"->.001;default->1;};var position=new JSONArray();for(int k=0;k<3;k++)position.put(b.getJSONArray("position").getDouble(k)/scale);o.put("position",position).put("rotation",b.getJSONArray("rotation"));}
  return new JSONObject().put("source",source).put("targets",args.getJSONArray("targets")).put("forceLimitN",args.optDouble("forceLimitN",60)).put("torqueLimitNm",args.optDouble("torqueLimitNm",4));
 }
 static JSONObject choose(JSONObject spec,String mode,JSONArray candidates){
  if(!Set.of("simple","memory","adaptive").contains(mode))throw new IllegalArgumentException("Choose simple, memory or adaptive");
  var result=new JSONObject().put("mode",mode).put("useMemory",false).put("preferred","direct").put("reason","Simple control requested").put("candidates",candidates);
  if(mode.equals("simple"))return result;
  JSONObject nearest=null;double best=Double.POSITIVE_INFINITY;
  for(Object raw:candidates){var c=(JSONObject)raw;double distance=c.getDouble("distance");if(distance<best){best=distance;nearest=c;}}
  if(nearest==null)return result.put("reason","No compatible verified experience; simple control");
  result.put("candidate",nearest);boolean use=mode.equals("memory")||nearest.getDouble("distance")<=.10&&!nearest.getString("family").equals("direct")&&nearest.getDouble("lowerSavingMillis")>0;
  return result.put("useMemory",use).put("preferred",use?nearest.getString("family"):"direct").put("reason",use?(mode.equals("memory")?"Explicit remembered-procedure control":"Measured avoided work exceeds proof cost and uncertainty margin"):"Estimated benefit does not justify proof cost or unfamiliarity; simple control");
 }
 static JSONObject estimate(JSONObject receipt,JSONObject spec,boolean warm){
  double distance=Procedure078.distance(receipt.getJSONObject("spec"),spec),proof=warm?Math.max(250,receipt.optDouble("warmMillis",250)):Math.max(1000,receipt.getDouble("auditMillis")),margin=500+2*distance*receipt.getDouble("activeMillis");
  return new JSONObject().put("id",receipt.getString("id")).put("family",receipt.getString("family")).put("distance",distance).put("warm",warm).put("avoidedMillis",receipt.getDouble("avoidedMillis")).put("proofEstimateMillis",proof).put("uncertaintyMarginMillis",margin).put("lowerSavingMillis",receipt.getDouble("avoidedMillis")-proof-margin);
 }
 static java.util.List<String> order(JSONObject decision){var a=new ArrayList<>(Procedure078.FAMILIES);String preferred=decision.getString("preferred");if(!a.contains(preferred))throw new IllegalArgumentException("Unknown procedure");if(decision.getBoolean("useMemory")){a.remove(preferred);a.add(0,preferred);}return a;}
    static JSONObject task(JSONObject initial,JSONObject spec,JSONObject frame){
        var source=WorldFrames.scene(initial);WorldFrames.verify(frame);if(!frame.getJSONObject("originalScene").similar(WorldFrames.frame(source).getJSONObject("originalScene")))throw new IllegalStateException("Task original source changed");
        double position=0,angle=0,speed=0,angular=0,protectedPosition=0,protectedAngle=0;var targets=new HashSet<String>();
        for(Object raw:spec.getJSONArray("targets")){var t=(JSONObject)raw;targets.add(t.getString("sourceId"));var o=WorldPlan.sourceObject(source,t.getString("sourceId"));var b=ContactPhysics.dynamic(frame.getJSONArray("bodies"),o.getString("id"));position=Math.max(position,WorldVerify.distance(t.getJSONArray("positionM"),b.getJSONArray("position")));angle=Math.max(angle,WorldPlan.angle(WorldPlan.quat(t.getDouble("yawDeg")),b.getJSONArray("rotation")));speed=Math.max(speed,WorldVerify.norm(b.getJSONArray("velocity")));angular=Math.max(angular,WorldVerify.norm(b.getJSONArray("angularVelocity")));}
        for(Object raw:source.getJSONArray("objects")){var o=(JSONObject)raw;if(o.getDouble("massKg")<=0||targets.contains(o.getString("sourceId")))continue;var b=ContactPhysics.dynamic(frame.getJSONArray("bodies"),o.getString("id"));protectedPosition=Math.max(protectedPosition,WorldVerify.distance(o.getJSONArray("position"),b.getJSONArray("position")));protectedAngle=Math.max(protectedAngle,WorldPlan.angle(o.getJSONArray("rotation"),b.getJSONArray("rotation")));}
        return new JSONObject().put("passed",position<=.1&&angle<=5&&speed<=.2&&angular<=.2&&protectedPosition<=.05&&protectedAngle<=5).put("positionM",position).put("angleDeg",angle).put("speedMps",speed).put("angularSpeedRadps",angular).put("protectedPositionM",protectedPosition).put("protectedAngleDeg",protectedAngle);
    }
    static JSONObject comparison(JSONObject forecast,JSONObject actual){
        if(forecast.getString("id").equals(actual.getString("id"))||!forecast.getJSONObject("definition").similar(actual.getJSONObject("definition"))||!forecast.getJSONObject("plan").similar(actual.getJSONObject("plan")))throw new IllegalStateException("Fresh stage source/plan identity differs");
        for(var run:List.of(forecast,actual))if(!run.getString("status").equals("completed")||!run.getBoolean("orientationControl")||run.optInt("resumes")>2||run.getJSONObject("plan").getJSONObject("intent").has("construction051")||!WorldVerify.verify(run).getBoolean("passed"))throw new IllegalStateException("A fresh verified native arrangement stage is required");
        var ticks=new HashMap<Integer,JSONObject>();for(Object raw:forecast.getJSONArray("samples")){var f=(JSONObject)raw;if(ticks.put(f.getInt("tick"),f)!=null)throw new IllegalStateException("Duplicate forecast tick");}
        int matched=0;double position=0,angle=0;var seen=new HashSet<Integer>();
        for(Object raw:actual.getJSONArray("samples")){var f=(JSONObject)raw;if(!seen.add(f.getInt("tick")))throw new IllegalStateException("Duplicate actual tick");var prior=ticks.get(f.getInt("tick"));if(prior==null)continue;matched++;if(prior.getJSONArray("bodies").length()!=f.getJSONArray("bodies").length())throw new IllegalStateException("Stage body set changed");for(Object item:f.getJSONArray("bodies")){var body=(JSONObject)item;var before=ContactPhysics.dynamic(prior.getJSONArray("bodies"),body.getString("id"));position=Math.max(position,WorldVerify.distance(before.getJSONArray("position"),body.getJSONArray("position")));angle=Math.max(angle,WorldPlan.angle(before.getJSONArray("rotation"),body.getJSONArray("rotation")));}}
        double coverage=matched/(double)Math.max(1,Math.max(ticks.size(),seen.size()));return new JSONObject().put("passed",coverage>=.95&&position<=.01&&angle<=.5).put("sampleCoverage",coverage).put("matchedSamples",matched).put("maxPositionResidualM",position).put("maxAngleResidualDeg",angle).put("forecastRunId",forecast.getString("id")).put("actualRunId",actual.getString("id")).put("forecastSha256",WorldFrames.hash(forecast)).put("actualSha256",WorldFrames.hash(actual));
    }
    static boolean protectedTrace(JSONObject start,JSONObject spec,JSONObject run){
        var initial=WorldFrames.scene(start);var selected=new HashSet<String>();for(Object raw:spec.getJSONArray("targets"))selected.add(((JSONObject)raw).getString("sourceId"));
        for(Object raw:initial.getJSONArray("objects")){var o=(JSONObject)raw;if(o.getDouble("massKg")<=0||selected.contains(o.getString("sourceId")))continue;String id=o.getString("id");var initialBody=ContactPhysics.dynamic(run.getJSONObject("initialState").getJSONArray("bodies"),id);var position=new JSONArray(initialBody.getJSONArray("position").toString());if(WorldVerify.distance(o.getJSONArray("position"),position)>.05||WorldPlan.angle(o.getJSONArray("rotation"),initialBody.getJSONArray("rotation"))>5)return false;
            for(Object f:run.getJSONArray("samples"))for(Object integration:((JSONObject)f).getJSONArray("integration"))for(Object delta:((JSONArray)integration).getJSONArray(6)){var d=(JSONArray)delta;if(!d.getString(0).equals(id))continue;for(int k=0;k<3;k++)position.put(k,position.getDouble(k)+d.getJSONArray(1).getDouble(k));if(WorldVerify.distance(o.getJSONArray("position"),position)>.05||WorldPlan.angle(o.getJSONArray("rotation"),d.getJSONArray(2))>5)return false;}}
        return true;
    }
}
