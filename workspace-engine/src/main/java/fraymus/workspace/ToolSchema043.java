package fraymus.workspace;
import org.json.*;

/** Owned validator for the bounded registered tool schema subset used by the Java workbench. */
final class ToolSchema043 {
    static void validate(JSONObject schema,Object value){check(schema,value,"args",0);}
    private static void check(JSONObject s,Object value,String at,int depth){
        if(depth>12)throw new IllegalArgumentException("Tool arguments nested too deeply");String type=s.optString("type","");
        if(s.has("enum")&&!s.getJSONArray("enum").toList().contains(value))throw new IllegalArgumentException(at+" outside registered choices");
        if(type.equals("object")){
            if(!(value instanceof JSONObject o))throw new IllegalArgumentException(at+" must be an object");var props=s.optJSONObject("properties");if(props==null)props=new JSONObject();
            for(Object k:s.optJSONArray("required")==null?new JSONArray():s.getJSONArray("required"))if(!o.has(k.toString()))throw new IllegalArgumentException(at+" missing "+k);
            for(String k:o.keySet())if(props.has(k))check(props.getJSONObject(k),o.get(k),at+"."+k,depth+1);else if(s.opt("additionalProperties") instanceof JSONObject extra)check(extra,o.get(k),at+"."+k,depth+1);else if(!s.optBoolean("additionalProperties",true))throw new IllegalArgumentException(at+" unknown field "+k+"; allowed fields "+props.keySet());
        }else if(type.equals("array")){
            if(!(value instanceof JSONArray a)||a.length()<s.optInt("minItems",0)||a.length()>s.optInt("maxItems",10000))throw new IllegalArgumentException(at+" invalid array length");if(s.has("items"))for(Object x:a)check(s.getJSONObject("items"),x,at+"[]",depth+1);
        }else if(type.equals("string")){
            if(!(value instanceof String text)||text.length()<s.optInt("minLength",0)||text.length()>s.optInt("maxLength",512000)||s.has("pattern")&&!text.matches(s.getString("pattern")))throw new IllegalArgumentException(at+" invalid string");
        }else if(type.equals("number")||type.equals("integer")){
            if(!(value instanceof Number n)||!Double.isFinite(n.doubleValue())||type.equals("integer")&&n.doubleValue()!=Math.rint(n.doubleValue())||n.doubleValue()<s.optDouble("minimum",-Double.MAX_VALUE)||n.doubleValue()>s.optDouble("maximum",Double.MAX_VALUE))throw new IllegalArgumentException(at+" invalid number");
        }else if(type.equals("boolean")&&!(value instanceof Boolean))throw new IllegalArgumentException(at+" must be boolean");
        if(s.has("anyOf")){boolean valid=false;for(Object raw:s.getJSONArray("anyOf"))try{check((JSONObject)raw,value,at,depth+1);valid=true;break;}catch(IllegalArgumentException ignored){}if(!valid)throw new IllegalArgumentException(at+" requires an actual assertion");}
        if(s.has("required")&&value instanceof JSONObject o)for(Object k:s.getJSONArray("required"))if(!o.has(k.toString()))throw new IllegalArgumentException(at+" missing "+k);
    }
}
