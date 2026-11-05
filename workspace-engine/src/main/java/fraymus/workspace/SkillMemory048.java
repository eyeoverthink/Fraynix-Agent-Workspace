package fraymus.workspace;

import fraymus.neural.HdcSkillMemory;
import fraymus.neural.HdcSkillMemory.SkillRef;
import fraymus.neural.HdcSkillMemory.Suggestion;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Local advisory recall. Procedure authority stays with GoalRunner. */
final class SkillMemory048 {
    private final Path file;
    private final GoalRunner goals;
    private HdcSkillMemory memory = new HdcSkillMemory();
    private String fingerprint;
    private String fault = "";

    SkillMemory048(Path project, GoalRunner goals) throws IOException {
        this.goals = goals;
        file = project.resolve("brain").resolve("skill-recall-048.bin");
        fingerprint = HdcSkillMemory.fingerprint(memory.checkpoint());
        if (Files.notExists(file)) return;
        try (var input = Files.newInputStream(file)) {
            byte[] bytes = input.readNBytes(HdcSkillMemory.MAX_CHECKPOINT_BYTES + 1);
            memory = HdcSkillMemory.restore(bytes);
            fingerprint = HdcSkillMemory.fingerprint(bytes);
        } catch (IOException error) {
            fault = "Skill memory could not be restored. The saved file needs review.";
        }
    }

    synchronized JSONObject state(JSONObject input) {
        SpatialScene.keys(input);
        var response = summary();
        var choices = new JSONArray();
        catalog().keySet().forEach(ref -> choices.put(json(ref)));
        return response.put("choices", choices);
    }

    synchronized JSONObject teach(JSONObject input) throws Exception {
        SpatialScene.keys(input, "skill", "phrase");
        requireReady();
        var supplied = input.getJSONObject("skill");
        SpatialScene.keys(supplied, "name", "version", "id", "sha256");
        Object version = supplied.get("version");
        if (!(version instanceof Number n) || n.doubleValue() != n.intValue()) {
            throw new IllegalArgumentException("Choose an exact saved skill version");
        }
        var ref = new SkillRef(supplied.getString("name"), n.intValue(), supplied.getString("id"), supplied.getString("sha256"));
        JSONObject selected = catalog().get(ref);
        if (selected == null) throw new IllegalArgumentException("The saved skill changed. Refresh the selection.");
        SkillGraph046.selected(goals, selected);
        HdcSkillMemory updated = memory.teach(ref, input.getString("phrase"));
        if (updated == memory) return summary().put("changed", false);
        byte[] snapshot = updated.checkpoint();
        save(snapshot);
        memory = updated;
        fingerprint = HdcSkillMemory.fingerprint(snapshot);
        return summary().put("changed", true);
    }

    synchronized JSONObject recall(JSONObject input) {
        SpatialScene.keys(input, "phrase");
        requireReady();
        var catalog = catalog();
        var candidates = memory.recall(input.getString("phrase"), catalog.keySet());
        var verification = new HashMap<SkillRef, Boolean>();
        return summary()
            .put("exact", checked(candidates.exact(), catalog, verification))
            .put("lexical", checked(candidates.lexical(), catalog, verification))
            .put("hdc", checked(candidates.hdc(), catalog, verification));
    }

    private JSONObject summary() {
        var result = new JSONObject().put("available", fault.isEmpty()).put("advisory", true);
        return fault.isEmpty()
            ? result.put("examples", memory.exampleCount()).put("skillCount", memory.references().size()).put("sha256", fingerprint)
            : result.put("reason", fault);
    }

    private void requireReady() {
        if (!fault.isEmpty()) throw new IllegalStateException(fault);
    }

    private Map<SkillRef, JSONObject> catalog() {
        var current = new LinkedHashMap<SkillRef, JSONObject>();
        for (Object raw : goals.procedures()) {
            JSONObject p = (JSONObject)raw;
            if (!WorldPlan.PROCEDURE.equals(p.optString("kind")) ||
                !WorldNativeCheckpoint.identity().equals(p.optString("worldCodeSha256"))) continue;
            JSONObject exact = SkillGraph046.ref(p);
            var ref = new SkillRef(exact.getString("name"), exact.getInt("version"), exact.getString("id"), exact.getString("sha256"));
            current.put(ref, exact);
        }
        return current;
    }

    private JSONObject checked(Suggestion suggestion, Map<SkillRef, JSONObject> catalog, Map<SkillRef, Boolean> verified) {
        if (suggestion == null) return new JSONObject().put("status", "abstain");
        SkillRef ref = suggestion.skill();
        boolean passed = verified.computeIfAbsent(ref, key -> {
            try { SkillGraph046.selected(goals, catalog.get(key)); return true; }
            catch (Exception unavailable) { return false; }
        });
        if (!passed) return new JSONObject().put("status", "abstain").put("reason", "Original skill evidence is unavailable or changed.");
        return new JSONObject().put("status", "suggestion").put("skill", json(ref))
            .put("score", suggestion.score()).put("lead", suggestion.lead());
    }

    private static JSONObject json(SkillRef ref) {
        return new JSONObject().put("name", ref.name()).put("version", ref.version())
            .put("id", ref.id()).put("sha256", ref.sha256());
    }

    private void save(byte[] snapshot) throws IOException {
        Files.createDirectories(file.getParent());
        Path temp = Files.createTempFile(file.getParent(), "skill-recall-", ".pending");
        try {
            try (var channel = FileChannel.open(temp, StandardOpenOption.WRITE)) {
                var bytes = ByteBuffer.wrap(snapshot);
                while (bytes.hasRemaining()) channel.write(bytes);
                channel.force(true);
            }
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
