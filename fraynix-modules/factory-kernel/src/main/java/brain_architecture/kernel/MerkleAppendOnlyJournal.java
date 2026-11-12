package brain_architecture.kernel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Verified hash-chain continuity. Integrity evidence, not a signature or an execution sandbox. */
public final class MerkleAppendOnlyJournal {
    private static final Map<Path, Object> LOCKS = new ConcurrentHashMap<>();
    private final Path logFile;
    private List<JournalEntry> entries = List.of();
    public record Verification(int entries, String lastEntryHash, String merkleRoot, List<String> pendingFiles) {}

    public MerkleAppendOnlyJournal(Path stateRoot) throws IOException {
        Files.createDirectories(stateRoot);
        logFile = stateRoot.toRealPath().resolve("kernel-journal.log");
        verify();
    }

    /** Cross-process serialization and atomic replacement prevent publishing a partially written append. */
    public synchronized JournalEntry append(String type, String message, Map<String, String> metadata) throws IOException {
        Objects.requireNonNull(type); Objects.requireNonNull(message);
        Map<String, String> immutable = metadata == null ? Map.of() : Map.copyOf(metadata);
        synchronized (LOCKS.computeIfAbsent(logFile, ignored -> new Object())) {
            try (var channel = FileChannel.open(lockFile(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 var held = channel.lock()) {
                String original = read();
                List<JournalEntry> disk = decodeVerified(original);
                requirePrefix(disk);
                int index = disk.size(); long timestamp = System.currentTimeMillis();
                String previous = index == 0 ? "GENESIS" : disk.get(index - 1).entryHash();
                String payload = payload(type, message, serializeMetadata(immutable, true), true);
                String hash = entryHash(index, timestamp, payload, previous);
                var tree = new MerkleTree(); disk.forEach(e -> tree.add(e.entryHash())); tree.add(hash);
                var entry = new JournalEntry(index, timestamp, type, message, immutable, previous, payload, hash, tree.root());
                String separator = original.isEmpty() || original.endsWith("\n") ? "" : "\n";
                atomicWrite(logFile, (original + separator + encode(entry) + "\n").getBytes(StandardCharsets.UTF_8));
                var updated = new ArrayList<>(disk); updated.add(entry); entries = List.copyOf(updated);
                return entry;
            }
        }
    }

    public synchronized Verification verify() throws IOException {
        synchronized (LOCKS.computeIfAbsent(logFile, ignored -> new Object())) {
            try (var channel = FileChannel.open(lockFile(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 var held = channel.lock()) {
                List<JournalEntry> disk = decodeVerified(read()); requirePrefix(disk); entries = List.copyOf(disk);
                List<String> pending;
                try (var files = Files.list(logFile.getParent())) {
                    pending = files.filter(p -> p.getFileName().toString().matches("kernel-journal-.*\\.pending"))
                        .map(p -> p.getFileName().toString()).sorted().toList();
                }
                return new Verification(size(), entries.isEmpty() ? "" : entries.get(entries.size() - 1).entryHash(), getLastMerkleRoot(), pending);
            }
        }
    }

    /** Explicit legacy-tail recovery requires an independently retained expected prefix root and count.
     * Complete invalid records are never automatically dropped. The original bytes remain as evidence. */
    public static Path recoverUnterminatedTail(Path stateRoot, int expectedCount, String expectedRoot) throws IOException {
        Path file = stateRoot.toRealPath().resolve("kernel-journal.log");
        synchronized (LOCKS.computeIfAbsent(file, ignored -> new Object())) {
            try (var channel = FileChannel.open(file.resolveSibling("kernel-journal.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 var held = channel.lock()) {
                byte[] original = Files.readAllBytes(file);
                if (original.length == 0 || original[original.length - 1] == '\n') throw new IOException("No unterminated tail to recover");
                int cut = original.length - 1; while (cut >= 0 && original[cut] != '\n') cut--;
                byte[] prefix = Arrays.copyOf(original, cut + 1);
                var valid = decodeVerified(new String(prefix, StandardCharsets.UTF_8));
                String root = valid.isEmpty() ? "" : valid.get(valid.size() - 1).merkleRoot();
                if (expectedCount != valid.size() || !root.equals(expectedRoot)) throw new IOException("Recovery anchor does not match the verified prefix");
                try { decodeVerified(new String(original, StandardCharsets.UTF_8)); throw new IOException("Tail is a complete valid record; recovery is unnecessary"); }
                catch (JournalIntegrityException partial) { /* Explicitly anchored recovery only. */ }
                Path evidence = file.resolveSibling("journal-recovery-" + UUID.randomUUID() + ".evidence");
                writeForced(evidence, original);
                atomicWrite(file, prefix);
                return evidence;
            }
        }
    }

    /** Recover only one complete, verified atomic append extending the exact committed bytes.
     * The owner validates domain evidence before publication; both original byte sequences are preserved. */
    public synchronized Path recoverPendingAppend(java.util.function.Consumer<List<JournalEntry>> ownerValidation) throws IOException {
        synchronized (LOCKS.computeIfAbsent(logFile, ignored -> new Object())) {
            try (var channel = FileChannel.open(lockFile(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 var held = channel.lock()) {
                byte[] original = Files.exists(logFile) ? Files.readAllBytes(logFile) : new byte[0];
                var disk = decodeVerified(new String(original, StandardCharsets.UTF_8));requirePrefix(disk);
                List<Path> pending;
                try(var files=Files.list(logFile.getParent())){pending=files.filter(p->p.getFileName().toString().matches("kernel-journal-.*\\.pending")).toList();}
                if(pending.size()!=1)throw new IOException("Recovery requires exactly one pending append; originals retained");
                Path candidate=pending.getFirst();byte[] proposed=Files.readAllBytes(candidate);
                if(proposed.length==0||proposed[proposed.length-1]!='\n')throw new IOException("Pending append is incomplete; original retained");
                var complete=decodeVerified(new String(proposed,StandardCharsets.UTF_8));
                if(complete.size()!=disk.size()+1||proposed.length<=original.length||!Arrays.equals(original,Arrays.copyOf(proposed,original.length)))throw new IOException("Pending append does not extend the exact committed history");
                for(int i=0;i<disk.size();i++)if(!disk.get(i).equals(complete.get(i)))throw new IOException("Pending append changed committed evidence");
                try{ownerValidation.accept(List.copyOf(complete));}catch(RuntimeException invalid){throw new IOException("Pending append failed owner evidence checks; original retained",invalid);}
                String key=complete.getLast().entryHash();
                Path evidence=logFile.resolveSibling("journal-recovery-"+key+".evidence");
                preserveRecovery(evidence,proposed);preserveRecovery(logFile.resolveSibling("journal-recovery-"+key+".before"),original);
                commitPending(candidate,logFile);entries=List.copyOf(complete);return evidence;
            }
        }
    }
    private static void preserveRecovery(Path path,byte[] bytes)throws IOException{
        if(Files.exists(path)){if(!Arrays.equals(Files.readAllBytes(path),bytes))throw new IOException("Retained recovery evidence changed");}
        else writeForced(path,bytes);
    }

    public synchronized List<JournalEntry> getEntries() { return List.copyOf(entries); }
    public synchronized int size() { return entries.size(); }
    public synchronized String getLastMerkleRoot() { return entries.isEmpty() ? "" : entries.get(entries.size() - 1).merkleRoot(); }
    private Path lockFile() { return logFile.resolveSibling("kernel-journal.lock"); }
    private String read() throws IOException { return Files.exists(logFile) ? Files.readString(logFile, StandardCharsets.UTF_8) : ""; }
    private void requirePrefix(List<JournalEntry> disk) throws IOException {
        if (disk.size() < entries.size()) throw new JournalIntegrityException("Journal shortened since this instance verified it");
        for (int i = 0; i < entries.size(); i++) if (!entries.get(i).entryHash().equals(disk.get(i).entryHash()))
            throw new JournalIntegrityException("Journal history changed at entry " + i);
    }

    private static List<JournalEntry> decodeVerified(String text) throws IOException {
        var result = new ArrayList<JournalEntry>(); var tree = new MerkleTree();
        String previous = "GENESIS"; boolean sawV2 = false;
        String[] lines = text.split("\n", -1);
        for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            String line = lines[lineIndex];
            if (line.endsWith("\r")) line = line.substring(0, line.length() - 1);
            if (line.isEmpty() && lineIndex == lines.length - 1) break;
            try {
                String[] p = line.split("\t", -1); boolean v2 = p.length == 10 && p[0].equals("J2"); int offset = v2 ? 1 : 0;
                if (p.length != (v2 ? 10 : 9)) throw new IllegalArgumentException("Wrong field count");
                if (sawV2 && !v2) throw new IllegalArgumentException("Legacy format after J2 entry"); sawV2 |= v2;
                int index = Integer.parseInt(p[offset]); long at = Long.parseLong(p[offset + 1]);
                if (!Integer.toString(index).equals(p[offset]) || !Long.toString(at).equals(p[offset + 1])) throw new IllegalArgumentException("Noncanonical sequence/timestamp");
                String type = unbase64(p[offset + 2]), message = unbase64(p[offset + 3]);
                String serialized = unbase64(p[offset + 4]); Map<String, String> metadata = deserializeMetadata(serialized, v2);
                if (!serialized.equals(serializeMetadata(metadata, v2))) throw new IllegalArgumentException("Noncanonical metadata");
                String payload = payload(type, message, serialized, v2);
                String hash = entryHash(index, at, payload, previous); tree.add(hash);
                if (index != result.size() || at < 0 || !previous.equals(p[offset + 5]) || !payload.equals(p[offset + 6])
                        || !hash.equals(p[offset + 7]) || !tree.root().equals(p[offset + 8])) throw new IllegalArgumentException("Sequence/hash/Merkle mismatch");
                result.add(new JournalEntry(index, at, type, message, metadata, previous, payload, hash, tree.root())); previous = hash;
            } catch (RuntimeException invalid) { throw new JournalIntegrityException("Invalid journal entry " + result.size() + "; original file retained", invalid); }
        }
        return List.copyOf(result);
    }

    private static String encode(JournalEntry e) {
        return "J2\t" + e.index() + "\t" + e.timestamp() + "\t" + base64(e.type()) + "\t" + base64(e.message()) + "\t"
            + base64(serializeMetadata(e.metadata(), true)) + "\t" + e.previousHash() + "\t" + e.payloadHash() + "\t" + e.entryHash() + "\t" + e.merkleRoot();
    }
    private static String payload(String type, String message, String metadata, boolean v2) {
        return sha256(v2 ? "J2|" + base64(type) + "|" + base64(message) + "|" + metadata : type + "|" + message + "|" + metadata);
    }
    private static String entryHash(int index, long at, String payload, String previous) { return sha256(index + "|" + at + "|" + payload + "|" + previous); }
    private static String serializeMetadata(Map<String, String> metadata, boolean v2) {
        var pairs = new ArrayList<String>();
        new TreeMap<>(metadata).forEach((k, v) -> pairs.add((v2 ? base64(k) : k) + (v2 ? ":" : "=") + base64(v)));
        return String.join(";", pairs);
    }
    private static Map<String, String> deserializeMetadata(String text, boolean v2) {
        var values = new TreeMap<String, String>();
        if (!text.isEmpty()) for (String pair : text.split(";", -1)) {
            int at = pair.indexOf(v2 ? ':' : '='); if (at < 0) throw new IllegalArgumentException("Invalid metadata");
            String key = v2 ? unbase64(pair.substring(0, at)) : pair.substring(0, at);
            if (values.put(key, unbase64(pair.substring(at + 1))) != null) throw new IllegalArgumentException("Duplicate metadata key");
        }
        return Map.copyOf(values);
    }
    private static String base64(String value) { return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8)); }
    private static String unbase64(String value) {
        String decoded = new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
        if (!base64(decoded).equals(value)) throw new IllegalArgumentException("Noncanonical base64/UTF-8"); return decoded;
    }
    private static String sha256(String text) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    private static void atomicWrite(Path file, byte[] data) throws IOException {
        Path pending = file.resolveSibling("kernel-journal-" + UUID.randomUUID() + ".pending");
        writeForced(pending, data);
        commitPending(pending,file);
    }
    private static void commitPending(Path pending,Path file)throws IOException {
        // No non-atomic fallback: a failed commit retains the previous journal and pending evidence.
        // Windows readers can briefly deny deletion of the destination. Retry only that
        // failure, with the same forced bytes and cross-process lock still held.
        for (int attempt = 0;; attempt++) {
            try {
                Files.move(pending, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                break;
            } catch (AccessDeniedException denied) {
                if (attempt >= 5) throw denied;
                try { Thread.sleep(25L << attempt); }
                catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    var failure = new java.io.InterruptedIOException("Journal replacement interrupted; previous and pending bytes retained");
                    failure.initCause(denied);
                    throw failure;
                }
            }
        }
    }
    private static void writeForced(Path file, byte[] data) throws IOException {
        try (var channel = FileChannel.open(file, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            ByteBuffer bytes = ByteBuffer.wrap(data); while (bytes.hasRemaining()) channel.write(bytes); channel.force(true);
        }
    }
    /** Incremental tree with the legacy duplicate-last-node convention. */
    private static final class MerkleTree {
        final List<String> levels = new ArrayList<>();
        void add(String hash) {
            int level = 0;
            while (level < levels.size() && levels.get(level) != null) { hash = sha256(levels.get(level) + hash); levels.set(level++, null); }
            if (level == levels.size()) levels.add(hash); else levels.set(level, hash);
        }
        String root() {
            String right = null; int height = 0;
            for (int level = 0; level < levels.size(); level++) if (levels.get(level) != null) {
                if (right == null) { right = levels.get(level); height = level; }
                else { while (height < level) { right = sha256(right + right); height++; } right = sha256(levels.get(level) + right); height++; }
            }
            return right == null ? "" : right;
        }
    }
    public static final class JournalIntegrityException extends IOException {
        public JournalIntegrityException(String message) { super(message); }
        public JournalIntegrityException(String message, Throwable cause) { super(message, cause); }
    }
    public record JournalEntry(int index, long timestamp, String type, String message, Map<String, String> metadata,
                               String previousHash, String payloadHash, String entryHash, String merkleRoot) {
        public JournalEntry { metadata = Map.copyOf(metadata); }
    }
}
