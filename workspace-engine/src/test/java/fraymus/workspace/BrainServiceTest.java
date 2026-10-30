package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
public class BrainServiceTest {
    static JSONObject await(BrainService brain,String id) throws Exception {
        long until=System.nanoTime()+180_000_000_000L;
        while(System.nanoTime()<until){var j=brain.read(id);if(!java.util.Set.of("queued","running").contains(j.getString("status"))&&!brain.state().getBoolean("busy"))return j;Thread.sleep(25);}
        throw new AssertionError("Java brain job timeout");
    }
    @Test public void physicalLearningIsEvaluatedPromotedRestartedAndRolledBack() throws Exception {
        var dir=Files.createTempDirectory("brain-learning-test-");JSONObject job;
        try(var brain=new BrainService(dir)){
            job=await(brain,brain.start(new JSONObject().put("kind","train")).getString("id"));
            Files.createDirectories(Path.of("build"));Files.writeString(Path.of("build/brain-learning-evidence.json"),job.toString(2));
            assertEquals(job.optString("reason"),"completed",job.getString("status"));assertTrue(job.getJSONObject("evaluation").toString(),job.getBoolean("accepted"));
            assertEquals(180,job.getInt("progress"));assertEquals(2,brain.activePolicy().getInt("version"));
            double base=0,trained=0;for(Object raw:job.getJSONArray("validation")){var row=(JSONObject)raw;var c=row.getJSONObject("case");for(String type:new String[]{"baseline","selected"}){var trial=row.getJSONObject(type);double independent=Math.abs(trial.getJSONObject("final").getDouble("distanceM")-c.getDouble("targetM"));assertEquals(independent,trial.getDouble("errorM"),1e-12);assertEquals(180,trial.getJSONObject("final").getInt("tick"));assertEquals(BulletRamp.BACKEND,trial.getString("backend"));}base+=row.getJSONObject("baseline").getDouble("errorM");trained+=row.getJSONObject("selected").getDouble("errorM");}
            assertEquals(base/6,job.getJSONObject("evaluation").getDouble("baselineMeanErrorM"),1e-12);assertEquals(trained/6,job.getJSONObject("evaluation").getDouble("trainedMeanErrorM"),1e-12);
        }
        try(var reopened=new BrainService(dir)){assertEquals(2,reopened.activePolicy().getInt("version"));assertEquals(job.getJSONArray("weights").toString(),reopened.activePolicy().getJSONArray("weights").toString());assertEquals("completed",reopened.read(job.getString("id")).getString("status"));assertEquals(3,reopened.rollback(2).getInt("version"));assertEquals("[0,0,0,0]",reopened.activePolicy().getJSONArray("weights").toString());}
    }
}
