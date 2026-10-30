package fraymus.neural;

/** Bounded PD tracking of a smooth one-second trajectory, then a half-second hold.
 * Gains are selected using actual native training outcomes, not evaluation outcomes.
 */
public final class FeedbackController {
    public static final int TICKS=360, CONTROL_TICKS=12;
    public static final double MAX_DV=.6, DT=1.0/240;
    private FeedbackController(){}
    public record Decision(double deltaVelocityMps,double referenceM,double referenceVelocityMps,double positionErrorM,double velocityErrorMps){}
    public static double tangentVelocity(double vx,double vy,double radians){return vx*Math.cos(radians)-vy*Math.sin(radians);}
    public static Decision decide(int tick,double distance,double velocity,double target,double angle,double gravity,double friction,double kp,double kd,boolean feedback){
        for(double x:new double[]{distance,velocity,target,angle,gravity,friction,kp,kd})if(!Double.isFinite(x))throw new IllegalArgumentException("Non-finite controller input");
        if(tick<0||tick>=TICKS||target<.4||target>1.8||kp<0||kp>64||kd<0||kd>16)throw new IllegalArgumentException("Controller budget exceeded");
        double u=Math.min(1,tick*DT),position=target*(3*u*u-2*u*u*u),speed=u<1?target*6*u*(1-u):0,acc=u<1?target*6*(1-2*u):0;
        double direction=Math.signum(feedback?velocity:speed);if(Math.abs(feedback?velocity:speed)<.01)direction=Math.signum(speed);
        double plant=gravity*(Math.sin(angle)-friction*Math.cos(angle)*direction);
        double error=position-distance,velocityError=speed-velocity;
        double dv=(acc-plant+(feedback?kp*error+kd*velocityError:0))*CONTROL_TICKS*DT;
        return new Decision(Math.max(-MAX_DV,Math.min(MAX_DV,dv)),position,speed,error,velocityError);
    }
}
