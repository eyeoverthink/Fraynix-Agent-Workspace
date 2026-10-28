package editor;

import components.MouseControls;
import fraymus.factory.SchemaRegistry;
import factory.FactorySceneAdapter;
import org.junit.Test;
import scenes.Scene;
import java.nio.file.*;
import static org.junit.Assert.*;

public class EditorCorrectionsTest {
    @Test public void assetSelectionDoesNotInsertAndPlacementRequiresExplicitMode() {
        Scene scene=new Scene(null);var tool=new MouseControls();var template=scene.createGameObject("Selected tile");
        template.transform.scale.set(.25f,.25f);tool.pickupObject(template);
        assertTrue(tool.hasAsset());assertFalse(tool.placing());assertEquals(0,scene.getGameObjects().size());
        try {tool.placeAt(scene,1,2);fail("Selection inserted an object");} catch(IllegalStateException expected) {}
        tool.enablePlacement();var placed=tool.placeAt(scene,1,2);
        assertEquals(1,scene.getGameObjects().size());assertNotEquals(template.getPersistentId(),placed.getPersistentId());
        tool.selectTool();assertFalse(tool.placing());assertEquals(1,scene.getGameObjects().size());
        tool.pickupObject(template);assertFalse(tool.placing());assertEquals(1,scene.getGameObjects().size());
    }
    @Test public void exportedOriginalLevelCanBeImportedAndInvalidTypesAreRejected() throws Exception {
        SchemaRegistry schemas=new SchemaRegistry();FactorySceneAdapter.registerSchemas(schemas);
        Scene original=LevelDocumentIO.read(Path.of("levels/mario-original.json"),schemas);
        Path saved=Files.createTempDirectory("level-roundtrip-").resolve("my level.json");
        LevelDocumentIO.write(original,saved);Scene restored=LevelDocumentIO.read(saved,schemas);
        assertEquals(original.serializeAuthoring(),restored.serializeAuthoring());
        Files.writeString(saved,"[{\"name\":\"bad\",\"components\":[{\"type\":\"java.lang.Runtime\",\"properties\":{}}]}]");
        try {LevelDocumentIO.read(saved,schemas);fail("Unregistered type imported");} catch(java.io.IOException expected) {assertTrue(expected.getMessage().contains("rejected"));}
        assertEquals(1248,original.getGameObjects().size());
    }
}
