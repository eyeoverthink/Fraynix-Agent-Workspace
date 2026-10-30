package fraymus.workspace;

import fraymus.factory.*;
import org.json.*;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Durable one-parameter native search. Validation is scored only after selection freezes. */
public final class CalibrationStore implements AutoCloseable {
    static final String METHOD="native-grid-v1: 9 coarse values, two four-point refinements; training RMSE only";
    private final SchemaRegistry schemas=new SchemaRegistry();
    private final FactoryWorld world;
    private final WorldCheckpointStore checkpoint;
    private final Map<String,JSONObject> jobs=new LinkedHashMap<>();
    private final SceneStore scenes;
    private final MeasurementStore measurements;
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"native-calibration-worker");t.setDaemon(true);return t;});
    private String activeId;
    private AtomicBoolean abort;
    private volatile boolean closing;

    public CalibrationStore(Path project,SceneStore scenes,MeasurementStore measurements) throws Exception {
        this.scenes=scenes;this.measurements=measurements;
        schemas.register(new ComponentSchema("workspace.calibration",1,Map.of("payload",ComponentSchema.Kind.TEXT),Set.of("payload")));
        world=new FactoryWorld(schemas);checkpoint=new WorldCheckpointStore(project.resolve("calibrations/jobs.fworld"));
        Files.createDirectories(checkpoint.path().getParent());
        if(Files.exists(checkpoint.path()))world.restore(checkpoint.load(schemas).snapshot());
        for(var e:world.snapshot().entities()){var job=new JSONObject(e.components().get(0).values().get("payload"));jobs.put(job.getString("id"),job);}
        for(var saved:new ArrayList<>(jobs.values())){
            var job=copy(saved);
            if(Set.of("queued","running").contains(job.getString("status"))){job.put("status","interrupted").put("reason","Application stopped before settlement; explicit resume is available");persist(job);}
            reconcileApplications(job);
        }
    }
    private static JSONObject copy(JSONObject value){return new JSONObject(value.toString());}
    private static double numeric(JSONObject input,String key,double fallback){
        if(!input.has(key))return fallback;Object o=input.get(key);
        if(!(o instanceof Number n)||!Double.isFinite(n.doubleValue()))throw new IllegalArgumentException(key+" must be finite numeric data");return n.doubleValue();
    }
    private static int version(JSONObject input,String key){
        double n=numeric(input,key,-1);if(n<1||n!=Math.rint(n)||n>Integer.MAX_VALUE)throw new IllegalArgumentException(key+" must be a positive integer");return (int)n;
    }
    private synchronized JSONObject require(String id){var job=jobs.get(id);if(job==null)throw new IllegalArgumentException("Unknown calibration job");return copy(job);}
    private synchronized void persist(JSONObject job) throws Exception {
        var existing=jobs.get(job.getString("id"));
        if(existing!=null&&existing.optBoolean("cancelRequested")&&!job.getString("status").equals("queued"))job.put("cancelRequested",true);
        var entity=world.snapshot().entities().stream().filter(e->e.id().toString().equals(job.getString("id"))).findFirst().orElse(null);
        var record=new EntityRecord(UUID.fromString(job.getString("id")),"Calibration "+job.getJSONObject("definition").getString("parameter"),List.of(
            new ComponentRecord(entity==null?UUID.randomUUID():entity.components().get(0).id(),"workspace.calibration",1,Map.of("payload",job.toString()))));
        var before=world.snapshot();var tx=new WorldTransaction(before);
        if(entity==null)tx.add(new WorldTransaction.Create(record));else tx.add(new WorldTransaction.Put(record.id(),record.components().get(0)));
        try{tx.commit(world,schemas);checkpoint.save(world.snapshot(),schemas);jobs.put(job.getString("id"),copy(job));}
        catch(Exception e){world.restore(before);throw e;}
    }
    private static String observationKey(JSONObject row){return decimal(row.getDouble("timeS"))+":"+decimal(row.getDouble("distanceM"));}
    private static String decimal(double value){return BigDecimal.valueOf(value).setScale(12,RoundingMode.HALF_EVEN).stripTrailingZeros().toPlainString();}
    private static JSONObject reference(JSONObject dataset){
        var normalized=new JSONArray();for(Object o:dataset.getJSONArray("rows"))normalized.put(observationKey((JSONObject)o));
        return new JSONObject().put("datasetId",dataset.getString("datasetId")).put("version",dataset.getInt("version")).put("versionId",dataset.getString("id")).put("title",dataset.getString("title"))
            .put("sourceSha256",dataset.getString("sourceSha256")).put("mappingSha256",dataset.getString("mappingSha256")).put("observationsSha256",MeasurementStore.hash(normalized.toString()))
            .put("sourceKind",dataset.getString("sourceKind")).put("sourceNote",dataset.getString("sourceNote")).put("count",dataset.getJSONArray("rows").length());
    }
    private static void checkDataset(JSONObject dataset,double duration){
        var rows=dataset.getJSONArray("rows");
        if(rows.length()<3||rows.length()>512||dataset.getString("csv").getBytes(StandardCharsets.UTF_8).length>131072)
            throw new IllegalArgumentException("Calibration requires 3..512 usable rows and at most 128 KiB per source; import an explicit subset if necessary");
        double minimum=Double.POSITIVE_INFINITY,maximum=Double.NEGATIVE_INFINITY;
        for(Object o:rows){var row=(JSONObject)o;
            if(row.getDouble("timeS")>duration)throw new IllegalArgumentException("Every observation must be covered by the declared native run duration");
            minimum=Math.min(minimum,row.getDouble("distanceM"));maximum=Math.max(maximum,row.getDouble("distanceM"));}
        if(maximum-minimum<.00001)throw new IllegalArgumentException("Calibration needs observable displacement; stationary observations cannot identify this parameter");
    }
    public synchronized JSONObject start(JSONObject input) throws Exception {
        Set<String> keys=Set.of("trainingSceneId","trainingSceneVersion","validationSceneId","validationSceneVersion","trainingDatasetId","trainingDatasetVersion","validationDatasetId","validationDatasetVersion","parameter","minimum","maximum","seconds","toleranceM","minImprovementM");
        if(!keys.containsAll(input.keySet()))throw new IllegalArgumentException("Unsupported calibration field");
        if(activeId!=null||closing)throw new IllegalStateException("One calibration job can run at a time");
        String parameter=input.getString("parameter");
        if(!Set.of("friction","gravityMps2").contains(parameter))throw new IllegalArgumentException("Choose friction or gravityMps2; one parameter is fitted at a time");
        double low=numeric(input,"minimum",Double.NaN),high=numeric(input,"maximum",Double.NaN),seconds=numeric(input,"seconds",.75),tolerance=numeric(input,"toleranceM",Double.NaN),improvement=numeric(input,"minImprovementM",.0005);
        RampDefinition.range(seconds,.05,10,"seconds");double duration=Math.round(seconds/BulletRamp.DT)*BulletRamp.DT;
        double parameterMin=parameter.equals("friction")?0:.1,parameterMax=parameter.equals("friction")?1:30;
        if(!Double.isFinite(low)||!Double.isFinite(high)||low<parameterMin||high>parameterMax||low>=high)throw new IllegalArgumentException("Declare ordered parameter bounds inside "+parameterMin+".."+parameterMax);
        if(!Double.isFinite(tolerance)||tolerance<0||tolerance>10||!Double.isFinite(improvement)||improvement<=0||improvement>10)throw new IllegalArgumentException("Declare tolerance 0..10 m and positive minimum training improvement up to 10 m");
        var trainScene=scenes.read(input.getString("trainingSceneId"));var validationScene=scenes.read(input.getString("validationSceneId"));
        if(trainScene.getInt("version")!=version(input,"trainingSceneVersion")||validationScene.getInt("version")!=version(input,"validationSceneVersion"))throw new ConcurrentModificationException("Scene binding changed before calibration");
        var train=measurements.read(input.getString("trainingDatasetId"),version(input,"trainingDatasetVersion"));
        var validation=measurements.read(input.getString("validationDatasetId"),version(input,"validationDatasetVersion"));
        checkDataset(train,duration);checkDataset(validation,duration);var tr=reference(train);var vr=reference(validation);
        measurements.withCurrentVersions(tr,vr,()->true);
        if(tr.getString("datasetId").equals(vr.getString("datasetId"))||tr.getString("observationsSha256").equals(vr.getString("observationsSha256")))
            throw new IllegalArgumentException("Training and validation require distinct datasets and distinct normalized observations");
        var used=new HashSet<String>();
        for(Object o:train.getJSONArray("rows")){var row=(JSONObject)o;if(row.getDouble("timeS")!=0||row.getDouble("distanceM")!=0)used.add(observationKey(row));}
        for(Object o:validation.getJSONArray("rows"))if(used.contains(observationKey((JSONObject)o)))throw new IllegalArgumentException("Training and validation share observation records beyond the common zero origin");
        int exposure=0;
        for(var previous:jobs.values()){
            var d=previous.getJSONObject("definition");String digest=vr.getString("observationsSha256");
            if(previous.has("baselineTraining")&&d.getJSONObject("training").getString("observationsSha256").equals(digest))exposure++;
            if(previous.has("validationStartedAt")&&d.getJSONObject("validation").getString("observationsSha256").equals(digest))exposure++;
        }
        for(Object o:measurements.comparisons("")){var c=(JSONObject)o;if(c.getString("sourceSha256").equals(vr.getString("sourceSha256")))exposure++;}
        var definition=new JSONObject().put("parameter",parameter).put("minimum",low).put("maximum",high).put("seconds",seconds).put("actualDurationS",duration).put("toleranceM",tolerance).put("minImprovementM",improvement)
            .put("training",tr).put("validation",vr).put("trainingScene",trainScene).put("validationScene",validationScene).put("validationPriorUseCount",exposure).put("method",METHOD).put("backend",BulletRamp.BACKEND)
            .put("limits",new JSONObject().put("nativeRuns",20).put("wallTimePerAttemptMs",60000).put("rowsPerDataset",512));
        var values=new JSONArray();for(int i=0;i<9;i++)values.put(low+(high-low)*i/8);
        var job=new JSONObject().put("id",UUID.randomUUID().toString()).put("schemaVersion",1).put("status","queued").put("phase","baseline").put("createdAt",Instant.now()).put("definition",definition)
            .put("candidateValues",values).put("trials",new JSONArray()).put("refinementRound",0).put("attempts",0).put("completedNativeOperations",0);
        persist(job);dispatch(job);return read(job.getString("id"));
    }
    private void dispatch(JSONObject job){activeId=job.getString("id");abort=new AtomicBoolean();var signal=abort;worker.submit(()->execute(job,signal));}
    private JSONObject dataset(JSONObject ref){return measurements.read(ref.getString("datasetId"),ref.getInt("version"));}
    private void checkAbort(AtomicBoolean signal,long deadline){
        if(closing||signal.get()||Thread.currentThread().isInterrupted())throw new CancellationException("Calibration interrupted");
        if(System.nanoTime()>deadline)throw new IllegalStateException("Calibration exceeded its 60-second attempt budget");
    }
    private JSONObject evaluate(JSONObject job,String phase,JSONObject sourceScene,JSONObject observations,double value,AtomicBoolean signal,long deadline) throws Exception {
        checkAbort(signal,deadline);var d=job.getJSONObject("definition");String parameter=d.getString("parameter");var definition=copy(sourceScene);
        definition.getJSONObject("parameters").put(parameter,value);
        definition.put("calibrationOverride",new JSONObject().put("jobId",job.getString("id")).put("parameter",parameter).put("value",value).put("phase",phase));
        var run=scenes.calibrationRun(definition,d.getDouble("seconds"),job.getString("id")+":"+phase,job.getString("id"),phase,()->{checkAbort(signal,deadline);return false;});
        checkAbort(signal,deadline);
        return MeasurementEvaluation.evaluate(observations,run,d.getDouble("toleranceM")).put("runId",run.getString("id")).put("value",value);
    }
    private static JSONObject best(JSONObject job){
        double baseline=job.getJSONObject("definition").getJSONObject("trainingScene").getJSONObject("parameters").getDouble(job.getJSONObject("definition").getString("parameter"));JSONObject best=null;
        for(Object o:job.getJSONArray("trials")){var trial=(JSONObject)o;
            if(best==null||trial.getJSONObject("metrics").getDouble("rmseM")<best.getJSONObject("metrics").getDouble("rmseM")||
                (trial.getJSONObject("metrics").getDouble("rmseM")==best.getJSONObject("metrics").getDouble("rmseM")&&Math.abs(trial.getDouble("value")-baseline)<Math.abs(best.getDouble("value")-baseline)))best=trial;}
        if(best==null)throw new IllegalStateException("No executed candidate is available");return best;
    }
    private void execute(JSONObject job,AtomicBoolean signal){
        String id=job.getString("id");long deadline=System.nanoTime()+60_000_000_000L;
        try{
            var d=job.getJSONObject("definition");
            if(!METHOD.equals(d.getString("method"))||!BulletRamp.BACKEND.equals(d.getString("backend")))throw new IllegalStateException("Calibration method/backend changed; start a new job");
            job.put("status","running").put("attempts",job.getInt("attempts")+1).put("startedAt",Instant.now());persist(job);
            var training=dataset(d.getJSONObject("training"));String parameter=d.getString("parameter");var trainingScene=d.getJSONObject("trainingScene");
            if(!job.has("baselineTraining")){
                job.put("phase","baseline");persist(job);
                job.put("baselineTraining",evaluate(job,"training-baseline",trainingScene,training,trainingScene.getJSONObject("parameters").getDouble(parameter),signal,deadline));
                job.put("completedNativeOperations",1);persist(job);
            }
            while(!job.has("selection")){
                var values=job.getJSONArray("candidateValues");var trials=job.getJSONArray("trials");
                while(trials.length()<values.length()){
                    int index=trials.length();job.put("phase","search").put("currentValue",values.getDouble(index));persist(job);
                    var evaluation=evaluate(job,"candidate-"+index,trainingScene,training,values.getDouble(index),signal,deadline);
                    trials.put(new JSONObject().put("index",index).put("value",values.getDouble(index)).put("runId",evaluation.getString("runId")).put("metrics",MeasurementEvaluation.summary(evaluation)));
                    job.put("completedNativeOperations",1+trials.length());persist(job);
                }
                int round=job.getInt("refinementRound");var winner=best(job);
                if(round>=2){
                    job.put("selection",new JSONObject().put("value",winner.getDouble("value")).put("runId",winner.getString("runId")).put("trainingRmseM",winner.getJSONObject("metrics").getDouble("rmseM")).put("frozenAt",Instant.now()).put("criterion","minimum executed training RMSE; ties prefer the existing parameter"));
                    job.put("phase","selection-frozen");persist(job);break;
                }
                double step=(d.getDouble("maximum")-d.getDouble("minimum"))/8/Math.pow(4,round),center=winner.getDouble("value");
                for(double multiplier:new double[]{-.5,-.25,.25,.5}){
                    double value=Math.max(d.getDouble("minimum"),Math.min(d.getDouble("maximum"),center+step*multiplier));boolean duplicate=false;
                    for(Object old:values)if(Math.abs(((Number)old).doubleValue()-value)<1e-12)duplicate=true;if(!duplicate)values.put(value);
                }
                job.put("refinementRound",round+1);persist(job);
            }
            checkAbort(signal,deadline);double selected=job.getJSONObject("selection").getDouble("value");
            if(!job.has("fittedTraining")){
                var run=scenes.runRecord(job.getJSONObject("selection").getString("runId"));
                job.put("fittedTraining",MeasurementEvaluation.evaluate(training,run,d.getDouble("toleranceM")).put("runId",run.getString("id")).put("value",selected));persist(job);
            }
            // Validation is scored only after the training selection is durable.
            if(!job.has("validationStartedAt")){job.put("validationStartedAt",Instant.now()).put("phase","validation");persist(job);}
            var validation=dataset(d.getJSONObject("validation"));var validationScene=d.getJSONObject("validationScene");
            if(!job.has("validationBaseline")){
                job.put("validationBaseline",evaluate(job,"validation-baseline",validationScene,validation,validationScene.getJSONObject("parameters").getDouble(parameter),signal,deadline));
                job.put("completedNativeOperations",2+job.getJSONArray("trials").length());persist(job);
            }
            if(!job.has("validationFitted")){
                job.put("validationFitted",evaluate(job,"validation-fitted",validationScene,validation,selected,signal,deadline));
                job.put("completedNativeOperations",3+job.getJSONArray("trials").length());persist(job);
            }
            checkAbort(signal,deadline);
            double gain=job.getJSONObject("baselineTraining").getDouble("rmseM")-job.getJSONObject("fittedTraining").getDouble("rmseM");
            boolean improves=gain>=d.getDouble("minImprovementM"),within=job.getJSONObject("validationFitted").getBoolean("passed"),
                notWorse=job.getJSONObject("validationFitted").getDouble("rmseM")<=job.getJSONObject("validationBaseline").getDouble("rmseM")+1e-9;
            job.put("evaluation",new JSONObject().put("accepted",improves&&within&&notWorse).put("trainingImprovementM",gain).put("trainingImproved",improves).put("validationWithinTolerance",within).put("validationNoRegression",notWorse)
                .put("criteria","training RMSE gain >= declared minimum; every validation residual <= tolerance; validation RMSE does not increase beyond 1e-9 m numerical slack")
                .put("scope","Parameter calibration in the recorded native model; source labels are user supplied, prior validation uses recorded, and collection independence not proven"));
            job.put("status","completed").put("phase","complete").put("endedAt",Instant.now());persist(job);
        }catch(Exception|LinkageError error){
            job.put("status",closing?"interrupted":signal.get()?"cancelled":"failed").put("reason",String.valueOf(error.getMessage())).put("endedAt",Instant.now());
            try{persist(job);}catch(Exception persistError){System.err.println("Calibration checkpoint failed: "+persistError.getMessage());}
        }finally{synchronized(this){if(id.equals(activeId)){activeId=null;abort=null;}}}
    }
    public synchronized JSONObject cancel(String id) throws Exception {
        var job=require(id);if(id.equals(activeId)&&abort!=null){abort.set(true);job.put("cancelRequested",true);persist(job);}return read(id);
    }
    public synchronized JSONObject resume(String id) throws Exception {
        if(activeId!=null||closing)throw new IllegalStateException("One calibration job can run at a time");var job=require(id);
        if(!Set.of("interrupted","cancelled").contains(job.getString("status")))throw new IllegalStateException("Only interrupted or cancelled jobs can resume");
        job.put("cancelRequested",false).put("status","queued").remove("reason");persist(job);dispatch(job);return read(id);
    }
    private boolean current(JSONObject job){
        try{var d=job.getJSONObject("definition");
            for(String role:List.of("training","validation"))if(measurements.read(d.getJSONObject(role).getString("datasetId"),0).getInt("version")!=d.getJSONObject(role).getInt("version"))return false;
            for(String role:List.of("trainingScene","validationScene"))if(scenes.read(d.getJSONObject(role).getString("id")).getInt("version")!=d.getJSONObject(role).getInt("version"))return false;return true;
        }catch(Exception e){return false;}
    }
    private void reconcileApplications(JSONObject job) throws Exception {
        if(!job.has("application"))return;var app=job.getJSONObject("application");boolean changed=false;
        for(String phase:List.of("apply","rollback"))if(app.has(phase+"OperationId")){
            var receipt=scenes.calibrationReceipt(app.getString(phase+"OperationId"));
            if(receipt!=null&&!app.has(phase+"Receipt")){app.put(phase+"Receipt",receipt).put("status",phase.equals("apply")?"applied":"rolled-back");changed=true;}
        }
        if(changed)persist(job);
    }
    public synchronized JSONObject read(String id){
        var job=require(id);var app=job.optJSONObject("application");boolean applied=app!=null&&app.has("applyReceipt");boolean bindings=current(job);
        job.put("bindingsCurrent",bindings).put("canApply",activeId==null&&job.getString("status").equals("completed")&&job.getJSONObject("evaluation").getBoolean("accepted")&&!applied&&bindings);
        boolean rollback=false;
        if(applied&&!app.has("rollbackReceipt"))try{rollback=scenes.read(app.getJSONObject("applyReceipt").getString("sceneId")).getInt("version")==app.getJSONObject("applyReceipt").getInt("toVersion");}catch(Exception ignored){}
        return job.put("canRollback",activeId==null&&rollback);
    }
    public synchronized JSONArray list(){
        var out=new JSONArray();for(var job:jobs.values().stream().sorted(Comparator.comparing(j->j.getString("createdAt"))).toList()){var d=job.getJSONObject("definition");
            var value=new JSONObject().put("id",job.getString("id")).put("status",job.getString("status")).put("parameter",d.getString("parameter")).put("sceneId",d.getJSONObject("trainingScene").getString("id")).put("createdAt",job.get("createdAt"));
            if(job.has("evaluation"))value.put("accepted",job.getJSONObject("evaluation").getBoolean("accepted"));out.put(value);}return out;
    }
    public synchronized JSONObject apply(String id) throws Exception {
        var job=require(id);reconcileApplications(job);job=require(id);var app=job.optJSONObject("application");
        if(app!=null&&app.has("applyReceipt"))return read(id);
        if(activeId!=null||!job.getString("status").equals("completed")||!job.getJSONObject("evaluation").getBoolean("accepted"))throw new IllegalStateException("Only completed, accepted calibration can be applied while no job is running");
        if(!current(job))throw new ConcurrentModificationException("Calibration bindings changed; start a new fit");
        if(app==null){app=new JSONObject().put("applyOperationId",UUID.randomUUID().toString()).put("status","apply-pending");job.put("application",app);persist(job);}
        var d=job.getJSONObject("definition");final String operation=app.getString("applyOperationId");final JSONObject definition=d;final double value=job.getJSONObject("selection").getDouble("value");
        var receipt=measurements.withCurrentVersions(d.getJSONObject("training"),d.getJSONObject("validation"),()->scenes.applyCalibration(id,operation,definition.getJSONObject("trainingScene").getString("id"),definition.getJSONObject("trainingScene").getInt("version"),definition.getJSONObject("validationScene").getString("id"),definition.getJSONObject("validationScene").getInt("version"),definition.getString("parameter"),value));
        app.put("applyReceipt",receipt).put("status","applied");persist(job);return read(id);
    }
    public synchronized JSONObject rollback(String id) throws Exception {
        var job=require(id);reconcileApplications(job);job=require(id);var app=job.optJSONObject("application");
        if(app==null||!app.has("applyReceipt"))throw new IllegalStateException("Calibration has not been applied");if(app.has("rollbackReceipt"))return read(id);
        if(activeId!=null)throw new IllegalStateException("Wait for the running calibration before rollback");
        if(!app.has("rollbackOperationId")){app.put("rollbackOperationId",UUID.randomUUID().toString()).put("status","rollback-pending");persist(job);}
        var receipt=scenes.rollbackCalibration(id,app.getString("rollbackOperationId"),app.getString("applyOperationId"));app.put("rollbackReceipt",receipt).put("status","rolled-back");persist(job);return read(id);
    }
    public synchronized JSONObject report(String id){
        var job=read(id);if(!job.getString("status").equals("completed"))throw new IllegalStateException("A completed calibration is required for a report");
        return CalibrationReport.build(job,dataset(job.getJSONObject("definition").getJSONObject("training")),dataset(job.getJSONObject("definition").getJSONObject("validation")));
    }
    @Override public void close(){
        closing=true;synchronized(this){if(abort!=null)abort.set(true);}worker.shutdown();
        try{if(!worker.awaitTermination(10,TimeUnit.SECONDS))worker.shutdownNow();}catch(InterruptedException e){worker.shutdownNow();Thread.currentThread().interrupt();}
    }
}
