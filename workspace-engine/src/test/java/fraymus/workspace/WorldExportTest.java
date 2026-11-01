package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import java.nio.file.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.io.ByteArrayInputStream;
import static org.junit.Assert.*;

public class WorldExportTest {
 @Test public void completeSequentialNativeHistoryExportsLosslesslyAndAlteredOriginalsAreRefused() throws Exception {
  Path root=Files.createTempDirectory("world035-export-");var c=WorldLedgerTest.suite().getJSONArray("cases").getJSONObject(0);var result=WorldLedgerTest.runCase(c,root);var world=result.getJSONObject("world");
  try(var store=new WorldNativeStore(root)){var packed=WorldExport.pack(world,store);byte[] raw;try(var in=new GZIPInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(packed.getString("gzipBase64"))))){raw=in.readAllBytes();}assertEquals(raw.length,packed.getInt("rawBytes"));assertEquals(MapDefinition.sha(raw),packed.getString("rawSha256"));var value=new JSONObject(new String(raw,java.nio.charset.StandardCharsets.UTF_8));assertTrue(world.similar(value.getJSONObject("world")));assertEquals(3,value.getJSONArray("runs").length());for(Object item:value.getJSONArray("runs")){var run=(JSONObject)item;assertTrue(run.similar(store.run(run.getString("id"))));assertTrue(WorldVerify.verify(run).getBoolean("passed"));}}
  Path state=root.resolve("world-native/state.json");var changed=new JSONObject(Files.readString(state));changed.getJSONArray("runs").getJSONObject(0).getJSONArray("samples").getJSONObject(0).getJSONArray("bodies").getJSONObject(0).getJSONArray("velocity").put(0,50);Files.writeString(state,changed.toString());try(var store=new WorldNativeStore(root)){assertThrows(IllegalStateException.class,()->WorldExport.pack(world,store));}
 }
}
