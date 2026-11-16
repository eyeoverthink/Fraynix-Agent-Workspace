package fraymus.workspace;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class RotationSearch153Test {
    static BufferedImage scene(int pattern,double x,double y,double angle,boolean copy){return RotationSearch152Test.scene(pattern,x,y,angle,copy);}
    static JSONObject empty(){return RotationSearch152Test.empty();}
    static RotationReturn153 tracker(RotationSearch153.Arithmetic mode){return new RotationReturn153(scene(15301,40,56,0,false),RotationSearch152Test.SEED,empty(),mode);}
    @Test public void scalarAndOptimizedTracesAreExactlyEqualAcrossReturnsAndCopies(){
        var scalar=tracker(RotationSearch153.Arithmetic.SCALAR);var fast=tracker(RotationSearch153.Arithmetic.PRECOMPUTED);
        var images=new ArrayList<BufferedImage>();
        for(int i=0;i<4;i++)images.add(scene(15301,130,78,37,false));
        images.add(scene(-1,0,0,0,false));images.add(scene(15302,130,78,-73,false));
        for(int i=0;i<4;i++)images.add(scene(15301,124+i,78,-73,false));
        images.add(scene(15301,130,78,-73,true));images.add(scene(15301,130,78,-73,false));
        for(int i=0;i<images.size();i++)assertTrue("exact arithmetic and full trace at "+i,scalar.update(images.get(i)).similar(fast.update(images.get(i))));
    }
    @Test public void freshFourFrameConfirmationAndPersistentCopyRefusal(){
        var t=tracker(RotationSearch153.Arithmetic.PRECOMPUTED);JSONObject last=null;
        for(int i=0;i<4;i++){last=t.update(scene(15301,130,78,37,false));if(i<3)assertTrue(last.isNull("box"));}
        assertFalse(last.toString(),last.isNull("box"));
        assertTrue(t.update(scene(-1,0,0,0,false)).isNull("box"));
        for(int i=0;i<3;i++)assertTrue(t.update(scene(15301,130,78,37,false)).isNull("box"));
        assertFalse(t.update(scene(15301,130,78,37,false)).isNull("box"));
        assertTrue(t.update(scene(15301,130,78,37,true)).getBoolean("ambiguityRetained"));
        for(int i=0;i<5;i++)assertTrue(t.update(scene(15301,130,78,37,false)).isNull("box"));
    }
    @Test public void recentPoseNeverSuppliesAppearanceForMissingOrWrongObjects(){
        var t=tracker(RotationSearch153.Arithmetic.PRECOMPUTED);
        for(int i=0;i<4;i++)t.update(scene(15301,130,78,-73,false));
        for(int i=0;i<6;i++){var row=t.update(scene(i%2==0?-1:15302,130,78,-73,false));assertTrue(row.toString(),row.isNull("box"));for(Object h:row.getJSONArray("hypotheses"))assertFalse(((JSONObject)h).getBoolean("accepted"));}
    }
    @Test public void nearCopyWithChangedQuadrantCannotGainConfirmation(){
        var t=tracker(RotationSearch153.Arithmetic.PRECOMPUTED);var patch=RotationSearch152Test.patch(15301);var g=patch.createGraphics();g.setColor(Color.BLACK);g.fillRect(0,0,16,32);g.dispose();
        for(int i=0;i<5;i++){var img=scene(-1,0,0,0,false);RotationSearch152Test.draw(img,patch,130,78,37);assertTrue(t.update(img).isNull("box"));}
    }
    @Test public void invalidReferencesDimensionsAndInterruptionRemainRefused(){
        var first=scene(15301,40,56,0,false);var search=new RotationSearch153(first,RotationSearch152Test.SEED,empty(),RotationSearch153.Arithmetic.PRECOMPUTED);
        assertThrows(IllegalArgumentException.class,()->search.search(new BufferedImage(100,100,BufferedImage.TYPE_INT_RGB)));
        var d=StructureSearch149.descriptor(first,RotationSearch152Test.SEED);d.getJSONArray("pixels").put(0,.123);var memory=empty().put("examples",new JSONArray().put(new JSONObject().put("imageSha256","a").put("ordinal",1).put("structure149",d)));
        assertThrows(IllegalArgumentException.class,()->new RotationSearch153(first,RotationSearch152Test.SEED,memory,RotationSearch153.Arithmetic.PRECOMPUTED));
        Thread.currentThread().interrupt();try{assertThrows(java.util.concurrent.CancellationException.class,()->search.search(first));}finally{Thread.interrupted();}
    }
}
