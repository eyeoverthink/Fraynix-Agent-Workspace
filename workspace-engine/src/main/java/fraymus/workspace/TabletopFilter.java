package fraymus.workspace;
import org.json.*;

/** Two metric constant-velocity axes, driven by real seconds. No image/frame-index noise rescaling. */
final class TabletopFilter {
    static final String VERSION="metric-seconds-cv-038-v1";
    private final double[][] axes=new double[2][5];
    private double time;
    TabletopFilter(double time,double[] p){this.time=time;for(int i=0;i<2;i++)axes[i]=new double[]{p[i],0,.003*.003,0,.02*.02};}
    JSONObject step(double next,double[] measurement,boolean appearanceAccepted,double anchorError){
        double dt=next-time;if(!Double.isFinite(dt)||dt<=0||dt>1800)throw new IllegalArgumentException("Actual source seconds must increase with gaps at most1800s (long setup gaps require fresh verified appearance)");double[] prior=new double[2];
        for(int i=0;i<2;i++){var a=axes[i];double q=.05*.05;a[0]+=dt*a[1];a[2]+=2*dt*a[3]+dt*dt*a[4]+q*Math.pow(dt,4)/4;a[3]+=dt*a[4]+q*Math.pow(dt,3)/2;a[4]+=q*dt*dt;prior[i]=a[0];}
        boolean accepted=false;double error=measurement==null?Double.NaN:PlaneCalibration.distance(prior,measurement),gate=Math.max(.06,4*Math.sqrt(axes[0][2]+axes[1][2]+2*.002*.002));
        boolean gap=dt>1;
        if(measurement!=null&&appearanceAccepted&&(error<=gate||gap)){
            accepted=true;for(int i=0;i<2;i++){var a=axes[i];if(gap){a[0]=measurement[i];a[1]=0;a[2]=.005*.005;a[3]=0;a[4]=.02*.02;continue;}double p=a[2],cross=a[3],s=p+.002*.002,k=p/s,kv=cross/s,delta=measurement[i]-a[0];a[0]+=k*delta;a[1]+=kv*delta;a[2]=(1-k)*p;a[3]=(1-k)*cross;a[4]-=kv*cross;}
        }
        time=next;double sigma=Math.sqrt(axes[0][2]+axes[1][2]);var out=new JSONObject().put("seconds",next).put("dtS",dt).put("predictedM",new JSONArray(prior)).put("positionM",new JSONArray(new double[]{axes[0][0],axes[1][0]})).put("velocityMps",new JSONArray(new double[]{axes[0][1],axes[1][1]})).put("sigmaM",sigma).put("radiusM",2.5*sigma+anchorError).put("accepted",accepted).put("status",accepted?(gap?"reacquired-after-gap":"measured-estimate"):"prediction-only").put("innovationGateM",gate).put("gapReset",accepted&&gap).put("uncertaintyMeaning","Declared motion/noise model plus anchor error; uncalibrated until separately evaluated. Appearance match is not probability.");
        if(measurement!=null)out.put("measurementM",new JSONArray(measurement)).put("innovationM",error);return out;
    }
}
