
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
