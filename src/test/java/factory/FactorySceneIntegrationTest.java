package factory;

import com.google.gson.*;
import components.Component;
import components.ComponentDeserializer;
import fraymus.factory.*;
import jade.*;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.World;
import org.junit.Test;
import scenes.Scene;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

public class FactorySceneIntegrationTest {
    private Gson gson() { return new GsonBuilder().registerTypeAdapter(Component.class, new ComponentDeserializer()).registerTypeAdapter(GameObject.class, new GameObjectDeserializer()).create(); }
    private Scene scene() {
        Scene scene = new Scene(null);
        GameObject object = scene.createGameObject("test entity"); object.transform.position.set(12, 34); object.transform.scale.set(1, 1);
        scene.addGameObjectToScene(object); return scene;
    }

    @Test public void jsonPreservesIdentityAndCloneAllocatesFreshIdentity() {
        GameObject original = scene().getGameObjects().get(0);
        GameObject restored = gson().fromJson(gson().toJson(original), GameObject.class);
        assertEquals(original.getUid(), restored.getUid()); assertEquals(original.getPersistentId(), restored.getPersistentId());
        assertEquals(original.transform.getPersistentId(), restored.transform.getPersistentId());
        GameObject clone = original.copy();
        assertNotEquals(original.getUid(), clone.getUid()); assertNotEquals(original.getPersistentId(), clone.getPersistentId());
        assertNotEquals(original.transform.getUid(), clone.transform.getUid()); assertNotEquals(original.transform.getPersistentId(), clone.transform.getPersistentId());
    }

    @Test public void legacyJsonWithoutUuidIsMigrated() {
        JsonObject json = JsonParser.parseString(gson().toJson(scene().getGameObjects().get(0))).getAsJsonObject();
        json.remove("persistentId");
        json.getAsJsonArray("components").get(0).getAsJsonObject().getAsJsonObject("properties").remove("persistentId");
        GameObject migrated = gson().fromJson(json, GameObject.class);
        assertNotNull(migrated.getPersistentId()); assertNotNull(migrated.transform.getPersistentId());
        assertEquals(migrated.getPersistentId(), gson().fromJson(gson().toJson(migrated), GameObject.class).getPersistentId());
    }

    @Test public void sharedWorldCloneRemapsSceneAndPickingHandles() {
        SchemaRegistry schemas = new SchemaRegistry(); FactorySceneAdapter.registerSchemas(schemas);
        FactoryWorld world = new FactoryWorld(schemas); FactorySceneAdapter adapter = new FactorySceneAdapter();
        Scene original = scene(); adapter.capture(original, world);
        UUID id = world.snapshot().entities().get(0).id(); var ids = world.duplicate(Set.of(id));
        Scene restored = new Scene(null); adapter.restore(world, restored);
        assertEquals(2, restored.getGameObjects().size());
        assertNotEquals(restored.getGameObjects().get(0).getUid(), restored.getGameObjects().get(1).getUid());
        assertTrue(restored.getGameObjects().stream().anyMatch(o -> o.getPersistentId().equals(ids.get(id))));
        assertNotEquals(restored.getGameObjects().get(0).transform.getPersistentId(), restored.getGameObjects().get(1).transform.getPersistentId());
    }

    @Test public void typedWorldTransformEditsReachRestoredScene() {
        SchemaRegistry schemas = new SchemaRegistry(); FactorySceneAdapter.registerSchemas(schemas);
        FactoryWorld world = new FactoryWorld(schemas); FactorySceneAdapter adapter = new FactorySceneAdapter(); adapter.capture(scene(), world);
        EntityRecord old = world.snapshot().entities().get(0); var components = new ArrayList<ComponentRecord>();
        for (ComponentRecord component : old.components()) {
            var values = new HashMap<>(component.values()); if (component.type().equals("editor.jade.Transform")) values.put("x", "123.5");
            components.add(new ComponentRecord(component.id(), component.type(), component.version(), values));
        }
        world.replace(world.snapshot().revision(), List.of(new EntityRecord(old.id(), old.name(), components)));
        Scene restored = new Scene(null); adapter.restore(world, restored);
        assertEquals(123.5f, restored.getGameObjects().get(0).transform.position.x, 0.0001f);
    }

    @Test public void legacySceneSaveIsAnIdentityPreservingRoundtrip() throws Exception {
        Scene scene = scene(); Path file = Files.createTempDirectory("factory-scene-json-").resolve("level.json"); scene.save(file);
        Scene restored = new Scene(null); restored.load(file);
        assertEquals(scene.getGameObjects().get(0).getPersistentId(), restored.getGameObjects().get(0).getPersistentId());
        assertEquals(scene.getGameObjects().get(0).getUid(), restored.getGameObjects().get(0).getUid());
    }

    @Test public void legacyDuplicateEditorHandlesAreMigratedWithoutChangingPersistentIds() throws Exception {
        var first = scene().getGameObjects().get(0); var second = scene().getGameObjects().get(0);
        JsonArray array = new JsonArray(); array.add(gson().toJsonTree(first)); array.add(gson().toJsonTree(second));
        array.get(1).getAsJsonObject().addProperty("uid", first.getUid());
        array.get(1).getAsJsonObject().getAsJsonArray("components").get(0).getAsJsonObject().getAsJsonObject("properties").addProperty("uid", first.transform.getUid());
        Path path = Files.createTempFile("legacy-duplicate-handles-", ".json"); Files.writeString(path, array.toString());
        Scene loaded = new Scene(null); loaded.load(path);
        var objects = loaded.getGameObjects();
        assertNotEquals(objects.get(0).getUid(), objects.get(1).getUid());
        assertNotEquals(objects.get(0).transform.getUid(), objects.get(1).transform.getUid());
        assertEquals(first.getPersistentId(), objects.get(0).getPersistentId());
        assertEquals(second.transform.getPersistentId(), objects.get(1).transform.getPersistentId());
        var schemas = new SchemaRegistry(); FactorySceneAdapter.registerSchemas(schemas);
        assertTrue(new FactorySceneAdapter().verifyRoundTrip(loaded, schemas).contains("2 objects"));
    }

    @Test public void unregisteredClassesCannotBeLoadedAsComponents() {
        try { gson().fromJson("{\"type\":\"java.lang.ProcessBuilder\",\"properties\":{}}", Component.class); fail(); }
        catch (JsonParseException expected) { assertTrue(expected.getMessage().contains("Unregistered")); }
    }

    static class CountingWorld extends World {
        int steps; CountingWorld() { super(new Vec2()); }
        @Override public void step(float dt, int v, int p) { steps++; }
    }
    private CountingWorld counter(physics2d.Physics2D physics) throws Exception {
        CountingWorld world = new CountingWorld(); Field field = physics2d.Physics2D.class.getDeclaredField("world"); field.setAccessible(true); field.set(physics, world); return world;
    }
    @Test public void fixedStepConsumesElapsedTimeAndDoesNotAdvanceAtZero() throws Exception {
        var physics = new physics2d.Physics2D(); var world = counter(physics);
        physics.update(0); assertEquals(0, world.steps); physics.update(1); assertEquals(60, world.steps);
        var split = new physics2d.Physics2D(); var splitWorld = counter(split);
        for (int i = 0; i < 60; i++) split.update(1f / 60f); assertEquals(world.steps, splitWorld.steps);
    }

    @Test public void circleBodyUsesCircleFixture() {
        GameObject ball = scene().getGameObjects().get(0);
        var body = new physics2d.components.Rigidbody2D(); body.setMass(1);
        ball.addComponent(body); ball.addComponent(new physics2d.components.CircleCollider());
        new physics2d.Physics2D().add(ball);
        assertEquals(org.jbox2d.collision.shapes.ShapeType.CIRCLE, body.getRawBody().getFixtureList().getType());
    }

    @Test public void negativeAndNonFiniteElapsedTimesAreRejected() {
        for (float value : new float[]{-1, Float.NaN, Float.POSITIVE_INFINITY}) {
            try { new physics2d.Physics2D().update(value); fail(); } catch (IllegalArgumentException expected) { }
        }
    }

    @Test public void sceneAndActualKernelTaskSurviveSessionRestart() throws Exception {
        Path project = Files.createTempDirectory("factory-editor-integration-"); Scene original = scene();
        UUID id = original.getGameObjects().get(0).getPersistentId();
        try (FactorySession session = new FactorySession(project)) {
            session.inspect(original); long deadline = System.nanoTime() + 30_000_000_000L;
            while (session.isRunning() && System.nanoTime() < deadline) { Thread.sleep(20); session.poll(); }
            assertFalse("Inspection timed out", session.isRunning()); assertTrue(session.status(), session.status().contains("validated"));
        }
        try (FactorySession restarted = new FactorySession(project)) {
            Scene restored = restarted.restore(); assertEquals(id, restored.getGameObjects().get(0).getPersistentId());
            assertTrue(restarted.world().snapshot().entities().stream().flatMap(e -> e.components().stream()).anyMatch(c -> c.type().equals("factory.task") && c.values().get("status").equals("validated")));
        }
    }

    @Test public void checkpointDoesNotPretendAnInterruptedWorkerIsStillRunning() throws Exception {
        Path project = Files.createTempDirectory("factory-interrupted-");
        SchemaRegistry schemas = new SchemaRegistry(); FactorySceneAdapter.registerSchemas(schemas);
        schemas.register(new ComponentSchema("factory.task", 1, Map.of("status", ComponentSchema.Kind.TEXT, "trace", ComponentSchema.Kind.TEXT), Set.of("status", "trace")));
        FactoryWorld world = new FactoryWorld(schemas); new FactorySceneAdapter().capture(scene(), world);
        var entities = new ArrayList<>(world.snapshot().entities());
        entities.add(new EntityRecord(UUID.randomUUID(), "interrupted task", List.of(new ComponentRecord(UUID.randomUUID(), "factory.task", 1, Map.of("status", "running", "trace", "")))));
        world.replace(world.snapshot().revision(), entities);
        new WorldCheckpointStore(project.resolve("scene.fworld")).save(world.snapshot(), schemas);
        try (FactorySession restarted = new FactorySession(project)) {
            assertTrue(restarted.hasCheckpoint()); restarted.restore(); assertTrue(restarted.status().contains("interrupted"));
            assertFalse(restarted.world().snapshot().entities().stream().flatMap(e -> e.components().stream()).anyMatch(c -> c.type().equals("factory.task") && c.values().get("status").equals("running")));
        }
    }
}
