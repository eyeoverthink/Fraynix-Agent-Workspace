
package fraymus.workspace;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/** Binary STL of one checked visual shell in local millimetres with Z up. */
final class SpatialStl {
    private static final byte[] HEADER = "Fraynix visual shell in millimetres, Z up".getBytes(StandardCharsets.US_ASCII);

    private SpatialStl() {}

    static byte[] encode(SpatialVisualMesh mesh) {
        SpatialFaceExtrusion.requireClosed(mesh);
        float[] points = mesh.positions();
        int[] corners = mesh.indices();
        int triangles = corners.length / 3;
        var out = ByteBuffer.allocate(84 + triangles * 50).order(ByteOrder.LITTLE_ENDIAN);
        out.put(HEADER).position(80);
        out.putInt(triangles);
        float bottomY = Float.POSITIVE_INFINITY;
        for (int vertex : corners) bottomY = Math.min(bottomY, points[vertex * 3 + 1]);
        for (int face = 0; face < triangles; face++) {
            double[] n = SpatialFaceExtrusion.normal(points, corners, face);
            double length = SpatialFaceExtrusion.length(n);
            out.putFloat((float) (n[0] / length));
            out.putFloat((float) (-n[2] / length));
            out.putFloat((float) (n[1] / length));
            for (int i = 0; i < 3; i++) {
                int p = corners[face * 3 + i] * 3;
                out.putFloat(points[p] * 1000);
                out.putFloat(-points[p + 2] * 1000);
                out.putFloat((points[p + 1] - bottomY) * 1000);
            }
            out.putShort((short) 0);
        }
        return out.array();
    }
}
