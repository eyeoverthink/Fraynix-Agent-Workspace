package fraymus.workspace;

import org.json.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/** Availability is separate from immutable procedure evidence. Stored in the owner's atomic checkpoint. */
final class ProcedureLifecycle127 {
    static final String TYPE="procedure-lifecycle127", SCHEMA="procedure-lifecycle-127-v1";
    static final String ID=UUID.nameUUIDFromBytes(SCHEMA.getBytes(StandardCharsets.UTF_8)).toString();
    static final int ACTIVE=32, RETIRED=512, CHANGES=32;
    private ProcedureLifecycle127(){}
    static JSONObject empty(){return new JSONObject().put("id",ID).put("schema",SCHEMA).put("records",new JSONObject());}
    static JSONObject record(JSONObject state,JSONObject p){return state.getJSONObject("records").optJSONObject(p.getString("id"));}
    static boolean retired(JSONObject state,JSONObject p){var r=record(state,p);return r!=null&&r.getString("status").equals("retired");}
    static void validate(JSONObject state,List<JSONObject> procedures){
        SpatialScene.keys(state,"id","schema","records");if(state.toString().getBytes(StandardCharsets.UTF_8).length>8_000_000)throw new IllegalStateException("Lifecycle evidence byte budget reached");
        if(!ID.equals(state.getString("id"))||!SCHEMA.equals(state.getString("schema")))throw new IllegalStateException("Procedure lifecycle identity changed");
        var byId=new HashMap<String,JSONObject>();for(var p:procedures)byId.put(p.getString("id"),p);
        var records=state.getJSONObject("records");if(records.length()>ACTIVE+RETIRED)throw new IllegalStateException("Lifecycle record budget exceeded");
        for(String id:records.keySet()){
            var p=byId.get(id);var r=records.getJSONObject(id);SpatialScene.keys(r,"procedureSha256","status","history");
            if(p==null||!WorldFrames.hash(p).equals(r.getString("procedureSha256")))throw new IllegalStateException("Managed procedure content changed; preserve original evidence");
            String previous="GENESIS",status="active";var history=r.getJSONArray("history");
            if(history.isEmpty()||history.length()>CHANGES)throw new IllegalStateException("Invalid lifecycle history size");
            for(Object raw:history){var e=(JSONObject)raw;SpatialScene.keys(e,"at","action","reason","previous","procedureSha256","evidenceSha256","evidence","sha256");
                Instant.parse(e.getString("at"));if(e.getString("reason").isBlank()||e.getString("reason").length()>240||!WorldFrames.hash(e.getJSONObject("evidence")).equals(e.getString("evidenceSha256")))throw new IllegalStateException("Lifecycle inspection evidence changed");String action=e.getString("action");
                if(!Set.of("retire","reactivate").contains(action)||action.equals("retire")!=status.equals("active")||!e.getString("previous").equals(previous)||!e.getString("procedureSha256").equals(r.getString("procedureSha256")))throw new IllegalStateException("Lifecycle history chain changed");
                if(action.equals("reactivate")&&!e.getJSONObject("evidence").optBoolean("passed"))throw new IllegalStateException("Reactivation lacks passing inspection evidence");String hash=e.getString("sha256");var payload=WorldFrames.copy(e);payload.remove("sha256");if(!WorldFrames.hash(payload).equals(hash))throw new IllegalStateException("Lifecycle event checksum changed");
                previous=hash;status=action.equals("retire")?"retired":"active";
            }
            if(!status.equals(r.getString("status")))throw new IllegalStateException("Lifecycle availability differs from its history");
        }
        long retired=procedures.stream().filter(p->retired(state,p)).count();
        if(retired>RETIRED||procedures.size()-retired>ACTIVE)throw new IllegalStateException("Procedure capacity exceeded; originals retained");
    }
    static String token(JSONObject state,List<JSONObject> procedures){return WorldFrames.hash(new JSONObject().put("lifecycle",state).put("procedures",new JSONArray(procedures)));}
    static JSONObject change(JSONObject state,List<JSONObject> procedures,JSONObject p,JSONObject input,JSONObject evidence){
        SpatialScene.keys(input,"id","action","expectedHash","reason");validate(state,procedures);
        if(!input.getString("id").equals(p.getString("id"))||!token(state,procedures).equals(input.getString("expectedHash")))throw new IllegalStateException("Procedure bank changed; inspect again");
        String action=input.getString("action"),reason=input.getString("reason").strip();if(reason.isEmpty()||reason.length()>240)throw new IllegalArgumentException("Give a reason using 1–240 characters");
        if(!Set.of("retire","reactivate").contains(action)||action.equals("retire")==retired(state,p))throw new IllegalStateException("Procedure is already in that state or action is unsupported");
        if(p.optString("publication101").equals("pending"))throw new IllegalStateException("Pending procedures cannot change availability");
        long retired=procedures.stream().filter(x->retired(state,x)).count();
        if(action.equals("retire")&&retired>=RETIRED||action.equals("reactivate")&&procedures.size()-retired>=ACTIVE)throw new IllegalStateException("Procedure capacity reached; active limit remains 32");
        if(action.equals("reactivate")&&!evidence.getBoolean("passed"))throw new IllegalStateException("Original evidence or current compatibility refused reactivation: "+evidence.optString("reason"));
        var next=WorldFrames.copy(state);var rows=next.getJSONObject("records");var r=rows.optJSONObject(p.getString("id"));
        if(r==null){r=new JSONObject().put("procedureSha256",WorldFrames.hash(p)).put("status","active").put("history",new JSONArray());rows.put(p.getString("id"),r);}
        var history=r.getJSONArray("history");if(history.length()>=CHANGES)throw new IllegalStateException("Lifecycle history limit reached; all 32 changes remain retained");
        var e=new JSONObject().put("at",Instant.now().toString()).put("action",action).put("reason",reason).put("previous",history.isEmpty()?"GENESIS":history.getJSONObject(history.length()-1).getString("sha256")).put("procedureSha256",WorldFrames.hash(p)).put("evidence",WorldFrames.copy(evidence)).put("evidenceSha256",WorldFrames.hash(evidence));e.put("sha256",WorldFrames.hash(e));history.put(e);r.put("status",action.equals("retire")?"retired":"active");
        if(next.toString().getBytes(StandardCharsets.UTF_8).length>8_000_000)throw new IllegalStateException("Lifecycle evidence byte budget reached");validate(next,procedures);return next;
    }
    static void compareContract(JSONObject actual,JSONObject expected){
        Object rawVersion=actual.get("version");if(!(rawVersion instanceof Number n)||n.doubleValue()<1||n.doubleValue()>8||n.doubleValue()!=Math.rint(n.doubleValue()))throw new IllegalStateException("Saved procedure version is outside the original eight-version contract");
        String id=UUID.nameUUIDFromBytes(("procedure:"+actual.getString("name")+":"+n.intValue()).getBytes(StandardCharsets.UTF_8)).toString();if(!id.equals(actual.getString("id")))throw new IllegalStateException("Saved procedure identity differs from its exact name and version");
        var a=WorldFrames.copy(actual);var b=WorldFrames.copy(expected);
        for(String key:List.of("id","version","verifiedAt")){a.remove(key);b.remove(key);}
        if(!a.similar(b))throw new IllegalStateException("Saved procedure contract differs from its verified source or current implementation");
    }
}
