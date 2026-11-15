package fraymus.workspace;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static fraymus.workspace.AppearanceConfirmation143Test.*;

public class ReturnConfirmation148Test {
 static BufferedImage frame(int i,boolean copyOnly){
  boolean hidden=i>=10&&(copyOnly||i<=11);
  var image=scene(32+2*i,48,80,i<=6?143819:143031,hidden,false,false);
  if(i>=7){var other=scene(234,148,68,143031,false,false,false);var g=image.createGraphics();g.drawImage(other.getSubimage(234,148,68,68),234,148,null);g.dispose();}
  return image;
 }
 static JSONObject memory(){return AmbiguityGuard144Test.donor();}
 @Test public void measuredSeparatedReturnAddsRealRecoveryAndMonitorIsExact()throws Exception{
  var parent=new ReturnContinuity146("body","object",frame(0,false),seed(),memory(),true);
  var monitor=new ReturnConfirmation148("body","object",frame(0,false),seed(),memory(),false);
  var candidate=new ReturnConfirmation148("body","object",frame(0,false),seed(),memory(),true);
  var fa=new ObservationFilter(0,seed());var fb=new ObservationFilter(0,seed());int added=0,returned=0;var trace=new JSONArray();
  for(int i=1;i<=20;i++){
   var a=parent.update(frame(i,false));var b=monitor.update(frame(i,false));var c=candidate.update(frame(i,false));trace.put(c);
   assertTrue(fa.step(i,a).similar(fb.step(i,b)));b.remove("confirmation148");assertTrue(a.similar(b));
   if(!a.isNull("box")){var preserved=ParsedJson137.copy(c);preserved.remove("confirmation148");assertTrue(a.similar(preserved));}
   if(c.getJSONObject("confirmation148").getBoolean("restored")){
    added++;if(i>=12)returned++;assertFalse(i==10||i==11);assertTrue(PixelTracker.iou(PixelTracker.Box.from(c.getJSONArray("box")),new PixelTracker.Box((32+2*i)/320.,.2,.25,1./3))>=.5);
    var h=c.getJSONObject("confirmation148").getJSONObject("chosen");assertEquals(3,h.getInt("confirmations"));assertTrue(h.getBoolean("coherentMotion"));assertEquals(6,h.getJSONObject("path").getJSONObject("target").getInt("anchorOrdinal"));
   }
  }
  Files.createDirectories(Path.of("build/return148-development"));Files.writeString(Path.of("build/return148-development/legitimate.json"),trace.toString(2));
  assertTrue("Must add pixel-level return after cover, not merely refuse all",returned>0);assertTrue(added>0);
  var replay=new ReturnConfirmation148("body","object",frame(0,false),seed(),memory(),true);for(int i=1;i<=20;i++)assertTrue(trace.getJSONObject(i-1).similar(replay.update(frame(i,false))));
 }
 @Test public void copyOnlyDoesNotAcquireTargetIdentity(){
  var tracker=new ReturnConfirmation148("body","object",frame(0,true),seed(),memory(),true);
  for(int i=1;i<=24;i++){var t=tracker.update(frame(i,true));if(i>=10)assertFalse(t.toString(),t.getJSONObject("confirmation148").getBoolean("restored"));}
 }
 @Test public void original146WrongObjectRegressionStaysRefused(){
  var tracker=new ReturnConfirmation148("body","object",ReturnContinuity146Test.exposed(0,true),seed(),ReturnContinuity146Test.memory(),true);
  for(int i=1;i<24;i++){var t=tracker.update(ReturnContinuity146Test.exposed(i,true));if(i>=10)assertTrue(t.toString(),t.isNull("box"));}
 }
 @Test public void independentAuditRejectsChangedConfirmationAndSource()throws Exception{
  var tracker=new ReturnConfirmation148("body","object",frame(0,false),seed(),memory(),true);var trace=new JSONArray();var rows=new JSONArray();var filter=new ObservationFilter(0,seed());
  for(int i=1;i<=20;i++){
   var t=tracker.update(frame(i,false));trace.put(new JSONObject().put("objects",new JSONArray().put(new JSONObject().put("bodyId","body").put("tracker",t).put("estimate",filter.step(i,t)))));
   var l=new JSONObject().put("ordinal",i).put("visibility",i==10||i==11?"occluded":"visible");if(i!=10&&i!=11)l.put("targetBox",new PixelTracker.Box((32+2*i)/320.,.2,.25,1./3).json());rows.put(l);
  }
  var data=new JSONObject().put("objects",new JSONArray().put(new JSONObject().put("key","subject").put("label","object").put("box",seed().json())));
  var run=new JSONObject().put("trackerPolicy","path-confirmation-148").put("referenceMemory137",memory()).put("trace",trace);var labels=new JSONObject().put("objectKey","subject").put("rows",rows);
  var audit=ReturnAudit148.check(run,data,labels,"body",i->frame(i,false));assertTrue(audit.toString(),audit.getJSONObject("additional").getInt("correct")>0);assertEquals(0,audit.getJSONObject("additional").getInt("incorrect"));
  assertThrows(IllegalStateException.class,()->ReturnAudit148.check(run,data,labels,"body",i->frame(i,true)));
  trace.getJSONObject(13).getJSONArray("objects").getJSONObject(0).getJSONObject("tracker").getJSONObject("confirmation148").put("maximumConfirmations",99);
  assertThrows(IllegalStateException.class,()->ReturnAudit148.check(run,data,labels,"body",i->frame(i,false)));
 }
 @Test public void runtimeSourceAndExportIntegrityRemainMandatory()throws Exception{
  try(var f=new VerifiedAppearances135Test.Fixture()){
   var set=f.service.retain(f.audit.getString("id"));int rev=f.observations.state(f.world).getJSONObject("estimate").getInt("revision");
   var args=WorldObservationStoreTest.arguments(f.ledger,f.world,f.dataset,rev).put("trackerPolicy","path-confirmation-148").put("appearanceSet137",set.getString("id")).put("referenceDeclaration137","Separate generated test source");
   var run=WorldObservationStoreTest.settled(f.observations,f.observations.start(args).getString("id"));var labels=f.association.save(AssociationAudit134Test.input(f.dataset));var audit=f.association.audit(new JSONObject().put("labelId",labels.getString("id")).put("runId",run.getString("id")));
   assertEquals(run.getInt("totalFrames"),audit.getJSONObject("confirmation148").getInt("replayedFrames"));
   var pixel=f.root.resolve("world-observations/datasets/"+f.data.getString("id")+"/12.image");var original=Files.readAllBytes(pixel);var bad=original.clone();bad[20]^=1;Files.write(pixel,bad);
   try{assertThrows(IllegalStateException.class,()->f.association.readAudit(audit.getString("id")));assertThrows(IllegalStateException.class,()->f.observations.export(run.getString("id")));}finally{Files.write(pixel,original);}
   assertTrue(audit.similar(f.association.readAudit(audit.getString("id"))));
  }
 }
 @Test public void expiredCrossingUnmeasuredAndOverflowPathsRefuse(){
  var g=new ReturnGate148(TrajectoryRecovery145Test.box(0));for(int i=1;i<=5;i++)g.step(TrajectoryRecovery145Test.event(i,true,TrajectoryRecovery145Test.box(i)),new JSONArray(),new JSONObject());
  for(int i=6;i<=20;i++){
   var b=TrajectoryRecovery145Test.box(i);var r=g.step(TrajectoryRecovery145Test.event(i,false,b),TrajectoryRecovery145Test.location(TrajectoryRecovery145Test.rival()),new JSONObject().put("box",b.json()).put("unique",false));
   assertEquals(5,r.getJSONObject("target").getInt("anchorOrdinal"));if(i==6||i>=18)assertFalse(r.getBoolean("qualified"));if(i==7)assertTrue(r.getBoolean("qualified"));
  }
  var unknown=new ReturnGate148(seed());assertFalse(unknown.step(TrajectoryRecovery145Test.event(1,false,seed()),new JSONArray(),new JSONObject().put("box",seed().json()).put("unique",true)).getBoolean("qualified"));
  assertEquals("legacy-flow",ObservationMethods129.policy(new JSONObject()));
 }
 @Test public void crossingAndOverflowCannotBeForgotten(){
  var cross=new ReturnGate148(TrajectoryRecovery145Test.box(0));
  var overflow=new ReturnGate148(TrajectoryRecovery145Test.box(0));
  for(int i=1;i<=5;i++)for(var g:new ReturnGate148[]{cross,overflow})g.step(TrajectoryRecovery145Test.event(i,true,TrajectoryRecovery145Test.box(i)),new JSONArray(),new JSONObject());
  var near=new PixelTracker.Box(.27,.2,.15,.2);var b=TrajectoryRecovery145Test.box(6);
  cross.step(TrajectoryRecovery145Test.event(6,false,b),TrajectoryRecovery145Test.location(near),new JSONObject().put("box",b.json()).put("unique",false));
  b=TrajectoryRecovery145Test.box(7);var r=cross.step(TrajectoryRecovery145Test.event(7,false,b),TrajectoryRecovery145Test.location(b),new JSONObject().put("box",b.json()).put("unique",true));
  assertTrue(r.getBoolean("conflict"));assertFalse(r.getBoolean("qualified"));
  r=cross.step(TrajectoryRecovery145Test.event(8,false,b),new JSONArray(),new JSONObject().put("box",b.json()).put("unique",true));assertTrue(r.getBoolean("conflict"));assertFalse(r.getBoolean("qualified"));
  var many=new JSONArray();for(int i=0;i<9;i++)many.put(new JSONObject().put("box",new PixelTracker.Box(.05+(i%3)*.3,.48+(i/3)*.17,.06,.06).json()));
  r=overflow.step(TrajectoryRecovery145Test.event(6,false,b),many,new JSONObject().put("box",b.json()).put("unique",true));assertTrue(r.getBoolean("conflict"));assertFalse(r.getBoolean("qualified"));assertEquals(8,r.getJSONArray("rivals").length());
 }
 @Test public void invalidOrDuplicatedAppearanceCannotEnterNewSearch(){
  var m=memory();m.getJSONArray("examples").getJSONObject(0).getJSONArray("descriptor").put(0,2);
  assertThrows(IllegalArgumentException.class,()->new ReturnConfirmation148("body","object",frame(0,false),seed(),m,true));
  var duplicates=AppearanceConfirmation143Test.memory(143031);duplicates.getJSONArray("examples").put(ParsedJson137.copy(duplicates.getJSONArray("examples").getJSONObject(0)));
  assertThrows(IllegalArgumentException.class,()->new ReturnSearch148(frame(0,false),seed(),duplicates,true));
 }
}
