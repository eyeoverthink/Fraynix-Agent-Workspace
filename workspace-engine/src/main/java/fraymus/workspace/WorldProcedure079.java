package fraymus.workspace;

import org.json.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/** Durable, bounded native procedures for the existing Java goal runner. */
final class WorldProcedure079 implements AutoCloseable {
 private final Path root;private final WorldLedger worlds;private final ProofMemo078 proofs;private final CalibrationScan081 calibrationScan081;
 private final Map<String,JSONObject> jobs=new LinkedHashMap<>(),published=new LinkedHashMap<>(),warm=new HashMap<>();
 private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"world-procedure-079");t.setDaemon(true);return t;});
 private final Set<String> stops=ConcurrentHashMap.newKeySet();private volatile boolean closed;private volatile String active="";private volatile WorldNativeStore owner;private volatile String nativeId="",activePhase="";private volatile long stageStartTicks;private final Map<String,JSONObject> lastActual=new ConcurrentHashMap<>();private long began;private double prior;
 WorldProcedure079(Path project,WorldLedger worlds)throws Exception {this.worlds=worlds;root=project.resolve("world-procedures-079");proofs=new ProofMemo078(root);calibrationScan081=new CalibrationScan081(root);if(Files.exists(root))try(var dirs=Files.list(root)){for(Path d:dirs.filter(Files::isDirectory).toList())if(Files.exists(d.resolve("goal.json"))){var j=AgentFiles042.loadChecked(d.resolve("goal.json"));if(!d.getFileName().toString().equals(j.getString("id")))throw new IllegalStateException("Procedure identity changed");jobs.put(j.getString("id"),j);if(Set.of("running","interrupted").contains(j.getString("status"))){j.put("status","interrupted").put("reason","Process ended; explicit bounded same-run resume required");try{reconcileInterrupted(j);}catch(Exception e){j.put("recoveryError",String.valueOf(e.getMessage())).put("reason","Original state cannot be reconciled: "+e.getMessage());}save(j);}else published.put(j.getString("id"),WorldFrames.copy(j));}}}
 static String code(){var x=new JSONObject().put("native",WorldNativeCheckpoint.identity());for(Class<?> c:new Class<?>[]{WorldProcedure079.class,Procedure079.class,Waypoints079.class,Procedure078.class,Procedure077.class,Waypoints077.class,ProofMemo078.class,TaskCost080.class,CalibrationScan081.class,GoalRunner.class})try(var in=c.getResourceAsStream(c.getSimpleName()+".class")){x.put(c.getSimpleName(),MapDefinition.sha(in.readAllBytes()));}catch(Exception e){throw new IllegalStateException(e);}return WorldFrames.hash(x);}
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
  var j=new JSONObject().put("id",id).put("code",code()).put("binding",binding).put("args",WorldFrames.copy(args)).put("initialFrame",frame).put("spec",spec).put("leasePlan",plan).put("mode",mode).put("status","queued").put("reason","Queued").put("resumes",0).put("activeMillis",0d).put("createdAt",Instant.now().toString()).put("arm",new JSONObject().put("method",mode).put("success",false).put("attempts",new JSONArray()).put("programs",new JSONObject()).put("chargedTicks",0).put("observedTicks",0));jobs.put(id,j);save(j);launch(j);return read(id);
 }
 private synchronized void launch(JSONObject j)throws Exception {active=j.getString("id");prior=j.getDouble("activeMillis");began=System.nanoTime();stops.remove(active);j.put("status","running");save(j);worker.submit(()->execute(j));}
 synchronized JSONObject resume(String id)throws Exception {var j=jobs.get(id);if(j==null||closed||busy()||!j.getString("status").equals("interrupted")||j.getInt("resumes")>=2||j.getDouble("activeMillis")>=1_200_000)throw new IllegalStateException("Explicit interrupted-procedure resume only, at most twice");compatible(j);if(j.has("recoveryError"))throw new IllegalStateException(j.getString("recoveryError"));current(j);j.put("resumes",j.getInt("resumes")+1);launch(j);return read(id);}
 synchronized void stop(String id)throws Exception {var j=jobs.get(id);if(j==null)return;stops.add(id);if(!id.equals(active)&&!Set.of("ready","cancelled").contains(j.getString("status"))){j.put("status","cancelled").put("reason","Stopped; no successful world commit");save(j);}if(owner!=null&&id.equals(active)&&!nativeId.isEmpty())owner.cancel(nativeId);}
 private void guard(JSONObject j)throws InterruptedException {if(closed||stops.contains(j.getString("id")))throw new InterruptedException("Procedure stopped or Java ended");}
 private void current(JSONObject j)throws Exception {var w=worlds.read(j.getJSONObject("binding").getString("worldId"));if(!WorldLedger.binding(w).similar(j.getJSONObject("binding")))throw new IllegalStateException("World revision changed before execution");if(w.has("pending")&&!w.getJSONObject("pending").getString("goalId").equals(j.getString("id")))throw new IllegalStateException("Foreign world owner");}
 private JSONObject raw(JSONObject j,JSONObject a)throws Exception {var envelope=AgentFiles042.read(attemptDir(j,a.getString("id")).resolve("original.json"),32_000_000);var run=envelope.getJSONObject("data");if(!envelope.getString("format").equals("java-agent-042-v1")||!envelope.getString("sha256").equals(WorldFrames.hash(run)))throw new IllegalStateException("Original native envelope changed");if(!WorldFrames.hash(run).equals(a.getString("sha256"))||!run.getString("id").equals(a.getString("id"))||!run.getString("goalId").equals(j.getString("id")))throw new IllegalStateException("Changed or foreign original native attempt");return run;}
 private JSONObject attempt(JSONObject j,String id){for(Object raw:j.getJSONObject("arm").getJSONArray("attempts"))if(((JSONObject)raw).getString("id").equals(id))return(JSONObject)raw;throw new IllegalStateException("Unowned native attempt");}
 private void settle(JSONObject j,JSONObject a,JSONObject run,boolean uncertain)throws Exception {int n=run.has("final")?run.getJSONObject("final").getInt("tick"):run.getJSONArray("samples").isEmpty()?0:run.getJSONArray("samples").getJSONObject(run.getJSONArray("samples").length()-1).getInt("tick");boolean tail=uncertain||a.optBoolean("unknownTail");a.put("status",run.getString("status")).put("observedTicks",n).put("chargedTicks",tail?WorldPhysics.MAX_TICKS:n).put("unknownTail",tail).put("sha256",WorldFrames.hash(run));if(run.has("final"))a.put("metrics",WorldVerify.verify(run));AgentFiles042.saveChecked(attemptDir(j,a.getString("id")).resolve("original.json"),run);ProcedureStudy078.recomputeCosts(j.getJSONObject("arm"));save(j);}
 private JSONObject trial(JSONObject j,String phase,String family,int step,JSONObject frame,JSONObject plan,int remaining)throws Exception {
  var arm=j.getJSONObject("arm");String stage=phase+"/"+step;JSONObject a=null;for(Object raw:arm.getJSONArray("attempts")){var t=(JSONObject)raw;if(t.getString("family").equals(family)&&t.getString("stage").equals(stage))a=t;}
  if(a!=null&&Set.of("completed","failed","cancelled").contains(a.getString("status")))return a;
  guard(j);current(j);save(j);
  if(a==null&&!Procedure078.admitted(arm.getInt("chargedTicks"),remaining,j.getDouble("activeMillis")))throw new IllegalStateException("Remaining full procedure exceeds tick/time budget");
  if(a==null){String id=UUID.randomUUID().toString();a=new JSONObject().put("id",id).put("family",family).put("stage",stage).put("status","running").put("plannedAt",Instant.now().toString()).put("sourceSha256",WorldFrames.hash(WorldFrames.scene(frame))).put("planSha256",WorldFrames.hash(plan)).put("admissionSpent",arm.getInt("chargedTicks")).put("admissionRemaining",remaining).put("wallMillis",0d);arm.getJSONArray("attempts").put(a);save(j);}
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
    p.getJSONObject("waypoints").put(phase+"/"+i,choice);var a=trial(j,phase,family,i,frame,choice.getJSONObject("plan"),steps-i+(phase.equals("forecast")?steps:0));p.getJSONArray(phase).put(i,a.getString("id"));save(j);
    if(!a.getString("status").equals("completed")||!a.optJSONObject("metrics",new JSONObject()).optBoolean("passed")){if(phase.equals("actual"))throw new IllegalStateException("Actual motion failed; no reset, fallback or commit");return p.put("status","failed").put("reason","Original native forecast did not pass");}
    var run=raw(j,a);if(!Procedure079.protectedTrace(j.getJSONObject("initialFrame"),spec,run))throw new IllegalStateException("Whole-task protected body drift");frame=WorldFrames.after(frame,run);
    if(phase.equals("actual")){var comparison=Procedure079.comparison(raw(j,attempt(j,p.getJSONArray("forecast").getString(i))),run);p.getJSONArray("comparisons").put(i,comparison);if(!comparison.getBoolean("passed"))throw new IllegalStateException("Forecast/actual mismatch; no commit");}
   }
   p.put(phase+"Frame",frame).put(phase+"Task",Procedure079.task(j.getJSONObject("initialFrame"),spec,frame));if(!p.getJSONObject(phase+"Task").getBoolean("passed")){if(phase.equals("actual"))throw new IllegalStateException("Final actual task failed");return p.put("status","failed").put("reason","Whole forecast task failed");}
  }
  return p.put("status","completed").put("success",true);
 }
 private void execute(JSONObject j){try{compatible(j);current(j);if(!j.has("decision")){long at=System.nanoTime();var cs=new JSONArray();var receipts=j.getString("mode").equals("simple")?new JSONArray():bank();for(Object raw:receipts){var b=(JSONObject)raw;try{var c=Procedure079.estimate(b,j.getJSONObject("spec"),isWarm(b));if(c.getBoolean("warm"))c.put("warmProof",WorldFrames.copy(warm.get(b.getString("id"))));cs.put(c);}catch(IllegalArgumentException incompatible){}}var calibration=j.getString("mode").equals("calibrated")?calibration080():new JSONObject();j.put("calibration080",calibration);var decision=decision080(j,cs,calibration);j.put("decision",decision).put("bankSnapshot",receipts);if(decision.getBoolean("useMemory")){var b=find(receipts,decision.getJSONObject("candidate").getString("id"));j.put("lookupProof",check(b));}j.put("lookupMillis",(System.nanoTime()-at)/1e6);j.getJSONObject("arm").put("candidateOrder",new JSONArray(Procedure079.order(decision)));save(j);}
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
 synchronized JSONObject retain(String id,JSONObject commit)throws Exception {var j=read(id);compatible(j);if(!j.getString("status").equals("ready")||!commit.getString("goalId").equals(id))throw new IllegalStateException("Only a verified committed procedure can be remembered");var entries=bank();for(Object x:entries)if(((JSONObject)x).getString("id").equals(id))return(JSONObject)x;if(entries.length()>=16)return new JSONObject().put("retained",false).put("reason","Memory capacity16 reached; originals remain");String family=j.getJSONObject("arm").getString("selected");double avoided=0;for(Object raw:j.getJSONObject("arm").getJSONArray("attempts")){var a=(JSONObject)raw;if(!a.getString("family").equals(family))avoided+=a.getDouble("wallMillis");}
  var b=new JSONObject().put("id",id).put("code",code()).put("spec",j.getJSONObject("spec")).put("family",family).put("activeMillis",j.getDouble("activeMillis")).put("auditMillis",j.getDouble("auditMillis")).put("avoidedMillis",avoided).put("commitHash",commit.getString("hash")).put("jobSha256",WorldFrames.hash(j)).put("retainedAt",Instant.now().toString());AgentFiles042.saveChecked(dir(id).resolve("memory.json"),b);check(b);entries.put(b);AgentFiles042.saveChecked(root.resolve("bank.json"),new JSONObject().put("entries",entries));return b;
 }
 JSONObject audit(String id)throws Exception{return audit(read(id));}
 private void selection(JSONObject j)throws Exception {
  var decision=j.getJSONObject("decision");var receipts=j.getJSONArray("bankSnapshot");var candidates=decision.getJSONArray("candidates");var rebuilt=new JSONArray();
  if(j.getString("mode").equals("simple")&&(!receipts.isEmpty()||!candidates.isEmpty()||j.has("lookupProof")))throw new IllegalStateException("Simple control consulted memory");
  for(Object raw:receipts){var b=(JSONObject)raw;var disk=AgentFiles042.loadChecked(dir(b.getString("id")).resolve("memory.json"));if(!disk.similar(b)||!b.getString("code").equals(code()))throw new IllegalStateException("Bank receipt source changed");JSONObject candidate=null;for(Object x:candidates)if(((JSONObject)x).getString("id").equals(b.getString("id")))candidate=(JSONObject)x;
   try{var expected=Procedure079.estimate(b,j.getJSONObject("spec"),candidate!=null&&candidate.getBoolean("warm"));if(candidate!=null&&candidate.getBoolean("warm")){var proof=candidate.getJSONObject("warmProof");var stored=AgentFiles042.loadChecked(root.resolve("proof-history").resolve(WorldFrames.hash(proof)+".json"));if(!stored.similar(proof)||!proof.getString("mode").equals("full")||!proof.getJSONObject("binding").similar(binding(b))||!proof.getString("key").equals(WorldFrames.hash(binding(b))))throw new IllegalStateException("Warm selection lacks owned full proof");expected.put("warmProof",proof);}rebuilt.put(expected);}catch(IllegalArgumentException incompatible){if(candidate!=null)throw new IllegalStateException("Incompatible candidate admitted",incompatible);}
  }
  if(!rebuilt.similar(candidates)||!decision080(j,rebuilt,checkedModel080(j)).similar(decision)||!new JSONArray(Procedure079.order(decision)).similar(j.getJSONObject("arm").getJSONArray("candidateOrder")))throw new IllegalStateException("Selection rule or measured estimate changed");
  if(decision.getBoolean("useMemory")){var b=find(receipts,decision.getJSONObject("candidate").getString("id"));var proof=j.getJSONObject("lookupProof");if(!proof.getJSONObject("binding").similar(binding(b))||!proof.getJSONArray("evidence").similar(proofs.snapshot(evidence(b))))throw new IllegalStateException("Selected memory no longer has original proof");if(proof.getString("mode").equals("hit")){var priorProof=AgentFiles042.loadChecked(root.resolve("proof-history").resolve(proof.getString("originReceiptSha256")+".json"));if(!priorProof.getJSONArray("evidence").similar(proof.getJSONArray("evidence"))||!priorProof.getJSONObject("binding").similar(proof.getJSONObject("binding"))||!priorProof.getString("mode").equals("full"))throw new IllegalStateException("Unbacked proof hit");}else if(!proof.getString("mode").equals("full"))throw new IllegalStateException("Unknown proof mode");}
  else if(j.has("lookupProof"))throw new IllegalStateException("Skipped lookup claimed a proof");
 }
 JSONObject audit(JSONObject j)throws Exception {
  compatible(j);WorldFrames.verify(j.getJSONObject("initialFrame"));var initial=j.getJSONObject("initialFrame");var spec=j.getJSONObject("spec");if(!Procedure079.spec(initial,j.getJSONObject("args")).similar(spec))throw new IllegalStateException("Frozen current-world request changed");selection(j);
  var arm=j.getJSONObject("arm");var order=Procedure079.order(j.getJSONObject("decision"));int ticks=0,observed=0,previous=-1;var seen=new HashSet<String>();
  for(Object x:arm.getJSONArray("attempts")){var a=(JSONObject)x;int index=order.indexOf(a.getString("family"));if(index<previous||index<0||!seen.add(a.getString("id")))throw new IllegalStateException("Reordered or reused native identity");previous=index;var r=raw(j,a);int n=r.has("final")?r.getJSONObject("final").getInt("tick"):r.getJSONArray("samples").isEmpty()?0:r.getJSONArray("samples").getJSONObject(r.getJSONArray("samples").length()-1).getInt("tick");
   if(a.getInt("admissionSpent")!=ticks||!Procedure078.admitted(ticks,a.getInt("admissionRemaining"),0)||n!=a.getInt("observedTicks")||a.getInt("chargedTicks")!=(a.optBoolean("unknownTail")?14400:n)||n>14400||!Double.isFinite(a.getDouble("wallMillis"))||a.getDouble("wallMillis")<0||!a.getString("status").equals(r.getString("status"))||Instant.parse(r.getString("createdAt")).isBefore(Instant.parse(a.getString("plannedAt"))))throw new IllegalStateException("Native admission, cost or chronology changed");
   if(r.has("final")&&!WorldVerify.verify(r).similar(a.getJSONObject("metrics")))throw new IllegalStateException("Original native metrics changed");ticks+=a.getInt("chargedTicks");observed+=n;
  }
  if(ticks!=arm.getInt("chargedTicks")||observed!=arm.getInt("observedTicks")||ticks>115200)throw new IllegalStateException("Goal work exceeds bound");boolean success=false,missing=false;String selected="";JSONObject finalFrame=null;var bound=new HashSet<String>();
  for(String family:order){var p=arm.getJSONObject("programs").optJSONObject(family);if(p==null){missing=true;continue;}if(missing||success)throw new IllegalStateException("Procedure skipped or continued after success");int steps=Procedure078.stages(family);boolean[] complete={false,false};int phaseIndex=0;
   for(String phase:List.of("forecast","actual")){var frame=WorldFrames.copy(initial);var ids=p.getJSONArray(phase);if(ids.length()>steps||phase.equals("actual")&&!complete[0]&&!ids.isEmpty())throw new IllegalStateException("Actual before complete forecast");boolean valid=true;int executed=0;
    for(int i=0;i<steps;i++){String key=phase+"/"+i;JSONObject choice;
     try{choice=Waypoints079.choose(spec,family,i,initial,frame);if(p.getJSONObject("waypoints").has(key)&&!choice.similar(p.getJSONObject("waypoints").getJSONObject(key)))throw new IllegalStateException("Waypoint search changed");}catch(IllegalArgumentException rejected){for(Object x:arm.getJSONArray("attempts")){var a=(JSONObject)x;if(a.getString("family").equals(family)&&a.getString("stage").equals(key))throw rejected;}valid=false;break;}
     for(Object x:arm.getJSONArray("attempts")){var a=(JSONObject)x;if(!a.getString("family").equals(family)||!a.getString("stage").equals(key))continue;bound.add(a.getString("id"));var r=raw(j,a);if(a.getInt("admissionRemaining")!=steps-i+(phase.equals("forecast")?steps:0)||!choice.getJSONObject("plan").getBoolean("passed")||!p.getJSONObject("waypoints").has(key)||!r.getJSONObject("definition").similar(WorldFrames.scene(frame))||!r.getJSONObject("plan").similar(choice.getJSONObject("plan"))||!a.getString("sourceSha256").equals(WorldFrames.hash(r.getJSONObject("definition")))||!a.getString("planSha256").equals(WorldFrames.hash(r.getJSONObject("plan"))))throw new IllegalStateException("Procedure or measured handoff substituted");}
     if(i>=ids.length())break;var a=attempt(j,ids.getString(i));if(!a.getString("stage").equals(key)||!a.getString("family").equals(family))throw new IllegalStateException("Wrong stage reference");if(!a.getString("status").equals("completed")||!a.getJSONObject("metrics").getBoolean("passed")){valid=false;break;}var r=raw(j,a);valid=Procedure079.protectedTrace(initial,spec,r);if(!valid)break;frame=WorldFrames.after(frame,r);executed++;
     if(phase.equals("actual")){var c=Procedure079.comparison(raw(j,attempt(j,p.getJSONArray("forecast").getString(i))),r);if(!c.similar(p.getJSONArray("comparisons").getJSONObject(i)))throw new IllegalStateException("Forecast comparison changed");valid&=c.getBoolean("passed");}if(!valid)break;
    }
    var task=Procedure079.task(initial,spec,frame);complete[phaseIndex]=valid&&executed==steps&&task.getBoolean("passed");if(p.has(phase+"Frame")&&(!frame.similar(p.getJSONObject(phase+"Frame"))||!task.similar(p.getJSONObject(phase+"Task"))))throw new IllegalStateException("Whole-task frame changed");if(phase.equals("actual")&&complete[phaseIndex])finalFrame=frame;phaseIndex++;
   }
   boolean passed=complete[0]&&complete[1];if(p.getBoolean("success")!=passed||passed&&!p.getString("status").equals("completed"))throw new IllegalStateException("False procedure success");if(passed){success=true;selected=family;}
  }
  if(!success||!arm.getBoolean("success")||!arm.getString("selected").equals(selected)||bound.size()!=seen.size())throw new IllegalStateException("Unbound action or wrong selected outcome");return new JSONObject().put("passed",true).put("ticks",ticks).put("nativeStages",seen.size()).put("family",selected).put("finalFrame",finalFrame).put("positionM",Procedure079.task(initial,spec,finalFrame).getDouble("positionM"));
 }
 JSONObject snapshot(String id)throws Exception {var running=owner;if(id.equals(active)&&activePhase.equals("actual")&&running!=null){var view=running.snapshot();if(view.has("sample")){view.put("elapsedTicksBeforeStage",stageStartTicks);return view;}}var prior=lastActual.get(id);if(prior!=null)return WorldFrames.copy(prior);if(!has(id))return new JSONObject();var j=read(id);var attempts=j.getJSONObject("arm").getJSONArray("attempts");for(int i=attempts.length()-1;i>=0;i--){var a=attempts.getJSONObject(i);if(!a.getString("stage").startsWith("actual/")||!a.has("sha256"))continue;var r=raw(j,a);if(r.getJSONArray("samples").isEmpty())continue;var f=r.has("final")?r.getJSONObject("final"):r.getJSONArray("samples").getJSONObject(r.getJSONArray("samples").length()-1);var objects=WorldFrames.copy(r.getJSONObject("definition")).getJSONArray("objects");for(Object x:f.getJSONArray("bodies")){var b=(JSONObject)x;for(Object o:objects)if(((JSONObject)o).getString("id").equals(b.getString("id")))for(String k:List.of("position","rotation","velocity","angularVelocity"))((JSONObject)o).put(k,b.get(k));}var view=new JSONObject().put("id",r.getString("id")).put("status",r.getString("status")).put("sample",f).put("current",new JSONObject().put("objects",objects).put("grippingId",f.optJSONObject("controller",new JSONObject()).optString("grippingId"))).put("elapsedTicksBeforeStage",r.getJSONObject("definition").getJSONObject("worldFrame").getLong("elapsedTicks"));lastActual.put(id,view);return WorldFrames.copy(view);}return new JSONObject();}
 JSONObject last(String id)throws Exception {var j=read(id);var p=j.getJSONObject("arm").getJSONObject("programs").getJSONObject(j.getJSONObject("arm").getString("selected"));var xs=p.getJSONArray("actual");return raw(j,attempt(j,xs.getString(xs.length()-1)));}
 JSONObject summary(String id){var j=read(id);var x=new JSONObject();for(String k:List.of("id","status","reason","mode","activeMillis","lookupMillis","auditMillis","decision","lookupProof","resumes"))if(j.has(k))x.put(k,j.get(k));x.put("chargedTicks",j.getJSONObject("arm").getInt("chargedTicks")).put("nativeStages",j.getJSONObject("arm").getJSONArray("attempts").length());return x;}
 JSONObject export(String id)throws Exception {var j=read(id);var runs=new JSONArray();for(Object x:j.getJSONObject("arm").getJSONArray("attempts")){var a=(JSONObject)x;if(a.has("sha256"))runs.put(WorldReport.packed(raw(j,a)));}return new JSONObject().put("format",Procedure079.KIND).put("job",j).put("originals",runs).put("cost080",cost080(id));}
 JSONObject report(String id)throws Exception {
  var summary=summary(id);var r=WorldReport.build(last(id));r.put("title","Verified world procedure");var files=r.getJSONObject("files");var decision=summary.getJSONObject("decision");var candidate=decision.optJSONObject("candidate");String cost=candidate==null?"No compatible experience needed.":String.format(Locale.ROOT,"Estimated avoided work %.3f s; proof %.3f s; uncertainty margin %.3f s.",candidate.getDouble("avoidedMillis")/1000,candidate.getDouble("proofEstimateMillis")/1000,candidate.getDouble("uncertaintyMarginMillis")/1000);
  if(decision.has("cost080")){var e=decision.getJSONObject("cost080");cost=e.optBoolean("available")?String.format(Locale.ROOT,"Complete task estimate: simple %.3f s; memory %.3f s; saving %.3f s with %.3f s empirical margin. Includes checks and commit; not a statistical guarantee.",e.getDouble("simpleMillis")/1000,e.getDouble("memoryMillis")/1000,e.getDouble("savingMillis")/1000,e.getDouble("marginMillis")/1000):e.getString("reason")+". No complete-task saving is claimed.";}
  String text=summary.toString(2).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");String section="<section id=\"procedure079-decision\"><h2>Memory decision from measured cost</h2><p>"+decision.getString("reason")+"</p><p>"+cost+"</p><p>"+summary.getInt("nativeStages")+" native stages · "+summary.getInt("chargedTicks")+" charged ticks. Every failed forecast is included.</p><p>These six charts show the final actual stage. The complete procedure export preserves all stages, measured handoffs and original proofs.</p><details><summary>Decision and cost evidence</summary><pre>"+text+"</pre></details></section>";
  files.put("index.html",files.getString("index.html").replace("<h1 id=\"pose-result\">",section+"<h1 id=\"pose-result\">"));files.put("procedure.json",summary.toString());return r;
 }

 private JSONObject decision080(JSONObject j,JSONArray candidates,JSONObject model){return j.getString("mode").equals("calibrated")?TaskCost080.choose(j.getJSONObject("spec"),candidates,j.getJSONObject("args").optJSONObject("workload080",new JSONObject().put("retain",false).put("skill",false)),model):Procedure079.choose(j.getJSONObject("spec"),j.getString("mode"),candidates);}
 private JSONObject checkedModel080(JSONObject j)throws Exception {var m=j.optJSONObject("calibration080",new JSONObject());if(!m.isEmpty()&&!loadCalibration080(m.getString("id")).similar(m))throw new IllegalStateException("Frozen cost calibration changed");return m;}
 private Path costPath080(String id){return dir(id).resolve("cost080.json");}
 synchronized JSONObject cost080(String id)throws Exception {return Files.exists(costPath080(id))?AgentFiles042.loadChecked(costPath080(id)):new JSONObject();}
 synchronized JSONObject recordCost080(JSONObject g,Long began)throws Exception {
  String id=g.getString("id");var old=cost080(id);if(!old.isEmpty())return old;var j=read(id);compatible(j);
  if(!g.getString("status").equals("completed")||!g.getJSONObject("poseView").getBoolean("passed")||!worlds.committed(j.getJSONObject("binding").getString("worldId"),id).similar(g.getJSONObject("worldCommit")))throw new IllegalStateException("Only completed, verified committed goals have complete cost");
  var decision=j.getJSONObject("decision");var c=decision.optJSONObject("candidate");boolean eligible=began!=null&&g.optInt("resumes")==0&&j.optInt("resumes")==0;
  double total=began==null?0:(System.nanoTime()-began)/1e6,nativeMs=0;for(Object raw:j.getJSONObject("arm").getJSONArray("attempts"))nativeMs+=((JSONObject)raw).getDouble("wallMillis");
  var data=new JSONObject().put("version",TaskCost080.VERSION).put("id",id).put("code",code()).put("completed",true).put("eligible",eligible)
   .put("reason",eligible?"Uninterrupted monotonic complete-task measurement":"Interrupted or missing monotonic clock; excluded from calibration")
   .put("totalMillis",total).put("nativeStageMillis",nativeMs).put("executionMillis",j.getDouble("activeMillis")).put("profile",TaskCost080.profile())
   .put("workload",TaskCost080.workload(g)).put("task",TaskCost080.task(j)).put("spec",j.getJSONObject("spec")).put("mode",j.getString("mode"))
   .put("usedMemory",decision.getBoolean("useMemory")).put("preferred",decision.getString("preferred")).put("receiptId",c==null?"":c.getString("id"))
   .put("warm",c!=null&&c.getBoolean("warm")).put("jobSha256",WorldFrames.hash(j)).put("goal",WorldFrames.copy(g)).put("commitHash",g.getJSONObject("worldCommit").getString("hash"))
   .put("recordedAt",Instant.now().toString()).put("scope","Admission through native work, reports, commit and optional retention; excludes this final timing record write and caller polling/transport");
  AgentFiles042.saveChecked(costPath080(id),data);return data;
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
  if(busy()||selected.length()<2||selected.length()>8)throw new IllegalArgumentException("Select two to eight completed comparison pairs while idle");var pairs=new JSONArray();var seen=new HashSet<String>();var evidence=new JSONObject();
  for(Object raw:selected){var item=(JSONObject)raw;SpatialScene.keys(item,"simpleId","memoryId");String a=item.getString("simpleId"),b=item.getString("memoryId");if(!seen.add(a)||!seen.add(b))throw new IllegalArgumentException("Each measured goal may occur only once");
   var simple=checkedCost080(a);var memory=checkedCost080(b);pairs.put(TaskCost080.pair(simple,memory));evidence.put(a,proofs.snapshot(costEvidence080(a))).put(b,proofs.snapshot(costEvidence080(b)));
  }
  var first=cost080(pairs.getJSONObject(0).getString("simpleId"));for(Object raw:pairs){var p=(JSONObject)raw;if(!cost080(p.getString("simpleId")).getJSONObject("profile").similar(first.getJSONObject("profile")))throw new IllegalArgumentException("One runtime profile per calibration");}
  var model=new JSONObject().put("id",UUID.randomUUID().toString()).put("version",TaskCost080.VERSION).put("code",code()).put("profile",first.getJSONObject("profile")).put("pairs",pairs).put("evidence",evidence).put("createdAt",Instant.now().toString());
  Path history=root.resolve("cost-models080");if(Files.isDirectory(history))try(var files=Files.list(history)){if(files.count()>=16)throw new IllegalStateException("Calibration history limit16");}
  AgentFiles042.saveChecked(history.resolve(model.getString("id")+".json"),model);AgentFiles042.saveChecked(root.resolve("cost-model080.json"),new JSONObject().put("id",model.getString("id")));return model;
 }
 synchronized JSONObject calibrationStats081(){return calibrationScan081.stats();}
 synchronized JSONObject testCalibrationMode081(String mode){if(!"1".equals(System.getenv("FRAYNIX_WORKSPACE_TEST"))||busy())throw new IllegalStateException("Idle isolated test runtime required");calibrationScan081.testMode(mode);return calibrationScan081.stats();}
 private synchronized JSONObject loadCalibration080(String id)throws Exception {
  long began=System.nanoTime();double hashing=0;long bytes=0;boolean success=false;
  try{
   UUID.fromString(id);var model=AgentFiles042.loadChecked(root.resolve("cost-models080").resolve(id+".json"));
   if(!model.getString("id").equals(id)||!model.getString("code").equals(code()))throw new IllegalStateException("Cost calibration implementation changed");
   var pairs=new JSONArray();var seen=new HashSet<String>();var groups=new LinkedHashMap<String,List<Path>>();
   for(Object raw:model.getJSONArray("pairs")){
    var p=(JSONObject)raw;String a=p.getString("simpleId"),b=p.getString("memoryId");if(!seen.add(a)||!seen.add(b))throw new IllegalStateException("Duplicate cost provenance");pairs.put(TaskCost080.pair(cost080(a),cost080(b)));
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
   if(!pairs.similar(model.getJSONArray("pairs")))throw new IllegalStateException("Calibration arithmetic changed");success=true;return model;
  }finally{calibrationScan081.record((System.nanoTime()-began)/1e6,hashing,bytes,success);}
 }
 synchronized JSONObject calibration080()throws Exception {var p=root.resolve("cost-model080.json");return Files.exists(p)?loadCalibration080(AgentFiles042.loadChecked(p).getString("id")):new JSONObject();}
 @Override public void close(){closed=true;worker.shutdown();try{worker.awaitTermination(10,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}calibrationScan081.close();}
}
