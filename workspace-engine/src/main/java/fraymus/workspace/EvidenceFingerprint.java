package fraymus.workspace;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Reads actual bytes on every call. File metadata never establishes equality. */
final class EvidenceFingerprint {
    private final MessageDigest digest;
    private long bytes;

    EvidenceFingerprint() throws Exception {
        digest = MessageDigest.getInstance("SHA-256");
        text(System.getProperty("java.runtime.version"));
        text(System.getProperty("java.vm.name"));
    }

    EvidenceFingerprint text(String value) {
        byte[] data = value.getBytes(StandardCharsets.UTF_8);
        digest.update((data.length + ":").getBytes(StandardCharsets.UTF_8));
        digest.update(data);
        return this;
    }

    EvidenceFingerprint file(Path path, long limit) throws Exception {
        regular(path);
        long size = Files.size(path);
        if (size > limit || size > 160_000_000 - bytes) {
            throw new IllegalStateException("Evidence witness byte budget exceeded");
        }
        text(path.toAbsolutePath().normalize().toString());
        text(Long.toString(size));
        long count = 0;
        byte[] buffer = new byte[65536];
        try (var in = Files.newInputStream(path)) {
            int length;
            while ((length = in.read(buffer)) != -1) {
                count += length;
                bytes += length;
                if (count > limit || bytes > 160_000_000) {
                    throw new IllegalStateException("Evidence witness grew beyond its budget");
                }
                digest.update(buffer, 0, length);
            }
        }
        if (count != size) throw new IllegalStateException("Evidence changed during byte read");
        regular(path);
        return this;
    }

    EvidenceFingerprint code(Class<?>... types) throws Exception {
        var origins = new LinkedHashSet<Path>();
        for (Class<?> type : types) {
            origins.add(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()));
        }
        var files = new TreeSet<Path>();
        for (Path origin : origins) {
            ReplayEvidence157.unlinkedRealPath(origin);
            if (Files.isDirectory(origin, LinkOption.NOFOLLOW_LINKS)) {
                try (var walk = Files.walk(origin)) {
                    for (Path path : walk.toList()) {
                        ReplayEvidence157.unlinkedRealPath(path);
                        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) continue;
                        regular(path);
                        if (path.toString().endsWith(".class")) files.add(path);
                    }
                }
            } else files.add(origin);
        }
        if (files.size() > 8192) throw new IllegalStateException("Implementation inventory too large");
        for (Path path : files) file(path, 128_000_000);
        return this;
    }

    String finish() { return HexFormat.of().formatHex(digest.digest()); }

    static void regular(Path path) throws Exception {
        ReplayEvidence157.unlinkedRealPath(path);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("Original regular evidence file missing");
        }
    }
}
