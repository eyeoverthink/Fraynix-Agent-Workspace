package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.Assert.*;

public class SpatialPropExchangeTest {
    private static byte[] fixture() throws Exception {
        try (var input = SpatialPropExchangeTest.class.getResourceAsStream("/spatial/edition217-beacon.glb")) {
            assertNotNull(input);
            return input.readAllBytes();
        }
    }

    private static JSONObject beacon(JSONObject scene) {
        for (Object item : scene.getJSONArray("objects")) {
            var body=(JSONObject)item;
            if ("beacon".equals(body.getString("sourceId"))) return body;
        }
        throw new AssertionError("The Blender beacon is missing.");
    }

    @Test public void retainsOnlyTheDeclaredBodiesAndRoundTripsTheVisualTriangles() throws Exception {
        var definition=SpatialGlb.importScene(fixture());
        var id=UUID.randomUUID().toString();
        var scene=SpatialScene.parse(definition.toString(),id);
        var body=beacon(scene);
        var mesh=SpatialGlb.asset(scene,body);
        assertEquals(2,scene.getJSONArray("objects").length());
        assertTrue(mesh.indices().length/3>500);
        assertNotNull(mesh.overhang(ObserverGeometry.v(body.getJSONArray("size"))));
        var exported=SpatialGlb.verifiedExport(scene);
        var restored=SpatialScene.parse(SpatialGlb.importScene(exported).toString(),id);
        assertTrue(mesh.closeTo(SpatialGlb.asset(restored,beacon(restored))));
        var assets=definition.getJSONObject("visualMeshes");
        String key=body.getString("visualMesh");
        byte[] changed=mesh.bytes();
        changed[changed.length-1]=(byte)255;
        assets.put(key,java.util.Base64.getEncoder().encodeToString(changed));
        assertThrows(IllegalArgumentException.class,()->SpatialScene.parse(definition.toString(),id));
    }

    @Test public void approvesOnlyTheReviewedCopyAndRechecksNativeContactsAndRetainedEvidence() throws Exception {
        Path project=Files.createTempDirectory("fraynix-prop-copy");
        String copyId;
        JSONObject receipt;
        try (var store=new SpatialStore(project)) {
            var original=store.importGlb(fixture());
            var body=beacon(original);
            var params=new JSONObject().put("sceneId",original.getString("id")).put("objectId",body.getString("id"))
                    .put("label","Beacon copy").put("yawDeg",35).put("offsetM",new JSONArray(new double[]{1.8,0,-.9}));
            var review=store.reviewProp(params);
            var wrong=new JSONObject().put("args",WorldFrames.copy(params).put("yawDeg",40))
                    .put("approvalSha256",review.getString("approvalSha256"));
            assertThrows(IllegalStateException.class,()->store.copyProp(wrong));
            var blocked=WorldFrames.copy(params).put("offsetM",new JSONArray(new double[]{.16,0,0}));
            var blockReview=store.reviewProp(blocked);
            assertThrows(IllegalStateException.class,()->store.copyProp(new JSONObject().put("args",blocked)
                    .put("approvalSha256",blockReview.getString("approvalSha256"))));
            assertEquals(1,store.list().length());
            var approval=new JSONObject().put("args",params).put("approvalSha256",review.getString("approvalSha256"));
            var copy=store.copyProp(approval);
            copyId=copy.getString("id");
            assertEquals(3,copy.getJSONArray("objects").length());
            assertEquals(1,copy.getJSONObject("visualMeshes").length());
            assertEquals(copyId,store.copyProp(approval).getString("id"));
            receipt=copy.getJSONObject("propCopy");
            var verify=receipt.getJSONObject("verify");
            assertTrue(verify.similar(receipt.getJSONObject("replay")));
            assertEquals("floor",verify.getString("support"));
            assertTrue(verify.getBoolean("visualOverhangTested"));
            assertTrue(verify.getInt("coreContacts")>0);
            assertEquals(0,verify.getInt("visualOnlyContacts"));
            assertTrue(original.similar(store.read(original.getString("id"))));
        }
        try (var reopened=new SpatialStore(project)) {
            assertTrue(receipt.similar(reopened.read(copyId).getJSONObject("propCopy")));
        }
        Path state=project.resolve("spatial/state.json");
        var changed=new JSONObject(Files.readString(state));
        var copy=changed.getJSONArray("scenes").getJSONObject(1).getJSONObject("propCopy");
        copy.getJSONObject("verify").put("coreContacts",99);
        Files.writeString(state,changed.toString());
        try (var reopened=new SpatialStore(project)) {
            assertThrows(IllegalStateException.class,()->reopened.read(copyId));
        }
    }
}
