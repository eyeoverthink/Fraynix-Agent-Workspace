package fraymus.workspace;

import org.json.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Fixed same-stock asymmetric hypotheses. Reserved measurements never select the design. */
final class SupportDesign059 {
    static final String LEGACY_058="933109af7e76c1cc8dbf32f556956505bbf762ee9b8e8fcca599420c1059b8f5";
    private SupportDesign059() {}
    static JSONObject protocol(){try(var in=SupportDesign059.class.getResourceAsStream("/construction059-protocol.json")){return new JSONObject(new String(in.readAllBytes(),StandardCharsets.UTF_8));}catch(Exception e){throw new IllegalStateException(e);}}
    static boolean requested(String text){String s=text.toLowerCase(Locale.ROOT);return s.contains("asymmetric")||s.contains("asymmetricsupports059");}
    static boolean enabled(JSONObject study){return study.optBoolean("asymmetricSupports059");}
    static JSONArray proposals(JSONObject scene,JSONObject base){
        if(!Construction054.is(scene))throw new IllegalArgumentException("Asymmetric supports require declared box stock");
        var intent=base.getJSONObject("intent");var config=intent.getJSONObject("construction051");
        if(config.optDouble("supportOffsetM",0)!=0)throw new IllegalArgumentException("The comparison must start with symmetric supports");
        double span=config.optDouble("supportSpanM",Construction054.config(scene).getDouble("spanM"));
        double maximum=WorldPlan.sourceObject(scene,"beam").getJSONArray("size").getDouble(0)-WorldPlan.sourceObject(scene,"support-a").getJSONArray("size").getDouble(0)-.16;
        double shift=Math.max(0,Math.min(protocol().getDouble("maxOffsetM"),(maximum-span)/2));
        String[] names={"maximum-force","supports-left","supports-right"};double[] offsets={0,-shift,shift};var out=new JSONArray();
        for(int i=0;i<3;i++){
            var candidate=WorldFrames.copy(config);if(i>0)candidate.put("supportOffsetM",offsets[i]);
            var plan=WorldPlan.build(scene,Construction051.intent(scene,candidate,intent.getDouble("forceLimitN"),intent.getDouble("torqueLimitNm")));
            out.put(new JSONObject().put("id",names[i]).put("plan",plan).put("hypothesis053",new JSONObject().put("forceLimitN",intent.getDouble("forceLimitN")).put("supportSpanM",span).put("supportOffsetM",offsets[i]).put("additionalSupportTravelM",2*Math.abs(offsets[i])).put("objection","Same span and stock; asymmetric support may help one side and harm the other. Measure all admitted candidates and preserve failures.")));
        }
        return out;
    }
}
