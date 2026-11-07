package fraymus.workspace;

import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.json.*;

/** Learns spending allowances from owned timings; never supplies motion or proof authority. */
final class CalibrationCosts087 {
    static final String VERSION="calibration-costs-087-v1";
    private final Path project, root, journal;
    private final GoalRunner goals;
    private final CalibrationAdmission086 admission;
    private final String boot=UUID.randomUUID().toString();
    private String recordingWarning="";
    private final Map<String,JSONObject> pending=new HashMap<>();
    interface Operation { JSONObject run() throws Exception; }
    CalibrationCosts087(Path project,GoalRunner goals) {
        this.project=project;this.goals=goals;root=project.resolve("world-procedures-079");journal=project.resolve("calibration-costs-087/history.json");
        admission=new CalibrationAdmission086(project,goals,this::measuredFit);
    }
    synchronized JSONObject history()throws Exception {
        var h=Files.exists(journal)?AgentFiles042.loadChecked(journal):new JSONObject().put("version",VERSION).put("samples",new JSONArray());
        if(!VERSION.equals(h.getString("version"))||h.getJSONArray("samples").length()>64)throw new IllegalStateException("Unknown or oversized cost journal");return h;
    }
    private void record(JSONObject sample)throws Exception {
        var h=history();var all=h.getJSONArray("samples");for(Object raw:all)if(((JSONObject)raw).getString("id").equals(sample.getString("id")))return;
        if(all.length()==64)all.remove(0);all.put(sample.put("at",Instant.now().toString()).put("boot",boot));AgentFiles042.saveChecked(journal,h);
    }
    private static String id(String s){if(!UUID.fromString(s).toString().equals(s))throw new IllegalArgumentException("Canonical source ID required");return s;}
    private JSONObject model(String id)throws Exception{return AgentFiles042.loadChecked(root.resolve("cost-models080").resolve(id(id)+".json"));}
    static JSONObject modelShape(JSONObject m) {
        var pairs=m.getJSONArray("pairs");if(pairs.isEmpty())throw new IllegalArgumentException("Missing pairs");var first=pairs.getJSONObject(0);long bytes=0;
        for(String k:m.getJSONObject("evidence").keySet())for(Object raw:m.getJSONObject("evidence").getJSONArray(k))bytes=Math.addExact(bytes,((JSONObject)raw).getLong("bytes"));
        return new JSONObject().put("code",m.getString("code")).put("profile",m.getJSONObject("profile")).put("pairs",pairs.length()).put("bytes",bytes)
            .put("family",first.getString("family")).put("warm",first.getBoolean("warm")).put("workload",first.getJSONObject("workload"));
    }
    private JSONObject proposalShape(JSONObject q,JSONObject preview)throws Exception {
        long bytes=0;for(Object raw:q.getJSONArray("pairs"))for(String key:List.of("simpleId","memoryId")){
            var dir=root.resolve(id(((JSONObject)raw).getString(key)));var j=AgentFiles042.loadChecked(dir.resolve("goal.json"));
            bytes=Math.addExact(bytes,Files.size(dir.resolve("goal.json"))+Files.size(dir.resolve("cost080.json")));
            var attempts=j.getJSONObject("arm").getJSONArray("attempts");if(attempts.length()>64)throw new IllegalStateException("Too many original attempts");
            for(Object a:attempts){var attempt=(JSONObject)a;if(attempt.has("sha256"))bytes=Math.addExact(bytes,Files.size(dir.resolve("attempts").resolve(id(attempt.getString("id"))).resolve("original.json")));}
        }
        var identity=preview.getJSONObject("identity");return new JSONObject().put("code",WorldProcedure079.code()).put("profile",TaskCost080.profile())
            .put("pairs",q.getJSONArray("pairs").length()).put("bytes",bytes).put("family",identity.getString("family")).put("warm",identity.getBoolean("warm")).put("workload",identity.getJSONObject("workload"));
    }
    JSONObject measuredFit(JSONArray pairs)throws Exception {
        synchronized(goals){synchronized(this){long began=System.nanoTime();var m=goals.fitCosts080(pairs);double elapsed=(System.nanoTime()-began)/1e6;
            try{record(new JSONObject().put("id","fit:"+m.getString("id")).put("kind","fit").put("millis",elapsed).put("shape",modelShape(m))
                .put("modelId",m.getString("id")).put("modelSha256",WorldFrames.hash(m)));}catch(Exception recordingFailure){recordingWarning="Verified model saved, but allowance observation was not saved: "+recordingFailure.getMessage();System.err.println(recordingWarning);}
            return m;
        }}
    }
    JSONObject start(Operation op)throws Exception {
        synchronized(goals){synchronized(this){var before=goals.calibrationStats081();var g=op.run();
            if(g.optString("procedure").equals(Procedure079.KIND)&&g.optString("memoryMode079").equals("calibrated")){
                // Only starts passing this wrapper own a before/after interval in this boot.
                pending.clear();pending.put(g.getString("id"),new JSONObject().put("before",before).put("began",System.nanoTime()));
            }return g;
        }}
    }
    synchronized void invalidate(){pending.clear();}
    JSONObject read(String id)throws Exception {
        synchronized(goals){synchronized(this){var g=goals.read(id);var p=pending.get(id);
            if(p!=null&&Set.of("completed","cancelled","interrupted").contains(g.getString("status"))){
                if(g.getString("status").equals("completed")&&g.has("taskCost080")){
                    var c=g.getJSONObject("taskCost080");var after=goals.calibrationStats081();var delta=delta(p.getJSONObject("before"),after);
                    if(c.getBoolean("eligible")&&g.optInt("resumes")==0&&delta.optBoolean("eligible")){
                        var job=AgentFiles042.loadChecked(root.resolve(id(id)).resolve("goal.json"));
                        var saved=AgentFiles042.loadChecked(root.resolve(id).resolve("cost080.json"));
                        if(!saved.similar(c)||!c.getString("jobSha256").equals(WorldFrames.hash(job)))throw new IllegalStateException("Completed timing source changed");
                        var m=model(job.getJSONObject("calibration080").getString("id"));var shape=modelShape(m);
                        if(c.getBoolean("warm")==shape.getBoolean("warm")&&c.getJSONObject("workload").similar(shape.getJSONObject("workload")))
                            record(new JSONObject().put("id","check:"+id).put("kind","check").put("millis",delta.getDouble("millis")).put("shape",shape)
                                .put("goalId",id).put("costSha256",WorldFrames.hash(c)).put("modelId",m.getString("id")).put("modelSha256",WorldFrames.hash(m))
                                .put("before",p.getJSONObject("before")).put("after",after).put("loads",delta.getLong("loads")).put("verifiedBytes",delta.getLong("verifiedBytes"))
                                .put("intervalMillis",(System.nanoTime()-p.getLong("began"))/1e6));
                    }
                }pending.remove(id);
            }return GoalRunner.view(g);
        }}
    }
    static JSONObject delta(JSONObject before,JSONObject after) {
        long loads=after.getLong("loads")-before.getLong("loads"),failures=after.getLong("failures")-before.getLong("failures"),bytes=after.getLong("verifiedBytes")-before.getLong("verifiedBytes");
        double millis=after.getDouble("loadMillis")-before.getDouble("loadMillis");
        boolean eligible=before.getString("mode").equals(after.getString("mode"))&&loads>0&&failures==0&&bytes>0&&Double.isFinite(millis)&&millis>0;
        return new JSONObject().put("eligible",eligible).put("loads",loads).put("verifiedBytes",bytes).put("millis",millis);
    }
    private boolean sourceUnchanged(JSONObject s)throws Exception {
        var m=model(s.getString("modelId"));if(!WorldFrames.hash(m).equals(s.getString("modelSha256"))||!modelShape(m).similar(s.getJSONObject("shape")))return false;
        return !s.getString("kind").equals("check")||WorldFrames.hash(AgentFiles042.loadChecked(root.resolve(id(s.getString("goalId"))).resolve("cost080.json"))).equals(s.getString("costSha256"));
    }
    static JSONObject estimate(JSONArray samples,JSONObject target,Instant now) {
        var used=new JSONArray();var values=new HashMap<String,List<Double>>();values.put("fit",new ArrayList<>());values.put("check",new ArrayList<>());
        var seen=new HashSet<String>();
        for(int i=samples.length()-1;i>=0;i--){var s=samples.getJSONObject(i);try{
            String kind=s.getString("kind");if(!values.containsKey(kind)||values.get(kind).size()>=12||!seen.add(s.getString("id")))continue;
            var at=Instant.parse(s.getString("at"));if(at.isAfter(now)||at.isBefore(now.minus(Duration.ofDays(14))))continue;
            var shape=s.getJSONObject("shape");boolean matches=true;for(String k:List.of("code","profile","pairs","family","warm","workload"))if(!new JSONObject().put("v",shape.get(k)).similar(new JSONObject().put("v",target.get(k))))matches=false;
            double original=shape.getDouble("bytes"),bytes=target.getDouble("bytes"),ratio=bytes/original,cost=s.getDouble("millis");
            if(!matches||original<=0||bytes<=0||!Double.isFinite(ratio)||ratio<.8||ratio>1.25||!Double.isFinite(cost)||cost<=0)continue;
            double scaled=cost*Math.max(1,ratio);values.get(kind).add(scaled);used.put(new JSONObject().put("id",s.getString("id")).put("kind",kind).put("observedMillis",cost).put("scaledMillis",scaled));
        }catch(RuntimeException invalid){/* Invalid observations are excluded, never made into cheap timings. */}}
        var out=new JSONObject().put("supported",values.get("fit").size()>=3&&values.get("check").size()>=3).put("fitSamples",values.get("fit").size()).put("checkSamples",values.get("check").size()).put("used",used).put("shape",target)
            .put("reason","Need three compatible successful fits and three uninterrupted completed calibrated tasks within14days")
            .put("scope","Empirical spending reserve, not a probability or execution authority. Upward byte scaling only; nearby evidence volume and matching workload required.");
        if(out.getBoolean("supported")){
            for(String kind:List.of("fit","check")){var v=values.get(kind);double max=Collections.max(v),min=Collections.min(v),allowance=Math.max(kind.equals("fit")?1000:10,max+Math.max(max-min,.1*max));out.put(kind+"BudgetMillis",allowance);}
            if(out.getDouble("fitBudgetMillis")>3_600_000||out.getDouble("checkBudgetMillis")>600_000)return out.put("supported",false).put("reason","Measured allowance exceeds supported bounds; explicit review required");
            out.put("reason","Using measured maxima plus observed spread or10% reserve, whichever is larger");
        }return out;
    }
    private JSONObject prepared(JSONObject request)throws Exception {
        SpatialScene.keys(request,"pairs","budget","allowanceMode");String mode=request.has("allowanceMode")?request.getString("allowanceMode"):"measured";
        if(!Set.of("manual","measured").contains(mode))throw new IllegalArgumentException("Unknown allowance mode");
        var q=new JSONObject().put("pairs",request.getJSONArray("pairs")).put("budget",request.has("budget")?WorldFrames.copy(request.getJSONObject("budget")):CalibrationAdmission086.defaults());
        long began=System.nanoTime();var preview=admission.preview(q);var learned=new JSONObject().put("supported",false).put("mode",mode).put("reason",mode.equals("manual")?"Explicit manual allowances":"Comparison cohort is not supported");
        if(mode.equals("measured")&&preview.getBoolean("supported")){
            var valid=new JSONArray();var excluded=new JSONArray();for(Object raw:history().getJSONArray("samples")){var s=(JSONObject)raw;try{if(sourceUnchanged(s))valid.put(s);else excluded.put(s.getString("id"));}catch(Exception changed){excluded.put(s.optString("id","invalid"));}}
            learned=estimate(valid,proposalShape(q,preview),Instant.now()).put("mode",mode).put("excludedSources",excluded);
            if(learned.getBoolean("supported")){var b=q.getJSONObject("budget");b.put("fitBudgetMillis",learned.getDouble("fitBudgetMillis")).put("checkBudgetMillis",learned.getDouble("checkBudgetMillis"));preview=admission.preview(q);}
        }
        preview.put("allowances087",learned).put("preflightMillis",(System.nanoTime()-began)/1e6);return new JSONObject().put("request",q).put("preview",preview);
    }
    JSONObject preview(JSONObject request)throws Exception {synchronized(goals){synchronized(this){return prepared(request).getJSONObject("preview");}}}
    JSONObject fit(JSONObject request)throws Exception {
        synchronized(goals){synchronized(this){var p=prepared(request);var result=admission.fit(p.getJSONObject("request"));
            result.put("allowances087",p.getJSONObject("preview").getJSONObject("allowances087"));result.getJSONObject("admission").put("allowances087",result.getJSONObject("allowances087"));AgentFiles042.saveChecked(project.resolve("calibration-costs-087/last-decision.json"),result);return result;
        }}
    }
    JSONObject review()throws Exception {synchronized(goals){synchronized(this){return admission.review().put("allowanceSamples087",history().getJSONArray("samples").length()).put("allowanceMode087","measured").put("recordingWarning087",recordingWarning);}}}
    JSONObject decisions()throws Exception{return admission.history();}
}
