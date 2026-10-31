package fraymus.workspace;

import org.junit.Test;
import static org.junit.Assert.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;

public class FlowTrackerTest {
    static BufferedImage photo() throws Exception {try(var in=FlowTrackerTest.class.getResourceAsStream("/perception/astronaut.png")){return ImageIO.read(in);}}
    static BufferedImage scene(BufferedImage photo,int x,int y,int size,boolean hidden,boolean duplicate){var out=new BufferedImage(480,300,BufferedImage.TYPE_INT_RGB);var g=out.createGraphics();g.setColor(new Color(0x233b50));g.fillRect(0,0,480,300);if(!hidden)g.drawImage(photo,x,y,size,size,null);if(duplicate)g.drawImage(photo,310,40,size,size,null);g.dispose();return out;}
    @Test public void actualPixelMotionTracksTranslationAndScaleWithoutFutureReferences() throws Exception {
        var photo=photo();var initial=new PixelTracker.Box(40/480.,50/300.,100/480.,100/300.);var tracker=new FlowTracker("object","person",scene(photo,40,50,100,false,false),initial);double overlap=0;
        for(int i=1;i<=16;i++){int x=40+i*4,y=50+i,size=100+i;var prediction=tracker.update(scene(photo,x,y,size,false,false));assertFalse(prediction.toString(),prediction.isNull("box"));overlap+=PixelTracker.iou(new PixelTracker.Box(x/480.,y/300.,size/480.,size/300.),PixelTracker.Box.from(prediction.getJSONArray("box")));assertTrue(prediction.getJSONObject("motion").getInt("inliers")>=8);}
        assertTrue("Measured synthetic overlap "+overlap/16,overlap/16>.7);
    }
    @Test public void fullLossAndAnIdenticalSecondCopyHaveNoInventedCurrentBox() throws Exception {
        var photo=photo();var image=scene(photo,40,50,100,false,false);var box=new PixelTracker.Box(40/480.,50/300.,100/480.,100/300.);
        assertTrue(new FlowTracker("x","person",image,box).update(scene(photo,40,50,100,true,false)).isNull("box"));
        var duplicate=scene(photo,40,50,100,false,true);var observation=new FlowTracker("x","person",image,box).update(duplicate);
        assertTrue(observation+" anchor="+new AdaptiveTracker("x","person",image,box).update(duplicate),observation.isNull("box"));
    }
    @Test public void lossAcrossSeveralFramesCanReacquireWithoutMovingThroughHiddenPixels() throws Exception {
        var guards=Tracking024Guards.run();assertTrue(guards.toString(),guards.getBoolean("duplicateAmbiguous"));assertTrue(guards.toString(),guards.getBoolean("hiddenHasNoCurrentBox"));assertTrue(guards.toString(),guards.getBoolean("reappearanceRecovered"));
    }
    @Test public void laterAnnotationsDoNotChangePredictionsAndLossesStillCount() throws Exception {
        var original=TrackingLabTest.pack("annotation-isolation","development",0,false);var altered=new org.json.JSONObject(original.toString());
        for(int i=1;i<altered.getJSONArray("frames").length();i++){var f=altered.getJSONArray("frames").getJSONObject(i);var image=new BufferedImage(320,240,BufferedImage.TYPE_BYTE_INDEXED);var bytes=new java.io.ByteArrayOutputStream();ImageIO.write(image,"png",bytes);f.put("mask",java.util.Base64.getEncoder().encodeToString(bytes.toByteArray())).put("maskSha256",PerceptionStore.hash(bytes.toByteArray()));}
        var a=Tracking024Evaluation.evaluate(original,Tracking024Evaluation.CANDIDATE,()->false,i->{});var b=Tracking024Evaluation.evaluate(altered,Tracking024Evaluation.CANDIDATE,()->false,i->{});
        assertEquals(7,b.getJSONObject("metrics").getInt("scoredFrames"));assertEquals(0,b.getJSONObject("metrics").getDouble("meanIou"),0);
        for(int i=0;i<7;i++)assertTrue(a.getJSONArray("frames").getJSONObject(i).getJSONObject("prediction").similar(b.getJSONArray("frames").getJSONObject(i).getJSONObject("prediction")));
    }
}
