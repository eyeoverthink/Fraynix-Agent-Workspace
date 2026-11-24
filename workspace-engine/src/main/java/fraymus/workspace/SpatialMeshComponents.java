
package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

/** Bounded changes to shared triangle vertices and an optional tight local box fit. */
final class SpatialMeshComponents {
    private SpatialMeshComponents() {}

    record Fit(SpatialVisualMesh mesh, double[] center, double[] size) {}

    static SpatialFaceExtrusion.Result move(SpatialVisualMesh source, JSONObject request) {
        SpatialFaceExtrusion.requireClosed(source);
        var mode = request.getString("mode");
        int[] indices = source.indices();
        boolean[] moved = new boolean[source.vertexCount()];
        int index;
        switch (mode) {
            case "vertex" -> {
                index = integer(request, "index", source.vertexCount() - 1);
                moved[index] = true;
            }
            case "edge", "face" -> {
                index = integer(request, "index", indices.length / 3 - 1);
                if (mode.equals("edge")) {
                    int edge = integer(request, "edge", 2);
                    moved[indices[index * 3 + edge]] = true;
                    moved[indices[index * 3 + (edge + 1) % 3]] = true;
                } else {
                    for (int i = 0; i < 3; i++) moved[indices[index * 3 + i]] = true;
                }
            }
            default -> throw new IllegalArgumentException("Choose vertex, edge or face.");
        }

        double[] delta = SpatialScene.vector(request.getJSONArray("deltaM"), 1);
        double length = SpatialFaceExtrusion.length(delta);
        RampDefinition.range(length, .0001, 1, "component displacement metres");
        var points = source.positions().clone();
        int count = 0;
        for (int vertex = 0; vertex < moved.length; vertex++) {
            if (!moved[vertex]) continue;
            count++;
            for (int axis = 0; axis < 3; axis++) {
                double value = points[vertex * 3 + axis] + delta[axis];
                RampDefinition.range(value, -20, 20, "edited vertex");
                points[vertex * 3 + axis] = (float) value;
            }
        }
        for (int face = 0; face < indices.length / 3; face++) {
            if (!moved[indices[face * 3]] && !moved[indices[face * 3 + 1]] && !moved[indices[face * 3 + 2]]) continue;
            var before = SpatialFaceExtrusion.normal(source.positions(), indices, face);
            var after = SpatialFaceExtrusion.normal(points, indices, face);
            if (ObserverGeometry.dot(before, after) <= 0) {
                throw new IllegalArgumentException("This move would collapse or reverse an adjacent triangle.");
            }
        }
        var mesh = new SpatialVisualMesh(points, indices);
        SpatialFaceExtrusion.requireClosed(mesh);
        var evidence = new JSONObject().put("mode", mode).put("index", index)
                .put("deltaM", new JSONArray(delta)).put("movedVertices", count)
                .put("sourceMeshSha256", source.key()).put("meshSha256", mesh.key())
                .put("triangles", indices.length / 3);
        if (mode.equals("edge")) evidence.put("edge", request.getInt("edge"));
        return new SpatialFaceExtrusion.Result(mesh, evidence);
    }

    static Fit fit(SpatialVisualMesh mesh) {
        SpatialFaceExtrusion.requireClosed(mesh);
        var low = new double[]{Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
        var high = new double[]{Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
        float[] points = mesh.positions().clone();
        for (int i = 0; i < points.length; i++) {
            int axis = i % 3;
            low[axis] = Math.min(low[axis], points[i]);
            high[axis] = Math.max(high[axis], points[i]);
        }
        var center = new double[3];
        var size = new double[3];
        for (int axis = 0; axis < 3; axis++) {
            center[axis] = (low[axis] + high[axis]) / 2;
            size[axis] = high[axis] - low[axis];
            RampDefinition.range(size[axis], .05, 40, "fitted collider size in metres");
        }
        for (int i = 0; i < points.length; i++) points[i] = (float) (points[i] - center[i % 3]);
        var recentered = new SpatialVisualMesh(points, mesh.indices());
        SpatialFaceExtrusion.requireClosed(recentered);
        return new Fit(recentered, center, size);
    }

    static int integer(JSONObject input, String name, int max) {
        double value = SpatialScene.number(input, name, 0, max);
        if ((int) value != value) throw new IllegalArgumentException("Use a whole " + name + " index.");
        return (int) value;
    }
}
