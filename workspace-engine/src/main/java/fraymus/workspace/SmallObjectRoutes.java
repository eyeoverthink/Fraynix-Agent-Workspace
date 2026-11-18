package fraymus.workspace;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.json.JSONObject;

/** Recorded observations and small-world routes behind the server's shared admission boundary. */
final class SmallObjectRoutes {
    @FunctionalInterface
    private interface Operation {
        JSONObject run(JSONObject input) throws Exception;
    }

    private static final Set<String> OWNED_CONTROLS = Set.of(
            "/api/small161/resume", "/api/small161/stop",
            "/api/small161/cancel", "/api/small161/acknowledge");

    private final SmallStore161 small;
    private final Map<String, Operation> operations;

    SmallObjectRoutes(SmallStore161 small, SpatialStore spatial, WorkflowRunner workflows) {
        this.small = small;
        var routes = new LinkedHashMap<String, Operation>();
        routes.put("/api/cost164/status", input -> {
            SpatialScene.keys(input);
            return workflows.workflowCosts164();
        });
        routes.put("/api/cost164/history", workflows::historyCosts164);
        routes.put("/api/recorded163/status", input -> {
            SpatialScene.keys(input);
            return workflows.recordedCosts163();
        });
        routes.put("/api/recorded163/preview-full", workflows::previewFull163);
        routes.put("/api/recorded162/preview", workflows::preview162);
        routes.put("/api/small161/state", input -> small.list().put("imports", spatial.list()));
        routes.put("/api/small161/example", input -> {
            try (var source = SmallObjectRoutes.class.getResourceAsStream("/small-body161.json")) {
                if (source == null) throw new IllegalStateException("Small-world example resource missing");
                return small.create(spatial.importScene(new String(source.readAllBytes(), StandardCharsets.UTF_8)));
            }
        });
        routes.put("/api/small161/create", input -> small.create(spatial.read(input.getString("sceneId"))));
        routes.put("/api/small161/world", input -> small.world(input.getString("id")));
        routes.put("/api/small161/read", input -> small.read(input.getString("id")));
        routes.put("/api/small161/preview", small::preview);
        routes.put("/api/small161/start", small::start);
        for (String command : new String[] {"resume", "stop", "cancel"}) {
            routes.put("/api/small161/" + command, input -> small.control(input.getString("id"), command));
        }
        routes.put("/api/small161/prepare", input -> small.prepare(input.getString("id")));
        routes.put("/api/small161/acknowledge", small::acknowledge);
        routes.put("/api/small161/verify", input -> small.verify(input.getString("id")));
        operations = Map.copyOf(routes);
    }

    boolean handles(String route) {
        return operations.containsKey(route) || route.startsWith("/api/small161/");
    }

    JSONObject dispatch(String route, JSONObject input) throws Exception {
        if (input.has("id") && OWNED_CONTROLS.contains(route)
                && small.read(input.getString("id")).has("owner162")) {
            throw new IllegalStateException("This child belongs to a recorded workflow; use its parent controls");
        }
        Operation operation = operations.get(route);
        if (operation == null) {
            throw new IllegalArgumentException(route.startsWith("/api/small161/")
                    ? "Unknown small-body operation" : "Unknown operation");
        }
        return operation.run(input);
    }

    Set<String> routes() {
        return operations.keySet();
    }
}
