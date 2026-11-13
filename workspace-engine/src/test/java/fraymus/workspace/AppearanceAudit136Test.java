package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class AppearanceAudit136Test {
    record Fixture(JSONObject dataset,JSONObject labels,JSONObject run) {}
    static Fixture fixture()throws Exception{
        var tracker=new AppearanceFlow136("subject","source",AppearanceFlow136Test.view(0,40,50,false,false),AppearanceFlow136Test.seed(),true);
        var frames=new JSONArray().put(new JSONObject().put("sourceFrame",1).put("imageSha256","seed"));var trace=new JSONArray();var labels=new JSONArray();
        for(int i=1;i<=12;i++){
            var t=tracker.update(AppearanceFlow136Test.view(i,40+i,50,false,false));var f=new JSONObject().put("ordinal",i).put("sourceFrame",i+1).put("imageSha256","source"+i);frames.put(WorldFrames.copy(f));
            trace.put(WorldFrames.copy(f).put("objects",new JSONArray().put(new JSONObject().put("bodyId","subject").put("tracker",t).put("estimate",new JSONObject().put("accepted",!t.isNull("box"))))));
            labels.put(WorldFrames.copy(f).put("visibility","visible").put("targetBox",new PixelTracker.Box((40+i)/480.,50/300.,100/480.,100/300.).json()).put("note","Independent generated coordinates"));
        }
        var dataset=new JSONObject().put("frames",frames).put("objects",new JSONArray().put(new JSONObject().put("key","subject").put("box",AppearanceFlow136Test.seed().json())));
        return new Fixture(dataset,new JSONObject().put("objectKey","subject").put("rows",labels),new JSONObject().put("trace",trace));
    }
    static JSONObject audit(Fixture f){return AssociationAudit134.compute(f.labels,f.dataset,f.run,"subject").getJSONObject("appearance135");}
    @Test public void independentCoordinatesScoreAllAdmittedExamples()throws Exception{
        var f=fixture();var result=audit(f);var s=result.getJSONObject("summary");assertTrue(s.getInt("admitted")>=2);assertEquals(s.getInt("admitted"),s.getInt("independentlyCorrect"));assertEquals(0,s.getInt("incorrect"));assertEquals(0,s.getInt("unknown"));
        assertTrue(result.similar(audit(f)));
    }
    @Test public void referenceDisagreementAndUnlabelledExamplesNeverBecomeVerifiedIdentity()throws Exception{
        var f=fixture();var original=WorldFrames.copy(f.run);var report=audit(f);int ordinal=report.getJSONArray("admissions").getJSONObject(0).getInt("ordinal");
        f.labels.getJSONArray("rows").getJSONObject(ordinal-1).put("targetBox",new JSONArray("[.65,.6,.9,.95]"));assertEquals(1,audit(f).getJSONObject("summary").getInt("incorrect"));
        f.labels.getJSONArray("rows").getJSONObject(ordinal-1).put("visibility","uncertain").remove("targetBox");assertEquals(1,audit(f).getJSONObject("summary").getInt("unknown"));assertTrue(f.run.similar(original));
    }
    @Test public void futureOrMutatedExampleProvenanceIsRefused()throws Exception{
        var f=fixture();var report=audit(f);int ordinal=report.getJSONArray("admissions").getJSONObject(0).getInt("ordinal");
        var bank=f.run.getJSONArray("trace").getJSONObject(ordinal-1).getJSONArray("objects").getJSONObject(0).getJSONObject("tracker").getJSONObject("appearance135");
        bank.getJSONArray("examples").getJSONObject(1).put("sourceOrdinal",90);assertThrows(IllegalArgumentException.class,()->audit(f));
    }
    @Test public void interruptedCandidateRebuildsSameBankInDurableRun()throws Exception{
        try(var f=new ObservationLabels132Test.Fixture()){
            var args=WorldObservationStoreTest.arguments(f.ledger,f.world,f.dataset,0).put("trackerPolicy","seed-anchored-136");String id=f.observations.start(args).getString("id");f.observations.stop(id);f.observations.resume(id);
            var run=WorldObservationStoreTest.settled(f.observations,id);assertEquals("completed",run.getString("status"));assertEquals(1,run.getInt("resumes"));
            var service=new AssociationAudit134(f.root,f.observations);var label=service.save(AssociationAudit134Test.input(f.dataset));var a=service.audit(new JSONObject().put("labelId",label.getString("id")).put("runId",id));
            assertTrue(a.getJSONObject("appearance135").getBoolean("available"));assertTrue(new AssociationAudit134(f.root,f.observations).readAudit(a.getString("id")).similar(a));
        }
    }
    @Test public void fabricatedOriginalSeedEvidenceCannotVerifyAnAdmission()throws Exception{
        for(String change:new String[]{"source","geometry","motion"}){
            var f=fixture();var report=audit(f);int ordinal=report.getJSONArray("admissions").getJSONObject(0).getInt("ordinal");
            var bank=f.run.getJSONArray("trace").getJSONObject(ordinal-1).getJSONArray("objects").getJSONObject(0).getJSONObject("tracker").getJSONObject("appearance135");
            var anchor=bank.getJSONObject("admission").getJSONObject("example").getJSONObject("checks").getJSONObject("anchor136");
            if(change.equals("source"))anchor.put("sourceOrdinal",1);else if(change.equals("geometry"))anchor.put("overlap",.123);else anchor.getJSONObject("forward").put("inliers",0);
            assertThrows(IllegalArgumentException.class,()->audit(f));
        }
    }
}
