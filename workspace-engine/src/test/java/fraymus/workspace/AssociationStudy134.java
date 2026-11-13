package fraymus.workspace;
import java.nio.file.*;
import java.util.*;
import org.json.*;
/** Source-only execution is completed before opening independent labels for scoring. */
public final class AssociationStudy134 {
 public static void main(String[] args)throws Exception{
  Path packPath=Path.of(args[0]),labelsPath=Path.of(args[1]),out=Path.of(args[2]);String policy=args[3];if(Files.exists(out))throw new IllegalStateException("Preserve previous attempt");
  var pack=new JSONObject(Files.readString(packPath));var frames=pack.getJSONArray("frames");var seed=PixelTracker.Box.from(pack.getJSONArray("objects").getJSONObject(0).getJSONArray("box"));var tracker=ObservationMethods129.create(policy,"subject","source",decode(frames.getJSONObject(0)),seed);var filter=new ObservationFilter(frames.getJSONObject(0).getInt("sourceFrame"),seed);var trace=new JSONArray();long start=System.nanoTime();
  for(int i=1;i<frames.length();i++){var f=frames.getJSONObject(i);var t=tracker.update(decode(f));var estimate=filter.step(f.getInt("sourceFrame"),t);trace.put(new JSONObject().put("ordinal",i).put("sourceFrame",f.getInt("sourceFrame")).put("imageSha256",f.getString("imageSha256")).put("objects",new JSONArray().put(new JSONObject().put("bodyId","subject").put("tracker",t).put("estimate",estimate))));}
  double processingMs=(System.nanoTime()-start)/1e6;var run=new JSONObject().put("trace",trace);var labels=new JSONObject(Files.readString(labelsPath));var score=AssociationAudit134.compute(labels,pack,run,"subject");var result=new JSONObject().put("policy",policy).put("sourceSha256",PerceptionStore.hash(Files.readAllBytes(packPath))).put("labelsSha256",PerceptionStore.hash(Files.readAllBytes(labelsPath))).put("trace",trace).put("audit",score).put("processingMs",processingMs).put("trackerCodeSha256",WorldObservationStore.identity()).put("auditCodeSha256",AssociationAudit134.identity());Files.createDirectories(out.toAbsolutePath().getParent());Files.writeString(out,result.toString(2),StandardOpenOption.CREATE_NEW);System.out.println(score.getJSONObject("summary"));
 }
 private static java.awt.image.BufferedImage decode(JSONObject f)throws Exception{byte[] b=Base64.getDecoder().decode(f.getString("image"));if(!PerceptionStore.hash(b).equals(f.getString("imageSha256")))throw new IllegalArgumentException("Source bytes changed");return WorldObservationStore.decode(b);}
}
