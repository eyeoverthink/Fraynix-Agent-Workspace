package fraymus.workspace;
import org.junit.Test;
import static org.junit.Assert.*;
import org.json.*;

public class BrainPayload064Test {
 @Test public void exactTextRoundTripPreservesUnicodeNumbersAndWhitespace(){String raw=" {\"kind\":\"contact-uncertainty\",\"value\":-0.0,\"tiny\":1e-20,\"text\":\"φ 漢字\",\"long\":\""+"contact repeated ".repeat(20000)+"\"}\n";String stored=BrainPayload064.pack(raw);assertEquals(raw,BrainPayload064.unpack(stored));assertTrue(stored.length()<raw.length()/10);}
 @Test public void oldAndSmallRecordsStayCompatible(){var j=new JSONObject().put("kind","pulse").put("data","x".repeat(80000));assertEquals(j.toString(),BrainPayload064.encode(j));assertEquals(j.toString(),BrainPayload064.unpack(j.toString()));j.put("kind","contact-uncertainty");assertTrue(BrainPayload064.encode(j).startsWith(BrainPayload064.PREFIX));assertEquals(j.toString(),BrainPayload064.unpack(BrainPayload064.encode(j)));assertEquals("{}",BrainPayload064.encode(new JSONObject()));}
 @Test public void corruptedOrOversizedEnvelopesAreRefused(){String packed=BrainPayload064.pack("{\"x\":123}");for(String key:new String[]{"bytes","sha256","data"}){var j=new JSONObject(packed.substring(BrainPayload064.PREFIX.length()));j.put(key,key.equals("bytes")?0:key.equals("sha256")?"changed":"invalid%");assertThrows(RuntimeException.class,()->BrainPayload064.unpack(BrainPayload064.PREFIX+j));}var j=new JSONObject(packed.substring(BrainPayload064.PREFIX.length()));j.put("bytes",BrainPayload064.MAX_BYTES+1);assertThrows(IllegalStateException.class,()->BrainPayload064.unpack(BrainPayload064.PREFIX+j));}
}
