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

    synchronized JSONObject review(SpatialStore scenes, JSONObject input) throws Exception {
        SpatialScene.keys(input,"sceneId","objectId","request");
        var parent = scenes.read(input.getString("sceneId"));
        return saveReview(scenes,LoadingAisle.plan(parent,input.getString("objectId"),SpatialScene.text(input,"request",520)));
    }

    synchronized JSONObject intent(SpatialStore scenes, JSONObject input) throws Exception {
        SpatialScene.keys(input,"sceneId","objectId","request");
        var parent = scenes.read(input.getString("sceneId"));
        var request = LoadingAisleIntent.parse(SpatialScene.text(input,"request",520));
        String selected = input.optString("objectId","");
        String resolved = LoadingAisleIntent.resolved(parent,request,selected);
        if (resolved.isEmpty())
            return saveState(scenes,LoadingAisleIntent.question(parent,request,selected));
        return search(scenes,parent,request,resolved,new JSONObject().put("kind","request").put("priorSelection",selected));
    }

    synchronized JSONObject choose(SpatialStore scenes, JSONObject input) throws Exception {
        SpatialScene.keys(input,"id","sceneId","objectId","answer");
        String id = input.getString("id");
        if (!id.equals(latestId())) throw new IllegalStateException("This crate question has been superseded.");
        var question = checkedState(scenes,id);
        if (!question.getString("status").equals("clarify") || !question.getString("parentId").equals(input.getString("sceneId")))
            throw new IllegalArgumentException("Return to the bay named in this crate question.");
        if (input.has("objectId") == input.has("answer"))
            throw new IllegalArgumentException("Give either one crate option or one reply.");
        String answer = input.has("answer") ? SpatialScene.text(input,"answer",120) : "";
        String objectId = LoadingAisleIntent.choice(question,input.optString("objectId",""),answer);
        if (objectId.isEmpty()) return question.put("id",id);
        var parent = scenes.read(question.getString("parentId"));
        return search(scenes,parent,LoadingAisleIntent.parse(question.getString("request")),objectId,
                new JSONObject().put("kind","answer").put("questionId",id));
    }

    private JSONObject search(SpatialStore scenes, JSONObject parent, LoadingAisleIntent.Request request,
                             String objectId, JSONObject intent) throws Exception {
        var result = LoadingAisleIntent.search(parent,objectId,request,intent,nativeDir);
        return result.plan() == null ? saveState(scenes,result.refusal()) : saveReview(scenes,result.plan());
    }

    private JSONObject saveState(SpatialStore scenes, JSONObject state) throws Exception {
        String id = WorldFrames.hash(state);
        save(id,"intent",state);
        return read(scenes,id);
    }

    private JSONObject saveReview(SpatialStore scenes, JSONObject review) throws Exception {
        String id = WorldFrames.hash(review);
        save(id,"review",review);
        return read(scenes,id);
    }

    private void save(String id, String stage, JSONObject data) throws Exception {
        Path path = file(id,stage);
        if (!Files.exists(path,LinkOption.NOFOLLOW_LINKS)) AgentFiles042.saveChecked(path,data);
        SpriteWorkshop.replace(directory.resolve("latest"),id.getBytes(StandardCharsets.US_ASCII));
    }

    private String latestId() throws Exception {
        Path pointer = directory.resolve("latest");
        return Files.exists(pointer,LinkOption.NOFOLLOW_LINKS) ?
                new String(SpriteWorkshop.read(pointer,64),StandardCharsets.US_ASCII) : "";
    }

    synchronized JSONObject latest(SpatialStore scenes) throws Exception {
        String id = latestId();
        return id.isEmpty() ? new JSONObject() : read(scenes,id);
    }

    private JSONObject checkedState(SpatialStore scenes, String id) throws Exception {
        var state = AgentFiles042.loadChecked(file(id,"intent"));
        if (!id.equals(WorldFrames.hash(state))) throw new IllegalStateException("The saved loading bay request changed.");
        var parent = scenes.read(state.getString("parentId"));
        if (!parent.getString("sourceSha256").equals(state.getString("parentSha256")))
            throw new IllegalStateException("The loading bay source changed.");
        var request = LoadingAisleIntent.parse(state.getString("request"));
        if (state.getString("status").equals("clarify")) {
            if (!state.similar(LoadingAisleIntent.question(parent,request,state.getString("priorSelection"))))
                throw new IllegalStateException("The crate choices differ from the saved question.");
        } else {
            if (!state.getString("status").equals("blocked"))
                throw new IllegalStateException("Unknown loading bay request status.");
            var intent = state.getJSONObject("intent");
            checkIntent(parent,request,state.getString("objectId"),intent,savedQuestion(intent));
        }
        return state;
    }

    private static void checkIntent(JSONObject parent, LoadingAisleIntent.Request request,
                                    String objectId, JSONObject intent, JSONObject portableQuestion) {
        String resolved = switch (intent.getString("kind")) {
            case "request" -> {
                SpatialScene.keys(intent,"kind","priorSelection");
                yield LoadingAisleIntent.resolved(parent,request,intent.getString("priorSelection"));
            }
            case "answer" -> {
                SpatialScene.keys(intent,"kind","questionId");
                if (portableQuestion==null) throw new IllegalStateException("The original crate question is missing.");
                var question = portableQuestion;
                var expected = LoadingAisleIntent.question(parent,request,question.getString("priorSelection"));
                if (!question.similar(expected) || !intent.getString("questionId").equals(WorldFrames.hash(question)))
                    throw new IllegalStateException("The approved crate differs from the original question.");
                yield LoadingAisleIntent.choice(question,objectId,"");
            }
            default -> throw new IllegalArgumentException("Unknown loading bay resolution.");
        };
        if (!objectId.equals(resolved)) throw new IllegalStateException("The crate resolution changed.");
    }

    private JSONObject checkedReview(SpatialStore scenes, String id) throws Exception {
        var review = AgentFiles042.loadChecked(file(id,"review"));
        if (!id.equals(WorldFrames.hash(review))) throw new IllegalStateException("The reviewed proposal digest changed.");
        var parent = scenes.read(review.getString("parentId"));
        verifyReview(parent,review,savedQuestion(review.optJSONObject("intent")));
        return review;
    }

    private static void verifyReview(JSONObject parent,JSONObject review,JSONObject question) {
        JSONObject expected;
        if (review.has("intent")) {
            var intent = review.getJSONObject("intent");
            checkIntent(parent,LoadingAisleIntent.parse(review.getString("request")),review.getString("objectId"),intent,question);
            expected = LoadingAisleIntent.rebuild(parent,review,intent);
        } else {
            expected = LoadingAisle.plan(parent,review.getString("objectId"),review.getString("request"));
        }
        if (!review.similar(expected))
            throw new IllegalStateException("The approved scene or its proposed change differs from the retained parent.");
    }

    private JSONObject savedQuestion(JSONObject intent) throws Exception {
        return intent!=null && "answer".equals(intent.optString("kind"))
                ? AgentFiles042.loadChecked(file(intent.getString("questionId"),"intent")) : null;
    }

    synchronized boolean hasReview(String id) {
        return Files.exists(file(id,"review"),LinkOption.NOFOLLOW_LINKS);
    }

    record ShotSource(JSONObject parent,JSONObject mission) {}

    synchronized JSONObject packShotSource(SpatialStore scenes,String id) throws Exception {
        var mission = read(scenes,id);
        if (!"verified".equals(mission.getString("status")))
            throw new IllegalArgumentException("The exact route needs an approval and independent measurement.");
        var review = mission.getJSONObject("review");
        var parent = scenes.read(review.getString("parentId"));
        var result = new JSONObject().put("sourceText",parent.getString("sourceText")).put("mission",mission)
                .put("approval",approval(id));
        var question = savedQuestion(review.optJSONObject("intent"));
        if (question!=null) result.put("question",question);
        return result;
    }

    ShotSource checkShotSource(JSONObject envelope,boolean replay) throws Exception {
        SpatialScene.keys(envelope,"sourceText","mission","approval","question");
        var mission = envelope.getJSONObject("mission");
        SpatialScene.keys(mission,"id","review","status","proof","sceneId");
        String id = mission.getString("id");
        file(id,"review");
        var review = mission.getJSONObject("review");
        String childId = sceneId(id);
        if (!"verified".equals(mission.getString("status")) || !id.equals(WorldFrames.hash(review)) ||
                !childId.equals(mission.getString("sceneId")) ||
                !new JSONObject().put("reviewSha256",id).similar(envelope.getJSONObject("approval")))
            throw new IllegalStateException("The exported route or retained acceptance digest changed.");
        var parent = SpatialScene.parse(envelope.getString("sourceText"),review.getString("parentId"));
        if (!parent.getString("sourceSha256").equals(review.getString("parentSha256")))
            throw new IllegalStateException("The exported source scene differs from its review.");
        var question = envelope.optJSONObject("question");
        boolean questionExpected = review.has("intent") && "answer".equals(review.getJSONObject("intent").optString("kind"));
        if (envelope.has("question")!=questionExpected || questionExpected && question==null)
            throw new IllegalStateException("The exported crate selection record changed.");
        verifyReview(parent,review,question);
        var proof = mission.getJSONObject("proof");
        SpatialScene.keys(proof,"reviewSha256","sceneId","nativeRepeats","measurements");
        var child = SpatialScene.parse(MapCheckpoint.canonical(LoadingAisle.revision(parent,review)),childId);
        var measurements = proof.getJSONObject("measurements");
        if (!id.equals(proof.getString("reviewSha256")) || !childId.equals(proof.getString("sceneId")) ||
                SpatialScene.number(proof,"nativeRepeats",2,2)!=2 ||
                !child.getString("sourceSha256").equals(measurements.getString("candidateSourceSha256")))
            throw new IllegalStateException("The exported route measurement differs from its verified revision.");
        LoadingAisle.checkScope(parent,child,review);
        if (replay) {
            var measured = LoadingAisle.measure(parent,child,review,nativeDir);
            LoadingAisleIntent.checkPreview(parent,review,measured,nativeDir);
            if (!measured.similar(measurements))
                throw new IllegalStateException("The exported route disagrees with a fresh native measurement.");
        }
        return new ShotSource(parent,mission);
    }

    private JSONObject approval(String id) throws Exception {
        Path path = file(id,"approval");
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return null;
        var approval = AgentFiles042.loadChecked(path);
        if (!approval.similar(new JSONObject().put("reviewSha256",id)))
            throw new IllegalStateException("The saved approval differs from this proposal.");
        return approval;
    }

    synchronized JSONObject read(SpatialStore scenes, String id) throws Exception {
        if (Files.exists(file(id,"intent"),LinkOption.NOFOLLOW_LINKS)) return checkedState(scenes,id).put("id",id);
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

    synchronized JSONObject approve(SpatialStore scenes, JSONObject input) throws Exception {
        SpatialScene.keys(input,"id","approvalSha256");
        String id = input.getString("id");
        if (!id.equals(input.getString("approvalSha256")))
            throw new IllegalArgumentException("Approve the digest of the exact displayed translation.");
        var current = read(scenes,id);
        if ("verified".equals(current.getString("status"))) return current;
        if (!current.has("review")) throw new IllegalStateException("Choose a crate and review a verified path before approval.");
        if (approval(id) == null) AgentFiles042.saveChecked(file(id,"approval"),new JSONObject().put("reviewSha256",id));

        var review = current.getJSONObject("review");
        var parent = scenes.read(review.getString("parentId"));
        String definition = MapCheckpoint.canonical(LoadingAisle.revision(parent,review));
        String childId = sceneId(id);
        var child = SpatialScene.parse(definition,childId);
        var measured = LoadingAisle.measure(parent,child,review,nativeDir);
        LoadingAisleIntent.checkPreview(parent,review,measured,nativeDir);
        if (!measured.similar(LoadingAisle.measure(parent,child,review,nativeDir)))
            throw new IllegalStateException("Independent native aisle checks disagreed.");
        var proof = new JSONObject().put("reviewSha256",id).put("sceneId",childId)
                .put("nativeRepeats",2).put("measurements",measured);
        var link = new JSONObject().put("reviewSha256",id).put("measurementSha256",WorldFrames.hash(proof));
        scenes.retainAisleEdit(child,link);
        AgentFiles042.saveChecked(file(id,"proof"),proof);
        return read(scenes,id);
    }

    synchronized JSONObject replay(SpatialStore scenes, String id) throws Exception {
        var saved = read(scenes,id);
        if (!"verified".equals(saved.getString("status")))
            throw new IllegalStateException("Approve and verify the reviewed translation first.");
        var review = saved.getJSONObject("review");
        var parent = scenes.read(review.getString("parentId"));
        var child = scenes.read(saved.getString("sceneId"));
        var actual = LoadingAisle.measure(parent,child,review,nativeDir);
        LoadingAisleIntent.checkPreview(parent,review,actual,nativeDir);
        if (!actual.similar(saved.getJSONObject("proof").getJSONObject("measurements")))
            throw new IllegalStateException("The fresh native aisle witness differs from the saved measurement.");
        return saved.put("replayed",true);
    }

    synchronized JSONObject export(SpatialStore scenes,String id) throws Exception {
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
