
package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Extrude one connected planar region on a supporting plane of a closed triangular shell. */
final class SpatialFaceExtrusion {
    private static final double PLANE_TOLERANCE_M = 1e-6;
    private record Edge(int low, int high) {
        static Edge of(int a, int b) { return new Edge(Math.min(a, b), Math.max(a, b)); }
    }
    private record Side(int triangle, int a, int b) {}

    record Result(SpatialVisualMesh mesh, JSONObject evidence) {}

    private SpatialFaceExtrusion() {}

    static Result apply(SpatialVisualMesh source, int seed, double distanceM) {
        RampDefinition.range(distanceM, .001, 1, "extrusion metres");
        int[] corners = source.indices();
        if (seed < 0 || seed >= corners.length / 3) throw new IllegalArgumentException("Pick a triangle on this mesh.");

        var shell = shell(source);
        double[] normal = normal(source.positions(), corners, seed);
        double magnitude = length(normal);
        for (int axis = 0; axis < 3; axis++) normal[axis] /= magnitude;
        double plane = projection(source.positions(), corners[seed * 3], normal);
        for (int vertex = 0; vertex < source.vertexCount(); vertex++) {
            if (projection(source.positions(), vertex, normal) > plane + PLANE_TOLERANCE_M) {
                throw new IllegalArgumentException("Choose an outward face on a supporting plane. Recess faces are outside this tool.");
            }
        }

        boolean[] region = new boolean[corners.length / 3];
        var frontier = new ArrayDeque<Integer>();
        region[seed] = true;
        frontier.add(seed);
        while (!frontier.isEmpty()) {
            int face = frontier.removeFirst();
            for (int side = 0; side < 3; side++) {
                var edge = edge(corners, face, side);
                for (var use : shell.get(edge)) {
                    int other = use.triangle;
                    if (!region[other] && coplanar(source.positions(), corners, other, normal, plane)) {
                        region[other] = true;
                        frontier.addLast(other);
                    }
                }
            }
        }

        var boundary = new ArrayList<Side>();
        int[] exits = new int[source.vertexCount()];
        int[] entries = new int[source.vertexCount()];
        var lifted = new HashMap<Integer, Integer>();
        var points = Arrays.copyOf(source.positions(), source.positions().length * 2);
        var output = new ArrayList<Integer>(corners.length);
        double area = 0;
        int selected = 0;
        for (int face = 0; face < region.length; face++) {
            if (region[face]) {
                selected++;
                area += length(normal(source.positions(), corners, face)) * .5;
            }
            for (int side = 0; side < 3; side++) {
                int a = corners[face * 3 + side];
                if (!region[face]) {
                    output.add(a);
                    continue;
                }
                output.add(lifted.computeIfAbsent(a, original -> {
                    int shifted = source.vertexCount() + lifted.size();
                    for (int axis = 0; axis < 3; axis++) {
                        double value = points[original * 3 + axis] + normal[axis] * distanceM;
                        RampDefinition.range(value, -20, 20, "extruded vertex");
                        points[shifted * 3 + axis] = (float) value;
                    }
                    return shifted;
                }));
                int b = corners[face * 3 + (side + 1) % 3];
                var uses = shell.get(Edge.of(a, b));
                if (uses.stream().anyMatch(use -> !region[use.triangle])) {
                    if (++exits[a] > 1 || ++entries[b] > 1) {
                        throw new IllegalArgumentException("The selected region has a pinched boundary.");
                    }
                    boundary.add(new Side(face, a, b));
                }
            }
        }
        if (boundary.size() < 3 || !Arrays.equals(entries, exits)) {
            throw new IllegalArgumentException("The selected region needs a closed boundary.");
        }
        for (Side side : boundary) {
            int a = side.a, b = side.b, topA = lifted.get(a), topB = lifted.get(b);
            output.addAll(List.of(a, b, topB, a, topB, topA));
        }

        var result = compact(points, output);
        shell(result);
        double startVolume = volume(source);
        double finalVolume = volume(result);
        double predicted = area * distanceM;
        if (startVolume <= 0 || Math.abs(finalVolume - startVolume - predicted) > Math.max(1e-8, predicted * 1e-4)) {
            throw new IllegalArgumentException("The extrusion did not pass its signed volume check.");
        }
        var evidence = new JSONObject().put("sourceMeshSha256", source.key()).put("meshSha256", result.key())
                .put("seedTriangle", seed).put("distanceM", distanceM).put("normal", new JSONArray(normal))
                .put("regionTriangles", selected).put("boundaryEdges", boundary.size()).put("regionAreaM2", area)
                .put("originalVolumeM3", startVolume).put("resultVolumeM3", finalVolume)
                .put("predictedGainM3", predicted).put("triangles", result.indices().length / 3);
        return new Result(result, evidence);
    }

    private static Map<Edge, List<Side>> shell(SpatialVisualMesh mesh) {
        var sides = new HashMap<Edge, List<Side>>();
        int[] first = new int[mesh.vertexCount()];
        int[] incidence = new int[mesh.vertexCount()];
        for (int vertex : mesh.indices()) incidence[vertex]++;
        int[] corners = mesh.indices();
        for (int face = 0; face < corners.length / 3; face++) {
            if (length(normal(mesh.positions(), corners, face)) < 1e-10) {
                throw new IllegalArgumentException("Clean degenerate triangles before extruding.");
            }
            for (int side = 0; side < 3; side++) {
                int a = corners[face * 3 + side], b = corners[face * 3 + (side + 1) % 3];
                first[a] = face;
                sides.computeIfAbsent(Edge.of(a, b), key -> new ArrayList<>(2)).add(new Side(face, a, b));
            }
        }
        for (var uses : sides.values()) {
            if (uses.size() != 2 || uses.get(0).a != uses.get(1).b) {
                throw new IllegalArgumentException("Use a joined, consistently oriented closed triangle shell.");
            }
        }
        for (int vertex = 0; vertex < mesh.vertexCount(); vertex++) {
            if (incidence[vertex] == 0) throw new IllegalArgumentException("Remove unused vertices from the shell.");
            int origin = first[vertex];
            int face = origin;
            int visited = 0;
            do {
                int local = 0;
                while (corners[face * 3 + local] != vertex) local++;
                int next = corners[face * 3 + (local + 1) % 3];
                var across = sides.get(Edge.of(vertex, next));
                face = across.get(0).triangle == face ? across.get(1).triangle : across.get(0).triangle;
                if (++visited > incidence[vertex]) throw new IllegalArgumentException("The shell has a disconnected vertex fan.");
            } while (face != origin);
            if (visited != incidence[vertex]) throw new IllegalArgumentException("The shell has a disconnected vertex fan.");
        }
        return sides;
    }

    static void requireClosed(SpatialVisualMesh mesh) {
        shell(mesh);
        if (volume(mesh) <= 0) throw new IllegalArgumentException("Use a shell with positive signed volume.");
    }

    private static Edge edge(int[] corners, int face, int side) {
        return Edge.of(corners[face * 3 + side], corners[face * 3 + (side + 1) % 3]);
    }

    private static boolean coplanar(float[] points, int[] corners, int face, double[] normal, double plane) {
        for (int side = 0; side < 3; side++) {
            if (Math.abs(projection(points, corners[face * 3 + side], normal) - plane) > PLANE_TOLERANCE_M) return false;
        }
        double[] cross = normal(points, corners, face);
        return cross[0] * normal[0] + cross[1] * normal[1] + cross[2] * normal[2] > 0;
    }

    private static double projection(float[] points, int vertex, double[] normal) {
        int i = vertex * 3;
        return points[i] * normal[0] + points[i + 1] * normal[1] + points[i + 2] * normal[2];
    }

    static double[] normal(float[] points, int[] corners, int face) {
        int a = corners[face * 3] * 3, b = corners[face * 3 + 1] * 3, c = corners[face * 3 + 2] * 3;
        double bx = (double) points[b] - points[a], by = (double) points[b + 1] - points[a + 1], bz = (double) points[b + 2] - points[a + 2];
        double cx = (double) points[c] - points[a], cy = (double) points[c + 1] - points[a + 1], cz = (double) points[c + 2] - points[a + 2];
        return new double[]{by * cz - bz * cy, bz * cx - bx * cz, bx * cy - by * cx};
    }

    static double length(double[] v) {
        return Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    }

    private static double volume(SpatialVisualMesh mesh) {
        var points = mesh.positions();
        var corners = mesh.indices();
        double sum = 0;
        for (int face = 0; face < corners.length / 3; face++) {
            sum += projection(points, corners[face * 3], normal(points, corners, face));
        }
        return sum / 6;
    }

    private static SpatialVisualMesh compact(float[] points, List<Integer> index) {
        int[] map = new int[points.length / 3];
        Arrays.fill(map, -1);
        int[] elements = new int[index.size()];
        float[] vertices = new float[points.length];
        int count = 0;
        for (int i = 0; i < elements.length; i++) {
            int original = index.get(i);
            if (map[original] < 0) {
                map[original] = count++;
                System.arraycopy(points, original * 3, vertices, map[original] * 3, 3);
            }
            elements[i] = map[original];
        }
        if (count > SpatialVisualMesh.MAX_VERTICES || elements.length > SpatialVisualMesh.MAX_INDICES) {
            throw new IllegalArgumentException("The edited surface exceeds the mesh budget.");
        }
        return new SpatialVisualMesh(Arrays.copyOf(vertices, count * 3), elements);
    }
}
