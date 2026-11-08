package fraymus.workspace;

import org.json.*;
import org.junit.*;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.function.Consumer;
import static org.junit.Assert.*;

/** Optional native-evidence suite, supplied by the completed isolated094 UI study. */
public class RecoveryAudit094Test {
 @Test public void recoveredNativeEvidenceRejectsBindingSourceChronologyAndCostChanges() throws Exception {
  String fixture=System.getenv("FRAYNIX_RECOVERY094_PROJECT");Assume.assumeTrue("Run recovery094-integration before this native-evidence probe",fixture!=null);
  Path root=Path.of(fixture);System.out.println("RECOVERY094_NATIVE_COPY "+root);
  try(var runner=new WorldProcedure079(root,new WorldLedger(root))){
   JSONObject job=null;try(var dirs=Files.list(root.resolve("world-procedures-079"))){for(Path d:dirs.filter(Files::isDirectory).toList())if(Files.exists(d.resolve("goal.json"))){var j=AgentFiles042.loadChecked(d.resolve("goal.json"));if(j.getString("status").equals("ready")&&j.getInt("resumes")==2)job=j;}}
   assertNotNull("The twice-resumed native goal is required",job);final JSONObject original=job;String id=job.getString("id");assertTrue(runner.audit(id).getBoolean("passed"));
   String family=job.getJSONObject("arm").getString("selected");var p=job.getJSONObject("arm").getJSONObject("programs").getJSONObject(family);assertEquals(3,p.getJSONArray("actual").length());assertEquals(2,p.getJSONObject("reforecasts093").length());
   for(String step:List.of("1","2")){String forecast=p.getJSONObject("reforecasts093").getJSONObject(step).getString("forecastId");
   List<Consumer<JSONObject>> mutations=new ArrayList<>();
   for(String key:List.of("originalForecastId","originalForecastSha256","previousActualId","previousActualSha256","resumedAncestorId","resumedAncestorSha256","frameSha256","planSha256","forecastId"))mutations.add(j->replacement(j,family,step).put(key,"changed"));
   mutations.add(j->replacement(j,family,step).put("extra",true));
   mutations.add(j->replacement(j,family,step).put("step",0));
   mutations.add(j->j.getJSONObject("arm").getJSONObject("programs").getJSONObject(family).remove("reforecasts093"));
   mutations.add(j->attempt(j,forecast).put("settledAt","2099-01-01T00:00:00Z"));
   mutations.add(j->attempt(j,forecast).put("plannedAt","2000-01-01T00:00:00Z"));
   mutations.add(j->attempt(j,forecast).put("sourceSha256","changed"));
   mutations.add(j->attempt(j,forecast).put("planSha256","changed"));
   mutations.add(j->attempt(j,forecast).put("admissionRemaining",1));
   mutations.add(j->attempt(j,forecast).put("admissionSpent",0));
   mutations.add(j->attempt(j,forecast).put("chargedTicks",0));
   mutations.add(j->attempt(j,forecast).put("unknownTail",!attempt(original,forecast).optBoolean("unknownTail")));
   mutations.add(j->attempt(j,forecast).getJSONObject("metrics").put("passed",false));
   mutations.add(j->attempt(j,forecast).put("status","failed"));
   mutations.add(j->j.put("resumes",0));
   mutations.add(j->j.getJSONObject("arm").getJSONObject("programs").getJSONObject(family).getJSONObject("waypoints").getJSONObject("reforecast/"+step).put("candidateChecks",0));
   for(Consumer<JSONObject> mutation:mutations){var changed=WorldFrames.copy(original);mutation.accept(changed);assertThrows(Exception.class,()->runner.audit(changed));}
   var file=root.resolve("world-procedures-079").resolve(id).resolve("attempts").resolve(forecast).resolve("original.json");byte[] bytes=Files.readAllBytes(file);FileTime time=Files.getLastModifiedTime(file);
   try{String text=new String(bytes,java.nio.charset.StandardCharsets.UTF_8);int at=text.indexOf("\"sha256\":\"")+10;assertTrue(at>9);char c=text.charAt(at);String altered=text.substring(0,at)+(c=='0'?'1':'0')+text.substring(at+1);Files.writeString(file,altered);Files.setLastModifiedTime(file,time);assertEquals(bytes.length,Files.size(file));assertThrows(Exception.class,()->runner.audit(id));}finally{Files.write(file,bytes);Files.setLastModifiedTime(file,time);}
   assertTrue(runner.audit(id).getBoolean("passed"));System.out.println("RECOVERY094_TAMPER_REFUSALS "+mutations.size()+" plus warm original-byte corruption at step "+step);}
  }
  try(var cold=new WorldProcedure079(root,new WorldLedger(root))){int verified=0;try(var dirs=Files.list(root.resolve("world-procedures-079"))){for(Path d:dirs.filter(Files::isDirectory).toList())if(Files.exists(d.resolve("goal.json"))){var j=AgentFiles042.loadChecked(d.resolve("goal.json"));if(j.getString("status").equals("ready")){assertTrue(cold.audit(j.getString("id")).getBoolean("passed"));verified++;}}}assertEquals(7,verified);System.out.println("RECOVERY094_COLD_FULL_AUDITS "+verified);}
 }
 static JSONObject replacement(JSONObject j,String family,String step){return j.getJSONObject("arm").getJSONObject("programs").getJSONObject(family).getJSONObject("reforecasts093").getJSONObject(step);}
 static JSONObject attempt(JSONObject j,String id){for(Object raw:j.getJSONObject("arm").getJSONArray("attempts")){var a=(JSONObject)raw;if(a.getString("id").equals(id))return a;}throw new AssertionError("Missing attempt");}
}
