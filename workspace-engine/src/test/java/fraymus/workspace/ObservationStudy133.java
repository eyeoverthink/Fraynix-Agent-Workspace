package fraymus.workspace;
import java.nio.file.*;
import java.util.*;
import org.json.*;

/** Execute source-only candidate, then open the already frozen label file for downstream scoring. */
public final class ObservationStudy133 {
 public static void main(String[] args)throws Exception{
  Path input=Path.of(args[0]),labelsPath=Path.of(args[1]),out=Path.of(args[2]);int window=Integer.parseInt(args[3]);if(Files.exists(out))throw new IllegalStateException("Preserve existing attempt");
  var pack=new JSONObject(Files.readString(input));var frames=pack.getJSONArray("frames");var seed=PixelTracker.Box.from(pack.getJSONArray("objects").getJSONObject(0).getJSONArray("box"));
  var t=new AssociationContact133("subject","original source seed",decode(frames.getJSONObject(0)),seed);var filter=new ObservationFilter(frames.getJSONObject(0).getInt("sourceFrame"),seed);var rows=new JSONArray();JSONObject originalRun=null;
  if(args.length>4){var previous=new JSONObject(Files.readString(Path.of(args[4])));for(Object raw:previous.getJSONArray("comparisons")){var c=(JSONObject)raw;if(c.getInt("window")==window&&c.getString("policy").equals("legacy-flow"))originalRun=c.getJSONObject("run");}if(originalRun==null)throw new IllegalArgumentException("No published original run");}
  var support=new VisualSupport133(decode(frames.getJSONObject(0)),seed);long start=System.nanoTime();
  for(int i=1;i<frames.length();i++){
   var f=frames.getJSONObject(i);var image=decode(f);JSONObject tracked,estimate;
   if(originalRun==null){tracked=t.update(image);estimate=filter.step(f.getInt("sourceFrame"),tracked);}
   else{var previous=originalRun.getJSONArray("trace").getJSONObject(i-1);if(previous.getInt("ordinal")!=i||previous.getInt("sourceFrame")!=f.getInt("sourceFrame")||!previous.getString("imageSha256").equals(f.getString("imageSha256")))throw new IllegalStateException("Published trace/frame differs");var obj=previous.getJSONArray("objects").getJSONObject(0);tracked=obj.getJSONObject("tracker");estimate=obj.getJSONObject("estimate");}
   var contact=estimate.getBoolean("accepted")&&!tracked.isNull("box")?support.support(image,PixelTracker.Box.from(tracked.getJSONArray("box"))):VisualSupport133.unavailable("original association/filter withheld measurement");
   rows.put(new JSONObject().put("ordinal",i).put("sourceFrame",f.getInt("sourceFrame")).put("imageSha256",f.getString("imageSha256")).put("tracker",tracked).put("estimate",estimate).put("contactDerived133",contact));
  }
  double elapsed=(System.nanoTime()-start)/1e6;var annotations=new JSONObject(Files.readString(labelsPath));JSONObject cohort=null;for(Object raw:annotations.getJSONArray("cohorts"))if(((JSONObject)raw).getInt("window")==window)cohort=(JSONObject)raw;if(cohort==null)throw new IllegalArgumentException("No labels");
  int observable=0,available=0,within=0,unsupported=0;double total=0;var scores=new JSONArray();
  var first=decode(frames.getJSONObject(0));
  for(Object raw:cohort.getJSONArray("rows")){var l=(JSONObject)raw;var row=rows.getJSONObject(l.getInt("ordinal")-1);if(!row.getString("imageSha256").equals(l.getString("imageSha256")))throw new IllegalStateException("Label source changed");var c=row.getJSONObject("contactDerived133");boolean found=row.getJSONObject("estimate").getBoolean("accepted")&&!c.isNull("pointImage");var s=WorldFrames.copy(l).put("associationAvailable",row.getJSONObject("estimate").getBoolean("accepted")).put("contactAvailable",found);
   if(l.getString("contact").equals("observed")){observable++;if(found){var a=c.getJSONArray("pointImage");var b=l.getJSONArray("pointImage");double e=Math.hypot((a.getDouble(0)-b.getDouble(0))*first.getWidth(),(a.getDouble(1)-b.getDouble(1))*first.getHeight());s.put("errorPx",e);available++;total+=e;if(e<=l.getDouble("radiusPx"))within++;}}else if(found)unsupported++;scores.put(s);
  }
  var result=new JSONObject().put("associationMethod",originalRun==null?"association-contact-133":"published132-legacy-flow").put("originalRunSha256",originalRun==null?JSONObject.NULL:WorldFrames.hash(originalRun)).put("sourceSha256",PerceptionStore.hash(Files.readAllBytes(input))).put("labelsSha256",PerceptionStore.hash(Files.readAllBytes(labelsPath))).put("rows",rows).put("scores",scores).put("metrics",new JSONObject().put("observable",observable).put("available",available).put("within",within).put("unsupported",unsupported).put("meanPx",available==0?JSONObject.NULL:total/available).put("processingMs",elapsed));Files.createDirectories(out.toAbsolutePath().getParent());Files.writeString(out,result.toString(2),StandardOpenOption.CREATE_NEW);System.out.println(result.getJSONObject("metrics"));
 }
 private static java.awt.image.BufferedImage decode(JSONObject f)throws Exception{byte[] bytes=Base64.getDecoder().decode(f.getString("image"));if(!PerceptionStore.hash(bytes).equals(f.getString("imageSha256")))throw new IllegalStateException("Original pixels changed");return WorldObservationStore.decode(bytes);}
}
