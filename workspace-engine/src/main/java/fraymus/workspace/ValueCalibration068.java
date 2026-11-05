package fraymus.workspace;
import org.json.*;
import java.util.*;

/** Weighted monotonic calibration of actual native recovery labels; no external ML runtime. */
final class ValueCalibration068 {
    static JSONObject fit(JSONArray rows){
        int[] n=new int[10],success=new int[10];var labels=new JSONArray();
        for(Object raw:rows){var row=(JSONObject)raw;if(!row.has("screen"))continue;
            double p=row.getJSONObject("screen").getDouble("successFraction");int hits=(int)Math.round(p*9);
            if(hits<0||hits>9||Math.abs(p-hits/9.0)>1e-12)throw new IllegalStateException("Invalid scenario score");
            var outcome=row.getJSONObject("outcome");if(!outcome.getBoolean("executed"))throw new IllegalStateException("Calibration label has no real attempt");
            boolean passed=outcome.getBoolean("success");n[hits]++;if(passed)success[hits]++;
            labels.put(new JSONObject().put("taskSha256",WorldFrames.hash(row.getJSONObject("task"))).put("recordSha256",row.getJSONObject("record").getString("sha256")).put("hits",hits).put("success",passed));
        }
        if(labels.isEmpty())throw new IllegalArgumentException("No native calibration labels");
        var knots=new JSONArray();var blocks=new ArrayList<double[]>();
        for(int hit=0;hit<10;hit++)if(n[hit]>0){
            int index=knots.length();knots.put(new JSONObject().put("hits",hit).put("count",n[hit]).put("successes",success[hit]).put("smoothedRate",(success[hit]+1.0)/(n[hit]+2)).put("weight",n[hit]+2));
            blocks.add(new double[]{index,index,success[hit]+1.0,n[hit]+2.0});
            while(blocks.size()>1){var a=blocks.get(blocks.size()-2);var b=blocks.get(blocks.size()-1);if(a[2]/a[3]<=b[2]/b[3])break;a[1]=b[1];a[2]+=b[2];a[3]+=b[3];blocks.remove(blocks.size()-1);}
        }
        var merged=new JSONArray();for(var b:blocks){double p=b[2]/b[3];for(int i=(int)b[0];i<=b[1];i++)knots.getJSONObject(i).put("rate",p);
            merged.put(new JSONObject().put("firstKnot",(int)b[0]).put("lastKnot",(int)b[1]).put("positiveWeight",b[2]).put("totalWeight",b[3]).put("rate",p));}
        var model=new JSONObject().put("format","recovery-pav068-v1").put("labels",labels).put("knots",knots).put("blocks",merged)
            .put("trainingSha256",WorldFrames.hash(new JSONObject().put("labels",labels))).put("scope","Calibration-only native outcomes; finite-data monotonic estimate, not a confidence guarantee");
        model.put("sha256",WorldFrames.hash(model));return model;
    }
    static double predict(JSONObject model,double raw){
        verify(model);if(!Double.isFinite(raw)||raw<0||raw>1)throw new IllegalArgumentException("Score outside [0,1]");
        var k=model.getJSONArray("knots");double x=raw*9;
        if(x<=k.getJSONObject(0).getInt("hits"))return k.getJSONObject(0).getDouble("rate");
        for(int i=1;i<k.length();i++){var a=k.getJSONObject(i-1);var b=k.getJSONObject(i);if(x<=b.getInt("hits")){
            double t=(x-a.getInt("hits"))/(b.getInt("hits")-a.getInt("hits"));return a.getDouble("rate")+t*(b.getDouble("rate")-a.getDouble("rate"));}}
        return k.getJSONObject(k.length()-1).getDouble("rate");
    }
    static void verify(JSONObject model){var copy=WorldFrames.copy(model);copy.remove("sha256");
        if(!model.getString("format").equals("recovery-pav068-v1")||!model.getString("sha256").equals(WorldFrames.hash(copy)))throw new IllegalStateException("Calibration identity changed");
        var k=model.getJSONArray("knots");if(k.isEmpty())throw new IllegalStateException("Empty calibration");double last=-1;int hit=-1;
        for(Object raw:k){var p=(JSONObject)raw;double rate=p.getDouble("rate");if(!Double.isFinite(rate)||rate<last||rate<0||rate>1||p.getInt("hits")<=hit)throw new IllegalStateException("Calibration is not monotonic");last=rate;hit=p.getInt("hits");}
    }
}
