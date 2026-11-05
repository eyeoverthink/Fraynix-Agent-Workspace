package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

public class Calibration068Test {
    static JSONObject label(int hits,boolean success,int id){
        return new JSONObject().put("task",new JSONObject().put("syntheticUnitLabel",id)).put("screen",new JSONObject().put("successFraction",hits/9.0))
            .put("record",new JSONObject().put("sha256","algorithm-unit-label-"+id)).put("outcome",new JSONObject().put("executed",true).put("success",success));
    }
    static JSONObject model(){var rows=new JSONArray();for(int i=0;i<8;i++)rows.put(label(i<4?0:9,i>=4,i));return ValueCalibration068.fit(rows);}
    @Test public void weightedPoolingInterpolationAndEmptyData(){
        var rows=new JSONArray();for(int i=0;i<8;i++)rows.put(label(i<4?0:9,i<4,i));var m=ValueCalibration068.fit(rows);
        assertEquals(1,m.getJSONArray("blocks").length());assertEquals(.5,ValueCalibration068.predict(m,0),1e-12);assertEquals(.5,ValueCalibration068.predict(m,1),1e-12);
        m=model();assertEquals(1.0/6,ValueCalibration068.predict(m,0),1e-12);assertEquals(5.0/6,ValueCalibration068.predict(m,1),1e-12);assertEquals(.5,ValueCalibration068.predict(m,.5),1e-12);
        var single=ValueCalibration068.fit(new JSONArray().put(label(3,true,1)));assertEquals(2.0/3,ValueCalibration068.predict(single,0),1e-12);assertEquals(2.0/3,ValueCalibration068.predict(single,1),1e-12);
        assertThrows(IllegalArgumentException.class,()->ValueCalibration068.fit(new JSONArray()));assertThrows(IllegalArgumentException.class,()->ValueCalibration068.predict(model(),Double.NaN));
        var bad=model();bad.getJSONArray("knots").getJSONObject(0).put("rate",1);assertThrows(IllegalStateException.class,()->ValueCalibration068.verify(bad));
    }
    @Test public void prepushChoicesKeepEarlierPoliciesAndNoFutureData(){
        var model=model();var oldHashes=new HashSet<String>();for(Object raw:TemporalStudy067.cases(false))oldHashes.add(WorldFrames.hash((JSONObject)raw));var all=new HashSet<String>();
        for(String phase:List.of("calibration","clean","disturbed","unsupported")){
            var cases=CalibrationStudy068.cases(phase);assertEquals(phase.equals("unsupported")?12:48,cases.length());
            for(Object raw:cases){var task=((JSONObject)raw).getJSONObject("task");String hash=WorldFrames.hash(task);assertTrue(all.add(hash));assertFalse(oldHashes.contains(hash));
                var original=TemporalPolicy067.initial(task);var planned=CalibratedPolicy068.initial(task,model);
                for(String name:List.of("simple","temporal","value"))assertTrue(original.getJSONObject("decisions").getJSONObject(name).similar(planned.getJSONObject("decisions").getJSONObject(name)));
                var d=planned.getJSONObject("decisions").getJSONObject("calibrated");if(phase.equals("unsupported")){assertFalse(d.getBoolean("execute"));assertFalse(d.has("screen"));}
                if(d.has("calibration068")){var s=d.getJSONObject("calibration068");assertEquals(3*s.getDouble("probability")-2-.147,s.getDouble("estimatedUtility"),1e-12);assertEquals(s.getBoolean("attempt"),d.getBoolean("execute"));assertEquals(model.getString("sha256"),s.getString("modelSha256"));}
            }
        }
        var c=Stopping063.definition(20,1.05,.24);var samples=Temporal067Test.prefix(c);var d=new JSONObject().put("method","calibrated").put("speedMps",.24);var t=Temporal067Test.task(.13,.045);
        var before=DisturbanceTrial068.plannerState(t,d,c,samples);samples.put(new JSONObject().put("futureEndpoint",999).put("disturbance","spin-after"));assertTrue(before.similar(DisturbanceTrial068.plannerState(t,d,c,samples)));
        assertFalse(CalibratedPolicy068.initial(t,model).toString().contains("spin-after"));assertEquals("agent",ChatPolicy044.route("Inspect recovery calibration and unfamiliar disturbances",new JSONObject(),false));
    }
    @Test public void lookupCostAndUnsupportedRefusalAreExplicit(){
        var t=Temporal067Test.task(.13,.012);var d=new JSONObject().put("execute",true).put("speedMps",.24).put("method","calibrated").put("screen",new JSONObject()).put("calibration068",new JSONObject());
        var r=new JSONObject().put("summary",new JSONObject().put("endpointM",.13)).put("timing068",new JSONObject().put("nativeWallMillis",5)).put("externalReceipts",new JSONArray().put(new JSONObject())).put("correctionPlan",new JSONObject().put("correct",true).put("deltaVelocityMps",.04));
        var o=CalibratedPolicy068.outcome(t,d,r);assertEquals(.853,o.getDouble("utility"),1e-12);assertEquals(3,o.getInt("observations"));assertEquals(1,o.getInt("lookups"));assertEquals(1,o.getInt("externalEvents"));assertEquals(2,o.getInt("actions"));
        d.put("execute",false);o=CalibratedPolicy068.outcome(t,d,null);assertEquals(-.157,o.getDouble("utility"),1e-12);assertEquals(0,o.getInt("externalEvents"));assertFalse(o.has("endpointM"));
        d.remove("screen");d.remove("calibration068");assertEquals(-.15,CalibratedPolicy068.outcome(t,d,null).getDouble("utility"),1e-12);
    }
    @Test public void independentLabelsReliabilityAndBothGates(){
        var rows=new JSONArray();for(int i=0;i<2;i++){var ds=new JSONObject().put("value",new JSONObject().put("screen",new JSONObject().put("successFraction",i))).put("calibrated",new JSONObject().put("calibration068",new JSONObject().put("probability",.5)));
            rows.put(new JSONObject().put("plan",new JSONObject().put("decisions",ds)).put("results",new JSONObject().put("temporal",new JSONObject().put("outcome",new JSONObject().put("executed",true).put("success",i==0)))));}
        var metrics=CalibrationStudy068.calibrationMetrics(rows);assertEquals(1,metrics.getDouble("rawBrier"),1e-12);assertEquals(.25,metrics.getDouble("calibratedBrier"),1e-12);assertEquals(0,metrics.getDouble("calibratedECE"),1e-12);
        var e=new JSONObject().put("calibration",new JSONObject().put("count",20).put("rawBrier",.3).put("calibratedBrier",.28)).put("pairedCalibratedMinus",new JSONObject().put("value",new JSONObject().put("meanDelta",0))).put("methods",new JSONObject().put("calibrated",new JSONObject().put("allTaskSuccessRate",.4).put("allTaskMissRate",.15)));
        assertTrue(CalibrationStudy068.qualifies(e,"clean"));assertTrue(CalibrationStudy068.qualifies(e,"disturbed"));assertFalse(CalibrationStudy068.qualifies(e,"unsupported"));
        var failed=WorldFrames.copy(e);failed.getJSONObject("calibration").put("calibratedBrier",.295);assertFalse(CalibrationStudy068.qualifies(failed,"clean"));
        failed=WorldFrames.copy(e);failed.getJSONObject("pairedCalibratedMinus").getJSONObject("value").put("meanDelta",-.011);assertFalse(CalibrationStudy068.qualifies(failed,"clean"));assertFalse(CalibrationStudy068.qualifies(failed,"disturbed"));
        failed=WorldFrames.copy(e);failed.getJSONObject("methods").getJSONObject("calibrated").put("allTaskMissRate",.26);assertFalse(CalibrationStudy068.qualifies(failed,"disturbed"));
    }
    @Test public void nativeCalibrationThenUnfamiliarDisturbancesColdRestoreStopAndTamper()throws Exception{
        String resume=System.getenv("FRAYNIX068_NATIVE_RESUME");var root=resume==null?Files.createTempDirectory("fraynix-calibration068-"):Path.of(resume);System.out.println("EDITION068_NATIVE_ROOT="+root);String id,hash;JSONObject job;
        try(var brain=new BrainService(root)){
            if(resume==null){id=brain.start(new JSONObject().put("kind","calibrated-recovery").put("autoPromote",false)).getString("id");Recovery066Test.waitFor(brain);}else id=new JSONObject(Files.readString(root.resolve("original-experiment.json"))).getString("id");job=brain.read(id);
            if(!Files.exists(root.resolve("original-attempt.json")))Files.writeString(root.resolve("original-attempt.json"),job.toString());assertEquals(job.optString("reason"),"completed",job.getString("status"));CalibrationStudy068.audit(job);
            if(Files.exists(root.resolve("original-experiment.json")))assertTrue(new JSONObject(Files.readString(root.resolve("original-experiment.json"))).similar(job));else Files.writeString(root.resolve("original-experiment.json"),job.toString());hash=WorldFrames.hash(job);
            System.out.println("CALIBRATION068_CLEAN="+job.getJSONObject("cleanEvaluation068"));System.out.println("CALIBRATION068_DISTURBED="+job.getJSONObject("disturbedEvaluation068"));
            assertEquals(484,job.getInt("progress"));assertEquals(48,job.getJSONArray("calibration068").length());assertEquals(1,brain.activePolicy().getInt("version"));
            assertEquals(0,job.getJSONObject("disturbedEvaluation068").getJSONObject("diagnostics").getInt("acceptedTemporalDisagreements"));
            var found=new HashSet<String>();for(Object raw:job.getJSONArray("disturbed068")){var row=(JSONObject)raw;var record=row.getJSONObject("results").getJSONObject("temporal").getJSONObject("record");assertEquals(1,record.getJSONArray("externalReceipts").length());found.add(row.getString("disturbance"));}
            assertEquals(new HashSet<>(CalibrationStudy068.FAMILIES),found);
            var event=job.getJSONArray("disturbed068").getJSONObject(0).getJSONObject("results").getJSONObject("temporal").getJSONObject("record");
            var corrupt=WorldFrames.copy(event);corrupt.remove("sha256");corrupt.getJSONArray("externalReceipts").getJSONObject(0).getJSONObject("after").getJSONArray("velocity").put(0,999);corrupt.put("sha256",WorldFrames.hash(corrupt));assertThrows(IllegalStateException.class,()->DisturbanceTrial068.verify(corrupt));
            var altered=WorldFrames.copy(job);altered.getJSONObject("calibrationModel068").getJSONArray("knots").getJSONObject(0).put("rate",.999);assertThrows(IllegalStateException.class,()->CalibrationStudy068.audit(altered));
            var leaked=WorldFrames.copy(job);leaked.put("modelFrozenAt","2099-01-01T00:00:00Z");assertThrows(IllegalStateException.class,()->CalibrationStudy068.audit(leaked));
            var query=new JSONObject().put("jobId",id).put("index",0).put("phase","disturbed");var trace=brain.calibrationTrace068(query);Files.writeString(root.resolve("trace.json"),trace.toString());assertEquals(46,trace.getJSONObject("results").getJSONObject("temporal").getJSONArray("series").length());
            var original=new JSONObject().put("agent042Facts",new JSONObject().put("calibrated068",brain.calibrationState068()));var projection=WorldFrames.copy(original);CalibrationContext068.project(projection);assertTrue(projection.toString().length()<11000);
            assertTrue(original.getJSONObject("agent042Facts").getJSONObject("calibrated068").getJSONObject("lastCompleted").getJSONObject("model").has("labels"));
            assertFalse(projection.getJSONObject("agent042Facts").getJSONObject("calibrated068").getJSONObject("lastCompleted").getJSONObject("model").has("labels"));
            assertEquals(hash,WorldFrames.hash(brain.read(id)));
        }
        try(var brain=new BrainService(root)){assertEquals(hash,WorldFrames.hash(brain.read(id)));assertTrue(new JSONObject(Files.readString(root.resolve("trace.json"))).similar(brain.calibrationTrace068(new JSONObject().put("jobId",id).put("index",0).put("phase","disturbed"))));
            String stop=brain.start(new JSONObject().put("kind","calibrated-recovery")).getString("id");brain.cancel(stop);Recovery066Test.waitFor(brain);assertEquals("cancelled",brain.read(stop).getString("status"));assertEquals(id,brain.calibrationState068().getJSONObject("lastCompleted").getString("jobId"));}
    }
}
