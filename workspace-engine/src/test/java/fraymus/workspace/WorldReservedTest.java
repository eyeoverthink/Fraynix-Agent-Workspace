package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import java.nio.file.*;
import static org.junit.Assert.*;
/** Historical035 sequences rerun against the explicitly reviewed096 source extension; not new held-out evidence. */
public class WorldReservedTest {
 @Test public void differentFrozenSequencesKeepStateAcrossGoalsAndFailureNeverAdvancesHead() throws Exception {
  Path repo=Path.of(System.getProperty("user.dir")).toAbsolutePath();while(!Files.exists(repo.resolve("docs/WORLD_035_CANDIDATE_LOCK.json")))repo=repo.getParent();
  WorldRegression096.verify(repo);
  Path root=Files.createTempDirectory("world035-reserved-");var results=new JSONArray();for(Object raw:WorldLedgerTest.suite().getJSONArray("cases")){var c=(JSONObject)raw;if(c.getString("split").equals("evaluation"))results.put(WorldLedgerTest.runCase(c,root.resolve(c.getString("id"))));}assertEquals(4,results.length());Files.writeString(root.resolve("summary.json"),results.toString(2));System.out.println("WORLD096_HISTORICAL035_REGRESSION "+root);
 }
}
