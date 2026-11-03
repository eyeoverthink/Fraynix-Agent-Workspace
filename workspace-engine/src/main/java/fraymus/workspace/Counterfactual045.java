package fraymus.workspace;

import org.json.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/** Native alternatives share one frozen frame; this worker never writes the live world. */
final class Counterfactual045 implements AutoCloseable {
    static final String VERSION="native-counterfactual-045-v1";
    static final String SCOPE="Bounded native simulation alternatives using the unchanged virtual force/torque gripper. Simulation forecasts compared with simulation execution, not a learned model of reality or physical robot control.";
    private final Path root,file; private final WorldLedger worlds; private JSONObject data;
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"java-counterfactual-045");t.setDaemon(true);return t;});
    private volatile boolean closed; private WorldNativeStore owner; private String active="",activeRun="";
    Counterfactual045(Path project,WorldLedger worlds)throws Exception{
        this.worlds=worlds;root=project.resolve("counterfactual-045");file=root.resolve("studies.json");
        data=Files.exists(file)?AgentFiles042.loadChecked(file):new JSONObject().put("studies",new JSONArray());
        for(Object raw:data.getJSONArray("studies")){var s=(JSONObject)raw;if(s.getString("status").equals("running"))s.put("status","interrupted").put("reason","Java ended; explicitly Resume. Completed alternatives remain bound; an unfinished virtual trial is retained before restart.");}
        save();
    }
    static JSONObject protocol(){try(var in=Counterfactual045.class.getResourceAsStream("/counterfactual045-protocol.json")){return new JSONObject(new String(in.readAllBytes(),StandardCharsets.UTF_8));}catch(Exception e){throw new IllegalStateException(e);}}
    static String code(){return WorldFrames.hash(new JSONObject().put("native",WorldNativeCheckpoint.identity()).put("protocol",WorldFrames.hash(protocol())).put("worker",implementationHash()));}
    private static String implementationHash(){var value=new JSONObject().put("version",VERSION);for(Class<?> t:new Class<?>[]{Counterfactual045.class,CounterfactualReport045.class,Design053.class})try(var in=t.getResourceAsStream(t.getSimpleName()+".class")){value.put(t.getSimpleName(),MapDefinition.sha(in.readAllBytes()));}catch(Exception e){throw new IllegalStateException(e);}return WorldFrames.hash(value);}
    private void save()throws Exception{if(data.toString().length()>2_000_000)throw new IllegalStateException("Counterfactual summary budget2MB reached");AgentFiles042.saveChecked(file,data);}
    private JSONObject study(String id){UUID.fromString(id);for(Object raw:data.getJSONArray("studies"))if(((JSONObject)raw).getString("id").equals(id))return(JSONObject)raw;throw new IllegalArgumentException("Unknown counterfactual study");}
    private Path branch(String id){UUID.fromString(id);return root.resolve("branches").resolve(id);}
    synchronized JSONObject prepare(JSONObject binding,JSONObject base)throws Exception{
        if(closed||!active.isEmpty())throw new IllegalStateException("Wait for the owned counterfactual study");if(data.getJSONArray("studies").length()>=8)throw new IllegalStateException("Counterfactual history budget8 reached");
        var source=worlds.source(binding);WorldPlan.verify(source,base);current(binding);var candidates=new JSONArray();var seen=new HashMap<String,String>();
        boolean design=base.getJSONObject("intent").has("construction051");var proposals=design?Design053.proposals(source,base):new JSONArray();
        String[] names=design?new String[]{"quarter-force","balanced-force","maximum-force"}:new String[]{"existing-controller-order","reversed-target-order","half-force"};
        for(int i=0;i<3;i++){
            var intent=WorldFrames.copy(base.getJSONObject("intent"));if(!design&&i==1){JSONArray a=intent.getJSONArray("targets"),rev=new JSONArray();for(int k=a.length()-1;k>=0;k--)rev.put(a.get(k));intent.put("targets",rev);}if(!design&&i==2)intent.put("forceLimitN",Math.max(2,intent.getDouble("forceLimitN")*.5));
            var plan=design?proposals.getJSONObject(i).getJSONObject("plan"):WorldPlan.build(source,intent);intent=plan.getJSONObject("intent");String motion=WorldFrames.hash(new JSONObject().put("order",plan.getJSONArray("orderedTargets")).put("height",plan.getDouble("liftHeightM")).put("force",intent.getDouble("forceLimitN")).put("torque",intent.getDouble("torqueLimitNm")));
            var c=new JSONObject().put("id",names[i]).put("ordinal",i).put("plan",plan).put("planSha256",WorldFrames.hash(plan)).put("motionSha256",motion).put("attempts",new JSONArray()).put("status","queued");
            if(design)c.put("hypothesis053",proposals.getJSONObject(i).getJSONObject("hypothesis053"));
            if(!plan.getBoolean("passed"))c.put("status","rejected").put("reason",plan.getString("reason"));else if(seen.containsKey(motion))c.put("status","duplicate").put("duplicateOf",seen.get(motion));else seen.put(motion,names[i]);candidates.put(c);
        }
        var s=new JSONObject().put("id",UUID.randomUUID().toString()).put("version",VERSION).put("scope",SCOPE).put("binding",WorldFrames.copy(binding)).put("source",source).put("sourceFrameSha256",WorldFrames.hash(WorldFrames.frame(source))).put("codeSha256",code()).put("originalIntent",base.getJSONObject("intent")).put("createdAt",Instant.now()).put("status","queued").put("resumes",0).put("candidates",candidates).put("policy",design?Design053.policy():protocol().getJSONObject("policy"));if(design)s.put("design053",Design053.policy());
        data.getJSONArray("studies").put(s);save();return view(s);
    }
    private void current(JSONObject binding)throws Exception{current(binding,"");}
    private void current(JSONObject binding,String owner)throws Exception{var w=worlds.observe(binding.getString("worldId"));for(String k:List.of("expectedRevision","expectedHash"))if(!binding.get(k).equals(w.get(k)))throw new IllegalStateException("Stale counterfactual world head; start a fresh study");if(w.has("pending")&&!w.getJSONObject("pending").getString("goalId").equals(owner))throw new IllegalStateException("A different live goal already owns the world");}
    synchronized JSONObject begin(String id)throws Exception{var s=study(id);if(s.getString("status").equals("queued")){if(closed||!active.isEmpty())throw new IllegalStateException("Counterfactual compute is occupied");compatible(s);s.put("status","running");active=id;save();worker.submit(()->forecast(id));}return view(s);}
    private void compatible(JSONObject s){if(s.has("design053")&&(!s.getJSONObject("design053").similar(Design053.policy())||!s.getJSONObject("policy").similar(Design053.policy())))throw new IllegalStateException("Assembly design policy changed");if(!code().equals(s.getString("codeSha256"))||!WorldFrames.hash(WorldFrames.frame(s.getJSONObject("source"))).equals(s.getString("sourceFrameSha256")))throw new IllegalStateException("Counterfactual source/controller/protocol changed");}
    synchronized boolean busy(){return !active.isEmpty();}
    synchronized JSONObject read(String id){return view(study(id)).put("computeActive",id.equals(active));}
    static JSONObject view(JSONObject s){var out=WorldFrames.copy(s);out.remove("source");var cs=out.getJSONArray("candidates");for(Object raw:cs){var c=(JSONObject)raw;c.remove("plan");c.remove("predictedFrame");}return out;}
    private void forecast(String id){
        try(var nativeStore=new WorldNativeStore(branch(id))){
            synchronized(this){owner=nativeStore;}
            for(int index=0;index<3;index++){
                JSONObject s,c; synchronized(this){s=WorldFrames.copy(study(id));if(closed||!s.getString("status").equals("running"))return;c=s.getJSONArray("candidates").getJSONObject(index);if(!c.getString("status").equals("queued"))continue;}
                compatible(s);String runId=UUID.randomUUID().toString();synchronized(this){activeRun=runId;var saved=study(id).getJSONArray("candidates").getJSONObject(index);saved.getJSONArray("attempts").put(runId);saved.put("status","running");save();}
                nativeStore.start(runId,id,s.getJSONObject("source"),c.getJSONObject("plan"),false,true);long began=System.nanoTime();boolean budget=false;
                while(nativeStore.status(runId).equals("running")){
                    boolean stop;synchronized(this){stop=closed||!study(id).getString("status").equals("running");}
                    if(stop||(System.nanoTime()-began)>90_000_000_000L){nativeStore.cancel(runId);budget=!stop;}
                    Thread.sleep(10);
                }
                var run=nativeStore.run(runId);var metrics=WorldVerify.verify(run);
                synchronized(this){var saved=study(id).getJSONArray("candidates").getJSONObject(index);saved.put("runId",runId).put("runSha256",WorldFrames.hash(run)).put("metrics",metrics).put("status",run.getString("status")).put("reason",budget?"Declared90-second candidate budget reached":run.optString("reason",metrics.optBoolean("passed")?"Original native gates passed":"Original native gates failed"));if(s.has("design053"))saved.put("structure053",Design053.structure(run));if(metrics.optBoolean("passed")&&run.getString("status").equals("completed"))saved.put("predictedFrame",WorldFrames.after(WorldFrames.frame(s.getJSONObject("source")),run));activeRun="";save();if(closed||!study(id).getString("status").equals("running"))return;}
            }
            synchronized(this){var s=study(id);var selected=select(s);if(selected==null)s.put("status","failed").put("reason","No alternative passed the original native gates; the live world was not changed");else{var decision=new JSONObject().put("studyId",id).put("sourceFrameSha256",s.getString("sourceFrameSha256")).put("codeSha256",s.getString("codeSha256")).put("selectedCandidate",selected.getString("id")).put("selectedPlanSha256",selected.getString("planSha256")).put("candidatesSha256",WorldFrames.hash(new JSONObject().put("candidates",s.getJSONArray("candidates"))));s.put("decision",decision).put("decisionSha256",WorldFrames.hash(decision)).put("status","ready").put("reason",s.has("design053")?"Original native and bridge support gates, then measured effort, duration and declaration ordinal":"Original native gates, then minimum duration, effort and declaration ordinal");}save();}
        }catch(Exception error){synchronized(this){try{var s=study(id);if(!s.getString("status").equals("cancelled"))s.put("status","interrupted").put("reason",String.valueOf(error.getMessage()));save();}catch(Exception failed){System.err.println(failed.getMessage());}}}
        finally{synchronized(this){owner=null;active="";activeRun="";}}
    }
    private static JSONObject select(JSONObject study){return study.has("design053")?Design053.choose(study.getJSONArray("candidates")):choose(study.getJSONArray("candidates"));}
    static JSONObject choose(JSONArray candidates){JSONObject best=null;for(Object raw:candidates){var c=(JSONObject)raw;var m=c.optJSONObject("metrics");if(!c.getString("status").equals("completed")||m==null||!m.optBoolean("passed"))continue;if(best==null||compare(c,best)<0)best=c;}return best;}
    private static int compare(JSONObject a,JSONObject b){JSONObject x=a.getJSONObject("metrics"),y=b.getJSONObject("metrics");int n=Integer.compare(x.getInt("nativeTicks"),y.getInt("nativeTicks"));if(n==0)n=Double.compare(x.getDouble("effortNs"),y.getDouble("effortNs"));return n==0?Integer.compare(a.getInt("ordinal"),b.getInt("ordinal")):n;}
    private JSONObject candidate(JSONObject s,String id){for(Object raw:s.getJSONArray("candidates"))if(((JSONObject)raw).getString("id").equals(id))return(JSONObject)raw;throw new IllegalStateException("Selected candidate missing");}
    private JSONObject verified(JSONObject s)throws Exception{
        compatible(s);if(!s.getString("status").equals("ready")||!WorldFrames.hash(s.getJSONObject("decision")).equals(s.getString("decisionSha256"))||!WorldFrames.hash(new JSONObject().put("candidates",s.getJSONArray("candidates"))).equals(s.getJSONObject("decision").getString("candidatesSha256")))throw new IllegalStateException("Counterfactual decision changed or not ready");
        try(var nativeStore=new WorldNativeStore(branch(s.getString("id")))){for(Object raw:s.getJSONArray("candidates")){var c=(JSONObject)raw;if(c.has("runId")){var run=nativeStore.run(c.getString("runId"));if(!WorldFrames.hash(run).equals(c.getString("runSha256"))||!WorldVerify.verify(run).similar(c.getJSONObject("metrics"))||!run.getJSONObject("definition").similar(s.getJSONObject("source"))||!WorldFrames.hash(run.getJSONObject("plan")).equals(c.getString("planSha256")))throw new IllegalStateException("Original counterfactual trial/metrics changed");if(s.has("design053")&&!Design053.structure(run).similar(c.getJSONObject("structure053")))throw new IllegalStateException("Original assembly support evidence changed");}}}
        var selected=select(s);if(selected==null||!selected.getString("id").equals(s.getJSONObject("decision").getString("selectedCandidate")))throw new IllegalStateException("Counterfactual ranking changed");return selected;
    }
    synchronized JSONObject executePlan(String id,String decision)throws Exception{var s=study(id);if(!s.optString("decisionSha256").equals(decision))throw new IllegalStateException("Selected decision hash changed");current(s.getJSONObject("binding"));return WorldFrames.copy(verified(s).getJSONObject("plan"));}
    synchronized JSONObject ownedPlan(String id,String decision,String owner)throws Exception{var s=study(id);if(!s.optString("decisionSha256").equals(decision))throw new IllegalStateException("Selected decision hash changed");current(s.getJSONObject("binding"),owner);return WorldFrames.copy(verified(s).getJSONObject("plan"));}
    synchronized JSONObject audit(String id)throws Exception{var s=study(id);verified(s);return view(s);}
    synchronized JSONObject run(String id,String candidate)throws Exception{var s=study(id);var c=candidate(s,candidate);try(var store=new WorldNativeStore(branch(id))){var run=store.run(c.getString("runId"));if(!WorldFrames.hash(run).equals(c.getString("runSha256")))throw new IllegalStateException("Original forecast bytes changed");return run;}}
    synchronized JSONArray attempts(String id)throws Exception{var s=study(id);var out=new JSONArray();try(var store=new WorldNativeStore(branch(id))){for(Object raw:s.getJSONArray("candidates")){var c=(JSONObject)raw;for(Object value:c.getJSONArray("attempts")){var run=store.run(value.toString());out.put(new JSONObject().put("candidate",c.getString("id")).put("run",WorldReport.packed(run)).put("status",run.getString("status")));}}}return out;}
    synchronized JSONObject export(String id)throws Exception{var s=study(id);if(s.getString("status").equals("ready"))verified(s);return new JSONObject().put("format","counterfactual-world-045-export-v1").put("study",WorldFrames.copy(s)).put("originalAttempts",attempts(id)).put("scope",SCOPE);}
    synchronized JSONObject compareActual(String id,JSONObject actual)throws Exception{
        var s=study(id);var c=verified(s);var forecast=run(id,c.getString("id"));if(!actual.getJSONObject("definition").similar(s.getJSONObject("source"))||!actual.getJSONObject("plan").similar(c.getJSONObject("plan"))||!WorldVerify.verify(actual).getBoolean("passed"))throw new IllegalStateException("Actual run is not the selected source/plan or failed native gates");
        if(s.has("design053")&&!Design053.structure(actual).getBoolean("passed"))throw new IllegalStateException("Selected actual construction lacks original support/stability proof");
        var predicted=new HashMap<Integer,JSONObject>();for(Object raw:forecast.getJSONArray("samples")){var f=(JSONObject)raw;predicted.put(f.getInt("tick"),f);}int matched=0;double maxPosition=0,maxAngle=0;
        for(Object raw:actual.getJSONArray("samples")){var a=(JSONObject)raw;var f=predicted.get(a.getInt("tick"));if(f==null)continue;matched++;for(Object item:a.getJSONArray("bodies")){var b=(JSONObject)item;var p=ContactPhysics.dynamic(f.getJSONArray("bodies"),b.getString("id"));maxPosition=Math.max(maxPosition,WorldVerify.distance(p.getJSONArray("position"),b.getJSONArray("position")));maxAngle=Math.max(maxAngle,WorldPlan.angle(p.getJSONArray("rotation"),b.getJSONArray("rotation")));}}
        var rows=new JSONArray();for(Object raw:actual.getJSONObject("final").getJSONArray("bodies")){var a=(JSONObject)raw;var f=ContactPhysics.dynamic(forecast.getJSONObject("final").getJSONArray("bodies"),a.getString("id"));rows.put(new JSONObject().put("id",a.getString("id")).put("forecastPositionM",f.getJSONArray("position")).put("actualPositionM",a.getJSONArray("position")).put("positionResidualM",WorldVerify.distance(f.getJSONArray("position"),a.getJSONArray("position"))).put("angleResidualDeg",WorldPlan.angle(f.getJSONArray("rotation"),a.getJSONArray("rotation"))));}
        double coverage=matched/(double)Math.max(forecast.getJSONArray("samples").length(),actual.getJSONArray("samples").length());JSONObject fm=WorldVerify.verify(forecast),am=WorldVerify.verify(actual);
        var result=new JSONObject().put("passed",coverage>=.95&&maxPosition<=.01&&maxAngle<=.5).put("scope",SCOPE).put("matchedSamples",matched).put("sampleCoverage",coverage).put("maxPositionResidualM",maxPosition).put("maxAngleResidualDeg",maxAngle).put("effortResidualNs",am.getDouble("effortNs")-fm.getDouble("effortNs")).put("timeResidualS",am.getDouble("timeS")-fm.getDouble("timeS")).put("bodies",rows).put("forecastRunId",forecast.getString("id")).put("actualRunId",actual.getString("id")).put("forecastRunSha256",WorldFrames.hash(forecast)).put("actualRunSha256",WorldFrames.hash(actual)).put("sourceFrameSha256",s.getString("sourceFrameSha256")).put("selectedCandidate",c.getString("id")).put("decisionSha256",s.getString("decisionSha256"));
        if(s.has("comparison")&&!s.getJSONObject("comparison").similar(result))throw new IllegalStateException("Existing actual comparison changed");s.put("comparison",result);save();return WorldFrames.copy(result);
    }
    synchronized JSONObject resume(String id)throws Exception{var s=study(id);if(!Set.of("interrupted","queued").contains(s.getString("status"))||!active.isEmpty()||s.getInt("resumes")>=2)throw new IllegalStateException("Explicit counterfactual resume budget/ownership refused");compatible(s);current(s.getJSONObject("binding"));for(Object raw:s.getJSONArray("candidates")){var c=(JSONObject)raw;if(Set.of("running","interrupted","cancelled").contains(c.getString("status"))){c.put("status","queued").put("reason","Unfinished virtual attempt retained; explicit fresh trial from the same frozen frame");for(String key:List.of("runId","runSha256","metrics","predictedFrame"))c.remove(key);}}s.put("status","queued").put("resumes",s.getInt("resumes")+1);save();return view(s);}
    synchronized void cancel(String id)throws Exception{var s=study(id);if(s.getString("status").equals("running")&&owner!=null&&!activeRun.isEmpty())owner.cancel(activeRun);s.put("status","cancelled").put("reason","Stopped; no candidate may actuate the live world");save();}
    @Override public void close(){synchronized(this){closed=true;if(owner!=null&&!activeRun.isEmpty())try{owner.cancel(activeRun);}catch(Exception e){System.err.println(e.getMessage());}}worker.shutdownNow();try{worker.awaitTermination(3,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
}
