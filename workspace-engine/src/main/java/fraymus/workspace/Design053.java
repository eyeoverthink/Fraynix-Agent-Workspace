package fraymus.workspace;

import org.json.*;
import java.util.*;

/** Declared assembly alternatives and evidence ranking; the native solver remains the authority. */
final class Design053 {
    static final String VERSION="assembly-design-053-v1";
    static final String SCOPE="Three declared force-limit profiles for the same synthetic bridge target. Independent native trials, unchanged support and pose checks, then a fresh actual execution. No global optimum, new geometry, material-strength or physical-robot claim.";
    private Design053() {}

    static JSONObject policy() {
        return new JSONObject().put("version",VERSION).put("scope",SCOPE)
            .put("fractions",new JSONArray(new double[]{.25,.65,1}))
            .put("minimumForceN",2).put("maximumCandidates",3)
            .put("feasibility","Original native pose/wrench/release gates AND original bridge support/stability gates")
            .put("ranking",new JSONArray(List.of("minimum measured effortNs","minimum nativeTicks","declaration ordinal")))
            .put("recovery","Retain every failure; no feasible candidate means no live-world action. Reuse reruns trials against the fresh source.");
    }

    static JSONArray proposals(JSONObject scene,JSONObject base) {
        WorldPlan.verify(scene,base);
        var intent=base.getJSONObject("intent");
        if(!intent.has("construction051"))throw new IllegalArgumentException("Assembly design requires declared construction stock and targets");
        double maximum=intent.getDouble("forceLimitN"),torque=intent.getDouble("torqueLimitNm"),weight=0;
        for(String id:Construction051.ROLES)weight=Math.max(weight,WorldPlan.sourceObject(scene,id).getDouble("massKg")*scene.getDouble("gravityMps2"));
        var out=new JSONArray();double[] fractions={.25,.65,1};String[] ids={"quarter-force","balanced-force","maximum-force"};
        for(int i=0;i<fractions.length;i++){
            double force=Math.max(2,maximum*fractions[i]);
            var proposed=Construction051.intent(scene,intent.getJSONObject("construction051"),force,torque);
            var hypothesis=new JSONObject().put("origin","Declared Java assembly design policy").put("forceLimitN",force)
                .put("maximumAdmittedForceN",maximum).put("torqueLimitNm",torque).put("largestStaticWeightN",weight)
                .put("staticLiftMarginN",force-weight).put("status","Unverified until native execution")
                .put("objection",force<=weight?"This force ceiling cannot balance the heaviest part's static weight; run the trial and retain the failure.":"Positive static lift margin does not establish successful motion or released bridge support.");
            out.put(new JSONObject().put("id",ids[i]).put("plan",WorldPlan.build(scene,proposed)).put("hypothesis053",hypothesis));
        }
        return out;
    }

    static JSONObject structure(JSONObject run) {
        if(!run.has("final")||run.getJSONArray("samples").isEmpty())return new JSONObject().put("passed",false).put("reason","No final native observation");
        return Construction051.verifyStructure(run);
    }

    static JSONObject choose(JSONArray candidates) {
        JSONObject best=null;
        for(Object raw:candidates){
            var c=(JSONObject)raw;var m=c.optJSONObject("metrics",new JSONObject());
            if(!c.getString("status").equals("completed")||!m.optBoolean("passed")||!c.optJSONObject("structure053",new JSONObject()).optBoolean("passed"))continue;
            if(best==null||compare(c,best)<0)best=c;
        }
        return best;
    }
    private static int compare(JSONObject a,JSONObject b){
        var x=a.getJSONObject("metrics");var y=b.getJSONObject("metrics");
        int n=Double.compare(x.getDouble("effortNs"),y.getDouble("effortNs"));
        if(n==0)n=Integer.compare(x.getInt("nativeTicks"),y.getInt("nativeTicks"));
        return n==0?Integer.compare(a.getInt("ordinal"),b.getInt("ordinal")):n;
    }
}
