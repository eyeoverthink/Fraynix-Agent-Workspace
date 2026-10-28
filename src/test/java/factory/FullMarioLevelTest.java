package factory;

import components.*;
import fraymus.factory.*;
import org.junit.Test;
import scenes.Scene;
import java.nio.file.Path;
import java.util.*;
import static org.junit.Assert.*;

public class FullMarioLevelTest {
    @Test public void importsEveryComponentAndRoundTripsTheOriginalLevel() throws Exception {
        Scene scene = new Scene(null); scene.load(Path.of("levels/mario-original.json"));
        assertEquals(1248, scene.getGameObjects().size());
        assertEquals(6085, scene.getGameObjects().stream().mapToInt(o -> o.getAllComponents().size()).sum());
        assertEquals(16, scene.getGameObjects().stream().flatMap(o -> o.getAllComponents().stream()).map(Object::getClass).distinct().count());
        assertEquals(20, scene.getGameObjectWith(PlayerController.class).getUid());
        Scene independentlyLoaded=new Scene(null); independentlyLoaded.load(Path.of("levels/mario-original.json"));
        assertEquals(scene.getGameObject(20).getPersistentId(),independentlyLoaded.getGameObject(20).getPersistentId());
        assertEquals(scene.getGameObject(20).getComponent(PlayerController.class).getPersistentId(),independentlyLoaded.getGameObject(20).getComponent(PlayerController.class).getPersistentId());
        Set<Integer> handles = new HashSet<>(); Set<UUID> identities = new HashSet<>();
        for (var object : scene.getGameObjects()) for (var component : object.getAllComponents()) {
            assertTrue("Duplicate component handle", handles.add(component.getUid()));
            assertTrue("Duplicate persistent identity", identities.add(component.getPersistentId()));
        }
        SchemaRegistry schemas = new SchemaRegistry(); FactorySceneAdapter.registerSchemas(schemas);
        assertTrue(new FactorySceneAdapter().verifyRoundTrip(scene, schemas).contains("1248 objects"));
        Scene copied = new Scene(null); copied.loadJson(scene.serializeAuthoring());
        assertEquals(scene.serializeAuthoring(), copied.serializeAuthoring());
    }

    @Test public void originalAssetsResolveAndPillboxRegistersBothFixtures() throws Exception {
        Scene scene = new Scene(null); scene.load(Path.of("levels/mario-original.json"));
        for (var object : scene.getGameObjects()) {
            var sprite = object.getComponent(SpriteRenderer.class);
            if (sprite != null && sprite.getTexture() != null) assertTrue(java.nio.file.Files.isRegularFile(Path.of(sprite.getTexture().getFilepath())));
            var pill = object.getComponent(physics2d.components.PillboxCollider.class);
            if (pill != null) pill.start();
            scene.physics().synchronize(object);
        }
        assertEquals(1156, scene.physics().bodyCount());
        var body = scene.getGameObjectWith(PlayerController.class).getComponent(physics2d.components.Rigidbody2D.class).getRawBody();
        assertNotNull(body.getFixtureList().getNext());
        assertNotNull(body.getFixtureList().getUserData());
    }

    @Test public void transformClonePreservesRotationAndLayer() {
        jade.Transform original = new jade.Transform(new org.joml.Vector2f(2,3), new org.joml.Vector2f(-.25f,.25f));
        original.rotation = 43; original.zIndex = 7;
        assertEquals(original, original.copy());
        jade.Transform destination = new jade.Transform(); original.copy(destination); assertEquals(original, destination);
    }
}
