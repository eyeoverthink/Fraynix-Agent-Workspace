package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class AdaptiveRouting070Test {
 @Test public void studyRequestsReadVerifiedEvidenceBeforeReporting(){
  for(String request:new String[]{"Inspect the adaptive observation timing study. Read actual Java state and create the verified report.","Explain observation timing costs and skipped checks","Inspect the observation schedule","Where did sensing effort help?"}){
   assertEquals(request,"agent",ChatPolicy044.route(request,new JSONObject(),false));
   var task=new JSONObject().put("family044","agent").put("goal",request).put("context044",new JSONObject());
   assertEquals(java.util.List.of("agent.read"),ChatPolicy044.allowed(task));
   task.getJSONObject("context044").put("agent042Facts",new JSONObject());
   assertEquals(java.util.List.of("agent.report"),ChatPolicy044.allowed(task));
   task.getJSONObject("context044").put("agent042Report",new JSONObject());
   assertEquals(java.util.List.of("finish"),ChatPolicy044.allowed(task));
  }
 }
 @Test public void explicitWebCreationRemainsContentCreation(){
  assertEquals("workbench",ChatPolicy044.route("Build a website about adaptive observation timing",new JSONObject(),false));
  assertEquals("observations",ChatPolicy044.route("Inspect the footage observation run",new JSONObject(),false));
 }
}
