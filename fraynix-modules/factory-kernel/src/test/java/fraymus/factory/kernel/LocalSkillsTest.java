package fraymus.factory.kernel;

import brain_architecture.kernel.*;
import fraymus.factory.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import static fraymus.factory.kernel.LocalSkillDispatcher.Capability.*;

public final class LocalSkillsTest {
    public static void main(String[] args) throws Exception {
        Path workspace = Files.createTempDirectory("local-skills-");
        Path source = workspace.resolve("Feature.java");
        Files.writeString(source, "class Feature { static { System.setProperty(\"studio.tool.executed\",\"bad\"); } int answer() { return 42; } }");
        Files.writeString(workspace.resolve("notes.md"), "Factory rigidbody scene checkpoint: verify persistent component storage.");
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(source)));
        var world = new WorldSnapshot(7, List.of(new EntityRecord(UUID.randomUUID(), "entity \"one\"", List.of(
            new ComponentRecord(UUID.randomUUID(), "rigidbody", 1, Map.of("mass", "2.5", "shape", "box\n2d"))))));
        int receiptCount;
        try (var kernel = new OfflineKernel(KernelConfiguration.defaultForWorkspace(workspace))) {
            kernel.ingestArtifact(workspace.resolve("notes.md"));
            var dispatcher = new LocalSkillDispatcher(workspace, kernel); var all = EnumSet.allOf(LocalSkillDispatcher.Capability.class);
            if (dispatcher.definitions().size() != 5 || dispatcher.definitions().stream().anyMatch(d -> !d.implementationSha256().matches("[0-9a-f]{64}"))) throw new AssertionError("Missing implementation identity");
            var observed = dispatcher.invoke("world.inspect", Map.of(), Set.of(WORLD_READ), world);
            if (!observed.output().get("components").equals("1") || !observed.output().get("revision").equals("7") || !observed.output().get("world").contains("rigidbody")) throw new AssertionError("Invented world output");
            var search = dispatcher.invoke("memory.search", Map.of("query", "rigidbody checkpoint"), Set.of(MEMORY_READ), null);
            if (Integer.parseInt(search.output().get("hits")) < 1) throw new AssertionError("Memory tool disconnected");
            var parsed = dispatcher.invoke("java.analyze", Map.of("path", "Feature.java"), Set.of(WORKSPACE_READ), null);
            if (!parsed.output().get("syntaxValid").equals("true") || !parsed.output().get("methods").equals("1") || !parsed.output().get("sha256").equals(digest) || System.getProperty("studio.tool.executed") != null) throw new AssertionError("Parser failed or executed source");
            Files.writeString(workspace.resolve("Broken.java"), "class Broken { void nope(");
            var broken = dispatcher.invoke("java.analyze", Map.of("path", "Broken.java"), Set.of(WORKSPACE_READ), null);
            if (!broken.output().get("syntaxValid").equals("false") || !broken.output().get("diagnostics").contains("ERROR")) throw new AssertionError("Parser errors hidden");
            var report = dispatcher.invoke("docs.generate", Map.of("title", "Live scene data"), Set.of(WORLD_READ, REPORT_WRITE), world);
            Path reportFile = workspace.resolve(report.output().get("path"));
            if (!Files.readString(reportFile).contains("rigidbody")) throw new AssertionError("Generated docs disconnected from world");
            var checked = dispatcher.invoke("artifact.verify", Map.of("path", "Feature.java", "expectedSha256", digest), Set.of(WORKSPACE_READ), null);
            if (!checked.output().get("matches").equals("true")) throw new AssertionError("Valid artifact rejected");
            checked = dispatcher.invoke("artifact.verify", Map.of("path", "Feature.java", "expectedSha256", "0".repeat(64)), Set.of(WORKSPACE_READ), null);
            if (!checked.output().get("matches").equals("false")) throw new AssertionError("Incorrect digest accepted");
            reject(() -> dispatcher.invoke("docs.generate", Map.of("title", "Denied"), Set.of(WORLD_READ), world));
            reject(() -> dispatcher.invoke("java.analyze", Map.of("path", "Feature.java", "grants", "WORKSPACE_READ"), Set.of(), null));
            reject(() -> dispatcher.invoke("java.analyze", Map.of("path", "Feature.java", "unexpected", "field"), all, null));
            reject(() -> dispatcher.invoke("java.analyze", Map.of("path", "../escape.java"), all, null));
            reject(() -> dispatcher.invoke("java.analyze", Map.of("path", source.toString()), all, null));
            reject(() -> dispatcher.invoke("artifact.verify", Map.of("path", "Feature.java", "expectedSha256", "not a hash"), all, null));
            reject(() -> dispatcher.invoke("world.inspect", Map.of(), all, null));
            reject(() -> dispatcher.invoke("made-up.skill", Map.of(), all, world));
            Path outside = Files.createTempFile("outside-skills-", ".java");
            boolean links = false;
            try { Files.createSymbolicLink(workspace.resolve("linked.java"), outside); links = true; }
            catch (java.io.IOException | UnsupportedOperationException unavailable) { System.out.println("Symlink creation unavailable; traversal and absolute-path checks still exercised"); }
            if (links) reject(() -> dispatcher.invoke("java.analyze", Map.of("path", "linked.java"), all, null));
            if (!Files.readString(source).contains("return 42")) throw new AssertionError("Source modified");
            receiptCount = (int)kernel.getJournal().getEntries().stream().filter(e -> e.type().equals("skill_succeeded")).count();
            if (receiptCount != 7) throw new AssertionError("Missing success receipts: " + receiptCount);
            Files.createDirectories(workspace.resolve("docs"));
            Files.writeString(workspace.resolve("README.md"), "# Studio project\nLocal tools and verified journal continuity.");
            Files.writeString(workspace.resolve("docs/ARCHITECTURE.md"), "# Architecture\nImmutable world snapshots and background tools.");
            Files.writeString(workspace.resolve("docs/CONTINUITY_AND_SKILLS.md"), "# Current plan\nDurable jobs and scoped agent tools.");
            Files.writeString(workspace.resolve("docs/VERIFICATION.md"), "# Verification\nLocal evidence and remaining limits.");
            var memory = kernel.refreshProjectMemoryArtifacts(false);
            if (!memory.canonicalDoc().equals(workspace.resolve("README.md")) || !memory.architectureDoc().equals(workspace.resolve("docs/ARCHITECTURE.md"))
                    || !memory.goalsDoc().equals(workspace.resolve("docs/CONTINUITY_AND_SKILLS.md")) || !memory.artifactPaths().contains(Path.of("docs/VERIFICATION.md"))) throw new AssertionError("Standalone project memory still uses legacy paths");
            if (!Files.readString(memory.operationalMemory()).contains("Studio Integration") || kernel.getMemoryStore().search("verified journal continuity", List.of(), 8).isEmpty()) throw new AssertionError("Current project memory was not re-ingested");
            kernel.getJournal().append("skill_started", "docs.generate", Map.of("callId", "simulated-interrupted-call"));
            kernel.getJournal().verify();
        }
        try (var service = new FactoryKernelService(workspace)) {
            var monitor = service.monitor("rigidbody");
            if (!monitor.status().get("journalIntegrity").equals("verified") || monitor.skills().size() != 5 || monitor.skillReceipts().isEmpty()) throw new AssertionError("Restart lost tool/continuity evidence");
            if (!monitor.status().get("unfinishedSkillCalls").contains("simulated-interrupted-call")) throw new AssertionError("Interrupted tool evidence lost");
            try (var reports = Files.list(workspace.resolve("skill-reports"))) {
                if (reports.count() != 1) throw new AssertionError("Unfinished call was replayed without authorization");
            }
        }
        System.out.println("LocalSkillsTest passed: all five tools, real AST/errors, hash mismatch, reference-data isolation, capabilities, path boundaries and persisted receipts");
    }
    private interface Attempt { void run() throws Exception; }
    private static void reject(Attempt attempt) throws Exception {
        try { attempt.run(); throw new AssertionError("Invalid request accepted"); }
        catch (IllegalArgumentException | SecurityException | java.io.IOException expected) { }
    }
}
