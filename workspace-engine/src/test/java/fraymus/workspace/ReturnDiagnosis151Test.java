package fraymus.workspace;

import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.util.*;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReturnDiagnosis151Test {
    @Test public void scoresAgreeWithOriginal149Implementation()throws Exception {
        var first=AppearanceFlow135Test.view(0,40,50,false,false);var seed=AppearanceFlow135Test.seed();var descriptor=StructureSearch149.descriptor(first,seed);
        var memory=new JSONObject().put("examples",new JSONArray().put(new JSONObject().put("imageSha256","f".repeat(64)).put("ordinal",1).put("structure149",descriptor)));
        var search=new StructureSearch149(first,seed,memory);Class<?> example=Class.forName("fraymus.workspace.StructureSearch149$Example"),peak=Class.forName("fraymus.workspace.StructureSearch149$Peak");
        var ec=example.getDeclaredConstructors()[0];ec.setAccessible(true);Object ref=ec.newInstance("check",ReturnDiagnosis151.values(descriptor.getJSONArray("coarse")),ReturnDiagnosis151.values(descriptor.getJSONArray("pixels")));
        var pc=peak.getDeclaredConstructors()[0];pc.setAccessible(true);var full=StructureSearch149.class.getDeclaredMethod("full",BufferedImage.class,peak);full.setAccessible(true);
        for(int x:new int[]{40,42,45,60}) {
            var image=AppearanceFlow135Test.view(0,x,50,false,false);var actual=(StructureSearch149.Match)full.invoke(search,image,pc.newInstance(seed,0.,0.,ref));
            var diagnostic=ReturnDiagnosis151.score(ReturnDiagnosis151.values(descriptor.getJSONArray("pixels")),ReturnDiagnosis151.sample(image,seed,0,0,0));
            assertEquals(actual.score(),diagnostic.getDouble("score"),1e-12);assertEquals(actual.colorError(),diagnostic.getDouble("colorError"),1e-12);assertEquals(actual.quadrantError(),diagnostic.getDouble("quadrantError"),1e-12);assertEquals(actual.gradient(),diagnostic.getDouble("gradient"),1e-12);assertEquals(actual.strong(),diagnostic.getBoolean("strong"));
        }
    }
    @Test public void translatedAndQuarterTurnControlsHaveKnownPixelCorrespondence() {
        var image=new BufferedImage(200,200,BufferedImage.TYPE_INT_RGB);var translated=new BufferedImage(200,200,BufferedImage.TYPE_INT_RGB);var turned=new BufferedImage(200,200,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<64;y++)for(int x=0;x<32;x++){int rgb=((x/2*17)<<16)|((y/4*17)<<8)|(((x/2+y/4)%16)*17);image.setRGB(40+x,50+y,rgb);translated.setRGB(90+x,70+y,rgb);turned.setRGB(113-y,40+x,rgb);}
        var original=new PixelTracker.Box(.2,.25,.16,.32);var displaced=new PixelTracker.Box(.45,.35,.16,.32);var turnedCrop=new PixelTracker.Box(.33,.20,.16,.32);
        var a=ReturnDiagnosis151.sample(image,original,0,0,0);assertTrue(ReturnDiagnosis151.score(a,ReturnDiagnosis151.sample(translated,displaced,0,0,0)).getBoolean("strong"));
        // Quarter-turn centre is (82,56); use unrotated sampling dimensions centred there.
        turnedCrop=new PixelTracker.Box(66./200,24./200,32./200,64./200);
        var rotated=ReturnDiagnosis151.score(a,ReturnDiagnosis151.sample(turned,turnedCrop,90,0,0));assertTrue(rotated.toString(),rotated.getBoolean("strong"));
        assertFalse(ReturnDiagnosis151.score(a,ReturnDiagnosis151.sample(translated,original,0,0,0)).getBoolean("strong"));
    }
    @Test public void flatOutOfFrameAndDifferentPatternsDoNotBecomeStrong() {
        var blank=new BufferedImage(64,64,BufferedImage.TYPE_INT_RGB);var b=new PixelTracker.Box(.1,.1,.5,.5);var flat=ReturnDiagnosis151.sample(blank,b,0,0,0);assertFalse(ReturnDiagnosis151.score(flat,flat).getBoolean("strong"));
        assertNull(ReturnDiagnosis151.sample(blank,b,45,-80,0));assertFalse(ReturnDiagnosis151.score(flat,null).getBoolean("strong"));
        var a=new double[768];var opposite=new double[768];for(int i=0;i<768;i++){a[i]=(i/3%2);opposite[i]=1-a[i];}assertFalse(ReturnDiagnosis151.score(a,opposite).getBoolean("strong"));
        assertThrows(IllegalArgumentException.class,()->ReturnDiagnosis151.score(new double[3],flat));
    }
}
