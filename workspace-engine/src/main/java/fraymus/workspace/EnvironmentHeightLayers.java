package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static fraymus.workspace.EnvironmentFloorPlan.Grid;
import static fraymus.workspace.EnvironmentFloorPlan.Plan;
import static fraymus.workspace.EnvironmentFloorPlan.Section;

/** Compares a reference section with two witness sections per height band before rebuilding. */
final class EnvironmentHeightLayers {
    private static final String FORMAT = "fraynix.height-layers-v1";
    private static final int MAX_BANDS = 6;
    private static final int MAX_SOLIDS = 2_048;
    private static final double[] SAMPLE_FRACTIONS = {.25, .5, .75};
    private final EnvironmentStore environments;
    private final Path receipts;

    EnvironmentHeightLayers(Path project, EnvironmentStore environments) {
        this.environments = environments;
        receipts = project.resolve("environments").resolve("height-layers");
    }

    private record Layer(double bottom, double top, double[] heights, List<Section> samples,
                         Plan solid, Plan varying, int openEdges) {
        JSONObject json() {
            var witnesses = new JSONArray();
            for (int i = 0; i < heights.length; i++) {
                Section sample = samples.get(i);
                witnesses.put(new JSONObject().put("heightM", heights[i]).put("role", i == 1 ? "reference" : "witness")
                        .put("filledCells", sample.plan().occupied()).put("planSha256", WorldFrames.hash(sample.plan().json()))
                        .put("openEdges", sample.open().size()));
            }
            return new JSONObject().put("bottomM", bottom).put("topM", top).put("solidCells", solid.occupied())
                    .put("varyingCells", varying.occupied()).put("openEdgesAcrossSamples", openEdges)
                    .put("solidRows", solid.json().getJSONArray("rows"))
                    .put("varyingRows", varying.json().getJSONArray("rows")).put("samples", witnesses);
        }
    }

    private record Audit(String sourceId, Grid grid, List<Layer> layers, JSONObject report) {
        String id() { return WorldFrames.hash(report); }

        JSONObject view(boolean preview) {
            JSONObject result = new JSONObject(report.toString()).put("auditId", id());
            if (preview) {
                var images = new JSONArray();
                Grid frame = diagramFrame(grid, layers);
                for (Layer layer : layers)
                    images.put(EnvironmentFloorPlan.layerSvg(layer.solid, layer.varying, frame));
                result.put("diagrams", images);
            }
            return result;
        }
    }

    synchronized JSONObject analyze(JSONObject request) throws Exception {
        var audit = calculate(request);
        // Prepare the actual preview before making its durable receipt visible.
        JSONObject view = audit.view(true);
        var receipt = new JSONObject().put("auditId", audit.id()).put("sourceId", audit.sourceId)
                .put("boundsM", audit.report.getJSONArray("boundsM")).put("cellM", audit.report.getDouble("requestedCellM"));
        AgentFiles042.saveChecked(path(audit.id()), receipt);
        SpriteWorkshop.replace(latest(audit.sourceId), audit.id().getBytes(StandardCharsets.US_ASCII));
        return view;
    }

    synchronized JSONObject read(JSONObject input) throws Exception {
        SpatialScene.keys(input, "auditId", "sourceId", "preview");
        if (input.has("auditId")) return restore(input.getString("auditId")).view(input.optBoolean("preview"));
        String sourceId = input.getString("sourceId");
        Path latest = latest(sourceId);
        if (!Files.exists(latest, LinkOption.NOFOLLOW_LINKS)) return new JSONObject();
        Audit audit = restore(new String(SpriteWorkshop.read(latest, 64), StandardCharsets.US_ASCII));
        require(sourceId.equals(audit.sourceId), "The saved height audit belongs to another source.");
        return audit.view(input.optBoolean("preview"));
    }

    synchronized JSONObject rebuild(JSONObject input) throws Exception {
        SpatialScene.keys(input, "auditId", "omitVarying");
        Audit audit = restore(input.getString("auditId"));
        int varying = audit.report.getInt("varyingBandCells");
        require(varying == 0 || Boolean.TRUE.equals(input.opt("omitVarying")),
                "The source changes inside these bands. Split the bands, or explicitly rebuild only the agreed cells.");
        var nodes = new JSONArray();
        Grid grid = audit.grid;
        int layerNum = 0;
        for (Layer layer : audit.layers) {
            layerNum++;
            for (var wall : EnvironmentFloorPlan.merge(layer.solid)) {
                var span = wall.span();
                nodes.put(EnvironmentFloorPlan.box("Band " + layerNum + " solid " + (nodes.length() + 1),
                        grid.x((span.start() + span.end()) / 2.0), (layer.bottom + layer.top) / 2,
                        grid.z((wall.firstRow() + wall.endRow()) / 2.0), (span.end() - span.start()) * grid.cell(),
                        layer.top - layer.bottom, (wall.endRow() - wall.firstRow()) * grid.cell(), 1));
                require(nodes.length() <= MAX_SOLIDS, "The rebuild needs more than 2,048 solids. Use fewer bands or a larger cell size.");
            }
        }
        require(!nodes.isEmpty(), "No filled cells agreed across their three cuts.");
        var link = new JSONObject().put("id", audit.sourceId).put("auditId", audit.id());
        byte[] mesh = SpatialGlb.environmentBoxes(nodes, grid.json(), audit.sourceId, link);
        var decoded = EnvironmentStore.parse(mesh).getJSONArray("nodes");
        int witnessCount = 0;
        for (Layer layer : audit.layers) for (double height : layer.heights) {
            var section = EnvironmentFloorPlan.measure(GlbSection.cut(mesh, decoded, height), grid);
            require(section.open().isEmpty() && Arrays.equals(layer.solid.walls(), section.plan().walls()),
                    "The rebuilt mesh differs from the agreed cells at a witness height.");
            witnessCount++;
        }
        var stored = environments.importFile(new JSONObject().put("filename", "height-layer-rebuild.glb")
                .put("base64", Base64.getEncoder().encodeToString(mesh)));
        return new JSONObject().put("id", stored.getString("id")).put("sourceId", audit.sourceId)
                .put("auditId", audit.id()).put("witnessCuts", witnessCount).put("solidMeshes", nodes.length())
                .put("omittedBandCells", varying);
    }

    private Audit restore(String id) throws Exception {
        Path file = path(id);
        require(Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS), "This height audit is unavailable in the local project.");
        JSONObject record = AgentFiles042.loadChecked(file);
        require(record.getString("auditId").equals(id), "The saved height audit identity changed.");
        Audit audit = calculate(new JSONObject().put("id", record.getString("sourceId"))
                .put("boundsM", record.getJSONArray("boundsM")).put("cellM", record.getDouble("cellM")));
        require(audit.id().equals(id), "The saved height audit changed on replay.");
        return audit;
    }

    private Audit calculate(JSONObject request) throws Exception {
        SpatialScene.keys(request, "id", "boundsM", "cellM");
        JSONArray bounds = request.getJSONArray("boundsM");
        double[] elevations = elevations(bounds);
        double pitch = SpatialScene.number(request, "cellM", .002, 2);
        var pin = new JSONObject().put("id", request.getString("id"));
        var retained = environments.open(pin, true);
        require(retained.has("id"), "Open an environment to measure height layers.");
        require(retained.getInt("triangleCount") <= 250_000, "Height layers support up to 250,000 source triangles.");
        String sourceId = retained.getString("id");
        byte[] original = SpriteWorkshop.decode(retained.getString("base64"), EnvironmentStore.MAX_BYTES);
        JSONArray nodes = retained.getJSONArray("nodes");
        var raw = new ArrayList<List<GlbSection.Result>>();
        Map<Integer, Set<GlbSection.Line>> allEdges = new LinkedHashMap<>();
        JSONObject gridHint = null;
        for (int band = 0; band < elevations.length - 1; band++) {
            var cuts = new ArrayList<GlbSection.Result>();
            for (double height : samples(elevations[band], elevations[band + 1])) {
                var cut = GlbSection.cut(original, nodes, height);
                cuts.add(cut);
                if (gridHint == null) gridHint = cut.grid();
                for (var lines : cut.lines().values()) allEdges.put(allEdges.size(), lines);
            }
            raw.add(cuts);
        }
        require(!allEdges.isEmpty(), "No source geometry crosses the sampled heights.");
        // Use whole-view bounds from all cuts, so coordinates refer to the same cell at every height.
        Grid grid = gridHint == null ? EnvironmentFloorPlan.fit(allEdges, pitch) : Grid.read(gridHint);
        if (gridHint != null) for (var lines : allEdges.values()) for (var line : lines)
            require(EnvironmentFloorPlan.onGrid(grid, line.a()) && EnvironmentFloorPlan.onGrid(grid, line.b()),
                    "The saved floor-plan grid does not cover all the sampled cuts.");
        var layers = new ArrayList<Layer>();
        var bands = new JSONArray();
        int varyingCells = 0, solidCells = 0;
        for (int band = 0; band < raw.size(); band++) {
            List<Section> slices = raw.get(band).stream().map(cut -> EnvironmentFloorPlan.measure(cut, grid)).toList();
            Layer details = combine(grid, elevations[band], elevations[band + 1], slices);
            layers.add(details);
            bands.put(details.json());
            varyingCells += details.varying.occupied();
            solidCells += details.solid.occupied();
        }
        var report = new JSONObject().put("format", FORMAT).put("sourceId", sourceId)
                .put("boundsM", new JSONArray(elevations)).put("requestedCellM", pitch).put("grid", grid.json())
                .put("solidBandCells", solidCells).put("varyingBandCells", varyingCells).put("bands", bands)
                .put("scope", "Agreement at three samples per band. Thin or unsampled features and open surfaces remain unresolved.");
        return new Audit(sourceId, grid, layers, report);
    }

    private static Grid diagramFrame(Grid grid, List<Layer> layers) {
        int left = grid.width(), right = 0, top = grid.depth(), bottom = 0;
        for (Layer layer : layers) for (Plan plan : List.of(layer.solid, layer.varying)) {
            for (int row = 0; row < grid.depth(); row++) {
                BitSet cells = plan.walls()[row];
                if (cells.isEmpty()) continue;
                left = Math.min(left, cells.nextSetBit(0));
                right = Math.max(right, cells.length());
                top = Math.min(top, row);
                bottom = Math.max(bottom, row + 1);
            }
        }
        // Keep every diagram aligned and fit the measured solid and changing cells.
        return right > left ? new Grid(right - left, bottom - top, grid.x(left), grid.z(top), grid.cell()) : grid;
    }

    private static Layer combine(Grid grid, double bottom, double top, List<Section> cuts) {
        var solid = EnvironmentFloorPlan.empty(grid);
        var varying = EnvironmentFloorPlan.empty(grid);
        for (int row = 0; row < grid.depth(); row++) {
            BitSet a = cuts.get(0).plan().walls()[row], b = cuts.get(1).plan().walls()[row], c = cuts.get(2).plan().walls()[row];
            solid[row].or(a);
            solid[row].and(b);
            solid[row].and(c);
            varying[row].or(a);
            varying[row].or(b);
            varying[row].or(c);
            varying[row].andNot(solid[row]);
        }
        int openEdges = cuts.stream().mapToInt(cut -> cut.open().size()).sum();
        return new Layer(bottom, top, samples(bottom, top), cuts, new Plan(grid, solid), new Plan(grid, varying), openEdges);
    }

    private static double[] elevations(JSONArray bounds) {
        require(bounds.length() >= 2 && bounds.length() <= MAX_BANDS + 1, "Enter two to seven height boundaries in metres.");
        var heights = new double[bounds.length()];
        for (int i = 0; i < heights.length; i++) {
            Object raw = bounds.get(i);
            require(raw instanceof Number, "Use numeric height boundaries in metres.");
            double value = ((Number)raw).doubleValue();
            require(Double.isFinite(value) && Math.abs(value) <= 100_000 && (i == 0 || value - heights[i - 1] >= .029999999),
                    "Height boundaries must rise by at least 0.03 m.");
            heights[i] = value;
        }
        return heights;
    }

    private static double[] samples(double bottom, double top) {
        return Arrays.stream(SAMPLE_FRACTIONS).map(fraction -> bottom + (top - bottom) * fraction).toArray();
    }

    private Path latest(String sourceId) {
        require(sourceId.matches("[a-f0-9]{64}"), "Choose a valid source environment.");
        return receipts.resolve(sourceId + ".latest");
    }

    private Path path(String id) {
        require(id.matches("[a-f0-9]{64}"), "Choose a valid height audit.");
        return receipts.resolve(id + ".json");
    }

    private static void require(boolean accepted, String message) {
        if (!accepted) throw new IllegalArgumentException(message);
    }
}
