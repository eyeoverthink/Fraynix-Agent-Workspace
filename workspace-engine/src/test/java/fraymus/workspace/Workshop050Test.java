package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;
public class Workshop050Test {
    @Test public void sharedRunnerRestoresAndRejectsCompetingGoals()throws Exception{
        var root=Files.createTempDirectory("workshop050-goal-");String id;
        try(var artifacts=new WorkspaceStore(root);var scenes=new SceneStore(root);var brain=new BrainService(root)){
            var goals=new GoalRunner(root,scenes,brain,artifacts);
            synchronized(goals){id=goals.startWorkshop(new JSONObject().put("saveSkillName","test-construction")).getString("id");final var owned=goals;assertThrows(IllegalStateException.class,()->owned.startWorkshop(new JSONObject()));goals.close();}
            try(var restored=new GoalRunner(root,scenes,brain,artifacts)){
                assertEquals("interrupted",restored.read(id).getString("status"));assertEquals(0,restored.procedures().length());restored.resume(id);
                long end=System.nanoTime()+30_000_000_000L;while(restored.read(id).getString("status").equals("running")&&System.nanoTime()<end)Thread.sleep(50);
                var g=restored.read(id);assertEquals(g.optString("reason"),"completed",g.getString("status"));assertEquals(3,g.getJSONObject("metrics").getInt("delivered"));assertEquals(1,restored.procedures().length());
                var trials=g.getJSONObject("workshop050").getJSONArray("trials");assertEquals(3,trials.length());assertFalse(trials.getJSONObject(0).getJSONObject("verification").getBoolean("passed"));
                assertThrows(IllegalStateException.class,()->restored.resume(id));
            }
        }
    }
    @Test public void cancelCannotCreateASkillAndForeignWorldStaysUnchanged()throws Exception{
        var root=Files.createTempDirectory("workshop050-cancel-");
        try(var artifacts=new WorkspaceStore(root);var scenes=new SceneStore(root);var brain=new BrainService(root);var goals=new GoalRunner(root,scenes,brain,artifacts)){
            var source=scenes.save(new JSONObject().put("title","Existing author scene"));String hash=WorldFrames.hash(source);
            synchronized(goals){var g=goals.startWorkshop(new JSONObject().put("saveSkillName","must-not-save"));goals.cancel(g.getString("id"));assertEquals(0,goals.procedures().length());assertThrows(IllegalStateException.class,()->goals.saveProcedure(g.getString("id"),"must-not-save"));}
            assertEquals(hash,WorldFrames.hash(scenes.read(source.getString("id"))));
        }
    }
    @Test public void nativeSmoothDeliveryAndRoughJam()throws Exception{
        var dir=Files.createTempDirectory("workshop050-native-");var w=new Workshop050(dir);var c=Workshop050.config(new JSONObject());
        for(String material:List.of("smooth","rough")){
            String id=UUID.randomUUID().toString();var p=w.prepare(id,c,Workshop050.design(material,20));assertEquals(material.equals("rough"),p.getJSONObject("specification").getJSONObject("prediction").getBoolean("predictedStall"));
            var r=w.run(id);System.out.println(material+" "+r.getJSONObject("verification"));assertEquals(material.equals("smooth")?3:0,r.getJSONObject("verification").getInt("delivered"));
            var finalBodies=r.getJSONArray("samples").getJSONObject(120).getJSONArray("bodies");for(Object raw:finalBodies)assertTrue(((JSONObject)raw).getInt("contacts")>0);
            assertEquals(WorldFrames.hash(r),WorldFrames.hash(new Workshop050(dir).read(id)));assertEquals(WorldFrames.hash(r),WorldFrames.hash(w.run(id)));
        }
    }
    @Test public void steeperRampAndChangedConditions()throws Exception{
        var w=new Workshop050(Files.createTempDirectory("workshop050-shift-"));
        for(double scale:new double[]{1,1.3}){var c=Workshop050.config(new JSONObject().put("frictionScale",scale).put("startShiftM",.25).put("massKg",1.6));String id=UUID.randomUUID().toString();w.prepare(id,c,Workshop050.design("smooth",20));var r=w.run(id);assertEquals(3,r.getJSONObject("verification").getInt("delivered"));}
        String id=UUID.randomUUID().toString();w.prepare(id,Workshop050.config(new JSONObject()),Workshop050.design("rough",40));var r=w.run(id);System.out.println("repair "+r.getJSONObject("verification"));assertEquals(3,r.getJSONObject("verification").getInt("delivered"));
    }
    @Test public void resourcesAndBoundsAreRealPrerequisites()throws Exception{
        for(JSONObject input:List.of(new JSONObject().put("blockStock",2),new JSONObject().put("wallStock",2),new JSONObject().put("rampStock",0),new JSONObject().put("blockStock",3.5),new JSONObject().put("frictionScale",2),new JSONObject().put("massKg","1"),new JSONObject().put("inventParts",true))){try{Workshop050.config(input);fail("accepted "+input);}catch(IllegalArgumentException expected){}}
    }
    @Test public void changedEvidenceAndIncompleteTraceRefused()throws Exception{
        Path dir=Files.createTempDirectory("workshop050-evidence-");var w=new Workshop050(dir);String id=UUID.randomUUID().toString();var c=Workshop050.config(new JSONObject());w.prepare(id,c,Workshop050.design("smooth",20));var r=w.run(id);
        var broken=new JSONObject(r.toString());broken.getJSONArray("samples").remove(1);try{Workshop050.verify(broken);fail();}catch(IllegalStateException expected){}
        Path file=dir.resolve("workshop-050/"+id+"-result.json");String bytes=Files.readString(file);Files.writeString(file,bytes.replace("\"delivered\":3","\"delivered\":2"));try{w.read(id);fail();}catch(IllegalStateException expected){assertTrue(expected.getMessage().contains("checksum"));}
        try{w.prepare(id,c,Workshop050.design("rough",20));fail();}catch(IllegalStateException expected){assertTrue(expected.getMessage().contains("binding"));}
    }
}
