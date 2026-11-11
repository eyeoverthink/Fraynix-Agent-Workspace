package agent;
import java.util.*;

/** Conservative opportunity assessment. A refusal is not proof of an impossible task. */
public final class PickupSafety118 {
    public record Enemy(float x,float y,float vx) {}
    public record Decision(boolean pursue,String reason,float supportY,float clearance) {}
    public static Decision assess(float x,float y,float supportY,boolean block,boolean obstructed,List<Enemy> enemies){
        if(!Float.isFinite(x)||!Float.isFinite(y))throw new IllegalArgumentException("Finite target required");
        if(!Float.isFinite(supportY))return new Decision(false,"No observed ground beneath the pickup approach",supportY,0);
        float clearance=y-.125f-supportY;
        if(block&&clearance<.48f)return new Decision(false,"Insufficient clearance below the power-up block",supportY,clearance);
        if(obstructed)return new Decision(false,"Structural obstacle blocks the optional pickup approach",supportY,clearance);
        for(var e:enemies){
            if(!Float.isFinite(e.x)||!Float.isFinite(e.y)||!Float.isFinite(e.vx))return new Decision(false,"Enemy motion is not finite",supportY,clearance);
            float future=e.x+e.vx*.75f,lo=Math.min(e.x,future),hi=Math.max(e.x,future),distance=x<lo?lo-x:x>hi?x-hi:0;
            if(distance<.85f&&Math.abs(e.y-(supportY+.15f))<.8f)return new Decision(false,"Enemy motion intersects the pickup approach",supportY,clearance);
        }
        return new Decision(true,"Observed approach has support and clearance",supportY,clearance);
    }
}
