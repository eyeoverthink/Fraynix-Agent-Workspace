
package fraymus.workspace;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.Assert.*;

public class SpatialFaceExtrusionTest {
    @Test public void extrudesAConnectedCapAndExportsTheSameMetricShell() throws Exception {
        var mesh = fixture();
        int top = plane(mesh, 1, .25);
        var result = SpatialFaceExtrusion.apply(mesh, top, .10);
        var proof = result.evidence();
        assertEquals(84, proof.getInt("regionTriangles"));
        assertEquals(26, proof.getInt("boundaryEdges"));
        assertEquals(.0105, proof.getDouble("predictedGainM3"), 1e-8);
        assertEquals(.08625, proof.getDouble("resultVolumeM3"), 1e-8);
        assertEquals(1428, result.mesh().indices().length / 3);
        assertNotEquals(mesh.key(), result.mesh().key());

        var stl = ByteBuffer.wrap(SpatialStl.encode(result.mesh())).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(1428, stl.getInt(80));
        stl.position(84);
        float bottom = Float.POSITIVE_INFINITY;
        float topMm = Float.NEGATIVE_INFINITY;
        while (stl.hasRemaining()) {
            stl.position(stl.position() + 12);
            for (int i = 0; i < 3; i++) {
                stl.getFloat();
                stl.getFloat();
                float z = stl.getFloat();
                bottom = Math.min(bottom, z);
                topMm = Math.max(topMm, z);
            }
            assertEquals(0, stl.getShort());
        }
        assertEquals(0, bottom, 0);
        assertEquals(600, topMm, .0001);
    }

    @Test public void refusesRecessFacesAndOpenShells() throws Exception {
        var mesh = fixture();
        int recess = plane(mesh, 2, .10);
        var unsupported = assertThrows(IllegalArgumentException.class, () -> SpatialFaceExtrusion.apply(mesh, recess, .10));
        assertTrue(unsupported.getMessage().contains("supporting plane"));
        int[] indices = Arrays.copyOf(mesh.indices(), mesh.indices().length - 3);
        var open = new SpatialVisualMesh(mesh.positions(), indices);
        assertThrows(IllegalArgumentException.class, () -> SpatialStl.encode(open));
        assertThrows(IllegalArgumentException.class, () -> SpatialFaceExtrusion.apply(open, 0, .10));
    }

    private static int plane(SpatialVisualMesh mesh, int axis, double value) {
        var points = mesh.positions();
        for (int i = 0; i < mesh.indices().length; i += 3) {
            var corners = mesh.indices();
            double[] normal = SpatialFaceExtrusion.normal(points, corners, i / 3);
            if (normal[axis] > 0 && Math.abs(points[corners[i] * 3 + axis] - value) < 1e-6
                    && Math.abs(points[corners[i + 1] * 3 + axis] - value) < 1e-6
                    && Math.abs(points[corners[i + 2] * 3 + axis] - value) < 1e-6) {
                return i / 3;
            }
        }
        throw new AssertionError("No outward triangle at the test coordinate.");
    }

    private static SpatialVisualMesh fixture() throws Exception {
        try (var in = SpatialFaceExtrusionTest.class.getResourceAsStream("/spatial/reconstruction/side-depth.json")) {
            assertNotNull(in);
            var result = SpatialReconstruction.build(in.readAllBytes());
            var scene = SpatialScene.parse(result.definition().toString(), UUID.randomUUID().toString());
            return SpatialGlb.asset(scene, scene.getJSONArray("objects").getJSONObject(1));
        }
    }
}
