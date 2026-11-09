package fraymus.workspace;

import fraymus.factory.*;
import org.json.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/** Durable supervisor of registered goals; imported text never installs a stage handler. */
public final class WorkflowRunner implements AutoCloseable {
    static final String PROCEDURE="evidence-workflow-v1";
    private static final List<String> STAGES=List.of("data","physics","creation");
    private final GoalRunner goals;private final MeasurementStore measurements;private final SceneStore scenes;private final WorkspaceStore artifacts;
    private final SkillGraph046.ContractMemo graphMemo=new SkillGraph046.ContractMemo();private long graphClock=System.nanoTime(),graphBudgetMs,graphBudgetCheckpoint;private String graphBudgetId="";
    private final SchemaRegistry schemas=new SchemaRegistry();private final FactoryWorld world;private final WorldCheckpointStore checkpoint;
    private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"java-workflow-runner");t.setDaemon(true);return t;});private boolean closed;
    public WorkflowRunner(Path root,GoalRunner goals,MeasurementStore measurements,SceneStore scenes,WorkspaceStore artifacts) throws Exception {
        this.goals=goals;this.measurements=measurements;this.scenes=scenes;this.artifacts=artifacts;schemas.register(new ComponentSchema("workspace.workflow",1,Map.of("payload",ComponentSchema.Kind.TEXT),Set.of("payload")));world=new FactoryWorld(schemas);checkpoint=new WorldCheckpointStore(root.resolve("workflows/tasks.fworld"));if(Files.exists(checkpoint.path()))world.restore(checkpoint.load(schemas).snapshot());
        for(var w:values())if(!settled(w)){w.put("status","interrupted");w.remove("claim");change(w,"Process ended; resume explicitly from verified stages");goals.reserveWorkflow(w.getString("id"));}
        for(var w:values())if(dataCreation101(w)&&w.getString("status").equals("completed")&&w.has("pendingSkill101"))try{publish101(w);}catch(Exception e){System.err.println("Completed workflow has pending skill publication; retry on restart: "+e.getMessage());}
        worker.scheduleWithFixedDelay(this::tick,70,100,TimeUnit.MILLISECONDS);
    }
    private List<JSONObject> values(){var all=new ArrayList<JSONObject>();for(var e:world.snapshot().entities())all.add(new JSONObject(e.components().getFirst().values().get("payload")));all.sort(Comparator.comparing(w->w.getString("createdAt")));return all;}
    private void save(JSONObject w) throws Exception {
        var before=world.snapshot();var id=UUID.fromString(w.getString("id"));var prior=before.entities().stream().filter(e->e.id().equals(id)).findFirst();var c=new ComponentRecord(prior.map(e->e.components().getFirst().id()).orElse(UUID.randomUUID()),"workspace.workflow",1,Map.of("payload",w.toString()));var tx=new WorldTransaction(before);if(prior.isPresent())tx.add(new WorldTransaction.Put(id,c));else tx.add(new WorldTransaction.Create(new EntityRecord(id,"workflow",List.of(c))));try{tx.commit(world,schemas);checkpoint.save(world.snapshot(),schemas);}catch(Exception e){world.restore(before);throw e;}
    }
    private static boolean settled(JSONObject w){return Set.of("completed","cancelled").contains(w.getString("status"));}
    private JSONObject active(){return values().stream().filter(w->!settled(w)).findFirst().orElse(null);}
    private void change(JSONObject w,String reason) throws Exception {w.put("reason",reason).put("updatedAt",Instant.now());w.getJSONArray("history").put(new JSONObject().put("at",Instant.now()).put("reason",reason).put("step",w.getString("step")).put("status",w.getString("status")));save(w);}
    private JSONObject raw(String id){UUID.fromString(id);return values().stream().filter(w->w.getString("id").equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown workflow"));}
    private static JSONObject view(JSONObject w){var out=new JSONObject().put("workflow",true).put("procedure",graph(w)?SkillGraph046.KIND:dataCreation101(w)?w.getString("profile101"):PROCEDURE).put("attempt",w.getInt("resumes")+1);for(String k:List.of("id","title","request","modelName","status","step","resumes","createdAt","updatedAt","reason","history","stages","currentChild","bundle","audit","auditHistory","report","metrics","savedSkill","skill","dataBinding","graph","graphSha256","graphCodeSha256","worldBinding","graphRemainingMs","diagnostics","memo","reviewEachNode","profile101","code101","pendingSkill101"))if(w.has(k))out.put(k,w.get(k));return out;}
    public synchronized JSONObject read(String id){return view(raw(id));}
    public synchronized JSONArray list(){var all=new JSONArray();for(var w:values())all.put(view(w));return all;}
    private JSONObject definition(JSONObject input) throws Exception {
        if(input.has("profile101"))return DataSummary102.definition(input,measurements);
        SpatialScene.keys(input,"request","title","modelName","datasetId","datasetVersion","maxGapS","speedLimitMps","parameters","targetM","disturbanceMps","sceneId","expectedVersion","saveSkillName","responsive");
        if(!(input.get("datasetVersion") instanceof Number version)||version.doubleValue()!=version.intValue()||version.intValue()<1)throw new IllegalArgumentException("Choose an explicit integer dataset version");for(String key:List.of("maxGapS","speedLimitMps","targetM","disturbanceMps"))if(input.has(key)&&!(input.get(key) instanceof Number))throw new IllegalArgumentException("Use numeric workflow criteria");
        if(input.has("responsive")&&!(input.get("responsive") instanceof Boolean))throw new IllegalArgumentException("Responsive profile must be a boolean");
        String request=Creation.text(input,"request",4000),title=Creation.text(input,"title",140),model=Creation.text(input,"modelName",120);var data=measurements.read(input.getString("datasetId"),input.getInt("datasetVersion"));var rows=MotionData.originalRows(data);if(rows.length()>1000||data.getString("csv").length()>100_000)throw new IllegalArgumentException("This workflow accepts up to1000 observation rows and100K original CSV characters; no hidden subsampling");
        double gap=input.optDouble("maxGapS",.25),speed=input.optDouble("speedLimitMps",5);MotionData.criteria(gap,speed);var physics=new JSONObject().put("parameters",input.optJSONObject("parameters")==null?new JSONObject():new JSONObject(input.getJSONObject("parameters").toString())).put("targetM",input.optDouble("targetM",1.2)).put("disturbanceMps",input.optDouble("disturbanceMps",.35));
        if(input.has("sceneId")){var scene=scenes.read(input.getString("sceneId"));if(scene.getInt("version")!=input.getInt("expectedVersion"))throw new IllegalStateException("Selected simulation scene changed");var parameters=new JSONObject(scene.getJSONObject("parameters").toString());for(String k:physics.getJSONObject("parameters").keySet())parameters.put(k,physics.getJSONObject("parameters").get(k));physics.put("parameters",parameters).put("sceneId",scene.getString("id")).put("expectedVersion",scene.getInt("version")).put("sourceSceneSha256",MeasurementStore.hash(scene.toString()));}
        var parameters=RampDefinition.from(physics.getJSONObject("parameters")).json();FeedbackExperiment.validate(parameters,physics.getDouble("targetM"));RampDefinition.range(physics.getDouble("disturbanceMps"),-.5,.5,"disturbance");physics.put("parameters",parameters);
        String name=input.optString("saveSkillName","");if(!name.isEmpty()&&!name.matches("[a-z][a-z0-9-]{2,55}"))throw new IllegalArgumentException("Workflow skill name needs3..56 lowercase letters, digits or hyphens");
        return new JSONObject().put("request",request).put("title",title).put("modelName",model).put("datasetId",data.getString("datasetId")).put("datasetVersion",data.getInt("version")).put("maxGapS",gap).put("speedLimitMps",speed).put("physics",physics).put("saveSkillName",name).put("responsive",input.optBoolean("responsive",false));
    }
    public synchronized JSONObject start(JSONObject input) throws Exception {return start(input,null);}
    private JSONObject start(JSONObject input,JSONObject skill) throws Exception {
        if(input.has("graph"))return startGraph(input,skill);
        if(closed||active()!=null||values().size()>=20)throw new IllegalStateException("Finish/resume/stop the current workflow; history limit20");goals.workflowCapacity();var def=definition(input);String id=UUID.randomUUID().toString();var stages=new JSONArray();for(String stage:def.has("profile101")?DataCreation101.STAGES:STAGES)stages.put(new JSONObject().put("name",stage).put("status","pending"));
        var w=new JSONObject().put("id",id).put("definition",def).put("title",def.getString("title")).put("request",def.getString("request")).put("modelName",def.getString("modelName")).put("dataBinding",MotionData.binding(measurements.read(def.getString("datasetId"),def.getInt("datasetVersion")))).put("status","running").put("step","data").put("stages",stages).put("stageIndex",0).put("resumes",0).put("deadline",System.currentTimeMillis()+1_200_000).put("createdAt",Instant.now()).put("history",new JSONArray()).put("auditHistory",new JSONArray());if(skill!=null)w.put("skill",new JSONObject().put("name",skill.getString("name")).put("version",skill.getInt("version")).put("id",skill.getString("id")));
        if(def.has("profile101"))w.put("profile101",def.getString("profile101")).put("code101",DataSummary102.code(def.getString("profile101")));
        goals.reserveWorkflow(id);try{change(w,"Request, source, conditions and registered stages frozen before execution");}catch(Exception e){goals.releaseWorkflow(id);throw e;}return view(w);
    }
    private JSONObject source(JSONObject w){var d=w.getJSONObject("definition");var data=measurements.read(d.getString("datasetId"),d.getInt("datasetVersion"));if(!MotionData.binding(data).similar(w.getJSONObject("dataBinding")))throw new IllegalStateException("Frozen observation source or mapping changed");MotionData.originalRows(data);if(dataCreation101(w)){if(!w.getString("code101").equals(DataSummary102.code(w.getString("profile101"))))throw new IllegalStateException("Data-creation verifier changed; start a newly reviewed workflow");return data;}var p=d.getJSONObject("physics");if(p.has("sceneId")&&!MeasurementStore.hash(scenes.read(p.getString("sceneId")).toString()).equals(p.getString("sourceSceneSha256")))throw new IllegalStateException("Frozen authored simulation scene changed");return data;}
    private JSONObject stage(JSONObject w,int index){return w.getJSONArray("stages").getJSONObject(index);}
    private JSONObject verifiedStage(JSONObject w,int index) throws Exception {var s=stage(w,index);var g=goals.verifiedOwned(w.getString("id"),s.getString("goalId"));if(!g.getString("workflowStage").equals(stageNames(w).get(index)))throw new IllegalStateException("Owned stage family changed");if(s.has("artifact")&&!Creation.reference(artifacts.read(g.getJSONObject("report").getString("id"))).similar(s.getJSONObject("artifact")))throw new IllegalStateException("Verified stage artifact changed");return g;}
    private JSONObject bundle(JSONObject w) throws Exception {
        var data=source(w);var dg=verifiedStage(w,0);var pg=verifiedStage(w,1);var run=goals.ownedNativeRun(w.getString("id"),pg.getString("id"));var args=WorkflowEvidence.bundle(w,data,dg,pg,run);var a=artifacts.writeOnce("workflow-bundle:"+w.getString("id"),args);if(a.getInt("version")!=1)throw new IllegalStateException("Evidence bundle was externally edited");if(w.has("bundle")&&!Creation.reference(a).similar(w.getJSONObject("bundle")))throw new IllegalStateException("Evidence bundle identity changed");return a;
    }
    private JSONObject intent(JSONObject w,int index) throws Exception {
        var d=w.getJSONObject("definition");if(index==0)return new JSONObject().put("request",d.getString("request")).put("datasetId",d.getString("datasetId")).put("datasetVersion",d.getInt("datasetVersion")).put("maxGapS",d.getDouble("maxGapS")).put("speedLimitMps",d.getDouble("speedLimitMps"));
        if(dataCreation101(w))return DataSummary102.creation(w,handoff101(w));
        if(index==1){var p=new JSONObject(d.getJSONObject("physics").toString());p.remove("sourceSceneSha256");return p.put("request",d.getString("request"));}
        var a=bundle(w);w.put("bundle",Creation.reference(a));return WorkflowEvidence.creation(w,a);
    }
    private synchronized void tick(){
        if(closed)return;var w=active();if(w==null||!w.getString("status").equals("running"))return;
        if(graph(w)){tickGraph(w);return;}
        try{
            if(System.currentTimeMillis()>w.getLong("deadline"))throw new IllegalStateException("Workflow attempt exceeded1200seconds");
            if(w.getString("step").equals("audit"))return;
            if(w.getString("step").equals("complete")){complete(w);return;}
            int index=w.getInt("stageIndex");var s=stage(w,index);if(!s.has("input")){source(w);s.put("input",intent(w,index));change(w,"Saved "+s.getString("name")+" intent before starting its stable owned child");return;}
            var g=goals.startOwned(w.getString("id"),s.getString("name"),s.getJSONObject("input"));s.put("goalId",g.getString("id"));
            if(!g.getString("status").equals("completed")){if(!s.optString("childUpdatedAt").equals(g.optString("updatedAt"))){s.put("status",g.getString("status")).put("childUpdatedAt",g.optString("updatedAt"));w.put("currentChild",g);save(w);}if(Set.of("interrupted","cancelled").contains(g.getString("status")))throw new IllegalStateException("Owned "+s.getString("name")+" stage stopped: "+g.getString("reason"));return;}
            var raw=goals.verifiedOwned(w.getString("id"),g.getString("id"));s.put("status","completed").put("artifact",Creation.reference(artifacts.read(raw.getJSONObject("report").getString("id")))).put("metrics",raw.getJSONObject("metrics"));if(raw.has("runId"))s.put("runId",raw.getString("runId"));w.put("currentChild",g);
            if(index<stageNames(w).size()-1){w.put("stageIndex",index+1).put("step",stageNames(w).get(index+1));if(dataCreation101(w)&&w.getJSONObject("definition").optBoolean("reviewAfterData101"))w.put("status","interrupted");change(w,"Verified "+s.getString("name")+" stage; retained outcome will not be repeated"+(w.getString("status").equals("interrupted")?". Requested review before creation; explicitly Resume.":""));}
            else{w.put("step","audit").put("report",s.getJSONObject("artifact"));w.remove("claim");change(w,dataCreation101(w)?"Generated behavior passed; independent visible rows and filter-state audit is next":"Generated behavior passed; independent source-cell and native SVG geometry audit is next");}
        }catch(Throwable e){try{var durable=raw(w.getString("id"));if(dataCreation101(durable)&&durable.getString("status").equals("completed")){goals.releaseWorkflow(w.getString("id"));System.err.println("Completed workflow publication/logging needs reconciliation on restart: "+e);}else interrupt(w,e.getMessage());}catch(Exception failed){System.err.println("Workflow checkpoint: "+failed);}}
    }
    private void interrupt(JSONObject w,String reason) throws Exception {var s=stage(w,w.getInt("stageIndex"));recoverPointer(w,s);if(s.has("goalId"))goals.interruptOwned(w.getString("id"),s.getString("goalId"),"Parent workflow interrupted: "+reason);if(graph(w))SkillGraph046.diagnostic(w,s.getString("name"),String.valueOf(reason));w.put("status","interrupted");w.remove("claim");change(w,reason+". Review and explicitly Resume or Stop the workflow.");}
    private void complete(JSONObject w) throws Exception {
        if(dataCreation101(w)){complete101(w);return;}
        source(w);for(int i=0;i<3;i++)verifiedStage(w,i);var b=bundle(w);var a=artifacts.read(w.getJSONObject("report").getString("id"));var audit=w.getJSONObject("audit");if(!audit.getBoolean("passed")||!audit.getJSONObject("artifact").similar(Creation.reference(a)))throw new IllegalStateException("Independent passing audit no longer matches the created app");
        var d=w.getJSONObject("definition");w.put("metrics",new JSONObject().put("stages",3).put("observationRows",w.getJSONObject("dataBinding").has("rowCount")?w.getJSONObject("dataBinding").getInt("rowCount"):source(w).getJSONArray("rows").length()).put("evidenceRows",audit.getInt("rowCount")).put("maxPlotError",audit.getDouble("maxPlotError")).put("qualityPassed",stage(w,0).getJSONObject("metrics").getBoolean("qualityPassed")).put("endpointErrorM",stage(w,1).getJSONObject("metrics").getDouble("endpointErrorM")));
        if(!d.getString("saveSkillName").isEmpty()){
            var defaults=new JSONObject().put("request",d.getString("request")).put("title",d.getString("title")).put("modelName",d.getString("modelName")).put("parameters",d.getJSONObject("physics").getJSONObject("parameters")).put("targetM",d.getJSONObject("physics").getDouble("targetM")).put("disturbanceMps",d.getJSONObject("physics").getDouble("disturbanceMps")).put("maxGapS",d.getDouble("maxGapS")).put("speedLimitMps",d.getDouble("speedLimitMps")).put("responsive",d.optBoolean("responsive",false));
            var p=new JSONObject().put("kind",PROCEDURE).put("sourceWorkflowId",w.getString("id")).put("report",w.getJSONObject("report")).put("defaults",defaults).put("sourceData",w.getJSONObject("dataBinding")).put("prerequisites",new JSONArray(List.of("Fresh explicitly selected observation dataset/version","Frozen data criteria and separate native simulation conditions","Admitted installed local model","No other shared workflow or goal"))).put("actions",new JSONArray(List.of("Analyze and verify original observation data","Execute and verify native world goal","Build actual qualified source evidence","Generate and test interactive content","Independently verify every cell and native chart point"))).put("recovery",new JSONArray(List.of("Stable child intents reconcile interrupted starts","Reuse completed verified stages","Incomplete physics resumes as a new recorded attempt","Three total creation drafts including audit repair","Explicit resume after interruption; stop cancels owned work"))).put("scope",WorkflowEvidence.SCOPE+" Registered three-stage workflow, not a general planner or LLM-weight training.");w.put("savedSkill",goals.retainWorkflow(p,d.getString("saveSkillName")));
        }
        w.put("status","completed");change(w,"All three actual stages and independent data/geometry audit verified"+(!d.getString("saveSkillName").isEmpty()?"; reusable workflow retained":""));goals.releaseWorkflow(w.getString("id"));artifacts.event("java_workflow_completed",view(w));
    }
    public synchronized JSONObject claim() throws Exception {
        var w=active();if(w==null||!w.getString("status").equals("running")||!w.getString("step").equals("audit"))return new JSONObject();if(graph(w))return claimGraph(w);if(dataCreation101(w))return claim101(w);try{var previous=w.optJSONObject("claim");if(previous!=null&&previous.getLong("expires")>System.currentTimeMillis())return new JSONObject();source(w);verifiedStage(w,2);var b=bundle(w);int rows=MeasurementStore.csv(b.getJSONObject("contents").getString("evidence.csv")).size()-1;
        var c=new JSONObject().put("nonce",UUID.randomUUID().toString()).put("expires",System.currentTimeMillis()+30_000).put("id",UUID.randomUUID().toString());w.put("claim",c);save(w);return new JSONObject(c.toString()).put("kind","workflow.audit").put("workflowId",w.getString("id")).put("report",w.getJSONObject("report")).put("rowCount",rows).put("responsive",w.getJSONObject("definition").optBoolean("responsive",false));}catch(Exception failure){interrupt(w,"Independent audit preflight: "+failure.getMessage());return new JSONObject();}
    }
    synchronized void hostDetached042()throws Exception{var w=active();if(w!=null&&w.has("claim"))interrupt(w,"Interface ended during claimed canvas audit; explicitly Resume after inspection");}
    public synchronized JSONObject acknowledge(JSONObject input) throws Exception {
        if(graph(raw(input.getString("workflowId"))))return acknowledgeGraph(input);
        if(dataCreation101(raw(input.getString("workflowId"))))return acknowledge101(input);
        SpatialScene.keys(input,"workflowId","id","nonce","result","error");var w=raw(input.getString("workflowId"));var c=w.optJSONObject("claim");if(!w.getString("status").equals("running")||!w.getString("step").equals("audit")||c==null||!c.getString("id").equals(input.getString("id"))||!c.getString("nonce").equals(input.getString("nonce"))||c.getLong("expires")<System.currentTimeMillis())throw new IllegalStateException("Stale or foreign workflow audit");w.remove("claim");
        try{if(input.has("error"))throw new IllegalStateException("Canvas audit failed: "+input.getString("error"));var g=verifiedStage(w,2);var a=artifacts.read(g.getJSONObject("report").getString("id"));var audit=WorkflowEvidence.audit(a,bundle(w),input.getJSONObject("result"));w.getJSONArray("auditHistory").put(audit);
            if(audit.getBoolean("passed")){w.put("audit",audit).put("step","complete");change(w,"Every original table cell and native chart coordinate independently verified");}
            else{goals.repairOwnedCreation(w.getString("id"),g.getString("id"),new JSONArray().put(new JSONObject().put("passed",false).put("error","Independent workflow audit failed").put("details",audit.getJSONArray("errors"))));stage(w,2).remove("artifact");stage(w,2).put("status","running");w.put("step","creation");change(w,"Independent audit rejected this draft; repair receives actual failures without weakening the contract");}
        }catch(Exception e){interrupt(w,e.getMessage());}return view(w);
    }
    public synchronized JSONObject resume(String id) throws Exception {
        var w=raw(id);if(graph(w))return resumeGraph(w);if(!w.getString("status").equals("interrupted")||w.getInt("resumes")>=3)throw new IllegalStateException("Resume only an interrupted workflow, at most3 times");source(w);for(int i=0;i<w.getInt("stageIndex");i++)verifiedStage(w,i);if(w.has("bundle"))bundle(w);var s=stage(w,w.getInt("stageIndex"));if(!s.has("goalId")&&s.has("input")){var recovered=goals.startOwned(id,s.getString("name"),s.getJSONObject("input"));s.put("goalId",recovered.getString("id"));}if(s.has("goalId")){var g=goals.read(s.getString("goalId"));if(g.getString("status").equals("interrupted"))goals.controlOwned(id,g.getString("id"),false);else if(g.getString("status").equals("cancelled"))throw new IllegalStateException("Owned stage was cancelled; start a new workflow");else if(g.getString("status").equals("completed"))goals.verifiedOwned(id,g.getString("id"));}
        w.put("status","running").put("resumes",w.getInt("resumes")+1).put("deadline",System.currentTimeMillis()+1_200_000);w.remove("claim");change(w,"Explicit resume reuses verified completed stages and the same owned child intent");return view(w);
    }
    private void recoverPointer(JSONObject w,JSONObject s){if(!s.has("goalId")){var g=goals.findOwned(w.getString("id"),(graph(w)?"graph:":"")+s.getString("name"));if(g!=null)s.put("goalId",g.getString("id"));}}
    public synchronized JSONObject cancel(String id) throws Exception {var w=raw(id);if(settled(w))throw new IllegalStateException("Workflow already settled");var s=stage(w,w.getInt("stageIndex"));recoverPointer(w,s);if(s.has("goalId")){var g=goals.read(s.getString("goalId"));if(Set.of("running","waiting-host","interrupted").contains(g.getString("status")))goals.controlOwned(id,g.getString("id"),true);}w.put("status","cancelled");w.remove("claim");change(w,"Stopped owned work; no workflow skill promoted");goals.releaseWorkflow(id);return view(w);}
    public synchronized JSONObject reuse(JSONObject input) throws Exception {
        if(input.has("worldBinding"))return reuseGraph(input);
        for(Object raw:goals.procedures()){var p=(JSONObject)raw;if(p.getString("name").equals(input.getString("name"))&&p.getInt("version")==input.getInt("version")&&DataSummary102.supported(p.getString("kind")))return reuse101(input,p);}
        SpatialScene.keys(input,"name","version","request","title","datasetId","datasetVersion","parameters","targetM","disturbanceMps","maxGapS","speedLimitMps","modelName","saveSkillName","responsive");JSONObject chosen=null;for(Object raw:goals.procedures()){var p=(JSONObject)raw;if(p.getString("name").equals(input.getString("name"))&&p.getInt("version")==input.getInt("version"))chosen=p;}if(chosen==null||!chosen.getString("kind").equals(PROCEDURE))throw new IllegalArgumentException("Select a retained evidence workflow");var args=new JSONObject(chosen.getJSONObject("defaults").toString());for(String k:input.keySet())if(!Set.of("name","version").contains(k)){if(k.equals("parameters")){for(String key:input.getJSONObject(k).keySet())args.getJSONObject(k).put(key,input.getJSONObject(k).get(key));}else args.put(k,input.get(k));}args.put("saveSkillName",input.optString("saveSkillName",""));return start(args,chosen);
    }
    private static boolean dataCreation101(JSONObject w){return DataSummary102.supported(w.optString("profile101"));}
    private static List<String> stageNames(JSONObject w){return dataCreation101(w)?DataCreation101.STAGES:STAGES;}
    private JSONObject handoff101(JSONObject w) throws Exception {
        source(w);var data=verifiedStage(w,0);var handoff=goals.creationSource100(new JSONObject().put("goalId",data.getString("id")));
        if(!handoff.getJSONObject("dataBinding").similar(w.getJSONObject("dataBinding")))throw new IllegalStateException("Workflow data lineage changed");return handoff;
    }
    private JSONObject verified101(JSONObject w) throws Exception {
        var data=source(w);var handoff=handoff101(w);var child=verifiedStage(w,1);
        if(!child.getJSONObject("dataHandoff100").similar(handoff))throw new IllegalStateException("Creation belongs to different data");
        var a=artifacts.read(child.getJSONObject("report").getString("id"));
        var actual=new JSONObject().put("id",a.getString("id")).put("version",a.getInt("version")).put("frames",w.getJSONObject("audit").getJSONArray("frames"));
        if(w.getJSONObject("audit").has("summaries102"))actual.put("summaries102",w.getJSONObject("audit").getJSONArray("summaries102"));var audit=DataSummary102.audit(w,a,data,handoff,actual);
        if(!audit.getBoolean("passed")||!audit.similar(w.getJSONObject("audit")))throw new IllegalStateException("Original visible data-creation evidence changed");return audit;
    }
    private JSONObject claim101(JSONObject w) throws Exception {
        try{var old=w.optJSONObject("claim");if(old!=null&&old.getLong("expires")>System.currentTimeMillis())return new JSONObject();var handoff=handoff101(w);var child=verifiedStage(w,1);
            if(!child.getJSONObject("dataHandoff100").similar(handoff))throw new IllegalStateException("Foreign data-creation child");
            var c=new JSONObject().put("nonce",UUID.randomUUID().toString()).put("expires",System.currentTimeMillis()+30_000).put("id",UUID.randomUUID().toString());w.put("claim",c);save(w);
            return WorldFrames.copy(c).put("kind",DataSummary102.KIND.equals(w.getString("profile101"))?"workflow.data-summary-audit102":"workflow.data-creation-audit101").put("workflowId",w.getString("id")).put("report",w.getJSONObject("report")).put("filter",handoff.getJSONObject("preview").getJSONArray("sample").getJSONObject(0).getString("source_row")).put("rowCount",handoff.getJSONObject("preview").getInt("rowCount"));
        }catch(Exception e){interrupt(w,"Data-creation audit preflight: "+e.getMessage());return new JSONObject();}
    }
    private JSONObject acknowledge101(JSONObject input) throws Exception {
        SpatialScene.keys(input,"workflowId","id","nonce","result","error");var w=raw(input.getString("workflowId"));var c=w.optJSONObject("claim");
        if(!w.getString("status").equals("running")||!w.getString("step").equals("audit")||c==null||!c.getString("id").equals(input.getString("id"))||!c.getString("nonce").equals(input.getString("nonce"))||c.getLong("expires")<System.currentTimeMillis())throw new IllegalStateException("Stale or foreign data-creation audit");w.remove("claim");
        try{if(input.has("error"))throw new IllegalStateException(input.getString("error"));var child=verifiedStage(w,1);var a=artifacts.read(child.getJSONObject("report").getString("id"));var audit=DataSummary102.audit(w,a,source(w),handoff101(w),input.getJSONObject("result"));w.getJSONArray("auditHistory").put(audit);
            if(audit.getBoolean("passed")){w.put("audit",audit).put("step","complete");change(w,"Every required source value, visible label, filter state and registered output independently verified");}
            else{goals.repairOwnedCreation(w.getString("id"),child.getString("id"),new JSONArray().put(new JSONObject().put("passed",false).put("error","Independent data-creation audit failed").put("details",audit.getJSONArray("errors"))));stage(w,1).remove("artifact");stage(w,1).put("status","running");w.put("step","creation");change(w,"Actual audit refused this draft; repair retains the original contract and total draft budget");}
        }catch(Exception e){interrupt(w,e.getMessage());}return view(w);
    }
    private void complete101(JSONObject w) throws Exception {
        var audit=verified101(w);w.put("metrics",new JSONObject().put("stages",2).put("observationRows",audit.getInt("rowCount")).put("auditedFrames",4).put("qualityPassed",stage(w,0).getJSONObject("metrics").getBoolean("qualityPassed")));
        String name=w.getJSONObject("definition").getString("saveSkillName");if(!name.isEmpty())w.put("pendingSkill101",goals.retainWorkflow(DataSummary102.procedure(w),name));
        w.put("status","completed");change(w,"Data, generated content and independent audit completed; skill publication follows durable parent completion");
        if(w.has("pendingSkill101"))publish101(w);goals.releaseWorkflow(w.getString("id"));artifacts.event("java_workflow_completed",view(w));
    }
    private void publish101(JSONObject w) throws Exception {
        if(!w.getString("status").equals("completed"))throw new IllegalStateException("Skill parent not completed");verified101(w);
        var ref=w.getJSONObject("pendingSkill101");
        // Save the exact parent linkage before exposing the skill. Keep pending until cleanup is durable.
        w.put("savedSkill",ref);change(w,"Completed parent and exact skill linkage persisted; publication pending");
        goals.publishWorkflow101(DataSummary102.procedure(w),ref);w.remove("pendingSkill101");change(w,"Completed parent and original evidence verified; whole data-creation procedure published");
    }
    private JSONObject reuse101(JSONObject input,JSONObject chosen) throws Exception {
        SpatialScene.keys(input,"name","version","request","title","modelName","datasetId","datasetVersion","maxGapS","speedLimitMps","saveSkillName","reviewAfterData101");
        if(!chosen.optString("publication101").equals("published"))throw new IllegalStateException("Whole procedure is pending publication");var parent=raw(chosen.getString("sourceWorkflowId"));
        if(!dataCreation101(parent)||!parent.getString("status").equals("completed")||!parent.has("savedSkill")||!parent.getJSONObject("savedSkill").getString("id").equals(chosen.getString("id")))throw new IllegalStateException("Procedure lacks a completed published parent");
        verified101(parent);var expected=DataSummary102.procedure(parent);for(String k:expected.keySet())if(!k.equals("publication101")&&!new JSONObject().put("v",expected.get(k)).similar(new JSONObject().put("v",chosen.get(k))))throw new IllegalStateException("Original whole-procedure contract changed");
        var args=WorldFrames.copy(chosen.getJSONObject("defaults"));for(String k:input.keySet())if(!Set.of("name","version").contains(k))args.put(k,input.get(k));args.put("saveSkillName",input.optString("saveSkillName",""));return start(args,chosen);
    }
    private static boolean graph(JSONObject w){return w.has("graph");}
    private JSONObject startGraph(JSONObject input,JSONObject skill) throws Exception {
        SpatialScene.keys(input,"graph","worldBinding","request","title","modelName","saveSkillName","reviewEachNode");
        if(closed||active()!=null||values().size()>=20)throw new IllegalStateException("Finish/resume/stop the current workflow; history limit20");goals.workflowCapacity();
        String title=Creation.text(input,"title",100),request=Creation.text(input,"request",4000),model=Creation.text(input,"modelName",120),name=input.optString("saveSkillName","");
        if(!name.isEmpty()&&!name.matches("[a-z][a-z0-9-]{2,55}"))throw new IllegalArgumentException("Graph skill name needs3..56 lowercase letters, digits or hyphens");
        if(input.has("reviewEachNode")&&!(input.get("reviewEachNode") instanceof Boolean))throw new IllegalArgumentException("Review each node must be boolean");
        var def=graphMemo.get(input.getJSONObject("graph"),goals);
        if(goals.list().length()+def.getJSONArray("nodes").length()-1>64)throw new IllegalStateException("Insufficient retained-goal capacity for every graph procedure");
        if(!name.isEmpty()){int version=1;var retained=goals.procedures();for(Object raw:retained){var p=(JSONObject)raw;if(p.getString("name").equals(name)){if(!p.getString("kind").equals(SkillGraph046.KIND))throw new IllegalArgumentException("Use a distinct name for this graph task family");version=Math.max(version,p.getInt("version")+1);}}if(retained.length()>=32||version>8)throw new IllegalStateException("No retained skill capacity for the requested graph version");}
        var binding=WorldFrames.copy(input.getJSONObject("worldBinding"));SkillGraph046.current(goals,binding);
        var scene=goals.worlds().source(SkillGraph046.binding(binding));
        for(Object raw:def.getJSONArray("nodes")){var n=(JSONObject)raw;if(n.getString("kind").equals("procedure"))WorldPlan.intent(scene,n.getJSONArray("targets"),n.getDouble("forceLimitN"),n.getDouble("torqueLimitNm"));}
        String id=UUID.randomUUID().toString();var stages=new JSONArray();for(Object raw:def.getJSONArray("nodes")){var n=(JSONObject)raw;stages.put(new JSONObject().put("name",n.getString("id")).put("kind",n.getString("kind")).put("status","pending"));}
        var w=new JSONObject().put("id",id).put("graph",def).put("graphSha256",WorldFrames.hash(def)).put("graphCodeSha256",SkillGraph046.code()).put("worldBinding",binding).put("title",title).put("request",request).put("modelName",model).put("saveSkillName",name).put("reviewEachNode",input.optBoolean("reviewEachNode")).put("status","running").put("step",stages.getJSONObject(0).getString("name")).put("stageIndex",0).put("stages",stages).put("resumes",0).put("graphRemainingMs",1_200_000L).put("createdAt",Instant.now()).put("history",new JSONArray()).put("auditHistory",new JSONArray()).put("diagnostics",new JSONArray()).put("memo",graphMemo.metrics());
        if(skill!=null)w.put("skill",new JSONObject().put("id",skill.getString("id")).put("name",skill.getString("name")).put("version",skill.getInt("version")));
        goals.reserveWorkflow(id);resetGraphClock(w);try{change(w,"Typed graph, exact verified procedures, source and wrench ceilings frozen before any effect");}catch(Exception e){goals.releaseWorkflow(id);throw e;}return view(w);
    }
    public synchronized JSONObject graphCatalog() throws Exception {
        var all=new JSONArray();for(Object raw:goals.procedures()){var p=(JSONObject)raw;if(p.getString("kind").equals(WorldPlan.PROCEDURE)){try{SkillGraph046.selected(goals,SkillGraph046.ref(p));all.put(new JSONObject().put("skill",SkillGraph046.ref(p)).put("defaults",p.getJSONObject("defaults")));}catch(Exception invalid){all.put(new JSONObject().put("name",p.getString("name")).put("version",p.getInt("version")).put("unavailable",invalid.getMessage()));}}}
        return new JSONObject().put("schema",SkillGraph046.SCHEMA).put("procedures",all).put("memo",graphMemo.metrics()).put("scope",SkillGraph046.SCOPE);
    }
    private JSONObject graphSource(JSONObject w,int index){return index==0?w.getJSONObject("worldBinding"):stage(w,index-1).getJSONObject("output").getJSONObject("world");}
    private void graphVerified(JSONObject w) throws Exception {
        if(!w.getString("graphCodeSha256").equals(SkillGraph046.code())||!w.getString("graphSha256").equals(WorldFrames.hash(w.getJSONObject("graph"))))throw new IllegalStateException("Graph implementation or definition changed; original retained");
        if(!graphMemo.get(w.getJSONObject("graph"),goals).similar(w.getJSONObject("graph")))throw new IllegalStateException("Graph contract changed");
        String previous=w.getString("graphSha256");boolean missing=false;
        for(int i=0;i<w.getJSONArray("stages").length()-1;i++){var s=stage(w,i);if(!s.has("output")){missing=true;continue;}if(missing)throw new IllegalStateException("Graph has an outcome before its prerequisite");var actual=SkillGraph046.outcome(goals,w.getString("id"),goals.read(s.getString("goalId")),graphSource(w,i));var output=s.getJSONObject("output");SkillGraph046.verifySeal(output,previous);if(!SkillGraph046.seal(actual,previous).similar(output))throw new IllegalStateException("Original graph outcome changed");previous=output.getString("sha256");}
        w.put("memo",graphMemo.metrics());
    }
    private void resetGraphClock(JSONObject w){graphBudgetId=w.getString("id");graphBudgetMs=w.getLong("graphRemainingMs");graphBudgetCheckpoint=graphClock=System.nanoTime();}
    private void chargeGraphClock(JSONObject w){
        if(!graphBudgetId.equals(w.getString("id")))resetGraphClock(w);long now=System.nanoTime(),elapsed=Math.max(0,(now-graphClock)/1_000_000);graphClock=now;graphBudgetMs=Math.max(0,graphBudgetMs-elapsed);w.put("graphRemainingMs",graphBudgetMs);if(graphBudgetMs==0)throw new IllegalStateException("Graph exhausted its retained1200seconds of awake execution");
    }
    private void tickGraph(JSONObject w){
        try{
            chargeGraphClock(w);if(System.nanoTime()-graphBudgetCheckpoint>=1_000_000_000L){save(w);graphBudgetCheckpoint=System.nanoTime();}
            if(w.getString("step").equals("audit"))return;
            if(w.getString("step").equals("complete")){completeGraph(w);return;}
            int index=w.getInt("stageIndex");var s=stage(w,index);var n=w.getJSONObject("graph").getJSONArray("nodes").getJSONObject(index);
            if(n.getString("kind").equals("compare")){
                graphVerified(w);SkillGraph046.current(goals,graphSource(w,index));
                var a=artifacts.writeOnce("graph-report:"+w.getString("id"),GraphReport046.build(w));if(w.has("report")&&!w.getJSONObject("report").similar(Creation.reference(a)))throw new IllegalStateException("Original graph report changed");
                w.put("report",Creation.reference(a)).put("step","audit");s.put("status","waiting-host");change(w,"All original outcomes reconstructed; actual comparison table and chart need Chromium verification");return;
            }
            if(!s.has("input")){
                graphVerified(w);var source=graphSource(w,index);SkillGraph046.current(goals,source);var intent=SkillGraph046.intent(n,source,w.getString("request"));
                var preview=goals.worlds().preview(intent);if(!preview.getBoolean("passed"))throw new IllegalStateException("Next typed route refused: "+preview.getString("reason"));
                s.put("input",intent).put("source",WorldFrames.copy(source));change(w,"Saved "+s.getString("name")+" exact source/skill intent before stable child admission");return;
            }
            // Re-resolve exact skill on every dispatch/reconciliation. The owned child checks byte-equivalent intent.
            if(!s.has("goalId"))SkillGraph046.selected(goals,n.getJSONObject("skill"));
            var g=goals.startProcedureOwned046(w.getString("id"),"graph:"+s.getString("name"),s.getJSONObject("input"));s.put("goalId",g.getString("id"));w.put("currentChild",g);
            if(!g.getString("status").equals("completed")){
                if(!s.optString("childUpdatedAt").equals(g.optString("updatedAt"))){s.put("status",g.getString("status")).put("childUpdatedAt",g.optString("updatedAt"));save(w);}
                if(Set.of("interrupted","cancelled").contains(g.getString("status")))throw new IllegalStateException("Owned graph node stopped: "+g.getString("reason"));return;
            }
            var outcome=SkillGraph046.outcome(goals,w.getString("id"),g,s.getJSONObject("source"));String previous=index==0?w.getString("graphSha256"):stage(w,index-1).getJSONObject("output").getString("sha256");
            s.put("status","completed").put("output",SkillGraph046.seal(outcome,previous)).put("runId",g.getString("runId")).put("artifact",outcome.getJSONObject("report"));
            w.put("stageIndex",index+1).put("step",stage(w,index+1).getString("name"));
            if(w.optBoolean("reviewEachNode")&&index+1<w.getJSONArray("stages").length()-1)w.put("status","interrupted");
            change(w,w.getString("status").equals("interrupted")?"Verified node committed; review the real handoff and explicitly Resume":"Verified node committed; successor will bind this exact retained world revision");
        }catch(Throwable error){try{interrupt(w,String.valueOf(error.getMessage()));}catch(Exception failed){System.err.println("Graph checkpoint: "+failed);}}
    }
    private JSONObject claimGraph(JSONObject w) throws Exception {
        try{var previous=w.optJSONObject("claim");if(previous!=null&&previous.getLong("expires")>System.currentTimeMillis())return new JSONObject();graphVerified(w);var a=graphArtifact(w);SkillGraph046.current(goals,graphSource(w,w.getInt("stageIndex")));
            var c=new JSONObject().put("nonce",UUID.randomUUID().toString()).put("expires",System.currentTimeMillis()+30_000).put("id",UUID.randomUUID().toString());w.put("claim",c);save(w);
            return WorldFrames.copy(c).put("kind","workflow.graph-audit").put("workflowId",w.getString("id")).put("report",Creation.reference(a)).put("rowCount",GraphReport046.rows(w).length());
        }catch(Exception error){interrupt(w,"Graph report preflight: "+error.getMessage());return new JSONObject();}
    }
    private JSONObject graphArtifact(JSONObject w) throws Exception {
        var a=artifacts.read(w.getJSONObject("report").getString("id"));if(!Creation.reference(a).similar(w.getJSONObject("report"))||!a.getJSONObject("contents").similar(GraphReport046.build(w).getJSONObject("files")))throw new IllegalStateException("Original graph artifact content/version changed");return a;
    }
    private JSONObject acknowledgeGraph(JSONObject input) throws Exception {
        SpatialScene.keys(input,"workflowId","id","nonce","result","error");var w=raw(input.getString("workflowId"));var c=w.optJSONObject("claim");
        if(!w.getString("status").equals("running")||!w.getString("step").equals("audit")||c==null||!c.getString("id").equals(input.getString("id"))||!c.getString("nonce").equals(input.getString("nonce"))||c.getLong("expires")<System.currentTimeMillis())throw new IllegalStateException("Stale or foreign graph audit");w.remove("claim");
        try{if(input.has("error"))throw new IllegalStateException(input.getString("error"));graphVerified(w);var audit=GraphReport046.audit(w,input.getJSONObject("result"),graphArtifact(w));w.put("audit",audit).put("step","complete");w.getJSONArray("auditHistory").put(audit);change(w,"Every original graph table cell and native time chart point verified in Chromium");}
        catch(Exception error){w.getJSONArray("auditHistory").put(new JSONObject().put("passed",false).put("error",String.valueOf(error.getMessage())));interrupt(w,"Independent graph audit refused: "+error.getMessage());}return view(w);
    }
    private void completeGraph(JSONObject w) throws Exception {
        graphVerified(w);var a=graphArtifact(w);var audit=w.getJSONObject("audit");if(!audit.getBoolean("passed")||!audit.getJSONObject("artifact").similar(Creation.reference(a))||!audit.getString("dataSha256").equals(WorldFrames.hash(GraphReport046.data(w))))throw new IllegalStateException("Graph audit no longer matches original outcomes");
        SkillGraph046.current(goals,graphSource(w,w.getInt("stageIndex")));stage(w,w.getInt("stageIndex")).put("status","completed");
        double nativeTime=0;for(int i=0;i<w.getInt("stageIndex");i++)nativeTime+=stage(w,i).getJSONObject("output").getJSONObject("metrics").getDouble("timeS");
        chargeGraphClock(w);w.put("metrics",new JSONObject().put("stages",w.getJSONArray("stages").length()).put("worldCommits",w.getInt("stageIndex")).put("nativeTimeS",nativeTime).put("awakeElapsedMs",1_200_000-w.getLong("graphRemainingMs")).put("modelCalls",0).put("cells",audit.getInt("cells")).put("maxPlotError",audit.getDouble("maxPlotError")));
        if(!w.getString("saveSkillName").isEmpty()){
            var p=new JSONObject().put("kind",SkillGraph046.KIND).put("sourceWorkflowId",w.getString("id")).put("graph",w.getJSONObject("graph")).put("graphSha256",w.getString("graphSha256")).put("graphCodeSha256",w.getString("graphCodeSha256")).put("report",w.getJSONObject("report")).put("defaults",new JSONObject().put("title",w.getString("title")).put("request",w.getString("request")).put("modelName",w.getString("modelName")).put("reviewEachNode",w.getBoolean("reviewEachNode"))).put("scope",SkillGraph046.SCOPE);
            w.put("savedSkill",goals.retainWorkflow(p,w.getString("saveSkillName")));
        }
        w.put("status","completed");change(w,"Exact procedure graph, original native commits and actual rendered comparison verified"+(w.has("savedSkill")?"; graph version retained":""));goals.releaseWorkflow(w.getString("id"));artifacts.event("java_workflow_completed",view(w));
    }
    private JSONObject resumeGraph(JSONObject w) throws Exception {
        if(!w.getString("status").equals("interrupted")||w.getInt("resumes")>=3||w.getLong("graphRemainingMs")<=0)throw new IllegalStateException("Resume an interrupted graph within its three-resume/retained-time budget");graphVerified(w);
        var s=stage(w,w.getInt("stageIndex"));recoverPointer(w,s);
        if(s.has("goalId")){var g=goals.read(s.getString("goalId"));if(g.getString("status").equals("interrupted"))goals.controlOwned(w.getString("id"),g.getString("id"),false);else if(g.getString("status").equals("cancelled"))throw new IllegalStateException("Graph child was cancelled; start a new graph");else if(g.getString("status").equals("completed"))SkillGraph046.outcome(goals,w.getString("id"),g,s.getJSONObject("source"));}
        else SkillGraph046.current(goals,graphSource(w,w.getInt("stageIndex")));
        w.put("status","running").put("resumes",w.getInt("resumes")+1);w.remove("claim");resetGraphClock(w);change(w,"Explicit continuation keeps the same owned child, committed outcomes and remaining awake-time budget");return view(w);
    }
    private JSONObject reuseGraph(JSONObject input) throws Exception {
        SpatialScene.keys(input,"name","version","worldBinding","request","title","modelName","saveSkillName","reviewEachNode");JSONObject chosen=null;for(Object raw:goals.procedures()){var p=(JSONObject)raw;if(p.getString("name").equals(input.getString("name"))&&p.getInt("version")==input.getInt("version"))chosen=p;}
        if(chosen==null||!chosen.getString("kind").equals(SkillGraph046.KIND)||!chosen.getString("graphCodeSha256").equals(SkillGraph046.code())||!chosen.getString("graphSha256").equals(WorldFrames.hash(chosen.getJSONObject("graph"))))throw new IllegalStateException("Unknown or incompatible retained graph version");
        var source=raw(chosen.getString("sourceWorkflowId"));if(!source.getString("status").equals("completed"))throw new IllegalStateException("Retained graph lost its completed source");graphVerified(source);graphArtifact(source);if(!source.getJSONObject("graph").similar(chosen.getJSONObject("graph"))||!source.getJSONObject("report").similar(chosen.getJSONObject("report"))||!source.getJSONObject("audit").getBoolean("passed")||!source.getJSONObject("audit").getString("dataSha256").equals(WorldFrames.hash(GraphReport046.data(source))))throw new IllegalStateException("Retained graph differs from its original verified procedure graph");
        var args=WorldFrames.copy(chosen.getJSONObject("defaults")).put("graph",chosen.getJSONObject("graph"));for(String k:input.keySet())if(!Set.of("name","version").contains(k))args.put(k,input.get(k));args.put("saveSkillName",input.optString("saveSkillName",""));return startGraph(args,chosen);
    }
    @Override public synchronized void close(){closed=true;worker.shutdownNow();}
}
