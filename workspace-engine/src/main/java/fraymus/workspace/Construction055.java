package fraymus.workspace;

import org.json.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Finite, source-bound routes for declared upright box stock. No native pose writes. */
final class Construction055 {
    static final String VERSION = "lateral-construction-055-v1";
    static final String LEGACY_054 = "4b42e70908cd1ba9a2447175f9f07c5fc1542283ae309a5cb50bb3736a0c3a4c";
    static final String SCOPE = "Declared upright boxes routed inside the floor around tall fixed walls; native forces and original release/contact checks remain authoritative. Not arbitrary mesh planning.";
    private Construction055() {}
    static JSONObject protocol() {
        try(var in=Construction055.class.getResourceAsStream("/construction055-protocol.json")) {
            return new JSONObject(new String(in.readAllBytes(),StandardCharsets.UTF_8));
        } catch(Exception e) { throw new IllegalStateException(e); }
    }
    static boolean preset(String name) { return Set.of("detour","sealed").contains(name); }
    static boolean is(JSONObject scene) { return Construction054.is(scene)&&preset(Construction054.config(scene).getString("preset")); }
    static JSONObject configuration(JSONObject requested) {
        String name=requested.getString("preset");
        if(!preset(name))throw new IllegalArgumentException("Unknown lateral route scene");
        var base=WorldFrames.copy(requested).put("preset","wide");
        if(base.has("obstacleHeightM")&&(!(base.get("obstacleHeightM") instanceof Number)||base.getDouble("obstacleHeightM")!=3.2))
            throw new IllegalArgumentException("The declared tall wall is fixed at 3.2 m");
        base.remove("obstacleHeightM");
        return Construction054.configuration(base).put("preset",name).put("obstacleHeightM",3.2);
    }
    static JSONObject kit(JSONObject requested) {
        var c=configuration(requested);var base=WorldFrames.copy(c).put("preset","wide").put("obstacleHeightM",.8);
        var scene=Construction054.kit(base);double width=c.getString("preset").equals("detour")?1.4:7.8;
        scene.getJSONArray("objects").put(new JSONObject().put("sourceId","obstacle-wall").put("label","Declared tall wall")
            .put("shape","box").put("position",new JSONArray(new double[]{0,1.6,-.8})).put("size",new JSONArray(new double[]{width,3.2,.25}))
            .put("rotation",WorldPlan.quat(0)).put("massKg",0).put("friction",.8).put("restitution",0));
        scene.put("title","Declared055 "+c.getString("preset")+" bridge").getJSONObject("source").put("note",Construction054.PREFIX+c);
        return scene;
    }
    static JSONObject plan(JSONObject scene,JSONObject request) {
        var expected=Construction054.intent(scene,request.getJSONObject("construction051"),request.getDouble("forceLimitN"),request.getDouble("torqueLimitNm"));
        if(!expected.similar(request))throw new IllegalStateException("Lateral construction source/intent changed");
        var bodies=new LinkedHashMap<String,JSONObject>();double top=0,halfHeight=0;
        for(Object raw:scene.getJSONArray("objects")) {
            var b=WorldFrames.copy((JSONObject)raw);if(b.getString("sourceId").equals("floor"))continue;
            bodies.put(b.getString("sourceId"),b);
            if(b.getDouble("massKg")>0) {
                if(Math.abs(WorldPlan.yaw(b.getJSONArray("rotation")))>2)throw new IllegalArgumentException("Inspect stock heading before lateral transport; supported heading is within two degrees");
                double h=b.getJSONArray("size").getDouble(1)/2;halfHeight=Math.max(halfHeight,h);top=Math.max(top,b.getJSONArray("position").getDouble(1)+h);
            }
        }
        for(Object raw:expected.getJSONArray("targets")) {var t=(JSONObject)raw;top=Math.max(top,t.getJSONArray("positionM").getDouble(1)+bodies.get(t.getString("sourceId")).getJSONArray("size").getDouble(1)/2);}
        double lift=top+halfHeight+.20;boolean passed=lift<=2;String reason=passed?"Finite lateral route clear; actual native support and obstacle checks still required":"Carry height exceeds the declared 2 m limit";
        var targets=new JSONArray();int detours=0;
        for(Object raw:expected.getJSONArray("targets")) {
            var t=(JSONObject)raw;String id=t.getString("sourceId");var b=bodies.get(id);var next=WorldFrames.copy(b).put("position",t.getJSONArray("positionM")).put("rotation",WorldPlan.quat(0));
            if(!Construction054.columnClear(b,bodies,id)||!Construction054.columnClear(next,bodies,id)){passed=false;reason="Occupied pickup or lowering corridor; no native action admitted";break;}
            var via=route(b,next,bodies,lift);
            if(via.isEmpty()){passed=false;reason="No clear route around the wall within the declared floor and carry-height bounds";break;}
            if(via.length()>2)detours++;
            targets.put(WorldFrames.copy(t).put("objectId",b.getString("id")).put("rotation",WorldPlan.quat(0)).put("viaM",via));bodies.put(id,next);
        }
        return new JSONObject().put("version",VERSION).put("scope",SCOPE).put("intent",expected).put("liftHeightM",lift)
            .put("orderedTargets",targets).put("passed",passed).put("reason",reason)
            .put("route055",new JSONObject().put("algorithm","Finite expanded-rectangle visibility graph / shortest Euclidean path")
                .put("horizontalMarginM",.12).put("detours",detours).put("maxCarryHeightM",2));
    }
    private static JSONArray route(JSONObject body,JSONObject target,Map<String,JSONObject> bodies,double height) {
        var p=body.getJSONArray("position");var q=target.getJSONArray("position");var size=body.getJSONArray("size");
        // Conservatively cover the small admitted yaw at pickup, then carry at zero yaw.
        double angle=Math.toRadians(2),hx=(size.getDouble(0)+Math.sin(angle)*size.getDouble(2))/2+.12;
        double hz=(size.getDouble(2)+Math.sin(angle)*size.getDouble(0))/2+.12,hy=size.getDouble(1)/2;
        double xBound=4-hx,zBound=4-hz;
        var rectangles=new ArrayList<double[]>();
        for(var b:bodies.values()) {
            if(b.getString("id").equals(body.getString("id")))continue;
            var bp=b.getJSONArray("position");var bs=b.getJSONArray("size");
            if(bp.getDouble(1)+bs.getDouble(1)/2<height-hy-.05)continue;
            if(bp.getDouble(1)-bs.getDouble(1)/2>height+hy+.05)continue;
            rectangles.add(new double[]{bp.getDouble(0)-bs.getDouble(0)/2-hx,bp.getDouble(0)+bs.getDouble(0)/2+hx,bp.getDouble(2)-bs.getDouble(2)/2-hz,bp.getDouble(2)+bs.getDouble(2)/2+hz});
        }
        var nodes=new ArrayList<double[]>();nodes.add(new double[]{p.getDouble(0),p.getDouble(2)});nodes.add(new double[]{q.getDouble(0),q.getDouble(2)});
        for(var r:rectangles)for(double x:new double[]{r[0]-.01,r[1]+.01})for(double z:new double[]{r[2]-.01,r[3]+.01})if(Math.abs(x)<=xBound&&Math.abs(z)<=zBound)nodes.add(new double[]{x,z});
        if(nodes.size()>66)return new JSONArray();
        for(int i=0;i<2;i++)if(Math.abs(nodes.get(i)[0])>xBound||Math.abs(nodes.get(i)[1])>zBound)return new JSONArray();
        double[] distances=new double[nodes.size()];Arrays.fill(distances,Double.POSITIVE_INFINITY);distances[0]=0;
        int[] previous=new int[nodes.size()];Arrays.fill(previous,-1);boolean[] visited=new boolean[nodes.size()];
        for(int step=0;step<nodes.size();step++) {
            int u=-1;for(int i=0;i<nodes.size();i++)if(!visited[i]&&(u<0||distances[i]<distances[u]))u=i;
            if(u<0||!Double.isFinite(distances[u]))break;if(u==1)break;visited[u]=true;
            for(int v=0;v<nodes.size();v++)if(!visited[v]&&v!=u) {
                boolean clear=true;for(var r:rectangles)if(intersects(nodes.get(u),nodes.get(v),r)){clear=false;break;}
                double cost=distances[u]+Math.hypot(nodes.get(u)[0]-nodes.get(v)[0],nodes.get(u)[1]-nodes.get(v)[1]);
                if(clear&&cost<distances[v]-1e-10){distances[v]=cost;previous[v]=u;}
            }
        }
        if(!Double.isFinite(distances[1]))return new JSONArray();
        var reverse=new ArrayList<Integer>();for(int i=1;i>=0;i=previous[i]){reverse.add(i);if(i==0)break;}
        Collections.reverse(reverse);var out=new JSONArray();for(int i:reverse){var a=nodes.get(i);out.put(new JSONArray(new double[]{a[0],height,a[1]}));}
        return out.length()>16?new JSONArray():out;
    }
    private static boolean intersects(double[] a,double[] b,double[] box) {
        double low=0,high=1;
        for(int k=0;k<2;k++) {
            double delta=b[k]-a[k],minimum=box[2*k],maximum=box[2*k+1];
            if(Math.abs(delta)<1e-12){if(a[k]<minimum||a[k]>maximum)return false;}
            else {double first=(minimum-a[k])/delta,last=(maximum-a[k])/delta;if(first>last){double swap=first;first=last;last=swap;}low=Math.max(low,first);high=Math.min(high,last);if(low>high)return false;}
        }
        return true;
    }
}
