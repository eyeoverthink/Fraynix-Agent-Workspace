package fraymus.workspace;

import fraymus.factory.*;
import org.json.*;
import javax.imageio.ImageIO;
import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** Java-owned observers, bounded actions, immutable source-linked frames and replay. */
public final class ObserverStore {
    static final String CONTRACT="native-box-observer-v1";
    private final SchemaRegistry schemas=new SchemaRegistry();
    private final FactoryWorld world;
    private final WorldCheckpointStore checkpoint;
    private final Path frames;
    private final SceneStore scenes;
    private final Clock clock;
    private final LinkedHashMap<String,JSONObject> leases=new LinkedHashMap<>();
    public ObserverStore(Path project,SceneStore scenes) throws Exception {this(project,scenes,Clock.systemUTC());}
    ObserverStore(Path project,SceneStore scenes,Clock clock) throws Exception {
        this.scenes=scenes;this.clock=clock;frames=project.resolve("observers/frames");Files.createDirectories(frames);
        schemas.register(new ComponentSchema("workspace.observer",1,Map.of("payload",ComponentSchema.Kind.TEXT),Set.of("payload")));
        for(String name:List.of("event","capture"))schemas.register(new ComponentSchema("workspace.observer."+name,1,Map.of("observer",ComponentSchema.Kind.ENTITY_REFERENCE,"payload",ComponentSchema.Kind.TEXT),Set.of("observer","payload")));
        world=new FactoryWorld(schemas);checkpoint=new WorldCheckpointStore(project.resolve("observers/state.fworld"));
        if(Files.exists(checkpoint.path()))world.restore(checkpoint.load(schemas).snapshot());
    }
    private static JSONObject copy(JSONObject value){return new JSONObject(value.toString());}
    private static String hashBytes(byte[] bytes){try{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));}catch(Exception error){throw new IllegalStateException(error);}}
    private static String type(EntityRecord e){return e.components().get(0).type();}
    private static JSONObject payload(EntityRecord e){return new JSONObject(e.components().get(0).values().get("payload"));}
    private EntityRecord find(String id){var uuid=UUID.fromString(id);return world.snapshot().entities().stream().filter(e->e.id().equals(uuid)).findFirst().orElse(null);}
    private JSONObject require(String id,String kind){var entity=find(id);if(entity==null||!type(entity).equals("workspace.observer"+kind))throw new IllegalArgumentException("Unknown observer record");return payload(entity);}
    private EntityRecord record(String kind,JSONObject value){
        var previous=find(value.getString("id"));var fields=new HashMap<String,String>();fields.put("payload",value.toString());if(!kind.isEmpty())fields.put("observer",value.getString("observerId"));
        return new EntityRecord(UUID.fromString(value.getString("id")),value.optString("title","Observer "+kind),List.of(new ComponentRecord(previous==null?UUID.randomUUID():previous.components().get(0).id(),"workspace.observer"+kind,1,fields)));
    }
    private void commit(EntityRecord...records) throws Exception {
        var before=world.snapshot();var tx=new WorldTransaction(before);
        for(var record:records)if(find(record.id().toString())==null)tx.add(new WorldTransaction.Create(record));else tx.add(new WorldTransaction.Put(record.id(),record.components().get(0)));
        try{tx.commit(world,schemas);checkpoint.save(world.snapshot(),schemas);}catch(Exception error){world.restore(before);throw error;}
    }
    private static void fields(JSONObject input,String...allowed){if(!Set.of(allowed).containsAll(input.keySet()))throw new IllegalArgumentException("Unsupported observer field");}
    private static double number(JSONObject input,String key,double fallback){
        if(!input.has(key))return fallback;Object raw=input.get(key);if(!(raw instanceof Number n)||!Double.isFinite(n.doubleValue()))throw new IllegalArgumentException(key+" must be finite numeric data");return n.doubleValue();
    }
    private static int integer(JSONObject input,String key,int fallback,int minimum,int maximum){
        double value=number(input,key,fallback);if(value!=Math.rint(value)||value<minimum||value>maximum)throw new IllegalArgumentException(key+" is outside its integer range");return (int)value;
    }
    private static void pose(JSONObject pose){
        var p=ObserverGeometry.v(pose.getJSONArray("position"));
        RampDefinition.range(p[0],-100,100,"camera x");RampDefinition.range(p[1],.05,50,"camera y");RampDefinition.range(p[2],-100,100,"camera z");
        RampDefinition.range(pose.getDouble("yawDeg"),-180,180,"yaw");RampDefinition.range(pose.getDouble("pitchDeg"),-85,85,"pitch");RampDefinition.range(pose.getDouble("fovDeg"),30,110,"field of view");
    }
    public synchronized JSONObject open(JSONObject input) throws Exception {
        fields(input,"sceneId","id","newObserver");String sceneId=input.getString("sceneId");var source=scenes.observerWorld(sceneId);
        if(input.has("id")){var observer=read(input.getString("id"));if(!observer.getString("sceneId").equals(sceneId))throw new IllegalArgumentException("Observer belongs to another scene");return observer;}
        if(input.has("newObserver")&&!(input.get("newObserver") instanceof Boolean))throw new IllegalArgumentException("newObserver must be boolean");
        if(!input.optBoolean("newObserver")){var all=list();for(int i=all.length()-1;i>=0;i--){var observer=all.getJSONObject(i);if(observer.getString("sceneId").equals(sceneId))return read(observer.getString("id"));}}
        if(list().length()>=32)throw new IllegalStateException("Observer limit reached (32)");
        JSONObject block=null;for(Object value:source.getJSONArray("objects"))if(((JSONObject)value).getString("label").equals("Block"))block=(JSONObject)value;
        if(block==null)throw new IllegalStateException("Scene has no initial focus object");var target=ObserverGeometry.v(block.getJSONArray("position"));
        var pose=new JSONObject().put("position",new JSONArray(List.of(target[0],target[1]+1.2,target[2]+5))).put("yawDeg",0).put("pitchDeg",-Math.toDegrees(Math.atan2(1.2,5))).put("fovDeg",60);
        pose(pose);
        var observer=new JSONObject().put("id",UUID.randomUUID().toString()).put("schemaVersion",1).put("sceneId",sceneId).put("title","Observer · "+scenes.read(sceneId).getString("title"))
            .put("version",1).put("pose",pose).put("selectedEntityId",JSONObject.NULL).put("actionCount",0).put("captureCount",0).put("createdAt",clock.instant()).put("motion","free camera; no avatar body or collisions");
        commit(record("",observer));return copy(observer);
    }
    public synchronized JSONObject read(String id){return require(id,"");}
    public synchronized JSONObject body(JSONObject input) throws Exception {
        var observer=read(input.getString("id"));
        synchronized(scenes){
            var prior=scenes.bodyPrior(input);if(prior!=null)return prior;
            if(input.getString("action").equals("cancel"))return scenes.bodyAction(input,null);
            var packet=lease(input.getString("observationId"),true);
            if(!packet.getJSONObject("observer").getString("id").equals(observer.getString("id")))throw new IllegalArgumentException("Body observation belongs to another observer");
            if(input.getString("action").equals("grab")){
                JSONObject target=null;for(Object value:packet.getJSONArray("visibleEntities"))if(((JSONObject)value).getString("id").equals(input.getString("entityId")))target=(JSONObject)value;
                if(target==null)throw new IllegalArgumentException("Grab requires a visible native entity");
                if(ObserverGeometry.norm(ObserverGeometry.sub(ObserverGeometry.v(target.getJSONArray("position")),ObserverGeometry.v(observer.getJSONObject("pose").getJSONArray("position"))))>6)throw new IllegalArgumentException("Entity exceeds the 6 m virtual actuator reach");
            }
            return scenes.bodyAction(input,packet);
        }
    }
    public synchronized JSONArray list(){var out=new ArrayList<JSONObject>();for(var entity:world.snapshot().entities())if(type(entity).equals("workspace.observer"))out.add(payload(entity));out.sort(Comparator.comparing(o->Instant.parse(o.getString("createdAt"))));return new JSONArray(out);}
    private JSONObject lease(String id,boolean requireCurrentWorld){
        var packet=leases.get(id);if(packet==null||clock.millis()>packet.getLong("expiresAtEpochMs"))throw new IllegalStateException("Observer observation expired; observe again");
        var observer=read(packet.getJSONObject("observer").getString("id"));
        if(observer.getInt("version")!=packet.getJSONObject("observer").getInt("version"))throw new ConcurrentModificationException("Observer pose changed; observe again");
        var current=scenes.observerWorld(observer.getString("sceneId"));var original=packet.getJSONObject("world");
        if(current.getInt("authoredVersion")!=original.getInt("authoredVersion"))throw new ConcurrentModificationException("Scene changed; observe again");
        if(requireCurrentWorld&&!MeasurementStore.hash(current.toString()).equals(packet.getString("worldSha256")))throw new ConcurrentModificationException("Physical state advanced; pause or observe again before object inspection");
        return copy(packet);
    }
    public synchronized JSONObject observe(JSONObject input){
        fields(input,"id","width","height");var observer=read(input.getString("id"));var source=scenes.observerWorld(observer.getString("sceneId"));
        var viewport=new JSONObject().put("width",integer(input,"width",960,320,1280)).put("height",integer(input,"height",540,240,960));
        var packet=new JSONObject().put("id",UUID.randomUUID().toString()).put("contract",CONTRACT).put("origin","native-simulation").put("observer",observer).put("world",source).put("worldSha256",MeasurementStore.hash(source.toString()))
            .put("viewport",viewport).put("visibleEntities",ObserverGeometry.visible(observer.getJSONObject("pose"),viewport,source.getJSONArray("objects")))
            .put("visibilityMethod","Sampled oriented-box frustum/ray occlusion, not image recognition; thin geometry can be missed")
            .put("createdAt",clock.instant()).put("expiresAtEpochMs",clock.millis()+120000).put("readOnly",false);
        leases.put(packet.getString("id"),copy(packet));while(leases.size()>128)leases.remove(leases.keySet().iterator().next());return packet;
    }
    public synchronized JSONObject action(JSONObject input) throws Exception {
        fields(input,"id","observationId","operationId","action","forward","right","up","yawDeg","pitchDeg","entityId","fovDeg");
        String id=input.getString("id"),operation=input.getString("operationId");UUID.fromString(operation);var prior=find(operation);String requestHash=MeasurementStore.hash(input.toString());
        if(prior!=null){
            var receipt=require(operation,".event");if(!receipt.getString("observerId").equals(id)||!receipt.getString("requestSha256").equals(requestHash))throw new IllegalArgumentException("Operation id already used with different input");
            return new JSONObject().put("observer",read(id)).put("receipt",receipt).put("duplicate",true);
        }
        synchronized(scenes){
        String action=input.getString("action");if(!Set.of("move","look","focus","inspect","fov").contains(action))throw new IllegalArgumentException("Unsupported observer action");
        var packet=lease(input.getString("observationId"),Set.of("focus","inspect").contains(action));var observer=read(id);
        if(!packet.getJSONObject("observer").getString("id").equals(id))throw new IllegalArgumentException("Observation belongs to another observer");
        if(observer.getInt("actionCount")>=500)throw new IllegalStateException("This observer reached its 500-action history budget; create a new observer");
        var before=copy(observer.getJSONObject("pose"));var after=copy(before);JSONObject inspected=null;
        Set<String> accepted=switch(action){case "move"->Set.of("forward","right","up");case "look"->Set.of("yawDeg","pitchDeg");case "focus","inspect"->Set.of("entityId");case "fov"->Set.of("fovDeg");default->Set.of();};
        for(String field:List.of("forward","right","up","yawDeg","pitchDeg","entityId","fovDeg"))if(input.has(field)&&!accepted.contains(field))throw new IllegalArgumentException("Field "+field+" does not belong to "+action);
        if(action.equals("move")){
            double f=number(input,"forward",0),r=number(input,"right",0),u=number(input,"up",0);for(double n:new double[]{f,r,u})RampDefinition.range(n,-5,5,"camera movement");
            if(f==0&&r==0&&u==0)throw new IllegalArgumentException("Supply a nonzero movement");
            if(Math.sqrt(f*f+r*r+u*u)>5)throw new IllegalArgumentException("Movement length exceeds 5 metres");
            double yaw=Math.toRadians(before.getDouble("yawDeg"));var position=ObserverGeometry.v(before.getJSONArray("position"));
            position[0]+=f*Math.sin(yaw)+r*Math.cos(yaw);position[1]+=u;position[2]+=-f*Math.cos(yaw)+r*Math.sin(yaw);after.put("position",new JSONArray(position));
        }else if(action.equals("look")){
            double y=number(input,"yawDeg",0),p=number(input,"pitchDeg",0);RampDefinition.range(y,-90,90,"yaw change");RampDefinition.range(p,-45,45,"pitch change");
            if(y==0&&p==0)throw new IllegalArgumentException("Supply a nonzero look change");
            after.put("yawDeg",((before.getDouble("yawDeg")+y+540)%360)-180).put("pitchDeg",before.getDouble("pitchDeg")+p);
        }else if(action.equals("fov"))after.put("fovDeg",number(input,"fovDeg",Double.NaN));
        else{
            String targetId=input.getString("entityId");JSONObject target=null;
            var objects=action.equals("inspect")?packet.getJSONArray("visibleEntities"):packet.getJSONObject("world").getJSONArray("objects");
            for(Object value:objects)if(((JSONObject)value).getString("id").equals(targetId))target=(JSONObject)value;
            if(target==null)throw new IllegalArgumentException(action.equals("inspect")?"Entity is not visible in this observation":"Unknown scene entity");
            if(action.equals("focus")){
                var delta=ObserverGeometry.sub(ObserverGeometry.v(target.getJSONArray("position")),ObserverGeometry.v(before.getJSONArray("position")));
                if(ObserverGeometry.norm(delta)<.05)throw new IllegalArgumentException("Camera is too close to the focus point");
                after.put("yawDeg",Math.toDegrees(Math.atan2(delta[0],-delta[2]))).put("pitchDeg",Math.toDegrees(Math.atan2(delta[1],Math.hypot(delta[0],delta[2]))));
            }else inspected=copy(target);
            observer.put("selectedEntityId",targetId);
        }
        pose(after);observer.put("pose",after).put("version",observer.getInt("version")+1).put("actionCount",observer.getInt("actionCount")+1);
        var receipt=new JSONObject().put("id",operation).put("observerId",id).put("action",action).put("requestSha256",requestHash).put("before",before).put("after",after)
            .put("fromVersion",packet.getJSONObject("observer").getInt("version")).put("toVersion",observer.getInt("version")).put("observationId",packet.getString("id"))
            .put("worldSha256",packet.getString("worldSha256")).put("source",packet.getJSONObject("world")).put("createdAt",clock.instant());
        if(inspected!=null)receipt.put("inspected",inspected);
        commit(record("",observer),record(".event",receipt));return new JSONObject().put("observer",observer).put("receipt",receipt).put("duplicate",false);
        }
    }
    public synchronized JSONObject capture(JSONObject input) throws Exception {
        fields(input,"observationId","pngBase64","worldSha256","operationId");
        String operation=input.getString("operationId");UUID.fromString(operation);
        if(find(operation)!=null){var old=require(operation,".capture");if(!old.getString("requestSha256").equals(MeasurementStore.hash(input.toString())))throw new IllegalArgumentException("Capture operation id reused");return old;}
        var packet=lease(input.getString("observationId"),false);if(!packet.getString("worldSha256").equals(input.getString("worldSha256")))throw new IllegalArgumentException("Rendered source differs");
        byte[] bytes=Base64.getDecoder().decode(input.getString("pngBase64"));
        if(bytes.length<32||bytes.length>1_200_000||!Arrays.equals(Arrays.copyOf(bytes,8),new byte[]{(byte)137,80,78,71,13,10,26,10}))throw new IllegalArgumentException("Invalid or oversized native PNG");
        var viewport=packet.getJSONObject("viewport");var header=ByteBuffer.wrap(bytes);
        if(header.getInt(16)!=viewport.getInt("width")||header.getInt(20)!=viewport.getInt("height"))throw new IllegalArgumentException("PNG dimensions differ from the frozen viewport");
        var decoded=ImageIO.read(new ByteArrayInputStream(bytes));if(decoded==null)throw new IllegalArgumentException("PNG cannot be decoded");
        var observer=read(packet.getJSONObject("observer").getString("id"));if(observer.getInt("captureCount")>=100)throw new IllegalStateException("Observer frame budget reached (100)");
        String hash=hashBytes(bytes);Path image=frames.resolve(hash+".png");if(Files.exists(image)&&!hashBytes(Files.readAllBytes(image)).equals(hash))throw new IOException("Existing frame integrity mismatch");
        if(!Files.exists(image)){var temporary=Files.createTempFile(frames,"frame-",".tmp");Files.write(temporary,bytes);try{Files.move(temporary,image,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(temporary,image);}}
        var capture=new JSONObject().put("id",operation).put("observerId",observer.getString("id")).put("observationId",packet.getString("id")).put("observerVersion",observer.getInt("version")).put("requestSha256",MeasurementStore.hash(input.toString())).put("pngSha256",hash).put("bytes",bytes.length).put("packet",packet).put("createdAt",clock.instant());
        observer.put("captureCount",observer.getInt("captureCount")+1);commit(record("",observer),record(".capture",capture));return capture;
    }
    public synchronized JSONObject replay(String id) throws IOException {
        var capture=require(id,".capture");var bytes=Files.readAllBytes(frames.resolve(capture.getString("pngSha256")+".png"));
        if(!hashBytes(bytes).equals(capture.getString("pngSha256")))throw new IOException("Captured frame integrity mismatch");
        var packet=copy(capture.getJSONObject("packet"));packet.put("readOnly",true).remove("expiresAtEpochMs");
        return new JSONObject().put("capture",capture).put("packet",packet).put("pngBase64",Base64.getEncoder().encodeToString(bytes));
    }
    public synchronized JSONObject history(String id){
        read(id);var events=new ArrayList<JSONObject>();var captures=new ArrayList<JSONObject>();
        for(var entity:world.snapshot().entities())if(!type(entity).equals("workspace.observer")){
            var value=payload(entity);if(!value.getString("observerId").equals(id))continue;
            if(type(entity).endsWith(".capture")){value.remove("packet");captures.add(value);}else events.add(value);
        }
        var order=Comparator.comparing((JSONObject value)->value.getString("createdAt")).thenComparing(value->value.getString("id"));events.sort(order);captures.sort(order);
        return new JSONObject().put("events",new JSONArray(events)).put("captures",new JSONArray(captures));
    }
}
