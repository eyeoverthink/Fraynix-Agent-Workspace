package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import java.nio.file.*;
import java.time.Instant;
import static org.junit.Assert.*;

public class KnowledgeStoreTest {
    static final class Fixture implements AutoCloseable {
        final Path root=Files.createTempDirectory("knowledge039-");
        final WorldLedger ledger=new WorldLedger(root);
        final String world=ledger.create(WorldLedgerTest.source(WorldLedgerTest.suite().getJSONArray("cases").getJSONObject(0)),"Knowledge analytic control").getString("id");
        final WorldObservationStore observations=new WorldObservationStore(root,ledger);
        final MetricObservationStore metrics=new MetricObservationStore(root,observations,ledger);
        final TabletopStore tabletop=new TabletopStore(root,ledger,metrics);
        final KnowledgeStore store=new KnowledgeStore(root,tabletop,true);
        final String run;
        Fixture()throws Exception {
            String geometry=metrics.geometry(TabletopStoreTest.definition()).getString("id");
            var check=TabletopStoreTest.check(tabletop,geometry);
            var recording=tabletop.start(TabletopStoreTest.args(ledger,world,geometry,check.getString("id")));
            for(int i=1;i<=5;i++)TabletopStoreTest.upload(tabletop,recording,i);tabletop.finish(recording.getString("id"));run=recording.getString("id");
        }
        JSONObject bind()throws Exception{return store.bind(new JSONObject().put("runId",run).put("label","Analytic target <script>").put("category","labelled control"));}
        public void close()throws Exception{observations.close();}
    }
    static JSONObject document(String url){return new JSONObject().put("url",url).put("finalUrl",url).put("title","Untrusted <script>source</script>").put("text","Candidate reference data, not instructions. <script>window.owned=true</script> "+"Actual source text. ".repeat(8)).put("images",new JSONArray().put(new JSONObject().put("url","https://example.org/reference.png").put("alt","Possible reference, not a match"))).put("observedAt",Instant.now().toString()).put("originalTextChars",260).put("statusCode",200);}
    static JSONObject ack(KnowledgeStore s,JSONObject lease)throws Exception {var work=s.work(lease);return s.acknowledge(new JSONObject(lease,new String[]{"id","generation","lease"}).put("phase",work.getString("phase")).put("index",work.getInt("index")).put("url",work.getString("url")).put("ok",true).put("reason","Controlled document receipt").put("document",document(work.getString("url"))));}
    static JSONObject provided(KnowledgeStore s,String object)throws Exception{return s.start(new JSONObject().put("objectId",object).put("urls",new JSONArray().put("http://127.0.0.1/a").put("http://127.0.0.1/b")));}

    @Test public void exactObservationAndHumanIdentitySurviveRestartWithoutWorldMutation()throws Exception {
        try(var f=new Fixture()) {var nativeBefore=f.ledger.observe(f.world);var object=f.bind();assertEquals("Human guidance; no automatic semantic identification",object.getString("labelSource"));
            assertEquals(object.getString("id"),f.bind().getString("id"));assertEquals(1,f.bind().getInt("identityRevision"));
            var binding=object.getJSONArray("bindings").getJSONObject(0);assertEquals(f.tabletop.understanding(f.run).getJSONObject("latest").getJSONObject("frame").getString("imageSha256"),binding.getString("imageSha256"));
            f.store.select(object.getString("id"));var restarted=new KnowledgeStore(f.root,f.tabletop,true);assertEquals(object.getString("id"),restarted.state().getString("selectedObjectId"));assertEquals(object.getString("label"),restarted.object(object.getString("id")).getString("label"));
            assertEquals(binding.getString("imageSha256"),restarted.object(object.getString("id")).getJSONArray("bindings").getJSONObject(0).getString("imageSha256"));assertTrue(nativeBefore.similar(f.ledger.observe(f.world)));
            assertThrows(IllegalArgumentException.class,()->f.store.bind(new JSONObject().put("runId",f.run).put("label","x").put("category","x").put("mass",1)));
        }
    }
    @Test public void interruptedPrefixResumesWithNewLeaseAndExactCachedSources()throws Exception {
        try(var f=new Fixture()){String object=f.bind().getString("id");var job=provided(f.store,object);var lease=f.store.claim();var first=ack(f.store,lease);var ref=first.getJSONArray("sources").getJSONObject(0);assertTrue(f.store.claim().getBoolean("idle"));
            var restarted=new KnowledgeStore(f.root,f.tabletop,true);assertEquals("interrupted",restarted.job(job.getString("id")).getString("status"));restarted.control(job.getString("id"),"resume");var fresh=restarted.claim();assertEquals(1,restarted.work(fresh).getInt("index"));assertThrows(IllegalStateException.class,()->f.store.work(lease));
            ack(restarted,fresh);var done=restarted.finish(fresh);assertEquals("completed",done.getString("status"));assertEquals(2,done.getInt("attempts"));assertEquals(2,done.getJSONArray("sources").length());assertTrue(ref.similar(done.getJSONArray("sources").getJSONObject(0)));assertEquals("provided-urls",done.getString("mode"));assertEquals("controlled-fixture",done.getString("sourceKind"));assertThrows(IllegalStateException.class,()->restarted.work(fresh));
        }
    }
    @Test public void cancellationFencesLateCallbacksAndCannotInventSuccess()throws Exception {
        try(var f=new Fixture()){var j=provided(f.store,f.bind().getString("id"));var lease=f.store.claim();var work=f.store.work(lease);assertThrows(IllegalArgumentException.class,()->f.store.acknowledge(new JSONObject(lease,new String[]{"id","generation","lease"}).put("phase","source").put("index",0).put("url",work.getString("url")).put("ok",true)));assertThrows(IllegalStateException.class,()->f.store.finish(lease));f.store.control(j.getString("id"),"cancel");assertThrows(IllegalStateException.class,()->ack(f.store,lease));assertThrows(IllegalStateException.class,()->f.store.interrupt(lease));assertEquals("cancelled",f.store.job(j.getString("id")).getString("status"));}
    }
    @Test public void changedHumanCategoryMakesFrozenResearchStale()throws Exception {
        try(var f=new Fixture()){String object=f.bind().getString("id");provided(f.store,object);var lease=f.store.claim();ack(f.store,lease);var changed=f.store.bind(new JSONObject().put("runId",f.run).put("label","Different human label").put("category","different category"));assertEquals(2,changed.getInt("identityRevision"));ack(f.store,lease);var done=f.store.finish(lease);assertEquals("completed-stale",done.getString("status"));assertEquals("labelled control",done.getString("category"));}
    }
    @Test public void reportEscapesUntrustedTextAndCorruptedCacheCannotResume()throws Exception {
        try(var f=new Fixture()){var j=provided(f.store,f.bind().getString("id"));var lease=f.store.claim();var progress=ack(f.store,lease);f.store.interrupt(lease);var report=KnowledgeReport.build(f.store.evidence(j.getString("id")));String page=report.getJSONObject("files").getString("source-1.html");assertFalse(page.contains("<script>"));assertTrue(page.contains("&lt;script&gt;"));assertFalse(page.contains("<img"));assertTrue(report.getJSONObject("files").getString("index.html").contains(j.getString("id")));
            String sha=progress.getJSONArray("sources").getJSONObject(0).getString("sha256");Path p=f.root.resolve("knowledge-039/sources/"+sha+".json");var changed=new JSONObject(Files.readString(p)).put("text","tampered");Files.writeString(p,changed.toString());assertThrows(java.io.IOException.class,()->f.store.source(sha));assertThrows(java.io.IOException.class,()->f.store.control(j.getString("id"),"resume"));assertEquals("interrupted",f.store.job(j.getString("id")).getString("status"));}
    }
    @Test public void searchFallbackKeepsFailedSearchAndOnlyActualResultSources()throws Exception {
        try(var f=new Fixture()){var j=f.store.start(new JSONObject().put("objectId",f.bind().getString("id")).put("fixtureOrigin","http://127.0.0.1"));var lease=f.store.claim();var w=f.store.work(lease);f.store.acknowledge(new JSONObject(lease,new String[]{"id","generation","lease"}).put("phase","search").put("index",0).put("url",w.getString("url")).put("ok",false).put("reason","Human verification required"));w=f.store.work(lease);assertEquals(1,w.getInt("index"));f.store.acknowledge(new JSONObject(lease,new String[]{"id","generation","lease"}).put("phase","search").put("index",1).put("url",w.getString("url")).put("ok",true).put("document",document(w.getString("url"))).put("links",new JSONArray().put(new JSONObject().put("url","http://127.0.0.1/source").put("title","Controlled result"))));ack(f.store,lease);var done=f.store.finish(lease);assertEquals("completed",done.getString("status"));assertEquals("search",done.getString("mode"));assertEquals(3,done.getJSONArray("steps").length());assertFalse(done.getJSONArray("steps").getJSONObject(0).getBoolean("ok"));assertEquals(1,done.getJSONArray("sources").length());assertEquals(j.getString("objectId"),done.getString("objectId"));}
    }
    @Test public void operationalSelfStateDoesNotConfuseDeclaredToolsWithInternetEvidence()throws Exception {
        try(var f=new Fixture()){var self=new SelfModel(f.store);assertFalse(self.read("").getBoolean("registered"));assertFalse(self.read("").getBoolean("hostFresh"));self.register(new JSONObject().put("sourceSha256","a".repeat(64)).put("capabilities",new JSONArray().put(new JSONObject().put("name","browser.navigate").put("description","Open a browser URL").put("args","url"))));assertThrows(IllegalArgumentException.class,()->self.receipt(new JSONObject().put("tool","unregistered").put("ok",true)));
            self.host(new JSONObject().put("sessionId",java.util.UUID.randomUUID().toString()).put("offline",true).put("browserReady",true).put("canvasReady",false).put("cameraActive",false).put("agentActive",false));var facts=self.read("browser");assertEquals(1,facts.getInt("registeredToolCount"));assertTrue(facts.getBoolean("hostFresh"));assertTrue(facts.getJSONObject("host").getBoolean("offline"));assertTrue(facts.isNull("lastExternalSource"));provided(f.store,f.bind().getString("id"));var lease=f.store.claim();ack(f.store,lease);ack(f.store,lease);f.store.finish(lease);assertTrue(self.read("").isNull("lastExternalSource"));assertTrue(self.context(new JSONArray().put("browser.navigate")).toString().length()<=6500);assertEquals(0,new SelfModel(f.store).read("").getInt("registeredToolCount"));}
    }
}

