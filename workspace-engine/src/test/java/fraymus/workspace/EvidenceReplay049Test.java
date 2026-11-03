package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

public class EvidenceReplay049Test {
    static JSONObject fixture(String name)throws Exception{
        return new JSONObject(Files.readString(Path.of("../examples/evidence-049",name+".json")));
    }
    static String status(JSONObject result){return result.getJSONArray("associations").getJSONObject(0).getString("status");}
    @Test public void frozenSixCasesRetainEvidenceKinds()throws Exception{
        var expected=Map.of("visible","measured-crossing-supported","occluded","prediction-near-witness",
            "shifted","witness-only","gap","witness-only","ambiguous","ambiguous");
        var results=new JSONObject();
        for(var test:expected.entrySet()){
            var recording=fixture(test.getKey());String original=recording.toString();
            var analysis=EvidenceReplay049.analyze(recording);
            assertEquals(test.getKey(),test.getValue(),status(analysis));
            assertEquals(original,recording.toString());
            results.put(test.getKey(),analysis);
        }
        var baseline=EvidenceReplay049.analyze(fixture("initial-blocked"));
        assertEquals(0,baseline.getJSONArray("associations").length());
        assertEquals(1,baseline.getJSONObject("counts").getInt("initialBlockedBaselines"));
        results.put("initial-blocked",baseline);
        Files.createDirectories(Path.of("build/reports"));
        Files.writeString(Path.of("build/reports/evidence-049.json"),results.toString(2)+"\n");
    }
    @Test public void clocksOffsetAndDriftChangeAlignmentWithoutRewritingSource()throws Exception{
        var recording=fixture("visible");
        var clock=recording.getJSONArray("sources").getJSONObject(1).getJSONObject("clock");
        clock.put("deviceEndMs",10810);
        for(Object raw:recording.getJSONArray("events")){
            var e=(JSONObject)raw;
            if(e.getString("sourceId").equals("beam"))e.put("deviceMs",800+(e.getDouble("deviceMs")-800)*1.001);
        }
        var result=EvidenceReplay049.analyze(recording);
        assertEquals("measured-crossing-supported",status(result));
        assertEquals(2795,result.getJSONArray("associations").getJSONObject(0).getDouble("fromMs"),1e-8);
        assertEquals(2905,result.getJSONArray("associations").getJSONObject(0).getDouble("toMs"),1e-8);
        assertEquals(.16,result.getJSONArray("displacement").getJSONObject(0).getDouble("distanceM"),1e-12);
        assertEquals(.004,result.getJSONArray("displacement").getJSONObject(0).getDouble("declaredUncertaintyM"),1e-12);
    }
    @Test public void aBeamGapDoesNotInventAnObservedTransition()throws Exception{
        var input=fixture("visible");input.getJSONArray("events").getJSONObject(6).put("seq",4);
        var result=EvidenceReplay049.analyze(input);
        assertEquals("gap-before-block",status(result));assertEquals(2,result.getJSONObject("counts").getInt("droppedSequenceNumbers"));
        assertEquals(1,result.getJSONObject("counts").getInt("unobservedBeamTransitions"));
    }
    @Test public void multipleBeamTransitionsCannotReuseOneCameraIntervalAsUniqueSupport()throws Exception{
        var input=fixture("visible");var events=input.getJSONArray("events");
        events.put(new JSONObject().put("sourceId","beam").put("seq",3).put("deviceMs",3750).put("state","clear"));
        events.put(new JSONObject().put("sourceId","beam").put("seq",4).put("deviceMs",3780).put("state","blocked"));
        var result=EvidenceReplay049.analyze(input);
        assertEquals(2,result.getJSONArray("associations").length());
        for(Object raw:result.getJSONArray("associations"))assertEquals("ambiguous",((JSONObject)raw).getString("status"));
    }
    @Test public void missingObservationsAndTimeGapsDoNotBecomeMeasuredCrossings()throws Exception{
        var input=fixture("visible");var events=input.getJSONArray("events");
        events.put(1,new JSONObject().put("sourceId","camera").put("seq",1).put("deviceMs",2000).put("state","missing"));
        var result=EvidenceReplay049.analyze(input);
        assertEquals("witness-only",status(result));assertEquals(1,result.getJSONObject("counts").getInt("reacquisitionTransitions"));
        assertEquals(1,result.getJSONObject("counts").getInt("missing"));
        input=fixture("visible");input.getJSONArray("sources").getJSONObject(0).put("periodMs",100);
        result=EvidenceReplay049.analyze(input);assertEquals("witness-only",status(result));assertEquals(3,result.getJSONArray("gaps").length());
    }
    @Test public void sharedClockAndUncertainPositionsDoNotEarnIndependentSupport()throws Exception{
        var input=fixture("visible");input.getJSONArray("sources").getJSONObject(1).put("clockId","host-test-clock");
        assertEquals("shared-clock",status(EvidenceReplay049.analyze(input)));
        input=fixture("visible");input.getJSONArray("events").getJSONObject(1).put("uncertaintyM",.05);
        assertEquals("witness-only",status(EvidenceReplay049.analyze(input)));
    }
    @Test public void invalidClocksSequencesUnitsAndPredictionsAreRefused()throws Exception{
        var reset=fixture("visible");reset.getJSONArray("events").getJSONObject(2).put("deviceMs",1500);
        assertThrows(IllegalArgumentException.class,()->EvidenceReplay049.analyze(reset));
        var outside=fixture("visible");outside.getJSONArray("events").getJSONObject(3).put("deviceMs",11000);
        assertThrows(IllegalArgumentException.class,()->EvidenceReplay049.analyze(outside));
        var units=fixture("visible");units.getJSONObject("frame").put("unit","inch");
        assertThrows(IllegalArgumentException.class,()->EvidenceReplay049.analyze(units));
        var future=fixture("occluded");future.getJSONArray("events").getJSONObject(1).put("issuedDeviceMs",2000);
        assertThrows(IllegalArgumentException.class,()->EvidenceReplay049.analyze(future));
        var numeric=fixture("visible");numeric.getJSONArray("events").getJSONObject(0).put("xM","0.02");
        assertThrows(IllegalArgumentException.class,()->EvidenceReplay049.analyze(numeric));
        var duplicate=fixture("visible");duplicate.getJSONArray("events").getJSONObject(1).put("seq",0);
        assertThrows(IllegalArgumentException.class,()->EvidenceReplay049.analyze(duplicate));
    }
    @Test public void independentInputOrderingDoesNotAffectAnalysis()throws Exception{
        var input=fixture("ambiguous");var ordered=EvidenceReplay049.analyze(input);
        var events=input.getJSONArray("events");var shuffled=new JSONArray();
        // Interleave independent sources while preserving each source's sequence.
        int[] order={0,7,4,1,8,5,2,9,6,3,10};
        for(int index:order)shuffled.put(events.getJSONObject(index));input.put("events",shuffled);
        assertTrue(ordered.similar(EvidenceReplay049.analyze(input)));
    }
    @Test public void storeRestoresExportsAndRefusesTamperedOriginal()throws Exception{
        Path project=Files.createTempDirectory("evidence049-test-");
        var store=new EvidenceStore049(project);var recording=fixture("occluded");
        var result=store.importText(recording.toString());String id=result.getString("id");
        Path file=project.resolve("evidence-049").resolve(id+".json");byte[] original=Files.readAllBytes(file);
        assertTrue(store.importText(recording.toString(2)).getBoolean("duplicate"));assertArrayEquals(original,Files.readAllBytes(file));
        var restored=new EvidenceStore049(project);
        assertTrue(recording.similar(new JSONObject(restored.export(id).getString("text"))));
        assertEquals(status(result),status(restored.read(id)));
        assertThrows(IllegalArgumentException.class,()->restored.read("../foreign"));
        assertThrows(IllegalArgumentException.class,()->store.importText(" ".repeat(EvidenceReplay049.MAX_BYTES+1)));
        var altered=new JSONObject(new String(original,java.nio.charset.StandardCharsets.UTF_8));
        altered.getJSONObject("recording").getJSONArray("events").getJSONObject(0).put("xM",.03);
        Files.writeString(file,altered.toString());byte[] damaged=Files.readAllBytes(file);
        assertThrows(IllegalStateException.class,()->restored.read(id));
        assertThrows(IllegalStateException.class,()->restored.importText(recording.toString()));
        assertFalse(restored.list().getJSONArray("recordings").getJSONObject(0).getBoolean("available"));
        assertArrayEquals(damaged,Files.readAllBytes(file));
    }
}
