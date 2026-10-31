package fraymus.workspace;

import org.json.*;
import java.util.*;

/** User-declared palette properties. No photographic material inference. */
final class MapMaterials {
    static final String VERSION = "declared-palette-materials-v1";

    static JSONArray validate(JSONObject map, JSONArray input) {
        if (input.isEmpty() || input.length() > map.getJSONArray("palette").length())
            throw new IllegalArgumentException("Declare 1..palette-size material bindings");
        var seen = new HashSet<Integer>();
        var result = new JSONArray();
        for (Object raw : input) {
            var item = (JSONObject) raw;
            SpatialScene.keys(item, "tile", "name", "friction", "restitution", "dragNsPerM");
            int tile = Procedural.integer(item, "tile", -1, 0, map.getJSONArray("palette").length()-1);
            if(!map.getJSONArray("walkable").getBoolean(tile)) throw new IllegalArgumentException("Bind materials to declared floor tiles");
            if (!seen.add(tile)) throw new IllegalArgumentException("Duplicate material tile");
            String name = item.getString("name").trim();
            if (name.isEmpty() || name.length() > 60) throw new IllegalArgumentException("Material name needs 1..60 characters");
            double friction = item.getDouble("friction"), restitution = item.optDouble("restitution", 0), drag = item.optDouble("dragNsPerM", 0);
            RampDefinition.range(friction, .05, 1.2, "material friction");
            RampDefinition.range(restitution, 0, .95, "material restitution");
            RampDefinition.range(drag, 0, 8, "material dragNsPerM");
            result.put(new JSONObject().put("tile", tile).put("name", name).put("friction", friction)
                .put("restitution", restitution).put("dragNsPerM", drag));
        }
        return result;
    }

    static JSONObject material(JSONObject map, int tile) {
        for (Object raw : map.optJSONArray("materials", new JSONArray())) {
            var m = (JSONObject) raw;
            if (m.getInt("tile") == tile) return m;
        }
        return new JSONObject().put("tile", tile).put("name", "Default surface")
            .put("friction", map.optDouble("friction", .6)).put("restitution", 0).put("dragNsPerM", 0);
    }

    static int tileAt(JSONObject map, double x, double z) {
        int w=map.getInt("width"), h=map.getInt("height");
        double size=map.getDouble("cellSizeM");
        int ix=(int)Math.floor(x/size+w/2.0), iz=(int)Math.floor(z/size+h/2.0);
        return ix<0||iz<0||ix>=w||iz>=h ? -1 : map.getJSONArray("cells").getInt(iz*w+ix);
    }

    static void compileFloors(JSONObject map, JSONArray objects, double size) {
        int w=map.getInt("width"), h=map.getInt("height");
        var cells=map.getJSONArray("cells");
        boolean[] done=new boolean[w*h];
        for(int y=0;y<h;y++) for(int x=0;x<w;x++) {
            int id=y*w+x, tile=cells.getInt(id);
            if(done[id]) continue;
            int width=1,height=1;
            while(x+width<w&&!done[id+width]&&cells.getInt(id+width)==tile) width++;
            outer: while(y+height<h) {
                for(int k=0;k<width;k++) if(done[(y+height)*w+x+k]||cells.getInt((y+height)*w+x+k)!=tile) break outer;
                height++;
            }
            for(int yy=0;yy<height;yy++) for(int xx=0;xx<width;xx++) done[(y+yy)*w+x+xx]=true;
            var m=material(map,tile);
            objects.put(MapDefinition.box("floor-"+id,
                new double[]{(x+(width-1)/2.0-(w-1)/2.0)*size,-.15,(y+(height-1)/2.0-(h-1)/2.0)*size},
                new double[]{width*size,.3,height*size},0,m.getDouble("friction"))
                .put("restitution",m.getDouble("restitution")).put("tile",tile).put("material",m));
        }
    }
}
