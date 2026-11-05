package fraymus.workspace;
import fraymus.factory.*;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class CalibrationCompatibility068Test {
    @Test public void published067OriginalRestoresThroughCurrentOwnerWithoutChangedEvidence()throws Exception{
        JSONObject original;try(var in=new GZIPInputStream(Files.newInputStream(Path.of("../docs/readmes/067/original/native067/original-experiment.json.gz")))){original=new JSONObject(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));}
        TemporalStudy067.audit(original);String id=original.getString("id"),hash=WorldFrames.hash(original);var root=Files.createTempDirectory("fraynix-original067-compat068-");Files.createDirectories(root.resolve("brain"));
        var schemas=new SchemaRegistry();schemas.register(new ComponentSchema("workspace.brain.job",1,Map.of("payload",ComponentSchema.Kind.TEXT),Set.of("payload")));var world=new FactoryWorld(schemas);var tx=new WorldTransaction(world.snapshot());tx.add(new WorldTransaction.Create(new EntityRecord(UUID.fromString(id),"job",List.of(new ComponentRecord(UUID.randomUUID(),"workspace.brain.job",1,Map.of("payload",BrainPayload064.encode(original)))))));tx.commit(world,schemas);new WorldCheckpointStore(root.resolve("brain/tasks.fworld")).save(world.snapshot(),schemas);
        for(int reopen=0;reopen<2;reopen++)try(var brain=new BrainService(root)){assertEquals(hash,WorldFrames.hash(brain.read(id)));assertEquals(id,brain.temporalState067().getJSONObject("lastCompleted").getString("jobId"));assertEquals(46,brain.temporalTrace067(new JSONObject().put("jobId",id).put("index",0).put("shifted",false)).getJSONObject("results").getJSONObject("simple").getJSONArray("series").length());assertFalse(brain.calibrationState068().getBoolean("available"));assertEquals(1,brain.activePolicy().getInt("version"));}
    }
}
