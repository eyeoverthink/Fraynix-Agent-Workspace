package fraymus.workspace;

import fraymus.factory.*;
import org.json.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/** Shared authored entities and bounded, separately persisted experiment runs. */
public final class SceneStore implements AutoCloseable {
    private final SchemaRegistry schemas=new SchemaRegistry();
    private final FactoryWorld world;
    private final WorldCheckpointStore checkpoint;
    private final Path root;
    private final ScheduledExecutorService clock=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"scene-physics-clock");t.setDaemon(true);return t;});
    private BulletRamp physics;
    private JSONObject active;
    private boolean playing;
    private JSONObject bodyPlan;
    private String error="";
    public SceneStore(Path project) throws Exception {
        root=project.resolve("experiments");Files.createDirectories(root);
        schemas.register(new ComponentSchema("workspace.scene",1,Map.of("payload",ComponentSchema.Kind.TEXT),Set.of("payload")));
        schemas.register(new ComponentSchema("workspace.scene.object3d",1,Map.of("scene",ComponentSchema.Kind.ENTITY_REFERENCE,"payload",ComponentSchema.Kind.TEXT),Set.of("scene","payload")));
        schemas.register(new ComponentSchema("workspace.experiment",1,Map.of("scene",ComponentSchema.Kind.ENTITY_REFERENCE,"payload",ComponentSchema.Kind.TEXT),Set.of("scene","payload")));
        schemas.register(new ComponentSchema("workspace.scene.calibration",1,Map.of("scene",ComponentSchema.Kind.ENTITY_REFERENCE,"payload",ComponentSchema.Kind.TEXT),Set.of("scene","payload")));
        schemas.register(new ComponentSchema("workspace.body.operation",1,Map.of("scene",ComponentSchema.Kind.ENTITY_REFERENCE,"payload",ComponentSchema.Kind.TEXT),Set.of("scene","payload")));
        schemas.register(new ComponentSchema("workspace.scene.import",1,Map.of("scene",ComponentSchema.Kind.ENTITY_REFERENCE,"payload",ComponentSchema.Kind.TEXT),Set.of("scene","payload")));
        world=new FactoryWorld(schemas);checkpoint=new WorldCheckpointStore(root.resolve("scenes.fworld"));
        if(Files.exists(checkpoint.path()))world.restore(checkpoint.load(schemas).snapshot());
        var interrupted=new ArrayList<EntityRecord>();
        for(var entity:world.snapshot().entities())if(type(entity).equals("workspace.experiment")){var run=payload(entity);if(Set.of("running","paused").contains(run.getString("status"))){run.put("status","interrupted").put("endedAt",Instant.now());interrupted.add(record("workspace.experiment",run,run.getString("sceneId")));}}
        for(var entity:world.snapshot().entities())if(type(entity).equals("workspace.body.operation")){var op=payload(entity);if(op.getString("status").equals("pending")){op.put("status","interrupted").put("error","Process ended before the action receipt; no action was retried");interrupted.add(record("workspace.body.operation",op,op.getString("sceneId")));}}
        if(!interrupted.isEmpty())commit(interrupted);
        clock.scheduleAtFixedRate(()->{synchronized(this){try{if(bodyPlan!=null)advanceBody(4);else if(playing)advance(4);}catch(Exception|AssertionError|LinkageError e){playing=false;error=e.toString();e.printStackTrace();if(physics!=null)physics.release();if(bodyPlan!=null){bodyPlan.put("status","failed").put("error",error);try{commit(List.of(record("workspace.body.operation",bodyPlan,bodyPlan.getString("sceneId"))));}catch(Exception ignored){}bodyPlan=null;}if(active!=null)try{finish("failed");}catch(Exception ignored){}}}},0,16_666_667,TimeUnit.NANOSECONDS);
    }
    private static String type(EntityRecord e){return e.components().get(0).type();}
    private static JSONObject payload(EntityRecord e){return new JSONObject(e.components().get(0).values().get("payload"));}
    private EntityRecord find(String id){UUID uuid=UUID.fromString(id);return world.snapshot().entities().stream().filter(e->e.id().equals(uuid)).findFirst().orElse(null);}
    private JSONObject require(String id,String type){var e=find(id);if(e==null||!type(e).equals(type))throw new IllegalArgumentException("Unknown "+type);return payload(e);}
    private EntityRecord record(String type,JSONObject value,String scene){var prior=find(value.getString("id"));var fields=new HashMap<String,String>();fields.put("payload",value.toString());if(scene!=null)fields.put("scene",scene);return new EntityRecord(UUID.fromString(value.getString("id")),value.optString("title",type),List.of(new ComponentRecord(prior==null?UUID.randomUUID():prior.components().get(0).id(),type,1,fields)));}
    private void commit(List<EntityRecord> records) throws Exception {
        var before=world.snapshot();var tx=new WorldTransaction(before);
        for(var e:records)if(find(e.id().toString())==null)tx.add(new WorldTransaction.Create(e));else{tx.add(new WorldTransaction.Rename(e.id(),e.name()));tx.add(new WorldTransaction.Put(e.id(),e.components().get(0)));}
        try{tx.commit(world,schemas);checkpoint.save(world.snapshot(),schemas);}catch(Exception e){world.restore(before);throw e;}
    }
    public synchronized JSONArray scenes(){var out=new JSONArray();for(var e:world.snapshot().entities())if(type(e).equals("workspace.scene")){var s=payload(e);out.put(new JSONObject().put("id",s.getString("id")).put("title",s.getString("title")).put("version",s.getInt("version")));}return out;}
    public synchronized JSONObject read(String id){var scene=require(id,"workspace.scene");var objects=new JSONArray();for(var e:world.snapshot().entities())if(type(e).equals("workspace.scene.object3d")&&e.components().get(0).values().get("scene").equals(id))objects.put(payload(e));return scene.put("objects",objects);}
    /** One atomic source for an observer; a replay retains its actual old definition. */
    public synchronized JSONObject observerWorld(String id){
        var authored=read(id);boolean live=active!=null&&physics!=null&&active.getString("sceneId").equals(id);
        var definition=live?active.getJSONObject("definition"):authored;
        var current=live?physics.snapshot():null;
        var objects=current==null?authored.getJSONArray("objects"):current.getJSONArray("objects");
        var values=new ArrayList<JSONObject>();for(Object value:objects)values.add(new JSONObject(value.toString()));values.sort(Comparator.comparing(o->o.getString("id")));var sorted=new JSONArray(values);
        return new JSONObject().put("sceneId",id).put("sceneVersion",definition.getInt("version")).put("authoredVersion",authored.getInt("version"))
            .put("runId",live?active.get("id"):JSONObject.NULL).put("tick",live?current.getInt("tick"):0).put("timeS",live?current.getDouble("timeS"):0)
            .put("source",live?"native-physics":"authored-geometry").put("status",live?active.getString("status"):"authored")
            .put("grip",live?physics.grip():new JSONObject().put("held",false)).put("actuatorRevision",live?active.optInt("actuatorRevision"):0)
            .put("scope",live?active.optString("scope","experiment"):"authored").put("units","metre, second, degree; +Y up").put("objects",sorted);
    }
    public synchronized JSONObject save(JSONObject input) throws Exception {
        return save(input,null);
    }
    private JSONObject save(JSONObject input,JSONObject application) throws Exception {
        return save(input,application,"workspace.scene.calibration");
    }
    private JSONObject save(JSONObject input,JSONObject application,String receiptType) throws Exception {
        if(!Set.of("id","expectedVersion","title","parameters").containsAll(input.keySet()))throw new IllegalArgumentException("Unsupported scene field");
        if(input.has("parameters")&&!(input.get("parameters") instanceof JSONObject))throw new IllegalArgumentException("Scene parameters must be an object");
        if(physics!=null)throw new IllegalStateException("Reset the current experiment before editing its authored scene");
        String title=input.optString("title","Ramp laboratory").strip();if(title.isEmpty()||title.length()>120)throw new IllegalArgumentException("Scene title needs 1..120 characters");
        boolean existing=input.has("id");JSONObject previous=existing?read(input.getString("id")):null;
        if(existing&&input.optInt("expectedVersion",-1)!=previous.getInt("version"))throw new ConcurrentModificationException("Scene changed; read the current version before editing");
        JSONObject config=previous==null?new JSONObject():previous.getJSONObject("parameters");var changes=input.optJSONObject("parameters");if(changes!=null)for(String key:changes.keySet())config.put(key,changes.get(key));var definition=RampDefinition.from(config);
        String id=existing?previous.getString("id"):UUID.randomUUID().toString();JSONObject ids=existing?previous.getJSONObject("entityIds"):new JSONObject().put("ramp",UUID.randomUUID().toString()).put("block",UUID.randomUUID().toString()).put("ground",UUID.randomUUID().toString());
        var scene=new JSONObject().put("id",id).put("schemaVersion",1).put("title",title).put("version",existing?previous.getInt("version")+1:1).put("units","SI: metre, second, kilogram").put("frame","right-handed, +Y up, downhill +X, depth Z")
            .put("parameters",definition.json()).put("entityIds",ids).put("updatedAt",Instant.now());
        var records=new ArrayList<EntityRecord>();records.add(record("workspace.scene",scene,null));for(Object o:definition.objects(ids)){var object=(JSONObject)o;object.put("schemaVersion",1);records.add(record("workspace.scene.object3d",object,id));}
        if(application!=null){application.put("sceneId",id);if(receiptType.equals("workspace.scene.import")){application.put("entityIds",ids);scene.put("importReceiptId",application.getString("id"));records.set(0,record("workspace.scene",scene,null));}records.add(record(receiptType,application,id));}commit(records);return read(id);
    }
    public synchronized JSONObject calibrationReceipt(String operationId){var e=find(operationId);return e!=null&&type(e).equals("workspace.scene.calibration")?payload(e):null;}
    public synchronized JSONObject applyCalibration(String jobId,String operationId,String sceneId,int expectedVersion,String validationSceneId,int validationVersion,String parameter,double value) throws Exception {
        var prior=calibrationReceipt(operationId);if(prior!=null)return prior;
        if(read(validationSceneId).getInt("version")!=validationVersion)throw new ConcurrentModificationException("Validation scene changed; start a new calibration");
        return parameterOperation(jobId,operationId,sceneId,expectedVersion,parameter,value,"apply",null);
    }
    public synchronized JSONObject rollbackCalibration(String jobId,String operationId,String originalOperationId) throws Exception {
        var prior=calibrationReceipt(operationId);if(prior!=null)return prior;
        var applied=calibrationReceipt(originalOperationId);
        if(applied==null||!applied.getString("kind").equals("apply")||!applied.getString("jobId").equals(jobId))throw new IllegalArgumentException("No matching calibration application");
        return parameterOperation(jobId,operationId,applied.getString("sceneId"),applied.getInt("toVersion"),applied.getString("parameter"),applied.getDouble("previousValue"),"rollback",originalOperationId);
    }
    private JSONObject parameterOperation(String jobId,String operationId,String sceneId,int expectedVersion,String parameter,double value,String kind,String original) throws Exception {
        if(!Set.of("friction","gravityMps2").contains(parameter))throw new IllegalArgumentException("Only friction or gravity calibration is supported");
        var scene=read(sceneId);if(scene.getInt("version")!=expectedVersion)throw new ConcurrentModificationException("Scene changed; calibration cannot overwrite subsequent edits");
        if(physics!=null){if(playing||Set.of("running","paused").contains(active.getString("status")))throw new IllegalStateException("Reset the active experiment before applying a calibration");control(new JSONObject().put("action","reset"));}
        var receipt=new JSONObject().put("id",operationId).put("jobId",jobId).put("sceneId",sceneId).put("kind",kind).put("parameter",parameter).put("previousValue",scene.getJSONObject("parameters").getDouble(parameter)).put("value",value).put("fromVersion",expectedVersion).put("toVersion",expectedVersion+1).put("createdAt",Instant.now());
        if(original!=null)receipt.put("originalOperationId",original);
        save(new JSONObject().put("id",sceneId).put("expectedVersion",expectedVersion).put("title",scene.getString("title")).put("parameters",new JSONObject().put(parameter,value)),receipt);return receipt;
    }
    /** Isolated native trial; records intent/result without moving the interactive viewport. */
    public JSONObject calibrationRun(JSONObject definition,double seconds,String operationId,String jobId,String phase,java.util.function.BooleanSupplier abort) throws Exception {
        RampDefinition.range(seconds,.05,10,"seconds");int target=(int)Math.round(seconds/BulletRamp.DT);
        synchronized(this){for(var e:world.snapshot().entities())if(type(e).equals("workspace.experiment")){var previous=payload(e);if(operationId.equals(previous.optString("operationId"))&&previous.getString("status").equals("completed")){
            if(!previous.getJSONObject("definition").similar(definition)||previous.getInt("targetTicks")!=target||!previous.getString("backend").equals(BulletRamp.BACKEND))throw new IllegalStateException("Saved calibration trial definition or backend differs");return previous;}}}
        var run=new JSONObject().put("id",UUID.randomUUID().toString()).put("title","Calibration "+phase).put("sceneId",definition.getString("id")).put("sceneVersion",definition.getInt("version")).put("definition",new JSONObject(definition.toString()))
            .put("scope","calibration").put("calibrationId",jobId).put("operationId",operationId).put("phase",phase).put("backend",BulletRamp.BACKEND).put("dt",BulletRamp.DT).put("targetTicks",target).put("status","running").put("startedAt",Instant.now()).put("samples",new JSONArray());
        synchronized(this){commit(List.of(record("workspace.experiment",run,run.getString("sceneId"))));}
        try(var trial=new BulletRamp(RampDefinition.from(definition.getJSONObject("parameters")),definition.getJSONObject("entityIds"),root.resolve("native"))){
            run.put("solverConfiguration",trial.configuration());run.getJSONArray("samples").put(trial.sample());
            for(int i=0;i<target;i++){if(abort.getAsBoolean()||Thread.currentThread().isInterrupted())throw new CancellationException("Calibration trial interrupted");trial.step();if(trial.tick()%4==0)run.getJSONArray("samples").put(trial.sample());}
            run.put("final",trial.snapshot()).put("status","completed");
        }catch(Exception error){run.put("status",error instanceof CancellationException?"interrupted":"failed").put("error",String.valueOf(error.getMessage()));throw error;}
        catch(LinkageError error){run.put("status","failed").put("error",String.valueOf(error.getMessage()));throw new IllegalStateException("Native calibration backend failed",error);}
        finally{run.put("endedAt",Instant.now());synchronized(this){commit(List.of(record("workspace.experiment",run,run.getString("sceneId"))));}}
        return run;
    }
    private void begin(String id,int expectedVersion,double seconds) throws Exception {
        var scene=read(id);if(scene.getInt("version")!=expectedVersion)throw new ConcurrentModificationException("Experiment scene version changed");
        beginDefinition(scene,seconds);
    }
    private void beginDefinition(JSONObject scene,double seconds) throws Exception {
        RampDefinition.range(seconds,.05,10,"seconds");if(physics!=null)throw new IllegalStateException("Reset the current experiment before starting another");String id=scene.getString("id");
        active=new JSONObject().put("id",UUID.randomUUID().toString()).put("title",scene.getString("title")+" experiment").put("sceneId",id).put("sceneVersion",scene.getInt("version")).put("definition",scene).put("backend",BulletRamp.BACKEND)
            .put("dt",BulletRamp.DT).put("targetTicks",(int)Math.round(seconds/BulletRamp.DT)).put("status","paused").put("startedAt",Instant.now()).put("samples",new JSONArray());
        if(scene.has("calibrationOverride"))active.put("scope","calibration");
        try{physics=new BulletRamp(RampDefinition.from(scene.getJSONObject("parameters")),scene.getJSONObject("entityIds"),root.resolve("native"));active.put("solverConfiguration",physics.configuration());sample();commit(List.of(record("workspace.experiment",active,id)));error="";}catch(Exception e){if(physics!=null)physics.close();physics=null;active=null;throw e;}
    }
    private void sample(){active.getJSONArray("samples").put(physics.sample());}
    private void advance(int ticks) throws Exception {
        if(physics==null)return;
        for(int i=0;i<ticks&&physics.tick()<active.getInt("targetTicks");i++){
            if(active.optString("scope").equals("feedback-policy")&&physics.tick()%12==0){
                active.put("pendingFeedback",new JSONObject().put("tick",physics.tick()).put("kind","control"));commit(List.of(record("workspace.experiment",active,active.getString("sceneId"))));
                FeedbackExperiment.act(physics,RampDefinition.from(active.getJSONObject("definition").getJSONObject("parameters")),active.getJSONObject("controller").getDouble("targetM"),active.getJSONObject("controller"),true,active.getJSONArray("feedbackEvents"));
                active.remove("pendingFeedback");commit(List.of(record("workspace.experiment",active,active.getString("sceneId"))));
            }
            physics.step();if(physics.tick()%4==0)sample();
        }
        if(physics.tick()>=active.getInt("targetTicks"))finish("completed");
    }
    private void finish(String status) throws Exception {
        playing=false;if(physics==null||active==null)return;
        JSONObject last=physics.snapshot();active.put("status",status).put("endedAt",Instant.now()).put("final",last);
        var params=RampDefinition.from(active.getJSONObject("definition").getJSONObject("parameters"));double t=last.getDouble("timeS"),expected=.5*params.gravityMps2()*Math.sin(params.radians())*t*t;
        boolean applicable=!Set.of("manipulation","brain-policy","feedback-policy").contains(active.optString("scope"))&&params.friction()==0&&t>=.1&&t<=1&&expected<params.lengthM()*.7-.7;
        double measured=last.getDouble("distanceM"),tolerance=Math.max(.015,expected*.04),error=Math.abs(measured-expected);
        active.put("evaluation",new JSONObject().put("reference","frictionless incline: distance = 0.5*g*sin(angle)*t^2").put("applicable",applicable).put("expectedDistanceM",expected).put("measuredDistanceM",measured).put("absoluteErrorM",error).put("toleranceM",tolerance).put("passed",status.equals("completed")&&applicable&&error<=tolerance));
        if(active.has("controller")){double target=active.getJSONObject("controller").getDouble("targetM");active.put("goalEvaluation",new JSONObject().put("targetM",target).put("measuredDistanceM",measured).put("absoluteErrorM",Math.abs(measured-target)).put("toleranceM",.25).put("passed",status.equals("completed")&&Math.abs(measured-target)<=.25));}
        if(active.optString("scope").equals("feedback-policy")){
            var c=new JSONObject(active.getJSONObject("definition").getJSONObject("parameters").toString()).put("targetM",active.getJSONObject("controller").getDouble("targetM"));
            var m=FeedbackExperiment.metrics(c,active.getJSONArray("samples"),last,active.getJSONArray("feedbackEvents"));
            boolean passed=status.equals("completed")&&m.getDouble("endpointErrorM")<=.06&&m.getDouble("terminalSpeedMps")<=.2&&m.getDouble("settlingMaxErrorM")<=.12;
            active.put("feedbackEvaluation",m.put("passed",passed));active.getJSONObject("goalEvaluation").put("toleranceM",.06).put("passed",passed);
        }
        commit(List.of(record("workspace.experiment",active,active.getString("sceneId"))));
    }
    public synchronized JSONObject snapshot(){
        if(active==null)return new JSONObject().put("status","idle").put("error",error);
        var value=new JSONObject(active.toString()).put("playing",playing).put("current",physics.snapshot());value.remove("samples");value.remove("actuatorEvents");
        if(value.has("feedbackEvents")){var events=value.getJSONArray("feedbackEvents");value.put("feedbackProgress",new JSONObject().put("actions",events.length()).put("ticks",physics.tick()).put("totalTicks",360).put("lastEvent",events.isEmpty()?JSONObject.NULL:events.getJSONObject(events.length()-1)));value.remove("feedbackEvents");}
        if(bodyPlan!=null)value.put("bodyProgress",new JSONObject().put("operationId",bodyPlan.getString("id")).put("action",bodyPlan.getString("action")).put("completedTicks",physics.tick()-bodyPlan.getInt("startTick")).put("totalTicks",bodyPlan.getInt("totalTicks")));
        var samples=active.getJSONArray("samples");var tail=new JSONArray();for(int i=Math.max(0,samples.length()-180);i<samples.length();i++)tail.put(samples.get(i));return value.put("trace",tail).put("error",error);
    }
    public synchronized JSONObject control(JSONObject input) throws Exception {
        String action=input.getString("action");
        if(action.equals("reset")){if(bodyPlan!=null)cancelBody("Reset requested");if(active!=null&&Set.of("running","paused").contains(active.getString("status")))finish("stopped");if(physics!=null)physics.close();physics=null;active=null;playing=false;return snapshot();}
        if(active!=null&&active.optString("scope").equals("manipulation"))throw new IllegalStateException("Use the body controls for this manipulation run, or Reset before another experiment");
        if(action.equals("start")){begin(input.getString("id"),input.getInt("expectedVersion"),input.optDouble("seconds",6));playing=true;active.put("status","running");return snapshot();}
        if(physics==null)throw new IllegalStateException("Start an experiment first");
        if(action.equals("pause")){playing=false;if(active.getString("status").equals("running")){active.put("status","paused");commit(List.of(record("workspace.experiment",active,active.getString("sceneId"))));}}
        else if(action.equals("resume")){if(!active.getString("status").equals("paused"))throw new IllegalStateException("Only a paused run can resume");playing=true;active.put("status","running");}
        else if(action.equals("step")){if(playing||!active.getString("status").equals("paused"))throw new IllegalStateException("Pause the run before stepping");advance(1);}
        else throw new IllegalArgumentException("Unknown experiment control");return snapshot();
    }
    public synchronized JSONObject run(JSONObject input) throws Exception {
        begin(input.getString("id"),input.getInt("expectedVersion"),input.optDouble("seconds",.75));advance(active.getInt("targetTicks"));return new JSONObject(active.toString());
    }
    public synchronized JSONObject controlledRun(JSONObject input,BrainService brain) throws Exception {
        var scene=read(input.getString("id"));var decision=brain.prediction(scene,input.getDouble("targetM"),input.optBoolean("gpu",false));decision.put("targetM",input.getDouble("targetM"));
        begin(input.getString("id"),input.getInt("expectedVersion"),.75);active.put("scope","brain-policy").put("controller",decision);
        physics.push(decision.getDouble("deltaVelocityMps"));advance(active.getInt("targetTicks"));return new JSONObject(active.toString());
    }
    public synchronized JSONObject feedbackStart(JSONObject input,BrainService brain) throws Exception {
        if(!input.keySet().equals(Set.of("id","expectedVersion","targetM")))throw new IllegalArgumentException("Unsupported feedback start field");
        var scene=read(input.getString("id"));FeedbackExperiment.validate(scene.getJSONObject("parameters"),input.getDouble("targetM"));var policy=brain.feedbackForRun();policy.put("targetM",input.getDouble("targetM"));
        begin(input.getString("id"),input.getInt("expectedVersion"),1.5);active.put("scope","feedback-policy").put("controller",policy).put("feedbackEvents",new JSONArray()).put("status","running").put("feedbackProtocol",FeedbackExperiment.manifest());
        commit(List.of(record("workspace.experiment",active,active.getString("sceneId"))));playing=true;return snapshot();
    }
    public synchronized JSONObject feedbackKick(JSONObject input) throws Exception {
        if(!input.keySet().equals(Set.of("runId","operationId","deltaVelocityMps")))throw new IllegalArgumentException("Unsupported feedback disturbance field");
        requireFeedback(input.getString("runId"));UUID.fromString(input.getString("operationId"));double dv=input.getDouble("deltaVelocityMps");RampDefinition.range(dv,-.5,.5,"disturbance");
        for(Object raw:active.getJSONArray("feedbackEvents")){var prior=(JSONObject)raw;if(prior.optString("operationId").equals(input.getString("operationId"))){if(prior.getDouble("deltaVelocityMps")!=dv)throw new IllegalArgumentException("Operation id reused with different disturbance");return prior;}}
        active.put("pendingFeedback",new JSONObject(input.toString()).put("tick",physics.tick()));commit(List.of(record("workspace.experiment",active,active.getString("sceneId"))));
        var receipt=FeedbackExperiment.impulse(physics,RampDefinition.from(active.getJSONObject("definition").getJSONObject("parameters")),dv,"disturbance").put("operationId",input.getString("operationId"));active.getJSONArray("feedbackEvents").put(receipt);active.remove("pendingFeedback");commit(List.of(record("workspace.experiment",active,active.getString("sceneId"))));return receipt;
    }
    private void requireFeedback(String id){if(active==null||physics==null||!active.getString("id").equals(id)||!active.optString("scope").equals("feedback-policy")||!Set.of("running","paused").contains(active.getString("status")))throw new IllegalStateException("No matching active feedback run");}
    public synchronized JSONObject feedbackCancel(String id) throws Exception {requireFeedback(id);finish("cancelled");return snapshot();}
    private JSONObject replayFeedback(JSONObject previous) throws Exception {
        if(!previous.getString("status").equals("completed"))throw new IllegalStateException("Only completed feedback runs can replay");
        beginDefinition(previous.getJSONObject("definition"),1.5);active.put("scope","feedback-policy").put("controller",new JSONObject(previous.getJSONObject("controller").toString())).put("feedbackEvents",new JSONArray()).put("replayOf",previous.getString("id")).put("feedbackProtocol",previous.getJSONObject("feedbackProtocol"));
        var config=RampDefinition.from(active.getJSONObject("definition").getJSONObject("parameters"));
        for(int tick=0;tick<360;tick++){
            for(Object raw:previous.getJSONArray("feedbackEvents")){var e=(JSONObject)raw;if(e.getInt("tick")==tick){double limit=e.getString("kind").equals("control")?.6:.5;RampDefinition.range(e.getDouble("deltaVelocityMps"),-limit,limit,"saved feedback impulse");var applied=FeedbackExperiment.impulse(physics,config,e.getDouble("deltaVelocityMps"),e.getString("kind"));active.getJSONArray("feedbackEvents").put(applied);}}
            physics.step();if(physics.tick()%4==0)sample();
        }
        double error=Math.abs(physics.snapshot().getDouble("distanceM")-previous.getJSONObject("final").getDouble("distanceM"));active.put("replayEvaluation",new JSONObject().put("sourceRunId",previous.getString("id")).put("positionErrorM",error).put("toleranceM",.0001).put("passed",error<=.0001));finish("completed");return new JSONObject(active.toString());
    }
    public synchronized JSONObject replay(String id) throws Exception {
        var previous=require(id,"workspace.experiment");if(physics!=null)throw new IllegalStateException("Reset the current experiment before replay");
        if(previous.optString("scope").equals("manipulation"))return replayBody(previous);
        if(previous.optString("scope").equals("feedback-policy"))return replayFeedback(previous);
        beginDefinition(previous.getJSONObject("definition"),previous.getInt("targetTicks")*BulletRamp.DT);active.put("replayOf",id);
        if(previous.optString("scope").equals("brain-policy")){active.put("scope","brain-policy").put("controller",new JSONObject(previous.getJSONObject("controller").toString()));physics.push(active.getJSONObject("controller").getDouble("deltaVelocityMps"));}
        advance(active.getInt("targetTicks"));return new JSONObject(active.toString());
    }
    public synchronized JSONObject runRecord(String id){return require(id,"workspace.experiment");}
    public synchronized JSONObject bodyOperation(String id){return require(id,"workspace.body.operation");}
    synchronized JSONObject bodyPrior(JSONObject input){
        var old=find(input.getString("operationId"));if(old==null)return null;
        var op=require(input.getString("operationId"),"workspace.body.operation");
        if(!op.getString("requestSha256").equals(MeasurementStore.hash(input.toString())))throw new IllegalArgumentException("Body operation id reused with different input");
        if(!op.getString("status").equals("completed"))throw new IllegalStateException("Previous body operation is "+op.getString("status")+"; it will not be applied twice");
        return op;
    }
    private static double[] position(JSONObject state){return ObserverGeometry.v(state.getJSONArray("position"));}
    private static double distance(double[] a,double[] b){return ObserverGeometry.norm(ObserverGeometry.sub(a,b));}
    private void requireBodyOwner(JSONObject input){
        if(active==null||physics==null||!active.optString("scope").equals("manipulation")||!active.getString("bodyOwner").equals(input.getString("id"))||!active.getString("id").equals(input.getString("runId")))
            throw new IllegalStateException("This observer does not own the active manipulation run");
        if(!active.getString("status").equals("paused"))throw new IllegalStateException("The manipulation run is already settled; Reset before another grab");
    }
    synchronized JSONObject bodyAction(JSONObject input,JSONObject packet) throws Exception {
        var previous=bodyPrior(input);if(previous!=null)return previous;
        String action=input.getString("action"),operation=input.getString("operationId");UUID.fromString(operation);
        var common=new HashSet<>(Set.of("id","operationId","action","observationId"));
        switch(action){case "grab"->common.add("entityId");case "move"->{common.addAll(Set.of("runId","gripId","target"));}case "release"->common.addAll(Set.of("runId","gripId"));case "cancel"->common.add("runId");default->throw new IllegalArgumentException("Unknown body action");}
        if(!common.containsAll(input.keySet()))throw new IllegalArgumentException("Unexpected body action field");
        if(action.equals("grab")){
            if(physics!=null)throw new IllegalStateException("Reset the current experiment before starting a new body manipulation");
            String entity=input.getString("entityId");var source=packet.getJSONObject("world");var scene=read(source.getString("sceneId"));
            if(!scene.getJSONObject("entityIds").getString("block").equals(entity))throw new IllegalArgumentException("Only the dynamic Block can be grabbed");
            if(scene.getJSONObject("parameters").getDouble("massKg")>10)throw new IllegalArgumentException("Actuator load limit is 10 kg");
            beginDefinition(scene,10);active.put("scope","manipulation").put("targetTicks",14_400).put("bodyOwner",input.getString("id")).put("actuatorRevision",0).put("actuatorEvents",new JSONArray());
        }else requireBodyOwner(input);
        if(action.equals("cancel")){
            var before=physics.snapshot();cancelBody("Owner requested cancellation");
            var op=operation(input,before).put("status","completed").put("outcome","cancelled").put("after",physics.snapshot()).put("endTick",physics.tick());
            commit(List.of(record("workspace.body.operation",op,active.getString("sceneId"))));return op;
        }
        if(bodyPlan!=null)throw new IllegalStateException("A body motion is in progress; wait or cancel it");
        if(!action.equals("grab")){
            var grip=physics.grip();if(!grip.getBoolean("held")||!grip.getString("id").equals(input.getString("gripId")))throw new IllegalStateException("Grip changed or was released");
        }
        if(action.equals("move")&&active.getJSONArray("actuatorEvents").length()>=23)throw new IllegalStateException("Manipulation move budget reached; release remains available");
        var op=operation(input,physics.snapshot());
        if(action.equals("move")){
            Object raw=input.get("target");if(!(raw instanceof JSONArray target)||target.length()!=3)throw new IllegalArgumentException("Target needs three finite metre coordinates");
            for(Object value:target)if(!(value instanceof Number n)||!Double.isFinite(n.doubleValue()))throw new IllegalArgumentException("Target must contain finite numbers");
            var p=ObserverGeometry.v(target);RampDefinition.range(p[0],-20,20,"body target x");RampDefinition.range(p[1],.26,20,"body target y");RampDefinition.range(p[2],-20,20,"body target z");
            var anchor=ObserverGeometry.v(physics.grip().getJSONArray("target"));double length=distance(anchor,p);
            if(length<.01||length>2||distance(position(physics.snapshot()),p)>2)throw new IllegalArgumentException("Body move must be 0.01..2 m from the body and current anchor");
            if(distance(p,position(packet.getJSONObject("observer").getJSONObject("pose")))>6)throw new IllegalArgumentException("Body target exceeds the 6 m virtual actuator reach");
            int travel=(int)Math.ceil(length/1.5/BulletRamp.DT);
            op.put("startAnchor",new JSONArray(anchor)).put("target",target).put("travelTicks",travel).put("totalTicks",travel+48).put("toleranceM",.08);
        }else op.put("totalTicks",action.equals("release")?120:0);
        if(physics.tick()+op.getInt("totalTicks")+(action.equals("release")?0:120)>14_400)throw new IllegalStateException("Manipulation tick budget reached");
        // Journal intent before touching the native constraint. Interrupted intent is never retried.
        commit(List.of(record("workspace.body.operation",op,active.getString("sceneId")),record("workspace.experiment",active,active.getString("sceneId"))));
        try{
            if(action.equals("grab")){physics.grab(input.getString("entityId"),input.getString("id"),operation);completeBody(op);}
            else {if(action.equals("release")){physics.release();op.put("velocityAtConstraintRemoval",physics.sample().getJSONArray("velocity"));}bodyPlan=op;}
        }catch(Exception e){physics.release();op.put("status","failed").put("error",String.valueOf(e.getMessage()));commit(List.of(record("workspace.body.operation",op,active.getString("sceneId"))));finish("failed");throw e;}
        return new JSONObject(op.toString());
    }
    private JSONObject operation(JSONObject input,JSONObject before){
        return new JSONObject().put("id",input.getString("operationId")).put("observerId",input.getString("id")).put("sceneId",active.getString("sceneId")).put("sceneVersion",active.getInt("sceneVersion")).put("runId",active.getString("id"))
            .put("action",input.getString("action")).put("request",new JSONObject(input.toString())).put("requestSha256",MeasurementStore.hash(input.toString())).put("status","pending").put("before",before).put("startTick",physics.tick()).put("createdAt",Instant.now());
    }
    private void stepBody(){physics.step();if(physics.tick()%4==0)sample();}
    private void advanceBody(int count) throws Exception {
        for(int i=0;i<count&&bodyPlan!=null;i++){
            var op=bodyPlan;int next=physics.tick()-op.getInt("startTick")+1;
            if(op.getString("action").equals("move")){
                var from=ObserverGeometry.v(op.getJSONArray("startAnchor"));var target=ObserverGeometry.v(op.getJSONArray("target"));double alpha=Math.min(1,next/(double)op.getInt("travelTicks"));
                physics.target(ObserverGeometry.add(from,ObserverGeometry.mul(ObserverGeometry.sub(target,from),alpha)));
            }
            stepBody();if(next>=op.getInt("totalTicks")){bodyPlan=null;completeBody(op);}
        }
    }
    private void completeBody(JSONObject op) throws Exception {
        var after=physics.snapshot();String action=op.getString("action");op.put("after",after).put("endTick",physics.tick()).put("endedAt",Instant.now()).put("status","completed");
        if(action.equals("move")){double error=distance(position(after),ObserverGeometry.v(op.getJSONArray("target")));op.put("targetErrorM",error).put("reached",error<=op.getDouble("toleranceM")).put("outcome",error<=op.getDouble("toleranceM")?"reached":"blocked-or-force-limited");}
        else op.put("outcome",action.equals("grab")?"held":"released");
        active.getJSONArray("actuatorEvents").put(new JSONObject(op.toString()));active.put("actuatorRevision",active.getInt("actuatorRevision")+1);
        if(action.equals("release")){
            boolean reached=true;for(Object value:active.getJSONArray("actuatorEvents")){var e=(JSONObject)value;if(e.getString("action").equals("move")&&!e.getBoolean("reached"))reached=false;}
            active.put("manipulationEvaluation",new JSONObject().put("released",!physics.held()).put("allMovesReached",reached).put("forceContract","300 N equivalent per-axis impulse clamp; other contact/gravity forces remain active").put("gripKind","native point-to-point constraint"));
            finish("completed");
        }
        commit(List.of(record("workspace.body.operation",op,active.getString("sceneId")),record("workspace.experiment",active,active.getString("sceneId"))));
    }
    private void cancelBody(String reason) throws Exception {
        physics.release();playing=false;
        if(bodyPlan!=null){var op=bodyPlan;bodyPlan=null;op.put("status","cancelled").put("error",reason).put("after",physics.snapshot()).put("endTick",physics.tick());commit(List.of(record("workspace.body.operation",op,active.getString("sceneId"))));}
        active.put("actuatorRevision",active.optInt("actuatorRevision")+1);finish("cancelled");
    }
    public synchronized JSONObject cancelOwnedBody(String observerId,String runId) throws Exception {
        if(active==null||!active.getString("id").equals(runId)||!active.optString("bodyOwner").equals(observerId))throw new IllegalStateException("No matching owned manipulation");
        if(active.getString("status").equals("paused"))cancelBody("Task cancelled");return snapshot();
    }
    private JSONObject replayBody(JSONObject previous) throws Exception {
        if(!previous.getString("status").equals("completed")||previous.optJSONObject("manipulationEvaluation")==null)throw new IllegalStateException("Only completed manipulation runs can be replayed");
        beginDefinition(previous.getJSONObject("definition"),.05);active.put("scope","manipulation").put("replayOf",previous.getString("id")).put("targetTicks",previous.getJSONObject("final").getInt("tick")).put("actuatorEvents",new JSONArray(previous.getJSONArray("actuatorEvents").toString()));
        for(Object value:previous.getJSONArray("actuatorEvents")){
            var op=(JSONObject)value;switch(op.getString("action")){
                case "grab"->physics.grab(op.getJSONObject("request").getString("entityId"),"replay",op.getString("id"));
                case "move"->{var from=ObserverGeometry.v(op.getJSONArray("startAnchor"));var to=ObserverGeometry.v(op.getJSONArray("target"));for(int i=1;i<=op.getInt("totalTicks");i++){physics.target(ObserverGeometry.add(from,ObserverGeometry.mul(ObserverGeometry.sub(to,from),Math.min(1,i/(double)op.getInt("travelTicks")))));stepBody();}}
                case "release"->{physics.release();for(int i=0;i<op.getInt("totalTicks");i++)stepBody();}
                default->throw new IllegalArgumentException("Unsupported recorded actuator event");
            }
        }
        double error=distance(position(physics.snapshot()),position(previous.getJSONObject("final")));
        active.put("manipulationEvaluation",new JSONObject(previous.getJSONObject("manipulationEvaluation").toString())).put("replayEvaluation",new JSONObject().put("sourceRunId",previous.getString("id")).put("positionErrorM",error).put("toleranceM",.0001).put("passed",error<=.0001&&physics.tick()==previous.getJSONObject("final").getInt("tick")));
        finish("completed");return new JSONObject(active.toString());
    }
    public synchronized JSONObject exportScene(String id){return SceneExchange.exportScene(read(id));}
    public synchronized JSONObject importScene(JSONObject input) throws Exception {
        if(!Set.of("package","title").containsAll(input.keySet()))throw new IllegalArgumentException("Unexpected scene import field");
        var file=input.getJSONObject("package");var verified=SceneExchange.validate(file);
        var receipt=new JSONObject().put("id",UUID.randomUUID().toString()).put("sourceSha256",MeasurementStore.hash(file.toString())).put("geometrySha256",file.getString("geometrySha256")).put("source",file.getJSONObject("source")).put("createdAt",Instant.now());
        var scene=save(new JSONObject().put("title",input.optString("title",file.getString("title"))).put("parameters",verified.json()),receipt,"workspace.scene.import");
        return new JSONObject().put("scene",scene).put("receipt",receipt);
    }
    public synchronized JSONArray runs(String scene){var result=new JSONArray();for(var e:world.snapshot().entities())if(type(e).equals("workspace.experiment")){var run=payload(e);if(scene.isBlank()||scene.equals(run.getString("sceneId")))result.put(new JSONObject().put("id",run.getString("id")).put("title",run.getString("title")).put("sceneId",run.getString("sceneId")).put("sceneVersion",run.getInt("sceneVersion")).put("status",run.getString("status")).put("scope",run.optString("scope")).put("evaluation",run.optJSONObject("evaluation")));}return result;}
    @Override public synchronized void close(){clock.shutdownNow();if(physics!=null)physics.close();physics=null;}
}
