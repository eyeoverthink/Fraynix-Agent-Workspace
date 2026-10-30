package fraymus.workspace;
import fraymus.neural.*;
import org.json.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Action-conditioned learned dynamics, separate calibration/evaluation and native planning. */
final class WorldModelExperiment {
    static final String PROTOCOL="structured-world-v1";
    private WorldModelExperiment(){}
    static String resource(String name) throws Exception {try(var s=WorldModelExperiment.class.getResourceAsStream("/world-model/"+name+"-v1.json")){if(s==null)throw new IllegalStateException("Missing world-model protocol");return new String(s.readAllBytes(),StandardCharsets.UTF_8);}}
    static JSONObject manifest() throws Exception {var c=new JSONObject(resource("contract"));if(c.getInt("ensemble")!=5||c.getInt("stepTicks")!=12||c.getInt("horizon")!=8||c.getInt("planBlock")!=4||c.getInt("stepsPerEpisode")!=16||c.getInt("fitEpisodesPerCase")!=4||c.getInt("calibrationEpisodesPerCase")!=2||c.getInt("evaluationEpisodesPerCase")!=2)throw new IllegalStateException("Packaged world protocol differs from the implementation");var out=new JSONObject().put("protocol",PROTOCOL);for(String n:List.of("contract","training","calibration","evaluation"))out.put(n+"Sha256",MeasurementStore.hash(resource(n)));return out;}
    static LearnedDynamics.Context context(JSONObject c){return new LearnedDynamics.Context(c.getDouble("angleDeg"),c.getDouble("gravityMps2"),c.getDouble("friction"),c.getDouble("massKg"));}
    static LearnedDynamics.State state(JSONObject sample,JSONObject c){return new LearnedDynamics.State(sample.getDouble("distanceM"),FeedbackExperiment.velocity(sample,c.getDouble("angleDeg")),sample.getInt("contacts")>0?1:0);}
    static JSONObject stateJson(LearnedDynamics.State s){return new JSONObject().put("x",s.x()).put("v",s.v()).put("contact",s.contact());}
    static JSONObject pack(LearnedDynamics model){return new JSONObject().put("protocol",PROTOCOL).put("weights",new JSONArray(model.weights())).put("calibrationX",model.calibrationX()).put("calibrationV",model.calibrationV());}
    static LearnedDynamics unpack(JSONObject p){if(!p.getString("protocol").equals(PROTOCOL))throw new IllegalArgumentException("Unknown learned dynamics");var a=p.getJSONArray("weights");if(a.length()!=5)throw new IllegalArgumentException("Invalid ensemble");double[][][] w=new double[5][3][7];for(int i=0;i<5;i++){if(a.getJSONArray(i).length()!=3)throw new IllegalArgumentException("Invalid outputs");for(int j=0;j<3;j++){var row=a.getJSONArray(i).getJSONArray(j);if(row.length()!=7)throw new IllegalArgumentException("Invalid features");for(int k=0;k<7;k++)w[i][j][k]=row.getDouble(k);}}return new LearnedDynamics(w,p.getDouble("calibrationX"),p.getDouble("calibrationV"));}
    private record Episode(JSONObject source,List<LearnedDynamics.Transition> rows,JSONArray evidence){}
    private static List<Episode> collect(JSONArray cases,int count,Path root,Runnable check,JSONObject job,FeedbackExperiment.Progress progress) throws Exception {
        var all=new ArrayList<Episode>();for(Object raw:cases){var c=(JSONObject)raw;for(int episode=0;episode<count;episode++){
            var params=new JSONObject();for(String k:List.of("angleDeg","friction","massKg","gravityMps2"))params.put(k,c.get(k));var config=RampDefinition.from(params);var ids=new JSONObject();for(String k:List.of("ramp","block","ground"))ids.put(k,UUID.randomUUID().toString());
            var rows=new ArrayList<LearnedDynamics.Transition>();var evidence=new JSONArray();var random=new Random(c.getLong("seed")+episode*1000);
            try(var physics=new BulletRamp(config,ids,root)){
                for(int step=0;step<16;step++){
                    check.run();var before=physics.sample();var s=state(before,c);double u=(random.nextInt(7)-3)*.2;if(s.v()>2.6)u=-.6;if(s.v() < -2.6||s.x() < -.5)u=.6;LearnedDynamics.validate(s,u,context(c));
                    physics.push(u);for(int t=0;t<12;t++){check.run();physics.step();}var after=physics.sample();var next=state(after,c);rows.add(new LearnedDynamics.Transition(s,u,next,context(c)));evidence.put(new JSONObject().put("before",before).put("action",u).put("after",after));
                }
            }
            all.add(new Episode(new JSONObject(c.toString()).put("episode",episode).put("entityIds",ids),List.copyOf(rows),evidence));job.put("progress",job.getInt("progress")+1);progress.save("collecting "+c.getString("id")+" episode "+episode);
        }}return all;
    }
    private static JSONArray evidence(List<Episode> episodes){var out=new JSONArray();for(var e:episodes)out.put(new JSONObject().put("source",e.source).put("transitions",e.evidence));return out;}
    private static JSONObject evaluate(LearnedDynamics model,List<Episode> episodes){
        double x=0,v=0,bx=0,bv=0,rx=0,rv=0;int n=0,covered=0,rollouts=0;var predictions=new JSONArray();
        for(var episode:episodes){for(var row:episode.rows){var p=model.predict(row.before(),row.action(),row.context());double ex=p.mean().x()-row.after().x(),ev=p.mean().v()-row.after().v();x+=ex*ex;v+=ev*ev;double baseV=row.before().v()+row.action(),baseX=row.before().x()+.05*baseV;bx+=Math.pow(baseX-row.after().x(),2);bv+=Math.pow(baseV-row.after().v(),2);if(Math.abs(ex)<=p.radiusX()&&Math.abs(ev)<=p.radiusV())covered++;n++;predictions.put(new JSONObject().put("source",episode.source).put("before",stateJson(row.before())).put("action",row.action()).put("actual",stateJson(row.after())).put("predicted",stateJson(p.mean())).put("radiusX",p.radiusX()).put("radiusV",p.radiusV()));}
            for(int start:new int[]{0,8}){var states=new ArrayList<LearnedDynamics.State>();for(int e=0;e<5;e++)states.add(episode.rows.get(start).before());for(int h=0;h<8;h++){var row=episode.rows.get(start+h);for(int e=0;e<5;e++)states.set(e,model.member(e,states.get(e),row.action(),row.context()));}double px=states.stream().mapToDouble(LearnedDynamics.State::x).average().orElseThrow(),pv=states.stream().mapToDouble(LearnedDynamics.State::v).average().orElseThrow();var actual=episode.rows.get(start+7).after();rx+=Math.pow(px-actual.x(),2);rv+=Math.pow(pv-actual.v(),2);rollouts++;}
        }
        return new JSONObject().put("transitions",n).put("positionRmseM",Math.sqrt(x/n)).put("velocityRmseMps",Math.sqrt(v/n)).put("kinematicPositionRmseM",Math.sqrt(bx/n)).put("kinematicVelocityRmseMps",Math.sqrt(bv/n)).put("rolloutPositionRmseM",Math.sqrt(rx/rollouts)).put("rolloutVelocityRmseMps",Math.sqrt(rv/rollouts)).put("rollouts",rollouts).put("oneStepBandCoverage",covered/(double)n).put("predictions",predictions);
    }
    static JSONObject planReceipt(BulletRamp physics,JSONObject c,LearnedDynamics model,int tick){
        var observed=physics.sample();var plan=WorldPlanner.plan(model,state(observed,c),context(c),tick,c.getDouble("targetM"));var future=new JSONArray();
        for(int i=0;i<plan.future().size();i++){var p=plan.future().get(i);future.put(new JSONObject().put("tick",tick+(i+1)*12).put("state",stateJson(p.mean())).put("radiusX",p.radiusX()).put("radiusV",p.radiusV()));}
        return new JSONObject().put("mode",plan.fallback()?"feedback-fallback":"learned-plan").put("reason",plan.reason()).put("action",plan.action()).put("cost",Double.isFinite(plan.cost())?plan.cost():JSONObject.NULL).put("future",future).put("sourceTick",tick).put("observed",observed).put("candidates",49);
    }
    static JSONObject planningTrial(JSONObject c,LearnedDynamics model,boolean learned,Path root,Runnable check) throws Exception {
        var params=new JSONObject();for(String k:List.of("angleDeg","friction","massKg","gravityMps2"))params.put(k,c.get(k));var config=RampDefinition.from(params);var ids=new JSONObject();for(String k:List.of("ramp","block","ground"))ids.put(k,UUID.randomUUID().toString());
        try(var physics=new BulletRamp(config,ids,root)){var samples=new JSONArray().put(physics.sample());var events=new JSONArray();var plans=new JSONArray();int selected=0;
            for(int tick=0;tick<360;tick++){
                check.run();for(Object raw:c.getJSONArray("kicks")){var kick=(JSONObject)raw;if(kick.getInt("tick")==tick)events.put(FeedbackExperiment.impulse(physics,config,kick.getDouble("deltaVelocityMps"),"disturbance"));}
                if(tick%12==0){if(learned){var plan=planReceipt(physics,c,model,tick);plans.put(plan);if(plan.getString("mode").equals("learned-plan"))selected++;events.put(FeedbackExperiment.impulse(physics,config,plan.getDouble("action"),"control"));}else FeedbackExperiment.act(physics,config,c.getDouble("targetM"),new JSONObject().put("kp",49).put("kd",10),true,events);}
                physics.step();if(physics.tick()%4==0)samples.put(physics.sample());
            }
            var last=physics.snapshot();return new JSONObject().put("case",c).put("mode",learned?"learned-plan":"feedback-017").put("backend",BulletRamp.BACKEND).put("samples",samples).put("events",events).put("plans",plans).put("modelActionFraction",learned?selected/30.0:0).put("final",last).put("metrics",FeedbackExperiment.metrics(c,samples,last,events));
        }
    }
    static void train(JSONObject job,Path nativeRoot,Path repo,Runnable check,FeedbackExperiment.Progress progress) throws Exception {
        var contract=new JSONObject(resource("contract"));job.put("worldProtocol",manifest()).put("progress",0).put("total",72);progress.save("world dynamics collection starting");
        var fitting=collect(new JSONArray(resource("training")),4,nativeRoot,check,job,progress);job.put("worldTraining",evidence(fitting));check.run();var model=LearnedDynamics.fit(fitting.stream().map(Episode::rows).toList(),contract.getDouble("ridge"),contract.getLong("bootstrapSeed"));
        var calibration=collect(new JSONArray(resource("calibration")),2,nativeRoot,check,job,progress);model=model.calibrate(calibration.stream().flatMap(e->e.rows.stream()).toList());job.put("worldCalibration",evidence(calibration)).put("worldCandidate",pack(model));progress.save("world model frozen before native evaluation");
        var testing=collect(new JSONArray(resource("evaluation")),2,nativeRoot,check,job,progress);var evaluation=evaluate(model,testing);job.put("worldTestEpisodes",evidence(testing)).put("worldPredictionEvaluation",evaluation);progress.save("testing learned counterfactual planning");
        var trials=new JSONArray();double score=0,baseline=0,maxX=0,maxV=0,maxSettling=0,minFraction=1;
        for(Object raw:new JSONArray(resource("evaluation"))){var c=(JSONObject)raw;for(boolean learned:new boolean[]{false,true}){var run=planningTrial(c,model,learned,nativeRoot,check);trials.put(run);var m=run.getJSONObject("metrics");if(learned){score+=m.getDouble("score");maxX=Math.max(maxX,m.getDouble("endpointErrorM"));maxV=Math.max(maxV,m.getDouble("terminalSpeedMps"));maxSettling=Math.max(maxSettling,m.getDouble("settlingMaxErrorM"));minFraction=Math.min(minFraction,run.getDouble("modelActionFraction"));}else baseline+=m.getDouble("score");job.put("progress",job.getInt("progress")+1);progress.save("native planning trial "+job.getInt("progress"));}}
        var gate=contract.getJSONObject("nativeAcceptance");boolean accepted=evaluation.getDouble("positionRmseM")<=gate.getDouble("maxPositionRmseM")&&evaluation.getDouble("velocityRmseMps")<=gate.getDouble("maxVelocityRmseMps")&&evaluation.getDouble("rolloutPositionRmseM")<=gate.getDouble("maxRolloutPositionRmseM")&&evaluation.getDouble("rolloutVelocityRmseMps")<=gate.getDouble("maxRolloutVelocityRmseMps")&&evaluation.getDouble("positionRmseM")<=evaluation.getDouble("kinematicPositionRmseM")*gate.getDouble("maxKinematicRmseRatio")&&evaluation.getDouble("velocityRmseMps")<=evaluation.getDouble("kinematicVelocityRmseMps")*gate.getDouble("maxKinematicRmseRatio")&&evaluation.getDouble("oneStepBandCoverage")>=gate.getDouble("minOneStepBandCoverage")&&maxX<=gate.getDouble("maxPlanningEndpointErrorM")&&maxV<=gate.getDouble("maxPlanningSpeedMps")&&maxSettling<=gate.getDouble("maxPlanningSettlingErrorM")&&score/8<=baseline/8+gate.getDouble("planningStrongMeanScoreMargin")&&minFraction>=gate.getDouble("minModelActionFraction");
        job.put("worldPlanningTrials",trials).put("worldPlanningEvaluation",new JSONObject().put("meanScore",score/8).put("feedback017MeanScore",baseline/8).put("maxEndpointErrorM",maxX).put("maxSpeedMps",maxV).put("maxSettlingErrorM",maxSettling).put("minModelActionFraction",minFraction)).put("accepted",accepted);
        job.put("realityEvaluation",RealityForecast.evaluate(repo,model));progress.save("native and recorded reality gates scored separately");
    }
}
