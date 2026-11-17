package fraymus.workspace;

import java.awt.image.BufferedImage;
import java.util.*;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class RotationSearch155Test {
    @Test public void gridScoresMatchEveryScalarCoordinateIncludingFractionalStepsAndBorders(){
        var random=new Random(15501);double[] reference=new double[192];for(int i=0;i<reference.length;i++)reference[i]=random.nextDouble();
        for(int[] dims:new int[][]{{96,72},{193,145},{640,480},{1920,1080}}){
            int w=dims[0],h=dims[1];double[] image=new double[w*h];for(int i=0;i<image.length;i++)image[i]=random.nextDouble();
            for(double[] spec:new double[][]{{32,64,-180,4},{35.7,59.3,37,2.7},{w*.025,h*.025,-77,3.1},{w*.95,h*.95,15,4}}){
                var kernel=new RotationSearch155.Kernel(reference,spec[0],spec[1],spec[2]);var grid=new RotationSearch155.Grid(kernel,w,h,spec[3]);
                for(int y=0;y<grid.ys.length;y++)for(int x=0;x<grid.xs.length;x++)
                    assertEquals(Double.doubleToLongBits(kernel.at(image,grid.xs[x],grid.ys[y],w,h)),Double.doubleToLongBits(grid.at(image,x,y)));
                assertTrue(grid.xs.length<=w+1);assertTrue(grid.ys.length<=h+1);
            }
        }
    }
    @Test public void fullHistoricalTraceEqualsGridThroughMotionLossAndCopy(){
        var first=RotationSearch153Test.scene(15301,40,56,0,false);var seed=RotationSearch152Test.SEED;var memory=RotationSearch153Test.empty();
        var old=new RotationReturn153(first,seed,memory,RotationSearch153.Arithmetic.PRECOMPUTED);
        var fast=new RotationReturn155(first,seed,memory,RotationSearch155.Arithmetic.GRID);
        var images=new ArrayList<BufferedImage>();
        for(int i=0;i<5;i++)images.add(RotationSearch153Test.scene(15301,40+i*.7,56,0,false));
        images.add(RotationSearch153Test.scene(-1,0,0,0,false));
        for(int i=0;i<5;i++)images.add(RotationSearch153Test.scene(15301,130-i*.4,78,37,false));
        images.add(RotationSearch153Test.scene(15302,125,78,-73,false));
        for(int i=0;i<5;i++)images.add(RotationSearch153Test.scene(15301,124+i*.3,78,-73,i==3));
        for(int i=0;i<images.size();i++)assertTrue("Full old trace differs at "+i,old.update(images.get(i)).similar(fast.update(images.get(i))));
    }
    @Test public void retainedReferencesAndUniformImagesRemainExactlyEqual(){
        var first=RotationSearch153Test.scene(15301,40,56,0,false);var seed=RotationSearch152Test.SEED;
        var memory=RotationSearch153Test.empty().put("examples",new JSONArray().put(new JSONObject().put("imageSha256","a").put("ordinal",1).put("structure149",StructureSearch149.descriptor(first,seed))));
        var old=new RotationSearch153(first,seed,memory,RotationSearch153.Arithmetic.PRECOMPUTED);var fast=new RotationSearch155(first,seed,memory,RotationSearch155.Arithmetic.GRID);
        for(var image:List.of(first,RotationSearch153Test.scene(-1,0,0,0,false),RotationSearch153Test.scene(15301,130,78,37,true))){
            var a=old.search(image);var b=fast.search(image);assertEquals(a.size(),b.size());for(int i=0;i<a.size();i++)assertTrue(a.get(i).json().similar(b.get(i).json()));assertEquals(old.evaluations(),fast.evaluations());
        }
    }
    @Test public void inputBoundsAndCancellationRemainEnforced(){
        var first=RotationSearch153Test.scene(15301,40,56,0,false);var search=new RotationSearch155(first,RotationSearch152Test.SEED,RotationSearch153Test.empty(),RotationSearch155.Arithmetic.GRID);
        assertThrows(IllegalArgumentException.class,()->search.search(new BufferedImage(100,100,BufferedImage.TYPE_INT_RGB)));
        var k=new RotationSearch155.Kernel(new double[192],32,64,0);assertThrows(IllegalArgumentException.class,()->new RotationSearch155.Grid(k,1920,1080,.01));
        Thread.currentThread().interrupt();try{assertThrows(java.util.concurrent.CancellationException.class,()->search.search(first));}finally{Thread.interrupted();}
    }
}
