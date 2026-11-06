package fraymus.workspace;
import java.nio.file.*;
import org.json.*;
/** Study preflight: invokes the unchanged installed task-distance implementation. */
public class CheckCalibrationOrder083 {
 public static void main(String[] args)throws Exception {
  var p=new JSONObject(Files.readString(Path.of(args[0])));var source=new JSONObject(Files.readString(Path.of(args[1]))).getJSONObject("data").getJSONObject("spec");
  var calibration=p.getJSONArray("calibration");var order=new int[]{1,0};var prior=new JSONArray().put(source);var results=new JSONArray();
  for(int i:order){var task=calibration.getJSONObject(i);var distances=new JSONArray();double best=Double.POSITIVE_INFINITY;int nearest=-1;for(int k=0;k<prior.length();k++){double d=Procedure078.distance(prior.getJSONObject(k),task);distances.put(d);if(d<best){best=d;nearest=k;}}if(nearest!=0)throw new IllegalStateException("Calibration order does not preserve original receipt for "+task.getString("id"));results.put(new JSONObject().put("task",task.getString("id")).put("distances",distances));prior.put(task);}
  int index=0;for(Object raw:p.getJSONArray("cases")){var task=(JSONObject)raw;var distances=new JSONArray();double best=Double.POSITIVE_INFINITY;int nearest=-1;for(int k=0;k<prior.length();k++){double d=Procedure078.distance(prior.getJSONObject(k),task);distances.put(d);if(d<best){best=d;nearest=k;}}int expected=index++==0?0:2;if(nearest!=expected)throw new IllegalStateException("Unexpected geometric receipt for "+task.getString("id"));results.put(new JSONObject().put("task",task.getString("id")).put("distances",distances).put("nearest",nearest==0?"seed":"calibration-0").put("estimateExpected",nearest==0));}
  System.out.println(new JSONObject().put("passed",true).put("memoryCalibrationOrder",new JSONArray(order)).put("results",results).put("scope","Only collection order changes; no scene, case, controller, cost or threshold changes. Exact installed Java distance implementation, not a timing-outcome search.").toString(2));
 }
}
