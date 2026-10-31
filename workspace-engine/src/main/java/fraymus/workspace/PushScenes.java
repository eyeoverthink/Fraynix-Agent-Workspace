package fraymus.workspace;

import org.json.*;
import java.util.*;

/** Clearly labelled synthetic source scenes, not captured or measured rooms. */
final class PushScenes {
    static JSONObject box(String id,double[] p,double[] size,double mass,double friction){return new JSONObject().put("sourceId",id).put("label",id).put("shape","box").put("position",new JSONArray(p)).put("rotation",new JSONArray(new int[]{0,0,0,1})).put("size",new JSONArray(size)).put("massKg",mass).put("friction",friction).put("restitution",0);}
    static String example(String variant){
        if(!Set.of("open","corner","high-friction","blocked","unreachable-standing").contains(variant))throw new IllegalArgumentException("Unknown labelled pushing scene");
        boolean high=variant.equals("high-friction");var objects=new JSONArray()
            .put(box("floor",new double[]{0,-.2,0},new double[]{8,.4,8},0,high?1:.35))
            .put(box("crate",new double[]{-1,.32,0},new double[]{.6,.6,.6},1,high?.95:.3))
            .put(box("pusher",new double[]{-3,.22,0},new double[]{.5,.4,.5},2,.15));
        for(int sign:new int[]{-1,1}){
            objects.put(box("boundary-x-"+sign,new double[]{sign*4,.75,0},new double[]{.3,1.5,8},0,.6));
            objects.put(box("boundary-z-"+sign,new double[]{0,.75,sign*4},new double[]{8,1.5,.3},0,.6));
        }
        if(variant.equals("corner"))objects.put(box("central-obstacle",new double[]{1,.6,0},new double[]{.6,1.2,.6},0,.6));
        if(variant.equals("blocked"))objects.put(box("divider",new double[]{1,.6,0},new double[]{.6,1.2,7},0,.6));
        if(variant.equals("unreachable-standing")){
            for(int sign:new int[]{-1,1})objects.put(box("corridor-"+sign,new double[]{0,.6,sign*.8},new double[]{7.2,1.2,.25},0,.6));
            objects.put(box("standing-obstruction",new double[]{-2,.6,0},new double[]{.6,1.2,.6},0,.6));
        }
        return new JSONObject().put("format","fraynix.spatial-scene").put("version",1).put("title","Labelled pushing test · "+variant).put("units","m").put("frame","right-handed-y-up")
            .put("source",new JSONObject().put("kind","synthetic").put("note","Declared test geometry and properties; no measured real room or physical-material inference.").put("physicalProperties","declared"))
            .put("scaleReference",new JSONObject().put("a",new JSONArray(new int[]{-1,0,0})).put("b",new JSONArray(new int[]{1,0,0})).put("knownLengthM",2).put("toleranceM",.001).put("basis","declared-test"))
            .put("gravityMps2",9.81).put("objects",objects).toString();
    }
}
