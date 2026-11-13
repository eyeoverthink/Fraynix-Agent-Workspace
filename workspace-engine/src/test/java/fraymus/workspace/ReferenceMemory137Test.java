package fraymus.workspace;
import org.json.*;
import java.nio.file.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReferenceMemory137Test {
 @Test public void genuineSourceSetReusedOnSeparatedPixelsThenTamperingIsRefused()throws Exception{
  try(var f=new VerifiedAppearances135Test.Fixture()){
   var set=f.service.retain(f.audit.getString("id"));var service=new ReferenceMemory137(f.root,f.observations);
   var memory=service.resolve(set.getString("id"),f.dataset.getString("id"),"Synthetic control, separate pixels; not a semantic identity proof");
   assertTrue(memory.getJSONArray("examples").length()>0);assertTrue(memory.getJSONArray("examples").length()<=6);
   assertThrows(IllegalArgumentException.class,()->service.resolve(set.getString("id"),f.data.getString("id"),"Same source refused"));
   assertThrows(IllegalArgumentException.class,()->service.resolve(set.getString("id"),f.dataset.getString("id"),""));
   int revision=f.observations.state(f.world).getJSONObject("estimate").getInt("revision");
   var args=WorldObservationStoreTest.arguments(f.ledger,f.world,f.dataset,revision).put("trackerPolicy","verified-memory-137").put("appearanceSet137",set.getString("id")).put("referenceDeclaration137","Synthetic separated control");
   var run=WorldObservationStoreTest.settled(f.observations,f.observations.start(args).getString("id"));assertEquals("completed",run.getString("status"));service.verify(run);
   String body=run.getJSONArray("bindings").getJSONObject(0).getString("bodyId");assertEquals(0,ReferenceAudit137.check(run,body).getInt("automaticAdmissions"));
   var changed=WorldFrames.copy(run);changed.getJSONArray("trace").getJSONObject(0).getJSONArray("objects").getJSONObject(0).getJSONObject("tracker").getJSONObject("reference137").put("automaticAdmissions",1);
   assertThrows(IllegalStateException.class,()->ReferenceAudit137.check(changed,body));
   assertEquals(1,service.list().getJSONArray("sets").length());assertTrue(service.bundle(run).getJSONObject("set").similar(set));
   var source=f.root.resolve("world-observations/datasets/"+f.data.getString("id")+"/12.image");var pixels=Files.readAllBytes(source);pixels[20]^=1;Files.write(source,pixels);
   assertThrows(IllegalStateException.class,()->service.verify(run));assertThrows(IllegalStateException.class,()->f.observations.export(run.getString("id")));
  }
 }
 @Test public void duplicateImageInDifferentlyNamedDatasetCannotLeakIntoEvaluation()throws Exception{
  try(var f=new VerifiedAppearances135Test.Fixture()){
   var set=f.service.retain(f.audit.getString("id"));var pack=WorldObservationStoreTest.pack();
   byte[] source=f.observations.image(f.data.getString("id"),1);
   // All frames share the donor dimensions; even one duplicate is enough for refusal.
   for(Object raw:pack.getJSONArray("frames")){var frame=(JSONObject)raw;frame.put("image",java.util.Base64.getEncoder().encodeToString(source)).put("imageSha256",PerceptionStore.hash(source));}
   var target=WorldObservationStoreTest.importPack(f.observations,pack);
   var error=assertThrows(IllegalArgumentException.class,()->new ReferenceMemory137(f.root,f.observations).resolve(set.getString("id"),target.getString("id"),"Renaming cannot separate pixels"));assertTrue(error.getMessage().contains("share source image bytes"));
  }
 }
}
