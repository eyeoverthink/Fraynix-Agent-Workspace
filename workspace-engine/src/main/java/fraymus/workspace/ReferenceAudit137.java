package fraymus.workspace;
import java.util.*;
import org.json.*;

/** Checks frozen bank identity and consecutive recovery evidence; target labels score outcomes separately. */
final class ReferenceAudit137 {
    static JSONObject check(JSONObject run,String body){
        var memory=run.getJSONObject("referenceMemory137");String hash=WorldFrames.hash(memory);
        boolean enabled=ObservationMethods129.policy(run).equals("verified-memory-137");
        var keys=new HashSet<String>();keys.add("initial-seed");
        if(enabled)for(Object raw:memory.getJSONArray("examples")){var e=(JSONObject)raw;keys.add(e.getString("imageSha256")+":"+e.getInt("ordinal"));}
        JSONObject previous=null;int ordinal=0,recoveries=0,imported=0;
        for(Object raw:run.getJSONArray("trace")){
            var frame=(JSONObject)raw;JSONObject tracker=null;
            for(Object item:frame.getJSONArray("objects")){var row=(JSONObject)item;if(row.getString("bodyId").equals(body))tracker=row.getJSONObject("tracker");}
            if(tracker==null)throw new IllegalStateException("Missing reference tracker row");
            var meta=tracker.getJSONObject("reference137");var recovery=meta.getJSONObject("recovery");
            if(!meta.getString("schema").equals("verified-reference-trace-137-v1")||meta.getInt("ordinal")!=++ordinal
                ||!meta.getString("memorySha256").equals(hash)||meta.getBoolean("importedEnabled")!=enabled
                ||meta.getInt("automaticAdmissions")!=0||meta.getInt("bankSize")!=keys.size())throw new IllegalStateException("Frozen reference bank changed");
            if(meta.getBoolean("incumbentAccepted")&&(recovery.getBoolean("attempted")||tracker.isNull("box")))throw new IllegalStateException("Incumbent continuity evidence changed");
            if(recovery.has("sourceKey")){
                if(!keys.contains(recovery.getString("sourceKey"))||recovery.getDouble("score")<.88||!recovery.getBoolean("attempted"))throw new IllegalStateException("Unsupported reference recovery");
                PixelTracker.Box.from(recovery.getJSONArray("box"));
            }
            if(recovery.getBoolean("accepted")){
                if(meta.getBoolean("incumbentAccepted")||previous==null||previous.getBoolean("accepted")
                    ||!recovery.getString("sourceKey").equals(previous.optString("sourceKey"))
                    ||PixelTracker.iou(PixelTracker.Box.from(previous.getJSONArray("box")),PixelTracker.Box.from(recovery.getJSONArray("box")))<.55
                    ||!tracker.getJSONArray("box").similar(recovery.getJSONArray("box")))throw new IllegalStateException("Recovery lacks consecutive unique-match evidence");
                recoveries++;if(!recovery.getString("sourceKey").equals("initial-seed"))imported++;
            }
            previous=meta.getBoolean("incumbentAccepted")?null:recovery;
        }
        return new JSONObject().put("memorySha256",hash).put("samples",ordinal).put("recoveries",recoveries).put("importedRecoveries",imported)
            .put("automaticAdmissions",0).put("scope","Frozen provenance and consecutive recovery checks. Independent target/visibility labels in the enclosing audit determine correctness; internal confidence is not identity proof.");
    }
}
