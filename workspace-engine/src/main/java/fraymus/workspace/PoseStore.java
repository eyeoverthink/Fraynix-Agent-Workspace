package fraymus.workspace;

import org.json.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/** Durable pose arrangement attempts; one owner thread is the only caller of native physics. */
final class PoseStore implements AutoCloseable {
    private final Path file,nativeDir;
    private final LinkedHashMap<String,JSONObject> runs=new LinkedHashMap<>();
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"push-native-owner");t.setDaemon(true);return t;});
    private volatile boolean closed,paused,cancelled;
    private String active="";private JSONObject current=new JSONObject().put("status","idle");
    PoseStore(Path project) throws Exception {
        file=project.resolve("pose-tasks/state.json");nativeDir=project.resolve("native");
        if(Files.exists(file)){
            var data=new JSONObject(Files.readString(file));
            for(Object raw:data.getJSONArray("runs")){
                var r=(JSONObject)raw;String id=r.getString("id");
                if(Set.of("running","paused","interrupted").contains(r.getString("status"))){
                    try{if(Files.exists(checkpointPath(id))){var envelope=PoseCheckpoint.read(checkpointPath(id),id,r.getJSONObject("definition"));r=envelope.getJSONObject("payload").getJSONObject("run");r.put("checkpoint",summary(envelope));}}
                    catch(Exception e){r.put("checkpointError",e.getMessage());}
                    r.put("status","interrupted").put("reason",r.optString("checkpointError","Process ended; explicit Resume restores both bodies and the committed controller"));
                }
                runs.put(id,r);
            }
            if(!runs.isEmpty()){var r=new ArrayList<>(runs.values()).get(runs.size()-1);var samples=r.getJSONArray("samples");if(!samples.isEmpty())current=view(r,samples.getJSONObject(samples.length()-1));}persist();
        }
    }
    private static JSONObject copy(JSONObject value){return new JSONObject(value.toString());}
    private Path checkpointPath(String id){UUID.fromString(id);return file.getParent().resolve("checkpoints").resolve(id+".json");}
    private synchronized void persist() throws Exception {Files.createDirectories(file.getParent());var pending=file.resolveSibling("state.pending.json");Files.writeString(pending,new JSONObject().put("runs",new JSONArray(runs.values())).toString(),StandardCharsets.UTF_8);Files.move(pending,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
    synchronized boolean hasRun(String id){return runs.containsKey(id);}
    synchronized String status(String id){var r=runs.get(id);if(r==null)throw new IllegalArgumentException("Unknown native pose arrangement run");return r.getString("status");}
    synchronized JSONObject run(String id){var r=runs.get(id);if(r==null)throw new IllegalArgumentException("Unknown native pose arrangement run");return copy(r);}
    synchronized JSONArray list(){var values=new JSONArray();for(var r:runs.values())values.put(new JSONObject().put("id",r.getString("id")).put("sceneId",r.getJSONObject("definition").getString("id")).put("status",r.getString("status")).put("goalId",r.getString("goalId")));return values;}
    synchronized JSONObject snapshot(){return copy(current);}
    synchronized JSONObject stateForScene(String sceneId){if(current.optString("sceneId").equals(sceneId))return snapshot();JSONObject last=null;for(var r:runs.values())if(r.getJSONObject("definition").getString("id").equals(sceneId)&&!r.getJSONArray("samples").isEmpty())last=r;if(last==null)return new JSONObject().put("sceneId",sceneId).put("status","authored");var samples=last.getJSONArray("samples");return view(last,samples.getJSONObject(samples.length()-1));}
    synchronized void available(){if(closed||!active.isEmpty())throw new IllegalStateException("Wait for or pause the active native pose arrangement world");}
    synchronized JSONObject start(String id,String goalId,JSONObject scene,JSONObject plan,boolean realtime,boolean orientation) throws Exception {
        if(runs.containsKey(id))return run(id);available();UUID.fromString(id);if(runs.size()>=32)throw new IllegalStateException("Native pose arrangement history limit32 reached");
        PosePlan.verify(scene,plan);
        var r=new JSONObject().put("id",id).put("goalId",goalId).put("controllerVersion",PosePhysics.VERSION).put("orientationControl",orientation).put("definition",scene).put("plan",plan).put("createdAt",Instant.now()).put("status","running").put("resumes",0).put("samples",new JSONArray()).put("events",new JSONArray());
        runs.put(id,r);active=id;paused=false;cancelled=false;persist();current=new JSONObject().put("id",id).put("sceneId",scene.getString("id")).put("status","running");worker.submit(()->execute(id,realtime,null));return copy(r);
    }
    synchronized JSONObject pause(){if(active.isEmpty())throw new IllegalStateException("No running push to pause");paused=true;return snapshot();}
    synchronized JSONObject cancel(String id) throws Exception {if(id.equals(active)){cancelled=true;paused=false;}else if(runs.containsKey(id)&&Set.of("paused","interrupted").contains(runs.get(id).getString("status"))){var r=runs.get(id);r.put("status","cancelled").put("reason","Explicitly abandoned saved pose arrangement task; no continuation or promotion");var samples=r.getJSONArray("samples");if(!samples.isEmpty()){r.put("final",samples.getJSONObject(samples.length()-1)).put("verification",PoseVerify.verify(r));current=view(r,r.getJSONObject("final"));}persist();}return run(id);}
    synchronized JSONObject resume(String id,JSONObject scene,boolean realtime) throws Exception {
        available();var prior=run(id);if(!Set.of("paused","interrupted").contains(prior.getString("status")))throw new IllegalStateException("Resume a paused/interrupted pose arrangement run; failed attempts require a new task");
        var envelope=PoseCheckpoint.read(checkpointPath(id),id,scene);var payload=envelope.getJSONObject("payload");var r=copy(payload.getJSONObject("run"));var expected=payload.getJSONObject("physics");
        if(r.optInt("resumes")>=3)throw new IllegalStateException("Pushing recovery budget reached");if(expected.getInt("tick")>0)PoseVerify.verify(copy(r).put("final",expected));
        r.put("status","running").put("resumes",r.optInt("resumes")+1).put("checkpoint",summary(envelope));r.remove("reason");
        r.getJSONArray("events").put(new JSONObject().put("kind","resume").put("tick",expected.getInt("tick")).put("expected",expected).put("checkpointSha256",envelope.getString("sha256")));
        runs.put(id,r);active=id;paused=false;cancelled=false;persist();current=view(r,expected);worker.submit(()->execute(id,realtime,payload));return copy(r);
    }
    private static JSONObject summary(JSONObject envelope){var p=envelope.getJSONObject("payload");return new JSONObject().put("tick",p.getJSONObject("physics").getInt("tick")).put("sha256",envelope.getString("sha256")).put("codeSha256",p.getString("codeSha256")).put("version",PoseCheckpoint.VERSION);}
    private JSONObject view(JSONObject run,JSONObject sample){var objects=new JSONArray();for(Object raw:run.getJSONObject("definition").getJSONArray("objects")){var o=copy((JSONObject)raw);if(o.getDouble("massKg")>0){var b=ContactPhysics.dynamic(sample.getJSONArray("bodies"),o.getString("id"));for(String key:List.of("position","rotation","velocity","angularVelocity"))o.put(key,b.get(key));}objects.put(o);}
        var out=new JSONObject().put("id",run.getString("id")).put("sceneId",run.getJSONObject("definition").getString("id")).put("status",run.getString("status")).put("sample",sample).put("current",new JSONObject().put("tick",sample.getInt("tick")).put("objects",objects).put("gripping",!sample.getJSONObject("controller").getString("grippingId").isEmpty()).put("grippingId",sample.getJSONObject("controller").getString("grippingId")).put("controller",sample.getJSONObject("controller"))).put("resumes",run.optInt("resumes")).put("targets",run.getJSONObject("plan").getJSONArray("orderedTargets"));if(run.has("checkpoint"))out.put("checkpoint",run.getJSONObject("checkpoint"));if(run.has("verification"))out.put("verification",run.getJSONObject("verification"));if(run.has("reason"))out.put("reason",run.getString("reason"));return out;
    }
    private JSONObject observe(String id,PosePhysics world){var samples=runs.get(id).getJSONArray("samples");if(!samples.isEmpty()&&samples.getJSONObject(samples.length()-1).getInt("tick")==world.tick)return samples.getJSONObject(samples.length()-1);var sample=world.sample().put("integration",world.takeLedger());if(world.tick>0)samples.put(sample);return sample;}
    private void commit(String id,PosePhysics world,JSONObject sample) throws Exception {var r=runs.get(id);var payload=new JSONObject().put("codeSha256",PoseCheckpoint.identity()).put("run",copy(r)).put("physics",new JSONObject(sample.toString()));var envelope=PoseCheckpoint.write(checkpointPath(id),payload);r.put("checkpoint",summary(envelope));current=view(r,sample);}
    private void execute(String id,boolean realtime,JSONObject restored){
        JSONObject r; synchronized(this){r=runs.get(id);}
        try(var world=new PosePhysics(r.getJSONObject("definition"),r.getJSONObject("plan"),nativeDir,r.getBoolean("orientationControl"),restored==null?null:restored.getJSONObject("physics"),r.getJSONArray("events"))){
            int startTick=world.tick;long started=System.nanoTime();
            synchronized(this){var initial=world.sample().put("integration",new JSONArray());if(restored==null)r.put("initialState",initial);else r.getJSONArray("events").getJSONObject(r.getJSONArray("events").length()-1).put("actualInitialization",initial);commit(id,world,restored==null?initial:restored.getJSONObject("physics"));}
            while(!world.settled()&&!closed&&!paused&&!cancelled){synchronized(this){world.step();if(world.tick%12==0||world.settled()){var sample=observe(id,world);if(world.tick%480==0||world.settled())commit(id,world,sample);}}
                if(realtime){long wait=(long)((world.tick-startTick)*BulletRamp.DT/4*1e9)-(System.nanoTime()-started);if(wait>0)Thread.sleep(Math.min(10,Math.max(1,wait/1_000_000)));}
            }
            synchronized(this){var last=observe(id,world);String status=cancelled?"cancelled":closed?"interrupted":paused?"paused":world.controller.getString("phase").equals("finished")?"completed":"failed";r.put("status",status);if(world.controller.has("reason"))r.put("reason",world.controller.getString("reason"));commit(id,world,last);
                if(!paused&&!closed){r.put("final",last).put("verification",last.getInt("tick")==0?new JSONObject().put("passed",false).put("reason","No native movement completed"):PoseVerify.verify(r));if(status.equals("completed")&&!r.getJSONObject("verification").getBoolean("passed"))r.put("status","failed").put("reason","Original final pose or protected-body gate failed");current=view(r,last);}persist();}
        }catch(Exception e){synchronized(this){r.put("status","interrupted").put("reason",String.valueOf(e.getMessage()));current.put("status","interrupted").put("reason",String.valueOf(e.getMessage()));try{persist();}catch(Exception ignored){}}}finally{synchronized(this){active="";}}
    }
    @Override public void close(){closed=true;worker.shutdownNow();try{worker.awaitTermination(3,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
}
