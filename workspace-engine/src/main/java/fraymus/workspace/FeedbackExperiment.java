package fraymus.workspace;

import fraymus.neural.FeedbackController;
import org.json.*;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Native outcomes select gains on training cases only. Evaluation is read after selection. */
final class FeedbackExperiment {
    static final String PROTOCOL="native-feedback-v1";
    private FeedbackExperiment(){}
    static String resource(String name) throws Exception {
        try(var stream=FeedbackExperiment.class.getResourceAsStream("/feedback/"+name+"-v1.json")){
            if(stream==null)throw new IllegalStateException("Missing packaged feedback protocol");
            return new String(stream.readAllBytes(),StandardCharsets.UTF_8);
        }
    }
    static JSONObject manifest() throws Exception {
        var c=new JSONObject(resource("contract"));if(!c.getString("protocol").equals(PROTOCOL)||c.getInt("ticks")!=FeedbackController.TICKS||c.getInt("controlTicks")!=FeedbackController.CONTROL_TICKS||c.getInt("trajectoryTicks")!=240||c.getDouble("maxDeltaVelocityMps")!=FeedbackController.MAX_DV)throw new IllegalStateException("Packaged protocol differs from controller implementation");
        return new JSONObject().put("protocol",PROTOCOL).put("contractSha256",MeasurementStore.hash(resource("contract")))
            .put("trainingSha256",MeasurementStore.hash(resource("training"))).put("evaluationSha256",MeasurementStore.hash(resource("evaluation")));
    }
    static double velocity(JSONObject state,double angle){var v=state.getJSONArray("velocity");return FeedbackController.tangentVelocity(v.getDouble(0),v.getDouble(1),Math.toRadians(angle));}
    static void validate(JSONObject parameters,double target){
        RampDefinition.range(target,.4,1.8,"feedback target");
        RampDefinition.range(parameters.getDouble("angleDeg"),10,40,"feedback slope");RampDefinition.range(parameters.getDouble("friction"),0,.3,"feedback friction");
        RampDefinition.range(parameters.getDouble("gravityMps2"),8,12,"feedback gravity");RampDefinition.range(parameters.getDouble("massKg"),.5,2,"feedback mass");
        if(parameters.getDouble("lengthM")!=12||parameters.getDouble("widthM")!=3||parameters.getDouble("offsetZ")!=0)throw new IllegalArgumentException("Feedback protocol requires default 12 × 3 m ramp at Z=0");
    }
    static JSONObject impulse(BulletRamp physics,RampDefinition config,double dv,String kind){
        var before=physics.sample();physics.push(dv);var after=physics.sample();
        return new JSONObject().put("tick",physics.tick()).put("kind",kind).put("deltaVelocityMps",dv).put("impulseNs",dv*config.massKg()).put("before",before).put("after",after);
    }
    static void act(BulletRamp physics,RampDefinition config,double target,JSONObject gains,boolean feedback,JSONArray events){
        var observed=physics.sample();double v=velocity(observed,config.angleDeg());
        var decision=FeedbackController.decide(physics.tick(),observed.getDouble("distanceM"),v,target,config.radians(),config.gravityMps2(),config.friction(),gains.getDouble("kp"),gains.getDouble("kd"),feedback);
        var receipt=impulse(physics,config,decision.deltaVelocityMps(),"control");receipt.put("referenceM",decision.referenceM()).put("referenceVelocityMps",decision.referenceVelocityMps()).put("positionErrorM",decision.positionErrorM()).put("velocityErrorMps",decision.velocityErrorMps());events.put(receipt);
    }
    static JSONObject metrics(JSONObject c,JSONArray samples,JSONObject last,JSONArray events){
        double target=c.getDouble("targetM"),endpoint=Math.abs(last.getDouble("distanceM")-target),speed=Math.abs(velocity(last,c.getDouble("angleDeg"))),settling=0,impulse=0;
        for(Object raw:samples){var row=(JSONObject)raw;if(row.getInt("tick")>=300)settling=Math.max(settling,Math.abs(row.getDouble("distanceM")-target));}
        for(Object raw:events){var row=(JSONObject)raw;if(row.getString("kind").equals("control"))impulse+=Math.abs(row.getDouble("impulseNs"));}
        return new JSONObject().put("endpointErrorM",endpoint).put("terminalSpeedMps",speed).put("settlingMaxErrorM",settling).put("score",endpoint+.15*speed+.5*settling).put("controlAbsoluteImpulseNs",impulse);
    }
    static JSONObject trial(JSONObject c,String mode,JSONObject gains,Path nativeRoot,Runnable check) throws Exception {
        var parameters=new JSONObject();for(String key:List.of("angleDeg","friction","massKg","gravityMps2"))parameters.put(key,c.get(key));var config=RampDefinition.from(parameters);
        var ids=new JSONObject();for(String key:List.of("ramp","block","ground"))ids.put(key,UUID.randomUUID().toString());
        try(var physics=new BulletRamp(config,ids,nativeRoot)){
            var events=new JSONArray();var samples=new JSONArray().put(physics.sample());
            for(int tick=0;tick<FeedbackController.TICKS;tick++){
                check.run();for(Object raw:c.getJSONArray("kicks")){var kick=(JSONObject)raw;if(kick.getInt("tick")==tick)events.put(impulse(physics,config,kick.getDouble("deltaVelocityMps"),"disturbance"));}
                if(tick%FeedbackController.CONTROL_TICKS==0&&!mode.equals("zero"))act(physics,config,c.getDouble("targetM"),gains,!mode.equals("feedforward"),events);
                physics.step();if(physics.tick()%4==0)samples.put(physics.sample());
            }
            var last=physics.snapshot();return new JSONObject().put("id",UUID.randomUUID().toString()).put("mode",mode).put("case",c).put("gains",gains).put("backend",BulletRamp.BACKEND).put("dt",BulletRamp.DT).put("samples",samples).put("events",events).put("final",last).put("metrics",metrics(c,samples,last,events));
        }
    }
    interface Progress {void save(String phase) throws Exception;}
    static void train(JSONObject job,Path nativeRoot,Runnable check,Progress progress) throws Exception {
        var contract=new JSONObject(resource("contract"));var training=new JSONArray(resource("training"));var candidates=contract.getJSONArray("trainingGains");
        job.put("feedbackProtocol",manifest()).put("contract",contract).put("progress",0).put("total",86).put("search",new JSONArray());progress.save("collecting closed-loop training outcomes");
        JSONObject selected=null;double best=Double.POSITIVE_INFINITY;
        for(Object raw:candidates){var gains=(JSONObject)raw;double score=0;var trials=new JSONArray();for(Object row:training){var trial=trial((JSONObject)row,"candidate",gains,nativeRoot,check);trials.put(trial);score+=trial.getJSONObject("metrics").getDouble("score");job.put("progress",job.getInt("progress")+1);}score/=training.length();job.getJSONArray("search").put(new JSONObject().put("gains",gains).put("meanScore",score).put("trials",trials));if(score<best){best=score;selected=new JSONObject(gains.toString());}progress.save("selecting from training only");}
        job.put("selectedGains",selected).put("trainingScore",best);progress.save("gains frozen before evaluation");
        // Evaluation is never supplied to the search or the controller; future kicks remain in the runner.
        var evaluation=new JSONArray(resource("evaluation"));var results=new JSONArray();var scores=new JSONObject();double maxError=0,maxSpeed=0,maxSettling=0;
        for(String mode:List.of("zero","feedforward","analytical-feedback","candidate")){
            double sum=0;for(Object row:evaluation){var trial=trial((JSONObject)row,mode,mode.equals("candidate")?selected:contract.getJSONObject("baselineGains"),nativeRoot,check);results.put(trial);var m=trial.getJSONObject("metrics");sum+=m.getDouble("score");if(mode.equals("candidate")){maxError=Math.max(maxError,m.getDouble("endpointErrorM"));maxSpeed=Math.max(maxSpeed,m.getDouble("terminalSpeedMps"));maxSettling=Math.max(maxSettling,m.getDouble("settlingMaxErrorM"));}job.put("progress",job.getInt("progress")+1);}scores.put(mode,sum/evaluation.length());job.put("feedbackTrials",results);progress.save("evaluating frozen feedback candidate");
        }
        var gate=contract.getJSONObject("acceptance");double gain=1-scores.getDouble("candidate")/scores.getDouble("feedforward");boolean accepted=maxError<=gate.getDouble("maxEndpointErrorM")&&maxSpeed<=gate.getDouble("maxTerminalSpeedMps")&&maxSettling<=gate.getDouble("maxSettlingErrorM")&&scores.getDouble("candidate")<=scores.getDouble("analytical-feedback")+gate.getDouble("strongBaselineScoreMargin")&&gain>=gate.getDouble("minFeedforwardRelativeGain");
        job.put("feedbackEvaluation",new JSONObject().put("meanScores",scores).put("maxEndpointErrorM",maxError).put("maxTerminalSpeedMps",maxSpeed).put("maxSettlingErrorM",maxSettling).put("relativeFeedforwardScoreGain",gain).put("accepted",accepted)).put("accepted",accepted);
    }
}
