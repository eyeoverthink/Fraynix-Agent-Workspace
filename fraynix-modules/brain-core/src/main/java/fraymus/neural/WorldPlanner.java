package fraymus.neural;
import java.util.*;

/** 49 two-block counterfactuals, eight transitions each; execute the first action only. */
public final class WorldPlanner {
    private static final double[] ACTIONS={-.6,-.4,-.2,0,.2,.4,.6};
    private WorldPlanner(){}
    public record Plan(double action,double cost,List<LearnedDynamics.Prediction> future,boolean fallback,String reason){}
    public static Plan plan(LearnedDynamics model,LearnedDynamics.State state,LearnedDynamics.Context context,int tick,double target){
        LearnedDynamics.validate(state,0,context);if(tick<0||tick>=360||!Double.isFinite(target)||target<.4||target>1.8)throw new IllegalArgumentException("Invalid world-planning goal");
        Plan best=null;
        for(double first:ACTIONS)for(double second:ACTIONS){
            try{
                var states=new ArrayList<LearnedDynamics.State>();for(int e=0;e<5;e++)states.add(state);var future=new ArrayList<LearnedDynamics.Prediction>();double cost=0;
                for(int h=0;h<8;h++){
                    double u=h<4?first:second,x=0,v=0,c=0;var next=new ArrayList<LearnedDynamics.State>();
                    for(int e=0;e<5;e++){var s=model.member(e,states.get(e),u,context);next.add(s);x+=s.x()/5;v+=s.v()/5;c+=s.contact()/5;}
                    double sx=0,sv=0;for(var s:next){sx+=(s.x()-x)*(s.x()-x)/5;sv+=(s.v()-v)*(s.v()-v)/5;}
                    var p=new LearnedDynamics.Prediction(new LearnedDynamics.State(x,v,c),Math.max((h+1)*model.calibrationX(),2*Math.sqrt(sx)),Math.max((h+1)*model.calibrationV(),2*Math.sqrt(sv)),List.copyOf(next));future.add(p);states=next;
                    double t=(tick+(h+1)*12)/240.0,a=Math.min(1,t),reference=target*(3*a*a-2*a*a*a),speed=t<1?target*6*a*(1-a):0;
                    cost+=(x-reference)*(x-reference)+(h==7?.1:.05)*(v-speed)*(v-speed)+.001*u*u+.1*p.radiusX()*p.radiusX();
                }
                var plan=new Plan(first,cost,List.copyOf(future),false,"");if(best==null||cost<best.cost)best=plan;
            }catch(IllegalArgumentException unsupportedFuture){/* Candidate leaves the declared state domain; it cannot be selected. */}
        }
        if(best!=null&&best.future.get(0).radiusX()<=.02&&best.future.get(0).radiusV()<=.15)return best;
        var feedback=FeedbackController.decide(tick,state.x(),state.v(),target,Math.toRadians(context.angleDeg()),context.gravity(),context.friction(),49,10,true);
        return new Plan(feedback.deltaVelocityMps(),Double.NaN,List.of(),true,best==null?"All imagined trajectories left the declared domain":"One-step empirical uncertainty exceeds the planning limit");
    }
}
