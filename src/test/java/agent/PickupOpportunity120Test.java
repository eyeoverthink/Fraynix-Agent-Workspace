package agent;
import components.*;import jade.GameObject;import scenes.Scene;import physics2d.components.*;import physics2d.enums.BodyType;import org.joml.Vector2f;import org.junit.Test;import static org.junit.Assert.*;
public class PickupOpportunity120Test {
 private GameObject body(Scene scene,float x,float y,float w,float h,boolean terrain){var o=scene.createGameObject("fixture");o.transform.position.set(x,y);o.transform.scale.set(w,h);var rb=new Rigidbody2D();rb.setBodyType(BodyType.Static);o.addComponent(rb);var box=new Box2DCollider();box.setHalfSize(new Vector2f(w,h));o.addComponent(box);if(terrain)o.addComponent(new Ground());scene.physics().add(o);scene.addGameObjectToScene(o);return o;}
 private GameObject player(Scene scene,float x){var p=body(scene,x,.625f,.25f,.25f,false);var pc=new PlayerController();pc.onGround=true;p.addComponent(pc);return p;}
 private GameObject block(Scene s,float x){var b=s.createGameObject("Powerup");b.transform.position.set(x,1.375f);b.addComponent(new com.google.gson.Gson().fromJson("{\"blockType\":\"Powerup\"}",QuestionBlock.class));s.addGameObjectToScene(b);return b;}
 @Test public void nativeFloorLossAbortsWaitingWithoutFakingCollection(){
  var s=new Scene(null);var floor=body(s,2,.375f,5,.25f,true);var p=player(s,.9f);var b=block(s,3.375f);var e=body(s,3.875f,.63f,.25f,.25f,false);e.addComponent(new GoombaAI());e.getComponent(Rigidbody2D.class).setVelocity(new Vector2f(-.6f,0));
  var op=new PickupOpportunity120();var planner=new PowerupPlanner();var goal=planner.step(s,p,20,1);assertEquals(PowerupPlanner.Kind.BUMP_BLOCK,goal.kind());var risk=PickupObserver118.assess(s,p,goal);assertTrue(op.consider(s,p,goal,risk,1));assertTrue(op.holding());
  s.physics().destroyGameObject(floor);op.choose(s,p,planner,goal,20,2);assertFalse(op.consider(s,p,goal,risk,2));assertFalse(op.holding());assertEquals(1,op.observation().get("waitAborts"));
 }
 @Test public void returnRequiresWholeObservedGroundAndHasADeadline(){
  var s=new Scene(null);var floor=body(s,2,.375f,5,.25f,true);var p=player(s,3.2f);var b=block(s,2);var goal=new PowerupPlanner.Goal(PowerupPlanner.Kind.BUMP_BLOCK,b,2,1.375f,"deferred");var planner=new PowerupPlanner();planner.defer(goal,0);var op=new PickupOpportunity120();var normal=planner.step(s,p,20,61);assertEquals(PowerupPlanner.Kind.TRAVEL,normal.kind());
  assertTrue(PickupOpportunity120.returnSafe(s,p,b));assertEquals(b,op.choose(s,p,planner,normal,20,61).object());assertEquals(1,op.observation().get("revisitStarts"));
  assertEquals(PowerupPlanner.Kind.TRAVEL,op.choose(s,p,planner,normal,20,301).kind());assertEquals(PowerupPlanner.Kind.TRAVEL,op.choose(s,p,planner,normal,20,400).kind());
  s.physics().destroyGameObject(floor);body(s,3.2f,.375f,.3f,.25f,true);body(s,2,.375f,.3f,.25f,true);assertFalse(PickupOpportunity120.returnSafe(s,p,b));
 }
 @Test public void permanentRefusalCannotBeRevisited(){var s=new Scene(null);body(s,2,.375f,5,.25f,true);var p=player(s,3.2f);var b=block(s,2);var g=new PowerupPlanner.Goal(PowerupPlanner.Kind.BUMP_BLOCK,b,2,1.375f,"refused");var planner=new PowerupPlanner();planner.defer(g,0);planner.declineForRun(g);assertFalse(planner.remembered(b.getPersistentId(),100));var normal=planner.step(s,p,20,100);assertEquals(PowerupPlanner.Kind.TRAVEL,new PickupOpportunity120().choose(s,p,planner,normal,20,100).kind());}
 @Test public void earlierWaitCannotOverrideLaterPermanentRefusal(){
  var s=new Scene(null);body(s,3,.375f,8,.25f,true);var p=player(s,.9f);var b=block(s,3.375f);var e=body(s,3.875f,.63f,.25f,.25f,false);e.addComponent(new GoombaAI());e.getComponent(Rigidbody2D.class).setVelocity(new Vector2f(-.6f,0));var planner=new PowerupPlanner();var op=new PickupOpportunity120();var g=planner.step(s,p,20,1);assertTrue(op.consider(s,p,g,PickupObserver118.assess(s,p,g),1));op.choose(s,p,planner,g,20,181);assertFalse(op.consider(s,p,g,PickupObserver118.assess(s,p,g),181));planner.defer(g,181);planner.declineForRun(g);
  e.destroy();p.transform.position.x=4.4f;var normal=planner.step(s,p,20,300);assertEquals(PowerupPlanner.Kind.TRAVEL,normal.kind());assertTrue(PickupOpportunity120.returnSafe(s,p,b));assertEquals(PowerupPlanner.Kind.TRAVEL,op.choose(s,p,planner,normal,20,300).kind());assertEquals(0,op.observation().get("revisitStarts"));
 }
 @Test public void waitingExpiresEvenWhenReportedVelocityNeverProducesClearance(){
  var s=new Scene(null);body(s,2,.375f,5,.25f,true);var p=player(s,.9f);var b=block(s,3.375f);var e=body(s,3.875f,.63f,.25f,.25f,false);e.addComponent(new GoombaAI());e.getComponent(Rigidbody2D.class).setVelocity(new Vector2f(-.6f,0));var planner=new PowerupPlanner();var op=new PickupOpportunity120();var goal=planner.step(s,p,20,1);var risk=PickupObserver118.assess(s,p,goal);
  for(int t=1;t<=180;t++){goal=op.choose(s,p,planner,goal,20,t);assertTrue(op.consider(s,p,goal,risk,t));}
  goal=op.choose(s,p,planner,goal,20,181);assertFalse(op.consider(s,p,goal,risk,181));assertEquals(180,op.observation().get("waitTicks"));assertFalse(op.consider(s,p,goal,risk,400));assertEquals(1,op.observation().get("waitStarts"));
 }
 @Test public void returnCannotSpendBrakingDistanceBeyondObservedPlatform(){
  var s=new Scene(null);body(s,2,.375f,2.8f,.25f,true);var p=player(s,3.2f);var b=block(s,2);assertTrue(PickupOpportunity120.returnSafe(s,p,b));p.getComponent(Rigidbody2D.class).setVelocity(new Vector2f(2.1f,0));assertFalse(PickupOpportunity120.returnSafe(s,p,b));
 }
}
