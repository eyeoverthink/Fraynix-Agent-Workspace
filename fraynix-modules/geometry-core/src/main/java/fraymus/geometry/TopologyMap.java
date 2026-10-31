package fraymus.geometry;

import java.util.*;

/** Finite selection of original adjacency candidates. Never edits a generated cell. */
public final class TopologyMap {
    public static final String PROFILE="connected-grid-v1";
    public static final int CANDIDATES=32,RESTARTS=2,REPAIRS=4000;
    private static final int[] DX={1,0,-1,0},DY={0,1,0,-1};
    public record Constraints(boolean connected,double minFloorFraction,double maxFloorFraction,int minRouteSteps,int maxRouteSteps){
        public Constraints {
            if(!Double.isFinite(minFloorFraction)||!Double.isFinite(maxFloorFraction)||minFloorFraction<.05||maxFloorFraction>.95||minFloorFraction>maxFloorFraction||minRouteSteps<0||maxRouteSteps>128||minRouteSteps>maxRouteSteps)throw new IllegalArgumentException("Floor fractions require0.05≤min≤max≤0.95 and route steps0≤min≤max≤128");
        }
    }
    public record Metrics(int floorCells,int reachableCells,int components,double floorFraction,int shortestSteps,int edges,int cycleRank,int deadEnds,int adjacencyViolations,int pinViolations,boolean routeValid){}
    public record Audit(boolean passed,Metrics metrics,List<String> failures){}
    public record Trial(int index,long candidateSeed,boolean localPassed,int localAttempts,int localRepairs,Audit audit){}
    public record Result(ReferenceMap.Result candidate,Audit audit,List<Trial> trials,int totalRepairs,boolean passed,String reason){}

    public static Result generate(ReferenceMap.Rules rules,int width,int height,long seed,List<ReferenceMap.Pin> pins,Constraints constraints){
        if(seed<0||seed>1_000_000_000L)throw new IllegalArgumentException("Bounded original seed required");
        Result best=null;var trials=new ArrayList<Trial>();int repairs=0;
        for(int i=0;i<CANDIDATES;i++){
            NDSpace.interrupted();long candidateSeed=Math.floorMod(seed+104729L*i,1_000_000_001L);
            var candidate=ReferenceMap.candidate(rules,width,height,candidateSeed,pins,RESTARTS,REPAIRS);
            var audit=audit(rules,candidate,constraints);repairs+=candidate.repairs();trials.add(new Trial(i+1,candidateSeed,candidate.passed(),candidate.attempts(),candidate.repairs(),audit));
            var result=new Result(candidate,audit,List.copyOf(trials),repairs,audit.passed(),audit.passed()?"Original adjacency, hard pins and requested global topology verified":String.join("; ",audit.failures()));
            if(best==null||score(result)<score(best))best=result;
            if(audit.passed())return result;
            // These geometric bounds cannot change with another random candidate.
            int minimum=width+height-6,maximumFloor=(width-2)*(height-2);
            if(constraints.maxRouteSteps()<minimum||constraints.minFloorFraction()>maximumFloor/(double)(width*height)||fixedConflict(rules,candidate))break;
        }
        return new Result(best.candidate(),best.audit(),List.copyOf(trials),repairs,false,"No candidate met the original global contract; "+String.join("; ",best.audit().failures())+"; finite selection budget exhausted");
    }
    private static double score(Result r){var m=r.audit().metrics();return m.adjacencyViolations()*100000.0+m.pinViolations()*100000.0+(m.routeValid()?0:10000)+r.audit().failures().size()*100+(m.floorCells()-m.reachableCells());}
    private static boolean fixedConflict(ReferenceMap.Rules r,ReferenceMap.Result candidate){var fixed=new HashMap<Integer,Integer>();int w=candidate.width();for(var p:candidate.pins())fixed.put(p.y()*w+p.x(),p.tile());for(var p:candidate.pins())for(int d=0;d<4;d++){int x=p.x()+DX[d],y=p.y()+DY[d];if(x>=0&&y>=0&&x<w&&y<candidate.height()){var other=fixed.get(y*w+x);if(other!=null&&!r.allowed()[d][p.tile()][other])return true;}}return false;}

    /** Independent edge enumeration and graph traversal; no cached solver score or connectivity is trusted. */
    public static Audit audit(ReferenceMap.Rules rules,ReferenceMap.Result result,Constraints c){
        int w=result.width(),h=result.height();int[] cells=result.cells();boolean[] open=rules.walkable();
        if(w<8||h<8||w>24||h>24||cells.length!=w*h)throw new IllegalArgumentException("Bounded original grid required");
        for(int tile:cells)if(tile<0||tile>=open.length)throw new IllegalArgumentException("Unknown grid class");
        int floor=0,edges=0,deadEnds=0,adjacency=0,pinErrors=0,blocked=0;while(open[blocked])blocked++;
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            int id=y*w+x,tile=cells[id],degree=0;
            if((x==0||y==0||x==w-1||y==h-1)&&tile!=blocked)pinErrors++;
            for(int d=0;d<4;d++){int nx=x+DX[d],ny=y+DY[d];if(nx<0||ny<0||nx>=w||ny>=h)continue;int other=cells[ny*w+nx];if(!rules.allowed()[d][tile][other])adjacency++;if(open[other])degree++;}
            if(open[tile]){floor++;if(degree==1)deadEnds++;if(x+1<w&&open[cells[id+1]])edges++;if(y+1<h&&open[cells[id+w]])edges++;}
        }
        for(var pin:result.pins()){if(pin.x()<0||pin.y()<0||pin.x()>=w||pin.y()>=h||cells[pin.y()*w+pin.x()]!=pin.tile())pinErrors++;}
        int start=w+1,end=(h-2)*w+w-2;int[] distance=flood(cells,w,h,open,start);int reachable=0;for(int d:distance)if(d>=0)reachable++;
        boolean[] visited=new boolean[cells.length];int components=0;
        for(int i=0;i<cells.length;i++)if(open[cells[i]]&&!visited[i]){components++;var group=flood(cells,w,h,open,i);for(int j=0;j<group.length;j++)if(group[j]>=0)visited[j]=true;}
        var path=result.path();boolean route=!path.isEmpty()&&path.getFirst()==start&&path.getLast()==end&&distance[end]>=0&&path.size()-1==distance[end];
        for(int i=0;i<path.size();i++){int id=path.get(i);if(id<0||id>=cells.length||!open[cells[id]]){route=false;continue;}if(i>0){int previous=path.get(i-1);if(Math.abs(id%w-previous%w)+Math.abs(id/w-previous/w)!=1)route=false;}}
        var m=new Metrics(floor,reachable,components,floor/(double)cells.length,distance[end],edges,edges-floor+components,deadEnds,adjacency,pinErrors,route);var failures=new ArrayList<String>();
        if(adjacency!=0)failures.add("Directional adjacency violations="+adjacency);
        if(pinErrors!=0)failures.add("Original hard pin violations="+pinErrors);
        if(!route)failures.add("Original spawn-to-goal shortest route is not valid");
        if(c.connected()&&(components!=1||reachable!=floor))failures.add("All floor must connect: components="+components+", reachable="+reachable+"/"+floor);
        if(m.floorFraction()<c.minFloorFraction()||m.floorFraction()>c.maxFloorFraction())failures.add("Floor fraction "+m.floorFraction()+" outside ["+c.minFloorFraction()+","+c.maxFloorFraction()+"]");
        if(m.shortestSteps()<c.minRouteSteps()||m.shortestSteps()>c.maxRouteSteps())failures.add("Shortest route "+m.shortestSteps()+" outside ["+c.minRouteSteps()+","+c.maxRouteSteps()+"]");
        return new Audit(failures.isEmpty(),m,List.copyOf(failures));
    }
    private static int[] flood(int[] cells,int w,int h,boolean[] open,int start){
        int[] distance=new int[cells.length];Arrays.fill(distance,-1);if(!open[cells[start]])return distance;var queue=new ArrayDeque<Integer>();queue.add(start);distance[start]=0;
        while(!queue.isEmpty()){int id=queue.remove();for(int next:new int[]{id-1,id+1,id-w,id+w})if(next>=0&&next<cells.length&&Math.abs(next%w-id%w)+Math.abs(next/w-id/w)==1&&open[cells[next]]&&distance[next]<0){distance[next]=distance[id]+1;queue.add(next);}}
        return distance;
    }
}
