package fraymus.workspace;
import org.junit.Test;import static org.junit.Assert.*;import org.json.*;import java.nio.file.*;import java.util.*;
public class GameOpportunity120Test {
 @Test public void inventedExpiredOrRepeatedWaitingIsRefused(){
  String id=UUID.randomUUID().toString();var o=new JSONObject().put("phase","WAIT").put("waitStarts",1).put("waitTicks",12).put("waitClears",0).put("waitAborts",0).put("revisitStarts",0).put("revisitTicks",0).put("decisions",new JSONArray().put(new JSONObject().put("tick",1).put("objectId",id).put("action","wait-started").put("reason","Moving enemy").put("plannedTicks",120)));
  var c=new JSONObject().put("policy","opportunity120").put("pickupDeferrals",0).put("pickupSkips",0).put("contactChecks",12).put("contactOverrides",0).put("contactNanos",1).put("contactRecovery119",new JSONObject().put("attempts",0).put("phase","OBSERVE")).put("pickupDecisions",new JSONArray()).put("opportunity120",o).put("opportunityChecks",12).put("opportunityNanos",1);
  GameEvidence120.verify(c,12);assertThrows(IllegalStateException.class,()->GameEvidence120.verify(c,182));
  for(String field:List.of("waitTicks","waitStarts","waitClears","waitAborts","revisitTicks","revisitStarts")){var f=WorldFrames.copy(c);f.getJSONObject("opportunity120").put(field,999);assertThrows(IllegalStateException.class,()->GameEvidence120.verify(f,12));}
  var f=WorldFrames.copy(c);f.getJSONObject("opportunity120").getJSONArray("decisions").put(WorldFrames.copy(o.getJSONArray("decisions").getJSONObject(0)));assertThrows(IllegalStateException.class,()->GameEvidence120.verify(f,12));
  var mismatch=WorldFrames.copy(c);mismatch.getJSONObject("opportunity120").put("phase","REVISIT");assertThrows(IllegalStateException.class,()->GameEvidence120.verify(mismatch,12));
 }
 @Test public void supportedPickupsCompleteAfterWaitAndRestartWithEveryReceiptChecked()throws Exception{
  Path repo=Path.of("..").toAbsolutePath().normalize(),project=Files.createTempDirectory("opportunity120-task"),out=Path.of("build/opportunity120-task");Files.createDirectories(out);var records=new JSONArray();
  for(String request:List.of("Create a playable Mario level seed=41 lengthTiles=72 gapTiles=3 secondGapTiles=4 goombaTile=15 turtleTile=28 obstacleTile=25 obstacleHeight=2","Create a playable Mario level seed=53 lengthTiles=64 gapTiles=2 secondGapTiles=2 turtleTile=15 obstacleTile=26 obstacleHeight=2")){
   String id=UUID.randomUUID().toString();
   try(var game=new EmbeddedGameClient(repo,project);var tasks=new GameTask115(repo,project,game)){tasks.start(new JSONObject().put("id",id).put("request",request));tasks.advance(id);tasks.advance(id);tasks.advance(id);}
   try(var game=new EmbeddedGameClient(repo,project);var tasks=new GameTask115(repo,project,game)){
    assertEquals("interrupted",tasks.read(id).getString("status"));tasks.control(id,"resume");var t=GameChallenge118Test.done(tasks,id);records.put(t);Files.writeString(out.resolve("result.json"),new JSONObject().put("project",project.toString()).put("tasks",records).toString(2));
    assertEquals(t.optString("reason"),"completed",t.getString("status"));var a=tasks.verify(id).getJSONObject("audit");assertEquals("revisit121",a.getString("completionControl"));assertEquals("won",a.getString("existingAI"));assertEquals("failed",a.getString("simpleControl"));assertEquals(1,a.getInt("aiPickups"));assertEquals(0,a.getInt("aiDamage"));assertEquals(0,a.getInt("repairCount"));var o=a.getJSONObject("aiOpportunity");assertTrue(o.getInt("waitTicks")>0);assertEquals(1,o.getInt("waitClears"));assertEquals(0,o.getInt("waitAborts"));assertTrue(GameSummary118.describe(a).contains("confirmed clear"));
    Path checkpoint=project.resolve("game-tasks115/"+id+".json");String original=Files.readString(checkpoint);var forged=AgentFiles042.loadChecked(checkpoint);forged.getJSONObject("audit").getJSONObject("aiOpportunity").put("waitTicks",0);AgentFiles042.saveChecked(checkpoint,forged);assertThrows(IllegalStateException.class,()->tasks.verify(id));Files.writeString(checkpoint,original);
   }
  }
 }
}
