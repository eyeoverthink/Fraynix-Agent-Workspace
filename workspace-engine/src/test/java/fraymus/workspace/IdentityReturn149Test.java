package fraymus.workspace;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static fraymus.workspace.AppearanceConfirmation143Test.*;

public class IdentityReturn149Test {
 static JSONObject refs(){var m=memory(143031);m.getJSONArray("examples").getJSONObject(0).put("structure149",StructureSearch149.descriptor(scene(32,48,80,143031,false,false,false),seed()));return m;}
 static BufferedImage frame(int i,boolean wrong){return scene(i<5?32+2*i:112+Math.max(0,i-23),48,80,i<5?143819:wrong?143032:143031,i>=5&&i<23,false,false);}
 @Test public void fullStructureFindsReferenceAndRejectsDifferentObject(){
  var search=new StructureSearch149(frame(0,false),seed(),refs());
  var found=search.search(frame(23,false));assertTrue(found.toString(),found.stream().anyMatch(StructureSearch149.Match::unique));
  assertTrue(search.search(frame(23,true)).stream().noneMatch(StructureSearch149.Match::strong));
 }
 @Test public void longHiddenReturnStartsFreshObservedMotionAndReplays()throws Exception{
  var tracker=new IdentityReturn149("body","object",frame(0,false),seed(),refs(),true);var trace=new JSONArray();int restored=0;
  for(int i=1;i<=29;i++){
   var t=tracker.update(frame(i,false));trace.put(t);var evidence=t.getJSONObject("identity149");
   assertFalse(evidence.getBoolean("physicalActionAuthorized"));assertEquals(0,evidence.getInt("automaticAdmissions"));
   if(i>=5&&i<23)assertFalse(evidence.getBoolean("restored"));
   if(evidence.getBoolean("restored")){restored++;assertTrue(i>=26);var s=evidence.getJSONObject("chosen").getJSONObject("segment");assertTrue(s.getInt("startOrdinal")>=23);assertFalse(s.getBoolean("forecastUsedForAdmission"));assertFalse(s.getJSONObject("nextSample").getBoolean("measured"));
    for(Object raw:s.getJSONArray("observations"))assertTrue(((JSONObject)raw).getInt("ordinal")>=23);
    assertTrue(PixelTracker.iou(PixelTracker.Box.from(t.getJSONArray("box")),new PixelTracker.Box((112+i-23)/320.,.2,.25,1./3))>.7);
   }
  }
  Files.createDirectories(Path.of("build/identity149-development"));Files.writeString(Path.of("build/identity149-development/long-return.json"),trace.toString(2));
  assertTrue("Must recover actual observations after the expired motion horizon",restored>0);
  var replay=new IdentityReturn149("body","object",frame(0,false),seed(),refs(),true);for(int i=1;i<=29;i++)assertTrue(trace.getJSONObject(i-1).similar(replay.update(frame(i,false))));
 }
 @Test public void differentStructureCannotRebuildMotion(){
  var tracker=new IdentityReturn149("body","object",frame(0,true),seed(),refs(),true);
  for(int i=1;i<=29;i++){var t=tracker.update(frame(i,true));if(i>=5)assertFalse(t.getJSONObject("identity149").getBoolean("restored"));}
 }
 @Test public void knownExactCopyCannotBecomeIdentityWhenOriginalDisappears(){
  var first=scene(32,48,80,143031,false,false,true);var tracker=new IdentityReturn149("body","object",first,seed(),refs(),true);
  for(int i=1;i<=6;i++){
   var image=scene(32,48,80,143031,true,false,false);if(i<4){var g=image.createGraphics();g.drawImage(first.getSubimage(8,145,70,70),8,145,null);g.dispose();}
   var t=tracker.update(image);var e=t.getJSONObject("identity149");assertTrue(e.toString(),e.getBoolean("ambiguityRetained"));assertFalse(e.getBoolean("restored"));
  }
 }
 @Test public void monitorKeepsEveryParentOutputAndFilter(){
  var parent=new ReturnConfirmation148("body","object",frame(0,false),seed(),IdentityReturn149.parentMemory(refs()),true);var monitor=new IdentityReturn149("body","object",frame(0,false),seed(),refs(),false);
  var a=new ObservationFilter(0,seed());var b=new ObservationFilter(0,seed());
  for(int i=1;i<=8;i++){var p=parent.update(frame(i,false));var m=monitor.update(frame(i,false));assertTrue(a.step(i,p).similar(b.step(i,m)));m.remove("identity149");assertTrue(p.similar(m));}
  assertEquals("legacy-flow",ObservationMethods129.policy(new JSONObject()));
 }
 @Test public void descriptorsAndFrameDimensionsAreChecked(){
  var m=refs();m.getJSONArray("examples").getJSONObject(0).getJSONObject("structure149").getJSONArray("pixels").put(0,2);
  assertThrows(IllegalArgumentException.class,()->new IdentityReturn149("body","object",frame(0,false),seed(),m,true));
  var search=new StructureSearch149(frame(0,false),seed(),refs());assertThrows(IllegalArgumentException.class,()->search.search(new BufferedImage(640,480,BufferedImage.TYPE_INT_RGB)));
 }
 @Test public void searchNeverReturnsCollapsedOrOutOfBoundsGeometry(){
  var tiny=new PixelTracker.Box(.1,.2,.026,.04);var first=scene(32,48,9,14977,false,false,false);var memory=refs();
  var search=new StructureSearch149(first,tiny,memory);for(var match:search.search(first))assertTrue(match.box().toString(),match.box().measurable());
 }
 @Test public void runtimeRebuildsStructureFromVerifiedOriginalsAndRefusesTampering()throws Exception{
  try(var f=new VerifiedAppearances135Test.Fixture()){
   var set=f.service.retain(f.audit.getString("id"));int revision=f.observations.state(f.world).getJSONObject("estimate").getInt("revision");
   var args=WorldObservationStoreTest.arguments(f.ledger,f.world,f.dataset,revision).put("trackerPolicy","identity-return-149").put("appearanceSet137",set.getString("id")).put("referenceDeclaration137","Separate generated source with independently checked boxes");
   var run=WorldObservationStoreTest.settled(f.observations,f.observations.start(args).getString("id"));
   assertEquals("completed",run.getString("status"));
   for(Object raw:run.getJSONObject("referenceMemory137").getJSONArray("examples")){var e=(JSONObject)raw;var original=WorldObservationStore.decode(f.observations.image(f.data.getString("id"),e.getInt("ordinal")));assertTrue(StructureSearch149.descriptor(original,PixelTracker.Box.from(e.getJSONArray("referenceBox"))).similar(e.getJSONObject("structure149")));}
   var labels=f.association.save(AssociationAudit134Test.input(f.dataset));var audit=f.association.audit(new JSONObject().put("labelId",labels.getString("id")).put("runId",run.getString("id")));
   assertEquals(run.getInt("totalFrames"),audit.getJSONObject("identity149").getInt("replayedFrames"));
   var pixel=f.root.resolve("world-observations/datasets/"+f.data.getString("id")+"/12.image");var original=Files.readAllBytes(pixel);var bad=original.clone();bad[20]^=1;Files.write(pixel,bad);
   try{assertThrows(IllegalStateException.class,()->f.association.readAudit(audit.getString("id")));assertThrows(IllegalStateException.class,()->f.observations.export(run.getString("id")));}finally{Files.write(pixel,original);}
   assertTrue(audit.similar(f.association.readAudit(audit.getString("id"))));
   var forged=ParsedJson137.copy(run);forged.getJSONObject("referenceMemory137").getJSONArray("examples").getJSONObject(0).getJSONObject("structure149").put("sha256","0".repeat(64));
   assertThrows(IllegalStateException.class,()->new ReferenceMemory137(f.root,f.observations).verify(forged));
  }
 }
}
