package agent;
import components.*;import jade.GameObject;import scenes.Scene;import physics2d.components.Rigidbody2D;import org.jbox2d.common.Vec2;import java.util.*;
/** Bounded identity-preserving velocity history. Observation only; no body writes. */
public final class OpportunityMotion121 {
 private record Seen(float x,float y,float vx,long tick,long reversed) {}
 private final Map<UUID,Seen> history=new LinkedHashMap<>();
 private final List<Map<String,Object>> events=new ArrayList<>();
 private long checks,lastTick=-1;private int reversals,refusedForecasts;
 public void observe(Scene scene,long tick){
  if(tick<=lastTick)throw new IllegalArgumentException("Increasing motion observation clock required");lastTick=tick;checks++;
  var live=new HashSet<UUID>();
  for(var o:scene.getGameObjects())if(!o.isDead()&&(o.getComponent(GoombaAI.class)!=null||o.getComponent(TurtleAI.class)!=null)){
   var rb=o.getComponent(Rigidbody2D.class);if(rb==null||rb.isSensor())continue;
   UUID id=o.getPersistentId();live.add(id);float vx=rb.getVelocity().x;var old=history.get(id);long reversed=old==null?-1000:old.reversed();
   if(old!=null&&tick-old.tick()<=120&&Math.abs(vx)>.05f&&Math.abs(old.vx())>.05f&&Math.signum(vx)!=Math.signum(old.vx())){
    reversed=tick;reversals++;if(events.size()<16)events.add(Map.of("tick",tick,"objectId",id.toString(),"x",o.transform.position.x,"y",o.transform.position.y,"beforeVx",old.vx(),"afterVx",vx));
   }
   if(history.containsKey(id)||history.size()<64)history.put(id,new Seen(o.transform.position.x,o.transform.position.y,Math.abs(vx)>.05f?vx:old==null?0:old.vx(),tick,reversed));
  }
  history.keySet().retainAll(live);
 }
 public boolean stableNear(float x,float y,long tick){for(var e:history.values())if(Math.abs(e.x()-x)<2&&Math.abs(e.y()-y)<.8f&&tick-e.reversed()<30)return false;return true;}
 public boolean predictable(Scene scene,GameObject player,float target,int ticks,long tick){
  if(!stableNear(target,player.transform.position.y,tick)){refusedForecasts++;return false;}
  for(var o:scene.getGameObjects())if(!o.isDead()&&(o.getComponent(GoombaAI.class)!=null||o.getComponent(TurtleAI.class)!=null)){
   var rb=o.getComponent(Rigidbody2D.class);if(rb==null||rb.isSensor()||Math.abs(rb.getVelocity().x)<.05f)continue;
   float x=o.transform.position.x,y=o.transform.position.y,end=x+rb.getVelocity().x*ticks/60f;
   if(Math.abs(y-player.transform.position.y)>=.8f||OpportunityTiming120.distance(target,x,end)>=.85f)continue;
   boolean[] wall={false};rb.getRawBody().getWorld().raycast((f,p,n,a)->{if(f.isSensor()||!(f.getUserData() instanceof GameObject body)||body.getComponent(Ground.class)==null||Math.abs(n.x)<.5f)return -1;wall[0]=true;return a;},new Vec2(x,y),new Vec2(end,y));
   if(wall[0]){refusedForecasts++;return false;}
  }
  return true;
 }
 public Map<String,Object> observation(boolean returnsEnabled){return Map.of("checks",checks,"reversals",reversals,"forecastRefusals",refusedForecasts,"tracked",history.size(),"events",new ArrayList<>(events),"returnsEnabled",returnsEnabled,"returnReach",2.75f,"stabilityTicks",30);}
}
