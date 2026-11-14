package fraymus.workspace;

import java.awt.image.BufferedImage;
import org.json.*;

/** Supplemental measurements never change the underlying139 guard or its accepted proposals. */
final class PartialRecovery142 implements ImageTracker {
    private final String id,label;
    private final boolean enabled;
    private final ForegroundGuard139 baseline;
    private final PartialSearch142 search;
    private final boolean imported;
    private String pendingKey;
    private PixelTracker.Box pending;
    private FlowTracker continuity;
    private boolean loss;
    private int confirmations,ordinal;
    PartialRecovery142(String id,String label,BufferedImage first,PixelTracker.Box seed,JSONObject memory,boolean enabled,boolean imported){
        this.id=id;this.label=label;this.enabled=enabled;baseline=new ForegroundGuard139(id,label,first,seed,true);
        this.imported=imported;search=new PartialSearch142(first,seed,memory,imported);
    }
    public JSONObject update(BufferedImage image){
        var base=baseline.update(image);var out=ParsedJson137.copy(base);boolean baseAccepted=!base.isNull("box");
        if(base.getJSONObject("drift139").getJSONObject("proposal").isNull("box"))loss=true;
        var evidence=new JSONObject().put("attempted",false).put("accepted",false).put("qualified",false).put("confirmations",0).put("requiredConfirmations",3);
        if(baseAccepted){loss=false;pending=null;continuity=null;confirmations=0;}
        else if(loss){
            var found=search.search(image);boolean supported=found.unique();
            double overlap=pending==null?0:PixelTracker.iou(pending,found.box());boolean coherent=false;var motion=new JSONObject();
            if(supported&&continuity!=null&&found.key().equals(pendingKey)&&overlap>=.55){
                var tracked=continuity.update(image);motion=tracked.getJSONObject("motion");
                coherent=!tracked.isNull("box")&&"pyramidal-flow".equals(motion.getString("method"))&&!motion.getBoolean("ambiguous")&&motion.getInt("inliers")>=8&&motion.getDouble("medianForwardBackwardPixels")<=1.2&&PixelTracker.iou(PixelTracker.Box.from(tracked.getJSONArray("box")),found.box())>=.7;
            }
            if(supported){confirmations=coherent?Math.min(3,confirmations+1):1;pending=found.box();pendingKey=found.key();continuity=new FlowTracker(id,label,image,found.box());}
            else{confirmations=0;pending=null;continuity=null;}
            boolean qualified=supported&&confirmations>=3;
            evidence=found.json().put("attempted",true).put("confirmationOverlap",overlap).put("motion",motion).put("coherentMotion",coherent).put("confirmations",confirmations).put("requiredConfirmations",3).put("qualified",qualified).put("accepted",enabled&&qualified);
            if(enabled&&qualified)out.put("box",found.box().json()).put("lastKnownBox",found.box().json()).put("status","partial-source-recovered-142").put("matchScore",found.score());
        }else{pending=null;continuity=null;confirmations=0;}
        return out.put("partial142",new JSONObject().put("schema","partial-reference-trace-142-v1").put("ordinal",++ordinal).put("enabled",enabled).put("memorySha256",search.memoryHash).put("importedEnabled",imported).put("automaticAdmissions",0).put("baselineAccepted",baseAccepted).put("lossObserved",loss).put("baseline",base).put("withheld",out.isNull("box")).put("recovery",evidence).put("scope","Fixed visible regions of verified full appearances plus adjacent motion. Whole geometry inferred from visible support; not physical contact or semantic identity. No target prediction is admitted to memory."));
    }
}
