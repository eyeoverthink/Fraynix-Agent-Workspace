package fraymus.workspace;

import org.json.*;
import java.util.*;

/** Original native metrics, linked revisions and actual rendered coordinates, never a passing flag. */
final class GraphReport046 {
    private GraphReport046(){}
    static JSONArray rows(JSONObject w){
        var rows=new JSONArray();for(Object raw:w.getJSONArray("stages")){var s=(JSONObject)raw;if(!s.has("output"))continue;var o=s.getJSONObject("output");var m=o.getJSONObject("metrics");
            rows.put(new JSONArray(List.of(s.getString("name"),o.getString("goalId"),o.getJSONObject("input").getInt("expectedRevision"),o.getJSONObject("world").getInt("expectedRevision"),m.getDouble("timeS"),m.getDouble("maxPositionErrorM"),m.getDouble("effortNs"),o.getString("nativeSha256"))));
        }return rows;
    }
    static JSONArray points(JSONArray rows){var p=new JSONArray();double total=0;for(Object r:rows)total+=((JSONArray)r).getDouble(4);double time=0;for(int i=0;i<rows.length();i++){time+=rows.getJSONArray(i).getDouble(4);p.put(new JSONArray(List.of(60+160*i,260-200*time/Math.max(1,total))));}return p;}
    static JSONObject data(JSONObject w){return new JSONObject().put("schema",SkillGraph046.SCHEMA).put("workflowId",w.getString("id")).put("graphSha256",w.getString("graphSha256")).put("rows",rows(w)).put("points",points(rows(w))).put("scope",SkillGraph046.SCOPE);}
    static String esc(Object v){return String.valueOf(v).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
    static JSONObject build(JSONObject w){
        var data=data(w);var rows=data.getJSONArray("rows");var body=new StringBuilder();var points=new StringBuilder();
        for(Object raw:rows){body.append("<tr>");for(Object cell:(JSONArray)raw)body.append("<td>").append(esc(cell)).append("</td>");body.append("</tr>");}
        for(Object raw:data.getJSONArray("points")){var p=(JSONArray)raw;points.append(p.getDouble(0)).append(',').append(p.getDouble(1)).append(' ');}
        String html="<!doctype html><html lang='en'><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1'><title>Verified procedure graph</title><style>body{font:16px system-ui;background:#121321;color:#e6e7f3;margin:0;padding:clamp(16px,4vw,46px)}main{max-width:1150px;margin:auto}h1{font-size:clamp(25px,4vw,42px)}p{line-height:1.6;color:#bec3d8}.pill{color:#74e2bd}.scroll{overflow:auto;border:1px solid #43445a;border-radius:12px}table{border-collapse:collapse;width:100%;font-size:13px}td,th{padding:12px;border-bottom:1px solid #34364a;text-align:left;white-space:nowrap}td:nth-child(2),td:nth-child(8){font:11px monospace}svg{width:100%;max-height:330px;background:#1b1e32;border-radius:12px;margin:20px 0}polyline{fill:none;stroke:#74e2bd;stroke-width:3}text{fill:#c7c9dd}</style><main><p class='pill'>JAVA · VERIFIED SKILL GRAPH</p><h1>One world, successive checked goals</h1><p id='graph-summary'>"+rows.length()+" procedures · exact source handoffs · original physical and report checks</p><p>Each point is cumulative simulated time after a verified commit; horizontal spacing is procedure order. This is simulation evidence, not a camera measurement.</p><svg viewBox='0 0 800 300' role='img' aria-label='Cumulative simulated time by procedure'><text x='40' y='285'>Procedure order → · cumulative simulated time upwards (scaled to total duration)</text><polyline id='graph-line' points='"+points+"'/></svg><div class='scroll'><table id='graph-table'><thead><tr><th>Node</th><th>Owned goal</th><th>Input revision</th><th>Output revision</th><th>Time s</th><th>Error m</th><th>Effort N·s</th><th>Original native SHA256</th></tr></thead><tbody>"+body+"</tbody></table></div><p id='graph-scope'>"+esc(SkillGraph046.SCOPE)+"</p><script type='application/json' id='graph-data'>"+data.toString().replace("<","\\u003c")+"</script></main></html>";
        return new JSONObject().put("title","Verified procedure graph · "+w.getString("title")).put("files",new JSONObject().put("index.html",html).put("graph.json",data.toString(2)));
    }
    static JSONObject audit(JSONObject w,JSONObject actual,JSONObject artifact) throws Exception {
        SpatialScene.keys(actual,"id","version","data","rows","points","visible","viewBox");
        var d=data(w);if(!actual.getString("id").equals(artifact.getString("id"))||actual.getInt("version")!=artifact.getInt("version")||!actual.getBoolean("visible")||!actual.getJSONObject("data").similar(d)||!actual.getJSONArray("viewBox").similar(new JSONArray(List.of(0,0,800,300))))throw new IllegalStateException("Actual graph report identity/data/visibility changed");
        var rows=d.getJSONArray("rows");var observed=actual.getJSONArray("rows");if(rows.length()!=observed.length())throw new IllegalStateException("Graph report lost a procedure row");
        for(int i=0;i<rows.length();i++){var expected=rows.getJSONArray(i);var row=observed.getJSONArray(i);if(row.length()!=expected.length())throw new IllegalStateException("Graph table column count differs");for(int j=0;j<row.length();j++)if(!row.getString(j).equals(String.valueOf(expected.get(j))))throw new IllegalStateException("Graph report cell differs: "+i+","+j);}
        var points=d.getJSONArray("points");var seen=actual.getJSONArray("points");if(points.length()!=seen.length())throw new IllegalStateException("Graph chart lost a commit");double error=0;
        for(int i=0;i<points.length();i++){if(seen.getJSONArray(i).length()!=2)throw new IllegalStateException("Graph point dimension differs");for(int j=0;j<2;j++){double delta=Math.abs(points.getJSONArray(i).getDouble(j)-seen.getJSONArray(i).getDouble(j));if(!Double.isFinite(delta)||delta>.01)throw new IllegalStateException("Graph chart coordinate changed");error=Math.max(error,delta);}}
        return new JSONObject().put("passed",true).put("artifact",Creation.reference(artifact)).put("dataSha256",WorldFrames.hash(d)).put("rowCount",rows.length()).put("cells",rows.length()*8).put("points",points.length()).put("maxPlotError",error);
    }
}
