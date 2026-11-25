package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/** Portable lineage of a replayed original and each committed local recipient pose. */
final class RecipientRevisionExchange {
    static final String FORMAT = "fraynix.verified-recipient-level-v1";
    private static final int MAX_REVISIONS = 31;

    private RecipientRevisionExchange() {}

    static JSONObject export(JSONObject head, JSONObject root, byte[] original, JSONArray stages) throws Exception {
        checkRevisionCount(stages);
        var glb = SpatialGlb.verifiedExport(head);
        var archive = new JSONObject().put("format",FORMAT).put("headSceneId",head.getString("id"))
                .put("originalArchiveSha256",MapDefinition.sha(original))
                .put("originalArchiveBase64",Base64.getEncoder().encodeToString(original))
                .put("baseReplay",root.getJSONObject("recipientBaseReplay")).put("rootImportedAt",root.getString("importedAt"))
                .put("revisions",stages).put("glbSha256",MapDefinition.sha(glb))
                .put("glbBase64",Base64.getEncoder().encodeToString(glb));
        byte[] bytes = archive.toString().getBytes(StandardCharsets.UTF_8);
        if (bytes.length > PlacementExchange.BYTE_LIMIT)
            throw new IllegalArgumentException("The recipient revision exceeds the 1.5 MB exchange limit.");
        return new JSONObject().put("sceneId",head.getString("id")).put("sha256",MapDefinition.sha(bytes))
                .put("originalArchiveSha256",archive.getString("originalArchiveSha256"))
                .put("base64",Base64.getEncoder().encodeToString(bytes));
    }

    static PlacementExchange.Bundle inspect(JSONObject input, String bundleHash) throws Exception {
        SpatialScene.keys(input,"format","headSceneId","originalArchiveSha256","originalArchiveBase64",
                "baseReplay","rootImportedAt","revisions","glbSha256","glbBase64");
        byte[] original = SpriteWorkshop.decode(input.getString("originalArchiveBase64"),PlacementExchange.AUTHORED_BYTE_LIMIT);
        var authored = PlacementExchange.inspect(original,false);
        if (!authored.destinationVerified() || !authored.id().equals(input.getString("originalArchiveSha256"))
                || !PlacementExchange.validReplay(authored,input.getJSONObject("baseReplay")))
            throw new IllegalArgumentException("The original archive or its attached local replay differs.");

        var edits = input.getJSONArray("revisions");
        checkRevisionCount(edits);
        String importedAt = input.getString("rootImportedAt");
        Instant last = Instant.parse(importedAt);
        var parent = RecipientLevelEdit.root(authored,input.getJSONObject("baseReplay"),importedAt);
        var stages = new ArrayList<PlacementExchange.Stage>();
        stages.add(authored.stage());
        JSONObject layout = null;
        for (Object entry : edits) {
            var revision = (JSONObject) entry;
            SpatialScene.keys(revision,"approvalSha256","receiptSha256","receipt","importedAt");
            var receipt = revision.getJSONObject("receipt");
            SpatialScene.keys(receipt,"reviewId","review","measured","repeat");
            String id = receipt.getString("reviewId");
            var review = receipt.getJSONObject("review");
            if (!WorldFrames.hash(review).equals(id) || !id.equals(revision.getString("approvalSha256"))
                    || !WorldFrames.hash(receipt).equals(revision.getString("receiptSha256")))
                throw new IllegalArgumentException("A recipient approval, review or trial receipt differs.");

            var target = review.getJSONArray("targetM");
            var issued = RecipientLevelEdit.plan(parent,new JSONObject().put("sceneId",parent.getString("id"))
                    .put("xM",target.getDouble(0)).put("zM",target.getDouble(2)).put("yawDeg",review.getDouble("yawDeg")));
            if (!issued.similar(review))
                throw new IllegalArgumentException("The recipient review does not match the preceding revision or bound aisle.");
            var candidate = RecipientLevelEdit.candidate(parent,review,id);
            String prop = RecipientLevelEdit.propKey(review);
            layout = SpatialLevelPlacement.playable(candidate,review,prop);
            var measured = receipt.getJSONObject("measured");
            PlacementExchange.checkTrials(measured,true);
            if (!measured.similar(receipt.getJSONObject("repeat"))
                    || Math.abs(measured.getDouble("routeLengthM")-layout.getDouble("routeLengthM")) > 1e-9)
                throw new IllegalArgumentException("The recipient's archived repeats or aisle length differ.");
            importedAt = revision.getString("importedAt");
            Instant now = Instant.parse(importedAt);
            if (!now.isAfter(last)) throw new IllegalArgumentException("The recipient revision order is invalid.");
            last = now;
            parent = RecipientLevelEdit.commit(candidate,parent,receipt,importedAt);
            stages.add(new PlacementExchange.Stage(parent,receipt,prop,true));
        }

        String headId = input.getString("headSceneId");
        if (!headId.equals(parent.getString("id"))) throw new IllegalArgumentException("The recipient archive head differs.");
        byte[] glb = SpriteWorkshop.decode(input.getString("glbBase64"),SpatialGlb.LIMIT);
        String glbHash = MapDefinition.sha(glb);
        if (!glbHash.equals(input.getString("glbSha256")))
            throw new IllegalArgumentException("The embedded GLB checksum differs.");
        SpatialGlb.verifyScene(parent,glb);
        return new PlacementExchange.Bundle(bundleHash,stages.removeLast(),glbHash,layout,true,List.copyOf(stages));
    }

    private static void checkRevisionCount(JSONArray stages) {
        if (stages.isEmpty() || stages.length() > MAX_REVISIONS)
            throw new IllegalArgumentException("Choose a recipient archive with 1 to 31 verified edits.");
    }
}
