package fraymus.workspace;
import org.junit.Test;
import static org.junit.Assert.*;
import org.json.*;
import java.nio.file.*;
import java.util.*;

public class GameCostTask125Test {
    @Test public void recoveryRetainsWallUncertaintyAndFailedOutcomesStayRefused()throws Exception {
        Path repo=Path.of("..").toAbsolutePath().normalize(),project=Files.createTempDirectory("cost125-recovery"),out=Path.of("build/cost125-task");Files.createDirectories(out);String id=UUID.randomUUID().toString();var result=new JSONObject().put("passed",false).put("project",project.toString());
        String text="Create a playable Mario level seed=127 "+GameCostStudy125Test.RETURN+" pickupStrategy=budget pickupBudgetSeconds=0";
        long began=System.nanoTime();
        try {try(var game=new EmbeddedGameClient(repo,project);var tasks=new GameCosts125(repo,project,game)) {tasks.start(new JSONObject().put("id",id).put("request",text));tasks.advance(id);tasks.advance(id);tasks.advance(id);}
            try(var game=new EmbeddedGameClient(repo,project);var tasks=new GameCosts125(repo,project,game)) {assertEquals("interrupted",tasks.read(id).getString("status"));tasks.control(id,"resume");GameCostStudy125Test.done(tasks,id);tasks.verify(id);var measured=tasks.measure(id);assertTrue(measured.getBoolean("completionCertified"));assertFalse(measured.getJSONObject("cost125").getJSONObject("terminal").getBoolean("comparable"));assertFalse(measured.getJSONObject("choice124").getBoolean("eligible"));result.put("recovered",measured).put("externalRecoveredSeconds",(System.nanoTime()-began)/1e9);
                var report=tasks.review(id);assertFalse(report.getJSONObject("metrics").getBoolean("established"));tasks.verifyReview(report.getString("id"));result.put("recoveryReview",report);
                var failed=GameCostStudy125Test.run(tasks,"gapTiles=10 pickupTile=17 goombaTile=45","budget pickupBudgetSeconds=30");assertFalse(failed.getJSONObject("measurement").getBoolean("completionCertified"));result.put("failed",failed);var refused=tasks.review(failed.getString("id"));assertFalse(refused.getJSONObject("metrics").getBoolean("established"));tasks.verifyReview(refused.getString("id"));result.put("failedReview",refused);
            }
            result.put("passed",true);
        }finally {Files.writeString(out.resolve("result.json"),result.toString(2));}
    }
    @Test public void legacyRepeatedAdmissionDoesNotInventNewTiming()throws Exception {
        Path repo=Path.of("..").toAbsolutePath().normalize(),project=Files.createTempDirectory("cost125-legacy");String id=UUID.randomUUID().toString(),request="Create a playable Mario level goombaTile=24 pickupStrategy=direct";
        try(var game=new EmbeddedGameClient(repo,project);var old=new GameChoices124(repo,project,game)){old.start(new JSONObject().put("id",id).put("request",request));}
        try(var game=new EmbeddedGameClient(repo,project);var tasks=new GameCosts125(repo,project,game)){var t=tasks.start(new JSONObject().put("id",id).put("request",request));assertFalse(t.getJSONObject("cost125").getBoolean("available"));assertFalse(Files.exists(project.resolve("game-costs125/"+id+".json")));}
    }
}
