package fraymus.workspace;

import org.json.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** Exact checked chat snapshots; retained records are history, never action authority. */
final class ChatArchive112 {
    static final String KEY="archives112",SCHEMA="chat-task-archive-112-v1";
    static final int MAX_RECORDS=512,MAX_BYTES=128*1024*1024,MAX_SNAPSHOT=4_000_000;
    interface WorkflowEvidence { JSONObject check(JSONObject task)throws Exception; }
    private final Path project,directory;
    private final WorkspaceStore store;
    private final LearningStore learning;
    private WorkflowEvidence workflows;
    ChatArchive112(Path project,WorkspaceStore store,LearningStore learning){this.project=project;this.directory=project.resolve("agency-043/archive112");this.store=store;this.learning=learning;}
    void workflows(WorkflowEvidence value){workflows=value;}
    static JSONArray refs(JSONObject data){return data.optJSONArray(KEY,new JSONArray());}
    static boolean eligible(JSONObject g){
        if(!g.optString("status").equals("completed")||g.has("claim")||g.has("nativeIntent")||!g.has("finishedAt")||!g.has("episodeId"))return false;
        String family=g.optString("family044","workbench");
        var w=g.optJSONObject("completion044",new JSONObject()).optJSONObject("javaWorkflow",new JSONObject());
        return family.equals("workbench")||family.equals("workflow")&&(DataSummary104.supported(w.optString("profile101"))||w.has("qualification105"));
    }
    private Path directory(){if(Files.isSymbolicLink(directory.getParent())||Files.isSymbolicLink(directory))throw new IllegalStateException("Chat archive paths must not be linked");return directory;}
    private static void closed(JSONObject g){
        if(!eligible(g))throw new IllegalStateException("Only supported completed chat tasks can be archived");
        Agency043.verifyHistory(g);var history=g.getJSONArray("history");
        if(history.isEmpty()||!history.getJSONObject(history.length()-1).getString("kind").equals("completed")||!Set.of("verified","completed-unverified").contains(g.getString("outcome"))||history.getJSONObject(history.length()-1).getJSONObject("detail").getBoolean("verified")!=g.getString("outcome").equals("verified"))throw new IllegalStateException("Chat completion and original journal differ");
        if(Instant.parse(g.getString("finishedAt")).isBefore(Instant.parse(g.getString("createdAt"))))throw new IllegalStateException("Chat completion clock differs");
    }
    private record Snapshot(int bytes,JSONObject data){}
    JSONObject resolve(JSONObject ref){return resolve(ref,new HashMap<>());}
    JSONArray resolveAll(JSONArray refs){var snapshots=new HashMap<String,Snapshot>();var out=new JSONArray();for(Object raw:refs)out.put(resolve((JSONObject)raw,snapshots));return out;}
    private JSONObject resolve(JSONObject ref,Map<String,Snapshot> snapshots){
        try{
            SpatialScene.keys(ref,"schema","id","createdAt","outcome","snapshotSha256","snapshotBytes","index","taskSha256","evidenceSha256","archivedAt");
            UUID.fromString(ref.getString("id"));String hash=ref.getString("snapshotSha256");long bytes=ref.getLong("snapshotBytes");
            if(!SCHEMA.equals(ref.getString("schema"))||!hash.matches("[a-f0-9]{64}")||bytes<2||bytes>MAX_SNAPSHOT)throw new IllegalStateException("Invalid chat archive reference");
            // Per-call sharing only: each unique file is reread and hashed for every operation.
            // No file timestamps or long-lived cached receipts authorize a later operation.
            var snapshot=snapshots.get(hash);if(snapshot==null){Path file=directory().resolve(hash+".json");if(!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)||Files.size(file)!=bytes)throw new IllegalStateException("Missing or changed chat archive snapshot");
                byte[] raw=Files.readAllBytes(file);if(!PerceptionStore.hash(raw).equals(hash))throw new IllegalStateException("Chat archive checksum differs");
                var envelope=new JSONObject(new String(raw,java.nio.charset.StandardCharsets.UTF_8));var data=envelope.getJSONObject("data");if(!envelope.getString("format").equals("java-agent-042-v1")||!WorldFrames.hash(data).equals(envelope.getString("sha256")))throw new IllegalStateException("Chat archive envelope differs");snapshot=new Snapshot(raw.length,data);snapshots.put(hash,snapshot);
            }if(snapshot.bytes()!=bytes)throw new IllegalStateException("Chat archive reference size differs");
            var task=snapshot.data().getJSONArray("tasks").getJSONObject(ref.getInt("index"));closed(task);
            if(!task.getString("id").equals(ref.getString("id"))||!task.getString("createdAt").equals(ref.getString("createdAt"))||!task.getString("outcome").equals(ref.getString("outcome"))||!WorldFrames.hash(task).equals(ref.getString("taskSha256")))throw new IllegalStateException("Archived chat identity/outcome differs");
            return task;
        }catch(Exception e){throw new IllegalStateException("Archived chat evidence unavailable; original retained: "+e.getMessage(),e);}
    }
    void validate(JSONObject data){
        if(data.getJSONArray("tasks").length()>24||refs(data).length()>MAX_RECORDS)throw new IllegalStateException("Chat history capacity differs");
        var ids=new HashSet<String>();for(Object raw:data.getJSONArray("tasks")){var g=(JSONObject)raw;if(!ids.add(g.getString("id")))throw new IllegalStateException("Duplicate active chat ID");}
        for(Object raw:resolveAll(refs(data))){var g=(JSONObject)raw;if(!ids.add(g.getString("id")))throw new IllegalStateException("Duplicate archived chat ID");}
    }
    JSONObject evidence(JSONObject g)throws Exception{
        closed(g);String outcome=g.getString("outcome");var episode=learning.read("episode",g.getString("episodeId"));
        if(!episode.getString("status").equals(outcome)||!episode.getString("goalDigest").equals(MeasurementStore.hash(g.getString("goal"))))throw new IllegalStateException("Original chat episode or outcome changed");
        var proof=new JSONObject().put("taskSha256",WorldFrames.hash(g)).put("episodeSha256",WorldFrames.hash(episode)).put("outcome",outcome);
        var frames=new JSONArray();for(Object value:g.getJSONArray("frames")){
            String id=value.toString();UUID.fromString(id);Path dir=project.resolve("agency-043/screens"),meta=dir.resolve(id+".json"),png=dir.resolve(id+".png");
            if(Files.isSymbolicLink(dir)||!Files.isRegularFile(meta,LinkOption.NOFOLLOW_LINKS)||!Files.isRegularFile(png,LinkOption.NOFOLLOW_LINKS))throw new IllegalStateException("Missing or linked original screen evidence");
            var m=AgentFiles042.loadChecked(meta);byte[] b=Files.readAllBytes(png);
            if(!m.getString("id").equals(id)||!m.getString("taskId").equals(g.getString("id"))||b.length!=m.getInt("bytes")||!PerceptionStore.hash(b).equals(m.getString("sha256")))throw new IllegalStateException("Original chat screen evidence changed");
            frames.put(new JSONObject().put("id",id).put("metadataSha256",WorldFrames.hash(m)).put("pixelsSha256",m.getString("sha256")));
        }proof.put("frames",frames);
        if(g.optString("family044").equals("workflow")){
            if(workflows==null)throw new IllegalStateException("Native workflow verifier unavailable");proof.put("workflow",workflows.check(WorldFrames.copy(g)));
        }else if(outcome.equals("verified")&&g.has(BrowserOutcome126.KEY)){
            proof.put("browserOutcome126",BrowserOutcome126.evidence(g));
        }else if(outcome.equals("verified")){
            var ref=g.getJSONObject(g.getString("mode").equals("inspect")?"report":"artifact");var artifact=store.read(ref.getString("id"));var test=artifact.optJSONObject("test");
            if(artifact.getInt("version")!=ref.getInt("version")||!WorldFrames.hash(artifact.getJSONObject("contents")).equals(ref.getString("sha256"))||test==null||!test.getBoolean("passed")||test.getInt("version")!=artifact.getInt("version")||test.getJSONArray("checks").isEmpty())throw new IllegalStateException("Original chat artifact or test changed");
            String testHash=WorldFrames.hash(test);boolean witnessed=false;for(Object raw:g.getJSONArray("history")){var row=(JSONObject)raw;var detail=row.getJSONObject("detail");if(row.getString("kind").equals("surface-action")&&detail.optString("error").isEmpty()&&detail.optString("resultSha256").equals(testHash))witnessed=true;}
            if(!witnessed)throw new IllegalStateException("Original journal does not witness this test receipt");
            if(g.optBoolean("counterContract")&&!test.getJSONArray("steps").similar(Agency043.counterChecks()))throw new IllegalStateException("Original frozen counter checks changed");
            proof.put("artifactSha256",WorldFrames.hash(artifact));
            for(String key:List.of("savedRecipe","usedRecipe","recipeBinding"))if(g.has(key)){var binding=g.getJSONObject(key);var recipe=store.skill(binding.getString("name"));if(recipe.getInt("version")!=binding.getInt("version")||!WorldFrames.hash(recipe).equals(binding.getString("sha256")))throw new IllegalStateException("Original recipe changed");proof.put(key,WorldFrames.hash(recipe));}
        }else if(g.optBoolean("created")||g.optBoolean("needsArtifact")||g.has("report"))throw new IllegalStateException("Unverified chat cannot replace a required artifact outcome");
        return proof;
    }
    JSONObject prepare(Path checkpoint,JSONObject current,Set<String> ids,Map<String,JSONObject> proofs)throws Exception{
        if(ids.isEmpty()||ids.size()>24||refs(current).length()+ids.size()>MAX_RECORDS)throw new IllegalArgumentException("Archive one to24 completed chat tasks within512 archived records");
        byte[] bytes=Files.readAllBytes(checkpoint);if(bytes.length>MAX_SNAPSHOT||!AgentFiles042.loadChecked(checkpoint).similar(current))throw new IllegalStateException("Chat checkpoint changed; retain originals and reopen");
        String hash=PerceptionStore.hash(bytes);Files.createDirectories(directory());Path target=directory.resolve(hash+".json");long total=0;
        try(var files=Files.list(directory)){for(Path p:files.toList()){if(!Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS))throw new IllegalStateException("Unexpected chat archive path");total=Math.addExact(total,Files.size(p));}}
        boolean exists=Files.exists(target,LinkOption.NOFOLLOW_LINKS);if(total+(exists?0:bytes.length)>MAX_BYTES)throw new IllegalStateException("Chat archive byte budget reached; originals retained");
        if(exists){if(!Files.isRegularFile(target,LinkOption.NOFOLLOW_LINKS)||!Arrays.equals(bytes,Files.readAllBytes(target)))throw new IllegalStateException("Prepared chat snapshot differs");}
        else{Path temporary=Files.createTempFile(directory,"chat-",".pending");try(var channel=FileChannel.open(temporary,StandardOpenOption.WRITE)){var buffer=ByteBuffer.wrap(bytes);while(buffer.hasRemaining())channel.write(buffer);channel.force(true);}Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE);}
        var next=WorldFrames.copy(current);var remaining=new JSONArray();var archived=refs(next);int selected=0;
        for(int i=0;i<current.getJSONArray("tasks").length();i++){var g=current.getJSONArray("tasks").getJSONObject(i);if(!ids.contains(g.getString("id"))){remaining.put(WorldFrames.copy(g));continue;}closed(g);var proof=Objects.requireNonNull(proofs.get(g.getString("id")));selected++;
            var ref=new JSONObject().put("schema",SCHEMA).put("id",g.getString("id")).put("createdAt",g.getString("createdAt")).put("outcome",g.getString("outcome")).put("snapshotSha256",hash).put("snapshotBytes",bytes.length).put("index",i).put("taskSha256",WorldFrames.hash(g)).put("evidenceSha256",WorldFrames.hash(proof)).put("archivedAt",Instant.now().toString());resolve(ref);archived.put(ref);
        }if(selected!=ids.size())throw new IllegalArgumentException("Unknown chat task");next.put("tasks",remaining).put(KEY,archived);validate(next);return next;
    }
}
