package fraymus.workspace;
import fraymus.neural.LearnedDynamics;
import org.json.*;
import java.nio.file.*;
import java.util.*;

/** Previously recorded observations, unchanged mapping; passive forecasting grants no action authority. */
final class RealityForecast {
    private RealityForecast(){}
    private record Point(double t,double x){}
    private static List<Point> read(Path path,String expected) throws Exception {
        String text=Files.readString(path);if(!MeasurementStore.hash(text).equals(expected))throw new IllegalStateException("Pinned recorded CSV digest differs: "+path.getFileName());
        var lines=text.strip().split("\\R");if(!lines[0].startsWith("time_s,displacement_m,"))throw new IllegalArgumentException("Recorded columns differ");var rows=new ArrayList<Point>();
        for(int i=1;i<lines.length;i++){var fields=lines[i].split(",");double t=Double.parseDouble(fields[0]),x=Double.parseDouble(fields[1]);if(!Double.isFinite(t)||!Double.isFinite(x)||Math.abs(t-(i-1)/60.0)>1e-12)throw new IllegalArgumentException("Recorded timestamps/values differ");rows.add(new Point(t,x));}return rows;
    }
    static JSONObject evaluate(Path repo,LearnedDynamics model) throws Exception {
        var folder=repo.resolve("docs/datasets/delfys75-013");var provenance=new JSONObject(Files.readString(folder.resolve("provenance.json")));var hashes=provenance.getJSONObject("outputs");if(!MeasurementStore.hash(Files.readString(folder.resolve("spatial-calibration-frozen.json"))).equals(hashes.getString("spatial-calibration-frozen.json")))throw new IllegalStateException("Recorded spatial mapping digest differs");
        var training=read(folder.resolve("training.csv"),hashes.getString("training.csv"));var validation=read(folder.resolve("validation.csv"),hashes.getString("validation.csv"));if(training.size()!=52||validation.size()!=49)throw new IllegalArgumentException("Recorded case row count differs");
        double dt=1.0/60,acceleration=0;for(int i=2;i<training.size();i++)acceleration+=(training.get(i).x-2*training.get(i-1).x+training.get(i-2).x)/(dt*dt);acceleration/=(training.size()-2);
        var residuals=new double[training.size()-2];for(int i=2;i<training.size();i++)residuals[i-2]=Math.abs(2*training.get(i-1).x-training.get(i-2).x+acceleration*dt*dt-training.get(i).x);Arrays.sort(residuals);double radius=residuals[(int)Math.ceil(.9*residuals.length)-1];
        // First two observations are declared conditioning, never counted as successful forecasts.
        Point origin=validation.get(1);double initialVelocity=(origin.x-validation.get(0).x)/dt;
        var context=new LearnedDynamics.Context(20,9.81,0,1);var state=new LearnedDynamics.State(origin.x,initialVelocity,1);
        var nativeFrames=new ArrayList<Point>();nativeFrames.add(origin);for(int i=1;origin.t+(i-1)*.05<validation.get(validation.size()-1).t;i++){state=model.predict(state,0,context).mean();nativeFrames.add(new Point(origin.t+i*.05,state.x()));}
        var rows=new JSONArray();double squares=0,max=0,oneSquares=0,cvSquares=0,nativeSquares=0;int covered=0;
        for(int i=2;i<validation.size();i++){
            var p=validation.get(i);double t=p.t-origin.t,predicted=origin.x+initialVelocity*t+.5*acceleration*t*t,cv=origin.x+initialVelocity*t;
            double one=2*validation.get(i-1).x-validation.get(i-2).x+acceleration*dt*dt;
            int left=Math.min(nativeFrames.size()-2,(int)Math.floor(t/.05));var a=nativeFrames.get(left);var b=nativeFrames.get(left+1);double nativeX=a.x+(p.t-a.t)/(b.t-a.t)*(b.x-a.x);
            double error=predicted-p.x;squares+=error*error;max=Math.max(max,Math.abs(error));oneSquares+=Math.pow(one-p.x,2);cvSquares+=Math.pow(cv-p.x,2);nativeSquares+=Math.pow(nativeX-p.x,2);if(Math.abs(one-p.x)<=radius)covered++;
            rows.put(new JSONObject().put("timeS",p.t).put("observedM",p.x).put("passiveFreeForecastM",predicted).put("passiveOneStepM",one).put("constantVelocityM",cv).put("nativeTransferM",nativeX).put("freeResidualM",error).put("oneStepRadiusM",radius));
        }
        int n=rows.length();return new JSONObject().put("dataset","Delfys75 low/01 → low/02").put("sourceKind","real video-derived mask centroids; approximate frozen image-plane mapping").put("previouslyObservedRegressionCase",true).put("trainingRows",training.size()).put("sourceValidationRows",validation.size()).put("conditioningRows",2).put("forecastRows",n).put("sourceHashes",new JSONObject().put("training.csv",hashes.getString("training.csv")).put("validation.csv",hashes.getString("validation.csv")).put("spatialProtocol",hashes.getString("spatial-calibration-frozen.json"))).put("accelerationEstimateMps2",acceleration).put("initialVelocityFromFirstTwoMps",initialVelocity).put("freeRmseM",Math.sqrt(squares/n)).put("freeMaxErrorM",max).put("oneStepRmseM",Math.sqrt(oneSquares/n)).put("constantVelocityFreeRmseM",Math.sqrt(cvSquares/n)).put("nativeTransferRmseM",Math.sqrt(nativeSquares/n)).put("oneStepEmpiricalCoverage",covered/(double)n).put("toleranceM",.015).put("accepted",max<=.015&&squares<=cvSquares).put("actionUseAuthorized",false).put("nativeTransferAssumptions","Nominal slope20°, gravity9.81, friction0, mass1kg and contact score1; missing physical calibration is not inferred from validation").put("scope","Passive free forecast and teacher-forced one-step are different tests. No validation-specific fit, rescaling, time offset or altered observations. No real hardware control or causal action identification.").put("rows",rows);
    }
}
