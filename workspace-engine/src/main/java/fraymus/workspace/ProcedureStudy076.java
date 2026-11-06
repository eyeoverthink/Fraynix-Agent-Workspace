package fraymus.workspace;

import org.json.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/** Isolated native experiments with a frozen owned bank and durable, fully charged attempts. */
final class ProcedureStudy076 implements AutoCloseable {
    private final Path root;
    private final Map<String,JSONObject> jobs=new LinkedHashMap<>();
    private final Map<String,JSONObject> published=new LinkedHashMap<>();
    private final Set<String> stops=ConcurrentHashMap.newKeySet();
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"verified-procedures-076");t.setDaemon(true);return t;});
    private volatile boolean closed;private volatile String active="";private volatile WorldNativeStore owner;private volatile String nativeId="";
    private JSONObject clockArm;private long clockAt;private double clockBase;
    ProcedureStudy076(Path project)throws Exception {
        root=project.resolve("procedures-076");
        if(Files.exists(root))try(var dirs=Files.list(root)){for(Path d:dirs.toList())if(Files.exists(d.resolve("study.json"))){
            var j=AgentFiles042.loadChecked(d.resolve("study.json"));UUID.fromString(j.getString("id"));if(!d.getFileName().toString().equals(j.getString("id")))throw new IllegalStateException("Study folder identity differs");
            jobs.put(j.getString("id"),j);if(j.getString("status").equals("running"))j.put("status","interrupted").put("reason","Java ended; Resume retains settled work and charges unfinished attempts conservatively");save(j);
        }}
    }
    static String code(){var o=new JSONObject().put("native",WorldNativeCheckpoint.identity()).put("protocol",WorldFrames.hash(Procedure076.protocol()));for(Class<?> c:new Class<?>[]{ProcedureStudy076.class,Procedure076.class})try(var in=c.getResourceAsStream(c.getSimpleName()+".class")){o.put(c.getSimpleName(),MapDefinition.sha(in.readAllBytes()));}catch(Exception e){throw new IllegalStateException(e);}return WorldFrames.hash(o);}
    private Path dir(String id){UUID.fromString(id);return root.resolve(id);}
    private JSONObject job(String id){var j=jobs.get(id);if(j==null)throw new IllegalArgumentException("Unknown procedure study");return j;}
    private synchronized void save(JSONObject j)throws Exception {if(clockArm!=null)clockArm.put("activeWallMillis",clockBase+(System.nanoTime()-clockAt)/1e6);j.put("updatedAt",Instant.now().toString());AgentFiles042.saveChecked(dir(j.getString("id")).resolve("study.json"),j);published.put(j.getString("id"),WorldFrames.copy(j));}
    synchronized boolean busy(){return !active.isEmpty();}
    synchronized JSONObject read(String id){job(id);return WorldFrames.copy(published.get(id));}
    synchronized JSONArray list(){var a=new JSONArray();for(var j:published.values())a.put(new JSONObject().put("id",j.getString("id")).put("status",j.getString("status")).put("phase",j.optString("phase")).put("progress",j.getInt("progress")).put("total",63));return a;}
    private static JSONObject checked(Path path)throws Exception {var e=AgentFiles042.read(path,32_000_000);var d=e.getJSONObject("data");if(!"java-agent-042-v1".equals(e.getString("format"))||!WorldFrames.hash(d).equals(e.getString("sha256")))throw new IllegalStateException("Original evidence checksum changed");return d;}
    private static void compatible(JSONObject j){if(!j.getString("code").equals(code())||!j.getJSONObject("protocol").similar(Procedure076.protocol()))throw new IllegalStateException("Study code or frozen protocol changed; do not mix native outcomes");}
    private void guard(JSONObject j)throws InterruptedException {if(closed||stops.contains(j.getString("id"))||!j.getString("status").equals("running"))throw new InterruptedException("Procedure study stopped or Java closed");}
    synchronized JSONObject start(JSONObject input)throws Exception {
        SpatialScene.keys(input);if(closed||busy()||jobs.size()>=4)throw new IllegalStateException("Finish active work; history limit4");
        var j=new JSONObject().put("id",UUID.randomUUID().toString()).put("code",code()).put("protocol",Procedure076.protocol()).put("nativeIdentity",WorldNativeCheckpoint.identity()).put("createdAt",Instant.now().toString()).put("status","queued").put("phase","training").put("progress",0).put("resumes",0).put("training",new JSONArray()).put("rows",new JSONArray()).put("bank",new JSONArray());
        for(Object raw:Procedure076.protocol().getJSONArray("training"))j.getJSONArray("training").put(row((JSONObject)raw,new JSONArray(Procedure076.FAMILIES)));
        int i=0;for(Object raw:Procedure076.protocol().getJSONArray("cases")){var order=new JSONArray();for(int k=0;k<4;k++)order.put(Procedure076.METHODS.get((i+k)%4));j.getJSONArray("rows").put(row((JSONObject)raw,order));i++;}
        jobs.put(j.getString("id"),j);save(j);launch(j);return read(j.getString("id"));
    }
    private static JSONObject row(JSONObject spec,JSONArray order){return new JSONObject().put("id",spec.getString("id")).put("split",spec.getString("split")).put("spec",spec).put("specSha256",WorldFrames.hash(spec)).put("methodOrder",order).put("results",new JSONObject());}
    private synchronized void launch(JSONObject j)throws Exception {active=j.getString("id");j.put("status","running");save(j);worker.submit(()->execute(j.getString("id")));}
    synchronized JSONObject resume(String id)throws Exception {var j=job(id);if(closed||busy()||!j.getString("status").equals("interrupted")||j.getInt("resumes")>=2)throw new IllegalStateException("Explicitly resume an interrupted study, at most twice");compatible(j);if(j.has("bankSha256"))bankMetadata(j);j.put("resumes",j.getInt("resumes")+1);launch(j);return read(id);}
    synchronized JSONObject cancel(String id)throws Exception {var j=job(id);if(!Set.of("running","interrupted","queued").contains(j.getString("status")))throw new IllegalStateException("Study already settled");stops.add(id);if(!id.equals(active)){j.remove("evaluation");j.put("status","cancelled").put("reason","Stopped; no qualification");save(j);}if(owner!=null&&!nativeId.isEmpty())owner.cancel(nativeId);return read(id).put("stopRequested",true);}
    private static JSONObject findRow(JSONArray rows,String id){for(Object x:rows)if(((JSONObject)x).getString("id").equals(id))return(JSONObject)x;throw new IllegalArgumentException("Unknown case");}
    private static JSONObject attempt(JSONObject arm,String id){for(Object a:arm.getJSONArray("attempts"))if(((JSONObject)a).getString("id").equals(id))return(JSONObject)a;throw new IllegalStateException("Unowned native attempt");}
    private JSONObject compare(JSONObject j,JSONObject arm,String forecast,String actual)throws Exception {return MemoryPolicy075.comparison(raw(j,attempt(arm,forecast)),raw(j,attempt(arm,actual)));}
    private void checkpoint(JSONObject j,String reason)throws Exception {j.put("reason",reason);save(j);guard(j);}
    private Path attemptDir(JSONObject j,String id){UUID.fromString(id);return dir(j.getString("id")).resolve("attempts").resolve(id);}
    private JSONObject raw(JSONObject j,JSONObject attempt)throws Exception {
        var r=checked(attemptDir(j,attempt.getString("id")).resolve("original.json"));
        if(!WorldFrames.hash(r).equals(attempt.getString("sha256"))||!r.getString("id").equals(attempt.getString("id"))||!r.getString("goalId").equals(j.getString("id")))throw new IllegalStateException("Native attempt identity changed");return r;
    }
    private void settle(JSONObject j,JSONObject arm,JSONObject a,JSONObject run,boolean uncertain)throws Exception {
        int observed=run.has("final")?run.getJSONObject("final").getInt("tick"):run.getJSONArray("samples").isEmpty()?0:run.getJSONArray("samples").getJSONObject(run.getJSONArray("samples").length()-1).getInt("tick");
        if(observed<0||observed>WorldPhysics.MAX_TICKS)throw new IllegalStateException("Native attempt exceeded its original bound");
        a.put("status",run.getString("status")).put("observedTicks",observed).put("chargedTicks",uncertain?WorldPhysics.MAX_TICKS:observed).put("unobservedTailCharged",uncertain).put("sha256",WorldFrames.hash(run)).put("reason",run.optString("reason",""));
        if(run.has("final"))a.put("metrics",WorldVerify.verify(run));
        AgentFiles042.saveChecked(attemptDir(j,a.getString("id")).resolve("original.json"),run);
        recomputeCosts(arm);save(j);
    }
    static void recomputeCosts(JSONObject arm){int ticks=0,observed=0;for(Object raw:arm.getJSONArray("attempts")){var a=(JSONObject)raw;ticks+=a.optInt("chargedTicks");observed+=a.optInt("observedTicks");}arm.put("chargedTicks",ticks).put("observedTicks",observed);}
    private JSONObject trial(JSONObject j,JSONObject arm,String stage,String family,JSONObject scene,JSONObject plan,int reserve)throws Exception {
        return trial(j,arm,stage,family,scene,plan,reserve,false);
    }
    private JSONObject trial(JSONObject j,JSONObject arm,String stage,String family,JSONObject scene,JSONObject plan,int reserve,boolean fresh)throws Exception {
        JSONObject last=null;for(Object raw:arm.getJSONArray("attempts")){var a=(JSONObject)raw;if(a.getString("stage").equals(stage)&&a.getString("family").equals(family))last=a;}
        if(fresh)last=null;
        if(last!=null&&last.getString("status").equals("running")){
            JSONObject run=null;try(var store=new WorldNativeStore(attemptDir(j,last.getString("id")))){if(store.hasRun(last.getString("id")))run=store.run(last.getString("id"));}
            if(run==null){last.put("status","not-started").put("observedTicks",0).put("chargedTicks",0).put("reason","No persisted native registration; execution starts only after registration");save(j);}
            else settle(j,arm,last,run,!Set.of("completed","failed","cancelled").contains(run.getString("status")));
        }
        if(last!=null&&Set.of("completed","failed","cancelled").contains(last.getString("status")))return last;
        guard(j);save(j);if(!Procedure076.admitted(arm.getInt("chargedTicks"),1+reserve/WorldPhysics.MAX_TICKS,arm.getDouble("activeWallMillis"))){arm.put("budgetRefused",true);save(j);return null;}
        String id=UUID.randomUUID().toString();var a=new JSONObject().put("id",id).put("stage",stage).put("family",family).put("sourceSha256",WorldFrames.hash(scene)).put("planSha256",WorldFrames.hash(plan)).put("plannedAt",Instant.now().toString()).put("admissionSpent",arm.getInt("chargedTicks")).put("admissionRemaining",1+reserve/WorldPhysics.MAX_TICKS).put("status","running");
        arm.getJSONArray("attempts").put(a);save(j);long began=System.nanoTime();boolean wallLimit=false;
        try(var store=new WorldNativeStore(attemptDir(j,id))){
            owner=store;nativeId=id;store.start(id,j.getString("id"),scene,plan,false,true);long checkpoint=System.nanoTime();
            while(store.status(id).equals("running")){
                if(closed||stops.contains(j.getString("id"))||!j.getString("status").equals("running")||(System.nanoTime()-began)/1e6>Procedure076.ATTEMPT_MS){wallLimit=(System.nanoTime()-began)/1e6>Procedure076.ATTEMPT_MS;store.cancel(id);}
                Thread.sleep(10);if(System.nanoTime()-checkpoint>2_000_000_000L){save(j);checkpoint=System.nanoTime();}
            }
            var run=store.run(id);a.put("nativeWallMillis",(System.nanoTime()-began)/1e6).put("wallLimit",wallLimit);settle(j,arm,a,run,run.getString("status").equals("interrupted"));
        }finally{owner=null;nativeId="";}
        guard(j);if(a.getString("status").equals("interrupted"))throw new IllegalStateException("Native attempt interrupted: "+a.optString("reason"));return a;
    }
    private static boolean passed(JSONObject a){return a!=null&&a.getString("status").equals("completed")&&a.optJSONObject("metrics",new JSONObject()).optBoolean("passed");}
    private JSONObject program(JSONObject j,JSONObject row,JSONObject arm,String family)throws Exception {
        var programs=arm.getJSONObject("programs");var p=programs.optJSONObject(family);int steps=Procedure076.stages(family);
        if(p!=null&&Set.of("completed","failed","refused").contains(p.getString("status")))return p;
        if(p==null){p=new JSONObject().put("family",family).put("status","running").put("success",false).put("forecast",new JSONArray()).put("actual",new JSONArray()).put("comparisons",new JSONArray());programs.put(family,p);save(j);
            if(!Procedure076.admitted(arm.getInt("chargedTicks"),2*steps,arm.getDouble("activeWallMillis"))){p.put("status","refused").put("reason","Insufficient complete forecast/actual worst-case reserve");return p;}}
        for(String phase:List.of("forecast","actual")){
            JSONObject frame=WorldFrames.frame(Procedure076.scene(row.getJSONObject("spec")));
            for(int i=0;i<steps;i++){
                checkpoint(j,row.getString("id")+" / "+arm.getString("method")+" / "+family+" / "+phase+" step "+(i+1)+" of "+steps);
                JSONObject plan;try{plan=Procedure076.plan(row.getJSONObject("spec"),family,i,frame);if(!plan.getBoolean("passed"))throw new IllegalArgumentException(plan.getString("reason"));}
                catch(IllegalArgumentException e){p.put("status","failed").put("reason",e.getMessage());return p;}
                int remaining=steps-i+(phase.equals("forecast")?steps:0);
                var a=trial(j,arm,phase+"/"+i,family,WorldFrames.scene(frame),plan,(remaining-1)*WorldPhysics.MAX_TICKS);
                if(a==null){p.put("status","refused").put("reason","Remaining stage reserve or active wall budget exhausted");return p;}
                p.getJSONArray(phase).put(i,a.getString("id"));save(j);
                if(!passed(a)){p.put("status","failed").put("reason",a.optString("reason","Native stage did not pass"));return p;}
                var run=raw(j,a);if(!Procedure076.protectedTrace(row.getJSONObject("spec"),run)){p.put("status","failed").put("reason","Whole-task protected-body drift exceeded original limits");return p;}
                frame=WorldFrames.after(frame,run);run=null;
                if(phase.equals("actual")){var c=compare(j,arm,p.getJSONArray("forecast").getString(i),a.getString("id"));p.getJSONArray("comparisons").put(i,c);if(!c.getBoolean("passed")){p.put("status","failed").put("reason","Fresh forecast/actual stage comparison failed");return p;}}
            }
            var result=Procedure076.task(row.getJSONObject("spec"),frame);p.put(phase+"Frame",frame).put(phase+"Task",result);
            if(!result.getBoolean("passed")){p.put("status","failed").put("reason","Whole-task target or protected-body gate failed");return p;}
        }
        return p.put("status","completed").put("success",true).put("reason","All native stages, exact kinematic handoffs and distinct actual execution verified");
    }
    private void arm(JSONObject j,JSONObject row,String method,boolean training)throws Exception {
        var a=row.getJSONObject("results").optJSONObject(method);if(a!=null&&Set.of("completed","refused").contains(a.getString("status")))return;
        if(a==null){a=new JSONObject().put("method",method).put("status","running").put("success",false).put("attempts",new JSONArray()).put("programs",new JSONObject()).put("chargedTicks",0).put("observedTicks",0).put("activeWallMillis",0d);row.getJSONObject("results").put(method,a);}a.put("status","running");
        synchronized(this){clockArm=a;clockAt=System.nanoTime();clockBase=a.getDouble("activeWallMillis");}save(j);
        try{
            var spec=row.getJSONObject("spec");try{Procedure076.supported(spec);}catch(IllegalArgumentException e){a.put("status","refused").put("reason",e.getMessage());return;}
            String preferred="direct";
            if(!training&&method.equals("geometry"))preferred=Procedure076.geometric(spec);
            if(!training&&method.equals("memory")){
                long began=System.nanoTime();bankMetadata(j);var rank=Procedure076.rank(j.getJSONArray("bank"),spec);
                if(rank.isEmpty())a.put("fallback","No compatible verified target roles; fixed-order simple control");
                else{var selected=rank.getJSONObject(0);verifyRecipe(j,selected.getString("id"));preferred=selected.getString("family");a.put("retrieval",new JSONObject().put("selected",selected).put("ranked",rank));}
                a.put("lookupMillis",(System.nanoTime()-began)/1e6);
            }
            var order=training?List.of(method):Procedure076.order(method,preferred);a.put("candidateOrder",new JSONArray(order));save(j);
            for(String family:order){var p=program(j,row,a,family);save(j);if(p.getBoolean("success")){a.put("selected",family).put("success",true);break;}}
            a.put("status","completed").put("reason",a.getBoolean("success")?"Fresh complete procedure verified":"No complete procedure passed within the declared budget");
        }finally{synchronized(this){save(j);clockArm=null;}}
    }
    private void progress(JSONObject j)throws Exception {int n=0;for(String kind:List.of("training","rows"))for(Object raw:j.getJSONArray(kind))for(String m:((JSONObject)raw).getJSONObject("results").keySet())if(Set.of("completed","refused").contains(((JSONObject)raw).getJSONObject("results").getJSONObject(m).getString("status")))n++;j.put("progress",n);save(j);}
    private void freeze(JSONObject j)throws Exception {
        if(j.has("bankSha256")){bankMetadata(j);return;}var entries=new JSONArray();long ticks=0;double wall=0;
        for(Object raw:j.getJSONArray("training")){var row=(JSONObject)raw;for(String family:Procedure076.FAMILIES){var a=row.getJSONObject("results").getJSONObject(family);if(!Set.of("completed","refused").contains(a.getString("status")))throw new IllegalStateException("Cannot freeze unfinished training");ticks+=a.getInt("chargedTicks");wall+=a.getDouble("activeWallMillis");
            if(!a.getBoolean("success"))continue;auditArm(j,row,a,true);String id=UUID.nameUUIDFromBytes((j.getString("id")+":"+row.getString("id")+":"+family).getBytes(StandardCharsets.UTF_8)).toString();
            var receipt=new JSONObject().put("id",id).put("studyId",j.getString("id")).put("trainingRowId",row.getString("id")).put("family",family).put("spec",row.getJSONObject("spec")).put("trainingTicks",a.getInt("chargedTicks")).put("proofSha256",WorldFrames.hash(a)).put("code",j.getString("code")).put("nativeIdentity",j.getString("nativeIdentity"));
            AgentFiles042.saveChecked(dir(j.getString("id")).resolve("bank").resolve(id+".json"),receipt);entries.put(WorldFrames.copy(receipt).put("sha256",WorldFrames.hash(receipt)));
        }}
        j.put("bank",entries).put("bankSha256",WorldFrames.hash(new JSONObject().put("entries",entries))).put("bankFrozenAt",Instant.now().toString()).put("trainingTicks",ticks).put("trainingWallMillis",wall);save(j);
    }
    private void bankMetadata(JSONObject j)throws Exception {if(!j.getString("bankSha256").equals(WorldFrames.hash(new JSONObject().put("entries",j.getJSONArray("bank")))))throw new IllegalStateException("Frozen bank changed");for(Object raw:j.getJSONArray("bank")){var entry=(JSONObject)raw;String id=entry.getString("id");UUID.fromString(id);var packet=checked(dir(j.getString("id")).resolve("bank").resolve(id+".json"));var expected=WorldFrames.copy(entry);expected.remove("sha256");if(!packet.similar(expected)||!WorldFrames.hash(packet).equals(entry.getString("sha256"))||!packet.getString("code").equals(j.getString("code"))||!packet.getString("studyId").equals(j.getString("id"))||!packet.getString("nativeIdentity").equals(WorldNativeCheckpoint.identity()))throw new IllegalStateException("Owned recipe proof/source/identity changed");var row=findRow(j.getJSONArray("training"),entry.getString("trainingRowId"));var a=row.getJSONObject("results").getJSONObject(entry.getString("family"));if(!row.getJSONObject("spec").similar(packet.getJSONObject("spec"))||!a.getBoolean("success")||a.getInt("chargedTicks")!=packet.getInt("trainingTicks")||!WorldFrames.hash(a).equals(packet.getString("proofSha256")))throw new IllegalStateException("Recipe is not the frozen verified training outcome");}}
    private void verifyRecipe(JSONObject j,String id)throws Exception {bankMetadata(j);for(Object raw:j.getJSONArray("bank")){var b=(JSONObject)raw;if(b.getString("id").equals(id)){var r=findRow(j.getJSONArray("training"),b.getString("trainingRowId"));auditArm(j,r,r.getJSONObject("results").getJSONObject(b.getString("family")),true);return;}}throw new IllegalStateException("Recipe outside owned frozen bank");}
    private void execute(String id){var j=job(id);try{compatible(j);j.put("phase","training");for(Object raw:j.getJSONArray("training")){var row=(JSONObject)raw;for(String family:Procedure076.FAMILIES){guard(j);arm(j,row,family,true);progress(j);}}checkpoint(j,"Verifying and freezing training-only procedures");freeze(j);j.put("phase","evaluation");save(j);for(Object raw:j.getJSONArray("rows")){var row=(JSONObject)raw;for(Object m:row.getJSONArray("methodOrder")){guard(j);arm(j,row,m.toString(),false);progress(j);}}j.put("phase","audit").put("auditProgress",0);save(j);long at=System.nanoTime();audit(id);guard(j);j.put("auditMillis",(System.nanoTime()-at)/1e6).put("evaluation",Procedure076.evaluation(j.getJSONArray("rows"))).put("status","completed").put("reason","Training bank frozen; all comparisons and failures retained; no promotion");save(j);}catch(Throwable e){synchronized(this){clockArm=null;try{j.remove("evaluation");j.put("status",stops.contains(id)?"cancelled":"interrupted").put("reason",e.toString());save(j);}catch(Throwable save){System.err.println("Procedure worker failed: "+e+"; persistence: "+save);}}}finally{synchronized(this){clockArm=null;active="";}}}
    private void auditArm(JSONObject j,JSONObject row,JSONObject arm,boolean training)throws Exception {
        var spec=row.getJSONObject("spec");if(!row.getString("specSha256").equals(WorldFrames.hash(spec)))throw new IllegalStateException("Case specification changed");
        boolean supported=true;try{Procedure076.supported(spec);}catch(IllegalArgumentException e){supported=false;}
        if(!supported){if(!arm.getString("status").equals("refused")||!arm.getJSONArray("attempts").isEmpty()||arm.getBoolean("success")||arm.getInt("chargedTicks")!=0||arm.getInt("observedTicks")!=0)throw new IllegalStateException("Unsupported task acquired action authority");return;}
        String method=arm.getString("method"),preferred="direct";
        if(!training&&method.equals("geometry"))preferred=Procedure076.geometric(spec);
        if(!training&&method.equals("memory")){var rank=Procedure076.rank(j.getJSONArray("bank"),spec);if(rank.isEmpty()){if(arm.has("retrieval")||!arm.has("fallback"))throw new IllegalStateException("Invalid fallback");}else{var r=arm.getJSONObject("retrieval");if(!rank.similar(r.getJSONArray("ranked"))||!rank.getJSONObject(0).similar(r.getJSONObject("selected")))throw new IllegalStateException("Recipe ranking changed");preferred=rank.getJSONObject(0).getString("family");}}
        else if(arm.has("retrieval"))throw new IllegalStateException("Control accessed memory");
        var order=training?List.of(method):Procedure076.order(method,preferred);if(!new JSONArray(order).similar(arm.getJSONArray("candidateOrder")))throw new IllegalStateException("Procedure ordering changed");
        int ticks=0,observed=0,previous=-1;var seen=new HashSet<String>();
        for(Object raw:arm.getJSONArray("attempts")){var a=(JSONObject)raw;int n=order.indexOf(a.getString("family"));if(n<0||n<previous)throw new IllegalStateException("Attempt order changed");previous=n;if(!seen.add(a.getString("id")))throw new IllegalStateException("Duplicate attempt");if(!a.has("sha256")){if(!a.getString("status").equals("not-started")||a.getInt("chargedTicks")!=0)throw new IllegalStateException("Missing proof");continue;}
            var run=raw(j,a);if(!training&&Instant.parse(a.getString("plannedAt")).isBefore(Instant.parse(j.getString("bankFrozenAt"))))throw new IllegalStateException("Evaluation preceded frozen training bank");int tick=run.has("final")?run.getJSONObject("final").getInt("tick"):run.getJSONArray("samples").isEmpty()?0:run.getJSONArray("samples").getJSONObject(run.getJSONArray("samples").length()-1).getInt("tick");
            if(a.getInt("admissionSpent")!=ticks||a.getInt("admissionRemaining")<1||!Procedure076.admitted(ticks,a.getInt("admissionRemaining"),0)||a.getInt("observedTicks")!=tick||a.getInt("chargedTicks")!=(a.getBoolean("unobservedTailCharged")?WorldPhysics.MAX_TICKS:tick))throw new IllegalStateException("Hidden native work or invalid stage admission");
            ticks+=a.getInt("chargedTicks");observed+=tick;if(!run.getString("status").equals(a.getString("status"))||Instant.parse(run.getString("createdAt")).isBefore(Instant.parse(a.getString("plannedAt"))))throw new IllegalStateException("Native status or chronology changed");
            if(run.has("final")&&!WorldVerify.verify(run).similar(a.getJSONObject("metrics")))throw new IllegalStateException("Native metrics changed");
        }
        if(ticks!=arm.getInt("chargedTicks")||observed!=arm.getInt("observedTicks")||ticks>Procedure076.BUDGET)throw new IllegalStateException("Invalid arm work");
        boolean success=false;String selected="";var bound=new HashSet<String>();boolean missing=false;
        for(String family:order){var p=arm.getJSONObject("programs").optJSONObject(family);if(p==null){missing=true;continue;}if(missing||success)throw new IllegalStateException("Procedure skipped or continued after success");int steps=Procedure076.stages(family);boolean[] complete={false,false};int phaseIndex=0;
            for(String phase:List.of("forecast","actual")){
                JSONObject frame=WorldFrames.frame(Procedure076.scene(spec));var ids=p.getJSONArray(phase);if(ids.length()>steps||phase.equals("actual")&&!complete[0]&&!ids.isEmpty())throw new IllegalStateException("Actual before complete forecast");boolean valid=true;int executed=0;
                for(int i=0;i<steps;i++){
                    String key=phase+"/"+i;JSONObject expected;try{expected=Procedure076.plan(spec,family,i,frame);}catch(IllegalArgumentException rejected){boolean hasAttempt=false;for(Object x:arm.getJSONArray("attempts")){var t=(JSONObject)x;if(t.getString("family").equals(family)&&t.getString("stage").equals(key))hasAttempt=true;}if(hasAttempt||i<ids.length())throw rejected;valid=false;break;}
                    for(Object raw:arm.getJSONArray("attempts")){var a=(JSONObject)raw;if(!a.getString("family").equals(family)||!a.getString("stage").equals(key))continue;bound.add(a.getString("id"));if(a.getInt("admissionRemaining")!=steps-i+(phase.equals("forecast")?steps:0))throw new IllegalStateException("Changed fresh actual reserve");if(!a.has("sha256"))continue;var run=raw(j,a);if(!expected.getBoolean("passed")||!run.getJSONObject("definition").similar(WorldFrames.scene(frame))||!run.getJSONObject("plan").similar(expected)||!a.getString("sourceSha256").equals(WorldFrames.hash(run.getJSONObject("definition")))||!a.getString("planSha256").equals(WorldFrames.hash(expected)))throw new IllegalStateException("Procedure or handoff substituted");}
                    if(i>=ids.length())break;var a=attempt(arm,ids.getString(i));if(!a.getString("stage").equals(key)||!a.getString("family").equals(family))throw new IllegalStateException("Wrong stage reference");if(!passed(a)){valid=false;break;}
                    var run=raw(j,a);valid&=Procedure076.protectedTrace(spec,run);if(!valid)break;frame=WorldFrames.after(frame,run);run=null;executed++;
                    if(phase.equals("actual")){var c=compare(j,arm,p.getJSONArray("forecast").getString(i),a.getString("id"));if(!c.similar(p.getJSONArray("comparisons").getJSONObject(i)))throw new IllegalStateException("Forecast comparison changed");valid&=c.getBoolean("passed");}
                    if(!valid)break;
                }
                complete[phaseIndex]=valid&&executed==steps&&Procedure076.task(spec,frame).getBoolean("passed");
                if(p.has(phase+"Frame")&&(!p.getJSONObject(phase+"Frame").similar(frame)||!p.getJSONObject(phase+"Task").similar(Procedure076.task(spec,frame))))throw new IllegalStateException("Final task frame changed");phaseIndex++;
            }
            boolean passed=complete[0]&&complete[1];if(p.getBoolean("success")!=passed||passed&&!p.getString("status").equals("completed"))throw new IllegalStateException("False procedure success");if(passed){success=true;selected=family;}
        }
        if(bound.size()!=arm.getJSONArray("attempts").length()||arm.getBoolean("success")!=success||!arm.optString("selected").equals(selected))throw new IllegalStateException("Unbound action or wrong selected outcome");
    }
    JSONObject audit(String id)throws Exception {
        var j=read(id);compatible(j);bankMetadata(j);int count=0;var globalAttempts=new HashSet<String>();
        for(String kind:List.of("training","rows")){
            var expected=Procedure076.protocol().getJSONArray(kind.equals("training")?"training":"cases");var rows=j.getJSONArray(kind);if(rows.length()!=expected.length())throw new IllegalStateException("Missing declared case");
            for(int i=0;i<rows.length();i++){var r=rows.getJSONObject(i);if(!r.getJSONObject("spec").similar(expected.getJSONObject(i))||!r.getString("id").equals(r.getJSONObject("spec").getString("id"))||!r.getString("split").equals(r.getJSONObject("spec").getString("split")))throw new IllegalStateException("Frozen case changed");var order=kind.equals("training")?Procedure076.FAMILIES:Procedure076.METHODS;var frozenOrder=new JSONArray();for(int k=0;k<order.size();k++)frozenOrder.put(order.get(kind.equals("training")?k:(i+k)%4));if(!frozenOrder.similar(r.getJSONArray("methodOrder")))throw new IllegalStateException("Counterbalanced order changed");if(r.getJSONObject("results").length()!=order.size())throw new IllegalStateException("Missing comparison arm");for(String method:order){if(id.equals(active))guard(job(id));var arm=r.getJSONObject("results").getJSONObject(method);if(!arm.getString("method").equals(method)||!Set.of("completed","refused").contains(arm.getString("status")))throw new IllegalStateException("Unsettled or relabelled arm");for(Object raw:arm.getJSONArray("attempts"))if(!globalAttempts.add(((JSONObject)raw).getString("id")))throw new IllegalStateException("Native attempt reused across arms");auditArm(j,r,arm,kind.equals("training"));count++;if(id.equals(active)){job(id).put("auditProgress",count);save(job(id));}}}
        }
        if(count!=63||j.getInt("progress")!=63)throw new IllegalStateException("Incomplete comparison");long ticks=0;double wall=0;for(Object raw:j.getJSONArray("training"))for(String f:Procedure076.FAMILIES){var a=((JSONObject)raw).getJSONObject("results").getJSONObject(f);ticks+=a.getInt("chargedTicks");wall+=a.getDouble("activeWallMillis");}if(ticks!=j.getLong("trainingTicks")||Math.abs(wall-j.getDouble("trainingWallMillis"))>1e-6)throw new IllegalStateException("Training costs changed");
        return new JSONObject().put("passed",true).put("arms",count).put("nativeIdentity",WorldNativeCheckpoint.identity()).put("trainingTicks",ticks).put("bankSize",j.getJSONArray("bank").length());
    }
    JSONObject trace(String id,String caseId,String method)throws Exception {var j=read(id);var row=findRow(caseId.startsWith("train-")?j.getJSONArray("training"):j.getJSONArray("rows"),caseId);var a=row.getJSONObject("results").getJSONObject(method);var runs=new JSONArray();for(Object raw:a.getJSONArray("attempts")){var t=(JSONObject)raw;if(!t.has("sha256"))continue;var r=raw(j,t);var samples=new JSONArray();for(Object v:r.getJSONArray("samples")){var f=(JSONObject)v;samples.put(new JSONObject().put("tick",f.getInt("tick")).put("bodies",f.getJSONArray("bodies")));}runs.put(new JSONObject().put("id",r.getString("id")).put("stage",t.getString("stage")).put("family",t.getString("family")).put("status",r.getString("status")).put("definition",r.getJSONObject("definition")).put("samples",samples));}return new JSONObject().put("runs",runs);}
    JSONObject exportCase(String id,String caseId)throws Exception {JSONObject j;synchronized(this){if(busy())throw new IllegalStateException("Wait for or Stop the study before export");j=read(id);}var row=findRow(caseId.startsWith("train-")?j.getJSONArray("training"):j.getJSONArray("rows"),caseId);var originals=new JSONArray();for(String m:row.getJSONObject("results").keySet())for(Object raw:row.getJSONObject("results").getJSONObject(m).getJSONArray("attempts")){var a=(JSONObject)raw;if(a.has("sha256"))originals.put(new JSONObject().put("method",m).put("attempt",a).put("run",MemoryStudy075.packed(raw(j,a))));}return new JSONObject().put("format","verified-procedures-076-v1").put("studyId",id).put("code",j.getString("code")).put("row",row).put("bank",j.getJSONArray("bank")).put("bankSha256",j.optString("bankSha256")).put("originals",originals);}
    @Override public void close(){closed=true;worker.shutdown();try{worker.awaitTermination(10,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
}
