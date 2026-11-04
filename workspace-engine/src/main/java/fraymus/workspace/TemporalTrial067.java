package fraymus.workspace;

import org.json.*;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

/** One native world from initial impulse to endpoint; no reset during recovery. */
final class TemporalTrial067 {
    interface Stage {void save(JSONObject partial)throws Exception;}
    private static final class Identity {static final String VALUE=implementation();}
    static String source(){return Identity.VALUE;}
    private static String implementation(){var b=new StringBuilder("temporal-contact-native-067-v1").append(RecoveryTrial066.source());var queue=new ArrayDeque<Class<?>>();queue.addAll(List.of(TemporalTrial067.class,RecoveryRamp066.class,TemporalPolicy067.class,TemporalContact067.class,ValueScreen067.class));while(!queue.isEmpty()){var c=queue.remove();try(var in=c.getResourceAsStream(c.getName().substring(c.getPackageName().length()+1)+".class")){b.append(MapDefinition.sha(in.readAllBytes()));}catch(Exception e){throw new IllegalStateException(e);}Arrays.stream(c.getDeclaredClasses()).sorted(Comparator.comparing(Class::getName)).forEach(queue::add);}return MeasurementStore.hash(b.toString());}
    static JSONObject run(JSONObject task,JSONObject decision,Path dir,Runnable cancel,Stage stage)throws Exception{
        if(!decision.getBoolean("execute"))throw new IllegalArgumentException("Cannot execute a refusal");
        var c=DecisionPolicy065.definition(task,decision.getDouble("speedMps"));Stopping063.queryDomain(c);long start=System.nanoTime();
        var r=new JSONObject().put("id",UUID.randomUUID().toString()).put("source",source()).put("task",WorldFrames.copy(task)).put("decision",WorldFrames.copy(decision)).put("definition",c).put("variant","baseline").put("ticks",180).put("stride",4).put("startedAt",Instant.now().toString());
        var times=new JSONObject().put("decisionSampleMillis",0).put("plannerMillis",0).put("impulseCallMillis",0);
        try(var world=new RecoveryRamp066(c,dir)){
            r.put("settings",world.settings());var samples=new JSONArray().put(world.sample());r.put("samples",samples);
            for(int tick=1;tick<=180;tick++){cancel.run();world.step();JSONObject sample=null;if(tick%4==0){long at=System.nanoTime();sample=world.sample();if(tick==60)times.put("decisionSampleMillis",(System.nanoTime()-at)/1e6);samples.put(sample);}
                if(tick==60&&decision.optBoolean("recover")){
                    r.put("sampleAtDecision",WorldFrames.copy(sample)).put("decisionObservedAt",Instant.now().toString());long at=System.nanoTime();
                    var state=TemporalContact067.plannerState(task,decision,c,samples);var plan=TemporalContact067.plan(task,state);times.put("plannerMillis",(System.nanoTime()-at)/1e6);
                    r.put("correctionPlan",plan).put("correctionPlannedAt",Instant.now().toString());stage.save(WorldFrames.copy(r));cancel.run();
                    if(plan.getBoolean("correct")){at=System.nanoTime();world.correct(plan.getDouble("deltaVelocityMps"));times.put("impulseCallMillis",(System.nanoTime()-at)/1e6);r.put("correctionReceipt",new JSONObject().put("worldId",r.getString("id")).put("tick",60).put("deltaVelocityMps",plan.getDouble("deltaVelocityMps")).put("before",WorldFrames.copy(sample)).put("after",world.sample()).put("appliedAt",Instant.now().toString()));}
                }
            }
            times.put("nativeWallMillis",(System.nanoTime()-start)/1e6);r.put("timing067",times).put("observedAt",Instant.now().toString()).put("summary",Stopping063.summary(r));r.put("sha256",WorldFrames.hash(r));verify(r);return r;
        }
    }
    static void verify(JSONObject r){
        var copy=WorldFrames.copy(r);copy.remove("sha256");if(!r.getString("source").equals(source())||!r.getString("sha256").equals(WorldFrames.hash(copy)))throw new IllegalStateException("Recovery source or record changed");UUID.fromString(r.getString("id"));
        var task=r.getJSONObject("task");var decision=r.getJSONObject("decision");var c=r.getJSONObject("definition");if(!c.similar(DecisionPolicy065.definition(task,decision.getDouble("speedMps")))||!decision.getBoolean("execute"))throw new IllegalStateException("Recovery definition differs");Stopping063.queryDomain(c);
        if(r.getInt("ticks")!=180||r.getInt("stride")!=4||!r.getString("variant").equals("baseline"))throw new IllegalStateException("Recovery clock differs");var settings=r.getJSONObject("settings");if(!settings.getString("backend").equals(BulletRamp.BACKEND)||settings.getInt("iterations")!=10||!settings.getJSONArray("angularFactor").similar(new JSONArray(new int[]{1,1,1})))throw new IllegalStateException("Recovery native settings differ");
        var samples=r.getJSONArray("samples");if(samples.length()!=46)throw new IllegalStateException("Recovery trace incomplete");for(int i=0;i<46;i++){var s=samples.getJSONObject(i);if(s.getInt("tick")!=i*4||Math.abs(s.getDouble("timeS")-i/60.0)>1e-12)throw new IllegalStateException("Recovery trace clock changed");Stopping063.projected(c,s);}
        Instant started=Instant.parse(r.getString("startedAt")),ended=Instant.parse(r.getString("observedAt"));if(ended.isBefore(started))throw new IllegalStateException("Recovery chronology differs");
        boolean recover=decision.optBoolean("recover");if(recover){
            if(!Set.of("blind","observed","temporal","value").contains(decision.getString("method")))throw new IllegalStateException("Unexpected replanning method");var sample=r.getJSONObject("sampleAtDecision");if(!sample.similar(samples.getJSONObject(15)))throw new IllegalStateException("Observation is not this world's tick60 sample");
            var state=TemporalContact067.plannerState(task,decision,c,samples);var expected=TemporalContact067.plan(task,state);if(!expected.similar(r.getJSONObject("correctionPlan")))throw new IllegalStateException("Correction uses changed or leaked observation");
            Instant observed=Instant.parse(r.getString("decisionObservedAt")),planned=Instant.parse(r.getString("correctionPlannedAt"));if(observed.isBefore(started)||planned.isBefore(observed)||ended.isBefore(planned))throw new IllegalStateException("Correction sequence changed");
            if(expected.getBoolean("correct")){var receipt=r.getJSONObject("correctionReceipt");if(!receipt.getString("worldId").equals(r.getString("id"))||receipt.getInt("tick")!=60||receipt.getDouble("deltaVelocityMps")!=expected.getDouble("deltaVelocityMps")||!receipt.getJSONObject("before").similar(sample))throw new IllegalStateException("Correction receipt identity differs");Instant applied=Instant.parse(receipt.getString("appliedAt"));if(applied.isBefore(planned)||ended.isBefore(applied))throw new IllegalStateException("Action precedes its saved plan");var after=receipt.getJSONObject("after");Stopping063.projected(c,after);
                for(String key:List.of("tick","timeS","position","rotation","angularVelocity"))if(!new JSONObject().put("x",sample.get(key)).similar(new JSONObject().put("x",after.get(key))))throw new IllegalStateException("Correction teleported or rewrote angular state");
                double angle=Math.toRadians(c.getDouble("angleDeg")),dv=expected.getDouble("deltaVelocityMps");double[] change={dv*Math.cos(angle),-dv*Math.sin(angle),0};for(int i=0;i<3;i++)if(Math.abs(after.getJSONArray("velocity").getDouble(i)-sample.getJSONArray("velocity").getDouble(i)-change[i])>1e-6)throw new IllegalStateException("Actual impulse response differs");
            }else if(r.has("correctionReceipt"))throw new IllegalStateException("Unexpected extra action");
        }else for(String key:List.of("correctionPlan","correctionReceipt","sampleAtDecision","decisionObservedAt","correctionPlannedAt"))if(r.has(key))throw new IllegalStateException("Direct attempt contains undeclared recovery");
        var times=r.getJSONObject("timing067");for(String key:List.of("nativeWallMillis","decisionSampleMillis","plannerMillis","impulseCallMillis")){double value=times.getDouble(key);if(!Double.isFinite(value)||value<0)throw new IllegalStateException("Invalid measured timing");}
        if(!recover&&times.getDouble("plannerMillis")!=0||!r.has("correctionReceipt")&&times.getDouble("impulseCallMillis")!=0)throw new IllegalStateException("Timing claims an unused operation");
        if(!Stopping063.summary(r).similar(r.getJSONObject("summary")))throw new IllegalStateException("Recovery endpoint summary changed");
    }
    static double parity(JSONObject old,JSONObject current){Stopping063.verify(old);verify(current);double maximum=0;for(int i=0;i<46;i++)for(String key:List.of("position","velocity","angularVelocity","rotation")){var a=old.getJSONArray("samples").getJSONObject(i).getJSONArray(key);var b=current.getJSONArray("samples").getJSONObject(i).getJSONArray(key);for(int k=0;k<a.length();k++)maximum=Math.max(maximum,Math.abs(a.getDouble(k)-b.getDouble(k)));}return maximum;}
}
