package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/** Lifts the checked raster layout itself through the existing native map collider compiler. */
final class SpatialBlueprint {
    private SpatialBlueprint() {}

    static JSONObject build(JSONObject reference, double cellSize) {
        MapDefinition.rules(reference);
        int width = reference.getInt("width"), height = reference.getInt("height");
        var cells = reference.getJSONArray("cells");
        var walkable = reference.getJSONArray("walkable");
        int[] floor = new int[cells.length()];
        for (int i = 0; i < floor.length; i++) floor[i] = walkable.getBoolean(cells.getInt(i)) ? 1 : 0;
        var connected = MapDefinition.longestComponentRoute(floor, width, height);
        if (connected.size() < 2) throw new IllegalArgumentException("The labelled blueprint needs at least two adjoining floor tiles.");
        var layout = new JSONObject().put("width", width).put("height", height).put("cells", cells)
                .put("walkable", walkable).put("path", new JSONArray(connected));
        MapDefinition.compile(layout,new JSONObject().put("cellSizeM",cellSize));
        var colliders = layout.getJSONArray("objects");
        if (colliders.length() > 32) throw new IllegalArgumentException("This blueprint needs more than 32 native boxes. Simplify its collider regions.");
        var objects = new JSONArray();
        for (Object raw : colliders) {
            var box = (JSONObject) raw;
            box.remove("id");
            if ("avatar".equals(box.getString("sourceId"))) {
                box.put("sourceId","marker").put("label","Movable blueprint marker");
                var p = box.getJSONArray("position");
                p.put(1,box.getJSONArray("size").getDouble(1)/2);
            }
            objects.put(box.put("shape","box"));
        }
        var source = new JSONObject().put("kind","unverified").put("physicalProperties","declared")
                .put("note","Labelled PNG SHA-256 " + reference.getString("imageSha256") + ". Original tiles retained in their original positions. "
                        + "The entered tile width sets scale. An authored collider height and marker supply the third dimension.");
        double span = width*cellSize;
        var scale = new JSONObject().put("a",new JSONArray(List.of(0,0,0))).put("b",new JSONArray(List.of(span,0,0)))
                .put("knownLengthM",span).put("toleranceM",0).put("basis","declared-test");
        String title = reference.getString("title").strip();
        return new JSONObject().put("format","fraynix.spatial-scene").put("version",1).put("units","m").put("frame","right-handed-y-up")
                .put("title",(title.isEmpty() ? "Blueprint" : title.substring(0,Math.min(title.length(),100)))+" · 3D copy")
                .put("source",source).put("gravityMps2",layout.getDouble("gravityMps2")).put("scaleReference",scale).put("objects",objects);
    }
}
