package agent;
import org.junit.Test;
import static org.junit.Assert.*;

public class ContactRecovery119Test {
    @Test public void realDisplacementDistinguishesTravelFromLodgedContact(){
        var moving=new ContactRecovery119();
        for(int t=0;t<100;t++)assertFalse(moving.step(t,t*.02f,1,1,true,true,false,true,false,false).override());
        var stuck=new ContactRecovery119();ContactRecovery119.Decision d=null;
        for(int t=0;t<=24;t++)d=stuck.step(t,6,.87f,1,true,true,false,true,false,false);
        assertTrue(d.override());assertEquals(-1,d.horizontal(),0);assertFalse(d.jump());assertEquals(1,stuck.attempts());
        var settled=stuck.step(25,5.9f,.72f,1,true,false,true,true,false,false);
        assertTrue(settled.jump());assertEquals(1,settled.horizontal(),0);
        assertFalse(stuck.step(54,6.5f,.72f,1,true,false,true,true,false,false).override());
    }
    @Test public void unsupportedRetreatAndCeilingNeverGrantAnInventedJump(){
        var unsupported=new ContactRecovery119();ContactRecovery119.Decision d=null;
        for(int t=0;t<=24;t++)d=unsupported.step(t,6,.87f,1,true,true,false,false,false,false);
        assertFalse(d.override());assertEquals(ContactRecovery119.Phase.REFUSED,unsupported.phase());
        var ceiling=new ContactRecovery119();
        for(int t=0;t<=24;t++)ceiling.step(t,6,.87f,1,true,true,false,true,true,false);
        assertFalse(ceiling.step(25,5.9f,.72f,1,true,false,true,true,true,false).jump());
        assertFalse(ceiling.step(115,5.9f,.72f,1,true,false,true,true,true,false).override());
        assertEquals(ContactRecovery119.Phase.REFUSED,ceiling.phase());
    }
    @Test public void rearThreatWaitIsBoundedAndMustClearBeforeRetreat(){
        var policy=new ContactRecovery119();ContactRecovery119.Decision d=null;
        for(int t=0;t<=24;t++)d=policy.step(t,3,1.5f,1,true,true,false,true,false,true);
        assertTrue(d.override());assertEquals(0,d.horizontal(),0);assertEquals(0,policy.attempts());
        assertEquals(-1,policy.step(40,3,1.5f,1,true,true,false,true,false,false).horizontal(),0);
        var exhausted=new ContactRecovery119();
        for(int t=0;t<=204;t++)d=exhausted.step(t,3,1.5f,1,true,true,false,true,false,true);
        assertFalse(d.override());assertEquals(ContactRecovery119.Phase.REFUSED,exhausted.phase());
    }
    @Test public void repeatedTickAndNonFiniteObservationAreRejected(){
        var policy=new ContactRecovery119();policy.step(1,0,1,1,false,false,false,true,false,false);
        assertThrows(IllegalArgumentException.class,()->policy.step(1,0,1,1,false,false,false,true,false,false));
        assertThrows(IllegalArgumentException.class,()->new ContactRecovery119().step(0,Float.NaN,1,1,true,true,true,true,false,false));
    }
    @Test public void successfulEscapesDoNotResetTheTotalAttemptBudget(){
        var policy=new ContactRecovery119();long tick=0;float x=6;
        for(int attempt=0;attempt<3;attempt++){
            for(int t=0;t<25;t++)policy.step(tick++,x,1,1,true,true,false,true,false,false);
            var jump=policy.step(tick++,x-.1f,.72f,1,true,false,true,true,false,false);assertTrue(jump.jump());
            tick+=29;x+=1;policy.step(tick++,x,.72f,1,true,false,true,true,false,false);
        }
        ContactRecovery119.Decision d=null;for(int t=0;t<26;t++)d=policy.step(tick++,x,1,1,true,true,false,true,false,false);
        assertFalse(d.override());assertEquals(3,policy.attempts());assertEquals(ContactRecovery119.Phase.REFUSED,policy.phase());
    }
}
