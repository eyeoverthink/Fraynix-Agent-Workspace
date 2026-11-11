package agent;

/** Bounded input-only escape driven by actual displacement and solid contacts. */
public final class ContactRecovery119 {
    public enum Phase { OBSERVE, WAIT, RELEASE, JUMP, REFUSED }
    public record Decision(boolean override,float horizontal,boolean jump,String reason) {}
    private Phase phase=Phase.OBSERVE;
    private long lastTick=-1,stillSince,until,jumpUntil;
    private float anchorX,anchorY,startX,direction;
    private int attempts;
    private boolean supported,blocked;
    public Phase phase(){return phase;}
    public int attempts(){return attempts;}
    public Decision step(long tick,float x,float y,float travel,boolean supportContact,boolean opposingContact,
                         boolean grounded,boolean floorBehind,boolean ceiling,boolean rearThreat){
        if(!Float.isFinite(x)||!Float.isFinite(y)||!Float.isFinite(travel)||travel==0||tick<0||tick<=lastTick)
            throw new IllegalArgumentException("Finite positions, direction and increasing ticks required");
        if(lastTick<0||Math.abs(x-anchorX)>.008f||Math.abs(y-anchorY)>.008f){anchorX=x;anchorY=y;stillSince=tick;}
        lastTick=tick;supported=supportContact;blocked=opposingContact;
        if(phase==Phase.OBSERVE&&tick-stillSince>=24&&supportContact&&opposingContact){
            if(attempts>=3||!floorBehind){phase=Phase.REFUSED;return no("Contact escape refused: unsupported retreat or attempt limit");}
            direction=Math.signum(travel);startX=x;
            if(rearThreat){phase=Phase.WAIT;until=tick+180;}
            else {attempts++;until=tick+90;phase=Phase.RELEASE;}
        }
        if(phase==Phase.WAIT){
            if(!floorBehind||tick>=until){phase=Phase.REFUSED;return no("Contact escape refused: observation budget or lost retreat support");}
            if(rearThreat)return new Decision(true,0,false,"Wait for observed rear threat to leave retreat path");
            attempts++;until=tick+90;phase=Phase.RELEASE;
        }
        if(phase==Phase.RELEASE){
            if(grounded&&Math.abs(x-startX)>.04f&&!ceiling){phase=Phase.JUMP;jumpUntil=tick+28;until=tick+90;}
            else if(!floorBehind||rearThreat||tick>=until){phase=Phase.REFUSED;return no("Contact escape refused: retreat lost support, threat or time budget");}
            else return new Decision(true,-direction,false,"Release observed corner contact");
        }
        if(phase==Phase.JUMP){
            if(tick>=until||tick>=jumpUntil&&grounded){phase=Phase.OBSERVE;stillSince=tick;anchorX=x;anchorY=y;return no("Contact escape ended; verify subsequent progress");}
            return new Decision(true,direction,tick<jumpUntil&&!ceiling,"Jump from observed support after retreat");
        }
        return no(phase==Phase.REFUSED?"Contact recovery budget refused":"Observe actual displacement and contacts");
    }
    private static Decision no(String reason){return new Decision(false,0,false,reason);}
    public java.util.Map<String,Object> observation(){return java.util.Map.of("phase",phase.name(),"attempts",attempts,"stillTicks",Math.max(0,lastTick-stillSince),"supportContact",supported,"opposingContact",blocked);}
}
