package agent;

/** Position-based recovery; never changes a transform or bypasses collisions. */
public final class MotorRecovery {
    public enum Phase { ADVANCE, BACK_OFF, JUMP_CLEAR, BLOCKED }
    public record Decision(float horizontal, boolean jump, Phase phase, int attempts) {}
    private Phase phase=Phase.ADVANCE;
    private long phaseUntil, lastAdvance, jumpUntil, lastJump=-100;
    private float bestDistance=Float.POSITIVE_INFINITY;
    private int attempts;
    private int jumpTicks=28;
    public void jumpTicks(int value) { jumpTicks=Math.max(8,Math.min(28,value)); }
    public Decision step(long tick,float distance,float direction,boolean grounded,boolean obstacle,
                         boolean gap,boolean enemy,boolean ceiling,boolean floorBehind) {
        return step(tick,distance,direction,grounded,obstacle,gap,enemy,ceiling,floorBehind,false);
    }
    public Decision step(long tick,float distance,float direction,boolean grounded,boolean obstacle,
                         boolean gap,boolean enemy,boolean ceiling,boolean floorBehind,boolean wedged) {
        if(distance<bestDistance-.08f) { bestDistance=distance; lastAdvance=tick; attempts=0; }
        if(phase==Phase.BACK_OFF && tick>=phaseUntil) {
            if(!grounded) return new Decision(0,false,phase,attempts);
            if(ceiling) {
                if(attempts>=3 || !floorBehind) phase=Phase.BLOCKED;
                else {attempts++;phaseUntil=tick+75;}
            } else {phase=Phase.JUMP_CLEAR; phaseUntil=tick+90;jumpUntil=tick+jumpTicks;lastJump=tick;}
        }
        if(phase==Phase.JUMP_CLEAR && tick>=phaseUntil && grounded) {phase=Phase.ADVANCE;lastAdvance=tick;}
        if(phase==Phase.ADVANCE && (grounded || wedged) && (tick-lastAdvance>=45 || ceiling&&(obstacle||enemy))) {
            if(attempts>=3 || !floorBehind) phase=Phase.BLOCKED;
            else {phase=Phase.BACK_OFF;attempts++;phaseUntil=tick+75;jumpUntil=0;}
        }
        if(phase==Phase.BLOCKED) return new Decision(0,false,phase,attempts);
        if(phase==Phase.BACK_OFF) {
            if(!floorBehind) {phase=Phase.BLOCKED;return new Decision(0,false,phase,attempts);}
            return new Decision(-direction,false,phase,attempts);
        }
        if(phase==Phase.ADVANCE && grounded && !ceiling && tick-lastJump>35 && (obstacle||gap||enemy)) {
            jumpUntil=tick+jumpTicks;lastJump=tick;
        }
        return new Decision(direction,tick<jumpUntil,phase,attempts);
    }
}
