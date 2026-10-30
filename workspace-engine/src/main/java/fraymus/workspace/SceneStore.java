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
    private String error="";
    public SceneStore(Path project) throws Exception {
        root=project.resolve("experiments");Files.createDirectories(root);
        schemas.register(new ComponentSchema("workspace.scene",1,Map.of("payload",ComponentSchema.Kind.TEXT),Set.of("payload")));
        schemas.register(new ComponentSchema("workspace.scene.object3d",1,Map.of("scene",ComponentSchema.Kind.ENTITY_REFERENCE,"payload",ComponentSchema.Kind.TEXT),Set.of("scene","payload")));
        schemas.register(new ComponentSchema("workspace.experiment",1,Map.of("scene",ComponentSchema.Kind.ENTITY_REFERENCE,"payload",ComponentSchema.Kind.TEXT),Set.of("scene","payload")));
        world=new FactoryWorld(schemas);checkpoint=new WorldCheckpointStore(root.resolve("scenes.fworld"));
        if(Files.exists(checkpoint.path()))world.restore(checkpoint.load(schemas).snapshot());
        var interrupted=new ArrayList<EntityRecord>();
        for(var entity:world.snapshot().entities())if(type(entity).equals("workspace.experiment")){var run=payload(entity);if(Set.of("running","paused").contains(run.getString("status"))){run.put("status","interrupted").put("endedAt",Instant.now());interrupted.add(record("workspace.experiment",run,run.getString("sceneId")));}}
        if(!interrupted.isEmpty())commit(interrupted);
        clock.scheduleAtFixedRate(()->{synchronized(this){if(playing)try{advance(4);}catch(Exception e){playing=false;error=e.getMessage();if(active!=null)try{finish("failed");}catch(Exception ignored){}}}},0,16_666_667,TimeUnit.NANOSECONDS);
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
    public synchronized JSONObject save(JSONObject input) throws Exception {
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
        var records=new ArrayList<EntityRecord>();records.add(record("workspace.scene",scene,null));for(Object o:definition.objects(ids)){var object=(JSONObject)o;object.put("schemaVersion",1);records.add(record("workspace.scene.object3d",object,id));}commit(records);return read(id);
    }
    private void begin(String id,int expectedVersion,double seconds) throws Exception {
        var scene=read(id);if(scene.getInt("version")!=expectedVersion)throw new ConcurrentModificationException("Experiment scene version changed");
        beginDefinition(scene,seconds);
    }
    private void beginDefinition(JSONObject scene,double seconds) throws Exception {
        RampDefinition.range(seconds,.05,10,"seconds");if(physics!=null)throw new IllegalStateException("Reset the current experiment before starting another");String id=scene.getString("id");
        active=new JSONObject().put("id",UUID.randomUUID().toString()).put("title",scene.getString("title")+" experiment").put("sceneId",id).put("sceneVersion",scene.getInt("version")).put("definition",scene).put("backend",BulletRamp.BACKEND)
            .put("dt",BulletRamp.DT).put("targetTicks",(int)Math.round(seconds/BulletRamp.DT)).put("status","paused").put("startedAt",Instant.now()).put("samples",new JSONArray());
        try{physics=new BulletRamp(RampDefinition.from(scene.getJSONObject("parameters")),scene.getJSONObject("entityIds"),root.resolve("native"));active.put("solverConfiguration",physics.configuration());sample();commit(List.of(record("workspace.experiment",active,id)));error="";}catch(Exception e){if(physics!=null)physics.close();physics=null;active=null;throw e;}
    }
    private void sample(){active.getJSONArray("samples").put(physics.sample());}
    private void advance(int ticks) throws Exception {
        if(physics==null)return;
        for(int i=0;i<ticks&&physics.tick()<active.getInt("targetTicks");i++){physics.step();if(physics.tick()%4==0)sample();}
        if(physics.tick()>=active.getInt("targetTicks"))finish("completed");
    }
    private void finish(String status) throws Exception {
        playing=false;if(physics==null||active==null)return;
        JSONObject last=physics.snapshot();active.put("status",status).put("endedAt",Instant.now()).put("final",last);
        var params=RampDefinition.from(active.getJSONObject("definition").getJSONObject("parameters"));double t=last.getDouble("timeS"),expected=.5*params.gravityMps2()*Math.sin(params.radians())*t*t;
        boolean applicable=params.friction()==0&&t>=.1&&t<=1&&expected<params.lengthM()*.7-.7;
        double measured=last.getDouble("distanceM"),tolerance=Math.max(.015,expected*.04),error=Math.abs(measured-expected);
        active.put("evaluation",new JSONObject().put("reference","frictionless incline: distance = 0.5*g*sin(angle)*t^2").put("applicable",applicable).put("expectedDistanceM",expected).put("measuredDistanceM",measured).put("absoluteErrorM",error).put("toleranceM",tolerance).put("passed",status.equals("completed")&&applicable&&error<=tolerance));
        commit(List.of(record("workspace.experiment",active,active.getString("sceneId"))));
    }
    public synchronized JSONObject snapshot(){
        if(active==null)return new JSONObject().put("status","idle").put("error",error);
        var value=new JSONObject(active.toString()).put("playing",playing).put("current",physics.snapshot());value.remove("samples");
        var samples=active.getJSONArray("samples");var tail=new JSONArray();for(int i=Math.max(0,samples.length()-180);i<samples.length();i++)tail.put(samples.get(i));return value.put("trace",tail).put("error",error);
    }
    public synchronized JSONObject control(JSONObject input) throws Exception {
        String action=input.getString("action");
        if(action.equals("reset")){if(active!=null&&Set.of("running","paused").contains(active.getString("status")))finish("stopped");if(physics!=null)physics.close();physics=null;active=null;playing=false;return snapshot();}
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
    public synchronized JSONObject replay(String id) throws Exception {
        var previous=require(id,"workspace.experiment");if(physics!=null)throw new IllegalStateException("Reset the current experiment before replay");
        beginDefinition(previous.getJSONObject("definition"),previous.getInt("targetTicks")*BulletRamp.DT);active.put("replayOf",id);advance(active.getInt("targetTicks"));return new JSONObject(active.toString());
    }
    public synchronized JSONObject runRecord(String id){return require(id,"workspace.experiment");}
    public synchronized JSONArray runs(String scene){var result=new JSONArray();for(var e:world.snapshot().entities())if(type(e).equals("workspace.experiment")){var run=payload(e);if(scene.isBlank()||scene.equals(run.getString("sceneId")))result.put(new JSONObject().put("id",run.getString("id")).put("title",run.getString("title")).put("sceneId",run.getString("sceneId")).put("sceneVersion",run.getInt("sceneVersion")).put("status",run.getString("status")).put("evaluation",run.optJSONObject("evaluation")));}return result;}
    @Override public synchronized void close(){clock.shutdownNow();if(physics!=null)physics.close();physics=null;}
}
