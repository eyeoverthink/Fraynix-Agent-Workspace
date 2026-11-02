package fraymus.workspace;

import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import org.json.*;

/** One Java-owned continuing agent. The desktop is a leased view/host, not the lifetime owner. */
final class AgentRuntime042 implements AutoCloseable {
    private final Path root,stateFile;
    private final BrainService brain;
    private final AgentClock042 clock;
    private final Witness042 witness;
    private final AgentAppraisal042 appraisal=new AgentAppraisal042();
    private final Supplier<JSONObject> observe;
    private final Runnable detached;
    private final ScheduledExecutorService workers=Executors.newScheduledThreadPool(2,r->{var t=new Thread(r,"java-agent-042");t.setDaemon(true);return t;});
    private final String agentId;
    private final Set<String> completedGoals=new HashSet<>(),replayed=new HashSet<>();
    private final ArrayDeque<JSONObject> experiences=new ArrayDeque<>();
    private JSONObject facts=new JSONObject(),affect=new JSONObject(),neural=new JSONObject(),neuralFile=new JSONObject();
    private String session="",priorSource="",fault="",lastDecision="starting";
    private long ticks,previousActiveMs,lastBeat,lastSeen,lastCheckpoint,lastWitnessAt,lastReplay;
    private boolean enabled,neuralDirty,detaching;
    private volatile boolean closed;
    AgentRuntime042(Path project,BrainService brain,Supplier<JSONObject> observe,Runnable detached,boolean enabled)throws Exception{this(project,brain,observe,detached,enabled,new AgentClock042(),true);}
    AgentRuntime042(Path project,BrainService brain,Supplier<JSONObject> observe,Runnable detached,boolean enabled,AgentClock042 clock,boolean schedule)throws Exception{
        this.brain=brain;this.observe=observe;this.detached=detached;this.clock=clock;root=project.resolve("runtime-042");stateFile=root.resolve("agent-state.json");Files.createDirectories(root);witness=new Witness042(root.resolve("witness"));
        JSONObject old=Files.exists(stateFile)?AgentFiles042.loadChecked(stateFile):new JSONObject();
        agentId=old.optString("agentId",UUID.randomUUID().toString());UUID.fromString(agentId);ticks=old.optLong("logicalTicks",0);previousActiveMs=old.optLong("retainedActiveMs",0);this.enabled=old.optBoolean("enabled",enabled);priorSource=old.optString("priorSource","");
        appraisal.restore(old.optJSONObject("appraisal")==null?new JSONObject():old.getJSONObject("appraisal"));
        for(Object value:old.optJSONArray("experiences")==null?new JSONArray():old.getJSONArray("experiences"))experiences.add((JSONObject)value);
        if(experiences.size()>128)throw new IllegalStateException("Agent experience checkpoint exceeds its format");
        for(Object id:old.optJSONArray("completedGoals")==null?new JSONArray():old.getJSONArray("completedGoals"))completedGoals.add(String.valueOf(id));
        for(Object id:old.optJSONArray("replayed")==null?new JSONArray():old.getJSONArray("replayed"))replayed.add(String.valueOf(id));
        if(old.has("neural")&&!old.getJSONObject("neural").isEmpty()){neuralFile=old.getJSONObject("neural");Path file=neuralPath(neuralFile.getString("file"));brain.restore042(file,neuralFile);neural=old.optJSONObject("neuralState")==null?new JSONObject():old.getJSONObject("neuralState");}
        if(old.has("witnessAnchor")&&!witness.hasAnchor(old.getJSONObject("witnessAnchor")))throw new IllegalStateException("Agent witness checkpoint anchor is missing; original retained");
        event("boot",new JSONObject().put("previousBootId",old.optString("bootId","")).put("previousSavedAt",old.optString("savedAt","")).put("recovery",old.isEmpty()?"new agent":"state restored; downtime is not activity").put("priorActiveMs",previousActiveMs));
        checkpoint();if(schedule){workers.scheduleWithFixedDelay(this::beat,0,250,TimeUnit.MILLISECONDS);workers.scheduleWithFixedDelay(this::collect,0,1000,TimeUnit.MILLISECONDS);}
    }
    private Path neuralPath(String name){if(!name.matches("cortex-[a-f0-9-]+\\.bin"))throw new IllegalArgumentException("Invalid owned neural checkpoint path");return root.resolve(name);}
    private void event(String kind,JSONObject data)throws Exception{data.put("agentId",agentId).put("bootId",clock.bootId).put("elapsedMs",clock.elapsedMs()).put("logicalTick",ticks).put("wallTime",clock.wall());witness.append(kind,data);}
    synchronized void beat(){if(closed)return;ticks++;long now=clock.elapsedMs();lastBeat=now;}
    void collect(){
        if(closed)return;long start=clock.elapsedMs();JSONObject next;
        try{next=observe.get();next=WorldFrames.copy(next);next.put("collectedAt",clock.wall()).put("collectionMs",clock.elapsedMs()-start);}
        catch(Exception error){next=new JSONObject().put("observationFresh",false).put("sourceError",String.valueOf(error.getMessage())).put("source",new JSONObject()).put("collectedAt",clock.wall()).put("collectionMs",clock.elapsedMs()-start);}
        boolean expire=false;
        try{synchronized(this){
            if(closed)return;long now=clock.elapsedMs();if(!session.isEmpty()&&now-lastSeen>7000){event("interface-lost",new JSONObject().put("sessionId",session).put("meaning","Host lease expired; no browser/camera capability remains live"));session="";detaching=true;expire=true;}
            String digest=WorldFrames.hash(next.optJSONObject("source")==null?new JSONObject():next.getJSONObject("source"));boolean changed=!digest.equals(priorSource);next.put("sourceChanged",changed);priorSource=digest;
            boolean completed=false;int failures=0;for(Object raw:next.optJSONArray("goals")==null?new JSONArray():next.getJSONArray("goals")){var goal=(JSONObject)raw;if(Set.of("completed","failed","cancelled").contains(goal.optString("status"))&&completedGoals.add(goal.getString("id"))){if(goal.getString("status").equals("completed"))completed=true;else failures++;event("goal-outcome",new JSONObject().put("goal",goal).put("meaning","Actual Java goal status; independent task metrics remain separately qualified"));}}
            next.put("newCompletedGoal",completed).put("newFailedGoals",failures).put("hasRetainedEvidence",!experiences.isEmpty());affect=appraisal.update(next,Math.max(1,now-facts.optLong("collectedElapsedMs",0)),enabled&&fault.isEmpty());next.put("collectedElapsedMs",now);facts=next;lastDecision=affect.getString("decision");
            if(affect.getBoolean("allowNeuralUpdate")){
                float[] features=features(next);neural=brain.background042(features,true);neuralDirty|=neural.optBoolean("updated");
                var experience=new JSONObject().put("id",UUID.randomUUID().toString()).put("sourceSha256",digest).put("source",next.optJSONObject("source")==null?new JSONObject():next.getJSONObject("source")).put("features",new JSONArray(features)).put("recordedAt",clock.wall()).put("decision",lastDecision).put("qualification",next.optBoolean("observationFresh")?"Fresh source-qualified observation":"Operational/recorded source, no fresh external measurement");
                experiences.addLast(experience);while(experiences.size()>128){var removed=experiences.removeFirst();replayed.remove(removed.getString("id"));}
            }else if(enabled&&lastDecision.equals("consolidate")&&now-lastReplay>5000){
                for(var experience:experiences)if(!replayed.contains(experience.getString("id"))){var a=experience.getJSONArray("features");float[] x=new float[a.length()];for(int i=0;i<x.length;i++)x[i]=(float)a.getDouble(i);neural=brain.background042(x,true);neuralDirty|=neural.optBoolean("updated");lastReplay=now;if(neural.optBoolean("updated")){replayed.add(experience.getString("id"));event("experience-replay",new JSONObject().put("experienceId",experience.getString("id")).put("sourceSha256",experience.getString("sourceSha256")).put("meaning","Retained sensory replay, not a new observation or demonstrated policy improvement"));}break;}
            }
            if((changed||completed||!fault.isEmpty())&&now-lastWitnessAt>=15000){event("appraisal",new JSONObject().put("sourceSha256",digest).put("appraisal",affect).put("neural",neural));lastWitnessAt=now;}
            if(now-lastCheckpoint>=30000)checkpoint();
        }}catch(Exception error){synchronized(this){fault=String.valueOf(error.getMessage());enabled=false;lastDecision="integrity-attention";}}
        if(expire)try{detached.run();}catch(Exception error){synchronized(this){fault="Host detach: "+error.getMessage();}}finally{synchronized(this){detaching=false;}}
    }
    private static float[] features(JSONObject facts){var source=facts.optJSONObject("source");var p=source==null?null:source.optJSONArray("positionM");var camera=source==null?null:source.optJSONObject("camera");var plane=camera==null?null:camera.getJSONObject("estimate").optJSONArray("positionM");return new float[]{facts.optBoolean("observationFresh")?1:-1,(float)facts.optDouble("heapFraction",0),(float)Math.min(1,facts.optInt("activeGoals")/4.0),facts.optBoolean("newCompletedGoal")?1:0,p==null?0:(float)Math.tanh(p.optDouble(0)),p==null?0:(float)Math.tanh(p.optDouble(1)),p==null?0:(float)Math.tanh(p.optDouble(2)),plane==null?0:(float)Math.tanh(plane.optDouble(0)),plane==null?0:(float)Math.tanh(plane.optDouble(1))};}
    synchronized JSONObject attach(String id)throws Exception{UUID.fromString(id);if(detaching||!session.isEmpty()&&clock.elapsedMs()-lastSeen>7000)throw new IllegalStateException("Previous interface cleanup is pending");if(!session.isEmpty()&&!session.equals(id))throw new IllegalStateException("Another interface owns this agent lease");session=id;lastSeen=clock.elapsedMs();event("interface-attached",new JSONObject().put("sessionId",id));checkpoint();return state();}
    synchronized JSONObject touch(String id){if(!session.equals(id)||clock.elapsedMs()-lastSeen>7000)throw new IllegalStateException("Stale interface lease");lastSeen=clock.elapsedMs();return state();}
    synchronized void requireLease(String id){if(id==null||!session.equals(id)||clock.elapsedMs()-lastSeen>7000)throw new IllegalStateException("This interface no longer owns the Java agent lease; reopen to reconnect");}
    JSONObject detach(String id)throws Exception{synchronized(this){if(!session.equals(id))throw new IllegalStateException("Stale interface cannot detach a new owner");event("interface-detached",new JSONObject().put("sessionId",id));session="";detaching=true;checkpoint();}try{detached.run();}finally{synchronized(this){detaching=false;}}return state();}
    synchronized JSONObject control(String action)throws Exception{if(!Set.of("pause","resume","checkpoint").contains(action))throw new IllegalArgumentException("Unknown agent control");if(action.equals("resume")&&!fault.isEmpty())throw new IllegalStateException("Resolve integrity attention before resuming");if(action.equals("pause"))enabled=false;if(action.equals("resume"))enabled=true;event("background-control",new JSONObject().put("action",action));checkpoint();return state();}
    synchronized JSONObject state(){return new JSONObject().put("schema","persistent-java-agent-042-v1").put("agentId",agentId).put("bootId",clock.bootId).put("pid",ProcessHandle.current().pid()).put("clock",clock.read(ticks,previousActiveMs)).put("lastHeartbeatElapsedMs",lastBeat).put("backgroundEnabled",enabled).put("interfaceAttached",!session.isEmpty()&&clock.elapsedMs()-lastSeen<=7000).put("interfaceAgeMs",session.isEmpty()?JSONObject.NULL:clock.elapsedMs()-lastSeen).put("status",fault.isEmpty()?(enabled?"awake":"background-paused"):"attention-required").put("fault",fault).put("decision",lastDecision).put("appraisal",WorldFrames.copy(affect)).put("observations",WorldFrames.copy(facts)).put("neural",WorldFrames.copy(neural)).put("experienceCount",experiences.size()).put("replayedExperiences",replayed.size()).put("witness",witness.summary()).put("recentWitnesses",witness.recent()).put("limits","Persistent operational agent and real shared Java sensory dynamics. Background work is not subjective awareness, new physical truth or proof of useful learning. No camera starts, browser actions or LLM calls occur on this heartbeat.");}
    synchronized JSONObject audit()throws Exception{return witness.verify().put("agentId",agentId).put("bootId",clock.bootId);}
    synchronized void checkpoint()throws Exception{
        witness.verify();JSONObject previous=neuralFile;
        if(neuralDirty||brain.cortexTicks042()!=neuralFile.optLong("ticks",-1)){var path=root.resolve("cortex-"+UUID.randomUUID()+".bin");neuralFile=brain.checkpoint042(path);}
        var data=new JSONObject().put("agentId",agentId).put("bootId",clock.bootId).put("logicalTicks",ticks).put("retainedActiveMs",previousActiveMs+clock.elapsedMs()).put("enabled",enabled).put("priorSource",priorSource).put("savedAt",clock.wall()).put("appraisal",appraisal.save()).put("witnessAnchor",witness.summary()).put("experiences",new JSONArray(experiences)).put("replayed",new JSONArray(replayed)).put("completedGoals",new JSONArray(completedGoals)).put("neural",neuralFile).put("neuralState",neural);
        AgentFiles042.saveChecked(stateFile,data);neuralDirty=false;lastCheckpoint=clock.elapsedMs();
        if(!previous.isEmpty()&&!previous.optString("file").equals(neuralFile.optString("file")))Files.deleteIfExists(neuralPath(previous.getString("file")));
    }
    @Override public void close(){synchronized(this){if(closed)return;closed=true;}workers.shutdownNow();try{workers.awaitTermination(8,TimeUnit.SECONDS);synchronized(this){event("shutdown",new JSONObject().put("reason","Owned runtime stopped"));checkpoint();}}catch(Exception error){System.err.println("Agent checkpoint retained with attention: "+error.getMessage());}}
}
