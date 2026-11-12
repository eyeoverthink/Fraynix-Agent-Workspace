package agent;

import java.util.List;

/** Short, conservative observation-only contact forecast; returns ordinary horizontal input. */
public final class EnemyLanding122 {
    public record Enemy(float x, float y, float vx, float vy, float halfHeight) {}
    public record Forecast(boolean supported, boolean contact, int ticks, float x, float y) {}
    public record Decision(float horizontal, boolean override, Forecast desired, Forecast chosen, String reason) {}
    private static final float DT=1f/60;
    public static Forecast forecast(float x,float y,float vx,float vy,float halfHeight,
            float acceleration,float gravity,float maxX,float maxY,float brake,float input,
            List<Enemy> enemies,LandingGuard116.Surface surface) {
        boolean contact=false;
        for(int tick=1;tick<=60;tick++) {
            if(input==0)vx=Math.copySign(Math.max(0,Math.abs(vx)-brake),vx);
            else {if(vx*input<0)vx+=input*brake;vx+=input*acceleration*DT;}
            vx=Math.max(-maxX,Math.min(maxX,vx));vy=Math.max(-maxY,Math.min(maxY,vy+gravity*DT));
            float nx=x+vx*DT,ny=y+vy*DT;
            for(var e:enemies) {
                float ex=e.x()+e.vx()*tick*DT,ey=e.y()+e.vy()*tick*DT;
                if(Math.abs(nx-ex)<.32f&&Math.abs(ny-ey)<halfHeight+e.halfHeight()+.045f)contact=true;
            }
            float top=y-halfHeight+.03f,bottom=ny-halfHeight-.02f;
            float a=surface.height(nx-.11f,top,bottom),b=surface.height(nx,top,bottom),c=surface.height(nx+.11f,top,bottom);
            if(Float.isFinite(a)||Float.isFinite(b)||Float.isFinite(c)) {
                boolean supported=Float.isFinite(a)&&Float.isFinite(b)&&Float.isFinite(c)&&Math.max(a,Math.max(b,c))-Math.min(a,Math.min(b,c))<.04f;
                // Keep separation after touchdown long enough for a fresh grounded decision.
                for(var e:enemies)if(Math.abs(nx-(e.x()+e.vx()*(tick+6)*DT))<.55f&&Math.abs(ny-e.y())<.6f)contact=true;
                return new Forecast(supported,contact,tick,nx,ny);
            }
            x=nx;y=ny;
        }
        return new Forecast(false,contact,60,x,y);
    }
    public static Decision choose(float x,float y,float vx,float vy,float halfHeight,
            float acceleration,float gravity,float maxX,float maxY,float brake,float desired,float destination,
            List<Enemy> enemies,LandingGuard116.Surface surface) {
        if(!Float.isFinite(x+y+vx+vy+halfHeight+acceleration+gravity+maxX+maxY+brake+desired+destination)||Math.abs(desired)>1||enemies.size()>64)throw new IllegalArgumentException("Finite bounded enemy landing input required");
        for(var e:enemies)if(!Float.isFinite(e.x()+e.y()+e.vx()+e.vy()+e.halfHeight())||e.halfHeight()<=0)throw new IllegalArgumentException("Finite observed enemy required");
        var original=forecast(x,y,vx,vy,halfHeight,acceleration,gravity,maxX,maxY,brake,desired,enemies,surface);
        if(!original.contact())return new Decision(desired,false,original,original,"No predicted enemy contact in descending approach");
        Forecast best=null;float selected=desired;
        for(float input:new float[]{0,-1,1})if(input!=desired){
            var f=forecast(x,y,vx,vy,halfHeight,acceleration,gravity,maxX,maxY,brake,input,enemies,surface);
            if(f.supported()&&!f.contact()&&(best==null||Math.abs(f.x()-destination)<Math.abs(best.x()-destination))){best=f;selected=input;}
        }
        return best==null?new Decision(desired,false,original,original,"No contact-free supported alternative; reobserve"):
            new Decision(selected,true,original,best,"Brake descending approach for separated supported landing");
    }
}
