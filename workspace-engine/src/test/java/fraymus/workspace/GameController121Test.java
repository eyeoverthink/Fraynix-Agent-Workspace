package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

public class GameController121Test {
    static JSONObject c(String name,int seed,int length,int g1,int g2,int goomba,int turtle,int obstacle,int height,int pickup){return new JSONObject().put("name",name).put("parameters",new JSONObject().put("seed",seed).put("lengthTiles",length).put("gapTiles",g1).put("secondGapTiles",g2).put("goombaTile",goomba).put("turtleTile",turtle).put("obstacleTile",obstacle).put("obstacleHeight",height).put("pickupTile",pickup));}
    static List<JSONObject> cases(boolean reserved){return reserved?List.of(
        c("reserved-late-goomba",137,72,2,3,46,0,0,0,42),c("reserved-late-turtle",139,72,3,2,0,47,0,0,43),c("reserved-return-room",149,72,2,2,48,0,60,1,44),c("reserved-reversal",151,64,2,3,15,0,13,1,10),c("reserved-turtle-reversal",157,72,3,2,0,16,14,2,11),c("reserved-gap-refusal",163,72,3,2,29,0,26,2,18)):
        List.of(c("late-goomba",107,72,2,2,44,0,0,0,40),c("late-near-guard",109,72,2,2,42,0,0,0,40),c("late-turtle",113,72,2,2,0,44,0,0,40),c("late-room",127,72,2,2,46,0,60,1,42),c("goomba-reversal",110,64,2,2,15,0,13,1,10),c("turtle-reversal",112,64,2,2,0,15,13,1,10),c("early-room-refusal",104,56,2,2,8,0,24,2,8),c("gap-refusal",50,72,3,2,28,0,25,2,18));}
    static List<JSONObject> partitionCases(boolean reserved){return cases(reserved);}
    @Test public void compareContactRecoveryAndPickupDecisions()throws Exception{
        boolean reserved=Boolean.getBoolean("study121.reserved");String policies=System.getProperty("study121.policies","simple,opportunity120,observe121,revisit121"),only=System.getProperty("study121.only","");
        Path repo=Path.of("..").toAbsolutePath().normalize(),project=Files.createTempDirectory("controller121"),out=Path.of(System.getProperty("study121.output","build/controller121"));Files.createDirectories(out);
        var result=new JSONObject().put("passed",false).put("partition",System.getProperty("study121.partition",reserved?"reserved":"development")).put("project",project.toString()).put("cases",new JSONArray());
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
                        frame=game.command(input);var after=frame.getJSONObject("state");if(policy.equals("contact122"))GameEvidence122.verify(after.getJSONObject("controller"),after.getInt("tick"));assertEquals(before.getInt("tick")+12,after.getInt("tick"));assertEquals(before.getString("sessionId"),after.getString("sessionId"));if(!policy.equals("simple"))GameEvidence121.verify(after.getJSONObject("controller"),after.getInt("tick"));if(policy.equals("opportunity120"))GameEvidence120.verify(after.getJSONObject("controller"),after.getInt("tick"));if(policy.equals("recover119"))GameEvidence119.verify(after.getJSONObject("controller"),after.getInt("tick"));trial.getJSONArray("steps").put(new JSONObject().put("before",before).put("input",input).put("after",after));var p=after.getJSONObject("player");if(p.getBoolean("won")||p.getBoolean("dead")||p.getDouble("y")<-.5)break;
                    }
                    trial.put("final",frame.getJSONObject("state")).put("won",frame.getJSONObject("state").getJSONObject("player").getBoolean("won")).put("elapsedNanos",System.nanoTime()-started);
                    Files.write(out.resolve(row.getString("name")+"-"+policy+".png"),Base64.getDecoder().decode(frame.getString("pngBase64")));Files.writeString(out.resolve("result.json"),result.toString(2));game.close();
                }
            }
            if(Boolean.parseBoolean(System.getProperty("study121.requireBenefit",Boolean.toString(!reserved&&only.isEmpty()&&policies.contains("revisit121")&&policies.contains("observe121")&&policies.contains("opportunity120"))))){
                var course=result.getJSONArray("cases").toList().stream().map(x->new JSONObject((Map<?,?>)x)).filter(x->x.getString("name").equals("late-room")).findFirst().orElseThrow();
                for(Object raw:course.getJSONArray("trials")){var t=(JSONObject)raw;String p=t.getString("policy");if(!Set.of("revisit121","observe121","opportunity120").contains(p))continue;var c=t.getJSONObject("final").getJSONObject("controller");assertTrue(t.getBoolean("won"));assertEquals(0,c.getInt("damage"));assertEquals(p.equals("revisit121")?1:0,c.getInt("pickups"));if(p.equals("revisit121")){assertEquals(1,c.getJSONObject("opportunity120").getInt("revisitStarts"));assertTrue(c.getJSONObject("opportunity120").getInt("revisitTicks")>0);}}
            }
            assertTrue(result.getJSONArray("cases").length()>0);result.put("passed",true).put("scope","Native input-only trials; explicit requireWins gate for final development; reserved cases retain all outcomes.");
        }finally{Files.writeString(out.resolve("result.json"),result.toString(2));}
    }
}
