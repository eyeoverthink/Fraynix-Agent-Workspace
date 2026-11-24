package fraymus.workspace;

import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/** Durable, source-bound carry proposals and results. Access is serialized by SpatialStore. */
final class SpatialCarryStore {
    private final Path directory;
    private final Path nativeDir;

    SpatialCarryStore(Path project) {
        directory=project.resolve("spatial/carry-reviews");
        nativeDir=project.resolve("native");
    }

    JSONObject review(JSONObject scene,JSONObject request) throws Exception {
        SpatialScene.keys(request,"sceneId","objectId","displacementM");
        var binding=new JSONObject().put("source",SpatialPropCopy.binding(scene))
                .put("request",request).put("policy",SpatialCarry.POLICY);
        var key=WorldFrames.hash(binding);
        Path file=file(key);
        if (Files.exists(file)) return read(scene,key);
        Files.createDirectories(directory);
        try (var entries=Files.list(directory)) {
            if (entries.filter(entry->entry.getFileName().toString().endsWith(".json")).limit(32).count()>=32)
                throw new IllegalStateException("The carry review history limit is 32.");
        }
        var forecast=SpatialCarry.forecast(scene,request,nativeDir);
        var proposal=binding.put("forecast",forecast);
        var record=new JSONObject().put("id",key).put("proposal",proposal).put("approvalSha256",WorldFrames.hash(proposal))
                .put("status",forecast.getBoolean("passed")?"review":"refused");
        save(file,record);
        return record;
    }

    JSONObject latestReference() throws Exception {
        if (!Files.isDirectory(directory)) return new JSONObject();
        Path latest=null;
        FileTime modified=null;
        try (var entries=Files.list(directory)) {
            for (var entry:entries.filter(path->path.getFileName().toString().matches("[a-f0-9]{64}\\.json")).toList()) {
                var time=Files.getLastModifiedTime(entry);
                if (modified==null || time.compareTo(modified)>0 || time.equals(modified) && entry.compareTo(latest)>0) {
                    latest=entry;
                    modified=time;
                }
            }
        }
        if (latest==null) return new JSONObject();
        var envelope=new JSONObject(Files.readString(latest));
        var request=envelope.getJSONObject("record").getJSONObject("proposal").getJSONObject("request");
        String id=latest.getFileName().toString().substring(0,64);
        return new JSONObject().put("sceneId",request.getString("sceneId")).put("id",id);
    }

    JSONObject read(JSONObject scene,String id) throws Exception {
        Path file=file(id);
        var envelope=new JSONObject(Files.readString(file));
        var record=envelope.getJSONObject("record");
        var proposal=record.getJSONObject("proposal");
        var basis=WorldFrames.copy(proposal);
        basis.remove("forecast");
        if (!WorldFrames.hash(record).equals(envelope.getString("sha256")) || !id.equals(record.getString("id"))
                || !id.equals(WorldFrames.hash(basis)) || !WorldFrames.hash(proposal).equals(record.getString("approvalSha256"))
                || !proposal.getJSONObject("source").similar(SpatialPropCopy.binding(scene)))
            throw new IllegalStateException("The carry review or its original source changed.");
        return record;
    }

    JSONObject approve(JSONObject scene,JSONObject input) throws Exception {
        SpatialScene.keys(input,"sceneId","id","approvalSha256");
        String id=input.getString("id");
        var record=read(scene,id);
        if (!record.getString("approvalSha256").equals(input.getString("approvalSha256")))
            throw new IllegalStateException("Approve the exact saved carry review.");
        return switch (record.getString("status")) {
            case "completed" -> record;
            case "review" -> {
                var proposal=record.getJSONObject("proposal");
                if (!SpatialCarry.POLICY.equals(proposal.getString("policy")))
                    throw new IllegalStateException("Review this carry again under the current policy.");
                var checked=SpatialCarry.replay(scene,proposal.getJSONObject("request"),proposal.getJSONObject("forecast"),nativeDir);
                record.put("status","completed").put("verified",checked);
                save(file(id),record);
                yield record;
            }
            default -> throw new IllegalStateException("No verified route is available to approve.");
        };
    }

    private Path file(String id) {
        if (!id.matches("[a-f0-9]{64}")) throw new IllegalArgumentException("Invalid carry review identity.");
        return directory.resolve(id+".json");
    }

    private static void save(Path file,JSONObject record) throws Exception {
        Path pending=Files.createTempFile(file.getParent(),"carry-",".pending");
        try {
            Files.writeString(pending,new JSONObject().put("record",record).put("sha256",WorldFrames.hash(record)).toString());
            Files.move(pending,file,ATOMIC_MOVE,REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(pending);
        }
    }
}
