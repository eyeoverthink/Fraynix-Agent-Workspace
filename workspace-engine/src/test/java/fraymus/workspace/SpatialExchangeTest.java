package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.Assert.*;

public class SpatialExchangeTest {
    @Test public void convertsMetricBoxesAndChecksNestedTransformsAndIndices() throws Exception {
        var raw = new JSONObject(new String(getClass().getResourceAsStream("/spatial/declared-test-scene-v1.json")
                .readAllBytes(),StandardCharsets.UTF_8));
        var box = raw.getJSONArray("objects").getJSONObject(2);
        box.put("rotation",new JSONArray(new double[]{0,Math.sqrt(.5),0,Math.sqrt(.5)}));
        var source = SpatialScene.parse(raw.toString(), UUID.randomUUID().toString());
        byte[] glb = SpatialGlb.verifiedExport(source);
        var imported = SpatialGlb.importScene(glb);
        assertEquals("m", imported.getString("units"));
        assertEquals(1,imported.getJSONObject("scaleReference").getDouble("knownLengthM"),1e-9);
        var changed = SpatialGlb.importScene(rewrite(glb,root -> {
            var nodes=root.getJSONArray("nodes");
            int index=nodes.length();
            nodes.put(new JSONObject().put("translation",new JSONArray(new double[]{.25,0,-.5}))
                    .put("children",new JSONArray().put(3)));
            root.getJSONArray("scenes").getJSONObject(0).getJSONArray("nodes").put(3,index);
        }));
        var moved = changed.getJSONArray("objects").getJSONObject(2).getJSONArray("position");
        var prior = imported.getJSONArray("objects").getJSONObject(2).getJSONArray("position");
        assertEquals(prior.getDouble(0)+.25,moved.getDouble(0),1e-9);
        assertEquals(prior.getDouble(2)-.5,moved.getDouble(2),1e-9);
        assertThrows(IllegalArgumentException.class, () -> SpatialGlb.importScene(rewrite(glb,root ->
                root.getJSONArray("nodes").getJSONObject(3).put("scale",new JSONArray(new double[]{-1,1,1})))));
        byte[] broken = glb.clone();
        // The final bytes are indices. Changing an index to 255 moves it outside the position accessor.
        broken[broken.length-1] = (byte) 255;
        assertThrows(IllegalArgumentException.class, () -> SpatialGlb.importScene(broken));
        assertThrows(IllegalArgumentException.class, () -> SpatialGlb.importScene(rewrite(glb,root ->
                root.getJSONArray("buffers").getJSONObject(0).put("uri","ignored.binf"))));
    }

    @Test public void convertsTheRetainedImageAtDeclaredScaleAndDetectsChangedPixels() throws Exception {
        var reference = MapDefinition.image(MapDefinition.example(1));
        var document = SpatialBlueprint.build(reference,.65);
        var scene = SpatialScene.parse(document.toString(),UUID.randomUUID().toString());
        assertEquals(16*.65,scene.getJSONObject("scaleCheck").getDouble("convertedLengthM"),1e-9);
        assertFalse(scene.getJSONObject("scaleCheck").getBoolean("independentlyCalibrated"));
        var objects = scene.getJSONArray("objects");
        assertEquals(1,objects.toList().stream().filter(o->((Number)((java.util.Map<?,?>)o).get("massKg")).doubleValue()>0).count());
        assertTrue(document.getJSONObject("source").getString("note").contains(reference.getString("imageSha256")));
        String replacement = MapDefinition.example(0).getString("pngBase64");
        assertThrows(RuntimeException.class, () -> SpatialBlueprint.build(reference.put("pngBase64",replacement),.65));
    }

    private static byte[] rewrite(byte[] glb,java.util.function.Consumer<JSONObject> edit) {
        var source=ByteBuffer.wrap(glb).order(ByteOrder.LITTLE_ENDIAN);
        source.position(12);
        int length=source.getInt();
        int kind=source.getInt();
        byte[] original=new byte[length];
        source.get(original);
        var root=new JSONObject(new String(original,StandardCharsets.UTF_8).strip());
        edit.accept(root);
        byte[] json=root.toString().getBytes(StandardCharsets.UTF_8);
        int padded=(json.length+3)&~3;
        int total=20+padded+source.remaining();
        var rebuilt=ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN)
                .putInt(0x46546c67).putInt(2).putInt(total).putInt(padded).putInt(kind).put(json);
        for(int i=json.length;i<padded;i++) rebuilt.put((byte)' ');
        return rebuilt.put(source).array();
    }
}
