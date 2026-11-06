package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;
/** Actual isolated native moving-stage proof, separate from frozen study scoring. */
public class ProcedureNative077Test {
 @Test public void freshStagedMotionCarriesMeasuredVelocityAndRejectsSubstitution()throws Exception{
  var root=Files.createTempDirectory("procedure077-native-");String goal=UUID.randomUUID().toString();var spec=Procedure077.protocol().getJSONArray("training").getJSONObject(1);var initial=WorldFrames.frame(Procedure077.scene(spec));var forecast=new ArrayList<JSONObject>();boolean moving=false;
  try(var store=new WorldNativeStore(root)){
   for(String phase:List.of("forecast","actual")){var frame=WorldFrames.copy(initial);
    for(int i=0;i<3;i++){String id=UUID.randomUUID().toString();var plan=Procedure077.plan(spec,"clear-three",i,frame);assertTrue(plan.getBoolean("passed"));store.start(id,goal,WorldFrames.scene(frame),plan,false,true);long end=System.nanoTime()+90_000_000_000L;while(store.status(id).equals("running")&&System.nanoTime()<end)Thread.sleep(20);assertEquals("completed",store.status(id));var run=store.run(id);assertTrue(WorldVerify.verify(run).getBoolean("passed"));assertTrue(Procedure077.protectedTrace(spec,run));AgentFiles042.saveChecked(root.resolve(phase+"-"+i+".json"),run);
     if(i==1){assertTrue(frame.getLong("elapsedTicks")>0);for(Object raw:frame.getJSONArray("bodies")){var b=(JSONObject)raw;moving|=WorldVerify.norm(b.getJSONArray("velocity"))>0;var initialized=ContactPhysics.dynamic(run.getJSONObject("initialState").getJSONArray("bodies"),b.getString("id"));assertTrue(WorldVerify.distance(b.getJSONArray("velocity"),initialized.getJSONArray("velocity"))<2e-6);}assertThrows(IllegalArgumentException.class,()->Rehearsal073.rest(run));}
     if(phase.equals("forecast"))forecast.add(run);else{var predicted=forecast.get(i);assertTrue(Procedure077.comparison(predicted,run).getBoolean("passed"));assertThrows(IllegalStateException.class,()->Procedure077.comparison(run,run));var changed=WorldFrames.copy(run);changed.getJSONObject("definition").getJSONObject("worldFrame").getJSONArray("bodies").getJSONObject(0).getJSONArray("velocity").put(0,1);assertThrows(IllegalStateException.class,()->Procedure077.comparison(predicted,changed));}
     var next=WorldFrames.after(frame,run);assertEquals(frame.getLong("elapsedTicks")+run.getJSONObject("final").getLong("tick"),next.getLong("elapsedTicks"));frame=next;
    }assertTrue(Procedure077.task(spec,frame).getBoolean("passed"));
   }
  }
  assertTrue("Measured stage retains nonzero velocity, not a synthetic rest reset",moving);System.out.println("PROCEDURE077_NATIVE_SOURCE "+root);
 }
}
