package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class VerifiedMemoryFlow137Test {
 static JSONObject fixture(double angle)throws Exception{
  var d=new JSONArray(PixelTracker.descriptor(PixelTracker.working(AppearanceFlow135Test.view(angle,40,50,false,false)),AppearanceFlow135Test.seed()));
  return new JSONObject().put("examples",new JSONArray().put(new JSONObject().put("descriptor",d).put("descriptorSha256",WorldFrames.hash(new JSONObject().put("values",d))).put("ordinal",1).put("imageSha256","synthetic-independent-pose")));
 }
 @Test public void knownPoseRecoversAfterLossButSeedOnlyCannotAndTwinsAreRefused()throws Exception{
  var first=AppearanceFlow135Test.view(0,40,50,false,false);var memory=fixture(90);
  var candidate=new VerifiedMemoryFlow137("x","object",first,AppearanceFlow135Test.seed(),memory,true);
  var control=new VerifiedMemoryFlow137("x","object",first,AppearanceFlow135Test.seed(),memory,false);
  for(int i=0;i<3;i++){var hidden=AppearanceFlow135Test.view(0,40,50,true,false);assertTrue(candidate.update(hidden).isNull("box"));assertTrue(control.update(hidden).isNull("box"));}
  boolean recovered=false;
  for(int i=0;i<3;i++){var returning=AppearanceFlow135Test.view(90,330,165,false,false);var output=candidate.update(returning);var r=output.getJSONObject("reference137").getJSONObject("recovery");if(r.getBoolean("accepted")){assertEquals("synthetic-independent-pose:1",r.getString("sourceKey"));assertTrue(PixelTracker.iou(PixelTracker.Box.from(output.getJSONArray("box")),new PixelTracker.Box(330/480.,165/300.,100/480.,100/300.))>.7);recovered=true;}assertTrue(control.update(returning).isNull("box"));}
  assertTrue("Reference must cause actual recovery",recovered);
  var twins=new VerifiedMemoryFlow137("x","object",first,AppearanceFlow135Test.seed(),memory,true);
  for(int i=0;i<3;i++)twins.update(AppearanceFlow135Test.view(0,40,50,true,false));
  for(int i=0;i<3;i++){var r=twins.update(AppearanceFlow135Test.view(90,40,50,false,true));assertTrue(r.toString(),r.isNull("box"));assertFalse(r.getJSONObject("reference137").getJSONObject("recovery").getBoolean("accepted"));}
 }
 @Test public void acceptedIncumbentRemainsExactAndMemoryNeverGrows()throws Exception{
  var first=AppearanceFlow135Test.view(0,40,50,false,false);var memory=fixture(90);var tracker=new VerifiedMemoryFlow137("x","object",first,AppearanceFlow135Test.seed(),memory,true);var baseline=new FlowTracker("x","object",first,AppearanceFlow135Test.seed());
  for(int i=1;i<10;i++){var pixels=AppearanceFlow135Test.view(i,40+i,50,false,false);var result=tracker.update(pixels);var meta=(JSONObject)result.remove("reference137");assertTrue(result.similar(baseline.update(pixels)));assertEquals(2,meta.getInt("bankSize"));assertEquals(0,meta.getInt("automaticAdmissions"));}
 }
 @Test public void alteredImportedDescriptorFailsBeforeTracking()throws Exception{
  var memory=fixture(90);memory.getJSONArray("examples").getJSONObject(0).getJSONArray("descriptor").put(0,999);
  var first=AppearanceFlow135Test.view(0,40,50,false,false);assertThrows(IllegalArgumentException.class,()->new VerifiedMemoryFlow137("x","object",first,AppearanceFlow135Test.seed(),memory,true));assertEquals("legacy-flow",ObservationMethods129.policy(new JSONObject()));
 }
}
