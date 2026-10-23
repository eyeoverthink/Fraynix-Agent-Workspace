package factory;

import fraymus.factory.*;
import scenes.Scene;
import physics2d.Physics2D;
import physics2d.components.Rigidbody2D;
import java.util.*;

/** Real JBox2D preview on a detached scene; no renderer or live GameObject is touched. */
public final class SceneSimulation {
    public record Position(double x, double y, double rotation) {}
    public record Result(double seconds, int steps, int bodies, int peakContacts, Map<UUID, List<Position>> trajectories,
                         Map<UUID, SpatialIndex2D.Bounds> initialBounds, Map<UUID, SpatialIndex2D.Bounds> finalBounds) {}
    private SceneSimulation() {}
    public static Result validate(SceneProposal.Draft draft, SchemaRegistry schemas, double seconds) {
        if (!Double.isFinite(seconds) || seconds <= 0 || seconds > 60) throw new IllegalArgumentException("Preview duration must be between zero and 60 seconds");
        var world = new FactoryWorld(schemas); world.restore(draft.candidate()); var scene = new Scene(null); var adapter = new FactorySceneAdapter(); adapter.restore(world, scene);
        adapter.verifyRoundTrip(scene, schemas);
        var physics = scene.physics(); var trajectories = new LinkedHashMap<UUID, List<Position>>(); var initial = new LinkedHashMap<UUID, SpatialIndex2D.Bounds>();
        for (var entity : draft.candidate().entities()) initial.put(entity.id(), SceneKnowledge.bounds(entity));
        int expected = 0;
        for (var object : scene.getGameObjects()) {
            if (object.getComponent(Rigidbody2D.class) != null && (object.getComponent(physics2d.components.Box2DCollider.class) != null
                || object.getComponent(physics2d.components.CircleCollider.class) != null)) expected++;
            physics.synchronize(object);
            if (draft.affected().contains(object.getPersistentId())) trajectories.put(object.getPersistentId(), new ArrayList<>());
        }
        if (physics.bodyCount() != expected || !physics.editorError().isBlank()) throw new IllegalArgumentException("Physics registration failed: " + physics.editorError());
        int steps = (int) Math.ceil(seconds * 60), peak = 0;
        for (int step = 0; step <= steps; step++) {
            if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException("Simulation stopped");
            if (step > 0) physics.update(1f / 60f);
            peak = Math.max(peak, physics.contactCount());
            for (var object : scene.getGameObjects()) {
                var body = object.getComponent(Rigidbody2D.class); if (body != null) body.update(0);
                Physics2D.validate(object);
                if (body != null && body.getRawBody() != null && (!Float.isFinite(body.getRawBody().getLinearVelocity().x)
                    || !Float.isFinite(body.getRawBody().getLinearVelocity().y) || !Float.isFinite(body.getRawBody().getAngularVelocity()))) throw new IllegalArgumentException("Simulation produced non-finite motion");
                if ((step % 6 == 0 || step == steps) && trajectories.containsKey(object.getPersistentId())) {
                    trajectories.get(object.getPersistentId()).add(new Position(object.transform.position.x, object.transform.position.y, object.transform.rotation));
                }
            }
        }
        var simulated = new FactoryWorld(schemas); adapter.capture(scene, simulated); var last = new LinkedHashMap<UUID, SpatialIndex2D.Bounds>();
        for (var entity : simulated.snapshot().entities()) last.put(entity.id(), SceneKnowledge.bounds(entity));
        var paths = new LinkedHashMap<UUID, List<Position>>(); trajectories.forEach((id, list) -> paths.put(id, List.copyOf(list)));
        return new Result(steps / 60.0, steps, physics.bodyCount(), peak, Map.copyOf(paths), Map.copyOf(initial), Map.copyOf(last));
    }
}
