package fraymus.geometry;

import java.util.*;

/** Bounded N-D BSP rooms, spanning-tree corridors and actual grid navigation. No renderer dependency. */
public final class Dungeon {
    public record Spec(int dimensions,long seed,int side,int rooms) {
        public Spec {if(dimensions<2||dimensions>4||side<12||side>40||rooms<4||rooms>24||StrictMath.pow(side,dimensions)>65536)throw new IllegalArgumentException("Dungeon needs2..4 dimensions,12..40 cells/axis,4..24 rooms and at most65536 grid cells");}
    }
    public record Box(int[] low,int[] high) {public Box {low=low.clone();high=high.clone();} @Override public int[] low(){return low.clone();} @Override public int[] high(){return high.clone();}}
    public record Corridor(int from,int to,List<Integer> cells) {}
    public record Result(Spec spec,List<Box> rooms,List<Corridor> corridors,List<Integer> floor,List<Integer> path,int reachable) {}
    public static int encode(int[] coordinate,int side){int id=0,stride=1;for(int x:coordinate){if(x<0||x>=side)throw new IllegalArgumentException("Grid coordinate outside map");id+=x*stride;stride*=side;}return id;}
    public static int[] decode(int id,int side,int dimensions){if(id<0||id>=StrictMath.pow(side,dimensions))throw new IllegalArgumentException("Invalid grid cell");int[] c=new int[dimensions];for(int k=0;k<dimensions;k++){c[k]=id%side;id/=side;}return c;}
    public static int[] center(Box box){int[] c=new int[box.low.length];for(int k=0;k<c.length;k++)c[k]=(box.low[k]+box.high[k])/2;return c;}
    public static Result generate(Spec s){
        var rng=new Random(s.seed());var leaves=new ArrayList<Box>();int[] hi=new int[s.dimensions()];Arrays.fill(hi,s.side()-1);leaves.add(new Box(new int[s.dimensions()],hi));
        while(leaves.size()<s.rooms()){
            NDSpace.interrupted();int choice=-1,axis=-1,longest=0;
            for(int i=0;i<leaves.size();i++)for(int k=0;k<s.dimensions();k++){int extent=leaves.get(i).high[k]-leaves.get(i).low[k]+1;if(extent>=6&&extent>longest){choice=i;axis=k;longest=extent;}}
            if(choice<0)throw new IllegalArgumentException("Requested room count does not fit the minimum BSP leaves; use a larger map or fewer rooms");
            Box box=leaves.remove(choice);int min=box.low[axis]+3,max=box.high[axis]-2,cut=min+rng.nextInt(max-min+1);int[] leftHi=box.high.clone(),rightLo=box.low.clone();leftHi[axis]=cut-1;rightLo[axis]=cut;leaves.add(new Box(box.low,leftHi));leaves.add(new Box(rightLo,box.high));
        }
        var rooms=new ArrayList<Box>();var floor=new BitSet((int)StrictMath.pow(s.side(),s.dimensions()));
        for(Box leaf:leaves){int[] low=new int[s.dimensions()],high=new int[s.dimensions()];for(int k=0;k<low.length;k++){int extent=leaf.high[k]-leaf.low[k]+1,width=Math.min(3,extent-2);low[k]=leaf.low[k]+1+rng.nextInt(extent-width-1);high[k]=low[k]+width-1;}Box room=new Box(low,high);rooms.add(room);fill(room,s.side(),floor);}
        boolean[] joined=new boolean[rooms.size()];joined[0]=true;var corridors=new ArrayList<Corridor>();
        for(int edge=1;edge<rooms.size();edge++){
            double best=Double.POSITIVE_INFINITY;int a=-1,b=-1;
            for(int i=0;i<rooms.size();i++)if(joined[i])for(int j=0;j<rooms.size();j++)if(!joined[j]){int[] x=center(rooms.get(i)),y=center(rooms.get(j));double distance=0;for(int k=0;k<x.length;k++)distance+=(x[k]-y[k])*(x[k]-y[k]);if(distance<best){best=distance;a=i;b=j;}}
            int[] c=center(rooms.get(a)),target=center(rooms.get(b)),axes=new int[s.dimensions()];for(int k=0;k<axes.length;k++)axes[k]=k;for(int k=axes.length-1;k>0;k--){int j=rng.nextInt(k+1),swap=axes[k];axes[k]=axes[j];axes[j]=swap;}
            var cells=new ArrayList<Integer>();cells.add(encode(c,s.side()));for(int axis:axes)while(c[axis]!=target[axis]){c[axis]+=Integer.signum(target[axis]-c[axis]);int id=encode(c,s.side());floor.set(id);cells.add(id);}corridors.add(new Corridor(a,b,List.copyOf(cells)));joined[b]=true;
        }
        if(floor.cardinality()>4096)throw new IllegalArgumentException("Generated floor exceeds4096 visible/exported cells; choose fewer rooms or a smaller map");
        int start=encode(center(rooms.getFirst()),s.side()),end=encode(center(rooms.getLast()),s.side());int[] parent=new int[(int)StrictMath.pow(s.side(),s.dimensions())];Arrays.fill(parent,-2);var queue=new ArrayDeque<Integer>();queue.add(start);parent[start]=-1;int reachable=0;
        while(!queue.isEmpty()){NDSpace.interrupted();int id=queue.remove();reachable++;for(int neighbor:neighbors(id,s))if(floor.get(neighbor)&&parent[neighbor]==-2){parent[neighbor]=id;queue.add(neighbor);}}
        if(parent[end]==-2)throw new IllegalStateException("Generated map did not connect target");var path=new ArrayList<Integer>();for(int id=end;id!=-1;id=parent[id])path.add(id);Collections.reverse(path);
        var result=new Result(s,List.copyOf(rooms),List.copyOf(corridors),floor.stream().boxed().toList(),List.copyOf(path),reachable);verify(result);return result;
    }
    private static void fill(Box b,int side,BitSet floor){int[] c=b.low.clone();while(true){floor.set(encode(c,side));int k=0;while(k<c.length&&c[k]==b.high[k]){c[k]=b.low[k];k++;}if(k==c.length)return;c[k]++;}}
    public static List<Integer> neighbors(int id,Spec s){int[] c=decode(id,s.side(),s.dimensions());var all=new ArrayList<Integer>();for(int k=0,stride=1;k<c.length;stride*=s.side(),k++){if(c[k]>0)all.add(id-stride);if(c[k]+1<s.side())all.add(id+stride);}return all;}
    public static void verify(Result r){
        Spec s=r.spec();if(r.rooms().size()!=s.rooms()||r.corridors().size()!=s.rooms()-1||r.floor().isEmpty()||r.floor().size()>4096||r.path().isEmpty())throw new IllegalStateException("Dungeon accounting failed");
        var cells=new HashSet<>(r.floor());if(cells.size()!=r.floor().size())throw new IllegalStateException("Repeated floor IDs");for(int id:cells)decode(id,s.side(),s.dimensions());
        for(Box room:r.rooms()){if(room.low.length!=s.dimensions()||room.high.length!=s.dimensions())throw new IllegalStateException("Room dimension mismatch");for(int k=0;k<s.dimensions();k++)if(room.low[k]<0||room.high[k]>=s.side()||room.low[k]>room.high[k])throw new IllegalStateException("Room bounds outside grid");var required=new BitSet();fill(room,s.side(),required);for(int cell:required.stream().toArray())if(!cells.contains(cell))throw new IllegalStateException("A declared room contains a wall cell");}
        var connected=new HashSet<Integer>();connected.add(0);for(int pass=0;pass<r.rooms().size();pass++)for(Corridor c:r.corridors()){if(c.from()<0||c.to()<0||c.from()>=r.rooms().size()||c.to()>=r.rooms().size()||c.from()==c.to()||c.cells().isEmpty()||c.cells().getFirst()!=encode(center(r.rooms().get(c.from())),s.side())||c.cells().getLast()!=encode(center(r.rooms().get(c.to())),s.side()))throw new IllegalStateException("Corridor endpoints do not bind actual rooms");for(int i=0;i<c.cells().size();i++)if(!cells.contains(c.cells().get(i))||i>0&&!neighbors(c.cells().get(i-1),s).contains(c.cells().get(i)))throw new IllegalStateException("Corridor crosses a wall or skips a grid step");if(connected.contains(c.from()))connected.add(c.to());if(connected.contains(c.to()))connected.add(c.from());}if(connected.size()!=s.rooms())throw new IllegalStateException("Room/corridor graph is disconnected");
        for(int a=0;a<r.rooms().size();a++)for(int b=a+1;b<r.rooms().size();b++){boolean separated=false;for(int k=0;k<s.dimensions();k++)if(r.rooms().get(a).high[k]<r.rooms().get(b).low[k]||r.rooms().get(b).high[k]<r.rooms().get(a).low[k])separated=true;if(!separated)throw new IllegalStateException("Overlapping BSP rooms");}
        int start=encode(center(r.rooms().getFirst()),s.side()),end=encode(center(r.rooms().getLast()),s.side());
        if(r.path().getFirst()!=start||r.path().getLast()!=end)throw new IllegalStateException("Path endpoints differ from selected rooms");
        for(int i=0;i<r.path().size();i++)if(!cells.contains(r.path().get(i))||i>0&&!neighbors(r.path().get(i-1),s).contains(r.path().get(i)))throw new IllegalStateException("Route leaves walkable cells or skips a grid step");
        // Separate flood implementation verifies actual cell connectivity and shortest length.
        var visited=new HashMap<Integer,Integer>();var queue=new ArrayDeque<Integer>();queue.add(start);visited.put(start,0);
        while(!queue.isEmpty()){int id=queue.remove();int[] p=decode(id,s.side(),s.dimensions());for(int k=0;k<p.length;k++)for(int sign:new int[]{-1,1}){int next=p[k]+sign;if(next<0||next>=s.side())continue;int[] c=p.clone();c[k]=next;int n=encode(c,s.side());if(cells.contains(n)&&!visited.containsKey(n)){visited.put(n,visited.get(id)+1);queue.add(n);}}}
        if(visited.size()!=cells.size()||r.reachable()!=cells.size()||visited.getOrDefault(end,-1)!=r.path().size()-1)throw new IllegalStateException("Connectivity or shortest-path verification failed");
    }
}
