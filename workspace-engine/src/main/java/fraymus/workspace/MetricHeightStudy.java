package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Conditional, metric reconstruction of a registered vertical segment from calibrated orthographic pixels. */
final class MetricHeightStudy {
    static final String POLICY="registered-height-v1";
    static final int LIMIT=1_200_000;
    private static final double WITNESS_LIMIT_PX=2;
    private static final double LOCALIZATION_PX=.8;
    private static final double TARGET_RISE_M=.035;
    private static final int[] COLORS={0xff2c2c,0x2cff2c,0x2c2cff,0xffff2c,0x2cffff,0xff8e2c,0xff2cff};

    private record Mark(double x,double y,int width,int height) {
        Mark(double x,double y) { this(x,y,0,0); }
        Mark minus(Mark other) { return new Mark(x-other.x,y-other.y); }
        double norm() { return Math.hypot(x,y); }
        double dot(Mark other) { return x*other.x+y*other.y; }
        JSONArray json() { return new JSONArray(new double[]{x,y}); }
    }

    private record View(String id,String role,String calibration,String observation) {}
    private record Fit(View view,int width,int height,double[][] inverse,Mark[] marks,double checkErrorPx) {
        double[] weights(double[] p) {
            double[] weights=new double[4];
            for (int i=0;i<4;i++)
                for (int k=0;k<4;k++) weights[i]+=(k==3?1:p[k])*inverse[k][i];
            return weights;
        }
        Mark project(double[] p) {
            var weights=weights(p);
            double x=0,y=0;
            for (int i=0;i<4;i++) { x+=weights[i]*marks[i].x; y+=weights[i]*marks[i].y; }
            return new Mark(x,y);
        }
        double budget(double[] p) {
            double amplification=0;
            for (double w:weights(p)) amplification+=Math.abs(w);
            return LOCALIZATION_PX*(1+amplification)+checkErrorPx;
        }
        double scale() {
            var zero=project(new double[3]);
            double total=0;
            for (int i=0;i<3;i++) {
                double[] axis=new double[3];
                axis[i]=1;
                var direction=project(axis).minus(zero);
                total+=direction.dot(direction);
            }
            return Math.sqrt(total/2);
        }
        double[] axis() {
            var zero=project(new double[3]);
            double[] u=new double[3],v=new double[3];
            for (int i=0;i<3;i++) {
                double[] axis=new double[3];
                axis[i]=1;
                Mark derivative=project(axis).minus(zero);
                u[i]=derivative.x;
                v[i]=derivative.y;
            }
            double a=ObserverGeometry.norm(u),b=ObserverGeometry.norm(v);
            if (Math.min(a,b)<10 || Math.max(a,b)/Math.min(a,b)>1.02
                    || Math.abs(ObserverGeometry.dot(u,v))/(a*b)>.02)
                throw new IllegalArgumentException("The calibration is inconsistent with an orthographic camera.");
            return ObserverGeometry.unit(new double[]{u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0]});
        }
        boolean inFrame(Mark p) {
            return p.x>=12 && p.y>=12 && p.x<width-12 && p.y<height-12;
        }
    }

    private final String title,note;
    private final double[] anchor,footprint,hypotheses;
    private final double[][] landmarks,inverse;
    private final View initial,witness;
    private final Map<String,View> candidates;

    MetricHeightStudy(byte[] source) {
        if (source.length==0 || source.length>LIMIT) throw new IllegalArgumentException("Choose a height study up to 1.2 MB.");
        var input=new JSONObject(new String(source,StandardCharsets.UTF_8));
        SpatialScene.keys(input,"format","title","sourceNote","baseAnchorM","footprintM","hypothesesM","landmarksM","views");
        if (!input.getString("format").equals("fraynix.metric-height-study-v1"))
            throw new IllegalArgumentException("Choose a registered metric height study.");
        title=SpatialScene.text(input,"title",80);
        note=SpatialScene.text(input,"sourceNote",360);
        anchor=SpatialScene.vector(input.getJSONArray("baseAnchorM"),1.2);
        if (anchor[1]!=0) throw new IllegalArgumentException("The registered base must lie on the Y=0 deck.");
        footprint=pair(input.getJSONArray("footprintM"),.05,.9);
        hypotheses=pair(input.getJSONArray("hypothesesM"),.08,1.0);
        if (hypotheses[1]-hypotheses[0]<.05) throw new IllegalArgumentException("Choose ordered hypotheses at least 50 mm apart.");
        var points=input.getJSONArray("landmarksM");
        if (points.length()!=6) throw new IllegalArgumentException("Register four fitting landmarks and two independent checks.");
        landmarks=new double[6][];
        for (int i=0;i<6;i++) landmarks[i]=SpatialScene.vector(points.getJSONArray(i),2);
        inverse=inverse(landmarks);
        View first=null,last=null;
        candidates=new LinkedHashMap<>();
        var views=input.getJSONArray("views");
        if (views.length()<4 || views.length()>7) throw new IllegalArgumentException("Use two to five next-view candidates and one withheld view.");
        var ids=new java.util.HashSet<String>();
        for (Object item:views) {
            var v=(JSONObject)item;
            SpatialScene.keys(v,"id","role","calibrationPng","observationPng");
            String id=v.getString("id");
            if (!id.matches("[a-z][a-z0-9-]{0,30}") || !ids.add(id)) throw new IllegalArgumentException("Use unique short camera view identifiers.");
            var view=new View(id,v.getString("role"),v.getString("calibrationPng"),v.getString("observationPng"));
            pngBytes(view.calibration);
            pngBytes(view.observation);
            switch (view.role) {
                case "initial" -> {
                    if (first!=null) throw new IllegalArgumentException("Use one initial view.");
                    first=view;
                }
                case "witness" -> {
                    if (last!=null) throw new IllegalArgumentException("Use one withheld view.");
                    last=view;
                }
                case "candidate" -> candidates.put(id,view);
                default -> throw new IllegalArgumentException("Use initial, candidate or witness view roles.");
            }
        }
        if (first==null || last==null || candidates.size()<2) throw new IllegalArgumentException("The study is missing a view role.");
        initial=first;
        witness=last;
    }

    JSONObject plan() throws Exception {
        Fit first=fit(initial);
        Mark start=observe(first);
        for (double hypothesis:hypotheses) {
            var point=target(hypothesis);
            if (first.project(point).minus(start).norm()>first.budget(point))
                throw new IllegalArgumentException("The initial marker doesn't support both registered height hypotheses.");
        }
        List<JSONObject> scores=new ArrayList<>();
        String selected="";
        double best=0;
        for (View candidate:candidates.values()) {
            var score=score(fit(candidate));
            scores.add(score);
            double lower=score.getDouble("separationLowerPx");
            if (lower>best) {best=lower;selected=candidate.id;}
        }
        return new JSONObject().put("policy",POLICY).put("title",title).put("sourceNote",note)
                .put("initialView",initial.id).put("initialMarkerPx",start.json())
                .put("initial",score(first)).put("heightMeasured",false)
                .put("hypothesesM",new JSONArray(hypotheses)).put("candidateScores",scores)
                .put("selectedView",selected).put("canObserve",!selected.isEmpty());
    }

    JSONObject verify(JSONObject plan) throws Exception {
        if (!POLICY.equals(plan.getString("policy")) || !plan.getBoolean("canObserve"))
            throw new IllegalStateException("No calibrated next view can distinguish the hypotheses.");
        View selected=candidates.get(plan.getString("selectedView"));
        if (selected==null) throw new IllegalStateException("The selected view is unavailable.");
        if (selected.observation.equals(initial.observation) || witness.observation.equals(selected.observation)
                || witness.calibration.equals(selected.calibration))
            return refusal("The selected and withheld observations must be distinct.");
        try {
            Fit active=fit(selected);
            Mark seen=observe(active);
            Mark p0=active.project(target(0));
            Mark slope=active.project(target(1)).minus(p0);
            if (slope.norm()<=1) return refusal("The approved view has no usable vertical scale.");
            double estimate=seen.minus(p0).dot(slope)/slope.dot(slope);
            double lo=hypotheses[0]-.05, hi=hypotheses[1]+.08;
            if (estimate<lo || estimate>hi) return refusal("The candidate height falls outside the registered working range.");
            var point=target(estimate);
            if (active.project(point).minus(seen).norm()>WITNESS_LIMIT_PX)
                return refusal("The selected marker does not lie on the registered vertical segment.");
            double budget=Math.max(active.budget(target(lo)),active.budget(target(hi)));
            double radius=budget/slope.norm();
            if (radius>.025 || estimate-radius<.05) return refusal("The selected view leaves height too uncertain for a collider.");
            var check=fit(witness);
            if (Math.abs(ObserverGeometry.dot(active.axis(),check.axis()))>.97)
                return refusal("The withheld camera needs a distinct viewing angle.");
            if (score(check).getDouble("separationLowerPx")<=0)
                return refusal("The withheld view cannot distinguish the hypotheses.");
            Mark observed=observe(check), projected=check.project(point);
            double residual=observed.minus(projected).norm();
            if (residual>WITNESS_LIMIT_PX) return refusal("The withheld view disagrees with the measured height.")
                    .put("withheldErrorPx",residual).put("witnessView",witness.id);
            return new JSONObject().put("passed",true).put("selectedView",selected.id).put("witnessView",witness.id)
                    .put("heightM",estimate).put("lowM",estimate-radius).put("highM",estimate+radius)
                    .put("localizationBudgetPx",budget).put("measuredMarkerPx",seen.json())
                    .put("withheldObservedPx",observed.json()).put("withheldProjectedPx",projected.json())
                    .put("withheldErrorPx",residual).put("withheldLimitPx",WITNESS_LIMIT_PX);
        } catch (IllegalArgumentException error) {
            return refusal(error.getMessage());
        }
    }

    JSONObject collider(JSONObject measured,String evidenceSha) {
        if (!measured.getBoolean("passed")) throw new IllegalStateException("A verified height is required before proposing collision geometry.");
        double height=measured.getDouble("highM");
        double x=anchor[0],z=anchor[2];
        return new JSONObject().put("format","fraynix.spatial-scene").put("version",1)
                .put("title","Reviewed collider · "+title).put("units","m").put("frame","right-handed-y-up")
                .put("source",new JSONObject().put("kind","synthetic").put("physicalProperties","declared")
                        .put("note","Registered orthographic height interval. Footprint declared. Height evidence SHA-256 "+evidenceSha))
                .put("scaleReference",new JSONObject().put("a",new JSONArray(new int[]{0,0,0}))
                        .put("b",new JSONArray(new int[]{1,0,0})).put("knownLengthM",1)
                        .put("toleranceM",0).put("basis","declared-test"))
                .put("gravityMps2",9.81).put("objects",new JSONArray()
                        .put(box("deck","Support deck",new double[]{0,-.08,0},new double[]{3.7,.16,3.7},0))
                        .put(box("prop","Upper-bound height collider",new double[]{x,height/2,z},
                                new double[]{footprint[0],height,footprint[1]},1)));
    }

    JSONObject previews(JSONObject record) {
        var out=new JSONObject().put("initial",initial.observation);
        var result=record.optJSONObject("measurement");
        if (result!=null) {
            String id=record.getJSONObject("plan").getString("selectedView");
            out.put("selected",candidates.get(id).observation);
            if (result.has("witnessView")) out.put("witness",witness.observation);
        }
        return out;
    }

    private JSONObject score(Fit fit) {
        double[] low=target(hypotheses[0]),high=target(hypotheses[1]);
        Mark a=fit.project(low),b=fit.project(high);
        double distance=a.minus(b).norm();
        double lower=fit.inFrame(a)&&fit.inFrame(b)?Math.max(0,distance-fit.budget(low)-fit.budget(high)):0;
        return new JSONObject().put("view",fit.view.id).put("hypothesisDistancePx",distance)
                .put("separationLowerPx",lower).put("calibrationCheckPx",fit.checkErrorPx);
    }

    private Fit fit(View view) throws Exception {
        var image=frame(view.calibration);
        var marks=marks(image,false);
        var provisional=new Fit(view,image.getWidth(),image.getHeight(),inverse,marks,0);
        provisional.axis();
        for (Mark marker:marks) checkDiameter(marker,provisional.scale()*.050,false);
        double residual=0;
        for (int i=4;i<6;i++) residual=Math.max(residual,provisional.project(landmarks[i]).minus(marks[i]).norm());
        if (residual>1.2) throw new IllegalArgumentException("Independent calibration marks disagree for "+view.id+".");
        return new Fit(view,image.getWidth(),image.getHeight(),inverse,marks,residual);
    }

    private Mark observe(Fit fit) throws Exception {
        var image=frame(fit.view.observation);
        if (image.getWidth()!=fit.width || image.getHeight()!=fit.height)
            throw new IllegalArgumentException("Image scale changed for "+fit.view.id+".");
        var marks=marks(image,true);
        double scale=fit.scale();
        for (int i=0;i<6;i++) {
            checkDiameter(marks[i],scale*.050,false);
            if (marks[i].minus(fit.marks[i]).norm()>LOCALIZATION_PX)
                throw new IllegalArgumentException("Calibration marks moved in "+fit.view.id+".");
        }
        checkDiameter(marks[6],scale*.048,true);
        return marks[6];
    }

    private static void checkDiameter(Mark mark,double pixels,boolean target) {
        if (Math.min(mark.width,mark.height)<pixels-3.4 || Math.max(mark.width,mark.height)>pixels+1.5)
            throw new IllegalArgumentException(target?"The top marker has an incomplete or unexpected silhouette."
                    :"A calibration marker has an incomplete or unexpected silhouette.");
    }

    private double[] target(double height) {
        return new double[]{anchor[0],anchor[1]+height+TARGET_RISE_M,anchor[2]};
    }

    private static double[] pair(JSONArray arr,double min,double max) {
        var values=PlaneCalibration.point(arr);
        for (double value:values) PlaneCalibration.bounded(value,min,max);
        return values;
    }

    private static JSONObject refusal(String reason) {
        return new JSONObject().put("passed",false).put("reason",reason);
    }

    private static JSONObject box(String id,String label,double[] position,double[] size,int mass) {
        return new JSONObject().put("sourceId",id).put("label",label).put("shape","box").put("massKg",mass)
                .put("position",new JSONArray(position)).put("size",new JSONArray(size))
                .put("rotation",new JSONArray(new int[]{0,0,0,1})).put("restitution",0).put("friction",.5);
    }

    private static BufferedImage frame(String encoded) throws Exception {
        try {
            return WorldObservationStore.decode(pngBytes(encoded));
        } catch (java.io.IOException error) {
            throw new IllegalArgumentException("Could not decode the registered PNG view.",error);
        }
    }

    private static byte[] pngBytes(String encoded) {
        byte[] bytes=Base64.getDecoder().decode(encoded);
        var header=ByteBuffer.wrap(bytes);
        if (bytes.length<33 || bytes.length>180000 || header.getLong()!=0x89504e470d0a1a0aL
                || header.getInt()!=13 || header.getInt()!=0x49484452)
            throw new IllegalArgumentException("Each view needs a PNG up to 180 KB.");
        int width=header.getInt(),height=header.getInt();
        if (width<128 || width>1280 || height<128 || height>900)
            throw new IllegalArgumentException("Registered images need 128 to 1280 by 128 to 900 pixels.");
        return bytes;
    }

    private static Mark[] marks(BufferedImage image,boolean target) {
        int count=target?7:6;
        int[] n=new int[count],xmin=new int[count],ymin=new int[count],xmax=new int[count],ymax=new int[count];
        int[] seed=new int[count];
        double[] sx=new double[count],sy=new double[count];
        java.util.Arrays.fill(xmin,image.getWidth());
        java.util.Arrays.fill(ymin,image.getHeight());
        int[] pixels=image.getRGB(0,0,image.getWidth(),image.getHeight(),null,0,image.getWidth());
        for (int i=0;i<pixels.length;i++) {
            int index=opaqueColor(pixels[i],count);
            if (index<0) continue;
            seed[index]=i;
            int x=i%image.getWidth(), y=i/image.getWidth();
            n[index]++;
            sx[index]+=x+.5;
            sy[index]+=y+.5;
            xmin[index]=Math.min(xmin[index],x); xmax[index]=Math.max(xmax[index],x);
            ymin[index]=Math.min(ymin[index],y); ymax[index]=Math.max(ymax[index],y);
        }
        var out=new Mark[count];
        for (int i=0;i<count;i++) {
            int w=xmax[i]-xmin[i]+1,h=ymax[i]-ymin[i]+1;
            if (n[i]<24 || w>40 || h>40 || xmin[i]<=0 || ymin[i]<=0 || xmax[i]>=image.getWidth()-1
                    || ymax[i]>=image.getHeight()-1 || Math.abs(w-h)>2 || n[i]<.54*w*h
                    || Math.hypot(sx[i]/n[i]-(xmin[i]+xmax[i]+1)/2.0,sy[i]/n[i]-(ymin[i]+ymax[i]+1)/2.0)>.7
                    || connected(pixels,image.getWidth(),i,xmin[i],ymin[i],w,h,seed[i])<.95*n[i])
                throw new IllegalArgumentException(i==6?"The top marker is missing, clipped or ambiguous.":"A fixed calibration marker is missing, clipped or ambiguous.");
            out[i]=new Mark(sx[i]/n[i],sy[i]/n[i],w,h);
        }
        return out;
    }

    private static int connected(int[] pixels,int stride,int mark,int left,int top,int width,int height,int seed) {
        var visited=new boolean[width*height];
        var queue=new int[visited.length];
        int start=(seed/stride-top)*width+seed%stride-left;
        queue[0]=start;
        visited[start]=true;
        int tail=1;
        for (int head=0;head<tail;head++) {
            int x=queue[head]%width,y=queue[head]/width;
            for (int dy=-1;dy<=1;dy++) for (int dx=-1;dx<=1;dx++) {
                int nx=x+dx,ny=y+dy;
                if (nx<0 || nx>=width || ny<0 || ny>=height) continue;
                int index=ny*width+nx;
                if (visited[index] || opaqueColor(pixels[(top+ny)*stride+left+nx],COLORS.length)!=mark) continue;
                visited[index]=true;
                queue[tail++]=index;
            }
        }
        return tail;
    }

    private static int opaqueColor(int argb,int count) {
        return (argb>>>24)==255?color(argb&0xffffff,count):-1;
    }

    private static int color(int value,int count) {
        int r=value>>16,g=(value>>8)&255,b=value&255;
        if (Math.max(r,Math.max(g,b))-Math.min(r,Math.min(g,b))<130) return -1;
        for (int i=0;i<count;i++) {
            int c=COLORS[i];
            if (Math.abs(r-(c>>16))<=4 && Math.abs(g-((c>>8)&255))<=4 && Math.abs(b-(c&255))<=4) return i;
        }
        return -1;
    }

    private static double[][] inverse(double[][] points) {
        double[][] m=new double[4][8];
        for (int i=0;i<4;i++) {
            System.arraycopy(points[i],0,m[i],0,3);
            m[i][3]=1;
            m[i][i+4]=1;
        }
        for (int c=0;c<4;c++) {
            int pivot=c;
            for (int i=c+1;i<4;i++) if (Math.abs(m[i][c])>Math.abs(m[pivot][c])) pivot=i;
            var row=m[pivot]; m[pivot]=m[c]; m[c]=row;
            double scale=m[c][c];
            if (Math.abs(scale)<.02) throw new IllegalArgumentException("Calibration landmarks do not span a stable 3D frame.");
            for (int j=0;j<8;j++) m[c][j]/=scale;
            for (int i=0;i<4;i++) if (i!=c) {
                scale=m[i][c];
                for (int j=0;j<8;j++) m[i][j]-=scale*m[c][j];
            }
        }
        double[][] out=new double[4][4];
        for (int i=0;i<4;i++) System.arraycopy(m[i],4,out[i],0,4);
        return out;
    }
}
