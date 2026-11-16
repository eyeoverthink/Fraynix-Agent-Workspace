package fraymus.workspace;

import java.awt.image.BufferedImage;
import java.util.*;
import org.json.*;

/** Full-object matching against immutable source crops. No predicted crop is admitted. */
final class StructureSearch149 {
    private record Example(String key,double[] coarse,double[] fine){}
    private record Peak(PixelTracker.Box box,double score,double step,Example ref){}
    record Match(PixelTracker.Box box,String key,double score,double colorError,double quadrantError,double gradient,double rival,boolean strong,boolean unique){
        JSONObject json(){return new JSONObject().put("box",box.json()).put("sourceKey",key).put("score",score).put("colorError",colorError).put("quadrantError",quadrantError).put("gradient",gradient).put("rivalScore",rival).put("fullObject",true).put("strong",strong).put("unique",unique);}
    }
    private final List<Example> refs=new ArrayList<>();
    private final PixelTracker.Box seed;
    private final int width,height;
    private int evaluations;
    StructureSearch149(BufferedImage first,PixelTracker.Box seed,JSONObject memory){
        if(!seed.measurable())throw new IllegalArgumentException("Measurable seed required");
        this.seed=seed;width=first.getWidth();height=first.getHeight();
        var initial=descriptor(first,seed);refs.add(example("initial-seed",initial));
        var examples=memory.getJSONArray("examples");
        if(examples.isEmpty()||examples.length()>ReferenceMemory137.CAPACITY)throw new IllegalArgumentException("Bounded verified references required");
        for(Object raw:examples){var e=(JSONObject)raw;String key=e.getString("imageSha256")+":"+e.getInt("ordinal");
            if(refs.stream().anyMatch(r->r.key.equals(key)))throw new IllegalArgumentException("Duplicate immutable reference");
            refs.add(example(key,e.getJSONObject("structure149")));
        }
    }
    static JSONObject descriptor(BufferedImage image,PixelTracker.Box box){
        if(!box.measurable()||box.x()<0||box.y()<0||box.x()+box.w()>1.0000001||box.y()+box.h()>1.0000001)throw new IllegalArgumentException("Complete reference box required");
        var fine=new JSONArray(sample(image,box,16));var coarse=new JSONArray(sample(image,box,8));
        var content=new JSONObject().put("schema","full-structure-149-v1").put("pixels",fine).put("coarse",coarse).put("box",box.json());
        return content.put("sha256",WorldFrames.hash(content));
    }
    private static Example example(String key,JSONObject descriptor){
        var content=ParsedJson137.copy(descriptor);String hash=content.getString("sha256");content.remove("sha256");
        if(!"full-structure-149-v1".equals(content.getString("schema"))||!WorldFrames.hash(content).equals(hash))throw new IllegalArgumentException("Structure descriptor changed");
        PixelTracker.Box.from(content.getJSONArray("box"));
        return new Example(key,values(content.getJSONArray("coarse"),192),values(content.getJSONArray("pixels"),768));
    }
    private static double[] values(JSONArray a,int size){
        if(a.length()!=size)throw new IllegalArgumentException("Invalid structure dimensions");var d=new double[size];
        for(int i=0;i<size;i++){d[i]=a.getDouble(i);if(!Double.isFinite(d[i])||d[i]<0||d[i]>1)throw new IllegalArgumentException("Invalid structure pixels");}return d;
    }
    private static double[] sample(BufferedImage image,PixelTracker.Box b,int size){
        var d=new double[size*size*3];int k=0;for(int y=0;y<size;y++)for(int x=0;x<size;x++){
            int px=Math.min(image.getWidth()-1,Math.max(0,(int)((b.x()+(x+.5)*b.w()/size)*image.getWidth())));
            int py=Math.min(image.getHeight()-1,Math.max(0,(int)((b.y()+(y+.5)*b.h()/size)*image.getHeight())));
            int rgb=image.getRGB(px,py);for(int c=0;c<3;c++)d[k++]=((rgb>>(16-c*8))&255)/255.;
        }return d;
    }
    private static double gray(double[] a,int p){return .299*a[p*3]+.587*a[p*3+1]+.114*a[p*3+2];}
    private static double correlation(double[] a,double[] b){
        double sa=0,sb=0,aa=0,bb=0,ab=0;int n=a.length/3;
        for(int i=0;i<n;i++){double x=gray(a,i),y=gray(b,i);sa+=x;sb+=y;aa+=x*x;bb+=y*y;ab+=x*y;}
        double va=aa-sa*sa/n,vb=bb-sb*sb/n;
        return va/n<.0005||vb/n<.0005?-1:Math.max(-1,Math.min(1,(ab-sa*sb/n)/Math.sqrt(va*vb)));
    }
    private Match full(BufferedImage image,Peak p){
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
        evaluations=0;var peaks=new ArrayList<Peak>();
        for(double f:new double[]{.75,.9,1,1.25,1.5}){
            double w=seed.w()*f,h=seed.h()*f;if(w<.025||h<.025||w>1||h>1)continue;
            double step=Math.max(1./192,Math.min(.025,Math.min(w,h)/3));var candidates=new ArrayList<List<Peak>>();for(var ref:refs)candidates.add(new ArrayList<>());
            for(int y=0;y<=Math.ceil((1-h)/step);y++)for(int x=0;x<=Math.ceil((1-w)/step);x++){
                var box=new PixelTracker.Box(Math.min(1-w,x*step),Math.min(1-h,y*step),w,h);var d=sample(image,box,8);
                for(int r=0;r<refs.size();r++){var ref=refs.get(r);candidates.get(r).add(new Peak(box,correlation(ref.coarse,d),step,ref));evaluations++;}
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
                    var found=full(image,new Peak(new PixelTracker.Box(x,y,w,h),0,0,peak.ref));if(found.score>best.score)best=found;
                }
            }refined.add(best);
        }
        var result=new ArrayList<Match>();
        for(var ref:refs){var top=refined.stream().filter(p->p.key.equals(ref.key)).max(Comparator.comparingDouble(Match::score)).orElseThrow();
            double rival=refined.stream().filter(p->PixelTracker.iou(p.box,top.box)<.25&&p.colorError<=.14).mapToDouble(Match::score).max().orElse(-1);
            result.add(new Match(top.box,top.key,top.score,top.colorError,top.quadrantError,top.gradient,rival,top.strong,top.strong&&rival<top.score-.08));
        }return List.copyOf(result);
    }
    int evaluations(){return evaluations;}
}
