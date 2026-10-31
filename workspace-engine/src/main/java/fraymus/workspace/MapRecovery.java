package fraymus.workspace;

import org.json.*;

/** Check the actual initialized native body against its committed trace boundary. */
final class MapRecovery {
    static JSONObject audit(JSONObject run) {
        int count=0,previous=-1;double positionError=0,velocityError=0,orientationError=0;
        for(Object raw:run.getJSONArray("events")) {
            var event=(JSONObject)raw;if(!event.getString("kind").equals("resume"))continue;count++;
            int tick=event.getInt("tick");var expected=event.getJSONObject("restored").getJSONObject("sample");
            var actual=event.getJSONObject("actualInitialization");
            if(tick<previous||actual.getInt("tick")!=tick||expected.getInt("tick")!=tick)throw new IllegalStateException("Recovery tick continuity failed");previous=tick;
            if(tick>0){JSONObject original=null;for(Object sample:run.getJSONArray("samples"))if(((JSONObject)sample).getInt("tick")==tick)original=(JSONObject)sample;
                if(original==null||!original.similar(expected))throw new IllegalStateException("Recovery boundary differs from original committed trace");}
            for(String key:new String[]{"position","velocity","angularVelocity"}){
                double error=ObserverGeometry.norm(ObserverGeometry.sub(SpatialScene.vector(expected.getJSONArray(key),300),SpatialScene.vector(actual.getJSONArray(key),300)));
                if(key.equals("position"))positionError=Math.max(positionError,error);else velocityError=Math.max(velocityError,error);
                if(error>1e-6)throw new IllegalStateException("Native restoration changed "+key);
            }
            double direct=0,opposite=0;for(int k=0;k<4;k++){double a=expected.getJSONArray("rotation").getDouble(k),b=actual.getJSONArray("rotation").getDouble(k);direct+=Math.pow(a-b,2);opposite+=Math.pow(a+b,2);}
            orientationError=Math.max(orientationError,Math.sqrt(Math.min(direct,opposite)));
            if(orientationError>1e-6||actual.getInt("pathIndex")!=expected.getInt("pathIndex"))throw new IllegalStateException("Native controller/orientation restoration failed");
            for(String key:new String[]{"effortNs","actuatorWorkJ","positiveWorkJ","brakingWorkJ","dragWorkJ","gravityWorkJ"})
                if(actual.getDouble(key)!=expected.getDouble(key))throw new IllegalStateException("Recovery reset a work/controller counter");
        }
        if(count!=run.optInt("resumes"))throw new IllegalStateException("Recovery count differs from actual initialization receipts");
        return new JSONObject().put("passed",true).put("resumes",count).put("maxPositionErrorM",positionError)
            .put("maxVelocityError",velocityError).put("maxQuaternionError",orientationError)
            .put("contactCache","Rebuilt by Bullet; subsequent trajectory is not promised bitwise identical");
    }
}
