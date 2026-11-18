package agent;

import org.junit.Test;
import static org.junit.Assert.*;

public class RailNavigationTest {
    private static final RailNavigation.Bounds ACTOR = new RailNavigation.Bounds(-.12f,.12f,-.13f,.13f);
    private static final RailNavigation.Motor MOTOR = new RailNavigation.Motor(1.9f,2.1f,3.1f,3f,1f,-7f);

    @Test public void shiftedAndReverseRailsUseFixtureBoundsAndSupportedLandings() {
        var rail = new RailNavigation.Bounds(41f,41.35f,2f,2.25f);
        RailNavigation.Surface floor = (x, top, bottom) -> top >= 2 && bottom <= 2 ? 2 : Float.NaN;
        var right = RailNavigation.forecast(40.3f,2.135f,2.1f,0,1,ACTOR,rail,MOTOR,floor);
        assertNotNull(right);
        assertTrue(right.landX() > rail.right());
        assertTrue(right.clearance() > .035f);
        var left = RailNavigation.forecast(42.05f,2.135f,-2.1f,0,-1,ACTOR,rail,MOTOR,floor);
        assertNotNull(left);
        assertTrue(left.landX() < rail.left());
        assertEquals(right.clearance(),left.clearance(),.001f);
    }

    @Test public void rejectsAnOversizedRailUnsafeApproachAndAnUnsupportedLanding() {
        RailNavigation.Surface floor = (x, top, bottom) -> top >= .5f && bottom <= .5f ? .5f : Float.NaN;
        var tooWide = new RailNavigation.Bounds(1,4,.5f,.75f);
        var lowRail = new RailNavigation.Bounds(1,1.35f,.5f,.75f);
        assertNull(RailNavigation.forecast(.30f,.635f,2.1f,0,1,ACTOR,tooWide,MOTOR,floor));
        assertNull(RailNavigation.forecast(.85f,.635f,2.1f,0,1,ACTOR,lowRail,MOTOR,floor));
        assertNull(RailNavigation.forecast(.30f,.635f,2.1f,0,1,ACTOR,lowRail,MOTOR,
                (x,top,bottom) -> Float.NaN));
    }
}
