package fraymus.workspace;

import org.json.*;

/** Two independent position/velocity Kalman axes. Covariance is a model assumption,
 * not calibrated perception confidence. Time is an explicit source-frame ordinal. */
final class ObservationFilter {
    static final String VERSION="image-plane-cv-036-v1";
    private final double[][] axis=new double[2][5]; // position, velocity, Pxx, Pxv, Pvv
    private double time;
    ObservationFilter(double time,PixelTracker.Box initial){
        this.time=time;
        for(int i=0;i<2;i++)axis[i]=new double[]{i==0?initial.x()+initial.w()/2:initial.y()+initial.h()/2,0,.02*.02,0,.03*.03};
    }
    JSONObject step(double next,JSONObject tracker){
        double dt=next-time;
        if(!Double.isFinite(dt)||dt<=0||dt>30)throw new IllegalArgumentException("Observation time must increase by 1..30 source frames");
        double[] prior=new double[2],variance=new double[2];
        for(int i=0;i<2;i++){
            var a=axis[i]; double q=.002*.002;
            a[0]+=dt*a[1];a[2]+=2*dt*a[3]+dt*dt*a[4]+q*Math.pow(dt,4)/4;
            a[3]+=dt*a[4]+q*Math.pow(dt,3)/2;a[4]+=q*dt*dt;
            prior[i]=a[0];variance[i]=a[2];
        }
        var out=new JSONObject().put("time",next).put("timeUnit","source-frame")
            .put("predicted",new JSONArray(prior)).put("predictedVariance",new JSONArray(variance))
            .put("domain","normalized image-plane; unknown depth, metric scale and capture FPS");
        boolean visible=tracker.has("box")&&!tracker.isNull("box");
        boolean accepted=false;String status="predicted",reason="Tracker supplied no accepted measurement; uncertainty grows";
        if(visible){
            var box=PixelTracker.Box.from(tracker.getJSONArray("box"));
            double[] measurement={box.x()+box.w()/2,box.y()+box.h()/2};
            double error=Math.hypot(measurement[0]-prior[0],measurement[1]-prior[1]);
            double gate=Math.max(.12,5*Math.sqrt(variance[0]+variance[1]+2*.015*.015));
            out.put("measurement",new JSONArray(measurement)).put("innovation",error).put("innovationGate",gate);
            accepted=error<=gate;
            status=accepted?"observed":"rejected";
            reason=accepted?"Actual pixel measurement accepted under the fixed innovation gate":"Pixel match disagrees with prediction; no observation revision";
            if(accepted)for(int i=0;i<2;i++){
                var a=axis[i];double p=a[2],cross=a[3],s=p+.015*.015,k=p/s,kv=cross/s,delta=measurement[i]-a[0];
                a[0]+=k*delta;a[1]+=kv*delta;a[2]=(1-k)*p;a[3]=(1-k)*cross;a[4]-=kv*cross;
            }
        }
        time=next;
        var position=new JSONArray();var velocity=new JSONArray();var covariance=new JSONArray();
        for(var a:axis){position.put(a[0]);velocity.put(a[1]);covariance.put(new JSONArray(new double[]{a[2],a[3],a[3],a[4]}));}
        return out.put("position",position).put("velocity",velocity).put("covariance",covariance)
            .put("positionSigma",Math.sqrt(axis[0][2]+axis[1][2])).put("accepted",accepted).put("status",status).put("reason",reason)
            .put("uncertaintyMeaning","Model covariance under declared motion/noise assumptions; not calibrated probability or metric accuracy");
    }
}
