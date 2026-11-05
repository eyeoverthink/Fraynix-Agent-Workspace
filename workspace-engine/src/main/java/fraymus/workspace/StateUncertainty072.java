package fraymus.workspace;

import org.json.*;
import java.util.*;

/** Separate-source, joint position/speed calibration. The observer does not see future truth. */
final class StateUncertainty072 {
    static JSONObject label(JSONObject record) {
        UncertainTrial071.verify(record);
        if (!record.getJSONObject("decision").getString("method").equals("blind") || !record.getJSONObject("disturbance").getString("family").equals("none")) throw new IllegalStateException("Clean blind calibration required");
        var task=record.getJSONObject("task");var decision=record.getJSONObject("decision");
        var anchor=ObservationSchedule070.anchor(task,decision,record.optJSONObject("correctionPlan"));var values=new JSONArray();
        for(int tick:Uncertainty071.TICKS){
            double[] prediction=ObservationSchedule070.advance(task,anchor.getDouble("distanceM"),anchor.getDouble("tangentMps"),(tick-60)/240.0);
            var actual=Stopping063.projected(record.getJSONObject("definition"),record.getJSONArray("samples").getJSONObject(tick/4));
            values.put(actual.getDouble("distanceM")-prediction[0]).put(actual.getDouble("tangentMps")-prediction[1]);
        }
        return new JSONObject().put("cell",Uncertainty071.cell(task,decision)).put("recordId",record.getString("id")).put("recordSha256",record.getString("sha256")).put("residuals",values);
    }
    static JSONObject fit(JSONArray fitting,JSONArray calibration) {
        var fit=new JSONArray();var cal=new JSONArray();var ids=new HashSet<String>();
        for(var phase:List.of(fitting,calibration))for(Object raw:phase){var l=label(((JSONObject)raw).getJSONObject("record"));if(!ids.add(l.getString("recordId")))throw new IllegalStateException("Reused training identity");(phase==fitting?fit:cal).put(l);}
        return new JSONObject().put("state",fitLabels(fit,cal)).put("distance",Uncertainty071.fit(fitting,calibration));
    }
    static JSONObject fitLabels(JSONArray fit,JSONArray calibration) {
        if(fit.length()!=24||calibration.length()!=36)throw new IllegalStateException("Incomplete separate training sets");var cells=new JSONObject();
        for(String cell:Uncertainty071.CELLS){
            var train=new ArrayList<JSONObject>();var cal=new ArrayList<JSONObject>();
            for(Object raw:fit)if(((JSONObject)raw).getString("cell").equals(cell))train.add((JSONObject)raw);
            for(Object raw:calibration)if(((JSONObject)raw).getString("cell").equals(cell))cal.add((JSONObject)raw);
            if(train.size()!=6||cal.size()!=9)throw new IllegalStateException("Incomplete four-cell calibration");
            var centers=new JSONArray();var scales=new JSONArray();
            for(int k=0;k<8;k++){var values=new ArrayList<Double>();for(var row:train)values.add(finite(row.getJSONArray("residuals").getDouble(k)));double center=Uncertainty071.median(values);var errors=new ArrayList<Double>();for(double v:values)errors.add(Math.abs(v-center));centers.put(center);scales.put(Math.max(k%2==0?.002:.01,Uncertainty071.median(errors)));}
            var scores=new JSONArray();var ordered=new ArrayList<Double>();
            for(var row:cal){double score=0;for(int k=0;k<8;k++)score=Math.max(score,Math.abs(finite(row.getJSONArray("residuals").getDouble(k))-centers.getDouble(k))/scales.getDouble(k));ordered.add(score);scores.put(new JSONObject().put("recordId",row.getString("recordId")).put("score",score));}
            ordered.sort(Double::compare);
            cells.put(cell,new JSONObject().put("fitCount",6).put("calibrationCount",9).put("center",centers).put("scale",scales).put("scores",scores).put("rank",9).put("multiplier",ordered.get(8)));
        }
        var m=new JSONObject().put("protocol","joint-state-residual-072-v1").put("ticks",new JSONArray(Uncertainty071.TICKS)).put("components",new JSONArray(List.of("distanceM","tangentMps"))).put("nominalCoverage",.90).put("distanceFloorM",.002).put("speedFloorMps",.01).put("fitLabels",fit).put("calibrationLabels",calibration).put("cells",cells).put("scope","Eight-component nominal joint calibration in native known-geometry worlds. Four discrete times; no physical, continuous-time or changed-condition coverage guarantee.");m.put("sha256",WorldFrames.hash(m));return m;
    }
    private static double finite(double v){if(!Double.isFinite(v))throw new IllegalArgumentException("Nonfinite training value");return v;}
    static void verify(JSONObject models){
        if(models==null||!models.keySet().equals(Set.of("state","distance")))throw new IllegalStateException("Missing or changed model bundle");
        var m=models.getJSONObject("state");if(!fitLabels(m.getJSONArray("fitLabels"),m.getJSONArray("calibrationLabels")).similar(m))throw new IllegalStateException("State calibration changed");Uncertainty071.verify(models.getJSONObject("distance"));
        for(String partition:List.of("fitLabels","calibrationLabels")){
            var a=m.getJSONArray(partition);var b=models.getJSONObject("distance").getJSONArray(partition);
            for(int i=0;i<a.length();i++){var x=a.getJSONObject(i);var y=b.getJSONObject(i);for(String key:List.of("cell","recordId","recordSha256"))if(!x.getString(key).equals(y.getString(key)))throw new IllegalStateException("Model training sources differ");for(int k=0;k<4;k++)if(x.getJSONArray("residuals").getDouble(k*2)!=y.getJSONArray("residualsM").getDouble(k))throw new IllegalStateException("Distance labels disagree");}
        }
    }
    static JSONObject forecast(JSONObject task,JSONObject decision,JSONObject first,JSONObject models){
        verify(models);var m=models.getJSONObject("state");var anchor=ObservationSchedule070.anchor(task,decision,first);String cell=Uncertainty071.cell(task,decision);var fitted=m.getJSONObject("cells").getJSONObject(cell);var bands=new JSONArray();
        for(int i=0;i<4;i++){
            int tick=Uncertainty071.TICKS.get(i);var nominal=ObservationSchedule070.advance(task,anchor.getDouble("distanceM"),anchor.getDouble("tangentMps"),(tick-60)/240.0);var band=new JSONObject().put("tick",tick);
            for(int k=0;k<2;k++){double center=nominal[k]+fitted.getJSONArray("center").getDouble(i*2+k),radius=fitted.getDouble("multiplier")*fitted.getJSONArray("scale").getDouble(i*2+k);band.put(k==0?"distance":"speed",new JSONObject().put("nominal",nominal[k]).put("center",center).put("lower",center-radius).put("upper",center+radius).put("residualCenter",center-nominal[k]).put("radius",radius));}
            bands.put(band);
        }
        return new JSONObject().put("cell",cell).put("modelSha256",m.getString("sha256")).put("anchor",anchor).put("bands",bands).put("scope",m.getString("scope"));
    }
    static JSONObject coverage(JSONObject blind,JSONObject models){
        UsefulTrial072.verify(blind);if(!blind.getJSONObject("decision").getString("method").equals("blind")||blind.has("laterPlan"))throw new IllegalStateException("Only paired blind future can label coverage");
        var f=forecast(blind.getJSONObject("task"),blind.getJSONObject("decision"),blind.optJSONObject("correctionPlan"),models);boolean joint=true,distance=true;
        for(Object raw:f.getJSONArray("bands")){var b=(JSONObject)raw;var actual=Stopping063.projected(blind.getJSONObject("definition"),blind.getJSONArray("samples").getJSONObject(b.getInt("tick")/4));for(String key:List.of("distance","speed")){var q=b.getJSONObject(key);double value=actual.getDouble(key.equals("distance")?"distanceM":"tangentMps");boolean covered=value>=q.getDouble("lower")-1e-12&&value<=q.getDouble("upper")+1e-12;q.put("actual",value).put("covered",covered);joint&=covered;if(key.equals("distance"))distance&=covered;}}
        var end=f.getJSONArray("bands").getJSONObject(3);return f.put("truthRecordId",blind.getString("id")).put("jointCovered",joint).put("distanceCovered",distance).put("terminalCovered",end.getJSONObject("distance").getBoolean("covered")).put("terminalWidthM",end.getJSONObject("distance").getDouble("upper")-end.getJSONObject("distance").getDouble("lower")).put("terminalSpeedWidthMps",end.getJSONObject("speed").getDouble("upper")-end.getJSONObject("speed").getDouble("lower"));
    }
}
