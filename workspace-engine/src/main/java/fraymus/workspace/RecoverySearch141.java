package fraymus.workspace;

import java.awt.image.BufferedImage;
import java.util.*;
import org.json.*;

/** Bounded multi-location source search. A high correlation is not object identity. */
final class RecoverySearch141 {
    private record Candidate(PixelTracker.Box box,double score,double stepX,double stepY){}
    record Result(PixelTracker.Box box,double score,double rival,int evaluations,boolean unique){
        JSONObject json(){return new JSONObject().put("box",box.json()).put("score",score).put("rivalScore",rival).put("evaluations",evaluations).put("unique",unique);}
    }
    private final PixelTracker.Box seed;
    private final double[] normalized;
    private final boolean textured;
    final String hash;
    RecoverySearch141(BufferedImage first,PixelTracker.Box seed){
        this.seed=seed;double[] d=PixelTracker.descriptor(PixelTracker.working(first),seed);double mean=Arrays.stream(d).average().orElse(0),norm=0;
        normalized=d.clone();for(int i=0;i<d.length;i++){normalized[i]-=mean;norm+=normalized[i]*normalized[i];}
        textured=norm>=.003*d.length;double divisor=Math.sqrt(Math.max(norm,1e-20));for(int i=0;i<d.length;i++)normalized[i]/=divisor;
        hash=WorldFrames.hash(new JSONObject().put("version","multipeak-original-search-141-v1").put("seed",seed.json()).put("descriptor",new JSONArray(d)));
    }
    private double score(int[] pixels,PixelTracker.Box b){
        double sum=0,squares=0,dot=0;int k=0,n=PixelTracker.WORK;
        for(int y=0;y<8;y++)for(int x=0;x<8;x++){
            int px=Math.max(0,Math.min(n-1,(int)((b.x()+(x+.5)*b.w()/8)*n))),py=Math.max(0,Math.min(n-1,(int)((b.y()+(y+.5)*b.h()/8)*n))),rgb=pixels[py*n+px];
            for(int c=0;c<3;c++){double v=((rgb>>(16-8*c))&255)/255.;sum+=v;squares+=v*v;dot+=normalized[k++]*v;}
        }
        double norm=squares-sum*sum/normalized.length;return norm<.001?0:Math.max(-1,Math.min(1,dot/Math.sqrt(norm)));
    }
    Result search(BufferedImage small){
        if(small.getWidth()!=PixelTracker.WORK||small.getHeight()!=PixelTracker.WORK)throw new IllegalArgumentException("Working image required");
        if(!textured)return new Result(seed,0,0,0,false);
        int[] pixels=small.getRGB(0,0,PixelTracker.WORK,PixelTracker.WORK,null,0,PixelTracker.WORK);var all=new ArrayList<Candidate>();var peaks=new ArrayList<Candidate>();int count=0;
        for(double factor:new double[]{.6,.75,.9,1,1.1,1.25,1.5,1.7}){
            double w=seed.w()*factor,h=seed.h()*factor;if(w<.025||h<.025||w>1||h>1)continue;
            double sx=Math.max(1./192,Math.min(.025,w/3)),sy=Math.max(1./192,Math.min(.025,h/3));int nx=(int)Math.ceil((1-w)/sx),ny=(int)Math.ceil((1-h)/sy);
            var scaleCandidates=new ArrayList<Candidate>();
            for(int iy=0;iy<=ny;iy++)for(int ix=0;ix<=nx;ix++){
                var b=new PixelTracker.Box(Math.min(1-w,ix*sx),Math.min(1-h,iy*sy),w,h);if(!b.measurable())continue;
                scaleCandidates.add(new Candidate(b,score(pixels,b),sx,sy));count++;
            }
            all.addAll(scaleCandidates);scaleCandidates.sort(Comparator.comparingDouble(Candidate::score).reversed());var scalePeaks=new ArrayList<Candidate>();
            // Keep alternatives at every scale; one large match cannot consume the entire search budget.
            for(var c:scaleCandidates){boolean separate=true;for(var p:scalePeaks)if(PixelTracker.iou(c.box,p.box)>=.5){separate=false;break;}if(separate)scalePeaks.add(c);if(scalePeaks.size()==3)break;}
            peaks.addAll(scalePeaks);
        }
        if(peaks.isEmpty())return new Result(seed,0,0,count,false);
        var refined=new ArrayList<Candidate>();double step=1./768;
        for(var p:peaks){Candidate best=p;
            for(double factor:new double[]{.96,1,1.04}){
                double w=p.box.w()*factor,h=p.box.h()*factor,area=w*h/(seed.w()*seed.h());if(w>1||h>1||area<.35||area>3)continue;
                double cx=p.box.x()+(p.box.w()-w)/2,cy=p.box.y()+(p.box.h()-h)/2;
                for(double y=Math.max(0,cy-p.stepY);y<=Math.min(1-h,cy+p.stepY)+1e-10;y+=step)
                    for(double x=Math.max(0,cx-p.stepX);x<=Math.min(1-w,cx+p.stepX)+1e-10;x+=step){
                        var b=new PixelTracker.Box(x,y,w,h);if(!b.measurable())continue;double q=score(pixels,b);count++;if(q>best.score)best=new Candidate(b,q,step,step);
                    }
            }
            // Refine size as well as location: a differently scaled copy must remain a rival.
            for(int iteration=0;iteration<5;iteration++){
                Candidate origin=best;
                for(double factor:new double[]{.985,1,1.015})for(int iy=-1;iy<=1;iy++)for(int ix=-1;ix<=1;ix++){
                    double w=origin.box.w()*factor,h=origin.box.h()*factor,area=w*h/(seed.w()*seed.h());
                    var b=new PixelTracker.Box(origin.box.x()+(origin.box.w()-w)/2+ix*step,origin.box.y()+(origin.box.h()-h)/2+iy*step,w,h);
                    if(area<.35||area>3||!b.measurable())continue;double q=score(pixels,b);count++;if(q>best.score)best=new Candidate(b,q,step,step);
                }
            }
            refined.add(best);
        }
        refined.sort(Comparator.comparingDouble(Candidate::score).reversed());var best=refined.get(0);double rival=-1;
        for(var c:all)if(PixelTracker.iou(c.box,best.box)<.25)rival=Math.max(rival,c.score);
        for(var c:refined)if(PixelTracker.iou(c.box,best.box)<.25)rival=Math.max(rival,c.score);
        return new Result(best.box,best.score,rival,count,best.score>=.90&&rival<best.score-.08);
    }
}
