package fraymus.workspace;

import org.json.*;
import java.util.*;

/** Reconstruct outcome, controlled force ledger and recovery from native observations. */
final class PushVerify {
    static double distance(JSONArray a,JSONArray b){var x=SpatialScene.vector(a,100);var y=SpatialScene.vector(b,100);return Math.hypot(x[0]-y[0],x[2]-y[2]);}
    static JSONObject verify(JSONObject run){
        var scene=run.getJSONObject("definition");var plan=run.getJSONObject("plan");var intent=plan.getJSONObject("intent");String crateId=intent.getString("objectId"),pusherId=intent.getString("pusherId");
        var planner=new PushPlan(scene,crateId,pusherId);planner.verify(plan);
        if(!planner.intent(intent.getJSONArray("displacementM"),intent.getDouble("initialForceN"),intent.getDouble("maxForceN")).similar(intent))throw new IllegalStateException("Push target/force intent changed");
        var parsed=SpatialScene.parse(scene.getString("sourceText"),scene.getString("id"));for(String key:parsed.keySet())if(!new JSONObject().put("value",parsed.get(key)).similar(new JSONObject().put("value",scene.get(key))))throw new IllegalStateException("Original pushing source changed");
        var pp=PushPlan.position(planner.pusher);var cp=PushPlan.position(planner.crate);double effort=0,work=0,maxForce=0,travel=0;int tick=0,contactTicks=0,completedActions=0,jams=0,recoveries=0,resumes=0;boolean contacted=false;
        for(Object raw:run.getJSONArray("samples")){
            var sample=(JSONObject)raw;
            for(Object value:sample.getJSONArray("integration")){
                var row=(JSONArray)value;if(row.length()!=10||row.getInt(0)!=++tick||!row.getString(1).equals(pusherId))throw new IllegalStateException("Missing tick or unauthorized native actuator body");
                for(int k=2;k<10;k++)if(!(row.get(k) instanceof Number n)||!Double.isFinite(n.doubleValue()))throw new IllegalStateException("Nonfinite pushing trace");
                double force=Math.hypot(row.getDouble(2),row.getDouble(3));maxForce=Math.max(maxForce,force);if(force>intent.getDouble("maxForceN")+.0001)throw new IllegalStateException("Force exceeds original pushing cap");
                pp[0]+=row.getDouble(4);pp[2]+=row.getDouble(5);cp[0]+=row.getDouble(6);cp[2]+=row.getDouble(7);travel+=Math.hypot(row.getDouble(6),row.getDouble(7));
                if(row.getInt(8)>0){contacted=true;contactTicks++;}if(!contacted&&Math.hypot(cp[0]-PushPlan.position(planner.crate)[0],cp[2]-PushPlan.position(planner.crate)[2])>.02)throw new IllegalStateException("Crate moved horizontally before measured pusher contact");
                effort+=force*BulletRamp.DT;work+=row.getDouble(2)*row.getDouble(4)+row.getDouble(3)*row.getDouble(5);
            }
            if(sample.getInt("tick")!=tick||tick>PushPhysics.MAX_TICKS)throw new IllegalStateException("Pushing sample order/budget changed");
            var pb=PushPhysics.dynamic(sample.getJSONArray("bodies"),pusherId);var cb=PushPhysics.dynamic(sample.getJSONArray("bodies"),crateId);
            for(var body:List.of(pb,cb)){
                if(body.getInt("initializationPoseWrites")!=1)throw new IllegalStateException("Body pose written outside initialization");
                SpatialScene.vector(body.getJSONArray("velocity"),100);SpatialScene.vector(body.getJSONArray("angularVelocity"),300);
                var position=SpatialScene.vector(body.getJSONArray("position"),30);var source=body==pb?planner.pusher:planner.crate;
                if(Math.abs(position[1]-PushPlan.size(source)[1]/2)>.15)throw new IllegalStateException("Pushing body left supported planar domain");
                if(position[0]<planner.minX-.55||position[0]>planner.maxX+.55||position[2]<planner.minZ-.55||position[2]>planner.maxZ+.55)throw new IllegalStateException("Body left declared pushing arena");
            }
            if(Math.hypot(pp[0]-pb.getJSONArray("position").getDouble(0),pp[2]-pb.getJSONArray("position").getDouble(2))>1e-6||Math.hypot(cp[0]-cb.getJSONArray("position").getDouble(0),cp[2]-cb.getJSONArray("position").getDouble(2))>1e-6)throw new IllegalStateException("Native displacements do not reconstruct both body positions");
            if(Math.abs(effort-sample.getDouble("effortNs"))>1e-7*Math.max(1,effort)||Math.abs(work-sample.getDouble("pusherWorkJ"))>1e-7*Math.max(1,Math.abs(work)))throw new IllegalStateException("Pusher work/effort counter differs from tick ledger");
        }
        double maxRestorePosition=0,maxRestoreVelocity=0,maxRestoreQuaternion=0;int lastEventTick=-1;
        for(Object raw:run.getJSONArray("events")){
            var event=(JSONObject)raw;int at=event.getInt("tick");if(at<lastEventTick||at>tick)throw new IllegalStateException("Push event order changed");lastEventTick=at;
            switch(event.getString("kind")){
                case "action-complete"->{int index=event.getInt("action");if(index!=completedActions++)throw new IllegalStateException("Push action skipped or repeated");var action=plan.getJSONArray("actions").getJSONObject(index);var body=PushPhysics.dynamic(event.getJSONArray("bodies"),action.getString("kind").equals("push")?crateId:pusherId);double error=distance(body.getJSONArray("position"),action.getJSONArray("target"));if(error>(action.getString("kind").equals("push")?.035:.07)+1e-5||ObserverGeometry.norm(SpatialScene.vector(body.getJSONArray("velocity"),100))>.1801)throw new IllegalStateException("Completed action did not reach its actual target");}
                case "jam"->{jams++;if(event.getInt("contacts")<1||at-event.getInt("windowTick")<240||event.getDouble("progressM")>=.02||Math.abs(distance(event.getJSONArray("windowPosition"),event.getJSONArray("cratePosition"))-event.getDouble("progressM"))>1e-8)throw new IllegalStateException("Jam lacks measured no-progress/contact evidence");}
                case "recovery-start"->{recoveries++;if(recoveries>PushPhysics.MAX_RECOVERIES||recoveries>jams)throw new IllegalStateException("Recovery budget/evidence changed");}
                case "recovery-complete"->{if(event.getDouble("forceLimitN")>intent.getDouble("maxForceN"))throw new IllegalStateException("Recovery changed authorized force cap");}
                case "resume"->{
                    resumes++;var expected=event.getJSONObject("expected");var actual=event.getJSONObject("actualInitialization");if(expected.getInt("tick")!=at||actual.getInt("tick")!=at||!expected.getJSONObject("controller").similar(actual.getJSONObject("controller")))throw new IllegalStateException("Controller changed during native recovery");
                    JSONObject original=at==0?run.getJSONObject("initialState"):null;for(Object s:run.getJSONArray("samples"))if(((JSONObject)s).getInt("tick")==at)original=(JSONObject)s;
                    if(original==null||!original.similar(expected))throw new IllegalStateException("Native recovery boundary differs from committed trace");
                    for(String id:List.of(pusherId,crateId)){
                        var e=PushPhysics.dynamic(expected.getJSONArray("bodies"),id);var a=PushPhysics.dynamic(actual.getJSONArray("bodies"),id);
                        for(String key:List.of("position","velocity","angularVelocity")){double error=ObserverGeometry.norm(ObserverGeometry.sub(SpatialScene.vector(e.getJSONArray(key),300),SpatialScene.vector(a.getJSONArray(key),300)));if(key.equals("position"))maxRestorePosition=Math.max(maxRestorePosition,error);else maxRestoreVelocity=Math.max(maxRestoreVelocity,error);if(error>1e-6)throw new IllegalStateException("Native recovery changed "+key);}
                        double direct=0,opposite=0;for(int k=0;k<4;k++){double x=e.getJSONArray("rotation").getDouble(k),y=a.getJSONArray("rotation").getDouble(k);direct+=Math.pow(x-y,2);opposite+=Math.pow(x+y,2);}maxRestoreQuaternion=Math.max(maxRestoreQuaternion,Math.sqrt(Math.min(direct,opposite)));if(maxRestoreQuaternion>1e-6)throw new IllegalStateException("Native recovery orientation changed");
                    }
                    if(expected.getDouble("effortNs")!=actual.getDouble("effortNs")||expected.getDouble("pusherWorkJ")!=actual.getDouble("pusherWorkJ"))throw new IllegalStateException("Recovery reset accumulated actuation");
                }
                case "aligned","settled","failed"->{}
                default->throw new IllegalStateException("Unknown native pushing event");
            }
        }
        var last=run.getJSONObject("final");if(last.getInt("tick")!=tick||run.getJSONArray("samples").isEmpty()||!last.similar(run.getJSONArray("samples").getJSONObject(run.getJSONArray("samples").length()-1)))throw new IllegalStateException("Final native body differs from recorded boundary");
        var lastCrate=PushPhysics.dynamic(last.getJSONArray("bodies"),crateId);double error=distance(lastCrate.getJSONArray("position"),intent.getJSONArray("targetM")),speed=ObserverGeometry.norm(SpatialScene.vector(lastCrate.getJSONArray("velocity"),100));
        if(resumes!=run.optInt("resumes")||recoveries!=last.getJSONObject("controller").getInt("recoveries"))throw new IllegalStateException("Recovery event/controller count differs");
        boolean passed=run.getString("status").equals("completed")&&last.getJSONObject("controller").getString("phase").equals("finished")&&error<=.1&&speed<=.2&&contactTicks>0&&completedActions==plan.getJSONArray("actions").length();
        return new JSONObject().put("passed",passed).put("endpointErrorM",error).put("terminalSpeedMps",speed).put("contactTicks",contactTicks).put("completedActions",completedActions).put("plannedActions",plan.getJSONArray("actions").length()).put("plannedPushes",plan.getJSONArray("pushes").length()).put("expandedStates",plan.getInt("expanded")).put("recoveries",recoveries).put("jams",jams).put("resumes",resumes).put("maxForceN",maxForce).put("effortNs",effort).put("pusherWorkJ",work).put("crateTravelM",travel).put("nativeTicks",tick).put("timeS",tick*BulletRamp.DT).put("maxRestorePositionErrorM",maxRestorePosition).put("maxRestoreVelocityError",maxRestoreVelocity).put("maxRestoreQuaternionError",maxRestoreQuaternion).put("physicalAxes",3).put("nativePhysics",true).put("scope",PushPlan.SCOPE);
    }
}
