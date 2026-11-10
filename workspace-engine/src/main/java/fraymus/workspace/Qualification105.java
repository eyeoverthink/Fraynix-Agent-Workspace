package fraymus.workspace;

import org.json.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Owned test inputs, never expected renderer answers or observations of the physical world. */
final class Qualification105 {
    static final String KIND="summary-qualification-workflow-105-v1";
    static final int MAX_SUPERVISORS=16;
    private static final String CODE=identity();
    static boolean own(JSONObject w){return w.has("qualification105");}
    static boolean requested(String text){return ChatPolicy044.has(text,"\\bqualif(?:y|ication)\\b")&&ChatPolicy044.has(text,"\\bsummary\\b");}
    static String code(){return CODE;}
    private static String identity(){try{
        var hash=MessageDigest.getInstance("SHA-256");
        for(Class<?> type:List.of(Qualification105.class,WorkflowRunner.class))try(var in=type.getResourceAsStream(type.getSimpleName()+".class")){hash.update(in.readAllBytes());}
        hash.update(DataSummary104.code(DataSummary104.KIND).getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash.digest());
    }catch(Exception e){throw new IllegalStateException(e);}}
    static List<JSONObject> cases(){
        var out=new ArrayList<JSONObject>();
        var monotonic=new StringBuilder("t,x\n");var reversal=new StringBuilder("t,x\n");var flat=new StringBuilder("t,x\n");
        for(int i=0;i<17;i++)monotonic.append(String.format(Locale.ROOT,"%.8f,%.8f%n",i*.04,i*.002));
        for(int i=0;i<23;i++)reversal.append(String.format(Locale.ROOT,"%.8f,%.8f%n",i*.06,(i<=11?i:22-i)*.003-.01));
        for(int i=0;i<11;i++)flat.append(String.format(Locale.ROOT,"%.8f,-0.01500000%n",i*.03));
        out.add(example("monotonic",monotonic.toString(),"s","m","reject",.25,5));
        out.add(example("reversal",reversal.toString(),"s","m","reject",.25,5));
        out.add(example("flat",flat.toString(),"s","m","reject",.25,5));
        out.add(example("units-flags","t,x\n0,0\n20,1\n60,0\n100,2\n300,2\n340,-1\n380,-1\n600,3\n640,2\n","ms","cm","reject",.08,.08));
        out.add(example("skipped","t,x\n0,0\n0.04,0.002\n0.08,\n0.12,-0.001\n0.16,0\n0.20,0.004\n","s","m","skip",.25,5));
        return out;
    }
    private static JSONObject example(String name,String csv,String time,String distance,String missing,double gap,double speed){
        var input=new JSONObject().put("title","Qualification105 · "+name+" · "+code().substring(0,8)).put("filename","qualification105-"+code().substring(0,12)+"-"+name+".csv")
            .put("csv",csv.replace("\r\n","\n")).put("sourceKind","synthetic").put("sourceNote","Java-authored qualification105 software test: "+name+". No real-world measurement or training claim. Suite "+code())
            .put("mapping",new JSONObject().put("timeColumn","t").put("distanceColumn","x").put("timeUnit",time).put("distanceUnit",distance).put("timeOffsetS",0).put("distanceOrigin",0).put("direction",1)).put("missingPolicy",missing);
        return new JSONObject().put("name",name).put("input",input).put("maxGapS",gap).put("speedLimitMps",speed);
    }
    static JSONObject example(String name){return cases().stream().filter(c->c.getString("name").equals(name)).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown qualification case"));}
    static JSONObject binding(JSONObject c){return new JSONObject().put("suiteCode",code()).put("name",c.getString("name")).put("caseSha256",WorldFrames.hash(c));}
    static void inputIntact(JSONObject c,JSONObject data){
        if(data.getInt("version")!=1)throw new IllegalStateException("Original qualification dataset was revised");
        var input=c.getJSONObject("input");for(String key:input.keySet())if(!new JSONObject().put("v",input.get(key)).similar(new JSONObject().put("v",data.get(key))))throw new IllegalStateException("Qualification source or mapping changed: "+key);
        MotionData.originalRows(data);
    }
    static JSONObject dataset(MeasurementStore store,JSONObject c)throws Exception{
        synchronized(store){
            var matches=new ArrayList<JSONObject>();var input=c.getJSONObject("input");
            for(Object value:store.datasets()){var head=(JSONObject)value;if(head.getString("title").equals(input.getString("title")))matches.add(store.read(head.getString("id"),0));}
            if(matches.size()>1)throw new IllegalStateException("Ambiguous qualification input; preserve originals and review");
            if(!matches.isEmpty()){inputIntact(c,matches.getFirst());return matches.getFirst();}
            var data=store.importCsv(WorldFrames.copy(input));inputIntact(c,data);return data;
        }
    }
    static JSONObject summary(JSONObject w){
        var q=WorldFrames.copy(w.getJSONObject("qualification105"));int checked=0,reused=0,created=0;
        for(Object raw:w.getJSONArray("stages")){var s=(JSONObject)raw;if(s.getString("name").equals("selected-data"))continue;if(s.getString("status").equals("completed"))checked++;if(s.optBoolean("reused"))reused++;else if(s.has("workflowId"))created++;}
        return q.put("checked",checked).put("total",5).put("reusedExamples",reused).put("createdExamples",created);
    }
}
