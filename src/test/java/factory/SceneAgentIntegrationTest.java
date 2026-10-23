package factory;

import com.google.gson.*;
import components.*;
import editor.SceneAgent;
import editor.PhysicsComponents;
import fraymus.factory.*;
import fraymus.factory.models.FactoryModelService;
import jade.*;
import org.junit.Test;
import physics2d.components.*;
import scenes.Scene;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;

public class SceneAgentIntegrationTest {
    private Scene scene() {
        var scene = new Scene(null); var object = scene.createGameObject("inspected tile"); object.transform.scale.set(.25f, .25f); scene.addGameObjectToScene(object); return scene;
    }
    private String proposal(SceneKnowledge observation, String operations) {
        return "{\"kind\":\"proposal\",\"observationId\":\"" + observation.fingerprint().substring(0,16) + "\",\"summary\":\"Test scene changes\",\"operations\":" + operations + "}";
    }
    private SceneProposal.Draft stage(FactorySession session, SceneKnowledge observation, String operations) {
        return SceneProposal.stage(proposal(observation, operations), observation, session.schemas(), index -> new Sprite());
    }
    @Test public void observationIncludesCompleteComponentsRuntimeAndSelection() throws Exception {
        var scene = scene(); var tile = scene.getGameObjects().get(0); PhysicsComponents.rigidbody(tile); PhysicsComponents.box(tile);
        tile.getComponent(Rigidbody2D.class).setMass(3.75f); tile.getComponent(Rigidbody2D.class).setFixedRotation(true);
        tile.getComponent(Box2DCollider.class).getOffset().set(.1f, .2f); scene.physics().synchronize(tile);
        try (var session = new FactorySession(Files.createTempDirectory("observe-components-"))) {
            var observation = session.observe(scene, tile.getPersistentId(), false); var json = SceneJson.parse(observation.json());
            assertEquals(tile.getPersistentId().toString(), json.get("selectedEntityId").getAsString());
            assertEquals(-10, json.getAsJsonObject("physicsSettings").get("gravityY").getAsDouble(), 0);
            var entity = json.getAsJsonArray("entities").get(0).getAsJsonObject();
            var components = entity.getAsJsonArray("components"); assertEquals(3, components.size());
            var rigid = components.get(1).getAsJsonObject().getAsJsonObject("properties");
            assertEquals(3.75, rigid.get("mass").getAsDouble(), 0); assertTrue(rigid.get("fixedRotation").getAsBoolean());
            assertEquals(.2, components.get(2).getAsJsonObject().getAsJsonObject("properties").getAsJsonObject("offset").get("y").getAsDouble(), .0001);
            assertTrue(entity.has("runtimePhysics")); assertFalse(rigid.has("rawBody"));
            assertEquals(1, SceneJson.parse(observation.query("{\"op\":\"components\",\"type\":\"physics2d.components.Rigidbody2D\"}")).getAsJsonArray("entities").size());
        }
    }
    @Test public void rotatedColliderBoundsAndSpatialQueriesMatchActualLocalOffsets() throws Exception {
        var scene = scene(); var tile = scene.getGameObjects().get(0); tile.transform.position.set(2,3); tile.transform.rotation = 90;
        PhysicsComponents.box(tile); var box = tile.getComponent(Box2DCollider.class); box.getHalfSize().set(2,4); box.getOrigin().set(.5f,-.5f); box.getOffset().set(1,0);
        try (var session = new FactorySession(Files.createTempDirectory("spatial-observation-"))) {
            var observation = session.observe(scene, null, false); var bounds = observation.spatial().items().get(0).bounds();
            assertEquals(.5, bounds.minX(), .00001); assertEquals(4.5, bounds.maxX(), .00001); assertEquals(3.5, bounds.minY(), .00001); assertEquals(5.5, bounds.maxY(), .00001);
            assertEquals(1, SceneJson.parse(observation.query("{\"op\":\"overlap\",\"minX\":0,\"minY\":3,\"maxX\":1,\"maxY\":4}")).getAsJsonArray("hits").size());
            assertEquals(0, SceneJson.parse(observation.query("{\"op\":\"nearest\",\"x\":20,\"y\":20,\"radius\":1}")).getAsJsonArray("hits").size());
        }
    }
    @Test public void stagedCreationSimulatesRealCollisionWithoutChangingLiveScene() throws Exception {
        var scene = new Scene(null);
        try (var session = new FactorySession(Files.createTempDirectory("scene-proposal-physics-"))) {
            var observation = session.observe(scene, null, false);
            var draft = stage(session, observation, "["
                + "{\"op\":\"create\",\"key\":\"floor\",\"name\":\"Floor\",\"x\":0,\"y\":-.25,\"width\":4,\"height\":.5},".replace("-.25", "-0.25").replace(":.5", ":0.5")
                + "{\"op\":\"add_component\",\"entityId\":\"$floor\",\"component\":\"physics2d.components.Rigidbody2D\"},"
                + "{\"op\":\"add_component\",\"entityId\":\"$floor\",\"component\":\"physics2d.components.Box2DCollider\"},"
                + "{\"op\":\"set\",\"entityId\":\"$floor\",\"component\":\"physics2d.components.Rigidbody2D\",\"values\":{\"bodyType\":\"Static\"}},"
                + "{\"op\":\"create\",\"key\":\"tile\",\"name\":\"Falling tile\",\"x\":0,\"y\":2,\"width\":0.25,\"height\":0.25},"
                + "{\"op\":\"add_component\",\"entityId\":\"$tile\",\"component\":\"physics2d.components.Rigidbody2D\"},"
                + "{\"op\":\"add_component\",\"entityId\":\"$tile\",\"component\":\"physics2d.components.Box2DCollider\"}]");
            var simulated = SceneSimulation.validate(draft, session.schemas(), 2);
            assertEquals(2, simulated.bodies()); assertTrue(simulated.peakContacts() > 0); assertTrue(scene.getGameObjects().isEmpty());
            UUID tile = draft.candidate().entities().stream().filter(e -> e.name().equals("Falling tile")).findFirst().orElseThrow().id();
            var path = simulated.trajectories().get(tile); double y = path.get(path.size()-1).y(); assertTrue("did not land: " + y, y > .1 && y < .2);
            assertTrue(session.world().snapshot().entities().isEmpty());
        }
    }
    @Test public void invalidOrExecutablePropertiesRejectTheWholeDraft() throws Exception {
        var scene = scene(); var id = scene.getGameObjects().get(0).getPersistentId();
        try (var session = new FactorySession(Files.createTempDirectory("reject-scene-proposals-"))) {
            var observation = session.observe(scene, null, false); var before = session.world().snapshot();
            for (String bad : List.of(
                "{\"op\":\"set\",\"entityId\":\""+id+"\",\"component\":\"jade.Transform\",\"values\":{\"uid\":123}}",
                "{\"op\":\"add_component\",\"entityId\":\""+id+"\",\"component\":\"java.lang.ProcessBuilder\"}",
                "{\"op\":\"create\",\"key\":\"bad\",\"name\":\"bad\",\"x\":0,\"y\":0,\"width\":-1,\"height\":1}")) {
                try { stage(session, observation, "[{\"op\":\"rename\",\"entityId\":\""+id+"\",\"name\":\"must not leak\"},"+bad+"]"); fail(); }
                catch (IllegalArgumentException expected) { }
                assertEquals(before, session.world().snapshot()); assertEquals("inspected tile", scene.getGameObjects().get(0).getName());
            }
            for (String invalid : List.of("{\"kind\":\"proposal\",\"kind\":\"answer\"}", "{\"x\":NaN}", "{\"x\":1} trailing")) {
                try { SceneJson.parse(invalid); fail(invalid); } catch (IllegalArgumentException expected) { }
            }
        }
    }
    @Test public void applicationIsGuardedAndUndoSurvivesRestart() throws Exception {
        Path root = Files.createTempDirectory("scene-agent-undo-"); var scene = scene(); UUID originalId = scene.getGameObjects().get(0).getPersistentId();
        UUID backgroundId = UUID.randomUUID();
        AtomicReference<Scene> installed = new AtomicReference<>(); String beforeHash;
        try (var session = new FactorySession(root)) {
            var observation = session.observe(scene, null, false); beforeHash = observation.fingerprint();
            var draft = stage(session, observation, "[{\"op\":\"duplicate\",\"entityId\":\""+originalId+"\",\"key\":\"copy\"},{\"op\":\"set\",\"entityId\":\"$copy\",\"component\":\"jade.Transform\",\"values\":{\"position.x\":3}}]");
            var history = new SceneEditHistory(session); var prepared = history.prepare(draft, SceneSimulation.validate(draft, session.schemas(), .25));
            scene.getGameObjects().get(0).transform.position.y = 1;
            try { history.apply(prepared, scene, installed::set); fail("stale edit applied"); } catch (ConcurrentModificationException expected) { }
            assertNull(installed.get()); scene.getGameObjects().get(0).transform.position.y = 0;
            var updated = new ArrayList<>(session.world().snapshot().entities());
            updated.add(new EntityRecord(backgroundId, "Background data", List.of(new ComponentRecord(UUID.randomUUID(), "factory.task", 1, Map.of("status", "ready", "trace", "")))));
            session.world().replace(session.world().snapshot().revision(), updated);
            history.apply(prepared, scene, installed::set); assertEquals(2, installed.get().getGameObjects().size());
            assertTrue(session.world().snapshot().entities().stream().anyMatch(e -> e.id().equals(backgroundId)));
            assertEquals(2, installed.get().getGameObjects().stream().map(GameObject::getPersistentId).distinct().count());
            assertEquals(2, installed.get().getGameObjects().stream().map(o -> o.transform.getUid()).distinct().count());
        }
        try (var restarted = new FactorySession(root)) {
            Scene loaded = restarted.restore(); var history = new SceneEditHistory(restarted); restarted.capture(loaded);
            assertTrue(history.canUndo(SceneKnowledge.fingerprint(restarted.world().snapshot())));
            loaded.getGameObjects().get(0).transform.position.y = 1;
            try { history.undo(loaded, installed::set); fail("Undo overwrote a later edit"); } catch (ConcurrentModificationException expected) { }
            assertEquals(1, loaded.getGameObjects().get(0).transform.position.y, 0);
            loaded.getGameObjects().get(0).transform.position.y = 0;
            history.undo(loaded, installed::set); assertEquals(1, installed.get().getGameObjects().size());
            assertEquals(originalId, installed.get().getGameObjects().get(0).getPersistentId());
            assertEquals(beforeHash, SceneKnowledge.fingerprint(restarted.world().snapshot())); assertEquals(0, history.size());
            assertTrue(restarted.world().snapshot().entities().stream().anyMatch(e -> e.id().equals(backgroundId)));
        }
    }
    @Test public void failedSceneInstallationRestoresTheCheckpointAndKeepsOriginalWorld() throws Exception {
        Path root = Files.createTempDirectory("scene-agent-install-failure-"); var scene = scene();
        try (var session = new FactorySession(root)) {
            session.save(scene); var observation = session.observe(scene, null, false); UUID id = scene.getGameObjects().get(0).getPersistentId();
            var draft = stage(session, observation, "[{\"op\":\"rename\",\"entityId\":\""+id+"\",\"name\":\"changed\"}]");
            var history = new SceneEditHistory(session); var prepared = history.prepare(draft, SceneSimulation.validate(draft, session.schemas(), .25));
            try { history.apply(prepared, scene, replacement -> { throw new IllegalStateException("renderer refused"); }); fail(); } catch (IllegalStateException expected) { }
            assertEquals(observation.fingerprint(), SceneKnowledge.fingerprint(session.world().snapshot())); assertEquals(0, history.size());
            assertEquals("inspected tile", session.restore().getGameObjects().get(0).getName());
        }
    }
    @Test public void modelQueriesFeedBackIntoARealAdapterProposalAndDetachedPreview() throws Exception {
        var server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        String previous = System.getProperty("fraynix.localModelCatalog"); Path root = Files.createTempDirectory("scene-agent-model-");
        var scene = scene(); String endpoint = "http://127.0.0.1:" + server.getAddress().getPort(); var calls = new AtomicInteger(); var received = new java.util.concurrent.CopyOnWriteArrayList<String>();
        try (var session = new FactorySession(root); var agent = new SceneAgent(session, i -> new Sprite())) {
            var observation = session.observe(scene, scene.getGameObjects().get(0).getPersistentId(), false);
            String source = proposal(observation, "[{\"op\":\"create\",\"key\":\"newTile\",\"name\":\"Model tile\",\"x\":2,\"y\":1,\"width\":0.25,\"height\":0.25}]");
            server.createContext("/api/chat", exchange -> {
                String body = new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8); received.add(body);
                int call = calls.incrementAndGet(); String content = call == 1 ? "{\"kind\":\"query\",\"queries\":[{\"op\":\"inspect\",\"entityId\":\""+scene.getGameObjects().get(0).getPersistentId()+"\"}]}"
                    : call == 2 ? source.replace("\"width\":0.25", "\"width\":-0.25") : source;
                var message = new JsonObject(); message.addProperty("content", content); var response = new JsonObject(); response.add("message", message);
                byte[] bytes = response.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8); exchange.sendResponseHeaders(200, bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
            }); server.start();
            Path catalog = root.resolve("models.json"); Files.writeString(catalog, "{\"models\":[{\"name\":\"scene-test:latest\",\"status\":\"LOCAL_INSTALLED\",\"endpoint\":\""+endpoint+"\"}]}"); System.setProperty("fraynix.localModelCatalog", catalog.toString());
            var model = new FactoryModelService.Model("scene-test:latest", endpoint, "test", "3B", "Q4", 1, 0, 8192, true, false, "fixture");
            agent.request("Create a tile", observation, model, 8192, 3, .25);
            long deadline = System.nanoTime()+15_000_000_000L;
            while (agent.busy() && System.nanoTime()<deadline) { agent.tick(); Thread.sleep(10); }
            agent.tick(); assertFalse("Agent timed out", agent.busy()); assertEquals(agent.error(), "", agent.error()); assertNotNull(agent.prepared());
            assertEquals(3, calls.get()); assertTrue(received.get(1).contains("Engine query results")); assertTrue(received.get(2).contains("engine rejected"));
            assertEquals("json", SceneJson.parse(received.get(0)).get("format").getAsString());
            assertEquals(1, scene.getGameObjects().size()); assertEquals(2, agent.prepared().draft().candidate().entities().size());
        } finally { server.stop(0); if (previous == null) System.clearProperty("fraynix.localModelCatalog"); else System.setProperty("fraynix.localModelCatalog", previous); }
    }
    @Test public void backgroundPreviewIdentityAllocationIsUniqueAcrossThreads() {
        Set<Integer> entities = java.util.concurrent.ConcurrentHashMap.newKeySet(), components = java.util.concurrent.ConcurrentHashMap.newKeySet();
        java.util.stream.IntStream.range(0, 4000).parallel().forEach(i -> {
            var object = new GameObject("parallel"); var transform = new Transform(); object.addComponent(transform);
            if (!entities.add(object.getUid()) || !components.add(transform.getUid())) throw new AssertionError("Duplicate editor handle");
        });
        assertEquals(4000, entities.size()); assertEquals(4000, components.size());
    }
}
