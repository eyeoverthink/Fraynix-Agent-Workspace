package fraymus.workspace;

import java.awt.image.BufferedImage;
import org.json.*;

/** Absence of a rival is not evidence that a surviving appearance belongs to the target. */
final class AmbiguityGuard144 implements ImageTracker {
    private final AppearanceConfirmation143 parent;
    private final boolean enabled;
    private boolean ambiguous;
    private int firstAmbiguousOrdinal;

    AmbiguityGuard144(String id,String label,BufferedImage first,PixelTracker.Box seed,JSONObject memory,boolean enabled){
        parent=new AppearanceConfirmation143(id,label,first,seed,memory,true,true);
        this.enabled=enabled;
    }

    public JSONObject update(BufferedImage image){return decide(parent.update(image));}

    // Also permits direct tests of the state transition, separately from pixel-level integration.
    JSONObject decide(JSONObject proposal){
        var references=proposal.getJSONObject("references143");
        boolean baseline=references.getBoolean("baselineAccepted"),lost=references.getBoolean("lossObserved");
        boolean competing=false;var keys=new JSONArray();
        if(lost&&!baseline)for(Object raw:references.getJSONArray("hypotheses")){
            var h=(JSONObject)raw;
            if(!h.getBoolean("unique")&&h.getDouble("score")>=.93&&h.getDouble("rivalScore")>=h.getDouble("score")-.08){
                competing=true;keys.put(h.getString("sourceKey"));
            }
        }
        if(keys.length()>ReferenceMemory137.CAPACITY+1)throw new IllegalStateException("Ambiguity evidence capacity exceeded");
        boolean cleared=baseline&&ambiguous;
        if(baseline){ambiguous=false;firstAmbiguousOrdinal=0;}
        else if(competing&&!ambiguous){ambiguous=true;firstAmbiguousOrdinal=references.getInt("ordinal");}
        boolean parentAccepted=references.getJSONObject("recovery").getBoolean("accepted");
        boolean wouldBlock=ambiguous&&parentAccepted&&!baseline,blocked=enabled&&wouldBlock;
        var out=ParsedJson137.copy(blocked?references.getJSONObject("baseline"):proposal);
        out.put("references143",ParsedJson137.copy(references));
        return out.put("ambiguity144",new JSONObject().put("schema","ambiguity-continuity-144-v1")
            .put("enabled",enabled).put("competingNow",competing).put("competingSourceKeys",keys)
            .put("latched",ambiguous).put("firstAmbiguousOrdinal",firstAmbiguousOrdinal)
            .put("clearedByBaseline",cleared).put("parentAccepted",parentAccepted)
            .put("wouldBlock",wouldBlock).put("blocked",blocked).put("accepted",parentAccepted&&!blocked)
            .put("withheld",out.isNull("box")).put("automaticAdmissions",0)
            .put("reason",ambiguous?"Competing appearances observed during this loss; disappearance cannot establish identity":cleared?"Existing baseline continuity returned":"No competing appearance observed in this loss episode")
            .put("scope","Remember ambiguity until the existing baseline accepts again. No expiry, new identity claim, source admission or baseline reset. A previously unseen identical copy remains unresolved."));
    }
}
