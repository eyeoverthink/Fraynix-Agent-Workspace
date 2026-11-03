package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import java.nio.file.*;
import static org.junit.Assert.*;
/** Historical035 sequences rerun against the separately locked055 extension; not new held-out evidence. */
public class WorldReservedTest {
 @Test public void differentFrozenSequencesKeepStateAcrossGoalsAndFailureNeverAdvancesHead() throws Exception {
  Path repo=Path.of(System.getProperty("user.dir")).toAbsolutePath();while(!Files.exists(repo.resolve("docs/WORLD_035_CANDIDATE_LOCK.json")))repo=repo.getParent();
  for(String name:new String[]{"WORLD_035_CANDIDATE_LOCK","WORLD_035_FROZEN_CONTRACT"}){var lock=new JSONObject(Files.readString(repo.resolve("docs/"+name+".json")));for(Object raw:lock.getJSONArray(name.contains("CANDIDATE")?"sources":"preservedSources")){var f=(JSONObject)raw;String expected=f.getString("sha256");if(name.contains("CANDIDATE"))for(Object extension:new JSONObject(Files.readString(repo.resolve("docs/WORLD_055_REGRESSION_LOCK.json"))).getJSONArray("extensions")){var e=(JSONObject)extension;if(e.getString("path").equals(f.getString("path"))){assertEquals(expected,e.getString("original035Sha256"));expected=e.getString("current055Sha256");}}assertEquals(f.getString("path"),expected,MeasurementStore.hash(Files.readString(repo.resolve(f.getString("path"))).replace("\r\n","\n")));}}
  Path root=Files.createTempDirectory("world035-reserved-");var results=new JSONArray();for(Object raw:WorldLedgerTest.suite().getJSONArray("cases")){var c=(JSONObject)raw;if(c.getString("split").equals("evaluation"))results.put(WorldLedgerTest.runCase(c,root.resolve(c.getString("id"))));}assertEquals(4,results.length());Files.writeString(root.resolve("summary.json"),results.toString(2));System.out.println("WORLD055_HISTORICAL035_REGRESSION "+root);
 }
}
