package fraymus.workspace;

import org.junit.Test;
import static org.junit.Assert.*;
import org.json.*;
import java.awt.image.BufferedImage;

public class TrackingEvaluationTest {
    @Test public void referencesAreDerivedFromMaskObjectIds(){var mask=new BufferedImage(80,60,BufferedImage.TYPE_BYTE_INDEXED);for(int y=12;y<35;y++)for(int x=17;x<39;x++)mask.getRaster().setSample(x,y,0,2);var box=TrackingEvaluation.truth(mask,2);assertEquals(17/80.,box.x(),1e-10);assertEquals(22/80.,box.w(),1e-10);assertNull(TrackingEvaluation.truth(mask,1));}
    @Test public void adaptiveProfileRejectsDuplicateAndAbsentAppearances() throws Exception {var guard=TrackingGuards.run(TrackingEvaluation.CANDIDATE);assertTrue(guard.toString(2),guard.getBoolean("duplicateAmbiguous"));assertTrue(guard.toString(2),guard.getBoolean("hiddenHasNoCurrentBox"));}
    @Test public void strongerDuplicateCounterexampleIsRetainedForBaseline() throws Exception {var guard=TrackingGuards.run(TrackingEvaluation.BASELINE);assertFalse("Frozen baseline accepts this stronger duplicate counterexample; retain its failure",guard.getBoolean("duplicateAmbiguous"));assertTrue(guard.toString(2),guard.getBoolean("hiddenHasNoCurrentBox"));}
    @Test public void allPredictionFailuresCountInMeasuredAccuracy(){var frames=new JSONArray();for(int i=0;i<4;i++)frames.put(new JSONObject().put("iou",i==0?.8:0).put("reference",new JSONArray(new double[]{0,0,.2,.2})).put("prediction",new JSONObject().put("status",i==0?"tracked":"lost").put("box",i==0?new JSONArray(new double[]{0,0,.2,.2}):JSONObject.NULL)).put("processingMillis",i+1));var m=TrackingEvaluation.metrics(frames);assertEquals(.2,m.getDouble("meanIou"),1e-10);assertEquals(.25,m.getDouble("iouHalfFraction"),1e-10);assertEquals(3,m.getInt("lost"));assertEquals(4,m.getInt("scoredFrames"));}
}
