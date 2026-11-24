
package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/** Combines registered first hits without allowing one view to erase another's surface evidence. */
final class SpatialReconstructionDepth {
    private static final int NO_HIT = -1;
    private static final int UNOBSERVED = -2;
    private static final double GRID_TOLERANCE_UM = 500;

    private enum Axis {
        X, Y, Z;

        Cell cell(int n, int row, int column, int depth) {
            return switch (this) {
                case X -> new Cell(depth, n - 1 - row, n - 1 - column);
                case Y -> new Cell(column, depth, row);
                case Z -> new Cell(column, n - 1 - row, depth);
            };
        }
    }

    private enum Direction {
        POSITIVE_X("+x", Axis.X, true),
        NEGATIVE_X("-x", Axis.X, false),
        POSITIVE_Y("+y", Axis.Y, true),
        NEGATIVE_Y("-y", Axis.Y, false),
        POSITIVE_Z("+z", Axis.Z, true),
        NEGATIVE_Z("-z", Axis.Z, false);

        final String label, origin;
        final Axis axis;
        final boolean positive;

        Direction(String label, Axis axis, boolean positive) {
            this.label = label;
            this.axis = axis;
            this.positive = positive;
            this.origin = positive ? "positive-grid-face" : "negative-grid-face";
        }

        int voxel(int n, int face) {
            return positive ? n - 1 - face : face;
        }

        static Direction read(String axis) {
            return switch (axis) {
                case "+x" -> POSITIVE_X;
                case "-x" -> NEGATIVE_X;
                case "+y" -> POSITIVE_Y;
                case "-y" -> NEGATIVE_Y;
                case "+z" -> POSITIVE_Z;
                case "-z" -> NEGATIVE_Z;
                default -> throw new IllegalArgumentException("Use a registered ±X, ±Y or ±Z orthographic direction.");
            };
        }
    }

    private record Cell(int x, int y, int z) {}
    private record Hit(int row, int column, int depth) {}
    private record Plan(Direction direction, List<Hit> hits, JSONObject proof) {}

    private SpatialReconstructionDepth() {}

    static JSONObject carve(JSONObject input, boolean[][][] hull, boolean[][] silhouette, double cellM) {
        var direction = Direction.read(input.getString("axis"));
        if (direction != Direction.POSITIVE_Z) {
            throw new IllegalArgumentException("The single-depth format requires +Z orthographic rays.");
        }
        var plan = read(input, direction, hull, silhouette, cellM);
        return apply(List.of(plan), hull).getJSONArray("views").getJSONObject(0);
    }

    static JSONObject fuse(JSONArray inputs, boolean[][][] hull, boolean[][][] masks, double cellM, boolean allAxes) {
        int limit = allAxes ? Direction.values().length : 2;
        if (inputs.length() < 1 || inputs.length() > limit) {
            throw new IllegalArgumentException("Provide between one and " + limit + " distinct registered depth grids.");
        }
        var plans = new EnumMap<Direction, Plan>(Direction.class);
        for (Object raw : inputs) {
            var input = (JSONObject) raw;
            var direction = Direction.read(input.getString("axis"));
            if (!allAxes && direction.axis != Axis.Z) {
                throw new IllegalArgumentException("This depth format supports +Z and -Z orthographic rays.");
            }
            if (plans.containsKey(direction)) throw new IllegalArgumentException("Use each depth direction once.");
            var mask = masks[direction.axis.ordinal()];
            plans.put(direction, read(input, direction, hull, mask, cellM));
        }
        var proof = apply(List.copyOf(plans.values()), hull);
        if (allAxes) {
            return proof.put("registration", new JSONObject()
                    .put("x", "rows -Y, columns -Z")
                    .put("y", "rows +Z, columns +X")
                    .put("z", "rows -Y, columns +X"));
        }
        return proof.put("registration", "world-x-positive-columns");
    }

    private static Plan read(JSONObject input, Direction direction, boolean[][][] hull, boolean[][] silhouette, double cellM) {
        SpatialScene.keys(input, "axis", "origin", "unit", "samples");
        if (!input.getString("origin").equals(direction.origin) || !input.getString("unit").equals("um")) {
            throw new IllegalArgumentException("Register depth at its facing grid plane in integer micrometres.");
        }
        JSONArray values = input.getJSONArray("samples");
        int n = hull.length;
        if (values.length() != n * n) throw new IllegalArgumentException("Provide one world-ordered depth sample per silhouette cell.");
        double stepUm = cellM * 1_000_000;
        int unknown = 0, misses = 0;
        double maxResidual = 0;
        var hits = new ArrayList<Hit>();
        for (int row = 0; row < n; row++) {
            for (int column = 0; column < n; column++) {
                var value = values.get(row * n + column);
                if (!(value instanceof Number number)) throw new IllegalArgumentException("Use integer depth samples.");
                double raw = number.doubleValue();
                if (!Double.isFinite(raw) || raw != Math.rint(raw) || raw < UNOBSERVED || raw >= n * stepUm) {
                    throw new IllegalArgumentException("Depth sample is outside the search cube.");
                }
                if (raw == UNOBSERVED) {
                    unknown++;
                    continue;
                }
                if (raw == NO_HIT) {
                    if (silhouette[row][column]) throw new IllegalArgumentException("Depth declares empty space at a silhouette foreground pixel.");
                    misses++;
                    continue;
                }
                int face = (int) Math.round(raw / stepUm);
                double residual = Math.abs(raw - face * stepUm);
                if (face <= 0 || face >= n || residual > GRID_TOLERANCE_UM) {
                    throw new IllegalArgumentException("Depth does not align with a supported interior grid face.");
                }
                int depth = direction.voxel(n, face);
                var cell = direction.axis.cell(n, row, column, depth);
                if (!silhouette[row][column] || !hull[cell.x][cell.y][cell.z]) {
                    throw new IllegalArgumentException("Depth hit is outside the silhouette hull.");
                }
                hits.add(new Hit(row, column, depth));
                maxResidual = Math.max(maxResidual, residual);
            }
        }
        if (hits.isEmpty()) throw new IllegalArgumentException("Provide at least one observed first-surface depth in each supplied grid.");
        var proof = new JSONObject().put("axis", direction.label).put("origin", direction.origin).put("unit", "um")
                .put("samplesSha256", WorldFrames.hash(input)).put("measuredRays", hits.size()).put("unobservedRays", unknown)
                .put("backgroundRays", misses).put("gridFit", "nearest-face")
                .put("maxInputGridResidualUm", maxResidual).put("inputGridToleranceUm", GRID_TOLERANCE_UM);
        return new Plan(direction, hits, proof);
    }

    private static JSONObject apply(List<Plan> plans, boolean[][][] hull) {
        int n = hull.length;
        var free = new boolean[n][n][n];
        var evidence = new JSONArray();
        for (var plan : plans) {
            var direction = plan.direction;
            int towardCamera = direction.positive ? 1 : -1;
            int supportedRemovals = 0;
            for (var hit : plan.hits) {
                for (int depth = hit.depth + towardCamera; depth >= 0 && depth < n; depth += towardCamera) {
                    var cell = direction.axis.cell(n, hit.row, hit.column, depth);
                    if (!hull[cell.x][cell.y][cell.z]) continue;
                    free[cell.x][cell.y][cell.z] = true;
                    supportedRemovals++;
                }
            }
            evidence.put(plan.proof.put("removedVoxels", supportedRemovals));
        }
        for (var plan : plans) {
            for (var hit : plan.hits) {
                var cell = plan.direction.axis.cell(n, hit.row, hit.column, hit.depth);
                if (free[cell.x][cell.y][cell.z]) {
                    throw new IllegalArgumentException("Conflicting depth: observed free space intersects a first hit at grid cell "
                            + cell.x + ", " + cell.y + ", " + cell.z + " (" + plan.direction.label + ").");
                }
            }
        }
        int total = 0;
        for (int x = 0; x < n; x++) {
            for (int y = 0; y < n; y++) {
                for (int z = 0; z < n; z++) {
                    if (!free[x][y][z]) continue;
                    hull[x][y][z] = false;
                    total++;
                }
            }
        }
        return new JSONObject().put("views", evidence).put("removedVoxels", total)
                .put("rules", "Validate all hits against the same hull before applying the combined free space.");
    }
}
