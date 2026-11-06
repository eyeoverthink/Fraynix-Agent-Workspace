package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

/** Artificial score rows exercise claim gates; they are never experiment evidence. */
public class Procedure077Test {
    private JSONArray rows(){var a=new JSONArray();for(int i=0;i<12;i++){var rs=new JSONObject();for(String m:Procedure077.METHODS)rs.put(m,new JSONObject().put("success",i<8).put("status",i<8?"completed":"refused").put("chargedTicks",i<8?1000:0).put("attempts",new JSONArray()).put("activeWallMillis",10));a.put(new JSONObject().put("split",i<8?"unseen":"unsupported").put("results",rs));}return a;}
    @Test public void equalWorkAndLegacyOnlyBenefitDoNotQualify(){var r=rows();assertFalse(Procedure077.evaluation(r).getBoolean("qualified"));r.getJSONObject(0).getJSONObject("results").getJSONObject("legacy").put("success",false);assertFalse(Procedure077.evaluation(r).getBoolean("qualified"));}
    @Test public void cheaperNativeWorkDoesNotEraseWallCostOrHarm(){var r=rows();for(int i=0;i<8;i++)r.getJSONObject(i).getJSONObject("results").getJSONObject("memory").put("chargedTicks",800).put("activeWallMillis",11);var e=Procedure077.evaluation(r);assertTrue(e.getBoolean("qualified"));assertFalse(e.getBoolean("efficient"));r.getJSONObject(1).getJSONObject("results").getJSONObject("memory").put("success",false);assertFalse(Procedure077.evaluation(r).getBoolean("qualified"));}
    @Test public void unsupportedActuationCannotBeExcluded(){var r=rows();for(int i=0;i<8;i++)r.getJSONObject(i).getJSONObject("results").getJSONObject("memory").put("chargedTicks",1);r.getJSONObject(8).getJSONObject("results").getJSONObject("memory").getJSONArray("attempts").put(new JSONObject());assertFalse(Procedure077.evaluation(r).getBoolean("qualified"));}
    @Test public void trainingCostIsIncludedInFirstBatchGate(){var r=rows();for(int i=0;i<8;i++)r.getJSONObject(i).getJSONObject("results").getJSONObject("memory").put("chargedTicks",800).put("activeWallMillis",5);var j=new JSONObject().put("rows",r).put("trainingTicks",2000).put("createdAt","2026-01-01T00:00:00Z").put("bankFrozenAt","2026-01-01T00:01:00Z");var e=ProcedureStudy077.evaluation(j);assertTrue(e.getBoolean("qualified"));assertTrue(e.getBoolean("efficient"));assertFalse(e.getBoolean("firstBatchEfficient"));assertEquals(8400,e.getLong("memoryWithTrainingTicks"));}
    @Test public void missingRolesFallBackWithoutInventingTraining(){var s=Procedure077.protocol().getJSONArray("training").getJSONObject(0);var bank=new JSONArray().put(new JSONObject().put("id","one").put("trainingRowId","train-open").put("family","direct").put("trainingTicks",100).put("spec",s));assertEquals(1,Procedure077.rank(bank,s).length());assertTrue(Procedure077.rank(bank,Procedure077.protocol().getJSONArray("cases").getJSONObject(7)).isEmpty());}
    @Test public void untrustedSuccessCannotStartAStudy()throws Exception{try(var store=new ProcedureStudy077(Files.createTempDirectory("procedure077-input-"))){assertThrows(IllegalArgumentException.class,()->store.start(new JSONObject().put("passed",true)));assertTrue(store.list().isEmpty());assertThrows(IllegalArgumentException.class,()->store.resume(UUID.randomUUID().toString()));}}
}
