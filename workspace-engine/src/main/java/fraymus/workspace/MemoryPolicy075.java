package fraymus.workspace;

import org.json.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Frozen task-level comparison. Remembered plans suggest an order, never certify a new result. */
final class MemoryPolicy075 {
    static final List<String> METHODS=List.of("search","simple","memory","cached");
    static final List<String> FAMILIES=List.of("existing-controller-order","reversed-target-order","half-force");
    static final int BUDGET=57600, WALL_MS=600000, ATTEMPT_MS=90000;
    static JSONObject protocol(){
        try(var in=MemoryPolicy075.class.getResourceAsStream("/memory075-protocol.json")){
            return new JSONObject(new String(in.readAllBytes(),StandardCharsets.UTF_8));
        }catch(Exception e){throw new IllegalStateException(e);}
    }
    static JSONObject scene(JSONObject spec){
        String id=UUID.nameUUIDFromBytes(("memory075:"+WorldFrames.hash(spec)).getBytes(StandardCharsets.UTF_8)).toString();
        return WorldFrames.scene(WorldFrames.initial(SpatialScene.parse(spec.getJSONObject("source").toString(),id)));
    }
    static JSONArray candidates(JSONObject spec,JSONObject scene){
        var out=new JSONArray();var seen=new HashSet<String>();
        for(int i=0;i<3;i++){
            var targets=new JSONArray(spec.getJSONArray("targets").toString());
            if(i==1){var rev=new JSONArray();for(int n=targets.length()-1;n>=0;n--)rev.put(targets.get(n));targets=rev;}
            double force=spec.getDouble("forceLimitN");if(i==2)force=Math.max(2,force*.5);
            var plan=WorldPlan.build(scene,WorldPlan.intent(scene,targets,force,spec.getDouble("torqueLimitNm")));
            var motion=new JSONObject().put("order",plan.getJSONArray("orderedTargets")).put("height",plan.getDouble("liftHeightM")).put("force",force).put("torque",spec.getDouble("torqueLimitNm"));
            String hash=WorldFrames.hash(motion),status=!plan.getBoolean("passed")?"rejected":seen.add(hash)?"queued":"duplicate";
            out.put(new JSONObject().put("id",FAMILIES.get(i)).put("ordinal",i).put("status",status).put("plan",plan).put("planSha256",WorldFrames.hash(plan)).put("motionSha256",hash));
        }
        return out;
    }
    static JSONObject target(JSONArray targets,String role){for(Object raw:targets){var t=(JSONObject)raw;if(t.getString("sourceId").equals(role))return t;}throw new IllegalArgumentException("Missing target role "+role);}
    static double distance(JSONObject bundle,JSONObject spec,JSONObject current){
        var e=bundle.getJSONObject("episode");Rehearsal073.rest(e.getJSONObject("run"));
        if(!WorldNativeCheckpoint.identity().equals(e.getString("nativeIdentity")))throw new IllegalArgumentException("Incompatible native identity");
        var old=e.getJSONObject("run").getJSONObject("definition");var oldTargets=e.getJSONObject("run").getJSONObject("plan").getJSONObject("intent").getJSONArray("targets");
        double total=0;int features=0;
        for(Object raw:spec.getJSONArray("targets")){
            var t=(JSONObject)raw;String role=t.getString("sourceId");var a=WorldPlan.sourceObject(old,role);var b=WorldPlan.sourceObject(current,role);
            if(a.getDouble("massKg")<=0||b.getDouble("massKg")<=0)throw new IllegalArgumentException("Target role must remain dynamic");
            total+=Math.abs(a.getDouble("massKg")-b.getDouble("massKg"))/4+Math.abs(a.getDouble("friction")-b.getDouble("friction"))/2;features+=2;
            for(int k=0;k<3;k++){total+=Math.abs(a.getJSONArray("size").getDouble(k)-b.getJSONArray("size").getDouble(k));features++;}
            total+=Math.abs(WorldVerify.distance(a.getJSONArray("position"),target(oldTargets,role).getJSONArray("positionM"))-WorldVerify.distance(b.getJSONArray("position"),t.getJSONArray("positionM")))/10;features++;
        }
        var roles=new HashSet<String>();for(Object o:old.getJSONArray("objects"))roles.add(((JSONObject)o).getString("sourceId"));
        int unmatched=0;for(Object o:current.getJSONArray("objects"))if(!roles.remove(((JSONObject)o).getString("sourceId")))unmatched++;
        return total/features+.25*(unmatched+roles.size());
    }
    static JSONArray rank(List<JSONObject> bank,JSONObject spec,JSONObject scene){
        var ranked=new ArrayList<JSONObject>();
        for(var b:bank){try{double d=distance(b,spec,scene);ranked.add(new JSONObject().put("id",b.getJSONObject("episode").getString("id")).put("distance",d).put("bundleSha256",WorldFrames.hash(b)).put("preferred",b.getString("preferred")));}catch(IllegalArgumentException incompatible){/* An already-verified memory with different target roles is ineligible for this task. */}}
        ranked.sort(Comparator.comparingDouble((JSONObject x)->x.getDouble("distance")).thenComparing(x->x.getString("id")));return new JSONArray(ranked);
    }
    static List<String> order(String method,String preferred){
        if(!METHODS.contains(method)||!FAMILIES.contains(preferred))throw new IllegalArgumentException("Unknown comparison policy");
        var out=new ArrayList<>(FAMILIES);if((method.equals("memory")||method.equals("cached"))){out.remove(preferred);out.add(0,preferred);}return out;
    }
    static boolean admitted(int used,int reserve,double wallMs){return used+WorldPhysics.MAX_TICKS+reserve<=BUDGET&&wallMs+ATTEMPT_MS<=WALL_MS;}
    static JSONObject comparison(JSONObject forecast,JSONObject actual){
        if(forecast.getString("id").equals(actual.getString("id"))||!forecast.getJSONObject("definition").similar(actual.getJSONObject("definition"))||!forecast.getJSONObject("plan").similar(actual.getJSONObject("plan")))throw new IllegalStateException("Fresh execution/source/plan identity differs");
        var c=Rehearsal073.comparison(forecast,actual);return c.put("passed",c.getDouble("sampleCoverage")>=.95&&c.getDouble("maxPositionResidualM")<=.01&&c.getDouble("maxAngleResidualDeg")<=.5);
    }
    static JSONObject evaluation(JSONArray rows){
        var methods=new JSONObject();for(String method:METHODS)methods.put(method,new JSONObject().put("successes",0).put("ticks",0L).put("observedTicks",0L).put("runs",0).put("wallMillis",0d).put("utility",0d));
        int count=0,unsupported=0;var paired=new JSONObject();for(String method:List.of("search","simple"))paired.put(method,new JSONObject().put("saved",0).put("harmed",0).put("tickDelta",0L).put("utilityDelta",0d));
        for(Object raw:rows){var row=(JSONObject)raw;if(row.getString("split").equals("unsupported")){unsupported++;continue;}count++;
            for(String method:METHODS){var a=row.getJSONObject("results").getJSONObject(method);var m=methods.getJSONObject(method);boolean success=a.optBoolean("success");long ticks=a.getLong("chargedTicks");double u=(success?1:0)-.2*ticks/BUDGET;
                m.put("successes",m.getInt("successes")+(success?1:0)).put("ticks",m.getLong("ticks")+ticks).put("observedTicks",m.getLong("observedTicks")+a.getLong("observedTicks")).put("runs",m.getInt("runs")+a.getJSONArray("attempts").length()).put("wallMillis",m.getDouble("wallMillis")+a.getDouble("activeWallMillis")).put("utility",m.getDouble("utility")+u);
            }
            var memory=row.getJSONObject("results").getJSONObject("cached");for(String other:List.of("search","simple")){var a=row.getJSONObject("results").getJSONObject(other);var p=paired.getJSONObject(other);boolean win=memory.optBoolean("success"),prior=a.optBoolean("success");long delta=memory.getLong("chargedTicks")-a.getLong("chargedTicks");p.put("saved",p.getInt("saved")+(win&&!prior?1:0)).put("harmed",p.getInt("harmed")+(!win&&prior?1:0)).put("tickDelta",p.getLong("tickDelta")+delta).put("utilityDelta",p.getDouble("utilityDelta")+(win?1:0)-(prior?1:0)-.2*delta/BUDGET);}
        }
        boolean qualified=count==12;var memory=methods.getJSONObject("cached");
        for(String method:METHODS){var m=methods.getJSONObject(method);m.put("meanTicks",count==0?0:m.getLong("ticks")/(double)count).put("meanUtility",count==0?0:m.getDouble("utility")/count);}
        for(String other:List.of("search","simple")){var a=methods.getJSONObject(other);var p=paired.getJSONObject(other);p.put("meanTickDelta",count==0?0:p.getLong("tickDelta")/(double)count).put("meanUtilityDelta",count==0?0:p.getDouble("utilityDelta")/count);qualified&=memory.getInt("successes")>=a.getInt("successes")&&memory.getLong("ticks")<=.9*a.getLong("ticks")&&p.getDouble("utilityDelta")>0;}
        var cold=methods.getJSONObject("memory");boolean cacheQualified=count==12&&memory.getInt("successes")>=cold.getInt("successes")&&memory.getLong("ticks")<=.9*cold.getLong("ticks")&&memory.getDouble("utility")>cold.getDouble("utility");
        return new JSONObject().put("cacheQualified",cacheQualified).put("cachedMinusColdTicks",memory.getLong("ticks")-cold.getLong("ticks")).put("supported",count).put("unsupported",unsupported).put("methods",methods).put("memoryMinus",paired).put("qualified",qualified).put("automaticPromotion",false);
    }
}
