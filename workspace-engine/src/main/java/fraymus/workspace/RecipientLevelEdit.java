package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Reviewed edits of the placed body with the original mesh, floor and aisle retained. */
final class RecipientLevelEdit {
    private static final double MAX_COLLIDER_DELTA_M = .10;
    private static final double MESH_TOLERANCE_M = 1e-5;

    private RecipientLevelEdit() {}

    static String rootId(String archive) { return derive("recipient-level-copy-v1",archive); }

    static String revisionId(String review) { return derive("recipient-level-edit-v1",review); }

    private static String derive(String role, String digest) {
        return UUID.nameUUIDFromBytes((role+":"+digest).getBytes(StandardCharsets.UTF_8)).toString();
    }

    static JSONObject provenance(PlacementExchange.Bundle bundle, JSONObject replay) {
        var reviewed = bundle.evidence().getJSONObject("review");
        String prop = bundle.recipient() ? bundle.stage().propKey() : SpatialLevelPlacement.propKey(reviewed);
        var link = new JSONObject().put("rootId",rootId(bundle.id())).put("archiveSha256",bundle.id())
                .put("glbSha256",bundle.glbSha256()).put("localReplaySha256",replay.getString("sha256"))
                .put("propSourceId",prop).put("floor",reviewed.getString("floor"))
                .put("aisle",reviewed.getJSONArray("aisle")).put("initialYawDeg",reviewed.getDouble("yawDeg"));
        if (bundle.recipient())
            link.put("inheritedRevisions",bundle.previous().size())
                    .put("originalArchiveSha256",bundle.originalArchiveSha256());
        return link;
    }

    static JSONObject root(PlacementExchange.Bundle bundle, JSONObject replay, String importedAt) {
        var link = provenance(bundle,replay);
        return SpatialScene.parse(bundle.scene().getString("sourceText"),link.getString("rootId"))
                .put("importedAt",importedAt).put("recipientBranch",link)
                .put("recipientRevision",0).put("recipientBaseReplay",replay);
    }

    static JSONObject candidate(JSONObject parent, JSONObject review, String id) {
        return SpatialScene.parse(definition(parent,review).toString(),revisionId(id));
    }

    static JSONObject commit(JSONObject candidate, JSONObject parent, JSONObject receipt, String importedAt) {
        return candidate.put("importedAt",importedAt)
                .put("recipientBranch",parent.getJSONObject("recipientBranch"))
                .put("recipientRevision",parent.getInt("recipientRevision")+1)
                .put("recipientEdit",receipt).put("recipientEditSha256",WorldFrames.hash(receipt));
    }

    static JSONObject plan(JSONObject scene, JSONObject input) {
        if (input.has("colliderWidthM") || input.has("colliderDepthM")) return planCollider(scene,input);
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
        return boundReview(scene,body,target,yaw,rotation);
    }

    private static JSONObject planCollider(JSONObject scene, JSONObject input) {
        SpatialScene.keys(input,"sceneId","colliderWidthM","colliderDepthM");
        var branch = scene.getJSONObject("recipientBranch");
        var body = SpatialLevelPlacement.propByKey(scene,branch.getString("propSourceId"));
        if (!body.has("visualMesh"))
            throw new IllegalArgumentException("A collider edit needs the placed prop’s retained visual mesh.");
        var size = body.getJSONArray("size");
        double width = SpatialScene.number(input,"colliderWidthM",.05,40);
        double depth = SpatialScene.number(input,"colliderDepthM",.05,40);
        double widthDelta = Math.abs(width-size.getDouble(0));
        double depthDelta = Math.abs(depth-size.getDouble(2));
        if (Math.max(widthDelta,depthDelta) <= MESH_TOLERANCE_M)
            throw new IllegalArgumentException("Change the collider width or depth before reviewing.");
        if (Math.max(widthDelta,depthDelta) > MAX_COLLIDER_DELTA_M+1e-9)
            throw new IllegalArgumentException("Change each collider dimension by at most 0.10 m per edit.");

        var mesh = SpatialGlb.asset(scene,body);
        var envelope = new double[3];
        for (int i=0;i<mesh.positions().length;i++)
            envelope[i%3] = Math.max(envelope[i%3],2*Math.abs((double)mesh.positions()[i]));
        var proposed = new double[]{width,size.getDouble(1),depth};
        for (int axis=0;axis<3;axis++) {
            if (proposed[axis] + 2*MESH_TOLERANCE_M < envelope[axis])
                throw new IllegalArgumentException("The proposed collider must enclose the retained visual mesh.");
        }
        var latest = scene.optJSONObject("recipientEdit");
        double yaw = latest == null ? branch.getDouble("initialYawDeg") : latest.getJSONObject("review").getDouble("yawDeg");
        var collider = new JSONObject().put("previousM",size).put("proposedM",new JSONArray(proposed))
                .put("meshEnvelopeM",new JSONArray(envelope)).put("meshSha256",mesh.key());
        return boundReview(scene,body,body.getJSONArray("position"),yaw,body.getJSONArray("rotation"))
                .put("collider",collider);
    }

    private static JSONObject boundReview(JSONObject scene, JSONObject body, JSONArray target, double yaw, JSONArray rotation) {
        var branch = scene.getJSONObject("recipientBranch");
        return new JSONObject().put("parent",SpatialPropCopy.binding(scene)).put("branch",branch)
                .put("sourceLabel",body.getString("label")).put("fromM",body.getJSONArray("position")).put("targetM",target)
                .put("yawDeg",yaw).put("rotation",rotation).put("floor",branch.getString("floor"))
                .put("aisle",branch.getJSONArray("aisle"));
    }

    private static void requireExactReview(JSONObject parent, JSONObject review) {
        var request = new JSONObject().put("sceneId",parent.getString("id"));
        if (review.has("collider")) {
            var size = review.getJSONObject("collider").getJSONArray("proposedM");
            request.put("colliderWidthM",size.getDouble(0)).put("colliderDepthM",size.getDouble(2));
        } else {
            var target = review.getJSONArray("targetM");
            request.put("xM",target.getDouble(0)).put("zM",target.getDouble(2)).put("yawDeg",review.getDouble("yawDeg"));
        }
        if (!plan(parent,request).similar(review))
            throw new IllegalArgumentException("The recipient review does not match the preceding revision or bound aisle.");
    }

    static JSONObject definition(JSONObject parent, JSONObject review) {
        requireExactReview(parent,review);
        var next = new JSONObject(parent.getString("sourceText"));
        var body = SpatialLevelPlacement.propByKey(next,propKey(review));
        if (review.has("collider")) {
            body.put("size",review.getJSONObject("collider").getJSONArray("proposedM"));
        } else {
            body.put("position",review.getJSONArray("targetM")).put("rotation",review.getJSONArray("rotation"));
        }
        return next;
    }

    static String propKey(JSONObject review) { return review.getJSONObject("branch").getString("propSourceId"); }
}
