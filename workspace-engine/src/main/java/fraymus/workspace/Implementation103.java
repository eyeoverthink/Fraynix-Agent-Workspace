package fraymus.workspace;

import org.json.*;
import java.util.*;
import java.security.MessageDigest;

/** Exact implementation provenance. Old receipts never become receipts for fresh inputs. */
final class Implementation103 {
    static final String SCHEMA="verified-implementation-103-v1";
    static String code() throws Exception {
        var hash=MessageDigest.getInstance("SHA-256");
        for(Class<?> c:List.of(Implementation103.class,WorkflowRunner.class,GoalRunner.class,Creation.class,GoalArchive111.class,Qualification111.class))
            try(var in=c.getResourceAsStream(c.getSimpleName()+".class")){hash.update(in.readAllBytes());}
        hash.update(Qualification111.code().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash.digest());
    }
    static JSONObject files(JSONObject artifact) {
        var out=new JSONObject();for(String k:artifact.getJSONObject("contents").keySet())
            if(!k.startsWith("inputs/"))out.put(k,artifact.getJSONObject("contents").getString(k));
        // Retain the ordinary file/path/size restrictions, even for a previously accepted artifact.
        return Creation.files(new JSONObject(),out);
    }
    static JSONObject binding(JSONObject parent,JSONObject child,JSONObject artifact) throws Exception {
        return new JSONObject().put("schema",SCHEMA).put("codeSha256",code())
            .put("sourceWorkflowId",parent.getString("id")).put("sourceGoalId",child.getString("id"))
            .put("skill",WorldFrames.copy(parent.getJSONObject("savedSkill")))
            .put("profile",parent.getString("profile101")).put("verifier",parent.getString("code101"))
            .put("artifact",Creation.reference(artifact)).put("implementationSha256",Creation.digest(files(artifact)))
            .put("sourceBinding",WorldFrames.copy(child.getJSONObject("sourceBinding")))
            .put("modelName",child.getString("modelName")).put("modelDigest",child.getString("modelDigest"));
    }
    static void current(JSONObject binding) throws Exception {
        if(!SCHEMA.equals(binding.getString("schema"))||!code().equals(binding.getString("codeSha256")))
            throw new IllegalStateException("Implementation reuse protocol changed; start a newly reviewed request");
    }
    static void original(JSONObject binding,JSONObject child,JSONObject artifact) throws Exception {
        current(binding);
        if(!child.getString("id").equals(binding.getString("sourceGoalId"))
            ||!child.getString("workflowId").equals(binding.getString("sourceWorkflowId"))
            ||!child.getString("status").equals("completed")
            ||!child.getJSONObject("sourceBinding").similar(binding.getJSONObject("sourceBinding"))
            ||!Creation.reference(artifact).similar(binding.getJSONObject("artifact"))
            ||!Creation.digest(files(artifact)).equals(binding.getString("implementationSha256"))
            ||!child.getString("modelName").equals(binding.getString("modelName"))
            ||!child.getString("modelDigest").equals(binding.getString("modelDigest")))
            throw new IllegalStateException("Original verified implementation or provenance changed");
    }
    static void candidate(JSONObject binding,JSONObject files) throws Exception {
        if(!Creation.digest(files(new JSONObject().put("contents",files))).equals(binding.getString("implementationSha256")))
            throw new IllegalStateException("Reused implementation bytes changed");
    }
    static void inputs(JSONObject goal,JSONObject files) {
        if(!files.getString("inputs/source.csv").equals(goal.getString("sourceCsv"))
            ||!files.getString("inputs/source.json").equals(goal.getJSONObject("sourceData").toString())
            ||files.keySet().stream().anyMatch(k->k.startsWith("inputs/")&&!Set.of("inputs/source.csv","inputs/source.json").contains(k)))
            throw new IllegalStateException("Reused artifact inputs differ from the frozen fresh source");
    }
}
