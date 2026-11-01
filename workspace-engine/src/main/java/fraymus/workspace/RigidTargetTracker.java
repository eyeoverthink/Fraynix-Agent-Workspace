package fraymus.workspace;

import org.json.*;
import java.awt.image.BufferedImage;
import java.util.*;

/** Immutable, human-seeded rigid appearance. Scores describe matching, never identity probability. */
final class RigidTargetTracker {
    static final String VERSION="rigid-anchor-038-v2";
    private static final int COLS=16,ROWS=24;
    private final double[] anchor;
    private final double baseW,baseH;
    private double cx,cy,scale=1,angle;
    private int lost;
    private final double contrast;
    record Candidate(double x,double y,double scale,double angle,double score){}
    RigidTargetTracker(BufferedImage image,PixelTracker.Box box){cx=box.x()+box.w()/2;cy=box.y()+box.h()/2;baseW=box.w();baseH=box.h();anchor=sample(image,cx,cy,baseW,baseH,0);contrast=variance(anchor);if(contrast<.00005)throw new IllegalArgumentException("Seed has too little appearance contrast; include visible target edges/markings");}
    static double variance(double[] a){double m=0,s=0;for(double x:a)m+=x;m/=a.length;for(double x:a)s+=(x-m)*(x-m);return s/a.length;}
    static double[] sample(BufferedImage image,double cx,double cy,double w,double h,double degrees){
        double[] out=new double[COLS*ROWS*3];double c=Math.cos(Math.toRadians(degrees)),s=Math.sin(Math.toRadians(degrees));int at=0;
        for(int row=0;row<ROWS;row++)for(int col=0;col<COLS;col++){
            double u=((col+.5)/COLS-.5)*w,v=((row+.5)/ROWS-.5)*h,x=(cx+c*u-s*v)*image.getWidth(),y=(cy+s*u+c*v)*image.getHeight();
            if(x<0||y<0||x>=image.getWidth()-1||y>=image.getHeight()-1)return null;
            int ix=(int)x,iy=(int)y;double fx=x-ix,fy=y-iy;int a=image.getRGB(ix,iy),b=image.getRGB(ix+1,iy),d=image.getRGB(ix,iy+1),e=image.getRGB(ix+1,iy+1);
            for(int shift=16;shift>=0;shift-=8)out[at++]=((1-fy)*((1-fx)*((a>>shift)&255)+fx*((b>>shift)&255))+fy*((1-fx)*((d>>shift)&255)+fx*((e>>shift)&255)))/255.;
        }return out;
    }
    private double score(BufferedImage image,double x,double y,double sc,double a){var values=sample(image,x,y,baseW*sc,baseH*sc,a);return values==null?-1:PixelTracker.correlation(anchor,values);}
    private static double overlap(Candidate a,Candidate b,double w,double h){return PixelTracker.iou(new PixelTracker.Box(a.x-w*a.scale/2,a.y-h*a.scale/2,w*a.scale,h*a.scale),new PixelTracker.Box(b.x-w*b.scale/2,b.y-h*b.scale/2,w*b.scale,h*b.scale));}
    JSONObject update(BufferedImage original){
        long begin=System.nanoTime();BufferedImage image=original;
        if(Math.max(image.getWidth(),image.getHeight())>640){double s=640./Math.max(image.getWidth(),image.getHeight());var small=new BufferedImage((int)(image.getWidth()*s),(int)(image.getHeight()*s),BufferedImage.TYPE_INT_RGB);var g=small.createGraphics();g.drawImage(image,0,0,small.getWidth(),small.getHeight(),null);g.dispose();image=small;}
        double range=lost>=2?1:.14,step=lost>=2?.0125:.015;var candidates=new ArrayList<Candidate>();
        for(double sc:new double[]{Math.max(.75,scale*.94),scale,Math.min(1.3,scale*1.06)}){
            double x0=lost>=2?baseW*sc/2:Math.max(baseW*sc/2,cx-range),x1=lost>=2?1-baseW*sc/2:Math.min(1-baseW*sc/2,cx+range),y0=lost>=2?baseH*sc/2:Math.max(baseH*sc/2,cy-range),y1=lost>=2?1-baseH*sc/2:Math.min(1-baseH*sc/2,cy+range);
            for(double y=y0;y<=y1+1e-8;y+=step)for(double x=x0;x<=x1+1e-8;x+=step){double q=score(image,x,y,sc,angle);candidates.add(new Candidate(x,y,sc,angle,q));}
        }
        candidates.add(new Candidate(cx,cy,scale,angle,score(image,cx,cy,scale,angle)));candidates.sort(Comparator.comparingDouble(Candidate::score).reversed());var best=candidates.get(0);
        double px=1./image.getWidth(),py=1./image.getHeight();
        // Nearby scales of one distractor must not consume every refinement slot.
        var seeds=new ArrayList<Candidate>();for(var c:candidates){boolean distinct=true;for(var seed:seeds)if(overlap(c,seed,baseW,baseH)>.45){distinct=false;break;}if(distinct)seeds.add(c);if(seeds.size()==8)break;}
        for(var seed:seeds){for(double a:new double[]{angle-6,angle,angle+6})for(double y=seed.y-step;y<=seed.y+step;y+=py*2)for(double x=seed.x-step;x<=seed.x+step;x+=px*2){double q=score(image,x,y,seed.scale,a);if(q>best.score)best=new Candidate(x,y,seed.scale,a,q);}}
        var coarse=best;for(double y=coarse.y-2*py;y<=coarse.y+2*py;y+=py*.5)for(double x=coarse.x-2*px;x<=coarse.x+2*px;x+=px*.5){double q=score(image,x,y,coarse.scale,coarse.angle);if(q>best.score)best=new Candidate(x,y,coarse.scale,coarse.angle,q);}
        double other=-1;Candidate distractor=null;for(var c:candidates)if(overlap(c,best,baseW,baseH)<.2&&(distractor==null||c.score>distractor.score))distractor=c;
        if(distractor!=null){other=distractor.score;for(double y=distractor.y-step;y<=distractor.y+step;y+=py)for(double x=distractor.x-step;x<=distractor.x+step;x+=px)other=Math.max(other,score(image,x,y,distractor.scale,distractor.angle));}double margin=best.score-other;boolean accepted=best.score>=.72&&margin>=.035;
        String status=accepted?(lost>0?"reacquired":"observed"):(best.score>=.72?"ambiguous":"lost");if(accepted){cx=best.x;cy=best.y;scale=best.scale;angle=best.angle;lost=0;}else lost++;
        double w=baseW*scale,h=baseH*scale;var out=new JSONObject().put("status",status).put("accepted",accepted).put("score",best.score).put("margin",margin).put("angleDeg",angle).put("lostFrames",lost).put("anchorContrast",contrast).put("processingMs",(System.nanoTime()-begin)/1e6).put("scoreMeaning","Original human-seeded appearance correlation and ambiguity margin; not semantic identity or calibrated probability");
        if(accepted)out.put("box",new JSONArray(new double[]{cx-w/2,cy-h/2,cx+w/2,cy+h/2})).put("point",new JSONArray(new double[]{cx,cy}));else out.put("box",JSONObject.NULL);
        return out;
    }
}
