package fraymus.workspace;

import org.json.*;
import java.util.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/** Explicit audit adapter for exact published107/109/110 identities, never execution authority. */
final class Qualification111 {
    static final String KEY="transition111",SCHEMA="qualification-history-transition-111-v1";
    private static final String RESOURCE="/history111-published.json";
    private static JSONObject published(){try(var in=Qualification111.class.getResourceAsStream(RESOURCE)){return new JSONObject(new String(in.readAllBytes(),StandardCharsets.UTF_8));}catch(Exception e){throw new IllegalStateException(e);}}
    static String code(){try{var h=MessageDigest.getInstance("SHA-256");for(String p:List.of("Qualification111.class",RESOURCE))try(var in=Qualification111.class.getResourceAsStream(p)){h.update(in.readAllBytes());}return HexFormat.of().formatHex(h.digest());}catch(Exception e){throw new IllegalStateException(e);}}
    static boolean supported(JSONObject parent){var q=parent.optJSONObject("qualification105");if(q==null||!q.has("maintenance107"))return false;return known(q.getJSONObject("maintenance107").getJSONObject("dependencies"));}
    static boolean known(JSONObject dependencies){for(Object value:published().getJSONArray("sources"))if(((JSONObject)value).getJSONObject("dependencies").similar(dependencies))return true;return false;}
    static void unchangedVerifiers(JSONObject old){
        if(!known(old))throw new IllegalStateException("Unreviewed historical dependency identity; preserve originals");
        var now=Qualification107.dependencies();for(String key:old.keySet())if(!Set.of("WorkflowRunner","GoalRunner","Qualification107","suite105").contains(key)&&!old.getString(key).equals(now.getString(key)))throw new IllegalStateException("Historical outcome verifier changed: "+key);
    }
    static JSONObject original(JSONObject parent){
        if(!parent.getString("status").equals("completed")||!parent.getJSONObject("audit").getBoolean("passed"))throw new IllegalStateException("Historical qualification is incomplete");
        var q=parent.getJSONObject("qualification105");var m=q.getJSONObject("maintenance107");SpatialScene.keys(m,"schema","mode","dependencies","baseline");
        if(!m.getString("schema").equals("qualification-maintenance-107-v1")||!Set.of("recheck","refresh").contains(m.getString("mode"))||!q.getString("maintenanceSha256").equals(WorldFrames.hash(m))||!q.getString("codeSha256").equals(m.getJSONObject("dependencies").getString("suite105"))||!q.getString("definitionSha256").equals(WorldFrames.hash(parent.getJSONObject("definition"))))throw new IllegalStateException("Historical qualification intent changed");
        unchangedVerifiers(m.getJSONObject("dependencies"));return m;
    }
    static JSONArray changes(JSONObject old,JSONObject now){var out=new JSONArray();var keys=new TreeSet<>(old.keySet());keys.addAll(now.keySet());for(String key:keys)if(!Objects.equals(old.opt(key),now.opt(key)))out.put(new JSONObject().put("name",key).put("from",old.opt(key)==null?JSONObject.NULL:old.get(key)).put("to",now.opt(key)==null?JSONObject.NULL:now.get(key)));return out;}
    static JSONObject manifest(JSONObject parent,JSONArray proofs){var old=original(parent);return new JSONObject().put("schema",SCHEMA).put("codeSha256",code()).put("fromWorkflowId",parent.getString("id")).put("fromRecordSha256",WorldFrames.hash(parent)).put("fromDependencies",old.getJSONObject("dependencies")).put("toDependencies",Qualification107.dependencies()).put("changes",changes(old.getJSONObject("dependencies"),Qualification107.dependencies())).put("originalProofs",proofs).put("policy","Audit exact published history; execute all five fresh cases; preserve original identities");}
    static void verify(JSONObject q){var m=q.getJSONObject(KEY);SpatialScene.keys(m,"schema","codeSha256","fromWorkflowId","fromRecordSha256","fromDependencies","toDependencies","changes","originalProofs","policy");unchangedVerifiers(m.getJSONObject("fromDependencies"));if(!SCHEMA.equals(m.getString("schema"))||!code().equals(m.getString("codeSha256"))||!q.getString("transitionSha256").equals(WorldFrames.hash(m))||!Qualification107.dependencies().similar(m.getJSONObject("toDependencies"))||!changes(m.getJSONObject("fromDependencies"),Qualification107.dependencies()).similar(m.getJSONArray("changes"))||!q.getJSONObject("maintenance107").getString("mode").equals("refresh"))throw new IllegalStateException("Historical transition intent or runtime changed");}
    static boolean historicalProtocol(JSONObject binding,JSONObject completed)throws Exception{
        if(Implementation103.code().equals(binding.optString("codeSha256")))return false;
        if(!completed.optString("status").equals("completed")||!Implementation103.SCHEMA.equals(binding.optString("schema")))throw new IllegalStateException("Historical implementation is inspection-only; no active execution or resume");
        for(Object value:published().getJSONArray("sources")){var entry=(JSONObject)value;if(entry.getString("implementation103").equals(binding.optString("codeSha256"))){unchangedVerifiers(entry.getJSONObject("dependencies"));return true;}}
        throw new IllegalStateException("Implementation reuse protocol changed; unreviewed original identity");
    }
    static void originalImplementation(JSONObject binding,JSONObject child,JSONObject artifact)throws Exception{
        // Same original binding fields as103, compared without substituting its retained identity.
        SpatialScene.keys(binding,"schema","codeSha256","sourceWorkflowId","sourceGoalId","skill","profile","verifier","artifact","implementationSha256","sourceBinding","modelName","modelDigest");
        if(!child.getString("id").equals(binding.getString("sourceGoalId"))||!child.getString("workflowId").equals(binding.getString("sourceWorkflowId"))||!child.getString("status").equals("completed")||!child.getJSONObject("sourceBinding").similar(binding.getJSONObject("sourceBinding"))||!Creation.reference(artifact).similar(binding.getJSONObject("artifact"))||!Creation.digest(Implementation103.files(artifact)).equals(binding.getString("implementationSha256"))||!child.getString("modelName").equals(binding.getString("modelName"))||!child.getString("modelDigest").equals(binding.getString("modelDigest")))throw new IllegalStateException("Original historical implementation or provenance changed");
    }
}
