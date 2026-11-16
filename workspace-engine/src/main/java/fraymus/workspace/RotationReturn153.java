package fraymus.workspace;

import java.awt.image.BufferedImage;
import java.util.*;
import org.json.*;

/** Independent experimental observations; never writes the active world or appearance bank. */
final class RotationReturn153 {
    private static final class Lane {FlowTracker flow;PixelTracker.Box last;int count;void clear(){flow=null;last=null;count=0;}}
    private final RotationSearch153 search;
    private final Map<String,Lane> lanes=new LinkedHashMap<>();
    private boolean ambiguity;
    private int ordinal;
    RotationReturn153(BufferedImage first,PixelTracker.Box seed,JSONObject memory,RotationSearch153.Arithmetic arithmetic){
        search=new RotationSearch153(first,seed,memory,arithmetic);
        for(var m:search.search(first))if(m.score().getBoolean("strong")&&(!m.unique()||PixelTracker.iou(m.box(),seed)<.25))ambiguity=true;
    }
    JSONObject update(BufferedImage image){
        ordinal++;var matches=search.search(image);var present=new HashSet<String>();
        for(var m:matches)if(m.score().getBoolean("strong")){
            if(!m.unique())ambiguity=true;
            for(var other:matches)if(other.score().getBoolean("strong")&&PixelTracker.iou(m.box(),other.box())<.25)ambiguity=true;
        }
        var hypotheses=new JSONArray();JSONObject chosen=null;
        for(var match:matches){present.add(match.key());var lane=lanes.computeIfAbsent(match.key(),k->new Lane());
            boolean eligible=!ambiguity&&match.unique()&&match.score().getBoolean("strong"),coherent=false;var motion=new JSONObject();
            if(eligible&&lane.flow!=null&&PixelTracker.iou(lane.last,match.box())>=.55){
                var flow=lane.flow.update(image);motion=flow.getJSONObject("motion");
                coherent=!flow.isNull("box")&&"pyramidal-flow".equals(motion.getString("method"))&&!motion.getBoolean("ambiguous")&&motion.getInt("inliers")>=8&&motion.getDouble("medianForwardBackwardPixels")<=1.2&&PixelTracker.iou(PixelTracker.Box.from(flow.getJSONArray("box")),match.box())>=.7;
            }
            if(!eligible)lane.clear();else{if(!coherent)lane.clear();lane.count=Math.min(4,lane.count+1);lane.last=match.box();lane.flow=new FlowTracker("rotation153","Original reference",image,match.box());}
            boolean accepted=eligible&&lane.count>=4;var h=match.json().put("eligible",eligible).put("coherentMotion",coherent).put("motion",motion).put("confirmations",lane.count).put("accepted",accepted);hypotheses.put(h);
            if(accepted&&(chosen==null||h.getDouble("score")>chosen.getDouble("score")))chosen=h;
        }
        for(var entry:lanes.entrySet())if(!present.contains(entry.getKey()))entry.getValue().clear();
        return new JSONObject().put("ordinal",ordinal).put("box",chosen==null?JSONObject.NULL:chosen.getJSONArray("box")).put("chosen",chosen==null?JSONObject.NULL:chosen)
            .put("hypotheses",hypotheses).put("ambiguityRetained",ambiguity).put("evaluations",search.evaluations()).put("requiredConfirmations",4).put("automaticAdmissions",0).put("physicalActionAuthorized",false);
    }
}
