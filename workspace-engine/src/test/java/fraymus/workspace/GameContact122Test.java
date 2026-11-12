package fraymus.workspace;
import org.junit.Test;
import static org.junit.Assert.*;
import org.json.*;
import java.nio.file.*;
import java.util.*;

public class GameContact122Test {
 @Test public void recordedContactFailuresFinishAfterRestartAndRejectAlteredForecasts()throws Exception {
  Path repo=Path.of("..").toAbsolutePath().normalize(),project=Files.createTempDirectory("contact122-task"),out=Path.of("build/contact122-task");Files.createDirectories(out);var records=new JSONArray();
  for(String parameters:List.of("seed=109 lengthTiles=72 gapTiles=2 secondGapTiles=2 pickupTile=40 goombaTile=42","seed=113 lengthTiles=72 gapTiles=2 secondGapTiles=2 pickupTile=40 turtleTile=44","seed=157 lengthTiles=72 gapTiles=3 secondGapTiles=2 pickupTile=11 turtleTile=16 obstacleTile=14 obstacleHeight=2")) {
   String id=UUID.randomUUID().toString();
   try(var game=new EmbeddedGameClient(repo,project);var tasks=new GameTask115(repo,project,game)){tasks.start(new JSONObject().put("id",id).put("request","Create a playable Mario level "+parameters+" repairLimit=0"));tasks.advance(id);tasks.advance(id);tasks.advance(id);}
   try(var game=new EmbeddedGameClient(repo,project);var tasks=new GameTask115(repo,project,game)){
    assertEquals("interrupted",tasks.read(id).getString("status"));tasks.control(id,"resume");var t=GameChallenge118Test.done(tasks,id);records.put(t);Files.writeString(out.resolve("result.json"),new JSONObject().put("project",project.toString()).put("tasks",records).toString(2));
    assertEquals(t.optString("reason"),"completed",t.getString("status"));var a=tasks.verify(id).getJSONObject("audit");assertEquals("contact122",a.getString("completionControl"));assertEquals("won",a.getString("existingAI"));assertEquals(0,a.getInt("aiDamage"));assertEquals(1,a.getInt("aiPickups"));assertEquals(0,a.getInt("repairCount"));
    var proof=a.getJSONObject("aiEnemyLanding");assertTrue(parameters.contains("seed=157")?proof.getInt("edgeStarts")>0:proof.getInt("overrides")>0);
    var c=t.getJSONArray("trials").getJSONObject(t.getJSONArray("trials").length()-1).getJSONObject("final").getJSONObject("controller");GameEvidence122.verify(c,10000);
    for(String field:List.of("checks","overrides","horizonTicks","threatLimit","edgeStarts")){var bad=WorldFrames.copy(c);bad.getJSONObject("enemyLanding122").put(field,999999);assertThrows(IllegalStateException.class,()->GameEvidence122.verify(bad,10000));}
    var unsafe=WorldFrames.copy(c);unsafe.getJSONObject("enemyLanding122").put("override",true).put("overrides",1).put("desiredContact",true).put("chosenContact",true);assertThrows(IllegalStateException.class,()->GameEvidence122.verify(unsafe,10000));
    var invented=WorldFrames.copy(c);invented.getJSONObject("enemyLanding122").put("checks",0).put("overrides",0);assertThrows(IllegalStateException.class,()->GameEvidence122.verify(invented,10000));
    Path checkpoint=project.resolve("game-tasks115/"+id+".json");String original=Files.readString(checkpoint);var forged=AgentFiles042.loadChecked(checkpoint);forged.getJSONObject("audit").getJSONObject("aiEnemyLanding").put("overrides",999999);AgentFiles042.saveChecked(checkpoint,forged);assertThrows(IllegalStateException.class,()->tasks.verify(id));Files.writeString(checkpoint,original);
   }
  }
  Files.writeString(out.resolve("result.json"),new JSONObject().put("passed",true).put("project",project.toString()).put("tasks",records).toString(2));
 }
}
