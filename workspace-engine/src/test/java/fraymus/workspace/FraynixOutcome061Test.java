package fraymus.workspace;
import org.junit.Test;
import static org.junit.Assert.*;
import org.json.*;
import java.nio.file.*;
import java.util.*;

public class FraynixOutcome061Test {
 @Test public void actualNativeTrainingFrozenEvaluationColdReloadAndTamperRefusal()throws Exception{
  Path p=Files.createTempDirectory("fraynix-outcome061-");String id;JSONObject finished;
  try(var b=new BrainService(p)){
   var job=b.start(new JSONObject().put("kind","fraynix-outcomes").put("autoPromote",false));id=job.getString("id");
   long limit=System.nanoTime()+180_000_000_000L;
   while(b.state().getBoolean("busy")&&System.nanoTime()<limit)Thread.sleep(100);
   finished=b.read(id);Files.writeString(p.resolve("original-experiment.json"),finished.toString());
   assertEquals(finished.optString("reason"),"completed",finished.getString("status"));assertEquals(36,finished.getInt("progress"));
   assertEquals(24,finished.getJSONArray("outcomes061").length());assertEquals(12,finished.getJSONArray("reserved061").length());
   FraynixOutcome061.audit(finished);assertEquals(1,b.activePolicy().getInt("version"));
   var invalid=WorldFrames.copy(finished);var row=invalid.getJSONArray("reserved061").getJSONObject(0).getJSONObject("record");row.getJSONObject("final").put("distanceM",99);
   try{FraynixOutcome061.audit(invalid);fail();}catch(IllegalStateException expected){}
   invalid=WorldFrames.copy(finished);invalid.getJSONArray("outcomes061").put(1,invalid.getJSONArray("outcomes061").get(0));
   try{FraynixOutcome061.audit(invalid);fail();}catch(IllegalStateException expected){}
   var request=new JSONObject().put("jobId",id).put("definition",FraynixOutcome061.definitions(false).getJSONObject(0));
   Files.writeString(p.resolve("prediction.json"),b.outcomePredict061(request).toString());
  }
  try(var b=new BrainService(p)){
   var request=new JSONObject().put("jobId",id).put("definition",FraynixOutcome061.definitions(false).getJSONObject(0));
   assertTrue(new JSONObject(Files.readString(p.resolve("prediction.json"))).similar(b.outcomePredict061(request)));
   assertEquals(finished.getString("modelSha256061"),b.outcomeState061().getString("modelSha256"));
   request.getJSONObject("definition").put("massKg",2);try{b.outcomePredict061(request);fail();}catch(IllegalArgumentException expected){}
  }
  System.out.println("EDITION061_NATIVE_ORIGINALS="+p);System.out.println(finished.getJSONObject("outcomeEvaluation061"));
 }
 @Test public void cancellationKeepsPartialEvidenceAndNoControllerChange()throws Exception{
  Path p=Files.createTempDirectory("fraynix-outcome061-stop-");
  try(var b=new BrainService(p)){
   String id=b.start(new JSONObject().put("kind","fraynix-outcomes")).getString("id");b.cancel(id);
   long deadline=System.nanoTime()+30_000_000_000L;while(b.state().getBoolean("busy")&&System.nanoTime()<deadline)Thread.sleep(50);
   assertEquals("cancelled",b.read(id).getString("status"));assertFalse(b.outcomeState061().getBoolean("available"));assertEquals(1,b.activePolicy().getInt("version"));
  }
 }
 @Test public void chatRoutesToActualRuntimeFacts(){assertEquals("agent",ChatPolicy044.route("Explain the FraynixGPT outcome comparison",new JSONObject(),false));}
}
