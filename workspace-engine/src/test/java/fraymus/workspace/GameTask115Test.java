package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

public class GameTask115Test {
    private static JSONObject untilDone(GameTask115 tasks,String id)throws Exception{JSONObject t=tasks.read(id);for(int i=0;i<650&&t.getString("status").equals("running");i++)t=tasks.advance(id);return t;}
    @Test public void routesRequestsAndBoundsRepairs(){
        assertEquals("gamelevel",ChatPolicy044.route("Create a playable Mario level and test it",new JSONObject(),false));
        assertEquals("workbench",ChatPolicy044.route("Create a website about Mario",new JSONObject(),false));
        assertEquals("workbench",ChatPolicy044.route("Explain how to create a Mario level",new JSONObject(),false));
        assertEquals(3,GameTask115.request("Create a Mario level seed=3.").getJSONObject("parameters").getInt("seed"));
        assertEquals(0,GameTask115.request("Construct a Game Lab level without repairs").getInt("repairLimit"));
        for(String bad:List.of("gapTiles=11","repairLimit=3","seed=-1","gapTiles=two","seed=1 seed=2"))assertThrows(IllegalArgumentException.class,()->GameTask115.request("Create a Mario level "+bad));
    }
    @Test public void realPhysicsRepairsObservedFallsAndRechecksEvidence()throws Exception{
        Path repo=Path.of("..").toAbsolutePath().normalize(),project=Files.createTempDirectory("game115-repair"),out=Path.of("build/game115-repair");Files.createDirectories(out);
        try(var game=new EmbeddedGameClient(repo,project);var tasks=new GameTask115(repo,project,game)){
            String id=UUID.randomUUID().toString();var request=new JSONObject().put("id",id).put("request","Create a playable Mario level gapTiles=10 repairLimit=2");tasks.start(request);assertEquals(id,tasks.start(request).getString("id"));
            var t=untilDone(tasks,id);Files.writeString(out.resolve("result.json"),t.toString(2));Files.writeString(out.resolve("project.txt"),project.toString());
            assertEquals(t.optString("reason"),"completed",t.getString("status"));assertEquals(2,t.getJSONObject("audit").getInt("repairCount"));assertEquals(0,t.getJSONObject("review").getJSONObject("state").getInt("tick"));assertFalse(t.getJSONObject("review").getJSONObject("state").getJSONObject("player").getBoolean("won"));assertTrue(t.getJSONObject("audit").getInt("failures")>=2);assertTrue(tasks.verify(id).getJSONObject("audit").getBoolean("passed"));
            Path evidence=project.resolve("game-tasks115").resolve(t.getJSONArray("trials").getJSONObject(0).getJSONObject("startImage").getString("file"));byte[] original=Files.readAllBytes(evidence);Files.write(evidence,new byte[]{1,2,3});assertThrows(IllegalStateException.class,()->tasks.verify(id));Files.write(evidence,original);
            Path checkpoint=project.resolve("game-tasks115").resolve(id+".json");String originalCheckpoint=Files.readString(checkpoint);var changed=AgentFiles042.loadChecked(checkpoint);changed.getJSONArray("trials").getJSONObject(0).put("status","won");AgentFiles042.saveChecked(checkpoint,changed);assertThrows(IllegalStateException.class,()->tasks.verify(id));Files.writeString(checkpoint,originalCheckpoint);
        }
    }
    @Test public void interruptionRestartsFreshSessionAndNoRepairBudgetReallyStops()throws Exception{
        Path repo=Path.of("..").toAbsolutePath().normalize(),project=Files.createTempDirectory("game115-restart"),out=Path.of("build/game115-restart");Files.createDirectories(out);String id=UUID.randomUUID().toString();String oldSession;
        try(var game=new EmbeddedGameClient(repo,project);var tasks=new GameTask115(repo,project,game)){
            tasks.start(new JSONObject().put("id",id).put("request","Create a playable Mario level"));tasks.advance(id);tasks.advance(id);tasks.advance(id);oldSession=tasks.read(id).getJSONArray("trials").getJSONObject(0).getString("sessionId");
        }
        try(var game=new EmbeddedGameClient(repo,project);var tasks=new GameTask115(repo,project,game)){
            assertEquals("interrupted",tasks.read(id).getString("status"));tasks.control(id,"resume");var t=untilDone(tasks,id);Files.writeString(out.resolve("result.json"),t.toString(2));assertEquals(t.optString("reason"),"completed",t.getString("status"));assertNotEquals(oldSession,t.getJSONArray("trials").getJSONObject(1).getString("sessionId"));tasks.verify(id);
            String refused=UUID.randomUUID().toString();tasks.start(new JSONObject().put("id",refused).put("request","Create a Mario level gapTiles=10 without repairs"));var failed=untilDone(tasks,refused);Files.writeString(out.resolve("no-repairs.json"),failed.toString(2));assertEquals("failed",failed.getString("status"));assertEquals(0,failed.getJSONArray("repairs").length());assertThrows(IllegalStateException.class,()->tasks.verify(refused));
        }
    }
}
