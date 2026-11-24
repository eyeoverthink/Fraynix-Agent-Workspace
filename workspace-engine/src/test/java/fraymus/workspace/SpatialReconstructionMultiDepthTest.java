package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class SpatialReconstructionMultiDepthTest {
    private static final int WITNESS=(15-10)*16+9;

    @Test public void eachSideCarvesItsRecessAndFusionIsOrderIndependent() throws Exception {
        var input=fixture();
        var grids=input.getJSONArray("depthViews");
        var plus=withGrids(input,grids.getJSONObject(0));
        var minus=withGrids(input,grids.getJSONObject(1));
        var plain=WorldFrames.copy(input).put("format",SpatialReconstruction.FORMAT);
        plain.remove("depthViews");
        assertEquals(650,proof(plain).getInt("voxels"));
        assertEquals(632,proof(plus).getInt("voxels"));
        assertEquals(632,proof(minus).getInt("voxels"));
        assertNotEquals(proof(plus).getString("meshSha256"),proof(minus).getString("meshSha256"));

        var combined=proof(input);
        assertEquals(614,combined.getInt("voxels"));
        assertEquals(.07675,combined.getDouble("volumeM3"),1e-9);
        assertEquals(36,combined.getJSONObject("depth").getInt("removedVoxels"));
        assertEquals(18,combined.getJSONObject("depth").getJSONArray("views").getJSONObject(1).getInt("removedVoxels"));
        var reverse=proof(withGrids(input,grids.getJSONObject(1),grids.getJSONObject(0)));
        assertTrue(combined.getJSONObject("depth").similar(reverse.getJSONObject("depth")));
        assertEquals(combined.getString("meshSha256"),reverse.getString("meshSha256"));
        assertNotEquals(combined.getString("sourceSha256"),reverse.getString("sourceSha256"));
    }

    @Test public void contradictoryHitsRefuseAndAnExactOneVoxelSlabSurvives() throws Exception {
        var input=fixture();
        var forward=input.getJSONArray("depthViews").getJSONObject(0);
        var inverse=input.getJSONArray("depthViews").getJSONObject(1);
        inverse.getJSONArray("samples").put(WITNESS,500000);
        assertTrue(proof(withGrids(input,inverse)).getInt("voxels")<632);
        for (var order:new JSONObject[][]{{forward,inverse},{inverse,forward}}) {
            var failure=assertThrows(IllegalArgumentException.class,()->proof(withGrids(input,order)));
            assertTrue(failure.getMessage().contains("Conflicting depth"));
        }

        inverse.getJSONArray("samples").put(WITNESS,450000);
        var slab=proof(withGrids(input,forward,inverse));
        assertEquals(610,slab.getInt("voxels"));
        var projections=slab.getJSONObject("inputProjectionIou");
        for (String axis:new String[]{"x","y","z"}) assertEquals(1,projections.getDouble(axis),0);
    }

    @Test public void unknownRaysAndLegacySingleDepthRemainCompatible() throws Exception {
        var input=fixture();
        var front=input.getJSONArray("depthViews").getJSONObject(0);
        var back=input.getJSONArray("depthViews").getJSONObject(1);
        for (int y=4;y<6;y++) for (int x=7;x<10;x++) back.getJSONArray("samples").put((15-y)*16+x,-2);

        var fused=proof(input);
        assertEquals(proof(withGrids(input,front)).getString("meshSha256"),fused.getString("meshSha256"));
        assertEquals(6,fused.getJSONObject("depth").getJSONArray("views").getJSONObject(1).getInt("unobservedRays"));

        var legacy=WorldFrames.copy(input).put("format",SpatialReconstruction.FORMAT_DEPTH).put("depth",front);
        legacy.remove("depthViews");
        var old=proof(legacy);
        assertEquals(SpatialReconstruction.POLICY_DEPTH,old.getString("policy"));
        assertEquals(18,old.getJSONObject("depth").getInt("removedVoxels"));
        assertFalse(old.getJSONObject("depth").has("views"));
        assertEquals(fused.getString("meshSha256"),old.getString("meshSha256"));
    }

    @Test public void rejectsDuplicateDirectionsAndWrongNearPlane() throws Exception {
        var input=fixture();
        var grid=input.getJSONArray("depthViews").getJSONObject(0);
        assertThrows(IllegalArgumentException.class,()->proof(withGrids(input,grid,grid)));
        var invalid=WorldFrames.copy(grid).put("axis","-z");
        assertThrows(IllegalArgumentException.class,()->proof(withGrids(input,grid,invalid)));
        assertThrows(IllegalArgumentException.class,()->proof(withGrids(input)));
    }

    private static JSONObject withGrids(JSONObject source,JSONObject... grids) {
        return WorldFrames.copy(source).put("depthViews",new JSONArray(grids));
    }

    private static JSONObject proof(JSONObject input) throws Exception {
        return SpatialReconstruction.build(input.toString().getBytes(StandardCharsets.UTF_8)).evidence();
    }

    private static JSONObject fixture() throws Exception {
        try (var in=SpatialReconstructionMultiDepthTest.class.getResourceAsStream("/spatial/reconstruction/opposed-depth.json")) {
            assertNotNull(in);
            return new JSONObject(new String(in.readAllBytes(),StandardCharsets.UTF_8));
        }
    }
}
