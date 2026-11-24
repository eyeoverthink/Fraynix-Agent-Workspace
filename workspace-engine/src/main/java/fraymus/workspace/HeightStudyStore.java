package fraymus.workspace;

import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Base64;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/** Retains the original study, reviewed view choice and measurement-backed collider proposal. */
final class HeightStudyStore {
    private final Path directory;

    HeightStudyStore(Path project) {
        directory=project.resolve("spatial/height-studies");
    }

    synchronized JSONObject review(JSONObject input) throws Exception {
        SpatialScene.keys(input,"base64");
        var encoded=input.getString("base64");
        if (encoded.length()>(MetricHeightStudy.LIMIT+2)/3*4) throw new IllegalArgumentException("Choose a height study up to 1.2 MB.");
        byte[] bytes=Base64.getDecoder().decode(encoded);
        String source=MapDefinition.sha(bytes);
        String id=studyId(source);
        if (Files.exists(path(id,".json"))) return view(id);
        Files.createDirectories(directory);
        try (var entries=Files.list(directory)) {
            if (entries.filter(entry->entry.getFileName().toString().endsWith(".json")).limit(32).count()>=32)
                throw new IllegalStateException("The height study history limit is 32.");
        }
        var study=new MetricHeightStudy(bytes);
        var plan=study.plan();
        var record=new JSONObject().put("id",id).put("sourceSha256",source).put("plan",plan)
                .put("status",plan.getBoolean("canObserve")?"review":"refused")
                .put("planSha256",WorldFrames.hash(new JSONObject().put("sourceSha256",source).put("plan",plan)));
        write(path(id,".source"),bytes);
        save(record);
        return response(record,study);
    }

    synchronized JSONObject approve(JSONObject input) throws Exception {
        SpatialScene.keys(input,"id","planSha256");
        var record=record(input.getString("id"));
        if (!input.getString("planSha256").equals(record.getString("planSha256")))
            throw new IllegalStateException("Approve the exact saved camera selection.");
        var study=study(record);
        if (!record.getString("status").equals("review")) return response(record,study);
        var plan=record.getJSONObject("plan");
        if (!plan.similar(study.plan())) throw new IllegalStateException("Recheck the chosen camera. The saved view plan differs.");
        var measurement=study.verify(plan);
        record.put("measurement",measurement).put("status",measurement.getBoolean("passed")?"verified":"refused");
        if (measurement.getBoolean("passed")) {
            String evidence=WorldFrames.hash(new JSONObject().put("planSha256",record.getString("planSha256")).put("measurement",measurement));
            var collider=study.collider(measurement,evidence);
            record.put("evidenceSha256",evidence).put("collider",collider)
                    .put("colliderApprovalSha256",WorldFrames.hash(new JSONObject().put("evidenceSha256",evidence).put("collider",collider)));
        }
        save(record);
        return response(record,study);
    }

    synchronized JSONObject approvedCollider(JSONObject input) throws Exception {
        SpatialScene.keys(input,"id","colliderApprovalSha256");
        var record=record(input.getString("id"));
        if (!record.getString("status").equals("verified")) throw new IllegalStateException("No verified collider proposal exists.");
        if (!input.getString("colliderApprovalSha256").equals(record.getString("colliderApprovalSha256")))
            throw new IllegalStateException("Approve the exact measured collider.");
        var study=study(record);
        var plan=record.getJSONObject("plan");
        var measurement=record.getJSONObject("measurement");
        var verified=study.verify(plan);
        var expected=study.collider(measurement,record.getString("evidenceSha256"));
        if (!verified.similar(measurement) || !expected.similar(record.getJSONObject("collider")))
            throw new IllegalStateException("Original height evidence or collider geometry changed.");
        return record;
    }

    synchronized JSONObject boundDefinition(JSONObject reference) throws Exception {
        var record=record(reference.getString("id"));
        if (!record.getString("status").equals("verified")
                || !record.getString("colliderApprovalSha256").equals(reference.getString("colliderApprovalSha256"))
                || !record.getString("evidenceSha256").equals(reference.getString("evidenceSha256")))
            throw new IllegalStateException("The original measured collider receipt changed.");
        sourceBytes(record);
        return record.getJSONObject("collider");
    }

    synchronized JSONObject view(String id) throws Exception {
        var record=record(id);
        return response(record,study(record));
    }

    synchronized JSONObject latest() throws Exception {
        if (!Files.isDirectory(directory)) return new JSONObject();
        Path latest=null;
        FileTime modified=null;
        try (var entries=Files.list(directory)) {
            for (var entry:entries.filter(p->p.getFileName().toString().matches("[a-f0-9]{64}\\.json")).toList()) {
                var time=Files.getLastModifiedTime(entry);
                if (modified==null || time.compareTo(modified)>0 || time.equals(modified) && entry.compareTo(latest)>0) {
                    latest=entry;
                    modified=time;
                }
            }
        }
        return latest==null?new JSONObject():view(latest.getFileName().toString().substring(0,64));
    }

    private JSONObject record(String id) throws Exception {
        var envelope=new JSONObject(Files.readString(path(id,".json")));
        var record=envelope.getJSONObject("record");
        var plan=new JSONObject().put("sourceSha256",record.getString("sourceSha256")).put("plan",record.getJSONObject("plan"));
        if (!id.equals(record.getString("id")) || !id.equals(studyId(record.getString("sourceSha256")))
                || !WorldFrames.hash(record).equals(envelope.getString("sha256"))
                || !WorldFrames.hash(plan).equals(record.getString("planSha256")))
            throw new IllegalStateException("The saved height study receipt changed.");
        return record;
    }

    private MetricHeightStudy study(JSONObject record) throws Exception {
        return new MetricHeightStudy(sourceBytes(record));
    }

    private byte[] sourceBytes(JSONObject record) throws Exception {
        Path file=path(record.getString("id"),".source");
        if (!Files.isRegularFile(file,java.nio.file.LinkOption.NOFOLLOW_LINKS) || Files.size(file)>MetricHeightStudy.LIMIT)
            throw new IllegalStateException("The original height-study bytes are unavailable.");
        byte[] bytes=Files.readAllBytes(file);
        if (!MapDefinition.sha(bytes).equals(record.getString("sourceSha256")))
            throw new IllegalStateException("The original height-study bytes changed.");
        return bytes;
    }

    private static JSONObject response(JSONObject record,MetricHeightStudy study) {
        return WorldFrames.copy(record).put("previews",study.previews(record));
    }

    private static String studyId(String source) {
        return MeasurementStore.hash(MetricHeightStudy.POLICY+":"+source);
    }

    private Path path(String id,String suffix) {
        if (!id.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Invalid height review identity.");
        return directory.resolve(id+suffix);
    }

    private void save(JSONObject record) throws Exception {
        var envelope=new JSONObject().put("record",record).put("sha256",WorldFrames.hash(record));
        write(path(record.getString("id"),".json"),envelope.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static void write(Path file,byte[] bytes) throws Exception {
        Path pending=Files.createTempFile(file.getParent(),"height-",".pending");
        try {
            Files.write(pending,bytes);
            Files.move(pending,file,ATOMIC_MOVE,REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(pending);
        }
    }
}
