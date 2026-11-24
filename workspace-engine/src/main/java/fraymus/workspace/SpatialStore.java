package fraymus.workspace;

import org.json.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Source-qualified imports and bounded native manipulation receipts, isolated from the ramp lab. */
public final class SpatialStore implements AutoCloseable {
    private final SpatialCarryStore carries;
    final HeightStudyStore heightStudies;
    private final Path file,nativeDir;private final LinkedHashMap<String,JSONObject> scenes=new LinkedHashMap<>(),runs=new LinkedHashMap<>();
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"spatial-native-owner");t.setDaemon(true);return t;});
    private final AtomicBoolean cancelled=new AtomicBoolean();private String active="";private JSONObject current;private volatile boolean closed;
    public SpatialStore(Path project) throws Exception {carries=new SpatialCarryStore(project);heightStudies=new HeightStudyStore(project);file=project.resolve("spatial/state.json");nativeDir=project.resolve("native");if(Files.exists(file)){var data=new JSONObject(Files.readString(file));for(Object raw:data.getJSONArray("scenes")){var s=(JSONObject)raw;validateDefinition(s);scenes.put(s.getString("id"),s);}for(Object raw:data.getJSONArray("runs")){var r=(JSONObject)raw;if(Set.of("executing","prepared").contains(r.getString("status")))r.put("status","interrupted").put("reason","Process ended; resume creates a separate native attempt from the immutable import");runs.put(r.getString("id"),r);}persist();}}
    private static JSONObject copy(JSONObject x){return new JSONObject(x.toString());}
    private Path glbFile(String id) {
        if (!UUID.fromString(id).toString().equals(id)) throw new IllegalArgumentException("Invalid spatial import identity.");
        return file.resolveSibling("glb-sources").resolve(id+".glb");
    }
    private static void validateDefinition(JSONObject scene) {
        var parsed=SpatialScene.parse(scene.getString("sourceText"),scene.getString("id"));
        for (String key:parsed.keySet()) if (!new JSONObject().put("v",parsed.get(key)).similar(new JSONObject().put("v",scene.get(key))))
            throw new IllegalStateException("Spatial source/definition integrity failed: "+key);
    }
    private void validate(JSONObject scene) {
        validateDefinition(scene);
        if (scene.has("heightStudy")) {
            try {
                var original=heightStudies.boundDefinition(scene.getJSONObject("heightStudy"));
                if (!original.similar(new JSONObject(scene.getString("sourceText"))))
                    throw new IllegalStateException("The measured collider definition changed.");
            } catch (Exception error) {
                throw new IllegalStateException("Cannot recheck the original height study.",error);
            }
        }
        if (scene.has("propCopy")) {
            var receipt=scene.getJSONObject("propCopy");
            var review=receipt.getJSONObject("review");
            var parent=scenes.get(review.getJSONObject("source").getString("sceneId"));
            if (parent==null || !SpatialPropCopy.binding(parent).similar(review.getJSONObject("source")))
                throw new IllegalStateException("The original prop scene changed.");
            if (!WorldFrames.hash(receipt).equals(scene.getString("propCopySha256"))
                    || !WorldFrames.hash(review).equals(receipt.getString("approvalSha256"))
                    || !receipt.getJSONObject("verify").similar(receipt.getJSONObject("replay")))
                throw new IllegalStateException("The reviewed prop copy contact receipt changed.");
            String expected=WorldFrames.hash(SpatialPropCopy.definition(parent,review));
            if (!expected.equals(WorldFrames.hash(new JSONObject(scene.getString("sourceText")))))
                throw new IllegalStateException("The reviewed prop copy geometry changed.");
            // A copy can be used as a source for another copy. Each link must precede it.
            if (!Instant.parse(parent.getString("importedAt")).isBefore(Instant.parse(scene.getString("importedAt"))))
                throw new IllegalStateException("Invalid prop copy ancestry.");
            validate(parent);
        }
        if (scene.has("glbSha256")) {
            try {
                Path original=glbFile(scene.getString("id"));
                if (!Files.isRegularFile(original,LinkOption.NOFOLLOW_LINKS) || Files.size(original)>SpatialGlb.LIMIT)
                    throw new IllegalStateException("The original GLB source is unavailable.");
                byte[] bytes=Files.readAllBytes(original);
                if (!MapDefinition.sha(bytes).equals(scene.getString("glbSha256"))
                        || !SpatialGlb.importScene(bytes).similar(new JSONObject(scene.getString("sourceText"))))
                    throw new IllegalStateException("The original GLB source changed.");
            } catch (Exception problem) {
                throw new IllegalStateException("Cannot recheck the original GLB source.",problem);
            }
        }
    }
    private synchronized void persist() throws Exception {Files.createDirectories(file.getParent());var temp=file.resolveSibling("state.pending.json");Files.writeString(temp,new JSONObject().put("scenes",new JSONArray(scenes.values())).put("runs",new JSONArray(runs.values())).toString(),StandardCharsets.UTF_8);try{Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}}
    public synchronized JSONObject importScene(String text) throws Exception {
        available();
        if (scenes.size()>=32) throw new IllegalStateException("Spatial import history limit reached (32)");
        return retainScene(SpatialScene.parse(text,UUID.randomUUID().toString()).put("importedAt",Instant.now().toString()));
    }
    public synchronized JSONObject importGlb(byte[] bytes) throws Exception {
        available();
        if (scenes.size()>=32) throw new IllegalStateException("Spatial import history limit reached (32)");
        String sha=MapDefinition.sha(bytes);
        String id=UUID.randomUUID().toString();
        var scene=SpatialScene.parse(SpatialGlb.importScene(bytes).toString(),id)
                .put("importedAt",Instant.now().toString()).put("glbSha256",sha);
        Path source=glbFile(id);
        Files.createDirectories(source.getParent());
        Files.write(source,bytes,StandardOpenOption.CREATE_NEW);
        try { return retainScene(scene); }
        catch (Exception error) {
            try { Files.deleteIfExists(source); }
            catch (Exception cleanup) { error.addSuppressed(cleanup); }
            throw error;
        }
    }
    synchronized JSONObject reviewCarry(JSONObject input) throws Exception {
        available();
        return carries.review(read(input.getString("sceneId")),input);
    }

    synchronized JSONObject readCarry(JSONObject input) throws Exception {
        SpatialScene.keys(input,"sceneId","id");
        return carries.read(read(input.getString("sceneId")),input.getString("id"));
    }

    synchronized JSONObject latestCarry() throws Exception {
        var reference=carries.latestReference();
        return reference.isEmpty()?reference:readCarry(reference);
    }

    synchronized JSONObject approveCarry(JSONObject input) throws Exception {
        available();
        return carries.approve(read(input.getString("sceneId")),input);
    }

    synchronized JSONObject reviewProp(JSONObject args) {
        available();
        var review=SpatialPropCopy.review(read(args.getString("sceneId")),args);
        return new JSONObject().put("review",review).put("approvalSha256",WorldFrames.hash(review));
    }
    synchronized JSONObject copyProp(JSONObject input) throws Exception {
        SpatialScene.keys(input,"args","approvalSha256");
        var args=input.getJSONObject("args");
        var parent=read(args.getString("sceneId"));
        var review=SpatialPropCopy.review(parent,args);
        String digest=WorldFrames.hash(review);
        if (!digest.equals(input.getString("approvalSha256")))
            throw new IllegalStateException("Approve the exact selected prop, source and transform.");
        String id=UUID.nameUUIDFromBytes(("spatial-prop-copy-v1:"+digest).getBytes(StandardCharsets.UTF_8)).toString();
        if (scenes.containsKey(id)) return read(id);
        available();
        if (scenes.size()>=32) throw new IllegalStateException("Spatial import history limit reached (32)");
        var scene=SpatialScene.parse(SpatialPropCopy.definition(parent,review).toString(),id);
        var checked=SpatialPropCopy.verifyNative(scene,review,nativeDir);
        var replay=SpatialPropCopy.verifyNative(scene,review,nativeDir);
        if (!checked.similar(replay)) throw new IllegalStateException("The independent native collision replay differs.");
        var receipt=new JSONObject().put("review",review).put("approvalSha256",digest).put("verify",checked).put("replay",replay);
        return retainScene(scene.put("importedAt",Instant.now().toString()).put("propCopy",receipt)
                .put("propCopySha256",WorldFrames.hash(receipt)));
    }

    synchronized JSONObject importHeightCollider(JSONObject approved) throws Exception {
        var definition=approved.getJSONObject("collider");
        String key=approved.getString("colliderApprovalSha256");
        String id=UUID.nameUUIDFromBytes(("height-collider:"+key).getBytes(StandardCharsets.UTF_8)).toString();
        if (scenes.containsKey(id)) {
            var existing=read(id);
            if (!new JSONObject(existing.getString("sourceText")).similar(definition))
                throw new IllegalStateException("Previously imported collider definition changed.");
            return existing;
        }
        available();
        if (scenes.size()>=32) throw new IllegalStateException("Spatial import history limit reached (32)");
        var source=new JSONObject().put("id",approved.getString("id")).put("colliderApprovalSha256",key)
                .put("evidenceSha256",approved.getString("evidenceSha256"));
        return retainScene(SpatialScene.parse(definition.toString(),id).put("importedAt",Instant.now().toString())
                .put("heightStudy",source));
    }

    private JSONObject retainScene(JSONObject scene) throws Exception {
        String id=scene.getString("id");
        scenes.put(id,scene);
        try { persist(); }
        catch (Exception error) { scenes.remove(id);throw error; }
        current=new JSONObject().put("sceneId",id).put("status","authored")
                .put("current",new JSONObject().put("tick",0).put("objects",scene.getJSONArray("objects")).put("gripping",false));
        return copy(scene);
    }
    public synchronized JSONArray list(){var out=new JSONArray();for(var s:scenes.values())out.put(new JSONObject().put("id",s.getString("id")).put("version",1).put("title",s.getString("title")).put("source",s.getJSONObject("source")).put("sourceSha256",s.getString("sourceSha256")));return out;}
    public synchronized JSONObject read(String id){UUID.fromString(id);var s=scenes.get(id);if(s==null)throw new IllegalArgumentException("Unknown spatial scene");validate(s);return copy(s);}
    public synchronized JSONObject snapshot(){return current==null?new JSONObject().put("status","idle"):copy(current);}
    public synchronized JSONObject stateForScene(String id){var s=read(id);if(current!=null&&current.optString("sceneId").equals(id))return copy(current);JSONObject last=null;for(var r:runs.values())if(r.getString("sceneId").equals(id)&&r.getString("status").equals("completed"))last=r;if(last!=null)return new JSONObject().put("id",last.getString("id")).put("sceneId",id).put("status","completed").put("current",last.getJSONObject("actual").getJSONObject("final")).put("verification",last.getJSONObject("verification"));return new JSONObject().put("sceneId",id).put("status","authored").put("current",new JSONObject().put("tick",0).put("objects",s.getJSONArray("objects")).put("gripping",false));}
    public synchronized void available(){if(closed||!active.isEmpty())throw new IllegalStateException("Wait for or cancel the owned spatial manipulation");}
    public synchronized JSONObject run(String id){UUID.fromString(id);var r=runs.get(id);if(r==null)throw new IllegalArgumentException("Unknown spatial run");return copy(r);}
    public synchronized boolean hasRun(String id){return runs.containsKey(id);}
    public synchronized JSONArray runs(){var out=new JSONArray();for(var r:runs.values())out.put(new JSONObject().put("id",r.getString("id")).put("sceneId",r.getString("sceneId")).put("status",r.getString("status")).put("createdAt",r.getString("createdAt")));return out;}
    public static JSONObject binding(JSONObject scene){return new JSONObject().put("sceneId",scene.getString("id")).put("version",1).put("sourceSha256",scene.getString("sourceSha256")).put("definitionSha256",MeasurementStore.hash(scene.toString()));}
    static JSONObject intent(JSONObject scene,String objectId,JSONArray displacement){var object=SpatialScene.object(scene,objectId);if(object.getDouble("massKg")<=0)throw new IllegalArgumentException("Select a dynamic object");var delta=SpatialScene.vector(displacement,6);if(ObserverGeometry.norm(delta)<.1||ObserverGeometry.norm(delta)>6||Math.abs(delta[1])>1e-8)throw new IllegalArgumentException("This procedure supports a 0.1..6 m horizontal displacement, ending at the source height");var target=ObserverGeometry.add(SpatialScene.vector(object.getJSONArray("position"),20),delta);for(double v:target)RampDefinition.range(v,-12,12,"target");return new JSONObject().put("binding",binding(scene)).put("objectId",objectId).put("sourceId",object.getString("sourceId")).put("displacementM",displacement).put("targetM",SpatialScene.array(target));}
    /** Four finite routes evaluated by native physics. No displayed world exists in these forks. */
    public JSONObject preview(JSONObject scene,String objectId,JSONArray displacement) throws Exception {
        var intent=intent(scene,objectId,displacement);var object=SpatialScene.object(scene,objectId);double[] start=SpatialScene.vector(object.getJSONArray("position"),20),target=SpatialScene.vector(intent.getJSONArray("targetM"),15),size=SpatialScene.vector(object.getJSONArray("size"),40);double radius=ObserverGeometry.norm(size)/2,top=Math.max(start[1],target[1]),left=Math.min(start[2],target[2]),right=Math.max(start[2],target[2]);
        for(Object raw:scene.getJSONArray("objects")){var o=(JSONObject)raw;if(o.getDouble("massKg")>0)continue;double[] p=SpatialScene.vector(o.getJSONArray("position"),20),s=SpatialScene.vector(o.getJSONArray("size"),40);double r=ObserverGeometry.norm(s)/2;double upper=p[1]+s[1]/2;if(upper<=Math.min(start[1],target[1])-.05)continue;top=Math.max(top,p[1]+r);left=Math.min(left,p[2]-r);right=Math.max(right,p[2]+r);}
        top+=radius+.25;left-=radius+.3;right+=radius+.3;
        var candidates=new LinkedHashMap<String,JSONArray>();candidates.put("direct",new JSONArray().put(SpatialScene.array(target)));
        if(top<10)candidates.put("over",new JSONArray().put(new JSONArray(new double[]{start[0],top,start[2]})).put(new JSONArray(new double[]{target[0],top,target[2]})).put(SpatialScene.array(target)));
        for(var side:List.of(Map.entry("left",left),Map.entry("right",right)))if(Math.abs(side.getValue())<10)candidates.put(side.getKey(),new JSONArray().put(new JSONArray(new double[]{start[0],start[1]+.15,side.getValue()})).put(new JSONArray(new double[]{target[0],target[1]+.15,side.getValue()})).put(SpatialScene.array(target)));
        var trials=new JSONArray();JSONObject best=null;double bestCost=Double.POSITIVE_INFINITY;
        for(var candidate:candidates.entrySet()){double cost=0;double[] prior=start;for(Object raw:candidate.getValue()){var p=SpatialScene.vector((JSONArray)raw,15);cost+=ObserverGeometry.norm(ObserverGeometry.sub(p,prior));prior=p;}if(cost>16){trials.put(new JSONObject().put("route",candidate.getKey()).put("passed",false).put("reason","Path exceeds 16 m budget"));continue;}
            try(var world=new SpatialPhysics(scene,nativeDir)){var result=world.execute(objectId,candidate.getValue(),()->closed||Thread.currentThread().isInterrupted(),s->{},false);trials.put(new JSONObject().put("route",candidate.getKey()).put("pathM",cost).put("passed",result.getJSONObject("metrics").getBoolean("passed")).put("metrics",result.getJSONObject("metrics")));if(result.getJSONObject("metrics").getBoolean("passed")&&cost<bestCost){bestCost=cost;best=new JSONObject().put("route",candidate.getKey()).put("waypoints",candidate.getValue()).put("forecast",result);}}
        }
        if(best==null)throw new IllegalStateException("No bounded native forecast reaches and releases at the target; no actual actuation occurred. Trials: "+trials);
        return best.put("intent",intent).put("trials",trials).put("modelKind","native rigid-body forward simulation; declared parameters; not a learned universal world model").put("previewOnly",true);
    }
    public synchronized JSONObject start(String runId,String goalId,int attempt,JSONObject scene,JSONObject plan) throws Exception {
        UUID.fromString(runId);var prior=runs.get(runId);if(prior!=null){if(!prior.optString("goalId").equals(goalId)||prior.optInt("attempt")!=attempt||!prior.getJSONObject("plan").similar(plan))throw new IllegalStateException("Foreign spatial effect identity");return copy(prior);}available();if(runs.size()>=32)throw new IllegalStateException("Spatial run history limit reached (32)");validate(scene);if(!binding(scene).similar(plan.getJSONObject("intent").getJSONObject("binding")))throw new IllegalStateException("Forecast source changed");
        var r=new JSONObject().put("id",runId).put("goalId",goalId).put("attempt",attempt).put("sceneId",scene.getString("id")).put("definition",scene).put("plan",plan).put("createdAt",Instant.now().toString()).put("status","prepared");runs.put(runId,r);persist();active=runId;cancelled.set(false);current=new JSONObject().put("id",runId).put("sceneId",r.getString("sceneId")).put("status","prepared").put("current",new JSONObject().put("tick",0).put("objects",scene.getJSONArray("objects")).put("gripping",false));
        worker.submit(()->execute(runId,scene,plan));return copy(r);
    }
    private void execute(String id,JSONObject scene,JSONObject plan){try{
        synchronized(this){runs.get(id).put("status","executing");persist();}
        JSONObject actual;try(var world=new SpatialPhysics(scene,nativeDir)){actual=world.execute(plan.getJSONObject("intent").getString("objectId"),plan.getJSONArray("waypoints"),cancelled::get,s->{synchronized(this){current=new JSONObject().put("id",id).put("sceneId",scene.getString("id")).put("status","executing").put("current",s);}},true);}
        synchronized(this){if(cancelled.get())throw new InterruptedException("Cancelled before native completion commit");var r=runs.get(id);r.put("actual",actual).put("status","completed");r.put("verification",verify(r));current.put("status","completed").put("current",actual.getJSONObject("final")).put("verification",r.getJSONObject("verification"));persist();}
    }catch(Exception e){synchronized(this){runs.get(id).put("status",cancelled.get()?"cancelled":"interrupted").put("reason",String.valueOf(e.getMessage()));if(current!=null)current.put("status",runs.get(id).getString("status"));try{persist();}catch(Exception failure){System.err.println(failure);}}}finally{synchronized(this){active="";}}}
    static JSONObject verify(JSONObject run){
        if(!run.getString("status").equals("completed"))throw new IllegalStateException("Spatial run did not complete");var plan=run.getJSONObject("plan");var actual=run.getJSONObject("actual");var expected=plan.getJSONObject("forecast");var intent=plan.getJSONObject("intent");if(!binding(run.getJSONObject("definition")).similar(intent.getJSONObject("binding")))throw new IllegalStateException("Spatial binding differs");
        var a=actual.getJSONArray("samples");var p=expected.getJSONArray("samples");if(a.length()!=p.length()||a.length()<10)throw new IllegalStateException("Forecast/sample alignment differs");double sum=0,max=0;for(int i=0;i<a.length();i++){var x=a.getJSONObject(i);var y=p.getJSONObject(i);if(x.getInt("tick")!=y.getInt("tick"))throw new IllegalStateException("Forecast tick differs");double e=ObserverGeometry.norm(ObserverGeometry.sub(SpatialScene.vector(x.getJSONArray("position"),30),SpatialScene.vector(y.getJSONArray("position"),30)));sum+=e*e;max=Math.max(max,e);}
        var last=a.getJSONObject(a.length()-1);var target=SpatialScene.vector(intent.getJSONArray("targetM"),15);var position=SpatialScene.vector(last.getJSONArray("position"),30);double horizontal=Math.hypot(position[0]-target[0],position[2]-target[2]),vertical=Math.abs(position[1]-target[1]),speed=ObserverGeometry.norm(SpatialScene.vector(last.getJSONArray("velocity"),100));var events=actual.getJSONArray("events");int grabs=0,releases=0,moves=0;double endpoint=0;
        for(Object raw:events){var e=(JSONObject)raw;switch(e.getString("kind")){case "grab"->grabs++;case "move"->{if(moves>=plan.getJSONArray("waypoints").length()||!e.getJSONArray("targetM").similar(plan.getJSONArray("waypoints").getJSONArray(moves)))throw new IllegalStateException("Move intent differs from forecast");moves++;double error=ObserverGeometry.norm(ObserverGeometry.sub(SpatialScene.vector(e.getJSONArray("position"),30),SpatialScene.vector(e.getJSONArray("targetM"),15)));if(Math.abs(error-e.getDouble("errorM"))>2e-6)throw new IllegalStateException("Move endpoint metric differs from native state");endpoint=Math.max(endpoint,error);}case "release"->{releases++;if(!e.getJSONArray("velocityBefore").similar(e.getJSONArray("velocityAfter")))throw new IllegalStateException("Release changed velocity");}default->throw new IllegalStateException("Unknown actual event");}}
        var finalObject=SpatialScene.object(new JSONObject().put("objects",actual.getJSONObject("final").getJSONArray("objects")),intent.getString("objectId"));if(!finalObject.getJSONArray("position").similar(last.getJSONArray("position")))throw new IllegalStateException("Final body differs from sample");
        boolean passed=horizontal<=.1&&vertical<=.15&&speed<=.25&&endpoint<=.08&&max<=.02&&grabs==1&&releases==1&&moves==plan.getJSONArray("waypoints").length()&&!actual.getJSONObject("final").getBoolean("gripping");
        return new JSONObject().put("passed",passed).put("horizontalErrorM",horizontal).put("verticalErrorM",vertical).put("terminalSpeedMps",speed).put("maxGripEndpointErrorM",endpoint).put("forecastRmseM",Math.sqrt(sum/a.length())).put("forecastMaxErrorM",max).put("samples",a.length()).put("grabs",grabs).put("releases",releases).put("route",plan.getString("route")).put("sourceKind",run.getJSONObject("definition").getJSONObject("source").getString("kind")).put("scaleIndependentlyCalibrated",false);
    }
    public synchronized JSONObject cancel(String id) throws Exception {var r=runs.get(id);if(r==null)return new JSONObject();if(id.equals(active)){cancelled.set(true);r.put("status","cancelled").put("reason","Explicitly cancelled; no successful procedure promoted");persist();}return copy(r);}
    @Override public void close(){synchronized(this){closed=true;cancelled.set(true);}worker.shutdownNow();try{worker.awaitTermination(3,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
}
