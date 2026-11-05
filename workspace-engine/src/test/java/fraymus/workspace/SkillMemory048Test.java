package fraymus.workspace;

import fraymus.neural.HdcSkillMemory;
import fraymus.neural.HdcSkillMemory.SkillRef;
import fraymus.neural.HdcSkillMemory.Suggestion;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Function;

import static org.junit.Assert.*;

public class SkillMemory048Test {
    private static SkillRef skill(String name) {
        return new SkillRef(name, 1, UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)).toString(), "a".repeat(64));
    }

    @Test public void exactCheckpointAndImmutableRecall() throws Exception {
        var one = skill("first-skill");
        var two = skill("second-skill");
        var empty = new HdcSkillMemory();
        var memory = empty.teach(one, "Turn the blue crate").teach(one, "Turn the crate toward the door")
            .teach(two, "Move the reference marker");
        assertEquals(0, empty.exampleCount());
        assertSame(memory, memory.teach(one, "TURN the blue crate!"));
        byte[] checkpoint = memory.checkpoint();
        var restored = HdcSkillMemory.restore(checkpoint);
        assertArrayEquals(checkpoint, restored.checkpoint());
        assertEquals(memory.recall("turn the blue crat", memory.references()),
            restored.recall("turn the blue crat", restored.references()));
        assertEquals(one, restored.recall("turn the blue crate", restored.references()).exact().skill());
        assertNull(restored.recall("turn the blue crate", Set.of(two)).exact());
        var conflicting = restored.teach(two, "turn the blue crate");
        assertNull(conflicting.recall("turn the blue crate", conflicting.references()).exact());
        assertArrayEquals(checkpoint, restored.checkpoint());

        var altered = checkpoint.clone();
        int votes = altered.length - 33;
        altered[votes] ^= 1;
        assertThrows(IOException.class, () -> HdcSkillMemory.restore(altered));
        byte[] newDigest = MessageDigest.getInstance("SHA-256").digest(Arrays.copyOf(altered, altered.length - 32));
        System.arraycopy(newDigest, 0, altered, altered.length - 32, 32);
        assertThrows(IOException.class, () -> HdcSkillMemory.restore(altered));
        assertThrows(IOException.class, () -> HdcSkillMemory.restore(Arrays.copyOf(checkpoint, checkpoint.length - 5)));
        assertThrows(IllegalArgumentException.class, () -> restored.recall("x".repeat(241), restored.references()));
    }

    @Test public void compareFrozenSuite() throws Exception {
        var lines = new ArrayList<String[]>();
        try (var stream = Objects.requireNonNull(getClass().getResourceAsStream("/skill-recall-048.tsv"));
             var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            for (String line : reader.lines().toList()) lines.add(line.split("\\t", 3));
        }
        var memory = new HdcSkillMemory();
        var training = new HashSet<String>();
        for (String[] row : lines) if (row[0].equals("train")) {
            assertTrue(training.add(row[2]));
            memory = memory.teach(skill(row[1]), row[2]);
        }
        assertEquals(18, memory.exampleCount());
        byte[] before = memory.checkpoint();
        var rows = new JSONArray();
        var methods = Map.<String, Function<HdcSkillMemory.Result, Suggestion>>of(
            "exact", HdcSkillMemory.Result::exact, "lexical", HdcSkillMemory.Result::lexical, "hdc", HdcSkillMemory.Result::hdc);
        var counts = new TreeMap<String, int[]>();
        methods.keySet().forEach(key -> counts.put(key, new int[5]));
        int holdouts = 0, controls = 0;
        for (String[] row : lines) {
            if (row[0].equals("train")) continue;
            var recall = memory.recall(row[2], memory.references());
            if (row[0].equals("control")) {
                assertEquals(row[1], recall.exact().skill().name());
                controls++;
                continue;
            }
            assertEquals("holdout", row[0]);
            assertFalse(training.contains(row[2]));
            holdouts++;
            var item = new JSONObject().put("phrase", row[2]).put("expected", row[1]);
            for (var method : methods.entrySet()) {
                var suggestion = method.getValue().apply(recall);
                String answer = suggestion == null ? "-" : suggestion.skill().name();
                item.put(method.getKey(), suggestion == null ? JSONObject.NULL : new JSONObject()
                    .put("skill", answer).put("score", suggestion.score()).put("lead", suggestion.lead()));
                int[] count = counts.get(method.getKey());
                if (row[1].equals("-")) count[suggestion == null ? 3 : 4]++;
                else if (suggestion == null) count[1]++;
                else count[answer.equals(row[1]) ? 0 : 2]++;
            }
            rows.put(item);
        }
        assertEquals(6, controls);
        assertEquals(30, holdouts);
        assertArrayEquals(before, memory.checkpoint());
        var totals = new JSONObject();
        counts.forEach((name, count) -> totals.put(name, new JSONObject().put("correct", count[0]).put("missed", count[1])
            .put("wrong", count[2]).put("unsupportedAbstentions", count[3]).put("unsupportedSuggestions", count[4])));
        var lexical = counts.get("lexical");
        var hdc = counts.get("hdc");
        boolean gate = hdc[0] >= lexical[0] + 2 && hdc[2] <= lexical[2] && hdc[4] <= lexical[4];
        var receipt = new JSONObject().put("trainingPhrases", 18).put("exactControlsPassed", controls)
            .put("holdouts", holdouts).put("totals", totals).put("candidateGatePassed", gate)
            .put("routing", "advisory").put("cases", rows);
        var output = Path.of("build", "reports", "skill-recall-048.json");
        Files.createDirectories(output.getParent());
        Files.writeString(output, receipt.toString(2));
        System.out.println(totals.toString() + " candidateGatePassed=" + gate);
    }
}
