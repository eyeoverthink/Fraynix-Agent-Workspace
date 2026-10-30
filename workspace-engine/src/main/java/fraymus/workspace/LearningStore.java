package fraymus.workspace;

import fraymus.factory.*;
import org.json.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

/** Authoritative, project-scoped learning evidence. Proposals cannot install executable tools. */
public final class LearningStore {
    private static final UUID STATE_ID=UUID.fromString("8d495fc3-2921-45e5-ade1-b16e5c9f8661");
    public static final String SUITE="browser-controller-v2";
    public static final List<String> CASES=List.of("train-stale-click","validation-stale-form","final-stale-link","regression-fresh-click","regression-password","regression-counter","regression-broken-counter","validation-observation-budget");
    private static final Set<String> FAILURES=Set.of("none","stale-observation","repeated-observation","test-failed","version-conflict","model-error","tool-error","cancelled");
    private final Path root;
    private final SchemaRegistry schemas=new SchemaRegistry();
    private final FactoryWorld world;
    private final WorldCheckpointStore checkpoints;

    public LearningStore(Path project) throws IOException {
        root=project.resolve("learning");Files.createDirectories(root);
        for(String kind:List.of("state","episode","candidate","evaluation","promotion"))
            schemas.register(new ComponentSchema("workspace.learning."+kind,1,Map.of("payload",ComponentSchema.Kind.TEXT),Set.of("payload")));
        world=new FactoryWorld(schemas);checkpoints=new WorldCheckpointStore(root.resolve("state.fworld"));
        if(Files.exists(root.resolve("state.fworld")))world.restore(checkpoints.load(schemas).snapshot());
        if(find(STATE_ID)==null)commit(List.of(record("state",new JSONObject().put("id",STATE_ID.toString()).put("generation",0).put("activePolicy",baseline()))));
        // Only a newly started owner recovers unfinished runs. Tests and other projects use separate directories.
        var recovered=new ArrayList<EntityRecord>();
        for(var entity:world.snapshot().entities())if(Set.of("episode","candidate").contains(kind(entity))) {
            var data=payload(entity);
            if(data.optString("status").equals("running")||data.optString("status").equals("proposed")) {
                data.put("status","interrupted").put("endedAt",now()).put("outcome","Engine stopped before a final outcome");
                recovered.add(record(kind(entity),data));
            }
        }
        if(!recovered.isEmpty())commit(recovered);
    }
    public static JSONObject baseline(){return new JSONObject().put("version",1).put("observationLimit",2).put("refreshStale",false);}
    public static String digest(String value){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
        catch(Exception error){throw new IllegalStateException(error);}
    }
    public static JSONObject policy(JSONObject input) {
        if(!input.keySet().equals(Set.of("observationLimit","refreshStale")))throw new IllegalArgumentException("Controller can change only observationLimit and refreshStale");
        Object limit=input.get("observationLimit"),refresh=input.get("refreshStale");
        if(!(limit instanceof Number)||((Number)limit).doubleValue()!=((Number)limit).intValue()||input.getInt("observationLimit")<1||input.getInt("observationLimit")>2||!(refresh instanceof Boolean))
            throw new IllegalArgumentException("Controller policy exceeds its bounded strategy schema");
        return new JSONObject().put("observationLimit",input.getInt("observationLimit")).put("refreshStale",input.getBoolean("refreshStale"));
    }
    private static String now(){return Instant.now().toString();}
    private static JSONObject copy(JSONObject value){return new JSONObject(value.toString());}
    private EntityRecord find(UUID id){return world.snapshot().entities().stream().filter(e->e.id().equals(id)).findFirst().orElse(null);}
    private static String kind(EntityRecord entity){return entity.components().get(0).type().substring("workspace.learning.".length());}
    private static JSONObject payload(EntityRecord entity){return new JSONObject(entity.components().get(0).values().get("payload"));}
    private JSONObject require(String id,String type){var entity=find(UUID.fromString(id));if(entity==null||!kind(entity).equals(type))throw new IllegalArgumentException("Unknown learning "+type);return payload(entity);}
    private EntityRecord record(String type,JSONObject data){
        UUID id=UUID.fromString(data.getString("id"));var existing=find(id);UUID component=existing==null?UUID.randomUUID():existing.components().get(0).id();
        return new EntityRecord(id,type+" · "+data.optString("label",data.optString("status","record")),List.of(new ComponentRecord(component,"workspace.learning."+type,1,Map.of("payload",data.toString()))));
    }
    private void commit(List<EntityRecord> records) throws IOException {
        var before=world.snapshot();var tx=new WorldTransaction(before);
        for(var entity:records)if(find(entity.id())==null)tx.add(new WorldTransaction.Create(entity));else {tx.add(new WorldTransaction.Rename(entity.id(),entity.name()));tx.add(new WorldTransaction.Put(entity.id(),entity.components().get(0)));}
        try{tx.commit(world,schemas);checkpoints.save(world.snapshot(),schemas);}catch(IOException|RuntimeException error){world.restore(before);throw error;}
    }
    private JSONObject header(){return payload(Objects.requireNonNull(find(STATE_ID)));}
    private JSONArray list(String type,int maximum){
        var values=world.snapshot().entities().stream().filter(e->kind(e).equals(type)).map(LearningStore::payload).toList();var out=new JSONArray();
        for(int i=values.size()-1;i>=Math.max(0,values.size()-maximum);i--)out.put(values.get(i));return out;
    }
    public synchronized JSONObject state(){
        var head=header();return new JSONObject().put("generation",head.getInt("generation")).put("runtimeContract",SUITE).put("activePolicy",head.getJSONObject("activePolicy")).put("controllerMemory",head.opt("controllerMemory"))
            .put("episodes",list("episode",50)).put("candidates",list("candidate",30)).put("evaluations",list("evaluation",30)).put("promotions",list("promotion",30))
            .put("episodeCount",world.snapshot().entities().stream().filter(e->kind(e).equals("episode")).count());
    }
    public synchronized JSONObject read(String type,String id){if(!Set.of("episode","candidate","evaluation","promotion").contains(type))throw new IllegalArgumentException("Unsupported learning record");return require(id,type);}
    public synchronized JSONObject begin(JSONObject input) throws IOException {
        String id=input.getString("id");if(find(UUID.fromString(id))!=null)throw new IllegalArgumentException("Episode already exists");
        String taskKind=input.getString("taskKind");if(!Set.of("artifact-task","browser-task","controller-rehearsal").contains(taskKind))throw new IllegalArgumentException("Invalid episode task kind");
        String model=input.optString("model","deterministic-controller");if(model.length()>140)throw new IllegalArgumentException("Model name too long");
        var episode=new JSONObject().put("id",id).put("label",taskKind).put("taskKind",taskKind).put("model",model).put("goalDigest",digest(input.optString("goal","")))
            .put("policy",header().getJSONObject("activePolicy")).put("runtimeContract",SUITE).put("status","running").put("startedAt",now()).put("steps",new JSONArray()).put("privacy","No page text, typed values, source or raw goals in episodes");
        commit(List.of(record("episode",episode)));return episode;
    }
    public synchronized JSONObject step(JSONObject input) throws IOException {
        var episode=require(input.getString("id"),"episode");if(!episode.getString("status").equals("running"))throw new IllegalStateException("Episode is closed");
        var steps=episode.getJSONArray("steps");if(steps.length()>=96)throw new IllegalStateException("Episode step limit reached");
        var raw=input.getJSONObject("step");Set<String> allowed=Set.of("tool","ok","failure","elapsedMs","observationId","controlCount","artifactId","artifactVersion","testPassed","transition");
        if(!allowed.containsAll(raw.keySet()))throw new IllegalArgumentException("Episode contains unsupported or raw content fields");
        String tool=raw.getString("tool"),failure=raw.optString("failure","none");if(!tool.matches("[a-z]+(?:\\.[a-z]+)?")||!FAILURES.contains(failure))throw new IllegalArgumentException("Invalid episode action or failure class");
        var item=new JSONObject().put("index",steps.length()).put("at",now()).put("tool",tool).put("ok",raw.getBoolean("ok")).put("failure",failure)
            .put("elapsedMs",Math.max(0,Math.min(600_000,raw.optLong("elapsedMs",0))));
        for(String key:List.of("observationId","artifactId"))if(raw.has(key)){UUID.fromString(raw.getString(key));item.put(key,raw.getString(key));}
        for(String key:List.of("controlCount","artifactVersion"))if(raw.has(key)){int number=raw.getInt(key);if(number<0||number>1_000_000)throw new IllegalArgumentException("Invalid episode count");item.put(key,number);}
        if(raw.has("testPassed")) {
            boolean passed=raw.getBoolean("testPassed");
            if(passed) {
                if(!tool.equals("canvas.test")||!raw.getBoolean("ok")||!raw.has("artifactId")||!raw.has("artifactVersion"))throw new IllegalStateException("Passing episode evidence needs an artifact test receipt");
                var receipt=new JSONObject(Files.readString(root.getParent().resolve("receipts").resolve(raw.getString("artifactId")+"-"+raw.getInt("artifactVersion")+".json")));
                if(!receipt.optBoolean("passed"))throw new IllegalStateException("Artifact receipt did not pass");
            }
            item.put("testPassed",passed);
        }
        if(raw.has("transition")){String transition=raw.getString("transition");if(!Set.of("refresh-before-action","refresh-after-change","read-budget","repair","none").contains(transition))throw new IllegalArgumentException("Invalid transition");item.put("transition",transition);}
        steps.put(item);commit(List.of(record("episode",episode)));return new JSONObject().put("saved",true).put("steps",steps.length());
    }
    public synchronized JSONObject end(JSONObject input) throws IOException {
        var episode=require(input.getString("id"),"episode");if(!episode.getString("status").equals("running"))throw new IllegalStateException("Episode is already closed");
        String status=input.getString("status");if(!Set.of("verified","completed-unverified","failed","cancelled").contains(status))throw new IllegalArgumentException("Invalid episode outcome");
        if(status.equals("verified")&&episode.getJSONArray("steps").toList().stream().noneMatch(x->Boolean.TRUE.equals(((Map<?,?>)x).get("testPassed"))))throw new IllegalStateException("A verified episode needs a passing artifact receipt step");
        episode.put("status",status).put("endedAt",now());commit(List.of(record("episode",episode)));return episode;
    }
    public synchronized JSONObject propose(JSONObject input) throws IOException {
        var head=header();int parent=head.getJSONObject("activePolicy").getInt("version");
        if(input.getInt("parentVersion")!=parent||input.getInt("parentGeneration")!=head.getInt("generation"))throw new ConcurrentModificationException("Active policy changed; learn from the current version");
        var proposal=policy(input.getJSONObject("policy"));var ids=input.getJSONArray("episodes");if(ids.isEmpty()||ids.length()>50)throw new IllegalArgumentException("A candidate needs 1..50 recorded episodes");
        boolean supported=false;
        for(Object id:ids){var episode=require(id.toString(),"episode");if(episode.getString("status").equals("running"))throw new IllegalStateException("Finish episodes before learning");
            for(Object raw:episode.getJSONArray("steps")){var step=(JSONObject)raw;if(Set.of("stale-observation","repeated-observation").contains(step.optString("failure")))supported=true;}}
        if(!supported)throw new IllegalStateException("No recorded controller failure supports a candidate");
        int version=2;for(Object raw:list("candidate",Integer.MAX_VALUE))version=Math.max(version,((JSONObject)raw).getJSONObject("policy").getInt("version")+1);
        proposal.put("version",version);var candidate=new JSONObject().put("id",UUID.randomUUID().toString()).put("label","Controller v"+version).put("status","proposed").put("policy",proposal)
            .put("parentVersion",parent).put("parentGeneration",head.getInt("generation")).put("episodes",ids).put("runtimeContract",SUITE).put("createdAt",now()).put("mechanism","bounded-rule-adaptation")
            .put("kernelCommit","bf7c513e97480376d2583688f9e5b43c6c44657f");
        if(input.has("controllerMemory"))candidate.put("controllerMemory",controllerMemory(input.getJSONObject("controllerMemory")));
        commit(List.of(record("candidate",candidate)));return candidate;
    }
    public synchronized JSONObject evaluate(JSONObject input) throws IOException {
        var candidate=require(input.getString("candidateId"),"candidate");var head=header();
        if(!candidate.getString("status").equals("proposed"))throw new IllegalStateException("Candidate already evaluated");
        if(candidate.getInt("parentGeneration")!=head.getInt("generation")||candidate.getInt("parentVersion")!=head.getJSONObject("activePolicy").getInt("version"))throw new ConcurrentModificationException("Candidate parent is no longer active");
        if(!input.getString("suite").equals(SUITE)||!input.getString("suiteDigest").equals(digest(SUITE+":"+String.join(",",CASES))))throw new IllegalArgumentException("Unknown independent replay suite");
        JSONArray rows=input.getJSONArray("results");if(rows.length()!=CASES.size())throw new IllegalArgumentException("Replay needs every fixed fixture");
        Set<String> seen=new HashSet<>();int baselinePass=0,candidatePass=0,baselineFailures=0,candidateFailures=0;boolean regression=false;
        for(Object raw:rows){var row=(JSONObject)raw;String id=row.getString("id");if(!CASES.contains(id)||!seen.add(id))throw new IllegalArgumentException("Unknown or duplicate replay case");
            var a=row.getJSONObject("baseline");var b=row.getJSONObject("candidate");boolean ap=a.getBoolean("passed"),bp=b.getBoolean("passed");
            int af=a.getInt("failures"),bf=b.getInt("failures");if(af<0||bf<0||af>50||bf>50)throw new IllegalArgumentException("Invalid replay metrics");
            if(ap)baselinePass++;if(bp)candidatePass++;if(ap&&!bp)regression=true;baselineFailures+=af;candidateFailures+=bf;
        }
        boolean accepted=!regression&&candidatePass==CASES.size()&&(candidatePass>baselinePass||candidateFailures<baselineFailures);
        var report=new JSONObject().put("id",UUID.randomUUID().toString()).put("label","Replay · controller v"+candidate.getJSONObject("policy").getInt("version")).put("candidateId",candidate.getString("id"))
            .put("suite",SUITE).put("suiteDigest",input.getString("suiteDigest")).put("results",rows).put("accepted",accepted).put("regression",regression)
            .put("baselinePassed",baselinePass).put("candidatePassed",candidatePass).put("baselineFailures",baselineFailures).put("candidateFailures",candidateFailures).put("at",now())
            .put("scope","Deterministic browser/controller and canvas acceptance fixtures; not a general intelligence score");
        candidate.put("status",accepted?"promoted":"rejected").put("evaluationId",report.getString("id"));
        var records=new ArrayList<EntityRecord>(List.of(record("candidate",candidate),record("evaluation",report)));
        if(accepted){var promotion=new JSONObject().put("id",UUID.randomUUID().toString()).put("label","Promoted controller v"+candidate.getJSONObject("policy").getInt("version")).put("kind","promotion")
                .put("previousPolicy",copy(head.getJSONObject("activePolicy"))).put("previousMemory",head.opt("controllerMemory")).put("activePolicy",copy(candidate.getJSONObject("policy"))).put("candidateId",candidate.getString("id")).put("evaluationId",report.getString("id")).put("at",now());
            head.put("activePolicy",copy(candidate.getJSONObject("policy"))).put("controllerMemory",candidate.opt("controllerMemory")).put("generation",head.getInt("generation")+1);records.add(record("state",head));records.add(record("promotion",promotion));}
        commit(records);return report;
    }
    public synchronized JSONObject rollback() throws IOException {
        var head=header();JSONObject prior=null;
        for(Object raw:list("promotion",Integer.MAX_VALUE)){var item=(JSONObject)raw;if(item.optString("kind").equals("promotion")&&item.getJSONObject("activePolicy").getInt("version")==head.getJSONObject("activePolicy").getInt("version")){prior=item;break;}}
        if(prior==null)throw new IllegalStateException("No earlier controller policy to restore");
        var entry=new JSONObject().put("id",UUID.randomUUID().toString()).put("label","Rolled back controller v"+head.getJSONObject("activePolicy").getInt("version")).put("kind","rollback")
            .put("previousPolicy",copy(head.getJSONObject("activePolicy"))).put("activePolicy",copy(prior.getJSONObject("previousPolicy"))).put("at",now());
        head.put("activePolicy",copy(prior.getJSONObject("previousPolicy"))).put("controllerMemory",prior.opt("previousMemory")).put("generation",head.getInt("generation")+1);
        commit(List.of(record("state",head),record("promotion",entry)));return entry;
    }
    public synchronized JSONObject cancelCandidate(String id,String status) throws IOException {
        if(!Set.of("cancelled","failed").contains(status))throw new IllegalArgumentException("Invalid unfinished candidate status");
        var candidate=require(id,"candidate");if(candidate.getString("status").equals("proposed")){candidate.put("status",status).put("endedAt",now());commit(List.of(record("candidate",candidate)));}return candidate;
    }
    private static JSONObject controllerMemory(JSONObject raw) {
        if(raw.toString().length()>40_000||raw.optInt("version")!=1||raw.getJSONArray("classes").length()!=2)throw new IllegalArgumentException("Invalid bounded controller memory");
        var clean=new JSONObject().put("version",1).put("alpha",.62).put("totalUpdates",Math.max(0,Math.min(1_000_000,raw.optInt("totalUpdates"))));var classes=new JSONArray();Set<String> ids=new HashSet<>();
        for(Object value:raw.getJSONArray("classes")){var c=(JSONObject)value;String id=c.getString("id");if(!Set.of("stale-observation","repeated-observation").contains(id)||!ids.add(id))throw new IllegalArgumentException("Invalid controller class");
            var centroid=vector(c.getJSONArray("centroid"));var subs=c.getJSONArray("subPrototypes");if(subs.length()>4)throw new IllegalArgumentException("Too many controller prototypes");var prototypes=new JSONArray();for(Object sub:subs)prototypes.put(vector((JSONArray)sub));
            classes.put(new JSONObject().put("id",id).put("label",id).put("seedText",id).put("centroid",centroid).put("subPrototypes",prototypes).put("samples",Math.max(1,Math.min(1_000_000,c.getInt("samples")))));}
        var episodes=raw.optJSONArray("episodeIds");var episodeIds=new JSONArray();if(episodes!=null){if(episodes.length()>256)throw new IllegalArgumentException("Controller episode index too large");for(Object id:episodes){UUID.fromString(id.toString());episodeIds.put(id);}}
        return clean.put("classes",classes).put("episodeIds",episodeIds);
    }
    private static JSONArray vector(JSONArray input){if(input.length()!=64)throw new IllegalArgumentException("Controller vectors have 64 dimensions");var result=new JSONArray();for(Object value:input){if(!(value instanceof Number n)||!Double.isFinite(n.doubleValue())||Math.abs(n.doubleValue())>2)throw new IllegalArgumentException("Invalid controller vector value");result.put(value);}return result;}
}
