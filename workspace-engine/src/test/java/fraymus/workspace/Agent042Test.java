package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;

public class Agent042Test {
    @Test public void failedGoalsAreWitnessedOnceWithoutRewardingSuccess()throws Exception{
        Path root=Files.createTempDirectory("agent042-outcomes");String failed=UUID.randomUUID().toString();var source=new JSONObject().put("source",new JSONObject().put("status","failed")).put("goals",new JSONArray().put(new JSONObject().put("id",failed).put("status","failed")));
        try(var brain=new BrainService(root);var r=new AgentRuntime042(root,brain,()->source,()->{},false,new AgentClock042(),false)){r.collect();var facts=r.state().getJSONObject("observations");assertFalse(facts.getBoolean("newCompletedGoal"));assertEquals(1,facts.getInt("newFailedGoals"));assertEquals(0,r.state().getJSONObject("appraisal").getDouble("goalProgress"),0);long records=r.state().getJSONObject("witness").getLong("records");r.collect();assertEquals(0,r.state().getJSONObject("observations").getInt("newFailedGoals"));assertEquals(records,r.state().getJSONObject("witness").getLong("records"));}
    }
    @Test public void acceptedCameraOutputsExpireAndReplayCannotRefreshThem()throws Exception{
        try(var f=new Alignment041Test.Fixture()){var t=f.start("stationary");var row=f.upload(t,1,.4,false,"start");var nano=new AtomicLong();var c=new CameraReceipt042(nano::get);c.accept(t.getString("id"),row);assertTrue(c.read().toString(),c.read().getBoolean("fresh"));assertEquals(row.getJSONObject("frame").getString("imageSha256"),c.read().getJSONObject("evidence").getString("imageSha256"));nano.addAndGet(4_000_000_000L);assertFalse(c.read().getBoolean("fresh"));c.accept(t.getString("id"),row);assertFalse(c.read().getBoolean("fresh"));var hidden=f.upload(t,2,.4,true,"start");c.accept(t.getString("id"),hidden);assertFalse(c.read().getBoolean("fresh"));c.clear();assertTrue(c.read().isNull("evidence"));row.put("receivedAt","2000-01-01T00:00:00Z");c.accept(t.getString("id"),row);assertFalse(c.read().getBoolean("fresh"));assertFalse(new CameraReceipt042().read().getBoolean("fresh"));}
    }
    @Test public void clocksKeepWallRollbackAndBootDomainsSeparate(){
        var nano=new AtomicLong(1000);var wall=new AtomicReference<>(Instant.parse("2026-11-01T12:00:00Z"));var clock=new AgentClock042(nano::get,wall::get);
        nano.addAndGet(2_000_000_000L);wall.set(wall.get().minusSeconds(3600));assertEquals(2000,clock.elapsedMs());assertEquals(9000,clock.read(8,7000).getLong("retainedActiveMs"));
        var next=new AgentClock042(nano::get,wall::get);assertNotEquals(clock.bootId,next.bootId);assertEquals(0,next.elapsedMs());assertEquals(8,clock.read(8,0).getLong("logicalTicks"));
    }
    @Test public void appraisalAblationsActuallyChangeWork(){
        var a=new AgentAppraisal042();var f=new JSONObject().put("sourceChanged",true).put("observationFresh",false).put("heapFraction",.2);
        assertTrue(a.update(f,1000,true).getBoolean("allowNeuralUpdate"));assertFalse(a.update(f,1000,false).getBoolean("allowNeuralUpdate"));
        f.put("heapFraction",.9);assertEquals("rest",a.update(f,1000,true).getString("decision"));f.put("heapFraction",.2).put("collectionMs",480);assertEquals("rest",a.update(f,1000,true).getString("decision"));
        f.put("collectionMs",0).put("sourceChanged",false);assertEquals("await-observation",a.update(f,1000,true).getString("decision"));f.put("hasRetainedEvidence",true);var r=a.update(f,1000,true);assertEquals("consolidate",r.getString("decision"));assertEquals(1,r.getDouble("uncertainty"),0);
    }
    @Test public void witnessSegmentsVerifyExactCheckpointPrefixAndRefuseDamage()throws Exception{
        Path root=Files.createTempDirectory("agent042-witness");var w=new Witness042(root);for(int i=0;i<255;i++)w.append("observation",new JSONObject().put("ordinal",i));var anchor=w.summary();assertEquals(256,anchor.getInt("records"));w.append("continued",new JSONObject().put("value",true));assertEquals(2,w.verify().getInt("segments"));assertTrue(w.hasAnchor(anchor));assertFalse(w.hasAnchor(new JSONObject(anchor.toString()).put("records",255)));assertFalse(w.hasAnchor(new JSONObject(anchor.toString()).put("segments",2)));
        Path file=root.resolve("segment-000000/kernel-journal.log");byte[] original=Files.readAllBytes(file);Files.writeString(file,"tamper\n",StandardOpenOption.APPEND);assertThrows(Exception.class,()->new Witness042(root));assertTrue(Files.size(file)>original.length);
    }
    @Test public void checkedStateRejectsAlterationWithoutErasingOriginal()throws Exception{
        Path file=Files.createTempDirectory("agent042-state").resolve("state.json");AgentFiles042.saveChecked(file,new JSONObject().put("agentId","original"));var e=AgentFiles042.read(file,1000);e.getJSONObject("data").put("agentId","forged");Files.writeString(file,e.toString());assertThrows(Exception.class,()->AgentFiles042.loadChecked(file));assertTrue(Files.readString(file).contains("forged"));
    }
    @Test public void actualSharedCortexCheckpointReplayLeaseAndRestart()throws Exception{
        Path root=Files.createTempDirectory("agent042-brain");var nano=new AtomicLong();var wall=new AtomicReference<>(Instant.parse("2026-11-01T12:00:00Z"));var detached=new AtomicInteger();var source=new AtomicReference<>(new JSONObject().put("source",new JSONObject().put("recordedSource","dated-frame-A")).put("observationFresh",false).put("heapFraction",.1));String identity,boot;long ticks;
        try(var brain=new BrainService(root);var r=new AgentRuntime042(root,brain,source::get,detached::incrementAndGet,true,new AgentClock042(nano::get,wall::get),false)){
            identity=r.state().getString("agentId");boot=r.state().getString("bootId");String first=UUID.randomUUID().toString(),other=UUID.randomUUID().toString();r.attach(first);assertThrows(IllegalStateException.class,()->r.attach(other));r.beat();r.collect();assertEquals(1,r.state().getInt("experienceCount"));assertTrue(r.state().getJSONObject("neural").getBoolean("updated"));assertEquals(32768,brain.state().getJSONObject("cortex").getInt("neurons"));assertFalse(r.state().getJSONObject("observations").getBoolean("observationFresh"));
            nano.addAndGet(6_000_000_000L);r.collect();assertEquals(1,r.state().getInt("replayedExperiences"));ticks=brain.cortexTicks042();assertEquals(2,ticks);r.control("checkpoint");var saved=AgentFiles042.loadChecked(root.resolve("runtime-042/agent-state.json"));assertEquals(ticks,saved.getJSONObject("neural").getLong("ticks"));
            r.control("pause");source.get().getJSONObject("source").put("recordedSource","dated-frame-B");r.collect();assertEquals(ticks,brain.cortexTicks042());var pulse=brain.start(new JSONObject().put("kind","pulse"));long deadline=System.currentTimeMillis()+10000;while(brain.state().getBoolean("busy")&&System.currentTimeMillis()<deadline)Thread.sleep(20);assertEquals("completed",brain.read(pulse.getString("id")).getString("status"));assertEquals(ticks+1,brain.cortexTicks042());ticks=brain.cortexTicks042();r.control("checkpoint");r.beat();assertEquals(2,r.state().getJSONObject("clock").getLong("logicalTicks"));nano.addAndGet(2_000_000_000L);assertThrows(IllegalStateException.class,()->r.touch(first));r.collect();assertEquals(1,detached.get());r.attach(other);assertThrows(IllegalStateException.class,()->r.detach(first));assertThrows(IllegalStateException.class,()->r.requireLease(first));r.detach(other);assertEquals(2,detached.get());
        }
        try(var brain=new BrainService(root);var r=new AgentRuntime042(root,brain,source::get,()->{},true,new AgentClock042(nano::get,wall::get),false)){
            assertEquals(identity,r.state().getString("agentId"));assertNotEquals(boot,r.state().getString("bootId"));assertEquals(ticks,brain.cortexTicks042());assertEquals(ticks,r.state().getLong("sharedCortexTicks"));assertEquals(2,r.state().getJSONObject("clock").getLong("logicalTicks"));assertEquals(8000,r.state().getJSONObject("clock").getLong("retainedActiveMs"));assertFalse(r.state().getBoolean("backgroundEnabled"));assertFalse(r.state().getBoolean("interfaceAttached"));assertTrue(r.audit().getBoolean("verified"));
        }
    }
    @Test public void missingNeuralCheckpointDoesNotSilentlyResetBrain()throws Exception{
        Path root=Files.createTempDirectory("agent042-neural-loss");try(var brain=new BrainService(root);var r=new AgentRuntime042(root,brain,()->new JSONObject().put("source",new JSONObject().put("v",1)),()->{},true,new AgentClock042(),false)){r.collect();r.control("checkpoint");}var s=AgentFiles042.loadChecked(root.resolve("runtime-042/agent-state.json"));Files.delete(root.resolve("runtime-042").resolve(s.getJSONObject("neural").getString("file")));try(var brain=new BrainService(root)){assertThrows(Exception.class,()->new AgentRuntime042(root,brain,JSONObject::new,()->{},true,new AgentClock042(),false));}assertTrue(Files.exists(root.resolve("runtime-042/agent-state.json")));
    }
    @Test public void reportEscapesSourceDataAndKeepsExactSnapshot()throws Exception{
        var s=new JSONObject().put("agentId",UUID.randomUUID().toString()).put("bootId",UUID.randomUUID().toString()).put("status","awake").put("decision","consolidate").put("clock",new JSONObject().put("retainedActiveMs",4).put("logicalTicks",1)).put("recentWitnesses",new JSONArray().put(new JSONObject().put("kind","<script>alert(1)</script>").put("hash","hash").put("evidence",new JSONObject().put("wallTime","now"))));var report=AgentReport042.build(s,WorldFrames.hash(s));String html=report.getJSONObject("files").getString("index.html");assertFalse(html.contains("<script>alert"));assertTrue(html.contains("&lt;script&gt;"));assertTrue(new JSONObject(report.getJSONObject("files").getString("snapshot.json")).similar(s));
    }
}
