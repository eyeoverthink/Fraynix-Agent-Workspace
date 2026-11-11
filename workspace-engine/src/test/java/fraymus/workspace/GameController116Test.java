package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

public class GameController116Test {
    @Test public void compareFreshControllersOnTheFailureAndNewLayouts() throws Exception {
        Path repo=Path.of("..").toAbsolutePath().normalize(), project=Files.createTempDirectory("controller116");
        Path out=Path.of(System.getProperty("study116.output","build/controller116"));Files.createDirectories(out);
        var result=new JSONObject().put("project",project.toString()).put("passed",false).put("cases",new JSONArray());
        int[][] cases=Boolean.getBoolean("study116.probe")?new int[][]{{0,56,2,2}}:new int[][]{{0,56,2,2},{1,48,2,2},{4,64,2,2},{8,72,2,2},{11,56,1,2},{17,64,2,1}};
        try(var game=new EmbeddedGameClient(repo,project)) {
            var levels=new GameLevels114(repo,project);
            for(int[] c:cases){
                var params=new JSONObject().put("seed",c[0]).put("lengthTiles",c[1]).put("gapTiles",c[2]).put("secondGapTiles",c[3]);
                var candidate=levels.create115(UUID.randomUUID().toString(),params);
                var row=new JSONObject().put("parameters",params).put("candidate",candidate).put("trials",new JSONArray());result.getJSONArray("cases").put(row);
                for(String policy:List.of("simple","legacy115","landing116")) {
                    long started=System.nanoTime();var frame=levels.launch(game,candidate.getString("id"));
                    var trial=new JSONObject().put("policy",policy).put("initial",frame.getJSONObject("state")).put("steps",new JSONArray());row.getJSONArray("trials").put(trial);
                    for(int i=0;i<180;i++){
                        var before=frame.getJSONObject("state");double x=before.getJSONObject("player").getDouble("x");boolean jump=false;
                        for(Object v:candidate.getJSONArray("gaps")){var g=(JSONObject)v;if(x>g.getDouble("startX")-.7&&x<g.getDouble("endX")+.1)jump=true;}
                        var input=policy.equals("simple")?new JSONObject().put("action","step").put("ticks",12).put("horizontal",1).put("jump",jump):new JSONObject().put("action","ai-step").put("ticks",12).put("targetX",candidate.getDouble("finishX")+1).put("policy",policy);
                        frame=game.command(input);var after=frame.getJSONObject("state");assertEquals(before.getInt("tick")+12,after.getInt("tick"));assertEquals(before.getString("sessionId"),after.getString("sessionId"));
                        trial.getJSONArray("steps").put(new JSONObject().put("before",before).put("input",input).put("after",after));var p=after.getJSONObject("player");
                        if(p.getBoolean("won")||p.getBoolean("dead")||p.getDouble("y")<-.5)break;
                    }
                    trial.put("final",frame.getJSONObject("state")).put("won",frame.getJSONObject("state").getJSONObject("player").getBoolean("won")).put("elapsedNanos",System.nanoTime()-started);
                    Files.write(out.resolve("seed"+c[0]+"-"+policy+".png"),Base64.getDecoder().decode(frame.getString("pngBase64")));
                    Files.writeString(out.resolve("result.json"),result.toString(2));game.close();
                }
            }
            var failure=result.getJSONArray("cases").getJSONObject(0).getJSONArray("trials");
            assertTrue("Simple baseline must finish",failure.getJSONObject(0).getBoolean("won"));
            assertFalse("Retain the original seed0 counterexample",failure.getJSONObject(1).getBoolean("won"));
            assertTrue("Revised controller must finish the retained failure",failure.getJSONObject(2).getBoolean("won"));
            int original=0,revised=0;for(Object value:result.getJSONArray("cases")){var trials=((JSONObject)value).getJSONArray("trials");if(trials.getJSONObject(1).getBoolean("won"))original++;if(trials.getJSONObject(2).getBoolean("won"))revised++;}
            assertTrue("Revised control must improve measured completion",revised>original);result.put("originalWins",original).put("revisedWins",revised).put("passed",true);
        } finally {Files.writeString(out.resolve("result.json"),result.toString(2));}
    }
}
