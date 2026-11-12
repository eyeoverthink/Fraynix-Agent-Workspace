package agent;
import components.*;
import jade.GameObject;
import scenes.Scene;
import physics2d.components.Rigidbody2D;
import org.jbox2d.common.Vec2;
import java.util.*;
/** One bounded wait/return per remembered object. Only observes physics and proposes goals. */
public final class PickupOpportunity120 {
 private final OpportunityMotion121 motion;
 private final boolean returnsEnabled,waitsEnabled;
 public PickupOpportunity120(){this(false,true);}
 public PickupOpportunity120(boolean observedMotion,boolean returnsEnabled){this(observedMotion,returnsEnabled,true);}
 public PickupOpportunity120(boolean observedMotion,boolean returnsEnabled,boolean waitsEnabled){this.motion=observedMotion?new OpportunityMotion121():null;this.returnsEnabled=returnsEnabled;this.waitsEnabled=waitsEnabled;}
 private final Set<UUID> waited=new HashSet<>(),returned=new HashSet<>();
 private GameObject waiting,revisiting;private long until;private float holdX;
 private int waitTicks,waitStarts,waitClears,waitAborts,revisitTicks,revisitStarts,plannedTicks;
 private String reason="No optional wait or return";private final List<Map<String,Object>> decisions=new ArrayList<>();
 private boolean holding;
 private static boolean eligible(GameObject o){return o!=null&&!o.isDead()&&o.getComponent(QuestionBlock.class)!=null&&o.getComponent(QuestionBlock.class).hasPowerup();}
 public static List<PickupSafety118.Enemy> enemies(Scene scene){var out=new ArrayList<PickupSafety118.Enemy>();for(var o:scene.getGameObjects())if(!o.isDead()&&(o.getComponent(GoombaAI.class)!=null||o.getComponent(TurtleAI.class)!=null)){var b=o.getComponent(Rigidbody2D.class);if(b!=null&&!b.isSensor())out.add(new PickupSafety118.Enemy(o.transform.position.x,o.transform.position.y,b.getVelocity().x));}return out;}
 public static float floor(GameObject player,float x,float y){float[] hit={Float.NaN};player.getComponent(Rigidbody2D.class).getRawBody().getWorld().raycast((f,p,n,portion)->{if(f.isSensor()||!(f.getUserData() instanceof GameObject o)||o.getComponent(Ground.class)==null||n.y<.5f)return -1;hit[0]=p.y;return portion;},new Vec2(x,y+.04f),new Vec2(x,y-.55f));return hit[0];}
 private static boolean holdSafe(Scene scene,GameObject player){float x=player.transform.position.x,y=player.transform.position.y;
  if(!player.getComponent(PlayerController.class).onGround||Math.abs(player.getComponent(Rigidbody2D.class).getVelocity().x)>.25f)return false;
  if(!Float.isFinite(floor(player,x-.2f,y))||!Float.isFinite(floor(player,x+.2f,y)))return false;
  for(var e:enemies(scene))if(Math.abs(e.y()-y)<.8f&&OpportunityTiming120.distance(x,e.x(),e.x()+e.vx()*.75f)<.9f)return false;
  return true;
 }
 public static boolean returnSafe(Scene scene,GameObject player,GameObject object){return returnSafe(scene,player,object,1.75f);}
 private static boolean returnSafe(Scene scene,GameObject player,GameObject object,float reach){float x=player.transform.position.x,y=player.transform.position.y,target=object.transform.position.x;
  if(!player.getComponent(PlayerController.class).onGround||Math.abs(target-x)>reach)return false;
  var pc=player.getComponent(PlayerController.class);float vx=player.getComponent(Rigidbody2D.class).getVelocity().x;float brake=vx*Math.abs(vx)/(2*(pc.walkSpeed+60*pc.slowDownForce));float end=x+brake+Math.signum(vx)*.4f;int stoppingSamples=Math.max(1,(int)Math.ceil(Math.abs(end-x)/.1f));for(int i=0;i<=stoppingSamples;i++)if(!Float.isFinite(floor(player,x+(end-x)*i/stoppingSamples,y)))return false;
  var goal=new PowerupPlanner.Goal(PowerupPlanner.Kind.BUMP_BLOCK,object,target,object.transform.position.y,"Revisit deferred power-up");
  if(!PickupObserver118.assess(scene,player,goal).pursue())return false;
  int samples=Math.max(1,(int)Math.ceil(Math.abs(target-x)/.15f));for(int i=0;i<=samples;i++){float f=floor(player,x+(target-x)*i/samples,y);if(!Float.isFinite(f)||Math.abs(f-(y-.125f))>.2f)return false;}
  float lo=Math.min(x,target),hi=Math.max(x,target);for(var e:enemies(scene))if(Math.abs(e.y()-y)<.8f&&Math.min(e.x(),e.x()+e.vx()*.75f)<hi+.85f&&Math.max(e.x(),e.x()+e.vx()*.75f)>lo-.85f)return false;
  return true;
 }
 private void record(long tick,GameObject object,String action,String why){reason=why;if(decisions.size()<24)decisions.add(Map.of("tick",tick,"objectId",object.getPersistentId().toString(),"action",action,"reason",why,"plannedTicks",plannedTicks));}
 private PowerupPlanner.Goal goal(GameObject o,String why){return new PowerupPlanner.Goal(PowerupPlanner.Kind.BUMP_BLOCK,o,o.transform.position.x,o.transform.position.y,why);}
 public PowerupPlanner.Goal choose(Scene scene,GameObject player,PowerupPlanner planner,PowerupPlanner.Goal normal,float destination,long tick){
  holding=false;if(motion!=null)motion.observe(scene,tick);
  if(waiting!=null){if(!eligible(waiting)){record(tick,waiting,"wait-ended","Observed item no longer needs waiting");waiting=null;}else return goal(waiting,"Observe temporary threat while waiting");}
  if(revisiting!=null){if(!eligible(revisiting)){record(tick,revisiting,"return-ended","Block changed; verify actual collection separately");revisiting=null;}
   else if(tick>=until){record(tick,revisiting,"return-ended","Return time budget exhausted");planner.declineForRun(goal(revisiting,reason));revisiting=null;}
   else {revisitTicks++;return goal(revisiting,"Return to a supported remembered pickup");}}
  if(!returnsEnabled||normal.kind()!=PowerupPlanner.Kind.TRAVEL||revisitStarts>=3)return normal;
  for(var o:scene.getGameObjects())if(eligible(o)&&planner.remembered(o.getPersistentId(),tick)&&!returned.contains(o.getPersistentId())&&(o.transform.position.x-player.transform.position.x)*Math.signum(destination-player.transform.position.x)<-.6f&&o.transform.position.y-player.transform.position.y>.25f&&o.transform.position.y-player.transform.position.y<1.6f&&returnSafe(scene,player,o,motion==null?1.75f:2.75f)&&(motion==null||motion.stableNear(o.transform.position.x,player.transform.position.y,tick))){
   returned.add(o.getPersistentId());revisiting=o;until=tick+240;revisitStarts++;plannedTicks=0;record(tick,o,"return-started","Nearby remembered pickup has continuous observed support and clear route");return goal(o,reason);
  }
  return normal;
 }
 public boolean consider(Scene scene,GameObject player,PowerupPlanner.Goal goal,PickupSafety118.Decision risk,long tick){
  if(risk.pursue()){if(waiting!=null){waitClears++;record(tick,waiting,"wait-cleared","Fresh observation confirms the approach cleared");waiting=null;}return false;}
  if(revisiting!=null){record(tick,revisiting,"return-ended","Fresh risk observation refuses the return");revisiting=null;}
  if(waiting!=null){
   if(!risk.reason().equals("Enemy motion intersects the pickup approach")||tick>=until||waitTicks>=360||Math.abs(player.transform.position.x-holdX)>.15f||!holdSafe(scene,player)){
    waitAborts++;record(tick,waiting,"wait-aborted","Budget, support, position or threat changed; release waiting input");waiting=null;return false;
   }
   waitTicks++;holding=true;return true;
  }
  if(!waitsEnabled||goal.kind()!=PowerupPlanner.Kind.BUMP_BLOCK||waited.contains(goal.object().getPersistentId())||waitStarts>=3||waitTicks>=360||!risk.reason().equals("Enemy motion intersects the pickup approach")||!holdSafe(scene,player))return false;
  var plan=OpportunityTiming120.plan(player.transform.position.x,goal.x(),risk.supportY(),enemies(scene));
  if(!plan.worthwhile()||plan.ticks()>360-waitTicks||(motion!=null&&!motion.predictable(scene,player,goal.x(),plan.ticks(),tick)))return false;
  waited.add(goal.object().getPersistentId());waiting=goal.object();holdX=player.transform.position.x;until=tick+180;plannedTicks=plan.ticks();waitStarts++;waitTicks++;holding=true;record(tick,waiting,"wait-started",plan.reason());return true;
 }
 public boolean returning(){return motion!=null&&revisiting!=null;}
 public boolean holding(){return holding;}
 public Map<String,Object> observation(){var out=new LinkedHashMap<String,Object>();out.put("waitsEnabled123",waitsEnabled);out.put("returnsEnabled123",returnsEnabled);out.put("phase",holding?"WAIT":revisiting!=null?"REVISIT":"TRAVEL");out.put("waitTicks",waitTicks);out.put("waitStarts",waitStarts);out.put("waitClears",waitClears);out.put("waitAborts",waitAborts);out.put("revisitTicks",revisitTicks);out.put("revisitStarts",revisitStarts);out.put("reason",reason);out.put("decisions",new ArrayList<>(decisions));if(motion!=null)out.put("motion121",motion.observation(returnsEnabled));return out;}
}
