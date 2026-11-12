package fraymus.workspace;
import org.junit.Test;import static org.junit.Assert.*;
import org.json.*;import java.nio.file.*;import java.util.*;

/** Repeated complete durable workflows, not isolated controller microbenchmarks. */
public class GamePickupStudy123Test {
 static Map<String,String> cases(String partition){var c=new LinkedHashMap<String,String>();
  switch(partition){
   case "development"->{c.put("supported-wait","seed=53 lengthTiles=64 gapTiles=2 secondGapTiles=2 turtleTile=15 obstacleTile=26 obstacleHeight=2");c.put("supported-return","seed=127 lengthTiles=72 gapTiles=2 secondGapTiles=2 goombaTile=46 obstacleTile=60 obstacleHeight=1 pickupTile=42");c.put("unsupported-pickup","seed=50 lengthTiles=72 gapTiles=3 secondGapTiles=2 goombaTile=28 obstacleTile=25 obstacleHeight=2 pickupTile=18");}
   case "reserved"->{c.put("fresh-wait","seed=251 lengthTiles=64 gapTiles=2 secondGapTiles=2 turtleTile=16 obstacleTile=26 obstacleHeight=2");c.put("fresh-return","seed=257 lengthTiles=72 gapTiles=2 secondGapTiles=2 goombaTile=46 obstacleTile=60 obstacleHeight=1 pickupTile=42");}
   case "smoke"->c.put("supported-return","seed=127 lengthTiles=72 gapTiles=2 secondGapTiles=2 goombaTile=46 obstacleTile=60 obstacleHeight=1 pickupTile=42");
   default->throw new IllegalArgumentException("Unknown partition");
  }return c;
 }
 @Test public void repeatedFullWorkflowCostsKeepEveryOutcomeAndCheck()throws Exception{
  String partition=System.getProperty("study123.partition","smoke"),only=System.getProperty("study123.only","");int repetitions=Integer.parseInt(System.getProperty("study123.repetitions","3"));assertTrue(repetitions>=1&&repetitions<=5);
  var strategies=Arrays.asList(System.getProperty("study123.strategies",partition.equals("smoke")?"full,no-return":"full,no-wait,no-return,direct").split(","));assertTrue(GameStrategy123.STRATEGIES.containsAll(strategies));assertEquals(strategies.size(),new HashSet<>(strategies).size());
  Path repo=Path.of("..").toAbsolutePath().normalize(),out=Path.of(System.getProperty("study123.output","build/pickup123"));Files.createDirectories(out);var rows=new JSONArray();var result=new JSONObject().put("passed",false).put("partition",partition).put("startedAt",java.time.Instant.now().toString()).put("environment",new JSONObject().put("java",System.getProperty("java.version")).put("os",System.getProperty("os.name")).put("processorLimit",Runtime.getRuntime().availableProcessors()).put("maxHeapBytes",Runtime.getRuntime().maxMemory())).put("rows",rows);
  var cases=cases(partition);for(String parameters:cases.values())GameTask115.request("Create a playable Mario level "+parameters+" repairLimit=0");
  try{
   int caseIndex=0;
   for(var c:cases.entrySet()){if(!only.isEmpty()&&!only.equals(c.getKey()))continue;
    for(int repeat=0;repeat<repetitions;repeat++)for(int j=0;j<strategies.size();j++){
     String strategy=strategies.get((j+repeat+caseIndex)%strategies.size());long begin=System.nanoTime();Path project=Files.createTempDirectory("pickup123");String id=UUID.randomUUID().toString();var row=new JSONObject().put("case",c.getKey()).put("repeat",repeat).put("order",j).put("strategy",strategy).put("project",project.toString());
     long setup,advance=0,verify,close;JSONObject measured,task;
     var game=new EmbeddedGameClient(repo,project);var tasks=new GameTask115(repo,project,game);
     try{
      task=tasks.start(new JSONObject().put("id",id).put("request","Create a playable Mario level "+c.getValue()+" repairLimit=0 pickupStrategy="+strategy));setup=System.nanoTime()-begin;
      int calls=0;while(task.getString("status").equals("running")&&calls++<650){long tick=System.nanoTime();task=tasks.advance(id);advance+=System.nanoTime()-tick;}
      assertTrue(task.optString("reason"),Set.of("completed","failed").contains(task.getString("status")));long check=System.nanoTime();measured=tasks.measure123(id);verify=System.nanoTime()-check;
      if(task.getString("status").equals("failed"))assertThrows(IllegalStateException.class,()->tasks.verify(id));
      row.put("advanceCalls",calls).put("measurement",measured).put("task",task);
     }finally{long closing=System.nanoTime();tasks.close();game.close();close=System.nanoTime()-closing;}
     row.put("wallNanos",System.nanoTime()-begin).put("setupNanos",setup).put("advanceNanos",advance).put("verifyNanos",verify).put("closeNanos",close);rows.put(row);
     Files.writeString(out.resolve("result.json"),result.toString(2));
     System.out.println(c.getKey()+" "+repeat+" "+strategy+" "+measured.getString("status")+" pickups="+measured.getJSONObject("audit").getInt("aiPickups"));
    }caseIndex++;
   }
   assertTrue(rows.length()>0);long start=System.nanoTime();result.put("analysis",GameCost123.summarize(rows)).put("analysisNanos",System.nanoTime()-start).put("passed",true);
  }finally{Files.writeString(out.resolve("result.json"),result.toString(2));}
 }
}
