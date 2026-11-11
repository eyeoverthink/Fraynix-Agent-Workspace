package agent;
import org.junit.Test;
import static org.junit.Assert.*;

public class LandingGuard116Test {
    @Test public void unknownFloorDoesNotInventASupportedLanding() {
        var d=LandingGuard116.choose(0,1,1,-1,.13f,1.9f,-7,2.1f,3.1f,.05f,1,10,(x,top,bottom)->Float.NaN);
        assertFalse(d.override());assertFalse(d.planned().supported());assertEquals(90,d.planned().ticks());
    }
    @Test public void clearGroundPreservesTheRequestedInput() {
        var d=LandingGuard116.choose(0,1,1,-1,.13f,1.9f,-7,2.1f,3.1f,.05f,1,10,(x,top,bottom)->top>=.5f&&bottom<=.5f?.5f:Float.NaN);
        assertFalse(d.override());assertTrue(d.planned().supported());assertEquals(1,d.horizontal(),0);
    }
}
