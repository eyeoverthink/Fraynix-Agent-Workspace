package fraymus.workspace;

import org.json.*;
import java.util.*;

/** Working source/bounds/outcome report; Java independently checks actual rendered states. */
final class CalibratedReport159 {
    static String number(double x){return String.format(Locale.ROOT,"%.6f",x);}
    static JSONObject data(JSONObject w){
        var rows=new JSONArray();var decision=w.getJSONObject("decision159");var outcome=w.getJSONObject("outcome159");
        double minX=Double.POSITIVE_INFINITY,minZ=minX,maxX=-minX,maxZ=-minX;
        for(int i=0;i<decision.getJSONArray("bounds").length();i++){
            var b=decision.getJSONArray("bounds").getJSONObject(i);var actual=outcome.getJSONArray("rows").getJSONObject(i);
            var r=new JSONObject().put("bound",b).put("outcome",actual);rows.put(r);
            minX=Math.min(minX,Math.min(b.getJSONArray("minimumM").getDouble(0),actual.getJSONArray("actualPositionM").getDouble(0)));
            maxX=Math.max(maxX,Math.max(b.getJSONArray("maximumM").getDouble(0),actual.getJSONArray("actualPositionM").getDouble(0)));
            minZ=Math.min(minZ,Math.min(b.getJSONArray("minimumM").getDouble(2),actual.getJSONArray("actualPositionM").getDouble(2)));
            maxZ=Math.max(maxZ,Math.max(b.getJSONArray("maximumM").getDouble(2),actual.getJSONArray("actualPositionM").getDouble(2)));
        }
        var source=w.getJSONObject("calibration159");
        return new JSONObject().put("rows",rows).put("plot",new JSONArray(new double[]{minX-.02,minZ-.02,maxX+.02,maxZ+.02}))
            .put("limits",CalibratedPlane159.LIMIT).put("source",source).put("decision",decision).put("outcome",outcome);
    }
    static JSONArray cells(JSONObject row){
        var b=row.getJSONObject("bound");var a=row.getJSONObject("outcome");var out=new JSONArray().put(b.getString("sourceId")).put(b.getString("name"));
        for(String key:List.of("centerM","minimumM","maximumM"))for(int axis:new int[]{0,2})out.put(number(b.getJSONArray(key).getDouble(axis)));
        for(int axis:new int[]{0,2})out.put(number(a.getJSONArray("actualPositionM").getDouble(axis)));
        for(double v:new double[]{a.getDouble("planarErrorM"),b.getDouble("radiusM"),a.getDouble("combinedErrorBudgetM")})out.put(number(v));return out;
    }
    static JSONObject build(JSONObject w){
        var data=data(w);var cells=new JSONArray();for(Object raw:data.getJSONArray("rows"))cells.put(cells((JSONObject)raw));data.put("cells",cells);
        String html="""
            <!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Calibrated recorded actions</title><style>
            :root{font:16px system-ui;color:#e6f3ef;background:#091c22;color-scheme:dark}body{margin:24px}h1{font-size:clamp(25px,4vw,42px)}p{line-height:1.6;overflow-wrap:anywhere}section{border:1px solid #45636a;border-radius:12px;padding:20px;margin:20px 0;background:#142e35}#limits{border-color:#c8a34b}select{padding:10px;background:#183b43;color:#fff;border:1px solid #7faca9}svg{display:block;width:100%;border:1px solid #54757c;margin-top:16px;background:#08171d}rect{stroke:#81dec2;stroke-width:2;fill:#82dec233}circle{fill:#ffcc79}table{border-collapse:collapse;min-width:1000px;width:100%}td,th{padding:10px;border-bottom:1px solid #416066;text-align:left}small{color:#a8ceca}.scroll{overflow:auto}pre{white-space:pre-wrap;overflow-wrap:anywhere}button{padding:8px}
            </style></head><body><small>FRAYNIX / EDITION 159 / RECORDED REFERENCE → VERIFIED SIMULATION</small><h1>Measured plane. Bounded action.</h1><section id="limits"></section><section><h2>Coordinates and actual outcome</h2><p>Rectangles show conditional coordinate regions; dots show actual committed simulation-marker positions. The marker's height is authored geometry. Plot symbols are not physical object sizes.</p><label>View <select id="mode"><option value="all">Bounds and actual outcome</option><option value="bounds">Coordinate bounds</option><option value="actual">Actual simulation outcome</option></select></label><svg id="plot" viewBox="0 0 800 450" aria-label="Native X and Z metres"></svg><p id="axes"></p></section><section><h2>Original coordinates and checked results</h2><p>All numerical distances are metres; image annotations and their declared pixel budget remain in the evidence.</p><div class="scroll"><table><thead><tr><th>Marker</th><th>Reference</th><th>Target X</th><th>Target Z</th><th>Min X</th><th>Min Z</th><th>Max X</th><th>Max Z</th><th>Actual X</th><th>Actual Z</th><th>Action error</th><th>Coordinate radius</th><th>Combined budget</th></tr></thead><tbody id="rows"></tbody></table></div></section><section><h2>Recorded source and independent checks</h2><p id="source"></p><p id="checks"></p><p id="world"></p><details><summary>Full retained numerical evidence</summary><pre id="evidence"></pre></details></section>
            """;
        html+="<script id=\"data\" type=\"application/json\">"+data.toString().replace("</","<\\/")+"</script>";
        html+="""
            <script>const d=JSON.parse(document.querySelector('#data').textContent),$=s=>document.querySelector(s),n=x=>x.toFixed(6);$('#limits').textContent=d.limits;for(const row of d.cells){const tr=document.createElement('tr');for(const value of row){const td=document.createElement('td');td.textContent=value;tr.append(td)}$('#rows').append(tr)}
            $('#source').textContent='Registration '+d.source.registration.id+' / original frame '+d.source.frameSha256+'. Recorded reference only.';
            $('#checks').textContent='Independent grid residual: '+n(d.source.fit.maxCheckErrorM)+' m; tape discrepancy: '+n(d.source.fit.tape.absoluteErrorM)+' m. Declared budgets are not measured coverage probabilities.';
            $('#world').textContent='Committed simulation goal '+d.outcome.goalId+' / revision hash '+d.outcome.commitHash+'. No hardware action or live object pose.';$('#axes').textContent='Native X and Z in metres. Display limits: '+d.plot.map(n).join(', ');$('#evidence').textContent=JSON.stringify(d,null,2);
            function xy(v){return[40+720*(v[0]-d.plot[0])/(d.plot[2]-d.plot[0]),40+370*(v[2]-d.plot[1])/(d.plot[3]-d.plot[1])]}function draw(){$('#plot').replaceChildren();for(const r of d.rows){if($('#mode').value!=='actual'){const lo=xy(r.bound.minimumM),hi=xy(r.bound.maximumM),e=document.createElementNS('http://www.w3.org/2000/svg','rect');for(const[k,v]of Object.entries({x:lo[0],y:lo[1],width:hi[0]-lo[0],height:hi[1]-lo[1]}))e.setAttribute(k,Math.fround(v));$('#plot').append(e)}if($('#mode').value!=='bounds'){const p=xy(r.outcome.actualPositionM),e=document.createElementNS('http://www.w3.org/2000/svg','circle');e.setAttribute('cx',Math.fround(p[0]));e.setAttribute('cy',Math.fround(p[1]));e.setAttribute('r',4);$('#plot').append(e)}}}$('#mode').onchange=draw;draw();</script></body></html>
            """;
        return new JSONObject().put("title","Calibrated recorded action").put("entrypoint","index.html").put("files",new JSONObject().put("index.html",html).put("calibrated.json",data.toString(2)));
    }
    private static double[] xy(JSONArray v,JSONArray p){return new double[]{40+720*(v.getDouble(0)-p.getDouble(0))/(p.getDouble(2)-p.getDouble(0)),40+370*(v.getDouble(2)-p.getDouble(1))/(p.getDouble(3)-p.getDouble(1))};}
    static JSONObject audit(JSONObject w,JSONObject artifact,JSONObject actual)throws Exception{
        var d=data(w);var expectedRows=new JSONArray();for(Object raw:d.getJSONArray("rows"))expectedRows.put(cells((JSONObject)raw));
        var errors=new JSONArray();
        if(!actual.optString("id").equals(artifact.getString("id"))||actual.optInt("version")!=artifact.getInt("version")||!expectedRows.similar(actual.optJSONArray("rows"))||!CalibratedPlane159.LIMIT.equals(actual.optString("limits")))errors.put("Original rows, artifact or uncertainty statement differs");
        String source="Registration "+d.getJSONObject("source").getJSONObject("registration").getString("id")+" / original frame "+d.getJSONObject("source").getString("frameSha256")+". Recorded reference only.";
        String world="Committed simulation goal "+d.getJSONObject("outcome").getString("goalId")+" / revision hash "+d.getJSONObject("outcome").getString("commitHash")+". No hardware action or live object pose.";
        if(!source.equals(actual.optString("source"))||!world.equals(actual.optString("world")))errors.put("Source or committed world label differs");
        String checks="Independent grid residual: "+number(d.getJSONObject("source").getJSONObject("fit").getDouble("maxCheckErrorM"))+" m; tape discrepancy: "+number(d.getJSONObject("source").getJSONObject("fit").getJSONObject("tape").getDouble("absoluteErrorM"))+" m. Declared budgets are not measured coverage probabilities.";
        var values=new ArrayList<String>();for(Object n:d.getJSONArray("plot"))values.add(number(((Number)n).doubleValue()));
        if(!checks.equals(actual.optString("checks"))||!("Native X and Z in metres. Display limits: "+String.join(", ",values)).equals(actual.optString("axes")))errors.put("Independent checks or plotted units differ");
        var states=actual.optJSONArray("states");
        if(states==null||states.length()!=3)errors.put("All three real chart controls required");
        else for(int k=0;k<3;k++){
            String mode=List.of("all","bounds","actual").get(k);var state=states.getJSONObject(k);var rects=new JSONArray();var circles=new JSONArray();
            for(Object raw:d.getJSONArray("rows")){
                var row=(JSONObject)raw;double[] a=xy(row.getJSONObject("bound").getJSONArray("minimumM"),d.getJSONArray("plot")),b=xy(row.getJSONObject("bound").getJSONArray("maximumM"),d.getJSONArray("plot")),c=xy(row.getJSONObject("outcome").getJSONArray("actualPositionM"),d.getJSONArray("plot"));
                if(!mode.equals("actual"))rects.put(new JSONObject().put("x",(double)(float)a[0]).put("y",(double)(float)a[1]).put("width",(double)(float)(b[0]-a[0])).put("height",(double)(float)(b[1]-a[1])).put("visible",true));
                if(!mode.equals("bounds"))circles.put(new JSONObject().put("cx",(double)(float)c[0]).put("cy",(double)(float)c[1]).put("visible",true));
            }
            if(!mode.equals(state.optString("mode"))||!state.optBoolean("visible")||!rects.similar(state.optJSONArray("rectangles"))||!circles.similar(state.optJSONArray("circles")))errors.put("Rendered coordinates or visibility differ for "+mode);
        }
        return new JSONObject().put("passed",errors.isEmpty()).put("errors",errors).put("artifact",Creation.reference(artifact)).put("dataSha256",WorldFrames.hash(d)).put("observed",WorldFrames.copy(actual));
    }
}
