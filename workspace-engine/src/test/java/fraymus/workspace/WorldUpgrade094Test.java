package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import java.nio.file.*;
import java.util.UUID;
import static org.junit.Assert.*;

/** Only an explicit closed-frame handoff admits the previous native engine. */
public class WorldUpgrade094Test {
 static void legacy(Path root) throws Exception {
  Path p=root.resolve("persistent-worlds/state.json");var e=new JSONObject(Files.readString(p));var data=e.getJSONObject("payload");
  data.getJSONArray("worlds").getJSONObject(0).put("codeSha256",WorldLedger.LEGACY_093);
  e.put("sha256",WorldFrames.hash(data));Files.writeString(p,e.toString());
 }
 @Test public void closedPriorWorldNeedsExplicitUpgradeAndPreservesExactFrame() throws Exception {
  var root=Files.createTempDirectory("closed-world094-");var ledger=new WorldLedger(root);var c=WorldLedgerTest.suite().getJSONArray("cases").getJSONObject(0);
  var w=ledger.create(WorldLedgerTest.source(c),"Previous engine fixture");String id=w.getString("id");var before=ledger.observe(id);legacy(root);
  var reopened=new WorldLedger(root);assertTrue(reopened.observe(id).getBoolean("requiresUpgrade051"));
  var args=WorldLedgerTest.args(before,c.getJSONArray("steps").getJSONObject(0));assertThrows(IllegalStateException.class,()->reopened.preview(args));
  var binding=reopened.upgrade051(id,0,before.getString("expectedHash"));assertEquals(1,binding.getInt("expectedRevision"));
  var after=reopened.observe(id);assertTrue(before.getJSONObject("frame").similar(after.getJSONObject("frame")));assertFalse(after.getBoolean("requiresUpgrade051"));
  assertEquals("upgrade094",reopened.read(id).getJSONArray("revisions").getJSONObject(1).getString("kind"));
  assertTrue(new WorldLedger(root).observe(id).similar(after));
 }
 @Test public void pendingPriorWorldCannotBeUpgradedOrReassigned() throws Exception {
  var root=Files.createTempDirectory("pending-world094-");var ledger=new WorldLedger(root);var c=WorldLedgerTest.suite().getJSONArray("cases").getJSONObject(0);
  var w=ledger.create(WorldLedgerTest.source(c),"Previous pending fixture");var before=ledger.observe(w.getString("id"));var plan=ledger.preview(WorldLedgerTest.args(before,c.getJSONArray("steps").getJSONObject(0)));
  String owner=UUID.randomUUID().toString();ledger.reserve(before,owner,UUID.randomUUID().toString(),plan);legacy(root);var reopened=new WorldLedger(root);
  byte[] original=Files.readAllBytes(root.resolve("persistent-worlds/state.json"));assertThrows(IllegalStateException.class,()->reopened.upgrade051(w.getString("id"),0,before.getString("expectedHash")));
  assertArrayEquals(original,Files.readAllBytes(root.resolve("persistent-worlds/state.json")));assertEquals(owner,reopened.observe(w.getString("id")).getJSONObject("pending").getString("goalId"));
 }
}
