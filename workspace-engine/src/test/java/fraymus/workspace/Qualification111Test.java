package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.charset.StandardCharsets;

/** Identity-policy checks only; actual published lineage is exercised by the Electron cohort. */
public class Qualification111Test {
    static JSONArray originals()throws Exception{try(var in=Qualification111.class.getResourceAsStream("/history111-published.json")){return new JSONObject(new String(in.readAllBytes(),StandardCharsets.UTF_8)).getJSONArray("sources");}}
    @Test public void onlyExactPublishedDependencySetsAreKnown()throws Exception{for(Object item:originals()){var old=((JSONObject)item).getJSONObject("dependencies");assertTrue(Qualification111.known(old));Qualification111.unchangedVerifiers(old);for(String key:old.keySet()){var changed=WorldFrames.copy(old).put(key,"f".repeat(64));assertFalse(Qualification111.known(changed));assertThrows(IllegalStateException.class,()->Qualification111.unchangedVerifiers(changed));}assertFalse(Qualification111.known(WorldFrames.copy(old).put("unreviewed",true)));}}
    @Test public void historicalProtocolIsNeverActiveExecutionAuthority()throws Exception{for(Object item:originals()){var binding=new JSONObject().put("schema",Implementation103.SCHEMA).put("codeSha256",((JSONObject)item).getString("implementation103"));assertTrue(Qualification111.historicalProtocol(binding,new JSONObject().put("status","completed")));for(String state:new String[]{"running","waiting-host","interrupted","cancelled"})assertThrows(IllegalStateException.class,()->Qualification111.historicalProtocol(binding,new JSONObject().put("status",state)));}assertThrows(IllegalStateException.class,()->Qualification111.historicalProtocol(new JSONObject().put("schema",Implementation103.SCHEMA).put("codeSha256","f".repeat(64)),new JSONObject().put("status","completed")));}
    @Test public void currentDependenciesBindBothNewHelpersAndPinnedManifest()throws Exception{var d=Qualification107.dependencies();assertTrue(d.has("GoalArchive111")&&d.has("Qualification111")&&d.has("Implementation103"));assertEquals(Qualification111.code(),d.getString("history111"));assertFalse(Qualification111.known(d));}
}
