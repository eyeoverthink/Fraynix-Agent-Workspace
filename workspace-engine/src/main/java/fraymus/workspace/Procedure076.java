package fraymus.workspace;

import org.json.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Typed task decomposition. Original native controller and verification remain authoritative. */
final class Procedure076 {
    static final List<String> FAMILIES=List.of("direct","reverse","half-force","two-stage","three-stage");
    static final List<String> METHODS=List.of("legacy","simple","geometry","memory");
    static final int BUDGET=86400,WALL_MS=900000,ATTEMPT_MS=90000;
    static JSONObject protocol(){try(var in=Procedure076.class.getResourceAsStream("/procedures076-protocol.json")){return new JSONObject(new String(in.readAllBytes(),StandardCharsets.UTF_8));}catch(Exception e){throw new IllegalStateException(e);}}
    static int stages(String family){if(!FAMILIES.contains(family))throw new IllegalArgumentException("Unknown procedure");return family.equals("three-stage")?3:family.equals("two-stage")?2:1;}
    static JSONObject scene(JSONObject spec){String id=UUID.nameUUIDFromBytes(("procedure076:"+WorldFrames.hash(spec)).getBytes(StandardCharsets.UTF_8)).toString();return WorldFrames.scene(WorldFrames.initial(SpatialScene.parse(spec.getJSONObject("source").toString(),id)));}
    static double yawDelta(double a,double b){double d=(b-a)%360;if(d>180)d-=360;if(d< -180)d+=360;return d;}
    static double wrap(double a){while(a>180)a-=360;while(a< -180)a+=360;return a;}
    static void supported(JSONObject spec){var s=scene(spec);WorldPlan.build(s,WorldPlan.intent(s,spec.getJSONArray("targets"),spec.getDouble("forceLimitN"),spec.getDouble("torqueLimitNm")));}
    static JSONObject plan(JSONObject spec,String family,int step,JSONObject frame){
        int count=stages(family);if(step<0||step>=count)throw new IllegalArgumentException("Invalid procedure step");
        var original=scene(spec);if(!WorldFrames.frame(original).getJSONObject("originalScene").similar(frame.getJSONObject("originalScene")))throw new IllegalStateException("Procedure source changed");
        var targets=new JSONArray();double fraction=(step+1d)/count;
        for(Object raw:spec.getJSONArray("targets")){
            var end=(JSONObject)raw;var body=WorldPlan.sourceObject(original,end.getString("sourceId"));var t=WorldFrames.copy(end);
            if(step+1<count){var p=new JSONArray();for(int k=0;k<3;k++)p.put(body.getJSONArray("position").getDouble(k)+(end.getJSONArray("positionM").getDouble(k)-body.getJSONArray("position").getDouble(k))*fraction);double yaw=WorldPlan.yaw(body.getJSONArray("rotation"));t.put("positionM",p).put("yawDeg",wrap(yaw+yawDelta(yaw,end.getDouble("yawDeg"))*fraction));}targets.put(t);
        }
        if(family.equals("reverse")){var rev=new JSONArray();for(int i=targets.length()-1;i>=0;i--)rev.put(targets.get(i));targets=rev;}
        var current=WorldFrames.scene(frame);double force=spec.getDouble("forceLimitN");if(family.equals("half-force"))force=Math.max(2,force*.5);
        return WorldPlan.build(current,WorldPlan.intent(current,targets,force,spec.getDouble("torqueLimitNm")));
    }
    static JSONObject task(JSONObject spec,JSONObject frame){
        var source=scene(spec);WorldFrames.verify(frame);if(!frame.getJSONObject("originalScene").similar(WorldFrames.frame(source).getJSONObject("originalScene")))throw new IllegalStateException("Task original source changed");
        double position=0,angle=0,speed=0,angular=0,protectedPosition=0,protectedAngle=0;var targets=new HashSet<String>();
        for(Object raw:spec.getJSONArray("targets")){var t=(JSONObject)raw;targets.add(t.getString("sourceId"));var o=WorldPlan.sourceObject(source,t.getString("sourceId"));var b=ContactPhysics.dynamic(frame.getJSONArray("bodies"),o.getString("id"));position=Math.max(position,WorldVerify.distance(t.getJSONArray("positionM"),b.getJSONArray("position")));angle=Math.max(angle,WorldPlan.angle(WorldPlan.quat(t.getDouble("yawDeg")),b.getJSONArray("rotation")));speed=Math.max(speed,WorldVerify.norm(b.getJSONArray("velocity")));angular=Math.max(angular,WorldVerify.norm(b.getJSONArray("angularVelocity")));}
        for(Object raw:source.getJSONArray("objects")){var o=(JSONObject)raw;if(o.getDouble("massKg")<=0||targets.contains(o.getString("sourceId")))continue;var b=ContactPhysics.dynamic(frame.getJSONArray("bodies"),o.getString("id"));protectedPosition=Math.max(protectedPosition,WorldVerify.distance(o.getJSONArray("position"),b.getJSONArray("position")));protectedAngle=Math.max(protectedAngle,WorldPlan.angle(o.getJSONArray("rotation"),b.getJSONArray("rotation")));}
        return new JSONObject().put("passed",position<=.1&&angle<=5&&speed<=.2&&angular<=.2&&protectedPosition<=.05&&protectedAngle<=5).put("positionM",position).put("angleDeg",angle).put("speedMps",speed).put("angularSpeedRadps",angular).put("protectedPositionM",protectedPosition).put("protectedAngleDeg",protectedAngle);
    }
    static boolean protectedTrace(JSONObject spec,JSONObject run){
        var initial=scene(spec);var selected=new HashSet<String>();for(Object raw:spec.getJSONArray("targets"))selected.add(((JSONObject)raw).getString("sourceId"));
        for(Object raw:initial.getJSONArray("objects")){var o=(JSONObject)raw;if(o.getDouble("massKg")<=0||selected.contains(o.getString("sourceId")))continue;for(Object f:run.getJSONArray("samples")){var b=ContactPhysics.dynamic(((JSONObject)f).getJSONArray("bodies"),o.getString("id"));if(WorldVerify.distance(o.getJSONArray("position"),b.getJSONArray("position"))>.05||WorldPlan.angle(o.getJSONArray("rotation"),b.getJSONArray("rotation"))>5)return false;}}return true;
    }
    static double distance(JSONObject older,JSONObject current){
        var a=scene(older);var b=scene(current);double sum=0;int count=0;
        for(Object raw:current.getJSONArray("targets")){var t=(JSONObject)raw;String role=t.getString("sourceId");var oldGoal=MemoryPolicy075.target(older.getJSONArray("targets"),role);var x=WorldPlan.sourceObject(a,role);var y=WorldPlan.sourceObject(b,role);
            sum+=Math.abs(x.getDouble("massKg")-y.getDouble("massKg"))/4+Math.abs(x.getDouble("friction")-y.getDouble("friction"))/2;count+=2;
            for(int k=0;k<3;k++){sum+=Math.abs(x.getJSONArray("size").getDouble(k)-y.getJSONArray("size").getDouble(k));count++;}
            sum+=Math.abs(WorldVerify.distance(x.getJSONArray("position"),oldGoal.getJSONArray("positionM"))-WorldVerify.distance(y.getJSONArray("position"),t.getJSONArray("positionM")))/10;
            sum+=Math.abs(Math.abs(yawDelta(WorldPlan.yaw(x.getJSONArray("rotation")),oldGoal.getDouble("yawDeg")))-Math.abs(yawDelta(WorldPlan.yaw(y.getJSONArray("rotation")),t.getDouble("yawDeg"))))/180;count+=2;
        }
        sum+=Math.abs(older.getDouble("forceLimitN")-current.getDouble("forceLimitN"))/60+Math.abs(older.getDouble("torqueLimitNm")-current.getDouble("torqueLimitNm"))/4;count+=2;
        return sum/count;
    }
    static JSONArray rank(JSONArray bank,JSONObject spec){var a=new ArrayList<JSONObject>();for(Object raw:bank){var b=(JSONObject)raw;try{a.add(new JSONObject().put("id",b.getString("id")).put("family",b.getString("family")).put("trainingRowId",b.getString("trainingRowId")).put("distance",distance(b.getJSONObject("spec"),spec)).put("trainingTicks",b.getInt("trainingTicks")));}catch(IllegalArgumentException incompatible){/* Missing target roles cannot supply a verified procedure. */}}a.sort(Comparator.comparingDouble((JSONObject x)->x.getDouble("distance")).thenComparingInt(x->x.getInt("trainingTicks")).thenComparing(x->x.getString("id")));return new JSONArray(a);}
    static String geometric(JSONObject spec){var scene=scene(spec);double travel=0,yaw=0;for(Object raw:spec.getJSONArray("targets")){var t=(JSONObject)raw;var o=WorldPlan.sourceObject(scene,t.getString("sourceId"));travel=Math.max(travel,WorldVerify.distance(o.getJSONArray("position"),t.getJSONArray("positionM")));yaw=Math.max(yaw,Math.abs(yawDelta(WorldPlan.yaw(o.getJSONArray("rotation")),t.getDouble("yawDeg"))));}return travel>8?"three-stage":yaw>130?"two-stage":"direct";}
    static List<String> order(String method,String preferred){if(!METHODS.contains(method)||!FAMILIES.contains(preferred))throw new IllegalArgumentException("Unknown method/recipe");var a=new ArrayList<>(method.equals("legacy")?FAMILIES.subList(0,3):FAMILIES);if(Set.of("memory","geometry").contains(method)){a.remove(preferred);a.add(0,preferred);}return a;}
    static boolean admitted(int spent,int remaining,double wall){return spent+(long)remaining*WorldPhysics.MAX_TICKS<=BUDGET&&wall+remaining*(double)ATTEMPT_MS<=WALL_MS;}
    static JSONObject evaluation(JSONArray rows){
        var methods=new JSONObject();for(String m:METHODS)methods.put(m,new JSONObject().put("successes",0).put("ticks",0L).put("runs",0).put("wallMillis",0d).put("utility",0d));int count=0,unsupported=0;boolean safe=true;
        for(Object raw:rows){var r=(JSONObject)raw;var results=r.getJSONObject("results");if(r.getString("split").equals("unsupported")){unsupported++;for(String m:METHODS){var a=results.getJSONObject(m);safe&=a.getString("status").equals("refused")&&a.getJSONArray("attempts").isEmpty()&&!a.getBoolean("success");}continue;}count++;
            for(String method:METHODS){var a=results.getJSONObject(method);var m=methods.getJSONObject(method);m.put("successes",m.getInt("successes")+(a.getBoolean("success")?1:0)).put("ticks",m.getLong("ticks")+a.getLong("chargedTicks")).put("runs",m.getInt("runs")+a.getJSONArray("attempts").length()).put("wallMillis",m.getDouble("wallMillis")+a.getDouble("activeWallMillis")).put("utility",m.getDouble("utility")+(a.getBoolean("success")?1:0)-.2*a.getLong("chargedTicks")/BUDGET);}
        }
        var pairs=new JSONObject();boolean qualified=count==8&&unsupported==4&&safe;for(String other:List.of("legacy","simple","geometry")){int saved=0,harmed=0;for(Object raw:rows){var r=(JSONObject)raw;if(!r.getString("split").equals("unseen"))continue;var rs=r.getJSONObject("results");boolean win=rs.getJSONObject("memory").getBoolean("success"),prior=rs.getJSONObject(other).getBoolean("success");if(win&&!prior)saved++;if(!win&&prior)harmed++;}var mem=methods.getJSONObject("memory");var base=methods.getJSONObject(other);double delta=mem.getDouble("utility")-base.getDouble("utility");pairs.put(other,new JSONObject().put("saved",saved).put("harmed",harmed).put("tickDelta",mem.getLong("ticks")-base.getLong("ticks")).put("utilityDelta",delta));if(!other.equals("legacy"))qualified&=saved>harmed&&harmed==0&&delta>0;}
        return new JSONObject().put("methods",methods).put("paired",pairs).put("supported",count).put("unsupported",unsupported).put("unsupportedSafe",safe).put("qualified",qualified).put("automaticPromotion",false);
    }
}
