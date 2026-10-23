package fraymus.factory.kernel;

import brain_architecture.kernel.MerkleAppendOnlyJournal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

public final class JournalContinuityTest {
    public static void main(String[] args) throws Exception {
        if (args.length == 1) { var journal = new MerkleAppendOnlyJournal(Path.of(args[0])); for (int i = 0; i < 12; i++) journal.append("child", "event " + i, Map.of()); return; }
        Path root = Files.createTempDirectory("journal-continuity-"); Path file = root.resolve("kernel-journal.log");
        String legacy = legacy(); Files.writeString(file, legacy);
        var journal = new MerkleAppendOnlyJournal(root);
        if (journal.size() != 1) throw new AssertionError("Legacy format rejected");
        var mutable = new HashMap<String, String>(); mutable.put("a;=|\t", "你好;\n|=");
        journal.append("type|one", "message|two", mutable); mutable.clear();
        if (journal.getEntries().get(1).metadata().isEmpty()) throw new AssertionError("Mutable metadata alias");
        for (int i = 0; i < 32; i++) {
            journal.append("event", "prefix " + i, Map.of("n", "" + i));
            var all = journal.getEntries();
            if (!root(all.stream().map(e -> e.entryHash()).toList()).equals(journal.getLastMerkleRoot())) throw new AssertionError("Merkle root differs at " + all.size());
        }
        byte[] valid = Files.readAllBytes(file); var reopened = new MerkleAppendOnlyJournal(root);
        if (reopened.size() != journal.size()) throw new AssertionError("Restart lost entries");
        // Every serialized field is checked, including fields that previously loaded without validation.
        String[] lines = Files.readString(file).split("\n");
        for (int field = 0; field < 10; field++) {
            String[] values = lines[1].split("\t", -1); values[field] = switch (field) {
                case 0 -> "J3"; case 1 -> "99"; case 2 -> "42"; case 3, 4, 5 -> b64("tampered"); default -> "0".repeat(64);
            };
            String[] corrupt = lines.clone(); corrupt[1] = String.join("\t", values);
            Files.writeString(file, String.join("\n", corrupt) + "\n");
            expectInvalid(root); if (!Arrays.equals(Files.readAllBytes(file), (String.join("\n", corrupt) + "\n").getBytes(StandardCharsets.UTF_8))) throw new AssertionError("Evidence altered");
        }
        Files.write(file, valid); Files.writeString(file, "J2\t35\tpartial", StandardOpenOption.APPEND);
        byte[] torn = Files.readAllBytes(file); expectInvalid(root);
        try { MerkleAppendOnlyJournal.recoverUnterminatedTail(root, journal.size() + 1, journal.getLastMerkleRoot()); throw new AssertionError("Wrong recovery anchor accepted"); }
        catch (java.io.IOException expected) { }
        if (!Arrays.equals(torn, Files.readAllBytes(file))) throw new AssertionError("Failed recovery changed evidence");
        Path evidence = MerkleAppendOnlyJournal.recoverUnterminatedTail(root, journal.size(), journal.getLastMerkleRoot());
        if (!Arrays.equals(Files.readAllBytes(evidence), torn) || !Arrays.equals(Files.readAllBytes(file), valid)) throw new AssertionError("Recovery did not preserve exact original bytes");
        // Simulate a crash after the pending file was forced but before rename. Never execute/replay it.
        Path pending = root.resolve("kernel-journal-crash.pending"); Files.writeString(pending, "uncommitted bytes");
        var recovered = new MerkleAppendOnlyJournal(root);
        if (recovered.size() != journal.size() || !recovered.verify().pendingFiles().contains(pending.getFileName().toString())) throw new AssertionError("Staged crash evidence lost");
        Files.writeString(file, legacy); // Fully valid prefix, but shorter than this already-open instance saw.
        try { journal.append("oops", "must fail", Map.of()); throw new AssertionError("Live truncation accepted"); }
        catch (java.io.IOException expected) { }
        if (journal.size() != lines.length) throw new AssertionError("Failed append published in memory");
        Files.write(file, valid);
        var writers = Executors.newFixedThreadPool(2);
        try {
            var futures = new ArrayList<Future<?>>();
            for (int t = 0; t < 2; t++) futures.add(writers.submit(() -> {
                try { var writer = new MerkleAppendOnlyJournal(root); for (int i = 0; i < 10; i++) writer.append("thread", "event", Map.of()); }
                catch (Exception error) { throw new RuntimeException(error); }
            }));
            for (var future : futures) future.get(30, TimeUnit.SECONDS);
        } finally { writers.shutdownNow(); }
        // Separate JVM writers exercise the file lock rather than just the in-process mutex.
        var children = new ArrayList<Process>();
        for (int i = 0; i < 2; i++) children.add(new ProcessBuilder(java(), "-cp", System.getProperty("java.class.path"), JournalContinuityTest.class.getName(), root.toString()).redirectErrorStream(true).start());
        for (var child : children) {
            if (!child.waitFor(30, TimeUnit.SECONDS)) { child.destroyForcibly(); throw new AssertionError("Journal child timed out"); }
            if (child.exitValue() != 0) throw new AssertionError(new String(child.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        }
        if (new MerkleAppendOnlyJournal(root).size() != lines.length + 44) throw new AssertionError("Concurrent events lost");
        Path failedRoot = Files.createTempDirectory("journal-failure-"); var failed = new MerkleAppendOnlyJournal(failedRoot);
        Files.createDirectory(failedRoot.resolve("kernel-journal.log"));
        try { failed.append("event", "unwritable", Map.of()); throw new AssertionError("Write failure ignored"); } catch (java.io.IOException expected) { }
        if (failed.size() != 0) throw new AssertionError("Failed disk operation published an entry");
        System.out.println("JournalContinuityTest passed: legacy/J2, every field tamper, Merkle prefixes, anchored tail recovery, staged crash, failed write, threads and JVM writers");
    }
    private static String java() { return Path.of(System.getProperty("java.home"), "bin", System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString(); }
    private static void expectInvalid(Path root) throws Exception { try { new MerkleAppendOnlyJournal(root); throw new AssertionError("Corruption accepted"); } catch (MerkleAppendOnlyJournal.JournalIntegrityException expected) { } }
    private static String legacy() throws Exception {
        String metadata = "key=" + b64("value"), payload = hash("old|message|" + metadata), entry = hash("0|123|" + payload + "|GENESIS");
        return "0\t123\t" + b64("old") + "\t" + b64("message") + "\t" + b64(metadata) + "\tGENESIS\t" + payload + "\t" + entry + "\t" + entry + "\n";
    }
    private static String root(List<String> hashes) throws Exception {
        var level = hashes;
        while (level.size() > 1) { var next = new ArrayList<String>(); for (int i = 0; i < level.size(); i += 2) next.add(hash(level.get(i) + level.get(Math.min(i + 1, level.size() - 1)))); level = next; }
        return level.get(0);
    }
    private static String b64(String text) { return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8)); }
    private static String hash(String text) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
}
