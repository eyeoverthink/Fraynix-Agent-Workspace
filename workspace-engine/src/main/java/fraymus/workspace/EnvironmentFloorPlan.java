package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static fraymus.workspace.GlbSection.Line;
import static fraymus.workspace.GlbSection.Point;

/** Metric wall-mask extrusion and independent horizontal mesh-section readback. */
final class EnvironmentFloorPlan {
    static final int MAX_SOURCE = 4 * 1024 * 1024;
    private static final int MAX_SIDE = 1024;
    private static final int MAX_WALLS = 2048;
    private static final String FORMAT = "fraynix.floor-plan-v1";

    private final EnvironmentStore environments;

    EnvironmentFloorPlan(EnvironmentStore environments) {
        this.environments = environments;
    }

    private record Grid(int width, int depth, double originX, double originZ, double cell) {
        JSONObject json() {
            return new JSONObject().put("width", width).put("depth", depth).put("originXM", originX)
                    .put("originZM", originZ).put("cellM", cell);
        }

        static Grid read(JSONObject json) {
            SpatialScene.keys(json, "width", "depth", "originXM", "originZM", "cellM");
            return new Grid(SpriteWorkshop.integer(json, "width", 1, MAX_SIDE),
                    SpriteWorkshop.integer(json, "depth", 1, MAX_SIDE),
                    SpatialScene.number(json, "originXM", -100_000, 100_000),
                    SpatialScene.number(json, "originZM", -100_000, 100_000),
                    SpatialScene.number(json, "cellM", .002, 2));
        }

        double x(double col) { return originX + col * cell; }
        double z(double row) { return originZ + row * cell; }
        int firstColumn(double x) { return (int)Math.ceil((x - originX) / cell - .5); }
        int firstRow(double z) { return (int)Math.ceil((z - originZ) / cell - .5); }
    }

    private record Plan(Grid grid, BitSet[] walls) {
        int occupied() { return Arrays.stream(walls).mapToInt(BitSet::cardinality).sum(); }

        JSONObject json() {
            var rows = new JSONArray();
            for (BitSet row : walls) {
                var spans = new JSONArray();
                for (int first = row.nextSetBit(0); first >= 0; first = row.nextSetBit(row.nextClearBit(first))) {
                    spans.put(first).put(row.nextClearBit(first));
                }
                rows.put(spans);
            }
            return new JSONObject().put("format", FORMAT).put("grid", grid.json()).put("rows", rows);
        }
    }

    private record Span(int start, int end) {}
    private record Wall(Span span, int firstRow, int endRow) {}
    private record Contours(Set<Line> closed, Set<Line> open) {}

    JSONObject build(JSONObject input) throws Exception {
        SpatialScene.keys(input, "filename", "base64", "widthM", "wallHeightM");
        double height = SpatialScene.number(input, "wallHeightM", .15, 20);
        String filename = SpatialScene.text(input, "filename", 140);
        String ext = filename.toLowerCase(Locale.ROOT);
        String base64 = input.getString("base64");
        byte[] source = SpriteWorkshop.decode(base64, MAX_SOURCE);
        Plan plan;
        if (ext.endsWith(".png")) {
            double width = SpatialScene.number(input, "widthM", .02, 500);
            plan = fromPng(source, width);
        } else if (ext.endsWith(".json")) {
            plan = fromJson(new JSONObject(new String(source, StandardCharsets.UTF_8)));
        } else {
            throw new IllegalArgumentException("Choose a wall-mask PNG or a Fraynix floor-plan JSON.");
        }
        int cells = plan.occupied();
        require(cells > 0, "The floor plan has no dark wall areas to raise.");
        require(cells < plan.grid.width * plan.grid.depth, "The floor plan is entirely solid. Leave open space in the mask.");
        var walls = merge(plan);
        var nodes = new JSONArray();
        Grid grid = plan.grid;
        nodes.put(box("Floor base", grid.x(grid.width / 2.0), -.06, grid.z(grid.depth / 2.0),
                grid.width * grid.cell, .12, grid.depth * grid.cell, 0));
        for (Wall wall : walls) {
            Span span = wall.span;
            nodes.put(box("Wall " + nodes.length(), grid.x((span.start + span.end) / 2.0), height / 2,
                    grid.z((wall.firstRow + wall.endRow) / 2.0), (span.end - span.start) * grid.cell,
                    height, (wall.endRow - wall.firstRow) * grid.cell, 1));
        }
        String sourceSha = MapDefinition.sha(source);
        byte[] glb = SpatialGlb.environmentBoxes(nodes, grid.json(), sourceSha);
        JSONObject checked = EnvironmentStore.parse(glb);
        Section measured = measure(GlbSection.cut(glb, checked.getJSONArray("nodes"), height / 2), grid.cell);
        require(measured.open.isEmpty() && grid.equals(measured.plan.grid) && Arrays.equals(plan.walls, measured.plan.walls),
                "The generated mesh did not reproduce every source wall cell.");
        var stored = environments.importFile(new JSONObject().put("filename", "floor-plan.glb")
                .put("base64", java.util.Base64.getEncoder().encodeToString(glb)));
        return new JSONObject().put("id", stored.getString("id")).put("sourceSha256", sourceSha)
                .put("wallHeightM", height).put("wallMeshes", walls.size()).put("verifiedCells", grid.width * grid.depth)
                .put("occupiedCells", cells).put("sectionHeightM", height / 2);
    }

    JSONObject section(JSONObject input) throws Exception {
        SpatialScene.keys(input, "id", "heightM", "cellM");
        double height = SpatialScene.number(input, "heightM", -100_000, 100_000);
        double cell = SpatialScene.number(input, "cellM", .002, 2);
        var request = new JSONObject();
        if (input.has("id")) request.put("id", input.getString("id"));
        var data = environments.open(request, true);
        require(data.has("id"), "Open a 3D environment first.");
        byte[] glb = SpriteWorkshop.decode(data.getString("base64"), EnvironmentStore.MAX_BYTES);
        var result = measure(GlbSection.cut(glb, data.getJSONArray("nodes"), height), cell);
        var plan = result.plan;
        return new JSONObject().put("id", data.getString("id")).put("heightM", height)
                .put("plan", plan.json()).put("closedEdges", result.closed.size()).put("openEdges", result.open.size())
                .put("occupiedCells", plan.occupied()).put("svg", svg(result));
    }

    private static Plan fromPng(byte[] png, double widthM) throws Exception {
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(png))) {
            var readers = ImageIO.getImageReaders(input);
            require(readers.hasNext(), "Choose a valid PNG wall mask.");
            var reader = readers.next();
            try {
                require(reader.getFormatName().equalsIgnoreCase("png"), "Choose a valid PNG wall mask.");
                reader.setInput(input, true, true);
                int width = reader.getWidth(0), depth = reader.getHeight(0);
                var grid = Grid.read(new Grid(width, depth, 0, 0, widthM / width).json());
                var image = reader.read(0);
                var walls = empty(grid);
                for (int z = 0; z < grid.depth; z++) for (int x = 0; x < grid.width; x++) {
                    int argb = image.getRGB(x, z);
                    int red = argb >>> 16 & 255, green = argb >>> 8 & 255, blue = argb & 255;
                    if (argb >>> 24 >= 128 && red * 2126 + green * 7152 + blue * 722 <= 1_270_000)
                        walls[z].set(x);
                }
                return new Plan(grid, walls);
            } finally {
                reader.dispose();
            }
        }
    }

    private static Plan fromJson(JSONObject json) {
        SpatialScene.keys(json, "format", "grid", "rows");
        require(FORMAT.equals(json.getString("format")), "Choose a Fraynix floor-plan JSON.");
        Grid grid = Grid.read(json.getJSONObject("grid"));
        var rows = json.getJSONArray("rows");
        require(rows.length() == grid.depth, "The floor plan row count differs from its grid.");
        var walls = empty(grid);
        for (int z = 0; z < rows.length(); z++) {
            JSONArray row = rows.getJSONArray(z);
            require(row.length() <= grid.width * 2 && row.length() % 2 == 0, "Invalid wall spans.");
            int previous = 0;
            for (int i = 0; i < row.length(); i += 2) {
                int start = bound(row.get(i), previous, grid.width);
                int end = bound(row.get(i + 1), start + 1, grid.width);
                walls[z].set(start, end);
                previous = end;
            }
        }
        return new Plan(grid, walls);
    }

    private static int bound(Object value, int lo, int hi) {
        require(value instanceof Number n && n.doubleValue() == n.intValue() && n.intValue() >= lo && n.intValue() <= hi,
                "The floor plan contains an invalid wall span.");
        return ((Number)value).intValue();
    }

    private static List<Wall> merge(Plan plan) {
        var done = new ArrayList<Wall>();
        Map<Span, Integer> startingRows = new LinkedHashMap<>();
        for (int z = 0; z <= plan.grid.depth; z++) {
            Map<Span, Integer> active = new LinkedHashMap<>();
            if (z < plan.grid.depth) {
                BitSet row = plan.walls[z];
                for (int x = row.nextSetBit(0); x >= 0; x = row.nextSetBit(row.nextClearBit(x))) {
                    var span = new Span(x, row.nextClearBit(x));
                    Integer previous = startingRows.remove(span);
                    active.put(span, previous == null ? z : previous);
                }
            }
            for (var entry : startingRows.entrySet()) done.add(new Wall(entry.getKey(), entry.getValue(), z));
            require(done.size() + active.size() <= MAX_WALLS, "This plan needs more than 2,048 wall solids. Simplify the mask.");
            startingRows = active;
        }
        return done;
    }

    private static JSONObject box(String name, double x, double y, double z, double w, double h, double d, int mesh) {
        return new JSONObject().put("name", name).put("mesh", mesh).put("translation", new double[]{x,y,z})
                .put("scale", new double[]{w,h,d});
    }

    private record Section(Plan plan, Set<Line> closed, Set<Line> open) {}

    private static Section measure(GlbSection.Result cut, double cell) {
        require(!cut.lines().isEmpty(), "No triangles cross that height. Try a height inside the model.");
        Grid grid = cut.grid() == null ? fit(cut.lines(), cell) : Grid.read(cut.grid());
        if (cut.grid() != null) for (Set<Line> lines : cut.lines().values()) for (Line line : lines)
            require(onGrid(grid, line.a()) && onGrid(grid, line.b()), "The saved floor-plan grid does not cover this section.");
        var walls = empty(grid);
        var closed = new LinkedHashSet<Line>();
        var open = new LinkedHashSet<Line>();
        int work = 0;
        for (Set<Line> lines : cut.lines().values()) {
            Contours classified = classify(lines);
            closed.addAll(classified.closed);
            open.addAll(classified.open);
            work = rasterize(grid, classified.closed, walls, work);
        }
        return new Section(new Plan(grid, walls), closed, open);
    }

    private static boolean onGrid(Grid grid, Point point) {
        double tolerance = 2e-6;
        return point.xm() >= grid.originX - tolerance && point.xm() <= grid.x(grid.width) + tolerance
                && point.zm() >= grid.originZ - tolerance && point.zm() <= grid.z(grid.depth) + tolerance;
    }

    private static Grid fit(Map<Integer, Set<Line>> lines, double cell) {
        double minX = Double.POSITIVE_INFINITY, minZ = minX;
        double maxX = Double.NEGATIVE_INFINITY, maxZ = maxX;
        for (Set<Line> mesh : lines.values()) for (Line line : mesh) {
            minX = Math.min(minX, Math.min(line.a().xm(), line.b().xm()));
            minZ = Math.min(minZ, Math.min(line.a().zm(), line.b().zm()));
            maxX = Math.max(maxX, Math.max(line.a().xm(), line.b().xm()));
            maxZ = Math.max(maxZ, Math.max(line.a().zm(), line.b().zm()));
        }
        double originX = Math.floor(minX / cell) * cell;
        double originZ = Math.floor(minZ / cell) * cell;
        int width = Math.max(1, (int)Math.ceil((maxX - originX) / cell));
        int depth = Math.max(1, (int)Math.ceil((maxZ - originZ) / cell));
        require(width <= MAX_SIDE && depth <= MAX_SIDE, "That grid is too fine for this scene. Increase the cell size.");
        return new Grid(width, depth, originX, originZ, cell);
    }

    private static Contours classify(Set<Line> lines) {
        Map<Point, List<Line>> adjacent = new HashMap<>();
        for (Line line : lines) {
            adjacent.computeIfAbsent(line.a(), ignored -> new ArrayList<>()).add(line);
            adjacent.computeIfAbsent(line.b(), ignored -> new ArrayList<>()).add(line);
        }
        var seen = new HashSet<Point>();
        var closed = new LinkedHashSet<Line>();
        var open = new LinkedHashSet<Line>();
        for (Point point : adjacent.keySet()) {
            if (!seen.add(point)) continue;
            var queue = new ArrayDeque<Point>();
            queue.add(point);
            var component = new LinkedHashSet<Line>();
            boolean loop = true;
            while (!queue.isEmpty()) {
                Point current = queue.removeFirst();
                List<Line> incident = adjacent.get(current);
                loop &= incident.size() == 2;
                for (Line line : incident) {
                    component.add(line);
                    Point next = other(line, current);
                    if (seen.add(next)) queue.addLast(next);
                }
            }
            (loop && hasArea(point, adjacent) ? closed : open).addAll(component);
        }
        return new Contours(closed, open);
    }

    private static boolean hasArea(Point start, Map<Point, List<Line>> adjacent) {
        Point current = start, previous = null;
        double area = 0;
        do {
            List<Line> choices = adjacent.get(current);
            Line edge = choices.getFirst();
            Point next = other(edge, current);
            if (next.equals(previous)) next = other(choices.getLast(), current);
            area += (current.x() - start.x()) * (double)(next.z() - start.z())
                    - (next.x() - start.x()) * (double)(current.z() - start.z());
            previous = current;
            current = next;
        } while (!current.equals(start));
        return Math.abs(area) > 1;
    }

    private static Point other(Line edge, Point point) {
        return edge.a().equals(point) ? edge.b() : edge.a();
    }

    private static int rasterize(Grid grid, Set<Line> closed, BitSet[] walls, int work) {
        Map<Integer, List<Double>> crossings = new HashMap<>();
        for (Line line : closed) {
            Point a = line.a().z() <= line.b().z() ? line.a() : line.b();
            Point b = a.equals(line.a()) ? line.b() : line.a();
            int start = Math.max(0, grid.firstRow(a.zm()));
            int end = Math.min(grid.depth, grid.firstRow(b.zm()));
            for (int z = start; z < end; z++) {
                require(++work <= 2_000_000, "Too many section crossings. Use a larger cell size.");
                double x = a.xm() + (grid.z(z + .5) - a.zm()) * (b.xm() - a.xm()) / (b.zm() - a.zm());
                crossings.computeIfAbsent(z, ignored -> new ArrayList<>()).add(x);
            }
        }
        for (var row : crossings.entrySet()) {
            List<Double> xs = row.getValue();
            xs.sort(Comparator.naturalOrder());
            require(xs.size() % 2 == 0, "A closed section has an inconsistent crossing count.");
            for (int i = 0; i < xs.size(); i += 2) {
                int start = Math.max(0, grid.firstColumn(xs.get(i)));
                int end = Math.min(grid.width, grid.firstColumn(xs.get(i + 1)));
                if (start < end) walls[row.getKey()].set(start, end);
            }
        }
        return work;
    }

    private static BitSet[] empty(Grid grid) {
        return java.util.stream.IntStream.range(0, grid.depth).mapToObj(i -> new BitSet(grid.width)).toArray(BitSet[]::new);
    }

    private static String svg(Section result) {
        Grid grid = result.plan.grid;
        double margin = Math.max(grid.width, grid.depth) * grid.cell * .05;
        double width = grid.width * grid.cell, depth = grid.depth * grid.cell;
        var svg = new StringBuilder("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"")
                .append(grid.originX - margin).append(' ').append(grid.originZ - margin).append(' ')
                .append(width + 2 * margin).append(' ').append(depth + 2 * margin).append("\">");
        svg.append("<rect x=\"").append(grid.originX - margin).append("\" y=\"").append(grid.originZ - margin)
                .append("\" width=\"").append(width + 2 * margin).append("\" height=\"").append(depth + 2 * margin)
                .append("\" fill=\"#f2f6f2\"/>");
        svg.append("<path fill=\"#a7c8b6\" d=\"");
        for (Wall wall : svgRuns(result.plan)) {
            double length = (wall.span.end - wall.span.start) * grid.cell;
            double height = (wall.endRow - wall.firstRow) * grid.cell;
            svg.append('M').append(grid.x(wall.span.start)).append(' ').append(grid.z(wall.firstRow))
                    .append('h').append(length).append('v').append(height).append('h').append(-length).append('z');
        }
        svg.append("\"/>");
        svgLines(svg, result.closed, "#173f3a", margin / 38);
        svgLines(svg, result.open, "#ba663d", margin / 42);
        return svg.append("</svg>").toString();
    }

    private static List<Wall> svgRuns(Plan plan) {
        var rows = new ArrayList<Wall>();
        for (int z = 0; z < plan.grid.depth; z++) {
            BitSet row = plan.walls[z];
            for (int x = row.nextSetBit(0); x >= 0; x = row.nextSetBit(row.nextClearBit(x))) {
                rows.add(new Wall(new Span(x, row.nextClearBit(x)), z, z + 1));
                require(rows.size() <= 100_000, "The floor plan is too detailed to export. Use a larger cell size.");
            }
        }
        return rows;
    }

    private static void svgLines(StringBuilder svg, Set<Line> lines, String color, double stroke) {
        svg.append("<path fill=\"none\" stroke=\"").append(color).append("\" stroke-width=\"").append(stroke).append("\" d=\"");
        // Sorting makes exports independent of hash or catalogue traversal order.
        for (Line line : lines.stream().sorted(Comparator.comparing(Line::a).thenComparing(Line::b)).toList()) {
            svg.append('M').append(line.a().xm()).append(' ').append(line.a().zm())
                    .append('L').append(line.b().xm()).append(' ').append(line.b().zm());
        }
        svg.append("\"/>");
    }

    private static void require(boolean accepted, String message) {
        if (!accepted) throw new IllegalArgumentException(message);
    }
}
