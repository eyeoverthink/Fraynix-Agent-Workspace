package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

public class GameController118Test {
    static JSONObject c(String name,int seed,int length,int g1,int g2,int goomba,int turtle,int obstacle,int height,int pickup){return new JSONObject().put("name",name).put("parameters",new JSONObject().put("seed",seed).put("lengthTiles",length).put("gapTiles",g1).put("secondGapTiles",g2).put("goombaTile",goomba).put("turtleTile",turtle).put("obstacleTile",obstacle).put("obstacleHeight",height).put("pickupTile",pickup));}
    static List<JSONObject> cases(boolean reserved){return reserved?List.of(c("reserved-gap-enemy",5,64,3,2,14,0,25,1,18),c("reserved-step-enemy",8,72,4,2,26,0,24,2,0),c("reserved-turtle",11,56,2,2,0,15,0,0,0),c("reserved-high-step",16,64,2,2,0,0,26,3,0),c("reserved-mixed-gap",20,72,3,3,28,30,25,1,20),c("reserved-two-enemies",23,64,2,3,14,27,24,2,0)):List.of(c("near-goomba",0,56,2,2,13,0,0,0,0),c("later-goomba",0,56,2,2,24,0,0,0,0),c("near-turtle",0,56,2,2,0,14,0,0,0),c("low-step",0,56,2,2,0,0,24,1,0),c("high-step",0,56,2,2,0,0,24,3,0),c("guarded-step",0,56,2,2,26,0,24,2,0),c("two-enemies",0,56,2,2,24,27,0,0,0),c("gap-pickup",0,56,2,2,0,0,0,0,17),c("low-clearance",0,56,2,2,0,0,12,2,12),c("obstructed-approach",0,56,2,2,0,0,10,3,13),c("mixed-gap",0,56,2,2,27,0,24,2,17),c("shifted-gap",4,64,2,2,13,0,0,0,18));}
    @Test public void actualEnemiesObstaclesAndRiskDecisions()throws Exception{
        boolean reserved=Boolean.getBoolean("study118.reserved");String policies=System.getProperty("study118.policies","simple,safe117,risk118"),only=System.getProperty("study118.only","");
        Path repo=Path.of("..").toAbsolutePath().normalize(),project=Files.createTempDirectory("controller118"),out=Path.of(System.getProperty("study118.output","build/controller118"));Files.createDirectories(out);
        var result=new JSONObject().put("passed",false).put("partition",reserved?"reserved":"development").put("project",project.toString()).put("cases",new JSONArray());
        try(var game=new EmbeddedGameClient(repo,project)){
            var levels=new GameLevels114(repo,project);
            for(var row:cases(reserved)){
                if(!only.isEmpty()&&!Arrays.asList(only.split(",")).contains(row.getString("name")))continue;
                var candidate=levels.create115(UUID.randomUUID().toString(),row.getJSONObject("parameters"));row.put("candidate",candidate).put("trials",new JSONArray());result.getJSONArray("cases").put(row);
                for(String policy:policies.split(",")){
                    long started=System.nanoTime();var frame=levels.launch(game,candidate.getString("id"));var trial=new JSONObject().put("policy",policy).put("initial",frame.getJSONObject("state")).put("steps",new JSONArray());row.getJSONArray("trials").put(trial);
                    assertEquals((row.getJSONObject("parameters").getInt("goombaTile")>0?1:0)+(row.getJSONObject("parameters").getInt("turtleTile")>0?1:0),frame.getJSONObject("state").getJSONObject("threats118").getInt("total"));
                    for(int i=0;i<180;i++){
                        var before=frame.getJSONObject("state");float x=before.getJSONObject("player").getFloat("x");boolean jump=false;for(Object raw:candidate.getJSONArray("gaps")){var gap=(JSONObject)raw;if(x>gap.getDouble("startX")-.7&&x<gap.getDouble("endX")+.1)jump=true;}
                        var input=policy.equals("simple")?new JSONObject().put("action","step").put("ticks",12).put("horizontal",1).put("jump",jump):new JSONObject().put("action","ai-step").put("ticks",12).put("targetX",candidate.getDouble("finishX")+1).put("policy",policy);
                        frame=game.command(input);var after=frame.getJSONObject("state");assertEquals(before.getInt("tick")+12,after.getInt("tick"));assertEquals(before.getString("sessionId"),after.getString("sessionId"));trial.getJSONArray("steps").put(new JSONObject().put("before",before).put("input",input).put("after",after));var p=after.getJSONObject("player");if(p.getBoolean("won")||p.getBoolean("dead")||p.getDouble("y")<-.5)break;
                    }
                    trial.put("final",frame.getJSONObject("state")).put("won",frame.getJSONObject("state").getJSONObject("player").getBoolean("won")).put("elapsedNanos",System.nanoTime()-started);
                    Files.write(out.resolve(row.getString("name")+"-"+policy+".png"),Base64.getDecoder().decode(frame.getString("pngBase64")));Files.writeString(out.resolve("result.json"),result.toString(2));game.close();
                }
            }
            if(!reserved&&only.isEmpty()&&Arrays.asList(policies.split(",")).contains("risk118")){
                assertEquals(12,result.getJSONArray("cases").length());
                for(Object raw:result.getJSONArray("cases")){var row=(JSONObject)raw;for(Object value:row.getJSONArray("trials")){
                    var trial=(JSONObject)value;if(!trial.getString("policy").equals("risk118"))continue;
                    assertTrue("Development regression: "+row.getString("name"),trial.getBoolean("won"));assertEquals(0,trial.getJSONObject("final").getJSONObject("controller").getInt("damage"));
                }}
            }
            assertTrue(result.getJSONArray("cases").length()>0);result.put("passed",true).put("scope","Development risk118 finish/damage regression gates apply when the full development cohort is requested; reserved outcomes remain measured, including retained failures.");
        }finally{Files.writeString(out.resolve("result.json"),result.toString(2));}
    }
}
