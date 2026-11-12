package fraymus.workspace;

import org.json.*;
import java.util.*;

/** Causal secants with a position-error envelope and bounded acceleration.
 * For baseline d and look-ahead h, acceleration contributes A*h*(d+h)/2.
 * Choosing a narrower valid envelope never removes either endpoint's error. */
final class ObservationForecast129 {
 static final String POLICY="bounded-baseline-129-v1";
 static boolean selected(JSONObject d){return POLICY.equals(d.optString("forecastPolicy129"));}
 static double numeric(JSONObject o,String k){if(!(o.get(k) instanceof Number))throw new IllegalArgumentException("Numeric "+k+" required");double v=o.getDouble(k);if(!Double.isFinite(v))throw new IllegalArgumentException("Finite "+k+" required");return v;}
 static void validate(JSONObject d){if(!selected(d))throw new IllegalArgumentException("Unknown forecast policy");for(String k:List.of("baselineWindowS","maxForecastRadiusM")){double v=d.has(k)?numeric(d,k):(k.equals("baselineWindowS")?.6:1);PlaneCalibration.bounded(v,k.equals("baselineWindowS")?.01:.001,k.equals("baselineWindowS")?2:10000);d.put(k,v);}}
 static JSONObject forecast(JSONObject source,JSONObject d){
  var past=new ArrayList<JSONObject>();var out=new JSONArray();double time=-1,sum=0,max=0;int measured=0,compared=0,covered=0,available=0;
  for(Object raw:source.getJSONArray("rows")){
   var row=WorldFrames.copy((JSONObject)raw);double t=numeric(row,"seconds");if(t<=time)throw new IllegalArgumentException("Observation times must strictly increase");time=t;
   var prediction=predict(past,t,d);row.put("forecast",prediction);if(prediction.getBoolean("available"))available++;
   if(row.has("positionM")){PlaneCalibration.point(row.getJSONArray("positionM"));PlaneCalibration.bounded(numeric(row,"radiusM"),0,10000);}
   if(row.getBoolean("accepted")&&row.has("positionM")&&row.getString("status").equals("measured-estimate")){
    measured++;if(prediction.getBoolean("available")){double error=PlaneCalibration.distance(PlaneCalibration.point(row.getJSONArray("positionM")),PlaneCalibration.point(prediction.getJSONArray("positionM")));boolean within=error<=prediction.getDouble("radiusM")+row.getDouble("radiusM");row.put("errorM",error).put("withinDeclaredBound",within);sum+=error;max=Math.max(max,error);compared++;if(within)covered++;}
    if(!past.isEmpty()&&t-past.get(past.size()-1).getDouble("seconds")>d.getDouble("maxGapS"))past.clear();past.add(row);while(past.size()>96||!past.isEmpty()&&t-past.get(0).getDouble("seconds")>d.getDouble("baselineWindowS")+1e-9)past.remove(0);
   }out.put(row);
  }
  if(measured<3||compared<1)throw new IllegalArgumentException("At least three accepted positions and one forecast within the declared radius are required; improve observations or explicitly choose a different precision budget");
  return new JSONObject().put("rows",out).put("future",predict(past,time+d.getDouble("horizonS"),d)).put("metrics",new JSONObject().put("rows",out.length()).put("measured",measured).put("rejected",out.length()-measured).put("availableForecasts",available).put("withheldForecasts",out.length()-available).put("compared",compared).put("covered",covered).put("meanErrorM",sum/compared).put("maxErrorM",max)).put("units","metres and source seconds").put("method","Causal minimum-envelope secant within declared baseline window; only earlier accepted points").put("uncertainty","Engineering position-error envelope with acceleration during both baseline and forecast; requested maximum radius "+d.getDouble("maxForecastRadiusM")+" m. Correlated tracking bias is not detected by smooth motion; this is not calibrated probability or a physical guarantee");
 }
 static JSONObject predict(List<JSONObject> past,double t,JSONObject d){
  var withheld=new JSONObject().put("available",false);if(past.size()<2)return withheld.put("reason","Need two earlier accepted measurements");
  var last=past.get(past.size()-1);double ahead=t-last.getDouble("seconds");if(ahead<=0||ahead>d.getDouble("horizonS")+1e-9||ahead>d.getDouble("maxGapS")+1e-9)return withheld.put("reason","Accepted-measurement gap or forecast horizon exceeded");
  double best=Double.POSITIVE_INFINITY;JSONObject chosen=null;double[] b=PlaneCalibration.point(last.getJSONArray("positionM"));
  for(int i=past.size()-2;i>=0;i--){var earlier=past.get(i);double dt=last.getDouble("seconds")-earlier.getDouble("seconds");if(dt<=0||dt>d.getDouble("baselineWindowS")+1e-9)continue;double radius=last.getDouble("radiusM")+ahead*(last.getDouble("radiusM")+earlier.getDouble("radiusM"))/dt+.5*d.getDouble("accelerationBoundMps2")*ahead*(ahead+dt);
   if(!Double.isFinite(radius)||radius>=best)continue;var a=PlaneCalibration.point(earlier.getJSONArray("positionM"));double x=b[0]+(b[0]-a[0])*ahead/dt,y=b[1]+(b[1]-a[1])*ahead/dt;if(!Double.isFinite(x)||!Double.isFinite(y))continue;best=radius;chosen=new JSONObject().put("available",true).put("seconds",t).put("positionM",new JSONArray(new double[]{x,y})).put("radiusM",radius).put("baselineS",dt).put("aheadS",ahead).put("fromOrdinals",new JSONArray().put(earlier.getInt("ordinal")).put(last.getInt("ordinal")));
  }
  if(chosen==null)return withheld.put("reason","No earlier accepted baseline within the declared window");
  if(best>d.getDouble("maxForecastRadiusM"))return withheld.put("reason",String.format(Locale.ROOT,"Best declared radius %.6f m exceeds requested %.6f m; no precise forecast claimed",best,d.getDouble("maxForecastRadiusM"))).put("bestRadiusM",best).put("requestedRadiusM",d.getDouble("maxForecastRadiusM"));return chosen;
 }
 static JSONObject chatSettings(String text){var out=new JSONObject().put("forecastPolicy129",POLICY).put("baselineWindowS",.6).put("maxForecastRadiusM",1);var m=java.util.regex.Pattern.compile("(?i)maximum forecast radius\\s+([0-9]+(?:\\.[0-9]+)?)\\s*(?:metres?|meters?|m)\\b").matcher(text);if(m.find()){out.put("maxForecastRadiusM",Double.parseDouble(m.group(1)));if(m.find())throw new IllegalArgumentException("Choose one maximum forecast radius");}else if(text.toLowerCase(Locale.ROOT).contains("maximum forecast radius"))throw new IllegalArgumentException("Declare maximum forecast radius as a positive number in metres");validate(out);return out;}
}
