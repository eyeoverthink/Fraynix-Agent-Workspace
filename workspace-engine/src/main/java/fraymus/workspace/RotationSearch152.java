package fraymus.workspace;

import java.awt.image.BufferedImage;
import java.util.*;
import org.json.*;

/** Label-free, bounded pose proposals. Only original reference pixels enter this search. */
final class RotationSearch152 {
    record Pose(double x,double y,double width,double height,double degrees) {
        PixelTracker.Box bounds(int width,int height){double a=Math.toRadians(degrees),c=Math.abs(Math.cos(a)),s=Math.abs(Math.sin(a));double w=c*this.width+s*this.height,h=s*this.width+c*this.height;return new PixelTracker.Box((x-w/2)/width,(y-h/2)/height,w/width,h/height);}
        JSONObject json(){return new JSONObject().put("centerX",x).put("centerY",y).put("width",width).put("height",height).put("degrees",degrees);}
    }
    private record Reference(String key,double[] fine,double[] coarse,double w,double h) {}
    private record Peak(Pose pose,double score) {}
    record Match(String key,Pose pose,PixelTracker.Box box,JSONObject score,double rival,boolean unique) {
        JSONObject json(){return new JSONObject(score.toString()).put("sourceKey",key).put("pose",pose.json()).put("box",box.json()).put("rivalScore",rival).put("unique",unique);}
    }
    private final List<Reference> references=new ArrayList<>();
    private final int width,height;
    private long evaluations;
    RotationSearch152(BufferedImage first,PixelTracker.Box seed,JSONObject memory){
        width=first.getWidth();height=first.getHeight();
        if(width>1920||height>1080||!seed.measurable())throw new IllegalArgumentException("Bounded image and full original reference required");
        add("initial-seed",StructureSearch149.descriptor(first,seed));
        JSONArray examples=memory.getJSONArray("examples");if(examples.length()>6)throw new IllegalArgumentException("At most six verified references");
        for(Object raw:examples){var e=(JSONObject)raw;add(e.getString("imageSha256")+":"+e.getInt("ordinal"),e.getJSONObject("structure149"));}
    }
    private void add(String key,JSONObject descriptor){
        var d=new JSONObject(descriptor.toString());String hash=d.getString("sha256");d.remove("sha256");
        if(!WorldFrames.hash(d).equals(hash)||!d.getString("schema").equals("full-structure-149-v1"))throw new IllegalArgumentException("Original reference structure changed");
        if(references.stream().anyMatch(r->r.key.equals(key)))throw new IllegalArgumentException("Duplicate reference identity");
        var b=PixelTracker.Box.from(d.getJSONArray("box"));double[] fine=values(d.getJSONArray("pixels"),768),coarse=values(d.getJSONArray("coarse"),192);
        references.add(new Reference(key,fine,coarse,b.w()*width,b.h()*height));
    }
    private static double[] values(JSONArray a,int n){if(a.length()!=n)throw new IllegalArgumentException("Invalid structure dimensions");double[] v=new double[n];for(int i=0;i<n;i++){v[i]=a.getDouble(i);if(!Double.isFinite(v[i])||v[i]<0||v[i]>1)throw new IllegalArgumentException("Invalid reference pixels");}return v;}
    List<Match> search(BufferedImage image){
        if(image.getWidth()!=width||image.getHeight()!=height)throw new IllegalArgumentException("Frame dimensions changed");
        int[] pixels=image.getRGB(0,0,width,height,null,0,width);evaluations=0;var all=new ArrayList<Match>();
        for(var ref:references){
            var coarse=new ArrayList<Peak>();double step=Math.max(1,Math.min(4,Math.min(ref.w,ref.h)/16));double[] sample=new double[192];
            for(double scale:new double[]{.8,1,1.2})for(int angle=-180;angle<180;angle+=15){
                if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException("Rotation search interrupted");
                var peaks=new ArrayList<Peak>();
                for(double y=0;y<=height;y+=step)for(double x=0;x<=width;x+=step){
                    var pose=new Pose(x,y,ref.w*scale,ref.h*scale,angle);
                    if(!sample(pixels,width,height,pose,8,sample))continue;
                    double score=correlation(ref.coarse,sample);evaluations++;
                    if(score<.25)continue;
                    insert(peaks,new Peak(pose,score),2);
                }coarse.addAll(peaks);
            }
            // Keep every angle/scale's separated hypotheses through refinement. A copy
            // must compete even when another orientation has a slightly higher score.
            for(var peak:coarse){var pose=peak.pose;JSONObject best=full(pixels,ref,pose);double value=best.getDouble("score");
                for(int level=0;level<5;level++){
                    double shift=step/Math.pow(2,level+1),angle=7.5/Math.pow(2,level),scale=.06/Math.pow(2,level);
                    for(int pass=0;pass<2;pass++){
                        Pose origin=pose;
                        // Joint x/y moves cross pixel-sampling plateaus which a single-axis
                        // hill climb can miss, including a genuine competing copy.
                        for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)if(dx!=0||dy!=0){
                            var next=new Pose(origin.x+dx*shift,origin.y+dy*shift,origin.width,origin.height,origin.degrees);
                            var candidate=full(pixels,ref,next);if(candidate.getDouble("score")>value){pose=next;best=candidate;value=best.getDouble("score");}
                        }
                        for(int axis=0;axis<4;axis++)for(int sign:new int[]{-1,1}){
                            Pose next=switch(axis){case 0->new Pose(origin.x+sign*shift,origin.y,origin.width,origin.height,origin.degrees);case 1->new Pose(origin.x,origin.y+sign*shift,origin.width,origin.height,origin.degrees);case 2->new Pose(origin.x,origin.y,origin.width,origin.height,origin.degrees+sign*angle);default->new Pose(origin.x,origin.y,origin.width*(1+sign*scale),origin.height*(1+sign*scale),origin.degrees);};
                            var candidate=full(pixels,ref,next);if(candidate.getDouble("score")>value){pose=next;best=candidate;value=best.getDouble("score");}
                        }
                    }
                }
                var box=pose.bounds(width,height);if(box.measurable())all.add(new Match(ref.key,pose,box,best,-1,false));
            }
        }
        var result=new ArrayList<Match>();
        for(var ref:references){var top=all.stream().filter(m->m.key.equals(ref.key)).max(Comparator.comparingDouble(m->m.score.getDouble("score"))).orElse(null);if(top==null)continue;
            double rival=all.stream().filter(m->PixelTracker.iou(m.box,top.box)<.25&&m.score.getDouble("colorError")<=.14).mapToDouble(m->m.score.getDouble("score")).max().orElse(-1);
            boolean unique=top.score.getBoolean("strong")&&rival<top.score.getDouble("score")-.08;
            result.add(new Match(top.key,top.pose,top.box,top.score,rival,unique));
        }return List.copyOf(result);
    }
    private void insert(List<Peak> peaks,Peak p,int capacity){
        var b=p.pose.bounds(width,height);int overlap=-1;for(int i=0;i<peaks.size();i++)if(PixelTracker.iou(b,peaks.get(i).pose.bounds(width,height))>=.25){overlap=i;break;}
        if(overlap>=0){if(peaks.get(overlap).score>=p.score)return;peaks.remove(overlap);}
        peaks.add(p);peaks.sort(Comparator.comparingDouble(Peak::score).reversed());if(peaks.size()>capacity)peaks.removeLast();
    }
    private JSONObject full(int[] pixels,Reference ref,Pose pose){evaluations++;double[] v=new double[768];return ReturnDiagnosis151.score(ref.fine,sample(pixels,width,height,pose,16,v)?v:null);}
    static boolean sample(int[] pixels,int width,int height,Pose p,int size,double[] out){
        double a=Math.toRadians(p.degrees),c=Math.cos(a),s=Math.sin(a);int k=0;
        // Require the whole oriented crop, not just its interior sample centres.
        var bounds=p.bounds(width,height);if(!bounds.measurable())return false;
        for(int y=0;y<size;y++)for(int x=0;x<size;x++){
            double u=((x+.5)/size-.5)*p.width,v=((y+.5)/size-.5)*p.height;
            int px=(int)(p.x+c*u-s*v),py=(int)(p.y+s*u+c*v);
            if(px<0||py<0||px>=width||py>=height)return false;
            int rgb=pixels[py*width+px];out[k++]=((rgb>>16)&255)/255.;out[k++]=((rgb>>8)&255)/255.;out[k++]=(rgb&255)/255.;
        }return true;
    }
    private static double gray(double[] v,int i){return .299*v[i*3]+.587*v[i*3+1]+.114*v[i*3+2];}
    private static double correlation(double[] a,double[] b){double sa=0,sb=0,aa=0,bb=0,ab=0;int n=a.length/3;for(int i=0;i<n;i++){double x=gray(a,i),y=gray(b,i);sa+=x;sb+=y;aa+=x*x;bb+=y*y;ab+=x*y;}double va=aa-sa*sa/n,vb=bb-sb*sb/n;return va/n<.0005||vb/n<.0005?-1:Math.max(-1,Math.min(1,(ab-sa*sb/n)/Math.sqrt(va*vb)));}
    long evaluations(){return evaluations;}
}
