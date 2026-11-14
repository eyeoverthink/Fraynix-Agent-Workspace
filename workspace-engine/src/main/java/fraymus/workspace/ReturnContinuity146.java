package fraymus.workspace;

import java.awt.image.BufferedImage;
import org.json.*;

/** A missing competitor warning is not positive evidence that a returning appearance is the target. */
final class ReturnContinuity146 implements ImageTracker {
    private final TrajectoryRecovery145 parent;
    private final boolean enabled;

    ReturnContinuity146(String id,String label,BufferedImage first,PixelTracker.Box seed,JSONObject memory,boolean enabled){
        parent=new TrajectoryRecovery145(id,label,first,seed,memory,true);
        this.enabled=enabled;
    }

    public JSONObject update(BufferedImage image){return decide(parent.update(image),enabled);}

    static JSONObject decide(JSONObject proposal,boolean enabled){
        var references=proposal.getJSONObject("references143");
        var trajectory=proposal.getJSONObject("trajectory145");
        var ambiguity=proposal.getJSONObject("ambiguity144");
        boolean baseline=references.getBoolean("baselineAccepted");
        boolean parentAccepted=trajectory.getBoolean("accepted");
        boolean checked=parentAccepted&&!baseline&&!ambiguity.getBoolean("latched");
        var target=trajectory.getJSONObject("target");
        boolean supported=target.optBoolean("valid")&&trajectory.getBoolean("targetCompatible")&&trajectory.getBoolean("scaleCompatible");
        boolean wouldBlock=checked&&!supported,blocked=enabled&&wouldBlock;
        String reason=baseline?"Existing baseline continuity preserved":!checked?
            "Existing trajectory and ambiguity decision preserved":!target.optBoolean("valid")?
            "Return lacks a current measured path: "+target.getString("reason"):!trajectory.getBoolean("targetCompatible")?
            "Returning appearance lies outside the measured pre-loss path":!trajectory.getBoolean("scaleCompatible")?
            "Returning appearance lies outside the measured size bounds":"Confirmed appearance also fits the measured pre-loss path";
        var out=ParsedJson137.copy(blocked?references.getJSONObject("baseline"):proposal);
        // Preserve the original parent decisions verbatim; the separate146 record describes the additional refusal.
        if(blocked)for(String key:new String[]{"references143","ambiguity144","trajectory145"})out.put(key,ParsedJson137.copy(proposal.getJSONObject(key)));
        return out.put("continuity146",new JSONObject().put("schema","return-continuity-146-v1")
            .put("enabled",enabled).put("baselineAccepted",baseline).put("parentAccepted",parentAccepted)
            .put("checkedWithoutWarning",checked).put("pathSupported",supported).put("wouldBlock",wouldBlock)
            .put("blocked",blocked).put("inheritedBlocked",trajectory.getBoolean("blocked"))
            .put("accepted",parentAccepted&&!blocked).put("withheld",out.isNull("box"))
            .put("automaticAdmissions",0).put("extraSearches",0).put("reason",reason)
            .put("scope","Reuse unchanged145 pre-loss baseline measurements and fixed image-plane bounds. No new search, automatic admission, baseline reset or self-confirming target anchor. Missing competitors are not identity proof. A same-path unseen copy, erroneous baseline, camera motion and long gaps remain unresolved."));
    }
}
