
package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.Assert.*;

public class PlacementExchangeTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void rejectsRehashedFalseTrajectoryAfterImport() throws Exception {
        var bundle = new JSONObject(Files.readString(frozen()));
        var evidence = bundle.getJSONObject("evidence");
        var trial = evidence.getJSONObject("measured");
        var value = trial.getJSONObject("hit").getJSONArray("samples").getJSONObject(2).getJSONArray("propM");
        value.put(0,value.getDouble(0)+.25);
        evidence.put("repeat",WorldFrames.copy(trial));
        bundle.put("evidenceSha256",WorldFrames.hash(evidence));
        var project = temp.newFolder().toPath();
        var exchange = new PlacementExchange(project);
        var archived = exchange.open(request(bundle));
        assertFalse(archived.has("replay"));
        String id = archived.getString("id");
        var failure = assertThrows(IllegalStateException.class,
                () -> exchange.replay(new JSONObject().put("id",id)));
        assertTrue(failure.getMessage().contains("hit replay differs"));
        assertFalse(new PlacementExchange(project).latest(new JSONObject()).has("replay"));
    }

    @Test
    public void refusesMissingSamplesAndGeometryDespiteMatchingContainerHashes() throws Exception {
        var original = new JSONObject(Files.readString(frozen()));
        var bundle = WorldFrames.copy(original);
        var evidence = bundle.getJSONObject("evidence");
        var result = evidence.getJSONObject("measured");
        result.getJSONObject("miss").getJSONArray("samples").remove(1);
        evidence.put("repeat",WorldFrames.copy(result));
        bundle.put("evidenceSha256",WorldFrames.hash(evidence));
        assertThrows(IllegalArgumentException.class, () -> PlacementExchange.inspect(encoded(bundle)));

        var wrongGeometry = original.getJSONObject("scene").getJSONArray("objects").getJSONObject(1);
        wrongGeometry.put("massKg",2);
        assertThrows(IllegalStateException.class, () -> PlacementExchange.inspect(encoded(original)));
    }

    @Test
    public void refusesChangedDestinationEvenWithMatchingFinalGlb() throws Exception {
        var bundle = authored();
        var raw = bundle.getJSONObject("scene");
        raw.getJSONArray("objects").getJSONObject(2).put("massKg", 3);
        var scene = SpatialScene.parse(raw.toString(), bundle.getString("sceneId"));
        byte[] glb = SpatialGlb.verifiedExport(scene);
        bundle.put("glbSha256", MapDefinition.sha(glb))
                .put("glbBase64", Base64.getEncoder().encodeToString(glb));
        var mismatch = assertThrows(IllegalArgumentException.class, () -> PlacementExchange.inspect(encoded(bundle)));
        assertTrue(mismatch.getMessage(), mismatch.getMessage().contains("Existing destination geometry"));
    }

    @Test
    public void refusesBlockedAisleEvenWhenReviewAndSceneIdentifiersAreRehashed() throws Exception {
        var bundle = authored();
        var review = bundle.getJSONObject("evidence").getJSONObject("review");
        review.put("aisle", new JSONArray(new double[]{-2.15,-1.1,-2.15,1.1}));
        sealReview(bundle,review);
        var mismatch = assertThrows(IllegalArgumentException.class, () -> PlacementExchange.inspect(encoded(bundle)));
        assertTrue(mismatch.getMessage(), mismatch.getMessage().contains("player aisle is blocked"));
    }

    @Test
    public void rejectsAuthoredTraceWithRehashedButFalseMotion() throws Exception {
        var bundle = authored();
        var evidence = bundle.getJSONObject("evidence");
        var measured = evidence.getJSONObject("measured");
        measured.getJSONObject("hit").getJSONArray("samples").getJSONObject(2).getJSONArray("propM").put(0, 3);
        evidence.put("repeat", WorldFrames.copy(measured));
        bundle.put("evidenceSha256", WorldFrames.hash(evidence));
        var exchange = new PlacementExchange(temp.newFolder().toPath());
        String id = exchange.open(request(bundle)).getString("id");
        var difference = assertThrows(IllegalStateException.class,
                () -> exchange.replay(new JSONObject().put("id",id)));
        assertTrue(difference.getMessage(), difference.getMessage().contains("hit replay differs"));
        assertFalse(exchange.latest(new JSONObject()).has("replay"));
    }

    @Test
    public void acceptsOlderAuthoredSceneWithFinishedGeometryScope() throws Exception {
        var bundle = authored();
        bundle.put("format",SpatialLevelPlacement.LEGACY_ARCHIVE);
        bundle.remove("destinationSourceText");
        var parsed = PlacementExchange.inspect(encoded(bundle));
        assertTrue(parsed.authored());
        assertFalse(parsed.destinationVerified());
        assertEquals(4.35, parsed.layout().getDouble("routeLengthM"), 1e-10);
    }

    @Test
    public void refusesALineageWithItsFirstApprovalOmitted() throws Exception {
        var archive = recipientRevision();
        archive.getJSONArray("revisions").remove(0);
        assertThrows(IllegalArgumentException.class, () -> PlacementExchange.inspect(encoded(archive)));
    }

    @Test
    public void rejectsFalseMotionInAnEarlierStageEvenWithAReboundApprovalChain() throws Exception {
        var archive = recipientRevision();
        var original = PlacementExchange.inspect(Base64.getDecoder().decode(archive.getString("originalArchiveBase64")));
        var root = RecipientLevelEdit.root(original, archive.getJSONObject("baseReplay"),
                archive.getString("rootImportedAt"));

        var edits = archive.getJSONArray("revisions");
        var first = edits.getJSONObject(0);
        var firstReceipt = first.getJSONObject("receipt");
        var measured = firstReceipt.getJSONObject("measured");
        var position = measured.getJSONObject("hit").getJSONArray("samples").getJSONObject(2).getJSONArray("propM");
        position.put(0, position.getDouble(0) + .25);
        firstReceipt.put("repeat", WorldFrames.copy(measured));
        first.put("receiptSha256", WorldFrames.hash(firstReceipt));
        var candidate = RecipientLevelEdit.candidate(root, firstReceipt.getJSONObject("review"),
                firstReceipt.getString("reviewId"));
        var parent = RecipientLevelEdit.commit(candidate, root, firstReceipt, first.getString("importedAt"));

        var second = edits.getJSONObject(1);
        var secondReceipt = second.getJSONObject("receipt");
        var originalReview = secondReceipt.getJSONObject("review");
        var target = originalReview.getJSONArray("targetM");
        var rebound = RecipientLevelEdit.plan(parent, new JSONObject().put("sceneId", parent.getString("id"))
                .put("xM", target.getDouble(0)).put("zM", target.getDouble(2))
                .put("yawDeg", originalReview.getDouble("yawDeg")));
        String approvalHash = WorldFrames.hash(rebound);
        secondReceipt.put("review", rebound).put("reviewId", approvalHash);
        second.put("approvalSha256", approvalHash).put("receiptSha256", WorldFrames.hash(secondReceipt));
        archive.put("headSceneId", RecipientLevelEdit.revisionId(approvalHash));

        var project = temp.newFolder().toPath();
        var exchange = new PlacementExchange(project);
        String id = exchange.open(request(archive)).getString("id");
        var difference = assertThrows(IllegalStateException.class,
                () -> exchange.replay(new JSONObject().put("id", id)));
        assertTrue(difference.getMessage(), difference.getMessage().contains("hit replay differs"));
        assertFalse(new PlacementExchange(project).latest(new JSONObject()).has("replay"));
    }

    @Test
    public void refusesARehashedColliderReviewThatChangesHeight() throws Exception {
        var parent = originalRecipientRoot();
        var review = RecipientLevelEdit.plan(parent, colliderRequest(parent, .56, .34));
        review.getJSONObject("collider").getJSONArray("proposedM").put(1, .65);
        String rehashed = WorldFrames.hash(review);
        var refused = assertThrows(IllegalArgumentException.class,
                () -> RecipientLevelEdit.candidate(parent, review, rehashed));
        assertTrue(refused.getMessage().contains("does not match the preceding revision"));
    }

    @Test
    public void rejectsInBudgetDepthChangeWhenItBlocksTheReviewedAisle() throws Exception {
        var root = originalRecipientRoot();
        var pose = RecipientLevelEdit.plan(root, new JSONObject().put("sceneId",root.getString("id"))
                .put("xM",.15).put("zM",-.74).put("yawDeg",0));
        String poseId = WorldFrames.hash(pose);
        var candidate = RecipientLevelEdit.candidate(root,pose,poseId);
        String prop = RecipientLevelEdit.propKey(pose);
        assertEquals(4.35,SpatialLevelPlacement.playable(candidate,pose,prop).getDouble("routeLengthM"),1e-10);
        var receipt = new JSONObject().put("reviewId",poseId).put("review",pose);
        var parent = RecipientLevelEdit.commit(candidate,root,receipt,"2026-11-25T15:00:00Z");
        var collider = RecipientLevelEdit.plan(parent,colliderRequest(parent,.50,.38));
        var wider = RecipientLevelEdit.candidate(parent,collider,WorldFrames.hash(collider));
        var refused = assertThrows(IllegalArgumentException.class,
                () -> SpatialLevelPlacement.playable(wider,collider,prop));
        assertTrue(refused.getMessage().contains("player aisle is blocked"));
    }

    private static JSONObject colliderRequest(JSONObject parent, double width, double depth) {
        return new JSONObject().put("sceneId",parent.getString("id"))
                .put("colliderWidthM",width).put("colliderDepthM",depth);
    }

    private static JSONObject originalRecipientRoot() throws Exception {
        var archive = recipientRevision();
        var original = PlacementExchange.inspect(Base64.getDecoder().decode(archive.getString("originalArchiveBase64")));
        return RecipientLevelEdit.root(original,archive.getJSONObject("baseReplay"),archive.getString("rootImportedAt"));
    }

    private static JSONObject recipientRevision() throws Exception {
        return new JSONObject(Files.readString(Path.of("..", "workspace-desktop", "test", "fixtures",
                "recipient-revision-exchange", "signal-bay-revision.fraynix-recipient.json")));
    }

    private static void sealReview(JSONObject bundle, JSONObject review) {
        var evidence = bundle.getJSONObject("evidence");
        String reviewId = WorldFrames.hash(review);
        evidence.put("reviewId",reviewId);
        bundle.put("evidenceSha256",WorldFrames.hash(evidence)).put("sceneId",SpatialLevelPlacement.sceneId(reviewId));
        var objects = bundle.getJSONObject("scene").getJSONArray("objects");
        objects.getJSONObject(objects.length()-1).put("sourceId",SpatialLevelPlacement.propKey(review));
    }

    private static JSONObject authored() throws Exception {
        Path fixture = Path.of("..","workspace-desktop","test","fixtures","authored-level-exchange",
                "edited-signal-bay.fraynix-level.json");
        return new JSONObject(Files.readString(fixture));
    }

    private static Path frozen() {
        return Path.of("..","workspace-desktop","test","fixtures","placement-exchange","edition232.fraynix-placement.json");
    }

    private static byte[] encoded(JSONObject input) {
        return input.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static JSONObject request(JSONObject data) {
        return new JSONObject().put("base64",Base64.getEncoder().encodeToString(encoded(data)));
    }
}
