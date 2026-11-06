package fraymus.workspace;
import org.json.*;
import org.junit.*;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

public class MemoryPolicy075Test {
    @Test public void frozenCasesHaveDistinctNumericScenesAndSupportedSchema(){
        var hashes=new HashSet<String>();int supported=0,unsupported=0;
        for(Object raw:MemoryPolicy075.protocol().getJSONArray("cases")){
            var s=(JSONObject)raw;assertTrue(hashes.add(WorldFrames.hash(s.getJSONObject("source"))));
            if(s.getString("split").equals("unseen")){supported++;var scene=MemoryPolicy075.scene(s);var c=MemoryPolicy075.candidates(s,scene);assertEquals(3,c.length());assertTrue(s.getString("id"),c.getJSONObject(0).getJSONObject("plan").getBoolean("passed"));}
            else{unsupported++;assertThrows(IllegalArgumentException.class,()->MemoryPolicy075.candidates(s,MemoryPolicy075.scene(s)));}
        }
        assertEquals(12,supported);assertEquals(4,unsupported);
    }
    @Test public void unchangedCounterfactualPlansAndOriginalRankAreTheComparator()throws Exception {
        var p=Files.createTempDirectory("memory075-plan-parity-");var ledger=new WorldLedger(p);var input=Counterfactual045Test.input(ledger,Counterfactual045Test.control());var binding=ledger.observe(input.getString("worldId"));var source=ledger.source(binding);var base=ledger.preview(input);
        try(var original=new Counterfactual045(p,ledger)){
            var study=original.prepare(binding,base);var saved=original.export(study.getString("id")).getJSONObject("study");
            var spec=new JSONObject().put("targets",base.getJSONObject("intent").getJSONArray("targets")).put("forceLimitN",base.getJSONObject("intent").getDouble("forceLimitN")).put("torqueLimitNm",base.getJSONObject("intent").getDouble("torqueLimitNm"));
            var now=MemoryPolicy075.candidates(spec,source);
            for(int i=0;i<3;i++)for(String key:List.of("id","ordinal","status","plan","planSha256","motionSha256"))assertTrue(key,new JSONObject().put("v",now.getJSONObject(i).get(key)).similar(new JSONObject().put("v",saved.getJSONArray("candidates").getJSONObject(i).get(key))));
        }
    }
    @Test public void worstCaseAdmissionAlwaysReservesFreshExecution(){
        assertTrue(MemoryPolicy075.admitted(28800,14400,0));assertFalse(MemoryPolicy075.admitted(28801,14400,0));
        assertTrue(MemoryPolicy075.admitted(43200,0,0));assertFalse(MemoryPolicy075.admitted(43201,0,0));
        assertFalse(MemoryPolicy075.admitted(0,0,510001));assertTrue(MemoryPolicy075.admitted(0,0,510000));
        var a=new JSONObject().put("attempts",new JSONArray().put(new JSONObject().put("chargedTicks",14400).put("observedTicks",480)).put(new JSONObject().put("chargedTicks",6000).put("observedTicks",6000)));MemoryStudy075.recomputeCosts(a);assertEquals(20400,a.getInt("chargedTicks"));assertEquals(6480,a.getInt("observedTicks"));
    }
    @Test public void qualificationCannotBeEarnedOnlyByBeatingExhaustiveSearch(){
        var rows=new JSONArray();for(int i=0;i<12;i++){var rs=new JSONObject();for(String m:MemoryPolicy075.METHODS){int ticks=m.equals("search")?24000:m.equals("simple")?12000:m.equals("memory")?24000:18000;rs.put(m,new JSONObject().put("success",true).put("chargedTicks",ticks).put("observedTicks",ticks).put("attempts",new JSONArray()).put("activeWallMillis",1));}rows.put(new JSONObject().put("split","unseen").put("results",rs));}
        var e=MemoryPolicy075.evaluation(rows);assertFalse(e.getBoolean("qualified"));assertTrue(e.getBoolean("cacheQualified"));assertTrue(e.getJSONObject("memoryMinus").getJSONObject("search").getDouble("meanUtilityDelta")>0);assertTrue(e.getJSONObject("memoryMinus").getJSONObject("simple").getDouble("meanUtilityDelta")<0);
        rows.getJSONObject(0).getJSONObject("results").getJSONObject("cached").put("success",false);assertEquals(1,MemoryPolicy075.evaluation(rows).getJSONObject("memoryMinus").getJSONObject("simple").getInt("harmed"));
    }
    @Test public void onlyMemoryMayReorderItsCandidateQueue(){
        assertEquals(MemoryPolicy075.FAMILIES,MemoryPolicy075.order("simple","half-force"));
        assertEquals(List.of("half-force","existing-controller-order","reversed-target-order"),MemoryPolicy075.order("memory","half-force"));
        assertThrows(IllegalArgumentException.class,()->MemoryPolicy075.order("memory","invented"));
    }
    @Test public void unknownInputsAndUnownedEpisodesCannotStartAStudy()throws Exception {
        var p=Files.createTempDirectory("memory075-refusal-");try(var s=new MemoryStudy075(p,id->{throw new IllegalArgumentException("No owned episode");})){
            assertThrows(IllegalArgumentException.class,()->s.start(new JSONObject().put("episodeIds",new JSONArray())));
            assertThrows(IllegalArgumentException.class,()->s.start(new JSONObject().put("episodeIds",new JSONArray().put(UUID.randomUUID().toString())).put("passed",true)));
            assertThrows(IllegalArgumentException.class,()->s.start(new JSONObject().put("episodeIds",new JSONArray().put(UUID.randomUUID().toString()))));assertEquals(0,s.list().length());
        }
    }
    @Test public void proofOnlyRecheckRejectsAnyUnsettledOrMissingCell(){
        var j=new JSONObject().put("progress",64).put("protocol",MemoryPolicy075.protocol()).put("bank",new JSONArray()).put("rows",new JSONArray());
        for(int i=0;i<16;i++){var rs=new JSONObject();for(String m:MemoryPolicy075.METHODS)rs.put(m,new JSONObject().put("status","completed"));j.getJSONArray("rows").put(new JSONObject().put("results",rs));}
        MemoryStudy075.completeEvidence(j);String digest=MemoryStudy075.evidenceDigest(j);j.put("auditProgress",4);assertEquals(digest,MemoryStudy075.evidenceDigest(j));
        j.getJSONArray("rows").getJSONObject(0).getJSONObject("results").getJSONObject("cached").put("status","running");assertThrows(IllegalStateException.class,()->MemoryStudy075.completeEvidence(j));assertNotEquals(digest,MemoryStudy075.evidenceDigest(j));
        j.getJSONArray("rows").getJSONObject(0).getJSONObject("results").remove("cached");assertThrows(IllegalStateException.class,()->MemoryStudy075.completeEvidence(j));
    }
    @Test public void fatalWorkerExitIsPersistedInsteadOfLeavingRunningForever()throws Exception {
        Path p=Files.createTempDirectory("memory075-worker-error-");String id=UUID.randomUUID().toString();
        try(var store=new MemoryStudy075(p,key->{throw new AssertionError("No episode should be read");})){
            var j=new JSONObject(){@Override public JSONArray getJSONArray(String key){if(key.equals("rows"))throw new AssertionError("Controlled worker error fixture");return super.getJSONArray(key);}};
            j.put("id",id).put("code",MemoryStudy075.code()).put("protocol",MemoryPolicy075.protocol()).put("status","running").put("progress",0).put("rows",new JSONArray()).put("evaluation",new JSONObject().put("qualified",true));
            var f=MemoryStudy075.class.getDeclaredField("jobs");f.setAccessible(true);((Map<String,JSONObject>)f.get(store)).put(id,j);
            var execute=MemoryStudy075.class.getDeclaredMethod("execute",String.class);execute.setAccessible(true);execute.invoke(store,id);
            var saved=store.read(id);assertEquals("interrupted",saved.getString("status"));assertTrue(saved.getString("reason").contains("Controlled worker error fixture"));assertFalse(saved.has("evaluation"));assertFalse(store.busy());
        }
        try(var cold=new MemoryStudy075(p,key->{throw new AssertionError();})){assertEquals("interrupted",cold.read(id).getString("status"));}
    }
}
