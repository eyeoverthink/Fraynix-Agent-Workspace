
package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class SpatialReconstructionAxisDepthTest {
    @Test public void perpendicularViewRecoversWhatOpposedViewsMiss() throws Exception {
        var input = fixture("side-depth.json");
        var old = fixture("opposed-depth.json");
        assertTrue(grid(input, "+z").similar(grid(old, "+z")));
        assertTrue(grid(input, "-z").similar(grid(old, "-z")));

        var pair = proof(withGrids(input, grid(input, "+z"), grid(input, "-z")));
        assertEquals(614, pair.getInt("voxels"));
        assertEquals(proof(old).getString("meshSha256"), pair.getString("meshSha256"));

        var chosen = proof(withGrids(input, grid(input, "+z"), grid(input, "-z"), grid(input, "+x")));
        var all = proof(input);
        assertEquals(SpatialReconstruction.POLICY_AXIS_DEPTH, all.getString("policy"));
        assertEquals(606, all.getInt("voxels"));
        assertEquals(44, all.getJSONObject("depth").getInt("removedVoxels"));
        assertEquals(6, all.getJSONObject("depth").getJSONArray("views").length());
        assertEquals(chosen.getString("meshSha256"), all.getString("meshSha256"));
        assertNotEquals(pair.getString("meshSha256"), all.getString("meshSha256"));
        assertEquals(.07575, all.getDouble("volumeM3"), 1e-9);

        JSONObject[] reordered = new JSONObject[6];
        var grids = input.getJSONArray("depthViews");
        for (int i = 0; i < reordered.length; i++) reordered[i] = grids.getJSONObject(reordered.length - 1 - i);
        var reverse = proof(withGrids(input, reordered));
        assertTrue(all.getJSONObject("depth").similar(reverse.getJSONObject("depth")));
        assertEquals(all.getString("meshSha256"), reverse.getString("meshSha256"));
        assertNotEquals(all.getString("sourceSha256"), reverse.getString("sourceSha256"));
    }

    @Test public void perpendicularViewsCanContradictIndependentlyPlausibleClaims() throws Exception {
        var input = fixture("side-depth.json");
        var x = witness("+x", 11, 5, 450000);
        var z = witness("+z", 11, 9, 250000);
        assertEquals(1, proof(withGrids(input, x)).getJSONObject("depth")
                .getJSONArray("views").getJSONObject(0).getInt("measuredRays"));
        assertEquals(1, proof(withGrids(input, z)).getJSONObject("depth")
                .getJSONArray("views").getJSONObject(0).getInt("measuredRays"));
        for (var order : new JSONObject[][]{{x, z}, {z, x}}) {
            var failure = assertThrows(IllegalArgumentException.class, () -> proof(withGrids(input, order)));
            assertTrue(failure.getMessage(), failure.getMessage().contains("first hit at grid cell 9, 4, 10 (+z)"));
        }
        var nearby = witness("+z", 10, 9, 250000);
        assertEquals(2, proof(withGrids(input, x, nearby)).getJSONObject("depth").getJSONArray("views").length());
    }

    @Test public void remainingFacesEachRemoveObservedFreeSpace() throws Exception {
        var input = fixture("side-depth.json");
        JSONObject[] witnesses = {
                witness("-x", 5, 6, 400000),
                witness("+y", 5, 7, 200000),
                witness("-y", 5, 6, 200000)
        };
        var seen = new java.util.HashSet<String>();
        for (var witness : witnesses) {
            var result = proof(withGrids(input, witness));
            assertEquals(witness.getString("axis"), 649, result.getInt("voxels"));
            assertEquals(1, result.getJSONObject("depth").getInt("removedVoxels"));
            assertTrue(seen.add(result.getString("meshSha256")));
        }
    }

    @Test public void eachGridUsesItsOwnMaskAndLegacyFormatsKeepTheirLimits() throws Exception {
        var input = fixture("side-depth.json");
        // +X row 3, column 5 maps to y=12,z=10 on the shoulder, x=12.
        var view = witness("+x", 3, 5, 150000);
        assertEquals(0, proof(withGrids(input, view)).getJSONObject("depth").getInt("removedVoxels"));
        var missed = WorldFrames.copy(view);
        missed.getJSONArray("samples").put(3 * 16 + 5, -1).put(11 * 16 + 7, 150000);
        var error = assertThrows(IllegalArgumentException.class, () -> proof(withGrids(input, missed)));
        assertTrue(error.getMessage().contains("silhouette foreground"));

        var v3 = withGrids(input, view).put("format", SpatialReconstruction.FORMAT_MULTIDEPTH);
        assertThrows(IllegalArgumentException.class, () -> proof(v3));
        var v2 = WorldFrames.copy(v3).put("format", SpatialReconstruction.FORMAT_DEPTH).put("depth", view);
        v2.remove("depthViews");
        assertThrows(IllegalArgumentException.class, () -> proof(v2));
        assertThrows(IllegalArgumentException.class, () -> proof(withGrids(input, view, view)));
        assertThrows(IllegalArgumentException.class, () -> proof(withGrids(input,
                WorldFrames.copy(view).put("origin", "negative-grid-face"))));
    }

    private static JSONObject witness(String face, int row, int column, int distance) {
        var values = new JSONArray();
        for (int i = 0; i < 16 * 16; i++) values.put(-2);
        values.put(row * 16 + column, distance);
        return new JSONObject().put("axis", face).put("origin", face.startsWith("+") ? "positive-grid-face" : "negative-grid-face")
                .put("unit", "um").put("samples", values);
    }

    private static JSONObject grid(JSONObject input, String axis) {
        for (Object raw : input.getJSONArray("depthViews")) {
            var grid = (JSONObject) raw;
            if (grid.getString("axis").equals(axis)) return grid;
        }
        throw new AssertionError("Missing test grid: " + axis);
    }

    private static JSONObject withGrids(JSONObject input, JSONObject... grids) {
        return WorldFrames.copy(input).put("depthViews", new JSONArray(grids));
    }

    private static JSONObject proof(JSONObject input) throws Exception {
        return SpatialReconstruction.build(input.toString().getBytes(StandardCharsets.UTF_8)).evidence();
    }

    private static JSONObject fixture(String name) throws Exception {
        try (var in = SpatialReconstructionAxisDepthTest.class.getResourceAsStream("/spatial/reconstruction/" + name)) {
            assertNotNull(in);
            return new JSONObject(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}
