package fraymus.factory.kernel;

import brain_architecture.kernel.OfflineKernel;
import fraymus.factory.*;
import com.sun.source.tree.*;
import com.sun.source.util.*;
import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.time.Instant;
import java.util.*;

/** Compiled, local tools. Prose and model output cannot install handlers or grant capabilities. */
public final class LocalSkillDispatcher {
    public enum Capability { WORLD_READ, MEMORY_READ, WORKSPACE_READ, REPORT_WRITE }
    public record Definition(String id, int version, String description, String implementationSha256,
                             Set<Capability> capabilities, SkillSchema input, SkillSchema output) {
        public Definition { capabilities = Set.copyOf(capabilities); }
    }
    public record Result(UUID callId, String skill, String status, Map<String, String> output, int journalIndex) {
        public Result { output = Map.copyOf(output); }
    }
    private final Path workspace;
    private final OfflineKernel kernel;
    private final Map<String, Definition> definitions = new LinkedHashMap<>();
    private final String implementationHash;
    private static final long MAX_ARTIFACT_BYTES = 32L * 1024 * 1024;

    public LocalSkillDispatcher(Path workspace, OfflineKernel kernel) throws IOException {
        this.workspace = workspace.toRealPath(); this.kernel = Objects.requireNonNull(kernel);
        try (var code = LocalSkillDispatcher.class.getResourceAsStream("LocalSkillDispatcher.class")) {
            if (code == null) throw new IOException("Cannot identify tool implementation");
            implementationHash = hash(code.readAllBytes());
        }
        register("world.inspect", "Read the captured world, entities and component values.", Set.of(Capability.WORLD_READ), Map.of(),
            Map.of("revision", SkillSchema.integer(), "entities", SkillSchema.integer(), "components", SkillSchema.integer(), "world", SkillSchema.text(1_000_000)));
        register("memory.search", "Search persisted project memory using the kernel's existing hybrid index.", Set.of(Capability.MEMORY_READ),
            Map.of("query", SkillSchema.text(1024)), Map.of("hits", SkillSchema.integer(), "results", SkillSchema.text(100_000)));
        register("java.analyze", "Parse a local Java file with the JDK compiler AST; no source execution.", Set.of(Capability.WORKSPACE_READ),
            Map.of("path", SkillSchema.text(4096)), Map.of("path", SkillSchema.text(4096), "sha256", SkillSchema.hash(), "syntaxValid", SkillSchema.bool(),
                "types", SkillSchema.integer(), "methods", SkillSchema.integer(), "declarations", SkillSchema.text(100_000), "diagnostics", SkillSchema.text(100_000)));
        register("docs.generate", "Create a new Markdown report from the captured world.", Set.of(Capability.WORLD_READ, Capability.REPORT_WRITE),
            Map.of("title", SkillSchema.text(200)), Map.of("path", SkillSchema.text(4096), "sha256", SkillSchema.hash(), "revision", SkillSchema.integer()));
        register("artifact.verify", "Compare a workspace file to a supplied SHA-256 digest.", Set.of(Capability.WORKSPACE_READ),
            Map.of("path", SkillSchema.text(4096), "expectedSha256", SkillSchema.hash()), Map.of("path", SkillSchema.text(4096), "sha256", SkillSchema.hash(), "bytes", SkillSchema.integer(), "matches", SkillSchema.bool()));
    }
    private void register(String id, String description, Set<Capability> capabilities, Map<String, SkillSchema.Field> in, Map<String, SkillSchema.Field> out) {
        if (definitions.putIfAbsent(id, new Definition(id, 1, description, implementationHash, capabilities, new SkillSchema(in), new SkillSchema(out))) != null)
            throw new IllegalArgumentException("Duplicate skill id: " + id);
    }
    public List<Definition> definitions() { return List.copyOf(definitions.values()); }

    /** Grants and immutable world are supplied by the host, never read from the request fields. */
    public synchronized Result invoke(String id, Map<String, String> fields, Set<Capability> grants, WorldSnapshot world) throws IOException {
        UUID call = UUID.randomUUID(); long start = System.nanoTime();
        Definition definition = definitions.get(id);
        if (definition == null) {
            kernel.getJournal().append("skill_rejected", "unknown skill", Map.of("callId", call.toString(), "requestedId", String.valueOf(id).substring(0, Math.min(String.valueOf(id).length(), 128))));
            throw new IllegalArgumentException("Unknown local skill: " + id);
        }
        var audit = new TreeMap<String, String>();
        audit.put("callId", call.toString()); audit.put("skill", id); audit.put("version", Integer.toString(definition.version()));
        audit.put("implementationSha256", definition.implementationSha256());
        audit.put("requiredCapabilities", new TreeSet<>(definition.capabilities()).toString());
        audit.put("grants", new TreeSet<>(grants).toString());
        try {
            if (!grants.containsAll(definition.capabilities())) throw new SecurityException("Required capabilities: " + definition.capabilities());
            fields = definition.input().validate(fields); audit.put("inputSha256", hash(canonical(fields).getBytes(StandardCharsets.UTF_8)));
            fields.forEach((key, value) -> audit.put("input." + key, value));
            if (definition.capabilities().contains(Capability.WORLD_READ) && world == null) throw new IllegalArgumentException("Host world snapshot is unavailable");
        } catch (IllegalArgumentException | SecurityException invalid) {
            audit.put("error", String.valueOf(invalid.getMessage()));
            kernel.getJournal().append("skill_rejected", id, audit); throw invalid;
        }
        kernel.getJournal().append("skill_started", id, audit);
        try {
            Map<String, String> output = definition.output().validate(switch (id) {
                case "world.inspect" -> describe(world);
                case "memory.search" -> search(fields.get("query"));
                case "java.analyze" -> analyze(fields.get("path"));
                case "docs.generate" -> document(fields.get("title"), world, call);
                case "artifact.verify" -> verify(fields.get("path"), fields.get("expectedSha256"));
                default -> throw new IllegalStateException("No compiled handler");
            });
            audit.put("outputSha256", hash(canonical(output).getBytes(StandardCharsets.UTF_8)));
            audit.put("elapsedMillis", Long.toString((System.nanoTime() - start) / 1_000_000));
            output.forEach((key, value) -> audit.put("output." + key, value));
            var entry = kernel.getJournal().append("skill_succeeded", id, audit);
            return new Result(call, id, "succeeded", output, entry.index());
        } catch (IOException | RuntimeException error) {
            audit.put("error", String.valueOf(error.getMessage()));
            try { kernel.getJournal().append("skill_failed", id, audit); } catch (IOException journalError) { error.addSuppressed(journalError); }
            throw error;
        }
    }

    /** Full immutable authored data, not a claim of pixel perception. */
    private static Map<String, String> describe(WorldSnapshot world) {
        int components = world.entities().stream().mapToInt(e -> e.components().size()).sum();
        var rows = new ArrayList<String>();
        for (EntityRecord entity : world.entities()) {
            var parts = new ArrayList<String>();
            for (ComponentRecord component : entity.components()) parts.add("{\"id\":" + quote(component.id().toString()) + ",\"type\":" + quote(component.type())
                + ",\"version\":" + component.version() + ",\"values\":" + object(component.values()) + "}");
            rows.add("{\"id\":" + quote(entity.id().toString()) + ",\"name\":" + quote(entity.name()) + ",\"components\":[" + String.join(",", parts) + "]}");
        }
        return Map.of("revision", Long.toString(world.revision()), "entities", Integer.toString(world.entities().size()), "components", Integer.toString(components), "world", "[" + String.join(",", rows) + "]");
    }
    private Map<String, String> search(String query) {
        if (query.isBlank()) throw new IllegalArgumentException("Search query is empty");
        var hits = kernel.getMemoryStore().search(query, List.of(), 8);
        var rows = hits.stream().map(hit -> "{\"path\":" + quote(String.valueOf(hit.getDocument().getPath())) + ",\"score\":" + hit.getTotalScore()
            + ",\"preview\":" + quote(hit.getDocument().getPreview()) + "}").toList();
        return Map.of("hits", Integer.toString(hits.size()), "results", "[" + String.join(",", rows) + "]");
    }
    private Map<String, String> analyze(String path) throws IOException {
        byte[] data = readBounded(path, 2L * 1024 * 1024);
        String source = StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(data)).toString();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IOException("Java analysis requires a local JDK (jdk.compiler)");
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        var unit = new SimpleJavaFileObject(URI.create("string:///Source.java"), JavaFileObject.Kind.SOURCE) { @Override public CharSequence getCharContent(boolean ignore) { return source; } };
        var declarations = new ArrayList<String>(); int[] counts = {0, 0};
        try (var manager = compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
            var task = (JavacTask) compiler.getTask(null, manager, diagnostics, List.of("-proc:none"), null, List.of(unit));
            for (var tree : task.parse()) new TreeScanner<Void, Void>() {
                @Override public Void visitClass(ClassTree type, Void context) { counts[0]++; declarations.add(type.getKind() + " " + type.getSimpleName()); return super.visitClass(type, context); }
                @Override public Void visitMethod(MethodTree method, Void context) { counts[1]++; declarations.add("METHOD " + method.getName()); return super.visitMethod(method, context); }
            }.scan(tree, null);
        }
        String messages = diagnostics.getDiagnostics().stream().map(d -> d.getKind() + " " + d.getLineNumber() + ":" + d.getColumnNumber() + " " + d.getMessage(Locale.ROOT)).collect(java.util.stream.Collectors.joining("\n"));
        return Map.of("path", path, "sha256", hash(data), "syntaxValid", Boolean.toString(diagnostics.getDiagnostics().stream().noneMatch(d -> d.getKind() == Diagnostic.Kind.ERROR)),
            "types", Integer.toString(counts[0]), "methods", Integer.toString(counts[1]), "declarations", String.join("\n", declarations), "diagnostics", messages);
    }
    private Map<String, String> document(String title, WorldSnapshot world, UUID call) throws IOException {
        if (title.isBlank() || title.contains("\n") || title.contains("\r")) throw new IllegalArgumentException("Use a single-line report title");
        var observed = describe(world); if (observed.get("world").length() > 1_000_000) throw new IOException("World report exceeds supported size");
        Path reports = workspace.resolve("skill-reports"); Files.createDirectories(reports);
        if (Files.isSymbolicLink(reports) || !reports.toRealPath().startsWith(workspace)) throw new SecurityException("Report directory leaves workspace");
        Path destination = reports.resolve(call + ".md");
        String text = "# World report\n\nTitle: " + title.replace("`", "'") + "\n\nObserved: " + Instant.now() + "\n\nRevision: " + world.revision()
            + "\n\nEntities: " + observed.get("entities") + " | Components: " + observed.get("components") + "\n\nCaptured component records (JSON):\n\n    " + observed.get("world") + "\n";
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        try (var channel = java.nio.channels.FileChannel.open(destination, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            var bytes = java.nio.ByteBuffer.wrap(data); while (bytes.hasRemaining()) channel.write(bytes); channel.force(true);
        }
        return Map.of("path", workspace.relativize(destination).toString(), "sha256", hash(data), "revision", Long.toString(world.revision()));
    }
    private Map<String, String> verify(String path, String expected) throws IOException {
        byte[] data = readBounded(path, MAX_ARTIFACT_BYTES); String actual = hash(data);
        return Map.of("path", path, "sha256", actual, "bytes", Integer.toString(data.length), "matches", Boolean.toString(actual.equals(expected)));
    }
    private byte[] readBounded(String relative, long limit) throws IOException {
        Path requested = Path.of(relative);
        if (requested.isAbsolute()) throw new SecurityException("Use a path relative to this workspace");
        Path file = workspace.resolve(requested).normalize();
        if (!file.startsWith(workspace) || !file.toRealPath().startsWith(workspace) || !Files.isRegularFile(file)) throw new SecurityException("Artifact leaves the workspace or is not a file");
        // Reject link traversal (including Windows junctions resolved by toRealPath) before opening.
        Path cursor = workspace;
        for (Path part : workspace.relativize(file)) { cursor = cursor.resolve(part); if (Files.isSymbolicLink(cursor)) throw new SecurityException("Linked artifacts are not permitted"); }
        try (var in = Files.newInputStream(file)) {
            byte[] data = in.readNBytes(Math.toIntExact(limit + 1)); if (data.length > limit) throw new IOException("Artifact exceeds " + limit + " bytes"); return data;
        }
    }
    private static String canonical(Map<String, String> fields) {
        var out = new StringBuilder(); new TreeMap<>(fields).forEach((k, v) -> out.append(quote(k)).append(':').append(quote(v)).append('\n')); return out.toString();
    }
    private static String object(Map<String, String> values) { return "{" + new TreeMap<>(values).entrySet().stream().map(e -> quote(e.getKey()) + ":" + quote(e.getValue())).collect(java.util.stream.Collectors.joining(",")) + "}"; }
    private static String quote(String value) {
        var out = new StringBuilder("\"");
        for (char c : value.toCharArray()) switch (c) {
            case '"' -> out.append("\\\""); case '\\' -> out.append("\\\\"); case '\n' -> out.append("\\n"); case '\r' -> out.append("\\r"); case '\t' -> out.append("\\t");
            default -> { if (c < 32 || Character.isSurrogate(c)) out.append(String.format(Locale.ROOT, "\\u%04x", (int)c)); else out.append(c); }
        }
        return out.append('"').toString();
    }
    private static String hash(byte[] data) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)); } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
