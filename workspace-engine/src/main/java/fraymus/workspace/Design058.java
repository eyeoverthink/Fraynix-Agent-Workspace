package fraymus.workspace;

import org.json.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Design-only choice, followed by decision-bound fresh and paired reserved evidence. */
final class Design058 {
    private Design058(){}
    static JSONObject protocol(){try(var in=Design058.class.getResourceAsStream("/construction058-protocol.json")){return new JSONObject(new String(in.readAllBytes(),StandardCharsets.UTF_8));}catch(Exception e){throw new IllegalStateException(e);}}
    static String code(){return Code.VALUE;}
    private static final class Code {static final String VALUE=computeCode();}
    private static String computeCode(){var d=new JSONObject().put("protocol",protocol()).put("payload",Payload057.code());for(Class<?> c:new Class<?>[]{Design058.class,PayloadSeries058.class,Evidence059.class,SupportDesign059.class})try(var in=c.getResourceAsStream(c.getSimpleName()+".class")){d.put(c.getSimpleName(),MapDefinition.sha(in.readAllBytes()));}catch(Exception e){throw new IllegalStateException(e);}return WorldFrames.hash(d);}
    static JSONObject policy(boolean allow){return policy(allow,false);}
    static JSONObject policy(boolean allow,boolean asymmetric){return new JSONObject().put("version",asymmetric?"asymmetric-payload-059-v1":"failure-redesign-058-v1").put("allowRedesign",allow).put("codeSha256",code()).put("protocol",asymmetric?SupportDesign059.protocol():protocol()).put("designProtocol",new PayloadSeries058("design",asymmetric).protocol()).put("reservedProtocol",new PayloadSeries058("reserved",asymmetric).protocol());}
    static JSONArray proposals(JSONObject source,JSONObject base){return Design056.proposals(source,base);}
    static JSONObject choose(JSONObject study){for(Object raw:study.getJSONArray("candidates")){var c=(JSONObject)raw;if(c.getString("status").equals("completed")&&c.optJSONObject("metrics",new JSONObject()).optBoolean("passed")&&c.optJSONObject("structure053",new JSONObject()).optBoolean("passed")&&c.optJSONObject("payloadDesign058",new JSONObject()).optBoolean("qualifies"))return c;}return null;}
    static String label(JSONObject series){int n=0;for(Object raw:series.getJSONArray("results"))if(((JSONObject)raw).getJSONObject("metrics").getBoolean("stable"))n++;return n+"/"+series.getJSONArray("results").length()+" stable; repeats "+(series.getBoolean("repeatable")?"agree":"DISAGREE");}
    static boolean requested(String text){String s=text.toLowerCase(Locale.ROOT);return SupportDesign059.requested(text)||s.contains("payloaddesign058")||s.contains("unfamiliar payload")||s.contains("reserved payload")||s.contains("payload redesign")||s.contains("redesign from measured failures");}
    static boolean eligible(JSONObject data){return data.optBoolean("complete")&&data.optBoolean("repeatable")&&data.optBoolean("freshDesignPassed")&&data.optBoolean("selectedReservedPassed");}
    private static Path folder(Path root,JSONObject run){UUID.fromString(run.getString("id"));return root.resolve(run.getString("id"));}
    private static JSONObject binding(Counterfactual045 store,String id,JSONObject actual)throws Exception {
        var study=store.audit(id);var chosen=choose(study);
        if(chosen==null||!chosen.getString("id").equals(study.getJSONObject("decision").getString("selectedCandidate"))||!store.compareActual(id,actual).getBoolean("passed"))throw new IllegalStateException("Design selection or fresh native execution changed");
        return new JSONObject().put("studyId",id).put("decisionSha256",study.getString("decisionSha256")).put("decision",study.getJSONObject("decision")).put("selectedCandidate",chosen.getString("id")).put("actualRunSha256",WorldFrames.hash(actual)).put("codeSha256",code());
    }
    static JSONObject evaluate(Path root,Counterfactual045 store,String id,JSONObject actual,BooleanSupplier stopped)throws Exception {
        var bound=binding(store,id,actual);Path dir=folder(root,actual),file=dir.resolve("evaluation.json");
        var saved=Files.exists(file)?AgentFiles042.loadChecked(file):new JSONObject().put("binding",bound).put("decisionPinnedAt",Instant.now()).put("stages",new JSONArray());
        if(!saved.getJSONObject("binding").similar(bound))throw new IllegalStateException("Reserved evaluation cannot change its pinned decision");
        AgentFiles042.saveChecked(file,saved);
        boolean asymmetric=SupportDesign059.enabled(store.read(id));var baseline=store.run(id,"maximum-force");
        for(String phase:List.of("fresh-design","baseline-reserved","selected-reserved")) {
            if(stopped.getAsBoolean()||Thread.currentThread().isInterrupted())throw new InterruptedException("Redesign evaluation stopped between stages");
            var series=new PayloadSeries058(phase.equals("fresh-design")?"design":"reserved",asymmetric);var run=phase.equals("baseline-reserved")?baseline:actual;
            boolean present=false;for(Object raw:saved.getJSONArray("stages")){var stage=(JSONObject)raw;if(stage.getString("phase").equals(phase)){if(!stage.getJSONObject("summary").similar(Payload057.summary(series.read(dir.resolve(phase),run))))throw new IllegalStateException("Retained evaluation stage changed");present=true;}}
            if(present)continue;
            var result=series.evaluate(dir.resolve(phase),run,stopped);
            saved.getJSONArray("stages").put(new JSONObject().put("phase",phase).put("finishedAt",Instant.now()).put("summary",Payload057.summary(result)));AgentFiles042.saveChecked(file,saved);
            if(phase.equals("fresh-design")&&!result.getBoolean("qualifies"))throw new IllegalStateException("Fresh execution failed design cases; reserved cases remain unexecuted");
        }
        return read(root,store,id,actual);
    }
    static JSONObject read(Path root,Counterfactual045 store,String id,JSONObject actual)throws Exception {
        var bound=binding(store,id,actual);Path dir=folder(root,actual);var saved=AgentFiles042.loadChecked(dir.resolve("evaluation.json"));
        if(!saved.getJSONObject("binding").similar(bound)||saved.getJSONArray("stages").length()!=3)throw new IllegalStateException("Evaluation binding or stage coverage changed");
        var all=new JSONArray();var groups=new JSONArray();long ticks=0;double wall=0;int attempts=0;boolean repeatable=true,fresh=false,selected=false;
        var study=store.audit(id);boolean asymmetric=SupportDesign059.enabled(study);
        for(Object raw:study.getJSONArray("candidates")){var c=(JSONObject)raw;if(!c.has("payloadDesign058"))continue;var data=store.payloadSeries058(id,c.getString("id"));String phase="design-"+c.getString("id");append(all,data,phase);groups.put(new JSONObject().put("phase",phase).put("summary",Payload057.summary(data)));ticks+=data.getLong("totalTicks");wall+=data.getDouble("observedWallMillis");attempts+=data.getInt("attempts");repeatable&=data.getBoolean("repeatable");}
        var forecastCosts=store.payloadCosts058(id);ticks=forecastCosts.getLong("ticks");wall=forecastCosts.getDouble("wallMillis");attempts=forecastCosts.getInt("attempts");
        int index=0;
        for(String phase:List.of("fresh-design","baseline-reserved","selected-reserved")) {
            var series=new PayloadSeries058(phase.equals("fresh-design")?"design":"reserved",asymmetric);var run=phase.equals("baseline-reserved")?store.run(id,"maximum-force"):actual;
            var data=series.read(dir.resolve(phase),run);var stage=saved.getJSONArray("stages").getJSONObject(index++);
            if(!stage.getString("phase").equals(phase)||!stage.getJSONObject("summary").similar(Payload057.summary(data)))throw new IllegalStateException("Evaluation chronology or original measurements changed");
            append(all,data,phase);groups.put(new JSONObject().put("phase",phase).put("summary",Payload057.summary(data)));ticks+=data.getLong("totalTicks");wall+=data.getDouble("observedWallMillis");attempts+=data.getInt("attempts");repeatable&=data.getBoolean("repeatable");if(phase.equals("fresh-design"))fresh=data.getBoolean("qualifies");if(phase.equals("selected-reserved"))selected=data.getBoolean("qualifies");
        }
        long assembly=store.costs054(id).getLong("totalTicks")+WorldVerify.verify(actual).getInt("nativeTicks");
        return new JSONObject().put("version",asymmetric?"asymmetric-payload-059-v1":"failure-redesign-058-v1").put("scope",(asymmetric?SupportDesign059.protocol():protocol()).getString("scope")).put("binding",bound).put("decisionPinnedAt",saved.getString("decisionPinnedAt")).put("complete",true).put("repeatable",repeatable).put("freshDesignPassed",fresh).put("selectedReservedPassed",selected).put("promotionAllowed",fresh&&selected&&repeatable).put("groups",groups).put("results",all).put("attempts",attempts).put("totalTicks",ticks).put("assemblyTicks",assembly).put("completeTaskTicks",ticks+assembly).put("observedWallMillis",wall);
    }
    private static void append(JSONArray all,JSONObject data,String phase){for(Object raw:data.getJSONArray("results")){var r=WorldFrames.copy((JSONObject)raw);r.put("caseId",phase+"/"+r.getString("caseId")).put("phase",phase).put("sourceRunSha256",data.getString("sourceRunSha256"));all.put(r);}}
    static JSONObject originals(Path root,Counterfactual045 store,String id,JSONObject actual)throws Exception {
        var groups=store.payloadOriginals058(id).getJSONArray("groups");boolean asymmetric=SupportDesign059.enabled(store.read(id));Path dir=folder(root,actual);
        for(String phase:List.of("fresh-design","baseline-reserved","selected-reserved")){var series=new PayloadSeries058(phase.equals("fresh-design")?"design":"reserved",asymmetric);var run=phase.equals("baseline-reserved")?store.run(id,"maximum-force"):actual;if(series.exists(dir.resolve(phase),run))groups.put(new JSONObject().put("phase",phase).put("original",series.originals(dir.resolve(phase),run)));}
        var manifests=new JSONArray();var records=new JSONArray();var headers=new JSONArray();int i=0;
        for(Object raw:groups){var group=(JSONObject)raw;var original=group.getJSONObject("original");var ids=new JSONArray();for(Object item:original.getJSONArray("records")){var r=(JSONObject)item;var packed=r.getJSONObject("original");String name="payload-attempt-"+(++i)+".json";ids.put(name);manifests.put(new JSONObject().put("file",name).put("phase",group.getString("phase")).put("attemptId",r.getString("attemptId")).put("rawBytes",packed.getInt("rawBytes")).put("rawSha256",packed.getString("rawSha256")));records.put(new JSONObject().put("file",name).put("original",packed));}headers.put(new JSONObject().put("phase",group.getString("phase")).put("header",original.getJSONObject("header")).put("originalCanonicalSha256",original.getString("originalCanonicalSha256")).put("files",ids));}
        var manifest=new JSONObject().put("format","fraynix.payload-attempt-manifest-057-v1").put("edition",asymmetric?59:58).put("studyId",id).put("groups",headers).put("attempts",manifests);manifest.put("originalCanonicalSha256",Payload057.evidenceHash(manifest));
        return new JSONObject().put("manifest",manifest).put("records",records);
    }
}
