package fraymus.workspace;

import org.json.*;
import java.util.*;
import java.nio.file.*;

/** Bounded planar forecasts from earlier measurements; never writes simulated body poses. */
final class ObservationArtifact128 {
    static final String KIND="observation-artifact-128-v1";
    private final Path imageRoot;
    final WorldObservationStore observations; final MetricObservationStore metrics; final WorldLedger worlds;
    ObservationArtifact128(Path project,WorldObservationStore o,MetricObservationStore m,WorldLedger w){imageRoot=project.toAbsolutePath().normalize().resolve("world-observations/datasets");observations=o;metrics=m;worlds=w;}
    static boolean own(JSONObject w){return KIND.equals(w.optString("profile128"));}
    static boolean requested(String text){return ChatPolicy044.has(text,"observation[- ]to[- ]artifact|(?:observed motion|calibrated (?:observation|footage)).*(?:explorer|artifact|interactive)");}
    static String code(){var s=new StringBuilder(KIND);for(Class<?> c:List.of(ObservationArtifact128.class,ObservationArtifactReport128.class,ObservationForecast129.class))try(var in=c.getResourceAsStream(c.getSimpleName()+".class")){s.append(PerceptionStore.hash(in.readAllBytes()));}catch(Exception e){throw new IllegalStateException(e);}return MeasurementStore.hash(s.toString());}
    static JSONObject definition(JSONObject in){
        SpatialScene.keys(in,"profile128","request","title","modelName","runId","geometryId","profileId","bodyId","horizonS","maxGapS","accelerationBoundMps2","saveSkillName","reviewAfterForecast128","forecastPolicy129","baselineWindowS","maxForecastRadiusM");
        if(!KIND.equals(in.getString("profile128")))throw new IllegalArgumentException("Unknown observation mission");
        var d=WorldFrames.copy(in);for(String k:List.of("request","title","modelName"))Creation.text(d,k,k.equals("request")?4000:140);
        for(String k:List.of("runId","geometryId"))UUID.fromString(d.getString(k));
        if(d.has("profileId")&&!d.getString("profileId").isEmpty())UUID.fromString(d.getString("profileId"));
        for(String k:List.of("horizonS","maxGapS","accelerationBoundMps2")){
            double v=d.has(k)?number(d,k):(k.equals("accelerationBoundMps2")?5:.6);
            PlaneCalibration.bounded(v,k.equals("accelerationBoundMps2")?0:.01,k.equals("accelerationBoundMps2")?100:2);d.put(k,v);
        }
        if(d.has("forecastPolicy129"))ObservationForecast129.validate(d);else if(d.has("baselineWindowS")||d.has("maxForecastRadiusM"))throw new IllegalArgumentException("Declare the bounded forecast policy for precision settings");
        String name=d.optString("saveSkillName","");if(!name.isEmpty()&&!name.matches("[a-z][a-z0-9-]{2,55}"))throw new IllegalArgumentException("Choose a3..56 character procedure name");d.put("saveSkillName",name);
        if(d.has("reviewAfterForecast128")&&!(d.get("reviewAfterForecast128") instanceof Boolean))throw new IllegalArgumentException("Review must be boolean");
        return d;
    }
    private static double number(JSONObject o,String k){if(!(o.get(k) instanceof Number))throw new IllegalArgumentException("Numeric "+k+" required");double v=o.getDouble(k);if(!Double.isFinite(v))throw new IllegalArgumentException("Nonfinite "+k);return v;}
    JSONObject source(JSONObject d,boolean current)throws Exception{
        var run=observations.read(d.getString("runId"));
        if(!run.getString("status").equals("completed")||!run.getString("codeSha256").equals(WorldObservationStore.identity()))throw new IllegalStateException("Choose a completed observation from this tracking implementation");
        var dataset=observations.dataset(run.getString("datasetId"));var frames=dataset.getJSONArray("frames");
        if(frames.length()<5||frames.length()>96||run.getInt("completedFrames")!=frames.length()-1)throw new IllegalStateException("Use a complete5..96-frame sequence; no hidden subsampling");
        String body=d.optString("bodyId","");if(body.isEmpty()){if(run.getJSONArray("bindings").length()!=1)throw new IllegalArgumentException("Select one body explicitly");body=run.getJSONArray("bindings").getJSONObject(0).getString("bodyId");}
        boolean bound=false;for(Object raw:run.getJSONArray("bindings"))if(((JSONObject)raw).getString("bodyId").equals(body))bound=true;if(!bound)throw new IllegalArgumentException("Body is not bound to this observation");
        var binding=run.getJSONObject("nativeBinding");if(current){var now=worlds.observe(binding.getString("worldId"));if(now.has("pending")||!now.getString("expectedHash").equals(binding.getString("expectedHash"))||now.getInt("expectedRevision")!=binding.getInt("expectedRevision"))throw new IllegalStateException("World changed; record and select a fresh observation binding");}
        // Read each original image, which verifies its stored hash; a trace alone is insufficient evidence.
        String datasetId=UUID.fromString(dataset.getString("id")).toString();Path folder=imageRoot.resolve(datasetId);if(Files.isSymbolicLink(imageRoot)||Files.isSymbolicLink(folder))throw new IllegalStateException("Original image location changed");long bytes=0;
        for(int i=0;i<frames.length();i++){Path file=folder.resolve(i+".image");if(!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS))throw new IllegalStateException("Original image missing or redirected");bytes=Math.addExact(bytes,Files.size(file));if(bytes>32_000_000)throw new IllegalStateException("Original image budget changed");if(!PerceptionStore.hash(Files.readAllBytes(file)).equals(frames.getJSONObject(i).getString("imageSha256")))throw new IllegalStateException("Original observation image integrity changed");}
        if(!dataset.similar(observations.dataset(datasetId))||!run.similar(observations.read(run.getString("id"))))throw new IllegalStateException("Original source changed while checking pixels");
        var req=new JSONObject().put("runId",run.getString("id")).put("geometryId",d.getString("geometryId"));if(d.has("profileId"))req.put("profileId",d.getString("profileId"));
        var view=metrics.view(req);view.remove("nativeStale");
        var rows=new JSONArray();for(Object raw:view.getJSONArray("rows")){var row=(JSONObject)raw;if(row.getString("bodyId").equals(body))rows.put(WorldFrames.copy(row));}
        if(rows.length()!=frames.length()-1)throw new IllegalStateException("Missing or duplicate bound-body metric rows");
        return new JSONObject().put("runId",run.getString("id")).put("trackerPolicy",run.optString("trackerPolicy",ObservationMethods129.DEFAULT)).put("runSha256",WorldFrames.hash(run)).put("datasetId",dataset.getString("id")).put("datasetSha256",WorldFrames.hash(dataset)).put("metricSha256",WorldFrames.hash(view)).put("nativeBinding",WorldFrames.copy(binding)).put("bodyId",body).put("rows",rows).put("geometry",view.getJSONObject("geometry")).put("clock",view.getJSONObject("clock")).put("profile",view.get("profile")).put("uncertaintyMeaning",view.getString("uncertaintyMeaning")).put("source",dataset.getString("source")).put("license",dataset.optString("license","See original dataset terms"));
    }
    static JSONObject forecast(JSONObject source,JSONObject definition){
        if(ObservationForecast129.selected(definition))return ObservationForecast129.forecast(source,definition);
        double horizon=definition.getDouble("horizonS"),gap=definition.getDouble("maxGapS"),accel=definition.getDouble("accelerationBoundMps2");
        JSONArray original=source.getJSONArray("rows"),out=new JSONArray();JSONObject previous=null,last=null;double time=-1,errorSum=0,maxError=0;int measured=0,compared=0,covered=0,available=0;
        for(Object raw:original){var row=WorldFrames.copy((JSONObject)raw);double t=number(row,"seconds");if(t<=time)throw new IllegalArgumentException("Observation times must strictly increase");time=t;
            var p=predict(previous,last,t,gap,horizon,accel);row.put("forecast",p);if(p.getBoolean("available"))available++;
            boolean accepted=row.getBoolean("accepted")&&row.has("positionM")&&row.getString("status").equals("measured-estimate");
            if(row.has("positionM")){PlaneCalibration.point(row.getJSONArray("positionM"));double radius=number(row,"radiusM");if(radius<0||radius>10000)throw new IllegalArgumentException("Unbounded position uncertainty");}
            if(accepted){measured++;if(p.getBoolean("available")){double error=PlaneCalibration.distance(PlaneCalibration.point(row.getJSONArray("positionM")),PlaneCalibration.point(p.getJSONArray("positionM")));boolean ok=error<=p.getDouble("radiusM")+row.getDouble("radiusM");row.put("errorM",error).put("withinDeclaredBound",ok);compared++;if(ok)covered++;errorSum+=error;maxError=Math.max(maxError,error);}previous=last;last=row;}
            out.put(row);
        }
        if(measured<3||compared<1)throw new IllegalArgumentException("At least three accepted positions and one bounded forecast comparison required");
        return new JSONObject().put("rows",out).put("future",predict(previous,last,time+horizon,gap,horizon,accel)).put("metrics",new JSONObject().put("rows",out.length()).put("measured",measured).put("rejected",out.length()-measured).put("availableForecasts",available).put("compared",compared).put("covered",covered).put("meanErrorM",errorSum/compared).put("maxErrorM",maxError)).put("units","metres and source seconds").put("method","Causal constant velocity from the last two accepted points; rejected tracker predictions never train velocity").put("uncertainty","Engineering envelope from position radii, propagated velocity error and declared acceleration bound; not a calibrated probability or physical guarantee");
    }
    private static JSONObject predict(JSONObject previous,JSONObject last,double t,double gap,double horizon,double accel){
        var p=new JSONObject().put("available",false);if(previous==null||last==null)return p.put("reason","Need two earlier accepted measurements");
        double dt=last.getDouble("seconds")-previous.getDouble("seconds"),ahead=t-last.getDouble("seconds");
        if(dt<=0||dt>gap||ahead<=0||ahead>horizon+1e-9)return p.put("reason","Accepted-measurement gap or forecast horizon exceeded");
        double[] a=PlaneCalibration.point(previous.getJSONArray("positionM")),b=PlaneCalibration.point(last.getJSONArray("positionM"));double vx=(b[0]-a[0])/dt,vy=(b[1]-a[1])/dt;
        double radius=last.getDouble("radiusM")+ahead*(last.getDouble("radiusM")+previous.getDouble("radiusM"))/dt+.5*accel*ahead*ahead;
        double x=b[0]+vx*ahead,y=b[1]+vy*ahead;if(!Double.isFinite(radius)||radius>10000||!Double.isFinite(x)||!Double.isFinite(y))return p.put("reason","Forecast or uncertainty exceeds bound");
        return p.put("available",true).put("seconds",t).put("positionM",new JSONArray(new double[]{x,y})).put("radiusM",radius).put("fromOrdinals",new JSONArray().put(previous.getInt("ordinal")).put(last.getInt("ordinal"))).put("aheadS",ahead);
    }
}
