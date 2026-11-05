package fraymus.workspace;

import org.json.*;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

/** One native world from initial impulse to endpoint; no reset during recovery. */
final class LaterTrial069 {
    interface Stage {void save(JSONObject partial)throws Exception;}
    private static final class Identity {static final String VALUE=implementation();}
    static String source(){return Identity.VALUE;}
    private static String implementation(){var b=new StringBuilder("later-observation-native-069-v1").append(TemporalTrial067.source());var queue=new ArrayDeque<Class<?>>();queue.addAll(List.of(LaterTrial069.class,LaterRamp069.class,LaterPolicy069.class,LaterContact069.class,FrozenCalibration069.class));while(!queue.isEmpty()){var c=queue.remove();try(var in=c.getResourceAsStream(c.getName().substring(c.getPackageName().length()+1)+".class")){b.append(MapDefinition.sha(in.readAllBytes()));}catch(Exception e){throw new IllegalStateException(e);}Arrays.stream(c.getDeclaredClasses()).sorted(Comparator.comparing(Class::getName)).forEach(queue::add);}return MeasurementStore.hash(b.toString());}
    static JSONObject run(JSONObject task,JSONObject decision,JSONObject event,Path dir,Runnable cancel,Stage stage)throws Exception{
        if(!decision.getBoolean("execute"))throw new IllegalArgumentException("Cannot execute a refusal");
        var c=DecisionPolicy065.definition(task,decision.getDouble("speedMps"));Stopping063.queryDomain(c);long start=System.nanoTime();
        var r=new JSONObject().put("id",UUID.randomUUID().toString()).put("source",source()).put("task",WorldFrames.copy(task)).put("decision",WorldFrames.copy(decision)).put("definition",c).put("disturbance",event).put("externalReceipts",new JSONArray()).put("variant","baseline").put("ticks",180).put("stride",4).put("startedAt",Instant.now().toString());
        var times=new JSONObject().put("decisionSampleMillis",0).put("plannerMillis",0).put("impulseCallMillis",0).put("laterSampleMillis",0).put("laterPlannerMillis",0).put("laterImpulseMillis",0);
        try(var world=new LaterRamp069(c,dir)){
            r.put("settings",world.settings());var samples=new JSONArray().put(world.sample());r.put("samples",samples);
            for(int tick=1;tick<=180;tick++){cancel.run();world.step();
                if(tick==event.getInt("tick")){
                    var before=world.sample();world.disturb(c,event);
                    r.getJSONArray("externalReceipts").put(new JSONObject().put("worldId",r.getString("id")).put("event",WorldFrames.copy(event))
                        .put("before",before).put("after",world.sample()).put("appliedAt",Instant.now().toString()));
                }
                JSONObject sample=null;if(tick%4==0){long at=System.nanoTime();sample=world.sample();if(tick==60)times.put("decisionSampleMillis",(System.nanoTime()-at)/1e6);if(tick==100||tick==104||tick==108)times.put("laterSampleMillis",times.getDouble("laterSampleMillis")+(System.nanoTime()-at)/1e6);samples.put(sample);}
                if(tick==60&&decision.optBoolean("recover")){
                    r.put("sampleAtDecision",WorldFrames.copy(sample)).put("decisionObservedAt",Instant.now().toString());long at=System.nanoTime();
                    var state=plannerState(task,decision,c,samples);var plan=TemporalContact067.plan(task,state);times.put("plannerMillis",(System.nanoTime()-at)/1e6);
                    r.put("correctionPlan",plan).put("correctionPlannedAt",Instant.now().toString());stage.save(WorldFrames.copy(r));cancel.run();
                    if(plan.getBoolean("correct")){at=System.nanoTime();world.correct(plan.getDouble("deltaVelocityMps"));times.put("impulseCallMillis",(System.nanoTime()-at)/1e6);r.put("correctionReceipt",new JSONObject().put("worldId",r.getString("id")).put("tick",60).put("deltaVelocityMps",plan.getDouble("deltaVelocityMps")).put("before",WorldFrames.copy(sample)).put("after",world.sample()).put("appliedAt",Instant.now().toString()));}
                }
                if(tick==108&&decision.optBoolean("later")){
                    r.put("laterObservedAt",Instant.now().toString());long at=System.nanoTime();
                    var plan=LaterContact069.plan(task,c,samples,r.optJSONObject("correctionReceipt"));
                    times.put("laterPlannerMillis",(System.nanoTime()-at)/1e6);
                    r.put("laterPlan",plan).put("laterPlannedAt",Instant.now().toString());stage.save(WorldFrames.copy(r));cancel.run();
                    if(plan.getBoolean("correct")){at=System.nanoTime();world.correct(plan.getDouble("deltaVelocityMps"));times.put("laterImpulseMillis",(System.nanoTime()-at)/1e6);
                        r.put("laterReceipt",new JSONObject().put("worldId",r.getString("id")).put("tick",108).put("deltaVelocityMps",plan.getDouble("deltaVelocityMps")).put("before",WorldFrames.copy(sample)).put("after",world.sample()).put("appliedAt",Instant.now().toString()));}
                }
            }
            times.put("nativeWallMillis",(System.nanoTime()-start)/1e6);r.put("timing069",times).put("observedAt",Instant.now().toString()).put("summary",Stopping063.summary(r));r.put("sha256",WorldFrames.hash(r));verify(r);return r;
        }
    }
    static void verify(JSONObject r){
        var copy=WorldFrames.copy(r);copy.remove("sha256");if(!r.getString("source").equals(source())||!r.getString("sha256").equals(WorldFrames.hash(copy)))throw new IllegalStateException("Recovery source or record changed");UUID.fromString(r.getString("id"));
        var task=r.getJSONObject("task");var decision=r.getJSONObject("decision");var c=r.getJSONObject("definition");if(!c.similar(DecisionPolicy065.definition(task,decision.getDouble("speedMps")))||!decision.getBoolean("execute"))throw new IllegalStateException("Recovery definition differs");Stopping063.queryDomain(c);
        if(r.getInt("ticks")!=180||r.getInt("stride")!=4||!r.getString("variant").equals("baseline"))throw new IllegalStateException("Recovery clock differs");var settings=r.getJSONObject("settings");if(!settings.getString("backend").equals(BulletRamp.BACKEND)||settings.getInt("iterations")!=10||!settings.getJSONArray("angularFactor").similar(new JSONArray(new int[]{1,1,1})))throw new IllegalStateException("Recovery native settings differ");
        var samples=r.getJSONArray("samples");if(samples.length()!=46)throw new IllegalStateException("Recovery trace incomplete");for(int i=0;i<46;i++){var s=samples.getJSONObject(i);if(s.getInt("tick")!=i*4||Math.abs(s.getDouble("timeS")-i/60.0)>1e-12)throw new IllegalStateException("Recovery trace clock changed");Stopping063.projected(c,s);}
        Instant started=Instant.parse(r.getString("startedAt")),ended=Instant.parse(r.getString("observedAt"));if(ended.isBefore(started))throw new IllegalStateException("Recovery chronology differs");

        var event=r.getJSONObject("disturbance");
        if(!LaterRamp069.event(event.getString("family"),event.getInt("amplitude")).similar(event))throw new IllegalStateException("Disturbance protocol changed");
        var external=r.getJSONArray("externalReceipts");
        if(external.length()!=(event.getInt("tick")<0?0:1))throw new IllegalStateException("External event count differs");
        for(Object raw:external)verifyExternal(r,(JSONObject)raw);
        boolean recover=decision.optBoolean("recover");if(recover){
            if(!Set.of("single","calibrated","later","laterCalibrated").contains(decision.getString("method")))throw new IllegalStateException("Unexpected replanning method");var sample=r.getJSONObject("sampleAtDecision");if(!sample.similar(samples.getJSONObject(15)))throw new IllegalStateException("Observation is not this world's tick60 sample");
            var state=plannerState(task,decision,c,samples);var expected=TemporalContact067.plan(task,state);if(!expected.similar(r.getJSONObject("correctionPlan")))throw new IllegalStateException("Correction uses changed or leaked observation");
            Instant observed=Instant.parse(r.getString("decisionObservedAt")),planned=Instant.parse(r.getString("correctionPlannedAt"));if(observed.isBefore(started)||planned.isBefore(observed)||ended.isBefore(planned))throw new IllegalStateException("Correction sequence changed");
            if(expected.getBoolean("correct")){var receipt=r.getJSONObject("correctionReceipt");if(!receipt.getString("worldId").equals(r.getString("id"))||receipt.getInt("tick")!=60||receipt.getDouble("deltaVelocityMps")!=expected.getDouble("deltaVelocityMps")||!receipt.getJSONObject("before").similar(sample))throw new IllegalStateException("Correction receipt identity differs");Instant applied=Instant.parse(receipt.getString("appliedAt"));if(applied.isBefore(planned)||ended.isBefore(applied))throw new IllegalStateException("Action precedes its saved plan");var after=receipt.getJSONObject("after");Stopping063.projected(c,after);
                for(String key:List.of("tick","timeS","position","rotation","angularVelocity"))if(!new JSONObject().put("x",sample.get(key)).similar(new JSONObject().put("x",after.get(key))))throw new IllegalStateException("Correction teleported or rewrote angular state");
                double angle=Math.toRadians(c.getDouble("angleDeg")),dv=expected.getDouble("deltaVelocityMps");double[] change={dv*Math.cos(angle),-dv*Math.sin(angle),0};for(int i=0;i<3;i++)if(Math.abs(after.getJSONArray("velocity").getDouble(i)-sample.getJSONArray("velocity").getDouble(i)-change[i])>1e-6)throw new IllegalStateException("Actual impulse response differs");
            }else if(r.has("correctionReceipt"))throw new IllegalStateException("Unexpected extra action");
        }else for(String key:List.of("correctionPlan","correctionReceipt","sampleAtDecision","decisionObservedAt","correctionPlannedAt"))if(r.has(key))throw new IllegalStateException("Direct attempt contains undeclared recovery");
        if(decision.optBoolean("later")){
            var prefix=new JSONArray();for(int i=0;i<=27;i++)prefix.put(samples.get(i));
            var expected=LaterContact069.plan(task,c,prefix,r.optJSONObject("correctionReceipt"));
            if(!expected.similar(r.getJSONObject("laterPlan")))throw new IllegalStateException("Later plan changed or used future data");
            Instant observed=Instant.parse(r.getString("laterObservedAt")),planned=Instant.parse(r.getString("laterPlannedAt"));
            if(observed.isBefore(started)||planned.isBefore(observed)||ended.isBefore(planned)||r.has("correctionPlannedAt")&&observed.isBefore(Instant.parse(r.getString("correctionPlannedAt"))))throw new IllegalStateException("Later chronology changed");
            if(expected.getBoolean("correct")){
                var receipt=r.getJSONObject("laterReceipt");var before=samples.getJSONObject(27);var after=receipt.getJSONObject("after");
                if(!receipt.getString("worldId").equals(r.getString("id"))||receipt.getInt("tick")!=108||!before.similar(receipt.getJSONObject("before"))||receipt.getDouble("deltaVelocityMps")!=expected.getDouble("deltaVelocityMps"))throw new IllegalStateException("Later receipt differs");
                Instant applied=Instant.parse(receipt.getString("appliedAt"));if(applied.isBefore(planned)||applied.isAfter(ended))throw new IllegalStateException("Later action preceded plan");
                for(String key:List.of("tick","timeS","position","rotation","angularVelocity"))if(!Procedure044.equal(before.get(key),after.get(key)))throw new IllegalStateException("Later action teleported body");
                double a=Math.toRadians(c.getDouble("angleDeg")),dv=expected.getDouble("deltaVelocityMps");double[] change={dv*Math.cos(a),-dv*Math.sin(a),0};
                for(int i=0;i<3;i++)if(Math.abs(after.getJSONArray("velocity").getDouble(i)-before.getJSONArray("velocity").getDouble(i)-change[i])>1e-6)throw new IllegalStateException("Later actual response differs");
            }else if(r.has("laterReceipt"))throw new IllegalStateException("Undeclared later impulse");
        }else for(String key:List.of("laterPlan","laterReceipt","laterObservedAt","laterPlannedAt"))if(r.has(key))throw new IllegalStateException("Single policy contains later action");
        var times=r.getJSONObject("timing069");for(String key:List.of("nativeWallMillis","decisionSampleMillis","plannerMillis","impulseCallMillis","laterSampleMillis","laterPlannerMillis","laterImpulseMillis")){double value=times.getDouble(key);if(!Double.isFinite(value)||value<0)throw new IllegalStateException("Invalid measured timing");}
        if(!recover&&times.getDouble("plannerMillis")!=0||!r.has("correctionReceipt")&&times.getDouble("impulseCallMillis")!=0)throw new IllegalStateException("Timing claims an unused operation");
        if(!decision.optBoolean("later")&&times.getDouble("laterPlannerMillis")!=0||!r.has("laterReceipt")&&times.getDouble("laterImpulseMillis")!=0)throw new IllegalStateException("Unused later timing");
        if(!Stopping063.summary(r).similar(r.getJSONObject("summary")))throw new IllegalStateException("Recovery endpoint summary changed");
    }

    static JSONObject plannerState(JSONObject task,JSONObject decision,JSONObject definition,JSONArray samples){
        var d=WorldFrames.copy(decision).put("method","temporal");
        return TemporalContact067.plannerState(task,d,definition,samples);
    }
    static void verifyExternal(JSONObject record,JSONObject receipt){
        var event=record.getJSONObject("disturbance");int tick=event.getInt("tick");var before=receipt.getJSONObject("before");var after=receipt.getJSONObject("after");
        if(!receipt.getString("worldId").equals(record.getString("id"))||!event.similar(receipt.getJSONObject("event"))||before.getInt("tick")!=tick||after.getInt("tick")!=tick
            ||!after.similar(record.getJSONArray("samples").getJSONObject(tick/4)))throw new IllegalStateException("External event identity/sample differs");
        var applied=Instant.parse(receipt.getString("appliedAt"));
        if(applied.isBefore(Instant.parse(record.getString("startedAt")))||applied.isAfter(Instant.parse(record.getString("observedAt"))))throw new IllegalStateException("External chronology differs");
        for(String key:List.of("tick","timeS","position","rotation"))if(!new JSONObject().put("x",before.get(key)).similar(new JSONObject().put("x",after.get(key))))throw new IllegalStateException("External event teleported the body");
        double[] impulse=LaterRamp069.impulseVector(record.getJSONObject("definition"),event);boolean torque=event.getString("kind").equals("torque");
        for(String key:List.of("velocity","angularVelocity"))for(int i=0;i<3;i++){
            double expected=key.equals("velocity")?(torque?0:impulse[i]):(torque?24*impulse[i]:0);
            if(Math.abs(after.getJSONArray(key).getDouble(i)-before.getJSONArray(key).getDouble(i)-expected)>1e-5)throw new IllegalStateException("External mass/inertia response differs");
        }
    }

    static double parity(JSONObject old,JSONObject current){Stopping063.verify(old);verify(current);double maximum=0;for(int i=0;i<46;i++)for(String key:List.of("position","velocity","angularVelocity","rotation")){var a=old.getJSONArray("samples").getJSONObject(i).getJSONArray(key);var b=current.getJSONArray("samples").getJSONObject(i).getJSONArray(key);for(int k=0;k<a.length();k++)maximum=Math.max(maximum,Math.abs(a.getDouble(k)-b.getDouble(k)));}return maximum;}
}
