package fraymus.workspace;

import org.json.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Independent same-stock bearings. Hypotheses cannot certify their own outcome. */
final class SupportRepair060 {
    static final String LEGACY_059="fb44c7ff862790e1d16fb5d1a48497f5ab2e2a001511c0a1767d6f31ddefa95e";
    private SupportRepair060(){}
    static JSONObject protocol(){try(var in=SupportRepair060.class.getResourceAsStream("/construction060-protocol.json")){return new JSONObject(new String(in.readAllBytes(),StandardCharsets.UTF_8));}catch(Exception e){throw new IllegalStateException(e);}}
    static boolean enabled(JSONObject s){return s.optBoolean("independentSupports060");}
    static boolean comparesAll(JSONObject s){return enabled(s)||SupportDesign059.enabled(s);}
    static boolean requested(String text){String s=text.toLowerCase(Locale.ROOT);return s.contains("independent support")||s.contains("independentsupports060")||s.contains("compact stock repair");}
    static void options(JSONObject input,JSONObject out){
        if(!input.has("supportA060")&&!input.has("supportB060"))return;
        if(!input.has("supportA060")||!input.has("supportB060")||input.has("supportSpanM")||input.has("supportOffsetM"))throw new IllegalArgumentException("Provide both independent supports without legacy span or pair offset");
        for(String key:List.of("supportA060","supportB060")){
            var p=input.getJSONObject(key);SpatialScene.keys(p,"x","z","yawDeg");
            SpatialScene.number(p,"x",-2,2);SpatialScene.number(p,"z",-1,1);SpatialScene.number(p,"yawDeg",0,90);
            if(p.getDouble("yawDeg")!=0&&p.getDouble("yawDeg")!=90)throw new IllegalArgumentException("Independent support yaw must be 0 or 90 degrees");
            out.put(key,WorldFrames.copy(p));
        }
    }
    static JSONArray targets(JSONObject scene,JSONObject options,double height){
        var beam=WorldPlan.sourceObject(scene,"beam").getJSONArray("size");var result=new JSONArray();double previousEdge=0;
        for(int i=0;i<2;i++){
            String id=i==0?"support-a":"support-b";var p=options.getJSONObject(i==0?"supportA060":"supportB060");var size=WorldPlan.sourceObject(scene,id).getJSONArray("size");
            boolean turn=p.getDouble("yawDeg")==90;double hx=size.getDouble(turn?2:0)/2,hz=size.getDouble(turn?0:2)/2,x=p.getDouble("x"),z=p.getDouble("z");
            if(Math.abs(x)+hx>beam.getDouble(0)/2-.08+1e-9||Math.abs(z)+beam.getDouble(2)/2>hz+1e-9)throw new IllegalArgumentException("Independent bearing must retain beam-end margins and cover beam width");
            if(i==1&&x-hx-previousEdge<.20-1e-9)throw new IllegalArgumentException("Independent bearings need a 0.20 m clear gap in A/B order");
            previousEdge=x+hx;
            result.put(new JSONObject().put("sourceId",id).put("positionM",new JSONArray(new double[]{options.getDouble("centerX")+x,height/2,options.getDouble("centerZ")+z})).put("yawDeg",p.getDouble("yawDeg")));
        }
        return result;
    }
    private static JSONObject pose(double x,double yaw){return new JSONObject().put("x",x).put("z",0).put("yawDeg",yaw);}
    static JSONArray proposals(JSONObject scene,JSONObject base){
        if(!Construction054.is(scene)||Construction055.is(scene))throw new IllegalArgumentException("Independent repair requires declared compact or wide rectangular stock");
        var intent=base.getJSONObject("intent");var config=intent.getJSONObject("construction051");
        if(config.has("supportA060")||config.optDouble("supportOffsetM",0)!=0)throw new IllegalArgumentException("Repair comparison starts with symmetric supports");
        double span=config.optDouble("supportSpanM",Construction054.config(scene).getDouble("spanM")),half=WorldPlan.sourceObject(scene,"beam").getJSONArray("size").getDouble(0)/2;
        double a=half-.08-WorldPlan.sourceObject(scene,"support-a").getJSONArray("size").getDouble(2)/2,b=half-.08-WorldPlan.sourceObject(scene,"support-b").getJSONArray("size").getDouble(2)/2;
        var out=new JSONArray();String[] names={"maximum-force","right-bearing","paired-bearing"};
        for(int i=0;i<3;i++){
            var c=WorldFrames.copy(config);double ax=i==2?-a:-span/2,bx=i>0?b:span/2;
            if(i>0){c.remove("supportSpanM");c.remove("supportOffsetM");c.put("supportA060",pose(ax,i==2?90:0)).put("supportB060",pose(bx,90));}
            var plan=WorldPlan.build(scene,Construction051.intent(scene,c,intent.getDouble("forceLimitN"),intent.getDouble("torqueLimitNm")));
            out.put(new JSONObject().put("id",names[i]).put("plan",plan).put("hypothesis053",new JSONObject().put("forceLimitN",intent.getDouble("forceLimitN")).put("supportA060",pose(ax,i==2?90:0)).put("supportB060",pose(bx,i>0?90:0)).put("additionalSupportTravelM",Math.abs(ax+span/2)+Math.abs(bx-span/2)).put("additionalSupportRotationDeg",i*90).put("objection","Rotated independent bearings may redistribute contact but can still slide or fail. Same stock and limits; only repeated native outcomes certify the repair.")));
        }
        return out;
    }
    /** Rebuilt from verified design measurements; never accepted from a chat claim. */
    static JSONObject witness(JSONObject series,JSONObject run){
        var failures=new JSONArray();String beam=WorldPlan.sourceObject(run.getJSONObject("definition"),"beam").getString("id");
        for(Object raw:series.getJSONArray("results")){
            var r=(JSONObject)raw;var m=r.getJSONObject("metrics");if(m.getBoolean("stable")&&r.getJSONObject("repeatMetrics").getBoolean("stable"))continue;
            var gates=new JSONArray();if(m.getDouble("maxBeamDisplacementM")>.05)gates.put("beam displacement > 0.05 m");if(m.getDouble("maxBeamTiltDeg")>5)gates.put("beam tilt > 5 degrees");if(m.getDouble("maxSettledSpeedMps")>.08)gates.put("settled speed > 0.08 m/s");if(m.getInt("supportedSamples")<20)gates.put("both support contacts missing in final samples");if(!m.isNull("payloadRetained")&&!m.getBoolean("payloadRetained"))gates.put("payload retention failed");if(!r.getJSONObject("repeatMetrics").getBoolean("stable"))gates.put("repeat failed; see repeat metrics");
            var f=new JSONObject().put("caseId",r.getString("caseId")).put("firstAttemptId",r.getString("firstAttemptId")).put("repeatAttemptId",r.getString("repeatAttemptId")).put("failedGates",gates).put("metrics",WorldFrames.copy(m)).put("repeatMetrics",WorldFrames.copy(r.getJSONObject("repeatMetrics")));
            var frames=r.getJSONArray("frames");if(frames.length()>0){var origin=ContactPhysics.dynamic(frames.getJSONObject(0).getJSONArray("bodies"),beam).getJSONArray("position");double max=-1;var delta=new JSONArray();int tick=0;
                for(Object v:frames){var frame=(JSONObject)v;var p=ContactPhysics.dynamic(frame.getJSONArray("bodies"),beam).getJSONArray("position");double d=Load055.distance(origin,p);if(d>max){max=d;delta=new JSONArray(new double[]{p.getDouble(0)-origin.getDouble(0),p.getDouble(1)-origin.getDouble(1),p.getDouble(2)-origin.getDouble(2)});tick=frame.getInt("tick");}}
                f.put("sampledTranslation",new JSONObject().put("fromTick",frames.getJSONObject(0).getInt("tick")).put("toTick",tick).put("deltaM",delta).put("distanceM",max).put("scope","Largest sampled displacement relative to first sampled frame; not the all-tick maximum or a causal proof"));
            }failures.put(f);
        }
        return new JSONObject().put("version","measured-repair-witness-060-v1").put("sourceRunSha256",WorldFrames.hash(run)).put("designOriginalSha256",series.getString("originalSha256")).put("baselineQualifies",series.getBoolean("qualifies")).put("failures",failures).put("scope","Observed baseline failures admit fixed hypotheses; independent repeated verification determines success");
    }
}
