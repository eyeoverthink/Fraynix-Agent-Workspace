package fraymus.workspace;

import java.nio.file.*;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

/** Independent reference scoring. No reference enters tracking, filtering or forecast construction. */
final class PhysicalAudit130 {
    static final String VERSION="physical-reference-audit-130-v1";
    private final Path directory;
    private final MetricObservationStore metrics;
    PhysicalAudit130(Path project,MetricObservationStore metrics){directory=project.resolve("physical-audits130");this.metrics=metrics;}
    static String identity(){var s=new StringBuilder(VERSION);for(Class<?> c:List.of(PhysicalAudit130.class,ObservationArtifact128.class,ObservationForecast129.class))try(var in=c.getResourceAsStream(c.getSimpleName()+".class")){s.append(PerceptionStore.hash(in.readAllBytes()));}catch(Exception e){throw new IllegalStateException(e);}return MeasurementStore.hash(s.toString());}
    private static JSONObject selection(JSONObject input){var s=new JSONObject().put("runId",input.getString("runId")).put("geometryId",input.getString("geometryId"));if(input.has("profileId"))s.put("profileId",input.getString("profileId"));return s;}
    private static String sourceHash(JSONObject view){var copy=WorldFrames.copy(view);copy.remove("nativeStale");return WorldFrames.hash(copy);}
    synchronized JSONObject save(JSONObject input)throws Exception{
        SpatialScene.keys(input,"runId","geometryId","profileId","references","source","referenceRadiusM");
        var view=metrics.view(selection(input));if(!view.getString("runStatus").equals("completed"))throw new IllegalStateException("Audit requires a completed immutable run");
        var result=compute(view,input);result.put("selection",selection(input)).put("sourceSha256",sourceHash(view)).put("metricCodeSha256",view.getString("metricCodeSha256")).put("trackerCodeSha256",view.getString("codeSha256")).put("codeSha256",identity()).put("referenceInput",WorldFrames.copy(input));
        String id=WorldFrames.hash(result);result.put("id",id);Files.createDirectories(directory);
        try(var channel=FileChannel.open(directory.resolve("audit.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);var lock=channel.lock()){
            Path file=directory.resolve(id+".json");if(Files.exists(file))return read(id);
            try(var files=Files.list(directory)){if(files.filter(p->p.toString().endsWith(".json")).count()>=64)throw new IllegalStateException("64 retained audits reached; no pruning");}
            byte[] bytes=result.toString(2).getBytes(StandardCharsets.UTF_8);Path pending=directory.resolve(id+".pending");
            try(var out=FileChannel.open(pending,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE)){ByteBuffer b=ByteBuffer.wrap(bytes);while(b.hasRemaining())out.write(b);out.force(true);}Files.move(pending,file,StandardCopyOption.ATOMIC_MOVE);
        }
        return read(id);
    }
    synchronized JSONObject read(String id)throws Exception{
        if(!id.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Invalid audit identity");Path file=directory.resolve(id+".json");if(Files.size(file)>1_000_000)throw new IllegalStateException("Audit size exceeds bound");
        var result=new JSONObject(Files.readString(file));var payload=WorldFrames.copy(result);payload.remove("id");if(!id.equals(result.getString("id"))||!id.equals(WorldFrames.hash(payload))||!identity().equals(result.getString("codeSha256")))throw new IllegalStateException("Audit evidence or implementation changed; preserve the original");
        var view=metrics.view(result.getJSONObject("selection"));if(!sourceHash(view).equals(result.getString("sourceSha256")))throw new IllegalStateException("Original source, clock, geometry or trace changed");
        var checked=compute(view,result.getJSONObject("referenceInput"));for(String key:checked.keySet())if(!new JSONObject().put("value",checked.get(key)).similar(new JSONObject().put("value",result.get(key))))throw new IllegalStateException("Audit outcome differs: "+key);
        return result;
    }
    static JSONObject compute(JSONObject view,JSONObject input){
        String source=input.getString("source");if(source.isBlank()||source.length()>1500)throw new IllegalArgumentException("Declare independent reference source");
        double allowance=input.optDouble("referenceRadiusM",0);PlaneCalibration.bounded(allowance,0,.5);
        var refs=input.getJSONArray("references");if(refs.length()<1||refs.length()>96)throw new IllegalArgumentException("Provide1..96 later references for audit; no training occurs");
        var selected=new LinkedHashMap<String,JSONObject>();var bodies=new LinkedHashSet<String>();
        for(Object raw:refs){var ref=(JSONObject)raw;SpatialScene.keys(ref,"ordinal","bodyId","positionM");int ordinal=ref.getInt("ordinal");String body=ref.getString("bodyId");PlaneCalibration.point(ref.getJSONArray("positionM"));if(ordinal<=0||ordinal!=ref.getDouble("ordinal")||body.isBlank()||selected.putIfAbsent(body+":"+ordinal,ref)!=null)throw new IllegalArgumentException("Distinct later frame/body references required");bodies.add(body);}
        var forecasts=new LinkedHashMap<String,Map<String,JSONObject>>();var refusals=new JSONObject();
        // Forecasts are completed from observations alone before reference positions are scored.
        for(String policy:List.of("legacy-adjacent","explicit-budget-1m")){
            var predictions=new HashMap<String,JSONObject>();var refused=new JSONArray();
            for(String body:bodies){var rows=new JSONArray();for(Object raw:view.getJSONArray("rows")){var row=(JSONObject)raw;if(body.equals(row.getString("bodyId")))rows.put(row);}
                var definition=new JSONObject().put("horizonS",.6).put("maxGapS",.6).put("accelerationBoundMps2",5);if(!policy.equals("legacy-adjacent"))definition.put("forecastPolicy129",ObservationForecast129.POLICY).put("baselineWindowS",.6).put("maxForecastRadiusM",1);
                try{var forecast=ObservationArtifact128.forecast(new JSONObject().put("rows",rows),definition);for(Object raw:forecast.getJSONArray("rows")){var row=(JSONObject)raw;predictions.put(body+":"+row.getInt("ordinal"),row.getJSONObject("forecast"));}}
                catch(IllegalArgumentException e){refused.put(new JSONObject().put("bodyId",body).put("reason",e.getMessage()));}
            }forecasts.put(policy,predictions);refusals.put(policy,refused);
        }
        var measured=new JSONArray();var predicted=new JSONObject();for(String policy:forecasts.keySet())predicted.put(policy,new JSONArray());
        for(var entry:selected.entrySet()){
            var ref=entry.getValue();JSONObject row=null;for(Object raw:view.getJSONArray("rows")){var candidate=(JSONObject)raw;if(entry.getKey().equals(candidate.getString("bodyId")+":"+candidate.getInt("ordinal"))){if(row!=null)throw new IllegalStateException("Duplicate source row");row=candidate;}}
            if(row==null)throw new IllegalArgumentException("Reference does not match original trace");
            measured.put(score(ref,row.getBoolean("accepted")&&row.has("positionM"),row,allowance));
            for(var f:forecasts.entrySet()){var prediction=f.getValue().get(entry.getKey());predicted.getJSONArray(f.getKey()).put(score(ref,prediction!=null&&prediction.getBoolean("available"),prediction,allowance));}
        }
        var summaries=new JSONObject();for(String policy:forecasts.keySet())summaries.put(policy,summary(predicted.getJSONArray(policy)));
        return new JSONObject().put("schema",VERSION).put("referenceSource",source).put("referenceRadiusM",allowance).put("measurement",summary(measured)).put("measurementScores",measured).put("forecastSummaries",summaries).put("forecastScores",predicted).put("forecastRefusals",refusals).put("scope","Declared independent planar references; their origin is supplied, not automatically authenticated. Missing observations remain in the denominator. Coverage is empirical on these samples, not confidence. Forecast comparisons are one-step causal checks at reference timestamps, not a validation of the full0.6second horizon. No fitting, promotion, tracker change or native actuation.");
    }
    private static JSONObject score(JSONObject ref,boolean available,JSONObject point,double allowance){var score=WorldFrames.copy(ref).put("available",available);if(available){double error=PlaneCalibration.distance(PlaneCalibration.point(ref.getJSONArray("positionM")),PlaneCalibration.point(point.getJSONArray("positionM")));double radius=point.getDouble("radiusM");PlaneCalibration.bounded(radius,0,1_000_000);score.put("errorM",error).put("radiusM",radius).put("coveredCentre",error<=radius).put("coveredIncludingReferenceRadius",error<=radius+allowance);}return score;}
    private static JSONObject summary(JSONArray scores){int available=0,covered=0,withAllowance=0,within=0;double error=0,max=0,radius=0;for(Object raw:scores){var r=(JSONObject)raw;if(!r.getBoolean("available"))continue;available++;double e=r.getDouble("errorM");error+=e;max=Math.max(max,e);radius+=r.getDouble("radiusM");if(r.getBoolean("coveredCentre"))covered++;if(r.getBoolean("coveredIncludingReferenceRadius"))withAllowance++;if(e<=1)within++;}return new JSONObject().put("references",scores.length()).put("available",available).put("missing",scores.length()-available).put("coveredCentre",covered).put("coveredIncludingReferenceRadius",withAllowance).put("withinOneMetre",within).put("meanErrorM",available==0?JSONObject.NULL:error/available).put("maxErrorM",available==0?JSONObject.NULL:max).put("meanRadiusM",available==0?JSONObject.NULL:radius/available);}
}
