package fraymus.workspace;

import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;
import java.util.HashSet;

/** A bounded indexed triangle surface expressed in metres in its collider's local frame. */
record SpatialVisualMesh(float[] positions, int[] indices) {
    static final int MAX_VERTICES = 4096;
    static final int MAX_INDICES = 12_288;

    int vertexCount() { return positions.length / 3; }

    byte[] bytes() {
        var out = ByteBuffer.allocate(8 + positions.length * 4 + indices.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        out.putInt(vertexCount()).putInt(indices.length);
        for (float value : positions) out.putFloat(value == 0 ? 0 : value);
        for (int index : indices) out.putShort((short) index);
        return out.array();
    }

    static SpatialVisualMesh read(String base64) {
        byte[] bytes = Base64.getDecoder().decode(base64);
        if (bytes.length < 8) throw new IllegalArgumentException("The visual mesh header is missing.");
        var data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int vertices = data.getInt(), indices = data.getInt();
        if (vertices < 3 || vertices > MAX_VERTICES || indices < 3 || indices > MAX_INDICES || indices % 3 != 0
                || bytes.length != 8 + vertices * 12 + indices * 2)
            throw new IllegalArgumentException("The visual mesh dimensions are outside the triangle budget.");
        float[] points = new float[vertices*3];
        int[] elements = new int[indices];
        for (int i=0;i<points.length;i++) {
            points[i]=data.getFloat();
            if (!Float.isFinite(points[i]) || Math.abs(points[i])>20) throw new IllegalArgumentException("Visual vertices must be finite and within 20 m of the collider.");
        }
        for (int i=0;i<elements.length;i++) {
            elements[i]=Short.toUnsignedInt(data.getShort());
            if (elements[i]>=vertices) throw new IllegalArgumentException("The visual triangle indexes a missing vertex.");
        }
        return new SpatialVisualMesh(points,elements);
    }

    String encoded() { return Base64.getEncoder().encodeToString(bytes()); }
    String key() { return MapDefinition.sha(bytes()); }

    static void validate(JSONObject scene, JSONObject assets) {
        var used = new HashSet<String>();
        for (Object entry : scene.getJSONArray("objects")) {
            var body = (JSONObject)entry;
            if (body.has("visualMesh")) used.add(body.getString("visualMesh"));
        }
        if (!used.equals(assets.keySet())) throw new IllegalArgumentException("Every detailed prop must refer to an exact visual asset.");
        for (String id : used) if (!read(assets.getString(id)).key().equals(id))
            throw new IllegalArgumentException("The visual mesh data changed.");
    }

    /** Select a real triangle centroid outside the collider, when the surface has an overhang. */
    double[] overhang(double[] size) {
        double max = .12;
        double[] result = null;
        for (int i=0;i<indices.length;i+=3) {
            var center = new double[3];
            for (int j=0;j<3;j++) for (int axis=0;axis<3;axis++)
                center[axis] += positions[indices[i+j]*3+axis]/3.0;
            for (int axis=0;axis<3;axis++) {
                double excess=Math.abs(center[axis])-size[axis]/2;
                if (excess>max) {max=excess;result=center;}
            }
        }
        return result;
    }

    boolean closeTo(SpatialVisualMesh other) {
        if (positions.length!=other.positions.length || !java.util.Arrays.equals(indices,other.indices)) return false;
        for (int i=0;i<positions.length;i++) if (Math.abs(positions[i]-other.positions[i])>1e-5) return false;
        return true;
    }
}
