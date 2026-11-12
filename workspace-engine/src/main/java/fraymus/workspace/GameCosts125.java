package fraymus.workspace;

import org.json.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.security.MessageDigest;

/** Adds measured costs and independent decision review without weakening the original verifier. */
final class GameCosts125 implements AutoCloseable {
    private final GameChoices124 tasks;
    private final TaskCost125 costs;
    private final Path reviews;
    private final String implementation;
    GameCosts125(Path repo,Path project,EmbeddedGameClient game)throws Exception {this(repo,project,game,"java-chat-cost125");}
    GameCosts125(Path repo,Path project,EmbeddedGameClient game,String context)throws Exception {
        tasks=new GameChoices124(repo,project,game,context);costs=new TaskCost125(project);reviews=project.resolve("game-reviews125");Files.createDirectories(reviews);
        var code=new JSONObject();for(Class<?> c:List.of(GameCosts125.class,TaskCost125.class,GameCostReview125.class))try(var in=c.getResourceAsStream(c.getSimpleName()+".class")){code.put(c.getSimpleName(),HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(in.readAllBytes())));}implementation=WorldFrames.hash(code);
    }
    private JSONObject attach(JSONObject task)throws Exception {
        var snapshot=costs.snapshot(task.getString("id"));
        if(snapshot!=null&&!snapshot.getString("requestHash").equals(WorldFrames.hash(new JSONObject().put("request",task.getString("request")))))throw new IllegalStateException("Cost request binding changed");
        var summary=TaskCost125.summary(snapshot);task.put("cost125",summary);if(task.has("audit"))task.getJSONObject("audit").put("cost125",WorldFrames.copy(summary));return task;
    }
    synchronized JSONObject start(JSONObject input)throws Exception {
        long began=System.nanoTime();Instant at=Instant.now();boolean existed=false;
        if(input.has("id"))for(Object raw:tasks.list().getJSONArray("tasks"))if(((JSONObject)raw).getString("id").equals(input.getString("id")))existed=true;
        var t=tasks.start(input);if(!existed)costs.admitted(t.getString("id"),t,began,at);return attach(t);
    }
    private JSONObject call(String id,String phase,Callable<JSONObject> action,boolean terminal,boolean recovered)throws Exception {
        long began=costs.begin(id,phase);JSONObject value;
        try { value=action.call(); }
        catch(Exception e){try{costs.end(id,phase,began,"refused: "+String.valueOf(e.getMessage()),false,false);}catch(Exception secondary){e.addSuppressed(secondary);}throw e;}
        costs.end(id,phase,began,"returned",terminal,recovered);return value;
    }
    synchronized JSONObject advance(String id)throws Exception {
        var t=call(id,"execution",()->tasks.advance(id),false,false);
        if(t.getString("status").equals("failed")&&GameChallenge118.active(t.getJSONObject("parameters")))measure(id);
        return attach(t);
    }
    synchronized JSONObject verify(String id)throws Exception {return attach(call(id,"verification",()->tasks.verify(id),true,false));}
    synchronized JSONObject measure(String id)throws Exception {
        var m=call(id,"measurement",()->tasks.measure(id),true,false);var snapshot=costs.snapshot(id);m.put("cost125",TaskCost125.summary(snapshot));if(snapshot!=null)m.put("timing125",snapshot);return m;
    }
    synchronized JSONObject control(String id,String action)throws Exception {return attach(call(id,action.equals("resume")?"recovery":action,()->tasks.control(id,action),false,action.equals("resume")));}
    synchronized JSONObject read(String id)throws Exception {return attach(tasks.read(id));}
    synchronized JSONObject list()throws Exception {var result=tasks.list();for(Object raw:result.getJSONArray("tasks"))attach((JSONObject)raw);return result;}
    synchronized boolean busy(){return tasks.busy();}
    private JSONObject source(String id)throws Exception {
        var m=measure(id);var c=m.getJSONObject("choice124");var timing=m.getJSONObject("timing125");
        String request=tasks.read(id).getString("request"),renderer=WorldFrames.hash(m.getJSONObject("final").getJSONObject("renderer"));boolean safe=m.getBoolean("completionCertified")&&m.getJSONObject("audit").getInt("aiDamage")==0;
        if(!timing.getString("requestHash").equals(WorldFrames.hash(new JSONObject().put("request",request)))||!c.getString("resolved").equals(request)||!c.getJSONObject("parameters").similar(m.getJSONObject("parameters")))throw new IllegalStateException("Timing or choice request binding changed");
        if(c.has("fact")) {
            var fact=c.getJSONObject("fact");
            if(!fact.getString("id").equals(id)||!fact.getString("strategy").equals(m.getString("strategy"))||fact.getInt("pickups")!=m.getJSONObject("audit").getInt("aiPickups")||fact.getBoolean("safe")!=safe||!fact.getString("renderer").equals(renderer)||fact.getDouble("seconds")!=c.getDouble("seconds")||!c.getString("trace").equals(m.getJSONObject("audit").getString("traceSha256")))throw new IllegalStateException("Original choice facts differ from the verified native outcome");
        }
        return new JSONObject().put("id",id).put("choice",c).put("timing",timing).put("audit",m.getJSONObject("audit")).put("safe",safe).put("renderer",renderer);
    }
    synchronized JSONObject review(String id)throws Exception {
        if(tasks.busy())throw new IllegalStateException("Finish or stop the active level task before reviewing costs");
        long began=System.nanoTime();var original=source(id);var choice=original.getJSONObject("choice");
        if(!choice.getBoolean("budgeted"))throw new IllegalArgumentException("Select an original budget decision to review");
        var samples=new JSONArray();var sources=new JSONArray().put(original);var excluded=new JSONArray();boolean invalid=false;
        var calibration=new HashSet<String>();for(Object raw:choice.getJSONObject("decision").getJSONArray("sources"))calibration.add(((JSONObject)raw).getString("id"));
        for(Object raw:tasks.list().getJSONArray("tasks")) {
            var task=(JSONObject)raw;String other=task.getString("id");var c=task.optJSONObject("pickupChoice124");
            if(other.equals(id)||calibration.contains(other)||c==null||c.getBoolean("budgeted")||!c.getJSONObject("parameters").similar(choice.getJSONObject("parameters"))||!Instant.parse(c.getString("createdAt")).isAfter(Instant.parse(choice.getString("createdAt"))))continue;
            if(!c.getString("host").equals(choice.getString("host"))||!c.getString("algorithm").equals(choice.getString("algorithm"))) {excluded.put(new JSONObject().put("id",other).put("reason","Different resource or algorithm context"));continue;}
            try {
                var s=source(other);var cost=TaskCost125.summary(s.getJSONObject("timing"));var terminal=cost.optJSONObject("terminal");String strategy=c.optJSONObject("fact")==null?"":c.getJSONObject("fact").getString("strategy");
                if(!Set.of("direct","full").contains(strategy)||terminal==null||!terminal.getBoolean("comparable")||!s.getString("renderer").equals(original.getString("renderer"))) {excluded.put(new JSONObject().put("id",other).put("reason","Incomplete, interrupted, different renderer or unsupported comparison strategy"));continue;}
                samples.put(new JSONObject().put("id",other).put("strategy",strategy).put("seconds",terminal.getDouble("seconds")).put("pickups",s.getJSONObject("audit").getInt("aiPickups")).put("safe",s.getBoolean("safe")));sources.put(s);
            } catch(Exception e) {invalid=true;excluded.put(new JSONObject().put("id",other).put("reason","Matched evidence invalid: "+String.valueOf(e.getMessage())));}
        }
        var decision=choice.getJSONObject("decision").getJSONObject("result");var metrics=GameCostReview125.analyze(decision,TaskCost125.summary(original.getJSONObject("timing")).put("outcomeSafe",original.getBoolean("safe")),invalid?new JSONArray():samples);
        if(invalid)metrics.put("reason","Invalid matched evidence prevents a favorable comparison; inspect exclusions");
        String reviewId=UUID.randomUUID().toString();try(var files=Files.list(reviews)){if(files.filter(p->p.toString().endsWith(".json")).count()>=256)throw new IllegalStateException("Retained review limit reached; original reviews preserved");}
        var result=new JSONObject().put("id",reviewId).put("taskId",id).put("at",Instant.now().toString()).put("implementation",implementation).put("decision",decision).put("metrics",metrics).put("samples",samples).put("sources",sources).put("excluded",excluded).put("invalidMatchedEvidence",invalid).put("reviewSeconds",(System.nanoTime()-began)/1e9).put("scope","Later repeated explicit comparisons, not randomization or a guarantee. Original policy unchanged. Complete first-outcome spans, later checks and recovered wall spans are separate. Review clock excludes final review-file write.");
        AgentFiles042.saveChecked(reviews.resolve(reviewId+".json"),result);return result;
    }
    synchronized JSONObject verifyReview(String id)throws Exception {
        if(tasks.busy())throw new IllegalStateException("Finish or stop the active level task before rechecking a review");
        if(!UUID.fromString(id).toString().equals(id))throw new IllegalArgumentException("Canonical review ID required");var r=AgentFiles042.loadChecked(reviews.resolve(id+".json"));
        if(!r.getString("id").equals(id)||!r.getString("implementation").equals(implementation))throw new IllegalStateException("Review identity or implementation changed");
        if(r.getBoolean("invalidMatchedEvidence"))throw new IllegalStateException("Original review retained invalid evidence; it cannot certify a comparison");
        var actualSamples=new JSONArray();JSONObject original=null;
        for(Object raw:r.getJSONArray("sources")) {
            var saved=(JSONObject)raw;var actual=source(saved.getString("id"));
            if(!actual.getJSONObject("choice").similar(saved.getJSONObject("choice"))||!actual.getJSONObject("audit").similar(saved.getJSONObject("audit"))||!actual.getString("renderer").equals(saved.getString("renderer"))||actual.getBoolean("safe")!=saved.getBoolean("safe"))throw new IllegalStateException("Original review source changed");
            costs.verifySnapshot(saved.getJSONObject("timing"));
            if(saved.getString("id").equals(r.getString("taskId")))original=saved;
            else actualSamples.put(new JSONObject().put("id",saved.getString("id")).put("strategy",saved.getJSONObject("choice").getJSONObject("fact").getString("strategy")).put("seconds",saved.getJSONObject("timing").getJSONObject("terminal").getDouble("seconds")).put("pickups",saved.getJSONObject("audit").getInt("aiPickups")).put("safe",saved.getBoolean("safe")));
        }
        if(original==null||!r.getJSONArray("samples").similar(actualSamples))throw new IllegalStateException("Review sample membership changed");
        var decision=original.getJSONObject("choice").getJSONObject("decision").getJSONObject("result");if(!decision.similar(r.getJSONObject("decision")))throw new IllegalStateException("Review decision changed");
        var expectedIds=new HashSet<String>();var retainedIds=new HashSet<String>();var excludedCalibration=new HashSet<String>();
        var originalChoice=original.getJSONObject("choice");Instant cutoff=Instant.parse(r.getString("at"));
        for(Object raw:originalChoice.getJSONObject("decision").getJSONArray("sources"))excludedCalibration.add(((JSONObject)raw).getString("id"));
        for(Object raw:actualSamples)retainedIds.add(((JSONObject)raw).getString("id"));
        for(Object raw:tasks.list().getJSONArray("tasks")) {
            var t=(JSONObject)raw;String other=t.getString("id");var c=t.optJSONObject("pickupChoice124");
            if(c==null||other.equals(r.getString("taskId"))||excludedCalibration.contains(other)||c.getBoolean("budgeted")||!c.getJSONObject("parameters").similar(originalChoice.getJSONObject("parameters"))||!c.getString("host").equals(originalChoice.getString("host"))||!c.getString("algorithm").equals(originalChoice.getString("algorithm")))continue;
            Instant created=Instant.parse(c.getString("createdAt"));if(!created.isAfter(Instant.parse(originalChoice.getString("createdAt")))||created.isAfter(cutoff))continue;
            var timing=costs.snapshot(other);var terminal=timing==null?null:timing.optJSONObject("terminal");var fact=c.optJSONObject("fact");
            if(terminal!=null&&terminal.getBoolean("comparable")&&!Instant.parse(terminal.getString("at")).isAfter(cutoff)) {
                var checked=source(other); // A changed renderer/fact must not hide an inconvenient trial.
                if(fact!=null&&Set.of("direct","full").contains(fact.getString("strategy"))&&checked.getString("renderer").equals(original.getString("renderer")))expectedIds.add(other);
            }
        }
        if(!retainedIds.equals(expectedIds))throw new IllegalStateException("Independent cohort changed or omitted a matched trial");
        var expected=GameCostReview125.analyze(decision,TaskCost125.summary(original.getJSONObject("timing")).put("outcomeSafe",original.getBoolean("safe")),r.getBoolean("invalidMatchedEvidence")?new JSONArray():actualSamples);
        if(r.getBoolean("invalidMatchedEvidence"))expected.put("reason","Invalid matched evidence prevents a favorable comparison; inspect exclusions");
        if(!expected.similar(r.getJSONObject("metrics")))throw new IllegalStateException("Review metrics changed");return r;
    }
    synchronized JSONObject latestReview(String taskId)throws Exception {
        tasks.read(taskId);JSONObject latest=null;
        try(var files=Files.list(reviews)) {for(Path p:files.filter(f->f.toString().endsWith(".json")).toList()) {
            var r=AgentFiles042.loadChecked(p);if(r.getString("taskId").equals(taskId)&&(latest==null||Instant.parse(r.getString("at")).isAfter(Instant.parse(latest.getString("at")))))latest=r;
        }}
        if(latest==null)throw new IllegalStateException("No saved cost review for this task");return verifyReview(latest.getString("id"));
    }
    @Override public synchronized void close(){tasks.close();}
}
