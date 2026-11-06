package fraymus.workspace;
import org.json.*;
import org.junit.*;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.io.*;

/** Certificate protocol tests use archived actual074 native evidence, not fabricated passing motion. */
public class RebuildCertificate075Test {
    static JSONObject bundle,run,attempt;
    static JSONObject gzip(Path p)throws Exception {try(var z=new GZIPInputStream(Files.newInputStream(p))){return new JSONObject(new String(z.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));}}
    @BeforeClass public static void actualPriorProof()throws Exception {
        Path root=Path.of("../docs/readmes/074/original");
        var x=gzip(root.resolve("app/footprint-1-originals.json.gz"));
        var arm=x.getJSONObject("row").getJSONObject("results").getJSONObject("memory");
        bundle=gzip(root.resolve("bank/"+arm.getJSONObject("retrieval").getJSONObject("selected").getString("id")+".json.gz")).getJSONObject("data");
        for(Object raw:x.getJSONArray("originals")){var o=(JSONObject)raw;if(o.getString("method").equals("memory")&&o.getJSONObject("attempt").getString("stage").equals("rebuild")){
            attempt=o.getJSONObject("attempt");var p=o.getJSONObject("run");
            try(var z=new GZIPInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(p.getString("gzipBase64"))))){byte[] b=z.readAllBytes();assertEquals(p.getString("rawSha256"),MapDefinition.sha(b));run=new JSONObject(new String(b,java.nio.charset.StandardCharsets.UTF_8));}
        }}
        assertNotNull(run);assertTrue(WorldVerify.verify(run).getBoolean("passed"));
    }
    static JSONObject job(){return new JSONObject().put("id",run.getString("goalId")).put("rows",new JSONArray().put(new JSONObject().put("results",new JSONObject().put("cached",new JSONObject().put("attempts",new JSONArray().put(WorldFrames.copy(attempt)))))));}
    static JSONObject owned(JSONObject j){return j.getJSONArray("rows").getJSONObject(0).getJSONObject("results").getJSONObject("cached").getJSONArray("attempts").getJSONObject(0);}
    @Test public void realProofCanBeRecheckedAfterColdStoreRestart()throws Exception {
        Path p=Files.createTempDirectory("certificate075-");var j=job();var store=new RebuildCertificate075(p);
        assertEquals("miss",store.lookup(j,bundle,a->run).getString("status"));
        var c=store.install(j,bundle,owned(j),a->run);var hit=new RebuildCertificate075(p).lookup(j,bundle,a->run);
        assertEquals("hit",hit.getString("status"));assertTrue(c.similar(hit.getJSONObject("certificate")));assertTrue(run.similar(hit.getJSONObject("run")));
        assertFalse(RebuildCertificate075.receipt(hit).has("run"));assertEquals(1,owned(j).getJSONObject("metrics").getBoolean("passed")?1:0);
    }
    @Test public void alteredAuthorityAndChronologyCannotAuthorizeReuse()throws Exception {
        var j=job();var store=new RebuildCertificate075(Files.createTempDirectory("certificate075-tamper-"));var c=store.install(j,bundle,owned(j),a->run);
        for(String k:List.of("version","key","verifier","nativeIdentity","studyId","bundleSha256","originAttemptId","runSha256")){var bad=WorldFrames.copy(c).put(k,"changed");assertThrows(k,Exception.class,()->RebuildCertificate075.verify(j,bundle,bad,a->run));}
        var old=WorldFrames.copy(c).put("createdAt","2000-01-01T00:00:00Z");assertThrows(Exception.class,()->RebuildCertificate075.verify(j,bundle,old,a->run));
        var cmp=WorldFrames.copy(c);cmp.getJSONObject("comparison").put("passed",false);assertThrows(Exception.class,()->RebuildCertificate075.verify(j,bundle,cmp,a->run));
        for(String k:List.of("stage","family","status")){var bad=job();owned(bad).put(k,"forecast");assertThrows(k,Exception.class,()->RebuildCertificate075.verify(bad,bundle,c,a->run));}
        var cost=job();owned(cost).put("chargedTicks",0);assertThrows(Exception.class,()->RebuildCertificate075.verify(cost,bundle,c,a->run));
        var failed=job();owned(failed).getJSONObject("metrics").put("passed",false);assertThrows(Exception.class,()->RebuildCertificate075.verify(failed,bundle,c,a->run));
        var source=WorldFrames.copy(bundle);source.put("preferred","half-force");assertThrows(Exception.class,()->RebuildCertificate075.verify(j,source,c,a->run));
    }
    @Test public void missingOrChangedOriginalAndDamagedCertificateAreInvalidAndPreserved()throws Exception {
        Path p=Files.createTempDirectory("certificate075-invalid-");var j=job();var store=new RebuildCertificate075(p);var c=store.install(j,bundle,owned(j),a->run);
        assertEquals("invalid",store.lookup(j,bundle,a->{throw new IOException("Original proof missing");}).getString("status"));
        var altered=WorldFrames.copy(run).put("status","failed");assertEquals("invalid",store.lookup(j,bundle,a->altered).getString("status"));
        Path file=p.resolve(c.getString("key")+".json");
        AgentFiles042.saveChecked(file,WorldFrames.copy(c).put("createdAt","2999-01-01T00:00:00Z"));assertEquals("invalid",store.lookup(j,bundle,a->run).getString("status"));
        Files.writeString(file,"damaged certificate");assertEquals("invalid",store.lookup(j,bundle,a->run).getString("status"));
        store.install(j,bundle,owned(j),a->run);try(var xs=Files.list(p.resolve("superseded"))){assertEquals("damaged certificate",Files.readString(xs.findFirst().orElseThrow()));}
        assertEquals("hit",store.lookup(j,bundle,a->run).getString("status"));
    }
}
