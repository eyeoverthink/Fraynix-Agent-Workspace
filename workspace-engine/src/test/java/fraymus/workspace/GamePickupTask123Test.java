package fraymus.workspace;
import org.junit.Test;import static org.junit.Assert.*;import org.json.*;import java.nio.file.*;import java.util.*;
public class GamePickupTask123Test {
 @Test public void disabledReturnSurvivesRestartAndFailureIsMeasuredWithoutCompletion()throws Exception{
  Path repo=Path.of("..").toAbsolutePath().normalize(),project=Files.createTempDirectory("pickup123-restart"),out=Path.of("build/pickup123-task");Files.createDirectories(out);String id=UUID.randomUUID().toString();var records=new JSONArray();
  try(var game=new EmbeddedGameClient(repo,project);var tasks=new GameTask115(repo,project,game)){tasks.start(new JSONObject().put("id",id).put("request","Create a playable Mario level seed=127 lengthTiles=72 gapTiles=2 secondGapTiles=2 pickupTile=42 goombaTile=46 obstacleTile=60 obstacleHeight=1 pickupStrategy=no-return"));tasks.advance(id);tasks.advance(id);tasks.advance(id);}
  try(var game=new EmbeddedGameClient(repo,project);var tasks=new GameTask115(repo,project,game)){
   assertEquals("interrupted",tasks.read(id).getString("status"));tasks.control(id,"resume");var t=GameChallenge118Test.done(tasks,id);assertEquals(t.optString("reason"),"completed",t.getString("status"));var m=tasks.measure123(id);records.put(m);assertTrue(m.getBoolean("completionCertified"));assertEquals(0,m.getJSONObject("audit").getJSONObject("aiOpportunity").getInt("revisitStarts"));assertEquals(0,m.getJSONObject("audit").getInt("aiPickups"));tasks.verify(id);
   var c=m.getJSONObject("final").getJSONObject("controller");var bad=WorldFrames.copy(c);bad.getJSONObject("opportunity120").put("returnsEnabled123",true);assertThrows(IllegalStateException.class,()->GameEvidence123.verify(bad,m.getJSONObject("final").getInt("tick")));
   Path checkpoint=project.resolve("game-tasks115/"+id+".json");String original=Files.readString(checkpoint);var forged=AgentFiles042.loadChecked(checkpoint);forged.getJSONObject("definition").put("pickupStrategy","full");AgentFiles042.saveChecked(checkpoint,forged);assertThrows(IllegalStateException.class,()->tasks.measure123(id));Files.writeString(checkpoint,original);
   String failed=UUID.randomUUID().toString();tasks.start(new JSONObject().put("id",failed).put("request","Create a playable Mario level gapTiles=10 pickupTile=17 goombaTile=45 pickupStrategy=direct"));var f=GameChallenge118Test.done(tasks,failed);assertEquals("failed",f.getString("status"));var fm=tasks.measure123(failed);records.put(fm);assertTrue(fm.getBoolean("evidenceVerified"));assertFalse(fm.getBoolean("completionCertified"));assertThrows(IllegalStateException.class,()->tasks.verify(failed));
  }
  Files.writeString(out.resolve("result.json"),new JSONObject().put("passed",true).put("project",project.toString()).put("measurements",records).toString(2));
 }
}
