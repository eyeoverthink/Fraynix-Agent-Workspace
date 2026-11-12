package fraymus.workspace;
import org.json.*;import java.util.*;import java.util.regex.*;

/** Empirical conservative envelope, not a confidence interval or a safety controller. */
final class GameBudgetPolicy124 {
 static final double RECORD_RESERVE_SECONDS=.25;
 static final Pattern STRATEGY=Pattern.compile("(?i)\\bpickupStrategy\\s*(?:=|:)\\s*([^\\s,;]+)");
 static JSONObject request(String text){
  var strategy=STRATEGY.matcher(text);String value=strategy.find()?strategy.group(1).replaceFirst("[.]$","").toLowerCase(Locale.ROOT):"full";
  boolean budget=value.equals("budget");long mentions=Pattern.compile("(?i)\\bpickupBudgetSeconds\\b").matcher(text).results().count();
  if(!budget){if(mentions>0)throw new IllegalArgumentException("pickupBudgetSeconds requires pickupStrategy=budget");GameStrategy123.parse(text);return new JSONObject().put("budgeted",false);}
  if(Pattern.compile("(?i)\\bpickupStrategy\\b").matcher(text).results().count()!=1||mentions!=1)throw new IllegalArgumentException("Choose one strategy and one explicit pickupBudgetSeconds");
  var m=Pattern.compile("(?i)\\bpickupBudgetSeconds\\s*(?:=|:)\\s*([^\\s,;]+)").matcher(text);if(!m.find())throw new IllegalArgumentException("An explicit pickupBudgetSeconds in0..120 is required");
  double seconds;try{seconds=Double.parseDouble(m.group(1).replaceFirst("(?<=\\d)[.]$",""));}catch(Exception e){throw new IllegalArgumentException("pickupBudgetSeconds must be a finite number");}
  if(!Double.isFinite(seconds)||seconds<0||seconds>120)throw new IllegalArgumentException("pickupBudgetSeconds must be in0..120");
  var definition=GameTask115.request(resolve(text,"direct"));if(!GameChallenge118.active(definition.getJSONObject("parameters")))throw new IllegalArgumentException("Budgeted pickups require declared challenge geometry");
  return new JSONObject().put("budgeted",true).put("allowance",seconds).put("parameters",definition.getJSONObject("parameters"));
 }
 static String resolve(String request,String chosen){if(!GameStrategy123.STRATEGIES.contains(chosen))throw new IllegalArgumentException("Unknown concrete strategy");return STRATEGY.matcher(request).replaceFirst("pickupStrategy="+chosen);}
 static JSONObject choose(JSONArray samples,double allowance,double overhead){
  if(!Double.isFinite(allowance)||allowance<0||allowance>120||!Double.isFinite(overhead)||overhead<0||overhead>600)throw new IllegalArgumentException("Invalid bounded choice inputs");
  var groups=new HashMap<String,List<JSONObject>>();var ids=new HashSet<String>();
  for(Object raw:samples){var s=(JSONObject)raw;String strategy=s.getString("strategy");double seconds=s.getDouble("seconds");int pickups=s.getInt("pickups");if(!GameStrategy123.STRATEGIES.contains(strategy)||!ids.add(s.getString("id"))||!Double.isFinite(seconds)||seconds<=0||seconds>600||pickups<0||pickups>100||pickups!=s.getDouble("pickups"))throw new IllegalArgumentException("Invalid or duplicate measured sample");s.getBoolean("safe");s.getString("renderer");groups.computeIfAbsent(strategy,k->new ArrayList<>()).add(s);}
  var options=new JSONArray();String chosen="direct",reason=allowance==0?"Zero extra allowance":"Insufficient matched evidence";double best=Double.POSITIVE_INFINITY;
  for(String strategy:List.of("no-return","no-wait","full")){
   var base=groups.getOrDefault("direct",List.of());var alt=groups.getOrDefault(strategy,List.of());var result=new JSONObject().put("strategy",strategy).put("baselineSamples",base.size()).put("samples",alt.size());String why="";
   if(base.size()<3||alt.size()<3)why="At least three direct and three alternative measurements are required";
   else{
    var all=new ArrayList<>(base);all.addAll(alt);var renderers=new HashSet<String>();for(var s:all)renderers.add(s.getString("renderer"));
    if(all.stream().anyMatch(s->!s.getBoolean("safe")))why="Unsafe or failed outcome retained: no optional recommendation";
    else if(renderers.size()!=1)why="Renderer changed: measurements cannot be combined";
    else{
     var b=stats(base);var a=stats(alt);double lower=a.getDouble("minPickups")-b.getDouble("maxPickups"),upper=a.getDouble("maxPickups")-b.getDouble("minPickups");double spread=Math.max(a.getDouble("maxSeconds")-a.getDouble("minSeconds"),b.getDouble("maxSeconds")-b.getDouble("minSeconds"));double extra=Math.max(0,a.getDouble("maxSeconds")-b.getDouble("minSeconds"));double total=extra+2*spread+overhead+RECORD_RESERVE_SECONDS;
     result.put("baseline",b).put("alternative",a).put("benefitLower",lower).put("benefitUpper",upper).put("extraSecondsUpper",extra).put("variationReserveSeconds",2*spread).put("decisionOverheadSeconds",overhead).put("recordingReserveSeconds",RECORD_RESERVE_SECONDS).put("estimatedExtraSeconds",total);
     if(lower<=0)why="No consistent additional pickup benefit";
     else if(a.getDouble("maxSeconds")/a.getDouble("minSeconds")>1.5||b.getDouble("maxSeconds")/b.getDouble("minSeconds")>1.5)why="Timing variation is too large";
     else {double threshold=total/lower;result.put("requiredSecondsPerPickup",threshold);if(allowance<=0||threshold>allowance)why="Conservative estimate exceeds the allowance";else{why="Benefit remains positive across observations and estimated cost fits";if(threshold<best){best=threshold;chosen=strategy;reason=why;}}}
    }
   }result.put("reason",why);options.put(result);
  }
  if(chosen.equals("direct")&&allowance>0)reason="Abstained from optional actions; see the retained evidence assessments";
  return new JSONObject().put("selected",chosen).put("allowance",allowance).put("reason",reason).put("alternatives",options).put("scope","Exact-condition empirical envelope; not a probability, hard deadline or safety guarantee. Ordinary safety checks remain authoritative.");
 }
 private static JSONObject stats(List<JSONObject> a){return new JSONObject().put("minSeconds",a.stream().mapToDouble(s->s.getDouble("seconds")).min().orElseThrow()).put("maxSeconds",a.stream().mapToDouble(s->s.getDouble("seconds")).max().orElseThrow()).put("minPickups",a.stream().mapToInt(s->s.getInt("pickups")).min().orElseThrow()).put("maxPickups",a.stream().mapToInt(s->s.getInt("pickups")).max().orElseThrow());}
 private GameBudgetPolicy124(){}
}
