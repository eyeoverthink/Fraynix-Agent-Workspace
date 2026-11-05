package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Design058Test {
    @Test public void measuredFailureSelectsBeforeReservedCasesAndFreshEvaluationSurvivesInterruption()throws Exception {
        Path root=Files.createTempDirectory("design058-native-");System.out.println("DESIGN058_NATIVE "+root);
        var worlds=new WorldLedger(root);var input=Load056Test.input(worlds,"wide",.8,0);var before=worlds.observe(input.getString("worldId"));
        String id,decision;JSONObject plan;
        try(var store=new Counterfactual045(root,worlds)) {
            id=store.prepare(before,worlds.preview(input),true,0,true,true,true).getString("id");store.begin(id);var study=Design056Test.terminal(store,id);
            Files.writeString(root.resolve("design-summary.json"),study.toString(2));
            assertEquals(study.optString("reason"),"ready",study.getString("status"));assertFalse(study.getJSONArray("candidates").getJSONObject(0).getJSONObject("payloadDesign058").getBoolean("qualifies"));
            assertNotEquals("maximum-force",study.getJSONObject("decision").getString("selectedCandidate"));
            assertTrue(Design058.choose(study).getJSONObject("payloadDesign058").getBoolean("qualifies"));
            for(Object raw:study.getJSONArray("candidates")){var c=(JSONObject)raw;if(c.has("payloadDesign058"))for(Object r:c.getJSONObject("payloadDesign058").getJSONArray("results"))assertFalse(((JSONObject)r).getString("caseId").startsWith("reserved-"));}
            assertFalse(Files.exists(root.resolve("evaluation")));decision=study.getString("decisionSha256");plan=store.executePlan(id,decision);
        }
        try(var store=new Counterfactual045(root,worlds)) {
            assertTrue(plan.similar(store.executePlan(id,decision)));assertTrue(before.similar(worlds.observe(input.getString("worldId"))));
            var actual=Construction054Test.nativeRun(root.resolve("fresh-assembly"),worlds.source(before),plan);assertTrue(WorldVerify.verify(actual).getBoolean("passed"));
            Files.writeString(root.resolve("fresh-assembly.json"),WorldReport.packed(actual).toString());
            Path evaluation=root.resolve("evaluation");var calls=new AtomicInteger();assertThrows(InterruptedException.class,()->Design058.evaluate(evaluation,store,id,actual,()->calls.incrementAndGet()>=3));
            Path folder=evaluation.resolve(actual.getString("id"));var pinned=AgentFiles042.loadChecked(folder.resolve("evaluation.json"));assertEquals(decision,pinned.getJSONObject("binding").getString("decisionSha256"));assertFalse(Files.exists(folder.resolve("baseline-reserved")));
            var partial=new PayloadSeries058("design").originals(folder.resolve("fresh-design"),actual);var first=partial.getJSONArray("records").getJSONObject(0).getJSONObject("original");assertTrue(first.getInt("rawBytes")>1000);Files.writeString(root.resolve("partial-originals.json"),partial.toString());
            var result=Design058.evaluate(evaluation,store,id,actual,()->false);Files.writeString(root.resolve("evaluation-summary.json"),Payload057.summary(result).toString(2));
            assertTrue(result.getBoolean("complete"));assertTrue(result.getBoolean("repeatable"));assertTrue(result.getBoolean("freshDesignPassed"));assertEquals(Design058.eligible(result),result.getBoolean("promotionAllowed"));
            assertEquals(decision,store.read(id).getString("decisionSha256"));assertTrue(before.similar(worlds.observe(input.getString("worldId"))));
            var originals=Design058.originals(evaluation,store,id,actual);Files.writeString(root.resolve("all-originals.json"),originals.toString());
            var preserved=new PayloadSeries058("design").originals(folder.resolve("fresh-design"),actual).getJSONArray("records").getJSONObject(0).getJSONObject("original");assertEquals(first.getString("rawSha256"),preserved.getString("rawSha256"));
            assertTrue(result.similar(Design058.read(evaluation,store,id,actual)));
            var forged=WorldFrames.copy(result).put("selectedReservedPassed",false);assertFalse(Design058.eligible(forged));
            var changed=WorldFrames.copy(pinned);changed.getJSONObject("binding").put("decisionSha256","0".repeat(64));AgentFiles042.saveChecked(folder.resolve("evaluation.json"),changed);assertThrows(IllegalStateException.class,()->Design058.evaluate(evaluation,store,id,actual,()->false));
            System.out.println("DESIGN058_RESULT "+Payload057.summary(result));
        }
    }
    @Test public void disabledRedesignAndReservedCasesCannotChangeSelection()throws Exception {
        Path root=Files.createTempDirectory("design058-policy-");var worlds=new WorldLedger(root);var in=Load056Test.input(worlds,"wide",.8,0);
        try(var store=new Counterfactual045(root,worlds)){var study=store.prepare(worlds.observe(in.getString("worldId")),worlds.preview(in),true,0,true,true,false);assertEquals("rejected",study.getJSONArray("candidates").getJSONObject(1).getString("status"));assertEquals("rejected",study.getJSONArray("candidates").getJSONObject(2).getString("status"));assertNull(Design058.choose(study));study.put("reserved",new JSONObject().put("qualifies",true));assertNull(Design058.choose(study));}
        var train=new PayloadSeries058("design").protocol();var reserved=new PayloadSeries058("reserved").protocol();assertEquals(4,train.getJSONArray("cases").length());assertEquals(4,reserved.getJSONArray("cases").length());for(Object raw:reserved.getJSONArray("cases")){var spec=(JSONObject)raw;assertThrows(IllegalArgumentException.class,()->Payload057.specification(spec.getString("id")));assertThrows(IllegalArgumentException.class,()->Payload057.specification(train,spec.getString("id")));}
    }
    @Test public void failedAssemblyDoesNotInventALoadFailureToAdmitWiderLayouts()throws Exception {
        Path root=Files.createTempDirectory("design058-unmeasured-");System.out.println("DESIGN058_UNMEASURED "+root);var worlds=new WorldLedger(root);var input=Load056Test.input(worlds,"wide",.8,0).put("forceLimitN",2);var before=worlds.observe(input.getString("worldId"));
        try(var store=new Counterfactual045(root,worlds)){String id=store.prepare(before,worlds.preview(input),true,0,true,true,true).getString("id");store.begin(id);var result=Design056Test.terminal(store,id);assertEquals("failed",result.getString("status"));var c=result.getJSONArray("candidates");assertFalse(c.getJSONObject(0).getJSONObject("metrics").getBoolean("passed"));for(int i=1;i<3;i++){assertEquals("rejected",c.getJSONObject(i).getString("status"));assertEquals(0,c.getJSONObject(i).getJSONArray("attempts").length());}assertTrue(before.similar(worlds.observe(input.getString("worldId"))));Files.writeString(root.resolve("original.json"),store.export(id).toString());}
    }
}
