package fraymus.workspace;

import org.json.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** A committed run/trace/body envelope. Integrity detects edits; it is not a signature. */
final class MapCheckpoint {
    static final String VERSION="native-map-checkpoint-v1";
    static String canonical(Object value) {
        if(value instanceof JSONObject object) {
            var parts=new java.util.ArrayList<String>();
            for(String key:new java.util.TreeSet<>(object.keySet()))parts.add(JSONObject.quote(key)+":"+canonical(object.get(key)));
            return "{"+String.join(",",parts)+"}";
        }
        if(value instanceof JSONArray array){var parts=new java.util.ArrayList<String>();for(Object item:array)parts.add(canonical(item));return "["+String.join(",",parts)+"]";}
        if(value instanceof Number number)return new java.math.BigDecimal(number.toString()).stripTrailingZeros().toPlainString();
        if(value instanceof Boolean)return value.toString();
        if(value==null||value==JSONObject.NULL)return "null";
        return JSONObject.quote(value.toString());
    }
    private static final class Identity {static final String VALUE=computeIdentity();}
    static String identity(){return Identity.VALUE;}
    private static String computeIdentity() {
        var text=new StringBuilder(VERSION).append("|Bullet240Hz|").append(BulletRamp.BACKEND).append("|")
            .append(System.getProperty("java.version")).append("|").append(System.getProperty("os.name")).append("|").append(System.getProperty("os.arch"));
        // The Minie artifact contains the native Bullet binaries. Bind its actual bytes,
        // not only the version label, so an incompatible backend cannot resume silently.
        try{var jar=Path.of(com.jme3.bullet.PhysicsSpace.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            var digest=java.security.MessageDigest.getInstance("SHA-256");try(var stream=Files.newInputStream(jar)){byte[] buffer=new byte[65536];int n;while((n=stream.read(buffer))>=0)digest.update(buffer,0,n);}text.append(java.util.HexFormat.of().formatHex(digest.digest()));
        }catch(Exception e){throw new IllegalStateException("Cannot establish native checkpoint backend identity",e);}
        for(Class<?> type:new Class<?>[]{MapPhysics.class,MapRecovery.class,MapStore.class,MapCheckpoint.class,MapWork.class,MapMaterials.class,MapDefinition.class}) {
            try(var stream=type.getResourceAsStream(type.getSimpleName()+".class")) {
                if(stream==null) throw new IllegalStateException("Missing checkpoint implementation");
                text.append(MapDefinition.sha(stream.readAllBytes()));
            } catch(Exception e) {throw new IllegalStateException(e);}
        }
        return MeasurementStore.hash(text.toString());
    }
    static JSONObject write(Path file, JSONObject payload) throws Exception {
        var envelope=new JSONObject().put("version",VERSION).put("payload",payload)
            .put("sha256",MeasurementStore.hash(canonical(payload)));
        Files.createDirectories(file.getParent());
        var pending=file.resolveSibling(file.getFileName()+".pending");
        byte[] bytes=envelope.toString().getBytes(StandardCharsets.UTF_8);
        try(var channel=FileChannel.open(pending,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE)) {
            var buffer=ByteBuffer.wrap(bytes);while(buffer.hasRemaining()) channel.write(buffer);channel.force(true);
        }
        try{Files.move(pending,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
        catch(AtomicMoveNotSupportedException e){throw new IllegalStateException("Native checkpoints require atomic replacement on this filesystem",e);}
        return envelope;
    }
    static JSONObject read(Path file, JSONObject record, String runId) throws Exception {
        if(Files.size(file)>24_000_000) throw new IllegalStateException("Checkpoint exceeds bounded trace size");
        var envelope=new JSONObject(Files.readString(file));var p=envelope.getJSONObject("payload");
        if(!VERSION.equals(envelope.getString("version"))||!MeasurementStore.hash(canonical(p)).equals(envelope.getString("sha256")))
            throw new IllegalStateException("Checkpoint checksum/version mismatch; preserved without respawning");
        if(!identity().equals(p.getString("codeSha256"))||!record.getString("sha256").equals(p.getString("mapSha256"))
            ||!record.getString("id").equals(p.getJSONObject("run").getString("mapId"))||!runId.equals(p.getJSONObject("run").getString("id")))
            throw new IllegalStateException("Checkpoint implementation/source/run binding mismatch");
        var state=p.getJSONObject("physics");var samples=p.getJSONObject("run").getJSONArray("samples");
        int tick=state.getInt("tick");
        if(tick<0||tick>21600||state.getInt("pathIndex")<0||state.getInt("pathIndex")>=record.getJSONObject("definition").getJSONArray("path").length()
            ||p.getInt("settled")<0||p.getInt("settled")>60||samples.length()>1801)
            throw new IllegalStateException("Checkpoint controller bounds changed");
        if(samples.isEmpty()?tick!=0:!samples.getJSONObject(samples.length()-1).similar(state.getJSONObject("sample")))
            throw new IllegalStateException("Checkpoint body and last committed sample differ");
        SpatialScene.vector(state.getJSONArray("position"),30);SpatialScene.vector(state.getJSONArray("velocity"),30);
        SpatialScene.vector(state.getJSONArray("angularVelocity"),300);
        var q=state.getJSONArray("rotation");double norm=0;
        if(q.length()!=4)throw new IllegalStateException("Invalid checkpoint orientation");
        for(Object v:q){double n=((Number)v).doubleValue();if(!Double.isFinite(n))throw new IllegalStateException("Nonfinite orientation");norm+=n*n;}
        if(Math.abs(norm-1)>.001)throw new IllegalStateException("Checkpoint orientation is not normalized");
        if(!state.getJSONArray("position").similar(state.getJSONObject("sample").getJSONArray("position"))
            ||!state.getJSONArray("velocity").similar(state.getJSONObject("sample").getJSONArray("velocity"))
            ||!state.getJSONArray("rotation").similar(state.getJSONObject("sample").getJSONArray("rotation"))
            ||!state.getJSONArray("angularVelocity").similar(state.getJSONObject("sample").getJSONArray("angularVelocity"))
            ||state.getInt("pathIndex")!=state.getJSONObject("sample").getInt("pathIndex")
            ||state.getJSONObject("sample").getInt("tick")!=tick)
            throw new IllegalStateException("Checkpoint sample/pose disagreement");
        return envelope;
    }
}
