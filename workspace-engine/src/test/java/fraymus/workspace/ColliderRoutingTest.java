package fraymus.workspace;

import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.Assert.*;

public class ColliderRoutingTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void rejectsUnexpectedToolAndDecisionBeforeRecordingOrActing() throws Exception {
        Path project = temp.newFolder().toPath();
        try (var spatial = new SpatialStore(project)) {
            var wrongTool = new ColliderRouting(project, (model,prompt,tool) -> choice("approve","compare"), spatial);
            assertThrows(IllegalStateException.class, () -> wrongTool.route(request("", "Compare the collider.")));
            assertTrue(wrongTool.latest().isEmpty());
            var wrongChoice = new ColliderRouting(project, (model,prompt,tool) -> choice("route.collider.request","approve"), spatial);
            assertThrows(IllegalArgumentException.class, () -> wrongChoice.route(request("", "Compare the collider.")));
            assertTrue(wrongChoice.latest().isEmpty());
            assertTrue(spatial.latestRecipientMission(new JSONObject()).isEmpty());
        }
    }

    @Test
    public void asksForAnExactProposalEvenWhenTheModelChoosesCompare() throws Exception {
        Path project = temp.newFolder().toPath();
        try (var spatial = new SpatialStore(project)) {
            var routing = new ColliderRouting(project, (model,prompt,tool) -> choice("route.collider.request","compare"), spatial);
            var question = routing.route(request("", "Compare the collider."));
            assertEquals("compare",question.getString("modelChoice"));
            assertEquals("clarify",question.getString("decision"));
            assertTrue(question.getString("question").startsWith("Which collider proposal"));
            assertTrue(spatial.latestRecipientMission(new JSONObject()).isEmpty());
            assertTrue(question.similar(new ColliderRouting(project,null,spatial).latest()));
        }
    }

    @Test
    public void aChangedProposalRequiresASecondConfirmationAfterRestart() throws Exception {
        Path project = temp.newFolder().toPath();
        String sceneId;
        JSONObject first;
        try (var spatial = new SpatialStore(project)) {
            var fixture = Path.of("..","workspace-desktop","test","fixtures","authored-level-exchange",
                    "edited-signal-bay.fraynix-level.json");
            var encoded = Base64.getEncoder().encodeToString(Files.readAllBytes(fixture));
            var opened = spatial.openPlacementExchange(new JSONObject().put("base64",encoded));
            var id = new JSONObject().put("id",opened.getString("id"));
            var replay = spatial.replayPlacementExchange(id).getJSONObject("replay");
            sceneId = spatial.forkPlacementExchange(id.put("replaySha256",replay.getString("sha256"))).getString("id");
            spatial.reviewRecipientEdit(new JSONObject().put("sceneId",sceneId).put("colliderWidthM",.54).put("colliderDepthM",.33));
            var routing = new ColliderRouting(project, (model,prompt,tool) -> choice("route.collider.request","clarify"), spatial);
            first = routing.route(request(sceneId,"What about that?"));
        }
        try (var spatial = new SpatialStore(project)) {
            spatial.reviewRecipientEdit(new JSONObject().put("sceneId",sceneId).put("colliderWidthM",.55).put("colliderDepthM",.34));
            var routing = new ColliderRouting(project, (model,prompt,tool) -> {
                throw new AssertionError("A reply about the replaced target cannot run inference or act.");
            }, spatial);
            var next = routing.route(request("","Yes, compare it.").put("clarificationId",first.getString("id")));
            assertEquals("clarify",next.getString("decision"));
            assertEquals(first.getString("id"),next.getString("clarificationOf"));
            assertNotEquals(first.getJSONObject("target").getString("reviewId"),next.getJSONObject("target").getString("reviewId"));
            assertTrue(next.getString("question").contains("proposal changed"));
            assertTrue(spatial.latestRecipientMission(new JSONObject()).isEmpty());
            assertThrows(IllegalStateException.class,
                    () -> routing.route(request("","Yes.").put("clarificationId",first.getString("id"))));
        }
    }

    private static JSONObject request(String scene, String text) {
        return new JSONObject().put("sceneId",scene).put("request",text).put("model","test-model");
    }

    private static JSONObject choice(String tool, String decision) {
        return new JSONObject().put("call",new JSONObject().put("tool",tool)
                .put("args",new JSONObject().put("decision",decision)))
                .put("provider",new JSONObject().put("modelDigest","0".repeat(64)));
    }
}
