package fraymus.workspace;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.UUID;

public class SmallObjectRoutesTest {
    @Test(timeout = 180000)
    public void parentOwnedActionsCannotBeControlledThroughChildAliases() throws Exception {
        try (var seed = new Alignment041Test.Fixture();
             var fixture = new SkillGraph046Test.F(seed.root);
             var small = new SmallStore161(seed.root, fixture.artifacts)) {
            var trial = PlaneAccuracy160Test.movement(seed);
            var service = new RecordedSmall162(seed.root, seed.store, fixture.goals.worlds(), small);
            fixture.workflows.install162(service);
            var routes = new SmallObjectRoutes(small, new SpatialStore(seed.root), fixture.workflows);
            var world = small.create(SmallBody161Test.scene(.05, .04));
            var input = RecordedSmall162Test.input(trial, world);
            var preview = routes.dispatch("/api/recorded162/preview", input);
            var parent = WorldFrames.copy(preview).put("id", UUID.randomUUID().toString())
                    .put("code162", RecordedSmall162.code());
            var child = service.start(parent);
            assertEquals(child.getString("id"), service.start(parent).getString("id"));
            child = SmallBody161Test.waitFor(small, child.getString("id"));
            String childId = child.getString("id");
            for (String command : new String[] {"resume", "stop", "cancel", "acknowledge"}) {
                var error = assertThrows(IllegalStateException.class, () -> routes.dispatch(
                        "/api/small161/" + command, new JSONObject().put("id", childId)));
                assertTrue(error.getMessage().contains("use its parent controls"));
            }
            assertEquals(1, small.list().getJSONArray("jobs").length());
            assertEquals(0, small.world(world.getString("id")).getInt("revision"));
            service.accept(parent, SmallBody161Test.observed(child));
            service.accept(parent, SmallBody161Test.observed(child));
            assertEquals(1, small.world(world.getString("id")).getInt("revision"));
            assertTrue(routes.dispatch("/api/small161/verify", new JSONObject().put("id", childId))
                    .getBoolean("passed"));
        }
    }

    @Test(timeout = 180000)
    public void routesPreserveOriginalValidationAndDoNotAdmitStaleActions() throws Exception {
        try (var seed = new Alignment041Test.Fixture();
             var fixture = new SkillGraph046Test.F(seed.root);
             var small = new SmallStore161(seed.root, fixture.artifacts)) {
            fixture.workflows.install162(new RecordedSmall162(seed.root, seed.store, fixture.goals.worlds(), small));
            var routes = new SmallObjectRoutes(small, new SpatialStore(seed.root), fixture.workflows);
            assertEquals(18, routes.routes().size());
            assertTrue(routes.handles("/api/small161/not-registered"));
            assertFalse(routes.handles("/api/small161x/state"));
            assertFalse(routes.handles("/api/cost164/not-registered"));
            assertEquals("Unknown small-body operation", assertThrows(IllegalArgumentException.class,
                    () -> routes.dispatch("/api/small161/not-registered", new JSONObject())).getMessage());
            assertThrows(IllegalArgumentException.class, () -> routes.dispatch(
                    "/api/recorded163/status", new JSONObject().put("extra", true)));
            var world = routes.dispatch("/api/small161/example", new JSONObject());
            var request = SmallBody161Test.request(world, .1, .06);
            assertThrows(Exception.class, () -> routes.dispatch("/api/small161/start",
                    WorldFrames.copy(request).put("expectedRevision", 999)));
            assertThrows(Exception.class, () -> routes.dispatch("/api/small161/preview",
                    WorldFrames.copy(request).put("hardware", true)));
            assertEquals(0, small.list().getJSONArray("jobs").length());
            // Legacy read routes accepted unrelated fields; extraction must not silently tighten them.
            assertEquals(world.getString("id"), routes.dispatch("/api/small161/world",
                    new JSONObject().put("id", world.getString("id")).put("extra", true)).getString("id"));
        }
    }
}
