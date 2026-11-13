package fraymus.workspace;

import java.awt.image.BufferedImage;
import java.util.*;
import org.json.*;

/** Explicit research method. Frozen source appearance, bounded association and visual support.
 * No annotations, class names, future pixels, metric coordinates or model inference enter it. */
final class AssociationContact133 implements ImageTracker {
    private static final int GX=10,GY=18;
    private final String id,label;
    private final double[] anchor,context;
    private final VisualSupport133 supportEstimator;
    private final int width,height;
    private final PixelTracker.Box seed;
    private PixelTracker.Box box,pending;
    private BufferedImage previousAccepted;
    private int lost,observations,accepted;
    private record Match(PixelTracker.Box box,double score,double rank){}

    AssociationContact133(String id,String label,BufferedImage first,PixelTracker.Box seed){
        if(!seed.measurable())throw new IllegalArgumentException("Valid source seed required");
        this.id=id;this.label=label;this.seed=seed;box=seed;width=first.getWidth();height=first.getHeight();
        previousAccepted=first;anchor=descriptor(first,seed);context=descriptor(first,contextBox(seed));supportEstimator=new VisualSupport133(first,seed);
    }
    private static double[] descriptor(BufferedImage image,PixelTracker.Box b){
        var d=new double[GX*GY*3];int k=0;
        for(int y=0;y<GY;y++)for(int x=0;x<GX;x++){
            int c=image.getRGB(Math.min(image.getWidth()-1,(int)((b.x()+(x+.5)*b.w()/GX)*image.getWidth())),Math.min(image.getHeight()-1,(int)((b.y()+(y+.5)*b.h()/GY)*image.getHeight())));
            for(int shift:new int[]{16,8,0})d[k++]=((c>>shift)&255)/255.;
        }
        return d;
    }
    private static PixelTracker.Box contextBox(PixelTracker.Box b){
        double x=Math.max(0,b.x()-.4*b.w()),y=Math.max(0,b.y()-.25*b.h());return new PixelTracker.Box(x,y,Math.min(1,b.x()+1.4*b.w())-x,Math.min(1,b.y()+1.05*b.h())-y);
    }
    private double score(BufferedImage image,PixelTracker.Box b){return .75*score(image,b,anchor)+.25*score(image,contextBox(b),context);}
    private double score(BufferedImage image,PixelTracker.Box b,double[] reference){
        double sa=0,sb=0,aa=0,bb=0,ab=0,error=0;int k=0;
        for(int y=0;y<GY;y++)for(int x=0;x<GX;x++){
            int c=image.getRGB(Math.min(width-1,(int)((b.x()+(x+.5)*b.w()/GX)*width)),Math.min(height-1,(int)((b.y()+(y+.5)*b.h()/GY)*height)));
            for(int shift=16;shift>=0;shift-=8){double a=reference[k++],v=((c>>shift)&255)/255.;sa+=a;sb+=v;aa+=a*a;bb+=v*v;ab+=a*v;error+=Math.abs(a-v);}
        }
        double va=aa-sa*sa/k,vb=bb-sb*sb/k;
        double correlation=va>1e-5&&vb>1e-5?(ab-sa*sb/k)/Math.sqrt(va*vb):0;
        return .7*correlation+.3*Math.max(0,1-3*error/k);
    }
    private double distance(PixelTracker.Box a,PixelTracker.Box b){return Math.hypot((a.x()+a.w()/2-b.x()-b.w()/2)*width,(a.y()+a.h()/2-b.y()-b.h()/2)*height);}
    private Match match(BufferedImage image){
        Match best=null;double range=Math.min(.32,.035+lost*.025);
        for(double factor:new double[]{.55,.7,.85,1,1.15,1.3}){
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
        boolean motionAgrees=false;
        if(lost==0&&found!=null&&!ambiguous&&found.score>=.60){
            var motion=new FlowTracker(id,label,previousAccepted,box).update(image);
            motionAgrees=!motion.isNull("box")&&motion.getJSONObject("motion").getInt("inliers")>=8&&!motion.getJSONObject("motion").getString("method").equals("appearance-recovery")&&PixelTracker.iou(found.box,PixelTracker.Box.from(motion.getJSONArray("box")))>=.6;
        }
        boolean strong=found!=null&&!ambiguous&&(found.score>=(lost>0?.90:.65)||(lost==0&&motionAgrees));
        boolean confirmed=strong&&(lost==0||(pending!=null&&PixelTracker.iou(pending,found.box)>=.55));
        String reason=found==null?"no valid search box":ambiguous?"competing source-appearance match":!strong?(lost>0?"recovery lacks strong original appearance":"frozen appearance mismatch"):!confirmed?"waiting for a second source image":"source appearance and separation accepted";
        String status=confirmed?(lost>0?"reacquired":"tracked"):"lost";
        if(confirmed){previousAccepted=image;box=found.box;lost=0;pending=null;accepted++;}else{lost++;pending=strong?found.box:null;}
        observations++;
        var contact=confirmed?support(image,box):unavailable("object association unconfirmed");
        return new JSONObject().put("id",id).put("label",label).put("status",status).put("box",confirmed?box.json():JSONObject.NULL).put("lastKnownBox",box.json())
            .put("matchScore",found==null?0:found.score).put("appearanceUpdates",0).put("scoreMeaning","Frozen RGB appearance agreement; not semantic identity or calibrated confidence")
            .put("association",new JSONObject().put("method","seed-appearance-133").put("reason",reason).put("competitorScore",rival).put("ambiguous",ambiguous).put("localMotionAgrees",motionAgrees).put("pending",pending!=null).put("observations",observations).put("accepted",accepted).put("lost",lost))
            .put("contact133",contact);
    }
    JSONObject support(BufferedImage image,PixelTracker.Box b){return supportEstimator.support(image,b);}
    private static JSONObject unavailable(String reason){return VisualSupport133.unavailable(reason);}
}
