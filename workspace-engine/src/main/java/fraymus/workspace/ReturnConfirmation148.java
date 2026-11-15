package fraymus.workspace;

import java.awt.image.BufferedImage;
import java.util.*;
import org.json.*;

/** Confirm each immutable appearance only while measured target and rival paths support it. */
final class ReturnConfirmation148 implements ImageTracker {
    private static final class Lane {
        final ReturnGate148 gate;
        PixelTracker.Box pending;
        FlowTracker continuity;
        int confirmations;
        Lane(PixelTracker.Box seed){gate=new ReturnGate148(seed);}
        void clear(){pending=null;continuity=null;confirmations=0;}
    }
    private final String id,label;
    private final boolean enabled;
    private final ReturnContinuity146 parent;
    private final ReturnSearch148 search;
    private final Map<String,Lane> lanes=new LinkedHashMap<>();

    ReturnConfirmation148(String id,String label,BufferedImage first,PixelTracker.Box seed,JSONObject memory,boolean enabled){
        this.id=id;this.label=label;this.enabled=enabled;
        parent=new ReturnContinuity146(id,label,first,seed,memory,true);
        search=new ReturnSearch148(first,seed,memory,true);
        lanes.put("initial-seed",new Lane(seed));
        for(Object raw:memory.getJSONArray("examples")){var e=(JSONObject)raw;lanes.put(e.getString("imageSha256")+":"+e.getInt("ordinal"),new Lane(seed));}
    }
    public JSONObject update(BufferedImage image){
        var previous=parent.update(image);var out=ParsedJson137.copy(previous);
        var refs=previous.getJSONObject("references143");boolean baseline=refs.getBoolean("baselineAccepted");
        var hypotheses=new JSONArray();var counts=new JSONObject();var locations=new JSONArray();
        JSONObject chosen=null;int maximum=0;
        if(baseline){
            for(var lane:lanes.values()){lane.clear();lane.gate.step(previous,locations,new JSONObject());}
        }else if(refs.getBoolean("lossObserved")){
            var batch=search.search(image);counts=batch.counts();locations=search.locations();
            for(var found:batch.candidates()){
                var lane=lanes.get(found.key());var evidence=found.json();
                var path=lane.gate.step(previous,locations,evidence);
                // A strong match alone cannot start or extend a confirmation lane.
                boolean eligible=found.score()>=.93&&path.getBoolean("qualified");
                double overlap=lane.pending==null?0:PixelTracker.iou(lane.pending,found.box());
                boolean coherent=false;var motion=new JSONObject();
                if(eligible&&lane.continuity!=null&&overlap>=.55){
                    var tracked=lane.continuity.update(image);motion=tracked.getJSONObject("motion");
                    coherent=!tracked.isNull("box")&&"pyramidal-flow".equals(motion.getString("method"))&&!motion.getBoolean("ambiguous")&&motion.getInt("inliers")>=8&&motion.getDouble("medianForwardBackwardPixels")<=1.2&&PixelTracker.iou(PixelTracker.Box.from(tracked.getJSONArray("box")),found.box())>=.7;
                }
                if(eligible){lane.confirmations=coherent?Math.min(3,lane.confirmations+1):1;lane.pending=found.box();lane.continuity=new FlowTracker(id,label,image,found.box());}else lane.clear();
                maximum=Math.max(maximum,lane.confirmations);boolean qualified=eligible&&lane.confirmations>=3;
                evidence.put("path",path).put("eligible",eligible).put("coherentMotion",coherent).put("motion",motion).put("confirmationOverlap",overlap).put("confirmations",lane.confirmations).put("requiredConfirmations",3).put("qualified",qualified);
                hypotheses.put(evidence);
                if(qualified&&(chosen==null||found.score()>chosen.getDouble("score")))chosen=evidence;
            }
        }else for(var lane:lanes.values())lane.clear();
        boolean additional=previous.isNull("box")&&chosen!=null,accepted=enabled&&additional;
        if(accepted)out.put("box",chosen.getJSONArray("box")).put("lastKnownBox",chosen.getJSONArray("box")).put("status","path-confirmed-148").put("matchScore",chosen.getDouble("score"));
        return out.put("confirmation148",new JSONObject().put("schema","return-confirmation-148-v1").put("enabled",enabled).put("parentAccepted",!previous.isNull("box")).put("baselineAccepted",baseline).put("maximumConfirmations",maximum).put("hypotheses",hypotheses).put("locations",locations).put("search",counts).put("wouldRestore",additional).put("restored",accepted).put("accepted",!out.isNull("box")).put("withheld",out.isNull("box")).put("chosen",chosen==null?JSONObject.NULL:chosen).put("automaticAdmissions",0).put("scope","Original146 proposals remain unchanged. Additional returns require three adjacent per-reference flow confirmations supported by original-baseline target motion and measured separation from every retained rival branch. No recovered anchor, pooled confirmation, automatic appearance admission or horizon extension. Same-path unseen copies remain unobservable; image-plane bounds are not calibrated identity certainty."));
    }
}
