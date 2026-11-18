package fraymus.workspace;

import org.json.*;

/** Reuses a completed original history check only behind a fresh, complete witness. */
final class VerifiedHistory {
    private final SmallStore161 store;
    private final VerifiedComputationCache cache;

    VerifiedHistory(SmallStore161 store) { this(store, new VerifiedComputationCache()); }
    VerifiedHistory(SmallStore161 store, VerifiedComputationCache cache) {
        this.store = store;
        this.cache = cache;
    }

    JSONObject read(String id) throws Exception {
        return cache.read(id, () -> witness(id), () -> {
            // Preserve the original verifier and strengthen its disk/memory binding on a miss.
            synchronized (store) {
                if (!store.load().similar(WorldFrames.copy(store.state))) {
                    throw new IllegalStateException("Persisted small-world evidence differs from memory");
                }
            }
            return store.verifyWorld(id);
        });
    }

    private String witness(String id) throws Exception {
        var fingerprint = new EvidenceFingerprint();
        synchronized (store) {
            if (store.closed) throw new IllegalStateException("Small-world store is closed");
            fingerprint.file(store.root, 48_000_000).text(store.state.toString());
            var world = store.find("worlds", id);
            fingerprint.text(id).text(world.toString());
            for (Object raw : world.getJSONArray("history")) {
                var job = store.find("jobs", ((JSONObject) raw).getString("jobId"));
                var report = store.artifacts.read(job.getJSONObject("report").getString("id"));
                for (Object name : report.getJSONArray("files")) {
                    EvidenceFingerprint.regular(store.artifacts.asset(
                            report.getString("id"), report.getInt("version"), name.toString()));
                }
                fingerprint.text(report.toString());
            }
        }
        return fingerprint.code(VerifiedHistory.class, JSONObject.class,
                com.jme3.bullet.PhysicsSpace.class).finish();
    }

    JSONObject status() { return cache.status(); }
}
