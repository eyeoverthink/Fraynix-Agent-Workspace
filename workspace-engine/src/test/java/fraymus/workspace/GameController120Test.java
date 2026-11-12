package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

public class GameController120Test {
    static JSONObject c(String name,int seed,int length,int g1,int g2,int goomba,int turtle,int obstacle,int height,int pickup){return new JSONObject().put("name",name).put("parameters",new JSONObject().put("seed",seed).put("lengthTiles",length).put("gapTiles",g1).put("secondGapTiles",g2).put("goombaTile",goomba).put("turtleTile",turtle).put("obstacleTile",obstacle).put("obstacleHeight",height).put("pickupTile",pickup));}
    static List<JSONObject> cases(boolean reserved){return reserved?List.of(
        c("new-goomba",71,64,3,2,16,0,26,2,0),c("new-turtle",73,72,2,3,0,16,27,1,0),c("new-two-guards",79,72,3,3,16,29,26,2,0),c("new-late-guard",83,64,2,2,17,0,25,2,0),c("new-gap-refusal",89,72,4,2,29,0,26,2,18),c("new-cramped",97,56,2,3,0,28,12,2,12)):
        List.of(c("retained-two-enemies",41,72,3,4,15,28,25,2,0),c("retained-turtle",53,64,2,2,0,15,26,2,0),c("retained-gap-refusal",50,72,3,2,28,0,25,2,18),c("shifted-goomba",61,64,2,3,16,0,26,1,0),c("shifted-turtle",64,72,3,3,0,16,27,2,0),c("cramped-refusal",65,56,2,2,0,0,12,2,12),c("early-pickup",68,56,2,2,10,0,24,2,8));}
    static List<JSONObject> partitionCases(boolean reserved){return switch(System.getProperty("study120.partition","new")){case "early-probes"->List.of(c("earlier-guard",101,56,2,2,9,0,24,2,8),c("later-guard",102,56,2,2,11,0,24,2,8),c("later-pickup",103,56,2,2,11,0,24,2,10),c("closest-guard",104,56,2,2,8,0,24,2,8));case "old119-development"->GameController119Test.cases(false);case "old119-reserved"->GameController119Test.cases(true);case "old118-development"->GameController118Test.cases(false);case "old118-reserved"->GameController118Test.cases(true);case "new"->cases(reserved);default->throw new IllegalArgumentException("Unknown partition");};}
    @Test public void compareContactRecoveryAndPickupDecisions()throws Exception{
        boolean reserved=Boolean.getBoolean("study120.reserved");String policies=System.getProperty("study120.policies","simple,safe117,recover119,opportunity120"),only=System.getProperty("study120.only","");
        Path repo=Path.of("..").toAbsolutePath().normalize(),project=Files.createTempDirectory("controller120"),out=Path.of(System.getProperty("study120.output","build/controller120"));Files.createDirectories(out);
        var result=new JSONObject().put("passed",false).put("partition",System.getProperty("study120.partition",reserved?"reserved":"development")).put("project",project.toString()).put("cases",new JSONArray());
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
                        frame=game.command(input);var after=frame.getJSONObject("state");assertEquals(before.getInt("tick")+12,after.getInt("tick"));assertEquals(before.getString("sessionId"),after.getString("sessionId"));GameEvidence121.verify(after.getJSONObject("controller"),after.getInt("tick"));if(policy.equals("opportunity120"))GameEvidence120.verify(after.getJSONObject("controller"),after.getInt("tick"));if(policy.equals("recover119"))GameEvidence119.verify(after.getJSONObject("controller"),after.getInt("tick"));trial.getJSONArray("steps").put(new JSONObject().put("before",before).put("input",input).put("after",after));var p=after.getJSONObject("player");if(p.getBoolean("won")||p.getBoolean("dead")||p.getDouble("y")<-.5)break;
                    }
                    trial.put("final",frame.getJSONObject("state")).put("won",frame.getJSONObject("state").getJSONObject("player").getBoolean("won")).put("elapsedNanos",System.nanoTime()-started);
                    Files.write(out.resolve(row.getString("name")+"-"+policy+".png"),Base64.getDecoder().decode(frame.getString("pngBase64")));Files.writeString(out.resolve("result.json"),result.toString(2));game.close();
                }
            }
            if(Boolean.parseBoolean(System.getProperty("study120.requireWins","false")))for(Object raw:result.getJSONArray("cases"))for(Object value:((JSONObject)raw).getJSONArray("trials")){
                var trial=(JSONObject)value;if(!Set.of("opportunity120","revisit121").contains(trial.getString("policy")))continue;
                assertTrue("Recovery regression: "+((JSONObject)raw).getString("name"),trial.getBoolean("won"));assertEquals(0,trial.getJSONObject("final").getJSONObject("controller").getInt("damage"));
                String name=((JSONObject)raw).getString("name");if(Set.of("retained-two-enemies","retained-turtle").contains(name))assertEquals(1,trial.getJSONObject("final").getJSONObject("controller").getInt("pickups"));if(name.equals("retained-gap-refusal"))assertEquals(0,trial.getJSONObject("final").getJSONObject("controller").getInt("pickups"));
            }
            assertTrue(result.getJSONArray("cases").length()>0);result.put("passed",true).put("scope","Native input-only trials; explicit requireWins gate for final development; reserved cases retain all outcomes.");
        }finally{Files.writeString(out.resolve("result.json"),result.toString(2));}
    }
}
