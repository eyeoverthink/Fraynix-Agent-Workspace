package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

public class MeasurementStoreTest {
    static JSONObject input(String csv) {
        return new JSONObject().put("title","Known trajectory").put("filename","observations.csv").put("csv",csv)
            .put("sourceKind","synthetic").put("sourceNote","Explicitly synthetic fixture; not instrument data").put("missingPolicy","reject")
            .put("mapping",new JSONObject().put("timeColumn","time").put("distanceColumn","distance").put("timeUnit","ms").put("distanceUnit","cm"));
    }
    static JSONObject binding(JSONObject data, JSONObject run, double tolerance) {
        return new JSONObject().put("datasetId",data.getString("datasetId")).put("datasetVersion",data.getInt("version")).put("runId",run.getString("id")).put("expectedSceneVersion",run.getInt("sceneVersion")).put("toleranceM",tolerance);
    }
    @Test public void quotedCsvUnitsOriginDirectionAndMissingRowsAreExplicit() throws Exception {
        var store=new MeasurementStore(Files.createTempDirectory("measure-import-"));
        var request=input("time,distance,note\r\n100,10,\"comma, and \"\"quote\"\"\"\r\n200,5,\"two\nlines\"\r\n300,,missing\r\n").put("missingPolicy","skip").put("uncertaintyM",.002);
        request.getJSONObject("mapping").put("timeOffsetS",-.1).put("distanceOrigin",10).put("direction",-1);
        var d=store.importCsv(request); assertEquals(2,d.getJSONArray("rows").length()); assertEquals(4,d.getJSONArray("skippedRows").getInt(0));
        assertEquals(0,d.getJSONArray("rows").getJSONObject(0).getDouble("timeS"),0); assertEquals(.05,d.getJSONArray("rows").getJSONObject(1).getDouble("distanceM"),1e-12);
        assertEquals(.002,d.getDouble("uncertaintyM"),0); assertEquals(MeasurementStore.hash(request.getString("csv")),d.getString("sourceSha256"));
        assertTrue(d.getBoolean("sourceLabelUserSupplied"));
    }
    @Test public void malformedAmbiguousNonfiniteAndOversizedDataAreRejected() throws Exception {
        var store=new MeasurementStore(Files.createTempDirectory("measure-invalid-"));
        for(String csv:List.of("time,time\n0,0\n1,1", "time,distance\n0,0\n0,1", "time,distance\n0,NaN\n1,1", "time,distance\n0,0\n1,1e999", "time,distance\n0,0\n1,", "time,distance\n0,\"unclosed\n1,1", "time,distance\n0,\"0\"x\n1,1", "time,distance\n0,0,extra\n1,1"))
            assertThrows(IllegalArgumentException.class,()->store.importCsv(input(csv)));
        var noUnits=input("time,distance\n0,0\n1,1"); noUnits.getJSONObject("mapping").put("timeUnit","guess");
        assertThrows(IllegalArgumentException.class,()->store.importCsv(noUnits));
        var wrong=input("time,distance\n0,0\n1,1"); wrong.getJSONObject("mapping").put("direction","1");
        assertThrows(IllegalArgumentException.class,()->store.importCsv(wrong));
        assertThrows(IllegalArgumentException.class,()->store.importCsv(input("x".repeat(262145))));
        assertEquals(0,store.datasets().length());
    }
    @Test public void immutableVersionsKeepExactSourcesAfterReimportAndRestart() throws Exception {
        var root=Files.createTempDirectory("measure-versions-"); var store=new MeasurementStore(root);
        var first=store.importCsv(input("time,distance\n0,0\n500,20")); String id=first.getString("datasetId");
        var update=input("time,distance\n0,0\n500,30").put("id",id).put("expectedVersion",1); var second=store.importCsv(update);
        assertEquals(2,second.getInt("version")); assertNotEquals(first.getString("id"),second.getString("id"));
        assertThrows(ConcurrentModificationException.class,()->store.importCsv(update));
        var restored=new MeasurementStore(root); assertEquals(2,restored.read(id,0).getInt("version")); assertEquals(first.toString(),restored.read(id,1).toString());
    }
    @Test public void independentlyComputedResidualsAndInterpolationMatchNativeRun() throws Exception {
        var root=Files.createTempDirectory("measure-residuals-"); var store=new MeasurementStore(root);
        try(var scenes=new SceneStore(root)) {
            var scene=scenes.save(new JSONObject()); var run=scenes.run(new JSONObject().put("id",scene.getString("id")).put("expectedVersion",1).put("seconds",.75));
            // Independent interpolation at 0.125 between recorded ticks 28 and 32; errors prescribed in metres.
            var samples=run.getJSONArray("samples"); double a=samples.getJSONObject(7).getDouble("distanceM"),b=samples.getJSONObject(8).getDouble("distanceM");
            double midpoint=a+(b-a)*.5, end=run.getJSONObject("final").getDouble("distanceM");
            double initial=samples.getJSONObject(0).getDouble("distanceM");
            var data=store.importCsv(input("time,distance\n0,"+(initial*100)+"\n125,"+((midpoint-.03)*100)+"\n750,"+((end+.04)*100)));
            var comparison=store.compare(binding(data,run,.05),scenes);
            assertEquals(.03,comparison.getJSONArray("points").getJSONObject(1).getDouble("residualM"),1e-10);
            assertEquals(-.04,comparison.getJSONArray("points").getJSONObject(2).getDouble("residualM"),1e-10);
            assertEquals(Math.sqrt((.03*.03+.04*.04)/3),comparison.getDouble("rmseM"),1e-10);
            assertEquals((.03-.04)/3,comparison.getDouble("meanResidualM"),1e-10); assertEquals(.04,comparison.getDouble("maxAbsErrorM"),1e-10);
            assertTrue(comparison.getBoolean("passed")); assertTrue(comparison.isNull("uncertaintyM"));
            assertFalse(store.compare(binding(data,run,.02),scenes).getBoolean("passed"));
        }
    }
    @Test public void uncoveredTimeAndStaleBindingsFailWhileHistoricalReceiptsSurvive() throws Exception {
        var root=Files.createTempDirectory("measure-bindings-"); var store=new MeasurementStore(root);
        try(var scenes=new SceneStore(root)) {
            var scene=scenes.save(new JSONObject()); var run=scenes.run(new JSONObject().put("id",scene.getString("id")).put("expectedVersion",1).put("seconds",.75));
            var outside=store.importCsv(input("time,distance\n0,0\n1000,100"));
            assertThrows(IllegalArgumentException.class,()->store.compare(binding(outside,run,.1),scenes));
            var data=store.importCsv(input("time,distance\n0,0\n500,50")); var result=store.compare(binding(data,run,1),scenes);
            store.importCsv(input("time,distance\n0,0\n500,60").put("id",data.getString("datasetId")).put("expectedVersion",1));
            assertThrows(ConcurrentModificationException.class,()->store.compare(binding(data,run,1),scenes));
            scenes.control(new JSONObject().put("action","reset")); scenes.save(new JSONObject().put("id",scene.getString("id")).put("expectedVersion",1).put("parameters",new JSONObject().put("angleDeg",10)));
            var current=store.read(data.getString("datasetId"),0);
            assertThrows(ConcurrentModificationException.class,()->store.compare(binding(current,run,1),scenes));
            assertEquals(result.toString(),new MeasurementStore(root).comparison(result.getString("id")).toString());
            assertTrue(store.report(result.getString("id")).getJSONObject("files").getString("index.html").contains("scene v1"));
        }
    }
    @Test public void reportEscapesUntrustedSourceTextAndRetainsOriginalCsvAndEvidence() throws Exception {
        var root=Files.createTempDirectory("measure-report-"); var store=new MeasurementStore(root);
        try(var scenes=new SceneStore(root);var artifacts=new WorkspaceStore(root)) {
            var scene=scenes.save(new JSONObject()); var run=scenes.run(new JSONObject().put("id",scene.getString("id")).put("expectedVersion",1).put("seconds",.5));
            var request=input("time,distance\n0,0\n500,50").put("title","<script>alert(1)</script>").put("sourceNote","<img src=x onerror=alert(1)>");
            var data=store.importCsv(request); var comparison=store.compare(binding(data,run,1),scenes); var files=store.report(comparison.getString("id")).getJSONObject("files");
            String html=files.getString("index.html"); assertFalse(html.contains("<script>")); assertFalse(html.contains("<img")); assertTrue(html.contains("&lt;script&gt;"));
            assertEquals(request.getString("csv"),files.getString("measurements.csv")); assertEquals(comparison.toString(),new JSONObject(files.getString("comparison.json")).toString());
            var artifact=artifacts.write(store.report(comparison.getString("id"))); assertEquals(3,artifact.getJSONArray("files").length());
        }
    }
    @Test public void reopenedComparisonRerunsNativePhysicsAndRejectsStaleReuseBeforeSideEffects() throws Exception {
        var root=Files.createTempDirectory("measure-repeat-");var store=new MeasurementStore(root);
        try(var scenes=new SceneStore(root)) {
            var scene=scenes.save(new JSONObject());var run=scenes.run(new JSONObject().put("id",scene.getString("id")).put("expectedVersion",1).put("seconds",.5));
            var data=store.importCsv(input("time,distance\n0,0\n500,50"));var first=store.compare(binding(data,run,1),scenes);
            var reopened=new MeasurementStore(root);var repeated=reopened.repeat(first.getString("id"),scenes);
            assertNotEquals(first.getString("id"),repeated.getString("id"));assertNotEquals(run.getString("id"),repeated.getString("runId"));
            assertEquals(first.getString("id"),repeated.getString("repeatedFrom"));assertEquals(first.getString("sourceSha256"),repeated.getString("sourceSha256"));
            assertEquals(120,scenes.runRecord(repeated.getString("runId")).getJSONObject("final").getInt("tick"));
            assertEquals(first.getDouble("rmseM"),repeated.getDouble("rmseM"),1e-6);assertEquals(first.toString(),reopened.comparison(first.getString("id")).toString());
            reopened.importCsv(input("time,distance\n0,0\n500,60").put("id",data.getString("datasetId")).put("expectedVersion",1));
            int before=scenes.runs(scene.getString("id")).length();
            assertThrows(ConcurrentModificationException.class,()->reopened.repeat(first.getString("id"),scenes));assertEquals(before,scenes.runs(scene.getString("id")).length());
        }
    }
}
