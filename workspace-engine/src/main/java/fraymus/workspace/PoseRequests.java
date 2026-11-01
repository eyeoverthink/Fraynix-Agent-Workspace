package fraymus.workspace;
import org.json.*;
/** User-facing admission narrows the new capability to its declared floor/body domain. */
final class PoseRequests {
 static void source(JSONObject scene){var floor=PosePlan.sourceObject(scene,"floor");var q=floor.getJSONArray("rotation");if(Math.abs(q.getDouble(0))+Math.abs(q.getDouble(1))+Math.abs(q.getDouble(2))>1e-6)throw new IllegalArgumentException("Pose tasks require an axis-aligned flat floor; box yaw remains supported");for(Object raw:scene.getJSONArray("objects")){var o=(JSONObject)raw;if(o.getDouble("massKg")>0)for(Object n:o.getJSONArray("size"))if(((Number)n).doubleValue()<.2)throw new IllegalArgumentException("Pose task dynamic box sides must be0.2..1m");}}
 static JSONObject plan(JSONObject scene,JSONArray targets,double force,double torque){source(scene);return PosePlan.build(scene,PosePlan.intent(scene,targets,force,torque));}
}
