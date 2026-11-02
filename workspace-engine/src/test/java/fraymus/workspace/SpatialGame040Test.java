package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

public class SpatialGame040Test {
    @Test public void actualGameEntitiesAndOriginalRasterShareOneTickAndPlayerIdentity()throws Exception{
        var project=Files.createTempDirectory("spatial-game040-");try(var game=new EmbeddedGameClient(Path.of(".."),project)){
            game.open();var before=game.spatial();var state=before.getJSONObject("state");assertEquals(state.getInt("objects"),before.getJSONArray("entities").length());var ids=new HashSet<String>();for(Object raw:before.getJSONArray("entities"))assertTrue(ids.add(((JSONObject)raw).getString("id")));var frame=SpatialRuntime040.gameFrame(before);String player=state.getJSONObject("player").getString("id");var actual=frame.getJSONArray("entities").toList().stream().map(v->new JSONObject((Map<?,?>)v)).filter(e->e.getString("id").equals(player)).findFirst().orElseThrow();assertEquals(0,actual.getJSONArray("position").getDouble(2),0);assertEquals("quad",actual.getString("shape"));assertEquals(state.getJSONObject("player").getDouble("x"),actual.getJSONArray("position").getDouble(0),0);assertTrue(actual.getJSONArray("components").toList().contains("components.PlayerController"));assertEquals(PerceptionStore.hash(Base64.getDecoder().decode(before.getString("pngBase64"))),before.getString("pngSha256"));
            game.command(new JSONObject().put("action","step").put("ticks",30).put("horizontal",1).put("jump",false));var after=game.spatial();assertEquals(state.getLong("tick")+30,after.getJSONObject("state").getLong("tick"));assertEquals(player,after.getJSONObject("state").getJSONObject("player").getString("id"));assertTrue(after.getJSONObject("state").getJSONObject("player").getDouble("x")>state.getJSONObject("player").getDouble("x"));assertNotEquals(before.getString("pngSha256"),after.getString("pngSha256"));assertFalse(after.getJSONObject("state").getBoolean("visibleWindow"));assertFalse(after.getJSONObject("state").getBoolean("editorInitialized"));
            Files.writeString(Path.of("build/spatial-game040.json"),new JSONObject().put("passed",true).put("beforeTick",state.getLong("tick")).put("afterTick",after.getJSONObject("state").getLong("tick")).put("entities",ids.size()).put("playerId",player).put("scope","Actual hidden original Java renderer, exact owner-thread snapshot and bounded manual physics step").toString(2));
        }
    }
}
