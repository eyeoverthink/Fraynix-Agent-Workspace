package fraymus.workspace;
import org.junit.Test;
import org.json.*;
import static org.junit.Assert.*;
import java.nio.file.*;

public class Tracking130Test {
 @Test public void sourceCameraMatchesIndependentForwardProjection()throws Exception{try(var in=getClass().getResourceAsStream("/observation129-camera.json")){var data=new JSONObject(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));var plane=new PlaneCalibration(data.getJSONObject("geometry"));for(Object raw:data.getJSONArray("points")){var p=(JSONObject)raw;var image=p.getJSONArray("image");assertArrayEquals(PlaneCalibration.point(p.getJSONArray("worldM")),plane.project(image.getDouble(0),image.getDouble(1)),1e-9);}}}
 @Test public void hiddenAndDuplicateObjectsHaveNoInventedCurrentBox()throws Exception{
  var photo=FlowTrackerTest.photo();var first=FlowTrackerTest.scene(photo,40,50,100,false,false);var box=new PixelTracker.Box(40/480.,50/300.,100/480.,100/300.);
  assertTrue(new ObjectFlow130("x","person",first,box).update(FlowTrackerTest.scene(photo,40,50,100,true,false)).isNull("box"));
  assertTrue(new ObjectFlow130("x","person",first,box).update(FlowTrackerTest.scene(photo,40,50,100,false,true)).isNull("box"));
 }
 @Test public void actualMotionStillRequiresPixelEvidence()throws Exception{
  var photo=FlowTrackerTest.photo();var tracker=new ObjectFlow130("x","person",FlowTrackerTest.scene(photo,40,50,100,false,false),new PixelTracker.Box(40/480.,50/300.,100/480.,100/300.));double overlap=0;
  for(int i=1;i<=16;i++){var result=tracker.update(FlowTrackerTest.scene(photo,40+4*i,50+i,100+i,false,false));assertFalse(result.toString(),result.isNull("box"));overlap+=PixelTracker.iou(new PixelTracker.Box((40+4*i)/480.,(50+i)/300.,(100+i)/480.,(100+i)/300.),PixelTracker.Box.from(result.getJSONArray("box")));}assertTrue(overlap/16>.7);
 }
 @Test public void explicitChoiceSurvivesRecoveryAndNeverChangesDefault()throws Exception{
  assertEquals(ObservationMethods129.DEFAULT,ObservationMethods129.policy(new JSONObject()));assertThrows(IllegalArgumentException.class,()->ObservationMethods129.policy(new JSONObject().put("trackerPolicy","unknown")));
  var root=Files.createTempDirectory("tracker130");var ledger=new WorldLedger(root);String world=ledger.create(WorldLedgerTest.source(WorldLedgerTest.suite().getJSONArray("cases").getJSONObject(0)),"129 ownership control").getString("id");JSONObject run;
  try(var store=new WorldObservationStore(root,ledger)){var data=WorldObservationStoreTest.importPack(store,WorldObservationStoreTest.pack());String id=store.start(WorldObservationStoreTest.arguments(ledger,world,data,0).put("trackerPolicy","object-flow-130")).getString("id");store.stop(id);store.resume(id);run=WorldObservationStoreTest.settled(store,id);assertEquals("completed",run.getString("status"));assertEquals("object-flow-130",run.getString("trackerPolicy"));assertEquals(1,run.getInt("resumes"));assertEquals(64,run.getString("codeSha256").length());}
  try(var restarted=new WorldObservationStore(root,new WorldLedger(root))){assertTrue(run.similar(restarted.read(run.getString("id"))));}
 }
}
