package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

public class ConstructionSchema096Test {
    @Test public void javaOwnedChoicesNeverBecomeDirectOrBoundModelArguments() {
        var catalog = Tools044.definition("construction.start");
        var task = Domain044Test.task("construction");
        task.getJSONObject("context044").put("procedureBinding", new JSONObject().put("name", "bridge").put("version", 1));
        // Even a malformed requested-fields projection cannot grant the model this authority.
        task.getJSONObject("requested044").put("design053", true).put("reviewCounterfactual045", true).put("centerX", .35);
        for (String tool : List.of("construction.start", "procedure.run")) {
            var schema = Tools044.schema(tool, task);
            for (String flag : List.of("design053", "reviewCounterfactual045")) {
                assertFalse(schema.getJSONObject("properties").has(flag));
                assertThrows(IllegalArgumentException.class, () -> ToolSchema043.validate(schema, new JSONObject().put(flag, false)));
            }
            assertTrue(schema.getJSONObject("properties").has("centerX"));
        }
        assertTrue(catalog.similar(Tools044.definition("construction.start")));
        assertTrue(catalog.getJSONObject("function").getJSONObject("parameters").getJSONObject("properties").has("design053"));
    }
    @Test public void requestPinnedReviewAndCeilingsSurviveBothPreparationPaths() throws Exception {
        try (var store = new WorkspaceStore(Files.createTempDirectory("construction096-"))) {
            var domain = new Domain044((route, args) -> route.equals("/api/state") ? new JSONObject()
                .put("scenes", new JSONArray()).put("datasets", new JSONArray()).put("procedures", new JSONArray()) : new JSONObject(), store);
            for (String tool : List.of("construction.start", "procedure.run")) {
                var task = Domain044Test.task("workbench").put("goal", "Compare assembly designs for the bridge at centerX 0.35, forceLimitN 40; do not move anything");
                domain.initialize(task, new JSONObject(), new JSONObject());
                if (tool.equals("procedure.run")) task.getJSONObject("context044").put("procedureBinding", new JSONObject().put("name", "bridge").put("version", 1));
                var prepared = domain.prepare(task, tool, new JSONObject().put("design053", false).put("reviewCounterfactual045", false));
                assertTrue(prepared.getBoolean("design053"));
                assertTrue(prepared.getBoolean("reviewCounterfactual045"));
                assertEquals(40, prepared.getDouble("forceLimitN"), 0);
                assertThrows(IllegalArgumentException.class, () -> domain.prepare(task, tool, new JSONObject().put("forceLimitN", 60)));
            }
        }
    }
}
