package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import static fraymus.workspace.ObserverGeometry.*;
import static fraymus.workspace.SpatialScene.array;

/** Declared-scale examples exercise route resolution and actual crate footprints. */
final class LoadingAisleWideBays {
    private enum Bay {
        FINE("wide-fine","Wide crate bay · −53°",-53,1,1,.42,.24,1.37,1.73),
        FILM("film-bay","Night dispatch bay · 96°",96,-1,1.10,.58,.26,1.46,1.84),
        MIRROR("wide-mirror","Wide mirrored bay · 96°",96,-1,1.10,.58,.26,1.46,1.84),
        COARSE("wide-coarse","Wide bay with open crossover · 27°",27,1,1,.42,.24,1.32,1.74),
        BLOCKED("wide-blocked","Oversize crate bay · −127°",-127,1,1,.74,.32,1.37,1.73),
        REMOTE("wide-remote","Two crossovers · −18°",-18,1,1,.42,.24,1.37,1.73);

        final String key, title;
        final int yaw, side;
        final double start, width, depth, first, second;

        Bay(String key, String title, int yaw, int side, double start, double width, double depth,
            double first, double second) {
            this.key = key;
            this.title = title;
            this.yaw = yaw;
            this.side = side;
            this.start = start;
            this.width = width;
            this.depth = depth;
            this.first = first;
            this.second = second;
        }
    }

    static boolean isFixture(String variant) { return find(variant)!=null; }

    private static Bay find(String variant) {
        for (var bay : Bay.values()) if (bay.key.equals(variant)) return bay;
        return null;
    }

    static JSONObject fixture(String variant) {
        var bay = find(variant);
        if (bay==null) throw new IllegalArgumentException("Choose a known wide-crate bay.");
        var scene = LoadingAisle.fixture(0,"none").put("title",bay.title);
        LoadingAisle.find(scene,"floor").put("position",array(new double[]{0,-.1,2.1}))
                .put("size",array(new double[]{7.2,.2,7.2}));
        LoadingAisle.find(scene,"aisle-end").put("position",array(new double[]{0,-.015,4.4}));
        LoadingAisle.find(scene,"cargo").put("label","Blue crate").put("massKg",12)
                .put("position",array(new double[]{1.65*bay.side,.24,bay.start}))
                .put("size",array(new double[]{bay.width,.48,bay.depth}));
        LoadingAisle.find(scene,"spare").put("label","Red crate")
                .put("position",array(new double[]{-1.56*bay.side,.30,2.95}));
        LoadingAisle.find(scene,"shelf-east").put("position",array(new double[]{3.23,.75,2.3}));
        LoadingAisle.find(scene,"shelf-west").put("position",array(new double[]{-3.23,.75,2.3}));
        var upright = new JSONArray(new double[]{0,0,0,1});
        var objects = scene.getJSONArray("objects");
        objects.put(LoadingAisle.box("inner-wide","Inner barrier",new double[]{1.215*bay.side,.29,bay.first},
                new double[]{1.13,.58,.08},0,upright));
        objects.put(LoadingAisle.box("outer-wide","Outer barrier",new double[]{2.235*bay.side,.29,bay.second},
                new double[]{1.41,.58,.08},0,upright));
        if (bay==Bay.FILM) {
            LoadingAisle.find(scene,"cargo").put("sourceId","flight-case").put("label","Amber crate").put("massKg",9.5);
            LoadingAisle.find(scene,"shelf-west").put("sourceId","dispatch-shelf").put("label","Dispatch shelf");
            LoadingAisle.find(scene,"shelf-east").put("label","Reserve shelf");
            LoadingAisle.find(scene,"dispatch-shelf").put("position",array(new double[]{-3.17,.78,2.23}))
                    .put("size",array(new double[]{.56,1.56,1.75}));
        }
        if (bay==Bay.REMOTE) {
            LoadingAisle.find(scene,"inner-wide").put("size",array(new double[]{1.03,.58,.08}))
                    .put("position",array(new double[]{1.165,.29,bay.first}));
            LoadingAisle.find(scene,"outer-wide").put("size",array(new double[]{1.31,.58,.08}))
                    .put("position",array(new double[]{2.285,.29,bay.second}));
            objects.put(LoadingAisle.box("remote-outer","Second outer barrier",new double[]{2.285,.29,2.17},
                    new double[]{1.31,.58,.08},0,upright));
            objects.put(LoadingAisle.box("remote-inner","Second inner barrier",new double[]{1.165,.29,2.53},
                    new double[]{1.03,.58,.08},0,upright));
        }
        double angle = Math.toRadians(bay.yaw)/2;
        var rotation = new JSONArray(new double[]{0,Math.sin(angle),0,Math.cos(angle)});
        double[] origin = {-.8*bay.side,0,.35};
        for (Object raw : objects) {
            var body = (JSONObject)raw;
            body.put("position",array(add(origin,rotate(v(body.getJSONArray("position")),rotation,false))))
                    .put("rotation",rotation);
        }
        return scene;
    }

    private LoadingAisleWideBays() {}
}
