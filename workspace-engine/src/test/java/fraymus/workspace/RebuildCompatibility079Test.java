package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
/** The immutable074 fixture is historical evidence, not current-engine authority. */
public class RebuildCompatibility079Test {
 @Test public void archivedProofRemainsValidMathButCannotAuthorizeCurrentReuse()throws Exception {
  RebuildCertificate075Test.actualPriorProof();var b=RebuildCertificate075Test.bundle;var run=RebuildCertificate075Test.run;var job=RebuildCertificate075Test.job();String before=WorldFrames.hash(b);
  assertNotEquals(WorldNativeCheckpoint.identity(),b.getJSONObject("episode").getString("nativeIdentity"));assertTrue(WorldVerify.verify(run).getBoolean("passed"));var store=new RebuildCertificate075(Files.createTempDirectory("old-certificate079-"));
  var e=assertThrows(IllegalStateException.class,()->store.install(job,b,RebuildCertificate075Test.owned(job),a->run));assertTrue(e.getMessage().contains("native identity"));assertEquals("miss",store.lookup(job,b,a->run).getString("status"));assertEquals(before,WorldFrames.hash(b));
 }
 static void freshNativeProof(JSONObject forecast,JSONObject actual,JSONObject sourceAttempt)throws Exception {
  var attempt=WorldFrames.copy(sourceAttempt).put("stage","rebuild").put("family","original-memory");var job=new JSONObject().put("id",actual.getString("goalId")).put("rows",new JSONArray().put(new JSONObject().put("results",new JSONObject().put("cached",new JSONObject().put("attempts",new JSONArray().put(attempt))))));
  var bundle=new JSONObject().put("episode",new JSONObject().put("nativeIdentity",WorldNativeCheckpoint.identity()).put("run",forecast));var root=Files.createTempDirectory("fresh-certificate079-");var store=new RebuildCertificate075(root);var c=store.install(job,bundle,attempt,a->actual);assertEquals("hit",new RebuildCertificate075(root).lookup(job,bundle,a->actual).getString("status"));
  for(String key:java.util.List.of("nativeIdentity","verifier","originAttemptId","runSha256")){var bad=WorldFrames.copy(c).put(key,"changed");assertThrows(Exception.class,()->RebuildCertificate075.verify(job,bundle,bad,a->actual));}
  assertEquals("invalid",store.lookup(job,bundle,a->{throw new java.io.IOException("Missing actual original");}).getString("status"));var badRun=WorldFrames.copy(actual).put("status","failed");assertEquals("invalid",store.lookup(job,bundle,a->badRun).getString("status"));
  Path file=root.resolve(c.getString("key")+".json");Files.writeString(file,"damaged");assertEquals("invalid",store.lookup(job,bundle,a->actual).getString("status"));store.install(job,bundle,attempt,a->actual);try(var files=Files.list(root.resolve("superseded"))){assertEquals("damaged",Files.readString(files.findFirst().orElseThrow()));}assertEquals("hit",store.lookup(job,bundle,a->actual).getString("status"));
 }
}
