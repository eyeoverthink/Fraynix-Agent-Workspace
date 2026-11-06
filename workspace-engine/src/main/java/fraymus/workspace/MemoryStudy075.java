package fraymus.workspace;

import org.json.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/** Isolated native experiments with a frozen owned bank and durable, fully charged attempts. */
final class MemoryStudy075 implements AutoCloseable {
    @FunctionalInterface interface Episodes { JSONObject read(String id)throws Exception; }
    private final Path root;private final Episodes episodes;
    private final Map<String,JSONObject> jobs=new LinkedHashMap<>();
    private final Map<String,JSONObject> published=new LinkedHashMap<>();
    private final Set<String> stops=ConcurrentHashMap.newKeySet();
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"memory-usefulness-075");t.setDaemon(true);return t;});
    private volatile boolean closed;private volatile String active="";private volatile WorldNativeStore owner;private volatile String nativeId="";
    private JSONObject clockArm;private long clockAt;private double clockBase;
    MemoryStudy075(Path project,Episodes episodes)throws Exception {
        root=project.resolve("memory-075");this.episodes=episodes;
        if(Files.exists(root))try(var dirs=Files.list(root)){for(Path d:dirs.toList())if(Files.exists(d.resolve("study.json"))){
            var j=AgentFiles042.loadChecked(d.resolve("study.json"));UUID.fromString(j.getString("id"));if(!d.getFileName().toString().equals(j.getString("id")))throw new IllegalStateException("Study folder identity differs");
            jobs.put(j.getString("id"),j);if(j.getString("status").equals("running"))j.put("status","interrupted").put("reason","Java ended; Resume retains settled work and charges unfinished attempts conservatively");save(j);
        }}
    }
    static String code(){
        var o=new JSONObject().put("native",WorldNativeCheckpoint.identity()).put("protocol",WorldFrames.hash(MemoryPolicy075.protocol()));
        for(Class<?> c:new Class<?>[]{MemoryStudy075.class,MemoryPolicy075.class,RebuildCertificate075.class})try(var in=c.getResourceAsStream(c.getSimpleName()+".class")){o.put(c.getSimpleName(),MapDefinition.sha(in.readAllBytes()));}catch(Exception e){throw new IllegalStateException(e);}return WorldFrames.hash(o);
    }
    private Path dir(String id){UUID.fromString(id);return root.resolve(id);}
    private JSONObject job(String id){var j=jobs.get(id);if(j==null)throw new IllegalArgumentException("Unknown memory comparison");return j;}
    private synchronized void save(JSONObject j)throws Exception {
        if(clockArm!=null)clockArm.put("activeWallMillis",clockBase+(System.nanoTime()-clockAt)/1e6);
        j.put("updatedAt",Instant.now().toString());AgentFiles042.saveChecked(dir(j.getString("id")).resolve("study.json"),j);
        published.put(j.getString("id"),WorldFrames.copy(j));
    }
    synchronized boolean busy(){return !active.isEmpty();}
    synchronized JSONArray list(){var a=new JSONArray();for(var j:published.values())a.put(new JSONObject().put("id",j.getString("id")).put("status",j.getString("status")).put("progress",j.getInt("progress")).put("total",64));return a;}
    synchronized JSONObject read(String id){job(id);return WorldFrames.copy(published.get(id));}
    private static JSONObject checked(Path p)throws Exception {var e=AgentFiles042.read(p,32_000_000);var d=e.getJSONObject("data");if(!WorldFrames.hash(d).equals(e.getString("sha256")))throw new IllegalStateException("Original evidence checksum changed");return d;}
    private JSONObject bank(JSONObject j,String id)throws Exception {
        JSONObject entry=null;for(Object raw:j.getJSONArray("bank"))if(((JSONObject)raw).getString("id").equals(id))entry=(JSONObject)raw;
        if(entry==null)throw new IllegalArgumentException("Episode outside the frozen bank");
        var b=checked(dir(j.getString("id")).resolve("bank").resolve(id+".json"));
        if(!WorldFrames.hash(b).equals(entry.getString("sha256"))||!b.getJSONObject("episode").getString("id").equals(id)||!MemoryPolicy075.FAMILIES.contains(b.getString("preferred")))throw new IllegalStateException("Frozen memory changed");
        var e=b.getJSONObject("episode");Rehearsal073.rest(e.getJSONObject("run"));if(!WorldNativeCheckpoint.identity().equals(e.getString("nativeIdentity"))||!e.getString("runSha256").equals(WorldFrames.hash(e.getJSONObject("run"))))throw new IllegalStateException("Memory native identity differs");return b;
    }
    synchronized JSONObject start(JSONObject input)throws Exception {
        SpatialScene.keys(input,"episodeIds");if(closed||busy()||jobs.size()>=4)throw new IllegalStateException("Finish the current comparison; study history limit4");
        var ids=input.getJSONArray("episodeIds");if(ids.isEmpty()||ids.length()>8)throw new IllegalArgumentException("Select1..8 verified owned memories");
        long started=System.nanoTime();var unique=new TreeSet<String>();for(Object id:ids)if(!(id instanceof String)||!unique.add((String)id))throw new IllegalArgumentException("Distinct episode IDs required");
        var bundles=new ArrayList<JSONObject>();for(String id:unique)bundles.add(episodes.read(id));
        var j=new JSONObject().put("id",UUID.randomUUID().toString()).put("version","memory-study-075-v1").put("code",code()).put("protocol",MemoryPolicy075.protocol()).put("createdAt",Instant.now().toString()).put("bankFrozenAt",Instant.now().toString()).put("bank",new JSONArray()).put("rows",new JSONArray()).put("status","queued").put("progress",0).put("resumes",0);
        long creationTicks=0;for(var b:bundles){var e=b.getJSONObject("episode");String id=e.getString("id");AgentFiles042.saveChecked(dir(j.getString("id")).resolve("bank").resolve(id+".json"),b);j.getJSONArray("bank").put(new JSONObject().put("id",id).put("sha256",WorldFrames.hash(b)).put("preferred",b.getString("preferred")).put("sourceGoalId",e.getString("sourceGoalId")));
            creationTicks+=e.getJSONObject("run").getJSONObject("final").getLong("tick");if(e.has("lineage")){var l=e.getJSONObject("lineage");creationTicks+=l.getJSONObject("costs").getLong("totalTicks");for(Object raw:l.getJSONArray("rebuildAttempts")){var a=(JSONObject)raw;creationTicks+=a.has("comparison")?a.getJSONObject("comparison").getLong("ticks"):WorldPhysics.MAX_TICKS;}}
        }
        j.put("bankConstruction",new JSONObject().put("attributableTicks",creationTicks).put("completeHistoricalCostKnown",false).put("historicalWallMillis",JSONObject.NULL).put("scope","Retained source executions and their recorded rebuild/alternative attempts; other historic collection failures and wall time are unavailable, not zero."));
        int index=0;for(Object raw:MemoryPolicy075.protocol().getJSONArray("cases")){var spec=(JSONObject)raw;var order=new JSONArray();for(int m=0;m<4;m++)order.put(MemoryPolicy075.METHODS.get((index+m)%4));j.getJSONArray("rows").put(new JSONObject().put("id",spec.getString("id")).put("split",spec.getString("split")).put("spec",spec).put("specSha256",WorldFrames.hash(spec)).put("methodOrder",order).put("results",new JSONObject()));index++;}
        j.put("bankPreparationMillis",(System.nanoTime()-started)/1e6);jobs.put(j.getString("id"),j);save(j);launch(j);return read(j.getString("id"));
    }
    private synchronized void launch(JSONObject j)throws Exception {active=j.getString("id");j.put("status","running");save(j);worker.submit(()->execute(j.getString("id")));}
    synchronized JSONObject resume(String id)throws Exception {
        var j=job(id);if(closed||busy()||!j.getString("status").equals("interrupted")||j.getInt("resumes")>=2)throw new IllegalStateException("Only an interrupted study can explicitly resume, at most twice");
        compatible(j);for(Object raw:j.getJSONArray("bank")){var b=(JSONObject)raw;if(!WorldFrames.hash(episodes.read(b.getString("id"))).equals(b.getString("sha256")))throw new IllegalStateException("Owned source episode changed since freezing");}
        j.put("resumes",j.getInt("resumes")+1);launch(j);return read(id);
    }
    synchronized JSONObject cancel(String id)throws Exception {var j=job(id);if(!Set.of("running","interrupted","queued").contains(j.getString("status")))throw new IllegalStateException("Study already settled");stops.add(id);if(!id.equals(active)){j.put("status","cancelled").put("reason","Stopped; incomplete comparisons cannot qualify");save(j);}if(owner!=null&&!nativeId.isEmpty())owner.cancel(nativeId);return read(id).put("stopRequested",true);}
    private void guard(JSONObject j)throws InterruptedException {if(closed||stops.contains(j.getString("id"))||!j.getString("status").equals("running"))throw new InterruptedException("Comparison stopped or Java closed");}
    private static void compatible(JSONObject j){if(!j.getString("code").equals(code())||!j.getJSONObject("protocol").similar(MemoryPolicy075.protocol()))throw new IllegalStateException("Study implementation or frozen protocol changed");}
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
        guard(j);save(j);if(!MemoryPolicy075.admitted(arm.getInt("chargedTicks"),reserve,arm.getDouble("activeWallMillis"))){arm.put("budgetRefused",true);save(j);return null;}
        String id=UUID.randomUUID().toString();var a=new JSONObject().put("id",id).put("stage",stage).put("family",family).put("sourceSha256",WorldFrames.hash(scene)).put("planSha256",WorldFrames.hash(plan)).put("plannedAt",Instant.now().toString()).put("status","running");
        arm.getJSONArray("attempts").put(a);save(j);long began=System.nanoTime();boolean wallLimit=false;
        try(var store=new WorldNativeStore(attemptDir(j,id))){
            owner=store;nativeId=id;store.start(id,j.getString("id"),scene,plan,false,true);long checkpoint=System.nanoTime();
            while(store.status(id).equals("running")){
                if(closed||stops.contains(j.getString("id"))||!j.getString("status").equals("running")||(System.nanoTime()-began)/1e6>MemoryPolicy075.ATTEMPT_MS){wallLimit=(System.nanoTime()-began)/1e6>MemoryPolicy075.ATTEMPT_MS;store.cancel(id);}
                Thread.sleep(10);if(System.nanoTime()-checkpoint>2_000_000_000L){save(j);checkpoint=System.nanoTime();}
            }
            var run=store.run(id);a.put("nativeWallMillis",(System.nanoTime()-began)/1e6).put("wallLimit",wallLimit);settle(j,arm,a,run,run.getString("status").equals("interrupted"));
        }finally{owner=null;nativeId="";}
        guard(j);if(a.getString("status").equals("interrupted"))throw new IllegalStateException("Native attempt interrupted: "+a.optString("reason"));return a;
    }
    private static boolean passed(JSONObject a){return a!=null&&a.getString("status").equals("completed")&&a.optJSONObject("metrics",new JSONObject()).optBoolean("passed");}
    private void arm(JSONObject j,JSONObject row,String method)throws Exception {
        var results=row.getJSONObject("results");var arm=results.optJSONObject(method);
        if(arm!=null&&Set.of("completed","refused").contains(arm.getString("status")))return;
        if(arm==null){arm=new JSONObject().put("status","running").put("method",method).put("attempts",new JSONArray()).put("chargedTicks",0).put("observedTicks",0).put("activeWallMillis",0d).put("success",false);results.put(method,arm);}
        arm.put("status","running"); // Explicit resume re-enters the durable interrupted arm.
        synchronized(this){clockArm=arm;clockBase=arm.getDouble("activeWallMillis");clockAt=System.nanoTime();}save(j);
        try {
            var spec=row.getJSONObject("spec");JSONObject scene;JSONArray candidates;long planning=System.nanoTime();
            try{scene=MemoryPolicy075.scene(spec);candidates=MemoryPolicy075.candidates(spec,scene);if(candidates.toList().stream().noneMatch(o->"queued".equals(((Map<?,?>)o).get("status"))))throw new IllegalArgumentException("No original candidate has a clear admissible route");}
            catch(IllegalArgumentException e){arm.put("status","refused").put("reason",e.getMessage());return;}
            arm.put("planningMillis",arm.optDouble("planningMillis",0)+(System.nanoTime()-planning)/1e6).put("sourceSha256",WorldFrames.hash(scene));
            String preferred=MemoryPolicy075.FAMILIES.get(0);
            if(method.equals("memory")||method.equals("cached")){
                if(!arm.has("retrieval")){
                    long start=System.nanoTime();var bank=new ArrayList<JSONObject>();for(Object raw:j.getJSONArray("bank"))bank.add(bank(j,((JSONObject)raw).getString("id")));
                    var ranked=MemoryPolicy075.rank(bank,spec,scene);if(ranked.isEmpty()){arm.put("retrievalMissing",true).put("retrievalMillis",(System.nanoTime()-start)/1e6).put("fallback","simple: no compatible target roles in the frozen bank");}else
                    arm.put("retrieval",new JSONObject().put("ranked",ranked).put("selected",ranked.getJSONObject(0)).put("millis",(System.nanoTime()-start)/1e6).put("selectedAt",Instant.now().toString()));save(j);
                }
                if(arm.has("retrieval")){var choice=arm.getJSONObject("retrieval").getJSONObject("selected");preferred=choice.getString("preferred");var e=bank(j,choice.getString("id")).getJSONObject("episode");var original=e.getJSONObject("run");
                var certificates=new RebuildCertificate075(dir(j.getString("id")).resolve("certificates"));JSONObject cached=null;
                if(method.equals("cached")){
                    cached=certificates.lookup(j,bank(j,choice.getString("id")),a->raw(j,a));
                    if(!arm.has("cacheChecks"))arm.put("cacheChecks",new JSONArray());
                    arm.getJSONArray("cacheChecks").put(RebuildCertificate075.receipt(cached));arm.put("cache",RebuildCertificate075.receipt(cached));
                }
                if(cached!=null&&cached.getString("status").equals("hit")){
                    // A resumed origin task keeps its original miss cost; a later hit links the same charged origin.
                    arm.put("certificate",cached.getJSONObject("certificate")).put("reconstruction",cached.getJSONObject("certificate").getJSONObject("comparison"));
                }else{
                    var rebuild=trial(j,arm,"rebuild","original-memory",original.getJSONObject("definition"),original.getJSONObject("plan"),WorldPhysics.MAX_TICKS,cached!=null&&arm.has("certificate"));
                    if(!passed(rebuild)){arm.put("reason","Reconstruction failed or shared budget exhausted");return;}
                    var comparison=Rehearsal073.comparison(original,raw(j,rebuild));arm.put("reconstruction",comparison);if(!comparison.getBoolean("passed")){arm.put("reason","Original reconstruction outside declared073 gates");return;}
                    if(method.equals("cached")){long began=System.nanoTime();var c=certificates.install(j,bank(j,choice.getString("id")),rebuild,a->raw(j,a));arm.put("certificate",c).put("certificateInstallMillis",(System.nanoTime()-began)/1e6);}
                }
                }
            }
            arm.put("candidateOrder",new JSONArray(MemoryPolicy075.order(method,preferred))).put("candidates",candidates);save(j);
            var completed=new JSONArray();
            for(String family:MemoryPolicy075.order(method,preferred)){
                JSONObject c=null;for(Object raw:candidates)if(((JSONObject)raw).getString("id").equals(family))c=(JSONObject)raw;
                if(!c.getString("status").equals("queued"))continue;
                var a=trial(j,arm,"forecast",family,scene,c.getJSONObject("plan"),WorldPhysics.MAX_TICKS);if(a==null)break;
                c.put("status",a.getString("status")).put("runId",a.getString("id")).put("metrics",a.optJSONObject("metrics",new JSONObject().put("passed",false)));
                if(passed(a)){completed.put(c);if(!method.equals("search"))break;}
            }
            var chosen=Counterfactual045.choose(completed);if(chosen==null){arm.put("reason","No verified forecast within budget");return;}
            arm.put("selected",chosen.getString("id")).put("forecastRunId",chosen.getString("runId"));save(j);
            var actual=trial(j,arm,"actual",chosen.getString("id"),scene,chosen.getJSONObject("plan"),0);if(!passed(actual)){arm.put("reason","Fresh actual execution failed or budget refused");return;}
            JSONObject forecastAttempt=null;for(Object raw:arm.getJSONArray("attempts"))if(((JSONObject)raw).getString("id").equals(chosen.getString("runId")))forecastAttempt=(JSONObject)raw;
            var comparison=MemoryPolicy075.comparison(raw(j,forecastAttempt),raw(j,actual));arm.put("actualRunId",actual.getString("id")).put("comparison",comparison).put("success",comparison.getBoolean("passed")).put("reason",comparison.getBoolean("passed")?"Fresh native motion and preserved gates verified":"Forecast/actual comparison failed");
        }catch(Exception e){arm.put("status","interrupted");throw e;}finally {
            synchronized(this){if(j.getString("status").equals("running")&&!closed&&arm.getString("status").equals("running"))arm.put("status","completed");save(j);clockArm=null;}
        }
    }
    private void execute(String id){
        var j=job(id);try {
            compatible(j);for(Object raw:j.getJSONArray("rows")){var row=(JSONObject)raw;for(Object m:row.getJSONArray("methodOrder")){guard(j);arm(j,row,m.toString());int n=0;for(Object rr:j.getJSONArray("rows"))for(String key:((JSONObject)rr).getJSONObject("results").keySet())if(Set.of("completed","refused").contains(((JSONObject)rr).getJSONObject("results").getJSONObject(key).getString("status")))n++;j.put("progress",n);save(j);}}
            guard(j);beginAudit(j);long auditAt=System.nanoTime();audit(id);guard(j);j.put("evaluation",MemoryPolicy075.evaluation(j.getJSONArray("rows"))).put("finalAuditMillis",(System.nanoTime()-auditAt)/1e6).put("status","completed").put("reason","All frozen comparisons retained; no automatic policy promotion");save(j);
        }catch(Throwable e){failed(j,e);}
        finally{synchronized(this){clockArm=null;active="";}}
    }
    private synchronized void failed(JSONObject j,Throwable e){
        clockArm=null;try{j.remove("evaluation");j.put("status",stops.contains(j.getString("id"))?"cancelled":"interrupted").put("reason",e.toString());save(j);}catch(Throwable persist){System.err.println("Comparison worker failed: "+e+"; persistence failed: "+persist);}
    }
    private void beginAudit(JSONObject j)throws Exception {
        int total=0;for(Object r:j.getJSONArray("rows"))for(String m:((JSONObject)r).getJSONObject("results").keySet())for(Object a:((JSONObject)r).getJSONObject("results").getJSONObject(m).getJSONArray("attempts"))if(((JSONObject)a).has("sha256"))total++;
        j.remove("evaluation");j.put("auditProgress",0).put("auditTotal",total).put("reason","Checking original traces and certificates");save(j);
    }
    static String evidenceDigest(JSONObject j){return WorldFrames.hash(new JSONObject().put("bank",j.getJSONArray("bank")).put("rows",j.getJSONArray("rows")).put("protocol",j.getJSONObject("protocol")));}
    static void completeEvidence(JSONObject j){
        if(j.getInt("progress")!=64||j.getJSONArray("rows").length()!=16||!j.getJSONObject("protocol").similar(MemoryPolicy075.protocol()))throw new IllegalStateException("Recheck requires every frozen cell to be settled");
        for(Object r:j.getJSONArray("rows")){var results=((JSONObject)r).getJSONObject("results");if(results.length()!=4)throw new IllegalStateException("Missing method");for(String m:MemoryPolicy075.METHODS)if(!Set.of("completed","refused").contains(results.getJSONObject(m).getString("status")))throw new IllegalStateException("Recheck cannot execute an unfinished native task");}
    }
    private static void compatibleAudit(JSONObject j){
        if(j.getString("code").equals(code())){compatible(j);return;}
        completeEvidence(j);
        if(!j.optString("auditCode").equals(code())||!j.optString("auditEvidence").equals(evidenceDigest(j)))throw new IllegalStateException("Changed implementation requires an explicit proof-only recheck; native attempts cannot resume");
        var entries=j.getJSONArray("rechecks");var last=entries.getJSONObject(entries.length()-1);if(!last.getString("originalCode").equals(j.getString("code"))||!last.getString("checkingCode").equals(code())||!last.getString("evidenceSha256").equals(evidenceDigest(j)))throw new IllegalStateException("Recheck provenance changed");
    }
    synchronized JSONObject recheck(String id)throws Exception {
        var j=job(id);if(closed||busy())throw new IllegalStateException("Finish active work before rechecking saved evidence");completeEvidence(j);
        var history=j.optJSONArray("rechecks");if(history==null){history=new JSONArray();j.put("rechecks",history);}if(history.length()>=3)throw new IllegalStateException("Recheck history limit3");
        for(Object raw:j.getJSONArray("bank")){var b=(JSONObject)raw;if(!WorldFrames.hash(episodes.read(b.getString("id"))).equals(b.getString("sha256")))throw new IllegalStateException("Owned bank changed; recheck refused");}
        String revision=UUID.randomUUID().toString(),digest=evidenceDigest(j);AgentFiles042.saveChecked(dir(id).resolve("rechecks").resolve(revision+"-before.json"),j);
        history.put(new JSONObject().put("id",revision).put("createdAt",Instant.now().toString()).put("originalCode",j.getString("code")).put("checkingCode",code()).put("evidenceSha256",digest).put("scope","Proof-only; no native execution, changed outcome, source, authority or memory ingestion").put("status","checking"));
        j.put("auditCode",code()).put("auditEvidence",digest).put("status","running");stops.remove(id);active=id;beginAudit(j);
        worker.submit(()->{try{long at=System.nanoTime();audit(id);guard(j);if(!evidenceDigest(j).equals(digest))throw new IllegalStateException("Recheck changed original evidence");j.getJSONArray("rechecks").getJSONObject(j.getJSONArray("rechecks").length()-1).put("status","passed");j.put("evaluation",MemoryPolicy075.evaluation(j.getJSONArray("rows"))).put("finalAuditMillis",(System.nanoTime()-at)/1e6).put("status","completed").put("reason","Original immutable attempts rechecked; no native tasks rerun");save(j);}catch(Throwable e){failed(j,e);}finally{synchronized(this){active="";}}});return read(id);
    }
    private JSONArray auditRank(JSONObject j,JSONObject spec,JSONObject scene)throws Exception {
        var values=new ArrayList<JSONObject>();for(Object entry:j.getJSONArray("bank")){var one=MemoryPolicy075.rank(List.of(bank(j,((JSONObject)entry).getString("id"))),spec,scene);for(Object value:one)values.add((JSONObject)value);}
        values.sort(Comparator.comparingDouble((JSONObject x)->x.getDouble("distance")).thenComparing(x->x.getString("id")));return new JSONArray(values);
    }
    private void auditCheckpoint(String id,int count)throws Exception {if(id.equals(active)){guard(job(id));job(id).put("auditProgress",count);save(job(id));}}
    JSONObject trace(String id,String caseId,String method)throws Exception {
        JSONObject j=read(id);var row=row(j,caseId);var a=row.getJSONObject("results").getJSONObject(method);var runs=new JSONArray();
        for(Object raw:a.getJSONArray("attempts")){var attempt=(JSONObject)raw;if(!attempt.has("sha256"))continue;var run=raw(j,attempt);var frames=new JSONArray();for(Object f:run.getJSONArray("samples")){var x=(JSONObject)f;frames.put(new JSONObject().put("tick",x.getInt("tick")).put("bodies",x.getJSONArray("bodies")));}runs.put(new JSONObject().put("id",run.getString("id")).put("stage",attempt.getString("stage")).put("family",attempt.getString("family")).put("status",run.getString("status")).put("definition",run.getJSONObject("definition")).put("samples",frames));}
        return new JSONObject().put("caseId",caseId).put("method",method).put("runs",runs);
    }
    private static JSONObject row(JSONObject j,String id){for(Object raw:j.getJSONArray("rows"))if(((JSONObject)raw).getString("id").equals(id))return(JSONObject)raw;throw new IllegalArgumentException("Unknown frozen case");}
    static JSONObject packed(JSONObject o)throws Exception {byte[] raw=o.toString().getBytes(StandardCharsets.UTF_8);var b=new java.io.ByteArrayOutputStream();try(var z=new java.util.zip.GZIPOutputStream(b)){z.write(raw);}return new JSONObject().put("format","fraynix.native-world-run-gzip-v1").put("rawBytes",raw.length).put("rawSha256",MapDefinition.sha(raw)).put("gzipBase64",Base64.getEncoder().encodeToString(b.toByteArray()));}
    JSONObject exportCase(String id,String caseId)throws Exception {
        JSONObject j; synchronized(this){if(busy())throw new IllegalStateException("Wait for or Stop the study before exporting originals");j=read(id);}var row=row(j,caseId);var originals=new JSONArray();
        for(String method:row.getJSONObject("results").keySet())for(Object raw:row.getJSONObject("results").getJSONObject(method).getJSONArray("attempts")){var a=(JSONObject)raw;if(a.has("sha256"))originals.put(new JSONObject().put("method",method).put("attempt",a).put("run",packed(raw(j,a))));}
        var certificates=new JSONArray();for(String method:row.getJSONObject("results").keySet()){var arm=row.getJSONObject("results").getJSONObject(method);if(arm.has("certificate")){var c=arm.getJSONObject("certificate");var bundle=bank(j,arm.getJSONObject("retrieval").getJSONObject("selected").getString("id"));var proof=RebuildCertificate075.verify(j,bundle,c,a->raw(j,a));certificates.put(new JSONObject().put("certificate",c).put("run",packed(proof)));}}
        return new JSONObject().put("certificateProofs",certificates).put("format","memory-study-case-075-v1").put("studyId",id).put("code",j.getString("code")).put("row",row).put("bank",j.getJSONArray("bank")).put("verificationCode",j.optString("auditCode",j.getString("code"))).put("rechecks",j.optJSONArray("rechecks")==null?new JSONArray():j.getJSONArray("rechecks")).put("originals",originals);
    }
    JSONObject audit(String id)throws Exception {
        var j=read(id);compatibleAudit(j);var ids=new HashSet<String>();int count=0;
        for(int i=0;i<j.getJSONArray("rows").length();i++){
            var row=j.getJSONArray("rows").getJSONObject(i);var spec=MemoryPolicy075.protocol().getJSONArray("cases").getJSONObject(i);
            if(!row.getJSONObject("spec").similar(spec)||!row.getString("specSha256").equals(WorldFrames.hash(spec)))throw new IllegalStateException("Held-out source changed");
            for(int k=0;k<4;k++)if(!row.getJSONArray("methodOrder").getString(k).equals(MemoryPolicy075.METHODS.get((i+k)%4)))throw new IllegalStateException("Counterbalanced method order changed");
            for(String method:row.getJSONObject("results").keySet()){
                auditCheckpoint(id,count);var arm=row.getJSONObject("results").getJSONObject(method);var cost=WorldFrames.copy(arm);recomputeCosts(cost);for(String k:List.of("chargedTicks","observedTicks"))if(cost.getInt(k)!=arm.getInt(k))throw new IllegalStateException("Attempt costs omitted");if(arm.getInt("chargedTicks")>MemoryPolicy075.BUDGET)throw new IllegalStateException("Native budget exceeded");
                if(!MemoryPolicy075.METHODS.contains(method)||!arm.getString("method").equals(method))throw new IllegalStateException("Unknown method");
                JSONObject expectedScene=null;JSONArray expectedCandidates=null;String refusal="";
                try{expectedScene=MemoryPolicy075.scene(spec);expectedCandidates=MemoryPolicy075.candidates(spec,expectedScene);if(expectedCandidates.toList().stream().noneMatch(o->"queued".equals(((Map<?,?>)o).get("status"))))throw new IllegalArgumentException("No original candidate has a clear admissible route");}catch(IllegalArgumentException e){refusal=e.getMessage();}
                if(!refusal.isEmpty()){if(!arm.getString("status").equals("refused")||!arm.getJSONArray("attempts").isEmpty()||arm.optBoolean("success"))throw new IllegalStateException("Refused source gained native authority");continue;}
                String preferred=MemoryPolicy075.FAMILIES.get(0);JSONObject originalMemory=null;
                if(Set.of("memory","cached").contains(method)&&!arm.has("retrieval")&&(!arm.optBoolean("retrievalMissing")||!auditRank(j,spec,expectedScene).isEmpty()))throw new IllegalStateException("Unjustified memory fallback");
                if((method.equals("memory")||method.equals("cached"))&&arm.has("retrieval")){
                    var ranked=auditRank(j,spec,expectedScene);
                    var retrieval=arm.getJSONObject("retrieval");if(!ranked.similar(retrieval.getJSONArray("ranked"))||!ranked.getJSONObject(0).similar(retrieval.getJSONObject("selected")))throw new IllegalStateException("Frozen retrieval choice changed");
                    preferred=ranked.getJSONObject(0).getString("preferred");originalMemory=bank(j,ranked.getJSONObject(0).getString("id")).getJSONObject("episode").getJSONObject("run");
                }else if(!Set.of("memory","cached").contains(method)&&arm.has("retrieval"))throw new IllegalStateException("Baseline read the memory bank");
                if(arm.has("candidateOrder")&&!arm.getJSONArray("candidateOrder").similar(new JSONArray(MemoryPolicy075.order(method,preferred))))throw new IllegalStateException("Candidate ordering changed");
                int charged=0;var settledForecasts=new JSONArray();JSONObject reconstruction=null;
                if(arm.has("certificate")){
                    if(!method.equals("cached")||!arm.has("retrieval"))throw new IllegalStateException("Certificate on a non-cache arm");
                    var bundle=bank(j,arm.getJSONObject("retrieval").getJSONObject("selected").getString("id"));var certificate=arm.getJSONObject("certificate");var certifiedRun=RebuildCertificate075.verify(j,bundle,certificate,a->raw(j,a));reconstruction=Rehearsal073.comparison(originalMemory,certifiedRun);
                    var receipt=arm.getJSONObject("cache");if(!receipt.getString("key").equals(RebuildCertificate075.key(bundle)))throw new IllegalStateException("Cache lookup source changed");
                    if(receipt.getString("status").equals("hit")&&(!receipt.getJSONObject("certificate").similar(certificate)||!receipt.getString("certificateSha256").equals(WorldFrames.hash(certificate))||Instant.parse(receipt.getString("checkedAt")).isBefore(Instant.parse(certificate.getString("createdAt")))))throw new IllegalStateException("Cache hit proof or chronology changed");
                    if(!receipt.getString("status").equals("hit")&&!arm.getJSONArray("attempts").toList().stream().anyMatch(t->certificate.getString("originAttemptId").equals(((Map<?,?>)t).get("id"))))throw new IllegalStateException("Cold certificate creation cost missing");
                }
                JSONObject actualAttempt=null,forecastAttempt=null;for(Object v:arm.getJSONArray("attempts")){var a=(JSONObject)v;if(!a.has("sha256"))continue;var run=raw(j,a);if(!ids.add(run.getString("id"))||Instant.parse(run.getString("createdAt")).isBefore(Instant.parse(a.getString("plannedAt"))))throw new IllegalStateException("Duplicate or future-leaking attempt");if(!WorldFrames.hash(run.getJSONObject("definition")).equals(a.getString("sourceSha256"))||!WorldFrames.hash(run.getJSONObject("plan")).equals(a.getString("planSha256")))throw new IllegalStateException("Attempt source/plan differs");
                    String stage=a.getString("stage");int reserve=stage.equals("actual")?0:WorldPhysics.MAX_TICKS;if(charged+WorldPhysics.MAX_TICKS+reserve>MemoryPolicy075.BUDGET)throw new IllegalStateException("Attempt exceeded worst-case admission budget");
                    int tick=run.has("final")?run.getJSONObject("final").getInt("tick"):run.getJSONArray("samples").isEmpty()?0:run.getJSONArray("samples").getJSONObject(run.getJSONArray("samples").length()-1).getInt("tick");
                    if(tick!=a.getInt("observedTicks")||a.getInt("chargedTicks")!=(a.getBoolean("unobservedTailCharged")?WorldPhysics.MAX_TICKS:tick))throw new IllegalStateException("Original attempt work changed");charged+=a.getInt("chargedTicks");
                    if(stage.equals("rebuild")){
                        if(originalMemory==null||!run.getJSONObject("definition").similar(originalMemory.getJSONObject("definition"))||!run.getJSONObject("plan").similar(originalMemory.getJSONObject("plan")))throw new IllegalStateException("Reconstruction substituted a different memory");
                        if(passed(a))reconstruction=Rehearsal073.comparison(originalMemory,run);
                    }else{
                        JSONObject candidate=null;for(Object c:expectedCandidates)if(((JSONObject)c).getString("id").equals(a.getString("family")))candidate=(JSONObject)c;
                        if(candidate==null||!Set.of("forecast","actual").contains(stage)||!run.getJSONObject("definition").similar(expectedScene)||!run.getJSONObject("plan").similar(candidate.getJSONObject("plan")))throw new IllegalStateException("New-scene source, targets or authority changed");
                        if(Set.of("memory","cached").contains(method)&&arm.has("retrieval")&&(reconstruction==null||!reconstruction.getBoolean("passed")))throw new IllegalStateException("Memory acted without a verified prior reconstruction");
                        if(stage.equals("forecast")&&passed(a))settledForecasts.put(new JSONObject().put("id",a.getString("family")).put("ordinal",candidate.getInt("ordinal")).put("status","completed").put("metrics",a.getJSONObject("metrics")).put("runId",a.getString("id")));
                    }
                    if(run.has("final")&&!WorldVerify.verify(run).similar(a.getJSONObject("metrics")))throw new IllegalStateException("Native metrics changed");if(a.getString("id").equals(arm.optString("actualRunId")))actualAttempt=a;if(a.getString("id").equals(arm.optString("forecastRunId")))forecastAttempt=a;count++;
                }
                if(arm.has("reconstruction")&&(reconstruction==null||!reconstruction.similar(arm.getJSONObject("reconstruction"))))throw new IllegalStateException("Reconstruction result changed");
                if(arm.has("selected")){var chosen=Counterfactual045.choose(settledForecasts);if(chosen==null||!chosen.getString("id").equals(arm.getString("selected"))||!chosen.getString("runId").equals(arm.getString("forecastRunId")))throw new IllegalStateException("Chosen forecast differs from preserved ranking");}
                originalMemory=null; // Release large source proof before loading the fresh pair.
                if(arm.optBoolean("success")){
                    if(actualAttempt==null||forecastAttempt==null)throw new IllegalStateException("Success lacks fresh native verification");
                    var forecast=raw(j,forecastAttempt);var actual=raw(j,actualAttempt);
                    if(!WorldVerify.verify(actual).getBoolean("passed")||!WorldVerify.verify(forecast).getBoolean("passed")||!MemoryPolicy075.comparison(forecast,actual).similar(arm.getJSONObject("comparison"))||!arm.getJSONObject("comparison").getBoolean("passed"))throw new IllegalStateException("Success lacks fresh native verification");
                }
                if(row.getString("split").equals("unsupported")&&!arm.getJSONArray("attempts").isEmpty())throw new IllegalStateException("Unsupported control actuated");
            }
        }
        auditCheckpoint(id,count);
        if(j.has("evaluation")&&!MemoryPolicy075.evaluation(j.getJSONArray("rows")).similar(j.getJSONObject("evaluation")))throw new IllegalStateException("Evaluation changed");
        return new JSONObject().put("passed",true).put("attempts",count).put("code",code());
    }
    @Override public void close(){closed=true;var store=owner;if(store!=null)store.close();worker.shutdown();try{worker.awaitTermination(5,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
}
