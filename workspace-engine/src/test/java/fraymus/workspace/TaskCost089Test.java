package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class TaskCost089Test {
    static final String OWNER="08900000-0000-4000-8000-000000000001";
    static JSONObject cost(double millis,long start,String owner) {
        return new JSONObject().put("totalMillis",millis).put("clock089",TaskCost089.clock(owner,start,start+(long)(millis*1e6)));
    }
    static void stamp(JSONObject p,boolean simpleFirst,long base,long gap) {
        double s=p.getDouble("simpleMillis"),m=p.getDouble("memoryMillis");
        var a=cost(s,simpleFirst?base:base+(long)(m*1e6)+gap,OWNER);
        var b=cost(m,simpleFirst?base+(long)(s*1e6)+gap:base,OWNER);
        p.put("pairing089",TaskCost089.pairing(a,b));
    }
    static JSONObject model() {
        var m=TaskCost085Test.model().put("version",TaskCost089.VERSION);int i=0;
        for(Object raw:m.getJSONArray("pairs")) {
            var p=(JSONObject)raw;double s=10000+(i/3)*2000+(i%3)*10000;
            p.put("simpleMillis",s).put("memoryMillis",s-4000).put("savingMillis",4000).put("memoryBankEntries",1);
            stamp(p,i%2==0,i*100_000_000_000L,1_000_000_000L);i++;
        }
        return setup(m);
    }
    static JSONObject setup(JSONObject m){return m.put("setup085",TaskCost085.setup(m.getJSONArray("pairs"),100));}
    static JSONObject estimate(JSONObject m,boolean reference){return TaskCost089.estimate(TaskCost085Test.candidate(),TaskCost080Test.spec(0),TaskCost080Test.workload(),m,1,reference);}
    static JSONObject choose(JSONObject m,String selector){return TaskCost089.choose(TaskCost080Test.spec(0),"calibrated",new JSONArray().put(TaskCost085Test.candidate()),TaskCost080Test.workload(),m,TaskCost085Test.ID,1,selector);}

    @Test public void commonSlowdownsDoNotCountAsTwoIndependentRanges() {
        var m=model();var old=estimate(m,true);var paired=estimate(m,false);
        assertEquals("paired089",paired.getString("estimator089"));assertEquals(4000,paired.getDouble("savingMillis"),0);
        assertEquals(500,paired.getDouble("pairedRangeMillis"),0);assertEquals(0,paired.getDouble("orderMarginMillis"),0);
        assertTrue(paired.getDouble("marginMillis")<old.getDouble("marginMillis"));
        assertTrue(choose(m,"paired089").getBoolean("useMemory"));assertFalse(choose(m,"reference085").getBoolean("useMemory"));
        assertEquals(WorldFrames.hash(m),paired.getString("modelSha256"));
    }
    @Test public void adversePairedVariationCanStillRejectMemory() {
        var m=model();var p=m.getJSONArray("pairs").getJSONObject(1);p.put("memoryMillis",p.getDouble("simpleMillis")+10000).put("savingMillis",-10000);stamp(p,false,100_000_000_000L,0);setup(m);
        var e=estimate(m,false);assertTrue(e.getDouble("pairedRangeMillis")>=14000);assertFalse(choose(m,"paired089").getBoolean("useMemory"));
    }
    @Test public void missingPairingKeepsOriginalUnpairedEstimate() {
        var m=model();m.getJSONArray("pairs").getJSONObject(0).remove("pairing089");var a=estimate(m,true);var b=estimate(m,false);
        assertEquals("unpaired-fallback",b.getString("estimator089"));assertEquals(a.getDouble("marginMillis"),b.getDouble("marginMillis"),0);assertEquals(a.getDouble("lowerSavingMillis"),b.getDouble("lowerSavingMillis"),0);
    }
    @Test public void bothOrdersAreRequiredPerTask() {
        var m=model();int i=0;for(Object raw:m.getJSONArray("pairs"))stamp((JSONObject)raw,true,100_000_000_000L*i++,0);
        assertEquals("unpaired-fallback",estimate(m,false).getString("estimator089"));assertFalse(choose(m,"paired089").getBoolean("useMemory"));
    }
    @Test public void gapBoundaryIsExactAndExcessFallsBack() {
        var m=model();var p=m.getJSONArray("pairs").getJSONObject(0);stamp(p,true,0,30_000_000_000L);assertTrue(p.getJSONObject("pairing089").getBoolean("eligible"));
        stamp(p,true,0,30_000_000_001L);assertFalse(p.getJSONObject("pairing089").getBoolean("eligible"));assertEquals("unpaired-fallback",estimate(m,false).getString("estimator089"));
    }
    @Test public void differentOwnersDoNotPretendToShareAClock() {
        var a=cost(1000,0,OWNER);var b=cost(1000,2_000_000_000L,"08900000-0000-4000-8000-000000000002");assertFalse(TaskCost089.pairing(a,b).getBoolean("eligible"));
    }
    @Test public void overlappingIntervalsAreNotPairedEvidence() {
        assertFalse(TaskCost089.pairing(cost(2000,0,OWNER),cost(2000,1_000_000_000L,OWNER)).getBoolean("eligible"));
        assertTrue(TaskCost089.pairing(cost(1000,0,OWNER),cost(1000,1_000_000_000L,OWNER)).getBoolean("eligible"));
    }
    @Test public void negativeClockOriginsRemainValidWithoutLosingIntegerPrecision() {
        var q=TaskCost089.pairing(cost(1000,-900000000000000000L,OWNER),cost(1000,-899999998000000000L,OWNER));assertTrue(q.getBoolean("eligible"));assertEquals(1000,q.getDouble("gapMillis"),0);
    }
    @Test public void alteredDurationAndNoncanonicalIntervalsAreRefused() {
        var a=cost(1000,0,OWNER);a.put("totalMillis",999);assertThrows(IllegalArgumentException.class,()->TaskCost089.pairing(a,cost(1000,2_000_000_000L,OWNER)));
        var b=cost(1000,0,OWNER);b.getJSONObject("clock089").put("start","00");assertThrows(IllegalArgumentException.class,()->TaskCost089.pairing(b,cost(1000,2_000_000_000L,OWNER)));
    }
    @Test public void fabricatedEligibleFlagCannotBypassIntervalArithmetic() {
        var m=model();m.getJSONArray("pairs").getJSONObject(0).getJSONObject("pairing089").put("gapMillis",1);assertThrows(IllegalArgumentException.class,()->estimate(m,false));
    }
    @Test public void duplicateGoalsRemainInsufficientEvidence() {
        var m=model();m.getJSONArray("pairs").getJSONObject(1).put("simpleId",m.getJSONArray("pairs").getJSONObject(0).getString("simpleId"));assertFalse(estimate(m,false).getBoolean("available"));
    }
    @Test public void receiptProfileAndWorkloadBoundariesRemain() {
        var m=model();m.getJSONObject("profile").put("cpus",999);assertFalse(estimate(m,false).getBoolean("available"));
        assertFalse(TaskCost089.estimate(TaskCost085Test.candidate().put("warm",false),TaskCost080Test.spec(0),TaskCost080Test.workload(),model(),1,false).getBoolean("available"));
        assertFalse(TaskCost089.estimate(TaskCost085Test.candidate(),TaskCost080Test.spec(0),TaskCost080Test.workload().put("skill",true),model(),1,false).getBoolean("available"));
    }
    @Test public void bankGrowthStillAddsUncertainty() {
        var m=model();var a=estimate(m,false);var b=TaskCost089.estimate(TaskCost085Test.candidate(),TaskCost080Test.spec(0),TaskCost080Test.workload(),m,5,false);assertEquals(a.getDouble("marginMillis")+100,b.getDouble("marginMillis"),1e-9);
    }
    @Test public void originalSetupAndVersionCannotBeRelabelled() {
        assertFalse(estimate(model().put("version",TaskCost085.VERSION),false).getBoolean("available"));
        var m=model();m.getJSONObject("setup085").put("totalMillis",1);assertThrows(IllegalStateException.class,()->estimate(m,false));
    }
    @Test public void referenceControlIsRestrictedToExplicitTestRuntime() {
        assertEquals("paired089",TaskCost089.selector(null,null));assertEquals("reference085",TaskCost089.selector("1","reference085"));
        assertThrows(IllegalArgumentException.class,()->TaskCost089.selector(null,"reference085"));assertThrows(IllegalArgumentException.class,()->TaskCost089.selector("1","unknown"));
    }
    @Test public void simpleAndFixedMemoryKeepTheirExistingChoices() {
        for(String mode:new String[]{"simple","memory","adaptive"}){
            var cs=new JSONArray().put(TaskCost085Test.candidate());
            assertTrue(TaskCost085.choose(TaskCost080Test.spec(0),mode,cs,TaskCost080Test.workload(),new JSONObject(),"",1).similar(TaskCost089.choose(TaskCost080Test.spec(0),mode,cs,TaskCost080Test.workload(),model(),"",1,"paired089")));
        }
    }
    @Test public void executionOrderBiasGetsItsOwnReserve() {
        var m=model();int i=0;for(Object raw:m.getJSONArray("pairs")){var p=(JSONObject)raw;double saving=i%2==0?4000:2000;p.put("memoryMillis",p.getDouble("simpleMillis")-saving).put("savingMillis",saving);stamp(p,i%2==0,100_000_000_000L*i,0);i++;}setup(m);
        var e=estimate(m,false);assertEquals(2000,e.getDouble("pairedRangeMillis"),0);assertEquals(2000,e.getDouble("orderMarginMillis"),0);
    }
    @Test public void originalJobStillBindsBankAndClockEvidence() {
        var s=TaskCost080Test.measured("s","simple",1000,0).put("clock089",TaskCost089.clock(OWNER,0,1_000_000_000L));
        var m=TaskCost080Test.measured("m","memory",900,0).put("clock089",TaskCost089.clock(OWNER,2_000_000_000L,2_900_000_000L));
        var j=new JSONObject().put("bankSnapshot",new JSONArray().put(new JSONObject().put("id","owned-memory")));m.put("jobSha256",WorldFrames.hash(j));
        assertTrue(TaskCost089.pair(s,m,j).getJSONObject("pairing089").getBoolean("eligible"));j.put("changed",true);assertThrows(IllegalArgumentException.class,()->TaskCost089.pair(s,m,j));
    }
}
