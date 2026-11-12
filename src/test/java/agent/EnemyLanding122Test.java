package agent;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class EnemyLanding122Test {
    private static final LandingGuard116.Surface FLOOR=(x,top,bottom)->top>=.5f&&bottom<=.5f?.5f:Float.NaN;
    @Test public void retainedDescendingApproachBrakesBeforeSideContact(){
        var enemies=List.of(new EnemyLanding122.Enemy(10.177682f,.62947f,-.6f,0,.13f));
        var choice=EnemyLanding122.choose(8.809682f,1.81375f,2.1f,-1.2409f,.13f,1.9f,-7,2.1f,3.1f,.05f,1,18,enemies,FLOOR);
        assertTrue(choice.desired().contact());assertTrue(choice.override());assertTrue(choice.chosen().supported());assertFalse(choice.chosen().contact());assertTrue(choice.horizontal()<=0);
    }
    @Test public void missingSupportCannotBePromotedToSafeLanding(){
        var choice=EnemyLanding122.choose(8.81f,1.81f,2.1f,-1.24f,.13f,1.9f,-7,2.1f,3.1f,.05f,1,18,List.of(new EnemyLanding122.Enemy(10.18f,.63f,-.6f,0,.13f)),(x,t,b)->Float.NaN);
        assertFalse(choice.override());assertFalse(choice.chosen().supported());assertTrue(choice.chosen().ticks()<=60);
    }
    @Test public void distantEnemyDoesNotChangeOrdinaryInputAndNonfiniteInputIsRefused(){
        var choice=EnemyLanding122.choose(2,1,1,-1,.13f,1.9f,-7,2.1f,3.1f,.05f,1,18,List.of(new EnemyLanding122.Enemy(10,.63f,0,0,.13f)),FLOOR);
        assertFalse(choice.override());assertEquals(1,choice.horizontal(),0);
        assertThrows(IllegalArgumentException.class,()->EnemyLanding122.choose(Float.NaN,1,1,-1,.13f,1.9f,-7,2.1f,3.1f,.05f,1,18,List.of(),FLOOR));
    }
    @Test public void stationarySupportedEdgeUsesBoundedRetreatWithoutInventingGroundedState(){
        var revised=new ContactRecovery119(true);var old=new ContactRecovery119();
        ContactRecovery119.Decision decision=null;
        for(int t=1;t<=25;t++){decision=revised.step(t,3.38f,1.12f,1,true,false,false,true,false,false);assertFalse(old.step(t,3.38f,1.12f,1,true,false,false,true,false,false).override());}
        assertTrue(decision.override());assertEquals(-1,decision.horizontal(),0);assertFalse(decision.jump());
        for(int t=26;t<=115;t++)decision=revised.step(t,3.38f,1.12f,1,true,false,false,true,false,false);
        assertFalse(decision.override());assertEquals(ContactRecovery119.Phase.REFUSED,revised.phase());assertEquals(1,revised.attempts());
    }
    @Test public void supportedEdgeNeverRetreatsWithoutGroundBehind(){
        var revised=new ContactRecovery119(true);ContactRecovery119.Decision d=null;
        for(int t=1;t<=25;t++)d=revised.step(t,3,1,1,true,false,false,false,false,false);
        assertFalse(d.override());assertEquals(0,revised.attempts());
    }
}
