package fraymus.workspace;

import org.json.*;
import java.time.Instant;
import java.util.*;

/** Inert typed contracts. Execution stays in WorkflowRunner and GoalRunner. */
final class SkillGraph046 {
    static final String KIND="typed-skill-graph-v1", SCHEMA="fraynix.skill-graph-046-v1";
    static final String SCOPE="Declared-scale native simulation; exact retained procedures, original native/report gates. Integrity and repeated success are not physical accuracy, consciousness or LLM-weight learning.";
    private SkillGraph046() {}

    static String code(){var hashes=new JSONObject();for(Class<?> t:List.of(SkillGraph046.class,ContractMemo.class,GraphReport046.class,WorkflowRunner.class,GoalRunner.class))try(var in=t.getResourceAsStream("/"+t.getName().replace('.','/')+".class")){hashes.put(t.getName(),MapDefinition.sha(in.readAllBytes()));}catch(Exception e){throw new IllegalStateException(e);}return WorldFrames.hash(hashes);}
    static JSONObject ref(JSONObject p){return new JSONObject().put("name",p.getString("name")).put("version",p.getInt("version")).put("id",p.getString("id")).put("kind",p.getString("kind")).put("sha256",WorldFrames.hash(p)).put("controllerSha256",p.getString("worldCodeSha256"));}
    static JSONObject selected(GoalRunner goals,JSONObject ref) throws Exception {
        SpatialScene.keys(ref,"name","version","id","kind","sha256","controllerSha256");
        if(!(ref.get("version") instanceof Number v)||v.doubleValue()!=v.intValue())throw new IllegalArgumentException("Exact integer skill version required");
        for(Object raw:goals.procedures()){
            var p=(JSONObject)raw;
            if(p.getString("name").equals(ref.getString("name"))&&p.getInt("version")==ref.getInt("version")){
                if(!p.getString("kind").equals(WorldPlan.PROCEDURE)||!ref(p).similar(ref))throw new IllegalStateException("Retained skill content/type changed");
                if(!p.getString("worldCodeSha256").equals(WorldNativeCheckpoint.identity()))throw new IllegalStateException("Retained native controller changed");
                goals.verifyProcedure046(p);return WorldFrames.copy(p);
            }
        }
        throw new IllegalArgumentException("Unknown exact retained skill");
    }
    static JSONObject binding(JSONObject b){
        SpatialScene.keys(b,"worldId","expectedRevision","expectedHash","unit","frame");
        UUID.fromString(b.getString("worldId"));
        if(!b.getString("unit").equals("m")||!b.getString("frame").equals("native3d"))throw new IllegalArgumentException("World edges require metres in native3d coordinates");
        if(!(b.get("expectedRevision") instanceof Number n)||n.doubleValue()!=n.intValue()||n.intValue()<0||!b.getString("expectedHash").matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Exact world revision/head required");
        var out=WorldFrames.copy(b);out.remove("unit");out.remove("frame");return out;
    }
    static JSONObject typed(JSONObject b){return WorldFrames.copy(b).put("unit","m").put("frame","native3d");}
    static void current(GoalRunner goals,JSONObject typed) throws Exception {
        var b=binding(typed);var now=goals.worlds().observe(b.getString("worldId"));
        if(now.has("pending")||now.getInt("expectedRevision")!=b.getInt("expectedRevision")||!now.getString("expectedHash").equals(b.getString("expectedHash")))throw new IllegalStateException("Stale or owned graph source; observe and confirm a fresh world");
    }
    static JSONObject definition(JSONObject graph,GoalRunner goals) throws Exception {
        if(graph.toString().length()>32000)throw new IllegalArgumentException("Graph definition exceeds32K characters");SpatialScene.keys(graph,"schema","nodes");if(!graph.getString("schema").equals(SCHEMA))throw new IllegalArgumentException("Unsupported skill graph schema");
        var nodes=graph.getJSONArray("nodes");if(nodes.length()<3||nodes.length()>5)throw new IllegalArgumentException("Use two to four world procedures and one comparison");
        var byId=new LinkedHashMap<String,JSONObject>();JSONObject comparison=null;
        for(Object raw:nodes){
            var n=(JSONObject)raw;String id=n.getString("id");if(!id.matches("[a-z][a-z0-9-]{0,31}")||id.equals("world")||byId.putIfAbsent(id,n)!=null)throw new IllegalArgumentException("Distinct bounded graph node names required");
            if(n.getString("kind").equals("procedure")){
                SpatialScene.keys(n,"id","kind","skill","source","unit","frame","targets","forceLimitN","torqueLimitNm");
                if(!n.getString("unit").equals("m")||!n.getString("frame").equals("native3d"))throw new IllegalArgumentException("Procedure input type must be metres/native3d");
                var p=selected(goals,n.getJSONObject("skill"));
                for(String k:List.of("forceLimitN","torqueLimitNm")){
                    if(!(n.get(k) instanceof Number num)||!Double.isFinite(num.doubleValue())||num.doubleValue()<(k.equals("forceLimitN")?2:0)||num.doubleValue()>p.getJSONObject("defaults").getDouble(k))throw new IllegalArgumentException("Graph cannot increase retained wrench authority: "+k);
                }
                // Geometry is checked against the actual incoming frame immediately before admission.
                if(n.getJSONArray("targets").length()<2||n.getJSONArray("targets").length()>4)throw new IllegalArgumentException("Two to four distinct native targets required");
                var roles=new HashSet<String>();for(Object t:n.getJSONArray("targets")){var target=(JSONObject)t;SpatialScene.keys(target,"sourceId","positionM","yawDeg");if(!roles.add(Creation.text(target,"sourceId",80)))throw new IllegalArgumentException("Repeated target role");SpatialScene.vector(target.getJSONArray("positionM"),5);SpatialScene.number(target,"yawDeg",-180,180);}
            }else if(n.getString("kind").equals("compare")){
                SpatialScene.keys(n,"id","kind","sources");if(comparison!=null)throw new IllegalArgumentException("One terminal comparison required");comparison=n;
            }else throw new IllegalArgumentException("Unknown registered graph node kind");
        }
        if(comparison==null)throw new IllegalArgumentException("A checked comparison is required");
        var ordered=new JSONArray();String prior="world";var seen=new HashSet<String>();
        while(true){JSONObject next=null;for(var n:byId.values())if(n.getString("kind").equals("procedure")&&n.getString("source").equals(prior)){if(next!=null)throw new IllegalArgumentException("This world type supports one successor, not competing writers");next=n;}
            if(next==null)break;if(!seen.add(next.getString("id")))throw new IllegalArgumentException("Cycle in skill graph");ordered.put(WorldFrames.copy(next));prior=next.getString("id");
        }
        if(ordered.length()!=nodes.length()-1)throw new IllegalArgumentException("Cycle, foreign edge or disconnected procedure");
        var sources=comparison.getJSONArray("sources");if(sources.length()!=ordered.length())throw new IllegalArgumentException("Comparison must retain every procedure outcome");
        for(int i=0;i<sources.length();i++)if(!(sources.get(i) instanceof String)||!sources.getString(i).equals(ordered.getJSONObject(i).getString("id")))throw new IllegalArgumentException("Comparison edges must follow the verified world chain");
        ordered.put(WorldFrames.copy(comparison));return new JSONObject().put("schema",SCHEMA).put("nodes",ordered);
    }
    static JSONObject intent(JSONObject n,JSONObject source,String request){
        var out=binding(source);out.put("name",n.getJSONObject("skill").getString("name")).put("version",n.getJSONObject("skill").getInt("version")).put("targets",n.getJSONArray("targets")).put("forceLimitN",n.getDouble("forceLimitN")).put("torqueLimitNm",n.getDouble("torqueLimitNm")).put("request",request);
        return out;
    }
    static JSONObject outcome(GoalRunner goals,String owner,JSONObject child,JSONObject prior) throws Exception {
        var g=goals.verifiedOwned(owner,child.getString("id"));if(!g.getString("procedure").equals(WorldPlan.PROCEDURE)||!g.getJSONObject("worldBinding").similar(binding(prior)))throw new IllegalStateException("Foreign outcome or input type");
        var rev=goals.worlds().committed(prior.getString("worldId"),g.getString("id"));
        if(rev.isEmpty()||!rev.similar(g.getJSONObject("worldCommit"))||rev.getInt("revision")!=prior.getInt("expectedRevision")+1||!rev.getString("parentHash").equals(prior.getString("expectedHash")))throw new IllegalStateException("Missing exact owned world commit");
        return new JSONObject().put("type","verified-world-outcome").put("goalId",g.getString("id")).put("runId",g.getString("runId")).put("input",WorldFrames.copy(prior)).put("world",typed(new JSONObject().put("worldId",prior.getString("worldId")).put("expectedRevision",rev.getInt("revision")).put("expectedHash",rev.getString("hash")))).put("nativeSha256",rev.getString("nativeCanonicalSha256")).put("metrics",g.getJSONObject("metrics")).put("report",g.getJSONObject("report"));
    }
    /** Dreamer-inspired content seal, ordered by predecessor rather than trusting wall time. */
    static JSONObject seal(JSONObject output,String previous){var sealed=WorldFrames.copy(output).put("previousSha256",previous);return sealed.put("sha256",WorldFrames.hash(sealed));}
    static void verifySeal(JSONObject sealed,String previous){var raw=WorldFrames.copy(sealed);String sha=raw.getString("sha256");raw.remove("sha256");if(!raw.getString("previousSha256").equals(previous)||!WorldFrames.hash(raw).equals(sha))throw new IllegalStateException("Graph witness content/order changed");}
    /** Logic-Chain normalized ledger: keep first text and EVERY occurrence/origin, never vote away a failure. */
    static void diagnostic(JSONObject w,String node,String text){
        String key=text.strip().replaceAll("\\s+"," ").toLowerCase(Locale.ROOT);var all=w.getJSONArray("diagnostics");JSONObject entry=null;
        for(Object raw:all){var r=(JSONObject)raw;if(r.getString("key").equals(key)){entry=r;break;}}
        if(entry==null){entry=new JSONObject().put("key",key).put("text",text).put("verified",false).put("occurrences",new JSONArray());all.put(entry);}
        entry.getJSONArray("occurrences").put(new JSONObject().put("node",node).put("at",Instant.now()).put("resumes",w.getInt("resumes")));
    }
    /** FraynixGPT-inspired reuse, made exact: only pure contract results; never cache actions or outcomes. */
    static final class ContractMemo {
        final LinkedHashMap<String,JSONObject> entries=new LinkedHashMap<>();long hits,misses,audits,invalidations;
        JSONObject get(JSONObject definition,GoalRunner goals) throws Exception {
            // Reconstruct original receipts even on hits. New content/code/source never inherits old trust.
            for(Object raw:definition.getJSONArray("nodes")){var n=(JSONObject)raw;if(n.getString("kind").equals("procedure"))selected(goals,n.getJSONObject("skill"));}
            String key=WorldFrames.hash(new JSONObject().put("definition",definition).put("code",code()).put("controller",WorldNativeCheckpoint.identity()));
            var cached=entries.get(key);if(cached!=null){hits++;if(hits%4==0){audits++;var fresh=SkillGraph046.definition(definition,goals);if(!cached.similar(fresh)){entries.remove(key);invalidations++;throw new IllegalStateException("Exact graph contract audit failed");}}return WorldFrames.copy(cached);}
            misses++;var fresh=SkillGraph046.definition(definition,goals);if(entries.size()>=16)entries.remove(entries.keySet().iterator().next());entries.put(key,WorldFrames.copy(fresh));return fresh;
        }
        JSONObject metrics(){return new JSONObject().put("hits",hits).put("misses",misses).put("audits",audits).put("invalidations",invalidations).put("entries",entries.size()).put("scope","pure contracts only; all original physical/report checks always run");}
    }
}
