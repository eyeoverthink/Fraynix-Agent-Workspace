package fraymus.workspace;

import java.awt.image.BufferedImage;
import java.util.*;
import org.json.*;

/** Exact original149 matcher with frame-local sampling and immutable reference terms. */
final class StructureReplay157 {
    private record Example(String key,double[] coarse,double[] fine,Template coarseTerms,Template fineTerms){}
    private static final class Template {
        final double[] gray,dx,dy; final double sum,squares,gradientNorm;
        Template(double[] rgb,int size){
            gray=new double[size*size];dx=new double[gray.length];dy=new double[gray.length];double s=0,ss=0,g=0;
            for(int i=0;i<gray.length;i++){double v=gray(rgb,i);gray[i]=v;s+=v;ss+=v*v;}
            // Preserve the original right-then-down accumulation order at every pixel.
            for(int y=0;y<size;y++)for(int x=0;x<size;x++){int i=y*size+x;
                if(x<size-1){double u=gray[i+1]-gray[i];dx[i]=u;g+=u*u;}
                if(y<size-1){double u=gray[i+size]-gray[i];dy[i]=u;g+=u*u;}
            }sum=s;squares=ss;gradientNorm=g;
        }
    }
    private record Peak(PixelTracker.Box box,double score,double step,Example ref){}
    record Match(PixelTracker.Box box,String key,double score,double colorError,double quadrantError,double gradient,double rival,boolean strong,boolean unique){
        JSONObject json(){return new JSONObject().put("box",box.json()).put("sourceKey",key).put("score",score).put("colorError",colorError).put("quadrantError",quadrantError).put("gradient",gradient).put("rivalScore",rival).put("fullObject",true).put("strong",strong).put("unique",unique);}
    }
    private final List<Example> refs=new ArrayList<>();
    private final Map<String,Peak> observedSearchSeeds=new LinkedHashMap<>();
    private final PixelTracker.Box seed;
    private final int width,height;
    private int evaluations;
    private int[] framePixels;
    private double[] frameGray;
    private final int[] xs=new int[16],coarseIndices=new int[64],fineIndices=new int[256];
    private final boolean precomputed;
    StructureReplay157(BufferedImage first,PixelTracker.Box seed,JSONObject memory,boolean precomputed){
        if(!seed.measurable())throw new IllegalArgumentException("Measurable seed required");
        this.precomputed=precomputed;this.seed=seed;width=first.getWidth();height=first.getHeight();
        var initial=descriptor(first,seed);refs.add(example("initial-seed",initial));
        var examples=memory.getJSONArray("examples");
        if(examples.isEmpty()||examples.length()>ReferenceMemory137.CAPACITY)throw new IllegalArgumentException("Bounded verified references required");
        for(Object raw:examples){var e=(JSONObject)raw;String key=e.getString("imageSha256")+":"+e.getInt("ordinal");
            if(refs.stream().anyMatch(r->r.key.equals(key)))throw new IllegalArgumentException("Duplicate immutable reference");
            refs.add(example(key,e.getJSONObject("structure149")));
        }
    }
    static JSONObject descriptor(BufferedImage image,PixelTracker.Box box){return StructureSearch149.descriptor(image,box);}
    private static Example example(String key,JSONObject descriptor){
        var content=ParsedJson137.copy(descriptor);String hash=content.getString("sha256");content.remove("sha256");
        if(!"full-structure-149-v1".equals(content.getString("schema"))||!WorldFrames.hash(content).equals(hash))throw new IllegalArgumentException("Structure descriptor changed");
        PixelTracker.Box.from(content.getJSONArray("box"));
        double[] coarse=values(content.getJSONArray("coarse"),192),fine=values(content.getJSONArray("pixels"),768);
        return new Example(key,coarse,fine,new Template(coarse,8),new Template(fine,16));
    }
    private static double[] values(JSONArray a,int size){
        if(a.length()!=size)throw new IllegalArgumentException("Invalid structure dimensions");var d=new double[size];
        for(int i=0;i<size;i++){d[i]=a.getDouble(i);if(!Double.isFinite(d[i])||d[i]<0||d[i]>1)throw new IllegalArgumentException("Invalid structure pixels");}return d;
    }
    private double[] sample(BufferedImage image,PixelTracker.Box b,int size){
        var d=new double[size*size*3];int k=0;for(int y=0;y<size;y++)for(int x=0;x<size;x++){
            int px=Math.min(image.getWidth()-1,Math.max(0,(int)((b.x()+(x+.5)*b.w()/size)*image.getWidth())));
            int py=Math.min(image.getHeight()-1,Math.max(0,(int)((b.y()+(y+.5)*b.h()/size)*image.getHeight())));
            int rgb=framePixels[py*width+px];for(int c=0;c<3;c++)d[k++]=((rgb>>(16-c*8))&255)/255.;
        }return d;
    }
    private static double gray(double[] a,int p){return .299*a[p*3]+.587*a[p*3+1]+.114*a[p*3+2];}
    static double pixelGray(int rgb){return .299*(((rgb>>16)&255)/255.)+.587*(((rgb>>8)&255)/255.)+.114*((rgb&255)/255.);}
    private void indices(PixelTracker.Box b,int size,int[] out){
        // The original expression/order is retained; x coordinates repeat for every row.
        for(int x=0;x<size;x++)xs[x]=Math.min(width-1,Math.max(0,(int)((b.x()+(x+.5)*b.w()/size)*width)));
        int k=0;for(int y=0;y<size;y++){
            int py=Math.min(height-1,Math.max(0,(int)((b.y()+(y+.5)*b.h()/size)*height)));
            for(int x=0;x<size;x++)out[k++]=py*width+xs[x];
        }
    }
    private double correlation(Template a,int[] pixels){
        double sb=0,bb=0,ab=0;int n=pixels.length;
        for(int i=0;i<n;i++){double x=a.gray[i],y=frameGray[pixels[i]];sb+=y;bb+=y*y;ab+=x*y;}
        double va=a.squares-a.sum*a.sum/n,vb=bb-sb*sb/n;
        return va/n<.0005||vb/n<.0005?-1:Math.max(-1,Math.min(1,(ab-a.sum*sb/n)/Math.sqrt(va*vb)));
    }
    private Match precomputedFull(Peak p){
        indices(p.box,16,fineIndices);var a=p.ref.fine;var t=p.ref.fineTerms;double error=0,gb=0,gab=0;var quadrants=new double[4];
        for(int y=0;y<16;y++)for(int x=0;x<16;x++){int i=y*16+x,q=(y/8)*2+x/8,rgb=framePixels[fineIndices[i]];double e=0;
            for(int c=0;c<3;c++)e+=Math.abs(a[i*3+c]-((rgb>>(16-c*8))&255)/255.)/3;
            error+=e;quadrants[q]+=e;double current=frameGray[fineIndices[i]];
            if(x<15){double u=t.dx[i],v=frameGray[fineIndices[i+1]]-current;gb+=v*v;gab+=u*v;}
            if(y<15){double u=t.dy[i],v=frameGray[fineIndices[i+16]]-current;gb+=v*v;gab+=u*v;}
        }
        error/=256;double quadrant=Arrays.stream(quadrants).max().orElse(64)/64;
        double ga=t.gradientNorm,gradient=ga<.001||gb<.001?-1:gab/Math.sqrt(ga*gb),score=correlation(t,fineIndices);
        boolean strong=score>=.94&&error<=.10&&quadrant<=.14&&gradient>=.85;
        evaluations++;return new Match(p.box,p.ref.key,score,error,quadrant,gradient,-1,strong,false);
    }
    private static double correlation(double[] a,double[] b){
        double sa=0,sb=0,aa=0,bb=0,ab=0;int n=a.length/3;
        for(int i=0;i<n;i++){double x=gray(a,i),y=gray(b,i);sa+=x;sb+=y;aa+=x*x;bb+=y*y;ab+=x*y;}
        double va=aa-sa*sa/n,vb=bb-sb*sb/n;
        return va/n<.0005||vb/n<.0005?-1:Math.max(-1,Math.min(1,(ab-sa*sb/n)/Math.sqrt(va*vb)));
    }
    private Match full(BufferedImage image,Peak p){
        if(precomputed)return precomputedFull(p);
        double[] b=sample(image,p.box,16),a=p.ref.fine;double error=0,ga=0,gb=0,gab=0;var quadrants=new double[4];
        for(int y=0;y<16;y++)for(int x=0;x<16;x++){int i=y*16+x,q=(y/8)*2+x/8;double e=0;for(int c=0;c<3;c++)e+=Math.abs(a[i*3+c]-b[i*3+c])/3;error+=e;quadrants[q]+=e;
            if(x<15){double u=gray(a,i+1)-gray(a,i),v=gray(b,i+1)-gray(b,i);ga+=u*u;gb+=v*v;gab+=u*v;}
            if(y<15){double u=gray(a,i+16)-gray(a,i),v=gray(b,i+16)-gray(b,i);ga+=u*u;gb+=v*v;gab+=u*v;}
        }
        error/=256;double quadrant=Arrays.stream(quadrants).max().orElse(64)/64;
        double gradient=ga<.001||gb<.001?-1:gab/Math.sqrt(ga*gb),score=correlation(a,b);
        boolean strong=score>=.94&&error<=.10&&quadrant<=.14&&gradient>=.85;
        evaluations++;return new Match(p.box,p.ref.key,score,error,quadrant,gradient,-1,strong,false);
    }
    List<Match> search(BufferedImage image){
        if(image.getWidth()!=width||image.getHeight()!=height)throw new IllegalArgumentException("Frame dimensions changed");
        if(width>1920||height>1080)throw new IllegalArgumentException("Bounded original image required");
        framePixels=image.getRGB(0,0,width,height,null,0,width);
        try {
        if(precomputed){frameGray=new double[framePixels.length];for(int i=0;i<framePixels.length;i++)frameGray[i]=pixelGray(framePixels[i]);}
        evaluations=0;var peaks=new ArrayList<Peak>(observedSearchSeeds.values());
        for(double f:new double[]{.75,.9,1,1.25,1.5}){
            double w=seed.w()*f,h=seed.h()*f;if(w<.025||h<.025||w>1||h>1)continue;
            double step=Math.max(1./192,Math.min(.025,Math.min(w,h)/3));var candidates=new ArrayList<List<Peak>>();for(var ref:refs)candidates.add(new ArrayList<>());
            for(int y=0;y<=Math.ceil((1-h)/step);y++)for(int x=0;x<=Math.ceil((1-w)/step);x++){
                var box=new PixelTracker.Box(Math.min(1-w,x*step),Math.min(1-h,y*step),w,h);if(!box.measurable())continue;
                double[] d=null;if(precomputed)indices(box,8,coarseIndices);else d=sample(image,box,8);
                for(int r=0;r<refs.size();r++){var ref=refs.get(r);double score=precomputed?correlation(ref.coarseTerms,coarseIndices):correlation(ref.coarse,d);candidates.get(r).add(new Peak(box,score,step,ref));evaluations++;}
            }
            for(var candidatesForRef:candidates){candidatesForRef.sort(Comparator.comparingDouble(Peak::score).reversed());var chosen=new ArrayList<Peak>();
                for(var p:candidatesForRef){if(chosen.stream().noneMatch(q->PixelTracker.iou(p.box,q.box)>=.5))chosen.add(p);if(chosen.size()==3)break;}peaks.addAll(chosen);
            }
        }
        var refined=new ArrayList<Match>();
        for(var peak:peaks){Match best=full(image,peak);
            for(double f:new double[]{.96,1,1.04}){double w=peak.box.w()*f,h=peak.box.h()*f;if(w>1||h>1)continue;
                for(double y=Math.max(0,peak.box.y()-peak.step);y<=Math.min(1-h,peak.box.y()+peak.step)+1e-10;y+=1./768)
                for(double x=Math.max(0,peak.box.x()-peak.step);x<=Math.min(1-w,peak.box.x()+peak.step)+1e-10;x+=1./768){
                    var box=new PixelTracker.Box(x,y,w,h);if(!box.measurable())continue;var found=full(image,new Peak(box,0,0,peak.ref));if(found.score>best.score)best=found;
                }
            }refined.add(best);
        }
        var result=new ArrayList<Match>();observedSearchSeeds.clear();
        for(var ref:refs){var top=refined.stream().filter(p->p.key.equals(ref.key)).max(Comparator.comparingDouble(Match::score)).orElseThrow();
            double rival=refined.stream().filter(p->PixelTracker.iou(p.box,top.box)<.25&&p.colorError<=.14).mapToDouble(Match::score).max().orElse(-1);
            result.add(new Match(top.box,top.key,top.score,top.colorError,top.quadrantError,top.gradient,rival,top.strong,top.strong&&rival<top.score-.08));
            // A prior strong crop proposes one bounded search region only. Its old pixels, score and
            // position do not count as current evidence; full matching and global rivals run again.
            if(top.strong)observedSearchSeeds.put(ref.key,new Peak(top.box,top.score,.025,ref));
        }return List.copyOf(result);
        } finally { framePixels=null;frameGray=null; }
    }
    int evaluations(){return evaluations;}
}
