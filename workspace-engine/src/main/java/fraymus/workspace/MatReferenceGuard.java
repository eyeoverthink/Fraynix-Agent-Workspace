package fraymus.workspace;
import org.json.*;
import java.awt.image.BufferedImage;
import java.util.*;

/** Reference-patch consistency. Cannot distinguish camera movement from mat movement/occlusion. */
final class MatReferenceGuard {
    private final JSONArray points;
    private final double[][] patches;
    private final int width,height;
    MatReferenceGuard(BufferedImage first,JSONObject definition){
        if(!definition.getString("mode").equals("homography"))throw new IllegalArgumentException("Tabletop monitoring requires four measured mat corners");points=definition.getJSONArray("imagePoints");width=first.getWidth();height=first.getHeight();patches=new double[4][];
        for(int i=0;i<4;i++){var p=points.getJSONArray(i);patches[i]=patch(first,p.getDouble(0)*width,p.getDouble(1)*height);}
    }
    private double[] patch(BufferedImage im,double x,double y){if(x<7||y<7||x>=width-7||y>=height-7)return null;var out=new double[13*13];int n=0;for(int dy=-6;dy<=6;dy++)for(int dx=-6;dx<=6;dx++){int v=im.getRGB((int)Math.round(x)+dx,(int)Math.round(y)+dy);out[n++]=(.299*((v>>16)&255)+.587*((v>>8)&255)+.114*(v&255))/255.;}return out;}
    JSONObject check(BufferedImage im){if(im.getWidth()!=width||im.getHeight()!=height)throw new IllegalArgumentException("Camera dimensions changed; recalibrate");var scores=new JSONArray();var shifts=new ArrayList<Double>();int usable=0,matched=0;
        for(int i=0;i<4;i++){var original=patches[i];if(original==null||RigidTargetTracker.variance(original)<.00005){scores.put(new JSONObject().put("corner",i).put("status","unusable"));continue;}usable++;var p=points.getJSONArray(i);double px=p.getDouble(0)*width,py=p.getDouble(1)*height,best=-1;int bx=0,by=0;for(int dy=-8;dy<=8;dy++)for(int dx=-8;dx<=8;dx++){var values=patch(im,px+dx,py+dy);if(values==null)continue;double q=PixelTracker.correlation(original,values);if(q>best){best=q;bx=dx;by=dy;}}double displacement=Math.hypot(bx,by);if(best>=.7){matched++;shifts.add(displacement);}scores.put(new JSONObject().put("corner",i).put("score",best).put("dxPixels",bx).put("dyPixels",by));}
        Collections.sort(shifts);double median=shifts.isEmpty()?0:shifts.get(shifts.size()/2);boolean valid=usable>=3&&matched>=3&&median<=3;return new JSONObject().put("valid",valid).put("usableAnchors",usable).put("matchedAnchors",matched).put("medianShiftPixels",median).put("corners",scores).put("status",valid?"reference-stable":usable<3?"reference-unverifiable":"reference-changed").put("meaning","Mat/camera reference-patch consistency, not a physical motion sensor. Movement, occlusion or lighting failure require re-observation.");
    }
}
