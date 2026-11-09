package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import java.nio.file.Files;
import static org.junit.Assert.*;

public class Chat099Test {
 private JSONObject world(){return new JSONObject().put("family044","world").put("goal","Arrange the selected targets").put("requested044",new JSONObject()).put("bindings044",new JSONObject().put("world",new JSONObject())).put("context044",new JSONObject().put("selectiveMemory079",true));}
 @Test public void optionalPlanningKeepsDirectDefaultAndReviewRefusal()throws Exception{
  try(var store=new WorkspaceStore(Files.createTempDirectory("chat099-"))){var domain=new Domain044((r,a)->new JSONObject(),store);var g=world();assertEquals("world.arrange",domain.bootstrap(g).getString("tool"));g.put("planWorld099",true);assertNull(domain.bootstrap(g));assertTrue(ChatPolicy044.allowed(g).contains("world.arrange"));g.put("goal","Review before executing; do not move");assertThrows(IllegalArgumentException.class,()->domain.bootstrap(g));g.put("planWorld099",false);assertThrows(IllegalArgumentException.class,()->domain.bootstrap(g));}
 }
 @Test public void planningChoiceIsSealedWhileOlderObjectiveHashesStayCompatible(){
  var g=new JSONObject().put("goal","Arrange").put("history",new JSONArray());assertEquals(WorldFrames.hash(new JSONObject().put("goal","Arrange")),ChatChain052.objectiveHash(g));g.put("planWorld099",true);g.getJSONArray("history").put(new JSONObject().put("kind","queued").put("detail",new JSONObject().put("objectiveSha256",ChatChain052.objectiveHash(g))));ChatChain052.verifyObjective(g);g.put("planWorld099",false);assertThrows(IllegalStateException.class,()->ChatChain052.verifyObjective(g));g.remove("planWorld099");assertThrows(IllegalStateException.class,()->ChatChain052.verifyObjective(g));
 }
 @Test public void malformedPreferenceIsRejectedBeforeAdmission()throws Exception{
  try(var f=new Agency043Test.Fixture(r->{throw new AssertionError("No planner may run");})){assertThrows(IllegalArgumentException.class,()->f.agency.start(new JSONObject().put("goal","Arrange").put("planWorld099","true")));assertFalse(f.agency.busy());}
 }
}
