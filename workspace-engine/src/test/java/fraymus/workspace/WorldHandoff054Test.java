package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

/** Copies the exact published synthetic 053 archive; never opens a user's active project. */
public class WorldHandoff054Test {
 static Path copy()throws Exception{var root=Files.createTempDirectory("world-handoff054-");var p=root.resolve("persistent-worlds/state.json");Files.createDirectories(p.getParent());try(var in=WorldHandoff054Test.class.getResourceAsStream("/legacy-world053.json")){Files.write(p,in.readAllBytes());}return root;}
 @Test public void exactPublishedColdWorldNeedsExplicitUpgradeAndRetainsEveryFrame()throws Exception{
  var root=copy();var file=root.resolve("persistent-worlds/state.json");byte[] original=Files.readAllBytes(file);var ledger=new WorldLedger(root);String id=ledger.list().getJSONObject(0).getString("worldId");var before=ledger.observe(id);var history=ledger.read(id).getJSONArray("revisions");assertTrue(before.getBoolean("requiresUpgrade051"));assertArrayEquals(original,Files.readAllBytes(file));assertEquals(Construction054.LEGACY_053,ledger.read(id).getString("codeSha256"));
  assertThrows(IllegalStateException.class,()->ledger.preview(WorldFrames.copy(before).put("construction051",Construction051.options(new JSONObject()))));assertArrayEquals(original,Files.readAllBytes(file));
  var binding=ledger.upgrade051(id,before.getInt("expectedRevision"),before.getString("expectedHash"));assertEquals(before.getInt("expectedRevision")+1,binding.getInt("expectedRevision"));var restarted=new WorldLedger(root);var after=restarted.observe(id);assertFalse(after.getBoolean("requiresUpgrade051"));assertTrue(before.getJSONObject("frame").similar(after.getJSONObject("frame")));
  var revisions=restarted.read(id).getJSONArray("revisions");assertEquals(history.length()+1,revisions.length());for(int i=0;i<history.length();i++)assertTrue(history.getJSONObject(i).similar(revisions.getJSONObject(i)));assertEquals("upgrade054",revisions.getJSONObject(revisions.length()-1).getString("kind"));
  assertThrows(ConcurrentModificationException.class,()->restarted.upgrade051(id,before.getInt("expectedRevision"),before.getString("expectedHash")));assertTrue(restarted.preview(WorldFrames.copy(binding).put("construction051",Construction051.options(new JSONObject()))).getBoolean("passed"));
 }
 @Test public void pendingOwnersAndUnknownIdentitiesCannotBeUpgraded()throws Exception{
  var root=copy();var file=root.resolve("persistent-worlds/state.json");var ledger=new WorldLedger(root);String id=ledger.list().getJSONObject(0).getString("worldId");var before=ledger.observe(id);var envelope=new JSONObject(Files.readString(file));var payload=envelope.getJSONObject("payload");var world=payload.getJSONArray("worlds").getJSONObject(0);
  world.put("pending",new JSONObject().put("goalId",UUID.randomUUID().toString()).put("runId",UUID.randomUUID().toString()).put("baseRevision",before.getInt("expectedRevision")).put("baseHash",before.getString("expectedHash")));envelope.put("sha256",WorldFrames.hash(payload));Files.writeString(file,envelope.toString());byte[] owned=Files.readAllBytes(file);var locked=new WorldLedger(root);assertThrows(IllegalStateException.class,()->locked.upgrade051(id,before.getInt("expectedRevision"),before.getString("expectedHash")));assertArrayEquals(owned,Files.readAllBytes(file));
  world.remove("pending");world.put("codeSha256","0".repeat(64));envelope.put("sha256",WorldFrames.hash(payload));Files.writeString(file,envelope.toString());byte[] unknown=Files.readAllBytes(file);assertThrows(IllegalStateException.class,()->new WorldLedger(root));assertArrayEquals(unknown,Files.readAllBytes(file));
 }
}
