package fraymus.workspace;

import java.awt.image.BufferedImage;
import java.util.*;
import org.json.*;

/** Replays original pixels, then scores warnings against downstream external labels. */
final class ForegroundAudit139 {
    interface Images { BufferedImage read(int ordinal)throws Exception; }
    static boolean uses(String policy){return Set.of("foreground-guard-139","monitor-only-139").contains(policy);}
    static JSONObject check(JSONObject run,JSONObject dataset,JSONObject labels,String body,Images images)throws Exception{
        String policy=ObservationMethods129.policy(run);boolean enabled=policy.equals("foreground-guard-139");
        if(!uses(policy))throw new IllegalArgumentException("Drift policy required");
        JSONObject seed=null;for(Object raw:dataset.getJSONArray("objects")){var o=(JSONObject)raw;if(o.getString("key").equals(labels.getString("objectKey")))seed=o;}
        if(seed==null)throw new IllegalStateException("Missing original object seed");
        var tracker=new ForegroundGuard139(body,seed.getString("label"),images.read(0),PixelTracker.Box.from(seed.getJSONArray("box")),enabled);
        var labelMap=new HashMap<Integer,JSONObject>();for(Object raw:labels.getJSONArray("rows")){var l=(JSONObject)raw;labelMap.put(l.getInt("ordinal"),l);}
        int warnings=0,useful=0,falseWarnings=0,missed=0,unknown=0,hidden=0,recovered=0;var scores=new JSONArray();
        for(int i=0;i<run.getJSONArray("trace").length();i++){
            var frame=run.getJSONArray("trace").getJSONObject(i);JSONObject observed=null;
            for(Object raw:frame.getJSONArray("objects")){var o=(JSONObject)raw;if(o.getString("bodyId").equals(body))observed=o.getJSONObject("tracker");}
            if(observed==null)throw new IllegalStateException("Missing drift tracker");
            var expected=tracker.update(images.read(i+1));
            // Object naming is presentation; every algorithm output and evidence field is exact.
            var actual=ParsedJson137.copy(observed);for(String k:List.of("id","label")){actual.remove(k);expected.remove(k);}
            if(!expected.similar(actual))throw new IllegalStateException("Original drift decision or pixel replay differs at "+(i+1));
            var m=observed.getJSONObject("drift139");if(m.getBoolean("suspect"))warnings++;
            if(m.getJSONObject("recovery").getBoolean("accepted")&&enabled)recovered++;
            var l=labelMap.get(i+1);if(l==null)continue;String v=l.getString("visibility");boolean visible=Set.of("visible","partial").contains(v),lost=Set.of("occluded","out-of-view").contains(v);
            var proposal=m.getJSONObject("proposal");boolean proposed=!proposal.isNull("box"),suspect=m.getBoolean("suspect");double overlap=0;
            if(proposed&&visible)overlap=PixelTracker.iou(PixelTracker.Box.from(proposal.getJSONArray("box")),PixelTracker.Box.from(l.getJSONArray("targetBox")));
            boolean correct=visible&&proposed&&overlap>=.5;
            if(suspect){if(correct)falseWarnings++;else if(visible||lost)useful++;else unknown++;}
            if(proposed&&!suspect&&(lost||visible&&!correct))missed++;
            if(proposed&&lost)hidden++;
            scores.put(new JSONObject().put("ordinal",i+1).put("visibility",v).put("proposalAccepted",proposed).put("proposalOverlap",overlap)
                .put("warning",suspect).put("withheld",m.getBoolean("withheld")).put("reason",m.getJSONArray("reasons")));
        }
        return new JSONObject().put("schema","foreground-audit-139-v1").put("enabled",enabled).put("replayedFrames",run.getJSONArray("trace").length())
            .put("summary",new JSONObject().put("warnings",warnings).put("usefulWarnings",useful).put("falseWarnings",falseWarnings).put("unknownWarnings",unknown)
                .put("missedWrongProposals",missed).put("hiddenProposals",hidden).put("confirmedRecoveries",recovered).put("automaticAdmissions",0))
            .put("scores",scores).put("scope","Original-pixel algorithm replay checked before scoring. Warning usefulness uses independent target/visibility labels; false warnings and missed wrong proposals remain explicit. Confirmed recovery is internal appearance evidence; enclosing outcome audit determines correctness.");
    }
}
