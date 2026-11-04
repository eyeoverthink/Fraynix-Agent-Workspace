package fraymus.workspace;

import fraymus.neural.*;
import org.json.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** Analytical physics plus bounded learned residuals. All gates are advisory, never actuation authority. */
final class ResidualOutcome062 {
    static final String PROTOCOL="analytical-residual-native-062-v1";
    static final List<String> METHODS=List.of("analytical","constant","linear","cortex");
    private static final class Identity {static final String VALUE=implementation();}
    static String source(){return Identity.VALUE;}
    private static String implementation(){
        var b=new StringBuilder(PROTOCOL);var classes=new ArrayDeque<Class<?>>();
        classes.addAll(List.of(ResidualOutcome062.class,FraynixCortex061.class,OutcomeReadout061.class,FraynixOutcome061.class,BulletRamp.class,RampDefinition.class));
        while(!classes.isEmpty()){var c=classes.remove();String name=c.getName().substring(c.getPackageName().length()+1)+".class";
            try(var in=c.getResourceAsStream(name)){b.append(MapDefinition.sha(in.readAllBytes()));}catch(Exception e){throw new IllegalStateException(e);}
            Arrays.stream(c.getDeclaredClasses()).sorted(Comparator.comparing(Class::getName)).forEach(classes::add);
        }return MeasurementStore.hash(b.toString());
    }
    static JSONArray definitions(String phase){
        double[] angles,ratios,pushes;
        switch(phase){
            case "train"->{angles=new double[]{8,16,24,32};ratios=new double[]{.5,.9,1.1,1.3};pushes=new double[]{0,.4};}
            case "select"->{angles=new double[]{12,20,28};ratios=new double[]{.8,1.2};pushes=new double[]{.1,.3};}
            case "reserved"->{angles=new double[]{10,18,26,30};ratios=new double[]{.7,1.05};pushes=new double[]{.05,.25};}
            default->throw new IllegalArgumentException("Unknown residual partition");
        }
        var out=new JSONArray();for(double a:angles)for(double ratio:ratios)for(double push:pushes)out.put(new JSONObject()
            .put("angleDeg",a).put("frictionRatio",ratio).put("pushMps",push).put("massKg",1).put("gravityMps2",9.81));return out;
    }
    static JSONObject manifest(){return new JSONObject().put("protocol",PROTOCOL).put("source",source())
        .put("train",definitions("train")).put("select",definitions("select")).put("reserved",definitions("reserved"))
        .put("ticks",180).put("dt",BulletRamp.DT).put("ridge",.001).put("correctionLimitM",.05)
        .put("minimumRelativeGain",.10).put("maximumErrorM",.020).put("maximumRegimeRatio",1.10).put("tieToleranceM",1e-12)
        .put("tieOrder",new JSONArray(METHODS)).put("controllerAuthority",false);}
    static JSONObject physical(JSONObject c){
        SpatialScene.keys(c,"angleDeg","frictionRatio","pushMps","massKg","gravityMps2");
        for(String k:c.keySet())if(!(c.get(k) instanceof Number))throw new IllegalArgumentException("Residual inputs must be numeric");
        RampDefinition.range(c.getDouble("angleDeg"),8,32,"residual angle");RampDefinition.range(c.getDouble("frictionRatio"),.5,1.3,"friction balance");RampDefinition.range(c.getDouble("pushMps"),0,.4,"residual push");
        if(c.getDouble("massKg")!=1||c.getDouble("gravityMps2")!=9.81)throw new IllegalArgumentException("Residual domain is 1 kg / 9.81 m/s²");
        return new JSONObject().put("angleDeg",c.getDouble("angleDeg")).put("friction",c.getDouble("frictionRatio")*Math.tan(Math.toRadians(c.getDouble("angleDeg"))))
            .put("massKg",1).put("gravityMps2",9.81);
    }
    static double analytic(JSONObject c){return FraynixOutcome061.analytical(physical(c).put("pushMps",c.getDouble("pushMps")));}
    static double[] features(JSONObject c){
        var p=physical(c);double a=Math.toRadians(c.getDouble("angleDeg")),r=c.getDouble("frictionRatio"),v=c.getDouble("pushMps"),acc=9.81*Math.sin(a)*(1-r);
        double movingTime=acc<0?Math.min(.75,-v/acc):.75;
        return new double[]{1,c.getDouble("angleDeg")/32,r/1.3,v/.4,acc/10,movingTime/.75,r<1?1:0,(1-r)*Math.sin(a)};
    }
    static JSONObject trial(JSONObject c,Path nativeDir,Runnable cancel)throws Exception{
        var config=RampDefinition.from(physical(c));var ids=new JSONObject().put("ramp",UUID.randomUUID().toString()).put("block",UUID.randomUUID().toString()).put("ground",UUID.randomUUID().toString());
        try(var p=new BulletRamp(config,ids,nativeDir)){
            p.push(c.getDouble("pushMps"));var samples=new JSONArray().put(p.sample());
            for(int t=0;t<180;t++){cancel.run();p.step();if(p.tick()%4==0)samples.put(p.sample());}
            var r=new JSONObject().put("id",UUID.randomUUID().toString()).put("definition",c).put("physicalParameters",config.json()).put("ids",ids)
                .put("source",source()).put("backend",BulletRamp.BACKEND).put("observedAt",Instant.now()).put("samples",samples).put("final",p.sample());
            r.put("sha256",WorldFrames.hash(r));verify(r);return r;
        }
    }
    static double verify(JSONObject r){
        var copy=WorldFrames.copy(r);copy.remove("sha256");
        if(!WorldFrames.hash(copy).equals(r.getString("sha256"))||!source().equals(r.getString("source"))||!BulletRamp.BACKEND.equals(r.getString("backend")))throw new IllegalStateException("Residual observation source or bytes changed");
        UUID.fromString(r.getString("id"));var config=RampDefinition.from(physical(r.getJSONObject("definition")));
        if(!config.json().similar(r.getJSONObject("physicalParameters")))throw new IllegalStateException("Native parameter binding differs");
        var samples=r.getJSONArray("samples");if(samples.length()!=46||!samples.getJSONObject(45).similar(r.getJSONObject("final")))throw new IllegalStateException("Incomplete residual observation");
        double[] start=config.start();for(int i=0;i<46;i++){
            var s=samples.getJSONObject(i);if(s.getInt("tick")!=i*4||Math.abs(s.getDouble("timeS")-i*4*BulletRamp.DT)>1e-12)throw new IllegalStateException("Native clock differs");
            double[] pos=SpatialScene.vector(s.getJSONArray("position"),100);SpatialScene.vector(s.getJSONArray("velocity"),100);WorldVerify.quaternion(s.getJSONArray("rotation"));
            for(int k=0;k<3;k++)pos[k]=(double)(float)pos[k];
            double distance=(pos[0]-start[0])*Math.cos(config.radians())-(pos[1]-start[1])*Math.sin(config.radians());
            if(!Double.isFinite(s.getDouble("distanceM"))||Math.abs(distance-s.getDouble("distanceM"))>1e-7||s.getInt("contacts")<0)throw new IllegalStateException("Native sample consistency differs");
        }return r.getJSONObject("final").getDouble("distanceM");
    }
    static JSONObject fit(JSONArray rows)throws Exception{
        if(rows.length()!=32)throw new IllegalArgumentException("Need the complete declared training partition");
        var h=new FraynixCortex061(3,8);double[][] x=new double[32][];double[] y=new double[32];var ids=new TreeSet<String>();
        for(int i=0;i<32;i++){
            var r=rows.getJSONObject(i).getJSONObject("record");var c=r.getJSONObject("definition");if(!definitions("train").getJSONObject(i).similar(c)||!ids.add(r.getString("id")))throw new IllegalStateException("Training partition or identity changed");
            y[i]=verify(r)-analytic(c);x[i]=features(c);h.step(x[i],16);
        }
        double[][] expanded=new double[32][];for(int i=0;i<32;i++)expanded[i]=OutcomeReadout061.features(x[i],h);
        return new JSONObject().put("cortex",Base64.getEncoder().encodeToString(h.checkpoint())).put("linearWeights",new JSONArray(OutcomeReadout061.train(x,y)))
            .put("cortexWeights",new JSONArray(OutcomeReadout061.train(expanded,y))).put("meanResidualM",Arrays.stream(y).average().orElseThrow()).put("trainingIds",new JSONArray(ids));
    }
    static double clip(double v){return Math.max(-.05,Math.min(.05,v));}
    static JSONObject predictions(JSONObject model,JSONObject c)throws Exception{
        double baseline=analytic(c);var h=FraynixCortex061.restore(Base64.getDecoder().decode(model.getString("cortex")));double[] x=features(c);
        return new JSONObject().put("analytical",baseline).put("constant",baseline+clip(model.getDouble("meanResidualM")))
            .put("linear",baseline+clip(OutcomeReadout061.predict(FraynixOutcome061.array(model.getJSONArray("linearWeights")),x)))
            .put("cortex",baseline+clip(OutcomeReadout061.predict(FraynixOutcome061.array(model.getJSONArray("cortexWeights")),OutcomeReadout061.features(x,h))));
    }
    static JSONObject metrics(JSONArray rows){
        var mean=new JSONObject();var max=new JSONObject();var regimes=new JSONObject();
        for(String method:METHODS){double sum=0,maximum=0;double[] subgroup=new double[2];int[] counts=new int[2];
            for(Object raw:rows){var row=(JSONObject)raw;double error=Math.abs(verify(row.getJSONObject("record"))-row.getJSONObject("predictionBeforeObservation").getDouble(method));
                sum+=error;maximum=Math.max(maximum,error);int group=row.getJSONObject("definition").getDouble("frictionRatio")<1?0:1;subgroup[group]+=error;counts[group]++;
            }
            if(rows.isEmpty()||counts[0]==0||counts[1]==0)throw new IllegalStateException("Both contact regimes must be present");
            mean.put(method,sum/rows.length());max.put(method,maximum);regimes.put(method,new JSONObject().put("accelerating",subgroup[0]/counts[0]).put("slowing",subgroup[1]/counts[1]));
        }return new JSONObject().put("meanAbsoluteErrorM",mean).put("maxErrorM",max).put("regimeMeanErrorM",regimes).put("count",rows.length());
    }
    static JSONObject select(JSONObject metrics){
        String best="analytical";var m=metrics.getJSONObject("meanAbsoluteErrorM");
        for(String method:METHODS)if(m.getDouble(method)<m.getDouble(best)-1e-12)best=method;
        boolean accepted=!best.equals("analytical")&&m.getDouble(best)<=.9*m.getDouble("analytical")&&metrics.getJSONObject("maxErrorM").getDouble(best)<=.020;
        return new JSONObject().put("method",best).put("selectionQualified",accepted).put("selectionMetrics",metrics);
    }
    static JSONObject evaluate(JSONObject decision,JSONObject metrics){
        String chosen=decision.getString("method");var means=metrics.getJSONObject("meanAbsoluteErrorM");var regimes=metrics.getJSONObject("regimeMeanErrorM");
        boolean subgroup=true;for(String key:List.of("accelerating","slowing"))subgroup&=regimes.getJSONObject(chosen).getDouble(key)<=1.10*regimes.getJSONObject("analytical").getDouble(key);
        boolean qualified=decision.getBoolean("selectionQualified")&&means.getDouble(chosen)<=.9*means.getDouble("analytical")&&metrics.getJSONObject("maxErrorM").getDouble(chosen)<=.020&&subgroup;
        return WorldFrames.copy(metrics).put("selectedMethod",chosen).put("selectionQualified",decision.getBoolean("selectionQualified")).put("regimeGatePassed",subgroup).put("qualified",qualified).put("controllerAuthority",false);
    }
    static void train(JSONObject job,Path dir,Runnable cancel,FraynixOutcome061.Progress progress)throws Exception{
        job.put("manifest062",manifest()).put("train062",new JSONArray()).put("select062",new JSONArray()).put("reserved062",new JSONArray()).put("progress",0).put("total",60);
        progress.save("Collecting new native training residuals");
        for(Object raw:definitions("train")){
            var c=(JSONObject)raw;var row=new JSONObject().put("definition",c).put("predictionBeforeObservation",new JSONObject().put("analytical",analytic(c))).put("predictedAt",Instant.now());
            job.getJSONArray("train062").put(row);progress.save("Analytical prediction saved before training observation");
            row.put("record",trial(c,dir,cancel));job.put("progress",job.getInt("progress")+1);progress.save("Training observation "+job.getInt("progress")+" / 32");
        }
        cancel.run();var model=fit(job.getJSONArray("train062"));job.put("model062",model).put("modelSha256062",WorldFrames.hash(model));progress.save("Learned residuals frozen before method selection");
        for(String phase:List.of("select","reserved")){
            if(phase.equals("reserved")){
                var decision=select(metrics(job.getJSONArray("select062"))).put("modelSha256",job.getString("modelSha256062"))
                    .put("selectionSha256",WorldFrames.hash(new JSONObject().put("rows",job.getJSONArray("select062")))).put("pinnedAt",Instant.now());
                job.put("decision062",decision).put("decisionSha256062",WorldFrames.hash(decision));progress.save("Selected method pinned before reserved observations");
            }
            for(Object raw:definitions(phase)){
                cancel.run();var c=(JSONObject)raw;var row=new JSONObject().put("definition",c).put("predictionBeforeObservation",predictions(model,c)).put("predictedAt",Instant.now());
                job.getJSONArray(phase+"062").put(row);progress.save(phase+" prediction saved before observation");
                row.put("record",trial(c,dir,cancel));job.put("progress",job.getInt("progress")+1);progress.save("Verified "+phase+" observation "+job.getInt("progress")+" / 60");
            }
        }
        var evaluation=evaluate(job.getJSONObject("decision062"),metrics(job.getJSONArray("reserved062")));
        job.put("evaluation062",evaluation).put("accepted",evaluation.getBoolean("qualified"))
            .put("reason",evaluation.getBoolean("qualified")?"Residual predictor qualified on new reserved conditions; advisory only":"Reserved gate did not qualify the selected residual model; analytical physics retained");
        audit(job);progress.save("Residual comparison complete; original controllers unchanged");
    }
    static void audit(JSONObject job)throws Exception{
        if(!job.getJSONObject("manifest062").similar(manifest()))throw new IllegalStateException("Residual protocol changed");
        var model=job.getJSONObject("model062");if(!WorldFrames.hash(model).equals(job.getString("modelSha256062"))||!fit(job.getJSONArray("train062")).similar(model))throw new IllegalStateException("Residual model does not reconstruct from training evidence");
        var seen=new HashSet<String>();var decision=job.getJSONObject("decision062");
        if(!WorldFrames.hash(decision).equals(job.getString("decisionSha256062"))||!decision.getString("modelSha256").equals(job.getString("modelSha256062"))
            ||!decision.getString("selectionSha256").equals(WorldFrames.hash(new JSONObject().put("rows",job.getJSONArray("select062")))))throw new IllegalStateException("Pinned residual decision changed");
        var expected=select(metrics(job.getJSONArray("select062")));for(String k:expected.keySet())if(!new JSONObject().put(k,expected.get(k)).similar(new JSONObject().put(k,decision.get(k))))throw new IllegalStateException("Selected method differs from declared comparison");
        Instant pinned=Instant.parse(decision.getString("pinnedAt"));
        for(String phase:List.of("train","select","reserved")){
            var rows=job.getJSONArray(phase+"062");var defs=definitions(phase);if(rows.length()!=defs.length())throw new IllegalStateException("Residual partition incomplete");
            for(int i=0;i<rows.length();i++){
                var row=rows.getJSONObject(i);var r=row.getJSONObject("record");var c=defs.getJSONObject(i);verify(r);
                if(!seen.add(r.getString("id"))||!c.similar(r.getJSONObject("definition"))||!c.similar(row.getJSONObject("definition")))throw new IllegalStateException("Residual identity or partition changed");
                var prediction=phase.equals("train")?new JSONObject().put("analytical",analytic(c)):predictions(model,c);
                if(!prediction.similar(row.getJSONObject("predictionBeforeObservation")))throw new IllegalStateException("Saved prediction differs");
                Instant before=Instant.parse(row.getString("predictedAt")),observed=Instant.parse(r.getString("observedAt"));
                if(before.isAfter(observed)||phase.equals("reserved")&&before.isBefore(pinned)||phase.equals("select")&&observed.isAfter(pinned))throw new IllegalStateException("Experiment sequence differs");
            }
        }
        if(!evaluate(decision,metrics(job.getJSONArray("reserved062"))).similar(job.getJSONObject("evaluation062")))throw new IllegalStateException("Residual qualification changed");
    }
}
