
package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.Color;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.Base64;

/** Fits two registered horizontal orthographic views and scores an untouched third view. */
final class BrowserViewTrace {
    static final String FORMAT = "fraynix.browser-registered-views-v2";
    static final String POLICY = "browser-registered-orthographic";
    private static final double MIN_FIT = .86;
    private static final double MIN_HELD_OUT = .86;
    private static final double DIAGONAL = Math.sqrt(.5);

    private record Capture(int side, boolean[] mask, int left, int right, int top, int bottom, JSONObject receipt) {}
    private record Model(SpatialVisualMesh mesh, int grid, int[] low, int[] high, double cellM, double front, double side) {}

    private BrowserViewTrace() {}

    static SpatialReconstruction.Result build(byte[] bytes, JSONObject input) throws Exception {
        SpatialScene.keys(input, "format", "widthM", "views");
        double width = SpatialScene.number(input, "widthM", .10, 5);
        var views = input.getJSONObject("views");
        SpatialScene.keys(views, "front", "side", "heldOut");
        var front = capture(views.getJSONObject("front"));
        var side = capture(views.getJSONObject("side"));
        register(front, side);
        var attempts = new JSONArray();
        Model best = null;
        for (int grid : new int[]{16, 20, 22}) {
            try {
                var model = fit(front, side, grid, width);
                attempts.put(new JSONObject().put("grid", grid).put("frontIou", model.front).put("sideIou", model.side));
                if (best == null || score(model) > score(best)) best = model;
            } catch (IllegalArgumentException error) {
                attempts.put(new JSONObject().put("grid", grid).put("refused", error.getMessage()));
            }
        }
        if (best == null || score(best) < MIN_FIT) {
            throw new IllegalArgumentException("The registered front and side could not be fit at the supported resolutions.");
        }

        // The third image enters the pipeline only after model selection is finished.
        var witness = capture(views.getJSONObject("heldOut"));
        register(front, witness);
        if (witness.receipt.getString("pngSha256").equals(front.receipt.getString("pngSha256"))
                || witness.receipt.getString("pngSha256").equals(side.receipt.getString("pngSha256"))) {
            throw new IllegalArgumentException("Choose a separate 45° held-out view.");
        }
        double verification = overlap(witness.mask, renderOblique(best, witness.side));
        if (verification < MIN_HELD_OUT) {
            throw new IllegalArgumentException(String.format(java.util.Locale.ROOT,
                    "Held-out view refused the fitted model (%.1f%% overlap, need %.0f%%).", verification * 100, MIN_HELD_OUT * 100));
        }
        double depth = width * (side.right - side.left + 1) / (front.right - front.left + 1);
        double[] dimensions = new double[3];
        for (int i = 0; i < dimensions.length; i++) dimensions[i] = (best.high[i] - best.low[i]) * best.cellM;
        String hash = MapDefinition.sha(bytes);
        var proof = new JSONObject().put("policy", POLICY).put("sourceSha256", hash)
                .put("meshSha256", best.mesh.key()).put("triangles", best.mesh.indices().length / 3)
                .put("grid", best.grid).put("cellM", best.cellM).put("candidateFits", attempts)
                .put("frontIou", best.front).put("sideIou", best.side)
                .put("heldOutIou", verification).put("heldOutMinimum", MIN_HELD_OUT)
                .put("declaredWidthM", width).put("estimatedDepthM", depth).put("meshDepthM", dimensions[2])
                .put("depthResolutionM", best.cellM)
                .put("views", new JSONObject().put("front", front.receipt).put("side", side.receipt).put("heldOut", witness.receipt))
                .put("scope", "Declared registration and front width. The side estimates depth. The oblique silhouette verifies the fitted mesh.");
        return new SpatialReconstruction.Result(definition(best.mesh, hash, width, dimensions), proof);
    }

    private static Capture capture(JSONObject input) throws Exception {
        SpatialScene.keys(input, "url", "tag", "png");
        String url = BrowserShapeTrace.sourceUrl(input);
        String tag = SpatialScene.text(input, "tag", 8);
        if (!java.util.Set.of("img", "svg", "canvas").contains(tag)) throw new IllegalArgumentException("Pick a browser graphic.");
        byte[] png = Base64.getDecoder().decode(input.getString("png"));
        var image = BrowserShapeTrace.decode(png);
        int side = image.getWidth();
        if (image.getHeight() != side || side < 40 || side > 512) {
            throw new IllegalArgumentException("Use equal square browser crops, 40–512 pixels per side.");
        }
        var mask = BrowserShapeTrace.isolate(image);
        int left = side, right = -1, top = side, bottom = -1;
        for (int i = 0; i < mask.length; i++) {
            if (!mask[i]) continue;
            int x = i % side, y = i / side;
            left = Math.min(left, x); right = Math.max(right, x);
            top = Math.min(top, y); bottom = Math.max(bottom, y);
        }
        if (left <= 0 || top <= 0 || right >= side - 1 || bottom >= side - 1) {
            throw new IllegalArgumentException("Keep the entire silhouette and its background margin in view.");
        }
        return new Capture(side, mask, left, right, top, bottom, new JSONObject()
                .put("url", url).put("tag", tag).put("pixels", side).put("pngSha256", MapDefinition.sha(png)));
    }

    private static void register(Capture known, Capture other) {
        if (known.side != other.side || Math.abs(known.top - other.top) > 2 || Math.abs(known.bottom - other.bottom) > 2) {
            throw new IllegalArgumentException("Keep crop size, scale, center and vertical framing fixed across all views.");
        }
    }

    private static double score(Model model) { return Math.min(model.front, model.side); }

    private static Model fit(Capture front, Capture side, int grid, double width) {
        var a = raster(front, grid);
        var b = raster(side, grid);
        var hull = new boolean[grid][grid][grid];
        var frontFit = new boolean[grid][grid];
        var sideFit = new boolean[grid][grid];
        int[] low = {grid, grid, grid}, high = {0, 0, 0};
        for (int x = 0; x < grid; x++) for (int y = 0; y < grid; y++) for (int z = 0; z < grid; z++) {
            if (!a[y][x] || !b[y][z]) continue;
            hull[x][y][z] = true;
            frontFit[y][x] = true;
            sideFit[y][z] = true;
            low[0] = Math.min(low[0], x); low[1] = Math.min(low[1], y); low[2] = Math.min(low[2], z);
            high[0] = Math.max(high[0], x + 1); high[1] = Math.max(high[1], y + 1); high[2] = Math.max(high[2], z + 1);
        }
        if (high[0] - low[0] < 2 || high[1] <= low[1] || high[2] <= low[2]) {
            throw new IllegalArgumentException("The registered views have no usable common volume.");
        }
        double cell = width / (high[0] - low[0]);
        for (int i = 0; i < 3; i++) RampDefinition.range((high[i] - low[i]) * cell, .05, 20, "model size metres");
        var mesh = SpatialReconstruction.surface(hull, low, high, cell);
        SpatialFaceExtrusion.requireClosed(mesh);
        return new Model(mesh, grid, low, high, cell, overlap(front.mask, enlarge(frontFit, front.side)),
                overlap(side.mask, enlarge(sideFit, side.side)));
    }

    private static boolean[][] raster(Capture source, int grid) {
        int[][] count = new int[grid][grid], hit = new int[grid][grid];
        int size = source.side;
        for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
            int row = grid - 1 - y * grid / size, col = x * grid / size;
            count[row][col]++;
            if (source.mask[y * size + x]) hit[row][col]++;
        }
        boolean[][] out = new boolean[grid][grid];
        for (int y = 0; y < grid; y++) for (int x = 0; x < grid; x++) {
            out[y][x] = hit[y][x] > 0 && hit[y][x] * 2 >= count[y][x];
        }
        return out;
    }

    private static boolean[] enlarge(boolean[][] mask, int size) {
        int grid = mask.length;
        boolean[] out = new boolean[size * size];
        for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
            out[y * size + x] = mask[grid - 1 - y * grid / size][x * grid / size];
        }
        return out;
    }

    private static boolean[] renderOblique(Model model, int size) {
        var pixels = new BufferedImage(size, size, BufferedImage.TYPE_BYTE_BINARY);
        var graphics = pixels.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            var mesh = model.mesh;
            var xy = new double[mesh.vertexCount()][2];
            double offsetX = (model.low[0] + model.high[0] - model.grid) / 2.0;
            double offsetY = (model.low[1] + model.high[1] - model.grid) / 2.0;
            double offsetZ = (model.low[2] + model.high[2] - model.grid) / 2.0;
            double pixelsPerGrid = size / (double) model.grid;
            for (int i = 0; i < xy.length; i++) {
                double x = mesh.positions()[i * 3] / model.cellM + offsetX;
                double y = mesh.positions()[i * 3 + 1] / model.cellM + offsetY;
                double z = mesh.positions()[i * 3 + 2] / model.cellM + offsetZ;
                xy[i][0] = size / 2.0 + (x + z) * DIAGONAL * pixelsPerGrid;
                xy[i][1] = size / 2.0 - y * pixelsPerGrid;
            }
            var indices = mesh.indices();
            for (int i = 0; i < indices.length; i += 3) {
                var a = xy[indices[i]];
                var b = xy[indices[i + 1]];
                var c = xy[indices[i + 2]];
                var triangle = new Path2D.Double();
                triangle.moveTo(a[0], a[1]);
                triangle.lineTo(b[0], b[1]);
                triangle.lineTo(c[0], c[1]);
                triangle.closePath();
                graphics.fill(triangle);
            }
        } finally { graphics.dispose(); }
        boolean[] out = new boolean[size * size];
        for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) out[y * size + x] = pixels.getRGB(x, y) == -1;
        return out;
    }

    private static double overlap(boolean[] a, boolean[] b) {
        int common = 0, union = 0;
        for (int i = 0; i < a.length; i++) {
            if (a[i] && b[i]) common++;
            if (a[i] || b[i]) union++;
        }
        return common / (double) union;
    }

    private static JSONObject definition(SpatialVisualMesh mesh, String hash, double width, double[] size) {
        return new JSONObject().put("format", "fraynix.spatial-scene").put("version", 1).put("units", "m")
                .put("frame", "right-handed-y-up").put("title", "Browser front and side model").put("gravityMps2", 9.81)
                .put("scaleReference", new JSONObject().put("a", new JSONArray(new int[]{0,0,0}))
                        .put("b", new JSONArray(new double[]{width,0,0})).put("knownLengthM", width)
                        .put("toleranceM", 0).put("basis", "declared-test"))
                .put("source", new JSONObject().put("kind", "unverified").put("physicalProperties", "declared")
                        .put("note", "Registered browser capture SHA-256 " + hash + ". Width declared. Side estimates depth."))
                .put("visualMeshes", new JSONObject().put(mesh.key(), mesh.encoded()))
                .put("objects", new JSONArray().put(SpatialReconstruction.box("floor", "Support deck",
                        new double[]{0, -size[1] / 2 - .1, 0}, new double[]{size[0] * 1.5, .2, size[2] * 1.5}, 0))
                        .put(SpatialReconstruction.box("surface", "Browser model", new double[]{0,0,0}, size, 1)
                                .put("visualMesh", mesh.key())));
    }
}
