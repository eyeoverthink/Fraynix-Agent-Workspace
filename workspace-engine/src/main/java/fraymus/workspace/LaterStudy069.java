package fraymus.workspace;
import org.json.*;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

/** Calibration is completed and frozen before clean and unfamiliar-condition evaluation. */
final class LaterStudy069 {
    static final String PROTOCOL="bounded-later-recovery-069-v1";
    static final int TOTAL=484;
    static final List<String> FAMILIES=List.of("brake","lift","push","spin","after-second");
    private static final class Identity {static final String VALUE=implementation();}
    static String source(){return Identity.VALUE;}
    private static String implementation(){try(var in=LaterStudy069.class.getResourceAsStream("LaterStudy069.class")){return MeasurementStore.hash(PROTOCOL+LaterTrial069.source()+MapDefinition.sha(in.readAllBytes()));}catch(Exception e){throw new IllegalStateException(e);}}
    static JSONArray cases(String phase){
        long seed=switch(phase){case "clean"->69001;case "disturbed"->69002;case "unsupported"->69003;default->throw new IllegalArgumentException("Unknown phase");};
        var random=new Random(seed);var rows=new JSONArray();boolean outside=phase.equals("unsupported");
        for(int regime=0;regime<2;regime++)for(int i=0;i<(outside?6:phase.equals("disturbed")?30:24);i++){
            double angle=12+20*random.nextDouble(),ratio=regime==0?.85+.14*random.nextDouble():1.01+.19*random.nextDouble(),center=.02+.34*random.nextDouble();
            if(outside)switch(i%3){case 0->angle=10;case 1->angle=34;case 2->ratio=1;}
            String family=phase.equals("disturbed")?FAMILIES.get(i%5):"none";
            rows.put(new JSONObject().put("task",new JSONObject().put("angleDeg",angle).put("frictionRatio",ratio).put("centerM",center).put("halfWidthM",new double[]{.012,.024,.045}[i%3]))
                .put("disturbance",family).put("event",LaterRamp069.event(family,phase.equals("disturbed")?(i/5)%2:0)));
        }return rows;
    }
    static JSONObject manifest(){var m=new JSONObject().put("protocol",PROTOCOL).put("source",source()).put("nativeSource",LaterTrial069.source()).put("methods",new JSONArray(LaterPolicy069.METHODS))
        .put("modelSha256",FrozenCalibration069.SHA).put("modelOrigin","Published068 single-stage training; no new fit and not calibrated for later policy")
        .put("parityCases",TemporalStudy067.parityCases()).put("total",TOTAL).put("laterTick",108).put("laterWindow",new JSONArray(new int[]{100,104,108})).put("laterBound",.12).put("cumulativeBound",.24)
        .put("cleanUtilityTolerance",.12).put("disturbedUtilityGain",.10).put("disturbedSuccessGain",.10).put("disturbedMaximumMiss",.45).put("controllerAuthority",false);
        for(String phase:List.of("clean","disturbed","unsupported"))m.put(phase,cases(phase));return m;
    }
    static double prefixParity(JSONObject a,JSONObject b){
        double maximum=0;for(int i=0;i<=27;i++)for(String key:List.of("position","velocity","angularVelocity","rotation")){
            var x=a.getJSONArray("samples").getJSONObject(i).getJSONArray(key);var y=b.getJSONArray("samples").getJSONObject(i).getJSONArray(key);
            for(int k=0;k<x.length();k++)maximum=Math.max(maximum,Math.abs(x.getDouble(k)-y.getDouble(k)));}
        if(a.has("correctionPlan")!=b.has("correctionPlan")||a.has("correctionPlan")&&!a.getJSONObject("correctionPlan").similar(b.getJSONObject("correctionPlan")))throw new IllegalStateException("First plan changed");
        return maximum;
    }
    static JSONObject diagnostics(JSONArray rows){
        var out=new JSONObject();
        for(String base:List.of("single","calibrated")){
            String later=base.equals("single")?"later":"laterCalibrated";int saved=0,harmed=0,actions=0,guard=0,unexpected=0,checks=0;double max=0,ms=0;
            for(Object raw:rows){var results=((JSONObject)raw).getJSONObject("results");var a=results.getJSONObject(base);var b=results.getJSONObject(later);
                boolean ae=a.getJSONObject("outcome").getBoolean("executed"),be=b.getJSONObject("outcome").getBoolean("executed");
                if(ae!=be)throw new IllegalStateException("Execution decision differs within paired policies");
                if(!ae)continue;
                var r=b.getJSONObject("record");var plan=r.getJSONObject("laterPlan");checks++;if(plan.getBoolean("correct"))actions++;
                if(!plan.getJSONObject("state").getJSONObject("contactWindow").getBoolean("eligible"))guard++;
                if(plan.getJSONObject("predictionError").getBoolean("unexpected"))unexpected++;
                boolean x=a.getJSONObject("outcome").getBoolean("success"),y=b.getJSONObject("outcome").getBoolean("success");if(!x&&y)saved++;if(x&&!y)harmed++;
                max=Math.max(max,prefixParity(a.getJSONObject("record"),r));ms+=r.getJSONObject("timing069").getDouble("laterPlannerMillis");
            }
            if(max>1e-6)throw new IllegalStateException("Paired prefix changed before later action");
            out.put(base,new JSONObject().put("goalsSaved",saved).put("goalsHarmed",harmed).put("laterChecks",checks).put("laterActions",actions).put("ineligibleWindows",guard).put("unexpectedMotion",unexpected).put("prefixMaximumDelta",max).put("laterPlannerMillis",ms));
        }return out;
    }
    static JSONObject evaluation(JSONArray rows,String phase){
        var e=LaterPolicy069.evaluate(rows,true).put("diagnostics",diagnostics(rows));var groups=new JSONObject();
        var names=new ArrayList<>(List.of("accelerating","slowing","width12mm","width24mm","width45mm"));if(phase.equals("disturbed"))names.addAll(FAMILIES);
        for(String name:names){var subset=new JSONArray();for(Object raw:rows){var row=(JSONObject)raw;var t=row.getJSONObject("task");
            boolean match=switch(name){case "accelerating"->t.getDouble("frictionRatio")<1;case "slowing"->t.getDouble("frictionRatio")>1;case "width12mm"->t.getDouble("halfWidthM")==.012;case "width24mm"->t.getDouble("halfWidthM")==.024;case "width45mm"->t.getDouble("halfWidthM")==.045;default->name.equals(row.getString("disturbance"));};if(match)subset.put(row);}
            if(!subset.isEmpty())groups.put(name,LaterPolicy069.evaluate(subset,false));
        }
        return e.put("subgroups",groups).put("qualified",qualifies(e,phase,"single")).put("screenedQualified",qualifies(e,phase,"calibrated")).put("phase",phase).put("controllerAuthority",false);
    }
    static boolean qualifies(JSONObject e,String phase,String base){
        if(!Set.of("clean","disturbed").contains(phase))return false;
        var before=e.getJSONObject("methods").getJSONObject(base);var after=e.getJSONObject("methods").getJSONObject(base.equals("single")?"later":"laterCalibrated");
        double gain=e.getJSONObject("pairedLaterMinus").getJSONObject(base).getDouble("meanDelta");
        if(phase.equals("clean"))return gain>=-.12-1e-12&&after.getInt("successes")>=before.getInt("successes");
        return gain>=.10-1e-12&&after.getDouble("allTaskSuccessRate")-before.getDouble("allTaskSuccessRate")>=.10-1e-12&&after.getDouble("allTaskMissRate")<=.45+1e-12;
    }
    static void run(JSONObject job,Path dir,Runnable cancel,FraynixOutcome061.Progress progress)throws Exception{
        job.put("manifest069",manifest()).put("parity069",new JSONArray()).put("clean069",new JSONArray()).put("disturbed069",new JSONArray()).put("unsupported069",new JSONArray()).put("progress",0).put("total",TOTAL).put("nativeExecutions",0);job.put("reusedModel069",FrozenCalibration069.model()).put("modelFrozenAt",Instant.now().toString());progress.save("Later-observation contract and reused model saved");
        for(Object raw:TemporalStudy067.parityCases()){cancel.run();var c=(JSONObject)raw;var old=Stopping063.trial(c,"baseline",180,4,dir,cancel);var now=LaterTrial069.run(TemporalStudy067.parityTask(c),TemporalStudy067.parityDecision(c),LaterRamp069.event("none",0),dir,cancel,partial->{});
            double delta=LaterTrial069.parity(old,now);job.getJSONArray("parity069").put(new JSONObject().put("definition",c).put("original",old).put("adapter",now).put("maximumDelta",delta));job.put("nativeExecutions",job.getInt("nativeExecutions")+2);advance(job,progress,"Original runtime parity retained");if(delta>1e-6)throw new IllegalStateException("New adapter changes original motion");}
        for(String phase:List.of("clean","disturbed","unsupported"))for(Object raw:cases(phase)){cancel.run();var spec=(JSONObject)raw;var task=spec.getJSONObject("task");var timing=new JSONObject();var plan=LaterPolicy069.initial(task,timing);
            var row=new JSONObject().put("task",task).put("disturbance",spec.getString("disturbance")).put("event",spec.getJSONObject("event")).put("plan",plan).put("screenTiming",timing).put("plannedAt",Instant.now().toString()).put("results",new JSONObject());job.getJSONArray(phase+"069").put(row);progress.save(phase+": four initial decisions saved before outcomes");
            for(String method:LaterPolicy069.METHODS){cancel.run();var decision=plan.getJSONObject("decisions").getJSONObject(method);var result=new JSONObject();row.getJSONObject("results").put(method,result);JSONObject record=null;
                if(decision.getBoolean("execute")){record=LaterTrial069.run(task,decision,row.getJSONObject("event"),dir,cancel,partial->{result.put("pending",partial);progress.save(phase+": "+method+" correction saved before action");});result.remove("pending");result.put("record",record);job.put("nativeExecutions",job.getInt("nativeExecutions")+1);}
                result.put("outcome",LaterPolicy069.outcome(task,decision,record));advance(job,progress,phase+": "+method+" actual outcome and cost retained");
            }
        }
        for(String phase:List.of("clean","disturbed","unsupported"))job.put(phase+"Evaluation069",evaluation(job.getJSONArray(phase+"069"),phase));
        job.put("accepted",job.getJSONObject("cleanEvaluation069").getBoolean("qualified")&&job.getJSONObject("disturbedEvaluation069").getBoolean("qualified")).put("reason","Bounded later observation and unfamiliar disturbances measured; no automatic controller promotion");audit(job);progress.save("Later observation and reserved evaluation verified");
    }
    private static void advance(JSONObject j,FraynixOutcome061.Progress p,String message)throws Exception{j.put("progress",j.getInt("progress")+1);p.save(message);}
    static void audit(JSONObject job){
        if(!manifest().similar(job.getJSONObject("manifest069")))throw new IllegalStateException("Calibration contract differs");
        var ids=new HashSet<String>();int count=0;var parity=job.getJSONArray("parity069");if(parity.length()!=4)throw new IllegalStateException("Parity incomplete");
        for(int i=0;i<4;i++){var row=parity.getJSONObject(i);var c=TemporalStudy067.parityCases().getJSONObject(i);var old=row.getJSONObject("original");var now=row.getJSONObject("adapter");double delta=LaterTrial069.parity(old,now);
            if(!c.similar(row.getJSONObject("definition"))||!c.similar(old.getJSONObject("definition"))||!TemporalStudy067.parityTask(c).similar(now.getJSONObject("task"))||!TemporalStudy067.parityDecision(c).similar(now.getJSONObject("decision"))||!now.getJSONObject("disturbance").getString("family").equals("none")||delta>1e-6||delta!=row.getDouble("maximumDelta")||!ids.add(old.getString("id"))||!ids.add(now.getString("id")))throw new IllegalStateException("Parity evidence changed");count+=2;}
        if(!FrozenCalibration069.model().similar(job.getJSONObject("reusedModel069")))throw new IllegalStateException("Frozen calibration changed");
        Instant frozen=Instant.parse(job.getString("modelFrozenAt"));
        for(String phase:List.of("clean","disturbed","unsupported")){var rows=job.getJSONArray(phase+"069");var specs=cases(phase);if(rows.length()!=specs.length())throw new IllegalStateException("Evaluation set differs");
            for(int i=0;i<rows.length();i++){var row=rows.getJSONObject(i);var spec=specs.getJSONObject(i);var task=spec.getJSONObject("task");
                if(!task.similar(row.getJSONObject("task"))||!spec.getString("disturbance").equals(row.getString("disturbance"))||!spec.getJSONObject("event").similar(row.getJSONObject("event"))||!LaterPolicy069.initial(task).similar(row.getJSONObject("plan"))||Instant.parse(row.getString("plannedAt")).isBefore(frozen))throw new IllegalStateException("Evaluation plan leaks data or predates frozen model");
                for(String key:List.of("valueMillis","calibratedMillis")){double ms=row.getJSONObject("screenTiming").getDouble(key);boolean used=row.getJSONObject("plan").getJSONObject("decisions").getJSONObject("calibrated").has("screen");if(!Double.isFinite(ms)||ms<0||!used&&ms!=0)throw new IllegalStateException("Invalid screening time");}
                var results=row.getJSONObject("results");if(!results.keySet().equals(new HashSet<>(LaterPolicy069.METHODS)))throw new IllegalStateException("Evaluation method missing");
                for(String method:LaterPolicy069.METHODS){var d=row.getJSONObject("plan").getJSONObject("decisions").getJSONObject(method);var result=results.getJSONObject(method);var record=result.optJSONObject("record");if(result.has("pending"))throw new IllegalStateException("Incomplete evaluation");
                    if(d.getBoolean("execute")){if(phase.equals("unsupported")||record==null)throw new IllegalStateException("Unsupported or missing execution");verifyRecord(row,task,d,record,row.getJSONObject("event"),ids);count++;}else if(record!=null)throw new IllegalStateException("Refusal fabricates motion");
                    if(!LaterPolicy069.outcome(task,d,record).similar(result.getJSONObject("outcome")))throw new IllegalStateException("Evaluation costs/outcome changed");
                }
            }
            if(!evaluation(rows,phase).similar(job.getJSONObject(phase+"Evaluation069")))throw new IllegalStateException("Evaluation metrics changed");
        }
        if(job.getInt("progress")!=TOTAL||job.getInt("total")!=TOTAL||job.getInt("nativeExecutions")!=count||job.getBoolean("accepted")!=(job.getJSONObject("cleanEvaluation069").getBoolean("qualified")&&job.getJSONObject("disturbedEvaluation069").getBoolean("qualified")))throw new IllegalStateException("Counts or qualification changed");
    }
    private static void verifyRecord(JSONObject row,JSONObject task,JSONObject decision,JSONObject record,JSONObject event,Set<String> ids){
        LaterTrial069.verify(record);if(!record.getJSONObject("task").similar(task)||!record.getJSONObject("decision").similar(decision)||!record.getJSONObject("disturbance").similar(event)||!ids.add(record.getString("id"))||Instant.parse(record.getString("startedAt")).isBefore(Instant.parse(row.getString("plannedAt"))))throw new IllegalStateException("Native source, world identity or prior plan differs");
    }
    static JSONObject summary(JSONObject job){var out=new JSONObject().put("jobId",job.getString("id")).put("status",job.getString("status")).put("nativeExecutions",job.optInt("nativeExecutions"));
        if(job.has("reusedModel069"))out.put("modelSha256",job.getJSONObject("reusedModel069").getString("sha256")).put("modelFrozenAt",job.getString("modelFrozenAt")).put("modelScope","Published068 calibration, unchanged; not calibrated for two-stage recovery. No new fit.");
        if(job.has("cleanEvaluation069"))for(String phase:List.of("clean","disturbed","unsupported"))out.put(phase,job.getJSONObject(phase+"Evaluation069"));
        if(job.has("accepted"))out.put("qualified",job.getBoolean("accepted"));return out;
    }
}
