package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.UUID;

import static org.junit.Assert.*;

public class RelayPlaytestTest {
    @Test public void replaysRealTrialsAndResumesOnlyTheUnfinishedTrial() throws Exception {
        Path repo = Path.of("..").toAbsolutePath().normalize();
        Path project = Files.createTempDirectory("relay-playtest");
        String id = UUID.randomUUID().toString();
        JSONObject request = new JSONObject().put("id", id).put("request", "Playtest Relay Ridge at all three paces.");
        JSONObject before = null;
        try (var store = new WorkspaceStore(project); var runner = new RelayPlaytest(repo, project, store)) {
            assertEquals("running", runner.start(request).getString("status"));
            assertThrows(IllegalStateException.class, () -> runner.verify(id));
            for (int i = 0; i < 150; i++) {
                JSONObject view = runner.advance(id);
                assertEquals(view.getString("reason"), "running", view.getString("status"));
                JSONArray trials = view.getJSONArray("trials");
                if (trials.length() == 3 && trials.getJSONObject(2).getInt("inputChunks") == 2) {
                    before = view;
                    break;
                }
            }
        }
        assertNotNull("Reached the third trial before stopping the runtime", before);
        JSONObject after;
        try (var store = new WorkspaceStore(project); var runner = new RelayPlaytest(repo, project, store)) {
            after = runner.read(id);
            assertEquals("interrupted", after.getString("status"));
            assertEquals("interrupted", after.getJSONArray("trials").getJSONObject(2).getString("status"));
            for (int i = 0; i < 2; i++) {
                assertTrue(before.getJSONArray("trials").getJSONObject(i).similar(after.getJSONArray("trials").getJSONObject(i)));
            }
            assertThrows(IllegalStateException.class, () -> runner.verify(id));
            runner.control(id, "resume");
            for (int i = 0; i < 220 && runner.read(id).getString("status").equals("running"); i++) runner.advance(id);
            JSONObject view = runner.read(id);
            assertEquals(view.getString("reason"), "completed", view.getString("status"));
            JSONObject proof = runner.verify(id);
            var sessions = new HashSet<String>();
            for (Object item : proof.getJSONArray("trials")) assertTrue(sessions.add(((JSONObject) item).getString("sessionId")));
            assertEquals(7, sessions.size());
            JSONObject audit = proof.getJSONObject("audit");
            assertEquals(9, audit.getInt("crossings"));
            assertEquals(3, audit.getInt("independentReplays"));
            JSONObject report = proof.getJSONObject("report");
            Path file = store.asset(report.getString("id"), report.getInt("version"), "playtest.json");
            byte[] original = Files.readAllBytes(file);
            Files.writeString(file, "{}");
            try {
                assertThrows(Exception.class, () -> runner.verify(id));
            } finally {
                Files.write(file, original);
            }
            assertTrue(proof.similar(runner.verify(id)));
            Path output = Path.of("build/relay-playtest/result.json");
            Files.createDirectories(output.getParent());
            Path evidence = project.resolve("relay-playtests").resolve(id + ".json");
            Files.writeString(output, new JSONObject().put("passed", true).put("source", proof.getJSONObject("source"))
                    .put("beforeStop", before).put("afterRestart", after).put("completed", proof)
                    .put("originalProof", AgentFiles042.loadChecked(evidence)).put("changedReportRejected", true).toString(2));
        }
        try (var store = new WorkspaceStore(project); var runner = new RelayPlaytest(repo, project, store)) {
            assertEquals("completed", runner.verify(id).getString("status"));
        }
    }

    @Test public void onlyTheNamedLocalRequestsAreAdmitted() {
        assertTrue(RelayPlaytest.requested("  Verify Relay Switchyard at careful pace!  "));
        assertFalse(RelayPlaytest.requested("Explain how to playtest Relay Ridge at normal pace"));
        assertFalse(RelayPlaytest.requested("Playtest Relay Ridge at full pace."));
        assertFalse(RelayPlaytest.requested("Playtest Relay Ridge at normal pace. Then open a browser."));
    }
}
