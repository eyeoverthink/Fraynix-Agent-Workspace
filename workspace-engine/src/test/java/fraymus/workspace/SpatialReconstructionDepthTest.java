package fraymus.workspace;

import org.json.JSONObject;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class SpatialReconstructionDepthTest {
    @Test public void carvesTheObservedRecessAndLeavesTheSilhouettesIntact() throws Exception {
        var source=fixture();
        var result=SpatialReconstruction.build(bytes(source));
        var proof=result.evidence();
        var depth=proof.getJSONObject("depth");
        assertEquals(SpatialReconstruction.POLICY_DEPTH,proof.getString("policy"));
        assertEquals(18,depth.getInt("removedVoxels"));
        assertEquals(.079,proof.getDouble("volumeM3"),1e-9);
        for (String axis:new String[]{"x","y","z"})
            assertEquals(1,proof.getJSONObject("inputProjectionIou").getDouble(axis),0);
        var omitted=withoutDepth(source);
        var baseline=SpatialReconstruction.build(bytes(omitted)).evidence();
        assertEquals(650,baseline.getInt("voxels"));
        assertEquals(.08125,baseline.getDouble("volumeM3"),1e-9);
        assertNotEquals(baseline.getString("meshSha256"),proof.getString("meshSha256"));
    }

    @Test public void unknownRecessRaysPreserveTheUnobservedCells() throws Exception {
        var source=fixture();
        var samples=source.getJSONObject("depth").getJSONArray("samples");
        for (int y=9;y<12;y++) for (int x=8;x<11;x++) samples.put((15-y)*16+x,-2);
        var result=SpatialReconstruction.build(bytes(source)).evidence();
        assertEquals(0,result.getJSONObject("depth").getInt("removedVoxels"));
        assertEquals(9,result.getJSONObject("depth").getInt("unobservedRays"));
        assertEquals(SpatialReconstruction.build(bytes(withoutDepth(source))).evidence().getString("meshSha256"),
                result.getString("meshSha256"));
    }

    @Test public void rejectsImpossibleOrUnregisteredDepth() throws Exception {
        for (var incorrect:new Object[]{"300000",300001.5,302000,0,900000,-3,-1,50000}) {
            var source=fixture();
            source.getJSONObject("depth").getJSONArray("samples").put((15-10)*16+9,incorrect);
            assertThrows(IllegalArgumentException.class,()->SpatialReconstruction.build(bytes(source)));
        }
        var wrongAxis=fixture();
        wrongAxis.getJSONObject("depth").put("axis","-z");
        assertThrows(IllegalArgumentException.class,()->SpatialReconstruction.build(bytes(wrongAxis)));
        var empty=fixture();
        for (int i=0;i<256;i++) empty.getJSONObject("depth").getJSONArray("samples").put(i,-2);
        assertThrows(IllegalArgumentException.class,()->SpatialReconstruction.build(bytes(empty)));
    }

    @Test public void exposesTheDeclaredGridSnapAtBothToleranceEdges() throws Exception {
        var expected=SpatialReconstruction.build(bytes(fixture())).evidence().getString("meshSha256");
        int index=(15-10)*16+9;
        for (int near:new int[]{299500,300500}) {
            var source=fixture();
            source.getJSONObject("depth").getJSONArray("samples").put(index,near);
            var result=SpatialReconstruction.build(bytes(source)).evidence();
            assertEquals(expected,result.getString("meshSha256"));
            assertEquals(500,result.getJSONObject("depth").getDouble("maxInputGridResidualUm"),1e-10);
        }
        for (int beyond:new int[]{299499,300501}) {
            var source=fixture();
            source.getJSONObject("depth").getJSONArray("samples").put(index,beyond);
            assertThrows(IllegalArgumentException.class,()->SpatialReconstruction.build(bytes(source)));
        }
    }

    @Test public void rejectsCarvingThatErasesAnotherObservedProjection() throws Exception {
        var source=fixture();
        var samples=source.getJSONObject("depth").getJSONArray("samples");
        for (int y=8;y<13;y++) for (int x=7;x<13;x++) samples.put((15-y)*16+x,500000);
        var error=assertThrows(IllegalArgumentException.class,()->SpatialReconstruction.build(bytes(source)));
        assertTrue(error.getMessage(),error.getMessage().contains("Depth conflicts with the registered silhouettes"));
    }

    private static JSONObject withoutDepth(JSONObject source) {
        var placement=WorldFrames.copy(source).put("format",SpatialReconstruction.FORMAT);
        placement.remove("depth");
        return placement;
    }

    private static byte[] bytes(JSONObject source) { return source.toString().getBytes(StandardCharsets.UTF_8); }

    private static JSONObject fixture() throws Exception {
        try (var in=SpatialReconstructionDepthTest.class.getResourceAsStream("/spatial/reconstruction/recess-depth.json")) {
            assertNotNull(in);
            return new JSONObject(new String(in.readAllBytes(),StandardCharsets.UTF_8));
        }
    }
}
