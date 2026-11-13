package fraymus.workspace;
import java.awt.image.BufferedImage;
import java.util.*;
import org.json.*;

/** Frozen imported references supplement loss only; current predictions never train this bank. */
final class VerifiedMemoryFlow137 implements ImageTracker {
    private final String id,label,memoryHash;
    private final int width,height;
    private final boolean enabled;
    private final ArrayList<Example> bank=new ArrayList<>();
    private final double[] originalHistogram;
    private FlowTracker flow;
    private PixelTracker.Box pending;
    private String pendingSource;
    private int ordinal;
    private record Example(double[] descriptor,double[] histogram,PixelTracker.Box box,JSONObject proof){}
    private record Proposal(PixelTracker.Box box,double score,Example example){}
    VerifiedMemoryFlow137(String id,String label,BufferedImage first,PixelTracker.Box seed,JSONObject memory,boolean enabled){
        if(!seed.measurable())throw new IllegalArgumentException("Valid source seed required");
        this.id=id;this.label=label;this.enabled=enabled;width=first.getWidth();height=first.getHeight();
        memoryHash=WorldFrames.hash(memory);flow=new FlowTracker(id,label,first,seed);
        var original=PixelTracker.descriptor(PixelTracker.working(first),seed);originalHistogram=histogram(original);
        bank.add(new Example(original,originalHistogram,seed,new JSONObject().put("key","initial-seed")));
        var examples=memory.getJSONArray("examples");if(examples.length()<1||examples.length()>ReferenceMemory137.CAPACITY)throw new IllegalArgumentException("Bounded verified examples required");
        if(enabled)for(Object raw:examples){var e=(JSONObject)raw;var values=e.getJSONArray("descriptor");
            if(values.length()!=192||!WorldFrames.hash(new JSONObject().put("values",values)).equals(e.getString("descriptorSha256")))throw new IllegalArgumentException("Reference descriptor changed");
            double[] d=new double[values.length()];for(int i=0;i<d.length;i++){d[i]=values.getDouble(i);if(!Double.isFinite(d[i])||d[i]<0||d[i]>1)throw new IllegalArgumentException("Reference descriptor outside pixel range");}
            bank.add(new Example(d,histogram(d),seed,new JSONObject().put("key",e.getString("imageSha256")+":"+e.getInt("ordinal")).put("descriptorSha256",e.getString("descriptorSha256"))));
        }
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

    public JSONObject update(BufferedImage image){
        if(image.getWidth()!=width||image.getHeight()!=height)throw new IllegalArgumentException("Original image dimensions changed");
        ordinal++;var prediction=flow.update(image);boolean incumbent=!prediction.isNull("box");
        var recovery=new JSONObject().put("attempted",false).put("accepted",false);
        if(incumbent){pending=null;pendingSource=null;}
        else{
            recovery.put("attempted",true);var proposal=search(image);
            if(proposal==null){pending=null;pendingSource=null;recovery.put("reason","No unique strong frozen-reference match");}
            else{
                String key=proposal.example.proof.getString("key");double overlap=pending==null?0:PixelTracker.iou(pending,proposal.box);
                boolean confirmed=key.equals(pendingSource)&&overlap>=.55;
                recovery.put("sourceKey",key).put("score",proposal.score).put("box",proposal.box.json()).put("confirmationOverlap",overlap)
                    .put("accepted",confirmed).put("reason",confirmed?"Two consecutive unique frozen-reference matches":"Waiting for a second consistent image");
                if(confirmed){flow=new FlowTracker(id,label,image,proposal.box);pending=null;pendingSource=null;
                    prediction.put("box",proposal.box.json()).put("lastKnownBox",proposal.box.json()).put("status","reacquired")
                        .put("matchScore",proposal.score).put("motion",new JSONObject().put("method","verified-reference-recovery-137"));
                }else{pending=proposal.box;pendingSource=key;}
            }
        }
        return prediction.put("reference137",new JSONObject().put("schema","verified-reference-trace-137-v1").put("ordinal",ordinal)
            .put("memorySha256",memoryHash).put("importedEnabled",enabled).put("bankSize",bank.size()).put("incumbentAccepted",incumbent)
            .put("automaticAdmissions",0).put("recovery",recovery));
    }
}
