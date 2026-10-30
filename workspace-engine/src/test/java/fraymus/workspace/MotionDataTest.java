package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

public class MotionDataTest {
 static JSONObject contract() throws Exception {try(var in=MotionDataTest.class.getResourceAsStream("/goals/data-contract-v1.json")){return new JSONObject(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));}}
 static JSONObject input(JSONObject c){return new JSONObject().put("title",c.getString("id")).put("filename","fixture.csv").put("csv",c.getString("csv")).put("sourceKind","synthetic").put("sourceNote","Declared local numerical fixture, not sensor measurements").put("missingPolicy","reject").put("mapping",new JSONObject().put("timeColumn","time_ms").put("distanceColumn",c.getString("distanceUnit").equals("cm")?"distance_cm":"distance_mm").put("timeUnit",c.getString("timeUnit")).put("distanceUnit",c.getString("distanceUnit")));}
 @Test public void explicitUnitsAndUnequalIntervalsGiveKnownMotion() throws Exception {
  var c=contract().getJSONArray("cases").getJSONObject(0);var store=new MeasurementStore(Files.createTempDirectory("data-si-"));var d=store.importCsv(input(c));var a=MotionData.analyze(d,.3,2);var s=a.getJSONObject("summary");
  assertEquals(.4,s.getDouble("totalTravelM"),1e-12);assertEquals(.8,s.getDouble("meanSpeedMps"),1e-12);assertEquals(.4,s.getDouble("netDisplacementM"),1e-12);assertTrue(s.getBoolean("qualityPassed"));assertEquals(3,a.getJSONArray("intervals").length());assertEquals(.2,a.getJSONArray("intervals").getJSONObject(0).getDouble("velocityMps"),1e-12);assertEquals(1.2,a.getJSONArray("intervals").getJSONObject(2).getDouble("velocityMps"),1e-12);assertEquals(10.0/3,s.getDouble("peakAbsAccelerationMps2"),1e-12);
  assertEquals(c.getString("csv"),d.getString("csv"));assertEquals(MeasurementStore.hash(c.getString("csv")),a.getJSONObject("source").getString("sourceSha256"));
 }
 @Test public void reversalsAndQualityFailuresDoNotBecomePassingSourceQuality() throws Exception {
  var c=contract().getJSONArray("cases").getJSONObject(1);var store=new MeasurementStore(Files.createTempDirectory("data-reverse-"));var d=store.importCsv(input(c));var a=MotionData.analyze(d,.25,.4);var s=a.getJSONObject("summary");
  assertEquals(.18,s.getDouble("totalTravelM"),1e-12);assertEquals(.08,s.getDouble("netDisplacementM"),1e-12);assertEquals(.18,s.getDouble("meanSpeedMps"),1e-12);assertEquals(1,s.getInt("gapFlags"));assertEquals(1,s.getInt("speedFlags"));assertFalse(s.getBoolean("qualityPassed"));assertTrue(s.getBoolean("passed"));assertEquals(2,a.getJSONArray("flags").length());assertEquals(-.3,a.getJSONArray("intervals").getJSONObject(1).getDouble("velocityMps"),1e-12);
 }
 @Test public void offsetOriginAndDirectionAreExplicitAndAuditable() throws Exception {
  var c=contract().getJSONArray("cases").getJSONObject(0);var args=input(c);args.getJSONObject("mapping").put("timeOffsetS",.3).put("distanceOrigin",2).put("direction",-1);var store=new MeasurementStore(Files.createTempDirectory("data-frame-"));var d=store.importCsv(args);var a=MotionData.analyze(d,.3,2);
  assertEquals(.02,a.getJSONArray("rows").getJSONObject(0).getDouble("distanceM"),1e-12);assertEquals(-.38,a.getJSONArray("rows").getJSONObject(3).getDouble("distanceM"),1e-12);assertEquals(.3,a.getJSONArray("rows").getJSONObject(0).getDouble("timeS"),1e-12);assertEquals(-.8,a.getJSONObject("summary").getDouble("meanVelocityMps"),1e-12);
 }
 @Test public void sourceMappingOrNormalizedRowCorruptionRejects() throws Exception {
  var store=new MeasurementStore(Files.createTempDirectory("data-tamper-"));var d=store.importCsv(input(contract().getJSONArray("cases").getJSONObject(0)));
  var raw=new JSONObject(d.toString()).put("csv","changed");assertThrows(IllegalStateException.class,()->MotionData.analyze(raw,.3,2));var mapping=new JSONObject(d.toString());mapping.getJSONObject("mapping").put("distanceUnit","m");assertThrows(IllegalStateException.class,()->MotionData.analyze(mapping,.3,2));var row=new JSONObject(d.toString());row.getJSONArray("rows").getJSONObject(1).put("distanceM",999);assertThrows(IllegalStateException.class,()->MotionData.analyze(row,.3,2));
 }
 @Test public void aDerivedOverflowIsRejectedInsteadOfInventingMotion() throws Exception {
  var store=new MeasurementStore(Files.createTempDirectory("data-overflow-"));var args=input(contract().getJSONArray("cases").getJSONObject(0));args.put("csv","time_ms,distance_cm\n0,0\n1e-307,100\n");var d=store.importCsv(args);assertThrows(IllegalArgumentException.class,()->MotionData.analyze(d,.3,2));assertThrows(IllegalArgumentException.class,()->MotionData.criteria(Double.NaN,5));
 }
 @Test public void twoPointsHaveNoFabricatedAccelerationAndDrawTheirSingleVelocity() throws Exception {
  var store=new MeasurementStore(Files.createTempDirectory("data-two-"));var args=input(contract().getJSONArray("cases").getJSONObject(0)).put("csv","time_ms,distance_cm\n0,0\n100,2\n");var d=store.importCsv(args);var a=MotionData.analyze(d,.3,2);assertTrue(a.getJSONObject("summary").isNull("peakAbsAccelerationMps2"));assertEquals(0,a.getJSONArray("accelerations").length());var g=new JSONObject().put("id",UUID.randomUUID().toString()).put("analysis",a).put("maxGapS",.3).put("speedLimitMps",2);assertTrue(MotionData.report(g,d).getJSONObject("files").getString("index.html").contains("<circle"));
 }
 @Test public void explicitMissingRowSkipIsPreservedAndFlagged() throws Exception {
  var store=new MeasurementStore(Files.createTempDirectory("data-skip-"));var args=input(contract().getJSONArray("cases").getJSONObject(0)).put("csv","time_ms,distance_cm\n0,0\n100,\n200,10\n").put("missingPolicy","skip");var d=store.importCsv(args);var a=MotionData.analyze(d,.3,2);assertEquals(2,a.getJSONArray("rows").length());assertEquals(1,a.getJSONObject("summary").getInt("skippedSourceRows"));assertFalse(a.getJSONObject("summary").getBoolean("qualityPassed"));assertEquals(3,d.getJSONArray("skippedRows").getInt(0));var g=new JSONObject().put("id",UUID.randomUUID().toString()).put("analysis",a).put("maxGapS",.3).put("speedLimitMps",2);assertEquals(3,new JSONObject(MotionData.report(g,d).getJSONObject("files").getString("summary.json")).getJSONArray("skippedSourceRowNumbers").getInt(0));
 }
 @Test public void sourceTextIsEscapedAndOriginalCsvIsRetained() throws Exception {
  var root=Files.createTempDirectory("data-text-");var store=new MeasurementStore(root);var args=input(contract().getJSONArray("cases").getJSONObject(0)).put("title","<img src=x onerror=alert(1)>").put("sourceNote","<script>run()</script>");var d=store.importCsv(args);var a=MotionData.analyze(d,.3,2);var g=new JSONObject().put("id",UUID.randomUUID().toString()).put("analysis",a).put("maxGapS",.3).put("speedLimitMps",2);var files=MotionData.report(g,d).getJSONObject("files");assertEquals(d.getString("csv"),files.getString("original.csv"));assertTrue(files.getString("index.html").contains("&lt;script&gt;"));assertFalse(files.getString("index.html").contains("<script>run"));
 }
 @Test public void maximumRowCountProducesRealArtifactsWithinExistingBudgets() throws Exception {
  var root=Files.createTempDirectory("data-max-");var store=new MeasurementStore(root);var source=new StringBuilder("time_ms,distance_cm\n");for(int i=0;i<2048;i++)source.append(i*10).append(',').append(i*2).append('\n');var d=store.importCsv(input(contract().getJSONArray("cases").getJSONObject(0)).put("csv",source.toString()));var a=MotionData.analyze(d,.02,3);assertEquals(2048,a.getJSONObject("summary").getInt("rowCount"));var g=new JSONObject().put("id",UUID.randomUUID().toString()).put("analysis",a).put("maxGapS",.02).put("speedLimitMps",3);try(var artifacts=new WorkspaceStore(root)){var artifact=artifacts.writeOnce("max-fixture",MotionData.report(g,d));assertEquals(8,artifact.getJSONArray("files").length());}
 }
 @Test public void aDataGoalDoesNotAcquireOrCancelForeignNativePhysics() throws Exception {
  var root=Files.createTempDirectory("data-own-");var measurements=new MeasurementStore(root);var d=measurements.importCsv(input(contract().getJSONArray("cases").getJSONObject(0)));try(var artifacts=new WorkspaceStore(root);var scenes=new SceneStore(root);var brain=new BrainService(root);var goals=new GoalRunner(root,scenes,brain,artifacts,measurements)){
   var source=scenes.save(new JSONObject());scenes.control(new JSONObject().put("action","start").put("id",source.getString("id")).put("expectedVersion",1));scenes.control(new JSONObject().put("action","pause"));var before=scenes.snapshot();assertThrows(IllegalArgumentException.class,()->goals.startData(new JSONObject().put("datasetId",d.getString("datasetId")).put("datasetVersion",4294967297L)));assertThrows(IllegalArgumentException.class,()->goals.runProcedure(new JSONObject().put("name","some-procedure").put("version",4294967297L)));var g=goals.startData(new JSONObject().put("datasetId",d.getString("datasetId")).put("datasetVersion",1));assertFalse(goals.ownsPhysics());assertTrue(goals.busy());
   long until=System.currentTimeMillis()+5000;while(!goals.read(g.getString("id")).getString("status").equals("waiting-host")&&System.currentTimeMillis()<until)Thread.sleep(20);assertEquals("waiting-host",goals.read(g.getString("id")).getString("status"));goals.cancel(g.getString("id"));assertEquals(before.getString("id"),scenes.snapshot().getString("id"));assertEquals(before.getJSONObject("current").getInt("tick"),scenes.snapshot().getJSONObject("current").getInt("tick"));assertEquals(0,brain.state().getJSONArray("jobs").length());assertEquals(1,scenes.scenes().length());assertThrows(IllegalStateException.class,()->goals.saveProcedure(g.getString("id"),"unverified-data"));
  }
 }
}
