package fraymus.workspace;

import java.awt.image.BufferedImage;
import org.json.*;
import java.util.*;

/** Withholds unsupported proposals before flow loss; recovery never teaches the immutable anchor. */
final class ForegroundGuard139 implements ImageTracker {
    private final String id,label;
    private final int width,height;
    private final boolean enabled;
    private final AppearanceEvidence138 anchor;
    private FlowTracker flow;
    private final SpatialEvidence139 spatial;
    private final Map<String,Integer> streaks=new TreeMap<>();
    private PixelTracker.Box lastAccepted,pending;
    private boolean quarantined;
    private int ordinal;
    ForegroundGuard139(String id,String label,BufferedImage first,PixelTracker.Box seed,boolean enabled){
        this.id=id;this.label=label;this.enabled=enabled;width=first.getWidth();height=first.getHeight();
        spatial=new SpatialEvidence139(first,seed);anchor=new AppearanceEvidence138(first,seed);flow=new FlowTracker(id,label,first,seed);lastAccepted=seed;
    }
    public JSONObject update(BufferedImage image){
        if(image.getWidth()!=width||image.getHeight()!=height)throw new IllegalArgumentException("Original image dimensions changed");
        var proposal=flow.update(image);var output=ParsedJson137.copy(proposal);boolean flowAccepted=!proposal.isNull("box");
        var small=PixelTracker.working(image);var evidence=new JSONObject();var reasons=new JSONArray();
        if(flowAccepted){
            var box=PixelTracker.Box.from(proposal.getJSONArray("box"));evidence=anchor.assess(small,box);
            double correlation=evidence.getDouble("correlation"),rival=correlation>=.7?anchor.rival(small,box):-1;
            evidence.put("rivalScore",rival).put("rivalChecked",correlation>=.7).put("ambiguous",correlation>=.7&&rival>=Math.max(.78,correlation-.05));
            reasons=AppearanceEvidence138.reasons(evidence);
        }
        var structure=spatial.assess(image,flowAccepted?PixelTracker.Box.from(proposal.getJSONArray("box")):null);
        var present=new HashSet<String>();for(Object raw:structure.getJSONArray("candidateReasons")){String reason=(String)raw;present.add(reason);int count=streaks.getOrDefault(reason,0)+1;streaks.put(reason,count);if(count>=2)reasons.put(reason);}
        streaks.keySet().removeIf(k->!present.contains(k));structure.put("consecutiveEvidence",new JSONObject(streaks));
        boolean suspect=flowAccepted&&!reasons.isEmpty(),wasQuarantined=quarantined;
        if(!flowAccepted||suspect)quarantined=true;
        var recovery=new JSONObject().put("attempted",false).put("accepted",false);
        boolean withheld=enabled&&quarantined;
        if(quarantined){
            recovery.put("attempted",true);var found=anchor.search(small);var spatialImage=spatial.currentImage();
            if(flowAccepted&&structure.getBoolean("supported")&&structure.getDouble("alignmentError")<.12&&structure.getInt("eligibleParts")>=3){
                var proposedBox=PixelTracker.Box.from(proposal.getJSONArray("box"));double score=evidence.getDouble("correlation"),rival=anchor.rival(small,proposedBox);
                if(score>=.88&&rival<score-.05&&AppearanceEvidence138.reasons(evidence).isEmpty()&&spatial.supports(spatialImage,proposedBox))found=new AppearanceEvidence138.Search(proposedBox,score,rival,true);
            }
            var recoveredEvidence=anchor.assess(small,found.box());
            boolean strong=found.unique()&&AppearanceEvidence138.reasons(recoveredEvidence).isEmpty()&&spatial.supports(spatialImage,found.box());
            double overlap=pending==null?0:PixelTracker.iou(pending,found.box());
            // A newly rejected frame cannot simultaneously certify recovery.
            boolean confirmed=wasQuarantined&&strong&&pending!=null&&overlap>=.55;
            recovery.put("unique",found.unique()).put("score",found.score()).put("rivalScore",found.rival()).put("box",found.box().json())
                .put("appearance",recoveredEvidence).put("strong",strong).put("confirmationOverlap",overlap).put("accepted",confirmed);
            if(confirmed){quarantined=false;pending=null;if(enabled){flow=new FlowTracker(id,label,image,found.box());output.put("box",found.box().json()).put("lastKnownBox",found.box().json()).put("status","reacquired").put("matchScore",found.score()).put("motion",new JSONObject().put("method","immutable-source-recovery-139"));withheld=false;}}
            else pending=strong?found.box():null;
        }else pending=null;
        if(enabled&&withheld)output.put("box",JSONObject.NULL).put("lastKnownBox",lastAccepted.json()).put("status",suspect?"drift-suspected":"withheld");
        if(!output.isNull("box"))lastAccepted=PixelTracker.Box.from(output.getJSONArray("box"));
        return output.put("drift139",new JSONObject().put("schema","foreground-guard-trace-139-v1").put("ordinal",++ordinal).put("enabled",enabled)
            .put("anchorSha256",anchor.hash).put("automaticAdmissions",0).put("proposal",proposal).put("appearance",evidence).put("reasons",reasons)
            .put("structure",structure).put("suspect",suspect).put("withheld",withheld).put("quarantined",quarantined).put("recovery",recovery)
            .put("scope","Appearance disagreement is a warning, not proven drift or semantic identity; independent labels score its value."));
    }
}
