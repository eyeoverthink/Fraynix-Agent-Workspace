package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

public class GameLevels114Test {
    final Path repo=Path.of("..").toAbsolutePath().normalize();
    @Test public void analyzesFullSourceAndDistinguishesExample() throws Exception {
        var value=new GameLevels114(repo,Files.createTempDirectory("levels114-analysis")).analyze();
        assertEquals(1248,value.getInt("objects"));assertEquals(6085,value.getInt("components"));assertEquals(16,value.getJSONObject("componentTypes").length());assertEquals(2,value.getInt("rootExampleObjects"));assertTrue(value.getJSONObject("assets").length()>3);
    }
    @Test public void savesVariedReconstructibleCandidatesWithoutChangingSource() throws Exception {
        Path project=Files.createTempDirectory("levels114-create");String before=GameLevels114.hash(repo.resolve("levels/mario-original.json"));var store=new GameLevels114(repo,project);var ids=new HashSet<String>();
        for(int seed:new int[]{0,4,9999}){
            var m=store.create(new JSONObject().put("seed",seed));var a=new JSONArray(Files.readString(Path.of(m.getString("levelPath"))));int players=0;
            for(Object object:a){var o=(JSONObject)object;assertTrue(ids.add(o.getString("persistentId")));for(Object component:o.getJSONArray("components")){var c=(JSONObject)component;assertTrue(ids.add(c.getJSONObject("properties").getString("persistentId")));if(c.getString("type").equals("components.PlayerController"))players++;}}
            assertEquals(1,players);assertEquals(2,m.getJSONArray("gaps").length());assertTrue(new GameLevels114(repo,project).verify(m.getString("id")).getBoolean("checked"));
        }
        assertEquals(before,GameLevels114.hash(repo.resolve("levels/mario-original.json")));assertEquals(3,store.list().getJSONArray("candidates").length());
    }
    @Test public void refusesInvalidParametersAndTraversal() throws Exception {
        var store=new GameLevels114(repo,Files.createTempDirectory("levels114-invalid"));
        for(JSONObject bad:List.of(new JSONObject().put("seed",.1),new JSONObject().put("lengthTiles",300),new JSONObject().put("gapTiles",3),new JSONObject().put("filename","../source"),new JSONObject().put("seed","2")))assertThrows(IllegalArgumentException.class,()->store.create(bad));
        assertThrows(IllegalArgumentException.class,()->store.verify("../source"));
    }
    @Test public void rejectsChangedCandidateEvenIfItsManifestHashIsUpdated() throws Exception {
        Path project=Files.createTempDirectory("levels114-change");var store=new GameLevels114(repo,project);var m=store.create(new JSONObject());Path level=Path.of(m.getString("levelPath"));var a=new JSONArray(Files.readString(level));a.getJSONObject(0).put("name","tampered");Files.writeString(level,a.toString());
        assertThrows(IllegalStateException.class,()->store.verify(m.getString("id")));
        m.put("levelSha256",GameLevels114.hash(level));Files.writeString(level.resolveSibling("manifest.json"),m.toString());assertThrows(IllegalStateException.class,()->store.verify(m.getString("id")));assertFalse(store.list().getJSONArray("candidates").getJSONObject(0).getBoolean("checked"));
    }
    @Test public void refusesChangedSourceAndAssetsOnAnIsolatedCopy() throws Exception {
        Path copy=Files.createTempDirectory("levels114-fixture"),project=Files.createTempDirectory("levels114-integrity");var analysis=new GameLevels114(repo,project).analyze();
        Set<String> files=new HashSet<>(analysis.getJSONObject("assets").keySet());files.add("levels/mario-original.json");files.add("level.txt");for(String file:files){Files.createDirectories(copy.resolve(file).getParent());Files.copy(repo.resolve(file),copy.resolve(file));}
        var store=new GameLevels114(copy,project);var m=store.create(new JSONObject());Path source=copy.resolve("levels/mario-original.json");String original=Files.readString(source);Files.writeString(source,original+" ");assertThrows(IllegalStateException.class,()->store.verify(m.getString("id")));Files.writeString(source,original);
        Path asset=copy.resolve(m.getJSONObject("assets").keySet().iterator().next());Files.write(asset,new byte[]{1,2,3});assertThrows(IllegalStateException.class,()->store.verify(m.getString("id")));
    }
}
