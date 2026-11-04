package fraymus.workspace;
import fraymus.factory.*;
import fraymus.neural.*;
import fraymus.neural.gpu.GlNeuralCompute;
import brain_architecture.kernel.MerkleAppendOnlyJournal;
import org.json.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/** Java-owned durable neural jobs, real native experience and gated policy promotion. */
public final class BrainService implements AutoCloseable {
    private final Path root;private final SchemaRegistry schemas=new SchemaRegistry();private final FactoryWorld world;private final WorldCheckpointStore checkpoint;
    private final MerkleAppendOnlyJournal journal;private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"java-brain-coordinator");t.setDaemon(true);return t;});
    private volatile boolean cancel,closed;private String busyId;private NeuromorphicCore cortex;
    static final String PROTOCOL="native-push-readout-v1";
    public BrainService(Path project) throws Exception {
        root=project.resolve("brain");Files.createDirectories(root);
        for(String type:List.of("job","policy","feedback","worldmodel"))schemas.register(new ComponentSchema("workspace.brain."+type,1,Map.of("payload",ComponentSchema.Kind.TEXT),Set.of("payload")));
        world=new FactoryWorld(schemas);checkpoint=new WorldCheckpointStore(root.resolve("tasks.fworld"));if(Files.exists(checkpoint.path()))world.restore(checkpoint.load(schemas).snapshot());
        journal=new MerkleAppendOnlyJournal(root.resolve("journal"));
        var recover=new ArrayList<JSONObject>();for(var e:world.snapshot().entities())if(e.components().get(0).type().endsWith(".job")){var job=value(e);if(Set.of("queued","running").contains(job.getString("status"))){job.put("status","interrupted").put("reason","Coordinator process ended; native trials are not silently resumed");recover.add(job);}}
        for(var job:recover)save("job",job);
    }
    private static JSONObject value(EntityRecord e){return new JSONObject(e.components().get(0).values().get("payload"));}
    private synchronized List<JSONObject> values(String type){var out=new ArrayList<JSONObject>();for(var e:world.snapshot().entities())if(e.components().get(0).type().equals("workspace.brain."+type))out.add(value(e));return out;}
    private synchronized void save(String type,JSONObject value) throws Exception {saveMany(Map.of(type,value));}
    private synchronized void saveMany(Map<String,JSONObject> records) throws Exception {
        var before=world.snapshot();var tx=new WorldTransaction(before);
        for(var entry:records.entrySet()){
            String type=entry.getKey();JSONObject value=entry.getValue();UUID id=UUID.fromString(value.getString("id"));var existing=before.entities().stream().filter(e->e.id().equals(id)).findFirst();
            var component=new ComponentRecord(existing.map(e->e.components().get(0).id()).orElse(UUID.randomUUID()),"workspace.brain."+type,1,Map.of("payload",value.toString()));
            if(existing.isEmpty())tx.add(new WorldTransaction.Create(new EntityRecord(id,type,List.of(component))));else tx.add(new WorldTransaction.Put(id,component));
        }
        try{tx.commit(world,schemas);checkpoint.save(world.snapshot(),schemas);}catch(Exception error){world.restore(before);throw error;}
    }
    private synchronized void commitPromotion(JSONObject job,JSONObject policy,String type) throws Exception {
        checkCancel();journal.append(type+"-promotion-intent","Validated policy and completed job will commit together",Map.of("jobId",job.getString("id"),"policySha256",MeasurementStore.hash(policy.toString())));
        job.put("status","completed").put("phase","completed").put("endedAt",Instant.now());
        try{saveMany(Map.of("job",job,type,policy));}catch(Exception error){job.put("status","running");throw error;}
    }
    public synchronized JSONObject read(String id){return values("job").stream().filter(j->j.getString("id").equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown brain job"));}
    public synchronized JSONObject activePolicy(){
        var policies=values("policy");return policies.stream().max(Comparator.comparingInt(p->p.getInt("version"))).orElseGet(()->new JSONObject().put("version",1).put("weights",new JSONArray(new float[4])).put("kind","zero-push baseline"));
    }
    public synchronized JSONObject state() throws Exception {
        var jobs=new JSONArray();for(var j:values("job"))jobs.put(new JSONObject().put("id",j.getString("id")).put("kind",j.getString("kind")).put("status",j.getString("status")).put("phase",j.optString("phase")).put("progress",j.optInt("progress")).put("total",j.optInt("total")).put("accepted",j.optBoolean("accepted")).put("reason",j.optString("reason")));
        var core=new JSONObject().put("module","shared brain-core").put("ready",cortex!=null).put("neurons",cortex==null?0:cortex.neurons()).put("synapses",cortex==null?0:cortex.synapses()).put("ticks",cortex==null?0:cortex.ticks()).put("spikingBackend","Java CPU");
        return new JSONObject().put("jobs",jobs).put("busy",busyId!=null).put("activePolicy",activePolicy()).put("activeFeedback",activeFeedback()).put("activeWorldModel",activeWorldModel()).put("cortex",core).put("journalEntries",journal.verify().entries()).put("protocol",PROTOCOL).put("fraynixGPT061",outcomeState061()).put("residual062",outcomeState062()).put("stopping063",stoppingState063());
    }
    public synchronized JSONObject start(JSONObject input) throws Exception { return start(input,UUID.randomUUID().toString()); }
    synchronized JSONObject startOwned(String id) throws Exception {
        UUID.fromString(id);for(var j:values("job"))if(j.getString("id").equals(id))return j;
        return start(new JSONObject().put("kind","world-model-train").put("autoPromote",true),id);
    }
    private JSONObject start(JSONObject input,String id) throws Exception {
        if(closed)throw new IllegalStateException("Java coordinator is closed");if(!Set.of("kind","autoPromote").containsAll(input.keySet()))throw new IllegalArgumentException("Unsupported brain job field");
        if(busyId!=null)throw new IllegalStateException("One Java brain job is active; cancel or wait");if(values("job").size()>=16)throw new IllegalStateException("Brain job history budget reached (16)");
        String kind=input.getString("kind");if(!Set.of("train","gpu-check","pulse","feedback-train","world-model-train","fraynix-outcomes","physics-residuals","stopping-uncertainty").contains(kind))throw new IllegalArgumentException("Unknown brain job kind");
        var job=new JSONObject().put("id",id).put("kind",kind).put("status","queued").put("phase","queued").put("createdAt",Instant.now()).put("activeVersionAtStart",activePolicy().getInt("version")).put("autoPromote",input.optBoolean("autoPromote",true)).put("protocol",PROTOCOL).put("feedbackVersionAtStart",activeFeedback().getInt("version")).put("worldVersionAtStart",activeWorldModel().getInt("version"));
        if(kind.equals("feedback-train"))job.put("protocol",FeedbackExperiment.PROTOCOL).put("feedbackProtocol",FeedbackExperiment.manifest());
        if(kind.equals("world-model-train"))job.put("protocol",WorldModelExperiment.PROTOCOL).put("worldProtocol",WorldModelExperiment.manifest());
        save("job",job);busyId=job.getString("id");cancel=false;
        var receipt=new JSONObject(job.toString());
        worker.submit(()->{
            try{job.put("status","running");update(job,"started");switch(kind){case "stopping-uncertainty"->Stopping063.run(job,root.resolve("native"),this::checkCancel,phase->update(job,phase));case "physics-residuals"->ResidualOutcome062.train(job,root.resolve("native"),this::checkCancel,phase->update(job,phase));case "fraynix-outcomes"->FraynixOutcome061.train(job,root.resolve("native"),this::checkCancel,phase->update(job,phase));case "train"->train(job);case "feedback-train"->trainFeedback(job);case "world-model-train"->trainWorldModel(job);case "gpu-check"->gpuCheck(job);case "pulse"->pulse(job);}synchronized(this){if(!job.getString("status").equals("completed")){checkCancel();job.put("status","completed").put("endedAt",Instant.now());update(job,"completed");}}}
            catch(Throwable error){job.remove("promotion");job.remove("feedbackPromotion");job.remove("worldPromotion");job.put("status",cancel?"cancelled":"failed").put("reason",String.valueOf(error.getMessage())).put("endedAt",Instant.now());try{update(job,"settled");}catch(Exception ignored){}}
            finally{synchronized(this){busyId=null;}}
        });
        return receipt;
    }
    public synchronized JSONObject outcomeState061(){
        var response=new JSONObject().put("available",false).put("advisory",true).put("protocol",FraynixOutcome061.PROTOCOL)
            .put("meaning","FraynixGPT numerical cortex + native outcome readout; language-model weights unchanged; no controller authority");
        var jobs=values("job");for(int i=jobs.size()-1;i>=0;i--){var j=jobs.get(i);if(!j.optString("kind").equals("fraynix-outcomes"))continue;
            response.put("jobId",j.getString("id")).put("status",j.getString("status")).put("progress",j.optInt("progress")).put("total",36).put("reason",j.optString("reason"));
            if(j.has("outcomeEvaluation061"))response.put("evaluation",j.getJSONObject("outcomeEvaluation061")).put("modelSha256",j.getString("modelSha256061"));
            response.put("available",j.getString("status").equals("completed"));break;
        }return response;
    }
    public synchronized JSONObject outcomePredict061(JSONObject input)throws Exception{
        SpatialScene.keys(input,"jobId","definition");if(busyId!=null)throw new IllegalStateException("Wait for the active Java brain job");
        var j=read(input.getString("jobId"));if(!j.getString("kind").equals("fraynix-outcomes")||!j.getString("status").equals("completed"))throw new IllegalStateException("Choose a completed outcome experiment");
        FraynixOutcome061.audit(j);return new JSONObject().put("advisory",true).put("qualified",j.getJSONObject("outcomeEvaluation061").getBoolean("qualified"))
            .put("modelSha256",j.getString("modelSha256061")).put("predictionM",FraynixOutcome061.predictions(j.getJSONObject("outcomeModel061"),input.getJSONObject("definition")));
    }
    private JSONObject residualSummary062(JSONObject j){
        var o=new JSONObject().put("available",j.getString("status").equals("completed")).put("advisory",true)
            .put("jobId",j.getString("id")).put("status",j.getString("status")).put("progress",j.optInt("progress")).put("total",60).put("reason",j.optString("reason"));
        if(j.has("decision062"))o.put("decision",j.getJSONObject("decision062"));
        if(j.has("modelSha256062"))o.put("modelSha256",j.getString("modelSha256062"));
        if(j.has("evaluation062"))o.put("evaluation",j.getJSONObject("evaluation062"));return o;
    }
    public synchronized JSONObject outcomeState062(){
        var jobs=values("job").stream().filter(j->j.optString("kind").equals("physics-residuals"))
            .sorted(Comparator.comparing(j->j.getString("createdAt"))).toList();
        var o=jobs.isEmpty()?new JSONObject().put("available",false).put("advisory",true):residualSummary062(jobs.get(jobs.size()-1));
        o.put("protocol",ResidualOutcome062.PROTOCOL).put("meaning","Analytical physics plus learned numerical residuals. No controller authority or language-model weight update.");
        for(int i=jobs.size()-1;i>=0;i--)if(jobs.get(i).getString("status").equals("completed")){o.put("lastCompleted",residualSummary062(jobs.get(i)));break;}return o;
    }
    public synchronized JSONObject residualPredict062(JSONObject input)throws Exception{
        SpatialScene.keys(input,"jobId","definition");if(busyId!=null)throw new IllegalStateException("Wait for the active Java brain job");
        var j=read(input.getString("jobId"));if(!j.getString("kind").equals("physics-residuals")||!j.getString("status").equals("completed"))throw new IllegalStateException("Choose a completed residual experiment");
        ResidualOutcome062.audit(j);var predictions=ResidualOutcome062.predictions(j.getJSONObject("model062"),input.getJSONObject("definition"));String selected=j.getJSONObject("decision062").getString("method");
        return new JSONObject().put("advisory",true).put("qualified",j.getJSONObject("evaluation062").getBoolean("qualified"))
            .put("selectedMethod",selected).put("selectedPredictionM",predictions.getDouble(selected)).put("predictionM",predictions)
            .put("modelSha256",j.getString("modelSha256062")).put("decisionSha256",j.getString("decisionSha256062"));
    }
    private JSONObject stoppingSummary063(JSONObject j){
        var o=new JSONObject().put("available",j.getString("status").equals("completed")).put("advisory",true).put("jobId",j.getString("id")).put("status",j.getString("status")).put("progress",j.optInt("progress")).put("total",Stopping063.TOTAL).put("reason",j.optString("reason"));
        if(j.has("intervalModel063"))o.put("intervalModel",j.getJSONObject("intervalModel063")).put("modelSha256",j.getString("intervalModelSha256063"));
        if(j.has("evaluation063"))o.put("evaluation",j.getJSONObject("evaluation063"));
        if(j.has("diagnostics063")){var traces=new JSONArray();for(Object raw:j.getJSONArray("diagnostics063")){var row=(JSONObject)raw;if(row.has("record"))traces.put(new JSONObject().put("definition",row.getJSONObject("definition")).put("variant",row.getString("variant")).put("summary",row.getJSONObject("record").getJSONObject("summary")));}o.put("traces",traces);}return o;
    }
    public synchronized JSONObject stoppingState063(){
        var jobs=values("job").stream().filter(j->j.optString("kind").equals("stopping-uncertainty")).sorted(Comparator.comparing(j->j.getString("createdAt"))).toList();
        var o=jobs.isEmpty()?new JSONObject().put("available",false):stoppingSummary063(jobs.get(jobs.size()-1));o.put("protocol",Stopping063.PROTOCOL).put("meaning","Native stopping interventions and empirical uncertainty; no physical coverage guarantee or controller authority");
        for(int i=jobs.size()-1;i>=0;i--)if(jobs.get(i).getString("status").equals("completed")){o.put("lastCompleted",stoppingSummary063(jobs.get(i)));break;}return o;
    }
    private JSONObject completedStopping063(String id){
        if(busyId!=null)throw new IllegalStateException("Wait for the active Java brain job");var j=read(id);if(!j.getString("kind").equals("stopping-uncertainty")||!j.getString("status").equals("completed"))throw new IllegalStateException("Choose a completed stopping study");Stopping063.audit(j);return j;
    }
    public synchronized JSONObject stoppingPredict063(JSONObject input){
        SpatialScene.keys(input,"jobId","definition");var j=completedStopping063(input.getString("jobId"));return new JSONObject().put("advisory",true).put("qualified",j.getJSONObject("evaluation063").getBoolean("qualified")).put("modelSha256",j.getString("intervalModelSha256063")).put("intervals",Stopping063.intervals(j.getJSONObject("intervalModel063").getJSONObject("radiiM"),input.getJSONObject("definition"))).put("meaning","Nominal90% calibrated intervals; finite simulation evaluation, not a real-world confidence guarantee");
    }
    public synchronized JSONObject stoppingTrace063(JSONObject input){
        SpatialScene.keys(input,"jobId","traceIndex");int index=input.getInt("traceIndex");if(index<0||index>=18||input.getDouble("traceIndex")!=index)throw new IllegalArgumentException("Trace index must be0–17");var j=completedStopping063(input.getString("jobId"));var row=j.getJSONArray("diagnostics063").getJSONObject(index);var series=new JSONArray();for(Object raw:row.getJSONObject("record").getJSONArray("samples"))series.put(Stopping063.projected(row.getJSONObject("definition"),(JSONObject)raw));return new JSONObject().put("definition",row.getJSONObject("definition")).put("variant",row.getString("variant")).put("summary",row.getJSONObject("record").getJSONObject("summary")).put("series",series).put("recordSha256",row.getJSONObject("record").getString("sha256"));
    }
    private void checkCancel(){if(cancel||closed||Thread.currentThread().isInterrupted())throw new CancellationException("Java brain job cancelled");}
    private synchronized void update(JSONObject job,String phase) throws Exception {job.put("phase",phase);save("job",job);journal.append("brain-job",phase,Map.of("id",job.getString("id"),"status",job.getString("status"),"payloadSha256",MeasurementStore.hash(job.toString())));}
    public synchronized JSONObject cancel(String id) throws Exception {if(!Objects.equals(busyId,id)||!Set.of("queued","running").contains(read(id).getString("status")))throw new IllegalStateException("This job is not active or has already committed");cancel=true;return read(id);}
    static float[] features(JSONObject c){double angle=Math.toRadians(c.getDouble("angleDeg")),g=c.getDouble("gravityMps2");return new float[]{1,(float)(c.getDouble("targetM")/2),(float)(g*Math.sin(angle)/10),(float)(g*c.getDouble("friction")*Math.cos(angle)/10)};}
    private static List<JSONObject> cases(boolean training){
        var out=new ArrayList<JSONObject>();int i=0;for(double angle:training?new double[]{10,20,30,40}:new double[]{15,25,35})for(double friction:training?new double[]{0,.15,.3}:new double[]{.05,.25}){
            double g=training?new double[]{8,9.81,12}[i%3]:new double[]{9.2,10.5}[i%2],target=training?.4+.3*(i%5):new double[]{.6,1.6}[i%2];
            out.add(new JSONObject().put("angleDeg",angle).put("friction",friction).put("gravityMps2",g).put("massKg",training?new double[]{.5,1,2}[i%3]:new double[]{.75,1.5}[i%2]).put("targetM",target).put("seconds",.75));i++;
        }return out;
    }
    private JSONObject trial(JSONObject c,double dv) throws Exception {
        checkCancel();var config=RampDefinition.from(new JSONObject().put("angleDeg",c.getDouble("angleDeg")).put("friction",c.getDouble("friction")).put("massKg",c.getDouble("massKg")).put("gravityMps2",c.getDouble("gravityMps2")));
        var ids=new JSONObject().put("ramp",UUID.randomUUID().toString()).put("block",UUID.randomUUID().toString()).put("ground",UUID.randomUUID().toString());
        try(var physics=new BulletRamp(config,ids,root.resolve("native"))){physics.push(dv);var samples=new JSONArray().put(physics.sample());for(int t=0;t<180;t++){checkCancel();physics.step();if(physics.tick()%4==0)samples.put(physics.sample());}
            var state=physics.snapshot();return new JSONObject().put("id",UUID.randomUUID().toString()).put("definition",new JSONObject(c.toString())).put("deltaVelocityMps",dv).put("impulseNs",c.getDouble("massKg")*dv).put("backend",BulletRamp.BACKEND).put("dt",BulletRamp.DT).put("final",state).put("samples",samples).put("errorM",Math.abs(state.getDouble("distanceM")-c.getDouble("targetM")));
        }
    }
    private void train(JSONObject job) throws Exception {
        var training=cases(true);var validation=cases(false);job.put("trainingDefinitions",new JSONArray(training)).put("validationDefinitions",new JSONArray(validation)).put("selectionRule","Training only: minimum absolute native endpoint error over -2..4 m/s, step 0.5").put("acceptanceRule",new JSONObject().put("minRelativeMeanGain",.3).put("maxValidationErrorM",.25)).put("featureContract","[1, target/2, gravity*sin(angle)/10, gravity*friction*cos(angle)/10]; engineered physical features").put("total",180).put("progress",0).put("training",new JSONArray()).put("validation",new JSONArray());
        update(job,"collecting native training outcomes");float[][] x=new float[training.size()][];float[] y=new float[training.size()];int index=0;
        for(var c:training){var trials=new JSONArray();JSONObject best=null;for(int k=0;k<=12;k++){var sample=trial(c,-2+k*.5);trials.put(sample);job.put("progress",job.getInt("progress")+1);if(best==null||sample.getDouble("errorM")<best.getDouble("errorM"))best=sample;}
            x[index]=features(c);y[index]=(float)(best.getDouble("deltaVelocityMps")/4);job.getJSONArray("training").put(new JSONObject().put("case",c).put("trials",trials).put("selectedTrialId",best.getString("id")));index++;update(job,"collecting native training outcomes");
        }
        checkCancel();float[] weights=LinearReadout.train(x,y,12_000,.06f);job.put("weights",new JSONArray(weights));update(job,"training readout complete; selection frozen");
        double base=0,fit=0,max=0;var cpu=new CpuNeuralCompute();
        for(var c:training){double dv=Math.max(-4,Math.min(4,cpu.matvec(weights,features(c),1,4)[0]*4));job.getJSONArray("training").getJSONObject(training.indexOf(c)).put("fittedTrial",trial(c,dv));job.put("progress",job.getInt("progress")+1);}
        for(var c:validation){var baseline=trial(c,0);double dv=Math.max(-4,Math.min(4,cpu.matvec(weights,features(c),1,4)[0]*4));var selected=trial(c,dv);base+=baseline.getDouble("errorM");fit+=selected.getDouble("errorM");max=Math.max(max,selected.getDouble("errorM"));job.getJSONArray("validation").put(new JSONObject().put("case",c).put("baseline",baseline).put("selected",selected));job.put("progress",job.getInt("progress")+2);update(job,"evaluating separate native cases");}
        base/=validation.size();fit/=validation.size();boolean accepted=fit<=base*.7&&max<=.25;
        job.put("evaluation",new JSONObject().put("baselineMeanErrorM",base).put("trainedMeanErrorM",fit).put("maxTrainedErrorM",max).put("relativeMeanGain",1-fit/base).put("accepted",accepted)).put("accepted",accepted);
        if(accepted&&job.getBoolean("autoPromote"))promote(job);else job.put("reason",accepted?"Accepted candidate retained without promotion":"Validation gate rejected; active policy unchanged");
    }
    private synchronized void promote(JSONObject job) throws Exception {
        checkCancel();int current=activePolicy().getInt("version");if(current!=job.getInt("activeVersionAtStart")){job.put("reason","Active policy changed; candidate not promoted");return;}
        var p=new JSONObject().put("id",UUID.randomUUID().toString()).put("version",current+1).put("kind","native-push-readout").put("weights",job.getJSONArray("weights")).put("jobId",job.getString("id")).put("protocol",PROTOCOL).put("evaluation",job.getJSONObject("evaluation")).put("createdAt",Instant.now());
        job.put("promotion",new JSONObject().put("policyId",p.getString("id")).put("version",p.getInt("version")));commitPromotion(job,p,"policy");
    }
    public synchronized JSONObject rollback(int expectedVersion) throws Exception {
        if(busyId!=null)throw new IllegalStateException("Wait for the active brain job");var current=activePolicy();if(current.getInt("version")!=expectedVersion||expectedVersion<2)throw new IllegalStateException("Policy changed or no rollback exists");
        var all=values("policy");var prior=all.stream().filter(p->p.getInt("version")==expectedVersion-1).findFirst().orElseGet(()->new JSONObject().put("weights",new JSONArray(new float[4])));
        var next=new JSONObject().put("id",UUID.randomUUID().toString()).put("version",expectedVersion+1).put("kind","rollback").put("weights",prior.getJSONArray("weights")).put("rollbackOf",current.getString("id")).put("createdAt",Instant.now());save("policy",next);return next;
    }
    public synchronized JSONObject prediction(JSONObject scene,double target,boolean gpu) throws Exception {
        RampDefinition.range(target,.4,1.8,"target distance");var params=scene.getJSONObject("parameters");
        RampDefinition.range(params.getDouble("angleDeg"),10,40,"policy angle");RampDefinition.range(params.getDouble("friction"),0,.3,"policy friction");RampDefinition.range(params.getDouble("gravityMps2"),8,12,"policy gravity");RampDefinition.range(params.getDouble("massKg"),.5,2,"policy mass");
        if(params.getDouble("lengthM")!=12||params.getDouble("widthM")!=3||params.getDouble("offsetZ")!=0)throw new IllegalArgumentException("This policy was evaluated on the default 12 m × 3 m ramp at Z=0; use that geometry");
        if(busyId!=null)throw new IllegalStateException("Wait for the active brain job");var p=activePolicy();var weights=floats(p.getJSONArray("weights"));var c=new JSONObject(params.toString()).put("targetM",target);
        try(NeuralCompute compute=gpu?new GlNeuralCompute():new CpuNeuralCompute()){
            float raw=compute.matvec(weights,features(c),1,4)[0]*4;return new JSONObject().put("policyVersion",p.getInt("version")).put("deltaVelocityMps",Math.max(-4,Math.min(4,raw))).put("backend",compute.device()).put("features",new JSONArray(features(c)));
        }
    }
    public synchronized JSONObject activeFeedback(){
        return values("feedback").stream().max(Comparator.comparingInt(p->p.getInt("version"))).orElseGet(()->new JSONObject().put("version",1).put("kind","analytical-feedback baseline").put("kp",25).put("kd",10).put("protocol",FeedbackExperiment.PROTOCOL));
    }
    public synchronized JSONObject feedbackForRun(){
        if(busyId!=null)throw new IllegalStateException("Wait for the active brain job");return new JSONObject(activeFeedback().toString());
    }
    private void trainFeedback(JSONObject job) throws Exception {
        FeedbackExperiment.train(job,root.resolve("native"),this::checkCancel,phase->update(job,phase));checkCancel();
        if(job.getBoolean("accepted")&&job.getBoolean("autoPromote"))synchronized(this){
            checkCancel();int version=activeFeedback().getInt("version");if(version!=job.getInt("feedbackVersionAtStart")){job.put("reason","Feedback parent changed; candidate not activated");return;}
            var p=new JSONObject(job.getJSONObject("selectedGains").toString()).put("id",UUID.randomUUID().toString()).put("version",version+1).put("kind","trained-feedback").put("protocol",FeedbackExperiment.PROTOCOL).put("source",job.getJSONObject("feedbackProtocol")).put("jobId",job.getString("id")).put("evaluation",job.getJSONObject("feedbackEvaluation"));
            job.put("feedbackPromotion",new JSONObject().put("version",version+1).put("policyId",p.getString("id")));commitPromotion(job,p,"feedback");
        }else job.put("reason",job.getBoolean("accepted")?"Accepted feedback candidate retained without activation":"Feedback validation rejected; previous controller preserved");
    }
    public synchronized JSONObject rollbackFeedback(int expectedVersion) throws Exception {
        if(busyId!=null||activeFeedback().getInt("version")!=expectedVersion||expectedVersion<2)throw new IllegalStateException("Feedback parent changed or no rollback exists");
        var prior=values("feedback").stream().filter(p->p.getInt("version")==expectedVersion-1).findFirst().orElseGet(()->new JSONObject().put("kp",25).put("kd",10));
        var next=new JSONObject().put("id",UUID.randomUUID().toString()).put("version",expectedVersion+1).put("kind","feedback rollback").put("kp",prior.getDouble("kp")).put("kd",prior.getDouble("kd")).put("protocol",FeedbackExperiment.PROTOCOL);save("feedback",next);return next;
    }
    public synchronized JSONObject activeWorldModel(){return values("worldmodel").stream().max(Comparator.comparingInt(p->p.getInt("version"))).orElseGet(()->new JSONObject().put("version",0).put("available",false).put("kind","No accepted learned world model"));}
    public synchronized JSONObject worldForRun(){if(busyId!=null)throw new IllegalStateException("Wait for the active brain job");var p=activeWorldModel();if(!p.optBoolean("available"))throw new IllegalStateException("Train and validate a world model first");return p;}
    private void trainWorldModel(JSONObject job) throws Exception {
        Path repo=Path.of(System.getProperty("user.dir")).toAbsolutePath();if(!Files.isDirectory(repo.resolve("docs/datasets/delfys75-013")))repo=repo.getParent();
        WorldModelExperiment.train(job,root.resolve("native"),repo,this::checkCancel,phase->update(job,phase));
        synchronized(this){checkCancel();if(job.getBoolean("accepted")&&job.getBoolean("autoPromote")){
            int version=activeWorldModel().getInt("version");if(version!=job.getInt("worldVersionAtStart")){job.put("reason","World-model parent changed; candidate not activated");return;}
            var p=new JSONObject(job.getJSONObject("worldCandidate").toString()).put("id",UUID.randomUUID().toString()).put("version",version+1).put("available",true).put("kind","learned numeric transition ensemble").put("jobId",job.getString("id")).put("source",job.getJSONObject("worldProtocol")).put("realityAccepted",job.getJSONObject("realityEvaluation").getBoolean("accepted"));
            job.put("worldPromotion",new JSONObject().put("version",version+1));commitPromotion(job,p,"worldmodel");
        }else job.put("reason",job.getBoolean("accepted")?"Accepted native model retained without activation":"Native model/planning gate rejected; previous world model preserved");}
    }
    public synchronized JSONObject rollbackWorld(int expectedVersion) throws Exception {
        if(busyId!=null||activeWorldModel().getInt("version")!=expectedVersion||expectedVersion<1)throw new IllegalStateException("World-model parent changed or no rollback exists");
        var prior=values("worldmodel").stream().filter(p->p.getInt("version")==expectedVersion-1).findFirst().orElseGet(()->new JSONObject().put("available",false).put("kind","No active world model"));
        var next=new JSONObject(prior.toString()).put("id",UUID.randomUUID().toString()).put("version",expectedVersion+1).put("kind","world-model rollback");save("worldmodel",next);return next;
    }
    private static float[] floats(JSONArray a){float[] out=new float[a.length()];for(int i=0;i<out.length;i++)out[i]=(float)a.getDouble(i);return out;}
    private void gpuCheck(JSONObject job) throws Exception {
        update(job,"dispatching actual GPU kernel");var random=new Random(16016);var rows=new JSONArray();
        try(var gpu=new GlNeuralCompute();var cpu=new CpuNeuralCompute()){
            job.put("device",gpu.device());
            for(int[] size:new int[][]{{5,7},{65,33},{1024,128}}){int r=size[0],c=size[1];float[] w=new float[r*c],x=new float[c];for(int i=0;i<w.length;i++)w[i]=random.nextFloat()-.5f;for(int i=0;i<c;i++)x[i]=random.nextFloat()-.5f;
                var expected=cpu.matvec(w,x,r,c);gpu.matvec(w,x,r,c);double max=0;long cpuNs=0,gpuNs=0;for(int n=0;n<10;n++){checkCancel();long t=System.nanoTime();cpu.matvec(w,x,r,c);cpuNs+=System.nanoTime()-t;t=System.nanoTime();var result=gpu.matvec(w,x,r,c);gpuNs+=System.nanoTime()-t;for(int i=0;i<r;i++)max=Math.max(max,Math.abs(result[i]-expected[i]));}
                if(max>.0002)throw new IllegalStateException("GPU numerical parity failed");rows.put(new JSONObject().put("rows",r).put("columns",c).put("maxDifference",max).put("tolerance",.0002).put("cpuMeanMs",cpuNs/10e6).put("gpuMeanIncludingTransfersMs",gpuNs/10e6));
            }
        }
        job.put("cases",rows).put("accepted",true).put("scope","Real dense readout kernel; full spiking cortex remains CPU. Transfer overhead included; no automatic speedup claim.");
    }
    private void pulse(JSONObject job) throws Exception {
        update(job,"running shared Java cortex");Path file=root.resolve("cortex.bin");
        synchronized(this){if(cortex==null)cortex=Files.exists(file)?NeuralCheckpoint.load(file):new NeuromorphicCore(32768,32,618033988L);
            float[] features={.5f,-.25f,.75f,0};cortex.inject(features);cortex.tick(false);String hash=NeuralCheckpoint.save(cortex,file);
            job.put("neurons",cortex.neurons()).put("synapses",cortex.synapses()).put("ticks",cortex.ticks()).put("spikes",cortex.spikeCount()).put("checkpointSha256",hash).put("accepted",true).put("scope","Synthetic diagnostic sensory pulse; no gameplay improvement claim and no physical-policy training");
        }
    }
    synchronized JSONObject background042(float[] sensory,boolean learning)throws Exception{
        if(closed)throw new IllegalStateException("Brain is closed");
        if(busyId!=null)return new JSONObject().put("status","foreground-brain-job").put("updated",false);
        if(cortex==null)cortex=Files.exists(root.resolve("cortex.bin"))?NeuralCheckpoint.load(root.resolve("cortex.bin")):new NeuromorphicCore(32768,32,618033988L);
        long begin=System.nanoTime();cortex.inject(sensory);cortex.tick(learning);
        return new JSONObject().put("status","shared-java-cortex").put("updated",true).put("neurons",cortex.neurons()).put("synapses",cortex.synapses()).put("ticks",cortex.ticks()).put("spikes",cortex.spikeCount()).put("plasticity",cortex.plasticity()).put("learningEnabled",learning).put("processingMs",(System.nanoTime()-begin)/1e6).put("scope","Actual sensory dynamics/plasticity. Task improvement still requires separate policy evaluation; no language-model weights are changed.");
    }
    synchronized JSONObject checkpoint042(Path target)throws Exception{
        if(cortex==null)return new JSONObject();String dataHash=NeuralCheckpoint.save(cortex,target);
        return new JSONObject().put("file",target.getFileName().toString()).put("fileSha256",PerceptionStore.hash(Files.readAllBytes(target))).put("dataSha256",dataHash).put("ticks",cortex.ticks());
    }
    synchronized long cortexTicks042(){return cortex==null?-1:cortex.ticks();}
    synchronized void restore042(Path file,JSONObject receipt)throws Exception{
        if(busyId!=null)throw new IllegalStateException("Cannot replace an active brain");
        if(!PerceptionStore.hash(Files.readAllBytes(file)).equals(receipt.getString("fileSha256")))throw new IllegalStateException("Agent neural checkpoint source differs");
        cortex=NeuralCheckpoint.load(file);if(cortex.ticks()!=receipt.getLong("ticks"))throw new IllegalStateException("Agent neural checkpoint tick differs");
    }
    public void close(){closed=true;cancel=true;worker.shutdown();try{if(!worker.awaitTermination(5,TimeUnit.SECONDS))worker.shutdownNow();}catch(InterruptedException e){Thread.currentThread().interrupt();worker.shutdownNow();}}
}
