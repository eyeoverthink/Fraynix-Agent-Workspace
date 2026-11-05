package fraymus.workspace;
import org.json.*;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

/** Calibration is completed and frozen before clean and unfamiliar-condition evaluation. */
final class AdaptiveStudy070 {
    static final String PROTOCOL="adaptive-observation-070-v1";
    static final int TOTAL=488;
    static final List<String> FAMILIES=List.of("brake","lift","push","spin","after-last");
    private static final class Identity {static final String VALUE=implementation();}
    static String source(){return Identity.VALUE;}
    private static String implementation(){try(var in=AdaptiveStudy070.class.getResourceAsStream("AdaptiveStudy070.class")){return MeasurementStore.hash(PROTOCOL+AdaptiveTrial070.source()+MapDefinition.sha(in.readAllBytes()));}catch(Exception e){throw new IllegalStateException(e);}}
    static JSONArray cases(String phase){
        long seed=switch(phase){case "clean"->70001;case "disturbed"->70002;case "unsupported"->70003;default->throw new IllegalArgumentException("Unknown phase");};
        var random=new Random(seed);var rows=new JSONArray();boolean outside=phase.equals("unsupported");
        for(int regime=0;regime<2;regime++)for(int i=0;i<(outside?6:phase.equals("disturbed")?30:24);i++){
            double angle=12+20*random.nextDouble(),ratio=regime==0?.85+.14*random.nextDouble():1.01+.19*random.nextDouble(),center=.02+.34*random.nextDouble();
            if(outside)switch(i%3){case 0->angle=10;case 1->angle=34;case 2->ratio=1;}
            String family=phase.equals("disturbed")?FAMILIES.get(i%5):"none";
            rows.put(new JSONObject().put("task",new JSONObject().put("angleDeg",angle).put("frictionRatio",ratio).put("centerM",center).put("halfWidthM",new double[]{.012,.024,.045}[i%3]))
                .put("disturbance",family).put("event",AdaptiveRamp070.event(family,phase.equals("disturbed")?(i/5)%2:0,phase.equals("disturbed")?((i/10)+regime)%2:0)));
        }return rows;
    }
    static JSONObject manifest(){var m=new JSONObject().put("protocol",PROTOCOL).put("source",source()).put("nativeSource",AdaptiveTrial070.source()).put("methods",new JSONArray(AdaptivePolicy070.METHODS))
        .put("modelSha256",FrozenCalibration069.SHA).put("modelOrigin","Published068 single-stage training; no new fit and not calibrated for later policy")
        .put("parityCases",TemporalStudy067.parityCases()).put("total",TOTAL).put("fixedTick",108).put("adaptiveTicks",new JSONArray(ObservationSchedule070.TICKS)).put("hypotheticalScenarios",ObservationSchedule070.scenarios()).put("scheduleFee",.005).put("minimumGain",.02).put("fixedParityCases",fixedParityCases()).put("laterWindow",new JSONArray(new int[]{100,104,108})).put("laterBound",.12).put("cumulativeBound",.24)
        .put("cleanSensingReduction",.25).put("cleanUtilityGain",.01).put("cleanAllowedLoss",1).put("disturbedAllowedLoss",2).put("disturbedUtilityTolerance",.03).put("controllerAuthority",false);
        for(String phase:List.of("clean","disturbed","unsupported"))m.put(phase,cases(phase));return m;
    }
    static JSONArray fixedParityCases(){var out=new JSONArray();for(double[] t:new double[][]{{14,.9,.09,.012},{30,.98,.31,.012},{14,1.05,.12,.012},{30,1.18,.25,.012}})out.put(new JSONObject().put("angleDeg",t[0]).put("frictionRatio",t[1]).put("centerM",t[2]).put("halfWidthM",t[3]));return out;}
    static double compareSamples(JSONObject a,JSONObject b,int last){
        double maximum=0;for(int i=0;i<=last;i++){
            var x=a.getJSONArray("samples").getJSONObject(i);var y=b.getJSONArray("samples").getJSONObject(i);
            for(String key:List.of("position","velocity","angularVelocity","rotation"))for(int k=0;k<x.getJSONArray(key).length();k++)maximum=Math.max(maximum,Math.abs(x.getJSONArray(key).getDouble(k)-y.getJSONArray(key).getDouble(k)));
            if(!x.similar(y))throw new IllegalStateException("Complete native prefix including contacts changed");
        }return maximum;
    }
    static double prefixParity(JSONObject a,JSONObject b){
        if(a.has("correctionPlan")!=b.has("correctionPlan")||a.has("correctionPlan")&&!a.getJSONObject("correctionPlan").similar(b.getJSONObject("correctionPlan")))throw new IllegalStateException("First plan changed");
        return compareSamples(a,b,15);
    }
    static double fixedParity(JSONObject a,JSONObject b){
        LaterTrial069.verify(a);AdaptiveTrial070.verify(b);double delta=compareSamples(a,b,45);
        for(String key:List.of("correctionPlan","laterPlan"))if(a.has(key)!=b.has(key)||a.has(key)&&!a.getJSONObject(key).similar(b.getJSONObject(key)))throw new IllegalStateException("Fixed plan changed");
        for(String key:List.of("correctionReceipt","laterReceipt")){
            if(a.has(key)!=b.has(key))throw new IllegalStateException("Fixed action changed");
            if(a.has(key))for(String part:List.of("before","after"))if(!a.getJSONObject(key).getJSONObject(part).similar(b.getJSONObject(key).getJSONObject(part)))throw new IllegalStateException("Immediate fixed pose changed");
        }return delta;
    }
    static JSONObject diagnostics(JSONArray rows){
        var out=new JSONObject();for(String base:List.of("fixed","fixedCalibrated")){
            String adaptive=base.equals("fixed")?"adaptive":"adaptiveCalibrated";int saved=0,harmed=0,actions=0,guard=0,unexpected=0,checks=0;double max=0,ms=0;
            var choices=new JSONObject().put("skip",0).put("92",0).put("116",0).put("140",0);
            for(Object raw:rows){var results=((JSONObject)raw).getJSONObject("results");var a=results.getJSONObject(base);var b=results.getJSONObject(adaptive);
                boolean ae=a.getJSONObject("outcome").getBoolean("executed"),be=b.getJSONObject("outcome").getBoolean("executed");if(ae!=be)throw new IllegalStateException("Pair execution differs");if(!ae)continue;
                var r=b.getJSONObject("record");int tick=r.getJSONObject("schedule070").getInt("selectedTick");String choice=tick<0?"skip":Integer.toString(tick);choices.put(choice,choices.getInt(choice)+1);
                if(r.has("laterPlan")){var plan=r.getJSONObject("laterPlan");checks++;if(plan.getBoolean("correct"))actions++;if(!plan.getJSONObject("state").getJSONObject("contactWindow").getBoolean("eligible"))guard++;if(plan.getJSONObject("predictionError").getBoolean("unexpected"))unexpected++;}
                boolean x=a.getJSONObject("outcome").getBoolean("success"),y=b.getJSONObject("outcome").getBoolean("success");if(!x&&y)saved++;if(x&&!y)harmed++;
                max=Math.max(max,prefixParity(a.getJSONObject("record"),r));ms+=r.getJSONObject("timing070").getDouble("laterPlannerMillis");
            }
            out.put(base,new JSONObject().put("goalsSaved",saved).put("goalsHarmed",harmed).put("laterChecks",checks).put("laterActions",actions).put("ineligibleWindows",guard).put("unexpectedMotion",unexpected).put("prefixMaximumDelta",max).put("laterPlannerMillis",ms).put("choices",choices));
        }return out;
    }
    static JSONObject evaluation(JSONArray rows,String phase){
        var e=AdaptivePolicy070.evaluate(rows,true).put("diagnostics",diagnostics(rows));var groups=new JSONObject();
        var names=new ArrayList<>(List.of("accelerating","slowing","width12mm","width24mm","width45mm"));if(phase.equals("disturbed"))names.addAll(FAMILIES);
        for(String name:names){var subset=new JSONArray();for(Object raw:rows){var row=(JSONObject)raw;var t=row.getJSONObject("task");
            boolean match=switch(name){case "accelerating"->t.getDouble("frictionRatio")<1;case "slowing"->t.getDouble("frictionRatio")>1;case "width12mm"->t.getDouble("halfWidthM")==.012;case "width24mm"->t.getDouble("halfWidthM")==.024;case "width45mm"->t.getDouble("halfWidthM")==.045;default->name.equals(row.getString("disturbance"));};if(match)subset.put(row);}
            if(!subset.isEmpty())groups.put(name,AdaptivePolicy070.evaluate(subset,false));
        }
        return e.put("subgroups",groups).put("qualified",qualifies(e,phase,"fixed")).put("screenedQualified",qualifies(e,phase,"fixedCalibrated")).put("phase",phase).put("controllerAuthority",false);
    }
    static boolean qualifies(JSONObject e,String phase,String base){
        if(!Set.of("clean","disturbed").contains(phase))return false;
        var before=e.getJSONObject("methods").getJSONObject(base);var after=e.getJSONObject("methods").getJSONObject(base.equals("fixed")?"adaptive":"adaptiveCalibrated");
        double gain=e.getJSONObject("pairedAdaptiveMinus").getJSONObject(base).getDouble("meanDelta");
        if(phase.equals("clean"))return gain>=.01-1e-12&&after.getInt("successes")>=before.getInt("successes")-1&&after.getInt("extraObservations")<=.75*before.getInt("extraObservations")+1e-12;
        return gain>=-.03-1e-12&&after.getInt("successes")>=before.getInt("successes")-2;
    }
    static boolean diverse(JSONObject job,String base){int skipped=0;var ticks=new HashSet<String>();for(String phase:List.of("clean","disturbed")){
        var choices=job.getJSONObject(phase+"Evaluation070").getJSONObject("diagnostics").getJSONObject(base).getJSONObject("choices");skipped+=choices.getInt("skip");for(String tick:List.of("92","116","140"))if(choices.getInt(tick)>0)ticks.add(tick);
    }return skipped>0&&ticks.size()>=2;}
    static boolean qualifiesAll(JSONObject job,String base){String key=base.equals("fixed")?"qualified":"screenedQualified";return job.getJSONObject("cleanEvaluation070").getBoolean(key)&&job.getJSONObject("disturbedEvaluation070").getBoolean(key)&&diverse(job,base);}
    static void run(JSONObject job,Path dir,Runnable cancel,FraynixOutcome061.Progress progress)throws Exception{
        job.put("manifest070",manifest()).put("parity070",new JSONArray()).put("fixedParity070",new JSONArray()).put("clean070",new JSONArray()).put("disturbed070",new JSONArray()).put("unsupported070",new JSONArray()).put("progress",0).put("total",TOTAL).put("nativeExecutions",0);job.put("reusedModel070",FrozenCalibration069.model()).put("modelFrozenAt",Instant.now().toString());progress.save("Adaptive-observation contract and reused model saved");
        for(Object raw:TemporalStudy067.parityCases()){cancel.run();var c=(JSONObject)raw;var old=Stopping063.trial(c,"baseline",180,4,dir,cancel);var now=AdaptiveTrial070.run(TemporalStudy067.parityTask(c),TemporalStudy067.parityDecision(c),AdaptiveRamp070.event("none",0,0),dir,cancel,partial->{});
            double delta=AdaptiveTrial070.parity(old,now);job.getJSONArray("parity070").put(new JSONObject().put("definition",c).put("original",old).put("adapter",now).put("maximumDelta",delta));job.put("nativeExecutions",job.getInt("nativeExecutions")+2);advance(job,progress,"Original runtime parity retained");if(delta>1e-6)throw new IllegalStateException("New adapter changes original motion");}
        for(Object raw:fixedParityCases()){cancel.run();var task=(JSONObject)raw;
            var old=LaterTrial069.run(task,LaterPolicy069.initial(task).getJSONObject("decisions").getJSONObject("later"),LaterRamp069.event("none",0),dir,cancel,partial->{});
            var now=AdaptiveTrial070.run(task,AdaptivePolicy070.initial(task).getJSONObject("decisions").getJSONObject("fixed"),AdaptiveRamp070.event("none",0,0),dir,cancel,partial->{});
            double delta=fixedParity(old,now);job.getJSONArray("fixedParity070").put(new JSONObject().put("task",task).put("original",old).put("adapter",now).put("maximumDelta",delta));job.put("nativeExecutions",job.getInt("nativeExecutions")+2);advance(job,progress,"Edition069 fixed policy parity retained");
        }
        for(String phase:List.of("clean","disturbed","unsupported"))for(Object raw:cases(phase)){cancel.run();var spec=(JSONObject)raw;var task=spec.getJSONObject("task");var timing=new JSONObject();var plan=AdaptivePolicy070.initial(task,timing);
            var row=new JSONObject().put("task",task).put("disturbance",spec.getString("disturbance")).put("event",spec.getJSONObject("event")).put("plan",plan).put("screenTiming",timing).put("plannedAt",Instant.now().toString()).put("results",new JSONObject());job.getJSONArray(phase+"070").put(row);progress.save(phase+": four initial decisions saved before outcomes");
            for(String method:AdaptivePolicy070.METHODS){cancel.run();var decision=plan.getJSONObject("decisions").getJSONObject(method);var result=new JSONObject();row.getJSONObject("results").put(method,result);JSONObject record=null;
                if(decision.getBoolean("execute")){record=AdaptiveTrial070.run(task,decision,row.getJSONObject("event"),dir,cancel,partial->{result.put("pending",partial);progress.save(phase+": "+method+" correction saved before action");});result.remove("pending");result.put("record",record);job.put("nativeExecutions",job.getInt("nativeExecutions")+1);}
                result.put("outcome",AdaptivePolicy070.outcome(task,decision,record));advance(job,progress,phase+": "+method+" actual outcome and cost retained");
            }
        }
        for(String phase:List.of("clean","disturbed","unsupported"))job.put(phase+"Evaluation070",evaluation(job.getJSONArray(phase+"070"),phase));
        job.put("accepted",qualifiesAll(job,"fixed")).put("screenedAccepted",qualifiesAll(job,"fixedCalibrated")).put("reason","Adaptive observation costs and unfamiliar disturbances measured; no automatic controller promotion");audit(job);progress.save("Adaptive observation and reserved evaluation verified");
    }
    private static void advance(JSONObject j,FraynixOutcome061.Progress p,String message)throws Exception{j.put("progress",j.getInt("progress")+1);p.save(message);}
    static void audit(JSONObject job){
        if(!manifest().similar(job.getJSONObject("manifest070")))throw new IllegalStateException("Calibration contract differs");
        var ids=new HashSet<String>();int count=0;var parity=job.getJSONArray("parity070");if(parity.length()!=4)throw new IllegalStateException("Parity incomplete");
        for(int i=0;i<4;i++){var row=parity.getJSONObject(i);var c=TemporalStudy067.parityCases().getJSONObject(i);var old=row.getJSONObject("original");var now=row.getJSONObject("adapter");double delta=AdaptiveTrial070.parity(old,now);
            if(!c.similar(row.getJSONObject("definition"))||!c.similar(old.getJSONObject("definition"))||!TemporalStudy067.parityTask(c).similar(now.getJSONObject("task"))||!TemporalStudy067.parityDecision(c).similar(now.getJSONObject("decision"))||!now.getJSONObject("disturbance").getString("family").equals("none")||delta>1e-6||delta!=row.getDouble("maximumDelta")||!ids.add(old.getString("id"))||!ids.add(now.getString("id")))throw new IllegalStateException("Parity evidence changed");count+=2;}
        var fixed=job.getJSONArray("fixedParity070");if(fixed.length()!=4)throw new IllegalStateException("Fixed parity incomplete");
        for(int i=0;i<4;i++){var row=fixed.getJSONObject(i);var task=fixedParityCases().getJSONObject(i);var a=row.getJSONObject("original");var b=row.getJSONObject("adapter");double delta=fixedParity(a,b);
            if(!row.getJSONObject("task").similar(task)||!a.getJSONObject("task").similar(task)||!b.getJSONObject("task").similar(task)||!a.getJSONObject("decision").similar(LaterPolicy069.initial(task).getJSONObject("decisions").getJSONObject("later"))||!b.getJSONObject("decision").similar(AdaptivePolicy070.initial(task).getJSONObject("decisions").getJSONObject("fixed"))||!a.getJSONObject("disturbance").similar(LaterRamp069.event("none",0))||!b.getJSONObject("disturbance").similar(AdaptiveRamp070.event("none",0,0))||delta!=row.getDouble("maximumDelta")||!ids.add(a.getString("id"))||!ids.add(b.getString("id")))throw new IllegalStateException("Fixed parity identity changed");count+=2;
        }
        if(job.getBoolean("screenedAccepted")!=qualifiesAll(job,"fixedCalibrated"))throw new IllegalStateException("Secondary qualification changed");
        if(!FrozenCalibration069.model().similar(job.getJSONObject("reusedModel070")))throw new IllegalStateException("Frozen calibration changed");
        Instant frozen=Instant.parse(job.getString("modelFrozenAt"));
        for(String phase:List.of("clean","disturbed","unsupported")){var rows=job.getJSONArray(phase+"070");var specs=cases(phase);if(rows.length()!=specs.length())throw new IllegalStateException("Evaluation set differs");
            for(int i=0;i<rows.length();i++){var row=rows.getJSONObject(i);var spec=specs.getJSONObject(i);var task=spec.getJSONObject("task");
                if(!task.similar(row.getJSONObject("task"))||!spec.getString("disturbance").equals(row.getString("disturbance"))||!spec.getJSONObject("event").similar(row.getJSONObject("event"))||!AdaptivePolicy070.initial(task).similar(row.getJSONObject("plan"))||Instant.parse(row.getString("plannedAt")).isBefore(frozen))throw new IllegalStateException("Evaluation plan leaks data or predates frozen model");
                for(String key:List.of("valueMillis","calibratedMillis")){double ms=row.getJSONObject("screenTiming").getDouble(key);boolean used=row.getJSONObject("plan").getJSONObject("decisions").getJSONObject("fixedCalibrated").has("screen");if(!Double.isFinite(ms)||ms<0||!used&&ms!=0)throw new IllegalStateException("Invalid screening time");}
                var results=row.getJSONObject("results");if(!results.keySet().equals(new HashSet<>(AdaptivePolicy070.METHODS)))throw new IllegalStateException("Evaluation method missing");
                for(String method:AdaptivePolicy070.METHODS){var d=row.getJSONObject("plan").getJSONObject("decisions").getJSONObject(method);var result=results.getJSONObject(method);var record=result.optJSONObject("record");if(result.has("pending"))throw new IllegalStateException("Incomplete evaluation");
                    if(d.getBoolean("execute")){if(phase.equals("unsupported")||record==null)throw new IllegalStateException("Unsupported or missing execution");verifyRecord(row,task,d,record,row.getJSONObject("event"),ids);count++;}else if(record!=null)throw new IllegalStateException("Refusal fabricates motion");
                    if(!AdaptivePolicy070.outcome(task,d,record).similar(result.getJSONObject("outcome")))throw new IllegalStateException("Evaluation costs/outcome changed");
                }
            }
            if(!evaluation(rows,phase).similar(job.getJSONObject(phase+"Evaluation070")))throw new IllegalStateException("Evaluation metrics changed");
        }
        if(job.getInt("progress")!=TOTAL||job.getInt("total")!=TOTAL||job.getInt("nativeExecutions")!=count||job.getBoolean("accepted")!=(qualifiesAll(job,"fixed")))throw new IllegalStateException("Counts or qualification changed");
    }
    private static void verifyRecord(JSONObject row,JSONObject task,JSONObject decision,JSONObject record,JSONObject event,Set<String> ids){
        AdaptiveTrial070.verify(record);if(!record.getJSONObject("task").similar(task)||!record.getJSONObject("decision").similar(decision)||!record.getJSONObject("disturbance").similar(event)||!ids.add(record.getString("id"))||Instant.parse(record.getString("startedAt")).isBefore(Instant.parse(row.getString("plannedAt"))))throw new IllegalStateException("Native source, world identity or prior plan differs");
    }
    static JSONObject summary(JSONObject job){var out=new JSONObject().put("jobId",job.getString("id")).put("status",job.getString("status")).put("nativeExecutions",job.optInt("nativeExecutions"));
        if(job.has("reusedModel070"))out.put("modelSha256",job.getJSONObject("reusedModel070").getString("sha256")).put("modelFrozenAt",job.getString("modelFrozenAt")).put("modelScope","Published068 calibration, unchanged; not calibrated for two-stage recovery. No new fit.");
        if(job.has("cleanEvaluation070"))for(String phase:List.of("clean","disturbed","unsupported"))out.put(phase,job.getJSONObject(phase+"Evaluation070"));
        if(job.has("accepted"))out.put("qualified",job.getBoolean("accepted")).put("screenedQualified",job.getBoolean("screenedAccepted")).put("primaryScheduleDiversity",diverse(job,"fixed")).put("screenedScheduleDiversity",diverse(job,"fixedCalibrated"));return out;
    }
}
