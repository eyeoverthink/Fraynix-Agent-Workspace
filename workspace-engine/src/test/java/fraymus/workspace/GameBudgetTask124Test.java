package fraymus.workspace;
import org.junit.Test;import static org.junit.Assert.*;import org.json.*;import java.nio.file.*;import java.util.*;
public class GameBudgetTask124Test {
 @Test public void oldFailedRecipeRetainsItsOriginalRefusalWithoutCalibration()throws Exception{
  Path repo=Path.of("..").toAbsolutePath().normalize(),project=Files.createTempDirectory("budget124-old-failure");String id=UUID.randomUUID().toString();
  try(var game=new EmbeddedGameClient(repo,project);var tasks=new GameChoices124(repo,project,game)){
   tasks.start(new JSONObject().put("id",id).put("request","Create a playable Mario level gapTiles=10 repairLimit=0"));var t=GameBudgetStudy124Test.done(tasks,id);assertEquals("failed",t.getString("status"));assertEquals("Actual playtest failed; no justified repair remains within the request budget",t.getString("reason"));assertFalse(t.getJSONObject("pickupChoice124").getBoolean("eligible"));assertThrows(IllegalStateException.class,()->tasks.verify(id));
   Files.createDirectories(Path.of("build/budget124-task"));Files.writeString(Path.of("build/budget124-task/legacy-failure.json"),new JSONObject().put("passed",true).put("project",project.toString()).put("task",t).toString(2));
  }
 }
 @Test public void restartAndFailureCannotCalibrateAndLegacyCannotInventAClock()throws Exception{
  Path repo=Path.of("..").toAbsolutePath().normalize(),project=Files.createTempDirectory("budget124-restart"),out=Path.of("build/budget124-task");Files.createDirectories(out);String id=UUID.randomUUID().toString();var results=new JSONArray();String request="Create a playable Mario level seed=127 lengthTiles=72 gapTiles=2 secondGapTiles=2 pickupTile=42 goombaTile=46 obstacleTile=60 obstacleHeight=1 pickupStrategy=budget pickupBudgetSeconds=0";
  try(var game=new EmbeddedGameClient(repo,project);var tasks=new GameChoices124(repo,project,game)){tasks.start(new JSONObject().put("id",id).put("request",request));tasks.advance(id);tasks.advance(id);tasks.advance(id);}
  try(var game=new EmbeddedGameClient(repo,project);var tasks=new GameChoices124(repo,project,game)){assertEquals("interrupted",tasks.read(id).getString("status"));tasks.control(id,"resume");GameBudgetStudy124Test.done(tasks,id);tasks.verify(id);var m=tasks.measure(id);assertFalse(m.getJSONObject("choice124").getBoolean("eligible"));assertTrue(m.getBoolean("completionCertified"));results.put(m);
   String failed=UUID.randomUUID().toString();tasks.start(new JSONObject().put("id",failed).put("request","Create a playable Mario level gapTiles=10 pickupTile=17 goombaTile=45 pickupStrategy=direct"));assertEquals("failed",GameBudgetStudy124Test.done(tasks,failed).getString("status"));var fm=tasks.measure(failed);assertFalse(fm.getBoolean("completionCertified"));assertFalse(fm.getJSONObject("choice124").getJSONObject("fact").getBoolean("safe"));assertTrue(fm.getJSONObject("choice124").getBoolean("eligible"));assertThrows(IllegalStateException.class,()->tasks.verify(failed));results.put(fm);
  }
  Path legacy=Files.createTempDirectory("budget124-legacy");String old=UUID.randomUUID().toString(),text=GameBudgetPolicy124.resolve(request,"direct").replace(" pickupBudgetSeconds=0","");
  try(var game=new EmbeddedGameClient(repo,legacy);var task=new GameTask115(repo,legacy,game)){task.start(new JSONObject().put("id",old).put("request",text));}
  try(var game=new EmbeddedGameClient(repo,legacy);var task=new GameChoices124(repo,legacy,game)){assertFalse(task.start(new JSONObject().put("id",old).put("request",text)).has("pickupChoice124"));assertThrows(IllegalStateException.class,()->task.start(new JSONObject().put("id",old).put("request",request)));assertFalse(Files.exists(legacy.resolve("game-choices124/"+old+".json")));}
  Files.writeString(out.resolve("result.json"),new JSONObject().put("passed",true).put("project",project.toString()).put("measurements",results).toString(2));
 }
}
