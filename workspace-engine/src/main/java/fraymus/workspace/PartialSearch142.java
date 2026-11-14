package fraymus.workspace;

import java.awt.image.BufferedImage;
import java.util.*;
import org.json.*;

/** Fixed visible regions of immutable, provenance-checked appearances. Never a learned identity. */
final class PartialSearch142 {
    private static final int[][] MASKS={{0,0,8,8},{0,0,6,8},{2,0,8,8},{0,0,8,6},{0,2,8,8}};
    private record Example(double[] pixels,String key){}
    private record Candidate(PixelTracker.Box box,double score,double error,int mask,Example example,double step){}
    record Result(PixelTracker.Box box,PixelTracker.Box support,double score,double rival,double error,int mask,String key,int evaluations,boolean unique){
        JSONObject json(){return new JSONObject().put("box",box.json()).put("visibleSupport",support.json()).put("wholeBoxInferred",mask!=0).put("visibleFraction",mask==0?1:.75).put("score",score).put("rivalScore",rival).put("meanColorError",error).put("mask",mask).put("sourceKey",key).put("evaluations",evaluations).put("unique",unique);}
    }
    private final List<Example> examples=new ArrayList<>();
    private final PixelTracker.Box seed;
    final String memoryHash;
    PartialSearch142(BufferedImage first,PixelTracker.Box seed,JSONObject memory,boolean imported){
        if(!seed.measurable())throw new IllegalArgumentException("Valid source seed required");this.seed=seed;memoryHash=WorldFrames.hash(memory);
        examples.add(new Example(PixelTracker.descriptor(PixelTracker.working(first),seed),"initial-seed"));
        var refs=memory.getJSONArray("examples");if(refs.isEmpty()||refs.length()>ReferenceMemory137.CAPACITY)throw new IllegalArgumentException("Bounded verified examples required");
        for(Object raw:refs){var e=(JSONObject)raw;var a=e.getJSONArray("descriptor");if(a.length()!=192||!WorldFrames.hash(new JSONObject().put("values",a)).equals(e.getString("descriptorSha256")))throw new IllegalArgumentException("Reference descriptor changed");
            var pixels=new double[192];for(int i=0;i<pixels.length;i++){pixels[i]=a.getDouble(i);if(!Double.isFinite(pixels[i])||pixels[i]<0||pixels[i]>1)throw new IllegalArgumentException("Invalid reference pixels");}
            if(imported)examples.add(new Example(pixels,e.getString("imageSha256")+":"+e.getInt("ordinal")));
        }
    }
    private static double[] sample(int[] image,PixelTracker.Box b){var d=new double[192];int k=0;for(int y=0;y<8;y++)for(int x=0;x<8;x++){int px=(int)((b.x()+(x+.5)*b.w()/8)*192),py=(int)((b.y()+(y+.5)*b.h()/8)*192),rgb=image[Math.max(0,Math.min(191,py))*192+Math.max(0,Math.min(191,px))];for(int c=0;c<3;c++)d[k++]=((rgb>>(16-c*8))&255)/255.;}return d;}
    // Grayscale texture rejects flat colored patches; independent RGB error guards lighting/color coincidence.
    private static double[] compare(double[] a,double[] b,int mask){int[] m=MASKS[mask];double sa=0,sb=0,aa=0,bb=0,ab=0,error=0;int n=0;
        for(int y=m[1];y<m[3];y++)for(int x=m[0];x<m[2];x++){int k=(y*8+x)*3;double va=.299*a[k]+.587*a[k+1]+.114*a[k+2],vb=.299*b[k]+.587*b[k+1]+.114*b[k+2];sa+=va;sb+=vb;aa+=va*va;bb+=vb*vb;ab+=va*vb;for(int c=0;c<3;c++)error+=Math.abs(a[k+c]-b[k+c])/3;n++;}
        double va=aa-sa*sa/n,vb=bb-sb*sb/n;return new double[]{va/n<.0005||vb/n<.0005?-1:Math.max(-1,Math.min(1,(ab-sa*sb/n)/Math.sqrt(va*vb))),error/n};
    }
    private Candidate best(int[] pixels,PixelTracker.Box box,double step){double[] d=sample(pixels,box);Candidate best=null;for(var e:examples)for(int m=0;m<MASKS.length;m++){var q=compare(e.pixels,d,m);double score=q[1]>.14?-1:q[0];if(best==null||score>best.score)best=new Candidate(box,score,q[1],m,e,step);}return best;}
    Result search(BufferedImage original){var small=PixelTracker.working(original);int[] pixels=small.getRGB(0,0,192,192,null,0,192);var all=new ArrayList<Candidate>();var peaks=new ArrayList<Candidate>();int count=0;
        for(double f:new double[]{.75,.9,1,1.25,1.5}){double w=seed.w()*f,h=seed.h()*f;if(w<.025||h<.025||w>1||h>1)continue;double step=Math.max(1./192,Math.min(.025,Math.min(w,h)/3));var scale=new ArrayList<Candidate>();int nx=(int)Math.ceil((1-w)/step),ny=(int)Math.ceil((1-h)/step);
            for(int y=0;y<=ny;y++)for(int x=0;x<=nx;x++){var b=new PixelTracker.Box(Math.min(1-w,x*step),Math.min(1-h,y*step),w,h);if(!b.measurable())continue;scale.add(best(pixels,b,step));count++;}
            all.addAll(scale);scale.sort(Comparator.comparingDouble(Candidate::score).reversed());var chosen=new ArrayList<Candidate>();for(var c:scale){boolean separate=true;for(var p:chosen)if(PixelTracker.iou(c.box,p.box)>=.5){separate=false;break;}if(separate)chosen.add(c);if(chosen.size()==3)break;}peaks.addAll(chosen);
        }
        var refined=new ArrayList<Candidate>();for(var p:peaks){Candidate winner=p;double step=1./768;
            for(double f:new double[]{.96,1,1.04}){double w=p.box.w()*f,h=p.box.h()*f;if(w>1||h>1)continue;for(double y=Math.max(0,p.box.y()-p.step);y<=Math.min(1-h,p.box.y()+p.step)+1e-10;y+=step)for(double x=Math.max(0,p.box.x()-p.step);x<=Math.min(1-w,p.box.x()+p.step)+1e-10;x+=step){var b=new PixelTracker.Box(x,y,w,h);if(!b.measurable())continue;var c=best(pixels,b,step);count++;if(c.score>winner.score)winner=c;}}
            refined.add(winner);
        }
        if(refined.isEmpty())return new Result(seed,seed,-1,-1,1,0,"initial-seed",count,false);
        refined.sort(Comparator.comparingDouble(Candidate::score).reversed());var top=refined.get(0);double rival=-1;for(var c:all)if(PixelTracker.iou(c.box,top.box)<.25)rival=Math.max(rival,c.score);for(var c:refined)if(PixelTracker.iou(c.box,top.box)<.25)rival=Math.max(rival,c.score);
        int[] m=MASKS[top.mask];var support=new PixelTracker.Box(top.box.x()+m[0]*top.box.w()/8,top.box.y()+m[1]*top.box.h()/8,(m[2]-m[0])*top.box.w()/8,(m[3]-m[1])*top.box.h()/8);
        return new Result(top.box,support,top.score,rival,top.error,top.mask,top.example.key,count,top.score>=.93&&rival<top.score-.08&&support.measurable());
    }
}
