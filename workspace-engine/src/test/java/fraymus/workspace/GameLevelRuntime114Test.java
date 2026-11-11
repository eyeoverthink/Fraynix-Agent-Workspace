package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

/** Real original renderer and physics. All movement comes through bounded inputs. */
public class GameLevelRuntime114Test {
    @Test public void generatedLevelRunsWithActualControlsAndRestartsFromItsSource() throws Exception {
        long started=System.nanoTime();Path repo=Path.of("..").toAbsolutePath().normalize(),project=Files.createTempDirectory("level114-runtime"),out=Path.of("build/level114-runtime");Files.createDirectories(out);
        var store=new GameLevels114(repo,project);var m=store.create(new JSONObject());var trace=new JSONArray();var result=new JSONObject().put("manifest",m).put("trace",trace).put("control","Simple gap-aware input; no player-position writes").put("passed",false);
        try(var game=new EmbeddedGameClient(repo,project)){
            var first=store.launch(game,m.getString("id"));assertEquals(m.getString("id"),first.getString("levelId"));var initial=first.getJSONObject("state");assertFalse(initial.getBoolean("visibleWindow"));assertFalse(initial.getBoolean("editorInitialized"));assertEquals(m.getInt("objects")+1,initial.getInt("objects"));
            Files.write(out.resolve("start.png"),Base64.getDecoder().decode(first.getString("pngBase64")));JSONObject frame=first;
            for(int n=0;n<180;n++){
                var state=frame.getJSONObject("state");var p=state.getJSONObject("player");trace.put(state);
                if(p.getBoolean("won")||p.getBoolean("dead"))break;
                double x=p.getDouble("x");boolean jump=false;
                for(Object g:m.getJSONArray("gaps")){var gap=(JSONObject)g;if(x>gap.getDouble("startX")-.7&&x<gap.getDouble("endX")+.1)jump=true;}
                frame=game.command(new JSONObject().put("action","step").put("ticks",12).put("horizontal",1).put("jump",jump));
            }
            result.put("final",frame.getJSONObject("state"));Files.write(out.resolve("finish.png"),Base64.getDecoder().decode(frame.getString("pngBase64")));
            assertTrue("Actual finish contact required; inspect preserved trace",frame.getJSONObject("state").getJSONObject("player").getBoolean("won"));
            game.close();var reopened=store.launch(game,m.getString("id"));assertNotEquals(initial.getString("sessionId"),reopened.getJSONObject("state").getString("sessionId"));assertEquals(initial.getJSONObject("player").getDouble("x"),reopened.getJSONObject("state").getJSONObject("player").getDouble("x"),.001);assertFalse(reopened.getJSONObject("state").getJSONObject("player").getBoolean("won"));result.put("reopened",reopened.getJSONObject("state")).put("passed",true);
        }finally{result.put("elapsedSeconds",(System.nanoTime()-started)/1e9);Files.writeString(out.resolve("result.json"),result.toString(2));}
    }
}
