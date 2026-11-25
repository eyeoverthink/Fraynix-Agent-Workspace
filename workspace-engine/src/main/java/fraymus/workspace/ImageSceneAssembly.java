package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;
import com.jme3.bullet.objects.PhysicsRigidBody;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.function.Function;

/** Compose independently checked meshes using a registered, colour-keyed top view. */
final class ImageSceneAssembly {
    static final int PNG_LIMIT = 512 * 1024;
    private static final int MAX_SIDE = 1024;
    private static final int TICKS = 240;
    private static final String FLOOR = "floor";
    private static final String FORMAT = "fraynix.image-scene-assembly-v1";

    record Plan(JSONObject definition, JSONObject binding) {
        String id() {
            return UUID.nameUUIDFromBytes((FORMAT+":"+WorldFrames.hash(binding)).getBytes(StandardCharsets.UTF_8)).toString();
        }
    }

    private static final class Marker {
        int left = Integer.MAX_VALUE, top = Integer.MAX_VALUE, right = -1, bottom = -1, pixels;
        void add(int x, int y) {
            left = Math.min(left,x);
            top = Math.min(top,y);
            right = Math.max(right,x);
            bottom = Math.max(bottom,y);
            pixels++;
        }
        void validate(int width, int height) {
            int w = right-left+1, h = bottom-top+1;
            if (w<3 || h<3 || pixels!=w*h || left<1 || top<1 || right>=width-1 || bottom>=height-1)
                throw new IllegalArgumentException("Each prop needs one solid rectangular marker at least 3 × 3 pixels, with a border around the layout.");
        }
    }

    static Plan plan(JSONObject input, byte[] png, Function<String,JSONObject> source) throws Exception {
        SpatialScene.keys(input,"title","widthM","props");
        String title = SpatialScene.text(input,"title",90);
        double widthM = SpatialScene.number(input,"widthM",.5,20);
        var slots = input.getJSONArray("props");
        if (slots.length()<2 || slots.length()>4) throw new IllegalArgumentException("Choose two to four checked props.");
        var markers = new LinkedHashMap<Integer,Marker>();
        for (Object value : slots) {
            var slot = (JSONObject)value;
            SpatialScene.keys(slot,"sceneId","label","color");
            int color = color(slot);
            if (markers.putIfAbsent(color,new Marker())!=null)
                throw new IllegalArgumentException("Use a distinct marker colour for each prop.");
        }
        var image = BrowserShapeTrace.decode(png);
        int cols = image.getWidth(), rows = image.getHeight();
        if (cols>MAX_SIDE || rows>MAX_SIDE) throw new IllegalArgumentException("Use a PNG up to 1024 × 1024 pixels.");
        int background = image.getRGB(0,0);
        if (markers.containsKey(background) || (background>>>24)!=255)
            throw new IllegalArgumentException("Use an opaque layout with a background different from the marker colours.");
        for (int y=0;y<rows;y++) for (int x=0;x<cols;x++) {
            int pixel = image.getRGB(x,y);
            if (pixel==background) continue;
            var marker = markers.get(pixel);
            if (marker==null) throw new IllegalArgumentException("The layout contains an unassigned colour or transparent pixel.");
            marker.add(x,y);
        }
        double cell = widthM/cols, depthM = rows*cell;
        RampDefinition.range(depthM,.25,20,"layout depth metres");
        var geometries = new JSONObject();
        var entries = new JSONArray();
        var objects = new JSONArray().put(SpatialReconstruction.box(FLOOR,"Layout floor",
                new double[]{0,-.1,0},new double[]{widthM,.2,depthM},0));
        var occupied = new ArrayList<double[]>();
        for (int i=0;i<slots.length();i++) {
            var slot = slots.getJSONObject(i);
            var marker = markers.get(color(slot));
            marker.validate(cols,rows);
            var original = source.apply(SpatialScene.text(slot,"sceneId",36));
            var proof = original.optJSONObject("reconstruction");
            if (proof==null || !BrowserViewTrace.POLICY.equals(proof.optString("policy")))
                throw new IllegalArgumentException("Use a prop checked against a separate registered image.");
            String mesh = proof.getString("meshSha256");
            var originalBody = original.getJSONArray("objects").getJSONObject(1);
            if (!mesh.equals(originalBody.optString("visualMesh")) || originalBody.getDouble("massKg")<=0)
                throw new IllegalStateException("The reconstructed visual mesh or collider changed.");
            var size = SpatialScene.vector(originalBody.getJSONArray("size"),20);
            double px = (marker.left+marker.right+1)/2.0, py = (marker.top+marker.bottom+1)/2.0;
            double x = (px-cols/2.0)*cell, z = (py-rows/2.0)*cell;
            double[] bounds = {x-size[0]/2,z-size[2]/2,x+size[0]/2,z+size[2]/2};
            if (bounds[0]<-widthM/2+.01 || bounds[2]>widthM/2-.01
                    || bounds[1]<-depthM/2+.01 || bounds[3]>depthM/2-.01)
                throw new IllegalArgumentException("A prop collider extends past the layout floor.");
            for (var prior : occupied) if (bounds[0]<prior[2]+.01 && bounds[2]>prior[0]-.01
                    && bounds[1]<prior[3]+.01 && bounds[3]>prior[1]-.01)
                throw new IllegalArgumentException("Placed prop colliders overlap or have less than 1 cm clearance.");
            occupied.add(bounds);
            String key = "image-prop-"+(i+1);
            var position = new double[]{x,size[1]/2,z};
            var body = SpatialReconstruction.box(key,SpatialScene.text(slot,"label",70),position,size,1)
                    .put("visualMesh",mesh).put("massKg",originalBody.getDouble("massKg"))
                    .put("friction",originalBody.getDouble("friction")).put("restitution",originalBody.getDouble("restitution"));
            objects.put(body);
            geometries.put(mesh,original.getJSONObject("visualMeshes").get(mesh));
            entries.put(new JSONObject().put("sourceId",key).put("source",SpatialPropCopy.binding(original))
                    .put("originalImageSha256",proof.getString("sourceSha256"))
                    .put("heldOutIou",proof.getDouble("heldOutIou")).put("meshSha256",mesh)
                    .put("markerCenterPx",new JSONArray(new double[]{px,py})).put("positionM",new JSONArray(position))
                    .put("colliderSizeM",new JSONArray(size)));
        }
        String layoutSha = MapDefinition.sha(png);
        var binding = new JSONObject().put("format",FORMAT).put("input",WorldFrames.copy(input)).put("layoutSha256",layoutSha)
                .put("imageSizePx",new JSONArray(new int[]{cols,rows})).put("metresPerPixel",cell).put("props",entries);
        var def = new JSONObject().put("format","fraynix.spatial-scene").put("version",1).put("units","m")
                .put("frame","right-handed-y-up").put("title",title).put("gravityMps2",9.81)
                .put("scaleReference",new JSONObject().put("a",new JSONArray(new int[]{0,0,0}))
                        .put("b",new JSONArray(new double[]{widthM,0,0})).put("knownLengthM",widthM)
                        .put("toleranceM",0).put("basis","declared-test"))
                .put("source",new JSONObject().put("kind","unverified").put("physicalProperties","declared")
                        .put("note","Orthographic colour layout "+layoutSha+". Declared width, axis-aligned props and a flat support floor."))
                .put("objects",objects).put("visualMeshes",geometries);
        return new Plan(def,binding);
    }

    private static int color(JSONObject slot) {
        String value = slot.getString("color");
        if (!value.matches("#[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Use a six-digit RGB marker colour.");
        return 0xff000000 | Integer.parseInt(value.substring(1),16);
    }

    static JSONArray verify(JSONObject scene, Path nativeDir) throws Exception {
        var lifted = WorldFrames.copy(scene);
        var objects = lifted.getJSONArray("objects");
        for (int i=1;i<objects.length();i++) {
            var pos = objects.getJSONObject(i).getJSONArray("position");
            pos.put(1,pos.getDouble(1)+.18);
        }
        var result = new JSONArray();
        try (var world = new SpatialPhysics(lifted,nativeDir)) {
            var floor = world.bodies.get(objects.getJSONObject(0).getString("id"));
            int[] contacts = new int[objects.length()];
            for (int tick=1;tick<=TICKS;tick++) {
                world.step();
                for (int i=1;i<objects.length();i++) if (contacts[i]==0) {
                    var body = world.bodies.get(objects.getJSONObject(i).getString("id"));
                    if (touchesFloor(world,floor,body)) contacts[i]=tick;
                }
            }
            for (int i=1;i<objects.length();i++) {
                var authored = scene.getJSONArray("objects").getJSONObject(i);
                var body = world.bodies.get(authored.getString("id"));
                var pos = body.getPhysicsLocation();
                var expected = authored.getJSONArray("position");
                double speed = body.getLinearVelocity().length();
                if (contacts[i]==0 || !touchesFloor(world,floor,body) || speed>.03
                        || Math.abs(pos.y-expected.getDouble(1))>.01
                        || Math.hypot(pos.x-expected.getDouble(0),pos.z-expected.getDouble(2))>.01)
                    throw new IllegalStateException("The native floor check did not settle "+authored.getString("label")+".");
                result.put(new JSONObject().put("sourceId",authored.getString("sourceId"))
                        .put("contactTick",contacts[i]).put("ticks",TICKS)
                        .put("centerMm",new JSONArray(new long[]{Math.round(pos.x*1000),Math.round(pos.y*1000),Math.round(pos.z*1000)}))
                        .put("speedMmps",Math.round(speed*1000)));
            }
        }
        return result;
    }

    private static boolean touchesFloor(SpatialPhysics world, PhysicsRigidBody floor, PhysicsRigidBody body) {
        boolean[] contact = {false};
        world.space.pairTest(floor,body,event->contact[0] |= event.getDistance1()<=.001);
        return contact[0];
    }
}
