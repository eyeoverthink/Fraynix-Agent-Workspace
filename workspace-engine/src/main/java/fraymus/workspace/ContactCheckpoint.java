package fraymus.workspace;

import org.json.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Both native bodies, original trace and complete task controller, atomically committed. */
final class ContactCheckpoint {
    static final String VERSION="native-contact-checkpoint-033-v1";
    private static final class Identity {static final String VALUE=compute();}
    static String identity(){return Identity.VALUE;}
    private static String compute(){var text=new StringBuilder(VERSION).append(PushCheckpoint.identity());for(Class<?> type:new Class<?>[]{ContactPhysics.class,PushPlan.class,PushVerify.class,ContactCheckpoint.class,ContactStore.class,ContactVerify.class})try(var stream=type.getResourceAsStream(type.getSimpleName()+".class")){text.append(MapDefinition.sha(stream.readAllBytes()));}catch(Exception e){throw new IllegalStateException(e);}return MeasurementStore.hash(text.toString());}
    static JSONObject write(Path file,JSONObject payload) throws Exception {
        var envelope=new JSONObject().put("version",VERSION).put("payload",payload).put("sha256",MeasurementStore.hash(MapCheckpoint.canonical(payload)));Files.createDirectories(file.getParent());var pending=file.resolveSibling(file.getFileName()+".pending");
        try(var channel=FileChannel.open(pending,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE)){var b=ByteBuffer.wrap(envelope.toString().getBytes(StandardCharsets.UTF_8));while(b.hasRemaining())channel.write(b);channel.force(true);}
        Files.move(pending,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);return envelope;
    }
    static JSONObject read(Path file,String runId,JSONObject scene) throws Exception {
        if(Files.size(file)>24_000_000)throw new IllegalStateException("Push checkpoint exceeds bounded trace size");var envelope=new JSONObject(Files.readString(file));var p=envelope.getJSONObject("payload");
        if(!VERSION.equals(envelope.getString("version"))||!MeasurementStore.hash(MapCheckpoint.canonical(p)).equals(envelope.getString("sha256")))throw new IllegalStateException("Push checkpoint checksum/version mismatch; no replacement world started");
        var r=p.getJSONObject("run");if(!p.getString("codeSha256").equals(identity())||!runId.equals(r.getString("id"))||!SpatialStore.binding(scene).similar(r.getJSONObject("plan").getJSONObject("intent").getJSONObject("binding")))throw new IllegalStateException("Push checkpoint implementation/source/run mismatch");
        var state=p.getJSONObject("physics");var samples=r.getJSONArray("samples");int tick=state.getInt("tick");var c=state.getJSONObject("controller");
        if(tick<0||tick>ContactPhysics.MAX_TICKS||c.getInt("action")<0||c.getInt("action")>r.getJSONObject("plan").getJSONArray("actions").length()||c.getInt("recoveries")<0||c.getInt("recoveries")>ContactPhysics.MAX_RECOVERIES||!Set.of("actions","settle","finished").contains(c.getString("phase")))throw new IllegalStateException("Push checkpoint controller bounds changed");
        var expected=samples.isEmpty()?r.getJSONObject("initialState"):samples.getJSONObject(samples.length()-1);if(!expected.similar(state))throw new IllegalStateException("Push checkpoint differs from original committed sample");
        var ids=new HashSet<String>();for(Object raw:state.getJSONArray("bodies")){var b=(JSONObject)raw;if(!ids.add(b.getString("id")))throw new IllegalStateException("Duplicate checkpoint body");SpatialScene.vector(b.getJSONArray("position"),30);SpatialScene.vector(b.getJSONArray("velocity"),100);SpatialScene.vector(b.getJSONArray("angularVelocity"),300);var q=b.getJSONArray("rotation");if(q.length()!=4)throw new IllegalStateException("Bad checkpoint quaternion");double n=0;for(Object value:q){double v=((Number)value).doubleValue();if(!Double.isFinite(v))throw new IllegalStateException("Nonfinite native orientation");n+=v*v;}if(Math.abs(n-1)>.001)throw new IllegalStateException("Invalid checkpoint orientation");}
        var intent=r.getJSONObject("plan").getJSONObject("intent");if(ids.size()!=2||!ids.contains(intent.getString("objectId"))||!ids.contains(intent.getString("pusherId")))throw new IllegalStateException("Checkpoint dynamic body identity changed");return envelope;
    }
}
