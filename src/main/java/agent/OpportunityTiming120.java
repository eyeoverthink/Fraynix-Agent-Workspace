package agent;
import java.util.List;
/** Linear clearance is a spending estimate; actual safety is observed again every tick. */
public final class OpportunityTiming120 {
 public record Plan(boolean worthwhile,int ticks,String reason) {}
 public static float distance(float point,float a,float b){return point<Math.min(a,b)?Math.min(a,b)-point:point>Math.max(a,b)?point-Math.max(a,b):0;}
 public static Plan plan(float holdX,float targetX,float supportY,List<PickupSafety118.Enemy> enemies){
  if(!Float.isFinite(holdX)||!Float.isFinite(targetX)||!Float.isFinite(supportY))throw new IllegalArgumentException("Finite observation required");
  float seconds=0;
  for(var e:enemies){
   if(!Float.isFinite(e.x())||!Float.isFinite(e.y())||!Float.isFinite(e.vx()))return new Plan(false,0,"Unknown enemy motion");
   if(Math.abs(e.y()-(supportY+.15f))>=.8f)continue;
   if(distance(targetX,e.x(),e.x()+e.vx()*.75f)>=.85f)continue;
   if(Math.abs(e.vx())<.05f)return new Plan(false,0,"No bounded clearance for stationary threat");
   float clear=e.vx()>0?(targetX+.85f-e.x())/e.vx():(e.x()-(targetX-.85f))/-e.vx();seconds=Math.max(seconds,clear+.05f);
  }
  if(seconds<=0||seconds>3)return new Plan(false,0,"Clearance estimate exceeds useful waiting budget");
  for(var e:enemies)if(Math.abs(e.y()-(supportY+.15f))<.8f&&distance(holdX,e.x(),e.x()+e.vx()*seconds)<.9f)return new Plan(false,0,"Predicted threat crosses the waiting position");
  return new Plan(true,(int)Math.ceil(seconds*60),"Threat predicted to clear within bounded waiting cost");
 }
}
