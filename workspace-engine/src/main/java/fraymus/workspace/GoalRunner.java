package fraymus.workspace;

import fraymus.factory.*;
import org.json.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/** Durable registered procedures. Text and imported recipes never become executable handlers. */
public final class GoalRunner implements AutoCloseable {
    public static final String PROCEDURE="world-reach-report-v1";
    private static final Set<String> LIVE=Set.of("running","waiting-host");
    private final SceneStore scenes; private final BrainService brain; private final WorkspaceStore artifacts;
    private final SchemaRegistry schemas=new SchemaRegistry(); private final FactoryWorld world;
    private final WorldCheckpointStore checkpoint;
    private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"java-goal-runner");t.setDaemon(true);return t;});
    private boolean closed;

    public GoalRunner(Path project,SceneStore scenes,BrainService brain,WorkspaceStore artifacts) throws Exception {
        this.scenes=scenes;this.brain=brain;this.artifacts=artifacts;
        for(String type:List.of("goal","procedure"))schemas.register(new ComponentSchema("workspace."+type,1,Map.of("payload",ComponentSchema.Kind.TEXT),Set.of("payload")));
        world=new FactoryWorld(schemas);checkpoint=new WorldCheckpointStore(project.resolve("goals/tasks.fworld"));
        if(Files.exists(checkpoint.path()))world.restore(checkpoint.load(schemas).snapshot());
        for(var g:values("goal"))if(LIVE.contains(g.getString("status"))){g.put("status","interrupted").put("reason","Process ended. Resume explicitly; incomplete native trials restart from frozen conditions.");g.remove("hostClaim");save("goal",g);}
        worker.scheduleWithFixedDelay(this::tick,50,100,TimeUnit.MILLISECONDS);
    }
    private List<JSONObject> values(String type){var out=new ArrayList<JSONObject>();for(var e:world.snapshot().entities())if(e.components().get(0).type().equals("workspace."+type))out.add(new JSONObject(e.components().get(0).values().get("payload")));return out;}
    private void save(String type,JSONObject value) throws Exception {
        UUID id=UUID.fromString(value.getString("id"));var before=world.snapshot();var prior=before.entities().stream().filter(e->e.id().equals(id)).findFirst();
        var c=new ComponentRecord(prior.map(e->e.components().get(0).id()).orElse(UUID.randomUUID()),"workspace."+type,1,Map.of("payload",value.toString()));var tx=new WorldTransaction(before);
        if(prior.isEmpty())tx.add(new WorldTransaction.Create(new EntityRecord(id,type,List.of(c))));else tx.add(new WorldTransaction.Put(id,c));
        try{tx.commit(world,schemas);checkpoint.save(world.snapshot(),schemas);}catch(Exception e){world.restore(before);throw e;}
    }
    private JSONObject active(){return values("goal").stream().filter(g->LIVE.contains(g.getString("status"))).findFirst().orElse(null);}
    public synchronized boolean busy(){return active()!=null;}
    public synchronized JSONObject read(String id){UUID.fromString(id);return values("goal").stream().filter(g->id.equals(g.getString("id"))).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown Java goal"));}
    public synchronized JSONArray list(){var out=new JSONArray();var all=values("goal");all.sort(Comparator.comparing(g->g.getString("createdAt")));for(var g:all)out.put(view(g));return out;}
    public synchronized JSONArray procedures(){var out=new JSONArray();for(var p:values("procedure"))out.put(p);return out;}
    static JSONObject view(JSONObject g){
        var out=new JSONObject();for(String k:List.of("id","request","status","step","attempt","resumes","createdAt","updatedAt","reason","sceneId","runId","report","metrics","savedSkill","skill","history","targetM","disturbanceMps","parameters","source","browserContext"))if(g.has(k))out.put(k,g.get(k));return out;
    }
    private static void keys(JSONObject input,Set<String> allowed){if(!allowed.containsAll(input.keySet()))throw new IllegalArgumentException("Unsupported goal/procedure field");}
    public synchronized JSONObject start(JSONObject input) throws Exception {return start(input,null);}
    private JSONObject start(JSONObject input,JSONObject skill) throws Exception {
        keys(input,Set.of("request","sceneId","expectedVersion","parameters","targetM","disturbanceMps","saveSkillName","browserContext"));
        if(closed||busy())throw new IllegalStateException("One Java goal is already active, or the runner is closed");
        if(values("goal").size()>=64)throw new IllegalStateException("Goal history budget reached (64)");scenes.goalAvailable();if(brain.state().getBoolean("busy"))throw new IllegalStateException("Wait for the current Java brain job before starting a goal");
        String request=input.optString("request","Predict, reach, chart and retain the verified procedure");if(request.isBlank()||request.length()>8000)throw new IllegalArgumentException("Goal request needs 1..8000 characters");
        var source=new JSONObject();var parameters=new JSONObject();
        if(input.has("sceneId")){var s=scenes.read(input.getString("sceneId"));if(input.optInt("expectedVersion",-1)!=s.getInt("version"))throw new ConcurrentModificationException("Source scene changed");parameters=new JSONObject(s.getJSONObject("parameters").toString());source.put("id",s.getString("id")).put("version",s.getInt("version")).put("sha256",MeasurementStore.hash(s.toString()));}
        var changes=input.optJSONObject("parameters");if(changes!=null)for(String k:changes.keySet())parameters.put(k,changes.get(k));
        parameters=RampDefinition.from(parameters).json();double target=input.optDouble("targetM",1.2),disturbance=input.optDouble("disturbanceMps",.35);FeedbackExperiment.validate(parameters,target);RampDefinition.range(disturbance,-.5,.5,"disturbance");
        String name=input.optString("saveSkillName","");if(!name.isEmpty())validName(name);
        var g=new JSONObject().put("id",UUID.randomUUID().toString()).put("procedure",PROCEDURE).put("request",request).put("source",source).put("parameters",parameters).put("targetM",target).put("disturbanceMps",disturbance).put("saveSkillName",name).put("includeBrowserContext",input.optBoolean("browserContext",false))
            .put("status","running").put("step","model").put("attempt",1).put("resumes",0).put("createdAt",Instant.now()).put("updatedAt",Instant.now()).put("deadline",System.currentTimeMillis()+180_000).put("history",new JSONArray());
        if(skill!=null)g.put("skill",new JSONObject().put("name",skill.getString("name")).put("version",skill.getInt("version")).put("id",skill.getString("id")));
        change(g,"model","Goal accepted; frozen physical conditions and finite recovery budget saved");return view(g);
    }
    private void change(JSONObject g,String step,String reason) throws Exception {
        g.put("step",step).put("reason",reason).put("updatedAt",Instant.now());g.getJSONArray("history").put(new JSONObject().put("at",Instant.now()).put("step",step).put("status",g.getString("status")).put("reason",reason).put("attempt",g.getInt("attempt")));save("goal",g);
    }
    private static String effectId(JSONObject g,String effect){return UUID.nameUUIDFromBytes((g.getString("id")+":"+g.getInt("attempt")+":"+effect).getBytes(StandardCharsets.UTF_8)).toString();}
    private synchronized void tick(){
        if(closed)return;var g=active();if(g==null)return;
        try{
            if(System.currentTimeMillis()>g.getLong("deadline"))throw new IllegalStateException("Goal exceeded its 180 second attempt budget");
            if(g.getString("status").equals("waiting-host"))return;
            switch(g.getString("step")){
                case "model" -> {
                    if(!brain.activeWorldModel().optBoolean("available")){
                        String id=effectId(g,"model");g.put("brainJobId",id);save("goal",g);var job=brain.startOwned(id);
                        if(Set.of("failed","cancelled","interrupted","completed").contains(job.getString("status"))&&!brain.activeWorldModel().optBoolean("available"))throw new IllegalStateException("Prerequisite model unavailable: "+job.optString("reason",job.getString("status")));
                        return;
                    }
                    if(g.getBoolean("includeBrowserContext")&&!g.has("browserContext"))host(g,"browser.observe");else change(g,"prepare","Accepted native model available; recorded real-data validation remains separate");
                }
                case "prepare" -> {
                    var scene=scenes.goalScene(g.getString("id"),g.getJSONObject("parameters"),g.getJSONObject("source"));g.put("sceneId",scene.getString("id"));save("goal",g);
                    var run=scenes.goalPrepare(g.getString("id"),g.getInt("attempt"),scene,g.getDouble("targetM"),g.getDouble("disturbanceMps"),brain);g.put("runId",run.getString("id"));
                    if(run.getString("status").equals("completed"))change(g,"verify","Recovered completed native receipt without repeating actions");
                    else if(run.getString("status").equals("paused")&&run.has("goalDisturbance"))change(g,"preview","Owned native trial prepared; preview must leave the body unchanged");
                    else throw new IllegalStateException("Incomplete native preparation; resume creates a separately recorded attempt");
                }
                case "preview" -> {
                    var s=scenes.snapshot();if(!s.getString("id").equals(g.getString("runId")))throw new IllegalStateException("Goal lost native run ownership");
                    var before=s.getJSONObject("current");var p=scenes.worldPreview(new JSONObject().put("runId",s.getString("id")).put("expectedTick",before.getInt("tick")).put("observationSha256",s.getJSONObject("worldProgress").getString("observationSha256")));
                    if(!before.similar(scenes.snapshot().getJSONObject("current")))throw new IllegalStateException("Preview changed physical state");g.put("preview",p);change(g,"execute","49 candidate plans previewed from the actual paused state; no preview actuation");
                }
                case "execute" -> {var s=scenes.snapshot();if(!g.getString("runId").equals(s.optString("id")))throw new IllegalStateException("Goal lost native run ownership");if(s.getString("status").equals("paused"))scenes.control(new JSONObject().put("action","resume"));change(g,"wait","20 Hz controller is executing through native physics; disturbance scheduled at tick 108");}
                case "wait" -> {var r=scenes.runRecord(g.getString("runId"));if(r.getString("status").equals("completed"))change(g,"verify","Native trial settled; checking actual outcomes");else if(!Set.of("paused","running").contains(r.getString("status")))throw new IllegalStateException("Native trial "+r.getString("status"));}
                case "verify" -> {var r=verifiedRun(g);g.put("metrics",r.getJSONObject("worldEvaluation")).put("runSha256",MeasurementStore.hash(r.toString()));change(g,"report","Physical endpoint, speed, settling, model use and exactly one actual disturbance verified");}
                case "report" -> {
                    var r=verifiedRun(g);var a=artifacts.writeOnce("goal-report:"+g.getString("id")+":"+g.getInt("attempt"),GoalReport.build(g,r));
                    g.put("report",new JSONObject().put("id",a.getString("id")).put("version",a.getInt("version")).put("sha256",MeasurementStore.hash(a.getJSONObject("contents").toString())));save("goal",g);host(g,"canvas.verify-report");
                }
                case "complete" -> {
                    verifiedRun(g);verifiedReport(g);String name=g.getString("saveSkillName");if(!name.isEmpty())g.put("savedSkill",saveProcedure(g,name));
                    g.put("status","completed");change(g,"completed","Physical goal and actual canvas report verified"+(name.isEmpty()?"":"; versioned procedural skill retained"));
                    artifacts.event("java_goal_completed",view(g));
                }
                default -> throw new IllegalStateException("Unknown registered goal step");
            }
        }catch(Throwable error){try{try{stopOwned(g);}catch(Exception cleanup){System.err.println("Goal cleanup: "+cleanup);}g.put("status","interrupted");change(g,g.getString("step"),"Execution stopped: "+error.getMessage()+". Review and Resume; no failed result is a saved skill.");}catch(Exception ignored){System.err.println("Goal checkpoint failed: "+ignored);}}
    }
    private JSONObject verifiedRun(JSONObject g){
        var r=scenes.runRecord(g.getString("runId"));var m=r.optJSONObject("worldEvaluation");
        if(!r.optString("goalId").equals(g.getString("id"))||r.optInt("goalAttempt")!=g.getInt("attempt")||!r.getString("sceneId").equals(g.getString("sceneId"))||!r.getString("status").equals("completed")||m==null||!m.getBoolean("passed")||r.getJSONObject("final").getInt("tick")!=360||!r.getJSONObject("definition").getJSONObject("parameters").similar(g.getJSONObject("parameters"))||r.getJSONObject("controller").getDouble("targetM")!=g.getDouble("targetM"))throw new IllegalStateException("Physical goal verification failed");
        if(!r.getString("backend").equals(BulletRamp.BACKEND)||!r.getJSONObject("worldPreview").getBoolean("previewOnly")||r.getJSONObject("worldPreview").getInt("candidates")!=49)throw new IllegalStateException("Missing actual native preview/backend evidence");
        var context=new JSONObject(g.getJSONObject("parameters").toString()).put("targetM",g.getDouble("targetM"));var independent=FeedbackExperiment.metrics(context,r.getJSONArray("samples"),r.getJSONObject("final"),r.getJSONArray("worldEvents"));
        int learned=0;for(Object raw:r.getJSONArray("worldPlans"))if(((JSONObject)raw).getString("mode").equals("learned-plan"))learned++;
        if(independent.getDouble("endpointErrorM")>.08||independent.getDouble("terminalSpeedMps")>.25||independent.getDouble("settlingMaxErrorM")>.15||learned/30.0<.8)throw new IllegalStateException("Reconstructed physical gates failed");
        for(String key:independent.keySet())if(Math.abs(independent.getDouble(key)-m.getDouble(key))>2e-6)throw new IllegalStateException("Stored metric differs from native samples: "+key);
        if(g.has("runSha256")&&!g.getString("runSha256").equals(MeasurementStore.hash(r.toString())))throw new IllegalStateException("Verified run changed after report binding");
        int disturbances=0;for(Object raw:r.getJSONArray("worldEvents")){var e=(JSONObject)raw;if(e.getString("kind").equals("disturbance")){disturbances++;if(e.getInt("tick")!=108||e.getDouble("deltaVelocityMps")!=g.getDouble("disturbanceMps"))throw new IllegalStateException("Actual disturbance differs from frozen intent");}}
        if(disturbances!=1||r.getJSONArray("worldPlans").length()!=30||r.getJSONArray("worldInnovations").length()!=29)throw new IllegalStateException("Incomplete native observation/action evidence");return r;
    }
    private void verifiedReport(JSONObject g) throws Exception {
        var ref=g.getJSONObject("report");var a=artifacts.read(ref.getString("id"));var proof=a.optJSONObject("test");
        if(a.getInt("version")!=ref.getInt("version")||!MeasurementStore.hash(a.getJSONObject("contents").toString()).equals(ref.getString("sha256"))||proof==null||!proof.optBoolean("passed")||!proof.getJSONArray("steps").similar(GoalReport.checks(g))||proof.getJSONArray("checks").length()!=GoalReport.checks(g).length())throw new IllegalStateException("Current report lacks its exact passing canvas receipt");
    }
    private void host(JSONObject g,String kind) throws Exception {g.put("status","waiting-host").put("hostEffect",new JSONObject().put("id",effectId(g,kind)).put("kind",kind));g.remove("hostClaim");change(g,g.getString("step"),"Waiting for registered desktop action: "+kind);}
    /** Private host route, deliberately absent from the model's tool catalog. */
    public synchronized JSONObject claim() throws Exception {
        var g=active();if(g==null||!g.getString("status").equals("waiting-host"))return new JSONObject();
        var claim=g.optJSONObject("hostClaim");if(claim!=null&&System.currentTimeMillis()<claim.getLong("expires"))return new JSONObject();
        claim=new JSONObject().put("nonce",UUID.randomUUID().toString()).put("expires",System.currentTimeMillis()+30_000);g.put("hostClaim",claim);save("goal",g);
        var out=new JSONObject(g.getJSONObject("hostEffect").toString()).put("goalId",g.getString("id")).put("nonce",claim.getString("nonce"));
        if(out.getString("kind").equals("canvas.verify-report"))out.put("report",g.getJSONObject("report")).put("steps",GoalReport.checks(g));return out;
    }
    public synchronized JSONObject acknowledge(JSONObject input) throws Exception {
        keys(input,Set.of("goalId","id","nonce","result","error"));var g=read(input.getString("goalId"));var c=g.optJSONObject("hostClaim");
        if(!g.getString("status").equals("waiting-host")||c==null||!c.getString("nonce").equals(input.getString("nonce"))||!g.getJSONObject("hostEffect").getString("id").equals(input.getString("id"))||System.currentTimeMillis()>c.getLong("expires"))throw new IllegalStateException("Stale or foreign host effect receipt");
        g.remove("hostClaim");
        if(input.has("error")){g.put("status","interrupted");change(g,g.getString("step"),"Desktop action failed: "+input.getString("error"));return view(g);}
        String kind=g.getJSONObject("hostEffect").getString("kind");
        if(kind.equals("canvas.verify-report")){verifiedReport(g);g.put("status","running");change(g,"complete","Actual Chromium canvas assertions passed for the pinned report version");}
        else {var result=input.getJSONObject("result");if(!result.has("url")||!result.has("observation"))throw new IllegalArgumentException("Browser observation receipt missing source");g.put("browserContext",new JSONObject().put("url",result.getString("url")).put("sha256",MeasurementStore.hash(result.toString())).put("authority","untrusted context only; not physical evidence"));g.put("status","running");change(g,"prepare","Browser context retained as data; it grants no executable authority");}
        return view(g);
    }
    private void stopOwned(JSONObject g) throws Exception {
        if(g.has("runId")){var s=scenes.snapshot();if(g.getString("runId").equals(s.optString("id"))&&Set.of("running","paused").contains(s.getString("status")))scenes.worldCancel(g.getString("runId"));}
        if(g.has("brainJobId")){try{var job=brain.read(g.getString("brainJobId"));if(Set.of("queued","running").contains(job.getString("status")))brain.cancel(job.getString("id"));}catch(IllegalArgumentException absent){/* Persisted intent may precede creation of its owned job. */}}
    }
    public synchronized JSONObject cancel(String id) throws Exception {var g=read(id);if(!LIVE.contains(g.getString("status"))&&!g.getString("status").equals("interrupted"))throw new IllegalStateException("Goal is already settled");stopOwned(g);g.put("status","cancelled");g.remove("hostClaim");change(g,g.getString("step"),"Cancelled; physical evolution stopped and no new skill promoted");return view(g);}
    public synchronized JSONObject resume(String id) throws Exception {
        var g=read(id);if(!g.getString("status").equals("interrupted")||busy())throw new IllegalStateException("Resume only an interrupted goal while the runner is idle");if(g.getInt("resumes")>=3)throw new IllegalStateException("Recovery budget reached (3 explicit resumes)");
        scenes.goalAvailable();g.put("resumes",g.getInt("resumes")+1).put("deadline",System.currentTimeMillis()+180_000).put("status","running");g.remove("hostClaim");
        var r=scenes.goalRun(id,g.getInt("attempt"));
        if(r!=null&&r.getString("status").equals("completed")){g.put("runId",r.getString("id"));if(g.has("report"))change(g,"report","Resuming report verification; completed physical actions will not repeat");else change(g,"verify","Recovering completed physical evidence without replay");}
        else {g.put("attempt",g.getInt("attempt")+1);for(String k:List.of("runId","report","metrics","preview","brainJobId"))g.remove(k);change(g,"model","New separately recorded native attempt from frozen conditions; interrupted effects are not retried in place");}
        return view(g);
    }
    private static void validName(String name){if(!name.matches("[a-z][a-z0-9-]{2,55}"))throw new IllegalArgumentException("Skill name needs 3..56 lowercase letters, digits or hyphens");}
    public synchronized JSONObject saveProcedure(String goalId,String name) throws Exception {var g=read(goalId);if(!g.getString("status").equals("completed"))throw new IllegalStateException("Only a completed verified goal can become a procedure");return saveProcedure(g,name);}
    private JSONObject saveProcedure(JSONObject g,String name) throws Exception {
        validName(name);verifiedRun(g);verifiedReport(g);var all=values("procedure");int version=1;
        for(var p:all)if(p.getString("name").equals(name)){if(p.getString("sourceGoalId").equals(g.getString("id")))return new JSONObject().put("name",name).put("version",p.getInt("version")).put("id",p.getString("id"));version=Math.max(version,p.getInt("version")+1);}
        if(all.size()>=32||version>8)throw new IllegalStateException("Procedural skill version/history budget reached");
        var p=new JSONObject().put("id",UUID.nameUUIDFromBytes(("procedure:"+name+":"+version).getBytes(StandardCharsets.UTF_8)).toString()).put("name",name).put("version",version).put("kind",PROCEDURE).put("sourceGoalId",g.getString("id")).put("sourceRunId",g.getString("runId")).put("report",g.getJSONObject("report")).put("verifiedAt",Instant.now())
            .put("defaults",new JSONObject().put("parameters",g.getJSONObject("parameters")).put("targetM",g.getDouble("targetM")).put("disturbanceMps",g.getDouble("disturbanceMps")))
            .put("prerequisites",new JSONArray(List.of("Native Bullet backend","Accepted native world model, or bounded training prerequisite","Default ramp geometry and evaluated parameter domain","No foreign active physics run")))
            .put("actions",new JSONArray(List.of("Freeze source into an owned scene","Ensure accepted model","Preview actual paused state","Run receding-horizon control with one scheduled disturbance","Check physical outcomes","Write and test motion chart in actual canvas")))
            .put("successChecks",new JSONObject().put("endpointM",.08).put("terminalSpeedMps",.25).put("settlingM",.15).put("minModelFraction",.8).put("actualDisturbances",1).put("passingCanvasReceipt",true))
            .put("recovery",new JSONArray(List.of("Stop owned motion on error/cancel","Require explicit resume after process loss","Restart incomplete physics as a new recorded attempt","Reuse completed physics and stable report identity","Never save failed outcomes as skills"))).put("maxExplicitResumes",3).put("scope","Registered native ramp workflow; this version proves its source conditions only. New conditions are independently verified at each execution.");
        save("procedure",p);return new JSONObject().put("name",name).put("version",version).put("id",p.getString("id"));
    }
    public synchronized JSONObject runProcedure(JSONObject input) throws Exception {
        keys(input,Set.of("name","version","parameters","targetM","disturbanceMps","sceneId","expectedVersion","browserContext"));validName(input.getString("name"));
        var p=values("procedure").stream().filter(v->v.getString("name").equals(input.getString("name"))&&v.getInt("version")==input.getInt("version")).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown procedural skill version"));
        if(!p.getString("kind").equals(PROCEDURE))throw new IllegalArgumentException("Unregistered procedure kind");var args=new JSONObject(p.getJSONObject("defaults").toString());for(String k:input.keySet())if(!Set.of("name","version").contains(k)){if(k.equals("parameters")){var merged=args.getJSONObject("parameters");for(String key:input.getJSONObject(k).keySet())merged.put(key,input.getJSONObject(k).get(key));}else args.put(k,input.get(k));}if(input.has("sceneId"))args.put("parameters",input.optJSONObject("parameters")==null?new JSONObject():input.getJSONObject("parameters"));args.put("request","Reuse "+p.getString("name")+" v"+p.getInt("version")+" with freshly verified physical outcomes");return start(args,p);
    }
    @Override public synchronized void close(){closed=true;worker.shutdownNow();}
}
