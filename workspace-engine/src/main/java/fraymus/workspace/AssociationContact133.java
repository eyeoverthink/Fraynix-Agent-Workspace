package fraymus.workspace;

import java.awt.image.BufferedImage;
import java.util.*;
import org.json.*;

/** Explicit research method. Frozen source appearance, bounded association and visual support.
 * No annotations, class names, future pixels, metric coordinates or model inference enter it. */
final class AssociationContact133 implements ImageTracker {
    private static final int GX=10,GY=18;
    private final String id,label;
    private final double[] anchor;
    private final double[] lowerEvidence=new double[512];
    private final int width,height;
    private final PixelTracker.Box seed;
    private PixelTracker.Box box,pending;
    private int lost,observations,accepted;
    private record Match(PixelTracker.Box box,double score,double rank){}

    AssociationContact133(String id,String label,BufferedImage first,PixelTracker.Box seed){
        if(!seed.measurable())throw new IllegalArgumentException("Valid source seed required");
        this.id=id;this.label=label;this.seed=seed;box=seed;width=first.getWidth();height=first.getHeight();
        anchor=descriptor(first,seed);initializeLower(first,seed);
    }
    private static int bin(int c){return ((c>>21)&7)*64+((c>>13)&7)*8+((c>>5)&7);}
    private void initializeLower(BufferedImage image,PixelTracker.Box b){
        double[] fg=new double[512],bg=new double[512];int nf=0,nb=0;
        for(int y=0;y<height;y++)for(int x=0;x<width;x++){
            double u=(x+.5)/width,v=(y+.5)/height;
            if(v<b.y()+.55*b.h()||v>b.y()+1.08*b.h()||u<b.x()-b.w()||u>b.x()+2*b.w())continue;
            if(u>=b.x()&&u<=b.x()+b.w()&&v<=b.y()+b.h()){fg[bin(image.getRGB(x,y))]++;nf++;}
            else{bg[bin(image.getRGB(x,y))]++;nb++;}
        }
        for(int i=0;i<512;i++)lowerEvidence[i]=((fg[i]+.2)/(nf+102.4))/((bg[i]+.2)/(nb+102.4));
    }
    private static double[] descriptor(BufferedImage image,PixelTracker.Box b){
        var d=new double[GX*GY*3];int k=0;
        for(int y=0;y<GY;y++)for(int x=0;x<GX;x++){
            int c=image.getRGB(Math.min(image.getWidth()-1,(int)((b.x()+(x+.5)*b.w()/GX)*image.getWidth())),Math.min(image.getHeight()-1,(int)((b.y()+(y+.5)*b.h()/GY)*image.getHeight())));
            for(int shift:new int[]{16,8,0})d[k++]=((c>>shift)&255)/255.;
        }
        return d;
    }
    private double score(BufferedImage image,PixelTracker.Box b){
        double sa=0,sb=0,aa=0,bb=0,ab=0,error=0;int k=0;
        for(int y=0;y<GY;y++)for(int x=0;x<GX;x++){
            int c=image.getRGB(Math.min(width-1,(int)((b.x()+(x+.5)*b.w()/GX)*width)),Math.min(height-1,(int)((b.y()+(y+.5)*b.h()/GY)*height)));
            for(int shift=16;shift>=0;shift-=8){double a=anchor[k++],v=((c>>shift)&255)/255.;sa+=a;sb+=v;aa+=a*a;bb+=v*v;ab+=a*v;error+=Math.abs(a-v);}
        }
        double va=aa-sa*sa/k,vb=bb-sb*sb/k;
        double correlation=va>1e-5&&vb>1e-5?(ab-sa*sb/k)/Math.sqrt(va*vb):0;
        return .7*correlation+.3*Math.max(0,1-3*error/k);
    }
    private double distance(PixelTracker.Box a,PixelTracker.Box b){return Math.hypot((a.x()+a.w()/2-b.x()-b.w()/2)*width,(a.y()+a.h()/2-b.y()-b.h()/2)*height);}
    private Match match(BufferedImage image){
        Match best=null;double range=Math.min(.32,.035+lost*.025);
        for(double factor:new double[]{.85,.925,1,1.075,1.15}){
            double w=seed.w()*factor,h=seed.h()*factor;
            if(w>=1||h>=1)continue;
            double stepX=Math.max(2,width*w/5)/width,stepY=Math.max(2,height*h/12)/height;
            Match coarse=null;
            for(double y=Math.max(0,box.y()-range);y<=Math.min(1-h,box.y()+range)+1e-9;y+=stepY)
                for(double x=Math.max(0,box.x()-range);x<=Math.min(1-w,box.x()+range)+1e-9;x+=stepX){
                    var b=new PixelTracker.Box(x,y,w,h);if(!b.measurable())continue;double score=score(image,b),rank=rank(score,b);if(coarse==null||rank>coarse.rank)coarse=new Match(b,score,rank);
                }
            if(coarse==null)continue;
            if(best==null||coarse.rank>best.rank)best=coarse;var origin=coarse.box;
            for(double y=Math.max(0,origin.y()-stepY);y<=Math.min(1-h,origin.y()+stepY)+1e-9;y+=1./height)
                for(double x=Math.max(0,origin.x()-stepX);x<=Math.min(1-w,origin.x()+stepX)+1e-9;x+=1./width){
                    var b=new PixelTracker.Box(x,y,w,h);double score=score(image,b),rank=rank(score,b);if(rank>best.rank)best=new Match(b,score,rank);
                }
        }
        return best;
    }
    private double rank(double score,PixelTracker.Box b){return score-.035*distance(b,box)/(height*box.h())-.025*Math.abs(Math.log(b.w()/seed.w()));}
    private double competitor(BufferedImage image,Match best){
        double top=-1,w=best.box.w(),h=best.box.h();
        double sx=Math.max(3,w*width/3)/width,sy=Math.max(3,h*height/8)/height;PixelTracker.Box candidate=null;
        for(double y=0;y<=1-h;y+=sy)for(double x=0;x<=1-w;x+=sx){var b=new PixelTracker.Box(x,y,w,h);if(PixelTracker.iou(b,best.box)>.15)continue;double s=score(image,b);if(s>top){top=s;candidate=b;}}
        if(candidate!=null&&top>.4){var coarse=candidate;
            for(double y=Math.max(0,best.box.y()+Math.ceil((coarse.y()-sy-best.box.y())*height)/height);y<=Math.min(1-h,coarse.y()+sy);y+=1./height)for(double x=Math.max(0,best.box.x()+Math.ceil((coarse.x()-sx-best.box.x())*width)/width);x<=Math.min(1-w,coarse.x()+sx);x+=1./width){var b=new PixelTracker.Box(x,y,w,h);if(PixelTracker.iou(b,best.box)<=.15)top=Math.max(top,score(image,b));}
        }
        return top;
    }
    public JSONObject update(BufferedImage image){
        if(image.getWidth()!=width||image.getHeight()!=height)throw new IllegalArgumentException("Original image dimensions changed");
        Match found=match(image);double rival=found==null?-1:competitor(image,found);
        boolean ambiguous=found!=null&&rival>=found.score-.025;
        boolean strong=found!=null&&found.score>=.70&&!ambiguous;
        boolean confirmed=strong&&(lost==0||(pending!=null&&PixelTracker.iou(pending,found.box)>=.55));
        String reason=found==null?"no valid search box":ambiguous?"competing source-appearance match":!strong?"frozen appearance mismatch":!confirmed?"waiting for a second source image":"source appearance and separation accepted";
        String status=confirmed?(lost>0?"reacquired":"tracked"):"lost";
        if(confirmed){box=found.box;lost=0;pending=null;accepted++;}else{lost++;pending=strong?found.box:null;}
        observations++;
        var contact=confirmed?support(image,box):unavailable("object association unconfirmed");
        return new JSONObject().put("id",id).put("label",label).put("status",status).put("box",confirmed?box.json():JSONObject.NULL).put("lastKnownBox",box.json())
            .put("matchScore",found==null?0:found.score).put("appearanceUpdates",0).put("scoreMeaning","Frozen RGB appearance agreement; not semantic identity or calibrated confidence")
            .put("association",new JSONObject().put("method","seed-appearance-133").put("reason",reason).put("competitorScore",rival).put("ambiguous",ambiguous).put("pending",pending!=null).put("observations",observations).put("accepted",accepted).put("lost",lost))
            .put("contact133",contact);
    }
    private static JSONObject unavailable(String reason){return new JSONObject().put("status","unobservable").put("reason",reason).put("pointImage",JSONObject.NULL).put("meaning","Visual support candidate only; not proof of physical ground contact");}
    JSONObject support(BufferedImage image,PixelTracker.Box b){
        // A finite connected lower-object foreground is required. The box bottom is never the answer by fallback.
        int x0=Math.max(0,(int)((b.x()-.7*b.w())*width)),x1=Math.min(width-1,(int)((b.x()+1.7*b.w())*width));
        int y0=Math.max(0,(int)((b.y()+.45*b.h())*height)),y1=Math.min(height-1,(int)((b.y()+1.18*b.h())*height));
        int rw=x1-x0+1,rh=y1-y0+1;if(rw<3||rh<3)return unavailable("insufficient lower-object pixels");
        boolean[] mask=new boolean[rw*rh],seen=new boolean[mask.length];
        for(int y=0;y<rh;y++)for(int x=0;x<rw;x++)mask[y*rw+x]=lowerEvidence[bin(image.getRGB(x+x0,y+y0))]>=1.6;
        var components=new ArrayList<int[]>();var queue=new int[mask.length];
        for(int start=0;start<mask.length;start++)if(mask[start]&&!seen[start]){
            int size=1,at=0;queue[0]=start;seen[start]=true;
            while(at<size){int p=queue[at++],x=p%rw,y=p/rw;for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){int nx=x+dx,ny=y+dy;if(nx>=0&&ny>=0&&nx<rw&&ny<rh){int n=ny*rw+nx;if(mask[n]&&!seen[n]){seen[n]=true;queue[size++]=n;}}}}
            if(size>=Math.max(6,b.w()*width*b.h()*height*.015)){int inside=0;for(int i=0;i<size;i++){double x=(queue[i]%rw+x0+.5)/width,y=(queue[i]/rw+y0+.5)/height;if(x>=b.x()&&x<=b.x()+b.w()&&y<=b.y()+.95*b.h())inside++;}if(inside>=size*.2)components.add(Arrays.copyOf(queue,size));}
        }
        if(components.isEmpty())return unavailable("no connected lower-object support");
        components.sort(Comparator.comparingInt((int[] c)->c.length).reversed());var part=components.get(0);
        if(part.length>rw*rh*.65)return unavailable("foreground/background separation insufficient");
        int[] rows=new int[rh];for(int p:part)rows[p/rw]++;
        int bottom=-1;for(int y=rh-1;y>=0;y--)if(rows[y]>=Math.max(2,b.w()*width*.08)){bottom=y;break;}
        if(bottom<0||bottom+y0<(b.y()+.85*b.h())*height)return unavailable("lower support not visible");
        if(bottom>=rh-2||bottom+y0>=height-2)return unavailable("support clipped by image or search boundary");
        int count=0;double sum=0;int lo=width,hi=0;for(int p:part)if(p/rw>=bottom-1&&p/rw<=bottom){int x=p%rw+x0;sum+=x+.5;count++;lo=Math.min(lo,x);hi=Math.max(hi,x);}
        if(count<2||lo<=0||hi>=width-1)return unavailable("support clipped or too sparse");
        return new JSONObject().put("status","visual-support").put("reason","Connected seed-contrasting lower-object pixels terminate inside image/search region")
            .put("pointImage",new JSONArray(new double[]{sum/count/width,(bottom+y0+1.)/height})).put("supportPixels",count).put("componentPixels",part.length).put("spanPixels",hi-lo+1)
            .put("meaning","Visual support candidate only; not proof of physical ground contact");
    }
}
