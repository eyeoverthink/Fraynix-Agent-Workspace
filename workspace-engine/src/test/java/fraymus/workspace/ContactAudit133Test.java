package fraymus.workspace;
import java.nio.file.*;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ContactAudit133Test {
 @Test public void sourceRecomputationRestartAndLaterLabelsDoNotAlterRunOrWorld()throws Exception{
  try(var f=new ObservationLabels132Test.Fixture()){
   var before=f.ledger.observe(f.world);var label=f.store.save(ObservationLabels132Test.input(f.dataset));var run=f.run();var service=new ContactAudit133(f.root,f.observations,f.store);var args=new JSONObject().put("labelId",label.getString("id")).put("runId",run.getString("id"));var audit=service.audit(args);
   assertFalse(audit.getBoolean("trackerReplayed"));assertEquals(4,audit.getJSONObject("summary").getInt("observableContacts"));assertTrue(audit.similar(service.audit(args)));assertTrue(audit.similar(new ContactAudit133(f.root,f.observations,f.store).read(audit.getString("id"))));
   var amended=WorldFrames.copy(label.getJSONObject("input"));for(Object raw:amended.getJSONArray("rows"))((JSONObject)raw).put("pointImage",new JSONArray("[.95,.95]"));var correction=f.store.save(amended);var other=service.audit(new JSONObject().put("labelId",correction.getString("id")).put("runId",run.getString("id")));
   for(int i=0;i<4;i++)assertTrue(audit.getJSONArray("scores").getJSONObject(i).getJSONObject("contact").similar(other.getJSONArray("scores").getJSONObject(i).getJSONObject("contact")));
   assertTrue(run.similar(f.observations.read(run.getString("id"))));assertTrue(before.similar(f.ledger.observe(f.world)));
  }
 }
 @Test public void changedOriginalUnlabelledPixelsAndDamagedAuditRefuse()throws Exception{
  try(var f=new ObservationLabels132Test.Fixture()){
   var label=f.store.save(ObservationLabels132Test.input(f.dataset));var run=f.run();var service=new ContactAudit133(f.root,f.observations,f.store);var audit=service.audit(new JSONObject().put("labelId",label.getString("id")).put("runId",run.getString("id")));
   Path image=f.root.resolve("world-observations/datasets/"+f.dataset.getString("id")+"/8.image");byte[] original=Files.readAllBytes(image),bad=original.clone();bad[20]^=1;Files.write(image,bad);assertThrows(IllegalStateException.class,()->service.read(audit.getString("id")));assertArrayEquals(bad,Files.readAllBytes(image));Files.write(image,original);
   Path file=f.root.resolve("contact-audits133/"+audit.getString("id")+".json");String prior=Files.readString(file);Files.writeString(file,prior.replace("\"trackerReplayed\": false","\"trackerReplayed\": true"));assertThrows(IllegalStateException.class,()->service.read(audit.getString("id")));Files.writeString(file,prior);assertTrue(audit.similar(service.read(audit.getString("id"))));
  }
 }
 @Test public void differentSourceAndIncompleteRunRefuse()throws Exception{
  try(var f=new ObservationLabels132Test.Fixture()){
   var label=f.store.save(ObservationLabels132Test.input(f.dataset));var other=WorldObservationStoreTest.importPack(f.observations,WorldObservationStoreTest.pack());var run=WorldObservationStoreTest.settled(f.observations,f.observations.start(WorldObservationStoreTest.arguments(f.ledger,f.world,other,0)).getString("id"));var service=new ContactAudit133(f.root,f.observations,f.store);
   assertThrows(IllegalArgumentException.class,()->service.audit(new JSONObject().put("labelId",label.getString("id")).put("runId",run.getString("id"))));
   var args=WorldObservationStoreTest.arguments(f.ledger,f.world,f.dataset,f.observations.state(f.world).getJSONObject("estimate").getInt("revision"));String id=f.observations.start(args).getString("id");f.observations.stop(id);assertThrows(IllegalArgumentException.class,()->service.audit(new JSONObject().put("labelId",label.getString("id")).put("runId",id)));
  }
 }
 @Test public void hiddenLabelsAreNotRemovedOrUsedToSuppressTheEstimator()throws Exception{
  try(var f=new ObservationLabels132Test.Fixture()){
   var input=ObservationLabels132Test.input(f.dataset);var visible=f.store.save(input);var run=f.run();var service=new ContactAudit133(f.root,f.observations,f.store);var a=service.audit(new JSONObject().put("labelId",visible.getString("id")).put("runId",run.getString("id")));
   ObservationLabels132Test.hidden(input.getJSONArray("rows").getJSONObject(0),"occluded");ObservationLabels132Test.hidden(input.getJSONArray("rows").getJSONObject(1),"out-of-view");var hidden=f.store.save(input);var b=service.audit(new JSONObject().put("labelId",hidden.getString("id")).put("runId",run.getString("id")));
   assertEquals(4,b.getJSONArray("scores").length());assertEquals(2,b.getJSONObject("summary").getInt("observableContacts"));assertEquals(2,b.getJSONObject("summary").getInt("unobservableContacts"));for(int i=0;i<4;i++)assertTrue(a.getJSONArray("scores").getJSONObject(i).getJSONObject("contact").similar(b.getJSONArray("scores").getJSONObject(i).getJSONObject("contact")));
  }
 }
 @Test public void explicitCandidateRetainsContactAcrossInterruptedRun()throws Exception{
  assertEquals("legacy-flow",ObservationMethods129.policy(new JSONObject()));
  try(var f=new ObservationLabels132Test.Fixture()){
   var before=f.ledger.observe(f.world);var args=WorldObservationStoreTest.arguments(f.ledger,f.world,f.dataset,0).put("trackerPolicy","association-contact-133");String id=f.observations.start(args).getString("id");f.observations.stop(id);f.observations.resume(id);var run=WorldObservationStoreTest.settled(f.observations,id);assertEquals("completed",run.getString("status"));assertEquals(1,run.getInt("resumes"));
   for(Object raw:run.getJSONArray("trace"))assertTrue(((JSONObject)raw).getJSONArray("objects").getJSONObject(0).getJSONObject("tracker").has("contact133"));
   var label=f.store.save(ObservationLabels132Test.input(f.dataset));var service=new ContactAudit133(f.root,f.observations,f.store);assertEquals("association-contact-133",service.audit(new JSONObject().put("labelId",label.getString("id")).put("runId",id)).getString("trackerPolicy"));assertTrue(before.similar(f.ledger.observe(f.world)));
  }
 }
}
