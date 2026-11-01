package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

public class TabletopRecoveryTest {
 @Test public void pendingOriginalIsAdoptedExactlyAndIncompleteOriginalIsRetained()throws Exception{
  Path root=Files.createTempDirectory("tabletop038-pending-");var ledger=new WorldLedger(root);String world=ledger.create(WorldLedgerTest.source(WorldLedgerTest.suite().getJSONArray("cases").getJSONObject(0)),"Pending source control").getString("id");
  try(var o=new WorldObservationStore(root,ledger)){var m=new MetricObservationStore(root,o,ledger);String g=m.geometry(TabletopStoreTest.definition()).getString("id");var s=new TabletopStore(root,ledger,m);var c=TabletopStoreTest.check(s,g);var r=s.start(TabletopStoreTest.args(ledger,world,g,c.getString("id")));String id=r.getString("id");for(int i=1;i<=6;i++)TabletopStoreTest.upload(s,r,i);s.stop(id);
   Path dir=root.resolve("tabletop-038/frames").resolve(id);var pending=TabletopStoreTest.frame(7);byte[] b=Base64.getDecoder().decode(pending.getString("image"));pending.remove("image");pending.put("bytes",b.length);Files.write(dir.resolve("7.image"),b);Files.writeString(dir.resolve("7.meta.json"),pending.toString());
   assertEquals(2,s.export(id).getJSONArray("pendingOriginals").length());r=new TabletopStore(root,ledger,m).resume(id);assertEquals(7,r.getInt("completedFrames"));s=new TabletopStore(root,ledger,m);assertEquals("interrupted",s.read(id).getString("status"));Files.write(dir.resolve("8.image"),b);var incomplete=s;assertThrows(IllegalStateException.class,()->incomplete.resume(id));assertEquals("interrupted",s.read(id).getString("status"));assertTrue(Files.exists(dir.resolve("8.image")));assertEquals(1,s.export(id).getJSONArray("pendingOriginals").length());
  }
 }
 @Test public void separatelyAcquiredAnalyticEvaluationUsesOnlyItsOwnReferences()throws Exception{
  Path root=Files.createTempDirectory("tabletop038-crossrun-");var ledger=new WorldLedger(root);String world=ledger.create(WorldLedgerTest.source(WorldLedgerTest.suite().getJSONArray("cases").getJSONObject(0)),"Separate analytic runs").getString("id");try(var o=new WorldObservationStore(root,ledger)){var m=new MetricObservationStore(root,o,ledger);String g=m.geometry(TabletopStoreTest.definition()).getString("id");var s=new TabletopStore(root,ledger,m);var c=TabletopStoreTest.check(s,g);var plane=new PlaneCalibration(TabletopStoreTest.definition());var refs=new JSONArray();for(int i=1;i<=12;i++)refs.put(new JSONObject().put("ordinal",i).put("positionM",new JSONArray(plane.project((114+Math.min(i,8))/320.,95./240))));var first=s.start(TabletopStoreTest.args(ledger,world,g,c.getString("id")));for(int i=1;i<=16;i++)TabletopStoreTest.upload(s,first,i);s.finish(first.getString("id"));var profile=s.analyse(new JSONObject().put("id",first.getString("id")).put("kind","fit").put("references",refs).put("source","Known analytic rendered centres, no hardware accuracy claim"));var second=s.start(TabletopStoreTest.args(ledger,world,g,c.getString("id")));for(int i=1;i<=16;i++)TabletopStoreTest.upload(s,second,i);s.finish(second.getString("id"));var evaluated=s.analyse(new JSONObject().put("id",second.getString("id")).put("kind","evaluate").put("profileId",profile.getString("id")).put("references",refs).put("source","Second analytic acquisition with known coordinates; not independent physical footage"));assertTrue(evaluated.toString(),evaluated.getBoolean("passed"));assertNotEquals(profile.getString("runId"),evaluated.getString("runId"));assertEquals(12,evaluated.getJSONObject("metrics").getInt("referenceCount"));}
 }
}
