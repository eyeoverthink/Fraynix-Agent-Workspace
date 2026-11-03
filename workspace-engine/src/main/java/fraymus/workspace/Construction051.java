package fraymus.workspace;

import org.json.*;
import java.util.*;

/** Declared bridge kit, conservative overhead planning and independent support checks. */
final class Construction051 {
    static final String KIND = "gripper-construction-051-v1";
    static final String SCOPE = "Three-part native bridge; bounded virtual force/torque gripper, gravity and contacts. No welded joints, real robot or material-strength claim.";
    static final String LEGACY_050 = "2d071b7114fb07967502425978b51eafaafdf2b7b372caf706977c1b0ba057f0";
    static final List<String> ROLES = List.of("support-a", "support-b", "beam");

    static JSONObject options(JSONObject input) {
        SpatialScene.keys(input,"centerX","centerZ");
        if(input.has("centerX"))SpatialScene.number(input,"centerX",-1,1);
        if(input.has("centerZ"))SpatialScene.number(input,"centerZ",-.5,.8);
        var out=new JSONObject().put("centerX",input.optDouble("centerX",0)).put("centerZ",input.optDouble("centerZ",0));
        SpatialScene.number(out,"centerX",-1,1);SpatialScene.number(out,"centerZ",-.5,.8);return out;
    }
    static String identity() {
        try(var in=Construction051.class.getResourceAsStream("Construction051.class")){return MapDefinition.sha(in.readAllBytes());}
        catch(Exception e){throw new IllegalStateException(e);}
    }
    static JSONObject kit() {
        var objects=new JSONArray();
        objects.put(part("floor",new double[]{0,-.1,0},new double[]{8,.2,8},0,0));
        objects.put(part("support-a",new double[]{-2,.3,-1.5},new double[]{.4,.6,.6},1.2,0));
        objects.put(part("support-b",new double[]{0,.3,-1.5},new double[]{.4,.6,.6},1.2,0));
        objects.put(part("beam",new double[]{2,.15,-1.5},new double[]{2,.3,.4},.8,90));
        return new JSONObject().put("format","fraynix.spatial-scene").put("version",1).put("frame","right-handed-y-up").put("title","Declared bridge kit · three loose parts").put("units","m").put("gravityMps2",9.81).put("objects",objects)
            .put("source",new JSONObject().put("kind","synthetic").put("note","Edition 051 declared-scale loose construction stock; not physical capture").put("physicalProperties","declared"))
            .put("scaleReference",new JSONObject().put("a",new JSONArray(new int[]{0,0,0})).put("b",new JSONArray(new int[]{1,0,0})).put("knownLengthM",1).put("toleranceM",0).put("basis","declared-test"));
    }
    private static JSONObject part(String id,double[] p,double[] size,double mass,double yaw) {
        return new JSONObject().put("sourceId",id).put("label",id).put("shape","box").put("position",new JSONArray(p)).put("size",new JSONArray(size))
            .put("rotation",WorldPlan.quat(yaw)).put("massKg",mass).put("friction",.8).put("restitution",0);
    }
    static void stock(JSONObject scene) {
        WorldFrames.verifyScene(scene);
        if(scene.getJSONArray("objects").length()!=4)throw new IllegalArgumentException("Bridge construction requires the declared floor and exactly three retained kit parts");
        for(Object raw:kit().getJSONArray("objects")) {
            var expected=(JSONObject)raw;var actual=WorldPlan.sourceObject(scene,expected.getString("sourceId"));
            for(String key:List.of("size","massKg","friction","restitution"))
                if(!new JSONObject().put("v",expected.get(key)).similar(new JSONObject().put("v",actual.get(key))))throw new IllegalArgumentException("Bridge stock geometry/material changed: "+expected.getString("sourceId"));
            var p=actual.getJSONArray("position");var q=actual.getJSONArray("rotation");
            if(Math.abs(q.getDouble(0))+Math.abs(q.getDouble(2))>.043)throw new IllegalArgumentException("Inspect tilted stock before assembly");
            if(expected.getString("sourceId").equals("floor")&&(!p.similar(expected.getJSONArray("position"))||!q.similar(expected.getJSONArray("rotation"))))throw new IllegalArgumentException("Declared flat kit floor required");
            if(actual.getDouble("massKg")>0 && (WorldVerify.norm(actual.getJSONArray("velocity"))>.2||WorldVerify.norm(actual.getJSONArray("angularVelocity"))>.2))throw new IllegalArgumentException("Stock must be settled before planning");
        }
    }
    static JSONObject intent(JSONObject scene,JSONObject requested,double force,double torque) {
        stock(scene);var o=options(requested);RampDefinition.range(force,2,60,"gripper force N");RampDefinition.range(torque,0,4,"gripper torque Nm");
        var targets=new JSONArray();var beam=WorldPlan.sourceObject(scene,"beam");
        // Removing the beam first protects supports when rebuilding a retained structure.
        if(beam.getJSONArray("position").getDouble(1)>.3)targets.put(target("beam",2,.15,-1.5));
        double x=o.getDouble("centerX"),z=o.getDouble("centerZ");
        targets.put(target("support-a",x-.65,.3,z)).put(target("support-b",x+.65,.3,z)).put(target("beam",x,.77,z));
        return new JSONObject().put("binding",SpatialStore.binding(scene)).put("targets",targets).put("forceLimitN",force).put("torqueLimitNm",torque)
            .put("actuator","bounded virtual force/torque gripper").put("construction051",o).put("constructionCodeSha256",identity());
    }
    private static JSONObject target(String id,double x,double y,double z){return new JSONObject().put("sourceId",id).put("positionM",new JSONArray(new double[]{x,y,z})).put("yawDeg",0);}
    static JSONObject plan(JSONObject scene,JSONObject requested) {
        var expected=intent(scene,requested.getJSONObject("construction051"),requested.getDouble("forceLimitN"),requested.getDouble("torqueLimitNm"));
        if(!expected.similar(requested))throw new IllegalStateException("Construction source, design, code or stock binding changed");
        var positions=new LinkedHashMap<String,JSONObject>();double top=0,radius=0;
        for(String id:ROLES){var o=WorldFrames.copy(WorldPlan.sourceObject(scene,id));positions.put(id,o);top=Math.max(top,o.getJSONArray("position").getDouble(1)+o.getJSONArray("size").getDouble(1)/2);radius=Math.max(radius,WorldPlan.radius(o));}
        double lift=top+radius+.3;var ordered=new JSONArray();boolean clear=true;String reason="Stock and overhead routes checked; supports precede beam; release must earn actual support contacts";
        for(Object raw:expected.getJSONArray("targets")){
            var t=(JSONObject)raw;String id=t.getString("sourceId");var body=positions.get(id);var to=t.getJSONArray("positionM");
            var next=WorldFrames.copy(body).put("position",to).put("rotation",WorldPlan.quat(0));
            if(!columnClear(body,positions,id)||!columnClear(next,positions,id)){clear=false;reason="A vertical pickup or lowering corridor is occupied; no native effect admitted";break;}
            for(int k:new int[]{0,2})if(Math.abs(to.getDouble(k))+next.getJSONArray("size").getDouble(k)/2>3.9){clear=false;reason="Construction exceeds the retained floor";}
            ordered.put(WorldFrames.copy(t).put("objectId",body.getString("id")).put("rotation",WorldPlan.quat(0)));positions.put(id,next);
        }
        return new JSONObject().put("version",KIND).put("scope",SCOPE).put("intent",expected).put("liftHeightM",lift).put("orderedTargets",ordered).put("passed",clear).put("reason",reason);
    }
    private static boolean columnClear(JSONObject selected,Map<String,JSONObject> bodies,String id) {
        double[] a=halfBounds(selected);var p=selected.getJSONArray("position");
        for(var e:bodies.entrySet())if(!e.getKey().equals(id)){
            var b=e.getValue();var q=b.getJSONArray("position");double[] h=halfBounds(b);
            if(Math.abs(p.getDouble(0)-q.getDouble(0))<a[0]+h[0]+.025&&Math.abs(p.getDouble(2)-q.getDouble(2))<a[2]+h[2]+.025&&p.getDouble(1)-a[1]<q.getDouble(1)+h[1]-.005)return false;
        }return true;
    }
    private static double[] halfBounds(JSONObject o){double yaw=Math.toRadians(WorldPlan.yaw(o.getJSONArray("rotation"))),c=Math.abs(Math.cos(yaw)),s=Math.abs(Math.sin(yaw));var b=o.getJSONArray("size");return new double[]{(c*b.getDouble(0)+s*b.getDouble(2))/2,b.getDouble(1)/2,(s*b.getDouble(0)+c*b.getDouble(2))/2};}

    static JSONObject verifyStructure(JSONObject run) {
        var scene=run.getJSONObject("definition");var plan=run.getJSONObject("plan");var intent=plan.getJSONObject("intent");
        if(!plan(scene,intent).similar(plan))throw new IllegalStateException("Construction plan changed");
        var last=run.getJSONObject("final");String beam=WorldPlan.sourceObject(scene,"beam").getString("id");
        var supports=new HashSet<String>();for(String s:List.of("support-a","support-b"))supports.add(WorldPlan.sourceObject(scene,s).getString("id"));
        var contacted=new HashSet<String>();int samples=0;double maxSpeed=0;int end=last.getInt("tick");
        for(Object raw:run.getJSONArray("samples")){var sample=(JSONObject)raw;if(sample.getInt("tick")<end-84)continue;samples++;
            for(Object body:sample.getJSONArray("bodies"))maxSpeed=Math.max(maxSpeed,WorldVerify.norm(((JSONObject)body).getJSONArray("velocity")));
            for(Object contact:sample.getJSONArray("contacts")){var c=(JSONObject)contact;String a=c.getString("a"),b=c.getString("b");if(a.equals(beam)&&supports.contains(b))contacted.add(b);if(b.equals(beam)&&supports.contains(a))contacted.add(a);}
        }
        var beamState=ContactPhysics.dynamic(last.getJSONArray("bodies"),beam);double height=beamState.getJSONArray("position").getDouble(1);boolean supported=contacted.size()==2&&height>.70&&height<.80;
        boolean passed=supported&&samples>=6&&maxSpeed<.08&&last.getJSONObject("controller").getString("grippingId").isEmpty();
        return new JSONObject().put("passed",passed).put("supportContacts",contacted.size()).put("beamCenterHeightM",height).put("settledSamples",samples).put("maxSettledSpeedMps",maxSpeed).put("released",last.getJSONObject("controller").getString("grippingId").isEmpty()).put("design",intent.getJSONObject("construction051")).put("scope",SCOPE);
    }
}
