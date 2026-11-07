package fraymus.workspace;

import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.json.*;

/** A bounded spending decision. Summary proposals never replace original outcome verification. */
final class CalibrationAdmission086 {
    static final String VERSION="calibration-admission-086-v1";
    interface Fitter { JSONObject fit(JSONArray pairs) throws Exception; }
    private final Path root, journal;
    private final Object owner;
    private final Fitter fitter;
    CalibrationAdmission086(Path project,Object owner,Fitter fitter) {
        root=project.resolve("world-procedures-079");journal=project.resolve("calibration-admission-086/history.json");
        this.owner=owner;this.fitter=fitter;
    }
    static JSONObject defaults(){return new JSONObject().put("horizon",6).put("fitBudgetMillis",120000)
        .put("checkBudgetMillis",3000).put("additionalRuns",0);}
    private static int integer(JSONObject b,String key,int min,int max) {
        double v=b.getDouble(key);if(!Double.isFinite(v)||v!=Math.rint(v)||v<min||v>max)throw new IllegalArgumentException("Invalid "+key);return (int)v;
    }
    static JSONObject assess(JSONArray pairs,JSONObject budget) {
        SpatialScene.keys(budget,"horizon","fitBudgetMillis","checkBudgetMillis","additionalRuns");
        int horizon=integer(budget,"horizon",1,256),additional=integer(budget,"additionalRuns",0,16);
        double fit=budget.getDouble("fitBudgetMillis"),check=budget.getDouble("checkBudgetMillis");
        if(!Double.isFinite(fit)||!Double.isFinite(check)||fit<=0||check<=0||fit>3_600_000||check>600_000)throw new IllegalArgumentException("Finite positive bounded cost allowances required");
        var out=new JSONObject().put("version",VERSION).put("supported",false).put("admit",false).put("budget",WorldFrames.copy(budget))
            .put("reason","Select two distinct tasks with three independent comparison pairs each")
            .put("scope","Unverified summary proposals and explicit planning allowances; admission only. Original evidence is required before a model is saved or used. No probability or future payoff guarantee.");
        if(pairs.length()<6||pairs.length()>8)return out;
        var seen=new HashSet<String>();var groups=new TreeMap<String,List<JSONObject>>();JSONObject identity=null;
        double sunk=0,maxTask=0;
        for(Object raw:pairs){var p=(JSONObject)raw;
            if(!seen.add(p.getString("simpleId"))||!seen.add(p.getString("memoryId")))return out.put("reason","Each original goal may occur only once");
            var key=new JSONObject();for(String k:List.of("receiptId","family","warm","workload"))key.put(k,p.get(k));
            if(identity==null)identity=key;else if(!identity.similar(key))return out.put("reason","One procedure, proof state and saving workload required");
            if(p.getString("family").equals("direct"))return out.put("reason","Direct control needs no remembered ordering");
            double a=p.getDouble("simpleMillis"),b=p.getDouble("memoryMillis");
            if(!Double.isFinite(a)||!Double.isFinite(b)||a<=0||b<=0)throw new IllegalArgumentException("Invalid comparison duration");
            sunk+=a+b;maxTask=Math.max(maxTask,Math.max(a,b));
            groups.computeIfAbsent(p.getString("taskSha256"),k->new ArrayList<>()).add(p);
        }
        if(groups.size()<2||groups.values().stream().anyMatch(g->g.size()<3))return out;
        // Local cohorts must describe comparable geometry; no extrapolation to unrelated work.
        for(Object a:pairs)for(Object b:pairs)if(Procedure078.distance(((JSONObject)a).getJSONObject("spec"),((JSONObject)b).getJSONObject("spec"))>.10)
            return out.put("reason","Comparison tasks are too distant from one another");
        double minimum=Double.POSITIVE_INFINITY,spread=500;
        for(var group:groups.values()){
            double sum=0,amin=Double.POSITIVE_INFINITY,amax=0,bmin=Double.POSITIVE_INFINITY,bmax=0;
            for(var p:group){double a=p.getDouble("simpleMillis"),b=p.getDouble("memoryMillis");sum+=a-b;amin=Math.min(amin,a);amax=Math.max(amax,a);bmin=Math.min(bmin,b);bmax=Math.max(bmax,b);}
            minimum=Math.min(minimum,sum/group.size());spread=Math.max(spread,amax-amin+bmax-bmin);
        }
        double lower=minimum-spread,perTask=lower-check,investment=fit+additional*maxTask,benefit=horizon*perTask;
        boolean admit=perTask>0&&benefit>investment*1.25;
        return out.put("supported",true).put("admit",admit).put("identity",identity).put("distinctTasks",groups.size())
            .put("historicalComparisonMillis",sunk).put("minimumGroupSavingMillis",minimum).put("variabilityAllowanceMillis",spread)
            .put("lowerSavingMillis",lower).put("netPerTaskMillis",perTask).put("futureInvestmentMillis",investment)
            .put("forecastBenefitMillis",benefit).put("requiredBenefitMillis",investment*1.25).put("forecastNetMillis",benefit-investment)
            .put("reason",admit?"Projected benefit clears the cost allowance and 25% reserve; original evidence must still pass":"Calibration is not justified for this horizon and cost allowance; use simple control")
            .put("assumption","Future tasks resemble this cohort. Budgets are planning assumptions, not deadlines. Historical work is already spent; future bank changes and drift can invalidate the forecast.");
    }
    private Path costPath(String id){if(!UUID.fromString(id).toString().equals(id))throw new IllegalArgumentException("Canonical goal ID required");return root.resolve(id).resolve("cost080.json");}
    private JSONObject cost(String id,String code)throws Exception {
        var c=AgentFiles042.loadChecked(costPath(id));
        if(!c.getString("id").equals(id)||!c.getString("code").equals(code)||!c.getJSONObject("profile").similar(TaskCost080.profile()))
            throw new IllegalStateException("Cost proposal belongs to a different goal, implementation or runtime");
        return c;
    }
    synchronized JSONObject preview(JSONObject request)throws Exception {
        long began=System.nanoTime();SpatialScene.keys(request,"pairs","budget");var selected=request.getJSONArray("pairs");
        if(selected.length()>8)throw new IllegalArgumentException("At most eight comparison pairs");
        var pairs=new JSONArray();var inputs=new JSONArray();long bytes=0;var seen=new HashSet<String>();String code=WorldProcedure079.code();
        for(Object raw:selected){var p=(JSONObject)raw;SpatialScene.keys(p,"simpleId","memoryId");var costs=new ArrayList<JSONObject>();
            for(String key:List.of("simpleId","memoryId")){String id=p.getString(key);if(!seen.add(id))throw new IllegalArgumentException("Duplicate original goal");var c=cost(id,code);costs.add(c);long n=Files.size(costPath(id));bytes+=n;inputs.put(new JSONObject().put("id",id).put("sha256",WorldFrames.hash(c)).put("bytes",n));}
            pairs.put(TaskCost080.pair(costs.get(0),costs.get(1)));
        }
        var out=assess(pairs,request.optJSONObject("budget",defaults()));
        return out.put("inputs",inputs).put("selected",WorldFrames.copy(request)).put("proposalSha256",WorldFrames.hash(new JSONObject().put("pairs",pairs)))
            .put("summaryBytes",bytes).put("originalTraceBytes",0).put("preflightMillis",(System.nanoTime()-began)/1e6);
    }
    synchronized JSONObject review()throws Exception {
        var costs=new ArrayList<JSONObject>();var skipped=new JSONArray();String code=WorldProcedure079.code();
        if(Files.isDirectory(root))try(var dirs=Files.list(root)){
            var paths=dirs.filter(p->Files.isRegularFile(p.resolve("cost080.json"))).sorted().limit(65).toList();
            if(paths.size()>64)throw new IllegalStateException("Bounded review supports at most64 completed goals");
            for(var p:paths)try{costs.add(cost(p.getFileName().toString(),code));}catch(Exception e){skipped.put(new JSONObject().put("id",p.getFileName().toString()).put("reason",e.getMessage()));}
        }
        var pairs=new JSONArray();for(var a:costs)for(var b:costs)if(a.optString("mode").equals("simple")&&b.optString("mode").equals("memory"))try{pairs.put(TaskCost080.pair(a,b));}catch(IllegalArgumentException incompatible){}
        var entries=history().getJSONArray("entries");
        return new JSONObject().put("lastDecision",entries.isEmpty()?new JSONObject():entries.getJSONObject(entries.length()-1)).put("pairs",pairs).put("calibration",new JSONObject()).put("skipped",skipped).put("defaults",defaults())
            .put("scope","Cheap proposal review only; existing models and native evidence have not been loaded.");
    }
    synchronized JSONObject history()throws Exception{return Files.exists(journal)?AgentFiles042.loadChecked(journal):new JSONObject().put("entries",new JSONArray());}
    private void record(JSONObject receipt)throws Exception {
        var entries=history().getJSONArray("entries");var next=new JSONArray();
        for(Object raw:entries)if(!((JSONObject)raw).getString("id").equals(receipt.getString("id")))next.put(raw);
        if(next.length()>=64)next.remove(0);next.put(receipt);AgentFiles042.saveChecked(journal,new JSONObject().put("entries",next));
    }
    JSONObject fit(JSONObject request)throws Exception {
        // Same owner lock as original goal admission/fitting. A new goal cannot interleave here.
        synchronized(owner){synchronized(this){
            var admission=preview(request);var receipt=new JSONObject().put("id",UUID.randomUUID().toString()).put("at",Instant.now().toString())
                .put("admission",admission).put("status",admission.getBoolean("admit")?"verifying":"declined").put("fitted",false);
            record(receipt);if(!admission.getBoolean("admit"))return receipt;
            long began=System.nanoTime();
            try{var model=fitter.fit(request.getJSONArray("pairs"));receipt.put("status","fitted").put("fitted",true).put("modelId",model.getString("id"));
                receipt.put("fitMillis",(System.nanoTime()-began)/1e6).put("fitBudgetExceeded",receipt.getDouble("fitMillis")>admission.getJSONObject("budget").getDouble("fitBudgetMillis"));record(receipt);return receipt;
            }catch(Exception e){receipt.put("status","verification-refused").put("error",String.valueOf(e.getMessage())).put("fitMillis",(System.nanoTime()-began)/1e6);record(receipt);throw e;}
        }}
    }
}
