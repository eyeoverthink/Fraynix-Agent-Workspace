package fraymus.workspace;

import java.awt.image.BufferedImage;
import java.util.*;
import org.json.*;

/** Search each fixed verified appearance independently; all references still veto rival locations. */
final class ReferenceSearch143 {
    private static final int[][] MASKS={{0,0,8,8},{0,0,6,8},{2,0,8,8},{0,0,8,6},{0,2,8,8}};
    private record Example(double[] pixels,String key){}
    private record Candidate(PixelTracker.Box box,double score,double error,int mask,Example example,double step){}
    record Result(PixelTracker.Box box,PixelTracker.Box support,double score,double rival,double error,int mask,String key,int evaluations,boolean unique){
        JSONObject json(){return new JSONObject().put("box",box.json()).put("visibleSupport",support.json()).put("wholeBoxInferred",mask!=0).put("visibleFraction",mask==0?1:.75).put("score",score).put("rivalScore",rival).put("meanColorError",error).put("mask",mask).put("sourceKey",key).put("evaluations",evaluations).put("unique",unique);}
    }
    private final List<Example> examples=new ArrayList<>();
    private final PixelTracker.Box seed;
    final String memoryHash;
    ReferenceSearch143(BufferedImage first,PixelTracker.Box seed,JSONObject memory,boolean imported){
        if(!seed.measurable())throw new IllegalArgumentException("Valid source seed required");this.seed=seed;memoryHash=WorldFrames.hash(memory);
        examples.add(new Example(PixelTracker.descriptor(PixelTracker.working(first),seed),"initial-seed"));
        var refs=memory.getJSONArray("examples");if(refs.isEmpty()||refs.length()>ReferenceMemory137.CAPACITY)throw new IllegalArgumentException("Bounded verified examples required");
        for(Object raw:refs){var e=(JSONObject)raw;var a=e.getJSONArray("descriptor");if(a.length()!=192||!WorldFrames.hash(new JSONObject().put("values",a)).equals(e.getString("descriptorSha256")))throw new IllegalArgumentException("Reference descriptor changed");
            var pixels=new double[192];for(int i=0;i<pixels.length;i++){pixels[i]=a.getDouble(i);if(!Double.isFinite(pixels[i])||pixels[i]<0||pixels[i]>1)throw new IllegalArgumentException("Invalid reference pixels");}
            String key=e.getString("imageSha256")+":"+e.getInt("ordinal");
            if(examples.stream().anyMatch(x->x.key.equals(key)))throw new IllegalArgumentException("Duplicate reference key");
            if(imported)examples.add(new Example(pixels,key));
        }
    }
    private static double[] sample(int[] image,PixelTracker.Box b){var d=new double[192];int k=0;for(int y=0;y<8;y++)for(int x=0;x<8;x++){int px=(int)((b.x()+(x+.5)*b.w()/8)*192),py=(int)((b.y()+(y+.5)*b.h()/8)*192),rgb=image[Math.max(0,Math.min(191,py))*192+Math.max(0,Math.min(191,px))];for(int c=0;c<3;c++)d[k++]=((rgb>>(16-c*8))&255)/255.;}return d;}
    // Grayscale texture rejects flat colored patches; independent RGB error guards lighting/color coincidence.
    private static double[] compare(double[] a,double[] b,int mask){int[] m=MASKS[mask];double sa=0,sb=0,aa=0,bb=0,ab=0,error=0;int n=0;
        for(int y=m[1];y<m[3];y++)for(int x=m[0];x<m[2];x++){int k=(y*8+x)*3;double va=.299*a[k]+.587*a[k+1]+.114*a[k+2],vb=.299*b[k]+.587*b[k+1]+.114*b[k+2];sa+=va;sb+=vb;aa+=va*va;bb+=vb*vb;ab+=va*vb;for(int c=0;c<3;c++)error+=Math.abs(a[k+c]-b[k+c])/3;n++;}
        double va=aa-sa*sa/n,vb=bb-sb*sb/n;return new double[]{va/n<.0005||vb/n<.0005?-1:Math.max(-1,Math.min(1,(ab-sa*sb/n)/Math.sqrt(va*vb))),error/n};
    }
    private Candidate best(double[] d,PixelTracker.Box box,double step,Example example){
        Candidate best=null;
        for(int mask=0;mask<MASKS.length;mask++){
            var q=compare(example.pixels,d,mask);double score=q[1]>.14?-1:q[0];
            if(best==null||score>best.score)best=new Candidate(box,score,q[1],mask,example,step);
        }
        return best;
    }
    record Batch(List<Result> candidates,int positionSamples,int appearanceEvaluations){
        JSONObject counts(){return new JSONObject().put("positionSamples",positionSamples).put("appearanceEvaluations",appearanceEvaluations).put("regionComparisons",(long)appearanceEvaluations*5).put("references",candidates.size());}
    }
    Batch search(BufferedImage original){
        var small=PixelTracker.working(original);int[] pixels=small.getRGB(0,0,192,192,null,0,192);
        var all=new ArrayList<Candidate>();var peaks=new ArrayList<List<Candidate>>();
        for(var ignored:examples)peaks.add(new ArrayList<>());
        int positions=0,comparisons=0;
        for(double f:new double[]{.75,.9,1,1.25,1.5}){
            double w=seed.w()*f,h=seed.h()*f;if(w<.025||h<.025||w>1||h>1)continue;
            double step=Math.max(1./192,Math.min(.025,Math.min(w,h)/3));
            var scale=new ArrayList<List<Candidate>>();for(var ignored:examples)scale.add(new ArrayList<>());
            int nx=(int)Math.ceil((1-w)/step),ny=(int)Math.ceil((1-h)/step);
            for(int y=0;y<=ny;y++)for(int x=0;x<=nx;x++){
                var box=new PixelTracker.Box(Math.min(1-w,x*step),Math.min(1-h,y*step),w,h);if(!box.measurable())continue;
                var d=sample(pixels,box);positions++;
                for(int e=0;e<examples.size();e++){var candidate=best(d,box,step,examples.get(e));scale.get(e).add(candidate);all.add(candidate);comparisons++;}
            }
            for(int e=0;e<examples.size();e++){
                var candidates=scale.get(e);candidates.sort(Comparator.comparingDouble(Candidate::score).reversed());
                var chosen=new ArrayList<Candidate>();
                for(var c:candidates){boolean separate=true;for(var p:chosen)if(PixelTracker.iou(c.box,p.box)>=.5){separate=false;break;}if(separate)chosen.add(c);if(chosen.size()==3)break;}
                peaks.get(e).addAll(chosen);
            }
        }
        var refined=new ArrayList<Candidate>();var bestByReference=new ArrayList<Candidate>();
        for(int e=0;e<examples.size();e++){
            Candidate referenceBest=null;
            for(var p:peaks.get(e)){
                Candidate winner=p;double step=1./768;
                for(double f:new double[]{.96,1,1.04}){
                    double w=p.box.w()*f,h=p.box.h()*f;if(w>1||h>1)continue;
                    for(double y=Math.max(0,p.box.y()-p.step);y<=Math.min(1-h,p.box.y()+p.step)+1e-10;y+=step)
                    for(double x=Math.max(0,p.box.x()-p.step);x<=Math.min(1-w,p.box.x()+p.step)+1e-10;x+=step){
                        var box=new PixelTracker.Box(x,y,w,h);if(!box.measurable())continue;
                        var c=best(sample(pixels,box),box,step,examples.get(e));positions++;comparisons++;if(c.score>winner.score)winner=c;
                    }
                }
                refined.add(winner);if(referenceBest==null||winner.score>referenceBest.score)referenceBest=winner;
            }
            bestByReference.add(referenceBest);
        }
        var results=new ArrayList<Result>();
        for(int e=0;e<examples.size();e++){
            var top=bestByReference.get(e);
            if(top==null){results.add(new Result(seed,seed,-1,-1,1,0,examples.get(e).key,positions,false));continue;}
            // Every appearance can veto a competing location. Evidence is never pooled between identities.
            double rival=-1;for(var c:all)if(PixelTracker.iou(c.box,top.box)<.25)rival=Math.max(rival,c.score);
            for(var c:refined)if(PixelTracker.iou(c.box,top.box)<.25)rival=Math.max(rival,c.score);
            int[] m=MASKS[top.mask];var support=new PixelTracker.Box(top.box.x()+m[0]*top.box.w()/8,top.box.y()+m[1]*top.box.h()/8,(m[2]-m[0])*top.box.w()/8,(m[3]-m[1])*top.box.h()/8);
            results.add(new Result(top.box,support,top.score,rival,top.error,top.mask,top.example.key,positions,top.score>=.93&&rival<top.score-.08&&support.measurable()));
        }
        return new Batch(List.copyOf(results),positions,comparisons);
    }
}
