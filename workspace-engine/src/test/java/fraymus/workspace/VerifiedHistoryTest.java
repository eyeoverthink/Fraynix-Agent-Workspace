package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;

public class VerifiedHistoryTest {
    @Test(timeout=180000) public void warmAndColdChecksPreserveOriginalNativeHistory() throws Exception {
        Path root = Files.createTempDirectory("verified-history");
        String id;
        JSONObject expected;
        try (var artifacts = new WorkspaceStore(root); var store = new SmallStore161(root, artifacts)) {
            var world = store.create(SmallBody161Test.scene(.05,.04));
            id = world.getString("id");
            world = HistoryCosts164Test.move(store, world, .10);
            var history = new VerifiedHistory(store, new VerifiedComputationCache(true));
            assertTrue(world.similar(history.read(id)));
            history.read(id).getJSONArray("history").getJSONObject(0).put("jobId", "forged");
            assertTrue(world.similar(history.read(id)));
            assertEquals(2, history.status().getInt("reused"));
            world = HistoryCosts164Test.move(store, world, -.08);
            assertTrue(world.similar(history.read(id)));
            assertEquals(2, history.status().getInt("computed"));
            expected = world;
            var full = new VerifiedHistory(store, new VerifiedComputationCache(false));
            assertTrue(full.read(id).similar(history.read(id)));
        }
        try (var artifacts = new WorkspaceStore(root); var store = new SmallStore161(root, artifacts)) {
            var cold = new VerifiedHistory(store);
            assertTrue(expected.similar(cold.read(id)));
            assertEquals(0, cold.status().getInt("reused"));
        }
    }

    @Test(timeout=180000) public void warmDiskTraceAndReportChangesRefuse() throws Exception {
        Path root = Files.createTempDirectory("verified-history-refusal");
        try (var artifacts = new WorkspaceStore(root); var store = new SmallStore161(root, artifacts)) {
            var world = HistoryCosts164Test.move(store, store.create(SmallBody161Test.scene(.05,.04)), .1);
            String id = world.getString("id");
            var history = new VerifiedHistory(store);
            history.read(id);
            byte[] disk = Files.readAllBytes(store.root);
            var timestamp = Files.getLastModifiedTime(store.root);
            byte[] broken = disk.clone(); broken[broken.length / 2] ^= 1;
            Files.write(store.root, broken); Files.setLastModifiedTime(store.root, timestamp);
            assertThrows(Exception.class, () -> history.read(id));
            assertEquals(0, history.status().getInt("entries"));
            Files.write(store.root, disk); history.read(id);
            var job = store.find("jobs", world.getJSONArray("history").getJSONObject(0).getString("jobId"));
            var run = WorldFrames.copy(job.getJSONObject("run"));
            job.getJSONObject("run").getJSONObject("initialState").getJSONArray("bodies")
                    .getJSONObject(0).getJSONArray("position").put(0, .25);
            store.save();
            assertThrows(Exception.class, () -> history.read(id));
            job.put("run", run); store.save(); history.read(id);
            var file = artifacts.asset(job.getJSONObject("report").getString("id"), 1, "index.html");
            byte[] good = Files.readAllBytes(file), bad = good.clone(); bad[bad.length / 2] ^= 1;
            timestamp = Files.getLastModifiedTime(file);
            Files.write(file, bad); Files.setLastModifiedTime(file, timestamp);
            assertThrows(Exception.class, () -> history.read(id));
            Files.write(file, good);
            assertTrue(world.similar(history.read(id)));
            assertEquals(1, store.world(id).getInt("revision"));
        }
    }
}
