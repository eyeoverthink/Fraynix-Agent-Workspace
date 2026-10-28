package agent;

import org.junit.Test;
import static org.junit.Assert.*;

public class MotorRecoveryTest {
    @Test public void blockedMovementBacksUpThenJumpsThroughNormalInputs() {
        MotorRecovery policy=new MotorRecovery();
        policy.step(1,10,1,true,true,false,false,false,true);
        var backed=policy.step(47,10,1,true,true,false,false,false,true);
        assertEquals(MotorRecovery.Phase.BACK_OFF,backed.phase());assertEquals(-1,backed.horizontal(),0);assertFalse(backed.jump());
        var jumping=policy.step(122,10.4f,1,true,true,false,false,false,true);
        assertEquals(MotorRecovery.Phase.JUMP_CLEAR,jumping.phase());assertTrue(jumping.jump());assertEquals(1,jumping.horizontal(),0);
    }
    @Test public void repeatedFailuresAndUnsafeBackoffStopInsteadOfLooping() {
        MotorRecovery policy=new MotorRecovery();var result=policy.step(1,10,1,true,true,false,false,true,true);
        for(int tick=2;tick<600;tick++) result=policy.step(tick,10,1,true,true,false,false,true,true);
        assertEquals(MotorRecovery.Phase.BLOCKED,result.phase());assertEquals(0,result.horizontal(),0);assertFalse(result.jump());
        var unsafe=new MotorRecovery().step(1,10,1,true,true,false,true,true,false);
        assertEquals(MotorRecovery.Phase.BLOCKED,unsafe.phase());
    }
    @Test public void ceilingPreventsTheForwardJumpUntilThereIsRoom() {
        var policy=new MotorRecovery();var first=policy.step(1,10,-1,true,true,false,false,true,true);
        assertEquals(1,first.horizontal(),0);assertFalse(first.jump());
        var next=policy.step(76,10.5f,-1,true,true,false,false,false,true);
        assertTrue(next.jump());assertEquals(-1,next.horizontal(),0);
    }
}
