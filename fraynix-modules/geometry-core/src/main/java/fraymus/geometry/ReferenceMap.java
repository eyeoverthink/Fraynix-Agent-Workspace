package fraymus.geometry;

import java.util.*;

/** Learned directed tile adjacency, hard pins and bounded minimum-conflicts repair. JDK only. */
public final class ReferenceMap {
    public record Pin(int x,int y,int tile) {}
    public record Rules(int width,int height,int[] source,boolean[] walkable,int[] frequency,boolean[][][] allowed) {}
    public record Result(int width,int height,long seed,int[] cells,List<Pin> pins,List<Integer> path,int conflicts,int attempts,int repairs,boolean passed,String reason) {}
    private static final int[] DX={1,0,-1,0},DY={0,1,0,-1};
    public static Rules learn(int width,int height,int[] source,boolean[] walkable){
        if(width<3||height<3||width>32||height>32||source.length!=width*height||walkable.length<2||walkable.length>8)throw new IllegalArgumentException("Reference needs3..32 cells/axis and2..8 tile classes");
        int n=walkable.length;int[] frequencies=new int[n];boolean[][][] allowed=new boolean[4][n][n];
        for(int y=0;y<height;y++)for(int x=0;x<width;x++){int tile=source[y*width+x];if(tile<0||tile>=n)throw new IllegalArgumentException("Unknown reference tile");frequencies[tile]++;for(int d=0;d<4;d++){int nx=x+DX[d],ny=y+DY[d];if(nx>=0&&ny>=0&&nx<width&&ny<height){int other=source[ny*width+nx];if(other<0||other>=n)throw new IllegalArgumentException("Unknown reference tile");allowed[d][tile][other]=true;}}}
        if(Arrays.stream(frequencies).anyMatch(x->x==0))throw new IllegalArgumentException("Every declared palette class must occur in the reference");
        boolean open=false,closed=false;for(boolean v:walkable){open|=v;closed|=!v;}if(!open||!closed)throw new IllegalArgumentException("Declare both walkable and blocked tile classes");
        return new Rules(width,height,source.clone(),walkable.clone(),frequencies,allowed);
    }
    public static Result generate(Rules rules,int width,int height,long seed,List<Pin> declared){
        return candidate(rules,width,height,seed,declared,8,20_000);
    }
    public static Result candidate(Rules rules,int width,int height,long seed,List<Pin> declared,int restarts,int iterations){
        if(restarts<1||restarts>8||iterations<1||iterations>20_000)throw new IllegalArgumentException("Bounded local repair budget required");
        if(width<8||height<8||width>24||height>24||seed<0||seed>1_000_000_000L||declared.size()>64)throw new IllegalArgumentException("Output needs8..24 cells/axis, bounded seed and at most64 user pins");
        int blocked=0;while(rules.walkable[blocked])blocked++;int[] pinned=new int[width*height];Arrays.fill(pinned,-1);
        for(int y=0;y<height;y++)for(int x=0;x<width;x++)if(x==0||y==0||x==width-1||y==height-1)pinned[y*width+x]=blocked;
        for(Pin p:declared){if(p.x<0||p.y<0||p.x>=width||p.y>=height||p.tile<0||p.tile>=rules.walkable.length)throw new IllegalArgumentException("Pin is outside the map or palette");int id=p.y*width+p.x;if(pinned[id]>=0&&pinned[id]!=p.tile)throw new IllegalArgumentException("Contradictory hard pin or boundary");pinned[id]=p.tile;}
        int start=width+1,end=(height-2)*width+width-2;int floor=0;while(!rules.walkable[floor])floor++;
        for(int id:new int[]{start,end}){if(pinned[id]>=0&&!rules.walkable[pinned[id]])throw new IllegalArgumentException("Spawn and goal must be walkable");if(pinned[id]<0)pinned[id]=floor;}
        var pins=new ArrayList<Pin>();for(int i=0;i<pinned.length;i++)if(pinned[i]>=0)pins.add(new Pin(i%width,i/width,pinned[i]));
        var rng=new Random(seed);Result best=null;int repairs=0;
        for(int attempt=1;attempt<=restarts;attempt++){
            int[] grid=new int[pinned.length];for(int i=0;i<grid.length;i++)grid[i]=pinned[i]>=0?pinned[i]:weighted(rng,rules.frequency,null);
            for(int iteration=0;iteration<iterations;iteration++){
                if((iteration&63)==0)NDSpace.interrupted();var bad=new ArrayList<Integer>();int count=0;
                for(int i=0;i<grid.length;i++){int c=local(rules,grid,width,height,i,grid[i]);count+=c;if(c>0&&pinned[i]<0)bad.add(i);}
                if(count==0){var path=path(grid,width,height,rules.walkable,start,end);var result=new Result(width,height,seed,grid.clone(),List.copyOf(pins),path,0,attempt,repairs,!path.isEmpty(),path.isEmpty()?"Adjacency satisfied but spawn and goal disconnected":"Adjacency, hard pins and route verified");if(!path.isEmpty()){verify(rules,result);return result;}if(best==null||best.conflicts>0)best=result;break;}
                if(best==null||count<best.conflicts)best=new Result(width,height,seed,grid.clone(),List.copyOf(pins),List.of(),count,attempt,repairs,false,"Unresolved adjacency conflicts");
                if(bad.isEmpty())break;int cell=bad.get(rng.nextInt(bad.size())),minimum=Integer.MAX_VALUE;boolean[] choices=new boolean[rules.walkable.length];
                for(int tile=0;tile<choices.length;tile++){int c=local(rules,grid,width,height,cell,tile);if(c<minimum){Arrays.fill(choices,false);minimum=c;}if(c==minimum)choices[tile]=true;}
                grid[cell]=weighted(rng,rules.frequency,choices);repairs++;
            }
        }
        return new Result(width,height,seed,best.cells,best.pins,best.path,best.conflicts,restarts,repairs,false,best.reason+"; finite"+restarts+"×"+iterations+" repair budget exhausted");
    }
    private static int weighted(Random r,int[] frequency,boolean[] eligible){int total=0;for(int i=0;i<frequency.length;i++)if(eligible==null||eligible[i])total+=frequency[i];int pick=r.nextInt(total);for(int i=0;i<frequency.length;i++)if(eligible==null||eligible[i]){pick-=frequency[i];if(pick<0)return i;}throw new AssertionError();}
    private static int local(Rules r,int[] grid,int w,int h,int id,int tile){int x=id%w,y=id/w,errors=0;for(int d=0;d<4;d++){int nx=x+DX[d],ny=y+DY[d];if(nx>=0&&ny>=0&&nx<w&&ny<h&&!r.allowed[d][tile][grid[ny*w+nx]])errors++;}return errors;}
    public static List<Integer> path(int[] grid,int w,int h,boolean[] walkable,int start,int end){
        if(start<0||end<0||start>=grid.length||end>=grid.length||!walkable[grid[start]]||!walkable[grid[end]])return List.of();int[] parent=new int[grid.length];Arrays.fill(parent,-2);var queue=new ArrayDeque<Integer>();queue.add(start);parent[start]=-1;
        while(!queue.isEmpty()){int id=queue.remove();if(id==end)break;for(int d=0;d<4;d++){int x=id%w+DX[d],y=id/w+DY[d];if(x>=0&&y>=0&&x<w&&y<h){int n=y*w+x;if(parent[n]==-2&&walkable[grid[n]]){parent[n]=id;queue.add(n);}}}}
        if(parent[end]==-2)return List.of();var result=new ArrayList<Integer>();for(int id=end;id!=-1;id=parent[id])result.add(id);Collections.reverse(result);return List.copyOf(result);
    }
    public static void verify(Rules r,Result result){
        if(result.cells.length!=result.width*result.height||!result.passed)throw new IllegalStateException("No successful generated map");
        // Independently enumerate directed edges and a second flood distance, not the repair score.
        int w=result.width,h=result.height;for(int y=0;y<h;y++)for(int x=0;x<w;x++){int id=y*w+x,t=result.cells[id];if(t<0||t>=r.walkable.length)throw new IllegalStateException("Unknown generated class");if(x+1<w&&(!r.allowed[0][t][result.cells[id+1]]||!r.allowed[2][result.cells[id+1]][t]))throw new IllegalStateException("Horizontal adjacency violation");if(y+1<h&&(!r.allowed[1][t][result.cells[id+w]]||!r.allowed[3][result.cells[id+w]][t]))throw new IllegalStateException("Vertical adjacency violation");}
        for(Pin p:result.pins)if(result.cells[p.y*w+p.x]!=p.tile)throw new IllegalStateException("Hard pin changed");
        if(result.path.isEmpty())throw new IllegalStateException("Missing route");int start=result.path.getFirst(),end=result.path.getLast();var distance=new HashMap<Integer,Integer>();var queue=new ArrayDeque<Integer>();queue.add(start);distance.put(start,0);
        while(!queue.isEmpty()){int id=queue.remove();for(int n:new int[]{id-1,id+1,id-w,id+w})if(n>=0&&n<result.cells.length&&Math.abs(n%w-id%w)+Math.abs(n/w-id/w)==1&&r.walkable[result.cells[n]]&&!distance.containsKey(n)){distance.put(n,distance.get(id)+1);queue.add(n);}}
        if(distance.getOrDefault(end,-1)!=result.path.size()-1)throw new IllegalStateException("Route is not shortest");for(int i=0;i<result.path.size();i++){int id=result.path.get(i);if(!r.walkable[result.cells[id]]||i>0&&Math.abs(id%w-result.path.get(i-1)%w)+Math.abs(id/w-result.path.get(i-1)/w)!=1)throw new IllegalStateException("Route crossed a blocked cell or jumped");}
    }
}
