package fraymus.workspace;
import org.junit.Test;
import static org.junit.Assert.*;
import org.json.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

public class TaskCost125Test {
    @Test public void terminalClockStaysFixedWhileLaterChecksAccumulate()throws Exception {
        Path project=Files.createTempDirectory("cost125");String id=UUID.randomUUID().toString();var c=new TaskCost125(project);var t=new JSONObject().put("request","fixture");c.admitted(id,t,System.nanoTime(),Instant.now());long begin=c.begin(id,"verification");c.end(id,"verification",begin,"returned",true,false);var original=c.snapshot(id);assertTrue(original.getJSONObject("terminal").getBoolean("comparable"));
        begin=c.begin(id,"measurement");c.end(id,"measurement",begin,"returned",true,false);var next=c.snapshot(id);assertTrue(original.getJSONObject("terminal").similar(next.getJSONObject("terminal")));assertTrue(TaskCost125.summary(next).getDouble("laterCheckSeconds")>0);c.verifySnapshot(original);
        var reopened=new TaskCost125(project);reopened.verifySnapshot(original);var modified=WorldFrames.copy(original);modified.getJSONObject("terminal").put("seconds",.000001);assertThrows(IllegalStateException.class,()->reopened.verifySnapshot(modified));
    }
    @Test public void incompleteCallAcrossRestartIsUnknownAndWallSpanCannotCalibrate()throws Exception {
        Path project=Files.createTempDirectory("cost125");String id=UUID.randomUUID().toString();var c=new TaskCost125(project);c.admitted(id,new JSONObject().put("request","fixture"),System.nanoTime(),Instant.now());c.begin(id,"execution");
        var recovered=new TaskCost125(project);assertEquals(1,TaskCost125.summary(recovered.snapshot(id)).getInt("unknownCalls"));long begin=recovered.begin(id,"recovery");recovered.end(id,"recovery",begin,"returned",false,true);begin=recovered.begin(id,"verification");recovered.end(id,"verification",begin,"returned",true,false);var r=recovered.snapshot(id);assertEquals(1,r.getInt("unknownCalls"));assertTrue(r.getBoolean("restarted"));assertFalse(r.getJSONObject("terminal").getBoolean("comparable"));assertTrue(r.getJSONObject("terminal").getString("clock").contains("wall span"));
    }
    @Test public void ownerRejectsChangedCostsAndLegacyHasNoInventedClock()throws Exception {
        Path project=Files.createTempDirectory("cost125");String id=UUID.randomUUID().toString();var c=new TaskCost125(project);assertFalse(TaskCost125.summary(c.snapshot(id)).getBoolean("available"));c.admitted(id,new JSONObject().put("request","fixture"),System.nanoTime(),Instant.now());Path file=project.resolve("game-costs125/"+id+".json");var r=AgentFiles042.loadChecked(file);r.getJSONArray("events").getJSONObject(0).put("seconds",12345);AgentFiles042.saveChecked(file,r);assertThrows(IllegalStateException.class,()->c.read(id));
    }
}
