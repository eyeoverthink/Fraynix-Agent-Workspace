package fraymus.workspace;

import java.awt.image.BufferedImage;
import java.util.*;
import org.json.*;

/** Immutable source appearance; no prediction or outcome label can modify it. */
final class AppearanceEvidence138 {
    static final String VERSION="immutable-appearance-138-v1";
    final PixelTracker.Box seed;
    final double[] descriptor,foreground,background;
    final double sourceSupport,backgroundSupport;
    final String hash;
    record Search(PixelTracker.Box box,double score,double rival,boolean unique){}

    AppearanceEvidence138(BufferedImage first,PixelTracker.Box seed){
        if(!seed.measurable())throw new IllegalArgumentException("Measurable source seed required");
        this.seed=seed;var small=PixelTracker.working(first);descriptor=PixelTracker.descriptor(small,seed);
        foreground=histogram(samples(small,seed,false));background=histogram(samples(small,seed,true));
        sourceSupport=support(foreground);backgroundSupport=support(background);
        hash=WorldFrames.hash(new JSONObject().put("version",VERSION).put("seed",seed.json())
            .put("descriptor",new JSONArray(descriptor)).put("foreground",new JSONArray(foreground)).put("background",new JSONArray(background)));
    }
    private static double[] samples(BufferedImage image,PixelTracker.Box b,boolean ring){
        var out=new ArrayList<Double>();
        for(int y=0;y<24;y++)for(int x=0;x<24;x++){
            double u=(x+.5)/24,v=(y+.5)/24;
            if(ring&&u>.2&&u<.8&&v>.2&&v<.8)continue;
            double px=b.x()+b.w()*(ring?u*1.6-.3:.15+.7*u),py=b.y()+b.h()*(ring?v*1.6-.3:.15+.7*v);
            if(px<0||py<0||px>=1||py>=1)continue;
            int c=image.getRGB((int)(px*image.getWidth()),(int)(py*image.getHeight()));
            out.add(((c>>16)&255)/255.);out.add(((c>>8)&255)/255.);out.add((c&255)/255.);
        }
        return out.stream().mapToDouble(Double::doubleValue).toArray();
    }
    private static double[] histogram(double[] colors){
        var h=new double[64];if(colors.length==0)return h;
        for(int i=0;i<colors.length;i+=3){
            var lo=new int[3];var f=new double[3];
            for(int c=0;c<3;c++){double v=colors[i+c]*3;lo[c]=(int)v;f[c]=v-lo[c];}
            for(int mask=0;mask<8;mask++){double w=1;int bin=0;for(int c=0;c<3;c++){int bit=(mask>>c)&1;w*=bit==0?1-f[c]:f[c];bin=bin*4+Math.min(3,lo[c]+bit);}h[bin]+=w;}
        }
        for(int i=0;i<h.length;i++)h[i]/=colors.length/3.;return h;
    }
    private static double agreement(double[] a,double[] b){double s=0;for(int i=0;i<a.length;i++)s+=Math.sqrt(a[i]*b[i]);return Math.min(1,s);}
    private double support(double[] h){double s=0;for(int i=0;i<h.length;i++)s+=h[i]*(foreground[i]+.001)/(foreground[i]+background[i]+.002);return s;}
    JSONObject assess(BufferedImage small,PixelTracker.Box box){
        var d=PixelTracker.descriptor(small,box);var h=histogram(samples(small,box,false));
        double correlation=PixelTracker.correlation(descriptor,d),color=agreement(foreground,h),occupancy=support(h);
        double ratio=box.w()*box.h()/(seed.w()*seed.h());
        return new JSONObject().put("correlation",correlation).put("colorAgreement",color).put("foregroundSupport",occupancy)
            .put("sourceSupport",sourceSupport).put("backgroundSupport",backgroundSupport).put("areaRatio",ratio);
    }
    static JSONArray reasons(JSONObject e){
        var reasons=new JSONArray();
        if(e.getDouble("colorAgreement")<.62)reasons.put("Original object colors disagree");
        if(e.getDouble("correlation")<.1&&e.getDouble("colorAgreement")<.85)reasons.put("Original spatial appearance and colors disagree");
        double gap=e.getDouble("sourceSupport")-e.getDouble("backgroundSupport");
        if(gap>.15&&e.getDouble("foregroundSupport")<e.getDouble("backgroundSupport")+.35*gap)reasons.put("Proposal resembles source background more than foreground");
        if(e.getDouble("areaRatio")<.35||e.getDouble("areaRatio")>3)reasons.put("Scale differs beyond the source-supported bound");
        if(e.optBoolean("ambiguous"))reasons.put("A separate source-like appearance competes with this proposal");
        return reasons;
    }
    Search search(BufferedImage small){
        var match=new PixelTracker("reference","initial source",descriptor,seed,false).match(small,true);
        double rival=rival(small,match.box());
        return new Search(match.box(),match.score(),rival,match.score()>=.88&&rival<match.score()-.05);
    }
    double rival(BufferedImage image,PixelTracker.Box winner){
        double top=-1;
        for(double scale:new double[]{.9,1,1.1}){
            double w=winner.w()*scale,h=winner.h()*scale;if(w>1||h>1)continue;
            double best=-1,bx=0,by=0;
            for(double y=0;y<=1-h;y+=.025)for(double x=0;x<=1-w;x+=.025){
                var box=new PixelTracker.Box(x,y,w,h);if(PixelTracker.iou(box,winner)>=.25)continue;
                double score=PixelTracker.correlation(descriptor,PixelTracker.descriptor(image,box));if(score>best){best=score;bx=x;by=y;}
            }
            top=Math.max(top,best);if(best<.5)continue;
            for(double y=Math.max(0,by-.025);y<=Math.min(1-h,by+.025);y+=.002)
                for(double x=Math.max(0,bx-.025);x<=Math.min(1-w,bx+.025);x+=.002){
                    var box=new PixelTracker.Box(x,y,w,h);if(PixelTracker.iou(box,winner)<.25)top=Math.max(top,PixelTracker.correlation(descriptor,PixelTracker.descriptor(image,box)));
                }
        }
        return top;
    }
}
