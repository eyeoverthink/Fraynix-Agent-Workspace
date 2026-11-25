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
    static final String HANDOFF = "fraynix.verified-recipient-handoff-v2";
    static final int MAX_HANDOFFS = 6;
    private static final int MAX_REVISIONS = 31;

    private RecipientRevisionExchange() {}

    static JSONObject export(JSONObject head, JSONObject root, byte[] original, JSONArray stages) throws Exception {
        var link = root.getJSONObject("recipientBranch");
        int inherited = link.optInt("inheritedRevisions");
        checkRevisionCount(stages,inherited);
        boolean forwarded = inherited > 0;
        if (forwarded && PlacementExchange.inspect(original).handoffDepth() >= MAX_HANDOFFS)
            throw new IllegalArgumentException("The exchange already includes six forwarded handoffs.");
        String parentHash = MapDefinition.sha(original);
        String originalHash = link.optString("originalArchiveSha256",parentHash);
        var glb = SpatialGlb.verifiedExport(head);
        var archive = new JSONObject().put("format",forwarded ? HANDOFF : FORMAT).put("headSceneId",head.getString("id"))
                .put("originalArchiveSha256",originalHash)
                .put("baseReplay",root.getJSONObject("recipientBaseReplay")).put("rootImportedAt",root.getString("importedAt"))
                .put("revisions",stages).put("glbSha256",MapDefinition.sha(glb))
                .put("glbBase64",Base64.getEncoder().encodeToString(glb));
        String archiveKey = forwarded ? "parentArchiveBase64" : "originalArchiveBase64";
        archive.put(archiveKey,Base64.getEncoder().encodeToString(original));
        if (forwarded) archive.put("parentArchiveSha256",parentHash);
        byte[] bytes = archive.toString().getBytes(StandardCharsets.UTF_8);
        if (bytes.length > PlacementExchange.BYTE_LIMIT)
            throw new IllegalArgumentException("The recipient revision exceeds the 1.5 MB exchange limit.");
        var result = new JSONObject().put("sceneId",head.getString("id")).put("sha256",MapDefinition.sha(bytes))
                .put("originalArchiveSha256",originalHash).put("base64",Base64.getEncoder().encodeToString(bytes));
        if (forwarded) result.put("parentArchiveSha256",parentHash);
        return result;
    }

    static PlacementExchange.Bundle inspect(JSONObject input, String bundleHash, int remainingHandoffs) throws Exception {
        boolean forwarded = HANDOFF.equals(input.getString("format"));
        String archiveKey = forwarded ? "parentArchiveBase64" : "originalArchiveBase64";
        String hashKey = forwarded ? "parentArchiveSha256" : "originalArchiveSha256";
        SpatialScene.keys(input,"format","headSceneId","originalArchiveSha256",archiveKey,"parentArchiveSha256",
                "baseReplay","rootImportedAt","revisions","glbSha256","glbBase64");
        if (input.has("parentArchiveSha256") != forwarded)
            throw new IllegalArgumentException("The attached archive does not match the exchange format.");
        if (forwarded && remainingHandoffs == 0)
            throw new IllegalArgumentException("The exchange already includes six forwarded handoffs.");
        byte[] original = SpriteWorkshop.decode(input.getString(archiveKey),
                forwarded ? PlacementExchange.BYTE_LIMIT : PlacementExchange.AUTHORED_BYTE_LIMIT);
        if (!MapDefinition.sha(original).equals(input.getString(hashKey)))
            throw new IllegalArgumentException("The attached archive checksum differs.");
        var source = PlacementExchange.inspect(original,forwarded ? remainingHandoffs-1 : -1);
        var baseReplay = input.getJSONObject("baseReplay");
        if (!source.destinationVerified() || source.recipient() != forwarded
                || !source.originalArchiveSha256().equals(input.getString("originalArchiveSha256"))
                || !PlacementExchange.validReplay(source,baseReplay))
            throw new IllegalArgumentException("The attached archive or its pinned local replay differs.");

        var edits = input.getJSONArray("revisions");
        checkRevisionCount(edits,source.previous().size());
        String importedAt = input.getString("rootImportedAt");
        Instant last = Instant.parse(importedAt);
        var parent = RecipientLevelEdit.root(source,baseReplay,importedAt);
        var stages = new ArrayList<>(source.previous());
        stages.add(source.stage());
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
        int handoffs = forwarded ? source.handoffDepth()+1 : 0;
        return new PlacementExchange.Bundle(bundleHash,stages.removeLast(),glbHash,layout,true,List.copyOf(stages),handoffs);
    }

    private static void checkRevisionCount(JSONArray stages, int inherited) {
        if (stages.isEmpty() || stages.length() + inherited > MAX_REVISIONS)
            throw new IllegalArgumentException("Choose a recipient archive with 1 to 31 verified edits.");
    }
}
