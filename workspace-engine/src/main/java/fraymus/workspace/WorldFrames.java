package fraymus.workspace;

import org.json.*;
import java.util.*;

/** Complete kinematic frames. Original authored geometry and stable body IDs never change. */
final class WorldFrames {
    static String hash(JSONObject value){return MeasurementStore.hash(MapCheckpoint.canonical(value));}
    static JSONObject copy(JSONObject value){return new JSONObject(value.toString());}
    static JSONObject initial(JSONObject source){
        PoseRequests.source(source);
        var bodies=new JSONArray();
        for(Object raw:source.getJSONArray("objects")){
            var o=(JSONObject)raw;if(o.getDouble("massKg")<=0)continue;
            bodies.put(new JSONObject().put("id",o.getString("id")).put("position",o.getJSONArray("position")).put("rotation",o.getJSONArray("rotation"))
                .put("velocity",new JSONArray(new int[]{0,0,0})).put("angularVelocity",new JSONArray(new int[]{0,0,0})));
        }
        var frame=new JSONObject().put("format","fraynix.world-frame-035-v1").put("originalScene",copy(source)).put("bodies",bodies).put("elapsedTicks",0);
        verify(frame);return frame;
    }
    static void verify(JSONObject frame){
        SpatialScene.keys(frame,"format","originalScene","bodies","elapsedTicks");
        if(!frame.getString("format").equals("fraynix.world-frame-035-v1")||frame.getLong("elapsedTicks")<0||frame.getLong("elapsedTicks")>1_000_000_000L)throw new IllegalStateException("World frame version/clock changed");
        var source=frame.getJSONObject("originalScene");var parsed=SpatialScene.parse(source.getString("sourceText"),source.getString("id"));
        for(String key:parsed.keySet())if(!new JSONObject().put("v",parsed.get(key)).similar(new JSONObject().put("v",source.get(key))))throw new IllegalStateException("Original world geometry changed");
        var ids=new HashSet<String>();for(Object raw:source.getJSONArray("objects")){var o=(JSONObject)raw;if(o.getDouble("massKg")>0)ids.add(o.getString("id"));}
        if(frame.getJSONArray("bodies").length()!=ids.size())throw new IllegalStateException("World frame lost a body");
        for(Object raw:frame.getJSONArray("bodies")){var b=(JSONObject)raw;SpatialScene.keys(b,"id","position","rotation","velocity","angularVelocity");if(!ids.remove(b.getString("id")))throw new IllegalStateException("Duplicate/foreign world body");SpatialScene.vector(b.getJSONArray("position"),20);PoseVerify.quaternion(b.getJSONArray("rotation"));for(String k:List.of("velocity","angularVelocity"))SpatialScene.vector(b.getJSONArray(k),100);}
    }
    static JSONObject scene(JSONObject frame){
        verify(frame);var scene=copy(frame.getJSONObject("originalScene"));
        for(Object raw:scene.getJSONArray("objects")){var o=(JSONObject)raw;if(o.getDouble("massKg")<=0)continue;var b=ContactPhysics.dynamic(frame.getJSONArray("bodies"),o.getString("id"));for(String k:List.of("position","rotation","velocity","angularVelocity"))o.put(k,b.get(k));}
        return scene.put("worldFrame",copy(frame));
    }
    static void verifyScene(JSONObject scene){if(!scene(frame(scene)).similar(scene))throw new IllegalStateException("Derived world scene differs from its original committed frame");}
    static JSONObject frame(JSONObject scene){return scene.getJSONObject("worldFrame");}
    static JSONObject after(JSONObject before,JSONObject run){
        verify(before);if(!scene(before).similar(run.getJSONObject("definition")))throw new IllegalStateException("Native task started from another world frame");
        if(!run.getString("status").equals("completed")||!WorldVerify.verify(run).getBoolean("passed"))throw new IllegalStateException("Failed native task cannot advance the world");
        var frame=copy(before);var bodies=new JSONArray();for(Object raw:run.getJSONObject("final").getJSONArray("bodies")){var b=(JSONObject)raw;var value=new JSONObject();for(String k:List.of("id","position","rotation","velocity","angularVelocity"))value.put(k,b.get(k));bodies.put(value);}
        frame.put("bodies",bodies).put("elapsedTicks",before.getLong("elapsedTicks")+run.getJSONObject("final").getLong("tick"));verify(frame);return frame;
    }
}
