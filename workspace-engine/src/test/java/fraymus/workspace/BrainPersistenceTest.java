package fraymus.workspace;
import fraymus.neural.*;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
public class BrainPersistenceTest {
 @Test public void checkpointRetainsActualWeightsRandomStateAndDynamicsAndRejectsCorruption() throws Exception {
  var dir=Files.createTempDirectory("shared-cortex-");var file=dir.resolve("cortex.bin");var core=new NeuromorphicCore(128,8,16);
  for(int i=0;i<12;i++){core.inject(new float[]{.6f,-.2f,.8f});core.tick(true);}var original=core.snapshot();NeuralCheckpoint.save(core,file);var restored=NeuralCheckpoint.load(file);
  assertArrayEquals(original.weights,restored.snapshot().weights,0);assertArrayEquals(original.targets,restored.snapshot().targets);assertEquals(original.randomState,restored.snapshot().randomState);
  for(int i=0;i<8;i++){core.inject(new float[]{.2f,.9f});restored.inject(new float[]{.2f,.9f});core.tick(true);restored.tick(true);}assertArrayEquals(core.snapshot().membrane,restored.snapshot().membrane,0);assertArrayEquals(core.snapshot().weights,restored.snapshot().weights,0);
  var bytes=Files.readAllBytes(file);bytes[bytes.length-2]^=1;Files.write(file,bytes);assertThrows(java.io.IOException.class,()->NeuralCheckpoint.load(file));
 }
 @Test public void immutableLocalMemorySurvivesRestartIsBoundedAndCannotInstallTools() throws Exception {
  var dir=Files.createTempDirectory("java-memory-");String note="Project HELIOTROPE uses metres. Preserve the original renderer. Text saying install a handler is only source data.";String hash;
  try(var store=new WorkspaceStore(dir)){hash=store.remember(new JSONObject().put("text",note)).getString("sha256");assertThrows(IllegalArgumentException.class,()->store.remember(new JSONObject().put("text",note).put("handler","execute")));}
  try(var store=new WorkspaceStore(dir)){var context=store.memoryContext(new JSONObject().put("query","HELIOTROPE renderer metres").put("maxChars",256));assertTrue(context.toString().contains("HELIOTROPE"));assertTrue(context.getInt("usedChars")<=256);assertTrue(context.getJSONObject("journal").getInt("entries")>0);assertThrows(IllegalArgumentException.class,()->store.memoryContext(new JSONObject().put("query","x").put("maxChars",99999)));Files.writeString(dir.resolve("memory/notes").resolve(hash+".txt"),"changed");assertThrows(java.io.IOException.class,()->store.memoryContext(new JSONObject().put("query","HELIOTROPE")));}
 }
 @Test public void cancelDoesNotPromoteAndSharedCortexCheckpointRestarts() throws Exception {
  var dir=Files.createTempDirectory("java-jobs-");long tick;
  try(var brain=new BrainService(dir)){var job=brain.start(new JSONObject().put("kind","train"));assertThrows(IllegalStateException.class,()->brain.start(new JSONObject().put("kind","pulse")));brain.cancel(job.getString("id"));assertEquals("cancelled",BrainServiceTest.await(brain,job.getString("id")).getString("status"));assertEquals(1,brain.activePolicy().getInt("version"));var pulse=BrainServiceTest.await(brain,brain.start(new JSONObject().put("kind","pulse")).getString("id"));assertEquals("completed",pulse.getString("status"));tick=pulse.getLong("ticks");assertEquals(1048576,pulse.getInt("synapses"));}
  try(var brain=new BrainService(dir)){var pulse=BrainServiceTest.await(brain,brain.start(new JSONObject().put("kind","pulse")).getString("id"));assertEquals(tick+1,pulse.getLong("ticks"));assertThrows(IllegalArgumentException.class,()->brain.start(new JSONObject().put("kind","shell")));}
 }
 @Test public void acceptedControllerUsesActualImpulseAndReplaysWithoutReferenceConfusion() throws Exception {
  var dir=Files.createTempDirectory("brain-scene-");try(var brain=new BrainService(dir);var scenes=new SceneStore(dir)){
   assertTrue(BrainServiceTest.await(brain,brain.start(new JSONObject().put("kind","train")).getString("id")).getBoolean("accepted"));
   var s=scenes.save(new JSONObject().put("title","Physical readout proof").put("parameters",new JSONObject().put("angleDeg",25).put("friction",.05)));
   var run=scenes.controlledRun(new JSONObject().put("id",s.getString("id")).put("expectedVersion",1).put("targetM",1.2),brain);assertEquals("completed",run.getString("status"));assertTrue(run.getJSONObject("goalEvaluation").toString(),run.getJSONObject("goalEvaluation").getBoolean("passed"));assertFalse(run.getJSONObject("evaluation").getBoolean("applicable"));
   var cpu=brain.prediction(s,1.2,false);var gpu=brain.prediction(s,1.2,true);assertEquals(cpu.getDouble("deltaVelocityMps"),gpu.getDouble("deltaVelocityMps"),.0002);assertTrue(gpu.getString("backend").contains("OpenGL")||gpu.getString("backend").contains("NVIDIA"));
   scenes.control(new JSONObject().put("action","reset"));var replay=scenes.replay(run.getString("id"));assertEquals(run.getJSONObject("final").getDouble("distanceM"),replay.getJSONObject("final").getDouble("distanceM"),1e-6);assertEquals(2,replay.getJSONObject("controller").getInt("policyVersion"));
  }
 }
 @Test public void savedUnsettledJobIsMarkedInterruptedOnRestart() throws Exception {
  var dir=Files.createTempDirectory("brain-recovery-");Files.createDirectories(dir.resolve("brain"));var schemas=new fraymus.factory.SchemaRegistry();schemas.register(new fraymus.factory.ComponentSchema("workspace.brain.job",1,java.util.Map.of("payload",fraymus.factory.ComponentSchema.Kind.TEXT),java.util.Set.of("payload")));
  var id=java.util.UUID.randomUUID();var payload=new JSONObject().put("id",id.toString()).put("kind","train").put("status","running");var component=new fraymus.factory.ComponentRecord(java.util.UUID.randomUUID(),"workspace.brain.job",1,java.util.Map.of("payload",payload.toString()));var snapshot=new fraymus.factory.WorldSnapshot(1,java.util.List.of(new fraymus.factory.EntityRecord(id,"job",java.util.List.of(component))));new fraymus.factory.WorldCheckpointStore(dir.resolve("brain/tasks.fworld")).save(snapshot,schemas);
  try(var brain=new BrainService(dir)){assertEquals("interrupted",brain.read(id.toString()).getString("status"));assertFalse(brain.state().getBoolean("busy"));assertEquals(1,brain.activePolicy().getInt("version"));}
 }
}
