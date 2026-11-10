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
    private String qualificationParent105="",qualificationChildId105="";private JSONObject qualificationCase105;
    private final Map<String,Long> qualificationClocks105=new HashMap<>();
    private final GoalRunner goals;private final MeasurementStore measurements;private final SceneStore scenes;private final WorkspaceStore artifacts;
    private final SkillGraph046.ContractMemo graphMemo=new SkillGraph046.ContractMemo();private long graphClock=System.nanoTime(),graphBudgetMs,graphBudgetCheckpoint;private String graphBudgetId="";
    private final SchemaRegistry schemas=new SchemaRegistry();private final FactoryWorld world;private final WorldCheckpointStore checkpoint;
    private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"java-workflow-runner");t.setDaemon(true);return t;});private boolean closed;
    public WorkflowRunner(Path root,GoalRunner goals,MeasurementStore measurements,SceneStore scenes,WorkspaceStore artifacts) throws Exception {
        this.goals=goals;this.measurements=measurements;this.scenes=scenes;this.artifacts=artifacts;schemas.register(new ComponentSchema("workspace.workflow",1,Map.of("payload",ComponentSchema.Kind.TEXT),Set.of("payload")));world=new FactoryWorld(schemas);checkpoint=new WorldCheckpointStore(root.resolve("workflows/tasks.fworld"));if(Files.exists(checkpoint.path()))world.restore(checkpoint.load(schemas).snapshot());
        for(var w:values())if(!settled(w)){w.put("status","interrupted");w.remove("claim");change(w,"Process ended; resume explicitly from verified stages");if(!Qualification105.own(w))goals.reserveWorkflow(w.getString("id"));}
        for(var w:values())if(Qualification105.own(w)&&!settled(w)){var child=leaf105(w);if(child==null||settled(child))goals.reserveWorkflow(w.getString("id"));}
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
    private JSONObject view(JSONObject w){if(Qualification105.own(w))return view105(w);var out=new JSONObject().put("workflow",true).put("procedure",graph(w)?SkillGraph046.KIND:dataCreation101(w)?w.getString("profile101"):PROCEDURE).put("attempt",w.getInt("resumes")+1);for(String k:List.of("id","title","request","modelName","status","step","resumes","createdAt","updatedAt","reason","history","stages","currentChild","bundle","audit","auditHistory","report","metrics","savedSkill","skill","dataBinding","graph","graphSha256","graphCodeSha256","worldBinding","graphRemainingMs","diagnostics","memo","reviewEachNode","profile101","code101","pendingSkill101","implementation103","qualification104","qualificationOwner105","qualificationCase105"))if(w.has(k))out.put(k,w.get(k));return out;}
    public synchronized JSONObject read(String id){return view(raw(id));}
    public synchronized JSONArray list(){var all=new JSONArray();for(var w:values())all.put(view(w));return all;}
    private JSONObject definition(JSONObject input) throws Exception {
        if(input.has("profile101"))return DataSummary104.definition(input,measurements);
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
    private JSONObject start(JSONObject input,JSONObject skill) throws Exception {return start(input,skill,null);}
    private JSONObject start(JSONObject input,JSONObject skill,JSONObject implementation) throws Exception {
        if(input.has("qualify105"))return start105(input);
        if(input.has("graph"))return startGraph(input,skill);
        if(closed||active()!=null&&!active().getString("id").equals(qualificationParent105)||executableCount105()>=20)throw new IllegalStateException("Finish/resume/stop the current workflow; history limit20");goals.workflowCapacity();var def=definition(input);String id=qualificationParent105.isEmpty()?UUID.randomUUID().toString():qualificationChildId105;var stages=new JSONArray();for(String stage:def.has("profile101")?DataCreation101.STAGES:STAGES)stages.put(new JSONObject().put("name",stage).put("status","pending"));
        var w=new JSONObject().put("id",id).put("definition",def).put("title",def.getString("title")).put("request",def.getString("request")).put("modelName",def.getString("modelName")).put("dataBinding",MotionData.binding(measurements.read(def.getString("datasetId"),def.getInt("datasetVersion")))).put("status","running").put("step","data").put("stages",stages).put("stageIndex",0).put("resumes",0).put("deadline",System.currentTimeMillis()+1_200_000).put("createdAt",Instant.now()).put("history",new JSONArray()).put("auditHistory",new JSONArray());if(skill!=null)w.put("skill",new JSONObject().put("name",skill.getString("name")).put("version",skill.getInt("version")).put("id",skill.getString("id")));
        if(!qualificationParent105.isEmpty()){w.put("qualificationOwner105",qualificationParent105);if(qualificationCase105!=null)w.put("qualificationCase105",WorldFrames.copy(qualificationCase105));}
        if(def.has("profile101"))w.put("profile101",def.getString("profile101")).put("code101",DataSummary104.code(def.getString("profile101")));
        if(implementation!=null)w.put("implementation103",WorldFrames.copy(implementation));
        goals.reserveWorkflow(id);try{change(w,"Request, source, conditions and registered stages frozen before execution");}catch(Exception e){goals.releaseWorkflow(id);throw e;}return view(w);
    }
    private JSONObject source(JSONObject w) throws Exception {if(w.has("implementation103"))verifyImplementation103(w);if(w.has("qualification104"))verifyQualification104(w);var d=w.getJSONObject("definition");var data=measurements.read(d.getString("datasetId"),d.getInt("datasetVersion"));if(!MotionData.binding(data).similar(w.getJSONObject("dataBinding")))throw new IllegalStateException("Frozen observation source or mapping changed");MotionData.originalRows(data);if(dataCreation101(w)){if(!w.getString("code101").equals(DataSummary104.code(w.getString("profile101"))))throw new IllegalStateException("Data-creation verifier changed; start a newly reviewed workflow");return data;}var p=d.getJSONObject("physics");if(p.has("sceneId")&&!MeasurementStore.hash(scenes.read(p.getString("sceneId")).toString()).equals(p.getString("sourceSceneSha256")))throw new IllegalStateException("Frozen authored simulation scene changed");return data;}
    private JSONObject stage(JSONObject w,int index){return w.getJSONArray("stages").getJSONObject(index);}
    private JSONObject verifiedStage(JSONObject w,int index) throws Exception {var s=stage(w,index);var g=goals.verifiedOwned(w.getString("id"),s.getString("goalId"));if(index==1&&DataSummary104.own(w)&&!g.has("summary104"))throw new IllegalStateException("Input-driven workflow lost its authored implementation");if(index==1&&w.has("implementation103")&&!w.getJSONObject("implementation103").similar(g.optJSONObject("implementation103")))throw new IllegalStateException("Owned creation implementation lineage changed");if(!g.getString("workflowStage").equals(stageNames(w).get(index)))throw new IllegalStateException("Owned stage family changed");if(s.has("artifact")&&!Creation.reference(artifacts.read(g.getJSONObject("report").getString("id"))).similar(s.getJSONObject("artifact")))throw new IllegalStateException("Verified stage artifact changed");return g;}
    private JSONObject bundle(JSONObject w) throws Exception {
        var data=source(w);var dg=verifiedStage(w,0);var pg=verifiedStage(w,1);var run=goals.ownedNativeRun(w.getString("id"),pg.getString("id"));var args=WorkflowEvidence.bundle(w,data,dg,pg,run);var a=artifacts.writeOnce("workflow-bundle:"+w.getString("id"),args);if(a.getInt("version")!=1)throw new IllegalStateException("Evidence bundle was externally edited");if(w.has("bundle")&&!Creation.reference(a).similar(w.getJSONObject("bundle")))throw new IllegalStateException("Evidence bundle identity changed");return a;
    }
    private JSONObject intent(JSONObject w,int index) throws Exception {
        var d=w.getJSONObject("definition");if(index==0)return new JSONObject().put("request",d.getString("request")).put("datasetId",d.getString("datasetId")).put("datasetVersion",d.getInt("datasetVersion")).put("maxGapS",d.getDouble("maxGapS")).put("speedLimitMps",d.getDouble("speedLimitMps"));
        if(dataCreation101(w)){var in=DataSummary104.creation(w,handoff101(w));if(w.has("implementation103"))in.put("implementation103",w.getJSONObject("implementation103"));return in;}
        if(index==1){var p=new JSONObject(d.getJSONObject("physics").toString());p.remove("sourceSceneSha256");return p.put("request",d.getString("request"));}
        var a=bundle(w);w.put("bundle",Creation.reference(a));return WorkflowEvidence.creation(w,a);
    }
    private synchronized void tick(){
        if(closed)return;var w=active();if(w==null||!w.getString("status").equals("running"))return;
        if(Qualification105.own(w)){tick105(w);return;}tickSingle105(w);
    }
    private void tickSingle105(JSONObject w){
        if(graph(w)){tickGraph(w);return;}
        try{
            if(System.currentTimeMillis()>w.getLong("deadline"))throw new IllegalStateException("Workflow attempt exceeded1200seconds");
            if(w.getString("step").equals("audit"))return;
            if(w.getString("step").equals("complete")){complete(w);return;}
            if(w.has("implementation103"))verifyImplementation103(w);
            int index=w.getInt("stageIndex");var s=stage(w,index);if(!s.has("input")){source(w);s.put("input",intent(w,index));change(w,"Saved "+s.getString("name")+" intent before starting its stable owned child");return;}
            var g=goals.startOwned(w.getString("id"),s.getString("name"),s.getJSONObject("input"));s.put("goalId",g.getString("id"));
            if(!g.getString("status").equals("completed")){if(!s.optString("childUpdatedAt").equals(g.optString("updatedAt"))){s.put("status",g.getString("status")).put("childUpdatedAt",g.optString("updatedAt"));w.put("currentChild",g);save(w);}if(Set.of("interrupted","cancelled").contains(g.getString("status")))throw new IllegalStateException("Owned "+s.getString("name")+" stage stopped: "+g.getString("reason"));return;}
            var raw=goals.verifiedOwned(w.getString("id"),g.getString("id"));s.put("status","completed").put("artifact",Creation.reference(artifacts.read(raw.getJSONObject("report").getString("id")))).put("metrics",raw.getJSONObject("metrics"));if(raw.has("runId"))s.put("runId",raw.getString("runId"));w.put("currentChild",g);
            if(index<stageNames(w).size()-1){w.put("stageIndex",index+1).put("step",stageNames(w).get(index+1));if(dataCreation101(w)&&w.getJSONObject("definition").optBoolean("reviewAfterData101"))w.put("status","interrupted");change(w,"Verified "+s.getString("name")+" stage; retained outcome will not be repeated"+(w.getString("status").equals("interrupted")?". Requested review before creation; explicitly Resume.":""));}
            else{w.put("step","audit").put("report",s.getJSONObject("artifact"));w.remove("claim");change(w,dataCreation101(w)?"Artifact behavior passed; independent visible rows and filter-state audit is next":"Generated behavior passed; independent source-cell and native SVG geometry audit is next");}
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
        var w=active();if(w!=null&&Qualification105.own(w)){if(!w.getString("status").equals("running"))return new JSONObject();w=leaf105(w);}if(w==null||!w.getString("status").equals("running")||!w.getString("step").equals("audit"))return new JSONObject();if(graph(w))return claimGraph(w);if(dataCreation101(w))return claim101(w);try{var previous=w.optJSONObject("claim");if(previous!=null&&previous.getLong("expires")>System.currentTimeMillis())return new JSONObject();source(w);verifiedStage(w,2);var b=bundle(w);int rows=MeasurementStore.csv(b.getJSONObject("contents").getString("evidence.csv")).size()-1;
        var c=new JSONObject().put("nonce",UUID.randomUUID().toString()).put("expires",System.currentTimeMillis()+30_000).put("id",UUID.randomUUID().toString());w.put("claim",c);save(w);return new JSONObject(c.toString()).put("kind","workflow.audit").put("workflowId",w.getString("id")).put("report",w.getJSONObject("report")).put("rowCount",rows).put("responsive",w.getJSONObject("definition").optBoolean("responsive",false));}catch(Exception failure){interrupt(w,"Independent audit preflight: "+failure.getMessage());return new JSONObject();}
    }
    synchronized void hostDetached042()throws Exception{var w=active();if(w!=null&&Qualification105.own(w))w=leaf105(w);if(w!=null&&w.has("claim"))interrupt(w,"Interface ended during claimed canvas audit; explicitly Resume after inspection");}
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
        var w=raw(id);if(Qualification105.own(w))return resume105(w);requireChildControl105(w);if(graph(w))return resumeGraph(w);if(!w.getString("status").equals("interrupted")||w.getInt("resumes")>=3)throw new IllegalStateException("Resume only an interrupted workflow, at most3 times");source(w);for(int i=0;i<w.getInt("stageIndex");i++)verifiedStage(w,i);if(w.has("bundle"))bundle(w);var s=stage(w,w.getInt("stageIndex"));if(!s.has("goalId")&&s.has("input")){var recovered=goals.startOwned(id,s.getString("name"),s.getJSONObject("input"));s.put("goalId",recovered.getString("id"));}if(s.has("goalId")){var g=goals.read(s.getString("goalId"));if(g.getString("status").equals("interrupted"))goals.controlOwned(id,g.getString("id"),false);else if(g.getString("status").equals("cancelled"))throw new IllegalStateException("Owned stage was cancelled; start a new workflow");else if(g.getString("status").equals("completed"))goals.verifiedOwned(id,g.getString("id"));}
        w.put("status","running").put("resumes",w.getInt("resumes")+1).put("deadline",System.currentTimeMillis()+1_200_000);w.remove("claim");change(w,"Explicit resume reuses verified completed stages and the same owned child intent");return view(w);
    }
    private void recoverPointer(JSONObject w,JSONObject s){if(!s.has("goalId")){var g=goals.findOwned(w.getString("id"),(graph(w)?"graph:":"")+s.getString("name"));if(g!=null)s.put("goalId",g.getString("id"));}}
    public synchronized JSONObject cancel(String id) throws Exception {var w=raw(id);if(Qualification105.own(w))return cancel105(w);requireChildControl105(w);if(settled(w))throw new IllegalStateException("Workflow already settled");var s=stage(w,w.getInt("stageIndex"));recoverPointer(w,s);if(s.has("goalId")){var g=goals.read(s.getString("goalId"));if(Set.of("running","waiting-host","interrupted").contains(g.getString("status")))goals.controlOwned(id,g.getString("id"),true);}w.put("status","cancelled");w.remove("claim");change(w,"Stopped owned work; no workflow skill promoted");goals.releaseWorkflow(id);return view(w);}
    public synchronized JSONObject reuse(JSONObject input) throws Exception {
        if(input.has("worldBinding"))return reuseGraph(input);
        for(Object raw:goals.procedures()){var p=(JSONObject)raw;if(p.getString("name").equals(input.getString("name"))&&p.getInt("version")==input.getInt("version")&&DataSummary104.supported(p.getString("kind")))return reuse101(input,p);}
        SpatialScene.keys(input,"name","version","request","title","datasetId","datasetVersion","parameters","targetM","disturbanceMps","maxGapS","speedLimitMps","modelName","saveSkillName","responsive");JSONObject chosen=null;for(Object raw:goals.procedures()){var p=(JSONObject)raw;if(p.getString("name").equals(input.getString("name"))&&p.getInt("version")==input.getInt("version"))chosen=p;}if(chosen==null||!chosen.getString("kind").equals(PROCEDURE))throw new IllegalArgumentException("Select a retained evidence workflow");var args=new JSONObject(chosen.getJSONObject("defaults").toString());for(String k:input.keySet())if(!Set.of("name","version").contains(k)){if(k.equals("parameters")){for(String key:input.getJSONObject(k).keySet())args.getJSONObject(k).put(key,input.getJSONObject(k).get(key));}else args.put(k,input.get(k));}args.put("saveSkillName",input.optString("saveSkillName",""));return start(args,chosen);
    }
    private static boolean dataCreation101(JSONObject w){return DataSummary104.supported(w.optString("profile101"));}
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
        if(w.getJSONObject("audit").has("summaries102"))actual.put("summaries102",w.getJSONObject("audit").getJSONArray("summaries102"));var audit=DataSummary104.audit(w,a,data,handoff,actual);
        if(!audit.getBoolean("passed")||!audit.similar(w.getJSONObject("audit")))throw new IllegalStateException("Original visible data-creation evidence changed");return audit;
    }
    private JSONObject claim101(JSONObject w) throws Exception {
        try{var old=w.optJSONObject("claim");if(old!=null&&old.getLong("expires")>System.currentTimeMillis())return new JSONObject();var handoff=handoff101(w);var child=verifiedStage(w,1);
            if(!child.getJSONObject("dataHandoff100").similar(handoff))throw new IllegalStateException("Foreign data-creation child");
            var c=new JSONObject().put("nonce",UUID.randomUUID().toString()).put("expires",System.currentTimeMillis()+30_000).put("id",UUID.randomUUID().toString());w.put("claim",c);save(w);
            return WorldFrames.copy(c).put("kind",DataSummary104.summaryKind(w.getString("profile101"))?"workflow.data-summary-audit102":"workflow.data-creation-audit101").put("workflowId",w.getString("id")).put("report",w.getJSONObject("report")).put("filter",handoff.getJSONObject("preview").getJSONArray("sample").getJSONObject(0).getString("source_row")).put("rowCount",handoff.getJSONObject("preview").getInt("rowCount"));
        }catch(Exception e){interrupt(w,"Data-creation audit preflight: "+e.getMessage());return new JSONObject();}
    }
    private JSONObject acknowledge101(JSONObject input) throws Exception {
        SpatialScene.keys(input,"workflowId","id","nonce","result","error");var w=raw(input.getString("workflowId"));var c=w.optJSONObject("claim");
        if(!w.getString("status").equals("running")||!w.getString("step").equals("audit")||c==null||!c.getString("id").equals(input.getString("id"))||!c.getString("nonce").equals(input.getString("nonce"))||c.getLong("expires")<System.currentTimeMillis())throw new IllegalStateException("Stale or foreign data-creation audit");w.remove("claim");
        try{if(input.has("error"))throw new IllegalStateException(input.getString("error"));var child=verifiedStage(w,1);var a=artifacts.read(child.getJSONObject("report").getString("id"));var audit=DataSummary104.audit(w,a,source(w),handoff101(w),input.getJSONObject("result"));w.getJSONArray("auditHistory").put(audit);
            if(audit.getBoolean("passed")){w.put("audit",audit).put("step","complete");change(w,"Every required source value, visible label, filter state and registered output independently verified");}
            else{goals.repairOwnedCreation(w.getString("id"),child.getString("id"),new JSONArray().put(new JSONObject().put("passed",false).put("error","Independent data-creation audit failed").put("details",audit.getJSONArray("errors"))));stage(w,1).remove("artifact");stage(w,1).put("status","running");w.put("step","creation");change(w,"Actual audit refused this draft; repair retains the original contract and total draft budget");}
        }catch(Exception e){interrupt(w,e.getMessage());}return view(w);
    }
    private void complete101(JSONObject w) throws Exception {
        if(w.has("qualificationOwner105"))checked108(raw(w.getString("qualificationOwner105")));
        var audit=verified101(w);w.put("metrics",new JSONObject().put("stages",2).put("observationRows",audit.getInt("rowCount")).put("auditedFrames",4).put("qualityPassed",stage(w,0).getJSONObject("metrics").getBoolean("qualityPassed")));
        String name=w.getJSONObject("definition").getString("saveSkillName");if(!name.isEmpty()){if(DataSummary104.own(w)&&!w.has("qualification104"))w.put("qualification104",qualify104(w));w.put("pendingSkill101",goals.retainWorkflow(procedure103(w),name));}
        w.put("status","completed");change(w,"Data, content and independent audit completed; skill publication follows durable parent completion");
        if(w.has("pendingSkill101"))publish101(w);goals.releaseWorkflow(w.getString("id"));artifacts.event("java_workflow_completed",view(w));
    }
    private void publish101(JSONObject w) throws Exception {
        if(w.has("qualificationOwner105"))checked108(raw(w.getString("qualificationOwner105")));
        if(!w.getString("status").equals("completed"))throw new IllegalStateException("Skill parent not completed");verified101(w);
        var ref=w.getJSONObject("pendingSkill101");
        // Save the exact parent linkage before exposing the skill. Keep pending until cleanup is durable.
        w.put("savedSkill",ref);change(w,"Completed parent and exact skill linkage persisted; publication pending");
        goals.publishWorkflow101(procedure103(w),ref);w.remove("pendingSkill101");change(w,"Completed parent and original evidence verified; whole data-creation procedure published");
    }
    private JSONObject reuse101(JSONObject input,JSONObject chosen) throws Exception {
        SpatialScene.keys(input,"name","version","request","title","modelName","datasetId","datasetVersion","maxGapS","speedLimitMps","saveSkillName","reviewAfterData101","reuseImplementation103");
        if(input.has("reuseImplementation103")&&!(input.get("reuseImplementation103") instanceof Boolean))throw new IllegalArgumentException("Implementation reuse requires a boolean");
        if(!chosen.optString("publication101").equals("published"))throw new IllegalStateException("Whole procedure is pending publication");var parent=raw(chosen.getString("sourceWorkflowId"));
        if(parent.has("qualificationOwner105")&&!raw(parent.getString("qualificationOwner105")).getJSONObject("qualification105").getString("codeSha256").equals(Qualification105.code()))throw new IllegalStateException("Saved procedure qualification uses earlier coordination code; explicitly request upgrade summary qualification before reuse");
        if(parent.has("qualificationOwner105"))checked108(raw(parent.getString("qualificationOwner105")));
        if(!dataCreation101(parent)||!parent.getString("status").equals("completed")||!parent.has("savedSkill")||!parent.getJSONObject("savedSkill").getString("id").equals(chosen.getString("id")))throw new IllegalStateException("Procedure lacks a completed published parent");
        verified101(parent);var expected=procedure103(parent);for(String k:expected.keySet())if(!k.equals("publication101")&&!new JSONObject().put("v",expected.get(k)).similar(new JSONObject().put("v",chosen.get(k))))throw new IllegalStateException("Original whole-procedure contract changed");
        var args=WorldFrames.copy(chosen.getJSONObject("defaults"));for(String k:input.keySet())if(!Set.of("name","version","reuseImplementation103").contains(k))args.put(k,input.get(k));args.put("saveSkillName",input.optString("saveSkillName",""));
        JSONObject implementation=null;if(input.optBoolean("reuseImplementation103")){var child=verifiedStage(parent,1);implementation=Implementation103.binding(parent,child,artifacts.read(child.getJSONObject("report").getString("id")));}
        return start(args,chosen,implementation);
    }
    private static JSONObject procedure103(JSONObject w){var p=DataSummary104.procedure(w);if(w.has("implementation103"))p.put("implementation103",WorldFrames.copy(w.getJSONObject("implementation103")));return p;}
    private final Set<String> checkingImplementations103=new HashSet<>();
    private void verifyImplementation103(JSONObject w) throws Exception {
        if(checkingImplementations103.size()>=8||!checkingImplementations103.add(w.getString("id")))throw new IllegalStateException("Implementation ancestry is cyclic or exceeds eight links");
        try{
            var binding=w.getJSONObject("implementation103");Implementation103.current(binding);var parent=raw(binding.getString("sourceWorkflowId"));
            if(parent.has("qualificationOwner105"))checked108(raw(parent.getString("qualificationOwner105")));
            if(!parent.getString("status").equals("completed")||!parent.has("savedSkill"))throw new IllegalStateException("Implementation parent is not a published completion");
            verified101(parent);var child=verifiedStage(parent,1);var expected=Implementation103.binding(parent,child,artifacts.read(child.getJSONObject("report").getString("id")));
            if(!binding.similar(expected)||!binding.getJSONObject("skill").similar(w.getJSONObject("skill"))||!binding.getString("profile").equals(w.getString("profile101")))throw new IllegalStateException("Frozen implementation lineage changed");
            var skill=goals.procedures().toList().stream().map(x->new JSONObject((Map<?,?>)x)).filter(p->p.getString("id").equals(parent.getJSONObject("savedSkill").getString("id"))).findFirst().orElseThrow(()->new IllegalStateException("Original published skill missing"));
            for(String k:procedure103(parent).keySet())if(!k.equals("publication101")&&!new JSONObject().put("v",procedure103(parent).get(k)).similar(new JSONObject().put("v",skill.get(k))))throw new IllegalStateException("Original procedure contract changed");
            if(!skill.optString("publication101").equals("published"))throw new IllegalStateException("Original skill is unpublished");
        }finally{checkingImplementations103.remove(w.getString("id"));}
    }
    private JSONObject qualificationProof104(JSONObject candidate)throws Exception {
        if(!DataSummary104.own(candidate)||!candidate.getString("status").equals("completed")||candidate.has("qualification104")||candidate.has("implementation103"))throw new IllegalStateException("Qualification requires an original completed input-driven report");
        verified101(candidate);var d=candidate.getJSONObject("definition");var data=measurements.read(d.getString("datasetId"),d.getInt("datasetVersion"));var handoff=handoff101(candidate);
        return new JSONObject().put("workflowId",candidate.getString("id")).put("report",candidate.getJSONObject("report")).put("dataBinding",candidate.getJSONObject("dataBinding")).put("features",new JSONArray(new TreeSet<>(DataSummary104.features(data,handoff))));
    }
    private JSONObject qualify104(JSONObject w)throws Exception {
        var proofs=new JSONArray();for(var p:values())if(!p.getString("id").equals(w.getString("id"))&&DataSummary104.own(p)&&p.getString("status").equals("completed")&&!p.has("qualification104")&&!p.has("implementation103"))proofs.put(qualificationProof104(p));
        var q=new JSONObject().put("schema","summary-qualification-104-v1").put("codeSha256",DataSummary104.code(DataSummary104.KIND)).put("proofs",proofs);checkCoverage104(q);return q;
    }
    private void checkCoverage104(JSONObject q){
        var seen=new HashSet<String>();var inputs=new HashSet<String>();var features=new HashSet<String>();var proofs=q.getJSONArray("proofs");if(proofs.length()>19)throw new IllegalStateException("Qualification proof budget exceeded");
        for(Object row:proofs){var p=(JSONObject)row;if(!seen.add(p.getString("workflowId")))throw new IllegalStateException("Duplicate qualification proof");var b=p.getJSONObject("dataBinding");inputs.add(b.getString("sourceSha256")+":"+b.getString("mappingSha256"));for(Object f:p.getJSONArray("features"))features.add((String)f);}
        if(inputs.size()<4||!features.containsAll(DataSummary104.COVERAGE))throw new IllegalStateException("Before saving, verify at least four distinct datasets covering monotonic, reversal, flat, gap plus speed flags and changed units. Stop this request, run those examples without saving, then start a fresh save request.");
    }
    private void verifyQualification104(JSONObject w)throws Exception {
        var q=w.getJSONObject("qualification104");SpatialScene.keys(q,"schema","codeSha256","proofs");if(!DataSummary104.own(w)||!q.getString("schema").equals("summary-qualification-104-v1")||!q.getString("codeSha256").equals(DataSummary104.code(DataSummary104.KIND)))throw new IllegalStateException("Summary qualification identity changed");
        for(Object row:q.getJSONArray("proofs")){var p=(JSONObject)row;if(p.getString("workflowId").equals(w.getString("id"))||!p.similar(qualificationProof104(raw(p.getString("workflowId")))))throw new IllegalStateException("Original qualification proof changed");}checkCoverage104(q);
    }
    private long executableCount105(){return values().stream().filter(w->!Qualification105.own(w)).count();}
    private JSONObject leaf105(JSONObject w){
        if(!Qualification105.own(w))return w;var s=stage(w,w.getInt("stageIndex"));
        if(s.optBoolean("reused")||!s.has("workflowId"))return null;
        var found=values().stream().filter(v->v.getString("id").equals(s.getString("workflowId"))).findFirst();
        if(found.isEmpty())return null;var child=found.get();
        if(!child.optString("qualificationOwner105").equals(w.getString("id"))||!child.getJSONObject("definition").similar(s.getJSONObject("input")))throw new IllegalStateException("Qualification child ownership or intent changed");
        return child;
    }
    private JSONObject view105(JSONObject w){
        var out=new JSONObject().put("workflow",true).put("procedure",Qualification105.KIND).put("attempt",w.getInt("resumes")+1);
        for(String k:List.of("id","title","request","modelName","status","step","resumes","createdAt","updatedAt","reason","history","stages","dataBinding","audit","report","savedSkill","metrics","cost105","remainingMs105"))if(w.has(k))out.put(k,w.get(k));
        out.put("qualification105",Qualification105.summary(w));var leaf=leaf105(w);
        if(leaf!=null){out.put("activeWorkflow105",view(leaf));if(leaf.has("currentChild"))out.put("currentChild",leaf.getJSONObject("currentChild"));}
        return out;
    }
    synchronized String authorityOwner105(String childId){
        var child=raw(childId);if(!child.has("qualificationOwner105"))return childId;
        var parent=raw(child.getString("qualificationOwner105"));var leaf=leaf105(parent);
        if(!parent.getString("status").equals("running")||leaf==null||!leaf.getString("id").equals(childId))throw new IllegalStateException("No active qualification parent owns this audit");
        return parent.getString("id");
    }
    private void requireChildControl105(JSONObject child){if(child.has("qualificationOwner105")&&!child.getString("qualificationOwner105").equals(qualificationParent105))throw new IllegalStateException("Control the owning qualification request, not an individual example");}
    private JSONObject proof105(JSONObject candidate,JSONObject example)throws Exception{
        if(!Qualification105.binding(example).similar(candidate.optJSONObject("qualificationCase105")))throw new IllegalStateException("Qualification example identity changed");
        var d=candidate.getJSONObject("definition");Qualification105.inputIntact(example,measurements.read(d.getString("datasetId"),0));
        if(d.getDouble("maxGapS")!=example.getDouble("maxGapS")||d.getDouble("speedLimitMps")!=example.getDouble("speedLimitMps")||!d.getString("saveSkillName").isEmpty())throw new IllegalStateException("Qualification criteria or retention intent changed");
        return qualificationProof104(candidate);
    }
    private JSONObject start105(JSONObject input)throws Exception{
        long begin=System.nanoTime(),wall=System.currentTimeMillis();
        if(!(input.get("qualify105") instanceof Boolean b)||!b)throw new IllegalArgumentException("Automatic qualification must be explicitly enabled");
        var args=WorldFrames.copy(input);args.remove("qualify105");boolean refresh=Qualification107.take(args);boolean transition=Qualification108.take(args);if(refresh&&transition)throw new IllegalArgumentException("Choose refresh or an explicit qualification transition");refresh|=transition;boolean review=args.optBoolean("reviewQualification105");
        if(args.has("reviewQualification105")&&!(args.get("reviewQualification105") instanceof Boolean))throw new IllegalArgumentException("Qualification review must be boolean");args.remove("reviewQualification105");
        var def=definition(args);if(!DataSummary104.KIND.equals(def.optString("profile101"))||def.getString("saveSkillName").isEmpty())throw new IllegalArgumentException("Qualification requires an input-driven summary, a selected dataset and a skill name");
        if(closed||active()!=null||values().stream().filter(Qualification105::own).count()>=Qualification105.MAX_SUPERVISORS)throw new IllegalStateException("Finish the active request; qualification supervisor history limit16");
        goals.workflowCapacity();var selected=measurements.read(def.getString("datasetId"),def.getInt("datasetVersion"));var stages=new JSONArray();var baseline107=new JSONArray();int missing=0;
        var historical=values().stream().filter(w->Qualification105.own(w)&&w.getString("status").equals("completed")&&!w.getJSONObject("qualification105").getString("codeSha256").equals(Qualification105.code())).sorted(Comparator.comparing((JSONObject w)->w.getString("updatedAt")).reversed()).toList();
        boolean current=values().stream().anyMatch(w->Qualification105.own(w)&&w.getString("status").equals("completed")&&w.getJSONObject("qualification105").getString("codeSha256").equals(Qualification105.code()));
        JSONObject transition108=null;
        if(transition){if(historical.isEmpty())throw new IllegalStateException("No completed earlier qualification exists to upgrade");var original=historical.getFirst();transition108=Qualification108.manifest(original,original108(original));}
        else if(!historical.isEmpty()&&!current)throw new IllegalStateException("Qualification dependencies changed; explicitly request upgrade summary qualification before reuse");
        for(var example:Qualification105.cases()){
            if(selected.getString("sourceSha256").equals(MeasurementStore.hash(example.getJSONObject("input").getString("csv")))&&selected.getJSONObject("mapping").similar(example.getJSONObject("input").getJSONObject("mapping")))throw new IllegalArgumentException("Select a source distinct from the generated qualification examples");
            var s=new JSONObject().put("name",example.getString("name")).put("status","pending").put("case105",Qualification105.binding(example));
            var candidate=values().stream().filter(w->w.getString("status").equals("completed")&&Qualification105.binding(example).similar(w.optJSONObject("qualificationCase105"))).findFirst();
            if(candidate.isPresent()){var original=candidate.get();var proof=proof105(original,example);baseline107.put(new JSONObject().put("name",example.getString("name")).put("workflowId",original.getString("id")).put("proof",proof));if(!refresh)s.put("workflowId",original.getString("id")).put("proof",proof).put("reused",true);}
            if(!s.optBoolean("reused")){missing++;s.put("reused",false);}stages.put(s);
        }
        if(executableCount105()+missing+1>20||goals.list().length()+2*missing+3>64)throw new IllegalStateException("Insufficient original workflow/goal capacity for all examples and selected-source verification; nothing started");
        var bank=goals.procedures();int version=1;for(Object raw:bank){var p=(JSONObject)raw;if(p.getString("name").equals(def.getString("saveSkillName"))){if(!p.getString("kind").equals(DataSummary104.KIND))throw new IllegalArgumentException("Use a distinct summary skill name");version=Math.max(version,p.getInt("version")+1);}}
        if(bank.length()>=32||version>8)throw new IllegalStateException("Insufficient saved-procedure capacity; nothing started");
        stages.put(new JSONObject().put("name","selected-data").put("status","pending").put("reused",false));
        var q=new JSONObject().put("schema",Qualification105.KIND).put("codeSha256",Qualification105.code()).put("definitionSha256",WorldFrames.hash(def)).put("bankBefore",bank.length()).put("missingExamples",missing).put("reviewAfterExamples",review).put("sourceKind","synthetic software qualification; selected source retains its own provenance");
        var maintenance107=Qualification107.manifest(refresh,baseline107);q.put("maintenance107",maintenance107).put("maintenanceSha256",WorldFrames.hash(maintenance107));
        if(transition108!=null)q.put(Qualification108.KEY,transition108).put("transitionSha256",WorldFrames.hash(transition108));
        var w=new JSONObject().put("id",UUID.randomUUID().toString()).put("qualification105",q).put("definition",def).put("dataBinding",MotionData.binding(selected)).put("title","Qualify and save motion summary").put("request",def.getString("request")).put("modelName",def.getString("modelName")).put("status","running").put("step","qualification:monotonic").put("stageIndex",0).put("stages",stages).put("resumes",0).put("remainingMs105",1_200_000L).put("createdAt",Instant.ofEpochMilli(wall)).put("history",new JSONArray()).put("cost105",new JSONObject().put("admittedAtMillis",wall).put("admissionNanos",Long.toString(System.nanoTime()-begin)).put("scope","Admission/final verification use monotonic time; phase wall intervals include interruption and explicit review. External caller timing includes chat/bootstrap and polling."));
        goals.reserveWorkflow(w.getString("id"));try{change(w,"Five labelled qualification examples and selected-source intent frozen; matching proofs will be rechecked");qualificationClocks105.put(w.getString("id"),System.nanoTime());}catch(Exception e){goals.releaseWorkflow(w.getString("id"));throw e;}return view(w);
    }
    private void source105(JSONObject w){
        if(w.getJSONObject("qualification105").has(Qualification108.KEY))Qualification108.verify(w.getJSONObject("qualification105"));
        var q=w.getJSONObject("qualification105");Qualification107.verify(q);if(!q.getString("codeSha256").equals(Qualification105.code())||!q.getString("definitionSha256").equals(WorldFrames.hash(w.getJSONObject("definition"))))throw new IllegalStateException("Qualification supervisor or selected-source intent changed");
        var d=w.getJSONObject("definition");var data=measurements.read(d.getString("datasetId"),d.getInt("datasetVersion"));if(!MotionData.binding(data).similar(w.getJSONObject("dataBinding")))throw new IllegalStateException("Selected source or mapping changed during qualification");MotionData.originalRows(data);
    }
    private void charge105(JSONObject w){long now=System.nanoTime(),old=qualificationClocks105.getOrDefault(w.getString("id"),now);qualificationClocks105.put(w.getString("id"),now);w.put("remainingMs105",Math.max(0,w.getLong("remainingMs105")-Math.max(0,(now-old)/1_000_000)));if(w.getLong("remainingMs105")==0)throw new IllegalStateException("Qualification request exhausted its retained1200seconds awake budget");}
    private void checked105(JSONObject w)throws Exception{
        checked108(w);
        source105(w);for(Object item:Qualification107.verify(w.getJSONObject("qualification105"))){var b=(JSONObject)item;if(!b.getJSONObject("proof").similar(proof105(raw(b.getString("workflowId")),Qualification105.example(b.getString("name")))))throw new IllegalStateException("Original maintenance baseline proof changed");}
        for(int i=0;i<Math.min(5,w.getInt("stageIndex"));i++){var s=stage(w,i);if(!s.getString("status").equals("completed")||!s.getJSONObject("proof").similar(proof105(raw(s.getString("workflowId")),Qualification105.example(s.getString("name")))))throw new IllegalStateException("Completed qualification proof changed");}
    }
    private JSONObject oldProof108(JSONObject candidate,String name,String oldCode)throws Exception{
        var c=Qualification108.oldCase(name,oldCode);if(!Qualification108.oldBinding(name,oldCode).similar(candidate.optJSONObject("qualificationCase105")))throw new IllegalStateException("Original transition case identity changed");
        var d=candidate.getJSONObject("definition");Qualification105.inputIntact(c,measurements.read(d.getString("datasetId"),0));
        if(d.getDouble("maxGapS")!=c.getDouble("maxGapS")||d.getDouble("speedLimitMps")!=c.getDouble("speedLimitMps")||!d.getString("saveSkillName").isEmpty())throw new IllegalStateException("Original transition criteria changed");
        return qualificationProof104(candidate);
    }
    private JSONArray original108(JSONObject parent)throws Exception{
        var m=Qualification108.original(parent);var q=parent.getJSONObject("qualification105");if(q.has(Qualification108.KEY))throw new IllegalStateException("Chained runtime transitions require separate review; originals retained");
        if(!q.getString("schema").equals(Qualification105.KIND)||parent.getJSONArray("stages").length()!=6||!stage(parent,5).getString("name").equals("selected-data")||!stage(parent,5).getString("status").equals("completed"))throw new IllegalStateException("Original qualification structure changed");
        String oldCode=q.getString("codeSha256");var proofs=new JSONArray();var names=new HashSet<String>();
        for(int i=0;i<5;i++){var s=stage(parent,i);String name=s.getString("name");if(!names.add(name)||!s.getString("status").equals("completed")||!Qualification108.oldBinding(name,oldCode).similar(s.getJSONObject("case105")))throw new IllegalStateException("Original qualification stage changed");
            var proof=oldProof108(raw(s.getString("workflowId")),name,oldCode);if(!proof.similar(s.getJSONObject("proof")))throw new IllegalStateException("Original qualification proof changed");proofs.put(proof);
        }
        var baselineNames=new HashSet<String>();for(Object row:m.getJSONArray("baseline")){var b=(JSONObject)row;SpatialScene.keys(b,"name","workflowId","proof");if(!baselineNames.add(b.getString("name"))||!b.getJSONObject("proof").similar(oldProof108(raw(b.getString("workflowId")),b.getString("name"),oldCode)))throw new IllegalStateException("Original maintenance baseline proof changed");}
        var audit=parent.getJSONObject("audit");if(!audit.getString("proofsSha256").equals(WorldFrames.hash(new JSONObject().put("proofs",proofs))))throw new IllegalStateException("Original qualification audit changed");
        var selected=raw(stage(parent,5).getString("workflowId"));var checked=verified101(selected);verifyQualification104(selected);
        if(!parent.getJSONObject("definition").similar(selected.getJSONObject("definition"))||!parent.getJSONObject("dataBinding").similar(selected.getJSONObject("dataBinding"))||!selected.getString("qualificationOwner105").equals(parent.getString("id")))throw new IllegalStateException("Original selected-source handoff changed");
        if(!selected.getString("status").equals("completed")||selected.has("pendingSkill101")||!audit.getString("selectedWorkflowId").equals(selected.getString("id"))||!audit.getString("selectedAuditSha256").equals(WorldFrames.hash(checked))||!parent.getJSONObject("savedSkill").similar(selected.getJSONObject("savedSkill"))||!parent.getJSONObject("report").similar(selected.getJSONObject("report")))throw new IllegalStateException("Original selected-source publication changed");
        var ref=selected.getJSONObject("savedSkill");var saved=goals.procedures().toList().stream().map(x->new JSONObject((Map<?,?>)x)).filter(p->p.getString("id").equals(ref.getString("id"))).findFirst().orElseThrow(()->new IllegalStateException("Original published procedure missing"));
        var expected=procedure103(selected);if(!saved.optString("publication101").equals("published"))throw new IllegalStateException("Original procedure not published");for(String k:expected.keySet())if(!k.equals("publication101")&&!new JSONObject().put("v",expected.get(k)).similar(new JSONObject().put("v",saved.get(k))))throw new IllegalStateException("Original procedure contract changed");return proofs;
    }
    private void checked108(JSONObject w)throws Exception{
        var q=w.getJSONObject("qualification105");if(!q.has(Qualification108.KEY))return;Qualification108.verify(q);var t=q.getJSONObject(Qualification108.KEY);var parent=raw(t.getString("fromWorkflowId"));
        if(!WorldFrames.hash(parent).equals(t.getString("fromRecordSha256"))||!t.similar(Qualification108.manifest(parent,original108(parent))))throw new IllegalStateException("Original qualification transition record changed");
    }
    private void childIntent105(JSONObject w,JSONObject s)throws Exception{
        JSONObject in;if(s.getString("name").equals("selected-data")){checked105(w);in=WorldFrames.copy(w.getJSONObject("definition"));}
        else{var example=Qualification105.example(s.getString("name"));if(!Qualification105.binding(example).similar(s.getJSONObject("case105")))throw new IllegalStateException("Generated example identity changed");var data=Qualification105.dataset(measurements,example);in=definition(new JSONObject().put("profile101",DataSummary104.KIND).put("request","Run labelled synthetic qualification105 case "+s.getString("name")+" with all original source and rendered outcome checks").put("title",data.getString("title")).put("modelName",w.getString("modelName")).put("datasetId",data.getString("datasetId")).put("datasetVersion",1).put("maxGapS",example.getDouble("maxGapS")).put("speedLimitMps",example.getDouble("speedLimitMps")).put("saveSkillName",""));}
        s.put("input",in).put("workflowId",UUID.nameUUIDFromBytes(("qualification105:"+w.getString("id")+":"+s.getString("name")).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString());change(w,"Stable "+s.getString("name")+" source and child intent persisted before execution");
    }
    private JSONObject startChild105(JSONObject w,JSONObject s)throws Exception{
        synchronized(goals){goals.releaseWorkflow(w.getString("id"));qualificationParent105=w.getString("id");qualificationChildId105=s.getString("workflowId");qualificationCase105=s.optJSONObject("case105");
            try{return start(s.getJSONObject("input"),null,null);}catch(Exception e){goals.reserveWorkflow(w.getString("id"));throw e;}finally{qualificationParent105="";qualificationChildId105="";qualificationCase105=null;}}
    }
    private void advance105(JSONObject w,JSONObject s)throws Exception{
        s.put("status","completed").put("endedAtMillis",System.currentTimeMillis());s.put("elapsedMs",s.getLong("endedAtMillis")-s.getLong("startedAtMillis"));int next=w.getInt("stageIndex")+1;
        if(next<6){w.put("stageIndex",next).put("step",next==5?"selected-data":"qualification:"+stage(w,next).getString("name"));if(next==5&&w.getJSONObject("qualification105").getBoolean("reviewAfterExamples")){w.put("status","interrupted");qualificationClocks105.remove(w.getString("id"));}change(w,next==5?"All five example outcomes verified; selected-source report is next"+(w.getString("status").equals("interrupted")?". Requested review; explicitly Resume.":""):"Verified "+s.getString("name")+"; continuing with the next stable stage");}
        else{w.put("step","complete");change(w,"Selected-source child completed; rechecking original qualification and publication");}
    }
    private void tick105(JSONObject w){
        try{charge105(w);source105(w);if(w.getString("step").equals("complete")){complete105(w);return;}var s=stage(w,w.getInt("stageIndex"));
            if(!s.has("startedAtMillis")){s.put("startedAtMillis",System.currentTimeMillis());change(w,"Checking "+s.getString("name")+(s.optBoolean("reused")?" against its retained originals":" through the registered summary workflow"));return;}
            if(s.optBoolean("reused")){var proof=proof105(raw(s.getString("workflowId")),Qualification105.example(s.getString("name")));if(!s.getJSONObject("proof").similar(proof))throw new IllegalStateException("Previously selected qualification proof changed");advance105(w,s);return;}
            if(!s.has("input")){childIntent105(w,s);return;}var child=leaf105(w);if(child==null){startChild105(w,s);return;}
            if(child.getString("status").equals("running")){synchronized(goals){tickSingle105(child);child=raw(child.getString("id"));if(settled(child))goals.reserveWorkflow(w.getString("id"));}save(w);return;}
            if(!child.getString("status").equals("completed"))throw new IllegalStateException("Owned "+s.getString("name")+" stopped: "+child.getString("reason"));
            if(s.getString("name").equals("selected-data")){verified101(child);if(child.has("pendingSkill101"))throw new IllegalStateException("Selected-source skill publication is pending; restart to reconcile");if(!child.has("savedSkill"))throw new IllegalStateException("Selected-source child did not retain a verified procedure");}
            else s.put("proof",proof105(child,Qualification105.example(s.getString("name"))));
            goals.reserveWorkflow(w.getString("id"));advance105(w,s);
        }catch(Throwable error){try{interrupt105(w,String.valueOf(error.getMessage()));}catch(Exception failed){System.err.println("Qualification checkpoint: "+failed);}}
    }
    private void complete105(JSONObject w)throws Exception{
        long begin=System.nanoTime();checked105(w);var child=leaf105(w);if(child==null||!child.getString("status").equals("completed"))throw new IllegalStateException("Selected-source child incomplete");var audit=verified101(child);verifyQualification104(child);
        var ref=child.getJSONObject("savedSkill");var saved=goals.procedures().toList().stream().map(x->new JSONObject((Map<?,?>)x)).filter(p->p.getString("id").equals(ref.getString("id"))).findFirst().orElseThrow(()->new IllegalStateException("Saved child procedure missing"));
        if(!saved.optString("publication101").equals("published")||!saved.getString("sourceWorkflowId").equals(child.getString("id")))throw new IllegalStateException("Selected-source publication lineage changed");
        var proofs=new JSONArray();for(int i=0;i<5;i++)proofs.put(stage(w,i).getJSONObject("proof"));
        w.put("audit",new JSONObject().put("passed",true).put("schema",Qualification105.KIND).put("proofsSha256",WorldFrames.hash(new JSONObject().put("proofs",proofs))).put("selectedWorkflowId",child.getString("id")).put("selectedAuditSha256",WorldFrames.hash(audit))).put("report",child.getJSONObject("report")).put("savedSkill",ref);
        var q=Qualification105.summary(w);w.put("metrics",new JSONObject().put("verifiedExamples",5).put("createdExamples",q.getInt("createdExamples")).put("reusedExamples",q.getInt("reusedExamples")).put("bankBefore",q.getInt("bankBefore")).put("bankAfter",goals.procedures().length()).put("observationRows",child.getJSONObject("metrics").getInt("observationRows")).put("qualityPassed",child.getJSONObject("metrics").getBoolean("qualityPassed")));
        w.getJSONObject("cost105").put("finalVerificationNanos",Long.toString(System.nanoTime()-begin)).put("finishedAtMillis",System.currentTimeMillis());w.put("status","completed");change(w,"Five original examples and selected-source outcome verified; procedure published by its completed child");qualificationClocks105.remove(w.getString("id"));goals.releaseWorkflow(w.getString("id"));artifacts.event("java_qualification105_completed",view(w));
    }
    private void interrupt105(JSONObject w,String reason)throws Exception{
        var child=leaf105(w);if(child!=null&&child.getString("status").equals("running"))interrupt(child,"Qualification parent interrupted: "+reason);
        w.put("status","interrupted");qualificationClocks105.remove(w.getString("id"));change(w,reason+". Inspect originals and explicitly Resume or Stop the qualification request.");
    }
    private JSONObject resume105(JSONObject w)throws Exception{
        if(!w.getString("status").equals("interrupted")||w.getInt("resumes")>=3||w.getLong("remainingMs105")<=0)throw new IllegalStateException("Resume within the original three-resume and retained awake-time limits");checked105(w);var child=leaf105(w);
        if(child!=null&&!settled(child)){qualificationParent105=w.getString("id");try{resume(child.getString("id"));}finally{qualificationParent105="";}}else goals.reserveWorkflow(w.getString("id"));
        w.put("status","running").put("resumes",w.getInt("resumes")+1);qualificationClocks105.put(w.getString("id"),System.nanoTime());change(w,"Resume retains completed examples, the same child identities and the frozen selected source");return view(w);
    }
    private JSONObject cancel105(JSONObject w)throws Exception{
        if(settled(w))throw new IllegalStateException("Qualification already settled");var child=leaf105(w);if(child!=null&&!settled(child)){qualificationParent105=w.getString("id");try{cancel(child.getString("id"));}finally{qualificationParent105="";}}
        w.put("status","cancelled");qualificationClocks105.remove(w.getString("id"));change(w,"Stopped qualification; completed examples and any already published child remain recorded");goals.releaseWorkflow(w.getString("id"));return view(w);
    }
    private static boolean graph(JSONObject w){return w.has("graph");}
    private JSONObject startGraph(JSONObject input,JSONObject skill) throws Exception {
        SpatialScene.keys(input,"graph","worldBinding","request","title","modelName","saveSkillName","reviewEachNode");
        if(closed||active()!=null||executableCount105()>=20)throw new IllegalStateException("Finish/resume/stop the current workflow; history limit20");goals.workflowCapacity();
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
