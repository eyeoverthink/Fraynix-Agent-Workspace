package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ContinuityReturn154Test {
    private static final PixelTracker.Box BOX=new PixelTracker.Box(.2,.2,.2,.3);
    private static JSONObject original(){return new JSONObject().put("estimate",new JSONObject().put("accepted",true))
        .put("tracker",new JSONObject().put("box",BOX.json()).put("status","tracked").put("motion",new JSONObject()
            .put("method","pyramidal-flow").put("ambiguous",false).put("inliers",12).put("medianForwardBackwardPixels",.2)));}
    private static JSONObject rotation(int ordinal,boolean confirmed){
        var h=new JSONObject().put("sourceKey","initial-seed").put("score",.98).put("strong",true).put("unique",true).put("box",BOX.json()).put("confirmations",confirmed?4:ordinal).put("accepted",confirmed);
        return new JSONObject().put("ordinal",ordinal).put("box",confirmed?BOX.json():JSONObject.NULL).put("chosen",confirmed?h:JSONObject.NULL)
            .put("ambiguityRetained",false).put("hypotheses",new JSONArray().put(h));
    }
    @Test public void continuousOriginalBoxesSurviveConfirmationDelayExactly(){
        var selector=new ContinuityReturn154(BOX);
        for(int i=1;i<=6;i++){var out=selector.update(original(),rotation(i,i>=4));assertEquals("original-continuity",out.getString("origin"));assertTrue(BOX.json().similar(out.getJSONArray("box")));assertTrue(out.getBoolean("continuityPreserved"));}
    }
    @Test public void lossClosesOriginalSegmentAndCannotBeBypassedByItsReturn(){
        var selector=new ContinuityReturn154(BOX);selector.update(original(),rotation(1,false));
        var lost=original();lost.getJSONObject("tracker").put("box",JSONObject.NULL).put("status","lost");lost.getJSONObject("estimate").put("accepted",false);
        assertEquals("withheld",selector.update(lost,rotation(2,false)).getString("origin"));
        assertEquals("withheld",selector.update(original(),rotation(3,false)).getString("origin"));
        assertEquals("confirmed-return-153",selector.update(original(),rotation(4,true)).getString("origin"));
        assertEquals("withheld",selector.update(original(),rotation(5,false)).getString("origin"));
    }
    @Test public void predictionsPoorMotionWeakAppearanceRivalsAndConflictsCannotPreserveContinuity(){
        for(String kind:new String[]{"prediction","reacquired","fallback","inliers","forward-backward","ambiguous","weak","copy","conflict","displacement"}){
            var seed=kind.equals("displacement")?new PixelTracker.Box(.7,.6,.2,.3):BOX;var selector=new ContinuityReturn154(seed);var o=original();var t=o.getJSONObject("tracker");var r=rotation(1,false);var h=r.getJSONArray("hypotheses").getJSONObject(0);
            switch(kind){
                case "prediction"->o.getJSONObject("estimate").put("accepted",false);
                case "reacquired"->t.put("status","reacquired");
                case "fallback"->t.getJSONObject("motion").put("method","appearance-recovery");
                case "inliers"->t.getJSONObject("motion").put("inliers",7);
                case "forward-backward"->t.getJSONObject("motion").put("medianForwardBackwardPixels",1.21);
                case "ambiguous"->t.getJSONObject("motion").put("ambiguous",true);
                case "weak"->h.put("strong",false);
                case "copy"->r.put("ambiguityRetained",true);
                case "conflict"->h.put("box",new PixelTracker.Box(.7,.6,.2,.3).json());
            }
            assertTrue(kind,selector.update(o,r).isNull("box"));assertFalse(selector.update(original(),rotation(2,false)).getBoolean("originalSegmentOpen"));
        }
    }
    @Test public void unconfirmedAndOutOfOrderReturnEvidenceRefuses(){
        var selector=new ContinuityReturn154(BOX);assertThrows(IllegalArgumentException.class,()->selector.update(original(),rotation(2,false)));
        var bad=rotation(1,true);bad.getJSONObject("chosen").put("confirmations",3);
        assertThrows(IllegalStateException.class,()->new ContinuityReturn154(BOX).update(original(),bad));
    }
}
