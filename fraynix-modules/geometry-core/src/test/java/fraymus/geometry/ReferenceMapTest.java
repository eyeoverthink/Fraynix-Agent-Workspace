package fraymus.geometry;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class ReferenceMapTest {
    static ReferenceMap.Rules rules(){int[] cells=new int[16*16];for(int y=0;y<16;y++)for(int x=0;x<16;x++)cells[y*16+x]=x==0||y==0||x==15||y==15||x==5&&y>3&&y<11?0:x>=10&&x<=12&&y>=5&&y<=7?2:1;return ReferenceMap.learn(16,16,cells,new boolean[]{false,true,false});}
    @Test public void rulesComeFromActualReferenceAndOppositeDirectionsAgree(){var r=rules();assertEquals(256,Arrays.stream(r.frequency()).sum());assertFalse(r.allowed()[0][0][2]);assertTrue(r.allowed()[0][1][2]);for(int a=0;a<3;a++)for(int b=0;b<3;b++)assertEquals(r.allowed()[0][a][b],r.allowed()[2][b][a]);}
    @Test public void seedsProduceReproducibleNewMapsWithOriginalPinsAndRoutes(){var r=rules();for(int seed=2801;seed<2805;seed++){var pins=List.of(new ReferenceMap.Pin(3,3,1));var a=ReferenceMap.generate(r,16,16,seed,pins);assertTrue(a.reason(),a.passed());ReferenceMap.verify(r,a);var b=ReferenceMap.generate(r,16,16,seed,pins);assertArrayEquals(a.cells(),b.cells());assertEquals(a.path(),b.path());assertEquals(1,a.cells()[3*16+3]);assertFalse(Arrays.equals(a.cells(),r.source()));}assertFalse(Arrays.equals(ReferenceMap.generate(r,16,16,2801,List.of()).cells(),ReferenceMap.generate(r,16,16,2802,List.of()).cells()));}
    @Test public void impossiblePinnedAdjacencyRemainsAnHonestBoundedFailure(){var r=rules();var result=ReferenceMap.generate(r,8,8,44,List.of(new ReferenceMap.Pin(3,3,0),new ReferenceMap.Pin(4,3,2)));assertFalse(result.passed());assertTrue(result.conflicts()>0);assertEquals(0,result.cells()[27]);assertEquals(2,result.cells()[28]);assertThrows(IllegalStateException.class,()->ReferenceMap.verify(r,result));}
    @Test public void alteredGeneratedCellsCannotPassIndependentAdjacencyVerification(){var r=rules();var a=ReferenceMap.generate(r,16,16,2801,List.of());var cells=a.cells().clone();cells[0]=2;assertThrows(IllegalStateException.class,()->ReferenceMap.verify(r,new ReferenceMap.Result(a.width(),a.height(),a.seed(),cells,a.pins(),a.path(),0,a.attempts(),a.repairs(),true,"forged")));}
}
