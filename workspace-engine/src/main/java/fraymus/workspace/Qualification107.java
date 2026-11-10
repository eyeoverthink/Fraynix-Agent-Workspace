package fraymus.workspace;

import org.json.*;
import java.security.MessageDigest;
import java.util.*;

/** Explicit maintenance decision and original proof references; never a substitute for verification. */
final class Qualification107 {
    static boolean requested(String text){return Qualification105.requested(text)&&ChatPolicy044.has(text,"\\b(?:refresh|rerun|re-run)\\b");}
    static boolean take(JSONObject args){
        if(args.has("refreshQualification107")&&!(args.get("refreshQualification107") instanceof Boolean))throw new IllegalArgumentException("Qualification refresh must be boolean");
        boolean refresh=args.optBoolean("refreshQualification107");args.remove("refreshQualification107");return refresh;
    }
    private static final JSONObject DEPENDENCIES=identity();
    static JSONObject dependencies(){return WorldFrames.copy(DEPENDENCIES);}
    private static JSONObject identity(){try{
        var hashes=new JSONObject();for(Class<?> type:List.of(Qualification107.class,WorkflowRunner.class,GoalRunner.class,MotionData.class,Creation.class,DataCreation101.class,DataSummary102.class,DataSummary104.class,Implementation103.class,GoalArchive111.class,Qualification111.class))try(var in=type.getResourceAsStream(type.getSimpleName()+".class")){hashes.put(type.getSimpleName(),HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(in.readAllBytes())));}
        return hashes.put("history111",Qualification111.code()).put("suite105",Qualification105.code()).put("summary104",DataSummary104.code(DataSummary104.KIND));
    }catch(Exception e){throw new IllegalStateException(e);}}
    static JSONObject manifest(boolean refresh,JSONArray baseline){return new JSONObject().put("schema","qualification-maintenance-107-v1").put("mode",refresh?"refresh":"recheck").put("dependencies",dependencies()).put("baseline",baseline);}
    static JSONArray verify(JSONObject q){
        var m=q.getJSONObject("maintenance107");SpatialScene.keys(m,"schema","mode","dependencies","baseline");
        if(!m.getString("schema").equals("qualification-maintenance-107-v1")||!Set.of("refresh","recheck").contains(m.getString("mode"))||!q.getString("maintenanceSha256").equals(WorldFrames.hash(m))||!dependencies().similar(m.getJSONObject("dependencies")))throw new IllegalStateException("Qualification maintenance intent or dependency identity changed");
        var seen=new HashSet<String>();for(Object raw:m.getJSONArray("baseline")){var b=(JSONObject)raw;SpatialScene.keys(b,"name","workflowId","proof");Qualification105.example(b.getString("name"));if(!seen.add(b.getString("name")))throw new IllegalStateException("Duplicate original qualification reference");}
        return m.getJSONArray("baseline");
    }
}
