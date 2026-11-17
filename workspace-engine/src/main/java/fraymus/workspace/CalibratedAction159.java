package fraymus.workspace;

import org.json.*;
import java.util.*;

/** Derive bounded simulation-marker intentions from recorded reference coordinates. */
final class CalibratedAction159 {
    static final String KIND="calibrated-recorded-action-159-v1";
    static boolean own(JSONObject w){return KIND.equals(w.optString("profile159"));}
    static boolean requested(String text){return ChatPolicy044.has(text,"calibrated recorded landmarks|calibrated world action");}
    static String code(){var b=new StringBuilder(KIND);for(Class<?> c:List.of(CalibratedAction159.class,CalibratedPlane159.class,CalibratedReport159.class,WorkflowRunner.class,GoalRunner.class,Domain044.class,ChatPolicy044.class))try(var in=c.getResourceAsStream(c.getSimpleName()+".class")){b.append(PerceptionStore.hash(in.readAllBytes()));}catch(Exception e){throw new IllegalStateException(e);}return MeasurementStore.hash(b.toString());}

    static JSONObject definition(JSONObject input){
        SpatialScene.keys(input,"profile159","registrationId","worldId","expectedRevision","expectedHash","markers","returnMarker","pixelRadius","annotationBudgetM","maxRadiusM","maxMoveM","executionToleranceM","maxTotalErrorM","forceLimitN","torqueLimitNm","reviewBeforeAction159","request","title","modelName","saveSkillName");
        var d=WorldFrames.copy(input);
        if(!KIND.equals(d.getString("profile159")))throw new IllegalArgumentException("Select the calibrated recorded action workflow");
        UUID.fromString(d.getString("registrationId"));UUID.fromString(d.getString("worldId"));
        if(!(d.get("expectedRevision") instanceof Number)||d.getDouble("expectedRevision")!=d.getInt("expectedRevision")||d.getInt("expectedRevision")<0||!d.getString("expectedHash").matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Explicit observed destination revision required");
        for(String k:List.of("request","title","modelName"))Creation.text(d,k,k.equals("request")?4000:140);
        double[][] limits={{.5,8},{.001,.02},{.001,.03},{.01,1},{.00001,.1},{.01,.13},{2,20},{0,1}};
        String[] names={"pixelRadius","annotationBudgetM","maxRadiusM","maxMoveM","executionToleranceM","maxTotalErrorM","forceLimitN","torqueLimitNm"};
        double[] defaults={2,.003,.015,.75,.05,.065,6,.5};
        for(int i=0;i<names.length;i++){if(!d.has(names[i]))d.put(names[i],defaults[i]);SpatialScene.number(d,names[i],limits[i][0],limits[i][1]);}
        var markers=d.getJSONArray("markers");if(markers.length()<1||markers.length()>2||markers.length()==1&&!d.has("returnMarker")||markers.length()==2&&d.has("returnMarker"))throw new IllegalArgumentException("Select one recorded marker plus an explicit return marker, or two separated recorded markers");
        var roles=new HashSet<String>();var points=new HashSet<String>();
        for(Object raw:markers){var m=(JSONObject)raw;SpatialScene.keys(m,"sourceId","landmark");if(!roles.add(Creation.text(m,"sourceId",80))||!points.add(Creation.text(m,"landmark",80)))throw new IllegalArgumentException("Distinct markers and landmarks required");}
        if(d.has("returnMarker")&&!roles.add(Creation.text(d,"returnMarker",80)))throw new IllegalArgumentException("Return marker must be a different body");
        String name=d.optString("saveSkillName","");if(!name.isEmpty()&&!name.matches("[a-z][a-z0-9-]{2,55}"))throw new IllegalArgumentException("Choose a procedure name of3..56 characters");d.put("saveSkillName",name);
        if(d.has("reviewBeforeAction159")&&!(d.get("reviewBeforeAction159") instanceof Boolean))throw new IllegalArgumentException("Review must be boolean");return d;
    }

    static JSONObject decision(JSONObject source,JSONObject scene,JSONObject d){
        WorldFrames.verifyScene(scene);var targets=new JSONArray();var bounds=new JSONArray();double maxRadius=0;
        for(Object raw:d.getJSONArray("markers")){
            var selected=(JSONObject)raw;var o=WorldPlan.sourceObject(scene,selected.getString("sourceId"));
            if(o.getDouble("massKg")<=0)throw new IllegalArgumentException("Choose actual dynamic simulation markers");
            var bound=CalibratedPlane159.bounds(source,selected.getString("landmark"),d.getDouble("pixelRadius"),d.getDouble("annotationBudgetM"));
            double radius=bound.getDouble("radiusM");maxRadius=Math.max(maxRadius,radius);
            if(radius>d.getDouble("maxRadiusM")||radius+d.getDouble("executionToleranceM")>d.getDouble("maxTotalErrorM"))throw new IllegalArgumentException("Coordinate or combined error budget is too wide for this action");
            var position=new JSONArray(bound.getJSONArray("centerM").toString());position.put(1,o.getJSONArray("size").getDouble(1)/2);
            if(WorldVerify.distance(position,o.getJSONArray("position"))>d.getDouble("maxMoveM"))throw new IllegalArgumentException("Recorded target exceeds bounded marker movement");
            targets.put(new JSONObject().put("sourceId",o.getString("sourceId")).put("positionM",position).put("yawDeg",0));
            bounds.put(bound.put("sourceId",o.getString("sourceId")).put("objectId",o.getString("id")));
        }
        if(d.has("returnMarker")){var keep=WorldPlan.sourceObject(scene,d.getString("returnMarker"));if(keep.getDouble("massKg")<=0)throw new IllegalArgumentException("Return marker must be dynamic");targets.put(new JSONObject().put("sourceId",keep.getString("sourceId")).put("positionM",new JSONArray(keep.getJSONArray("position").toString())).put("yawDeg",WorldPlan.yaw(keep.getJSONArray("rotation"))));}
        var plan=WorldPlan.build(scene,WorldPlan.intent(scene,targets,d.getDouble("forceLimitN"),d.getDouble("torqueLimitNm")));
        if(!plan.getBoolean("passed"))throw new IllegalArgumentException("Native route refused: "+plan.getString("reason"));
        // Check the planned corridor enlarged for both moving and previously placed coordinate errors.
        var positions=new LinkedHashMap<String,double[]>();JSONObject floor=WorldPlan.sourceObject(scene,"floor");
        for(Object raw:scene.getJSONArray("objects")){var o=(JSONObject)raw;if(o.getDouble("massKg")>0)positions.put(o.getString("sourceId"),SpatialScene.vector(o.getJSONArray("position"),20));}
        for(Object raw:plan.getJSONArray("orderedTargets")){
            var t=(JSONObject)raw;String role=t.getString("sourceId");var o=WorldPlan.sourceObject(scene,role);
            double radius=WorldPlan.radius(o)+2*maxRadius;double[] to=SpatialScene.vector(t.getJSONArray("positionM"),5),from=positions.get(role);
            var fp=floor.getJSONArray("position");var fs=floor.getJSONArray("size");
            if(Math.abs(to[0]-fp.getDouble(0))+radius>fs.getDouble(0)/2-.05||Math.abs(to[2]-fp.getDouble(2))+radius>fs.getDouble(2)/2-.05)throw new IllegalArgumentException("Coordinate interval reaches beyond the supported floor");
            double lift=plan.getDouble("liftHeightM");double[][] path={from,{from[0],lift,from[2]},{to[0],lift,to[2]},to};
            for(int leg=0;leg<3;leg++)if(!WorldPlan.clear(scene,role,positions,path[leg],path[leg+1],radius))throw new IllegalArgumentException("Uncertainty-expanded marker corridor is obstructed");
            positions.put(role,to);
        }
        var policy=new JSONObject().put("bounds",bounds).put("executionToleranceM",d.getDouble("executionToleranceM")).put("maxTotalErrorM",d.getDouble("maxTotalErrorM"));
        var args=new JSONObject().put("calibratedOutcome159",policy).put("worldId",d.getString("worldId")).put("expectedRevision",d.getInt("expectedRevision")).put("expectedHash",d.getString("expectedHash"))
            .put("targets",targets).put("forceLimitN",d.getDouble("forceLimitN")).put("torqueLimitNm",d.getDouble("torqueLimitNm")).put("request",d.getString("request"));
        return new JSONObject().put("sourceSha256",WorldFrames.hash(source)).put("destinationSceneSha256",WorldFrames.hash(scene)).put("bounds",bounds).put("plan",plan).put("childInput",args)
            .put("executionToleranceM",d.getDouble("executionToleranceM")).put("maxTotalErrorM",d.getDouble("maxTotalErrorM"))
            .put("hardwareAuthorized",false).put("liveObjectPose",false).put("meaning","Move explicitly selected virtual markers to recorded plane references. An explicit return marker, when selected, is lifted and returned to its authored starting pose under the original controller; it is not a second measured reference. Marker height comes from simulation geometry; it is not observed object height.");
    }

    static JSONObject outcome(JSONObject decision,JSONObject child){
        if(!child.getString("status").equals("completed")||!child.has("worldCommit"))throw new IllegalStateException("Native action has no verified committed outcome");
        var rows=checkBodies(decision,child.getJSONObject("worldCommit").getJSONObject("frame").getJSONArray("bodies"));
        return new JSONObject().put("passed",true).put("rows",rows).put("goalId",child.getString("id")).put("commitHash",child.getJSONObject("worldCommit").getString("hash"))
            .put("scope","Actual verified simulation outcome against recorded conditional planar bounds. No physical actuation or live pose claim.");
    }
    static JSONArray checkBodies(JSONObject decision,JSONArray bodies){
        var rows=new JSONArray();
        for(Object raw:decision.getJSONArray("bounds")){
            var b=(JSONObject)raw;var actual=ContactPhysics.dynamic(bodies,b.getString("objectId"));var center=b.getJSONArray("centerM");var p=actual.getJSONArray("position");
            double error=Math.hypot(p.getDouble(0)-center.getDouble(0),p.getDouble(2)-center.getDouble(2));
            if(error>decision.getDouble("executionToleranceM")||error+b.getDouble("radiusM")>decision.getDouble("maxTotalErrorM"))throw new IllegalStateException("Actual native marker exceeded the frozen position/error budget; native outcome retained");
            rows.put(new JSONObject().put("sourceId",b.getString("sourceId")).put("landmark",b.getString("name")).put("actualPositionM",new JSONArray(p.toString())).put("planarErrorM",error).put("coordinateRadiusM",b.getDouble("radiusM")).put("combinedErrorBudgetM",error+b.getDouble("radiusM")));
        }
        return rows;
    }
    static void verifyBeforeCommit(JSONObject goal,JSONObject run){
        boolean calibrated="calibrated-world159".equals(goal.optString("workflowStage"));
        if(!calibrated){if(goal.has("calibratedOutcome159"))throw new IllegalStateException("Foreign calibrated action authority");return;}
        if(!goal.has("calibratedOutcome159")||!goal.getJSONObject("calibratedOutcome159").similar(goal.getJSONObject("workflowInput").getJSONObject("calibratedOutcome159")))throw new IllegalStateException("Frozen calibrated outcome policy changed");
        checkBodies(goal.getJSONObject("calibratedOutcome159"),run.getJSONObject("final").getJSONArray("bodies"));
    }

}
