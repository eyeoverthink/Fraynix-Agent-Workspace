package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

public class GameChallenge118Test {
    final Path repo=Path.of("..").toAbsolutePath().normalize();
    @Test public void boundsDeclaredConstructionAndRefusesUnsupportedRequests(){
        var p=GameTask115.request("Create a playable Mario level goombaTile=24 obstacleTile=27 obstacleHeight=2");
        assertEquals(0,p.getInt("repairLimit"));assertEquals(24,p.getJSONObject("parameters").getInt("goombaTile"));
        for(String bad:List.of("goombaTile=5","goombaTile=17","turtleTile=49","obstacleTile=24","obstacleHeight=2","obstacleTile=24 obstacleHeight=4","goombaTile=24 turtleTile=24","goombaTile=24 repairLimit=1","goombaTile=two","goombaTile=24 goombaTile=26","with enemies"))
            assertThrows(bad,IllegalArgumentException.class,()->GameTask115.request("Create a playable Mario level "+bad));
        assertThrows(IllegalArgumentException.class,()->GameChallenge118.parameters(new JSONObject().put("goombaTile",24).put("unknown",1)));
    }
    @Test public void clonesOriginalBehaviorAndRefusesRelabelledGeometry()throws Exception{
        Path project=Files.createTempDirectory("challenge118-source");var levels=new GameLevels114(repo,project);
        var m=levels.create115(UUID.randomUUID().toString(),new JSONObject().put("goombaTile",24).put("turtleTile",27).put("obstacleTile",30).put("obstacleHeight",3).put("pickupTile",17));
        assertEquals(GameChallenge118.RECIPE,m.getString("recipe"));var a=new JSONArray(Files.readString(Path.of(m.getString("levelPath"))));
        assertNotNull(GameLevels114.first(a,"components.GoombaAI"));assertNotNull(GameLevels114.first(a,"components.TurtleAI"));
        assertTrue(levels.verify(m.getString("id")).getBoolean("checked"));
        Path level=Path.of(m.getString("levelPath"));var goomba=GameLevels114.first(a,"components.GoombaAI");GameLevels114.prop(goomba,"jade.Transform").getJSONObject("position").put("x",0);Files.writeString(level,a.toString());m.put("levelSha256",GameLevels114.hash(level));Files.writeString(level.resolveSibling("manifest.json"),m.toString());assertThrows(IllegalStateException.class,()->levels.verify(m.getString("id")));
    }
    static JSONObject done(GameTask115 tasks,String id)throws Exception{var t=tasks.read(id);for(int i=0;i<650&&t.getString("status").equals("running");i++)t=tasks.advance(id);return t;}
    @Test public void challengeRequiresAiFinishAndPreservesRestartedSimpleFailure()throws Exception{
        Path project=Files.createTempDirectory("challenge118-task"),out=Path.of("build/challenge118-task");Files.createDirectories(out);String id=UUID.randomUUID().toString(),session;
        try(var game=new EmbeddedGameClient(repo,project);var tasks=new GameTask115(repo,project,game)){
            tasks.start(new JSONObject().put("id",id).put("request","Create a playable Mario level pickupTile=12 obstacleTile=12 obstacleHeight=2"));tasks.advance(id);tasks.advance(id);tasks.advance(id);session=tasks.read(id).getJSONArray("trials").getJSONObject(0).getString("sessionId");
        }
        try(var game=new EmbeddedGameClient(repo,project);var tasks=new GameTask115(repo,project,game)){
            assertEquals("interrupted",tasks.read(id).getString("status"));tasks.control(id,"resume");var t=done(tasks,id);Files.writeString(out.resolve("result.json"),t.toString(2));Files.writeString(out.resolve("project.txt"),project.toString());
            assertEquals(t.optString("reason"),"completed",t.getString("status"));assertNotEquals(session,t.getJSONArray("trials").getJSONObject(1).getString("sessionId"));
            var audit=tasks.verify(id).getJSONObject("audit");assertEquals("failed",audit.getString("simpleControl"));assertEquals("won",audit.getString("existingAI"));assertEquals("revisit121",audit.getString("completionControl"));assertEquals("revisit121",t.getJSONArray("trials").getJSONObject(t.getJSONArray("trials").length()-1).getJSONObject("final").getJSONObject("controller").getString("policy"));assertEquals(1,audit.getInt("aiPickupSkips"));assertEquals(0,audit.getInt("repairCount"));assertTrue(audit.getJSONArray("pickupDecisions").getJSONObject(0).getString("reason").contains("clearance"));
            Path checkpoint=project.resolve("game-tasks115/"+id+".json");String original=Files.readString(checkpoint);var forged=AgentFiles042.loadChecked(checkpoint);forged.getJSONObject("audit").put("aiPickupSkips",0);AgentFiles042.saveChecked(checkpoint,forged);assertThrows(IllegalStateException.class,()->tasks.verify(id));Files.writeString(checkpoint,original);
        }
    }
    @Test public void actualAiFailureCannotBePublishedAsChallengeCompletion()throws Exception{
        Path project=Files.createTempDirectory("challenge118-failed"),out=Path.of("build/challenge118-failed");Files.createDirectories(out);String id=UUID.randomUUID().toString();
        try(var game=new EmbeddedGameClient(repo,project);var tasks=new GameTask115(repo,project,game)){
            tasks.start(new JSONObject().put("id",id).put("request","Create a playable Mario level gapTiles=10 pickupTile=17 goombaTile=45"));var t=done(tasks,id);Files.writeString(out.resolve("result.json"),t.toString(2));Files.writeString(out.resolve("project.txt"),project.toString());assertEquals("failed",t.getString("status"));assertEquals(2,t.getJSONArray("trials").length());assertEquals(0,t.getJSONArray("repairs").length());assertThrows(IllegalStateException.class,()->tasks.verify(id));
        }
        Path checkpoint=project.resolve("game-tasks115/"+id+".json");var forged=AgentFiles042.loadChecked(checkpoint);forged.put("status","completed");AgentFiles042.saveChecked(checkpoint,forged);
        try(var game=new EmbeddedGameClient(repo,project);var tasks=new GameTask115(repo,project,game)){assertThrows(IllegalStateException.class,()->tasks.verify(id));}
    }
    @Test public void enemyReferenceIsRefusedInsteadOfDroppingTheEnemy()throws Exception{
        Path project=Files.createTempDirectory("challenge118-image"),out=Path.of("build/challenge118-image");Files.createDirectories(out);
        try(var game=new EmbeddedGameClient(repo,project)){
            var levels=new GameLevels114(repo,project);var m=levels.create115(UUID.randomUUID().toString(),new JSONObject().put("goombaTile",24));levels.launch(game,m.getString("id"));var frame=game.command(new JSONObject().put("action","overview"));
            var refs=new GameReference115(repo,project);var ref=refs.importImage(new JSONObject().put("pngBase64",frame.getString("pngBase64")));Files.write(out.resolve("enemy-reference.png"),Base64.getDecoder().decode(frame.getString("pngBase64")));Files.writeString(out.resolve("result.json"),new JSONObject().put("project",project.toString()).put("reference",ref).toString(2));
            assertFalse("Unsupported enemy image must not be silently simplified",ref.getBoolean("accepted"));assertThrows(IllegalArgumentException.class,()->refs.verify(ref.getString("id")));
            assertEquals("revisit121",game.command(new JSONObject().put("action","ai-step").put("ticks",12).put("targetX",m.getDouble("finishX")+1)).getJSONObject("state").getJSONObject("controller").getString("policy"));
        }
    }
}
