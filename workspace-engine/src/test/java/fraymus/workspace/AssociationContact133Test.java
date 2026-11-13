package fraymus.workspace;
import org.junit.Test;
import static org.junit.Assert.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import org.json.*;

public class AssociationContact133Test {
 static BufferedImage scene(int x,boolean hidden,boolean distractor,boolean feet){
  var image=new BufferedImage(200,160,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();g.setColor(new Color(0xd0c6b1));g.fillRect(0,0,200,160);
  if(!hidden)draw(g,x,feet,false);if(distractor)draw(g,145,true,true);g.dispose();return image;
 }
 static void draw(Graphics2D g,int x,boolean feet,boolean different){
  g.setColor(new Color(0x303952));g.fillRect(x,30,26,42);g.setColor(new Color(different?0xb54325:0x55aacf));g.fillRect(x+4,34,9,22);g.setColor(new Color(0x171e29));g.fillRect(x+2,72,8,25);g.fillRect(x+16,72,8,25);if(feet){g.fillRect(x,94,12,7);g.fillRect(x+15,94,14,7);}
 }
 static PixelTracker.Box seed(){return new PixelTracker.Box(40/200.,30/160.,29/200.,71/160.);}
 @Test public void followsOriginalAppearancePastDifferentDistractorAndFindsLowerPixels(){
  var t=new AssociationContact133("x","source",scene(40,false,false,true),seed());
  for(int x=42;x<=56;x+=2){var r=t.update(scene(x,false,true,true));assertFalse(r.toString(),r.isNull("box"));assertTrue(PixelTracker.iou(new PixelTracker.Box(x/200.,30/160.,29/200.,71/160.),PixelTracker.Box.from(r.getJSONArray("box")))>.7);var c=r.getJSONObject("contact133");assertEquals(c.toString(),"visual-support",c.getString("status"));assertEquals(101,c.getJSONArray("pointImage").getDouble(1)*160,2);}
 }
 @Test public void fullLossHasNoContactAndRecoveryNeedsTwoVisibleFrames(){
  var t=new AssociationContact133("x","source",scene(40,false,false,true),seed());
  for(int i=0;i<3;i++){var r=t.update(scene(40,true,false,true));assertTrue(r.isNull("box"));assertTrue(r.getJSONObject("contact133").isNull("pointImage"));}
  assertTrue(t.update(scene(44,false,false,true)).isNull("box"));var r=t.update(scene(45,false,false,true));assertFalse(r.toString(),r.isNull("box"));assertEquals("reacquired",r.getString("status"));
 }
 @Test public void equalVisibleCopiesRefuseAssociation(){
  var first=scene(40,false,false,true);var t=new AssociationContact133("x","source",first,seed());var twice=scene(40,false,false,true);var g=twice.createGraphics();draw(g,145,true,false);g.dispose();var r=t.update(twice);assertTrue(r.toString(),r.isNull("box"));assertTrue(r.getJSONObject("association").getBoolean("ambiguous"));
 }
 @Test public void supportDoesNotFallbackToBoxWhenLowerPixelsAreMissing(){
  var first=scene(40,false,false,true);var t=new AssociationContact133("x","source",first,seed());var hidden=scene(40,false,false,true);var g=hidden.createGraphics();g.setColor(new Color(0xd0c6b1));g.fillRect(20,80,80,60);g.dispose();assertTrue(t.support(hidden,seed()).isNull("pointImage"));
 }
 @Test public void dimensionsAndInvalidSeedsRefuse(){
  var t=new AssociationContact133("x","source",scene(40,false,false,true),seed());assertThrows(IllegalArgumentException.class,()->t.update(new BufferedImage(32,32,1)));
  assertThrows(IllegalArgumentException.class,()->new AssociationContact133("x","x",scene(40,false,false,true),new PixelTracker.Box(.1,.1,.01,.1)));
 }
 @Test public void aDifferentLookalikeAfterLossDoesNotAcquireIdentity(){
  var t=new AssociationContact133("x","source",scene(40,false,false,true),seed());t.update(scene(40,true,false,true));
  var other=scene(40,true,false,true);var g=other.createGraphics();draw(g,45,true,true);g.dispose();for(int i=0;i<4;i++)assertTrue(t.update(other).isNull("box"));
 }
 @Test public void clippedLowerSupportIsWithheld(){
  var first=scene(40,false,false,true);var support=new VisualSupport133(first,seed());var cropped=new BufferedImage(200,160,BufferedImage.TYPE_INT_RGB);var g=cropped.createGraphics();g.setColor(new Color(0xd0c6b1));g.fillRect(0,0,200,160);g.translate(0,60);draw(g,40,true,false);g.dispose();assertTrue(support.support(cropped,new PixelTracker.Box(.2,90/160.,29/200.,70/160.)).isNull("pointImage"));
 }
}
