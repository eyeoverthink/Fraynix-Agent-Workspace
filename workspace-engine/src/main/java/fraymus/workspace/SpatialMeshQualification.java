package fraymus.workspace;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Qualification of a saved browser-mesh edit against an independently registered observation. */
final class SpatialMeshQualification {
    private static final String POLICY = "registered-browser-edit-witness-v1";
    static final int MAX_CAPTURE_BYTES = 1_200_000;

    private SpatialMeshQualification() {}

    static JSONObject check(JSONObject original, JSONObject edited, String objectId, byte[] originals,
                            byte[] capture, int elevation) throws Exception {
        var proof = original.optJSONObject("reconstruction");
        if (proof == null || !BrowserViewTrace.POLICY.equals(proof.optString("policy")) || proof.has("viewSource"))
            throw new IllegalArgumentException("Save a direct edit of an original registered browser reconstruction first.");
        var change = edited.getJSONObject("meshEdit");
        var body = SpatialScene.object(edited, objectId);
        if (!body.getString("sourceId").equals(change.getString("sourceId")))
            throw new IllegalArgumentException("Select the prop changed by this saved edit.");
        var prior = SpatialScene.object(original, change.getString("objectId"));
        if (!prior.getString("visualMesh").equals(proof.getString("meshSha256"))
                || !prior.getString("visualMesh").equals(change.getString("sourceMeshSha256"))
                || !original.getString("sourceSha256").equals(change.getString("sourceSha256"))
                || !body.getJSONArray("rotation").similar(prior.getJSONArray("rotation")))
            throw new IllegalStateException("The edit and original registered pose no longer match.");
        var mesh = SpatialGlb.asset(edited, body);
        if (mesh.key().equals(prior.getString("visualMesh")))
            throw new IllegalArgumentException("The saved edit must change the visual mesh.");
        SpatialFaceExtrusion.requireClosed(mesh);
        var bounds = SpatialScene.vector(body.getJSONArray("size"), 40);
        for (int i = 0; i < mesh.positions().length; i++)
            if (Math.abs(mesh.positions()[i]) > bounds[i%3]/2 + 1e-5)
                throw new IllegalArgumentException("Fit the collider to enclose the edited mesh before verifying.");
        var delta = ObserverGeometry.sub(ObserverGeometry.v(body.getJSONArray("position")),
                ObserverGeometry.v(prior.getJSONArray("position")));
        var localShift = ObserverGeometry.rotate(delta, prior.getJSONArray("rotation"), true);
        var witness = new JSONObject(new String(capture,StandardCharsets.UTF_8));
        return BrowserViewTrace.verifyEdit(originals,proof,witness,elevation,mesh,localShift)
                .put("policy",POLICY).put("sourceId",body.getString("sourceId"))
                .put("original",SpatialPropCopy.binding(original)).put("edited",SpatialPropCopy.binding(edited))
                .put("meshSha256",mesh.key()).put("captureSha256",MapDefinition.sha(capture));
    }

    static String sceneId(String hash) {
        return UUID.nameUUIDFromBytes((POLICY+":"+hash).getBytes(StandardCharsets.UTF_8)).toString();
    }
}
