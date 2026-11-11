package agent;

/** A bounded intercept from observed item motion, not a teleport or a pickup claim. */
public final class Pursuit117 {
    public record Choice(float targetX,boolean allowJump,String reason) {}
    public static Choice choose(float x,float y,float vx,float vy,boolean mushroom) {
        if(!Float.isFinite(x)||!Float.isFinite(y)||!Float.isFinite(vx)||!Float.isFinite(vy))
            throw new IllegalArgumentException("Finite observed item motion required");
        float target=x+Math.max(-.25f,Math.min(.25f,vx*.2f));
        boolean falling=mushroom&&vy<-.1f;
        return new Choice(target,!falling,falling?"Meet the falling mushroom on supported ground":"Aim ahead of observed item motion");
    }
}
