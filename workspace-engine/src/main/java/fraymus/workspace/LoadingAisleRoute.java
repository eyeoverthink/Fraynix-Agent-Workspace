package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

import static fraymus.workspace.ObserverGeometry.*;
import static fraymus.workspace.SpatialScene.array;

/** Search a bounded door-relative grid, then verify the actual consolidated route with Bullet. */
final class LoadingAisleRoute {
    private static final int FORWARD_STEPS = 4;
    private static final int FORWARD_CM = 10, SIDE_CM = 15, SIDE_STEPS = 6;
    private static final int MAX_LENGTH_CM = 220;
    private static final Cell START = new Cell(0,0), TARGET = new Cell(FORWARD_STEPS,0);
    private static final String POLICY = "door-relative-grid-v1";

    private record Cell(int forward, int side) {
        boolean admitted() { return forward >= 0 && forward <= FORWARD_STEPS && Math.abs(side) <= SIDE_STEPS; }
        int distance(Cell other) { return FORWARD_CM*Math.abs(forward-other.forward) + SIDE_CM*Math.abs(side-other.side); }
        boolean collinear(Cell a, Cell b) {
            return (forward == a.forward && forward == b.forward) || (side == a.side && side == b.side);
        }
    }

    private record Work(Cell cell, int distance, int order) {}

    private record Frame(double[] origin, double[] forward, double[] side) {
        Frame(JSONObject plan) {
            this(v(plan.getJSONArray("fromM")),v(plan.getJSONArray("directionM")),right(plan));
        }
        private static double[] right(JSONObject plan) {
            var forward = v(plan.getJSONArray("directionM"));
            return new double[]{forward[2],0,-forward[0]};
        }
        double[] point(Cell cell) {
            return add(add(origin,mul(forward,cell.forward*FORWARD_CM/100.0)),mul(side,cell.side*SIDE_CM/100.0));
        }
    }

    record Attempt(JSONObject plan, int edgesChecked) {}

    static Attempt search(JSONObject parent, JSONObject direct, JSONObject obstruction, Path nativeDir) throws Exception {
        var frame = new Frame(direct);
        var distance = new HashMap<Cell,Integer>();
        var previous = new HashMap<Cell,Cell>();
        var queue = new PriorityQueue<>(Comparator.comparingInt(Work::distance).thenComparingInt(Work::order));
        int order = 0, checked = 0;
        distance.put(START,0);
        queue.add(new Work(START,0,order++));
        try (var world = new SpatialPhysics(parent,nativeDir); var probe = new LoadingAisle.RouteProbe(world,direct)) {
            while (!queue.isEmpty()) {
                var next = queue.remove();
                var here = next.cell;
                if (next.distance != distance.get(here)) continue;
                if (here.equals(TARGET)) {
                    var route = compact(previous);
                    return new Attempt(route.size()<3 ? null : bind(direct,route,obstruction),checked);
                }
                for (var edge : List.of(new Cell(here.forward+1,here.side),
                        new Cell(here.forward,here.side+1),new Cell(here.forward,here.side-1))) {
                    int cost = next.distance+here.distance(edge);
                    if (!edge.admitted() || cost > MAX_LENGTH_CM || cost >= distance.getOrDefault(edge,Integer.MAX_VALUE))
                        continue;
                    checked++;
                    try {
                        probe.check(frame.point(here),frame.point(edge));
                    } catch (LoadingAisle.Obstruction | LoadingAisle.FloorBounds blocked) {
                        continue;
                    }
                    previous.put(edge,here);
                    distance.put(edge,cost);
                    queue.add(new Work(edge,cost,order++));
                }
            }
        }
        return new Attempt(null,checked);
    }

    private static List<Cell> compact(HashMap<Cell,Cell> previous) {
        var reversed = new ArrayList<Cell>();
        for (Cell point=TARGET; point!=null; point=previous.get(point)) reversed.add(point);
        Collections.reverse(reversed);
        var result = new ArrayList<Cell>();
        for (var point : reversed) {
            int size = result.size();
            if (size>=2 && point.collinear(result.get(size-2),result.getLast())) result.removeLast();
            result.add(point);
        }
        return result;
    }

    private static JSONObject bind(JSONObject direct, List<Cell> path, JSONObject obstruction) {
        var frame = new Frame(direct);
        var cells = new JSONArray();
        var points = new JSONArray();
        int length = 0;
        if (path.size()<3 || path.size()>10 || !path.getFirst().equals(START) || !path.getLast().equals(TARGET))
            throw new IllegalArgumentException("The reviewed route must go from the original crate to the exact 40 cm destination.");
        for (int index=0; index<path.size(); index++) {
            var cell = path.get(index);
            if (!cell.admitted()) throw new IllegalArgumentException("A route waypoint is outside the bounded search.");
            if (index>0) {
                var prior = path.get(index-1);
                if (cell.equals(prior) || cell.forward<prior.forward ||
                        (cell.forward!=prior.forward && cell.side!=prior.side))
                    throw new IllegalArgumentException("Each route segment must advance or shift sideways at a fixed height.");
                length += prior.distance(cell);
            }
            cells.put(new JSONArray().put(cell.forward).put(cell.side));
            points.put(index==0 ? direct.getJSONArray("fromM") :
                    index==path.size()-1 ? direct.getJSONArray("toM") : array(frame.point(cell)));
        }
        if (length>MAX_LENGTH_CM) throw new IllegalArgumentException("The route exceeds its distance budget.");
        var route = new JSONObject().put("policy",POLICY).put("cells",cells).put("pointsM",points)
                .put("distanceM",length/100.0).put("direct",obstruction);
        return new JSONObject(direct.toString()).put("route",route);
    }

    static JSONObject rebuild(JSONObject direct, JSONObject route) {
        var cells = route.getJSONArray("cells");
        if (cells.length()<3 || cells.length()>10) throw new IllegalArgumentException("Unexpected route length.");
        var path = new ArrayList<Cell>();
        for (Object raw : cells) {
            var point = (JSONArray)raw;
            if (point.length()!=2 || !(point.get(0) instanceof Integer forward) ||
                    !(point.get(1) instanceof Integer side))
                throw new IllegalArgumentException("A route waypoint must use the approved integer grid.");
            path.add(new Cell(forward,side));
        }
        return bind(direct,path,route.getJSONObject("direct"));
    }

    static JSONObject verify(SpatialPhysics world, JSONObject review) {
        var route = review.getJSONObject("route");
        var path = route.getJSONArray("pointsM");
        var segments = new JSONArray();
        try (var probe = new LoadingAisle.RouteProbe(world,review)) {
            for (int index=1; index<path.length(); index++) {
                var from = path.getJSONArray(index-1);
                var to = path.getJSONArray(index);
                try {
                    probe.check(v(from),v(to));
                } catch (LoadingAisle.Obstruction blocked) {
                    throw new LoadingAisle.Obstruction(blocked.gate,"Segment " + index + ": " + blocked.getMessage());
                }
                segments.put(new JSONObject().put("number",index).put("fromM",from).put("toM",to)
                        .put("distanceM",Math.round(norm(sub(v(to),v(from)))*1e6)/1e6)
                        .put("endpointsClear",true).put("sweptPathClear",true).put("aisleClear",true));
            }
        }
        return new JSONObject().put("distanceM",route.getDouble("distanceM"))
                .put("segmentCount",segments.length()).put("segments",segments);
    }

    static boolean isFixture(String variant) {
        return List.of("staggered-east","staggered-west","staggered-closed").contains(variant);
    }

    static JSONObject fixture(String variant) {
        int degrees = switch (variant) {
            case "staggered-east" -> -41;
            case "staggered-west" -> 83;
            case "staggered-closed" -> -118;
            default -> throw new IllegalArgumentException("Choose a known staggered loading bay.");
        };
        int side = variant.equals("staggered-west") ? -1 : 1;
        String title = switch (variant) {
            case "staggered-east" -> "Staggered bay · −41°";
            case "staggered-west" -> "Mirrored staggered bay · 83°";
            default -> "Blocked crossover · −118°";
        };
        double halfYaw = Math.toRadians(degrees)/2;
        var noRotation = new JSONArray(new double[]{0,0,0,1});
        var rotation = new JSONArray(new double[]{0,Math.sin(halfYaw),0,Math.cos(halfYaw)});
        double[] offset = side>0 ? new double[]{-1.75,0,.4} : new double[]{1.48,0,-.6};
        var scene = LoadingAisle.fixture(0,"none").put("title",title);
        LoadingAisle.find(scene,"cargo").put("label","Blue crate")
                .put("position",array(new double[]{1.5*side,.08,1.30}))
                .put("size",array(new double[]{.12,.16,.12})).put("massKg",2);
        LoadingAisle.find(scene,"spare").put("label","Red crate")
                .put("position",array(new double[]{-1.56*side,.30,2.65}));
        LoadingAisle.find(scene,"shelf-east").put("position",array(new double[]{2.67,.75,2.2}));
        LoadingAisle.find(scene,"shelf-west").put("position",array(new double[]{-2.67,.75,2.2}));
        var objects = scene.getJSONArray("objects");
        objects.put(LoadingAisle.box("inside-block","Inside staggered barrier",
                new double[]{1.165*side,.23,1.4},new double[]{.93,.46,.052},0,noRotation));
        objects.put(LoadingAisle.box("outside-block","Outside staggered barrier",
                new double[]{1.855*side,.23,1.6},new double[]{1.01,.46,.052},0,noRotation));
        if (variant.equals("staggered-closed"))
            objects.put(LoadingAisle.box("closed-crossing","Crossover barrier",
                    new double[]{1.58,.23,1.50},new double[]{1.94,.46,.052},0,noRotation));
        for (Object raw : objects) {
            var body = (JSONObject)raw;
            body.put("position",array(add(offset,rotate(v(body.getJSONArray("position")),rotation,false))))
                    .put("rotation",rotation);
        }
        return scene;
    }

    private LoadingAisleRoute() {}
}
