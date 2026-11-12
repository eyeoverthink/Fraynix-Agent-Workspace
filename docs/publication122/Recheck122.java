package fraymus.workspace;
import java.nio.file.*;
import org.json.*;
/** Recheck every recorded native comparison with each applicable evidence validator. */
public final class Recheck122 {
 public static void main(String[] args)throws Exception {
  long trials=0,frames=0;
  for(String folder:args){var d=new JSONObject(Files.readString(Path.of(folder).resolve("result.json")));if(!d.getBoolean("passed"))throw new IllegalStateException("Unfinished comparison "+folder);
   for(Object raw:d.getJSONArray("cases"))for(Object item:((JSONObject)raw).getJSONArray("trials")){var t=(JSONObject)item;trials++;for(Object step:t.getJSONArray("steps")){var s=(JSONObject)step;var b=s.getJSONObject("before");var a=s.getJSONObject("after");if(a.getInt("tick")!=b.getInt("tick")+12||!a.getString("sessionId").equals(b.getString("sessionId")))throw new IllegalStateException("Changed native continuity");if(!t.getString("policy").equals("simple")){var c=a.getJSONObject("controller");int tick=a.getInt("tick");GameEvidence119.verify(c,tick);GameEvidence120.verify(c,tick);GameEvidence121.verify(c,tick);GameEvidence122.verify(c,tick);}frames++;}}
  }
  System.out.println(new JSONObject().put("passed",true).put("trials",trials).put("frames",frames));
 }
}
