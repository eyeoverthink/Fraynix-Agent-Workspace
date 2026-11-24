
package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Base64;

/** Turn a captured, contrasting graphic into a simple dimensional hypothesis. */
final class BrowserShapeTrace {
    static final String FORMAT = "fraynix.browser-graphic-v1";
    private record Candidate(SpatialVisualMesh mesh, int grid, double iou, double heightM) {}

    private BrowserShapeTrace() {}

    static SpatialReconstruction.Result build(byte[] bytes) throws Exception {
        var input = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
        SpatialScene.keys(input,"format","url","selector","widthM","thicknessM","png");
        if (!FORMAT.equals(input.getString("format"))) throw new IllegalArgumentException("Unsupported browser crop format.");
        String url = SpatialScene.text(input,"url",800);
        var uri = java.net.URI.create(url);
        if (!java.util.Set.of("http","https").contains(uri.getScheme()) || uri.getHost()==null
                || uri.getUserInfo()!=null || uri.getQuery()!=null || uri.getFragment()!=null)
            throw new IllegalArgumentException("Use a browser page URL without credentials, query, or fragment.");
        String selector = SpatialScene.text(input,"selector",180);
        double width = SpatialScene.number(input,"widthM",.10,5), depth = SpatialScene.number(input,"thicknessM",.05,2);
        byte[] png = Base64.getDecoder().decode(input.getString("png"));
        if (png.length<24 || java.nio.ByteBuffer.wrap(png).getLong()!=0x89504e470d0a1a0aL)
            throw new IllegalArgumentException("Choose a captured PNG graphic.");
        BufferedImage image = WorldObservationStore.decode(png);
        int w = image.getWidth(), h = image.getHeight();
        if (w<16 || h<16 || w>1024 || h>1024) throw new IllegalArgumentException("Use a visible graphic between 16 and 1024 pixels per side.");
        var foreground = isolate(image);
        var attempts = new JSONArray();
        Candidate best = null;
        for (int grid : new int[]{16,20,22}) {
            try {
                var candidate = fit(foreground,w,h,grid,width,depth);
                attempts.put(new JSONObject().put("grid",grid).put("foregroundIou",candidate.iou));
                if (best==null || candidate.iou>best.iou) best=candidate;
            } catch (IllegalArgumentException refused) {
                attempts.put(new JSONObject().put("grid",grid).put("refused",refused.getMessage()));
            }
        }
        if (best==null || best.iou<.78) throw new IllegalArgumentException("The visible silhouette could not be reproduced at the supported resolutions.");
        var mesh=best.mesh;
        String hash=MapDefinition.sha(bytes);
        var proof=new JSONObject().put("policy","browser-silhouette-slab").put("sourceSha256",hash)
                .put("pngSha256",MapDefinition.sha(png)).put("sourceUrl",url).put("selector",selector)
                .put("grid",best.grid).put("candidateFits",attempts).put("foregroundIou",best.iou)
                .put("declaredWidthM",width).put("declaredThicknessM",depth)
                .put("triangles",mesh.indices().length/3).put("meshSha256",mesh.key())
                .put("scope","One visible silhouette. Thickness and scale are declared.");
        var definition = new JSONObject().put("format","fraynix.spatial-scene").put("version",1).put("units","m")
                .put("frame","right-handed-y-up").put("title","Browser silhouette").put("gravityMps2",9.81)
                .put("scaleReference",new JSONObject().put("a",new JSONArray(new double[]{0,0,0}))
                        .put("b",new JSONArray(new double[]{width,0,0})).put("knownLengthM",width)
                        .put("toleranceM",0).put("basis","declared-test"))
                .put("source",new JSONObject().put("kind","unverified").put("physicalProperties","declared")
                        .put("note","Browser crop SHA-256 "+hash+". Single silhouette with declared width and thickness."))
                .put("visualMeshes",new JSONObject().put(mesh.key(),mesh.encoded()))
                .put("objects",new JSONArray()
                        .put(SpatialReconstruction.box("floor","Support deck",new double[]{0,-best.heightM/2-.1,0},
                                new double[]{width*1.5,.2,Math.max(depth*1.5,width)},0))
                        .put(SpatialReconstruction.box("surface","Traced graphic",new double[]{0,0,0},
                                new double[]{width,best.heightM,depth},1).put("visualMesh",mesh.key())));
        return new SpatialReconstruction.Result(definition,proof);
    }

    private static boolean[] isolate(BufferedImage img) {
        int w=img.getWidth(),h=img.getHeight(),bg=img.getRGB(0,0);
        for(int corner:new int[]{img.getRGB(w-1,0),img.getRGB(0,h-1),img.getRGB(w-1,h-1)})
            if (difference(bg,corner)>24) throw new IllegalArgumentException("Choose a graphic with a uniform contrasting background.");
        var ink=new boolean[w*h];
        for(int y=0;y<h;y++) for(int x=0;x<w;x++) ink[y*w+x]=difference(bg,img.getRGB(x,y))>=40;
        var best=new java.util.ArrayList<Integer>();
        for(int i=0;i<ink.length;i++) {
            if(!ink[i]) continue;
            var component=new java.util.ArrayList<Integer>();
            var queue=new ArrayDeque<Integer>();
            add(ink,queue,i);
            while(!queue.isEmpty()) {
                int p=queue.removeFirst(), x=p%w, y=p/w;
                component.add(p);
                if(x>0)add(ink,queue,p-1);
                if(x+1<w)add(ink,queue,p+1);
                if(y>0)add(ink,queue,p-w);
                if(y+1<h)add(ink,queue,p+w);
            }
            if(component.size()>best.size())best=component;
        }
        if(best.size()<64 || best.size()>.85*w*h) throw new IllegalArgumentException("Choose one clearly isolated foreground item.");
        for(int p:best) ink[p]=true;
        return ink;
    }

    private static void add(boolean[] ink,ArrayDeque<Integer> queue,int i) {
        if(ink[i]) {ink[i]=false;queue.addLast(i);}
    }

    private static int difference(int a,int b) {
        return Math.max(Math.abs((a&255)-(b&255)),Math.max(Math.abs(((a>>8)&255)-((b>>8)&255)),
                Math.abs(((a>>16)&255)-((b>>16)&255))));
    }

    private static Candidate fit(boolean[] reference,int w,int h,int grid,double width,double depth) {
        int side=Math.max(w,h), padX=(side-w)/2,padY=(side-h)/2;
        int n=grid+2;
        var hits=new int[grid][grid];
        var totals=new int[grid][grid];
        for(int y=0;y<h;y++) for(int x=0;x<w;x++) {
            int c=(x+padX)*grid/side,r=(y+padY)*grid/side;
            totals[r][c]++;
            if(reference[y*w+x])hits[r][c]++;
        }
        var hull=new boolean[n][n][n];
        int[] low={n,n,1}, high={0,0,2};
        for(int r=0;r<grid;r++) for(int c=0;c<grid;c++) {
            if(hits[r][c]==0 || hits[r][c]*2<totals[r][c])continue;
            int x=c+1,y=grid-r;
            hull[x][y][1]=true;
            low[0]=Math.min(low[0],x);low[1]=Math.min(low[1],y);
            high[0]=Math.max(high[0],x+1);high[1]=Math.max(high[1],y+1);
        }
        if(high[0]-low[0]<2 || high[1]-low[1]<2) throw new IllegalArgumentException("The traced region is too small.");
        double step=width/(high[0]-low[0]),height=step*(high[1]-low[1]);
        RampDefinition.range(height,.05,20,"traced height metres");
        var scaled=SpatialReconstruction.surface(hull,low,high,step);
        var points=scaled.positions();
        for(int i=2;i<points.length;i+=3)points[i]=(float)(points[i]*depth/step);
        SpatialFaceExtrusion.requireClosed(scaled);
        int overlap=0, union=0;
        for(int y=0;y<h;y++) for(int x=0;x<w;x++) {
            boolean model=hull[(x+padX)*grid/side+1][grid-(y+padY)*grid/side][1];
            boolean seen=reference[y*w+x];
            if(model&&seen)overlap++;
            if(model||seen)union++;
        }
        return new Candidate(scaled,grid,overlap/(double)union,height);
    }
}
