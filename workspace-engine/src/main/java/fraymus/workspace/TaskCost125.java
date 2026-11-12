package fraymus.workspace;

import org.json.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** Observations of actual calls. These records never authorize task effects. */
final class TaskCost125 {
    private final Path root;
    private final String boot = UUID.randomUUID().toString();
    private final Map<String,JSONObject> records = new HashMap<>();
    private final Map<String,Long> starts = new HashMap<>();
    private final Map<String,String> damaged = new HashMap<>();
    TaskCost125(Path project) throws Exception {
        root=project.resolve("game-costs125"); Files.createDirectories(root);
        try(var files=Files.list(root)) {
            for(Path p:files.filter(f->f.toString().endsWith(".json")).toList()) try {
                var r=AgentFiles042.loadChecked(p); String id=id(r.getString("id"));
                if(!p.getFileName().toString().equals(id+".json"))throw new IllegalStateException("Cost record identity changed");
                records.put(id,r);
            } catch(Exception e) { damaged.put(p.getFileName().toString(),e.getMessage()); }
        }
    }
    private static String id(String s) { if(!UUID.fromString(s).toString().equals(s))throw new IllegalArgumentException("Canonical task ID required");return s; }
    private Path file(String id) { return root.resolve(id(id)+".json"); }
    private void save(JSONObject r) throws Exception { AgentFiles042.saveChecked(file(r.getString("id")),r); }
    JSONObject read(String id) throws Exception {
        id(id); if(damaged.containsKey(id+".json"))throw new IllegalStateException("Cost evidence needs attention; original retained");
        var r=records.get(id);
        if(r!=null&&!r.similar(AgentFiles042.loadChecked(file(id))))throw new IllegalStateException("Cost evidence changed outside its owner");
        return r;
    }
    void admitted(String id,JSONObject task,long began,Instant at) throws Exception {
        var prior=read(id); String requestHash=WorldFrames.hash(new JSONObject().put("request",task.getString("request")));
        if(prior!=null) { if(!prior.getString("requestHash").equals(requestHash))throw new IllegalStateException("Cost request binding changed");return; }
        var r=new JSONObject().put("schema",125).put("id",id).put("requestHash",requestHash).put("createdAt",at.toString()).put("boot",boot).put("restarted",false).put("unknownCalls",0).put("events",new JSONArray());
        r.getJSONArray("events").put(new JSONObject().put("phase","admission").put("seconds",(System.nanoTime()-began)/1e9).put("at",Instant.now().toString()).put("late",false).put("outcome","returned"));
        records.put(id,r); starts.put(id,began); save(r);
    }
    long begin(String id,String phase) throws Exception {
        long began=System.nanoTime(); var r=read(id); if(r==null)return began;
        if(r.getJSONArray("events").length()>=4094)throw new IllegalStateException("Cost history limit reached; retained evidence needs review");
        if(r.has("pending")) {
            var pending=r.getJSONObject("pending");
            if(pending.getString("boot").equals(boot))throw new IllegalStateException("Unfinished cost call requires recovery");
            r.getJSONArray("events").put(new JSONObject().put("phase",pending.getString("phase")).put("at",Instant.now().toString()).put("outcome","interrupted; duration unknown").put("late",pending.getBoolean("late")));
            r.put("unknownCalls",r.getInt("unknownCalls")+1).remove("pending");
        }
        if(!r.getString("boot").equals(boot)&&!r.has("terminal"))r.put("restarted",true);
        r.put("pending",new JSONObject().put("phase",phase).put("boot",boot).put("late",r.has("terminal")).put("at",Instant.now().toString())); save(r);return began;
    }
    void end(String id,String phase,long began,String outcome,boolean terminal,boolean recovered) throws Exception {
        var r=read(id);if(r==null)return;
        var pending=r.getJSONObject("pending"); if(!pending.getString("phase").equals(phase)||!pending.getString("boot").equals(boot))throw new IllegalStateException("Cost call ownership changed");
        var now=Instant.now();double seconds=(System.nanoTime()-began)/1e9;
        r.getJSONArray("events").put(new JSONObject().put("phase",phase).put("seconds",seconds).put("at",now.toString()).put("outcome",outcome).put("late",pending.getBoolean("late")));r.remove("pending");
        if(recovered)r.put("restarted",true);
        if(terminal&&!r.has("terminal")) {
            boolean monotonic=starts.containsKey(id)&&r.getString("boot").equals(boot);
            double span=monotonic?(System.nanoTime()-starts.get(id))/1e9:Duration.between(Instant.parse(r.getString("createdAt")),now).toNanos()/1e9;
            r.put("terminal",new JSONObject().put("at",now.toString()).put("seconds",span).put("clock",monotonic?"monotonic":"wall span across restart; clock continuity unproven").put("comparable",monotonic&&!r.getBoolean("restarted")&&r.getInt("unknownCalls")==0&&span>0));
        }
        save(r);
    }
    JSONObject snapshot(String id) throws Exception { var r=read(id);return r==null?null:WorldFrames.copy(r); }
    static JSONObject summary(JSONObject r) {
        if(r==null)return new JSONObject().put("available",false).put("reason","No edition125 timing record; original task remains authoritative");
        var phases=new JSONObject(); double recorded=0,late=0;
        for(Object raw:r.getJSONArray("events")) { var e=(JSONObject)raw;if(!e.has("seconds"))continue;double seconds=e.getDouble("seconds");recorded+=seconds;if(e.getBoolean("late"))late+=seconds;
            String phase=e.getString("phase");var p=phases.optJSONObject(phase);if(p==null){p=new JSONObject().put("calls",0).put("seconds",0);phases.put(phase,p);}p.put("calls",p.getInt("calls")+1).put("seconds",p.getDouble("seconds")+seconds);
        }
        return new JSONObject().put("available",true).put("terminal",r.optJSONObject("terminal")).put("recordedOperationSeconds",recorded).put("laterCheckSeconds",late).put("phases",phases).put("unknownCalls",r.getInt("unknownCalls")).put("restarted",r.getBoolean("restarted")).put("pending",r.has("pending")).put("scope","First verified outcome span includes caller waits and earlier record writes. Operation durations exclude each ending receipt write. Later checks are separate; interrupted calls have unknown duration. Wall-clock spans are descriptive and cannot calibrate a decision.");
    }
    void verifySnapshot(JSONObject original) throws Exception {
        var current=read(original.getString("id"));if(current==null||original.has("pending"))throw new IllegalStateException("Original completed cost snapshot unavailable");
        for(String key:List.of("id","schema","requestHash","createdAt","boot","terminal"))if(!Objects.equals(String.valueOf(original.opt(key)),String.valueOf(current.opt(key)))) {
            // JSONObject serialization order is not an evidence comparison.
            if(!(original.opt(key) instanceof JSONObject a&&current.opt(key) instanceof JSONObject b&&a.similar(b)))throw new IllegalStateException("Original cost metadata changed: "+key);
        }
        var a=original.getJSONArray("events");var b=current.getJSONArray("events");if(a.length()>b.length())throw new IllegalStateException("Original cost history truncated");
        for(int i=0;i<a.length();i++)if(!a.getJSONObject(i).similar(b.getJSONObject(i)))throw new IllegalStateException("Original cost history changed");
    }
}
