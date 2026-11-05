package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;

/** Durable source receipt must survive generic size compaction before bounded planner projection. */
public class CalibrationChat068Test {
 @Test public void largeAgentReceiptRetainsOriginalEvidenceAndDoesNotTrustHostReplacement()throws Exception{
  try(var store=new WorkspaceStore(Files.createTempDirectory("calibration068-chat-boundary-"))){
   var domain=Domain044Test.domain(store);var task=Domain044Test.task("agent");
   var facts=new JSONObject().put("schema","persistent-java-agent-042-v1").put("status","awake").put("calibrated068",new JSONObject().put("originalLabels","x".repeat(51000)).put("modelSha256","actual-native-model"));
   domain.noteNative(task,"/api/agent042/state",facts);
   domain.accept(task,"agent.read",facts,new JSONObject().put("agent042Facts",new JSONObject().put("modelSha256","forged-host")));
   assertTrue(facts.similar(task.getJSONObject("context044").getJSONObject("agent042Facts")));
   assertTrue(facts.similar(task.getJSONObject("nativeReplies044").getJSONObject("/api/agent042/state")));
   assertFalse(task.getJSONObject("context044").toString().contains("forged-host"));
   assertThrows(IllegalStateException.class,()->ChatChain052.exact(facts,12500));
   var ordinary=new JSONObject().put("status","completed").put("large","x".repeat(51000));assertTrue(Domain044.compact(ordinary).getBoolean("largeSourceRetainedInJavaStore"));
  }
 }
 @Test public void actualSavedAgentSnapshotFitsPlanningAndKeepsFullReport()throws Exception{
  String fixture=System.getenv("FRAYNIX068_CHAT_SNAPSHOT"),text;
  if(fixture!=null)text=Files.readString(Path.of(fixture));else try(var in=new java.util.zip.GZIPInputStream(Files.newInputStream(Path.of("../docs/readmes/068/original/logs/agent-snapshot068.json.gz")))){text=new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);}
  var facts=new JSONObject(text);assertTrue(facts.toString().length()>50000);
  var task=Domain044Test.task("agent");task.getJSONObject("context044").put("agent042Facts",Domain044.compact(facts));
  var projection=ChatChain052.context(task);String prompt=ChatChain052.exact(projection,12500);
  var model=facts.getJSONObject("calibrated068").getJSONObject("lastCompleted").getJSONObject("model");
  assertTrue(prompt.contains(model.getString("sha256")));assertTrue(prompt.contains("rawBrier"));assertTrue(prompt.contains("calibratedBrier"));assertTrue(prompt.contains("pairedCalibratedMinus"));assertTrue(prompt.contains("trainingCosts"));
  var projectedStudy=projection.getJSONObject("agent042Facts").getJSONObject("calibrated068").getJSONObject("lastCompleted");var originalStudy=facts.getJSONObject("calibrated068").getJSONObject("lastCompleted");
  for(String phase:java.util.List.of("clean","disturbed","unsupported"))for(String name:java.util.List.of("methods","pairedCalibratedMinus")){
   var packed=projectedStudy.getJSONObject(phase).getJSONObject(name);var columns=projectedStudy.getJSONObject("tableColumns068").getJSONArray(packed.getString("columnsRef"));var shared=packed.getJSONObject("sharedIndexes");var decoded=new JSONObject();
   for(Object raw:packed.getJSONArray("rows")){var row=(JSONArray)raw;var values=new JSONObject();int cursor=1;for(int i=0;i<columns.length();i++)values.put(columns.getString(i),shared.has(""+i)?shared.get(""+i):row.get(cursor++));assertEquals(cursor,row.length());decoded.put(row.getString(0),values);}
   assertTrue(originalStudy.getJSONObject(phase).getJSONObject(name).similar(decoded));
  }
  projection.put("realisticRequestAndReceiptOverhead","x".repeat(2000));ChatChain052.exact(projection,12500);
  assertTrue(model.has("labels"));assertTrue(task.getJSONObject("context044").getJSONObject("agent042Facts").similar(facts));
  var report=AgentReport042.build(facts,WorldFrames.hash(facts));assertTrue(new JSONObject(report.getJSONObject("files").getString("snapshot.json")).similar(facts));
 }
}
