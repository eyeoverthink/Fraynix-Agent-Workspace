package fraymus.workspace;
import org.json.*;

/** One numerical evaluator for normal comparisons and calibration, without fitting logic. */
final class MeasurementEvaluation {
    static JSONObject evaluate(JSONObject dataset,JSONObject run,double tolerance) {
        if(!run.getString("status").equals("completed"))throw new IllegalArgumentException("Evaluation requires a completed native run");
        if(!Double.isFinite(tolerance)||tolerance<0||tolerance>10)throw new IllegalArgumentException("Declare tolerance from 0 to 10 metres");
        var samples=new JSONArray(run.getJSONArray("samples").toString());var last=run.getJSONObject("final");
        if(samples.length()<2)throw new IllegalArgumentException("Native run has too few retained samples");
        if(samples.getJSONObject(samples.length()-1).getDouble("timeS")<last.getDouble("timeS"))samples.put(last);
        var points=new JSONArray();double squares=0,total=0,absolute=0,maximum=0;int within=0,cursor=0;
        double start=samples.getJSONObject(0).getDouble("timeS"),end=last.getDouble("timeS");
        for(Object item:dataset.getJSONArray("rows")){
            var row=(JSONObject)item;double time=row.getDouble("timeS"),observed=row.getDouble("distanceM");
            if(time<start||time>end)throw new IllegalArgumentException("Measurement time "+time+" s is outside recorded run coverage; no extrapolation or silent trimming");
            while(cursor+1<samples.length()&&samples.getJSONObject(cursor+1).getDouble("timeS")<time)cursor++;
            var a=samples.getJSONObject(cursor);var b=samples.getJSONObject(Math.min(cursor+1,samples.length()-1));double ta=a.getDouble("timeS"),tb=b.getDouble("timeS");
            double value=a.getDouble("distanceM")+(tb==ta?0:(time-ta)/(tb-ta)*(b.getDouble("distanceM")-a.getDouble("distanceM")));
            double residual=value-observed;if(!Double.isFinite(residual))throw new IllegalArgumentException("Comparison produced non-finite values");
            squares+=residual*residual;total+=residual;absolute+=Math.abs(residual);maximum=Math.max(maximum,Math.abs(residual));if(Math.abs(residual)<=tolerance)within++;
            points.put(new JSONObject().put("sourceRow",row.getInt("sourceRow")).put("timeS",time).put("observedM",observed).put("simulatedM",value).put("residualM",residual));
        }
        int count=points.length();if(count<2)throw new IllegalArgumentException("At least two observations are required");
        return new JSONObject().put("count",count).put("rmseM",Math.sqrt(squares/count)).put("meanResidualM",total/count).put("maeM",absolute/count).put("maxAbsErrorM",maximum)
            .put("toleranceM",tolerance).put("withinTolerance",within).put("passed",within==count).put("uncertaintyM",dataset.opt("uncertaintyM")).put("points",points);
    }
    static JSONObject summary(JSONObject evaluation){var value=new JSONObject(evaluation.toString());value.remove("points");return value;}
}
