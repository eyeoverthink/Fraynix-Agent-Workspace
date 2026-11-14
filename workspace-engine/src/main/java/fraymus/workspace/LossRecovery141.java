package fraymus.workspace;

import java.awt.image.BufferedImage;
import org.json.*;

/** Supplemental measurements never change the underlying139 guard or its accepted proposals. */
final class LossRecovery141 implements ImageTracker {
    private final String id,label;
    private final boolean enabled;
    private final ForegroundGuard139 baseline;
    private final RecoverySearch141 search;
    private final AppearanceEvidence138 anchor;
    private final SpatialEvidence139 parts;
    private final int eligible;
    private PixelTracker.Box pending;
    private FlowTracker continuity;
    private boolean loss;
    private int confirmations,ordinal;
    LossRecovery141(String id,String label,BufferedImage first,PixelTracker.Box seed,boolean enabled){
        this.id=id;this.label=label;this.enabled=enabled;baseline=new ForegroundGuard139(id,label,first,seed,true);
        search=new RecoverySearch141(first,seed);anchor=new AppearanceEvidence138(first,seed);parts=new SpatialEvidence139(first,seed);eligible=parts.assess(first,seed).getInt("eligibleParts");
    }
    public JSONObject update(BufferedImage image){
        var base=baseline.update(image);var out=ParsedJson137.copy(base);boolean baseAccepted=!base.isNull("box");
        if(base.getJSONObject("drift139").getJSONObject("proposal").isNull("box"))loss=true;
        var evidence=new JSONObject().put("attempted",false).put("accepted",false).put("qualified",false).put("confirmations",0).put("requiredConfirmations",3).put("eligibleParts",eligible);
        if(baseAccepted){loss=false;pending=null;continuity=null;confirmations=0;}
        else if(loss&&eligible>=4){
            var small=PixelTracker.working(image);var found=search.search(small);var appearance=anchor.assess(small,found.box());
            boolean supported=found.unique()&&AppearanceEvidence138.reasons(appearance).isEmpty()&&appearance.getDouble("colorAgreement")>=.90&&parts.supports(SpatialEvidence139.working(image),found.box());
            double overlap=pending==null?0:PixelTracker.iou(pending,found.box());boolean coherent=false;var motion=new JSONObject();
            if(supported&&continuity!=null&&overlap>=.55){
                var tracked=continuity.update(image);motion=tracked.getJSONObject("motion");
                coherent=!tracked.isNull("box")&&"pyramidal-flow".equals(motion.getString("method"))&&!motion.getBoolean("ambiguous")&&motion.getInt("inliers")>=8&&motion.getDouble("medianForwardBackwardPixels")<=1.2&&PixelTracker.iou(PixelTracker.Box.from(tracked.getJSONArray("box")),found.box())>=.7;
            }
            if(supported){confirmations=coherent?Math.min(3,confirmations+1):1;pending=found.box();continuity=new FlowTracker(id,label,image,found.box());}
            else{confirmations=0;pending=null;continuity=null;}
            boolean qualified=supported&&confirmations>=3;
            evidence=found.json().put("attempted",true).put("appearance",appearance).put("spatialSupported",supported).put("eligibleParts",eligible).put("confirmationOverlap",overlap).put("motion",motion).put("coherentMotion",coherent).put("confirmations",confirmations).put("requiredConfirmations",3).put("qualified",qualified).put("accepted",enabled&&qualified);
            if(enabled&&qualified)out.put("box",found.box().json()).put("lastKnownBox",found.box().json()).put("status","source-recovered-141").put("matchScore",found.score());
        }else{pending=null;continuity=null;confirmations=0;}
        return out.put("recovery141",new JSONObject().put("schema","loss-recovery-trace-141-v1").put("ordinal",++ordinal).put("enabled",enabled).put("anchorSha256",search.hash).put("automaticAdmissions",0).put("baselineAccepted",baseAccepted).put("lossObserved",loss).put("baseline",base).put("withheld",out.isNull("box")).put("recovery",evidence).put("scope","Original-source appearance, spatial support and repeated motion do not establish semantic identity. Independent outcomes verify every additional measurement."));
    }
}
