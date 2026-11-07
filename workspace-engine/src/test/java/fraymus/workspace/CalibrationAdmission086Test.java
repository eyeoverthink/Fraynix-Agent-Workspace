package fraymus.workspace;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class CalibrationAdmission086Test {
    static JSONArray pairs(){var ps=TaskCost085Test.model().getJSONArray("pairs");for(Object raw:ps){var p=(JSONObject)raw;p.put("simpleMillis",10000).put("memoryMillis",1000);}return ps;}
    static JSONObject budget(){return CalibrationAdmission086.defaults().put("horizon",100).put("fitBudgetMillis",1000).put("checkBudgetMillis",100);}
    @Test public void profitableProposalRequiresReserveAndExcludesSunkWork(){var a=CalibrationAdmission086.assess(pairs(),budget());assertTrue(a.getBoolean("admit"));assertEquals(66000,a.getDouble("historicalComparisonMillis"),0);assertEquals(840000,a.getDouble("forecastBenefitMillis"),0);assertEquals(1250,a.getDouble("requiredBenefitMillis"),0);}
    @Test public void negativeBenefitNeverAdmittedEvenAtLongestHorizon(){var ps=pairs();for(Object p:ps)((JSONObject)p).put("memoryMillis",12000);assertFalse(CalibrationAdmission086.assess(ps,budget().put("horizon",256)).getBoolean("admit"));}
    @Test public void equalityIsNotEnough(){var b=budget().put("horizon",1).put("fitBudgetMillis",6720);assertFalse(CalibrationAdmission086.assess(pairs(),b).getBoolean("admit"));assertTrue(CalibrationAdmission086.assess(pairs(),b.put("fitBudgetMillis",6719)).getBoolean("admit"));}
    @Test public void futureCollectionIsChargedNotDoubleCounted(){var a=CalibrationAdmission086.assess(pairs(),budget().put("additionalRuns",12));assertEquals(121000,a.getDouble("futureInvestmentMillis"),0);assertEquals(66000,a.getDouble("historicalComparisonMillis"),0);}
    @Test public void variabilityCanVetoPositiveMean(){var ps=pairs();ps.getJSONObject(0).put("memoryMillis",30000);assertFalse(CalibrationAdmission086.assess(ps,budget()).getBoolean("admit"));}
    @Test public void duplicateGoalAndMixedWorkloadFailClosed(){var ps=pairs();ps.getJSONObject(1).put("simpleId",ps.getJSONObject(0).getString("simpleId"));assertFalse(CalibrationAdmission086.assess(ps,budget()).getBoolean("supported"));ps=pairs();ps.getJSONObject(0).put("receiptId","different");assertFalse(CalibrationAdmission086.assess(ps,budget()).getBoolean("supported"));ps=pairs();ps.getJSONObject(0).getJSONObject("workload").put("skill",true);assertFalse(CalibrationAdmission086.assess(ps,budget()).getBoolean("supported"));}
    @Test public void countsAndDistinctTasksMatter(){var ps=pairs();ps.remove(0);assertFalse(CalibrationAdmission086.assess(ps,budget()).getBoolean("supported"));ps=pairs();for(Object p:ps)((JSONObject)p).put("taskSha256","same");assertFalse(CalibrationAdmission086.assess(ps,budget()).getBoolean("supported"));}
    @Test public void invalidAllowancesCannotOpenTheGate(){for(String key:List.of("horizon","fitBudgetMillis","checkBudgetMillis")){assertThrows(IllegalArgumentException.class,()->CalibrationAdmission086.assess(pairs(),budget().put(key,0)));assertThrows(IllegalArgumentException.class,()->CalibrationAdmission086.assess(pairs(),budget().put(key,-1)));}assertThrows(IllegalArgumentException.class,()->CalibrationAdmission086.assess(pairs(),budget().put("horizon",1.5)));assertThrows(IllegalArgumentException.class,()->CalibrationAdmission086.assess(pairs(),budget().put("additionalRuns",17)));assertThrows(IllegalArgumentException.class,()->CalibrationAdmission086.assess(pairs(),budget().put("force",true)));}
    static JSONObject seed(Path root,boolean positive)throws Exception {
        var selected=new JSONArray();for(int i=0;i<6;i++){
            String a=UUID.randomUUID().toString(),b=UUID.randomUUID().toString();
            var s=TaskCost080Test.measured(a,"simple",10000,i%2*.01).put("code",WorldProcedure079.code());
            var m=TaskCost080Test.measured(b,"memory",positive?1000:12000,i%2*.01).put("code",WorldProcedure079.code());
            for(var c:List.of(s,m))AgentFiles042.saveChecked(root.resolve("world-procedures-079").resolve(c.getString("id")).resolve("cost080.json"),c);
            selected.put(new JSONObject().put("simpleId",a).put("memoryId",b));
        }return new JSONObject().put("pairs",selected).put("budget",budget());
    }
    @Test public void declineDoesNotCallFitterAndReceiptSurvivesRestart()throws Exception {
        Path root=Files.createTempDirectory("admission086-test");var q=seed(root,false);var count=new AtomicInteger();
        var service=new CalibrationAdmission086(root,new Object(),p->{count.incrementAndGet();throw new AssertionError("Should not fit");});
        var result=service.fit(q);assertFalse(result.getBoolean("fitted"));assertEquals(0,count.get());assertEquals("declined",result.getString("status"));assertEquals(0,result.getJSONObject("admission").getLong("originalTraceBytes"));
        assertTrue(new CalibrationAdmission086(root,new Object(),p->null).history().getJSONArray("entries").getJSONObject(0).similar(result));
    }
    @Test public void positiveProposalCallsFullFitterAndRecordsActualCost()throws Exception {
        Path root=Files.createTempDirectory("admission086-test");var q=seed(root,true);var count=new AtomicInteger();
        var service=new CalibrationAdmission086(root,new Object(),p->{count.incrementAndGet();assertTrue(p.similar(q.getJSONArray("pairs")));return new JSONObject().put("id","verified-model");});
        var r=service.fit(q);assertTrue(r.getBoolean("fitted"));assertEquals(1,count.get());assertTrue(r.getDouble("fitMillis")>=0);assertEquals("verified-model",r.getString("modelId"));
    }
    @Test public void positiveAdmissionCannotSuppressVerificationFailure()throws Exception {
        Path root=Files.createTempDirectory("admission086-test");var q=seed(root,true);
        var service=new CalibrationAdmission086(root,new Object(),p->{throw new IllegalStateException("Original report changed");});
        assertThrows(IllegalStateException.class,()->service.fit(q));var r=service.history().getJSONArray("entries").getJSONObject(0);assertFalse(r.getBoolean("fitted"));assertEquals("verification-refused",r.getString("status"));
    }
    @Test public void proposalMustMatchCurrentRuntimeAndCode()throws Exception {
        Path root=Files.createTempDirectory("admission086-test");var q=seed(root,true);String id=q.getJSONArray("pairs").getJSONObject(0).getString("simpleId");Path p=root.resolve("world-procedures-079").resolve(id).resolve("cost080.json");var c=AgentFiles042.loadChecked(p);c.put("code","old");AgentFiles042.saveChecked(p,c);
        var service=new CalibrationAdmission086(root,new Object(),x->null);assertThrows(IllegalStateException.class,()->service.preview(q));assertEquals(1,service.review().getJSONArray("skipped").length());
    }
}
