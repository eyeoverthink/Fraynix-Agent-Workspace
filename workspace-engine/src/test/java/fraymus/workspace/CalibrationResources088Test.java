package fraymus.workspace;

import java.time.*;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class CalibrationResources088Test {
    static final Instant NOW=Instant.parse("2026-11-07T12:00:00Z");
    static JSONObject profile(){return new JSONObject().put("cpus",4).put("maxHeap",768L*1024*1024).put("java","test");}
    static JSONObject sample(String id){return new JSONObject().put("id",id).put("kind","fit").put("millis",1000).put("at",NOW.toString()).put("shape",new JSONObject().put("profile",profile()).put("code","code"));}
    static JSONObject summary(JSONArray a){return CalibrationResources088.summarize(a,profile(),"code",NOW);}
    @Test public void emptyJournalDoesNotClaimSupport(){var x=summary(new JSONArray());assertEquals(0,x.getInt("matchingProfile"));assertFalse(x.has("supported"));assertFalse(x.has("admit"));}
    @Test public void cpuAndHeapChangesAreSeparated(){var a=sample("cpu");a.getJSONObject("shape").getJSONObject("profile").put("cpus",2);var b=sample("heap");b.getJSONObject("shape").getJSONObject("profile").put("maxHeap",512L*1024*1024);var x=summary(new JSONArray().put(sample("match")).put(a).put(b));assertEquals(1,x.getInt("matchingProfile"));assertEquals(2,x.getInt("differentProfile"));}
    @Test public void restoringProfileRestoresCountsWithoutMutation(){var a=new JSONArray().put(sample("a"));String before=a.toString();assertEquals(1,CalibrationResources088.summarize(a,profile().put("cpus",2),"code",NOW).getInt("differentProfile"));assertEquals(1,summary(a).getInt("matchingProfile"));assertEquals(before,a.toString());}
    @Test public void oldAndFutureMeasurementsAreExcluded(){var a=new JSONArray().put(sample("old").put("at",NOW.minus(Duration.ofDays(14)).minusSeconds(1).toString())).put(sample("future").put("at",NOW.plusSeconds(1).toString()));assertEquals(2,summary(a).getInt("staleOrFuture"));}
    @Test public void exactAgeBoundaryRemainsVisible(){assertEquals(1,summary(new JSONArray().put(sample("edge").put("at",NOW.minus(Duration.ofDays(14)).toString()))).getInt("matchingProfile"));}
    @Test public void codeAndReaderChangesAreExcluded(){var a=sample("code");a.getJSONObject("shape").put("code","old");var x=summary(new JSONArray().put(a).put(sample("mode").put("readerMode","reference")));assertEquals(1,x.getInt("differentCode"));assertEquals(1,x.getInt("differentReader"));}
    @Test public void checkIntervalsRequireBothNormalReaders(){var a=sample("task").put("kind","check").put("before",new JSONObject().put("mode","parallel")).put("after",new JSONObject().put("mode","reference"));assertEquals(1,summary(new JSONArray().put(a)).getInt("differentReader"));a.getJSONObject("after").put("mode","parallel");assertEquals(1,summary(new JSONArray().put(a)).getInt("matchingProfile"));}
    @Test public void malformedAndDuplicateEntriesNeverInflateCounts(){var a=new JSONArray().put(sample("a")).put(sample("a")).put("bad").put(sample("negative").put("millis",-1));var x=summary(a);assertEquals(1,x.getInt("matchingProfile"));assertEquals(3,x.getInt("invalidOrDuplicate"));}
    @Test(expected=IllegalArgumentException.class) public void oversizedJournalRefused(){var a=new JSONArray();for(int i=0;i<65;i++)a.put(sample("s"+i));summary(a);}
    @Test public void outputProfileDoesNotAliasInput(){var p=profile();var x=CalibrationResources088.summarize(new JSONArray(),p,"code",NOW);x.getJSONObject("profile").put("cpus",1);assertEquals(4,p.getInt("cpus"));}
}
