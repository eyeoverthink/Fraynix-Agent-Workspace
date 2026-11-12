package fraymus.workspace;
import org.junit.Test;import static org.junit.Assert.*;import org.json.*;import java.nio.file.*;import java.util.*;
public class GameOpportunity121Test {
 @Test public void inventedExpiredOrRepeatedWaitingIsRefused(){
  String id=UUID.randomUUID().toString();var o=new JSONObject().put("phase","WAIT").put("waitStarts",1).put("waitTicks",12).put("waitClears",0).put("waitAborts",0).put("revisitStarts",0).put("revisitTicks",0).put("decisions",new JSONArray().put(new JSONObject().put("tick",1).put("objectId",id).put("action","wait-started").put("reason","Moving enemy").put("plannedTicks",120)));
  var c=new JSONObject().put("policy","opportunity120").put("pickupDeferrals",0).put("pickupSkips",0).put("contactChecks",12).put("contactOverrides",0).put("contactNanos",1).put("contactRecovery119",new JSONObject().put("attempts",0).put("phase","OBSERVE")).put("pickupDecisions",new JSONArray()).put("opportunity120",o).put("opportunityChecks",12).put("opportunityNanos",1);
  GameEvidence120.verify(c,12);assertThrows(IllegalStateException.class,()->GameEvidence120.verify(c,182));
  for(String field:List.of("waitTicks","waitStarts","waitClears","waitAborts","revisitTicks","revisitStarts")){var f=WorldFrames.copy(c);f.getJSONObject("opportunity120").put(field,999);assertThrows(IllegalStateException.class,()->GameEvidence120.verify(f,12));}
  var f=WorldFrames.copy(c);f.getJSONObject("opportunity120").getJSONArray("decisions").put(WorldFrames.copy(o.getJSONArray("decisions").getJSONObject(0)));assertThrows(IllegalStateException.class,()->GameEvidence120.verify(f,12));
  var mismatch=WorldFrames.copy(c);mismatch.getJSONObject("opportunity120").put("phase","REVISIT");assertThrows(IllegalStateException.class,()->GameEvidence120.verify(mismatch,12));
 }
 @Test public void supportedPickupsCompleteAfterWaitAndRestartWithEveryReceiptChecked()throws Exception{
  Path repo=Path.of("..").toAbsolutePath().normalize(),project=Files.createTempDirectory("opportunity121-task"),out=Path.of("build/opportunity121-task");Files.createDirectories(out);var records=new JSONArray();
  for(String request:List.of("Create a playable Mario level seed=127 lengthTiles=72 gapTiles=2 secondGapTiles=2 pickupTile=42 goombaTile=46 obstacleTile=60 obstacleHeight=1","Create a playable Mario level seed=110 lengthTiles=64 gapTiles=2 secondGapTiles=2 pickupTile=10 goombaTile=15 obstacleTile=13 obstacleHeight=1")){
   String id=UUID.randomUUID().toString();
   try(var game=new EmbeddedGameClient(repo,project);var tasks=new GameTask115(repo,project,game)){tasks.start(new JSONObject().put("id",id).put("request",request));tasks.advance(id);tasks.advance(id);tasks.advance(id);}
   try(var game=new EmbeddedGameClient(repo,project);var tasks=new GameTask115(repo,project,game)){
    assertEquals("interrupted",tasks.read(id).getString("status"));tasks.control(id,"resume");var t=GameChallenge118Test.done(tasks,id);records.put(t);Files.writeString(out.resolve("result.json"),new JSONObject().put("project",project.toString()).put("tasks",records).toString(2));
    assertEquals(t.optString("reason"),"completed",t.getString("status"));var a=tasks.verify(id).getJSONObject("audit");assertEquals("revisit121",a.getString("completionControl"));assertEquals("won",a.getString("existingAI"));assertTrue(Set.of("won","failed").contains(a.getString("simpleControl")));assertEquals(1,a.getInt("aiPickups"));assertEquals(0,a.getInt("aiDamage"));assertEquals(0,a.getInt("repairCount"));var o=a.getJSONObject("aiOpportunity");if(request.contains("seed=127")){assertEquals(1,o.getInt("revisitStarts"));assertTrue(o.getInt("revisitTicks")>0);}else assertTrue(o.getJSONObject("motion121").getInt("reversals")>0);
    var finalController=t.getJSONArray("trials").getJSONObject(t.getJSONArray("trials").length()-1).getJSONObject("final").getJSONObject("controller");
    GameEvidence121.verify(finalController,10000);
    for(String field:List.of("returnReach","checks","stabilityTicks","tracked","reversals")){var bad=WorldFrames.copy(finalController);bad.getJSONObject("opportunity120").getJSONObject("motion121").put(field,999999);assertThrows(IllegalStateException.class,()->GameEvidence121.verify(bad,10000));}
    if(o.getJSONObject("motion121").getJSONArray("events").length()>0){var bad=WorldFrames.copy(finalController);var e=bad.getJSONObject("opportunity120").getJSONObject("motion121").getJSONArray("events").getJSONObject(0);e.put("afterVx",e.getDouble("beforeVx"));assertThrows(IllegalStateException.class,()->GameEvidence121.verify(bad,10000));}

    Path checkpoint=project.resolve("game-tasks115/"+id+".json");String original=Files.readString(checkpoint);var forged=AgentFiles042.loadChecked(checkpoint);forged.getJSONObject("audit").getJSONObject("aiOpportunity").put("revisitTicks",999999);AgentFiles042.saveChecked(checkpoint,forged);assertThrows(IllegalStateException.class,()->tasks.verify(id));Files.writeString(checkpoint,original);
   }
  }
 }
}
