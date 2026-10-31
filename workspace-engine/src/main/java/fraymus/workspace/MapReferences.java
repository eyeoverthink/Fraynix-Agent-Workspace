package fraymus.workspace;

import org.json.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

/** Checked-in labelled references, treated as source data with byte hashes. */
final class MapReferences {
    static JSONObject cases(){try(var in=MapReferences.class.getResourceAsStream("/maps029/reserved-cases.json")){if(in==null)throw new IllegalStateException("Reference corpus missing");return new JSONObject(new String(in.readAllBytes(),StandardCharsets.UTF_8));}catch(Exception e){throw new IllegalStateException("Cannot read reference corpus",e);}}
    static JSONObject catalog(){var out=new JSONArray();for(Object raw:cases().getJSONArray("cases")){var c=(JSONObject)raw;example(c.getString("id"));out.put(new JSONObject().put("id",c.getString("id")).put("title",c.getString("title")).put("sha256",c.getString("sha256")).put("kind",c.getString("kind")));}return new JSONObject().put("cases",out);}
    static JSONObject example(String id){for(Object raw:cases().getJSONArray("cases")){var c=(JSONObject)raw;if(!c.getString("id").equals(id))continue;try(var in=MapReferences.class.getResourceAsStream("/maps029/"+c.getString("file"))){if(in==null)throw new IllegalStateException("Original reference missing");byte[] bytes=in.readAllBytes();if(!MapDefinition.sha(bytes).equals(c.getString("sha256")))throw new IllegalStateException("Original reference bytes changed");return new JSONObject().put("title",c.getString("title")+" · labelled test reference").put("pngBase64",Base64.getEncoder().encodeToString(bytes)).put("tilePixels",1).put("walkableColors",c.getJSONArray("walkableColors"));}catch(Exception e){throw new IllegalStateException("Cannot read original reference",e);}}throw new IllegalArgumentException("Unknown labelled reference");}
}
