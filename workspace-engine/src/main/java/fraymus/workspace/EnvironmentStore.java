package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

/** Retains self-contained visual environments beside, and independently of, simulated worlds. */
final class EnvironmentStore {
    static final int MAX_BYTES = 16 * 1024 * 1024;
    private static final Set<String> PNG_JPEG = Set.of("image/png", "image/jpeg");
    private static final Set<String> REQUIRED_EXTENSIONS = Set.of("KHR_texture_transform",
            "KHR_materials_emissive_strength", "KHR_materials_unlit", "KHR_lights_punctual");
    private final Path root;

    EnvironmentStore(Path project) {
        root = project.resolve("environments");
    }

    synchronized JSONObject importFile(JSONObject input) throws Exception {
        SpatialScene.keys(input, "filename", "base64");
        String name = SpatialScene.text(input, "filename", 150);
        if (!name.toLowerCase(java.util.Locale.ROOT).endsWith(".glb") || name.matches(".*[/\\\\].*"))
            throw new IllegalArgumentException("Choose a local .glb file.");
        byte[] bytes = SpriteWorkshop.decode(input.getString("base64"), MAX_BYTES);
        String id = MapDefinition.sha(bytes);
        // Complete validation precedes any publication into the project.
        JSONObject catalog = parse(bytes).put("id", id).put("bytes", bytes.length).put("name", name);
        Path file = file(id);
        Files.createDirectories(root);
        if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
            SpriteWorkshop.replace(file, bytes);
            AgentFiles042.saveChecked(root.resolve(id + ".json"), catalog);
        } else {
            // A previous process can stop after storing the immutable bytes.
            SpriteWorkshop.read(file, MAX_BYTES, id);
            Path saved = root.resolve(id + ".json");
            if (!Files.exists(saved, LinkOption.NOFOLLOW_LINKS)) AgentFiles042.saveChecked(saved, catalog);
            catalog = retained(id);
        }
        return catalog;
    }

    synchronized JSONObject activate(JSONObject input) throws Exception {
        var catalog=open(input,false);
        String id=catalog.getString("id");
        SpriteWorkshop.replace(root.resolve("latest"), id.getBytes(StandardCharsets.US_ASCII));
        return catalog;
    }

    synchronized JSONObject open(JSONObject input, boolean includeBytes) throws Exception {
        SpatialScene.keys(input, "id");
        String id = input.optString("id", "");
        if (id.isEmpty()) {
            Path latest = root.resolve("latest");
            if (!Files.exists(latest, LinkOption.NOFOLLOW_LINKS)) return new JSONObject();
            id = new String(SpriteWorkshop.read(latest,64), StandardCharsets.US_ASCII);
        }
        var catalog = retained(id);
        byte[] bytes = SpriteWorkshop.read(file(id),MAX_BYTES,id);
        if (includeBytes) catalog.put("base64", Base64.getEncoder().encodeToString(bytes));
        Path cursor = root.resolve(id + ".selection");
        if (Files.exists(cursor, LinkOption.NOFOLLOW_LINKS)) {
            var selection = AgentFiles042.loadChecked(cursor);
            if (!id.equals(selection.getString("id"))) throw new IOException("Environment selection changed.");
            int node = selection.getInt("node");
            requireNode(catalog, node);
            catalog.put("selected", node);
        }
        return catalog;
    }

    synchronized JSONObject select(JSONObject input) throws Exception {
        SpatialScene.keys(input,"id","node");
        String id = input.getString("id");
        var catalog = open(new JSONObject().put("id",id), false);
        int node = SpriteWorkshop.integer(input,"node",0,4095);
        requireNode(catalog,node);
        var selection = new JSONObject().put("id",id).put("node",node);
        AgentFiles042.saveChecked(root.resolve(id + ".selection"), selection);
        return selection;
    }

    private static void requireNode(JSONObject catalog, int index) {
        for (Object value : catalog.getJSONArray("nodes"))
            if (((JSONObject)value).getInt("index") == index) return;
        throw new IllegalArgumentException("Choose a node in the displayed scene.");
    }

    private JSONObject retained(String id) throws IOException {
        file(id);
        var catalog = AgentFiles042.loadChecked(root.resolve(id + ".json"));
        if (!id.equals(catalog.getString("id"))) throw new IOException("Environment identity changed.");
        return catalog;
    }

    private Path file(String id) {
        if (!id.matches("[a-f0-9]{64}")) throw new IllegalArgumentException("Invalid environment ID.");
        return root.resolve(id + ".glb");
    }

    static JSONObject parse(byte[] bytes) throws Exception {
        var buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        require(bytes.length >= 28 && bytes.length <= MAX_BYTES && buffer.getInt() == 0x46546c67
                && buffer.getInt() == 2 && buffer.getInt() == bytes.length,"Use a complete binary glTF version 2 file.");
        int jsonLength = buffer.getInt();
        require(buffer.getInt() == 0x4e4f534a && jsonLength > 0 && jsonLength % 4 == 0
                && jsonLength <= buffer.remaining()-8,"The GLB JSON chunk is incomplete.");
        byte[] json = new byte[jsonLength];
        buffer.get(json);
        int binaryLength = buffer.getInt();
        require(buffer.getInt() == 0x004e4942 && binaryLength == buffer.remaining() && binaryLength % 4 == 0,
                "Use a single embedded GLB binary chunk.");
        int binary = buffer.position();
        var data = new JSONObject(new String(json,StandardCharsets.UTF_8).stripTrailing());
        require(data.getJSONObject("asset").getString("version").equals("2.0"),"Use glTF 2.0.");
        for (Object extension : data.optJSONArray("extensionsRequired",new JSONArray()))
            require(REQUIRED_EXTENSIONS.contains(extension),"Unsupported required GLB extension: " + extension);
        var buffers = data.getJSONArray("buffers");
        require(buffers.length()==1 && !buffers.getJSONObject(0).has("uri"),"The environment must embed its binary payload.");
        int payload = SpriteWorkshop.integer(buffers.getJSONObject(0),"byteLength",1,binaryLength);
        require(binaryLength-payload<=3,"The binary payload length differs from its declaration.");
        var views = array(data,"bufferViews",24_000);
        for (Object value : views) {
            var view = (JSONObject)value;
            int offset = optionalInteger(view,"byteOffset",0,payload,0);
            int length = SpriteWorkshop.integer(view,"byteLength",1,payload);
            require(SpriteWorkshop.integer(view,"buffer",0,0)==0 && offset+length<=payload,
                    "A GLB buffer view extends outside its payload.");
        }
        var images = array(data,"images",64);
        long pixels = 0;
        for (Object value : images) {
            var image = (JSONObject)value;
            require(!image.has("uri") && PNG_JPEG.contains(image.optString("mimeType")),
                    "Embed PNG or JPEG textures in the GLB.");
            var part = views.getJSONObject(SpriteWorkshop.integer(image,"bufferView",0,views.length()-1));
            int offset = binary + part.optInt("byteOffset");
            int length = part.getInt("byteLength");
            try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes,offset,length))) {
                var readers = ImageIO.getImageReaders(input);
                require(readers.hasNext(),"An embedded texture could not be read.");
                var reader = readers.next();
                try {
                    reader.setInput(input,true,true);
                    String format = reader.getFormatName();
                    require(image.getString("mimeType").equals("image/png")?format.equalsIgnoreCase("png"):format.equalsIgnoreCase("jpeg"),
                            "An embedded texture differs from its declared type.");
                    int w = reader.getWidth(0), h = reader.getHeight(0);
                    require(w >= 1 && h >= 1 && w <= 4096 && h <= 4096,"An embedded texture exceeds 4096 pixels per side.");
                    pixels += (long)w*h;
                    require(pixels <= 45_000_000,"The embedded textures exceed the environment pixel limit.");
                } finally {
                    reader.dispose();
                }
            }
        }
        var textures = array(data,"textures",256);
        for (Object value : textures) SpriteWorkshop.integer((JSONObject)value,"source",0,images.length()-1);
        var accessors = array(data,"accessors",24_000);
        for (Object value : accessors) {
            var accessor = (JSONObject)value;
            int count = SpriteWorkshop.integer(accessor,"count",1,2_250_000);
            int components = switch(accessor.getString("type")) {
                case "SCALAR" -> 1; case "VEC2" -> 2; case "VEC3" -> 3; case "VEC4", "MAT2" -> 4;
                case "MAT3" -> 9; case "MAT4" -> 16; default -> 0;
            };
            int bytesPerComponent = switch(accessor.getInt("componentType")) {
                case 5120, 5121 -> 1; case 5122, 5123 -> 2; case 5125, 5126 -> 4; default -> 0;
            };
            require(components > 0 && bytesPerComponent > 0 &&
                    (!accessor.getString("type").startsWith("MAT") || bytesPerComponent==4),
                    "An accessor uses an unsupported shape or component format.");
            checkAccessor(accessor,views,count,components*bytesPerComponent);
        }
        var meshes = array(data,"meshes",4096);
        var triangleCounts = new ArrayList<Integer>();
        for (Object value : meshes) {
            var mesh = (JSONObject)value;
            var primitives = array(mesh,"primitives",128);
            require(!primitives.isEmpty(),"A mesh contains no primitives.");
            int triangles = 0;
            for (Object raw : primitives) {
                var primitive = (JSONObject)raw;
                require(optionalInteger(primitive,"mode",0,6,4)==4,"Environment meshes use triangles.");
                int positionIndex = SpriteWorkshop.integer(primitive.getJSONObject("attributes"),"POSITION",0,accessors.length()-1);
                var position = accessors.getJSONObject(positionIndex);
                int count = SpriteWorkshop.integer(position,"count",1,750_000);
                require(position.getString("type").equals("VEC3") && position.getInt("componentType")==5126,
                        "Use float VEC3 mesh positions.");
                int positions=count;
                checkedPositions(buffer,binary,views,position,positions);
                if (primitive.has("indices")) {
                    var index = accessors.getJSONObject(SpriteWorkshop.integer(primitive,"indices",0,accessors.length()-1));
                    count = SpriteWorkshop.integer(index,"count",1,2_250_000);
                    int width = switch(index.getInt("componentType")) {case 5121 -> 1; case 5123 -> 2; case 5125 -> 4; default -> 0;};
                    require(width > 0 && index.getString("type").equals("SCALAR"),"Use unsigned mesh indices.");
                    checkedIndices(buffer,binary,views,index,count,width,positions);
                }
                require(count % 3==0,"A triangle primitive has an incomplete face.");
                triangles += count/3;
                require(triangles<=750_000,"A mesh exceeds the environment triangle limit.");
            }
            triangleCounts.add(triangles);
        }
        var nodes = array(data,"nodes",4096);
        var scenes = array(data,"scenes",32);
        require(!scenes.isEmpty(),"The GLB has no displayable scene.");
        int active = optionalInteger(data,"scene",0,scenes.length()-1,0);
        for (int i=0;i<scenes.length();i++) if (i!=active) {
            var other = new JSONArray();
            Set<Integer> visited = new HashSet<>();
            for (Object value : array(scenes.getJSONObject(i),"nodes",4096))
                walk(nodes,triangleCounts,index(value,nodes.length()),-1,0,visited,other);
        }
        var def = scenes.getJSONObject(active);
        var rows = new JSONArray();
        var seen = new HashSet<Integer>();
        int triangles=0, meshNodes=0, markers=0;
        for (Object value : array(def,"nodes",4096)) walk(nodes, triangleCounts, index(value,nodes.length()), -1, 0, seen, rows);
        require(!rows.isEmpty(),"The default GLB scene is empty.");
        for (Object value : rows) {
            var node = (JSONObject)value;
            if (node.has("mesh")) meshNodes++;
            if (node.has("marker")) markers++;
            triangles += node.getInt("triangles");
            require(triangles<=750_000,"The default scene exceeds 750,000 triangles.");
        }
        require(meshNodes>0,"The default scene contains no triangle meshes.");
        return new JSONObject().put("format","fraynix.environment-glb-v1")
                .put("units","glTF metres").put("frame","right-handed-y-up")
                .put("triangleCount",triangles).put("meshNodes",meshNodes).put("markerCount",markers)
                .put("imageCount",images.length()).put("textureCount",textures.length()).put("nodes",rows);
    }

    private static void walk(JSONArray nodes, ArrayList<Integer> meshes,int index,int parent,int depth,
                             Set<Integer> seen, JSONArray rows) {
        require(depth <= 80 && seen.add(index),"The GLB node graph contains a cycle, duplicate instance or excessive depth.");
        var node = nodes.getJSONObject(index);
        vector(node,"translation",3); vector(node,"rotation",4);
        vector(node,"scale",3); vector(node,"matrix",16);
        var row = new JSONObject().put("index",index).put("parent",parent)
                .put("name",limitedLabel(node.optString("name"),"Node "+index)).put("triangles",0);
        if (node.has("mesh")) {
            int mesh=SpriteWorkshop.integer(node,"mesh",0,meshes.size()-1);
            row.put("mesh",mesh).put("triangles",meshes.get(mesh));
        }
        var extras=node.optJSONObject("extras");
        if (extras != null && extras.opt("escape_label") instanceof String label && !label.isBlank())
            row.put("marker",limitedLabel(label,"Interaction marker"));
        rows.put(row);
        for (Object child : array(node,"children",4096)) walk(nodes,meshes,index(child,nodes.length()),index,depth+1,seen,rows);
    }

    private static void checkedPositions(ByteBuffer data,int binary,JSONArray views,JSONObject accessor,int count) {
        var view=views.getJSONObject(accessor.getInt("bufferView"));
        int start=binary+view.optInt("byteOffset")+accessor.optInt("byteOffset");
        int stride=view.optInt("byteStride",12);
        for (int i=0;i<count;i++) for (int axis=0;axis<3;axis++) {
            float value=data.getFloat(start+i*stride+axis*4);
            require(Float.isFinite(value) && Math.abs(value)<=10_000_000,"A mesh contains an invalid position.");
        }
    }

    private static void checkedIndices(ByteBuffer data,int binary,JSONArray views,JSONObject accessor,int count,int width,int positions) {
        var view=views.getJSONObject(accessor.getInt("bufferView"));
        int start=binary+view.optInt("byteOffset")+accessor.optInt("byteOffset");
        int stride=view.optInt("byteStride",width);
        for (int i=0;i<count;i++) {
            int at=start+i*stride;
            long index=switch(width) {
                case 1 -> Byte.toUnsignedInt(data.get(at));
                case 2 -> Short.toUnsignedInt(data.getShort(at));
                default -> Integer.toUnsignedLong(data.getInt(at));
            };
            require(index<positions,"A triangle refers to a missing vertex.");
        }
    }

    private static void vector(JSONObject node, String field, int count) {
        if (!node.has(field)) return;
        var vector = node.getJSONArray(field);
        require(vector.length()==count,"A scene transform has an invalid vector length.");
        for (Object value : vector) require(value instanceof Number n && Double.isFinite(n.doubleValue()) &&
                Math.abs(n.doubleValue()) <= 10_000_000,"A scene transform is outside its finite range.");
    }

    private static void checkAccessor(JSONObject accessor,JSONArray views,int count,int width) {
        require(!accessor.has("sparse"),"Use regular buffer-backed mesh accessors.");
        var view = views.getJSONObject(SpriteWorkshop.integer(accessor,"bufferView",0,views.length()-1));
        int limit = view.getInt("byteLength");
        int offset = optionalInteger(accessor,"byteOffset",0,limit,0);
        int stride = optionalInteger(view,"byteStride",width,252,width);
        require(offset+(long)(count-1)*stride+width<=limit,"A mesh accessor extends outside its buffer view.");
    }

    private static JSONArray array(JSONObject data,String key,int cap) {
        var array=data.optJSONArray(key,new JSONArray());
        require(array.length()<=cap,"Too many GLB "+key+".");
        return array;
    }

    private static int optionalInteger(JSONObject data,String key,int min,int max,int fallback) {
        return data.has(key)?SpriteWorkshop.integer(data,key,min,max):fallback;
    }

    private static int index(Object value,int limit) {
        require(value instanceof Number n && n.doubleValue()==n.intValue() && n.intValue()>=0 && n.intValue()<limit,
                "A scene node index is invalid.");
        return ((Number)value).intValue();
    }

    private static String limitedLabel(String raw,String fallback) {
        var clean = raw.replaceAll("[\\p{Cntrl}]", " ").strip();
        return clean.isEmpty()?fallback:clean.substring(0,Math.min(140,clean.length()));
    }

    private static void require(boolean valid,String message) {
        if (!valid) throw new IllegalArgumentException(message);
    }
}
