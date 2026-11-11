package agent;
import org.junit.Test;
import static org.junit.Assert.*;
public class Pursuit117Test {
    @Test public void descendingMushroomDoesNotInviteAnUnnecessaryJump(){var c=Pursuit117.choose(3,1,.8f,-2,true);assertFalse(c.allowJump());assertEquals(3.16f,c.targetX(),.0001f);}
    @Test public void staticFlowerStillAllowsASupportedJumpAndPredictionIsBounded(){assertTrue(Pursuit117.choose(3,1,0,0,false).allowJump());assertEquals(3.25f,Pursuit117.choose(3,1,100,0,false).targetX(),0);assertThrows(IllegalArgumentException.class,()->Pursuit117.choose(Float.NaN,1,0,0,true));}
}
