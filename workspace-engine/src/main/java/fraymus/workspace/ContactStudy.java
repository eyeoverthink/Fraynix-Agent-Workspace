package fraymus.workspace;
import org.json.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
/** Separate reserved test of an experience-guided controller, not cap selection. */
final class ContactStudy {
 static final String PROCEDURE="contact-study-v1";
 static final String SCOPE="Versioned contact-controller comparison on seen development and different reserved synthetic scenes. Same8/32N and original native budgets. Not LLM training or calibrated robot control.";
 static JSONObject suite(){try(var s=ContactStudy.class.getResourceAsStream("/contact-study033.json")){return new JSONObject(new String(Objects.requireNonNull(s).readAllBytes(),StandardCharsets.UTF_8));}catch(Exception e){throw new IllegalStateException(e);}}
 static String identity(){var text=new StringBuilder(ContactCheckpoint.identity()).append(PushLearning.hash(suite()));for(Class<?> type:new Class<?>[]{ContactStudy.class,ContactStudyStore.class,ContactStudyReport.class})try(var s=type.getResourceAsStream(type.getSimpleName()+".class")){text.append(MapDefinition.sha(s.readAllBytes()));}catch(Exception e){throw new IllegalStateException(e);}return MeasurementStore.hash(text.toString());}
 static double score(JSONObject suite,JSONObject row){var m=row.getJSONObject("metrics");return m.getBoolean("passed")?m.getDouble("timeS"):suite.getDouble("failedSeconds");}
 static JSONObject decide(JSONObject suite,JSONArray rows){
  var pairs=new JSONArray();int devPass=0,devExpected=0,evalPass=0,evalExpected=0,baselineEval=0,regressions=0,negativeErrors=0,matched=0;double baselineTime=0,candidateTime=0,baselineEffort=0,candidateEffort=0,totalBaselineEffort=0,totalCandidateEffort=0;
  for(Object raw:suite.getJSONArray("cases")){var c=(JSONObject)raw;JSONObject b=null,n=null;for(Object value:rows){var row=(JSONObject)value;if(!row.getString("caseId").equals(c.getString("id")))continue;if(row.getString("arm").equals("baseline")){if(b!=null)throw new IllegalStateException("Duplicate baseline case");b=row;}else if(row.getString("arm").equals("candidate")){if(n!=null)throw new IllegalStateException("Duplicate candidate case");n=row;}}
   if(b==null||n==null)throw new IllegalStateException("Complete paired native evidence required");var bm=b.getJSONObject("metrics");var nm=n.getJSONObject("metrics");boolean bp=bm.getBoolean("passed"),np=nm.getBoolean("passed"),negative=c.getBoolean("negativeControl"),evaluation=c.getString("split").equals("evaluation");if(bp&&!np)regressions++;if(negative&&(bp||np))negativeErrors++;
   if(!negative){if(evaluation){evalExpected++;if(np)evalPass++;if(bp)baselineEval++;}else{devExpected++;if(np)devPass++;}}
   if(evaluation){baselineTime+=score(suite,b);candidateTime+=score(suite,n);totalBaselineEffort+=bm.getDouble("effortNs");totalCandidateEffort+=nm.getDouble("effortNs");if(bp&&np&&!negative){matched++;baselineEffort+=bm.getDouble("effortNs");candidateEffort+=nm.getDouble("effortNs");}}
   pairs.put(new JSONObject().put("caseId",c.getString("id")).put("split",c.getString("split")).put("negativeControl",negative).put("baselinePassed",bp).put("candidatePassed",np).put("baselineTimeS",score(suite,b)).put("candidateTimeS",score(suite,n)).put("baselineEffortNs",bm.getDouble("effortNs")).put("candidateEffortNs",nm.getDouble("effortNs")).put("baselineJams",bm.getInt("jams")).put("candidateJams",nm.getInt("jams")).put("baselineRunId",b.getString("runId")).put("candidateRunId",n.getString("runId")));
  }
  var g=suite.getJSONObject("gates");double gain=(baselineTime-candidateTime)/PushLearning.cases(suite,"evaluation").length(),ratio=matched>0?candidateEffort/Math.max(1e-9,baselineEffort):1e9;boolean accepted=devPass==devExpected&&evalPass==evalExpected&&evalPass-baselineEval>=g.getInt("minEvaluationSuccessGain")&&gain>=g.getDouble("minEvaluationMeanPenalizedTimeGainS")&&ratio<=g.getDouble("maxMatchedSuccessEffortRatio")&&regressions==0&&negativeErrors==0&&matched>0;
  return new JSONObject().put("passed",true).put("accepted",accepted).put("developmentPositivePasses",devPass).put("developmentPositiveCases",devExpected).put("evaluationPositivePasses",evalPass).put("evaluationPositiveCases",evalExpected).put("baselineEvaluationPositivePasses",baselineEval).put("successRegressions",regressions).put("negativeControlErrors",negativeErrors).put("meanPenalizedTimeGainS",gain).put("matchedSuccessCases",matched).put("matchedSuccessEffortRatio",ratio).put("allCaseEffortRatio",totalCandidateEffort/Math.max(1e-9,totalBaselineEffort)).put("gates",g).put("pairs",pairs).put("scope",SCOPE);
 }
}
