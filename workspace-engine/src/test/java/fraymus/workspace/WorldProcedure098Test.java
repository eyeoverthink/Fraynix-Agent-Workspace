package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

public class WorldProcedure098Test {
    @Test public void nativeDefaultDecisionPersistsAndAlteredClockCannotAuthorizeReuse()throws Exception {
        var root=Files.createTempDirectory("world098-clock-");
        var spec=Procedure079Test.protocol().getJSONArray("training").getJSONObject(0);
        var ledger=new WorldLedger(root);
        var w=ledger.create(SpatialScene.parse(spec.getJSONObject("source").toString(),UUID.randomUUID().toString()),"LABELLED098 clock verification");
        String id=UUID.randomUUID().toString();var args=WorldProcedure079Test.args(spec,"adaptive");
        Path clock=root.resolve("world-procedures-079").resolve(id).resolve("selection-clock098.json");
        try(var runner=new WorldProcedure079(root,ledger)) {
            runner.start(id,WorldLedger.binding(w),args);var j=WorldProcedure079Test.waitDone(runner,id);
            assertEquals(j.optString("reason"),"ready",j.getString("status"));
            assertFalse(j.getJSONObject("decision").getBoolean("useMemory"));assertFalse(j.has("lookupProof"));
            assertTrue(runner.audit(id).getBoolean("passed"));
            var original=Files.readAllBytes(clock);var modified=WorldFrames.copy(j);
            modified.getJSONObject("selectionTiming098").put("totalMillis",0);
            assertThrows(Exception.class,()->runner.audit(modified));
            var timing=AgentFiles042.loadChecked(clock);var c=timing.getJSONObject("clock089");
            c.put("end",Long.toString(Long.parseLong(c.getString("end"))+1_000_000));timing.put("totalMillis",timing.getDouble("totalMillis")+1);
            AgentFiles042.saveChecked(clock,timing);assertThrows(Exception.class,()->runner.audit(id));
            Files.write(clock,original);assertTrue(runner.audit(id).getBoolean("passed"));
            var commit=ledger.commitProcedure079(runner,id,WorldLedgerTest.receipt(runner.last(id)),WorldLedgerTest.meta());
            runner.retain(id,commit);assertEquals(1,runner.bank().length());
            var proofRoot=root.resolve("world-procedures-079/proof-history");boolean clockBound=false;
            try(var paths=Files.list(proofRoot)){for(Path p:paths.toList())for(Object raw:AgentFiles042.loadChecked(p).getJSONArray("evidence"))clockBound|=((JSONObject)raw).getString("path").endsWith("selection-clock098.json");}
            assertTrue("Decision clock must remain bound to retained proof",clockBound);
        }
        try(var cold=new WorldProcedure079(root,new WorldLedger(root))){assertTrue(cold.audit(id).getBoolean("passed"));assertEquals(1,cold.bank().length());}
    }
}
