package agent;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.List;
public class OpportunityTiming120Test {
 @Test public void pricesBothDirectionsAndRejectsStationaryOrUnboundedThreats(){
  var p=OpportunityTiming120.plan(.875f,3.375f,.5f,List.of(new PickupSafety118.Enemy(3.875f,.63f,-.6f)));assertTrue(p.worthwhile());assertTrue(p.ticks()>=137&&p.ticks()<=139);
  assertTrue(OpportunityTiming120.plan(.875f,3.375f,.5f,List.of(new PickupSafety118.Enemy(3.1f,.63f,.6f))).worthwhile());
  assertFalse(OpportunityTiming120.plan(.875f,3.375f,.5f,List.of(new PickupSafety118.Enemy(3.4f,.63f,0))).worthwhile());
  assertFalse(OpportunityTiming120.plan(.875f,3.375f,.5f,List.of(new PickupSafety118.Enemy(3.875f,.63f,-.1f))).worthwhile());
 }
 @Test public void prospectiveHoldCollisionAndUnknownMotionRefuse(){
  assertFalse(OpportunityTiming120.plan(2.2f,3.375f,.5f,List.of(new PickupSafety118.Enemy(3.875f,.63f,-.6f))).worthwhile());
  assertFalse(OpportunityTiming120.plan(.875f,3.375f,.5f,List.of(new PickupSafety118.Enemy(Float.NaN,.63f,-.6f))).worthwhile());
  assertThrows(IllegalArgumentException.class,()->OpportunityTiming120.plan(Float.NaN,3,.5f,List.of()));
 }
}
