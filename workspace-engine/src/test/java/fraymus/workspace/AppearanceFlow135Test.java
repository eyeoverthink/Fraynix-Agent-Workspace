package fraymus.workspace;

import java.awt.*;
import java.awt.image.BufferedImage;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class AppearanceFlow135Test {
    static PixelTracker.Box seed(){return new PixelTracker.Box(40/480.,50/300.,100/480.,100/300.);}
    static BufferedImage view(double angle,int x,int y,boolean hidden,boolean duplicate)throws Exception{
        var photo=FlowTrackerTest.photo();var tile=new BufferedImage(100,100,BufferedImage.TYPE_INT_RGB);var g=tile.createGraphics();
        g.setColor(new Color(0x233b50));g.fillRect(0,0,100,100);g.rotate(Math.toRadians(angle),50,50);g.drawImage(photo,0,0,100,100,null);g.dispose();
        return FlowTrackerTest.scene(tile,x,y,100,hidden,duplicate);
    }
    static JSONObject original(JSONObject value){var copy=new JSONObject(value.toString());copy.remove("appearance135");return copy;}
    @Test public void uninterruptedIncumbentOutputRemainsExactlyEqual()throws Exception{
        var first=view(0,40,50,false,false);var a=new FlowTracker("x","object",first,seed());var b=new AppearanceFlow135("x","object",first,seed(),true);
        for(int i=1;i<=16;i++){var image=view(i,40+i*2,50+i,false,false);var old=a.update(image);var next=b.update(image);assertFalse(old.toString(),old.isNull("box"));assertTrue(original(next).similar(old));}
    }
    @Test public void disabledBankMatchesWholeIncumbentLossSequence()throws Exception{
        var first=view(0,40,50,false,false);var a=new FlowTracker("x","object",first,seed());var b=new AppearanceFlow135("x","object",first,seed(),false);
        for(int i=1;i<=12;i++){var image=view(0,40+i*2,50,i>=4&&i<=8,false);assertTrue(original(b.update(image)).similar(a.update(image)));}
    }
    @Test public void examplesHaveCycleEvidenceAndBoundedCapacity()throws Exception{
        var t=new AppearanceFlow135("x","object",view(0,40,50,false,false),seed(),true);int count=0;
        for(int i=1;i<=30;i++){var r=t.update(view(i,40+i,50,false,false));var b=r.getJSONObject("appearance135");assertTrue(b.getJSONArray("examples").length()<=6);
            var a=b.getJSONObject("admission");if(a.getBoolean("accepted")){count++;var e=a.getJSONObject("example");assertEquals(i,e.getInt("sourceOrdinal"));assertEquals(64,e.getString("descriptorSha256").length());assertTrue(e.getJSONObject("checks").getDouble("reverseOverlap")>=.85);assertTrue(e.getJSONArray("box").similar(r.getJSONArray("box")));}}
        assertTrue("At least two motion-checked appearances must be retained",count>=2);
    }
    @Test public void hiddenAndIdenticalDuplicateImagesCannotTeachTheBank()throws Exception{
        var first=view(0,40,50,false,false);var t=new AppearanceFlow135("x","object",first,seed(),true);
        for(int i=0;i<3;i++){var r=t.update(view(0,40,50,true,false));assertTrue(r.toString(),r.isNull("box"));assertFalse(r.getJSONObject("appearance135").getJSONObject("admission").getBoolean("accepted"));}
        for(int i=0;i<3;i++){var r=t.update(view(0,40,50,false,true));assertTrue(r.toString(),r.isNull("box"));assertFalse(r.getJSONObject("appearance135").getJSONObject("recovery").getBoolean("accepted"));}
    }
    @Test public void replayRebuildsTheSameCheckedBankAndPredictions()throws Exception{
        var first=view(0,40,50,false,false);var a=new AppearanceFlow135("x","object",first,seed(),true);var b=new AppearanceFlow135("x","object",first,seed(),true);
        for(int i=1;i<=10;i++){var image=view(i*2,40+i,50,false,false);assertTrue(a.update(image).similar(b.update(image)));}
    }
    @Test public void originalAppearanceCanRecoverAfterDifferentLearnedPose()throws Exception{
        var t=new AppearanceFlow135("x","object",view(0,40,50,false,false),seed(),true);
        for(int i=1;i<=30;i++)t.update(view(i*2,40+i,50,false,false));
        for(int i=0;i<3;i++)assertTrue(t.update(view(0,40,50,true,false)).isNull("box"));
        boolean recovery=false;
        for(int i=0;i<4;i++){var r=t.update(view(0,330,165,false,false));if(r.getJSONObject("appearance135").getJSONObject("recovery").getBoolean("accepted")){recovery=true;assertTrue(PixelTracker.iou(PixelTracker.Box.from(r.getJSONArray("box")),new PixelTracker.Box(330/480.,165/300.,100/480.,100/300.))>.7);assertFalse(r.getJSONObject("appearance135").getJSONObject("admission").getBoolean("accepted"));}}
        assertTrue("The checked bank must perform real recovery, not merely wrap the incumbent",recovery);
    }
    @Test public void publishedEvidenceCannotMutateRetainedBank()throws Exception{
        var first=view(0,40,50,false,false);var a=new AppearanceFlow135("x","object",first,seed(),true);var b=new AppearanceFlow135("x","object",first,seed(),true);
        var image=view(1,42,50,false,false);var publicResult=a.update(image);b.update(image);
        publicResult.getJSONObject("appearance135").getJSONArray("examples").getJSONObject(0).put("sourceOrdinal",99);
        image=view(2,44,50,false,false);assertTrue(a.update(image).similar(b.update(image)));
    }
    @Test public void dimensionsAndDefaultsArePreserved()throws Exception{
        var t=new AppearanceFlow135("x","object",view(0,40,50,false,false),seed(),true);
        assertThrows(IllegalArgumentException.class,()->t.update(new BufferedImage(40,40,1)));
        assertEquals("legacy-flow",ObservationMethods129.policy(new JSONObject()));
        assertEquals("appearance-continuity-135",ObservationMethods129.policy(new JSONObject().put("trackerPolicy","appearance-continuity-135")));
    }
}
