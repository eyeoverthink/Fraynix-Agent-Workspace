package factory;

import editor.PhysicsComponents;
import fraymus.factory.*;
import jade.GameObject;
import org.jbox2d.collision.shapes.PolygonShape;
import org.joml.Vector2f;
import org.junit.Test;
import physics2d.Physics2D;
import physics2d.components.*;
import physics2d.enums.BodyType;
import scenes.Scene;
import java.nio.file.Files;
import java.util.*;
import static org.junit.Assert.*;

public class EditorPhysicsRegressionTest {
    private GameObject object(Scene scene, String name) {
        GameObject object = scene.createGameObject(name); object.transform.scale.set(.25f, .25f); scene.addGameObjectToScene(object); return object;
    }
    @Test public void visibleEditorCommandsAreIdempotentAndRegisterLateComponents() {
        GameObject object = object(new Scene(null), "tile"); Physics2D physics = new Physics2D();
        PhysicsComponents.rigidbody(object); physics.synchronize(object); assertEquals(0, physics.bodyCount());
        for (int i=0; i<8; i++) { PhysicsComponents.rigidbody(object); PhysicsComponents.box(object); physics.synchronize(object); }
        assertEquals(3, object.getAllComponents().size()); assertEquals(1, physics.bodyCount());
        assertNotNull(object.getComponent(Rigidbody2D.class).getRawBody());
        assertEquals(.25f, object.getComponent(Box2DCollider.class).getHalfSize().x, .00001f);
        PhysicsComponents.circle(object); assertNull(object.getComponent(CircleCollider.class));
    }
    @Test public void editorChangesRefreshFixtureAndDoNotShiftObjectByColliderOffset() {
        GameObject object = object(new Scene(null), "body"); object.transform.position.set(2, 3);
        PhysicsComponents.rigidbody(object); PhysicsComponents.box(object); Physics2D physics = new Physics2D(); physics.synchronize(object);
        Rigidbody2D rb = object.getComponent(Rigidbody2D.class); Box2DCollider box = object.getComponent(Box2DCollider.class);
        box.setHalfSize(new Vector2f(.5f, .75f)); box.getOffset().set(.2f, .3f);
        rb.setVelocity(new Vector2f(2, 0)); rb.setFixedRotation(true); physics.synchronize(object);
        assertEquals(1, physics.bodyCount()); assertTrue(rb.getRawBody().isFixedRotation());
        assertEquals(2, rb.getRawBody().getPosition().x, .00001f);
        assertEquals(3, rb.getRawBody().getPosition().y, .00001f);
        assertEquals(2, rb.getRawBody().getLinearVelocity().x, .00001f);
        PolygonShape shape = (PolygonShape)rb.getRawBody().getFixtureList().getShape();
        assertEquals(.2f, shape.m_centroid.x, .00001f); assertEquals(.3f, shape.m_centroid.y, .00001f);
        object.removeComponent(Rigidbody2D.class); physics.synchronize(object); assertEquals(0, physics.bodyCount());
    }
    @Test public void dynamicBoxFallsAndLandsOnStaticBox() {
        Scene scene = new Scene(null); Physics2D physics = new Physics2D();
        GameObject floor = object(scene, "floor"); floor.transform.position.set(0, -1); PhysicsComponents.rigidbody(floor); PhysicsComponents.box(floor);
        floor.getComponent(Rigidbody2D.class).setBodyType(BodyType.Static); floor.getComponent(Box2DCollider.class).setHalfSize(new Vector2f(4, .5f));
        GameObject tile = object(scene, "tile"); tile.transform.position.set(0, 1); PhysicsComponents.rigidbody(tile); PhysicsComponents.box(tile);
        physics.synchronize(floor); physics.synchronize(tile); physics.update(3);
        Rigidbody2D body = tile.getComponent(Rigidbody2D.class); float y = body.getRawBody().getPosition().y;
        assertTrue("Body did not fall", y < .5f); assertTrue("Body fell through floor: " + y, y > -.66f);
        body.update(0); assertEquals(y, tile.transform.position.y, .00001f);
    }
    @Test public void physicsValuesAndIdentitySurviveActualCheckpointDeserialization() throws Exception {
        Scene scene = new Scene(null); GameObject object = object(scene, "physics tile"); PhysicsComponents.rigidbody(object); PhysicsComponents.box(object);
        Rigidbody2D rb = object.getComponent(Rigidbody2D.class); rb.setMass(3.75f); rb.setFixedRotation(true); rb.setVelocity(new Vector2f(1.25f, -2));
        Box2DCollider box = object.getComponent(Box2DCollider.class); box.setHalfSize(new Vector2f(.5f, .75f)); box.getOffset().set(.1f, .2f);
        var root = Files.createTempDirectory("physics-roundtrip-");
        try (FactorySession first = new FactorySession(root)) { first.save(scene); }
        try (FactorySession second = new FactorySession(root)) {
            Scene restored = second.restore(); GameObject tile = restored.getGameObjects().get(0);
            assertEquals(3, tile.getAllComponents().size()); assertEquals(object.getPersistentId(), tile.getPersistentId());
            Rigidbody2D actual = tile.getComponent(Rigidbody2D.class); assertEquals(rb.getPersistentId(), actual.getPersistentId());
            assertEquals(3.75f, actual.getMass(), .00001f); assertTrue(actual.isFixedRotation()); assertEquals(rb.getVelocity(), actual.getVelocity());
            assertEquals(box.getOffset(), tile.getComponent(Box2DCollider.class).getOffset());
            restored.physics().synchronize(tile); assertEquals(1, restored.physics().bodyCount());
        }
    }
    @Test public void repeatedInspectionKeepsOneStatusAndDoesNotAddSceneComponents() throws Exception {
        Scene scene = new Scene(null); GameObject object = object(scene, "tile"); PhysicsComponents.rigidbody(object); PhysicsComponents.box(object);
        try (FactorySession session = new FactorySession(Files.createTempDirectory("inspection-repeat-"))) {
            for (int i=0; i<2; i++) {
                // Inspection starts verifier JVMs and persists kernel state. Shared CI disks
                // can exceed 30 seconds; this is an idempotence test, not a latency benchmark.
                session.inspect(scene); long deadline = System.nanoTime() + 120_000_000_000L;
                while (session.isRunning() && System.nanoTime() < deadline) { Thread.sleep(20); session.poll(); }
                assertFalse("Inspection exceeded 120 seconds: " + session.status(), session.isRunning());
                assertTrue(session.status(), session.status().contains("Round-trip verified"));
            }
            assertEquals(1, scene.getGameObjects().size()); assertEquals(3, object.getAllComponents().size());
            assertEquals(1, session.world().snapshot().entities().stream().filter(e -> e.name().equals("Fraynix scene inspection")).count());
        }
    }
}
