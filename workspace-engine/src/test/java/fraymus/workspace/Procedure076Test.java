package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

/** Contract/authority tests; synthetic rows here are not native study outcomes. */
public class Procedure076Test {
    private JSONObject spec(){return Procedure076.protocol().getJSONArray("training").getJSONObject(0);}
    @Test public void finalStageKeepsExactRequestedTaskAndOriginalLimits(){
        var s=spec();var frame=WorldFrames.frame(Procedure076.scene(s));
        for(String family:Procedure076.FAMILIES){int n=Procedure076.stages(family);var p=Procedure076.plan(s,family,n-1,frame);var ts=p.getJSONObject("intent").getJSONArray("targets");if(family.equals("reverse")){assertTrue(ts.getJSONObject(0).similar(s.getJSONArray("targets").getJSONObject(1)));}else assertTrue(ts.similar(s.getJSONArray("targets")));assertEquals(s.getDouble("torqueLimitNm"),p.getJSONObject("intent").getDouble("torqueLimitNm"),0);assertEquals(family.equals("half-force")?s.getDouble("forceLimitN")*.5:s.getDouble("forceLimitN"),p.getJSONObject("intent").getDouble("forceLimitN"),0);}
    }
    @Test public void stagesAreAnchoredToOriginalSourceAndShortestYaw(){
        var s=spec();var source=s.getJSONObject("source");var a=source.getJSONArray("objects").getJSONObject(1);a.put("rotation",WorldPlan.quat(170));s.getJSONArray("targets").getJSONObject(0).put("yawDeg",-170);
        var frame=WorldFrames.frame(Procedure076.scene(s));var p=Procedure076.plan(s,"two-stage",0,frame);assertEquals(180,p.getJSONObject("intent").getJSONArray("targets").getJSONObject(0).getDouble("yawDeg"),1e-10);
        var moved=WorldFrames.copy(frame);moved.getJSONArray("bodies").getJSONObject(0).getJSONArray("position").put(0,-2.2);var q=Procedure076.plan(s,"two-stage",0,moved);assertTrue(p.getJSONObject("intent").getJSONArray("targets").similar(q.getJSONObject("intent").getJSONArray("targets")));
    }
    @Test public void rejectsChangedOriginalGeometryAndUnknownProcedures(){
        var s=spec();var frame=WorldFrames.frame(Procedure076.scene(s));var changed=WorldFrames.copy(s);changed.put("forceLimitN",40);
        assertThrows(IllegalStateException.class,()->Procedure076.plan(changed,"direct",0,frame));assertThrows(IllegalArgumentException.class,()->Procedure076.plan(s,"invented",0,frame));assertThrows(IllegalArgumentException.class,()->Procedure076.plan(s,"three-stage",3,frame));
    }
    @Test public void completeFreshActualReserveIsNeverBorrowed(){
        assertTrue(Procedure076.admitted(0,6,0));assertFalse(Procedure076.admitted(1,6,0));assertTrue(Procedure076.admitted(14400,5,0));assertFalse(Procedure076.admitted(72001,1,0));assertFalse(Procedure076.admitted(0,6,360001));
    }
    @Test public void roleMismatchDoesNotInventVerifiedProcedure(){
        var s=spec();var bank=new JSONArray().put(new JSONObject().put("id","one").put("trainingRowId","train-short").put("family","direct").put("trainingTicks",100).put("spec",s));assertEquals(1,Procedure076.rank(bank,s).length());
        var novel=Procedure076.protocol().getJSONArray("cases").getJSONObject(7);assertTrue(Procedure076.rank(bank,novel).isEmpty());
    }
    @Test public void geometryAndSimpleHaveSamePortfolioWithoutBankAccess(){
        var p=Procedure076.protocol();assertEquals("three-stage",Procedure076.geometric(p.getJSONArray("training").getJSONObject(2)));assertEquals("two-stage",Procedure076.geometric(p.getJSONArray("training").getJSONObject(1)));assertEquals("direct",Procedure076.geometric(spec()));assertEquals(Procedure076.FAMILIES,Procedure076.order("simple","three-stage"));assertEquals("three-stage",Procedure076.order("memory","three-stage").get(0));assertEquals(3,Procedure076.order("legacy","three-stage").size());
    }
    private JSONArray rows(){var a=new JSONArray();for(int i=0;i<12;i++){var results=new JSONObject();for(String m:Procedure076.METHODS)results.put(m,new JSONObject().put("success",i<8).put("status",i<8?"completed":"refused").put("chargedTicks",i<8?1000:0).put("attempts",new JSONArray()).put("activeWallMillis",1));a.put(new JSONObject().put("split",i<8?"unseen":"unsupported").put("results",results));}return a;}
    @Test public void cheaperTiesOrLegacyOnlyWinsCannotClaimLearnedSuccessImprovement(){var r=rows();for(int i=0;i<8;i++)r.getJSONObject(i).getJSONObject("results").getJSONObject("memory").put("chargedTicks",1);assertFalse(Procedure076.evaluation(r).getBoolean("qualified"));r.getJSONObject(0).getJSONObject("results").getJSONObject("legacy").put("success",false);assertFalse(Procedure076.evaluation(r).getBoolean("qualified"));for(String m:List.of("simple","geometry"))r.getJSONObject(0).getJSONObject("results").getJSONObject(m).put("success",false);assertTrue(Procedure076.evaluation(r).getBoolean("qualified"));r.getJSONObject(1).getJSONObject("results").getJSONObject("memory").put("success",false);assertFalse(Procedure076.evaluation(r).getBoolean("qualified"));}
    @Test public void unsupportedControlsCannotBeDroppedOrActuate(){var r=rows();for(String m:List.of("simple","geometry"))r.getJSONObject(0).getJSONObject("results").getJSONObject(m).put("success",false);r.getJSONObject(8).getJSONObject("results").getJSONObject("memory").getJSONArray("attempts").put(new JSONObject());assertFalse(Procedure076.evaluation(r).getBoolean("qualified"));assertFalse(Procedure076.evaluation(r).getBoolean("unsupportedSafe"));}
    @Test public void untrustedPassedClaimsCannotStartTraining()throws Exception {try(var store=new ProcedureStudy076(Files.createTempDirectory("procedure076-input-"))){assertThrows(IllegalArgumentException.class,()->store.start(new JSONObject().put("passed",true)));assertTrue(store.list().isEmpty());assertThrows(IllegalArgumentException.class,()->store.resume(UUID.randomUUID().toString()));}}
    @Test public void protectedBodyExcursionBetweenSamplesCannotBeHidden(){
        var s=spec();var scene=Procedure076.scene(s);var body=WorldPlan.sourceObject(scene,"bystander");String id=body.getString("id");var initial=WorldFrames.frame(scene).getJSONArray("bodies");
        var rows=new JSONArray();for(int i=0;i<2;i++){var deltas=new JSONArray().put(new JSONArray().put(id).put(new JSONArray(new double[]{i==0?.06:-.06,0,0})).put(body.getJSONArray("rotation")));rows.put(new JSONArray().put(i+1).put("").put(new JSONArray()).put(new JSONArray()).put(0).put(0).put(deltas));}
        var run=new JSONObject().put("initialState",new JSONObject().put("bodies",initial)).put("samples",new JSONArray().put(new JSONObject().put("integration",rows)));
        assertFalse(Procedure076.protectedTrace(s,run));rows.getJSONArray(0).getJSONArray(6).getJSONArray(0).getJSONArray(1).put(0,.01);rows.getJSONArray(1).getJSONArray(6).getJSONArray(0).getJSONArray(1).put(0,-.01);assertTrue(Procedure076.protectedTrace(s,run));
    }
}
