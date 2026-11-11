package agent;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class PickupSafety118Test {
    @Test public void refusesUnsupportedBlockedAndLowClearanceTargets(){
        assertFalse(PickupSafety118.assess(3,1.375f,Float.NaN,true,false,List.of()).pursue());
        assertFalse(PickupSafety118.assess(3,1.375f,1,true,false,List.of()).pursue());
        assertFalse(PickupSafety118.assess(3,1.375f,.5f,true,true,List.of()).pursue());
        assertTrue(PickupSafety118.assess(3,1.375f,.5f,true,false,List.of()).pursue());
    }
    @Test public void considersApproachingMotionAndRetainsSafeOpportunities(){
        assertFalse(PickupSafety118.assess(3,1.375f,.5f,true,false,List.of(new PickupSafety118.Enemy(4,.625f,-.6f))).pursue());
        assertTrue(PickupSafety118.assess(3,1.375f,.5f,true,false,List.of(new PickupSafety118.Enemy(4,.625f,.6f))).pursue());
        assertTrue(PickupSafety118.assess(3,.625f,.5f,false,false,List.of(new PickupSafety118.Enemy(3,4,0))).pursue());
        assertThrows(IllegalArgumentException.class,()->PickupSafety118.assess(Float.NaN,1,.5f,false,false,List.of()));
    }
    @org.junit.Test public void timesSmallPlayerJumpUsingClosingMotion(){
        org.junit.Assert.assertFalse(EnemyApproach118.jumpNow(2.4f,.6f));
        org.junit.Assert.assertTrue(EnemyApproach118.jumpNow(1.2f,2.7f));
        org.junit.Assert.assertFalse(EnemyApproach118.jumpNow(1.2f,-2));
        org.junit.Assert.assertTrue(EnemyApproach118.jumpNow(.5f,0));
        org.junit.Assert.assertThrows(IllegalArgumentException.class,()->EnemyApproach118.jumpNow(Float.NaN,1));
    }
}
