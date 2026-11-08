package fraymus.workspace;

import org.json.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/** Durable, bounded native procedures for the existing Java goal runner. */
final class WorldProcedure079 implements AutoCloseable {
 private final String costClock089=UUID.randomUUID().toString();private final String selector089=TaskCost089.selector(System.getenv("FRAYNIX_WORKSPACE_TEST"),System.getenv("FRAYNIX_WORKSPACE_REFERENCE089"));
 private final Path root;private final WorldLedger worlds;private final ProofMemo078 proofs;private final CalibrationScan081 calibrationScan081;private final NativeEnvelope091 envelopes091;
 private final Map<String,JSONObject> jobs=new LinkedHashMap<>(),published=new LinkedHashMap<>(),warm=new HashMap<>();
 private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"world-procedure-079");t.setDaemon(true);return t;});
 private final Set<String> stops=ConcurrentHashMap.newKeySet();private volatile boolean closed;private volatile String active="";private volatile WorldNativeStore owner;private volatile String nativeId="",activePhase="";private volatile long stageStartTicks;private final Map<String,JSONObject> lastActual=new ConcurrentHashMap<>();private long began;private double prior;
 WorldProcedure079(Path project,WorldLedger worlds)throws Exception {this.worlds=worlds;root=project.resolve("world-procedures-079");proofs=new ProofMemo078(root);calibrationScan081=new CalibrationScan081(root);envelopes091=new NativeEnvelope091(root);if(Files.exists(root))try(var dirs=Files.list(root)){for(Path d:dirs.filter(Files::isDirectory).toList())if(Files.exists(d.resolve("goal.json"))){var j=AgentFiles042.loadChecked(d.resolve("goal.json"));if(!d.getFileName().toString().equals(j.getString("id")))throw new IllegalStateException("Procedure identity changed");jobs.put(j.getString("id"),j);if(Set.of("running","interrupted").contains(j.getString("status"))){j.put("status","interrupted").put("reason","Process ended; explicit bounded same-run resume required");try{reconcileInterrupted(j);}catch(Exception e){j.put("recoveryError",String.valueOf(e.getMessage())).put("reason","Original state cannot be reconciled: "+e.getMessage());}save(j);}else published.put(j.getString("id"),WorldFrames.copy(j));}}}
 static String code(){var x=new JSONObject().put("native",WorldNativeCheckpoint.identity());for(Class<?> c:new Class<?>[]{WorldProcedure079.class,Procedure079.class,Waypoints079.class,Procedure078.class,Procedure077.class,Waypoints077.class,ProofMemo078.class,TaskCost080.class,TaskCost084.class,TaskCost085.class,TaskCost089.class,TaskCost090.class,NativeEnvelope091.class,RecoveryForecast093.class,CalibrationScan081.class,GoalRunner.class})try(var in=c.getResourceAsStream(c.getSimpleName()+".class")){x.put(c.getSimpleName(),MapDefinition.sha(in.readAllBytes()));}catch(Exception e){throw new IllegalStateException(e);}return WorldFrames.hash(x);}
 private void reconcileInterrupted(JSONObject j)throws Exception {
  compatible(j);
  for(Object raw:j.getJSONObject("arm").getJSONArray("attempts")){
   var a=(JSONObject)raw;if(!a.getString("status").equals("running"))continue;
   try(var store=new WorldNativeStore(attemptDir(j,a.getString("id")))){
    if(!store.hasRun(a.getString("id")))throw new IllegalStateException("Owned native state is missing; explicit world review required");
    var run=store.run(a.getString("id"));if(run.has("checkpointError"))throw new IllegalStateException(run.getString("checkpointError"));
    if(!run.getString("goalId").equals(j.getString("id"))||!WorldFrames.hash(run.getJSONObject("definition")).equals(a.getString("sourceSha256"))||!WorldFrames.hash(run.getJSONObject("plan")).equals(a.getString("planSha256")))throw new IllegalStateException("Interrupted native source or plan changed");
    settle(j,a,run,true);
   }
  }
 }
 private Path dir(String id){UUID.fromString(id);return root.resolve(id);}
 private Path attemptDir(JSONObject j,String id){UUID.fromString(id);return dir(j.getString("id")).resolve("attempts").resolve(id);}
 private synchronized void save(JSONObject j)throws Exception {if(j.getString("id").equals(active)&&j.getString("status").equals("running"))j.put("activeMillis",prior+(System.nanoTime()-began)/1e6);j.put("updatedAt",Instant.now().toString());AgentFiles042.saveChecked(dir(j.getString("id")).resolve("goal.json"),j);published.put(j.getString("id"),WorldFrames.copy(j));}
 synchronized boolean busy(){return !active.isEmpty();}
 synchronized JSONObject read(String id){var j=published.get(id);if(j==null)throw new IllegalArgumentException("Unknown world procedure");return WorldFrames.copy(j);}
 synchronized boolean has(String id){return published.containsKey(id);}
 private void compatible(JSONObject j){if(!j.getString("code").equals(code()))throw new IllegalStateException("Procedure implementation changed; retain originals and start a reviewed goal");}
 synchronized JSONObject start(String id,JSONObject binding,JSONObject args)throws Exception {
  if(jobs.containsKey(id)){var j=read(id);if(!j.getJSONObject("binding").similar(binding)||!j.getJSONObject("args").similar(args))throw new IllegalStateException("Idempotent procedure input changed");return j;}
  if(closed||busy()||jobs.size()>=64)throw new IllegalStateException("One bounded world procedure at a time; history limit64");
  var obs=worlds.observe(binding.getString("worldId"));if(!WorldLedger.binding(worlds.read(binding.getString("worldId"))).similar(binding)||obs.has("pending"))throw new IllegalStateException("Stale or owned world");
  var frame=obs.getJSONObject("frame");var spec=Procedure079.spec(frame,args);var plan=worlds.preview(new JSONObject(binding.toString()).put("targets",args.getJSONArray("targets")).put("forceLimitN",spec.getDouble("forceLimitN")).put("torqueLimitNm",spec.getDouble("torqueLimitNm")));
  if(!plan.getBoolean("passed"))throw new IllegalArgumentException(plan.getString("reason"));String mode=args.optString("memoryMode079","adaptive");if(!mode.equals("calibrated"))Procedure079.choose(spec,mode,new JSONArray());
  var j=new JSONObject().put("id",id).put("code",code()).put("binding",binding).put("args",WorldFrames.copy(args)).put("initialFrame",frame).put("spec",spec).put("leasePlan",plan).put("selector089",selector089).put("mode",mode).put("status","queued").put("reason","Queued").put("resumes",0).put("activeMillis",0d).put("createdAt",Instant.now().toString()).put("arm",new JSONObject().put("method",mode).put("success",false).put("attempts",new JSONArray()).put("programs",new JSONObject()).put("chargedTicks",0).put("observedTicks",0));jobs.put(id,j);save(j);launch(j);return read(id);
 }
 private synchronized void launch(JSONObject j)throws Exception {active=j.getString("id");prior=j.getDouble("activeMillis");began=System.nanoTime();stops.remove(active);j.put("status","running");save(j);worker.submit(()->execute(j));}
 synchronized JSONObject resume(String id)throws Exception {var j=jobs.get(id);if(j==null||closed||busy()||!j.getString("status").equals("interrupted")||j.getInt("resumes")>=2||j.getDouble("activeMillis")>=1_200_000)throw new IllegalStateException("Explicit interrupted-procedure resume only, at most twice");compatible(j);if(j.has("recoveryError"))throw new IllegalStateException(j.getString("recoveryError"));current(j);j.put("resumes",j.getInt("resumes")+1);launch(j);return read(id);}
 synchronized void stop(String id)throws Exception {var j=jobs.get(id);if(j==null)return;stops.add(id);if(!id.equals(active)&&!Set.of("ready","cancelled").contains(j.getString("status"))){j.put("status","cancelled").put("reason","Stopped; no successful world commit");save(j);}if(owner!=null&&id.equals(active)&&!nativeId.isEmpty())owner.cancel(nativeId);}
 private void guard(JSONObject j)throws InterruptedException {if(closed||stops.contains(j.getString("id")))throw new InterruptedException("Procedure stopped or Java ended");}
 private void current(JSONObject j)throws Exception {var w=worlds.read(j.getJSONObject("binding").getString("worldId"));if(!WorldLedger.binding(w).similar(j.getJSONObject("binding")))throw new IllegalStateException("World revision changed before execution");if(w.has("pending")&&!w.getJSONObject("pending").getString("goalId").equals(j.getString("id")))throw new IllegalStateException("Foreign world owner");}
 private JSONObject raw(JSONObject j,JSONObject a)throws Exception {return envelopes091.read(attemptDir(j,a.getString("id")).resolve("original.json"),j.getString("id"),a.getString("id"),a.getString("sha256"));}
 private JSONObject attempt(JSONObject j,String id){for(Object raw:j.getJSONObject("arm").getJSONArray("attempts"))if(((JSONObject)raw).getString("id").equals(id))return(JSONObject)raw;throw new IllegalStateException("Unowned native attempt");}
 private void settle(JSONObject j,JSONObject a,JSONObject run,boolean uncertain)throws Exception {int n=run.has("final")?run.getJSONObject("final").getInt("tick"):run.getJSONArray("samples").isEmpty()?0:run.getJSONArray("samples").getJSONObject(run.getJSONArray("samples").length()-1).getInt("tick");boolean tail=uncertain||a.optBoolean("unknownTail");a.put("settledAt",Instant.now().toString()).put("status",run.getString("status")).put("observedTicks",n).put("chargedTicks",tail?WorldPhysics.MAX_TICKS:n).put("unknownTail",tail).put("sha256",WorldFrames.hash(run));if(run.has("final"))a.put("metrics",WorldVerify.verify(run));AgentFiles042.saveChecked(attemptDir(j,a.getString("id")).resolve("original.json"),run);ProcedureStudy078.recomputeCosts(j.getJSONObject("arm"));save(j);}
 private JSONObject trial(JSONObject j,String phase,String family,int step,JSONObject frame,JSONObject plan,int remaining)throws Exception {
  var arm=j.getJSONObject("arm");String stage=phase+"/"+step;JSONObject a=null;for(Object raw:arm.getJSONArray("attempts")){var t=(JSONObject)raw;if(t.getString("family").equals(family)&&t.getString("stage").equals(stage))a=t;}
  if(a!=null&&(!a.getString("sourceSha256").equals(WorldFrames.hash(WorldFrames.scene(frame)))||!a.getString("planSha256").equals(WorldFrames.hash(plan))))throw new IllegalStateException("Existing stage source or plan changed");
  if(a!=null&&Set.of("completed","failed","cancelled").contains(a.getString("status")))return a;
  guard(j);current(j);save(j);
  if(a==null&&!Procedure078.admitted(arm.getInt("chargedTicks"),remaining,j.getDouble("activeMillis")))throw new IllegalStateException("Remaining full procedure exceeds tick/time budget");
  if(a==null){String id=UUID.randomUUID().toString();a=new JSONObject().put("id",id).put("family",family).put("stage",stage).put("status","running").put("plannedAt",Instant.now().toString()).put("sourceSha256",WorldFrames.hash(WorldFrames.scene(frame))).put("planSha256",WorldFrames.hash(plan)).put("admissionSpent",arm.getInt("chargedTicks")).put("admissionRemaining",remaining).put("wallMillis",0d);if(phase.equals("actual")){var rs=arm.getJSONObject("programs").getJSONObject(family).optJSONObject("reforecasts093",new JSONObject());if(rs.has(""+step))RecoveryForecast093.before(attempt(j,rs.getJSONObject(""+step).getString("forecastId")),a);}arm.getJSONArray("attempts").put(a);save(j);}
  String id=a.getString("id");double old=a.getDouble("wallMillis");long at=System.nanoTime();
  try(var store=new WorldNativeStore(attemptDir(j,id))){owner=store;nativeId=id;activePhase=phase;stageStartTicks=frame.getLong("elapsedTicks");
   if(store.hasRun(id)){String state=store.status(id);if(state.equals("interrupted")||state.equals("paused")){if(j.getInt("resumes")==0)throw new IllegalStateException("Explicit same-run resume required");a.put("unknownTail",true);store.resume(id,WorldFrames.scene(frame),false);}else if(!state.equals("completed"))throw new IllegalStateException("Native attempt is not resumable: "+state);}
   else store.start(id,j.getString("id"),WorldFrames.scene(frame),plan,false,true);
   long saved=System.nanoTime();while(store.status(id).equals("running")){if(closed){store.close();break;}if(stops.contains(j.getString("id"))||old+(System.nanoTime()-at)/1e6>Procedure078.ATTEMPT_MS)store.cancel(id);Thread.sleep(15);if(System.nanoTime()-saved>2_000_000_000L){a.put("wallMillis",old+(System.nanoTime()-at)/1e6);save(j);saved=System.nanoTime();}}
   var run=store.run(id);a.put("wallMillis",old+(System.nanoTime()-at)/1e6);settle(j,a,run,run.getString("status").equals("interrupted"));if(phase.equals("actual"))lastActual.put(j.getString("id"),store.snapshot().put("elapsedTicksBeforeStage",frame.getLong("elapsedTicks")));
  }finally{owner=null;nativeId="";}guard(j);if(a.getString("status").equals("interrupted"))throw new InterruptedException("Native checkpoint interrupted");return a;
 }
 private JSONObject program(JSONObject j,String family)throws Exception {
  var arm=j.getJSONObject("arm");var spec=j.getJSONObject("spec");var programs=arm.getJSONObject("programs");var p=programs.optJSONObject(family);int steps=Procedure078.stages(family);
  if(p!=null&&Set.of("completed","failed","refused").contains(p.getString("status")))return p;
  if(p==null){p=new JSONObject().put("family",family).put("status","running").put("success",false).put("forecast",new JSONArray()).put("actual",new JSONArray()).put("comparisons",new JSONArray()).put("waypoints",new JSONObject());programs.put(family,p);save(j);if(!Procedure078.admitted(arm.getInt("chargedTicks"),2*steps,j.getDouble("activeMillis")))return p.put("status","refused").put("reason","Insufficient full forecast/actual reserve");}
  for(String phase:List.of("forecast","actual")){
   var frame=WorldFrames.copy(j.getJSONObject("initialFrame"));
   if(phase.equals("actual")){current(j);worlds.reserve(j.getJSONObject("binding"),j.getString("id"),j.getString("id"),j.getJSONObject("leasePlan"));}
   for(int i=0;i<steps;i++){
    guard(j);j.put("reason",family+" / "+phase+" stage "+(i+1)+" of "+steps);save(j);JSONObject choice;
    try{choice=Waypoints079.choose(spec,family,i,j.getJSONObject("initialFrame"),frame);if(!choice.getJSONObject("plan").getBoolean("passed"))throw new IllegalArgumentException(choice.getJSONObject("plan").getString("reason"));}
    catch(IllegalArgumentException e){if(phase.equals("actual"))throw new IllegalStateException("Actual stage refused; owned world requires review",e);return p.put("status","failed").put("reason",e.getMessage());}
    p.getJSONObject("waypoints").put(phase+"/"+i,choice);var prediction=phase.equals("actual")?actualForecast093(j,p,family,i,frame,choice,true,null,null):null;var a=trial(j,phase,family,i,frame,choice.getJSONObject("plan"),steps-i+(phase.equals("forecast")?steps:0));p.getJSONArray(phase).put(i,a.getString("id"));save(j);
    if(!a.getString("status").equals("completed")||!a.optJSONObject("metrics",new JSONObject()).optBoolean("passed")){if(phase.equals("actual"))throw new IllegalStateException("Actual motion failed; no reset, fallback or commit");return p.put("status","failed").put("reason","Original native forecast did not pass");}
    var run=raw(j,a);if(!Procedure079.protectedTrace(j.getJSONObject("initialFrame"),spec,run))throw new IllegalStateException("Whole-task protected body drift");frame=WorldFrames.after(frame,run);
    if(phase.equals("actual")){var comparison=Procedure079.comparison(prediction,run);p.getJSONArray("comparisons").put(i,comparison);if(!comparison.getBoolean("passed"))throw new IllegalStateException("Forecast/actual mismatch; no commit");}
   }
   p.put(phase+"Frame",frame).put(phase+"Task",Procedure079.task(j.getJSONObject("initialFrame"),spec,frame));if(!p.getJSONObject(phase+"Task").getBoolean("passed")){if(phase.equals("actual"))throw new IllegalStateException("Final actual task failed");return p.put("status","failed").put("reason","Whole forecast task failed");}
  }
  return p.put("status","completed").put("success",true);
 }
 private JSONObject actualForecast093(JSONObject j,JSONObject p,String family,int step,JSONObject frame,JSONObject choice,boolean execute,JSONObject actual,Set<String> bound)throws Exception {
  var originalAttempt=attempt(j,p.getJSONArray("forecast").getString(step));var original=raw(j,originalAttempt);var plan=choice.getJSONObject("plan");
  var replacements=p.optJSONObject("reforecasts093",new JSONObject());String key=""+step;
  if(RecoveryForecast093.matches(original,frame,plan)){
   if(replacements.has(key))throw new IllegalStateException("Unnecessary or substituted replacement forecast");return original;
  }
  if(step<1||j.getInt("resumes")<1)throw new IllegalStateException("Fresh stage source/plan identity differs without recovered actual provenance");
  JSONObject anchor=null;
  for(int i=0;i<step;i++){var a=attempt(j,p.getJSONArray("actual").getString(i));var run=raw(j,a);
   if(a.optBoolean("unknownTail")&&run.optInt("resumes")>0&&run.getString("status").equals("completed")&&WorldVerify.verify(run).getBoolean("passed"))anchor=run;
  }
  if(anchor==null)throw new IllegalStateException("No verified resumed actual ancestor for replacement forecast");
  var previous=attempt(j,p.getJSONArray("actual").getString(step-1));var previousRun=raw(j,previous);
  if(!WorldFrames.after(previousRun.getJSONObject("definition").getJSONObject("worldFrame"),previousRun).similar(frame))throw new IllegalStateException("Recovered frame differs from the previous measured outcome");
  var expected=RecoveryForecast093.binding(step,original,previousRun,anchor,frame,plan);var saved=replacements.optJSONObject(key);
  if(saved!=null)RecoveryForecast093.bindingMatches(saved,expected);
  else if(!execute)throw new IllegalStateException("Missing replacement forecast binding");
  JSONObject a;
  if(execute){
   boolean exists=false;for(Object raw:j.getJSONObject("arm").getJSONArray("attempts")){var t=(JSONObject)raw;if(t.getString("family").equals(family)&&t.getString("stage").equals("reforecast/"+step))exists=true;}
   if(!exists)RecoveryForecast093.admit(j.getJSONObject("arm").getInt("chargedTicks"),Procedure078.stages(family),step,j.getDouble("activeMillis"));
   if(saved==null){saved=expected;replacements.put(key,saved);p.put("reforecasts093",replacements);}
   var waypoints=p.getJSONObject("waypoints");String stage="reforecast/"+step;
   if(waypoints.has(stage)&&!waypoints.getJSONObject(stage).similar(choice))throw new IllegalStateException("Replacement waypoint changed");
   waypoints.put(stage,WorldFrames.copy(choice));j.put("reason","Verify a fresh forecast from recovered state before actual stage "+(step+1));save(j);
   a=trial(j,"reforecast",family,step,frame,plan,RecoveryForecast093.reserve(Procedure078.stages(family),step));
   if(saved.has("forecastId")&&!saved.getString("forecastId").equals(a.getString("id")))throw new IllegalStateException("Replacement identity changed");
   saved.put("forecastId",a.getString("id"));save(j);
  }else a=attempt(j,saved.getString("forecastId"));
  var forecast=raw(j,a);
  if(!a.getString("family").equals(family)||!a.getString("stage").equals("reforecast/"+step)
   ||a.getInt("admissionRemaining")!=RecoveryForecast093.reserve(Procedure078.stages(family),step)
   ||!p.getJSONObject("waypoints").getJSONObject("reforecast/"+step).similar(choice)
   ||!a.getString("sourceSha256").equals(WorldFrames.hash(WorldFrames.scene(frame)))||!a.getString("planSha256").equals(WorldFrames.hash(plan))
   ||!RecoveryForecast093.matches(forecast,frame,plan)||!forecast.getString("status").equals("completed")
   ||!WorldVerify.verify(forecast).getBoolean("passed")||!Procedure079.protectedTrace(j.getJSONObject("initialFrame"),j.getJSONObject("spec"),forecast))
    throw new IllegalStateException("Fresh recovered forecast failed its original source or outcome checks");
  RecoveryForecast093.before(previous,a);
  var attempts=j.getJSONObject("arm").getJSONArray("attempts");int pi=-1,fi=-1,ai=-1;
  for(int i=0;i<attempts.length();i++){String id=attempts.getJSONObject(i).getString("id");if(id.equals(previous.getString("id")))pi=i;if(id.equals(a.getString("id")))fi=i;if(actual!=null&&id.equals(actual.getString("id")))ai=i;}
  if(pi<0||fi<=pi||actual!=null&&ai<=fi)throw new IllegalStateException("Replacement forecast chronology changed");
  if(actual!=null)RecoveryForecast093.before(a,actual);if(bound!=null)bound.add(a.getString("id"));return forecast;
 }
 private void execute(JSONObject j){try{compatible(j);current(j);if(!j.has("decision")){long at=System.nanoTime();var cs=new JSONArray();var receipts=j.getString("mode").equals("simple")?new JSONArray():bank();for(Object raw:receipts){var b=(JSONObject)raw;try{var c=Procedure079.estimate(b,j.getJSONObject("spec"),isWarm(b));if(c.getBoolean("warm"))c.put("warmProof",WorldFrames.copy(warm.get(b.getString("id"))));cs.put(c);}catch(IllegalArgumentException incompatible){}}var calibration=j.getString("mode").equals("calibrated")?calibration080():new JSONObject();j.put("calibration080",calibration);j.put("bankSnapshot",receipts);j.put("freshnessEvidence090",recentCosts090(j,cs,calibration));var decision=decision080(j,cs,calibration);j.put("decision",decision);if(decision.getBoolean("useMemory")){var b=find(receipts,decision.getJSONObject("candidate").getString("id"));j.put("lookupProof",check(b));}j.put("lookupMillis",(System.nanoTime()-at)/1e6);j.getJSONObject("arm").put("candidateOrder",new JSONArray(Procedure079.order(decision)));save(j);}
   var arm=j.getJSONObject("arm");for(String family:Procedure079.order(j.getJSONObject("decision"))){var p=program(j,family);save(j);if(p.getBoolean("success")){arm.put("success",true).put("selected",family);break;}}
   if(!arm.getBoolean("success"))throw new IllegalStateException("No verified procedure within the original budget");long at=System.nanoTime();audit(j);j.put("auditMillis",(System.nanoTime()-at)/1e6);save(j);j.put("status","ready").put("reason","Fresh native sequence verified; awaiting actual report and atomic world commit");save(j);
  }catch(Throwable e){j.put("status",stops.contains(j.getString("id"))?"cancelled":"interrupted").put("reason",e.getClass().getSimpleName()+": "+String.valueOf(e.getMessage()));try{save(j);}catch(Exception failure){System.err.println("Procedure checkpoint failed: "+failure.getMessage());}}
  finally{synchronized(this){active="";}owner=null;nativeId="";}}
 private static JSONObject find(JSONArray xs,String id){for(Object x:xs)if(((JSONObject)x).getString("id").equals(id))return(JSONObject)x;throw new IllegalStateException("Unknown retained procedure");}
 synchronized JSONArray bank()throws Exception {Path p=root.resolve("bank.json");return Files.exists(p)?AgentFiles042.loadChecked(p).getJSONArray("entries"):new JSONArray();}
 private List<Path> evidence(JSONObject b)throws Exception {var j=read(b.getString("id"));var paths=new ArrayList<Path>();paths.add(dir(j.getString("id")).resolve("memory.json"));paths.add(dir(j.getString("id")).resolve("goal.json"));for(Object raw:j.getJSONObject("arm").getJSONArray("attempts")){var a=(JSONObject)raw;if(a.has("sha256"))paths.add(attemptDir(j,a.getString("id")).resolve("original.json"));}return paths;}
 private JSONObject binding(JSONObject b){return new JSONObject().put("code",code()).put("receipt",b);}
 private boolean isWarm(JSONObject b)throws Exception {var prior=warm.get(b.getString("id"));return prior!=null&&prior.getJSONObject("binding").similar(binding(b))&&prior.getJSONArray("evidence").similar(proofs.snapshot(evidence(b)));}
 private JSONObject check(JSONObject b)throws Exception {var p=proofs.check(binding(b),evidence(b),true,()->{var actual=AgentFiles042.loadChecked(dir(b.getString("id")).resolve("memory.json"));if(!actual.similar(b))throw new IllegalStateException("Retained memory changed");var j=AgentFiles042.loadChecked(dir(b.getString("id")).resolve("goal.json"));if(!j.similar(read(b.getString("id"))))throw new IllegalStateException("Retained job changed on disk");if(!j.getString("code").equals(b.getString("code"))||!WorldFrames.hash(j).equals(b.getString("jobSha256")))throw new IllegalStateException("Retained procedure source changed");var rev=worlds.committed(j.getJSONObject("binding").getString("worldId"),j.getString("id"));if(!rev.getString("hash").equals(b.getString("commitHash")))throw new IllegalStateException("Missing verified world commit");audit(j);});if(p.getString("mode").equals("full")){warm.put(b.getString("id"),WorldFrames.copy(p));AgentFiles042.saveChecked(root.resolve("proof-history").resolve(WorldFrames.hash(p)+".json"),p);}return p;}
 synchronized JSONObject retain(String id,JSONObject commit)throws Exception {var j=read(id);compatible(j);if(!j.getString("status").equals("ready")||!commit.getString("goalId").equals(id))throw new IllegalStateException("Only a verified committed procedure can be remembered");var entries=bank();for(Object x:entries)if(((JSONObject)x).getString("id").equals(id))return(JSONObject)x;if(entries.length()>=32)return new JSONObject().put("retained",false).put("reason","Memory capacity32 reached; originals remain");String family=j.getJSONObject("arm").getString("selected");double avoided=0;for(Object raw:j.getJSONObject("arm").getJSONArray("attempts")){var a=(JSONObject)raw;if(!a.getString("family").equals(family))avoided+=a.getDouble("wallMillis");}
  var b=new JSONObject().put("id",id).put("code",code()).put("spec",j.getJSONObject("spec")).put("family",family).put("activeMillis",j.getDouble("activeMillis")).put("auditMillis",j.getDouble("auditMillis")).put("avoidedMillis",avoided).put("commitHash",commit.getString("hash")).put("jobSha256",WorldFrames.hash(j)).put("retainedAt",Instant.now().toString());AgentFiles042.saveChecked(dir(id).resolve("memory.json"),b);check(b);entries.put(b);AgentFiles042.saveChecked(root.resolve("bank.json"),new JSONObject().put("entries",entries));return b;
 }
 JSONObject audit(String id)throws Exception{return audit(read(id));}
 private void selection(JSONObject j)throws Exception {
  var decision=j.getJSONObject("decision");var receipts=j.getJSONArray("bankSnapshot");var candidates=decision.getJSONArray("candidates");var rebuilt=new JSONArray();
  if(j.getString("mode").equals("simple")&&(!receipts.isEmpty()||!candidates.isEmpty()||j.has("lookupProof")))throw new IllegalStateException("Simple control consulted memory");
  for(Object raw:receipts){var b=(JSONObject)raw;var disk=AgentFiles042.loadChecked(dir(b.getString("id")).resolve("memory.json"));if(!disk.similar(b)||!b.getString("code").equals(code()))throw new IllegalStateException("Bank receipt source changed");JSONObject candidate=null;for(Object x:candidates)if(((JSONObject)x).getString("id").equals(b.getString("id")))candidate=(JSONObject)x;
   try{var expected=Procedure079.estimate(b,j.getJSONObject("spec"),candidate!=null&&candidate.getBoolean("warm"));if(candidate!=null&&candidate.getBoolean("warm")){var proof=candidate.getJSONObject("warmProof");var stored=AgentFiles042.loadChecked(root.resolve("proof-history").resolve(WorldFrames.hash(proof)+".json"));if(!stored.similar(proof)||!proof.getString("mode").equals("full")||!proof.getJSONObject("binding").similar(binding(b))||!proof.getString("key").equals(WorldFrames.hash(binding(b))))throw new IllegalStateException("Warm selection lacks owned full proof");expected.put("warmProof",proof);}rebuilt.put(expected);}catch(IllegalArgumentException incompatible){if(candidate!=null)throw new IllegalStateException("Incompatible candidate admitted",incompatible);}
  }
  checkFreshness090(j);
  if(!rebuilt.similar(candidates)||!decision080(j,rebuilt,checkedModel080(j)).similar(decision)||!new JSONArray(Procedure079.order(decision)).similar(j.getJSONObject("arm").getJSONArray("candidateOrder")))throw new IllegalStateException("Selection rule or measured estimate changed");
  if(decision.getBoolean("useMemory")){var b=find(receipts,decision.getJSONObject("candidate").getString("id"));var proof=j.getJSONObject("lookupProof");if(!proof.getJSONObject("binding").similar(binding(b))||!proof.getJSONArray("evidence").similar(proofs.snapshot(evidence(b))))throw new IllegalStateException("Selected memory no longer has original proof");if(proof.getString("mode").equals("hit")){var priorProof=AgentFiles042.loadChecked(root.resolve("proof-history").resolve(proof.getString("originReceiptSha256")+".json"));if(!priorProof.getJSONArray("evidence").similar(proof.getJSONArray("evidence"))||!priorProof.getJSONObject("binding").similar(proof.getJSONObject("binding"))||!priorProof.getString("mode").equals("full"))throw new IllegalStateException("Unbacked proof hit");}else if(!proof.getString("mode").equals("full"))throw new IllegalStateException("Unknown proof mode");}
  else if(j.has("lookupProof"))throw new IllegalStateException("Skipped lookup claimed a proof");
 }
 JSONObject audit(JSONObject j)throws Exception {
  compatible(j);WorldFrames.verify(j.getJSONObject("initialFrame"));var initial=j.getJSONObject("initialFrame");var spec=j.getJSONObject("spec");if(!Procedure079.spec(initial,j.getJSONObject("args")).similar(spec))throw new IllegalStateException("Frozen current-world request changed");selection(j);
  var arm=j.getJSONObject("arm");var order=Procedure079.order(j.getJSONObject("decision"));int ticks=0,observed=0,previous=-1;var seen=new HashSet<String>();var stages=new HashSet<String>();
  for(Object x:arm.getJSONArray("attempts")){var a=(JSONObject)x;int index=order.indexOf(a.getString("family"));if(index<previous||index<0||!seen.add(a.getString("id")))throw new IllegalStateException("Reordered or reused native identity");previous=index;String[] parts=a.getString("stage").split("/");int step=parts.length==2?Integer.parseInt(parts[1]):-1;int count=Procedure078.stages(a.getString("family"));if(!stages.add(a.getString("family")+"/"+a.getString("stage"))||step<0||step>=count||!Set.of("forecast","actual","reforecast").contains(parts[0])||parts[0].equals("reforecast")&&step==0)throw new IllegalStateException("Duplicate or invalid native stage");int reserve=parts[0].equals("reforecast")?RecoveryForecast093.reserve(count,step):count-step+(parts[0].equals("forecast")?count:0);if(a.getInt("admissionRemaining")!=reserve)throw new IllegalStateException("Native remaining-work reserve changed");var r=raw(j,a);int n=r.has("final")?r.getJSONObject("final").getInt("tick"):r.getJSONArray("samples").isEmpty()?0:r.getJSONArray("samples").getJSONObject(r.getJSONArray("samples").length()-1).getInt("tick");
   if(a.getInt("admissionSpent")!=ticks||!Procedure078.admitted(ticks,a.getInt("admissionRemaining"),0)||n!=a.getInt("observedTicks")||a.getInt("chargedTicks")!=(a.optBoolean("unknownTail")?14400:n)||n>14400||!Double.isFinite(a.getDouble("wallMillis"))||a.getDouble("wallMillis")<0||!a.getString("status").equals(r.getString("status"))||Instant.parse(r.getString("createdAt")).isBefore(Instant.parse(a.getString("plannedAt"))))throw new IllegalStateException("Native admission, cost or chronology changed");
   if(r.has("final")&&!WorldVerify.verify(r).similar(a.getJSONObject("metrics")))throw new IllegalStateException("Original native metrics changed");ticks+=a.getInt("chargedTicks");observed+=n;
  }
  if(ticks!=arm.getInt("chargedTicks")||observed!=arm.getInt("observedTicks")||ticks>115200)throw new IllegalStateException("Goal work exceeds bound");boolean success=false,missing=false;String selected="";JSONObject finalFrame=null;var bound=new HashSet<String>();
  for(String family:order){var p=arm.getJSONObject("programs").optJSONObject(family);if(p==null){missing=true;continue;}if(missing||success)throw new IllegalStateException("Procedure skipped or continued after success");int steps=Procedure078.stages(family);boolean[] complete={false,false};int phaseIndex=0;
   for(String phase:List.of("forecast","actual")){var frame=WorldFrames.copy(initial);var ids=p.getJSONArray(phase);if(ids.length()>steps||phase.equals("actual")&&!complete[0]&&!ids.isEmpty())throw new IllegalStateException("Actual before complete forecast");boolean valid=true;int executed=0;
    for(int i=0;i<steps;i++){String key=phase+"/"+i;JSONObject choice;
     try{choice=Waypoints079.choose(spec,family,i,initial,frame);if(p.getJSONObject("waypoints").has(key)&&!choice.similar(p.getJSONObject("waypoints").getJSONObject(key)))throw new IllegalStateException("Waypoint search changed");}catch(IllegalArgumentException rejected){for(Object x:arm.getJSONArray("attempts")){var a=(JSONObject)x;if(a.getString("family").equals(family)&&a.getString("stage").equals(key))throw rejected;}valid=false;break;}
     for(Object x:arm.getJSONArray("attempts")){var a=(JSONObject)x;if(!a.getString("family").equals(family)||!a.getString("stage").equals(key))continue;bound.add(a.getString("id"));var r=raw(j,a);if(a.getInt("admissionRemaining")!=steps-i+(phase.equals("forecast")?steps:0)||!choice.getJSONObject("plan").getBoolean("passed")||!p.getJSONObject("waypoints").has(key)||!r.getJSONObject("definition").similar(WorldFrames.scene(frame))||!r.getJSONObject("plan").similar(choice.getJSONObject("plan"))||!a.getString("sourceSha256").equals(WorldFrames.hash(r.getJSONObject("definition")))||!a.getString("planSha256").equals(WorldFrames.hash(r.getJSONObject("plan"))))throw new IllegalStateException("Procedure or measured handoff substituted");}
     if(i>=ids.length())break;var a=attempt(j,ids.getString(i));if(!a.getString("stage").equals(key)||!a.getString("family").equals(family))throw new IllegalStateException("Wrong stage reference");if(!a.getString("status").equals("completed")||!a.getJSONObject("metrics").getBoolean("passed")){valid=false;break;}var r=raw(j,a);var prediction=phase.equals("actual")?actualForecast093(j,p,family,i,frame,choice,false,a,bound):null;valid=Procedure079.protectedTrace(initial,spec,r);if(!valid)break;frame=WorldFrames.after(frame,r);executed++;
     if(phase.equals("actual")){var c=Procedure079.comparison(prediction,r);if(!c.similar(p.getJSONArray("comparisons").getJSONObject(i)))throw new IllegalStateException("Forecast comparison changed");valid&=c.getBoolean("passed");}if(!valid)break;
    }
    var task=Procedure079.task(initial,spec,frame);complete[phaseIndex]=valid&&executed==steps&&task.getBoolean("passed");if(p.has(phase+"Frame")&&(!frame.similar(p.getJSONObject(phase+"Frame"))||!task.similar(p.getJSONObject(phase+"Task"))))throw new IllegalStateException("Whole-task frame changed");if(phase.equals("actual")&&complete[phaseIndex])finalFrame=frame;phaseIndex++;
   }
   var replacements=p.optJSONObject("reforecasts093",new JSONObject());for(String key:replacements.keySet()){var replacement=replacements.getJSONObject(key);int step=Integer.parseInt(key);if(step<1||step>=steps||!key.equals(""+step)||!bound.contains(replacement.getString("forecastId")))throw new IllegalStateException("Unbound replacement forecast");}
   boolean passed=complete[0]&&complete[1];if(p.getBoolean("success")!=passed||passed&&!p.getString("status").equals("completed"))throw new IllegalStateException("False procedure success");if(passed){success=true;selected=family;}
  }
  if(!success||!arm.getBoolean("success")||!arm.getString("selected").equals(selected)||bound.size()!=seen.size())throw new IllegalStateException("Unbound action or wrong selected outcome");return new JSONObject().put("passed",true).put("ticks",ticks).put("nativeStages",seen.size()).put("family",selected).put("finalFrame",finalFrame).put("positionM",Procedure079.task(initial,spec,finalFrame).getDouble("positionM"));
 }
 JSONObject snapshot(String id)throws Exception {var running=owner;if(id.equals(active)&&activePhase.equals("actual")&&running!=null){var view=running.snapshot();if(view.has("sample")){view.put("elapsedTicksBeforeStage",stageStartTicks);return view;}}var prior=lastActual.get(id);if(prior!=null)return WorldFrames.copy(prior);if(!has(id))return new JSONObject();var j=read(id);var attempts=j.getJSONObject("arm").getJSONArray("attempts");for(int i=attempts.length()-1;i>=0;i--){var a=attempts.getJSONObject(i);if(!a.getString("stage").startsWith("actual/")||!a.has("sha256"))continue;var r=raw(j,a);if(r.getJSONArray("samples").isEmpty())continue;var f=r.has("final")?r.getJSONObject("final"):r.getJSONArray("samples").getJSONObject(r.getJSONArray("samples").length()-1);var objects=WorldFrames.copy(r.getJSONObject("definition")).getJSONArray("objects");for(Object x:f.getJSONArray("bodies")){var b=(JSONObject)x;for(Object o:objects)if(((JSONObject)o).getString("id").equals(b.getString("id")))for(String k:List.of("position","rotation","velocity","angularVelocity"))((JSONObject)o).put(k,b.get(k));}var view=new JSONObject().put("id",r.getString("id")).put("status",r.getString("status")).put("sample",f).put("current",new JSONObject().put("objects",objects).put("grippingId",f.optJSONObject("controller",new JSONObject()).optString("grippingId"))).put("elapsedTicksBeforeStage",r.getJSONObject("definition").getJSONObject("worldFrame").getLong("elapsedTicks"));lastActual.put(id,view);return WorldFrames.copy(view);}return new JSONObject();}
 JSONObject last(String id)throws Exception {var j=read(id);var p=j.getJSONObject("arm").getJSONObject("programs").getJSONObject(j.getJSONObject("arm").getString("selected"));var xs=p.getJSONArray("actual");return raw(j,attempt(j,xs.getString(xs.length()-1)));}
 JSONObject summary(String id){var j=read(id);var x=new JSONObject();for(String k:List.of("id","status","reason","mode","activeMillis","lookupMillis","auditMillis","decision","lookupProof","resumes"))if(j.has(k))x.put(k,j.get(k));var recovery=new JSONObject().put("count",0).put("chargedTicks",0).put("wallMillis",0d);for(Object raw:j.getJSONObject("arm").getJSONArray("attempts")){var a=(JSONObject)raw;if(a.getString("stage").startsWith("reforecast/")){recovery.put("count",recovery.getInt("count")+1).put("chargedTicks",recovery.getInt("chargedTicks")+a.optInt("chargedTicks")).put("wallMillis",recovery.getDouble("wallMillis")+a.optDouble("wallMillis"));}}x.put("recoveryForecasts093",recovery);x.put("chargedTicks",j.getJSONObject("arm").getInt("chargedTicks")).put("nativeStages",j.getJSONObject("arm").getJSONArray("attempts").length());return x;}
 JSONObject export(String id)throws Exception {var j=read(id);var runs=new JSONArray();for(Object x:j.getJSONObject("arm").getJSONArray("attempts")){var a=(JSONObject)x;if(a.has("sha256"))runs.put(WorldReport.packed(raw(j,a)));}return new JSONObject().put("format",Procedure079.KIND).put("job",j).put("originals",runs).put("cost080",cost080(id));}
 JSONObject report(String id)throws Exception {
  var summary=summary(id);var r=WorldReport.build(last(id));r.put("title","Verified world procedure");var files=r.getJSONObject("files");var decision=summary.getJSONObject("decision");var candidate=decision.optJSONObject("candidate");String cost=candidate==null?"No compatible experience needed.":String.format(Locale.ROOT,"Estimated avoided work %.3f s; proof %.3f s; uncertainty margin %.3f s.",candidate.getDouble("avoidedMillis")/1000,candidate.getDouble("proofEstimateMillis")/1000,candidate.getDouble("uncertaintyMarginMillis")/1000);
  if(decision.has("cost080")){var e=decision.getJSONObject("cost080");cost=e.optBoolean("available")?String.format(Locale.ROOT,"Complete task estimate: simple %.3f s; memory %.3f s; saving %.3f s with %.3f s empirical margin. Includes checks and commit; not a statistical guarantee.",e.getDouble("simpleMillis")/1000,e.getDouble("memoryMillis")/1000,e.getDouble("savingMillis")/1000,e.getDouble("marginMillis")/1000):e.getString("reason")+". No complete-task saving is claimed.";}
  if(decision.optJSONObject("cost080",new JSONObject()).has("payback085")){var e=decision.getJSONObject("cost080");var p=e.getJSONObject("payback085");cost+=String.format(Locale.ROOT," Bank %d entries; measured range %d–%d; extra growth margin %.3f s. Calibration setup %.3f s. ",e.getInt("bankEntries"),e.getInt("calibrationBankMin"),e.getInt("calibrationBankMax"),e.getDouble("bankGrowthMarginMillis")/1000,p.getDouble("setupMillis")/1000)+(p.getBoolean("available")?"Conditional setup repayment: at least "+p.getDouble("tasks")+" tasks, excluding learning/launch and future checking. Not realized payback.":"No positive lower saving bound; payback is not established.");}

  if(decision.has("freshness090")){var f=decision.getJSONObject("freshness090");cost+=" Cost freshness: "+f.getString("status")+". "+f.getString("reason")+". Prior verified outcomes: "+f.getJSONArray("observations").length()+". ";for(Object raw:f.getJSONArray("observations")){var o=(JSONObject)raw;if(o.optBoolean("available"))cost+=String.format(Locale.ROOT,"Outcome %s: expected %.2f s, actual %.2f s, reserve %.2f s (%s). ",o.getString("id"),o.getDouble("predictedMillis")/1000,o.getDouble("actualMillis")/1000,o.getDouble("toleranceMillis")/1000,o.getString("direction"));}}
  if(decision.optJSONObject("cost080",new JSONObject()).has("pairingReason089")){var e=decision.getJSONObject("cost080");cost+=" "+e.getString("pairingReason089")+". ";if(e.has("pairedRangeMillis"))cost+=String.format(Locale.ROOT,"Paired range %.3f s; order reserve %.3f s; prior unpaired margin %.3f s.",e.getDouble("pairedRangeMillis")/1000,e.getDouble("orderMarginMillis")/1000,e.getDouble("referenceMarginMillis")/1000);}
  String text=summary.toString(2).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");var recovery=summary.getJSONObject("recoveryForecasts093");String recoveryText=recovery.getInt("count")==0?"":String.format(Locale.ROOT,"<p id=\"recovery093\">Recovery forecasts: %d · %d extra charged ticks · %.3f s native work. Verified from recovered state before later actual motion; original forecasts preserved.</p>",recovery.getInt("count"),recovery.getInt("chargedTicks"),recovery.getDouble("wallMillis")/1000);String section=recoveryText+"<section id=\"procedure079-decision\"><h2>Memory decision from measured cost</h2><p>"+decision.getString("reason")+"</p><p>"+cost+"</p><p>"+summary.getInt("nativeStages")+" native stages · "+summary.getInt("chargedTicks")+" charged ticks. Every failed forecast is included.</p><p>These six charts show the final actual stage. The complete procedure export preserves all stages, measured handoffs and original proofs.</p><details><summary>Decision and cost evidence</summary><pre>"+text+"</pre></details></section>";
  files.put("index.html",files.getString("index.html").replace("<h1 id=\"pose-result\">",section+"<h1 id=\"pose-result\">"));files.put("procedure.json",summary.toString());return r;
 }

 private JSONObject decision080(JSONObject j,JSONArray candidates,JSONObject model){var d=TaskCost089.choose(j.getJSONObject("spec"),j.getString("mode"),candidates,j.getJSONObject("args").optJSONObject("workload080",new JSONObject().put("retain",false).put("skill",false)),model,j.getJSONObject("args").optString("procedureReceipt085",""),j.getJSONArray("bankSnapshot").length(),j.getString("selector089"));return TaskCost090.apply(j.getJSONObject("spec"),d,observations090(j));}
 synchronized void validateChoice085(JSONObject binding,JSONObject args)throws Exception {String id=args.optString("procedureReceipt085","");TaskCost085.validateSelection(args.getString("memoryMode079"),id);if(id.isEmpty())return;var spec=Procedure079.spec(worlds.observe(binding.getString("worldId")).getJSONObject("frame"),args);var chosen=find(bank(),id);if(!chosen.getString("code").equals(code()))throw new IllegalArgumentException("Selected procedure implementation changed");var c=Procedure079.estimate(chosen,spec,false);if(c.getDouble("distance")>.10)throw new IllegalArgumentException("Selected procedure is too distant for this task");}

 private JSONObject checkedModel080(JSONObject j)throws Exception {var m=j.optJSONObject("calibration080",new JSONObject());if(!m.isEmpty()&&!loadCalibration080(m.getString("id")).similar(m))throw new IllegalStateException("Frozen cost calibration changed");return m;}
 private Path costPath080(String id){return dir(id).resolve("cost080.json");}
 synchronized JSONObject cost080(String id)throws Exception {return Files.exists(costPath080(id))?AgentFiles042.loadChecked(costPath080(id)):new JSONObject();}
 synchronized JSONObject recordCost080(JSONObject g,Long began)throws Exception {
  String id=g.getString("id");var old=cost080(id);if(!old.isEmpty())return old;var j=read(id);compatible(j);
  if(!g.getString("status").equals("completed")||!g.getJSONObject("poseView").getBoolean("passed")||!worlds.committed(j.getJSONObject("binding").getString("worldId"),id).similar(g.getJSONObject("worldCommit")))throw new IllegalStateException("Only completed, verified committed goals have complete cost");
  var decision=j.getJSONObject("decision");var c=decision.optJSONObject("candidate");boolean eligible=began!=null&&g.optInt("resumes")==0&&j.optInt("resumes")==0;
  long ended089=System.nanoTime();double total=began==null?0:(ended089-began)/1e6,nativeMs=0;for(Object raw:j.getJSONObject("arm").getJSONArray("attempts"))nativeMs+=((JSONObject)raw).getDouble("wallMillis");
  var data=new JSONObject().put("version",TaskCost080.VERSION).put("id",id).put("code",code()).put("completed",true).put("eligible",eligible)
   .put("reason",eligible?"Uninterrupted monotonic complete-task measurement":"Interrupted or missing monotonic clock; excluded from calibration")
   .put("totalMillis",total).put("nativeStageMillis",nativeMs).put("executionMillis",j.getDouble("activeMillis")).put("profile",TaskCost080.profile())
   .put("workload",TaskCost080.workload(g)).put("task",TaskCost080.task(j)).put("spec",j.getJSONObject("spec")).put("mode",j.getString("mode"))
   .put("usedMemory",decision.getBoolean("useMemory")).put("preferred",decision.getString("preferred")).put("receiptId",c==null?"":c.getString("id"))
   .put("warm",c!=null&&c.getBoolean("warm")).put("jobSha256",WorldFrames.hash(j)).put("goal",WorldFrames.copy(g)).put("commitHash",g.getJSONObject("worldCommit").getString("hash"))
   .put("recordedAt",Instant.now().toString()).put("scope","Admission through native work, reports, commit and optional retention; excludes this final timing record write and caller polling/transport");
  if(eligible)data.put("clock089",TaskCost089.clock(costClock089,began,ended089));
  AgentFiles042.saveChecked(costPath080(id),data);AgentFiles042.saveChecked(dir(id).resolve("cost-proof090.json"),new JSONObject().put("id",id).put("evidence",proofs.snapshot(costEvidence080(id))));return data;
 }
 private List<Path> costEvidence080(String id)throws Exception {
  var j=read(id);var paths=new ArrayList<Path>();paths.add(costPath080(id));paths.add(dir(id).resolve("goal.json"));
  for(Object raw:j.getJSONObject("arm").getJSONArray("attempts")){var a=(JSONObject)raw;if(a.has("sha256"))paths.add(attemptDir(j,a.getString("id")).resolve("original.json"));}return paths;
 }
 private JSONObject checkedCost080(String id)throws Exception {
  var c=cost080(id);if(c.isEmpty())throw new IllegalArgumentException("Missing complete task measurement");var j=read(id);compatible(j);
  if(!c.getString("code").equals(code())||!c.getString("jobSha256").equals(WorldFrames.hash(j))||!c.getJSONObject("task").similar(TaskCost080.task(j))||!c.getString("commitHash").equals(worlds.committed(j.getJSONObject("binding").getString("worldId"),id).getString("hash")))throw new IllegalStateException("Cost source or commit changed");audit(j);return c;
 }
 synchronized JSONObject fitCosts080(JSONArray selected)throws Exception {
  long fittingBegan085=System.nanoTime();
  if(busy()||selected.length()<2||selected.length()>8)throw new IllegalArgumentException("Select two to eight completed comparison pairs while idle");var pairs=new JSONArray();var seen=new HashSet<String>();var evidence=new JSONObject();
  for(Object raw:selected){var item=(JSONObject)raw;SpatialScene.keys(item,"simpleId","memoryId");String a=item.getString("simpleId"),b=item.getString("memoryId");if(!seen.add(a)||!seen.add(b))throw new IllegalArgumentException("Each measured goal may occur only once");
   var simple=checkedCost080(a);var memory=checkedCost080(b);pairs.put(TaskCost089.pair(simple,memory,read(b)));evidence.put(a,proofs.snapshot(costEvidence080(a))).put(b,proofs.snapshot(costEvidence080(b)));
  }
  var first=cost080(pairs.getJSONObject(0).getString("simpleId"));for(Object raw:pairs){var p=(JSONObject)raw;if(!cost080(p.getString("simpleId")).getJSONObject("profile").similar(first.getJSONObject("profile")))throw new IllegalArgumentException("One runtime profile per calibration");}
  var model=new JSONObject().put("id",UUID.randomUUID().toString()).put("version",TaskCost089.VERSION).put("code",code()).put("profile",first.getJSONObject("profile")).put("pairs",pairs).put("evidence",evidence).put("createdAt",Instant.now().toString());
  model.put("setup085",TaskCost085.setup(pairs,(System.nanoTime()-fittingBegan085)/1e6));
  Path history=root.resolve("cost-models080");if(Files.isDirectory(history))try(var files=Files.list(history)){if(files.count()>=16)throw new IllegalStateException("Calibration history limit16");}
  AgentFiles042.saveChecked(history.resolve(model.getString("id")+".json"),model);AgentFiles042.saveChecked(root.resolve("cost-model080.json"),new JSONObject().put("id",model.getString("id")));return model;
 }
 synchronized JSONObject calibrationStats081(){return calibrationScan081.stats();}
 synchronized JSONObject envelopeStats091(){return envelopes091.stats();}
 synchronized JSONObject testEnvelopeMode091(String mode){if(!"1".equals(System.getenv("FRAYNIX_WORKSPACE_TEST"))||busy())throw new IllegalStateException("Idle isolated test runtime required");envelopes091.testMode(mode);return envelopes091.stats();}
 synchronized JSONObject testCalibrationMode081(String mode){if(!"1".equals(System.getenv("FRAYNIX_WORKSPACE_TEST"))||busy())throw new IllegalStateException("Idle isolated test runtime required");calibrationScan081.testMode(mode);return calibrationScan081.stats();}
 private synchronized JSONObject loadCalibration080(String id)throws Exception {
  long began=System.nanoTime();double hashing=0;long bytes=0;boolean success=false;
  try{
   UUID.fromString(id);var model=AgentFiles042.loadChecked(root.resolve("cost-models080").resolve(id+".json"));
   if(!TaskCost089.VERSION.equals(model.getString("version"))||!model.getString("id").equals(id)||!model.getString("code").equals(code()))throw new IllegalStateException("Cost calibration implementation changed");
   var pairs=new JSONArray();var seen=new HashSet<String>();var groups=new LinkedHashMap<String,List<Path>>();
   for(Object raw:model.getJSONArray("pairs")){
    var p=(JSONObject)raw;String a=p.getString("simpleId"),b=p.getString("memoryId");if(!seen.add(a)||!seen.add(b))throw new IllegalStateException("Duplicate cost provenance");pairs.put(TaskCost089.pair(cost080(a),cost080(b),read(b)));
    for(String goal:List.of(a,b)){
     var paths=costEvidence080(goal);
     if(calibrationScan081.reference()){
      long at=System.nanoTime();JSONArray scan;try{scan=calibrationScan081.serial(paths);}finally{hashing+=(System.nanoTime()-at)/1e6;}
      if(!scan.similar(model.getJSONObject("evidence").getJSONArray(goal)))throw new IllegalStateException("Calibration evidence bytes changed");
      for(Object e:scan)bytes+=((JSONObject)e).getLong("bytes");
     }else groups.put(goal,paths);
    }
   }
   if(!calibrationScan081.reference()){
    long at=System.nanoTime();Map<String,JSONArray> scanned;try{scanned=calibrationScan081.parallel(groups);}finally{hashing+=(System.nanoTime()-at)/1e6;}
    for(var entry:scanned.entrySet()){
     if(!entry.getValue().similar(model.getJSONObject("evidence").getJSONArray(entry.getKey())))throw new IllegalStateException("Calibration evidence bytes changed");
     for(Object e:entry.getValue())bytes+=((JSONObject)e).getLong("bytes");
    }
   }
   if(!pairs.similar(model.getJSONArray("pairs")))throw new IllegalStateException("Calibration arithmetic changed");TaskCost085.verifySetup(model);success=true;return model;
  }finally{calibrationScan081.record((System.nanoTime()-began)/1e6,hashing,bytes,success);}
 }

 private JSONArray observations090(JSONObject j){var a=new JSONArray();for(Object raw:j.optJSONArray("freshnessEvidence090",new JSONArray()))a.put(((JSONObject)raw).getJSONObject("observation"));return a;}
 private JSONArray recentCosts090(JSONObject j,JSONArray candidates,JSONObject model)throws Exception {
  var result=new JSONArray();if(!j.getString("mode").equals("calibrated")||model.isEmpty())return result;
  var d=TaskCost089.choose(j.getJSONObject("spec"),"calibrated",candidates,j.getJSONObject("args").getJSONObject("workload080"),model,j.getJSONObject("args").optString("procedureReceipt085",""),j.getJSONArray("bankSnapshot").length(),j.getString("selector089"));
  var candidate=d.optJSONObject("candidate");if(candidate==null||!d.optJSONObject("cost080",new JSONObject()).optBoolean("available"))return result;
  if(TaskCost090.guard(j.getJSONObject("spec"),d,new JSONArray()).getBoolean("blocked"))return result;
  // A verified warning remains attached to this model and scene neighborhood until a new fit.
  for(var priorJob:new ArrayList<>(jobs.values())) {
   var priorDecision=priorJob.optJSONObject("decision",new JSONObject());var f=priorDecision.optJSONObject("freshness090",new JSONObject());
   var c=priorDecision.optJSONObject("candidate",new JSONObject());var m=priorJob.optJSONObject("calibration080",new JSONObject());
   if(!priorJob.getString("id").equals(j.getString("id"))&&f.optString("status").equals("suspect")&&m.optString("id").equals(model.getString("id"))
      &&c.optString("id").equals(candidate.getString("id"))&&c.optBoolean("warm")==candidate.getBoolean("warm")
      &&priorJob.getJSONObject("args").getJSONObject("workload080").similar(j.getJSONObject("args").getJSONObject("workload080"))&&TaskCost090.nearby(priorJob.getJSONObject("spec"),j.getJSONObject("spec"))) {
    checkFreshness090(priorJob);var saved=priorJob.getJSONArray("freshnessEvidence090");boolean matched=true;
    for(Object raw:saved){var cost=cost080(((JSONObject)raw).getJSONObject("observation").getString("id"));matched&=TaskCost090.eligible(cost,candidate,j.getJSONObject("spec"),j.getJSONObject("args").getJSONObject("workload080"),model);}
    if(matched)return new JSONArray(saved.toString());
   }
  }
  var relevant=new ArrayList<JSONObject>();
  for(String id:new ArrayList<>(jobs.keySet())){if(id.equals(j.getString("id")))continue;var c=cost080(id);if(!c.isEmpty()&&TaskCost090.eligible(c,candidate,j.getJSONObject("spec"),j.getJSONObject("args").getJSONObject("workload080"),model))relevant.add(c);}
  relevant.sort(Comparator.comparing((JSONObject c)->Instant.parse(c.getString("recordedAt"))).thenComparing(c->c.getString("id")).reversed());
  for(var c:relevant.subList(0,Math.min(TaskCost090.LIMIT,relevant.size()))){String id=c.getString("id");var anchor=AgentFiles042.loadChecked(dir(id).resolve("cost-proof090.json"));if(!anchor.getString("id").equals(id)||!anchor.getJSONArray("evidence").similar(proofs.snapshot(costEvidence080(id))))throw new IllegalStateException("Recent cost evidence changed");checkedCost080(id);
   result.put(new JSONObject().put("anchor",anchor).put("observation",TaskCost090.observation(c,read(id),candidate,model)));
  }return result;
 }
 private void checkFreshness090(JSONObject j)throws Exception {
  var items=j.optJSONArray("freshnessEvidence090",new JSONArray());if(items.length()>TaskCost090.LIMIT)throw new IllegalStateException("Recent cost evidence exceeds bound");
  if(!j.getString("mode").equals("calibrated")&&!items.isEmpty())throw new IllegalStateException("Fixed control consulted freshness evidence");
  var seen=new HashSet<String>();for(Object raw:items){var item=(JSONObject)raw;var o=item.getJSONObject("observation");String id=o.getString("id");if(id.equals(j.getString("id"))||!seen.add(id))throw new IllegalStateException("Cyclic or repeated freshness evidence");
   var c=cost080(id);var candidate=j.getJSONObject("decision").getJSONObject("candidate");var model=j.getJSONObject("calibration080");
   if(!TaskCost090.eligible(c,candidate,j.getJSONObject("spec"),j.getJSONObject("args").getJSONObject("workload080"),model)||!Instant.parse(c.getString("recordedAt")).isBefore(Instant.parse(j.getString("createdAt"))))throw new IllegalStateException("Freshness source was unavailable or ineligible at decision time");
   var anchor=AgentFiles042.loadChecked(dir(id).resolve("cost-proof090.json"));if(!anchor.similar(item.getJSONObject("anchor"))||!anchor.getJSONArray("evidence").similar(proofs.snapshot(costEvidence080(id)))||!TaskCost090.observation(c,read(id),candidate,model).similar(o))throw new IllegalStateException("Frozen freshness evidence changed");
  }
 }
 synchronized JSONObject calibration080()throws Exception {var p=root.resolve("cost-model080.json");return Files.exists(p)?loadCalibration080(AgentFiles042.loadChecked(p).getString("id")):new JSONObject();}
 @Override public void close(){closed=true;worker.shutdown();try{worker.awaitTermination(10,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}calibrationScan081.close();}
}
