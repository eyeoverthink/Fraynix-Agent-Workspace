package fraymus.workspace;
import org.junit.Test;import static org.junit.Assert.*;import org.json.*;import java.nio.file.*;import java.util.*;
/** Real repeated native workflows; no invented timings or substituted physics. */
public class GameBudgetStudy124Test {
 static Map<String,String> cases(String partition){var c=new LinkedHashMap<String,String>();
  if(partition.equals("reserved")){c.put("fresh-wait","seed=311 lengthTiles=64 gapTiles=2 secondGapTiles=2 turtleTile=16 obstacleTile=26 obstacleHeight=2");c.put("fresh-return","seed=313 lengthTiles=72 gapTiles=2 secondGapTiles=2 goombaTile=47 obstacleTile=60 obstacleHeight=1 pickupTile=42");}
  else{if(partition.equals("development"))c.put("supported-wait","seed=53 lengthTiles=64 gapTiles=2 secondGapTiles=2 turtleTile=15 obstacleTile=26 obstacleHeight=2");c.put("supported-return","seed=127 lengthTiles=72 gapTiles=2 secondGapTiles=2 goombaTile=46 obstacleTile=60 obstacleHeight=1 pickupTile=42");if(partition.equals("development"))c.put("no-gain","seed=257 lengthTiles=72 gapTiles=2 secondGapTiles=2 goombaTile=46 obstacleTile=60 obstacleHeight=1 pickupTile=42");}return c;
 }
 static JSONObject done(GameChoices124 tasks,String id)throws Exception{var t=tasks.read(id);int calls=0;while(t.getString("status").equals("running")&&calls++<650)t=tasks.advance(id);assertTrue(t.optString("reason"),Set.of("completed","failed").contains(t.getString("status")));return t;}
 static void recheck(GameChoices124 tasks,String id)throws Exception{if(tasks.read(id).getString("status").equals("completed"))tasks.verify(id);else{tasks.measure(id);assertThrows(IllegalStateException.class,()->tasks.verify(id));}}
 static JSONObject run(GameChoices124 tasks,String parameters,String strategy)throws Exception{
  long began=System.nanoTime();String id=UUID.randomUUID().toString();var t=tasks.start(new JSONObject().put("id",id).put("request","Create a playable Mario level "+parameters+" repairLimit=0 pickupStrategy="+strategy));var decision=t.getJSONObject("pickupChoice124").optJSONObject("decision");t=done(tasks,id);if(t.getString("status").equals("completed"))tasks.verify(id);else assertThrows(IllegalStateException.class,()->tasks.verify(id));var m=tasks.measure(id);return new JSONObject().put("id",id).put("requested",strategy).put("decision",decision).put("measurement",m).put("wallNanos",System.nanoTime()-began);
 }
 @Test public void repeatedMeasurementsChooseWithinAllowanceAndKeepUncertainty()throws Exception{
  String partition=System.getProperty("study124.partition","smoke");Path repo=Path.of("..").toAbsolutePath().normalize(),out=Path.of(System.getProperty("study124.output","build/budget124"));Files.createDirectories(out);var rows=new JSONArray();var result=new JSONObject().put("passed",false).put("partition",partition).put("rows",rows).put("projects",new JSONArray()).put("startedAt",java.time.Instant.now().toString());
  try{for(var c:cases(partition).entrySet()){
   Path project=Files.createTempDirectory("budget124");result.getJSONArray("projects").put(project.toString());String last=null;
   try(var game=new EmbeddedGameClient(repo,project);var tasks=new GameChoices124(repo,project,game,"study124")){
    var sequence=new ArrayList<String>();if(partition.equals("reserved"))sequence.addAll(List.of("budget pickupBudgetSeconds=30","full"));else{for(int i=0;i<3;i++)sequence.addAll(i%2==0?List.of("direct","full"):List.of("full","direct"));sequence.addAll(List.of("budget pickupBudgetSeconds=0","budget pickupBudgetSeconds=4","budget pickupBudgetSeconds=30"));}
    for(String strategy:sequence){var row=run(tasks,c.getValue(),strategy).put("case",c.getKey()).put("project",project.toString());rows.put(row);last=row.getString("id");Files.writeString(out.resolve("result.json"),result.toString(2));var m=row.getJSONObject("measurement");assertTrue(m.getBoolean("evidenceVerified"));if(!partition.equals("reserved")){assertTrue(c.getKey()+" "+strategy,m.getBoolean("completionCertified"));assertEquals(0,m.getJSONObject("audit").getInt("aiDamage"));}
     if(strategy.startsWith("budget")){var d=row.getJSONObject("decision").getJSONObject("result");String selected=d.getString("selected");if(partition.equals("reserved")||strategy.endsWith("=0")||c.getKey().equals("no-gain"))assertEquals("direct",selected);if(!selected.equals("direct")){var option=d.getJSONArray("alternatives").toList().stream().map(x->new JSONObject((Map<?,?>)x)).filter(x->x.getString("strategy").equals(selected)).findFirst().orElseThrow();assertTrue(option.getDouble("requiredSecondsPerPickup")<=d.getDouble("allowance"));assertTrue(option.getDouble("benefitLower")>0);}}
     System.out.println(c.getKey()+" "+strategy+" -> "+m.getString("strategy")+" pickups="+m.getJSONObject("audit").getInt("aiPickups"));
    }
   }
   // Closed/reopened task history and choice sources remain independently verifiable.
   try(var game=new EmbeddedGameClient(repo,project);var tasks=new GameChoices124(repo,project,game,"study124")){recheck(tasks,last);Path receipt=project.resolve("game-choices124/"+last+".json");String original=Files.readString(receipt);String finishedId=last;assertThrows(IllegalStateException.class,()->tasks.control(finishedId,"resume"));assertEquals(original,Files.readString(receipt));var changed=AgentFiles042.loadChecked(receipt);changed.put("seconds",.001);AgentFiles042.saveChecked(receipt,changed);String verifyId=last;assertThrows(IllegalStateException.class,()->tasks.verify(verifyId));Files.writeString(receipt,original);recheck(tasks,last);}
   Path receipt=project.resolve("game-choices124/"+last+".json");String original=Files.readString(receipt);var forged=AgentFiles042.loadChecked(receipt);
   if(forged.getBoolean("budgeted")){
    forged.getJSONObject("decision").getJSONObject("result").put("selected","no-wait");AgentFiles042.saveChecked(receipt,forged);
    try(var game=new EmbeddedGameClient(repo,project);var tasks=new GameChoices124(repo,project,game,"study124")){String verifyId=last;assertThrows(IllegalStateException.class,()->tasks.verify(verifyId));}finally{Files.writeString(receipt,original);}
    var decision=AgentFiles042.loadChecked(receipt).getJSONObject("decision");if(!decision.getJSONArray("sources").isEmpty()){
     Path source=project.resolve("game-choices124/"+decision.getJSONArray("sources").getJSONObject(0).getString("id")+".json");String sourceOriginal=Files.readString(source);var bad=AgentFiles042.loadChecked(source);bad.put("seconds",1);bad.getJSONObject("fact").put("seconds",1);AgentFiles042.saveChecked(source,bad);
     try(var game=new EmbeddedGameClient(repo,project);var tasks=new GameChoices124(repo,project,game,"study124")){String verifyId=last;assertThrows(IllegalStateException.class,()->tasks.verify(verifyId));}finally{Files.writeString(source,sourceOriginal);}
    }
   }
   for(String context:List.of("study124","changed-resource-context"))try(var game=new EmbeddedGameClient(repo,project);var tasks=new GameChoices124(repo,project,game,context)){
    String parameters=context.equals("study124")?c.getValue().replaceFirst("seed=\\d+","seed=997"):c.getValue();String fresh=UUID.randomUUID().toString();var admitted=tasks.start(new JSONObject().put("id",fresh).put("request","Create a playable Mario level "+parameters+" pickupStrategy=budget pickupBudgetSeconds=30"));assertEquals("direct",admitted.getJSONObject("pickupChoice124").getJSONObject("decision").getJSONObject("result").getString("selected"));tasks.control(fresh,"cancel");
   }
  }result.put("passed",true);}finally{Files.writeString(out.resolve("result.json"),result.toString(2));}
 }
}
