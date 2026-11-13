package fraymus.workspace;

import java.awt.image.BufferedImage;
import java.util.*;
import org.json.*;

/** Corroborated drift checks; source-qualified restoration is bounded and never learns predictions. */
final class CorroboratedGuard140 implements ImageTracker {
    private final String id,label;
    private final int width,height;
    private final boolean enabled;
    private final AppearanceEvidence138 anchor;
    private final SpatialEvidence139 spatial;
    private final RestorationGate140 restoration=new RestorationGate140();
    private final Map<String,Integer> streaks=new TreeMap<>();
    private FlowTracker flow;
    private PixelTracker.Box lastAccepted,previousProposal,pending;
    private boolean quarantined;
    private int ordinal;
    CorroboratedGuard140(String id,String label,BufferedImage first,PixelTracker.Box seed,boolean enabled){
        this.id=id;this.label=label;this.enabled=enabled;width=first.getWidth();height=first.getHeight();
        anchor=new AppearanceEvidence138(first,seed);spatial=new SpatialEvidence139(first,seed);
        flow=new FlowTracker(id,label,first,seed);lastAccepted=previousProposal=seed;
    }
    static boolean corroborates(JSONObject structure){
        int eligible=structure.getInt("eligibleParts"),direct=structure.getInt("directMatches");
        return eligible>=4&&direct<Math.ceil(.45*eligible)&&structure.getDouble("structuralAgreement")<.4;
    }
    public JSONObject update(BufferedImage image){
        if(image.getWidth()!=width||image.getHeight()!=height)throw new IllegalArgumentException("Original image dimensions changed");
        var proposal=flow.update(image);var output=ParsedJson137.copy(proposal);boolean present=!proposal.isNull("box");
        var box=present?PixelTracker.Box.from(proposal.getJSONArray("box")):null;
        var small=PixelTracker.working(image);var appearance=new JSONObject();var hard=new JSONArray();
        if(present){
            appearance=anchor.assess(small,box);double score=appearance.getDouble("correlation"),rival=score>=.7?anchor.rival(small,box):-1;
            appearance.put("rivalScore",rival).put("rivalChecked",score>=.7).put("ambiguous",score>=.7&&rival>=Math.max(.78,score-.05));
            hard=AppearanceEvidence138.reasons(appearance);
        }
        var structure=spatial.assess(image,box);var advisory=structure.getJSONArray("candidateReasons");
        boolean corroborated=corroborates(structure);var reasons=new JSONArray(hard.toString());var current=new HashSet<String>();
        for(Object raw:advisory){String reason=(String)raw;if(!corroborated)continue;current.add(reason);int count=streaks.getOrDefault(reason,0)+1;streaks.put(reason,count);if(count>=3)reasons.put(reason);}
        streaks.keySet().removeIf(k->!current.contains(k));structure.put("corroborated",corroborated).put("consecutiveEvidence",new JSONObject(streaks));
        boolean suspect=present&&!reasons.isEmpty(),wasQuarantined=quarantined;
        var motion=proposal.getJSONObject("motion");
        boolean continuous=present&&hard.isEmpty()&&"pyramidal-flow".equals(motion.getString("method"))&&!motion.getBoolean("ambiguous")
            &&previousProposal!=null&&PixelTracker.iou(previousProposal,box)>=.55;
        if(!present||suspect){
            if(!quarantined)restoration.begin(present&&hard.isEmpty()&&continuous);
            quarantined=true;
        }
        var recovery=new JSONObject().put("attempted",false).put("accepted",false).put("mode","none");
        boolean withheld=enabled&&quarantined;
        if(quarantined){
            recovery.put("attempted",true);
            boolean supported=present&&!suspect&&structure.getInt("eligibleParts")>=5&&structure.getInt("directMatches")>=Math.ceil(.6*structure.getInt("eligibleParts"))
                &&structure.getInt("inlierParts")>=5&&structure.getBoolean("supported")&&structure.getDouble("alignmentError")<.12
                &&appearance.getDouble("correlation")>=.7&&appearance.getDouble("colorAgreement")>=.9
                &&appearance.getDouble("rivalScore")<appearance.getDouble("correlation")-.08
                &&motion.getInt("inliers")>=8&&motion.getDouble("medianForwardBackwardPixels")<=1.2;
            boolean restored=restoration.observe(continuous,wasQuarantined&&supported,box);
            recovery.put("continuity",restoration.evidence().put("sourceSupported",supported));
            if(restored){
                quarantined=false;pending=null;recovery.put("mode","source-supported-continuity").put("accepted",true).put("box",box.json());
                if(enabled){output.put("status","restored");withheld=false;}
            }else{
                var found=anchor.search(small);var spatialImage=spatial.currentImage();
                if(present&&structure.getBoolean("supported")&&structure.getDouble("alignmentError")<.12&&structure.getInt("eligibleParts")>=3){
                    double score=appearance.getDouble("correlation"),rival=anchor.rival(small,box);
                    if(score>=.88&&rival<score-.05&&hard.isEmpty()&&spatial.supports(spatialImage,box))found=new AppearanceEvidence138.Search(box,score,rival,true);
                }
                var source=anchor.assess(small,found.box());boolean strong=found.unique()&&AppearanceEvidence138.reasons(source).isEmpty()&&spatial.supports(spatialImage,found.box());
                double overlap=pending==null?0:PixelTracker.iou(pending,found.box());boolean confirmed=wasQuarantined&&strong&&pending!=null&&overlap>=.55;
                recovery.put("mode","strict-original-source").put("unique",found.unique()).put("score",found.score()).put("rivalScore",found.rival())
                    .put("box",found.box().json()).put("appearance",source).put("strong",strong).put("confirmationOverlap",overlap).put("accepted",confirmed);
                if(confirmed){quarantined=false;pending=null;if(enabled){flow=new FlowTracker(id,label,image,found.box());output.put("box",found.box().json()).put("lastKnownBox",found.box().json()).put("status","reacquired").put("matchScore",found.score()).put("motion",new JSONObject().put("method","immutable-source-recovery-140"));withheld=false;}}
                else pending=strong?found.box():null;
            }
        }else pending=null;
        if(enabled&&withheld)output.put("box",JSONObject.NULL).put("lastKnownBox",lastAccepted.json()).put("status",suspect?"drift-suspected":"withheld");
        if(!output.isNull("box"))lastAccepted=PixelTracker.Box.from(output.getJSONArray("box"));
        previousProposal=box;
        return output.put("drift140",new JSONObject().put("schema","corroborated-guard-trace-140-v1").put("ordinal",++ordinal).put("enabled",enabled)
            .put("anchorSha256",anchor.hash).put("automaticAdmissions",0).put("proposal",proposal).put("appearance",appearance).put("structure",structure)
            .put("advisoryReasons",advisory).put("reasons",reasons).put("suspect",suspect).put("withheld",withheld).put("quarantined",quarantined).put("recovery",recovery)
            .put("scope","Advisory disagreement is not confirmed drift. Bounded source corroboration is not semantic identity; independent labels verify outcomes."));
    }
}
