package fraymus.workspace;
import org.json.*;
import java.nio.file.*;
/** Reuses immutable numerical tracker results; later references only score completed forecasts. */
public final class ForecastStudy129 {
 public static void main(String[] args)throws Exception{
  Path input=Path.of(args[0]),output=Path.of(args[1]);if(Files.exists(output))throw new IllegalStateException("Preserve original result");var source=new JSONObject(Files.readString(input));var records=new JSONArray();
  for(String policy:new String[]{"legacy-adjacent","bounded-budget-1m","bounded-diagnostic-100m"}){
   var d=new JSONObject().put("horizonS",.6).put("maxGapS",.6).put("accelerationBoundMps2",5);
   if(!policy.equals("legacy-adjacent"))d.put("forecastPolicy129",ObservationForecast129.POLICY).put("baselineWindowS",.6).put("maxForecastRadiusM",policy.endsWith("100m")?100:1);
   var record=new JSONObject().put("policy",policy);long start=System.nanoTime();
   try{var forecast=ObservationArtifact128.forecast(source,d);record.put("elapsedMs",(System.nanoTime()-start)/1e6).put("forecast",forecast);double sumRadius=0;int n=0;for(Object raw:forecast.getJSONArray("rows")){var f=((JSONObject)raw).getJSONObject("forecast");if(f.getBoolean("available")){sumRadius+=f.getDouble("radiusM");n++;}}record.put("meanAvailableRadiusM",n==0?JSONObject.NULL:sumRadius/n);var scores=new JSONArray();for(Object raw:source.getJSONArray("scores")){var ref=(JSONObject)raw;var f=forecast.getJSONArray("rows").getJSONObject(ref.getInt("ordinal")-1).getJSONObject("forecast");var score=new JSONObject().put("ordinal",ref.getInt("ordinal")).put("available",f.getBoolean("available"));if(f.getBoolean("available")){double error=PlaneCalibration.distance(PlaneCalibration.point(f.getJSONArray("positionM")),PlaneCalibration.point(ref.getJSONArray("referenceM")));score.put("errorM",error).put("radiusM",f.getDouble("radiusM")).put("coveredIncludingGrid",error<=f.getDouble("radiusM")+Math.sqrt(2)*.125);}scores.put(score);}record.put("independentScores",scores);
   }catch(IllegalArgumentException e){record.put("refused",e.getMessage()).put("elapsedMs",(System.nanoTime()-start)/1e6);}records.put(record);
  }
  var result=new JSONObject().put("inputSha256",PerceptionStore.hash(Files.readAllBytes(input))).put("input",input.getFileName().toString()).put("records",records).put("scope","Post-tracking forecast comparison. Independent later grid references only score completed predictions. Engineering envelopes conditional on measurement and acceleration assumptions; no probability calibration. Single timings include warmup and are not speed claims.");Files.writeString(output,result.toString(2),StandardOpenOption.CREATE_NEW);
 }
}
