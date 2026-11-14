package fraymus.workspace;

import java.awt.image.BufferedImage;
import java.util.*;
import org.json.*;

/** Outcome labels cannot guide source replay. Additional boxes and filter acceptance are distinct. */
final class ConfirmationAudit143 {
    interface Images { BufferedImage read(int ordinal)throws Exception; }
    static boolean uses(String p){return Set.of("reference-confirmation-143","reference-monitor-143","reference-seed-143").contains(p);}
    static JSONObject check(JSONObject run,JSONObject dataset,JSONObject labels,String body,Images images)throws Exception{
        String policy=ObservationMethods129.policy(run);if(!uses(policy))throw new IllegalArgumentException("Recovery policy required");
        JSONObject seed=null;for(Object raw:dataset.getJSONArray("objects")){var o=(JSONObject)raw;if(o.getString("key").equals(labels.getString("objectKey")))seed=o;}
        if(seed==null)throw new IllegalStateException("Missing original object seed");
        var tracker=new AppearanceConfirmation143(body,seed.getString("label"),images.read(0),PixelTracker.Box.from(seed.getJSONArray("box")),run.getJSONObject("referenceMemory137"),!policy.equals("reference-monitor-143"),!policy.equals("reference-seed-143"));
        var labelMap=new HashMap<Integer,JSONObject>();for(Object raw:labels.getJSONArray("rows")){var l=(JSONObject)raw;labelMap.put(l.getInt("ordinal"),l);}
        int additional=0,correct=0,wrong=0,unknown=0,accepted=0,acceptedCorrect=0,acceptedWrong=0,acceptedUnknown=0,preserved=0;var scores=new JSONArray();
        for(int i=0;i<run.getJSONArray("trace").length();i++){
            JSONObject observed=null;for(Object raw:run.getJSONArray("trace").getJSONObject(i).getJSONArray("objects")){var o=(JSONObject)raw;if(o.getString("bodyId").equals(body))observed=o;}
            if(observed==null)throw new IllegalStateException("Missing recovery tracker");
            var actual=ParsedJson137.copy(observed.getJSONObject("tracker"));var expected=tracker.update(images.read(i+1));
            for(String k:List.of("id","label")){actual.remove(k);expected.remove(k);}
            // Nested baseline names are also generated from the supplied source name.
            for(var t:List.of(actual,expected)){var b=t.getJSONObject("references143").getJSONObject("baseline");for(String k:List.of("id","label"))b.remove(k);}
            if(!expected.similar(actual))throw new IllegalStateException("Original recovery decision or pixel replay differs at "+(i+1));
            var t=observed.getJSONObject("tracker");var m=t.getJSONObject("references143");if(m.getBoolean("baselineAccepted"))preserved++;
            var l=labelMap.get(i+1);boolean extra=m.getJSONObject("recovery").getBoolean("accepted");if(!extra)continue;
            additional++;String v=l==null?"uncertain":l.getString("visibility");boolean visible=Set.of("visible","partial").contains(v),hidden=Set.of("occluded","out-of-view").contains(v);
            double overlap=visible?PixelTracker.iou(PixelTracker.Box.from(t.getJSONArray("box")),PixelTracker.Box.from(l.getJSONArray("targetBox"))):0;
            String outcome=visible&&overlap>=.5?"correct":visible||hidden?"incorrect":"unknown";
            if(outcome.equals("correct"))correct++;else if(outcome.equals("incorrect"))wrong++;else unknown++;
            boolean filter=observed.has("estimate")&&observed.getJSONObject("estimate").getBoolean("accepted");
            if(filter){accepted++;if(outcome.equals("correct"))acceptedCorrect++;else if(outcome.equals("incorrect"))acceptedWrong++;else acceptedUnknown++;}
            scores.put(new JSONObject().put("ordinal",i+1).put("visibility",v).put("targetOverlap",overlap).put("outcome",outcome).put("filterAccepted",filter));
        }
        return new JSONObject().put("schema","reference-confirmation-audit-143-v1").put("replayedFrames",run.getJSONArray("trace").length()).put("baselineAcceptedPreserved",preserved).put("automaticAdmissions",0)
            .put("additional",new JSONObject().put("returned",additional).put("correct",correct).put("incorrect",wrong).put("unknown",unknown))
            .put("filtered",new JSONObject().put("accepted",accepted).put("correct",acceptedCorrect).put("incorrect",acceptedWrong).put("unknown",acceptedUnknown)).put("scores",scores)
            .put("scope","Original pixels replay every decision before independent scoring. Baseline139 accepted tracker proposals remain unchanged; the shared filter can still refuse supplemental boxes. No semantic identity or automatic admission.");
    }
}
