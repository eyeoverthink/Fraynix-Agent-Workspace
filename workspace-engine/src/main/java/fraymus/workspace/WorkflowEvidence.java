package fraymus.workspace;

import org.json.*;
import java.util.*;

/** Reconstructs source rows and chart geometry independently of generated code. */
final class WorkflowEvidence {
    static final String SCOPE="Observation data and a separate native simulation. No physical calibration or causal equivalence is claimed.";
    static final double PLOT_TOLERANCE=.02;
    static String number(double value){return String.format(Locale.ROOT,"%.6f",value);}
    static String cell(String value){return "\""+value.replace("\"","\"\"")+"\"";}
    static void row(StringBuilder csv,String series,double t,double x,String source){csv.append(cell(series)).append(',').append(t).append(',').append(x).append(',').append(cell(source)).append('\n');}
    static JSONObject bundle(JSONObject workflow,JSONObject data,JSONObject dataGoal,JSONObject physicsGoal,JSONObject run) throws Exception {
        var rows=MotionData.originalRows(data);var csv=new StringBuilder("series,time_s,position_m,source_kind\n");
        for(Object raw:rows){var p=(JSONObject)raw;row(csv,"observations",p.getDouble("timeS"),p.getDouble("distanceM"),data.getString("sourceKind"));}
        var innovations=run.getJSONArray("worldInnovations");double sum=0;
        for(Object raw:innovations){var p=(JSONObject)raw;double a=p.getJSONObject("actual").getDouble("distanceM"),pred=p.getJSONObject("predicted").getJSONObject("state").getDouble("x");sum+=(a-pred)*(a-pred);row(csv,"native_actual",p.getInt("tick")/240.0,a,"native simulation");}
        for(Object raw:innovations){var p=(JSONObject)raw;row(csv,"native_prediction",p.getInt("tick")/240.0,p.getJSONObject("predicted").getJSONObject("state").getDouble("x"),"native model forecast");}
        var metadata=new JSONObject().put("workflowId",workflow.getString("id")).put("scope",SCOPE).put("source",MotionData.binding(data)).put("observationSummary",dataGoal.getJSONObject("metrics")).put("nativeSummary",physicsGoal.getJSONObject("metrics")).put("nativeParameters",physicsGoal.getJSONObject("parameters")).put("nativeRunId",run.getString("id")).put("nativeRunSha256",MeasurementStore.hash(run.toString())).put("forecastRmseM",Math.sqrt(sum/innovations.length())).put("plotYMaxM",Math.max(1.8,physicsGoal.getDouble("targetM")+.2)).put("forecastPairs",innovations.length()).put("dataGoalId",dataGoal.getString("id")).put("physicsGoalId",physicsGoal.getString("id")).put("originalSourcePreserved",true).put("calibratedToObservations",false);
        if(workflow.getJSONObject("definition").optBoolean("responsive",false))metadata.put("layoutProfile",ResponsiveLayout.PROFILE);
        String html="<!doctype html><html><meta charset='utf-8'><title>Verified workflow evidence</title><style>body{font:17px system-ui;max-width:900px;margin:40px auto;padding:20px;color:#193c35;background:#eef4f0}code{overflow-wrap:anywhere}</style><h1>Source evidence for the creation stage</h1><p>"+SCOPE+"</p><p>"+rows.length()+" original observations and "+innovations.length()+" actual native forecast pairs. Original CSV, qualified combined rows and numeric summaries are preserved in this artifact.</p><p>Workflow <code>"+workflow.getString("id")+"</code></p></html>";
        return new JSONObject().put("title","Evidence bundle · "+workflow.getString("id").substring(0,8)).put("files",new JSONObject().put("index.html",html).put("evidence.csv",csv.toString()).put("original-observations.csv",data.getString("csv")).put("evidence.json",metadata.toString(2))).put("sources",new JSONArray(List.of("workflow:"+workflow.getString("id"),"dataset:"+data.getString("datasetId")+":"+data.getInt("version"),"native-run:"+run.getString("id"))));
    }
    static JSONArray checks(JSONObject metadata){
        String quality=metadata.getJSONObject("observationSummary").getBoolean("qualityPassed")?"Within declared thresholds":"Quality flags retained";
        return new JSONArray().put(new JSONObject().put("selector","#row-count").put("sourceRows",true))
            .put(new JSONObject().put("selector","#filter").put("action","fill").put("value","observations"))
            .put(new JSONObject().put("selector","#row-count").put("sourceCount",new JSONObject().put("column","series").put("equals","observations")))
            .put(new JSONObject().put("selector","#filter").put("action","fill").put("value","native_prediction"))
            .put(new JSONObject().put("selector","#row-count").put("sourceCount",new JSONObject().put("column","series").put("equals","native_prediction")))
            .put(new JSONObject().put("selector","#reset").put("action","click"))
            .put(new JSONObject().put("selector","#row-count").put("sourceRows",true))
            .put(new JSONObject().put("selector","#filter").put("expectValue",""))
            .put(new JSONObject().put("selector","#scope").put("expectText",SCOPE))
            .put(new JSONObject().put("selector","#data-quality").put("expectText",quality))
            .put(new JSONObject().put("selector","#endpoint-error").put("expectText",number(metadata.getJSONObject("nativeSummary").getDouble("endpointErrorM"))))
            .put(new JSONObject().put("selector","#prediction-error").put("expectText",number(metadata.getDouble("forecastRmseM"))))
            .put(new JSONObject().put("selector","svg polyline[data-series='native_actual']").put("expectCount",1))
            .put(new JSONObject().put("selector","svg polyline[data-series='native_prediction']").put("expectCount",1));
    }
    static JSONObject creation(JSONObject workflow,JSONObject artifact){
        var metadata=new JSONObject(artifact.getJSONObject("contents").getString("evidence.json"));var spec=workflow.getJSONObject("definition");double ymax=Math.max(1.8,spec.getJSONObject("physics").getDouble("targetM")+.2);
        String request="Create a responsive evidence explorer for the real source FILE inputs/source.json written by Java. Load it with await (await fetch('inputs/source.json')).json(); no source globals exist. "+
            "Use text heading #title, digit-only text #row-count, input #filter and button #reset. #filter matches exact case-sensitive series values; empty shows every row. Reset clears it. Table #data-table tbody must render EVERY original row in original order, one td per source.headers entry in that exact order, using unchanged source string cells via textContent; no rounding, extra columns, pagination or invented values. "+
            "Render two visible SVG polylines in the SAME svg with viewBox='0 0 800 300': data-series='native_actual' and data-series='native_prediction'. Use only those source rows for their respective line, in original order. For each point t=Number(row.time_s),p=Number(row.position_m): x=40+720*t/1.5,y=260-220*p/"+ymax+". Keep at least3 decimals. A POLYLINE uses its points attribute with space-separated x,y pairs: polyline.setAttribute('points',pairs.map(([x,y])=>x.toFixed(3)+','+y.toFixed(3)).join(' ')). NEVER set a path d attribute on a polyline: it renders nothing and yields zero SVG points. Do not chart observation rows in that native graph. Add meaningful labelled axes and a legend. Native SVG only, no libraries/CDNs. "+
            "Show this exact visible text in #scope: "+SCOPE+" Show exact text #data-quality: "+(metadata.getJSONObject("observationSummary").getBoolean("qualityPassed")?"Within declared thresholds":"Quality flags retained")+". Text #endpoint-error must be "+number(metadata.getJSONObject("nativeSummary").getDouble("endpointErrorM"))+" (m); text #prediction-error must be "+number(metadata.getDouble("forecastRmseM"))+" (m). These are text elements, not inputs. "+
            ((metadata.has("layoutProfile")?"Layout is independently tested at780px and390px. Use normal semantic table layout: thead and tbody stay table groups, th are sticky within an overflow-auto wrapper; NEVER position thead absolutely or set tbody display:block. Align all four header columns with body cells. Use table width:100%;table-layout:fixed; th,td font-size:14px and overflow-wrap:anywhere. Root box-sizing:border-box, responsive body, controls flex-wrap with input min-width:0; SVG width:100%;height:auto. No document horizontal overflow or clipped controls. ":""))+
            "The source and simulation have separate authority. Quality flags are not discarded and the native simulation is not a fitted physical experiment. USER INTENT (the explicit source/contract above remains authoritative): "+spec.getString("request");
        return new JSONObject().put("title",spec.getString("title")).put("request",request).put("modelName",spec.getString("modelName")).put("sourceArtifactId",artifact.getString("id")).put("sourceVersion",artifact.getInt("version")).put("sourceFile","evidence.csv").put("checks",checks(metadata));
    }
    static JSONObject audit(JSONObject artifact,JSONObject bundle,JSONObject actual) throws Exception {
        SpatialScene.keys(actual,"id","version","rows","plots","layout");if(!actual.getString("id").equals(artifact.getString("id"))||actual.getInt("version")!=artifact.getInt("version"))throw new IllegalStateException("Independent audit belongs to another artifact");
        String csv=bundle.getJSONObject("contents").getString("evidence.csv");var parsed=MeasurementStore.csv(csv);var rows=actual.getJSONArray("rows");var errors=new JSONArray();
        if(rows.length()!=parsed.size()-1)errors.put("Displayed row count differs from original bundle");
        for(int i=0;i<Math.min(rows.length(),parsed.size()-1);i++){var row=rows.getJSONArray(i);if(!row.toList().equals(parsed.get(i+1))){errors.put("Row "+i+" expected "+parsed.get(i+1)+", observed "+row);if(errors.length()>=4)break;}}
        var metadata=new JSONObject(bundle.getJSONObject("contents").getString("evidence.json"));double ymax=metadata.getDouble("plotYMaxM");var plots=actual.getJSONArray("plots");double maxError=0;
        if(plots.length()!=2)errors.put("Exactly two native forecast/actual polylines are required");
        for(String series:List.of("native_actual","native_prediction")){
            JSONObject plot=null;int matches=0;for(Object raw:plots){var p=(JSONObject)raw;if(p.optString("series").equals(series)){plot=p;matches++;}}
            if(matches!=1){errors.put("Missing/duplicate plot "+series);continue;}if(!plot.optBoolean("visible")||!plot.getJSONArray("viewBox").similar(new JSONArray(List.of(0,0,800,300))))errors.put("Plot "+series+" is hidden or uses a different coordinate frame");
            var expected=new ArrayList<List<Double>>();for(var row:parsed.subList(1,parsed.size()))if(row.getFirst().equals(series)){double t=Double.parseDouble(row.get(1)),x=Double.parseDouble(row.get(2));expected.add(List.of(40+720*t/1.5,260-220*x/ymax));}
            var points=plot.getJSONArray("points");if(points.length()!=expected.size())errors.put("Plot "+series+" requires "+expected.size()+" SVGPointList entries in its points attribute; observed "+points.length()+". A polyline uses points='x,y x,y ...', not a path d attribute.");
            for(int i=0;i<Math.min(points.length(),expected.size());i++){var p=points.getJSONArray(i);if(p.length()!=2)throw new IllegalArgumentException("Actual SVG points need x/y");double error=Math.max(Math.abs(p.getDouble(0)-expected.get(i).get(0)),Math.abs(p.getDouble(1)-expected.get(i).get(1)));if(!Double.isFinite(error))throw new IllegalArgumentException("Nonfinite actual plot point");maxError=Math.max(maxError,error);}
        }
        if(maxError>PLOT_TOLERANCE)errors.put("Native plot geometry error "+maxError+" exceeds "+PLOT_TOLERANCE+" SVG units");
        JSONObject layout=null;if(metadata.has("layoutProfile")){layout=ResponsiveLayout.verify(actual.getJSONArray("layout"));for(Object error:layout.getJSONArray("errors"))errors.put(error);}
        var result=new JSONObject().put("passed",errors.isEmpty()).put("rowCount",rows.length()).put("tableSha256",MeasurementStore.hash(rows.toString())).put("maxPlotError",maxError).put("plotTolerance",PLOT_TOLERANCE).put("errors",errors).put("artifact",Creation.reference(artifact));if(layout!=null)result.put("layout",layout);return result;
    }
}
