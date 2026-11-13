package fraymus.workspace;
import java.nio.file.*;
import java.util.*;
import org.json.*;

/** Same independently retained completed source histories; compatible with published136 binaries. */
public final class ObservationCost137Study {
 public static void main(String[] args)throws Exception{
  Path source=Path.of(args[0]),output=Path.of(args[1]);if(Files.exists(output))throw new IllegalStateException("Preserve previous cost attempt");Files.createDirectories(output);
  byte[] original=Files.readAllBytes(source.resolve("world-observations/state.json"));var envelope=new JSONObject(new String(original,java.nio.charset.StandardCharsets.UTF_8));var all=envelope.getJSONObject("payload");if(!WorldFrames.hash(all).equals(envelope.getString("sha256")))throw new IllegalStateException("Original source state changed");
  var rows=new JSONArray();
  for(int count:new int[]{1,3,6}){
   var data=WorldFrames.copy(all);var runs=new JSONArray();var ids=new HashSet<String>();var datasets=new HashSet<String>();
   for(int i=0;i<count;i++){var run=data.getJSONArray("runs").getJSONObject(i);if(!run.getString("status").equals("completed"))throw new IllegalStateException("Completed source runs required");runs.put(run);ids.add(run.getString("id"));datasets.add(run.getString("datasetId"));}
   data.put("runs",runs);var ds=new JSONArray();for(Object raw:data.getJSONArray("datasets"))if(datasets.contains(((JSONObject)raw).getString("id")))ds.put(raw);data.put("datasets",ds);
   for(String world:data.getJSONObject("worlds").keySet()){var w=data.getJSONObject("worlds").getJSONObject(world);var history=new JSONArray();var tracks=new JSONObject();
    for(Object raw:w.getJSONArray("history")){var h=(JSONObject)raw;if(!ids.contains(h.getString("runId")))break;history.put(h);for(String body:h.getJSONObject("updates").keySet())tracks.put(body,h.getJSONObject("updates").get(body));}
    w.put("history",history).put("revision",history.length()).put("tracks",tracks);if(!history.isEmpty())w.put("nativeBinding",history.getJSONObject(history.length()-1).getJSONObject("nativeBinding"));
   }
   var project=output.resolve("project-"+count);Files.createDirectories(project.resolve("world-observations"));Files.createDirectories(project.resolve("persistent-worlds"));Files.copy(source.resolve("persistent-worlds/state.json"),project.resolve("persistent-worlds/state.json"));
   for(Object raw:ds){var d=(JSONObject)raw;Path target=project.resolve("world-observations/datasets").resolve(d.getString("id"));Files.createDirectories(target);for(int i=0;i<d.getJSONArray("frames").length();i++)Files.copy(source.resolve("world-observations/datasets").resolve(d.getString("id")).resolve(i+".image"),target.resolve(i+".image"));}
   byte[] seed=new JSONObject().put("version",envelope.get("version")).put("payload",data).put("sha256",WorldFrames.hash(data)).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);Files.write(project.resolve("world-observations/state.json"),seed);
   var row=new JSONObject().put("runs",count).put("seedBytes",seed.length).put("seedSha256",PerceptionStore.hash(seed));long start=System.nanoTime();
   try(var store=new WorldObservationStore(project,new WorldLedger(project))){row.put("coldOpenMs",(System.nanoTime()-start)/1e6);var values=new JSONArray();var run=runs.getJSONObject(runs.length()-1);String id=run.getString("id"),dataset=run.getString("datasetId");
    for(int repeat=0;repeat<3;repeat++)for(String operation:List.of("summary","run","dataset","image")){start=System.nanoTime();JSONObject result=switch(operation){case "summary"->store.state("");case "run"->store.read(id);case "dataset"->store.dataset(dataset);default->new JSONObject().put("sha256",PerceptionStore.hash(store.image(dataset,1)));};double ms=(System.nanoTime()-start)/1e6;if(operation.equals("summary"))result.remove("codeSha256");values.put(new JSONObject().put("repeat",repeat).put("operation",operation).put("ms",ms).put("resultSha256",WorldFrames.hash(result)));}
    row.put("reads",values);var writes=new JSONArray();
    Class<?> tx=Class.forName("fraymus.workspace.WorldObservationStore$Tx");var access=WorldObservationStore.class.getDeclaredMethod("access",boolean.class,tx);access.setAccessible(true);
    for(int repeat=0;repeat<3;repeat++){final int n=repeat;Object callback=java.lang.reflect.Proxy.newProxyInstance(tx.getClassLoader(),new Class<?>[]{tx},(proxy,method,arguments)->{
      var state=(JSONObject)arguments[0];for(Object raw:state.getJSONArray("runs")){var r=(JSONObject)raw;if(r.getString("id").equals(id))r.put("reason","Deterministic transaction cost probe137 "+n);}
      return WorldFrames.hash(state);
    });start=System.nanoTime();Object result=access.invoke(store,true,callback);double ms=(System.nanoTime()-start)/1e6;
    var persisted=new JSONObject(Files.readString(project.resolve("world-observations/state.json")));
    writes.put(new JSONObject().put("repeat",repeat).put("ms",ms).put("resultSha256",result).put("persistedSha256",WorldFrames.hash(persisted.getJSONObject("payload"))));}
    row.put("writes",writes);try{row.put("verificationCost",WorldObservationStore.class.getDeclaredMethod("verificationCost136").invoke(store));}catch(NoSuchMethodException ignored){row.put("verificationCost",JSONObject.NULL);}
   }
   rows.put(row);Files.writeString(output.resolve("result.json"),new JSONObject().put("originalStateSha256",PerceptionStore.hash(original)).put("rows",rows).put("scope","Derived exact first1/3/6 completed prefixes of the same public study; original revision hashes retained. Same read workload and three deterministic reason-field transactions on isolated derived copies; transaction probe is not an end-to-end task. Fresh copies. Result hashes exclude current engine identity only in summaries; original run identities are untouched.").toString(2));
  }
 }
}
