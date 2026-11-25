
package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.Color;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Base64;

/** Fits registered orthographic views and scores a separately captured projection. */
final class BrowserViewTrace {
    static final String FORMAT = "fraynix.browser-registered-views-v2";
    static final String POLICY = "browser-registered-orthographic";
    static final String FORMAT_LIBRARY = "fraynix.library-registered-views-v1";
    static final String FORMAT_ELEVATED = "fraynix.browser-registered-views-v3";
    private static final double MIN_FIT = .86;
    private static final double MIN_HELD_OUT = .86;
    private static final double DIAGONAL = Math.sqrt(.5);

    private record Capture(int side, boolean[] mask, int left, int right, int top, int bottom, JSONObject receipt) {}
    private record Model(SpatialVisualMesh mesh, int grid, int[] low, int[] high, double cellM, double front, double side, Double top) {}

    private BrowserViewTrace() {}

    static SpatialReconstruction.Result build(byte[] bytes, JSONObject input) throws Exception {
        return build(bytes, input, false);
    }

    static SpatialReconstruction.Result build(byte[] bytes, JSONObject input, boolean library) throws Exception {
        boolean extended = library || FORMAT_ELEVATED.equals(input.getString("format"));
        int elevation = 0;
        if (extended) {
            if (library) {
                if (!FORMAT_LIBRARY.equals(input.getString("format"))) throw new IllegalArgumentException("Invalid registered library format.");
                SpatialScene.keys(input, "format", "name", "widthM", "views", "heldOutElevationDeg");
            } else SpatialScene.keys(input, "format", "widthM", "views", "heldOutElevationDeg");
            double value = SpatialScene.number(input, "heldOutElevationDeg", 0, 60);
            if (value != 0 && value != 60) throw new IllegalArgumentException("Choose a level or 60° elevated witness.");
            elevation = (int) value;
        } else SpatialScene.keys(input, "format", "widthM", "views");
        double width = SpatialScene.number(input, "widthM", .10, 5);
        var views = input.getJSONObject("views");
        if (extended) SpatialScene.keys(views, "front", "side", "top", "heldOut");
        else SpatialScene.keys(views, "front", "side", "heldOut");
        var front = capture(views.getJSONObject("front"), library);
        var side = capture(views.getJSONObject("side"), library);
        register(front, side);
        var top = views.has("top") ? capture(views.getJSONObject("top"), library) : null;
        if (top != null) registerTop(front, side, top);
        var attempts = new JSONArray();
        Model best = null;
        for (int grid : new int[]{16, 20, 22}) {
            try {
                var model = fit(front, side, top, grid, width);
                var attempt = new JSONObject().put("grid", grid).put("frontIou", model.front).put("sideIou", model.side);
                if (top != null) attempt.put("topIou", model.top);
                attempts.put(attempt);
                if (best == null || score(model) > score(best)) best = model;
            } catch (IllegalArgumentException error) {
                attempts.put(new JSONObject().put("grid", grid).put("refused", error.getMessage()));
            }
        }
        if (best == null || score(best) < MIN_FIT) {
            throw new IllegalArgumentException("The registered fitting views could not be reproduced at the supported resolutions.");
        }

        // The witness enters the pipeline only after model selection is finished.
        var witness = capture(views.getJSONObject("heldOut"), library);
        if (elevation == 0) register(front, witness);
        else if (front.side != witness.side) throw new IllegalArgumentException("The held-out crop must use the same size and scale.");
        if (samePixels(witness, front, library) || samePixels(witness, side, library) || (top != null && samePixels(witness, top, library))) {
            throw new IllegalArgumentException("Choose a separate 45° held-out view.");
        }
        double verification = overlap(witness.mask, renderOblique(best, witness.side, elevation));
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
        if (extended) proof.put("heldOutElevationDeg", elevation);
        if (top != null) {
            proof.getJSONObject("views").put("top", top.receipt);
            proof.put("topIou", best.top)
                    .put("scope", "Declared registration and front width. Front, side and top fit the mesh. A separate oblique view tests it.");
        }
        var definition = definition(best.mesh, hash, width, dimensions);
        if (!library && proof.getJSONObject("views").keySet().stream()
                .anyMatch(role->proof.getJSONObject("views").getJSONObject(role).has("fileName"))) {
            definition.put("title","Registered image prop");
            definition.getJSONArray("objects").getJSONObject(1).put("label","Registered image model");
            definition.getJSONObject("source").put("note","Registered image set "+hash
                    +". Declared scale and enclosing physics proxy. Separate oblique view checked.");
        }
        if (library) {
            definition.put("title", SpatialScene.text(input, "name", 70));
            definition.getJSONArray("objects").getJSONObject(1).put("label", "Reconstructed prop");
            definition.getJSONObject("source").put("note", "Registered library originals SHA-256 " + hash
                    + ". Width declared. Visual mesh and enclosing collider are separate.");
            proof.put("viewSource", "sprite-library");
        }
        return new SpatialReconstruction.Result(definition, proof);
    }

    static JSONObject verifyEdit(byte[] originals, JSONObject proof, JSONObject capture, int elevation,
                                 SpatialVisualMesh edit, double[] centreShift) throws Exception {
        var views = new JSONObject(new String(originals, java.nio.charset.StandardCharsets.UTF_8))
                .getJSONObject("views");
        if (elevation == proof.optInt("heldOutElevationDeg", 0))
            throw new IllegalArgumentException("Pick the other registered elevation for a new view of the edit.");
        var witness = capture(capture, false);
        var front = capture(views.getJSONObject("front"), false);
        if (front.side != witness.side)
            throw new IllegalArgumentException("Keep the new crop the same size and scale as the original views.");
        var side = capture(views.getJSONObject("side"), false);
        var top = views.has("top") ? capture(views.getJSONObject("top"), false) : null;
        for (String role : views.keySet()) {
            var old = capture(views.getJSONObject(role), false);
            if (Arrays.equals(old.mask, witness.mask))
                throw new IllegalArgumentException("Pick a fresh witness showing the edited shape.");
        }
        // Rebuild the exact original registration. The new witness never changes the fit or scale.
        var baseline = fit(front, side, top, proof.getInt("grid"), proof.getDouble("declaredWidthM"));
        if (!baseline.mesh.key().equals(proof.getString("meshSha256")))
            throw new IllegalStateException("The original mesh no longer matches its registered views.");
        double originalScore = overlap(witness.mask,
                renderOblique(baseline, baseline.mesh, new double[3], witness.side, elevation, true));
        double editedScore = overlap(witness.mask,
                renderOblique(baseline, edit, centreShift, witness.side, elevation, true));
        if (editedScore < MIN_HELD_OUT || editedScore - originalScore < .02)
            throw new IllegalArgumentException(String.format(java.util.Locale.ROOT,
                    "The new view did not verify the edit (%.1f%% edited, %.1f%% original). "
                            + "Require 86%% overlap and a 2-point improvement.",
                    editedScore * 100, originalScore * 100));
        return new JSONObject().put("heldOutIou", editedScore).put("originalIou", originalScore)
                .put("minimumOverlap", MIN_HELD_OUT).put("minimumGain", .02).put("elevationDeg", elevation)
                .put("witness", new JSONObject(witness.receipt, "tag", "pixels", "pngSha256"));
    }

    private static Capture capture(JSONObject input, boolean library) throws Exception {
        JSONObject receipt;
        if (library) {
            SpatialScene.keys(input, "assetId", "label", "png", "pngSha256", "sourceSha256", "spriteSha256");
            receipt = new JSONObject(input, "assetId", "label", "pngSha256", "sourceSha256", "spriteSha256");
        } else if (input.has("fileName")) {
            SpatialScene.keys(input,"fileName","png");
            String name = SpatialScene.text(input,"fileName",160);
            if (name.contains("/") || name.contains("\\") || name.chars().anyMatch(Character::isISOControl)
                    || !name.toLowerCase(java.util.Locale.ROOT).endsWith(".png"))
                throw new IllegalArgumentException("Choose a local PNG filename without a path.");
            receipt = new JSONObject().put("fileName",name).put("tag","img");
        } else {
            SpatialScene.keys(input, "url", "tag", "png");
            String url = BrowserShapeTrace.sourceUrl(input);
            String tag = SpatialScene.text(input, "tag", 8);
            if (!java.util.Set.of("img", "svg", "canvas").contains(tag)) throw new IllegalArgumentException("Pick a browser graphic.");
            receipt = new JSONObject().put("url", url).put("tag", tag);
        }
        byte[] png = Base64.getDecoder().decode(input.getString("png"));
        var image = BrowserShapeTrace.decode(png);
        int side = image.getWidth();
        if (image.getHeight() != side || side < 40 || side > 512) {
            throw new IllegalArgumentException(library ? "Use equal square originals, 40–512 pixels per side."
                    : "Use equal square browser crops, 40–512 pixels per side.");
        }
        var mask = library ? libraryMask(image) : BrowserShapeTrace.isolate(image);
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
        String digest = MapDefinition.sha(png);
        if (library && !digest.equals(receipt.getString("pngSha256")))
            throw new IllegalStateException("The library source pixels changed.");
        return new Capture(side, mask, left, right, top, bottom, receipt.put("pixels", side).put("pngSha256", digest));
    }

    private static boolean[] libraryMask(BufferedImage image) {
        int width = image.getWidth(), height = image.getHeight();
        int[] rgba = image.getRGB(0, 0, width, height, null, 0, width);
        if (java.util.Arrays.stream(rgba).noneMatch(pixel -> pixel >>> 24 < 255))
            return BrowserShapeTrace.isolate(image);
        boolean[] mask = new boolean[rgba.length];
        int area = 0;
        for (int i = 0; i < rgba.length; i++) {
            mask[i] = (rgba[i] >>> 24) >= 128;
            if (mask[i]) area++;
        }
        if (area < 64 || area > rgba.length * .85)
            throw new IllegalArgumentException("Choose a registered alpha silhouette with clear padding.");
        return mask;
    }

    private static void register(Capture known, Capture other) {
        if (known.side != other.side || Math.abs(known.top - other.top) > 2 || Math.abs(known.bottom - other.bottom) > 2) {
            throw new IllegalArgumentException("Keep crop size, scale, center and vertical framing fixed across all views.");
        }
    }

    private static boolean samePixels(Capture a, Capture b, boolean library) {
        return a.receipt.getString("pngSha256").equals(b.receipt.getString("pngSha256"))
                || (library && java.util.Arrays.equals(a.mask, b.mask));
    }

    private static void registerTop(Capture front, Capture side, Capture top) {
        if (front.side != top.side || Math.abs(front.left - top.left) > 2 || Math.abs(front.right - top.right) > 2
                || Math.abs(side.left - top.top) > 2 || Math.abs(side.right - top.bottom) > 2) {
            throw new IllegalArgumentException("Align the top crop to the same scale and center, with the front edge at the bottom.");
        }
    }

    private static double score(Model model) {
        double horizontal = Math.min(model.front, model.side);
        return model.top == null ? horizontal : Math.min(horizontal, model.top);
    }

    private static Model fit(Capture front, Capture side, Capture top, int grid, double width) {
        var a = raster(front, grid);
        var b = raster(side, grid);
        var c = top == null ? null : raster(top, grid);
        var topFit = top == null ? null : new boolean[grid][grid];
        var hull = new boolean[grid][grid][grid];
        var frontFit = new boolean[grid][grid];
        var sideFit = new boolean[grid][grid];
        int[] low = {grid, grid, grid}, high = {0, 0, 0};
        for (int x = 0; x < grid; x++) for (int y = 0; y < grid; y++) for (int z = 0; z < grid; z++) {
            int topRow = grid - 1 - z;
            if (!a[y][x] || !b[y][z] || (c != null && !c[topRow][x])) continue;
            if (topFit != null) topFit[topRow][x] = true;
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
                overlap(side.mask, enlarge(sideFit, side.side)),
                top == null ? null : overlap(top.mask, enlarge(topFit, top.side)));
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

    private static boolean[] renderOblique(Model model, int size, int elevation) {
        return renderOblique(model, model.mesh, new double[3], size, elevation, false);
    }

    private static boolean[] renderOblique(Model model, SpatialVisualMesh mesh, double[] shift,
                                            int size, int elevation, boolean keepFrame) {
        // Elevated silhouettes contain diagonal edges in both axes. Sample within each source pixel.
        int samples = elevation == 0 ? 1 : 3;
        int rasterSize = size * samples;
        var pixels = new BufferedImage(rasterSize, rasterSize, BufferedImage.TYPE_BYTE_BINARY);
        var graphics = pixels.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            var xy = new double[mesh.vertexCount()][2];
            double offsetX = (model.low[0] + model.high[0] - model.grid) / 2.0;
            double offsetY = (model.low[1] + model.high[1] - model.grid) / 2.0;
            double offsetZ = (model.low[2] + model.high[2] - model.grid) / 2.0;
            double pixelsPerGrid = rasterSize / (double) model.grid;
            double radians = Math.toRadians(elevation);
            double up = Math.cos(radians), away = Math.sin(radians);
            for (int i = 0; i < xy.length; i++) {
                double x = (mesh.positions()[i * 3] + shift[0]) / model.cellM + offsetX;
                double y = (mesh.positions()[i * 3 + 1] + shift[1]) / model.cellM + offsetY;
                double z = (mesh.positions()[i * 3 + 2] + shift[2]) / model.cellM + offsetZ;
                xy[i][0] = rasterSize / 2.0 + (x + z) * DIAGONAL * pixelsPerGrid;
                xy[i][1] = rasterSize / 2.0 - (y * up - (z - x) * DIAGONAL * away) * pixelsPerGrid;
                if (keepFrame && (xy[i][0] < samples || xy[i][1] < samples
                        || xy[i][0] > rasterSize - samples || xy[i][1] > rasterSize - samples))
                    throw new IllegalArgumentException("Keep the full model inside the registered witness frame.");
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
        for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
            int covered = 0;
            for (int row = 0; row < samples; row++) for (int col = 0; col < samples; col++) {
                if (pixels.getRGB(x * samples + col, y * samples + row) == -1) covered++;
            }
            out[y * size + x] = covered * 2 >= samples * samples;
        }
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
