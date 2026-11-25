package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.Color;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.TreeMap;
import java.util.function.IntFunction;

/** Match silhouettes in plan view, then check the frozen positions in a separate registered view. */
final class ImageSceneRegistration {
    static final String POLICY = "registered-silhouettes-v1";
    static final String YAW_POLICY = "registered-silhouettes-yaw-v2";
    private static final double DIAGONAL = Math.sqrt(.5);
    private static final double WITNESS_UP = .5;
    private static final double WITNESS_AWAY = Math.sqrt(.75);
    static final double MIN_OVERHEAD_IOU = .82;
    static final double MIN_WITNESS_IOU = .83;
    static final double MIN_ASSIGNMENT_GAP = .06;
    static final double MAX_WITNESS_CENTER_PX = 2.5;
    private static final int RASTER_SAMPLES = 3;
    private static final int FOREGROUND_CONTRAST = 64;
    private static final int COARSE_YAW_STEP = 12;
    private static final int FINE_YAW_STEP = 2;
    private static final int ALTERNATE_YAW_DISTANCE = 24;
    private static final double MIN_YAW_GAP = .035;
    private static final int MAX_WITNESS_YAW_ERROR = 6;

    record Prop(String label, SpatialVisualMesh mesh, double heightM) {}
    record Result(List<double[]> centersPx, int[] yawDeg, JSONObject evidence) {}
    record Match(int dx, int dy, double iou, double centerErrorPx) {}
    private record Heading(int yaw, Match match, double gap) {}

    static final class Silhouette {
        final BitSet mask = new BitSet();
        final int width;
        int count, minX, maxX=-1, minY, maxY=-1;
        long sumX, sumY;

        Silhouette(int width, int height) {
            this.width = width;
            minX=width;
            minY=height;
        }

        void add(int pixel) {
            mask.set(pixel);
            int x=pixel%width, y=pixel/width;
            minX=Math.min(minX,x); maxX=Math.max(maxX,x);
            minY=Math.min(minY,y); maxY=Math.max(maxY,y);
            sumX+=x; sumY+=y; count++;
        }

        double centerX() { return sumX/(double)count+.5; }
        double centerY() { return sumY/(double)count+.5; }
    }

    static final class Assignment {
        int[] best;
        double score=Double.NEGATIVE_INFINITY, runner=Double.NEGATIVE_INFINITY;

        void search(Match[][] scores, int depth, int taken, int[] path, double total) {
            if (depth==scores.length) {
                if (total>score) {
                    runner=score;
                    score=total;
                    best=path.clone();
                } else runner=Math.max(runner,total);
                return;
            }
            for (int col=0;col<scores.length;col++) if ((taken & (1<<col))==0) {
                path[depth]=col;
                search(scores,depth+1,taken|(1<<col),path,total+scores[depth][col].iou);
            }
        }
    }

    static Result locate(BufferedImage overhead, byte[] witnessBytes, double cellM, List<Prop> props,
                         boolean recoverYaw) throws Exception {
        int w=overhead.getWidth(), h=overhead.getHeight(), n=props.size();
        if (cellM>.025) throw new IllegalArgumentException("Use at least 40 pixels per metre in the registered views.");
        var observed=components(overhead,n,"Overhead");
        var scores=new Match[n][n];
        var headings=new Heading[n][];
        for (int i=0;i<n;i++) {
            var prop=props.get(i);
            if (recoverYaw) {
                headings[i]=searchHeadings(n, yaw->fits(render(w,h,cellM,prop,0,0,false,yaw,true),observed,w,h));
                for (int j=0;j<n;j++) scores[i][j]=headings[i][j].match;
            } else scores[i]=fits(render(w,h,cellM,prop,0,0,false,0,false),observed,w,h);
        }
        var assignment=assign(scores);
        if (assignment.score-assignment.runner<MIN_ASSIGNMENT_GAP)
            throw new IllegalArgumentException("Overhead silhouettes do not uniquely identify the selected props.");
        var positions=new ArrayList<double[]>();
        int[] yawDeg=new int[n];
        for (int i=0;i<n;i++) {
            var match=scores[i][assignment.best[i]];
            if (match.iou<MIN_OVERHEAD_IOU)
                throw new IllegalArgumentException(String.format(java.util.Locale.ROOT,
                        "Overhead refused %s at this scale (%.1f%% silhouette overlap).",props.get(i).label,match.iou*100));
            positions.add(new double[]{w/2.0+match.dx,h/2.0+match.dy});
            if (recoverYaw) {
                var heading=headings[i][assignment.best[i]];
                if (heading.gap<MIN_YAW_GAP)
                    throw new IllegalArgumentException("Overhead orientation is ambiguous for "+props.get(i).label+".");
                yawDeg[i]=heading.yaw;
            }
        }

        // The second image is first decoded here. It cannot change the fitted identities, positions or angles.
        var witness=BrowserShapeTrace.decode(witnessBytes);
        if (witness.getWidth()!=w || witness.getHeight()!=h)
            throw new IllegalArgumentException("Both registered views need the same image dimensions and scale.");
        var witnessParts=components(witness,n,"Second view");
        boolean sameSilhouettes=true;
        for (int i=0;i<n;i++) sameSilhouettes &= observed.get(i).mask.equals(witnessParts.get(i).mask);
        if (sameSilhouettes) throw new IllegalArgumentException("Choose a separate registered second view.");
        var projected=new Match[n][n];
        for (int i=0;i<n;i++) {
            var center=positions.get(i);
            var predicted=render(w,h,cellM,props.get(i),(center[0]-w/2.0)*cellM,(center[1]-h/2.0)*cellM,
                    true,yawDeg[i],false);
            for (int j=0;j<n;j++) projected[i][j]=compare(predicted,witnessParts.get(j),w,h,0,0);
        }
        var pairing=assign(projected);
        var perProp=new JSONArray();
        for (int i=0;i<n;i++) {
            var overheadMatch=scores[i][assignment.best[i]];
            var witnessMatch=projected[i][pairing.best[i]];
            if (witnessMatch.iou<MIN_WITNESS_IOU || witnessMatch.centerErrorPx>MAX_WITNESS_CENTER_PX)
                throw new IllegalArgumentException(String.format(java.util.Locale.ROOT,
                        "Second view refused %s at its overhead position (%.1f%% overlap, %.2f px center error).",
                        props.get(i).label,witnessMatch.iou*100,witnessMatch.centerErrorPx));
            var row=new JSONObject().put("sourceId","image-prop-"+(i+1))
                    .put("label",props.get(i).label)
                    .put("overheadIou",overheadMatch.iou).put("witnessIou",witnessMatch.iou)
                    .put("witnessCenterErrorPx",witnessMatch.centerErrorPx);
            if (recoverYaw) {
                var prop=props.get(i);
                var measured=witnessParts.get(pairing.best[i]);
                var center=positions.get(i);
                double x=(center[0]-w/2.0)*cellM, z=(center[1]-h/2.0)*cellM;
                var witnessHeading=searchHeadings(1, yaw->{
                    var view=render(w,h,cellM,prop,x,z,true,yaw,true);
                    return new Match[]{view==null?null:compare(view,measured,w,h,0,0)};
                })[0];
                int disagreement=angleDistance(yawDeg[i],witnessHeading.yaw);
                if (witnessHeading.gap<MIN_YAW_GAP)
                    throw new IllegalArgumentException("Second-view orientation is ambiguous for "+prop.label+".");
                if (disagreement>MAX_WITNESS_YAW_ERROR)
                    throw new IllegalArgumentException("Second view refused "+prop.label+" orientation at its fixed position ("
                            +disagreement+"° difference).");
                row.put("overheadYawDeg",yawDeg[i]).put("witnessYawDeg",witnessHeading.yaw)
                        .put("witnessYawErrorDeg",disagreement).put("witnessYawGap",witnessHeading.gap)
                        .put("overheadYawGap",headings[i][assignment.best[i]].gap);
            }
            perProp.put(row);
        }
        var evidence=new JSONObject().put("policy",recoverYaw?YAW_POLICY:POLICY).put("witnessSha256",MapDefinition.sha(witnessBytes))
                .put("witnessElevationDeg",60).put("witnessAzimuthFromFrontDeg",45)
                .put("assignmentGap",assignment.score-assignment.runner).put("minimumAssignmentGap",MIN_ASSIGNMENT_GAP)
                .put("minimumOverheadIou",MIN_OVERHEAD_IOU).put("minimumWitnessIou",MIN_WITNESS_IOU)
                .put("maximumWitnessCenterErrorPx",MAX_WITNESS_CENTER_PX).put("props",perProp);
        if (recoverYaw) evidence.put("yawResolutionDeg",FINE_YAW_STEP)
                .put("minimumYawGap",MIN_YAW_GAP).put("alternateYawDistanceDeg",ALTERNATE_YAW_DISTANCE)
                .put("maximumWitnessYawErrorDeg",MAX_WITNESS_YAW_ERROR);
        return new Result(positions,yawDeg,evidence);
    }

    private static Match[] fits(Silhouette template, List<Silhouette> observed, int w, int h) {
        var scores=new Match[observed.size()];
        if (template!=null)
            for (int j=0;j<observed.size();j++) scores[j]=fit(template,observed.get(j),w,h);
        return scores;
    }

    private static Heading[] searchHeadings(int count, IntFunction<Match[]> evaluate) {
        var samples=new TreeMap<Integer,Match[]>();
        int first=-180;
        for (int yaw=first;yaw<180;yaw+=COARSE_YAW_STEP) samples.put(yaw,evaluate.apply(yaw));
        int steps=360/COARSE_YAW_STEP;
        for (int component=0;component<count;component++) {
            double[] coarse=new double[steps];
            double best=-1;
            for (int i=0;i<steps;i++) {
                var match=samples.get(first+i*COARSE_YAW_STEP)[component];
                coarse[i]=match==null?-1:match.iou;
                best=Math.max(best,coarse[i]);
            }
            for (int i=0;i<steps;i++) {
                if (coarse[i]<0 || coarse[i]<best-.14 ||
                        coarse[i]<coarse[(i+steps-1)%steps] || coarse[i]<coarse[(i+1)%steps]) continue;
                for (int delta=-COARSE_YAW_STEP+FINE_YAW_STEP;delta<COARSE_YAW_STEP;delta+=FINE_YAW_STEP)
                    samples.computeIfAbsent(wrap(first+i*COARSE_YAW_STEP+delta),evaluate::apply);
            }
        }
        var result=new Heading[count];
        for (int component=0;component<count;component++) {
            Match best=null;
            int yaw=0;
            for (var row:samples.entrySet()) {
                var match=row.getValue()[component];
                if (match!=null && (best==null || match.iou>best.iou ||
                        match.iou==best.iou && match.centerErrorPx<best.centerErrorPx)) {
                    best=match;
                    yaw=row.getKey();
                }
            }
            if (best==null) throw new IllegalArgumentException("A rotated prop could not be projected inside the registered frame.");
            double alternate=0;
            for (var row:samples.entrySet()) {
                var match=row.getValue()[component];
                if (match!=null && angleDistance(row.getKey(),yaw)>=ALTERNATE_YAW_DISTANCE)
                    alternate=Math.max(alternate,match.iou);
            }
            result[component]=new Heading(yaw,best,best.iou-alternate);
        }
        return result;
    }

    private static int wrap(int degrees) { return Math.floorMod(degrees+180,360)-180; }
    private static int angleDistance(int a,int b) { return Math.abs(wrap(a-b)); }

    static Assignment assign(Match[][] scores) {
        var assignment=new Assignment();
        assignment.search(scores,0,0,new int[scores.length],0);
        return assignment;
    }

    private static Match fit(Silhouette a, Silhouette b, int w, int h) {
        return fit(a,b,w,h,2);
    }

    static Match fit(Silhouette a, Silhouette b, int w, int h, int radius) {
        int x=(int)Math.round((b.minX+b.maxX-a.minX-a.maxX)/2.0);
        int y=(int)Math.round((b.minY+b.maxY-a.minY-a.maxY)/2.0);
        Match best=null;
        for (int dy=y-radius;dy<=y+radius;dy++) for (int dx=x-radius;dx<=x+radius;dx++) {
            var match=compare(a,b,w,h,dx,dy);
            if (best==null || match.iou>best.iou || match.iou==best.iou && match.centerErrorPx<best.centerErrorPx)
                best=match;
        }
        return best;
    }

    static Match compare(Silhouette a, Silhouette b, int w, int h, int dx, int dy) {
        int common=0;
        if (a.minX+dx<=b.maxX && a.maxX+dx>=b.minX && a.minY+dy<=b.maxY && a.maxY+dy>=b.minY) {
            for (int bit=a.mask.nextSetBit(0);bit>=0;bit=a.mask.nextSetBit(bit+1)) {
                int x=bit%w+dx, y=bit/w+dy;
                if (x>=0 && x<w && y>=0 && y<h && b.mask.get(y*w+x)) common++;
            }
        }
        return new Match(dx,dy,common/(double)(a.count+b.count-common),
                Math.hypot(a.centerX()+dx-b.centerX(),a.centerY()+dy-b.centerY()));
    }

    private static Silhouette render(int w, int h, double cellM, Prop prop, double x, double z, boolean witness,
                                     int yaw, boolean crop) {
        var mesh=prop.mesh;
        double angle=Math.toRadians(yaw), sin=Math.sin(angle), cos=Math.cos(angle);
        double[][] projected=new double[mesh.vertexCount()][2];
        for (int i=0;i<projected.length;i++) {
            double lx=mesh.positions()[3*i], lz=mesh.positions()[3*i+2];
            double vx=cos*lx+sin*lz+x, vy=mesh.positions()[3*i+1]+prop.heightM/2, vz=-sin*lx+cos*lz+z;
            double u=witness?(vx+vz)*DIAGONAL:vx;
            double v=witness?(vz-vx)*DIAGONAL*WITNESS_AWAY-vy*WITNESS_UP:vz;
            double col=w/2.0+u/cellM, row=h/2.0+v/cellM;
            projected[i][0]=col;
            projected[i][1]=row;
        }
        return raster(w,h,mesh,projected,crop);
    }

    static Silhouette raster(int w, int h, SpatialVisualMesh mesh, double[][] projected, boolean crop) {
        int left=w, right=0, top=h, bottom=0;
        for (var pixel:projected) {
            double col=pixel[0], row=pixel[1];
            if (col<1 || col>=w-1 || row<1 || row>=h-1) {
                if (crop) return null;
                throw new IllegalArgumentException("Keep every projected model fully inside the registered image frame.");
            }
            left=Math.min(left,(int)Math.floor(col));
            top=Math.min(top,(int)Math.floor(row));
            right=Math.max(right,(int)Math.ceil(col));
            bottom=Math.max(bottom,(int)Math.ceil(row));
        }
        if (!crop) {
            left=0; top=0; right=w; bottom=h;
        }
        var pixels=new BufferedImage((right-left)*RASTER_SAMPLES,(bottom-top)*RASTER_SAMPLES,BufferedImage.TYPE_BYTE_BINARY);
        var graphics=pixels.createGraphics();
        try {
            graphics.scale(RASTER_SAMPLES,RASTER_SAMPLES);
            graphics.translate(-left,-top);
            graphics.setColor(Color.WHITE);
            int[] indices=mesh.indices();
            for (int i=0;i<indices.length;i+=3) {
                var a=projected[indices[i]];
                var b=projected[indices[i+1]];
                var c=projected[indices[i+2]];
                var triangle=new Path2D.Double();
                triangle.moveTo(a[0],a[1]);
                triangle.lineTo(b[0],b[1]);
                triangle.lineTo(c[0],c[1]);
                triangle.closePath();
                graphics.fill(triangle);
            }
        } finally { graphics.dispose(); }
        var result=new Silhouette(w,h);
        var raster=pixels.getRaster();
        for (int row=top;row<bottom;row++) for (int col=left;col<right;col++) {
            int sampleX=(col-left)*RASTER_SAMPLES+RASTER_SAMPLES/2, sampleY=(row-top)*RASTER_SAMPLES+RASTER_SAMPLES/2;
            if (raster.getSample(sampleX,sampleY,0)!=0) result.add(row*w+col);
        }
        if (result.count<16) {
            if (crop) return null;
            throw new IllegalArgumentException("A projected mesh is too small to match.");
        }
        return result;
    }

    static List<Silhouette> components(BufferedImage img, int limit, String label) {
        int w=img.getWidth(), h=img.getHeight();
        var pixels=img.getRGB(0,0,w,h,null,0,w);
        int bg=pixels[0];
        for (int corner : new int[]{w-1,w*(h-1),w*h-1}) {
            if (contrast(pixels[corner],bg)>10) throw new IllegalArgumentException(label+" needs a plain, contrasting background.");
        }
        var foreground=new BitSet(w*h);
        for (int i=0;i<pixels.length;i++) {
            if (pixels[i]>>>24!=255) throw new IllegalArgumentException(label+" must be opaque.");
            if (contrast(pixels[i],bg)>=FOREGROUND_CONTRAST) foreground.set(i);
        }
        int[] queue=new int[pixels.length];
        var parts=new ArrayList<Silhouette>();
        for (int first=foreground.nextSetBit(0);first>=0;first=foreground.nextSetBit(0)) {
            int head=0, tail=enqueue(foreground,queue,0,first);
            var part=new Silhouette(w,h);
            while (head<tail) {
                int pixel=queue[head++], x=pixel%w, y=pixel/w;
                part.add(pixel);
                if (x==0 || x==w-1 || y==0 || y==h-1)
                    throw new IllegalArgumentException(label+" clips a silhouette at the frame edge.");
                tail=enqueue(foreground,queue,tail,pixel-1);
                tail=enqueue(foreground,queue,tail,pixel+1);
                tail=enqueue(foreground,queue,tail,pixel-w);
                tail=enqueue(foreground,queue,tail,pixel+w);
            }
            if (part.count<16) throw new IllegalArgumentException(label+" contains detached pixels or props too small to measure.");
            parts.add(part);
            if (parts.size()>limit) throw new IllegalArgumentException(label+" needs one separated silhouette per selected prop.");
        }
        if (parts.size()!=limit) throw new IllegalArgumentException(label+" needs one separated silhouette per selected prop.");
        return parts;
    }

    private static int enqueue(BitSet mask, int[] queue, int tail, int at) {
        if (!mask.get(at)) return tail;
        mask.clear(at);
        queue[tail]=at;
        return tail+1;
    }

    private static int contrast(int a, int b) {
        return Math.max(Math.abs((a & 255)-(b & 255)),Math.max(
                Math.abs(((a>>>8) & 255)-((b>>>8) & 255)),Math.abs(((a>>>16) & 255)-((b>>>16) & 255))));
    }
}
