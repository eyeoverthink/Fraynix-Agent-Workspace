package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;
public class ProofNative078Test {
 @Test public void realNativeProofHitsRechecksAfterRestartAndRefusesChangedMotion()throws Exception{
  var root=Files.createTempDirectory("proof078-native-");var spec=Procedure078.protocol().getJSONArray("training").getJSONObject(0);var scene=Procedure078.scene(spec);var plan=Procedure078.plan(spec,"direct",0,WorldFrames.frame(scene));String id=UUID.randomUUID().toString();JSONObject run;
  try(var store=new WorldNativeStore(root.resolve("native"))){store.start(id,UUID.randomUUID().toString(),scene,plan,false,true);long end=System.nanoTime()+90_000_000_000L;while(store.status(id).equals("running")&&System.nanoTime()<end)Thread.sleep(20);assertEquals("completed",store.status(id));run=store.run(id);}
  assertTrue(WorldVerify.verify(run).getBoolean("passed"));var file=root.resolve("proof.json");AgentFiles042.saveChecked(file,run);Files.copy(file,root.resolve("original.json"));var binding=new JSONObject().put("nativeIdentity",WorldNativeCheckpoint.identity()).put("runSha256",WorldFrames.hash(run));var count=new AtomicInteger();
  ProofMemo078.Audit full=()->{count.incrementAndGet();var e=AgentFiles042.read(file,32_000_000);var data=e.getJSONObject("data");if(!WorldFrames.hash(data).equals(e.getString("sha256"))||!WorldFrames.hash(data).equals(binding.getString("runSha256"))||!WorldVerify.verify(data).getBoolean("passed"))throw new IllegalStateException("Original native proof changed");};
  var memo=new ProofMemo078(root);var checks=new JSONArray();checks.put(memo.check(binding,List.of(file),true,full));checks.put(memo.check(binding,List.of(file),true,full));assertEquals(1,count.get());assertEquals("hit",checks.getJSONObject(1).getString("mode"));checks.put(new ProofMemo078(root).check(binding,List.of(file),true,full));assertEquals(2,count.get());assertEquals("full",checks.getJSONObject(2).getString("mode"));
  var changed=WorldFrames.copy(run);changed.getJSONObject("final").getJSONArray("bodies").getJSONObject(0).getJSONArray("position").put(0,4);AgentFiles042.saveChecked(file,changed);assertThrows(IllegalStateException.class,()->memo.check(binding,List.of(file),true,full));assertEquals(3,count.get());AgentFiles042.saveChecked(root.resolve("cache-checks.json"),new JSONObject().put("passed",true).put("checks",checks).put("originalSha256",binding.getString("runSha256")));System.out.println("PROOF078_NATIVE_SOURCE "+root);
 }
}
