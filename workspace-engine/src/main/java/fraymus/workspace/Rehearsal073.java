package fraymus.workspace;

import org.json.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/** Executable experiment memory. Native evidence remains authoritative; a replay is a view. */
final class Rehearsal073 implements AutoCloseable {
    static final String VERSION="experiment-memory-073-v1";
    static final String RESTORE="initial-at-rest-cold-controller-073-v1";
    private final Path root;
    private final WorldNativeStore rebuilds;
    Rehearsal073(Path project)throws Exception {
        root=project.resolve("experiments-073");
        rebuilds=new WorldNativeStore(root.resolve("rebuilds"));
    }
    private Path path(String id){UUID.fromString(id);return root.resolve("episodes").resolve(id+".json");}
    static String episodeId(String goal){return UUID.nameUUIDFromBytes((VERSION+":"+goal).getBytes(StandardCharsets.UTF_8)).toString();}
    static void rest(JSONObject run){
        if(!run.getString("status").equals("completed")||!run.getBoolean("orientationControl")||!WorldVerify.verify(run).getBoolean("passed"))throw new IllegalArgumentException("A completed, verified native arrangement is required");
        var scene=run.getJSONObject("definition");WorldFrames.verifyScene(scene);WorldPlan.verify(scene,run.getJSONObject("plan"));
        var f=WorldFrames.frame(scene);
        if(!f.similar(WorldFrames.initial(f.getJSONObject("originalScene")))||run.getJSONObject("plan").getJSONObject("intent").has("construction051")||run.optInt("resumes")!=0)throw new IllegalArgumentException("Memory073 supports initial-at-rest arrangements with a fresh controller, not moving/contact checkpoints or constructions");
    }
    JSONObject remember(JSONObject goal,JSONObject run)throws Exception {
        rest(run);
        if(!goal.has("worldCommit")||!goal.has("report")||!goal.getJSONObject("poseView").optBoolean("passed")||!run.getString("goalId").equals(goal.getString("id")))throw new IllegalStateException("Commit and verified original report are required before memory retention");
        String id=episodeId(goal.getString("id"));
        var e=new JSONObject().put("format",VERSION).put("id",id).put("sourceGoalId",goal.getString("id"))
            .put("nativeIdentity",WorldNativeCheckpoint.identity()).put("restoreContract",RESTORE)
            .put("sourceBinding",goal.getJSONObject("worldBinding")).put("committed",goal.getJSONObject("worldCommit"))
            .put("report",goal.getJSONObject("report")).put("poseView",goal.getJSONObject("poseView"))
            .put("runSha256",WorldFrames.hash(run)).put("run",run);
        if(goal.has("rehearsal073")){var lineage=WorldFrames.copy(goal.getJSONObject("rehearsal073"));lineage.remove("childEpisode");e.put("lineage",lineage);}
        if(Files.exists(path(id))){var old=read(id);var stable=WorldFrames.copy(old);stable.remove("createdAt");if(!stable.similar(e))throw new IllegalStateException("Immutable episode conflicts with the completed goal");return summary(old);}
        if(ids().size()>=32)throw new IllegalStateException("Experiment memory limit32 reached");
        e.put("createdAt",Instant.now().toString());AgentFiles042.saveChecked(path(id),e);return summary(e);
    }
    List<String> ids()throws Exception {
        var p=root.resolve("episodes");if(!Files.exists(p))return List.of();
        try(var files=Files.list(p)){return files.filter(x->x.getFileName().toString().endsWith(".json")).map(x->x.getFileName().toString().replace(".json","")).sorted().toList();}
    }
    JSONObject read(String id)throws Exception {
        var envelope=AgentFiles042.read(path(id),32_000_000);var e=envelope.getJSONObject("data");
        if(!envelope.getString("format").equals("java-agent-042-v1")||!WorldFrames.hash(e).equals(envelope.getString("sha256"))||!VERSION.equals(e.getString("format"))||!id.equals(e.getString("id"))||!id.equals(episodeId(e.getString("sourceGoalId")))||!RESTORE.equals(e.getString("restoreContract"))||!WorldNativeCheckpoint.identity().equals(e.getString("nativeIdentity")))throw new IllegalStateException("Experiment identity, restore contract or native version changed");
        var r=e.getJSONObject("run");rest(r);if(!e.getString("runSha256").equals(WorldFrames.hash(r))||!r.getString("goalId").equals(e.getString("sourceGoalId")))throw new IllegalStateException("Original experiment trace changed");return e;
    }
    static JSONObject summary(JSONObject e){
        var r=e.getJSONObject("run");var roles=new JSONArray();for(Object raw:r.getJSONObject("definition").getJSONArray("objects")){var o=(JSONObject)raw;roles.put(new JSONObject().put("sourceId",o.getString("sourceId")).put("label",o.getString("label")).put("massKg",o.getDouble("massKg")).put("friction",o.getDouble("friction")));}
        var s=new JSONObject();for(String key:List.of("id","sourceGoalId","createdAt","nativeIdentity","restoreContract","report","committed","lineage"))if(e.has(key))s.put(key,e.get(key));
        return s.put("episodeSha256",WorldFrames.hash(e)).put("runId",r.getString("id")).put("roles",roles).put("ticks",r.getJSONObject("final").getInt("tick"));
    }
    static void change(JSONObject request){
        SpatialScene.keys(request,"episodeId","sourceId","property","value","review","request");
        if(request.has("episodeId")&&!request.getString("episodeId").isEmpty())UUID.fromString(request.getString("episodeId"));
        if(!request.getString("sourceId").matches("[A-Za-z0-9_.-]{1,80}"))throw new IllegalArgumentException("Use an actual source role");
        String p=request.getString("property");if(!Set.of("friction","massKg").contains(p))throw new IllegalArgumentException("Change one friction or dynamic payload mass value");
        if(!(request.get("value") instanceof Number))throw new IllegalArgumentException("Component value must be numeric");
        double v=request.getDouble("value");RampDefinition.range(v,p.equals("massKg")?0.000001:0,p.equals("massKg")?20:2,p);
        if(request.has("review")&&!(request.get("review") instanceof Boolean))throw new IllegalArgumentException("Review must be boolean");
        if(request.has("request")&&(request.getString("request").isBlank()||request.getString("request").length()>8000))throw new IllegalArgumentException("Request needs1..8000characters");
    }
    static JSONObject request(String text){
        var pattern=java.util.regex.Pattern.compile("(?i)\\b(?:change|set)\\s+([A-Za-z0-9_.-]+)\\s+(friction|massKg)\\s*(?:to|=|:)\\s*([+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+))");
        var match=pattern.matcher(text);if(!match.find())throw new IllegalArgumentException("Say: Rehearse experiment, change <source role> friction to0.2 and execute. Use massKg for payload mass.");
        var a=new JSONObject().put("sourceId",match.group(1)).put("property",match.group(2).equalsIgnoreCase("massKg")?"massKg":"friction").put("value",Double.parseDouble(match.group(3))).put("review",ChatPolicy044.counterReview045(text));
        if(match.find())throw new IllegalArgumentException("Change exactly one component per experiment");
        var id=java.util.regex.Pattern.compile("(?i)\\bexperiment\\s+([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})\\b").matcher(text);if(id.find())a.put("episodeId",id.group(1).toLowerCase(Locale.ROOT));
        change(a);return a;
    }
    static JSONObject variant(JSONObject episode,JSONObject request){
        change(request);var scene=episode.getJSONObject("run").getJSONObject("definition");var source=WorldFrames.frame(scene).getJSONObject("originalScene");
        var selected=WorldPlan.sourceObject(scene,request.getString("sourceId"));String property=request.getString("property");double before=selected.getDouble(property),after=request.getDouble("value");
        if(property.equals("massKg")&&selected.getDouble("massKg")<=0)throw new IllegalArgumentException("A static body cannot become a payload");
        if(before==after)throw new IllegalArgumentException("Choose a changed component value");
        var authored=new JSONObject(source.getString("sourceText"));boolean found=false;for(Object raw:authored.getJSONArray("objects")){var o=(JSONObject)raw;if(o.getString("sourceId").equals(request.getString("sourceId"))){o.put(property,after);found=true;}}
        if(!found)throw new IllegalStateException("Source role missing from original authoring");
        return new JSONObject().put("sourceText",authored.toString()).put("sourceId",request.getString("sourceId")).put("property",property).put("before",before).put("after",after);
    }
    static JSONObject comparison(JSONObject original,JSONObject rebuilt){
        rest(original);rest(rebuilt);
        if(!original.getJSONObject("definition").similar(rebuilt.getJSONObject("definition"))||!original.getJSONObject("plan").similar(rebuilt.getJSONObject("plan")))throw new IllegalStateException("Reconstruction source or controller plan changed");
        var ticks=new HashMap<Integer,JSONObject>();for(Object raw:original.getJSONArray("samples")){var f=(JSONObject)raw;if(ticks.put(f.getInt("tick"),f)!=null)throw new IllegalStateException("Duplicate original sample tick");}
        int matched=0;double pos=0,angle=0;var seen=new HashSet<Integer>();
        for(Object raw:rebuilt.getJSONArray("samples")){var f=(JSONObject)raw;if(!seen.add(f.getInt("tick")))throw new IllegalStateException("Duplicate rebuilt sample tick");var p=ticks.get(f.getInt("tick"));if(p==null)continue;matched++;if(p.getJSONArray("bodies").length()!=f.getJSONArray("bodies").length())throw new IllegalStateException("Rebuilt body set differs");for(Object item:f.getJSONArray("bodies")){var b=(JSONObject)item;var a=ContactPhysics.dynamic(p.getJSONArray("bodies"),b.getString("id"));pos=Math.max(pos,WorldVerify.distance(a.getJSONArray("position"),b.getJSONArray("position")));angle=Math.max(angle,WorldPlan.angle(a.getJSONArray("rotation"),b.getJSONArray("rotation")));}}
        double coverage=matched/(double)Math.max(1,Math.max(ticks.size(),seen.size()));
        return new JSONObject().put("passed",coverage>=.95&&pos<=.00001&&angle<=.001).put("sampleCoverage",coverage).put("matchedSamples",matched).put("maxPositionResidualM",pos).put("maxAngleResidualDeg",angle).put("originalRunId",original.getString("id")).put("rebuiltRunId",rebuilt.getString("id")).put("originalSha256",WorldFrames.hash(original)).put("rebuiltSha256",WorldFrames.hash(rebuilt)).put("ticks",rebuilt.getJSONObject("final").getInt("tick"));
    }
    void start(String runId,String goalId,JSONObject episode)throws Exception {var r=episode.getJSONObject("run");rest(r);rebuilds.start(runId,goalId,r.getJSONObject("definition"),r.getJSONObject("plan"),false,true);}
    boolean has(String id){return rebuilds.hasRun(id);}
    String status(String id){return rebuilds.status(id);}
    JSONObject run(String id){return rebuilds.run(id);}
    void stop(String id)throws Exception{if(has(id)&&Set.of("running","paused","interrupted").contains(status(id)))rebuilds.cancel(id);}
    @Override public void close(){rebuilds.close();}
}
