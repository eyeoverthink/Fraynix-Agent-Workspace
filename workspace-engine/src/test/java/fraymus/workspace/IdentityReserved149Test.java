package fraymus.workspace;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static fraymus.workspace.AppearanceConfirmation143Test.*;

/** Reserved generated controls. Execute only after the149 implementation/protocol commit. Not camera evidence. */
public class IdentityReserved149Test {
 static JSONObject refs(int texture){var m=memory(texture);m.getJSONArray("examples").getJSONObject(0).put("structure149",StructureSearch149.descriptor(scene(32,48,80,texture,false,false,false),seed()));return m;}
 static BufferedImage frame(int seed,int i,boolean wrong,boolean repeated){
  boolean hidden=i>=5&&i<=22||repeated&&i==29;int x=i<5?32+2*i:128+2*(i-23);
  return scene(x,48,80,i<5?seed+50:wrong?seed+1:seed,hidden,false,false);
 }
 @Test public void exposedFirstCohortRegression()throws Exception{runSuite(new int[]{149101,149211,149307},"identity149-exposed-v2");}
 @Test public void reservedTexturesCoverReturnWrongPatternAndSecondLoss()throws Exception{runSuite(new int[]{149401,149509,149601},"identity149-reserved-v2");}
 static void runSuite(int[] textures,String directory)throws Exception{
  var cases=new JSONArray();int totalAdded=0;
  for(int seed:textures)for(String kind:new String[]{"return","wrong-pattern","second-loss"}){
   boolean wrong=kind.equals("wrong-pattern"),repeated=kind.equals("second-loss");var t=new IdentityReturn149("body","reserved generated object",frame(seed,0,wrong,repeated),seed(),refs(seed),true);
   int visible=0,hidden=0,correct=0,incorrect=0,added=0,hiddenOutputs=0;var trace=new JSONArray();
   for(int i=1;i<=35;i++){
    var out=t.update(frame(seed,i,wrong,repeated));trace.put(out);var q=out.getJSONObject("identity149");boolean targetVisible=!(i>=5&&i<=22||repeated&&i==29)&&!(wrong&&i>=23);
    if(targetVisible)visible++;else hidden++;
    if(!out.isNull("box")){if(!targetVisible)hiddenOutputs++;else if(PixelTracker.iou(PixelTracker.Box.from(out.getJSONArray("box")),new PixelTracker.Box((i<5?32+2*i:128+2*(i-23))/320.,.2,.25,1./3))>=.5)correct++;else incorrect++;}
    if(q.getBoolean("restored")){added++;assertTrue("New method must not accept the wrong pattern or hidden target",targetVisible);assertTrue(PixelTracker.iou(PixelTracker.Box.from(out.getJSONArray("box")),new PixelTracker.Box((128+2*(i-23))/320.,.2,.25,1./3))>=.5);}
    if(repeated&&i>=30&&i<33)assertFalse("Second cover must reset confirmation",q.getBoolean("restored"));
    if(repeated&&i>=33&&!q.isNull("chosen"))assertTrue(q.getJSONObject("chosen").getJSONObject("segment").getInt("startOrdinal")>=30);
   }
   totalAdded+=added;cases.put(new JSONObject().put("textureSeed",seed).put("case",kind).put("visible",visible).put("hidden",hidden).put("correct",correct).put("incorrect",incorrect).put("hiddenOutputs",hiddenOutputs).put("additional",added).put("trace",trace));
  }
  var result=new JSONObject().put("schema","reserved-generated-149-v1").put("cases",cases).put("additional",totalAdded).put("scope","Generated images with reserved textures and exact rendered boxes. Not real footage; no semantic identity or physical calibration claim.");
  var dir=Path.of("build",directory);Files.createDirectories(dir);var file=dir.resolve("result.json");
  if(Files.exists(file))assertTrue("Repeated execution must preserve first reserved result",new JSONObject(Files.readString(file)).similar(result));else Files.writeString(file,result.toString(2));
  assertTrue("Require useful recovery in at least one reserved generated sequence",totalAdded>0);
 }
}
