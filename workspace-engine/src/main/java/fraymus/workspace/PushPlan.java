package fraymus.workspace;

import org.json.*;
import java.util.*;

/** Bounded configuration search: a push needs a reachable standing cell. */
final class PushPlan {
    static final String PROCEDURE="spatial-push-v1",VERSION="cardinal-contact-push-v1";
    static final String SCOPE="Declared-scale native contact pushing of two boxes on a flat floor. Conservative integer-metre planning, finite force/recovery budgets; not universal robotic manipulation or measured material inference.";
    static final int MAX_STATES=8192,MAX_PUSHES=12;
    static final int[][] DIRECTIONS={{1,0},{0,1},{-1,0},{0,-1}};
    final JSONObject scene,pusher,crate,floor;
    final int minX,maxX,minZ,maxZ,width,height;
    final double pusherRadius,crateRadius;
    final List<JSONObject> obstacles=new ArrayList<>();

    PushPlan(JSONObject scene,String objectId,String pusherId){
        this.scene=scene;crate=SpatialScene.object(scene,objectId);pusher=SpatialScene.object(scene,pusherId);
        if(objectId.equals(pusherId)||crate.getDouble("massKg")<=0||pusher.getDouble("massKg")<=0)
            throw new IllegalArgumentException("Select two distinct dynamic bodies: crate and pusher");
        JSONObject ground=null;int dynamic=0;
        for(Object raw:scene.getJSONArray("objects")){
            var o=(JSONObject)raw;var q=o.getJSONArray("rotation");
            if(Math.abs(q.getDouble(0))+Math.abs(q.getDouble(1))+Math.abs(q.getDouble(2))>1e-6||Math.abs(Math.abs(q.getDouble(3))-1)>1e-6)
                throw new IllegalArgumentException("Pushing currently requires axis-aligned source boxes");
            if(o.getDouble("massKg")>0){dynamic++;continue;}
            if(o.getString("sourceId").equals("floor"))ground=o;else obstacles.add(o);
        }
        if(dynamic!=2||ground==null)throw new IllegalArgumentException("Pushing requires exactly two dynamic boxes and an explicit static floor");floor=ground;
        var fs=size(floor);var fp=position(floor);
        if(Math.abs(fp[1]+fs[1]/2)>.001||fs[0]<5||fs[2]<5||fs[0]>12||fs[2]>12)
            throw new IllegalArgumentException("Use a flat floor top at Y=0, 5..12 m per horizontal axis");
        for(var body:List.of(pusher,crate)){
            var s=size(body);var p=position(body);
            if(s[0]<.4||s[0]>.8||s[2]<.4||s[2]>.8||s[1]<.3||s[1]>1||body.getDouble("massKg")<.5||body.getDouble("massKg")>4||Math.abs(p[1]-s[1]/2)>.06)
                throw new IllegalArgumentException("Supported pushing bodies: 0.4..0.8 m horizontal size, 0.3..1 m height, mass <=4 kg, on the floor");
            grid(p[0]);grid(p[2]);
        }
        pusherRadius=Math.hypot(size(pusher)[0],size(pusher)[2])/2+.025;
        crateRadius=Math.hypot(size(crate)[0],size(crate)[2])/2+.025;
        double radius=Math.max(pusherRadius,crateRadius);
        minX=(int)Math.ceil(fp[0]-fs[0]/2+radius);maxX=(int)Math.floor(fp[0]+fs[0]/2-radius);
        minZ=(int)Math.ceil(fp[2]-fs[2]/2+radius);maxZ=(int)Math.floor(fp[2]+fs[2]/2-radius);
        width=maxX-minX+1;height=maxZ-minZ+1;
        if(width<2||height<2||width*height>169)throw new IllegalArgumentException("Unsupported pushing arena bounds");
        for(var body:List.of(pusher,crate))if(!free(grid(position(body)[0]),grid(position(body)[2]),body==pusher?pusherRadius:crateRadius,-1))
            throw new IllegalArgumentException("Initial body lacks conservative obstacle/floor clearance");
        if(Math.hypot(position(pusher)[0]-position(crate)[0],position(pusher)[2]-position(crate)[2])<pusherRadius+crateRadius)
            throw new IllegalArgumentException("Initial pusher and crate overlap their planning bounds");
    }
    static int grid(double v){int n=(int)Math.round(v);if(Math.abs(v-n)>.01)throw new IllegalArgumentException("Source positions and requested displacements must align to the integer-metre pushing grid");return n;}
    static double[] position(JSONObject o){return SpatialScene.vector(o.getJSONArray("position"),20);}
    static double[] size(JSONObject o){return SpatialScene.vector(o.getJSONArray("size"),40);}
    int cell(int x,int z){return (z-minZ)*width+x-minX;}
    int x(int cell){return minX+cell%width;}int z(int cell){return minZ+cell/width;}
    boolean inside(int x,int z){return x>=minX&&x<=maxX&&z>=minZ&&z<=maxZ;}
    boolean free(int x,int z,double radius,int crateCell){
        if(!inside(x,z))return false;
        if(crateCell>=0&&Math.hypot(x-x(crateCell),z-z(crateCell))<pusherRadius+crateRadius)return false;
        for(var o:obstacles){var p=position(o);var s=size(o);if(p[1]-s[1]/2>=Math.max(size(pusher)[1],size(crate)[1])+.02)continue;
            if(Math.abs(x-p[0])<s[0]/2+radius&&Math.abs(z-p[2])<s[2]/2+radius)return false;}
        return true;
    }
    boolean clear(int from,int to,double radius,int crateCell){
        int dx=x(to)-x(from),dz=z(to)-z(from);if(Math.abs(dx)+Math.abs(dz)!=1)return false;
        for(int step=0;step<=10;step++){
            double px=x(from)+dx*step/10.0,pz=z(from)+dz*step/10.0;
            if(crateCell>=0&&Math.hypot(px-x(crateCell),pz-z(crateCell))<pusherRadius+crateRadius)return false;
            for(var o:obstacles){var p=position(o);var s=size(o);if(p[1]-s[1]/2>=Math.max(size(pusher)[1],size(crate)[1])+.02)continue;
                if(Math.abs(px-p[0])<s[0]/2+radius&&Math.abs(pz-p[2])<s[2]/2+radius)return false;}
        }
        return true;
    }
    List<Integer> walk(int from,int to,int crateCell){
        if(!free(x(from),z(from),pusherRadius,crateCell)||!free(x(to),z(to),pusherRadius,crateCell))return List.of();
        int[] prior=new int[width*height];Arrays.fill(prior,-1);var queue=new ArrayDeque<Integer>();queue.add(from);prior[from]=from;
        while(!queue.isEmpty()){
            int at=queue.remove();if(at==to)break;
            for(var d:DIRECTIONS){int nx=x(at)+d[0],nz=z(at)+d[1];if(!free(nx,nz,pusherRadius,crateCell))continue;int next=cell(nx,nz);
                if(prior[next]<0&&clear(at,next,pusherRadius,crateCell)){prior[next]=at;queue.add(next);}}
        }
        if(prior[to]<0)return List.of();var path=new ArrayList<Integer>();for(int at=to;;at=prior[at]){path.add(at);if(at==from)break;}Collections.reverse(path);return path;
    }
    JSONObject intent(JSONArray displacement,double initialForce,double maxForce){
        var delta=SpatialScene.vector(displacement,4);if(Math.abs(delta[1])>1e-9)throw new IllegalArgumentException("Push displacement is horizontal");int dx=grid(delta[0]),dz=grid(delta[2]);
        if(Math.abs(dx)+Math.abs(dz)<1||Math.abs(dx)+Math.abs(dz)>8)throw new IllegalArgumentException("Request 1..8 Manhattan metres of pushing");
        RampDefinition.range(initialForce,2,60,"initialForceN");RampDefinition.range(maxForce,initialForce,60,"maxForceN");
        var p=position(crate);int tx=grid(p[0])+dx,tz=grid(p[2])+dz;
        return new JSONObject().put("binding",SpatialStore.binding(scene)).put("objectId",crate.getString("id")).put("pusherId",pusher.getString("id"))
            .put("displacementM",displacement).put("targetM",new JSONArray(new double[]{tx,size(crate)[1]/2,tz})).put("initialForceN",initialForce).put("maxForceN",maxForce)
            .put("gridM",1).put("scope",SCOPE);
    }
    private record Node(int crate,int pusher,int depth,Node parent,int[] direction,List<Integer> walk){}
    JSONObject plan(JSONObject intent){
        var cp=position(crate);var pp=position(pusher);int start=cell(grid(cp[0]),grid(cp[2])),pusherStart=cell(grid(pp[0]),grid(pp[2]));var target=intent.getJSONArray("targetM");int tx=target.getInt(0),tz=target.getInt(2);
        var result=new JSONObject().put("version",VERSION).put("intent",intent).put("sourceSha256",scene.getString("sourceSha256")).put("maxStates",MAX_STATES).put("maxPushes",MAX_PUSHES);
        if(!free(tx,tz,crateRadius,-1))return result.put("passed",false).put("expanded",0).put("reason","Target lacks conservative floor/obstacle clearance");
        int destination=cell(tx,tz);var queue=new ArrayDeque<Node>();var visited=new HashSet<Long>();queue.add(new Node(start,pusherStart,0,null,null,List.of()));visited.add((long)start*width*height+pusherStart);Node found=null;int expanded=0;
        while(!queue.isEmpty()&&expanded<MAX_STATES){
            if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException("Push planning cancelled");var node=queue.remove();expanded++;if(node.crate==destination){found=node;break;}if(node.depth>=MAX_PUSHES)continue;
            for(var dir:DIRECTIONS){int nx=x(node.crate)+dir[0],nz=z(node.crate)+dir[1],bx=x(node.crate)-dir[0],bz=z(node.crate)-dir[1];
                if(!free(nx,nz,crateRadius,-1)||!inside(bx,bz))continue;int next=cell(nx,nz),behind=cell(bx,bz);if(!clear(node.crate,next,crateRadius,-1))continue;
                var path=walk(node.pusher,behind,node.crate);if(path.isEmpty())continue;long key=(long)next*width*height+node.crate;
                if(visited.add(key))queue.add(new Node(next,node.crate,node.depth+1,node,dir,path));
            }
        }
        result.put("expanded",expanded).put("visited",visited.size());
        if(found==null)return result.put("passed",false).put("reason",queue.isEmpty()?"No reachable standing/pushing sequence":"Configuration search budget exhausted");
        var reverse=new ArrayList<Node>();for(var n=found;n.parent!=null;n=n.parent)reverse.add(n);Collections.reverse(reverse);
        var pushes=new JSONArray();var actions=new JSONArray();
        for(var n:reverse){
            for(int at:n.walk)actions.put(new JSONObject().put("kind","approach").put("target",new JSONArray(new double[]{x(at),size(pusher)[1]/2,z(at)})));
            var targetPoint=new JSONArray(new double[]{x(n.crate),size(crate)[1]/2,z(n.crate)});
            pushes.put(new JSONObject().put("from",n.parent.crate).put("to",n.crate).put("standing",n.walk.get(n.walk.size()-1)).put("walk",new JSONArray(n.walk)).put("direction",new JSONArray(n.direction)));
            actions.put(new JSONObject().put("kind","push").put("target",targetPoint).put("direction",new JSONArray(n.direction)));
        }
        result.put("passed",true).put("reason","Every push has a reachable standing pose and conservatively clear swept crate path").put("pushes",pushes).put("actions",actions)
            .put("arena",new JSONObject().put("minX",minX).put("maxX",maxX).put("minZ",minZ).put("maxZ",maxZ));
        verify(result);return result;
    }
    void verify(JSONObject plan){
        if(!plan.getBoolean("passed")||!plan.getString("version").equals(VERSION)||!plan.getString("sourceSha256").equals(scene.getString("sourceSha256"))
            ||!plan.getJSONObject("intent").getJSONObject("binding").similar(SpatialStore.binding(scene)))throw new IllegalStateException("Push plan/source mismatch");
        int currentCrate=cell(grid(position(crate)[0]),grid(position(crate)[2])),currentPusher=cell(grid(position(pusher)[0]),grid(position(pusher)[2]));
        if(plan.getJSONArray("pushes").length()<1||plan.getJSONArray("pushes").length()>MAX_PUSHES)throw new IllegalStateException("Push count out of bounds");
        var expectedActions=new JSONArray();
        for(Object raw:plan.getJSONArray("pushes")){
            var push=(JSONObject)raw;int from=push.getInt("from"),to=push.getInt("to"),standing=push.getInt("standing");var dir=push.getJSONArray("direction");
            if(from!=currentCrate||dir.length()!=2||Math.abs(dir.getInt(0))+Math.abs(dir.getInt(1))!=1||x(to)!=x(from)+dir.getInt(0)||z(to)!=z(from)+dir.getInt(1)
                ||x(standing)!=x(from)-dir.getInt(0)||z(standing)!=z(from)-dir.getInt(1)||!free(x(to),z(to),crateRadius,-1)||!clear(from,to,crateRadius,-1))throw new IllegalStateException("Invalid swept pushing step");
            var walk=push.getJSONArray("walk");if(walk.isEmpty()||walk.getInt(0)!=currentPusher||walk.getInt(walk.length()-1)!=standing)throw new IllegalStateException("Missing standing-pose approach");
            int previous=currentPusher;for(int i=0;i<walk.length();i++){int at=walk.getInt(i);if(!free(x(at),z(at),pusherRadius,currentCrate)||i>0&&!clear(previous,at,pusherRadius,currentCrate))throw new IllegalStateException("Approach walks through an obstacle/crate");previous=at;expectedActions.put(new JSONObject().put("kind","approach").put("target",new JSONArray(new double[]{x(at),size(pusher)[1]/2,z(at)})));}
            expectedActions.put(new JSONObject().put("kind","push").put("target",new JSONArray(new double[]{x(to),size(crate)[1]/2,z(to)})).put("direction",dir));currentPusher=from;currentCrate=to;
        }
        var target=plan.getJSONObject("intent").getJSONArray("targetM");if(x(currentCrate)!=target.getInt(0)||z(currentCrate)!=target.getInt(2)||!expectedActions.similar(plan.getJSONArray("actions")))throw new IllegalStateException("Plan does not reach requested target");
    }
}
