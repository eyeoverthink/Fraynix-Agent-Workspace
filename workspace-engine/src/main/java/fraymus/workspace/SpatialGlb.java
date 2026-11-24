package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Offline glTF 2.0 binary exchange for the native oriented-box scene contract. */
final class SpatialGlb {
    static final int LIMIT = 512 * 1024;
    private static final int MAGIC = 0x46546c67, JSON = 0x4e4f534a, BIN = 0x004e4942;
    private static final String METADATA = "fraynixScene", BODY = "fraynixBody";
    private static final int[] INDICES = {0,2,3,0,3,1,4,5,7,4,7,6,0,1,5,0,5,4,2,6,7,2,7,3,
            0,4,6,0,6,2,1,3,7,1,7,5};
    private static final double EPS = 1e-5;

    private SpatialGlb() {}

    static byte[] exportScene(JSONObject scene) {
        var definition = new JSONObject(scene.getString("sourceText"));
        var info = new JSONObject().put("title", definition.getString("title"))
                .put("gravityMps2", definition.getDouble("gravityMps2")).put("source", definition.getJSONObject("source"))
                .put("scaleReference", metresReference(definition));
        var nodes = new JSONArray();
        var roots = new JSONArray();
        nodes.put(new JSONObject().put("name", "Fraynix scene metadata")
                .put("extras", new JSONObject().put(METADATA, info.toString())));
        roots.put(0);
        for (Object raw : scene.getJSONArray("objects")) {
            var box = (JSONObject) raw;
            var body = new JSONObject();
            for (String key : Set.of("sourceId", "label", "massKg", "friction", "restitution")) body.put(key, box.get(key));
            roots.put(nodes.length());
            nodes.put(new JSONObject().put("name", box.getString("label")).put("mesh", 0)
                    .put("translation", box.getJSONArray("position")).put("rotation", normalized(box.getJSONArray("rotation")))
                    .put("scale", box.getJSONArray("size")).put("extras", new JSONObject().put(BODY, body.toString())));
        }
        var bin = buffer(8 * 12 + INDICES.length * 2);
        for (int i = 0; i < 8; i++) for (int axis = 2; axis >= 0; axis--)
            bin.putFloat((i & (1 << axis)) == 0 ? -.5f : .5f);
        for (int index : INDICES) bin.putShort((short) index);
        var json = new JSONObject().put("asset", new JSONObject().put("version", "2.0").put("generator", "Fraynix Spatial"))
                .put("scene", 0).put("scenes", new JSONArray().put(new JSONObject().put("nodes", roots)))
                .put("nodes", nodes).put("buffers", new JSONArray().put(new JSONObject().put("byteLength", bin.capacity())))
                .put("bufferViews", new JSONArray().put(view(0, 96)).put(view(96, INDICES.length * 2)))
                .put("accessors", new JSONArray().put(new JSONObject().put("bufferView", 0).put("componentType", 5126)
                        .put("count", 8).put("type", "VEC3").put("min", new JSONArray(new double[]{-.5,-.5,-.5}))
                        .put("max", new JSONArray(new double[]{.5,.5,.5}))).put(new JSONObject().put("bufferView", 1)
                        .put("componentType", 5123).put("count", INDICES.length).put("type", "SCALAR")))
                .put("meshes", new JSONArray().put(new JSONObject().put("primitives", new JSONArray().put(
                        new JSONObject().put("attributes", new JSONObject().put("POSITION", 0)).put("indices", 1)))));
        byte[] text = json.toString().getBytes(StandardCharsets.UTF_8);
        int padded = (text.length + 3) & ~3;
        var output = buffer(28 + padded + bin.capacity()).putInt(MAGIC).putInt(2).putInt(28 + padded + bin.capacity())
                .putInt(padded).putInt(JSON).put(text);
        for (int i = text.length; i < padded; i++) output.put((byte) ' ');
        return output.putInt(bin.capacity()).putInt(BIN).put(bin.array()).array();
    }

    static byte[] verifiedExport(JSONObject scene) throws Exception {
        byte[] encoded=exportScene(scene);
        var restored=SpatialScene.parse(importScene(encoded).toString(),scene.getString("id"));
        var originals=scene.getJSONArray("objects");
        var boxes=restored.getJSONArray("objects");
        if (originals.length()!=boxes.length() || restored.getDouble("gravityMps2")!=scene.getDouble("gravityMps2"))
            throw new IllegalStateException("GLB export changed the scene contract.");
        for (int i=0;i<originals.length();i++) {
            var a=originals.getJSONObject(i);
            var b=boxes.getJSONObject(i);
            for (String key : Set.of("sourceId","label","massKg","friction","restitution"))
                if (!new JSONObject().put(key,a.get(key)).similar(new JSONObject().put(key,b.get(key))))
                    throw new IllegalStateException("GLB export changed body properties.");
            for (String key : Set.of("position","size"))
                if (ObserverGeometry.norm(ObserverGeometry.sub(ObserverGeometry.v(a.getJSONArray(key)),
                        ObserverGeometry.v(b.getJSONArray(key))))>EPS)
                    throw new IllegalStateException("GLB export changed an oriented box.");
            var q=normalized(a.getJSONArray("rotation"));
            var r=normalized(b.getJSONArray("rotation"));
            double matches=0;
            for (int k=0;k<4;k++) matches+=q.getDouble(k)*r.getDouble(k);
            if (Math.abs(matches)<1-EPS) throw new IllegalStateException("GLB export changed a box rotation.");
        }
        if (!scene.getJSONObject("scaleCheck").similar(restored.getJSONObject("scaleCheck"))) {
            double expected=scene.getJSONObject("scaleCheck").getDouble("convertedLengthM");
            double actual=restored.getJSONObject("scaleCheck").getDouble("convertedLengthM");
            if (Math.abs(expected-actual)>EPS) throw new IllegalStateException("GLB export changed the scale reference.");
        }
        return encoded;
    }

    private static JSONArray normalized(JSONArray q) {
        double total=0;
        for (int i=0;i<4;i++) total+=q.getDouble(i)*q.getDouble(i);
        var output=new JSONArray();
        for (int i=0;i<4;i++) output.put(q.getDouble(i)/Math.sqrt(total));
        return output;
    }

    private static JSONObject metresReference(JSONObject original) {
        var ref = WorldFrames.copy(original.getJSONObject("scaleReference"));
        double scale = switch (original.getString("units")) { case "cm" -> .01; case "mm" -> .001; default -> 1; };
        for (String key : Set.of("a", "b")) {
            var point = ref.getJSONArray(key);
            ref.put(key, new JSONArray(new double[]{point.getDouble(0)*scale, point.getDouble(1)*scale, point.getDouble(2)*scale}));
        }
        return ref;
    }

    private static JSONObject view(int offset, int length) {
        return new JSONObject().put("buffer", 0).put("byteOffset", offset).put("byteLength", length);
    }
    private static ByteBuffer buffer(int size) { return ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN); }

    static JSONObject importScene(byte[] bytes) throws Exception {
        if (bytes.length < 32 || bytes.length > LIMIT) throw new IllegalArgumentException("Choose a GLB up to 512 KiB.");
        var reader = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        if (reader.getInt() != MAGIC || reader.getInt() != 2 || reader.getInt() != bytes.length)
            throw new IllegalArgumentException("Only complete glTF 2.0 binary files are supported.");
        byte[] text = chunk(reader, JSON);
        byte[] bin = chunk(reader, BIN);
        if (reader.hasRemaining()) throw new IllegalArgumentException("Unsupported additional GLB chunk.");
        var root = new JSONObject(StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(text)).toString().strip());
        if (!"2.0".equals(root.getJSONObject("asset").getString("version"))
                || root.optJSONArray("extensionsRequired", new JSONArray()).length() > 0
                || root.optJSONArray("animations", new JSONArray()).length() > 0
                || root.optJSONArray("skins", new JSONArray()).length() > 0)
            throw new IllegalArgumentException("Use static, uncompressed box geometry with no required extensions.");
        var buffers = root.getJSONArray("buffers");
        if (buffers.length() != 1 || buffers.getJSONObject(0).has("uri")) throw new IllegalArgumentException("GLB needs one embedded buffer.");
        int length = count(buffers.getJSONObject(0), "byteLength", 1, bin.length);
        if (bin.length - length > 3) throw new IllegalArgumentException("GLB buffer length disagrees with the binary chunk.");
        return new Reader(root, Arrays.copyOf(bin, length)).scene(MapDefinition.sha(bytes));
    }

    private static byte[] chunk(ByteBuffer buffer, int type) {
        if (buffer.remaining() < 8) throw new IllegalArgumentException("Missing GLB chunk.");
        int length = buffer.getInt();
        if (length <= 0 || length % 4 != 0 || buffer.getInt() != type || length > buffer.remaining())
            throw new IllegalArgumentException("Invalid GLB chunk length or type.");
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return bytes;
    }

    private static int count(JSONObject object, String key, int min, int max) {
        double value = SpatialScene.number(object, key, min, max);
        if (value != (int) value) throw new IllegalArgumentException("A GLB index or length must be a whole number.");
        return (int) value;
    }
    private static int offset(JSONObject object, String key, int max) {
        return object.has(key) ? count(object,key,0,max) : 0;
    }
    private static int index(JSONArray nodes, int at) {
        Object value = nodes.get(at);
        if (!(value instanceof Number n) || n.doubleValue() != n.intValue()) throw new IllegalArgumentException("Invalid glTF node index.");
        return n.intValue();
    }

    private static final class Reader {
        final JSONObject root;
        final ByteBuffer bytes;
        final JSONArray nodes;
        final Set<Integer> visited = new HashSet<>();
        final JSONArray boxes = new JSONArray();
        JSONObject metadata;

        Reader(JSONObject root, byte[] bytes) {
            this.root = root;
            this.bytes = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            nodes = root.getJSONArray("nodes");
            if (nodes.length() > 128) throw new IllegalArgumentException("GLB scene has more than 128 nodes.");
        }

        JSONObject scene(String sha) {
            var parents = root.getJSONArray("scenes");
            var roots = parents.getJSONObject(offset(root, "scene", parents.length()-1)).getJSONArray("nodes");
            for (int i = 0; i < roots.length(); i++) visit(index(roots,i), Transform.identity(), 0);
            if (metadata == null) throw new IllegalArgumentException("The GLB needs the Fraynix scene metadata node. Export a scene from Spatial first.");
            SpatialScene.keys(metadata, "title", "source", "scaleReference", "gravityMps2");
            var source = WorldFrames.copy(metadata.getJSONObject("source"));
            String original = source.getString("note");
            String prefix = "GLB SHA-256 " + sha + ". ";
            source.put("note", prefix + original.substring(0, Math.min(original.length(),1000-prefix.length())));
            return new JSONObject().put("format","fraynix.spatial-scene").put("version",1).put("units","m").put("frame","right-handed-y-up")
                    .put("title",metadata.getString("title")).put("gravityMps2",metadata.get("gravityMps2"))
                    .put("source",source).put("scaleReference",metadata.getJSONObject("scaleReference")).put("objects",boxes);
        }

        void visit(int index, Transform parent, int depth) {
            if (depth > 24 || !visited.add(index) || index < 0 || index >= nodes.length())
                throw new IllegalArgumentException("GLB hierarchy contains an invalid, repeated or too-deep node.");
            var node = nodes.getJSONObject(index);
            if (node.has("skin") || node.has("weights")) throw new IllegalArgumentException("Unsupported deformed GLB node.");
            var world = parent.then(Transform.node(node));
            var extras = node.optJSONObject("extras",new JSONObject());
            if (extras.has(METADATA)) {
                if (metadata != null || node.has("mesh")) throw new IllegalArgumentException("Choose one scene metadata empty.");
                metadata = new JSONObject(extras.getString(METADATA));
            }
            if (node.has("mesh")) {
                if (!extras.has(BODY)) throw new IllegalArgumentException("Every box needs its Fraynix body properties.");
                if (boxes.length() == 32) throw new IllegalArgumentException("GLB exceeds the 32-box spatial budget.");
                var body = new JSONObject(extras.getString(BODY));
                SpatialScene.keys(body,"sourceId","label","massKg","friction","restitution");
                var bounds = bounds(count(node,"mesh",0,root.getJSONArray("meshes").length()-1));
                boxes.put(world.box(body,bounds));
            }
            var children = node.optJSONArray("children",new JSONArray());
            for (int i = 0; i < children.length(); i++) visit(index(children,i),world,depth+1);
        }

        Bounds bounds(int id) {
            var mesh = root.getJSONArray("meshes").getJSONObject(id);
            var pieces = mesh.getJSONArray("primitives");
            if (pieces.length() != 1 || mesh.has("weights")) throw new IllegalArgumentException("Use one triangle primitive per box.");
            var primitive = pieces.getJSONObject(0);
            if ((primitive.has("mode") && count(primitive,"mode",0,6)!=4)
                    || primitive.has("targets") || primitive.has("extensions"))
                throw new IllegalArgumentException("Only uncompressed triangle boxes can become native colliders.");
            var attrs = primitive.getJSONObject("attributes");
            var positions = accessor(attrs,"POSITION","VEC3",12,5126);
            if (positions.count < 8 || positions.count > 256 || positions.component != 5126)
                throw new IllegalArgumentException("GLB boxes require 8–256 float positions.");
            var vertices = new double[positions.count][3];
            var low = new double[]{Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY};
            var high = new double[]{Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
            for (int i = 0; i < vertices.length; i++) for (int axis = 0; axis < 3; axis++) {
                double value = bytes.getFloat(positions.start+i*positions.stride+axis*4);
                if (!Double.isFinite(value) || Math.abs(value)>100_000) throw new IllegalArgumentException("Invalid GLB vertex.");
                vertices[i][axis] = value;
                low[axis] = Math.min(low[axis],value);
                high[axis] = Math.max(high[axis],value);
            }
            int[] codes = corners(vertices,low,high);
            var triangles = accessor(primitive,"indices","SCALAR",0,5123,5121,5125);
            if (triangles.count != 36) throw new IllegalArgumentException("A GLB box needs exactly 12 indexed triangles.");
            verifyFaces(codes,triangles);
            return new Bounds(low,high);
        }

        private static int[] corners(double[][] vertices, double[] low, double[] high) {
            int[] codes = new int[vertices.length];
            for (int i = 0; i < vertices.length; i++) for (int axis = 0; axis < 3; axis++) {
                if (high[axis]-low[axis] <= 0) throw new IllegalArgumentException("Collapsed GLB box.");
                double tolerance = (high[axis]-low[axis])*EPS;
                if (Math.abs(vertices[i][axis]-high[axis]) <= tolerance) codes[i] |= 1 << axis;
                else if (Math.abs(vertices[i][axis]-low[axis]) > tolerance)
                    throw new IllegalArgumentException("Mesh has non-box vertices. Keep the source mesh and export a box collider.");
            }
            return codes;
        }

        private void verifyFaces(int[] codes, Access indices) {
            int[] trianglesPerFace = new int[6], unions = new int[6], first = new int[6];
            for (int triangle = 0; triangle < 12; triangle++) {
                int corners = 0;
                for (int j=0;j<3;j++) {
                    int at = indices.start+(triangle*3+j)*indices.stride;
                    int value = switch (indices.component) {
                        case 5121 -> Byte.toUnsignedInt(bytes.get(at));
                        case 5123 -> Short.toUnsignedInt(bytes.getShort(at));
                        default -> bytes.getInt(at);
                    };
                    if (value<0 || value>=codes.length) throw new IllegalArgumentException("GLB index points outside the positions.");
                    corners |= 1 << codes[value];
                }
                int face = face(corners);
                if (face < 0 || trianglesPerFace[face] >= 2) throw new IllegalArgumentException("Mesh triangles do not cover the box faces.");
                if (trianglesPerFace[face] == 0) first[face] = corners;
                else if (!diagonal(corners & first[face]))
                    throw new IllegalArgumentException("Box face triangles do not meet on a diagonal.");
                trianglesPerFace[face]++;
                unions[face] |= corners;
            }
            for (int f=0;f<6;f++) if (trianglesPerFace[f]!=2 || Integer.bitCount(unions[f])!=4)
                throw new IllegalArgumentException("Each box face needs two triangles covering its four corners.");
        }

        private static int face(int corners) {
            if (Integer.bitCount(corners)!=3) return -1;
            for (int axis=0;axis<3;axis++) {
                int upper=0;
                for (int corner=0;corner<8;corner++) if ((corner & (1<<axis))!=0) upper |= 1<<corner;
                if ((corners & upper)==0) return 2*axis;
                if ((corners & upper)==corners) return 2*axis+1;
            }
            return -1;
        }
        private static boolean diagonal(int corners) {
            if (Integer.bitCount(corners)!=2) return false;
            int first = Integer.numberOfTrailingZeros(corners);
            int second = Integer.numberOfTrailingZeros(corners & (corners-1));
            return Integer.bitCount(first ^ second)==2;
        }

        private record Access(int component, int start, int count, int stride) {}

        private Access accessor(JSONObject owner,String key,String type,int width,int... components) {
            var list = root.getJSONArray("accessors");
            var a = list.getJSONObject(count(owner,key,0,list.length()-1));
            int component = count(a,"componentType",5120,5126);
            if (!type.equals(a.getString("type")) || Arrays.stream(components).noneMatch(c->c==component)
                    || a.has("sparse") || a.optBoolean("normalized"))
                throw new IllegalArgumentException("Unsupported GLB accessor.");
            var listViews = root.getJSONArray("bufferViews");
            var view = listViews.getJSONObject(count(a,"bufferView",0,listViews.length()-1));
            count(view,"buffer",0,0);
            int widthBytes = width != 0 ? width : component == 5121 ? 1 : component == 5123 ? 2 : 4;
            if (width == 0 && view.has("byteStride")) throw new IllegalArgumentException("Index arrays must be tightly packed.");
            int stride = view.has("byteStride") ? count(view,"byteStride",widthBytes,252) : widthBytes;
            int base = offset(view,"byteOffset",bytes.capacity()), length = count(view,"byteLength",1,bytes.capacity());
            int relative = offset(a,"byteOffset",length), size = count(a,"count",1,4096);
            int alignment = width == 0 ? widthBytes : 4;
            if (base+length>bytes.capacity() || relative+(size-1)*stride+widthBytes>length
                    || (base+relative)%alignment!=0 || stride%alignment!=0)
                throw new IllegalArgumentException("GLB accessor is misaligned or leaves its buffer view.");
            return new Access(component,base+relative,size,stride);
        }
    }

    private record Bounds(double[] low, double[] high) {}

    /** Affine column basis and translation in glTF's right-handed Y-up coordinates. */
    private record Transform(double[][] columns, double[] origin) {
        static Transform identity() { return new Transform(new double[][]{{1,0,0},{0,1,0},{0,0,1}}, new double[3]); }
        static Transform node(JSONObject node) {
            if (node.has("matrix")) {
                if (node.has("translation") || node.has("rotation") || node.has("scale"))
                    throw new IllegalArgumentException("GLB node must use either matrix or TRS.");
                var m=node.getJSONArray("matrix");
                if (m.length()!=16 || m.getDouble(3)!=0 || m.getDouble(7)!=0 || m.getDouble(11)!=0 || m.getDouble(15)!=1)
                    throw new IllegalArgumentException("Use an affine GLB node matrix.");
                return new Transform(new double[][]{column(m,0),column(m,4),column(m,8)},column(m,12));
            }
            var q = node.optJSONArray("rotation",new JSONArray(new int[]{0,0,0,1}));
            if (q.length()!=4) throw new IllegalArgumentException("GLB needs a quaternion.");
            double x=q.getDouble(0),y=q.getDouble(1),z=q.getDouble(2),w=q.getDouble(3);
            if (Math.abs(x*x+y*y+z*z+w*w-1)>EPS || !Double.isFinite(x+y+z+w))
                throw new IllegalArgumentException("GLB rotation is not a unit quaternion.");
            var s=SpatialScene.vector(node.optJSONArray("scale",new JSONArray(new int[]{1,1,1})),100_000);
            var basis=new double[][]{{1-2*(y*y+z*z),2*(x*y+w*z),2*(x*z-w*y)},
                    {2*(x*y-w*z),1-2*(x*x+z*z),2*(y*z+w*x)},{2*(x*z+w*y),2*(y*z-w*x),1-2*(x*x+y*y)}};
            for(int i=0;i<3;i++) for (int axis=0;axis<3;axis++) basis[i][axis]*=s[i];
            return new Transform(basis,SpatialScene.vector(node.optJSONArray("translation",new JSONArray(new int[3])),100_000));
        }
        private static double[] column(JSONArray matrix,int k) {
            return SpatialScene.vector(new JSONArray().put(matrix.get(k)).put(matrix.get(k+1)).put(matrix.get(k+2)),100_000);
        }
        Transform then(Transform child) {
            return new Transform(new double[][]{apply(child.columns[0]),apply(child.columns[1]),apply(child.columns[2])},
                    ObserverGeometry.add(origin,apply(child.origin)));
        }
        double[] apply(double[] point) {
            var result=new double[3];
            for(int i=0;i<3;i++) for(int j=0;j<3;j++) result[i]+=columns[j][i]*point[j];
            return result;
        }
        JSONObject box(JSONObject body,Bounds bounds) {
            var basis=new double[3][3];
            double[] size=new double[3], center=new double[3];
            for(int i=0;i<3;i++) {
                double scale=ObserverGeometry.norm(columns[i]);
                if (scale<=EPS || !Double.isFinite(scale)) throw new IllegalArgumentException("Collapsed GLB transform.");
                for(int j=0;j<3;j++) basis[i][j]=columns[i][j]/scale;
                center[i]=(bounds.high[i]+bounds.low[i])/2;
                size[i]=(bounds.high[i]-bounds.low[i])*scale;
            }
            if (Math.abs(ObserverGeometry.dot(basis[0],basis[1]))>EPS || Math.abs(ObserverGeometry.dot(basis[0],basis[2]))>EPS
                    || Math.abs(ObserverGeometry.dot(basis[1],basis[2]))>EPS
                    || ObserverGeometry.dot(cross(basis[0],basis[1]),basis[2]) < 1-EPS)
                throw new IllegalArgumentException("Reflected or sheared GLB colliders cannot become oriented boxes.");
            return body.put("shape","box").put("size",new JSONArray(size))
                    .put("position",new JSONArray(ObserverGeometry.add(origin,apply(center))))
                    .put("rotation",new JSONArray(quaternion(basis)));
        }

        private static double[] cross(double[] a, double[] b) {
            return new double[]{a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0]};
        }

        private static double[] quaternion(double[][] m) {
            double x=m[0][0], y=m[1][1], z=m[2][2];
            double[] q={Math.sqrt(Math.max(0,1+x-y-z))/2,Math.sqrt(Math.max(0,1-x+y-z))/2,
                    Math.sqrt(Math.max(0,1-x-y+z))/2,Math.sqrt(Math.max(0,1+x+y+z))/2};
            int lead=0;
            for(int i=1;i<4;i++) if(q[i]>q[lead]) lead=i;
            double d=4*q[lead];
            switch (lead) {
                case 0 -> { q[1]=(m[0][1]+m[1][0])/d; q[2]=(m[0][2]+m[2][0])/d; q[3]=(m[1][2]-m[2][1])/d; }
                case 1 -> { q[0]=(m[0][1]+m[1][0])/d; q[2]=(m[1][2]+m[2][1])/d; q[3]=(m[2][0]-m[0][2])/d; }
                case 2 -> { q[0]=(m[0][2]+m[2][0])/d; q[1]=(m[1][2]+m[2][1])/d; q[3]=(m[0][1]-m[1][0])/d; }
                default -> { q[0]=(m[1][2]-m[2][1])/d; q[1]=(m[2][0]-m[0][2])/d; q[2]=(m[0][1]-m[1][0])/d; }
            }
            return q;
        }
    }
}
