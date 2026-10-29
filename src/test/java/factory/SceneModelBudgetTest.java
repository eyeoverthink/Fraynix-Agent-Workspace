package factory;

import org.junit.Test;
import fraymus.factory.*;
import scenes.Scene;
import java.nio.file.Path;
import static org.junit.Assert.*;

public class SceneModelBudgetTest {
    @Test public void fullLevelProducesBoundedValidJsonAndRetainsSelectedEntity() throws Exception {
        Scene scene=new Scene(null);scene.load(Path.of("levels/mario-original.json"));
        SchemaRegistry schemas=new SchemaRegistry();FactorySceneAdapter.registerSchemas(schemas);FactoryWorld world=new FactoryWorld(schemas);
        new FactorySceneAdapter().capture(scene,world);var selected=scene.getGameObject(20).getPersistentId();
        var observation=SceneKnowledge.capture(scene,world.snapshot(),selected,false);
        String summary=observation.forModel(6000);assertTrue(summary.length()<=6000);
        var json=SceneJson.parse(summary);assertEquals(1248,json.get("totalEntities").getAsInt());assertTrue(json.get("partial").getAsBoolean());
        assertEquals("Mario",json.getAsJsonObject("player").get("character").getAsString());
        assertEquals("small",json.getAsJsonObject("player").get("form").getAsString());
        assertEquals(selected.toString(),json.getAsJsonArray("entities").get(0).getAsJsonObject().get("entityId").getAsString());
        String result=observation.queryForModel("{\"op\":\"components\",\"type\":\"components.Ground\"}",4000);
        assertTrue(result.length()<=4000);assertTrue(SceneJson.parse(result).get("partial").getAsBoolean());
        var detail=SceneJson.parse(observation.query("{\"op\":\"inspect\",\"entityId\":\""+selected+"\",\"componentType\":\"jade.Transform\",\"property\":\"position\"}"));
        assertEquals(1,detail.getAsJsonObject("entity").getAsJsonArray("components").size());
        assertTrue(observation.json().length()>summary.length());
    }
}
