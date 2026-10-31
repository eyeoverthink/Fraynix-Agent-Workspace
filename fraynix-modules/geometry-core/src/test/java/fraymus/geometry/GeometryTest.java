package fraymus.geometry;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class GeometryTest {
    @Test public void rotationsPreserveLengthsAndInvertAcrossAllSupportedDimensions(){
        var rng=new Random(2718);for(int d=2;d<=16;d++)for(int j=0;j<15;j++){double[] p=new double[d],zero=new double[d];for(int k=0;k<d;k++)p[k]=rng.nextDouble()*4-2;int a=j%d,b=(a+1)%d;var rotated=NDSpace.rotate(p,a,b,37);assertEquals(NDSpace.distance(p,zero),NDSpace.distance(rotated,zero),1e-12);assertArrayEquals(p,NDSpace.rotate(rotated,a,b,-37),1e-12);var xy=NDSpace.project(p,a,b,37);assertEquals(2,xy.length);assertTrue(Double.isFinite(xy[0]));assertTrue(Double.isFinite(xy[1]));}
    }
    @Test public void mathematicalInputsRejectWrongDimensionsAndNonfiniteCoordinates(){
        assertThrows(IllegalArgumentException.class,()->NDSpace.vector(new double[17]));assertThrows(IllegalArgumentException.class,()->NDSpace.distance(new double[2],new double[3]));assertThrows(IllegalArgumentException.class,()->NDSpace.rotate(new double[3],0,0,30));assertThrows(IllegalArgumentException.class,()->NDSpace.vector(new double[]{Double.NaN,0}));assertThrows(IllegalArgumentException.class,()->NDSpace.rotate(new double[3],0,2,Double.POSITIVE_INFINITY));
    }
    @Test public void seededColonizationReplaysAndEveryDeclaredDimensionHasRealBranches(){
        for(int d:new int[]{2,3,4,8,16}){var spec=new Colonization.Spec(d,2701,96,48,.035,.3,.055);var a=Colonization.grow(spec);var b=Colonization.grow(spec);assertTrue(a.nodes().size()>2);assertEquals(a.nodes().size(),b.nodes().size());for(int i=0;i<a.nodes().size();i++){assertEquals(a.nodes().get(i).parent(),b.nodes().get(i).parent());assertArrayEquals(a.nodes().get(i).position(),b.nodes().get(i).position(),0);}assertEquals(a.consumedBy(),b.consumedBy());Colonization.verify(a);var c=Colonization.grow(new Colonization.Spec(d,2702,96,48,.035,.3,.055));assertFalse(Arrays.equals(a.attractors().getFirst(),c.attractors().getFirst()));}
    }
    @Test public void independentBranchChecksRejectInventedStepsAndConsumedAttractors(){
        var a=Colonization.grow(new Colonization.Spec(3,41,32,20,.04,.3,.055));var nodes=new ArrayList<>(a.nodes());nodes.set(1,new Colonization.Node(0,new double[]{9,9,9},1));assertThrows(IllegalStateException.class,()->Colonization.verify(new Colonization.Result(a.spec(),nodes,a.attractors(),a.consumedBy(),a.growth(),a.stopReason())));var consumed=new ArrayList<>(a.consumedBy());consumed.set(0,a.nodes().size()+1);assertThrows(IllegalStateException.class,()->Colonization.verify(new Colonization.Result(a.spec(),a.nodes(),a.attractors(),consumed,a.growth(),a.stopReason())));
    }
    @Test public void dungeonsHaveActualConnectedFloorsAndShortestPathsOnDifferentSeeds(){
        for(int d=2;d<=4;d++)for(int seed=0;seed<12;seed++){var spec=new Dungeon.Spec(d,seed,d==2?24:d==3?18:12,12);var a=Dungeon.generate(spec);var b=Dungeon.generate(spec);assertEquals(12,a.rooms().size());assertEquals(a.floor(),b.floor());assertEquals(a.path(),b.path());assertEquals(a.floor().size(),a.reachable());Dungeon.verify(a);for(int cell:a.floor())assertEquals(cell,Dungeon.encode(Dungeon.decode(cell,spec.side(),d),spec.side()));}
    }
    @Test public void dungeonVerificationRejectsAPathThatWalksThroughWalls(){
        var a=Dungeon.generate(new Dungeon.Spec(4,17,12,12));var p=new ArrayList<>(a.path());p.set(1,0);assertThrows(IllegalStateException.class,()->Dungeon.verify(new Dungeon.Result(a.spec(),a.rooms(),a.corridors(),a.floor(),p,a.reachable())));assertThrows(IllegalArgumentException.class,()->new Dungeon.Spec(16,1,16,8));assertThrows(IllegalArgumentException.class,()->new Dungeon.Spec(4,1,40,8));
    }
    @Test public void differentDungeonSeedChangesGeometryWithoutLosingConnectivity(){
        var a=Dungeon.generate(new Dungeon.Spec(3,10,20,16));var b=Dungeon.generate(new Dungeon.Spec(3,11,20,16));assertNotEquals(a.floor(),b.floor());Dungeon.verify(a);Dungeon.verify(b);
    }
}
