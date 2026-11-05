package fraymus.workspace;
import org.json.*;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

/** Calibration is completed and frozen before clean and unfamiliar-condition evaluation. */
final class CalibrationStudy068 {
    static final String PROTOCOL="calibrated-recovery-disturbances-068-v1";
    static final int TOTAL=484;
    static final List<String> FAMILIES=List.of("brake-before","lift-before","push-after","spin-after");
    private static final class Identity {static final String VALUE=implementation();}
    static String source(){return Identity.VALUE;}
    private static String implementation(){try(var in=CalibrationStudy068.class.getResourceAsStream("CalibrationStudy068.class")){return MeasurementStore.hash(PROTOCOL+DisturbanceTrial068.source()+MapDefinition.sha(in.readAllBytes()));}catch(Exception e){throw new IllegalStateException(e);}}
    static JSONArray cases(String phase){
        long seed=switch(phase){case "calibration"->68001;case "clean"->68002;case "disturbed"->68003;case "unsupported"->68004;default->throw new IllegalArgumentException("Unknown phase");};
        var random=new Random(seed);var rows=new JSONArray();boolean outside=phase.equals("unsupported");
        for(int regime=0;regime<2;regime++)for(int i=0;i<(outside?6:24);i++){
            double angle=12+20*random.nextDouble(),ratio=regime==0?.85+.14*random.nextDouble():1.01+.19*random.nextDouble(),center=.02+.34*random.nextDouble();
            if(outside)switch(i%3){case 0->angle=10;case 1->angle=34;case 2->ratio=1;}
            rows.put(new JSONObject().put("task",new JSONObject().put("angleDeg",angle).put("frictionRatio",ratio).put("centerM",center).put("halfWidthM",new double[]{.012,.024,.045}[i%3]))
                .put("disturbance",phase.equals("disturbed")?FAMILIES.get(i%4):"none"));
        }
        return rows;
    }
    static JSONObject manifest(){var m=new JSONObject().put("protocol",PROTOCOL).put("source",source()).put("nativeSource",DisturbanceTrial068.source()).put("scenarioModel",Calibration065.info()).put("methods",new JSONArray(CalibratedPolicy068.METHODS))
        .put("parityCases",TemporalStudy067.parityCases()).put("events",new JSONArray(FAMILIES.stream().map(DisturbanceRamp068::event).toList())).put("total",TOTAL).put("lookupFee",.002).put("fitRule","occupied-bin Laplace(1,1), weighted PAV, linear interpolation, clamped edges")
        .put("cleanBrierGain",.01).put("utilityNoninferiority",.01).put("cleanMinimumSuccess",.4).put("cleanMaximumMiss",.15).put("disturbedMinimumSuccess",.25).put("disturbedMaximumMiss",.25).put("controllerAuthority",false);
        for(String phase:List.of("calibration","clean","disturbed","unsupported"))m.put(phase,cases(phase));return m;
    }
    static JSONObject calibrationMetrics(JSONArray rows){
        double rawLoss=0,calLoss=0;int n=0;double[][] rawBins=new double[5][3],calBins=new double[5][3];
        for(Object x:rows){var row=(JSONObject)x;var d=row.getJSONObject("plan").getJSONObject("decisions");if(!d.getJSONObject("value").has("screen"))continue;
            var truth=row.getJSONObject("results").getJSONObject("temporal").getJSONObject("outcome");if(!truth.getBoolean("executed"))throw new IllegalStateException("Calibration assessment requires unscreened native truth");
            double y=truth.getBoolean("success")?1:0,p=d.getJSONObject("value").getJSONObject("screen").getDouble("successFraction"),q=d.getJSONObject("calibrated").getJSONObject("calibration068").getDouble("probability");
            n++;rawLoss+=(p-y)*(p-y);calLoss+=(q-y)*(q-y);bin(rawBins,p,y);bin(calBins,q,y);
        }
        return new JSONObject().put("count",n).put("rawBrier",n==0?JSONObject.NULL:rawLoss/n).put("calibratedBrier",n==0?JSONObject.NULL:calLoss/n)
            .put("rawReliability",reliability(rawBins)).put("calibratedReliability",reliability(calBins)).put("rawECE",n==0?JSONObject.NULL:ece(rawBins,n)).put("calibratedECE",n==0?JSONObject.NULL:ece(calBins,n))
            .put("meaning","All initially Pooled-refused tasks, including declined screens; labels are independent unscreened Temporal outcomes");
    }
    private static void bin(double[][] bins,double p,double y){var b=bins[Math.min(4,(int)(p*5))];b[0]++;b[1]+=p;b[2]+=y;}
    private static JSONArray reliability(double[][] bins){var rows=new JSONArray();for(int i=0;i<5;i++){var b=bins[i];rows.put(new JSONObject().put("low",i/5.0).put("high",(i+1)/5.0).put("count",(int)b[0]).put("meanProbability",b[0]==0?JSONObject.NULL:b[1]/b[0]).put("successRate",b[0]==0?JSONObject.NULL:b[2]/b[0]));}return rows;}
    private static double ece(double[][] bins,int n){double error=0;for(var b:bins)error+=Math.abs(b[1]-b[2]);return error/n;}
    static JSONObject diagnostics(JSONArray rows){
        int avoided=0,sacrificed=0,accepted=0,disagree=0,valueLost=0,valueAvoided=0,valueRecovered=0;double rawMs=0,calMs=0;
        for(Object x:rows){var row=(JSONObject)x;var results=row.getJSONObject("results");var t=results.getJSONObject("temporal").getJSONObject("outcome");var v=results.getJSONObject("value").getJSONObject("outcome");var c=results.getJSONObject("calibrated").getJSONObject("outcome");
            if(t.getBoolean("executed")&&!c.getBoolean("executed")){if(t.getBoolean("success"))sacrificed++;else avoided++;}
            if(row.getJSONObject("plan").getJSONObject("decisions").getJSONObject("calibrated").has("screen")&&c.getBoolean("executed")){accepted++;if(c.getBoolean("success")!=t.getBoolean("success")||Math.abs(c.getDouble("endpointM")-t.getDouble("endpointM"))>1e-6)disagree++;}
            if(v.getBoolean("success")&&!c.getBoolean("success"))valueLost++;
            if(v.getBoolean("executed")&&!v.getBoolean("success")&&!c.getBoolean("executed"))valueAvoided++;
            if(!v.getBoolean("executed")&&c.getBoolean("success"))valueRecovered++;
            rawMs+=row.getJSONObject("screenTiming").getDouble("valueMillis");calMs+=row.getJSONObject("screenTiming").getDouble("calibratedMillis");
        }
        return new JSONObject().put("avoidedTemporalMisses",avoided).put("sacrificedTemporalSuccesses",sacrificed).put("acceptedScreenedAttempts",accepted).put("acceptedTemporalDisagreements",disagree)
            .put("valueSuccessesLost",valueLost).put("valueMissesAvoided",valueAvoided).put("valueRefusalsRecovered",valueRecovered).put("rawScreenMillis",rawMs).put("calibratedScreenMillis",calMs);
    }
    static JSONObject evaluation(JSONArray rows,String phase,double trainingFees){
        var e=CalibratedPolicy068.evaluate(rows,true).put("calibration",calibrationMetrics(rows)).put("diagnostics",diagnostics(rows));
        var groups=new JSONObject();var names=new ArrayList<>(List.of("accelerating","slowing","width12mm","width24mm","width45mm"));if(phase.equals("disturbed"))names.addAll(FAMILIES);
        for(String name:names){var subset=new JSONArray();for(Object raw:rows){var row=(JSONObject)raw;var t=row.getJSONObject("task");
            boolean match=switch(name){case "accelerating"->t.getDouble("frictionRatio")<1;case "slowing"->t.getDouble("frictionRatio")>1;case "width12mm"->t.getDouble("halfWidthM")==.012;case "width24mm"->t.getDouble("halfWidthM")==.024;case "width45mm"->t.getDouble("halfWidthM")==.045;default->name.equals(row.getString("disturbance"));};if(match)subset.put(row);}
            if(!subset.isEmpty())groups.put(name,CalibratedPolicy068.evaluate(subset,false).put("calibration",calibrationMetrics(subset)));
        }
        double gain=e.getJSONObject("pairedCalibratedMinus").getJSONObject("value").getDouble("meanDelta");
        return e.put("subgroups",groups).put("qualified",qualifies(e,phase)).put("phase",phase).put("controllerAuthority",false)
            .put("operationFeeBreakEvenTasks",gain>0?Math.ceil(trainingFees/gain):JSONObject.NULL).put("breakEvenScope","Assumed training operation fees only; excludes fitting compute, training miss penalties and physical risk");
    }
    static boolean qualifies(JSONObject e,String phase){
        if(!Set.of("clean","disturbed").contains(phase))return false;var c=e.getJSONObject("calibration");if(c.getInt("count")==0)return false;
        var m=e.getJSONObject("methods").getJSONObject("calibrated");boolean clean=phase.equals("clean");
        return c.getDouble("calibratedBrier")<=c.getDouble("rawBrier")-(clean?.01:0)
            &&e.getJSONObject("pairedCalibratedMinus").getJSONObject("value").getDouble("meanDelta")>=-.01
            &&m.getDouble("allTaskSuccessRate")>=(clean?.4:.25)&&m.getDouble("allTaskMissRate")<=(clean?.15:.25);
    }
    static JSONObject trainingCosts(JSONArray rows){int actions=0,observations=0,replans=0,misses=0,screens=0;double fees=0,millis=0;
        for(Object x:rows){var row=(JSONObject)x;var o=row.getJSONObject("outcome");actions+=o.getInt("actions");observations+=o.getInt("observations");replans+=o.getInt("replans");if(!o.getBoolean("success"))misses++;fees+=o.getDouble("logicalCost");millis+=o.getDouble("nativeWallMillis");if(row.has("screen"))screens++;}
        return new JSONObject().put("nativeExecutions",rows.length()).put("actions",actions).put("observations",observations).put("replans",replans).put("misses",misses).put("featureScreens",screens).put("featureScreenFees",screens*.005).put("operationFees",fees+screens*.005).put("nativeWallMillis",millis);
    }
    static void run(JSONObject job,Path dir,Runnable cancel,FraynixOutcome061.Progress progress)throws Exception{
        job.put("manifest068",manifest()).put("parity068",new JSONArray()).put("calibration068",new JSONArray()).put("clean068",new JSONArray()).put("disturbed068",new JSONArray()).put("unsupported068",new JSONArray()).put("progress",0).put("total",TOTAL).put("nativeExecutions",0);progress.save("Calibration and disturbance contract saved");
        for(Object raw:TemporalStudy067.parityCases()){cancel.run();var c=(JSONObject)raw;var old=Stopping063.trial(c,"baseline",180,4,dir,cancel);var now=DisturbanceTrial068.run(TemporalStudy067.parityTask(c),TemporalStudy067.parityDecision(c),"none",dir,cancel,partial->{});
            double delta=DisturbanceTrial068.parity(old,now);job.getJSONArray("parity068").put(new JSONObject().put("definition",c).put("original",old).put("adapter",now).put("maximumDelta",delta));job.put("nativeExecutions",job.getInt("nativeExecutions")+2);advance(job,progress,"Original runtime parity retained");if(delta>1e-6)throw new IllegalStateException("New adapter changes original motion");}
        for(Object raw:cases("calibration")){cancel.run();var task=((JSONObject)raw).getJSONObject("task");var plan=TemporalPolicy067.initial(task);var decision=plan.getJSONObject("decisions").getJSONObject("temporal");var row=new JSONObject().put("task",task).put("decision",decision).put("plannedAt",Instant.now().toString());
            var value=plan.getJSONObject("decisions").getJSONObject("value");if(value.has("screen"))row.put("screen",value.getJSONObject("screen"));job.getJSONArray("calibration068").put(row);progress.save("Calibration plan saved before native outcome");
            var record=DisturbanceTrial068.run(task,decision,"none",dir,cancel,partial->{row.put("pending",partial);progress.save("Calibration correction saved before its impulse");});row.remove("pending");row.put("record",record).put("outcome",CalibratedPolicy068.outcome(task,decision,record));job.put("nativeExecutions",job.getInt("nativeExecutions")+1);advance(job,progress,"Unscreened calibration label retained");
        }
        long at=System.nanoTime();var model=ValueCalibration068.fit(job.getJSONArray("calibration068"));job.put("fitMillis",(System.nanoTime()-at)/1e6).put("calibrationModel068",model).put("modelFrozenAt",Instant.now().toString()).put("trainingCosts068",trainingCosts(job.getJSONArray("calibration068")));progress.save("Calibration model frozen before evaluation");
        for(String phase:List.of("clean","disturbed","unsupported"))for(Object raw:cases(phase)){cancel.run();var spec=(JSONObject)raw;var task=spec.getJSONObject("task");var timing=new JSONObject();var plan=CalibratedPolicy068.initial(task,model,timing);
            var row=new JSONObject().put("task",task).put("disturbance",spec.getString("disturbance")).put("plan",plan).put("screenTiming",timing).put("plannedAt",Instant.now().toString()).put("results",new JSONObject());job.getJSONArray(phase+"068").put(row);progress.save(phase+": four initial decisions saved before outcomes");
            for(String method:CalibratedPolicy068.METHODS){cancel.run();var decision=plan.getJSONObject("decisions").getJSONObject(method);var result=new JSONObject();row.getJSONObject("results").put(method,result);JSONObject record=null;
                if(decision.getBoolean("execute")){record=DisturbanceTrial068.run(task,decision,row.getString("disturbance"),dir,cancel,partial->{result.put("pending",partial);progress.save(phase+": "+method+" correction saved before action");});result.remove("pending");result.put("record",record);job.put("nativeExecutions",job.getInt("nativeExecutions")+1);}
                result.put("outcome",CalibratedPolicy068.outcome(task,decision,record));advance(job,progress,phase+": "+method+" actual outcome and cost retained");
            }
        }
        double fees=job.getJSONObject("trainingCosts068").getDouble("operationFees");for(String phase:List.of("clean","disturbed","unsupported"))job.put(phase+"Evaluation068",evaluation(job.getJSONArray(phase+"068"),phase,fees));
        job.put("accepted",job.getJSONObject("cleanEvaluation068").getBoolean("qualified")&&job.getJSONObject("disturbedEvaluation068").getBoolean("qualified")).put("reason","Calibration and unfamiliar disturbances measured; no automatic controller promotion");audit(job);progress.save("Calibration and reserved evaluation verified");
    }
    private static void advance(JSONObject j,FraynixOutcome061.Progress p,String message)throws Exception{j.put("progress",j.getInt("progress")+1);p.save(message);}
    static void audit(JSONObject job){
        if(!manifest().similar(job.getJSONObject("manifest068")))throw new IllegalStateException("Calibration contract differs");
        var ids=new HashSet<String>();int count=0;var parity=job.getJSONArray("parity068");if(parity.length()!=4)throw new IllegalStateException("Parity incomplete");
        for(int i=0;i<4;i++){var row=parity.getJSONObject(i);var c=TemporalStudy067.parityCases().getJSONObject(i);var old=row.getJSONObject("original");var now=row.getJSONObject("adapter");double delta=DisturbanceTrial068.parity(old,now);
            if(!c.similar(row.getJSONObject("definition"))||!c.similar(old.getJSONObject("definition"))||!TemporalStudy067.parityTask(c).similar(now.getJSONObject("task"))||!TemporalStudy067.parityDecision(c).similar(now.getJSONObject("decision"))||!now.getJSONObject("disturbance").getString("family").equals("none")||delta>1e-6||delta!=row.getDouble("maximumDelta")||!ids.add(old.getString("id"))||!ids.add(now.getString("id")))throw new IllegalStateException("Parity evidence changed");count+=2;}
        var training=job.getJSONArray("calibration068");if(training.length()!=48)throw new IllegalStateException("Calibration incomplete");Instant frozen=Instant.parse(job.getString("modelFrozenAt"));
        for(int i=0;i<training.length();i++){var row=training.getJSONObject(i);var task=cases("calibration").getJSONObject(i).getJSONObject("task");var initial=TemporalPolicy067.initial(task);var decision=initial.getJSONObject("decisions").getJSONObject("temporal");var value=initial.getJSONObject("decisions").getJSONObject("value");
            if(!task.similar(row.getJSONObject("task"))||!decision.similar(row.getJSONObject("decision"))||row.has("pending")||value.has("screen")!=row.has("screen")||value.has("screen")&&!value.getJSONObject("screen").similar(row.getJSONObject("screen")))throw new IllegalStateException("Calibration plan or label source differs");
            var record=row.getJSONObject("record");verifyRecord(row,task,decision,record,"none",ids);if(Instant.parse(record.getString("observedAt")).isAfter(frozen)||!CalibratedPolicy068.outcome(task,decision,record).similar(row.getJSONObject("outcome")))throw new IllegalStateException("Calibration outcome or freeze boundary differs");count++;
        }
        var model=ValueCalibration068.fit(training);if(!model.similar(job.getJSONObject("calibrationModel068"))||!trainingCosts(training).similar(job.getJSONObject("trainingCosts068")))throw new IllegalStateException("Calibration uses changed or reserved labels");
        double fit=job.getDouble("fitMillis");if(!Double.isFinite(fit)||fit<0)throw new IllegalStateException("Invalid fitting time");double fees=job.getJSONObject("trainingCosts068").getDouble("operationFees");
        for(String phase:List.of("clean","disturbed","unsupported")){var rows=job.getJSONArray(phase+"068");var specs=cases(phase);if(rows.length()!=specs.length())throw new IllegalStateException("Evaluation set differs");
            for(int i=0;i<rows.length();i++){var row=rows.getJSONObject(i);var spec=specs.getJSONObject(i);var task=spec.getJSONObject("task");
                if(!task.similar(row.getJSONObject("task"))||!spec.getString("disturbance").equals(row.getString("disturbance"))||!CalibratedPolicy068.initial(task,model).similar(row.getJSONObject("plan"))||Instant.parse(row.getString("plannedAt")).isBefore(frozen))throw new IllegalStateException("Evaluation plan leaks data or predates frozen model");
                for(String key:List.of("valueMillis","calibratedMillis")){double ms=row.getJSONObject("screenTiming").getDouble(key);boolean used=row.getJSONObject("plan").getJSONObject("decisions").getJSONObject(key.equals("valueMillis")?"value":"calibrated").has("screen");if(!Double.isFinite(ms)||ms<0||!used&&ms!=0)throw new IllegalStateException("Invalid screening time");}
                var results=row.getJSONObject("results");if(!results.keySet().equals(new HashSet<>(CalibratedPolicy068.METHODS)))throw new IllegalStateException("Evaluation method missing");
                for(String method:CalibratedPolicy068.METHODS){var d=row.getJSONObject("plan").getJSONObject("decisions").getJSONObject(method);var result=results.getJSONObject(method);var record=result.optJSONObject("record");if(result.has("pending"))throw new IllegalStateException("Incomplete evaluation");
                    if(d.getBoolean("execute")){if(phase.equals("unsupported")||record==null)throw new IllegalStateException("Unsupported or missing execution");verifyRecord(row,task,d,record,row.getString("disturbance"),ids);count++;}else if(record!=null)throw new IllegalStateException("Refusal fabricates motion");
                    if(!CalibratedPolicy068.outcome(task,d,record).similar(result.getJSONObject("outcome")))throw new IllegalStateException("Evaluation costs/outcome changed");
                }
            }
            if(!evaluation(rows,phase,fees).similar(job.getJSONObject(phase+"Evaluation068")))throw new IllegalStateException("Evaluation metrics changed");
        }
        if(job.getInt("progress")!=TOTAL||job.getInt("total")!=TOTAL||job.getInt("nativeExecutions")!=count||job.getBoolean("accepted")!=(job.getJSONObject("cleanEvaluation068").getBoolean("qualified")&&job.getJSONObject("disturbedEvaluation068").getBoolean("qualified")))throw new IllegalStateException("Counts or qualification changed");
    }
    private static void verifyRecord(JSONObject row,JSONObject task,JSONObject decision,JSONObject record,String family,Set<String> ids){
        DisturbanceTrial068.verify(record);if(!record.getJSONObject("task").similar(task)||!record.getJSONObject("decision").similar(decision)||!record.getJSONObject("disturbance").getString("family").equals(family)||!ids.add(record.getString("id"))||Instant.parse(record.getString("startedAt")).isBefore(Instant.parse(row.getString("plannedAt"))))throw new IllegalStateException("Native source, world identity or prior plan differs");
    }
    static JSONObject summary(JSONObject job){var out=new JSONObject().put("jobId",job.getString("id")).put("status",job.getString("status")).put("nativeExecutions",job.optInt("nativeExecutions"));
        if(job.has("calibrationModel068"))out.put("model",job.getJSONObject("calibrationModel068")).put("trainingCosts",job.getJSONObject("trainingCosts068")).put("fitMillis",job.getDouble("fitMillis")).put("modelFrozenAt",job.getString("modelFrozenAt"));
        if(job.has("cleanEvaluation068"))for(String phase:List.of("clean","disturbed","unsupported"))out.put(phase,job.getJSONObject(phase+"Evaluation068"));
        if(job.has("accepted"))out.put("qualified",job.getBoolean("accepted"));return out;
    }
}
