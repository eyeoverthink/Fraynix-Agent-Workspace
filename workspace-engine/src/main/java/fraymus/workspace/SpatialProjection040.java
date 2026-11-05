package fraymus.workspace;

import org.json.*;
import java.util.*;
import static fraymus.workspace.ObserverGeometry.*;

/** Pure Java camera/projection and exact oriented-shape ray picking. No physics updates. */
final class SpatialProjection040 {
    static final Set<String> VIEWS=Set.of("perspective","top","front","side");
    static double finite(double v,double lo,double hi){if(!Double.isFinite(v)||v<lo||v>hi)throw new IllegalArgumentException("Camera/source number outside finite bounds");return v;}
    static double[] vector(JSONArray a){if(a.length()!=3)throw new IllegalArgumentException("Three coordinates required");double[] p=v(a);for(double x:p)finite(x,-1e6,1e6);return p;}
    static double[] cross(double[] a,double[] b){return new double[]{a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]};}
    record Camera(String view,int width,int height,double[] eye,double[] forward,double[] right,double[] up,double near,double far,double spanY,double spanX,double tan) {
        double[] local(double[] point){var d=sub(point,eye);return new double[]{dot(d,right),dot(d,up),dot(d,forward)};}
        double[] screen(double[] p){double sx=view.equals("perspective")?p[0]/(p[2]*tan*width/height):2*p[0]/spanX,sy=view.equals("perspective")?p[1]/(p[2]*tan):2*p[1]/spanY;return new double[]{(sx+1)*width/2,(1-sy)*height/2};}
        JSONObject json(){return new JSONObject().put("view",view).put("width",width).put("height",height).put("eye",eye).put("forward",forward).put("right",right).put("up",up).put("near",near).put("far",far).put("spanY",spanY).put("spanX",spanX).put("tanHalfFov",tan);}
    }
    static Camera camera(JSONObject frame,JSONObject input){
        frame=WorldFrames.copy(frame);input=WorldFrames.copy(input);
        SpatialScene.keys(input,"view","width","height","center","span","yawDeg","pitchDeg");String name=input.getString("view");if(!VIEWS.contains(name))throw new IllegalArgumentException("Unknown spatial view");
        int width=(int)finite(input.getDouble("width"),160,4096),height=(int)finite(input.getDouble("height"),160,4096);if(width!=input.getDouble("width")||height!=input.getDouble("height"))throw new IllegalArgumentException("Integral viewport dimensions required");
        var focus=frame.getJSONObject("focus");double[] center=vector(input.optJSONArray("center",focus.getJSONArray("center")));double span=finite(input.optDouble("span",focus.getDouble("span")),.001,20000);double[] outward,upHint;
        if(name.equals("top")){outward=new double[]{0,1,0};upHint=new double[]{0,0,-1};}
        else if(name.equals("front")){outward=new double[]{0,0,1};upHint=new double[]{0,1,0};}
        else if(name.equals("side")){outward=new double[]{1,0,0};upHint=new double[]{0,1,0};}
        else{double yaw=Math.toRadians(finite(input.optDouble("yawDeg",35),-360,360)),pitch=Math.toRadians(finite(input.optDouble("pitchDeg",30),-85,85));outward=new double[]{Math.sin(yaw)*Math.cos(pitch),Math.sin(pitch),Math.cos(yaw)*Math.cos(pitch)};upHint=new double[]{0,1,0};}
        double distance=Math.max(.01,span*1.7);var forward=mul(outward,-1);var right=unit(cross(forward,upHint));var up=unit(cross(right,forward));double spanX=span*width/height;
        if(frame.getJSONObject("source").getString("kind").equals("game")&&name.equals("front")&&!input.has("span")&&!input.has("center"))spanX=focus.getDouble("spanX");
        return new Camera(name,width,height,add(center,mul(outward,distance)),forward,right,up,Math.max(1e-5,span/10000),Math.max(200,span*100),span,spanX,Math.tan(Math.toRadians(45)/2));
    }
    static List<double[]> clip(List<double[]> polygon,int axis,double limit,boolean greater){var out=new ArrayList<double[]>();if(polygon.isEmpty())return out;var a=polygon.get(polygon.size()-1);boolean ain=greater?a[axis]>=limit:a[axis]<=limit;for(var b:polygon){boolean bin=greater?b[axis]>=limit:b[axis]<=limit;if(ain!=bin){double t=(limit-a[axis])/(b[axis]-a[axis]);out.add(new double[]{a[0]+t*(b[0]-a[0]),a[1]+t*(b[1]-a[1]),a[2]+t*(b[2]-a[2])});}if(bin)out.add(b);a=b;ain=bin;}return out;}
    static List<double[]> frustum(List<double[]> p,Camera c){
        p=clip(p,2,c.near,true);p=clip(p,2,c.far,false);if(p.isEmpty())return p;
        // Clip in homogeneous viewport coordinates after near/far clipping.
        var screen=new ArrayList<double[]>();for(var v:p){var s=c.screen(v);screen.add(new double[]{s[0],s[1],v[2]});}
        List<double[]> out=screen;out=clip(out,0,0,true);out=clip(out,0,c.width,false);out=clip(out,1,0,true);return clip(out,1,c.height,false);
    }
    static JSONArray corners(JSONObject e){e=WorldFrames.copy(e);var p=vector(e.getJSONArray("position"));var size=vector(e.getJSONArray("size"));var q=e.getJSONArray("rotation");PoseVerify.quaternion(q);var a=new JSONArray();boolean quad=e.getString("shape").equals("quad");for(int i=0;i<(quad?4:8);i++){int bits=quad?new int[]{0,1,3,2}[i]:i;var local=new double[]{((bits&1)==0?-.5:.5)*size[0],((bits&2)==0?-.5:.5)*size[1],quad?0:((bits&4)==0?-.5:.5)*size[2]};a.put(new JSONArray(add(p,rotate(local,q,false))));}return a;}
    static JSONObject project(JSONObject frame,JSONObject input){
        frame=WorldFrames.copy(frame);
        var c=camera(frame,input);var polygons=new ArrayList<JSONObject>();var centers=new JSONArray();boolean game=frame.getJSONObject("source").getString("kind").equals("game");
        for(Object raw:frame.getJSONArray("entities")){var e=(JSONObject)raw;if(!e.has("position")||!e.has("shape"))continue;var source=corners(e);boolean quad=e.getString("shape").equals("quad");int[][] faces=quad?new int[][]{{0,1,2,3}}:new int[][]{{4,5,7,6},{0,2,3,1},{0,4,6,2},{1,3,7,5},{0,1,5,4},{2,6,7,3}};
            var cp=c.local(vector(e.getJSONArray("position")));if(cp[2]>=c.near&&cp[2]<=c.far){var screen=c.screen(cp);centers.put(new JSONObject().put("id",e.getString("id")).put("pixel",screen).put("depth",cp[2]));}
            for(int fi=0;fi<faces.length;fi++){var f=faces[fi];var world=new ArrayList<double[]>();for(int i:f)world.add(vector(source.getJSONArray(i)));if(!quad){var normal=cross(sub(world.get(1),world.get(0)),sub(world.get(2),world.get(0)));if(dot(normal,sub(c.eye,world.get(0)))<=0)continue;}
                var local=world.stream().map(c::local).toList();double depth=local.stream().mapToDouble(p->p[2]).average().orElse(0);var clipped=frustum(local,c);if(clipped.size()<3)continue;double area=0;for(int i=0;i<clipped.size();i++){var a=clipped.get(i);var b=clipped.get((i+1)%clipped.size());area+=a[0]*b[1]-b[0]*a[1];}if(Math.abs(area)<.01)continue;
                var points=new JSONArray();for(var p:clipped)points.put(new JSONArray(new double[]{p[0],p[1]}));polygons.add(new JSONObject().put("id",e.getString("id")).put("face",fi).put("points",points).put("depth",depth).put("layer",e.optInt("renderLayer",0)).put("dynamic",e.optBoolean("dynamic",false)).put("selected",e.getString("id").equals(frame.optString("selectedId"))));
            }
        }
        polygons.sort((a,b)->{if(game){int l=Integer.compare(a.getInt("layer"),b.getInt("layer"));if(l!=0)return l;}return Double.compare(b.getDouble("depth"),a.getDouble("depth"));});return new JSONObject().put("snapshotHash",frame.getString("snapshotHash")).put("camera",c.json()).put("polygons",polygons).put("centers",centers).put("entityCount",frame.getJSONArray("entities").length()).put("scope","Actual source geometry projection. Filled faces use depth ordering; this is not calibrated image reconstruction.");
    }
    static JSONObject pick(JSONObject frame,JSONObject input){
        frame=WorldFrames.copy(frame);
        SpatialScene.keys(input,"camera","x","y");var camera=camera(frame,input.getJSONObject("camera"));double x=finite(input.getDouble("x"),0,camera.width),y=finite(input.getDouble("y"),0,camera.height),sx=2*x/camera.width-1,sy=1-2*y/camera.height;double[] origin,ray;
        if(camera.view.equals("perspective")){origin=camera.eye;ray=unit(add(camera.forward,add(mul(camera.right,sx*camera.tan*camera.width/camera.height),mul(camera.up,sy*camera.tan))));}
        else{origin=add(camera.eye,add(mul(camera.right,sx*camera.spanX/2),mul(camera.up,sy*camera.spanY/2)));ray=camera.forward;}
        double nearest=Double.POSITIVE_INFINITY;JSONObject hit=null;
        for(Object raw:frame.getJSONArray("entities")){var e=(JSONObject)raw;if(!e.has("shape"))continue;var local=rotate(sub(origin,vector(e.getJSONArray("position"))),e.getJSONArray("rotation"),true);var direction=rotate(ray,e.getJSONArray("rotation"),true);var size=vector(e.getJSONArray("size"));double near=camera.near,far=camera.far;
            for(int i=0;i<3;i++){double half=size[i]/2;if(Math.abs(direction[i])<1e-12){if(Math.abs(local[i])>half+1e-10){far=-1;break;}}else{double a=(-half-local[i])/direction[i],b=(half-local[i])/direction[i];near=Math.max(near,Math.min(a,b));far=Math.min(far,Math.max(a,b));}}
            if(near<=far&&(near<nearest-1e-8||Math.abs(near-nearest)<1e-8&&hit!=null&&e.optInt("renderLayer",0)>hit.optInt("renderLayer",0))){nearest=near;hit=e;}
        }
        return new JSONObject().put("snapshotHash",frame.getString("snapshotHash")).put("id",hit==null?"":hit.getString("id")).put("distance",hit==null?JSONObject.NULL:nearest).put("units",frame.getJSONObject("source").getString("units"));
    }
}
