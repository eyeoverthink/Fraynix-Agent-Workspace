package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;
import java.util.UUID;

import static fraymus.workspace.ObserverGeometry.*;
import static org.junit.Assert.*;

public class LoadingAisleRouteTest {
    @Rule public final TemporaryFolder temporary = new TemporaryFolder();

    private static JSONObject scene() throws Exception {
        return SpatialScene.parse(LoadingAisleRoute.fixture("staggered-east").toString(), UUID.randomUUID().toString());
    }

    private static JSONObject direct(JSONObject parent) {
        var request = LoadingAisleIntent.parse("Move the blue crate 40 cm away from the loading door.");
        return LoadingAisleIntent.base(parent,LoadingAisle.find(parent,"cargo").getString("id"),request,
                new JSONObject().put("kind","request").put("priorSelection",""));
    }

    @Test public void clearEndpointsDoNotHideAnObstructionInTheThirdSegment() throws Exception {
        Path nativeDir = temporary.newFolder("native").toPath();
        var parent = scene();
        var direct = direct(parent);
        var forged = LoadingAisleRoute.rebuild(direct,new JSONObject()
                .put("cells",new JSONArray("[[0,0],[0,2],[2,2],[4,2],[4,0]]"))
                .put("direct",new JSONObject().put("gate","path").put("reason","Direct movement is blocked.")));
        try (var world = new SpatialPhysics(parent,nativeDir)) {
            var blocked = assertThrows(LoadingAisle.Obstruction.class,() -> LoadingAisleRoute.verify(world,forged));
            assertEquals("path",blocked.gate);
            assertTrue(blocked.getMessage(),blocked.getMessage().startsWith("Segment 3:"));
            assertTrue(blocked.getMessage(),blocked.getMessage().contains("Outside staggered barrier"));
        }
    }

    @Test public void theContinuousSweepCannotCutThroughTheAisle() throws Exception {
        var parent = scene();
        var review = direct(parent);
        var entry = v(review.getJSONObject("aisle").getJSONArray("entryM"));
        var normal = v(review.getJSONArray("directionM"));
        var side = new double[]{normal[2],0,-normal[0]};
        var cross = add(add(entry,mul(normal,2)),new double[]{0,.08,0});
        var right = add(cross,mul(side,1.3));
        var left = add(cross,mul(side,-1.3));
        try (var world = new SpatialPhysics(parent,temporary.newFolder("aisle").toPath());
             var probe = new LoadingAisle.RouteProbe(world,review)) {
            var blocked = assertThrows(LoadingAisle.Obstruction.class,() -> probe.check(right,left));
            assertEquals("sweep-aisle",blocked.gate);
            assertTrue(blocked.getMessage(),blocked.getMessage().contains("would cross"));
        }
    }
}
