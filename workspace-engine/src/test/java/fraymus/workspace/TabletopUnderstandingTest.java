package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class TabletopUnderstandingTest {
    static JSONObject facts(){return new JSONObject("{runId:'r',nativeBinding:{worldId:'w'},codeSha256:'test',status:'completed',ageMs:2000,relation:{interpolatedTapeMark:6.0,unit:'in'},tapeCheck:{expectedM:0.1016,measuredM:0.10447,absoluteErrorM:0.00287,passed:true},latest:{frame:{seconds:2,imageSha256:'example'},receivedAt:'2026-11-01T20:00:00Z',reference:{valid:true,status:'reference-stable'},tracker:{status:'observed'},estimate:{accepted:true,positionM:[0.1,0.2],velocityMps:[0.02,0],radiusM:0.012}}}");}
    @Test public void positionMotionAndScaleUseActualReceiptsAndQualifyOldSamples(){var f=facts();var p=TabletopUnderstanding.probe(f,"position");assertTrue(p.getString("answer").contains("100.0mm"));assertTrue(p.getString("answer").contains("6.00in"));assertTrue(p.getBoolean("recorded"));assertTrue(TabletopUnderstanding.probe(f,"motion").getString("answer").contains("20.0mm/s"));assertTrue(TabletopUnderstanding.probe(f,"calibration").getString("answer").contains("2.87mm"));var pred=TabletopUnderstanding.probe(f,"prediction");assertEquals(.104,pred.getJSONArray("constantVelocityForecastM").getDouble(0),1e-12);}
    @Test public void lossAndChangedReferenceNeverBecomeCurrentMeasuredKnowledge(){var f=facts().put("status","running").put("ageMs",10);f.getJSONObject("latest").getJSONObject("estimate").put("accepted",false);f.getJSONObject("latest").getJSONObject("tracker").put("status","lost");assertTrue(TabletopUnderstanding.probe(f,"visibility").getString("answer").contains("No accepted target measurement"));f.getJSONObject("latest").getJSONObject("reference").put("valid",false);assertTrue(TabletopUnderstanding.probe(f,"visibility").getString("answer").contains("unverified or changed"));assertThrows(IllegalArgumentException.class,()->TabletopUnderstanding.probe(f,"invent a measurement"));}
}
