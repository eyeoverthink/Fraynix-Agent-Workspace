package fraymus.workspace;

import org.json.*;

/** Combines original measured continuity with independently confirmed returns. No labels. */
final class ContinuityReturn154 {
    private boolean originalSegment=true;
    private PixelTracker.Box previous;
    private int ordinal;

    ContinuityReturn154(PixelTracker.Box seed){previous=seed;}

    JSONObject update(JSONObject original,JSONObject rotation){
        if(rotation.getInt("ordinal")!=++ordinal)throw new IllegalArgumentException("Complete ordered rotation trace required");
        var tracker=original.getJSONObject("tracker");var motion=tracker.optJSONObject("motion");
        boolean measured=original.getJSONObject("estimate").getBoolean("accepted")&&!tracker.isNull("box");
        PixelTracker.Box box=measured?PixelTracker.Box.from(tracker.getJSONArray("box")):null;
        boolean flow=motion!=null&&"pyramidal-flow".equals(motion.optString("method"))&&!motion.optBoolean("ambiguous",true)
            &&motion.optInt("inliers",0)>=8&&motion.optDouble("medianForwardBackwardPixels",Double.POSITIVE_INFINITY)<=1.2;
        JSONObject support=null;
        if(measured&&!rotation.getBoolean("ambiguityRetained"))for(Object raw:rotation.getJSONArray("hypotheses")){
            var h=(JSONObject)raw;
            if(h.getBoolean("strong")&&h.getBoolean("unique")&&PixelTracker.iou(box,PixelTracker.Box.from(h.getJSONArray("box")))>=.7
                &&(support==null||h.getDouble("score")>support.getDouble("score")))support=h;
        }
        boolean continuity=originalSegment&&measured&&"tracked".equals(tracker.getString("status"))&&flow&&support!=null&&PixelTracker.iou(previous,box)>=.55;
        if(!continuity)originalSegment=false;else previous=box;
        boolean returned=!rotation.isNull("box");
        if(returned&&(rotation.isNull("chosen")||rotation.getBoolean("ambiguityRetained")||!rotation.getJSONObject("chosen").getBoolean("accepted")
            ||rotation.getJSONObject("chosen").getInt("confirmations")<4))throw new IllegalStateException("Unconfirmed return cannot be combined");
        String origin=continuity?"original-continuity":returned?"confirmed-return-153":"withheld";
        return new JSONObject().put("ordinal",ordinal).put("box",continuity?box.json():returned?new JSONArray(rotation.getJSONArray("box").toString()):JSONObject.NULL)
            .put("origin",origin).put("originalAccepted",measured).put("originalStatus",tracker.getString("status"))
            .put("originalSegmentOpen",originalSegment).put("continuityPreserved",continuity).put("continuityFlowPassed",flow)
            .put("supportKey",support==null?JSONObject.NULL:support.getString("sourceKey"))
            .put("rotation",new JSONObject(rotation.toString())).put("automaticAdmissions",0).put("physicalActionAuthorized",false);
    }
}
