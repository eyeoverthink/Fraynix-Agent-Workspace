package fraymus.workspace;

import org.json.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Authored input-driven content; independent102 verification remains byte-for-byte unchanged. */
final class DataSummary104 {
    static final String KIND="input-summary-workflow-104-v1";
    static final Set<String> COVERAGE=Set.of("monotonic","reversal","flat","flags","units");
    static boolean supported(String kind){return KIND.equals(kind)||DataSummary102.supported(kind);}
    static boolean summaryKind(String kind){return KIND.equals(kind)||DataSummary102.KIND.equals(kind);}
    static boolean own(JSONObject w){return KIND.equals(w.optString("profile101"));}
    static JSONObject legacy(JSONObject w){return own(w)?WorldFrames.copy(w).put("profile101",DataSummary102.KIND):w;}
    static JSONObject definition(JSONObject in,MeasurementStore store){return own(in)?DataSummary102.definition(legacy(in),store).put("profile101",KIND):DataSummary102.definition(in,store);}
    static String code(String kind){if(!KIND.equals(kind))return DataSummary102.code(kind);try{
        var hash=MessageDigest.getInstance("SHA-256");hash.update(DataSummary102.code(DataSummary102.KIND).getBytes(StandardCharsets.UTF_8));
        try(var in=DataSummary104.class.getResourceAsStream("DataSummary104.class")){hash.update(in.readAllBytes());}
        for(String name:List.of("summary104.html","summary104.js"))hash.update(resource(name).getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash.digest());
    }catch(Exception e){throw new IllegalStateException(e);}}
    static String resource(String name)throws Exception{try(var in=DataSummary104.class.getResourceAsStream("/"+name)){if(in==null)throw new IllegalStateException("Missing summary implementation");return new String(in.readAllBytes(),StandardCharsets.UTF_8);}}
    static JSONObject files()throws Exception{return new JSONObject().put("index.html",resource("summary104.html")).put("summary.js",resource("summary104.js"));}
    static JSONObject criteria(JSONObject dataGoal,JSONObject handoff){return new JSONObject().put("maxGapS",dataGoal.getDouble("maxGapS")).put("speedLimitMps",dataGoal.getDouble("speedLimitMps")).put("skippedSourceRows",handoff.getJSONObject("quality").getInt("skippedSourceRows"));}
    static JSONObject creation(JSONObject w,JSONObject handoff){var out=DataSummary102.creation(legacy(w),handoff);if(own(w))out.put("summary104",new JSONObject().put("schema",KIND).put("codeSha256",code(KIND)));return out;}
    static JSONObject audit(JSONObject w,JSONObject artifact,JSONObject data,JSONObject handoff,JSONObject actual)throws Exception{return DataSummary102.audit(legacy(w),artifact,data,handoff,actual);}
    static JSONObject procedure(JSONObject w){var p=DataSummary102.procedure(legacy(w));if(!own(w))return p;
        p.put("kind",KIND).put("defaults",WorldFrames.copy(p.getJSONObject("defaults")).put("profile101",KIND));
        if(w.has("qualification104"))p.put("qualification104",WorldFrames.copy(w.getJSONObject("qualification104")));
        return p.put("scope",DataCreation101.SCOPE+" Java-owned input-driven summary. Linked varied-data qualification; fresh verification still required. No model-weight training.")
            .put("actions",new JSONArray(List.of("Verify fresh source and quality criteria","Build Java-owned input-driven report","Check every source cell, summary value and SVG point","Recheck varied original qualification evidence before retention")));
    }
    static Set<String> features(JSONObject data,JSONObject handoff){var rows=MotionData.originalRows(data);boolean pos=false,neg=false;for(int i=1;i<rows.length();i++){double dx=rows.getJSONObject(i).getDouble("distanceM")-rows.getJSONObject(i-1).getDouble("distanceM");pos|=dx>0;neg|=dx<0;}var out=new TreeSet<String>();out.add(pos&&neg?"reversal":pos||neg?"monotonic":"flat");var q=handoff.getJSONObject("quality");if(q.getInt("gapFlags")>0&&q.getInt("speedFlags")>0)out.add("flags");var m=data.getJSONObject("mapping");if(!m.getString("timeUnit").equals("s")||!m.getString("distanceUnit").equals("m"))out.add("units");return out;}
}
