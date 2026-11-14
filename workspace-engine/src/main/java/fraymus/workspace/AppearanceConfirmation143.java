package fraymus.workspace;

import java.awt.image.BufferedImage;
import java.util.*;
import org.json.*;

/** Each immutable reference earns its own uninterrupted evidence. Counts never migrate between references. */
final class AppearanceConfirmation143 implements ImageTracker {
    private static final class Lane {
        PixelTracker.Box pending;
        FlowTracker continuity;
        int confirmations;
        void clear(){pending=null;continuity=null;confirmations=0;}
    }
    private final String id,label;
    private final boolean enabled,imported;
    private final ForegroundGuard139 baseline;
    private final ReferenceSearch143 search;
    private final Map<String,Lane> lanes=new LinkedHashMap<>();
    private boolean loss;
    private int ordinal;

    AppearanceConfirmation143(String id,String label,BufferedImage first,PixelTracker.Box seed,JSONObject memory,boolean enabled,boolean imported){
        this.id=id;this.label=label;this.enabled=enabled;this.imported=imported;
        baseline=new ForegroundGuard139(id,label,first,seed,true);
        search=new ReferenceSearch143(first,seed,memory,imported);
    }
    public JSONObject update(BufferedImage image){
        var base=baseline.update(image);var out=ParsedJson137.copy(base);
        boolean baseAccepted=!base.isNull("box");
        if(base.getJSONObject("drift139").getJSONObject("proposal").isNull("box"))loss=true;
        var hypotheses=new JSONArray();var counts=new JSONObject();int maximum=0;
        JSONObject recovery=new JSONObject().put("attempted",false).put("accepted",false).put("qualified",false).put("confirmations",0).put("requiredConfirmations",3);
        if(baseAccepted){loss=false;lanes.clear();}
        else if(loss){
            var batch=search.search(image);counts=batch.counts();
            ReferenceSearch143.Result bestQualified=null,bestCandidate=null;JSONObject qualifiedEvidence=null,bestEvidence=null;
            for(var found:batch.candidates()){
                var lane=lanes.computeIfAbsent(found.key(),ignored->new Lane());
                double overlap=lane.pending==null?0:PixelTracker.iou(lane.pending,found.box());
                boolean coherent=false;var motion=new JSONObject();
                if(found.unique()&&lane.continuity!=null&&overlap>=.55){
                    var tracked=lane.continuity.update(image);motion=tracked.getJSONObject("motion");
                    coherent=!tracked.isNull("box")&&"pyramidal-flow".equals(motion.getString("method"))&&!motion.getBoolean("ambiguous")&&motion.getInt("inliers")>=8&&motion.getDouble("medianForwardBackwardPixels")<=1.2&&PixelTracker.iou(PixelTracker.Box.from(tracked.getJSONArray("box")),found.box())>=.7;
                }
                if(found.unique()){
                    lane.confirmations=coherent?Math.min(3,lane.confirmations+1):1;
                    lane.pending=found.box();lane.continuity=new FlowTracker(id,label,image,found.box());
                }else lane.clear();
                maximum=Math.max(maximum,lane.confirmations);
                boolean qualified=found.unique()&&lane.confirmations>=3;
                var evidence=found.json().put("attempted",true).put("confirmationOverlap",overlap).put("motion",motion).put("coherentMotion",coherent).put("confirmations",lane.confirmations).put("requiredConfirmations",3).put("qualified",qualified).put("accepted",false);
                hypotheses.put(evidence);
                if(bestCandidate==null||found.score()>bestCandidate.score()){bestCandidate=found;bestEvidence=evidence;}
                if(qualified&&(bestQualified==null||found.score()>bestQualified.score())){bestQualified=found;qualifiedEvidence=evidence;}
            }
            if(lanes.size()>ReferenceMemory137.CAPACITY+1)throw new IllegalStateException("Reference confirmation capacity exceeded");
            recovery=ParsedJson137.copy(bestQualified==null?bestEvidence:qualifiedEvidence);
            recovery.put("accepted",enabled&&bestQualified!=null);
            if(enabled&&bestQualified!=null)out.put("box",bestQualified.box().json()).put("lastKnownBox",bestQualified.box().json()).put("status","reference-confirmed-143").put("matchScore",bestQualified.score());
        }else lanes.clear();
        return out.put("references143",new JSONObject().put("schema","reference-confirmation-trace-143-v1").put("ordinal",++ordinal).put("enabled",enabled).put("memorySha256",search.memoryHash).put("importedEnabled",imported).put("automaticAdmissions",0).put("baselineAccepted",baseAccepted).put("lossObserved",loss).put("baseline",base).put("withheld",out.isNull("box")).put("search",counts).put("maximumConfirmations",maximum).put("hypotheses",hypotheses).put("recovery",recovery).put("scope","Each immutable verified appearance must independently earn three adjacent motion confirmations. Every reference can veto a competing location. No pooled confirmations, identity certification, target admission or baseline reset."));
    }
}
