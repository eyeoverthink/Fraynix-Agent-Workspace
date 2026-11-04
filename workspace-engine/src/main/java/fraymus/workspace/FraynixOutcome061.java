package fraymus.workspace;

import fraymus.neural.*;
import org.json.*;
import java.nio.file.*;
import java.util.*;

/** Existing Java coordinator owns this job. Every training label comes from retained native samples. */
final class FraynixOutcome061 {
    static final String PROTOCOL="fraynix-cortex-native-endpoint-061-v1";
    private static final class Identity {static final String VALUE=implementation();}
    static String source(){return Identity.VALUE;}
    private static String implementation(){
        var parts=new StringBuilder(PROTOCOL);var queue=new ArrayDeque<Class<?>>();
        queue.addAll(List.of(FraynixOutcome061.class,FraynixCortex061.class,OutcomeReadout061.class,BulletRamp.class,RampDefinition.class));
        while(!queue.isEmpty()){
            var c=queue.remove();try(var in=c.getResourceAsStream(c.getSimpleName().equals("Identity")?"FraynixOutcome061$Identity.class":c.getName().substring(c.getPackageName().length()+1)+".class")){
                parts.append(MapDefinition.sha(in.readAllBytes()));
            }catch(Exception e){throw new IllegalStateException("Outcome implementation unavailable",e);}
            Arrays.stream(c.getDeclaredClasses()).sorted(Comparator.comparing(Class::getName)).forEach(queue::add);
        }return MeasurementStore.hash(parts.toString());
    }
    interface Progress {void save(String phase)throws Exception;}
    static JSONArray definitions(boolean train){
        var out=new JSONArray();for(double angle:train?new double[]{10,20,30,40}:new double[]{15,25,35})
        for(double friction:train?new double[]{0,.15,.30}:new double[]{.05,.25})
        for(double push:train?new double[]{0,1}:new double[]{.25,.75})out.put(new JSONObject()
            .put("angleDeg",angle).put("friction",friction).put("pushMps",push).put("gravityMps2",9.81).put("massKg",1));
        return out;
    }
    static JSONObject manifest(){return new JSONObject().put("protocol",PROTOCOL).put("source",source())
        .put("training",definitions(true)).put("reserved",definitions(false)).put("ticks",180)
        .put("readoutRidge",.001).put("minimumGain",.05).put("maximumErrorM",.1).put("controllerAuthority",false);}
    static double[] features(JSONObject c){
        SpatialScene.keys(c,"angleDeg","friction","pushMps","gravityMps2","massKg");
        double a=c.getDouble("angleDeg"),f=c.getDouble("friction"),p=c.getDouble("pushMps");
        RampDefinition.range(a,10,40,"outcome angle");RampDefinition.range(f,0,.3,"outcome friction");RampDefinition.range(p,0,1,"outcome push");
        if(c.getDouble("gravityMps2")!=9.81||c.getDouble("massKg")!=1)throw new IllegalArgumentException("Only the evaluated 1 kg / 9.81 m/s² domain is available");
        return new double[]{1,a/40,f/.3,p,Math.sin(Math.toRadians(a)),Math.cos(Math.toRadians(a)),f*Math.cos(Math.toRadians(a)),p*p};
    }
    static double analytical(JSONObject c){double a=Math.toRadians(c.getDouble("angleDeg")),acc=9.81*(Math.sin(a)-c.getDouble("friction")*Math.cos(a)),v=c.getDouble("pushMps"),t=.75;
        if(v==0&&acc<0)return 0;if(acc<0)t=Math.min(t,-v/acc);return v*t+.5*acc*t*t;}
    static JSONObject trial(JSONObject definition,Path nativeDir,Runnable cancel)throws Exception{
        features(definition);var ids=new JSONObject().put("ramp",UUID.randomUUID().toString()).put("block",UUID.randomUUID().toString()).put("ground",UUID.randomUUID().toString());
        var params=new JSONObject(definition.toString());params.remove("pushMps");var config=RampDefinition.from(params);
        try(var p=new BulletRamp(config,ids,nativeDir)){
            p.push(definition.getDouble("pushMps"));var samples=new JSONArray().put(p.sample());
            for(int t=0;t<180;t++){cancel.run();p.step();if(p.tick()%4==0)samples.put(p.sample());}
            var r=new JSONObject().put("id",UUID.randomUUID().toString()).put("definition",definition).put("ids",ids)
                .put("source",source()).put("backend",BulletRamp.BACKEND).put("samples",samples).put("final",p.sample());
            r.put("sha256",WorldFrames.hash(r));verify(r);return r;
        }
    }
    static double verify(JSONObject r){
        var copy=WorldFrames.copy(r);copy.remove("sha256");
        if(!WorldFrames.hash(copy).equals(r.getString("sha256"))||!source().equals(r.getString("source"))||!BulletRamp.BACKEND.equals(r.getString("backend")))throw new IllegalStateException("Outcome source or bytes changed");
        var c=r.getJSONObject("definition");features(c);var params=WorldFrames.copy(c);params.remove("pushMps");var config=RampDefinition.from(params);var samples=r.getJSONArray("samples");
        if(samples.length()!=46||!samples.getJSONObject(45).similar(r.getJSONObject("final")))throw new IllegalStateException("Incomplete outcome trace");
        double[] start=config.start();for(int i=0;i<46;i++){
            var s=samples.getJSONObject(i);if(s.getInt("tick")!=i*4||Math.abs(s.getDouble("timeS")-i*4*BulletRamp.DT)>1e-12)throw new IllegalStateException("Outcome clock differs");
            double[] pos=SpatialScene.vector(s.getJSONArray("position"),100);SpatialScene.vector(s.getJSONArray("velocity"),100);
            // Bullet returns binary32. JSON's shortest Float decimal must be decoded back
            // to binary32 before comparing the original double projection of that float.
            for(int k=0;k<3;k++)pos[k]=(double)(float)pos[k];
            double d=(pos[0]-start[0])*Math.cos(config.radians())-(pos[1]-start[1])*Math.sin(config.radians());
            if(!Double.isFinite(s.getDouble("distanceM"))||Math.abs(d-s.getDouble("distanceM"))>1e-7)throw new IllegalStateException("Outcome position/distance differs");
        }
        return r.getJSONObject("final").getDouble("distanceM");
    }
    static double[] array(JSONArray a){double[] x=new double[a.length()];for(int i=0;i<x.length;i++){x[i]=a.getDouble(i);if(!Double.isFinite(x[i]))throw new IllegalArgumentException("Invalid readout");}return x;}
    static JSONObject predictions(JSONObject model,JSONObject c)throws Exception{
        var cortex=FraynixCortex061.restore(Base64.getDecoder().decode(model.getString("cortex")));
        double[] f=features(c);
        return new JSONObject().put("cortex",OutcomeReadout061.predict(array(model.getJSONArray("weights")),OutcomeReadout061.features(f,cortex)))
            .put("linear",OutcomeReadout061.predict(array(model.getJSONArray("linearWeights")),f)).put("mean",model.getDouble("mean")).put("analytical",analytical(c));
    }
    static void train(JSONObject job,Path nativeDir,Runnable cancel,Progress progress)throws Exception{
        job.put("outcomeManifest061",manifest()).put("outcomes061",new JSONArray()).put("reserved061",new JSONArray()).put("progress",0).put("total",36);
        var cortex=new FraynixCortex061(3,8);var training=definitions(true);var x=new double[24][];var y=new double[24];var seen=new HashSet<String>();
        progress.save("Collecting verified native training outcomes");
        for(int i=0;i<training.length();i++){
            var c=training.getJSONObject(i);var prior=new JSONObject().put("analytical",analytical(c));
            var r=trial(c,nativeDir,cancel);y[i]=verify(r);if(!seen.add(r.getString("id")))throw new IllegalStateException("Duplicate outcome");
            x[i]=features(c);cortex.step(x[i],16);
            job.getJSONArray("outcomes061").put(new JSONObject().put("predictionBeforeObservation",prior).put("record",r));
            job.put("cortexCheckpoint061",Base64.getEncoder().encodeToString(cortex.checkpoint())).put("progress",i+1);progress.save("Learning from native outcome "+(i+1)+" / 24");
        }
        double[][] expanded=new double[24][];for(int i=0;i<24;i++)expanded[i]=OutcomeReadout061.features(x[i],cortex);
        var model=new JSONObject().put("cortex",Base64.getEncoder().encodeToString(cortex.checkpoint()))
            .put("weights",new JSONArray(OutcomeReadout061.train(expanded,y))).put("linearWeights",new JSONArray(OutcomeReadout061.train(x,y)))
            .put("mean",Arrays.stream(y).average().orElseThrow()).put("trainedOutcomeIds",new JSONArray(seen.stream().sorted().toList()));
        job.put("outcomeModel061",model).put("modelSha256061",WorldFrames.hash(model));
        progress.save("Candidate frozen; evaluating withheld native conditions");
        double[] sum=new double[4];double max=0;String[] methods={"cortex","linear","mean","analytical"};
        for(Object raw:definitions(false)){
            cancel.run();var c=(JSONObject)raw;var before=predictions(model,c);
            // Persist the prediction before the corresponding native experiment exists.
            var row=new JSONObject().put("definition",c).put("predictionBeforeObservation",before);job.getJSONArray("reserved061").put(row);progress.save("Prediction sealed before withheld observation");
            var r=trial(c,nativeDir,cancel);double actual=verify(r);row.put("record",r);
            for(int k=0;k<4;k++)sum[k]+=Math.abs(actual-before.getDouble(methods[k]));max=Math.max(max,Math.abs(actual-before.getDouble("cortex")));
            job.put("progress",job.getInt("progress")+1);progress.save("Checking withheld native outcomes "+(job.getInt("progress")-24)+" / 12");
        }
        var errors=new JSONObject();for(int i=0;i<4;i++)errors.put(methods[i],sum[i]/12);
        boolean qualified=max<=.1&&sum[0]<=.95*sum[1]&&sum[0]<=.95*sum[2]&&sum[0]<=.95*sum[3];
        job.put("outcomeEvaluation061",new JSONObject().put("meanAbsoluteErrorM",errors).put("maxErrorM",max).put("qualified",qualified).put("evaluationCount",12))
            .put("accepted",qualified).put("reason",qualified?"Qualified advisory predictor; existing controllers and verifiers retain authority":"Candidate retained; did not beat every simpler baseline. Controllers unchanged.");
        audit(job);progress.save("Outcome comparison complete");
    }
    static void audit(JSONObject job)throws Exception{
        if(!job.getJSONObject("outcomeManifest061").similar(manifest())||!WorldFrames.hash(job.getJSONObject("outcomeModel061")).equals(job.getString("modelSha256061")))throw new IllegalStateException("Outcome model or protocol changed");
        var seen=new HashSet<String>();int t=0,e=0;var cortex=new FraynixCortex061(3,8);double[][] x=new double[24][];double[] y=new double[24];
        double[] sums=new double[4];double max=0;String[] names={"cortex","linear","mean","analytical"};
        for(String key:List.of("outcomes061","reserved061"))for(Object raw:job.getJSONArray(key)){
            var row=(JSONObject)raw;var r=row.getJSONObject("record");verify(r);if(!seen.add(r.getString("id")))throw new IllegalStateException("Duplicate outcome evidence");
            var expected=definitions(key.equals("outcomes061")).getJSONObject(key.equals("outcomes061")?t++:e++);
            if(!expected.similar(r.getJSONObject("definition")))throw new IllegalStateException("Outcome partition changed");
            if(key.equals("reserved061")&&!predictions(job.getJSONObject("outcomeModel061"),expected).similar(row.getJSONObject("predictionBeforeObservation")))throw new IllegalStateException("Frozen prediction changed");
            if(key.equals("outcomes061")){x[t-1]=features(expected);y[t-1]=verify(r);cortex.step(x[t-1],16);}
            else{double actual=verify(r);var predicted=row.getJSONObject("predictionBeforeObservation");for(int k=0;k<4;k++)sums[k]+=Math.abs(actual-predicted.getDouble(names[k]));max=Math.max(max,Math.abs(actual-predicted.getDouble("cortex")));}
        }
        if(t!=24||e!=12)throw new IllegalStateException("Incomplete outcome evaluation");
        var model=job.getJSONObject("outcomeModel061");double[][] expanded=new double[24][];for(int i=0;i<24;i++)expanded[i]=OutcomeReadout061.features(x[i],cortex);
        if(!Base64.getEncoder().encodeToString(cortex.checkpoint()).equals(model.getString("cortex"))
            ||!new JSONArray(OutcomeReadout061.train(expanded,y)).similar(model.getJSONArray("weights"))
            ||!new JSONArray(OutcomeReadout061.train(x,y)).similar(model.getJSONArray("linearWeights"))
            ||model.getDouble("mean")!=Arrays.stream(y).average().orElseThrow()
            ||!new JSONArray(job.getJSONArray("outcomes061").toList().stream().map(v->((Map<?,?>)((Map<?,?>)v).get("record")).get("id").toString()).sorted().toList()).similar(model.getJSONArray("trainedOutcomeIds")))throw new IllegalStateException("Learned state does not reconstruct from training-only evidence");
        var eval=job.getJSONObject("outcomeEvaluation061");boolean qualified=max<=.1&&sums[0]<=.95*sums[1]&&sums[0]<=.95*sums[2]&&sums[0]<=.95*sums[3];
        if(eval.getDouble("maxErrorM")!=max||eval.getBoolean("qualified")!=qualified||eval.getInt("evaluationCount")!=12)throw new IllegalStateException("Outcome evaluation changed");
        for(int k=0;k<4;k++)if(eval.getJSONObject("meanAbsoluteErrorM").getDouble(names[k])!=sums[k]/12)throw new IllegalStateException("Outcome comparison differs");
    }
}
