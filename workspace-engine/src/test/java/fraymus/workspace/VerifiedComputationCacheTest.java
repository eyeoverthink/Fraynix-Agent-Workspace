package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicInteger;

public class VerifiedComputationCacheTest {
    @Test public void completedValuesAreIsolatedAndEveryHitChecksBothWitnesses() throws Exception {
        var cache = new VerifiedComputationCache(true);
        var checks = new AtomicInteger();
        var computes = new AtomicInteger();
        VerifiedComputationCache.Work<String> witness = () -> { checks.incrementAndGet(); return "same"; };
        VerifiedComputationCache.Work<JSONObject> original = () -> {
            computes.incrementAndGet();
            return new JSONObject("{nested:{values:[1,2,3]}}");
        };
        var first = cache.read("a", witness, original);
        first.getJSONObject("nested").getJSONArray("values").put(0, 999);
        assertEquals(1, cache.read("a", witness, original).getJSONObject("nested")
                .getJSONArray("values").getInt(0));
        assertEquals(4, checks.get());
        assertEquals(1, computes.get());
        assertEquals(1, cache.status().getInt("reused"));
        var changed = new AtomicInteger();
        assertThrows(Exception.class, () -> cache.read("a",
                () -> changed.incrementAndGet() == 1 ? "same" : "changed", original));
        assertEquals(0, cache.status().getInt("entries"));
    }

    @Test public void failuresEvictionAndDisabledModeNeverInventAResult() throws Exception {
        var cache = new VerifiedComputationCache(true);
        var calls = new AtomicInteger();
        VerifiedComputationCache.Work<JSONObject> original = () -> {
            calls.incrementAndGet(); return new JSONObject().put("n", 7);
        };
        for (int i = 0; i < 5; i++) cache.read("key" + i, () -> "same", original);
        assertEquals(4, cache.status().getInt("entries"));
        cache.read("key0", () -> "same", original);
        assertEquals(6, calls.get());
        var failure = new IllegalStateException("Original refusal");
        assertSame(failure, assertThrows(Exception.class,
                () -> cache.read("key0", () -> "new", () -> { throw failure; })));
        assertEquals(0, cache.status().getInt("entries"));
        var disabled = new VerifiedComputationCache(false);
        disabled.read("a", () -> "same", original);
        disabled.read("a", () -> "same", original);
        assertEquals(2, disabled.status().getInt("computed"));
        assertEquals(0, disabled.status().getInt("entries"));
    }

    @Test public void witnessReadsBytesAndImplementationInventoryNotTimestamps() throws Exception {
        var file = Files.createTempFile("witness", ".json");
        Files.writeString(file, "original");
        var time = Files.getLastModifiedTime(file);
        String first = new EvidenceFingerprint().file(file, 20).finish();
        Files.writeString(file, "changed!");
        Files.setLastModifiedTime(file, time);
        assertNotEquals(first, new EvidenceFingerprint().file(file, 20).finish());
        assertThrows(Exception.class, () -> new EvidenceFingerprint().file(file, 2));
        Files.delete(file);
        assertThrows(Exception.class, () -> new EvidenceFingerprint().file(file, 20));
        Path origin = Path.of(VerifiedComputationCache.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Path marker = origin.resolve("EvidenceInventoryTest.class");
        assertFalse(Files.exists(marker));
        first = new EvidenceFingerprint().code(VerifiedComputationCache.class).finish();
        try {
            Files.write(marker, new byte[]{1,2,3});
            assertNotEquals(first, new EvidenceFingerprint().code(VerifiedComputationCache.class).finish());
        } finally { Files.deleteIfExists(marker); }
    }
}
