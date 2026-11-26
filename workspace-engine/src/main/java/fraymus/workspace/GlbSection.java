package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Intersects the retained glTF mesh triangles with a horizontal world plane. */
final class GlbSection {
    static final int CAP = 50_000;
    private static final double QUANTUM = 1e-6;

    record Point(long x, long z) implements Comparable<Point> {
        double xm() { return x * QUANTUM; }
        double zm() { return z * QUANTUM; }

        @Override public int compareTo(Point other) {
            int order = Long.compare(x, other.x);
            return order == 0 ? Long.compare(z, other.z) : order;
        }
    }

    record Line(Point a, Point b) {
        static Line between(Point a, Point b) {
            return a.compareTo(b) <= 0 ? new Line(a, b) : new Line(b, a);
        }
    }

    private record Vertex(double x, double y, double z) {}

    record Result(Map<Integer, Set<Line>> lines, JSONObject grid) {}

    private record Access(ByteBuffer bin, JSONObject accessor, int start, int stride) {
        int count() { return accessor.getInt("count"); }
        int index(int i) {
            int offset = start + i * stride;
            return switch (accessor.getInt("componentType")) {
                case 5121 -> Byte.toUnsignedInt(bin.get(offset));
                case 5123 -> Short.toUnsignedInt(bin.getShort(offset));
                default -> bin.getInt(offset);
            };
        }
        Vertex vertex(int i, double[] matrix) {
            int offset = start + i * stride;
            double x = bin.getFloat(offset), y = bin.getFloat(offset + 4), z = bin.getFloat(offset + 8);
            return new Vertex(axis(matrix, 0, x, y, z), axis(matrix, 1, x, y, z), axis(matrix, 2, x, y, z));
        }
    }

    private GlbSection() {}

    record Ray(double[] origin, double[] direction, double length) {}

    record Hit(int node, double distance) {}

    @FunctionalInterface
    private interface Triangle {
        void accept(int node, Vertex a, Vertex b, Vertex c);
    }

    static Result cut(byte[] glb, JSONArray catalog, double height) {
        var lines = new LinkedHashMap<Integer, Set<Line>>();
        int[] count = {0};
        var document = visit(glb, catalog, (node, a, b, c) -> {
            double sample = height + QUANTUM / 8;
            Point ab = edge(a, b, sample), bc = edge(b, c, sample), ca = edge(c, a, sample);
            Point start = ab == null ? bc : ab;
            Point end = ca == null ? bc : ca;
            if (start != null && end != null && !start.equals(end)
                    && lines.computeIfAbsent(node, ignored -> new LinkedHashSet<>()).add(Line.between(start, end))) {
                require(++count[0] <= CAP, "This section exceeds 50,000 edges. Try another section height.");
            }
        });
        var extras = document.optJSONObject("extras", new JSONObject());
        return new Result(lines, extras.optJSONObject("fraynixFloorPlanGrid"));
    }

    static Hit[] probe(byte[] glb, JSONArray catalog, Ray... rays) {
        var hits = new Hit[rays.length];
        visit(glb, catalog, (node, a, b, c) -> {
            for (int i = 0; i < rays.length; i++) {
                double distance = intersect(rays[i], a, b, c);
                if (distance > 1e-6 && distance <= rays[i].length
                        && (hits[i] == null || distance < hits[i].distance)) hits[i] = new Hit(node, distance);
            }
        });
        return hits;
    }

    private static double intersect(Ray ray, Vertex a, Vertex b, Vertex c) {
        double ux = b.x - a.x, uy = b.y - a.y, uz = b.z - a.z;
        double vx = c.x - a.x, vy = c.y - a.y, vz = c.z - a.z;
        double dx = ray.direction[0], dy = ray.direction[1], dz = ray.direction[2];
        double px = dy*vz - dz*vy, py = dz*vx - dx*vz, pz = dx*vy - dy*vx;
        double det = ux*px + uy*py + uz*pz;
        if (Math.abs(det) < 1e-12) return -1;
        double tx = ray.origin[0] - a.x, ty = ray.origin[1] - a.y, tz = ray.origin[2] - a.z;
        double u = (tx*px + ty*py + tz*pz) / det;
        if (u < -1e-9 || u > 1 + 1e-9) return -1;
        double qx = ty*uz - tz*uy, qy = tz*ux - tx*uz, qz = tx*uy - ty*ux;
        double v = (dx*qx + dy*qy + dz*qz) / det;
        if (v < -1e-9 || u + v > 1 + 1e-9) return -1;
        return (vx*qx + vy*qy + vz*qz) / det;
    }

    private static JSONObject visit(byte[] glb, JSONArray catalog, Triangle visitor) {
        // EnvironmentStore already validated the exact retained container and catalogue.
        var bin = ByteBuffer.wrap(glb).order(ByteOrder.LITTLE_ENDIAN);
        int jsonLength = bin.getInt(12);
        var document = new JSONObject(new String(glb, 20, jsonLength, StandardCharsets.UTF_8));
        var definitions = document.getJSONArray("nodes");
        var meshes = document.getJSONArray("meshes");
        var matrices = new double[definitions.length()][];
        for (Object value : catalog) {
            var row = (JSONObject)value;
            int index = row.getInt("index"), parent = row.getInt("parent");
            double[] local = transform(definitions.getJSONObject(index));
            matrices[index] = parent < 0 ? local : multiply(matrices[parent], local);
            if (!row.has("mesh")) continue;
            for (Object primitive : meshes.getJSONObject(row.getInt("mesh")).getJSONArray("primitives"))
                visitPrimitive(index, bin, jsonLength + 28, document, (JSONObject)primitive, matrices[index], visitor);
        }
        return document;
    }

    private static void visitPrimitive(int node, ByteBuffer bin, int base, JSONObject doc,
                                       JSONObject primitive, double[] matrix, Triangle visitor) {
        var position = access(bin, base, doc, primitive.getJSONObject("attributes").getInt("POSITION"), 12);
        Access indices = primitive.has("indices") ? access(bin, base, doc, primitive.getInt("indices"), 0) : null;
        var vertices = new Vertex[position.count()];
        for (int i = 0; i < vertices.length; i++) vertices[i] = position.vertex(i, matrix);
        int size = indices == null ? vertices.length : indices.count();
        for (int i = 0; i < size; i += 3)
            visitor.accept(node, vertices[indices == null ? i : indices.index(i)],
                    vertices[indices == null ? i + 1 : indices.index(i + 1)],
                    vertices[indices == null ? i + 2 : indices.index(i + 2)]);
    }

    private static Point edge(Vertex a, Vertex b, double height) {
        if ((a.y >= height) == (b.y >= height)) return null;
        double t = (height - a.y) / (b.y - a.y);
        double x = a.x + t * (b.x - a.x), z = a.z + t * (b.z - a.z);
        require(Double.isFinite(x) && Double.isFinite(z) && Math.abs(x) <= 100_000 && Math.abs(z) <= 100_000,
                "The section lies outside the supported coordinate range.");
        return new Point(Math.round(x / QUANTUM), Math.round(z / QUANTUM));
    }

    private static Access access(ByteBuffer bin, int base, JSONObject doc, int index, int width) {
        var accessor = doc.getJSONArray("accessors").getJSONObject(index);
        var view = doc.getJSONArray("bufferViews").getJSONObject(accessor.getInt("bufferView"));
        int component = accessor.getInt("componentType");
        int size = width == 0 ? component == 5121 ? 1 : component == 5123 ? 2 : 4 : width;
        return new Access(bin, accessor, base + view.optInt("byteOffset") + accessor.optInt("byteOffset"),
                view.optInt("byteStride", size));
    }

    private static double axis(double[] matrix, int row, double x, double y, double z) {
        return matrix[row] * x + matrix[4 + row] * y + matrix[8 + row] * z + matrix[12 + row];
    }

    private static double[] transform(JSONObject node) {
        if (node.has("matrix")) {
            double[] matrix = vector(node, "matrix");
            require(matrix[3] == 0 && matrix[7] == 0 && matrix[11] == 0 && matrix[15] == 1,
                    "A floor-plan section needs affine mesh transforms.");
            return matrix;
        }
        double[] s = node.has("scale") ? vector(node, "scale") : new double[]{1, 1, 1};
        double[] t = node.has("translation") ? vector(node, "translation") : new double[]{0, 0, 0};
        double[] q = node.has("rotation") ? vector(node, "rotation") : new double[]{0, 0, 0, 1};
        double x = q[0], y = q[1], z = q[2], w = q[3];
        return new double[]{
                (1 - 2 * (y*y + z*z)) * s[0], 2 * (x*y + z*w) * s[0], 2 * (x*z - y*w) * s[0], 0,
                2 * (x*y - z*w) * s[1], (1 - 2 * (x*x + z*z)) * s[1], 2 * (y*z + x*w) * s[1], 0,
                2 * (x*z + y*w) * s[2], 2 * (y*z - x*w) * s[2], (1 - 2 * (x*x + y*y)) * s[2], 0,
                t[0], t[1], t[2], 1
        };
    }

    private static double[] vector(JSONObject node, String name) {
        JSONArray raw = node.getJSONArray(name);
        var result = new double[raw.length()];
        for (int i = 0; i < result.length; i++) result[i] = raw.getDouble(i);
        return result;
    }

    private static double[] multiply(double[] parent, double[] local) {
        double[] out = new double[16];
        for (int column = 0; column < 16; column += 4)
            for (int row = 0; row < 4; row++)
                out[column + row] = parent[row] * local[column] + parent[4 + row] * local[column + 1]
                        + parent[8 + row] * local[column + 2] + parent[12 + row] * local[column + 3];
        return out;
    }

    private static void require(boolean accepted, String message) {
        if (!accepted) throw new IllegalArgumentException(message);
    }
}
