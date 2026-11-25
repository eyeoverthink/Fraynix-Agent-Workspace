package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;
import com.jme3.bullet.objects.PhysicsRigidBody;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/** Compose independently checked meshes from marked or registered unmarked images. */
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

    private record Asset(JSONObject slot, JSONObject original, JSONObject proof, JSONObject body, String mesh, double[] size) {}

    static Plan plan(JSONObject input, byte[] png, byte[] guide, byte[] witness, Function<String,JSONObject> source) throws Exception {
        SpatialScene.keys(input,"title","widthM","props","recoverYaw","cameraAngles");
        if (input.has("cameraAngles") && guide==null)
            throw new IllegalArgumentException("Custom camera angles require full-pose recovery.");
        boolean recoverYaw = input.has("recoverYaw");
        if (recoverYaw && (!Boolean.TRUE.equals(input.get("recoverYaw")) || witness==null || guide!=null))
            throw new IllegalArgumentException("Rotation recovery requires two registered views.");
        if (guide!=null && witness==null)
            throw new IllegalArgumentException("Tilt recovery needs an overhead image, a fitting side and a held-out view.");
        String title = SpatialScene.text(input,"title",90);
        double widthM = SpatialScene.number(input,"widthM",.5,20);
        var slots = input.getJSONArray("props");
        if (slots.length()<2 || slots.length()>4) throw new IllegalArgumentException("Choose two to four checked props.");
        var image = BrowserShapeTrace.decode(png);
        int cols = image.getWidth(), rows = image.getHeight();
        if (cols>MAX_SIDE || rows>MAX_SIDE) throw new IllegalArgumentException("Use a PNG up to 1024 × 1024 pixels.");
        double cell = widthM/cols, depthM = rows*cell;
        RampDefinition.range(depthM,.25,20,"layout depth metres");
        var assets = new ArrayList<Asset>();
        for (Object value : slots) {
            var slot = (JSONObject)value;
            if (witness==null) SpatialScene.keys(slot,"sceneId","label","color");
            else SpatialScene.keys(slot,"sceneId","label");
            var original = source.apply(SpatialScene.text(slot,"sceneId",36));
            var proof = original.optJSONObject("reconstruction");
            if (proof==null || !BrowserViewTrace.POLICY.equals(proof.optString("policy")))
                throw new IllegalArgumentException("Use a prop checked against a separate registered image.");
            String mesh = proof.getString("meshSha256");
            var body = original.getJSONArray("objects").getJSONObject(1);
            if (!mesh.equals(body.optString("visualMesh")) || body.getDouble("massKg")<=0)
                throw new IllegalStateException("The reconstructed visual mesh or collider changed.");
            assets.add(new Asset(slot,original,proof,body,mesh,SpatialScene.vector(body.getJSONArray("size"),20)));
        }
        ImageSceneRegistration.Result registration = null;
        ImageTiltRegistration.Result tilt = null;
        List<double[]> centers;
        if (witness==null) centers = markerCenters(image,slots);
        else {
            if (java.util.Arrays.equals(png,witness))
                throw new IllegalArgumentException("Choose a separate registered second view.");
            var props = assets.stream().map(asset->new ImageSceneRegistration.Prop(SpatialScene.text(asset.slot,"label",70),
                    SpatialVisualMesh.read(asset.original.getJSONObject("visualMeshes").getString(asset.mesh)),asset.size[1])).toList();
            if (guide==null) {
                registration = ImageSceneRegistration.locate(image,witness,cell,props,recoverYaw);
                centers = registration.centersPx();
            } else {
                tilt = ImageTiltRegistration.locate(image,guide,witness,cell,props,input.has("cameraAngles")?input.getJSONObject("cameraAngles"):null);
                centers = tilt.poses().stream().map(pose->new double[]{cols/2.0+pose.x()/cell,rows/2.0+pose.z()/cell}).toList();
            }
        }
        var geometries = new JSONObject();
        var entries = new JSONArray();
        var objects = new JSONArray().put(SpatialReconstruction.box(FLOOR,"Layout floor",
                new double[]{0,-.1,0},new double[]{widthM,.2,depthM},0));
        var occupied = new ArrayList<double[]>();
        for (int i=0;i<assets.size();i++) {
            var asset = assets.get(i);
            var original = asset.original;
            var proof = asset.proof;
            var originalBody = asset.body;
            String mesh = asset.mesh;
            double[] size = asset.size;
            double px = centers.get(i)[0], py = centers.get(i)[1];
            double x = (px-cols/2.0)*cell, z = (py-rows/2.0)*cell;
            int yaw = recoverYaw?registration.yawDeg()[i]:0;
            double radians = Math.toRadians(yaw);
            double sin = Math.abs(Math.sin(radians)), cos = Math.abs(Math.cos(radians));
            double extentX=(cos*size[0]+sin*size[2])/2, extentZ=(sin*size[0]+cos*size[2])/2;
            var pose = tilt==null?null:tilt.poses().get(i);
            double y = size[1]/2;
            if (pose!=null) {
                x=pose.x(); y=pose.y(); z=pose.z();
                var rotation = ImageTiltRegistration.rotation(pose.angles());
                extentX=extent(rotation[0],size);
                extentZ=extent(rotation[2],size);
                if (y<extent(rotation[1],size)-.002)
                    throw new IllegalArgumentException("The recovered "+SpatialScene.text(asset.slot,"label",70)+" collider intersects the floor.");
            }
            double[] bounds = {x-extentX,z-extentZ,x+extentX,z+extentZ};
            if (bounds[0]<-widthM/2+.01 || bounds[2]>widthM/2-.01
                    || bounds[1]<-depthM/2+.01 || bounds[3]>depthM/2-.01)
                throw new IllegalArgumentException("A prop collider extends past the layout floor.");
            for (var prior : occupied) if (bounds[0]<prior[2]+.01 && bounds[2]>prior[0]-.01
                    && bounds[1]<prior[3]+.01 && bounds[3]>prior[1]-.01)
                throw new IllegalArgumentException("Placed prop colliders overlap or have less than 1 cm clearance.");
            occupied.add(bounds);
            String key = "image-prop-"+(i+1);
            var position = new double[]{x,y,z};
            var body = SpatialReconstruction.box(key,SpatialScene.text(asset.slot,"label",70),position,size,1)
                    .put("visualMesh",mesh).put("massKg",originalBody.getDouble("massKg"))
                    .put("friction",originalBody.getDouble("friction")).put("restitution",originalBody.getDouble("restitution"));
            if (recoverYaw) body.put("rotation",new JSONArray(new double[]{0,Math.sin(radians/2),0,Math.cos(radians/2)}));
            if (pose!=null) body.put("rotation",new JSONArray(pose.angles().quaternion()));
            objects.put(body);
            geometries.put(mesh,original.getJSONObject("visualMeshes").get(mesh));
            var entry = new JSONObject().put("sourceId",key).put("source",SpatialPropCopy.binding(original))
                    .put("originalImageSha256",proof.getString("sourceSha256"))
                    .put("heldOutIou",proof.getDouble("heldOutIou")).put("meshSha256",mesh)
                    .put(witness==null?"markerCenterPx":"overheadCenterPx",new JSONArray(new double[]{px,py})).put("positionM",new JSONArray(position))
                    .put("colliderSizeM",new JSONArray(size));
            if (recoverYaw) entry.put("yawDeg",yaw);
            if (pose!=null) entry.put("anglesDeg",pose.angles().values());
            entries.put(entry);
        }
        String layoutSha = MapDefinition.sha(png);
        var binding = new JSONObject().put("format",FORMAT).put("input",WorldFrames.copy(input)).put("layoutSha256",layoutSha)
                .put("imageSizePx",new JSONArray(new int[]{cols,rows})).put("metresPerPixel",cell).put("props",entries);
        if (registration!=null) binding.put("registration",registration.evidence());
        if (tilt!=null) binding.put("registration",tilt.evidence());
        String note = witness==null
                ? "Orthographic colour layout "+layoutSha+". Declared width, axis-aligned props and a flat support floor."
                : "Unmarked overhead view "+layoutSha+". "+(tilt!=null
                    ? "Full poses fitted from overhead and a registered side, then checked against a held-out angle. Declared scale."
                    : recoverYaw
                      ? "Positions and upright orientations checked against a registered second angle. Declared width and a flat support floor."
                      : "Positions cross-checked against a registered second angle. Declared width, axis-aligned props and a flat support floor.");
        var def = new JSONObject().put("format","fraynix.spatial-scene").put("version",1).put("units","m")
                .put("frame","right-handed-y-up").put("title",title).put("gravityMps2",9.81)
                .put("scaleReference",new JSONObject().put("a",new JSONArray(new int[]{0,0,0}))
                        .put("b",new JSONArray(new double[]{widthM,0,0})).put("knownLengthM",widthM)
                        .put("toleranceM",0).put("basis","declared-test"))
                .put("source",new JSONObject().put("kind","unverified").put("physicalProperties","declared")
                        .put("note",note))
                .put("objects",objects).put("visualMeshes",geometries);
        return new Plan(def,binding);
    }

    private static double extent(double[] axis, double[] size) {
        return (Math.abs(axis[0])*size[0]+Math.abs(axis[1])*size[1]+Math.abs(axis[2])*size[2])/2;
    }

    private static List<double[]> markerCenters(BufferedImage image, JSONArray slots) {
        int cols = image.getWidth(), rows = image.getHeight();
        var markers = new LinkedHashMap<Integer,Marker>();
        for (Object value : slots) {
            int color = color((JSONObject)value);
            if (markers.putIfAbsent(color,new Marker())!=null)
                throw new IllegalArgumentException("Use a distinct marker colour for each prop.");
        }
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
        var result = new ArrayList<double[]>();
        for (var marker : markers.values()) {
            marker.validate(cols,rows);
            result.add(new double[]{(marker.left+marker.right+1)/2.0,(marker.top+marker.bottom+1)/2.0});
        }
        return result;
    }

    private static int color(JSONObject slot) {
        String value = slot.getString("color");
        if (!value.matches("#[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Use a six-digit RGB marker colour.");
        return 0xff000000 | Integer.parseInt(value.substring(1),16);
    }

    static JSONArray verify(JSONObject scene, Path nativeDir, boolean verifyRotation, boolean releaseTilt) throws Exception {
        var lifted = WorldFrames.copy(scene);
        var objects = lifted.getJSONArray("objects");
        for (int i=1;i<objects.length();i++) {
            var pos = objects.getJSONObject(i).getJSONArray("position");
            if (!releaseTilt) pos.put(1,pos.getDouble(1)+.18);
        }
        var result = new JSONArray();
        try (var world = new SpatialPhysics(lifted,nativeDir)) {
            var floor = world.bodies.get(objects.getJSONObject(0).getString("id"));
            int[] contacts = new int[objects.length()];
            int ticks=releaseTilt?TICKS*2:TICKS;
            var initialErrors = new long[objects.length()];
            if (releaseTilt) for (int i=1;i<objects.length();i++) {
                var authored = objects.getJSONObject(i);
                var body = world.bodies.get(authored.getString("id"));
                double error = rotationError(body,authored.getJSONArray("rotation"));
                if (error>.1 || body.getPhysicsLocation().distance(
                        SpatialPhysics.vec(SpatialScene.vector(authored.getJSONArray("position"),20)))>1e-5)
                    throw new IllegalStateException("The native world did not start at "+authored.getString("label")+"'s fitted pose.");
                initialErrors[i]=Math.round(error*1000);
            }
            for (int tick=1;tick<=ticks;tick++) {
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
                        || !releaseTilt && (Math.abs(pos.y-expected.getDouble(1))>.01
                        || Math.hypot(pos.x-expected.getDouble(0),pos.z-expected.getDouble(2))>.01))
                    throw new IllegalStateException("The native floor check did not settle "+authored.getString("label")+".");
                var trace = new JSONObject().put("sourceId",authored.getString("sourceId"))
                        .put("contactTick",contacts[i]).put("ticks",ticks)
                        .put("centerMm",new JSONArray(new long[]{Math.round(pos.x*1000),Math.round(pos.y*1000),Math.round(pos.z*1000)}))
                        .put("speedMmps",Math.round(speed*1000));
                if (verifyRotation) {
                    double error=rotationError(body,authored.getJSONArray("rotation"));
                    if (error>1 && !releaseTilt)
                        throw new IllegalStateException("The native floor check did not retain "+authored.getString("label")+"'s orientation.");
                    trace.put("rotationErrorMilliDeg",Math.round(error*1000));
                    if (releaseTilt) trace.put("initialRotationErrorMilliDeg",initialErrors[i])
                            .put("initialCenterM",expected);
                }
                result.put(trace);
            }
        }
        return result;
    }

    private static double rotationError(PhysicsRigidBody body, JSONArray target) {
        var actual = body.getPhysicsRotation();
        double[] quaternion = {actual.getX(),actual.getY(),actual.getZ(),actual.getW()};
        double dot=0, norm=0;
        for (int axis=0;axis<4;axis++) {
            dot+=quaternion[axis]*target.getDouble(axis);
            norm+=quaternion[axis]*quaternion[axis];
        }
        if (norm==0 || !Double.isFinite(norm))
            throw new IllegalStateException("The native floor check returned an invalid rotation.");
        return 2*Math.toDegrees(Math.acos(Math.min(1,Math.abs(dot)/Math.sqrt(norm))));
    }

    private static boolean touchesFloor(SpatialPhysics world, PhysicsRigidBody floor, PhysicsRigidBody body) {
        boolean[] contact = {false};
        world.space.pairTest(floor,body,event->contact[0] |= event.getDistance1()<=.001);
        return contact[0];
    }
}
