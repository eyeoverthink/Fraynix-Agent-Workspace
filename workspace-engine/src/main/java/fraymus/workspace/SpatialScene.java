package fraymus.workspace;

import org.json.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Data-only, declared-scale box scene. No scripts, meshes or inferred material properties. */
final class SpatialScene {
    static final String PROCEDURE="spatial-manipulation-v1";
    static void keys(JSONObject o,String... keys){if(!Set.of(keys).containsAll(o.keySet()))throw new IllegalArgumentException("Unsupported spatial field: "+o.keySet());}
    static double number(JSONObject o,String key,double lo,double hi){Object v=o.get(key);if(!(v instanceof Number n))throw new IllegalArgumentException(key+" must be numeric");RampDefinition.range(n.doubleValue(),lo,hi,key);return n.doubleValue();}
    static double[] vector(JSONArray a,double bound){if(a.length()!=3)throw new IllegalArgumentException("A spatial vector needs three numbers");double[] out=new double[3];for(int i=0;i<3;i++){if(!(a.get(i) instanceof Number n))throw new IllegalArgumentException("Numeric vector required");RampDefinition.range(n.doubleValue(),-bound,bound,"vector");out[i]=n.doubleValue();}return out;}
    static JSONArray array(double[] p){return new JSONArray(p);}
    static String text(JSONObject o,String k,int max){String s=o.getString(k);if(s.isBlank()||s.length()>max)throw new IllegalArgumentException("Invalid "+k);return s;}
    static JSONObject parse(String source,String id){
        if(source.getBytes(StandardCharsets.UTF_8).length>131072)throw new IllegalArgumentException("Spatial JSON exceeds 128 KiB");UUID.fromString(id);
        var in=new JSONObject(source);keys(in,"format","version","title","units","frame","source","scaleReference","gravityMps2","objects");
        if(!in.getString("format").equals("fraynix.spatial-scene")||number(in,"version",1,1)!=1||!in.getString("frame").equals("right-handed-y-up"))throw new IllegalArgumentException("Require spatial-scene v1 / right-handed-y-up");
        double scale=switch(in.getString("units")){case "m"->1;case "cm"->.01;case "mm"->.001;default->throw new IllegalArgumentException("Explicit m, cm or mm units required");};
        var provenance=in.getJSONObject("source");keys(provenance,"kind","note","physicalProperties");
        if(!Set.of("synthetic","measured-user-label","unverified").contains(provenance.getString("kind"))||!Set.of("declared","measured-user-label").contains(provenance.getString("physicalProperties")))throw new IllegalArgumentException("Explicit qualified source/property labels required");text(provenance,"note",1000);
        var ref=in.getJSONObject("scaleReference");keys(ref,"a","b","knownLengthM","toleranceM","basis");
        if(!Set.of("declared-test","user-measurement").contains(ref.getString("basis")))throw new IllegalArgumentException("Explicit scale-reference basis required");
        var a=vector(ref.getJSONArray("a"),100000);var b=vector(ref.getJSONArray("b"),100000);double known=number(ref,"knownLengthM",.001,1000),tolerance=number(ref,"toleranceM",0,.1),length=ObserverGeometry.norm(ObserverGeometry.sub(a,b))*scale;
        if(Math.abs(length-known)>tolerance+1e-9)throw new IllegalArgumentException("Declared scale reference disagrees with geometry/units");
        var objects=in.getJSONArray("objects");if(objects.length()<2||objects.length()>32)throw new IllegalArgumentException("Import 2..32 box objects");var out=new JSONArray();var ids=new HashSet<String>();int dynamic=0;
        for(Object value:objects){var o=(JSONObject)value;keys(o,"sourceId","label","shape","position","rotation","size","massKg","friction","restitution");String key=text(o,"sourceId",80);if(!key.matches("[A-Za-z0-9_.-]+")||!ids.add(key)||!o.getString("shape").equals("box"))throw new IllegalArgumentException("Unique source IDs and box geometry required");
            double mass=number(o,"massKg",0,20),friction=number(o,"friction",0,2),restitution=number(o,"restitution",0,.8);if(mass>0)dynamic++;
            var p=vector(o.getJSONArray("position"),100000);var size=vector(o.getJSONArray("size"),100000);for(int i=0;i<3;i++){p[i]*=scale;size[i]*=scale;RampDefinition.range(p[i],-20,20,"position metres");RampDefinition.range(size[i],.05,40,"size metres");}
            var q=o.getJSONArray("rotation");if(q.length()!=4)throw new IllegalArgumentException("Quaternion needs four components");double norm=0;for(Object raw:q){if(!(raw instanceof Number n)||!Double.isFinite(n.doubleValue()))throw new IllegalArgumentException("Finite quaternion required");norm+=n.doubleValue()*n.doubleValue();}if(Math.abs(norm-1)>.001)throw new IllegalArgumentException("Unit quaternion required");
            out.put(new JSONObject().put("id",UUID.nameUUIDFromBytes((id+":"+key).getBytes(StandardCharsets.UTF_8)).toString()).put("sourceId",key).put("label",text(o,"label",100)).put("position",array(p)).put("size",array(size)).put("rotation",q).put("massKg",mass).put("friction",friction).put("restitution",restitution).put("bodyType",mass>0?"dynamic":"static"));
        }
        if(dynamic<1||dynamic>8||dynamic==objects.length())throw new IllegalArgumentException("Require a static surface and 1..8 dynamic bodies");
        return new JSONObject().put("id",id).put("version",1).put("title",text(in,"title",120)).put("format","fraynix.spatial-scene").put("sourceUnits",in.getString("units")).put("units","m").put("frame",in.getString("frame")).put("source",provenance).put("sourceText",source).put("sourceSha256",MeasurementStore.hash(source)).put("gravityMps2",number(in,"gravityMps2",.1,20)).put("objects",out)
            .put("scaleCheck",new JSONObject().put("basis",ref.getString("basis")).put("knownLengthM",known).put("convertedLengthM",length).put("toleranceM",tolerance).put("passed",true).put("independentlyCalibrated",false));
    }
    static JSONObject object(JSONObject scene,String id){for(Object raw:scene.getJSONArray("objects")){var o=(JSONObject)raw;if(o.getString("id").equals(id))return o;}throw new IllegalArgumentException("Object is not in the selected spatial scene");}
}
