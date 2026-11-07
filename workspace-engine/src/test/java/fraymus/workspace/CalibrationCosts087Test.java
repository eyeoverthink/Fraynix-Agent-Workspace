package fraymus.workspace;

import java.time.*;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class CalibrationCosts087Test {
    static final Instant NOW=Instant.parse("2026-11-06T12:00:00Z");
    static JSONObject shape(){return new JSONObject().put("code","code").put("profile",new JSONObject().put("cpus",4)).put("pairs",6).put("bytes",10000).put("family","clear-three").put("warm",true).put("workload",new JSONObject().put("retain",false).put("skill",false));}
    static JSONArray samples(){var out=new JSONArray();for(String kind:new String[]{"fit","check"})for(int i=0;i<3;i++)out.put(new JSONObject().put("id",kind+i).put("kind",kind).put("at",NOW.minusSeconds(i).toString()).put("millis",kind.equals("fit")?10000+i*1000:100+i*10).put("shape",shape()));return out;}
    @Test public void measuredMaxAndSpreadBoundBothAllowances(){var r=CalibrationCosts087.estimate(samples(),shape(),NOW);assertTrue(r.getBoolean("supported"));assertEquals(14000,r.getDouble("fitBudgetMillis"),0);assertEquals(140,r.getDouble("checkBudgetMillis"),0);}
    @Test public void threeOfEachAreRequired(){var s=samples();s.remove(0);assertFalse(CalibrationCosts087.estimate(s,shape(),NOW).getBoolean("supported"));}
    @Test public void smallerEvidenceNeverLowersAllowance(){assertEquals(14000,CalibrationCosts087.estimate(samples(),shape().put("bytes",8000),NOW).getDouble("fitBudgetMillis"),0);}
    @Test public void largerNearbyEvidenceScalesUp(){assertEquals(17500,CalibrationCosts087.estimate(samples(),shape().put("bytes",12500),NOW).getDouble("fitBudgetMillis"),0);}
    @Test public void distantVolumeFallsBack(){for(int b:new int[]{0,7999,12501})assertFalse(CalibrationCosts087.estimate(samples(),shape().put("bytes",b),NOW).getBoolean("supported"));}
    @Test public void noCrossWorkloadOrRestartTransfer(){for(String k:new String[]{"code","family","pairs","profile","warm","workload"}){var target=shape();target.put(k,k.equals("warm")?false:k.equals("pairs")?4:k.equals("profile")?new JSONObject().put("cpus",2):k.equals("workload")?new JSONObject().put("retain",true).put("skill",true):"different");assertFalse(k,CalibrationCosts087.estimate(samples(),target,NOW).getBoolean("supported"));}}
    @Test public void staleAndFutureSamplesAreExcluded(){for(String t:new String[]{NOW.minus(Duration.ofDays(15)).toString(),NOW.plusSeconds(1).toString()}){var s=samples();s.getJSONObject(0).put("at",t);assertFalse(CalibrationCosts087.estimate(s,shape(),NOW).getBoolean("supported"));}}
    @Test public void duplicateReadsCannotCreateRepetitions(){var s=samples();s.getJSONObject(0).put("id","fit1");assertFalse(CalibrationCosts087.estimate(s,shape(),NOW).getBoolean("supported"));}
    @Test public void newestTwelveBoundDriftHistory(){var s=samples();for(int i=3;i<20;i++)for(String k:new String[]{"fit","check"})s.put(new JSONObject().put("id",k+i).put("kind",k).put("at",NOW.toString()).put("millis",k.equals("fit")?10000:100).put("shape",shape()));var r=CalibrationCosts087.estimate(s,shape(),NOW);assertEquals(12,r.getInt("fitSamples"));assertEquals(12,r.getInt("checkSamples"));assertEquals(11000,r.getDouble("fitBudgetMillis"),0);}
    @Test public void slowerNewRunRaisesAllowance(){var s=samples();s.getJSONObject(0).put("millis",30000);assertEquals(49000,CalibrationCosts087.estimate(s,shape(),NOW).getDouble("fitBudgetMillis"),0);}
    static JSONObject stats(long n,long fail,long b,double ms){return new JSONObject().put("mode","parallel").put("loads",n).put("failures",fail).put("verifiedBytes",b).put("loadMillis",ms);}
    @Test public void completeCounterIntervalIncludesEveryLoad(){var r=CalibrationCosts087.delta(stats(2,0,1000,10),stats(9,0,8000,100));assertTrue(r.getBoolean("eligible"));assertEquals(7,r.getInt("loads"));assertEquals(90,r.getDouble("millis"),0);}
    @Test public void failureResetAndNoLoadsCannotTeach(){var b=stats(2,0,1000,10);for(var a:new JSONObject[]{stats(3,1,2000,20),stats(1,0,500,5),stats(2,0,1000,10),stats(3,0,2000,20).put("mode","reference")})assertFalse(CalibrationCosts087.delta(b,a).getBoolean("eligible"));}
    @Test public void referenceReaderCannotTeachOrUseNormalAllowances(){assertTrue(CalibrationCosts087.normalReader("parallel"));assertFalse(CalibrationCosts087.normalReader("reference"));assertFalse(CalibrationCosts087.normalReader("unknown"));assertFalse(CalibrationCosts087.delta(stats(0,0,0,0).put("mode","reference"),stats(5,0,5000,50).put("mode","reference")).getBoolean("eligible"));}
}
