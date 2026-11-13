package fraymus.workspace;
import java.nio.file.*;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class Persistence137Test {
 @Test public void completedStartupPreservesEveryByteAndTimestampButValidates()throws Exception{
  try(var f=new ObservationLabels132Test.Fixture()){
   var run=f.run();f.observations.close();Path file=f.root.resolve("world-observations/state.json");byte[] original=Files.readAllBytes(file);var time=Files.getLastModifiedTime(file);
   for(int i=0;i<2;i++)try(var reopened=new WorldObservationStore(f.root,new WorldLedger(f.root))){
    assertTrue(reopened.read(run.getString("id")).similar(run));assertArrayEquals(original,Files.readAllBytes(file));assertEquals(time,Files.getLastModifiedTime(file));
    assertEquals(0,reopened.verificationCost136().getLong("durableWrites"));assertEquals(1,reopened.verificationCost136().getLong("structuralValidations"));
   }
  }
 }
 @Test public void runningStartupWritesRecoveryOnceAndRetainsExactPrefix()throws Exception{
  try(var f=new ObservationLabels132Test.Fixture()){
   var run=f.run();f.observations.close();Path file=f.root.resolve("world-observations/state.json");var envelope=new JSONObject(Files.readString(file));var payload=envelope.getJSONObject("payload");payload.getJSONArray("runs").getJSONObject(0).put("status","running");envelope.put("sha256",WorldFrames.hash(payload));Files.writeString(file,envelope.toString());
   try(var reopened=new WorldObservationStore(f.root,new WorldLedger(f.root))){var recovered=reopened.read(run.getString("id"));assertEquals("interrupted",recovered.getString("status"));assertTrue(recovered.getJSONArray("trace").similar(run.getJSONArray("trace")));assertEquals(1,reopened.verificationCost136().getLong("durableWrites"));}
   byte[] recovered=Files.readAllBytes(file);try(var reopened=new WorldObservationStore(f.root,new WorldLedger(f.root))){assertEquals(0,reopened.verificationCost136().getLong("durableWrites"));assertArrayEquals(recovered,Files.readAllBytes(file));}
  }
 }
 @Test public void recursiveCopyIsDetachedAndPreservesNumbersAndNulls(){
  var original=new JSONObject("{\"nested\":[{\"x\":[null,123456789012345678901234567890,0.0000000000000000000001]}]}");
  var copy=ParsedJson137.copy(original);assertTrue(copy.similar(original));copy.getJSONArray("nested").getJSONObject(0).getJSONArray("x").put(0,42);assertTrue(original.getJSONArray("nested").getJSONObject(0).getJSONArray("x").isNull(0));
  assertThrows(IllegalArgumentException.class,()->ParsedJson137.copy(new JSONObject().put("mutable",new StringBuilder("bad"))));
 }
}
