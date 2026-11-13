package fraymus.workspace;
import org.junit.Test;
import org.json.*;
import static org.junit.Assert.*;
import java.nio.file.*;

public class Tracking131Test {
 @Test public void producerValidityMatchesStrictConsumerIncludingFloatingEndpoints(){
  for(double x:new double[]{0,.1,.7,.975})for(double w:new double[]{.0249,.025,.0251,.1,Double.NaN}){
   var b=new PixelTracker.Box(x,.2,w,.05);boolean parsed;
   try{PixelTracker.Box.from(b.json());parsed=true;}catch(Exception expected){parsed=false;}
   assertEquals(b.toString(),parsed,b.measurable());
  }
  assertThrows(IllegalArgumentException.class,()->PixelTracker.Box.from(new JSONArray(new double[]{.1,.1,.124,.3})));
 }
 @Test public void smallShrinkingTargetsNeverEmitIllegalBoxes()throws Exception{
  var photo=FlowTrackerTest.photo();var initial=FlowTrackerTest.scene(photo,40,50,20,false,false);var b=new PixelTracker.Box(40/480.,50/300.,20/480.,20/300.);
  ImageTracker[] trackers={new FlowTracker("x","target",initial,b),new ScaleAwareFlow129("x","target",initial,b),new ObjectFlow130("x","target",initial,b),new RenewingFlow131("x","target",initial,b),new AdaptiveTracker("x","target",initial,b),new PixelTracker("x","target",initial,b,false)};
  for(var tracker:trackers)for(int size:new int[]{18,16,14,12,10,8,6}){
   var r=tracker.update(FlowTrackerTest.scene(photo,40,50,size,false,false));
   if(!r.isNull("box"))assertTrue(PixelTracker.Box.from(r.getJSONArray("box")).measurable());
   else assertEquals("lost",r.getString("status"));
   assertTrue(PixelTracker.Box.from(r.getJSONArray("lastKnownBox")).measurable());
  }
 }
 @Test public void lossBudgetAndDuplicateRefusalDoNotInventMeasurements()throws Exception{
  var photo=FlowTrackerTest.photo();var first=FlowTrackerTest.scene(photo,40,50,100,false,false);var b=new PixelTracker.Box(40/480.,50/300.,100/480.,100/300.);
  var tracker=new RenewingFlow131("x","target",first,b);
  for(int i=0;i<28;i++){var r=tracker.update(FlowTrackerTest.scene(photo,40,50,100,true,false));assertTrue(r.toString(),r.isNull("box"));assertEquals(Math.min(24,i+1),r.getJSONObject("motion").getInt("recoveryAttempts"));}
  assertTrue(new RenewingFlow131("x","target",first,b).update(FlowTrackerTest.scene(photo,40,50,100,false,true)).isNull("box"));
 }
 @Test public void disappearanceAndReturnRequireActualPixels()throws Exception{
  var photo=FlowTrackerTest.photo();var b=new PixelTracker.Box(40/480.,50/300.,100/480.,100/300.);var tracker=new RenewingFlow131("x","target",FlowTrackerTest.scene(photo,40,50,100,false,false),b);
  for(int i=0;i<3;i++)assertTrue(tracker.update(FlowTrackerTest.scene(photo,40,50,100,true,false)).isNull("box"));
  var recovered=tracker.update(FlowTrackerTest.scene(photo,44,51,100,false,false));assertFalse(recovered.toString(),recovered.isNull("box"));assertEquals("reacquired",recovered.getString("status"));assertTrue(PixelTracker.iou(b,PixelTracker.Box.from(recovered.getJSONArray("box")))>.7);
 }
 @Test public void explicitChoiceSurvivesRestartWithSourceChecks()throws Exception{
  assertEquals("legacy-flow",ObservationMethods129.policy(new JSONObject()));
  var root=Files.createTempDirectory("tracker131");var ledger=new WorldLedger(root);String world=ledger.create(WorldLedgerTest.source(WorldLedgerTest.suite().getJSONArray("cases").getJSONObject(0)),"131 ownership control").getString("id");JSONObject run;
  try(var store=new WorldObservationStore(root,ledger)){var data=WorldObservationStoreTest.importPack(store,WorldObservationStoreTest.pack());String id=store.start(WorldObservationStoreTest.arguments(ledger,world,data,0).put("trackerPolicy","renewing-flow-131")).getString("id");store.stop(id);store.resume(id);run=WorldObservationStoreTest.settled(store,id);assertEquals("completed",run.getString("status"));assertEquals("renewing-flow-131",run.getString("trackerPolicy"));assertEquals(1,run.getInt("resumes"));}
  try(var restarted=new WorldObservationStore(root,new WorldLedger(root))){assertTrue(run.similar(restarted.read(run.getString("id"))));}
 }
 @Test public void largerReturnNeedsTwoImageConfirmation()throws Exception{
  var photo=FlowTrackerTest.photo();var b=new PixelTracker.Box(40/480.,50/300.,100/480.,100/300.);var tracker=new RenewingFlow131("x","target",FlowTrackerTest.scene(photo,40,50,100,false,false),b);
  for(int i=0;i<4;i++)assertTrue(tracker.update(FlowTrackerTest.scene(photo,40,50,100,true,false)).isNull("box"));
  var pending=tracker.update(FlowTrackerTest.scene(photo,130,50,100,false,false));assertTrue(pending.toString(),pending.isNull("box"));assertTrue(pending.toString(),pending.getJSONObject("motion").getBoolean("recoveryPending"));
  var recovered=tracker.update(FlowTrackerTest.scene(photo,132,50,100,false,false));assertFalse(recovered.toString(),recovered.isNull("box"));assertEquals("two-frame-recovery",recovered.getJSONObject("motion").getString("method"));assertTrue(PixelTracker.iou(new PixelTracker.Box(132/480.,50/300.,100/480.,100/300.),PixelTracker.Box.from(recovered.getJSONArray("box")))>.7);
 }
}
