package fraymus.workspace;

import org.json.*;
import java.util.*;

/** Authored SI values. A new schema, separate from the inherited 2D scene units. */
public record RampDefinition(double angleDeg,double lengthM,double widthM,double gravityMps2,double massKg,double friction,double offsetZ) {
    public RampDefinition {
        range(angleDeg,0,45,"angleDeg");range(lengthM,4,20,"lengthM");range(widthM,1,8,"widthM");
        range(gravityMps2,.1,30,"gravityMps2");range(massKg,.1,100,"massKg");range(friction,0,1,"friction");
        range(offsetZ,-widthM/2+.3,widthM/2-.3,"offsetZ");
    }
    static void range(double value,double min,double max,String name){if(!Double.isFinite(value)||value<min||value>max)throw new IllegalArgumentException(name+" must be finite and between "+min+" and "+max);}
    public static RampDefinition defaults(){return new RampDefinition(25,12,3,9.81,1,0,0);}
    public JSONObject json(){return new JSONObject().put("angleDeg",angleDeg).put("lengthM",lengthM).put("widthM",widthM).put("gravityMps2",gravityMps2).put("massKg",massKg).put("friction",friction).put("offsetZ",offsetZ);}
    public static RampDefinition from(JSONObject input){
        var defaults=defaults().json();if(!defaults.keySet().containsAll(input.keySet()))throw new IllegalArgumentException("Unsupported ramp parameter");
        for(String key:input.keySet()){if(!(input.get(key) instanceof Number))throw new IllegalArgumentException(key+" must be numeric");defaults.put(key,input.get(key));}
        return new RampDefinition(defaults.getDouble("angleDeg"),defaults.getDouble("lengthM"),defaults.getDouble("widthM"),defaults.getDouble("gravityMps2"),defaults.getDouble("massKg"),defaults.getDouble("friction"),defaults.getDouble("offsetZ"));
    }
    public double radians(){return Math.toRadians(angleDeg);}
    public double centerY(){return lengthM/2*Math.sin(radians())+.2;}
    public double[] start(){double s=-lengthM*.3;return new double[]{s*Math.cos(radians())+.351*Math.sin(radians()),centerY()-s*Math.sin(radians())+.351*Math.cos(radians()),offsetZ};}
    public JSONArray objects(JSONObject ids){
        double a=radians();var q=new JSONArray(List.of(0,0,-Math.sin(a/2),Math.cos(a/2)));
        return new JSONArray().put(object(ids.getString("ramp"),"Ramp",new double[]{0,centerY(),0},q,new double[]{lengthM,.2,widthM},0))
            .put(object(ids.getString("ground"),"Ground",new double[]{0,-.2,0},new JSONArray(List.of(0,0,0,1)),new double[]{40,.4,24},0))
            .put(object(ids.getString("block"),"Block",start(),q,new double[]{.5,.5,.5},massKg));
    }
    static JSONArray vector(double[] values){return new JSONArray(values);}
    static JSONObject object(String id,String label,double[] position,JSONArray rotation,double[] size,double mass){return new JSONObject().put("id",id).put("label",label).put("position",vector(position)).put("rotation",rotation).put("size",vector(size)).put("massKg",mass).put("bodyType",mass==0?"static":"dynamic");}
}
