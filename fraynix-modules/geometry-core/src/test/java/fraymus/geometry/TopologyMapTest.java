package fraymus.geometry;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class TopologyMapTest {
    private static ReferenceMap.Rules reference(){int[] cells=new int[256];for(int y=0;y<16;y++)for(int x=0;x<16;x++){int t=x==0||y==0||x==15||y==15||x==5&&y>3&&y<11?0:1;if(x>=10&&x<=12&&y>=5&&y<=7)t=2;cells[y*16+x]=t;}return ReferenceMap.learn(16,16,cells,new boolean[]{false,true,false});}
    @Test public void globalSelectionUsesOriginalCandidatesAndMeetsIndependentGraphChecks(){
        var rules=reference();var c=new TopologyMap.Constraints(true,.3,.75,18,64);var r=TopologyMap.generate(rules,12,12,2801,List.of(),c);assertTrue(r.reason(),r.passed());assertEquals(1,r.audit().metrics().components());assertEquals(r.audit().metrics().floorCells(),r.audit().metrics().reachableCells());
        var candidate=ReferenceMap.candidate(rules,12,12,r.candidate().seed(),List.of(),TopologyMap.RESTARTS,TopologyMap.REPAIRS);assertArrayEquals(candidate.cells(),r.candidate().cells());
        var again=TopologyMap.generate(rules,12,12,2801,List.of(),c);assertArrayEquals(r.candidate().cells(),again.candidate().cells());assertEquals(r.trials(),again.trials());assertTrue(r.trials().size()<=32);assertTrue(r.totalRepairs()<=256000);
    }
    @Test public void impossibleRouteContractRetainsAnUneditedRejectedCandidate(){
        var r=TopologyMap.generate(reference(),12,12,2801,List.of(),new TopologyMap.Constraints(true,.3,.75,0,3));assertFalse(r.passed());assertEquals(1,r.trials().size());assertTrue(r.audit().failures().stream().anyMatch(s->s.contains("route")));assertArrayEquals(ReferenceMap.candidate(reference(),12,12,2801,List.of(),2,4000).cells(),r.candidate().cells());
    }
    @Test public void independentAuditRejectsHiddenIslandsAndSubstitutedEndpoints(){
        var rules=ReferenceMap.learn(3,3,new int[]{0,0,0,0,1,1,0,1,0},new boolean[]{false,true});int[] cells=new int[64];for(int y=1;y<=6;y++)cells[y*8+1]=1;for(int x=1;x<=6;x++)cells[6*8+x]=1;cells[2*8+4]=1;
        var path=ReferenceMap.path(cells,8,8,rules.walkable(),9,54);var candidate=new ReferenceMap.Result(8,8,1,cells,List.of(),path,0,1,0,true,"claimed success");var c=new TopologyMap.Constraints(true,.05,.95,0,128);var audit=TopologyMap.audit(rules,candidate,c);assertFalse(audit.passed());assertEquals(2,audit.metrics().components());assertTrue(audit.metrics().routeValid());assertTrue(audit.metrics().reachableCells()<audit.metrics().floorCells());
        var wrong=new ReferenceMap.Result(8,8,1,cells,List.of(),List.of(9,17),0,1,0,true,"forged short route");assertFalse(TopologyMap.audit(rules,wrong,new TopologyMap.Constraints(false,.05,.95,0,128)).metrics().routeValid());
    }
    @Test public void invalidConstraintUnitsAndOriginalSeedsAreRejected(){assertThrows(IllegalArgumentException.class,()->new TopologyMap.Constraints(true,Double.NaN,.75,0,64));assertThrows(IllegalArgumentException.class,()->new TopologyMap.Constraints(true,.8,.2,0,64));assertThrows(IllegalArgumentException.class,()->TopologyMap.generate(reference(),12,12,-1,List.of(),new TopologyMap.Constraints(true,.3,.75,0,64)));}
}
