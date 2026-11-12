package fraymus.workspace;
import org.junit.Test;
import static org.junit.Assert.*;
import org.json.*;
import java.util.*;

/** Declared arithmetic fixtures; native timings are exercised separately. */
public class GameCostReview125Test {
    static JSONObject decision(){return new JSONObject().put("selected","direct").put("allowance",10).put("alternatives",new JSONArray().put(new JSONObject().put("strategy","full").put("requiredSecondsPerPickup",20)));}
    static JSONObject cost(){return new JSONObject().put("outcomeSafe",true).put("terminal",new JSONObject().put("seconds",12).put("comparable",true));}
    static JSONArray samples(){var a=new JSONArray();for(int i=0;i<3;i++){a.put(sample("direct",10+i,0));a.put(sample("full",14+i,1));}return a;}
    static JSONObject sample(String strategy,double seconds,int pickups){return new JSONObject().put("id",UUID.randomUUID().toString()).put("strategy",strategy).put("seconds",seconds).put("pickups",pickups).put("safe",true);}
    @Test public void comparesIndependentRangesAndCountsDecisionRunExcess(){var r=GameCostReview125.analyze(decision(),cost(),samples());assertTrue(r.getBoolean("established"));assertEquals(2,r.getDouble("pairedExtraSecondsLower"),0);assertEquals(6,r.getDouble("pairedExtraSecondsUpper"),0);assertEquals(2,r.getDouble("decisionAndRunExcessSeconds"),0);assertEquals(8,r.getDouble("observedSecondsPerPickupUpper"),0);assertEquals(12,r.getDouble("predictionMinusObservedUpperSeconds"),0);assertEquals("supported by these later comparisons",r.getString("missedOpportunity"));}
    @Test public void separatesInsufficientFailedAndZeroBenefit(){assertFalse(GameCostReview125.analyze(decision(),cost(),new JSONArray()).getBoolean("established"));var a=samples();a.getJSONObject(0).put("safe",false);assertFalse(GameCostReview125.analyze(decision(),cost(),a).getBoolean("established"));a=samples();for(Object x:a)((JSONObject)x).put("pickups",1);var r=GameCostReview125.analyze(decision(),cost(),a);assertTrue(r.getBoolean("established"));assertFalse(r.has("observedSecondsPerPickupUpper"));assertEquals("no consistent extra benefit",r.getString("missedOpportunity"));}
    @Test public void recoveredOrFailedOriginalCannotCertifyMissedOpportunity(){var c=cost();c.getJSONObject("terminal").put("comparable",false);assertFalse(GameCostReview125.analyze(decision(),c,samples()).getBoolean("established"));c=cost().put("outcomeSafe",false);assertFalse(GameCostReview125.analyze(decision(),c,samples()).getBoolean("established"));}
    @Test public void doesNotClaimGainWhenCostExceedsAllowanceOrAlternativeAlreadyChosen(){var d=decision().put("allowance",4);assertEquals("extra cost did not fit the allowance",GameCostReview125.analyze(d,cost(),samples()).getString("missedOpportunity"));d.put("selected","full");assertEquals("optional strategy was selected",GameCostReview125.analyze(d,cost(),samples()).getString("missedOpportunity"));}
    @Test public void rejectsDuplicateMalformedSamples(){var a=samples();a.put(a.getJSONObject(0));assertThrows(IllegalArgumentException.class,()->GameCostReview125.analyze(decision(),cost(),a));var b=samples();b.getJSONObject(0).put("seconds",-1);assertThrows(IllegalArgumentException.class,()->GameCostReview125.analyze(decision(),cost(),b));}
}
