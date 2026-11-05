package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

/** Frozen changed configurations. First-result files are never overwritten by later regression runs. */
public class CounterfactualReserved045Test {
    @Test public void frozenMaterialAndPoseConfigurationsRetainFirstNativeResults()throws Exception{
        Path first=Path.of("build/counter045-reserved-first");boolean initial=!Files.exists(first);Path root=initial?first:Files.createTempDirectory("counter045-reserved-regression-");Files.createDirectories(root);var results=new JSONArray();
        for(Object raw:Counterfactual045.protocol().getJSONArray("cases")){
            var c=(JSONObject)raw;if(!c.getString("split").equals("reserved"))continue;Path dir=root.resolve(c.getString("id"));Files.createDirectories(dir);var ledger=new WorldLedger(dir);var args=Counterfactual045Test.input(ledger,c);var before=ledger.observe(args.getString("worldId"));
            try(var counter=new Counterfactual045(dir,ledger)){
                var prepared=counter.prepare(before,ledger.preview(args));counter.begin(prepared.getString("id"));JSONObject study=null;for(int i=0;i<18000;i++){study=counter.read(prepared.getString("id"));if(!Set.of("queued","running").contains(study.getString("status")))break;Thread.sleep(10);}
                var result=new JSONObject().put("caseId",c.getString("id")).put("initialReservedResult",initial).put("codeSha256",Counterfactual045.code()).put("protocolSha256",WorldFrames.hash(Counterfactual045.protocol())).put("originalWorld",before).put("study",study);
                Files.writeString(dir.resolve("first-result.json"),result.toString());assertEquals(study.optString("reason"),"ready",study.getString("status"));assertTrue(before.similar(ledger.observe(args.getString("worldId"))));
                var selected=counter.executePlan(study.getString("id"),study.getString("decisionSha256"));var actual=Counterfactual045Test.actual(ledger,dir,before,selected);var data=CounterfactualReport045.data(counter,study.getString("id"),actual);result.put("data",data).put("export",counter.export(study.getString("id")).put("actualRun",WorldReport.packed(actual)));Files.writeString(dir.resolve("first-result.json"),result.toString());
                assertTrue(data.getJSONObject("comparison").toString(),data.getJSONObject("comparison").getBoolean("passed"));assertEquals(3,study.getJSONArray("candidates").length());ledger.commit(before,actual.getString("goalId"),actual,WorldLedgerTest.receipt(actual),WorldLedgerTest.meta());assertEquals(1,ledger.observe(args.getString("worldId")).getInt("expectedRevision"));results.put(result);
            }
        }
        assertEquals(2,results.length());Files.writeString(root.resolve("summary.json"),new JSONObject().put("passed",true).put("initialReservedResult",initial).put("results",results).toString());System.out.println("COUNTER045_RESERVED "+root.toAbsolutePath());
    }
}
