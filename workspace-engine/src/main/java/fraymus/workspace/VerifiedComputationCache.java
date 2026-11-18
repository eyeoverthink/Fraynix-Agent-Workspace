package fraymus.workspace;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import org.json.JSONObject;

/** Bounded reuse of completed computation, never of unchecked inputs or mutable results. */
final class VerifiedComputationCache {
    @FunctionalInterface interface Work<T> { T run() throws Exception; }
    private record Entry(String witness, String result) {}
    private final LinkedHashMap<String, Entry> entries = new LinkedHashMap<>(8, .75f, true);
    private final boolean enabled;
    private long reused, computed, refused;

    VerifiedComputationCache() {
        this(!"off".equals(System.getenv("FRAYNIX_WORKSPACE_EVIDENCE_REUSE")));
    }

    VerifiedComputationCache(boolean enabled) { this.enabled = enabled; }

    synchronized JSONObject read(String key, Work<String> witness, Work<JSONObject> original)
            throws Exception {
        try {
            String before = witness.run();
            Entry prior = entries.get(key);
            boolean hit = enabled && prior != null && prior.witness.equals(before);
            JSONObject result;
            if (hit) {
                result = new JSONObject(prior.result);
            } else {
                entries.remove(key);
                computed++;
                result = original.run();
            }
            String after = witness.run();
            if (!before.equals(after)) {
                throw new IllegalStateException("Original evidence changed during checked computation");
            }
            if (hit) reused++;
            String text = result.toString();
            if (enabled && text.getBytes(StandardCharsets.UTF_8).length <= 2_000_000) {
                entries.put(key, new Entry(after, text));
                while (entries.size() > 4) entries.remove(entries.keySet().iterator().next());
            }
            return result;
        } catch (Exception failure) {
            entries.clear();
            refused++;
            throw failure;
        }
    }

    static String identity() {
        var types = new java.util.ArrayList<Class<?>>();
        types.add(VerifiedComputationCache.class);
        types.addAll(java.util.Arrays.asList(VerifiedComputationCache.class.getDeclaredClasses()));
        types.sort(java.util.Comparator.comparing(Class::getName));
        var value = new StringBuilder();
        for (Class<?> type : types) {
            try (var in = type.getResourceAsStream("/" + type.getName().replace('.', '/') + ".class")) {
                if (in == null) throw new IllegalStateException("Missing computation implementation");
                value.append(type.getName()).append(PerceptionStore.hash(in.readAllBytes()));
            } catch (Exception failure) { throw new IllegalStateException(failure); }
        }
        return MeasurementStore.hash(value.toString());
    }

    synchronized void clear() { entries.clear(); }

    synchronized JSONObject status() {
        return new JSONObject().put("enabled", enabled).put("entries", entries.size())
                .put("reused", reused).put("computed", computed).put("refused", refused)
                .put("capacity", 4).put("maxResultBytes", 2_000_000);
    }
}
