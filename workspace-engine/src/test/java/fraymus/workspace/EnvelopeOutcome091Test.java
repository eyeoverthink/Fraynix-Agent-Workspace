package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

/** Actual native result, warmed digest table, then independent outcome/source substitutions. */
public class EnvelopeOutcome091Test {
    @Test public void warmEnvelopeNeverAuthorizesAlteredOutcomeOrResignedNativeTrace()throws Exception {
        var root=Files.createTempDirectory("outcome091-native-");System.out.println("OUTCOME091_NATIVE "+root);
        var spec=Procedure079Test.protocol().getJSONArray("training").getJSONObject(0);
        var ledger=new WorldLedger(root);var world=ledger.create(SpatialScene.parse(spec.getJSONObject("source").toString(),UUID.randomUUID().toString()),"LABELLED091 outcome regression");
        String id=UUID.randomUUID().toString();
        try(var runner=new WorldProcedure079(root,ledger)){
            runner.start(id,WorldLedger.binding(world),WorldProcedure079Test.args(spec,"simple"));
            var settled=WorldProcedure079Test.waitDone(runner,id);assertEquals(settled.optString("reason"),"ready",settled.getString("status"));
            var expected=runner.audit(id);var before=runner.envelopeStats091();assertTrue(expected.similar(runner.audit(id)));
            assertEquals(before.getLong("canonicalChecks"),runner.envelopeStats091().getLong("canonicalChecks"));assertTrue(runner.envelopeStats091().getLong("hits")>before.getLong("hits"));
            for(String substitution:List.of("metric","digest","source","plan","cost","outcome","comparison")){
                var j=WorldFrames.copy(settled);var a=j.getJSONObject("arm").getJSONArray("attempts").getJSONObject(0);
                switch(substitution){
                    case "metric"->a.getJSONObject("metrics").put("nativeTicks",-1);
                    case "digest"->a.put("sha256","0".repeat(64));
                    case "source"->a.put("sourceSha256","0".repeat(64));
                    case "plan"->a.put("planSha256","0".repeat(64));
                    case "cost"->a.put("chargedTicks",-1);
                    case "outcome"->j.getJSONObject("arm").put("success",false);
                    case "comparison"->{var programs=j.getJSONObject("arm").getJSONObject("programs");programs.getJSONObject(j.getJSONObject("arm").getString("selected")).getJSONArray("comparisons").getJSONObject(0).put("maxPositionResidualM",999);}
                }
                assertThrows(substitution,Exception.class,()->runner.audit(j));
            }
            var a=settled.getJSONObject("arm").getJSONArray("attempts").getJSONObject(0);var file=root.resolve("world-procedures-079").resolve(id).resolve("attempts").resolve(a.getString("id")).resolve("original.json");
            byte[] original=Files.readAllBytes(file);var run=AgentFiles042.read(file,32_000_000).getJSONObject("data");run.getJSONObject("final").put("workJ",999999);
            AgentFiles042.saveChecked(file,run);var changed=WorldFrames.copy(settled);changed.getJSONObject("arm").getJSONArray("attempts").getJSONObject(0).put("sha256",WorldFrames.hash(run));
            try{assertThrows(Exception.class,()->runner.audit(changed));}finally{Files.write(file,original);}
            assertTrue(expected.similar(runner.audit(id)));assertEquals(0,ledger.observe(world.getString("id")).getInt("expectedRevision"));
        }
        try(var cold=new WorldProcedure079(root,new WorldLedger(root))){assertEquals(0,cold.envelopeStats091().getLong("canonicalChecks"));assertTrue(cold.audit(id).getBoolean("passed"));assertTrue(cold.envelopeStats091().getLong("canonicalChecks")>0);}
    }
}
