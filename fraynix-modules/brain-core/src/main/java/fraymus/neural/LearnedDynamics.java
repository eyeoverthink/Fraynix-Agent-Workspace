package fraymus.neural;

import java.util.*;

/** Small learned, action-conditioned transition ensemble. Units: metres, seconds, m/s. */
public final class LearnedDynamics {
    public static final double DT=.05;
    public record State(double x,double v,double contact){}
    public record Context(double angleDeg,double gravity,double friction,double mass){}
    public record Transition(State before,double action,State after,Context context){}
    public record Prediction(State mean,double radiusX,double radiusV,List<State> members){}
    private final double[][][] weights;
    private final double calibrationX,calibrationV;
    public LearnedDynamics(double[][][] weights,double calibrationX,double calibrationV){
        if(weights.length!=5||!Double.isFinite(calibrationX)||!Double.isFinite(calibrationV)||calibrationX<0||calibrationV<0)throw new IllegalArgumentException("Invalid dynamics checkpoint");
        this.weights=new double[5][3][7];for(int e=0;e<5;e++){if(weights[e].length!=3)throw new IllegalArgumentException("Invalid dynamics outputs");for(int k=0;k<3;k++){if(weights[e][k].length!=7)throw new IllegalArgumentException("Invalid dynamics features");for(int j=0;j<7;j++){double w=weights[e][k][j];if(!Double.isFinite(w))throw new IllegalArgumentException("Non-finite dynamics weight");this.weights[e][k][j]=w;}}}
        this.calibrationX=calibrationX;this.calibrationV=calibrationV;
    }
    public static void validate(State s,double action,Context c){
        for(double x:new double[]{s.x,s.v,s.contact,action,c.angleDeg,c.gravity,c.friction,c.mass})if(!Double.isFinite(x))throw new IllegalArgumentException("Non-finite world state");
        if(s.x < -1||s.x>6||Math.abs(s.v)>4||s.contact<0||s.contact>1||Math.abs(action)>.6000001||c.angleDeg<10||c.angleDeg>40||c.gravity<8||c.gravity>12||c.friction<0||c.friction>.3||c.mass<.5||c.mass>2)throw new IllegalArgumentException("World model outside validated domain");
    }
    private static double[] features(State s,double u,Context c){double a=Math.toRadians(c.angleDeg),sign=Math.signum(s.v+u);return new double[]{1,s.v/4,u/.6,c.gravity*Math.sin(a)/10,c.friction*c.gravity*Math.cos(a)*sign*s.contact/3,Math.tanh((s.v+u)/.15),s.contact};}
    public State member(int model,State s,double u,Context c){
        validate(s,u,c);double[] f=features(s,u,c),out=new double[3];for(int k=0;k<3;k++)for(int j=0;j<7;j++)out[k]+=weights[model][k][j]*f[j];
        return new State(s.x+out[0]*.2,out[1]*4,Math.max(0,Math.min(1,out[2])));
    }
    public Prediction predict(State s,double u,Context c){
        var members=new ArrayList<State>();double x=0,v=0,contact=0;for(int e=0;e<5;e++){var next=member(e,s,u,c);members.add(next);x+=next.x/5;v+=next.v/5;contact+=next.contact/5;}
        double sx=0,sv=0;for(var m:members){sx+=(m.x-x)*(m.x-x)/5;sv+=(m.v-v)*(m.v-v)/5;}
        return new Prediction(new State(x,v,contact),Math.max(calibrationX,2*Math.sqrt(sx)),Math.max(calibrationV,2*Math.sqrt(sv)),List.copyOf(members));
    }
    public double[][][] weights(){var copy=new double[5][3][7];for(int i=0;i<5;i++)for(int j=0;j<3;j++)copy[i][j]=weights[i][j].clone();return copy;}
    public double calibrationX(){return calibrationX;}public double calibrationV(){return calibrationV;}
    public static LearnedDynamics fit(List<List<Transition>> episodes,double ridge,long seed){
        if(episodes.size()<8||episodes.size()>256||ridge<=0||!Double.isFinite(ridge))throw new IllegalArgumentException("Invalid dynamics training budget");
        var random=new Random(seed);var weights=new double[5][3][7];
        for(int model=0;model<5;model++){
            double[][] gram=new double[7][7],rhs=new double[3][7];
            for(int draw=0;draw<episodes.size();draw++)for(var row:episodes.get(random.nextInt(episodes.size()))){
                validate(row.before,row.action,row.context);double[] x=features(row.before,row.action,row.context),y={(row.after.x-row.before.x)/.2,row.after.v/4,row.after.contact};
                for(int i=0;i<7;i++){for(int j=0;j<7;j++)gram[i][j]+=x[i]*x[j];for(int k=0;k<3;k++)rhs[k][i]+=x[i]*y[k];}
            }
            for(int i=0;i<7;i++)gram[i][i]+=ridge;
            for(int k=0;k<3;k++)weights[model][k]=solve(gram,rhs[k]);
        }return new LearnedDynamics(weights,0,0);
    }
    private static double[] solve(double[][] matrix,double[] rhs){
        int n=rhs.length;double[][] a=new double[n][n+1];for(int i=0;i<n;i++){System.arraycopy(matrix[i],0,a[i],0,n);a[i][n]=rhs[i];}
        for(int j=0;j<n;j++){int pivot=j;for(int i=j+1;i<n;i++)if(Math.abs(a[i][j])>Math.abs(a[pivot][j]))pivot=i;var swap=a[j];a[j]=a[pivot];a[pivot]=swap;if(Math.abs(a[j][j])<1e-12)throw new IllegalStateException("Singular dynamics fit");double scale=a[j][j];for(int k=j;k<=n;k++)a[j][k]/=scale;for(int i=0;i<n;i++)if(i!=j){double s=a[i][j];for(int k=j;k<=n;k++)a[i][k]-=s*a[j][k];}}
        double[] out=new double[n];for(int i=0;i<n;i++){out[i]=a[i][n];if(!Double.isFinite(out[i]))throw new IllegalStateException("Invalid dynamics solution");}return out;
    }
    public LearnedDynamics calibrate(List<Transition> data){
        if(data.size()<16)throw new IllegalArgumentException("Insufficient independent calibration transitions");
        double[] x=new double[data.size()],v=new double[data.size()];int i=0;for(var row:data){var p=predict(row.before,row.action,row.context);x[i]=Math.abs(p.mean.x-row.after.x);v[i]=Math.abs(p.mean.v-row.after.v);i++;}
        Arrays.sort(x);Arrays.sort(v);int q=Math.min(x.length-1,(int)Math.ceil(.9*x.length)-1);return new LearnedDynamics(weights,x[q],v[q]);
    }
}
