package fraymus.workspace;

import org.json.*;
import java.util.*;

/** Registered workflow contract. The model supplies content, never this verifier. */
final class DataCreation101 {
    static final String KIND="data-creation-workflow-101-v1";
    static final String SCOPE="Recorded or declared data; quality flags retained. Not a new physical measurement.";
    static final List<String> STAGES=List.of("data","creation");
    static boolean requested(String text){
        return ChatPolicy044.has(text,"\\bdata[- ]to[- ]creation\\b") ||
            !ChatPolicy044.has(text,"\\b(native|physics|simulate|simulation|predict)\\b")&&ChatPolicy044.has(text,"\\banaly[sz]e\\b")&&ChatPolicy044.has(text,"\\b(dataset|csv|data)\\b")&&
            ChatPolicy044.has(text,"\\b(create|build)\\b.*\\b(explorer|interactive table)\\b");
    }
    static String code(){try(var in=DataCreation101.class.getResourceAsStream("DataCreation101.class")){return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(in.readAllBytes()));}catch(Exception e){throw new IllegalStateException(e);}}
    static JSONObject definition(JSONObject in,MeasurementStore store){
        SpatialScene.keys(in,"profile101","request","title","modelName","datasetId","datasetVersion","maxGapS","speedLimitMps","saveSkillName","reviewAfterData101");
        if(!KIND.equals(in.getString("profile101")))throw new IllegalArgumentException("Unknown data-creation contract");
        if(!(in.get("datasetVersion") instanceof Number n)||n.doubleValue()!=n.intValue()||n.intValue()<1)throw new IllegalArgumentException("Explicit integer dataset version required");
        for(String k:List.of("maxGapS","speedLimitMps"))if(in.has(k)&&!(in.get(k) instanceof Number))throw new IllegalArgumentException("Numeric data criteria required");
        if(in.has("reviewAfterData101")&&!(in.get("reviewAfterData101") instanceof Boolean))throw new IllegalArgumentException("Review-after-data must be boolean");
        var data=store.read(in.getString("datasetId"),in.getInt("datasetVersion"));
        if(MotionData.originalRows(data).length()>1000||data.getString("csv").length()>100_000)throw new IllegalArgumentException("Data creation accepts at most1000 rows and100K CSV characters; no subsampling");
        double gap=in.optDouble("maxGapS",.25),speed=in.optDouble("speedLimitMps",5);MotionData.criteria(gap,speed);
        String name=in.optString("saveSkillName","");if(!name.isEmpty()&&!name.matches("[a-z][a-z0-9-]{2,55}"))throw new IllegalArgumentException("Skill name requires3..56 lowercase letters, digits or hyphens");
        return new JSONObject().put("profile101",KIND).put("request",Creation.text(in,"request",4000)).put("title",Creation.text(in,"title",140)).put("modelName",Creation.text(in,"modelName",120))
            .put("datasetId",data.getString("datasetId")).put("datasetVersion",data.getInt("version")).put("maxGapS",gap).put("speedLimitMps",speed).put("saveSkillName",name).put("reviewAfterData101",in.optBoolean("reviewAfterData101"));
    }
    static String quality(JSONObject handoff){return handoff.getJSONObject("quality").getBoolean("qualityPassed")?"Within declared thresholds":"Quality flags retained";}
    static JSONObject creation(JSONObject w,JSONObject handoff){
        var d=w.getJSONObject("definition");String filter=handoff.getJSONObject("preview").getJSONArray("sample").getJSONObject(0).getString("source_row");
        var checks=new JSONArray().put(new JSONObject().put("selector","#row-count").put("sourceRows",true))
            .put(new JSONObject().put("selector","#filter").put("action","fill").put("value",filter))
            .put(new JSONObject().put("selector","#row-count").put("sourceCount",new JSONObject().put("column","source_row").put("equals",filter)))
            .put(new JSONObject().put("selector","#filter").put("action","fill").put("value","__no_match_101__"))
            .put(new JSONObject().put("selector","#row-count").put("expectText","0"))
            .put(new JSONObject().put("selector","#reset").put("action","click"))
            .put(new JSONObject().put("selector","#row-count").put("sourceRows",true))
            .put(new JSONObject().put("selector","#filter").put("expectValue",""))
            .put(new JSONObject().put("selector","#scope").put("expectText",SCOPE))
            .put(new JSONObject().put("selector","#data-quality").put("expectText",quality(handoff)));
        String request="Build a compact working motion-data explorer with HTML, CSS and JavaScript, no libraries or charts. Load inputs/source.json with fetch. Render the source title in #title. Use TABLE id=data-table with thead/tr/th and tbody/tr/td. Exact headers in order: source_row, time_s, displacement_m. Every row in source order, every original source string unchanged via textContent. Do not round or append units inside cells; explain seconds and metres in a caption. Iterate source.headers for column order. "
            +"Visible text #row-count must show only the number of displayed rows, never an input. Input #filter filters immediately by exact source_row string; empty displays all rows; unknown string displays zero rows. Button #reset clears the filter and restores all rows. Table headers remain present even when zero rows match. All cells and controls must be visible (scrolling for long tables is allowed). Render exact visible #scope text: "+SCOPE+" Render exact visible #data-quality text: "+quality(handoff)+". Use a readable responsive teal design with scrollable table and wrapping controls. Keep code concise. Java owns the fixed checks and independently compares actual cells and controls after rendering. User intent, subject to that fixed source contract: "+d.getString("request");
        var out=WorldFrames.copy(handoff.getJSONObject("source"));
        return out.put("request",request).put("title",d.getString("title")).put("modelName",d.getString("modelName")).put("checks",checks);
    }
    static JSONObject audit(JSONObject artifact,JSONObject data,JSONObject handoff,JSONObject actual) throws Exception {
        SpatialScene.keys(actual,"id","version","frames");
        if(!actual.getString("id").equals(artifact.getString("id"))||actual.getInt("version")!=artifact.getInt("version"))throw new IllegalStateException("Foreign data-creation audit");
        var expected=MotionData.originalRows(data);var frames=actual.getJSONArray("frames");if(frames.length()!=4)throw new IllegalArgumentException("Require all, exact filter, no-match and reset observations");
        var errors=new JSONArray();String filter=String.valueOf(expected.getJSONObject(0).getInt("sourceRow"));
        for(int f=0;f<4;f++){
            var frame=frames.getJSONObject(f);SpatialScene.keys(frame,"rows","headers","filter","count","scope","quality","visible");
            if(!frame.getJSONArray("headers").similar(new JSONArray(List.of("source_row","time_s","displacement_m"))))errors.put("Frame"+f+": column headers/order differ from the normalized source");
            if(!frame.getBoolean("visible"))errors.put("Frame"+f+": source table or controls are hidden");
            String wantFilter=f==1?filter:f==2?"__no_match_101__":"";
            if(!frame.getString("filter").equals(wantFilter))errors.put("Frame"+f+": filter/reset state differs");
            var rows=frame.getJSONArray("rows");int count=f==1?1:f==2?0:expected.length();
            if(rows.length()!=count||!frame.getString("count").equals(String.valueOf(count)))errors.put("Frame"+f+": actual rows/count differ");
            if(!frame.getString("scope").equals(SCOPE)||!frame.getString("quality").equals(quality(handoff)))errors.put("Frame"+f+": source scope or quality label changed");
            for(int i=0;i<Math.min(rows.length(),count);i++){
                var row=rows.getJSONArray(i);var source=expected.getJSONObject(f==1?0:i);
                if(row.length()!=3){errors.put("Frame"+f+": invalid cell count");break;}
                for(int c=0;c<3;c++){
                    String cell=row.getString(c);String key=List.of("sourceRow","timeS","distanceM").get(c);
                    if(!cell.equals(c==0?String.valueOf(source.getInt(key)):String.valueOf(source.getDouble(key)))){errors.put("Frame"+f+" row"+i+" column"+c+": original value changed");break;}
                }
                if(errors.length()>12)break;
            }
        }
        return new JSONObject().put("passed",errors.isEmpty()).put("rowCount",expected.length()).put("frames",frames).put("errors",errors).put("artifact",Creation.reference(artifact)).put("sourceSha256",data.getString("sourceSha256"));
    }
    static JSONObject procedure(JSONObject w){
        var d=WorldFrames.copy(w.getJSONObject("definition"));for(String k:List.of("datasetId","datasetVersion","saveSkillName","reviewAfterData101"))d.remove(k);
        return new JSONObject().put("kind",KIND).put("sourceWorkflowId",w.getString("id")).put("report",w.getJSONObject("report")).put("defaults",d).put("code101",w.getString("code101"))
            .put("sourceData",w.getJSONObject("dataBinding")).put("publication101","pending")
            .put("prerequisites",new JSONArray(List.of("Fresh explicit motion CSV/version","Current Java contract and completed source workflow","Admitted installed local model")))
            .put("actions",new JSONArray(List.of("Analyze original data","Verify normalized source handoff","Generate and test explorer","Independently inspect every cell and filter state","Publish whole procedure after durable completion")))
            .put("recovery",new JSONArray(List.of("Explicit resume reuses verified data and stable children","Reject stale or altered source and audit","No more than three creation drafts or three parent resumes","Pending skills unavailable until parent completion")))
            .put("scope",SCOPE+" Registered motion-data explorer workflow; no arbitrary task graph or weight training.");
    }
}
