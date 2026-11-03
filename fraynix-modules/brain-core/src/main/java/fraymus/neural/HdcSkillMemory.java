package fraymus.neural;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

/** A bounded, immutable associative memory for user-taught procedure phrases. */
public final class HdcSkillMemory {
    private static final int MAGIC = 0x46534d31; // FSM1
    private static final int DIMENSIONS = 8192;
    private static final int WORDS = DIMENSIONS / Long.SIZE;
    private static final int MAX_SKILLS = 16, MAX_PER_SKILL = 12, MAX_EXAMPLES = 96;
    public static final int MAX_CHECKPOINT_BYTES = 256 * 1024;
    private static final Pattern SEPARATORS = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Set<String> COMMON_WORDS = Set.of(
        "a", "an", "the", "to", "of", "in", "on", "at", "for", "by", "and", "it", "is");
    private static final Comparator<SkillRef> ORDER = Comparator.comparing(SkillRef::name)
        .thenComparingInt(SkillRef::version).thenComparing(SkillRef::id).thenComparing(SkillRef::sha256);

    public record SkillRef(String name, int version, String id, String sha256) {
        public SkillRef {
            if (!name.matches("[A-Za-z0-9][A-Za-z0-9_-]{1,63}") || version < 1 ||
                !UUID.fromString(id).toString().equals(id) || !sha256.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("An exact saved skill reference is required");
            }
        }
    }

    public record Suggestion(SkillRef skill, double score, double lead) {}
    public record Result(Suggestion exact, Suggestion lexical, Suggestion hdc) {}
    private record Entry(List<String> examples, byte[] accumulators, long[] prototype) {
        Entry(List<String> examples, byte[] accumulators) {
            this(List.copyOf(examples), accumulators, project(accumulators));
        }
    }

    private final NavigableMap<SkillRef, Entry> entries;
    private final int exampleCount;

    public HdcSkillMemory() {
        this(new TreeMap<>(ORDER));
    }

    private HdcSkillMemory(NavigableMap<SkillRef, Entry> entries) {
        this.entries = Collections.unmodifiableNavigableMap(entries);
        this.exampleCount = entries.values().stream().mapToInt(entry -> entry.examples.size()).sum();
    }

    public Set<SkillRef> references() { return entries.navigableKeySet(); }
    public int exampleCount() { return exampleCount; }

    public HdcSkillMemory teach(SkillRef skill, String phrase) {
        String normalized = normalize(phrase);
        Entry previous = entries.get(skill);
        if (previous != null && previous.examples.contains(normalized)) return this;
        if (exampleCount >= MAX_EXAMPLES || (previous == null && entries.size() >= MAX_SKILLS) ||
            (previous != null && previous.examples.size() >= MAX_PER_SKILL)) {
            throw new IllegalStateException("Skill memory has reached its lesson limit");
        }
        var examples = new ArrayList<String>();
        byte[] votes = new byte[DIMENSIONS];
        if (previous != null) {
            examples.addAll(previous.examples);
            votes = previous.accumulators.clone();
        }
        examples.add(normalized);
        addExample(votes, normalized);
        var updated = new TreeMap<>(entries);
        updated.put(skill, new Entry(examples, votes));
        return new HdcSkillMemory(updated);
    }

    public Result recall(String query, Set<SkillRef> eligible) {
        String normalized = normalize(query);
        Set<String> words = terms(normalized);
        long[] vector = encode(normalized);
        var exact = new Ranking();
        var lexical = new Ranking();
        var hdc = new Ranking();
        for (var item : entries.entrySet()) {
            SkillRef skill = item.getKey();
            if (!eligible.contains(skill)) continue;
            Entry entry = item.getValue();
            exact.add(skill, entry.examples.contains(normalized) ? 1 : 0);
            double bestWordScore = entry.examples.stream().mapToDouble(example -> jaccard(words, terms(example))).max().orElse(0);
            lexical.add(skill, bestWordScore);
            int different = 0;
            for (int i = 0; i < WORDS; i++) different += Long.bitCount(vector[i] ^ entry.prototype[i]);
            hdc.add(skill, 1 - (double)different / DIMENSIONS);
        }
        return new Result(exact.select(1, 1), lexical.select(.28, .08), hdc.select(.56, .025));
    }

    private static final class Ranking {
        private SkillRef best;
        private double score, runnerUp;
        void add(SkillRef skill, double candidate) {
            if (candidate > score) {
                runnerUp = score;
                score = candidate;
                best = skill;
            } else if (candidate > runnerUp) {
                runnerUp = candidate;
            }
        }
        Suggestion select(double minimum, double margin) {
            double lead = score - runnerUp;
            return best != null && score >= minimum && lead >= margin ? new Suggestion(best, score, lead) : null;
        }
    }

    public byte[] checkpoint() throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(bytes)) {
            out.writeInt(MAGIC);
            out.writeInt(entries.size());
            for (var item : entries.entrySet()) {
                SkillRef ref = item.getKey();
                Entry entry = item.getValue();
                out.writeUTF(ref.name);
                out.writeInt(ref.version);
                out.writeUTF(ref.id);
                out.writeUTF(ref.sha256);
                out.writeInt(entry.examples.size());
                for (String example : entry.examples) out.writeUTF(example);
                out.write(entry.accumulators);
            }
            out.flush();
            out.write(digest(bytes.toByteArray()));
        }
        return bytes.toByteArray();
    }

    public static HdcSkillMemory restore(byte[] bytes) throws IOException {
        if (bytes.length < 40 || bytes.length > MAX_CHECKPOINT_BYTES) throw new IOException("Invalid skill memory size");
        int bodyLength = bytes.length - 32;
        byte[] body = Arrays.copyOf(bytes, bodyLength);
        if (!MessageDigest.isEqual(digest(body), Arrays.copyOfRange(bytes, bodyLength, bytes.length))) {
            throw new IOException("Skill memory checksum mismatch");
        }
        try (var in = new DataInputStream(new ByteArrayInputStream(body))) {
            if (in.readInt() != MAGIC) throw new IOException("Unsupported skill memory format");
            int count = bounded(in.readInt(), 0, MAX_SKILLS);
            int total = 0;
            var restored = new TreeMap<SkillRef, Entry>(ORDER);
            for (int i = 0; i < count; i++) {
                var ref = new SkillRef(in.readUTF(), in.readInt(), in.readUTF(), in.readUTF());
                int lessons = bounded(in.readInt(), 1, MAX_PER_SKILL);
                total = bounded(total + lessons, 0, MAX_EXAMPLES);
                var examples = new LinkedHashSet<String>();
                var expected = new byte[DIMENSIONS];
                for (int j = 0; j < lessons; j++) {
                    String phrase = in.readUTF();
                    if (!phrase.equals(normalize(phrase)) || !examples.add(phrase)) {
                        throw new IOException("Invalid or repeated skill memory phrase");
                    }
                    addExample(expected, phrase);
                }
                byte[] accumulators = in.readNBytes(DIMENSIONS);
                if (!Arrays.equals(accumulators, expected)) throw new IOException("Skill memory accumulator mismatch");
                if (restored.putIfAbsent(ref, new Entry(List.copyOf(examples), accumulators)) != null) {
                    throw new IOException("Duplicate skill memory reference");
                }
            }
            if (in.available() != 0) throw new IOException("Trailing skill memory data");
            return new HdcSkillMemory(restored);
        } catch (IllegalArgumentException invalid) {
            throw new IOException("Invalid skill memory data", invalid);
        }
    }

    private static int bounded(int n, int min, int max) throws IOException {
        if (n < min || n > max) throw new IOException("Invalid skill memory count");
        return n;
    }

    public static String fingerprint(byte[] bytes) { return HexFormat.of().formatHex(digest(bytes)); }

    private static byte[] digest(byte[] bytes) {
        try { return MessageDigest.getInstance("SHA-256").digest(bytes); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }

    private static String normalize(String phrase) {
        if (phrase.length() > 240) throw new IllegalArgumentException("Use a phrase of 1 to 240 characters");
        String normalized = SEPARATORS.matcher(Normalizer.normalize(phrase, Normalizer.Form.NFKC)
            .toLowerCase(Locale.ROOT)).replaceAll(" ").strip();
        if (normalized.isEmpty() || normalized.length() > 240 || terms(normalized).isEmpty()) {
            throw new IllegalArgumentException("Enter a short phrase with at least one meaningful word");
        }
        return normalized;
    }

    private static Set<String> terms(String text) {
        var words = new HashSet<>(Arrays.asList(text.split(" ")));
        words.removeAll(COMMON_WORDS);
        return words;
    }

    private static double jaccard(Set<String> left, Set<String> right) {
        int shared = 0;
        for (String word : left) if (right.contains(word)) shared++;
        return (double)shared / (left.size() + right.size() - shared);
    }

    private static long[] encode(String phrase) {
        var features = new HashSet<String>();
        for (String token : terms(phrase)) {
            features.add("word:" + token);
            int[] padded = ("^" + token + "$").codePoints().toArray();
            for (int i = 0; i + 3 <= padded.length; i++) features.add("tri:" + new String(padded, i, 3));
        }
        var votes = new short[DIMENSIONS];
        for (String feature : features) {
            long seed = 0xcbf29ce484222325L;
            for (byte value : feature.getBytes(StandardCharsets.UTF_8)) {
                seed = (seed ^ Byte.toUnsignedLong(value)) * 0x100000001b3L;
            }
            for (int word = 0; word < WORDS; word++) {
                seed += 0x9e3779b97f4a7c15L;
                long bits = mix(seed);
                for (int bit = 0; bit < Long.SIZE; bit++, bits >>>= 1) {
                    votes[word * Long.SIZE + bit] += (bits & 1L) == 0 ? -1 : 1;
                }
            }
        }
        var vector = new long[WORDS];
        for (int i = 0; i < votes.length; i++) if (votes[i] > 0) vector[i >>> 6] |= 1L << (i & 63);
        return vector;
    }

    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }

    private static void addExample(byte[] votes, String phrase) {
        long[] vector = encode(phrase);
        for (int i = 0; i < votes.length; i++) {
            votes[i] += (vector[i >>> 6] & (1L << (i & 63))) == 0 ? -1 : 1;
        }
    }

    private static long[] project(byte[] votes) {
        var vector = new long[WORDS];
        for (int i = 0; i < votes.length; i++) if (votes[i] > 0) vector[i >>> 6] |= 1L << (i & 63);
        return vector;
    }
}
