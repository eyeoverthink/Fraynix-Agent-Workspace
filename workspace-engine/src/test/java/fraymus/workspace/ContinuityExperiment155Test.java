package fraymus.workspace;

import java.nio.file.*;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ContinuityExperiment155Test {
    private static JSONObject settled(ContinuityExperiment155 service,String id)throws Exception{
        for(int i=0;i<6000;i++){var state=service.status(id);if(!state.getString("status").equals("running"))return state;Thread.sleep(10);}throw new AssertionError("Background calculation exceeded test bound");
    }
    @Test public void backgroundProgressBoundedConcurrencyAndDetachedStatus()throws Exception{
        try(var f=new StartingReference150Test.Fixture();var service=new ContinuityExperiment155(f.root,f.observations,f.association,new ReplayEvidence151(f.root,f.association))){
            var job=service.start(f.audit.getString("id"),false);String id=job.getString("jobId");assertThrows(IllegalStateException.class,()->service.start(f.audit.getString("id"),false));job.put("status","completed");
            var done=settled(service,id);assertEquals(done.toString(),"completed",done.getString("status"));assertEquals(8,done.getInt("completedFrames"));
            var original=done.getJSONObject("result").getJSONObject("experiment");assertTrue(original.getJSONArray("trace").getJSONObject(0).getBoolean("continuityPreserved"));done.getJSONObject("result").put("experiment",new JSONObject());assertTrue(original.similar(service.status(id).getJSONObject("result").getJSONObject("experiment")));
            var verify=settled(service,service.start(original.getString("id"),true).getString("jobId"));assertEquals("completed",verify.getString("status"));assertTrue(original.similar(verify.getJSONObject("result").getJSONObject("experiment")));
            assertThrows(IllegalArgumentException.class,()->service.status("unknown"));assertThrows(IllegalArgumentException.class,()->service.start("../wrong",false));
        }
    }
    @Test public void interruptionNeverCreatesACompletedReceipt()throws Exception{
        try(var f=new StartingReference150Test.Fixture();var service=new ContinuityExperiment155(f.root,f.observations,f.association,new ReplayEvidence151(f.root,f.association))){
            var job=service.start(f.audit.getString("id"),false);service.close();var state=settled(service,job.getString("jobId"));assertEquals("failed",state.getString("status"));assertFalse(state.has("result"));assertFalse(Files.exists(f.root.resolve("continuity-experiments155")));
        }
    }
    @Test public void exactReplayLabelIsolationForgeryAndSourceRefusal()throws Exception{
        try(var f=new StartingReference150Test.Fixture()){
            var moved=WorldFrames.copy(f.labels);moved.put("provenance","Independent negative control labels; unchanged original pixels");
            for(Object raw:moved.getJSONArray("rows"))((JSONObject)raw).put("targetBox",new JSONArray("[.7,.6,.9,.9]"));
            var otherAudit=f.audit(f.association.save(moved));
            var memo=new ReplayEvidence151(f.root,f.association);var service=new ContinuityExperiment155(f.root,f.observations,f.association,memo);
            var nativeBefore=f.ledger.observe(f.world);byte[] state=Files.readAllBytes(f.root.resolve("world-observations/state.json"));
            var first=service.run(f.audit.getString("id")).getJSONObject("experiment");var other=service.run(otherAudit.getString("id")).getJSONObject("experiment");
            assertTrue(first.getJSONArray("trace").similar(other.getJSONArray("trace"))); // Labels cannot alter search.
            var warm=service.read(first.getString("id"));assertTrue(warm.getJSONObject("cost155").getBoolean("reusedReplay"));assertTrue(first.similar(warm.getJSONObject("experiment")));
            warm.getJSONObject("experiment").getJSONObject("summary").put("correct",999);
            assertTrue(first.similar(service.read(first.getString("id")).getJSONObject("experiment")));
            var cold=new ContinuityExperiment155(f.root,f.observations,f.association,new ReplayEvidence151(f.root,f.association)).read(first.getString("id"));assertTrue(first.similar(cold.getJSONObject("experiment")));assertFalse(cold.getJSONObject("cost155").getBoolean("reusedReplay"));
            var forced=service.read(first.getString("id"),true);assertFalse(forced.getJSONObject("cost155").getBoolean("reusedReplay"));assertTrue(first.similar(forced.getJSONObject("experiment")));
            var forged=WorldFrames.copy(first);forged.remove("id");forged.getJSONObject("summary").put("correct",1000);String id=WorldFrames.hash(forged);forged.put("id",id);
            Files.writeString(f.root.resolve("continuity-experiments155/"+id+".json"),forged.toString());assertThrows(IllegalStateException.class,()->service.read(id));
            var source=f.root.resolve("world-observations/datasets/"+f.data.getString("id")+"/2.image");byte[] bytes=Files.readAllBytes(source),bad=bytes.clone();bad[20]^=1;var time=Files.getLastModifiedTime(source);Files.write(source,bad);Files.setLastModifiedTime(source,time);
            assertThrows(Exception.class,()->service.read(first.getString("id")));Files.write(source,bytes);Files.setLastModifiedTime(source,time);
            assertTrue(first.similar(service.read(first.getString("id")).getJSONObject("experiment")));
            assertArrayEquals(state,Files.readAllBytes(f.root.resolve("world-observations/state.json")));assertTrue(nativeBefore.similar(f.ledger.observe(f.world)));
        }
    }
    @Test public void gridSearchProducesExactEvidenceWithEveryHistoryCheck()throws Exception{
        try(var f=new StartingReference150Test.Fixture()){
            var replay=new ReplayEvidence151(f.root,f.association);replay.read(f.audit.getString("id"),false);
            var reference=new ContinuityExperiment155(f.root,f.observations,f.association,replay,false).run(f.audit.getString("id"));
            var optimized=new ContinuityExperiment155(f.root,f.observations,f.association,replay,true).run(f.audit.getString("id"));
            assertTrue(reference.getJSONObject("experiment").similar(optimized.getJSONObject("experiment")));
            assertEquals(reference.getJSONObject("cost155").getLong("historyReads"),optimized.getJSONObject("cost155").getLong("historyReads"));
            var old=new ContinuityExperiment154(f.root,f.observations,f.association,replay).run(f.audit.getString("id"));
            assertTrue(old.getJSONObject("experiment").getJSONArray("trace").similar(optimized.getJSONObject("experiment").getJSONArray("trace")));
        }
    }
    @Test public void missingHistoryCannotBeRecreatedOnWarmRead()throws Exception{
        try(var f=new StartingReference150Test.Fixture();var service=new ContinuityExperiment155(f.root,f.observations,f.association,new ReplayEvidence151(f.root,f.association))){
            var first=service.run(f.audit.getString("id"));Path state=f.root.resolve("world-observations/state.json"),backup=f.root.resolve("held-history");Files.move(state,backup);
            try{assertThrows(IllegalStateException.class,()->service.read(first.getJSONObject("experiment").getString("id")));assertFalse(Files.exists(state));}finally{Files.move(backup,state);}
            assertTrue(first.getJSONObject("experiment").similar(service.read(first.getJSONObject("experiment").getString("id")).getJSONObject("experiment")));
        }
    }

}
