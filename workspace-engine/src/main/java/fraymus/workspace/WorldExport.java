package fraymus.workspace;
import org.json.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
/** Full world lineage plus every referenced available native attempt; no selected-summary substitution. */
final class WorldExport {
 static JSONObject pack(JSONObject world,WorldNativeStore nativeStore){
  var ids=new LinkedHashSet<String>();for(Object raw:world.getJSONArray("revisions")){var revision=(JSONObject)raw;for(String key:List.of("runId","abandonedRunId"))if(revision.has(key))ids.add(revision.getString(key));}if(world.has("pending"))ids.add(world.getJSONObject("pending").getString("runId"));
  var runs=new JSONArray();long bytes=0;for(String id:ids){if(!nativeStore.hasRun(id))throw new IllegalStateException("Native attempt missing; original world export cannot be completed");var run=nativeStore.run(id);byte[] raw=run.toString().getBytes(StandardCharsets.UTF_8);bytes+=raw.length;if(bytes>256_000_000)throw new IllegalStateException("Full history exceeds256MBraw bound; export individual attempts without discarding world history");for(Object item:world.getJSONArray("revisions")){var rev=(JSONObject)item;if(rev.optString("runId").equals(id)){if(!WorldFrames.hash(run).equals(rev.getString("nativeCanonicalSha256"))||!WorldVerify.verify(run).getBoolean("passed"))throw new IllegalStateException("Original committed native evidence differs");}}runs.put(run);}
  var value=new JSONObject().put("format","fraynix.persistent-world-export-v1").put("scope","Full declared synthetic world revisions and native attempts. Paused between goals; native solver caches reconstructed. Failed/interrupted attempts do not advance verified head.").put("world",world).put("runs",runs).put("nativeCodeSha256",WorldNativeCheckpoint.identity());
  try{byte[] raw=value.toString().getBytes(StandardCharsets.UTF_8);if(raw.length>256_000_000)throw new IllegalStateException("World export exceeds256MBraw bound");var buffer=new java.io.ByteArrayOutputStream();try(var gzip=new java.util.zip.GZIPOutputStream(buffer)){gzip.write(raw);}var packed=new JSONObject().put("format","fraynix.persistent-world-export-gzip-v1").put("rawBytes",raw.length).put("rawSha256",MapDefinition.sha(raw)).put("gzipBase64",Base64.getEncoder().encodeToString(buffer.toByteArray()));if(packed.toString().getBytes(StandardCharsets.UTF_8).length>20_000_000)throw new IllegalStateException("Full world transport exceeds20MB; export individual native attempts");return packed;}catch(java.io.IOException e){throw new IllegalStateException(e);}
 }
}
