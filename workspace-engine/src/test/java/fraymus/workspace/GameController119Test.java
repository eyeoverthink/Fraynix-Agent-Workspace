package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

public class GameController119Test {
    static JSONObject c(String name,int seed,int length,int g1,int g2,int goomba,int turtle,int obstacle,int height,int pickup){return new JSONObject().put("name",name).put("parameters",new JSONObject().put("seed",seed).put("lengthTiles",length).put("gapTiles",g1).put("secondGapTiles",g2).put("goombaTile",goomba).put("turtleTile",turtle).put("obstacleTile",obstacle).put("obstacleHeight",height).put("pickupTile",pickup));}
    static List<JSONObject> cases(boolean reserved){return reserved?List.of(
        c("new-step-enemy",32,64,4,3,27,0,24,2,0),c("new-two-enemies",41,72,3,4,15,28,25,2,0),c("new-low-step",44,56,4,2,0,0,24,1,0),c("new-high-guard",47,64,2,3,14,28,25,3,0),c("new-gap-pickup",50,72,3,2,28,0,25,2,18),c("new-turtle-step",53,64,2,2,0,15,26,2,0)):
        List.of(c("retained-step",8,72,4,2,26,0,24,2,0),c("retained-two-enemies",23,64,2,3,14,27,24,2,0),c("step-shift",2,64,4,2,26,0,24,2,0),c("unguarded-step",14,56,4,2,0,0,24,2,0),c("two-enemies-shift",29,72,3,2,15,28,24,2,0),c("two-enemies-short",3,56,2,3,14,27,24,2,0));}
    static List<JSONObject> partitionCases(boolean reserved){return switch(System.getProperty("study119.partition","new")){case "old-development"->GameController118Test.cases(false);case "old-reserved"->GameController118Test.cases(true);case "new"->cases(reserved);default->throw new IllegalArgumentException("Unknown partition");};}
    @Test public void compareContactRecoveryAndPickupDecisions()throws Exception{
        boolean reserved=Boolean.getBoolean("study119.reserved");String policies=System.getProperty("study119.policies","simple,safe117,risk118,recover119"),only=System.getProperty("study119.only","");
        Path repo=Path.of("..").toAbsolutePath().normalize(),project=Files.createTempDirectory("controller119"),out=Path.of(System.getProperty("study119.output","build/controller119"));Files.createDirectories(out);
        var result=new JSONObject().put("passed",false).put("partition",System.getProperty("study119.partition",reserved?"reserved":"development")).put("project",project.toString()).put("cases",new JSONArray());
        try(var game=new EmbeddedGameClient(repo,project)){
            var levels=new GameLevels114(repo,project);
            for(var row:partitionCases(reserved)){
                if(!only.isEmpty()&&!Arrays.asList(only.split(",")).contains(row.getString("name")))continue;
                var candidate=levels.create115(UUID.randomUUID().toString(),row.getJSONObject("parameters"));row.put("candidate",candidate).put("trials",new JSONArray());result.getJSONArray("cases").put(row);
                for(String policy:policies.split(",")){
                    long started=System.nanoTime();var frame=levels.launch(game,candidate.getString("id"));var trial=new JSONObject().put("policy",policy).put("initial",frame.getJSONObject("state")).put("steps",new JSONArray());row.getJSONArray("trials").put(trial);
                    assertEquals((row.getJSONObject("parameters").getInt("goombaTile")>0?1:0)+(row.getJSONObject("parameters").getInt("turtleTile")>0?1:0),frame.getJSONObject("state").getJSONObject("threats118").getInt("total"));
                    for(int i=0;i<180;i++){
                        var before=frame.getJSONObject("state");float x=before.getJSONObject("player").getFloat("x");boolean jump=false;for(Object raw:candidate.getJSONArray("gaps")){var gap=(JSONObject)raw;if(x>gap.getDouble("startX")-.7&&x<gap.getDouble("endX")+.1)jump=true;}
                        var input=policy.equals("simple")?new JSONObject().put("action","step").put("ticks",12).put("horizontal",1).put("jump",jump):new JSONObject().put("action","ai-step").put("ticks",12).put("targetX",candidate.getDouble("finishX")+1).put("policy",policy);
                        frame=game.command(input);var after=frame.getJSONObject("state");assertEquals(before.getInt("tick")+12,after.getInt("tick"));assertEquals(before.getString("sessionId"),after.getString("sessionId"));if(policy.equals("recover119"))GameEvidence119.verify(after.getJSONObject("controller"),after.getInt("tick"));trial.getJSONArray("steps").put(new JSONObject().put("before",before).put("input",input).put("after",after));var p=after.getJSONObject("player");if(p.getBoolean("won")||p.getBoolean("dead")||p.getDouble("y")<-.5)break;
                    }
                    trial.put("final",frame.getJSONObject("state")).put("won",frame.getJSONObject("state").getJSONObject("player").getBoolean("won")).put("elapsedNanos",System.nanoTime()-started);
                    Files.write(out.resolve(row.getString("name")+"-"+policy+".png"),Base64.getDecoder().decode(frame.getString("pngBase64")));Files.writeString(out.resolve("result.json"),result.toString(2));game.close();
                }
            }
            if(Boolean.parseBoolean(System.getProperty("study119.requireWins",reserved?"false":"true")))for(Object raw:result.getJSONArray("cases"))for(Object value:((JSONObject)raw).getJSONArray("trials")){
                var trial=(JSONObject)value;if(!trial.getString("policy").equals("recover119"))continue;
                assertTrue("Recovery regression: "+((JSONObject)raw).getString("name"),trial.getBoolean("won"));assertEquals(0,trial.getJSONObject("final").getJSONObject("controller").getInt("damage"));
            }
            assertTrue(result.getJSONArray("cases").length()>0);result.put("passed",true).put("scope","Native input-only trials; explicit requireWins gate for final development; reserved cases retain all outcomes.");
        }finally{Files.writeString(out.resolve("result.json"),result.toString(2));}
    }
}
