package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import java.nio.file.*;
import static org.junit.Assert.*;

public class AssociationAudit134Test {
 static JSONObject input(JSONObject d){var x=ObservationLabels132Test.input(d).put("schema",AssociationAudit134.VERSION);for(Object raw:x.getJSONArray("rows")){var r=(JSONObject)raw;r.remove("contact");r.remove("pointImage");r.remove("radiusPx");r.put("targetBox",new JSONArray("[.25,.25,.55,.75]"));}return x;}
 @Test public void immutableLabelImportAndAuditSurviveRestartWithoutAffectingTracking()throws Exception{
  try(var f=new ObservationLabels132Test.Fixture()){var service=new AssociationAudit134(f.root,f.observations);var label=service.save(input(f.dataset));var before=f.ledger.observe(f.world);var run=f.run();var a=service.audit(new JSONObject().put("labelId",label.getString("id")).put("runId",run.getString("id")));assertEquals(4,a.getJSONObject("summary").getInt("visible"));
   var restarted=new AssociationAudit134(f.root,f.observations);assertTrue(label.similar(restarted.importRecord(label)));assertTrue(a.similar(restarted.readAudit(a.getString("id"))));assertTrue(before.similar(f.ledger.observe(f.world)));assertTrue(run.similar(f.observations.read(run.getString("id"))));
   var changed=WorldFrames.copy(label.getJSONObject("input"));changed.getJSONArray("rows").getJSONObject(0).put("targetBox",new JSONArray("[.6,.2,.9,.8]"));service.save(changed);assertTrue(run.similar(f.observations.read(run.getString("id"))));assertEquals(2,service.state().getJSONArray("labels").length());
  }
 }
 @Test public void hiddenLabelsCannotInventVisibleBoxAndSourceTamperingIsRefused()throws Exception{
  try(var f=new ObservationLabels132Test.Fixture()){var service=new AssociationAudit134(f.root,f.observations);var x=input(f.dataset);x.getJSONArray("rows").getJSONObject(0).put("visibility","occluded");assertThrows(IllegalArgumentException.class,()->service.save(x));x.getJSONArray("rows").getJSONObject(0).remove("targetBox");var label=service.save(x);var run=f.run();var a=service.audit(new JSONObject().put("labelId",label.getString("id")).put("runId",run.getString("id")));
   Path image=f.root.resolve("world-observations/datasets/"+f.dataset.getString("id")+"/8.image");byte[] original=Files.readAllBytes(image),bad=original.clone();bad[20]^=1;Files.write(image,bad);assertThrows(IllegalStateException.class,()->service.readAudit(a.getString("id")));Files.write(image,original);assertTrue(a.similar(service.readAudit(a.getString("id"))));
   var corrupted=WorldFrames.copy(label);corrupted.getJSONObject("input").put("annotator","changed");assertThrows(IllegalArgumentException.class,()->service.importRecord(corrupted));
  }
 }
 @Test public void scorerSeparatesWrongOverlapFromConfirmedOtherObjectAndHiddenOutput()throws Exception{
  try(var f=new ObservationLabels132Test.Fixture()){var run=f.run();var labels=input(f.dataset);String body=run.getJSONArray("bindings").getJSONObject(0).getString("bodyId");
   for(int i=0;i<4;i++){var obj=run.getJSONArray("trace").getJSONObject(i).getJSONArray("objects").getJSONObject(0);obj.getJSONObject("estimate").put("accepted",true);obj.getJSONObject("tracker").put("box",new JSONArray("[.6,.2,.9,.8]")).put("status",i==3?"reacquired":"tracked");}
   var rows=labels.getJSONArray("rows");rows.getJSONObject(1).put("otherBoxes",new JSONArray("[[.6,.2,.9,.8]]"));rows.getJSONObject(2).put("visibility","occluded").remove("targetBox");rows.getJSONObject(3).put("targetBox",new JSONArray("[.6,.2,.9,.8]"));
   var a=AssociationAudit134.compute(labels,f.dataset,run,body);var s=a.getJSONObject("summary");assertEquals(3,s.getInt("visible"));assertEquals(2,s.getInt("overlapFailures"));assertEquals(1,s.getInt("confirmedDistractorMatches"));assertEquals(1,s.getInt("unsupported"));assertEquals(1,s.getInt("correctReacquired"));assertEquals(4,a.getJSONArray("episodes").getJSONObject(0).getInt("verifiedRecovery"));
   run.getJSONArray("trace").getJSONObject(7).put("imageSha256","changed");assertThrows(IllegalArgumentException.class,()->AssociationAudit134.compute(labels,f.dataset,run,body));
  }
 }
 @Test public void wrongDatasetAndIncompleteRunRefuse()throws Exception{
  try(var f=new ObservationLabels132Test.Fixture()){var service=new AssociationAudit134(f.root,f.observations);var label=service.save(input(f.dataset));var d=WorldObservationStoreTest.importPack(f.observations,WorldObservationStoreTest.pack());var run=WorldObservationStoreTest.settled(f.observations,f.observations.start(WorldObservationStoreTest.arguments(f.ledger,f.world,d,0)).getString("id"));assertThrows(IllegalArgumentException.class,()->service.audit(new JSONObject().put("labelId",label.getString("id")).put("runId",run.getString("id"))));}
 }
 @Test public void candidateInterruptionResumesSameEvidence()throws Exception{
  try(var f=new ObservationLabels132Test.Fixture()){var args=WorldObservationStoreTest.arguments(f.ledger,f.world,f.dataset,0).put("trackerPolicy","identity-recovery-134");String id=f.observations.start(args).getString("id");f.observations.stop(id);f.observations.resume(id);var run=WorldObservationStoreTest.settled(f.observations,id);assertEquals("completed",run.getString("status"));assertEquals(1,run.getInt("resumes"));var service=new AssociationAudit134(f.root,f.observations);var label=service.save(input(f.dataset));assertEquals("identity-recovery-134",service.audit(new JSONObject().put("labelId",label.getString("id")).put("runId",id)).getString("trackerPolicy"));}
 }
}
