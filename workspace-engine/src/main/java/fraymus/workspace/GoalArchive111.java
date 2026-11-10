package fraymus.workspace;

import fraymus.factory.*;
import org.json.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.*;

/** Exact goal payload storage. Admission and outcome authority remain in the runners. */
final class GoalArchive111 {
    static final String KEY="archive111",SCHEMA="child-goal-archive-111-v1";
    static final int MAX_RECORDS=512,MAX_BYTES=128*1024*1024,MAX_RECORD_BYTES=8*1024*1024;
    private final Path directory;
    GoalArchive111(Path checkpoint){directory=checkpoint.getParent().resolve("archive111");}
    private Path directory(){
        if(Files.isSymbolicLink(directory.getParent())||Files.isSymbolicLink(directory))throw new IllegalStateException("Goal archive directory must not be linked");
        return directory;
    }
    static boolean eligible(JSONObject g){return !g.has(KEY)&&g.optString("status").equals("completed")&&g.has("workflowId")&&Set.of("data","creation").contains(g.optString("workflowStage"))&&!g.has("hostClaim")&&!g.has("savedSkill");}
    JSONObject resolve(JSONObject record){
        if(!record.has(KEY))return record;
        try{
            SpatialScene.keys(record,"id","createdAt",KEY);var ref=record.getJSONObject(KEY);SpatialScene.keys(ref,"schema","sha256","bytes","archivedAt");
            String hash=ref.getString("sha256");long size=ref.getLong("bytes");
            if(!SCHEMA.equals(ref.getString("schema"))||!hash.matches("[0-9a-f]{64}")||size<2||size>MAX_RECORD_BYTES)throw new IllegalStateException("Invalid child-goal archive reference");
            var file=directory().resolve(hash+".json");if(!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)||Files.size(file)!=size)throw new IllegalStateException("Missing or changed child-goal archive");
            String payload=Files.readString(file,StandardCharsets.UTF_8);if(!MeasurementStore.hash(payload).equals(hash))throw new IllegalStateException("Child-goal archive checksum mismatch");
            var original=new JSONObject(payload);if(!eligible(original)||!original.getString("id").equals(record.getString("id"))||!original.getString("createdAt").equals(record.getString("createdAt")))throw new IllegalStateException("Child-goal identity or completion changed");
            return original;
        }catch(Exception e){throw new IllegalStateException("Archived child evidence unavailable; preserve originals: "+e.getMessage(),e);}
    }
    WorldSnapshot prepare(WorldSnapshot before,SchemaRegistry schemas,Set<String> ids)throws Exception{
        long archived=before.entities().stream().filter(e->e.components().getFirst().type().equals("workspace.goal")&&new JSONObject(e.components().getFirst().values().get("payload")).has(KEY)).count();
        if(ids.isEmpty()||ids.size()>64||archived+ids.size()>MAX_RECORDS)throw new IllegalArgumentException("Archive one to64 child goals within the512-record budget");
        var selected=new ArrayList<EntityRecord>();
        for(String id:ids){var e=before.entities().stream().filter(v->v.id().toString().equals(id)&&v.components().getFirst().type().equals("workspace.goal")).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown child goal"));if(!eligible(new JSONObject(e.components().getFirst().values().get("payload"))))throw new IllegalStateException("Only completed owned data/creation child goals can be archived");selected.add(e);}
        Files.createDirectories(directory());long total=0,additional=0;
        try(var files=Files.list(directory())){for(Path file:files.toList()){if(!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS))throw new IllegalStateException("Unexpected goal archive path");total=Math.addExact(total,Files.size(file));}}
        for(var e:selected){String p=e.components().getFirst().values().get("payload");int bytes=p.getBytes(StandardCharsets.UTF_8).length;if(bytes>MAX_RECORD_BYTES)throw new IllegalStateException("Child-goal record exceeds8MiB");if(!Files.exists(directory().resolve(MeasurementStore.hash(p)+".json"),LinkOption.NOFOLLOW_LINKS))additional+=bytes;}
        if(total+additional>MAX_BYTES)throw new IllegalStateException("Goal archive byte budget reached; originals retained");
        var tx=new WorldTransaction(before);String at=Instant.now().toString();
        for(var e:selected){
            var component=e.components().getFirst();String payload=component.values().get("payload"),hash=MeasurementStore.hash(payload);byte[] bytes=payload.getBytes(StandardCharsets.UTF_8);var target=directory().resolve(hash+".json");
            if(Files.exists(target,LinkOption.NOFOLLOW_LINKS)){if(!Files.isRegularFile(target,LinkOption.NOFOLLOW_LINKS)||!Arrays.equals(bytes,Files.readAllBytes(target)))throw new IllegalStateException("Existing child archive differs; originals retained");}
            else{var temporary=Files.createTempFile(directory(),"goal-",".pending");try(var channel=FileChannel.open(temporary,StandardOpenOption.WRITE)){var buffer=ByteBuffer.wrap(bytes);while(buffer.hasRemaining())channel.write(buffer);channel.force(true);}Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE);}
            var original=new JSONObject(payload);var pointer=new JSONObject().put("id",original.getString("id")).put("createdAt",original.getString("createdAt")).put(KEY,new JSONObject().put("schema",SCHEMA).put("sha256",hash).put("bytes",bytes.length).put("archivedAt",at));resolve(pointer);
            tx.add(new WorldTransaction.Put(e.id(),new ComponentRecord(component.id(),component.type(),component.version(),Map.of("payload",pointer.toString()))));
        }
        return tx.stage(schemas);
    }
}
