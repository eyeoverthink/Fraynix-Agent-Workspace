package fraymus.workspace;
import org.junit.Test;
import static org.junit.Assert.*;
import org.json.*;
import java.nio.file.*;
import java.util.*;

public class GameRecovery119Test {
    @Test public void recoveryCountersCannotInventAttemptsOrUnboundedDeferrals(){
        String id=UUID.randomUUID().toString();var c=new JSONObject().put("policy","recover119").put("pickupDeferrals",1).put("pickupSkips",0).put("contactChecks",63).put("contactOverrides",0).put("contactNanos",12).put("contactRecovery119",new JSONObject().put("attempts",0).put("phase","OBSERVE")).put("pickupDecisions",new JSONArray().put(new JSONObject().put("objectId",id).put("tick",3).put("action","deferred for fresh observation").put("reason","Enemy motion intersects the pickup approach")));
        GameEvidence119.verify(c,63);
        var negative=WorldFrames.copy(c);negative.put("pickupDeferrals",-1);assertThrows(IllegalStateException.class,()->GameEvidence119.verify(negative,63));
        var repeated=WorldFrames.copy(c);repeated.put("pickupDeferrals",2);repeated.getJSONArray("pickupDecisions").put(WorldFrames.copy(repeated.getJSONArray("pickupDecisions").getJSONObject(0)).put("tick",30));assertThrows(IllegalStateException.class,()->GameEvidence119.verify(repeated,63));
        var invented=WorldFrames.copy(c);invented.getJSONObject("contactRecovery119").put("attempts",4);assertThrows(IllegalStateException.class,()->GameEvidence119.verify(invented,63));
        var uncounted=WorldFrames.copy(c);uncounted.put("pickupDeferrals",2);assertThrows(IllegalStateException.class,()->GameEvidence119.verify(uncounted,63));
    }
    @Test public void retainedFailuresCompleteThroughVerifiedTaskAndRecovery()throws Exception{
        Path repo=Path.of("..").toAbsolutePath().normalize(),project=Files.createTempDirectory("recovery119-task"),out=Path.of("build/recovery119-task");Files.createDirectories(out);
        var records=new JSONArray();
        for(String request:List.of("Create a playable Mario level seed=8 lengthTiles=72 gapTiles=4 secondGapTiles=2 goombaTile=26 obstacleTile=24 obstacleHeight=2","Create a playable Mario level seed=23 lengthTiles=64 gapTiles=2 secondGapTiles=3 goombaTile=14 turtleTile=27 obstacleTile=24 obstacleHeight=2")){
            String id=UUID.randomUUID().toString();
            try(var game=new EmbeddedGameClient(repo,project);var tasks=new GameTask115(repo,project,game)){
                tasks.start(new JSONObject().put("id",id).put("request",request));tasks.advance(id);tasks.advance(id);tasks.advance(id);
            }
            try(var game=new EmbeddedGameClient(repo,project);var tasks=new GameTask115(repo,project,game)){
                assertEquals("interrupted",tasks.read(id).getString("status"));tasks.control(id,"resume");var t=GameChallenge118Test.done(tasks,id);records.put(t);Files.writeString(out.resolve("result.json"),new JSONObject().put("project",project.toString()).put("tasks",records).toString(2));
                assertEquals(t.optString("reason"),"completed",t.getString("status"));var audit=tasks.verify(id).getJSONObject("audit");assertEquals("contact122",audit.getString("completionControl"));assertEquals("won",audit.getString("existingAI"));assertEquals("failed",audit.getString("simpleControl"));assertEquals(1,audit.getInt("aiPickups"));assertEquals(0,audit.getInt("aiDamage"));if(request.contains("seed=8"))assertTrue(audit.getInt("aiContactAttempts")>0);
                if(request.contains("seed=23"))assertEquals(1,audit.getJSONObject("aiOpportunity").getInt("waitClears"));
                Path checkpoint=project.resolve("game-tasks115/"+id+".json");String original=Files.readString(checkpoint);var forged=AgentFiles042.loadChecked(checkpoint);forged.getJSONObject("audit").put("aiDamage",1);AgentFiles042.saveChecked(checkpoint,forged);assertThrows(IllegalStateException.class,()->tasks.verify(id));Files.writeString(checkpoint,original);
            }
        }
    }
}
