package fraymus.workspace;

import org.json.*;
import java.util.*;

/** Deterministic camera geometry over the engine's actual oriented boxes. No learned model. */
final class ObserverGeometry {
    static double[] v(JSONArray a){return new double[]{a.getDouble(0),a.getDouble(1),a.getDouble(2)};}
    static double[] add(double[] a,double[] b){return new double[]{a[0]+b[0],a[1]+b[1],a[2]+b[2]};}
    static double[] mul(double[] a,double k){return new double[]{a[0]*k,a[1]*k,a[2]*k};}
    static double dot(double[] a,double[] b){return a[0]*b[0]+a[1]*b[1]+a[2]*b[2];}
    static double[] sub(double[] a,double[] b){return add(a,mul(b,-1));}
    static double norm(double[] a){return Math.sqrt(dot(a,a));}
    static double[] unit(double[] a){return mul(a,1/norm(a));}
    static double[] forward(JSONObject pose){double y=Math.toRadians(pose.getDouble("yawDeg")),p=Math.toRadians(pose.getDouble("pitchDeg"));return new double[]{Math.sin(y)*Math.cos(p),Math.sin(p),-Math.cos(y)*Math.cos(p)};}
    static double[] right(JSONObject pose){double y=Math.toRadians(pose.getDouble("yawDeg"));return new double[]{Math.cos(y),0,Math.sin(y)};}
    static double[] up(JSONObject pose){var r=right(pose);var f=forward(pose);return new double[]{r[1]*f[2]-r[2]*f[1],r[2]*f[0]-r[0]*f[2],r[0]*f[1]-r[1]*f[0]};}
    static double[] rotate(double[] p,JSONArray q,boolean inverse){
        double x=q.getDouble(0),y=q.getDouble(1),z=q.getDouble(2),w=q.getDouble(3);if(inverse){x=-x;y=-y;z=-z;}
        double[] t={2*(y*p[2]-z*p[1]),2*(z*p[0]-x*p[2]),2*(x*p[1]-y*p[0])};
        return new double[]{p[0]+w*t[0]+y*t[2]-z*t[1],p[1]+w*t[1]+z*t[0]-x*t[2],p[2]+w*t[2]+x*t[1]-y*t[0]};
    }
    static double hit(JSONObject box,double[] origin,double[] direction){
        var local=rotate(sub(origin,v(box.getJSONArray("position"))),box.getJSONArray("rotation"),true);
        var ray=rotate(direction,box.getJSONArray("rotation"),true);var size=v(box.getJSONArray("size"));double near=0,far=150;boolean inside=true;
        for(int i=0;i<3;i++){
            double half=size[i]/2;if(Math.abs(local[i])>half)inside=false;
            if(Math.abs(ray[i])<1e-10){if(Math.abs(local[i])>half)return Double.POSITIVE_INFINITY;continue;}
            double a=(-half-local[i])/ray[i],b=(half-local[i])/ray[i];near=Math.max(near,Math.min(a,b));far=Math.min(far,Math.max(a,b));if(near>far)return Double.POSITIVE_INFINITY;
        }
        // First-person free camera is not an avatar; interior backfaces are not counted as seen.
        return inside?Double.POSITIVE_INFINITY:near>=.05&&near<=150?near:Double.POSITIVE_INFINITY;
    }
    static String first(JSONArray objects,double[] eye,double[] ray){
        double nearest=Double.POSITIVE_INFINITY;String id="";
        for(Object value:objects){var box=(JSONObject)value;double t=hit(box,eye,ray);if(t<nearest){nearest=t;id=box.getString("id");}}return id;
    }
    static JSONArray visible(JSONObject pose,JSONObject viewport,JSONArray objects){
        var eye=v(pose.getJSONArray("position"));var forward=forward(pose);var right=right(pose);var up=up(pose);
        double tan=Math.tan(Math.toRadians(pose.getDouble("fovDeg"))/2),aspect=viewport.getDouble("width")/viewport.getDouble("height");
        var counts=new HashMap<String,Integer>();
        // Screen rays find large surfaces; per-box center/corner rays find small visible objects.
        for(int x=-4;x<=4;x++)for(int y=-2;y<=2;y++){
            var ray=unit(add(forward,add(mul(right,x/4.0*.95*tan*aspect),mul(up,y/2.0*.95*tan))));
            counts.merge(first(objects,eye,ray),1,Integer::sum);
        }
        for(Object value:objects){
            var box=(JSONObject)value;var center=v(box.getJSONArray("position"));var size=v(box.getJSONArray("size"));
            for(int corner=-1;corner<8;corner++){
                var offset=corner<0?new double[3]:new double[]{((corner&1)==0?-1:1)*size[0]/2,((corner&2)==0?-1:1)*size[1]/2,((corner&4)==0?-1:1)*size[2]/2};
                var point=add(center,rotate(offset,box.getJSONArray("rotation"),false));var delta=sub(point,eye);double depth=dot(delta,forward);
                if(depth<.05||depth>150||Math.abs(dot(delta,right))>depth*tan*aspect||Math.abs(dot(delta,up))>depth*tan)continue;
                if(norm(delta)>1e-9&&first(objects,eye,unit(delta)).equals(box.getString("id")))counts.merge(box.getString("id"),1,Integer::sum);
            }
        }
        var result=new JSONArray();
        for(Object value:objects){
            var box=(JSONObject)value;int samples=counts.getOrDefault(box.getString("id"),0);if(samples==0)continue;
            var delta=sub(v(box.getJSONArray("position")),eye);double depth=dot(delta,forward);
            result.put(new JSONObject(box.toString()).put("distanceM",norm(delta)).put("visibleSamples",samples)
                .put("centerNdc",depth>.05?new JSONArray(List.of(dot(delta,right)/(depth*tan*aspect),dot(delta,up)/(depth*tan))):JSONObject.NULL));
        }
        return result;
    }
}
