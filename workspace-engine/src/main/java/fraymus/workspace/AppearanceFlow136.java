package fraymus.workspace;

import java.awt.image.BufferedImage;
import java.util.*;
import org.json.*;

/** Incumbent motion first; a bounded, source-linked bank may supplement losses.
 * Admission verifies motion conditions, not semantic identity. No labels enter this class. */
final class AppearanceFlow136 implements ImageTracker {
    static final int CAPACITY = 6;
    private final String id, label;
    private final int width, height;
    private final BufferedImage seedImage;
    private final PixelTracker.Box seedBox;
    private final boolean enabled;
    private final ArrayList<Example> bank = new ArrayList<>();
    private final double[] originalHistogram;
    private FlowTracker flow;
    private BufferedImage lastImage;
    private PixelTracker.Box lastBox, pending;
    private int ordinal, lastOrdinal, clean, lastAdmission, cooldown, pendingSource = -1;
    private record Example(double[] descriptor, double[] histogram, PixelTracker.Box box, JSONObject proof) {}
    private record Proposal(PixelTracker.Box box, double score, Example example) {}

    AppearanceFlow136(String id, String label, BufferedImage first, PixelTracker.Box seed, boolean enabled) {
        if (!seed.measurable()) throw new IllegalArgumentException("Valid source seed required");
        this.id=id; this.label=label; this.enabled=enabled; width=first.getWidth(); height=first.getHeight();
        seedImage=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);var g=seedImage.createGraphics();g.drawImage(first,0,0,null);g.dispose();seedBox=seed;
        flow=new FlowTracker(id,label,first,seed); lastImage=first; lastBox=seed;
        originalHistogram=histogram(PixelTracker.descriptor(PixelTracker.working(first),seed));
        bank.add(example(first,seed,new JSONObject().put("verification","initial-source-seed")));
    }

    private Example example(BufferedImage image, PixelTracker.Box box, JSONObject checks) {
        double[] d=PixelTracker.descriptor(PixelTracker.working(image),box);
        var proof=new JSONObject().put("sourceOrdinal",ordinal).put("box",box.json())
            .put("descriptorSha256",WorldFrames.hash(new JSONObject().put("values",new JSONArray(d))))
            .put("checks",checks);
        return new Example(d,histogram(d),box,proof);
    }

    private static double[] histogram(double[] d) {
        var h=new double[64];
        for(int i=0;i<d.length;i+=3)h[Math.min(3,(int)(d[i]*4))*16+Math.min(3,(int)(d[i+1]*4))*4+Math.min(3,(int)(d[i+2]*4))]++;
        for(int i=0;i<h.length;i++)h[i]/=d.length/3.;
        return h;
    }

    private static double agreement(double[] a,double[] b) {
        double sum=0; for(int i=0;i<a.length;i++)sum+=Math.sqrt(a[i]*b[i]); return sum;
    }

    private static boolean strongMotion(JSONObject prediction) {
        var m=prediction.optJSONObject("motion");
        return !prediction.isNull("box") && m!=null && m.getString("method").equals("pyramidal-flow")
            && !m.getBoolean("ambiguous") && m.getInt("inliers")>=16
            && m.getInt("inliers")>=.6*m.getInt("seedPoints") && m.getDouble("medianForwardBackwardPixels")<=.35;
    }

    private JSONObject admit(BufferedImage image,JSONObject prediction,boolean consecutive) {
        var reason=new JSONObject().put("accepted",false);
        if(!enabled)return reason.put("reason","bank disabled for control comparison");
        if(!consecutive || !strongMotion(prediction)){clean=0;return reason.put("reason","requires uninterrupted strong motion; no fallback examples");}
        clean++;
        if(cooldown>0){cooldown--;return reason.put("reason","recovery quarantine");}
        if(clean<3 || ordinal-lastAdmission<3)return reason.put("reason","collecting consecutive motion evidence");
        var box=PixelTracker.Box.from(prediction.getJSONArray("box"));
        var reverse=new FlowTracker(id,label,image,box).update(lastImage);
        double reverseOverlap=reverse.isNull("box")?0:PixelTracker.iou(lastBox,PixelTracker.Box.from(reverse.getJSONArray("box")));
        if(!strongMotion(reverse)||reverseOverlap<.85)return reason.put("reason","reverse motion/box check refused").put("reverseOverlap",reverseOverlap);
        var vectors=prediction.getJSONObject("motion").getJSONArray("vectors");
        double minX=1,maxX=0,minY=1,maxY=0;
        for(Object raw:vectors){var v=(JSONArray)raw;minX=Math.min(minX,v.getDouble(0));maxX=Math.max(maxX,v.getDouble(0));minY=Math.min(minY,v.getDouble(1));maxY=Math.max(maxY,v.getDouble(1));}
        double spanX=(maxX-minX)/lastBox.w(),spanY=(maxY-minY)/lastBox.h();
        if(spanX<.2||spanY<.04)return reason.put("reason","reported motion samples cover too little of the source box");
        var d=PixelTracker.descriptor(PixelTracker.working(image),box);
        double color=agreement(originalHistogram,histogram(d));
        if(color<.7)return reason.put("reason","original color consistency refused");
        double nearest=-1;for(var e:bank)nearest=Math.max(nearest,PixelTracker.correlation(e.descriptor,d));
        if(nearest>=.995)return reason.put("reason","appearance already represented");
        var anchor=anchor(image,box);
        if(!anchor.getBoolean("accepted"))return reason.put("reason","immutable source reconstruction refused; proposal not admitted").put("anchor136",anchor);
        var forwardProof=WorldFrames.copy(prediction.getJSONObject("motion"));forwardProof.remove("vectors");
        var reverseProof=WorldFrames.copy(reverse.getJSONObject("motion"));reverseProof.remove("vectors");
        var checks=new JSONObject().put("verification","seed-cycle-136").put("anchor136",anchor).put("consecutiveStrongFrames",clean)
            .put("previousSourceOrdinal",lastOrdinal).put("reverseOverlap",reverseOverlap)
            .put("forward",forwardProof).put("reverse",reverseProof)
            .put("reportedSpanX",spanX).put("reportedSpanY",spanY).put("originalColorAgreement",color).put("nearestAppearance",nearest);
        var e=example(image,box,checks);JSONObject retired=null;
        if(bank.size()==CAPACITY)retired=bank.remove(1).proof;
        bank.add(e);lastAdmission=ordinal;
        return reason.put("accepted",true).put("reason","original-seed and local motion checks passed; independent identity audit still required")
            .put("example",e.proof).put("retired",retired==null?JSONObject.NULL:retired);
    }

    private JSONObject anchor(BufferedImage image,PixelTracker.Box current){
        var forward=new FlowTracker(id,label,seedImage,seedBox).update(image);
        var reverse=new FlowTracker(id,label,image,current).update(seedImage);
        double overlap=forward.isNull("box")?0:PixelTracker.iou(current,PixelTracker.Box.from(forward.getJSONArray("box")));
        double back=reverse.isNull("box")?0:PixelTracker.iou(seedBox,PixelTracker.Box.from(reverse.getJSONArray("box")));
        var a=new JSONObject().put("sourceOrdinal",0).put("sourceBox",seedBox.json()).put("proposedBox",current.json())
            .put("forwardBox",forward.opt("box")).put("reverseBox",reverse.opt("box")).put("overlap",overlap).put("reverseOverlap",back)
            .put("accepted",strongMotion(forward)&&strongMotion(reverse)&&overlap>=.75&&back>=.75);
        for(var pair:List.of(new Object[]{"forward",forward},new Object[]{"reverse",reverse})){
            var motion=((JSONObject)pair[1]).optJSONObject("motion");if(motion!=null){var proof=WorldFrames.copy(motion);proof.remove("vectors");a.put((String)pair[0],proof);}
        }
        return a;
    }

    private Proposal search(BufferedImage image) {
        var small=PixelTracker.working(image);var candidates=new ArrayList<Proposal>();
        for(var e:bank){
            var match=new PixelTracker(id,label,e.descriptor,e.box,false).match(small,true);
            if(match.score()<.88||!match.box().measurable())continue;
            // Refine the competing peak too: a coarse grid alone can undersample
            // an identical copy and manufacture a misleading separation margin.
            if(rival(small,e,match.box())>=match.score()-.05)return null;
            var h=histogram(PixelTracker.descriptor(small,match.box()));
            if(agreement(h,e.histogram)<.8||agreement(h,originalHistogram)<.7)continue;
            candidates.add(new Proposal(match.box(),match.score(),e));
        }
        candidates.sort(Comparator.comparingDouble(Proposal::score).reversed());
        if(candidates.isEmpty())return null;var best=candidates.get(0);
        for(var other:candidates)if(PixelTracker.iou(best.box,other.box)<.25&&other.score>=best.score-.05)return null;
        return best;
    }

    private static double rival(BufferedImage image,Example e,PixelTracker.Box winner) {
        double top=-1;
        for(double scale:new double[]{.9,1,1.1}){
            double w=winner.w()*scale,h=winner.h()*scale;if(w>1||h>1)continue;
            double score=-1,bx=0,by=0;
            for(double y=0;y<=1-h;y+=.025)for(double x=0;x<=1-w;x+=.025){
                var b=new PixelTracker.Box(x,y,w,h);if(PixelTracker.iou(b,winner)>=.25)continue;
                double s=PixelTracker.correlation(e.descriptor,PixelTracker.descriptor(image,b));
                if(s>score){score=s;bx=x;by=y;}
            }
            top=Math.max(top,score);
            if(score<.5)continue;
            for(double y=Math.max(0,by-.025);y<=Math.min(1-h,by+.025);y+=.002)
                for(double x=Math.max(0,bx-.025);x<=Math.min(1-w,bx+.025);x+=.002){
                    var b=new PixelTracker.Box(x,y,w,h);if(PixelTracker.iou(b,winner)<.25)
                        top=Math.max(top,PixelTracker.correlation(e.descriptor,PixelTracker.descriptor(image,b)));
                }
        }
        return top;
    }

    public JSONObject update(BufferedImage image) {
        if(image.getWidth()!=width||image.getHeight()!=height)throw new IllegalArgumentException("Original image dimensions changed");
        ordinal++;
        var prediction=flow.update(image);boolean incumbent=!prediction.isNull("box");
        var admission=new JSONObject().put("accepted",false).put("reason","no current incumbent measurement");
        var recovery=new JSONObject().put("attempted",false).put("accepted",false);
        if(incumbent){
            admission=admit(image,prediction,lastOrdinal==ordinal-1);
            lastBox=PixelTracker.Box.from(prediction.getJSONArray("box"));lastImage=image;lastOrdinal=ordinal;pending=null;pendingSource=-1;
        }else{
            clean=0;
            if(enabled){
                var proposal=search(image);recovery.put("attempted",true);
                if(proposal==null){pending=null;pendingSource=-1;recovery.put("reason","no unique strong checked-example match");}
                else{
                    int source=proposal.example.proof.getInt("sourceOrdinal");
                    boolean confirmed=pending!=null&&pendingSource==source&&PixelTracker.iou(pending,proposal.box)>=.55;
                    recovery.put("sourceExampleOrdinal",source).put("score",proposal.score).put("accepted",confirmed)
                        .put("reason",confirmed?"same separated example confirmed on consecutive images":"waiting for a second consistent image");
                    if(confirmed){
                        lastBox=proposal.box;lastImage=image;lastOrdinal=ordinal;cooldown=6;
                        flow=new FlowTracker(id,label,image,lastBox);pending=null;pendingSource=-1;
                        prediction.put("box",lastBox.json()).put("lastKnownBox",lastBox.json()).put("status","reacquired")
                            .put("matchScore",proposal.score).put("motion",new JSONObject().put("method","checked-bank-recovery-135"));
                    }else{pending=proposal.box;pendingSource=source;}
                }
            }
        }
        var proofs=new JSONArray();for(var e:bank)proofs.put(e.proof);
        prediction.put("appearance135",WorldFrames.copy(new JSONObject().put("schema","seed-anchored-136-v1").put("enabled",enabled)
            .put("ordinal",ordinal).put("incumbentAccepted",incumbent).put("capacity",CAPACITY).put("examples",proofs)
            .put("admission",admission).put("recovery",recovery)
            .put("scope","Source seed plus motion-checked examples; these checks do not establish semantic identity. Labels are downstream only.")));
        return prediction;
    }
}
