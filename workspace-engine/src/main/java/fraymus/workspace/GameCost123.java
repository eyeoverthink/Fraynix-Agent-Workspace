package fraymus.workspace;

import org.json.*;
import java.util.*;

/** Full-workflow measurements; economic thresholds never override safety or certify a task. */
final class GameCost123 {
 static JSONObject summarize(JSONArray rows) {
  var groups=new TreeMap<String,Map<String,TreeMap<Integer,JSONObject>>>();var ids=new HashSet<String>();
  for(Object raw:rows){var row=(JSONObject)raw;var m=row.getJSONObject("measurement");String strategy=row.getString("strategy"),name=row.getString("case");
   if(!GameStrategy123.STRATEGIES.contains(strategy)||!strategy.equals(m.getString("strategy"))||!m.getBoolean("evidenceVerified")||!ids.add(m.getString("taskId"))||!GameStrategy123.policy(strategy).equals(m.getJSONObject("audit").getString("completionControl"))||!GameStrategy123.policy(strategy).equals(m.getJSONObject("final").getJSONObject("controller").getString("policy")))throw new IllegalArgumentException("Unbound or duplicate measured outcome");
   long wall=duration(row,"wallNanos"),sum=0;for(String key:List.of("setupNanos","advanceNanos","verifyNanos","closeNanos"))sum=Math.addExact(sum,duration(row,key));if(wall<sum||wall==0)throw new IllegalArgumentException("Incomplete full-cost timing");
   int repeat=row.getInt("repeat");if(repeat<0||repeat>99)throw new IllegalArgumentException("Bounded repetition required");
   var bucket=groups.computeIfAbsent(name,k->new TreeMap<>()).computeIfAbsent(strategy,k->new TreeMap<>());if(bucket.putIfAbsent(repeat,row)!=null)throw new IllegalArgumentException("Duplicate measured repeat");
  }
  var cases=new JSONArray();
  for(var group:groups.entrySet()){
   var policies=group.getValue();JSONObject parameters=null;var metrics=new JSONArray();
   for(var p:policies.entrySet()){
    var walls=new ArrayList<Double>();var simulation=new ArrayList<Double>();int wins=0,deaths=0,damage=0,pickups=0,waits=0,returns=0;
    for(var row:p.getValue().values()){
     var m=row.getJSONObject("measurement");var a=m.getJSONObject("audit");var f=m.getJSONObject("final");var player=f.getJSONObject("player");var c=f.getJSONObject("controller");
     if(parameters==null)parameters=m.getJSONObject("parameters");else if(!parameters.similar(m.getJSONObject("parameters")))throw new IllegalArgumentException("Different geometry cannot be paired");
     boolean won=player.getBoolean("won"),dead=player.getBoolean("dead");if(m.getBoolean("completionCertified")!=won||won&&dead||!a.getString("existingAI").equals(won?"won":"failed"))throw new IllegalArgumentException("Outcome labels disagree");
     if(c.getInt("damage")<0||c.getInt("pickups")<0||a.getInt("aiDamage")!=c.getInt("damage")||a.getInt("aiPickups")!=c.getInt("pickups"))throw new IllegalArgumentException("Outcome counters disagree");
     wins+=won?1:0;deaths+=dead?1:0;damage+=c.getInt("damage");pickups+=c.getInt("pickups");waits+=c.getJSONObject("opportunity120").getInt("waitStarts");returns+=c.getJSONObject("opportunity120").getInt("revisitStarts");walls.add(row.getLong("wallNanos")/1e9);simulation.add(f.getInt("tick")/60.0);
    }
    metrics.put(new JSONObject().put("strategy",p.getKey()).put("repetitions",walls.size()).put("wins",wins).put("deaths",deaths).put("damage",damage).put("pickups",pickups).put("waitStarts",waits).put("returnStarts",returns).put("wallSeconds",stats(walls)).put("simulationSeconds",stats(simulation)));
   }
   var comparisons=new JSONArray();for(String[] pair:List.of(new String[]{"full","no-wait"},new String[]{"full","no-return"},new String[]{"no-return","direct"},new String[]{"no-wait","direct"},new String[]{"full","direct"})){
    if(policies.containsKey(pair[0])&&policies.containsKey(pair[1]))comparisons.put(compare(pair[0],pair[1],policies.get(pair[0]),policies.get(pair[1])));
   }
   cases.put(new JSONObject().put("case",group.getKey()).put("parameters",parameters).put("strategies",metrics).put("comparisons",comparisons));
  }
  return new JSONObject().put("schema",123).put("cases",cases).put("scope","Measured complete Java task calls, source/outcome verification, persistence and shutdown. UI scheduling and study report writing are separate. Observed ranges from repeated timings are not statistical confidence bounds. No automatic policy promotion or assigned user value.");
 }
 private static long duration(JSONObject row,String key){double value=row.getDouble(key);long n=row.getLong(key);if(!Double.isFinite(value)||value!=n||n<0||n>86_400_000_000_000L)throw new IllegalArgumentException("Invalid duration");return n;}
 private static JSONObject stats(List<Double> x){var a=new ArrayList<>(x);Collections.sort(a);int n=a.size();return new JSONObject().put("min",a.get(0)).put("median",n%2==0?(a.get(n/2-1)+a.get(n/2))/2:a.get(n/2)).put("max",a.get(n-1)).put("mean",a.stream().mapToDouble(Double::doubleValue).average().orElseThrow());}
 private static JSONObject compare(String enabled,String disabled,TreeMap<Integer,JSONObject> a,TreeMap<Integer,JSONObject> b){
  if(!a.keySet().equals(b.keySet()))throw new IllegalArgumentException("Pair every declared repetition");var delta=new ArrayList<Double>();var gain=new ArrayList<Double>();boolean safe=true;
  for(int i:a.keySet()){
   var ma=a.get(i).getJSONObject("measurement");var mb=b.get(i).getJSONObject("measurement");var fa=ma.getJSONObject("final");var fb=mb.getJSONObject("final");
   safe&=ma.getBoolean("completionCertified")&&mb.getBoolean("completionCertified")&&!fa.getJSONObject("player").getBoolean("dead")&&!fb.getJSONObject("player").getBoolean("dead")&&fa.getJSONObject("controller").getInt("damage")==0&&fb.getJSONObject("controller").getInt("damage")==0;
   delta.add((a.get(i).getLong("wallNanos")-b.get(i).getLong("wallNanos"))/1e9);gain.add((double)(ma.getJSONObject("audit").getInt("aiPickups")-mb.getJSONObject("audit").getInt("aiPickups")));
  }
  var ds=stats(delta);var gs=stats(gain);var out=new JSONObject().put("enabled",enabled).put("comparison",disabled).put("pairs",a.size()).put("elapsedDeltaSeconds",ds).put("extraPickups",gs).put("safePairedFinishes",safe);
  String verdict;
  if(!safe)verdict="Safety or completion differs: no economic recommendation";
  else if(a.size()<3)verdict="Insufficient repeated timings";
  else if(gs.getDouble("min")>0){out.put("observedConservativeSecondsPerExtraPickup",Math.max(0,ds.getDouble("max"))/gs.getDouble("min"));verdict="Extra pickup in every repeat: compare its value with the measured cost threshold";}
  else if(gs.getDouble("min")>=0&&ds.getDouble("max")<0)verdict="No worse pickup count and faster in every measured pair";
  else if(gs.getDouble("max")<=0)verdict="No additional pickup benefit demonstrated";
  else verdict="Mixed pickup benefit: no stable break-even value";
  return out.put("assessment",verdict);
 }
}
