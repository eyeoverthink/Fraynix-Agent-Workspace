package fraymus.workspace;

import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class DriftGuard138Test {
 static PixelTracker.Box seed(){return new PixelTracker.Box(40/320.,35/220.,90/320.,120/220.);}
 static BufferedImage scene(int shift,boolean wrong,boolean hidden,boolean twin){
  var image=new BufferedImage(320,220,BufferedImage.TYPE_INT_RGB);for(int y=0;y<220;y++)for(int x=0;x<320;x++)image.setRGB(x,y,0x303038);
  if(hidden)return image;
  var random=new Random(138);for(int y=0;y<120;y++)for(int x=0;x<90;x++){
   int q=random.nextInt(70),r=(wrong?200:20)+q/2,g=(wrong?58:150)+q/2,b=20+q/2,c=(r<<16)|(g<<8)|b;
   image.setRGB(40+shift+x,35+y,c);if(twin)image.setRGB(210+x,35+y,c);
  }return image;
 }
 @Test public void acceptedWrongAppearanceIsWithheldBeforeFlowLossAndMonitorIsExact(){
  var first=scene(0,false,false,false);var guarded=new DriftGuard138("body","object",first,seed(),true);var monitor=new DriftGuard138("body","object",first,seed(),false);var baseline=new FlowTracker("body","object",first,seed());
  for(int i=1;i<=4;i++){
   var frame=scene(i,i>=3,false,false);var g=guarded.update(frame);var m=monitor.update(frame);var original=baseline.update(frame);var meta=(JSONObject)m.remove("drift138");
   assertTrue(m.similar(original));assertFalse("Synthetic motion must remain accepted by incumbent",original.isNull("box"));
   assertEquals(0,meta.getInt("automaticAdmissions"));
   if(i<3)assertFalse(g.toString(),g.isNull("box"));else{assertTrue(g.toString(),g.isNull("box"));assertTrue(g.getJSONObject("drift138").getBoolean("suspect"));assertFalse(new ObservationFilter(0,seed()).step(i,g).getBoolean("accepted"));}
  }
 }
 @Test public void completeOcclusionRequiresTwoUniqueReturnImagesAndTwinsStayWithheld(){
  var first=scene(0,false,false,false);var g=new DriftGuard138("body","object",first,seed(),true);
  assertTrue(g.update(scene(0,false,true,false)).isNull("box"));assertTrue(g.update(scene(0,false,true,false)).isNull("box"));
  assertTrue(g.update(scene(100,false,false,false)).isNull("box"));var recovered=g.update(scene(100,false,false,false));
  assertFalse(recovered.toString(),recovered.isNull("box"));assertTrue(recovered.getJSONObject("drift138").getJSONObject("recovery").getBoolean("accepted"));
  var twins=new DriftGuard138("body","object",first,seed(),true);twins.update(scene(0,false,true,false));
  for(int i=0;i<3;i++)assertTrue(twins.update(scene(0,false,false,true)).isNull("box"));
 }
 @Test public void replayIsDeterministicAndSourceDimensionsAndDefaultsRemainChecked(){
  var a=new DriftGuard138("body","object",scene(0,false,false,false),seed(),true);var b=new DriftGuard138("body","object",scene(0,false,false,false),seed(),true);
  for(int i=0;i<8;i++){var image=scene(i,i>=4,false,false);assertTrue(a.update(image).similar(b.update(image)));}
  assertThrows(IllegalArgumentException.class,()->a.update(new BufferedImage(321,220,BufferedImage.TYPE_INT_RGB)));
  assertEquals("legacy-flow",ObservationMethods129.policy(new JSONObject()));
 }
 @Test public void durableStoppedPrefixAndAuditPixelReplaySurviveRestartAndRejectChangedEvidence()throws Exception{
  Path root=Files.createTempDirectory("drift138-");var ledger=new WorldLedger(root);String world=ledger.create(WorldLedgerTest.source(WorldLedgerTest.suite().getJSONArray("cases").getJSONObject(0)),"Drift control").getString("id");
  JSONObject run,audit,label,dataset;byte[] state;
  try(var s=new WorldObservationStore(root,ledger)){
   var pack=WorldObservationStoreTest.pack();dataset=WorldObservationStoreTest.importPack(s,pack);
   String id=s.start(WorldObservationStoreTest.arguments(ledger,world,dataset,0).put("trackerPolicy","drift-guard-138")).getString("id");s.stop(id);var prefix=s.read(id).getJSONArray("trace");s.resume(id);run=WorldObservationStoreTest.settled(s,id);assertEquals(run.toString(),"completed",run.getString("status"));
   for(int i=0;i<prefix.length();i++)assertTrue(prefix.getJSONObject(i).similar(run.getJSONArray("trace").getJSONObject(i)));
   var rows=new JSONArray();for(int i=1;i<9;i++){var f=dataset.getJSONArray("frames").getJSONObject(i);rows.put(new JSONObject().put("ordinal",i).put("sourceFrame",f.getInt("sourceFrame")).put("imageSha256",f.getString("imageSha256")).put("visibility","visible").put("targetBox",pack.getJSONArray("objects").getJSONObject(0).getJSONArray("box")).put("note","Independent static fixture geometry"));}
   var input=new JSONObject().put("schema",AssociationAudit134.VERSION).put("datasetId",dataset.getString("id")).put("datasetSha256",WorldFrames.hash(dataset)).put("objectKey","subject").put("annotator","Generated independent geometry").put("method","synthetic-control").put("exposure","before-tracking").put("provenance","Fixed placements, not predictions").put("rows",rows);
   var service=new AssociationAudit134(root,s);label=service.save(input);audit=service.audit(new JSONObject().put("labelId",label.getString("id")).put("runId",id));assertEquals(8,audit.getJSONObject("drift138").getInt("replayedFrames"));
   var fake=WorldFrames.copy(run);fake.getJSONArray("trace").getJSONObject(0).getJSONArray("objects").getJSONObject(0).getJSONObject("tracker").getJSONObject("drift138").put("anchorSha256","forged");
   String body=run.getJSONArray("bindings").getJSONObject(0).getString("bodyId");
   assertThrows(IllegalStateException.class,()->DriftAudit138.check(fake,dataset,label.getJSONObject("input"),body,i->ImageIO.read(root.resolve("world-observations/datasets/"+dataset.getString("id")+"/"+i+".image").toFile())));
   state=Files.readAllBytes(root.resolve("world-observations/state.json"));
  }
  try(var s=new WorldObservationStore(root,new WorldLedger(root))){assertArrayEquals(state,Files.readAllBytes(root.resolve("world-observations/state.json")));assertTrue(run.similar(s.read(run.getString("id"))));var service=new AssociationAudit134(root,s);assertTrue(audit.similar(service.readAudit(audit.getString("id"))));
   Path file=root.resolve("world-observations/datasets/"+dataset.getString("id")+"/4.image");byte[] original=Files.readAllBytes(file),bad=original.clone();bad[20]^=1;Files.write(file,bad);assertThrows(IllegalStateException.class,()->service.readAudit(audit.getString("id")));Files.write(file,original);assertTrue(audit.similar(service.readAudit(audit.getString("id"))));}
 }
}
