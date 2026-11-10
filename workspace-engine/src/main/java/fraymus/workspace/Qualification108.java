package fraymus.workspace;

import org.json.*;
import java.util.*;
import java.security.MessageDigest;

/** Explicit orchestration-only transition. Original outcomes are still verified by WorkflowRunner. */
final class Qualification108 {
    static final String KEY="transition108", SCHEMA="qualification-transition-108-v1";
    static boolean requested(String text){return Qualification105.requested(text)&&ChatPolicy044.has(text,"\\b(upgrade|transition)\\b");}
    static boolean take(JSONObject args){Object v=args.remove("transitionQualification108");if(v==null)return false;if(!(v instanceof Boolean))throw new IllegalArgumentException("Qualification transition must be boolean");return (Boolean)v;}
    static String code(){try(var in=Qualification108.class.getResourceAsStream("Qualification108.class")){return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(in.readAllBytes()));}catch(Exception e){throw new IllegalStateException(e);}}
    static JSONArray differences(JSONObject before){
        var now=Qualification107.dependencies();if(!before.keySet().equals(now.keySet()))throw new IllegalStateException("Unsupported qualification dependency set; preserve originals and review");
        var changes=new JSONArray();for(String k:new TreeSet<>(now.keySet()))if(!now.getString(k).equals(before.getString(k))){
            if(!Set.of("WorkflowRunner","suite105").contains(k))throw new IllegalStateException("Unsupported qualification dependency change: "+k);
            if(!before.getString(k).matches("[0-9a-f]{64}"))throw new IllegalStateException("Invalid original dependency identity");
            changes.put(new JSONObject().put("name",k).put("from",before.getString(k)).put("to",now.getString(k)));
        }
        if(changes.length()!=2)throw new IllegalStateException("Transition requires a changed orchestration and suite identity");return changes;
    }
    static JSONObject oldCase(String name,String oldCode){
        if(!oldCode.matches("[0-9a-f]{64}"))throw new IllegalStateException("Invalid original suite identity");
        var c=Qualification105.example(name);var in=c.getJSONObject("input");
        in.put("title","Qualification105 · "+name+" · "+oldCode.substring(0,8)).put("filename","qualification105-"+oldCode.substring(0,12)+"-"+name+".csv")
          .put("sourceNote","Java-authored qualification105 software test: "+name+". No real-world measurement or training claim. Suite "+oldCode);
        return c;
    }
    static JSONObject oldBinding(String name,String code){return new JSONObject().put("suiteCode",code).put("name",name).put("caseSha256",WorldFrames.hash(oldCase(name,code)));}
    static JSONObject original(JSONObject parent){
        if(!parent.getString("status").equals("completed")||!parent.getJSONObject("audit").getBoolean("passed"))throw new IllegalStateException("Transition requires a completed original qualification");
        var q=parent.getJSONObject("qualification105");var m=q.getJSONObject("maintenance107");SpatialScene.keys(m,"schema","mode","dependencies","baseline");
        if(!m.getString("schema").equals("qualification-maintenance-107-v1")||!Set.of("recheck","refresh").contains(m.getString("mode"))||!q.getString("maintenanceSha256").equals(WorldFrames.hash(m))||!q.getString("codeSha256").equals(m.getJSONObject("dependencies").getString("suite105"))||!q.getString("definitionSha256").equals(WorldFrames.hash(parent.getJSONObject("definition"))))throw new IllegalStateException("Original qualification intent or identity changed");
        differences(m.getJSONObject("dependencies"));return m;
    }
    static JSONObject manifest(JSONObject parent,JSONArray proofs){
        var old=original(parent);return new JSONObject().put("schema",SCHEMA).put("codeSha256",code()).put("fromWorkflowId",parent.getString("id")).put("fromRecordSha256",WorldFrames.hash(parent))
            .put("fromDependencies",old.getJSONObject("dependencies")).put("toDependencies",Qualification107.dependencies()).put("changes",differences(old.getJSONObject("dependencies")))
            .put("originalProofs",proofs).put("policy","Verify originals; execute all five fresh cases; never relabel old evidence");
    }
    static void verify(JSONObject q){
        var m=q.getJSONObject(KEY);SpatialScene.keys(m,"schema","codeSha256","fromWorkflowId","fromRecordSha256","fromDependencies","toDependencies","changes","originalProofs","policy");
        if(!SCHEMA.equals(m.getString("schema"))||!code().equals(m.getString("codeSha256"))||!q.getString("transitionSha256").equals(WorldFrames.hash(m))||!Qualification107.dependencies().similar(m.getJSONObject("toDependencies"))||!differences(m.getJSONObject("fromDependencies")).similar(m.getJSONArray("changes")))throw new IllegalStateException("Qualification transition intent or runtime changed");
        if(!q.getJSONObject("maintenance107").getString("mode").equals("refresh"))throw new IllegalStateException("Transition must run every fresh example");
    }
}
