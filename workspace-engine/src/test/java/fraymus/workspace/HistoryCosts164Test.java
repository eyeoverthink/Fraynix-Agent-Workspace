package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;

public class HistoryCosts164Test {
 static JSONObject input(JSONObject w){return new JSONObject().put("worldId",w.getString("id")).put("expectedRevision",w.getInt("revision")).put("expectedHash",w.getString("hash"));}
 static JSONObject move(SmallStore161 s,JSONObject w,double x)throws Exception{var j=s.start(SmallBody161Test.request(w,x,.04));j=SmallBody161Test.waitFor(s,j.getString("id"));assertEquals(j.optString("reason"),"awaiting-report",j.getString("status"));s.acknowledge(new JSONObject().put("id",j.getString("id")).put("observed",SmallBody161Test.observed(j)));return s.world(w.getString("id"));}
 @Test(timeout=180000) public void nativeAndReportBreakdownPreservesHistoryAndRestart()throws Exception{
  Path root=Files.createTempDirectory("history164");String id;JSONObject frozen;
  try(var a=new WorkspaceStore(root);var s=new SmallStore161(root,a)){var w=s.create(SmallBody161Test.scene(.05,.04));id=w.getString("id");assertEquals(0,HistoryCosts164.measure(s,input(w)).getInt("revision"));w=move(s,w,.10);w=move(s,w,-.08);byte[] bytes=Files.readAllBytes(s.root);var measured=HistoryCosts164.measure(s,input(w));assertTrue(measured.getBoolean("passed"));assertEquals(2,measured.getJSONArray("rows").length());assertTrue(measured.getLong("nativeTicks")>0);assertTrue(measured.getLong("encodedReportBytes")>0);assertArrayEquals(bytes,Files.readAllBytes(s.root));assertTrue(w.similar(s.verifyWorld(id)));frozen=w;}
  try(var a=new WorkspaceStore(root);var s=new SmallStore161(root,a)){assertTrue(frozen.similar(s.verifyWorld(id)));assertTrue(HistoryCosts164.measure(s,input(frozen)).getBoolean("passed"));}
 }
 @Test(timeout=180000) public void staleAndForgedHistoryCannotProduceMeasurements()throws Exception{
  Path root=Files.createTempDirectory("history164-refuse");try(var a=new WorkspaceStore(root);var s=new SmallStore161(root,a)){var initial=s.create(SmallBody161Test.scene(.05,.04));var w=move(s,initial,.10);assertThrows(Exception.class,()->HistoryCosts164.measure(s,input(initial)));var disk=Files.readString(s.root);Files.writeString(s.root,"{}");assertThrows(Exception.class,()->HistoryCosts164.measure(s,input(w)));Files.writeString(s.root,disk);
   var j=s.find("jobs",w.getJSONArray("history").getJSONObject(0).getString("jobId"));var original=j.getJSONObject("run").toString();j.getJSONObject("run").getJSONObject("initialState").getJSONArray("bodies").getJSONObject(0).getJSONArray("position").put(0,.25);s.save();assertThrows(Exception.class,()->HistoryCosts164.measure(s,input(w)));j.put("run",new JSONObject(original));s.save();assertTrue(HistoryCosts164.measure(s,input(w)).getBoolean("passed"));
   var report=a.read(j.getJSONObject("report").getString("id"));a.write(new JSONObject().put("id",report.getString("id")).put("title",report.getString("title")).put("expectedVersion",report.getInt("version")).put("files",new JSONObject().put("index.html","changed")));assertThrows(Exception.class,()->HistoryCosts164.measure(s,input(w)));assertEquals(1,s.world(w.getString("id")).getInt("revision"));
  }
 }
 @Test public void pendingCapacityAndUnknownAuthorityRefuse()throws Exception{Path root=Files.createTempDirectory("history164-bounds");try(var a=new WorkspaceStore(root);var s=new SmallStore161(root,a)){var w=s.create(SmallBody161Test.scene(.05,.04));assertThrows(Exception.class,()->HistoryCosts164.measure(s,input(w).put("skipChecks",true)));var saved=s.find("worlds",w.getString("id"));saved.put("pending","pending");assertThrows(Exception.class,()->HistoryCosts164.measure(s,input(w)));saved.remove("pending");for(int i=0;i<9;i++)saved.getJSONArray("history").put(new JSONObject());assertThrows(Exception.class,()->HistoryCosts164.measure(s,input(w)));}}
}
