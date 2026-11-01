package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import java.awt.image.BufferedImage;
import java.util.*;
import static org.junit.Assert.*;

public class TabletopCoreTest {
    static BufferedImage image(int dx,int dy,boolean hidden){var im=new BufferedImage(320,240,BufferedImage.TYPE_INT_RGB);for(int y=0;y<240;y++)for(int x=0;x<320;x++)im.setRGB(x,y,(x%32<2||y%30<2)?0x808080:0xd0d8c0);if(!hidden){var random=new Random(38);for(int y=60;y<130;y++)for(int x=100;x<128;x++)im.setRGB(x+dx,y+dy,random.nextInt(0x505050));}return im;}
    static PixelTracker.Box box(){return new PixelTracker.Box(97./320,57./240,34./320,76./240);}
    @Test public void rigidAnchorFollowsSmallTranslationAndRejectsDisappearance(){var t=new RigidTargetTracker(image(0,0,false),box());var r=t.update(image(12,6,false));assertTrue(r.toString(),r.getBoolean("accepted"));assertEquals((114+12)/320.,r.getJSONArray("point").getDouble(0),.006);assertEquals((95+6)/240.,r.getJSONArray("point").getDouble(1),.006);var missing=t.update(image(0,0,true));assertFalse(missing.toString(),missing.getBoolean("accepted"));var back=t.update(image(25,5,false));assertTrue(back.toString(),back.getBoolean("accepted"));assertEquals((114+25)/320.,back.getJSONArray("point").getDouble(0),.008);}
    @Test public void metricSecondsFilterRetainsRealClockAndGrowsUncertaintyOnLoss(){var f=new TabletopFilter(20,new double[]{.1,.2});var a=f.step(20.2,new double[]{.104,.2},true,.003);assertTrue(a.getBoolean("accepted"));assertEquals(.2,a.getDouble("dtS"),1e-10);double initial=a.getDouble("radiusM");for(int i=2;i<8;i++)a=f.step(20+i*.2,null,false,.003);assertFalse(a.getBoolean("accepted"));assertTrue(a.getDouble("radiusM")>initial);assertThrows(IllegalArgumentException.class,()->f.step(20,null,false,.003));}
    @Test public void referenceGuardDetectsMatShiftRatherThanSilentlyChangingCoordinates(){var d=PlaneCalibrationTest.mat().put("imagePoints",new JSONArray("[[0.2,0.25],[0.8,0.25],[0.8,0.75],[0.2,0.75]]"));var first=image(0,0,false);var guard=new MatReferenceGuard(first,d);assertTrue(guard.check(first).toString(),guard.check(first).getBoolean("valid"));var shifted=new BufferedImage(320,240,BufferedImage.TYPE_INT_RGB);var g=shifted.createGraphics();g.drawImage(first,6,0,null);g.dispose();assertFalse(guard.check(shifted).getBoolean("valid"));}
}
