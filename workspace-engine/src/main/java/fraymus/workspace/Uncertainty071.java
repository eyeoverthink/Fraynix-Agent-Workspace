package fraymus.workspace;
import org.json.*;
import java.util.*;

/** Owned residual fitting and separately calibrated joint envelope. Never reads a held-out future. */
final class Uncertainty071 {
 static final List<Integer> TICKS=List.of(92,116,140,180);
 static final List<String> CELLS=List.of("accelerating-predicted","accelerating-measured","slowing-predicted","slowing-measured");
 static String cell(JSONObject task,JSONObject decision){return (task.getDouble("frictionRatio")<1?"accelerating":"slowing")+(decision.getBoolean("recover")?"-measured":"-predicted");}
 static double median(List<Double> values){if(values.isEmpty())throw new IllegalArgumentException("Empty residual sample");var sorted=new ArrayList<>(values);sorted.sort(Double::compare);int n=sorted.size();return n%2==1?sorted.get(n/2):(sorted.get(n/2-1)+sorted.get(n/2))/2;}
 static JSONObject label(JSONObject record){
  UncertainTrial071.verify(record);if(!record.getJSONObject("decision").getString("method").equals("blind")||!record.getJSONObject("disturbance").getString("family").equals("none"))throw new IllegalStateException("Calibration requires a clean blind world");
  var task=record.getJSONObject("task");var decision=record.getJSONObject("decision");var anchor=ObservationSchedule070.anchor(task,decision,record.optJSONObject("correctionPlan"));var values=new JSONArray();
  for(int tick:TICKS){double predicted=ObservationSchedule070.advance(task,anchor.getDouble("distanceM"),anchor.getDouble("tangentMps"),(tick-60)/240.0)[0];double actual=Stopping063.projected(record.getJSONObject("definition"),record.getJSONArray("samples").getJSONObject(tick/4)).getDouble("distanceM");values.put(actual-predicted);}
  return new JSONObject().put("cell",cell(task,decision)).put("recordId",record.getString("id")).put("recordSha256",record.getString("sha256")).put("residualsM",values);
 }
 static JSONObject fit(JSONArray fitting,JSONArray calibration){
  if(fitting.length()!=24||calibration.length()!=36)throw new IllegalStateException("Training partitions incomplete");
  var fitLabels=new JSONArray();var calLabels=new JSONArray();var ids=new HashSet<String>();
  for(Object raw:fitting){var l=label(((JSONObject)raw).getJSONObject("record"));if(!ids.add(l.getString("recordId")))throw new IllegalStateException("Duplicate fit identity");fitLabels.put(l);}
  for(Object raw:calibration){var l=label(((JSONObject)raw).getJSONObject("record"));if(!ids.add(l.getString("recordId")))throw new IllegalStateException("Calibration reused fitting world");calLabels.put(l);}
  return fitLabels(fitLabels,calLabels);
 }
 static JSONObject fitLabels(JSONArray fit,JSONArray calibration){
  if(fit.length()!=24||calibration.length()!=36)throw new IllegalStateException("Invalid residual partition sizes");var cells=new JSONObject();
  for(String cell:CELLS){var train=new ArrayList<JSONObject>();var cal=new ArrayList<JSONObject>();for(Object raw:fit)if(((JSONObject)raw).getString("cell").equals(cell))train.add((JSONObject)raw);for(Object raw:calibration)if(((JSONObject)raw).getString("cell").equals(cell))cal.add((JSONObject)raw);
   if(train.size()!=6||cal.size()!=9)throw new IllegalStateException("Expected six fit and nine separate calibration rows per cell");
   var centers=new JSONArray();var scales=new JSONArray();for(int k=0;k<4;k++){var values=new ArrayList<Double>();for(var row:train)values.add(row.getJSONArray("residualsM").getDouble(k));double center=median(values);var errors=new ArrayList<Double>();for(double v:values)errors.add(Math.abs(v-center));centers.put(center);scales.put(Math.max(.002,median(errors)));}
   var scores=new JSONArray();var ordered=new ArrayList<Double>();for(var row:cal){double score=0;for(int k=0;k<4;k++)score=Math.max(score,Math.abs(row.getJSONArray("residualsM").getDouble(k)-centers.getDouble(k))/scales.getDouble(k));scores.put(new JSONObject().put("recordId",row.getString("recordId")).put("score",score));ordered.add(score);}ordered.sort(Double::compare);int rank=Math.min(ordered.size(),(int)Math.ceil((ordered.size()+1)*.90));
   cells.put(cell,new JSONObject().put("fitCount",6).put("calibrationCount",9).put("centerM",centers).put("scaleM",scales).put("scores",scores).put("rank",rank).put("multiplier",ordered.get(rank-1)));
  }
  var model=new JSONObject().put("protocol","joint-residual-071-v1").put("ticks",new JSONArray(TICKS)).put("nominalCoverage",.90).put("scaleFloorM",.002).put("fitLabels",fit).put("calibrationLabels",calibration).put("cells",cells).put("scope","Separate native fitting/calibration; nominal joint residual envelope, not guaranteed coverage under changed conditions or physical measurement uncertainty");model.put("sha256",WorldFrames.hash(model));return model;
 }
 static void verify(JSONObject model){
  if(model==null)throw new IllegalStateException("Missing frozen uncertainty model");var expected=fitLabels(model.getJSONArray("fitLabels"),model.getJSONArray("calibrationLabels"));if(!expected.similar(model))throw new IllegalStateException("Frozen uncertainty model or calibration scores changed");
 }
 static JSONObject forecast(JSONObject task,JSONObject decision,JSONObject first,JSONObject model){
  verify(model);var anchor=ObservationSchedule070.anchor(task,decision,first);String key=cell(task,decision);var fitted=model.getJSONObject("cells").getJSONObject(key);var bands=new JSONArray();
  for(int i=0;i<TICKS.size();i++){int tick=TICKS.get(i);double nominal=ObservationSchedule070.advance(task,anchor.getDouble("distanceM"),anchor.getDouble("tangentMps"),(tick-60)/240.0)[0];double center=nominal+fitted.getJSONArray("centerM").getDouble(i),radius=fitted.getDouble("multiplier")*fitted.getJSONArray("scaleM").getDouble(i);bands.put(new JSONObject().put("tick",tick).put("nominalM",nominal).put("centerM",center).put("lowerM",center-radius).put("upperM",center+radius));}
  return new JSONObject().put("cell",key).put("modelSha256",model.getString("sha256")).put("anchor",anchor).put("bands",bands).put("scope","No-extra-observation trajectory. Compare only with a blind paired world, not a corrected endpoint.");
 }
 static JSONObject schedule(JSONObject task,JSONObject decision,JSONObject first,JSONObject model){
  var old=ObservationSchedule070.plan(task,decision,first);var prediction=forecast(task,decision,first,model);var terminal=prediction.getJSONArray("bands").getJSONObject(3);var hypothetical=new JSONArray();double lower=Double.POSITIVE_INFINITY,upper=Double.NEGATIVE_INFINITY;
  for(Object raw:old.getJSONArray("candidates").getJSONObject(0).getJSONArray("scenarios")){var scenario=(JSONObject)raw;double endpoint=scenario.getDouble("noCheckEndpointM"),lo=endpoint+terminal.getDouble("lowerM")-terminal.getDouble("nominalM"),hi=endpoint+terminal.getDouble("upperM")-terminal.getDouble("nominalM");lower=Math.min(lower,lo);upper=Math.max(upper,hi);hypothetical.put(new JSONObject().put("scenario",scenario.getJSONObject("scenario").getString("name")).put("lowerM",lo).put("upperM",hi));}
  double goalLow=task.getDouble("centerM")-task.getDouble("halfWidthM"),goalHigh=task.getDouble("centerM")+task.getDouble("halfWidthM");boolean safe=lower>=goalLow-1e-12&&upper<=goalHigh+1e-12;int original=old.getInt("selectedTick");boolean forced=original<0&&!safe;
  prediction.put("scenarioIntervals",hypothetical).put("unionLowerM",lower).put("unionUpperM",upper).put("goalLowerM",goalLow).put("goalUpperM",goalHigh).put("contained",safe).put("oldSelectedTick",original).put("forcedLook",forced).put("inferenceFee",.005);
  old.put("uncertainty071",prediction);if(forced)old.put("selectedTick",92).put("reason","Observe early: calibrated residual envelope makes the old skip risky");else old.put("reason",original<0?"Keep skip: whole residual/scenario envelope fits the goal":"Keep original selected check; uncertainty does not postpone it");return old;
 }
 static JSONObject coverage(JSONObject blind,JSONObject model){
  var task=blind.getJSONObject("task");var d=blind.getJSONObject("decision");if(!d.getString("method").equals("blind")||blind.has("laterPlan"))throw new IllegalStateException("Coverage truth must be an uncorrected later world");
  var f=forecast(task,d,blind.optJSONObject("correctionPlan"),model);boolean joint=true;var points=f.getJSONArray("bands");
  for(Object raw:points){var b=(JSONObject)raw;double actual=Stopping063.projected(blind.getJSONObject("definition"),blind.getJSONArray("samples").getJSONObject(b.getInt("tick")/4)).getDouble("distanceM");boolean inside=actual>=b.getDouble("lowerM")-1e-12&&actual<=b.getDouble("upperM")+1e-12;joint&=inside;b.put("actualM",actual).put("covered",inside);}
  var old=ObservationSchedule070.plan(task,d,blind.optJSONObject("correctionPlan"));double lo=Double.POSITIVE_INFINITY,hi=Double.NEGATIVE_INFINITY;for(Object raw:old.getJSONArray("candidates").getJSONObject(0).getJSONArray("scenarios")){double v=((JSONObject)raw).getDouble("noCheckEndpointM");lo=Math.min(lo,v);hi=Math.max(hi,v);}var last=points.getJSONObject(3);double actual=last.getDouble("actualM");
  return f.put("truthRecordId",blind.getString("id")).put("jointCovered",joint).put("terminalCovered",last.getBoolean("covered")).put("terminalWidthM",last.getDouble("upperM")-last.getDouble("lowerM")).put("oldRangeLowerM",lo).put("oldRangeUpperM",hi).put("oldRangeCovered",actual>=lo-1e-12&&actual<=hi+1e-12);
 }
}
