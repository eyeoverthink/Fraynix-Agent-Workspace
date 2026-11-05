package fraymus.workspace;

import org.json.*;
import org.junit.*;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

public class Rehearsal073Test {
    static JSONObject original,goal;static Path root;
    @BeforeClass public static void actualNativeSource()throws Exception {
        root=Files.createTempDirectory("memory073-native-");var ledger=new WorldLedger(root);var args=Counterfactual045Test.input(ledger,Counterfactual045Test.control());var binding=ledger.observe(args.getString("worldId"));
        original=Counterfactual045Test.actual(ledger,root,binding,ledger.preview(args));
        // Software receipt fixture only; the separate Electron test uses actual canvas checks.
        goal=new JSONObject().put("id",original.getString("goalId")).put("worldBinding",binding).put("worldCommit",ledger.commit(binding,original.getString("goalId"),original,WorldLedgerTest.receipt(original),WorldLedgerTest.meta())).put("poseView",WorldLedgerTest.receipt(original)).put("report",WorldLedgerTest.meta());
        Files.writeString(root.resolve("original.json"),original.toString());System.out.println("MEMORY073_NATIVE_SOURCE "+root);
    }
    @Test public void actualColdReconstructionAndImmutableMemorySurviveRestart()throws Exception {
        String id;JSONObject first;try(var memory=new Rehearsal073(root)){
            first=memory.remember(goal,original);id=first.getString("id");assertTrue(first.similar(memory.remember(goal,original)));String rid=UUID.randomUUID().toString();memory.start(rid,UUID.randomUUID().toString(),memory.read(id));
            long until=System.nanoTime()+90_000_000_000L;while(memory.status(rid).equals("running")&&System.nanoTime()<until)Thread.sleep(10);
            var cmp=Rehearsal073.comparison(original,memory.run(rid));assertTrue(cmp.toString(),cmp.getBoolean("passed"));assertEquals(1,cmp.getDouble("sampleCoverage"),0);Files.writeString(root.resolve("reconstruction.json"),new JSONObject().put("comparison",cmp).put("run",memory.run(rid)).toString());
        }
        try(var memory=new Rehearsal073(root)){assertTrue(first.similar(Rehearsal073.summary(memory.read(id))));}
    }
    @Test public void oneComponentChangesWithoutLosingRolesUnitsOrTargets()throws Exception {
        var e=new JSONObject().put("run",original);String role=original.getJSONObject("plan").getJSONObject("intent").getJSONArray("targets").getJSONObject(0).getString("sourceId");
        for(String property:List.of("friction","massKg"))for(double value:property.equals("friction")?new double[]{.2,1.2}:new double[]{.7,1.5}){
            var a=new JSONObject().put("sourceId",role).put("property",property).put("value",value);var v=Rehearsal073.variant(e,a);var changed=new JSONObject(v.getString("sourceText"));var before=new JSONObject(WorldFrames.frame(original.getJSONObject("definition")).getJSONObject("originalScene").getString("sourceText"));
            for(Object raw:changed.getJSONArray("objects")){var o=(JSONObject)raw;if(o.getString("sourceId").equals(role)){assertEquals(value,o.getDouble(property),0);o.put(property,v.getDouble("before"));}}
            assertTrue(before.similar(changed));
        }
        assertThrows(IllegalArgumentException.class,()->Rehearsal073.variant(e,new JSONObject().put("sourceId","floor").put("property","massKg").put("value",1)));
        assertThrows(IllegalArgumentException.class,()->Rehearsal073.variant(e,new JSONObject().put("sourceId","missing").put("property","friction").put("value",.2)));
    }
    @Test public void malformedChangesAndAmbiguousChatCannotGainActionAuthority(){
        var a=Rehearsal073.request("Rehearse experiment, change block-a friction to0.2 and execute");assertEquals("block-a",a.getString("sourceId"));assertFalse(a.getBoolean("review"));
        assertTrue(Rehearsal073.request("Rehearse experiment, change block-a massKg to1.5; review before executing").getBoolean("review"));
        assertTrue(Rehearsal073.request("Rehearse experiment, change block-a friction to0.2").getBoolean("review"));
        assertThrows(IllegalArgumentException.class,()->Rehearsal073.request("Rehearse experiment, change a friction to0.2 and change b massKg to1"));
        assertThrows(IllegalArgumentException.class,()->Rehearsal073.request("Rehearse experiment; assume success"));
        for(Object bad:List.of(".2",-1,3))assertThrows(IllegalArgumentException.class,()->Rehearsal073.change(new JSONObject().put("sourceId","a").put("property","friction").put("value",bad)));
        assertThrows(IllegalArgumentException.class,()->Rehearsal073.change(new JSONObject(a.toString()).put("handler","execute")));
        assertEquals("rehearsal",ChatPolicy044.route("Rehearse experiment, change a friction to0.2",new JSONObject(),false));
        assertEquals("workbench",ChatPolicy044.route("Build a web app to rehearse experiment",new JSONObject(),false));
        assertTrue(Tools044.names().contains("experiment.rehearse"));
    }
    @Test public void rehashedTraceAndCodeTamperingAreRejected()throws Exception {
        Path folder=Files.createTempDirectory("memory073-tamper-");String id;try(var memory=new Rehearsal073(folder)){id=memory.remember(goal,original).getString("id");}
        Path file=folder.resolve("experiments-073/episodes/"+id+".json");byte[] bytes=Files.readAllBytes(file);var e=AgentFiles042.read(file,32_000_000).getJSONObject("data");e.put("nativeIdentity","0".repeat(64));AgentFiles042.saveChecked(file,e);
        try(var memory=new Rehearsal073(folder)){assertThrows(IllegalStateException.class,()->memory.read(id));}Files.write(file,bytes);
        e=AgentFiles042.read(file,32_000_000).getJSONObject("data");e.getJSONObject("run").getJSONObject("final").getJSONArray("bodies").getJSONObject(0).getJSONArray("position").put(0,10);e.put("runSha256",WorldFrames.hash(e.getJSONObject("run")));AgentFiles042.saveChecked(file,e);
        try(var memory=new Rehearsal073(folder)){assertThrows(RuntimeException.class,()->memory.read(id));}
    }
    @Test public void arbitraryMovingStateAndUnownedMemoryAreRefused()throws Exception {
        var r=WorldFrames.copy(original);r.put("resumes",1);JSONObject resumed=r;assertThrows(IllegalArgumentException.class,()->Rehearsal073.rest(resumed));
        r=WorldFrames.copy(original);r.getJSONObject("definition").getJSONObject("worldFrame").put("elapsedTicks",12);JSONObject moving=r;assertThrows(RuntimeException.class,()->Rehearsal073.rest(moving));
        var folder=Files.createTempDirectory("memory073-unowned-");try(var artifacts=new WorkspaceStore(folder);var scenes=new SceneStore(folder);var brain=new BrainService(folder);var goals=new GoalRunner(folder,scenes,brain,artifacts)){
            assertThrows(RuntimeException.class,()->goals.remember073(goal.getString("id")));
            assertThrows(IllegalArgumentException.class,()->goals.startRehearsal073(new JSONObject().put("sourceId","a").put("property","friction").put("value",.2)));
        }
    }
}
