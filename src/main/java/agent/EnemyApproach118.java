package agent;
/** Small-player jump timing from observed separation and relative closing speed.
 * Powered-up behavior retains the original controller; its collision size differs.
 */
public final class EnemyApproach118 {
    public static boolean jumpNow(float separation,float closingSpeed){
        if(!Float.isFinite(separation)||!Float.isFinite(closingSpeed)||separation<0)throw new IllegalArgumentException("Finite enemy separation and relative speed required");
        return separation<Math.max(.7f,Math.min(1.7f,.25f+Math.max(0,closingSpeed)*.48f));
    }
}
