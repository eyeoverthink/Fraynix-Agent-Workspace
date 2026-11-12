package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

public class GameController117Test {
    static final int[][] REGRESSION={{0,56,2,2},{1,48,2,2},{4,64,2,2},{8,72,2,2},{11,56,1,2},{17,64,2,1},{2,48,2,1},{5,64,1,2},{6,72,2,2},{10,56,2,2},{13,64,2,1},{16,72,1,2}};
    static final int[][] WIDER={{15,56,5,2},{9,48,4,3},{20,64,6,2},{23,72,5,4},{27,64,3,5},{30,56,4,4}};
    static final int[][] RESERVED={{22,48,3,3},{26,64,5,2},{29,72,4,5},{31,56,6,2},{35,64,5,4},{38,72,3,6}};
    @Test public void compareNativeControllersWithContactsAndPickups()throws Exception{
        String partition=System.getProperty("study117.partition","regression"),policies=System.getProperty("study117.policies","simple,legacy115,landing116,safe117");
        int[][] cases=switch(partition){case "probe"->new int[][]{{15,56,5,2},{0,56,2,2},{4,64,2,2}};case "wider"->WIDER;case "reserved"->RESERVED;case "regression"->REGRESSION;default->throw new IllegalArgumentException("Unknown partition");};
        Path repo=Path.of("..").toAbsolutePath().normalize(),project=Files.createTempDirectory("controller117"),out=Path.of(System.getProperty("study117.output","build/controller117"));Files.createDirectories(out);
        var result=new JSONObject().put("passed",false).put("partition",partition).put("project",project.toString()).put("cases",new JSONArray());
        try(var game=new EmbeddedGameClient(repo,project)){
            var levels=new GameLevels114(repo,project);
            for(int[] c:cases){
                var params=new JSONObject().put("seed",c[0]).put("lengthTiles",c[1]).put("gapTiles",c[2]).put("secondGapTiles",c[3]);var candidate=levels.create115(UUID.randomUUID().toString(),params);
                var row=new JSONObject().put("parameters",params).put("candidate",candidate).put("trials",new JSONArray());result.getJSONArray("cases").put(row);
                for(String policy:policies.split(",")){
                    long started=System.nanoTime();var frame=levels.launch(game,candidate.getString("id"));
                    var trial=new JSONObject().put("policy",policy).put("initial",frame.getJSONObject("state")).put("steps",new JSONArray());row.getJSONArray("trials").put(trial);
                    for(int i=0;i<180;i++){
                        var before=frame.getJSONObject("state");float x=before.getJSONObject("player").getFloat("x");boolean jump=false;
                        for(Object v:candidate.getJSONArray("gaps")){var gap=(JSONObject)v;if(x>gap.getDouble("startX")-.7&&x<gap.getDouble("endX")+.1)jump=true;}
                        var input=policy.equals("simple")?new JSONObject().put("action","step").put("ticks",12).put("horizontal",1).put("jump",jump):new JSONObject().put("action","ai-step").put("ticks",12).put("targetX",candidate.getDouble("finishX")+1).put("policy",policy);
                        frame=game.command(input);var after=frame.getJSONObject("state");if(policy.equals("contact122"))GameEvidence122.verify(after.getJSONObject("controller"),after.getInt("tick"));assertEquals(before.getInt("tick")+12,after.getInt("tick"));assertEquals(before.getString("sessionId"),after.getString("sessionId"));
                        trial.getJSONArray("steps").put(new JSONObject().put("before",before).put("input",input).put("after",after));var p=after.getJSONObject("player");if(p.getBoolean("won")||p.getBoolean("dead")||p.getDouble("y")<-.5)break;
                    }
                    trial.put("final",frame.getJSONObject("state")).put("won",frame.getJSONObject("state").getJSONObject("player").getBoolean("won")).put("elapsedNanos",System.nanoTime()-started);
                    Files.write(out.resolve("seed"+c[0]+"-"+policy+".png"),Base64.getDecoder().decode(frame.getString("pngBase64")));Files.writeString(out.resolve("result.json"),result.toString(2));game.close();
                }
            }
            // Prototype runs can request only baselines; final comparisons must meet all gates.
            if(policies.contains("safe117")){
                int previousPickups=0,newPickups=0;
                for(Object value:result.getJSONArray("cases")){var trials=((JSONObject)value).getJSONArray("trials");JSONObject revised=null,previous=null;
                    for(Object v:trials){var t=(JSONObject)v;if(t.getString("policy").equals("safe117"))revised=t;if(t.getString("policy").equals("landing116"))previous=t;}
                    assertNotNull(revised);assertTrue("New controller must finish declared case "+((JSONObject)value).getJSONObject("parameters"),revised.getBoolean("won"));
                    newPickups+=revised.getJSONObject("final").getJSONObject("controller").getInt("pickups");if(previous!=null)previousPickups+=previous.getJSONObject("final").getJSONObject("controller").getInt("pickups");
                }
                if(partition.equals("regression"))assertTrue("Collection must improve on116",newPickups>previousPickups);
                result.put("previousPickups",previousPickups).put("newPickups",newPickups);
            }
            if(policies.contains("risk118")||policies.contains("recover119")||policies.contains("opportunity120")||policies.contains("revisit121"))for(Object v:result.getJSONArray("cases"))for(Object raw:((JSONObject)v).getJSONArray("trials")){
                var trial=(JSONObject)raw;if(!Set.of("risk118","recover119","opportunity120","revisit121").contains(trial.getString("policy")))continue;
                assertTrue("118 must preserve previous finish",trial.getBoolean("won"));var controller=trial.getJSONObject("final").getJSONObject("controller");
                assertEquals("118 must preserve safe collection",1,controller.getInt("pickups"));assertEquals(0,controller.getInt("damage"));
            }
            result.put("passed",true);
        }finally{Files.writeString(out.resolve("result.json"),result.toString(2));}
    }
}
