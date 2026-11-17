package fraymus.workspace;

import java.awt.image.BufferedImage;
import java.util.*;
import org.json.*;

/** Complete global proposals plus freshly rescored pose history. No returned locations. */
final class RotationSearch155 {
    enum Arithmetic { SCALAR, PRECOMPUTED, GRID }
    private record Reference(String key,double[] fine,double[] coarse,double w,double h) {}
    private record Peak(RotationSearch152.Pose pose,double score) {}
    private record Recent(RotationSearch152.Pose pose,int frame) {}
    private final List<Reference> references=new ArrayList<>();
    private final Map<String,Recent> recent=new HashMap<>();
    private final int width,height;
    private final Arithmetic arithmetic;
    private long evaluations;
    private int frame;
    RotationSearch155(BufferedImage first,PixelTracker.Box seed,JSONObject memory,Arithmetic arithmetic){
        width=first.getWidth();height=first.getHeight();this.arithmetic=Objects.requireNonNull(arithmetic);
        if(width>1920||height>1080||!seed.measurable())throw new IllegalArgumentException("Bounded image and full original reference required");
        add("initial-seed",StructureSearch149.descriptor(first,seed));
        var examples=memory.getJSONArray("examples");if(examples.length()>6)throw new IllegalArgumentException("At most six verified references");
        for(Object raw:examples){var e=(JSONObject)raw;add(e.getString("imageSha256")+":"+e.getInt("ordinal"),e.getJSONObject("structure149"));}
    }
    private void add(String key,JSONObject descriptor){
        var d=new JSONObject(descriptor.toString());String hash=d.getString("sha256");d.remove("sha256");
        if(!WorldFrames.hash(d).equals(hash)||!d.getString("schema").equals("full-structure-149-v1"))throw new IllegalArgumentException("Original reference structure changed");
        if(references.stream().anyMatch(r->r.key.equals(key)))throw new IllegalArgumentException("Duplicate reference identity");
        var b=PixelTracker.Box.from(d.getJSONArray("box"));references.add(new Reference(key,values(d.getJSONArray("pixels"),768),values(d.getJSONArray("coarse"),192),b.w()*width,b.h()*height));
    }
    private static double[] values(JSONArray a,int n){if(a.length()!=n)throw new IllegalArgumentException("Invalid structure dimensions");double[] v=new double[n];for(int i=0;i<n;i++){v[i]=a.getDouble(i);if(!Double.isFinite(v[i])||v[i]<0||v[i]>1)throw new IllegalArgumentException("Invalid reference pixels");}return v;}
    List<RotationSearch152.Match> search(BufferedImage image){
        if(image.getWidth()!=width||image.getHeight()!=height)throw new IllegalArgumentException("Frame dimensions changed");
        frame++;int[] pixels=image.getRGB(0,0,width,height,null,0,width);evaluations=0;var all=new ArrayList<RotationSearch152.Match>();
        double[] gray=arithmetic!=Arithmetic.SCALAR?grayscale(pixels):null;
        for(var ref:references){
            var coarse=new ArrayList<Peak>();double step=Math.max(1,Math.min(4,Math.min(ref.w,ref.h)/16));double[] sample=new double[192];
            for(double scale:new double[]{.8,1,1.2})for(int angle=-180;angle<180;angle+=15){
                interrupted();var peaks=new ArrayList<Peak>();double w=ref.w*scale,h=ref.h*scale;
                var kernel=arithmetic!=Arithmetic.SCALAR?new Kernel(ref.coarse,w,h,angle):null;
                if(arithmetic==Arithmetic.GRID){
                    var grid=new Grid(kernel,width,height,step);
                    for(int yi=0;yi<grid.ys.length;yi++)for(int xi=0;xi<grid.xs.length;xi++){
                        double score=grid.at(gray,xi,yi);if(Double.isNaN(score))continue;
                        evaluations++;if(score<.25)continue;
                        insert(peaks,new Peak(new RotationSearch152.Pose(grid.xs[xi],grid.ys[yi],w,h,angle),score));
                    }
                }else{
                    for(double y=0;y<=height;y+=step)for(double x=0;x<=width;x+=step){
                        double score;
                        if(kernel!=null){score=kernel.at(gray,x,y,width,height);if(Double.isNaN(score))continue;}
                        else {var pose=new RotationSearch152.Pose(x,y,w,h,angle);if(!RotationSearch152.sample(pixels,width,height,pose,8,sample))continue;score=correlation(ref.coarse,sample);}
                        evaluations++;if(score<.25)continue;insert(peaks,new Peak(new RotationSearch152.Pose(x,y,w,h,angle),score));
                    }
                }coarse.addAll(peaks);
            }
            var refined=new ArrayList<RotationSearch152.Match>();
            for(var peak:coarse)refined.add(refine(pixels,ref,peak.pose,step,false));
            // The four best full-image proposals get a second, bounded refinement
            // targeting the weakest required criterion. All thresholds stay intact.
            var selected=refined.stream().sorted(Comparator.comparingDouble((RotationSearch152.Match m)->m.score().getDouble("score")).reversed()).limit(4).toList();
            for(var p:selected)refined.add(refine(pixels,ref,p.pose(),step,true));
            var prior=recent.get(ref.key);
            if(prior!=null&&frame-prior.frame<=3)refined.add(refine(pixels,ref,prior.pose,step,true));
            all.addAll(refined);
        }
        var result=new ArrayList<RotationSearch152.Match>();
        for(var ref:references){
            var top=all.stream().filter(m->m.key().equals(ref.key)).max(Comparator.comparing((RotationSearch152.Match m)->m.score().getBoolean("strong")).thenComparingDouble(m->m.score().getDouble("score"))).orElse(null);if(top==null)continue;
            double rival=all.stream().filter(m->PixelTracker.iou(m.box(),top.box())<.25&&m.score().getDouble("colorError")<=.14).mapToDouble(m->m.score().getDouble("score")).max().orElse(-1);
            boolean unique=top.score().getBoolean("strong")&&rival<top.score().getDouble("score")-.08;
            var match=new RotationSearch152.Match(top.key(),top.pose(),top.box(),top.score(),rival,unique);result.add(match);
            if(unique)recent.put(ref.key,new Recent(top.pose(),frame));else if(recent.containsKey(ref.key)&&frame-recent.get(ref.key).frame>3)recent.remove(ref.key);
        }return List.copyOf(result);
    }
    private RotationSearch152.Match refine(int[] pixels,Reference ref,RotationSearch152.Pose pose,double step,boolean balanced){
        JSONObject best=full(pixels,ref,pose);double value=objective(best,balanced);
        for(int level=0;level<5;level++){
            interrupted();double shift=step/Math.pow(2,level+1),angle=7.5/Math.pow(2,level),scale=.06/Math.pow(2,level);
            for(int pass=0;pass<2;pass++){
                var origin=pose;var candidates=new ArrayList<RotationSearch152.Pose>();
                for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)if(dx!=0||dy!=0)candidates.add(new RotationSearch152.Pose(origin.x()+dx*shift,origin.y()+dy*shift,origin.width(),origin.height(),origin.degrees()));
                for(int axis=0;axis<4;axis++)for(int sign:new int[]{-1,1})candidates.add(switch(axis){case 0->new RotationSearch152.Pose(origin.x()+sign*shift,origin.y(),origin.width(),origin.height(),origin.degrees());case 1->new RotationSearch152.Pose(origin.x(),origin.y()+sign*shift,origin.width(),origin.height(),origin.degrees());case 2->new RotationSearch152.Pose(origin.x(),origin.y(),origin.width(),origin.height(),origin.degrees()+sign*angle);default->new RotationSearch152.Pose(origin.x(),origin.y(),origin.width()*(1+sign*scale),origin.height()*(1+sign*scale),origin.degrees());});
                for(var next:candidates){var score=full(pixels,ref,next);double nextValue=objective(score,balanced);if(nextValue>value){pose=next;best=score;value=nextValue;}}
            }
        }
        return new RotationSearch152.Match(ref.key,pose,pose.bounds(width,height),best,-1,false);
    }
    private static double objective(JSONObject score,boolean balanced){
        if(!balanced)return score.getDouble("score");
        if(!score.getBoolean("inFrame"))return -100;
        return Math.min(Math.min(score.getDouble("score")/.94,score.getDouble("gradient")/.85),Math.min(.10/Math.max(1e-12,score.getDouble("colorError")),.14/Math.max(1e-12,score.getDouble("quadrantError"))));
    }
    private JSONObject full(int[] pixels,Reference ref,RotationSearch152.Pose pose){evaluations++;double[] v=new double[768];return ReturnDiagnosis151.score(ref.fine,RotationSearch152.sample(pixels,width,height,pose,16,v)?v:null);}
    private void insert(List<Peak> peaks,Peak p){
        var b=p.pose.bounds(width,height);int overlap=-1;for(int i=0;i<peaks.size();i++)if(PixelTracker.iou(b,peaks.get(i).pose.bounds(width,height))>=.25){overlap=i;break;}
        if(overlap>=0){if(peaks.get(overlap).score>=p.score)return;peaks.remove(overlap);}peaks.add(p);peaks.sort(Comparator.comparingDouble(Peak::score).reversed());if(peaks.size()>2)peaks.removeLast();
    }
    private static void interrupted(){if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException("Rotation search interrupted");}
    private static double gray(double[] v,int i){return .299*v[i*3]+.587*v[i*3+1]+.114*v[i*3+2];}
    private static double[] grayscale(int[] pixels){double[] result=new double[pixels.length];for(int i=0;i<pixels.length;i++){int p=pixels[i];result[i]=.299*(((p>>16)&255)/255.)+.587*(((p>>8)&255)/255.)+.114*((p&255)/255.);}return result;}
    private static double correlation(double[] a,double[] b){double sa=0,sb=0,aa=0,bb=0,ab=0;int n=a.length/3;for(int i=0;i<n;i++){double x=gray(a,i),y=gray(b,i);sa+=x;sb+=y;aa+=x*x;bb+=y*y;ab+=x*y;}return normalized(sa,sb,aa,bb,ab,n);}
    private static double normalized(double sa,double sb,double aa,double bb,double ab,int n){double va=aa-sa*sa/n,vb=bb-sb*sb/n;return va/n<.0005||vb/n<.0005?-1:Math.max(-1,Math.min(1,(ab-sa*sb/n)/Math.sqrt(va*vb)));}
    /** Same operations/order as scalar sampling. Do not reassociate x+cu-sv. */
    static final class Kernel {
        final double[] cu=new double[64],sv=new double[64],su=new double[64],cv=new double[64],reference=new double[64];
        final double bw,bh,sa,aa;
        Kernel(double[] original,double w,double h,double degrees){
            double a=Math.toRadians(degrees),c=Math.cos(a),s=Math.sin(a);bw=Math.abs(c)*w+Math.abs(s)*h;bh=Math.abs(s)*w+Math.abs(c)*h;double sum=0,square=0;
            for(int y=0;y<8;y++)for(int x=0;x<8;x++){int i=y*8+x;double u=((x+.5)/8-.5)*w,v=((y+.5)/8-.5)*h;cu[i]=c*u;sv[i]=s*v;su[i]=s*u;cv[i]=c*v;reference[i]=gray(original,i);sum+=reference[i];square+=reference[i]*reference[i];}sa=sum;aa=square;
        }
        double at(double[] pixels,double x,double y,int width,int height){
            if(!new PixelTracker.Box((x-bw/2)/width,(y-bh/2)/height,bw/width,bh/height).measurable())return Double.NaN;
            double sb=0,bb=0,ab=0;
            for(int i=0;i<64;i++){int px=(int)(x+cu[i]-sv[i]),py=(int)(y+su[i]+cv[i]);if(px<0||py<0||px>=width||py>=height)return Double.NaN;double b=pixels[py*width+px];sb+=b;bb+=b*b;ab+=reference[i]*b;}
            return normalized(sa,sb,aa,bb,ab,64);
        }
    }
    /** One bounded angle at a time; no frame pixels or proposals are cached.
     * Axis values use repeated addition, and casts retain the exact old operation order. */
    static final class Grid {
        final Kernel kernel;
        final double[] xs,ys;
        final int[][] columns,rows;
        Grid(Kernel kernel,int width,int height,double step){
            if(width<1||width>1920||height<1||height>1080||!Double.isFinite(step)||step<1||step>4)
                throw new IllegalArgumentException("Bounded grid required");
            this.kernel=Objects.requireNonNull(kernel);xs=axis(width,step);ys=axis(height,step);
            columns=new int[xs.length][];rows=new int[ys.length][];
            for(int x=0;x<xs.length;x++){
                if(!measurableAxis(xs[x],kernel.bw,width))continue;
                int[] values=new int[64];boolean valid=true;
                for(int i=0;i<64;i++){int px=(int)(xs[x]+kernel.cu[i]-kernel.sv[i]);if(px<0||px>=width){valid=false;break;}values[i]=px;}
                if(valid)columns[x]=values;
            }
            for(int y=0;y<ys.length;y++){
                if(!measurableAxis(ys[y],kernel.bh,height))continue;
                int[] values=new int[64];boolean valid=true;
                for(int i=0;i<64;i++){int py=(int)(ys[y]+kernel.su[i]+kernel.cv[i]);if(py<0||py>=height){valid=false;break;}values[i]=py*width;}
                if(valid)rows[y]=values;
            }
        }
        private static double[] axis(int end,double step){
            var values=new double[(int)Math.floor(end/step)+2];int count=0;
            for(double v=0;v<=end;v+=step)values[count++]=v;
            return Arrays.copyOf(values,count);
        }
        private static boolean measurableAxis(double center,double span,int dimension){
            double start=(center-span/2)/dimension,size=span/dimension,end=start+size;
            return Double.isFinite(start)&&Double.isFinite(end)&&start>=0&&end<=1&&end-start>=.025;
        }
        double at(double[] pixels,int x,int y){
            var col=columns[x];var row=rows[y];if(col==null||row==null)return Double.NaN;
            double sb=0,bb=0,ab=0;
            for(int i=0;i<64;i++){double b=pixels[row[i]+col[i]];sb+=b;bb+=b*b;ab+=kernel.reference[i]*b;}
            return normalized(kernel.sa,sb,kernel.aa,bb,ab,64);
        }
    }
    long evaluations(){return evaluations;}
}
