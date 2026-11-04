package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Evidence059Test {
 @Test public void numericSnapshotsPreserveExactOutputAndSignedZero()throws Exception {
  var value=new JSONObject().put("n",new JSONArray().put(-0.0).put(-0.0f).put(1).put(1L).put(new java.math.BigDecimal("1.2300")).put(new java.math.BigInteger("123456789012345678901234567890"))).put("text","Unicode π 🧠\n");var copy=EvidenceValue059.decode(EvidenceValue059.encode(value));assertEquals(value.toString(),copy.toString());assertEquals(Double.doubleToRawLongBits(-0.0),Double.doubleToRawLongBits(copy.getJSONArray("n").getDouble(0)));assertEquals(Payload057.evidenceHash(value),Payload057.evidenceHash(copy));
 }
 @Test public void sameLengthSameTimestampEditsCannotReuseCheckedEvidence()throws Exception {
  Evidence059.clear();Path p=Files.createTempDirectory("evidence059-").resolve("data.json");var data=new JSONObject().put("value",1);Payload057.save(p,data);var time=Files.getLastModifiedTime(p);long size=Files.size(p);var calls=new AtomicInteger();
  Evidence059.Derive derive=d->{calls.incrementAndGet();return new JSONObject().put("verified",d.getInt("value"));};
  var first=Evidence059.derive(p,"source:policy:code",derive);first.put("verified",999);assertEquals(1,Evidence059.derive(p,"source:policy:code",derive).getInt("verified"));assertEquals(1,calls.get());
  Payload057.save(p,new JSONObject().put("value",2));assertEquals(size,Files.size(p));Files.setLastModifiedTime(p,time);assertEquals(2,Evidence059.derive(p,"source:policy:code",derive).getInt("verified"));assertEquals(2,calls.get());
  Evidence059.derive(p,"changed-source",derive);assertEquals(3,calls.get());Evidence059.derive(p,"changed-policy",derive);assertEquals(4,calls.get());
  var corrupt=new JSONObject(Files.readString(p));corrupt.getJSONObject("data").put("value",3);Files.writeString(p,corrupt.toString());Files.setLastModifiedTime(p,time);assertThrows(IllegalStateException.class,()->Evidence059.derive(p,"changed-policy",derive));assertEquals(4,calls.get());
 }
 @Test public void failuresAreNeverCachedAndColdReadsReverify()throws Exception {
  Evidence059.clear();Path p=Files.createTempDirectory("evidence059-failure-").resolve("original.json");Payload057.save(p,new JSONObject().put("value",1));var calls=new AtomicInteger();
  Evidence059.Derive reject=d->{calls.incrementAndGet();throw new IllegalStateException("Independent check failed");};for(int i=0;i<2;i++)assertThrows(IllegalStateException.class,()->Evidence059.derive(p,"key",reject));assertEquals(2,calls.get());assertEquals(0,Evidence059.stats().getInt("entries"));
  Evidence059.Derive verify=d->{calls.incrementAndGet();return d;};Evidence059.derive(p,"key",verify);Evidence059.clear();Evidence059.derive(p,"key",verify);assertEquals(4,calls.get());
 }
 @Test public void cacheIsBoundedAndReturnedOriginalsAreIsolated()throws Exception {
  Evidence059.clear();Path p=Files.createTempDirectory("evidence059-bounds-").resolve("original.json");Payload057.save(p,new JSONObject().put("value",1));String large="x".repeat(1_100_000);
  for(int i=0;i<35;i++)Evidence059.derive(p,"version"+i,d->new JSONObject().put("large",large));var stats=Evidence059.stats();assertTrue(stats.getLong("retainedBytes")<=32_000_000);assertTrue(stats.getInt("entries")<32);Evidence059.clear();
 }
}
