package fraymus.workspace;
import java.nio.file.*;import java.util.*;import org.json.*;
public class SearchProbe155 {
 public static void main(String[] a)throws Exception{
  Path root=Path.of(a[0]);var state=new JSONObject(Files.readString(root.resolve("world-observations/state.json"))).getJSONObject("payload");
  JSONObject run=null;for(Object raw:state.getJSONArray("runs")){var v=(JSONObject)raw;if(v.optString("trackerPolicy").equals("identity-return-149"))run=v;}if(run==null)throw new AssertionError();
  final String id=run.getString("datasetId");JSONObject data=null;for(Object raw:state.getJSONArray("datasets")){var d=(JSONObject)raw;if(d.getString("id").equals(id))data=d;}
  var seed=PixelTracker.Box.from(data.getJSONArray("objects").getJSONObject(0).getJSONArray("box"));var first=WorldObservationStore.decode(Files.readAllBytes(root.resolve("world-observations/datasets/"+id+"/0.image")));var rows=new JSONArray();JSONArray expected=null;
  for(var mode:List.of(RotationSearch155.Arithmetic.PRECOMPUTED,RotationSearch155.Arithmetic.GRID)){
   long start=System.nanoTime();var t=new RotationReturn155(first,seed,run.getJSONObject("referenceMemory137"),mode);var trace=new JSONArray();
   for(int i:new int[]{1,2,35,36,40,65})trace.put(t.update(WorldObservationStore.decode(Files.readAllBytes(root.resolve("world-observations/datasets/"+id+"/"+i+".image")))));
   if(expected!=null&&!expected.similar(trace))throw new AssertionError("trace changed");expected=trace;rows.put(new JSONObject().put("mode",mode).put("ms",(System.nanoTime()-start)/1e6));
  }
  var result=new JSONObject().put("passed",true).put("measurements",rows).put("scope","Exposed development only; sparse frames, search path, not complete reconstruction");Files.writeString(Path.of(a[1]),result.toString(2));System.out.println(result);
 }
}
