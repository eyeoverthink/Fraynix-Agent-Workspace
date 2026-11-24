package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reconstructs an editable visual hull and optionally carves observed free space. */
final class SpatialReconstruction {
    static final int LIMIT=1_200_000;
    static final String FORMAT="fraynix.orthographic-silhouettes-v1";
    static final String POLICY="axis-visual-hull-v1";
    static final String FORMAT_DEPTH="fraynix.orthographic-silhouettes-depth-v2";
    static final String POLICY_DEPTH="axis-visual-hull-depth-v2";
    static final String FORMAT_MULTIDEPTH="fraynix.orthographic-silhouettes-multidepth-v3";
    static final String POLICY_MULTIDEPTH="axis-visual-hull-multidepth-v3";

    static final String FORMAT_AXIS_DEPTH="fraynix.orthographic-silhouettes-axis-depth-v4";
    static final String POLICY_AXIS_DEPTH="axis-visual-hull-axis-depth-v4";

    record Result(JSONObject definition,JSONObject evidence) {}
    private record Point(int x,int y,int z) {}

    private SpatialReconstruction() {}

    static Result build(byte[] bytes) throws Exception {
        if (bytes.length==0 || bytes.length>LIMIT) throw new IllegalArgumentException("Choose a silhouette set up to 1.2 MB.");
        var input=new JSONObject(new String(bytes,StandardCharsets.UTF_8));
        String format=input.getString("format");
        boolean allAxes=FORMAT_AXIS_DEPTH.equals(format);
        boolean multi=allAxes || FORMAT_MULTIDEPTH.equals(format);
        boolean withDepth=multi || FORMAT_DEPTH.equals(format);
        if (!withDepth && !FORMAT.equals(format))
            throw new IllegalArgumentException("Choose a registered orthographic silhouette set.");
        if (multi) SpatialScene.keys(input,"format","title","grid","cellM","views","depthViews");
        else if (withDepth) SpatialScene.keys(input,"format","title","grid","cellM","views","depth");
        else SpatialScene.keys(input,"format","title","grid","cellM","views");
        String title=SpatialScene.text(input,"title",80);
        int n=(int)SpatialScene.number(input,"grid",8,22);
        if (input.getDouble("grid")!=n) throw new IllegalArgumentException("Grid size must be an integer.");
        double step=SpatialScene.number(input,"cellM",.025,.20);
        var views=input.getJSONObject("views");
        if (!views.keySet().equals(java.util.Set.of("x","y","z")))
            throw new IllegalArgumentException("Supply one registered PNG per positive X, Y and Z view.");
        boolean[][][] masks=new boolean[3][][];
        var hashes=new JSONObject();
        for (int axis=0;axis<3;axis++) {
            String name="xyz".substring(axis,axis+1);
            byte[] image=Base64.getDecoder().decode(views.getString(name));
            masks[axis]=mask(image,n);
            hashes.put(name,MapDefinition.sha(image));
        }

        var hull=new boolean[n][n][n];
        for (int x=0;x<n;x++) for (int y=0;y<n;y++) for (int z=0;z<n;z++)
            hull[x][y][z]=masks[0][n-1-y][n-1-z] && masks[1][z][x] && masks[2][n-1-y][x];
        JSONObject depth=null;
        if (multi) depth=SpatialReconstructionDepth.fuse(input.getJSONArray("depthViews"),hull,masks,step,allAxes);
        else if (withDepth) depth=SpatialReconstructionDepth.carve(input.getJSONObject("depth"),hull,masks[2],step);
        var projected=new boolean[3][n][n];
        int[] low={n,n,n},high={0,0,0};
        int solid=0;
        for (int x=0;x<n;x++) for (int y=0;y<n;y++) for (int z=0;z<n;z++) {
            if (!hull[x][y][z]) continue;
            projected[0][n-1-y][n-1-z]=true;
            projected[1][z][x]=true;
            projected[2][n-1-y][x]=true;
            solid++;
            low[0]=Math.min(low[0],x);low[1]=Math.min(low[1],y);low[2]=Math.min(low[2],z);
            high[0]=Math.max(high[0],x+1);high[1]=Math.max(high[1],y+1);high[2]=Math.max(high[2],z+1);
        }
        if (solid==0) throw new IllegalArgumentException("The aligned views have no shared volume.");
        var consistency=new JSONObject();
        for (int axis=0;axis<3;axis++) {
            double iou=iou(masks[axis],projected[axis]);
            if (withDepth && iou<1) throw new IllegalArgumentException("Depth conflicts with the registered silhouettes.");
            if (iou<.98) throw new IllegalArgumentException("The registered views disagree at the declared resolution.");
            consistency.put("xyz".substring(axis,axis+1),iou);
        }
        var mesh=surface(hull,low,high,step);
        String sourceSha=MapDefinition.sha(bytes);
        var scene=scene(title,mesh,sourceSha,n,low,high,step,withDepth);
        String policy=allAxes?POLICY_AXIS_DEPTH:multi?POLICY_MULTIDEPTH:withDepth?POLICY_DEPTH:POLICY;
        var evidence=new JSONObject().put("policy",policy).put("sourceSha256",sourceSha).put("pngSha256",hashes)
                .put("grid",n).put("cellM",step).put("voxels",solid).put("volumeM3",solid*step*step*step)
                .put("vertices",mesh.vertexCount()).put("triangles",mesh.indices().length/3)
                .put("inputProjectionIou",consistency).put("meshSha256",mesh.key())
                .put("scope","Three orthographic silhouettes at a declared scale; surface depth hidden in every silhouette is unresolved.")
                .put("physics","Separate enclosing box; visible cutouts belong to the visual mesh.");
        if (withDepth) evidence.put("depth",depth)
                .put("scope","Silhouettes plus registered first hits; carve only in front of the fitted grid face. Detail behind that face remains unresolved by the depth ray.");
        if (multi) evidence.put("scope","Registered silhouette and first-hit evidence. Each ray carves before its fitted face. Any detail beyond all observed faces remains unresolved.");
        return new Result(scene,evidence);
    }

    private static boolean[][] mask(byte[] bytes,int n) throws Exception {
        if (bytes.length<24 || bytes.length>240000 || java.nio.ByteBuffer.wrap(bytes).getLong()!=0x89504e470d0a1a0aL)
            throw new IllegalArgumentException("Each registered silhouette must be a PNG up to 240 KB.");
        BufferedImage image=WorldObservationStore.decode(bytes);
        int side=image.getWidth();
        if (!image.getColorModel().hasAlpha() || side!=image.getHeight() || side%n!=0 || side/n<4)
            throw new IllegalArgumentException("Use square transparent PNGs with at least four pixels per grid cell.");
        var cells=new boolean[n][n];
        int step=side/n,count=0;
        for (int row=0;row<n;row++) for (int col=0;col<n;col++) {
            boolean occupied=(image.getRGB(col*step+step/2,row*step+step/2)>>>24)>=128;
            if (occupied && (row==0 || col==0 || row==n-1 || col==n-1))
                throw new IllegalArgumentException("A silhouette touches the search boundary.");
            cells[row][col]=occupied;
            if (occupied) count++;
        }
        if (count<4) throw new IllegalArgumentException("The image has no usable foreground silhouette.");
        return cells;
    }

    private static double iou(boolean[][] a,boolean[][] b) {
        int common=0, union=0;
        for (int y=0;y<a.length;y++) for (int x=0;x<a.length;x++) {
            if (a[y][x] && b[y][x]) common++;
            if (a[y][x] || b[y][x]) union++;
        }
        return common/(double)union;
    }

    private static SpatialVisualMesh surface(boolean[][][] hull,int[] low,int[] high,double step) {
        var keys=new LinkedHashMap<Point,Integer>();
        List<Integer> indices=new ArrayList<>();
        int n=hull.length;
        for (int x=low[0];x<high[0];x++) for (int y=low[1];y<high[1];y++) for (int z=low[2];z<high[2];z++) {
            if (!hull[x][y][z]) continue;
            int[] cell={x,y,z};
            for (int axis=0;axis<3;axis++) for (int sign:new int[]{-1,1}) {
                int[] neighbor=cell.clone();
                neighbor[axis]+=sign;
                if (neighbor[axis]>=0 && neighbor[axis]<n && hull[neighbor[0]][neighbor[1]][neighbor[2]]) continue;
                face(keys,indices,cell,axis,sign);
                if (keys.size()>SpatialVisualMesh.MAX_VERTICES || indices.size()>SpatialVisualMesh.MAX_INDICES)
                    throw new IllegalArgumentException("The reconstructed surface exceeds the editable mesh budget. Use a smaller grid.");
            }
        }
        var vertices=new float[3*keys.size()];
        keys.forEach((point,index)->{
            vertices[index*3]=(float)((point.x-(low[0]+high[0])/2.0)*step);
            vertices[index*3+1]=(float)((point.y-(low[1]+high[1])/2.0)*step);
            vertices[index*3+2]=(float)((point.z-(low[2]+high[2])/2.0)*step);
        });
        return new SpatialVisualMesh(vertices,indices.stream().mapToInt(Integer::intValue).toArray());
    }

    private static void face(Map<Point,Integer> keys,List<Integer> indices,int[] cell,int axis,int sign) {
        int u=(axis+1)%3,v=(axis+2)%3;
        int[] base=cell.clone();
        if (sign>0) base[axis]++;
        int[] corners=new int[4];
        for (int i=0;i<4;i++) {
            int[] p=base.clone();
            if (i==1 || i==2) p[u]++;
            if (i>=2) p[v]++;
            corners[i]=keys.computeIfAbsent(new Point(p[0],p[1],p[2]),key->keys.size());
        }
        for (int i:sign>0?new int[]{0,1,2,0,2,3}:new int[]{0,2,1,0,3,2}) indices.add(corners[i]);
    }

    private static JSONObject scene(String title,SpatialVisualMesh mesh,String source,int n,int[] low,int[] high,double step,boolean withDepth) {
        double[] center=new double[3],size=new double[3];
        for (int axis=0;axis<3;axis++) {
            center[axis]=((low[axis]+high[axis])/2.0-n/2.0)*step;
            size[axis]=(high[axis]-low[axis])*step;
            if (size[axis]<.05) throw new IllegalArgumentException("The reconstructed object is smaller than the scene format allows.");
        }
        return new JSONObject().put("format","fraynix.spatial-scene").put("version",1).put("units","m")
                .put("frame","right-handed-y-up").put("title",title+" · reconstructed").put("gravityMps2",9.81)
                .put("scaleReference",new JSONObject().put("a",new JSONArray(new int[]{0,0,0}))
                        .put("b",new JSONArray(new int[]{1,0,0})).put("knownLengthM",1).put("toleranceM",0).put("basis","declared-test"))
                .put("source",new JSONObject().put("kind","unverified").put("physicalProperties","declared")
                        .put("note",(withDepth?"Orthographic silhouette and first-hit depth package ":"Orthographic visual hull from PNG set ")
                                +source+". Declared scale and enclosing physics proxy."))
                .put("visualMeshes",new JSONObject().put(mesh.key(),mesh.encoded()))
                .put("objects",new JSONArray()
                        .put(box("floor","Support deck",new double[]{0,(low[1]-n/2.0)*step-.10,0},
                                new double[]{n*step*1.4,.2,n*step*1.4},0))
                        .put(box("surface","Reconstructed surface",center,size,1).put("visualMesh",mesh.key())));
    }

    private static JSONObject box(String key,String label,double[] position,double[] size,int mass) {
        return new JSONObject().put("sourceId",key).put("label",label).put("shape","box").put("massKg",mass)
                .put("position",new JSONArray(position)).put("size",new JSONArray(size))
                .put("rotation",new JSONArray(new int[]{0,0,0,1})).put("restitution",0).put("friction",.5);
    }
}
