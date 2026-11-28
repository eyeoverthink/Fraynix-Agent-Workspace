package fraymus.workspace;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;

/** Durable request, exact approval and reproducible native witness for a scoped scene revision. */
final class LoadingAisleMission {
    private final Path directory, nativeDir;

    LoadingAisleMission(Path project) {
        directory = project.resolve("spatial/loading-aisle");
        nativeDir = project.resolve("native");
    }

    JSONObject review(SpatialStore scenes, JSONObject input) throws Exception {
        SpatialScene.keys(input,"sceneId","objectId","request");
        var parent = scenes.read(input.getString("sceneId"));
        var plan = LoadingAisle.plan(parent, input.getString("objectId"), SpatialScene.text(input,"request",520));
        String id = WorldFrames.hash(plan);
        Path proposal = file(id,"review");
        if (!Files.exists(proposal, LinkOption.NOFOLLOW_LINKS)) AgentFiles042.saveChecked(proposal,plan);
        SpriteWorkshop.replace(directory.resolve("latest"), id.getBytes(StandardCharsets.US_ASCII));
        return read(scenes,id);
    }

    JSONObject latest(SpatialStore scenes) throws Exception {
        Path pointer = directory.resolve("latest");
        if (!Files.exists(pointer, LinkOption.NOFOLLOW_LINKS)) return new JSONObject();
        return read(scenes,new String(SpriteWorkshop.read(pointer,64),StandardCharsets.US_ASCII));
    }

    private JSONObject checkedReview(SpatialStore scenes, String id) throws Exception {
        var review = AgentFiles042.loadChecked(file(id,"review"));
        if (!id.equals(WorldFrames.hash(review))) throw new IllegalStateException("The reviewed proposal digest changed.");
        var parent = scenes.read(review.getString("parentId"));
        if (!review.similar(LoadingAisle.plan(parent,review.getString("objectId"),review.getString("request"))))
            throw new IllegalStateException("The approved scene or its proposed change differs from the retained parent.");
        return review;
    }

    private JSONObject approval(String id) throws Exception {
        Path path = file(id,"approval");
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return null;
        var approval = AgentFiles042.loadChecked(path);
        if (!approval.similar(new JSONObject().put("reviewSha256",id)))
            throw new IllegalStateException("The saved approval differs from this proposal.");
        return approval;
    }

    JSONObject read(SpatialStore scenes, String id) throws Exception {
        var review = checkedReview(scenes,id);
        var result = new JSONObject().put("id",id).put("review",review)
                .put("status", approval(id) == null ? "review" : "approved");
        Path proofFile = file(id,"proof");
        if (!Files.exists(proofFile, LinkOption.NOFOLLOW_LINKS)) return result;
        if ("review".equals(result.getString("status"))) throw new IllegalStateException("The exact approval is missing.");
        var proof = AgentFiles042.loadChecked(proofFile);
        if (!id.equals(proof.getString("reviewSha256")) || !sceneId(id).equals(proof.getString("sceneId")))
            throw new IllegalStateException("The measurement belongs to another revision.");
        var resultScene = scenes.read(sceneId(id));
        verifyLink(resultScene, proof, id);
        LoadingAisle.checkScope(scenes.read(review.getString("parentId")),resultScene,review);
        return result.put("status","verified").put("proof",proof).put("sceneId",sceneId(id));
    }

    private static void verifyLink(JSONObject scene, JSONObject proof, String id) {
        var expected = new JSONObject().put("reviewSha256",id).put("measurementSha256",WorldFrames.hash(proof));
        if (!expected.similar(scene.optJSONObject("loadingAisle")) ||
                !scene.getString("sourceSha256").equals(proof.getJSONObject("measurements").getString("candidateSourceSha256")))
            throw new IllegalStateException("The committed scene differs from the saved verification.");
    }

    JSONObject approve(SpatialStore scenes, JSONObject input) throws Exception {
        SpatialScene.keys(input,"id","approvalSha256");
        String id = input.getString("id");
        if (!id.equals(input.getString("approvalSha256")))
            throw new IllegalArgumentException("Approve the digest of the exact displayed translation.");
        var current = read(scenes,id);
        if ("verified".equals(current.getString("status"))) return current;
        if (approval(id) == null) AgentFiles042.saveChecked(file(id,"approval"),new JSONObject().put("reviewSha256",id));

        var review = current.getJSONObject("review");
        var parent = scenes.read(review.getString("parentId"));
        String definition = MapCheckpoint.canonical(LoadingAisle.revision(parent,review));
        String childId = sceneId(id);
        var child = SpatialScene.parse(definition,childId);
        var measured = LoadingAisle.measure(parent,child,review,nativeDir);
        if (!measured.similar(LoadingAisle.measure(parent,child,review,nativeDir)))
            throw new IllegalStateException("Independent native aisle checks disagreed.");
        var proof = new JSONObject().put("reviewSha256",id).put("sceneId",childId)
                .put("nativeRepeats",2).put("measurements",measured);
        var link = new JSONObject().put("reviewSha256",id).put("measurementSha256",WorldFrames.hash(proof));
        scenes.retainAisleEdit(child,link);
        AgentFiles042.saveChecked(file(id,"proof"),proof);
        return read(scenes,id);
    }

    JSONObject replay(SpatialStore scenes, String id) throws Exception {
        var saved = read(scenes,id);
        if (!"verified".equals(saved.getString("status")))
            throw new IllegalStateException("Approve and verify the reviewed translation first.");
        var review = saved.getJSONObject("review");
        var parent = scenes.read(review.getString("parentId"));
        var child = scenes.read(saved.getString("sceneId"));
        var actual = LoadingAisle.measure(parent,child,review,nativeDir);
        if (!actual.similar(saved.getJSONObject("proof").getJSONObject("measurements")))
            throw new IllegalStateException("The fresh native aisle witness differs from the saved measurement.");
        return saved.put("replayed",true);
    }

    JSONObject export(SpatialStore scenes,String id) throws Exception {
        var saved = read(scenes,id);
        if (!"verified".equals(saved.getString("status"))) throw new IllegalStateException("Verify the translation before export.");
        var child = scenes.read(saved.getString("sceneId"));
        var envelope = new JSONObject().put("format","fraynix.loading-aisle-evidence-v1")
                .put("mission",saved).put("scene",new JSONObject(child.getString("sourceText")));
        byte[] bytes = MapCheckpoint.canonical(envelope).getBytes(StandardCharsets.UTF_8);
        return new JSONObject().put("base64",Base64.getEncoder().encodeToString(bytes))
                .put("sha256",MapDefinition.sha(bytes)).put("sceneId",child.getString("id"));
    }

    private Path file(String id,String stage) {
        if (!id.matches("[a-f0-9]{64}")) throw new IllegalArgumentException("Choose an exact aisle proposal.");
        return directory.resolve(id + "." + stage + ".json");
    }

    private static String sceneId(String id) {
        return UUID.nameUUIDFromBytes(("loading-aisle:" + id).getBytes(StandardCharsets.US_ASCII)).toString();
    }
}
