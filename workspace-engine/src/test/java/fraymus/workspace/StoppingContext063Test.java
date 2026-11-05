package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class StoppingContext063Test {
    @Test public void projectionPreservesFailuresExactMetricsAndOriginalEvidence(){
        var traces=new JSONArray();for(int i=0;i<18;i++)traces.put(new JSONObject().put("definition",Stopping063.definition(26,1.05,.25)).put("variant",Stopping063.VARIANTS.get(i%3)).put("summary",new JSONObject().put("endpointErrorM",-.0447).put("finalErrorM",-.0536).put("maximumRotationDeg",.214).put("firstTouchTick",4).put("firstQuietWindowStartTick",170).put("finalQuiet100ms",true).put("otherOriginalFields","x".repeat(1000))));
        var completed=new JSONObject().put("jobId","actual-study-id").put("modelSha256","model-identity").put("traces",traces).put("evaluation",new JSONObject().put("qualified",false).put("coverage",.875));
        var facts=new JSONObject().put("agentId","persistent-identity").put("recentWitnesses",new JSONArray().put("x".repeat(10000))).put("stopping063",new JSONObject().put("status","cancelled").put("lastCompleted",completed));var before=WorldFrames.hash(facts);
        var g=ChatChain052Test.task();g.getJSONObject("context044").put("agent042Facts",facts).put("worldBinding",new JSONObject().put("expectedRevision",7));var projected=ChatChain052.context(g);ChatChain052.exact(projected,12500);
        assertEquals(before,WorldFrames.hash(facts));assertEquals(7,projected.getJSONObject("worldBinding").getInt("expectedRevision"));var f=projected.getJSONObject("agent042Facts");assertEquals(before,f.getJSONObject("retainedSource063").getString("sha256"));var last=f.getJSONObject("stopping063").getJSONObject("lastCompleted");assertFalse(last.getJSONObject("evaluation").getBoolean("qualified"));assertEquals(.875,last.getJSONObject("evaluation").getDouble("coverage"),0);assertEquals("model-identity",last.getString("modelSha256"));assertEquals(18,last.getJSONObject("traceComparison063").getJSONArray("rows").length());assertEquals(-.0447,last.getJSONObject("traceComparison063").getJSONArray("rows").getJSONArray(0).getDouble(3),0);assertEquals(1000,traces.getJSONObject(0).getJSONObject("summary").getString("otherOriginalFields").length());
    }
    @Test public void ordinaryFactsAndEssentialTaskRequirementsAreNotReduced(){
        var facts=new JSONObject().put("agentId","original").put("recentWitnesses",new JSONArray().put("retained"));assertTrue(facts.similar(StoppingContext063.project(facts)));var g=ChatChain052Test.task();g.getJSONObject("context044").put("agent042Facts",facts).put("worldBinding",new JSONObject().put("mandatory","x".repeat(13000)));assertThrows(IllegalStateException.class,()->ChatChain052.exact(ChatChain052.context(g),12500));
    }
}
