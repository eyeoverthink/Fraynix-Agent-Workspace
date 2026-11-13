package fraymus.workspace;
import java.nio.file.*;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ObservationCache136Test {
 @Test public void repeatedReadsHashEveryByteAndReturnDetachedValues()throws Exception{
  try(var f=new ObservationLabels132Test.Fixture()){
   var run=f.run();var before=f.observations.verificationCost136();long bytes=Files.size(f.root.resolve("world-observations/state.json"));
   for(int i=0;i<4;i++){var result=f.observations.read(run.getString("id"));assertTrue(result.similar(run));result.getJSONArray("trace").getJSONObject(0).put("sourceFrame",999);}
   var after=f.observations.verificationCost136();assertEquals(4,after.getLong("stateReads")-before.getLong("stateReads"));assertEquals(4*bytes,after.getLong("stateBytesHashed")-before.getLong("stateBytesHashed"));assertEquals(4,after.getLong("exactByteReuses")-before.getLong("exactByteReuses"));assertEquals(before.getLong("structuralValidations"),after.getLong("structuralValidations"));assertTrue(run.similar(f.observations.read(run.getString("id"))));
  }
 }
 @Test public void changedBytesWithIdenticalLengthAndTimestampAreRefused()throws Exception{
  try(var f=new ObservationLabels132Test.Fixture()){
   var file=f.root.resolve("world-observations/state.json");byte[] original=Files.readAllBytes(file);var time=Files.getLastModifiedTime(file);f.observations.state("");
   String text=new String(original,java.nio.charset.StandardCharsets.UTF_8);String changed=text.replace("Labelled synthetic static texture control","Labelled synthetic STATIC texture control");assertNotEquals(text,changed);assertEquals(text.length(),changed.length());Files.writeString(file,changed);Files.setLastModifiedTime(file,time);assertThrows(IllegalStateException.class,()->f.observations.state(""));assertEquals(changed,Files.readString(file));Files.write(file,original);Files.setLastModifiedTime(file,time);assertEquals(1,f.observations.state("").getJSONArray("datasets").length());
  }
 }
 @Test public void resignedOuterEnvelopeCannotHideDamagedRevisionChain()throws Exception{
  try(var f=new ObservationLabels132Test.Fixture()){
   f.run();f.observations.state("");var file=f.root.resolve("world-observations/state.json");var envelope=new JSONObject(Files.readString(file));var payload=envelope.getJSONObject("payload");payload.getJSONObject("worlds").getJSONObject(f.world).getJSONArray("history").getJSONObject(0).put("revision",90);envelope.put("sha256",WorldFrames.hash(payload));String damaged=envelope.toString();Files.writeString(file,damaged);assertThrows(IllegalStateException.class,()->f.observations.state(""));assertEquals(damaged,Files.readString(file));
  }
 }
 @Test public void anotherValidWriterInvalidatesTheContentMemo()throws Exception{
  try(var f=new ObservationLabels132Test.Fixture()){
   f.observations.state("");long parsed=f.observations.verificationCost136().getLong("parsedStates");JSONObject added;
   try(var second=new WorldObservationStore(f.root,new WorldLedger(f.root))){added=WorldObservationStoreTest.importPack(second,WorldObservationStoreTest.pack());}
   assertTrue(added.similar(f.observations.dataset(added.getString("id"))));assertTrue(f.observations.verificationCost136().getLong("parsedStates")>parsed);assertEquals(2,f.observations.state("").getJSONArray("datasets").length());
  }
 }
 @Test public void failedPersistenceCannotPublishAnInMemoryDataset()throws Exception{
  try(var f=new ObservationLabels132Test.Fixture()){
   var before=f.observations.state("");var file=f.root.resolve("world-observations/state.json");byte[] bytes=Files.readAllBytes(file);var pending=f.root.resolve("world-observations/state.pending.json");Files.createDirectory(pending);Files.writeString(pending.resolve("block"),"Failure fixture");
   assertThrows(java.io.IOException.class,()->WorldObservationStoreTest.importPack(f.observations,WorldObservationStoreTest.pack()));assertArrayEquals(bytes,Files.readAllBytes(file));assertTrue(before.similar(f.observations.state("")));assertEquals(1,f.observations.state("").getJSONArray("datasets").length());
  }
 }
 @Test public void callerBindingsCannotPoisonCachedDurableStateAndRestartRevalidates()throws Exception{
  try(var f=new ObservationLabels132Test.Fixture()){
   var args=WorldObservationStoreTest.arguments(f.ledger,f.world,f.dataset,0);String id=f.observations.start(args).getString("id");args.getJSONArray("bindings").getJSONObject(0).put("objectKey","changed-by-caller");f.observations.stop(id);var stopped=f.observations.read(id);assertEquals("subject",stopped.getJSONArray("bindings").getJSONObject(0).getString("objectKey"));
   try(var restarted=new WorldObservationStore(f.root,new WorldLedger(f.root))){assertTrue(stopped.similar(restarted.read(id)));assertTrue(restarted.verificationCost136().getLong("structuralValidations")>0);}
  }
 }
}
