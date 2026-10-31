package fraymus.workspace;

import org.json.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Fixed native experiment design, training-only selection and reserved paired gates. */
final class PushLearning {
    static final String PROCEDURE="push-learning-v1",VERSION="force-selection-032-v1";
    static final String SCOPE="One initial-force parameter selected from actual training physics and checked on separately reserved synthetic cases; unchanged031 controller and32N maximum. Not real-world calibration, universal learning or Ollama-weight training.";
    static JSONObject copy(JSONObject o){return new JSONObject(o.toString());}
    static String hash(JSONObject o){return MeasurementStore.hash(MapCheckpoint.canonical(o));}
    static JSONObject suite(){try(var stream=PushLearning.class.getResourceAsStream("/push-evaluation032.json")){return new JSONObject(new String(Objects.requireNonNull(stream).readAllBytes(),StandardCharsets.UTF_8));}catch(Exception e){throw new IllegalStateException(e);}}
    static String identity(){var text=new StringBuilder(VERSION).append(PushCheckpoint.identity()).append(hash(suite()));for(Class<?> type:new Class<?>[]{PushLearning.class,PushLearningStore.class,PushLearningReport.class})try(var stream=type.getResourceAsStream(type.getSimpleName()+".class")){text.append(MapDefinition.sha(stream.readAllBytes()));}catch(Exception e){throw new IllegalStateException(e);}return MeasurementStore.hash(text.toString());}
    static JSONArray cases(JSONObject suite,String split){var a=new JSONArray();for(Object raw:suite.getJSONArray("cases"))if(((JSONObject)raw).getString("split").equals(split))a.put(raw);return a;}
    static JSONObject find(JSONObject suite,String id){for(Object raw:suite.getJSONArray("cases"))if(((JSONObject)raw).getString("id").equals(id))return (JSONObject)raw;throw new IllegalArgumentException("Unknown frozen case");}
    static JSONObject source(JSONObject c,String job){String id=UUID.nameUUIDFromBytes((job+":"+c.getString("id")).getBytes(StandardCharsets.UTF_8)).toString();return SpatialScene.parse(c.getJSONObject("source").toString(),id);}
    static JSONObject plan(JSONObject suite,JSONObject c,String job,double force){var s=source(c,job);String crate="",pusher="";for(Object raw:s.getJSONArray("objects")){var o=(JSONObject)raw;if(o.getString("sourceId").equals("crate"))crate=o.getString("id");if(o.getString("sourceId").equals("pusher"))pusher=o.getString("id");}var p=new PushPlan(s,crate,pusher);return p.plan(p.intent(c.getJSONArray("displacementM"),force,suite.getDouble("maxForceN")));}
    static double score(JSONObject suite,JSONObject row){return row.getJSONObject("metrics").getBoolean("passed")?row.getJSONObject("metrics").getDouble("timeS"):suite.getJSONObject("selection").getDouble("failedSeconds");}
    static JSONObject select(JSONObject suite,JSONArray rows){
        var trials=new JSONArray();double best=Double.POSITIVE_INFINITY,winner=-1;int expected=cases(suite,"training").length();
        for(Object raw:suite.getJSONArray("candidateInitialForcesN")){double force=((Number)raw).doubleValue(),total=0;var found=new HashSet<String>();for(Object value:rows){var row=(JSONObject)value;if(!row.getString("split").equals("training"))continue;if(row.getDouble("initialForceN")==force){if(!find(suite,row.getString("caseId")).getString("split").equals("training")||!found.add(row.getString("caseId")))throw new IllegalStateException("Training split/count changed");total+=score(suite,row);}}
            if(found.size()!=expected)throw new IllegalStateException("Complete training evidence required before selection");double mean=total/expected;trials.put(new JSONObject().put("initialForceN",force).put("meanPenalizedTimeS",mean));if(mean<best-1e-9||Math.abs(mean-best)<1e-9&&force<winner){best=mean;winner=force;}}
        return new JSONObject().put("initialForceN",winner).put("trainingScores",trials).put("objective",suite.getJSONObject("selection")).put("trainingOnly",true);
    }
    static JSONObject decide(JSONObject suite,JSONArray rows,JSONObject selection){
        var actual=select(suite,rows);if(!actual.similar(selection))throw new IllegalStateException("Candidate does not match training-only selection");
        double baseline=suite.getDouble("baselineInitialForceN"),candidate=actual.getDouble("initialForceN"),bt=0,ct=0,be=0,ce=0,maxRegression=Double.NEGATIVE_INFINITY;int bs=0,cs=0,regressions=0,newJams=0;var pairs=new JSONArray();
        for(Object raw:cases(suite,"evaluation")){var c=(JSONObject)raw;JSONObject b=null,n=null;for(Object value:rows){var row=(JSONObject)value;if(row.getString("split").equals("evaluation")&&row.getString("caseId").equals(c.getString("id"))){if(row.getString("arm").equals("baseline")){if(b!=null)throw new IllegalStateException("Duplicate baseline");b=row;}else if(row.getString("arm").equals("candidate")){if(n!=null)throw new IllegalStateException("Duplicate candidate");n=row;}}}if(b==null||n==null||b.getDouble("initialForceN")!=baseline||n.getDouble("initialForceN")!=candidate)throw new IllegalStateException("Missing/original paired evidence changed");
            var bm=b.getJSONObject("metrics");var nm=n.getJSONObject("metrics");boolean bp=bm.getBoolean("passed"),np=nm.getBoolean("passed");if(bp)bs++;if(np)cs++;if(bp&&!np)regressions++;if(bp&&np&&nm.getInt("jams")>bm.getInt("jams"))newJams++;double btime=score(suite,b),ctime=score(suite,n);bt+=btime;ct+=ctime;be+=bm.getDouble("effortNs");ce+=nm.getDouble("effortNs");maxRegression=Math.max(maxRegression,ctime-btime);
            pairs.put(new JSONObject().put("caseId",c.getString("id")).put("baselineRunId",b.getString("runId")).put("candidateRunId",n.getString("runId")).put("baselinePassed",bp).put("candidatePassed",np).put("baselineTimeS",btime).put("candidateTimeS",ctime).put("baselineEffortNs",bm.getDouble("effortNs")).put("candidateEffortNs",nm.getDouble("effortNs")).put("baselineJams",bm.getInt("jams")).put("candidateJams",nm.getInt("jams")));
        }
        int count=pairs.length();double improvement=(bt-ct)/count,ratio=ce/Math.max(be,1e-9);var g=suite.getJSONObject("gates");boolean accepted=cs/(double)count>=g.getDouble("minSuccessFraction")&&regressions==0&&newJams==0&&improvement>=g.getDouble("minMeanTimeImprovementS")&&ratio<=g.getDouble("maxEffortRatio")&&maxRegression<=g.getDouble("maxPerCaseTimeRegressionS");
        return new JSONObject().put("passed",true).put("accepted",accepted).put("baselineSuccesses",bs).put("candidateSuccesses",cs).put("evaluationCases",count).put("successRegressions",regressions).put("newSuccessfulCaseJams",newJams).put("meanPenalizedTimeImprovementS",improvement).put("effortRatio",ratio).put("maxPerCaseTimeRegressionS",maxRegression).put("gates",g).put("pairs",pairs).put("selection",actual).put("scope",SCOPE);
    }
}
