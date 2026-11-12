package fraymus.workspace;
import org.junit.Test;
import static org.junit.Assert.*;
import org.json.*;
import java.nio.file.*;
import java.util.*;

public class GameCostStudy125Test {
    static final String RETURN="lengthTiles=72 gapTiles=2 secondGapTiles=2 goombaTile=46 obstacleTile=60 obstacleHeight=1 pickupTile=42";
    static JSONObject done(GameCosts125 tasks,String id)throws Exception {
        var t=tasks.read(id);int calls=0;while(t.getString("status").equals("running")&&calls++<650)t=tasks.advance(id);
        assertTrue(t.optString("reason"),Set.of("completed","failed").contains(t.getString("status")));return t;
    }
    static JSONObject run(GameCosts125 tasks,String parameters,String strategy)throws Exception {
        long began=System.nanoTime();String id=UUID.randomUUID().toString();tasks.start(new JSONObject().put("id",id).put("request","Create a playable Mario level "+parameters+" repairLimit=0 pickupStrategy="+strategy));var t=done(tasks,id);
        if(t.getString("status").equals("completed"))tasks.verify(id);else assertThrows(IllegalStateException.class,()->tasks.verify(id));
        var m=tasks.measure(id);return new JSONObject().put("id",id).put("requested",strategy).put("measurement",m).put("externalSeconds",(System.nanoTime()-began)/1e9);
    }
    static Map<String,String> cases(String partition) {
        var result=new LinkedHashMap<String,String>();
        if(partition.equals("reserved")) {result.put("fresh-wait","seed=347 lengthTiles=64 gapTiles=2 secondGapTiles=2 turtleTile=17 obstacleTile=26 obstacleHeight=2");result.put("fresh-return","seed=353 lengthTiles=72 gapTiles=2 secondGapTiles=2 goombaTile=48 obstacleTile=60 obstacleHeight=1 pickupTile=42");}
        else {result.put("known-return","seed=127 "+RETURN);if(partition.equals("development"))result.put("no-gain","seed=257 "+RETURN);}return result;
    }
    @Test public void independentLaterTrialsAuditPredictionCostAndMissedOpportunities()throws Exception {
        String partition=System.getProperty("study125.partition","smoke");Path repo=Path.of("..").toAbsolutePath().normalize(),out=Path.of(System.getProperty("study125.output","build/cost125-study"));Files.createDirectories(out);
        var result=new JSONObject().put("passed",false).put("partition",partition).put("rows",new JSONArray()).put("reviews",new JSONArray()).put("projects",new JSONArray());
        try {for(var item:cases(partition).entrySet()) {
            Path project=Files.createTempDirectory("cost125-study");result.getJSONArray("projects").put(project.toString());String budgetId,lastReview;
            try(var game=new EmbeddedGameClient(repo,project);var tasks=new GameCosts125(repo,project,game,"study125")) {
                var order=new ArrayList<String>();if(!partition.equals("reserved"))for(int i=0;i<3;i++)order.addAll(i%2==0?List.of("direct","full"):List.of("full","direct"));
                for(String strategy:order){var row=run(tasks,item.getValue(),strategy).put("case",item.getKey()).put("role","calibration").put("project",project.toString());result.getJSONArray("rows").put(row);Files.writeString(out.resolve("result.json"),result.toString(2));}
                var budget=run(tasks,item.getValue(),"budget pickupBudgetSeconds=30").put("case",item.getKey()).put("role","decision").put("project",project.toString());budgetId=budget.getString("id");result.getJSONArray("rows").put(budget);Files.writeString(out.resolve("result.json"),result.toString(2));
                if(partition.equals("reserved"))assertEquals("direct",budget.getJSONObject("measurement").getJSONObject("choice124").getJSONObject("decision").getJSONObject("result").getString("selected"));
                var insufficient=tasks.review(budgetId);assertFalse(insufficient.getJSONObject("metrics").getBoolean("established"));assertEquals(0,insufficient.getJSONObject("metrics").getInt("directSamples"));tasks.verifyReview(insufficient.getString("id"));result.getJSONArray("reviews").put(new JSONObject().put("case",item.getKey()).put("role","before independent comparisons").put("report",insufficient));
                for(int i=0;i<3;i++)for(String strategy:(i%2==0?List.of("direct","full"):List.of("full","direct"))) {
                    var row=run(tasks,item.getValue(),strategy).put("case",item.getKey()).put("role","evaluation").put("project",project.toString());result.getJSONArray("rows").put(row);Files.writeString(out.resolve("result.json"),result.toString(2));
                    var m=row.getJSONObject("measurement");assertTrue(m.getBoolean("evidenceVerified"));assertTrue(m.getJSONObject("cost125").getJSONObject("terminal").getBoolean("comparable"));
                    if(!partition.equals("reserved")){assertTrue(m.getBoolean("completionCertified"));assertEquals(0,m.getJSONObject("audit").getInt("aiDamage"));}
                }
                var reviewed=tasks.review(budgetId);lastReview=reviewed.getString("id");assertEquals(3,reviewed.getJSONObject("metrics").getInt("directSamples"));assertEquals(3,reviewed.getJSONObject("metrics").getInt("fullSamples"));tasks.verifyReview(lastReview);tasks.verifyReview(insufficient.getString("id"));result.getJSONArray("reviews").put(new JSONObject().put("case",item.getKey()).put("role","final").put("report",reviewed));Files.writeString(out.resolve("result.json"),result.toString(2));
                // Changed conclusions and removed unfavorable observations cannot become authority.
                Path file=project.resolve("game-reviews125/"+lastReview+".json");String original=Files.readString(file);var bad=AgentFiles042.loadChecked(file);bad.getJSONObject("metrics").put("missedOpportunity","invented success");AgentFiles042.saveChecked(file,bad);assertThrows(IllegalStateException.class,()->tasks.verifyReview(lastReview));Files.writeString(file,original);
                bad=AgentFiles042.loadChecked(file);bad.getJSONArray("sources").remove(1);bad.getJSONArray("samples").remove(0);AgentFiles042.saveChecked(file,bad);assertThrows(IllegalStateException.class,()->tasks.verifyReview(lastReview));Files.writeString(file,original);tasks.verifyReview(lastReview);
                Path clock=project.resolve("game-costs125/"+budgetId+".json");String clockOriginal=Files.readString(clock);var changed=AgentFiles042.loadChecked(clock);changed.getJSONObject("terminal").put("seconds",.00001);AgentFiles042.saveChecked(clock,changed);assertThrows(IllegalStateException.class,()->tasks.verifyReview(lastReview));Files.writeString(clock,clockOriginal);tasks.verifyReview(lastReview);
            }
            try(var game=new EmbeddedGameClient(repo,project);var reopened=new GameCosts125(repo,project,game,"study125")){assertEquals(lastReview,reopened.latestReview(budgetId).getString("id"));reopened.verifyReview(lastReview);}
        }result.put("passed",true);}finally {Files.writeString(out.resolve("result.json"),result.toString(2));}
    }
}
