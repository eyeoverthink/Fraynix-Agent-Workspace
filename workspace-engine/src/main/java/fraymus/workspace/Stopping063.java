package fraymus.workspace;

import org.json.*;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

/** Declared interventions and calibration-only prediction intervals; no controller authority. */
final class Stopping063 {
    static final String PROTOCOL="stopping-uncertainty-063-v1";
    static final List<String> VARIANTS=List.of("baseline","rotationLocked","iterations80");
    static final List<String> METHODS=List.of("fixed20mm","pooled","regime");
    static final int TOTAL=104;
    private static final class Identity {static final String VALUE=implementation();}
    static String source(){return Identity.VALUE;}
    private static String implementation(){
        var text=new StringBuilder(PROTOCOL).append(ResidualOutcome062.source());var classes=new ArrayDeque<Class<?>>();classes.add(Stopping063.class);classes.add(StoppingRamp063.class);
        while(!classes.isEmpty()){var c=classes.remove();try(var in=c.getResourceAsStream(c.getName().substring(c.getPackageName().length()+1)+".class")){text.append(MapDefinition.sha(in.readAllBytes()));}catch(Exception e){throw new IllegalStateException(e);}Arrays.stream(c.getDeclaredClasses()).sorted(Comparator.comparing(Class::getName)).forEach(classes::add);}return MeasurementStore.hash(text.toString());
    }
    static JSONObject definition(double a,double ratio,double push){return new JSONObject().put("angleDeg",a).put("frictionRatio",ratio).put("pushMps",push).put("massKg",1).put("gravityMps2",9.81);}
    static JSONArray anchors(){var a=new JSONArray();for(double angle:new double[]{26,30})for(double v:new double[]{.10,.25,.40})a.put(definition(angle,1.05,v));return a;}
    static JSONArray cases(String partition){
        if(!Set.of("calibration","reserved").contains(partition))throw new IllegalArgumentException("Unknown uncertainty partition");
        var random=new Random(partition.equals("calibration")?63001:63002);var a=new JSONArray();
        for(int regime=0;regime<2;regime++)for(int i=0;i<20;i++){
            double angle=12+20*random.nextDouble(),push=.05+.35*random.nextDouble(),ratio=regime==0?.85+.14*random.nextDouble():1.01+.19*random.nextDouble();a.put(definition(angle,ratio,push));
        }return a;
    }
    static JSONObject manifest(){return new JSONObject().put("protocol",PROTOCOL).put("source",source()).put("anchors",anchors()).put("variants",new JSONArray(VARIANTS)).put("calibration",cases("calibration")).put("reserved",cases("reserved"))
        .put("nominalCoverage",.9).put("minimumCoverage",.9).put("minimumRegimeCoverage",.85).put("maximumMeanWidthM",.08).put("diagnosticTicks",720).put("uncertaintyTicks",180).put("dt",BulletRamp.DT).put("controllerAuthority",false);}
    static String regime(JSONObject c){return c.getDouble("frictionRatio")<1?"accelerating":"slowing";}
    static void queryDomain(JSONObject c){ResidualOutcome062.physical(c);double a=c.getDouble("angleDeg"),v=c.getDouble("pushMps"),ratio=c.getDouble("frictionRatio");if(a<12||a>=32||v<.05||v>=.4||!((ratio>=.85&&ratio<.99)||(ratio>1.01&&ratio<1.20)))throw new IllegalArgumentException("Outside calibrated domain: angle12–32, push.05–.40, balance.85–.99 or1.01–1.20; upper bounds/gap excluded");}
    static double analytic(JSONObject c,double t){ResidualOutcome062.physical(c);double a=9.81*Math.sin(Math.toRadians(c.getDouble("angleDeg")))*(1-c.getDouble("frictionRatio")),v=c.getDouble("pushMps");if(a<0)t=Math.min(t,-v/a);return v*t+.5*a*t*t;}
    static double analyticStop(JSONObject c){double a=9.81*Math.sin(Math.toRadians(c.getDouble("angleDeg")))*(1-c.getDouble("frictionRatio"));return a<0?-c.getDouble("pushMps")/a:Double.POSITIVE_INFINITY;}
    static JSONObject projected(JSONObject c,JSONObject s){
        var config=RampDefinition.from(ResidualOutcome062.physical(c));double angle=config.radians(),co=Math.cos(angle),si=Math.sin(angle);double[] start=config.start(),p=SpatialScene.vector(s.getJSONArray("position"),100),v=SpatialScene.vector(s.getJSONArray("velocity"),100),w=SpatialScene.vector(s.getJSONArray("angularVelocity"),100);
        WorldVerify.quaternion(s.getJSONArray("rotation"));var q=s.getJSONArray("rotation");double norm=0;for(int i=0;i<4;i++)norm+=q.getDouble(i)*q.getDouble(i);double dot=(q.getDouble(2)*-Math.sin(angle/2)+q.getDouble(3)*Math.cos(angle/2))/Math.sqrt(norm);
        int near=0,touch=0;double min=Double.POSITIVE_INFINITY;for(Object x:s.getJSONArray("contactSeparationsM")){if(!(x instanceof Number))throw new IllegalStateException("Non-numeric contact separation");double z=((Number)x).doubleValue();if(!Double.isFinite(z)||Math.abs(z)>100)throw new IllegalStateException("Invalid contact separation");if(z<=.03)near++;if(z<=0)touch++;min=Math.min(min,z);}
        double speed=Math.sqrt(v[0]*v[0]+v[1]*v[1]+v[2]*v[2]),angular=Math.sqrt(w[0]*w[0]+w[1]*w[1]+w[2]*w[2]);
        return new JSONObject().put("tick",s.getInt("tick")).put("timeS",s.getDouble("timeS")).put("distanceM",(p[0]-start[0])*co-(p[1]-start[1])*si).put("tangentMps",v[0]*co-v[1]*si).put("normalMps",v[0]*si+v[1]*co).put("speedMps",speed).put("angularSpeedRadps",angular).put("rotationDeg",Math.toDegrees(2*Math.acos(Math.min(1,Math.abs(dot)))))
            .put("nearPoints",near).put("touchPoints",touch).put("minimumSeparationM",Double.isFinite(min)?min:JSONObject.NULL);
    }
    static JSONObject summary(JSONObject record){
        var c=record.getJSONObject("definition");var samples=record.getJSONArray("samples");int firstTouch=-1,quiet=-1,firstRest=-1;double maxRotation=0,maxAngular=0;JSONObject end=null,endpoint=null;
        for(Object raw:samples){var s=projected(c,(JSONObject)raw);int t=s.getInt("tick");if(firstTouch<0&&s.getInt("touchPoints")>0)firstTouch=t;maxRotation=Math.max(maxRotation,s.getDouble("rotationDeg"));maxAngular=Math.max(maxAngular,s.getDouble("angularSpeedRadps"));
            if(s.getDouble("speedMps")<=.005&&s.getDouble("angularSpeedRadps")<=.02){if(quiet<0)quiet=t;if(t-quiet>=24&&firstRest<0)firstRest=quiet;}else quiet=-1;
            if(t==180)endpoint=s;end=s;
        }
        if(endpoint==null)throw new IllegalStateException("Missing prediction horizon");double stop=analyticStop(c);
        return new JSONObject().put("firstTouchTick",firstTouch<0?JSONObject.NULL:firstTouch).put("firstQuietWindowStartTick",firstRest<0?JSONObject.NULL:firstRest).put("finalQuiet100ms",quiet>=0&&end.getInt("tick")-quiet>=24)
            .put("analyticStopS",Double.isFinite(stop)?stop:JSONObject.NULL).put("maximumRotationDeg",maxRotation).put("maximumAngularSpeedRadps",maxAngular).put("endpointM",endpoint.getDouble("distanceM")).put("endpointErrorM",endpoint.getDouble("distanceM")-analytic(c,.75))
            .put("finalDistanceM",end.getDouble("distanceM")).put("finalErrorM",end.getDouble("distanceM")-analytic(c,end.getDouble("timeS"))).put("finalTangentMps",end.getDouble("tangentMps")).put("finalAngularSpeedRadps",end.getDouble("angularSpeedRadps"));
    }
    static JSONObject trial(JSONObject c,String variant,int ticks,int stride,Path dir,Runnable cancel)throws Exception{
        ResidualOutcome062.physical(c);if(!VARIANTS.contains(variant))throw new IllegalArgumentException("Unknown intervention");
        try(var world=new StoppingRamp063(c,variant,dir)){
            var samples=new JSONArray().put(world.sample());for(int t=1;t<=ticks;t++){cancel.run();world.step();if(t%stride==0)samples.put(world.sample());}
            var r=new JSONObject().put("id",UUID.randomUUID().toString()).put("definition",c).put("variant",variant).put("ticks",ticks).put("stride",stride).put("source",source()).put("physicalParameters",RampDefinition.from(ResidualOutcome062.physical(c)).json()).put("settings",world.settings()).put("observedAt",Instant.now().toString()).put("samples",samples);
            r.put("summary",summary(r));r.put("sha256",WorldFrames.hash(r));verify(r);return r;
        }
    }
    static void verify(JSONObject r){
        var copy=WorldFrames.copy(r);copy.remove("sha256");if(!source().equals(r.getString("source"))||!WorldFrames.hash(copy).equals(r.getString("sha256")))throw new IllegalStateException("Stopping source or record changed");UUID.fromString(r.getString("id"));Instant.parse(r.getString("observedAt"));
        var c=r.getJSONObject("definition");if(!RampDefinition.from(ResidualOutcome062.physical(c)).json().similar(r.getJSONObject("physicalParameters")))throw new IllegalStateException("Stopping parameters changed");
        int ticks=r.getInt("ticks"),stride=r.getInt("stride");if(!((ticks==720&&stride==1)||(ticks==180&&stride==4)))throw new IllegalStateException("Unsupported trace clock");
        String variant=r.getString("variant");if(!VARIANTS.contains(variant))throw new IllegalStateException("Unsupported trace variant");var settings=r.getJSONObject("settings");
        if(!BulletRamp.BACKEND.equals(settings.getString("backend"))||!settings.getJSONArray("angularFactor").similar(new JSONArray(variant.equals("rotationLocked")?new int[]{0,0,0}:new int[]{1,1,1}))||settings.getInt("iterations")!=(variant.equals("iterations80")?80:10))throw new IllegalStateException("Native settings differ");
        var samples=r.getJSONArray("samples");if(samples.length()!=ticks/stride+1)throw new IllegalStateException("Incomplete trace");for(int i=0;i<samples.length();i++){var s=samples.getJSONObject(i);if(s.getInt("tick")!=i*stride||Math.abs(s.getDouble("timeS")-i*stride*BulletRamp.DT)>1e-12)throw new IllegalStateException("Trace clock changed");projected(c,s);}
        if(!summary(r).similar(r.getJSONObject("summary")))throw new IllegalStateException("Derived stopping summary differs");
    }
    static double quantile(double[] errors){if(errors.length<9)throw new IllegalArgumentException("Too few calibration observations");double[] sorted=errors.clone();for(double e:sorted)if(!Double.isFinite(e)||e<0)throw new IllegalArgumentException("Invalid calibration error");Arrays.sort(sorted);int rank=(int)Math.ceil((sorted.length+1)*.9);return sorted[rank-1];}
    static JSONObject calibrate(JSONArray rows){
        if(rows.length()!=40)throw new IllegalStateException("Need40calibration observations");var groups=new LinkedHashMap<String,List<Double>>();for(String g:List.of("pooled","accelerating","slowing"))groups.put(g,new ArrayList<>());
        for(int i=0;i<40;i++){var row=rows.getJSONObject(i);if(!cases("calibration").getJSONObject(i).similar(row.getJSONObject("definition")))throw new IllegalStateException("Calibration partition differs");var r=row.getJSONObject("record");verify(r);double error=Math.abs(r.getJSONObject("summary").getDouble("endpointM")-analytic(row.getJSONObject("definition"),.75));groups.get("pooled").add(error);groups.get(regime(row.getJSONObject("definition"))).add(error);}
        var radii=new JSONObject();for(var e:groups.entrySet())radii.put(e.getKey(),quantile(e.getValue().stream().mapToDouble(Double::doubleValue).toArray()));return radii;
    }
    static JSONObject intervals(JSONObject radii,JSONObject c){queryDomain(c);double center=analytic(c,.75);var out=new JSONObject();for(String method:METHODS){double radius=method.equals("fixed20mm")?.02:radii.getDouble(method.equals("regime")?regime(c):"pooled");if(!Double.isFinite(radius)||radius<0)throw new IllegalArgumentException("Invalid radius");out.put(method,new JSONObject().put("predictionM",center).put("lowerM",center-radius).put("upperM",center+radius).put("radiusM",radius));}return out;}
    static JSONObject evaluate(JSONArray rows){
        if(rows.length()!=40)throw new IllegalStateException("Need40reserved observations");var result=new JSONObject();
        for(String method:METHODS){int hits=0;double widths=0,maxWidth=0;int[] counts=new int[2],covered=new int[2];var misses=new JSONArray();
            for(int i=0;i<rows.length();i++){var row=rows.getJSONObject(i);double actual=row.getJSONObject("record").getJSONObject("summary").getDouble("endpointM");var interval=row.getJSONObject("intervalsBeforeObservation").getJSONObject(method);boolean hit=actual>=interval.getDouble("lowerM")&&actual<=interval.getDouble("upperM");int group=regime(row.getJSONObject("definition")).equals("accelerating")?0:1;counts[group]++;if(hit){hits++;covered[group]++;}else misses.put(i+1);double width=interval.getDouble("upperM")-interval.getDouble("lowerM");widths+=width;maxWidth=Math.max(maxWidth,width);}
            var groups=new JSONObject();for(int i=0;i<2;i++){if(counts[i]!=20)throw new IllegalStateException("Reserved regimes incomplete");groups.put(i==0?"accelerating":"slowing",covered[i]/20.0);}
            result.put(method,new JSONObject().put("covered",hits).put("count",40).put("coverage",hits/40.0).put("regimeCoverage",groups).put("meanWidthM",widths/40).put("maxWidthM",maxWidth).put("missedCases",misses));
        }var p=result.getJSONObject("regime");boolean qualified=p.getDouble("coverage")>=.9&&p.getJSONObject("regimeCoverage").getDouble("accelerating")>=.85&&p.getJSONObject("regimeCoverage").getDouble("slowing")>=.85&&p.getDouble("meanWidthM")<=.08;
        return new JSONObject().put("methods",result).put("proposed","regime").put("qualified",qualified).put("controllerAuthority",false).put("interpretation","Empirical simulation coverage on40reserved cases; no physical coverage guarantee");
    }
    static void run(JSONObject job,Path dir,Runnable cancel,FraynixOutcome061.Progress progress)throws Exception{
        job.put("manifest063",manifest()).put("diagnostics063",new JSONArray()).put("calibration063",new JSONArray()).put("reserved063",new JSONArray()).put("progress",0).put("total",TOTAL);progress.save("Stopping study declared");
        for(Object raw:anchors())for(String variant:VARIANTS){var c=(JSONObject)raw;cancel.run();var row=new JSONObject().put("definition",c).put("variant",variant).put("predictedAt",Instant.now().toString()).put("analyticalEndpointM",analytic(c,.75)).put("analyticalFinalM",analytic(c,3));job.getJSONArray("diagnostics063").put(row);progress.save("Recording "+variant+" stopping trace");
            row.put("record",trial(c,variant,720,1,dir,cancel));job.put("progress",job.getInt("progress")+1);
            if(variant.equals("baseline")){var legacy=ResidualOutcome062.trial(c,dir,cancel);row.put("original062",legacy);double difference=Math.abs(legacy.getJSONObject("final").getDouble("distanceM")-row.getJSONObject("record").getJSONObject("summary").getDouble("endpointM"));row.put("parityErrorM",difference);if(difference>1e-6)throw new IllegalStateException("Instrumented baseline differs from original runtime");job.put("progress",job.getInt("progress")+1);}progress.save("Stopping trace retained");}
        for(String phase:List.of("calibration","reserved")){
            if(phase.equals("reserved")){var model=new JSONObject().put("radiiM",calibrate(job.getJSONArray("calibration063"))).put("calibrationSha256",WorldFrames.hash(new JSONObject().put("rows",job.getJSONArray("calibration063")))).put("pinnedAt",Instant.now().toString());job.put("intervalModel063",model).put("intervalModelSha256063",WorldFrames.hash(model));progress.save("Calibration frozen before reserved observations");}
            for(Object raw:cases(phase)){cancel.run();var c=(JSONObject)raw;var row=new JSONObject().put("definition",c).put("predictedAt",Instant.now().toString()).put("analyticalEndpointM",analytic(c,.75));if(phase.equals("reserved"))row.put("intervalsBeforeObservation",intervals(job.getJSONObject("intervalModel063").getJSONObject("radiiM"),c));job.getJSONArray(phase+"063").put(row);progress.save(phase+" prediction saved before observation");row.put("record",trial(c,"baseline",180,4,dir,cancel));job.put("progress",job.getInt("progress")+1);progress.save(phase+" observation retained");}
        }var evaluation=evaluate(job.getJSONArray("reserved063"));job.put("evaluation063",evaluation).put("accepted",evaluation.getBoolean("qualified")).put("reason",evaluation.getBoolean("qualified")?"Uncertainty passed the declared empirical coverage/width gate; advisory only":"Uncertainty did not pass the declared empirical gate; observations retained");audit(job);progress.save("Stopping and uncertainty checks complete");
    }
    static void audit(JSONObject job){
        if(!job.getJSONObject("manifest063").similar(manifest()))throw new IllegalStateException("Stopping manifest differs");var ids=new HashSet<String>();var diagnostic=job.getJSONArray("diagnostics063");if(diagnostic.length()!=18)throw new IllegalStateException("Missing intervention");
        for(int i=0;i<18;i++){var row=diagnostic.getJSONObject(i);var r=row.getJSONObject("record");checkRow(row,r,anchors().getJSONObject(i/3),VARIANTS.get(i%3),720,1,ids);if(row.getDouble("analyticalFinalM")!=analytic(row.getJSONObject("definition"),3)||!row.getString("variant").equals(VARIANTS.get(i%3)))throw new IllegalStateException("Intervention prediction differs");if(i%3==0){var old=row.getJSONObject("original062");ResidualOutcome062.verify(old);if(!ids.add(old.getString("id"))||!old.getJSONObject("definition").similar(row.getJSONObject("definition")))throw new IllegalStateException("Original parity identity differs");double delta=Math.abs(old.getJSONObject("final").getDouble("distanceM")-r.getJSONObject("summary").getDouble("endpointM"));if(delta>1e-6||delta!=row.getDouble("parityErrorM"))throw new IllegalStateException("Original parity differs");}}
        var model=job.getJSONObject("intervalModel063");if(!WorldFrames.hash(model).equals(job.getString("intervalModelSha256063"))||!calibrate(job.getJSONArray("calibration063")).similar(model.getJSONObject("radiiM"))||!WorldFrames.hash(new JSONObject().put("rows",job.getJSONArray("calibration063"))).equals(model.getString("calibrationSha256")))throw new IllegalStateException("Calibration changed");Instant pinned=Instant.parse(model.getString("pinnedAt"));
        for(String phase:List.of("calibration","reserved")){var rows=job.getJSONArray(phase+"063");if(rows.length()!=40)throw new IllegalStateException("Uncertainty partition incomplete");for(int i=0;i<40;i++){var row=rows.getJSONObject(i);var r=row.getJSONObject("record");checkRow(row,r,cases(phase).getJSONObject(i),"baseline",180,4,ids);if(phase.equals("reserved")){if(Instant.parse(row.getString("predictedAt")).isBefore(pinned)||!intervals(model.getJSONObject("radiiM"),row.getJSONObject("definition")).similar(row.getJSONObject("intervalsBeforeObservation")))throw new IllegalStateException("Reserved interval or sequence changed");}else if(Instant.parse(r.getString("observedAt")).isAfter(pinned))throw new IllegalStateException("Calibration follows interval freeze");}}
        if(!evaluate(job.getJSONArray("reserved063")).similar(job.getJSONObject("evaluation063")))throw new IllegalStateException("Uncertainty evaluation changed");
    }
    private static void checkRow(JSONObject row,JSONObject r,JSONObject c,String variant,int ticks,int stride,Set<String> ids){verify(r);if(!ids.add(r.getString("id"))||!row.getJSONObject("definition").similar(c)||!r.getJSONObject("definition").similar(c)||!r.getString("variant").equals(variant)||r.getInt("ticks")!=ticks||r.getInt("stride")!=stride||row.getDouble("analyticalEndpointM")!=analytic(c,.75)||Instant.parse(row.getString("predictedAt")).isAfter(Instant.parse(r.getString("observedAt"))))throw new IllegalStateException("Stopping row identity, sequence or prediction differs");}
}
