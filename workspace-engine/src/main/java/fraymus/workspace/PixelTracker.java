package fraymus.workspace;

import org.json.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

/** Small CPU appearance learner/tracker. Correlation is a match score, never class probability. */
final class PixelTracker implements ImageTracker {
    static final int GRID=8,WORK=192;
    record Box(double x,double y,double w,double h){
        JSONArray json(){return new JSONArray(new double[]{x,y,x+w,y+h});}
        static Box from(JSONArray a){if(a.length()!=4)throw new IllegalArgumentException("Box needs [left,top,right,bottom]");double[] v=new double[4];for(int i=0;i<4;i++){if(!(a.get(i) instanceof Number n))throw new IllegalArgumentException("Numeric box required");v[i]=n.doubleValue();RampDefinition.range(v[i],0,1,"normalized box");}if(v[2]-v[0]<.025||v[3]-v[1]<.025)throw new IllegalArgumentException("Box is too small or reversed");return new Box(v[0],v[1],v[2]-v[0],v[3]-v[1]);}
    }
    private final String id,label;private final double[] anchor;private double[] appearance;private Box box;private int updates,lost;private final boolean adapt;
    PixelTracker(String id,String label,BufferedImage image,Box box,boolean adapt){this(id,label,descriptor(working(image),box),box,adapt);}
    PixelTracker(String id,String label,double[] learned,Box box,boolean adapt){this.id=id;this.label=label;anchor=learned.clone();appearance=learned.clone();this.box=box;this.adapt=adapt;}
    static BufferedImage working(BufferedImage original){var out=new BufferedImage(WORK,WORK,BufferedImage.TYPE_INT_RGB);var g=out.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.drawImage(original,0,0,WORK,WORK,null);g.dispose();return out;}
    static double[] descriptor(BufferedImage image,Box b){var v=new double[GRID*GRID*3];int at=0;for(int y=0;y<GRID;y++)for(int x=0;x<GRID;x++){int px=(int)((b.x+(x+.5)*b.w/GRID)*image.getWidth()),py=(int)((b.y+(y+.5)*b.h/GRID)*image.getHeight());int rgb=image.getRGB(Math.max(0,Math.min(image.getWidth()-1,px)),Math.max(0,Math.min(image.getHeight()-1,py)));v[at++]=((rgb>>16)&255)/255.;v[at++]=((rgb>>8)&255)/255.;v[at++]=(rgb&255)/255.;}return v;}
    static double correlation(double[] a,double[] b){double ma=0,mb=0;for(int i=0;i<a.length;i++){ma+=a[i];mb+=b[i];}ma/=a.length;mb/=b.length;double cross=0,va=0,vb=0;for(int i=0;i<a.length;i++){double x=a[i]-ma,y=b[i]-mb;cross+=x*y;va+=x*x;vb+=y*y;}return va<.001||vb<.001?0:cross/Math.sqrt(va*vb);}
    static double iou(Box a,Box b){double iw=Math.max(0,Math.min(a.x+a.w,b.x+b.w)-Math.max(a.x,b.x)),ih=Math.max(0,Math.min(a.y+a.h,b.y+b.h)-Math.max(a.y,b.y));double i=iw*ih;return i/(a.w*a.h+b.w*b.h-i);}
    record Match(Box box,double score,double anchorScore){}
    Match match(BufferedImage image,boolean global){
        var candidates=new ArrayList<Match>();double range=global?1:.24;double cx=box.x+box.w/2,cy=box.y+box.h/2;
        for(double factor:new double[]{.8,1,1.2}){double w=box.w*factor,h=box.h*factor;if(w>1||h>1||w<.025||h<.025)continue;
            double x0=Math.max(0,global?0:cx-range-w/2),x1=Math.min(1-w,global?1:cx+range-w/2),y0=Math.max(0,global?0:cy-range-h/2),y1=Math.min(1-h,global?1:cy+range-h/2);
            for(double y=y0;y<=y1+1e-8;y+=.025)for(double x=x0;x<=x1+1e-8;x+=.025){var b=new Box(x,y,w,h);var d=descriptor(image,b);candidates.add(new Match(b,correlation(appearance,d),correlation(anchor,d)));}}
        if(candidates.isEmpty())return new Match(box,0,0);candidates.sort(Comparator.comparingDouble(Match::score).reversed());var coarse=candidates.get(0);var best=coarse;
        for(double y=Math.max(0,coarse.box.y-.025);y<=Math.min(1-coarse.box.h,coarse.box.y+.025)+1e-8;y+=.004)for(double x=Math.max(0,coarse.box.x-.025);x<=Math.min(1-coarse.box.w,coarse.box.x+.025)+1e-8;x+=.004){var b=new Box(x,y,coarse.box.w,coarse.box.h);var d=descriptor(image,b);var m=new Match(b,correlation(appearance,d),correlation(anchor,d));if(m.score>best.score)best=m;}
        double other=0;for(var m:candidates)if(iou(m.box,best.box)<.25)other=Math.max(other,m.score);if(other>best.score-.035)return new Match(best.box,0,best.anchorScore);return best;
    }
    public JSONObject update(BufferedImage image){var small=working(image);var m=match(small,lost>0||updates==0);boolean accepted=m.score>=.78&&m.anchorScore>=.65;String status=accepted?(lost>0?"reacquired":"tracked"):"lost";
        if(accepted){box=m.box;if(adapt&&m.score>=.9&&m.anchorScore>=.8){var d=descriptor(small,box);for(int i=0;i<d.length;i++)appearance[i]=.97*appearance[i]+.03*d[i];updates++;}lost=0;}else lost++;
        return new JSONObject().put("id",id).put("label",label).put("status",status).put("box",accepted?m.box.json():JSONObject.NULL).put("lastKnownBox",box.json()).put("matchScore",m.score).put("anchorScore",m.anchorScore).put("appearanceUpdates",updates).put("scoreMeaning","normalized appearance correlation, not calibrated semantic confidence");
    }
    JSONObject lesson(){return new JSONObject().put("descriptor",new JSONArray(anchor)).put("box",box.json()).put("method","8x8 RGB appearance; normalized correlation; no named-person identification");}
    static double[] learned(JSONObject lesson){var a=lesson.getJSONArray("descriptor");if(a.length()!=GRID*GRID*3)throw new IllegalArgumentException("Invalid learned appearance length");double[] d=new double[a.length()];for(int i=0;i<d.length;i++){d[i]=a.getDouble(i);RampDefinition.range(d[i],0,1,"appearance");}return d;}
}
