package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Exact local pose edits of the archived placed body, with the original floor and aisle retained. */
final class RecipientLevelEdit {
    private RecipientLevelEdit() {}

    static String rootId(String archive) { return derive("recipient-level-copy-v1",archive); }

    static String revisionId(String review) { return derive("recipient-level-edit-v1",review); }

    private static String derive(String role, String digest) {
        return UUID.nameUUIDFromBytes((role+":"+digest).getBytes(StandardCharsets.UTF_8)).toString();
    }

    static JSONObject provenance(PlacementExchange.Bundle bundle, JSONObject replay) {
        var reviewed = bundle.evidence().getJSONObject("review");
        return new JSONObject().put("rootId",rootId(bundle.id())).put("archiveSha256",bundle.id())
                .put("glbSha256",bundle.glbSha256()).put("localReplaySha256",replay.getString("sha256"))
                .put("propSourceId",SpatialLevelPlacement.propKey(reviewed)).put("floor",reviewed.getString("floor"))
                .put("aisle",reviewed.getJSONArray("aisle")).put("initialYawDeg",reviewed.getDouble("yawDeg"));
    }

    static JSONObject plan(JSONObject scene, JSONObject input) {
        SpatialScene.keys(input,"sceneId","xM","zM","yawDeg");
        var provenance = scene.getJSONObject("recipientBranch");
        var body = SpatialLevelPlacement.propByKey(scene,provenance.getString("propSourceId"));
        var from = body.getJSONArray("position");
        double x = SpatialScene.number(input,"xM",-20,20);
        double z = SpatialScene.number(input,"zM",-20,20);
        double yaw = SpatialScene.number(input,"yawDeg",-180,180);
        double angle = Math.toRadians(yaw)/2;
        var rotation = new JSONArray(new double[]{0,Math.sin(angle),0,Math.cos(angle)});
        var target = new JSONArray(new double[]{x,from.getDouble(1),z});
        if (target.similar(from) && rotation.similar(body.getJSONArray("rotation")))
            throw new IllegalArgumentException("Change the prop position or yaw before reviewing.");
        return new JSONObject().put("parent",SpatialPropCopy.binding(scene)).put("branch",provenance)
                .put("sourceLabel",body.getString("label")).put("fromM",from).put("targetM",target)
                .put("yawDeg",yaw).put("rotation",rotation).put("floor",provenance.getString("floor"))
                .put("aisle",provenance.getJSONArray("aisle"));
    }

    static JSONObject definition(JSONObject parent, JSONObject review) {
        if (!SpatialPropCopy.binding(parent).similar(review.getJSONObject("parent"))
                || !parent.getJSONObject("recipientBranch").similar(review.getJSONObject("branch")))
            throw new IllegalStateException("The reviewed local revision changed.");
        var next = new JSONObject(parent.getString("sourceText"));
        SpatialLevelPlacement.propByKey(next,propKey(review)).put("position",review.getJSONArray("targetM"))
                .put("rotation",review.getJSONArray("rotation"));
        return next;
    }

    static String propKey(JSONObject review) { return review.getJSONObject("branch").getString("propSourceId"); }
}
