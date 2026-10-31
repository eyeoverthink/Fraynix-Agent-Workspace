package fraymus.workspace;

import org.json.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Registered creation contract. Files and source content are data, never host tool handlers. */
final class Creation {
    static final String PROCEDURE="creation-web-v1";
    static String digest(JSONObject files) throws Exception {
        var hash=MessageDigest.getInstance("SHA-256");for(String name:new TreeSet<>(files.keySet())){hash.update(name.getBytes(StandardCharsets.UTF_8));hash.update((byte)0);hash.update(files.getString(name).getBytes(StandardCharsets.UTF_8));hash.update((byte)0);}return HexFormat.of().formatHex(hash.digest());
    }
    static String text(JSONObject input,String key,int max){String value=input.getString(key).strip();if(value.isEmpty()||value.length()>max||value.indexOf('\0')>=0)throw new IllegalArgumentException(key+" needs1.."+max+" text characters");return value;}
    static JSONObject preview(WorkspaceStore store,JSONObject input) throws Exception {
        SpatialScene.keys(input,"sourceArtifactId","sourceVersion","sourceFile");var args=new JSONObject(input.toString()).put("request","Preview selected source as untrusted data").put("title","Source preview").put("modelName","preview").put("checks",new JSONArray().put(new JSONObject().put("selector","body").put("sourceRows",true)));var d=definition(store,args);var out=new JSONObject(d.getJSONObject("sourceBinding").toString());var sample=new JSONArray();for(int i=0;i<Math.min(3,d.getJSONObject("sourceData").getJSONArray("rows").length());i++)sample.put(d.getJSONObject("sourceData").getJSONArray("rows").get(i));return out.put("sample",sample);
    }
    static JSONObject definition(WorkspaceStore store,JSONObject input) throws Exception {
        SpatialScene.keys(input,"request","title","modelName","checks","sourceArtifactId","sourceVersion","sourceFile","saveSkillName");
        var out=new JSONObject().put("request",text(input,"request",8000)).put("title",text(input,"title",140)).put("modelName",text(input,"modelName",120)).put("checkTemplates",input.has("checks")?new JSONArray(input.getJSONArray("checks").toString()):new JSONArray());
        if(input.has("sourceArtifactId")){
            var a=store.read(input.getString("sourceArtifactId"));if(a.getInt("version")!=input.getInt("sourceVersion"))throw new IllegalStateException("Selected creation source version changed");String file=input.getString("sourceFile");WorkspaceStore.validFile(file);if(!file.endsWith(".csv"))throw new IllegalArgumentException("Creation source must be a selected CSV file");String csv=a.getJSONObject("contents").getString(file);if(csv.length()>100_000)throw new IllegalArgumentException("Creation source exceeds100K characters");
            var rows=MeasurementStore.csv(csv);var headers=rows.get(0).stream().map(String::strip).toList();if(headers.stream().anyMatch(h->h.isEmpty()||h.length()>80)||new HashSet<>(headers).size()!=headers.size())throw new IllegalArgumentException("CSV headers must be unique and1..80 characters");var data=new JSONArray();for(var row:rows.subList(1,rows.size())){if(row.size()!=headers.size())throw new IllegalArgumentException("CSV column count changed");var record=new JSONObject();for(int i=0;i<headers.size();i++)record.put(headers.get(i),row.get(i));data.put(record);}
            out.put("sourceBinding",new JSONObject().put("id",a.getString("id")).put("version",a.getInt("version")).put("file",file).put("sha256",digest(a.getJSONObject("contents"))).put("fileSha256",MeasurementStore.hash(csv)).put("title",a.getString("title")).put("rowCount",data.length()).put("headers",new JSONArray(headers)));
            out.put("sourceCsv",csv).put("sourceData",new JSONObject().put("title",a.getString("title")).put("headers",new JSONArray(headers)).put("rows",data).put("rowCount",data.length()).put("sourceSha256",MeasurementStore.hash(csv)));
        }else if(input.has("sourceVersion")||input.has("sourceFile"))throw new IllegalArgumentException("Choose a complete creation source binding");
        if(input.has("checks"))out.put("testSteps",resolveChecks(out,out.getJSONArray("checkTemplates")));return out;
    }
    static JSONArray resolveChecks(JSONObject g,JSONArray templates){
        if(templates.isEmpty()||templates.length()>24||templates.toString().length()>6000)throw new IllegalArgumentException("Declare1..24 behavior steps within6K characters");var out=new JSONArray();int assertions=0;
        for(Object raw:templates){var step=new JSONObject(((JSONObject)raw).toString());SpatialScene.keys(step,"selector","action","value","expectText","expectValue","expectCount","sourceRows","sourceCount");text(step,"selector",200);
            if(step.has("action")&&!Set.of("click","fill").contains(step.getString("action")))throw new IllegalArgumentException("Only click/fill behavior actions are registered");if(step.has("value")&&step.getString("value").length()>4096)throw new IllegalArgumentException("Behavior input exceeds budget");
            if(step.has("sourceRows")||step.has("sourceCount")){
                if(!g.has("sourceData")||step.has("expectText")||step.has("expectValue")||step.has("expectCount")||step.has("sourceRows")&&step.has("sourceCount"))throw new IllegalArgumentException("Source-count assertion needs one selected source and no competing expectation");
                int count=0;var data=g.getJSONObject("sourceData").getJSONArray("rows");
                if(step.has("sourceRows")){if(!step.getBoolean("sourceRows"))throw new IllegalArgumentException("sourceRows must be true");count=data.length();step.remove("sourceRows");}
                else{var filter=step.getJSONObject("sourceCount");SpatialScene.keys(filter,"column","equals");String column=text(filter,"column",80),value=filter.getString("equals");if(value.length()>500||!g.getJSONObject("sourceBinding").getJSONArray("headers").toList().contains(column))throw new IllegalArgumentException("Choose an actual source column");for(Object row:data)if(((JSONObject)row).getString(column).equals(value))count++;step.remove("sourceCount");}
                step.put("expectText",Integer.toString(count));
            }
            for(String key:List.of("expectText","expectValue"))if(step.has(key)&&step.getString(key).length()>500)throw new IllegalArgumentException("Bounded behavior expectation required");
            if(step.has("expectCount")){Object v=step.get("expectCount");if(!(v instanceof Number n)||n.doubleValue()!=n.intValue()||n.intValue()<0||n.intValue()>10000)throw new IllegalArgumentException("Bounded integer expected count required");}
            if(List.of("expectText","expectValue","expectCount").stream().anyMatch(step::has))assertions++;out.put(step);
        }
        if(assertions==0)throw new IllegalArgumentException("At least one observable assertion is required");return out;
    }
    static void sourceIntact(WorkspaceStore store,JSONObject g) throws Exception {
        if(!g.has("sourceBinding"))return;var b=g.getJSONObject("sourceBinding");var a=store.read(b.getString("id"));if(a.getInt("version")!=b.getInt("version")||!digest(a.getJSONObject("contents")).equals(b.getString("sha256"))||!MeasurementStore.hash(a.getJSONObject("contents").getString(b.getString("file"))).equals(b.getString("fileSha256")))throw new IllegalStateException("Frozen creation source changed; original task will not silently rebind");
    }
    static JSONObject files(JSONObject g,JSONObject generated){
        if(generated.isEmpty()||generated.length()>18||!generated.has("index.html")||generated.getString("index.html").length()<30)throw new IllegalArgumentException("Creation needs a real index.html and up to18 generated files");int chars=0;var files=new JSONObject();
        for(String name:generated.keySet()){WorkspaceStore.validFile(name);String value=generated.getString(name);if(name.startsWith("inputs/")||value.indexOf('\0')>=0||!StandardCharsets.UTF_8.newEncoder().canEncode(value))throw new IllegalArgumentException("Generated code cannot replace reserved inputs or contain invalid text");chars+=value.length();files.put(name,value);}
        if(chars>64_000)throw new IllegalArgumentException("Generated code exceeds64K character budget");
        if(g.has("sourceData")){files.put("inputs/source.csv",g.getString("sourceCsv"));files.put("inputs/source.json",g.getJSONObject("sourceData").toString());}return files;
    }
    static JSONObject reference(JSONObject artifact) throws Exception {return new JSONObject().put("id",artifact.getString("id")).put("version",artifact.getInt("version")).put("sha256",digest(artifact.getJSONObject("contents")));}
    static JSONObject artifact(WorkspaceStore store,JSONObject g) throws Exception {var ref=g.getJSONObject("report");var a=store.read(ref.getString("id"));if(a.getInt("version")!=ref.getInt("version")||!digest(a.getJSONObject("contents")).equals(ref.getString("sha256")))throw new IllegalStateException("Created artifact was changed outside its owned revision");return a;}
    static boolean receipt(JSONObject g,JSONObject artifact,boolean requirePass){
        var proof=artifact.optJSONObject("test");var steps=g.getJSONArray("testSteps");if(proof==null||!proof.getJSONArray("steps").similar(steps)||proof.getJSONArray("checks").length()<steps.length()||proof.getJSONArray("checks").length()>steps.length()+1)throw new IllegalStateException("Creation lacks its exact behavior receipt");boolean passed=true;
        for(int i=0;i<steps.length();i++){var step=steps.getJSONObject(i);var check=proof.getJSONArray("checks").getJSONObject(i);if(check.getInt("index")!=i||!check.getString("selector").equals(step.getString("selector")))throw new IllegalStateException("Creation behavior receipt was rebound");var actual=check.optJSONObject("actual");boolean ok=actual!=null&&(!step.has("expectText")||actual.optString("text").strip().equals(step.getString("expectText")))&&(!step.has("expectValue")||actual.optString("value").equals(step.getString("expectValue")))&&(!step.has("expectCount")||actual.optInt("count",-1)==step.getInt("expectCount"));if(check.getBoolean("passed")!=ok)throw new IllegalStateException("Creation assertion differs from its recorded observation");passed&=ok;}
        if(proof.getJSONArray("checks").length()>steps.length())passed=false;
        if(proof.getBoolean("passed")!=passed)throw new IllegalStateException("Creation result differs from its actual checks");if(requirePass&&!passed)throw new IllegalStateException("Declared creation behavior did not pass");return passed;
    }
    static JSONObject generation(JSONObject g){
        var out=new JSONObject().put("request",g.getString("request")).put("title",g.getString("title")).put("modelName",g.getString("modelName")).put("draft",g.getInt("draftNumber")+1).put("checks",g.optJSONArray("testSteps")==null?new JSONArray():g.getJSONArray("testSteps"));if(g.has("modelDigest"))out.put("modelDigest",g.getString("modelDigest"));
        if(g.has("sourceBinding")){var data=g.getJSONObject("sourceData");var sample=new JSONArray();for(int i=0;i<Math.min(3,data.getJSONArray("rows").length());i++)sample.put(data.getJSONArray("rows").get(i));out.put("source",new JSONObject(g.getJSONObject("sourceBinding").toString()).put("sample",sample).put("runtimeFile","inputs/source.json"));}
        if(g.has("lastFailedChecks"))out.put("failedChecks",g.getJSONArray("lastFailedChecks"));if(g.has("lastGeneratedFiles"))out.put("previousFiles",g.getJSONObject("lastGeneratedFiles"));return out;
    }
}
