package fraymus.workspace;

import org.json.JSONObject;
import org.junit.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class HypothesisExperimentTest {
    private static final JSONObject REQUEST = new JSONObject().put("request", "Resume my last hypothesis experiment.");

    @Test public void validatesTheModelProposalAndKeepsBoundaryResultsInconclusive() {
        assertEquals("right-24", HypothesisExperiment.proposal(proposal()).getString("probe"));
        for (var invalid : new JSONObject[]{
                proposal().put("tool", "game.command"),
                edit("probe", "custom"),
                edit("aHigh", 0.15),
                edit("aHigh", 0.0999),
                edit("aLow", 0.09),
                edit("aHigh", "0.01"),
                edit("target", "remote")}) {
            assertThrows(IllegalArgumentException.class, () -> HypothesisExperiment.proposal(invalid));
        }
        var prediction = HypothesisExperiment.proposal(proposal()).getJSONObject("b");
        assertEquals("Supports prediction", HypothesisExperiment.assess(interval("0.2", "0.3"), prediction));
        assertEquals("Conflicts with prediction", HypothesisExperiment.assess(interval("-0.1", "0"), prediction));
        assertEquals("Inconclusive at the boundary", HypothesisExperiment.assess(interval("0.0999", "0.105"), prediction));
    }

    @Test public void waitsForApprovalAndReplaysTheOriginalGameInTwoFreshSessions() throws Exception {
        Path repo = Path.of("..").toAbsolutePath().normalize();
        Path project = Files.createTempDirectory("hypothesis-experiment-test");
        var calls = new int[]{0};
        HypothesisExperiment.Planner fixture = (model, prompt, definition) -> {
            calls[0]++;
            assertTrue(prompt.contains("Movement disabled"));
            assertTrue(prompt.contains("Movement works"));
            assertEquals("experiment_propose", definition.getJSONObject("function").getString("name"));
            return new JSONObject().put("call", proposal()).put("provider", new JSONObject().put("fixture", true));
        };
        String id;
        JSONObject approval;
        try (var store = new WorkspaceStore(project); var relay = new RelayPlaytest(repo, project, store)) {
            var experiment = new HypothesisExperiment(repo, project, store, relay, fixture);
            var review = experiment.propose(new JSONObject().put("a", "Movement disabled.")
                    .put("b", "Movement works.").put("model", "unit-test-provider"));
            id = review.getString("id");
            assertEquals("review", review.getString("status"));
            assertFalse(Files.exists(project.resolve("hypothesis-experiments/runtime")));
            assertThrows(IllegalStateException.class, () -> experiment.approve(new JSONObject()
                    .put("id", id).put("reviewSha256", "wrong")));
            approval = new JSONObject().put("id", id).put("reviewSha256", review.getString("reviewSha256"));
        }
        JSONObject artifact;
        try (var store = new WorkspaceStore(project); var relay = new RelayPlaytest(repo, project, store)) {
            var experiment = new HypothesisExperiment(repo, project, store, relay, fixture);
            assertEquals("review", experiment.intent(REQUEST).getJSONObject("experiment").getString("status"));
            JSONObject completed;
            try { completed = experiment.approve(approval); }
            catch (java.util.concurrent.ExecutionException error) {
                boolean hostedMac = "1".equals(System.getenv("FRAYNIX_CI_MAC_DISPLAY")) &&
                        System.getProperty("os.name").toLowerCase(java.util.Locale.ROOT).startsWith("mac");
                String message = String.valueOf(error.getCause().getMessage());
                if (!hostedMac || !message.contains("Game Lab cannot get a macOS OpenGL 4.1 context from the current display."))
                    throw error;
                var interrupted = experiment.intent(REQUEST).getJSONObject("experiment");
                assertEquals("interrupted", interrupted.getString("status"));
                assertFalse(interrupted.has("artifact"));
                assertTrue(store.artifacts().isEmpty());
                assertEquals(1, calls[0]);
                record("unavailable-opengl", new JSONObject());
                return;
            }
            assertEquals("complete", completed.getString("status"));
            assertEquals("Conflicts with prediction", completed.getJSONObject("result").getString("a"));
            assertEquals("Supports prediction", completed.getJSONObject("result").getString("b"));
            artifact = completed.getJSONObject("artifact");
            assertTrue(completed.similar(experiment.approve(approval)));
            var contents = store.read(artifact.getString("id")).getJSONObject("contents");
            var evidence = new JSONObject(contents.getString("experiment-evidence.json"));
            assertNotEquals(evidence.getJSONObject("original").getString("sessionId"),
                    evidence.getJSONObject("replay").getString("sessionId"));
            assertEquals(48, evidence.getJSONObject("original").getJSONObject("observed").getInt("tick"));
            record("verified-replay", completed.getJSONObject("result"));
        }
        try (var store = new WorkspaceStore(project); var relay = new RelayPlaytest(repo, project, store)) {
            var experiment = new HypothesisExperiment(repo, project, store, relay, fixture);
            assertTrue(artifact.similar(experiment.intent(REQUEST).getJSONObject("experiment").getJSONObject("artifact")));
            Path file = store.asset(artifact.getString("id"), 1, "experiment-evidence.json");
            byte[] before = Files.readAllBytes(file);
            try {
                Files.writeString(file, "{}");
                assertThrows(IllegalStateException.class, () -> experiment.intent(REQUEST));
            } finally { Files.write(file, before); }
        }
        assertEquals(1, calls[0]);
    }

    private static void record(String status, JSONObject result) throws Exception {
        Path output = Path.of("build", "hypothesis-experiment-ci.json");
        Files.createDirectories(output.getParent());
        Files.writeString(output, new JSONObject().put("nativeGame", status).put("result", result).toString());
    }

    private static ExplanationEvidence.Interval interval(String low, String high) {
        return new ExplanationEvidence.Interval(new BigDecimal(low), new BigDecimal(high));
    }

    private static JSONObject edit(String key, Object value) {
        var call = proposal();
        call.getJSONObject("args").put(key, value);
        return call;
    }

    private static JSONObject proposal() {
        return new JSONObject().put("tool", HypothesisExperiment.TOOL).put("args", new JSONObject()
                .put("probe", "right-24").put("reason", "The predictions disagree about horizontal movement.")
                .put("aLow", -.01).put("aHigh", .01).put("bLow", .1).put("bHigh", 3));
    }
}
