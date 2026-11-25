package fraymus.workspace;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Instant;

/** Durable evidence for one Chat request. The scene store remains the approval and commit authority. */
final class RecipientMissionStore {
    private final Path directory;
    private final Path exports;

    RecipientMissionStore(Path project) {
        directory = project.resolve("spatial/recipient-missions");
        exports = project.resolve("exports");
    }

    void start(String id, JSONObject parent, JSONObject review, String request) throws Exception {
        if (read(id) == null) {
            Files.createDirectories(directory);
            save(id,new JSONObject().put("reviewId",id).put("parentSceneId",parent.getString("id"))
                    .put("parent",SpatialPropCopy.binding(parent)).put("review",review)
                    .put("request",request).put("requestedAt",Instant.now().toString()));
        }
        SpriteWorkshop.replace(directory.resolve("latest"),id.getBytes(StandardCharsets.US_ASCII));
    }

    void compared(String id, JSONObject comparison) throws Exception {
        var mission = read(id);
        if (mission != null && !mission.has("approval")) save(id,mission.put("comparison",comparison));
    }

    void approve(String id, JSONObject comparison) throws Exception {
        var mission = read(id);
        if (mission == null) return;
        var prior = mission.optJSONObject("comparison");
        if (prior == null || !prior.similar(comparison))
            throw new IllegalStateException("The mission comparison changed. Compare the exact proposal first.");
        if (!mission.has("approval")) save(id,mission.put("approval",new JSONObject().put("reviewId",id)
                .put("comparisonSha256",comparison.getString("sha256")).put("requestedAt",Instant.now().toString())));
    }

    JSONObject latest() throws Exception {
        Path path = directory.resolve("latest");
        if (!Files.exists(path,LinkOption.NOFOLLOW_LINKS)) return null;
        var mission = read(new String(SpriteWorkshop.read(path,64),StandardCharsets.US_ASCII));
        if (mission == null) throw new IllegalStateException("The latest collider mission is unavailable.");
        return mission;
    }

    JSONObject read(String id) throws Exception {
        Path path = file(id);
        if (!Files.exists(path,LinkOption.NOFOLLOW_LINKS)) return null;
        var sealed = new JSONObject(new String(SpriteWorkshop.read(path,PlacementExchange.BYTE_LIMIT),StandardCharsets.UTF_8));
        String hash = sealed.getString("sha256");
        var mission = sealed.getJSONObject("mission");
        if (!id.equals(mission.getString("reviewId")) || !WorldFrames.hash(mission).equals(hash))
            throw new IllegalStateException("The saved collider mission changed.");
        return mission;
    }

    private void save(String id, JSONObject mission) throws Exception {
        var sealed = new JSONObject().put("mission",mission).put("sha256",WorldFrames.hash(mission));
        SpriteWorkshop.replace(file(id),sealed.toString().getBytes(StandardCharsets.UTF_8));
    }

    JSONObject export(String id, JSONObject output) throws Exception {
        var mission = read(id);
        String name = exportName(id);
        var claim = new JSONObject().put("fileName",name).put("sha256",output.getString("sha256"))
                .put("sceneId",output.getString("sceneId"));
        var prior = mission.optJSONObject("export");
        if (prior != null && !prior.similar(claim))
            throw new IllegalStateException("The verified export differs from the retained mission.");
        var bytes = SpriteWorkshop.decode(output.getString("base64"),PlacementExchange.BYTE_LIMIT);
        if (!MapDefinition.sha(bytes).equals(claim.getString("sha256")))
            throw new IllegalStateException("The verified export bytes changed.");
        if (prior == null) save(id,mission.put("export",claim));
        Files.createDirectories(exports);
        Path target = exports.resolve(name);
        if (!available(id,claim)) SpriteWorkshop.replace(target,bytes);
        return WorldFrames.copy(claim).put("path",target.toAbsolutePath().toString());
    }

    boolean available(String id, JSONObject claim) {
        try {
            String name = exportName(id);
            if (!name.equals(claim.getString("fileName"))) return false;
            SpriteWorkshop.read(exports.resolve(name),PlacementExchange.BYTE_LIMIT,claim.getString("sha256"));
            return true;
        } catch (java.io.IOException | RuntimeException changed) {
            return false;
        }
    }

    private Path file(String id) {
        if (!id.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Invalid collider review ID.");
        return directory.resolve(id+".json");
    }

    private String exportName(String id) {
        file(id);
        return "collider-"+id+".fraynix-recipient.json";
    }
}
