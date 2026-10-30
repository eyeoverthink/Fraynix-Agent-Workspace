package fraymus.workspace;
import org.json.*;
import java.util.*;

/** Data-only portable definition for the supported native ramp/block scene. */
final class SceneExchange {
    static final String FORMAT="fraynix.scene";
    static JSONObject roles(RampDefinition definition){
        var ids=new JSONObject().put("ramp","ramp").put("ground","ground").put("block","block");
        return new JSONObject().put("objects",definition.objects(ids));
    }
    static JSONObject exportScene(JSONObject scene){
        var config=RampDefinition.from(scene.getJSONObject("parameters"));var geometry=roles(config);
        return new JSONObject().put("format",FORMAT).put("version",1).put("title",scene.getString("title")).put("units","metre, second, kilogram").put("frame","+Y up; downhill +X; depth Z")
            .put("parameters",config.json()).put("geometry",geometry).put("geometrySha256",MeasurementStore.hash(geometry.toString()))
            .put("source",new JSONObject().put("sceneId",scene.getString("id")).put("sceneVersion",scene.getInt("version")));
    }
    static RampDefinition validate(JSONObject file){
        if(file.toString().length()>64_000)throw new IllegalArgumentException("Scene package exceeds 64 KB");
        if(!file.keySet().equals(Set.of("format","version","title","units","frame","parameters","geometry","geometrySha256","source")))throw new IllegalArgumentException("Unexpected scene package fields; code and instructions are not scene data");
        if(!file.getString("format").equals(FORMAT)||file.getInt("version")!=1||!file.get("version").toString().equals("1"))throw new IllegalArgumentException("Unsupported scene package format/version");
        if(!file.getString("units").equals("metre, second, kilogram")||!file.getString("frame").equals("+Y up; downhill +X; depth Z"))throw new IllegalArgumentException("Scene units/frame differ");
        if(file.getString("title").isBlank()||file.getString("title").length()>120)throw new IllegalArgumentException("Invalid scene title");
        var parameters=file.getJSONObject("parameters");if(!parameters.keySet().equals(RampDefinition.defaults().json().keySet()))throw new IllegalArgumentException("Complete native parameters are required");
        var config=RampDefinition.from(parameters);var geometry=roles(config);
        if(!geometry.similar(file.getJSONObject("geometry"))||!MeasurementStore.hash(geometry.toString()).equals(file.getString("geometrySha256")))throw new IllegalArgumentException("Geometry differs from supported native definition or its hash");
        var source=file.getJSONObject("source");if(!source.keySet().equals(Set.of("sceneId","sceneVersion")))throw new IllegalArgumentException("Invalid source provenance");
        UUID.fromString(source.getString("sceneId"));if(source.getInt("sceneVersion")<1)throw new IllegalArgumentException("Invalid source version");
        return config;
    }
}
