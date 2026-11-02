package fraymus.workspace;
import org.json.*;
import java.util.*;
final class Tools044 {
 static final JSONArray ALL=load("/tools-044.json");
 static JSONArray load(String name){try(var in=Tools044.class.getResourceAsStream(name)){return new JSONArray(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));}catch(Exception e){throw new IllegalStateException("Owned Java tool catalog unavailable",e);}}
 static Set<String> names(){var names=new HashSet<String>();for(Object raw:ALL)names.add(((JSONObject)raw).getJSONObject("function").getString("name").replace('_','.'));return names;}
 static JSONObject definition(String tool){for(Object raw:ALL){var d=(JSONObject)raw;if(d.getJSONObject("function").getString("name").replace('_','.').equals(tool))return WorldFrames.copy(d);}throw new IllegalArgumentException("Tool not registered: "+tool);}
 static JSONObject schema(String tool,JSONObject task){var s=definition(tool).getJSONObject("function").getJSONObject("parameters");if(tool.equals("procedure.run")&&task.optJSONObject("context044",new JSONObject()).has("procedureBinding")){String f=task.getString("family044"),starter=ChatPolicy044.starter(f);if(!starter.equals("procedure.run")){s=definition(starter).getJSONObject("function").getJSONObject("parameters");for(String key:List.of("name","version","sceneId","objectId","pusherId","worldId","expectedRevision","expectedHash"))s.getJSONObject("properties").remove(key);if(f.equals("perception"))for(String key:List.of("mode","visionModel","lessonName"))s.getJSONObject("properties").remove(key);if(f.equals("map")&&task.optJSONObject("bindings044",new JSONObject()).optJSONObject("mapping",new JSONObject()).has("mapId"))for(String key:new ArrayList<>(s.getJSONObject("properties").keySet()))if(!Set.of("disturbanceNs","saveSkillName").contains(key))s.getJSONObject("properties").remove(key);s.put("required",new JSONArray());}}return s;}
 static JSONArray definitions(List<String> allowed,JSONObject task){var out=new JSONArray();for(String tool:allowed){var d=definition(tool);d.getJSONObject("function").put("parameters",schema(tool,task));out.put(d);}return out;}
}
