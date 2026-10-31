package fraymus.workspace;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import org.json.*;

/** Candidate profile: denser RGB/edge templates, independent aspect search and a frozen anchor.
 * No reference annotation after initialization enters this class. Scores are appearance similarity. */
final class AdaptiveTracker implements ImageTracker {
    static final int WORK=256,GRID=12,COLOR=GRID*GRID*3;
    private final String id,label;private final double[] anchor;private double[] appearance;
    private PixelTracker.Box box;private int lost,updates,observations;
    AdaptiveTracker(String id,String label,BufferedImage image,PixelTracker.Box box){this.id=id;this.label=label;this.box=box;anchor=features(new Plane(image),box);appearance=anchor.clone();}
    private static final class Plane {
        final double[][] data=new double[4][WORK*WORK];
        Plane(BufferedImage image){var small=new BufferedImage(WORK,WORK,BufferedImage.TYPE_INT_RGB);var g=small.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.drawImage(image,0,0,WORK,WORK,null);g.dispose();var gray=new double[WORK*WORK];for(int y=0;y<WORK;y++)for(int x=0;x<WORK;x++){int i=y*WORK+x,c=small.getRGB(x,y);data[0][i]=((c>>16)&255)/255.;data[1][i]=((c>>8)&255)/255.;data[2][i]=(c&255)/255.;gray[i]=.299*data[0][i]+.587*data[1][i]+.114*data[2][i];}for(int y=1;y<WORK-1;y++)for(int x=1;x<WORK-1;x++){int i=y*WORK+x;data[3][i]=Math.hypot(gray[i+1]-gray[i-1],gray[i+WORK]-gray[i-WORK]);}}
    }
    static double[] features(Plane p,PixelTracker.Box b){var out=new double[GRID*GRID*4];int cell=0;for(int y=0;y<GRID;y++)for(int x=0;x<GRID;x++){int px=Math.min(WORK-1,Math.max(0,(int)((b.x()+(x+.5)*b.w()/GRID)*WORK))),py=Math.min(WORK-1,Math.max(0,(int)((b.y()+(y+.5)*b.h()/GRID)*WORK)));for(int c=0;c<3;c++)out[cell*3+c]=p.data[c][py*WORK+px];out[COLOR+cell]=p.data[3][py*WORK+px];cell++;}return out;}
    static double corr(double[] a,double[] b,int from,int to){double ma=0,mb=0;for(int i=from;i<to;i++){ma+=a[i];mb+=b[i];}ma/=to-from;mb/=to-from;double va=0,vb=0,dot=0;for(int i=from;i<to;i++){double x=a[i]-ma,y=b[i]-mb;va+=x*x;vb+=y*y;dot+=x*y;}return va<.0001||vb<.0001?0:dot/Math.sqrt(va*vb);}
    static double similarity(double[] a,double[] b){return .85*corr(a,b,0,COLOR)+.15*corr(a,b,COLOR,a.length);}
    record Match(PixelTracker.Box box,double score,double anchorScore){}
    private Match score(Plane p,PixelTracker.Box b){var d=features(p,b);double original=similarity(anchor,d);return new Match(b,.85*similarity(appearance,d)+.15*original,original);}
    private Match match(Plane p,boolean global){var candidates=new ArrayList<Match>();double cx=box.x()+box.w()/2,cy=box.y()+box.h()/2;
        for(double sw:new double[]{.86,1,1.16})for(double sh:new double[]{.86,1,1.16}){double w=box.w()*sw,h=box.h()*sh;if(w>1||h>1||w<.025||h<.025)continue;double x0=global?0:Math.max(0,cx-.25-w/2),x1=global?1-w:Math.min(1-w,cx+.25-w/2),y0=global?0:Math.max(0,cy-.25-h/2),y1=global?1-h:Math.min(1-h,cy+.25-h/2);
            for(double y=y0;y<=y1+1e-8;y+=.02)for(double x=x0;x<=x1+1e-8;x+=.02)candidates.add(score(p,new PixelTracker.Box(x,y,w,h)));}
        if(candidates.isEmpty())return new Match(box,0,0);candidates.sort(Comparator.comparingDouble(Match::score).reversed());var best=candidates.get(0);
        var peaks=new ArrayList<Match>();peaks.add(best);for(var c:candidates)if(PixelTracker.iou(c.box,best.box)<.25){peaks.add(c);break;}var refined=new ArrayList<Match>();
        for(var c:peaks){var local=c;for(double sw:new double[]{.95,1,1.05})for(double sh:new double[]{.95,1,1.05}){double w=c.box.w()*sw,h=c.box.h()*sh;if(w>1||h>1)continue;for(double y=Math.max(0,c.box.y()-.04);y<=Math.min(1-h,c.box.y()+.04)+1e-8;y+=.004)for(double x=Math.max(0,c.box.x()-.04);x<=Math.min(1-w,c.box.x()+.04)+1e-8;x+=.004){var m=score(p,new PixelTracker.Box(x,y,w,h));if(m.score>local.score)local=m;}}refined.add(local);if(local.score>best.score)best=local;}
        double other=0;for(var c:refined)if(PixelTracker.iou(c.box,best.box)<.25)other=Math.max(other,c.score);for(var c:candidates)if(PixelTracker.iou(c.box,best.box)<.25)other=Math.max(other,c.score);return other>best.score-.035?new Match(best.box,0,best.anchorScore):best;
    }
    public JSONObject update(BufferedImage image){var p=new Plane(image);var m=match(p,lost>0||observations==0);observations++;boolean accepted=m.score>=.72&&m.anchorScore>=.55;String status=accepted?(lost>0?"reacquired":"tracked"):"lost";
        if(accepted){box=m.box;if(m.score>=.88&&m.anchorScore>=.7){var d=features(p,box);for(int i=0;i<d.length;i++)appearance[i]=.96*appearance[i]+.04*d[i];updates++;}lost=0;}else lost++;
        return new JSONObject().put("id",id).put("label",label).put("status",status).put("box",accepted?box.json():JSONObject.NULL).put("lastKnownBox",box.json()).put("matchScore",m.score).put("anchorScore",m.anchorScore).put("appearanceUpdates",updates).put("scoreMeaning","RGB/edge appearance similarity, not calibrated semantic confidence");
    }
}
