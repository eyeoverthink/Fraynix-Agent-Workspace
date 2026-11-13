package fraymus.workspace;

import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class CorroboratedGuard140Test {
 @Test public void originalPixelReplayIndependentlyScoresRestoration()throws Exception{
  var images=new ArrayList<BufferedImage>();images.add(RestorationImageStudy140.image(0,1));
  var tracker=new CorroboratedGuard140("subject","object",images.get(0),seed(),true);var trace=new JSONArray();var rows=new JSONArray();
  for(int i=1;i<=20;i++){images.add(restorationFrame(i,false));trace.put(new JSONObject().put("objects",new JSONArray().put(new JSONObject().put("bodyId","subject").put("tracker",tracker.update(images.get(i))))));rows.put(new JSONObject().put("ordinal",i).put("visibility","visible").put("targetBox",seed().json()));}
  var run=new JSONObject().put("trackerPolicy","corroborated-guard-140").put("trace",trace);
  var dataset=new JSONObject().put("objects",new JSONArray().put(new JSONObject().put("key","subject").put("label","object").put("box",seed().json())));
  var labels=new JSONObject().put("objectKey","subject").put("rows",rows);String before=WorldFrames.hash(run);
  var good=CorroboratedAudit140.check(run,dataset,labels,"subject",images::get).getJSONObject("continuity");
  assertEquals(1,good.getInt("restored"));assertEquals(1,good.getInt("correct"));assertEquals(0,good.getInt("incorrect"));
  // A contradictory external label must contradict the outcome, never change the replayed decision.
  rows.getJSONObject(16).put("visibility","occluded").remove("targetBox");
  var contradicted=CorroboratedAudit140.check(run,dataset,labels,"subject",images::get).getJSONObject("continuity");
  assertEquals(0,contradicted.getInt("correct"));assertEquals(1,contradicted.getInt("incorrect"));assertEquals(before,WorldFrames.hash(run));
 }
 static BufferedImage restorationFrame(int i,boolean twin){
  double fraction=Math.min(1,(i-12)/4.);
  var frame=i<=12?RestorationImageStudy140.image(Math.min(90,i*9),1):RestorationImageStudy140.image(90,1-.7*fraction,fraction);
  if(twin&&i>=15)for(int y=35;y<155;y++)for(int x=0;x<90;x++)frame.setRGB(210+x,y,frame.getRGB(40+x,y));
  return frame;
 }
 @Test public void realPixelsRestoreAfterShadowedReturnWithThreeSourceConfirmations(){
  var first=RestorationImageStudy140.image(0,1);var guard=new CorroboratedGuard140("body","object",first,seed(),true);
  var old=new ForegroundGuard139("body","object",first,seed(),true);String anchor=null;JSONObject last=null,prior=null;int restored=0;
  for(int i=1;i<=20;i++){
   var frame=restorationFrame(i,false);last=guard.update(frame);prior=old.update(frame);var m=last.getJSONObject("drift140");
   if(anchor==null)anchor=m.getString("anchorSha256");assertEquals(anchor,m.getString("anchorSha256"));assertEquals(0,m.getInt("automaticAdmissions"));
   if(i>=10&&i<17)assertTrue("Warning/partial confirmation must stay withheld at "+i,last.isNull("box"));
   if("source-supported-continuity".equals(m.getJSONObject("recovery").getString("mode"))){
    restored++;assertEquals(17,i);assertEquals(3,m.getJSONObject("recovery").getJSONObject("continuity").getInt("consecutiveConfirmations"));
    assertTrue(PixelTracker.iou(seed(),PixelTracker.Box.from(last.getJSONArray("box")))>.8);
   }
  }
  assertEquals(1,restored);assertFalse(last.isNull("box"));assertTrue("The prior strict-only method does not restore this shadowed return",prior.isNull("box"));
 }
 @Test public void competingShadowedReturnPreventsContinuityRestoration(){
  var guard=new CorroboratedGuard140("body","object",RestorationImageStudy140.image(0,1),seed(),true);
  for(int i=1;i<=20;i++){
   var result=guard.update(restorationFrame(i,true));if(i>=15){assertTrue(result.toString(),result.isNull("box"));assertFalse(result.getJSONObject("drift140").getJSONObject("recovery").getBoolean("accepted"));}
  }
 }
 @Test public void abruptTextureReturnWithFlowLossCannotUseTheContinuityLane(){
  var guard=new CorroboratedGuard140("body","object",RestorationImageStudy140.image(0,1),seed(),true);
  boolean loss=false;
  for(int i=1;i<=20;i++){
   var frame=i<=12?RestorationImageStudy140.image(Math.min(90,i*9),1):RestorationImageStudy140.image(0,.3);
   var m=guard.update(frame).getJSONObject("drift140");if(m.getJSONObject("proposal").isNull("box"))loss=true;
   if(i>=13){assertFalse(m.getJSONObject("recovery").getBoolean("accepted"));assertFalse(m.getJSONObject("recovery").getJSONObject("continuity").getBoolean("eligible"));}
  }
  assertTrue(loss);
 }
 @Test public void restorationRequiresConsecutiveSourceSupportAndSpatialContinuity(){
  var gate=new RestorationGate140();gate.begin(true);var box=seed();
  assertFalse(gate.observe(true,true,box));assertFalse(gate.observe(true,true,box));
  assertFalse("An unsupported image resets confirmation",gate.observe(true,false,box));
  assertFalse(gate.observe(true,true,box));assertFalse(gate.observe(true,true,box));assertTrue(gate.observe(true,true,box));
  gate.begin(true);gate.observe(true,true,box);gate.observe(true,true,box);
  var jumped=new PixelTracker.Box(.65,box.y(),box.w(),box.h());assertFalse("A distant candidate starts a new streak",gate.observe(true,true,jumped));
  assertFalse(gate.observe(true,true,jumped));assertTrue(gate.observe(true,true,jumped));
 }
 @Test public void lossPermanentlyDisablesContinuityRestorationForThatEpisode(){
  var gate=new RestorationGate140();gate.begin(true);gate.observe(true,true,seed());
  assertFalse(gate.observe(false,true,seed()));for(int i=0;i<5;i++)assertFalse(gate.observe(true,true,seed()));
  assertFalse(gate.evidence().getBoolean("eligible"));gate.begin(false);
  for(int i=0;i<5;i++)assertFalse("A hard-warning episode cannot take the continuity path",gate.observe(true,true,seed()));
 }
 @Test public void elapsedWindowNeverReleasesAQuarantinedObservation(){
  var gate=new RestorationGate140();gate.begin(true);for(int i=0;i<12;i++)assertFalse(gate.observe(true,false,seed()));
  for(int i=0;i<5;i++)assertFalse(gate.observe(true,true,seed()));assertFalse(gate.evidence().getBoolean("eligible"));
 }
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
  var first=scene(0,false,false,false);var guarded=new CorroboratedGuard140("body","object",first,seed(),true);var monitor=new CorroboratedGuard140("body","object",first,seed(),false);var baseline=new FlowTracker("body","object",first,seed());
  for(int i=1;i<=4;i++){
   var frame=scene(i,i>=3,false,false);var g=guarded.update(frame);var m=monitor.update(frame);var original=baseline.update(frame);var meta=(JSONObject)m.remove("drift140");
   assertTrue(m.similar(original));assertFalse("Synthetic motion must remain accepted by incumbent",original.isNull("box"));
   assertEquals(0,meta.getInt("automaticAdmissions"));
   if(i<3)assertFalse(g.toString(),g.isNull("box"));else{assertTrue(g.toString(),g.isNull("box"));assertTrue(g.getJSONObject("drift140").getBoolean("suspect"));assertFalse(new ObservationFilter(0,seed()).step(i,g).getBoolean("accepted"));}
  }
 }
 @Test public void completeOcclusionRequiresTwoUniqueReturnImagesAndTwinsStayWithheld(){
  var first=scene(0,false,false,false);var g=new CorroboratedGuard140("body","object",first,seed(),true);
  assertTrue(g.update(scene(0,false,true,false)).isNull("box"));assertTrue(g.update(scene(0,false,true,false)).isNull("box"));
  assertTrue(g.update(scene(100,false,false,false)).isNull("box"));var recovered=g.update(scene(100,false,false,false));
  assertFalse(recovered.toString(),recovered.isNull("box"));assertTrue(recovered.getJSONObject("drift140").getJSONObject("recovery").getBoolean("accepted"));
  var twins=new CorroboratedGuard140("body","object",first,seed(),true);twins.update(scene(0,false,true,false));
  for(int i=0;i<3;i++)assertTrue(twins.update(scene(0,false,false,true)).isNull("box"));
 }
 @Test public void replayIsDeterministicAndSourceDimensionsAndDefaultsRemainChecked(){
  var a=new CorroboratedGuard140("body","object",scene(0,false,false,false),seed(),true);var b=new CorroboratedGuard140("body","object",scene(0,false,false,false),seed(),true);
  for(int i=0;i<8;i++){var image=scene(i,i>=4,false,false);assertTrue(a.update(image).similar(b.update(image)));}
  assertThrows(IllegalArgumentException.class,()->a.update(new BufferedImage(321,220,BufferedImage.TYPE_INT_RGB)));
  assertEquals("legacy-flow",ObservationMethods129.policy(new JSONObject()));
 }
 @Test public void durableStoppedPrefixAndAuditPixelReplaySurviveRestartAndRejectChangedEvidence()throws Exception{
  Path root=Files.createTempDirectory("drift140-");var ledger=new WorldLedger(root);String world=ledger.create(WorldLedgerTest.source(WorldLedgerTest.suite().getJSONArray("cases").getJSONObject(0)),"Drift control").getString("id");
  JSONObject run,audit,label,dataset;byte[] state;
  try(var s=new WorldObservationStore(root,ledger)){
   var pack=WorldObservationStoreTest.pack();dataset=WorldObservationStoreTest.importPack(s,pack);
   String id=s.start(WorldObservationStoreTest.arguments(ledger,world,dataset,0).put("trackerPolicy","corroborated-guard-140")).getString("id");s.stop(id);var prefix=s.read(id).getJSONArray("trace");s.resume(id);run=WorldObservationStoreTest.settled(s,id);assertEquals(run.toString(),"completed",run.getString("status"));
   for(int i=0;i<prefix.length();i++)assertTrue(prefix.getJSONObject(i).similar(run.getJSONArray("trace").getJSONObject(i)));
   var rows=new JSONArray();for(int i=1;i<9;i++){var f=dataset.getJSONArray("frames").getJSONObject(i);rows.put(new JSONObject().put("ordinal",i).put("sourceFrame",f.getInt("sourceFrame")).put("imageSha256",f.getString("imageSha256")).put("visibility","visible").put("targetBox",pack.getJSONArray("objects").getJSONObject(0).getJSONArray("box")).put("note","Independent static fixture geometry"));}
   var input=new JSONObject().put("schema",AssociationAudit134.VERSION).put("datasetId",dataset.getString("id")).put("datasetSha256",WorldFrames.hash(dataset)).put("objectKey","subject").put("annotator","Generated independent geometry").put("method","synthetic-control").put("exposure","before-tracking").put("provenance","Fixed placements, not predictions").put("rows",rows);
   var service=new AssociationAudit134(root,s);label=service.save(input);audit=service.audit(new JSONObject().put("labelId",label.getString("id")).put("runId",id));assertEquals(8,audit.getJSONObject("drift140").getInt("replayedFrames"));
   var fake=WorldFrames.copy(run);fake.getJSONArray("trace").getJSONObject(0).getJSONArray("objects").getJSONObject(0).getJSONObject("tracker").getJSONObject("drift140").put("anchorSha256","forged");
   String body=run.getJSONArray("bindings").getJSONObject(0).getString("bodyId");
   assertThrows(IllegalStateException.class,()->CorroboratedAudit140.check(fake,dataset,label.getJSONObject("input"),body,i->ImageIO.read(root.resolve("world-observations/datasets/"+dataset.getString("id")+"/"+i+".image").toFile())));
   state=Files.readAllBytes(root.resolve("world-observations/state.json"));
  }
  try(var s=new WorldObservationStore(root,new WorldLedger(root))){assertArrayEquals(state,Files.readAllBytes(root.resolve("world-observations/state.json")));assertTrue(run.similar(s.read(run.getString("id"))));var service=new AssociationAudit134(root,s);assertTrue(audit.similar(service.readAudit(audit.getString("id"))));
   Path file=root.resolve("world-observations/datasets/"+dataset.getString("id")+"/4.image");byte[] original=Files.readAllBytes(file),bad=original.clone();bad[20]^=1;Files.write(file,bad);assertThrows(IllegalStateException.class,()->service.readAudit(audit.getString("id")));Files.write(file,original);assertTrue(audit.similar(service.readAudit(audit.getString("id"))));}
 }

 static BufferedImage covered(int columns){
  var image=scene(0,false,false,false);var other=new Random(9139);for(int y=0;y<120;y++)for(int x=0;x<90;x++){int q=other.nextInt(70);if(x<columns)image.setRGB(40+x,35+y,((20+q/2)<<16)|((150+q/2)<<8)|(20+q/2));}return image;
 }
 @Test public void gradualSameColorCoverCannotUseColorAgreementAsSpatialProof(){
  var first=covered(0);var base=new FlowTracker("body","object",first,seed());var old=new DriftGuard138("body","object",first,seed(),true);var guarded=new CorroboratedGuard140("body","object",first,seed(),true);int warnings=0,oldAccepted=0;
  for(int i=1;i<=12;i++){
   var frame=covered(Math.min(90,i*9));var b=base.update(frame);var o=old.update(frame);var g=guarded.update(frame);var m=g.getJSONObject("drift140");
   if(i>=10&&!b.isNull("box")&&!o.isNull("box")){oldAccepted++;if(g.isNull("box")&&m.getBoolean("suspect"))warnings++;}
  }
  assertTrue("Control must still accept the fully replaced same-color object",oldAccepted>0);assertTrue("Spatial structure must expose accepted replacement",warnings>0);
 }
 @Test public void persistentPartLocationsMeasureGradualBoxMisalignment(){
  var evidence=new SpatialEvidence139(scene(0,false,false,false),seed());JSONObject last=null;
  for(int i=1;i<=12;i++)last=evidence.assess(scene(i*2,false,false,false),seed());
  assertTrue(last.toString(),last.getBoolean("supported"));assertTrue(last.toString(),last.getDouble("alignmentError")>.18);assertTrue(last.getJSONArray("candidateReasons").toString().contains("Seed parts"));
  var inferred=PixelTracker.Box.from(last.getJSONArray("witnessBox"));assertEquals(seed().x()+24/320.,inferred.x(),.025);assertEquals(0,last.getInt("automaticAdmissions"));
 }
 @Test public void uniformColorDoesNotInventTexturedPartEvidence(){
  var image=new BufferedImage(320,220,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();g.setColor(new java.awt.Color(40,90,40));g.fillRect(0,0,320,220);g.dispose();
  var evidence=new SpatialEvidence139(image,seed());var result=evidence.assess(image,seed());assertEquals(0,result.getInt("eligibleParts"));assertFalse(result.getBoolean("supported"));assertTrue(result.getJSONArray("candidateReasons").isEmpty());
  var full=new SpatialEvidence139(scene(0,false,false,false),new PixelTracker.Box(0,0,1,1)).assess(scene(0,false,false,false),new PixelTracker.Box(0,0,1,1));assertEquals(0,full.getInt("sourceBackgroundParts"));assertEquals(0,full.getInt("eligibleParts"));
 }

 static BufferedImage movingBackground(int camera,int relative){
  var image=new BufferedImage(320,220,BufferedImage.TYPE_INT_RGB);var random=new Random(7139);
  for(int y=0;y<220;y++)for(int x=0;x<320;x++){int q=60+random.nextInt(130),at=x+camera;if(at<320)image.setRGB(at,y,(q<<16)|(q<<8)|q);}
  var object=scene(camera+relative,false,false,false);for(int y=35;y<155;y++)for(int x=40+camera+relative;x<130+camera+relative;x++)image.setRGB(x,y,object.getRGB(x,y));return image;
 }
 @Test public void relativeForegroundMotionIsSeparatedFromCameraAndBackgroundMotion(){
  var mismatch=new SpatialEvidence139(movingBackground(0,0),seed());var cameraOnly=new SpatialEvidence139(movingBackground(0,0),seed());JSONObject last=null;
  for(int i=1;i<=4;i++){
   var b=new PixelTracker.Box(seed().x()+2*i/320.,seed().y(),seed().w(),seed().h());
   var benign=cameraOnly.assess(movingBackground(2*i,0),b);assertTrue(benign.toString(),benign.getJSONArray("candidateReasons").isEmpty());
   last=mismatch.assess(movingBackground(2*i,8*i),b);
  }
  assertTrue(last.toString(),last.getBoolean("supported"));assertTrue(last.toString(),last.getInt("backgroundPoints")>=3);
  assertEquals(2/320.,last.getJSONArray("backgroundMotion").getDouble(0),.01);
  assertEquals(10/320.,last.getJSONArray("foregroundMotion").getDouble(0),.01);
  assertTrue(last.toString(),last.getJSONArray("candidateReasons").toString().contains("background"));
 }
}
